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
     if(!java.util.Set.of("ggbot_car08_van_shell","ggbot_car01","ggbot_car07").contains(mesh)
         ||!Double.isFinite(x+z+scale+yaw)||scale<=0)
       throw new IllegalArgumentException("invalid static PSX vehicle");
   }
 }
 private static final List<ParkedVehicle> PARKED=List.of(
     new ParkedVehicle("ggbot_car08_van_shell",15.5,44.0,1.0,0),
     new ParkedVehicle("ggbot_car01",10.3,51.5,1.0,0),
     new ParkedVehicle("ggbot_car07",21.2,56.5,1.0,Math.PI)
 );
 public static List<ParkedVehicle> parkedVehicles(){return PARKED;}

 public static List<RoadPoint> road(){return List.of(new RoadPoint(15.5,34.4),new RoadPoint(16,65),new RoadPoint(26,95),new RoadPoint(5,145));}
 public static List<Tree> trees(){List<Tree> result=new ArrayList<>();for(int i=0;i<20;i++){result.add(new Tree(-8-i%3*3,2+i*2.5,.85+(i%4)*.09));result.add(new Tree(54+i%3*3,-3+i*2.7,.9+(i%3)*.1));}for(int i=0;i<19;i++){double x=-8+i*3.5;result.add(new Tree(x,-10-(i%3)*3,1.1));if(x<10||x>22)result.add(new Tree(x,45+(i%3)*3,1.0));}return List.copyOf(result);}
 public static List<Ridge> mountains(double cx,double cz){List<Ridge> result=new ArrayList<>();for(int ring=0;ring<2;ring++)for(int i=0;i<18;i++){double a=i*Math.PI/9,b=(i+1)*Math.PI/9,r=76+ring*29,h=30+9*Math.sin(i*2.3+ring)+ring*11;result.add(new Ridge(cx+Math.cos(a)*r,cz+Math.sin(a)*r,cx+Math.cos(b)*r,cz+Math.sin(b)*r,cx+Math.cos((a+b)/2)*(r+8),cz+Math.sin((a+b)/2)*(r+8),h,12+6*Math.sin(i*1.7+ring),12+6*Math.sin((i+1)*1.7+ring)));}return List.copyOf(result);}
}
