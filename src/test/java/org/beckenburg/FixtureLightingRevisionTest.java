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
        System.out.println("PASS lighting state revisions: warm stable, breaker off/on, door occluder");
    }
}
