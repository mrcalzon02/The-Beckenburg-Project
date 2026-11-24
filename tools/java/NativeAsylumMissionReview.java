package org.beckenburg;

import java.util.List;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

final class NativeAsylumMissionReview {
    private NativeAsylumMissionReview(){}

    private static void require(boolean condition,String label){
        if(!condition)throw new AssertionError(label);
    }

    static void audit(HouseMap map,List<String> report){
        require(map.valid()&&RitualSites.valid(map),"Asylum mission graph or ritual mounts are invalid");
        for(var room:map.level().rooms()){
            var at=room.spawn();
            require(map.roomAt(at.x(),at.y(),at.z())==room.id(),"Wrong Asylum owner at "+room.name()+" "+at);
        }
        report.add("staged_mission_owners\tAll 15 supported spawns and three retained ritual mounts");
    }

    static void live(HouseMap map,boolean packaged,List<String> report)throws Exception{
        var clientMap=packaged?HouseMap.forNetworkRevision(HouseMap.Site.ASYLUM,map.level().revision())
            :new HouseMap(HouseMap.Site.ASYLUM,map.level(),null,map.nativeInvestigation());
        var contract=CustomContract.standard(3);
        try(var host=packaged?new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,contract,false,false,HouseMap.Site.ASYLUM)
                :new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,contract,map);
            var scout=new Peer(host,false,"Scout");
            var walker=new Peer(host,false,"Walker");
            var witness=new Peer(host,false,"Witness");
            var ghost=new Peer(host,true,"Ghost")){
            String source="SITE|ASYLUM|"+map.level().revision();
            for(var peer:List.of(scout,walker,witness,ghost)){
                peer.awaitExact(source);
                peer.client.ready();
            }
            scout.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            scout.client.start();
            scout.await("INFO|mission started");
            scout.client.view3D();
            scout.awaitExact("ACCEPT|VIEW3D");
            var at=NativeSchoolInteriorReview.body(scout.await("BODY|0|"));
            require(map.roomAt(at.x(),at.y(),at.z())==14,"Scout did not start aboard the actual van");
            for(int i=0;i<9;i++)at=NativeSchoolInteriorReview.step(scout,0,-100,0);
            require(map.roomAt(at.x(),at.y(),at.z())==13&&Math.abs(at.y())<.005,
                "Scout did not reach the actual forecourt "+at);

            for(int room:NativeSchoolInteriorReview.missionRoute(map,2)){
                walker.client.move(room);
                walker.awaitExact("ACCEPT|MOVE|"+room);
            }
            walker.client.view3D();
            walker.awaitExact("ACCEPT|VIEW3D");
            at=NativeSchoolInteriorReview.body(walker.await("BODY|1|"));
            require(at.equals(map.spawn(2)),"Walker did not start in day room "+at);
            Thread.sleep(125);
            at=NativeSchoolInteriorReview.step(walker,1,0,-100);
            Thread.sleep(125);
            walker.client.step(0,-100);
            walker.awaitExact("REJECT|blocked by world collision");
            var eye=new LooseClutter.Observer(1,at.x(),at.y(),at.z(),Math.PI,-.11,true);
            var focus=InteractionFocus.pick(clientMap,eye,List.of());
            require(focus!=null&&focus.kind()==InteractionFocus.Kind.DOOR&&focus.id().equals("474"),
                "No physical focus on day room court leaves from "+at+" "+focus);
            walker.client.turn(180);
            walker.client.interact(focus,Math.PI,-.11);
            walker.awaitExact("ACCEPT|INTERACT|Open door");
            var open=witness.awaitExact("DOORS|474");
            clientMap.applyDoorSnapshot(open.substring("DOORS|".length()));
            require(clientMap.doorOpen(474),"Independent peer missed court door state");
            for(int i=0;i<4;i++)at=NativeSchoolInteriorReview.step(walker,1,0,-100);
            require(clientMap.roomAt(at.x(),at.y(),at.z())==12,"Walker missed the real patient court "+at);
            for(int i=0;i<4;i++)at=NativeSchoolInteriorReview.step(walker,1,0,100);
            require(clientMap.roomAt(at.x(),at.y(),at.z())==2,"Walker missed the day room on return "+at);
            if(map.level().socket("hide",2)!=null){
                at=useDayCupboard(map,walker);
                report.add("tcp_asylum_day_cupboard\tHunter moved into the day-room cupboard, crouched, hid, revealed and exited");
            }
            if(map.level().portals().stream().anyMatch(portal->portal.id()==477&&portal.door())){
                walkDaySpineDoor(clientMap,walker,witness,at);
                report.add("tcp_asylum_day_spine_door\tClosed collision refused the crossing; aimed opening and both physical crossings replicated");
            }
            ghost.client.viewGhost3D();
            ghost.awaitExact("ACCEPT|GVIEW3D");
            ghost.awaitExact(source);
            ghost.await("GPOSE|");
            if(map.level().socket("breaker",11)!=null){
                NativeSchoolInteriorReview.ghostTrip(host,scout,ghost);
                restorePower(map,witness,scout,host);
                report.add("tcp_asylum_live_breaker\tGhost-induced breaker trip, physical plant approach and aimed reset");
            }
        }
        report.add((packaged?"packaged":"staged")+"_tcp_asylum_case\tFour peers, actual ramp discharge, closed court threshold refusal and live door crossing in both directions");
    }

    private static Vec useDayCupboard(HouseMap map,Peer walker)throws Exception{
        for(int step=0;step<19;step++)NativeSchoolInteriorReview.step(walker,1,-100,0);
        Vec at=null;
        for(int step=0;step<27;step++)at=NativeSchoolInteriorReview.step(walker,1,0,100);
        var cupboard=map.level().socket("hide",2);
        require(at!=null&&LevelClosets.inside(cupboard,at.x(),at.y(),at.z()),"The live hunter missed the day-room pocket "+at);
        walker.client.crouch(true);
        walker.awaitExact("HIDING|0");
        walker.client.hide();
        walker.awaitExact("HIDING|1");
        walker.client.hide();
        walker.awaitExact("HIDING|0");
        walker.client.crouch(false);
        walker.awaitExact("HIDING|0");
        for(int step=0;step<6;step++)at=NativeSchoolInteriorReview.step(walker,1,0,-100);
        require(!LevelClosets.inside(cupboard,at.x(),at.y(),at.z()),"The live hunter never left the fitted pocket");
        return at;
    }

    private static void walkDaySpineDoor(HouseMap map,Peer walker,Peer witness,Vec at)throws Exception{
        var door=map.level().portals().stream().filter(portal->portal.id()==477).findFirst().orElseThrow();
        while(at.z()>door.z()+.16)at=NativeSchoolInteriorReview.step(walker,1,0,-100);
        require(Math.abs(at.z()-door.z())<=.16,"Approach did not meet the doorsill "+at);
        while(at.x()<door.x()-.60)at=NativeSchoolInteriorReview.step(walker,1,100,0);
        Thread.sleep(125);
        walker.client.step(100,0);
        walker.awaitExact("REJECT|blocked by world collision");
        double yaw=Math.atan2(door.x()-at.x(),door.z()-at.z());
        var observer=new LooseClutter.Observer(1,at.x(),at.y(),at.z(),yaw,-.11,true);
        var focus=InteractionFocus.pick(map,observer,List.of());
        require(focus!=null&&focus.kind()==InteractionFocus.Kind.DOOR&&focus.id().equals("477"),
            "No aimed interaction at the fitted day-spine door "+at);
        walker.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360));
        walker.client.interact(focus,yaw,-.11);
        walker.awaitExact("ACCEPT|INTERACT|Open door");
        var state=witness.awaitExact("DOORS|474,477");
        map.applyDoorSnapshot(state.substring("DOORS|".length()));
        for(int i=0;i<4;i++)at=NativeSchoolInteriorReview.step(walker,1,100,0);
        require(map.roomAt(at.x(),at.y(),at.z())==0,"Live hunter failed to enter the spine "+at);
        for(int i=0;i<4;i++)at=NativeSchoolInteriorReview.step(walker,1,-100,0);
        require(map.roomAt(at.x(),at.y(),at.z())==2,"Live hunter failed to return to the day room "+at);
    }

    private static void restorePower(HouseMap map,Peer operator,Peer witness,LanHost host)throws Exception{
        for(int room:NativeSchoolInteriorReview.missionRoute(map,11)){
            operator.client.move(room);
            operator.awaitExact("ACCEPT|MOVE|"+room);
        }
        operator.client.view3D();
        operator.awaitExact("ACCEPT|VIEW3D");
        var at=NativeSchoolInteriorReview.body(operator.await("BODY|2|"));
        require(at.equals(map.spawn(11)),"Wrong Asylum plant arrival");
        for(int step=0;step<13;step++)at=NativeSchoolInteriorReview.step(operator,2,0,-100);
        var panel=RoomLighting.breaker(map);
        double dx=panel.x()-at.x(),dz=panel.z()-at.z();
        double yaw=Math.atan2(dx,dz);
        double pitch=Math.atan2(panel.y()-at.y()-HunterPosture.STANDING_EYE,Math.hypot(dx,dz));
        var eye=new LooseClutter.Observer(2,at.x(),at.y(),at.z(),yaw,pitch,true);
        var focus=InteractionFocus.pick(map,eye,List.of());
        require(focus!=null&&focus.kind()==InteractionFocus.Kind.BREAKER&&focus.id().equals(panel.id()),
            "No physical focus at the Asylum plant breaker from "+at+" "+focus);
        operator.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360));
        operator.client.interact(focus,yaw,pitch);
        operator.awaitExact("ACCEPT|INTERACT|Electrical breaker");
        witness.awaitExact("LIGHTS|111111111111|1");
        require(((RoomLighting)NativeSchoolInteriorReview.field(host,"lights")).supplied(),"Asylum plant did not restore power");
    }

}
