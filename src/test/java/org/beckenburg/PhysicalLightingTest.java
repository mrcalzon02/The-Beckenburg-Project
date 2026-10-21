package org.beckenburg;
import java.util.*;
public final class PhysicalLightingTest {
 static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 public static void main(String[]args){
  check(FixtureLighting.angular(FixtureLighting.Kind.FLUORESCENT,1)>.99&&FixtureLighting.angular(FixtureLighting.Kind.FLUORESCENT,0)==0,"fluorescent housing must direct its light downward");
  check(FixtureLighting.angular(FixtureLighting.Kind.BARE_BULB,1)==FixtureLighting.angular(FixtureLighting.Kind.BARE_BULB,-1),"bare bulb must emit omnidirectionally");
  check(FixtureLighting.angular(FixtureLighting.Kind.TABLE_LAMP,.9)>FixtureLighting.angular(FixtureLighting.Kind.TABLE_LAMP,0)*5,"shade does not form a warm downward cone");
  var map=new HouseMap();var lights=new RoomLighting();lights.acceptSnapshot("000000000000");var field=new FixtureLighting(map);field.frame(lights,1);
  var tv=map.fixtures().states().stream().filter(f->f.kind()==HauntedFixtures.Kind.TELEVISION).findFirst().orElseThrow();double dx=-Math.sin(tv.yaw()),dz=Math.cos(tv.yaw()),y=tv.y()+.32*tv.scale();
  double x=tv.x()+dx*1.1,z=tv.z()+dz*1.1;double off=field.sample(tv.room(),x,y,z).energy();map.fixtures().activate(tv.id(),tv.room(),1);field.frame(lights,2);var front=field.sample(tv.room(),x,y,z);var back=field.sample(tv.room(),tv.x()-dx*1.1,y,tv.z()-dz*1.1);
  check(front.energy()>off+.15,"active television casts no real light into its room");check(front.energy()>back.energy()+.1,"television glow leaks behind the cabinet");check(front.b()>=front.r(),"static glow must be white/cool, not a warm lamp");check(field.sources().stream().filter(s->s.kind()==FixtureLighting.Kind.TELEVISION).allMatch(s->s.red()>.85*s.blue()&&s.blue()>=s.red()),"TV emitter colour must stay white/cool even at clipping");
  var observer=InteractionFocusTest.find(map,tv.x(),y,tv.z()+.31*tv.scale(),InteractionFocus.Kind.FIXTURE,""+tv.id(),List.of());check(observer!=null,"actual TV screen cannot be used by E");map.fixtures().toggleFocused(tv.id(),observer,3);field.frame(lights,3);check(field.sample(tv.room(),x,y,z).energy()<front.energy()-.15,"switching TV off leaves its glow active");
  lights.toggle(4);field.frame(lights,4);double shut=field.sample(1,17.5,1.3,20.5).energy();var door=AuthoredTestSupport.door(map,1,4);map.toggleDoor(door.id());field.frame(lights,4);double open=field.sample(1,17.5,1.3,20.5).energy();check(open>shut+.02,"closed doorway does not shadow actual source light, or shadow cache ignores opening");
  for(var site:AuthoredTestSupport.SITES){var m=new HouseMap(site);var engine=new FixtureLighting(m);for(var source:engine.sources()){
   check(source.radius()>0&&source.power()>0,"invalid physical emitter");if(source.kind()==FixtureLighting.Kind.TABLE_LAMP)check(source.red()>source.blue()*2,"bedside lamp should be warm");
  }
   for(var f:m.fixtures().states())if(f.kind()==HauntedFixtures.Kind.LIGHT_SWITCH){var o=InteractionFocusTest.find(m,f.x(),f.y(),f.z(),InteractionFocus.Kind.FIXTURE,""+f.id(),List.of());check(o!=null,"new washroom switch has no real operating point "+site);check(engine.sources().stream().anyMatch(s->s.fixture()==f.id()),"switch does not control physical lights");m.fixtures().toggleFocused(f.id(),o,1);check(!m.fixtures().state(f.id()).active(),"washroom light switch does not operate");}
  }
  var castle=new FixtureLighting(new CastleHubMap());castle.castleFrame(true,1);check(!castle.sources().isEmpty(),"castle physical lamp sources missing");
  System.out.println("PASS native source profiles, directional and warm emission, closed-door shadows/cache invalidation, exact TV glow/sound use, independent washroom switches and castle lamp sources");
 }
}
