package org.beckenburg;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;

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
        // Every actual catalog item must show its approved real model icon, not
        // a silent text label caused by a missing legacy base preview.
        for(var tool:EquipmentRegistry.all())
            require(ToolBeltHud.itemIcon(tool.id(),Map.of(),48)!=null,
                "missing individual Tier-I hotbar image: "+tool.id());
        require(ToolBeltHud.itemIcon(FieldEquipment.EMPTY,Map.of(),48)==null,
            "empty dropped slot must not show a phantom item");
        require(ToolBeltHud.itemIcon("EMF_READER",
                Map.of("EMF_READER",EquipmentTiers.Tier.III),54)
                ==ItemIconAtlas.getTier("emf_reader",EquipmentTiers.Tier.III,54),
            "hotbar must use the identical singular image as the equipment catalog");
        BufferedImage tierOne=new BufferedImage(850,555,BufferedImage.TYPE_INT_ARGB);
        BufferedImage tierThree=new BufferedImage(850,555,BufferedImage.TYPE_INT_ARGB);
        Graphics2D first=tierOne.createGraphics(),third=tierThree.createGraphics();
        try{
            String[] belt={"EMF_READER","THERMOMETER","SPIRIT_BOX","DOTS_PROJECTOR"};
            ToolBeltHud.draw(first,850,555,belt,0,null,false,false,false,0,0,
                Map.of("EMF_READER",EquipmentTiers.Tier.I));
            ToolBeltHud.draw(third,850,555,belt,0,null,false,false,false,0,0,
                Map.of("EMF_READER",EquipmentTiers.Tier.III));
        }finally{first.dispose();third.dispose();}
        require(changedPixels(tierOne,tierThree,273,471,72,72)>20,
            "switching tier must repaint the actual first-slot device image");
        require(changedPixels(tierOne,tierThree,350,471,72,72)==0,
            "changing the first-slot tier must not alter the second-slot image");
        require(ToolBeltHud.focusedReadingX(850,0)==712
            &&ToolBeltHud.focusedReadingX(850,1)==425
            &&ToolBeltHud.focusedReadingY(555,1)<ToolBeltHud.focusedReadingY(555,0),
            "live readout travels from the right into the center with the tool");
        // A current device reading is visible only while inspection is held.
        BufferedImage idle=new BufferedImage(850,555,BufferedImage.TYPE_INT_ARGB);
        BufferedImage active=new BufferedImage(850,555,BufferedImage.TYPE_INT_ARGB);
        Graphics2D idleGraphics=idle.createGraphics(),activeGraphics=active.createGraphics();
        try{
            String[] belt={"EMF_READER","THERMOMETER","SPIRIT_BOX","DOTS_PROJECTOR"};
            ToolBeltHud.draw(idleGraphics,850,555,belt,0,"EMF 6/10",false,false,false,0);
            ToolBeltHud.draw(activeGraphics,850,555,belt,0,"EMF 6/10",true,false,false,0);
        }finally{idleGraphics.dispose();activeGraphics.dispose();}
        require((idle.getRGB(425,150)>>>24)==0
            &&(active.getRGB(425,150)>>>24)>0,
            "authoritative reading only visible during held use");
        System.out.println("ToolBeltHudTest PASS");
    }
    private static int changedPixels(BufferedImage a,BufferedImage b,int x,int y,int w,int h){
        int changed=0;
        for(int row=y;row<y+h;row++)for(int col=x;col<x+w;col++)
            if(a.getRGB(col,row)!=b.getRGB(col,row))changed++;
        return changed;
    }
    private static void require(boolean condition,String description){
        if(!condition)throw new AssertionError(description);
    }
}
