package org.beckenburg;

/**
 * Monotonic introduction clock with a bounded GPU-preview readiness window.
 * No game tick, input, hunter pose, GPU call or blocking wait occurs here.
 * A prepared preview can start the cinematic; a finite timeout guarantees
 * the existing software overview can complete even after failed GPU setup.
 */
final class OpeningPlaybackClock {
    private final long createdNs,limitNs;
    private long originNs=Long.MIN_VALUE;
    private boolean timedOut;

    OpeningPlaybackClock(long createdNs,long maxPreviewWaitNs) {
        if(maxPreviewWaitNs<=0)throw new IllegalArgumentException("Nonzero readiness window");
        this.createdNs=createdNs;limitNs=maxPreviewWaitNs;
    }
    synchronized void previewPrepared(long readyNs) {
        if(originNs!=Long.MIN_VALUE)return; // once running, never rewind
        if(readyNs<createdNs)throw new IllegalArgumentException("Nonmonotonic preview readiness");
        if(readyNs-createdNs<=limitNs)originNs=readyNs;
        else{
            originNs=createdNs+limitNs;
            timedOut=true;
        }
    }
    synchronized double progress(long nowNs,double durationSeconds,boolean needsPreview) {
        if(!Double.isFinite(durationSeconds)||durationSeconds<=0)
            throw new IllegalArgumentException("Finite positive cinematic duration required");
        long start=createdNs;
        if(needsPreview){
            if(originNs==Long.MIN_VALUE){
                if(nowNs-createdNs<limitNs)return 0;
                originNs=createdNs+limitNs;
                timedOut=true;
            }
            start=originNs;
        }
        return Math.max(0,Math.min(1,(nowNs-start)/1_000_000_000.0/durationSeconds));
    }
    synchronized boolean readinessTimedOut(){return timedOut;}
    synchronized boolean playbackStarted(){return originNs!=Long.MIN_VALUE;}
}
