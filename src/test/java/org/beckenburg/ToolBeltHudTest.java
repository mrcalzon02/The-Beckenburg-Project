package org.beckenburg;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Headless four-slot interaction presentation regression. */
public final class ToolBeltHudTest {
    public static void main(String[] args){
        require(ToolBeltHud.placeable("VIDEO_CAMERA"),"camera placement");
        require(ToolBeltHud.placeable("DOTS_PROJECTOR"),"projector placement");
        require(ToolBeltHud.placeable("SALT"),"salt placement");
        require(!ToolBeltHud.placeable("THERMOMETER"),"thermometer is handheld");
        require(ToolBeltHud.switchable("UV_LIGHT"),"UV switch");
        require(!ToolBeltHud.switchable("EMF_READER"),"EMF switch");
        require(ToolBeltHud.repeatable("EMF_READER"),"repeat scans");
        require(!ToolBeltHud.repeatable("PHOTO_CAMERA"),"do not auto-consume film");
        ToolBeltHud.Grip grip=new ToolBeltHud.Grip();
        grip.hold();
        for(int i=0;i<9;i++)grip.advance();
        require(grip.held()&&grip.amount()>.95,"raises smoothly");
        grip.release();
        for(int i=0;i<20;i++)grip.advance();
        require(!grip.held()&&grip.amount()<.01,"returns to rest");
        BufferedImage frame=new BufferedImage(850,555,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=frame.createGraphics();
        try{
            ToolBeltHud.draw(g,850,555,
                new String[]{"EMF_READER","THERMOMETER","SPIRIT_BOX","DOTS_PROJECTOR"},
                3,null,true,false,true,135);
        }finally{g.dispose();}
        require((frame.getRGB(456,500)>>>24)!=0,"center belt visible");
        // Only an actively inspected device may expose a current reading above the belt.
        BufferedImage idle=new BufferedImage(850,555,BufferedImage.TYPE_INT_ARGB);
        BufferedImage active=new BufferedImage(850,555,BufferedImage.TYPE_INT_ARGB);
        Graphics2D idleGraphics=idle.createGraphics(),activeGraphics=active.createGraphics();
        try{
            String[] belt={"EMF_READER","THERMOMETER","SPIRIT_BOX","DOTS_PROJECTOR"};
            ToolBeltHud.draw(idleGraphics,850,555,belt,0,"EMF 6/10",false,false,false,0);
            ToolBeltHud.draw(activeGraphics,850,555,belt,0,"EMF 6/10",true,false,false,0);
        }finally{idleGraphics.dispose();activeGraphics.dispose();}
        require((idle.getRGB(425,390)>>>24)==0
            &&(active.getRGB(425,390)>>>24)>0,
            "authoritative reading only visible during held use");
        System.out.println("ToolBeltHudTest PASS");
    }
    private static void require(boolean condition,String description){
        if(!condition)throw new AssertionError(description);
    }
}
