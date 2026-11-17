package org.beckenburg;

/** Continuous host-clock pressure for a standard 20–30 minute investigation.
 * Early contact, provocation and a solved rite remain possible at every stage. */
final class GhostPacing {
    static final int TICKS_PER_MINUTE=600;
    static final int FULL_PRESSURE_TICK=20*TICKS_PER_MINUTE;
    private GhostPacing(){}
    private static int blend(int tick,int early,int late){
        int elapsed=Math.max(0,Math.min(tick,FULL_PRESSURE_TICK));
        return early+(late-early)*elapsed/FULL_PRESSURE_TICK;
    }
    /** Scales qualifying feeding ticks, never changes a species' feeding condition. */
    static int rechargePercent(int tick){return blend(tick,48,116);}
    static int minorPeriod(int baseTicks,int tick,int frequency){
        int scaled=baseTicks*blend(tick,210,110)/frequency;
        return Math.max(6,scaled);
    }
    /** Rest from major hunts is longer than the vulnerable post-hunt phase.
     * Investigators can witness ordinary haunts between major hunts. */
    static int frenzyRest(int baseTicks,int tick){return baseTicks*blend(tick,550,240)/100;}
    /** The first hit stays available; rebuilding a physical attack is slower early on. */
    static int strikeCooldown(int base,int usedAt){return base*blend(usedAt,470,180)/100;}
    static int clawCooldown(int base,int usedAt){return base*blend(usedAt,220,120)/100;}
    static int signaturePremium(int tick){return blend(tick,30,0);}
    static int signatureMeanTicks(int tick){return blend(tick,15*TICKS_PER_MINUTE,5*TICKS_PER_MINUTE);}
    static int naturalSanityThreshold(int threshold,int tick){
        return Math.max(0,threshold-blend(tick,20,0));
    }
}
