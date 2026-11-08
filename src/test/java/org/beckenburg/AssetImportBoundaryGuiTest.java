package org.beckenburg;

import java.awt.event.MouseEvent;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.beckenburg.editor.*;
import static org.beckenburg.ModelEditorGuiTestSupport.*;

/** Select a hole boundary, transform it as one selection and check the game mesh. */
public final class AssetImportBoundaryGuiTest {
    private interface Step { void run() throws Exception; }
    private static void edt(Step action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try { action.run(); } catch (Exception failure) { throw new RuntimeException(failure); }
        });
    }
    private static final String RING = """
            o ring
            v -2 -2 0
            v 2 -2 0
            v 2 2 0
            v -2 2 0
            v -1 -1 0
            v 1 -1 0
            v 1 1 0
            v -1 1 0
            f 1 2 6 5
            f 2 3 7 6
            f 3 4 8 7
            f 4 1 5 8
            o separate
            v 3 0 0
            v 4 0 0
            v 3 1 0
            f 9 10 11
            """;
    public static void main(String[] args) throws Exception {
        Path root=Files.createTempDirectory("native-boundary-");
        Path source=root.resolve("ring.obj"); Files.writeString(source,RING);
        Path output=Path.of("build/editor-agent-evidence").toAbsolutePath(); Files.createDirectories(output);
        String previous=System.getProperty("user.home"); System.setProperty("user.home",root.toString());
        var ui=new AtomicReference<AssetImportDialog>(); var published=new AtomicReference<String>();
        try {
            edt(()->{var dock=new AssetImportDialog(null,published::set);ui.set(dock);((JDialog)field(dock,"dialog")).setVisible(true);dock.loadSource(source);});
            var dock=ui.get(); await(()->ready(dock)); var project=(EditorTopologyProject)field(dock,"topologyProject");
            var before=project.snapshot();long epoch=project.revision();var inner=new HashSet<Long>();
            for(var v:before.vertices())if(Math.abs(v.x())==1 && Math.abs(v.y())==1)inner.add(v.id());
            var outer=before.vertices().stream().filter(v->v.x()==-2 && v.y()==-2).findFirst().orElseThrow();
            var innerAtCorner=before.vertices().stream().filter(v->v.x()==-1 && v.y()==-1).findFirst().orElseThrow();
            try {project.observe().boundaryLoop(outer.id(),innerAtCorner.id());throw new AssertionError("Interior edge accepted as boundary");}
            catch(IllegalArgumentException expected) {check(project.revision()==epoch,"Refused interior edge preserves native revision");}
            edt(()->{
                ((JComboBox<?>)field(dock,"meshView")).setSelectedItem(EditorMeshPicking.Projection.FRONT);
                ((JComboBox<?>)field(dock,"selectionMode")).setSelectedItem("EDGE");
                var ref=call(dock,"workingReference",new Class<?>[0]);var view=call(dock,"viewport",new Class<?>[]{List.class,int.class,int.class},ref,0,dock.workingViewportWidth());
                var at=EditorMeshPicking.Projection.FRONT.project(0,-1.025,0);
                int x=(Integer)call(view,"x",new Class<?>[]{double.class},at.x()),y=(Integer)call(view,"y",new Class<?>[]{double.class},at.y());
                var canvas=(JPanel)field(dock,"preview");canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,x,y,1,false,1));
                var hit=(EditorMeshPicking.Hit)field(dock,"selectedHit");check(hit!=null && inner.containsAll(List.of(hit.edgeA(),hit.edgeB())),"Real pick hits hole rim");
                for(long[] seed:List.of(new long[]{hit.edgeA(),hit.edgeB()},new long[]{hit.edgeB(),hit.edgeA()})){
                    var loop=project.observe().boundaryLoop(seed[0],seed[1]);check(loop.size()==4 && loop.get(0).id()==seed[0] && loop.get(1).id()==seed[1],"Loop respects either seed direction");
                }
                check(((JButton)field(dock,"selectBoundary")).isShowing(),"Boundary action visible in Select inspector");button(dock,"selectBoundary");
            });
            await(()->{try{return (boolean)call(dock,"hasBoundary",new Class<?>[0]);}catch(Exception e){throw new RuntimeException(e);}});
            edt(()->{
                check(call(dock,"selectedIds",new Class<?>[0]).equals(inner),"Only the real hole rim is selected");
                check(project.revision()==epoch && project.snapshot()==before,"Selecting a boundary never edits geometry or history");
                capture(dock,output.resolve("human-boundary-loop.png"));tool((JDialog)field(dock,"dialog"),"Transform").doClick();
                ((JComboBox<?>)field(dock,"transformKind")).setSelectedItem(EditorTopologyProject.Transform.SCALE_UNIFORM);
                ((JSpinner)field(dock,"transformAmount")).setValue(1.25);button(dock,"previewTransform");
            });
            await(()->reviewed(dock));
            edt(()->{
                button(dock,"applyFaceEdit");var moved=project.snapshot();
                for(int i=0;i<before.vertices().size();i++){var v=before.vertices().get(i);var now=moved.vertices().get(i);if(inner.contains(v.id()))check(now.x()==v.x()*1.25 && now.y()==v.y()*1.25,"Hole vertex scaled");else check(now.equals(v),"Outer boundary and separate part unchanged");}
                check(!(boolean)call(dock,"hasBoundary",new Class<?>[0]),"Old boundary observation invalid after commit");
                button(dock,"undoMesh");check(project.snapshot().equals(before),"One undo restores entire hole");button(dock,"redoMesh");button(dock,"saveMesh");
                check(EditorTopologyProject.open(project.documentPath()).snapshot().equals(moved),"Fresh open retains edited hole");
            });
            await(()->ready(dock));edt(()->button(dock,"apply"));await(()->published.get()!=null);
            var game=GlbProps.model(published.get()).triangles();var nativeMesh=project.mesh().triangles();check(game.size()==nativeMesh.size(),"Game triangle count");
            for(int i=0;i<game.size();i++){var a=game.get(i);var b=nativeMesh.get(i);var got=List.of(a.a(),a.b(),a.c());var want=List.of(b.a(),b.b(),b.c());for(int j=0;j<3;j++)check(got.get(j).x()==want.get(j).x()&&got.get(j).y()==want.get(j).y()&&got.get(j).z()==want.get(j).z(),"Every published game corner matches native ring");}
            System.out.println("PASS boundary loop: directed native hole rim, independent outer/part boundaries, read-only selection, shared transform/undo/save/reopen and actual game corners");
        } finally {
            edt(()->{if(ui.get()!=null)((JDialog)field(ui.get(),"dialog")).dispose();});System.setProperty("user.home",previous);
            try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
        }
    }
}
