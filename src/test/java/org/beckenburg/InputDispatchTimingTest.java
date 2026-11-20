package org.beckenburg;

public final class InputDispatchTimingTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args){
        var tracker=new InputDispatchTiming();
        check(tracker.events()==0&&tracker.lastEventAgeNs(100)==-1,
            "No input should be explicitly unknown");
        tracker.received(1000,1035,1_000_000L);
        tracker.received(1050,1052,2_000_000L);
        check(tracker.events()==2&&tracker.worstQueueMs()==35,
            "Retain worst queue lag for inputs before the frame");
        check(tracker.lastEventAgeNs(10_000_000L)==8_000_000L,
            "Monotonic time from dispatched input to next sample");
        tracker.afterFrame();
        check(tracker.events()==0&&tracker.worstQueueMs()==0&&
            tracker.lastEventAgeNs(20_000_000L)==-1,
            "Idle frames must not invent stale input latency");
        tracker.received(3000,2999,21_000_000L);
        check(tracker.worstQueueMs()==0,"Clock adjustment must not create negative queue delay");
        tracker.received(0,100000,22_000_000L);
        check(tracker.worstQueueMs()==60_000L,"Corrupt huge timestamps must be bounded");
        tracker.reset();
        check(tracker.lastEventAgeNs(22_000_000L)==-1,
            "Stopping capture must clear previous event state");
        System.out.println("PASS input queue delay and event-age instrumentation");
    }
}
