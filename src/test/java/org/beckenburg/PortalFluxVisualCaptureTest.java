package org.beckenburg;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

/** Actual production software-scene screenshots, never generated concept art.
 * Legacy direct light and doorway-transport light receive the same authored
 * geometry, same map, same camera, same time and completed door poses.
 * The comparison is visual diagnostic only, not a fabricated GPU FPS claim. */
public final class PortalFluxVisualCaptureTest {
    private static final String DISABLE="beckenburg.lighting.portalFlux.disabled";
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    private record Render(BufferedImage image,int[] rgb,long incident,long receiver,long hits){}
    private static Render render(HouseMap map,LevelDefinition.Vec eye,
            LevelDefinition.Vec target,boolean disabled,double clock){
        String original=System.getProperty(DISABLE);
        try{
            if(disabled)System.setProperty(DISABLE,"true");
            else System.clearProperty(DISABLE);
            var view=new SceneRenderer();
            view.setRenderSize(854,480);
            view.setPsxPreset(PsxPipeline.Preset.OFF);
            view.roomLights=new RoomLighting();
            view.rackStock=java.util.Set.of();
            view.lamp=false;
            view.x=eye.x();view.z=eye.z();view.eyeHeight=eye.y()+1.62;
            view.yaw=Math.atan2(target.x()-eye.x(),target.z()-eye.z());
            view.pitch=0;
            view.drawPractice(map,List.of(),clock);
            var image=view.image();
            int[] pixels=image.getRGB(0,0,image.getWidth(),image.getHeight(),
                null,0,image.getWidth());
            // Keep the same raster identity for PNG and pixel comparison.
            var copy=new BufferedImage(image.getWidth(),image.getHeight(),
                BufferedImage.TYPE_INT_RGB);
            copy.setRGB(0,0,image.getWidth(),image.getHeight(),
                pixels,0,image.getWidth());
            return new Render(copy,pixels,0,0,0);
        }finally{
            if(original==null)System.clearProperty(DISABLE);
            else System.setProperty(DISABLE,original);
        }
    }
    private static int delta(Render a,Render b,Path save)throws Exception{
        check(a.rgb().length==b.rgb().length,"Mismatched renderer viewports");
        var diff=new BufferedImage(a.image().getWidth(),a.image().getHeight(),
            BufferedImage.TYPE_INT_RGB);
        int changed=0,largest=0;
        for(int i=0;i<a.rgb().length;i++){
            int left=a.rgb()[i],right=b.rgb()[i];
            int dr=Math.abs((left>>16&255)-(right>>16&255));
            int dg=Math.abs((left>>8&255)-(right>>8&255));
            int db=Math.abs((left&255)-(right&255));
            if(dr+dg+db>0)changed++;
            largest=Math.max(largest,Math.max(dr,Math.max(dg,db)));
            int col=(Math.min(255,dr*3)<<16)
                   |(Math.min(255,dg*3)<<8)|Math.min(255,db*3);
            int x=i%a.image().getWidth(),y=i/a.image().getWidth();
            diff.setRGB(x,y,col);
        }
        ImageIO.write(diff,"png",save.toFile());
        System.out.println("PORTAL REAL PIXEL COMPARISON "+save.getFileName()
            +" changed="+changed+" largest_channel_delta="+largest);
        return changed;
    }
    private static void save(Render view,Path destination)throws Exception{
        check(ImageIO.write(view.image(),"png",destination.toFile()),
            "PNG encoder failed for "+destination);
    }
    public static void main(String[] args)throws Exception{
        LevelVisualCapture.inspection();
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var portal=AuthoredTestSupport.door(map,3,4);
        var observer=AuthoredTestSupport.side(map,portal,4,1.15);
        if(!map.traversable(observer.x(),observer.y(),observer.z()))
            observer=map.spawn(4);
        var target=AuthoredTestSupport.side(map,portal,3,.65);
        Path out=Path.of("build","e2e","portal-flux-capture");
        Files.createDirectories(out);
        var closedOld=render(map,observer,target,true,2.75);
        var closedNew=render(map,observer,target,false,2.75);
        save(closedOld,out.resolve("closed-direct.png"));
        save(closedNew,out.resolve("closed-portal.png"));
        int closedDelta=delta(closedOld,closedNew,out.resolve("closed-delta.png"));
        map.toggleDoor(portal.id());
        // Freeze the physical door in its fully open pose for matched images.
        map.doorOpenFraction(portal.id(),System.nanoTime()+HouseMap.DOOR_SWING_NANOS+1);
        var openOld=render(map,observer,target,true,2.75);
        var openNew=render(map,observer,target,false,2.75);
        save(openOld,out.resolve("open-direct.png"));
        save(openNew,out.resolve("open-portal.png"));
        int openDelta=delta(openOld,openNew,out.resolve("open-delta.png"));
        Files.writeString(out.resolve("comparison.txt"),
            "Real production SceneRenderer, HOUSE, door="+portal.id()+"\n"
            +"Door states: closed and fully open; same scene, 854x480, PSX off, lamp off.\n"
            +"Original light renderer = portalFlux.disabled=true\n"
            +"Portal light renderer = portalFlux.disabled=false\n"
            +"Changed RGB pixels closed="+closedDelta+" open="+openDelta+"\n"
            +"Source level revision="+map.level().revision()+"\n"
            +"This is rendered color comparison, not physical GPU frame timing.\n");
        check(closedDelta==0,"Closed door changed original scene shading");
        System.out.println("PASS actual House software-rendered open/closed portal light A/B screenshots; open_delta="+openDelta);
    }
}
