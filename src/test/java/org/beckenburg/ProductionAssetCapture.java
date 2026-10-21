package org.beckenburg;
import java.nio.file.*;
import java.util.*;
/** Native runtime placement review, separately from the model's bright studio render. */
public final class ProductionAssetCapture {
    public static void main(String[] args)throws Exception{
        LevelVisualCapture.inspection();var map=BlackpineLevel.map();for(var p:map.level().portals())if(p.door())map.openDoorBetweenRooms(p.a(),p.b());
        var scene=new SceneRenderer();scene.setRenderSize(1120,700);scene.setPsxPreset(PsxPipeline.Preset.OFF);scene.roomLights=new RoomLighting();scene.environmentLight=1;scene.rackStock=Set.of();
        var out=Path.of("development/levels/previews/model-production");Files.createDirectories(out);
        for(var p:map.level().props())if(BlackpineProductionAssets.ASSETS.contains(p.mesh())){
            var r=map.level().room(p.room());double co=Math.cos(p.yaw()),sn=Math.sin(p.yaw());
            double x=Math.max(r.x0()+.45,Math.min(r.x1()-.45,p.x()+2.8*co-2.7*sn));
            double z=Math.max(r.z0()+.45,Math.min(r.z1()-.45,p.z()+2.8*sn+2.7*co)),y=map.floorY(p.room());
            LevelVisualCapture.capture(scene,map,out,p.mesh(),x,y,z,p.x(),p.y()+Math.min(1.5,p.height()*.65),p.z());
        }
    }
}
