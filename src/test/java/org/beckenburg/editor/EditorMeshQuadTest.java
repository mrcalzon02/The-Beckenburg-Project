package org.beckenburg.editor;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Source-declared quads survive native history, topology edits and actual interchange. */
public final class EditorMeshQuadTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static final String SOURCE="""
            o panel
            v 0 0 0
            v 1 0 0
            v 2 0 0
            v 0 1 0
            v 1 1 0
            v 2 1 0
            f 1 2 5 4
            f 2 3 6 5
            """;
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("authored-quads-");
        try{
            var mesh=EditorMeshPipeline.read("panel.obj",SOURCE.getBytes(StandardCharsets.UTF_8));
            var expected=List.of(new EditorMeshQuad(0,1),new EditorMeshQuad(2,3));
            check(mesh.quads().equals(expected),"OBJ fan retains both authored quads");
            var split=EditorMeshPipeline.read("split.obj",SOURCE.replace("f 1 2 5 4","f 1 2 5\nf 1 5 4").getBytes(StandardCharsets.UTF_8));
            check(split.quads().equals(List.of(expected.get(1))),"Independent coplanar triangles never imply an authored quad");
            var project=EditorTopologyProject.fromMesh(mesh,"1".repeat(64),root.resolve("panel.btop"));
            var plain=new ByteArrayOutputStream();EditorObjWriter.write(project.snapshot(),plain);
            long quadLines=plain.toString(StandardCharsets.UTF_8).lines().filter(line->line.startsWith("f ")&&line.split(" ").length==5).count();
            check(quadLines==2,"Representable authored faces export as standard four-corner OBJ polygons");
            project.applyPreview(project.previewUvTransform(2,false,EditorTopologyProject.UvTransform.MOVE,.125,.25,false));
            project.save();check(ByteBuffer.wrap(project.snapshotBytes()).getInt(4)==6,"Quad-bearing native schema");
            check(EditorTopologyProject.open(project.documentPath()).snapshot().equals(project.snapshot()),"Fresh open retains pairs and independently edited seam UVs");
            var copy=new ByteArrayOutputStream();EditorObjWriter.write(project.snapshot(),copy);EditorObjWriter.validate(project.snapshot(),copy.toByteArray());
            var bundle=new ByteArrayOutputStream();EditorObjBundle.write(project.snapshot(),bundle);EditorObjBundle.validate(project.snapshot(),bundle.toByteArray());
            var box=new EditorMeshPrimitives.Parameters(EditorMeshPrimitives.Kind.BOX,2,2,2,12,1);
            project.appendPrimitive(box);check(project.snapshot().quads().size()==8,"Appended box carries six real authored sides in addition to original pairs");
            var boxOnly=EditorTopologyProject.fromMesh(EditorMeshPrimitives.preview(box),"2".repeat(64),root.resolve("box.btop"));
            long a=EditorMeshBevelTest.vertex(boxOnly.snapshot(),1,1,-1), b=EditorMeshBevelTest.vertex(boxOnly.snapshot(),1,1,1);
            var beveled=boxOnly.previewBevel(a,b,EditorMeshBevelTest.face(boxOnly.snapshot(),a,b).id(),.2);
            check(beveled.snapshot().quads().size()==2,"Bevel retains only the two completely untouched original box quads");
            project.restore(EditorTopologyProject.open(project.documentPath()).snapshot());
            byte[] glb=EditorGlbWriter.encode(project.mesh());EditorGlbWriter.validate(project.mesh(),glb);
            var imported=EditorTopologyProject.fromGlb("panel.glb",glb,root.resolve("reimport.btop"));
            check(imported.snapshot().quads().equals(expected),"Shared runtime GLB reader preserves authoring topology");
            check(Arrays.equals(glb,EditorGlbWriter.encode(imported.mesh())),"Unedited original GLB remains byte exact");
            var history=new EditorHistory<>(imported,10);var edit=imported.previewFaceEdit(0,EditorTopologyProject.FaceEdit.INSET,.25);
            history.execute(()->imported.applyPreview(edit));check(imported.snapshot().quads().equals(List.of(expected.get(1))),"Edited pair invalidated, second quad survives");
            byte[] changed=EditorGlbWriter.encode(imported.mesh());EditorGlbWriter.validate(imported.mesh(),changed);
            check(EditorGlbReader.read(changed).mesh("edited.glb").quads().size()==1,"Edited source GLB retains surviving quad");
            check(history.undo()&&imported.snapshot().quads().equals(expected),"Native undo restores original authored pairs");
            var decoded=EditorGlbReader.read(glb);
            var badRoot=EditorGlbReader.obj(EditorGlbJson.read(EditorGlbJson.write(decoded.root())));
            var extensions=EditorGlbReader.obj(badRoot.get("extensions"));
            extensions.put("BECK_authored_quads",Map.of("version",2,"pairs",List.of(List.of(0,1))));
            try{EditorTopologyProject.fromGlb("future.glb",EditorGlbDocumentWriter.container(badRoot,decoded.binary()),root.resolve("bad.btop"));throw new AssertionError("Future quad metadata accepted");}catch(IOException expectedRefusal){}
            var raw=project.snapshotBytes();ByteBuffer.wrap(raw).putLong(raw.length-8,987654321L);
            try{EditorTopologyProject.decode(raw);throw new AssertionError("Missing quad face accepted");}catch(IOException expectedRefusal){}
            System.out.println("PASS authored OBJ quads, split-triangle distinction, BTOP6/OBJ bundle/GLB, seam UV edit, selective topology invalidation and native undo");
        }finally{try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
}
