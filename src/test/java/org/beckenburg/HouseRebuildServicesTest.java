package org.beckenburg;
import java.util.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.*;
/** Actual original radiator support, connected terminals and unchanged full circulation. */
public final class HouseRebuildServicesTest {
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    static boolean onSource(Prop p,Vec point){
        double c=Math.cos(p.yaw()),s=Math.sin(p.yaw()),dx=point.x()-p.x(),dz=point.z()-p.z();double[] at={(dx*c+dz*s)/p.scale(),(point.y()-p.y())/p.scale(),(-dx*s+dz*c)/p.scale()};
        for(var t:GlbProps.model(p.mesh()).triangles()){
            double[] a={t.a().x(),t.a().y(),t.a().z()},b={t.b().x(),t.b().y(),t.b().z()},d={t.c().x(),t.c().y(),t.c().z()},u=new double[3],v=new double[3],w=new double[3];
            for(int i=0;i<3;i++){u[i]=b[i]-a[i];v[i]=d[i]-a[i];w[i]=at[i]-a[i];}
            double[] n={u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]};double norm=Math.sqrt(dot(n,n));if(norm<1e-10||Math.abs(dot(n,w))/norm>1e-5)continue;
            double uu=dot(u,u),uv=dot(u,v),vv=dot(v,v),wu=dot(w,u),wv=dot(w,v),den=uu*vv-uv*uv;if(Math.abs(den)<1e-16)continue;
            double x=(vv*wu-uv*wv)/den,y=(uu*wv-uv*wu)/den;if(x>=-1e-5&&y>=-1e-5&&x+y<=1+1e-5)return true;
        }return false;
    }
    static double dot(double[] a,double[] b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
    public static void main(String[] args)throws Exception{
        var assembly=HouseRebuildServices.assembly();var map=new HouseMap(HouseMap.Site.HOUSE,assembly.level());var base=HouseRebuildStory.source();
        check(map.level().stairs().equals(base.stairs())&&map.level().voids().equals(base.voids())&&map.level().dock().equals(base.dock())&&map.level().portals().equals(base.portals()),"Heating changed original stairs/openings/van/portals");
        check(assembly.radiators().size()==7&&map.level().props().size()==39,"Missing original heating assets");
        for(var placed:assembly.radiators()){
            var p=placed.prop();var b=SceneRenderer.castleBounds(p.mesh());check(p.scale()==1&&Math.abs(p.y()+b[1]-map.level().floorY(p.room()))<1e-6,"Radiator feet float above original floor "+placed.key());
            check(p.height()<.9,"Radiator obstructs low daylight opening "+placed.key());
            for(var other:base.colliders())if(Math.abs(other.y()-placed.solid().y())<.9){
                var q=placed.solid();double c=Math.cos(q.yaw()),s=Math.sin(q.yaw()),dx=other.x()-q.x(),dz=other.z()-q.z();
                // A conservative world-AABB test catches ordinary furnishing overlaps.
                double rx=Math.abs(c)*q.halfX()+Math.abs(s)*q.halfZ(),rz=Math.abs(s)*q.halfX()+Math.abs(c)*q.halfZ();
                double ox=Math.abs(Math.cos(other.yaw()))*other.halfX()+Math.abs(Math.sin(other.yaw()))*other.halfZ(),oz=Math.abs(Math.sin(other.yaw()))*other.halfX()+Math.abs(Math.cos(other.yaw()))*other.halfZ();
                check(Math.abs(dx)>=rx+ox-.002||Math.abs(dz)>=rz+oz-.002,"Radiator overlaps existing furnishing "+placed.key());
            }
        }
        check(assembly.contacts().size()==31,"Flow/return and domestic water/waste must reach the source fixtures");
        for(var contact:assembly.contacts()){
            check(onSource(contact.parent(),contact.point()),"Terminal floats off actual source triangles "+contact.id());
            var point=contact.point();check(map.level().sockets().stream().filter(s->s.type().equals("service-node")).anyMatch(s->Math.abs(s.x()-point.x())<1e-7&&Math.abs(s.y()-point.y())<1e-7&&Math.abs(s.z()-point.z())<1e-7),"Detached terminal "+contact.id());
        }
        check(map.services().parts().stream().anyMatch(p->p.triangles().size()==28),"Sanitary pipes lack low-count closed round sections");
        check(!map.services().physics().clearSight(new Vec(20.2,3.15,10.13),new Vec(20.5,3.15,10.13)),"Actual soil pipe must occlude through its body");
        check(map.services().physics().clearSight(new Vec(20.397,3.1,10.177),new Vec(20.397,3.2,10.177)),"Round pipe must not retain invisible square-corner collision");
        check(map.clutter().states().size()==8,"Service work replaced authored belongings");map.configureHiding(100);
        for(var socket:HidingBudget.candidates(map.level())){var p=LevelClosets.world(socket,0,0,0);check(map.traversable(p.x(),p.y(),p.z()),"Heating blocked cover "+socket.id());}
        HouseRebuildPowerTest.approach(map,InteractionFocus.Kind.GENERATOR);HouseRebuildPowerTest.approach(map,InteractionFocus.Kind.BREAKER);
        var audit=LevelTraversalAudit.inspect(map);check(audit.passed(),"Heating obstructs required actual routes "+audit.findings());
        HouseRebuildPowerTest.main(new String[]{"SERVICES"});GhostElectricalTest.main(new String[]{"SERVICES"});
        if(args.length>0){var out=Path.of(args[0]);Files.createDirectories(out);var renderer=new SceneRenderer();renderer.setRenderSize(1000,625);renderer.lamp=true;renderer.roomLights=new RoomLighting();
            for(var placed:assembly.radiators()){
                var solid=placed.solid();double x=solid.x(),z=solid.z();var room=map.level().room(placed.prop().room());
                double cx=room.spawn().x(),cz=room.spawn().z(),y=room.y();check(map.traversable(cx,y,cz),"Service review spawn blocked "+placed.key());
                renderer.x=cx;renderer.z=cz;renderer.eyeHeight=y+1.62;renderer.yaw=Math.atan2(x-cx,z-cz);renderer.pitch=Math.atan2(y+.4-renderer.eyeHeight,Math.hypot(x-cx,z-cz));renderer.drawNetwork(map,List.of(),-1,.4);ImageIO.write(renderer.image(),"png",out.resolve(placed.key()+"-runtime.png").toFile());
            }
            double[][] fixtures={{18.3,12.4,-.78,20.5,12.8},{18.3,12.4,-.78,19,10.49},{19.6,12.4,4.42,19.85,10.34}};String[] names={"boiler-pipework","laundry-water","bathroom-water"};
            for(int i=0;i<fixtures.length;i++){var p=fixtures[i];check(map.traversable(p[0],p[2]-1.62,p[1]),"Fixture review camera blocked "+names[i]);renderer.x=p[0];renderer.z=p[1];renderer.eyeHeight=p[2];renderer.yaw=Math.atan2(p[3]-p[0],p[4]-p[1]);renderer.pitch=-.35;renderer.drawNetwork(map,List.of(),-1,.4);ImageIO.write(renderer.image(),"png",out.resolve(names[i]+"-runtime.png").toFile());}
        }
        System.out.println("PASS7full-size original radiators,31source-contact heat/water/waste terminals,shared closed service geometry,8retained loose belongings and whole-House routes="+audit.positions());
    }
}
