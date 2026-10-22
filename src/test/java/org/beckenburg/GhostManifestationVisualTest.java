package org.beckenburg;

import java.awt.image.BufferedImage;

/** Visual identity stays hidden except during a host-authorized signature event. */
public final class GhostManifestationVisualTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args) {
        BufferedImage image=GhostManifestationVisual.frame();
        check(image.getWidth()==128&&image.getHeight()==224,"PSX generic sprite geometry");
        check(image.getColorModel().hasAlpha(),"generic sprite alpha mask");
        check((image.getRGB(0,0)>>>24)==0,"transparent matte");
        check((image.getRGB(64,116)>>>24)>24,"visible ghost silhouette");
        for(LanGhostAnimation.Phase phase:LanGhostAnimation.Phase.values()){
            String id=GhostManifestationVisual.appearance("elder_nothic",phase);
            check(id.equals(phase==LanGhostAnimation.Phase.SIGNATURE
                ?"elder_nothic":GhostManifestationVisual.UNREVEALED),
                "ghost identity leaked before signature: "+phase);
        }
        for(int facing=0;facing<8;facing++)
            check(DirectionalSprites.frame(GhostManifestationVisual.UNREVEALED,
                facing,LanGhostAnimation.Phase.LURK,0)==image,
                "generic texture must cover all directions");
        System.out.println("PASS: generic apparition texture and signature-only identity selection");
    }
}
