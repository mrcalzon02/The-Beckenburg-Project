package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;

/** Check the first real camp shell, dimensions and physical approaches, not just room counts. */
public final class BlackpineGrayboxTest {
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    public static void main(String[] args){
        var map=BlackpineLevel.map();map.configureHiding(100);var l=map.level();
        for(var p:l.portals())if(p.door())map.toggleDoor(p.id());
        check(l.title().equals("Blackpine Logging Camp"),"distinct real level definition");
        check(l.room(6).x1()-l.room(6).x0()==6&&l.room(6).z1()-l.room(6).z0()==8,"cabin remains normal 6 by 8 metres");
        check(l.room(0).x1()-l.room(0).x0()==12&&l.room(0).z1()-l.room(0).z0()==18,"modest working mill scale");
        check(l.sockets().stream().filter(s->s.type().equals("outdoor-hide")).count()==3,"only three authored outdoor refuges across the camp");
        check(l.sockets().stream().noneMatch(s->s.type().equals("hide")),"no automatic cupboard in every building");
        check(l.sockets().stream().filter(s->s.type().equals("ritual")).map(Socket::room).distinct().count()==3,"three distinct defended rite rooms");
        check(map.valid(),"physical spawns and graph are valid");
        for(var portal:l.portals())if(portal.door()){
            var a=AuthoredTestSupport.side(map,portal,portal.a(),.5);var b=AuthoredTestSupport.side(map,portal,portal.b(),.5);
            check(map.resolveMove(a,b.x(),b.z())!=null,"real open threshold traversable: "+portal.id());
            map.toggleDoor(portal.id());check(map.resolveMove(a,b.x(),b.z())==null,"closed threshold blocks: "+portal.id());map.toggleDoor(portal.id());
        }
        var stair=l.stairs().get(0);Vec at=new Vec(stair.x0(),stair.y0(),stair.z0());
        for(int i=1;i<=120;i++){double t=i/120.;at=map.resolveMove(at,stair.x0()+(stair.x1()-stair.x0())*t,stair.z0()+(stair.z1()-stair.z0())*t);check(at!=null,"mill stair continuously supports ascent "+i);}
        check(Math.abs(at.y()-l.room(11).y())<.02,"actual upper deck elevation reached");
        for(int i=119;i>=0;i--){double t=i/120.;at=map.resolveMove(at,stair.x0()+(stair.x1()-stair.x0())*t,stair.z0()+(stair.z1()-stair.z0())*t);check(at!=null,"mill stair continuously supports descent "+i);}
        System.out.println("PASS: Blackpine dimensions, sparse refuge, physical doors, spawns, three rites and supported mill stairs");
    }
}
