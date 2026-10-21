package org.beckenburg;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Actual runtime inspection views of the camp graybox and the two physical postures. */
public final class BlackpineCapture {
    public static void main(String[] args)throws Exception{
        LevelVisualCapture.inspection();
        var map=BlackpineLevel.map();map.configureHiding(100);
        for(var p:map.level().portals())if(p.door())map.toggleDoor(p.id());
        var scene=new SceneRenderer();scene.setRenderSize(960,600);scene.setPsxPreset(PsxPipeline.Preset.OFF);
        scene.roomLights=new RoomLighting();scene.environmentLight=1;scene.rackStock=java.util.Set.of();
        Path out=Path.of("development/levels/previews/blackpine");Files.createDirectories(out);
        LevelVisualCapture.capture(scene,map,out,"01-yard-and-mill",30,0,40,45,1.2,28);
        LevelVisualCapture.capture(scene,map,out,"02-compact-cabin",27,0,14,26,1.3,9);
        LevelVisualCapture.capture(scene,map,out,"03-mill-and-stair",43,0,28,50.5,2,30);
        LevelVisualCapture.capture(scene,map,out,"04-loft-guard-and-return",48,3,35,50.5,3.4,30);
        LevelVisualCapture.capture(scene,map,out,"05-camp-spacing",24,0,25,30,1.3,10);
        scene.x=9;scene.z=30;scene.yaw=Math.PI/2;scene.pitch=0;
        for(boolean low:new boolean[]{false,true}){
            scene.eyeHeight=HunterPosture.eye(low);scene.lamp=true;
            scene.drawNetwork(map,java.util.List.of(),-1,0);
            ImageIO.write(scene.image(),"PNG",out.resolve("blackpine-"+(low?"07-crouched-shrub":"06-standing-shrub")+".png").toFile());
        }
        System.out.println("Saved real runtime Blackpine graybox and crouch inspection images.");
    }
}
