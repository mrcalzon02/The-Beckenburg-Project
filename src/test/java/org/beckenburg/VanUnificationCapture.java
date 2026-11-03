package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Matched production software-renderer inspection; no user preferences are saved. */
public final class VanUnificationCapture {
    public static void main(String[] args)throws Exception{
        LevelVisualCapture.inspection();Path out=Path.of(args[0]);Files.createDirectories(out);
        for(String site:List.of("castle","house","hollow_creek")){
            if(args.length>1&&!site.equals(args[1]))continue;
            HouseMap map=site.equals("castle")?null:new HouseMap(HouseMap.Site.valueOf(site.toUpperCase(Locale.ROOT)));
            var van=map==null?CastleSurroundings.courtyardVan():map.van();
            for(String shot:List.of("right-wheel-well","left-stocked-rack","exterior","rear-open","rear-closed","rear-frame","wheel-exterior-close")){
                var renderer=new SceneRenderer();renderer.setRenderSize(640,400);
                renderer.setPsxPreset(PsxPipeline.Preset.OFF);renderer.environmentLight=1;
                renderer.roomLights=new RoomLighting();renderer.lamp=true;
                renderer.rackStock=java.util.EnumSet.allOf(EquipmentLoadout.Item.class);
                if(map!=null)renderer.rackInventory=VanRackStockTest.fullStock(EquipmentTiers.Tier.III);
                boolean exterior=shot.equals("exterior"),left=shot.startsWith("left");
                var p=van.world(exterior?(map==null?2.5:4.5):left?.25:-.25,exterior?(map==null?-4.2:-5): .65);
                var target=van.world(exterior?0:left?-.84:.84,exterior?0:-.42);
                renderer.x=p.x();renderer.z=p.z();renderer.eyeHeight=van.placement().y()+(exterior?1.72:1.95);
                renderer.yaw=Math.atan2(target.x()-p.x(),target.z()-p.z());
                renderer.pitch=Math.atan2(van.placement().y()+(exterior?1.3:.78)-renderer.eyeHeight,
                    Math.hypot(target.x()-p.x(),target.z()-p.z()));
                if(shot.startsWith("rear-")){
                    p=van.world(0,shot.equals("rear-frame")?-3.65:-5.5);target=van.world(0,0);
                    renderer.x=p.x();renderer.z=p.z();renderer.eyeHeight=van.placement().y()+1.65;
                    renderer.yaw=Math.atan2(target.x()-p.x(),target.z()-p.z());renderer.pitch=shot.equals("rear-frame")?.12:0;
                }else if(shot.equals("wheel-exterior-close")){
                    p=van.world(2.8,-.55);target=van.world(0,-.55);
                    renderer.x=p.x();renderer.z=p.z();renderer.eyeHeight=van.placement().y()+.70;
                    renderer.yaw=Math.atan2(target.x()-p.x(),target.z()-p.z());renderer.pitch=0;
                }
                van.setCargoDoorClosed(shot.equals("rear-closed"));
                if(map==null)renderer.drawCastle(new CastleHubMap(),0);
                else renderer.drawNetwork(map,List.of(),-1,0);
                ImageIO.write(renderer.image(),"PNG",out.resolve(site+"-"+shot+".png").toFile());
                System.out.println("CAPTURE "+site+" "+shot);
            }
            van.setCargoDoorClosed(false);
        }
    }
}
