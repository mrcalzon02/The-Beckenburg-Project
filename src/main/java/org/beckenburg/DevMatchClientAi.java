package org.beckenburg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Minimal live development AI driver. One driver owns exactly one genuine
 * LanClient; its only observations are that client's delivered LAN frames and
 * the same static authored map a normal game client can load. No LanHost,
 * DevAdminSnapshot, GhostDomain or LanWorld mutation paths exist here.
 *
 * This is a navigation/scan/haunt smoke controller, NOT yet full investigation AI.
 */
final class DevMatchClientAi implements AutoCloseable {
    private final LanClient client;
    private final String seat;
    private final boolean ghost;
    private final HouseMap map;
    private final Map<String,String> dashboard;
    private final ConcurrentLinkedQueue<String> inbox=new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService executor;
    private final Random random;
    private int tick=-1,room=-1,previousRoom=-1,targetRoom=-1,lastMotionTick=-1;
    private int lastScanTick=-1000,lastDoorTick=-1000,lastGhostEvent=-1000,blocked;
    private int lastActionRoom=-1,majorAttempts;
    private double x,y,z,lastX=Double.NaN,lastZ=Double.NaN;
    private boolean observedPose,closed;
    private String legalActions="[]";
    private final Map<Integer,Integer> visited=new HashMap<>();

    DevMatchClientAi(LanClient client,HouseMap.Site site,Map<String,String> dashboard,int seed){
        this.client=client;seat=client.seat();ghost=seat.equals("GHOST");
        map=new HouseMap(site);
        this.dashboard=dashboard;
        random=new Random(0xBEECE0L+seed*109L);
        executor=Executors.newSingleThreadScheduledExecutor(r->{
            Thread t=new Thread(r,"beckenburg-dev-ai-"+seat);t.setDaemon(true);return t;
        });
        dashboard.put(seat,"booting real LAN AI client");
    }
    void onFrame(String frame){if(!closed)inbox.offer(frame);}
    void begin(){executor.scheduleWithFixedDelay(this::updateSafely,180,180,TimeUnit.MILLISECONDS);}
    private void updateSafely(){
        if(closed)return;
        try{processFrames();if(tick>=0&&observedPose)act();}
        catch(Exception error){dashboard.put(seat,"AI exception: "+error.getMessage());}
    }
    private void processFrames(){
        String frame;
        while((frame=inbox.poll())!=null){
            try{
                var p=LanProtocol.parts(frame);
                switch(p.get(0)){
                    case "BODY" -> {
                        if(ghost||p.size()<7||Integer.parseInt(p.get(1))!=Integer.parseInt(seat.substring(1))-1)
                            break;
                        x=Integer.parseInt(p.get(2))/1000.0;
                        y=Integer.parseInt(p.get(3))/1000.0;
                        z=Integer.parseInt(p.get(4))/1000.0;
                        observedPose=true;
                        int prior=room;
                        room=map.roomAt(x,y,z);
                        if(prior!=room&&room>=0){visited.merge(room,1,Integer::sum);targetRoom=-1;}
                        tick=Math.max(tick,Integer.parseInt(p.get(6)));
                    }
                    case "GPOSE" -> {
                        if(!ghost||p.size()<7)break;
                        int prior=room;
                        room=Integer.parseInt(p.get(1));
                        x=Integer.parseInt(p.get(2))/1000.0;
                        y=Integer.parseInt(p.get(3))/1000.0;
                        z=Integer.parseInt(p.get(4))/1000.0;
                        tick=Math.max(tick,Integer.parseInt(p.get(6)));
                        observedPose=true;
                        if(prior!=room){previousRoom=prior;targetRoom=-1;}
                    }
                    case "STATE" -> {
                        if(p.size()>=2)tick=Math.max(tick,Integer.parseInt(p.get(1)));
                        if(ghost&&p.size()>=6)legalActions=p.get(5);
                    }
                    case "DOORS" -> {
                        if(p.size()==2)map.applyDoorSnapshot(p.get(1));
                    }
                    case "ACCEPT","REJECT" -> {
                        if(frame.startsWith("REJECT|")&&frame.contains("blocked"))
                            blocked++;
                        else if(frame.startsWith("ACCEPT|STEP")||frame.startsWith("ACCEPT|GSTEP"))
                            blocked=Math.max(0,blocked-1);
                        dashboard.put(seat,frame.length()>78?frame.substring(0,78):frame);
                    }
                    default -> { }
                }
            }catch(RuntimeException invalid){dashboard.put(seat,"unreadable own client frame: "+invalid.getMessage());}
        }
    }
    private void act()throws IOException {
        if(tick==lastMotionTick)return; // host movement is one input per authoritative tick
        lastMotionTick=tick;
        if(ghost)ghostTurn();else hunterTurn();
    }
    private void hunterTurn()throws IOException {
        if(room<0)return;
        if(tick-lastScanTick>=145+random.nextInt(25)){
            lastScanTick=tick;
            LanCase.Tool tool=LanCase.Tool.values()[Math.floorMod((tick/140)+Integer.parseInt(seat.substring(1)),LanCase.Tool.values().length)];
            client.scan(tool);
            dashboard.put(seat,"SCAN "+tool+" / R"+room);
        }
        if(room!=lastActionRoom){
            lastActionRoom=room;targetRoom=-1;blocked=0;
        }
        if(targetRoom<0||targetRoom==room||!map.adjacentRooms(room).contains(targetRoom))
            targetRoom=chooseNextRoom();
        if(targetRoom<0)return;
        LevelDefinition.Portal portal=null;
        for(var candidate:map.level().portalsFrom(room))
            if(candidate.joins(room,targetRoom)&&!candidate.kind().equals("window")){
                portal=candidate;break;
            }
        if(portal==null){targetRoom=-1;return;}
        double dist=Math.hypot(x-portal.x(),z-portal.z());
        if(portal.door()&&!map.doorOpen(portal.id())&&dist<1.35&&tick-lastDoorTick>=18){
            lastDoorTick=tick;
            client.door();
            dashboard.put(seat,"DOOR to R"+targetRoom);
            return;
        }
        double gx,gz;
        // Use the actual authored threshold and opposite-room center; never
        // teleport or ask the host where the ghost/other bots are.
        if(dist>0.75){
            gx=portal.x();gz=portal.z();
        }else{
            gx=map.centerX(targetRoom);gz=map.centerZ(targetRoom);
            if(Math.hypot(gx-x,gz-z)>3.0){
                double a=Math.atan2(gx-portal.x(),gz-portal.z());
                gx=portal.x()+Math.sin(a)*1.15;
                gz=portal.z()+Math.cos(a)*1.15;
            }
        }
        double vx=gx-x,vz=gz-z;
        if(Math.hypot(vx,vz)<.10){targetRoom=-1;return;}
        if(blocked>4){
            int phase=(tick/8+Integer.parseInt(seat.substring(1)))%4;
            double theta=Math.atan2(vx,vz)+(phase%2==0?.75:-.75);
            vx=Math.sin(theta);vz=Math.cos(theta);
            if(blocked>24){targetRoom=-1;blocked=0;}
        }
        double d=Math.hypot(vx,vz);
        client.step((int)Math.round(100*vx/d),(int)Math.round(100*vz/d));
        dashboard.put(seat,"EXPLORE R"+room+" -> R"+targetRoom+
            " / "+Math.round(x*10)/10.0+","+Math.round(z*10)/10.0);
        lastX=x;lastZ=z;
    }
    private int chooseNextRoom(){
        List<Integer> choices=new ArrayList<>(map.adjacentRooms(room));
        choices.removeIf(r->r==HouseMap.EXTRACTION_ROOM||r==room);
        if(choices.isEmpty())return -1;
        choices.sort(Comparator.comparingInt((Integer r)->visited.getOrDefault(r,0))
              .thenComparingInt(r->Math.floorMod(r*11+Integer.parseInt(seat.substring(1))*7,17)));
        return choices.get(Math.min(choices.size()-1,random.nextInt(Math.min(2,choices.size()))));
    }
    private void ghostTurn()throws IOException {
        if(room<0)return;
        if(tick-lastGhostEvent>=45&&legalActions!=null){
            GhostDomain.Action chosen=null;
            for(GhostDomain.Action action:new GhostDomain.Action[]{
                    GhostDomain.Action.WHISPER,GhostDomain.Action.DISTURB,
                    GhostDomain.Action.SIGNATURE,GhostDomain.Action.MANIFEST,
                    GhostDomain.Action.DRIFT}){
                if(legalActions.contains(action.name())){
                    chosen=action;
                    if(random.nextInt(4)==0)break;
                }
            }
            if(chosen!=null){
                int nextRoom=room;
                if(chosen==GhostDomain.Action.DRIFT){
                    var exits=map.ghostExits(room);
                    if(!exits.isEmpty())nextRoom=exits.get(random.nextInt(exits.size()));
                }
                lastGhostEvent=tick;
                client.ghost(chosen,nextRoom);majorAttempts++;
                dashboard.put(seat,"GHOST "+chosen+" -> R"+nextRoom+" / #"+majorAttempts);
                return;
            }
        }
        int direction=Math.floorMod(tick/12+3,4);
        int[][] steps={{90,0},{0,90},{-90,0},{0,-90}};
        int[] v=steps[direction];
        client.ghostStep(v[0],v[1]);
        dashboard.put(seat,"HAUNT R"+room+" / step "+v[0]+","+v[1]);
    }
    @Override public void close(){
        closed=true;
        executor.shutdownNow();
    }
}
