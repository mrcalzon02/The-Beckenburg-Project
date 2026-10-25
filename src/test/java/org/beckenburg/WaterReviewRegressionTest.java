package org.beckenburg;
import java.util.*;
import dev.mrcalzon.water.*;
public final class WaterReviewRegressionTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static int transmission(WaterModule.Style style,double depth){var area=new WaterModule.Polygon(List.of(new WaterModule.Point(-5,-5),new WaterModule.Point(5,-5),new WaterModule.Point(5,5),new WaterModule.Point(-5,5)));var tex=new WaterModule.Texture(2,2,new int[]{0,0,0,0});var b=new WaterModule.Body("optical test",area,0,(x,z)->-depth,tex,style,List.of());int[] pixels=new int[9];Arrays.fill(pixels,0xffffff);double[] d=new double[9];Arrays.fill(d,Double.POSITIVE_INFINITY);new WaterModule(List.of(b)).composite(new WaterModule.Camera(0,2,0,0,-Math.PI/4,2,3,3),pixels,d,0,(rgb,x,y,z)->0);return pixels[4]&255;}
 public static void main(String[] args)throws Exception{
  int[] rgb={0xff0000,0xff0000,0xff0000,0x00ff00,0x00ff00,0x00ff00,0x0000ff,0x0000ff,0x0000ff};double[] depth={2,2,2,100,100,100,100,100,100};DistanceDetail.apply(rgb,depth,3,3,0x18202a);check(rgb[0]==0xff0000&&rgb[1]==0xff0000,"Near detail must remain unchanged");check(((rgb[4]>>16)&255)<=24,"Distance filter must reject foreground red samples");check(rgb[4]!=0x00ff00,"Distant detail is filtered and hazed");
  int[] far={0xffffff};DistanceDetail.apply(far,new double[]{180},1,1,0x18202a);check(Math.abs(((far[0]>>16)&255)-24)<=1,"Far water must converge to actual horizon colour before clipping");
  var body=HollowCreekWater.module().bodies().get(0);var effect=body.effects();check(effect.shoreline()!=null&&effect.envelope()<.11,"Shore waves fit the complete authored dry hull profile");var shore=effect.shoreline();double maximum=0;
  for(double t=0;t<5.5;t+=.1)maximum=Math.max(maximum,Math.abs(shore.height(34,1,t)));check(maximum>.001,"Shore run-up actually displaces water");
  int near=transmission(body.style(),.1),deep=transmission(body.style(),1),old=transmission(new WaterModule.Style(0,0,.65,.22,.92,3.2,.018),.1);
  check(near<old-40&&near>50,"Revised water must obscure more bed detail while retaining shallow transparency");check(deep<near*.4,"Bed visibility must fall strongly with depth");
  var model=HollowCreekScene.model(HollowCreekScene.instances().stream().filter(i->i.id().equals("level_hc_native_361_0")).findFirst().orElseThrow());int samples=0;
  for(var triangle:model.triangles())for(var v:List.of(triangle.a(),triangle.b(),triangle.c())){double length=Math.sqrt(v.nx()*v.nx()+v.ny()*v.ny()+v.nz()*v.nz());check(Math.abs(length-1)<1e-6,"Boat's source shading normals must survive loading");samples++;check(GlbProps.textureWidth(triangle.material())==256,"Refined source textures must retain their authored resolution");}
  var map=new HouseMap(HouseMap.Site.HOLLOW_CREEK);var lights=new RoomLighting();var field=new FixtureLighting(map);field.frame(lights,0);var fixture=map.level().sockets().stream().filter(s->s.type().equals("light")).findFirst().orElseThrow();double up=field.sampleNormal(fixture.room(),fixture.x(),fixture.y()-.3,fixture.z(),0,1,0).energy(),down=field.sampleNormal(fixture.room(),fixture.x(),fixture.y()-.3,fixture.z(),0,-1,0).energy();check(up>down,"Audited boat shading must respond to light direction");
  System.out.println("PASS distance sharpness/foreground isolation/horizon convergence; slow shore displacement and hull envelope; "+samples+" unit boat normals and native texture resolution; directional fixture response");
 }
}
