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
    /** Ray-cast, textured oval volume in world space, with its lower shell below the floor. */
    public static void drawBody(int[] pixels,double[] depth,double cameraX,double cameraZ,double yaw,double pitch,
            SceneRenderer.RemoteBody actor,double elevation,boolean lamp,int width,int height,double focal){
        if(pixels.length!=width*height||depth.length!=pixels.length)return;
        final double lengthRadius=1.0,widthRadius=.46,heightRadius=.20,centerHeight=.10;
        final double textureLength=1.8,textureWidth=.8;
        double dx=actor.x()-cameraX,dz=actor.z()-cameraZ,cos=Math.cos(yaw),sin=Math.sin(yaw);
        double right=-dx*cos+dz*sin,ahead=dx*sin+dz*cos,cy=Math.cos(pitch),sy=Math.sin(pitch);
        double ground=elevation-1.62,centerDepth=(ground+centerHeight)*sy+ahead*cy;
        if(centerDepth+1.2<.065||centerDepth-1.2>24)return;
        var image=body(actor.appearance());double heading=Math.toRadians(actor.heading()),hs=Math.sin(heading),hc=Math.cos(heading);
        // Three overlapping soft contacts form an oval shadow along the body.
        for(double offset:new double[]{-.48,0,.48})
            EntityFootShadow.draw(pixels,depth,cameraX,cameraZ,yaw,pitch,
                actor.x()+hs*offset,actor.z()+hc*offset,elevation,.43,85,width,height,focal);
        int x0=width-1,x1=0,y0=height-1,y1=0;boolean clipped=false;
        for(int i=0;i<8;i++){
            double along=(i&1)==0?-lengthRadius:lengthRadius;
            double across=(i&2)==0?-widthRadius:widthRadius;
            double wy=ground+((i&4)==0?0:centerHeight+heightRadius);
            double wx=dx+along*hs+across*hc,wz=dz+along*hc-across*hs;
            double r=-wx*cos+wz*sin,z=wx*sin+wz*cos,d=wy*sy+z*cy;
            if(d<.065){clipped=true;continue;}
            double px=width*.5+r/d*focal,py=height*.5-(wy*cy-z*sy)/d*focal;
            x0=Math.min(x0,(int)Math.floor(px)-1);x1=Math.max(x1,(int)Math.ceil(px)+1);
            y0=Math.min(y0,(int)Math.floor(py)-1);y1=Math.max(y1,(int)Math.ceil(py)+1);
        }
        if(clipped){x0=0;x1=width-1;y0=0;y1=height-1;}
        x0=Math.max(0,x0);x1=Math.min(width-1,x1);y0=Math.max(0,y0);y1=Math.min(height-1,y1);
        double ol=-dx*hs-dz*hc,ow=-dx*hc+dz*hs,oy=-ground-centerHeight;
        double il=1/(lengthRadius*lengthRadius),iw=1/(widthRadius*widthRadius),ih=1/(heightRadius*heightRadius);
        double c=ol*ol*il+ow*ow*iw+oy*oy*ih-1;
        for(int py=y0;py<=y1;py++){
            double up=(height*.5-py-.5)/focal,ry=up*cy+sy,forward=cy-up*sy;
            for(int px=x0;px<=x1;px++){
                double side=(px+.5-width*.5)/focal;
                double rx=-side*cos+forward*sin,rz=side*sin+forward*cos;
                double dl=rx*hs+rz*hc,dw=rx*hc-rz*hs;
                double qa=dl*dl*il+dw*dw*iw+ry*ry*ih;
                double qb=2*(ol*dl*il+ow*dw*iw+oy*ry*ih),disc=qb*qb-4*qa*c;
                if(disc<0)continue;
                double d=(-qb-Math.sqrt(disc))/(2*qa);int at=py*width+px;
                if(d<.065||d>24||d>=depth[at])continue;
                double along=ol+dl*d,across=ow+dw*d,vertical=oy+ry*d;
                if(vertical+centerHeight<-.002)continue;
                double u=.5-along/textureLength,v=.5+across/textureWidth;
                if(u<0||u>=1||v<0||v>=1)continue;
                int argb=image.getRGB((int)(u*384),(int)(v*192)),alpha=argb>>>24;if(alpha<24)continue;
                // Curved normals make the rounded shell readable from a low side view.
                double nl=along*il,nw=across*iw,ny=vertical*ih,norm=Math.sqrt(nl*nl+nw*nw+ny*ny);
                double toward=-(nl*dl+nw*dw+ny*ry)/(norm*Math.sqrt(side*side+up*up+1));
                double diffuse=Math.min(1,Math.max(0,.85*ny/norm+.25*Math.max(0,toward)));
                double light=(lamp?1.1:.55)*Math.max(.22,1-d/26)*(.50+.50*diffuse);
                double fog=Math.min(.75,Math.max(0,(d-5)/15));
                int red=(int)(Math.min(255,((argb>>>16)&255)*light)*(1-fog)+18*fog);
                int green=(int)(Math.min(255,((argb>>>8)&255)*light)*(1-fog)+21*fog);
                int blue=(int)(Math.min(255,(argb&255)*light)*(1-fog)+30*fog),old=pixels[at];
                pixels[at]=((red*alpha+((old>>>16)&255)*(255-alpha))/255<<16)
                    |((green*alpha+((old>>>8)&255)*(255-alpha))/255<<8)|(blue*alpha+(old&255)*(255-alpha))/255;
                if(alpha>=220)depth[at]=d;
            }
        }
    }
}
