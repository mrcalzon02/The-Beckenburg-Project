package org.beckenburg;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;

/** Standalone, headless-compatible HUD clipping regression. */
public final class HudTextFitTest {
    private static void check(boolean test,String what){if(!test)throw new AssertionError(what);}
    public static void main(String[] args){
        var image=new BufferedImage(256,64,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();
        try{
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,13));
            FontMetrics fm=g.getFontMetrics();
            String text="HEARD: Footsteps near the stairwell from the east";
            for(int width:new int[]{0,1,10,25,60,90,120,180,260,500}){
                String clipped=HudTextFit.ellipsize(fm,text,width);
                check(fm.stringWidth(clipped)<=width,"overflow at "+width);
                if(fm.stringWidth(text)<=width)check(clipped.equals(text),"unnecessary truncation");
            }
            check(HudTextFit.ellipsize(fm,text,500).equals(text),"full text retained");
            check(HudTextFit.ellipsize(fm,text,70).endsWith("..."),"ellipsis visible");
            String unicode="HEARD: \uD83D\uDD14 a distant bell rings";
            for(int width=10;width<fm.stringWidth(unicode);width++){
                String clipped=HudTextFit.ellipsize(fm,unicode,width);
                check(!clipped.contains("\uFFFD"),"invalid replacement character");
                for(int i=0;i<clipped.length();i++)
                    if(Character.isHighSurrogate(clipped.charAt(i)))
                        check(i+1<clipped.length()&&Character.isLowSurrogate(clipped.charAt(i+1)),"split surrogate");
            }
            System.out.println("PASS HudTextFit clipping at all tested widths");
        }finally{g.dispose();}
    }
}
