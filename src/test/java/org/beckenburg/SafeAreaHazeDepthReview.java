package org.beckenburg;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Run the unchanged analytic surface at every pixel as an independent occlusion oracle.
 * With an output directory also time the production shader against the real House van depth image.
 */
public final class SafeAreaHazeDepthReview {
    private record Eye(double x,double y,double z,double yaw,double pitch) {}
    private static final SafeAreaBarrier.Volume DOME = new SafeAreaBarrier.Volume(2,-1,3,5,1.8,4.5);

    private static int blend(int old,double alpha) {
        int blue=0xa9dfff;
        int value=0;
        for (int shift=0;shift<=16;shift+=8) {
            int channel=(int)(((old>>>shift)&255)*(1-alpha)+((blue>>>shift)&255)*alpha);
            value|=channel<<shift;
        }
        return value;
    }

    private static void parity(Eye eye, int seed,SafeAreaHaze.RayCache cache) {
        int width=83,height=49,n=width*height;
        double focal=43, time=seed*.347;
        double cy=Math.cos(eye.yaw),sy=Math.sin(eye.yaw),cp=Math.cos(eye.pitch),sp=Math.sin(eye.pitch);
        int[] expected=new int[n],actual=new int[n];
        double[] depths=new double[n];
        var random=new Random(seed);
        int expectedCount=0;
        for (int row=0;row<height;row++) {
            double v=(height*.5-row-.5)/focal,dy=v*cp+sp,forward=cp-v*sp;
            for (int col=0;col<width;col++) {
                int i=row*width+col;
                actual[i]=expected[i]=random.nextInt(1<<24);
                double u=(col+.5-width*.5)/focal,dx=-u*cy+forward*sy,dz=u*sy+forward*cy;
                double hit=SafeAreaHaze.surface(DOME,eye.x-DOME.x(),eye.y-DOME.y(),eye.z-DOME.z(),dx,dy,dz);
                double boundary=hit+.008;
                depths[i]=switch (i%9) {
                    case 0 -> Double.POSITIVE_INFINITY;
                    case 1 -> 0;
                    case 2 -> Math.nextDown(boundary);
                    case 3 -> boundary;
                    case 4 -> Math.nextUp(boundary);
                    case 5 -> -1;
                    default -> random.nextDouble()*14;
                };
                if (hit<.035||hit>=depths[i]-.008) continue;
                double alpha=SafeAreaHaze.opacity(DOME,eye.x+dx*hit,eye.y+dy*hit-DOME.y(),eye.z+dz*hit,time);
                expected[i]=blend(expected[i],alpha);
                expectedCount++;
            }
        }
        var originalDepth=depths.clone();
        int changed=SafeAreaHaze.draw(actual,depths,width,height,focal,eye.x,eye.y,eye.z,eye.yaw,eye.pitch,time,List.of(DOME),cache);
        if (expectedCount!=changed || !Arrays.equals(expected,actual) || !Arrays.equals(depths,originalDepth))
            throw new AssertionError("Haze color/count/depth mismatch at "+eye+" pixel="+Arrays.mismatch(expected,actual));
    }

    private static void projectionChanges() {
        var cache=new SafeAreaHaze.RayCache();
        var other=new SafeAreaBarrier.Volume(3,-.7,4,3,1.2,3.2);
        // Repeated poses warm the cache, then change only one projection or volume input.
        for(int step=0;step<64;step++){
            int phase=step/8;
            int width=phase>=1?81:83,height=phase>=2?51:49;
            double focal=phase>=3?54:43;
            double yaw=phase>=4?.11:0,pitch=phase>=5?-.1:0;
            var areas=phase==6?List.of(other,DOME):List.of(DOME,other);
            // Also exercise a primary dome large enough to shade in parallel.
            if(phase>=5){width=400;height=260;}
            // The server clock can hold while a character exposes or occludes a ray.
            double clock=(step/4)*.2;
            int count=width*height;
            int[] expected=new int[count],actual=new int[count];
            var rng=new Random(7001+step);
            double[] depth=new double[count];
            for(int n=0;n<count;n++){
                expected[n]=actual[n]=rng.nextInt(1<<24);
                depth[n]=n%3==0?Double.POSITIVE_INFINITY:rng.nextDouble()*9;
            }
            double[] originalDepth=depth.clone();
            int direct=SafeAreaHaze.draw(expected,depth,width,height,focal,2,1,3,yaw,pitch,clock,areas);
            int reused=SafeAreaHaze.draw(actual,depth,width,height,focal,2,1,3,yaw,pitch,clock,areas,cache);
            if(direct!=reused||!Arrays.equals(expected,actual)||!Arrays.equals(originalDepth,depth))
                throw new AssertionError("Changed lens, target or sanctuary order at step "+step);
        }
    }

    private record View(Eye eye,int[] rgb,double[] depth,int depthHash) {}
    private static void benchmark(Path output,boolean moving) throws Exception {
        Files.createDirectories(output);
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var scene=new SceneRenderer();
        scene.setRenderSize(960,540);
        var spawn=map.van().spawn(0);
        scene.roomLights=new RoomLighting();scene.roomLights.setBreaker(true);
        Field focal=SceneRenderer.class.getDeclaredField("focal");focal.setAccessible(true);
        double focalLength=focal.getDouble(scene);
        int width=scene.width(),height=scene.height();
        View[] views=new View[moving?8:1];
        for(int i=0;i<views.length;i++){
            double angle=i*Math.PI/4;
            scene.x=spawn.x()+.22*Math.sin(angle);scene.z=spawn.z()+.18*(1-Math.cos(angle));
            scene.eyeHeight=map.van().floorY()+HunterPosture.STANDING_EYE;
            scene.pitch=-.08+.04*Math.sin(angle);scene.yaw=-Math.PI/2+.38*Math.sin(angle);
            scene.drawNetwork(map,List.of(),-1,1);
            double[] depth=scene.depths().clone();
            views[i]=new View(new Eye(scene.x,scene.eyeHeight,scene.z,scene.yaw,scene.pitch),
                scene.image().getRGB(0,0,width,height,null,0,width),depth,Arrays.hashCode(depth));
        }
        int[] scratch=new int[width*height];
        var areas=List.of(SafeAreaBarrier.van(map.van()));
        var cache=new SafeAreaHaze.RayCache();
        var thread=java.lang.management.ManagementFactory.getThreadMXBean();
        if(!thread.isCurrentThreadCpuTimeSupported())throw new IllegalStateException("Thread CPU clock unavailable");
        if(!thread.isThreadCpuTimeEnabled())thread.setThreadCpuTimeEnabled(true);
        try (var csv=Files.newBufferedWriter(output.resolve("haze.csv"))) {
            csv.write("sample,draw_ns,cpu_ns,changed_pixels,image_hash,depth_hash\n");
            for (int i=-40;i<120;i++) {
                var view=views[Math.floorMod(i,views.length)];
                var eye=view.eye();
                System.arraycopy(view.rgb(),0,scratch,0,scratch.length);
                double clock=1+i*.033;
                long cpu=thread.getCurrentThreadCpuTime(),start=System.nanoTime();
                int changed=SafeAreaHaze.draw(scratch,view.depth(),width,height,focalLength,eye.x(),eye.y(),eye.z(),eye.yaw(),eye.pitch(),clock,areas,cache);
                long elapsed=System.nanoTime()-start;
                cpu=thread.getCurrentThreadCpuTime()-cpu;
                if (i>=0)csv.write(String.format(Locale.ROOT,"%d,%d,%d,%d,%d,%d%n",
                    i,elapsed,cpu,changed,Arrays.hashCode(scratch),view.depthHash()));
            }
        }
    }

    public static void main(String[] args)throws Exception {
        double edge=1e-9;
        var rng=new Random(638104);
        var cache=new SafeAreaHaze.RayCache();
        for (int i=0;i<100;i++) {
            double x=switch(i%6){case 0->2;case 1->7-edge;case 2->7+edge;default->2+rng.nextDouble()*16-8;};
            double y=switch(i%8){case 0->-1-edge;case 1->.8;case 2->3.5-edge;case 3->3.5+edge;default->-4+rng.nextDouble()*10;};
            double z=i%3==0?3:3+rng.nextDouble()*13-6.5;
            var eye=new Eye(x,y,z,rng.nextDouble()*2*Math.PI,rng.nextDouble()*2-1);
            for(int frame=0;frame<4;frame++)parity(eye,i*4+frame,cache);
        }
        projectionChanges();
        System.out.println("PASS 1626800 exact RGB/depth samples with pose changes and live occluders and changed counts against unculled analytic dome");
        if(args.length>0)benchmark(Path.of(args[0]),args.length>1&&args[1].equals("moving"));
    }
}
