package org.beckenburg;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Exact RGB, depth, and animated-clock parity for moving sanctuary views. */
public final class SafeAreaHazeColumnsTest {
    private static void ok(boolean value,String message){
        if(!value)throw new AssertionError(message);
    }
    public static void main(String[] args){
        var dome=new SafeAreaBarrier.Volume(0,0,0,5,1.8,4.5);
        var region=List.of(dome);
        var cache=new SafeAreaHaze.RayCache();
        var random=new Random(0x5341464548415a45L);
        int shaded=0;
        for(int step=0;step<28;step++){
            int width=step>=14?321:320,height=step>=14?201:200;
            double focal=step>=21?175:210;
            int[] direct=new int[width*height],cached=new int[direct.length];
            double[] depths=new double[direct.length];
            for(int i=0;i<direct.length;i++){
                direct[i]=cached[i]=random.nextInt(1<<24);
                depths[i]=i%9==0?Double.POSITIVE_INFINITY:
                    i%9==1?2.4:random.nextDouble()*16;
            }
            double[] before=depths.clone();
            double yaw=step*.021,pitch=-.10+(step%3)*.025;
            double x=step%4==0?.0:(step%4)*.04,z=-7+step*.06,clock=(step/2)*.07;
            int a=SafeAreaHaze.draw(direct,depths,width,height,focal,
                x,1.62,z,yaw,pitch,clock,region);
            int b=SafeAreaHaze.draw(cached,depths,width,height,focal,
                x,1.62,z,yaw,pitch,clock,region,cache);
            ok(a==b&&Arrays.equals(direct,cached),
                "Column cache changed moving view "+step);
            ok(Arrays.equals(before,depths),"Haze may not alter scene depth");
            shaded+=a;
        }
        ok(shaded>2_000,"Moving views must exercise real sanctuary rays");
        for(int width:new int[]{1,83,320,321,960,1920}){
            for(double focal:new double[]{.25,42,210,840}){
                var column=new SafeAreaHaze.RayCache().horizontal(width,focal);
                for(int x=0;x<width;x++){
                    double reference=(x+.5-width*.5)/focal;
                    ok(Double.doubleToLongBits(reference)==Double.doubleToLongBits(column[x]),
                        "Column projection must remain bit-identical");
                }
            }
        }
        System.out.println("PASS moving van haze pixel, occlusion, clock, resize and exact column parity");
    }
}
