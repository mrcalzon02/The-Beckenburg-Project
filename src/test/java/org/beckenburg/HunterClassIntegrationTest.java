package org.beckenburg;

/** No phantom evidence, no wrong-class skills, real per-seat party defense. */
public final class HunterClassIntegrationTest {
    public static void main(String[] args) {
        var mission=new LanCase(4);
        mission.configureHunters(3);
        mission.selectDiscipline(0,CharacterVisuals.Hunter.CLERIC);
        mission.selectDiscipline(1,CharacterVisuals.Hunter.OBSERVER);
        mission.selectDiscipline(2,CharacterVisuals.Hunter.DEMONOLOGIST);
        mission.setStartingSanity(50);
        boolean[] nearby={true,true,false,false,false,false};
        assert !mission.classAbility(0,HunterDiscipline.Ability.INVOKE,10,nearby).accepted();
        assert mission.classAbility(0,HunterDiscipline.Ability.COMFORT,10,nearby).accepted();
        assert mission.stress(0)==30&&mission.stress(1)==30&&mission.stress(2)==50;
        assert !mission.classAbility(0,HunterDiscipline.Ability.COMFORT,11,nearby).accepted();
        assert mission.classAbility(0,HunterDiscipline.Ability.HALLOW,20,nearby).accepted();
        int[] rooms={4,4,4,HouseMap.EXTRACTION_ROOM,HouseMap.EXTRACTION_ROOM,HouseMap.EXTRACTION_ROOM};
        var hit=new GhostDomain.Resolution(true,"real spectral hit",
            GhostDomain.Action.STRIKE,4,60,0,30,true);
        mission.advanceClassClock(25);
        mission.applyGhostEffect(4,rooms,hit);
        assert mission.health(0)>mission.health(2):"Cleric protection should work";
        assert mission.health(1)>mission.health(2):"Nearby observer was shielded";
        assert mission.classAbility(1,HunterDiscipline.Ability.SURVEY,30,
            new boolean[]{false,true,false,false,false,false}).accepted();
        assert mission.classAbility(1,HunterDiscipline.Ability.FOCUS,40,
            new boolean[]{false,true,false,false,false,false}).accepted();
        int before=mission.stress(2);
        assert mission.classAbility(2,HunterDiscipline.Ability.INVOKE,50,
            new boolean[]{false,false,true,false,false,false}).accepted();
        assert mission.stress(2)==before+16;
        assert !mission.classAbility(2,HunterDiscipline.Ability.INVOKE,51,
            new boolean[]{false,false,true,false,false,false}).accepted();
        System.out.println("HunterClassIntegrationTest passed");
    }
}
