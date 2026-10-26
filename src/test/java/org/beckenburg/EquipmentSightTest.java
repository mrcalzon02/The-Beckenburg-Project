package org.beckenburg;

/** Camera line-of-sight is governed by actual map doors, not a UI-only sight flag. */
public final class EquipmentSightTest {
    private static void ok(boolean b,String reason){if(!b)throw new AssertionError(reason);}
    public static void main(String[] args){
        HouseMap map=new HouseMap();
        // Use the real shared portal and legal physical approaches, not a
        // stale coordinate from the former grid-built house.
        var door=map.level().portals().stream().filter(p->p.door()&&p.joins(0,1))
            .findFirst().orElseThrow();
        var from=AuthoredTestSupport.side(map,door,0,.75);
        var to=AuthoredTestSupport.side(map,door,1,.75);
        ok(map.traversable(from.x(),from.y(),from.z())
                &&map.traversable(to.x(),to.y(),to.z()),"both door approaches are reachable");
        ok(!map.clearLineOfSight(from.x(),from.z(),to.x(),to.z()),
            "closed door blocks photo");
        ok(map.toggleNearestDoor(from.x(),from.y(),from.z())&&map.doorOpen(door.id()),
            "real door is operable from a physical hunter approach");
        ok(map.clearLineOfSight(from.x(),from.z(),to.x(),to.z()),
            "open door permits photo");
        ok(!map.clearLineOfSight(Double.NaN,from.z(),to.x(),to.z()),
            "nonfinite camera path rejected");
        System.out.println("PASS: playable house geometry occludes camera evidence");
    }
}
