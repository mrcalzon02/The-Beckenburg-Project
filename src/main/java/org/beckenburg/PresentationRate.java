package org.beckenburg;

import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Local presentation target, independent from the host's authoritative 10 Hz
 * world simulation and 50 Hz network-input sender. Slow frames never overlap:
 * the Swing render timer is one-shot and reschedules only after drawing.
 *
 * 60 Hz replaces the historical 20 ms / 50 Hz render callback limit. Higher
 * rates are optional; actual frames remain workload and driver dependent.
 */
public final class PresentationRate {
    private PresentationRate(){}
    public enum Target {
        COMPATIBILITY_50(50,"50 FPS (compatibility / lower draw load)"),
        STANDARD_60(60,"60 FPS (recommended)"),
        HIGH_90(90,"90 FPS (high-refresh display)"),
        HIGH_120(120,"120 FPS (high-refresh display)");
        private final int hz;
        private final String label;
        Target(int hz,String label){this.hz=hz;this.label=label;}
        public int hz(){return hz;}
        @Override public String toString(){return label;}
    }
    private static final Preferences PREF=Preferences.userNodeForPackage(PresentationRate.class);
    private static volatile Target selected=read();
    private static Target read(){
        try{return Target.valueOf(PREF.get("clientPresentationTarget","STANDARD_60"));}
        catch(IllegalArgumentException invalid){return Target.STANDARD_60;}
    }
    public static Target current(){return selected;}
    public static void save(Target target){
        Target checked=Objects.requireNonNull(target);
        PREF.put("clientPresentationTarget",checked.name());
        selected=checked;
    }
    /** Actual callback interval target in nanoseconds; not a world tick. */
    static long periodNanos(Target target){
        Objects.requireNonNull(target);
        return Math.round(1_000_000_000.0/target.hz());
    }
    /** Swing timers use integer milliseconds, so a 60 Hz first callback is 16 ms. */
    public static int initialDelayMillis(){
        return Math.max(1,1000/current().hz());
    }
    /** Remaining delay from start of the most recent completed actual draw.
     * Over-budget drawing never sleeps another full frame or spins at 0ms. */
    static int nextDelayMillis(Target target,long spentNs){
        long remaining=periodNanos(target)-Math.max(0,spentNs);
        return (int)Math.max(1,(Math.max(0,remaining)+999_999L)/1_000_000L);
    }
    public static int nextDelayMillis(long spentNs){
        return nextDelayMillis(current(),spentNs);
    }
}
