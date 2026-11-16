package org.beckenburg.editor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Standalone import, reduction, source-preservation and export smoke proof. */
public final class EditorMeshPipelineTest {
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    private static void checkInvalidIds(Runnable construct) {
        try { construct.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid native mesh IDs were accepted");
    }
    private static void checkMeshBindings(EditorMeshPipeline.Triangle face) {
        var triangles = List.of(face);
        var corners = new ArrayList<>(List.of(8L, 9L, 8L));
        var faces = new ArrayList<>(List.of(5L));
        var original = new EditorMeshPipeline.Mesh("bindings", triangles, 0, corners, List.of(), null, faces);
        corners.set(0, -1L);
        faces.clear();
        check(original.cornerVertexIds().equals(List.of(8L, 9L, 8L)) && original.faceIds().equals(List.of(5L)),
            "A caller changed an already validated mesh");
        try {
            original.cornerVertexIds().set(0, -1L);
            throw new AssertionError("A mesh exposed mutable native bindings");
        } catch (UnsupportedOperationException expected) {
            // Independent revisions may safely reuse these bindings.
        }
        var revised = new EditorMeshPipeline.Mesh("revised", triangles, 0, original.cornerVertexIds(),
            List.of(), null, original.faceIds());
        check(revised.cornerVertexIds() == original.cornerVertexIds() && revised.faceIds() == original.faceIds(),
            "A mesh revision recopied unchanged native bindings");

        checkInvalidIds(() -> new EditorMeshPipeline.Mesh("negative corner", triangles, 0, corners));
        checkInvalidIds(() -> new EditorMeshPipeline.Mesh("negative face", triangles, 0, List.of(), List.of(), null, List.of(-1L)));
        checkInvalidIds(() -> new EditorMeshPipeline.Mesh("wrong count", List.of(face, face), 0, revised.cornerVertexIds()));
        checkInvalidIds(() -> new EditorMeshPipeline.Mesh("wrong face count", List.of(face, face), 0, List.of(),
            List.of(), null, revised.faceIds()));
        checkInvalidIds(() -> new EditorMeshPipeline.Mesh("corners used as faces", Collections.nCopies(3, face), 0,
            List.of(), List.of(), null, revised.cornerVertexIds()));
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("editor-mesh-lab-");
        try{
            Path obj=root.resolve("figure.obj");
            String geometry="""
                o Head
                v 0 1.7 0
                v .2 1.4 0
                v -.2 1.4 0
                v 0 1.4 .2
                f 1 2 4
                f 1 4 3
                f 1 3 2
                f 2 3 4
                o Torso
                v -.6 0 -.25
                v .6 0 -.25
                v .6 1.2 -.25
                v -.6 1.2 -.25
                v -.6 0 .25
                v .6 0 .25
                v .6 1.2 .25
                v -.6 1.2 .25
                f 5 6 7 8
                f 9 10 11 12
                f 5 9 12 8
                f 6 10 11 7
                f 8 7 11 12
                f 5 6 10 9
                """;
            Files.writeString(obj,geometry);
            byte[] unchanged=Files.readAllBytes(obj);
            var model=EditorMeshPipeline.read(obj);
            check(model.triangles().size()==16,"OBJ triangulation");
            check(model.triangles().stream().anyMatch(t->t.part().equals("Head")),"Head identity");
            var exact=EditorMeshPipeline.regress(model,0);
            check(exact.parts().stream().allMatch(p->p.before()==p.after()),"Level zero no loss");
            var face=model.triangles().get(0);
            checkMeshBindings(face);
            var repeated=new EditorMeshPipeline.Mesh("two-sided-face",
                java.util.List.of(face,face),10);
            check(EditorMeshPipeline.regress(repeated,0).optimized().triangles().size()==2,
                "Level 0 must not silently deduplicate UV/sidedness-sensitive geometry");
            byte[] glb=EditorGlbWriter.encode(exact.optimized());
            check(ByteBuffer.wrap(glb).order(ByteOrder.LITTLE_ENDIAN).getInt(0)==0x46546c67,
                  "Export GLB magic");
            check(glb.length<=4_000_000,"Game decoder size budget");
            var extreme=EditorMeshPipeline.regress(model,12);
            check(extreme.overRegressed(),"Warn before a head or torso collapses");
            check(extreme.warnings().stream().anyMatch(w->w.contains("OVER-REGRESSION")&&w.contains("Head")),
                  "Head-specific over-regression warning");
            check(Files.readAllBytes(obj).length==unchanged.length &&
                  java.util.Arrays.equals(Files.readAllBytes(obj),unchanged),"Original unchanged");
            Path stl=root.resolve("triangles.stl");
            Files.writeString(stl,"""
                solid Test
                facet normal 0 0 1
                outer loop
                vertex 0 0 0
                vertex 1 0 0
                vertex 0 1 0
                endloop
                endfacet
                endsolid Test
                """);
            check(EditorMeshPipeline.read(stl).triangles().size()==1,"ASCII STL");
            ByteBuffer b=ByteBuffer.allocate(134).order(ByteOrder.LITTLE_ENDIAN);
            b.putInt(80,1);b.putFloat(84+12+12,1);b.putFloat(84+12+24+4,1);
            Path binary=root.resolve("binary.stl");Files.write(binary,b.array());
            check(EditorMeshPipeline.read(binary).triangles().size()==1,"Binary STL");
            boolean refused=false;
            try{EditorMeshPipeline.read(root.resolve("missing.gltf"));}
            catch(java.io.IOException expected){refused=true;}
            check(refused,"Missing/unimplemented formats must fail, not silently import");
            System.out.println("PASS OBJ, ASCII/binary STL, GLB export and head-specific over-regression");
        }finally{try(var walk=Files.walk(root)){
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p->{
                try{Files.deleteIfExists(p);}catch(Exception ignored){}
            });
        }}
    }
}
