package org.beckenburg;

import java.util.*;

/** Compare retained lighting with fresh rays after every actual House door. */
public final class StationaryDoorDependencyTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args){
        var map=new HouseMap();var circuits=new RoomLighting();circuits.acceptSnapshot("111111111111");
        var field=new FixtureLighting(map);field.frame(circuits,0);
        var points=new ArrayList<PreparedFixtureProbeCache.Probe>();
        for(var source:field.sources())points.add(new PreparedFixtureProbeCache.Probe(
            source.room(),source.x(),source.y()-.7,source.z(),0,0,0));
        for(var door:map.level().portals())if(door.door())for(double side:new double[]{-.4,.4}){
            double x=door.x()+(door.alongX()?0:side),z=door.z()+(door.alongX()?side:0),y=door.y()+1.2;
            points.add(new PreparedFixtureProbeCache.Probe(map.roomAt(x,y,z),x,y,z,0,0,0));
        }
        var cache=new PreparedFixtureProbeCache(field,points,true);var previous=cache.refresh();
        check(cache.refresh().recomputed()==0,"warm stationary samples remain stable");
        int changedColours=0,retraced=0,totalRays=0;long largestRefresh=0;
        for(var door:map.level().portals())if(door.door()){
            map.toggleDoor(door.id());field.frame(circuits,0);var retained=cache.refresh();
            long rays=field.frameStats().shadowRays()+field.frameStats().normalRays();
            var fresh=new FixtureLighting(map);fresh.frame(circuits,0);
            var reference=new PreparedFixtureProbeCache(fresh,points,true).refresh();
            check(retained.colours().equals(reference.colours()),"retained rays disagree with fresh real occlusion after door "+door.id());
            check(retained.recomputed()<points.size(),"a door must not revise every House probe");
            check(rays<fresh.frameStats().shadowRays(),"a door must not retrace every House ray");
            for(int i=0;i<points.size();i++)if(!retained.colours().get(i).equals(previous.colours().get(i)))changedColours++;
            previous=retained;retraced+=rays;totalRays+=fresh.frameStats().shadowRays();largestRefresh=Math.max(largestRefresh,retained.recomputed());
            field.frame(circuits,0);check(cache.refresh().recomputed()==0,"unchanged door/camera preserves revisions");
        }
        check(changedColours>0&&retraced>0,"real door changes must affect real illumination and visibility");
        System.out.println("PASS all House doors match fresh occlusion; changed colours="+changedColours+
            " localized rays="+retraced+" fresh rays="+totalRays+" maximum refreshed probes="+largestRefresh+"/"+points.size());
    }
}
