package org.beckenburg.editor;

import java.nio.file.*;
import java.util.*;

public final class EditorTopologyProjectTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static EditorMeshPipeline.Vertex v(double x,double y){return new EditorMeshPipeline.Vertex(x,y,0,x,y);}
    public static void main(String[] args)throws Exception{
        Path home=Files.createTempDirectory("native-topology-project-");
        try{
            var source=new EditorMeshPipeline.Mesh("source.obj",List.of(
                new EditorMeshPipeline.Triangle("part",v(0,0),v(1,0),v(0,1)),
                new EditorMeshPipeline.Triangle("part",v(2,0),v(3,0),v(2,1))),117);
            var project=EditorTopologyProject.fromMesh(source,EditorChange.fingerprint("actual fixture source"),home.resolve("model.btop"));
            var history=new EditorHistory<>(project,60);var initial=project.snapshot();project.save();byte[] first=Files.readAllBytes(project.documentPath());
            check(project.mesh().equals(source),"Imported geometry, named parts and corner UVs roundtrip without editing");
            long id=initial.vertices().get(0).id();history.execute(()->project.moveVertex(id,2,0,0));
            var edited=project.snapshot();
            project.moveVertex(id,2,-0.0,0);check(project.topology().manifold(),"Signed zero is the same geometric position without losing indexed identity");project.restore(edited);
            check(project.topology().vertices().size()==6&&project.topology().islands()==2&&project.topology().manifold(),"Coincident positions preserve distinct indexed vertex identities");
            check(EditorMeshTopology.build(project.mesh()).vertices().size()==5,"Negative control: geometric regrouping would silently weld explicit IDs");
            check(initial.faces().equals(edited.faces()),"Vertex edit preserves face IDs, part ownership and every corner UV");
            check(history.undo()&&project.snapshot().equals(initial),"Shared history restores original indexed topology");
            check(history.redo()&&project.snapshot().equals(edited),"Shared redo restores same vertex IDs and native geometry");
            try{history.execute(()->project.moveVertex(id,1,0,0));throw new AssertionError("Degenerate move accepted");}catch(IllegalArgumentException expected){}
            check(project.snapshot().equals(edited),"Rejected degenerate move preserves exact authoring state");
            project.save();byte[] second=Files.readAllBytes(project.documentPath());project.save();
            check(Arrays.equals(second,Files.readAllBytes(project.documentPath()))&&Arrays.equals(first,Files.readAllBytes(EditorDocumentStore.previous(project.documentPath()))),"Native binary is deterministic and retains distinct prior state");
            var fresh=EditorTopologyProject.open(project.documentPath());check(fresh.snapshot().equals(edited)&&fresh.mesh().equals(project.mesh()),"Fresh native open preserves full indexed topology");
            Files.writeString(project.documentPath(),"corrupt topology");
            try{project.reopen();throw new AssertionError("Corrupt topology accepted");}catch(java.io.IOException expected){}
            check(project.snapshot().equals(edited),"Corrupt reopen cannot replace live mesh");
            history.execute(()->{try{project.recoverPrevious();}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}});
            check(project.snapshot().equals(initial),"Explicit recovery reloads same project object");history.undo();
            check(project.snapshot().equals(edited)&&Arrays.equals(first,Files.readAllBytes(project.documentPath())),"Recovery undo restores live mesh without overwriting recovered disk");
            var foreign=EditorTopologyProject.fromMesh(source,initial.sourceFingerprint(),home.resolve("foreign.btop"));foreign.save();
            Files.copy(foreign.documentPath(),EditorDocumentStore.previous(project.documentPath()),StandardCopyOption.REPLACE_EXISTING);
            try{project.recoverPrevious();throw new AssertionError("Foreign topology backup accepted");}catch(java.io.IOException expected){}
            check(project.snapshot().equals(edited)&&Arrays.equals(first,Files.readAllBytes(project.documentPath())),"Foreign ID backup preserves native authoring/disk bytes");
            byte[] future=first.clone();future[7]=2;Files.write(project.documentPath(),future);
            try{project.reopen();throw new AssertionError("Unknown future version accepted");}catch(java.io.IOException expected){}
            check(project.snapshot().equals(edited)&&Arrays.equals(future,Files.readAllBytes(project.documentPath())),"Unsupported native version retained without reinterpretation");
            Files.write(project.documentPath(),first);
            var shared=EditorTopologyProject.fromMesh(source,initial.sourceFingerprint(),home.resolve("shared.btop"));shared.save();
            var firstEditor=EditorTopologyProject.open(shared.documentPath());var secondEditor=EditorTopologyProject.open(shared.documentPath());
            firstEditor.moveVertex(id,-.25,0,0);firstEditor.save();byte[] newer=Files.readAllBytes(shared.documentPath());
            secondEditor.moveVertex(id,-.5,0,0);
            try{secondEditor.save();throw new AssertionError("Stale native editor overwrote newer saved topology");}catch(java.io.IOException expected){}
            check(Arrays.equals(newer,Files.readAllBytes(shared.documentPath()))&&secondEditor.snapshot().vertices().get(0).x()==-.5,"External revision rejection preserves newer disk and unsaved live mesh");
            secondEditor.reopen();check(secondEditor.snapshot().equals(firstEditor.snapshot()),"Explicit reopen observes actual newer native state");
            secondEditor.moveVertex(id,-.5,0,0);secondEditor.save();check(EditorTopologyProject.open(shared.documentPath()).snapshot().equals(secondEditor.snapshot()),"Save succeeds after observing actual disk revision");
            String part="x".repeat(512);var vertices=new ArrayList<EditorTopologyProject.Vertex>();var faces=new ArrayList<EditorTopologyProject.Face>();
            for(int i=0;i<10000;i++){
                long base=i*3;vertices.add(new EditorTopologyProject.Vertex(base,part,i*2,0,0));
                vertices.add(new EditorTopologyProject.Vertex(base+1,part,i*2+1,0,0));vertices.add(new EditorTopologyProject.Vertex(base+2,part,i*2,1,0));
                faces.add(new EditorTopologyProject.Face(i,part,new EditorTopologyProject.Corner(base,0,0),new EditorTopologyProject.Corner(base+1,1,0),new EditorTopologyProject.Corner(base+2,0,1)));
            }
            project.restore(new EditorTopologyProject.State(initial.sceneId(),initial.sourceFingerprint(),initial.name(),initial.sourceBytes(),vertices,faces));
            try{project.save();throw new AssertionError("Oversize topology wrote past document budget");}catch(java.io.IOException expected){}
            check(Arrays.equals(first,Files.readAllBytes(project.documentPath()))&&!Files.exists(EditorDocumentStore.journal(project.documentPath())),"Budget failure preserves valid native source without pending save");
            System.out.println("PASS editable native topology: stable indexed vertices/faces, no implicit welding, UV/part preservation, degenerate refusal, shared undo, deterministic save/fresh open/recovery, ownership/version controls and bounded output");
        }finally{try(var paths=Files.walk(home)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
}
