package org.beckenburg;

import java.awt.event.MouseEvent;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.beckenburg.editor.*;
import static org.beckenburg.ModelEditorGuiTestSupport.*;

/** Slide a real OBJ interior vertex and verify the outline, attributes and published game mesh. */
public final class AssetImportSlideGuiTest {
    private interface Step { void run() throws Exception; }
    private static void edt(Step action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try { action.run(); } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
    private static void refuse(Step action) throws Exception {
        try { action.run(); throw new AssertionError("Expected invalid slide refusal"); }
        catch (IllegalArgumentException expected) { }
    }
    private static long vertex(EditorTopologyProject.State state, double x, double y) {
        return state.vertices().stream().filter(v -> v.x()==x && v.y()==y).findFirst().orElseThrow().id();
    }
    public static void main(String[] args) throws Exception {
        Path root=Files.createTempDirectory("human-slide-");
        Path output=Path.of("build/editor-agent-evidence").toAbsolutePath();
        Files.createDirectories(output);
        Path source=root.resolve("panel.obj");
        Files.writeString(source,"o panel\nv 0 0 0\nv 1 0 0\nv 0 1 0\nv -1 0 0\nv 0 -1 0\nvt .5 .5\nvt 1 .5\nvt .5 1\nvt 0 .5\nvt .5 0\nvn 0 0 2\nf 1/1/1 2/2/1 3/3/1\nf 1/1/1 3/3/1 4/4/1\nf 1/1/1 4/4/1 5/5/1\nf 1/1/1 5/5/1 2/2/1\n");
        byte[] original=Files.readAllBytes(source);
        String home=System.getProperty("user.home");System.setProperty("user.home",root.toString());
        var ui=new AtomicReference<AssetImportDialog>();var published=new AtomicReference<String>();
        try {
            edt(()->{var dock=new AssetImportDialog(null,published::set);ui.set(dock);((JDialog)field(dock,"dialog")).setVisible(true);dock.loadSource(source);});
            var dock=ui.get();await(()->ready(dock));
            var project=(EditorTopologyProject)field(dock,"topologyProject");var initial=project.snapshot();
            long center=vertex(initial,0,0), east=vertex(initial,1,0), west=vertex(initial,-1,0);
            refuse(()->project.previewVertexSlide(east,center,.25));
            refuse(()->project.previewVertexSlide(east,west,.25));
            refuse(()->project.previewVertexSlide(center,east,1));
            var cube=EditorTopologyProject.fromMesh(EditorMeshPrimitives.preview(new EditorMeshPrimitives.Parameters(EditorMeshPrimitives.Kind.BOX,2,2,2,12,1)),"0".repeat(64),root.resolve("box.btop"));
            var side=cube.snapshot().faces().get(0);refuse(()->cube.previewVertexSlide(side.a().vertex(),side.b().vertex(),.25));
            edt(()->{
                ((JComboBox<?>)field(dock,"meshView")).setSelectedItem(EditorMeshPicking.Projection.FRONT);
                ((JComboBox<?>)field(dock,"selectionMode")).setSelectedItem("EDGE");
                tool((JDialog)field(dock,"dialog"),"Move").doClick();
                var reference=call(dock,"workingReference",new Class<?>[0]);
                var view=call(dock,"viewport",new Class<?>[]{List.class,int.class,int.class},reference,0,dock.workingViewportWidth());
                var point=EditorMeshPicking.Projection.FRONT.project(.5,.015,0);
                int x=(Integer)call(view,"x",new Class<?>[]{double.class},point.x());
                int y=(Integer)call(view,"y",new Class<?>[]{double.class},point.y());
                var canvas=(JPanel)field(dock,"preview");
                canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,x,y,1,false,1));
                var hit=(EditorMeshPicking.Hit)field(dock,"selectedHit");
                check(hit!=null && Set.of(hit.edgeA(),hit.edgeB()).equals(Set.of(center,east)),"Visible click picks intended slide edge");
                if(hit.edgeA()!=center)((JCheckBox)field(dock,"reverseSlide")).doClick();
                check(((JButton)field(dock,"previewSlide")).isShowing(),"Slide has visible controls in Move inspector");
                button(dock,"previewSlide");
            });
            await(()->reviewed(dock));
            edt(()->{capture(dock,output.resolve("human-slide-preview.png"));button(dock,"cancelFaceEdit");check(project.snapshot()==initial,"Cancel preserves exact native state");button(dock,"previewSlide");});
            await(()->reviewed(dock));
            edt(()->{
                button(dock,"applyFaceEdit");var moved=project.snapshot();
                for(int i=0;i<initial.vertices().size();i++){var old=initial.vertices().get(i);var now=moved.vertices().get(i);if(old.id()==center)check(now.x()==.25&&now.y()==0&&now.z()==0,"Center slides along connected edge");else check(now.equals(old),"Every outline vertex stays exact");}
                check(moved.faces().equals(initial.faces()),"Faces, IDs, raw normals and UVs survive slide");
                button(dock,"undoMesh");check(project.snapshot().equals(initial),"One undo restores slide");
                button(dock,"redoMesh");button(dock,"saveMesh");button(dock,"reopenMesh");
                check(EditorTopologyProject.open(project.documentPath()).snapshot().equals(moved),"Fresh native reopen retains slide");
            });
            await(()->ready(dock));edt(()->button(dock,"apply"));await(()->published.get()!=null);
            var actual=GlbProps.model(published.get()).triangles();var expected=project.mesh().triangles();
            check(actual.size()==expected.size(),"Published game retains topology count");
            for(int i=0;i<expected.size();i++){var t=expected.get(i);var r=actual.get(i);var wanted=List.of(t.a(),t.b(),t.c());var got=List.of(r.a(),r.b(),r.c());for(int j=0;j<3;j++){var a=wanted.get(j);var b=got.get(j);check(a.x()==b.x()&&a.y()==b.y()&&a.z()==b.z()&&a.u()==b.u()&&a.v()==b.v(),"Every runtime slide position and UV matches native mesh");}}
            check(Arrays.equals(original,Files.readAllBytes(source)),"Original source remains exact");
            var archived = ImportedAssetLibrary.root().resolve(published.get()+".authoring.btop");
            check(EditorTopologyProject.open(archived).snapshot().equals(project.snapshot()), "Published archive retains exact native slide");
            Files.copy(archived, output.resolve("human-slide.btop"), StandardCopyOption.REPLACE_EXISTING);
            Files.write(output.resolve("human-slide-runtime.glb"), ImportedAssetLibrary.read(published.get()));
            Files.write(output.resolve("human-slide-source.obj"), original);
            System.out.println("PASS actual vertex slide: native edge pick, surface/outline refusal, preview/cancel/apply, one-step history, exact native reopen and every game corner");
        } finally {
            edt(()->{if(ui.get()!=null)((JDialog)field(ui.get(),"dialog")).dispose();});
            System.setProperty("user.home",home);
            try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
        }
    }
}
