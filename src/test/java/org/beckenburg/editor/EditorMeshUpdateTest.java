package org.beckenburg.editor;

import java.nio.file.Path;
import java.util.*;
import org.beckenburg.editor.EditorTopologyProject.*;

public final class EditorMeshUpdateTest {
    private static Face face(long id,long a,long b,long c) {
        return new Face(id,"grid",new Corner(a,0,0),new Corner(b,1,0),new Corner(c,0,1));
    }
    private static void checkConnectivityChange() {
        var doc=EditorTopologyProject.fromMesh(new EditorMeshPipeline.Mesh("small",List.of(),0),
            "0".repeat(64),Path.of("small.btop"));
        var empty=doc.snapshot();
        var vertices=List.of(new Vertex(0,"grid",0,0,0),new Vertex(1,"grid",1,0,0),
            new Vertex(2,"grid",0,1,0),new Vertex(3,"grid",1,1,0));
        var initial=new State(empty.sceneId(),empty.sourceFingerprint(),"small",0,vertices,List.of(face(9,0,1,2)));
        int[] projections={0};
        EditorMeshPicking.Projector camera=(x,y,z)->{
            projections[0]++;
            return new EditorMeshPicking.Point(x,y,z);
        };
        var index=EditorMeshPicking.prepare(initial,camera);
        var face=new Face(12,"grid",new Corner(0,1,0),new Corner(1,2,0),new Corner(2,1,1));
        var changed=new State(initial.sceneId(),initial.sourceFingerprint(),"small",0,vertices,List.of(face));
        var refreshed=index.update(changed,camera);
        var hit=refreshed.pick(.25,.25).orElseThrow();
        var old=index.pick(.25,.25).orElseThrow();
        if (projections[0]!=4 || hit.faceId()!=12 || hit.u()!=1.25 || old.faceId()!=9 || old.u()!=.25)
            throw new AssertionError("Attribute updates must retain projections and use the requested face data");
        doc.restore(initial);
        var before=doc.observe();
        before.mesh();
        doc.restore(new State(initial.sceneId(),initial.sourceFingerprint(),"small",0,vertices,List.of(face(9,1,3,2))));
        var after=doc.observe(before);
        var rewired=refreshed.update(after.snapshot(),camera);
        if (projections[0]!=8 || rewired.pick(.25,.25).isPresent() || rewired.pick(.75,.75).isEmpty())
            throw new AssertionError("Connectivity change must replace the spatial index");
        if (!after.mesh().equals(doc.mesh()) || !after.mesh().cornerVertexIds().equals(List.of(1L,3L,2L)))
            throw new AssertionError("Same count with new connectivity must rebuild the native corner bindings");
        doc.restore(initial);
        if (!doc.observe(after).mesh().equals(before.mesh())) throw new AssertionError("Restore lost original connectivity");
    }
    private static void checkSeamUpdate() {
        var doc=EditorTopologyProject.fromMesh(new EditorMeshPipeline.Mesh("seams",List.of(),0),
            "0".repeat(64),Path.of("seams.btop"));
        var empty=doc.snapshot();
        var vertices=List.of(new Vertex(0,"grid",0,0,0),new Vertex(1,"grid",1,0,0),
            new Vertex(2,"grid",0,1,0),new Vertex(3,"grid",1,1,0));
        var first=face(0,0,1,2);
        var second=new Face(1,"grid",new Corner(1,1,0),new Corner(3,1,1),new Corner(2,0,1));
        var original=new State(empty.sceneId(),empty.sourceFingerprint(),"seams",0,vertices,List.of(first,second));
        doc.restore(original);
        var before=doc.observe();
        var noSeams=before.topology();
        var edit=new Face(1,"grid",new Corner(1,.5,0),second.b(),second.c());
        doc.restore(new State(original.sceneId(),original.sourceFingerprint(),"seams",0,vertices,List.of(first,edit)));
        var after=doc.observe(before);
        var withSeam=after.topology();
        var fresh=doc.topology();
        if (noSeams.uvSeams()!=0 || withSeam.uvSeams()!=1 || !fresh.edges().equals(withSeam.edges()) ||
            withSeam.edges().get(0)!=noSeams.edges().get(0) || withSeam.faces().get(0)!=noSeams.faces().get(0))
            throw new AssertionError("UV edits must update the twin seam and reuse unchanged adjacency");
        doc.restore(original);
        var restored=doc.observe(after).topology();
        if (restored.uvSeams()!=0 || !restored.edges().equals(noSeams.edges()))
            throw new AssertionError("Restoring UVs must clear both half-edge seams");
        var positions=new ArrayList<>(vertices);
        positions.set(3,new Vertex(3,"grid",1,1,.25));
        doc.restore(new State(original.sceneId(),original.sourceFingerprint(),"seams",0,positions,original.faces()));
        var shifted=doc.observe(before);
        var shiftedGraph=shifted.topology();
        if (!shiftedGraph.vertices().equals(doc.topology().vertices()) || shiftedGraph.edges()!=noSeams.edges() ||
            shiftedGraph.vertices().get(0)!=noSeams.vertices().get(0) || noSeams.vertices().get(3).z()!=0)
            throw new AssertionError("Vertex movement must retain unchanged positions and native adjacency");
        positions.set(3,new Vertex(3,"grid",1,0,0));
        doc.restore(new State(original.sceneId(),original.sourceFingerprint(),"seams",0,positions,original.faces()));
        var collapsed=doc.observe(shifted);
        if (collapsed.topology().manifold() || !collapsed.topology().issues().equals(doc.topology().issues()))
            throw new AssertionError("Collapsed face must report the full native diagnostic");
        doc.restore(original);
        if (!doc.observe(collapsed).topology().manifold()) throw new AssertionError("Repair retained stale diagnostics");
    }
    private static void checkPickingRefit() {
        var vertices=new ArrayList<Vertex>();
        var faces=new ArrayList<Face>();
        for (int i=0;i<32;i++) {
            int id=3*i;
            vertices.add(new Vertex(id,"grid",i,0,0));
            vertices.add(new Vertex(id+1,"grid",i+1,0,0));
            vertices.add(new Vertex(id+2,"grid",i,1,0));
            faces.add(face(i,id,id+1,id+2));
        }
        var initial=new State("refit","0".repeat(64),"refit",0,vertices,faces);
        int[] projections={0};
        EditorMeshPicking.Projector camera=(x,y,z)->{
            projections[0]++;
            return new EditorMeshPicking.Point(x,y,z);
        };
        var old=EditorMeshPicking.prepare(initial,camera);
        vertices.set(0,new Vertex(0,"grid",42,.5,1));
        var moved=new State(initial.sceneId(),initial.sourceFingerprint(),"refit",0,vertices,faces);
        var refit=old.update(moved,camera);
        if (projections[0]!=vertices.size()+1) throw new AssertionError("Local move must reproject one vertex");
        var fresh=EditorMeshPicking.prepare(moved,camera);
        for (double x=0;x<=42;x+=.25) for (double y=0;y<=1;y+=.125) {
            if (!refit.pick(x,y).equals(fresh.pick(x,y))) throw new AssertionError("Refitted pick differs at "+x+","+y);
        }
        if (!refit.verticesInBox(0,0,42,1,false).equals(fresh.verticesInBox(0,0,42,1,false)) ||
            old.pick(38,.5).isPresent() || refit.pick(38,.5).isEmpty())
            throw new AssertionError("Bounds or old index changed after refit");
    }
    private static void checkMaximumPicking(State initial) {
        int[] count={0};
        EditorMeshPicking.Projector camera=(x,y,z)->{
            count[0]++;
            return new EditorMeshPicking.Point(x,y,z);
        };
        var old=EditorMeshPicking.prepare(initial,camera);
        var moved=new ArrayList<>(initial.vertices());
        moved.set(0,new Vertex(0,"grid",-1,0,.25));
        var next=new State(initial.sceneId(),initial.sourceFingerprint(),"grid",0,moved,initial.faces());
        var refit=old.update(next,camera);
        if (count[0]!=initial.vertices().size()+1 || old.pick(-.5,.1).isPresent() || refit.pick(-.5,.1).isEmpty())
            throw new AssertionError("Maximum grid must expand the correct branch with one new projection");
        System.out.println("PASS 120000-face picking index: one vertex reprojected, old and refitted bounds verified");
    }
    public static void main(String[] args) {
        checkConnectivityChange();
        checkSeamUpdate();
        checkPickingRefit();
        var project=EditorTopologyProject.fromMesh(new EditorMeshPipeline.Mesh("grid",List.of(),0),
            "0".repeat(64),Path.of("grid.btop"));
        var empty=project.snapshot();
        var vertices=new ArrayList<Vertex>();
        var faces=new ArrayList<Face>();
        for (int y=0;y<=200;y++) for (int x=0;x<=300;x++) {
            long a=vertices.size();
            vertices.add(new Vertex(a,"grid",x,y,0));
            if (x<300 && y<200) {
                faces.add(face(faces.size(),a,a+1,a+302));
                faces.add(face(faces.size(),a,a+302,a+301));
            }
        }
        var initial=new State(empty.sceneId(),empty.sourceFingerprint(),"grid",0,vertices,faces);
        checkMaximumPicking(initial);
        project.restore(initial);
        var before=project.observe();
        var original=before.mesh();
        var graph=before.topology();
        project.moveVertex(0,0,0,.25);
        var observation=project.observe(before);
        var updated=observation.mesh();
        var movedGraph=observation.topology();
        if (graph.edges().size()!=360000 || movedGraph.edges()!=graph.edges() || !movedGraph.manifold() ||
            movedGraph.vertices().get(0).z()!=.25) throw new AssertionError("Maximum mesh did not reuse valid adjacency");
        int replacements=0, corners=0;
        for (int i=0;i<original.triangles().size();i++) {
            var prior=original.triangles().get(i);
            var next=updated.triangles().get(i);
            if (next!=prior) replacements++;
            if (prior.a()!=next.a()) corners++;
            if (prior.b()!=next.b()) corners++;
            if (prior.c()!=next.c()) corners++;
        }
        if (replacements!=2 || corners!=2 || !updated.equals(project.mesh()) ||
            original.triangles().get(0).a().z()!=0 || updated.triangles().get(0).a().z()!=.25)
            throw new AssertionError("Localized geometry update or immutable snapshot mismatch");
        System.out.println("PASS 120000 native triangles: moving one grid corner replaces two triangles and two corners");
    }
}
