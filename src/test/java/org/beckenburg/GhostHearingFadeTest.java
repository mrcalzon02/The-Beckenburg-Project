package org.beckenburg;

/** Exact-clock, headphone-independent hearing fade and mixer boundary tests. */
public final class GhostHearingFadeTest {
    private static void ok(boolean value,String message){
        if(!value)throw new AssertionError(message);
    }
    private static int sample(byte[] pcm,int frame){
        int p=frame*4;return (short)((pcm[p]&255)|(pcm[p+1]<<8));
    }
    private static byte[] tone(int value){
        byte[] pcm=new byte[512*4];
        for(int i=0;i<512;i++){
            for(int ch=0;ch<2;ch++){
                int p=i*4+ch*2;
                pcm[p]=(byte)value;pcm[p+1]=(byte)(value>>8);
            }
        }
        return pcm;
    }
    public static void main(String[] args){
        GhostHearingFade fade=new GhostHearingFade();
        ok(fade.advance(true,0)==0,"New ghost hearing cannot begin fully audible");
        double previous=0;
        for(int t=100;t<=600;t+=100){
            double g=fade.advance(true,t*1_000_000L);
            ok(g>previous&&g<=1,"Stationary listening fades smoothly in");
            previous=g;
        }
        ok(Math.abs(fade.gain()-1)<.000001,"600ms reaches full hearing");
        double falling=fade.advance(false,700_000_000L);
        ok(falling>0&&falling<1,"Moving fades out instead of hard cut");
        fade.advance(false,800_000_000L);
        fade.advance(false,900_000_000L);
        ok(fade.gain()==0,"260ms fade-out fully mutes previous hunter cues");
        fade.reset();ok(fade.gain()==0,"Site change resets sound envelope");
        fade.advance(true,-500_000_000L);
        ok(fade.advance(true,-400_000_000L)>.1,
            "Negative but monotonic System.nanoTime clocks remain valid");
        fade.reset();
        byte[] ghost=tone(1000),environment=tone(2000);
        byte[] silent=LanAudio.mixHearingForTest(ghost,environment,0,0);
        byte[] loud=LanAudio.mixHearingForTest(ghost,environment,1,1);
        byte[] ramp=LanAudio.mixHearingForTest(ghost,environment,0,1);
        ok(sample(silent,0)==2000&&sample(silent,511)==2000,
            "Fully muted human cues leave spectral ambient/actions untouched");
        ok(sample(loud,0)==3000&&sample(loud,511)==3000,
            "Full hearing restores hunter sounds without changing normal mixing");
        ok(sample(ramp,0)>2000&&sample(ramp,0)<2010&&sample(ramp,511)==3000,
            "Per-block gain interpolation has no abrupt boundary");
        ok(LanVoiceClientAudio.scaledVoiceGain(80,80,.5)==32,
            "Anonymous nearby real speech follows the same hearing gain");
        System.out.println("PASS: host-controlled hearing fade-in/out, isolated audio mixing, voice attenuation");
    }
}
