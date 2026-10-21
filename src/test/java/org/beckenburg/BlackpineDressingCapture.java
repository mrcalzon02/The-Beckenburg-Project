package org.beckenburg;
import java.nio.file.*;

/** Actual runtime perspectives for the woodland/site dressing review. */
public final class BlackpineDressingCapture {
    public static void main(String[] args)throws Exception{
        LevelVisualCapture.inspection();var map=BlackpineLevel.map();map.configureHiding(100);
        for(var p:map.level().portals())if(p.door())map.toggleDoor(p.id());
        var scene=new SceneRenderer();scene.setRenderSize(1120,700);scene.setPsxPreset(PsxPipeline.Preset.OFF);
        scene.roomLights=new RoomLighting();scene.environmentLight=1;scene.rackStock=java.util.Set.of();
        Path out=Path.of("development/levels/previews/blackpine-dressing");Files.createDirectories(out);
        LevelVisualCapture.capture(scene,map,out,"01-well-and-worn-paths",31,0,40,26,1.1,36);
        LevelVisualCapture.capture(scene,map,out,"02-foreman-patio-and-chimney",31.5,0,22,26,2.0,15);
        LevelVisualCapture.capture(scene,map,out,"03-cold-cabin-hearth",28.5,0,13,24.95,.85,9.5);
        LevelVisualCapture.capture(scene,map,out,"04-mess-stoop-and-flowers",23.5,0,51,18,1.5,46);
        LevelVisualCapture.capture(scene,map,out,"05-mill-timber-and-work",59,0,46,54.8,.8,41);
        LevelVisualCapture.capture(scene,map,out,"06-layered-eastern-woodland",63.5,0,32,78,3,31);
        LevelVisualCapture.capture(scene,map,out,"07-low-planting-and-cabins",21,0,23,27,.8,17);
        LevelVisualCapture.capture(scene,map,out,"08-open-yard-retained",31,0,51,32,1.2,28);
    }
}
