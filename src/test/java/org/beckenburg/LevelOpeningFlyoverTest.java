package org.beckenburg;

/** A map opening is a spectator camera, never a player teleport or AI tick. */
public final class LevelOpeningFlyoverTest {
    private static void ok(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args){
        for(var site:new HouseMap.Site[]{
            HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL}){
            HouseMap map=new HouseMap(site);
            var spawn=map.van().spawn(0);
            var player=new LevelOpeningFlyover.Pose(spawn.x(),
                map.van().floorY()+1.62,spawn.z(),-Math.PI/2,-.09);
            LevelOpeningFlyover flight=new LevelOpeningFlyover(map,player);
            var start=flight.pose(0);
            var orbit=flight.pose(.39);
            var approach=flight.pose(.78);
            var end=flight.pose(1);
            ok(end.equals(player),"Opening must hand off at the exact hunter camera for "+site);
            ok(flight.pose(-100).equals(start)&&flight.pose(500).equals(end),
                "Opening progression clamps rather than extrapolating into walls");
            ok(start.y()>approach.y()&&orbit.y()>player.y()+5,
                "Location flyover begins well above architecture and descends");
            ok(Math.hypot(start.x()-player.x(),start.z()-player.z())>5,
                "The opening camera must survey the authored level, not start in the van");
            for(int i=0;i<=100;i++){
                var pose=flight.pose(i/100.);
                ok(Double.isFinite(pose.x()+pose.y()+pose.z()+pose.yaw()+pose.pitch()),
                    "Every cinematic camera orientation must remain finite at "+site);
                ok(pose.y()>=Math.min(player.y(),approach.y())-.01,
                    "Cinematic path cannot dive underneath the hunter's final deck");
            }
            // The opening may be skipped at any point; player spawn remains unchanged.
            ok(map.van().spawn(0).equals(spawn),
                "Cinematic camera sampling must never change the physical van or spawn");
            System.out.println("PASS "+site+": high-altitude flyover, descending approach and exact van view handoff");
        }
    }
}
