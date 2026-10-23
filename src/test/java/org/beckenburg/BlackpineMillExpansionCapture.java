package org.beckenburg;
import java.nio.file.*;
import java.util.*;
/** Inspection lighting only; every capture is the actual runtime renderer. */
public final class BlackpineMillExpansionCapture {
    public static void main(String[] args)throws Exception{
        LevelVisualCapture.inspection();var map=BlackpineLevel.map();
        for(var p:map.level().portals())if(p.door())map.openDoorBetweenRooms(p.a(),p.b());
        var scene=new SceneRenderer();scene.setRenderSize(1120,700);scene.setPsxPreset(PsxPipeline.Preset.OFF);
        scene.roomLights=new RoomLighting();scene.environmentLight=1;scene.rackStock=Set.of();
        var out=Path.of("development/levels/previews/mill-expansion");Files.createDirectories(out);
        double[][] poses={{43.5,36.3,41.25,36.3,1.4},{60.2,22.6,60.4,20.75,1.0},{47.5,51.5,49.45,51.6,1.25},{45,27.5,42.2,34.5,1.4}};
        String[] names={"dust-extractor","timber-rack","safety-station","mill-working-floor"};
        for(int i=0;i<poses.length;i++){
            var q=poses[i];double y=map.groundHeight(q[0],q[1],0);
            if(i==1)y=map.floorY(10);if(i==2)y=map.floorY(8);
            if(!map.traversable(q[0],y,q[1]))throw new AssertionError("authored camera unsupported or obstructed "+names[i]);
            LevelVisualCapture.capture(scene,map,out,names[i],q[0],y,q[1],q[2],y+q[4],q[3]);
        }
    }
}
