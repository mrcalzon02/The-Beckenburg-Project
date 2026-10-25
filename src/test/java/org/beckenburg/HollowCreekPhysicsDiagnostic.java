package org.beckenburg;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
/** Actual packaged HCS2 query visualization, not a world-renderer capture. */
public final class HollowCreekPhysicsDiagnostic {
 private static int px(double x){return (int)Math.round(70+x*10);}
 private static int pz(double z){return (int)Math.round(900-z*10);}
 public static void main(String[] args)throws Exception{
  var map=AuthoredSitePhysics.hollowCreek();var image=new BufferedImage(1370,1120,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(new Color(19,24,29));g.fillRect(0,0,1370,1120);
  for(int i=0;i<map.triangleCount();i++){
   var t=map.triangle(i);if((t.flags()&AuthoredSitePhysics.SUPPORT)==0)continue;
   double y=(t.a().y()+t.b().y()+t.c().y())/3;float h=(float)Math.max(0,Math.min(1,(y+2)/6));g.setColor(new Color(.17f+h*.24f,.25f+h*.27f,.21f+h*.12f));
   g.fillPolygon(new int[]{px(t.a().x()),px(t.b().x()),px(t.c().x())},new int[]{pz(t.a().z()),pz(t.b().z()),pz(t.c().z())},3);
  }
  g.setColor(new Color(156,165,158,65));
  for(int i=0;i<map.triangleCount();i++){
   var t=map.triangle(i);if((t.flags()&AuthoredSitePhysics.BODY)==0||(t.flags()&AuthoredSitePhysics.SUPPORT)!=0)continue;
   g.drawPolygon(new int[]{px(t.a().x()),px(t.b().x()),px(t.c().x())},new int[]{pz(t.a().z()),pz(t.b().z()),pz(t.c().z())},3);
  }
  g.setStroke(new BasicStroke(2));g.setColor(new Color(239,117,96));
  for(var b:HollowCreekContract.blockers()){
   int[] x=new int[4],z=new int[4];double cs=Math.cos(b.yaw()),sn=Math.sin(b.yaw());for(int i=0;i<4;i++){double u=(i==0||i==3?-1:1)*b.halfX(),v=(i<2?-1:1)*b.halfZ();x[i]=px(b.x()+u*cs-v*sn);z[i]=pz(b.z()+u*sn+v*cs);}g.drawPolygon(x,z,4);
  }
  g.setColor(new Color(101,220,213));
  for(String line:Files.readAllLines(Path.of("development/levels/verification/hollow-creek-native-queries.tsv"))){var p=line.split("\t");if(p[0].equals("S")&&p[4].startsWith("L")){int x=px(Double.parseDouble(p[1])),z=pz(Double.parseDouble(p[2]));g.fillOval(x-2,z-2,4,4);}}
  g.setColor(new Color(255,221,120));g.setStroke(new BasicStroke(3));g.drawOval(px(55)-18,pz(14.6)-18,36,36);g.setFont(new Font("SansSerif",Font.BOLD,16));g.drawString("L14",px(55)+22,pz(14.6));
  g.setColor(new Color(19,24,29));g.fillRect(935,0,435,1120);g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.BOLD,22));g.drawString("HOLLOW CREEK",963,70);g.setFont(new Font("SansSerif",Font.PLAIN,17));
  String[] lines={"Runtime geometry component diagnostic","25 October 2026","","351,179 source triangles / 7,070 parts","250 movement-only water barriers","","Green: native support surfaces","Grey: physical mesh edges","Coral: movement-only boundaries","Cyan: 969 standing route probes","Gold: local L14 grade correction","","3 terrain heights adjusted <=12.54mm","18 protected faces <=19.01% grade","20% runtime grade cap unchanged","","1,041 support probes + 346 optical rays","939 nonzero route sweeps","12 authored zone anchors","","Not a world-renderer/gameplay capture.","Mission remains locked.","Van elevation, renderer, AI, circuits","and host/LAN integration still required."};
  int y=112;for(String line:lines){g.drawString(line,963,y);y+=28;}g.dispose();Path out=Path.of("development/levels/previews/hollow-creek/native-physics-query-diagnostic.png");Files.createDirectories(out.getParent());ImageIO.write(image,"PNG",out.toFile());System.out.println("WROTE actual HCS2 diagnostic "+out);
 }
}
