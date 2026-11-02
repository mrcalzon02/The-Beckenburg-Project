package org.beckenburg.editor;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;

public final class EditorObjWriterTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private interface IO {void run()throws Exception;}
    private static void refused(IO action)throws Exception{try{action.run();throw new AssertionError("Unsafe export accepted");}catch(IOException expected){}}
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("native-obj-export-");
        try{
            byte[] raw=("o surface\nv 0 0 0\nv 1 0 0\nv 0 1 0\nv 0 1 0\nv 1 0 0\nv 1 1 0\nvt 0 0\nvt 1 0\nvt 0 1\nvt .25 .25\nvt .75 .25\nvt 1 1\nf 1/1 2/2 3/3\nf 4/4 5/5 6/6\n").getBytes(StandardCharsets.UTF_8);
            var p=EditorTopologyProject.fromMesh(EditorMeshPipeline.read("source.obj",raw),EditorMeshPipeline.sourceFingerprint(raw),root.resolve("native.btop"));
            var history=new EditorHistory<>(p,8);var before=p.snapshot();Path target=root.resolve("copy.obj");
            p.exportObj(p.observe(),target,null);byte[] first=Files.readAllBytes(target);EditorObjWriter.validate(before,first);
            var reimport=EditorTopologyProject.fromMesh(EditorMeshPipeline.read("copy.obj",first),EditorMeshPipeline.sourceFingerprint(first),root.resolve("copy.btop"));
            check(reimport.snapshot().vertices().size()==6&&reimport.mesh().triangles().equals(p.mesh().triangles()),"Separate coincident source IDs, per-corner UV, part, positions and winding survive export/import");
            check(p.snapshot()==before&&!p.hasUnsavedChanges()&&!history.canUndo(),"Export leaves native authority, history and clean baseline unchanged");
            check(new String(first,StandardCharsets.UTF_8).lines().filter(l->l.startsWith("vn ")).allMatch(l->{var n=l.split(" ");return Double.parseDouble(n[1])==0&&Double.parseDouble(n[2])==0&&Double.parseDouble(n[3])==1;}),"Derived flat normals face outward");
            p.exportObj(p.observe(),target,EditorDocumentStore.fingerprint(first));check(Arrays.equals(first,Files.readAllBytes(target)),"Export is deterministic");
            var old=p.observe();history.execute(()->p.applyPreview(p.previewWeld(p.snapshot().vertices().stream().map(EditorTopologyProject.Vertex::id).toList())));
            refused(()->p.exportObj(old,target,EditorDocumentStore.fingerprint(first)));check(Arrays.equals(first,Files.readAllBytes(target)),"Stale native observation cannot overwrite export");
            p.exportObj(p.observe(),target,EditorDocumentStore.fingerprint(first));byte[] welded=Files.readAllBytes(target);
            var parsed=EditorMeshPipeline.read("copy.obj",welded);check(EditorMeshTopology.build(parsed).vertices().size()==4&&EditorMeshTopology.build(parsed).uvSeams()==1,"Welded connectivity and authored UV seam survive OBJ");
            check(p.hasUnsavedChanges()&&history.canUndo(),"Export does not mark edited native state saved");
            refused(()->p.exportObj(p.observe(),target,EditorDocumentStore.fingerprint(first)));check(Arrays.equals(welded,Files.readAllBytes(target)),"Competing target revision retained");
            refused(()->p.exportObj(p.observe(),p.documentPath(),null));
            var damaged=new EditorTopologyProject.State(before.sceneId(),before.sourceFingerprint(),before.name(),before.sourceBytes(),before.vertices(),List.of(before.faces().get(0)));
            p.restore(damaged);refused(()->p.exportObj(p.observe(),target,EditorDocumentStore.fingerprint(welded)));check(Arrays.equals(welded,Files.readAllBytes(target)),"Unused vertex loss is refused before target publication");
            var clean=p.previewRemoveUnusedVertices();check(p.snapshot()==damaged&&clean.snapshot().vertices().size()==3&&clean.snapshot().faces().equals(damaged.faces()),"Unused cleanup is explicit native preview preserving surviving IDs and faces");
            p.cancelPreview(clean);check(p.snapshot()==damaged,"Cancel leaves native state unchanged");
            history.execute(()->p.applyPreview(p.previewRemoveUnusedVertices()));var cleaned=p.snapshot();check(history.undo()&&p.snapshot().equals(damaged)&&history.redo()&&p.snapshot().equals(cleaned),"Cleanup is one reversible native transaction");
            p.exportObj(p.observe(),target,EditorDocumentStore.fingerprint(welded));EditorObjWriter.validate(cleaned,Files.readAllBytes(target));
            p.save();check(EditorTopologyProject.open(p.documentPath()).snapshot().equals(cleaned),"Native archive retains exact surviving numeric IDs");
            System.out.println("PASS native OBJ export: real importer roundtrip, connectivity/UV seams/facing, deterministic readback, stale/native/target guards, explicit unused cleanup and history");
        }finally{try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
}
