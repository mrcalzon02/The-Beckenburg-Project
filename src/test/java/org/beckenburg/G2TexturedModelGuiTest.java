package org.beckenburg;

import static org.beckenburg.ModelEditorGuiTestSupport.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.beckenburg.editor.*;

/** One asset through both human workspaces, native history, interchange and the game registry. */
public final class G2TexturedModelGuiTest {
    private final Path output;
    private final AtomicReference<String> published = new AtomicReference<>();
    private AssetImportDialog ui;
    private EditorTopologyProject project;

    private G2TexturedModelGuiTest(Path output) { this.output = output; }
    private interface Action { void run() throws Exception; }
    private static void edt(Action action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try { action.run(); } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
    private void spin(String name, Number value) throws Exception {
        ((JSpinner)field(ui,name)).setValue(value);
    }
    private void select(String name, int index) throws Exception {
        ((JComboBox<?>)field(ui,name)).setSelectedIndex(index);
    }
    private void applyReview() throws Exception {
        await(() -> reviewed(ui));
        edt(() -> {
            var candidate = (EditorTopologyProject.EditPreview)field(ui,"modelingPreview");
            button(ui,"applyFaceEdit");
            check(project.snapshot()==candidate.snapshot(),"Apply must install the reviewed candidate");
        });
        await(() -> ready(ui));
    }
    private void transform(EditorTopologyProject.Transform kind, int axis, double amount) throws Exception {
        edt(() -> {
            var parts = (JList<?>)field(ui,"partList");
            parts.clearSelection();
            parts.setSelectedIndex(0);
            tool((JDialog)field(ui,"dialog"),"Transform").doClick();
            ((JComboBox<?>)field(ui,"transformKind")).setSelectedItem(kind);
            select("transformSpace",0);
            select("transformAxis",axis);
            select("transformPivot",1);
            spin("transformAmount",amount);
            button(ui,"previewTransform");
        });
        applyReview();
    }
    private void create(Path obj) throws Exception {
        edt(() -> {
            ui = new AssetImportDialog(null,published::set);
            ((JDialog)field(ui,"dialog")).setVisible(true);
            button(ui,"newMesh");
        });
        await(() -> {
            try { return ((JButton)field(ui,"previewPrimitive")).isEnabled(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        edt(() -> {
            tool((JDialog)field(ui,"dialog"),"Create").doClick();
            spin("primitiveWidth",2d);
            spin("primitiveDepth",3d);
            spin("primitiveRings",1);
            button(ui,"previewPrimitive");
            var empty = (EditorTopologyProject)field(ui,"topologyProject");
            check(empty.snapshot().faces().isEmpty(),"Primitive preview changed the native scene");
            button(ui,"applyPrimitive");
        });
        await(() -> ready(ui));
        edt(() -> {
            button(ui,"saveMesh");
            check(ui.exportObj(obj),"Export created box as OBJ");
            ui.loadSource(obj);
        });
        await(() -> ready(ui));
        edt(() -> {
            project = (EditorTopologyProject)field(ui,"topologyProject");
            var state = project.snapshot();
            check(state.vertices().size()==8 && state.faces().size()==12 && state.quads().size()==6,
                "OBJ import must retain the box topology");
            check(project.topology().manifold() && project.topology().boundaryEdges()==0,"Expected a closed box");
        });
    }
    private void texture(Path png) throws Exception {
        int[] colors = {0xffff0000,0xff00ff00,0xff0000ff,0xffffff00};
        var image = new BufferedImage(4,4,BufferedImage.TYPE_INT_ARGB);
        for (int y=0;y<4;y++) for (int x=0;x<4;x++) image.setRGB(x,y,colors[2*(y/2)+x/2]);
        ImageIO.write(image,"png",png.toFile());
        edt(() -> ui.openMaterials());
        for (int i=0;i<12;i++) {
            final int face = i;
            edt(() -> {
                var faces = (JList<?>)field(field(ui,"materialWorkspace"),"faceList");
                faces.clearSelection();
                faces.setSelectedIndex(face);
                if (face==0) {
                    ((JComboBox<?>)field(ui,"materialOrigin")).setSelectedItem(EditorMeshMaterial.Origin.TOP_LEFT);
                    ui.previewPngMaterial(png);
                } else {
                    select("materialList",0);
                    button(ui,"previewMaterial");
                }
            });
            applyReview();
        }
        await(() -> {
            try { return !(boolean)field(ui,"materialRendering") && ((JLabel)field(ui,"materialRuntime")).getIcon()!=null; }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        edt(() -> {
            var window = (JDialog)field(ui,"materialDialog");
            var pane = window.getContentPane();
            var shot = new BufferedImage(pane.getWidth(),pane.getHeight(),BufferedImage.TYPE_INT_ARGB);
            var graphics = shot.createGraphics();
            try { pane.printAll(graphics); } finally { graphics.dispose(); }
            ImageIO.write(shot,"png",output.resolve("materials.png").toFile());
            window.dispatchEvent(new WindowEvent(window,WindowEvent.WINDOW_CLOSING));
        });
        var state = project.snapshot();
        check(state.materials().size()==1 && Arrays.equals(state.materials().get(0).png(),Files.readAllBytes(png)),
            "Captured original PNG bytes");
        check(state.faces().stream().allMatch(f -> f.material().equals(state.materials().get(0).key())),
            "All native faces must use the selected material");
    }
    private static void near(double actual,double expected) {
        check(Math.abs(actual-expected)<1e-5,"Expected "+expected+", got "+actual);
    }
    private void verifyRuntime(EditorTopologyProject.State original) {
        var model = GlbProps.model(published.get());
        check(model.triangles().size()==original.faces().size(),"Game triangle count");
        var points = new HashMap<Long,EditorTopologyProject.Vertex>();
        original.vertices().forEach(v -> points.put(v.id(),v));
        double cos=Math.cos(Math.PI/6), sin=Math.sin(Math.PI/6);
        for (int i=0;i<original.faces().size();i++) {
            var face=original.faces().get(i);
            var triangle=model.triangles().get(i);
            var actual=List.of(triangle.a(),triangle.b(),triangle.c());
            var corners=List.of(face.a(),face.b(),face.c());
            for (int j=0;j<3;j++) {
                var out=actual.get(j);
                var corner=corners.get(j);
                var in=points.get(corner.vertex());
                near(out.x(),1.5*(cos*in.x()+sin*in.z()));
                near(out.y(),in.y());
                near(out.z(),cos*in.z()-sin*in.x());
                near(out.u(),corner.u());
                near(out.v(),corner.v());
                var n=corner.normal();
                double x=(cos*n.x()+sin*n.z())/1.5, y=n.y(), z=cos*n.z()-sin*n.x();
                double length=Math.sqrt(x*x+y*y+z*z);
                near(out.nx(),x/length);
                near(out.ny(),y/length);
                near(out.nz(),z/length);
                check(out.x()*out.nx()+out.y()*out.ny()+out.z()*out.nz()>0,"Game normal faces outward");
            }
            var a=triangle.a();
            var b=triangle.b();
            var c=triangle.c();
            double ux=b.x()-a.x(), uy=b.y()-a.y(), uz=b.z()-a.z();
            double vx=c.x()-a.x(), vy=c.y()-a.y(), vz=c.z()-a.z();
            double nx=uy*vz-uz*vy, ny=uz*vx-ux*vz, nz=ux*vy-uy*vx;
            double area=Math.sqrt(nx*nx+ny*ny+nz*nz);
            near(nx/area,a.nx());
            near(ny/area,a.ny());
            near(nz/area,a.nz());
            check(GlbProps.sampleRgba(triangle.material(),.25,.25)==0xffff0000 &&
                  GlbProps.sampleRgba(triangle.material(),.25,.75)==0xff0000ff,"Game texture and UV origin");
        }
    }
    private void run() throws Exception {
        Path obj=output.resolve("created.obj"), png=output.resolve("checker.png");
        create(obj);
        byte[] originalObj=Files.readAllBytes(obj);
        texture(png);
        var textured=project.snapshot();
        transform(EditorTopologyProject.Transform.ROTATE,1,30);
        var rotated=project.snapshot();
        transform(EditorTopologyProject.Transform.SCALE_AXIS,0,1.5);
        var edited=project.snapshot();
        edt(() -> button(ui,"undoMesh"));
        await(() -> ready(ui));
        check(project.snapshot().equals(rotated),"Undo restores the complete rotated mesh");
        edt(() -> button(ui,"redoMesh"));
        await(() -> ready(ui));
        check(project.snapshot().equals(edited),"Redo restores the complete scaled mesh");
        var bundle=output.resolve("edited.obj.zip");
        var glb=output.resolve("edited.glb");
        edt(() -> {
            button(ui,"saveMesh");
            check(ui.exportObj(bundle) && ui.exportGlb(glb),"Human interchange export");
            capture(ui,output.resolve("model.png"));
            button(ui,"apply");
        });
        await(() -> published.get()!=null);
        EditorObjBundle.validate(edited,Files.readAllBytes(bundle));
        EditorGlbWriter.validate(project.mesh(),Files.readAllBytes(glb));
        verifyRuntime(textured);
        var archive=ImportedAssetLibrary.root().resolve(published.get()+".authoring.btop");
        check(EditorTopologyProject.open(archive).snapshot().equals(edited),"Published native archive");
        Files.copy(archive,output.resolve("edited.btop"),StandardCopyOption.REPLACE_EXISTING);
        check(Arrays.equals(originalObj,Files.readAllBytes(obj)),"Original OBJ changed");
        var renderer=new SceneRenderer();
        renderer.setRenderSize(640,400);
        renderer.x=SceneRenderer.devAssetX(0)-1.8;
        renderer.z=SceneRenderer.devAssetZ(0)-3.5;
        renderer.eyeHeight=1.5;
        renderer.yaw=Math.atan2(1.8,3.5);
        renderer.pitch=-.08;
        renderer.lamp=false;
        renderer.drawDevAssetRoom(List.of(new DevAssetReviewCatalog.Entry(DevAssetReviewCatalog.Kind.IMPORTED,published.get())),0,.4);
        ImageIO.write(renderer.image(),"png",output.resolve("game.png").toFile());
        for (Path source : List.of(bundle,glb,output.resolve("edited.btop"))) {
            edt(() -> ui.loadSource(source));
            await(() -> ready(ui));
            edt(() -> {
                var imported=(EditorTopologyProject)field(ui,"topologyProject");
                check(imported!=project && imported.snapshot().faces().size()==12 &&
                    imported.snapshot().quads().size()==6,"Editor import of "+source);
                if (source.toString().endsWith(".btop")) check(imported.snapshot().equals(edited),"Native reopen");
            });
        }
        System.out.println("PASS G2 textured box: two workspaces, 8 vertices, 6 quads, 12 triangles, "
            +"36 runtime corners with transformed hard normals and UVs, undo/redo, OBJ bundle/GLB/native reopen");
    }
    public static void main(String[] args) throws Exception {
        Path home=Files.createTempDirectory("g2-textured-");
        var oldHome=System.getProperty("user.home");
        var test=new G2TexturedModelGuiTest(Files.createDirectories(Path.of("build/editor-agent-evidence/g2-textured-roundtrip").toAbsolutePath()));
        System.setProperty("user.home",home.toString());
        try { test.run(); }
        finally {
            edt(() -> { if (test.ui!=null) ((JDialog)field(test.ui,"dialog")).dispose(); });
            System.setProperty("user.home",oldHome);
            try (var paths=Files.walk(home)) {
                for (var path:paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
}
