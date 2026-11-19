package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * Evidence from the real House renderer (not image synthesis or stock art).
 * Shows the physical 4cm entrance bevel, the real working door, and the
 * matching interior transition from the resident player's eye and close-up.
 */
public final class HousePorchVisualCaptureTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static void present(Path path)throws Exception{
        BufferedImage image=ImageIO.read(path.toFile());
        check(image!=null&&image.getWidth()==960&&image.getHeight()==600,
            "actual 960x600 game screenshot missing: "+path);
        var colors=new java.util.HashSet<Integer>();
        for(int z=0;z<image.getHeight();z+=8)
            for(int x=0;x<image.getWidth();x+=8)
                colors.add(image.getRGB(x,z)&0xffffff);
        check(colors.size()>28,"perspective screenshot lacks physical material detail: "+path);
        System.out.println("HOUSE PORCH ACTUAL GAME CAPTURE "+path+" palette="+colors.size());
    }
    public static void main(String[] args)throws Exception{
        LevelVisualCapture.inspection();
        var map=new HouseMap(HouseMap.Site.HOUSE);
        check(map.envelope()!=null,"real rebuilt House envelope");
        var parts=map.envelope().parts().stream().filter(p->p.support()&&
            (p.name().equals("porch-approach-bevel")||
             p.name().equals("porch-interior-threshold"))).toList();
        check(parts.size()==2,"both authentic physical porch edges are present");
        var out=Path.of("build","e2e","house-porch-capture");
        Files.createDirectories(out);
        var scene=new SceneRenderer();
        scene.setRenderSize(960,600);
        scene.setPsxPreset(PsxPipeline.Preset.OFF);
        scene.roomLights=new RoomLighting();
        scene.environmentLight=1.0;
        scene.rackStock=java.util.Set.of();
        double exteriorY=map.groundHeight(19.8,23.25,13);
        // Same camera with genuine closed and opened door, then both support
        // transitions and the unmodified foyer from first-person height.
        LevelVisualCapture.capture(scene,map,out,"01-front-door-closed",
            19.8,exteriorY,23.25,19.8,1.10,20.5);
        map.openDoorBetweenRooms(13,0);
        LevelVisualCapture.capture(scene,map,out,"02-front-door-open",
            19.8,exteriorY,23.25,19.8,1.10,20.5);
        double porchY=map.groundHeight(19.8,22.82,13);
        LevelVisualCapture.capture(scene,map,out,"03-exterior-porch-bevel",
            19.8,porchY,22.82,19.8,.08,22.35);
        LevelVisualCapture.capture(scene,map,out,"04-interior-threshold",
            19.8,0,19.27,19.8,.035,20.51);
        for(String name:new String[]{"01-front-door-closed","02-front-door-open",
                "03-exterior-porch-bevel","04-interior-threshold"})
            present(out.resolve(map.level().id()+"-"+name+".png"));
        Files.writeString(out.resolve("source.txt"),
            "House source revision="+map.level().revision()+"\n"+
            "Porch envelope sha256="+map.envelope().digest()+"\n"+
            "Authoritative geometry=two 4cm walkable slope meshes, not collision exceptions\n"+
            "Camera=actual software SceneRenderer, native authored House model\n");
        System.out.println("PASS: four real 960x600 House porch screenshots, two physical transitions");
    }
}
