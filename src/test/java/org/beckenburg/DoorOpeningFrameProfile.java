package org.beckenburg;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Real authored House, real renderer and real interactive door state/animation.
 * Windows CI records performance, not a host-dependent FPS pass threshold.
 * 'submission' installs a recording backend, NOT a working OpenGL device.
 * Run each backend in its own JVM because GPU installation is process-wide.
 */
public final class DoorOpeningFrameProfile {
    private DoorOpeningFrameProfile(){}

    private static final class SubmissionRecorder implements GpuWorldRenderer {
        GpuWorldRenderer.Frame previous;
        private GpuWorldRenderer.Result result;
        private long changedResources, uploadedBytes, generationChanges;
        private long frameRevisions, frameBytes, frameSceneChanges;
        private final java.util.Map<Long,Long> resources=new java.util.HashMap<>();
        @Override public boolean ready(){return true;}
        @Override public String rendererDescription(){return "GPU submission recorder; not hardware"; }
        @Override public void submit(GpuWorldRenderer.Frame frame){
            long beforeRevisions=changedResources,beforeBytes=uploadedBytes;
            long beforeScenes=generationChanges;
            if(previous!=null&&previous.sceneId()!=frame.sceneId()){
                generationChanges++;
                resources.clear();
            }
            for(var surface:frame.surfaces()){
                if(surface.resourceId()==0){
                    uploadedBytes+=(long)surface.vertices().length*Float.BYTES;
                }else if(!java.util.Objects.equals(
                        resources.get(surface.resourceId()),surface.revision())){
                    resources.put(surface.resourceId(),surface.revision());
                    uploadedBytes+=(long)surface.vertices().length*Float.BYTES;
                    changedResources++;
                }
            }
            previous=frame;
            // Per-submission deltas distinguish one door action from the
            // cumulative totals (which also include warmup and baseline).
            frameRevisions=changedResources-beforeRevisions;
            frameBytes=uploadedBytes-beforeBytes;
            frameSceneChanges=generationChanges-beforeScenes;
            // Pure recorder reports no hardware completion; this does not
            // claim pixels, GPU latency, VRAM, or GPU frame-rate acceptance.
            result=null;
        }
        @Override public GpuWorldRenderer.Result latest(){return result;}
        long sceneId(){return previous==null?-1:previous.sceneId();}
        long changedResources(){return changedResources;}
        long uploadedBytes(){return uploadedBytes;}
        long generationChanges(){return generationChanges;}
        long frameRevisions(){return frameRevisions;}
        long frameBytes(){return frameBytes;}
        long frameSceneChanges(){return frameSceneChanges;}
    }

    private static final class Sample {
        final String phase;
        final double millis;
        Sample(String phase,double millis){this.phase=phase;this.millis=millis;}
    }
    private static double percentile(List<Sample> samples,String category,double fraction){
        double[] sorted=samples.stream()
            .filter(s->s.phase.equals(category)).mapToDouble(s->s.millis).sorted().toArray();
        if(sorted.length==0)return 0;
        return sorted[Math.min(sorted.length-1,
            Math.max(0,(int)Math.ceil(sorted.length*fraction)-1))];
    }
    private static long gcMillis(){
        long out=0;
        for(var gc:ManagementFactory.getGarbageCollectorMXBeans()){
            if(gc.getCollectionTime()>0)out+=gc.getCollectionTime();
        }
        return out;
    }
    private static long heapUsed(){
        var rt=Runtime.getRuntime();
        return rt.totalMemory()-rt.freeMemory();
    }

    public static void main(String[] args)throws Exception {
        boolean submission=args.length>0&&args[0].equals("submission");
        String backend=submission?"submission-recorder":"cpu-software";
        SubmissionRecorder recorder=submission?new SubmissionRecorder():null;
        if(recorder!=null)GpuWorldRenderer.install(recorder);
        Path root=Path.of("build","e2e","door-opening-profiler");
        Files.createDirectories(root);
        HouseMap map=new HouseMap(HouseMap.Site.HOUSE);
        var portal=map.level().portals().stream().filter(p->p.door())
            .filter(p->p.a()==0||p.b()==0).findFirst()
            .orElseGet(()->map.level().portals().stream()
                .filter(p->p.door()).findFirst().orElseThrow());
        var renderer=new SceneRenderer();
        renderer.setRenderSize(320,200);
        // Stable view 1.6m above floor and about 1.4m from the selected
        // actual portal; identical camera for baseline/toggle/recovery.
        renderer.x=portal.x()+(portal.alongX()?0:1.4);
        renderer.z=portal.z()+(portal.alongX()?1.4:0);
        renderer.eyeHeight=portal.y()+1.62;
        renderer.yaw=Math.atan2(portal.x()-renderer.x,portal.z()-renderer.z);
        renderer.pitch=-.03;
        renderer.lamp=false;
        List<Sample> samples=new ArrayList<>();
        List<Sample> revisionSamples=new ArrayList<>(),byteSamples=new ArrayList<>();
        StringBuilder csv=new StringBuilder("backend,phase,toggle,frame,door_open,door_fraction,wall_ms,render_ms,build_ms,geometry_ms,effects_ms,shadow_rays,normal_rays,invalidated_cells,lighting_samples,cache_hits,world_builds,world_hits,world_bytes,scene_id,scene_changes,resource_revisions,submitted_bytes,heap_used_bytes,gc_ms,frame_resource_revisions,frame_submitted_bytes,frame_scene_changes\n");
        long baselineGc=gcMillis(),initialScene=-1;
        int frame=0;
        long maxRays=0,maxInvalidated=0;
        for(int step=0;step<4+10+16*4;step++){
            String phase;
            int toggle=-1;
            if(step<4)phase="warmup";
            else if(step<14)phase="baseline";
            else {
                int offset=step-14;
                toggle=offset/4;
                phase=offset%4==0?"toggle":offset%4==3?"recovery":"swing";
                if(offset%4==0)map.toggleDoor(portal.id());
            }
            long begin=System.nanoTime();
            renderer.drawPractice(map,List.of(),frame/30.0);
            long elapsed=System.nanoTime()-begin;
            var m=renderer.frameDiagnostics();
            if(m==null)throw new IllegalStateException("No actual House render diagnostics");
            var light=m.lighting();
            var resident=renderer.worldResidentStats();
            long sceneId=recorder==null?-1:recorder.sceneId();
            if(initialScene<0&&sceneId>0)initialScene=sceneId;
            maxRays=Math.max(maxRays,light.shadowRays()+light.normalRays());
            maxInvalidated=Math.max(maxInvalidated,light.invalidatedCells());
            samples.add(new Sample(phase,elapsed/1_000_000.0));
            csv.append(String.format(Locale.ROOT,
                "%s,%s,%d,%d,%s,%.4f,%.3f,%.3f,%.3f,%.3f,%.3f,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d%n",
                backend,phase,toggle,frame,map.doorOpen(portal.id()),
                map.doorOpenFraction(portal.id()),elapsed/1e6,
                m.totalNs()/1e6,m.sceneBuildNs()/1e6,
                m.geometryNs()/1e6,m.effectsNs()/1e6,
                light.shadowRays(),light.normalRays(),light.invalidatedCells(),
                light.samples(),light.cacheHits(),resident.builds(),resident.hits(),
                resident.bytes(),sceneId,recorder==null?0:recorder.generationChanges(),
                recorder==null?0:recorder.changedResources(),
                recorder==null?0:recorder.uploadedBytes(),heapUsed(),gcMillis(),
                recorder==null?0:recorder.frameRevisions(),
                recorder==null?0:recorder.frameBytes(),
                recorder==null?0:recorder.frameSceneChanges()));
            if(recorder!=null){
                revisionSamples.add(new Sample(phase,recorder.frameRevisions()));
                byteSamples.add(new Sample(phase,recorder.frameBytes()));
            }
            frame++;
            // Exercise actual 450ms swing rather than snapping only at endpoints.
            // Slow CPU draws also count as actual frame latency; never assert FPS.
            if(step>=14&&((step-14)%4)!=3)Thread.sleep(35);
        }
        Files.writeString(root.resolve("door-"+backend+".csv"),
            csv.toString(),StandardCharsets.UTF_8);
        StringBuilder report=new StringBuilder();
        report.append("PROFILER source=actual HouseMap and SceneRenderer; mode=").append(backend)
              .append("; door_id=").append(portal.id())
              .append("; view=near authored portal; renderer=320x200\n");
        report.append("CPU timing includes headless runner overhead; ")
              .append("submission-recorder is NOT OpenGL GPU timing.\n");
        for(String phase:List.of("baseline","toggle","swing","recovery")){
            report.append(String.format(Locale.ROOT,
                "%s p50=%.3f p95=%.3f p99=%.3f ms%n",phase,
                percentile(samples,phase,.50),percentile(samples,phase,.95),
                percentile(samples,phase,.99)));
        }
        if(recorder!=null)report.append(String.format(Locale.ROOT,
            "toggle_frame_resource_revisions_p50=%.0f p95=%.0f"
                +" toggle_frame_submitted_bytes_p50=%.0f p95=%.0f%n",
            percentile(revisionSamples,"toggle",.50),percentile(revisionSamples,"toggle",.95),
            percentile(byteSamples,"toggle",.50),percentile(byteSamples,"toggle",.95)));
        report.append("peak_shadow_rays=").append(maxRays)
              .append(" peak_invalidated_cells=").append(maxInvalidated)
              .append(" gc_delta_ms=").append(gcMillis()-baselineGc)
              .append(" scene_generations=").append(recorder==null?"n/a":recorder.generationChanges())
              .append(" resident_resource_revisions=").append(recorder==null?"n/a":recorder.changedResources())
              .append(" uploaded_bytes=").append(recorder==null?"n/a":recorder.uploadedBytes())
              .append('\n');
        Files.writeString(root.resolve("summary-"+backend+".txt"),
            report.toString(),StandardCharsets.UTF_8);
        System.out.print(report);
        // Structural correctness only; timing thresholds are strictly diagnostic.
        if(frame!=78||Files.size(root.resolve("door-"+backend+".csv"))==0)
            throw new AssertionError("Incomplete actual door profile");
    }
}
