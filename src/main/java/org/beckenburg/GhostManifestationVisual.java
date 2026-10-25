package org.beckenburg;

import java.awt.image.BufferedImage;

/**
 * Every visible but not yet signature-revealed ghost uses the already shipped
 * Mimic cutouts. This is a PRESENTATION binding, not a change to the ghost's
 * identity, evidence, abilities, host authority, or signature-specific art.
 *
 * Keep UNREVEALED distinct from "mimic": clients must not infer the real ghost
 * species from a non-signature manifestation. No procedural placeholder sprite.
 */
public final class GhostManifestationVisual {
    public static final String UNREVEALED = "unrevealed_ghost";
    public static final String DEFAULT_SPRITE = "mimic";
    private GhostManifestationVisual() {}

    public static boolean revealsIdentity(LanGhostAnimation.Phase phase) {
        return phase == LanGhostAnimation.Phase.SIGNATURE;
    }
    public static String appearance(String actual, LanGhostAnimation.Phase phase) {
        return revealsIdentity(phase) ? actual : UNREVEALED;
    }

    /** All original-direction cutouts are taken from the Mimic's shipped atlas.
     * The present atlas is not authored frame-by-frame animation, so retain the
     * genuine four source angles and idle cutout for all unrevealed phases.
     */
    static int sourceFacing(int facing) {
        if(facing<0||facing>=8)throw new IllegalArgumentException("facing must be 0..7");
        return switch(facing) {
            case 0,1,7 -> 0; // front
            case 2,6 -> 2;   // side
            case 3,5 -> 3;   // three-quarter
            default -> 4;   // back
        };
    }
    public static BufferedImage frame() { return frame(0); }
    public static BufferedImage frame(int facing) {
        BufferedImage sprite=GhostSpriteAtlas.frame(DEFAULT_SPRITE,sourceFacing(facing),"idle",0);
        if(sprite==null)
            throw new IllegalStateException(
                "Required Mimic directional atlas missing; unrevealed ghosts cannot use a placeholder");
        return sprite;
    }
}
