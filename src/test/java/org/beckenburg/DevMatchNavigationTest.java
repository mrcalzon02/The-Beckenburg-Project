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
        // Real campaign run 64 got stuck at (19.59, 0.001, 22.55).
        // Report which authored triangle actually blocks the short porch approach.
        // This is diagnostic, not an excuse to remove any wall/door collision.
        var historic=new LevelDefinition.Vec(19.595,.001,22.555);
        Double adjusted=map.supportNear(historic.x(),historic.z(),historic.y());
        check(adjusted!=null,"The rebuilt porch must support the formerly stalled hunter");
        var stalled=new LevelDefinition.Vec(historic.x(),adjusted,historic.z());
        check(map.envelope()!=null&&map.envelope().parts().stream()
                .anyMatch(part->part.name().equals("porch-approach-bevel")&&part.support()),
            "A real authored support surface must bridge the porch, not a collision exemption");
        var entry=map.level().portalsFrom(13).stream()
            .filter(p->p.door()&&p.joins(13,0)).findFirst().orElseThrow();
        var command=new HunterFieldNavigation(map).to(150,
            new LanWorld.Body(stalled.x(),stalled.y(),stalled.z(),180,13),
            0,map.centerX(0),map.centerZ(0),1.15);
        System.out.printf(java.util.Locale.ROOT,
            "DEV PORCH entry=(%.3f,%.3f) from=(%.3f,%.3f) initial=%s%n",
            entry.x(),entry.z(),stalled.x(),stalled.z(),command.action());
        for(double z:new double[]{22.70,22.60,22.555,22.50,22.45,22.40,22.35,22.25,22.10,21.90,21.60}){
            double x=19.60;
            Double supported=map.supportNear(x,z,.001);
            var pos=new LevelDefinition.Vec(x,supported==null?.001:supported,z);
            var env=map.envelope();
            System.out.printf(java.util.Locale.ROOT,
                "DEV PORCH x=%.3f z=%.3f feet=%.3f support=%s traversable=%s envelope=%s terrain=%s%n",
                x,z,pos.y(),supported,map.traversable(pos.x(),pos.y(),pos.z()),
                env==null?java.util.List.of():env.physics().diagnosticBodyBlockers(
                    pos,HunterPosture.STANDING_HEIGHT,.22),
                map.worldSurface()==null?java.util.List.of():map.worldSurface().diagnosticBodyBlockers(
                    pos,HunterPosture.STANDING_HEIGHT,.22));
        }
        var after=map.resolveMove(stalled,19.62,22.245);
        System.out.printf(java.util.Locale.ROOT,
            "DEV PORCH direct_short_step=%s nearestDoor=%s%n",
            after,map.nearestDoor(stalled.x(),stalled.y(),stalled.z()));
        check(after!=null,"A 31cm physical step must cross the former porch collision deadlock");
        check(command.action()==CampaignGuestBot.Action.WALK,
            "Hired hunter must issue an actual walking command from the supported porch edge");
        // Validate the full physical approach with the SAME swept capsule that
        // a human/hired hunter uses. No scene-only ramp or bypassed walls.
        double approachZ=23.14,approachX=19.8;
        double approachY=map.groundHeight(approachX,approachZ,13);
        var position=new LevelDefinition.Vec(approachX,approachY,approachZ);
        for(int step=0;step<14&&position.z()>21.26;step++){
            double nextZ=Math.max(21.26,position.z()-.24);
            var next=map.resolveMove(position,approachX,nextZ);
            check(next!=null,"Actual capsule cannot climb authored porch at z="+nextZ+
                " from "+position);
            position=next;
        }
        check(position.z()<=21.26+.001&&Math.hypot(
                position.x()-entry.x(),position.z()-entry.z())<1.5,
            "Physical approach must reach the genuine closed door's interaction radius");
        check(map.nearestDoor(position.x(),position.y(),position.z())!=null,
            "The existing door must be reached normally");
        check(map.toggleNearestDoor(position.x(),position.y(),position.z()),
            "Open the SAME authored door a player interacts with");
        for(int step=0;step<10&&position.z()>20.14;step++){
            var next=map.resolveMove(position,approachX,Math.max(20.14,position.z()-.24));
            check(next!=null,"Open physical doorway must admit a normal 31cm movement step from "+position);
            position=next;
        }
        check(map.roomAt(position.x(),position.y(),position.z())==0,
            "Physical walk must enter the interior foyer without a room jump: "+position);
        System.out.println("PASS: genuine 4cm walkable porch bevel, open door and continuous capsule entry");
        System.out.println("PASS: six van aisle routes, supported ramps and two genuine house entrances");
    }
}
