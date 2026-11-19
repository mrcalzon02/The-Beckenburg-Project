package org.beckenburg;

/**
 * Host-monotonic, packet-rate-independent elapsed time for live locomotion.
 * Protocol sequence validation and collision remain on the authoritative host.
 * Gameplay/evidence clocks are separate and may continue at 10 Hz.
 */
final class LanRealtimeClock {
    static final double INITIAL_SECONDS=.020;
    static final double MAX_SECONDS=.100;
    private long lastNanos=Long.MIN_VALUE;
    double advance(long nowNanos){
        if(lastNanos==Long.MIN_VALUE){lastNanos=nowNanos;return INITIAL_SECONDS;}
        long elapsed=nowNanos-lastNanos;
        if(elapsed<5_000_000L)return 0; // bursts cannot mint more displacement
        lastNanos=nowNanos;
        // Releasing WASD for a second must not credit 100ms of motion to
        // the very first fresh input. Keep start/stop latency genuinely low.
        if(elapsed>200_000_000L)return INITIAL_SECONDS;
        return Math.min(MAX_SECONDS,elapsed/1_000_000_000.0);
    }
    void reset(){lastNanos=Long.MIN_VALUE;}
}
