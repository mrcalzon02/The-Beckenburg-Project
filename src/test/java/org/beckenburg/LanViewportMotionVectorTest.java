package org.beckenburg;

import java.awt.event.KeyEvent;
import java.util.Arrays;

/** Offscreen benchmark must issue the same world HINPUT vector as a real held key.
 * Never fake BODY movement, relax the input gate or count synthetic STEP replies. */
public final class LanViewportMotionVectorTest {
    private static void expect(int key,double yaw,int x,int z){
        int[] actual=LanViewportPerformanceReview.worldInput(key,yaw);
        if(!Arrays.equals(actual,new int[]{x,z}))
            throw new AssertionError("Wrong host movement direction key="+key+
                " yaw="+yaw+" expected="+x+","+z+" actual="+Arrays.toString(actual));
        // SceneRenderer projects camera-right as -dx*cos(yaw)+dz*sin(yaw).
        // At yaw 0, +world-X is SCREEN LEFT; reversing this assertion would
        // test the opposite of the player's actual view.
        if(key==KeyEvent.VK_A||key==KeyEvent.VK_D){
            double screenX=-actual[0]*Math.cos(yaw)+actual[1]*Math.sin(yaw);
            if(key==KeyEvent.VK_A&&screenX>=0
                    ||key==KeyEvent.VK_D&&screenX<=0)
                throw new AssertionError("Lateral HINPUT moved the wrong SCREEN direction "+
                    "key="+key+" yaw="+yaw+" screenX="+screenX);
        }
    }
    public static void main(String[] args){
        expect(KeyEvent.VK_W,0,0,100);
        expect(KeyEvent.VK_S,0,0,-100);
        expect(KeyEvent.VK_A,0,100,0);
        expect(KeyEvent.VK_D,0,-100,0);
        expect(KeyEvent.VK_W,Math.PI/2,100,0);
        expect(KeyEvent.VK_A,Math.PI/2,0,-100);
        expect(KeyEvent.VK_D,Math.PI/2,0,100);
        try{
            LanViewportPerformanceReview.worldInput(KeyEvent.VK_ESCAPE,0);
            throw new AssertionError("Menu key used as movement vector");
        }catch(IllegalArgumentException expected){}
        System.out.println("PASS physical benchmark world vectors match normal 50Hz input, no menu bypass");
    }
}
