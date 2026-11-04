package org.beckenburg;
import java.util.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
public final class CastleResidentAnimationTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  Path out=Path.of("development/quality/evidence/resident-animation-2026-11-03");Files.createDirectories(out);
  var map=new CastleHubMap();var routine=map.residents();
  EnumMap<CastleRelations.Resident,EnumSet<HunterSpriteAnimation.Pose>> seen=new EnumMap<>(CastleRelations.Resident.class);
  for(var r:CastleRelations.Resident.values())seen.put(r,EnumSet.noneOf(HunterSpriteAnimation.Pose.class));
  for(int tick=0;tick<660;tick++){
   routine.update(map,tick*.05,-100,-100);
   for(var r:CastleRelations.Resident.values()){
    var body=routine.body(r);seen.get(r).add(body.pose());
    routine.update(map,tick*.05,-100,-100);check(routine.body(r).equals(body),"same-clock draw preserves pose");
    check(Math.hypot(body.x()-r.x,body.z()-r.z)<=.751,"resident stays near service "+r);
    if(Math.hypot(body.x()-r.x,body.z()-r.z)>.001)check(map.residentPlacementClear(body.x(),body.z()),"walking resident is supported and clear "+r);
   }
  }
  for(var r:CastleRelations.Resident.values()){
   System.out.println(r+" "+seen.get(r));
   check(seen.get(r).containsAll(EnumSet.allOf(HunterSpriteAnimation.Pose.class)),"actual routine covers idle, both walk strides and crouch "+r);
   var old=routine.body(r);routine.update(map,33,old.x()+1,old.z());var paused=routine.body(r);
   check(paused.pose()==HunterSpriteAnimation.Pose.IDLE,"approaching player pauses resident "+r);
   check(paused.x()==old.x()&&paused.z()==old.z(),"pause holds world position "+r);
   var preview=new BufferedImage(512,224,BufferedImage.TYPE_INT_RGB);var g=preview.createGraphics();g.setColor(new java.awt.Color(35,39,46));g.fillRect(0,0,512,224);
   int col=0;
   for(var pose:HunterSpriteAnimation.Pose.values()){
    for(int facing=0;facing<8;facing++)check(CastleResidentAnimation.frame(r,facing,pose)!=null,"all directional poses present");
    var image=CastleResidentAnimation.frame(r,0,pose);g.drawImage(image,col++*128,0,null);
    var body=new CastleResidentAnimation.Body(0,4,180,pose);int[] pixels=new int[SceneRenderer.W*SceneRenderer.H];double[] depth=new double[pixels.length];Arrays.fill(depth,Double.POSITIVE_INFINITY);
    DirectionalSprites.drawResident(pixels,depth,0,0,0,0,r,body,0,true,SceneRenderer.W,SceneRenderer.H,153);
    check(Arrays.stream(pixels).anyMatch(p->p!=0),"resident pose renders "+r+pose);
    Arrays.fill(pixels,0);Arrays.fill(depth,1);
    DirectionalSprites.drawResident(pixels,depth,0,0,0,0,r,body,0,true,SceneRenderer.W,SceneRenderer.H,153);
    check(Arrays.stream(pixels).allMatch(p->p==0),"walls occlude resident pose "+r+pose);
   }
   g.dispose();ImageIO.write(preview,"PNG",out.resolve(r.name()+"-poses.png").toFile());
  }
  routine.update(map,0,-100,-100);for(var r:CastleRelations.Resident.values())check(routine.body(r).x()==r.x&&routine.body(r).pose()==HunterSpriteAnimation.Pose.IDLE,"clock rewind resets routine");
  System.out.println("PASS five resident routines, collision, player pause, reset, 160 directional poses and depth raster");
 }
}
