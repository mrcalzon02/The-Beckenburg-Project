package org.beckenburg;
import java.util.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.Vec;
/** Actual supported services, sparse shared cover and unchanged structural routes. */
public final class HouseRebuildFitoutTest {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    public static void main(String[] args)throws Exception{
        var map=HouseRebuildFitout.map();var base=HouseRebuildFunctions.source();
        check(map.level().portals().equals(base.portals())&&map.level().stairs().equals(base.stairs())&&map.level().voids().equals(base.voids())&&map.level().dock().equals(base.dock()),"Structural/van regression");
        check(map.services()!=null&&map.services().parts().size()>50,"Missing actual closed service network");
        for(var part:map.services().parts())check(part.triangles().size()==12,"Nonclosed conduit segment "+part.name());
        var extents=new ArrayList<double[]>();
        for(var part:map.services().parts()){
            double[] b={1e9,1e9,1e9,-1e9,-1e9,-1e9};
            for(var t:part.triangles())for(var v:List.of(t.a(),t.b(),t.c())){double[] q={v.p().x(),v.p().y(),v.p().z()};for(int i=0;i<3;i++){b[i]=Math.min(b[i],q[i]);b[i+3]=Math.max(b[i+3],q[i]);}}extents.add(b);
        }
        for(int i=0;i<extents.size();i++)for(int j=i+1;j<extents.size();j++){
            var a=extents.get(i);var b=extents.get(j);int axis=0,other=0;for(int k=1;k<3;k++){if(a[k+3]-a[k]>a[axis+3]-a[axis])axis=k;if(b[k+3]-b[k]>b[other+3]-b[other])other=k;}
            if(axis!=other)continue;boolean same=true;for(int k=0;k<3;k++)if(k!=axis&&Math.abs(a[k]+a[k+3]-b[k]-b[k+3])>1e-6)same=false;
            if(same)check(Math.min(a[axis+3],b[axis+3])-Math.max(a[axis],b[axis])<=.04,"Duplicate overlapping service lengths "+map.services().parts().get(i).name()+" / "+map.services().parts().get(j).name());
        }
        var servicePhysics=map.services().physics();
        check(!servicePhysics.clearSight(new Vec(18.04,1.5,10.092),new Vec(18.14,1.5,10.092)),"Actual riser mesh must occlude across its mass");
        check(servicePhysics.clearSight(new Vec(18.04,1.5,10.15),new Vec(18.14,1.5,10.15)),"Adjacent clear ray cannot inherit a pipe-sized world blocker");
        check(servicePhysics.bodyBlocked(new Vec(18.092,1.45,10.092),.1,.005),"Actual riser mass must block small physical bodies");
        var switchBounds=SceneRenderer.castleBounds("light_switch_1");
        for(var sw:map.level().sockets())if(sw.type().equals("switch")){
            String id=String.format(java.util.Locale.ROOT,"BHR-L%02d-3",sw.room()+1);
            var end=map.level().sockets().stream().filter(q->q.id().equals(id)).findFirst().orElseThrow();
            check(end.y()-.012<=sw.y()+switchBounds[1]+switchBounds[4]+1e-6,"Conduit floats above actual switch plate "+sw.id());
        }
        var beds=map.level().props().stream().filter(p->p.mesh().startsWith("level_bedding_")).toList();check(beds.size()==2,"Missing fitted bed dressings");
        for(var p:beds){var parent=map.level().props().stream().filter(q->q.room()==p.room()&&q.mesh().startsWith("bed_")).findFirst().orElseThrow();check(p.scale()==1&&p.x()==parent.x()&&p.y()==parent.y()&&p.z()==parent.z()&&p.yaw()==parent.yaw(),"Original matching bed/bedding source frames lost");}
        var hiding=map.level().sockets().stream().filter(s->s.type().equals("hide")).toList();check(hiding.size()==HidingBudget.MAXIMUM,"Shared hiding allowance");
        for(int density=0;density<=100;density++){
            map.configureHiding(density);int count=0;for(var s:hiding)if(LevelClosets.available(map.level(),s.room(),density)){
                count++;var inside=LevelClosets.world(s,0,0,0);check(map.traversable(inside.x(),inside.y(),inside.z()),"Cupboard pocket blocked "+s.id());
                var from=LevelClosets.world(s,0,0,1.05);check(map.traversable(from.x(),from.y(),from.z()),"Cupboard entrance blocked "+s.id());
                var into=map.resolveMove(from,inside.x(),inside.z());check(into!=null&&Math.hypot(into.x()-inside.x(),into.z()-inside.z())<.01,"Cupboard route blocked "+s.id());
            }check(count==HidingBudget.MAXIMUM*density/100,"Difficulty budget mismatch "+density);
        }
        map.configureHiding(100);var audit=LevelTraversalAudit.inspect(map);check(audit.passed(),"Fit-out circulation "+audit.findings());
        HouseRebuildPowerTest.approach(map,InteractionFocus.Kind.GENERATOR);HouseRebuildPowerTest.approach(map,InteractionFocus.Kind.BREAKER);
        if(args.length>0){var out=Path.of(args[0]);Files.createDirectories(out);var r=new SceneRenderer();r.setRenderSize(1000,625);r.lamp=true;r.roomLights=new RoomLighting();
            double[][] views={{15.5,18.5,4.42,-Math.PI/2,-.5},{15.5,11.2,4.42,-Math.PI/2,-.5},{16.4,12,-.78,-2.27,.25},{13.5,17.3,-.78,0,-.18},{15.8,17.1,4.42,2.6,-.05},{15.8,12.4,4.42,2.6,-.05}};
            String[] names={"parents-bedding","child-bedding","distribution","cellar-refuge","parents-refuge","child-refuge"};
            for(int i=0;i<views.length;i++){var v=views[i];check(map.traversable(v[0],v[2]-1.62,v[1]),"Capture blocked "+names[i]);r.x=v[0];r.z=v[1];r.eyeHeight=v[2];r.yaw=v[3];r.pitch=v[4];r.drawNetwork(map,List.of(),-1,0);ImageIO.write(r.image(),"png",out.resolve(names[i]+"-runtime.png").toFile());}
        }
        System.out.println("PASS2source-frame bed dressings; "+map.services().parts().size()+" closed source-textured service sections;101difficulty budgets and3real cupboard entries; whole-House routes="+audit.positions()+"; actual generator/breaker approaches");
    }
}
