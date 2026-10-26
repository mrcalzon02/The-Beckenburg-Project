package org.beckenburg;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Conservative diagnostic portal candidates, never a final hard occlusion
 * proof. Sightlines through unmodeled voids and windows remain possible.
 * Does not affect actual renderer draws, ghost visibility or simulation.
 */
public final class RoomPortalCandidates {
    public record Snapshot(String revision,int cameraRoom,Set<Integer> candidates,
        int blockedDoors,boolean fallbackAll){
        public Snapshot{candidates=Set.copyOf(candidates);}
        public boolean includes(int room){return candidates.contains(room);}
    }
    private final HouseMap map;
    private String lastRevision="";
    private int lastCameraRoom=Integer.MIN_VALUE;
    private Snapshot lastSnapshot;
    public RoomPortalCandidates(HouseMap map){
        this.map=java.util.Objects.requireNonNull(map);
    }
    public Snapshot evaluate(int cameraRoom){
        var level=map.level();
        String revision=level.revision()+":"+map.hidingDensity()+":"+
            map.doorSnapshot()+":"+map.windowSnapshot();
        if(lastSnapshot!=null&&cameraRoom==lastCameraRoom
                &&revision.equals(lastRevision))return lastSnapshot;
        int blocked=0;
        for(var portal:level.portals())
            if(portal.door()&&!map.doorOpen(portal.id()))blocked++;
        var all=new HashSet<Integer>();
        for(var room:level.rooms())all.add(room.id());
        boolean unknown=!all.contains(cameraRoom);
        boolean exterior=!unknown&&level.room(cameraRoom).outside();
        var candidates=new HashSet<Integer>();
        if(unknown||exterior){
            candidates.addAll(all);
        }else{
            var queue=new ArrayDeque<Integer>();
            queue.add(cameraRoom);candidates.add(cameraRoom);
            while(!queue.isEmpty()){
                int room=queue.removeFirst();
                for(var portal:level.portals()){
                    if(portal.a()!=room&&portal.b()!=room)continue;
                    if(portal.door()&&!map.doorOpen(portal.id()))continue;
                    int next=portal.a()==room?portal.b():portal.a();
                    if(all.contains(next)&&candidates.add(next))queue.addLast(next);
                }
                for(var stair:level.stairs()){
                    if(stair.a()!=room&&stair.b()!=room)continue;
                    int next=stair.a()==room?stair.b():stair.a();
                    if(all.contains(next)&&candidates.add(next))queue.addLast(next);
                }
            }
            // Exterior sightlines are insufficiently modeled: fail open.
            for(var room:level.rooms())if(room.outside())
                candidates.add(room.id());
        }
        lastRevision=revision;lastCameraRoom=cameraRoom;
        return lastSnapshot=new Snapshot(revision,cameraRoom,candidates,
            blocked,unknown||exterior);
    }
}
