package org.beckenburg;

import java.util.*;
import org.beckenburg.LevelDefinition.*;

/** Authored ordinary household activities; no generic room-name prop scatter. */
public final class HouseRebuildFunctions {
    private HouseRebuildFunctions(){}
    public record Placement(String key,Prop prop,Collider solid,String support){}
    public record Assembly(LevelDefinition level,List<Placement> placements){public Assembly{placements=List.copyOf(placements);}}
    public static HouseMap map(){return new HouseMap(HouseMap.Site.HOUSE,source());}
    public static LevelDefinition source(){return assembly().level();}
    public static Assembly assembly(){
        var b=new Fit(HouseRebuildEnvelope.source());
        b.standing(3,18.7,11.8);b.standing(6,15.8,17.1);
        b.floor("family-sofa","sofa_1",1,14,15.36,0);
        b.floor("reading-chair","armchair_1",1,12.65,19.15,-Math.PI/2);
        var tv=b.floor("television-stand","tv_table_1_4",1,17.57,18,Math.PI/2);
        b.on("family-television","tv_mp_1",tv,17.57,18,Math.PI/2);
        b.floor("sitting-end-table","bedside_table_1",1,14.4,19.7,0);
        b.floor("family-dining-table","dinner_table_1",3,20,11.8,0);
        b.floor("dining-chair-nw","chair_mp_5",3,19.7,10.89,0);
        b.floor("dining-chair-ne","chair_mp_5",3,20.5,10.89,0);
        b.floor("dining-chair-sw","chair_mp_5",3,19.7,12.71,Math.PI);
        b.floor("dining-chair-se","chair_mp_5",3,20.5,12.71,Math.PI);
        b.floor("parents-double-bed","bed_4",6,13.55,18.5,0);
        b.floor("parents-bedside","bedside_table_1",6,12.65,17.0,0);
        b.floor("parents-wardrobe","wardrobe_mp_1",6,16.9,15.2,0);
        b.floor("child-single-bed","bed_5",7,13.55,11.2,0);
        b.floor("child-bedside","bedside_table_1",7,12.65,12.5,0);
        b.floor("child-wardrobe","wardrobe_mp_1",7,16.9,10.6,0);
        b.floor("homework-desk","level_student_desk",7,13,14.2,Math.PI);
        b.floor("homework-chair","chair_mp_5",7,13,13.1,0);
        b.floor("landing-linen","shelf_mp_6",5,19.15,18.05,Math.PI/2);
        b.floor("cellar-repair-bench","level_workbench_worn",11,16.8,20.0,Math.PI);
        b.floor("seasonal-shelf-a","shelf_mp_6",11,20.6,15.05,0);
        b.floor("seasonal-shelf-b","shelf_mp_6",11,20.6,16.2,Math.PI/2);
        b.floor("heating-plant","level_boiler_modern_worn",10,20.94,12.8,Math.PI/2);
        var generator=b.floor("site-generator","level_trace_generator",13,25.6,29.3,0);
        b.generatorControl(generator);
        return b.finish();
    }
    static final class Fit {
        final LevelDefinition base;final ArrayList<Room> rooms;final ArrayList<Socket> sockets;
        final ArrayList<Prop> props;final ArrayList<Collider> solids;final ArrayList<Placement> placements=new ArrayList<>();
        Fit(LevelDefinition base){this.base=base;rooms=new ArrayList<>(base.rooms());sockets=new ArrayList<>(base.sockets());props=new ArrayList<>(base.props());solids=new ArrayList<>(base.colliders());}
        void standing(int room,double x,double z){var r=rooms.get(room);rooms.set(room,new Room(r.id(),r.name(),r.x0(),r.z0(),r.x1(),r.z1(),r.y(),r.height(),r.finish(),r.acoustic(),new Vec(x,r.y(),z)));
            for(int i=0;i<sockets.size();i++){var s=sockets.get(i);if(s.room()==room&&(s.type().equals("spawn")||s.type().equals("evidence")))sockets.set(i,new Socket(s.id(),s.type(),room,x,s.y(),z,s.yaw(),s.style()));}}
        Placement floor(String key,String mesh,int room,double cx,double cz,double yaw){
            double y=rooms.get(room).y();if(room>=12){var s=WorldTerrainSurface.of(base).support(cx,cz,0,1);if(s==null)throw new IllegalArgumentException("Unsupported outdoor fixture "+key);y=s.y();}
            return place(key,mesh,room,cx,y,cz,yaw,"floor");
        }
        Placement place(String key,String mesh,int room,double cx,double bottom,double cz,double yaw,String support){
            var b=SceneRenderer.castleBounds(mesh);double c=Math.cos(yaw),s=Math.sin(yaw);
            var p=new Prop(mesh,room,cx-(b[0]*c-b[2]*s),bottom-b[1],cz-(b[0]*s+b[2]*c),1,yaw,b[3]/2,b[5]/2,b[4],support);
            var solid=new Collider(cx,bottom,cz,b[3]/2,b[5]/2,b[4],yaw);var result=new Placement(key,p,solid,support);
            props.add(p);solids.add(solid);placements.add(result);return result;
        }
        Placement on(String key,String mesh,Placement parent,double cx,double cz,double yaw){
            double top=upward(parent.prop(),cx,cz);var b=SceneRenderer.castleBounds(mesh);
            double c=Math.cos(yaw),s=Math.sin(yaw);
            for(double x:new double[]{-b[3]/2,b[3]/2})for(double z:new double[]{-b[5]/2,b[5]/2})
                if(Math.abs(upward(parent.prop(),cx+x*c-z*s,cz+x*s+z*c)-top)>.003)throw new IllegalArgumentException("Uneven/incomplete native support "+key);
            return place(key,mesh,parent.prop().room(),cx,top,cz,yaw,parent.key());
        }
        void generatorControl(Placement parent){
            var p=parent.prop();double x=parent.solid().x()+.25,y=parent.solid().y()+.7,z=Double.NEGATIVE_INFINITY;
            for(var t:GlbProps.model(p.mesh()).triangles()){
                var a=t.a();var b=t.b();var c=t.c();double den=(b.y()-c.y())*(a.x()-c.x())+(c.x()-b.x())*(a.y()-c.y());if(Math.abs(den)<1e-12)continue;
                double lx=x-p.x(),ly=y-p.y(),u=((b.y()-c.y())*(lx-c.x())+(c.x()-b.x())*(ly-c.y()))/den,v=((c.y()-a.y())*(lx-c.x())+(a.x()-c.x())*(ly-c.y()))/den;
                if(u>=0&&v>=0&&u+v<=1)z=Math.max(z,p.z()+a.z()*u+b.z()*v+c.z()*(1-u-v));
            }
            if(!Double.isFinite(z))throw new IllegalArgumentException("Generator control has no actual supporting face");
            var button=SceneRenderer.castleBounds("button_12");
            sockets.add(new Socket("briar-generator-starter","generator",p.room(),x-button[0]*.30,y-(button[1]+button[4]/2)*.30,z-(button[2]-button[5]/2)*.30,0,"BHR-MAIN-01"));
            sockets.add(new Socket("briar-source-plan","electrical-plan",p.room(),p.x(),p.y(),p.z(),0,"BHR-MAIN-01;existing-main-breaker-room10;room-switches0-11;source-independent-of-breaker"));
        }
        Assembly finish(){return new Assembly(new LevelDefinition(base.id(),"first-principles-house-functions-v1",base.title().replace("dwelling construction","functional construction"),rooms,base.portals(),base.stairs(),base.voids(),props,sockets,solids,base.dock(),base.ambience(),base.water(),base.sounds()),placements);}
    }
    static double upward(Prop p,double x,double z){
        double c=Math.cos(p.yaw()),s=Math.sin(p.yaw()),lx=(x-p.x())*c+(z-p.z())*s,lz=-(x-p.x())*s+(z-p.z())*c,best=Double.NEGATIVE_INFINITY;
        for(var t:GlbProps.model(p.mesh()).triangles()){
            var a=t.a();var b=t.b();var d=t.c();double den=(b.z()-d.z())*(a.x()-d.x())+(d.x()-b.x())*(a.z()-d.z());if(Math.abs(den)<1e-12)continue;
            double u=((b.z()-d.z())*(lx-d.x())+(d.x()-b.x())*(lz-d.z()))/den,v=((d.z()-a.z())*(lx-d.x())+(a.x()-d.x())*(lz-d.z()))/den;
            if(u>=-1e-7&&v>=-1e-7&&u+v<=1+1e-7)best=Math.max(best,p.y()+a.y()*u+b.y()*v+d.y()*(1-u-v));
        }
        if(!Double.isFinite(best))throw new IllegalArgumentException("No native support at "+x+","+z);return best;
    }
}
