package org.beckenburg;

/** Measures actual host mutex times without altering network/ghost authority. */
public final class LanHostLockTelemetryTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static void close(double a,double b,String why){
        check(Math.abs(a-b)<.000001,why+": "+a+" expected "+b);
    }
    public static void main(String[] args){
        var disabled=new LanHostLockTelemetry(false);
        for(int i=0;i<1000;i++)disabled.record(LanHostLockTelemetry.Section.REALTIME_INPUT,100_000_000,200_000_000);
        check(disabled.snapshot().realtime().count()==0,"diagnostics disabled by default");
        var telem=new LanHostLockTelemetry(true);
        for(int i=1;i<=100;i++){
            telem.record(LanHostLockTelemetry.Section.REALTIME_INPUT,i*1_000_000L,2_000_000L);
            if(i%10==0)telem.record(LanHostLockTelemetry.Section.HOST_TICK,2_000_000L,100_000_000L);
        }
        var snap=telem.snapshot();
        check(snap.realtime().count()==100,"exact movement sample count");
        close(snap.realtime().waitP50Ms(),50,"movement mutex wait p50");
        close(snap.realtime().waitP95Ms(),95,"movement mutex wait p95");
        close(snap.realtime().waitP99Ms(),99,"movement mutex wait p99");
        check(snap.realtime().waitOver20Ms()==80,"large input wait count");
        check(snap.tick().count()==10&&snap.tick().holdOver50Ms()==10,
            "long real host ticks are reported, not silently hidden");
        for(int i=0;i<400;i++)
            telem.record(LanHostLockTelemetry.Section.OTHER_COMMAND,(i+1)*1_000L,50_000L);
        check(telem.snapshot().commands().count()==400,"ring survives wrap without losing total count");
        check(telem.localReport().contains("HOST_LOCK real_input_count=100"),
            "local-only report carries exact sample provenance");
        System.out.println("PASS: host mutex wait/hold p50 p95 p99, 256-sample ring, opt-in instrumentation");
    }
}
