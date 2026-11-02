package org.beckenburg.editor;

import java.util.*;

public final class EditorMeshPickingTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static EditorTopologyProject.Corner c(long id,double u,double v){return new EditorTopologyProject.Corner(id,u,v);}
    private static EditorTopologyProject.State layers(EditorMeshPicking.Projection projection){
        double[] right,up,normal;
        switch(projection){
            case FRONT->{right=new double[]{1,0,0};up=new double[]{0,1,0};normal=new double[]{0,0,-1};}
            case TOP->{right=new double[]{1,0,0};up=new double[]{0,0,-1};normal=new double[]{0,1,0};}
            case SIDE->{right=new double[]{0,0,1};up=new double[]{0,1,0};normal=new double[]{1,0,0};}
            default->{right=new double[]{.83,0,-.55};up=new double[]{.24,.95,.35};normal=new double[]{-.5225,.4225,-.7885};}
        }
        var vertices=new ArrayList<EditorTopologyProject.Vertex>();
        for(int layer=0;layer<2;layer++)for(int i=0;i<3;i++){
            double a=i==1?1:0,b=i==2?1:0,d=layer==0?-1:1;
            vertices.add(new EditorTopologyProject.Vertex(100+layer*3+i,"shell",a*right[0]+b*up[0]+d*normal[0],a*right[1]+b*up[1]+d*normal[1],a*right[2]+b*up[2]+d*normal[2]));
        }
        return new EditorTopologyProject.State("actual-project","0".repeat(64),"layers",0,vertices,List.of(
            new EditorTopologyProject.Face(42,"shell",c(100,0,0),c(101,1,0),c(102,0,1)),
            new EditorTopologyProject.Face(7,"shell",c(103,0,0),c(104,1,0),c(105,0,1))));
    }
    public static void main(String[] args){
        for(var projection:EditorMeshPicking.Projection.values()){
            var state=layers(projection);var a=state.vertices().get(3);var b=state.vertices().get(4);var c=state.vertices().get(5);
            var pa=projection.project(a.x(),a.y(),a.z());var pb=projection.project(b.x(),b.y(),b.z());var pc=projection.project(c.x(),c.y(),c.z());
            double x=.8*pa.x()+.15*pb.x()+.05*pc.x(),y=.8*pa.y()+.15*pb.y()+.05*pc.y();
            var hit=EditorMeshPicking.pick(state,projection,x,y).orElseThrow();
            check(hit.faceId()==7,"Nearest triangle wins over earlier hidden triangle: "+projection);
            check(hit.vertexId()==103&&hit.edgeA()==103&&hit.edgeB()==104,"Native stable vertex and edge IDs: "+projection);
            check(Math.abs(hit.u()-.15)<1e-12&&Math.abs(hit.v()-.05)<1e-12,"Actual barycentric corner UV");
            check(EditorMeshPicking.pick(state,projection,100,100).isEmpty(),"Miss is empty, not nearest scene node");
            var reversed=new EditorTopologyProject.State(state.sceneId(),state.sourceFingerprint(),state.name(),0,state.vertices(),List.of(state.faces().get(1),state.faces().get(0)));
            check(EditorMeshPicking.pick(reversed,projection,x,y).orElseThrow().equals(hit),"Result independent of traversal order");
            check(EditorMeshPicking.pick(state,projection,pa.x(),pa.y()).orElseThrow().vertexId()==103,"Triangle boundary hit");
            var prepared=EditorMeshPicking.prepare(state,projection);
            check(prepared.verticesInBox(-10,-10,10,10,false).equals(Set.of(103L,104L,105L)),"Visible-only box excludes exact overlapping back layer in "+projection);
            check(prepared.verticesInBox(-10,-10,10,10,true).equals(Set.of(100L,101L,102L,103L,104L,105L)),"X-ray box retains hidden native vertices");
            var near=prepared.verticesInBox(pa.x()-1e-6,pa.y()-1e-6,pa.x()+1e-6,pa.y()+1e-6,false);
            check(near.equals(Set.of(103L)),"Tight native vertex box");
            var lasso=List.of(new EditorMeshPicking.Point(pa.x()-.01,pa.y(),0),new EditorMeshPicking.Point(pa.x(),pa.y()-.01,0),new EditorMeshPicking.Point(pa.x()+.01,pa.y(),0),new EditorMeshPicking.Point(pa.x(),pa.y()+.01,0));
            check(prepared.verticesInLasso(lasso,false).equals(Set.of(103L)),"Visible lasso uses exact triangle occlusion");
            check(prepared.verticesInLasso(lasso,true).equals(Set.of(100L,103L)),"X-ray lasso keeps overlapping IDs");
            try{prepared.verticesInLasso(lasso.subList(0,2),true);throw new AssertionError("Short lasso");}catch(IllegalArgumentException expected){}
            try{near.add(99L);throw new AssertionError("Mutable selection");}catch(UnsupportedOperationException expected){}
            var empty=new EditorTopologyProject.State(state.sceneId(),state.sourceFingerprint(),state.name(),0,List.of(),List.of());
            check(EditorMeshPicking.pick(empty,projection,0,0).isEmpty(),"Empty native mesh");
        }
        var state=layers(EditorMeshPicking.Projection.FRONT);
        var concave=List.of(new EditorMeshPicking.Point(-.2,-1.2,0),new EditorMeshPicking.Point(1.2,-1.2,0),new EditorMeshPicking.Point(1.2,.2,0),new EditorMeshPicking.Point(.2,.2,0),new EditorMeshPicking.Point(.2,-.2,0),new EditorMeshPicking.Point(-.2,-.2,0));
        check(EditorMeshPicking.prepare(state,EditorMeshPicking.Projection.FRONT).verticesInLasso(concave,false).equals(Set.of(104L,105L)),"Concave lasso excludes native corner inside bounding box but outside polygon");
        try{EditorMeshPicking.prepare(state,EditorMeshPicking.Projection.FRONT).verticesInLasso(List.of(new EditorMeshPicking.Point(0,0,0),new EditorMeshPicking.Point(1,0,0),new EditorMeshPicking.Point(2,0,0)),true);throw new AssertionError("Collinear lasso");}catch(IllegalArgumentException expected){}
        check(EditorMeshPicking.pick(state,EditorMeshPicking.Projection.SIDE,0,0).isEmpty(),"Edge-on triangles cannot masquerade as surfaces");
        try{EditorMeshPicking.pick(state,EditorMeshPicking.Projection.FRONT,Double.NaN,0);throw new AssertionError("Nonfinite pick");}catch(IllegalArgumentException expected){}
        var crowdedVertices=new ArrayList<EditorTopologyProject.Vertex>();var crowdedFaces=new ArrayList<EditorTopologyProject.Face>();
        for(int i=0;i<2500;i++){
            long base=(long)i*3;crowdedVertices.add(new EditorTopologyProject.Vertex(base,"crowded",0,0,0));crowdedVertices.add(new EditorTopologyProject.Vertex(base+1,"crowded",1,0,0));crowdedVertices.add(new EditorTopologyProject.Vertex(base+2,"crowded",0,1,0));
            crowdedFaces.add(new EditorTopologyProject.Face(i,"crowded",c(base,0,0),c(base+1,1,0),c(base+2,0,1)));
        }
        var crowded=new EditorTopologyProject.State("crowded","0".repeat(64),"crowded",0,crowdedVertices,crowdedFaces);
        var bounded=EditorMeshPicking.prepare(crowded,EditorMeshPicking.Projection.FRONT);
        try{bounded.verticesInBox(-1,-2,2,1,false);throw new AssertionError("Unbounded overlapping selection");}
        catch(IllegalArgumentException expected){check(expected.getMessage().contains("budget"),"Structured bounded-work refusal");}
        check(bounded.verticesInBox(-1,-2,2,1,true).size()==7500,"X-ray remains exact without occlusion work");
        if(args.length>0){
            var vertices=new ArrayList<EditorTopologyProject.Vertex>();var faces=new ArrayList<EditorTopologyProject.Face>();
            for(int i=0;i<EditorMeshPipeline.MAX_TRIANGLES;i++){
                long base=(long)i*3;double x=i*2;
                vertices.add(new EditorTopologyProject.Vertex(base,"stress",x,0,0));vertices.add(new EditorTopologyProject.Vertex(base+1,"stress",x+1,0,0));vertices.add(new EditorTopologyProject.Vertex(base+2,"stress",x,1,0));
                faces.add(new EditorTopologyProject.Face(i,"stress",c(base,0,0),c(base+1,1,0),c(base+2,0,1)));
            }
            var max=new EditorTopologyProject.State("max-native","0".repeat(64),"stress",0,vertices,faces);vertices=null;faces=null;
            var prepared=EditorMeshPicking.prepare(max,EditorMeshPicking.Projection.FRONT);
            check(prepared.pick(200.25,-.25).orElseThrow().faceId()==100,"Maximum native triangle query");
            check(prepared.verticesInBox(-1,-2,EditorMeshPipeline.MAX_TRIANGLES*2,1,false).size()==EditorMeshPipeline.MAX_TRIANGLES*3,"All maximum native vertices visible under bounded query work");
        }
        System.out.println("PASS actual indexed picking and visible/X-ray boxes: four projections, overlapping surface visibility, stable IDs/UV, boundaries, misses, edge-on rejection and immutable selection");
    }
}
