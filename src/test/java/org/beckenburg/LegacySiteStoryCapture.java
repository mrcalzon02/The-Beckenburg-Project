package org.beckenburg;
import java.nio.file.*;
import java.util.*;
public final class LegacySiteStoryCapture {
    public static void main(String[] args)throws Exception{
        LevelVisualCapture.inspection();var out=Path.of("development/levels/previews/site-story-runtime");Files.createDirectories(out);
        for(var site:List.of(HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL)){
            var map=new HouseMap(site);for(var p:map.level().portals())if(p.door())map.openDoorBetweenRooms(p.a(),p.b());
            var scene=new SceneRenderer();scene.setRenderSize(1120,700);scene.setPsxPreset(PsxPipeline.Preset.OFF);scene.roomLights=new RoomLighting();scene.environmentLight=1;scene.rackStock=Set.of();
            for(var s:map.level().sockets())if(s.type().equals("story")&&s.style().startsWith("ROOM_")){
                var spawn=map.spawn(s.room());LevelVisualCapture.capture(scene,map,out,s.style().toLowerCase(Locale.ROOT),spawn.x(),spawn.y(),spawn.z(),s.x(),s.y()+.06,s.z());
            }
            var eye=map.van().world(0,.8);var target=map.van().world(.83,.8);
            LevelVisualCapture.capture(scene,map,out,"van-case-clipboard",eye.x(),VanStaging.FLOOR_Y-.65,eye.z(),target.x(),VanStaging.FLOOR_Y+.90,target.z());
        }
    }
}
