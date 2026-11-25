package org.beckenburg;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
/** Real house scene, actor pose API and normal sprite/depth compositing. */
public final class HunterReactionsRenderReview {
 public static void main(String[] args)throws Exception{
  boolean gpu=List.of(args).contains("--gpu");java.lang.reflect.Method stop=null;
  if(gpu){var launcher=Class.forName("org.beckenburg.gpu.GpuDesktop");var start=launcher.getDeclaredMethod("startGpu");start.setAccessible(true);start.invoke(null);stop=launcher.getDeclaredMethod("stopReviewGpu");stop.setAccessible(true);}
  try{review(gpu);}finally{if(stop!=null)stop.invoke(null);}
 }
 static void review(boolean gpu)throws Exception{
  Path out=Path.of("development/levels/previews/hunter-reactions-2026-11-07/"+(gpu?"runtime-gpu":"runtime"));Files.createDirectories(out);
  var map=new HouseMap();double ax=0,az=0,cx=0,cz=0,floor=0;boolean found=false;
  outer:for(int room=0;room<HouseMap.INDOOR_ROOMS;room++){
   var r=map.level().room(room);
   for(double x=r.x0()+1;x<r.x1()-1;x+=.5)for(double z=r.z0()+1;z<r.z1()-1;z+=.5){
    boolean free=true;for(double dx:new double[]{-.95,.95})for(double dz:new double[]{-.5,.5})if(!map.traversable(x+dx,r.y(),z+dz))free=false;
    if(!free)continue;
    for(double angle:new double[]{0,Math.PI/2,Math.PI,Math.PI*1.5}){
     double cameraX=x+Math.sin(angle)*2.4,cameraZ=z+Math.cos(angle)*2.4;
     boolean sight=map.traversable(cameraX,r.y(),cameraZ);
     for(double ox:new double[]{-.65,.65})for(double oz:new double[]{-.65,.65})for(double oy:new double[]{.12,1.7})
      if(!map.clearLineOfSight(cameraX,r.y()+1.62,cameraZ,x+ox,r.y()+oy,z+oz))sight=false;
     if(sight){
      ax=x;az=z;cx=cameraX;cz=cameraZ;floor=r.y();found=true;break outer;
     }
    }
   }
  }
  if(!found)throw new AssertionError("No grounded actor/camera fixture");
  var scene=new SceneRenderer();scene.setRenderSize(854,480);scene.x=cx;scene.z=cz;scene.eyeHeight=floor+1.62;scene.yaw=Math.atan2(ax-cx,az-cz);scene.pitch=-.32;scene.setPsxPreset(PsxPipeline.Preset.OFF);
  int count=0;
  for(String id:CharacterVisuals.HUNTERS){
   var body=new SceneRenderer.RemoteBody(ax,floor,az,0,Math.floorMod((int)Math.toDegrees(scene.yaw)+180,360),id,false);
   scene.setHunterReactions(Set.of(),Map.of(0,1.0));scene.drawNetwork(map,List.of(body),(SceneRenderer.Apparition)null,1.15);
   ImageIO.write(scene.image(),"PNG",out.resolve(id+"-pain-runtime.png").toFile());
   scene.setHunterReactions(Set.of(0),Map.of());scene.drawNetwork(map,List.of(body),(SceneRenderer.Apparition)null,1.3);
   ImageIO.write(scene.image(),"PNG",out.resolve(id+"-body-runtime.png").toFile());count+=2;
   if(gpu&&!scene.frameDiagnostics().gpu())throw new AssertionError("Actual GPU scene required");
  }
  System.out.println("PASS "+count+" real "+(gpu?"OpenGL":"software")+" hunter pain/body scene captures at actual floor "+floor);
 }
}
