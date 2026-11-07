package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Actual source-mesh fit and swept circulation, before any dispatched-map promotion. */
public final class HouseRebuildFittingsTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        var map=HouseRebuildFittings.map();var level=map.level();var base=HouseRebuild.source();
        check(base.props().isEmpty()&&base.colliders().isEmpty(),"Empty graybox changed");
        check(level.rooms().equals(base.rooms())&&level.stairs().equals(base.stairs())&&level.voids().equals(base.voids()),"Structural authority changed");
        check(level.sockets().equals(base.sockets())&&level.dock().equals(base.dock()),"Services/terrain/van changed");
        check(level.props().size()==5&&level.colliders().size()==5,"Missing measured fixtures");
        for(int i=0;i<5;i++){
            var p=level.props().get(i);var solid=level.colliders().get(i);var r=level.room(p.room());
            double[] b=SceneRenderer.castleBounds(p.mesh());double c=Math.cos(p.yaw()),s=Math.sin(p.yaw());
            check(p.scale()==1&&Math.abs(p.y()+b[1]-r.y())<1e-8,"Native floor contact "+p.mesh());
            check(Math.abs(p.x()+b[0]*c-b[2]*s-solid.x())<1e-8&&Math.abs(p.z()+b[0]*s+b[2]*c-solid.z())<1e-8,"Pivot/collider mismatch");
            double hx=Math.abs(c)*b[3]/2+Math.abs(s)*b[5]/2,hz=Math.abs(s)*b[3]/2+Math.abs(c)*b[5]/2;
            check(solid.x()-hx>=r.x0()+.079&&solid.x()+hx<=r.x1()-.079&&solid.z()-hz>=r.z0()+.079&&solid.z()+hz<=r.z1()-.079,"Wall penetration "+p.mesh());
            check(map.traversable(r.spawn().x(),r.spawn().y(),r.spawn().z()),"Blocked standing anchor "+r.name());
            for(var door:level.portalsFrom(p.room()))if(door.door()){
                double dx=Math.max(0,Math.abs(door.x()-solid.x())-hx),dz=Math.max(0,Math.abs(door.z()-solid.z())-hz);
                check(Math.hypot(dx,dz)>door.width()/2+.21,"Door approach blocked "+p.mesh());
            }
            for(int j=0;j<i;j++){
                var q=level.colliders().get(j);if(Math.abs(q.y()-solid.y())>.1)continue;
                double qx=Math.abs(Math.cos(q.yaw()))*q.halfX()+Math.abs(Math.sin(q.yaw()))*q.halfZ();
                double qz=Math.abs(Math.sin(q.yaw()))*q.halfX()+Math.abs(Math.cos(q.yaw()))*q.halfZ();
                check(Math.abs(q.x()-solid.x())>=qx+hx||Math.abs(q.z()-solid.z())>=qz+hz,"Fixtures intersect");
            }
            System.out.println(p.mesh()+" source-size "+b[3]+" x "+b[4]+" x "+b[5]+" floor="+r.y());
        }
        var audit=LevelTraversalAudit.inspect(map);
        check(audit.passed(),"Swept circulation: "+audit.findings());
        if(args.length>0){
            var out=Path.of(args[0]);Files.createDirectories(out);var renderer=new SceneRenderer();renderer.setRenderSize(1000,625);renderer.lamp=true;
            double[][] views={{15.3,12.8,1.62,Math.PI/2,-.12},{19.2,12.25,4.42,Math.PI,-.25},{17.5,13.3,-.78,2.55,-.12},{19,12.25,4.42,1.91,-.20}};
            String[] names={"kitchen","bathroom","laundry","bath"};
            for(int i=0;i<views.length;i++){var v=views[i];renderer.x=v[0];renderer.z=v[1];renderer.eyeHeight=v[2];renderer.yaw=v[3];renderer.pitch=v[4];renderer.drawNetwork(map,List.of(),-1,0);ImageIO.write(renderer.image(),"png",out.resolve(names[i]+"-fixture-fit-runtime.png").toFile());}
        }
        System.out.println("PASS5 full-scale native fixtures; preserved pivots/terrain/stairs/services/van; swept all15zones and functional sockets: "+audit.positions()+" positions. Construction candidate only.");
    }
}
