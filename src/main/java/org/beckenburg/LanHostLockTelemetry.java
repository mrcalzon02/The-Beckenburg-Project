package org.beckenburg;

import java.util.Arrays;
import java.util.Locale;

/**
 * Local-only opt-in measurement of authoritative host mutex contention.
 * No packets, coordinates, ghost identities or gameplay state leave the JVM.
 * All acquisition and hold measurements use System.nanoTime(), never host ticks.
 */
final class LanHostLockTelemetry {
    enum Section { REALTIME_INPUT, HOST_TICK, OTHER_COMMAND }
    static final int HISTORY=256;
    record Measure(long count,double waitP50Ms,double waitP95Ms,double waitP99Ms,
                   double holdP50Ms,double holdP95Ms,double holdP99Ms,
                   long waitOver20Ms,long holdOver50Ms) {}
    record Snapshot(Measure realtime,Measure tick,Measure commands) {}
    private static final class Bucket {
        final long[] waits=new long[HISTORY],holds=new long[HISTORY];
        int size,next;
        long count,waitOver20,holdOver50;
    }
    private final boolean enabled;
    private final Bucket[] buckets=new Bucket[Section.values().length];
    LanHostLockTelemetry(boolean enabled){
        this.enabled=enabled;
        for(int i=0;i<buckets.length;i++)buckets[i]=new Bucket();
    }
    boolean enabled(){return enabled;}
    synchronized void record(Section section,long waitNs,long heldNs){
        if(!enabled)return;
        if(section==null||waitNs<0||heldNs<0)
            throw new IllegalArgumentException("Invalid host lock sample");
        Bucket bucket=buckets[section.ordinal()];
        bucket.waits[bucket.next]=waitNs;
        bucket.holds[bucket.next]=heldNs;
        bucket.next=(bucket.next+1)%HISTORY;
        if(bucket.size<HISTORY)bucket.size++;
        bucket.count++;
        if(waitNs>20_000_000L)bucket.waitOver20++;
        if(heldNs>50_000_000L)bucket.holdOver50++;
    }
    private static double quantile(long[] data,int count,double fraction){
        if(count==0)return 0;
        var sorted=Arrays.copyOf(data,count);
        Arrays.sort(sorted);
        return sorted[Math.max(0,(int)Math.ceil(count*fraction)-1)]/1_000_000.0;
    }
    private static Measure measure(Bucket bucket){
        return new Measure(bucket.count,
            quantile(bucket.waits,bucket.size,.5),quantile(bucket.waits,bucket.size,.95),
            quantile(bucket.waits,bucket.size,.99),
            quantile(bucket.holds,bucket.size,.5),quantile(bucket.holds,bucket.size,.95),
            quantile(bucket.holds,bucket.size,.99),
            bucket.waitOver20,bucket.holdOver50);
    }
    synchronized Snapshot snapshot(){
        return new Snapshot(measure(buckets[Section.REALTIME_INPUT.ordinal()]),
            measure(buckets[Section.HOST_TICK.ordinal()]),
            measure(buckets[Section.OTHER_COMMAND.ordinal()]));
    }
    synchronized String localReport(){
        Snapshot current=snapshot();
        return String.format(Locale.ROOT,
            "HOST_LOCK real_input_count=%d wait_p95_ms=%.3f hold_p95_ms=%.3f wait_over_20=%d "+
            "tick_count=%d wait_p95_ms=%.3f hold_p95_ms=%.3f hold_over_50=%d "+
            "other_count=%d wait_p95_ms=%.3f hold_p95_ms=%.3f",
            current.realtime().count(),current.realtime().waitP95Ms(),
            current.realtime().holdP95Ms(),current.realtime().waitOver20Ms(),
            current.tick().count(),current.tick().waitP95Ms(),
            current.tick().holdP95Ms(),current.tick().holdOver50Ms(),
            current.commands().count(),current.commands().waitP95Ms(),
            current.commands().holdP95Ms());
    }
}
