package org.beckenburg;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.Vec;

/** Actual terrain support, complete parcel seams, cellar holes and swept arrival acceptance. */
public final class HouseRebuildLandscapeTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        var old=HouseRebuild.graybox();var oldGround=old.worldSurface().support(8,5,0,1);
        check(oldGround!=null&&Math.abs(old.groundHeight(8,5,12)-oldGround.y())>.6,"Original mismatch reproduction lost");
        System.out.println("REPRO original pilot outdoor walking="+old.groundHeight(8,5,12)+" rendered terrain="+oldGround.y());
        var map=HouseRebuildLandscape.map();var level=map.level();var terrain=map.worldSurface();
        var base=HouseRebuildFittings.source();
        check(level.props().equals(base.props())&&level.stairs().equals(base.stairs())&&level.voids().equals(base.voids()),"Native fittings/stairs/voids changed");
        check(level.dock().equals(base.dock()),"Canonical van moved");
        var blockedHome=map.van().world(8,-7.3);
        check(old.traversable(blockedHome.x(),0,blockedHome.z()),"Original missing-neighbour-solid reproduction changed");
        check(!map.traversable(blockedHome.x(),0,blockedHome.z()),"Rendered neighbouring house has no solid");
        double low=1,high=-1;int samples=0;
        for(double x=6.5;x<27;x+=.5)for(double z=3;z<25;z+=.5){
            if(x>=12&&x<=21.6&&z>=10&&z<=20.5)continue;
            var s=terrain.support(x,z,0,1);check(s!=null,"Landscape has a support hole");
            check(Math.abs(map.groundHeight(x,z,12)-s.y())<1e-8,"Feet differ from native triangle");
            low=Math.min(low,s.y());high=Math.max(high,s.y());samples++;
        }
        check(high-low>.08,"Landscape still flat");
        check(terrain.support(13.1,12,0,1)==null,"Cellar capped by world terrain");
        var deck=SceneRenderer.class.getDeclaredMethod("levelDeck",List.class,LevelDefinition.class,LevelDefinition.Room.class,boolean.class);
        deck.setAccessible(true);var faces=new ArrayList<>();deck.invoke(null,faces,level,level.room(12),false);deck.invoke(null,faces,level,level.room(13),false);
        check(faces.isEmpty(),"Nominal outdoor rectangles still render above real terrain");
        check(!map.clearLineOfSight(9,.2,6,9,-.3,6),"Terrain optical surface absent");
        var polygon=HouseRebuildLandscape.EDGE;int seams=0;
        for(int i=0;i<polygon.length;i++){
            var a=polygon[i];var b=polygon[(i+1)%polygon.length];double dx=b[0]-a[0],dz=b[1]-a[1],length=Math.hypot(dx,dz),nx=-dz/length,nz=dx/length;
            for(double t=0;t<1;t+=.025){
                double x=a[0]+dx*t,z=a[1]+dz*t,y=HouseRebuildLandscape.height(terrain,x,z);
                check(!map.traversable(x,y,z),"Fence player seam "+i+" "+t);
                check(!map.traversable(x,y,z,HunterPosture.CROUCH_HEIGHT),"Fence crouched seam");
                check(!map.clearObjectPath(x-nx*.4,y+.6,z-nz*.4,x+nx*.4,y+.6,z+nz*.4,.03,.03),"Fence object seam");
                seams++;
            }
        }
        var audit=LevelTraversalAudit.inspect(map);check(audit.passed(),"Swept routes "+audit.findings());
        for(var room:level.rooms())check(map.traversable(room.spawn().x(),room.spawn().y(),room.spawn().z())||room.id()<12,"Outdoor spawn unsupported");
        // A varying source height must reject the former phantom zero plane.
        check(map.traversable(23.5,map.groundHeight(23.5,7,12),7),"Garden ground not walkable");
        if(args.length>0){
            var out=Path.of(args[0]);Files.createDirectories(out);var r=new SceneRenderer();r.setRenderSize(1000,625);r.lamp=true;
            r.weatherModule().setTimeOfDay(.42);r.weatherModule().pauseClock(true);
            double[][] views={{18.2,28.5,1.62,.827,-.05},{9.5,17,1.62,2.2,-.08},{28,45,24,-2.727,-.65},{20,25.5,1.62,Math.PI,-.06},{9,6,.92,1.7,-.06}};
            String[] names={"canonical-van-arrival","garden-side-passage","rejected-high-overview","house-approach","garden-crouched"};
            for(int i=0;i<views.length;i++){var v=views[i];double ground=map.groundHeight(v[0],v[1],12);
                if(i!=2)check(map.traversable(v[0],ground,v[1],i==4?HunterPosture.CROUCH_HEIGHT:HunterPosture.STANDING_HEIGHT),"Review camera inside actual solid "+names[i]);
                r.x=v[0];r.z=v[1];r.eyeHeight=v[2]+(i==2?0:ground);r.yaw=v[3];r.pitch=v[4];r.drawNetwork(map,List.of(),-1,0);ImageIO.write(r.image(),"png",out.resolve(names[i]+"-runtime.png").toFile());}
        }
        System.out.println("PASS House landscape "+terrain.triangleCount()+" source triangles; "+samples+" shared foot/mesh samples, relief="+(high-low)+"; "+seams+" standing/crouched/body fence seam checks; all15zones/required sockets swept at "+audit.positions()+" positions; real cellar aperture and unchanged canonical van/fittings.");
    }
}
