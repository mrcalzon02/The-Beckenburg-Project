import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.zip.*;
import javax.imageio.ImageIO;
import java.io.*;
/** Deterministic grid extraction only; preserves generated alpha and source art. */
public class BuildCastleResidentAnimation {
 static final String[] IDS={"lore_master","elsbeth_vale","rowan_pike","tomas_greaves","sister_mirelle_ash"};
 static final String[] POSES={"walk_a","walk_b","crouch"};
 static BufferedImage cut(BufferedImage cell){
  int x0=cell.getWidth(),x1=-1,y0=cell.getHeight(),y1=-1;
  for(int y=0;y<cell.getHeight();y++)for(int x=0;x<cell.getWidth();x++)if((cell.getRGB(x,y)>>>24)>=64){x0=Math.min(x0,x);x1=Math.max(x1,x);y0=Math.min(y0,y);y1=Math.max(y1,y);}
  if(x1<x0)throw new IllegalStateException("Empty generated cell");
  if(x0==0||y0==0||x1==cell.getWidth()-1||y1==cell.getHeight()-1)throw new IllegalStateException("Silhouette touches cell edge: "+x0+","+y0+" -> "+x1+","+y1+" cell="+cell.getWidth()+"x"+cell.getHeight());
  return cell.getSubimage(x0,y0,x1-x0+1,y1-y0+1);
 }
 static int gap(BufferedImage sheet,int low,int high){
  int bestStart=-1,bestEnd=-1,start=-1;
  for(int y=low;y<=high;y++){
   int count=0;for(int x=0;x<sheet.getWidth();x++)if((sheet.getRGB(x,y)>>>24)>=64)count++;
   if(count<=3){if(start<0)start=y;}else{if(start>=0&&y-start>bestEnd-bestStart){bestStart=start;bestEnd=y;}start=-1;}
  }
  if(start>=0&&high+1-start>bestEnd-bestStart){bestStart=start;bestEnd=high+1;}
  if(bestStart<0)throw new IllegalStateException("No transparent row gutter");
  return (bestStart+bestEnd)/2;
 }
 public static void main(String[] args)throws Exception{
  Path root=Path.of("."); Path art=root.resolve("art/castle-residents/animation-v1");
  BufferedImage preview=new BufferedImage(640,15*248,BufferedImage.TYPE_INT_RGB);var pg=preview.createGraphics();pg.setColor(new Color(35,39,46));pg.fillRect(0,0,preview.getWidth(),preview.getHeight());
  StringBuilder manifest=new StringBuilder("{\n  \"format\": \"castle-resident-animation-v1\",\n  \"generator\": \"built-in image_gen\",\n  \"size\": [128,224],\n  \"frames\": [\n");int count=0;
  try(var zip=new ZipOutputStream(Files.newOutputStream(root.resolve("assets/runtime/castle-resident-animation.zip")))){
   for(int id=0;id<IDS.length;id++){
    var sheet=ImageIO.read(art.resolve(IDS[id]+".png").toFile());var cells=new BufferedImage[12];int maxH=0,maxW=0;
    int[] rows={0,gap(sheet,sheet.getHeight()*28/100,sheet.getHeight()*40/100),gap(sheet,sheet.getHeight()*62/100,sheet.getHeight()*76/100),sheet.getHeight()};
    for(int row=0;row<3;row++)for(int col=0;col<4;col++){
     int x=col*sheet.getWidth()/4,y=rows[row];
     var cell=cut(sheet.getSubimage(x,y,(col+1)*sheet.getWidth()/4-x,rows[row+1]-y));cells[row*4+col]=cell;
     if(row<2)maxH=Math.max(maxH,cell.getHeight());maxW=Math.max(maxW,cell.getWidth());
    }
    double scale=Math.min(210.0/maxH,116.0/maxW);
    for(int row=0;row<3;row++)for(int col=0;col<4;col++){
     var cell=cells[row*4+col];int w=Math.max(1,(int)Math.round(cell.getWidth()*scale)),h=210;
     var frame=new BufferedImage(128,224,BufferedImage.TYPE_INT_ARGB);var g=frame.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(cell,(128-w)/2,220-h,w,h,null);g.dispose();
     String view=new String[]{"front","side","back",id==0?"right":"three_quarter"}[col];String entry=IDS[id]+"/"+POSES[row]+"/"+view+".png";
     var bytes=new ByteArrayOutputStream();ImageIO.write(frame,"PNG",bytes);var ze=new ZipEntry(entry);ze.setTime(1793664000000L);zip.putNextEntry(ze);zip.write(bytes.toByteArray());zip.closeEntry();
     Path target=root.resolve("assets/runtime/castle-resident-animation").resolve(entry);Files.createDirectories(target.getParent());Files.write(target,bytes.toByteArray());
     if(count++>0)manifest.append(",\n");manifest.append("    {\"entry\":\"").append(entry).append("\",\"sha256\":\"").append(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()))).append("\",\"source\":\"art/castle-residents/animation-v1/").append(IDS[id]).append(".png\"}");
     int py=(id*3+row)*248,dh=row==2?132:224;pg.drawImage(frame,col*160+16,py+224-dh,128,dh,null);pg.setColor(Color.WHITE);pg.drawString(IDS[id]+" "+POSES[row],col*160+3,py+240);
    }
   }
  }
  pg.dispose();ImageIO.write(preview,"PNG",art.resolve("preview.png").toFile());Files.writeString(root.resolve("assets/runtime/castle-resident-animation-manifest.json"),manifest+"\n  ]\n}\n");System.out.println("Built "+count+" transparent resident animation frames");
 }
}
