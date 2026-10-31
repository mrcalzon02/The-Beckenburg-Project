package org.beckenburg;

import java.awt.image.BufferedImage;

/** Diagnostic: sprite appearance must not move with camera eye elevation or facing. */
public final class GroundedSpriteFeetTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    private static void near(double got,double expected,String message){
        check(Math.abs(got-expected)<1e-9,message+" got="+got+" expected="+expected);
    }
    public static void main(String[] args){
        for(double floor:new double[]{0,0.45,3.4})for(double eye:new double[]{
                floor+1.62,floor+1.12,floor+1.85,floor+2.4}){
            near(GroundedSpriteFeet.relativeElevation(floor,eye)-1.62,
                floor-eye,"world floor and billboard foot must project from identical Y");
        }
        for(String name:new String[]{"lore_master","elsbeth_vale","rowan_pike",
                "tomas_greaves","sister_mirelle_ash","cleric","observer","demonologist"})
            check(GroundedSpriteFeet.isGroundedHuman(name),"missing human: "+name);
        check(!GroundedSpriteFeet.isGroundedHuman("banshee")
            &&!GroundedSpriteFeet.isGroundedHuman("elder_blade_demon")
            &&!GroundedSpriteFeet.isGroundedHuman(null),
            "intentional ghost floating must be untouched");
        BufferedImage frame=new BufferedImage(128,224,BufferedImage.TYPE_INT_ARGB);
        for(int y=2;y<=205;y++)for(int x=12;x<116;x++)frame.setRGB(x,y,0xff8899aa);
        check(GroundedSpriteFeet.lastVisibleRow(frame)==205,"cutout foot location");
        near(GroundedSpriteFeet.bottomPaddingShift(frame,112),9.0,
            "transparent tail has 18 pixels and half-scale shift should be 9");
        for(double screenHeight:new double[]{32,112,264}){
            double shift=GroundedSpriteFeet.bottomPaddingShift(frame,screenHeight);
            double apparentShoe=shift+(205.5)/224.0*screenHeight;
            near(apparentShoe,screenHeight-.5/224.0*screenHeight,
                "same floor contact after perspective scaling");
        }
        BufferedImage full=new BufferedImage(8,8,BufferedImage.TYPE_INT_ARGB);
        full.setRGB(4,7,0xff000000);
        near(GroundedSpriteFeet.bottomPaddingShift(full,100),0,
            "already grounded art must be unmodified");
        System.out.println("PASS human sprite world foot anchor, camera movement and original alpha bounds");
    }
}
