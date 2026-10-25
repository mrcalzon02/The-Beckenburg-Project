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
  int[] pixels=new int[64*64];
  for(int z=0;z<64;z++)for(int x=0;x<64;x++){double ripple=Math.sin(x*Math.PI/8+Math.sin(z*Math.PI/16)*.8)*Math.cos(z*Math.PI/8+x*Math.PI/32);int v=(int)(ripple*10);pixels[z*64+x]=((128+v*5)<<16)|((128+v*5)<<8)|(128+v*5);}
  var texture=new Texture(64,64,pixels);
  var effects=new WaterEffects(new WaterEffects.Layer(texture,.28,.28,.015,.001,128),new WaterEffects.Layer(texture,.19,.23,-.015,-.02,192),.27,.35,.065,1.0,.8,.045,.28,.65,5,.5,0x303b4c,new WaterEffects.ShoreWave(bed(),LEVEL,.03,5.5,2.8,.22));
  var body=new Body("Hollow Creek lake and creek",new Polygon(List.of(new Point(-105,-120),new Point(145,-120),new Point(145,30),new Point(-105,30))),LEVEL,bed(),new Texture(64,64,pixels),new Style(0x3a6261,0x173b44,.65,.22,.92,3.2,.018),List.of(dry),Boolean.getBoolean("beckenburg.water.flat")?null:effects);
  return module=new WaterModule(List.of(body));
 }
 private HollowCreekWater(){}
}
