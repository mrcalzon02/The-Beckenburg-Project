package org.beckenburg;
import java.nio.file.*;
import java.util.*;
/** Matched runtime views with real circuits; no flashlight or studio fill. */
public final class BlackpineLightingCapture {
    public static void main(String[] args)throws Exception{
        LevelVisualCapture.inspection();var map=BlackpineLevel.map();
        for(var p:map.level().portals())if(p.door())map.openDoorBetweenRooms(p.a(),p.b());
        var out=Path.of("development/levels/previews/mill-lighting");Files.createDirectories(out);
        var scene=new SceneRenderer();scene.setRenderSize(1120,700);scene.setPsxPreset(PsxPipeline.Preset.OFF);
        scene.environmentLight=1;scene.rackStock=Set.of();scene.roomLights=new RoomLighting();
        for(boolean on:new boolean[]{true,false}){
            if(!on)scene.roomLights.toggle(0);
            view(scene,map,out,"logway-"+(on?"lit":"off"),45.2,31.9,42.4,34.0,1.3);
            view(scene,map,out,"underloft-"+(on?"lit":"off"),48.5,32.3,48.5,36.3,1.3);
        }
    }
    static void view(SceneRenderer scene,HouseMap map,Path out,String name,double x,double z,double tx,double tz,double ty)throws Exception{
        double floor=map.floorY(0);if(!map.traversable(x,floor,z))throw new AssertionError("unsupported camera "+name);
        scene.x=x;scene.z=z;scene.eyeHeight=floor+1.62;scene.yaw=Math.atan2(tx-x,tz-z);scene.pitch=Math.atan2(floor+ty-scene.eyeHeight,Math.hypot(tx-x,tz-z));
        scene.lamp=false;scene.drawNetwork(map,List.of(),-1,0);
        javax.imageio.ImageIO.write(scene.image(),"PNG",out.resolve(name+".png").toFile());
        System.out.println("VIEW "+name);
    }
}
