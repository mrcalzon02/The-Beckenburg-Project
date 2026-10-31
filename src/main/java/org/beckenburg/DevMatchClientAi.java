package org.beckenburg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
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
    private GhostFrenzy.Phase frenzy=GhostFrenzy.Phase.CALM;
    private LanGhostHearing.Event heard;
    private int lastFrenzyRequest=-1000,lastFrenzyAttack=-1000,lastStrikeAttempt=-1000;
    private int lastIdentifyRequest=-1000,lastRiteRequest=-1000,lastWardRequest=-1000;
    private int health=100,stress,seenScanCount,observedSignals,suspectedAnchor=-1,ghostHeading=180;
    private GhostIdentity.Type hypothesis;
    private boolean guessAttempted;
    private final FieldJournal notes=new FieldJournal();
    private final Set<String> distinctSamples=new HashSet<>();
    private final Map<Integer,Integer> emfStrongRooms=new HashMap<>();
    private final Map<Integer,Integer> coldRooms=new HashMap<>();
    private final Map<Integer,Integer> hotRooms=new HashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger frenzyStarts=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger effectiveAttacks=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger goodScans=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger correctResponses=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger ritesStarted=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger commands=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger accepted=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger rejected=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger attacks=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger identifications=new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger rituals=new java.util.concurrent.atomic.AtomicInteger();
    record Stats(String seat,int commands,int accepted,int rejected,int attacks,
                 int identifications,int rituals,int observations,int health,int stress,
                 int roomsVisited,boolean enteredSite,
                 int frenzyStarts,int effectiveAttacks,int goodScans,int correctResponses,
                 int ritesStarted,String hypothesis,String state){}
    Stats stats(){
        return new Stats(seat,commands.get(),accepted.get(),rejected.get(),
            attacks.get(),identifications.get(),rituals.get(),observedSignals,
            health,stress,visited.size(),visited.keySet().stream().anyMatch(r->r<HouseMap.EXTRACTION_ROOM),
            frenzyStarts.get(),effectiveAttacks.get(),goodScans.get(),
            correctResponses.get(),ritesStarted.get(),
            String.valueOf(hypothesis),dashboard.getOrDefault(seat,""));
    }
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
                        ghostHeading=Integer.parseInt(p.get(5));
                        observedPose=true;
                        if(prior!=room){previousRoom=prior;targetRoom=-1;}
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
                    case "NOTE" -> {
                        if(!ghost)notes.accept(FieldJournal.Note.decode(frame));
                    }
                    case "CASEFILE" -> {
                        if(!ghost)observeCasefile(LanEvidenceJournal.Entry.decode(frame));
                    }
                    case "IDENTIFIED" -> {
                        if(!ghost&&p.size()>=2)hypothesis=GhostIdentity.Type.valueOf(p.get(1));
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
                            if(frame.startsWith("ACCEPT|FRENZY"))frenzyStarts.incrementAndGet();
                            if(frame.startsWith("ACCEPT|FATTACK")||frame.startsWith("ACCEPT|GHOST|STRIKE"))
                                effectiveAttacks.incrementAndGet();
                            if(frame.startsWith("ACCEPT|SCAN"))goodScans.incrementAndGet();
                            if(frame.startsWith("ACCEPT|IDENTIFY"))correctResponses.incrementAndGet();
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
    /** Read ONLY ordinary casefile measurements; never know current ghost species. */
    private void observeCasefile(LanEvidenceJournal.Entry item){
        if(item==null)return;
        String key=item.tick()+"/"+item.seat()+"/"+item.tool();
        if(!distinctSamples.add(key))return;
        observedSignals++;
        String reading=item.reading();
        if(item.tool().equals("EMF")){
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("EMF ([0-9]+)/10").matcher(reading);
            if(m.find()&&Integer.parseInt(m.group(1))>=8){
                int count=emfStrongRooms.merge(item.room(),1,Integer::sum);
                if(count>=2){
                    suspectedAnchor=item.room(); // inferred from repeat instrument readings
                    markSeen(GhostFieldGuide.Clue.EMF_OVER_SIX,item.tick());
                }
            }
        }else if(item.tool().equals("THERMOMETER")&&item.room()<HouseMap.INDOOR_ROOMS){
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("(-?[0-9]+(?:\\.[0-9]+)?)\\s*°?C").matcher(reading);
            if(m.find()){
                double c=Double.parseDouble(m.group(1));
                if(c<0&&coldRooms.merge(item.room(),1,Integer::sum)>=2){
                    suspectedAnchor=item.room();
                    markSeen(GhostFieldGuide.Clue.EXTREME_COLD,item.tick());
                }else if(c>=32.3&&hotRooms.merge(item.room(),1,Integer::sum)>=2){
                    suspectedAnchor=item.room();
                    markSeen(GhostFieldGuide.Clue.EXTREME_HEAT,item.tick());
                }
            }
        }else if(item.tool().equals("SPIRIT_BOX")&&reading.contains("Voice response detected"))
            markSeen(GhostFieldGuide.Clue.SPIRIT_REPLY,item.tick());
    }
    private void markSeen(GhostFieldGuide.Clue clue,int sampleTick){
        if(notes.status(clue)!=FieldJournal.Status.SEEN){
            notes.set(clue,FieldJournal.Status.SEEN,ghost?0:Integer.parseInt(seat.substring(1))-1,
                Math.max(0,sampleTick));
            dashboard.put(seat,"EVIDENCE "+clue+" / "+GhostFieldGuide.shortlist(notes).size()+" candidates");
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
        if(room<0)return;
        if(room<HouseMap.EXTRACTION_ROOM&&tick-lastScanTick>=75+random.nextInt(20)){
            lastScanTick=tick;
            LanCase.Tool tool=LanCase.Tool.values()[Math.floorMod((tick/140)+Integer.parseInt(seat.substring(1)),LanCase.Tool.values().length)];
            sendAction("SCAN "+tool+" R"+room,()->client.scan(tool));
            seenScanCount++;
        }
        if(health<=0)return;
        // One designated investigator makes ONE evidence-based deduction.
        // Never use rejection responses as a brute-force species oracle.
        if(seat.equals("H1")&&!guessAttempted&&tick>=300&&notes.seenCount()>=2){
            var possibilities=GhostFieldGuide.ordered(notes);
            boolean realDiagnostic=notes.status(GhostFieldGuide.Clue.EMF_OVER_SIX)==FieldJournal.Status.SEEN
                ||notes.status(GhostFieldGuide.Clue.EXTREME_HEAT)==FieldJournal.Status.SEEN
                ||notes.status(GhostFieldGuide.Clue.EXTREME_COLD)==FieldJournal.Status.SEEN
                ||notes.status(GhostFieldGuide.Clue.GHOST_WRITING)==FieldJournal.Status.SEEN;
            if(!possibilities.isEmpty()&&realDiagnostic){
                // Without unmistakable Ancient/Elder clues, favor the known
                // base species over an otherwise indistinguishable variant.
                GhostIdentity.Type candidate=possibilities.stream()
                    .filter(t->!t.ancient()&&!t.elder()).findFirst()
                    .orElse(possibilities.get(0));
                guessAttempted=true;
                lastIdentifyRequest=tick;
                identifications.incrementAndGet();
                sendAction("IDENTIFY "+candidate+" from "+notes.seenCount()+" clues",
                    ()->client.identify(candidate));
                return;
            }
        }
        if(hypothesis!=null){
            var rite=RitualSites.site(map,hypothesis.rite());
            // Host anchor never enters this client. A ward is only attempted at
            // a room inferred from real, independently received instrument clues.
            if(room==suspectedAnchor&&tick-lastWardRequest>150){
                lastWardRequest=tick;
                sendAction("WARD source",client::ward);
                return;
            }
            if(room==rite.room()&&rite.near(new LanWorld.Body(x,y,z,0,room))
                    &&tick-lastRiteRequest>90){
                lastRiteRequest=tick;
                rituals.incrementAndGet();
                switch(hypothesis.rite()){
                    case BANISHMENT -> sendAction("BANISH",client::banish);
                    case CLEANSING -> sendAction("CLEANSE",client::cleanse);
                    case ABSOLUTION -> sendAction("ABSOLVE",client::absolve);
                }
                return;
            }
            if(room!=rite.room())targetRoom=routeToward(room,rite.room());
        }
        if(room!=lastActionRoom){
            lastActionRoom=room;
            if(hypothesis==null)targetRoom=-1;
            blocked=0;
        }
        if(hypothesis!=null&&room!=RitualSites.site(map,hypothesis.rite()).room())
            targetRoom=routeToward(room,RitualSites.site(map,hypothesis.rite()).room());
        else if(targetRoom<0||targetRoom==room||!map.adjacentRooms(room).contains(targetRoom))
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
            sendAction("DOOR to R"+targetRoom,client::door);
            return;
        }
        double gx,gz;
        // Use the actual authored threshold and opposite-room center; never
        // teleport or ask the host where the ghost/other bots are.
        if(room==HouseMap.EXTRACTION_ROOM&&targetRoom==13){
            // All six hunters spawn at different offsets inside the REAL van.
            // First converge on its physical centre aisle, then walk down the
            // lowered cargo ramp. Never aim straight through a side wall.
            var local=map.van().local(x,z);
            var waypoint=map.van().world(0,
                local.z()>VanStaging.FRONT+.68?VanStaging.FRONT+.67:VanStaging.FRONT-.70);
            gx=waypoint.x();gz=waypoint.z();
        }else if(dist>0.75){
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
        // Use the same authored collision geometry as an actual hunter but
        // only for planning (the authoritative host still validates STEP).
        // A repeated blocked input must NOT be our default navigation behavior.
        double heading=Math.atan2(vx,vz);
        double[] offsets=blocked>3
            ?new double[]{.7,-.7,1.15,-1.15,1.6,-1.6,2.2,-2.2,0,.34,-.34}
            :new double[]{0,.32,-.32,.68,-.68,1.1,-1.1,1.58,-1.58};
        double chooseX=Double.NaN,chooseZ=Double.NaN;
        var start=new LevelDefinition.Vec(x,y,z);
        for(double offset:offsets){
            double angle=heading+offset,nx=Math.sin(angle),nz=Math.cos(angle);
            double moveX=x+nx*.31,moveZ=z+nz*.31;
            if(map.resolveMove(start,moveX,moveZ)==null
                ||map.resolveMove(start,moveX,z)==null
                ||map.resolveMove(start,x,moveZ)==null)continue;
            chooseX=nx;chooseZ=nz;break;
        }
        if(!Double.isFinite(chooseX)){
            blocked++;
            if(blocked%16==0)targetRoom=-1;
            dashboard.put(seat,"NO VALID WALK STEP R"+room+" -> R"+targetRoom);
            return;
        }
        int dx=(int)Math.round(100*chooseX),dz=(int)Math.round(100*chooseZ);
        sendAction("EXPLORE R"+room+" -> R"+targetRoom,()->client.step(dx,dz));
        lastX=x;lastZ=z;
    }
    private int routeToward(int from,int goal){
        if(from==goal)return from;
        int[] parent=new int[HouseMap.ROOM_COUNT];
        java.util.Arrays.fill(parent,-1);
        java.util.ArrayDeque<Integer> q=new java.util.ArrayDeque<>();
        q.add(from);parent[from]=from;
        while(!q.isEmpty()&&parent[goal]<0){
            int at=q.remove();
            for(int next:map.adjacentRooms(at))
                if(parent[next]<0){parent[next]=at;q.add(next);}
        }
        if(parent[goal]<0)return -1;
        int next=goal;
        while(parent[next]!=from)next=parent[next];
        return next;
    }
    private int chooseNextRoom(){
        List<Integer> choices=new ArrayList<>(map.adjacentRooms(room));
        choices.removeIf(r->r==HouseMap.EXTRACTION_ROOM||r==room);
        if(choices.isEmpty())return -1;
        choices.sort(Comparator.comparingInt((Integer r)->visited.getOrDefault(r,0))
              .thenComparingInt(r->Math.floorMod(r*11+Integer.parseInt(seat.substring(1))*7,17)));
        return choices.get(Math.min(choices.size()-1,random.nextInt(Math.min(2,choices.size()))));
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
                                double requested=switch(heard.direction()){
                                    case AHEAD,HERE -> ghostHeading;
                                    case LEFT -> ghostHeading+90;
                                    case RIGHT -> ghostHeading-90;
                                    case BEHIND -> ghostHeading+180;
                                };
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
        // Orient toward a genuine sound bearing, without any hunter position.
        int direction=Math.floorMod(tick/12+3,4);
        int[][] steps={{90,0},{0,90},{-90,0},{0,-90}};
        if(heard!=null&&tick-heard.tick()<30){
            switch(heard.direction()){
                case LEFT -> direction=2;
                case RIGHT -> direction=0;
                case AHEAD,HERE -> direction=1;
                case BEHIND -> direction=3;
            }
        }
        int[] v=steps[direction];
        sendAction("GSTEP "+v[0]+","+v[1],
            ()->client.ghostStep(v[0],v[1]));
    }
    @Override public void close(){
        closed=true;
        executor.shutdownNow();
    }
}
