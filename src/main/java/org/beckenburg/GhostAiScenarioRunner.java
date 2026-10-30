package org.beckenburg;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.beckenburg.editor.ai.NpcAiTelemetry;

/**
 * Deterministic authoring-only host-rule and ghost-controller scenario.
 * Calls REAL GhostDomain, GhostActionCadence, LanGhostPursuit, and HouseMap.
 * Does NOT substitute for full physics, frenzy, multiplayer or sensory playtesting.
 */
public final class GhostAiScenarioRunner {
    public record Event(int tick,String action,boolean accepted,String reason) {}
    public record Result(GhostIdentity.Type type,HouseMap.Site site,int duration,
                         int accepted,int denied,int drift,int emittedSounds,
                         int finalRoom,int energy,List<Event> recent,
                         NpcAiTelemetry.Snapshot timings) {
        public Result {recent=List.copyOf(recent);}
    }
    private GhostAiScenarioRunner(){}
    public static Result run(GhostIdentity.Type type,HouseMap.Site site,int duration,long seed,
                             boolean syntheticSound) {
        Objects.requireNonNull(type);Objects.requireNonNull(site);
        if(duration<1||duration>1800)throw new IllegalArgumentException("Use 1..1800 simulation ticks");
        HouseMap map=new HouseMap(site);
        GhostDomain ghost=new GhostDomain(map,4,type);
        NpcAiTelemetry telemetry=new NpcAiTelemetry();
        telemetry.setEnabled(true);
        LanGhostPursuit pursuit=new LanGhostPursuit(map,seed,type,telemetry);
        GhostActionCadence cadence=new GhostActionCadence(type,new Random(seed^0x5eeda11L));
        LevelDefinition.Vec origin=map.spawn(4);
        List<Event> recent=new ArrayList<>();
        int applied=0,rejected=0,drift=0,delivered=0;
        long sequence=0;
        for(int tick=1;tick<=duration;tick++){
            // A controlled, coarse 100ms host tick. The synthetic listener occupies
            // authored room 4 only; there is NO actual investigator body or contact.
            int occupants=syntheticSound&&ghost.room()==4?1:0;
            int listener=syntheticSound?4:-1;
            ghost.advance(tick,listener,occupants);
            if(syntheticSound&&tick%40==0){
                // Test source is a fixed authoring marker, not an actual player feed.
                pursuit.heard(ghost.room(),4,origin.x(),origin.z(),
                    LanGhostPursuit.Sound.EQUIPMENT,tick);
                delivered++;
            }
            long started=telemetry.begin();
            GhostDomain.Observation state=ghost.observe();
            telemetry.end(NpcAiTelemetry.Metric.PERCEPTION,started);
            started=telemetry.begin();
            GhostDomain.Command choice;
            try{choice=pursuit.decide(state,false);}
            finally{telemetry.end(NpcAiTelemetry.Metric.DECISION,started);}
            GhostDomain.Action action=choice==null?null:choice.action();
            int destination=choice==null?state.room():choice.targetRoom();
            if(action==null&&cadence.due(tick)){
                action=cadence.fallback(state,false);
                destination=state.room();
            }
            if(action==null)continue;
            // No physical contact exists in this isolated rule runner: even if
            // a contract accidentally offered STRIKE, it cannot be dispatched.
            if(action==GhostDomain.Action.STRIKE){
                rejected++;
                record(recent,new Event(tick,"STRIKE",false,"No spatially verified victim"));
                continue;
            }
            GhostDomain.Resolution appliedEvent=ghost.dispatch(new GhostDomain.Command(
                action,destination,++sequence,GhostDomain.Source.AI));
            if(appliedEvent.applied()){
                applied++;
                if(action==GhostDomain.Action.DRIFT)drift++;
                cadence.completed(tick,appliedEvent);
            }else rejected++;
            record(recent,new Event(tick,action.name(),appliedEvent.applied(),appliedEvent.reason()));
        }
        return new Result(type,site,duration,applied,rejected,drift,delivered,
            ghost.room(),ghost.energy(),recent,telemetry.snapshot());
    }
    private static void record(List<Event> log,Event event){
        if(log.size()==128)log.remove(0);
        log.add(event);
    }
}
