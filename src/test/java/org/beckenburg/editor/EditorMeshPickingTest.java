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
            var empty=new EditorTopologyProject.State(state.sceneId(),state.sourceFingerprint(),state.name(),0,List.of(),List.of());
            check(EditorMeshPicking.pick(empty,projection,0,0).isEmpty(),"Empty native mesh");
        }
        var state=layers(EditorMeshPicking.Projection.FRONT);
        check(EditorMeshPicking.pick(state,EditorMeshPicking.Projection.SIDE,0,0).isEmpty(),"Edge-on triangles cannot masquerade as surfaces");
        try{EditorMeshPicking.pick(state,EditorMeshPicking.Projection.FRONT,Double.NaN,0);throw new AssertionError("Nonfinite pick");}catch(IllegalArgumentException expected){}
        System.out.println("PASS actual indexed triangle picking: four projections, nearest overlapping surface, stable face/edge/vertex IDs, interpolated UV, traversal order, boundaries, misses and edge-on rejection");
    }
}
