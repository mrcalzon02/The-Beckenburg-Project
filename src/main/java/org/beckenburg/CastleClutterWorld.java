package org.beckenburg;

import java.util.*;
import org.beckenburg.LevelDefinition.Vec;

/** Native Castle adapter for the existing bounded loose-body simulation. */
final class CastleClutterWorld {
    private final CastleHubMap map;
    CastleClutterWorld(CastleHubMap map){this.map=Objects.requireNonNull(map);}
    boolean valid(LooseClutter.Observer o){return o.seat()==0&&Double.isFinite(o.x()+o.y()+o.z()+o.yaw()+o.pitch())&&map.traversable(o.x(),o.z(),o.y());}
    record Socket(String parent,String mesh,LooseClutter.Kind kind,double x,double z,String purpose){}
    static final List<Socket> SOCKETS=List.of(
        new Socket("ground37","dinner_table_1",LooseClutter.Kind.MUG,-.4,-.18,"Common-room tea mug"),
        new Socket("ground37","dinner_table_1",LooseClutter.Kind.TIN,.4,.18,"Common-room tea tin"),
        new Socket("upper3","dinner_table_1",LooseClutter.Kind.BOOK,-.45,.15,"Upper reading-room ledger"),
        new Socket("upper-bedside-0","bedside_table_1",LooseClutter.Kind.MUG,0,0,"Resident's bedside mug"),
        new Socket("finish-vault-table","dinner_table_1",LooseClutter.Kind.BOOK,-.4,-.15,"Collection-store accession ledger"),
        new Socket("finish-vault-table","dinner_table_1",LooseClutter.Kind.CANDLESTICK,.4,.15,"Unlit conservation-store candlestick")
    );
    record Seed(int id,LooseClutter.Kind kind,double x,double bottom,double z,double yaw){}
    List<Seed> seeds(){
        var result=new ArrayList<Seed>();int id=0;
        for(var s:SOCKETS){id++;var p=CastleLevelEdits.current().props().stream().filter(a->a.id().equals(s.parent)&&a.mesh().equals(s.mesh)).findFirst().orElse(null);if(p==null)continue;
            double x=p.x()+p.scale()*(s.x*Math.cos(p.yaw())-s.z*Math.sin(p.yaw())),z=p.z()+p.scale()*(s.x*Math.sin(p.yaw())+s.z*Math.cos(p.yaw()));
            double top=CastleObjectRay.support(p,x,z,p.y()+SceneRenderer.castleBounds(p.mesh())[4]*p.scale()+.02,p.y()+.25);
            if(top<-99)continue;
            boolean supported=true;for(double[] d:new double[][]{{s.kind.radius,0},{-s.kind.radius,0},{0,s.kind.radius},{0,-s.kind.radius}})
                if(Math.abs(CastleObjectRay.support(p,x+d[0],z+d[1],top+.001,top-.001)-top)>.003)supported=false;
            if(supported)result.add(new Seed(id,s.kind,x,top+.004,z,p.yaw()));
        }return List.copyOf(result);
    }
    double support(double x,double z,double before,double after){
        var scene=CastleLevelEdits.current();double best=-100;
        var terrain=map.worldSurface().support(x,z,(before+after)/2,Math.min(4,Math.abs(before-after)/2+.02));
        if(terrain!=null&&terrain.y()<=before+.01&&terrain.y()>=after-.02)best=terrain.y();
        if(CastleRooms.ground(x,z)&&!scene.floorOpening(x,z,0)&&0<=before+.01&&0>=after-.02)best=Math.max(best,0);
        for(var s:scene.surfaces())if(s.kind()==CastleLevelEdits.Kind.FLOOR&&x>=s.x0()&&x<=s.x1()&&z>=s.z0()&&z<=s.z1()&&!scene.floorOpening(x,z,s.base())&&s.base()<=before+.01&&s.base()>=after-.02)best=Math.max(best,s.base());
        double stair=CastleCellar.onFlight(x,z)?CastleCellar.stairHeight(z):CastleHubArchitecturalPlan.onStairs(x,z)?CastleHubArchitecturalPlan.stairHeight(x,z):Double.NaN;
        if(Double.isFinite(stair)&&stair<=before+.01&&stair>=after-.02)best=Math.max(best,stair);
        for(var p:scene.props())best=Math.max(best,CastleObjectRay.support(p,x,z,before,after));
        return best;
    }
    boolean clearSight(double ax,double ay,double az,double bx,double by,double bz){
        if(!FixtureLighting.castleRayClear(map,ax,ay,az,bx,by,bz))return false;
        double length=Math.sqrt((bx-ax)*(bx-ax)+(by-ay)*(by-ay)+(bz-az)*(bz-az));if(length<1e-8)return true;
        double dx=(bx-ax)/length,dy=(by-ay)/length,dz=(bz-az)/length;
        for(var p:CastleLevelEdits.current().props())if(Double.isFinite(CastleObjectRay.hit(p,ax,ay,az,dx,dy,dz,length-1e-7,1e-7)))return false;
        for(var g:CastleRooms.guards())if(CastleDoorSlots.crossesBox(ax,ay,az,bx,by,bz,g.x0()-.055,g.top(),g.z0()-.055,g.x1()+.055,g.top()+1.05,g.z1()+.055))return false;
        var van=CastleSurroundings.courtyardVan();var a=van.local(ax,az);var b=van.local(bx,bz);
        return !CastleDoorSlots.crossesBox(a.x(),ay,a.z(),b.x(),by,b.z(),-VanStaging.HALF_WIDTH,van.placement().y(),VanStaging.FRONT,VanStaging.HALF_WIDTH,van.placement().y()+VanStaging.ROOF_Y,VanStaging.REAR+VanStaging.CAB_LENGTH);
    }
    boolean clearPath(double ax,double ay,double az,double bx,double by,double bz,double radius,double half){
        for(double[] d:new double[][]{{0,0,0},{radius,0,0},{-radius,0,0},{0,half,0},{0,-half,0},{0,0,radius},{0,0,-radius}})
            if(!clearSight(ax+d[0],ay+d[1],az+d[2],bx+d[0],by+d[1],bz+d[2]))return false;
        return true;
    }
}
