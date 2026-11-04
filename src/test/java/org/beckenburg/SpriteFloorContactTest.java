package org.beckenburg;
import java.util.*;
public final class SpriteFloorContactTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  int w=640,h=400;double focal=306;
  for(double pitch:new double[]{-.5,-.2,0,.2})for(double eye:new double[]{1.05,1.62,2.05})
   for(String id:new String[]{"elsbeth_vale","rowan_pike","tomas_greaves","sister_mirelle_ash","observer","lore_master"}){
    int[] open=new int[w*h],floor=new int[w*h];double[] a=new double[w*h],b=new double[w*h];Arrays.fill(a,Double.POSITIVE_INFINITY);Arrays.fill(b,Double.POSITIVE_INFINITY);
    for(int y=0;y<h;y++){
     double dy=((h*.5-y-.5)/focal)*Math.cos(pitch)+Math.sin(pitch),d=-eye/dy;
     if(d>.065)Arrays.fill(b,y*w,(y+1)*w,d);
    }
    DirectionalSprites.draw(open,a,0,0,0,pitch,0,3,180,id,1.79,.98,1.62-eye,255,true,w,h,focal);
    DirectionalSprites.draw(floor,b,0,0,0,pitch,0,3,180,id,1.79,.98,1.62-eye,255,true,w,h,focal);
    int opaque=0,missing=0;
    for(int i=0;i<a.length;i++)if(Double.isFinite(a[i])){opaque++;if(floor[i]!=open[i])missing++;}
    check(opaque>200,"test character visible");
    check(missing<=Math.max(2,opaque/250),"floor clips opaque body pixels: "+id+" pitch="+pitch+" eye="+eye+" missing="+missing+"/"+opaque);
   }
  for(var resident:CastleRelations.Resident.values())for(int facing:new int[]{0,2,4,6}){
   var image=CastleResidentAnimation.frame(resident,facing,HunterSpriteAnimation.Pose.IDLE);
   for(int x=0;x<image.getWidth();x++)for(int y=image.getHeight()-5;y<image.getHeight();y++)
    check((image.getRGB(x,y)>>>24)==0,"complete soles need a transparent lower margin: "+resident+" facing="+facing);
  }
  System.out.println("PASS complete human shoe raster against real floor depth across standing/crouched camera heights and pitch");
 }
}
