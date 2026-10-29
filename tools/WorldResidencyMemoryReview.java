package org.beckenburg;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Opt-in real GPU lifecycle diagnostic, deliberately separate from FPS runs.
 * Compile against the packaged GPU jar and launch with isolated preferences. */
public final class WorldResidencyMemoryReview {
    private static long count(Object object,String field)throws Exception{
        var member=object.getClass().getDeclaredField(field);member.setAccessible(true);
        return ((Map<?,?>)member.get(object)).size();
    }
    private static void frame(SceneRenderer renderer,HouseMap map,CastleHubMap castle,double clock)throws Exception{
        renderer.roomLightTick=(int)(clock*10);
        if(map==null)renderer.drawCastle(castle,clock,true);else renderer.drawNetwork(map,List.of(),-1,clock);
        long expected=renderer.submittedGpuFrameId(),deadline=System.nanoTime()+30_000_000_000L;
        while(GpuWorldRenderer.active().latest()==null||GpuWorldRenderer.active().latest().frameId()!=expected){
            if(System.nanoTime()>deadline)throw new AssertionError("Lifecycle GPU timeout");Thread.sleep(1);
        }
    }
    private static void castlePose(SceneRenderer renderer){renderer.x=13.5;renderer.z=20.5;renderer.eyeHeight=1.62;renderer.yaw=.1;renderer.pitch=-.05;}
    public static void main(String[] args)throws Exception{
        Class<?> desktop=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=desktop.getDeclaredMethod("startGpu");start.setAccessible(true);
        var stop=desktop.getDeclaredMethod("stopReviewGpu");stop.setAccessible(true);
        Path output=Path.of(System.getProperty("beckenburg.memory.output","build/memory-review"));Files.createDirectories(output);
        start.invoke(null);
        try(var log=Files.newBufferedWriter(output.resolve("lifecycle.csv"))){
            log.write("cycle,phase,heap_used_bytes,retained_geometry_bytes,backend_meshes,backend_textures\n");
            var renderer=new SceneRenderer();renderer.setRenderSize(640,400);renderer.roomLights=new RoomLighting();renderer.lamp=false;
            var castle=new CastleHubMap();
            for(int cycle=0;cycle<6;cycle++){
                for(var site:List.of(HouseMap.Site.HOUSE,HouseMap.Site.SCHOOL,HouseMap.Site.HOLLOW_CREEK)){
                    var map=new HouseMap(site);var spawn=map.spawn(0);renderer.x=spawn.x();renderer.z=spawn.z();renderer.eyeHeight=spawn.y()+1.62;renderer.yaw=Math.PI/3;renderer.pitch=-.1;
                    for(int i=0;i<18;i++){
                        if(i==6)renderer.roomLights.setBreaker(false);
                        if(i==12)renderer.roomLights.setBreaker(true);
                        frame(renderer,map,null,2.5+i/30.0);
                    }
                    long bytes=renderer.worldResidentStats().bytes();
                    if(bytes<0||bytes>128L*1024*1024)throw new AssertionError("Resident geometry exceeded its declared bound");
                    log.write(cycle+","+site+","+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())+","+bytes+","+count(GpuWorldRenderer.active(),"meshes")+","+count(GpuWorldRenderer.active(),"textures")+"\n");
                }
                castlePose(renderer);for(int i=0;i<18;i++)frame(renderer,null,castle,2.5+i/30.0);
                // This is a lifecycle/memory diagnostic, not a frame-time sample.
                System.gc();Thread.sleep(250);
                long heap=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
                long meshes=count(GpuWorldRenderer.active(),"meshes"),textures=count(GpuWorldRenderer.active(),"textures");
                log.write(cycle+",CASTLE_POST_GC,"+heap+","+renderer.worldResidentStats().bytes()+","+meshes+","+textures+"\n");log.flush();
                System.out.println("MEMORY cycle="+cycle+" post_gc_heap="+heap+" resident_bytes="+renderer.worldResidentStats().bytes()+" meshes="+meshes+" textures="+textures);
            }
        }finally{stop.invoke(null);}
    }
}
