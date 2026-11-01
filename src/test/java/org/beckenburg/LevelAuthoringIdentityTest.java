package org.beckenburg;

import org.beckenburg.editor.EditorAgentService;
import org.beckenburg.editor.EditorHistory;
import org.beckenburg.editor.RuntimeScene;
import org.beckenburg.editor.RuntimeSceneDocument;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/** G1 native compatibility, stable IDs and actual USER document ownership. */
public final class LevelAuthoringIdentityTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void rejects(Runnable action){
        try{action.run();}catch(IllegalArgumentException expected){return;}
        throw new AssertionError("Expected identity rejection");
    }
    public static void main(String[] args)throws Exception{
        var home=Files.createTempDirectory("mission-identity-");
        String previous=System.getProperty("user.home");System.setProperty("user.home",home.toString());
        try{
            var site=HouseMap.Site.HOUSE;var nativeSource=LevelPlans.buildSource(site);
            // Existing BSCN v1 files already contain node IDs. Do not rewrite the format.
            LevelAuthoringSession.saveProps(LevelAuthoringSession.path(site),"beckenburg/house",nativeSource.props());
            var session=new LevelAuthoringSession(site);var original=session.snapshot();
            String first=session.nodeId(0),second=session.nodeId(1);
            check(first.equals("prop-0")&&second.equals("prop-1"),"Legacy BSCN node identity preserved");
            var service=new EditorAgentService<>(new LevelEditorAgent(session,()->new HouseMap(site,session.level()),
                new SceneRenderer(),()->{}),new EditorHistory<>(session,60));
            service.edit(()->session.remove(0));
            check(session.nodeId(0).equals(second),"Deleting first prop does not rename surviving prop");
            var survivor=session.prop(0);
            service.transact(session.revision(),"move-prop",Map.of("id",second,"x",Double.toString(survivor.x()+.25),
                "y",Double.toString(survivor.y()),"z",Double.toString(survivor.z())));
            check(session.prop(0).x()==survivor.x()+.25,"Stable ID command resolves surviving native prop");
            service.undo(session.revision());service.undo(session.revision());
            check(session.snapshot().equals(original),"Shared undo restores props AND identity metadata");
            service.redo(session.revision());service.save(session.revision());
            var reopened=new LevelAuthoringSession(site);
            check(reopened.nodeId(0).equals(second),"Stable ID survives native save and reopen");
            check(new HouseMap(site).level().props().equals(reopened.props()),"Game loads exact identity-preserving authoring geometry");
            reopened.add(reopened.prop(0));String added=reopened.nodeId(reopened.size()-1);
            check(!original.ids().contains(added),"New object gets fresh identity");
            reopened.save();byte[] bytes=Files.readAllBytes(reopened.documentPath());reopened.save();
            check(java.util.Arrays.equals(bytes,Files.readAllBytes(reopened.documentPath())),"Repeated saves deterministic with persisted IDs");
            reopened.reopen();check(reopened.nodeId(reopened.size()-1).equals(added),"Added UUID identity persists");
            var saved=reopened.snapshot();
            rejects(()->reopened.restore(new LevelAuthoringSession.Snapshot("beckenburg/school",saved.ids(),saved.props())));
            check(reopened.snapshot().equals(saved),"Cross-scene snapshot cannot change mission");
            rejects(()->new LevelAuthoringSession.Snapshot(saved.sceneId(),List.of("same","same"),saved.props().subList(0,2)));
            // A valid portable node unsupported by native mission geometry must fail, not be flattened.
            var document=RuntimeSceneDocument.load(reopened.documentPath());var node=document.nodes().get(0);var t=node.transform();
            document.replace(new RuntimeScene.Node(node.id(),node.name(),null,node.layer(),
                new RuntimeScene.Transform(t.x(),t.y(),t.z(),0,t.yaw(),0,t.scaleX(),t.scaleX()*2,t.scaleZ()),node.components()));
            RuntimeSceneDocument.save(reopened.documentPath(),document);
            try{reopened.reopen();throw new AssertionError("Nonuniform scale silently discarded");}catch(java.io.IOException expected){}
            check(reopened.snapshot().equals(saved),"Unsupported persisted transforms preserve live state");
            UserLevelLibrary.create("OwnedFirst",HouseMap.Site.HOUSE);
            var firstUser=new LevelAuthoringSession(HouseMap.Site.USER);var ownedPath=firstUser.documentPath();String ownedId=firstUser.id();
            UserLevelLibrary.create("OwnedSecond",HouseMap.Site.SCHOOL);
            var secondUser=new LevelAuthoringSession(HouseMap.Site.USER);
            byte[] secondBytes=Files.readAllBytes(secondUser.documentPath());
            check(firstUser.documentPath().equals(ownedPath)&&firstUser.id().equals(ownedId),"USER session identity and path pinned at open");
            firstUser.remove(0);firstUser.save();firstUser.reopen();
            check(firstUser.size()==nativeSource.props().size()-1,"Older session reopens its own source while another project is selected");
            check(java.util.Arrays.equals(secondBytes,Files.readAllBytes(secondUser.documentPath())),"Older USER session cannot overwrite active second project");
            UserLevelLibrary.activate("OwnedFirst");
            check(new HouseMap(HouseMap.Site.USER).level().props().equals(firstUser.props()),"Real selected game reads first project's own saved changes");
            System.out.println("PASS G1 native identity: legacy BSCN, deletion, shared undo/redo, stable-ID edit, deterministic save/reopen, unsupported transform rejection and USER ownership");
        }finally{
            System.setProperty("user.home",previous);
            try(var paths=Files.walk(home)){paths.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}
        }
    }
}
