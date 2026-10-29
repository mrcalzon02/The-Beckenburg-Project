package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Functional checks against completed real OpenGL frames. This is separate
 * from the headless submission recorder and from installed gameplay testing. */
public final class WorldResidencyGpuReview {
    private WorldResidencyGpuReview(){}
    private static final Path ROOT=Path.of(System.getProperty("beckenburg.residency.review.output","build/world-residency-review"));
    private static final StringBuilder LOG=new StringBuilder("step,frame,upload_bytes,reused_bytes,shadow_rays,world_builds,world_bytes\n");
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static long number(Object target,String name)throws Exception{
        return ((Number)target.getClass().getMethod(name).invoke(target)).longValue();
    }
    private static GpuWorldRenderer.Result frame(String name,SceneRenderer renderer,HouseMap map,CastleHubMap castle,double time,boolean on)throws Exception{
        renderer.roomLightTick=(int)(time*10);
        if(map==null)renderer.drawCastle(castle,time,on);else renderer.drawNetwork(map,List.of(),-1,time);
        var gpu=GpuWorldRenderer.active();long expected=renderer.submittedGpuFrameId(),deadline=System.nanoTime()+30_000_000_000L;
        GpuWorldRenderer.Result result;
        while((result=gpu.latest())==null||result.frameId()!=expected){
            if(System.nanoTime()>deadline)throw new AssertionError("Actual GPU timed out at "+name);
            Thread.sleep(1);
        }
        Object stats=gpu.getClass().getMethod("uploadStats").invoke(gpu);
        check(number(stats,"frameId")==expected,"Upload statistics must belong to this completed frame");
        long upload=number(stats,"uploadedBytes"),reused=number(stats,"reusedBytes");
        var world=renderer.worldResidentStats();
        LOG.append(name).append(',').append(expected).append(',').append(upload).append(',').append(reused).append(',')
            .append(renderer.frameDiagnostics().lighting().shadowRays()).append(',').append(world.builds()).append(',').append(world.bytes()).append('\n');
        Files.writeString(ROOT.resolve("frames.csv"),LOG);
        if(name.endsWith("repeat")){
            check(upload==0,"Unchanged actual "+name+" unexpectedly uploaded "+upload+" bytes");
            check(reused>0,"Unchanged actual "+name+" must reuse GPU buffers");
        }
        return result;
    }
    private static void save(String name,SceneRenderer renderer,HouseMap map,CastleHubMap castle,GpuWorldRenderer.Result result,double time,boolean on)throws Exception{
        renderer.pinCompletedGpuFrame(result);
        if(map==null)renderer.drawCastle(castle,time,on);else renderer.drawNetwork(map,List.of(),-1,time);
        ImageIO.write(renderer.image(),"png",ROOT.resolve(name+".png").toFile());
    }
    private static long energy(GpuWorldRenderer.Result frame){long result=0;for(int p:frame.argb())if((p>>>24)>127)result+=(p>>16&255)+(p>>8&255)+(p&255);return result;}
    private static GpuWorldRenderer.Result liveTextureFrame(long scene,long resource,int material,float[] vertices,StringBuilder report,String label)throws Exception{
        var gpu=GpuWorldRenderer.active();long id=GpuWorldRenderer.nextId();
        gpu.submit(new GpuWorldRenderer.Frame(scene,id,320,200,153,0,0,
            List.of(new GpuWorldRenderer.Surface(material,vertices,resource,1))));
        long deadline=System.nanoTime()+30_000_000_000L;
        while(gpu.latest()==null||gpu.latest().frameId()!=id){if(System.nanoTime()>deadline)throw new AssertionError("Live texture GPU timeout");Thread.sleep(1);}
        Object textures=gpu.getClass().getMethod("textureStats").invoke(gpu),meshes=gpu.getClass().getMethod("uploadStats").invoke(gpu);
        check(number(textures,"frameId")==id,"Texture evidence must belong to this exact frame");
        if(label.endsWith("repeat"))check(number(textures,"uploads")==0,"Unchanged live pixels must not reupload");
        if(!label.endsWith("first"))check(number(meshes,"uploadedBytes")==0,"Live screen updates must not reupload static screen geometry");
        report.append(label).append(',').append(id).append(',').append(number(textures,"uploads")).append(',')
            .append(number(textures,"uploadedBytes")).append(',').append(number(meshes,"uploadedBytes")).append('\n');
        return gpu.latest();
    }
    private static void liveTextures()throws Exception{
        float[] vertices={-1,-.7f,3,0,0,1,1,1, 1,-.7f,3,1,0,1,1,1, 1,.7f,3,1,1,1,1,1,
                          -1,-.7f,3,0,0,1,1,1, 1,.7f,3,1,1,1,1,1, -1,.7f,3,0,1,1,1,1};
        var report=new StringBuilder("step,frame,texture_uploads,texture_bytes,mesh_bytes\n");
        long scene=GpuWorldRenderer.nextId(),resource=GpuWorldRenderer.nextId();
        int tv=GlbProps.televisionMaterial(1002,2,1002);GlbProps.updateTelevision(tv,true,100);
        var first=liveTextureFrame(scene,resource,tv,vertices,report,"tv-first");
        GlbProps.updateTelevision(tv,true,101);var second=liveTextureFrame(scene,resource,tv,vertices,report,"tv-changed");
        int changed=0;for(int i=0;i<first.argb().length;i++)if(first.argb()[i]!=second.argb()[i])changed++;
        check(changed>500,"Actual GPU television noise must update after its source pixels change");
        liveTextureFrame(scene,resource,tv,vertices,report,"tv-repeat");
        scene=GpuWorldRenderer.nextId();resource=GpuWorldRenderer.nextId();
        var image=new java.awt.image.BufferedImage(256,256,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics=image.createGraphics();graphics.setColor(new java.awt.Color(16,150,40));graphics.fillRect(0,0,256,256);graphics.dispose();
        GlbProps.updateVanMap(image);first=liveTextureFrame(scene,resource,GlbProps.VAN_MAP_MATERIAL,vertices,report,"map-first");
        image=new java.awt.image.BufferedImage(256,256,java.awt.image.BufferedImage.TYPE_INT_RGB);
        graphics=image.createGraphics();graphics.setColor(new java.awt.Color(40,40,220));graphics.fillRect(0,0,256,256);graphics.dispose();
        GlbProps.updateVanMap(image);second=liveTextureFrame(scene,resource,GlbProps.VAN_MAP_MATERIAL,vertices,report,"map-changed");
        changed=0;for(int i=0;i<first.argb().length;i++)if(first.argb()[i]!=second.argb()[i])changed++;
        check(changed>500,"Actual GPU map display must reflect the next host-approved image");
        liveTextureFrame(scene,resource,GlbProps.VAN_MAP_MATERIAL,vertices,report,"map-repeat");
        Files.writeString(ROOT.resolve("live-textures.csv"),report);
    }
    public static void execute()throws Exception{
        check(GpuWorldRenderer.active()!=null,"Actual GPU required");Files.createDirectories(ROOT);
        var renderer=new SceneRenderer();renderer.setRenderSize(640,400);renderer.roomLights=new RoomLighting();renderer.lamp=false;
        var castle=new CastleHubMap();renderer.x=32.3;renderer.z=27.5;renderer.eyeHeight=1.55;renderer.yaw=-Math.PI/2;renderer.pitch=-.08;
        frame("castle-first",renderer,null,castle,2.5,true);
        var base=frame("castle-repeat",renderer,null,castle,2.5,true);
        save("castle-lit",renderer,null,castle,base,2.5,true);
        renderer.lamp=true;var torch=frame("castle-torch-repeat",renderer,null,castle,2.5,true);
        check(energy(torch)>energy(base),"Real GPU flashlight must brighten retained Castle geometry");
        renderer.lamp=false;renderer.yaw+=.04;frame("castle-turn",renderer,null,castle,2.5,true);
        frame("castle-turn-repeat",renderer,null,castle,2.5,true);
        frame("castle-flicker",renderer,null,castle,4.3,true);
        check(renderer.frameDiagnostics().lighting().shadowRays()==0,"Actual candle frame must not retrace static shadows");
        var blackout=frame("castle-blackout",renderer,null,castle,4.3,false);
        check(energy(blackout)<energy(base),"Real Castle switches/blackout must darken resident geometry");
        save("castle-blackout",renderer,null,castle,blackout,4.3,false);
        frame("castle-blackout-repeat",renderer,null,castle,4.3,false);
        castle.toggleDoor(CastleDoorSlots.all().stream().filter(s->s.gate()).findFirst().orElseThrow().id());
        frame("castle-door",renderer,null,castle,4.3,true);
        frame("castle-door-repeat",renderer,null,castle,4.3,true);
        renderer.setRenderSize(800,500);frame("castle-resize",renderer,null,castle,4.3,true);
        frame("castle-resize-repeat",renderer,null,castle,4.3,true);
        renderer.setRenderSize(640,400);
        for(var site:HouseMap.Site.values()){
            var map=new HouseMap(site);var at=map.spawn(0);renderer.x=at.x();renderer.z=at.z();renderer.eyeHeight=at.y()+1.62;renderer.yaw=Math.PI/3;renderer.pitch=-.1;
            frame(site+"-first",renderer,map,null,2.5,true);
            var result=frame(site+"-repeat",renderer,map,null,2.5,true);
            save(site.toString().toLowerCase(Locale.ROOT),renderer,map,null,result,2.5,true);
            renderer.lamp=true;frame(site+"-torch-repeat",renderer,map,null,2.5,true);renderer.lamp=false;
        }
        renderer.x=13.5;renderer.z=20.5;renderer.eyeHeight=1.62;renderer.yaw=.1;
        frame("castle-return",renderer,null,castle,4.3,true);
        frame("castle-return-repeat",renderer,null,castle,4.3,true);
        liveTextures();
        System.out.println("PASS actual OpenGL Castle and all10 map types: zero unchanged uploads, real torch, flicker, blackout, door, resize and return");
    }
}
