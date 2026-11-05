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
    record Result(int passes,long elapsedMs,int staticShellChunks,long staticFaces,
            long staticPreparationMs,int stagedSceneryInstances,String sceneRevision,
            boolean shellReused,boolean sceneryReused,
            int gpuSubmittedPasses,int gpuConfirmedPasses,boolean gpuPrewarmTimedOut){}
    static Result prepare(HouseMap map,SceneRenderer scene,LevelOpeningFlyover flight,
            BooleanSupplier cancelled){
        Objects.requireNonNull(map);Objects.requireNonNull(scene);
        Objects.requireNonNull(flight);Objects.requireNonNull(cancelled);
        var saved=new LevelOpeningFlyover.Pose(scene.x,scene.eyeHeight,scene.z,
            scene.yaw,scene.pitch);
        int width=scene.width(),height=scene.height();
        long start=System.nanoTime();
        int passes=0;int chunks=0;long faces=0;
        int gpuSubmitted=0,gpuConfirmed=0;boolean gpuTimedOut=false;
        long deadline=start+TimeUnit.SECONDS.toNanos(5);
        SceneRenderer.StaticPreparation staged=null;
        try{
            // Stage 2: build the renderer's existing revision-owned immutable
            // shell/material groups and scenery index BEFORE its first draw.
            // This never mutates a door, mission clock, ghost or authority.
            if(!cancelled.getAsBoolean()&&!Thread.currentThread().isInterrupted())
                staged=scene.prepareStaticScene(map);
            // Warm van cargo approach first, then immediate exterior, then
            // authored interior. These are REAL perspective camera passes on
            // the same renderer used by gameplay, not flat-map thumbnails.
            // A GPU submit is nonblocking and can be superseded: on the
            // loading worker only, confirm GL readback before the next pass
            // so the GL owner actually uploads the preceding VBOs/textures.
            scene.setRenderSize(Math.min(480,width),Math.min(270,height));
            for(double progress:new double[]{.925,.60}){
                if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted()
                    ||System.nanoTime()>=deadline)break;
                flight.pose(progress).apply(scene);
                scene.drawPractice(map,List.of(),0);
                passes++;
                var shell=scene.shellChunkStats();
                chunks=Math.max(chunks,shell.totalChunks());
                faces=Math.max(faces,shell.totalFaces());
                var frame=scene.frameDiagnostics();
                if(frame!=null&&frame.gpu()){
                    gpuSubmitted++;
                    long waitNs=Math.min(TimeUnit.MILLISECONDS.toNanos(650),
                        Math.max(0,deadline-System.nanoTime()));
                    boolean confirmed=scene.awaitSubmittedGpuPrewarm(waitNs,cancelled);
                    if(confirmed)gpuConfirmed++;
                    else{gpuTimedOut=true;break;}
                }
            }
            if(!gpuTimedOut&&!cancelled.getAsBoolean()
                    &&!Thread.currentThread().isInterrupted()
                    &&System.nanoTime()<deadline){
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
                    var frame=scene.frameDiagnostics();
                    if(frame!=null&&frame.gpu()){
                        gpuSubmitted++;
                        long waitNs=Math.min(TimeUnit.MILLISECONDS.toNanos(650),
                            Math.max(0,deadline-System.nanoTime()));
                        if(scene.awaitSubmittedGpuPrewarm(waitNs,cancelled))
                            gpuConfirmed++;
                        else gpuTimedOut=true;
                    }
                }
            }
        }finally{
            saved.apply(scene);
            scene.setRenderSize(width,height);
        }
        long elapsed=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start);
        if(staged!=null)BeckenburgDiagnostics.record("LOADING STATIC PREBAKE site="+map.site()
            +" revision="+staged.revision()+" elapsed_ms="
            +TimeUnit.NANOSECONDS.toMillis(staged.elapsedNs())
            +" shell_chunks="+staged.shellChunks()
            +" shell_faces="+staged.shellFaces()
            +" scenery_instances="+staged.sceneryInstances()
            +" shell_reused="+staged.shellReused()
            +" scenery_reused="+staged.sceneryReused()
            +" prewarm_passes="+passes
            +" gpu_submitted_passes="+gpuSubmitted
            +" gpu_confirmed_passes="+gpuConfirmed
            +" gpu_wait_timeout="+gpuTimedOut+" total_ms="+elapsed);
        return new Result(passes,elapsed,chunks,faces,
            staged==null?0:TimeUnit.NANOSECONDS.toMillis(staged.elapsedNs()),
            staged==null?0:staged.sceneryInstances(),
            staged==null?"not-prepared":staged.revision(),
            staged!=null&&staged.shellReused(),staged!=null&&staged.sceneryReused(),
            gpuSubmitted,gpuConfirmed,gpuTimedOut);
    }
}
