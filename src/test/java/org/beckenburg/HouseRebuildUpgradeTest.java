package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
import org.beckenburg.editor.*;

/** Real native House upgrades, undo, original-file recovery, authored deltas and rejected conflicts. */
public final class HouseRebuildUpgradeTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Prop moved(Prop p,double dx){return new Prop(p.mesh(),p.room(),p.x()+dx,p.y(),p.z(),p.scale(),p.yaw(),p.halfX(),p.halfZ(),p.height(),p.support());}
    private static Prop crate(int room,double cx,double bottom,double cz){var b=SceneRenderer.castleBounds("wooden_crate_1");return new Prop("wooden_crate_1",room,cx-b[0],bottom-b[1],cz-b[2],1,0,b[3]/2,b[5]/2,b[4],"floor");}
    private static EditorAgentService<LevelAuthoringSession.Snapshot> service(LevelAuthoringSession session,SceneRenderer renderer){
        return new EditorAgentService<>(new LevelEditorAgent(session,()->new HouseMap(HouseMap.Site.HOUSE,session.level()),renderer,()->{}),new EditorHistory<>(session,30));
    }
    private static void rebuild(EditorAgentService<?> service,long revision){service.transact(revision,"rebuild-house",Map.of("baseline","ORIGINAL"));}
    public static void main(String[] args)throws Exception{
        String previous=System.getProperty("user.home");Path home=Files.createTempDirectory("house-native-upgrade-");System.setProperty("user.home",home.toString());
        try{
            var site=HouseMap.Site.HOUSE;var original=LevelPlans.legacyHouseSource();var target=HouseRebuildFurnishings.source();
            var inner=crate(11,17,-2.4,16);var outer=new Prop(inner.mesh(),inner.room(),inner.x(),inner.y(),inner.z(),2,0,inner.halfX()*2,inner.halfZ()*2,inner.height()*2,inner.support());
            check(CastleRebuildValidation.modelConflict(HouseRebuildUpgrade.triangles(inner),HouseRebuildUpgrade.triangles(outer)),"Retained/native model containment cannot pass a vertex-only collision check");
            LevelAuthoringSession.saveProps(LevelAuthoringSession.path(site),"beckenburg/house",original.props());
            var session=new LevelAuthoringSession(site);var renderer=new SceneRenderer();var agent=service(session,renderer);
            byte[] before=Files.readAllBytes(session.documentPath());var saved=session.snapshot();
            check(session.sourceLevel().rooms().get(5).y()==3,"Versionless historical House source");
            rebuild(agent,session.revision());var upgraded=session.snapshot();
            check(session.sourceLevel().rooms().equals(target.rooms())&&session.level().props().equals(target.props()),"Actual native source and all current furnishing defaults upgrade together");
            check(session.level().colliders().equals(target.colliders()),"Exact source collision preserved across native architecture switch");
            check(new HouseMap(site).level().rooms().equals(original.rooms()),"Unsaved upgrade must not change live disk-dispatched House");
            check(Arrays.equals(before,Files.readAllBytes(session.documentPath())),"Agent must not implicitly save");
            agent.undo(session.revision());check(session.snapshot().equals(saved)&&session.level().rooms().equals(original.rooms()),"Undo restores architecture AND exact original prop IDs");
            agent.redo(session.revision());check(session.snapshot().equals(upgraded),"Redo retains rebuilt source metadata and IDs");agent.save(session.revision());
            Path backup=session.documentPath().resolveSibling("house.bscn.before-first-principles.bscn");
            check(Arrays.equals(before,Files.readAllBytes(backup)),"First rebuild save preserves original versionless file bytes");
            var reopened=new LevelAuthoringSession(site);
            check(reopened.snapshot().equals(upgraded)&&new HouseMap(site).level().rooms().equals(target.rooms()),"Real default House loads explicitly saved rebuilt native source");
            check(new HouseMap(site).level().revision().equals(reopened.level().revision()),"Editor and actual default map share network revision");
            HouseRebuildPowerTest.main(new String[]{"SAVED_HOUSE"});GhostElectricalTest.main(new String[]{"SAVED_HOUSE"});
            if(args.length>0){
                Path out=Path.of(args[0]);Files.createDirectories(out);renderer.setRenderSize(1000,625);renderer.lamp=true;renderer.roomLights=new RoomLighting();
                double[][] views={{16,12.4,2,14.9,14.1925,.85},{18.3,17.6,11,16.8,20.0,-1.5},{18.8,19.2,5,20.5,16.5,3.6}};
                String[] names={"saved-built-in-house-kitchen","saved-built-in-house-cellar","saved-built-in-house-landing"};
                for(int i=0;i<views.length;i++){
                    var v=views[i];var actual=new HouseMap(site);double floor=actual.groundHeight(v[0],v[1],(int)v[2]);
                    check(actual.traversable(v[0],floor,v[1])&&actual.roomAt(v[0],floor,v[1])==(int)v[2],"Actual migrated saved House camera support/room "+names[i]);
                    renderer.x=v[0];renderer.z=v[1];renderer.eyeHeight=floor+1.62;renderer.yaw=Math.atan2(v[3]-v[0],v[4]-v[1]);renderer.pitch=Math.atan2(v[5]-renderer.eyeHeight,Math.hypot(v[3]-v[0],v[4]-v[1]));
                    check(agent.capture(session.revision(),out.resolve(names[i]+"-runtime.png")).result().renderer().equals("SceneRenderer.drawPractice"),"Actual migrated native viewport capture");
                }
            }
            reopened.resetToSource();check(reopened.snapshot().architecture().equals(UserLevelLibrary.HOUSE_REBUILD),"Reset retains selected rebuilt architecture");
            var again=service(reopened,new SceneRenderer());rebuild(again,reopened.revision());check(reopened.level().props().equals(target.props()),"Repeated rebuild preserves current source");
            int generator=-1,fridge=-1;for(int i=0;i<reopened.size();i++){
                if(reopened.prop(i).mesh().equals("level_trace_generator"))generator=i;
                if(reopened.prop(i).mesh().equals("level_house_fridge_worn"))fridge=i;
            }
            check(generator>=0&&fridge>=0,"Rebuilt functional fixture references");var beforeAttachment=reopened.snapshot();
            try{reopened.put(generator,moved(reopened.prop(generator),.1));throw new AssertionError("Generator detached from its starter/feed");}catch(IllegalArgumentException expected){check(expected.getMessage().contains("attachment"),"Attachment failure is explicit");}
            try{reopened.remove(generator);throw new AssertionError("Generator source removed leaving live sockets");}catch(IllegalArgumentException expected){}
            check(reopened.snapshot().equals(beforeAttachment),"Rejected standalone attachment edits do not mutate native state");
            reopened.remove(fridge);var deleted=reopened.snapshot();rebuild(again,reopened.revision());check(reopened.snapshot().equals(deleted),"Repeated rebuilt-source operation never resurrects user deletion");

            // Historical source edits are transformed as deltas around the new canonical fixture.
            session.restore(saved);int firstChair=-1;
            for(int i=0;i<session.size();i++)if(session.prop(i).room()==3&&session.prop(i).mesh().equals("chair_mp_5")){firstChair=i;break;}
            check(firstChair>=0,"Historical dining chair fixture");String chairId=session.nodeId(firstChair);var chair=session.prop(firstChair);session.put(firstChair,moved(chair,.02));
            // A personal cellar object retains its exact X/Z and authored offset above the new floor.
            session.add(crate(11,16,-3,15.7));String personal=session.nodeId(session.size()-1);session.save();var authored=session.snapshot();
            byte[] authoredBytes=Files.readAllBytes(session.documentPath());
            rebuild(agent,session.revision());
            check(session.snapshot().ids().contains(personal),"Personal object identity retained");var retained=session.prop(session.indexOf(personal));
            check(Math.abs(retained.y()+SceneRenderer.castleBounds(retained.mesh())[1]+2.4)<1e-8,"Personal source feet follow actual changed cellar elevation");
            var desiredChair=target.props().stream().filter(p->p.room()==3&&p.mesh().equals("chair_mp_5")).findFirst().orElseThrow();
            check(Math.abs(session.prop(session.indexOf(chairId)).x()-desiredChair.x()-.02)<1e-8,"Authored fixture delta retained around rebuilt placement");
            check(Arrays.equals(authoredBytes,Files.readAllBytes(session.documentPath())),"Retained-edit rebuild remains unsaved");
            agent.undo(session.revision());check(session.snapshot().equals(authored),"Authored deltas and personal identity remain undoable");
            session.restore(saved);session.remove(firstChair);var legacyDeletion=session.snapshot();rebuild(agent,session.revision());
            check(!session.snapshot().ids().contains(chairId)&&session.props().stream().filter(p->p.room()==3&&p.mesh().equals("chair_mp_5")).count()==3,"Historical source deletion must remove its corresponding new fixture without deleting the other chairs");
            agent.undo(session.revision());check(session.snapshot().equals(legacyDeletion),"Historical deletion remains fully undoable across architecture change");

            // A retained crate inside the new finished wall must stop the whole transaction.
            session.restore(saved);session.add(crate(0,21.6,0,19.4));var conflicting=session.snapshot();long revision=session.revision();
            try{rebuild(agent,revision);throw new AssertionError("Wall conflict accepted");}catch(IllegalArgumentException expected){check(expected.getMessage().contains(session.nodeId(session.size()-1)),"Conflict identifies actual retained native ID");}
            check(session.snapshot().equals(conflicting)&&Arrays.equals(authoredBytes,Files.readAllBytes(session.documentPath())),"Rejected geometry preserves complete live scene and saved bytes");

            // Source-version corruption must never be interpreted as the other building.
            session.restore(upgraded);session.save();var portable=RuntimeSceneDocument.load(session.documentPath());var node=portable.find(HouseSourceVersions.NODE);
            portable.replace(new RuntimeScene.Node(node.id(),node.name(),node.parentId(),node.layer(),node.transform(),List.of(new RuntimeScene.Component(HouseSourceVersions.COMPONENT,Map.of("architecture","house-future-v99")))));
            RuntimeSceneDocument.save(session.documentPath(),portable);byte[] invalid=Files.readAllBytes(session.documentPath());
            try{new HouseMap(site);throw new AssertionError("Future native architecture substituted");}catch(IllegalStateException expected){}
            check(Arrays.equals(invalid,Files.readAllBytes(session.documentPath()))&&Arrays.equals(before,Files.readAllBytes(backup)),"Rejected metadata preserves document and first original backup");
            System.out.println("PASS House native three-way upgrade, actual saved default dispatch, source/ID undo-redo, exact original backup, personal cellar support, authored deltas, conflict rollback and future-version rejection");
        }finally{
            System.setProperty("user.home",previous);try(var paths=Files.walk(home)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
    }
}
