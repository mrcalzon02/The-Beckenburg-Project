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
        // Reproduce the stop/hold/step/stop sequence that exposed the
        // suspected sixth-frame sanctuary hitch. Exact colors and depth
        // must match independent uncached rays through all six repeated
        // frames, including live occluder-depth and animation changes.
        var heldCache=new SafeAreaHaze.RayCache();
        int heldShaded=0;
        for(int pose=0;pose<4;pose++){
            int width=pose>=2?321:320,height=pose>=2?201:200;
            double focal=pose==1?175:210;
            double x=pose==3?0:pose*.17;
            double z=pose==3?0:-8+pose*.36;
            double yaw=pose*.045,pitch=pose==3?-.09:0;
            for(int frame=0;frame<6;frame++){
                int[] uncached=new int[width*height],cached=new int[uncached.length];
                double[] depths=new double[uncached.length];
                for(int i=0;i<uncached.length;i++){
                    uncached[i]=cached[i]=random.nextInt(1<<24);
                    depths[i]=(i+frame)%17==0?2.0:
                        (i+pose)%11==0?Double.POSITIVE_INFINITY:15.0;
                }
                double[] originalDepth=depths.clone();
                double clock=pose*.37+(frame<3?0:frame<5?.18:.41);
                int plain=SafeAreaHaze.draw(uncached,depths,width,height,focal,
                    x,1.62,z,yaw,pitch,clock,region);
                int retained=SafeAreaHaze.draw(cached,depths,width,height,focal,
                    x,1.62,z,yaw,pitch,clock,region,heldCache);
                ok(plain==retained&&Arrays.equals(uncached,cached),
                    "Held-frame cache changed pixels or occlusion pose="+pose+" frame="+frame);
                ok(Arrays.equals(depths,originalDepth),
                    "Held-frame cache changed scene depth pose="+pose+" frame="+frame);
                heldShaded+=plain;
            }
        }
        ok(heldShaded>2_000,"Stop-and-go test must shade the real sanctuary");
        // The native-map atmosphere must also reach the parallel scanline
        // workers and every cached held frame. Vary fog without changing
        // camera/clock to exercise an already valid opacity cache.
        {
        int width=640,height=400;
        double focal=420;
        var largeRegion=List.of(new SafeAreaBarrier.Volume(0,0,0,18,2,12));
        var fogCache=new SafeAreaHaze.RayCache();
        int[] clear=new int[width*height];
        double[] fogDepth=new double[clear.length];
        Arrays.fill(fogDepth,Double.POSITIVE_INFINITY);
        Arrays.fill(clear,0x182838);
        SafeAreaHaze.draw(clear,fogDepth,width,height,focal,0,1.62,0,0,0,.5,largeRegion);
        for(int pass=0;pass<5;pass++){
            int[] direct=new int[clear.length],cached=new int[clear.length];
            Arrays.fill(direct,0x182838);Arrays.fill(cached,0x182838);
            int fog=pass<2?0x183824:0x462218;
            try(var atmosphere=LevelAtmosphere.enter(width,height,focal,pass<3?1:2,fog)){
                int a=SafeAreaHaze.draw(direct,fogDepth,width,height,focal,
                    0,1.62,0,0,0,.5,largeRegion);
                int b=SafeAreaHaze.draw(cached,fogDepth,width,height,focal,
                    0,1.62,0,0,0,.5,largeRegion,fogCache);
                ok(a>96_000&&a==b,"Large sanctuary must exercise parallel lanes");
                ok(Arrays.equals(direct,cached),"Parallel/cached atmosphere differs at pass "+pass);
                ok(!Arrays.equals(clear,cached),"Atmosphere must shade the actual sanctuary");
            }
            ok(Arrays.stream(fogDepth).allMatch(Double::isInfinite),"Fogged sanctuary wrote scene depth");
            ok(!LevelAtmosphere.active(),"Parallel sanctuary leaked atmosphere scope");
        }
        }
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
        System.out.println("PASS moving/held six-frame sanctuary RGB-depth parity, occlusion, clock, resize, exact columns and parallel atmosphere changes");
    }
}
