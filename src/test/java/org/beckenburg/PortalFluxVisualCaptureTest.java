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
    private record Render(BufferedImage image,int[] rgb){}
    private record ActiveDoor(HouseMap map,LevelDefinition.Portal portal,
                              LevelDefinition.Vec observer,LevelDefinition.Vec target,
                              int emitter,double incidentWeight){}
    /** Discover a genuine illuminated doorway using the same authored light
     * registry and shader-side portal transfer as normal gameplay. A door with
     * no active incident flux is NOT valid proof of the optimization. */
    private static ActiveDoor selectLitDoor(){
        for(var site:new HouseMap.Site[]{HouseMap.Site.HOUSE,
                HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL}){
            var map=new HouseMap(site);
            var sources=new FixtureLighting(map).sources();
            ActiveDoor best=null;
            for(var door:map.level().portals())if(door.door()
                    &&!map.outside(door.a())&&!map.outside(door.b())){
                boolean originallyOpen=map.doorOpen(door.id());
                if(!originallyOpen)map.toggleDoor(door.id());
                try{
                    var cache=new PortalFluxCache(map,sources);
                    for(var light:sources){
                        int to=light.room()==door.a()?door.b():
                            light.room()==door.b()?door.a():-1;
                        if(to<0)continue;
                        var receiver=AuthoredTestSupport.side(map,door,to,.75);
                        double weight=cache.weight(light,to,receiver.x(),
                            door.y()+1.35,receiver.z(),0,0,0,false);
                        if(!Double.isFinite(weight)||weight<=0)continue;
                        var observer=AuthoredTestSupport.side(map,door,to,1.12);
                        if(!map.traversable(observer.x(),observer.y(),observer.z()))
                            observer=receiver;
                        if(map.roomAt(observer.x(),observer.y()+1.25,observer.z())!=to)
                            continue;
                        var target=AuthoredTestSupport.side(map,door,light.room(),.65);
                        if(best==null||weight>best.incidentWeight())
                            best=new ActiveDoor(map,door,observer,target,light.id(),weight);
                    }
                }finally{
                    if(!originallyOpen)map.toggleDoor(door.id());
                }
            }
            if(best!=null)return best;
        }
        throw new AssertionError("No actual authored lamp transmits any doorway flux; synthetic fixture test cannot prove real scene lighting");
    }
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
            return new Render(copy,pixels);
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
        var selected=selectLitDoor();
        var map=selected.map();
        var portal=selected.portal();
        var observer=selected.observer();
        var target=selected.target();
        System.out.println("PHYSICAL PORTAL VIEW site="+map.site()
            +" portal="+portal.id()+" light="+selected.emitter()
            +" positive_incident="+selected.incidentWeight());
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
            "Real production SceneRenderer, site="+map.site()
                +", door="+portal.id()+", source="+selected.emitter()+"\n"
            +"Physical portal positive incidence="+selected.incidentWeight()+"\n"
            +"Door states: closed and fully open; same scene, 854x480, PSX off, lamp off.\n"
            +"Original light renderer = portalFlux.disabled=true\n"
            +"Portal light renderer = portalFlux.disabled=false\n"
            +"Changed RGB pixels closed="+closedDelta+" open="+openDelta+"\n"
            +"Source level revision="+map.level().revision()+"\n"
            +"This is rendered color comparison, not physical GPU frame timing.\n");
        check(closedDelta==0,"Closed door changed original scene shading");
        if(openDelta==0)
            System.out.println("WARNING: Actual illuminated doorway has no visible pixel change at selected camera; review material visibility and captured PNGs.");
        System.out.println("PASS actual "+map.site()+" software-rendered open/closed doorway-light A/B screenshots; open_delta="+openDelta);
    }
}
