package org.beckenburg;

import java.nio.file.Path;
import java.util.List;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Replays fitted leaf behavior against the exact Northwatch shell and each receiving floor. */
final class NorthwatchDoorRuntime {
    private NorthwatchDoorRuntime() {}

    static void review(HouseMap map,HouseMap replica,List<NativeInvestigationDoor.Spec> frames,
                       Path captures)throws Exception {
        require(frames.size()==16,"One of the surveyed Northwatch frames went missing");
        for(var frame:frames) {
            require(NativeSchoolInteriorReview.sweep(map.nativeInvestigation().physics(),frame),
                    "Northwatch sweep misses its receiving floor "+frame.frameId());
            NativeSchoolInteriorReview.audit(map,frame,false);
            String label=switch(frame.portal().id()) {
                case 841 -> "radio-generator";
                case 845 -> "court-radio";
                case 854 -> "lower-tunnel-door";
                default -> null;
            };
            if(label!=null&&captures!=null)NativeSchoolInteriorReview.render(map,frame.portal(),
                captures.resolve(label+"-closed.png"),-frame.swingDirection());
            map.toggleDoor(frame.portal().id(),System.nanoTime()-2_000_000_000L);
            replica.applyDoorSnapshot(map.doorSnapshot());
            NativeSchoolInteriorReview.audit(map,frame,true);
            NativeSchoolInteriorReview.audit(replica,frame,true);
            if(label!=null&&captures!=null)NativeSchoolInteriorReview.render(map,frame.portal(),
                captures.resolve(label+"-open.png"),-frame.swingDirection());
            System.out.println("PASS Northwatch fitted "+frame.frameId());
        }
    }

    static void transits(HouseMap map,List<Portal> portals) {
        var physics=map.nativeInvestigation().physics();
        for(var p:portals) {
            if(p.door()||p.id()==905)continue;
            var a=onFloor(physics,NativeSchoolInteriorReview.point(p,-.34,0,0));
            var b=onFloor(physics,NativeSchoolInteriorReview.point(p,.34,0,0));
            require(p.joins(map.roomAt(a.x(),a.y(),a.z()),map.roomAt(b.x(),b.y(),b.z())),
                    "Open connection does not cross the measured rooms "+p+" at "+a+" and "+b);
            for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
                NativeSchoolInteriorReview.walk(map,List.of(a,b),height,"Northwatch transition "+p.id());
                NativeSchoolInteriorReview.walk(map,List.of(b,a),height,"Northwatch transition return "+p.id());
            }
        }
    }

    static void live(LevelDefinition level,NativeInvestigationScene scene,List<Vec> serviceRoute,List<String> report)throws Exception {
        var map=new HouseMap(HouseMap.Site.USER,level,null,scene);
        var replica=new HouseMap(HouseMap.Site.USER,level,null,scene);
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
            var leader=new Peer(host,false,"Northwatch operator");
            var witness=new Peer(host,false,"Door observer");
            var crew=new Peer(host,false,"Station crew");
            var ghost=new Peer(host,true,"Station ghost")) {
            start(level,leader,List.of(leader,witness,crew,ghost));
            leader.client.view3D();
            leader.awaitExact("ACCEPT|VIEW3D");
            var at=NativeSchoolInteriorReview.body(leader.await("BODY|0|"));
            require(map.roomAt(at.x(),at.y(),at.z())==14,"Live case must begin on the cargo floor");
            if(serviceRoute!=null) {
                ghost.client.viewGhost3D();
                ghost.awaitExact("ACCEPT|GVIEW3D");
                ghost.await("GPOSE|");
                NativeSchoolInteriorReview.ghostTrip(host,witness,ghost);
            }
            for(int i=0;i<15;i++)at=NativeSchoolInteriorReview.step(leader,0,0,-100);
            at=walk(leader,at,new Vec(32,0,63),new Vec(32,0,56),new Vec(32,0,41),new Vec(26,0,41));
            at=open(map,replica,scene,leader,witness,at,845);
            at=walk(leader,at,new Vec(26,0,19));
            at=open(map,replica,scene,leader,witness,at,841);
            at=walk(leader,at,new Vec(13.35,0,19),new Vec(13.35,0,16.05),new Vec(11,0,16));
            require(map.roomAt(at.x(),at.y(),at.z())==5&&"generator".equals(map.architecturalRoomAt(at.x(),at.y(),at.z())),
                "The live van approach missed the real generator receiving floor");
            if(serviceRoute!=null)NorthwatchPowerReview.live(map,host,leader,witness,crew,at,serviceRoute,report);
        }
    }

    static void liveCover(LevelDefinition level,NativeInvestigationScene scene,List<String> report)throws Exception {
        var map=new HouseMap(HouseMap.Site.USER,level,null,scene);
        var contract=new CustomContract(3,100,150,100,100,100,100,100,CustomContract.Sprint.LIMITED,100,true,true);
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,contract,map);
            var lab=new Peer(host,false,"Laboratory refuge");
            var dorm=new Peer(host,false,"Dormitory refuge");
            var shelter=new Peer(host,false,"Shelter refuge");
            var ghost=new Peer(host,true,"Station ghost")) {
            start(level,lab,List.of(lab,dorm,shelter,ghost));
            var hunters=List.of(lab,dorm,shelter);
            int[] owners={0,1,9};
            for(int seat=0;seat<owners.length;seat++){
                var hunter=hunters.get(seat);
                int owner=owners[seat];
                Socket pocket=level.socket("hide",owner);
                require(pocket!=null,"Refuge is missing in owner "+owner);
                for(int room:NativeSchoolInteriorReview.missionRoute(map,owner)){
                    hunter.client.move(room);
                    hunter.awaitExact("ACCEPT|MOVE|"+room);
                }
                hunter.client.view3D();
                hunter.awaitExact("ACCEPT|VIEW3D");
                var at=NativeSchoolInteriorReview.body(hunter.await("BODY|"+seat+"|"));
                var outside=LevelClosets.world(pocket,0,0,1.24);
                var inside=LevelClosets.world(pocket,0,0,.22);
                at=CemeteryRefugeReview.walk(hunter,seat,at,outside);
                require(!SafeAreaBarrier.protectedAt(map,at.x(),at.y(),at.z()),"Cover starts before "+pocket.id());
                at=CemeteryRefugeReview.walk(hunter,seat,at,inside);
                require(SafeAreaBarrier.protectedAt(map,at.x(),at.y(),at.z()),"No cover after crossing "+pocket.id());
                hunter.client.hide();
                hunter.awaitExact("HIDING|1");
                hunter.client.hide();
                hunter.awaitExact("HIDING|0");
                at=CemeteryRefugeReview.walk(hunter,seat,at,outside);
                require(!SafeAreaBarrier.protectedAt(map,at.x(),at.y(),at.z()),"Cover extends beyond "+pocket.id());
                report.add(pocket.id()+"_live_entry_hide_reveal_exit\tPASS");
            }
        }
    }

    private static void start(LevelDefinition level,Peer leader,List<Peer> peers)throws Exception {
        for(var peer:peers) {
            peer.awaitExact("SITE|USER|"+level.revision());
            peer.client.ready();
        }
        leader.awaitExact("LOBBY|3|HUMAN|4|WAITING");
        leader.client.start();
        leader.await("INFO|mission started");
    }

    private static Vec open(HouseMap map,HouseMap replica,NativeInvestigationScene scene,Peer leader,Peer witness,
                            Vec at,int id)throws Exception {
        var frame=scene.doors().stream().map(NativeInvestigationDoor::spec)
            .filter(spec->spec.portal().id()==id).findFirst().orElseThrow();
        var p=frame.portal();
        at=walk(leader,at,NativeSchoolInteriorReview.point(p,.88,0,0));
        var crossed=NativeSchoolInteriorReview.point(p,-.86,0,0);
        require(map.resolveMove(at,crossed.x(),crossed.z(),HunterPosture.STANDING_HEIGHT)==null,
            "Closed Northwatch door did not stop movement "+id);
        double yaw=Math.atan2(p.x()-at.x(),p.z()-at.z());
        var observer=new LooseClutter.Observer(0,at.x(),at.y(),at.z(),yaw,-.10,true);
        var target=InteractionFocus.pick(map,observer,List.of());
        require(target!=null&&target.kind()==InteractionFocus.Kind.DOOR&&target.id().equals(Integer.toString(id)),
                "Live Northwatch aim missed leaf "+id+" / "+target);
        leader.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360));
        leader.client.interact(target,yaw,-.10);
        leader.awaitExact("ACCEPT|INTERACT|Open door");
        var frameState=witness.await("DOORS|");
        while(!List.of(frameState.substring(6).split(",")).contains(Integer.toString(id)))frameState=witness.await("DOORS|");
        replica.applyDoorSnapshot(frameState.substring(6));
        require(replica.doorOpen(id),"Witness did not apply operating leaf "+id);
        return walk(leader,at,crossed);
    }

    private static Vec onFloor(AuthoredSitePhysics physics,Vec point) {
        var bearing=physics.support(point.x(),point.z(),point.y(),.24);
        require(bearing!=null,"Open station connection has no supported floor at "+point);
        return new Vec(point.x(),bearing.y(),point.z());
    }

    private static Vec walk(Peer leader,Vec at,Vec... points)throws Exception {
        for(var point:points)at=CemeteryRefugeReview.walk(leader,0,at,point);
        return at;
    }

    private static void require(boolean pass,String message){if(!pass)throw new AssertionError(message);}
}
