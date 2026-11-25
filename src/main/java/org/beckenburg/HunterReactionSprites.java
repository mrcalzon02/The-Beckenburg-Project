package org.beckenburg;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;

/** Authored impact cutouts and a top-down body texture per hunter class. */
public final class HunterReactionSprites {
    public static final double PAIN_SECONDS=.4;
    private static final String[] VIEWS={"front","front","side","three_quarter","back","three_quarter","side","front"};
    private static final Map<String,BufferedImage> FRAMES=load();
    private HunterReactionSprites(){}
    private static Map<String,BufferedImage> load(){
        var frames=new HashMap<String,BufferedImage>();
        try(var source=HunterReactionSprites.class.getResourceAsStream("hunter-reactions.zip")){
            if(source==null)throw new IOException("Missing hunter reaction sprites");
            try(var zip=new ZipInputStream(source)){
                java.util.zip.ZipEntry entry;
                while((entry=zip.getNextEntry())!=null){
                    String name=entry.getName();
                    if(!name.matches("(cleric|observer|demonologist)/(pain/(front|side|back|three_quarter)|body)[.]png"))
                        throw new IOException("Unexpected hunter reaction: "+name);
                    var image=ImageIO.read(zip);boolean body=name.endsWith("/body.png");
                    if(image==null||!image.getColorModel().hasAlpha()||image.getWidth()!=(body?384:128)||image.getHeight()!=(body?192:224)
                        ||frames.put(name.substring(0,name.length()-4),image)!=null)throw new IOException("Invalid hunter reaction "+name);
                }
            }
            if(frames.size()!=15)throw new IOException("Expected fifteen hunter reaction images");
        }catch(IOException e){throw new IllegalStateException("Cannot load hunter reactions",e);}
        return Map.copyOf(frames);
    }
    public static BufferedImage pain(String identity,int facing){
        if(!CharacterVisuals.isHunter(identity)||facing<0||facing>7)throw new IllegalArgumentException("Invalid hunter reaction");
        return FRAMES.get(identity+"/pain/"+VIEWS[facing]);
    }
    public static BufferedImage body(String identity){
        if(!CharacterVisuals.isHunter(identity))throw new IllegalArgumentException("Invalid body identity");
        return FRAMES.get(identity+"/body");
    }
    /** A body is a real horizontal floor card, never a squashed standing billboard. */
    public static void drawBody(int[] pixels,double[] depth,double cameraX,double cameraZ,double yaw,double pitch,
            SceneRenderer.RemoteBody actor,double elevation,boolean lamp,int width,int height,double focal){
        double dx=actor.x()-cameraX,dz=actor.z()-cameraZ,cos=Math.cos(yaw),sin=Math.sin(yaw);
        double right=-dx*cos+dz*sin,ahead=dx*sin+dz*cos,cy=Math.cos(pitch),sy=Math.sin(pitch);
        if(ahead+1.1<.065||ahead-1.1>24)return;
        EntityFootShadow.draw(pixels,depth,cameraX,cameraZ,yaw,pitch,actor.x(),actor.z(),elevation,.65,135,width,height,focal);
        var image=body(actor.appearance());double heading=Math.toRadians(actor.heading()),hs=Math.sin(heading),hc=Math.cos(heading);
        double ground=elevation-1.62;int x0=width-1,x1=0,y0=height-1,y1=0;boolean clipped=false;
        for(int i=0;i<4;i++){
            double along=(i<2?-.9:.9),across=(i%2==0?-.4:.4);
            double wx=dx+along*hs+across*hc,wz=dz+along*hc-across*hs;
            double r=-wx*cos+wz*sin,z=wx*sin+wz*cos,d=ground*sy+z*cy;
            if(d<.065){clipped=true;continue;}
            double px=width*.5+r/d*focal,py=height*.5-(ground*cy-z*sy)/d*focal;
            x0=Math.min(x0,(int)Math.floor(px)-1);x1=Math.max(x1,(int)Math.ceil(px)+1);
            y0=Math.min(y0,(int)Math.floor(py)-1);y1=Math.max(y1,(int)Math.ceil(py)+1);
        }
        if(clipped){x0=0;x1=width-1;y0=0;y1=height-1;}
        x0=Math.max(0,x0);x1=Math.min(width-1,x1);y0=Math.max(0,y0);y1=Math.min(height-1,y1);
        for(int py=y0;py<=y1;py++){
            double up=(height*.5-py-.5)/focal,rayY=up*cy+sy;
            if(Math.abs(rayY)<1e-8)continue;
            double d=ground/rayY;if(d<.065||d>24)continue;
            double forward=d*(cy-up*sy);
            for(int px=x0;px<=x1;px++){
                double side=(px+.5-width*.5)/focal*d;
                double wx=-side*cos+forward*sin-dx,wz=side*sin+forward*cos-dz;
                double u=.5-(wx*hs+wz*hc)/1.8,v=.5+(wx*hc-wz*hs)/.8;
                if(u<0||u>=1||v<0||v>=1)continue;
                int at=py*width+px;if(!Double.isFinite(depth[at])||Math.abs(depth[at]-d)>.04)continue;
                int argb=image.getRGB((int)(u*384),(int)(v*192)),alpha=argb>>>24;if(alpha<24)continue;
                double light=(lamp?1.1:.55)*Math.max(.22,1-d/26),fog=Math.min(.75,Math.max(0,(d-5)/15));
                int red=(int)(Math.min(255,((argb>>>16)&255)*light)*(1-fog)+18*fog);
                int green=(int)(Math.min(255,((argb>>>8)&255)*light)*(1-fog)+21*fog);
                int blue=(int)(Math.min(255,(argb&255)*light)*(1-fog)+30*fog),old=pixels[at];
                pixels[at]=((red*alpha+((old>>>16)&255)*(255-alpha))/255<<16)
                    |((green*alpha+((old>>>8)&255)*(255-alpha))/255<<8)|(blue*alpha+(old&255)*(255-alpha))/255;
                if(alpha>=220)depth[at]=Math.min(depth[at],d-.00001);
            }
        }
    }
}
