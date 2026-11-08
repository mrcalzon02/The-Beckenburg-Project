package org.beckenburg.editor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import static org.beckenburg.editor.EditorMeshEdges.Mode.*;

/** Open regular grids, closed cylinder strips, poles and independent triangle regions. */
public final class EditorMeshEdgesTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static EditorTopologyProject project(EditorMeshPipeline.Mesh mesh){
        return EditorTopologyProject.fromMesh(mesh,"0".repeat(64),Path.of("edges.btop"));
    }
    private static long at(EditorTopologyProject p,double x,double z){
        return p.snapshot().vertices().stream().filter(v->v.x()==x && v.z()==z).findFirst().orElseThrow().id();
    }
    private static void maximum(){
        int columns=300,rows=200;
        var vertices=new ArrayList<EditorTopologyProject.Vertex>();
        var faces=new ArrayList<EditorTopologyProject.Face>();var pairs=new ArrayList<EditorMeshQuad>();
        for(int y=0;y<=rows;y++)for(int x=0;x<=columns;x++)vertices.add(new EditorTopologyProject.Vertex(vertices.size(),"grid",x,y,0));
        for(int y=0;y<rows;y++)for(int x=0;x<columns;x++){
            long a=(long)y*(columns+1)+x,b=a+1,d=a+columns+1,c=d+1;
            var ca=new EditorTopologyProject.Corner(a,0,0);var cb=new EditorTopologyProject.Corner(b,1,0);
            var cc=new EditorTopologyProject.Corner(c,1,1);var cd=new EditorTopologyProject.Corner(d,0,1);
            int first=faces.size();faces.add(new EditorTopologyProject.Face(first,"grid",ca,cb,cc));
            faces.add(new EditorTopologyProject.Face(first+1,"grid",ca,cc,cd));pairs.add(new EditorMeshQuad(first,first+1));
        }
        var p=project(new EditorMeshPipeline.Mesh("grid",List.of(),0));var init=p.snapshot();
        p.restore(new EditorTopologyProject.State(init.sceneId(),init.sourceFingerprint(),"maximum grid",0,vertices,faces,List.of(),null,Map.of(),pairs));
        var observation=p.observe();long a=100L*(columns+1)+150;
        check(EditorMeshEdges.select(observation,LOOP,a,a+1).size()==columns,"Maximum grid interior loop");
        check(EditorMeshEdges.select(observation,RING,a,a+1).size()==rows+1,"Maximum grid ring");
    }
    public static void main(String[] args)throws Exception{
        var grid=project(EditorMeshPrimitives.preview(new EditorMeshPrimitives.Parameters(EditorMeshPrimitives.Kind.PLANE,4,1,4,4,4)));
        var obs=grid.observe();long a=at(grid,0,0),b=at(grid,1,0);
        var loop=EditorMeshEdges.select(obs,LOOP,a,b);check(loop.size()==4,"Interior grid loop reaches both sides");
        for(var edge:loop)check(edge.a().z()==0&&edge.b().z()==0,"Loop continues along the opposite vertex spoke");
        var ring=EditorMeshEdges.select(obs,RING,a,b);check(ring.size()==5,"Grid ring crosses four authored quads");
        for(var edge:ring)check(edge.a().z()==edge.b().z()&&Math.min(edge.a().x(),edge.b().x())==0&&Math.max(edge.a().x(),edge.b().x())==1,"Every ring edge is opposite in the same strip");
        var face=grid.snapshot().faces().get(0);
        try{EditorMeshEdges.select(obs,RING,face.a().vertex(),face.c().vertex());throw new AssertionError("Selected triangulation diagonal");}catch(IllegalArgumentException expected){}
        var cylinder=project(EditorMeshPrimitives.preview(new EditorMeshPrimitives.Parameters(EditorMeshPrimitives.Kind.CYLINDER,2,2,2,8,2)));
        var side=cylinder.snapshot().faces().get(0);
        var wrapped=EditorMeshEdges.select(cylinder.observe(),RING,side.a().vertex(),side.b().vertex());
        check(wrapped.size()==8&&new HashSet<>(wrapped).size()==8,"Closed cylinder ring terminates after eight distinct edges");
        var around=EditorMeshEdges.select(cylinder.observe(),LOOP,side.b().vertex(),side.c().vertex());
        check(around.size()==8,"Interior cylinder loop closes once");
        var split=project(EditorMeshPipeline.read("triangles.obj","v 0 0 0\nv 1 0 0\nv 1 1 0\nv 0 1 0\nf 1 2 3\nf 1 3 4\n".getBytes(StandardCharsets.UTF_8)));
        try{EditorMeshEdges.select(split.observe(),LOOP,0,1);throw new AssertionError("Inferred a quad from two triangles");}catch(IllegalArgumentException expected){}
        var cube=project(EditorMeshPrimitives.preview(new EditorMeshPrimitives.Parameters(EditorMeshPrimitives.Kind.BOX,2,2,2,8,1)));
        var cubeFace=cube.snapshot().faces().get(0);
        check(EditorMeshEdges.select(cube.observe(),LOOP,cubeFace.a().vertex(),cubeFace.b().vertex()).size()==1,"Three-quad poles terminate loops");
        var original=grid.snapshot();
        var removed=original.quads().stream().filter(q->{
            var f=original.faces().get(Math.toIntExact(q.first()));var t=original.faces().get(Math.toIntExact(q.second()));
            var corners=Set.of(f.a().vertex(),f.b().vertex(),f.c().vertex(),t.c().vertex());
            return corners.equals(Set.of(at(grid,0,0),at(grid,1,0),at(grid,1,1),at(grid,0,1)));
        }).findFirst().orElseThrow();
        var mixed=project(grid.mesh());var m=mixed.snapshot();
        mixed.restore(new EditorTopologyProject.State(m.sceneId(),m.sourceFingerprint(),m.name(),m.sourceBytes(),m.vertices(),m.faces(),m.materials(),m.glbSource(),m.frames(),m.quads().stream().filter(q->!q.equals(removed)).toList()));
        var stopped=EditorMeshEdges.select(mixed.observe(),LOOP,at(mixed,-1,0),at(mixed,0,0));
        check(stopped.size()==2 && stopped.stream().allMatch(e->e.a().x()<=0&&e.b().x()<=0),"Unpaired incident triangles stop continuation");
        if(args.length>0 && args[0].equals("--stress")) maximum();
        check(grid.isCurrent(obs),"Traversal leaves native state and revision unchanged");
        System.out.println("PASS authored quad edge traversal: regular interior and closed loops, opposite-edge rings, triangle/diagonal refusals and native revision stability");
    }
}
