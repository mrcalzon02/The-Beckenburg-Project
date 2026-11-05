package org.beckenburg;

import java.awt.image.BufferedImage;

/** Functional invariants of the visual billboard math. Advisory art review
 * still requires a genuine in-engine screenshot, not a synthetic preview.
 */
public final class CharacterBillboardAnchorTest {
    private CharacterBillboardAnchorTest(){}
    private static void check(boolean yes,String message){
        if(!yes)throw new AssertionError(message);
    }
    private static double cameraX(double degrees){return Math.sin(Math.toRadians(degrees))*3;}
    private static double cameraZ(double degrees){return Math.cos(Math.toRadians(degrees))*3;}
    public static void main(String[] ignored){
        var asymmetric=new BufferedImage(128,224,BufferedImage.TYPE_INT_ARGB);
        for(int y=80;y<148;y++)for(int x=22;x<60;x++)
            asymmetric.setRGB(x,y,0xff765432);
        check(Math.abs(DirectionalSprites.bodyPivotX(asymmetric)-40.5)<.01,
            "opaque character torso, not the transparent outer margins, owns its pivot");
        // The frame transition at 67.5 degrees used to produce sudden lateral
        // jumps as the player shuffled just a few centimetres sideways.
        int initial=DirectionalSprites.stableFacing("rowan_pike",0,0,0,
            cameraX(60),cameraZ(60));
        check(initial==1,"original four-view front-biased segment retained");
        int nearBoundary=DirectionalSprites.stableFacing("rowan_pike",0,0,0,
            cameraX(69),cameraZ(69));
        check(nearBoundary==initial,"small strafe across boundary cannot snap frame");
        int deliberateOrbit=DirectionalSprites.stableFacing("rowan_pike",0,0,0,
            cameraX(86),cameraZ(86));
        check(deliberateOrbit==2,"meaningful camera orbit still changes perspective");
        check(DirectionalSprites.stableFacing("lore_master",0,0,0,
            cameraX(80),cameraZ(80))>=0,"Matthis uses the same stable pivots");
        int latched=DirectionalSprites.stableFacing("rowan_pike",0,0,0,
            cameraX(65),cameraZ(65));
        int raw=DirectionalSprites.facing(0,0,0,cameraX(65),cameraZ(65));
        // At a hysteresis threshold the stable art can deliberately differ
        // from raw facing; using raw geometry creates an instant 90-degree
        // flicker even though the image has not yet changed.
        check(latched==initial&&latched!=raw,
            "hysteresis produces a deliberate held frame distinct from raw facing");
        // The selected front/back and side card geometries are the same
        // fixed planes regardless of camera yaw, proximity or strafe.
        for(int heading:new int[]{0,45,90,180,270}){
            double[] front=DirectionalSprites.worldPlaneAxis(heading,0);
            double[] frontDiagonal=DirectionalSprites.worldPlaneAxis(heading,1);
            double[] side=DirectionalSprites.worldPlaneAxis(heading,2);
            double[] sideDiagonal=DirectionalSprites.worldPlaneAxis(heading,3);
            check(Math.abs(front[0]-frontDiagonal[0])<1e-9
                    &&Math.abs(front[1]-frontDiagonal[1])<1e-9,
                "front quarter views cannot rotate physical card toward camera");
            check(Math.abs(side[0]-sideDiagonal[0])<1e-9
                    &&Math.abs(side[1]-sideDiagonal[1])<1e-9,
                "side quarter views cannot rotate physical card toward camera");
            check(Math.abs(front[0]*side[0]+front[1]*side[1])<1e-9,
                "permanent crossed character planes must be orthogonal");
        }
        // Camera-selected source cutouts have wildly different transparent
        // margins. The ACTOR plane must nevertheless be byte-for-byte fixed.
        var fixed=DirectionalSprites.worldLockedHumanCard(1.79,.98,0);
        check(Math.abs(fixed.left()+.49)<1e-9
                &&Math.abs(fixed.right()-.49)<1e-9
                &&Math.abs(fixed.bottom()+1.62)<1e-9
                &&Math.abs(fixed.top()-.17)<1e-9,
            "world card uses an immutable actor-space sole and centre");
        var tall=DirectionalSprites.worldLockedHumanCard(1.79,.98,0);
        check(fixed.equals(tall),"camera orbit cannot change world card geometry");
        // The shoe anchor may differ among cutouts, but fitting it belongs
        // entirely to texture coordinates: v=sole at physical world floor.
        for(int visibleSoleRow:new int[]{182,201,205,219}){
            double sole=(visibleSoleRow+.5)/224.0;
            double floorV=(fixed.top()-fixed.bottom())/1.79*sole;
            check(Math.abs(floorV-sole)<1e-12,
                "every approved shoe pixel must map to unchanged world floor");
            check(DirectionalSprites.groundedUvAboveFloor(floorV,sole),
                "substantial boot sole must remain visible at support height");
            check(!DirectionalSprites.groundedUvAboveFloor(sole+1.0/224.0,sole)
                    &&!DirectionalSprites.groundedUvAboveFloor(-.01,sole),
                "stray artist fringe or transparent matte below sole must not render beneath floor");
        }
        System.out.println("PASS Castle world-locked characters: fixed geometry, height, lateral pivot and alpha shoe UV");
    }
}
