package org.beckenburg;

import java.awt.Font;
import java.awt.image.BufferedImage;

/** Standalone headless regression for compact stamina status presentation. */
public final class SprintHudResponsiveTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args){
        var image=new BufferedImage(512,128,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();
        try{
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,11));
            var fm=g.getFontMetrics();
            for(int width:new int[]{80,90,120,150,180,220,255,320,480}){
                for(int remaining:new int[]{0,15,300}){
                    for(boolean spending:new boolean[]{false,true}){
                        String value=SprintHud.caption(fm,width,"GHOST BURST / SHIFT",remaining,300,spending);
                        check(fm.stringWidth(value)<=width-16,"caption exceeds width "+width);
                        check(!value.isBlank(),"caption blank at usable width "+width);
                        SprintHud.draw(g,0,0,width,"GHOST BURST / SHIFT",remaining,300,spending);
                    }
                }
            }
            String large=SprintHud.caption(fm,480,"GHOST BURST / SHIFT",300,300,false);
            check(large.contains("GHOST BURST / SHIFT")&&large.endsWith("READY"),"full caption retained");
            String small=SprintHud.caption(fm,80,"GHOST BURST / SHIFT",300,300,false);
            check(small.startsWith("READY"),"critical ready state remains visible at minimum width");
            System.out.println("PASS responsive stamina HUD across 9 widths and 54 state combinations");
        }finally{g.dispose();}
    }
}
