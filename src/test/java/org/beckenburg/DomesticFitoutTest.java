package org.beckenburg;
import java.util.*;
/** Plumbing fronts, supports, complete domestic fixtures and real operating points. */
public final class DomesticFitoutTest {
 static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 static long count(HouseMap map,String id){return map.level().props().stream().filter(p->LivedAssets.base(p.mesh()).equals(id)).count();}
 public static void main(String[]args){
  var map=new HouseMap();check(count(map,"toilet_mp_1")==1,"house lost its toilet");
  for(String id:new String[]{"level_bath_basin","level_bathtub","level_bath_shower_rail","level_laundry_washer","level_bathroom_wall_set","level_laundry_shelf"})check(count(map,id)==1,"house missing "+id);
  check(count(map,"level_laundry_hamper")>=2,"bathroom and laundry need their hampers");
  for(var p:map.level().props())if(p.mesh().equals("level_bath_basin")){
   var room=map.level().room(p.room());double dx=(room.x0()+room.x1())/2-p.x(),dz=(room.z0()+room.z1())/2-p.z();check(-Math.sin(p.yaw())*dx+Math.cos(p.yaw())*dz>0,"basin faces the wall");
  }
  for(var p:map.level().props())if(LivedAssets.base(p.mesh()).startsWith("level_laundry_")||LivedAssets.base(p.mesh()).equals("level_bathroom_wall_set")||LivedAssets.base(p.mesh()).equals("level_bath_shower_rail")){
   var b=VanInteriorLayout.bounds(p.mesh());var room=map.level().room(p.room());
   for(double x:new double[]{b.x0(),b.x1()})for(double z:new double[]{b.z0(),b.z1()}){
    double xx=p.x()+p.scale()*(x*Math.cos(p.yaw())-z*Math.sin(p.yaw())),zz=p.z()+p.scale()*(x*Math.sin(p.yaw())+z*Math.cos(p.yaw()));
    check(xx>=room.x0()+.065&&xx<=room.x1()-.065&&zz>=room.z0()+.065&&zz<=room.z1()-.065,"new fitting penetrates supporting room shell "+p.mesh()+" "+xx+","+zz);
   }
  }
  var washer=map.fixtures().states().stream().filter(f->f.kind()==HauntedFixtures.Kind.WASHING_MACHINE).findFirst().orElseThrow();
  var o=InteractionFocusTest.find(map,washer.x(),washer.y(),washer.z(),InteractionFocus.Kind.FIXTURE,""+washer.id(),List.of());check(o!=null,"washer cannot be used through exact E interaction");
  map.fixtures().toggleFocused(washer.id(),o,1);check(map.fixtures().state(washer.id()).active(),"washer cannot be started");map.fixtures().toggleFocused(washer.id(),o,2);check(!map.fixtures().state(washer.id()).active(),"washer cannot be stopped");
  check(FixtureSoundscape.sound(washer.kind())==FixtureSoundscape.Sound.WASHER,"wrong washer audio");
  System.out.println("PASS complete house plumbing/laundry, room-facing basin, native shell fit and exact usable washer with its own sound");
 }
}
