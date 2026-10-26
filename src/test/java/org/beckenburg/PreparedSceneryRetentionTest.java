package org.beckenburg;

import java.util.List;

/** Regression for real renderer reuse, pixel parity and map invalidation. */
public final class PreparedSceneryRetentionTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    private static void lookFromVan(SceneRenderer s,HouseMap map){
        var camera=map.spawn(14);var target=map.spawn(0);
        s.x=camera.x();s.z=camera.z();s.eyeHeight=camera.y()+1.62;
        s.yaw=Math.atan2(target.x()-camera.x(),target.z()-camera.z());
        s.pitch=-.075;
    }
    public static void main(String[] args){
        var house=new HouseMap(HouseMap.Site.HOUSE);
        var renderer=new SceneRenderer();
        renderer.setRenderSize(160,120);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.roomLights=new RoomLighting();
        lookFromVan(renderer,house);
        renderer.drawPractice(house,List.of(),0);
        var first=renderer.preparedSceneryStats();
        check(first.builtInstances()>0&&first.cachedInstances()>0,
            "The House van view must prepare some real static scenery");
        check(first.cachedTriangles()>0&&first.cachedTriangles()<=first.triangleLimit(),
            "Prepared static scenery must respect its memory budget");
        int[] expected=renderer.pixels().clone();
        renderer.drawPractice(house,List.of(),0);
        var warmed=renderer.preparedSceneryStats();
        check(warmed.builtInstances()==first.builtInstances() &&
                warmed.reusedInstances()>first.reusedInstances(),
            "Warm frame must reuse exactly the already prepared scenery");
        check(java.util.Arrays.equals(expected,renderer.pixels()),
            "Prepared scenery must render identical pixels on unchanged frame");
        renderer.yaw+=.25;
        renderer.drawPractice(house,List.of(),0);
        renderer.yaw-=.25;
        renderer.drawPractice(house,List.of(),0);
        var returned=renderer.preparedSceneryStats();
        check(returned.reusedInstances()>warmed.reusedInstances(),
            "Returning to earlier camera angle must reuse prepared geometry");
        var fresh=new HouseMap(HouseMap.Site.HOUSE);
        lookFromVan(renderer,fresh);
        renderer.drawPractice(fresh,List.of(),0);
        var afterMapSwitch=renderer.preparedSceneryStats();
        check(afterMapSwitch.builtInstances()>returned.builtInstances(),
            "A distinct House map must never inherit stale prepared faces");
        check(afterMapSwitch.cachedTriangles()<=afterMapSwitch.triangleLimit(),
            "Prepared scenery memory bound must persist across map transitions");
        System.out.println("PASS prepared House scenery: reuse, exact pixels, rotation, new-map invalidation and budget");
    }
}
