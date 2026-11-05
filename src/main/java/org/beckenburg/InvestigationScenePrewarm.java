package org.beckenburg;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Bounded, case-start-only prewarming on the EXACT renderer handed to the
 * player. Reuses authored static shell chunks, nearby scenery batches and
 * lighting visibility caches across the initial camera frames. Never advances
 * the map, ghost, multiplayer authority, or evidence simulation.
 *
 * A GPU context must never be accessed concurrently with active gameplay:
 * callers execute this before enabling the case frame timer.
 */
final class InvestigationScenePrewarm {
    private InvestigationScenePrewarm(){}
    record Result(int passes,long elapsedMs,int staticShellChunks,long staticFaces){}
    static Result prepare(HouseMap map,SceneRenderer scene,LevelOpeningFlyover flight,
            BooleanSupplier cancelled){
        Objects.requireNonNull(map);Objects.requireNonNull(scene);
        Objects.requireNonNull(flight);Objects.requireNonNull(cancelled);
        var saved=new LevelOpeningFlyover.Pose(scene.x,scene.eyeHeight,scene.z,
            scene.yaw,scene.pitch);
        int width=scene.width(),height=scene.height();
        long start=System.nanoTime();
        int passes=0;int chunks=0;long faces=0;
        try{
            // Authoring/visibility warmup, not a fake loading percentage.
            // The initial shot samples a mid-distance view of the site, the
            // approach samples outside van geometry, and final game render
            // supplies the near-hunter view. Only two extra passes.
            scene.setRenderSize(Math.min(480,width),Math.min(270,height));
            for(double progress:new double[]{.60,.84}){
                if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted()
                    ||System.nanoTime()-start>=TimeUnit.SECONDS.toNanos(5))break;
                flight.pose(progress).apply(scene);
                scene.drawPractice(map,List.of(),0);
                passes++;
                var shell=scene.shellChunkStats();
                chunks=Math.max(chunks,shell.totalChunks());
                faces=Math.max(faces,shell.totalFaces());
            }
        }finally{
            saved.apply(scene);
            scene.setRenderSize(width,height);
        }
        return new Result(passes,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start),
            chunks,faces);
    }
}
