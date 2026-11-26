package org.beckenburg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Single host-style rule authority for ghost actions. AI and human controllers submit identical
 * Command records. Source is attribution ONLY: it must never change costs or outcomes.
 * No networking or remote command authentication is implied by this local implementation.
 */
public final class GhostDomain {
    public enum Action { DRIFT, WHISPER, DISTURB, MANIFEST, STRIKE, SIGNATURE }
    public enum Source { AI, HUMAN }
    public record Command(Action action, int targetRoom, long sequence, Source source) {
        public Command {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(source, "source");
        }
    }
    /** No hunter coordinates, private journal, role, health or inventory may leak to a controller. */
    public record Observation(int tick, int room, int anchorRoom, int energy,
            boolean manifested, boolean hearsHunter, List<Integer> exits, List<Action> legalActions) {
        public Observation {
            exits = List.copyOf(exits);
            legalActions = List.copyOf(legalActions);
        }
    }
    public record Resolution(boolean applied, String reason, Action action, int room, int energy,
            int fear, int harm, boolean manifested) { }

    private static final int[] COST = { 2, 6, 8, 18, 30, 55 };
    private static final int[] COOLDOWN = { 35, 22, 30, 78, 90, 180 };
    static final int MAX_AWAY_TICKS=180,HOME_DWELL_TICKS=80;
    private int departedTick=-1,stayHomeUntil,routeTick=-1,routeFrom=-1,routeStep=-1;
    private final int anchorRoom;
    private final HouseMap map;
    private final GhostIdentity.Type archetype;
    private final int[] lastActionTick = new int[Action.values().length];
    private int room, tick, energy = 100, manifestUntil, occupants;
    private int energyRecovery=100,energyRemainder;
    private int gained,homeGained,ordinarySpent,frenzySpent,repelled;
    private final int[] uses=new int[Action.values().length];
    int gained(){return gained;}
    int homeGained(){return homeGained;}
    int ordinarySpent(){return ordinarySpent;}
    int frenzySpent(){return frenzySpent;}
    int repelled(){return repelled;}
    java.util.Map<Action,Integer> actionCounts(){
        var result=new java.util.EnumMap<Action,Integer>(Action.class);
        for(var action:Action.values())result.put(action,uses[action.ordinal()]);
        return java.util.Map.copyOf(result);
    }
    private void pay(Action action,int amount){
        energy-=amount;ordinarySpent+=amount;uses[action.ordinal()]++;
    }
    public void configureRecovery(int rate){
        if(rate<50||rate>200)throw new IllegalArgumentException("invalid energy recovery");
        energyRecovery=rate;
    }
    private int suppressedUntil=-1,slowedUntil=-1;
    private boolean hearsHunter;
    private long lastSequence = -1;

    public GhostDomain(int anchorRoom) { this(new HouseMap(), anchorRoom, GhostIdentity.Type.SPIRIT); }
    public GhostDomain(HouseMap map, int anchorRoom) { this(map,anchorRoom,GhostIdentity.Type.SPIRIT); }
    public GhostDomain(HouseMap map,int anchorRoom,GhostIdentity.Type type) {
        this.map=Objects.requireNonNull(map, "map");
        this.archetype=Objects.requireNonNull(type,"type");
        if (anchorRoom < 0 || anchorRoom >= map.interiorRoomCount()) throw new IllegalArgumentException("invalid anchor room");
        this.anchorRoom = anchorRoom;
        this.room = anchorRoom;
        Arrays.fill(lastActionTick, -100000);
    }
    public int room() { return room; }
    public GhostIdentity.Type type(){return archetype;}
    public int energy() { return energy; }
    /** Host-only major action: reserves accumulated energy without reusing STRIKE. */
    public boolean spendFrenzyEnergy(int amount){
        if(amount<1||amount>100||tick<1||tick<suppressedUntil||energy<amount)return false;
        energy-=amount;frenzySpent+=amount;
        return true;
    }
    public boolean suppressed(){return tick<suppressedUntil;}
    public int tickCount() { return tick; }
    public boolean manifested() { return tick > 0 && tick <= manifestUntil; }
    public void reveal(int duration){
        if(duration<1||duration>100)throw new IllegalArgumentException("invalid reveal duration");
        manifestUntil=Math.max(manifestUntil,tick+duration);
    }
    public void suppress(int duration){
        if(duration<1||duration>100)throw new IllegalArgumentException("invalid suppression duration");
        suppressedUntil=Math.max(suppressedUntil,tick+duration);
    }
    public void slowFor(int duration){
        if(duration<1||duration>100)throw new IllegalArgumentException("invalid slow duration");
        slowedUntil=Math.max(slowedUntil,tick+duration);
    }
    public void repel(int duration){
        suppress(duration);reveal(Math.min(duration,18));
        int lost=Math.min(12,energy);
        energy-=lost;repelled+=lost;
    }

    /** Called exactly once each 100ms simulation step, independent of renderer FPS. */
    public void advance(int nextTick, int hunterRoom) {
        advance(nextTick,hunterRoom,hunterRoom==room?1:0);
    }
    /** Room occupancy controls energy. Controllers never receive the actual occupant count. */
    public void advance(int nextTick, int hunterRoom, int occupants) {
        if (nextTick != tick + 1) throw new IllegalArgumentException("ghost tick must advance exactly once");
        if(occupants<0||occupants>6)throw new IllegalArgumentException("invalid occupancy");
        tick=nextTick;this.occupants=occupants;
        hearsHunter = hunterRoom >= 0 && hunterRoom == room;
        energyRemainder+=GhostTraits.gained(archetype,tick,occupants)*energyRecovery
            *GhostPacing.rechargePercent(tick);
        int admitted=Math.min(100-energy,energyRemainder/10000);
        energy+=admitted;gained+=admitted;
        energyRemainder%=10000;
        // Every species can muster a single drift fare even if investigators
        // camp its home or completely leave its feeding range. This minimum
        // buys travel only and cannot fund an offensive power.
        int driftCost=cost(Action.DRIFT);
        if(energy<driftCost&&tick%10==0){
            energy++;gained++;
            if(returnDue())homeGained++;
        }
    }
    private boolean returnDue(){
        return departedTick>=0&&tick-departedTick>=MAX_AWAY_TICKS;
    }
    private int nextHomeExit(){
        if(routeTick!=tick||routeFrom!=room){
            var route=GhostAiNavigationAudit.path(map,room,anchorRoom);
            routeStep=route.size()>1?route.get(1):-1;
            routeTick=tick;routeFrom=room;
        }
        return routeStep;
    }
    /** A temporarily warded destination, low energy or salt delays travel;
     * the ordinary dispatcher revalidates the exact exit before committing. */
    int homewardStep(){
        if(!returnDue()||tick<slowedUntil||!ready(Action.DRIFT))return -1;
        return nextHomeExit();
    }
    private List<Integer> exits(int current) {
        if(tick<stayHomeUntil)return List.of();
        if(returnDue()&&ready(Action.DRIFT)){
            int next=nextHomeExit();
            return next>=0&&map.ghostExits(current).contains(next)?List.of(next):List.of();
        }
        return map.ghostExits(current);
    }
    private boolean ready(Action action){
        return energy>=cost(action)&&cooldownRemaining(action)==0;
    }
    static int cost(GhostIdentity.Type type,Action action){return GhostBehavior.cost(type,action,COST[action.ordinal()]);}
    int cost(Action action){
        return cost(archetype,action)+(action==Action.SIGNATURE?GhostPacing.signaturePremium(tick):0);
    }
    int lastUsed(Action action){return lastActionTick[action.ordinal()];}
    int cooldownRemaining(Action action){
        int duration=GhostBehavior.cooldown(archetype,action,COOLDOWN[action.ordinal()]);
        if(action==Action.STRIKE)duration=GhostPacing.strikeCooldown(duration,lastUsed(action));
        return Math.max(0,lastUsed(action)+duration-tick);
    }
    private boolean valid(Action a, int target) {
        if(!ready(a))return false;
        if(a!=Action.DRIFT && tick<suppressedUntil)return false;
        if(a==Action.DRIFT)return tick>=slowedUntil && exits(room).contains(target);
        if (target != room) return false;
        return switch (a) {
            case MANIFEST -> tick >= 80 && (hearsHunter||unattendedManifestation()) && !manifested();
            case STRIKE -> tick > 160 && hearsHunter && manifested()
                &&(!GhostTraits.of(archetype).huntsSolitary()||occupants==1);
            case SIGNATURE -> tick>130 && (unattendedManifestation()
                    ||hearsHunter&&(!GhostTraits.of(archetype).huntsSolitary()||occupants==1));
            case WHISPER, DISTURB -> true;
            case DRIFT -> throw new AssertionError();
        };
    }
    private boolean unattendedManifestation(){
        return occupants==0&&GhostTraits.witnesses(archetype)==GhostTraits.Witnesses.EMPTY;
    }

    /** Only the host calls this; clients cannot provide their own observations. */
    public Observation observe() {
        var availableExits = exits(room);
        if (!map.temporarySafeAreas().isEmpty())
            availableExits = availableExits.stream().filter(to -> SafeAreaBarrier.ghostSpawn(map,to)!=null).toList();
        List<Action> allowed = new ArrayList<>();
        for (Action action : Action.values()) {
            if (action == Action.DRIFT ? availableExits.stream().anyMatch(to -> valid(action, to)) : valid(action, room)) {
                allowed.add(action);
            }
        }
        return new Observation(tick, room, anchorRoom, energy, manifested(), hearsHunter,
                availableExits, allowed);
    }
    public Resolution dispatch(Command command) {
        Objects.requireNonNull(command, "command");
        // Consume rejected sequence numbers too: a retried forged request must not be reusable.
        if (command.sequence() <= lastSequence || command.sequence() < 0)
            return rejected(command, "duplicate or stale command");
        lastSequence = command.sequence();
        if (!valid(command.action(), command.targetRoom()))
            return rejected(command, "insufficient energy, cooldown or invalid target/state");
        Action a = command.action();
        if (a==Action.DRIFT && SafeAreaBarrier.ghostSpawn(map,command.targetRoom())==null)
            return rejected(command, "destination has no unprotected footing");
        pay(a,cost(a));
        lastActionTick[a.ordinal()] = tick;
        int fear = 0, harm = 0;
        switch (a) {
            case DRIFT -> {
                if(room==anchorRoom)departedTick=tick;
                room=command.targetRoom();
                if(room==anchorRoom){
                    departedTick=-1;
                    stayHomeUntil=tick+HOME_DWELL_TICKS;
                }
            }
            case WHISPER -> fear = hearsHunter ? 2 : 0;
            // Object injuries are resolved by actual armed poltergeist projectile contacts.
            case DISTURB -> fear = hearsHunter ? 3 : 0;
            case MANIFEST -> {manifestUntil = tick + 49 + GhostBehavior.profile(archetype).manifestTicks();
                fear=hearsHunter?5:0;}
            case STRIKE -> { harm = 12; fear = 6; manifestUntil = tick + 9; }
            case SIGNATURE -> {
                // Species-specific modifiers apply below. This is an expensive room-scale event,
                // not a free kill and not a substitute for completing the assigned rite.
                fear=hearsHunter?14:0;
                harm=hearsHunter?6:0;
                manifestUntil=Math.max(manifestUntil,tick+18);
            }
        }
        fear=GhostBehavior.fear(archetype,a,fear);
        harm=GhostBehavior.harm(archetype,a,harm);
        fear=GhostTraits.fear(archetype,fear);
        harm=GhostTraits.harm(archetype,harm);
        return new Resolution(true, "accepted", a, room, energy, fear, harm, manifested());
    }
    private long lastCompanionSequence=-1;
    /**
     * The other Twin acts from HER physical room, without moving the main ghost
     * identity/anchor or obtaining a second energy pool. These are nonlethal,
     * ordinary shared-cooldown actions, not a second strike executor.
     *
     * Used only for the host's genuine TwinCompanionAi body. The caller supplies
     * the already-authoritative pose's room, never client-reported coordinates.
     */
    public Resolution dispatchCompanionMinor(Command command,boolean hearsCompanion) {
        Objects.requireNonNull(command,"command");
        Action action=command.action();
        if(archetype.baseType()!=GhostIdentity.Type.TWINS
                ||command.source()!=Source.AI
                ||(action!=Action.WHISPER&&action!=Action.DISTURB)
                ||command.targetRoom()<0||command.targetRoom()>=map.interiorRoomCount()
                ||command.sequence()<0||command.sequence()<=lastCompanionSequence)
            return rejected(command,"not a valid independent Twin minor action");
        lastCompanionSequence=command.sequence();
        int cost=cost(action);
        if(tick<1||tick<suppressedUntil||energy<cost
                ||cooldownRemaining(action)>0)
            return rejected(command,"shared energy, suppression or cooldown prevents companion action");
        pay(action,cost);
        lastActionTick[action.ordinal()]=tick;
        int fear=hearsCompanion?(action==Action.WHISPER?2:3):0;
        fear=GhostTraits.fear(archetype,GhostBehavior.fear(archetype,action,fear));
        return new Resolution(true,"accepted companion action",action,
            command.targetRoom(),energy,fear,0,manifested());
    }
    private Resolution rejected(Command command, String why) {
        return new Resolution(false, why, command.action(), room, energy, 0, 0, manifested());
    }

    /** A decision maker, not a second world mutation path. It sees only Observation. */
    public static final class AiController {
        private final Random rng;
        private long sequence;
        public AiController(long seed) { rng = new Random(seed); }
        public Command decide(Observation observation) {
            Objects.requireNonNull(observation, "observation");
            Action a = null;
            int target = observation.room();
            List<Action> legal = observation.legalActions();
            if (observation.hearsHunter() && legal.contains(Action.STRIKE)) a = Action.STRIKE;
            else if(observation.tick()%197==0
                    &&legal.contains(Action.SIGNATURE))a=Action.SIGNATURE;
            else if (legal.contains(Action.MANIFEST)) a = Action.MANIFEST;
            else if (observation.tick() % 29 == 0 && legal.contains(Action.WHISPER)) a = Action.WHISPER;
            else if (observation.tick() % 41 == 0 && legal.contains(Action.DISTURB)) a = Action.DISTURB;
            else if (observation.tick() % 35 == 0 && !observation.hearsHunter()
                    && legal.contains(Action.DRIFT) && !observation.exits().isEmpty()) {
                a = Action.DRIFT;
                target = observation.exits().get(rng.nextInt(observation.exits().size()));
            }
            return a == null ? null : new Command(a, target, ++sequence, Source.AI);
        }
    }
}
