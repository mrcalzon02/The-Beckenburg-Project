package org.beckenburg.editor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;

public final class EditorModelWorkspaceTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private interface Step{void run()throws Exception;}
    private static void refuse(Step step)throws Exception{try{step.run();throw new AssertionError("Expected workspace refusal");}catch(IOException|IllegalArgumentException expected){}}
    private static EditorModelWorkspace view(double zoom) {
        return new EditorModelWorkspace(EditorMeshPicking.Projection.PERSPECTIVE,42,-12,zoom,.125,-.25,
                Map.of(EditorModelWorkspace.Field.TOOL,2d,EditorModelWorkspace.Field.MODE,1d),List.of(0L),0L,
                new EditorMeshPicking.Hit(0,0,0,1,1,.25,.5),0L,true);
    }
    public static void main(String[] args)throws Exception{
        var root=Files.createTempDirectory("native-workspace-");try{
            var mesh=EditorMeshPrimitives.preview(new EditorMeshPrimitives.Parameters(EditorMeshPrimitives.Kind.PLANE,2,2,2,8,1));
            var p=EditorTopologyProject.fromMesh(mesh,"3".repeat(64),root.resolve("model.btop"));p.save();
            check(ByteBuffer.wrap(p.snapshotBytes()).getInt(4)==6 && p.savedWorkspace()==null,"Legacy native geometry unchanged without view metadata");
            var state=p.snapshot();var observation=p.observe();p.save(view(2));
            byte[] before=Files.readAllBytes(p.documentPath());check(ByteBuffer.wrap(before).getInt(4)==7,"Saved workspace uses version7");
            var competing=EditorTopologyProject.open(p.documentPath());check(competing.savedWorkspace().equals(view(2))&&competing.snapshot().equals(state),"Fresh native file restores geometry and workspace together");
            check(p.isCurrent(observation)&&!p.hasUnsavedChanges(),"Saving a view preserves native geometry revision");
            p.save(view(3));refuse(()->competing.save(view(4)));check(competing.savedWorkspace().equals(view(2)),"Failed stale save leaves prior local workspace intact");
            competing.reopen();check(competing.savedWorkspace().equals(view(3)),"Reopen reads actual winning workspace");
            competing.recoverPrevious();check(competing.savedWorkspace().equals(view(2))&&competing.snapshot().equals(state),"Backup restores exact workspace/geometry pair");
            var damaged=before.clone();damaged[damaged.length-1]=2;refuse(()->EditorTopologyProject.decode(damaged));
            var staleId=before.clone();ByteBuffer.wrap(staleId).putLong(staleId.length-9,Long.MAX_VALUE);refuse(()->EditorTopologyProject.decode(staleId));
            var future=before.clone();ByteBuffer.wrap(future).putInt(4,12);refuse(()->EditorTopologyProject.decode(future));
            refuse(()->view(Double.NaN));refuse(()->view(50));
            System.out.println("PASS native workspace: version6/7, exact geometry/view save, fresh open, stale-writer protection, recovery, invalid ID/flag/future refusal");
        }finally{try(var paths=Files.walk(root)){for(var file:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(file);}}
    }
}
