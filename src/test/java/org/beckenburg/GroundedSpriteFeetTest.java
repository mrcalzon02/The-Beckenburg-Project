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
        // A lone fringe or matte pixel below the real boots is not ground contact.
        frame.setRGB(3,223,0xff8899aa);
        frame.setRGB(60,221,0x308899aa);
        check(GroundedSpriteFeet.lastVisibleRow(frame)==205,"substantial sole, not stray pixel, sets the foot anchor");
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
        // An ideal horizontal support plane intersects the sprite's world
        // foot depth at row 100; a snapped GPU plane shifts that crossing.
        int w=80,h=200;double[] floorDepth=new double[w*h];
        java.util.Arrays.fill(floorDepth,Double.POSITIVE_INFINITY);
        for(int x=0;x<w;x++)for(int y=0;y<h;y++)
            floorDepth[y*w+x]=4.0+(102-y)*.07;
        near(GroundedSpriteFeet.renderedFloorCorrection(floorDepth,w,h,40,100,4.0,30),
            2,"rendered floor contact corrects a two-pixel raster displacement");
        java.util.Arrays.fill(floorDepth,Double.POSITIVE_INFINITY);
        near(GroundedSpriteFeet.renderedFloorCorrection(floorDepth,w,h,40,100,4.0,30),
            0,"sky or missing floor cannot move shoes");
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)floorDepth[y*w+x]=3.0;
        near(GroundedSpriteFeet.renderedFloorCorrection(floorDepth,w,h,40,100,4.0,30),
            0,"flat near wall cannot masquerade as a sloping floor");
        System.out.println("PASS human sprite alpha shoe anchor and rendered-floor depth lock");
    }
}
