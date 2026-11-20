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
    private final long[] times=new long[CAPACITY],render=new long[CAPACITY];
    private final long[] sceneBuild=new long[CAPACITY],geometry=new long[CAPACITY];
    private final long[] effects=new long[CAPACITY],frameId=new long[CAPACITY];
    private final long[] freshInterval=new long[CAPACITY];
    private final int[] room=new int[CAPACITY];
    private final byte[] flags=new byte[CAPACITY]; // bit0 GPU, bit1 new frame
    private int size,duplicates;
    private long lastPresentedNs,lastGpuId=Long.MIN_VALUE;
    private boolean previousGpu,haveBackend;

    FrameTimeCapture(String role){this(role,System.nanoTime(),Instant.now());}
    FrameTimeCapture(String role,long startedNs,Instant startedUtc){
        String cleaned=role.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+","-");
        this.role=cleaned.isEmpty()?"viewport":cleaned;
        this.startedNs=startedNs;
        this.startedUtc=startedUtc;
    }
    int size(){return size;}
    boolean record(long atNs,long renderNs,boolean gpu,long consumedFrameId,
                   long buildNs,long geometryNs,long effectsNs,int roomId){
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
        flags[i]=(byte)((gpu?1:0)|(fresh?2:0));
        return size<CAPACITY;
    }
    record Summary(int samples,int newFrames,int cadenceIntervals,int duplicateGpuReads,
                   int over33,int over50,int over100,double p50Ms,double p95Ms,double p99Ms){}
    Summary summary(){
        double[] intervals=new double[size];
        int seen=0,newFrames=0,over33=0,over50=0,over100=0;
        for(int i=0;i<size;i++){
            if((flags[i]&2)!=0)newFrames++;
            if(freshInterval[i]<=0)continue;
            double ms=freshInterval[i]/1_000_000.0;
            intervals[seen++]=ms;
            if(ms>33.3)over33++;
            if(ms>50)over50++;
            if(ms>100)over100++;
        }
        if(seen==0)return new Summary(size,newFrames,0,duplicates,0,0,0,0,0,0);
        Arrays.sort(intervals,0,seen);
        return new Summary(size,newFrames,seen,duplicates,over33,over50,over100,
            percentile(intervals,seen,.50),percentile(intervals,seen,.95),
            percentile(intervals,seen,.99));
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
    private static double ms(long nanos){return nanos/1_000_000.0;}
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
            out.write("elapsed_ms,render_ms,presented_interval_ms,backend,frame_id,new_frame,scene_build_ms,geometry_ms,effects_ms,room\n");
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
                    .append(room[i]).append('\n');
                out.write(line.toString());
            }
        }
    }
}
