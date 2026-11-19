package org.beckenburg;

/**
 * Host-monotonic, packet-rate-independent elapsed time for live locomotion.
 * Gameplay/evidence clocks remain separate. Explicit neutral input resets
 * any unconsumed movement time; large frame/host stalls cannot teleport.
 */
final class LanRealtimeClock {
    static final double INITIAL_SECONDS = .020;
    static final double MAX_SECONDS = .100;
    static final double MAX_CARRY_SECONDS = .120;
    private static final long MIN_INPUT_INTERVAL_NS = 5_000_000L;
    private long lastNanos = Long.MIN_VALUE;
    private double carrySeconds;
    double advance(long nowNanos) {
        if (lastNanos == Long.MIN_VALUE) {
            lastNanos = nowNanos;
            return INITIAL_SECONDS;
        }
        long elapsed = nowNanos - lastNanos;
        if (elapsed < MIN_INPUT_INTERVAL_NS) return 0;
        lastNanos = nowNanos;
        // A blocked host may receive delayed packets in a burst. Preserve
        // short stalls instead of permanently slowing the player, but never
        // discharge more than 100ms of physical motion in a single collision
        // step or more than 120ms afterward. A stop discards unused credit.
        double available = elapsed / 1_000_000_000.0 + carrySeconds;
        double step = Math.min(MAX_SECONDS, available);
        carrySeconds = Math.min(MAX_CARRY_SECONDS, Math.max(0, available - step));
        return step;
    }
    void reset() {lastNanos = Long.MIN_VALUE; carrySeconds = 0;}
}
