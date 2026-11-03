package org.beckenburg.editor;
import java.util.*;

public final class EditorMeshTopologyTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static EditorMeshPipeline.Vertex v(double x,double y,double z){return new EditorMeshPipeline.Vertex(x,y,z,x,z);}
    private static EditorMeshPipeline.Triangle t(String part,EditorMeshPipeline.Vertex a,EditorMeshPipeline.Vertex b,EditorMeshPipeline.Vertex c){return new EditorMeshPipeline.Triangle(part,a,b,c);}
    private static EditorMeshPipeline.Mesh mesh(EditorMeshPipeline.Triangle... faces){return new EditorMeshPipeline.Mesh("topology",List.of(faces),0);}
    private static void links(EditorMeshTopology graph){
        for(int i=0;i<graph.edges().size();i++){
            var e=graph.edges().get(i);check(graph.edges().get(e.next()).previous()==i&&graph.edges().get(e.previous()).next()==i,"next/previous cycle");
            check(graph.edges().get(e.next()).next()==e.previous(),"Triangle degree is exactly three");
            if(e.twin()>=0){var other=graph.edges().get(e.twin());check(other.twin()==i&&other.origin()==e.destination()&&other.destination()==e.origin(),"Reciprocal opposite twin");}
        }
    }
    public static void main(String[] args){
        if(args.length>0&&args[0].equals("--stress")){
            var triangles=new ArrayList<EditorMeshPipeline.Triangle>();
            for(int i=0;i<EditorMeshPipeline.MAX_TRIANGLES;i++)triangles.add(t("mesh",v(i*2,0,0),v(i*2+1,0,0),v(i*2,1,0)));
            var graph=EditorMeshTopology.build(new EditorMeshPipeline.Mesh("maximum",triangles,0));
            check(graph.manifold()&&graph.edges().size()==360000&&graph.vertices().size()==360000&&graph.islands()==120000,"Maximum import budget remains bounded");
            System.out.println("PASS maximum topology budget: 120000 faces / 360000 vertices and half-edges in bounded heap");return;
        }
        var o=v(0,0,0);var a=v(1,0,0);var b=v(0,1,0);var c=v(0,0,1);
        var tetra=mesh(t("shell",o,b,a),t("shell",o,a,c),t("shell",o,c,b),t("shell",a,b,c));
        var closed=EditorMeshTopology.build(tetra);links(closed);
        check(closed.manifold()&&closed.boundaryEdges()==0&&closed.vertices().size()==4&&closed.islands()==1,"Closed oriented tetrahedron");
        var d=v(1,0,1);
        var square=mesh(t("floor",o,a,d),t("floor",new EditorMeshPipeline.Vertex(0,0,0,2,2),d,c));
        var open=EditorMeshTopology.build(square);links(open);
        check(open.manifold()&&open.boundaryEdges()==4&&open.uvSeams()==1&&open.vertices().size()==4,"Open manifold retains UV seam across geometric twin");
        check(square.triangles().get(1).a().u()==2,"UV seam never silently welded or rewritten");
        var reversed=EditorMeshTopology.build(mesh(t("part",o,a,b),t("part",o,a,c)));
        check(reversed.issues().stream().anyMatch(i->i.code()==EditorMeshTopology.Code.EDGE_ORIENTATION),"Same-direction shared edge diagnosed");
        var triple=EditorMeshTopology.build(mesh(t("part",o,a,b),t("part",a,o,c),t("part",o,a,d)));
        check(triple.issues().stream().anyMatch(i->i.code()==EditorMeshTopology.Code.NON_MANIFOLD_EDGE),"Three incident faces diagnosed");
        var bowtie=EditorMeshTopology.build(mesh(t("part",o,a,b),t("part",o,c,v(-1,0,0))));
        check(bowtie.issues().stream().anyMatch(i->i.code()==EditorMeshTopology.Code.NON_MANIFOLD_VERTEX)&&bowtie.islands()==2,"Disconnected vertex fans diagnosed");
        var flat=EditorMeshTopology.build(mesh(t("part",o,a,v(2,0,0))));
        check(flat.issues().stream().anyMatch(i->i.code()==EditorMeshTopology.Code.DEGENERATE_FACE),"Collinear triangle diagnosed");
        var duplicate=EditorMeshTopology.build(mesh(t("part",o,o,b)));
        check(!duplicate.manifold(),"Repeated point rejected by diagnostics");
        var parts=new ArrayList<>(tetra.triangles());for(var face:tetra.triangles())parts.add(t("second",face.a(),face.b(),face.c()));
        var separate=EditorMeshTopology.build(new EditorMeshPipeline.Mesh("parts",parts,0));
        check(separate.manifold()&&separate.vertices().size()==8&&separate.islands()==2,"Named source parts retain independent ownership at coincident positions");
        check(EditorMeshTopology.build(new EditorMeshPipeline.Mesh("empty",List.of(),0)).islands()==0,"Empty bounded mesh");
        var many=new ArrayList<EditorMeshPipeline.Triangle>();for(int i=0;i<5000;i++)many.add(t("p"+i,o,a,v(2,0,0)));
        var bounded=EditorMeshTopology.build(new EditorMeshPipeline.Mesh("bounded",many,0));
        check(bounded.issueCount()==5000&&bounded.issues().size()==4096,"Bounded diagnostics retain actual total without unbounded issue payload");
        var again=EditorMeshTopology.build(square);check(again.edges().equals(open.edges())&&again.faces().equals(open.faces()),"Deterministic adjacency and islands");
        try{open.edges().clear();throw new AssertionError("Mutable graph");}catch(UnsupportedOperationException expected){}
        check(tetra.triangles().size()==4&&square.triangles().size()==2,"Source triangles untouched");
        System.out.println("PASS half-edge topology: twins/cycles, oriented closed/open manifolds, UV seams, part ownership, disconnected fans, degeneracy, edge errors and bounded deterministic diagnostics");
    }
}
