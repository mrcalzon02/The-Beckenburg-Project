package org.beckenburg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Live development AI driver. One driver owns exactly one genuine
 * LanClient; its only observations are that client's delivered LAN frames and
 * the same static authored map a normal game client can load. No LanHost,
 * DevAdminSnapshot, GhostDomain or LanWorld mutation paths exist here.
 *
 * Hunter seats use the same investigator and equipment commands as campaign hires.
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
    private final CampaignHunterInvestigator investigator;
    private final LanWorld.Body[] hunterBodies=new LanWorld.Body[6];
    private final int hunterIndex;
    private int tick=-1,room=-1,lastMotionTick=-1,lastGhostEvent=-1000,blocked,majorAttempts;
    private double x,y,z;
    private boolean observedPose;
    private volatile boolean closed;
    private String legalActions="[]";
    private GhostFrenzy.Phase frenzy=GhostFrenzy.Phase.CALM;
    private LanGhostHearing.Event heard;
    private int lastFrenzyRequest=-1000,lastFrenzyAttack=-1000,lastStrikeAttempt=-1000;
    private int health=100,stress,ghostHeading=180;
    private final java.util.concurrent.atomic.AtomicInteger frenzyStarts=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger effectiveAttacks=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger goodScans=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger acceptedHypotheses=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger ritesStarted=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger commands=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger accepted=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger acceptedMoves=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger rejected=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger attacks=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger identifications=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger rituals=new java.util.concurrent.atomic.AtomicInteger();
    record Stats(String seat,int commands,int accepted,int acceptedMoves,int rejected,int attacks,
                 int identifications,int rituals,int observations,int health,int stress,
                 int roomsVisited,boolean enteredSite,boolean enteredInterior,
                 int finalRoom,double finalX,double finalZ,int blockedSteps,
                 int frenzyStarts,int effectiveAttacks,int goodScans,int acceptedHypotheses,
                 int ritesStarted,String hypothesis,String state){}
    Stats stats(){
        return new Stats(seat,commands.get(),accepted.get(),acceptedMoves.get(),rejected.get(),
            attacks.get(),identifications.get(),rituals.get(),ghost?0:investigator.observationCount(),
            health,stress,visited.size(),
            visited.keySet().stream().anyMatch(r->r<HouseMap.EXTRACTION_ROOM),
            visited.keySet().stream().anyMatch(r->r<HouseMap.INDOOR_ROOMS),
            room,x,z,blocked,
            frenzyStarts.get(),effectiveAttacks.get(),goodScans.get(),
            acceptedHypotheses.get(),ritesStarted.get(),
            ghost?"":String.valueOf(investigator.hypothesis()),dashboard.getOrDefault(seat,""));
    }
    private final Map<Integer,Integer> visited=new HashMap<>();

    DevMatchClientAi(LanClient client,HouseMap.Site site,Map<String,String> dashboard,int seed){
        this.client=client;seat=client.seat();ghost=seat.equals("GHOST");
        map=new HouseMap(site);
        hunterIndex=ghost?-1:Integer.parseInt(seat.substring(1))-1;
        investigator=ghost?null:new CampaignHunterInvestigator(map,hunterIndex+1);
        if(!ghost)investigator.assignSeat(hunterIndex);
        this.dashboard=dashboard;
        random=new Random(0xBEECE0L+seed*109L);
        executor=Executors.newSingleThreadScheduledExecutor(r->{
            Thread t=new Thread(r,"beckenburg-dev-ai-"+seat);t.setDaemon(true);return t;
        });
        dashboard.put(seat,"booting real LAN AI client");
    }
    void onFrame(String frame){if(!closed)inbox.offer(frame);}
    void begin(){begin(180);}
    void begin(int intervalMillis){
        // Apply initial site/pose/casefile snapshots before AI decisions begin,
        // but never count scripted host/lobby setup acknowledgments as actions
        // performed by this autonomous client.
        processFrames();
        accepted.set(0);acceptedMoves.set(0);rejected.set(0);
        frenzyStarts.set(0);effectiveAttacks.set(0);goodScans.set(0);
        acceptedHypotheses.set(0);ritesStarted.set(0);
        executor.scheduleWithFixedDelay(this::updateSafely,intervalMillis,intervalMillis,TimeUnit.MILLISECONDS);
    }
    private void updateSafely(){
        if(closed)return;
        try{processFrames();if(tick>=0&&observedPose)act();}
        catch(Exception error){dashboard.put(seat,"AI exception: "+error.getMessage());}
    }
    private void processFrames(){
        String frame;
        while((frame=inbox.poll())!=null){
            try{
                if(!ghost)investigator.accept(frame);
                var p=LanProtocol.parts(frame);
                switch(p.get(0)){
                    case "BODY" -> {
                        if(ghost||p.size()!=7)break;
                        int index=LanProtocol.integer(p.get(1),0,5);
                        double bx=Integer.parseInt(p.get(2))/1000.0,by=Integer.parseInt(p.get(3))/1000.0,
                            bz=Integer.parseInt(p.get(4))/1000.0;
                        int localRoom=map.roomAt(bx,by,bz);
                        hunterBodies[index]=new LanWorld.Body(bx,by,bz,Integer.parseInt(p.get(5)),localRoom);
                        if(index!=hunterIndex)break;
                        x=bx;y=by;z=bz;observedPose=true;
                        if(room!=localRoom&&localRoom>=0)visited.merge(localRoom,1,Integer::sum);
                        room=localRoom;
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
                        ghostHeading=Integer.parseInt(p.get(5));
                        observedPose=true;
                        if(prior!=room&&room>=0)visited.merge(room,1,Integer::sum);
                    }
                    case "GHEAR" -> {
                        if(ghost)heard=LanGhostHearing.decode(frame);
                    }
                    case "FRENZY" -> {
                        if(ghost&&p.size()>=2)frenzy=GhostFrenzy.Phase.valueOf(p.get(1));
                    }
                    case "PROGRESS" -> {
                        if(!ghost&&p.size()>=3){
                            health=Integer.parseInt(p.get(1));stress=Integer.parseInt(p.get(2));
                        }
                    }
                    case "STATE" -> {
                        if(p.size()>=2)tick=Math.max(tick,Integer.parseInt(p.get(1)));
                        if(ghost&&p.size()>=6)legalActions=p.get(5);
                    }
                    case "DOORS" -> {
                        if(p.size()==2)map.applyDoorSnapshot(p.get(1));
                    }
                    case "ACCEPT","REJECT" -> {
                        if(frame.startsWith("ACCEPT|")){
                            accepted.incrementAndGet();
                            if(frame.startsWith("ACCEPT|STEP")||frame.startsWith("ACCEPT|GSTEP"))
                                acceptedMoves.incrementAndGet();
                            if(frame.startsWith("ACCEPT|FRENZY"))frenzyStarts.incrementAndGet();
                            if(frame.startsWith("ACCEPT|FATTACK")||frame.startsWith("ACCEPT|GHOST|STRIKE"))
                                effectiveAttacks.incrementAndGet();
                            if(frame.startsWith("ACCEPT|SCAN|")||frame.startsWith("ACCEPT|GEARUSE|"))goodScans.incrementAndGet();
                            if(frame.startsWith("ACCEPT|IDENTIFY"))acceptedHypotheses.incrementAndGet();
                            if(frame.startsWith("ACCEPT|BANISH")||frame.startsWith("ACCEPT|ABSOLVE")
                                    ||frame.startsWith("ACCEPT|CLEANSE"))ritesStarted.incrementAndGet();
                        }else if(frame.startsWith("REJECT|"))rejected.incrementAndGet();
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
    private void sendAction(String name,Action action)throws IOException{
        commands.incrementAndGet();
        action.perform();
        dashboard.put(seat,name+" / tick "+tick);
    }
    @FunctionalInterface private interface Action {void perform()throws IOException;}
    private void act()throws IOException {
        if(tick==lastMotionTick)return; // host movement is one input per authoritative tick
        lastMotionTick=tick;
        if(ghost)ghostTurn();else hunterTurn();
    }
    private void hunterTurn()throws IOException {
        if(room<0||health<=0)return;
        var input=investigator.decide(tick,hunterBodies[hunterIndex],hunterBodies[investigator.directorSeat()]);
        if(input==null||input.action()==CampaignGuestBot.Action.WAIT)return;
        if(input.action()==CampaignGuestBot.Action.IDENTIFY)identifications.incrementAndGet();
        if(input.action()==CampaignGuestBot.Action.RITE)rituals.incrementAndGet();
        sendAction(input.action().name(),()->input.send(client));
    }
    static VanStaging.Position vanExitWaypoint(VanStaging van,double x,double z){
        // The old staging point sat almost on its own switch threshold.
        // A single .31 m STEP put H2/H5 inside the .10 m goal dead-zone,
        // but still on the first branch. Cross the switch threshold BEFORE
        // reaching that goal; preserve the real van aisle and cargo ramp.
        var local=van.local(x,z);
        return van.world(0,local.z()>VanStaging.FRONT+.65
            ?VanStaging.FRONT+.15:VanStaging.FRONT-.95);
    }
    static int preferredExteriorEntrance(HouseMap map,int room,int hunterIndex){
        if(!map.outside(room))return -1;
        List<Integer> entrances=new ArrayList<>();
        for(int next:map.adjacentRooms(room))
            if(next!=HouseMap.EXTRACTION_ROOM&&!map.outside(next))
                entrances.add(next);
        if(entrances.isEmpty())return -1;
        entrances.sort(Integer::compareTo);
        return entrances.get(Math.floorMod(hunterIndex,entrances.size()));
    }
    /** Only GHEAR bearing, own pose, own legal moves and frenzy frames inform ghost AI. */
    private void ghostTurn()throws IOException {
        if(room<0)return;
        if(frenzy==GhostFrenzy.Phase.RAGING){
            if(tick-lastFrenzyAttack>=14){
                lastFrenzyAttack=tick;
                GhostFrenzy.Attack attack=majorAttempts++%3==0
                    ?GhostFrenzy.Attack.DREAD:GhostFrenzy.Attack.CLAW;
                attacks.incrementAndGet();
                sendAction("FATTACK "+attack,()->client.frenzyAttack(attack));
                return;
            }
        }else if(frenzy==GhostFrenzy.Phase.CALM){
            if(tick>=135&&tick-lastFrenzyRequest>=75){
                lastFrenzyRequest=tick;
                attacks.incrementAndGet();
                sendAction("FRENZY attempt",client::frenzy);
                return;
            }
            if(legalActions.contains("STRIKE")&&tick-lastStrikeAttempt>=28){
                lastStrikeAttempt=tick;
                attacks.incrementAndGet();
                sendAction("STRIKE reach test",
                    ()->client.ghost(GhostDomain.Action.STRIKE,room));
                return;
            }
            if(tick-lastGhostEvent>=45){
                GhostDomain.Action chosen=null;
                for(GhostDomain.Action action:new GhostDomain.Action[]{
                        GhostDomain.Action.MANIFEST,GhostDomain.Action.SIGNATURE,
                        GhostDomain.Action.WHISPER,GhostDomain.Action.DISTURB,
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
                        if(!exits.isEmpty()){
                            // A GHEAR cue is directional only, never a
                            // hidden room/coordinate. Prefer a connected door
                            // facing the audible bearing; roam if the cue fades.
                            int choice=random.nextInt(exits.size());
                            if(heard!=null&&tick-heard.tick()<35){
                                double requested=heardHeading();
                                double best=-Double.MAX_VALUE;
                                for(int i=0;i<exits.size();i++){
                                    int roomChoice=exits.get(i);
                                    double dx=map.centerX(roomChoice)-x;
                                    double dz=map.centerZ(roomChoice)-z;
                                    double bearing=Math.toRadians(requested);
                                    double score=(dx*Math.sin(bearing)+dz*Math.cos(bearing))/
                                        Math.max(.01,Math.hypot(dx,dz));
                                    if(score>best){best=score;choice=i;}
                                }
                            }
                            nextRoom=exits.get(choice);
                        }
                    }
                    lastGhostEvent=tick;
                    int dest=nextRoom;
                    GhostDomain.Action action=chosen;
                    sendAction("GHOST "+action+" R"+dest,
                        ()->client.ghost(action,dest));
                    return;
                }
            }
        }
        // A moving ghost cannot listen. Leave time for the host settling gate.
        boolean tracking=heard!=null&&tick-heard.tick()<25;
        if(!tracking&&tick%32>=18)return;
        double heading=tracking?heardHeading():Math.floorMod(tick/32*71,360);
        double radians=Math.toRadians(heading);
        int dx=(int)Math.round(100*Math.sin(radians)),dz=(int)Math.round(100*Math.cos(radians));
        sendAction("GSTEP "+dx+","+dz,()->client.ghostStep(dx,dz));
    }
    private double heardHeading(){
        return ghostHeading+switch(heard.direction()){
            case AHEAD,HERE -> 0;
            case LEFT -> 90;
            case RIGHT -> -90;
            case BEHIND -> 180;
        };
    }
    @Override public void close(){
        closed=true;
        executor.shutdownNow();
    }
}
