package org.beckenburg;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

/**
 * Explicit, local-only, no-per-frame-allocation performance capture. A bounded
 * primitive-array trace belongs to one Swing viewport. No host/network inputs
 * or authoritative simulation are read, sent or modified.
 */
final class FrameTimeCapture {
    static final int CAPACITY=120_000;
    private static final DateTimeFormatter FILE_DATE=
        DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss-SSS",Locale.ROOT)
            .withZone(ZoneOffset.UTC);
    private final String role;
    private final Instant startedUtc;
    private final long startedNs;
    private final FrameCaptureProvenance.Snapshot provenance;
    private final long[] times=new long[CAPACITY],render=new long[CAPACITY];
    private final long[] sceneBuild=new long[CAPACITY],geometry=new long[CAPACITY];
    private final long[] effects=new long[CAPACITY],frameId=new long[CAPACITY];
    private final long[] freshInterval=new long[CAPACITY];
    private final int[] room=new int[CAPACITY],dispatchedEvents=new int[CAPACITY];
    // Optional six-stage breakdown in bounded integer microseconds.
    private final int[] charactersUs=new int[CAPACITY],environmentUs=new int[CAPACITY];
    private final int[] sanctuaryUs=new int[CAPACITY],psxUs=new int[CAPACITY];
    private final int[] graphicsUs=new int[CAPACITY],hauntingUs=new int[CAPACITY];
    // CPU lighting counters. Cumulative columns are explicitly marked in CSV.
    private final int[] lightingShadowRays=new int[CAPACITY],portalRejected=new int[CAPACITY];
    private final int[] portalResolved=new int[CAPACITY],portalTransfers=new int[CAPACITY];
    private final int[] portalIncident=new int[CAPACITY],portalReceiver=new int[CAPACITY];
    private final int[] portalReceiverReuse=new int[CAPACITY];
    private final long[] queueDelayMs=new long[CAPACITY],postDispatchNs=new long[CAPACITY];
    private final byte[] flags=new byte[CAPACITY]; // bit0 GPU, bit1 new frame
    private int size,duplicates;
    private long lastPresentedNs,lastGpuId=Long.MIN_VALUE;
    private boolean previousGpu,haveBackend;

    FrameTimeCapture(String role){
        this(role,System.nanoTime(),Instant.now(),FrameCaptureProvenance.current());
    }
    FrameTimeCapture(String role,long startedNs,Instant startedUtc){
        this(role,startedNs,startedUtc,FrameCaptureProvenance.unavailable());
    }
    FrameTimeCapture(String role,long startedNs,Instant startedUtc,
                     FrameCaptureProvenance.Snapshot provenance){
        this.provenance=java.util.Objects.requireNonNull(provenance);
        String cleaned=role.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+","-");
        this.role=cleaned.isEmpty()?"viewport":cleaned;
        this.startedNs=startedNs;
        this.startedUtc=startedUtc;
    }
    int size(){return size;}
    boolean record(long atNs,long renderNs,boolean gpu,long consumedFrameId,
                   long buildNs,long geometryNs,long effectsNs,int roomId){
        return record(atNs,renderNs,gpu,consumedFrameId,
            buildNs,geometryNs,effectsNs,roomId,0,0,-1L);
    }
    /** Input queue lag is observed on the EDT before this render callback.
     * Delay uses event.getWhen() vs callback time; postDispatchNs is monotonic
     * time from the last handled event to this sample, or -1 if none. */
    boolean record(long atNs,long renderNs,boolean gpu,long consumedFrameId,
                   long buildNs,long geometryNs,long effectsNs,int roomId,
                   int inputEvents,long maxQueueDelayMs,long postDispatchAgeNs){
        return record(atNs,renderNs,gpu,consumedFrameId,buildNs,geometryNs,
            effectsNs,roomId,inputEvents,maxQueueDelayMs,postDispatchAgeNs,null);
    }
    /** Null stage data is allowed for older callers. No GPU timer claims. */
    boolean record(long atNs,long renderNs,boolean gpu,long consumedFrameId,
                   long buildNs,long geometryNs,long effectsNs,int roomId,
                   int inputEvents,long maxQueueDelayMs,long postDispatchAgeNs,
                   SceneRenderer.PostEffectsDiagnostics stages){
        return record(atNs,renderNs,gpu,consumedFrameId,buildNs,geometryNs,
            effectsNs,roomId,inputEvents,maxQueueDelayMs,postDispatchAgeNs,stages,null);
    }
    /** The attached lighting counters are CPU-side and are captured only
     * when the HUD's explicit frame recorder is active. */
    boolean record(long atNs,long renderNs,boolean gpu,long consumedFrameId,
                   long buildNs,long geometryNs,long effectsNs,int roomId,
                   int inputEvents,long maxQueueDelayMs,long postDispatchAgeNs,
                   SceneRenderer.PostEffectsDiagnostics stages,
                   FixtureLighting.FrameStats lights){
        if(size>=CAPACITY)return false;
        if(haveBackend&&gpu!=previousGpu){
            // GPU/CPU fallback cannot turn a backend change into a fake hitch.
            lastPresentedNs=0;
            lastGpuId=Long.MIN_VALUE;
        }
        haveBackend=true;previousGpu=gpu;
        boolean fresh=!gpu||(consumedFrameId>0&&consumedFrameId!=lastGpuId);
        if(gpu&&consumedFrameId>0&&!fresh)duplicates++;
        long interval=0;
        if(fresh){
            if(lastPresentedNs!=0&&atNs>lastPresentedNs)interval=atNs-lastPresentedNs;
            lastPresentedNs=atNs;
        }
        if(gpu&&consumedFrameId>0)lastGpuId=consumedFrameId;
        int i=size++;
        times[i]=atNs;
        render[i]=Math.max(0,renderNs);
        sceneBuild[i]=Math.max(0,buildNs);
        geometry[i]=Math.max(0,geometryNs);
        effects[i]=Math.max(0,effectsNs);
        frameId[i]=gpu?consumedFrameId:0;
        freshInterval[i]=interval;
        room[i]=roomId;
        dispatchedEvents[i]=Math.max(0,inputEvents);
        queueDelayMs[i]=Math.max(0,maxQueueDelayMs);
        postDispatchNs[i]=postDispatchAgeNs<0?-1L:postDispatchAgeNs;
        if(stages!=null){
            charactersUs[i]=micros(stages.charactersNs());
            environmentUs[i]=micros(stages.environmentNs());
            sanctuaryUs[i]=micros(stages.sanctuaryNs());
            psxUs[i]=micros(stages.psxNs());
            graphicsUs[i]=micros(stages.graphicsNs());
            hauntingUs[i]=micros(stages.hauntingNs());
        }
        if(lights!=null){
            lightingShadowRays[i]=boundedCount(lights.shadowRays()+lights.normalRays());
            portalRejected[i]=boundedCount(lights.portalRejectedCandidates());
            portalResolved[i]=boundedCount(lights.portalFluxResolved());
            portalTransfers[i]=boundedCount(lights.portalTransfers());
            portalIncident[i]=boundedCount(lights.portalIncidentRays());
            portalReceiver[i]=boundedCount(lights.portalReceiverRays());
            portalReceiverReuse[i]=boundedCount(lights.portalReceiverHits());
        }
        flags[i]=(byte)((gpu?1:0)|(fresh?2:0));
        return size<CAPACITY;
    }
    record Summary(int samples,int newFrames,int cadenceIntervals,int duplicateGpuReads,
                   int over33,int over50,int over100,double p50Ms,double p95Ms,double p99Ms,
                   long inputEvents,int queueDelayOver50,int queueDelayOver100,long maxQueueDelayMs){}
    Summary summary(){
        double[] intervals=new double[size];
        int seen=0,newFrames=0,over33=0,over50=0,over100=0,queueOver50=0,queueOver100=0;
        long totalInputs=0,worstQueueMs=0;
        for(int i=0;i<size;i++){
            totalInputs+=dispatchedEvents[i];
            if(queueDelayMs[i]>50)queueOver50++;
            if(queueDelayMs[i]>100)queueOver100++;
            worstQueueMs=Math.max(worstQueueMs,queueDelayMs[i]);
            if((flags[i]&2)!=0)newFrames++;
            if(freshInterval[i]<=0)continue;
            double ms=freshInterval[i]/1_000_000.0;
            intervals[seen++]=ms;
            if(ms>33.3)over33++;
            if(ms>50)over50++;
            if(ms>100)over100++;
        }
        if(seen==0)return new Summary(size,newFrames,0,duplicates,0,0,0,0,0,0,
            totalInputs,queueOver50,queueOver100,worstQueueMs);
        Arrays.sort(intervals,0,seen);
        return new Summary(size,newFrames,seen,duplicates,over33,over50,over100,
            percentile(intervals,seen,.50),percentile(intervals,seen,.95),
            percentile(intervals,seen,.99),
            totalInputs,queueOver50,queueOver100,worstQueueMs);
    }
    private static double percentile(double[] sorted,int count,double fraction){
        return sorted[Math.max(0,(int)Math.ceil(count*fraction)-1)];
    }
    Path defaultOutput(){
        String override=System.getProperty("beckenburg.performance.capture.dir","");
        Path folder=override.isBlank()
            ?Path.of(System.getProperty("user.home","."),"Beckenburg","performance")
            :Path.of(override);
        return folder.resolve("frames-"+role+"-"+FILE_DATE.format(startedUtc)+".csv");
    }
    /**
     * A fast second hotkey press can share the same UTC millisecond. Use an
     * atomic CREATE_NEW retry rather than overwrite or silently lose the trace.
     * Called only after the render-owned recording is detached.
     */
    Path saveUnique()throws IOException{
        Path base=defaultOutput();
        String file=base.getFileName().toString();
        int dot=file.lastIndexOf('.');
        String stem=dot>0?file.substring(0,dot):file;
        String suffix=dot>0?file.substring(dot):"";
        for(int attempt=0;attempt<100;attempt++){
            Path target=attempt==0?base:base.resolveSibling(
                stem+"-"+(attempt+1)+suffix);
            try{
                write(target);
                return target;
            }catch(java.nio.file.FileAlreadyExistsException occupied){
                // Existing evidence belongs to another recording. Never modify it.
            }
        }
        throw new java.nio.file.FileAlreadyExistsException(base.toString(),
            null,"100 recording filenames already in use");
    }
    private static int boundedCount(long number){
        return (int)Math.max(0,Math.min(Integer.MAX_VALUE,number));
    }
    private static int micros(long nanos){
        return (int)Math.min(Integer.MAX_VALUE,Math.max(0,nanos/1_000L));
    }
    private static double ms(long nanos){return nanos/1_000_000.0;}
    private static double stageMs(int micros){return micros/1_000.0;}
    /** Called only after the recording is detached from its EDT owner. */
    void write(Path destination)throws IOException{
        Path parent=destination.toAbsolutePath().getParent();
        if(parent!=null)Files.createDirectories(parent);
        Summary s=summary();
        try(BufferedWriter out=Files.newBufferedWriter(destination,StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){
            out.write("# Beckenburg local frame trace; no host simulation changes\n");
            out.write("# started_utc="+startedUtc+"\n");
            out.write("# viewport="+role+"\n");
            out.write("# source_revision="+provenance.sourceRevision()+"\n");
            out.write("# renderer="+provenance.renderer()+"\n");
            out.write("# os="+provenance.os()+"\n");
            out.write("# java_version="+provenance.javaVersion()+"\n");
            out.write("# detail="+provenance.detail()+"\n");
            out.write("# display_setting="+provenance.display()+"\n");
            out.write("# aspect_setting="+provenance.aspect()+"\n");
            out.write("# upscale="+provenance.upscale()+"\n");
            out.write("# antialias="+provenance.antialias()+"\n");
            out.write("# psx_preset="+provenance.psxPreset()+"\n");
            out.write("# field_of_view="+provenance.fieldOfView()+"\n");
            out.write("# presentation_target_hz="+provenance.presentationTargetHz()+"\n");
            out.write("# samples="+s.samples()+"\n");
            out.write("# new_frames="+s.newFrames()+"\n");
            out.write("# cadence_intervals="+s.cadenceIntervals()+"\n");
            out.write("# duplicate_gpu_readbacks="+s.duplicateGpuReads()+"\n");
            out.write("# p50_presented_ms="+s.p50Ms()+"\n");
            out.write("# p95_presented_ms="+s.p95Ms()+"\n");
            out.write("# p99_presented_ms="+s.p99Ms()+"\n");
            out.write("# intervals_over_33ms="+s.over33()+"\n");
            out.write("# intervals_over_50ms="+s.over50()+"\n");
            out.write("# intervals_over_100ms="+s.over100()+"\n");
            out.write("# dispatched_input_events="+s.inputEvents()+"\n");
            out.write("# input_queue_frames_over_50ms="+s.queueDelayOver50()+"\n");
            out.write("# input_queue_frames_over_100ms="+s.queueDelayOver100()+"\n");
            out.write("# max_input_queue_delay_ms="+s.maxQueueDelayMs()+"\n");
            out.write("elapsed_ms,render_ms,presented_interval_ms,backend,frame_id,new_frame,scene_build_ms,geometry_ms,effects_ms,room,dispatched_input_events,max_input_queue_delay_ms,post_dispatch_to_frame_sample_ms,characters_ms,environment_ms,sanctuary_haze_ms,psx_ms,graphics_post_ms,haunting_overlay_ms,light_shadow_rays,portal_rejected_candidates,portal_flux_resolved,portal_transfers,portal_incident_rays_cumulative,portal_receiver_rays_cumulative,portal_receiver_reuse_cumulative\n");
            StringBuilder line=new StringBuilder(160);
            for(int i=0;i<size;i++){
                line.setLength(0);
                line.append(ms(Math.max(0,times[i]-startedNs))).append(',')
                    .append(ms(render[i])).append(',')
                    .append(ms(freshInterval[i])).append(',')
                    .append((flags[i]&1)!=0?"GPU":"CPU").append(',')
                    .append(frameId[i]).append(',')
                    .append((flags[i]&2)!=0?1:0).append(',')
                    .append(ms(sceneBuild[i])).append(',')
                    .append(ms(geometry[i])).append(',')
                    .append(ms(effects[i])).append(',')
                    .append(room[i]).append(',')
                    .append(dispatchedEvents[i]).append(',')
                    .append(queueDelayMs[i]).append(',')
                    .append(postDispatchNs[i]<0?"-1":Double.toString(ms(postDispatchNs[i])))
                    .append(',').append(stageMs(charactersUs[i]))
                    .append(',').append(stageMs(environmentUs[i]))
                    .append(',').append(stageMs(sanctuaryUs[i]))
                    .append(',').append(stageMs(psxUs[i]))
                    .append(',').append(stageMs(graphicsUs[i]))
                    .append(',').append(stageMs(hauntingUs[i]))
                    .append(',').append(lightingShadowRays[i])
                    .append(',').append(portalRejected[i])
                    .append(',').append(portalResolved[i])
                    .append(',').append(portalTransfers[i])
                    .append(',').append(portalIncident[i])
                    .append(',').append(portalReceiver[i])
                    .append(',').append(portalReceiverReuse[i])
                    .append('\n');
                out.write(line.toString());
            }
        }
    }
}
