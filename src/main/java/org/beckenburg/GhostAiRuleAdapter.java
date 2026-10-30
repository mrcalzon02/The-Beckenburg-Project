package org.beckenburg;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.beckenburg.editor.ai.NpcDecisionWorkbench;
import org.beckenburg.editor.ai.NpcRuleBook;

/**
 * READ-ONLY Beckenburg adapter for the portable NPC/AI editor module.
 * All data is obtained from live rule classes, not copied species tables.
 * No host execution, command dispatch, evidence fabrication or actor teleport.
 */
public final class GhostAiRuleAdapter {
    private GhostAiRuleAdapter(){}
    public static NpcRuleBook read(){
        List<NpcRuleBook.Rule> all=new ArrayList<>();
        for(GhostIdentity.Type type:GhostIdentity.Type.values()){
            GhostTraits.Profile traits=GhostTraits.of(type);
            GhostBehavior.Profile behavior=GhostBehavior.profile(type);
            GhostFrenzy.Profile frenzy=GhostFrenzy.profile(type);
            GhostEvidenceMatrix.Row evidence=GhostEvidenceMatrix.forGhost(type);
            Map<String,String> sources=new LinkedHashMap<>();
            sources.put("EMF",evidence.emf().name());
            sources.put("TEMPERATURE",evidence.temperature().name());
            sources.put("WRITING",String.join(",",
                evidence.writing().stream().map(Enum::name).toList()));
            sources.put("UV",Boolean.toString(evidence.uv()));
            sources.put("SIGHT",evidence.optical().name());
            sources.put("FUTURE_POSSIBILITIES",String.join(",",
                evidence.future().stream().map(Enum::name).toList()));
            Set<String> allowed=new LinkedHashSet<>();
            for(GhostFrenzy.Attack attack:GhostFrenzy.Attack.values())
                if(frenzy.permits(attack))allowed.add(attack.name());
            all.add(new NpcRuleBook.Rule(type.name(),type.baseType().name(),
                type.elder()?"ELDER":type.ancient()?"ANCIENT":"STANDARD",
                type.rite().name(),type.behavior().name(),
                traits.locomotion().name(),traits.passage().name(),traits.sight().name(),
                traits.charge().name(),traits.chargePeriod(),
                GhostTraits.movementPercent(type),traits.huntsSolitary(),traits.leavesUv(),
                behavior.favored().name(),behavior.aiPeriod(),behavior.tell(),traits.clue(),
                frenzy.energyCost(),frenzy.earliestTick(),allowed,sources));
        }
        return new NpcRuleBook(all);
    }
    /** Host-filtered legal actions only; no hunter health, position, gear or journal. */
    public static NpcDecisionWorkbench.Observation from(GhostDomain.Observation observation){
        if(observation==null)throw new IllegalArgumentException("Host observation required");
        Set<String> legal=new LinkedHashSet<>();
        for(GhostDomain.Action action:observation.legalActions())legal.add(action.name());
        return new NpcDecisionWorkbench.Observation(observation.tick(),observation.room(),
            observation.energy(),observation.hearsHunter(),observation.manifested(),
            observation.exits(),legal);
    }
    /**
     * ISOLATED EDITOR SANDBOX, never an active host session.
     * Runs the real GhostDomain ticking and observes its real action legality.
     * Hearing is simulated by the actual same-room host condition, not copied from
     * a client or an omniscient player-location feed.
     */
    public static GhostDomain.Observation sandbox(GhostIdentity.Type type,int tick,boolean audible){
        if(type==null||tick<0||tick>1000)
            throw new IllegalArgumentException("Editor sandbox allows 0..1000 ticks");
        GhostDomain domain=new GhostDomain(new HouseMap(),0,type);
        for(int i=1;i<=tick;i++)domain.advance(i,audible?0:-1,audible?1:0);
        return domain.observe();
    }
}
