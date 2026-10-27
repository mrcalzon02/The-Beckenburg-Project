package org.beckenburg.gpu;

import org.beckenburg.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Matched van-to-House camera turns through the normal gameplay renderer.
 * This is a hardware diagnostic, not installed player/ritual acceptance. */
final class LiveHouseGpuProfile {
    private LiveHouseGpuProfile(){}
    static GpuWorldRenderer.Result await(OpenGlWorldRenderer engine,SceneRenderer renderer)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(60);
        while(System.nanoTime()<deadline){
            var frame=engine.latest();
            if(frame!=null&&frame.frameId()==renderer.submittedGpuFrameId())return frame;
            LockSupport.parkNanos(200000);
        }
        throw new AssertionError("Actual House frame did not complete");
    }
    static double percentile(List<Double> values,double p){var copy=new ArrayList<>(values);Collections.sort(copy);return copy.get(Math.min(copy.size()-1,(int)Math.ceil(copy.size()*p)-1));}
    static void execute(OpenGlWorldRenderer engine)throws Exception{
        var map=new HouseMap();var renderer=new SceneRenderer();renderer.roomLights=new RoomLighting();
        boolean enabled=Boolean.getBoolean("beckenburg.gpu.residentHouse");renderer.setResidentHouse(enabled);
        renderer.setRenderSize(Integer.getInteger("beckenburg.profile.width",712),Integer.getInteger("beckenburg.profile.height",200));
        renderer.setFieldOfView(93);var eye=map.van().spawn(0);var house=map.spawn(0);
        renderer.x=eye.x();renderer.z=eye.z();renderer.eyeHeight=map.van().floorY()+1.62;
        double heading=Math.atan2(house.x()-eye.x(),house.z()-eye.z());renderer.yaw=heading+Math.PI;
        var output=Path.of("build/e2e/gpu/live-house");Files.createDirectories(output);
        long cold=System.nanoTime();renderer.drawNetwork(map,List.of(),-1,0);await(engine,renderer);cold=System.nanoTime()-cold;
        for(int i=0;i<12;i++){renderer.drawNetwork(map,List.of(),-1,0);await(engine,renderer);}
        long initialBuilds=renderer.liveHouseStats().builds(),initialPacked=renderer.liveHouseStats().packedChunks();
        var cpu=new ArrayList<Double>();var elapsed=new ArrayList<Double>();long staticBytes=0;
        var csv=new StringBuilder("sample,room,phase,yaw,cpu_ms,end_to_end_ms,resident_uploaded_bytes,total_uploaded_bytes,resident_reused_bytes,resident_capacity_bytes,static_builds,packed_chunks,recomputed_probes,scene_build_ms,geometry_ms,effects_ms,gl_issue_ms,readback_ms,shadow_rays,normal_rays\n");
        boolean liveClock=Boolean.getBoolean("beckenburg.profile.liveClock"),events=Boolean.getBoolean("beckenburg.profile.events");long clockStart=System.nanoTime();
        for(int i=0;i<120;i++){
            // The cargo spawn can face a monitor wall even when pointing at
            // the House. Use authored yard, entrance and upstairs positions
            // so the timed samples actually expose the expensive building.
            int room=i<40?13:i<80?0:8;var position=map.spawn(room);
            var target=map.spawn(room==0||room==8?4:0);
            renderer.x=position.x();renderer.z=position.z();renderer.eyeHeight=position.y()+1.62;
            renderer.pitch=-.075;renderer.yaw=Math.atan2(target.x()-position.x(),target.z()-position.z())+.60*Math.sin(i*.09);
            String phase="camera";
            if(events&&(i==20||i==100)){
                var door=map.level().portals().stream().filter(p->p.door()).min(Comparator.comparingDouble(p->Math.hypot(p.x()-renderer.x,p.z()-renderer.z))).orElseThrow();
                map.toggleDoor(door.id());phase="door_"+door.id();
            }
            if(events&&i==60){renderer.roomLights.toggle(0);phase="circuit_0";}
            renderer.roomLightTick=liveClock?(int)((System.nanoTime()-clockStart)/100000000L):0;
            long start=System.nanoTime();renderer.drawNetwork(map,List.of(),-1,0);var frame=await(engine,renderer);
            double duration=(System.nanoTime()-start)/1e6;var timing=renderer.frameDiagnostics();var resident=engine.residentUploadStats();var upload=engine.uploadStats();var source=renderer.liveHouseStats();
            if(resident==null||resident.frameId()!=frame.frameId()||upload.frameId()!=frame.frameId())throw new AssertionError("GPU upload counters mismatch actual frame");
            cpu.add(timing.totalNs()/1e6);elapsed.add(duration);staticBytes+=resident.uploadedBytes();
            if(enabled&&(source.builds()!=initialBuilds||!liveClock&&!events&&(source.packedChunks()!=initialPacked||resident.uploadedBytes()!=0)))throw new AssertionError("Camera-only turn rebuilt or uploaded stationary House data");
            var gl=engine.frameTiming();
            csv.append(String.format(Locale.ROOT,"%d,%d,%s,%.6f,%.3f,%.3f,%d,%d,%d,%d,%d,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%d,%d%n",i,room,phase,renderer.yaw,cpu.get(i),duration,resident.uploadedBytes(),upload.uploadedBytes(),resident.reusedBytes(),resident.allocatedVertexBytes(),source.builds(),source.packedChunks(),source.recomputedProbes(),timing.sceneBuildNs()/1e6,timing.geometryNs()/1e6,timing.effectsNs()/1e6,gl.drawIssueNs()/1e6,gl.readbackAndCopyNs()/1e6,timing.lighting().shadowRays(),timing.lighting().normalRays()));
            if(i==0||i==40||i==80||i==119){var image=new BufferedImage(frame.width(),frame.height(),BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,frame.width(),frame.height(),frame.argb(),0,frame.width());ImageIO.write(image,"png",output.resolve("house-turn-"+i+".png").toFile());}
        }
        Files.writeString(output.resolve("frames.csv"),csv.toString());
        String summary=String.format(Locale.ROOT,"resident=%s cold_ms=%.3f samples=%d cpu_p50_ms=%.3f cpu_p95_ms=%.3f cpu_p99_ms=%.3f frame_p50_ms=%.3f frame_p95_ms=%.3f frame_p99_ms=%.3f static_uploaded_bytes=%d%n",enabled,cold/1e6,cpu.size(),percentile(cpu,.5),percentile(cpu,.95),percentile(cpu,.99),percentile(elapsed,.5),percentile(elapsed,.95),percentile(elapsed,.99),staticBytes);
        Files.writeString(output.resolve("summary.txt"),"Source: "+System.getenv().getOrDefault("GITHUB_SHA","local-unversioned")+"\n"+engine.rendererDescription()+"\nViewport: "+renderer.width()+"x"+renderer.height()+" FOV=93 preset="+renderer.psxPreset()+"\n12 van warmup frames; 40 yard, 40 entrance, 40 upstairs camera turns; live circuit clock="+liveClock+". Not physical traversal or installed FPS acceptance.\n"+summary);
        System.out.print(summary);
        if(Boolean.getBoolean("beckenburg.profile.depthParity"))depthParity(engine,map,output);
    }
    static void depthParity(OpenGlWorldRenderer engine,HouseMap map,Path output)throws Exception{
        var reference=new SceneRenderer();var candidate=new SceneRenderer();
        reference.setResidentHouse(false);candidate.setResidentHouse(true);
        for(var renderer:List.of(reference,candidate)){
            renderer.roomLights=new RoomLighting();renderer.setRenderSize(320,200);
            renderer.setPsxPreset(PsxPipeline.Preset.OFF);renderer.lamp=false;
        }
        var csv=new StringBuilder("room,reference_surface_pixels,missing_or_farther_pixels,extra_or_nearer_pixels,max_farther_metres\n");
        for(int room:new int[]{14,0,8}){
            var eye=map.spawn(room);var target=map.spawn(room==0?4:0);
            for(var renderer:List.of(reference,candidate)){
                renderer.x=eye.x();renderer.z=eye.z();renderer.eyeHeight=eye.y()+1.62;
                renderer.yaw=Math.atan2(target.x()-eye.x(),target.z()-eye.z());renderer.pitch=-.075;
            }
            reference.drawNetwork(map,List.of(),-1,0);var before=await(engine,reference);
            candidate.drawNetwork(map,List.of(),-1,0);var after=await(engine,candidate);
            int pixels=0,missing=0,nearer=0;double farthest=0;
            for(int i=0;i<before.depth().length;i++){
                if(Float.isFinite(before.depth()[i]))pixels++;
                double delta=after.depth()[i]-before.depth()[i];
                if(delta>.05){missing++;farthest=Math.max(farthest,delta);}else if(delta<-.05)nearer++;
            }
            csv.append(room).append(',').append(pixels).append(',').append(missing).append(',').append(nearer).append(',').append(farthest).append('\n');
            Files.writeString(output.resolve("depth-parity.csv"),csv.toString());
            if(pixels<250||missing>0||nearer>0)throw new AssertionError("Live House depth disagrees with original geometry: room="+room+" missing="+missing+" nearer="+nearer);
        }
        System.out.println("PASS actual OpenGL depth coverage: van, House interior and upstairs; no original surface lost beyond 5cm depth tolerance");
    }
}
