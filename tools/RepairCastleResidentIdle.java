import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;
import java.io.*;
public class RepairCastleResidentIdle {
 static int gap(BufferedImage image,int low,int high){
  int start=-1,bestStart=-1,bestEnd=-1;
  for(int x=low;x<=high;x++){
   int count=0;for(int y=0;y<image.getHeight();y++)if((image.getRGB(x,y)>>>24)>=64)count++;
   if(count<=3){if(start<0)start=x;}else{if(start>=0&&x-start>bestEnd-bestStart){bestStart=start;bestEnd=x;}start=-1;}
  }
  if(start>=0&&high+1-start>bestEnd-bestStart){bestStart=start;bestEnd=high+1;}
  if(bestStart<0)throw new IllegalStateException("Missing transparent column gutter");return (bestStart+bestEnd)/2;
 }
 public static void main(String[] args)throws Exception{
  Path root=Path.of("."),art=root.resolve("art/castle-residents/animation-v1");
  for(String identity:new String[]{"elsbeth_vale","rowan_pike","tomas_greaves","sister_mirelle_ash","lore_master"}){
  boolean lore=identity.equals("lore_master");int size=lore?128:256,canvasH=lore?224:448;
  String file=identity.equals("elsbeth_vale")?"elsbeth-idle-v2.png":identity+"-idle-v2.png";
  var sheet=ImageIO.read(art.resolve(file).toFile());
  var updated=new LinkedHashMap<String,byte[]>();Path archive=root.resolve("assets/runtime/"+(lore?"lore-master-directions":"castle-support-directions")+".zip");
  try(var zip=new ZipInputStream(Files.newInputStream(archive))){ZipEntry entry;while((entry=zip.getNextEntry())!=null)updated.put(entry.getName(),zip.readAllBytes());}
  int[] columns={0,gap(sheet,sheet.getWidth()*18/100,sheet.getWidth()*32/100),gap(sheet,sheet.getWidth()*44/100,sheet.getWidth()*56/100),gap(sheet,sheet.getWidth()*69/100,sheet.getWidth()*81/100),sheet.getWidth()};
  for(int col=0;col<4;col++){
   int left=columns[col],right=columns[col+1],x0=right,x1=-1,y0=sheet.getHeight(),y1=-1;
   for(int y=0;y<sheet.getHeight();y++)for(int x=left;x<right;x++)if((sheet.getRGB(x,y)>>>24)>=64){x0=Math.min(x0,x);x1=Math.max(x1,x);y0=Math.min(y0,y);y1=Math.max(y1,y);}
   if(x0<=left||x1>=right-1||y0<=0||y1>=sheet.getHeight()-1)throw new IllegalStateException("Truncated idle cell "+col);
   var cut=sheet.getSubimage(x0,y0,x1-x0+1,y1-y0+1);double scale=Math.min((size-24.0)/cut.getWidth(),(canvasH-28.0)/cut.getHeight());int w=(int)Math.round(cut.getWidth()*scale),h=(int)Math.round(cut.getHeight()*scale);
   var frame=new BufferedImage(size,canvasH,BufferedImage.TYPE_INT_ARGB);var g=frame.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(cut,(size-w)/2,canvasH-10-h,w,h,null);g.dispose();
   String view=new String[]{"front","side","back","right"}[col];var bytes=new ByteArrayOutputStream();ImageIO.write(frame,"PNG",bytes);String entry=lore?updated.keySet().stream().filter(n->n.endsWith("/"+view+".png")).findFirst().orElseThrow():"assets/castle_support/"+identity+"/"+view+".png";updated.put(entry,bytes.toByteArray());ImageIO.write(frame,"PNG",art.resolve(identity+"-idle-"+view+".png").toFile());
  }
  try(var zip=new ZipOutputStream(Files.newOutputStream(archive))){for(var item:updated.entrySet()){var e=new ZipEntry(item.getKey());e.setTime(1793664000000L);zip.putNextEntry(e);zip.write(item.getValue());zip.closeEntry();}}
  System.out.println("Repaired four "+identity+" standing views with complete boots and transparent sole margins");
  }
 }
}
