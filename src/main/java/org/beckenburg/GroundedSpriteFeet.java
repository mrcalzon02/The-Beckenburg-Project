package org.beckenburg;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Presentation-only foot anchoring for human character cutout billboards.
 * Does not modify source art, collision, world coordinates or ghost hover FX. */
public final class GroundedSpriteFeet {
    private static final double BILLBOARD_REFERENCE_EYE=1.62;
    private static final Map<BufferedImage,Integer> BOTTOM_ROWS=
        Collections.synchronizedMap(new WeakHashMap<>());
    private GroundedSpriteFeet(){}

    /** DirectionalSprites interprets elevation relative to a reference-height
     * camera; compensate for the actual camera eye used by castle geometry. */
    public static double relativeElevation(double floorY,double cameraEyeY){
        if(!Double.isFinite(floorY)||!Double.isFinite(cameraEyeY))
            throw new IllegalArgumentException("non-finite billboard elevation");
        return floorY-(cameraEyeY-BILLBOARD_REFERENCE_EYE);
    }

    public static boolean isGroundedHuman(String identity){
        if(identity==null)return false;
        return switch(identity){
            case "cleric","observer","demonologist","lore_master",
                 "elsbeth_vale","rowan_pike","tomas_greaves",
                 "sister_mirelle_ash" -> true;
            default -> false;
        };
    }

    /** Transparent bottom rows differ between approved directional cutouts.
     * Translate the image (never stretch or crop source pixels) so the last
     * visible shoe pixel meets the projected world-floor contact position. */
    public static double bottomPaddingShift(BufferedImage image,double screenHeight){
        if(image==null||!Double.isFinite(screenHeight)||screenHeight<=0)return 0;
        int row=BOTTOM_ROWS.computeIfAbsent(image,GroundedSpriteFeet::lastVisibleRow);
        if(row<0)return 0;
        return Math.max(0,image.getHeight()-1-row)*screenHeight/image.getHeight();
    }

    /** Use a substantial shoe-width alpha run, not a detached pixel, trailing
     * decorative fringe, or the transparent matte around an approved cutout. */
    static int lastVisibleRow(BufferedImage image){
        int width=image.getWidth(),left=width/8,right=width-left;
        int minimum=Math.max(3,width/24);
        for(int y=image.getHeight()-1;y>=0;y--){
            int count=0,run=0,longest=0;
            for(int x=left;x<right;x++){
                if((image.getRGB(x,y)>>>24)>=96){
                    count++;longest=Math.max(longest,++run);
                }else run=0;
            }
            if(count>=minimum&&longest>=Math.max(2,width/64))return y;
        }
        // Tiny legacy cutouts may lack a shoe-width opaque region. Retain their
        // prior behavior rather than hiding or drastically relocating them.
        for(int y=image.getHeight()-1;y>=0;y--)
            for(int x=0;x<width;x++)
                if((image.getRGB(x,y)>>>24)>=24)return y;
        return -1;
    }

    /** Match the character's world-space sole to the *rasterized* floor, not
     * merely to an ideal unsnapped plane. GPU vertex snapping can shift the
     * visible surface by several screen pixels while leaving its depth valid.
     * Require two independent, smoothly sloping floor-depth crossings; walls,
     * sky, props, gaps and stale/nonfinite buffers keep the geometric anchor. */
    public static double renderedFloorCorrection(double[] depth,int width,int height,
            double centerX,double footY,double contactDepth,double spriteWidth){
        if(depth==null||depth.length!=width*height||width<2||height<2
                ||!Double.isFinite(centerX+footY+contactDepth+spriteWidth)
                ||contactDepth<=0||spriteWidth<=0)return 0;
        int radius=Math.max(4,Math.min(12,(int)Math.ceil(height*.015)));
        double[] offsets=new double[3];int count=0;
        for(double fraction:new double[]{-.22,0,.22}){
            int px=(int)Math.round(centerX+fraction*spriteWidth);
            if(px<0||px>=width)continue;
            double best=Double.POSITIVE_INFINITY;
            for(int py=Math.max(0,(int)Math.floor(footY)-radius);
                    py<Math.min(height-1,(int)Math.ceil(footY)+radius);py++){
                double above=depth[py*width+px],below=depth[(py+1)*width+px];
                if(!Double.isFinite(above)||!Double.isFinite(below)
                        ||above<contactDepth||below>contactDepth
                        ||above-below<0
                        ||above-below>Math.max(.15,contactDepth*.08))continue;
                double correction=py+1-footY;
                if(Math.abs(correction)<Math.abs(best))best=correction;
            }
            if(Double.isFinite(best))offsets[count++]=best;
        }
        if(count<2)return 0;
        java.util.Arrays.sort(offsets,0,count);
        if(offsets[count-1]-offsets[0]>3)return 0;
        return Math.max(-radius,Math.min(radius,offsets[count/2]));
    }
}
