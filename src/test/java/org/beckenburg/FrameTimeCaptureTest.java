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
        var trace=new FrameTimeCapture("LAN HUNTER",base,
            Instant.parse("2026-11-20T20:00:00Z"));
        trace.record(base,4_000_000,true,0,1,2,3,14);
        trace.record(base+20_000_000L,5_000_000,true,10,1,2,3,14);
        trace.record(base+40_000_000L,6_000_000,true,10,1,2,3,14);
        trace.record(base+80_000_000L,7_000_000,true,11,1,2,3,13);
        trace.record(base+200_000_000L,8_000_000,true,12,1,2,3,13);
        var gpu=trace.summary();
        check(gpu.samples()==5&&gpu.newFrames()==3&&gpu.duplicateGpuReads()==1,
            "Repeated readback cannot claim to be a newly presented frame");
        check(gpu.cadenceIntervals()==2&&gpu.over33()==2&&gpu.over100()==1,
            "Real 120ms hitch must not be disguised as a duplicate");
        near(gpu.p50Ms(),60,"GPU p50");
        near(gpu.p95Ms(),120,"GPU p95");
        near(gpu.p99Ms(),120,"GPU p99");
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
        check(rows.stream().filter(v->v.startsWith("20.0,")).count()==1,
            "Each measured render callback gets one time-series row");
        check(rows.stream().anyMatch(v->v.startsWith("elapsed_ms,render_ms,")),
            "CSV columns must be machine-readable");
        boolean refused=false;
        try{trace.write(csv);}catch(java.nio.file.FileAlreadyExistsException expected){refused=true;}
        check(refused,"A second save must never overwrite prior measurements");
        check(trace.defaultOutput().getFileName().toString()
            .startsWith("frames-lan-hunter-20261120-200000"),"UTC/viewport filename");
        Files.delete(csv);Files.delete(dir);
        System.out.println("PASS frame capture true GPU cadence, fallback, p99, CSV and no-overwrite");
    }
}
