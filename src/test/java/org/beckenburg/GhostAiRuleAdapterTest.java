package org.beckenburg;

import java.util.Arrays;
import java.util.HashSet;
import org.beckenburg.editor.ai.NpcDecisionWorkbench;

public final class GhostAiRuleAdapterTest {
    private static void check(boolean v,String msg){if(!v)throw new AssertionError(msg);}
    public static void main(String[] args){
        var book=GhostAiRuleAdapter.read();
        var roster=GhostIdentity.Type.values();
        check(book.all().size()==roster.length,"Read every GhostIdentity including Ancient/Elder");
        var ids=new HashSet<String>();
        for(var type:roster){
            var rule=book.get(type.name());
            ids.add(rule.id());
            check(rule.baseId().equals(type.baseType().name()),"True base variant mapping "+type);
            check(rule.rite().equals(type.rite().name()),"Rite from GhostIdentity "+type);
            check(rule.favoredAction().equals(GhostBehavior.profile(type).favored().name()),
                "Preferred action from actual runtime "+type);
            check(rule.passage().equals(GhostTraits.of(type).passage().name()),
                "Wall or door restrictions from actual runtime "+type);
            check(rule.sight().equals(GhostTraits.of(type).sight().name()),
                "Camera and orb visibility from real game "+type);
            check(rule.evidence().get("EMF").equals(GhostEvidenceMatrix.forGhost(type).emf().name()),
                "Evidence reference matches game matrix "+type);
            check(rule.frenzyEnergyCost()==GhostFrenzy.profile(type).energyCost(),
                "Real frenzy energy threshold "+type);
            check(rule.frenzyAttacks().contains("CLAW")==GhostFrenzy.profile(type).permits(GhostFrenzy.Attack.CLAW),
                "Nonlethal species frenzy correctly shown "+type);
        }
        check(ids.size()==roster.length,"No species dropped or duplicated");
        check(book.get("SHADE").sight().equals("CAMERA_ONLY"),"Shade remote-only image");
        check(book.get("YOKAI").passage().equals("DOORS"),"Yokai no wall phase");
        check(book.get("WRAITH").passage().equals("WALLS"),"Wraith wall phase");
        check(!book.get("SPECTER").frenzyAttacks().contains("CLAW"),"Specter no physical frenzy claw");
        for(var type:ListSpecific.values()){
            var obs=GhostAiRuleAdapter.from(GhostAiRuleAdapter.sandbox(type.type,180,true));
            check(!obs.legalActions().contains("STRIKE"),"No strike absent prior manifestation "+type);
            check(obs.legalActions().contains("DRIFT"),"Real map exit available "+type);
            var proposal=NpcDecisionWorkbench.propose(book.get(type.type.name()),obs,false);
            check(!proposal.action().equals("STRIKE"),"Editor workbench never forges strike contact");
            if(proposal.eligible())check(obs.legalActions().contains(proposal.action()),
                "Proposed action must be host legal");
        }
        var testDomain=new GhostDomain(new HouseMap(),0,GhostIdentity.Type.SPIRIT);
        for(int tick=1;tick<=140;tick++)testDomain.advance(tick,0,1);
        check(testDomain.observe().legalActions().contains(GhostDomain.Action.MANIFEST),
            "Host must authorize prior manifest for strike scenario");
        var event=testDomain.dispatch(new GhostDomain.Command(GhostDomain.Action.MANIFEST,
            0,1,GhostDomain.Source.AI));
        check(event.applied(),"Sandbox uses actual host dispatch legality");
        for(int tick=141;tick<=170;tick++)testDomain.advance(tick,0,1);
        var observed=GhostAiRuleAdapter.from(testDomain.observe());
        check(observed.legalActions().contains("STRIKE"),"Host-authorized strike exists during manifestation");
        check(!NpcDecisionWorkbench.inspect(book.get("SPIRIT"),observed,"STRIKE",0,false).eligible(),
            "Editor refuses strike without host spatial contact even if domain allows event");
        check(NpcDecisionWorkbench.inspect(book.get("SPIRIT"),observed,"STRIKE",0,true).eligible(),
            "Explicit host spatial-contact precondition accepted");
        var domainOnly=GhostAiRuleAdapter.from(testDomain.observe());
        check(domainOnly.energy()==testDomain.energy(),"Read-only adapter leaves host energy unchanged");
        System.out.println("PASS "+roster.length+" canonical ghost rules, evidence, movement, frenzy, host legality and contact denial");
    }
    private enum ListSpecific {
        DEMON(GhostIdentity.Type.DEMON),SHADE(GhostIdentity.Type.SHADE),
        ANCIENT_WRAITH(GhostIdentity.Type.ANCIENT_WRAITH),
        ELDER_PLAGUE(GhostIdentity.Type.ELDER_PLAGUE_DEMON);
        final GhostIdentity.Type type;
        ListSpecific(GhostIdentity.Type type){this.type=type;}
    }
}
