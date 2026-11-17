package org.beckenburg;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Bounded, host-side sound tracking for an AI ghost. Does not accept private hunter bodies,
 * notebook entries, health, or a continuously updated hunter transform. The same GhostDomain
 * dispatcher and LanGhostSpatial step method remain the only ways to move/affect the world.
 */
public final class LanGhostPursuit {
    public enum Sound { FOOTSTEP(1, 25), EMOTE(1, 25), DOOR(4, 55),
        EQUIPMENT(4, 55), CHAT(3, 45), VOICE(3, 45), RITUAL(8, 85);
        final int budget, lifetime;
        Sound(int budget, int lifetime) { this.budget=budget;this.lifetime=lifetime; }
    }
    public record Echo(int room, double approximateX, double approximateZ, int heardTick,
                       int expiryTick, Sound source) { }
    public record Impulse(int dx, int dz) { }
    /** Short search at the last audible location, with no future body updates. */
    public record Search(int room, double approximateX, double approximateZ, int startedTick, int expiryTick) { }
    private final HouseMap map;
    private final GhostIdentity.Type archetype;
    private final Random random;
    private final java.util.SplittableRandom signatureRolls;
    private int signatureClock;
    private double signatureWait;
    private final org.beckenburg.editor.ai.NpcAiTelemetry diagnostics;
    private Echo memory;
    private int memoryStrength=-1;
    private Search search;
    private long sequence;
    private final int[] visitedAt=new int[HouseMap.ROOM_COUNT];
    private int patrolRoom=-1,nextPatrolTick,walkUntil,restUntil;
    private double patrolX,patrolZ;
    private int eventFrequency=100;
    public void configureFrequency(int percent){
        if(percent<50||percent>200)throw new IllegalArgumentException("invalid event rate");
        eventFrequency=percent;
    }

    public LanGhostPursuit(HouseMap map,long seed){this(map,seed,GhostIdentity.Type.SPIRIT);}
    public LanGhostPursuit(HouseMap map,long seed,GhostIdentity.Type type) {
        this(map,seed,type,new org.beckenburg.editor.ai.NpcAiTelemetry());
    }
    /** The host supplies per-session opt-in telemetry; no global shared counters. */
    public LanGhostPursuit(HouseMap map,long seed,GhostIdentity.Type type,
            org.beckenburg.editor.ai.NpcAiTelemetry diagnostics) {
        this.map=Objects.requireNonNull(map);
        this.archetype=Objects.requireNonNull(type);
        this.diagnostics=Objects.requireNonNull(diagnostics);
        random=new Random(seed);
        signatureRolls=new java.util.SplittableRandom(seed^0x5349474e41545552L);
        resetSignature(0);
        java.util.Arrays.fill(visitedAt,-1);
    }
    /** World-authorized events only. Coarse 2m cells intentionally discard exact positions. */
    public void heard(int ghostRoom, int sourceRoom, double x, double z, Sound kind, int tick) {
        heard(ghostRoom,sourceRoom,x,z,kind,tick,GhostSoundProfile.forSound(kind));
    }
    public void heard(int ghostRoom, int sourceRoom, double x, double z, Sound kind, int tick,
                      GhostSoundProfile.Profile profile) {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(profile);
        if(ghostRoom<0||ghostRoom>=HouseMap.EXTRACTION_ROOM
                ||sourceRoom<0||sourceRoom>=HouseMap.EXTRACTION_ROOM
                ||!Double.isFinite(x)||!Double.isFinite(z)||tick<0
                ||map.roomAt(x,map.groundHeight(x,z,sourceRoom),z)!=sourceRoom)return;
        long hearingStart=diagnostics.begin();
        int attenuation;
        try{attenuation=LanAcoustics.cost(map,ghostRoom,sourceRoom);}
        finally{diagnostics.end(org.beckenburg.editor.ai.NpcAiTelemetry.Metric.HEARING,hearingStart);}
        rememberSound(sourceRoom,x,z,kind,tick,attenuation,profile);
    }
    /** The live host provides its authoritative spectral pose, not a
     * client-supplied location. Child-room doors now affect AI hearing even
     * when both actors remain inside the same legacy gameplay zone. */
    public void heard(LanGhostSpatial.Pose ghost,int sourceRoom,
                      double x,double sourceY,double z,Sound kind,int tick){
        heard(ghost,sourceRoom,x,sourceY,z,kind,tick,GhostSoundProfile.forSound(kind));
    }
    public void heard(LanGhostSpatial.Pose ghost,int sourceRoom,
                      double x,double sourceY,double z,Sound kind,int tick,
                      GhostSoundProfile.Profile profile){
        Objects.requireNonNull(kind);
        Objects.requireNonNull(profile);
        if(ghost==null||ghost.room()<0||ghost.room()>=HouseMap.EXTRACTION_ROOM||
            sourceRoom<0||sourceRoom>=HouseMap.EXTRACTION_ROOM||
            !Double.isFinite(ghost.x()+ghost.y()+ghost.z()+x+sourceY+z)||
            tick<0||map.roomAt(ghost.x(),ghost.y(),ghost.z())!=ghost.room()||
            map.roomAt(x,sourceY,z)!=sourceRoom)return;
        long hearingStart=diagnostics.begin();
        int attenuation;
        try{
            attenuation=LanAcoustics.costAt(map,ghost.room(),
                new LevelDefinition.Vec(ghost.x(),ghost.y(),ghost.z()),
                sourceRoom,new LevelDefinition.Vec(x,sourceY,z));
        }finally{diagnostics.end(org.beckenburg.editor.ai.NpcAiTelemetry.Metric.HEARING,hearingStart);}
        rememberSound(sourceRoom,x,z,kind,tick,attenuation,profile);
    }
    private void rememberSound(int sourceRoom,double x,double z,
                               Sound kind,int tick,int attenuation,
                               GhostSoundProfile.Profile profile){
        if(attenuation>GhostSoundProfile.acousticBudget(kind,profile))return;
        // Text length controls acoustic memory; one event never subscribes
        // the ghost to subsequent hunter positions or transmitted words.
        int lifetime=(kind==Sound.CHAT
            ?Math.min(70,profile.durationTicks()+14):kind.lifetime)-attenuation*5;
        if(lifetime<=0)return;
        // Tile-centred estimate, NEVER a hunter-body position subscription.
        double cx=2*Math.floor(x/2)+1,cz=2*Math.floor(z/2)+1;
        Echo candidate=new Echo(sourceRoom,cx,cz,tick,tick+lifetime,kind);
        int incoming=GhostSoundProfile.perceived(profile,attenuation);
        if(memory==null||tick>=memory.expiryTick()
                ||GhostSoundProfile.supersedes(incoming,memoryStrength,
                    Math.max(0,tick-memory.heardTick()))){
            memory=candidate;memoryStrength=incoming;search=null;
        }
    }
    /** Time-limited immutable ghost observation, not a feed of all hunter locations. */
    public Echo memory(int tick) {
        if(memory!=null && tick>=memory.expiryTick()) {
            // Searching uses the expired clue, not a live position subscription.
            Echo ended=memory;
            memory=null;memoryStrength=-1;
            search=new Search(ended.room(),ended.approximateX(),ended.approximateZ(),
                    ended.expiryTick(),ended.expiryTick()+24);
        }
        if(search!=null && tick>=search.expiryTick())search=null;
        return memory;
    }
    public Search search(int tick) { memory(tick);return search; }
    public void forget(){memory=null;memoryStrength=-1;search=null;}

    // An exponentially distributed opportunity can occur early. It is held
    // until actually spent, so an empty room cannot consume the species tell.
    boolean signatureAvailable(int tick){
        // A long possession by a human controller can skip AI decisions. Account
        // for the elapsed early minutes at their own rates on controller handover.
        while(signatureClock<tick){
            int through=tick;
            if(signatureClock<GhostPacing.FULL_PRESSURE_TICK)
                through=Math.min(tick,(signatureClock/GhostPacing.TICKS_PER_MINUTE+1)*GhostPacing.TICKS_PER_MINUTE);
            signatureWait-=(through-signatureClock)*(eventFrequency/100.0)/GhostPacing.signatureMeanTicks(through);
            signatureClock=through;
        }
        return signatureWait<=0;
    }
    void resetSignature(int tick){
        signatureWait=-Math.log1p(-signatureRolls.nextDouble());
        signatureClock=tick;
    }
    /** Choose room actions from legal host observations and a finite memory of audible events. */
    public GhostDomain.Command decide(GhostDomain.Observation seen, boolean hasSpatialVictim) {
        Objects.requireNonNull(seen);
        List<GhostDomain.Action> legal=seen.legalActions();
        GhostDomain.Action action=null;
        int target=seen.room();
        visitedAt[seen.room()]=seen.tick();
        boolean locallyHeard=memory(seen.tick())!=null && memory.room()==seen.room();
        boolean signatureDue=signatureAvailable(seen.tick());
        int reserve=signatureDue?GhostDomain.cost(archetype,GhostDomain.Action.SIGNATURE)
            +GhostPacing.signaturePremium(seen.tick()):GhostFrenzy.profile(archetype).energyCost();
        boolean saving=seen.energy()<reserve;
        // Isolation feeders cannot recharge beside a pursuing team. Leave a
        // recently audible room on a paid exit and pause before chasing again.
        if(saving&&GhostTraits.of(archetype).charge()==GhostTraits.Charge.ISOLATION){
            if(locallyHeard&&legal.contains(GhostDomain.Action.DRIFT)&&!seen.exits().isEmpty()){
                int retreat=leastVisited(seen.exits());
                restUntil=seen.tick()+220;
                return new GhostDomain.Command(GhostDomain.Action.DRIFT,retreat,++sequence,GhostDomain.Source.AI);
            }
            if(seen.tick()<restUntil&&!locallyHeard)return null;
        }
        if(signatureDue&&locallyHeard&&seen.hearsHunter()&&legal.contains(GhostDomain.Action.SIGNATURE))
            action=GhostDomain.Action.SIGNATURE;
        else if(locallyHeard && seen.hearsHunter() && hasSpatialVictim && legal.contains(GhostDomain.Action.STRIKE))
            action=GhostDomain.Action.STRIKE;
        else if(locallyHeard && seen.hearsHunter() && (!saving||hasSpatialVictim&&!signatureDue)
                && legal.contains(GhostDomain.Action.MANIFEST))
            action=GhostDomain.Action.MANIFEST;
        else if(legal.contains(GhostDomain.Action.DRIFT) && memory(seen.tick())!=null
                && memory.room()!=seen.room()) {
            int next=route(seen.room(),memory.room());
            if(next>=0 && seen.exits().contains(next)){
                action=GhostDomain.Action.DRIFT;target=next;
            }
        }
        // A fading sound buys only a brief local sweep. No post-expiry STRIKE or MANIFEST.
        Search sweep=search(seen.tick());
        if(action==null && sweep!=null && seen.room()!=sweep.room()
                && legal.contains(GhostDomain.Action.DRIFT)) {
            int next=route(seen.room(),sweep.room());
            if(next>=0 && seen.exits().contains(next)){action=GhostDomain.Action.DRIFT;target=next;}
        }
        // Species bias uses only filtered host observations; never private hunter positions.
        GhostBehavior.Profile bias=GhostBehavior.profile(archetype);
        // A species-specific attack preference is NOT a target acquisition.
        // Its STRIKE choice must pass the same host-owned spatial-victim gate
        // as the ordinary pursuit route. Hearing alone never grants contact.
        if(action==null && !saving && bias.favored()!=GhostDomain.Action.DRIFT
                && (bias.favored()!=GhostDomain.Action.STRIKE||hasSpatialVictim&&locallyHeard)
                && seen.tick()%GhostPacing.minorPeriod(bias.aiPeriod(),seen.tick(),eventFrequency)==0
                && legal.contains(bias.favored()))
            action=bias.favored();
        if(action==null && !saving && seen.tick()%GhostPacing.minorPeriod(29,seen.tick(),eventFrequency)==0 && legal.contains(GhostDomain.Action.WHISPER))
            action=GhostDomain.Action.WHISPER;
        if(action==null && !saving && seen.tick()%GhostPacing.minorPeriod(41,seen.tick(),eventFrequency)==0 && legal.contains(GhostDomain.Action.DISTURB))
            action=GhostDomain.Action.DISTURB;
        // With no sound, patrol occasionally rather than knowing where the team is.
        if(action==null && seen.tick()%((bias.favored()==GhostDomain.Action.DRIFT?bias.aiPeriod():35)*(saving?3:1))==0
                && legal.contains(GhostDomain.Action.DRIFT)
                && !seen.exits().isEmpty() && memory(seen.tick())==null && search(seen.tick())==null){
            action=GhostDomain.Action.DRIFT;
            // Cover unexplored rooms before repeating the same two-door loop.
            target=leastVisited(seen.exits());
        }
        // Future preference routes must never bypass this invariant, either.
        if(action==GhostDomain.Action.STRIKE&&!hasSpatialVictim)return null;
        return action==null?null:new GhostDomain.Command(action,target,++sequence,GhostDomain.Source.AI);
    }
    private int leastVisited(List<Integer> exits){
        int oldest=Integer.MAX_VALUE,ties=0,target=exits.get(0);
        for(int exit:exits){
            int last=visitedAt[exit];
            if(last<oldest){oldest=last;target=exit;ties=1;}
            else if(last==oldest&&random.nextInt(++ties)==0)target=exit;
        }
        return target;
    }
    /** Move toward an old approximate sound in the current room; the host applies this using
     * LanGhostSpatial.step, which enforces exactly the same walls and speed as a human ghost.
     */
    public Impulse approach(GhostDomain.Observation seen, LanGhostSpatial.Pose pose) {
        if(seen==null||pose==null||seen.room()!=pose.room())return null;
        Echo e=memory(seen.tick());
        double tx,tz;
        if(e!=null && e.room()==pose.room()) {tx=e.approximateX();tz=e.approximateZ();}
        else {
            Search sweep=search(seen.tick());
            if(sweep==null||sweep.room()!=pose.room())return null;
            int phase=((seen.tick()-sweep.startedTick())/6)%4;
            // Search nearby positions, using only the stale 2m acoustic cell.
            double[] ox={1.0,0,-1.0,0}, oz={0,1.0,0,-1.0};
            tx=sweep.approximateX()+ox[phase];tz=sweep.approximateZ()+oz[phase];
        }
        double dx=tx-pose.x(), dz=tz-pose.z();
        if(Math.hypot(dx,dz)<.55)return null;
        // A direction, not a teleport or arbitrary speed.
        return new Impulse((int)Math.round(Math.max(-100,Math.min(100,dx*100))),
                (int)Math.round(Math.max(-100,Math.min(100,dz*100))));
    }
    /** Quiet patrol alternates short physical walks and stationary listening windows.
     * Only the authored room, collision and safe-area volumes inform a waypoint. */
    public Impulse patrol(GhostDomain.Observation seen,LanGhostSpatial.Pose pose){
        if(seen.room()!=pose.room())return null;
        if(patrolRoom!=pose.room()||seen.tick()>=nextPatrolTick){
            patrolRoom=pose.room();
            walkUntil=seen.tick()+8;
            nextPatrolTick=walkUntil+12+random.nextInt(8);
            patrolX=pose.x();patrolZ=pose.z();
            double bearing=random.nextDouble()*2*Math.PI;
            for(int attempt=0;attempt<8;attempt++,bearing+=Math.PI/4){
                double x=pose.x()+2*Math.cos(bearing),z=pose.z()+2*Math.sin(bearing);
                var to=map.resolveMove(new LevelDefinition.Vec(pose.x(),pose.y(),pose.z()),x,z);
                if(to==null||Math.hypot(to.x()-x,to.z()-z)>.3
                        ||map.roomAt(to.x(),to.y(),to.z())!=pose.room()
                        ||LanGhostSpatial.hidingPocketAt(map,pose.room(),to.x(),to.y(),to.z())
                        ||SafeAreaBarrier.protectedAt(map,to.x(),to.y(),to.z()))continue;
                patrolX=to.x();patrolZ=to.z();break;
            }
        }
        double dx=patrolX-pose.x(),dz=patrolZ-pose.z(),distance=Math.hypot(dx,dz);
        if(seen.tick()>=walkUntil||distance<.4)return null;
        return new Impulse((int)Math.round(100*dx/distance),(int)Math.round(100*dz/distance));
    }
    private int route(int from,int to){
        long started=diagnostics.begin();
        try{return calculateRoute(from,to);}
        finally{diagnostics.end(org.beckenburg.editor.ai.NpcAiTelemetry.Metric.ROUTE,started);}
    }
    private int calculateRoute(int from,int to){
        if(from==to)return from;
        boolean[] seen=new boolean[HouseMap.ROOM_COUNT];
        ArrayDeque<int[]> todo=new ArrayDeque<>();todo.add(new int[]{from,-1});seen[from]=true;
        while(!todo.isEmpty()){
            int[] at=todo.removeFirst();
            for(int next:map.ghostExits(at[0]))if(!seen[next]){
                int first=at[1]<0?next:at[1];
                if(next==to)return first;
                seen[next]=true;todo.addLast(new int[]{next,first});
            }
        }
        return -1;
    }
}
