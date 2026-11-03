package org.beckenburg.editor;

import java.util.*;

/** Ray-equivalent UV/depth, visibility and exact projective axis dragging on real native triangles. */
public final class EditorPerspectivePickingTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void near(double a,double b){check(Math.abs(a-b)<1e-9,"Camera result "+a+" != "+b);}
    private static EditorTopologyProject.Corner c(long id,double u,double v){return new EditorTopologyProject.Corner(id,u,v);}
    public static void main(String[] args){
        try{EditorMeshPicking.class.getMethod("pick",EditorTopologyProject.State.class,EditorMeshPicking.Projection.class,double.class,double.class);EditorMeshPicking.class.getMethod("prepare",EditorTopologyProject.State.class,EditorMeshPicking.Projection.class);}catch(ReflectiveOperationException missing){throw new AssertionError("Existing compiled picker signatures must remain available",missing);}
        var camera=new EditorMeshPicking.Camera(0,0,0,1,0,0);
        var vertices=List.of(new EditorTopologyProject.Vertex(0,"native",0,0,1),new EditorTopologyProject.Vertex(1,"native",2,0,-1),new EditorTopologyProject.Vertex(2,"native",0,2,-1),new EditorTopologyProject.Vertex(3,"native",0,0,-1),new EditorTopologyProject.Vertex(4,"native",4,0,-5),new EditorTopologyProject.Vertex(5,"native",0,4,-5));
        var state=new EditorTopologyProject.State("perspective","0".repeat(64),"perspective",0,vertices,List.of(new EditorTopologyProject.Face(9,"native",c(3,0,0),c(4,1,0),c(5,0,1)),new EditorTopologyProject.Face(17,"native",c(0,0,0),c(1,1,0),c(2,0,1))));
        var prepared=EditorMeshPicking.prepare(state,camera);var hit=prepared.pick(.125,-.125).orElseThrow();check(hit.faceId()==17,"Nearest actual surface uses reciprocal depth, independent of face order");near(hit.depth(),.375);near(hit.u(),1d/6);near(hit.v(),1d/6);
        check(prepared.verticesInBox(-.1,-.6,.6,.1,false).equals(Set.of(0L,1L,2L)),"Visible box excludes back layer at identical projected corners");check(prepared.verticesInBox(-.1,-.6,.6,.1,true).size()==6,"X-ray preserves all native IDs");
        var lasso=List.of(new EditorMeshPicking.Point(-.1,-.6,0),new EditorMeshPicking.Point(.6,-.6,0),new EditorMeshPicking.Point(.6,.1,0),new EditorMeshPicking.Point(-.1,.1,0));check(prepared.verticesInLasso(lasso,false).equals(Set.of(0L,1L,2L)),"Perspective lasso shares actual depth visibility");check(prepared.pick(4,4).isEmpty(),"No proxy hit outside actual triangles");
        for(double yaw:new double[]{-140,0,35,179})for(double pitch:new double[]{-70,0,25,80})for(int axis=0;axis<3;axis++){
            var orbit=new EditorMeshPicking.Camera(0,0,0,2,yaw,pitch);double x=.2,y=.3,z=.4;var start=orbit.project(x,y,z);var direction=orbit.axisDirection(x,y,z,axis);if(Math.hypot(direction.x(),direction.y())<1e-12)continue;
            for(double delta:new double[]{-.5,.1,.7}){var end=orbit.project(x+(axis==0?delta:0),y+(axis==1?delta:0),z+(axis==2?delta:0));near(orbit.axisDelta(x,y,z,axis,end.x()-start.x(),end.y()-start.y()),delta);}
            near(orbit.axisDelta(x,y,z,axis,-direction.y(),direction.x()),0);
        }
        try{camera.axisDelta(.5,0,0,2,-1d/6,0);throw new AssertionError("Camera horizon drag accepted");}catch(IllegalArgumentException expected){}
        check(Double.isNaN(camera.project(0,0,3).depth()),"Near/behind eye cannot yield a drawable/pickable point");
        try{EditorMeshPicking.prepare(state,EditorMeshPicking.Projection.PERSPECTIVE);throw new AssertionError("Independent implicit camera accepted");}catch(IllegalArgumentException expected){}
        var sample=new EditorMeshPipeline.Vertex(1e9,2e9,3e9,0,0);var fit=EditorMeshPicking.camera(List.of(sample,new EditorMeshPipeline.Vertex(1e9+2,2e9+3,3e9+4,0,0)),35,25);check(Double.isFinite(fit.project(sample.x(),sample.y(),sample.z()).depth()),"Source-centered fitting handles large world offsets");
        check(state.vertices().equals(vertices),"All camera/selection operations leave native geometry exact");
        if(args.length>0){
            var all=new ArrayList<EditorTopologyProject.Vertex>();var faces=new ArrayList<EditorTopologyProject.Face>();var bounds=new ArrayList<EditorMeshPipeline.Vertex>();
            for(int i=0;i<EditorMeshPipeline.MAX_TRIANGLES;i++){long id=(long)i*3;double x=(i%400)*2,y=(i/400)*2;all.add(new EditorTopologyProject.Vertex(id,"maximum",x,y,0));all.add(new EditorTopologyProject.Vertex(id+1,"maximum",x+1,y,0));all.add(new EditorTopologyProject.Vertex(id+2,"maximum",x,y+1,0));faces.add(new EditorTopologyProject.Face(i,"maximum",c(id,0,0),c(id+1,1,0),c(id+2,0,1)));}
            for(var v:all)bounds.add(new EditorMeshPipeline.Vertex(v.x(),v.y(),v.z(),0,0));var maximum=new EditorTopologyProject.State("maximum","0".repeat(64),"maximum",0,all,faces);var fitted=EditorMeshPicking.camera(bounds,0,0);var query=EditorMeshPicking.prepare(maximum,fitted);check(query.verticesInBox(-1,-1,1,1,false).size()==360000,"Maximum120000 native faces/360000 vertices use the actual perspective BVH and visibility under256MiB");
        }
        System.out.println("PASS native perspective: reciprocal visibility, perspective-correct source UVs, box/lasso, orbit and exact axis inversion across depth, stable IDs and unchanged authority");
    }
}
