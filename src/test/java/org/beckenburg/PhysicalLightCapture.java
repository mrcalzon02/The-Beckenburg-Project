package org.beckenburg;
import java.util.*;import java.nio.file.*;import javax.imageio.ImageIO;
public final class PhysicalLightCapture {
 static Path out=Path.of("development/levels/previews/physical-lighting");
 static void view(SceneRenderer s,HouseMap map,String id,double x,double y,double z,double tx,double ty,double tz)throws Exception{s.x=x;s.eyeHeight=y;s.z=z;s.yaw=Math.atan2(tx-x,tz-z);s.pitch=Math.atan2(ty-y,Math.hypot(tx-x,tz-z));s.drawNetwork(map,List.of(),-1,0);ImageIO.write(s.image(),"PNG",out.resolve(id+".png").toFile());System.out.println("VIEW "+id);}
 public static void main(String[]args)throws Exception{
  Files.createDirectories(out);LevelVisualCapture.inspection();var s=new SceneRenderer();s.setRenderSize(960,600);s.setPsxPreset(PsxPipeline.Preset.OFF);s.environmentLight=1;s.fogDensity=.12;s.lamp=false;s.roomLights=new RoomLighting();s.roomLights.acceptSnapshot("000000000000");
  var house=new HouseMap();var lamp=house.fixtures().states().stream().filter(f->f.kind()==HauntedFixtures.Kind.TABLE_LAMP&&f.room()==6).findFirst().orElseThrow();
  var o=HauntedFixturesTest.operator(house,lamp);
  for(double scale:new double[]{2.7,2.1}){double px=lamp.x()+(o.x()-lamp.x())*scale,pz=lamp.z()+(o.z()-lamp.z())*scale;if(!house.traversable(px,o.y(),pz))continue;var candidate=InteractionFocusTest.aim(px,o.y(),pz,lamp.x(),lamp.y()+.22,lamp.z());var target=InteractionFocus.pick(house,candidate,List.of());if(target!=null&&target.kind()==InteractionFocus.Kind.FIXTURE&&target.id().equals(""+lamp.id())){o=candidate;break;}}
  view(s,house,"house-01-bedside-warm-cone",o.x(),o.y()+1.62,o.z(),lamp.x(),lamp.y()+.28,lamp.z());
  house.fixtures().toggleFocused(lamp.id(),o,1);
  view(s,house,"house-02-bedside-switched-off",o.x(),o.y()+1.62,o.z(),lamp.x(),lamp.y()+.28,lamp.z());
  view(s,house,"house-03-candle-chandelier",19.2,1.62,14.0,21,2,12.2);
  var tv=house.fixtures().states().stream().filter(f->f.kind()==HauntedFixtures.Kind.TELEVISION).findFirst().orElseThrow();house.fixtures().activate(tv.id(),tv.room(),1);double dx=-Math.sin(tv.yaw()),dz=Math.cos(tv.yaw());
  view(s,house,"house-04-tv-static-glow",tv.x()+dx*1.55,1.62,tv.z()+dz*1.55,tv.x(),tv.y()+.27,tv.z());
  house.fixtures().toggleFocused(tv.id(),HauntedFixturesTest.operator(house,tv),2);
  view(s,house,"house-05-tv-switched-off",tv.x()+dx*1.55,1.62,tv.z()+dz*1.55,tv.x(),tv.y()+.27,tv.z());
  var farm=new HouseMap(HouseMap.Site.FARMHOUSE);s.roomLights=new RoomLighting();s.roomLights.acceptSnapshot("000000000000");s.roomLights.toggle(11);
  view(s,farm,"farm-01-bare-bulb-laundry",33.9,1.62,29.0,35,2.1,27.5);
  var school=new HouseMap(HouseMap.Site.SCHOOL);s.roomLights=new RoomLighting();
  view(s,school,"school-01-washroom-fluorescents",26,5.22,14.65,26.1,4.6,13.4);
  view(s,school,"school-02-boiler-fluorescents",29,-1.38,29,22,-1.5,23);
  var castle=new CastleHubMap();s.x=15.5;s.z=27;s.eyeHeight=2.4;s.yaw=0;s.pitch=.12;s.drawCastle(castle,2);ImageIO.write(s.image(),"PNG",out.resolve("castle-01-mountain-courtyard.png").toFile());
  s.x=52;s.z=68;s.eyeHeight=18;s.yaw=Math.atan2(22-s.x,17-s.z);s.pitch=-.12;s.drawCastle(castle,2);ImageIO.write(s.image(),"PNG",out.resolve("castle-02-surroundings-overview.png").toFile());
 }
}
