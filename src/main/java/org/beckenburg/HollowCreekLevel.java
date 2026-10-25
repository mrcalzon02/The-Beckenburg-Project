package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
/** Native site zones are investigation ownership, never synthetic floor/wall geometry. */
public final class HollowCreekLevel {
 public static final String ID="hollow-creek";
 public static boolean is(LevelDefinition l){return l.id().equals(ID);}
 public static AuthoredSitePhysics physics(){try{return AuthoredSitePhysics.hollowCreek();}catch(java.io.IOException e){throw new IllegalStateException("Missing verified Hollow Creek physics",e);}}
 public static int owner(double x,double z){
  if(z>=73.9&&Math.abs(x-32)<8)return 13;if(z>69&&Math.abs(x-32)<8)return 12;
  int best=0;double distance=Double.POSITIVE_INFINITY;var zones=HollowCreekContract.zones();
  for(int i=0;i<zones.size();i++){var r=zones.get(i);double d=Math.hypot(x-r.x(),z-r.z());if(d<distance){best=i;distance=d;}}return best;
 }
 private static Vec floor(double x,double z,double hint){var hit=physics().support(x,z,hint,4);if(hit==null)throw new IllegalStateException("No authored support at "+x+","+z);return new Vec(x,hit.y(),z);}
 public static LevelDefinition build(){
  var rooms=new ArrayList<Room>();var portals=new ArrayList<Portal>();var sockets=new ArrayList<Socket>();
  for(var zone:HollowCreekContract.zones()){int i=rooms.size();rooms.add(new Room(i,zone.name(),zone.x0(),zone.z0(),zone.x1(),zone.z1(),zone.y(),8,"native-outdoor","woodland",new Vec(zone.x(),zone.y(),zone.z())));sockets.add(new Socket("hc-evidence-"+i,"evidence",i,zone.x(),zone.y()+.025,zone.z(),0,"floor-inspection"));}
  for(int id=12;id<14;id++){double z=id==12?71:74;Vec p=floor(32,z,1.5);rooms.add(new Room(id,id==12?"Woodland arrival trail":"Van approach",24,68,40,84,p.y(),10,"native-outdoor","woodland",p));}
  rooms.add(new Room(14,"Expedition van",30.9,75.8,33.1,80.2,2,VanStaging.INTERIOR_HEIGHT,"van","metal",new Vec(32,2,78)));
  for(var route:HollowCreekContract.links()){double[] p=route.points()[route.points().length/2];Vec at=floor(p[0],p[2],p[1]);portals.add(new Portal(portals.size(),route.a(),route.b(),at.x(),at.y(),at.z(),route.width(),true,"trail",.12));}
  portals.add(new Portal(portals.size(),13,12,32,1.5,73,3,true,"trail",.05));portals.add(new Portal(portals.size(),13,14,32,1.5,74.3,1.2,true,"trail",.05));
  double[][] rites={{32,-1.3,12},{49,.3,54},{32,0,42}};String[] names={"banishment","cleansing","absolution"};
  for(int i=0;i<3;i++){Vec p=floor(rites[i][0],rites[i][2],rites[i][1]);sockets.add(new Socket("hc-ritual-"+names[i],"ritual",owner(p.x(),p.z()),p.x(),p.y(),p.z(),0,names[i]));}
  sockets.add(new Socket("hc-native-scene","native_scene",13,32,1.5,78,0,HollowCreekScene.revision()+"|"+HollowCreekContract.SHA256));
  HollowCreekPower.install(sockets);
  for(var refuge:HollowCreekContract.refuges()){Vec p=floor(refuge[0],refuge[2],refuge[1]);sockets.add(new Socket("hc-refuge-"+sockets.size(),"outdoor-hide",owner(p.x(),p.z()),p.x(),p.y(),p.z(),0,"shrub-pocket"));}
  return new LevelDefinition(ID,"native-site-v1", "Camp Hollow Creek",rooms,portals,List.of(),List.of(),List.of(),sockets,List.of(),new Dock(32,1.5,78,0),"woodland");
 }
 private HollowCreekLevel(){}
}
