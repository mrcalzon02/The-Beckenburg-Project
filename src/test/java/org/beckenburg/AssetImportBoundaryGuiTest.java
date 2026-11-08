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
    private static void awaitEdges(AssetImportDialog dock)throws Exception {
        await(()->{try{return (boolean)call(dock,"hasEdgeSelection",new Class<?>[0]);}catch(Exception e){throw new RuntimeException(e);}});
    }
    private static void verifyInteriorGrid(AssetImportDialog dock,Path root,Path output)throws Exception {
        var mesh=EditorMeshPrimitives.preview(new EditorMeshPrimitives.Parameters(EditorMeshPrimitives.Kind.PLANE,4,1,4,4,4));
        var grid=EditorTopologyProject.fromMesh(mesh,"1".repeat(64),root.resolve("fixture.btop"));
        Path source=root.resolve("grid.obj");try(var stream=Files.newOutputStream(source)){EditorObjWriter.write(grid.snapshot(),stream);}
        edt(()->dock.loadSource(source));await(()->ready(dock));
        var project=(EditorTopologyProject)field(dock,"topologyProject");var before=project.snapshot();
        edt(()->{
            ((JComboBox<?>)field(dock,"meshView")).setSelectedItem(EditorMeshPicking.Projection.TOP);
            ((JComboBox<?>)field(dock,"selectionMode")).setSelectedItem("EDGE");
            var ref=call(dock,"workingReference",new Class<?>[0]);var view=call(dock,"viewport",new Class<?>[]{List.class,int.class,int.class},ref,0,dock.workingViewportWidth());
            var point=EditorMeshPicking.Projection.TOP.project(.5,0,.015);
            int x=(Integer)call(view,"x",new Class<?>[]{double.class},point.x()),y=(Integer)call(view,"y",new Class<?>[]{double.class},point.y());
            var canvas=(JPanel)field(dock,"preview");canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,x,y,1,false,1));
            ((JComboBox<?>)field(dock,"edgeMode")).setSelectedItem(EditorMeshEdges.Mode.RING);button(dock,"selectEdges");
        });
        awaitEdges(dock);
        edt(()->{
            var wanted=before.vertices().stream().filter(v->v.x()==0||v.x()==1).map(EditorTopologyProject.Vertex::id).collect(java.util.stream.Collectors.toSet());
            check(call(dock,"selectedIds",new Class<?>[0]).equals(wanted),"Interior ring selects five parallel edges across the grid");
            capture(dock,output.resolve("human-grid-ring.png"));
            ((JComboBox<?>)field(dock,"edgeMode")).setSelectedItem(EditorMeshEdges.Mode.LOOP);button(dock,"selectEdges");
        });
        awaitEdges(dock);
        edt(()->{
            var wanted=before.vertices().stream().filter(v->v.z()==0).map(EditorTopologyProject.Vertex::id).collect(java.util.stream.Collectors.toSet());
            check(call(dock,"selectedIds",new Class<?>[0]).equals(wanted),"Interior loop selects the five vertices in the original row");
            capture(dock,output.resolve("human-grid-loop.png"));tool((JDialog)field(dock,"dialog"),"Transform").doClick();
            ((JComboBox<?>)field(dock,"transformKind")).setSelectedItem(EditorTopologyProject.Transform.SCALE_UNIFORM);
            ((JSpinner)field(dock,"transformAmount")).setValue(1.125);button(dock,"previewTransform");
        });
        await(()->reviewed(dock));
        edt(()->{
            button(dock,"applyFaceEdit");var after=project.snapshot();
            for(int i=0;i<before.vertices().size();i++){var v=before.vertices().get(i);var w=after.vertices().get(i);if(v.z()==0)check(Math.abs(w.x()-v.x()*1.125)<1e-14&&w.z()==0&&w.y()==v.y(),"Selected row scales: before="+v+", after="+w);else check(w.equals(v),"Every other grid vertex is unchanged");}
            button(dock,"undoMesh");check(project.snapshot().equals(before),"One undo restores complete interior grid");
            tool((JDialog)field(dock,"dialog"),"Select").doClick();
        });
        await(()->ready(dock));
    }
    public static void main(String[] args) throws Exception {
        Path root=Files.createTempDirectory("native-boundary-");
        Path source=root.resolve("ring.obj"); Files.writeString(source,RING);
        Path output=Path.of("build/editor-agent-evidence").toAbsolutePath(); Files.createDirectories(output);
        String previous=System.getProperty("user.home"); System.setProperty("user.home",root.toString());
        var ui=new AtomicReference<AssetImportDialog>(); var published=new AtomicReference<String>();
        try {
            edt(()->{var dock=new AssetImportDialog(null,published::set);ui.set(dock);((JDialog)field(dock,"dialog")).setVisible(true);});
            var dock=ui.get();verifyInteriorGrid(dock,root,output);edt(()->dock.loadSource(source));await(()->ready(dock)); var project=(EditorTopologyProject)field(dock,"topologyProject");
            var before=project.snapshot();long epoch=project.revision();check(before.quads().size()==4,"Four source OBJ quads retained in native editor");var inner=new HashSet<Long>();
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
                check(((JButton)field(dock,"selectEdges")).isShowing(),"Boundary action visible in Select inspector");((JComboBox<?>)field(dock,"edgeMode")).setSelectedItem(EditorMeshEdges.Mode.RING);button(dock,"selectEdges");
            });
            await(()->{try{return (boolean)call(dock,"hasEdgeSelection",new Class<?>[0]);}catch(Exception e){throw new RuntimeException(e);}});
            edt(()->{
                var segments=(List<?>)field(dock,"edgeSegments");check(segments.size()==2,"Ring reaches opposite outside edge across exactly one authored quad");
                var expected=before.vertices().stream().filter(v->v.y()<0 && v.part().equals(before.faces().get(0).part())).map(EditorTopologyProject.Vertex::id).collect(java.util.stream.Collectors.toSet());
                check(call(dock,"selectedIds",new Class<?>[0]).equals(expected),"Ring uses the exact four bottom rim endpoints");
                capture(dock,output.resolve("human-quad-ring.png"));
                ((JComboBox<?>)field(dock,"edgeMode")).setSelectedItem(EditorMeshEdges.Mode.LOOP);button(dock,"selectEdges");
            });
            await(()->{try{return (boolean)call(dock,"hasEdgeSelection",new Class<?>[0]);}catch(Exception e){throw new RuntimeException(e);}});
            edt(()->{
                check(call(dock,"selectedIds",new Class<?>[0]).equals(inner),"Quad loop continues along the four surrounding faces");
                capture(dock,output.resolve("human-quad-loop.png"));
                ((JComboBox<?>)field(dock,"edgeMode")).setSelectedItem(EditorMeshEdges.Mode.BOUNDARY);button(dock,"selectEdges");
            });
            await(()->{try{return (boolean)call(dock,"hasEdgeSelection",new Class<?>[0]);}catch(Exception e){throw new RuntimeException(e);}});
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
                check(!(boolean)call(dock,"hasEdgeSelection",new Class<?>[0]),"Old boundary observation invalid after commit");
                button(dock,"undoMesh");check(project.snapshot().equals(before),"One undo restores entire hole");button(dock,"redoMesh");button(dock,"saveMesh");
                check(EditorTopologyProject.open(project.documentPath()).snapshot().equals(moved),"Fresh open retains edited hole");
            });
            await(()->ready(dock));edt(()->button(dock,"apply"));await(()->published.get()!=null);
            var game=GlbProps.model(published.get()).triangles();var nativeMesh=project.mesh().triangles();check(game.size()==nativeMesh.size(),"Game triangle count");
            for(int i=0;i<game.size();i++){var a=game.get(i);var b=nativeMesh.get(i);var got=List.of(a.a(),a.b(),a.c());var want=List.of(b.a(),b.b(),b.c());for(int j=0;j<3;j++)check(got.get(j).x()==want.get(j).x()&&got.get(j).y()==want.get(j).y()&&got.get(j).z()==want.get(j).z(),"Every published game corner matches native ring");}
            byte[] outputGlb=ImportedAssetLibrary.read(published.get());
            check(EditorGlbReader.read(outputGlb).mesh("publication.glb").quads().equals(before.quads()),"Published source quad boundaries match the edited native project");
            Files.write(output.resolve("human-authored-quads.glb"),outputGlb);
            Files.write(output.resolve("human-authored-quads.btop"),project.snapshotBytes());
            Files.writeString(output.resolve("human-authored-quads.obj"),RING);
            System.out.println("PASS native interior quad loop/ring and boundary selection: exact grid transform/undo, directed hole rim, separate parts, native reopen and actual game corners");
        } finally {
            edt(()->{if(ui.get()!=null)((JDialog)field(ui.get(),"dialog")).dispose();});System.setProperty("user.home",previous);
            try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
        }
    }
}
