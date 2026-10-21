package org.beckenburg;
import java.util.*;
public final class SurroundingClearanceTest {
 static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 public static void main(String[]args){
  for(var site:AuthoredTestSupport.SITES){var map=new HouseMap(site);var setting=LevelEnclosure.setting(map.level().id());int checked=0;
   if(setting==LevelEnclosure.Setting.SUBURBAN||setting==LevelEnclosure.Setting.URBAN)for(var home:VanStreetHouses.HOMES){double xmin=Double.POSITIVE_INFINITY,xmax=-xmin,zmin=xmin,zmax=-xmin;
    for(double x:new double[]{home.buildingStartX()-.18,home.buildingEndX()+.18})for(double z:new double[]{home.frontageStartZ()-.2,home.frontageEndZ()+.2}){var p=map.van().world(x,z);xmin=Math.min(xmin,p.x());xmax=Math.max(xmax,p.x());zmin=Math.min(zmin,p.z());zmax=Math.max(zmax,p.z());}
    for(var room:map.level().rooms())if(!room.outside())check(!(xmin<room.x1()+.12&&xmax>room.x0()-.12&&zmin<room.z1()+.12&&zmax>room.z0()-.12),"surrounding residence impinges on "+site+" "+room.name());
    for(var edge:LevelEnclosure.edges(map.level().rooms())){boolean vertical=Math.abs(edge.x0()-edge.x1())<.01;double plane=vertical?edge.x0()+edge.inwardX()*.18:edge.z0()+edge.inwardZ()*.18;check(!(vertical?plane>xmin&&plane<xmax&&Math.max(edge.z0(),edge.z1())>zmin&&Math.min(edge.z0(),edge.z1())<zmax:plane>zmin&&plane<zmax&&Math.max(edge.x0(),edge.x1())>xmin&&Math.min(edge.x0(),edge.x1())<xmax),"outer fence passes through a surrounding residence "+site);}
    check(!home.intrudesOnStreet(),"residence obstructs shared road");checked++;
   }
   for(var tree:map.level().sockets())if(tree.type().equals("landscape_tree"))for(var room:map.level().rooms())if(!room.outside())check(!(tree.x()>room.x0()-.3&&tree.x()<room.x1()+.3&&tree.z()>room.z0()-.3&&tree.z()<room.z1()+.3),"landscape tree intersects actual building");
   System.out.println("PASS "+site+" "+checked+" neighbouring structures clear of native building, road and enclosing fences");
  }
  for(var tree:CastleSurroundings.trees())check(tree.x()<-3||tree.x()>CastleHubMap.WIDTH+3||tree.z()<-3||tree.z()>CastleHubMap.HEIGHT+3,"castle backdrop tree invades authored hub");
  check(CastleSurroundings.road().get(0).z()>34&&CastleSurroundings.road().get(3).z()>120,"castle road does not lead into distance outside its closed perimeter");
  System.out.println("PASS castle mountain/tree/road backdrop outside physical castle footprint");
 }
}
