package org.beckenburg;

import java.util.*;
import org.beckenburg.LevelDefinition.*;

/** Bounded open-door audit through the actual runtime swept-capsule solver.
 * Uses a separate map instance so inspection never changes live door state.
 * This establishes reachable goals, not closed-door, hunt or mission acceptance.
 */
public final class LevelTraversalAudit {
    private LevelTraversalAudit(){}
    public record Finding(String code,int room,String detail){}
    public record Result(String level,String revision,int positions,double lowest,double highest,
                         List<Finding> findings){public boolean passed(){return findings.isEmpty();}}
    public static Result inspect(HouseMap source){
        var map=new HouseMap(source.site(),source.level(),source.worldSurface());map.configureHiding(source.hidingDensity());
        for(Portal p:map.level().portals())if(p.door()&&!map.doorOpen(p.id()))map.toggleDoor(p.id());
        var queue=new ArrayDeque<Vec>();var seen=new HashSet<String>();var reached=new ArrayList<Vec>();
        Vec origin=map.spawn(14);queue.add(origin);seen.add(key(origin));
        var findings=new ArrayList<Finding>();
        while(!queue.isEmpty()){
            Vec at=queue.remove();reached.add(at);
            for(double[] d:new double[][]{{.125,0},{-.125,0},{0,.125},{0,-.125}}){
                Vec next=map.resolveMove(at,at.x()+d[0],at.z()+d[1]);
                if(next!=null&&seen.add(key(next)))queue.add(next);
            }
            if(seen.size()>1_000_000){findings.add(new Finding("TRAVERSAL_BUDGET",-1,"Open-route audit exceeded one million positions"));break;}
        }
        for(Room room:map.level().rooms()){
            Vec goal=room.spawn();
            if(reached.stream().noneMatch(p->Math.abs(p.y()-goal.y())<.2&&Math.hypot(p.x()-goal.x(),p.z()-goal.z())<.55))
                findings.add(new Finding("UNREACHABLE_ROOM",room.id(),room.name()));
        }
        for(Socket socket:map.level().sockets())if(Set.of("switch","breaker","ritual","evidence","hide","outdoor-hide","story").contains(socket.type())){
            double floor=map.groundHeight(socket.x(),socket.z(),socket.room());
            double reach=socket.type().equals("outdoor-hide")?.3:socket.type().equals("hide")?.8:1.45;
            if(reached.stream().noneMatch(p->Math.abs(p.y()-floor)<.2&&map.roomAt(p.x(),p.y(),p.z())==socket.room()
                &&Math.hypot(p.x()-socket.x(),p.z()-socket.z())<reach))
                findings.add(new Finding("UNREACHABLE_SOCKET",socket.room(),socket.type()+" "+socket.id()));
        }
        return new Result(map.level().id(),map.level().revision(),seen.size(),
            reached.stream().mapToDouble(Vec::y).min().orElse(Double.NaN),
            reached.stream().mapToDouble(Vec::y).max().orElse(Double.NaN),List.copyOf(findings));
    }
    private static String key(Vec p){return Math.round(p.x()*8)+":"+Math.round(p.y()*1000)+":"+Math.round(p.z()*8);}
}
