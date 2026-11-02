package org.beckenburg;

import org.beckenburg.editor.*;
import java.nio.file.*;
import java.util.*;

/** Real native sessions and active services share observed file-revision preconditions. */
public final class EditorDiskRevisionTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private interface IO{void run()throws Exception;}
    private static void refused(IO action)throws Exception{try{action.run();}catch(java.io.IOException expected){return;}throw new AssertionError("Stale disk save accepted");}
    private static LevelDefinition.Prop moved(LevelDefinition.Prop p,double dx){return new LevelDefinition.Prop(p.mesh(),p.room(),p.x()+dx,p.y(),p.z(),p.scale(),p.yaw(),p.halfX(),p.halfZ(),p.height(),p.support());}
    public static void main(String[] args)throws Exception{
        Path home=Files.createTempDirectory("native-editor-file-revisions-");String previous=System.getProperty("user.home");System.setProperty("user.home",home.toString());
        try{
            var session=new LevelAuthoringSession(HouseMap.Site.HOUSE);var history=new EditorHistory<>(session,60);
            var service=new EditorAgentService<>(new LevelEditorAgent(session,()->new HouseMap(HouseMap.Site.HOUSE,session.level()),new SceneRenderer(),()->{}),history);
            service.save(session.revision());var base=session.prop(0);var peer=new LevelAuthoringSession(HouseMap.Site.HOUSE);
            peer.put(0,moved(base,.25));peer.save();byte[] newer=Files.readAllBytes(session.documentPath());
            service.edit(()->session.put(0,moved(base,.5)));var unsaved=session.snapshot();
            refused(()->service.save(session.revision()));
            check(session.snapshot().equals(unsaved)&&Arrays.equals(newer,Files.readAllBytes(session.documentPath())),"Mission stale save preserves newer disk AND unsaved native scene");
            check(new HouseMap(HouseMap.Site.HOUSE).level().props().equals(peer.props()),"Actual game reads newer peer's saved scene");
            service.reopen(session.revision());check(session.props().equals(peer.props()),"Explicit reopen observes actual current mission bytes");
            service.undo(session.revision());check(session.snapshot().equals(unsaved),"Undo restores live edits without rewinding observed disk ownership");
            service.save(session.revision());check(new HouseMap(HouseMap.Site.HOUSE).level().props().equals(session.props()),"Save after observe/undo writes intentionally restored live edits");
            refused(peer::save);
            Files.delete(session.documentPath());refused(()->service.save(session.revision()));check(!Files.exists(session.documentPath()),"Stale mission cannot silently recreate externally deleted source");
            service.reopen(session.revision());check(new HouseMap(HouseMap.Site.HOUSE).level().props().equals(session.props()),"Explicit missing-file reopen observes same native source fallback as gameplay");
            service.undo(session.revision());service.save(session.revision());check(new HouseMap(HouseMap.Site.HOUSE).level().props().equals(unsaved.props()),"Observed deletion can be intentionally recreated through shared undo/save");
            var castle=CastleLevelEdits.current();var project=new CastleEditorProject(castle);
            var castleService=new EditorAgentService<>(new CastleEditorAgent(project,new CastleHubMap(),new SceneRenderer(),()->{}),new EditorHistory<>(project,60));
            castleService.save(castle.revision());var castlePeer=new CastleLevelEdits(castle.path());
            castlePeer.setSpawn(13.5,19,1.25);castlePeer.save();byte[] castleNewer=Files.readAllBytes(castle.path());
            castleService.transact(castle.revision(),"set-spawn",Map.of("x","13.5","z","19","yaw","1.5"));
            refused(()->castleService.save(castle.revision()));
            check(castle.spawn()[2]==1.5&&Arrays.equals(castleNewer,Files.readAllBytes(castle.path())),"Active Castle singleton preserves unsaved pose and newer peer bytes");
            castleService.reopen(castle.revision());check(castle.spawn()[2]==1.25,"Castle reopens actual peer version into authoritative singleton");
            castleService.undo(castle.revision());castleService.save(castle.revision());
            check(new CastleLevelEdits(castle.path()).spawn()[2]==1.5,"Castle shared undo/save explicitly publishes observed live pose");refused(castlePeer::save);
            Files.delete(castle.path());refused(()->castleService.save(castle.revision()));check(!Files.exists(castle.path()),"Castle stale save cannot recreate missing source");
            castleService.recoverPrevious(castle.revision());check(castle.spawn()[2]==1.25,"Explicit recovery can restore a deleted Castle document and refresh disk revision");
            castleService.undo(castle.revision());castleService.save(castle.revision());check(new CastleLevelEdits(castle.path()).spawn()[2]==1.5,"Recovery undo preserves explicit-save ownership");
            UserLevelLibrary.create("FirstDiskOwner",HouseMap.Site.HOUSE);var first=new LevelAuthoringSession(HouseMap.Site.USER);var userPeer=new LevelAuthoringSession(HouseMap.Site.USER);
            userPeer.put(0,moved(userPeer.prop(0),.25));userPeer.save();Path owned=first.documentPath();
            UserLevelLibrary.create("SecondDiskOwner",HouseMap.Site.SCHOOL);Path other=UserLevelLibrary.activeDocument();byte[] otherBytes=Files.readAllBytes(other);
            first.put(0,moved(first.prop(0),.5));refused(first::save);
            check(first.documentPath().equals(owned)&&Arrays.equals(otherBytes,Files.readAllBytes(other)),"Pinned USER resource guard cannot redirect stale save to another active project");
            first.reopen();first.put(0,moved(first.prop(0),.1));first.save();
            check(Arrays.equals(otherBytes,Files.readAllBytes(other)),"Observed USER save remains bound to original document");
            System.out.println("PASS native disk revisions: competing mission/Castle saves, active singleton and game readback, preserve unsaved edits, observe/undo/save, deletion/recovery and pinned USER ownership");
        }finally{
            System.setProperty("user.home",previous);try(var paths=Files.walk(home)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
    }
}
