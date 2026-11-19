package org.beckenburg;

/** Same GPU texture can be painted repeatedly but must count as one presented frame. */
public final class PresentedFrameCadenceTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void near(double actual,double expected,String why){
        check(Math.abs(actual-expected)<1e-6,why+" "+actual+" vs "+expected);
    }
    public static void main(String[] args){
        PresentedFrameCadence sample=new PresentedFrameCadence();
        long ns=1_000_000_000L;
        sample.record(ns,true,0);
        check(sample.snapshot().distinct()==0,"pending GPU is not an actual frame");
        sample.record(ns,true,100);
        sample.record(ns+20_000_000L,true,100);
        sample.record(ns+40_000_000L,true,100);
        sample.record(ns+60_000_000L,true,101);
        sample.record(ns+80_000_000L,true,101);
        sample.record(ns+100_000_000L,true,102);
        var metrics=sample.snapshot();
        check(metrics.distinct()==3&&metrics.duplicates()==3,
            "duplicate GPU readbacks must not masquerade as FPS");
        near(metrics.p50(),40,"p50 distinct GPU interval");
        near(metrics.p95(),60,"p95 distinct GPU interval");
        check(metrics.over33()==2&&metrics.over50()==1&&metrics.over100()==0,
            "hitch counts over honest distinct display intervals");
        sample.record(ns+120_000_000L,false,0);
        sample.record(ns+140_000_000L,false,0);
        var cpu=sample.snapshot();
        check(cpu.distinct()==2&&cpu.duplicates()==0,"CPU redraws are newly rasterized");
        near(cpu.p50(),20,"CPU loop interval");
        sample.record(ns+200_000_000L,true,104);
        check(sample.snapshot().distinct()==1&&sample.snapshot().intervals()==0,
            "backend changes never synthesize a giant paused frame interval");
        System.out.println("PASS: distinct GPU frame intervals, duplicate readback, stall buckets and CPU fallback");
    }
}
