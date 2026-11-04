package org.beckenburg;
import java.nio.file.*;import java.util.*;import javax.imageio.ImageIO;
public class ChurchOpeningCapture {
 public static void main(String[] args)throws Exception{
  HouseMap map=new HouseMap(HouseMap.Site.CHURCH);
  for(int target:new int[]{8,9}){
   var door=map.level().portals().stream().filter(p->p.door()&&p.joins(7,target)).findFirst().orElseThrow();
   map.toggleDoor(door.id(),System.nanoTime()-HouseMap.DOOR_SWING_NANOS);
   SceneRenderer r=new SceneRenderer();r.setRenderSize(960,600);r.lamp=true;
   r.x=door.x()+(target==8?1.4:-1.4);r.z=door.z();r.eyeHeight=door.y()+1.62;r.yaw=target==8?-Math.PI/2:Math.PI/2;r.pitch=-.08;
   if(!map.traversable(r.x,door.y(),r.z))throw new AssertionError("Capture camera outside physical path");
   r.drawPractice(map,List.of(),0);
   Path out=Path.of("development/levels/rebuild/captures/church-upper-openings/door-7-"+target+".png");Files.createDirectories(out.getParent());ImageIO.write(r.image(),"png",out.toFile());
  }
 }
}
