import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.io.*;
import java.util.zip.*;
import javax.imageio.ImageIO;
/** Crop generated reaction boards into packaged transparent game sprites. */
public class BuildHunterReactions {
 static final String[] IDS={"cleric","observer","demonologist"},VIEWS={"front","side","back","three_quarter"};
 static BufferedImage cut(BufferedImage im){
  int x0=im.getWidth(),x1=-1,y0=im.getHeight(),y1=-1;
  for(int y=0;y<im.getHeight();y++)for(int x=0;x<im.getWidth();x++)if((im.getRGB(x,y)>>>24)>=64){x0=Math.min(x0,x);x1=Math.max(x1,x);y0=Math.min(y0,y);y1=Math.max(y1,y);}
  if(x1<0||x0==0||y0==0||x1==im.getWidth()-1||y1==im.getHeight()-1)throw new IllegalStateException("Empty or cropped reaction cell "+x0+","+y0+".."+x1+","+y1);
  return im.getSubimage(x0,y0,x1-x0+1,y1-y0+1);
 }
 static int gap(BufferedImage im){
  int start=-1,bestStart=-1,bestEnd=-1,low=im.getHeight()*42/100,high=im.getHeight()*64/100;
  for(int y=low;y<=high;y++){
   int count=0;for(int x=0;x<im.getWidth();x++)if((im.getRGB(x,y)>>>24)>=64)count++;
   if(count<4){if(start<0)start=y;}else{if(start>=0&&y-start>bestEnd-bestStart){bestStart=start;bestEnd=y;}start=-1;}
  }
  if(start>=0&&high+1-start>bestEnd-bestStart){bestStart=start;bestEnd=high+1;}
  if(bestStart<0)throw new IllegalStateException("Missing transparent band gutter");return (bestStart+bestEnd)/2;
 }
 static BufferedImage fit(BufferedImage cut,boolean body){
  int w=body?384:128,h=body?192:224;double scale=Math.min((w-16.0)/cut.getWidth(),(h-16.0)/cut.getHeight());
  int sw=(int)Math.round(cut.getWidth()*scale),sh=body?(int)Math.round(cut.getHeight()*scale):210;
  var out=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);var g=out.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(cut,(w-sw)/2,body?(h-sh)/2:220-sh,sw,sh,null);g.dispose();return out;
 }
 public static void main(String[] args)throws Exception{
  Path art=Path.of("art/hunters/reactions-v1"),runtime=Path.of("assets/runtime");
  var preview=new BufferedImage(640,3*448,BufferedImage.TYPE_INT_RGB);var pg=preview.createGraphics();pg.setColor(new Color(35,39,46));pg.fillRect(0,0,640,preview.getHeight());int count=0;
  var manifest=new StringBuilder("{\n  \"format\":\"hunter-reactions-v1\",\n  \"generator\":\"built-in image_gen\",\n  \"frames\":[\n");
  try(var zip=new ZipOutputStream(Files.newOutputStream(runtime.resolve("hunter-reactions.zip")))){
   for(int i=0;i<IDS.length;i++){
    String source=IDS[i]+(i==2?"-alpha-v2":"")+".png";var sheet=ImageIO.read(art.resolve(source).toFile());int split=gap(sheet);
    for(int cell=0;cell<5;cell++){
     boolean body=cell==4;var crop=body?sheet.getSubimage(0,split,sheet.getWidth(),sheet.getHeight()-split):sheet.getSubimage(cell*sheet.getWidth()/4,0,sheet.getWidth()/4,split);
     var frame=fit(cut(crop),body);String name=IDS[i]+"/"+(body?"body":"pain/"+VIEWS[cell])+".png";
     var bytes=new ByteArrayOutputStream();ImageIO.write(frame,"PNG",bytes);var entry=new ZipEntry(name);entry.setTime(1794009600000L);zip.putNextEntry(entry);zip.write(bytes.toByteArray());zip.closeEntry();
     Path target=runtime.resolve("hunter-reactions").resolve(name);Files.createDirectories(target.getParent());Files.write(target,bytes.toByteArray());
     if(count++>0)manifest.append(",\n");manifest.append("    {\"entry\":\"").append(name).append("\",\"sha256\":\"").append(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()))).append("\",\"source\":\"art/hunters/reactions-v1/").append(source).append("\",\"source_sha256\":\"").append(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(art.resolve(source))))).append("\"}");
     if(body)pg.drawImage(frame,128,i*448+244,null);else pg.drawImage(frame,cell*160+16,i*448,null);
    }
    pg.setColor(Color.WHITE);pg.drawString(IDS[i]+" / pain in four directions; overhead fallen body",8,i*448+238);
   }
  }
  pg.dispose();ImageIO.write(preview,"PNG",art.resolve("preview.png").toFile());Files.writeString(runtime.resolve("hunter-reactions-manifest.json"),manifest+"\n  ]\n}\n");System.out.println("Built "+count+" hunter pain/body sprites");
 }
}
