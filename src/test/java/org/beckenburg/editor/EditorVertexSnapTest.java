package org.beckenburg.editor;

import java.nio.file.Path;
import java.util.*;
import org.beckenburg.editor.EditorTopologyProject.*;

public final class EditorVertexSnapTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void refuse(Runnable operation){try{operation.run();throw new AssertionError("Expected refusal");}catch(IllegalArgumentException expected){}}
    private static EditorTopologyProject project(){return EditorTopologyProject.fromMesh(new EditorMeshPipeline.Mesh("snap",List.of(),0),"0".repeat(64),Path.of("snap-test.btop"));}
    private static void precision(){
        var p=project();var initial=p.snapshot();double large=1e16;
        var points=List.of(new Vertex(0,"moving",large,0,0),new Vertex(1,"moving",large+4,0,0),new Vertex(2,"moving",large,2,0),new Vertex(3,"target",1,0,0));
        var face=new Face(0,"moving",new Corner(0,0,0),new Corner(1,1,0),new Corner(2,0,1));
        p.restore(new State(initial.sceneId(),initial.sourceFingerprint(),initial.name(),0,points,List.of(face)));
        var observed=p.observe();var aligned=p.previewVertexSnap(observed,Set.of(0L,1L,2L),0,3);
        check(aligned.snapshot().vertices().get(0).x()==1&&aligned.snapshot().vertices().get(1).x()==5,"Anchor is exact and neighboring offsets survive large coordinate cancellation");
        refuse(()->p.previewVertexSnap(observed,Set.of(0L),0,1));
        var flipped=new ArrayList<>(points);flipped.set(3,new Vertex(3,"target",large+8,0,0));
        p.restore(new State(initial.sceneId(),initial.sourceFingerprint(),initial.name(),0,flipped,List.of(face)));
        refuse(()->p.previewVertexSnap(p.observe(),Set.of(0L),0,3));
        refuse(()->p.applyPreview(aligned));
    }
    private static void maximum(){
        var p=project();var initial=p.snapshot();var points=new ArrayList<Vertex>();var faces=new ArrayList<Face>();var selected=new HashSet<Long>();
        for(int i=0;i<EditorMeshPipeline.MAX_TRIANGLES;i++){
            long id=(long)i*3;boolean moving=i<EditorMeshPipeline.MAX_TRIANGLES-1;String part=moving?"moving":"target";double x=i*4;
            points.add(new Vertex(id,part,x,0,0));points.add(new Vertex(id+1,part,x+1,0,0));points.add(new Vertex(id+2,part,x,1,0));
            faces.add(new Face(i,part,new Corner(id,0,0),new Corner(id+1,1,0),new Corner(id+2,0,1)));
            if(moving){selected.add(id);selected.add(id+1);selected.add(id+2);}
        }
        p.restore(new State(initial.sceneId(),initial.sourceFingerprint(),"maximum",0,points,faces));
        long target=points.size()-3;var after=p.previewVertexSnap(p.observe(),selected,0,target).snapshot();
        check(after.vertices().size()==360000&&after.faces().size()==120000&&after.vertices().get(0).x()==points.get((int)target).x(),"Max native part snap under 256MiB");
    }
    public static void main(String[] args)throws Exception{
        precision();
        var p=EditorTopologyProject.fromGlb("source.glb",EditorGlbAuthoringTest.fixture(),Path.of("source-snap.btop"));var before=p.snapshot();
        var moving=before.vertices().get(0);var ids=new HashSet<Long>();for(var v:before.vertices())if(v.part().equals(moving.part()))ids.add(v.id());
        var target=before.vertices().stream().filter(v->!ids.contains(v.id())).findFirst().orElseThrow();
        var edit=p.previewVertexSnap(p.observe(),ids,moving.id(),target.id());p.applyPreview(edit);
        var anchor=p.observe().vertex(moving.id());check(anchor.x()==target.x()&&anchor.y()==target.y()&&anchor.z()==target.z(),"Source node anchor matches target exactly");
        check(p.snapshot().glbSource()==before.glbSource()&&p.snapshot().materials().equals(before.materials())&&p.snapshot().faces().equals(before.faces()),"Source document, materials and corner attributes preserved");
        EditorGlbWriter.validate(p.mesh(),EditorGlbWriter.encode(p.mesh()));
        if(args.length>0&&args[0].equals("--stress"))maximum();
        System.out.println("PASS vertex snap: exact anchor/relative precision, degenerate/flip/stale refusal, original source GLB and native frame ownership");
    }
}
