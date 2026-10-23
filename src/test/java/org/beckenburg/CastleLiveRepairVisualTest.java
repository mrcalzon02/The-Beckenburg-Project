package org.beckenburg;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;

/** Store actual renderer frames as visual acceptance evidence for the current repairs. */
public final class CastleLiveRepairVisualTest {
    private static final Path DIR=Path.of("build","e2e","castle-repairs");
    private static void capture(SceneRenderer view,CastleHubMap map,String name)throws Exception{
        view.drawCastle(map,1.4,!name.endsWith("-dark"));
        var image=view.image();
        int first=image.getRGB(0,0);boolean variation=false;
        for(int y=0;y<image.getHeight()&&!variation;y+=10)
            for(int x=0;x<image.getWidth();x+=10)
                if(image.getRGB(x,y)!=first){variation=true;break;}
        if(!variation)throw new AssertionError(name+" rendered uniform/blank frame");
        ImageIO.write(image,"png",DIR.resolve(name+".png").toFile());
        System.out.println("CAPTURE "+name+" "+image.getWidth()+"x"+image.getHeight());
    }
    public static void main(String[] args)throws Exception{
        Files.createDirectories(DIR);
        var map=new CastleHubMap();
        var view=new SceneRenderer();view.setRenderSize(640,400);
        view.x=22.5;view.z=19.5;view.eyeHeight=1.62;
        view.yaw=Math.PI;view.pitch=-.14;
        capture(view,map,"network-workstation");
        capture(view,map,"network-workstation-dark");
        view.x=17.5;view.z=27;view.eyeHeight=3.30;
        view.yaw=Math.PI;view.pitch=-.02;
        capture(view,map,"stairs-and-railing");
        view.x=14.5;view.z=8.5;view.eyeHeight=5.02;
        view.yaw=Math.PI;view.pitch=-.09;
        capture(view,map,"upper-library-new-lights");
        view.x=13.5;view.z=26;view.eyeHeight=1.62;
        view.yaw=Math.PI;view.pitch=.18;
        capture(view,map,"castle-rooflines");
        view.x=13.5;view.z=20.5;view.eyeHeight=1.62;
        view.yaw=0;view.pitch=-.10;
        capture(view,map,"courtyard-van-and-castle-towers");
        view.x=35;view.z=24.5;view.eyeHeight=1.62;
        view.yaw=-Math.PI/2;view.pitch=-.12;
        capture(view,map,"castle-stocked-equipment-warehouse");
        view.x=15.5;view.z=31.3;view.eyeHeight=1.62;
        view.yaw=0;view.pitch=-.07;
        capture(view,map,"castle-entrance-leaf");
        view.x=36.0;view.z=26.0;view.eyeHeight=1.62;
        view.yaw=0;view.pitch=-.13;
        capture(view,map,"east-wing-brighter-lights");
        capture(view,map,"east-wing-brighter-lights-dark");
        // Source cars and noninteractive van, viewed from inside the gate.
        view.x=15.5;view.z=32.5;view.eyeHeight=1.62;
        view.yaw=0;view.pitch=-.12;
        capture(view,map,"castle-static-van-and-textured-psx-cars");
        System.out.println("PASS castle source-rendered visual repair captures");
    }
}
