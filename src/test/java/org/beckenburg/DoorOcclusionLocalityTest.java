package org.beckenburg;

import java.util.*;

/** Diagnostic, not a release gate: one door must not invalidate distant light
 * receivers or repeatedly clear the entire house's stationary visibility. */
public final class DoorOcclusionLocalityTest {
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    private static boolean influenced(FixtureLighting.Source s,LevelDefinition.Portal p){
        double half=p.width()/2+.35;
        double dx=Math.max(0,Math.abs(s.x()-p.x())-(p.alongX()?half:.35));
        double dz=Math.max(0,Math.abs(s.z()-p.z())-(p.alongX()?.35:half));
        double dy=Math.max(0,Math.abs(s.y()-(p.y()+1.15))-1.15);
        return dx*dx+dy*dy+dz*dz<=s.radius()*s.radius();
    }
    public static void main(String[] args){
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var lighting=new FixtureLighting(map);
        var rooms=new RoomLighting();
        lighting.frame(rooms,1);
        var door=map.level().portals().stream()
            .filter(LevelDefinition.Portal::door)
            .filter(p->lighting.sources().stream().anyMatch(s->influenced(s,p)))
            .findFirst().orElseThrow();
        var source=lighting.sources().stream().filter(s->influenced(s,door)).findFirst().orElseThrow();
        long far=lighting.regionOccluderVersion(-200,-200,-200,-195,-195,-195);
        long previous=lighting.regionOccluderVersion(
            source.x()-.1,source.y()-.1,source.z()-.1,
            source.x()+.1,source.y()+.1,source.z()+.1);
        for(int i=0;i<20;i++){
            map.toggleDoor(door.id());
            lighting.frame(rooms,i+2);
            long near=lighting.regionOccluderVersion(
                source.x()-.1,source.y()-.1,source.z()-.1,
                source.x()+.1,source.y()+.1,source.z()+.1);
            check(near>previous,"Door changed without invalidating its contributing light");
            check(lighting.regionOccluderVersion(-200,-200,-200,-195,-195,-195)==far,
                "Door update invalidated a distant room/world region");
            check(lighting.frameStats().invalidatedCells()>0,
                "Door change must propagate to nearby light receivers");
            previous=near;
        }
        System.out.println("PASS 20 alternating House door transitions: affected lights refresh, distant occluders retain revisions (diagnostic)");
    }
}
