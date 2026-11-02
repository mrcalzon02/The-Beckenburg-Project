package org.beckenburg.editor;

import java.nio.file.*;
import java.util.*;

/** Geometric controls, indexed native history and deterministic persisted primitive state. */
public final class EditorMeshPrimitivesTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void refused(Runnable action){try{action.run();throw new AssertionError("Expected rejection");}catch(IllegalArgumentException expected){}}
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("native-primitives-");
        try{
            var empty=new EditorMeshPipeline.Mesh("Primitive sample",List.of(),0);
            var project=EditorTopologyProject.fromMesh(empty,EditorMeshPipeline.sourceFingerprint(new byte[]{1}),root.resolve("sample.btop"));
            var history=new EditorHistory<>(project,20);var initial=project.snapshot();
            for(var kind:EditorMeshPrimitives.Kind.values()){
                var p=new EditorMeshPrimitives.Parameters(kind,2,3,4,12,4);
                var before=project.snapshot();var mesh=EditorMeshPrimitives.preview(p);
                check(project.snapshot()==before,"Preview never mutates project");check(mesh.equals(EditorMeshPrimitives.preview(p)),"Deterministic preview");
                var graph=EditorMeshTopology.build(mesh);
                check(graph.issueCount()==0,"Primitive manifold controls: "+kind+" "+graph.issues());
                check(graph.islands()==1,"One connected primitive");
                check(graph.boundaryEdges()==(kind==EditorMeshPrimitives.Kind.PLANE?16:0),"Exact closed shell or plane boundary: "+kind+" "+graph.summary());
                int e=graph.edges().size()/2; // Closed shells share every edge.
                if(kind!=EditorMeshPrimitives.Kind.PLANE)check(graph.vertices().size()-e+graph.faces().size()==(kind==EditorMeshPrimitives.Kind.TORUS?0:2),"Euler topology: "+kind);
                for(var t:mesh.triangles()){
                    var a=t.a();var b=t.b();var c=t.c();double x=b.x()-a.x(),y=b.y()-a.y(),z=b.z()-a.z(),u=c.x()-a.x(),v=c.y()-a.y(),w=c.z()-a.z();
                    double nx=y*w-z*v,ny=z*u-x*w,nz=x*v-y*u,cx=(a.x()+b.x()+c.x())/3,cy=(a.y()+b.y()+c.y())/3,cz=(a.z()+b.z()+c.z())/3;
                    double facing;
                    if(kind==EditorMeshPrimitives.Kind.PLANE)facing=ny;
                    else if(kind==EditorMeshPrimitives.Kind.TORUS){double angle=Math.atan2(cz/4,cx/2);facing=nx*(cx-.375*2*Math.cos(angle))+ny*cy+nz*(cz-.375*4*Math.sin(angle));}
                    else facing=nx*cx+ny*cy+nz*cz;
                    check(facing>0,"Every triangle outward facing: "+kind);
                    for(var q:List.of(a,b,c))check(q.u()>=0&&q.u()<=1&&q.v()>=0&&q.v()<=1,"Bounded corner UV");
                }
                history.execute(()->project.appendPrimitive(p));var after=project.snapshot();
                check(after.vertices().subList(0,before.vertices().size()).equals(before.vertices())&&after.faces().subList(0,before.faces().size()).equals(before.faces()),"Appending preserves previous IDs and corner data");
                check(project.topology().islands()==kind.ordinal()+1,"Separate primitive parts cannot weld even at coincident coordinates");
                check(history.undo()&&project.snapshot().equals(before),"Apply undo");check(history.redo()&&project.snapshot().equals(after),"Apply redo");
                project.save();byte[] bytes=project.snapshotBytes();check(Arrays.equals(bytes,Files.readAllBytes(project.documentPath())),"Canonical native bytes");
                check(EditorTopologyProject.open(project.documentPath()).snapshot().equals(after),"Fresh native reopen");
            }
            for(var kind:EditorMeshPrimitives.Kind.values()){
                refused(()->new EditorMeshPrimitives.Parameters(kind,Double.NaN,1,1,12,4));
                refused(()->new EditorMeshPrimitives.Parameters(kind,0,1,1,12,4));
                refused(()->new EditorMeshPrimitives.Parameters(kind,1,1,1,129,4));
            }
            refused(()->new EditorMeshPrimitives.Parameters(EditorMeshPrimitives.Kind.SPHERE,1,1,1,12,1));
            refused(()->new EditorMeshPrimitives.Parameters(EditorMeshPrimitives.Kind.TORUS,1,1,1,12,2));
            for(var kind:EditorMeshPrimitives.Kind.values()){
                var max=EditorMeshPrimitives.preview(new EditorMeshPrimitives.Parameters(kind,.0001,10000,1,128,64));
                check(EditorMeshTopology.build(max).issueCount()==0,"Maximum bounded primitive under 256 MiB: "+kind);
            }
            while(history.undo()){}check(project.snapshot().equals(initial),"All primitive commits undo to original empty project");
            System.out.println("PASS five deterministic native primitives, exact topology/Euler/boundaries, outward winding, UVs, budgets, stable append IDs, shared history and native save/reopen");
        }finally{try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
}
