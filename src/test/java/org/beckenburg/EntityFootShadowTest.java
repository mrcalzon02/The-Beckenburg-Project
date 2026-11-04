package org.beckenburg;
import java.util.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
public final class EntityFootShadowTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static double[] floor(int w,int h,double focal,double pitch,double elevation){
  double[] depth=new double[w*h];Arrays.fill(depth,Double.POSITIVE_INFINITY);
  for(int y=0;y<h;y++){double ray=((h*.5-y-.5)/focal)*Math.cos(pitch)+Math.sin(pitch);
   double d=(elevation-1.62)/ray;
   if(d>.065)Arrays.fill(depth,y*w,(y+1)*w,d);
  }return depth;
 }
 public static void main(String[] args)throws Exception{
  for(double pitch:new double[]{0,-.35,.2})for(int w:new int[]{320,854}){
   int h=w==320?200:480;double focal=153.0*h/200;
   int[] pixels=new int[w*h];Arrays.fill(pixels,0x999999);double[] depth=floor(w,h,focal,pitch,0),before=depth.clone();
   EntityFootShadow.draw(pixels,depth,0,0,0,pitch,0,4,0,.42,145,w,h,focal);
   long dark=Arrays.stream(pixels).filter(c->c!=0x999999).count();check(dark>4,"projected shadow on floor at pitch "+pitch);
   check(Arrays.equals(before,depth),"shadow does not modify entity/world depth");
   check(Arrays.stream(pixels).allMatch(c->c>=0x414141&&c<=0x999999),"soft black darkening bounded by requested opacity");
   Arrays.fill(pixels,0x999999);Arrays.fill(depth,1);
   EntityFootShadow.draw(pixels,depth,0,0,0,pitch,0,4,0,.42,145,w,h,focal);
   check(Arrays.stream(pixels).allMatch(c->c==0x999999),"foreground wall fully occludes shadow");
   Arrays.fill(depth,Double.POSITIVE_INFINITY);
   EntityFootShadow.draw(pixels,depth,0,0,0,pitch,0,4,0,.42,145,w,h,focal);
   check(Arrays.stream(pixels).allMatch(c->c==0x999999),"no shadow painted into sky or through floor holes");
  }
  int w=640,h=400;double focal=306;double[] depth=floor(w,h,focal,-.12,0);int[] pixels=new int[w*h];Arrays.fill(pixels,0x82786a);
  var r=CastleRelations.Resident.ELSBETH_VALE;
  DirectionalSprites.drawResident(pixels,depth,0,0,0,-.12,r,new CastleResidentAnimation.Body(0,4,180,HunterSpriteAnimation.Pose.IDLE),0,true,w,h,focal);
  var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);image.setRGB(0,0,w,h,pixels,0,w);
  Path out=Path.of("development/quality/evidence/resident-animation-2026-11-03/foot-shadow-raster.png");Files.createDirectories(out.getParent());ImageIO.write(image,"PNG",out.toFile());
  System.out.println("PASS circular foot shadows: floor projection, pitch, resolution, opacity, depth preservation, wall and sky rejection");
 }
}
