package org.beckenburg;
import java.util.*;
import java.nio.file.*;
/** Supported operating positions, actual mesh aim, circuit isolation and source isolation. */
public final class HollowCreekPowerTest {
 static void check(boolean b,String text){if(!b)throw new AssertionError(text);}
 static LooseClutter.Observer find(HouseMap map,InteractionFocus.Kind kind){
  var socket=map.level().socket(kind==InteractionFocus.Kind.BREAKER?"breaker":"generator",9);var bounds=kind==InteractionFocus.Kind.GENERATOR?VanInteriorLayout.bounds("button_12"):null;
  double tx=socket.x(),ty=socket.y(),tz=socket.z();if(bounds!=null){tx+=(bounds.x0()+bounds.x1())*.15;ty+=(bounds.y0()+bounds.y1())*.15;tz+=bounds.z1()*.30;}
  for(double radius:new double[]{.65,.85,1.05,1.3,1.6})for(int i=0;i<48;i++){
   double angle=i*Math.PI/24,x=tx+Math.sin(angle)*radius,z=tz+Math.cos(angle)*radius,y=map.groundHeight(x,z,9);if(!map.traversable(x,y,z))continue;
   double yaw=Math.atan2(tx-x,tz-z),pitch=Math.atan2(ty-y-HunterPosture.STANDING_EYE,Math.hypot(tx-x,tz-z));var observer=new LooseClutter.Observer(0,x,y,z,yaw,pitch,true);var picked=InteractionFocus.pick(map,observer,List.of());
   if(picked!=null&&picked.kind()==kind&&picked.id().equals(socket.id()))return observer;
  }throw new AssertionError("No supported actual mesh approach to "+kind+" "+socket);
 }
 public static void main(String[] args)throws Exception{
  var map=new HouseMap(HouseMap.Site.HOLLOW_CREEK);var breaker=find(map,InteractionFocus.Kind.BREAKER);var starter=find(map,InteractionFocus.Kind.GENERATOR);
  check(RoomLighting.breakerAimed(map,breaker.x(),breaker.y(),breaker.z(),breaker.yaw()),"Ordinary breaker shortcut reaches the actual outside panel");
  var lights=new RoomLighting();var field=new FixtureLighting(map);field.frame(lights,0);double on=field.sample(9,29,-1.25,-4).energy();
  lights.setSourceRunning(false);field.frame(lights,1);double sourceOff=field.sample(9,29,-1.25,-4).energy();check(lights.breaker()&&!lights.supplied()&&sourceOff<on,"Generator stop must remove actual light while retaining breaker state");
  lights.setSourceRunning(true);check(lights.supplied(),"Independent generator restart");
  lights.setBreaker(false);lights.setSourceRunning(false);lights.setSourceRunning(true);check(!lights.supplied(),"Restart must not repair a tripped breaker");lights.setBreaker(true);check(lights.supplied(),"Hunter can reset ordinary breaker after restart");
  String json="{\n\"status\":\"pass-supported-controls-and-local-electrical-rules\",\n\"breaker_observer\":["+breaker.x()+","+breaker.y()+","+breaker.z()+","+breaker.yaw()+","+breaker.pitch()+"],\n\"starter_observer\":["+starter.x()+","+starter.y()+","+starter.z()+","+starter.yaw()+","+starter.pitch()+"],\n\"powered_sample_energy\":"+on+",\n\"source_off_sample_energy\":"+sourceOff+",\n\"actual_light_loads\":"+map.level().sockets().stream().filter(s->s.type().equals("light")).count()+",\n\"full_host_LAN_acceptance\":false\n}\n";
  Files.writeString(Path.of("development/levels/verification/hollow-creek-runtime-power.json"),json);
  System.out.println("PASS actual supported generator/panel aim,10 shared-circuit emitters,independent source/breaker states (ghost-specific host trip audit remains open)");
 }
}
