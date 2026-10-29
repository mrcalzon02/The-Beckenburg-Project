package org.beckenburg;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Same-instance, completed-frame hardware profile. Waypoints are scripted,
 * not a substitute for player navigation or installed-product acceptance. */
public final class CastlePerformanceReview {
    private CastlePerformanceReview(){}
    public static void main(String[] args)throws Exception{
        Class<?> desktop=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=desktop.getDeclaredMethod("startGpu");start.setAccessible(true);
        var stop=desktop.getDeclaredMethod("stopReviewGpu");stop.setAccessible(true);
        start.invoke(null);
        try{execute();}finally{stop.invoke(null);}
    }
    private static Object property(Object target,String method){
        try{return target.getClass().getMethod(method).invoke(target);}
        catch(ReflectiveOperationException unavailable){return null;}
    }
    private static long number(Object target,String method){
        Object result=target==null?null:property(target,method);
        return result instanceof Number n?n.longValue():-1;
    }
    private static long gc(){return ManagementFactory.getGarbageCollectorMXBeans()
        .stream().mapToLong(b->Math.max(0,b.getCollectionTime())).sum();}
    private static long allocated(){
        var bean=ManagementFactory.getThreadMXBean();
        return bean instanceof com.sun.management.ThreadMXBean b
            ?b.getThreadAllocatedBytes(Thread.currentThread().getId()):0;
    }
    public static void execute()throws Exception{
        var backend=Objects.requireNonNull(GpuWorldRenderer.active(),"Actual OpenGL required");
        Path output=Path.of(System.getProperty("beckenburg.profile.output","build/castle-profile"));
        Files.createDirectories(output);
        int samples=Integer.getInteger("beckenburg.profile.samples",60);
        int warmup=Integer.getInteger("beckenburg.profile.warmup",12);
        int width=Integer.getInteger("beckenburg.profile.width",640),height=width*5/8;
        String site=System.getProperty("beckenburg.profile.site","CASTLE");
        boolean castle=site.equals("CASTLE"),on=Boolean.parseBoolean(System.getProperty("beckenburg.profile.lights","true"));
        boolean freeze=Boolean.getBoolean("beckenburg.profile.freeze");
        CastleHubMap hub=castle?new CastleHubMap():null;
        HouseMap map=castle?null:new HouseMap(HouseMap.Site.valueOf(site));
        var renderer=new SceneRenderer();renderer.setRenderSize(width,height);
        renderer.roomLights=new RoomLighting();renderer.roomLights.setBreaker(on);renderer.lamp=false;
        double[][] poses=castle?new double[][]{
            {"courtyard".hashCode(),13.5,1.62,20.5,.1,-.05},
            {"store".hashCode(),32.3,1.55,27.5,-Math.PI/2,-.08},
            {"library".hashCode(),17.5,5.02,9.2,-2.6,-.1},
            {"exterior".hashCode(),22,24,57,Math.PI,-.5}}
            :new double[][]{
            {"van".hashCode(),map.van().spawn(0).x(),map.van().floorY()+1.62,map.van().spawn(0).z(),map.van().placement().yaw(),-.09},
            {"interior".hashCode(),map.spawn(0).x(),map.spawn(0).y()+1.62,map.spawn(0).z(),Math.PI/3,-.1}};
        String[] names=castle?new String[]{"courtyard","store","library","exterior"}:new String[]{"van","interior"};
        Files.writeString(output.resolve("environment.txt"),"site="+site+"\nGPU="+backend.rendererDescription()+
            "\njava="+System.getProperty("java.runtime.version")+"\nOS="+System.getProperty("os.name")+
            "\nCPU="+System.getenv("PROCESSOR_IDENTIFIER")+"\nprocessors="+Runtime.getRuntime().availableProcessors()+
            "\nresolution="+width+"x"+height+"\nlights="+on+"\nfreeze="+freeze+
            "\nsamples="+samples+"\nwarmup="+warmup+"\npreset="+renderer.psxPreset()+"\n");
        try(var csv=Files.newBufferedWriter(output.resolve("frames.csv"))){
            csv.write("site,view,sample,total_ms,cpu_ms,build_ms,geometry_ms,effects_ms,gl_issue_ms,readback_copy_ms,gl_total_ms,faces,frustum_faces,triangles,lights,light_samples,cache_hits,shadow_rays,considered_sources,invalidated_cells,uploaded_bytes,reused_bytes,batches,allocated_bytes,gc_ms\n");
            for(int p=0;p<poses.length;p++){
                double[] pose=poses[p];renderer.x=pose[1];renderer.eyeHeight=pose[2];renderer.z=pose[3];
                renderer.yaw=pose[4];renderer.pitch=pose[5];
                var times=new ArrayList<Double>();
                for(int i=-warmup;i<samples;i++){
                    // Sweep a small continuous camera turn and advance cosmetic time.
                    renderer.yaw=pose[4]+(freeze?0:Math.sin((i+warmup)*.085)*.12);
                    double clock=freeze?2.5:2.5+(i+warmup)/30.0;
                    renderer.roomLightTick=(int)(clock*10);
                    long a=allocated(),g=gc(),start=System.nanoTime();
                    if(castle)renderer.drawCastle(hub,clock,on);else renderer.drawNetwork(map,List.of(),-1,clock);
                    long cpu=System.nanoTime()-start,expected=renderer.submittedGpuFrameId();
                    GpuWorldRenderer.Result result;
                    long deadline=System.nanoTime()+30_000_000_000L;
                    while((result=backend.latest())==null||result.frameId()!=expected){
                        if(System.nanoTime()>deadline)throw new IllegalStateException("GPU timeout "+site+"/"+names[p]);
                        Thread.sleep(1);
                    }
                    long total=System.nanoTime()-start;
                    Object d=renderer.frameDiagnostics(),l=d==null?null:property(d,"lighting");
                    Object timing=property(backend,"frameTiming"),upload=property(backend,"uploadStats");
                    if(i>=0){
                        times.add(total/1e6);
                        csv.write(String.format(Locale.ROOT,"%s,%s,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d%n",
                            site,names[p],i,total/1e6,cpu/1e6,number(d,"sceneBuildNs")/1e6,number(d,"geometryNs")/1e6,number(d,"effectsNs")/1e6,
                            number(timing,"drawIssueNs")/1e6,number(timing,"readbackAndCopyNs")/1e6,number(timing,"totalNs")/1e6,
                            number(d,"faces"),number(d,"frustumFaces"),number(d,"submittedTriangles"),number(l,"lights"),number(l,"samples"),number(l,"cacheHits"),
                            number(l,"shadowRays"),number(l,"consideredSources"),number(l,"invalidatedCells"),number(upload,"uploadedBytes"),number(upload,"reusedBytes"),
                            number(upload,"uploadedBatches")+number(upload,"reusedBatches"),allocated()-a,gc()-g));
                    }
                    if(i==samples-1){
                        renderer.pinCompletedGpuFrame(result);
                        if(castle)renderer.drawCastle(hub,clock,on);else renderer.drawNetwork(map,List.of(),-1,clock);
                        ImageIO.write(renderer.image(),"png",output.resolve(names[p]+".png").toFile());
                    }
                }
                Collections.sort(times);
                System.out.printf(Locale.ROOT,"PROFILE %s %s p50=%.2f p95=%.2f p99=%.2f ms%n",site,names[p],
                    times.get(times.size()/2),times.get(Math.min(times.size()-1,(int)Math.ceil(times.size()*.95)-1)),times.get(Math.min(times.size()-1,(int)Math.ceil(times.size()*.99)-1)));
                csv.flush();
            }
        }
    }
}
