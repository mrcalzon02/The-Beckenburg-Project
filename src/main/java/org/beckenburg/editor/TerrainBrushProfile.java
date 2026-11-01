package org.beckenburg.editor;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Reusable precision sculpt-brush inputs and path spacing. All coordinates
 * are heightmap sample coordinates; painting is performed by TerrainLayerStack.
 * A custom alpha image is a per-stamp opacity mask, not a destructive stamp.
 */
public final class TerrainBrushProfile {
    private TerrainBrushProfile(){}

    public record Settings(double radius,double strength,double target,double inner,
                           int curve,double spacing,double rotationDegrees,
                           double minElevation,double maxElevation,double maxSlopeDegrees,
                           TerrainLayerStack.FlattenLimit flattenLimit) {
        public Settings {
            if(!Double.isFinite(radius)||radius<=0||radius>4096
                ||!Double.isFinite(strength)||!Double.isFinite(target)
                ||!Double.isFinite(inner)||inner<0||inner>=1
                ||curve<0||curve>2||!Double.isFinite(spacing)||spacing<=0||spacing>4096
                ||!Double.isFinite(rotationDegrees)
                ||!Double.isFinite(minElevation)||!Double.isFinite(maxElevation)
                ||minElevation>maxElevation||!Double.isFinite(maxSlopeDegrees)
                ||maxSlopeDegrees<0||maxSlopeDegrees>90
                ||flattenLimit==null)
                throw new IllegalArgumentException("Invalid precision brush settings");
        }
    }

    public static final class Alpha {
        private final float[][] values;
        private final int width,height;
        public Alpha(float[][] source) {
            Objects.requireNonNull(source,"alpha");
            if(source.length<2||source.length>2048||source[0]==null
                ||source[0].length<2||source[0].length>2048)
                throw new IllegalArgumentException("Alpha dimensions must be 2..2048");
            height=source.length;width=source[0].length;values=new float[height][width];
            for(int z=0;z<height;z++) {
                if(source[z]==null||source[z].length!=width)
                    throw new IllegalArgumentException("Ragged brush alpha");
                for(int x=0;x<width;x++){
                    float v=source[z][x];
                    if(!Float.isFinite(v)||v<0||v>1)
                        throw new IllegalArgumentException("Alpha outside 0..1");
                    values[z][x]=v;
                }
            }
        }
        public static Alpha fromImage(BufferedImage image) {
            Objects.requireNonNull(image,"image");
            if(image.getWidth()<2||image.getHeight()<2
                ||image.getWidth()>2048||image.getHeight()>2048)
                throw new IllegalArgumentException("Alpha PNG dimensions must be 2..2048");
            int width=image.getWidth(),height=image.getHeight();
            float[][] samples=new float[height][width];
            var raster=image.getRaster();
            boolean grayscale=raster.getNumBands()==1;
            int bits=grayscale?raster.getSampleModel().getSampleSize(0):8;
            double maximum=Math.pow(2,bits)-1;
            for(int z=0;z<height;z++)for(int x=0;x<width;x++){
                if(grayscale)samples[z][x]=(float)(raster.getSample(x,z,0)/maximum);
                else {
                    int argb=image.getRGB(x,z),r=(argb>>>16)&255,
                        g=(argb>>>8)&255,b=argb&255,a=(argb>>>24)&255;
                    samples[z][x]=(float)((.2126*r+.7152*g+.0722*b)/255.0*a/255.0);
                }
            }
            return new Alpha(samples);
        }
        /**
         * Samples bilinear alpha under a rotated stamp. Inputs are delta/radius.
         * Rotation is around brush center; outside the image footprint is zero.
         */
        public double sample(double dx,double dz,double degrees){
            if(!Double.isFinite(dx)||!Double.isFinite(dz)||!Double.isFinite(degrees))
                throw new IllegalArgumentException("Invalid brush sample");
            double radians=Math.toRadians(degrees);
            double c=Math.cos(radians),s=Math.sin(radians);
            double u=dx*c+dz*s,v=-dx*s+dz*c;
            if(u< -1||u>1||v< -1||v>1)return 0;
            double px=(u+1)*.5*(width-1),pz=(v+1)*.5*(height-1);
            int x=(int)Math.floor(px),z=(int)Math.floor(pz);
            int nextX=Math.min(width-1,x+1),nextZ=Math.min(height-1,z+1);
            double fx=px-x,fz=pz-z;
            return (values[z][x]*(1-fx)+values[z][nextX]*fx)*(1-fz)
                 +(values[nextZ][x]*(1-fx)+values[nextZ][nextX]*fx)*fz;
        }
    }

    @FunctionalInterface public interface Dabbler {
        void dab(double x,double z);
    }

    /**
     * Stroke sampler with true world-grid spacing, independent of the number
     * of mouse events. Emits the first dab immediately and interpolates across
     * skipped/fast mouse positions without prematurely finishing undo history.
     */
    public static final class Stroke {
        private final double spacing;
        private final Dabbler dab;
        private double lastX,lastZ,untilNext;
        private boolean active;
        public Stroke(double spacing,Dabbler dab) {
            if(!Double.isFinite(spacing)||spacing<=0||spacing>4096)
                throw new IllegalArgumentException("Invalid stroke spacing");
            this.spacing=spacing;this.dab=Objects.requireNonNull(dab);
        }
        public void begin(double x,double z) {
            point(x,z);lastX=x;lastZ=z;untilNext=spacing;active=true;dab.dab(x,z);
        }
        public void move(double x,double z) {
            point(x,z);
            if(!active)throw new IllegalStateException("Stroke has not begun");
            double length=Math.hypot(x-lastX,z-lastZ);
            if(length==0)return;
            if(length>100_000)throw new IllegalArgumentException("Stroke segment too long");
            double distance=untilNext;
            while(distance<=length+1e-9){
                double t=Math.min(1,distance/length);
                dab.dab(lastX+(x-lastX)*t,lastZ+(z-lastZ)*t);
                distance+=spacing;
            }
            untilNext=distance-length;
            lastX=x;lastZ=z;
        }
        public void end(){active=false;}
        private static void point(double x,double z){
            if(!Double.isFinite(x)||!Double.isFinite(z))
                throw new IllegalArgumentException("Invalid stroke location");
        }
    }
}
