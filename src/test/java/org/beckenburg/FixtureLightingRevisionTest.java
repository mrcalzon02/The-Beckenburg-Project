package org.beckenburg;

/** Functional future lightmap/VBO revision contract, not a cosmetic gate. */
public final class FixtureLightingRevisionTest {
    private static void check(boolean yes,String why){
        if(!yes)throw new AssertionError(why);
    }
    public static void main(String[] args){
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var circuits=new RoomLighting();
        var lights=new FixtureLighting(map);
        lights.frame(circuits,0);
        long first=lights.stateRevision();
        lights.frame(circuits,0);
        check(lights.stateRevision()==first,
            "Identical lights and geometry must not invalidate the cache");
        circuits.setBreaker(false);
        lights.frame(circuits,0);
        long dark=lights.stateRevision();
        check(dark>first,"Breaker blackout must revise static lighting");
        lights.frame(circuits,0);
        check(lights.stateRevision()==dark,"Stable blackout must reuse cached data");
        circuits.setBreaker(true);
        lights.frame(circuits,0);
        long restored=lights.stateRevision();
        check(restored>dark,"Restored power must revise static light state");
        var door=map.level().portals().stream().filter(p->p.door()).findFirst().orElseThrow();
        map.toggleDoor(door.id());
        lights.frame(circuits,0);
        long moved=lights.stateRevision();
        check(moved>restored,"Opened door must revise occlusion-dependent lighting");
        lights.frame(circuits,0);
        check(lights.stateRevision()==moved,"Unchanged doorway cannot revise light state");
        check(lights.regionRevision(10000,10000,10000)==moved,
            "Occluder changes must invalidate even areas outside fixture spheres");
        boolean rejected=false;
        try{lights.regionRevision(Double.NaN,0,0);}
        catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"Nonfinite region coordinates must be rejected");
        var source=lights.sources().stream()
            .filter(p->p.room()>=0&&p.room()<12
                &&p.kind()==FixtureLighting.Kind.FROSTED_DOME)
            .findFirst().orElseThrow();
        long nearBefore=lights.regionRevision(source.x(),source.y(),source.z());
        long farBefore=lights.regionRevision(10000,10000,10000);
        circuits.toggle(source.room());
        lights.frame(circuits,0);
        long nearAfter=lights.regionRevision(source.x(),source.y(),source.z());
        check(nearAfter>nearBefore,
            "Changing a room fixture must invalidate nearby prepared lighting");
        check(lights.regionRevision(10000,10000,10000)==farBefore,
            "Local fixture toggles must not invalidate unrelated remote regions");
        lights.frame(circuits,0);
        check(lights.regionRevision(source.x(),source.y(),source.z())==nearAfter,
            "Identical light state must preserve regional cached lighting");
        System.out.println("PASS lighting revisions: source-local, unchanged far cells, breaker, door occluder and stable frames");
    }
}
