package org.beckenburg;
import java.nio.file.*;import java.util.*;import javax.imageio.ImageIO;
/** Actual runtime views of surroundings and narrative props; elevated overviews are marked as such. */
public final class EnclosureVisualCapture {
 static Path out=Path.of("development/levels/previews/enclosures");
 static void view(SceneRenderer s,HouseMap map,String name,double x,double y,double z,double tx,double ty,double tz)throws Exception{s.x=x;s.eyeHeight=y;s.z=z;s.yaw=Math.atan2(tx-x,tz-z);s.pitch=Math.atan2(ty-y,Math.hypot(tx-x,tz-z));s.drawNetwork(map,List.of(),-1,0);ImageIO.write(s.image(),"PNG",out.resolve(name+".png").toFile());System.out.println("VIEW "+name);}
 public static void main(String[]args)throws Exception{
  Files.createDirectories(out);LevelVisualCapture.inspection();var s=new SceneRenderer();s.setRenderSize(960,600);s.setPsxPreset(PsxPipeline.Preset.OFF);s.roomLights=new RoomLighting();s.environmentLight=1;s.fogDensity=.12;
  for(var site:AuthoredTestSupport.SITES){var map=new HouseMap(site);var land=map.level().room(12);var entry=map.level().room(13);var d=map.level().dock();String id=map.level().id();
   var road=map.van().world(0,-11);view(s,map,id+"-01-arrival-road",road.x(),1.62,road.z(),d.x(),1.2,d.z());
   view(s,map,id+"-02-yard-boundary",land.x0()+2,1.62,land.z0()+4,land.x0()+.15,1,land.z0()+8);
   view(s,map,id+"-03-elevated-overview",entry.x0()-5,12,entry.z1()+5,(land.x0()+land.x1())/2,2,(land.z0()+entry.z1())/2);
   var plate=map.level().sockets().stream().filter(a->a.type().equals("story")&&a.style().equals("HISTORY")).findFirst().orElseThrow();view(s,map,id+"-04-name-and-address",plate.x(),1.62,plate.z()+2.0,plate.x(),1.93,plate.z());
   for(var label:map.level().sockets())if(label.type().equals("story")&&label.style().equals("LETTERS"))view(s,map,id+"-05-family-papers",label.x()-Math.sin(map.level().props().stream().filter(p->LivedAssets.base(p.mesh()).equals("level_story_letters")).findFirst().orElseThrow().yaw())*1.0,label.y()+1.1,label.z()+Math.cos(map.level().props().stream().filter(p->LivedAssets.base(p.mesh()).equals("level_story_letters")).findFirst().orElseThrow().yaw())*1.0,label.x(),label.y()+.02,label.z());
  }
 }
}
