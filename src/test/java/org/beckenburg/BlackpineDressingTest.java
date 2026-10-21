package org.beckenburg;

import java.util.*;
import org.beckenburg.LevelDefinition.*;

/** Physical checks for the dressing pass: source meshes, real obstructions, open routes and sparse cover. */
public final class BlackpineDressingTest {
    static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    public static void main(String[] args){
        HouseMap map=BlackpineLevel.map();map.configureHiding(100);
        var l=map.level();for(var p:l.portals())if(p.door())map.toggleDoor(p.id());
        check(l.revision().equals(BlackpineLevel.build().revision()),"repeatable landscape and handshake");
        check(l.sockets().stream().map(Socket::id).distinct().count()==l.sockets().size(),"all authored scenery and functional socket identities are unique");
        int triangles=0;
        for(String mesh:BlackpineDressing.ASSETS){
            var model=GlbProps.model(mesh);check(!model.triangles().isEmpty(),"real mesh packaged: "+mesh);
            triangles+=model.triangles().size();
        }
        check(l.sockets().stream().filter(s->s.type().equals("outdoor-hide")).count()==3,"decoration does not manufacture new hiding pockets");
        check(l.portals().stream().filter(s->s.kind().equals("window")).count()==6,"authored windows cut the real shell");
        for(double[] patio:new double[][]{{27,17.25},{18,47.25}}){
            double base=map.groundHeight(patio[0],patio[1],12);
            check(map.traversable(patio[0],base,patio[1]),"standing body fits beneath supported canopy");
            check(!map.traversable(patio[0]-1.75,base,patio[1]+1.1),"visible canopy post blocks body");
            Vec at=new Vec(patio[0],base,patio[1]+2);
            for(int i=0;i<50;i++){
                Vec next=map.resolveMove(at,at.x(),at.z()-.05);
                check(next!=null,"clear swept centre aisle to patio door");at=next;
            }
            check(Math.abs(at.y()-base)<.02,"flush patio keeps feet on authoritative foundation");
        }
        check(!map.traversable(26,map.groundHeight(26,36,12),36),"sealed well cannot be walked through");
        check(!map.traversable(54.6,map.groundHeight(54.6,41.2,12),41.2),"solid timber stack cannot be walked through");
        check(!map.traversable(24.95,map.floorY(6),9.5),"hearth has physical collision");
        double wy=map.groundHeight(26,36,12)+1.2;
        check(!map.clearLineOfSight(25.4,wy,36,26.6,wy,36),"well body blocks an actual sight ray");
        long trees=l.sockets().stream().filter(s->s.type().equals("landscape_detail")&&s.style().startsWith("level_tree_")).count();
        check(trees>90,"several forest bands replace the thin tree line");
        double nearest=Double.MAX_VALUE,farthest=0;
        for(var s:l.sockets())if(s.type().equals("landscape_detail")&&s.style().startsWith("level_tree_")){
            check(map.roomAt(s.x(),0,s.z())<0,"backdrop trees remain outside playable circulation");
            double edge=Double.MAX_VALUE;
            for(var e:LevelEnclosure.edges(l.rooms())){
                double dx=e.x1()-e.x0(),dz=e.z1()-e.z0();
                double t=Math.max(0,Math.min(1,((s.x()-e.x0())*dx+(s.z()-e.z0())*dz)/(dx*dx+dz*dz)));
                edge=Math.min(edge,Math.hypot(s.x()-e.x0()-t*dx,s.z()-e.z0()-t*dz));
            }
            nearest=Math.min(nearest,edge);farthest=Math.max(farthest,edge);
        }
        check(nearest<10&&farthest>35,"forest has measured near and distant depth");
        System.out.println("PASS: "+BlackpineDressing.ASSETS.size()+" original meshes ("+triangles+" library triangles), "+trees+" backdrop trees, supported patios, real solid bodies and only three refuges");
    }
}
