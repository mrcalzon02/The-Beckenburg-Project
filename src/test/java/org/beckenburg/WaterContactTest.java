package org.beckenburg;
import dev.mrcalzon.water.*;
import dev.mrcalzon.water.WaterModule.*;
import java.util.*;
public final class WaterContactTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  var c=new Camera(0,1,0,0,-Math.PI/4,100,101,101);double[] depth=new double[10201];Arrays.fill(depth,Double.POSITIVE_INFINITY);int at=50*101+51;
  depth[at]=Math.sqrt(2);var contact=new WaterContact(c,depth).near(50,50,0,0,1,Math.sqrt(2),0,0);check(contact!=null&&contact.distance()<.02&&contact.foam()>.9,"Actual nearby solid-water intersection must create foam");
  depth[at]=1;check(new WaterContact(c,depth).near(50,50,0,0,1,Math.sqrt(2),0,0)==null,"Floating object above water must not create a foamy contact");
  depth[at]=10;check(new WaterContact(c,depth).near(50,50,0,0,1,Math.sqrt(2),0,0)==null,"Unrelated distant opaque geometry cannot produce a halo");
  double[] bed=new double[10201];
  for(int y=0;y<101;y++)for(int x=0;x<101;x++){double screenY=(50-y)/100.0,dy=screenY*Math.cos(c.pitch())+Math.sin(c.pitch());bed[y*101+x]=dy<0?(-.15-c.y())/dy:Double.POSITIVE_INFINITY;}
  check(new WaterContact(c,bed).near(50,50,0,0,1,Math.sqrt(2),0,0)==null,"A shallow flat bed must not cover the whole water surface with contact foam");
  int[] pixels=new int[10201];double[] far=new double[10201];Arrays.fill(far,Double.POSITIVE_INFINITY);var emitters=List.of(new WaterSpray.Emitter(0,0,2,1,0,1,17));int droplets=0;
  for(double t=0;t<2;t+=.05)droplets+=WaterSpray.draw(c,pixels,far,t,emitters,(rgb,x,y,z)->rgb);check(droplets>0,"Actual ballistic spray must project visible moving droplets");Arrays.fill(far,.1);check(WaterSpray.draw(c,pixels,far,.1,emitters,(rgb,x,y,z)->rgb)==0,"Foreground geometry must occlude spray");
  var water=HollowCreekWater.module();var e=water.bodies().get(0).effects();check(e.amplitude()>=.15,"Geometry wave amplitude must exceed the earlier subtle pass");double low=10,high=-10;
  for(double t=0;t<16;t+=.1){double h=e.surface(30,0,t).offset();low=Math.min(low,h);high=Math.max(high,h);}check(high-low>.20,"Rendered wave triangles must have substantial actual vertical travel");
  var hull=HullSections.read(WaterContactTest.class.getResourceAsStream("/org/beckenburg/sites/hollow-creek-water-hull.tsv"));check(hull.sections().get(hull.sections().size()-1).y()>=HollowCreekWater.LEVEL+e.envelope(),"Hull section top covers highest crest");
  var boatCamera=new Camera(23.9,-.5,-5,0,-Math.PI/2,1,1,1);
  for(double t=0;t<16;t+=.1){int[] boat={0xa06030};double[] floor={1.585};water.composite(boatCamera,boat,floor,t);check(boat[0]==0xa06030,"Full-wave hull exclusion or actual floor occlusion must keep the cockpit dry t="+t+" waterY="+(HollowCreekWater.LEVEL+e.surface(23.9,-5,t).offset())+" pixel="+Integer.toHexString(boat[0]));}
  System.out.println("PASS physical-depth foam contacts; no floating/distant-object halos; visible depth-tested ballistic spray; >200mm real wave travel; highest crest profile and full-cycle dry cockpit");
 }
}
