package org.beckenburg;

import java.util.List;

/** Deterministic door-pose regression: the host still owns final traversal state,
 * while each viewport animates the visible leaf without teleporting or restarting. */
public final class DoorSwingTest {
    private static void check(boolean condition,String detail){
        if(!condition)throw new AssertionError(detail);
    }
    private static void near(double actual,double expected,String detail){
        if(Math.abs(actual-expected)>.0001)
            throw new AssertionError(detail+" expected="+expected+" actual="+actual);
    }
    public static void main(String[] args){
        long duration=HouseMap.DOOR_SWING_NANOS;
        for(HouseMap.Site site:List.of(HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,
                                     HouseMap.Site.SCHOOL)){
            HouseMap map=new HouseMap(site);
            var portal=map.level().portals().stream().filter(p->p.door())
                .findFirst().orElseThrow();
            int id=portal.id();
            long start=System.nanoTime();
            near(map.doorOpenFraction(id,start),0,"closed initial pose "+site);
            map.toggleDoor(id,start);
            check(map.doorOpen(id),"authoritative state must switch at interaction "+site);
            near(map.doorOpenFraction(id,start),0,"opening starts from closed "+site);
            near(map.doorOpenFraction(id,start+duration/2),.5,
                "opening has intermediate frame "+site);
            // Repeated unchanged host snapshot must not restart interpolation.
            map.applyDoorSnapshot(map.doorSnapshot());
            near(map.doorOpenFraction(id,start+duration/2),.5,
                "identical LAN DOORS snapshot must not restart "+site);
            near(map.doorOpenFraction(id,start+duration),1,
                "opening must finish fully open "+site);
            check(!map.doorMoving(id),"settled pose must stop being transient "+site);
            long reverse=System.nanoTime();
            map.toggleDoor(id,reverse);
            check(!map.doorOpen(id),"closing must be authoritative immediately "+site);
            near(map.doorOpenFraction(id,reverse),1,"closing starts at open pose "+site);
            near(map.doorOpenFraction(id,reverse+duration/2),.5,
                "closing must have intermediate frame "+site);
            near(map.doorOpenFraction(id,reverse+duration),0,
                "closing must finish fully closed "+site);
            // Reverse while in flight without snapping back to either endpoint.
            long interrupted=System.nanoTime();
            map.toggleDoor(id,interrupted);
            near(map.doorOpenFraction(id,interrupted+duration/2),.5,
                "pre-reversal half-open pose "+site);
            map.toggleDoor(id,interrupted+duration/2);
            near(map.doorOpenFraction(id,interrupted+duration/2),.5,
                "reversal must preserve half-open pose "+site);
            near(map.doorOpenFraction(id,interrupted+duration),.25,
                "reverse swing must ease from the current pose "+site);
            near(map.doorOpenFraction(id,interrupted+duration+duration/2),0,
                "reverse swing settles closed "+site);
            // Remote snapshot transition follows the same continuous visual contract.
            map.applyDoorSnapshot(String.valueOf(id));
            check(map.doorOpen(id),"remote open door must update authority "+site);
            double first=map.doorOpenFraction(id);
            check(first>=0&&first<.5,"remote opening must animate, not snap "+site);
            map.applyDoorSnapshot("");
            check(!map.doorOpen(id),"remote closing must update authority "+site);
            System.out.println("PASS "+site+" eased open/close/reverse and LAN snapshot continuity");
        }
    }
}
