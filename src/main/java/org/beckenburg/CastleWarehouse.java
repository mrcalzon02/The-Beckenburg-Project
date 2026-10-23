package org.beckenburg;
import java.util.ArrayList;
import java.util.List;
/** Model-backed castle equipment warehouse and physical retail counter. */
public final class CastleWarehouse {
 private CastleWarehouse(){}
 public record Rack(double x,double z,double scale){}
 public record Exhibit(String id,String mesh,double x,double y,double z,double scale){}
 public static final double COUNTER_X=32.7, COUNTER_Z=24.5;
 private static final List<Rack> RACKS=createRacks();
 private static final List<Exhibit> EXHIBITS=createExhibits();
 private static List<Rack> createRacks(){
  var result=new ArrayList<Rack>();
  for(double x:new double[]{30.5,40.7})
   for(double z:new double[]{19.5,22.7,25.9,29.1})
    result.add(new Rack(x,z,1.05));
  return List.copyOf(result);
 }
 private static List<Exhibit> createExhibits(){
  var result=new ArrayList<Exhibit>();int index=0;
  for(var tool:EquipmentRegistry.all()){
   var rack=RACKS.get(index/6);
   result.add(new Exhibit(tool.id(),EquipmentModelCatalog.model(tool.id(),EquipmentTiers.Tier.I),
    rack.x()+(index%2==0?-.25:.25),.40+(index%6/2)*.60,rack.z()+.1,.34));
   index++;
  }
  var rack=RACKS.get(index/6);
  result.add(new Exhibit("HEALTH_SUPPLIES",EquipmentModelCatalog.model("HEALTH_SUPPLIES",EquipmentTiers.Tier.I),
   rack.x()+(index%2==0?-.25:.25),.40+(index%6/2)*.60,rack.z()+.1,.34));
  return List.copyOf(result);
 }
 public static List<Rack> racks(){return RACKS;}
 public static List<Exhibit> exhibits(){return EXHIBITS;}
 public static boolean blocks(double x,double z,double radius){
  for(var rack:RACKS)
   if(Math.abs(x-rack.x())<.43+radius&&Math.abs(z-rack.z())<.60+radius)return true;
  return Math.abs(x-COUNTER_X)<.55+radius&&Math.abs(z-COUNTER_Z)<.35+radius;
 }
 public static boolean aimed(CastleHubMap map,double x,double z,double yaw){
  if(map==null||!map.traversable(x,z)||!Double.isFinite(yaw))return false;
  double dx=COUNTER_X-x,dz=COUNTER_Z-z,d=Math.hypot(dx,dz);
  return d>.45&&d<=2.6&&(dx*Math.sin(yaw)+dz*Math.cos(yaw))/d>.72;
 }
}
