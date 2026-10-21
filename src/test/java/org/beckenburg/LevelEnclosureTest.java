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
   int papers=0;for(var s:map.level().sockets())if(s.type().equals("story")){
    double y=s.style().equals("HISTORY")?s.y():s.y()+.02;
    var observer=InteractionFocusTest.find(map,s.x(),y,s.z(),InteractionFocus.Kind.INTERFACE,"STORY_"+s.style(),List.of());check(observer!=null,"readable object has no exact visible approach "+site+" "+s.style());papers++;
   }
   check(papers>=2,"location has no authored identity and correspondence");System.out.println("PASS "+site+" physical perimeter "+barriers.size()+", full reusable road access, "+papers+" exact-ray readables");
  }
  var mountain=LevelEnclosure.template(LevelEnclosure.Setting.MOUNTAIN);check(LevelEnclosure.setting(mountain.id())==LevelEnclosure.Setting.MOUNTAIN,"mountain castle template lost its setting");
  System.out.println("PASS three live enclosure settings and reusable mountain variant; castle remains outside the playable-site catalogue");
 }
}
