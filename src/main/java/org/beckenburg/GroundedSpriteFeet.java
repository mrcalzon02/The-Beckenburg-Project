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

    static int lastVisibleRow(BufferedImage image){
        for(int y=image.getHeight()-1;y>=0;y--){
            for(int x=0;x<image.getWidth();x++){
                if((image.getRGB(x,y)>>>24)>=24)return y;
            }
        }
        return -1;
    }
}
