package org.beckenburg;

import java.util.*;
import org.beckenburg.LevelDefinition.Vec;

/** One physical definition for protected space and its recognizable blue haze.
 * Hunters pass freely. Only host-side spectral movement/combat consumes it. */
public final class SafeAreaBarrier {
    private SafeAreaBarrier(){}
    public static final double WARD_RADIUS=1.45;
    public record Volume(double x,double y,double z,double radius,double shoulder,double height){
        public Volume{
            if(!Double.isFinite(x+y+z+radius+shoulder+height)||radius<=0||shoulder<=0||height<=shoulder)
                throw new IllegalArgumentException("invalid safe-area dome");
        }
        public boolean contains(double px,double py,double pz){
            double dy=py-y;if(dy<-.20||dy>height)return false;
            double q=((px-x)*(px-x)+(pz-z)*(pz-z))/(radius*radius);
            if(dy>shoulder)q+=(dy-shoulder)*(dy-shoulder)/((height-shoulder)*(height-shoulder));
            return q<=1+1e-10;
        }
        /** Exact swept intersection: even a long step cannot tunnel through a dome. */
        public boolean crosses(Vec a,Vec b){
            return intersects(a,b,-.20,shoulder,false)||intersects(a,b,shoulder,height,true);
        }
        private boolean intersects(Vec a,Vec b,double low,double high,boolean cap){
            double dy=b.y()-a.y(),lo=0,hi=1;
            if(Math.abs(dy)<1e-12){if(a.y()<y+low||a.y()>y+high)return false;}
            else{double t0=(y+low-a.y())/dy,t1=(y+high-a.y())/dy;lo=Math.max(0,Math.min(t0,t1));hi=Math.min(1,Math.max(t0,t1));if(lo>hi)return false;}
            double dx=(b.x()-a.x())/radius,dz=(b.z()-a.z())/radius;
            double ox=(a.x()-x)/radius,oz=(a.z()-z)/radius;
            double yy=cap?(a.y()-y-shoulder)/(height-shoulder):0,vy=cap?dy/(height-shoulder):0;
            double aa=dx*dx+dz*dz+vy*vy,bb=ox*dx+oz*dz+yy*vy;
            double t=aa<1e-14?lo:Math.max(lo,Math.min(hi,-bb/aa));
            double xx=ox+t*dx,zz=oz+t*dz,cy=yy+t*vy;
            return xx*xx+zz*zz+cy*cy<=1+1e-10;
        }
    }
    /** Centre of the complete vehicle, with room for its raised deck/loading ramp. */
    public static Volume van(VanStaging van){
        var p=van.world(0,(VanStaging.FRONT+VanStaging.REAR+VanStaging.CAB_LENGTH)/2);
        return new Volume(p.x(),van.placement().y(),p.z(),5.0,1.80,4.50);
    }
    public static Volume ward(double x,double feet,double z){return new Volume(x,feet,z,WARD_RADIUS,1.65,2.50);}
    /** Headquarters is one sanctuary, including its west training annex,
     * cellar, courtyard and gate. The parked van has no independent shield. */
    public static Volume castle(){
        double x0=Double.POSITIVE_INFINITY,z0=x0,x1=-x0,z1=-x0;
        for(var room:CastleRooms.ALL){x0=Math.min(x0,room.x0());x1=Math.max(x1,room.x1());z0=Math.min(z0,room.z0());z1=Math.max(z1,room.z1());}
        for(var room:CastleAnnex.SPACES){x0=Math.min(x0,room.x0());x1=Math.max(x1,room.x1());z0=Math.min(z0,room.z0());z1=Math.max(z1,room.z1());}
        return new Volume((x0+x1)/2,-5,(z0+z1)/2,Math.hypot((x1-x0)/2,(z1-z0)/2)+4,6.80,29);
    }
    public static List<Volume> areas(HouseMap map){
        var out=new ArrayList<Volume>();out.add(van(map.van()));out.addAll(map.temporarySafeAreas());return List.copyOf(out);
    }
    public static boolean protectedAt(HouseMap map,double x,double y,double z){
        if(overlapsBody(van(map.van()),x,y,z))return true;
        if(LevelClosets.protectedAt(map.level(),x,y,z,map.hidingDensity())
            ||OutdoorHiding.containing(map.level(),x,y,z,map.hidingDensity())!=null)return true;
        for(var area:map.temporarySafeAreas())if(overlapsBody(area,x,y,z))return true;
        return false;
    }
    private static boolean overlapsBody(Volume a,double x,double y,double z){
        return a.contains(x,y,z)||a.contains(x,y+.8,z)||a.contains(x,y+1.6,z);
    }
    public static boolean blocks(HouseMap map,Vec from,Vec to){
        if(crossesBody(van(map.van()),from,to))return true;
        if(LevelClosets.blocks(map.level(),from,to,map.hidingDensity())
            ||OutdoorHiding.blocks(map.level(),from,to,map.hidingDensity()))return true;
        for(var area:map.temporarySafeAreas())if(crossesBody(area,from,to))return true;
        return false;
    }
    private static boolean crossesBody(Volume area,Vec from,Vec to){
        return area.crosses(from,to)||area.crosses(new Vec(from.x(),from.y()+.8,from.z()),new Vec(to.x(),to.y()+.8,to.z()))
            ||area.crosses(new Vec(from.x(),from.y()+1.6,from.z()),new Vec(to.x(),to.y()+1.6,to.z()));
    }
    /** A room transfer must choose supported footing outside the protective area. */
    public static Vec ghostSpawn(HouseMap map,int room){
        var original=map.spawn(room);
        if(spectralFooting(map,room,original))return original;
        var scene=map.nativeInvestigation();
        Vec closest=null;
        double distance=Double.POSITIVE_INFINITY;
        if(scene==null){
            var bounds=map.level().room(room);
            return nearestFooting(map,room,original,bounds.x0(),bounds.z0(),bounds.x1(),bounds.z1(),bounds.y(),null,distance,false);
        }
        for(var space:scene.spaces()){
            if(!space.accessible()||space.owner()!=room)continue;
            var candidate=nearestFooting(map,room,original,space.x0(),space.z0(),space.x1(),space.z1(),
                                         space.floor(),closest,distance,true);
            if(candidate!=closest){
                closest=candidate;
                distance=distance(original,candidate);
            }
        }
        return closest;
    }

    private static Vec nearestFooting(HouseMap map,int room,Vec origin,double x0,double z0,double x1,double z1,
                                      double datum,Vec best,double distance,boolean nativeSpace){
        for(double x=x0+.27;x<=x1-.27;x+=.45)for(double z=z0+.27;z<=z1-.27;z+=.45){
            double horizontal=(x-origin.x())*(x-origin.x())+(z-origin.z())*(z-origin.z());
            if(horizontal>=distance)continue;
            double feet;
            if(nativeSpace){
                var floor=map.supportNear(x,z,datum);
                if(floor==null)continue;
                feet=floor;
            }else feet=map.groundHeight(x,z,room);
            if(!Double.isFinite(feet))continue;
            var pose=new Vec(x,feet,z);
            double candidate=distance(origin,pose);
            if(candidate<distance&&spectralFooting(map,room,pose)){best=pose;distance=candidate;}
        }
        return best;
    }

    private static boolean spectralFooting(HouseMap map,int room,Vec at){
        return !protectedAt(map,at.x(),at.y(),at.z())
            &&map.roomAt(at.x(),at.y(),at.z())==room&&map.traversable(at.x(),at.y(),at.z());
    }

    private static double distance(Vec a,Vec b){
        double x=a.x()-b.x(),y=a.y()-b.y(),z=a.z()-b.z();
        return x*x+y*y+z*z;
    }
    /** Only settled, live wards are safe areas; flight never advertises protection. */
    public static List<Volume> visibleWards(HouseMap map,List<SceneRenderer.WorldMark> marks){
        var out=new ArrayList<Volume>();
        for(var mark:marks){var area=settledWard(map,mark);if(area!=null)out.add(area);}return List.copyOf(out);
    }
    static Volume settledWard(HouseMap map,SceneRenderer.WorldMark mark){
        if(mark.kind()!=HunterCounters.Kind.WARD)return null;
        int room=map.roomAt(mark.x(),mark.height(),mark.z());if(room<0)room=map.roomAt(mark.x(),mark.z());
        if(room<0)return null;double feet=map.groundHeight(mark.x(),mark.z(),room);
        return Math.abs(mark.height()-feet-.045)<.035?ward(mark.x(),feet,mark.z()):null;
    }
    static List<String> wardFrames(HouseMap map,int tick){
        var out=new ArrayList<String>();out.add("SAFEBEGIN|"+tick);
        for(var a:map.temporarySafeAreas())out.add("SAFEAREA|"+Math.round(a.x()*1000)+"|"+Math.round(a.y()*1000)+"|"+Math.round(a.z()*1000));
        out.add("SAFEEND|"+tick);return List.copyOf(out);
    }
    /** Atomic bounded public-effect snapshot; it carries no investigator state. */
    static final class Receiver {
        private int pending=-1,version=-1;private final List<Volume> next=new ArrayList<>();
        void accept(HouseMap map,List<String> p){
            if(p.size()==2&&p.get(0).equals("SAFEBEGIN")){
                int v=LanProtocol.integer(p.get(1),0,99999999);pending=v<version?-1:v;next.clear();
            }else if(p.size()==4&&p.get(0).equals("SAFEAREA")){
                if(pending<0)return;if(next.size()>=32)throw new IllegalArgumentException("too many safe areas");
                next.add(ward(LanProtocol.integer(p.get(1),-1000000,1000000)/1000.0,
                    LanProtocol.integer(p.get(2),-50000,50000)/1000.0,LanProtocol.integer(p.get(3),-1000000,1000000)/1000.0));
            }else if(p.size()==2&&p.get(0).equals("SAFEEND")){
                int v=LanProtocol.integer(p.get(1),0,99999999);
                if(v==pending){map.setTemporarySafeAreas(next);version=v;pending=-1;next.clear();}
            }else throw new IllegalArgumentException("invalid safe-area frame");
        }
    }
}
