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
        check(latched==deliberateOrbit&&latched!=raw,
            "returning across the threshold must retain the stable art view even when raw facing differs");
        // Camera-facing is essential: stationary crossed quads become
        // edge-on while circling an NPC and appear to jitter or disappear.
        // The billboard is still in world space and keeps its floor anchor.
        for(int degrees=0;degrees<360;degrees+=15){
            double cx=cameraX(degrees),cz=cameraZ(degrees);
            double[] axis=DirectionalSprites.cameraFacingPlaneAxis(cx,cz,0,0);
            double dot=axis[0]*cx+axis[1]*cz;
            check(Math.abs(dot)<1e-9,"NPC card must face camera in every quadrant");
            check(Math.abs(Math.hypot(axis[0],axis[1])-1)<1e-9,
                "NPC card must preserve full physical width while orbiting");
            double[] farther=DirectionalSprites.cameraFacingPlaneAxis(cx*2,cz*2,0,0);
            check(Math.abs(axis[0]-farther[0])<1e-9
                    &&Math.abs(axis[1]-farther[1])<1e-9,
                "camera distance must not change the billboard's rotation");
        }
        var left=DirectionalSprites.cameraFacingPlaneAxis(-.03,3,0,0);
        var right=DirectionalSprites.cameraFacingPlaneAxis(.03,3,0,0);
        check(left[0]*right[0]+left[1]*right[1]>.999,
            "small lateral camera movement must smoothly rotate the card, not flip it");
        var card=DirectionalSprites.groundedBillboardCard(1.79,.98,0);
        check(Math.abs(card.left()+.49)<1e-9
                &&Math.abs(card.right()-.49)<1e-9
                &&Math.abs(card.bottom()+1.62)<1e-9
                &&Math.abs(card.top()-.17)<1e-9,
            "camera-facing card preserves an immutable actor-space sole and centre");
        check(card.equals(DirectionalSprites.groundedBillboardCard(1.79,.98,0)),
            "camera orbit cannot change world-space character height/width");
        // Camera-facing source art still has directional shoe padding; it
        // must not be mistaken for new body geometry below the floor.
        for(int visibleSoleRow:new int[]{182,201,205,219}){
            double sole=(visibleSoleRow+.5)/224.0;
            double floorV=(card.top()-card.bottom())/1.79*sole;
            check(Math.abs(floorV-sole)<1e-12,
                "every approved boot pixel must map to fixed world floor");
            check(DirectionalSprites.groundedUvAboveFloor(floorV,sole),
                "substantial boot sole must remain on the supporting floor");
            check(!DirectionalSprites.groundedUvAboveFloor(sole+1.0/224.0,sole)
                    &&!DirectionalSprites.groundedUvAboveFloor(-.01,sole),
                "stray alpha fringe beneath the boot must never render");
        }
        // Movement is not a change of character identity. Previously every
        // millimetre of Tomas's routine discarded hysteresis and snapped art.
        int tomasFront=DirectionalSprites.stableFacing("tomas_greaves",0,10,10,
            10+cameraX(60),10+cameraZ(60));
        check(tomasFront==1,"Tomas original angle selection");
        int tomasMoving=DirectionalSprites.stableFacing("tomas_greaves",0,10.014,10.008,
            10.014+cameraX(69),10.008+cameraZ(69));
        check(tomasMoving==tomasFront,
            "walking a few millimetres cannot reset Tomas's facing hysteresis");
        System.out.println("PASS Castle camera-facing depth-tested NPCs: smooth orbit, persistent facing hysteresis, fixed floor contact");
    }
}
