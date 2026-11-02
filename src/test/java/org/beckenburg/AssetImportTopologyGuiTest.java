package org.beckenburg;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;

/** Real human import dock: read native source, review topology and publish runtime GLB. */
public final class AssetImportTopologyGuiTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Object field(Object owner,String name)throws Exception{
        var f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);
    }
    private static void await(java.util.function.BooleanSupplier done)throws Exception{
        long end=System.nanoTime()+30_000_000_000L;
        while(System.nanoTime()<end){var ready=new java.util.concurrent.atomic.AtomicBoolean();
            SwingUtilities.invokeAndWait(()->ready.set(done.getAsBoolean()));if(ready.get())return;Thread.sleep(30);}
        throw new AssertionError("Human import worker did not complete");
    }
    private static void runtimeMesh(String id,org.beckenburg.editor.EditorMeshPipeline.Mesh expected){
        var actual=GlbProps.model(id).triangles();check(actual.size()==expected.triangles().size(),"Actual runtime triangle count");
        for(int i=0;i<actual.size();i++){
            var t=actual.get(i);var e=expected.triangles().get(i);
            var av=java.util.List.of(t.a(),t.b(),t.c());var ev=java.util.List.of(e.a(),e.b(),e.c());
            for(int j=0;j<3;j++){
                var a=av.get(j);var b=ev.get(j);
                check(a.x()==(double)(float)b.x()&&a.y()==(double)(float)b.y()&&a.z()==(double)(float)b.z()
                    &&a.u()==(double)(float)b.u()&&a.v()==(double)(float)b.v(),"Real game readback retains edited position, winding and UV at every corner");
            }
        }
    }
    public static void main(String[] args)throws Exception{
        Path output=Path.of("build/editor-agent-evidence/mesh-topology-import.png").toAbsolutePath();Files.createDirectories(output.getParent());
        Path home=Files.createTempDirectory("topology-human-import-");String oldHome=System.getProperty("user.home");
        System.setProperty("user.home",home.toString());var ui=new AtomicReference<AssetImportDialog>();var published=new AtomicReference<String>();
        try{
            Path source=home.resolve("tetra.obj");Files.writeString(source,"""
                o shell
                v 0 0 0
                v 1 0 0
                v 0 1 0
                v 0 0 1
                vt 0 0
                vt 1 0
                vt 0 1
                f 1/1 3/3 2/2
                f 1/1 2/2 4/3
                f 1/1 4/3 3/2
                f 2/2 3/3 4/1
                """);byte[] original=Files.readAllBytes(source);
            SwingUtilities.invokeAndWait(()->{
                var dock=new AssetImportDialog(null,published::set);ui.set(dock);
                try{((JDialog)field(dock,"dialog")).setVisible(true);}catch(Exception e){throw new RuntimeException(e);}
                dock.loadSource(source);
            });
            await(()->{try{return ((JButton)field(ui.get(),"apply")).isEnabled();}catch(Exception e){throw new RuntimeException(e);}});
            AtomicReference<org.beckenburg.editor.EditorTopologyProject> authoring=new AtomicReference<>();
            AtomicReference<org.beckenburg.editor.EditorTopologyProject.State> initial=new AtomicReference<>(),edited=new AtomicReference<>();
            SwingUtilities.invokeAndWait(()->{try{
                var dock=ui.get();var project=(org.beckenburg.editor.EditorTopologyProject)field(dock,"topologyProject");authoring.set(project);initial.set(project.snapshot());
                ((JButton)field(dock,"saveMesh")).doClick();
                ((JSpinner)field(dock,"vertexX")).setValue(-.25);((JButton)field(dock,"moveVertex")).doClick();edited.set(project.snapshot());
                check(edited.get().vertices().get(0).x()==-.25,"Human vertex transform changes authoritative indexed mesh");
                ((JButton)field(dock,"undoMesh")).doClick();check(project.snapshot().equals(initial.get()),"Human mesh undo");
                ((JButton)field(dock,"redoMesh")).doClick();check(project.snapshot().equals(edited.get()),"Human mesh redo preserves IDs and UVs");
                ((JButton)field(dock,"saveMesh")).doClick();
                ((JSpinner)field(dock,"vertexX")).setValue(-.5);((JButton)field(dock,"moveVertex")).doClick();
                ((JButton)field(dock,"reopenMesh")).doClick();check(project.snapshot().equals(edited.get()),"Human native mesh reopen uses same project");
            }catch(Exception e){throw new RuntimeException(e);}});
            await(()->{try{return ((JButton)field(ui.get(),"apply")).isEnabled();}catch(Exception e){throw new RuntimeException(e);}});
            SwingUtilities.invokeAndWait(()->{try{
                var dock=ui.get();var report=((JTextArea)field(dock,"details")).getText();
                check(report.contains("4 vertices / 12 half-edges / 1 islands / 0 boundary edges"),"Native import topology counts: "+report);
                check(report.contains("0 issues"),"Oriented closed source is valid");
                check(!report.contains("\\n"),"Human report uses real newlines, not escaped text");
                var dialog=(JDialog)field(dock,"dialog");var content=dialog.getContentPane();
                var image=new BufferedImage(content.getWidth(),content.getHeight(),BufferedImage.TYPE_INT_ARGB);
                var g=image.createGraphics();try{content.printAll(g);}finally{g.dispose();}
                ImageIO.write(image,"png",output.toFile());
                ((JButton)field(dock,"apply")).doClick();
            }catch(Exception e){throw new RuntimeException(e);}});
            await(()->published.get()!=null);
            String id=published.get();check(ImportedAssetLibrary.approved(id),"Actual human publish registered native asset");
            runtimeMesh(id,authoring.get().mesh());
            var archived=org.beckenburg.editor.EditorTopologyProject.open(ImportedAssetLibrary.root().resolve(id+".authoring.btop"));
            check(archived.snapshot().equals(edited.get()),"Published asset retains exact editable native topology beside original source");
            check(Arrays.equals(original,Files.readAllBytes(source))&&Arrays.equals(original,Files.readAllBytes(ImportedAssetLibrary.root().resolve(id+".source.obj"))),"Human topology review preserves exact source and archived bytes");
            check(org.beckenburg.editor.EditorTopologyProject.open(authoring.get().documentPath()).snapshot().equals(edited.get()),"Fresh native mesh open retains edited IDs, positions and corner UVs");
            published.set(null);
            SwingUtilities.invokeAndWait(()->ui.get().loadSource(authoring.get().documentPath()));
            await(()->{try{return ((JButton)field(ui.get(),"apply")).isEnabled();}catch(Exception e){throw new RuntimeException(e);}});
            SwingUtilities.invokeAndWait(()->{try{
                var fresh=(org.beckenburg.editor.EditorTopologyProject)field(ui.get(),"topologyProject");
                check(fresh!=authoring.get()&&fresh.snapshot().equals(edited.get()),"Human source chooser native path opens fresh persisted topology");
                ((JButton)field(ui.get(),"apply")).doClick();
            }catch(Exception e){throw new RuntimeException(e);}});
            await(()->published.get()!=null);
            runtimeMesh(published.get(),authoring.get().mesh());
            check(Arrays.equals(Files.readAllBytes(authoring.get().documentPath()),Files.readAllBytes(ImportedAssetLibrary.root().resolve(published.get()+".source.btop"))),"Published native authoring source archived exactly");
            var approved=ImportedAssetLibrary.available();published.set(null);
            SwingUtilities.invokeAndWait(()->ui.get().loadSource(source));
            await(()->{try{return ((JButton)field(ui.get(),"apply")).isEnabled();}catch(Exception e){throw new RuntimeException(e);}});
            byte[] changed=new String(original,java.nio.charset.StandardCharsets.UTF_8).replace("v 1 0 0","v 2 0 0").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Files.write(source,changed);
            SwingUtilities.invokeAndWait(()->{try{((JButton)field(ui.get(),"apply")).doClick();}catch(Exception e){throw new RuntimeException(e);}});
            await(()->{try{return ((JTextArea)field(ui.get(),"details")).getText().contains("PUBLISH FAILED:");}catch(Exception e){throw new RuntimeException(e);}});
            check(published.get()==null&&ImportedAssetLibrary.available().equals(approved)&&Arrays.equals(changed,Files.readAllBytes(source)),
                "Changed-after-review source cannot publish unobserved geometry or overwrite external source changes");
            Path glb=home.resolve("review.glb");Files.write(glb,ImportedAssetLibrary.read(id));
            SwingUtilities.invokeAndWait(()->ui.get().loadSource(glb));
            await(()->{try{return ((JButton)field(ui.get(),"apply")).isEnabled();}catch(Exception e){throw new RuntimeException(e);}});
            SwingUtilities.invokeAndWait(()->{try{
                check(!((JButton)field(ui.get(),"moveVertex")).isEnabled(),"GLB editing cannot silently discard original material/normal bindings");
            }catch(Exception e){throw new RuntimeException(e);}});
            byte[] changedGlb=org.beckenburg.editor.EditorGlbWriter.encode(org.beckenburg.editor.EditorMeshPipeline.read("tetra.obj",original));
            Files.write(glb,changedGlb);
            SwingUtilities.invokeAndWait(()->{try{((JButton)field(ui.get(),"apply")).doClick();}catch(Exception e){throw new RuntimeException(e);}});
            await(()->{try{return ((JTextArea)field(ui.get(),"details")).getText().contains("PUBLISH FAILED:");}catch(Exception e){throw new RuntimeException(e);}});
            check(published.get()==null&&ImportedAssetLibrary.available().equals(approved)&&Arrays.equals(changedGlb,Files.readAllBytes(glb)),
                "Zero-regression GLB cannot bypass review by rereading externally replaced geometry");
            SwingUtilities.invokeAndWait(()->{try{((JButton)field(ui.get(),"newMesh")).doClick();}catch(Exception e){throw new RuntimeException(e);}});
            var nativeProject=new AtomicReference<org.beckenburg.editor.EditorTopologyProject>();
            SwingUtilities.invokeAndWait(()->{try{
                var dock=ui.get();var project=(org.beckenburg.editor.EditorTopologyProject)field(dock,"topologyProject");nativeProject.set(project);
                check(project.snapshot().faces().isEmpty(),"New human mesh starts empty in native state");
                for(var kind:org.beckenburg.editor.EditorMeshPrimitives.Kind.values()){
                    var before=project.snapshot();
                    ((JComboBox<?>)field(dock,"primitiveKind")).setSelectedItem(kind);
                    ((JButton)field(dock,"previewPrimitive")).doClick();
                    check(project.snapshot()==before,"Human primitive preview leaves actual authoring state untouched");
                    check(((JButton)field(dock,"applyPrimitive")).isEnabled(),"Reviewed primitive is explicitly applicable");
                    if(kind==org.beckenburg.editor.EditorMeshPrimitives.Kind.BOX){
                        ((JSpinner)field(dock,"primitiveWidth")).setValue(2d);
                        check(!((JButton)field(dock,"applyPrimitive")).isEnabled(),"Changing parameters invalidates prior primitive review");
                        ((JButton)field(dock,"previewPrimitive")).doClick();
                    }
                    ((JButton)field(dock,"applyPrimitive")).doClick();var after=project.snapshot();
                    check(after.faces().size()>before.faces().size(),"Human apply commits real primitive geometry");
                    ((JButton)field(dock,"undoMesh")).doClick();check(project.snapshot().equals(before),"Human primitive undo");
                    ((JButton)field(dock,"redoMesh")).doClick();check(project.snapshot().equals(after),"Human primitive redo");
                }
                ((JButton)field(dock,"saveMesh")).doClick();
                check(org.beckenburg.editor.EditorTopologyProject.open(project.documentPath()).snapshot().equals(project.snapshot()),"Fresh native primitive document readback");
            }catch(Exception e){throw new RuntimeException(e);}});
            await(()->{try{return ((JButton)field(ui.get(),"apply")).isEnabled();}catch(Exception e){throw new RuntimeException(e);}});
            SwingUtilities.invokeAndWait(()->{try{
                var dock=ui.get();((JButton)field(dock,"previewPrimitive")).doClick();
                // Click the real FRONT viewport at a stable interior point of the actual native mesh.
                ((JComboBox<?>)field(dock,"meshView")).setSelectedItem(org.beckenburg.editor.EditorMeshPicking.Projection.FRONT);
                var canvas=(JPanel)field(dock,"preview");int half=canvas.getWidth()/2,height=Math.max(180,canvas.getHeight());
                double scale=Math.min((half-26)/2d,(height-36)/1d);
                double rawX=.15,rawY=-.2;int clickX=half/2+(int)Math.round(rawX*scale),clickY=height/2+(int)Math.round(rawY*scale);
                var state=nativeProject.get().snapshot();
                var expected=org.beckenburg.editor.EditorMeshPicking.pick(state,org.beckenburg.editor.EditorMeshPicking.Projection.FRONT,
                    (clickX-half/2)/scale,(clickY-height/2)/scale).orElseThrow();
                for(String mode:java.util.List.of("VERTEX","EDGE","FACE")){
                    ((JComboBox<?>)field(dock,"selectionMode")).setSelectedItem(mode);
                    canvas.dispatchEvent(new java.awt.event.MouseEvent(canvas,java.awt.event.MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,clickX,clickY,1,false,java.awt.event.MouseEvent.BUTTON1));
                    var actual=(org.beckenburg.editor.EditorMeshPicking.Hit)field(dock,"selectedHit");
                    check(actual!=null&&actual.faceId()==expected.faceId()&&actual.vertexId()==expected.vertexId()&&actual.edgeA()==expected.edgeA()&&actual.edgeB()==expected.edgeB(),"Real mouse event selects native triangle and stable "+mode+" IDs");
                    check(nativeProject.get().snapshot()==state,"Picking cannot mutate geometry or history");
                }
                ((JComboBox<?>)field(dock,"meshView")).setSelectedItem(org.beckenburg.editor.EditorMeshPicking.Projection.ISOMETRIC);
                var content=((JDialog)field(dock,"dialog")).getContentPane();
                var image=new BufferedImage(content.getWidth(),content.getHeight(),BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
                try{content.printAll(g);}finally{g.dispose();}ImageIO.write(image,"png",output.resolveSibling("mesh-primitives-human.png").toFile());
                ((JButton)field(dock,"apply")).doClick();
            }catch(Exception e){throw new RuntimeException(e);}});
            await(()->published.get()!=null);runtimeMesh(published.get(),nativeProject.get().mesh());
            var primitiveArchive=org.beckenburg.editor.EditorTopologyProject.open(ImportedAssetLibrary.root().resolve(published.get()+".authoring.btop"));
            check(primitiveArchive.snapshot().equals(nativeProject.get().snapshot()),"Published primitive retains exact native IDs and corner UVs");
            System.out.println("PASS real human five-primitive preview/apply/undo/redo, parameter invalidation, new native mesh/save/fresh reopen, real mouse triangle/vertex/edge/face selection, UI PNG and every published runtime GLB corner");
            System.out.println("PASS actual human mesh authoring: stable vertex move/undo/redo, native save/reopen/fresh chooser load, topology review/UI PNG, runtime GLB publish, exact editable/original source archives and changed-source rejection");
        }finally{
            SwingUtilities.invokeAndWait(()->{if(ui.get()!=null)try{((JDialog)field(ui.get(),"dialog")).dispose();}catch(Exception e){throw new RuntimeException(e);}});
            System.setProperty("user.home",oldHome);
            try(var paths=Files.walk(home)){for(var p:paths.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
    }
}
