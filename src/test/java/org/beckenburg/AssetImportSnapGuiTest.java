package org.beckenburg;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.beckenburg.editor.*;
import static org.beckenburg.ModelEditorGuiTestSupport.*;

/** Real Model Editor anchor/target picking and native publication, including coincident distinct IDs. */
public final class AssetImportSnapGuiTest {
    private interface Step {void run() throws Exception;}
    private static void edt(Step action)throws Exception {
        SwingUtilities.invokeAndWait(()->{try{action.run();}catch(Exception failure){throw new RuntimeException(failure);}});
    }
    private static void refuse(Step action)throws Exception {
        try{action.run();throw new AssertionError("Expected snap refusal");}catch(IllegalArgumentException expected){}
    }
    private static void point(AssetImportDialog ui,double x,double y,double z,int event)throws Exception {
        var ref=call(ui,"workingReference",new Class<?>[0]);
        var camera=(EditorMeshPicking.Projector)call(ui,"projector",new Class<?>[]{List.class},ref);
        var view=call(ui,"viewport",new Class<?>[]{List.class,int.class,int.class},ref,0,ui.workingViewportWidth());
        var p=camera.project(x,y,z);int px=(Integer)call(view,"x",new Class<?>[]{double.class},p.x()),py=(Integer)call(view,"y",new Class<?>[]{double.class},p.y());
        var canvas=(JPanel)field(ui,"preview");int button=event==MouseEvent.MOUSE_MOVED?0:MouseEvent.BUTTON1;
        canvas.dispatchEvent(new MouseEvent(canvas,event,System.currentTimeMillis(),0,px,py,1,false,button));
    }
    private static void checkGuide(AssetImportDialog dock)throws Exception {
        var canvas=(JPanel)field(dock,"preview");var image=new BufferedImage(canvas.getWidth(),canvas.getHeight(),BufferedImage.TYPE_INT_RGB);
        var graphics=image.createGraphics();try{canvas.paint(graphics);}finally{graphics.dispose();}
        int pixels=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<dock.workingViewportWidth();x++)if((image.getRGB(x,y)&0xffffff)==0xff9641)pixels++;
        check(pixels>100,"Working canvas shows the snap guide between native vertices");
    }
    public static void main(String[] args)throws Exception {
        Path root=Files.createTempDirectory("native-snap-");String home=System.getProperty("user.home");System.setProperty("user.home",root.toString());
        Path output=Path.of("build/editor-agent-evidence");Files.createDirectories(output);Path source=root.resolve("assembly.obj");
        Files.writeString(source,"v .25 0 0\nv 1.25 0 0\nv .25 1 0\nv 3.25 .5 .5\nv 4.25 .5 .5\nv 3.25 1.5 .5\nvt 0 0\nvt 1 0\nvt 0 1\nvn 0 0 2\no moving\nf 1/1/1 2/2/1 3/3/1\no target\nf 4/1/1 5/2/1 6/3/1\n");
        var original=Files.readAllBytes(source);var ui=new AtomicReference<AssetImportDialog>();var published=new AtomicReference<String>();
        try {
            edt(()->{var dock=new AssetImportDialog(null,published::set);ui.set(dock);((JDialog)field(dock,"dialog")).setVisible(true);dock.loadSource(source);});
            var dock=ui.get();await(()->ready(dock));var project=(EditorTopologyProject)field(dock,"topologyProject");var initial=project.snapshot();
            long anchor=initial.faces().get(0).a().vertex(),target=initial.faces().get(1).a().vertex();
            refuse(()->project.previewVertexSnap(project.observe(),Set.of(anchor),anchor,initial.faces().get(0).b().vertex()));
            refuse(()->project.previewVertexSnap(project.observe(),Set.of(anchor,target),anchor,target));
            var stale=project.observe();project.restore(initial);refuse(()->project.previewVertexSnap(stale,Set.of(anchor),anchor,target));
            edt(()->{
                ((JComboBox<?>)field(dock,"meshView")).setSelectedItem(EditorMeshPicking.Projection.FRONT);
                ((JComboBox<?>)field(dock,"selectionMode")).setSelectedItem("FACE");
                point(dock,.5,.25,0,MouseEvent.MOUSE_PRESSED);tool((JDialog)field(dock,"dialog"),"Move").doClick();
                var toggle=(JToggleButton)field(dock,"snapToVertex");check(toggle.isShowing()&&toggle.isEnabled(),"Move exposes native snap action");toggle.doClick();
                ((JDialog)field(dock,"dialog")).getRootPane().getActionMap().get("cancel-axis-drag").actionPerformed(null);
                check(!toggle.isSelected()&&field(dock,"snapPick")==null&&project.snapshot()==initial,"Escape cancels the snap gesture");toggle.doClick();
                point(dock,.26,.01,0,MouseEvent.MOUSE_PRESSED);
                point(dock,3.26,.51,.5,MouseEvent.MOUSE_MOVED);checkGuide(dock);capture(dock,output.resolve("human-snap-guide.png"));
                check(project.snapshot()==initial,"Arming and hovering leave native geometry unchanged");
                point(dock,3.26,.51,.5,MouseEvent.MOUSE_PRESSED);
            });
            await(()->reviewed(dock));
            edt(()->{
                checkGuide(dock);capture(dock,output.resolve("human-snap-preview.png"));button(dock,"cancelFaceEdit");check(project.snapshot()==initial,"Cancel preserves native scene");
                ((JToggleButton)field(dock,"snapToVertex")).doClick();point(dock,.26,.01,0,MouseEvent.MOUSE_PRESSED);point(dock,3.26,.51,.5,MouseEvent.MOUSE_PRESSED);
            });
            await(()->reviewed(dock));
            edt(()->{
                button(dock,"applyFaceEdit");var moved=project.snapshot();check(moved.faces().equals(initial.faces())&&moved.vertices().size()==6,"Distinct IDs, faces, raw normals and UVs retained");
                for(int i=0;i<6;i++){var a=initial.vertices().get(i);var b=moved.vertices().get(i);if(a.part().equals("moving"))check(b.x()==a.x()+3&&b.y()==a.y()+.5&&b.z()==a.z()+.5,"Whole selection translated");else check(a.equals(b),"Target part unchanged");}
                check(project.frame("moving").ox()==3&&project.frame("moving").oy()==.5&&project.frame("moving").oz()==.5,"Whole-part origin snaps with geometry");
                button(dock,"undoMesh");check(project.snapshot().equals(initial),"One undo reverses snap and frame");button(dock,"redoMesh");button(dock,"saveMesh");button(dock,"reopenMesh");
                check(EditorTopologyProject.open(project.documentPath()).snapshot().equals(moved),"Fresh native file retains exact snap");
            });
            await(()->ready(dock));edt(()->button(dock,"apply"));await(()->published.get()!=null);
            byte[] glb=ImportedAssetLibrary.read(published.get());EditorGlbWriter.validate(project.mesh(),glb);
            var game=GlbProps.model(published.get()).triangles();var expected=project.mesh().triangles();check(game.size()==expected.size(),"Game count after snap");
            for(int i=0;i<game.size();i++){var t=game.get(i);var n=expected.get(i);var a=List.of(t.a(),t.b(),t.c());var b=List.of(n.a(),n.b(),n.c());for(int j=0;j<3;j++)check(a.get(j).x()==b.get(j).x()&&a.get(j).y()==b.get(j).y()&&a.get(j).z()==b.get(j).z(),"Every game position matches snapped native mesh");}
            check(EditorGlbReader.read(glb).mesh("snapped.glb").cornerVertexIds().stream().distinct().count()==6,"Actual GLB reimport retains all six distinct source identities");
            var archive=ImportedAssetLibrary.root().resolve(published.get()+".authoring.btop");
            check(EditorTopologyProject.open(archive).snapshot().equals(project.snapshot()),"Published native archive retains snapped geometry and part frame");
            check(Arrays.equals(original,Files.readAllBytes(source)),"Original OBJ preserved");
            Files.write(output.resolve("human-snap.glb"),glb);Files.write(output.resolve("human-snap.btop"),project.snapshotBytes());
            System.out.println("PASS native vertex snap: visible anchor/target guide, distinct IDs, shape/frame translation, guards, one-step history, reopen and actual game vertices");
        } finally {
            edt(()->{if(ui.get()!=null)((JDialog)field(ui.get(),"dialog")).dispose();});System.setProperty("user.home",home);
            try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
        }
    }
}
