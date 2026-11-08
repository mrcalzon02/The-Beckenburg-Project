package org.beckenburg;
import java.util.List;

/** Recording GPU backend checks actual scene chunk light revision queries.
 * Never claims physical hardware frame timings or visual lighting parity. */
public final class ShellRegionRevisionCacheTest {
    private static void require(boolean ok,String why){
        if(!ok)throw new AssertionError(why);
    }
    private static final class Recorder implements GpuWorldRenderer {
        private Frame latestFrame;
        public boolean ready(){return true;}
        public String rendererDescription(){return "Shell-region regression recorder";}
        public void submit(Frame frame){latestFrame=frame;}
        public Result latest(){return null;}
    }
    public static void main(String[] args){
        var backend=new Recorder();
        GpuWorldRenderer.install(backend);
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var player=map.spawn(0);
        var renderer=new SceneRenderer();
        renderer.setRenderSize(320,200);
        renderer.x=player.x();renderer.z=player.z();
        renderer.eyeHeight=player.y()+1.62;
        renderer.yaw=.5;renderer.pitch=-.06;renderer.lamp=false;
        renderer.drawPractice(map,List.of(),0);
        var cold=renderer.shellRegionStats();
        require(cold.spatialChecks()>0&&cold.cachedRegions()>0,
            "First House render must evaluate actual spatial light revisions");
        long scene=backend.latestFrame.sceneId();
        renderer.drawPractice(map,List.of(),0);
        var warm=renderer.shellRegionStats();
        require(warm.spatialChecks()==cold.spatialChecks()
                &&warm.generationReuses()>cold.generationReuses(),
            "Identical-frame House VBOs must reuse region stamps without rescanning cells");
        renderer.yaw+=.12;
        renderer.drawPractice(map,List.of(),0);
        var rotated=renderer.shellRegionStats();
        // Rotation may reveal previously unseen chunks; each newly admitted
        // material needs ONE initial lookup, not a fresh scan of every region
        // that was already cached before rotating the camera.
        require(rotated.spatialChecks()-warm.spatialChecks()
                    ==rotated.cachedRegions()-warm.cachedRegions()
                &&rotated.generationReuses()>warm.generationReuses(),
            "Camera rotation must only evaluate newly visible material regions");
        var portal=map.level().portals().stream().filter(p->p.door()).findFirst().orElseThrow();
        map.toggleDoor(portal.id());
        renderer.drawPractice(map,List.of(),0);
        var opened=renderer.shellRegionStats();
        require(opened.spatialChecks()>rotated.spatialChecks(),
            "Real door revisions must re-evaluate affected spatial regions");
        require(backend.latestFrame.sceneId()==scene,
            "Door state must not rebuild the entire GPU scene generation");
        renderer.drawPractice(map,List.of(),0);
        var stable=renderer.shellRegionStats();
        require(stable.spatialChecks()==opened.spatialChecks()
                &&stable.generationReuses()>opened.generationReuses(),
            "Warm door state must stop repeated cell scans after first precise invalidation");
        System.out.println("PASS same-map House shell region caching:"
            +" spatial_checks="+stable.spatialChecks()
            +" generation_reuses="+stable.generationReuses()
            +" retained_regions="+stable.cachedRegions());
    }
}
