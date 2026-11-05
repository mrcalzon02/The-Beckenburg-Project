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
        System.out.println("PASS Castle character world-locked axes, alpha pivot and fixed spatial positions");
    }
}
