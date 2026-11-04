package org.beckenburg;
import java.io.*;
import javax.imageio.ImageIO;
/** Actual exported wall-light triangles must sample their original atlas pixels. */
public final class WallLampSourceUvTest {
 public static void main(String[] args)throws Exception{
  int samples=0,distinguished=0;
  for(String name:new String[]{"lamp_3_on","lamp_3_off"}){
   byte[] bytes;try(var in=WallLampSourceUvTest.class.getResourceAsStream("/org/beckenburg/models/"+name+".glb")){bytes=in.readAllBytes();}
   var source=org.beckenburg.editor.EditorGlbReader.read(bytes);
   if(source.materials().size()!=1)throw new AssertionError("Review wall-light material split");
   var image=ImageIO.read(new ByteArrayInputStream(source.materials().get(0).png()));
   for(var triangle:GlbProps.model(name).triangles()){
    double u=(triangle.a().u()+triangle.b().u()+triangle.c().u())/3,v=(triangle.a().v()+triangle.b().v()+triangle.c().v())/3;
    u-=Math.floor(u);v-=Math.floor(v);
    int x=Math.min(image.getWidth()-1,(int)(u*image.getWidth())),y=Math.min(image.getHeight()-1,(int)(v*image.getHeight()));
    int expected=image.getRGB(x,y)&0xffffff,actual=GlbProps.sample(triangle.material(),u,v)&0xffffff;
    if(actual!=expected)throw new AssertionError(name+" source atlas mismatch at UV "+u+","+v+": expected="+Integer.toHexString(expected)+" actual="+Integer.toHexString(actual));
    if(expected!=(image.getRGB(x,Math.min(image.getHeight()-1,(int)((1-v)*image.getHeight())))&0xffffff))distinguished++;
    samples++;
   }
  }
  if(distinguished<20)throw new AssertionError("Source samples cannot distinguish upside-down mapping");
  System.out.println("PASS wall-light on/off source atlas: "+samples+" actual triangle centroids, "+distinguished+" distinguish inverted V; original meshes/images unchanged");
 }
}
