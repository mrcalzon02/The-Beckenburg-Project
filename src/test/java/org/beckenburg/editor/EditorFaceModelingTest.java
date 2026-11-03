package org.beckenburg.editor;

import java.nio.file.*;
import java.util.*;

/** Native preview/apply modeling, stable IDs, UV continuity, manifold boundaries and persistence. */
public final class EditorFaceModelingTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void refused(Runnable action){try{action.run();throw new AssertionError("Invalid native edit accepted");}catch(IllegalArgumentException expected){}}
    private static EditorMeshPipeline.Vertex v(double x,double y,double z){return new EditorMeshPipeline.Vertex(x,y,z,x,y);}
    private static EditorMeshPipeline.Mesh plane(){return new EditorMeshPipeline.Mesh("plane",List.of(new EditorMeshPipeline.Triangle("surface",v(0,0,0),v(1,0,0),v(0,1,0))),0);}
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("native-face-modeling-");
        try{
            for(var operation:EditorTopologyProject.FaceEdit.values())for(double amount:operation==EditorTopologyProject.FaceEdit.EXTRUDE?new double[]{.25,-.25}:new double[]{.25,.75}){
                var project=EditorTopologyProject.fromMesh(plane(),"0".repeat(64),root.resolve(operation+"-"+amount+".btop"));
                var initial=project.snapshot();long revision=project.revision();var history=new EditorHistory<>(project,8);
                var preview=project.previewFaceEdit(0,operation,amount);
                var canceled=project.previewFaceEdit(0,operation,amount);project.cancelPreview(canceled);refused(()->project.applyPreview(canceled));
                check(project.snapshot()==initial&&project.revision()==revision&&!history.canUndo(),"Preview never changes native state/revision/history");
                check(preview.snapshot().vertices().size()==6&&preview.snapshot().faces().size()==7,"Exact preview counts");
                check(preview.topology().manifold()&&preview.topology().boundaryEdges()==3,"Native boundary count/orientation retained");
                check(preview.snapshot().vertices().subList(0,3).equals(initial.vertices()),"Original native vertex IDs and values unchanged");
                var cap=preview.snapshot().faces().stream().filter(f->f.id()==0).findFirst().orElseThrow();
                var positions=new HashMap<Long,EditorTopologyProject.Vertex>();for(var vertex:preview.snapshot().vertices())positions.put(vertex.id(),vertex);
                for(var corner:List.of(cap.a(),cap.b(),cap.c())){
                    var point=positions.get(corner.vertex());
                    if(operation==EditorTopologyProject.FaceEdit.EXTRUDE)check(point.z()==amount,"Extrude follows actual oriented normal, signed distance");
                    else check(point.z()==0&&corner.u()==point.x()&&corner.v()==point.y(),"Inset retains the original planar UV mapping");
                }
                if(operation==EditorTopologyProject.FaceEdit.EXTRUDE){var old=initial.faces().get(0);check(cap.a().u()==old.a().u()&&cap.a().v()==old.a().v()&&cap.b().u()==old.b().u()&&cap.b().v()==old.b().v()&&cap.c().u()==old.c().u()&&cap.c().v()==old.c().v(),"Extruded cap preserves all original corner UVs");}
                history.execute(()->project.applyPreview(preview));check(project.snapshot()==preview.snapshot(),"Apply installs the exact reviewed native candidate");
                check(history.undo()&&project.snapshot().equals(initial)&&history.redo()&&project.snapshot().equals(preview.snapshot()),"One native undo/redo reverses full topology edit");
                project.save();check(EditorTopologyProject.open(project.documentPath()).snapshot().equals(project.snapshot()),"New topology roundtrips through native codec");
                refused(()->project.applyPreview(preview));
                var stale=project.previewFaceEdit(0,operation,amount);var before=project.snapshot();project.restore(before);
                refused(()->project.applyPreview(stale));check(project.snapshot()==before,"Identical snapshot with newer native revision refuses stale candidate");
                var foreign=EditorTopologyProject.open(project.documentPath());refused(()->foreign.applyPreview(stale));
            }
            var closed=EditorTopologyProject.fromMesh(EditorMeshPrimitives.preview(new EditorMeshPrimitives.Parameters(EditorMeshPrimitives.Kind.BOX,1,1,1,8,1)),"1".repeat(64),root.resolve("closed.btop"));
            for(var op:EditorTopologyProject.FaceEdit.values()){
                var before=closed.snapshot();var preview=closed.previewFaceEdit(0,op,.2);
                check(preview.topology().manifold()&&preview.topology().boundaryEdges()==0&&preview.topology().islands()==1,"Closed shell remains an oriented closed shell");
                for(var f:before.faces())if(f.id()!=0)check(preview.snapshot().faces().contains(f),"Unselected native face IDs/connectivity/UVs unchanged");
            }
            refused(()->closed.previewFaceEdit(0,EditorTopologyProject.FaceEdit.INSET,1));refused(()->closed.previewFaceEdit(0,EditorTopologyProject.FaceEdit.INSET,0));
            refused(()->closed.previewFaceEdit(0,EditorTopologyProject.FaceEdit.EXTRUDE,0));refused(()->closed.previewFaceEdit(0,EditorTopologyProject.FaceEdit.EXTRUDE,Double.NaN));refused(()->closed.previewFaceEdit(-1,EditorTopologyProject.FaceEdit.EXTRUDE,1));
            var budget=EditorTopologyProject.fromMesh(plane(),"2".repeat(64),root.resolve("budget.btop"));var base=budget.snapshot();var many=new ArrayList<EditorTopologyProject.Face>();
            for(int i=0;i<EditorMeshPipeline.MAX_TRIANGLES;i++)many.add(new EditorTopologyProject.Face(i,"surface",base.faces().get(0).a(),base.faces().get(0).b(),base.faces().get(0).c()));
            budget.restore(new EditorTopologyProject.State(base.sceneId(),base.sourceFingerprint(),base.name(),0,base.vertices(),many));var full=budget.snapshot();
            refused(()->budget.previewFaceEdit(0,EditorTopologyProject.FaceEdit.EXTRUDE,.25));check(budget.snapshot()==full,"Topology budget refusal is atomic before geometry allocation");
            var observed=closed.observe();var vertex=closed.snapshot().vertices().get(0);closed.moveVertex(vertex.id(),vertex.x()+.1,vertex.y(),vertex.z());
            check(!closed.isCurrent(observed),"Native observation identifies actual changed epoch");
            try{closed.saveReviewed(observed);throw new AssertionError("Stale observed save");}catch(java.io.IOException expected){}
            check(!Files.exists(closed.documentPath()),"Stale native review refuses before disk writes");
            var current=closed.observe();var saved=closed.saveReviewed(current);byte[] returned=saved.bytes();returned[0]=0;
            check(EditorTopologyProject.decode(saved.bytes()).equals(current.snapshot()),"Saved review owns exact immutable reviewed bytes");
            check(EditorMeshPipeline.sourceFingerprint(saved.bytes()).equals(saved.diskFingerprint()),"Saved fingerprint is the actual encoded snapshot");
            closed.moveVertex(vertex.id(),vertex.x()+.2,vertex.y(),vertex.z());
            check(!closed.isCurrent(current)&&EditorTopologyProject.open(closed.documentPath()).snapshot().equals(current.snapshot()),"Later native edits cannot alter saved snapshot or falsely appear saved");
            check(closed.hasUnsavedChanges(),"Native source owns unsaved status independently of UI mirrors");closed.restore(current.snapshot());check(!closed.hasUnsavedChanges(),"Restoring saved value clears semantic dirtiness despite newer epoch");
            System.out.println("PASS native signed extrude/inset: immutable preview, exact reviewed apply, stable source/cap IDs, UV continuity, oriented open/closed topology, one-step history, stale/foreign refusal and native save/reopen");
        }finally{try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
}
