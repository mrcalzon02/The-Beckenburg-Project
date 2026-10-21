package org.beckenburg;
import java.nio.file.*;import java.util.*;import javax.imageio.ImageIO;
/** Actual camera review of lived-in plumbing and supported household details. */
public final class LivedLocationCapture {
 static Path OUT=Path.of("development/levels/previews/lived-location");
 static void view(SceneRenderer s,HouseMap map,String name,double x,double y,double z,double tx,double ty,double tz)throws Exception{
  s.x=x;s.eyeHeight=y;s.z=z;s.yaw=Math.atan2(tx-x,tz-z);s.pitch=Math.atan2(ty-y,Math.hypot(tx-x,tz-z));s.drawNetwork(map,List.of(),-1,0);ImageIO.write(s.image(),"PNG",OUT.resolve(name+".png").toFile());System.out.println("VIEW "+name);
 }
 public static void main(String[]args)throws Exception{
  Files.createDirectories(OUT);LevelVisualCapture.inspection();var map=new HouseMap();var s=new SceneRenderer();s.setRenderSize(960,600);s.setPsxPreset(PsxPipeline.Preset.OFF);s.roomLights=new RoomLighting();s.lamp=true;s.environmentLight=1;
  view(s,map,"house-01-complete-bathroom",20.5,4.62,13.9,21.8,3.9,11.6);
  view(s,map,"house-02-basin-and-toiletries",21,4.62,12.5,23.6,4.3,11.5);
  view(s,map,"house-03-bath-and-wall-supported-shower",20.8,4.62,12.2,22.9,4.1,14.4);
  view(s,map,"house-04-laundry-utility-room",21,-1.38,13.3,23.5,-2.05,13.7);
  view(s,map,"house-05-toilet-and-hamper",21,4.62,12.8,19.1,3.65,11.4);
  map=new HouseMap(HouseMap.Site.FARMHOUSE);
  for(var p:map.level().portals())if(p.door()&&p.a()==p.b())map.toggleDoor(p.id());
  view(s,map,"farm-01-private-washroom",23.4,4.62,14.7,24.5,3.8,12.7);
  view(s,map,"farm-02-shower-and-hamper",24.1,4.62,13.3,25.2,4.0,14.9);
  view(s,map,"farm-03-shed-laundry",34.6,1.62,26.8,33.6,.8,24.5);
  view(s,map,"farm-04-yard-drying-line",8.5,1.62,10.4,8.5,1.0,7.5);
  map=new HouseMap(HouseMap.Site.SCHOOL);
  for(var p:map.level().portals())if(p.door()&&p.a()==p.b())map.toggleDoor(p.id());
  view(s,map,"school-01-boys-handwash-and-urinal",26.0,5.22,14.65,26.1,4.5,13.4);
  view(s,map,"school-02-boys-private-cubicles",26.6,5.22,17.175,27.42,4.2,17.175);
  view(s,map,"school-03-girls-handwash",26.0,5.22,22.1,26.1,4.5,20.9);
  view(s,map,"school-04-upper-corridor-clearance",22.4,5.22,18.2,24.8,4.7,22);
 }
}
