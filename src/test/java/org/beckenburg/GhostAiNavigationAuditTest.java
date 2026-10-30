package org.beckenburg;

import java.util.List;
import org.beckenburg.editor.ai.NpcAiTelemetry;

public final class GhostAiNavigationAuditTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var report=GhostAiNavigationAudit.inspect(map,4);
        check(report.inspectedRooms()==HouseMap.EXTRACTION_ROOM,"Audit excludes van from navigable rooms");
        check(report.site()==HouseMap.Site.HOUSE,"Site identity preserved");
        check(!report.links().isEmpty(),"Uses actual authored portals");
        check(map.ghostExits(HouseMap.EXTRACTION_ROOM).isEmpty(),"Ghost cannot originate in van");
        for(var link:report.links()){
            check(map.ghostExits(link.from()).contains(link.to()),"No fabricated edge");
            check(link.to()!=HouseMap.EXTRACTION_ROOM,"No route into extraction");
            check(link.acousticLoss()==LanAcoustics.cost(map,link.from(),link.to()),
                "Acoustics matches real door/portal material logic");
            var path=GhostAiNavigationAudit.path(map,link.from(),link.to());
            check(!path.isEmpty()&&path.get(0)==link.from()&&path.get(path.size()-1)==link.to(),
                "Adjacent rooms have real path");
            for(int i=1;i<path.size();i++)
                check(map.ghostExits(path.get(i-1)).contains(path.get(i)),
                    "Every hop is authorized");
        }
        boolean bad=false;
        try{GhostAiNavigationAudit.path(map,4,HouseMap.EXTRACTION_ROOM);}
        catch(IllegalArgumentException expected){bad=true;}
        check(bad,"No ghost path into van");
        var diagnostics=new NpcAiTelemetry();
        diagnostics.setEnabled(true);
        var ai=new LanGhostPursuit(map,19L,GhostIdentity.Type.SPIRIT,diagnostics);
        ai.heard(4,4,map.centerX(4),map.centerZ(4),LanGhostPursuit.Sound.RITUAL,30);
        check(diagnostics.snapshot().timings().get(NpcAiTelemetry.Metric.HEARING).calls()==1,
            "Actual pursuit hearing uses live acoustic timings");
        var exits=map.ghostExits(4);
        if(!exits.isEmpty()){
            int neighbor=exits.get(0);
            ai.heard(4,neighbor,map.centerX(neighbor),map.centerZ(neighbor),LanGhostPursuit.Sound.RITUAL,40);
            GhostDomain ghost=new GhostDomain(map,4,GhostIdentity.Type.SPIRIT);
            for(int tick=1;tick<=40;tick++)ghost.advance(tick,-1,0);
            ai.decide(ghost.observe(),false);
            // Even if the particular sound is attenuated, the timer must never leak
            // a hunter coordinate or override the domain's legal actions.
            check(diagnostics.snapshot().timings().get(NpcAiTelemetry.Metric.HEARING).calls()>=2,
                "Cross-room acoustic computation measured");
        }
        check(diagnostics.snapshot().timings().get(NpcAiTelemetry.Metric.DECISION).calls()==0,
            "Decision timing belongs to the authoritative host call site");
        System.out.println("PASS authored navigation, hearing, extraction boundary and per-session AI telemetry");
    }
}
