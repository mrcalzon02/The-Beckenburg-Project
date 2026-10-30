package org.beckenburg;

import org.beckenburg.editor.ai.NpcAiTelemetry;

public final class GhostAiScenarioRunnerTest {
    private static void check(boolean yes,String msg){if(!yes)throw new AssertionError(msg);}
    public static void main(String[] args){
        for(var species:new GhostIdentity.Type[]{
                GhostIdentity.Type.SPIRIT,GhostIdentity.Type.WRAITH,
                GhostIdentity.Type.ANCIENT_SHADE,GhostIdentity.Type.ELDER_BLADE_DEMON}){
            var first=GhostAiScenarioRunner.run(species,HouseMap.Site.HOUSE,320,8123L,true);
            var second=GhostAiScenarioRunner.run(species,HouseMap.Site.HOUSE,320,8123L,true);
            check(first.recent().equals(second.recent())&&
                first.accepted()==second.accepted()&&first.denied()==second.denied()&&
                first.finalRoom()==second.finalRoom()&&first.energy()==second.energy(),
                "Seeded real-rule decisions are deterministic for "+species);
            check(first.duration()==320&&first.emittedSounds()==8,
                "Bounded authored sound pulses");
            check(first.recent().size()<=128,"Trace memory capped");
            check(first.recent().stream().noneMatch(x->x.action().equals("STRIKE")),
                "Synthetic audio never grants physical strike contact");
            check(first.timings().timings().get(NpcAiTelemetry.Metric.DECISION).calls()==320,
                "Every actual real AI decision measured");
            check(first.timings().timings().get(NpcAiTelemetry.Metric.HEARING).calls()>0,
                "Real game acoustic processing measured");
        }
        var quiet=GhostAiScenarioRunner.run(GhostIdentity.Type.SPIRIT,HouseMap.Site.HOUSE,
            200,2026L,false);
        check(quiet.emittedSounds()==0&&
            quiet.timings().timings().get(NpcAiTelemetry.Metric.HEARING).calls()==0,
            "No invented sound without explicit synthetic source");
        boolean invalid=false;
        try{GhostAiScenarioRunner.run(GhostIdentity.Type.SPIRIT,HouseMap.Site.HOUSE,
            5000,42L,true);}catch(IllegalArgumentException expected){invalid=true;}
        check(invalid,"Hard scenario time budget enforced");
        System.out.println("PASS seeded real ghost/domain/cadence decision automation with no synthetic strikes");
    }
}
