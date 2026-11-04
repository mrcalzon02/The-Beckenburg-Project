package org.beckenburg;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
/** Captures the live Castle renderer at genuine resident routine states. */
public final class CastleResidentMotionReview {
 public static void main(String[] args)throws Exception{
  Path out=Path.of(args.length>0?args[0]:"development/levels/previews/resident-animation-2026-11-04/runtime");Files.createDirectories(out);
  int captures=0;
  for(var resident:CastleRelations.Resident.values()){
   var map=new CastleHubMap();var scene=new SceneRenderer();scene.setRenderSize(854,480);scene.setPsxPreset(PsxPipeline.Preset.OFF);scene.eyeHeight=1.62;scene.pitch=-.08;
   // Pick a visible room-side camera far enough away to let the work routine run.
   boolean found=false;
   for(int i=0;i<16;i++){
    double a=i*Math.PI/8,cx=resident.x+Math.sin(a)*4.4,cz=resident.z+Math.cos(a)*4.4;
    var room=CastleRooms.at(cx,0,cz);var home=CastleRooms.at(resident.x,0,resident.z);
    if(map.traversable(cx,cz)&&room!=null&&home!=null&&room.id().equals(home.id())){scene.x=cx;scene.z=cz;found=true;break;}
   }
   if(!found)throw new AssertionError("No routine review camera for "+resident);
   var seen=EnumSet.noneOf(HunterSpriteAnimation.Pose.class);
   for(int tick=0;tick<900&&seen.size()<4;tick++){
    double clock=tick*.05;map.residents().update(map,clock,-100,-100);var body=map.residents().body(resident);
    if(seen.contains(body.pose())||Math.hypot(body.x()-scene.x,body.z()-scene.z)<3.4)continue;
    scene.yaw=Math.atan2(body.x()-scene.x,body.z()-scene.z);scene.drawCastle(map,clock,true);
    var drawn=map.residents().body(resident);if(drawn.pose()!=body.pose())continue;
    ImageIO.write(scene.image(),"PNG",out.resolve(resident.name().toLowerCase(Locale.ROOT)+"-"+drawn.pose().name().toLowerCase(Locale.ROOT)+"-runtime.png").toFile());seen.add(drawn.pose());captures++;
   }
   if(seen.size()!=4)throw new AssertionError("Missing live routine captures "+resident+seen);
  }
  System.out.println("PASS live Castle routine review: "+captures+" actual full-scene captures, all five residents idle, two walk strides and crouch");
 }
}
