package org.beckenburg;
import java.util.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
public final class HunterReactionSpritesTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static double[] floor(int w,int h,double focal,double pitch){double[] d=new double[w*h];Arrays.fill(d,Double.POSITIVE_INFINITY);for(int y=0;y<h;y++){double ray=(h*.5-y-.5)/focal*Math.cos(pitch)+Math.sin(pitch),z=-1.62/ray;if(z>.065)Arrays.fill(d,y*w,(y+1)*w,z);}return d;}
 public static void main(String[] args)throws Exception{
  Path out=Path.of("development/levels/previews/hunter-reactions-2026-11-07/raster-oval");Files.createDirectories(out);
  int w=640,h=400;double focal=306,pitch=-.25;
  for(String id:CharacterVisuals.HUNTERS){
   for(int face=0;face<8;face++){
    var image=HunterReactionSprites.pain(id,face);check(image.getWidth()==128&&image.getHeight()==224,"pain frame size");
    check(!Arrays.equals(image.getRGB(0,0,128,224,null,0,128),HunterSpriteAnimation.frame(id,face,HunterSpriteAnimation.Pose.IDLE).getRGB(0,0,128,224,null,0,128)),"distinct authored pain "+id);
    for(int x=0;x<128;x++)check((image.getRGB(x,223)>>>24)==0,"pain boots have clear margin");
   }
   check(HunterReactionSprites.body(id).getWidth()==384,"wide overhead body texture");
   for(int heading:new int[]{0,90,180,270}){
    int[] pixels=new int[w*h];Arrays.fill(pixels,0x898174);double[] depth=floor(w,h,focal,pitch),floorDepth=depth.clone();
    var body=new SceneRenderer.RemoteBody(0,0,4,0,heading,id,false);
    HunterReactionSprites.drawBody(pixels,depth,0,0,0,pitch,body,0,true,w,h,focal);
    check(Arrays.stream(pixels).filter(c->c!=0x898174).count()>100,"oval body projects at heading "+heading+" "+id);
    int raised=0;double low=1,high=-1;
    for(int k=0;k<depth.length;k++)if(Double.isFinite(depth[k])&&depth[k]<floorDepth[k]-.03){
     raised++;double up=(h*.5-(k/w)-.5)/focal;
     double bodyHeight=1.62+(up*Math.cos(pitch)+Math.sin(pitch))*depth[k];
     low=Math.min(low,bodyHeight);high=Math.max(high,bodyHeight);
    }
    check(raised>30,"body must have visible rounded volume above floor, not a flat card: "+id+" "+heading);
    check(high-low>.04&&high<=.301&&low>=-.003,"curved body surface needs real varying height and a grounded base: "+id+" "+heading);
    var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);image.setRGB(0,0,w,h,pixels,0,w);ImageIO.write(image,"PNG",out.resolve(id+"-body-"+heading+"-raster.png").toFile());
    Arrays.fill(pixels,0x898174);Arrays.fill(depth,1);
    HunterReactionSprites.drawBody(pixels,depth,0,0,0,pitch,body,0,true,w,h,focal);
    check(Arrays.stream(pixels).allMatch(c->c==0x898174),"wall hides body and shadow");
    Arrays.fill(depth,Double.POSITIVE_INFINITY);
    HunterReactionSprites.drawBody(pixels,depth,0,0,0,pitch,body,0,true,w,h,focal);
    check(Arrays.stream(pixels).anyMatch(c->c!=0x898174),"oval body is real volume independent of a floor decal receiver");
   }
   int[] pixels=new int[w*h];double[] depth=new double[w*h];Arrays.fill(depth,Double.POSITIVE_INFINITY);
   DirectionalSprites.drawHunter(pixels,depth,0,0,0,pitch,new SceneRenderer.RemoteBody(0,0,4,0,180,id,false),HunterSpriteAnimation.Pose.IDLE,0,true,w,h,focal,true);
   check(Arrays.stream(pixels).anyMatch(c->c!=0),"pain pose uses actual sprite raster");
  }
  var renderer=new SceneRenderer();renderer.setHunterReactions(Set.of(),Map.of(5,2.0));
  check(!renderer.hunterPainVisible(5,1.9)&&renderer.hunterPainVisible(5,2.2)&&!renderer.hunterPainVisible(5,2.5),"pain only during accepted event window");
  renderer.setHunterReactions(Set.of(5),Map.of(5,2.0));check(!renderer.hunterPainVisible(5,2.2),"downed body overrides pain");
  renderer.setHunterReactions(Set.of(),Map.of());check(!renderer.hunterPainVisible(5,2.2),"new scene/departure resets reaction");
  System.out.println("PASS three hunters: directional pain, rounded oval bodies with raised depth at four headings, shadow/depth occlusion, curved surface, reaction expiry and down override");
 }
}
