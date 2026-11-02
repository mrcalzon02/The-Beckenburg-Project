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
    private static JFileChooser chooser(java.awt.Container parent){
        for(var child:parent.getComponents()){if(child instanceof JFileChooser found)return found;if(child instanceof java.awt.Container nested){var found=chooser(nested);if(found!=null)return found;}}return null;
    }
    private static void exportThroughChooser(AssetImportDialog dock,Path target)throws Exception{
        SwingUtilities.invokeAndWait(()->{
            var timer=new javax.swing.Timer(50,null);timer.addActionListener(e->{for(var window:java.awt.Window.getWindows())if(window.isShowing()){var chooser=chooser(window);if(chooser!=null){timer.stop();chooser.setSelectedFile(target.toFile());chooser.approveSelection();return;}}});timer.start();
            try{button(dock,"exportMesh");}catch(Exception failure){throw new RuntimeException(failure);}finally{timer.stop();}
        });
        check(Files.exists(target),"Actual human Save chooser exports OBJ");
    }
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
            Path exported=root.resolve("welded-copy.obj");var exportedState=project.get().snapshot();var exportedTriangles=project.get().mesh().triangles();exportThroughChooser(ui.get(),exported);EditorObjWriter.validate(exportedState,Files.readAllBytes(exported));
            SwingUtilities.invokeAndWait(()->check(!ui.get().exportObj(source),"Human export refuses original source overwrite"));
            published.set(null);SwingUtilities.invokeAndWait(()->ui.get().loadSource(exported));await(()->ready(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{project.set((EditorTopologyProject)field(ui.get(),"topologyProject"));check(project.get().snapshot().vertices().size()==4&&project.get().topology().uvSeams()==1,"Fresh human OBJ import retains authored connectivity and UV seam");check(project.get().mesh().triangles().equals(exportedTriangles),"Exported human geometry matches native archive");}catch(Exception failure){throw new RuntimeException(failure);}});
            SwingUtilities.invokeAndWait(()->{try{button(ui.get(),"apply");}catch(Exception failure){throw new RuntimeException(failure);}});await(()->published.get()!=null);runtime(published.get(),project.get());
            Path bad=root.resolve("degenerate.obj");Files.writeString(bad,"v 0 0 0\nv 1 0 0\nv 0 1 0\nv 0 0 0\nf 1 2 3\nf 1 4 2\n");byte[] badOriginal=Files.readAllBytes(bad);published.set(null);
            SwingUtilities.invokeAndWait(()->ui.get().loadSource(bad));await(()->ready(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();project.set((EditorTopologyProject)field(dock,"topologyProject"));before.set(project.get().snapshot());check(!project.get().topology().manifold(),"Real native source reports invalid face");button(dock,"previewDegenerates");}catch(Exception e){throw new RuntimeException(e);}});await(()->reviewed(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();check(project.get().snapshot()==before.get(),"Repair preview does not mutate actual native state");capture(dock,output.resolve("mesh-degenerate-repair-preview.png"));button(dock,"applyFaceEdit");check(project.get().snapshot().faces().size()==1&&project.get().topology().manifold(),"Human repair removes real degenerate face");button(dock,"saveMesh");}catch(Exception e){throw new RuntimeException(e);}});await(()->ready(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{button(ui.get(),"apply");}catch(Exception e){throw new RuntimeException(e);}});await(()->published.get()!=null);runtime(published.get(),project.get());check(Arrays.equals(badOriginal,Files.readAllBytes(bad)),"Repair preserves exact original invalid source for provenance");
            SwingUtilities.invokeAndWait(()->{try{button(ui.get(),"previewUnused");}catch(Exception failure){throw new RuntimeException(failure);}});await(()->reviewed(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();var beforeCleanup=project.get().snapshot();var candidate=(EditorTopologyProject.EditPreview)field(dock,"modelingPreview");check(candidate.snapshot().vertices().size()==3&&beforeCleanup.vertices().size()==4,"Unused cleanup preview is explicit");capture(dock,output.resolve("mesh-unused-cleanup-preview.png"));button(dock,"applyFaceEdit");var afterCleanup=project.get().snapshot();button(dock,"undoMesh");check(project.get().snapshot().equals(beforeCleanup),"Human cleanup undo retains unused ID");button(dock,"redoMesh");check(project.get().snapshot().equals(afterCleanup),"Human cleanup redo installs candidate");button(dock,"saveMesh");}catch(Exception failure){throw new RuntimeException(failure);}});await(()->ready(ui.get()));
            exportThroughChooser(ui.get(),root.resolve("cleaned-copy.obj"));EditorObjWriter.validate(project.get().snapshot(),Files.readAllBytes(root.resolve("cleaned-copy.obj")));
            published.set(null);SwingUtilities.invokeAndWait(()->{try{button(ui.get(),"apply");}catch(Exception failure){throw new RuntimeException(failure);}});await(()->published.get()!=null);runtime(published.get(),project.get());
            System.out.println("PASS actual human native weld/repair/cleanup/OBJ chooser: real selection/preview/apply, stable IDs/UVs, undo/redo/native persistence and every runtime corner");
        }finally{SwingUtilities.invokeAndWait(()->{if(ui.get()!=null)try{((JDialog)field(ui.get(),"dialog")).dispose();}catch(Exception e){throw new RuntimeException(e);}});System.setProperty("user.home",oldHome);try(var paths=Files.walk(root)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
}
