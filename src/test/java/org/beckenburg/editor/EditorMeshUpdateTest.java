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
    /** Change both faces in each selected pair, so the seam is encountered from both directions. */
    private static State pairedSeams(State base, int first, int stride) {
        var faces = new ArrayList<>(base.faces());
        int pairs = faces.size() / 2;
        for (int i = first; i < pairs; i += stride) {
            var a = faces.get(i);
            var b = faces.get(pairs + i);
            faces.set(i, a.withCorners(a.a(), new Corner(a.b().vertex(), .25, 0), a.c()));
            faces.set(pairs + i, b.withCorners(new Corner(b.a().vertex(), .5, 0), b.b(), b.c()));
        }
        return new State(base.sceneId(), base.sourceFingerprint(), base.name(), base.sourceBytes(), base.vertices(), faces);
    }
    private static void checkDistantTwins() {
        int pairs = 347;
        var doc = EditorTopologyProject.fromMesh(new EditorMeshPipeline.Mesh("distant", List.of(), 0),
            "0".repeat(64), Path.of("distant.btop"));
        var seed = doc.snapshot();
        var vertices = new ArrayList<Vertex>();
        var firstFaces = new ArrayList<Face>();
        var secondFaces = new ArrayList<Face>();
        for (int i = 0; i < pairs; i++) {
            int v = vertices.size(), x = 2 * i;
            vertices.add(new Vertex(v, "grid", x, 0, 0));
            vertices.add(new Vertex(v + 1, "grid", x + 1, 0, 0));
            vertices.add(new Vertex(v + 2, "grid", x, 1, 0));
            vertices.add(new Vertex(v + 3, "grid", x + 1, 1, 0));
            firstFaces.add(face(i, v, v + 1, v + 2));
            secondFaces.add(new Face(pairs + i, "grid", new Corner(v + 1, 1, 0),
                new Corner(v + 3, 1, 1), new Corner(v + 2, 0, 1)));
        }
        firstFaces.addAll(secondFaces);
        var initial = new State(seed.sceneId(), seed.sourceFingerprint(), "distant", 0, vertices, firstFaces);
        doc.restore(initial);
        var base = doc.observe();
        var edges = base.topology().edges();
        for (int i = 0; i < pairs; i++) {
            if (edges.get(3 * i + 1).twin() != 3 * (pairs + i) + 2)
                throw new AssertionError("Expected distant paired half-edges at square " + i);
        }
        doc.restore(pairedSeams(initial, 0, 1));
        var dense = doc.observe(base);
        doc.restore(pairedSeams(initial, 0, 2));
        var evens = doc.observe(dense);
        doc.restore(pairedSeams(initial, 1, 2));
        var odds = doc.observe(dense);
        doc.restore(pairedSeams(initial, pairs - 1, pairs));
        var single = doc.observe(base);
        if (base.topology().uvSeams() != 0 || dense.topology().uvSeams() != pairs ||
                evens.topology().uvSeams() != (pairs + 1) / 2 || odds.topology().uvSeams() != pairs / 2 ||
                single.topology().uvSeams() != 1 || single.topology().edges().get(1) != edges.get(1))
            throw new AssertionError("A divergent edit changed seam totals or counted both twins");
        try {
            odds.topology().edges().set(1, edges.get(0));
            throw new AssertionError("A published half-edge was writable");
        } catch (UnsupportedOperationException expected) { }
        // Recheck every retained observation against a fresh, independent full rebuild after both branches exist.
        for (var observation : List.of(base, dense, evens, odds, single)) {
            doc.restore(observation.snapshot());
            assertFullParity(doc, observation);
        }
        System.out.println("PASS 347 distant twin pairs: dense, alternating and single-pair edits preserve five native snapshots");
    }
    /** Compare incremental observations with independent full native derivations across
     * interleaved attribute, geometry, connectivity and shared-history changes. */
    private static void checkMixedUpdates() {
        var doc=EditorTopologyProject.fromMesh(new EditorMeshPipeline.Mesh("mixed",List.of(),0),
            "0".repeat(64),Path.of("mixed.btop"));
        var empty=doc.snapshot();
        var points=List.of(new Vertex(0,"grid",0,0,0),new Vertex(1,"grid",1,0,0),
            new Vertex(2,"grid",0,1,0),new Vertex(3,"grid",1,1,0));
        var first=face(0,0,1,2);
        var second=new Face(1,"grid",new Corner(1,1,0),new Corner(3,1,1),new Corner(2,0,1));
        doc.restore(new State(empty.sceneId(),empty.sourceFingerprint(),"mixed",0,points,List.of(first,second)));
        var history=new EditorHistory<>(doc,60);
        var observed=doc.observe();
        var random=new Random(7891);
        for (int step=0;step<96;step++) {
            var priorMesh=observed.mesh();
            var priorGraph=observed.topology();
            var priorTriangles=new ArrayList<>(priorMesh.triangles());
            var current=doc.observe(observed);
            if (current.mesh()!=priorMesh || current.topology()!=priorGraph)
                throw new AssertionError("Unchanged observations must retain both derived caches");
            var before=doc.snapshot();
            var vertices=new ArrayList<>(before.vertices());
            var faces=new ArrayList<>(before.faces());
            if (step%4==0) {
                var v=vertices.get(3);
                vertices.set(3,new Vertex(v.id(),v.part(),v.x(),v.y(),random.nextDouble()));
            } else if (step%4==1) {
                // Change both sides of a shared edge in one native revision.
                for (int i=0;i<faces.size();i++) {
                    var f=faces.get(i);
                    faces.set(i,f.withCorners(new Corner(f.a().vertex(),random.nextDouble(),0),f.b(),f.c()));
                }
            } else if (step%4==2) {
                var f=faces.get(0);
                faces.set(0,f.withCorners(new Corner(f.a().vertex(),f.a().u(),f.a().v(),
                    new EditorMeshPipeline.Normal(0,0,2)),f.b(),f.c()));
            } else {
                // Same face count with different indexed connectivity must use a full rebuild.
                var f=faces.get(0);
                faces.set(0,f.c().vertex()==2 ? face(0,0,1,3) : face(0,0,1,2));
            }
            var next=new State(before.sceneId(),before.sourceFingerprint(),before.name(),0,step%4==0 ? vertices : before.vertices(),faces);
            history.execute(()->doc.restore(next));
            observed=doc.observe(observed);
            assertFullParity(doc,observed);
            if (step%7==0) {
                if (!history.undo()) throw new AssertionError("Missing native undo");
                observed=doc.observe(observed);assertFullParity(doc,observed);
                if (!history.redo()) throw new AssertionError("Missing native redo");
                observed=doc.observe(observed);assertFullParity(doc,observed);
            }
            if (!priorMesh.triangles().equals(priorTriangles))
                throw new AssertionError("An old immutable mesh changed during an update");
        }
        System.out.println("PASS mixed incremental/full native parity: 96 edits, shared undo/redo, normals and paired UV seams");
    }
    private static void assertFullParity(EditorTopologyProject doc,Observation observed) {
        var expected=doc.topology();var actual=observed.topology();
        if (!observed.mesh().equals(doc.mesh()) || !actual.vertices().equals(expected.vertices()) ||
            !actual.edges().equals(expected.edges()) || !actual.faces().equals(expected.faces()) ||
            !actual.issues().equals(expected.issues()) || actual.issueCount()!=expected.issueCount() ||
            actual.uvSeams()!=expected.uvSeams() || actual.boundaryEdges()!=expected.boundaryEdges() ||
            actual.islands()!=expected.islands()) throw new AssertionError("Incremental/full native derivation mismatch");
    }
    private static void checkPreviewHandoff() {
        var doc=EditorTopologyProject.fromMesh(new EditorMeshPipeline.Mesh("review",List.of(),0),
            "0".repeat(64),Path.of("review.btop"));
        var seed=doc.snapshot();
        var points=List.of(new Vertex(2,"grid",0,0,0),new Vertex(8,"grid",1,0,0),
            new Vertex(4,"grid",0,1,0),new Vertex(3,"grid",1,1,0));
        var first=face(80,2,8,4);
        var second=new Face(17,"grid",new Corner(8,1,0),new Corner(3,1,1),new Corner(4,0,1));
        doc.restore(new State(seed.sceneId(),seed.sourceFingerprint(),"review",0,points,List.of(first,second)));
        var base=doc.observe();
        var mesh=base.mesh();
        var graph=base.topology();
        var history=doc.createHistory(8);
        var edit=doc.previewUvTransform(17,false,UvTransform.MOVE,.25,.5,false);
        var preview=edit.review(base);
        if (doc.isCurrent(preview) || preview.mesh().triangles().get(0)!=mesh.triangles().get(0)
                || preview.mesh().triangles().get(1)==mesh.triangles().get(1)
                || preview.topology().uvSeams()!=1 || graph.uvSeams()!=0)
            throw new AssertionError("Local UV review must preserve the live mesh and update the paired seam");
        history.execute(()->doc.applyPreview(edit));
        if (!doc.isCurrent(preview) || doc.observe(preview).mesh()!=preview.mesh())
            throw new AssertionError("Committed viewport must reuse the displayed candidate");
        assertFullParity(doc,preview);
        history.undo();
        if (doc.isCurrent(preview) || doc.isCurrent(base)) throw new AssertionError("Undo must invalidate both revisions");
        var stale=doc.previewUvTransform(80,true,UvTransform.MOVE,1,0,false);
        try {
            stale.review(base);
            throw new AssertionError("An old revision with the same state cannot authorize a preview");
        } catch (IllegalArgumentException expected) { }
        var islandBase=doc.observe(preview);
        var island=stale.review(islandBase);
        if (island.mesh().triangles().get(0)==mesh.triangles().get(0)
                || island.mesh().triangles().get(1)==mesh.triangles().get(1) || island.topology().uvSeams()!=0)
            throw new AssertionError("Both faces in the welded UV island must move together");
        doc.cancelPreview(stale);
        history.redo();
        if (doc.isCurrent(island) || doc.isCurrent(preview)) throw new AssertionError("Redo must respect revision identity");
        assertFullParity(doc,doc.observe(islandBase));
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
        checkPreviewHandoff();
        checkSeamUpdate();
        checkDistantTwins();
        checkPickingRefit();
        checkMixedUpdates();
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
