package org.beckenburg;
import dev.mrcalzon.water.WaterModule;
import dev.mrcalzon.water.WaterEffects;
import dev.mrcalzon.water.HullSections;
import dev.mrcalzon.water.GridBed;
import dev.mrcalzon.water.WaterModule.*;
import java.util.*;
/** Actual inner hull waterline exported from the native double-skin mesh. */
public final class HollowCreekWater {
 public static final double LEVEL=-1.95;
 private static GridBed bed;
 public static synchronized GridBed bed(){if(bed==null)try{bed=GridBed.read(HollowCreekWater.class.getResourceAsStream("/org/beckenburg/sites/hollow-creek-water-bed.bin"));}catch(java.io.IOException e){throw new IllegalStateException("Invalid optical basin",e);}return bed;}
 private static WaterModule module;
 public static synchronized WaterModule module(){
  if(module!=null)return module;
  if(!HollowCreekScene.sourceSha().equals("e7d53ad52972b5963132876aa40540fa197c8f92db131d8360c90c378ee7740f"))throw new IllegalStateException("Regenerate native water/hull contract");
  HullSections dry;try{dry=HullSections.read(HollowCreekWater.class.getResourceAsStream("/org/beckenburg/sites/hollow-creek-water-hull.tsv"));}catch(java.io.IOException e){throw new IllegalStateException("Invalid native hull",e);}
  int[] pixels=new int[64*64],second=new int[64*64];
  for(int z=0;z<64;z++)for(int x=0;x<64;x++){
   double u=x/64.0,v=z/64.0,warp=.19*Math.sin(u*Math.PI*2)+.13*Math.sin((u+v)*Math.PI*2),noise=(Math.sin(x*127.1+z*311.7)*43758.5453)%1;
   int a=(int)(128+16*Math.sin(Math.PI*2*(v*5+warp))+7*noise),b=(int)(128+14*Math.sin(Math.PI*2*(u*3+v*2+.21*Math.sin(v*Math.PI*4)))-6*noise);
   pixels[z*64+x]=(a<<16)|(a<<8)|a;second[z*64+x]=(b<<16)|(b<<8)|b;
  }
  var texture=new Texture(64,64,pixels);var texture2=new Texture(64,64,second);
  var effects=new WaterEffects(new WaterEffects.Layer(texture,.28,.28,.015,.001,128),new WaterEffects.Layer(texture2,.19,.23,-.015,-.02,192),.27,.35,.065,1.0,.8,.045,.28,.65,5,.5,0x303b4c,new WaterEffects.ShoreWave(bed(),LEVEL,.03,5.5,2.8,.22));
  var body=new Body("Hollow Creek lake and creek",new Polygon(List.of(new Point(-105,-120),new Point(145,-120),new Point(145,30),new Point(-105,30))),LEVEL,bed(),new Texture(64,64,pixels),new Style(0x365859,0x132e39,1.8,.48,.97,3.2,.018),List.of(dry),Boolean.getBoolean("beckenburg.water.flat")?null:effects);
  return module=new WaterModule(List.of(body));
 }
 private HollowCreekWater(){}
}
