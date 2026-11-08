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
            int editable=0;while(HouseAuthoringAttachments.required(session.sourceLevel(),session.prop(editable)))editable++;
            var p=session.prop(editable);
            service.transact(session.revision(),"move-prop",Map.of("id",session.nodeId(editable),"x",Double.toString(p.x()+.25),
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
            // CLI bootstrap has no live session/source to fall back on. A bad
            // primary must not be parsed before validating its previous file.
            LevelAuthoringSession.saveProps(EditorDocumentStore.previous(session.documentPath()),"beckenburg/house",LevelPlans.legacyHouseSource().props());
            byte[] legacy=Files.readAllBytes(EditorDocumentStore.previous(session.documentPath()));Files.writeString(session.documentPath(),"corrupt primary before CLI open");
            LevelAuthoringSession.recoverSavedDocument(HouseMap.Site.HOUSE);
            check(java.util.Arrays.equals(legacy,Files.readAllBytes(session.documentPath()))&&new HouseMap().level().id().equals("house"),"Bootstrap restores exact original source without reading corrupt primary");
            Files.copy(school.documentPath(),EditorDocumentStore.previous(session.documentPath()),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            byte[] corrupt="foreign backup must not replace this corrupt primary".getBytes(java.nio.charset.StandardCharsets.UTF_8);Files.write(session.documentPath(),corrupt);
            try{LevelAuthoringSession.recoverSavedDocument(HouseMap.Site.HOUSE);throw new AssertionError("Foreign bootstrap backup accepted");}catch(java.io.IOException expected){}
            check(java.util.Arrays.equals(corrupt,Files.readAllBytes(session.documentPath())),"Rejected foreign bootstrap preserves exact primary bytes");Files.write(session.documentPath(),legacy);session.reopen();
            var rebuilt=HouseRebuildFurnishings.source();var ids=java.util.stream.IntStream.range(0,rebuilt.props().size()).mapToObj(i->"rebuilt-"+i).toList();
            session.restore(new LevelAuthoringSession.Snapshot("beckenburg/house",ids,rebuilt.props(),UserLevelLibrary.HOUSE_REBUILD));session.save();
            byte[] validRebuilt=Files.readAllBytes(session.documentPath());int fridge=0;while(!session.prop(fridge).mesh().equals("level_house_fridge_worn"))fridge++;
            var cold=session.prop(fridge);session.put(fridge,new org.beckenburg.LevelDefinition.Prop(cold.mesh(),cold.room(),cold.x()+.01,cold.y(),cold.z(),cold.scale(),cold.yaw(),cold.halfX(),cold.halfZ(),cold.height(),cold.support()));session.save();
            Files.writeString(session.documentPath(),"corrupt rebuilt primary before CLI open");LevelAuthoringSession.recoverSavedDocument(HouseMap.Site.HOUSE);
            var restored=new LevelAuthoringSession(HouseMap.Site.HOUSE);
            check(java.util.Arrays.equals(validRebuilt,Files.readAllBytes(session.documentPath()))&&restored.snapshot().architecture().equals(UserLevelLibrary.HOUSE_REBUILD)
                &&new HouseMap().level().room(5).y()==2.8&&restored.props().equals(rebuilt.props()),"Bootstrap validates backup metadata and restores exact rebuilt authority, props and default gameplay");
            var invalid=org.beckenburg.editor.RuntimeSceneDocument.load(session.documentPath());var marker=invalid.find(HouseSourceVersions.NODE);
            invalid.replace(new org.beckenburg.editor.RuntimeScene.Node(marker.id(),marker.name(),marker.parentId(),marker.layer(),marker.transform(),
                java.util.List.of(new org.beckenburg.editor.RuntimeScene.Component(HouseSourceVersions.COMPONENT,Map.of("architecture","house-future-v99")))));
            org.beckenburg.editor.RuntimeSceneDocument.save(EditorDocumentStore.previous(session.documentPath()),invalid);Files.write(session.documentPath(),corrupt);
            try{LevelAuthoringSession.recoverSavedDocument(HouseMap.Site.HOUSE);throw new AssertionError("Future backup architecture accepted");}catch(java.io.IOException expected){}
            check(java.util.Arrays.equals(corrupt,Files.readAllBytes(session.documentPath())),"Future backup cannot overwrite corrupt primary");
            var detached=org.beckenburg.editor.RuntimeSceneDocument.load(EditorDocumentStore.previous(session.documentPath()));
            detached.replace(HouseSourceVersions.node(UserLevelLibrary.HOUSE_REBUILD));
            var generator=detached.nodes().stream().filter(n->n.name().equals("level_trace_generator")).findFirst().orElseThrow();detached.remove(generator.id());
            org.beckenburg.editor.RuntimeSceneDocument.save(EditorDocumentStore.previous(session.documentPath()),detached);
            try{LevelAuthoringSession.recoverSavedDocument(HouseMap.Site.HOUSE);throw new AssertionError("Detached rebuilt service backup accepted");}catch(java.io.IOException expected){}
            check(java.util.Arrays.equals(corrupt,Files.readAllBytes(session.documentPath())),"Invalid source attachment backup preserves primary bytes");
            System.out.println("PASS native mission recovery: distinct backup, corrupt-file rejection, explicit restore, shared undo, game load, foreign isolation and corrupt-primary legacy/rebuilt CLI bootstrap");
        }finally{
            System.setProperty("user.home",previous);
            try(var paths=Files.walk(home)){for(var p:paths.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
    }
}
