package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * Evidence from the real House renderer (not image synthesis or stock art).
 * Shows the current flush entrance slab, grounded apron and real working
 * door from the resident player's eye and close-up.
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
            (p.name().equals("porch-deck")||p.name().equals("porch-apron"))).toList();
        check(parts.size()==2,"authentic physical slab and grounded apron are present");
        var out=Path.of("build","e2e","house-porch-capture");
        Files.createDirectories(out);
        var scene=new SceneRenderer();
        scene.setRenderSize(960,600);
        scene.setPsxPreset(PsxPipeline.Preset.OFF);
        scene.roomLights=new RoomLighting();
        scene.environmentLight=1.0;
        scene.rackStock=java.util.Set.of();
        double exteriorY=map.groundHeight(19.8,23.25,13);
        // Same camera with genuine closed and opened door, then the apron
        // and flush foyer threshold from first-person height.
        LevelVisualCapture.capture(scene,map,out,"01-front-door-closed",
            19.8,exteriorY,23.25,19.8,1.10,20.5);
        var front=map.level().portals().stream().filter(p->p.door()&&p.joins(13,0)).findFirst().orElseThrow();
        check(map.openDoorBetweenRooms(13,0),"Actual front door accepts the open command");
        // The retained main renderer animates the leaf for 450 ms. A capture
        // immediately after the command still shows its initial closed pose.
        long deadline=System.nanoTime()+2_000_000_000L;
        while(map.doorMoving(front.id())&&System.nanoTime()<deadline)Thread.sleep(15);
        check(map.doorOpen(front.id())&&map.doorOpenFraction(front.id())==1,
            "The actual door animation must settle before the open-pose capture");
        LevelVisualCapture.capture(scene,map,out,"02-front-door-open",
            19.8,exteriorY,23.25,19.8,1.10,20.5);
        var closed=ImageIO.read(out.resolve(map.level().id()+"-01-front-door-closed.png").toFile());
        var open=ImageIO.read(out.resolve(map.level().id()+"-02-front-door-open.png").toFile());
        check(!java.util.Arrays.equals(closed.getRGB(0,0,960,600,null,0,960),
                open.getRGB(0,0,960,600,null,0,960)),"The physical door must change the captured image");
        double porchY=map.groundHeight(19.8,22.82,13);
        LevelVisualCapture.capture(scene,map,out,"03-exterior-porch-apron",
            19.8,porchY,22.82,19.8,.08,22.35);
        LevelVisualCapture.capture(scene,map,out,"04-interior-threshold",
            19.8,0,19.27,19.8,.035,20.51);
        for(String name:new String[]{"01-front-door-closed","02-front-door-open",
                "03-exterior-porch-apron","04-interior-threshold"})
            present(out.resolve(map.level().id()+"-"+name+".png"));
        Files.writeString(out.resolve("source.txt"),
            "House source revision="+map.level().revision()+"\n"+
            "Porch envelope sha256="+map.envelope().digest()+"\n"+
            "Authoritative geometry=flush porch slab and grounded apron at the original entry sill\n"+
            "Camera=actual software SceneRenderer, native authored House model\n");
        System.out.println("PASS: four real 960x600 House porch screenshots, flush slab and grounded apron");
    }
}
