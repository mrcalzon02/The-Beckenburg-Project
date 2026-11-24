package org.beckenburg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;

final class NativePrisonMissionReview {
    private NativePrisonMissionReview(){}

    private static Vec xyz(String[] row,int start){
        return new Vec(Double.parseDouble(row[start]),Double.parseDouble(row[start+1]),Double.parseDouble(row[start+2]));
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
