package org.beckenburg;

/** Developer AI navigation acceptance: actual authored house ports and van exit. */
public final class DevMatchNavigationTest {
    private static void check(boolean ok,String problem){
        if(!ok)throw new AssertionError(problem);
    }
    public static void main(String[] args) {
        HouseMap map=new HouseMap(HouseMap.Site.HOUSE);
        // Starting at the outdoor apron must aim for the actual foyer, not
        // endlessly alternate between the two outdoor map regions.
        for(int seat=0;seat<6;seat++){
            check(DevMatchClientAi.preferredExteriorEntrance(map,13,seat)==0,
                "front apron must choose authored foyer entrance");
            check(DevMatchClientAi.preferredExteriorEntrance(map,12,seat)==2,
                "rear yard must choose authored kitchen entrance");
        }
        check(DevMatchClientAi.preferredExteriorEntrance(map,14,0)==-1,
            "inside the safe van is not an outside-building entry");
        boolean frontDoor=false;
        for(var p:map.level().portalsFrom(13))
            if(p.joins(13,0)&&p.door())frontDoor=true;
        check(frontDoor,"selected house entry must be a physical door, not a room jump");

        var van=map.van();
        for(int seat=0;seat<6;seat++){
            var start=van.spawn(seat);
            check(map.traversable(start.x(),van.floorY(),start.z()),
                "all six real player spawn pads must be traversable");
            var first=DevMatchClientAi.vanExitWaypoint(van,start.x(),start.z());
            var front=van.local(first.x(),first.z());
            check(Math.abs(front.x())<1e-8
                &&Math.abs(front.z()-(VanStaging.FRONT+.15))<1e-8,
                "bot must first enter the central aisle toward the cargo door");
            // Immediately after passing the transition plane the goal must
            // jump onto the supported cargo ramp, never get stuck at a near
            // intermediate waypoint as H2/H5 did in the first real match.
            var passed=van.world(0,VanStaging.FRONT+.62);
            var second=DevMatchClientAi.vanExitWaypoint(van,passed.x(),passed.z());
            var ramp=van.local(second.x(),second.z());
            check(Math.abs(ramp.x())<1e-8
                &&Math.abs(ramp.z()-(VanStaging.FRONT-.95))<1e-8,
                "after transition bot must aim down physical cargo ramp");
            check(van.supportHeight(second.x(),second.z())!=null,
                "van exit waypoint must have physical ramp support");
        }
        System.out.println("PASS: six van aisle routes, supported ramps and two genuine house entrances");
    }
}
