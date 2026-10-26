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
            // The GPU renderer also requests directional probes for real
            // geometry normals. Those expensive physical rays must be retained.
            var authored=light.sources().stream()
                .filter(source->source.room()==0).findFirst().orElseThrow();
            double nx=authored.x(),ny=authored.y()-.4,nz=authored.z();
            var firstNormal=light.sampleNormal(0,nx,ny,nz,0,1,0);
            ok(light.frameStats().normalRays()>0,
                "Authored upward-facing surface must exercise real shadow rays: "+site);
            light.frame(circuits,1);
            var unchanged=light.sample(0,sample.x(),sample.y()+1.1,sample.z());
            var unchangedNormal=light.sampleNormal(0,nx,ny,nz,0,1,0);
            ok(firstNormal.equals(unchangedNormal)
                &&light.frameStats().normalCacheHits()==1
                &&light.frameStats().normalRays()==0,
                "Stable surface normals must reuse physical directional lighting: "+site);
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
            light.sampleNormal(0,nx,ny,nz,0,1,0);
            ok(light.frameStats().normalCacheHits()==0,
                "A switched light must invalidate its directional normal probes: "+site);
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

            System.out.println("PASS lighting retention "+site+
                ": identical-frame reuse, local invalidation and ambient retention");
        }

        // The physical LOS is authoritative even when caches are warm.
        // The House reference doorway has independently measured illumination;
        // other authored sites differ in coordinates, so don't use House XYZ
        // as an invented test location for Farmhouse or School.
        var doorMap=new HouseMap(HouseMap.Site.HOUSE);
        var doorCircuits=new RoomLighting();
        var door=AuthoredTestSupport.door(doorMap,1,4);
        var doorLighting=new FixtureLighting(doorMap);
        doorCircuits.toggle(4);
        doorLighting.frame(doorCircuits,4);
        double closed=doorLighting.sample(1,17.5,1.3,20.5).energy();
        doorLighting.sampleNormal(1,17.5,1.3,20.5,0,1,0);
        doorMap.toggleDoor(door.id());
        doorLighting.frame(doorCircuits,4);
        double opened=doorLighting.sample(1,17.5,1.3,20.5).energy();
        doorLighting.sampleNormal(1,17.5,1.3,20.5,0,1,0);
        ok(doorLighting.frameStats().normalCacheHits()==0,
            "Closing or opening a door must invalidate directional visibility probes");
        ok(opened>closed+.02,
            "Door opening must immediately reveal real fixture light without a stale shadow");

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
