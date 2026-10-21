package org.beckenburg;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
public final class BlackpineStoryCapture {
    public static void main(String[] args)throws Exception{
        LevelVisualCapture.inspection();var map=BlackpineLevel.map();for(var p:map.level().portals())if(p.door())map.openDoorBetweenRooms(p.a(),p.b());
        var s=new SceneRenderer();s.setRenderSize(1120,700);s.setPsxPreset(PsxPipeline.Preset.OFF);s.roomLights=new RoomLighting();s.environmentLight=1;s.rackStock=Set.of();
        var out=Path.of("development/levels/previews/blackpine-story");Files.createDirectories(out);
        view(s,map,out,"01-crew-beds-stove-and-boards",4,11.8,21.6,14.8,.8,17.5);
        view(s,map,out,"02-payroll-pencils-and-papers",9,20,7.5,19.7,.90,5.7);
        view(s,map,out,"03-foreman-photo-and-journal",6,27.8,14.5,26.2,.9,13.5);
        view(s,map,out,"04-recent-shelter",7,36.5,9.2,38.7,.25,10.4);
        view(s,map,out,"05-washhouse-oil-and-water",5,10.5,40.6,8.5,1,36.7);
        view(s,map,out,"06-repair-shed-power-service",8,47.2,51.6,45.1,.7,47.8);
        view(s,map,out,"07-kitchen-heat-and-dry-stores",3,18.4,34.8,20.6,1.2,31);
        for(String id:List.of("PAYROLL","SHELTER","SERVICE")){
            var im=new BufferedImage(900,600,BufferedImage.TYPE_INT_RGB);var g=im.createGraphics();SiteStories.draw(g,900,600,map,id);g.dispose();
            ImageIO.write(im,"PNG",out.resolve("read-"+id.toLowerCase(Locale.ROOT)+".png").toFile());
        }
    }
    static void view(SceneRenderer s,HouseMap m,Path out,String name,int room,double x,double z,double tx,double ty,double tz)throws Exception{
        LevelVisualCapture.capture(s,m,out,name,x,m.floorY(room),z,tx,m.floorY(room)+ty,tz);
    }
}
