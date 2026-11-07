package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
/** Original household heating assets with source-contact flow/return and under-deck distribution. */
public final class HouseRebuildServices {
    private HouseRebuildServices(){}
    public record Contact(String id,Prop parent,Vec point,String circuit){}
    public record Assembly(LevelDefinition level,List<HouseRebuildFunctions.Placement> radiators,List<Contact> contacts){
        public Assembly{radiators=List.copyOf(radiators);contacts=List.copyOf(contacts);}
    }
    public static HouseMap map(){return new HouseMap(HouseMap.Site.HOUSE,source());}
    public static LevelDefinition source(){return assembly().level();}
    public static Assembly assembly(){
        var fit=new HouseRebuildFunctions.Fit(HouseRebuildStory.source());var radiators=new ArrayList<HouseRebuildFunctions.Placement>();var contacts=new ArrayList<Contact>();
        radiators.add(fit.floor("sitting-radiator","level_radiator_worn",1,12.2075,17.6,Math.PI/2));
        radiators.add(fit.floor("kitchen-radiator","level_radiator_worn",2,14.9,10.2075,0));
        radiators.add(fit.floor("dining-radiator","level_radiator_worn",3,21.3925,11.8,-Math.PI/2));
        radiators.add(fit.floor("landing-radiator","level_radiator_worn",5,19.8,20.2925,Math.PI));
        radiators.add(fit.floor("parents-radiator","level_radiator_worn",6,15,20.2925,Math.PI));
        radiators.add(fit.floor("child-radiator","level_radiator_worn",7,15,10.2075,0));
        radiators.add(fit.floor("bath-radiator","level_radiator_worn",8,18.2075,12.2,Math.PI/2));
        var boiler=fit.props.stream().filter(p->p.mesh().equals("level_boiler_modern_worn")).findFirst().orElseThrow();
        for(int circuit=0;circuit<2;circuit++){
            double spine=18.015+circuit*.04;String name=circuit==0?"FLOW":"RETURN";
            // Rear service connections leave the original front grille unobstructed.
            var port=face(boiler,2,circuit==0?-.2:.2,.8,false);contacts.add(new Contact("boiler-"+name,boiler,port,name));
            double outX=port.x()+.04;
            HouseRebuildFitout.run(fit.sockets,"BHR-HEAT-"+name,10,"black","heating-main",
                port,new Vec(outX,port.y(),port.z()),new Vec(outX,-2.42,port.z()),new Vec(outX,-2.42,10.092),new Vec(spine,-2.42,10.092),new Vec(spine,2.78,10.092));
            for(var placed:radiators){var p=placed.prop();double floor=fit.base.floorY(p.room());
                var end=circuit==0?face(p,0,.72,.02,true):face(p,0,.15,-.03,false);
                contacts.add(new Contact(placed.key()+"-"+name,p,end,name));
                double direction=circuit==0?1:-1,c=Math.cos(p.yaw()),s=Math.sin(p.yaw());
                var outside=new Vec(end.x()+direction*.06*c,end.y(),end.z()+direction*.06*s);
                HouseRebuildFitout.run(fit.sockets,"BHR-HEAT-"+placed.key()+"-"+name,p.room(),"black","underfloor-and-terminal",
                    new Vec(spine,floor-.02,10.092),new Vec(spine,floor-.02,outside.z()),new Vec(outside.x(),floor-.02,outside.z()),outside,end);
            }
        }
        water(fit,contacts,boiler);
        var base=fit.finish().level();
        var level=new LevelDefinition(base.id(),"first-principles-heating-v1",base.title().replace("household story construction","heating construction"),base.rooms(),base.portals(),base.stairs(),base.voids(),base.props(),base.sockets(),base.colliders(),base.dock(),base.ambience(),base.water(),base.sounds());
        return new Assembly(level,radiators,contacts);
    }
    private static void water(HouseRebuildFunctions.Fit fit,List<Contact> contacts,Prop boiler){
        run(fit.sockets,"BHR-COLD-MAIN",10,"ivory","wall-water",24,new Vec(21.508,-1.9,10.5),new Vec(21.508,-1.9,10.092),new Vec(19.35,-1.9,10.092),new Vec(19.35,3.6,10.092));
        var hot=face(boiler,2,.35,1.0,false);contacts.add(new Contact("boiler-DOMESTIC",boiler,hot,"HOT"));
        run(fit.sockets,"BHR-HOT-MAIN",10,"ivory","underfloor-and-wall-water",24,hot,new Vec(hot.x()+.04,hot.y(),hot.z()),new Vec(hot.x()+.04,-2.42,hot.z()),new Vec(hot.x()+.04,-2.42,10.092),new Vec(19.4,-2.42,10.092),new Vec(19.4,3.6,10.092));
        Prop kitchen=prop(fit,"level_kitchen_counter"),basin=prop(fit,"level_bath_basin"),tub=prop(fit,"level_bathtub"),washer=prop(fit,"level_laundry_washer"),toilet=prop(fit,"toilet_mp_1");
        for(int line=0;line<2;line++){
            String name=line==0?"COLD":"HOT";double spine=line==0?19.35:19.4;
            waterBranch(fit,contacts,kitchen,"kitchen-"+name,face(kitchen,2,-.6+(line==0?-.04:.04),.5,false),spine,name);
            waterBranch(fit,contacts,basin,"basin-"+name,face(basin,2,line==0?-.08:.08,.75,false),spine,name);
            waterBranch(fit,contacts,tub,"bath-"+name,face(tub,2,line==0?.57:.65,.42,false),spine,name);
        }
        waterBranch(fit,contacts,washer,"washer-COLD",face(washer,2,0,.45,false),19.35,"COLD");
        waterBranch(fit,contacts,toilet,"cistern-COLD",face(toilet,2,0,.5,false),19.35,"COLD");
        waterBranch(fit,contacts,boiler,"boiler-COLD",face(boiler,2,-.35,1.0,false),19.35,"COLD");
        // Shared soil connection below the cellar slab, with a supported service head below the upper sill.
        run(fit.sockets,"BHR-SOIL",10,"black","wall-soil",100,new Vec(21.7,-2.6,10.13),new Vec(20.35,-2.6,10.13),new Vec(20.35,3.25,10.13));
        run(fit.sockets,"BHR-SOIL-HEAD",8,"ivory","service-head",100,new Vec(20.35,3.25,10.13),new Vec(20.35,3.30,10.13));
        drain(fit,contacts,kitchen,"kitchen",face(kitchen,1,0,-.6,false),40);
        drain(fit,contacts,basin,"basin",face(basin,1,0,0,false),40);
        drain(fit,contacts,tub,"bath",face(tub,1,0,0,false),40);
        drain(fit,contacts,toilet,"toilet",face(toilet,1,0,0,false),100);
        drain(fit,contacts,washer,"washer",face(washer,2,.12,.3,false),40);
    }
    private static Prop prop(HouseRebuildFunctions.Fit f,String mesh){return f.props.stream().filter(p->p.mesh().equals(mesh)).findFirst().orElseThrow();}
    private static void waterBranch(HouseRebuildFunctions.Fit fit,List<Contact> contacts,Prop parent,String id,Vec port,double spine,String circuit){
        contacts.add(new Contact(id,parent,port,circuit));double y=fit.base.floorY(parent.room())-.02;
        run(fit.sockets,"BHR-WATER-"+id,parent.room(),"ivory","underfloor-and-fixture",24,new Vec(spine,y,10.092),new Vec(port.x(),y,10.092),new Vec(port.x(),y,port.z()),port);
    }
    private static void drain(HouseRebuildFunctions.Fit fit,List<Contact> contacts,Prop parent,String id,Vec port,int diameter){
        contacts.add(new Contact(id+"-WASTE",parent,port,"WASTE"));double y=fit.base.floorY(parent.room())-.04-diameter/2000.0;
        run(fit.sockets,"BHR-WASTE-"+id,parent.room(),"black","underfloor-waste",diameter,port,new Vec(port.x(),y,port.z()),new Vec(port.x(),y,10.13),new Vec(20.35,y,10.13));
    }
    private static void run(List<Socket> out,String id,int room,String material,String mounting,int diameter,Vec... points){
        for(int i=0;i<points.length;i++){var p=points[i];out.add(new Socket(id+"-"+i,"service-node",room,p.x(),p.y(),p.z(),0,id+"|"+i+"|"+material+"|"+mounting+"|conduit-"+diameter+"mm-v1"));}
    }
    /** Intersect actual local source triangles at the selected terminal plane. */
    static Vec face(Prop p,int axis,double first,double second,boolean maximum){
        int aa=(axis+1)%3,bb=(axis+2)%3;double best=maximum?Double.NEGATIVE_INFINITY:Double.POSITIVE_INFINITY;
        for(var t:GlbProps.model(p.mesh()).triangles()){
            var a=t.a();var b=t.b();var d=t.c();double[][] q={{a.x(),a.y(),a.z()},{b.x(),b.y(),b.z()},{d.x(),d.y(),d.z()}};
            double den=(q[1][bb]-q[2][bb])*(q[0][aa]-q[2][aa])+(q[2][aa]-q[1][aa])*(q[0][bb]-q[2][bb]);if(Math.abs(den)<1e-12)continue;
            double u=((q[1][bb]-q[2][bb])*(first-q[2][aa])+(q[2][aa]-q[1][aa])*(second-q[2][bb]))/den,v=((q[2][bb]-q[0][bb])*(first-q[2][aa])+(q[0][aa]-q[2][aa])*(second-q[2][bb]))/den;
            if(u>=-1e-8&&v>=-1e-8&&u+v<=1+1e-8){double at=q[0][axis]*u+q[1][axis]*v+q[2][axis]*(1-u-v);best=maximum?Math.max(best,at):Math.min(best,at);}
        }
        if(!Double.isFinite(best))throw new IllegalArgumentException("No actual source terminal "+p.mesh()+" axis="+axis+" at="+first+","+second);
        double[] at=new double[3];at[axis]=best;at[aa]=first;at[bb]=second;double c=Math.cos(p.yaw()),s=Math.sin(p.yaw());
        return new Vec(p.x()+(at[0]*c-at[2]*s)*p.scale(),p.y()+at[1]*p.scale(),p.z()+(at[0]*s+at[2]*c)*p.scale());
    }
}
