package org.beckenburg;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Images baked from Castle gameplay captures and a native Beckenburg mark. */
final class UiBranding {
    private UiBranding(){}
    private static BufferedImage castle;
    private static BufferedImage badge;
    private static BufferedImage read(String name){
        try(var stream=UiBranding.class.getResourceAsStream(
                "/org/beckenburg/ui/branding/"+name+".png")){
            return stream==null?null:ImageIO.read(stream);
        }catch(IOException ignored){return null;}
    }
    static synchronized BufferedImage splash(){
        if(castle==null)castle=read("castle-splash");
        return castle;
    }
    static synchronized Image desktopIcon(){
        if(badge==null)badge=read("beckenburg");
        return badge;
    }
}
