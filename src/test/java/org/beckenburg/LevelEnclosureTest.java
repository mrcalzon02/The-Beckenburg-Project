package org.beckenburg;
import java.util.*;
/** Physical perimeter closure, fully supported arrival module and visible readable objects. */
public final class LevelEnclosureTest {
 static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 public static void main(String[]args)throws Exception{
  for(var site:AuthoredTestSupport.SITES){
   var map=new HouseMap(site);var edges=LevelEnclosure.edges(map.level().rooms());var barriers=map.level().sockets().stream().filter(s->s.type().equals("enclosure")).toList();check(edges.size()==barriers.size()&&edges.size()>=4,"incomplete visible perimeter");
   for(int i=0;i<edges.size();i++){
    var e=edges.get(i);var s=barriers.get(i);check(map.level().colliders().stream().anyMatch(c->Math.hypot(c.x()-s.x(),c.z()-s.z())<.001&&Math.abs(c.halfX()-e.length()/2)<.001),"visible barrier lacks exact collision");
    for(double t=.08;t<1;t+=.14){double x=e.x0()+(e.x1()-e.x0())*t+e.inwardX()*.18,z=e.z0()+(e.z1()-e.z0())*t+e.inwardZ()*.18;check(!map.traversable(x,0,z),"walked through boundary "+site);}
    check(!map.clearLineOfSight(s.x()-e.inwardX()*.4,.7,s.z()-e.inwardZ()*.4,s.x()+e.inwardX()*.4,.7,s.z()+e.inwardZ()*.4),"barrier does not obstruct at its physical height");
   }
   for(double z=VanArrivalStreet.ROAD_END+.4;z<VanStaging.FRONT-VanStaging.RAMP_LENGTH-.2;z+=.18)for(double x:new double[]{-.75,0,.75}){var p=map.van().world(x,z);check(map.traversable(p.x(),0,p.z()),"shared road has unsupported/blocked lane "+site+" "+x+","+z);}
   // Reading an indoor document is allowed after opening its real doors.
   // Keep the original closed-door perimeter/road checks above unchanged.
   for(var p:map.level().portals())if(p.door())map.openDoorBetweenRooms(p.a(),p.b());
   int papers=0;for(var s:map.level().sockets())if(s.type().equals("story")){
    String id="STORY_"+s.style();
    double y=s.style().equals("HISTORY")?s.y():s.y()+.02;
    boolean readable=InteractionFocusTest.find(map,s.x(),y,s.z(),
        InteractionFocus.Kind.INTERFACE,id,List.of())!=null;
    // Older room records are narrow meshes mounted on a real, rotated prop.
    // A 24-direction coarse probe can miss their usable face. Exercise actual
    // physical standing positions and the same exact centre ray as live play,
    // following the independent legacy-story acceptance (96 directions).
    if(!readable&&s.style().startsWith("ROOM_")){
     var prop=map.level().props().stream().filter(p->p.room()==s.room()
         &&Math.hypot(p.x()-s.x(),p.z()-s.z())<.001
         &&p.mesh().startsWith("level_trace_")).findFirst().orElseThrow();
     double floor=map.floorY(s.room());
     for(double radius:new double[]{.7,1,1.3,1.6}){
      for(int a=0;a<96&&!readable;a++){
       double angle=a*Math.PI/48,x=s.x()+radius*Math.cos(angle),
           z=s.z()+radius*Math.sin(angle);
       if(!map.traversable(x,floor,z))continue;
       double tx=s.x()+.09*Math.cos(prop.yaw()),
           tz=s.z()+.09*Math.sin(prop.yaw());
       var observer=new LooseClutter.Observer(0,x,floor,z,
           Math.atan2(tx-x,tz-z),
           Math.atan2(s.y()+.015-floor-1.62,radius),true);
       var focus=InteractionFocus.pick(map,observer,List.of());
       readable=focus!=null&&focus.kind()==InteractionFocus.Kind.INTERFACE
           &&focus.id().equals(id);
      }
      if(readable)break;
     }
    }
    check(readable,"readable object has no exact visible approach "+site+" "+s.style());
    papers++;
   }
   check(papers>=2,"location has no authored identity and correspondence");System.out.println("PASS "+site+" physical perimeter "+barriers.size()+", full reusable road access, "+papers+" exact-ray readables");
  }
  var mountain=LevelEnclosure.template(LevelEnclosure.Setting.MOUNTAIN);check(LevelEnclosure.setting(mountain.id())==LevelEnclosure.Setting.MOUNTAIN,"mountain castle template lost its setting");
  System.out.println("PASS three live enclosure settings and reusable mountain variant; castle remains outside the playable-site catalogue");
 }
}
