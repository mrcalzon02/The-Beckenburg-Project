package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Synthetic frame chronology with no Swing, OpenGL or network dependency. */
public final class FrameTimeCaptureTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static void near(double got,double want,String why){
        check(Math.abs(got-want)<.0001,why+": "+got+" vs "+want);
    }
    public static void main(String[] args)throws Exception{
        long base=1_000_000_000L;
        var metadata=new FrameCaptureProvenance.Snapshot(
            "a1b2c3", "NVIDIA GTX 1660 SUPER", "Windows 10", "17",
            "HIGH","NATIVE","AUTO","CRISP","OFF","CLASSIC",93,60);
        var trace=new FrameTimeCapture("LAN HUNTER",base,
            Instant.parse("2026-11-20T20:00:00Z"),metadata);
        trace.record(base,4_000_000,true,0,1,2,3,14);
        trace.record(base+20_000_000L,5_000_000,true,10,1,2,3,14,
            2,48,3_000_000L,new SceneRenderer.PostEffectsDiagnostics(
                1_000_000L,2_000_000L,3_000_000L,4_000_000L,5_000_000L,6_000_000L),
            new FixtureLighting.FrameStats(1,2,3,4,5,6,7,8,9,10,
                11,12,13,14,15,16,17,18,19));
        trace.record(base+40_000_000L,6_000_000,true,10,1,2,3,14);
        trace.record(base+80_000_000L,7_000_000,true,11,1,2,3,13,
            1,124,14_000_000L);
        trace.record(base+200_000_000L,8_000_000,true,12,1,2,3,13);
        var gpu=trace.summary();
        check(gpu.samples()==5&&gpu.newFrames()==3&&gpu.duplicateGpuReads()==1,
            "Repeated readback cannot claim to be a newly presented frame");
        check(gpu.cadenceIntervals()==2&&gpu.over33()==2&&gpu.over100()==1,
            "Real 120ms hitch must not be disguised as a duplicate");
        near(gpu.p50Ms(),60,"GPU p50");
        near(gpu.p95Ms(),120,"GPU p95");
        near(gpu.p99Ms(),120,"GPU p99");
        check(gpu.inputEvents()==3&&gpu.queueDelayOver50()==1
                &&gpu.queueDelayOver100()==1&&gpu.maxQueueDelayMs()==124,
            "Queue dispatch backlog must remain independent from GPU cadence");
        trace.record(base+600_000_000L,1_000_000,false,0,0,0,0,-1);
        trace.record(base+620_000_000L,1_000_000,false,0,0,0,0,-1);
        var cpu=trace.summary();
        check(cpu.cadenceIntervals()==3&&cpu.over100()==1,
            "Backend switching cannot fabricate a 400ms gap");
        near(cpu.p50Ms(),60,"cross-backend median");
        Path dir=Files.createTempDirectory("beckenburg-frame-test-");
        Path csv=dir.resolve("trace.csv");
        trace.write(csv);
        List<String> rows=Files.readAllLines(csv);
        check(rows.contains("# duplicate_gpu_readbacks=1"),"CSV duplicate count missing");
        check(rows.contains("# p99_presented_ms=120.0"),"Exported p99 must match");
        check(rows.contains("# dispatched_input_events=3"),"Input count header missing");
        check(rows.contains("# max_input_queue_delay_ms=124"),"Queue max header missing");
        check(rows.contains("# source_revision=a1b2c3"),"Build provenance header missing");
        check(rows.contains("# renderer=NVIDIA GTX 1660 SUPER"),"GPU model header missing");
        check(rows.contains("# psx_preset=CLASSIC"),"PSX preset header missing");
        check(rows.contains("# presentation_target_hz=60"),
            "CSV must identify rendering rate for comparable physical traces");
        check(rows.stream().anyMatch(v->v.contains(",14,2,48,3.0,")),
            "CSV input columns must preserve per-frame queue delay and event age");
        check(rows.stream().anyMatch(v->v.contains(
                    ",1.0,2.0,3.0,4.0,5.0,6.0,8,14,15,16,17,18,19")),
            "Post-render stages and portal ray counters must survive CSV export");
        check(rows.stream().anyMatch(v->v.startsWith("elapsed_ms,render_ms,")
                    &&v.endsWith("portal_receiver_reuse_cumulative")),
            "CSV must label post-render stages independently");
        check(rows.stream().anyMatch(v->v.startsWith("elapsed_ms,render_ms,")
                &&v.contains("portal_flux_resolved")
                &&v.contains("light_shadow_rays")),
            "Capture must label ray counts and portal reuse separately from GPU cadence");
        check(rows.stream().filter(v->v.startsWith("20.0,")).count()==1,
            "Each measured render callback gets one time-series row");
        check(rows.stream().anyMatch(v->v.startsWith("elapsed_ms,render_ms,")),
            "CSV columns must be machine-readable");
        boolean refused=false;
        try{trace.write(csv);}catch(java.nio.file.FileAlreadyExistsException expected){refused=true;}
        check(refused,"A second save must never overwrite prior measurements");
        check(trace.defaultOutput().getFileName().toString()
            .startsWith("frames-lan-hunter-20261120-200000"),"UTC/viewport filename");
        String previous=System.getProperty("beckenburg.performance.capture.dir");
        Path first,second;
        try{
            System.setProperty("beckenburg.performance.capture.dir",dir.toString());
            first=trace.saveUnique();
            second=trace.saveUnique();
            check(!first.equals(second),"Same-millisecond recordings need distinct paths");
            check(Files.exists(first)&&Files.exists(second),"Both recordings preserved");
            check(Files.readAllLines(first).equals(Files.readAllLines(second)),
                "Collision avoidance must not alter captured samples");
            Files.delete(first);Files.delete(second);
        }finally{
            if(previous==null)System.clearProperty("beckenburg.performance.capture.dir");
            else System.setProperty("beckenburg.performance.capture.dir",previous);
        }
        check(FrameCaptureProvenance.clean("GPU\nFake=Header,#oops").indexOf('\n')<0,
            "Driver information cannot inject a second CSV metadata row");
        Files.delete(csv);Files.delete(dir);
        System.out.println("PASS frame capture true GPU cadence, fallback, p99, CSV and no-overwrite");
    }
}
