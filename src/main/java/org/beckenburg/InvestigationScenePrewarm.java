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
            // The first two passes cover the site and the van approach.
            // The third (budget-permitting) pass warms the first authored
            // interior so the player does not pay the cold wall/ceiling
            // cache cost the moment they leave the expedition van.
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
            if(!cancelled.getAsBoolean()&&!Thread.currentThread().isInterrupted()
                    &&System.nanoTime()-start<TimeUnit.SECONDS.toNanos(5)){
                // Warm the genuine authored room-zero spawn, never an invented
                // generic building rectangle. No map/door/host state changes.
                var entry=map.spawn(0);
                if(Double.isFinite(entry.x()+entry.y()+entry.z())){
                    scene.x=entry.x();scene.eyeHeight=entry.y()+1.62;
                    scene.z=entry.z();
                    scene.yaw=Math.atan2(map.van().placement().x()-entry.x(),
                        map.van().placement().z()-entry.z());
                    scene.pitch=-.10;
                    scene.drawPractice(map,List.of(),0);
                    passes++;
                    var shell=scene.shellChunkStats();
                    chunks=Math.max(chunks,shell.totalChunks());
                    faces=Math.max(faces,shell.totalFaces());
                }
            }
        }finally{
            saved.apply(scene);
            scene.setRenderSize(width,height);
        }
        return new Result(passes,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start),
            chunks,faces);
    }
}
