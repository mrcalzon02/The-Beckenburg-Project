package org.beckenburg;
import java.util.*;
/** Actual GPU evidence for the focused van and fitted-door repair. */
public final class VanCastleRepairReview {
    public static void execute()throws Exception{
        for(var site:List.of(HouseMap.Site.HOUSE,HouseMap.Site.HOLLOW_CREEK)){
            var map=new HouseMap(site);var van=map.van();van.setCargoDoorClosed(true);
            for(String view:List.of("workstation","equipment","roof")){
                var renderer=new SceneRenderer();renderer.setRenderSize(480,300);renderer.roomLights=new RoomLighting();
                var p=van.world(0,view.equals("workstation")?-1.55:0);var target=van.world(view.equals("equipment")?-.8:0,view.equals("workstation")?1.8:1);
                var pose=new LevelOpeningFlyover.Pose(p.x(),van.floorY()+1.62,p.z(),Math.atan2(target.x()-p.x(),target.z()-p.z()),view.equals("roof")?.55:-.12);
                MapCompatibilityReview.capture("van-"+map.level().id(),new MapCompatibilityReview.Shot(view,pose,false,false),renderer,map,null);
            }
        }
        if(Boolean.getBoolean("beckenburg.review.vanOnly"))return;
        var edits=CastleLevelEdits.current();var saved=edits.snapshot();try{
            var slot=CastleDoorSlots.all().stream().filter(s->!s.gate()&&s.bottom()==0).findFirst().orElseThrow();if(edits.doorType(slot.id())==CastleDoorSlots.Type.EMPTY)edits.installDoor(slot.id(),CastleDoorSlots.Type.WOOD);
            var castle=new CastleHubMap();var renderer=new SceneRenderer();renderer.setRenderSize(480,300);renderer.roomLights=new RoomLighting();renderer.setLampHardware(EquipmentTiers.Tier.II,false);
            double x=slot.centerX()-(slot.horizontal()?0:2.8),z=slot.centerZ()-(slot.horizontal()?2.8:0),yaw=slot.horizontal()?0:Math.PI/2;
            var pose=new LevelOpeningFlyover.Pose(x,slot.bottom()+1.62,z,yaw,-.04);
            MapCompatibilityReview.capture("castle-door",new MapCompatibilityReview.Shot("closed",pose,true,true),renderer,null,castle);
            castle.toggleDoor(slot.id());MapCompatibilityReview.capture("castle-door",new MapCompatibilityReview.Shot("open",pose,true,true),renderer,null,castle);
        }finally{edits.restore(saved);}
    }
}
