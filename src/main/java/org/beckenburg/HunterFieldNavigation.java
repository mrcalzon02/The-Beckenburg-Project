package org.beckenburg;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.PriorityQueue;
import org.beckenburg.CampaignGuestBot.Action;
import org.beckenburg.CampaignGuestBot.Input;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Vec;

/** Routes over authored thresholds, then around furniture using the same swept capsule as STEP. */
final class HunterFieldNavigation {
    private final HouseMap map;
    private final Set<Integer> blockedPortals=new HashSet<>();
    private final int[][] distances=new int[HouseMap.ROOM_COUNT][HouseMap.ROOM_COUNT];
    private final double[][] routeLength=new double[HouseMap.ROOM_COUNT][HouseMap.ROOM_COUNT];
    private final ArrayDeque<Vec> waypoints=new ArrayDeque<>();
    private int lastDoor=-100,failedUntil;
    private Vec target,previous;
    private int stationary;
    private Search search;
    HunterFieldNavigation(HouseMap map){
        this.map=map;
        for(var portal:map.level().portals()){
            if(portal.kind().equals("window")||!hasExteriorCrossing(portal))blockedPortals.add(portal.id());
        }
        for(int from=0;from<distances.length;from++){
            int[] d=distances[from];Arrays.fill(d,-1);d[from]=0;
            var pending=new ArrayDeque<Integer>();pending.add(from);
            while(!pending.isEmpty()){
                int room=pending.remove();
                for(var p:map.level().portalsFrom(room)){
                    if(blockedPortals.contains(p.id()))continue;
                    int n=p.a()==room?p.b():p.a();
                    if(d[n]>=0)continue;
                    d[n]=d[room]+1;pending.add(n);
                }
            }
        }
        // Estimate walking distance through each doorway. Two large exterior zones
        // can take longer to cross than several adjacent rooms inside the house.
        for(int a=0;a<routeLength.length;a++){
            Arrays.fill(routeLength[a],Double.POSITIVE_INFINITY);
            routeLength[a][a]=0;
            for(var p:map.level().portalsFrom(a)){
                if(blockedPortals.contains(p.id()))continue;
                int b=p.a()==a?p.b():p.a();
                double length=distanceToCenter(a,p)+distanceToCenter(b,p);
                routeLength[a][b]=Math.min(routeLength[a][b],length);
            }
        }
        for(int via=0;via<routeLength.length;via++)
            for(int a=0;a<routeLength.length;a++)
                for(int b=0;b<routeLength.length;b++)
                    routeLength[a][b]=Math.min(routeLength[a][b],routeLength[a][via]+routeLength[via][b]);
    }
    private boolean hasExteriorCrossing(Portal p){
        if(!p.kind().equals("open")||!map.outside(p.a())||!map.outside(p.b())
                ||p.a()==HouseMap.EXTRACTION_ROOM||p.b()==HouseMap.EXTRACTION_ROOM)return true;
        // A room boundary can meet a building along one side of an exterior opening.
        // Check across the aperture with the same swept body used by ordinary movement.
        double halfLane=Math.max(0,p.width()*.5-.25);
        int samples=Math.max(1,(int)Math.ceil(halfLane/.3));
        double nx=p.alongX()?0:.45,nz=p.alongX()?.45:0;
        for(int i=-samples;i<=samples;i++){
            double offset=halfLane*i/samples;
            double x=p.x()+(p.alongX()?offset:0),z=p.z()+(p.alongX()?0:offset);
            var from=new Vec(x-nx,p.y(),z-nz);
            if(map.traversable(from.x(),from.y(),from.z())&&map.resolveMove(from,x+nx,z+nz)!=null)return true;
        }
        return false;
    }
    private double distanceToCenter(int room,Portal p){
        return Math.hypot(Math.hypot(map.centerX(room)-p.x(),map.centerZ(room)-p.z()),map.floorY(room)-p.y());
    }
    int[] distances(int from){return distances[from];}
    void reset(){target=null;previous=null;search=null;waypoints.clear();stationary=0;failedUntil=0;}
    Input to(int tick,LanWorld.Body self,int goalRoom,double gx,double gz,double tolerance){
        return to(tick,self,goalRoom,gx,map.floorY(goalRoom),gz,tolerance);
    }
    Input to(int tick,LanWorld.Body self,int goalRoom,double gx,double gy,double gz,double tolerance){
        int room=self.room();
        if(room<0||goalRoom<0||distances[room][goalRoom]<0)return Input.simple(Action.WAIT);
        Vec pos=new Vec(self.x(),self.y(),self.z());
        if(room==goalRoom&&Math.abs(self.y()-gy)<.45&&Math.hypot(self.x()-gx,self.z()-gz)<=tolerance)return Input.simple(Action.WAIT);
        Vec goal=new Vec(gx,gy,gz);
        boolean approachingStair=false;
        double reach=room==goalRoom?tolerance:.62;
        // The van's physical ramp extends well beyond the room boundary. Leave it
        // along its centre line before turning across the apron towards the house.
        var van=map.van();var local=van.local(pos.x(),pos.z());
        if(goalRoom!=HouseMap.EXTRACTION_ROOM&&Math.abs(local.x())<VanStaging.INTERIOR_HALF_WIDTH
                &&local.z()>VanStaging.FRONT-VanStaging.RAMP_LENGTH-.3&&local.z()<VanStaging.REAR){
            var end=van.world(0,VanStaging.FRONT-VanStaging.RAMP_LENGTH-.75);
            goal=new Vec(end.x(),van.placement().y(),end.z());
        }else if(room!=goalRoom){
            Portal crossing=null;
            // Choose an entrance by travel distance from the van apron, and on the
            // way back to the van. Inside, retain the nearest equal-hop doorway.
            double nearest=Double.POSITIVE_INFINITY;
            boolean vanRoute=goalRoom==HouseMap.EXTRACTION_ROOM||distances[room][HouseMap.EXTRACTION_ROOM]==1;
            for(var p:map.level().portalsFrom(room)){
                int next=room==p.a()?p.b():p.a();
                if(blockedPortals.contains(p.id()))continue;
                double distance=Math.hypot(pos.x()-p.x(),pos.z()-p.z());
                if(vanRoute){
                    if(routeLength[next][goalRoom]>=routeLength[room][goalRoom])continue;
                    distance+=distanceToCenter(next,p)+routeLength[next][goalRoom];
                }else if(distances[next][goalRoom]!=distances[room][goalRoom]-1)continue;
                if(distance<nearest){nearest=distance;crossing=p;}
            }
            if(crossing==null)return Input.simple(Action.WAIT);
            int next=crossing.a()==room?crossing.b():crossing.a();
            double distance=Math.hypot(pos.x()-crossing.x(),pos.z()-crossing.z());
            if(crossing.door()&&!map.doorOpen(crossing.id())){
                if(distance<1.50&&crossing.equals(map.nearestDoor(pos.x(),pos.y(),pos.z()))){
                    if(tick-lastDoor<6)return Input.simple(Action.WAIT);
                    lastDoor=tick;
                    reset();return Input.simple(Action.DOOR);
                }
                goal=sideOfPortal(crossing,room,.9);
            }else if(crossing.kind().equals("stair")){
                for(var stair:map.level().stairs())if(stair.a()==room&&stair.b()==next||stair.b()==room&&stair.a()==next){
                    var bottom=new Vec(stair.x0(),stair.y0(),stair.z0());
                    var top=new Vec(stair.x1(),stair.y1(),stair.z1());
                    approachingStair=true;
                    var entry=room==stair.a()?bottom:top;
                    var exit=room==stair.a()?top:bottom;
                    // The lower floor extends under this flight. Feet close to its entry
                    // in plan can still be underneath the ramp and must approach its foot.
                    boolean onSurface=Math.abs(pos.y()-stair.yAt(pos.x(),pos.z()))<.01;
                    boolean atEntry=separation(pos,entry)<.45;
                    goal=onSurface&&(stair.contains(pos.x(),pos.z(),.15)||atEntry)?exit:entry;
                    break;
                }
            }else if(next==HouseMap.EXTRACTION_ROOM){
                var center=van.world(0,VanStaging.FRONT+.7);
                goal=new Vec(center.x(),van.floorY(),center.z());
            }else{
                goal=sideOfPortal(crossing,next,.75);
            }
        }else if(Math.abs(pos.y()-goal.y())>=.45){
            // A hunter can be over the circle in plan view, part-way up
            // a staircase whose enclosing room still has the ground-floor ID.
            for(var stair:map.level().stairs()){
                if(!stair.contains(pos.x(),pos.z(),.15)||Math.abs(pos.y()-stair.yAt(pos.x(),pos.z()))>.28)continue;
                boolean descend=goal.y()<pos.y();
                goal=descend?new Vec(stair.x0(),stair.y0(),stair.z0()):new Vec(stair.x1(),stair.y1(),stair.z1());
                reach=.42;break;
            }
        }
        if(target==null||separation(goal,target)>.4){reset();target=goal;}
        if(previous!=null&&separation(pos,previous)<.015)stationary++;
        else stationary=0;
        previous=pos;
        while(!waypoints.isEmpty()&&separation(pos,waypoints.peek())<.23)waypoints.remove();
        if(search==null&&waypoints.isEmpty()&&tick>=failedUntil){
            // Direct sweeps are cheap on an open floor; use a planned detour when
            // even one of the host's diagonal clearance tests rejects a step.
            if(stationary<2&&separation(pos,goal)>.08){
                var step=walk(pos,goal,!approachingStair);
                if(step!=null)return step;
            }
            search=new Search(pos,goal,reach);stationary=0;
        }
        if(search!=null){
            search.advance();
            if(search.finished){
                stationary=0;
                if(waypoints.isEmpty())failedUntil=tick+15;
                search=null;
            }
        }
        if(!waypoints.isEmpty()){
            var command=walk(pos,waypoints.peek());
            if(command!=null&&stationary<3)return command;
            waypoints.clear();stationary=0;
        }
        return Input.simple(Action.WAIT);
    }
    private Vec sideOfPortal(Portal portal,int room,double offset){
        double sign=Math.signum(portal.alongX()?map.centerZ(room)-portal.z():map.centerX(room)-portal.x());
        if(sign==0)sign=1;
        double dx=portal.alongX()?0:sign*offset,dz=portal.alongX()?sign*offset:0;
        var side=new Vec(portal.x()+dx,portal.y(),portal.z()+dz);
        // An exterior room can wrap around a building, with its center on the
        // indoor side of the threshold. Check the actual room at both positions.
        if(map.roomAt(side.x(),side.y(),side.z())!=room
                &&map.roomAt(portal.x()-dx,portal.y(),portal.z()-dz)==room)
            return new Vec(portal.x()-dx,portal.y(),portal.z()-dz);
        return side;
    }
    /** Try short, collision-checked escape steps away from an observed attacker. */
    Input evade(LanWorld.Body self,double bearing){
        Vec pos=new Vec(self.x(),self.y(),self.z());
        for(double offset:new double[]{0,.5,-.5,1,-1,1.5,-1.5}){
            var step=walk(pos,new Vec(pos.x()+Math.sin(bearing+offset),pos.y(),pos.z()+Math.cos(bearing+offset)));
            if(step!=null)return step;
        }
        return null;
    }
    private Input walk(Vec pos,Vec goal){return walk(pos,goal,false);}
    private Input walk(Vec pos,Vec goal,boolean direct){
        double dx=goal.x()-pos.x(),dz=goal.z()-pos.z(),length=Math.hypot(dx,dz);
        if(length<.001)return null;
        int x=(int)Math.round(dx/length*100),z=(int)Math.round(dz/length*100);
        double scale=.31/Math.hypot(x,z);
        Vec after=clear(pos,pos.x()+x*scale,pos.z()+z*scale);
        if(after==null||separation(after,goal)>=separation(pos,goal))return null;
        // A straight line to the next doorway can double back down the flight.
        // Ask the local search for a walkable landing route before taking that step.
        if(direct&&Math.abs(after.y()-goal.y())>Math.abs(pos.y()-goal.y())+.03)
            for(var stair:map.level().stairs())
                if(stair.contains(after.x(),after.z(),.15)&&Math.abs(after.y()-stair.yAt(after.x(),after.z()))<.01)return null;
        return Input.walk(x,z);
    }
    private Vec clear(Vec pos,double x,double z){
        Vec next=map.resolveMove(pos,x,z);
        if(next==null)return null;
        if(Math.abs(pos.x()-x)>.0001&&Math.abs(pos.z()-z)>.0001&&
            (map.resolveMove(pos,x,pos.z())==null||map.resolveMove(pos,pos.x(),z)==null))return null;
        return next;
    }
    private static double separation(Vec a,Vec b){
        return Math.hypot(Math.hypot(a.x()-b.x(),a.z()-b.z()),a.y()-b.y());
    }
    private record Cell(int x,int z,int height){}
    private record Node(Cell cell,Vec point,Node parent,double cost,double estimate){}
    /** Local, bounded A* search. Spread expensive geometry queries across decision ticks. */
    private final class Search {
        // One ordinary walking step preserves precise clearance around narrow
        // door leaves. Axis-aligned neighbors also obey the host corner rule.
        private static final double GRID=.31;
        private static final int[][] DIRECTIONS={{1,0},{-1,0},{0,1},{0,-1}};
        final PriorityQueue<Node> open=new PriorityQueue<>(Comparator.comparingDouble(Node::estimate));
        final HashMap<Cell,Double> best=new HashMap<>();
        final Vec goal,start;
        Vec origin;
        int alignment;
        final double reach;
        boolean finished;
        Search(Vec start,Vec goal,double reach){
            this.goal=goal;this.start=start;origin=start;this.reach=reach;
            var root=new Cell(0,0,(int)Math.round(start.y()*3));
            open.add(new Node(root,start,null,0,separation(start,goal)));best.put(root,0.0);
        }
        void advance(){
            for(int i=0;i<40&&!open.isEmpty()&&best.size()<2200;i++){
                var node=open.remove();
                if(node.cost()>best.get(node.cell()))continue;
                if(node.parent()!=null&&Math.abs(node.point().y()-goal.y())<.45
                        &&Math.hypot(node.point().x()-goal.x(),node.point().z()-goal.z())<reach){
                    for(var n=node;n.parent()!=null;n=n.parent())waypoints.addFirst(n.point());
                    finished=true;return;
                }
                for(var direction:DIRECTIONS){
                    int x=node.cell().x()+direction[0],z=node.cell().z()+direction[1];
                    double cost=node.cost()+GRID*Math.hypot(direction[0],direction[1]);
                    double nx=origin.x()+GRID*x,nz=origin.z()+GRID*z;
                    // Most room floors are flat. Skip a known equal-cost horizontal
                    // neighbor before performing another expensive geometry sweep.
                    var sameHeight=new Cell(x,z,node.cell().height());
                    if(best.getOrDefault(sameHeight,Double.POSITIVE_INFINITY)<=cost+1e-7)continue;
                    var next=clear(node.point(),nx,nz);if(next==null)continue;
                    var key=new Cell(x,z,(int)Math.round(next.y()*3));
                    if(best.getOrDefault(key,Double.POSITIVE_INFINITY)<=cost+1e-7)continue;
                    best.put(key,cost);
                    open.add(new Node(key,next,node,cost,cost+1.35*separation(next,goal)));
                }
            }
            if(!open.isEmpty()&&best.size()<2200)return;
            // A doorway can fall between all rows of this step-sized grid.
            // Retry on a shifted grid reached by one legal diagonal step.
            while(alignment<4){
                int i=alignment++;
                double delta=GRID/Math.sqrt(2);
                Vec shifted=clear(start,start.x()+(i%2==0?delta:-delta),start.z()+(i<2?delta:-delta));
                if(shifted==null)continue;
                origin=shifted;open.clear();best.clear();
                Cell cell=new Cell(0,0,(int)Math.round(shifted.y()*3));
                Node first=new Node(null,start,null,0,0);
                open.add(new Node(cell,shifted,first,GRID,GRID+separation(shifted,goal)));
                best.put(cell,GRID);return;
            }
            finished=true;
        }
    }
}
