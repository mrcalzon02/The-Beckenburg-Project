package org.beckenburg;

import java.awt.image.BufferedImage;

/**
 * Shared PSX-resolution neutral apparition. Its shape and colors are independent
 * of the real ghost type, variant, Twin member, and Ancient/Elder status.
 * Replace this centrally with approved generic art when it is available.
 */
public final class GhostManifestationVisual {
    public static final String UNREVEALED = "unrevealed_ghost";
    private static final BufferedImage TEXTURE = create();
    private GhostManifestationVisual() {}

    public static boolean revealsIdentity(LanGhostAnimation.Phase phase) {
        return phase == LanGhostAnimation.Phase.SIGNATURE;
    }
    public static String appearance(String actual, LanGhostAnimation.Phase phase) {
        return revealsIdentity(phase) ? actual : UNREVEALED;
    }
    public static BufferedImage frame() { return TEXTURE; }

    private static double ell(double dx,double dy,double rx,double ry) {
        double norm=dx*dx/(rx*rx)+dy*dy/(ry*ry);
        return Math.exp(-2.1*norm);
    }
    private static BufferedImage create() {
        BufferedImage img=new BufferedImage(128,224,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<224;y++)for(int x=0;x<128;x++){
            double xx=x-64.0;
            double shape=Math.max(
                Math.max(ell(xx,y-57,19,24),ell(xx,y-103,34,21)),
                Math.max(ell(xx,y-144,29,65),
                    Math.max(ell(xx+32,y-147,12,55),ell(xx-32,y-147,12,55))));
            double haze=.16*ell(xx,y-174,44,43);
            double dissolve=Math.max(.12,Math.min(1.0,(222.0-y)/32.0));
            double grain=.82+.13*Math.sin(x*1.77+y*2.51)*Math.sin(x*.77-y*1.97);
            int alpha=(int)(210*Math.min(1.0,shape+haze)*dissolve*grain);
            if(alpha<12)continue;
            int tone=Math.max(0,Math.min(220,(int)(42+17*Math.sin(x*.09+y*.045))));
            img.setRGB(x,y,(Math.min(255,alpha)<<24)|(tone<<16)|((tone+6)<<8)|(tone+15));
        }
        return img;
    }
}
