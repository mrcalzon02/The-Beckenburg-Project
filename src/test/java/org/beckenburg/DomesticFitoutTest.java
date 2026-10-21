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
  var farm=new HouseMap(HouseMap.Site.FARMHOUSE);check(count(farm,"toilet_mp_1")==1&&count(farm,"level_bath_shower")==1,"farm private bathroom incomplete");
  check(count(farm,"level_laundry_washer")==1&&count(farm,"level_yard_clothesline")==1,"farm washing/drying missing");
  var school=new HouseMap(HouseMap.Site.SCHOOL);check(count(school,"toilet_mp_1")==6,"school needs three private cubicles per washroom");
  var sinks=school.fixtures().states().stream().filter(f->f.kind()==HauntedFixtures.Kind.SINK_BATH).toList();
  check(sinks.size()==4&&sinks.stream().filter(f->f.scale()==.72).count()==2&&sinks.stream().filter(f->f.scale()==1).count()==2,"mixed-age school handwashing heights lost");
  check(school.level().props().stream().filter(p->p.mesh().equals("toilet_mp_1")&&p.scale()==.72).count()==2,"younger pupils need appropriately sized toilets");
  for(HouseMap site:List.of(farm,school))for(var door:site.level().portals())if(door.door()&&door.a()==door.b()){
   double dx=door.alongX()?0:.55,dz=door.alongX()?.55:0,y=door.y()+1.1;
   check(!site.clearInteractionSight(door.x()-dx,y,door.z()-dz,door.x()+dx,y,door.z()+dz,-1),"privacy door must block sight while shut "+door.id());
   site.toggleDoor(door.id());check(site.clearInteractionSight(door.x()-dx,y,door.z()-dz,door.x()+dx,y,door.z()+dz,-1),"privacy door must clear sight when open "+door.id());
  }
  for(var f:sinks){var observer=InteractionFocusTest.find(school,f.x(),f.y()-.12*f.scale(),f.z()+.19*f.scale(),InteractionFocus.Kind.FIXTURE,""+f.id(),List.of());check(observer!=null,"school basin not usable by exact crosshair "+f.id());}
  System.out.println("PASS complete house plumbing/laundry, room-facing basin, native shell fit and exact usable washer with its own sound");
 }
}
