package org.beckenburg;

import dev.mrcalzon.weather.WeatherSky;

/** Shared atmosphere policy for software/OpenGL fragments and translucent layers.
 * No depth writes, scene geometry, visibility authority or map-specific opt-in.
 * The existing world-anchored weather sky supplies the upper skybox; the lower
 * hemisphere converges to the same colour as distant physical geometry.
 */
public final class LevelAtmosphere {
    public static final double CLEAR_METRES=6,OPAQUE_METRES=24;
    public record Policy(int fogRgb,float clearMetres,float opaqueMetres){
        public Policy {if(!Float.isFinite(clearMetres)||!Float.isFinite(opaqueMetres)||clearMetres<0||opaqueMetres<=clearMetres)throw new IllegalArgumentException("Atmosphere range");}
    }
    private record Pass(int width,int height,double focal,Policy policy){}
    private static final ThreadLocal<Pass> ACTIVE=new ThreadLocal<>();
    public static final class Scope implements AutoCloseable {
        private final Pass previous;
        private Scope(Pass next){previous=ACTIVE.get();ACTIVE.set(next);}
        public void close(){if(previous==null)ACTIVE.remove();else ACTIVE.set(previous);}
    }
    public static Scope enter(int width,int height,double focal,double density,int fog){
        return new Scope(new Pass(width,height,focal,new Policy(fog,6,(float)opaqueMetres(density))));
    }
    public static boolean active(){return ACTIVE.get()!=null;}
    public static Policy policy(){Pass p=ACTIVE.get();return p==null?null:p.policy;}
    public static int shadeSquared(int colour,double distanceSquared){
        Pass p=ACTIVE.get();return p==null?colour:shadeSquared(colour,distanceSquared,p.policy);
    }
    private static int shadeSquared(int colour,double distanceSquared,Policy policy){
        double near=policy.clearMetres,far=policy.opaqueMetres;
        return blend(colour,policy.fogRgb,smooth((distanceSquared-near*near)/(far*far-near*near)));
    }
    public static int shadePixel(int colour,double depth,int x,int y){
        Pass p=ACTIVE.get();if(p==null)return colour;
        double rx=(x+.5-p.width*.5)/p.focal,ry=(y+.5-p.height*.5)/p.focal;
        return shadeSquared(colour,depth*depth*(1+rx*rx+ry*ry),p.policy);
    }
    public record Frame(int foggedPixels,int skyPixels,int fogRgb,
                        double clearMetres,double opaqueMetres){}
    private double[] horizontalSquared=new double[0];
    private double cachedFocal=Double.NaN;

    public static int fogColour(WeatherSky.State sky){
        double daylight=sky.daylight()*(1-sky.cloudCover()*.28);
        return ((int)Math.round(18+88*daylight)<<16)
            |((int)Math.round(24+94*daylight)<<8)
            |(int)Math.round(32+100*daylight);
    }
    public static double opaqueMetres(double density){
        return CLEAR_METRES+(OPAQUE_METRES-CLEAR_METRES)/Math.max(1,density);
    }
    private static double smooth(double value){
        double t=Math.max(0,Math.min(1,value));return t*t*(3-2*t);
    }
    private static int blend(int colour,int fog,double weight){
        if(weight>=1)return fog;
        int amount=(int)Math.round(weight*256),keep=256-amount;
        int r=(((colour>>>16)&255)*keep+((fog>>>16)&255)*amount+128)>>8;
        int g=(((colour>>>8)&255)*keep+((fog>>>8)&255)*amount+128)>>8;
        int b=((colour&255)*keep+(fog&255)*amount+128)>>8;
        return (r<<16)|(g<<8)|b;
    }
    /** Blend the world skybox horizon, never the foreground or its depth. */
    public static void skyboxHaze(int[] pixels,int width,int height,
                                  double focal,double pitch,int fog){
        for(int y=0;y<height;y++){
            double elevation=Math.atan((height*.5-y-.5)/focal)+pitch;
            double weight=smooth((.42-elevation)/.42);
            if(weight==0)continue;
            for(int x=0;x<width;x++){int at=y*width+x;pixels[at]=blend(pixels[at],fog,weight);}
        }
    }
    /** Radial, squared-distance smooth fade: clear near field, opaque by24m.
     * Squared camera ray length avoids a square root/exponential per pixel.
     * Storm/mission fog can thicken the baseline, never remove it.
     */
    public Frame inspect(double[] depth,int width,int height,double focal,double density,int fog){
        if(depth.length!=width*height
                ||!Double.isFinite(focal)||focal<=0||!Double.isFinite(density))
            throw new IllegalArgumentException("Invalid world atmosphere frame");
        if(horizontalSquared.length!=width||cachedFocal!=focal){
            horizontalSquared=new double[width];cachedFocal=focal;
            for(int x=0;x<width;x++){double ray=(x+.5-width*.5)/focal;horizontalSquared[x]=ray*ray;}
        }
        double far=opaqueMetres(density),near2=CLEAR_METRES*CLEAR_METRES,
            span=far*far-near2;int changed=0,sky=0;
        for(int y=0;y<height;y++){
            double vertical=(y+.5-height*.5)/focal;double rayBase=1+vertical*vertical;
            for(int x=0;x<width;x++){
                int at=y*width+x;double d=depth[at];
                if(!Double.isFinite(d)){sky++;continue;}
                if(d<=0)continue;
                double radial2=d*d*(rayBase+horizontalSquared[x]);
                if(radial2<=near2)continue;
                changed++;
            }
        }
        return new Frame(changed,sky,fog,CLEAR_METRES,far);
    }
}
