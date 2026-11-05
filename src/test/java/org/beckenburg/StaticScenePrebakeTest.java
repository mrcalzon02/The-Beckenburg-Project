package org.beckenburg;

/** Stage 2 acceptance: prepare real immutable House data once per revision.
 * A door is dynamic and must not invalidate shell or scenery preparation. */
public final class StaticScenePrebakeTest {
    private static void check(boolean condition,String reason) {
        if(!condition)throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var renderer=new SceneRenderer();
        String initialDoors=map.doorSnapshot();
        var originalSpawn=map.van().spawn(0);
        var cold=renderer.prepareStaticScene(map);
        check(!cold.shellReused()&&!cold.sceneryReused(),
            "Cold House preparation must build real immutable scene indexes");
        check(cold.shellChunks()>0&&cold.shellFaces()>0&&cold.sceneryInstances()>0,
            "Preparation must include actual House shell faces and scenery instances");
        var warmed=renderer.prepareStaticScene(map);
        check(warmed.shellReused()&&warmed.sceneryReused(),
            "Same renderer + same revision must reuse prepared static geometry");
        check(warmed.shellChunks()==cold.shellChunks()
            &&warmed.shellFaces()==cold.shellFaces()
            &&warmed.sceneryInstances()==cold.sceneryInstances(),
            "Unchanged world must keep identical shell and scenery topology");
        check(map.doorSnapshot().equals(initialDoors)&&map.van().spawn(0).equals(originalSpawn),
            "Preparation must not advance doors, van or gameplay authority");

        var portal=map.level().portals().stream().filter(p->p.door())
            .findFirst().orElseThrow();
        map.toggleDoor(portal.id());
        var opened=renderer.prepareStaticScene(map);
        check(opened.shellReused()&&opened.sceneryReused(),
            "Opening an actual House door must NOT invalidate immutable scene indexes");
        check(opened.revision().equals(cold.revision()),
            "Door openness is transient, not an authored topology revision");

        map.configureHiding(map.hidingDensity()==75?70:75);
        var edited=renderer.prepareStaticScene(map);
        check(!edited.shellReused()&&!edited.sceneryReused(),
            "An authored hiding-density revision must rebuild the dependent source index");
        check(!edited.revision().equals(cold.revision()),
            "Edited preparation must carry an explicit distinct revision");

        var separate=new HouseMap(HouseMap.Site.HOUSE);
        var newMap=renderer.prepareStaticScene(separate);
        check(!newMap.shellReused()&&!newMap.sceneryReused(),
            "An unrelated House world must never reuse another world's mutable cache owner");

        var cancelled=new SceneRenderer();
        var camera=new LevelOpeningFlyover.Pose(cancelled.x,cancelled.eyeHeight,
            cancelled.z,cancelled.yaw,cancelled.pitch);
        var result=InvestigationScenePrewarm.prepare(separate,cancelled,
            new LevelOpeningFlyover(separate,camera),()->true);
        check(result.passes()==0&&result.sceneRevision().equals("not-prepared"),
            "Cancelled loading may not perform scene preparation or raster passes");
        check(cancelled.x==camera.x()&&cancelled.eyeHeight==camera.y()
            &&cancelled.z==camera.z()&&cancelled.yaw==camera.yaw()
            &&cancelled.pitch==camera.pitch(),
            "Cancellation must preserve exact user camera state");
        System.out.println("PASS Stage 2 immutable House geometry staging, warm reuse, "
            +"real-door locality, revision/map invalidation, cancellation");
    }
}
