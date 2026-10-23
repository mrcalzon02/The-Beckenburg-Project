package org.beckenburg;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.imageio.ImageIO;
public final class WorldLoreCapture {
    public static void main(String[] args)throws Exception{
        LevelVisualCapture.inspection();Path out=Path.of("development/levels/previews/common-archive");Files.createDirectories(out);
        var scene=new SceneRenderer();scene.setRenderSize(1120,700);scene.setPsxPreset(PsxPipeline.Preset.OFF);
        scene.x=36;scene.z=14;scene.yaw=Math.PI;scene.pitch=-.10;scene.lamp=true;scene.drawCastle(new CastleHubMap(),0,true);
        ImageIO.write(scene.image(),"PNG",out.resolve("castle-records-desk-and-lorekeeper.png").toFile());
        for(int[] size:new int[][]{{1120,700},{640,360}})for(int page:new int[]{0,1,4,8,13}){
            var im=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_RGB);var g=im.createGraphics();g.drawImage(scene.image(),0,0,size[0],size[1],null);
            var reader=new CastleArchive.Reader();reader.open();reader.turn(page);CastleArchive.draw(g,size[0],size[1],reader);g.dispose();
            ImageIO.write(im,"PNG",out.resolve("archive-"+size[0]+"-page-"+page+".png").toFile());
        }
        for(var site:List.of(HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL)){
            var im=new BufferedImage(900,600,BufferedImage.TYPE_INT_RGB);var g=im.createGraphics();SiteStories.draw(g,900,600,new HouseMap(site),"EMERGENCE");g.dispose();
            ImageIO.write(im,"PNG",out.resolve("survivor-record-"+site.name().toLowerCase(Locale.ROOT)+".png").toFile());
        }
        System.out.println("PASS Castle archive/lorekeeper and small/large reader captures");
    }
}
