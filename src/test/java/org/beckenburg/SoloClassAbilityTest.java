package org.beckenburg;

/** Solo and AI companions share real class activation, cooldown and field readings. */
public final class SoloClassAbilityTest {
    public static void main(String[] args){
        HouseMap map=new HouseMap();
        var p=map.spawn(1);
        var observer=new InvestigationSession(map,InvestigationSession.Role.OBSERVER,711L);
        int before=observer.evidenceCount();
        assert observer.classAbility(HunterDiscipline.Ability.SURVEY,p.x(),p.y(),p.z()).accepted();
        assert observer.evidenceCount()==before:"Survey is not hard evidence";
        assert !observer.classAbility(HunterDiscipline.Ability.SURVEY,p.x(),p.y(),p.z()).accepted();
        var cleric=new InvestigationSession(map,InvestigationSession.Role.CLERIC,712L);
        assert !cleric.classAbility(HunterDiscipline.Ability.INVOKE,p.x(),p.y(),p.z()).accepted();
        assert cleric.classAbility(HunterDiscipline.Ability.HALLOW,p.x(),p.y(),p.z()).accepted();
        var ally=new CampaignAlly(map,observer,CharacterVisuals.Hunter.OBSERVER);
        String response=ally.useClassAbility(HunterDiscipline.Ability.SURVEY,
            ally.position().x(),ally.position().y(),ally.position().z());
        assert response.contains("Survey");
        assert ally.observations().size()==2:"Companion must record real sensor samples";
        assert ally.useClassAbility(HunterDiscipline.Ability.SURVEY,
            ally.position().x(),ally.position().y(),ally.position().z()).contains("recharging");
        assert !new HunterDiscipline(1).activate(0,CharacterVisuals.Hunter.CLERIC,
            HunterDiscipline.Ability.FOCUS,20).accepted();
        System.out.println("SoloClassAbilityTest passed");
    }
}
