package org.beckenburg;

import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.beckenburg.editor.*;

/** Real human material preview/history/save/publish and authoritative runtime sampling. */
public final class AssetImportMaterialsGuiTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Object field(Object owner,String name)throws Exception{var f=(owner instanceof Class<?> type?type:owner.getClass()).getDeclaredField(name);f.setAccessible(true);return f.get(owner instanceof Class<?>?null:owner);}
    private static void button(AssetImportDialog dock,String name)throws Exception{((JButton)field(dock,name)).doClick();}
    private static boolean ready(AssetImportDialog dock){try{return !((Boolean)field(dock,"busy"))&&((JButton)field(dock,"apply")).isEnabled();}catch(Exception e){throw new RuntimeException(e);}}
    private static boolean reviewed(AssetImportDialog dock){try{return !((Boolean)field(dock,"busy"))&&field(dock,"modelingPreview")!=null;}catch(Exception e){throw new RuntimeException(e);}}
    private static void await(java.util.function.BooleanSupplier condition)throws Exception{long end=System.nanoTime()+30_000_000_000L;while(System.nanoTime()<end){var done=new java.util.concurrent.atomic.AtomicBoolean();SwingUtilities.invokeAndWait(()->done.set(condition.getAsBoolean()));if(done.get())return;Thread.sleep(20);}throw new AssertionError("Material worker did not complete");}
    private static JToggleButton tool(java.awt.Container parent,String name){for(var child:parent.getComponents()){if(child instanceof JToggleButton b&&b.getText().equals(name))return b;if(child instanceof java.awt.Container nested){var found=tool(nested,name);if(found!=null)return found;}}return null;}
    private static void select(AssetImportDialog dock)throws Exception{
        ((JComboBox<?>)field(dock,"selectionMode")).setSelectedItem("FACE");((JComboBox<?>)field(dock,"meshView")).setSelectedItem(EditorMeshPicking.Projection.FRONT);
        var canvas=(JPanel)field(dock,"preview");int width=dock.workingViewportWidth(),height=Math.max(180,canvas.getHeight());double scale=Math.min(width-26,height-36);
        int x=width/2+(int)Math.round(-.25*scale),y=height/2+(int)Math.round(.25*scale);canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,x,y,1,false,MouseEvent.BUTTON1));
        var hit=(EditorMeshPicking.Hit)field(dock,"selectedHit");check(hit!=null&&hit.faceId()==0&&((JButton)field(dock,"chooseMaterial")).isEnabled(),"Human face selection uses actual native face");
        dock.openMaterials();var window=(JDialog)field(dock,"materialDialog");check(window!=field(dock,"dialog")&&window.isVisible()&&window.getTitle().contains("Material Editor"),"Materials have a separate owned window");check(SwingUtilities.getWindowAncestor((JButton)field(dock,"chooseMaterial"))==window,"Material controls belong to their own window");check(tool((JDialog)field(dock,"dialog"),"Material")==null,"Model tool rail does not inherit material layout");
    }
    private static void capture(AssetImportDialog dock,Path file)throws Exception{var content=((JDialog)field(dock,"materialDialog")).getContentPane();var image=new BufferedImage(content.getWidth(),content.getHeight(),BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();try{content.printAll(g);}finally{g.dispose();}ImageIO.write(image,"png",file.toFile());}
    private static byte[] png()throws Exception{var im=new BufferedImage(4,4,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<4;y++)for(int x=0;x<4;x++)im.setRGB(x,y,x<2?(y<2?0xffff0000:0xff0000ff):(y<2?0x8000ff00:0xffffff00));var bytes=new java.io.ByteArrayOutputStream();ImageIO.write(im,"png",bytes);return bytes.toByteArray();}
    private static void verifyRuntime(EditorTopologyProject project,byte[] png)throws Exception{
        var model=GlbProps.decode(EditorGlbWriter.encode(project.mesh()));check(model.triangles().size()==2,"Runtime retains both actual native faces");int a=model.triangles().get(0).material(),b=model.triangles().get(1).material();
        check(a!=b&&GlbProps.materialName(a).equals("asymmetric.png"),"Face bindings use distinct actual runtime materials");
        check(GlbProps.sampleRgba(a,.25,.25)==0xffff0000&&GlbProps.sampleRgba(a,.75,.25)==0x8000ff00&&GlbProps.sampleRgba(a,.25,.75)==0xff0000ff&&GlbProps.sampleRgba(a,.75,.75)==0xffffff00,"Asymmetric original texels, alpha and explicit top-left UV origin reach the real runtime");
        check(GlbProps.sampleRgba(a,-.25,.25)==0xffff0000&&GlbProps.sampleRgba(a,1.25,.25)==0x8000ff00,"Clamp sampler reaches actual runtime");
        check(Arrays.equals(project.snapshot().materials().get(0).png(),png),"Runtime generation does not rewrite original PNG");
        var face=project.mesh().triangles().get(0);var source=project.snapshot().materials().get(0);
        for(int wrap:new int[]{10497,33648})for(var origin:EditorMeshMaterial.Origin.values()){
            var m=new EditorMeshMaterial("test", "Sampler test",png,new EditorMeshMaterial.Color(.5,1,.25,.5),wrap,wrap,EditorMeshMaterial.Alpha.MASK,.6,origin,Map.of());
            var t=new EditorMeshPipeline.Triangle(face.part(),face.a(),face.b(),face.c(),m.key());var actual=GlbProps.decode(EditorGlbWriter.encode(new EditorMeshPipeline.Mesh("sample",List.of(t),0,List.of(),List.of(m))));int mat=actual.triangles().get(0).material();
            int expected=origin==EditorMeshMaterial.Origin.TOP_LEFT?0x80000040:0x80800000;check(GlbProps.sampleRgba(mat,.25,.75)==expected,"Diffuse and alpha factors plus explicit origin survive real decoder");
            check(GlbProps.sampleRgba(mat,-.25,.25)==GlbProps.sampleRgba(mat,wrap==10497?.75:.25,.25),"Repeat/mirror settings survive real decoder");
        }
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("human-material-");String oldHome=System.getProperty("user.home");System.setProperty("user.home",root.toString());Path output=Path.of("build/editor-agent-evidence").toAbsolutePath();Files.createDirectories(output);
        Path source=root.resolve("source.obj"),png=root.resolve("asymmetric.png");Files.writeString(source,"o panel\nv 0 0 0\nv 1 0 0\nv 0 1 0\nv 1 1 0\nvt 0 0\nvt 1 0\nvt 0 1\nvt 1 1\nvn 0 0 1\nf 1/1/1 2/2/1 3/3/1\nf 3/3/1 2/2/1 4/4/1\n");byte[] original=Files.readAllBytes(source),texture=png();Files.write(png,texture);
        var ui=new AtomicReference<AssetImportDialog>();var project=new AtomicReference<EditorTopologyProject>();var published=new AtomicReference<String>();var before=new AtomicReference<EditorTopologyProject.State>();
        try{
            SwingUtilities.invokeAndWait(()->{var dock=new AssetImportDialog(null,published::set);ui.set(dock);try{((JDialog)field(dock,"dialog")).setVisible(true);}catch(Exception e){throw new RuntimeException(e);}dock.loadSource(source);});await(()->ready(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();project.set((EditorTopologyProject)field(dock,"topologyProject"));before.set(project.get().snapshot());select(dock);((JComboBox<?>)field(dock,"materialWrap")).setSelectedIndex(1);((JComboBox<?>)field(dock,"materialAlpha")).setSelectedItem(EditorMeshMaterial.Alpha.BLEND);((JComboBox<?>)field(dock,"materialOrigin")).setSelectedItem(EditorMeshMaterial.Origin.TOP_LEFT);dock.previewPngMaterial(png);}catch(Exception e){throw new RuntimeException(e);}});await(()->reviewed(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{check(project.get().snapshot()==before.get(),"PNG chooser preview does not mutate native state");capture(ui.get(),output.resolve("material-binding-preview.png"));button(ui.get(),"cancelFaceEdit");check(project.get().snapshot()==before.get(),"Human Cancel retains exact state");select(ui.get());ui.get().previewPngMaterial(png);}catch(Exception e){throw new RuntimeException(e);}});await(()->reviewed(ui.get()));
            // The exact captured image, rather than a later mutable file lookup, must be installed.
            Files.writeString(png,"changed after preview");
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();var edit=(EditorTopologyProject.EditPreview)field(dock,"modelingPreview");button(dock,"applyFaceEdit");check(project.get().snapshot()==edit.snapshot(),"Human Apply installs exact captured native candidate");button(dock,"undoMesh");check(project.get().snapshot().equals(before.get()),"Human Undo removes material and assignment together");button(dock,"redoMesh");check(project.get().snapshot().equals(edit.snapshot()),"Human Redo restores exact PNG and binding");}catch(Exception e){throw new RuntimeException(e);}});await(()->ready(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{button(ui.get(),"saveMesh");check(EditorTopologyProject.open(project.get().documentPath()).snapshot().equals(project.get().snapshot()),"Human Save/fresh open retains complete native material");capture(ui.get(),output.resolve("material-binding-saved.png"));button(ui.get(),"apply");}catch(Exception e){throw new RuntimeException(e);}});await(()->published.get()!=null);
            await(()->{try{return !((Boolean)field(ui.get(),"materialRendering"))&&((JLabel)field(ui.get(),"materialRuntime")).getIcon()!=null;}catch(Exception e){throw new RuntimeException(e);}});
            SwingUtilities.invokeAndWait(()->{try{capture(ui.get(),output.resolve("material-workspace.png"));var workspace=field(ui.get(),"materialWorkspace");var list=(JList<?>)field(workspace,"faceList");list.setSelectedIndex(1);check(((EditorMeshPicking.Hit)field(ui.get(),"selectedHit")).faceId()==1,"Material face selection synchronizes the same native model selection");var rect=workspace.getClass().getDeclaredMethod("imageRect");rect.setAccessible(true);double[] r=(double[])rect.invoke(workspace);var canvas=(JPanel)field(workspace,"canvas");canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,(int)(r[0]+r[2]*.2),(int)(r[1]+r[3]*.2),1,false,MouseEvent.BUTTON1));check(((EditorMeshPicking.Hit)field(ui.get(),"selectedHit")).faceId()==0&&list.getSelectedIndex()==0,"Real UV-canvas click selects the same native face in the model and face list");capture(ui.get(),output.resolve("material-workspace.png"));}catch(Exception e){throw new RuntimeException(e);}});
            verifyRuntime(project.get(),texture);check(EditorTopologyProject.open(ImportedAssetLibrary.root().resolve(published.get()+".authoring.btop")).snapshot().equals(project.get().snapshot()),"Published source archive is the exact authoritative native state");check(Arrays.equals(Files.readAllBytes(source),original),"Original geometry source unchanged");
            var renderer=new SceneRenderer();renderer.setRenderSize(640,400);renderer.x=SceneRenderer.devAssetX(0)-1.8;renderer.z=SceneRenderer.devAssetZ(0)-3.5;renderer.eyeHeight=1.5;renderer.yaw=Math.atan2(1.8,3.5);renderer.pitch=-.08;renderer.lamp=false;renderer.drawDevAssetRoom(List.of(new DevAssetReviewCatalog.Entry(DevAssetReviewCatalog.Kind.IMPORTED,published.get())),0,.4);ImageIO.write(renderer.image(),"png",output.resolve("material-binding-runtime.png").toFile());
            var nativeFace=project.get().mesh().triangles().get(0);var alternating=new ArrayList<EditorMeshPipeline.Triangle>();var binding=project.get().snapshot().materials().get(0);for(int i=0;i<800;i++)alternating.add(new EditorMeshPipeline.Triangle("part-"+i,nativeFace.a(),nativeFace.b(),nativeFace.c(),binding.key()));
            byte[] many=EditorGlbWriter.encode(new EditorMeshPipeline.Mesh("many materials",alternating,0,List.of(),List.of(binding)));check(GlbProps.decode(many).triangles().size()==800,"Hundreds of primitive-local material runs decode actual geometry under bounded data");
            int registry=((List<?>)field(GlbProps.class,"textures")).size();
            byte[] captured=EditorGlbWriter.encode(project.get().mesh());for(int i=0;i<8;i++){var live=new SceneRenderer();live.setRenderSize(280,190);GlbProps.renderNativeMaterialReview(captured,live,i*.1);check(((List<?>)field(GlbProps.class,"textures")).size()==registry,"Transient real material previews do not leak runtime registrations");}
            var nativeDestination=output.resolve("material-binding.obj.zip");SwingUtilities.invokeAndWait(()->check(ui.get().exportObj(nativeDestination),"Human export saves a material-preserving OBJ bundle"));EditorObjBundle.validate(project.get().snapshot(),Files.readAllBytes(nativeDestination));
            Files.copy(project.get().documentPath(),output.resolve("material-binding.btop"),StandardCopyOption.REPLACE_EXISTING);Files.copy(ImportedAssetLibrary.root().resolve(published.get()+".glb"),output.resolve("material-binding.glb"),StandardCopyOption.REPLACE_EXISTING);Files.write(output.resolve("material-binding-original.png"),texture);
            System.out.println("PASS real human PNG materials: face picking, captured preview/cancel/apply, exact shared undo/redo/save/fresh open/publication, runtime face bindings/origin/RGBA/tint/wrap and production screenshot");
        }finally{SwingUtilities.invokeAndWait(()->{if(ui.get()!=null)try{((JDialog)field(ui.get(),"dialog")).dispose();}catch(Exception e){throw new RuntimeException(e);}});System.setProperty("user.home",oldHome);try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
}
