package org.beckenburg;

import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.beckenburg.editor.*;

/** Actual native face preview/apply controls, persistence and runtime publication. */
public final class AssetImportFaceModelingGuiTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Object field(Object owner,String name)throws Exception{var f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    private static void await(java.util.function.BooleanSupplier condition)throws Exception{long end=System.nanoTime()+30_000_000_000L;while(System.nanoTime()<end){var done=new java.util.concurrent.atomic.AtomicBoolean();SwingUtilities.invokeAndWait(()->done.set(condition.getAsBoolean()));if(done.get())return;Thread.sleep(20);}throw new AssertionError("Human face editor worker did not complete");}
    private static boolean ready(AssetImportDialog dock){try{return !((Boolean)field(dock,"busy"))&&((JButton)field(dock,"apply")).isEnabled();}catch(Exception e){throw new RuntimeException(e);}}
    private static boolean reviewed(AssetImportDialog dock){try{return !((Boolean)field(dock,"busy"))&&field(dock,"modelingPreview")!=null;}catch(Exception e){throw new RuntimeException(e);}}
    private static void button(AssetImportDialog dock,String name)throws Exception{((JButton)field(dock,name)).doClick();}
    private static void selectFace(AssetImportDialog dock)throws Exception{
        ((JComboBox<?>)field(dock,"meshView")).setSelectedItem(EditorMeshPicking.Projection.FRONT);((JComboBox<?>)field(dock,"selectionMode")).setSelectedItem("FACE");
        var canvas=(JPanel)field(dock,"preview");int half=canvas.getWidth()/2,height=Math.max(180,canvas.getHeight());double scale=Math.min(half-26,height-36);
        int x=half/2+(int)Math.round((.25-.5)*scale),y=height/2+(int)Math.round((-.25+.5)*scale);
        canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,x,y,1,false,MouseEvent.BUTTON1));
        var hit=(EditorMeshPicking.Hit)field(dock,"selectedHit");check(hit!=null&&hit.faceId()==0,"Real viewport click selects original/cap native face0");
        check(((JButton)field(dock,"previewFaceEdit")).isEnabled(),"Selected native face enables human modeling");
    }
    private static void capture(AssetImportDialog dock,Path file)throws Exception{var content=((JDialog)field(dock,"dialog")).getContentPane();var image=new BufferedImage(content.getWidth(),content.getHeight(),BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();try{content.printAll(g);}finally{g.dispose();}ImageIO.write(image,"png",file.toFile());}
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("human-face-modeling-");String oldHome=System.getProperty("user.home");System.setProperty("user.home",root.toString());
        Path output=Path.of("build/editor-agent-evidence").toAbsolutePath();Files.createDirectories(output);
        Path source=root.resolve("plane.obj");Files.writeString(source,"v 0 0 0\nv 1 0 0\nv 0 1 0\nvt 0 0\nvt 1 0\nvt 0 1\nf 1/1 2/2 3/3\n");byte[] original=Files.readAllBytes(source);
        var ui=new AtomicReference<AssetImportDialog>();var published=new AtomicReference<String>();var project=new AtomicReference<EditorTopologyProject>();var initial=new AtomicReference<EditorTopologyProject.State>();var committed=new AtomicReference<EditorTopologyProject.State>();
        try{
            SwingUtilities.invokeAndWait(()->{var dock=new AssetImportDialog(null,published::set);ui.set(dock);try{((JDialog)field(dock,"dialog")).setVisible(true);}catch(Exception e){throw new RuntimeException(e);}dock.loadSource(source);});await(()->ready(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();project.set((EditorTopologyProject)field(dock,"topologyProject"));initial.set(project.get().snapshot());button(dock,"saveMesh");selectFace(dock);button(dock,"previewFaceEdit");check(((JButton)field(dock,"cancelFaceEdit")).isEnabled(),"In-flight native preview is cancelable");button(dock,"cancelFaceEdit");}catch(Exception e){throw new RuntimeException(e);}});await(()->ready(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();check(field(dock,"modelingPreview")==null&&project.get().snapshot()==initial.get(),"Canceled background candidate cannot install later");button(dock,"previewFaceEdit");}catch(Exception e){throw new RuntimeException(e);}});await(()->reviewed(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();check(project.get().snapshot()==initial.get(),"Human preview leaves actual native state untouched");check(!((JButton)field(dock,"apply")).isEnabled(),"Pending face candidate cannot be published as working geometry");((JComboBox<?>)field(dock,"meshView")).setSelectedItem(EditorMeshPicking.Projection.ISOMETRIC);capture(dock,output.resolve("face-extrude-preview.png"));((JSpinner)field(dock,"faceAmount")).setValue(.3);check(field(dock,"modelingPreview")==null&&!((JButton)field(dock,"applyFaceEdit")).isEnabled(),"Parameter change invalidates reviewed native candidate");((JSpinner)field(dock,"faceAmount")).setValue(.25);button(dock,"previewFaceEdit");}catch(Exception e){throw new RuntimeException(e);}});await(()->reviewed(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();button(dock,"cancelFaceEdit");check(project.get().snapshot()==initial.get()&&((ArrayDeque<?>)field(field(dock,"meshHistory"),"undo")).isEmpty(),"Human cancel preserves native state and history");button(dock,"previewFaceEdit");}catch(Exception e){throw new RuntimeException(e);}});await(()->reviewed(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();project.get().restore(project.get().snapshot());button(dock,"applyFaceEdit");check(project.get().snapshot()==initial.get()&&((ArrayDeque<?>)field(field(dock,"meshHistory"),"undo")).isEmpty(),"Stale preview is refused even with identical native geometry");button(dock,"previewFaceEdit");}catch(Exception e){throw new RuntimeException(e);}});await(()->reviewed(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();var edit=(EditorTopologyProject.EditPreview)field(dock,"modelingPreview");button(dock,"applyFaceEdit");committed.set(project.get().snapshot());check(committed.get()==edit.snapshot()&&committed.get().faces().size()==7,"Human apply commits exact reviewed native extrude candidate");button(dock,"undoMesh");check(project.get().snapshot().equals(initial.get()),"One human undo reverses extrude");button(dock,"redoMesh");check(project.get().snapshot().equals(committed.get()),"One human redo restores extrusion");}catch(Exception e){throw new RuntimeException(e);}});await(()->ready(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();selectFace(dock);((JComboBox<?>)field(dock,"faceOperation")).setSelectedItem(EditorTopologyProject.FaceEdit.INSET);button(dock,"previewFaceEdit");}catch(Exception e){throw new RuntimeException(e);}});await(()->reviewed(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();((JComboBox<?>)field(dock,"meshView")).setSelectedItem(EditorMeshPicking.Projection.ISOMETRIC);capture(dock,output.resolve("face-inset-preview.png"));button(dock,"applyFaceEdit");committed.set(project.get().snapshot());check(committed.get().vertices().size()==9&&committed.get().faces().size()==13&&project.get().topology().manifold(),"Human inset commits real native topology");button(dock,"saveMesh");check(EditorTopologyProject.open(project.get().documentPath()).snapshot().equals(committed.get()),"Fresh native document retains modeled topology");((JSpinner)field(dock,"vertexX")).setValue(.05);button(dock,"moveVertex");button(dock,"reopenMesh");check(project.get().snapshot().equals(committed.get()),"Actual reopen restores saved modeled state");}catch(Exception e){throw new RuntimeException(e);}});await(()->ready(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{capture(ui.get(),output.resolve("face-modeling-committed.png"));button(ui.get(),"apply");}catch(Exception e){throw new RuntimeException(e);}});await(()->published.get()!=null);
            var actual=GlbProps.model(published.get()).triangles();var expected=project.get().mesh().triangles();check(actual.size()==13&&actual.size()==expected.size(),"Published game model consumes committed face edits");
            for(int i=0;i<actual.size();i++){var a=List.of(actual.get(i).a(),actual.get(i).b(),actual.get(i).c());var b=List.of(expected.get(i).a(),expected.get(i).b(),expected.get(i).c());for(int j=0;j<3;j++){var p=a.get(j);var q=b.get(j);check(p.x()==(double)(float)q.x()&&p.y()==(double)(float)q.y()&&p.z()==(double)(float)q.z()&&p.u()==(double)(float)q.u()&&p.v()==(double)(float)q.v(),"Every actual modeled runtime corner/UV/winding");}}
            check(EditorTopologyProject.open(ImportedAssetLibrary.root().resolve(published.get()+".authoring.btop")).snapshot().equals(project.get().snapshot()),"Published native archive is exact committed state");check(Arrays.equals(original,Files.readAllBytes(source))&&Arrays.equals(original,Files.readAllBytes(ImportedAssetLibrary.root().resolve(published.get()+".source.obj"))),"Original source remains exact");
            System.out.println("PASS real human extrude/inset: native preview/cancel/parameter and stale guards, exact apply, one-step history, save/reopen, source/native archives and every actual runtime corner");
        }finally{SwingUtilities.invokeAndWait(()->{if(ui.get()!=null)try{((JDialog)field(ui.get(),"dialog")).dispose();}catch(Exception e){throw new RuntimeException(e);}});System.setProperty("user.home",oldHome);try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
}
