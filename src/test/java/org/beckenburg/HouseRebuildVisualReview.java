package org.beckenburg;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
/** Actual complete-room diagnostic using the established supported-camera audit. */
public final class HouseRebuildVisualReview {
    public static void main(String[] args)throws Exception{
        if(args.length<1||args.length>2||(args.length==2&&!args[1].equals("--supplement-only")))throw new IllegalArgumentException("Usage: HouseRebuildVisualReview <capture directory> [--supplement-only]");
        var audit=SiteRoomCompassAudit.class.getDeclaredMethod("auditMap",HouseMap.class,String.class,Path.class);audit.setAccessible(true);
        var map=HouseRebuildServices.map();if(args.length==1)audit.invoke(null,map,"HOUSE_REBUILD_SERVICES",Path.of(args[0]));
        for(var portal:map.level().portals())if(portal.door()&&!map.doorOpen(portal.id())){map.toggleDoor(portal.id());map.doorOpenFraction(portal.id(),System.nanoTime()+HouseMap.DOOR_SWING_NANOS);}
        var scene=new SceneRenderer();scene.setRenderSize(640,400);scene.setPsxPreset(PsxPipeline.Preset.CLASSIC);scene.roomLights=new RoomLighting();scene.environmentLight=1;scene.lamp=true;
        var csv=new StringBuilder("room,x,y,z,reason\n");String[] facing={"north","east","south","west"};double[] headings={Math.PI,Math.PI/2,0,-Math.PI/2};
        for(int room:new int[]{5,12}){
            var p=map.level().room(room).spawn();if(!map.traversable(p.x(),p.y(),p.z())||map.roomAt(p.x(),p.y(),p.z())!=room)throw new AssertionError("Invalid authored review anchor "+room);
            var out=Path.of(args[0],"house_rebuild_services",String.format(Locale.ROOT,"author-spawn-room-%02d",room));Files.createDirectories(out);
            for(int i=0;i<4;i++){scene.x=p.x();scene.z=p.z();scene.eyeHeight=p.y()+1.62;scene.yaw=headings[i];scene.pitch=-.07;scene.drawNetwork(map,List.of(),-1,12);ImageIO.write(scene.image(),"png",out.resolve(facing[i]+".png").toFile());}
            csv.append(room).append(',').append(p.x()).append(',').append(p.y()).append(',').append(p.z()).append(",authored landing/garden anchor supplements nearest-centre fallback\n");
        }
        Files.writeString(Path.of(args[0],"house_rebuild_services","supplemental-cameras.csv"),csv);
        System.out.println("HOUSE_ROOM_REVIEW_CAPTURED: inspect every room and record actual findings; captures are not automatic acceptance");
    }
}
