package org.beckenburg;

/** Deterministic shared contract for human requests and companion decisions. */
public final class HunterDisciplineTest {
    public static void main(String[] args){
        HunterDiscipline engine=new HunterDiscipline(3);
        var cleric=CharacterVisuals.Hunter.CLERIC;
        assert !engine.activate(0,cleric,HunterDiscipline.Ability.INVOKE,10).accepted();
        assert engine.activate(0,cleric,HunterDiscipline.Ability.COMFORT,10).accepted();
        assert !engine.activate(0,cleric,HunterDiscipline.Ability.COMFORT,11).accepted();
        assert engine.activate(0,cleric,HunterDiscipline.Ability.COMFORT,150).accepted();
        assert engine.activate(0,cleric,HunterDiscipline.Ability.HALLOW,150).accepted();
        assert engine.harm(0,cleric,170,20)<20;
        assert engine.harm(0,cleric,251,20)>engine.harm(0,cleric,170,20);
        engine.shareHallow(1,170);
        assert engine.hallowed(1,180);
        assert !engine.hallowed(2,180);
        var observer=CharacterVisuals.Hunter.OBSERVER;
        assert engine.activate(1,observer,HunterDiscipline.Ability.SURVEY,20).accepted();
        assert engine.activate(1,observer,HunterDiscipline.Ability.FOCUS,20).accepted();
        assert engine.fear(1,observer,25,20)<engine.fear(1,observer,170,20);
        var demon=CharacterVisuals.Hunter.DEMONOLOGIST;
        assert engine.activate(2,demon,HunterDiscipline.Ability.SIGIL,30).accepted();
        assert engine.harm(2,demon,50,20)<engine.harm(2,demon,170,20);
        assert engine.activate(2,demon,HunterDiscipline.Ability.INVOKE,30).accepted();
        System.out.println("HunterDisciplineTest passed");
    }
}
