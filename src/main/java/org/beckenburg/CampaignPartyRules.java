package org.beckenburg;

/**
 * Single campaign host owns the mission, stock and advancement.
 * Every physically deployed Hunter (AI or human) counts toward both
 * additional threat and host credits, never extra class XP or guest saves.
 */
public final class CampaignPartyRules {
    private CampaignPartyRules(){}
    public static int requireSize(int members){
        if(members<1||members>6)throw new IllegalArgumentException("Hunter party must be 1..6");
        return members;
    }
    public static int recoveryRate(int members){
        return 100+15*(requireSize(members)-1);
    }
    public static int additionalHarm(int members){
        return requireSize(members)-1;
    }
    public static int hostCredits(int soloCredits,int members){
        if(soloCredits<0)throw new IllegalArgumentException("negative award");
        return Math.multiplyExact(soloCredits,requireSize(members));
    }
}
