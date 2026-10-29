package org.beckenburg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** Each physically authored portal is a unique, occupiable doorway socket.
 * The doorway is not a second door model; it owns zero or one assembly.
 */
public final class CastleDoorSlots {
 private CastleDoorSlots(){}
 public static final double MASONRY_THICKNESS=.42;
 public enum Type { EMPTY, WOOD, PORTCULLIS }
 /** Apertures are inside the wall volume, never painted on an unbroken face. */
 public record WindowSlot(String id,double x,double z0,double z1,double sill,double head){}
 private static final List<WindowSlot> WINDOWS=List.of(
    new WindowSlot("west-casement-north",1,16,17.8,1.05,2.45),
    new WindowSlot("west-casement-south",1,26,27.8,1.05,2.45));
 public static List<WindowSlot> windows(){return WINDOWS;}
 public record Slot(String id,double x0,double z0,double x1,double z1,
                    double bottom,double clearHeight,boolean gate) {
  public boolean horizontal(){return z0==z1;}
  public double centerX(){return (x0+x1)*.5;}
  public double centerZ(){return (z0+z1)*.5;}
  public double width(){return horizontal()?x1-x0:z1-z0;}
 }
 private static final List<Slot> SLOTS=discover();
 private static final java.util.Map<String,Slot> BY_ID=indexSlots();
 private static java.util.Map<String,Slot> indexSlots(){
  var result=new java.util.HashMap<String,Slot>();
  for(var slot:SLOTS)if(result.put(slot.id(),slot)!=null)throw new IllegalStateException("Duplicate Castle doorway");
  return java.util.Map.copyOf(result);
 }
 private static List<Slot> discover(){
  var slots=new ArrayList<Slot>();var names=new HashSet<String>();
  for(var wall:CastleHubArchitecturalPlan.walls()){
   if(!wall.hasDoor())continue;
   boolean gate=wall.horizontal()&&Math.abs(wall.z0()-34)<.001
       &&Math.abs(wall.portalCenter()-15.5)<.001;
   String id=gate?"courtyard-main-gate":
       String.format(Locale.ROOT,"portal-%s-%d-%d",
           wall.horizontal()?"h":"v",
           Math.round((wall.horizontal()?wall.z0():wall.x0())*100),
           Math.round(wall.portalCenter()*100));
   if(!names.add(id))throw new IllegalStateException("Duplicate physical portal "+id);
   slots.add(new Slot(id,wall.x0(),wall.z0(),wall.x1(),wall.z1(),
       wall.bottom(),gate?2.95:CastleHubArchitecturalPlan.DOOR_HEIGHT,gate));
  }
  if(slots.stream().filter(Slot::gate).count()!=1)
   throw new IllegalStateException("Castle requires exactly one courtyard gate socket");
  return List.copyOf(slots);
 }
 public static List<Slot> all(){return SLOTS;}
 /** A slot is a single point of authority for occupancy, not overlapping door meshes. */
 public static boolean canInstall(String id,Type type){
  Slot slot=require(id);
  return type!=null&&type!=Type.EMPTY&&(slot.gate()==(type==Type.PORTCULLIS));
 }
 public static Slot require(String id){
  Slot slot=id==null?null:BY_ID.get(id);
  if(slot==null)throw new IllegalArgumentException("Unknown physical castle doorway "+id);
  return slot;
 }
 public static Type initial(Slot slot){return slot.gate()?Type.PORTCULLIS:Type.EMPTY;}
 /** Solid core geometry has identical extent for visuals, collision and shadows. */
 public static boolean containsWall(CastleHubArchitecturalPlan.Wall wall,
                                      double x,double y,double z,double radius){
  if(wall.hasDoor()||y>=wall.top()||y+1.55<=wall.bottom())return false;
  double half=MASONRY_THICKNESS*.5;
  return wall.horizontal()
      ?x>=Math.min(wall.x0(),wall.x1())-radius&&x<=Math.max(wall.x0(),wall.x1())+radius
       &&Math.abs(z-wall.z0())<=half+radius
      :z>=Math.min(wall.z0(),wall.z1())-radius&&z<=Math.max(wall.z0(),wall.z1())+radius
       &&Math.abs(x-wall.x0())<=half+radius;
 }
 /** Slab intersection of a light ray and a solid rectangular block. */
 public static boolean crossesBox(double ax,double ay,double az,double bx,double by,double bz,
                                   double x0,double y0,double z0,double x1,double y1,double z1){
  double[] origin={ax,ay,az},delta={bx-ax,by-ay,bz-az},lo={x0,y0,z0},hi={x1,y1,z1};
  double enter=0,exit=1;
  for(int i=0;i<3;i++){
   if(Math.abs(delta[i])<1e-9){
    if(origin[i]<=lo[i]+1e-5||origin[i]>=hi[i]-1e-5)return false;
    continue;
   }
   double a=(lo[i]-origin[i])/delta[i],b=(hi[i]-origin[i])/delta[i];
   if(a>b){double temp=a;a=b;b=temp;}
   enter=Math.max(enter,a);exit=Math.min(exit,b);
   if(exit<=enter+1e-7)return false;
  }
  return enter<1-1e-5&&exit>1e-5;
 }
 public static boolean crossesWall(CastleHubArchitecturalPlan.Wall wall,
                    double ax,double ay,double az,double bx,double by,double bz){
  if(wall.hasDoor())return false;
  double half=MASONRY_THICKNESS*.5;
  return wall.horizontal()
   ?crossesBox(ax,ay,az,bx,by,bz,Math.min(wall.x0(),wall.x1()),
      wall.bottom(),wall.z0()-half,Math.max(wall.x0(),wall.x1()),wall.top(),wall.z0()+half)
   :crossesBox(ax,ay,az,bx,by,bz,wall.x0()-half,wall.bottom(),
      Math.min(wall.z0(),wall.z1()),wall.x0()+half,wall.top(),
      Math.max(wall.z0(),wall.z1()));
 }
 public static boolean leafBlocks(Slot slot,double x,double y,double z,double radius){
  if(y>slot.bottom()+slot.clearHeight()||y+1.55<slot.bottom())return false;
  double half=MASONRY_THICKNESS*.35;
  return slot.horizontal()
      ?x>=slot.x0()-radius&&x<=slot.x1()+radius
         &&Math.abs(z-slot.z0())<=half+radius
      :z>=slot.z0()-radius&&z<=slot.z1()+radius
         &&Math.abs(x-slot.x0())<=half+radius;
 }
 public static boolean crossesLeaf(Slot slot,
             double ax,double ay,double az,double bx,double by,double bz){
  double half=MASONRY_THICKNESS*.35;
  return slot.horizontal()
     ?crossesBox(ax,ay,az,bx,by,bz,slot.x0(),slot.bottom(),
         slot.z0()-half,slot.x1(),slot.bottom()+slot.clearHeight(),slot.z0()+half)
     :crossesBox(ax,ay,az,bx,by,bz,slot.x0()-half,slot.bottom(),
         slot.z0(),slot.x0()+half,slot.bottom()+slot.clearHeight(),slot.z1());
 }
}
