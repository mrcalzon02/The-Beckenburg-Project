package org.beckenburg;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Tests each grounded refuge and the annex office's physical staff approach. */
final class CemeteryRefugeReview {
    private static final Vec BAY_THRESHOLD = new Vec(72,0,65);
    private static final Vec TRANSFER = new Vec(74,0,65);
    private static final Vec OFFICE_THRESHOLD = new Vec(76,0,62);
    private static final Vec OFFICE = new Vec(80,0,62);

    private CemeteryRefugeReview() {}

    static void audit(HouseMap map,LevelDefinition level,NativeInvestigationScene scene,Path captures,
                      List<String> report,boolean annex,boolean gpu)throws Exception {
        var approaches=annex?Map.of(10,List.of(new Vec(69,0,65),BAY_THRESHOLD,TRANSFER,new Vec(74,0,62),
                                             OFFICE_THRESHOLD,OFFICE)):Map.<Integer,List<Vec>>of();
        NativeSchoolInteriorReview.auditWalkInCover(map,captures,report,"cemetery",approaches);
        if(!gpu) live(level,scene,annex,report);
    }

    private static void live(LevelDefinition level,NativeInvestigationScene scene,boolean annex,List<String> report)
            throws Exception {
        var map=new HouseMap(HouseMap.Site.USER,level,null,scene);
        var contract=new CustomContract(3,100,150,100,100,100,100,100,CustomContract.Sprint.LIMITED,100,true,true);
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,contract,map);
            var first=new Peer(host,false,"Caretaker observer");
            var second=new Peer(host,false,"Tool-room observer");
            var third=new Peer(host,false,"Third refuge observer");
            var ghost=new Peer(host,true,"Cemetery ghost")){
            var hunters=List.of(first,second,third);
            for(var peer:List.of(first,second,third,ghost)){
                peer.awaitExact("SITE|USER|"+level.revision());
                peer.client.ready();
            }
            first.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            first.client.start();
            first.await("INFO|mission started");
            int[] owners={1,2,annex?10:0};
            for(int seat=0;seat<hunters.size();seat++){
                var hunter=hunters.get(seat);
                int owner=owners[seat];
                var refuge=level.socket("hide",owner);
                for(int room:NativeSchoolInteriorReview.missionRoute(map,owner)){
                    hunter.client.move(room);
                    hunter.awaitExact("ACCEPT|MOVE|"+room);
                }
                hunter.client.view3D();
                hunter.awaitExact("ACCEPT|VIEW3D");
                var feet=NativeSchoolInteriorReview.body(hunter.await("BODY|"+seat+"|"));
                require(feet.equals(map.spawn(owner)),"Wrong live refuge arrival "+refuge.id());
                if(annex&&owner==10){
                    feet=walk(hunter,seat,feet,new Vec(69,0,65));
                    feet=openStaffDoor(hunter,seat,map,feet,BAY_THRESHOLD);
                    feet=walk(hunter,seat,feet,TRANSFER);
                    feet=walk(hunter,seat,feet,new Vec(74,0,62));
                    feet=openStaffDoor(hunter,seat,map,feet,OFFICE_THRESHOLD);
                    feet=walk(hunter,seat,feet,OFFICE);
                    require("mort-office".equals(map.architecturalRoomAt(feet.x(),feet.y(),feet.z())),
                        "Staff route missed the actual mortuary office");
                    report.add("annex_staff_doors_aimed_and_crossed_to_office\tPASS");
                }
                var outside=LevelClosets.world(refuge,0,0,1.24);
                var inside=LevelClosets.world(refuge,0,0,.22);
                feet=walk(hunter,seat,feet,outside);
                require(!protectedAt(map,feet),"Protection started before the threshold "+refuge.id());
                feet=walk(hunter,seat,feet,inside);
                require(protectedAt(map,feet),"Protection missing inside "+refuge.id());
                hunter.client.hide();
                hunter.awaitExact("HIDING|1");
                hunter.client.hide();
                hunter.awaitExact("HIDING|0");
                feet=walk(hunter,seat,feet,outside);
                require(!protectedAt(map,feet),"Protection remained after exit "+refuge.id());
                report.add(refuge.id()+"_live_entry_hide_reveal_exit\tPASS");
            }
        }
    }

    private static Vec openStaffDoor(Peer hunter,int seat,HouseMap map,Vec at,Vec threshold)throws Exception{
        at=walk(hunter,seat,at,new Vec(threshold.x()-.82,threshold.y(),threshold.z()));
        double dx=threshold.x()-at.x(),dz=threshold.z()-at.z();
        double yaw=Math.atan2(dx,dz);
        var target=InteractionFocus.pick(map,
            new LooseClutter.Observer(seat,at.x(),at.y(),at.z(),yaw,-.10,true),List.of());
        var expected=map.level().portals().stream().filter(p->p.door()&&p.x()==threshold.x()&&p.z()==threshold.z())
            .findFirst().orElseThrow();
        require(target!=null&&target.kind()==InteractionFocus.Kind.DOOR&&target.id().equals(Integer.toString(expected.id())),
            "Live refuge approach missed the actual staff door at "+threshold+" / "+target);
        hunter.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360));
        hunter.client.interact(target,yaw,-.10);
        hunter.awaitExact("ACCEPT|INTERACT|Open door");
        return at;
    }

    static Vec walk(Peer hunter,int seat,Vec at,Vec target)throws Exception{
        for(int i=0;i<72;i++){
            double dx=target.x()-at.x(),dz=target.z()-at.z(),distance=Math.hypot(dx,dz);
            if(distance<.19)return at;
            double gain=Math.min(100/distance,310);
            at=NativeSchoolInteriorReview.step(hunter,seat,(int)Math.round(dx*gain),(int)Math.round(dz*gain));
        }
        throw new AssertionError("Live refuge route stopped at "+at+" toward "+target);
    }

    private static boolean protectedAt(HouseMap map,Vec feet){
        return SafeAreaBarrier.protectedAt(map,feet.x(),feet.y(),feet.z());
    }

    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
