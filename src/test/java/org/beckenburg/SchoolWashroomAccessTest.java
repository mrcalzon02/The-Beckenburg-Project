package org.beckenburg;

import java.util.*;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Stair;
import org.beckenburg.LevelDefinition.Vec;

/** Diagnostic school washroom access via actual standing-hunter swept capsule,
 * with real room 7, child bays, self-door portals and stairs; open doors only.
 */
public final class SchoolWashroomAccessTest {
    private SchoolWashroomAccessTest() {}
    private static final double STEP=.125, X0=20.5, Z0=12.5;
    private static final int NX=43, NZ=133;
    private static final int[][] DIR={{1,0},{-1,0},{0,1},{0,-1}};
    private record Sample(int ix,int iz,Vec feet,int parent){}
    private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    private static List<Sample> sweep(HouseMap map,Vec start){
        check(map.traversable(start.x(),start.y(),start.z()),"School upper corridor spawn blocked");
        int sx=(int)Math.round((start.x()-X0)/STEP),sz=(int)Math.round((start.z()-Z0)/STEP);
        check(sx>=0&&sx<NX&&sz>=0&&sz<NZ,"School upper spawn outside sampled corridor");
        int[] seen=new int[NX*NZ];Arrays.fill(seen,-1);
        var nodes=new ArrayList<Sample>();var queue=new ArrayDeque<Integer>();
        nodes.add(new Sample(sx,sz,start,-1));seen[sz*NX+sx]=0;queue.add(0);
        while(!queue.isEmpty()){
            int index=queue.removeFirst();Sample from=nodes.get(index);
            for(int[] d:DIR){
                int x=from.ix()+d[0],z=from.iz()+d[1];
                if(x<0||z<0||x>=NX||z>=NZ||seen[z*NX+x]>=0)continue;
                Vec next=map.resolveMove(from.feet(),X0+x*STEP,Z0+z*STEP);
                if(next==null||Math.abs(next.y()-start.y())>.15
                    ||map.roomAt(next.x(),next.y(),next.z())!=7)continue;
                seen[z*NX+x]=nodes.size();nodes.add(new Sample(x,z,next,index));
                queue.add(nodes.size()-1);
            }
        }
        return nodes;
    }
    private static int nearest(List<Sample> nodes,double x,double z,double limit){
        int found=-1;double best=limit;
        for(int i=0;i<nodes.size();i++){
            Vec p=nodes.get(i).feet();double d=Math.hypot(p.x()-x,p.z()-z);
            if(d<best){found=i;best=d;}
        }
        return found;
    }
    private static int returnSteps(HouseMap map,List<Sample> nodes,int goal){
        int steps=0;
        for(int at=goal;nodes.get(at).parent()>=0;at=nodes.get(at).parent()){
            Sample from=nodes.get(at),to=nodes.get(from.parent());
            Vec back=map.resolveMove(from.feet(),to.feet().x(),to.feet().z());
            check(back!=null&&Math.hypot(back.x()-to.feet().x(),back.z()-to.feet().z())<.015
                &&Math.abs(back.y()-to.feet().y())<.15,
                "Upper washroom reverse swept move blocked at "+from.feet());
            steps++;
        }
        return steps;
    }
    public static void main(String[] args){
        HouseMap map=new HouseMap(HouseMap.Site.SCHOOL);
        var upper=map.level().room(7);
        check(upper.name().equals("Upper locker corridor")&&Math.abs(upper.y()-3.6)<.01,
            "School upper washroom game-zone/elevation source changed");
        List<Socket> bays=map.level().sockets().stream()
            .filter(s->s.room()==7&&s.type().equals("service_reservation")).toList();
        check(bays.size()==2,"Expected two measured upper washroom bays, got "+bays.size());
        List<Stair> stairs=map.level().stairs().stream()
            .filter(s->s.a()==1&&s.b()==7&&Math.abs(s.y1()-upper.y())<.01).toList();
        check(stairs.size()==1,"Missing ground/upper school stair room-1-to-7 link");
        for(Portal p:map.level().portals())if(p.door()){
            if(!map.doorOpen(p.id()))map.toggleDoor(p.id());
            map.doorOpenFraction(p.id(),System.nanoTime()+HouseMap.DOOR_SWING_NANOS);
        }
        List<Sample> reached=sweep(map,map.spawn(7));
        Stair top=stairs.get(0);
        int landing=nearest(reached,top.x1(),top.z1(),.7);
        check(landing>=0,"Stair "+top.id()+" upper landing disconnected from room 7 corridor");
        int landingBack=returnSteps(map,reached,landing);
        Set<Integer> unique=new HashSet<>();
        for(Socket bay:bays){
            String[] dimensions=bay.style().split(":");
            check(dimensions.length==2,"Missing child washroom bounds for "+bay.id());
            double halfDepth=Double.parseDouble(dimensions[1])/2;
            List<Portal> entries=map.level().portals().stream()
                .filter(p->p.a()==7&&p.b()==7&&p.door()
                    &&Math.abs(p.y()-upper.y())<.01&&p.x()<bay.x()-1
                    &&Math.abs(p.z()-bay.z())<halfDepth).toList();
            check(entries.size()==1,"Washroom bay "+bay.id()+" corridor door count "+entries.size());
            Portal door=entries.get(0);
            check(unique.add(door.id()),"Both washrooms incorrectly share portal "+door.id());
            double targetX=door.x()+.6,targetZ=door.z()+1.2;
            check(map.traversable(targetX,upper.y(),targetZ),
                "Obstructed inner washroom circulation behind portal "+door.id());
            int goal=nearest(reached,targetX,targetZ,.25);
            check(goal>=0,"No swept legal route from upper corridor to washroom portal "
                +door.id()+" at ("+targetX+","+targetZ+")");
            int back=returnSteps(map,reached,goal);
            System.out.println("PASS SCHOOL room 7 washroom "+bay.id()+" door "+door.id()
                +" @"+door.x()+","+door.z()+" interior "+targetX+","+targetZ
                +" return="+back+" actual 0.125m sweeps");
        }
        System.out.println("PASS SCHOOL upper landing stair "+top.id()+" rooms "
            +top.a()+"->"+top.b()+" return="+landingBack+"; two washroom entrances and return; "
            +reached.size()+" physical swept grid points; diagnostic open-door route only");
    }
}
