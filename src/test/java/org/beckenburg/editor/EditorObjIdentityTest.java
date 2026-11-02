package org.beckenburg.editor;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Native OBJ source index identity, distinct coincident vertices and per-corner UV seams. */
public final class EditorObjIdentityTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static final String SOURCE="""
        o shell
        v 0 0 0
        v 1 0 0
        v 0 1 0
        v 0 0 0
        v -1 0 0
        v 0 -1 0
        vt 0 0
        vt 1 0
        vt 0 1
        f 1/1 2/2 3/3
        f 4/1 5/2 6/3
        """;
    public static void main(String[] args)throws Exception{
        byte[] raw=SOURCE.getBytes(StandardCharsets.UTF_8);
        if(Arrays.asList(args).contains("--comment-check")){
            EditorMeshPipeline.read("comment.obj",SOURCE.replace("f 1/1 2/2 3/3","f 1/1 2/2 3/3 # inline comment").getBytes(StandardCharsets.UTF_8));
            System.out.println("PASS authored OBJ inline face comment");return;
        }
        var source=EditorMeshPipeline.read("coincident.obj",raw);
        Path root=Files.createTempDirectory("obj-native-identity-");
        try{
            var project=EditorTopologyProject.fromMesh(source,EditorMeshPipeline.sourceFingerprint(raw),root.resolve("actual.btop"));
            var initial=project.snapshot();
            check(initial.vertices().size()==6,"OBJ position indices must preserve six authored identities instead of implicitly welding the coincident pair");
            check(project.topology().islands()==2&&project.topology().manifold(),"Separate source vertex fans remain separate and valid");
            check(EditorMeshTopology.build(new EditorMeshPipeline.Mesh(source.name(),source.triangles(),source.sourceBytes())).vertices().size()==5,"Negative control: dropping source IDs silently welds authored vertices");
            check(source.cornerVertexIds().equals(List.of(0L,1L,2L,3L,4L,5L)),"Source position identity order");
            check(EditorMeshPipeline.read("comment.obj",SOURCE.replace("f 1/1 2/2 3/3","f 1/1 2/2 3/3 # inline comment").getBytes(StandardCharsets.UTF_8)).cornerVertexIds().equals(source.cornerVertexIds()),"Inline comments preserve authored face references");
            try{source.cornerVertexIds().add(9L);throw new AssertionError("Mutable source IDs");}catch(UnsupportedOperationException expected){}
            var history=new EditorHistory<>(project,8);history.execute(()->project.moveVertex(initial.faces().get(0).a().vertex(),-.25,0,0));var edited=project.snapshot();
            var second=edited.faces().get(1);var other=edited.vertices().stream().filter(v->v.id()==second.a().vertex()).findFirst().orElseThrow();
            check(other.x()==0&&other.y()==0&&other.z()==0,"Editing one authored source vertex cannot move its distinct coincident neighbor");
            check(initial.faces().equals(edited.faces()),"Source connectivity, IDs and every corner UV remain unchanged");
            check(history.undo()&&project.snapshot().equals(initial)&&history.redo()&&project.snapshot().equals(edited),"Native identity survives shared undo/redo");
            project.save();check(EditorTopologyProject.open(project.documentPath()).snapshot().equals(edited),"Native save/reopen retains distinct source connectivity");
            var seam=EditorMeshPipeline.read("seam.obj",("""
                o shell
                v 0 0 0
                v 1 0 0
                v 0 1 0
                v 1 1 0
                vt 0 0
                vt 1 0
                vt 0 1
                vt .25 .25
                vt .75 .25
                vt 1 1
                f 1/1 2/2 3/3
                f 3/4 2/5 4/6
                """).getBytes(StandardCharsets.UTF_8));
            var graph=EditorMeshTopology.build(seam);check(graph.vertices().size()==4&&graph.uvSeams()==1&&graph.boundaryEdges()==4,"Shared source vertices retain distinct corner UVs without splitting the actual edge");
            var negative=EditorMeshPipeline.read("negative.obj",("""
                v 0 0 0
                v 1 0 0
                v 1 1 0
                v 0 1 0
                f -4 -3 -2 -1
                """).getBytes(StandardCharsets.UTF_8));
            check(negative.cornerVertexIds().equals(List.of(0L,1L,2L,0L,2L,3L)),"Negative indices and fan triangulation retain original sharing");
            check(EditorMeshPipeline.regress(source,0).optimized().cornerVertexIds().equals(source.cornerVertexIds()),"Level-zero exact geometry retains source topology metadata");
            try{EditorMeshTopology.build(new EditorMeshPipeline.Mesh("bad",List.of(source.triangles().get(0)),0,List.of(0L,0L,2L)));throw new AssertionError("Contradictory indexed position accepted");}catch(IllegalArgumentException expected){}
            if(Arrays.asList(args).contains("--stress")){
                var text=new StringBuilder();
                for(int i=0;i<EditorMeshPipeline.MAX_TRIANGLES;i++){int x=i*2;text.append("v ").append(x).append(" 0 0\nv ").append(x+1).append(" 0 0\nv ").append(x).append(" 1 0\n");}
                for(int i=0;i<EditorMeshPipeline.MAX_TRIANGLES;i++){int id=i*3+1;text.append("f ").append(id).append(' ').append(id+1).append(' ').append(id+2).append('\n');}
                var maximum=EditorMeshPipeline.read("max.obj",text.toString().getBytes(StandardCharsets.UTF_8));text=null;
                check(maximum.cornerVertexIds().size()==360000&&EditorMeshTopology.build(maximum).vertices().size()==360000,"Maximum indexed OBJ parser/graph budget under256MiB");
            }
            System.out.println("PASS native OBJ identities: coincident source indices, distinct edits, original sharing/UV seams, negative indices/fans, undo/redo and exact native save/reopen");
        }finally{try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
}
