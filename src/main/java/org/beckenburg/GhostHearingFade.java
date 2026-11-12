package org.beckenburg;

/**
 * Local, monotonic hearing envelope. Only the host's GLISTEN gate can enable
 * reception; this gain merely fades already-authorized sounds and x-ray rings.
 * No hunter location or identity is carried by this class.
 */
public final class GhostHearingFade {
    public static final long FADE_IN_NS=600_000_000L,FADE_OUT_NS=260_000_000L;
    private long previous=Long.MIN_VALUE;
    private double gain;
    public double gain(){return gain;}
    public void reset(){previous=Long.MIN_VALUE;gain=0;}
    public double advance(boolean hostListening,long now){
        if(now<0)throw new IllegalArgumentException("Invalid hearing clock");
        if(previous==Long.MIN_VALUE){previous=now;return gain;}
        // Cap a delayed render frame; never jump to full hearing after a stall.
        long nanos=Math.max(0,Math.min(200_000_000L,now-previous));
        previous=now;
        double delta=(double)nanos/(hostListening?FADE_IN_NS:FADE_OUT_NS);
        gain=Math.max(0,Math.min(1,gain+(hostListening?delta:-delta)));
        return gain;
    }
}
