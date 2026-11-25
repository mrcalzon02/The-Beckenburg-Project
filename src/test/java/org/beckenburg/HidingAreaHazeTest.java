package org.beckenburg;
import java.util.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Same sanctuary material, exact cover eligibility, geometry depth and no hunter-state input. */
public final class HidingAreaHazeTest {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    static int draw(int[] pixels,double[] depths,HouseMap map,LevelDefinition.Socket s,double clock,boolean inside){
        var p=s.type().equals("hide")?LevelClosets.world(s,0,0,inside?0:1.9):new LevelDefinition.Vec(s.x(),s.y(),s.z()+(inside?0:1.35));
        double yaw=inside?s.yaw():Math.atan2(s.x()-p.x(),s.z()-p.z()),eye=p.y()+(s.type().equals("hide")?1.4:.8);
        return SafeAreaHaze.drawHiding(pixels,depths,320,200,200,p.x(),eye,p.z(),yaw,0,clock,map);
    }
    private static void checkWoodlandRefuges(HouseMap.Site site){
        var map=new HouseMap(site);
        map.configureHiding(100);
        var refuges=HidingBudget.candidates(map.level());
        check(refuges.size()==3,"Three woodland refuges at "+site);
        for(var socket:refuges){
            var volume=OutdoorHiding.footprint(map.level(),socket);
            check(volume.radius()>.55,"Refuge too small to enter at "+socket.id());
            double insideX=socket.x()+volume.radius()*.70;
            double outsideX=socket.x()+volume.radius()+.05;
            double y=map.groundHeight(insideX,socket.z(),socket.room()),z=socket.z();
            check(map.traversable(insideX,y,z,HunterPosture.CROUCH_HEIGHT),"Usable cover beyond the old center at "+socket.id());
            check(OutdoorHiding.containing(map.level(),insideX,y,z,100)==socket,
                "The visible foliage footprint conceals the player at "+socket.id());
            check(SafeAreaBarrier.protectedAt(map,insideX,y,z),
                "Hunters have protection upon entering "+socket.id());
            check(!SafeAreaBarrier.protectedAt(map,outsideX,y,z),
                "Protection ends at the refuge edge "+socket.id());
            var from=new LevelDefinition.Vec(outsideX,y,z);
            var to=new LevelDefinition.Vec(socket.x()-volume.radius()-.05,y,z);
            check(SafeAreaBarrier.blocks(map,from,to),"Spectral passage crosses a refuge at "+socket.id());
            check(draw(new int[320*200],clearDepth(),map,socket,.4,true)>1000,
                "Authored refuge haze visible from within at "+socket.id());
            map.configureHiding(0);
            check(!SafeAreaBarrier.protectedAt(map,insideX,y,z)&&!SafeAreaBarrier.blocks(map,from,to),
                "Disabled refuge provides no protection at "+socket.id());
            map.configureHiding(100);
        }
    }
    private static double[] clearDepth(){
        var depth=new double[320*200];
        Arrays.fill(depth,Double.POSITIVE_INFINITY);
        return depth;
    }
    public static void main(String[] args)throws Exception{
        var map=HouseRebuildFitout.map();map.configureHiding(100);var closet=HidingBudget.candidates(map.level()).stream().filter(s->s.room()==6).findFirst().orElseThrow();
        int[] original=new int[320*200];Arrays.fill(original,0x18242a);double[] depth=new double[original.length];Arrays.fill(depth,Double.POSITIVE_INFINITY);var before=depth.clone();
        int[] a=original.clone(),same=original.clone(),animated=original.clone();int changed=draw(a,depth,map,closet,0,false);draw(same,depth,map,closet,0,false);draw(animated,depth,map,closet,1.2,false);
        check(changed>1000&&changed<original.length,"Available cupboard haze must visibly occupy its pocket");check(Arrays.equals(a,same)&&!Arrays.equals(a,animated),"Same deterministic animated sanctuary material");check(Arrays.equals(depth,before),"Energy veil must not write opaque world depth");
        int[] inside=original.clone();check(draw(inside,depth,map,closet,.4,true)>1000,"Haze visible from inside actual cupboard");
        Arrays.fill(depth,.10);int[] occluded=original.clone();check(draw(occluded,depth,map,closet,0,false)==0&&Arrays.equals(occluded,original),"Walls/solid doors must occlude hiding haze");
        Arrays.fill(depth,Double.POSITIVE_INFINITY);map.configureHiding(0);int[] disabled=original.clone();check(draw(disabled,depth,map,closet,0,false)==0&&Arrays.equals(disabled,original),"Zero-cover contract has no false safe-location veil");
        for(int density=0;density<=100;density++){
            map.configureHiding(density);for(var s:HidingBudget.candidates(map.level()))check(LanGhostSpatial.hidingPocketAt(map,s.room(),s.x(),s.y(),s.z())==HidingBudget.available(map.level(),s,density),"Ghost exclusion and visible cover eligibility diverge "+density+" / "+s.id());
        }
        var outdoor=new HouseMap(HouseMap.Site.BLACKPINE);outdoor.configureHiding(100);var shrub=HidingBudget.candidates(outdoor.level()).stream().filter(s->s.type().equals("outdoor-hide")).findFirst().orElseThrow();
        check(draw(original.clone(),depth,outdoor,shrub,0,false)>1000,"Outdoor hiding pocket must reuse energy haze");outdoor.configureHiding(0);check(!LanGhostSpatial.hidingPocketAt(outdoor,shrub.room(),shrub.x(),shrub.y(),shrub.z()),"Disabled outdoor cover has no invisible ghost exclusion");
        checkWoodlandRefuges(HouseMap.Site.BLACKPINE);
        checkWoodlandRefuges(HouseMap.Site.HOLLOW_CREEK);
        if(args.length>0){var out=Path.of(args[0]);Files.createDirectories(out);map.configureHiding(100);var r=new SceneRenderer();r.setRenderSize(1000,625);r.lamp=true;r.roomLights=new RoomLighting();
            for(var s:HidingBudget.candidates(map.level())){var p=LevelClosets.world(s,0,0,1.75);r.x=p.x();r.z=p.z();r.eyeHeight=p.y()+1.62;r.yaw=Math.atan2(s.x()-p.x(),s.z()-p.z());r.pitch=-.1;check(map.traversable(p.x(),p.y(),p.z()),"Barrier capture camera blocked "+s.id());r.drawNetwork(map,List.of(),-1,.4);ImageIO.write(r.image(),"png",out.resolve(s.id()+"-energy-runtime.png").toFile());}
            var p=shrub;r.x=p.x();r.z=p.z()+3.1;r.eyeHeight=p.y()+1.0;r.yaw=Math.PI;r.pitch=-.12;outdoor.configureHiding(100);r.drawNetwork(outdoor,List.of(),-1,.4);ImageIO.write(r.image(),"png",out.resolve("outdoor-refuge-energy-runtime.png").toFile());
        }
        System.out.println("PASS same animated sanctuary haze on available cupboard/outdoor refuges,inside/outside/occluded views,unchanged depth,101difficulty settings and zero-cover ghost exclusion");
    }
}
