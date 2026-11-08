package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.imageio.ImageIO;

/** Exact imported switch GLB, against each actual authored supporting wall.
 * Includes one real in-game frame for visual inspection in Actions artifacts. */
public final class CastleSwitchMountTest {
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception {
  var edits=CastleLevelEdits.current();
  var bounds=SceneRenderer.castleBounds("light_switch_1");
  check(bounds[3]>0&&bounds[4]>0&&bounds[5]>0,"Source switch model must have finite volume");
  var available=new ArrayList<CastleHubDecor.Switch>();
  for(var sw:CastleHubDecor.SWITCHES)if(CastleHubDecor.switchAvailable(sw)){
   var light=CastleSwitchMount.fit(sw,edits.walls());
   check(light!=null,"Castle switch has no authored backing wall: "+sw);
   var pose=CastleWallLampMount.resolve(light,edits.walls());
   check(pose.supported(),"Fitted source switch intersects no masonry face: "+sw);
   check(bounds[3]*light.scale()<=CastleSwitchMount.MAX_WIDTH+1e-8
      &&bounds[4]*light.scale()<=CastleSwitchMount.MAX_HEIGHT+1e-8
      &&bounds[5]*light.scale()<=CastleSwitchMount.MAX_DEPTH+1e-8,
      "Scaled imported switch is larger than a physical wall switch: "+sw);
   check(light.scale()>0&&light.scale()<=.85,"Switch uses uniform bounded source scale");
   // Supporting stone must be in the facing direction, even for manually
   // authored switches whose old yaw pointed away from their actual wall.
   double dx=pose.wallX()-sw.x(),dz=pose.wallZ()-sw.z(),d=Math.hypot(dx,dz);
   check(d>.06 && (dx*Math.sin(light.yaw())+dz*Math.cos(light.yaw()))/d>.98,
       "Switch not oriented toward supporting stone: "+sw);
   available.add(sw);
  }
  check(available.size()>=12,"Too few verified authored Castle light switches");
  var sw=available.get(0);
  var fixture=CastleSwitchMount.fit(sw,edits.walls());
  double faceX=Math.sin(fixture.yaw()),faceZ=Math.cos(fixture.yaw());
  var renderer=new SceneRenderer();
  renderer.setRenderSize(640,400);
  renderer.x=sw.x()-faceX*1.18;renderer.z=sw.z()-faceZ*1.18;
  renderer.eyeHeight=sw.y()+.42;renderer.yaw=fixture.yaw();
  renderer.pitch=Math.atan2(sw.y()-renderer.eyeHeight,1.18);
  renderer.lamp=false;
  renderer.drawCastle(new CastleHubMap(),.4,true);
  Path dest=Path.of("build","e2e","castle-repairs","castle-switch-source-fitted.png");
  Files.createDirectories(dest.getParent());
  check(ImageIO.write(renderer.image(),"png",dest.toFile()),"Renderer refused switch capture");
  check(Files.size(dest)>2500,"Empty Castle switch renderer capture");
  System.out.println("PASS "+available.size()+" Castle switches: bounded original GLB geometry, wall-oriented mounts, no stretched generated backing; real frame "+dest);
 }
}
