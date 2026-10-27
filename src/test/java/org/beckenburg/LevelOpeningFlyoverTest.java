package org.beckenburg;

/** A map opening is a spectator camera, never a player teleport or AI tick. */
public final class LevelOpeningFlyoverTest {
    private static void ok(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args){
        for(var site:HouseMap.Site.values()){
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
            var framing=LevelOpeningFlyover.framing(map);
            ok(Double.isFinite(framing.cx()+framing.cz()+framing.span()+framing.top()),
                "Indoor and outdoor fallback framing must remain finite");
            var renderer=new SceneRenderer();player.apply(renderer);
            try(var cinematic=new LevelOpeningCinematic(map,renderer,480,300)){
                var before=cinematic.previewStatus();
                ok(before.requests()==0&&before.failures()==0
                    &&before.firstFrameElapsedMs()==-1,
                    "Cold opening must report no phantom completed GPU frame");
                var image=new java.awt.image.BufferedImage(480,300,java.awt.image.BufferedImage.TYPE_INT_RGB);
                var graphics=image.createGraphics();try{cinematic.draw(graphics,480,300);}finally{graphics.dispose();}
                var after=cinematic.previewStatus();
                ok(after.requests()>=before.requests()&&after.failures()>=0,
                    "Opening instrumentation may not fabricate or lose readiness state");
                ok(map.van().spawn(0).equals(spawn),
                    "Preview instrumentation cannot change real player spawn or case");
            }
            System.out.println("PASS "+site+": high-altitude flyover, descending approach and exact van view handoff");
        }
    }
}
