package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
/** Reusable surroundings. Every playable edge has a visible barrier at the same collision plane. */
public final class LevelEnclosure {
 private LevelEnclosure(){}
 public enum Setting { SUBURBAN,WOODLAND,URBAN,MOUNTAIN }
 public static Setting setting(String id){if(id.startsWith("enclosure-"))return Setting.valueOf(id.substring(10).toUpperCase(java.util.Locale.ROOT));return switch(id){case "house","mansion"->Setting.SUBURBAN;case "farmhouse","blackpine","hollow-creek"->Setting.WOODLAND;case "castle"->Setting.MOUNTAIN;default->Setting.URBAN;};}
 public record Edge(double x0,double z0,double x1,double z1,double inwardX,double inwardZ){public double length(){return Math.hypot(x1-x0,z1-z0);}}
 /** Boundary of the union, not a bounding rectangle that could enclose unsupported gaps. */
 public static List<Edge> edges(List<Room> rooms){
  var land=rooms.stream().filter(r->r.id()==12||r.id()==13).toList();TreeSet<Double> xs=new TreeSet<>(),zs=new TreeSet<>();
  for(var r:land){xs.add(r.x0());xs.add(r.x1());zs.add(r.z0());zs.add(r.z1());}
  var xx=new ArrayList<>(xs);var zz=new ArrayList<>(zs);List<Edge> result=new ArrayList<>();
  for(int i=0;i<xx.size()-1;i++)for(int j=0;j<zz.size()-1;j++){
   double x0=xx.get(i),x1=xx.get(i+1),z0=zz.get(j),z1=zz.get(j+1),x=(x0+x1)/2,z=(z0+z1)/2;
   if(!inside(land,x,z))continue;
   if(!inside(land,x0-.001,z))result.add(new Edge(x0,z0,x0,z1,1,0));
   if(!inside(land,x1+.001,z))result.add(new Edge(x1,z0,x1,z1,-1,0));
   if(!inside(land,x,z0-.001))result.add(new Edge(x0,z0,x1,z0,0,1));
   if(!inside(land,x,z1+.001))result.add(new Edge(x0,z1,x1,z1,0,-1));
  }return List.copyOf(result);
 }
 private static boolean inside(List<Room> rooms,double x,double z){return rooms.stream().anyMatch(r->x>=r.x0()&&x<=r.x1()&&z>=r.z0()&&z<=r.z1());}
 public static LevelDefinition template(Setting setting){
  var base=LevelPlans.build(switch(setting){case SUBURBAN->HouseMap.Site.HOUSE;case URBAN->HouseMap.Site.SCHOOL;default->HouseMap.Site.FARMHOUSE;});
  String id=setting==Setting.MOUNTAIN?"castle":base.id();var b=new LevelPlans.B(id,"Reusable "+setting+" enclosure",base.ambience());b.rooms.addAll(base.rooms());populate(b);
  return new LevelDefinition("enclosure-"+setting.name().toLowerCase(),"template-v1",b.title,b.rooms,List.of(),List.of(),List.of(),List.of(),b.sockets,b.colliders,base.dock(),setting.name());
 }
 static void populate(LevelPlans.B b){
  Setting setting=setting(b.id);String family=switch(setting){case SUBURBAN->"garden";case WOODLAND,MOUNTAIN->"field";default->"urban";};double height=switch(family){case "garden"->1.96;case "field"->1.4;default->1.87;};
  for(var e:edges(b.rooms)){
   double x=(e.x0+e.x1)/2+e.inwardX*.18,z=(e.z0+e.z1)/2+e.inwardZ*.18,angle=Math.atan2(e.z1-e.z0,e.x1-e.x0);
   b.socket("enclosure",13,x,0,z,-angle,family+":"+e.length());
   b.colliders.add(new Collider(x,0,z,e.length()/2,.15,height,angle));
   // A second line of landscape lies beyond the physical boundary, never in a walkable aisle.
   if(setting==Setting.WOODLAND||setting==Setting.MOUNTAIN||setting==Setting.SUBURBAN){
    for(double t=2;t<e.length();t+=setting==Setting.SUBURBAN?9:4.3){double ratio=t/e.length(),tx=e.x0+(e.x1-e.x0)*ratio-e.inwardX*(3.8+(t%3)),tz=e.z0+(e.z1-e.z0)*ratio-e.inwardZ*(3.8+(t%3));
     b.socket("landscape_tree",13,tx,0,tz,t*.7,setting==Setting.SUBURBAN?"level_tree_broadleaf_worn":"level_tree_conifer_worn");
    }
   }
  }
  var vanRoom=b.rooms.get(14);var van=new VanStaging(new VanStaging.Placement(vanRoom.cx(),vanRoom.cz(),-Math.PI/2));
  b.socket("arrival_tile",13,vanRoom.cx(),0,vanRoom.cz(),Math.PI/2,VanArrivalTile.ID);
  if(setting==Setting.SUBURBAN||setting==Setting.URBAN){
   // North-side residences occupy explicitly closed neighbour parcels; south-side homes stand beyond the outer fence.
   for(var home:VanStreetHouses.HOMES)if(home.side()>0){double front=home.streetFace()-.45,back=home.streetFace()+home.depth()+.25,lo=home.frontageStartZ()-.25,hi=home.frontageEndZ()+.25;
    var corners=List.of(van.world(front,lo),van.world(back,lo),van.world(back,hi),van.world(front,hi));
    for(int i=0;i<4;i++){var a=corners.get(i);var c=corners.get((i+1)%4);double length=Math.hypot(c.x()-a.x(),c.z()-a.z()),angle=Math.atan2(c.z()-a.z(),c.x()-a.x()),x=(a.x()+c.x())/2,z=(a.z()+c.z())/2;b.socket("parcel_fence",13,x,0,z,-angle,family+":"+length);b.colliders.add(new Collider(x,0,z,length/2,.10,height,angle));}
   }
   for(var h:VanStreetHouses.HOMES){double lx=(h.buildingStartX()+h.buildingEndX())/2,lz=h.frontageZ();var p=van.world(lx,lz);b.colliders.add(new Collider(p.x(),0,p.z(),h.depth()/2,h.frontage()/2,setting==Setting.URBAN?8:h.wallHeight(),van.placement().yaw()));}
   var h=VanStreetHouses.TERMINUS;var p=van.world(0,h.centreZ());b.colliders.add(new Collider(p.x(),0,p.z(),h.width()/2,h.depth()/2,h.height(),van.placement().yaw()));
  }
 }
}
