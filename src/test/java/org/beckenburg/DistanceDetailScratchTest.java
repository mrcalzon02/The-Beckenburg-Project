package org.beckenburg;

import java.util.Arrays;
import java.util.Random;

/** Reusing a native-site framebuffer source must be byte-exact to legacy clone path. */
public final class DistanceDetailScratchTest {
    private static void check(boolean good,String reason){if(!good)throw new AssertionError(reason);}
    public static void main(String[] args){
        Random rng=new Random(0xD157A11L);
        for(int trial=0;trial<25;trial++){
            int width=16+trial*3,height=9+trial*2,n=width*height;
            int[] input=new int[n],sky=new int[n],baseline=new int[n],scratchOutput=new int[n],scratch=new int[n];
            double[] depth=new double[n];
            for(int i=0;i<n;i++){
                input[i]=rng.nextInt(0x1000000);sky[i]=rng.nextInt(0x1000000);
                depth[i]=switch(i%12){
                    case 0->Double.POSITIVE_INFINITY;
                    case 1->Double.NaN;
                    case 2->0;
                    case 3->24;
                    case 4->24.01;
                    case 5->-12;
                    default->rng.nextDouble()*180;
                };
            }
            System.arraycopy(input,0,baseline,0,n);
            System.arraycopy(input,0,scratchOutput,0,n);
            DistanceDetail.apply(baseline,depth,width,height,sky);
            DistanceDetail.apply(scratchOutput,depth,width,height,sky,scratch);
            check(Arrays.equals(baseline,scratchOutput),
                "scratch output differs from original native scene at trial "+trial);
            // Even when reusing the same scratch buffer on the next frame,
            // old pixels must never leak into the new scene.
            System.arraycopy(input,0,scratchOutput,0,n);
            DistanceDetail.apply(scratchOutput,depth,width,height,sky,scratch);
            check(Arrays.equals(baseline,scratchOutput),"scratch state leaked across frames");
        }
        int[] pixels=new int[4],sky=new int[4];double[] depth=new double[4];
        try{DistanceDetail.apply(pixels,depth,2,2,sky,pixels);
            throw new AssertionError("aliasing source/output accepted");}
        catch(IllegalArgumentException expected){}
        try{DistanceDetail.apply(pixels,depth,2,2,sky,new int[3]);
            throw new AssertionError("incorrect scratch size accepted");}
        catch(IllegalArgumentException expected){}
        try{DistanceDetail.apply(pixels,depth,2,2,sky,sky);
            throw new AssertionError("sky/scratch alias accepted");}
        catch(IllegalArgumentException expected){}
        System.out.println("PASS: 25 exact depth-filter frame comparisons, scratch reuse, sky alias guards");
    }
}
