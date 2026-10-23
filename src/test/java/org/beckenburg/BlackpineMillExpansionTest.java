package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
/** New furnishings must fit the actual room and remain independent of existing solids. */
public final class BlackpineMillExpansionTest {
    static final Set<String> NEW=Set.of("level_work_dust_extractor","level_work_timber_rack","level_work_safety_station","level_work_circular_headrig","level_work_bandsaw");
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static boolean overlap(Prop a,Prop b){
        for(double yaw:new double[]{a.yaw(),a.yaw()+Math.PI/2,b.yaw(),b.yaw()+Math.PI/2}){
            double x=Math.cos(yaw),z=Math.sin(yaw);
            double ra=Math.abs(Math.cos(a.yaw())*x+Math.sin(a.yaw())*z)*a.halfX()+Math.abs(-Math.sin(a.yaw())*x+Math.cos(a.yaw())*z)*a.halfZ();
            double rb=Math.abs(Math.cos(b.yaw())*x+Math.sin(b.yaw())*z)*b.halfX()+Math.abs(-Math.sin(b.yaw())*x+Math.cos(b.yaw())*z)*b.halfZ();
            if(Math.abs((a.x()-b.x())*x+(a.z()-b.z())*z)>=ra+rb-.015)return false;
        }
        return a.y()<b.y()+b.height()-.015&&b.y()<a.y()+a.height()-.015;
    }
    public static void main(String[] args){
        var map=BlackpineLevel.map();var l=map.level();int count=0;
        for(var p:l.props())if(NEW.contains(p.mesh())){
            count++;var r=l.room(p.room());
            double ex=Math.abs(Math.cos(p.yaw()))*p.halfX()+Math.abs(Math.sin(p.yaw()))*p.halfZ();
            double ez=Math.abs(Math.sin(p.yaw()))*p.halfX()+Math.abs(Math.cos(p.yaw()))*p.halfZ();
            check(p.x()-ex>r.x0()+.08&&p.x()+ex<r.x1()-.08&&p.z()-ez>r.z0()+.08&&p.z()+ez<r.z1()-.08,"inside finished room faces "+p.mesh());
            for(var other:l.props())if(other!=p&&other.room()==p.room()&&other.halfX()>0&&other.halfZ()>0)
                check(!overlap(p,other),"solid furnishing intersection "+p.mesh()+" with "+other.mesh()+" at "+other.x()+","+other.z());
            for(var portal:l.portals())if(portal.door()&&(portal.a()==p.room()||portal.b()==p.room()))
                check(Math.hypot(portal.x()-p.x(),portal.z()-p.z())>Math.max(ex,ez)+portal.width()/2+.35,"door approach clear "+p.mesh());
        }
        check(count==5,"four new meshes and relocated workshop saw");
        var saw=l.props().stream().filter(p->p.mesh().equals("level_work_circular_headrig")).findFirst().orElseThrow();
        var carriage=l.props().stream().filter(p->p.mesh().equals("level_work_log_carriage")).findFirst().orElseThrow();
        check(Math.abs(saw.x()-carriage.x())<.001&&saw.yaw()==carriage.yaw(),"blade and logway share authoritative alignment");
        check(l.props().stream().noneMatch(p->p.mesh().equals("level_work_bandsaw")&&p.room()==0),"workshop saw is not the mill head saw");
        System.out.println("PASS four new models and relocated workshop saw inside finished walls, disjoint from solid furnishings; circular head saw aligned to logway");
    }
}
