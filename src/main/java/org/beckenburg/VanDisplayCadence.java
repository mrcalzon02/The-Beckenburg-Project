package org.beckenburg;

/** Sampled VAN displays. Simulation, network authority and field evidence are not delayed. */
public final class VanDisplayCadence {
    private VanDisplayCadence(){}
    /** At a 10 Hz mission clock, LCD telemetry and the site map refresh every 3 seconds. */
    public static final int TELEMETRY_TICKS=30;
    /** Expensive remote-camera scene capture is capped at one image every 2 seconds. */
    public static final int CAMERA_TICKS=20;
    /** The host evaluates optical disclosure only to van viewers, at most once/second. */
    public static final int CAMERA_SIGHT_TICKS=10;
    public static boolean due(boolean viewer,int tick,int previous,int interval){
        if(interval<1)throw new IllegalArgumentException("Invalid van display interval");
        return viewer&&tick>=0&&(long)tick-previous>=interval;
    }
}
