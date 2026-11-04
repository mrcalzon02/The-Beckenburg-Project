package org.beckenburg;
import java.nio.file.*;
import javax.imageio.ImageIO;
public final class WallLampBracketReview {
 public static void main(String[] args)throws Exception{
  System.setProperty("user.home",Files.createTempDirectory("wall-lamp-review-").toString());
  var map=new CastleHubMap();var r=new SceneRenderer();r.setRenderSize(1000,625);r.lamp=false;
  var folder=Path.of(args[0]);Files.createDirectories(folder);
  double[][] views={{12.4,8.8,-Math.PI/2},{12.1,7.8,Math.atan2(-1.3,1)},{12.1,9.8,Math.atan2(-1.3,-1)},{11.1,8.25,Math.atan2(-.25,.55)},{11.1,9.35,Math.atan2(-.25,-.55)}};
  for(int i=0;i<views.length;i++)for(boolean on:new boolean[]{true,false}){
   var v=views[i];r.x=v[0];r.z=v[1];r.eyeHeight=2.25;r.yaw=v[2];r.pitch=0;
   if(!on)map.toggleCircuit(CastleHubDecor.SWITCHES.get(1));
   r.drawCastle(map,0,true);if(!on)map.toggleCircuit(CastleHubDecor.SWITCHES.get(1));ImageIO.write(r.image(),"png",folder.resolve("wall-lamp-"+i+"-"+(on?"on":"off")+".png").toFile());
  }
 }
}
