package org.beckenburg;
import java.awt.image.BufferedImage;

/** Gameplay viewfinder contract and physical camera screen pixel acceptance. */
public final class CameraViewfinderTest {
    private static void require(boolean good,String why){
        if(!good)throw new AssertionError(why);
    }
    public static void main(String[] args){
        require(ToolBeltHud.stillCamera("PHOTO_CAMERA"),"still camera has separate shutter");
        require(ToolBeltHud.cameraView("VIDEO_CAMERA"),"video camera needs a live held LCD");
        require(!ToolBeltHud.shutterAllowed("PHOTO_CAMERA",false),
            "LMB cannot expend film before R raises the still camera");
        ToolBeltHud.Grip grip=new ToolBeltHud.Grip();
        grip.hold();
        require(ToolBeltHud.shutterAllowed("PHOTO_CAMERA",grip.held()),
            "LMB may expose film while the viewfinder is aimed");
        grip.release();
        require(!ToolBeltHud.shutterAllowed("PHOTO_CAMERA",grip.held()),
            "lowering or changing tools disarms the shutter");
        require(!ToolBeltHud.shutterAllowed("VIDEO_CAMERA",true),
            "deployed video equipment cannot consume photo film");
        BufferedImage scene=new BufferedImage(320,200,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<200;y++)for(int x=0;x<320;x++)
            scene.setRGB(x,y,x<160?0xff00b3:0x00ffcc);
        for(var tier:EquipmentTiers.Tier.values()){
            BufferedImage panel=HeldInstrumentMesh.image("PHOTO_CAMERA",tier,null,scene);
            int tint=0;
            for(int y=0;y<panel.getHeight();y++)
                for(int x=0;x<panel.getWidth();x++){
                    int color=panel.getRGB(x,y)&0xffffff;
                    if(color==0xff00b3||color==0x00ffcc)tint++;
                }
            require(tint>10,"physical rear photo-camera LCD lacks real supplied scene pixels at tier "+tier+": "+tint);
        }
        BufferedImage video=HeldInstrumentMesh.image("VIDEO_CAMERA",EquipmentTiers.Tier.I,null,scene);
        int videoPixels=0;
        for(int y=0;y<video.getHeight();y++)for(int x=0;x<video.getWidth();x++){
            int color=video.getRGB(x,y)&0xffffff;
            if(color==0xff00b3||color==0x00ffcc)videoPixels++;
        }
        require(videoPixels>10,"physical video-camera rear LCD lacks scene pixels: "+videoPixels);
        System.out.println("PASS camera R aim / LMB shutter separation and real rear LCD pixels");
    }
}
