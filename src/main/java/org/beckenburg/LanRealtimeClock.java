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
        // Explicit neutral input resets the clock on key release. While keys
        // remain held, host/network stalls must not become 20ms slow-motion
        // pulses. Never credit more than one legacy 100ms movement quantum.
        return Math.min(MAX_SECONDS,elapsed/1_000_000_000.0);
    }
    void reset(){lastNanos=Long.MIN_VALUE;}
}
