package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
/** Generated from actual native supports; HC-DOCK-01, not a replacement grid. */
public final class HollowCreekPower {
 public static final String SOURCE_SHA="e7d53ad52972b5963132876aa40540fa197c8f92db131d8360c90c378ee7740f";
 public static final Vec PANEL=new Vec(27.80250061035156,-0.019999921321868896,-7.3999998569488525);
 public static final Vec STARTER_SUPPORT=new Vec(26.649999618530273,-0.3499999940395355,-7.8580001335144045);
 public static final double[][] LAMPS={{30.119999885559082,1.1599999475479126,-2.683333158493042},{30.119999885559082,1.2055513668060303,1.4000000357627869},{30.119999885559082,1.1599999475479126,0.3999999761581421},{30.119999885559082,1.2055513668060303,-0.6000001430511475},{30.119999885559082,1.3300001430511474,-1.6000001430511475},{30.119999885559082,1.3300001430511474,-3.633333206176758},{30.119999885559082,1.2055513668060303,-4.6666669845581055},{30.119999885559082,1.1599999475479126,-5.700000286102295},{30.119999885559082,1.2055513668060303,-6.733333110809326},{30.119999885559082,1.3300001430511474,-7.766666412353516}};
 public static void install(List<Socket> sockets){
  if(!HollowCreekScene.sourceSha().equals(SOURCE_SHA))throw new IllegalStateException("Regenerate actual native power supports");
  sockets.add(new Socket("hc-main-breaker","breaker",9,PANEL.x(),PANEL.y(),PANEL.z(),Math.PI/2,"HC-DOCK-01"));
  var b=VanInteriorLayout.bounds("button_12");
  sockets.add(new Socket("hc-generator","generator",9,STARTER_SUPPORT.x()-.15*(b.x0()+b.x1()),STARTER_SUPPORT.y()-.15*(b.y0()+b.y1()),STARTER_SUPPORT.z()-.30*b.z0(),0,"diesel-source-starter"));
  int i=0;for(var p:LAMPS)sockets.add(new Socket("hc-dock-light-"+i++,"light",9,p[0],p[1],p[2],0,"native-string"));
 }
 private HollowCreekPower(){}
}
