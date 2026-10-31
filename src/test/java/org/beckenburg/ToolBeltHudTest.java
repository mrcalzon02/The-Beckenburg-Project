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
        System.out.println("ToolBeltHudTest PASS");
    }
    private static void require(boolean condition,String description){
        if(!condition)throw new AssertionError(description);
    }
}
