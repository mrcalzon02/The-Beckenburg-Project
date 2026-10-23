package org.beckenburg;

import java.util.*;
import java.awt.image.BufferedImage;
import org.beckenburg.LevelDefinition.*;

/** A baked half-metre height field: the same triangles support feet, plants, texture and native export. */
public final class SiteTerrain {
    public static final double STEP=.5,ROAD_Z=72,BACK_Z=1,RISE=HunterPosture.STANDING_HEIGHT;
    public static final String VERSION="blackpine-graded-terrain-v1";
    private static final Map<String,SiteTerrain> CACHE=new LinkedHashMap<>();
    public static synchronized SiteTerrain of(LevelDefinition level){
        if(!level.id().equals("blackpine"))return null;
        SiteTerrain found=CACHE.get(level.revision());if(found!=null)return found;
        var terrain=new SiteTerrain(level);if(CACHE.size()>=4)CACHE.remove(CACHE.keySet().iterator().next());CACHE.put(level.revision(),terrain);return terrain;
    }
    public final double x0=3,z0=1,x1=68,z1=79.5;
    public final int nx=(int)((x1-x0)/STEP)+1,nz=(int)((z1-z0)/STEP)+1;
    private final double[] heights=new double[nx*nz];
    private final LevelDefinition level;private BufferedImage diffuse;private int material=-1;
    private record Pad(double x0,double z0,double x1,double z1,double y){}
    private final List<Pad> pads=new ArrayList<>();
    private SiteTerrain(LevelDefinition level){
        this.level=level;
        for(var r:level.rooms())if(!r.outside()&&r.id()!=11)pads.add(new Pad(r.x0(),r.z0(),r.x1(),r.z1(),r.y()));
        pads.add(new Pad(25,15.75,29,18.75,level.room(6).y()));pads.add(new Pad(16,45.75,20,48.75,level.room(2).y()));
        for(int iz=0;iz<nz;iz++)for(int ix=0;ix<nx;ix++)heights[iz*nx+ix]=sculpt(x0+ix*STEP,z0+iz*STEP);
    }
    public static double grade(double z){return RISE*Math.max(0,Math.min(1,(ROAD_Z-z)/(ROAD_Z-BACK_Z)));}
    private static double smooth(double a,double b,double value){double t=Math.max(0,Math.min(1,(value-a)/(b-a)));return t*t*(3-2*t);}
    private double sculpt(double x,double z){
        double road=1-smooth(64,69,z),distance=BlackpineDressing.distanceToPath(x,z);
        double shoulder=smooth(.55,2.2,distance),edge=smooth(1,6,z);
        double micro=(.16+.055*Math.sin(x*.19+z*.09)+.035*Math.sin(z*.27-x*.12))*shoulder*edge;
        double h=(grade(z)+micro)*road,weight=0,padY=0;
        for(var p:pads){
            double dx=Math.max(0,Math.max(p.x0-x,x-p.x1)),dz=Math.max(0,Math.max(p.z0-z,z-p.z1));
            double w=1-smooth(.60,2.6,Math.hypot(dx,dz));
            if(w>weight){weight=w;padY=p.y;}
        }
        return h*(1-weight)+padY*weight;
    }
    public boolean contains(double x,double z){return x>=x0&&x<=x1&&z>=z0&&z<=z1;}
    public double vertexHeight(int ix,int iz){return heights[iz*nx+ix];}
    /** Match the rendered diagonal exactly, rather than walking on a different bilinear surface. */
    public double height(double x,double z){
        if(!contains(x,z))return grade(z)*(1-smooth(64,69,z));
        double gx=Math.max(0,Math.min(nx-1,(x-x0)/STEP)),gz=Math.max(0,Math.min(nz-1,(z-z0)/STEP));
        int ix=Math.min(nx-2,(int)gx),iz=Math.min(nz-2,(int)gz);double tx=gx-ix,tz=gz-iz;
        double a=vertexHeight(ix,iz),b=vertexHeight(ix+1,iz),c=vertexHeight(ix+1,iz+1),d=vertexHeight(ix,iz+1);
        return tz<=tx?a+(b-a)*tx+(c-b)*tz:a+(c-d)*tx+(d-a)*tz;
    }
    public Vec normal(double x,double z){double dx=(height(x+.05,z)-height(x-.05,z))/.1,dz=(height(x,z+.05)-height(x,z-.05))/.1,n=Math.sqrt(1+dx*dx+dz*dz);return new Vec(-dx/n,1/n,-dz/n);}
    public boolean playable(double x,double z){return level.rooms().stream().anyMatch(r->r.id()>=12&&r.id()!=14&&x>=r.x0()&&x<=r.x1()&&z>=r.z0()&&z<=r.z1());}
    public boolean indoors(double x,double z){return level.rooms().stream().anyMatch(r->!r.outside()&&r.id()!=11&&x>r.x0()&&x<r.x1()&&z>r.z0()&&z<r.z1());}
    public double u(double x){return (x-x0)/(x1-x0);}public double v(double z){return 1-(z-z0)/(z1-z0);}
    public synchronized BufferedImage diffuse(){
        if(diffuse!=null)return diffuse;
        int grass=GlbProps.model("level_woodland_floor").triangles().get(0).material(),soil=GlbProps.model("level_woodland_track").triangles().get(0).material();
        diffuse=new BufferedImage(1024,1024,BufferedImage.TYPE_INT_RGB);
        for(int pz=0;pz<1024;pz++)for(int px=0;px<1024;px++){
            double x=x0+(px+.5)/1024*(x1-x0),z=z0+(pz+.5)/1024*(z1-z0),d=BlackpineDressing.distanceToPath(x,z);
            double rough=.045*Math.sin(x*4.8+z*1.9)+.025*Math.sin(z*8.1-x*2.7),wear=1-smooth(.55,.99+rough,d);
            if(patio(x,z))wear=0;
            double scuff=.5+.5*Math.sin(x*.73+Math.sin(z*.35)*2)*Math.sin(z*.61+x*.15);wear=Math.max(wear,.18*smooth(.68,.93,scuff));
            int a=GlbProps.sample(grass,x*.32,z*.32),b=GlbProps.sample(soil,x*.48,z*.48),rgb=0;
            for(int shift:new int[]{16,8,0})rgb|=(int)(((a>>shift)&255)*(1-wear)+((b>>shift)&255)*wear)<<shift;
            diffuse.setRGB(px,pz,rgb);
        }return diffuse;
    }
    public synchronized int material(){if(material<0)material=GlbProps.surfaceMaterial(VERSION+":"+level.revision(),diffuse());return material;}
    public static boolean patio(double x,double z){return Math.abs(x-27)<=2&&Math.abs(z-17.25)<=1.5||Math.abs(x-18)<=2&&Math.abs(z-47.25)<=1.5;}
    private static double foundation(int room){return grade(new double[]{32,32,38,38,20,39,12,8,50,8,23,32}[room]);}
    /** Lift the complete authored level together, preserving shared floors, portals and the canonical road/van. */
    public static LevelDefinition gradeLevel(LevelDefinition base){
        var rooms=new ArrayList<Room>();
        for(var r:base.rooms()){
            double dy=r.outside()?0:foundation(r.id());var p=r.spawn();
            rooms.add(new Room(r.id(),r.name(),r.x0(),r.z0(),r.x1(),r.z1(),r.y()+dy,r.height(),r.finish(),r.acoustic(),new Vec(p.x(),p.y()+dy,p.z())));
        }
        var interim=new LevelDefinition(base.id(),VERSION,base.title(),rooms,base.portals(),base.stairs(),base.voids(),base.props(),base.sockets(),base.colliders(),base.dock(),base.ambience());
        var terrain=new SiteTerrain(interim);
        for(int i=12;i<14;i++){var r=rooms.get(i);var p=r.spawn();rooms.set(i,new Room(r.id(),r.name(),r.x0(),r.z0(),r.x1(),r.z1(),r.y(),r.height(),r.finish(),r.acoustic(),new Vec(p.x(),terrain.height(p.x(),p.z()),p.z())));}
        var portals=base.portals().stream().map(p->new Portal(p.id(),p.a(),p.b(),p.x(),p.y()+offset(base,terrain,p.x(),p.z(),p.a()<12?p.a():p.b()),p.z(),p.width(),p.alongX(),p.kind(),p.loss())).toList();
        var stairs=base.stairs().stream().map(s->new Stair(s.id(),s.a(),s.b(),s.x0(),s.z0(),s.x1(),s.z1(),s.width(),s.y0()+foundation(s.a()),s.y1()+foundation(s.b()),s.steps())).toList();
        var voids=base.voids().stream().map(v->new LevelDefinition.Void(v.y()+foundation(11),v.x0(),v.z0(),v.x1(),v.z1())).toList();
        var props=base.props().stream().map(p->new Prop(p.mesh(),p.room(),p.x(),p.y()+offset(base,terrain,p.x(),p.z(),p.room()),p.z(),p.scale(),p.yaw(),p.halfX(),p.halfZ(),p.height(),p.support())).toList();
        var sockets=new ArrayList<Socket>();
        for(var s:base.sockets())sockets.add(new Socket(s.id(),s.type(),s.room(),s.x(),s.y()+offset(base,terrain,s.x(),s.z(),s.room()),s.z(),s.yaw(),s.style()));
        sockets.add(new Socket("blackpine-terrain-profile","terrain_profile",12,35.5,0,ROAD_Z,0,VERSION+"|rise="+RISE+"|cell="+STEP+"|routes="+baseRoutes()));
        var colliders=new ArrayList<Collider>();
        for(var c:base.colliders()){
            if(base.sockets().stream().anyMatch(s->s.room()>=12&&s.type().equals("enclosure")&&Math.hypot(s.x()-c.x(),s.z()-c.z())<.001))continue;
            colliders.add(new Collider(c.x(),c.y()+offset(base,terrain,c.x(),c.z(),12),c.z(),c.halfX(),c.halfZ(),c.height(),c.yaw()));
        }
        for(var s:base.sockets())if(s.room()>=12&&s.type().equals("enclosure")){
            double length=Double.parseDouble(s.style().split(":")[1]),angle=-s.yaw();int count=(int)Math.ceil(length/2);double width=length/count;
            for(int i=0;i<count;i++){double t=-length/2+width*(i+.5),x=s.x()+t*Math.cos(angle),z=s.z()+t*Math.sin(angle);colliders.add(new Collider(x,terrain.height(x,z)-.05,z,width/2,.15,1.5,angle));}
        }
        return new LevelDefinition(base.id(),VERSION,base.title(),rooms,portals,stairs,voids,props,sockets,colliders,base.dock(),base.ambience());
    }
    private static String baseRoutes(){return BlackpineDressing.ROUTES.stream().map(Arrays::toString).collect(java.util.stream.Collectors.joining(";"));}
    private static double offset(LevelDefinition base,SiteTerrain terrain,double x,double z,int room){
        if(room==14)return 0;if(room>=0&&room<12)return foundation(room);
        for(var r:base.rooms())if(!r.outside()&&r.id()!=11&&x>=r.x0()&&x<=r.x1()&&z>=r.z0()&&z<=r.z1())return foundation(r.id());
        return terrain.contains(x,z)?terrain.height(x,z):grade(z)*(1-smooth(64,69,z));
    }
}
