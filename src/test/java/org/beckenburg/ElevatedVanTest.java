package org.beckenburg;
/** The same module's cargo/ramp supports follow its authored world elevation. */
public final class ElevatedVanTest {
 private static void close(double a,double b){if(Math.abs(a-b)>1e-8)throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  for(double y:new double[]{-2,0,1.5})for(double yaw:new double[]{0,Math.PI/2,-.4}){
   var van=new VanStaging(new VanStaging.Placement(32,y,78,yaw));
   var cargo=van.world(0,0);close(van.supportHeight(cargo.x(),cargo.z()),y+.5);
   var toe=van.world(0,VanStaging.FRONT-VanStaging.RAMP_LENGTH);close(van.supportHeight(toe.x(),toe.z()),y);
   var mid=van.world(0,VanStaging.FRONT-VanStaging.RAMP_LENGTH/2);close(van.supportHeight(mid.x(),mid.z()),y+.25);
   close(van.floorY(),y+.5);
   var side=van.world(2,0);if(van.clearSight(cargo.x(),y+1.5,cargo.z(),side.x(),y+1.5,side.z()))throw new AssertionError("Raised side panel light leak");
   var front=van.world(0,VanStaging.FRONT-1);if(!van.clearSight(cargo.x(),y+1.5,cargo.z(),front.x(),y+1.5,front.z()))throw new AssertionError("Open cargo aperture occluded");
   van.setCargoDoorClosed(true);if(van.clearSight(cargo.x(),y+1.5,cargo.z(),front.x(),y+1.5,front.z()))throw new AssertionError("Closed cargo door light leak");
   if(van.clearSight(cargo.x(),y+2.4,cargo.z(),cargo.x(),y+3.5,cargo.z()))throw new AssertionError("Raised roof light leak");
  }
  close(new VanStaging.Placement(32,78,0).y(),0);
  System.out.println("PASS elevated van cargo/ramp support across three heights and rotations; original three-argument placements preserved");
 }
}
