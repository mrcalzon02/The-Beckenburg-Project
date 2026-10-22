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
    public GhostDomain.Resolution lastResolution(){return lastResolution;}

    public void tick(){
        if(outcome!=Outcome.ACTIVE)return;
        tick++;
        hunter.advance(tick);
        ghost.advance(tick,hunter.room());
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
