package org.beckenburg;

import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.beckenburg.editor.*;

import static org.beckenburg.ModelEditorGuiTestSupport.*;

/** Actual bevel inspector, visible native picking, transactions, archives and game readback. */
public final class AssetImportBevelGuiTest {
    private static EditorMeshPicking.Hit verifyHover(AssetImportDialog ui, EditorTopologyProject project,
                                                     JPanel canvas, int x, int y) throws Exception {
        var before = project.snapshot();
        long revision = project.revision();
        Object selected = field(ui, "selectedHit");
        var move = new MouseEvent(canvas, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, x, y, 0, false);
        canvas.dispatchEvent(move);
        var hit = (EditorMeshPicking.Hit) field(ui, "hoverHit");
        var prepared = field(ui, "preparedPicking");
        canvas.dispatchEvent(move);
        check(prepared != null && field(ui, "preparedPicking") == prepared, "Hover reuses picking data for the same native state and camera");
        check(project.snapshot() == before && project.revision() == revision && field(ui, "selectedHit") == selected,
                "Hover preserves native geometry, revision and committed selection");
        if (hit != null) {
            var image = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics();
            try { canvas.paint(graphics); } finally { graphics.dispose(); }
            boolean highlighted = false;
            for (int py = 0; py < image.getHeight() && !highlighted; py++) {
                for (int px = 0; px < ui.workingViewportWidth(); px++) {
                    if ((image.getRGB(px, py) & 0xffffff) == 0x5adcff) { highlighted = true; break; }
                }
            }
            check(highlighted, "Working canvas paints the hovered native component");
            String mode = String.valueOf(((JComboBox<?>) field(ui, "selectionMode")).getSelectedItem());
            capture(ui, Path.of("build/editor-agent-evidence/human-hover-" + mode + ".png"));
        }
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, -1, -1, 0, false));
        check(field(ui, "hoverHit") == null, "Leaving the working canvas clears preselection");
        return hit;
    }
    private static void pick(AssetImportDialog ui,EditorTopologyProject p,boolean corner)throws Exception{
        ((JComboBox<?>)field(ui,"selectionMode")).setSelectedItem(corner?"VERTEX":"EDGE");var reference=call(ui,"workingReference",new Class<?>[0]);var camera=(EditorMeshPicking.Projector)call(ui,"projector",new Class<?>[]{List.class},reference);var view=call(ui,"viewport",new Class<?>[]{List.class,int.class,int.class},reference,0,ui.workingViewportWidth());var canvas=(JPanel)field(ui,"preview");
        for(var t:p.mesh().triangles()){var vertices=List.of(t.a(),t.b(),t.c());double cx=(t.a().x()+t.b().x()+t.c().x())/3,cy=(t.a().y()+t.b().y()+t.c().y())/3,cz=(t.a().z()+t.b().z()+t.c().z())/3;
            for(int i=0;i<3;i++){var a=vertices.get(i);var b=vertices.get((i+1)%3);double x=corner?a.x():(a.x()+b.x())/2,y=corner?a.y():(a.y()+b.y())/2,z=corner?a.z():(a.z()+b.z())/2;var projected=camera.project(x*.97+cx*.03,y*.97+cy*.03,z*.97+cz*.03);int px=(Integer)call(view,"x",new Class<?>[]{double.class},projected.x()),py=(Integer)call(view,"y",new Class<?>[]{double.class},projected.y());
                var mode=(JComboBox<?>)field(ui,"selectionMode");mode.setSelectedItem("FACE");var faceHover=verifyHover(ui,p,canvas,px,py);
                mode.setSelectedItem(corner?"VERTEX":"EDGE");var hovered=verifyHover(ui,p,canvas,px,py);
                canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,px,py,1,false,MouseEvent.BUTTON1));canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_RELEASED,System.currentTimeMillis(),0,px,py,1,false,MouseEvent.BUTTON1));var hit=(EditorMeshPicking.Hit)field(ui,"selectedHit");if(hit==null)continue;try{var probe=p.previewBevel(corner?hit.vertexId():hit.edgeA(),corner?hit.vertexId():hit.edgeB(),hit.faceId(),.2);p.cancelPreview(probe);check(hit.equals(hovered)&&faceHover!=null&&faceHover.faceId()==hit.faceId(),"Face and component hover match the real perspective click");check(((JButton)field(ui,"previewBevel")).isEnabled(),"Actual native pick enables visible bevel control");return;}catch(IllegalArgumentException refused){}
            }
        }throw new AssertionError("No visible native convex edge/corner selected");
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("human-bevel-");String previous=System.getProperty("user.home");System.setProperty("user.home",root.toString());Path output=Path.of("build/editor-agent-evidence").toAbsolutePath();Files.createDirectories(output);Path source=root.resolve("box.glb");var fixture=EditorTopologyProject.fromMesh(EditorMeshPrimitives.preview(new EditorMeshPrimitives.Parameters(EditorMeshPrimitives.Kind.BOX,2,2,2,12,1)),"0".repeat(64),root.resolve("fixture.btop"));var mesh=fixture.mesh();byte[] original=EditorGlbWriter.encode(new EditorMeshPipeline.Mesh(mesh.name(),mesh.triangles(),0,mesh.cornerVertexIds(),mesh.materials(),null,mesh.faceIds(),Map.of(fixture.snapshot().vertices().get(0).part(),EditorMeshFrame.IDENTITY)));Files.write(source,original);
        var ui=new AtomicReference<AssetImportDialog>();var published=new AtomicReference<String>();var project=new AtomicReference<EditorTopologyProject>();var initial=new AtomicReference<EditorTopologyProject.State>();var committed=new AtomicReference<EditorTopologyProject.State>();
        try{
            SwingUtilities.invokeAndWait(()->{var dock=new AssetImportDialog(null,published::set);ui.set(dock);try{((JDialog)field(dock,"dialog")).setVisible(true);dock.loadSource(source);}catch(Exception e){throw new RuntimeException(e);}});await(()->ready(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();project.set((EditorTopologyProject)field(dock,"topologyProject"));initial.set(project.get().snapshot());((JComboBox<?>)field(dock,"meshView")).setSelectedItem(EditorMeshPicking.Projection.PERSPECTIVE);tool((JDialog)field(dock,"dialog"),"Bevel").doClick();pick(dock,project.get(),false);((JSpinner)field(dock,"bevelDepth")).setValue(.2);check(((JSpinner)field(dock,"bevelDepth")).isShowing(),"Bevel owns actual model inspector card");button(dock,"previewBevel");}catch(Exception e){throw new RuntimeException(e);}});await(()->reviewed(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();check(project.get().snapshot()==initial.get(),"Visible bevel preview leaves authoritative native scene unchanged");capture(dock,output.resolve("human-edge-bevel-preview.png"));((JSpinner)field(dock,"bevelDepth")).setValue(.15);check(field(dock,"modelingPreview")==null,"Depth change invalidates reviewed bevel");((JSpinner)field(dock,"bevelDepth")).setValue(.2);button(dock,"previewBevel");}catch(Exception e){throw new RuntimeException(e);}});await(()->reviewed(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();button(dock,"cancelFaceEdit");check(project.get().snapshot()==initial.get(),"Actual bevel Cancel preserves native state");button(dock,"previewBevel");}catch(Exception e){throw new RuntimeException(e);}});await(()->reviewed(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();var candidate=(EditorTopologyProject.EditPreview)field(dock,"modelingPreview");button(dock,"applyFaceEdit");committed.set(project.get().snapshot());check(committed.get()==candidate.snapshot()&&project.get().topology().boundaryEdges()==0,"Apply installs exact reviewed closed bevel");button(dock,"undoMesh");check(project.get().snapshot().equals(initial.get()),"Bevel one-step undo");button(dock,"redoMesh");check(project.get().snapshot().equals(committed.get()),"Bevel exact redo");button(dock,"saveMesh");button(dock,"reopenMesh");check(EditorTopologyProject.open(project.get().documentPath()).snapshot().equals(committed.get()),"Native save/fresh reopen exact bevel");capture(dock,output.resolve("human-edge-bevel-model.png"));}catch(Exception e){throw new RuntimeException(e);}});await(()->ready(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();button(dock,"undoMesh");pick(dock,project.get(),true);button(dock,"previewBevel");}catch(Exception e){throw new RuntimeException(e);}});await(()->reviewed(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();capture(dock,output.resolve("human-corner-bevel-preview.png"));button(dock,"applyFaceEdit");check(project.get().topology().manifold()&&project.get().topology().boundaryEdges()==0,"Actual corner bevel closed native topology");button(dock,"saveMesh");}catch(Exception e){throw new RuntimeException(e);}});await(()->ready(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{button(ui.get(),"apply");}catch(Exception e){throw new RuntimeException(e);}});await(()->published.get()!=null);var expected=project.get().mesh();byte[] glb=ImportedAssetLibrary.read(published.get());EditorGlbWriter.validate(expected,glb);var actual=GlbProps.model(published.get()).triangles();check(actual.size()==expected.triangles().size(),"Actual game consumes all beveled triangles");for(int i=0;i<actual.size();i++){var t=expected.triangles().get(i);var a=actual.get(i);var wanted=List.of(t.a(),t.b(),t.c());var got=List.of(a.a(),a.b(),a.c());for(int j=0;j<3;j++){var v=wanted.get(j);var w=got.get(j);check(w.x()==(double)(float)v.x()&&w.y()==(double)(float)v.y()&&w.z()==(double)(float)v.z()&&w.u()==(double)(float)v.u()&&w.v()==(double)(float)v.v(),"Every actual game beveled position/UV/winding matches native state");}}
            check(EditorTopologyProject.open(ImportedAssetLibrary.root().resolve(published.get()+".authoring.btop")).snapshot().equals(project.get().snapshot()),"Published native archive exact bevel");check(Arrays.equals(original,Files.readAllBytes(source))&&Arrays.equals(original,Files.readAllBytes(ImportedAssetLibrary.root().resolve(published.get()+".source.glb"))),"Source and archived original unchanged");var renderer=new SceneRenderer();renderer.setRenderSize(640,400);renderer.x=SceneRenderer.devAssetX(0)-1.8;renderer.z=SceneRenderer.devAssetZ(0)-3.5;renderer.eyeHeight=1.5;renderer.yaw=Math.atan2(1.8,3.5);renderer.pitch=-.08;renderer.lamp=false;renderer.drawDevAssetRoom(List.of(new DevAssetReviewCatalog.Entry(DevAssetReviewCatalog.Kind.IMPORTED,published.get())),0,.4);ImageIO.write(renderer.image(),"png",output.resolve("human-bevel-runtime.png").toFile());Files.write(output.resolve("human-bevel-runtime.glb"),glb);Files.write(output.resolve("human-bevel-source.glb"),original);Files.copy(project.get().documentPath(),output.resolve("human-bevel-model.btop"),StandardCopyOption.REPLACE_EXISTING);System.out.println("PASS actual human bevel: visible native edge/corner picking, separate tool card, preview/cancel/parameter/apply, shared undo/redo/save/reopen and every real game corner/source archive");
        }finally{SwingUtilities.invokeAndWait(()->{if(ui.get()!=null)try{((JDialog)field(ui.get(),"dialog")).dispose();}catch(Exception e){throw new RuntimeException(e);}});System.setProperty("user.home",previous);try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
}
