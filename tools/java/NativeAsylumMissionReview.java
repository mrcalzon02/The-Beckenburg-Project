package org.beckenburg;

import java.io.IOException;
import java.util.List;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

final class NativeAsylumMissionReview {
    private NativeAsylumMissionReview(){}

    private static Vec point(String[] row,int x){
        return new Vec(Double.parseDouble(row[x]),Double.parseDouble(row[x+1]),Double.parseDouble(row[x+2]));
    }

    static List<Room> bind(NativeInvestigationScene scene,Dock dock,List<String[]> records,List<Portal> portals)
            throws IOException{
        var owners=new Room[HouseMap.ROOM_COUNT];
        var physics=scene.physics();
        for(var row:records)switch(row[0]){
            case "ANCHOR" -> {
                int owner=Integer.parseInt(row[1]);
                var feet=point(row,3);
                var room=scene.spaceAt(feet.x(),feet.y(),feet.z());
                if(owners[owner]!=null||room==null||room.owner()!=owner||!room.id().equals(row[2])||!room.accessible()
                        ||!physics.traversable(feet,HunterPosture.STANDING_HEIGHT))
                    throw new IOException("Invalid Asylum anchor "+row[2]+" "+feet);
                owners[owner]=new Room(owner,room.id(),room.x0(),room.z0(),room.x1(),room.z1(),room.floor(),room.height(),
                    room.outside()?"ground":"stone",room.outside()?"outdoor":"masonry",feet);
            }
            case "TRANSITION" -> {
                var at=point(row,5);
                int a=Integer.parseInt(row[3]),b=Integer.parseInt(row[4]);
                boolean along=row[9].equals("z");
                var support=physics.support(at.x(),at.z(),at.y(),.055);
                if(support==null||Math.abs(support.y()-at.y())>.05)
                    throw new IOException("No existing support for Asylum transition "+row[2]+" "+at);
                if(row[10].equals("trail")){
                    double dx=along?0:.68,dz=along?.68:0;
                    int near=scene.ownerAt(at.x()-dx,at.y(),at.z()-dz);
                    int far=scene.ownerAt(at.x()+dx,at.y(),at.z()+dz);
                    if(!((a==near&&b==far)||(a==far&&b==near)))
                        throw new IOException("Asylum opening did not separate the named owners "+row[2]+" "+near+"/"+far);
                }
                portals.add(new Portal(Integer.parseInt(row[1]),a,b,at.x(),at.y(),at.z(),
                    Double.parseDouble(row[8]),along,row[10],1));
            }
            default -> throw new IOException("Unknown Asylum mission record "+row[0]);
        }
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        double y=dock.y()+VanStaging.FLOOR_Y;
        var lip=van.world(0,VanStaging.FRONT);
        portals.add(new Portal(486,13,14,lip.x(),y,lip.z(),VanStaging.RAMP_HALF_WIDTH*2,
            Math.abs(Math.sin(dock.yaw()))<.5,"trail",1));
        owners[14]=new Room(14,"Expedition van",dock.x()-4,dock.z()-1.3,dock.x()+4,dock.z()+1.3,
            y,VanStaging.INTERIOR_HEIGHT,"van","metal",new Vec(dock.x(),y,dock.z()));
        for(int id=0;id<owners.length;id++)if(owners[id]==null)throw new IOException("Missing Asylum owner "+id);
        return List.of(owners);
    }

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

    static void live(HouseMap map,List<String> report)throws Exception{
        var clientMap=new HouseMap(HouseMap.Site.ASYLUM,map.level(),null,map.nativeInvestigation());
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
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
            ghost.client.viewGhost3D();
            ghost.awaitExact("ACCEPT|GVIEW3D");
            ghost.awaitExact(source);
            ghost.await("GPOSE|");
        }
        report.add("staged_tcp_asylum_case\tFour peers, actual ramp discharge, closed court threshold refusal and live door crossing in both directions");
    }
}
