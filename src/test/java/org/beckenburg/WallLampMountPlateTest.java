package org.beckenburg;
import java.util.*;
/** Actual renderer bracket faces retain source metal UVs, contact and fixture scaling. */
public final class WallLampMountPlateTest {
 private static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  var scene=CastleLevelEdits.current();int plates=0;
  var renderer=SceneRenderer.class.getDeclaredMethod("lampPlate",List.class,CastleWallLampMount.Plate.class,int.class);renderer.setAccessible(true);
  for(var lamp:scene.lamps())if(lamp.mount()==CastleHubDecor.Mount.WALL){
   var fixture=lamp.fixture();var pose=CastleWallLampMount.resolve(fixture,scene.walls());check(pose.supported(),"Missing wall support");
   var p=CastleWallLampMount.plate(fixture,pose);double fx=Math.sin(fixture.yaw()),fz=Math.cos(fixture.yaw());
   double centerNormal=(pose.wallX()-(p.x()+p.width()/2))*fx+(pose.wallZ()-(p.z()+p.depth()/2))*fz;
   check(Math.abs(centerNormal-(CastleDoorSlots.MASONRY_THICKNESS/2+.011))<1e-8,"Plate separated from masonry");
   check(Math.abs(p.height()-.12*fixture.scale())<1e-8,"Unscaled oversized plate");
   check(Math.abs(Math.max(p.width(),p.depth())-.76*fixture.scale())<1e-8,"Bracket width does not follow fixture");
   int material=GlbProps.model(fixture.off()).triangles().get(0).material();
   var faces=new ArrayList<>();renderer.invoke(null,faces,p,material);check(faces.size()==12,"Bracket is not closed six-sided geometry");
   var colors=new HashSet<Integer>();
   for(var face:faces){
    var type=face.getClass();var accessor=type.getDeclaredMethod("material");accessor.setAccessible(true);
    check((int)accessor.invoke(face)==material&&material>=GlbProps.TEXTURE_BASE,"Bracket regressed to flat color");
    for(String prefix:new String[]{"a","b","c"}){
     var u=type.getDeclaredMethod(prefix+"u");var v=type.getDeclaredMethod(prefix+"v");u.setAccessible(true);v.setAccessible(true);
     check((double)u.invoke(face)>=.08&&(double)u.invoke(face)<=.92&&(double)v.invoke(face)>=.60&&(double)v.invoke(face)<=.95,"Bracket UV escapes original metal region");
    }
   }
   for(int u=0;u<20;u++)for(int v=0;v<20;v++)colors.add(GlbProps.sample(material,.08+.84*u/19,.60+.35*v/19));
   check(colors.size()>20,"Bracket metal region lacks texture detail");
   // Clip original rendered source triangles to the bracket box. Contact must
   // be actual geometry, not only overlapping overall source bounding boxes.
   boolean contact=false;var b=SceneRenderer.castleBounds(fixture.off());
   for(var t:GlbProps.model(fixture.off()).triangles()){
    var polygon=new ArrayList<double[]>();
    for(var vertex:List.of(t.a(),t.b(),t.c())){
     double x=(vertex.x()-b[0])*fixture.scale(),z=(vertex.z()-b[2])*fixture.scale();
     polygon.add(new double[]{pose.x()+x*Math.cos(fixture.yaw())-z*Math.sin(fixture.yaw()),fixture.y()+(vertex.y()-b[1]-b[4]/2)*fixture.scale(),pose.z()+x*Math.sin(fixture.yaw())+z*Math.cos(fixture.yaw())});
    }
    double[] lo={p.x(),p.y(),p.z()},hi={p.x()+p.width(),p.y()+p.height(),p.z()+p.depth()};
    for(int axis=0;axis<3;axis++){polygon=clip(polygon,axis,lo[axis],true);polygon=clip(polygon,axis,hi[axis],false);}
    if(!polygon.isEmpty()){contact=true;break;}
   }
   check(contact,"Textured bracket does not meet original fixture geometry "+lamp.id());plates++;
  }
  check(plates>=12,"Expected real Castle wall fixtures");
  System.out.println("PASS "+plates+" actual wall-light brackets: original textured metal faces/UV region, masonry contact, scaled dimensions and source-triangle contact");
 }
 private static ArrayList<double[]> clip(ArrayList<double[]> input,int axis,double edge,boolean lower){
  var out=new ArrayList<double[]>();if(input.isEmpty())return out;double[] a=input.get(input.size()-1);boolean insideA=lower?a[axis]>=edge:a[axis]<=edge;
  for(double[] b:input){boolean insideB=lower?b[axis]>=edge:b[axis]<=edge;
   if(insideA!=insideB){double t=(edge-a[axis])/(b[axis]-a[axis]);out.add(new double[]{a[0]+(b[0]-a[0])*t,a[1]+(b[1]-a[1])*t,a[2]+(b[2]-a[2])*t});}
   if(insideB)out.add(b);a=b;insideA=insideB;
  }return out;
 }
}
