package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
/** Household finish layer: original bedding, three shared-budget cupboards and physical services. */
public final class HouseRebuildFitout {
    private HouseRebuildFitout(){}
    public static HouseMap map(){return new HouseMap(HouseMap.Site.HOUSE,source());}
    public static LevelDefinition source(){
        var base=HouseRebuildFunctions.source();var props=new ArrayList<>(base.props());var sockets=new ArrayList<>(base.sockets());var solids=new ArrayList<>(base.colliders());
        for(var p:base.props())if(p.mesh().equals("bed_4")||p.mesh().equals("bed_5")){
            String mesh=p.mesh().equals("bed_4")?"level_bedding_double":"level_bedding_single";var b=SceneRenderer.castleBounds(mesh);
            props.add(new Prop(mesh,p.room(),p.x(),p.y(),p.z(),1,p.yaw(),b[3]/2,b[5]/2,b[4],"source-bed-origin:"+p.room()));
        }
        for(var p:base.props())if(p.mesh().equals("wardrobe_mp_1")&&(p.room()==6||p.room()==7)){
            props.remove(p);var b=SceneRenderer.castleBounds(p.mesh());double c=Math.cos(p.yaw()),s=Math.sin(p.yaw()),x=p.x()+b[0]*c-b[2]*s,z=p.z()+b[0]*s+b[2]*c;
            solids.removeIf(q->Math.abs(q.x()-x)<1e-6&&Math.abs(q.z()-z)<1e-6&&Math.abs(q.y()-(p.y()+b[1]))<1e-6&&Math.abs(q.halfX()-b[3]/2)<1e-6&&Math.abs(q.halfZ()-b[5]/2)<1e-6);
        }
        sockets.add(new Socket("parents-refuge","hide",6,16.9,2.8,15.5,0,"walk-in-cupboard"));
        sockets.add(new Socket("child-refuge","hide",7,16.9,2.8,10.8,0,"walk-in-cupboard"));
        sockets.add(new Socket("cellar-refuge","hide",9,13.5,-2.4,19.7,Math.PI,"walk-in-cupboard"));
        var generator=base.props().stream().filter(p->p.mesh().equals("level_trace_generator")).findFirst().orElseThrow();
        double gx=generator.x()+.55,gy=generator.y()+.55,gz=front(generator,gx,gy);
        run(sockets,"BHR-SOURCE",13,"black","source-and-buried",new Vec(gx,gy,gz),new Vec(gx,gy,gz+.04),new Vec(gx,-1.1,gz+.04),new Vec(25.8,-1.1,gz+.04),new Vec(25.8,-1.1,10.9),new Vec(21.508,-1.1,10.9),new Vec(21.508,-.14,10.9),new Vec(21.508,-.14,10.092),new Vec(15.092,-.14,10.092),new Vec(15.092,-.14,10.76),new Vec(15.092,-.66,10.76));
        run(sockets,"BHR-RISER",10,"ivory","wall",new Vec(15.092,-.66,11.04),new Vec(15.092,-.052,11.04),new Vec(15.092,-.052,10.092),new Vec(18.092,-.052,10.092),new Vec(18.092,5.548,10.092));
        for(var room:base.rooms())if(!room.outside()){
            var sw=base.socket("switch",room.id());var light=base.socket("light",room.id());double top=room.y()+room.height()-.052;
            String id=String.format(java.util.Locale.ROOT,"BHR-L%02d",room.id()+1);
            run(sockets,id,room.id(),"ivory","ceiling-and-wall",new Vec(18.092,top,10.092),new Vec(18.092,top,sw.z()),new Vec(sw.x(),top,sw.z()),new Vec(sw.x(),sw.y()+SceneRenderer.castleBounds("light_switch_1")[1]+SceneRenderer.castleBounds("light_switch_1")[4],sw.z()));
            run(sockets,id+"-LOAD",room.id(),"ivory","ceiling",new Vec(sw.x(),top,sw.z()),new Vec(light.x(),top,sw.z()),new Vec(light.x(),top,light.z()),new Vec(light.x(),light.y(),light.z()));
        }
        return new LevelDefinition(base.id(),"first-principles-fitout-v1",base.title().replace("functional construction","service fit-out"),base.rooms(),base.portals(),base.stairs(),base.voids(),props,sockets,solids,base.dock(),base.ambience(),base.water(),base.sounds());
    }
    static void run(List<Socket> out,String id,int room,String material,String mount,Vec... points){
        for(int i=0;i<points.length;i++){var p=points[i];out.add(new Socket(id+"-"+i,"service-node",room,p.x(),p.y(),p.z(),0,id+"|"+i+"|"+material+"|"+mount+"|conduit-24mm-v1"));}
    }
    static double front(Prop p,double x,double y){
        double best=Double.NEGATIVE_INFINITY;
        for(var t:GlbProps.model(p.mesh()).triangles()){var a=t.a();var b=t.b();var c=t.c();double den=(b.y()-c.y())*(a.x()-c.x())+(c.x()-b.x())*(a.y()-c.y());if(Math.abs(den)<1e-12)continue;
            double u=((b.y()-c.y())*(x-p.x()-c.x())+(c.x()-b.x())*(y-p.y()-c.y()))/den,v=((c.y()-a.y())*(x-p.x()-c.x())+(a.x()-c.x())*(y-p.y()-c.y()))/den;
            if(u>=0&&v>=0&&u+v<=1)best=Math.max(best,p.z()+a.z()*u+b.z()*v+c.z()*(1-u-v));}
        if(!Double.isFinite(best))throw new IllegalArgumentException("No real generator output support");return best;
    }
}
