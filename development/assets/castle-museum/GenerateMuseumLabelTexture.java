import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
/** Original fictional collection label, painted into the low-count display atlas. */
public final class GenerateMuseumLabelTexture {
 public static void main(String[] args)throws Exception{
  var image=new BufferedImage(512,256,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
  for(int y=0;y<256;y++)for(int x=0;x<512;x++){
   int wear=(int)Math.round(3*Math.sin(x*.041)+2*Math.cos(y*.19));
   image.setRGB(x,y,new Color(73+wear,59+wear,39+wear).getRGB());
  }
  g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
  g.setColor(new Color(151,128,80));g.drawRect(8,8,495,239);g.drawRect(12,12,487,231);
  g.setColor(new Color(234,225,192));g.setFont(new Font("Serif",Font.BOLD,34));g.drawString("FIELD HARNESS",30,52);
  g.setFont(new Font("SansSerif",Font.PLAIN,18));g.drawString("COMPOSITE DISPLAY  /  COLLECTION 02",30,83);
  g.setColor(new Color(174,153,105));g.drawLine(30,99,480,99);
  g.setColor(new Color(231,219,185));g.setFont(new Font("Serif",Font.PLAIN,21));
  String[] lines={"Lost fittings were replaced during", "the west-wing restoration. The old", "museum collection remains in the", "care of the present custodians."};
  for(int i=0;i<lines.length;i++)g.drawString(lines[i],30,130+i*25);
  g.setFont(new Font("SansSerif",Font.PLAIN,13));g.setColor(new Color(183,160,105));g.drawString("BECKENBURG MUSEUM  /  ARCHIVE GALLERY",30,237);g.dispose();
  var folder=Path.of("development/assets/castle-museum/textures");Files.createDirectories(folder);ImageIO.write(image,"png",folder.resolve("armour-exhibit-label.png").toFile());
 }
}
