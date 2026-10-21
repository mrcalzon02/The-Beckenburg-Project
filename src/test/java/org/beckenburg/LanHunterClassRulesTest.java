package org.beckenburg;

/** Actual host-case Hunter classes, using the established solo Cleric defenses. */
public final class LanHunterClassRulesTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args){
        LanCase mission=new LanCase(4,new HouseMap());
        mission.configureHunters(3);
        mission.configurePartyThreat(1);
        mission.setStartingSanity(50);
        mission.configureHunterRole(0,CharacterVisuals.Hunter.CLERIC);
        mission.configureHunterRole(1,CharacterVisuals.Hunter.OBSERVER);
        mission.configureHunterRole(2,CharacterVisuals.Hunter.DEMONOLOGIST);
        check(mission.wards(0)==3&&mission.wards(1)==2&&mission.wards(2)==2,
            "Only the Cleric receives an extra starting ward");
        boolean rejected=false;
        try{mission.configureHunterRole(0,CharacterVisuals.Hunter.OBSERVER);}
        catch(IllegalStateException expected){rejected=true;}
        check(rejected&&mission.wards(0)==3,
            "Rebinding must not replenish consumable wards");
        var strike=new GhostDomain.Resolution(true,"physical strike",
            GhostDomain.Action.STRIKE,5,80,0,20,false);
        mission.applyGhostEffect(5,new int[]{5,5,5},strike,
            new boolean[]{true,true,true});
        check(mission.health(0)==92,
            "Cleric absorbs at most eight points of unwarded damage");
        check(mission.health(1)==78&&mission.health(2)==78,
            "Other Hunters take normal strike damage under party scaling");
        check(mission.ward(0,4).accepted()&&mission.ward(1,4).accepted(),
            "All classes retain ritual ward capabilities");
        check(mission.wards(0)==2&&mission.wards(1)==1,
            "Used wards must be consumed from individual inventories");
        check(mission.stress(0)==15&&mission.stress(1)==30,
            "Cleric ward relieves 35 fear, ordinary ward relieves 20");
        check(mission.wardPrepared(),"The shared ritual anchor uses standard case rules");
        System.out.println("PASS: LAN Cleric grants ward capacity, fear recovery and strike protection with shared ritual rules");
    }
}
