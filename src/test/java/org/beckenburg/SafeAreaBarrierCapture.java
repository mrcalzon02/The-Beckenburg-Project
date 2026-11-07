package org.beckenburg;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Actual game-rendered fog, with a second set of frames proving animation. */
public final class SafeAreaBarrierCapture {
    public static void main(String[] args)throws Exception{
        LevelVisualCapture.inspection();Path out=Path.of(args[0]);Files.createDirectories(out);
        boolean rebuilt=args.length>1&&args[1].equals("--rebuilt-house");
        var map=rebuilt?HouseRebuildFunctions.map():new HouseMap();var van=map.van();var renderer=new SceneRenderer();
        renderer.setRenderSize(640,400);renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.environmentLight=1;renderer.roomLights=new RoomLighting();
        for(String shot:rebuilt?List.of("van-exterior","van-inside"):List.of("van-exterior","van-inside","van-ward","castle-sanctuary","castle-courtyard")){
            boolean castle=shot.startsWith("castle-");var placed=castle?CastleSurroundings.courtyardVan():van;
            var p=placed.world(shot.equals("van-inside")?0:4.5,shot.equals("van-inside")?-3.2:-5.5);
            var target=placed.world(0,.8);double targetY=placed.placement().y()+1.5;
            List<SceneRenderer.WorldMark> marks=List.of();
            if(shot.equals("van-inside")){p=placed.world(0,-1.6);target=placed.world(0,-7);}
            renderer.eyeHeight=placed.placement().y()+(shot.equals("van-inside")?1.65:2.6);
            if(shot.equals("van-ward")){
                var origin=map.spawn(1);boolean found=false;var room=map.level().room(1);
                for(double x=room.x0()+.5;x<room.x1()-.5&&!found;x+=.3)for(double z=room.z0()+.5;z<room.z1()-.5&&!found;z+=.3){
                    double d=Math.hypot(x-origin.x(),z-origin.z());
                    if(d<2.7||d>4||!map.traversable(x,origin.y(),z)||!map.clearLineOfSight(x,origin.y()+1.6,z,origin.x(),origin.y()+1,origin.z()))continue;
                    p=new VanStaging.Position(x,z);target=new VanStaging.Position(origin.x(),origin.z());found=true;
                }
                if(!found)throw new AssertionError("clear in-room ward inspection camera");
                renderer.eyeHeight=origin.y()+1.65;targetY=origin.y()+1;
                map.setTemporarySafeAreas(List.of(SafeAreaBarrier.ward(origin.x(),origin.y(),origin.z())));
                marks=List.of(new SceneRenderer.WorldMark(origin.x(),origin.z(),origin.y()+.045,HunterCounters.Kind.WARD));
            }else map.setTemporarySafeAreas(List.of());
            renderer.x=p.x();renderer.z=p.z();renderer.yaw=Math.atan2(target.x()-p.x(),target.z()-p.z());
            renderer.pitch=Math.atan2(targetY-renderer.eyeHeight,Math.hypot(target.x()-p.x(),target.z()-p.z()));
            if(castle){renderer.x=16;renderer.z=22.8;renderer.eyeHeight=1.7;renderer.yaw=Math.atan2(13.3-16,27.7-22.8);renderer.pitch=.03;}
            if(shot.equals("castle-sanctuary")){
                var shield=SafeAreaBarrier.castle();renderer.x=shield.x()+shield.radius()*.8+8;renderer.z=shield.z()+shield.radius()*.8+8;
                renderer.eyeHeight=30;renderer.yaw=Math.atan2(shield.x()-renderer.x,shield.z()-renderer.z);
                renderer.pitch=Math.atan2(6-renderer.eyeHeight,Math.hypot(shield.x()-renderer.x,shield.z()-renderer.z));
                renderer.fogDensity=0;
            }
            int count=castle||shot.equals("van-inside")?1:12;
            for(int frame=0;frame<count;frame++){
                double clock=frame*.18;
                if(castle)renderer.drawCastle(new CastleHubMap(),clock);
                else renderer.drawNetwork(map,List.of(),(SceneRenderer.Apparition)null,clock,marks);
                ImageIO.write(renderer.image(),"PNG",out.resolve(shot+String.format("-%02d",frame)+".png").toFile());
                if(renderer.safeAreaPixels()==0&&!shot.equals("castle-courtyard"))throw new AssertionError("safe-area haze not visible "+shot);
            }
            System.out.println("CAPTURE "+shot+" "+count+" authentic frames");
        }
    }
}
