package org.beckenburg;

import java.util.List;

/** Functional dependency invalidation: normal probes + remote retention. */
public final class PreparedFixtureProbeCacheTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args){
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var circuits=new RoomLighting();
        var lights=new FixtureLighting(map);
        lights.frame(circuits,0);
        var lamp=lights.sources().stream()
            .filter(s->s.room()>=0&&s.room()<12&&
                s.kind()==FixtureLighting.Kind.FROSTED_DOME).findFirst().orElseThrow();
        var front=new PreparedFixtureProbeCache.Probe(lamp.room(),lamp.x(),lamp.y(),lamp.z(),0,1,0);
        var back=new PreparedFixtureProbeCache.Probe(lamp.room(),lamp.x(),lamp.y(),lamp.z(),0,-1,0);
        var far=new PreparedFixtureProbeCache.Probe(lamp.room(),10000,10000,10000,0,0,0);
        var cache=new PreparedFixtureProbeCache(lights,List.of(front,back,far));
        var cold=cache.refresh();
        check(cold.recomputed()==3&&cold.retained()==0,"First pass prepares all 3 world probes");
        lights.frame(circuits,0);
        var warm=cache.refresh();
        check(warm.recomputed()==0&&warm.retained()==3,"Unchanged frame must reuse light probes");
        check(warm.colours().get(0)==cold.colours().get(0),"Reuse existing immutable RGB");
        circuits.toggle(lamp.room());lights.frame(circuits,0);
        var changed=cache.refresh();
        check(changed.recomputed()==2&&changed.retained()==1
                &&changed.indices().equals(List.of(0,1)),
            "Local fixture switch updates both normals but not remote light region");
        check(changed.colours().get(2)==cold.colours().get(2),
            "Distant light sample must retain exact cached RGB");
        check(cache.refresh().recomputed()==0,"Camera-only repeat must avoid light work");
        var door=map.level().portals().stream().filter(p->p.door()).findFirst().orElseThrow();
        map.toggleDoor(door.id());lights.frame(circuits,0);
        var occlusion=cache.refresh();
        check(occlusion.recomputed()==3&&occlusion.retained()==0,
            "Door topology change must conservatively invalidate all three probes");
        check(cache.refresh().recomputed()==0,"Stable doorway does not recompute");
        boolean failed=false;
        try{new PreparedFixtureProbeCache.Probe(0,Double.NaN,0,0,0,1,0);}
        catch(IllegalArgumentException expected){failed=true;}
        check(failed,"Nonfinite geometry must be rejected");
        System.out.println("PASS static fixture probes: warm reuse, local normals, remote retention, door invalidation");
    }
}
