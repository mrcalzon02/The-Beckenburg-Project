package org.beckenburg;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Completed OpenGL depth readback plus the real shared ethereal fog pass. */
public final class SafeAreaBarrierGpuReview {
    public static void main(String[] args)throws Exception{
        var launcher=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=launcher.getDeclaredMethod("startGpu");start.setAccessible(true);
        var stop=launcher.getDeclaredMethod("stopReviewGpu");stop.setAccessible(true);start.invoke(null);
        try{
            LevelVisualCapture.inspection();Path out=Path.of(args[0]);Files.createDirectories(out);
            var map=new HouseMap();var van=map.van();var renderer=new SceneRenderer();renderer.roomLights=new RoomLighting();
            renderer.setRenderSize(640,400);renderer.setPsxPreset(PsxPipeline.Preset.OFF);
            var p=van.world(4.5,-5.5);var target=van.world(0,.8);
            renderer.x=p.x();renderer.z=p.z();renderer.eyeHeight=2.6;
            renderer.yaw=Math.atan2(target.x()-p.x(),target.z()-p.z());renderer.pitch=Math.atan2(1.5-2.6,Math.hypot(target.x()-p.x(),target.z()-p.z()));
            int[] first=null;
            for(int i=0;i<3;i++){
                double clock=i*.8;renderer.drawNetwork(map,List.of(),-1,clock);
                long frame=renderer.submittedGpuFrameId(),deadline=System.nanoTime()+30_000_000_000L;var gpu=GpuWorldRenderer.active();
                while(gpu.latest()==null||gpu.latest().frameId()!=frame){if(System.nanoTime()>deadline)throw new AssertionError("GPU barrier frame timed out");Thread.sleep(2);}
                renderer.pinCompletedGpuFrame(gpu.latest());renderer.drawNetwork(map,List.of(),-1,clock);
                if(!renderer.frameDiagnostics().gpu()||renderer.safeAreaPixels()<1000)throw new AssertionError("real GPU depth + visible safe haze required");
                ImageIO.write(renderer.image(),"PNG",out.resolve("gpu-van-"+i+".png").toFile());
                if(i==0)first=renderer.pixels().clone();else if(Arrays.equals(first,renderer.pixels()))throw new AssertionError("GPU-composited energy haze must shift");
            }
            System.out.println("PASS actual completed OpenGL frames, production depth-occluded blue barrier and animated transparent material");
        }finally{stop.invoke(null);}
    }
}
