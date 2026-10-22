package org.beckenburg;

import java.util.Objects;

/** XP curve and earned, bounded field resilience for the selected Hunter class. */
public final class HunterProgression {
    public static final int MAX_LEVEL=20;
    private HunterProgression(){}
    public static int threshold(int level){
        if(level<1||level>MAX_LEVEL)throw new IllegalArgumentException("level");
        int steps=level-1;
        return steps*90+steps*steps*30;
    }
    public static int level(int experience){
        if(experience<0)throw new IllegalArgumentException("negative experience");
        for(int n=MAX_LEVEL;n>1;n--)if(experience>=threshold(n))return n;
        return 1;
    }
    public static int nextThreshold(int experience){
        int current=level(experience);
        return current==MAX_LEVEL?threshold(MAX_LEVEL):threshold(current+1);
    }
    /** Every level is consequential: slightly more stamina/health, no damage cheat. */
    public static int maxHealth(int level){
        if(level<1||level>MAX_LEVEL)throw new IllegalArgumentException("level");
        return 100+(level-1)*2;
    }
    /** Carefully limited extra protective consumables at veteran levels. */
    public static int extraWards(int level){
        if(level<1||level>MAX_LEVEL)throw new IllegalArgumentException("level");
        return (level-1)/5;
    }
    public static String title(int level){
        if(level<1||level>MAX_LEVEL)throw new IllegalArgumentException("level");
        return level>=16?"Veteran Investigator":level>=11?"Senior Investigator":
            level>=6?"Field Specialist":level>=3?"Experienced Hunter":"Novice Hunter";
    }
}
