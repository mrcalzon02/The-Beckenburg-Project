package org.beckenburg;

import org.beckenburg.editor.EditorAgentService;
import org.beckenburg.editor.EditorHistory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Real mission-side agent construction, not a second synthetic scene. */
public final class EditorAgentMissionPropsTest {
    private EditorAgentMissionPropsTest(){}
    private static void check(boolean good,String message){if(!good)throw new AssertionError(message);}
    private static void rejected(Runnable run){
        try{run.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}
        throw new AssertionError("An unsupported native agent edit was accepted");
    }
    private record Spot(int room,double x,double y,double z){}
    private static boolean free(LevelAuthoringSession session,HouseMap map,
                                LevelDefinition.Room room,double x,double z){
        if(!map.traversable(x,room.y(),z))return false;
        for(var prop:session.props())if(prop.room()==room.id()&&prop.halfX()>0&&prop.halfZ()>0){
            if(Math.abs(prop.x()-x)<prop.halfX()+1.4&&Math.abs(prop.z()-z)<prop.halfZ()+1.4
                &&Math.abs(prop.y()-room.y())<2.5)return false;
        }
        return true;
    }
    private static List<Spot> safeSpots(LevelAuthoringSession session,HouseMap map){
        for(var room:session.level().rooms()){
            if(room.outside()||room.x1()-room.x0()<5||room.z1()-room.z0()<5)continue;
            Spot first=null;
            for(double x=room.x0()+1.5;x<room.x1()-1.5;x+=1.25)
                for(double z=room.z0()+1.5;z<room.z1()-1.5;z+=1.25){
                    if(!free(session,map,room,x,z))continue;
                    var at=new Spot(room.id(),x,room.y(),z);
                    if(first==null)first=at;
                    else if(Math.hypot(x-first.x(),z-first.z())>3.5)return List.of(first,at);
                }
        }
        throw new AssertionError("No pair of actual native floor positions for a construction test");
    }
    private static Map<String,String> place(Spot spot,String mesh){
        return Map.of("mesh",mesh,"room",Integer.toString(spot.room()),
            "x",Double.toString(spot.x()),"y",Double.toString(spot.y()),"z",Double.toString(spot.z()),
            "scale","0.8","yawDegrees","0");
    }
    private static Map<String,String> position(String id,Spot spot){
        return Map.of("id",id,"x",Double.toString(spot.x()),"y",Double.toString(spot.y()),
            "z",Double.toString(spot.z()));
    }
    public static void main(String[] args)throws Exception{
        Path home=Files.createTempDirectory("mission-agent-props-");
        String previous=System.getProperty("user.home");
        System.setProperty("user.home",home.toString());
        try{
            var session=new LevelAuthoringSession(HouseMap.Site.SCHOOL);
            var map= new HouseMap(HouseMap.Site.SCHOOL,session.level());
            var spots=safeSpots(session,map);
            var first=spots.get(0);var second=spots.get(1);
            var renderer=new SceneRenderer();renderer.setRenderSize(320,200);
            var history=new EditorHistory<>(session,60);
            var host=new LevelEditorAgent(session,()->new HouseMap(HouseMap.Site.SCHOOL,session.level()),
                renderer,()->{});
            var agent=new EditorAgentService<>(host,history);
            var operations=agent.summary().operations();
            check(operations.containsAll(List.of("move-prop","place-prop","transform-prop",
                "duplicate-prop","remove-prop")),"Mission agent advertises the human prop operations");
            check(!operations.contains("rebuild-house"),"School must not expose House rebuild");
            var bounds=SceneRenderer.castleBounds("chair_mp_5");
            var room=session.level().room(first.room());
            double px=first.x()-2,pz=first.z()-2;
            if(!map.traversable(px,first.y(),pz)){
                px=first.x()+2;pz=first.z()+2;
            }
            if(!map.traversable(px,first.y(),pz)){px=room.spawn().x();pz=room.spawn().z();}
            renderer.x=px;renderer.z=pz;renderer.eyeHeight=first.y()+1.62;
            renderer.yaw=Math.atan2(first.x()-px,first.z()-pz);renderer.pitch=-.20;
            Path proof=Path.of("build","editor-agent-evidence","mission-agent-props");
            Files.createDirectories(proof);
            int originalSize=session.size();long originalRev=session.revision();
            agent.capture(originalRev,proof.resolve("before.png"));
            agent.transact(originalRev,"place-prop",place(first,"chair_mp_5"));
            check(session.size()==originalSize+1,"Placed real mission prop");
            String added=session.nodeId(session.size()-1);
            check(added!=null&&!added.isBlank()&&!session.snapshot().ids().subList(0,originalSize).contains(added),
                "New prop has a fresh stable ID");
            var created=session.prop(session.size()-1);
            check(created.support().equals("floor")
                &&Math.abs(created.halfX()-bounds[3]*.4)<1e-8
                &&Math.abs(created.halfZ()-bounds[5]*.4)<1e-8,
                "Native source mesh dimensions define the physical collider");
            check(agent.test(session.revision(),"mission-runtime-parity").result().stream().allMatch(EditorAgentService.Check::passed),
                "Real runtime collision consumes added prop");
            agent.capture(session.revision(),proof.resolve("after.png"));
            check(!Arrays.equals(Files.readAllBytes(proof.resolve("before.png")),
                Files.readAllBytes(proof.resolve("after.png"))),
                "Actual fixed-camera renderer must show the new placed object");

            var transformed=Map.of("id",added,"x",Double.toString(first.x()+.125),
                "y",Double.toString(first.y()),"z",Double.toString(first.z()),
                "scale","0.9","yawDegrees","35");
            agent.transact(session.revision(),"transform-prop",transformed);
            var moved=session.prop(session.indexOf(added));
            check(Math.abs(moved.x()-(first.x()+.125))<1e-8&&moved.scale()==.9
                &&Math.abs(moved.yaw()-Math.toRadians(35))<1e-10
                &&Math.abs(moved.halfX()-created.halfX()*(.9/.8))<1e-8,
                "Transform updates real native pose and collider bounds");

            agent.transact(session.revision(),"duplicate-prop",position(added,second));
            check(session.size()==originalSize+2,"Native duplication makes one new object");
            String clone=session.nodeId(session.size()-1);
            check(!clone.equals(added)&&!clone.isBlank(),"Duplicate gets an independent stable ID");
            var afterDuplicates=session.snapshot();
            rejected(()->agent.transact(session.revision(),"duplicate-prop",position(added,first)));
            check(session.snapshot().equals(afterDuplicates),"Overlapping duplicate is refused without scene changes");

            long beforeBadBatch=session.revision();
            rejected(()->agent.transact(beforeBadBatch,List.of(
                new EditorAgentService.Command("remove-prop",Map.of("id",added)),
                new EditorAgentService.Command("place-prop",place(second,"not_an_approved_mesh")))));
            check(session.snapshot().equals(afterDuplicates),"Failed mixed transaction restores IDs, objects and geometry");
            check(agent.test(session.revision(),"mission-runtime-parity").result().stream().allMatch(EditorAgentService.Check::passed),
                "Post-rollback collider parity preserved");
            agent.save(session.revision());
            var saved=session.snapshot();
            check(new HouseMap(HouseMap.Site.SCHOOL).level().props().equals(saved.props()),
                "Actual newly loaded game consumes saved native prop geometry");
            agent.reopen(session.revision());
            check(saved.equals(session.snapshot())&&session.indexOf(added)>=0&&session.indexOf(clone)>=0,
                "Save/reopen retains edited props and stable identities");

            agent.transact(session.revision(),"remove-prop",Map.of("id",clone));
            check(session.size()==originalSize+1,"Remove deletes exact native ID");
            agent.undo(session.revision());
            check(session.snapshot().ids().contains(clone),"Undo restores duplicated native ID");
            agent.redo(session.revision());
            check(!session.snapshot().ids().contains(clone),"Redo removes duplicate again");
            rejected(()->agent.transact(session.revision(),"place-prop",Map.of("mesh","chair_mp_5")));
            rejected(()->agent.transact(session.revision(),"remove-prop",Map.of("id","missing-id")));

            var house=new LevelAuthoringSession(HouseMap.Site.HOUSE);
            house.rebuildHouse();var houseSource=house.sourceLevel();int protectedIndex=-1;
            for(int i=0;i<house.size();i++)if(HouseAuthoringAttachments.required(houseSource,house.prop(i))){
                protectedIndex=i;break;
            }
            check(protectedIndex>=0,"Rebuilt House has at least one authored service/attachment");
            var houseAgent=new EditorAgentService<>(new LevelEditorAgent(house,
                ()->new HouseMap(HouseMap.Site.HOUSE,house.level()),renderer,()->{}),new EditorHistory<>(house,60));
            String protectedId=house.nodeId(protectedIndex);
            var protectedState=house.snapshot();
            rejected(()->houseAgent.transact(house.revision(),"remove-prop",Map.of("id",protectedId)));
            check(protectedState.equals(house.snapshot()),"Agent cannot destroy House's authored service linkage");
            System.out.println("PASS mission agent prop parity: real placement, mesh bounds, transform, duplicate, overlaps, protected services, rollback, native render, game load, save/reopen and shared undo");
        }finally{
            System.setProperty("user.home",previous);
            try(var files=Files.walk(home)){files.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}
        }
    }
}
