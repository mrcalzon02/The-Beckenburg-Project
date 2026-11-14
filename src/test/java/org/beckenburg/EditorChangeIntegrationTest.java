package org.beckenburg;

import org.beckenburg.editor.*;
import java.nio.file.*;
import java.util.*;

/** Deltas describe actual committed native edits; they never apply scene changes. */
public final class EditorChangeIntegrationTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Map<String,String> move(String id,LevelDefinition.Prop p,double x){
        return Map.of("id",id,"x",Double.toString(x),"y",Double.toString(p.y()),"z",Double.toString(p.z()));
    }
    private static void covers(EditorChange.Bounds bounds,LevelDefinition.Prop p){
        var c=LevelPlans.propCollider(p);
        for(int sx:new int[]{-1,1})for(int sz:new int[]{-1,1}){
            double x=c.x()+Math.cos(c.yaw())*sx*c.halfX()-Math.sin(c.yaw())*sz*c.halfZ();
            double z=c.z()+Math.sin(c.yaw())*sx*c.halfX()+Math.cos(c.yaw())*sz*c.halfZ();
            check(x>=bounds.x0()-1e-9&&x<=bounds.x1()+1e-9&&z>=bounds.z0()-1e-9&&z<=bounds.z1()+1e-9
                &&bounds.y0()<=c.y()&&bounds.y1()>=c.y()+c.height(),"Dirty region covers every native rotated collider corner");
        }
    }
    public static void main(String[] args)throws Exception{
        Path home=Files.createTempDirectory("native-editor-changes-");String oldHome=System.getProperty("user.home");
        System.setProperty("user.home",home.toString());
        try{
            var session=new LevelAuthoringSession(HouseMap.Site.HOUSE);var events=new ArrayList<EditorChange>();
            var adapter=new LevelEditorAgent(session,()->new HouseMap(HouseMap.Site.HOUSE,session.level()),new SceneRenderer(),()->{});
            var service=new EditorAgentService<>(adapter,new EditorHistory<>(session,60),event->{
                check(event.revision()==session.revision(),"Publish only after native commit");events.add(event);
            },()->{});
            int index=0;while(session.prop(index).halfX()<=0||session.prop(index).halfZ()<=0)index++;
            final int chosen=index;String id=session.nodeId(index);var p=session.prop(index);long revision=session.revision();
            var command=new EditorAgentService.Command("move-prop",move(id,p,p.x()+.25));
            service.transact(revision,List.of(command,new EditorAgentService.Command("move-prop",move(id,p,p.x()+.5))));
            check(events.size()==1,"One event for whole committed batch");var event=events.get(0);
            check(event.sceneId().equals(session.id())&&event.document().equals(session.documentPath())
                &&event.beforeRevision()==revision&&event.revision()==session.revision(),"Actual owner/document and revision provenance");
            check(event.details().nodes().size()==1&&event.details().nodes().get(0).id().equals(id),"One stable-ID delta for repeated edits");
            var node=event.details().nodes().get(0);check(node.kind()==EditorChange.Kind.UPDATED&&!node.beforeFingerprint().equals(node.fingerprint()),"Native content fingerprint changes");
            check(event.details().assets().contains("assets://models/"+p.mesh())&&event.details().dependencies().contains(EditorChange.Dependency.PHYSICS),"Actual asset and physics dependencies");
            covers(event.details().physicsBounds(),p);covers(event.details().physicsBounds(),session.prop(index));
            var committed=session.snapshot();
            try{service.transact(session.revision(),List.of(command,new EditorAgentService.Command("move-prop",move("absent",p,1))));
                throw new AssertionError("Invalid batch committed");}catch(IllegalArgumentException expected){}
            check(events.size()==1&&session.snapshot().equals(committed),"Rollback emits no partial event");
            try{service.transact(session.revision(),List.of(command),()->true);throw new AssertionError("Cancelled batch committed");}
            catch(java.util.concurrent.CancellationException expected){}
            check(events.size()==1&&session.snapshot().equals(committed),"Cancellation emits no event");
            service.undo(session.revision());check(events.size()==2&&session.prop(index).equals(p),"Shared undo publishes actual reversed delta");
            check(events.get(1).details().nodes().get(0).fingerprint().equals(node.beforeFingerprint()),"Undo returns original native fingerprint");
            service.redo(session.revision());check(events.size()==3&&session.snapshot().equals(committed),"Redo publishes native result");
            service.edit(()->session.remove(chosen));
            check(events.get(3).details().nodes().get(0).kind()==EditorChange.Kind.REMOVED,"Human removal reports stable removed ID");
            service.undo(session.revision());check(events.get(4).details().nodes().get(0).kind()==EditorChange.Kind.ADDED,"Undo removal reports native addition");
            service.save(session.revision());check(events.size()==5,"Save does not invent a scene edit");
            service.edit(()->session.put(chosen,p));service.reopen(session.revision());
            check(events.get(events.size()-1).details().nodes().size()==1,"Reopen reports restored actual prop");
            // Same-position changes must still invalidate: hierarchy coordinates alone cannot detect these.
            var before=session.snapshot();var old=session.prop(index);
            var rotated=new LevelDefinition.Prop(old.mesh(),old.room(),old.x(),old.y(),old.z(),old.scale(),.713,old.halfX(),old.halfZ(),old.height(),old.support());
            service.edit(()->session.put(chosen,rotated));event=events.get(events.size()-1);
            check(event.details().nodes().size()==1,"Rotation-only native edit produces delta");covers(event.details().physicsBounds(),rotated);
            try{event.details().nodes().clear();throw new AssertionError("Mutable event");}catch(UnsupportedOperationException expected){}
            var independent=new LevelAuthoringSession(HouseMap.Site.SCHOOL);
            try{adapter.changes(before,independent.snapshot());throw new AssertionError("Foreign scene diff accepted");}catch(IllegalArgumentException expected){}
            var project=new CastleEditorProject(CastleLevelEdits.current());var castleEvents=new ArrayList<EditorChange>();
            var castle=new EditorAgentService<>(new CastleEditorAgent(project,new CastleHubMap(),new SceneRenderer(),()->{}),new EditorHistory<>(project,60),castleEvents::add,()->{});
            castle.transact(CastleLevelEdits.current().revision(),"set-spawn",Map.of("x","13.5","z","19","yaw","1.25"));
            var spawn=castleEvents.get(0).details();
            check(!spawn.fullScene() && spawn.nodes().size()==1 && spawn.nodes().get(0).id().equals("start"),
                "Castle spawn reports one stable hierarchy node");
            castle.transact(project.scene().revision(),"put-surface",Map.of("id","delta_floor","kind","FLOOR",
                "x0","10","z0","10","x1","11","z1","11","base","0","height",".25","material","1"));
            var added=castleEvents.get(1).details();
            check(added.fullScene() && added.nodes().size()==1 && added.nodes().get(0).kind()==EditorChange.Kind.ADDED &&
                added.nodes().get(0).id().equals(project.node("delta_floor").id()),"Surface delta resolves to the native hierarchy");
            castle.undo(project.scene().revision());
            var removed=castleEvents.get(2).details().nodes().get(0);
            check(removed.kind()==EditorChange.Kind.REMOVED && removed.beforeFingerprint().equals(added.nodes().get(0).fingerprint()),
                "Surface undo reports removal of the exact committed content");
            castle.edit(()->project.scene().setNavBlocked(10,10,!project.scene().navBlocked(10,10)));
            var nav=castleEvents.get(3).details();
            check(nav.fullScene() && nav.nodes().isEmpty(),"Navigation-only change requires full invalidation");
            System.out.println("PASS native editor changes: mission deltas, Castle spawn/surface fingerprints and conservative world invalidation");
        }finally{
            System.setProperty("user.home",oldHome);
            try(var paths=Files.walk(home)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
    }
}
