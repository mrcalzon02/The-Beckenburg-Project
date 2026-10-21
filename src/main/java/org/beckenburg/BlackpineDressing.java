package org.beckenburg;

import java.util.*;
import org.beckenburg.LevelDefinition.*;

/** Authored camp furnishings and deterministic woodland. All placement lives in the level definition. */
public final class BlackpineDressing {
    private BlackpineDressing(){}
    public static final Set<String> ASSETS=Set.of("level_camp_well","level_camp_patio","level_camp_canopy",
        "level_camp_fireplace","level_camp_chimney","level_camp_logpile","level_camp_sawhorse",
        "level_camp_rainbarrel","level_camp_bench","level_woodland_grass","level_woodland_fern","level_woodland_flowers",
        "level_woodland_scrub","level_woodland_stump","level_woodland_boulder","level_woodland_deadfall",
        "level_woodland_track","level_woodland_litter","level_woodland_floor","level_woodland_bank");
    /** Irregular overlapping stamps follow these real routes. Values are centreline X/Z metres. */
    public static final List<double[]> ROUTES=List.of(
        new double[]{35.5,60,33,55,31,50,29,45,30,38,33,32,38,25,40,25},
        new double[]{31,50,25,49,18,48,18,46},
        new double[]{25,49,13,47,13,40,14,40},
        new double[]{13,47,9,44,9,42},
        new double[]{30,38,26,30,22,26,16,28,12,27,12,26},
        new double[]{26,30,27,23,27,18,27,16},
        new double[]{27,23,34,18,37,14,37,12},
        new double[]{27,18,24,16,21,14,21,12},
        new double[]{33,55,40,56,47,56,47,54},
        new double[]{38,25,38,32,38,41,40,41},
        new double[]{52,34,55,32,58,29,60,27,62,25,62,23},
        new double[]{29,45,27,40,26,38}
    );
    public static LevelDefinition apply(LevelDefinition base){
        var d=new Dressing(base);d.build();
        return new LevelDefinition(base.id(),"blackpine-dressed-v2",base.title(),base.rooms(),base.portals(),
            base.stairs(),base.voids(),d.props,d.sockets,d.colliders,base.dock(),base.ambience());
    }
    private static final class Dressing {
        final LevelDefinition base;
        final List<Prop> props;final List<Socket> sockets;final List<Collider> colliders;
        final Random rng=new Random(0xB1AC071E);
        Dressing(LevelDefinition l){base=l;props=new ArrayList<>(l.props());sockets=new ArrayList<>(l.sockets());colliders=new ArrayList<>(l.colliders());}
        void detail(String mesh,int room,double x,double y,double z,double sx,double sy,double sz,double yaw){
            sockets.add(new Socket("blackpine-detail-"+sockets.size(),"landscape_detail",room,x,y,z,yaw,mesh+"|"+sx+"|"+sy+"|"+sz));
        }
        void prop(String mesh,int room,double x,double z,double yaw,double hx,double hz,double height){
            props.add(new Prop(mesh,room,x,0,z,1,yaw,hx,hz,height,"floor"));
            if(hx>0&&hz>0)colliders.add(new Collider(x,0,z,hx,hz,height,yaw));
        }
        void build(){
            prop("level_camp_well",12,26,36,0,1.0,.86,2.8);
            canopy(27,17.25);canopy(18,47.25);
            hearth(6,24.95,9.5,24.25);hearth(2,14.95,44.1,14.25);
            prop("level_camp_logpile",12,54.6,41.2,0,1.37,.71,.97);
            prop("level_camp_logpile",12,57.7,41.2,0,1.37,.71,.97);
            prop("level_camp_logpile",12,43,55.8,0,1.37,.71,.97);
            prop("level_camp_sawhorse",12,52.8,44.2,.20,.84,.38,.97);
            prop("level_camp_sawhorse",8,48.7,47.5,Math.PI/2,.84,.38,.97);
            barrel(29.35,15.58);barrel(21.35,45.58);barrel(11.35,41.58);
            // Sit on the patio edges, leaving the full door approach and canopy aisle clear.
            prop("level_camp_bench",12,25.5,17.1,Math.PI/2,.875,.31,.91);
            prop("level_camp_bench",12,16.5,47.1,Math.PI/2,.875,.31,.91);
            prop("level_woodland_stump",12,53.8,45.5,0,.45,.45,.53);
            // No new refuge sockets: low verge plants cannot conceal a hunter.
            paths();yardPlants();forest();
        }
        void canopy(double x,double z){
            prop("level_camp_patio",12,x,z,0,0,0,.01);
            prop("level_camp_canopy",12,x,z,0,0,0,3.08);
            for(double dx:new double[]{-1.75,1.75})colliders.add(new Collider(x+dx,0,z+1.1,.07,.07,2.54,0));
            // Conservative lowest roof underside; a standing hunter has clear headroom below it.
            colliders.add(new Collider(x,2.49,z,2.1,1.34,.62,0));
        }
        void hearth(int room,double x,double z,double shaftX){
            prop("level_camp_fireplace",room,x,z,-Math.PI/2,.98,.725,1.99);
            prop("level_camp_chimney",12,shaftX,z,0,.48,.39,4.84);
        }
        void barrel(double x,double z){
            // Only the barrel is solid across its width; the downpipe occupies its own narrow shaft.
            props.add(new Prop("level_camp_rainbarrel",12,x,0,z,1,0,.38,.38,1.18,"floor"));
            colliders.add(new Collider(x,0,z,.38,.38,1.18,0));
            colliders.add(new Collider(x+.2,1.18,z-.2,.04,.04,1.72,0));
        }
        void paths(){
            int n=0;
            for(double[] route:ROUTES)for(int i=0;i<route.length-2;i+=2){
                double ax=route[i],az=route[i+1],bx=route[i+2],bz=route[i+3];
                double length=Math.hypot(bx-ax,bz-az),yaw=-Math.atan2(bx-ax,bz-az);
                int count=(int)Math.ceil(length/.78);
                for(int j=0;j<=count;j++){
                    double t=j/(double)count,x=ax+(bx-ax)*t,z=az+(bz-az)*t;
                    if(patioReserve(x,z,1))continue;
                    detail("level_woodland_track",12,x,.012+(n++%5)*.0008,z,1.45+rng.nextDouble()*.32,1,1.65,yaw);
                }
            }
            // Sparse scuffs/needle islands retain the open working yard instead of carpeting it.
            for(int i=0;i<90;i++){
                double x=5+rng.nextDouble()*61,z=3+rng.nextDouble()*55;
                if(clearPlant(x,z,.5,false)&&distanceToPath(x,z)>1.05)
                    detail("level_woodland_litter",12,x,.007,z,1.0+rng.nextDouble()*2,1,1+rng.nextDouble()*2,rng.nextDouble()*6.28);
            }
        }
        void yardPlants(){
            for(int i=0;i<660;i++){
                double x=4.5+rng.nextDouble()*62,z=2.5+rng.nextDouble()*56;
                if(!clearPlant(x,z,.65,true)||distanceToPath(x,z)<1.25)continue;
                if(x>24&&x<40&&z>22&&z<49&&rng.nextDouble()<.82)continue;
                String mesh=i%17==0?"level_woodland_flowers":i%8==0?"level_woodland_fern":i%19==0?"level_woodland_scrub":"level_woodland_grass";
                double scale=.65+rng.nextDouble()*.5;
                detail(mesh,12,x,0,z,scale,scale,scale,rng.nextDouble()*Math.PI*2);
            }
            // Deliberately tended little flower patches beside the foreman's stoop and mess.
            for(double[] patch:new double[][]{{29.7,18.3},{24.3,18.3},{20.7,48.3},{8,43.3}})
                for(int i=0;i<3;i++)detail("level_woodland_flowers",12,patch[0]+(i-1)*.34,0,patch[1],.8,.8,.8,i);
        }
        boolean clearPlant(double x,double z,double margin,boolean fixtures){
            if(patioReserve(x,z,margin))return false;
            for(var r:base.rooms())if(!r.outside()&&x>r.x0()-margin&&x<r.x1()+margin&&z>r.z0()-margin&&z<r.z1()+margin)return false;
            for(var p:base.portals())if(Math.hypot(x-p.x(),z-p.z())<1.7)return false;
            for(var s:base.sockets())if((s.type().equals("outdoor-hide")||s.type().equals("spawn"))&&Math.hypot(x-s.x(),z-s.z())<1.8)return false;
            if(fixtures)for(var p:props)if(p.room()>=12&&p.halfX()>0&&Math.hypot(x-p.x(),z-p.z())<Math.max(p.halfX(),p.halfZ())+margin)return false;
            return true;
        }
        void forest(){
            sockets.removeIf(s->s.type().equals("landscape_tree")&&s.room()==13);
            for(var e:LevelEnclosure.edges(base.rooms())){
                double tangentX=(e.x1()-e.x0())/e.length(),tangentZ=(e.z1()-e.z0())/e.length();
                for(int band=0;band<4;band++){
                    double depth=new double[]{5.5,13,24,38}[band],spacing=7+band*3.5;
                    for(double t=2.5+band*1.8;t<e.length()-1;t+=spacing){
                        double jitter=rng.nextDouble()*3-1.5;
                        double x=e.x0()+tangentX*(t+jitter)-e.inwardX()*(depth+rng.nextDouble()*3),z=e.z0()+tangentZ*(t+jitter)-e.inwardZ()*(depth+rng.nextDouble()*3);
                        if(roadReserve(x,z))continue;
                        double scale=.72+rng.nextDouble()*.60+band*.10;
                        detail(band<2&&rng.nextInt(5)==0?"level_tree_broadleaf_worn":"level_tree_conifer_worn",13,x,0,z,scale,scale,scale,rng.nextDouble()*6.28);
                        if(band<2){
                            for(int j=0;j<2;j++)detail(j==0?"level_woodland_fern":"level_woodland_scrub",13,x+rng.nextDouble()*3-1.5,0,z+rng.nextDouble()*3-1.5,1,1,1,rng.nextDouble()*6.28);
                            if(rng.nextInt(3)==0)detail("level_woodland_deadfall",13,x,0,z+1.5,1,1,1,rng.nextDouble()*6.28);
                        }
                    }
                }
                for(double t=4;t<e.length()-3;t+=13){
                    double x=e.x0()+tangentX*t-e.inwardX()*18,z=e.z0()+tangentZ*t-e.inwardZ()*18;
                    if(roadReserve(x,z))continue;
                    detail("level_woodland_bank",13,x,0,z,1.3,1,1.3,-Math.atan2(tangentX,tangentZ));
                    detail("level_woodland_boulder",13,x,0,z,1.5,1.5,1.5,t*.3);
                }
            }
        }
        boolean roadReserve(double x,double z){
            var van=new VanStaging(new VanStaging.Placement(base.dock().x(),base.dock().z(),base.dock().yaw()));
            var p=van.local(x,z);
            return Math.abs(p.x())<7&&p.z()<8&&p.z()>-105;
        }
        boolean patioReserve(double x,double z,double margin){
            return Math.abs(x-27)<2+margin&&Math.abs(z-17.25)<1.5+margin
                ||Math.abs(x-18)<2+margin&&Math.abs(z-47.25)<1.5+margin;
        }
    }
    public static double distanceToPath(double x,double z){
        double best=Double.MAX_VALUE;
        for(double[] route:ROUTES)for(int i=0;i<route.length-2;i+=2){
            double ax=route[i],az=route[i+1],dx=route[i+2]-ax,dz=route[i+3]-az;
            double t=Math.max(0,Math.min(1,((x-ax)*dx+(z-az)*dz)/(dx*dx+dz*dz)));
            best=Math.min(best,Math.hypot(x-ax-dx*t,z-az-dz*t));
        }return best;
    }
}
