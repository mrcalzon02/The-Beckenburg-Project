package org.beckenburg;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
/** Controlled runtime optics fixtures, not a claim of a completed human mission. */
public final class OpticalEvidenceCapture {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static final Path OUT=Path.of("development/levels/previews/optical-evidence");
    static BufferedImage capture(SceneRenderer scene,HouseMap map,String name)throws Exception{
        scene.drawNetwork(map,List.of(),-1,1.1);
        var copy=new BufferedImage(scene.width(),scene.height(),BufferedImage.TYPE_INT_RGB);copy.setData(scene.image().getData());
        ImageIO.write(copy,"PNG",OUT.resolve(name+".png").toFile());return copy;
    }
    static int green(BufferedImage image){int count=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){int c=image.getRGB(x,y),r=(c>>16)&255,g=(c>>8)&255,b=c&255;if(g>r*1.25&&g>b*1.12&&g>45)count++;}return count;}
    public static void main(String[] args)throws Exception{
        LevelVisualCapture.inspection();Files.createDirectories(OUT);var map=BlackpineLevel.map();double floor=map.floorY(0);
        var scene=new SceneRenderer();scene.setRenderSize(800,500);scene.setPsxPreset(PsxPipeline.Preset.OFF);
        scene.x=48.5;scene.z=32.3;scene.eyeHeight=floor+1.62;scene.yaw=0;scene.pitch=Math.atan2(.012-1.62,1.7);
        check(map.traversable(scene.x,floor,scene.z),"capture camera stands on real floor");
        scene.roomLights=new RoomLighting();scene.roomLights.acceptSnapshot("000000000000");scene.roomLightTick=11;
        scene.lamp=false;scene.uvMarks=List.of(new UvEvidenceMarks.Mark(0,0,48.5,floor+.012,34,10));scene.uvTick=11;
        var off=capture(scene,map,"uv-off-controlled-runtime");scene.uvLamp=true;
        var on=capture(scene,map,"uv-on-controlled-runtime");check(green(on)>green(off)+100,"UV actually reveals fluorescent surface pixels");
        int purple=0;for(int yy=0;yy<on.getHeight();yy++)for(int xx=0;xx<on.getWidth();xx++){int color=on.getRGB(xx,yy),red=(color>>16)&255,green=(color>>8)&255,blue=color&255;if(blue>red*1.15&&blue>green*1.3&&blue>12)purple++;}
        check(purple>300,"UV must project a visibly purple pool on real surfaces: "+purple);
        ImageIO.write(HunterPhotography.snapshot(on,EquipmentTiers.Tier.II),"PNG",OUT.resolve("uv-actual-photograph.png").toFile());
        ImageIO.write(UvSurfaceDecal.texture(),"PNG",Path.of("assets/runtime/level-textures/uv-residue-splat.png").toFile());
        // GPU bridge receives the same real purple beam; no hardware context is claimed.
        Class<?> point=Class.forName("org.beckenburg.SceneRenderer$Point");var ctor=point.getDeclaredConstructor(double.class,double.class,double.class);ctor.setAccessible(true);
        var gpu=SceneRenderer.class.getDeclaredMethod("gpuLitVertex",point,double.class,double.class,int.class,int.class);gpu.setAccessible(true);
        Object at=ctor.newInstance(48.6,floor+.05,34);Object vertex=gpu.invoke(scene,at,0.,0.,-1,0);
        check(((Number)LevelSceneSource.read(vertex,"b")).doubleValue()>((Number)LevelSceneSource.read(vertex,"r")).doubleValue()+.05,"GPU UV vertex output is purple");
        scene.uvLamp=false;scene.pitch=0;scene.lamp=true;
        Object target=ctor.newInstance(48.5,scene.eyeHeight,36.3);
        double weak=((Number)LevelSceneSource.read(gpu.invoke(scene,target,0.,0.,-1,0),"r")).doubleValue();
        scene.setLampHardware(EquipmentTiers.Tier.I,false);
        double paid=((Number)LevelSceneSource.read(gpu.invoke(scene,target,0.,0.,-1,0),"r")).doubleValue();
        check(paid>weak+.1,"GPU receives stronger purchased beam, not just CPU raster");
        capture(scene,map,"purchased-tier-I-controlled-runtime");scene.lampReach=7.5;scene.lampIntensity=.72;scene.lampWidth=.44;
        capture(scene,map,"default-flashlight-controlled-runtime");scene.lamp=false;
        scene.pitch=Math.atan2(.9-1.62,1.7);scene.dotsPositions.put(0,new LanDotsProjectors.Placement(0,0,48.5,floor,32.8,EquipmentTiers.Tier.I));
        var roomDots=capture(scene,map,"dots-room-surfaces-controlled-runtime");
        scene.dotsPassY=floor;scene.dotsPassRoom=0;scene.dotsPassX=48.5;scene.dotsPassZ=34;scene.dotsPassUntil=14;
        var dots=capture(scene,map,"dots-intercepted-rays-controlled-runtime");check(green(dots)>20,"actual intercepted rays render");
        int dotChanges=0;for(int yy=0;yy<dots.getHeight();yy++)for(int xx=0;xx<dots.getWidth();xx++)if(dots.getRGB(xx,yy)!=roomDots.getRGB(xx,yy))dotChanges++;
        check(dotChanges>30,"ghost interception transfers the same source pattern from room to nearer body");
        check(green(HunterPhotography.snapshot(dots,EquipmentTiers.Tier.II))>20,"photograph preserves visible DOTS");
        ImageIO.write(HunterPhotography.snapshot(dots,EquipmentTiers.Tier.II),"PNG",OUT.resolve("dots-actual-photograph.png").toFile());
        scene.dotsPassRoom=-1;scene.dotsPositions.clear();
        var station=new VanSensorStations.Station(1,0,"LASER_GRID",0,48.5,32.8,EquipmentTiers.Tier.II);scene.sensorPositions.put(1,station);
        var straight=capture(scene,map,"laser-straight-controlled-runtime");scene.laserContacts.put(1,new StructuredLightVolume.Contact(1,0,48.5,floor,34,11));
        var laser=capture(scene,map,"laser-spiral-controlled-runtime");int changed=0;for(int yy=0;yy<laser.getHeight();yy++)for(int xx=0;xx<laser.getWidth();xx++)if(laser.getRGB(xx,yy)!=straight.getRGB(xx,yy))changed++;check(changed>100,"real laser intersection visibly distorts the photographed field");ImageIO.write(HunterPhotography.snapshot(laser,EquipmentTiers.Tier.II),"PNG",OUT.resolve("laser-actual-photograph.png").toFile());
        System.out.println("PASS actual UV off/on pixels and purple GPU bridge, stronger paid GPU beam, projected DOTS and photographed runtime effects; native OpenGL acceptance pending");
    }
}
