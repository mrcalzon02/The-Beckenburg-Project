package org.beckenburg;

/** Deterministic presentation pacing; never changes the 50Hz input sender. */
public final class PresentationRateTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args){
        check(PresentationRate.periodNanos(PresentationRate.Target.STANDARD_60)
                ==16_666_667L,"A 60Hz render interval is 16.667ms, not 20ms");
        check(PresentationRate.nextDelayMillis(PresentationRate.Target.STANDARD_60,0)==17,
            "Use rounded-up integer timer ticks for sub-millisecond remainder");
        check(PresentationRate.nextDelayMillis(PresentationRate.Target.STANDARD_60,8_000_000L)==9,
            "Draw duration is part of the current period, not additional waiting");
        check(PresentationRate.nextDelayMillis(PresentationRate.Target.STANDARD_60,22_000_000L)==1,
            "Overrunning frame immediately permits next callback without overlap");
        check(PresentationRate.nextDelayMillis(PresentationRate.Target.COMPATIBILITY_50,0)==20,
            "Legacy 50 FPS presentation target is available for A/B comparison");
        check(PresentationRate.nextDelayMillis(PresentationRate.Target.HIGH_90,0)==12,
            "High-refresh 90Hz target is independent from network movement");
        check(PresentationRate.nextDelayMillis(PresentationRate.Target.HIGH_120,0)==9,
            "High-refresh 120Hz target remains bounded by Swing integer milliseconds");
        check(PresentationRate.nextDelayMillis(PresentationRate.Target.HIGH_120,150_000_000L)==1,
            "Even very slow renders are not double-delayed");
        check(PresentationRate.current()!=null&&PresentationRate.initialDelayMillis()>=1,
            "Persisted target must always be a valid nonzero frame period");
        System.out.println("PASS 50/60/90/120 client presentation rates, bounded overruns and independent host clock");
    }
}
