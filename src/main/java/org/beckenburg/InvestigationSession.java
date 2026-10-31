package org.beckenburg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Deterministic solo contract rules. Networking and human ghost controls are NOT yet implemented. */
public final class InvestigationSession {
    public enum Role { CLERIC, OBSERVER, DEMONOLOGIST }
    public enum Instrument { EMF, THERMOMETER, SPIRIT_BOX }
    public enum Outcome { ACTIVE, CLEANSED, RETREATED, LOST }
    private final HouseMap map;
    private final Random random;
    private final Role role;
    private final HunterDiscipline classSkills=new HunterDiscipline(1);
    private CharacterVisuals.Hunter discipline(){return CharacterVisuals.Hunter.valueOf(role.name());}
    public HunterDiscipline.Result classAbility(HunterDiscipline.Ability ability,double x,double y,double z){
        if(outcome!=Outcome.ACTIVE||health<=0||!map.traversable(x,y,z))
            return new HunterDiscipline.Result(false,"Cannot use discipline now");
        HunterDiscipline.Result result=classSkills.activate(0,discipline(),ability,ticks);
        if(!result.accepted())return result;
        switch(ability){
            case COMFORT -> stress=Math.max(0,stress-20);
            case SURVEY -> {
                log("SURVEY / "+rawEmfAt(x,y,z)+" / "+rawTemperatureAt(x,y,z)
                    +" / field readings only, not verified evidence");
            }
            case INVOKE -> {
                stress=Math.min(100,stress+16);
                // An invocation risks the user, and can provoke a present ghost,
                // but never exposes a remote true anchor or its species.
                if(ghost.room()==map.roomAt(x,y,z))ghost.reveal(12);
            }
            default -> {}
        }
        log(ability.label+" / "+result.message());
        return result;
    }
    /** Companion benefits use the same effect sizes as human abilities. */
    public boolean companionSkill(HunterDiscipline.Ability ability,boolean nearPlayer,int allyRoom){
        if(outcome!=Outcome.ACTIVE||health<=0)return false;
        switch(ability){
            case COMFORT -> {if(nearPlayer)stress=Math.max(0,stress-20);}
            case HALLOW -> {if(nearPlayer)classSkills.shareHallow(0,ticks);}
            case SIGIL -> {if(nearPlayer)classSkills.shareSigil(0,ticks);}
            case INVOKE -> {
                // Only a supernatural presence actually near the embodied ally reacts.
                if(ghost.room()==allyRoom)ghost.reveal(12);
            }
            default -> {}
        }
        return true;
    }
    private final String missionId=UUID.randomUUID().toString();
    private final int anchorRoom;
    private final int[] spectralTrace=new int[HouseMap.ROOM_COUNT];
    private int saltRoom=-1,saltTick=-1;
    private boolean apparitionPhotographed;
    private final EnumSet<Instrument> measuredTools = EnumSet.noneOf(Instrument.class);
    private final java.util.Map<Instrument,String> lastReadings=new java.util.EnumMap<>(Instrument.class);
    public String deviceReading(String kind){try{return lastReadings.get(Instrument.valueOf(kind));}catch(IllegalArgumentException ignored){return null;}}
    private final ArrayList<String> journal = new ArrayList<>();
    /** Actual completed haunt events; these do not confer evidence. */
    public record WorldCue(int room,double x,double y,double z,LanSound.Cue cue,int tick){}
    private final java.util.ArrayDeque<WorldCue> pendingSounds=new java.util.ArrayDeque<>();
    public record HauntSound(int room,double x,double y,double z,String id,int beat,int tick){}
    private final java.util.ArrayDeque<HauntSound> stagedHauntSounds=
        new java.util.ArrayDeque<>();
    public List<HauntSound> drainHauntSounds(){
        List<HauntSound> out=List.copyOf(stagedHauntSounds);
        stagedHauntSounds.clear();return out;
    }
    public List<WorldCue> drainWorldSounds(){
        List<WorldCue> events=List.copyOf(pendingSounds);
        pendingSounds.clear();return events;
    }
    private Outcome outcome = Outcome.ACTIVE;
    private int ticks, health = 100, stress, wards, payouts;
    private final int hunterLevel, maxHealth;
    private final long evidenceSeed;
    private final EvidenceSignals.Polarity thermalPolarity;
    private final EvidenceSignals.Season season;
    private MissionWeather missionWeather;
    public void configureWeather(MissionWeather.Mode mode,EvidenceSignals.Weather selected,
            int intensity){
        if(ticks>0)throw new IllegalStateException("weather cannot be changed mid-session");
        missionWeather=new MissionWeather(season,mode,selected,intensity,evidenceSeed^0x57454154484552L);
    }
    public MissionWeather.State weather(){return missionWeather.state();}
    private int lastEvidenceTick=-10000,lastEvidenceRoom=-1,lastEvidenceStrength;
    private int lastVocalEventTick=-1,lastVocalEventRoom=-1,lastSpiritReplyTick=-1;
    private SpiritBoxResponses.Phrase latestSpiritReply;
    /** This is a presentation cue, not a confirmed evidence identity. */
    public LanSound.Cue spiritBoxReplyCue(){
        return latestSpiritReply==null?null:latestSpiritReply.cue;
    }
    private final GhostDomain ghost;
    private final GhostHauntingFolio hauntingFolio;
    private final java.util.ArrayDeque<GhostHauntingFolio.Occurrence> hauntings=
        new java.util.ArrayDeque<>();
    public java.util.List<GhostHauntingFolio.Occurrence> hauntings(){
        return java.util.List.copyOf(hauntings);
    }
    private int partySize=1;
    /** Party-wide challenge is host-owned and applied before deployment. */
    public void configureParty(int members){
        if(ticks>0)throw new IllegalStateException("Party is fixed before deployment");
        partySize=CampaignPartyRules.requireSize(members);
        ghost.configureRecovery(CampaignPartyRules.recoveryRate(members));
    }
    public int partySize(){return partySize;}
    private double clutterYaw,clutterPitch;
    public void clutterLook(double yaw,double pitch){clutterYaw=yaw;clutterPitch=pitch;}
    public void clutterAction(String action,int id,double x,double y,double z){
        if(outcome!=Outcome.ACTIVE)throw new IllegalArgumentException("case is not active");
        map.clutter().action(action,id,new LooseClutter.Observer(0,x,y,z,clutterYaw,clutterPitch,!hidden&&health>0,HunterPosture.eye(crouched)),ticks);
        log(map.clutter().hint(new LooseClutter.Observer(0,x,y,z,clutterYaw,clutterPitch,true,HunterPosture.eye(crouched))));
    }
    private RoomLighting roomLights;
    public void connectLighting(RoomLighting roomLights){this.roomLights=roomLights;}
    private final HunterCounters counters=new HunterCounters();
    private final UvEvidenceMarks uvEvidence=new UvEvidenceMarks();
    private final FieldEquipment uvHardware=new FieldEquipment();
    public boolean uvPowered(){return uvHardware.powered("UV_LIGHT");}
    public java.util.List<UvEvidenceMarks.Mark> uvVisible(double x,double y,double z,double yaw){
        return uvEvidence.visible(map,new LanWorld.Body(x,y,z,(int)Math.toDegrees(yaw),map.roomAt(x,y,z)),EquipmentTiers.Tier.I,ticks,uvPowered());
    }
    public void rechargeUvAtVan(double x,double y,double z){
        if(map.roomAt(x,y,z)!=HouseMap.EXTRACTION_ROOM)throw new IllegalArgumentException("UV service requires van rack");
        uvHardware.rechargeAtVan();
    }
    private final AmbientSanity ambientSanity=new AmbientSanity();
    private final GhostDomain.AiController ghostAi;
    private boolean prepared, haunted, rewarded;
    private int signatureAppearanceUntil=-1;
    private boolean hidden,shrubHidden,crouched;
    public boolean crouched(){return crouched;}
    public boolean posture(boolean low,double x,double y,double z){
        if(!HunterPosture.fits(map,new LevelDefinition.Vec(x,y,z),low)){log("Not enough headroom to stand.");return false;}
        crouched=low;if(!low&&shrubHidden)leaveHiding();return true;
    }
    private LevelDefinition.Vec hidingAt;
    public boolean hidden(){return hidden;}
    public void leaveHiding(){if(hidden){hidden=false;shrubHidden=false;hidingAt=null;log("You left concealment.");}}
    public boolean toggleHide(double x,double y,double z){
        if(hidden){leaveHiding();return true;}
        int room=map.roomAt(x,y,z);
        if(outcome==Outcome.ACTIVE&&crouched&&OutdoorHiding.containing(map.level(),x,y,z,map.hidingDensity())!=null){
            hidden=true;shrubHidden=true;hidingAt=new LevelDefinition.Vec(x,y,z);log("HIDDEN IN SHRUB / moving, standing or a nearby searching ghost exposes you.");return true;
        }
        if(outcome!=Outcome.ACTIVE||!LevelClosets.available(map.level(),room,map.hidingDensity())
                ||!LevelClosets.inside(map.level().socket("hide",room),x,y,z)||!map.traversable(x,y,z)){
            log("Enter an available cupboard, or crouch inside a dense shrub first.");return false;
        }
        hidden=true;hidingAt=new LevelDefinition.Vec(x,y,z);log("HIDDEN IN CUPBOARD / L to emerge. Moving exposes you.");return true;
    }
    private String message = "Inspect the haunting, record instrument measurements, prepare a ward, and attempt the rite.";

    public InvestigationSession(HouseMap map, Role role, long seed) {
        this(map,role,seed,1);
    }
    public InvestigationSession(HouseMap map, Role role, long seed,int level) {
        this(map,role,seed,level,GhostIdentity.Type.SPIRIT);
    }
    /** Campaign injection is host-owned: never render the chosen archetype in deduction UI. */
    public InvestigationSession(HouseMap map,Role role,long seed,int level,
            GhostIdentity.Type assignedGhost) {
        java.util.Objects.requireNonNull(assignedGhost);
        hunterLevel=level;
        maxHealth=HunterProgression.maxHealth(level);
        health=maxHealth;
        this.map=map; this.role=role; this.random=new Random(seed);
        evidenceSeed=seed;
        thermalPolarity=GhostEvidenceProfile.thermal(assignedGhost);
        int month=java.time.LocalDate.now().getMonthValue();
        season=switch(month){
            case 12,1,2->EvidenceSignals.Season.WINTER;
            case 3,4,5->EvidenceSignals.Season.SPRING;
            case 6,7,8->EvidenceSignals.Season.SUMMER;
            default->EvidenceSignals.Season.AUTUMN;
        };
        missionWeather=new MissionWeather(season,MissionWeather.Mode.FIXED,
            EvidenceSignals.Weather.CLEAR,1,seed);
        java.util.Arrays.fill(spectralTrace,-100000);
        this.wards=(role==Role.CLERIC?3:2)+HunterProgression.extraWards(level);
        this.anchorRoom=random.nextInt(map.interiorRoomCount());
        this.ghost=new GhostDomain(map,anchorRoom,assignedGhost);
        this.ghostAi=new GhostDomain.AiController(seed ^ 0x4245434b454eL);
        this.hauntingFolio=new GhostHauntingFolio(seed ^ 0x4841554e54534cL);
        journal.add("Case 001 / All valid instrument measurements are recorded; interpret the evidence yourself.");
        journal.add("All classes can finish the contract; specialist actions change efficiency.");
    }
    public HouseMap map() { return map; }
    public Role role() { return role; }
    public String missionId() { return missionId; }
    public int anchorRoom() { return anchorRoom; } // simulation/test authority; do not expose in player HUD
    public int ghostRoom() { return ghost.room(); }
    /** Render-only identity for local manifestations; never publish to the player's deduction HUD. */
    GhostIdentity.Type ghostVisualType(){return ghost.type();}
    /** Used only by the local simulated van sensor, never by the hunter's HUD. */
    int ghostEnergy(){return ghost.energy();}
    public java.util.List<SceneRenderer.WorldMark> counterMarks(){
        return counters.snapshot(ticks).stream()
                .map(m->new SceneRenderer.WorldMark(m.x(),m.z(),m.height(),m.kind())).toList();
    }
    public int counterCharges(HunterCounters.Kind kind){return counters.charges(0,kind);}
    public boolean deployCounter(HunterCounters.Kind kind,double x,double z,double yaw){
        return deployCounter(kind,x,standingY(x,z),z,yaw);
    }
    public boolean deployCounter(HunterCounters.Kind kind,double x,double y,double z,double yaw){
        if(outcome!=Outcome.ACTIVE)return false;
        try{
            int heading=Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360);
            counters.launch(0,kind,new LanWorld.Body(x,y,z,heading,map.roomAt(x,y,z)),map,ticks);
            log(switch(kind){case CROSS->"Cross thrown: try to interrupt a manifested strike.";
                case WARD->"Ward thrown: temporary shelter where it lands.";
                case SALT->"Salt deployed: slows and reveals grounded spirits only.";});
            return true;
        }catch(IllegalArgumentException denied){log(denied.getMessage());return false;}
    }
    public int ticks() { return ticks; }
    public int health() { return health; }
    public int maxHealth(){return maxHealth;}
    public int hunterLevel(){return hunterLevel;}
    public int stress() { return stress; }
    public boolean useMedicine(FieldSupplies.Kind kind,int amount){
        if(outcome!=Outcome.ACTIVE||amount<1||amount>100)return false;
        if(kind==FieldSupplies.Kind.HEALTH){
            if(health>=maxHealth)return false;
            health=Math.min(maxHealth,health+amount);
            log("Medical treatment restored health. Current HP "+health);
        }else{
            if(stress==0)return false;
            stress=Math.max(0,stress-amount);
            log("Sanity treatment eased distress. Remaining fear "+stress);
        }
        return true;
    }
    public int wards() { return wards; }
    public int payout() { return payouts; }
    public boolean apparitionPhotographed(){return apparitionPhotographed;}
    /** Legacy HUD/API count: different instruments used, NOT verified ghost evidence. */
    public int evidenceCount() { return measuredTools.size(); }
    public boolean hasEvidence(Instrument instrument) { return measuredTools.contains(instrument); }
    public boolean wardPrepared() { return prepared; }
    public boolean ghostManifesting() { return haunted; }
    /** Cosmetic-only temporary authorization; never an evidence confirmation. */
    public boolean ghostSignatureVisible() {
        return haunted && ticks<=signatureAppearanceUntil;
    }
    public Outcome outcome() { return outcome; }
    public String message() { return message; }
    public List<String> journal() { return Collections.unmodifiableList(journal); }
    public String statusLine() { return (hidden?"HIDDEN  ":"")+(crouched?"CROUCHED  ":"")+"INJURY " + (maxHealth-health) + "  FEAR " + stress + "  MEASURED TOOLS " + measuredTools.size() + "  WARDS " + wards; }
    private void log(String text) {
        message = text;
        journal.add(String.format("%02d:%02d  %s",ticks/600,(ticks/10)%60,text));
        if(journal.size()>64) journal.remove(2);
    }
    public void tick(double playerX, double playerZ) {
        tick(playerX,playerZ,false);
    }
    public void tick(double playerX,double playerZ,boolean carriedLight) {
        tick(playerX,standingY(playerX,playerZ),playerZ,carriedLight);
    }
    public void tick(double playerX,double playerY,double playerZ,boolean carriedLight) {
        if(outcome!=Outcome.ACTIVE) return;
        ticks++;
        missionWeather.advance(ticks);
        // Every supernatural action, including AI behavior, follows the same host-style dispatcher.
        // Ghost observation has only room-local presence, never the hunter's global position or notebook.
        int playerRoom=map.roomAt(playerX,playerY,playerZ);
        var fixtureSounds=map.fixtures().advance(ticks);
        for(var f:map.fixtures().states())GameSfx.fixtureState(f);
        for(var sound:fixtureSounds){
            var f=sound.source();var cue=LanSound.project(map,new LanWorld.Body(playerX,playerY,playerZ,0,playerRoom),f.room(),f.x(),f.y(),f.z(),sound.cue(),ticks);
            if(cue!=null)GameSfx.fixtureSound(f,cue);
        }
        var clutterObserver=new LooseClutter.Observer(0,playerX,playerY,playerZ,clutterYaw,clutterPitch,!hidden&&health>0,HunterPosture.eye(crouched));
        for(var hit:map.clutter().advance(List.of(clutterObserver))){health=Math.max(0,health-hit.damage());log("Struck by a flying object! -"+hit.damage()+" health.");if(health==0){finish(Outcome.LOST);return;}}
        if(hidden&&(hidingAt==null||Math.hypot(playerX-hidingAt.x(),playerZ-hidingAt.z())>.02
                ||Math.abs(playerY-hidingAt.y())>.1||(shrubHidden?(!crouched||OutdoorHiding.containing(map.level(),playerX,playerY,playerZ,map.hidingDensity())==null||ghost.room()==playerRoom&&Math.hypot(playerX-map.centerX(playerRoom),playerZ-map.centerZ(playerRoom))<2.4):!LevelClosets.available(map.level(),playerRoom,map.hidingDensity()))))leaveHiding();
        ghost.advance(ticks,playerRoom,playerRoom==ghost.room()&&!hidden?1:0);
        boolean indoor=playerRoom>=0&&playerRoom<HouseMap.INDOOR_ROOMS;
        boolean light=carriedLight||(indoor&&(roomLights==null
            ||roomLights.brightness(playerRoom,ticks)>.32));
        int stressFromRoom=ambientSanity.advance(0,indoor,1,light,
            playerRoom==ghost.room(),GhostTraits.of(ghost.type()).charge());
        if(stressFromRoom>0)stress=Math.min(100,stress+classSkills.fear(0,discipline(),ticks,stressFromRoom));
        int feed=GhostTraits.passiveFear(ghost.type(),ticks,playerRoom==ghost.room()?1:0);
        if(feed>0){
            stress=Math.min(100,stress+classSkills.fear(0,discipline(),ticks,feed));
            if(ticks%90==0)log("Your sanity keeps draining. Something is feeding on your presence.");
        }
        if(ghost.room()>=0&&ghost.room()<spectralTrace.length){
            if(haunted&&GhostTraits.of(ghost.type()).leavesUv()){
                spectralTrace[ghost.room()]=ticks;
                uvEvidence.leave(map,new LanGhostSpatial.Pose(ghost.room(),map.centerX(ghost.room()),standingY(map.centerX(ghost.room()),map.centerZ(ghost.room())),map.centerZ(ghost.room()),0),ticks);
            }
            uvHardware.advancePower(ticks);
            if(saltRoom==ghost.room()&&ticks>saltTick&&GhostTraits.saltAffects(ghost.type())){
                ghost.reveal(30);ghost.slowFor(26);
                log("Salt disturbed in "+map.roomName(saltRoom)+" — grounded spirit slowed and briefly revealed.");
                saltRoom=-1;saltTick=-1;
            }
        }
        HunterCounters.Impact counter=counters.advance(map,ghost.type(),ghost,
            new LanGhostSpatial.Pose(ghost.room(),map.centerX(ghost.room()),standingY(map.centerX(ghost.room()),map.centerZ(ghost.room())),map.centerZ(ghost.room()),0),ticks);
        if(counter.saltTrack())log("Salt tracks: the grounded ghost is visible and slowed.");
        if(counter.repelled())log("Cross struck the manifestation: attack interrupted.");
        GhostDomain.Command command=ghostAi.decide(ghost.observe());
        if(command!=null) {
            GhostDomain.Resolution result=ghost.dispatch(command);
            if(result.applied()) {
                GhostHauntingFolio.Occurrence haunt=hauntingFolio.choose(
                    result.action(),ghost.type(),result.room(),ticks);
                if(haunt!=null){
                    hauntings.addLast(haunt);
                    while(hauntings.size()>8)hauntings.removeFirst();
                }
                if(result.action()==GhostDomain.Action.SIGNATURE)
                    signatureAppearanceUntil=ticks+9;
                map.fixtures().witness(result,ticks,-1);
                LanSound.Cue sound=haunt==null
                    ?LanSound.Cue.GHOST_DRIFT:haunt.entry().cue();
                if(haunt==null||GhostHauntingChoreography.forEvent(haunt.entry().id())==null){
                    if(pendingSounds.size()==24)pendingSounds.removeFirst();
                    pendingSounds.addLast(new WorldCue(result.room(),
                        map.centerX(result.room()),map.floorY(result.room()),
                        map.centerZ(result.room()),sound,ticks));
                }
                int power=switch(result.action()){
                    case WHISPER->3;case DISTURB->8;case MANIFEST->9;
                    case STRIKE,SIGNATURE->10;case DRIFT->0;
                };
                if(result.action()==GhostDomain.Action.WHISPER
                        ||result.action()==GhostDomain.Action.SIGNATURE){
                    lastVocalEventTick=ticks;
                    lastVocalEventRoom=result.room();
                }
                if(power>0){
                    lastEvidenceTick=ticks;
                    lastEvidenceRoom=result.room();
                    lastEvidenceStrength=power;
                }
                if(roomLights!=null && result.room()<HouseMap.INDOOR_ROOMS){
                    RoomLighting.Interference effect=switch(result.action()){
                        case WHISPER->RoomLighting.Interference.MINOR;
                        case DISTURB,MANIFEST->RoomLighting.Interference.MODERATE;
                        case STRIKE->RoomLighting.Interference.DANGEROUS;
                        case SIGNATURE->RoomLighting.Interference.SIGNATURE;
                        default->null;
                    };
                    if(effect!=null)roomLights.pulse(result.room(),effect,ticks);
                }
                if(result.fear()>0) stress=Math.min(100,stress+classSkills.fear(0,discipline(),ticks,result.fear()+partySize-1));
                // Only present events a hunter could plausibly hear in their current room.
                if(playerRoom==result.room() && result.action()==GhostDomain.Action.WHISPER)
                    log("An indistinct whisper moves through "+map.roomName(playerRoom)+".");
                if(playerRoom==result.room() && result.action()==GhostDomain.Action.DISTURB)
                    log("Loose objects shudder in "+map.roomName(playerRoom)+".");
                map.clutter().haunt(ghost.type(),result,List.of(clutterObserver),ticks);
                if(result.harm()>0&&!hidden) {
                    int wound=classSkills.harm(0,discipline(),ticks,prepared?Math.min(result.harm(),4):result.harm()+CampaignPartyRules.additionalHarm(partySize));
                    health=Math.max(0,health-wound);
                    log("A spectral strike! -"+wound+" health. Retreat, or use protective wards.");
                    if(health==0) finish(Outcome.LOST);
                }
            }
        }
        for(var event:hauntings){
            int beat=GhostHauntingChoreography.dueIndex(event,ticks);
            if(beat<0)continue;
            if(stagedHauntSounds.size()==24)stagedHauntSounds.removeFirst();
            stagedHauntSounds.addLast(new HauntSound(event.room(),
                map.centerX(event.room()),map.floorY(event.room()),
                map.centerZ(event.room()),event.entry().id(),beat,ticks));
        }
        haunted=ghost.manifested();
        hauntings.removeIf(event->ticks>=event.untilTick());
        if(haunted && playerRoom==ghost.room() && ticks%10==0)
            stress=Math.min(100,stress+classSkills.fear(0,discipline(),ticks,1));
    }

    /** In-hand use has real case effects. Only three evidence instruments count toward a cleansing. */
    public boolean useEquipment(EquipmentLoadout loadout,double x,double z){
        return useEquipment(loadout,x,z,0);
    }
    public boolean useEquipment(EquipmentLoadout loadout,double x,double z,double yaw){
        return useEquipment(loadout,x,standingY(x,z),z,yaw);
    }
    public boolean useEquipment(EquipmentLoadout loadout,double x,double y,double z,double yaw){
        java.util.Objects.requireNonNull(loadout);
        if(outcome!=Outcome.ACTIVE||!map.traversable(x,y,z)){
            log("Cannot operate equipment here.");return false;
        }
        EquipmentLoadout.Item item=loadout.equipped();
        return switch(item){
            case EMF->scan(Instrument.EMF,x,y,z);
            case THERMOMETER->scan(Instrument.THERMOMETER,x,y,z);
            case SPIRIT_BOX->scan(Instrument.SPIRIT_BOX,x,y,z);
            case UV_LAMP->{
                int room=map.roomAt(x,y,z);
                try{uvHardware.setPower("UV_LIGHT",!uvPowered());}catch(IllegalArgumentException empty){log("UV battery empty. Service at the van rack.");yield false;}
                boolean residue=!uvVisible(x,y,z,yaw).isEmpty();
                log(residue?"UV: transient spectral residue detected in "+map.roomName(room)+"."
                    :"UV: no fresh traces in "+map.roomName(room)+".");
                yield true;
            }
            case PHOTO_CAMERA->{
                if(!loadout.consume(item)){log("Camera empty. Reload at the van equipment rack.");yield false;}
                int room=map.roomAt(x,y,z);
                boolean visible=haunted&&room==ghost.room()
                    &&CameraAim.onTarget(x,z,yaw,map.centerX(ghost.room()),map.centerZ(ghost.room()))
                    &&map.clearLineOfSight(x,y+1.62,z,map.centerX(ghost.room()),
                        standingY(map.centerX(ghost.room()),map.centerZ(ghost.room()))+1.2,map.centerZ(ghost.room()));
                if(visible&&!apparitionPhotographed){
                    apparitionPhotographed=true;
                    log("APPARITION PHOTOGRAPHED. The developed image will count in your case payout.");
                }else log(visible?"Apparition photographed again.":"No apparition visible in the photograph.");
                yield visible;
            }
            case SALT->{
                int room=map.roomAt(x,y,z);
                if(room<0||map.outside(room)){log("Salt must be placed in a traversable interior.");yield false;}
                if(!loadout.consume(item)){log("No salt left. Refill at the van equipment rack.");yield false;}
                saltRoom=room;saltTick=ticks;
                log("Salt scattered in "+map.roomName(room)+". Return later to check for tracks.");
                yield true;
            }
        };
    }

    /**
     * An embodied AI technician may read only the air at its real position.
     * This uses exactly the player's thermometer physics but never writes a
     * journal entry, labels evidence, or reveals a hidden ghost identity.
     */
    public String rawTemperatureAt(double x,double y,double z){
        if(outcome!=Outcome.ACTIVE||!map.traversable(x,y,z))return null;
        int room=map.roomAt(x,y,z);
        if(room<0)return null;
        int age=ticks-lastEvidenceTick;
        int power=age>=0&&age<=40&&lastEvidenceRoom==room
            ?Math.max(0,lastEvidenceStrength-age/7):0;
        boolean indoor=room<HouseMap.INDOOR_ROOMS;
        boolean heated=roomLights==null||roomLights.supplied();
        var location=new EvidenceSignals.Location(season,missionWeather.state().weather(),
            14,room,indoor,heated);
        var measured=EvidenceSignals.temperature(evidenceSeed,ticks,location,
            thermalPolarity,room==anchorRoom,power,missionWeather.state().intensity());
        return "Air "+measured.displayText(EvidenceSignals.Unit.CELSIUS)
            +" / "+measured.displayText(EvidenceSignals.Unit.FAHRENHEIT);
    }

    /** AI instrument reading from its actual physical position. Like a player
     * EMF scan this is ordinary weather + locally generated ghost disturbance,
     * not evidence confirmation or access to a private ghost identity.
     */
    public String rawEmfAt(double x,double y,double z){
        if(outcome!=Outcome.ACTIVE||!map.traversable(x,y,z))return null;
        int room=map.roomAt(x,y,z);
        if(room<0)return null;
        int age=ticks-lastEvidenceTick;
        int power=age>=0&&age<=40?Math.max(0,lastEvidenceStrength-age/7):0;
        var measured=EvidenceSignals.emf(evidenceSeed,ticks,room,lastEvidenceRoom,power,
            false,missionWeather.state().weather(),missionWeather.state().intensity());
        return "EMF "+measured.level()+"/10";
    }

    /** Environmental measurements mirror the LAN's numeric evidence model. */
    /** A valid scan ALWAYS produces a measurement and a journal entry. */
    public boolean scan(Instrument tool, double x, double z) {
        return scan(tool,x,standingY(x,z),z);
    }
    public boolean scan(Instrument tool,double x,double y,double z){
        if(outcome!=Outcome.ACTIVE||tool==null)return false;
        if(!map.traversable(x,y,z))return false;
        int room=map.roomAt(x,y,z);
        if(room<0)return false;
        int age=ticks-lastEvidenceTick;
        int eventPower=age>=0&&age<=40
            ?Math.max(0,lastEvidenceStrength-age/7):0;
        String reading;
        switch(tool){
            case EMF -> reading=rawEmfAt(x,y,z);
            case THERMOMETER -> {
                reading=rawTemperatureAt(x,y,z);
            }
            case SPIRIT_BOX -> {
                double distance=Math.hypot(x-map.centerX(ghost.room()),
                    z-map.centerZ(ghost.room()));
                boolean fresh=SpiritBoxResponses.qualifies(room,ghost.room(),
                    lastVocalEventRoom,lastVocalEventTick,ticks,true,
                    GhostEvidenceProfile.compatible(ghost.type(),GhostFieldGuide.Clue.SPIRIT_REPLY),
                    distance,
                    EquipmentTiers.stage("SPIRIT_BOX",EquipmentTiers.Tier.I).reachCm()/100.0,
                    lastSpiritReplyTick);
                latestSpiritReply=fresh?SpiritBoxResponses.choose(lastVocalEventTick,0):null;
                if(fresh)lastSpiritReplyTick=lastVocalEventTick;
                reading=SpiritBoxResponses.observation(latestSpiritReply);
            }
            default -> throw new AssertionError(tool);
        }
        lastReadings.put(tool,reading);
        measuredTools.add(tool);
        log("T"+ticks+" / "+map.roomName(room)+" / "+tool+" / "+reading);
        return true;
    }
    public boolean placeWard(double x,double z) {
        return placeWard(x,standingY(x,z),z);
    }
    public boolean placeWard(double x,double y,double z){
        if(outcome!=Outcome.ACTIVE || wards<=0) return false;
        if(!map.traversable(x,y,z)) return false;
        wards--;
        prepared=map.roomAt(x,y,z)==anchorRoom;
        stress=Math.max(0,stress-(role==Role.CLERIC?35:17));
        log(prepared?"Ward established at the attachment locus; the circle is ready.":"Defensive ward established; fear reduced. The primary anchor lies elsewhere.");
        return true;
    }
    public boolean cleanse(double x,double z) {
        return cleanse(x,standingY(x,z),z);
    }
    public boolean cleanse(double x,double y,double z){
        if(outcome!=Outcome.ACTIVE) return false;
        if(!map.traversable(x,y,z))return false;
        if(map.roomAt(x,y,z)!=anchorRoom) { log("Ritual failed: no true attachment anchor in this room.");stress=Math.min(100,stress+5);return false; }
        if(!prepared) { log("The anchor needs a prepared ward before the banishment rite.");return false; }
        finish(Outcome.CLEANSED);
        log("Anchor broken. Spirit banished. Return to the extraction point to secure the site.");
        return true;
    }
    public boolean extract(double x,double z) {
        return extract(x,standingY(x,z),z);
    }
    public boolean extract(double x,double y,double z){
        var local=map.van().local(x,z);
        if(Math.abs(y-map.floorY(HouseMap.EXTRACTION_ROOM))>.25
                ||map.roomAt(x,y,z)!=HouseMap.EXTRACTION_ROOM
                ||Math.abs(local.x())>=VanStaging.HALF_WIDTH-VanStaging.WALL_THICKNESS
                ||local.z()<VanStaging.FRONT||local.z()>=VanStaging.REAR-VanStaging.WALL_THICKNESS) {
            log("Return inside the expedition van to extract.");return false; }
        if(outcome==Outcome.ACTIVE) { finish(Outcome.RETREATED);log("Team withdrew with partial evidence. The haunting remains."); }
        else if(outcome==Outcome.CLEANSED) log("Site secured. Beckenburg case closed.");
        return outcome!=Outcome.LOST;
    }
    private double standingY(double x,double z){return map.groundHeight(x,z,map.roomAt(x,z));}
    private void finish(Outcome result) {
        if(rewarded)return;
        outcome=result;rewarded=true;
        int base=switch(result){case CLEANSED->220 + measuredTools.size()*40 + health/2 + (apparitionPhotographed?45:0);case RETREATED->measuredTools.size()*30 + (apparitionPhotographed?25:0);case LOST->0;case ACTIVE->0;};
        payouts=CampaignPartyRules.hostCredits(base,partySize);
    }
}
