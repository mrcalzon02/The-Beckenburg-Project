package org.beckenburg;

import org.beckenburg.editor.EditorAgentService;
import org.beckenburg.editor.EditorDocumentStore;
import org.beckenburg.editor.EditorHistory;
import java.nio.file.Files;
import java.util.Map;

/** Recover actual mission documents into the same live session and game map. */
public final class LevelAuthoringRecoveryTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        var home=Files.createTempDirectory("native-editor-recovery-");
        String previous=System.getProperty("user.home");System.setProperty("user.home",home.toString());
        try{
            var session=new LevelAuthoringSession(HouseMap.Site.HOUSE);var history=new EditorHistory<>(session,60);
            var service=new EditorAgentService<>(new LevelEditorAgent(session,()->new HouseMap(HouseMap.Site.HOUSE,session.level()),
                new SceneRenderer(),()->{}),history);
            var original=session.snapshot();service.save(session.revision());
            var p=session.prop(0);
            service.transact(session.revision(),"move-prop",Map.of("id",session.nodeId(0),"x",Double.toString(p.x()+.25),
                "y",Double.toString(p.y()),"z",Double.toString(p.z())));
            var moved=session.snapshot();service.save(session.revision());
            byte[] prior=Files.readAllBytes(EditorDocumentStore.previous(session.documentPath()));
            service.save(session.revision());
            check(java.util.Arrays.equals(prior,Files.readAllBytes(EditorDocumentStore.previous(session.documentPath()))),"No-op human/agent save retains prior real version");
            Files.writeString(session.documentPath(),"corrupt saved scene");
            try{service.reopen(session.revision());throw new AssertionError("Corruption silently hidden");}catch(java.io.IOException expected){}
            check(session.snapshot().equals(moved),"Corrupt file cannot replace unsaved native scene");
            service.recoverPrevious(session.revision());
            check(session.snapshot().equals(original),"Explicit recovery reloads prior props AND IDs into same authoring object");
            check(new HouseMap(HouseMap.Site.HOUSE).level().props().equals(original.props()),"Real game consumes recovered document");
            service.undo(session.revision());
            check(session.snapshot().equals(moved),"Shared undo restores pre-recovery live edits");
            check(new HouseMap(HouseMap.Site.HOUSE).level().props().equals(original.props()),"Undo does not silently save over recovered document");
            var school=new LevelAuthoringSession(HouseMap.Site.SCHOOL);school.save();
            Files.copy(school.documentPath(),EditorDocumentStore.previous(session.documentPath()),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            byte[] saved=Files.readAllBytes(session.documentPath());
            try{service.recoverPrevious(session.revision());throw new AssertionError("Foreign backup accepted");}catch(java.io.IOException expected){}
            check(session.snapshot().equals(moved)&&java.util.Arrays.equals(saved,Files.readAllBytes(session.documentPath())),
                "Foreign backup preserves exact live and on-disk native scene");
            check(service.test(session.revision(),"mission-runtime-parity").result().stream().allMatch(EditorAgentService.Check::passed),"Recovered/undone unsaved state still uses native colliders");
            System.out.println("PASS native mission recovery: distinct backup, corrupt-file rejection, explicit restore, shared undo, real game load and foreign-scene isolation");
        }finally{
            System.setProperty("user.home",previous);
            try(var paths=Files.walk(home)){for(var p:paths.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
    }
}
