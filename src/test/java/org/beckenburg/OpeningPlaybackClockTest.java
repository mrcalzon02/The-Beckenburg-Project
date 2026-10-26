package org.beckenburg;

import java.util.concurrent.TimeUnit;

/** Functional case-start timing and bounded fallback, no sleeps or cosmetics. */
public final class OpeningPlaybackClockTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args) {
        long start=TimeUnit.SECONDS.toNanos(20);
        long timeout=TimeUnit.SECONDS.toNanos(3);
        double duration=LevelOpeningFlyover.DURATION_SECONDS;

        var cold=new OpeningPlaybackClock(start,timeout);
        check(cold.progress(start+TimeUnit.SECONDS.toNanos(2),duration,true)==0,
            "Cold GPU preview may not consume the flyover before a first frame");
        check(!cold.readinessTimedOut()&&!cold.playbackStarted(),
            "A cold frame must not fabricate readiness or fallback");
        cold.previewPrepared(start+TimeUnit.SECONDS.toNanos(2));
        check(cold.progress(start+TimeUnit.SECONDS.toNanos(2),duration,true)==0,
            "Actual first prepared image begins the flyover at its first pose");
        check(Math.abs(cold.progress(start+TimeUnit.SECONDS.toNanos(4),duration,true)
            -2/duration)<1e-9,"After readiness, normal animation time must advance");
        check(cold.progress(start+TimeUnit.SECONDS.toNanos(13),duration,true)==1,
            "Ready introduction must complete without affecting game state");

        var stalled=new OpeningPlaybackClock(start,timeout);
        check(stalled.progress(start+timeout-1,duration,true)==0,
            "Still waiting just before bounded readiness deadline");
        check(stalled.progress(start+timeout,duration,true)==0
            &&stalled.readinessTimedOut(),
            "Unavailable GPU must activate fallback at the deadline");
        double before=stalled.progress(start+timeout+TimeUnit.SECONDS.toNanos(2),duration,true);
        stalled.previewPrepared(start+timeout+TimeUnit.SECONDS.toNanos(3));
        check(stalled.progress(start+timeout+TimeUnit.SECONDS.toNanos(2),duration,true)==before,
            "A late GPU frame must never rewind the already-playing fallback");
        check(stalled.progress(start+timeout+TimeUnit.SECONDS.toNanos(10),duration,true)==1,
            "Failed or late GPU preparation must always reach the player handoff");
        var late=new OpeningPlaybackClock(start,timeout);
        late.previewPrepared(start+timeout+1);
        check(late.readinessTimedOut()&&late.progress(start+timeout,duration,true)==0,
            "Late first frame must not extend the finite preparation deadline");

        var software=new OpeningPlaybackClock(start,timeout);
        check(software.progress(start+TimeUnit.SECONDS.toNanos(2),duration,false)>0
            &&!software.readinessTimedOut(),
            "Software-only overview must start immediately without GPU readiness");
        boolean bad=false;
        try{software.progress(start,0,true);}
        catch(IllegalArgumentException expected){bad=true;}
        check(bad,"Invalid cinematic duration must be rejected");
        System.out.println("PASS flyover readiness: cold hold, prepared start, bounded fallback, late frame monotonicity, software mode");
    }
}
