package org.beckenburg;

/** Continuous host time is unrelated to 10Hz evidence/gameplay ticks. */
public final class LanRealtimeClockTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        LanRealtimeClock clock=new LanRealtimeClock();
        long start=1_000_000_000L;
        double elapsed=clock.advance(start);
        for(int i=1;i<=50;i++)elapsed+=clock.advance(start+i*20_000_000L);
        check(Math.abs(elapsed-1.02)<.000001,"50 Hz must retain physical seconds");
        check(clock.advance(start+1_001_000_000L)==0,"packet bursts must not create free steps");
        check(Math.abs(clock.advance(start+1_020_000_000L)-.020)<.000001,
            "rejected burst does not reset host elapsed clock");
        check(Math.abs(clock.advance(start+5_020_000_000L)-.100)<.000001,
            "network stalls cannot accumulate a teleport");
        clock.reset();
        check(Math.abs(clock.advance(start+6_000_000_000L)-.020)<.000001,
            "new movement epoch starts without 100ms lurch");
        System.out.println("PASS: host realtime movement is time-scaled at 50 Hz, bounded, non-accelerating");
    }
}
