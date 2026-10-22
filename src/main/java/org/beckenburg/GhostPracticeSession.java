package org.beckenburg;

import java.util.List;

/** A genuine HUMAN-origin controller exercising the same local GhostDomain dispatcher as AI.
 * One embodied, hearing Hunter AI now patrols; still a local training match, not a campaign.
 */
public final class GhostPracticeSession {
    public enum Outcome { ACTIVE, TWO_STRIKES }
    private final HouseMap map;
    private final GhostDomain ghost;
    private final GhostPracticeHunterAi hunter;
    private final HunterCounters counters=new HunterCounters();
    private final long evidenceSeed;
    private int lastEventRoom=-1,lastEventTick=-10000,lastEventPower;
    private int tick, strikes;
    private long sequence;
    private Outcome outcome=Outcome.ACTIVE;
    private String status="Listen for a nearby investigator. Haunt, manifest, then strike.";
    private GhostDomain.Resolution lastResolution;

    public GhostPracticeSession(HouseMap map, long seed) {
        this.map=java.util.Objects.requireNonNull(map);
        if (!map.valid()) throw new IllegalArgumentException("invalid practice map");
        int anchor=(int)Math.floorMod(seed, map.interiorRoomCount());
        ghost=new GhostDomain(map,anchor);
        hunter=new GhostPracticeHunterAi(map,anchor,seed);
        evidenceSeed=seed^0x535045435452414CL;
    }
    public int tickCount(){return tick;}
    public int ghostRoom(){return ghost.room();}
    public int strikes(){return strikes;}
    public Outcome outcome(){return outcome;}
    public String status(){return status;}
    public GhostDomain.Observation observation(){return ghost.observe();}
    /** The simulated hunter's exact location is deliberately concealed unless the ghost senses them. */
    public int visibleHunterRoom(){return ghost.observe().hearsHunter()?hunter.room():-1;}
    public List<SceneRenderer.RemoteBody> visibleHunterBodies(){
        return visibleHunterRoom()<0?List.of():List.of(hunter.sprite());
    }
    public GhostPracticeHunterAi hunter(){return hunter;}
    /** Physical counter objects, also rendered in first-person practice. */
    public List<SceneRenderer.WorldMark> counterMarks(){
        return counters.snapshot(tick).stream()
            .map(c->new SceneRenderer.WorldMark(c.x(),c.z(),c.height(),c.kind())).toList();
    }
    public int counterCharges(HunterCounters.Kind kind){return counters.charges(0,kind);}
    public GhostDomain.Resolution lastResolution(){return lastResolution;}
    private String environmentalReading(EquipmentLoadout.Item tool){
        int room=hunter.room();
        int age=tick-lastEventTick;
        int strength=age>=0&&age<=40?Math.max(0,lastEventPower-age/7):0;
        if(tool==EquipmentLoadout.Item.EMF){
            var measured=EvidenceSignals.emf(evidenceSeed,tick,room,lastEventRoom,
                strength,GhostEvidenceProfile.emfBand(ghost.type()),
                EvidenceSignals.Weather.CLEAR,1);
            return "EMF "+measured.level()+"/10";
        }
        if(tool==EquipmentLoadout.Item.THERMOMETER){
            var location=new EvidenceSignals.Location(EvidenceSignals.Season.AUTUMN,
                EvidenceSignals.Weather.CLEAR,14,room,!map.outside(room),true);
            var measured=EvidenceSignals.temperature(evidenceSeed,tick,location,
                GhostEvidenceProfile.thermal(ghost.type()),room==ghost.observe().anchorRoom(),
                room==lastEventRoom?strength:0,1);
            return "Air "+measured.displayText(EvidenceSignals.Unit.CELSIUS)+
                " / "+measured.displayText(EvidenceSignals.Unit.FAHRENHEIT);
        }
        throw new IllegalArgumentException("no such issued scanner");
    }
    /** Authority evaluates real geometry, not a magical shared ghost-position feed. */
    private boolean hunterSeesApparition(){
        if(!ghost.manifested()||hunter.room()!=ghost.room())return false;
        var eye=hunter.position();
        double gx=map.centerX(ghost.room()),gz=map.centerZ(ghost.room());
        double dx=gx-eye.x(),dz=gz-eye.z(),distance=Math.hypot(dx,dz);
        if(distance>.35&&distance<5.5){
            double yaw=Math.toRadians(hunter.heading());
            double forward=(dx*Math.sin(yaw)+dz*Math.cos(yaw))/distance;
            return forward>=.4&&map.clearLineOfSight(
                eye.x(),eye.y()+1.62,eye.z(),
                gx,map.floorY(ghost.room())+1.0,gz);
        }
        return false;
    }

    public void tick(){
        if(outcome!=Outcome.ACTIVE)return;
        tick++;
        hunter.advance(tick);
        ghost.advance(tick,hunter.room());
        EquipmentLoadout.Item tool=hunter.toolDue(tick);
        if(tool!=null)hunter.observed(tick,tool,environmentalReading(tool));
        if(tick>=321&&hunterSeesApparition())hunter.spottedManifestation(tick);
        HunterCounters.Kind defense=hunter.counterDue(tick,counters);
        if(defense!=null){
            try{
                counters.launch(0,defense,hunter.listener(),map,tick);
                hunter.counterDeployed(defense,tick);
            }catch(IllegalArgumentException refused){
                // Never bypass invalid placement or the equipment's real cooldown.
            }
        }
        counters.advance(map,ghost.type(),ghost,
            new LanGhostSpatial.Pose(ghost.room(),map.centerX(ghost.room()),
                map.floorY(ghost.room()),map.centerZ(ghost.room()),0),tick);
    }
    public GhostDomain.Resolution act(GhostDomain.Action action,int targetRoom){
        if(action==null)throw new IllegalArgumentException("action");
        if(outcome!=Outcome.ACTIVE)return null;
        GhostDomain.Command input=new GhostDomain.Command(action,targetRoom,++sequence,GhostDomain.Source.HUMAN);
        GhostDomain.Resolution result=ghost.dispatch(input);
        lastResolution=result;
        if(!result.applied()){
            status="Rejected: "+result.reason()+".";
            return result;
        }
        // Only the projected audible cue crosses into Hunter AI perception.
        // No hidden Ghost identity, true location or anchor is provided.
        LanSound.Cue cue=switch(action){
            case DRIFT->LanSound.Cue.GHOST_DRIFT;
            case WHISPER->LanSound.Cue.WHISPER;
            case DISTURB->LanSound.Cue.DISTURB;
            case MANIFEST->LanSound.Cue.MANIFEST;
            case STRIKE->LanSound.Cue.STRIKE;
            case SIGNATURE->LanSound.Cue.SIGNATURE;
        };
        hunter.hear(LanSound.project(map,hunter.listener(),result.room(),
            map.centerX(result.room()),map.floorY(result.room()),
            map.centerZ(result.room()),cue,tick));
        hunter.frightened(result.fear());
        if(action!=GhostDomain.Action.DRIFT){
            lastEventTick=tick;lastEventRoom=result.room();
            lastEventPower=switch(action){
                case WHISPER->3;case DISTURB->8;case MANIFEST->9;
                case STRIKE,SIGNATURE->10;case DRIFT->0;
            };
        }
        if(result.harm()>0){
            hunter.wounded(result.harm());
            strikes++;
            status="Strike registered ("+strikes+"/2). Energy "+result.energy()+".";
            if(strikes==2){outcome=Outcome.TWO_STRIKES;status="Ghost training complete. Two legal strikes registered.";}
        } else {
            status=switch(action){
                case DRIFT -> "Phased into "+map.roomName(result.room())+".";
                case WHISPER -> "Whisper emitted. Nearby investigator may hear it.";
                case DISTURB -> "Environmental disturbance released.";
                case MANIFEST -> "Manifestation active. Strike when the investigator is near.";
                case STRIKE -> "Strike resolved.";
                case SIGNATURE -> "Signature haunting released. Energy "+result.energy()+".";
            };
        }
        return result;
    }
}
