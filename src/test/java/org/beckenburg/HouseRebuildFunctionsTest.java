package org.beckenburg;
import java.util.*;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Whole household fit, source supports, actual use-space and swept required circulation. */
public final class HouseRebuildFunctionsTest {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    public static void main(String[] args)throws Exception{
        var assembly=HouseRebuildFunctions.assembly();var map=new HouseMap(HouseMap.Site.HOUSE,assembly.level());var base=HouseRebuildEnvelope.source();
        check(map.level().stairs().equals(base.stairs())&&map.level().voids().equals(base.voids())&&map.level().portals().equals(base.portals())&&map.level().dock().equals(base.dock()),"Building/van changed");
        for(var place:assembly.placements()){
            var p=place.prop();var r=map.level().room(p.room());var b=SceneRenderer.castleBounds(p.mesh());var solid=place.solid();
            check(p.scale()==1,"Miniature fixture "+place.key());
            check(Math.abs(p.y()+b[1]-solid.y())<1e-7,"Floor/source-pivot mismatch "+place.key());
            double hx=Math.abs(Math.cos(p.yaw()))*b[3]/2+Math.abs(Math.sin(p.yaw()))*b[5]/2,hz=Math.abs(Math.sin(p.yaw()))*b[3]/2+Math.abs(Math.cos(p.yaw()))*b[5]/2;
            check(solid.x()-hx>=r.x0()+.079&&solid.x()+hx<=r.x1()-.079&&solid.z()-hz>=r.z0()+.079&&solid.z()+hz<=r.z1()-.079,"Wall/zone penetration "+place.key());
            if(!r.outside())check(p.y()+b[1]+b[4]<=r.y()+r.height()-.04+1e-6,"Ceiling penetration "+place.key());
            for(var q:map.level().props())if(q!=p&&q.room()==p.room()){
                var qb=SceneRenderer.castleBounds(q.mesh());double qc=Math.cos(q.yaw()),qs=Math.sin(q.yaw()),qx=q.x()+qb[0]*qc-qb[2]*qs,qz=q.z()+qb[0]*qs+qb[2]*qc;
                double qhx=Math.abs(qc)*qb[3]/2+Math.abs(qs)*qb[5]/2,qhz=Math.abs(qs)*qb[3]/2+Math.abs(qc)*qb[5]/2,qy=q.y()+qb[1];
                if(Math.min(solid.y()+b[4],qy+qb[4])-Math.max(solid.y(),qy)>.005)
                    check(Math.abs(qx-solid.x())>=qhx+hx-.003||Math.abs(qz-solid.z())>=qhz+hz-.003,"Models intersect "+place.key()+" / "+q.mesh());
            }
            for(var hole:map.level().voids())if(Math.abs(hole.y()-r.y())<.01)check(solid.x()+hx<=hole.x0()||solid.x()-hx>=hole.x1()||solid.z()+hz<=hole.z0()||solid.z()-hz>=hole.z1(),"Stair opening occupied "+place.key());
            for(var s:map.level().sockets())if(s.type().equals("ritual")&&s.room()==p.room())check(Math.abs(s.x()-solid.x())>hx+1||Math.abs(s.z()-solid.z())>hz+1,"Ritual reservation occupied "+place.key());
            System.out.println("FIT "+place.key()+" "+p.mesh()+" native="+b[3]+"x"+b[4]+"x"+b[5]+" support="+place.support());
        }
        for(var r:map.level().rooms())check(map.traversable(r.spawn().x(),r.spawn().y(),r.spawn().z()),"Standing anchor inside furniture "+r.name());
        var audit=LevelTraversalAudit.inspect(map);check(audit.passed(),"Required route blocked "+audit.findings());
        check(map.level().props().stream().filter(p->p.mesh().equals("bed_4")||p.mesh().equals("bed_5")).count()==2,"Missing sleeping places");
        if(args.length>0){
            var out=Path.of(args[0]);Files.createDirectories(out);var renderer=new SceneRenderer();renderer.setRenderSize(1000,625);renderer.lamp=true;
            double[][] views={{15.8,18,1.62,-2.5,-.15},{18.6,12.7,1.62,1.8,-.1},{16.1,16.6,4.42,-.5,-.12},{16,13.1,4.42,-1.6,-.1},{18.3,12.4,-.78,1.4,-.1},{18.3,17.6,-.78,-.5,-.1},{24,27.6,1.62,.7,-.2}};
            String[] names={"sitting","dining","parents","child-study","heating-laundry","cellar-store","generator"};
            for(int i=0;i<views.length;i++){var v=views[i];check(map.traversable(v[0],v[2]-1.62,v[1]),"Capture camera blocked "+names[i]);renderer.x=v[0];renderer.z=v[1];renderer.eyeHeight=v[2];renderer.yaw=v[3];renderer.pitch=v[4];renderer.drawNetwork(map,List.of(),-1,0);ImageIO.write(renderer.image(),"png",out.resolve(names[i]+"-runtime.png").toFile());}
        }
        System.out.println("PASS "+assembly.placements().size()+" authored full-scale household fixtures; real tabletop support; all15anchors/zones/required sockets swept at "+audit.positions()+" positions");
    }
}
