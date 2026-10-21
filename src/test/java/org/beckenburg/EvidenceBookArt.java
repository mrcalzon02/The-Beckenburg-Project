package org.beckenburg;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Random;
import javax.imageio.ImageIO;

/** Original authored page artwork; independent of species and shared by every equipment tier. */
public final class EvidenceBookArt {
    static void stroke(Graphics2D g,Random random,double... xy){
        Path2D p=new Path2D.Double();p.moveTo(xy[0],xy[1]);
        for(int i=2;i<xy.length;i+=2)p.lineTo(xy[i]+random.nextDouble()*3-1.5,xy[i+1]+random.nextDouble()*3-1.5);g.draw(p);
    }
    static void ring(Graphics2D g,Random r,double x,double y,double rx,double ry){
        double[] pts=new double[130];for(int i=0;i<65;i++){double a=i*Math.PI/32;pts[i*2]=x+Math.cos(a)*(rx+r.nextDouble()*2);pts[i*2+1]=y+Math.sin(a)*(ry+r.nextDouble()*2);}stroke(g,r,pts);
    }
    static void person(Graphics2D g,Random r,int x,int y,int size){
        ring(g,r,x,y,size*.19,size*.21);stroke(g,r,x,y+size*.2,x-2,y+size*.65,x-size*.2,y+size,x-2,y+size*.65,x+size*.25,y+size);stroke(g,r,x-size*.29,y+size*.36,x,y+size*.31,x+size*.31,y+size*.43);
    }
    static BufferedImage image(int tier,String style){
        var im=new BufferedImage(1024,768,BufferedImage.TYPE_INT_RGB);var rng=new Random(8129+tier);var g=im.createGraphics();
        for(int y=0;y<768;y++)for(int x=0;x<1024;x++){
            double edge=Math.min(Math.min(x%512,511-x%512),Math.min(y,767-y));int grain=rng.nextInt(9)-4;int shade=(int)Math.max(0,13-edge*.25);
            int base=224+(tier-2)*4+grain-shade;im.setRGB(x,y,(base<<16)|((base-9)<<8)|(base-29));
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(4.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(new Color(130,109,78,90));
        for(int page=0;page<2;page++){g.drawRoundRect(page*512+25,25,461,715,4,4);for(int i=0;i<5;i++)g.drawLine(page*512+31,33+i*2,page*512+44,33+i*2);}
        g.setColor(new Color(70,52,39));rng=new Random(1337);
        if(style.equals("child_drawings")){
            g.setStroke(new BasicStroke(5.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            stroke(g,rng,75,345,75,197,212,91,367,211,367,344,75,345);stroke(g,rng,60,210,211,76,386,219);stroke(g,rng,181,344,181,250,240,252,240,344);
            for(int x:new int[]{110,274}){stroke(g,rng,x,211,x+48,211,x+48,259,x,259,x,211);stroke(g,rng,x+24,212,x+24,259);}
            person(g,rng,130,443,148);person(g,rng,278,463,123);person(g,rng,374,429,171);
            g.setColor(new Color(115,51,47));for(int i=0;i<5;i++){
                int x=607+(i%2)*177,y=145+(i/2)*182;stroke(g,rng,x-53,y,x-20,y-28,x+21,y-27,x+54,y,x+18,y+24,x-18,y+26,x-53,y);ring(g,rng,x,y,12,19);
            }
            g.setColor(new Color(59,65,82));stroke(g,rng,574,674,629,632,697,679,749,638,809,683,866,635,930,679);
        }else if(style.equals("frantic_scrawls")){
            for(int page=0;page<2;page++)for(int row=0;row<13;row++){
                double[] points=new double[62];for(int i=0;i<31;i++){points[i*2]=page*512+52+i*13;points[i*2+1]=91+row*43+rng.nextInt(28)-14;}
                stroke(g,rng,points);
            }
            g.setFont(new Font("Serif",Font.ITALIC|Font.BOLD,43));g.rotate(-.08,256,384);g.drawString("let me out",92,272);g.drawString("do not leave",76,503);g.rotate(.08,256,384);
            g.setFont(new Font("Serif",Font.ITALIC,39));g.drawString("STAY",683,305);g.drawString("again again",591,539);
            for(int i=0;i<19;i++)stroke(g,rng,60+rng.nextInt(405),160+rng.nextInt(470),49+rng.nextInt(415),149+rng.nextInt(480));
            stroke(g,rng,573,299,925,315,581,322,935,296);stroke(g,rng,578,527,933,547,583,550);
        }else if(style.equals("ritual_circles")){
            for(int page=0;page<2;page++){
                int x=256+page*512,y=365;
                for(int rad:new int[]{105,151,184})ring(g,rng,x,y,rad,rad*.98);
                for(int i=0;i<9;i++){double a=i*Math.PI*2/9;stroke(g,rng,x+Math.cos(a)*154,y+Math.sin(a)*154,x+Math.cos(a+.18)*183,y+Math.sin(a+.18)*183);}
                stroke(g,rng,x,y-140,x+123,y+70,x-123,y+70,x,y-140);stroke(g,rng,x,y+140,x-123,y-70,x+123,y-70,x,y+140);
                for(int i=0;i<7;i++){int xx=page*512+100+i*52;stroke(g,rng,xx,626,xx+8,612,xx+15,629,xx+24,611,xx+20,641);}
                ring(g,rng,x,y,20,22);stroke(g,rng,x-13,y-7,x+15,y+11,x+5,y-19,x-5,y+20);
            }
        }
        g.dispose();return im;
    }
    public static void main(String[] args)throws Exception{
        var folder=Path.of("development/assets/equipment-catalog/textures/evidence-books");Files.createDirectories(folder);
        for(int tier=1;tier<=3;tier++)for(String style:new String[]{"blank","child_drawings","frantic_scrawls","ritual_circles"})ImageIO.write(image(tier,style),"png",folder.resolve("tier-"+tier+"-"+style+".png").toFile());
        System.out.println("PASS 12 original parchment spreads: three tiers, blank plus all three existing writing styles");
    }
}
