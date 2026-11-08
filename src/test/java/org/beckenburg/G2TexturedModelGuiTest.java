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
            var original=new AtomicReference<EditorMeshPipeline.Mesh>();
            var graph=new AtomicReference<EditorMeshTopology>();
            edt(() -> {
                original.set((EditorMeshPipeline.Mesh)field(ui,"source"));
                graph.set(((EditorTopologyProject.Observation)field(ui,"workingObservation")).topology());
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
            edt(() -> {
                var next=(EditorMeshPipeline.Mesh)field(ui,"source");
                check(next.equals(project.mesh()),"Incremental viewport must equal full native derivation");
                check(((EditorTopologyProject.Observation)field(ui,"workingObservation")).topology()==graph.get(),
                    "Material-only edits retain native adjacency");
                for (int j=0;j<next.triangles().size();j++) {
                    var prior=original.get().triangles().get(j);
                    var current=next.triangles().get(j);
                    check((prior==current)==(j!=face),"Only the newly bound face changes");
                    check(prior.a()==current.a() && prior.b()==current.b() && prior.c()==current.c(),
                        "Binding a material reuses the existing geometry corners");
                }
            });
        }
        await(() -> {
            try { return !(boolean)field(ui,"materialRendering") && ((JLabel)field(ui,"materialRuntime")).getIcon()!=null; }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        verifyVisibility();
        verifyPencil(png);
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
    private void verifyVisibility() throws Exception {
        var workspace=(EditorMaterialWorkspace)field(ui,"materialWorkspace");
        var original=project.snapshot();
        long revision=project.revision();
        var full=new AtomicReference<java.awt.Image>();
        edt(()->{
            full.set(((ImageIcon)((JLabel)field(ui,"materialRuntime")).getIcon()).getImage());
            ((JList<?>)field(workspace,"faceList")).setSelectedIndex(10);
            button(workspace,"isolate");
            check(workspace.visibleFaces()==1 && workspace.visibility().includes(original.faces().get(10).id()),
                "Isolate only the selected native face");
        });
        await(()->{try{return !(boolean)field(ui,"materialRendering") && ((JLabel)field(ui,"materialRuntime")).getIcon()!=null;}
            catch(Exception e){throw new RuntimeException(e);}});
        edt(()->{
            var isolated=(BufferedImage)((ImageIcon)((JLabel)field(ui,"materialRuntime")).getIcon()).getImage();
            var all=(BufferedImage)full.get();
            var pixels=isolated.getRGB(0,0,280,190,null,0,280);
            check(Arrays.stream(pixels).distinct().count()>2 && !Arrays.equals(pixels,all.getRGB(0,0,280,190,null,0,280)),
                "Isolated face must remain visible in the actual runtime image");
            ImageIO.write(isolated,"png",output.resolve("isolated-runtime.png").toFile());
            button(workspace,"hide");
            check(workspace.visibleFaces()==0 && field(ui,"selectedHit")==null &&
                ((JLabel)field(ui,"materialRuntime")).getIcon()==null,"Hide removes the last visible face");
            var rect=workspace.getClass().getDeclaredMethod("imageRect");
            rect.setAccessible(true);
            double[] bounds=(double[])rect.invoke(workspace);
            var canvas=(JPanel)field(workspace,"canvas");
            canvas.dispatchEvent(new java.awt.event.MouseEvent(canvas,java.awt.event.MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(),0,(int)(bounds[0]+.65*bounds[2]),(int)(bounds[1]+.3*bounds[3]),
                1,false,java.awt.event.MouseEvent.BUTTON1));
            check(field(ui,"selectedHit")==null,"Hidden overlapping UVs cannot be picked");
            button(workspace,"showAll");
            check(workspace.visibleFaces()==original.faces().size(),"Show all restores the native face list");
            check(original==project.snapshot() && project.revision()==revision,"Visibility changed native state or history");
        });
        await(()->{try{return !(boolean)field(ui,"materialRendering") && ((JLabel)field(ui,"materialRuntime")).getIcon()!=null;}
            catch(Exception e){throw new RuntimeException(e);}});
    }
    private void verifyPencil(Path sourcePng) throws Exception {
        var workspace=(EditorMaterialWorkspace)field(ui,"materialWorkspace");
        var original=project.snapshot();
        edt(()->{
            ((JList<?>)field(workspace,"faceList")).setSelectedIndex(10);
            var pencil=(JToggleButton)field(workspace,"pencil");
            check(pencil.isEnabled(),"Pencil must act on the selected native binding");
            pencil.doClick();
            var canvas=(JPanel)field(workspace,"canvas");
            double[] bounds=(double[])call(workspace,"imageRect",new Class<?>[0]);
            int y=(int)(bounds[1]+bounds[3]*.125);
            int[] events={java.awt.event.MouseEvent.MOUSE_PRESSED,java.awt.event.MouseEvent.MOUSE_DRAGGED,java.awt.event.MouseEvent.MOUSE_RELEASED};
            for(int i=0;i<events.length;i++)canvas.dispatchEvent(new java.awt.event.MouseEvent(canvas,events[i],
                System.currentTimeMillis(),i==2?0:java.awt.event.InputEvent.BUTTON1_DOWN_MASK,
                (int)(bounds[0]+bounds[2]*(i==0?.375:.875)),y,1,false,i==1?0:1));
        });
        await(()->reviewed(ui));
        check(project.snapshot()==original,"Pencil preview must leave the native state intact");
        applyReview();
        var painted=project.snapshot();
        var material=painted.materials().get(0);
        check(material.paintedTileCount()==1 && Arrays.equals(material.originalPng(),Files.readAllBytes(sourcePng)),
            "One stroke edits one tile and preserves source PNG");
        var composed=ImageIO.read(new java.io.ByteArrayInputStream(material.png()));
        for(int x=1;x<4;x++)check(composed.getRGB(x,0)==0xffff00ff,"Pencil interpolates each touched texel");
        verifyPaintRecovery(painted);
        edt(()->button(ui,"undoMesh"));
        await(()->ready(ui));
        check(project.snapshot().equals(original),"One undo removes the whole stroke");
        edt(()->button(ui,"redoMesh"));
        await(()->ready(ui));
        check(project.snapshot().equals(painted),"Redo restores the painted layer");
        verifyLayerStyle(workspace,painted);
        edt(()->((JList<?>)field(workspace,"faceList")).setSelectedIndex(10));
        await(()->{try{return !(boolean)field(ui,"materialRendering") && field(ui,"materialRenderState")==project.snapshot()
            && ((JLabel)field(ui,"materialRuntime")).getIcon()!=null;}catch(Exception e){throw new RuntimeException(e);}});
        edt(()->{
            var pane=((JDialog)field(ui,"materialDialog")).getContentPane();
            var shot=new BufferedImage(pane.getWidth(),pane.getHeight(),BufferedImage.TYPE_INT_ARGB);
            var graphics=shot.createGraphics();
            try{pane.printAll(graphics);}finally{graphics.dispose();}
            ImageIO.write(shot,"png",output.resolve("painted-workspace.png").toFile());
            button(ui,"saveMesh");
            check(ui.exportObj(output.resolve("painted.obj.zip")),"Painted OBJ bundle export");
            button(ui,"apply");
        });
        await(()->published.get()!=null);
        var archive=ImportedAssetLibrary.root().resolve(published.get()+".authoring.btop");
        check(EditorTopologyProject.open(archive).snapshot().equals(painted),"Published archive retains editable tiles");
        var game=GlbProps.model(published.get());
        check(GlbProps.sampleRgba(game.triangles().get(10).material(),.375,.125)==0xffff00ff,"Game samples painted texel");
        Files.copy(archive,output.resolve("painted.btop"),StandardCopyOption.REPLACE_EXISTING);
        Files.write(output.resolve("painted.png"),material.png());
        published.set(null);
        edt(()->button(ui,"undoMesh"));
        await(()->ready(ui));
        check(project.snapshot().equals(original),"Return fixture to the original textured state after publication");
    }
    private void verifyLayerStyle(EditorMaterialWorkspace workspace,EditorTopologyProject.State painted)throws Exception{
        edt(()->{
            ((JList<?>)field(workspace,"faceList")).setSelectedIndex(10);
            ((JTabbedPane)field(ui,"materialTools")).setSelectedIndex(2);
            ((JComboBox<?>)field(ui,"paintBlend")).setSelectedItem(EditorMeshMaterial.Blend.MULTIPLY);
            ((JSpinner)field(ui,"paintOpacity")).setValue(50);
            button(ui,"previewPaintStyle");
        });
        await(()->reviewed(ui));
        check(project.snapshot()==painted,"Style preview mutated the live layer");
        applyReview();
        var styled=project.snapshot();
        edt(()->((JList<?>)field(workspace,"faceList")).setSelectedIndex(10));
        await(()->{try{return !(boolean)field(ui,"materialRendering") && field(ui,"materialRenderState")==styled &&
            ((JLabel)field(ui,"materialRuntime")).getIcon()!=null;}catch(Exception e){throw new RuntimeException(e);}});
        edt(()->{
            var pane=((JDialog)field(ui,"materialDialog")).getContentPane();
            var shot=new BufferedImage(pane.getWidth(),pane.getHeight(),BufferedImage.TYPE_INT_ARGB);
            var graphics=shot.createGraphics();
            try{pane.printAll(graphics);}finally{graphics.dispose();}
            ImageIO.write(shot,"png",output.resolve("layer-settings.png").toFile());
            button(ui,"saveMesh");
            check(ui.exportObj(output.resolve("styled.obj.zip")),"Export editable layer settings");
            button(ui,"apply");
        });
        await(()->published.get()!=null);
        var game=GlbProps.model(published.get());
        check(GlbProps.sampleRgba(game.triangles().get(10).material(),.625,.125)==0xff008000,
            "Game must show 50% multiply of magenta over green");
        var file=ImportedAssetLibrary.root().resolve(published.get()+".authoring.btop");
        check(EditorTopologyProject.open(file).snapshot().equals(styled),"Layer settings lost in native archive");
        Files.copy(file,output.resolve("styled.btop"),StandardCopyOption.REPLACE_EXISTING);
        published.set(null);
        edt(()->button(ui,"undoMesh"));
        await(()->ready(ui));
        check(project.snapshot().equals(painted),"Undo style must retain original paint stroke");
    }
    private void verifyPaintRecovery(EditorTopologyProject.State painted)throws Exception{
        long revision=project.revision();
        await(()->{try{return ((JLabel)field(ui,"paintRecoveryStatus")).getText().equals("Paint recovery saved · revision "+revision);}
            catch(Exception e){throw new RuntimeException(e);}});
        var recovery=(EditorPaintRecovery)field(ui,"paintRecovery");
        Path file=recovery.file();
        byte[] durable=Files.readAllBytes(file);
        check(EditorTopologyProject.decode(durable).equals(painted),"Autosave captures the unsaved stroke");
        check(project.hasUnsavedChanges(),"Autosave must leave the manual document dirty");
        var fresh=new AtomicReference<AssetImportDialog>();
        try{
            edt(()->{
                var window=new AssetImportDialog(null,id->{});
                fresh.set(window);
                ((JDialog)field(window,"dialog")).setVisible(true);
                window.loadPaintRecovery(file);
            });
            await(()->ready(fresh.get()));
            edt(()->{
                var copy=(EditorTopologyProject)field(fresh.get(),"topologyProject");
                check(copy.snapshot().equals(painted) && !copy.documentPath().equals(file) &&
                    !copy.documentPath().equals(project.documentPath()),"Recovery opens an independent writable native copy");
                button(fresh.get(),"saveMesh");
            });
            check(Arrays.equals(durable,Files.readAllBytes(file)),"Saving the recovered copy changed the recovery file");
            Path invalid=Files.writeString(Path.of(System.getProperty("user.home"),"invalid-draft.btop"),"truncated");
            var current=(EditorTopologyProject)field(fresh.get(),"topologyProject");
            edt(()->fresh.get().loadPaintRecovery(invalid));
            await(()->ready(fresh.get()));
            edt(()->check(field(fresh.get(),"topologyProject")==current &&
                ((JLabel)field(fresh.get(),"paintRecoveryStatus")).getText().startsWith("Recovery refused"),
                "Bad recovery input must preserve the open editor document"));
        }finally{edt(()->{if(fresh.get()!=null)((JDialog)field(fresh.get(),"dialog")).dispose();});}
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
        var gltf=output.resolve("edited.gltf");
        edt(() -> {
            button(ui,"saveMesh");
            check(ui.exportObj(bundle) && ui.exportGlb(glb) && ui.exportGlb(gltf),"Human interchange export");
            capture(ui,output.resolve("model.png"));
            button(ui,"apply");
        });
        await(() -> published.get()!=null);
        EditorObjBundle.validate(edited,Files.readAllBytes(bundle));
        EditorGlbWriter.validate(project.mesh(),Files.readAllBytes(glb));
        EditorGltfFileTest.verify(project.mesh(),Files.readAllBytes(gltf),Files.readAllBytes(png));
        byte[] reviewedJson=Files.readAllBytes(gltf);
        try {
            project.exportGlb(project.observe(),gltf,"0".repeat(64));
            throw new AssertionError("Stale writer replaced JSON glTF");
        } catch (java.io.IOException expected) {
            check(Arrays.equals(reviewedJson,Files.readAllBytes(gltf)),"Failed export changed reviewed JSON");
        }
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
        var multi=output.resolve("multi-buffer.gltf");
        Files.write(multi,EditorGltfFileTest.splitBuffers(Files.readAllBytes(gltf)));
        EditorGlbWriter.validate(project.mesh(),EditorGltfFile.toGlb(Files.readAllBytes(multi)));
        var external=EditorGltfFileTest.externalFiles(output.resolve("external"),reviewedJson,Files.readAllBytes(png));
        for (Path source : List.of(bundle,glb,gltf,multi,external,output.resolve("edited.btop"))) {
            edt(() -> ui.loadSource(source));
            await(() -> ready(ui));
            edt(() -> {
                var imported=(EditorTopologyProject)field(ui,"topologyProject");
                check(imported!=project && imported.snapshot().faces().size()==12 &&
                    imported.snapshot().quads().size()==6,"Editor import of "+source);
                if (source.toString().endsWith(".btop")) check(imported.snapshot().equals(edited),"Native reopen");
            });
            if (source.equals(gltf) || source.equals(multi) || source.equals(external)) {
                published.set(null);
                if (source.equals(external)) {
                    var sidecar=source.resolveSibling("box data.bin");
                    byte[] saved=Files.readAllBytes(sidecar);
                    try {
                        Files.write(sidecar,new byte[]{1});
                        edt(() -> button(ui,"apply"));
                        await(() -> ready(ui));
                        edt(() -> check(((JTextArea)field(ui,"details")).getText().contains("sidecar changed") &&
                            published.get()==null,"Refuse changed glTF dependency"));
                    } finally { Files.write(sidecar,saved); }
                }
                edt(() -> button(ui,"apply"));
                await(() -> published.get()!=null);
                verifyRuntime(textured);
                check(Arrays.equals(Files.readAllBytes(source),Files.readAllBytes(
                    ImportedAssetLibrary.root().resolve(published.get()+".source.gltf"))),"Original JSON source archive");
                if (source.equals(external)) {
                    Path zip=ImportedAssetLibrary.root().resolve(published.get()+".source.zip");
                    Path restored=Files.createTempDirectory(Path.of(System.getProperty("user.home")),"restore-");
                    try (var packageZip=new java.util.zip.ZipFile(zip.toFile())) {
                        check(packageZip.size()==3,"Expected JSON, binary and texture in source archive");
                        for (String name:List.of("box.gltf","box data.bin","textures/checker.png")) {
                            try (var stream=packageZip.getInputStream(packageZip.getEntry(name))) {
                                byte[] bytes=stream.readAllBytes();
                                check(Arrays.equals(bytes,Files.readAllBytes(external.resolveSibling(name))),
                                    "Source package changed "+name);
                                Files.createDirectories(restored.resolve(name).getParent());
                                Files.write(restored.resolve(name),bytes);
                            }
                        }
                    }
                    Path restoredSource=restored.resolve("box.gltf");
                    var capture=new EditorGltfSource(restoredSource,Files.readAllBytes(restoredSource));
                    EditorGlbWriter.validate(project.mesh(),capture.glb());
                    check(Arrays.equals(Files.readAllBytes(zip),capture.reviewedArchive(Files.readAllBytes(restoredSource))),
                        "Restored source package must reproduce the reviewed archive");
                    Files.copy(zip,output.resolve("external-source.zip"),StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        System.out.println("PASS G2 textured box: two workspaces, 8 vertices, 6 quads, 12 triangles, "
            +"36 runtime corners with transformed hard normals and UVs, undo/redo, OBJ bundle/GLB/glTF/native reopen");
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
