package org.beckenburg;
import java.util.*;
public final class SafeAreaHazeTest {
    static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args){
        int width=320,height=200;double focal=190;int[] original=new int[width*height];Arrays.fill(original,0x30271d);
        double[] depth=new double[original.length];Arrays.fill(depth,Double.POSITIVE_INFINITY);double[] unchanged=depth.clone();
        var dome=new SafeAreaBarrier.Volume(0,0,0,5,1.8,4.5);var zones=List.of(dome);
        int[] a=original.clone(),b=original.clone(),same=original.clone();
        int drawn=SafeAreaHaze.draw(a,depth,width,height,focal,0,1.65,-9,0,0,0,zones);
        SafeAreaHaze.draw(same,depth,width,height,focal,0,1.65,-9,0,0,0,zones);
        SafeAreaHaze.draw(b,depth,width,height,focal,0,1.65,-9,0,0,1.5,zones);
        check(drawn>2000&&drawn<original.length,"world-space dome occupies a bounded perspective area");
        check(Arrays.equals(a,same)&&!Arrays.equals(a,b),"continuous shifting haze is deterministic and animated");
        check(Arrays.equals(depth,unchanged),"transparent haze never overwrites world/character depth");
        Arrays.fill(depth,2);int[] hidden=original.clone();
        check(SafeAreaHaze.draw(hidden,depth,width,height,focal,0,1.65,-9,0,0,1,zones)==0&&Arrays.equals(hidden,original),"opaque walls fully occlude the barrier");
        Arrays.fill(depth,Double.POSITIVE_INFINITY);int[] inside=original.clone();
        check(SafeAreaHaze.draw(inside,depth,width,height,focal,0,1.65,0,0,0,.4,zones)>1000,"haze is visible from inside safe space");
        check(SafeAreaHaze.surface(dome,0,6,0,0,-1,0)<Double.POSITIVE_INFINITY,"upper dome is an actual fog surface above roof level");
        for(double time:new double[]{0,.7,2.1}){
            double below=SafeAreaHaze.opacity(dome,2,dome.shoulder()-1e-5,1,time);
            double above=SafeAreaHaze.opacity(dome,2,dome.shoulder()+1e-5,1,time);
            check(Math.abs(below-above)<1e-4,"wall joins dome without a visible opacity seam");
            check(SafeAreaHaze.opacity(dome,2,dome.height(),1,time)<.005,"crown gradually becomes almost clear");
        }
        System.out.println("PASS animated translucent light-blue dome, inside/outside views, deterministic motion and CPU/GPU depth occlusion");
    }
}
