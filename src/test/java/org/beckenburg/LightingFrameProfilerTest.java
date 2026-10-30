package org.beckenburg;

import dev.mrcalzon.lighting.LightModel;
import dev.mrcalzon.lighting.LightingFrameProfiler;

/** Opt-in diagnostic, never an art/balance/release gate. */
public final class LightingFrameProfilerTest {
    private static void check(boolean ok,String detail){
        if(!ok)throw new AssertionError(detail);
    }
    private static void approx(double got,double want,String detail){
        if(Math.abs(got-want)>.000001)throw new AssertionError(detail+": "+got+" expected "+want);
    }
    public static void main(String[] args){
        var p=new LightingFrameProfiler(4);
        for(int i=1;i<=5;i++)
            p.record(i*1_000_000L,2_000_000L,3_000_000L,
                     i*100_000L,i*200_000L,100*i,10*i,20*i,i%2==0);
        var window=p.snapshot();
        check(window.samples()==4,"Rolling buffer must evict the oldest sample");
        approx(window.frameP50Ms(),3,"Correct rolling p50");
        approx(window.frameP95Ms(),5,"Correct rolling p95");
        approx(window.frameP99Ms(),5,"Correct rolling p99");
        approx(window.psxPostP95Ms(),.5,"Separate PSX stage p95");
        approx(window.effectsPostP95Ms(),1,"Separate effects stage p95");
        approx(window.postTotalP95Ms(),1.5,"Combined post-processing p95");
        check(window.peakTriangles()==500,"Triangle high-water mark");
        check(window.peakShadowRays()==50,"Ray high-water mark");
        check(window.peakInvalidatedCells()==100,"Invalidation high-water mark");
        check(window.gpuReadbackMisses()==2,"Delayed GPU frame reporting");
        approx(LightModel.diffusionFade(2,8,1),.75,
            "100% diffusion must match the original linear falloff");
        approx(LightModel.diffusionFade(2,8,.25),1,
            "A narrow diffusion band must preserve inner-core strength");
        approx(LightModel.diffusionFade(7,8,.25),.5,
            "Diffuse edge has linear falloff inside the requested band");
        approx(LightModel.diffusionFade(9,8,.25),0,
            "Outside radius is never illuminated");
        System.out.println("PASS bounded frame percentiles, stage attribution, GPU misses, diffusion radius invariants");
    }
}
