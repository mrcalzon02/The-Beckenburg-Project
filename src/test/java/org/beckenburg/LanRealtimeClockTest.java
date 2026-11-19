package org.beckenburg;

/** Continuous host time is unrelated to 10Hz evidence/gameplay ticks. */
public final class LanRealtimeClockTest {
    private static void near(double a,double b,String message) {
        if (Math.abs(a-b)>1e-8)throw new AssertionError(message+" "+a+" instead of "+b);
    }
    public static void main(String[] args) {
        LanRealtimeClock clock = new LanRealtimeClock();
        long start=1_000_000_000L;
        double elapsed=clock.advance(start);
        for(int i=1;i<=50;i++)elapsed+=clock.advance(start+i*20_000_000L);
        near(elapsed,1.02,"50Hz physical seconds");
        near(clock.advance(start+1_001_000_000L),0,"bursts do not mint movement");
        near(clock.advance(start+1_020_000_000L),.020,"burst does not reset host time");
        near(clock.advance(start+5_020_000_000L),.100,"bounded congestion movement");
        near(clock.advance(start+5_040_000_000L),.100,"bounded saved congestion");
        near(clock.advance(start+5_060_000_000L),.060,"bounded carry exhausted");
        near(clock.advance(start+5_080_000_000L),.020,"normal 50Hz resumes");
        clock.reset();
        near(clock.advance(start+6_000_000_000L),.020,"fresh epoch after stop");
        clock.reset();
        long t=start+7_000_000_000L;
        near(clock.advance(t),.020,"new epoch");
        near(clock.advance(t+20_000_000L),.020,"steady state");
        near(clock.advance(t+180_000_000L),.100,"brief host stall bounded");
        near(clock.advance(t+181_000_000L),0,"burst blocked without carry payment");
        near(clock.advance(t+200_000_000L),.080,"brief host stall recovered in next packet");
        clock.reset();
        near(clock.advance(t+1_000_000_000L),.020,"neutral stop discards any debt");
        System.out.println("PASS: paced movement, bounded host stall recovery, no burst minting, neutral reset");
    }
}
