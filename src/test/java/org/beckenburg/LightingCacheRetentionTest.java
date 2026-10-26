package org.beckenburg;

/**
 * Regression for per-light change invalidation. An unchanged viewport must
 * keep its warm colour probes, while switch and doorway changes must never
 * reuse lighting or visibility that was true for a different world state.
 */
public final class LightingCacheRetentionTest {
    private LightingCacheRetentionTest(){}
    private static void ok(boolean result,String message){
        if(!result)throw new AssertionError(message);
    }
    public static void main(String[] args){
        for(var site:new HouseMap.Site[]{
                HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL}){
            var map=new HouseMap(site);
            var circuits=new RoomLighting();
            var light=new FixtureLighting(map);
            var sample=map.spawn(0);
            light.frame(circuits,0);
            var baseline=light.sample(0,sample.x(),sample.y()+1.1,sample.z());
            light.frame(circuits,1);
            var unchanged=light.sample(0,sample.x(),sample.y()+1.1,sample.z());
            ok(baseline.equals(unchanged),"Stationary lighting changed without a light event: "+site);
            ok(light.frameStats().cacheHits()==1&&light.frameStats().shadowRays()==0,
                "Unchanged camera/light state must reuse warm samples without shadow rays: "+site);
            ok(light.frameStats().invalidatedCells()==0,
                "No sphere should be invalidated on an unchanged ordinary frame: "+site);

            // Probe an area outside all authored lamps; a change elsewhere
            // must not flush its existing ambient result or corrupt room data.
            double farX=1000,farZ=1000;
            var far=light.sample(13,farX,1,farZ);
            circuits.toggle(0);
            light.frame(circuits,1);
            ok(light.frameStats().invalidatedCells()>0,
                "Switching a real room circuit must invalidate its light volumes: "+site);
            ok(light.sample(13,farX,1,farZ).equals(far)
                &&light.frameStats().cacheHits()==1,
                "A room switch must not invalidate unrelated cached exterior lighting: "+site);
            var switched=light.sample(0,sample.x(),sample.y()+1.1,sample.z());
            ok(light.frameStats().cacheHits()==1,
                "Changed room-light sample must recalculate instead of returning stale colour: "+site);
            circuits.toggle(0);
            light.frame(circuits,1);
            var restored=light.sample(0,sample.x(),sample.y()+1.1,sample.z());
            ok(restored.equals(baseline),
                "Switch restoration must recover the authored light state without flicker: "+site);
            ok(light.frameStats().consideredSources()>=0,
                "Light broadphase diagnostics must stay valid");
            light.frame(circuits,2);
            light.sample(13,farX+2,1,farZ+2);
            ok(light.frameStats().consideredSources()==0,
                "Remote sample should not loop through every distant building lamp: "+site);

            // The physical LOS is authoritative even when caches are warm.
            // Door changes are not a cosmetic light event: all occlusion
            // decisions must be refreshed immediately.
            var door=AuthoredTestSupport.door(map,1,4);
            var nearDoor=new FixtureLighting(map);
            circuits.toggle(4);
            nearDoor.frame(circuits,4);
            double closed=nearDoor.sample(1,17.5,1.3,20.5).energy();
            map.toggleDoor(door.id());
            nearDoor.frame(circuits,4);
            double opened=nearDoor.sample(1,17.5,1.3,20.5).energy();
            ok(opened>closed+.02,
                "Door opening must immediately reveal light without a retained shadow leak: "+site);
            System.out.println("PASS lighting retention "+site+
                ": identical-frame reuse, local invalidation, ambient retention and door occlusion");
        }

        // Deliberate ghost/light interference must still be visible on its
        // authoritative tick rather than being throttled as cosmetic noise.
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var circuits=new RoomLighting();
        var light=new FixtureLighting(map);
        light.frame(circuits,0);
        circuits.pulseUntil(0,RoomLighting.Interference.SIGNATURE,4,36);
        light.frame(circuits,6);
        ok(light.frameStats().invalidatedCells()>0,
            "Signature ghost interference must not be suppressed by cosmetic light throttling");

        var tv=map.fixtures().states().stream()
            .filter(s->s.kind()==HauntedFixtures.Kind.TELEVISION).findFirst().orElseThrow();
        map.fixtures().activate(tv.id(),tv.room(),12);
        light.frame(circuits,12);
        var tvLight=light.sources().stream()
            .filter(s->s.kind()==FixtureLighting.Kind.TELEVISION).findFirst().orElseThrow();
        light.sample(tv.room(),tvLight.x(),tvLight.y(),tvLight.z());
        light.frame(circuits,13);
        light.sample(tv.room(),tvLight.x(),tvLight.y(),tvLight.z());
        ok(light.frameStats().cacheHits()==1&&light.frameStats().invalidatedCells()==0,
            "Cosmetic TV glow should reuse the stable six-tick lightmap");
        light.frame(circuits,18);
        ok(light.frameStats().invalidatedCells()>0,
            "The next authored TV animation step must update its local light volume");
        System.out.println("PASS intentional ghost pulses, retained television light and six-tick cosmetic animation");
    }
}
