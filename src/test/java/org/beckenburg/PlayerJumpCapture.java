package org.beckenburg;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
/** Raster review fixture using the same authored hunter sprite path as SceneRenderer. */
public final class PlayerJumpCapture {
 public static void main(String[] args)throws Exception{
  int w=240,h=260;var sheet=new BufferedImage(w*5,h,BufferedImage.TYPE_INT_RGB);var g=sheet.createGraphics();
  double[] feet={0,.73,1.34,0,0};boolean[] low={false,true,true,true,false};
  String[] captions={"Standing","Takeoff tuck","Airborne apex","Landing crouch","Standing again"};
  for(int n=0;n<5;n++){
   int[] pixels=new int[w*h];double[] depths=new double[w*h];Arrays.fill(depths,Double.POSITIVE_INFINITY);
   for(int y=0;y<h;y++)Arrays.fill(pixels,y*w,(y+1)*w,y<182?0x29333c:0x596267);
   var body=new SceneRenderer.RemoteBody(0,feet[n],4,0,180,"observer",low[n]);
   DirectionalSprites.drawHunter(pixels,depths,0,0,0,0,body,low[n]?HunterSpriteAnimation.Pose.CROUCH:HunterSpriteAnimation.Pose.IDLE,feet[n],true,w,h,200);
   var frame=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);frame.setRGB(0,0,w,h,pixels,0,w);g.drawImage(frame,n*w,0,null);
   g.setColor(Color.WHITE);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,15));g.drawString(captions[n],n*w+12,24);
  }
  g.dispose();Path output=Path.of(args[0]);Files.createDirectories(output.getParent());ImageIO.write(sheet,"png",output.toFile());
 }
}
