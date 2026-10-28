package org.beckenburg;
import java.util.*;
/** Backdrop coordinates remain outside the castle structure; existing stone walls bound player movement. */
public final class CastleSurroundings {
 private CastleSurroundings(){}
 public record Tree(double x,double z,double scale){}
 public record Ridge(double x0,double z0,double x1,double z1,double peakX,double peakZ,double height,double heightLeft,double heightRight){}
 public record RoadPoint(double x,double z){}
 /** Exterior-only display objects. They have no stations, doors, inventory,
  * evidence actions or playable van state: castle navigation ends at the gate. */
 public record ParkedVehicle(String mesh,double x,double z,double scale,double yaw){
   public ParkedVehicle {
     if(!java.util.Set.of("ggbot_car08_van_shell","ggbot_car01","ggbot_car05").contains(mesh)
         ||!Double.isFinite(x+z+scale+yaw)||scale<=0)
       throw new IllegalArgumentException("invalid static PSX vehicle");
   }
 }
 private static final List<ParkedVehicle> PARKED=List.of(
     new ParkedVehicle("ggbot_car08_van_shell",13.3,27.7,.82,0),
     new ParkedVehicle("ggbot_car01",10.3,51.5,1.0,0),
     new ParkedVehicle("ggbot_car05",21.2,56.5,1.0,Math.PI)
 );
 public static List<ParkedVehicle> parkedVehicles(){return PARKED;}
 /** Matching world-space collision for the courtyard staging van. */
 public static boolean courtyardVanBlocks(double x,double z,double r){
  var van=PARKED.get(0);
  return Math.abs(x-van.x())<1.17+r&&Math.abs(z-van.z())<2.75+r;
 }

 public static List<RoadPoint> road(){return List.of(new RoadPoint(15.5,34.4),new RoadPoint(16,65),new RoadPoint(26,95),new RoadPoint(5,145));}
 public static List<Tree> trees(){List<Tree> result=new ArrayList<>();for(int i=0;i<20;i++){result.add(new Tree(-8-i%3*3,2+i*2.5,.85+(i%4)*.09));result.add(new Tree(54+i%3*3,-3+i*2.7,.9+(i%3)*.1));}for(int i=0;i<19;i++){double x=-8+i*3.5;result.add(new Tree(x,-10-(i%3)*3,1.1));if(x<10||x>22)result.add(new Tree(x,45+(i%3)*3,1.0));}return List.copyOf(result);}
 public static final double TERRAIN_STEP=8;
 /** Exact triangle interpolation shared by the terrain and all background roots. */
 private static double vertexHeight(double x,double z){
  double r=Math.hypot(x-22,z-18),blend=Math.max(0,Math.min(1,(r-34)/90));
  double roadDistance=Double.POSITIVE_INFINITY;var path=road();
  for(int i=0;i<path.size()-1;i++){
   var a=path.get(i);var b=path.get(i+1);double dx=b.x()-a.x(),dz=b.z()-a.z();
   double t=Math.max(0,Math.min(1,((x-a.x())*dx+(z-a.z())*dz)/(dx*dx+dz*dz)));
   roadDistance=Math.min(roadDistance,Math.hypot(x-a.x()-t*dx,z-a.z()-t*dz));
  }
  double corridor=Math.max(0,Math.min(1,(roadDistance-12)/9));
  return -.65+blend*corridor*(2.5+2.0*Math.sin(x*.027)+1.4*Math.cos(z*.031)+1.1*Math.sin((x+z)*.019));
 }
 public static double groundHeight(double x,double z){
  double sx=Math.floor(x/TERRAIN_STEP)*TERRAIN_STEP,sz=Math.floor(z/TERRAIN_STEP)*TERRAIN_STEP;
  double fx=(x-sx)/TERRAIN_STEP,fz=(z-sz)/TERRAIN_STEP;
  double a=vertexHeight(sx,sz),b=vertexHeight(sx+TERRAIN_STEP,sz),c=vertexHeight(sx+TERRAIN_STEP,sz+TERRAIN_STEP),d=vertexHeight(sx,sz+TERRAIN_STEP);
  return fx>=fz?a+(b-a)*fx+(c-b)*fz:a+(c-d)*fx+(d-a)*fz;
 }
 public static boolean landscapeContains(double x,double z){
  double dx=x-22,dz=z-18,a=Math.atan2(dz,dx);
  return Math.hypot(dx,dz)<168+19*Math.sin(a*3)+14*Math.cos(a*5);
 }
 public static List<Ridge> mountains(double cx,double cz){
  List<Ridge> out=new ArrayList<>();
  for(int ring=0;ring<2;ring++)for(int i=0;i<18;i++){
   double a=i*Math.PI/9,b=(i+1)*Math.PI/9,r=155+ring*58;
   double left=16+ring*9+7*Math.sin(i*.8+ring),right=16+ring*9+7*Math.sin((i+1)*.8+ring);
   out.add(new Ridge(cx+Math.cos(a)*r,cz+Math.sin(a)*r,cx+Math.cos(b)*r,cz+Math.sin(b)*r,
    cx+Math.cos((a+b)/2)*(r+3),cz+Math.sin((a+b)/2)*(r+3),(left+right)*.5+3,left,right));
  }
  return List.copyOf(out);
 }
}
