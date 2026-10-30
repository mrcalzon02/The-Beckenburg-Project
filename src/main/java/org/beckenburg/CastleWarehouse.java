package org.beckenburg;
import java.util.ArrayList;
import java.util.List;
/** Supported, aisle-facing retail displays. Each family occupies one column; tiers form rows. */
public final class CastleWarehouse {
 private CastleWarehouse(){}
 public static final String SHELF="level_castle_retail_shelf";
 public static final double WIDTH=2.06,DEPTH=.64,HEIGHT=2.54;
 public static final double[] SHELVES={.35,1.10,1.85};
 public record Rack(double x,double z,double scale,double yaw){
  public double[] world(double x,double z){return new double[]{this.x+x*Math.cos(yaw)-z*Math.sin(yaw),this.z+x*Math.sin(yaw)+z*Math.cos(yaw)};}
 }
 public record Exhibit(String id,EquipmentTiers.Tier tier,String mesh,int rack,int column,
                       double x,double y,double z,double scale,double yaw){}
 public static final double COUNTER_X=32.7,COUNTER_Z=24.5;
 private static final List<Rack> RACKS=createRacks();
 private static List<Exhibit> exhibits;
 private static List<Rack> createRacks(){
  var out=new ArrayList<Rack>();
  for(double z:new double[]{19.4,21.55,27.5,29.65,31.8})out.add(new Rack(29.95,z,1,-Math.PI/2));
  for(double z:new double[]{19.4,21.55,23.7,25.85,28,30.15})out.add(new Rack(41.65,z,1,Math.PI/2));
  out.add(new Rack(32.2,18.1,1,0));out.add(new Rack(39.1,18.1,1,0));
  out.add(new Rack(34.5,32.75,1,Math.PI));out.add(new Rack(37,32.75,1,Math.PI));
  return List.copyOf(out);
 }
 public static synchronized List<Exhibit> exhibits(){
  if(exhibits!=null)return exhibits;
  var families=new ArrayList<String>();for(var tool:EquipmentRegistry.all())families.add(tool.id());
  families.add("HEALTH_SUPPLIES");families.sort(String::compareTo);
  var out=new ArrayList<Exhibit>();
  for(int i=0;i<families.size();i++)for(var tier:EquipmentTiers.Tier.values()){
   var rack=RACKS.get(i/3);var p=rack.world((i%3-1)*.66,0);
   String id=families.get(i),mesh=EquipmentModelCatalog.model(id,tier);
   double[] b=SceneRenderer.castleBounds(mesh);
   double scale=Math.min(1,Math.min(.55/b[3],Math.min(.50/b[5],.62/b[4])));
   out.add(new Exhibit(id,tier,mesh,i/3,i%3,p[0],SHELVES[tier.ordinal()],p[1],scale,rack.yaw));
  }
  return exhibits=List.copyOf(out);
 }
 public static List<Rack> racks(){return RACKS;}
 /** Read a physical plaque at the crosshair. Rear-facing, distant and
  * architecture-occluded labels cannot produce a readable overlay. */
 public static Exhibit aimedExhibit(CastleHubMap map,double x,double eye,double z,double yaw,double pitch){
  Exhibit best=null;double score=Double.POSITIVE_INFINITY;
  double cy=Math.cos(yaw),sy=Math.sin(yaw),cp=Math.cos(pitch),sp=Math.sin(pitch);
  for(var item:exhibits()){
   var rack=RACKS.get(item.rack);var center=rack.world((item.column-1)*.66,.326);
   if((x-center[0])*(-Math.sin(rack.yaw))+(z-center[1])*Math.cos(rack.yaw)<.05)continue;
   double dx=center[0]-x,dy=item.y+.20-eye,dz=center[1]-z;
   double d2=dx*dx+dy*dy+dz*dz;if(d2>4.5*4.5||d2<.15*.15)continue;
   double forward=dx*sy+dz*cy,side=-dx*cy+dz*sy,up=dy*cp-forward*sp,depth=dy*sp+forward*cp;
   if(depth<=.15||Math.abs(side)>CastleShelfLabels.WIDTH*.55||Math.abs(up)>.40)continue;
   double error=side*side+up*up;
   if(error>=score||!FixtureLighting.castleRayClear(map,x,eye,z,center[0],item.y+.20,center[1]))continue;
   best=item;score=error;
  }
  return best;
 }
 public static boolean blocks(double x,double z,double radius){
  for(var r:RACKS){double dx=x-r.x,dz=z-r.z;
   if(Math.abs(dx*Math.cos(r.yaw)+dz*Math.sin(r.yaw))<WIDTH*.5+radius&&
      Math.abs(-dx*Math.sin(r.yaw)+dz*Math.cos(r.yaw))<DEPTH*.5+radius)return true;
  }
  return Math.abs(x-COUNTER_X)<.82+radius&&Math.abs(z-COUNTER_Z)<.27+radius;
 }
 public static boolean aimed(CastleHubMap map,double x,double z,double yaw){
  if(map==null||!map.traversable(x,z)||!Double.isFinite(yaw))return false;
  double dx=COUNTER_X-x,dz=COUNTER_Z-z,d=Math.hypot(dx,dz);
  return d>.45&&d<=2.6&&(dx*Math.sin(yaw)+dz*Math.cos(yaw))/d>.72;
 }
}
