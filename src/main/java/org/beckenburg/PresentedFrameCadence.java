package org.beckenburg;

import java.util.Arrays;

/** What actually reached the compositor, not duplicate Swing redraw callbacks. */
final class PresentedFrameCadence {
    private static final int HISTORY=120;
    private final double[] ms=new double[HISTORY];
    private int count,next;
    private long lastSeenId=Long.MIN_VALUE,lastDistinctNs=Long.MIN_VALUE;
    private long distinct,duplicates;
    private boolean gpuMode;
    record Snapshot(double p50,double p95,double p99,long distinct,long duplicates,
                    int intervals,int over33,int over50,int over100) {
        double fps(){return p50>0?1000.0/p50:0;}
    }
    void record(long nowNs,boolean gpu,long consumedId){
        if(gpu!=gpuMode){
            gpuMode=gpu;count=next=0;lastSeenId=Long.MIN_VALUE;
            lastDistinctNs=Long.MIN_VALUE;distinct=duplicates=0;
        }
        // The GL worker has not delivered a framebuffer yet.
        if(gpu&&consumedId<=0)return;
        if(gpu&&lastSeenId==consumedId){duplicates++;return;}
        if(lastDistinctNs!=Long.MIN_VALUE && nowNs>lastDistinctNs){
            ms[next]=(nowNs-lastDistinctNs)/1_000_000.0;
            next=(next+1)%HISTORY;
            if(count<HISTORY)count++;
        }
        lastDistinctNs=nowNs;
        lastSeenId=gpu?consumedId:Long.MIN_VALUE;
        distinct++;
    }
    Snapshot snapshot(){
        if(count==0)return new Snapshot(0,0,0,distinct,duplicates,0,0,0,0);
        double[] sorted=Arrays.copyOf(ms,count);
        Arrays.sort(sorted);
        int over33=0,over50=0,over100=0;
        for(double value:sorted){
            if(value>33.3)over33++;
            if(value>50)over50++;
            if(value>100)over100++;
        }
        return new Snapshot(sorted[(count-1)/2],sorted[(int)Math.ceil(count*.95)-1],
            sorted[(int)Math.ceil(count*.99)-1],distinct,duplicates,count,
            over33,over50,over100);
    }
}
