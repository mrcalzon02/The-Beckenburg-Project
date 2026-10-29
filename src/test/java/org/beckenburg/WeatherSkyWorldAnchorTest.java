package org.beckenburg;

import dev.mrcalzon.weather.WeatherSky;
import java.awt.image.BufferedImage;

/** A camera turn/pitch must alter the viewed sky, not rotate a screen-fixed overlay. */
public final class WeatherSkyWorldAnchorTest {
    private static BufferedImage sky(double heading,double pitch){
        int w=640,h=360;
        BufferedImage out=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        var graphics=out.createGraphics();
        try{
            var state=new WeatherSky.State(.86,0,0,.95,.07,0);
            WeatherSky.paint(graphics,w,h,state,0x4245434b454e4255L,
                .37,120,heading,0,pitch,275);
        }finally{graphics.dispose();}
        return out;
    }
    private static int difference(BufferedImage a,BufferedImage b){
        int changed=0;
        for(int y=0;y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++)
            if(a.getRGB(x,y)!=b.getRGB(x,y))changed++;
        return changed;
    }
    public static void main(String[] args){
        var straight=sky(0,0);
        if(difference(straight,sky(.45,0))<100)
            throw new AssertionError("Clouds/sun are nailed to the viewer's heading");
        if(difference(straight,sky(0,.25))<100)
            throw new AssertionError("Sky elevation is nailed to the camera viewport");
        if(difference(straight,sky(0,0))!=0)
            throw new AssertionError("Static world weather sky is nondeterministic");
        System.out.println("PASS: world-anchored weather sky pans and pitches with the viewpoint");
    }
}
