package org.beckenburg;
import java.nio.file.*;

/** Actual runtime perspectives for the woodland/site dressing review. */
public final class BlackpineDressingCapture {
    private static void capture(SceneRenderer scene,HouseMap map,Path out,String name,double x,double y,double z,double tx,double ty,double tz)throws Exception{
        LevelVisualCapture.capture(scene,map,out,name,x,y+map.groundHeight(x,z,12),z,tx,ty+map.groundHeight(tx,tz,12),tz);
    }
    public static void main(String[] args)throws Exception{
        LevelVisualCapture.inspection();var map=BlackpineLevel.map();map.configureHiding(100);
        for(var p:map.level().portals())if(p.door())map.toggleDoor(p.id());
        var scene=new SceneRenderer();scene.setRenderSize(1120,700);scene.setPsxPreset(PsxPipeline.Preset.OFF);
        scene.roomLights=new RoomLighting();scene.environmentLight=1;scene.rackStock=java.util.Set.of();
        Path out=Path.of("development/levels/previews/blackpine-dressing");Files.createDirectories(out);
        capture(scene,map,out,"01-well-and-worn-paths",31,0,40,26,1.1,36);
        capture(scene,map,out,"02-foreman-patio-and-chimney",31.5,0,22,26,2.0,15);
        capture(scene,map,out,"03-cold-cabin-hearth",28.5,0,13,24.95,.85,9.5);
        capture(scene,map,out,"04-mess-stoop-and-flowers",23.5,0,51,18,1.5,46);
        capture(scene,map,out,"05-mill-timber-and-work",59,0,46,54.8,.8,41);
        capture(scene,map,out,"06-layered-eastern-woodland",63.5,0,32,78,3,31);
        capture(scene,map,out,"07-low-planting-and-cabins",21,0,23,27,.8,17);
        capture(scene,map,out,"08-open-yard-retained",31,0,51,32,1.2,28);
    }
}
