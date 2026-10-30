package org.beckenburg;

import java.awt.image.BufferedImage;

/** Diagnostic only: inspect bottom-edge alpha for the shipped Mimic cutouts.
 * A populated last row can indicate cropped feet; report rather than blocking release.
 */
public final class MimicSpriteFootAudit {
    public static void main(String[] args) {
        int suspect = 0;
        for (int facing=0; facing<8; facing++) {
            BufferedImage image=GhostSpriteAtlas.frame("mimic",facing,"idle",0);
            if (image==null) {
                System.out.println("Mimic atlas unavailable for facing "+facing);
                continue;
            }
            int bottom=0,nearBottom=0;
            for (int y=Math.max(0,image.getHeight()-4); y<image.getHeight(); y++)
                for (int x=0; x<image.getWidth(); x++)
                    if ((image.getRGB(x,y)>>>24)>=64) {
                        nearBottom++;
                        if (y==image.getHeight()-1) bottom++;
                    }
            System.out.println("Mimic facing "+facing+" bottom-edge alpha="+bottom+
                " last-four-rows="+nearBottom+" image="+image.getWidth()+"x"+image.getHeight());
            if (bottom>0) suspect++;
        }
        System.out.println("Mimic atlas frames touching bottom boundary: "+suspect+
            "/8; visual inspection required if nonzero.");
    }
}
