package org.beckenburg;

/** A zero-allocation EDT-only counter for delay before input callbacks are dispatched. */
final class InputDispatchTiming {
    private static final long MAX_DELAY_MS=60_000L;
    private int events;
    private long worstQueueMs;
    private long lastEventNs=Long.MIN_VALUE;

    void received(long whenWallMs,long processingWallMs,long processingNs){
        long delayMs=0;
        if(whenWallMs>=0&&processingWallMs>=whenWallMs)
            delayMs=Math.min(MAX_DELAY_MS,processingWallMs-whenWallMs);
        if(events<Integer.MAX_VALUE)events++;
        worstQueueMs=Math.max(worstQueueMs,delayMs);
        lastEventNs=processingNs;
    }
    int events(){return events;}
    long worstQueueMs(){return worstQueueMs;}
    /** Age only for input dispatched since the previous sampled frame. */
    long lastEventAgeNs(long sampledNs){
        return events==0||lastEventNs==Long.MIN_VALUE||sampledNs<lastEventNs
            ?-1L:sampledNs-lastEventNs;
    }
    void afterFrame(){events=0;worstQueueMs=0;}
    void reset(){afterFrame();lastEventNs=Long.MIN_VALUE;}
}
