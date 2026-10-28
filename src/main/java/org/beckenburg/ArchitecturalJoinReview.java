package org.beckenburg;
import java.util.*;
/** Explicit synthetic-source runtime diagnostic on actual Blackpine geometry. */
public final class ArchitecturalJoinReview {
    private static HouseMap withSource(double sourceX,boolean enabled){
        var base=BlackpineLevel.map();var l=base.level();var sockets=new ArrayList<LevelDefinition.Socket>();
        for(var s:l.sockets())if(!s.type().equals("light"))sockets.add(s);
        if(enabled)sockets.add(new LevelDefinition.Socket("join-diagnostic","light",0,sourceX,base.floorY(0)+1.865,23,0,"bare-bulb"));
        return new HouseMap(base.site(),new LevelDefinition(l.id(),l.revision()+"-diagnostic-"+sourceX+enabled,l.title(),l.rooms(),l.portals(),l.stairs(),l.voids(),l.props(),sockets,l.colliders(),l.dock(),l.ambience()));
    }
    public static void execute()throws Exception{
        for(String mode:List.of("exterior-source","source-off","same-side-control")){
            var map=withSource(mode.equals("same-side-control")?41:39,!mode.equals("source-off"));
            for(String view:List.of("wall-face","floor-join","grazing-join")){
                var renderer=new SceneRenderer();renderer.setRenderSize(480,300);renderer.roomLights=new RoomLighting();
                var pose=view.equals("grazing-join")
                    ?new LevelOpeningFlyover.Pose(40.2,map.floorY(0)+1.3,21.3,Math.atan2(-.12,1.7),-.2)
                    :new LevelOpeningFlyover.Pose(41.2,map.floorY(0)+1.3,23,-Math.PI/2,view.equals("floor-join")?-.45:0);
                MapCompatibilityReview.capture(mode,new MapCompatibilityReview.Shot(view,pose,true,false),renderer,map,null);
            }
        }
    }
}
