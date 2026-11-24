package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Vec;

final class NativePrisonMissionReview {
    private NativePrisonMissionReview(){}

    static List<String[]> read(Path packet)throws Exception{
        var path=packet.resolve("mission.tsv");
        if(!Files.isRegularFile(path))return List.of();
        return Files.readAllLines(path).stream().map(l->l.split("\\t",-1)).toList();
    }

    private static Vec xyz(String[] row,int start){
        return new Vec(Double.parseDouble(row[start]),Double.parseDouble(row[start+1]),Double.parseDouble(row[start+2]));
    }

    static List<Room> bind(NativeInvestigationScene scene,Dock dock,List<String[]> records,List<Portal> portals){
        var rooms=new Room[HouseMap.ROOM_COUNT];
        var physics=scene.physics();
        for(var row:records)switch(row[0]){
            case "ANCHOR" -> {
                int owner=Integer.parseInt(row[1]);
                var feet=xyz(row,3);
                var room=scene.spaceAt(feet.x(),feet.y(),feet.z());
                if(room==null||room.owner()!=owner||!room.id().equals(row[2])||!room.accessible()
                    ||!physics.traversable(feet,HunterPosture.STANDING_HEIGHT))
                    throw new AssertionError("Invalid Prison anchor "+row[2]+" "+feet);
                rooms[owner]=new Room(owner,room.id(),room.x0(),room.z0(),room.x1(),room.z1(),room.floor(),room.height(),
                    room.outside()?"ground":"stone",room.outside()?"outdoor":"masonry",feet);
            }
            case "TRANSITION" -> {
                var at=xyz(row,5);
                var support=physics.support(at.x(),at.z(),at.y(),.06);
                if(support==null||Math.abs(support.y()-at.y())>.05)
                    throw new AssertionError("No existing support for Prison transition "+row[2]+" "+at);
                portals.add(new Portal(Integer.parseInt(row[1]),Integer.parseInt(row[3]),Integer.parseInt(row[4]),
                    at.x(),at.y(),at.z(),Double.parseDouble(row[8]),row[9].equals("z"),row[10],1));
            }
            default -> {}
        }
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        double y=dock.y()+VanStaging.FLOOR_Y;
        var lip=van.world(0,VanStaging.FRONT);
        portals.add(new Portal(464,13,14,lip.x(),y,lip.z(),VanStaging.RAMP_HALF_WIDTH*2,
            Math.abs(Math.sin(dock.yaw()))<.5,"trail",1));
        rooms[14]=new Room(14,"Expedition van",dock.x()-4,dock.z()-1.3,dock.x()+4,dock.z()+1.3,
            y,VanStaging.INTERIOR_HEIGHT,"van","metal",new Vec(dock.x(),y,dock.z()));
        return List.of(rooms);
    }

    static void audit(HouseMap map,List<String[]> records,List<String> report){
        var walks=new LinkedHashMap<String,List<Vec>>();
        for(var row:records)if(row[0].equals("WALK"))
            walks.computeIfAbsent(row[1],key->new ArrayList<>()).add(xyz(row,2));
        var portal=map.nativeInvestigation().doors().stream().filter(d->d.sliding()).findFirst().orElseThrow().spec().portal();
        map.toggleDoor(portal.id());
        for(var walk:walks.entrySet()){
            var forward=walk.getValue();
            var backward=new ArrayList<>(forward);
            Collections.reverse(backward);
            for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
                NativeSchoolInteriorReview.walk(map,forward,height,walk.getKey()+" forward");
                NativeSchoolInteriorReview.walk(map,backward,height,walk.getKey()+" backward");
            }
        }
        map.toggleDoor(portal.id());
        if(!map.valid()||!RitualSites.valid(map))throw new AssertionError("Invalid staged Prison mission");
        for(var room:map.level().rooms()){
            var at=room.spawn();
            if(map.roomAt(at.x(),at.y(),at.z())!=room.id())
                throw new AssertionError("Wrong supported Prison owner "+room.name()+" at "+at);
        }
        report.add("staged_mission_owners\t15 supported owners and three retained ritual mounts");
        report.add("staged_connected_route_replays\t"+4*walks.size()+" physical link direction and posture combinations");
    }
}
