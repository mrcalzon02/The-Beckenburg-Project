package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.Vec;
/** Optical intersection with a standing capsule, not a room-presence approximation. */
public final class StructuredLightVolume {
    private StructuredLightVolume(){}
    public static final double RADIUS=.22,HEIGHT=HunterPosture.STANDING_HEIGHT;
    public record Beam(Vec a,Vec b){}
    public record Contact(int station,int room,double x,double y,double z,int tick){
        public Contact {if(station<1||room<0||room>=12||!Double.isFinite(x+y+z)||tick<0)throw new IllegalArgumentException("laser contact");}
        public String frame(){return "LASER|HIT|"+station+"|"+room+"|"+x+"|"+y+"|"+z+"|"+tick;}
    }
    public static double radiusAt(double height){
        if(height<0||height>HEIGHT)return 0;
        double cap=height<RADIUS?RADIUS-height:height>HEIGHT-RADIUS?height-(HEIGHT-RADIUS):0;
        return Math.sqrt(Math.max(0,RADIUS*RADIUS-cap*cap));
    }
    public static List<Beam> beams(HouseMap map,VanSensorStations.Station station){
        double floor=map.groundHeight(station.x(),station.z(),station.room()),range=EquipmentTiers.stage("LASER_GRID",station.tier()).reachCm()/100.0;
        int count=station.tier()==EquipmentTiers.Tier.I?3:station.tier()==EquipmentTiers.Tier.II?5:7;
        int layers=station.tier()==EquipmentTiers.Tier.I?1:station.tier()==EquipmentTiers.Tier.II?3:5;
        var out=new ArrayList<Beam>();
        for(int layer=0;layer<layers;layer++)for(int i=0;i<count;i++){
            double angle=(i-(count-1)/2.0)*.12,h=floor+(layers==1?1.0:.3+layer*1.2/(layers-1));
            var a=new Vec(station.x(),h,station.z());double dx=Math.sin(angle)*range,dz=Math.cos(angle)*range;
            double lo=0,hi=1;
            if(map.clearLineOfSight(a.x(),h,a.z(),a.x()+dx,h,a.z()+dz))lo=1;
            else for(int step=0;step<9;step++){double t=(lo+hi)/2;if(map.clearLineOfSight(a.x(),h,a.z(),a.x()+dx*t,h,a.z()+dz*t))lo=t;else hi=t;}
            out.add(new Beam(a,new Vec(a.x()+dx*lo,h,a.z()+dz*lo)));
        }
        return List.copyOf(out);
    }
    /** Exact horizontal beam/capsule cross-section interval. */
    public static double[] intersection(Beam b,Contact c){
        double radius=radiusAt(b.a().y()-c.y());if(radius<=0)return null;
        double dx=b.b().x()-b.a().x(),dz=b.b().z()-b.a().z(),px=b.a().x()-c.x(),pz=b.a().z()-c.z();
        double a=dx*dx+dz*dz;if(a<1e-10)return null;
        double linear=2*(dx*px+dz*pz),disc=linear*linear-4*a*(px*px+pz*pz-radius*radius);
        if(disc<=0)return null;double root=Math.sqrt(disc),lo=Math.max(0,(-linear-root)/(2*a)),hi=Math.min(1,(-linear+root)/(2*a));
        return hi>lo?new double[]{lo,hi}:null;
    }
    public static List<Beam> distort(Beam b,Contact c,double phase){
        double[] hit=c==null?null:intersection(b,c);if(hit==null)return List.of(b);
        var out=new ArrayList<Beam>();Vec at=point(b,0);double dx=b.b().x()-b.a().x(),dz=b.b().z()-b.a().z(),length=Math.hypot(dx,dz);
        out.add(new Beam(at,point(b,hit[0])));at=point(b,hit[0]);
        for(int i=1;i<=18;i++){
            double f=i/18.0,t=hit[0]+(hit[1]-hit[0])*f,amplitude=.075*Math.sin(Math.PI*f),angle=phase+f*Math.PI*6;
            var center=point(b,t);var next=new Vec(center.x()-dz/length*amplitude*Math.cos(angle),center.y()+amplitude*Math.sin(angle),center.z()+dx/length*amplitude*Math.cos(angle));
            out.add(new Beam(at,next));at=next;
        }
        out.add(new Beam(at,b.b()));return List.copyOf(out);
    }
    private static Vec point(Beam b,double t){return new Vec(b.a().x()+(b.b().x()-b.a().x())*t,b.a().y(),b.a().z()+(b.b().z()-b.a().z())*t);}
}
