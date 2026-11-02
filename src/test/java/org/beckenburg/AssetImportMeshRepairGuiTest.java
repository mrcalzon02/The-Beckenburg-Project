package org.beckenburg;

import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.beckenburg.editor.*;

public final class AssetImportMeshRepairGuiTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Object field(Object owner,String name)throws Exception{var f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    private static void await(java.util.function.BooleanSupplier condition)throws Exception{long end=System.nanoTime()+30_000_000_000L;while(System.nanoTime()<end){var done=new java.util.concurrent.atomic.AtomicBoolean();SwingUtilities.invokeAndWait(()->done.set(condition.getAsBoolean()));if(done.get())return;Thread.sleep(20);}throw new AssertionError("Human repair worker did not complete");}
    private static boolean ready(AssetImportDialog dock){try{return !((Boolean)field(dock,"busy"))&&((JButton)field(dock,"apply")).isEnabled();}catch(Exception e){throw new RuntimeException(e);}}
    private static boolean reviewed(AssetImportDialog dock){try{return !((Boolean)field(dock,"busy"))&&field(dock,"modelingPreview")!=null;}catch(Exception e){throw new RuntimeException(e);}}
    private static void button(AssetImportDialog dock,String name)throws Exception{((JButton)field(dock,name)).doClick();}
    private static void capture(AssetImportDialog dock,Path file)throws Exception{var content=((JDialog)field(dock,"dialog")).getContentPane();var image=new BufferedImage(content.getWidth(),content.getHeight(),BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();try{content.printAll(g);}finally{g.dispose();}ImageIO.write(image,"png",file.toFile());}
    private static void runtime(String id,EditorTopologyProject project)throws Exception{
        var actual=GlbProps.model(id).triangles();var expected=project.mesh().triangles();check(actual.size()==expected.size(),"Actual repaired runtime triangle count");
        for(int i=0;i<actual.size();i++){var a=List.of(actual.get(i).a(),actual.get(i).b(),actual.get(i).c());var b=List.of(expected.get(i).a(),expected.get(i).b(),expected.get(i).c());for(int j=0;j<3;j++){var p=a.get(j);var q=b.get(j);check(p.x()==(double)(float)q.x()&&p.y()==(double)(float)q.y()&&p.z()==(double)(float)q.z()&&p.u()==(double)(float)q.u()&&p.v()==(double)(float)q.v(),"Every actual repaired runtime position/UV/winding");}}
        check(EditorTopologyProject.open(ImportedAssetLibrary.root().resolve(id+".authoring.btop")).snapshot().equals(project.snapshot()),"Runtime publication archives exact repaired native identities");
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("human-mesh-repair-");String oldHome=System.getProperty("user.home");System.setProperty("user.home",root.toString());Path output=Path.of("build/editor-agent-evidence").toAbsolutePath();Files.createDirectories(output);
        Path source=root.resolve("seam.obj");Files.writeString(source,"""
            o surface
            v 0 0 0
            v 1 0 0
            v 0 1 0
            v 0 1 0
            v 1 0 0
            v 1 1 0
            vt 0 0
            vt 1 0
            vt 0 1
            vt .25 .25
            vt .75 .25
            vt 1 1
            f 1/1 2/2 3/3
            f 4/4 5/5 6/6
            """);byte[] original=Files.readAllBytes(source);var ui=new AtomicReference<AssetImportDialog>();var published=new AtomicReference<String>();var project=new AtomicReference<EditorTopologyProject>();var before=new AtomicReference<EditorTopologyProject.State>();
        try{
            SwingUtilities.invokeAndWait(()->{var dock=new AssetImportDialog(null,published::set);ui.set(dock);try{((JDialog)field(dock,"dialog")).setVisible(true);}catch(Exception e){throw new RuntimeException(e);}dock.loadSource(source);});await(()->ready(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();project.set((EditorTopologyProject)field(dock,"topologyProject"));before.set(project.get().snapshot());check(before.get().vertices().size()==6,"Human source has separate authored seam IDs");((JComboBox<?>)field(dock,"meshView")).setSelectedItem(EditorMeshPicking.Projection.FRONT);((JToggleButton)field(dock,"boxSelect")).setSelected(true);((JCheckBox)field(dock,"xraySelect")).setSelected(true);var canvas=(JPanel)field(dock,"preview");int x=canvas.getWidth()/2-5,y=canvas.getHeight()-5;canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,5,5,1,false,MouseEvent.BUTTON1));canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_DRAGGED,System.currentTimeMillis(),InputEvent.BUTTON1_DOWN_MASK,x,y,0,false,MouseEvent.NOBUTTON));canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_RELEASED,System.currentTimeMillis(),0,x,y,1,false,MouseEvent.BUTTON1));}catch(Exception e){throw new RuntimeException(e);}});await(()->ready(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();check(((Set<?>)field(dock,"selectedVertices")).size()==6&&((JButton)field(dock,"previewWeld")).isEnabled(),"Real X-ray rectangle selects complete source seams");button(dock,"previewWeld");}catch(Exception e){throw new RuntimeException(e);}});await(()->reviewed(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();check(project.get().snapshot()==before.get(),"Human weld preview leaves native source unchanged");capture(dock,output.resolve("mesh-weld-preview.png"));var candidate=(EditorTopologyProject.EditPreview)field(dock,"modelingPreview");button(dock,"applyFaceEdit");check(project.get().snapshot()==candidate.snapshot()&&project.get().snapshot().vertices().size()==4&&project.get().topology().uvSeams()==1,"Human Apply installs exact native weld preserving UV seam");var after=project.get().snapshot();button(dock,"undoMesh");check(project.get().snapshot().equals(before.get()),"One undo restores original separate IDs");button(dock,"redoMesh");check(project.get().snapshot().equals(after),"One redo restores weld");button(dock,"saveMesh");check(EditorTopologyProject.open(project.get().documentPath()).snapshot().equals(after),"Human weld native save/fresh open");}catch(Exception e){throw new RuntimeException(e);}});await(()->ready(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{button(ui.get(),"apply");}catch(Exception e){throw new RuntimeException(e);}});await(()->published.get()!=null);runtime(published.get(),project.get());check(Arrays.equals(original,Files.readAllBytes(source)),"Weld never rewrites original source");
            Path bad=root.resolve("degenerate.obj");Files.writeString(bad,"v 0 0 0\nv 1 0 0\nv 0 1 0\nv 0 0 0\nf 1 2 3\nf 1 4 2\n");byte[] badOriginal=Files.readAllBytes(bad);published.set(null);
            SwingUtilities.invokeAndWait(()->ui.get().loadSource(bad));await(()->ready(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();project.set((EditorTopologyProject)field(dock,"topologyProject"));before.set(project.get().snapshot());check(!project.get().topology().manifold(),"Real native source reports invalid face");button(dock,"previewDegenerates");}catch(Exception e){throw new RuntimeException(e);}});await(()->reviewed(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();check(project.get().snapshot()==before.get(),"Repair preview does not mutate actual native state");capture(dock,output.resolve("mesh-degenerate-repair-preview.png"));button(dock,"applyFaceEdit");check(project.get().snapshot().faces().size()==1&&project.get().topology().manifold(),"Human repair removes real degenerate face");button(dock,"saveMesh");}catch(Exception e){throw new RuntimeException(e);}});await(()->ready(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{button(ui.get(),"apply");}catch(Exception e){throw new RuntimeException(e);}});await(()->published.get()!=null);runtime(published.get(),project.get());check(Arrays.equals(badOriginal,Files.readAllBytes(bad)),"Repair preserves exact original invalid source for provenance");
            System.out.println("PASS actual human exact weld/degenerate repair: real selection/preview/apply, stable IDs/UVs, undo/redo/native persistence and every runtime corner");
        }finally{SwingUtilities.invokeAndWait(()->{if(ui.get()!=null)try{((JDialog)field(ui.get(),"dialog")).dispose();}catch(Exception e){throw new RuntimeException(e);}});System.setProperty("user.home",oldHome);try(var paths=Files.walk(root)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
}
