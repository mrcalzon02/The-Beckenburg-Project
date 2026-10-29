package org.beckenburg.gpu;

import org.beckenburg.GpuWorldRenderer;
import org.beckenburg.MaterialAtlas;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/** Synthetic depth controls for the actual production flashlight shader. */
final class HandheldDepthReview {
 private HandheldDepthReview(){}
 private static final Path OUTPUT=Path.of(System.getProperty("beckenburg.handheld.depth.output","build/e2e/gpu/handheld"));
 private static GpuWorldRenderer.Surface panel(float half,float z,int material){
  float[] vertices=new float[48];float[][] points={{-half,-half},{half,-half},{half,half},{-half,-half},{half,half},{-half,half}};
  for(int i=0;i<6;i++){
   int at=i*8;vertices[at]=points[i][0];vertices[at+1]=points[i][1];vertices[at+2]=z;
   vertices[at+3]=points[i][0]>0?1:0;vertices[at+4]=points[i][1]>0?1:0;
   vertices[at+5]=vertices[at+6]=vertices[at+7]=.03f;
  }
  return new GpuWorldRenderer.Surface(material,vertices);
 }
 private static GpuWorldRenderer.Result draw(GpuWorldRenderer gpu,long scene,boolean on,List<GpuWorldRenderer.Surface> surfaces)throws Exception{
  long frame=GpuWorldRenderer.nextId();
  gpu.submit(new GpuWorldRenderer.Frame(scene,frame,320,200,153,0,0,surfaces,
      false,0,0,0,0,0,on,1,8,.8f,false));
  long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
  while(System.nanoTime()<deadline){var result=gpu.latest();
   if(result!=null&&result.sceneId()==scene&&result.frameId()==frame)return result;
   Thread.sleep(10);
  }
  throw new AssertionError("No fresh handheld depth-control framebuffer");
 }
 private static int energy(int argb){return ((argb>>16)&255)+((argb>>8)&255)+(argb&255);}
 private static void capture(GpuWorldRenderer.Result frame,String name)throws Exception{
  var image=new BufferedImage(frame.width(),frame.height(),BufferedImage.TYPE_INT_ARGB);
  image.setRGB(0,0,frame.width(),frame.height(),frame.argb(),0,frame.width());
  ImageIO.write(image,"png",OUTPUT.resolve(name+".png").toFile());
 }
 static void execute(GpuWorldRenderer gpu)throws Exception{
  Files.createDirectories(OUTPUT);long scene=GpuWorldRenderer.nextId();
  var near=panel(.45f,2,MaterialAtlas.PLASTER);var far=panel(3,4,MaterialAtlas.BRICK);
  var dark=draw(gpu,scene,false,List.of(far));var exposed=draw(gpu,scene,true,List.of(far));
  int center=100*320+160;
  if(energy(exposed.argb()[center])<=energy(dark.argb()[center])+30)throw new AssertionError("Unblocked flashlight positive control did not brighten");
  var foreground=draw(gpu,scene,true,List.of(near));int covered=0;
  for(var surfaces:List.of(List.of(near,far),List.of(far,near))){
   var blocked=draw(gpu,scene,true,surfaces);
   for(int i=0;i<foreground.depth().length;i++)if(Math.abs(foreground.depth()[i]-2)<.001){
    if(Math.abs(blocked.depth()[i]-2)>.001||blocked.argb()[i]!=foreground.argb()[i])
     throw new AssertionError("Hidden background changed the first-surface flashlight result at pixel "+i);
    covered++;
   }
   capture(blocked,surfaces.get(0)==near?"synthetic-near-first":"synthetic-far-first");
  }
  if(covered<1000)throw new AssertionError("Foreground occlusion control has too few pixels");
  var beyond=panel(8,12,MaterialAtlas.BRICK);
  var beyondOff=draw(gpu,scene,false,List.of(beyond));var beyondOn=draw(gpu,scene,true,List.of(beyond));
  if(!java.util.Arrays.equals(beyondOff.argb(),beyondOn.argb()))throw new AssertionError("Flashlight illuminated beyond its finite reach");
  capture(exposed,"synthetic-unblocked");capture(beyondOn,"synthetic-beyond-reach");
  String result="PASS production OpenGL flashlight: unblocked positive, "+covered+
      " first-surface pixel checks across both submission orders, exact depth, finite reach. Synthetic controls, not map acceptance.\n";
  Files.writeString(OUTPUT.resolve("result.txt"),result);System.out.print(result);
 }
}
