package org.beckenburg;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * All non-signature bodies use the already shipped Mimic direction cutouts.
 * Nothing here grants a new species reveal, a second Twins power, or an Ancient
 * green tint before the host's signature window.
 */
public final class GhostManifestationVisualTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    private static int[] pixels(BufferedImage image){
        return image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth());
    }
    private static void exact(BufferedImage actual,BufferedImage expected,String why){
        check(actual!=null&&expected!=null
            &&actual.getWidth()==expected.getWidth()
            &&actual.getHeight()==expected.getHeight()
            &&Arrays.equals(pixels(actual),pixels(expected)),why);
    }
    public static void main(String[] args) {
        check(GhostSpriteAtlas.installed()&&GhostSpriteAtlas.supports("mimic"),
            "unrevealed ghost requires the real packaged Mimic sprite atlas");
        check(GhostManifestationVisual.DEFAULT_SPRITE.equals("mimic"),
            "Mimic must be the generic pre-reveal appearance");
        BufferedImage front=GhostManifestationVisual.frame();
        check(front.getWidth()==64&&front.getHeight()==112,
            "pre-reveal Mimic uses shipped low-resolution PSX cutouts");
        check(front.getColorModel().hasAlpha(),"generic sprite keeps Mimic alpha");
        int transparent=0,visible=0;
        for(int p:pixels(front)){
            if((p>>>24)==0)transparent++;
            if((p>>>24)>=24)visible++;
        }
        check(transparent>75&&visible>=50,"Mimic's real silhouette and transparent matte");
        exact(front,GhostSpriteAtlas.frame("mimic",0,"idle",0),
            "generic front must be authentic Mimic art, not a procedural silhouette");
        for(int facing=0;facing<8;facing++){
            BufferedImage expected=GhostSpriteAtlas.frame("mimic",
                GhostManifestationVisual.sourceFacing(facing),"idle",0);
            exact(GhostManifestationVisual.frame(facing),expected,
                "generic facing must reuse the Mimic's actual directional cutout "+facing);
            exact(DirectionalSprites.frame(GhostManifestationVisual.UNREVEALED,facing),
                expected,"legacy directional entry point must use Mimic "+facing);
            for(LanGhostAnimation.Phase phase:LanGhostAnimation.Phase.values()){
                // Signature phase with the anonymous ID is not normally emitted,
                // but the renderer should still resolve the designated art.
                exact(DirectionalSprites.frame(GhostManifestationVisual.UNREVEALED,
                    facing,phase,3),expected,
                    "runtime directional entry point leaked/dropped Mimic "+facing+"/"+phase);
            }
        }
        check(!Arrays.equals(pixels(GhostManifestationVisual.frame(0)),
            pixels(GhostManifestationVisual.frame(2))),
            "Mimic front and side must be distinguishable; do not reuse one billboard");

        for(GhostIdentity.Type ghost:GhostIdentity.Type.values()){
            String actual=CharacterVisuals.ghost(ghost);
            for(LanGhostAnimation.Phase phase:LanGhostAnimation.Phase.values()){
                String chosen=GhostManifestationVisual.appearance(actual,phase);
                check(chosen.equals(phase==LanGhostAnimation.Phase.SIGNATURE
                        ?actual:GhostManifestationVisual.UNREVEALED),
                    "species revealed outside signature window: "+actual+"/"+phase);
            }
        }
        for(String twin:new String[]{"twins_a","twins_b","ancient_twins_a","ancient_twins_b"}){
            check(GhostManifestationVisual.appearance(twin,LanGhostAnimation.Phase.LURK)
                .equals(GhostManifestationVisual.UNREVEALED),
                "second Twins body must retain anonymous pre-reveal art "+twin);
            check(GhostManifestationVisual.appearance(twin,LanGhostAnimation.Phase.SIGNATURE)
                .equals(twin),"signature must preserve distinct Twin body "+twin);
        }
        exact(DirectionalSprites.frame(GhostManifestationVisual.appearance("elder_nothic",
            LanGhostAnimation.Phase.WALK),2,LanGhostAnimation.Phase.WALK,0),
            GhostSpriteAtlas.frame("mimic",2,"idle",0),
            "Elder ordinary manifestation must use the same Mimic pre-reveal art");
        check(!Arrays.equals(pixels(DirectionalSprites.frame("banshee",0,
                LanGhostAnimation.Phase.SIGNATURE,0)),pixels(front)),
            "non-Mimic signature must still reveal its own sprite");
        System.out.println("PASS: authored Mimic directional pre-reveal sprites, 26 ghost identities, "+
            "both Twins bodies, and signature-only species reveal");
    }
}
