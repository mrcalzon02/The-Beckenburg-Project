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
        doc.restore(initial);
        var before=doc.observe();
        before.mesh();
        doc.restore(new State(initial.sceneId(),initial.sourceFingerprint(),"small",0,vertices,List.of(face(9,1,3,2))));
        var after=doc.observe(before);
        if (!after.mesh().equals(doc.mesh()) || !after.mesh().cornerVertexIds().equals(List.of(1L,3L,2L)))
            throw new AssertionError("Same count with new connectivity must rebuild the native corner bindings");
        doc.restore(initial);
        if (!doc.observe(after).mesh().equals(before.mesh())) throw new AssertionError("Restore lost original connectivity");
    }
    public static void main(String[] args) {
        checkConnectivityChange();
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
        project.restore(initial);
        var before=project.observe();
        var original=before.mesh();
        project.moveVertex(0,0,0,.25);
        var updated=project.observe(before).mesh();
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
