package org.beckenburg;

import java.util.*;

/**
 * First campaign field assistant. Uses authored portal routing, swept Hunter
 * collision, actual feet elevation and a separately issued thermometer.
 * No ghost state is available to its decisions; observed air is always raw.
 */
public final class CampaignAlly {
    public enum Order { FOLLOW, HOLD, INVESTIGATE }
    private final HouseMap map;
    private final InvestigationSession caseSession;
    private LevelDefinition.Vec position;
    private Order order=Order.FOLLOW;
    private int destination=-1,heading=0,lastReading=-10000;
    private String report="Awaiting field orders.";
    private final EquipmentLoadout.Item issuedTool=EquipmentLoadout.Item.THERMOMETER;
    public CampaignAlly(HouseMap map,InvestigationSession session){
        this.map=Objects.requireNonNull(map);
        caseSession=Objects.requireNonNull(session);
        var spawn=map.van().spawn(1);
        position=new LevelDefinition.Vec(spawn.x(),map.floorY(HouseMap.EXTRACTION_ROOM),spawn.z());
        if(!map.traversable(position.x(),position.y(),position.z()))
            throw new IllegalStateException("Assistant has no valid starting position");
    }
    public LevelDefinition.Vec position(){return position;}
    public int room(){return map.roomAt(position.x(),position.y(),position.z());}
    public Order order(){return order;}
    public String report(){return report;}
    public int destination(){return destination;}
    public void follow(){order=Order.FOLLOW;destination=-1;report="Following investigator."; }
    public void hold(){order=Order.HOLD;destination=-1;report="Holding at "+map.roomName(room())+"."; }
    public void investigate(int zone){
        if(zone<0||zone>=HouseMap.ROOM_COUNT)
            throw new IllegalArgumentException("Unknown investigation zone");
        order=Order.INVESTIGATE;destination=zone;
        report="Heading to "+map.roomName(zone)+".";
    }
    /** A real issued instrument; never automatically completes the journal. */
    public String readThermometer(){
        if(issuedTool!=EquipmentLoadout.Item.THERMOMETER)throw new IllegalStateException("No thermometer");
        if(caseSession.ticks()-lastReading<30){
            report="Thermometer is still settling.";return report;
        }
        String raw=caseSession.rawTemperatureAt(position.x(),position.y(),position.z());
        report=raw==null?"Cannot measure air from here.":
            "T"+caseSession.ticks()+" / "+map.roomName(room())+" / "+raw;
        if(raw!=null)lastReading=caseSession.ticks();
        return report;
    }
    private List<Integer> route(int from,int to){
        if(from==to)return List.of(from);
        if(from<0||to<0)return List.of();
        int[] parent=new int[HouseMap.ROOM_COUNT];Arrays.fill(parent,-1);
        ArrayDeque<Integer> queue=new ArrayDeque<>();queue.add(from);parent[from]=from;
        while(!queue.isEmpty()&&parent[to]==-1){
            int here=queue.remove();
            for(int next:map.adjacentRooms(here))if(parent[next]==-1){
                parent[next]=here;queue.add(next);
            }
        }
        if(parent[to]==-1)return List.of();
        ArrayList<Integer> path=new ArrayList<>();
        for(int at=to;at!=from;at=parent[at])path.add(at);
        path.add(from);Collections.reverse(path);return path;
    }
    /** Ten steps per second, like the solo simulation clock. */
    public void advance(double playerX,double playerY,double playerZ){
        if(caseSession.outcome()!=InvestigationSession.Outcome.ACTIVE||order==Order.HOLD)return;
        int targetRoom=order==Order.FOLLOW?map.roomAt(playerX,playerY,playerZ):destination;
        if(targetRoom<0)return;
        int here=room();
        if(here<0){report="Navigation blocked; waiting for a route.";return;}
        double goalX=order==Order.FOLLOW?playerX:map.centerX(targetRoom);
        double goalZ=order==Order.FOLLOW?playerZ:map.centerZ(targetRoom);
        if(here!=targetRoom){
            var path=route(here,targetRoom);
            if(path.size()<2){report="No reachable route to "+map.roomName(targetRoom)+".";return;}
            int next=path.get(1);
            LevelDefinition.Portal portal=null;
            for(var p:map.level().portalsFrom(here))
                if(!p.kind().equals("window")&&p.joins(here,next)){portal=p;break;}
            if(portal==null)return;
            double atDoor=Math.hypot(position.x()-portal.x(),position.z()-portal.z());
            if(portal.door()&&!map.doorOpen(portal.id())&&atDoor<=1.10)
                map.openDoorBetweenRooms(here,next);
            // Portal width lies ALONG its tangent, not toward the centre of an
            // often enormous outdoor room. A diagonal centre vector would walk
            // off the narrow van ramp and collide with the cargo door frame.
            // Approach the portal at its centre, then cross perpendicular to it.
            double beyond=atDoor<1.30?.85:0;
            if(portal.joins(HouseMap.EXTRACTION_ROOM,13)){
                // The outdoor room centre is NOT necessarily outside the van:
                // large farmhouse and school aprons may centre behind the cargo
                // threshold. Use the actual dock orientation, not room centroids.
                var centreline=map.van().world(0,VanStaging.FRONT+
                    (next==HouseMap.EXTRACTION_ROOM?beyond:-beyond));
                goalX=centreline.x();goalZ=centreline.z();
            }else if(portal.alongX()){
                goalX=portal.x();
                goalZ=portal.z()+Math.signum(map.centerZ(next)-portal.z())*beyond;
            }else{
                goalX=portal.x()+Math.signum(map.centerX(next)-portal.x())*beyond;
                goalZ=portal.z();
            }
        }else if(here!=HouseMap.EXTRACTION_ROOM&&
                targetRoom!=HouseMap.EXTRACTION_ROOM&&
                position.y()>.10&&map.van().supportHeight(position.x(),position.z())!=null){
            // After crossing the exterior room boundary, the companion is
            // still physically on the descending cargo ramp. Finish that
            // descent along its clear centreline before pursuing room goals.
            var bottom=map.van().world(0,VanStaging.FRONT-VanStaging.RAMP_LENGTH-.14);
            goalX=bottom.x();goalZ=bottom.z();
        }else if(Math.hypot(position.x()-goalX,position.z()-goalZ)
                <(order==Order.FOLLOW?1.55:.65)){
            if(order==Order.INVESTIGATE){
                readThermometer();order=Order.HOLD;destination=-1;
            }
            return;
        }
        double dx=goalX-position.x(),dz=goalZ-position.z(),distance=Math.hypot(dx,dz);
        if(distance<.00001)return;
        double speed=Math.min(.21,distance),headingRad=Math.atan2(dx,dz);
        heading=Math.floorMod((int)Math.round(Math.toDegrees(headingRad)),360);
        // No coordinate jump: a companion must move using the same swept
        // collision/elevation function that controls human solo movement.
        LevelDefinition.Vec candidate=null;
        for(double correction:new double[]{0,.40,-.40,.85,-.85,1.25,-1.25}){
            double angle=headingRad+correction;
            var next=map.resolveMove(position,position.x()+Math.sin(angle)*speed,
                position.z()+Math.cos(angle)*speed);
            if(next!=null&&Math.hypot(next.x()-position.x(),next.z()-position.z())>.035){
                candidate=next;break;
            }
        }
        if(candidate!=null)position=candidate;
    }
    public SceneRenderer.RemoteBody sprite(){
        return new SceneRenderer.RemoteBody(position.x(),position.y(),position.z(),
            1,heading,"observer");
    }
}
