package org.beckenburg;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bespoke presentation scores for selected folio haunts. All work is cosmetic:
 * no evidence, damage, positions or supernatural identity is created here.
 * Every sound stage is projected from the real source through LanSound.
 */
public final class GhostHauntingChoreography {
    public enum Motif {
        LAMP_STROBE, RADIO_GLITCH, PERSPECTIVE_BEND, DOORFRAME_WIPE,
        SHROUD_SWIRL, FLICKER_STEP, ICE_BLOOM, EDGE_WATCHER,
        IMPACT_RING, GRASPING_EDGES, TWIN_ECHO, BLADE_SWEEP,
        PLAGUE_MIASMA, NOTHIC_IRIS
    }
    public enum Timbre {
        ELECTRIC_ARC, RADIO_HISS, WOOD_CREAK, HOLLOW_BREATH,
        DAMP_DRONE, GLASS_FROST, LOW_THUD, WHISPER_SWELL,
        TWIN_KNOCK, METAL_SCRAPE, PLAGUE_BELL, EYE_TONE
    }
    public record Beat(int offsetTicks,Timbre timbre,LanSound.Cue cue,int gainPercent) {
        public Beat {
            Objects.requireNonNull(timbre);Objects.requireNonNull(cue);
            if(offsetTicks<0||offsetTicks>95||gainPercent<20||gainPercent>100)
                throw new IllegalArgumentException("invalid haunting beat");
        }
    }
    public record Score(String id,Motif motif,List<Beat> beats) {
        public Score {
            Objects.requireNonNull(id);Objects.requireNonNull(motif);
            beats=List.copyOf(beats);
            if(beats.isEmpty()||beats.size()>5||beats.get(0).offsetTicks()!=0)
                throw new IllegalArgumentException("score must begin on its real event");
            int previous=-1;
            for(Beat beat:beats){
                if(beat.offsetTicks()<=previous)
                    throw new IllegalArgumentException("non-monotonic score");
                previous=beat.offsetTicks();
            }
            GhostHauntingFolio.Entry entry=GhostHauntingFolio.entry(id);
            if(entry==null||previous>=entry.duration())
                throw new IllegalArgumentException("score must fit registered event");
        }
    }
    /** A bounded, coordinate-free, host-authored positional audio stage. */
    public record SoundFrame(String id,int beat,int pan,int volume,int tick){
        public SoundFrame {
            Score score=SCORES.get(id);
            if(score==null||beat<0||beat>=score.beats().size()
                    ||pan< -100||pan>100||pan%25!=0
                    ||volume<1||volume>100||tick<0||tick>99999999)
                throw new IllegalArgumentException("invalid haunted sound stage");
        }
        public String frame(){
            return "HAUNTAUDIO|"+id+"|"+beat+"|"+pan+"|"+volume+"|"+tick;
        }
    }
    private static Beat beat(int at,Timbre sound,LanSound.Cue cue,int gain){
        return new Beat(at,sound,cue,gain);
    }
    private static Score score(String id,Motif motif,Beat... beats){
        return new Score(id,motif,List.of(beats));
    }
    private static final Map<String,Score> SCORES=create();
    private static Map<String,Score> create(){
        List<Score> scores=List.of(
            score("lamp_convulsion",Motif.LAMP_STROBE,
                beat(0,Timbre.ELECTRIC_ARC,LanSound.Cue.DISTURB,95),
                beat(10,Timbre.ELECTRIC_ARC,LanSound.Cue.DISTURB,70),
                beat(28,Timbre.LOW_THUD,LanSound.Cue.DISTURB,55)),
            score("radio_crawl",Motif.RADIO_GLITCH,
                beat(0,Timbre.RADIO_HISS,LanSound.Cue.DISTURB,75),
                beat(12,Timbre.RADIO_HISS,LanSound.Cue.HORROR_STING_B,90),
                beat(26,Timbre.WHISPER_SWELL,LanSound.Cue.WHISPER,60)),
            score("bent_perspective",Motif.PERSPECTIVE_BEND,
                beat(0,Timbre.DAMP_DRONE,LanSound.Cue.DISTURB,80),
                beat(16,Timbre.WOOD_CREAK,LanSound.Cue.DISTURB,90)),
            score("doorframe_figure",Motif.DOORFRAME_WIPE,
                beat(0,Timbre.HOLLOW_BREATH,LanSound.Cue.MANIFEST,60),
                beat(18,Timbre.WOOD_CREAK,LanSound.Cue.MANIFEST,78),
                beat(39,Timbre.WHISPER_SWELL,LanSound.Cue.WHISPER,40)),
            score("shrouded_witness",Motif.SHROUD_SWIRL,
                beat(0,Timbre.DAMP_DRONE,LanSound.Cue.MANIFEST,75),
                beat(27,Timbre.HOLLOW_BREATH,LanSound.Cue.MANIFEST,70)),
            score("flicker_walker",Motif.FLICKER_STEP,
                beat(0,Timbre.LOW_THUD,LanSound.Cue.MANIFEST,65),
                beat(11,Timbre.LOW_THUD,LanSound.Cue.FOOTFALL,75),
                beat(23,Timbre.LOW_THUD,LanSound.Cue.FOOTFALL,85),
                beat(37,Timbre.WHISPER_SWELL,LanSound.Cue.MANIFEST,45)),
            score("frozen_silhouette",Motif.ICE_BLOOM,
                beat(0,Timbre.GLASS_FROST,LanSound.Cue.MANIFEST,70),
                beat(22,Timbre.HOLLOW_BREATH,LanSound.Cue.MANIFEST,90),
                beat(43,Timbre.GLASS_FROST,LanSound.Cue.HORROR_STING_C,60)),
            score("blindspot_watcher",Motif.EDGE_WATCHER,
                beat(0,Timbre.WHISPER_SWELL,LanSound.Cue.MANIFEST,60),
                beat(22,Timbre.HOLLOW_BREATH,LanSound.Cue.MANIFEST,65)),
            score("pressure_wave",Motif.IMPACT_RING,
                beat(0,Timbre.LOW_THUD,LanSound.Cue.STRIKE,100),
                beat(12,Timbre.DAMP_DRONE,LanSound.Cue.TENSION_BUILD,65)),
            score("grasping_darkness",Motif.GRASPING_EDGES,
                beat(0,Timbre.WHISPER_SWELL,LanSound.Cue.STRIKE,70),
                beat(18,Timbre.LOW_THUD,LanSound.Cue.STRIKE,100)),
            score("paired_revelation",Motif.TWIN_ECHO,
                beat(0,Timbre.TWIN_KNOCK,LanSound.Cue.SIGNATURE,80),
                beat(14,Timbre.TWIN_KNOCK,LanSound.Cue.SIGNATURE,80),
                beat(38,Timbre.WHISPER_SWELL,LanSound.Cue.SIGNATURE,70)),
            score("executioners_coming",Motif.BLADE_SWEEP,
                beat(0,Timbre.METAL_SCRAPE,LanSound.Cue.SIGNATURE,70),
                beat(20,Timbre.METAL_SCRAPE,LanSound.Cue.SIGNATURE,90),
                beat(42,Timbre.LOW_THUD,LanSound.Cue.SIGNATURE,100)),
            score("plague_procession",Motif.PLAGUE_MIASMA,
                beat(0,Timbre.PLAGUE_BELL,LanSound.Cue.SIGNATURE,65),
                beat(28,Timbre.PLAGUE_BELL,LanSound.Cue.SIGNATURE,85),
                beat(55,Timbre.HOLLOW_BREATH,LanSound.Cue.SIGNATURE,75)),
            score("unblinking_judgment",Motif.NOTHIC_IRIS,
                beat(0,Timbre.EYE_TONE,LanSound.Cue.SIGNATURE,55),
                beat(24,Timbre.EYE_TONE,LanSound.Cue.SIGNATURE,90),
                beat(49,Timbre.LOW_THUD,LanSound.Cue.SIGNATURE,100))
        );
        java.util.Map<String,Score> byId=new java.util.HashMap<>();
        for(Score score:scores)
            if(byId.putIfAbsent(score.id(),score)!=null)
                throw new IllegalStateException("duplicate haunting score");
        return Map.copyOf(byId);
    }
    public static List<Score> scores(){return List.copyOf(SCORES.values());}
    public static Score forEvent(String id){return SCORES.get(id);}
    public static SoundFrame decodeSound(String frame){
        List<String> parts=LanProtocol.parts(frame);
        if(parts.size()!=6||!parts.get(0).equals("HAUNTAUDIO"))
            throw new IllegalArgumentException("invalid haunting audio packet");
        return new SoundFrame(parts.get(1),
            LanProtocol.integer(parts.get(2),0,4),
            LanProtocol.integer(parts.get(3),-100,100),
            LanProtocol.integer(parts.get(4),1,100),
            LanProtocol.integer(parts.get(5),0,99999999));
    }
    public static Beat due(GhostHauntingFolio.Occurrence occurrence,int atTick){
        Score score=forEvent(occurrence.entry().id());
        if(score==null)return null;
        int age=atTick-occurrence.startTick();
        for(Beat beat:score.beats())if(beat.offsetTicks()==age)return beat;
        return null;
    }
    public static int dueIndex(GhostHauntingFolio.Occurrence occurrence,int atTick){
        Score score=forEvent(occurrence.entry().id());
        if(score==null)return -1;
        for(int i=0;i<score.beats().size();i++)
            if(occurrence.startTick()+score.beats().get(i).offsetTicks()==atTick)
                return i;
        return -1;
    }

    /** PSX-era storyboarding: visual gestures, not a second ghost actor. */
    public static void paint(int[] pixels,int width,int height,
            GhostHauntingFolio.Occurrence event,int tick) {
        Score score=forEvent(event.entry().id());
        if(score==null||tick<event.startTick()||tick>=event.untilTick())return;
        int age=tick-event.startTick();
        double t=age/(double)event.entry().duration();
        double swell=Math.sin(Math.PI*t);
        if(swell<=0)return;
        int[] original=pixels.clone(); // scanline shifts always sample the complete earlier image
        int seed=event.entry().id().hashCode();
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            double nx=(x-width*.5)/(width*.5),ny=(y-height*.5)/(height*.5);
            double radius=Math.hypot(nx,ny);
            double phase=Math.sin(age*.39),grain=Math.sin(x*.68+y*1.71+seed*.001+age*.8);
            int dx=0;
            double effect=switch(score.motif()){
                case LAMP_STROBE->(age%11<3?1.0:.05)*swell*(.35+.65*Math.abs(grain));
                case RADIO_GLITCH->(y%7<2?.85:.19)*swell*grain;
                case PERSPECTIVE_BEND->swell*Math.sin(y*.065+age*.14);
                case DOORFRAME_WIPE->swell*(Math.abs(nx+.28-.42*t)<.055?1:-.25);
                case SHROUD_SWIRL->swell*Math.sin(radius*16-age*.27+Math.atan2(ny,nx)*2);
                case FLICKER_STEP->swell*(age%13<4?1:-.32)*(Math.abs(nx-.12*Math.sin(age*.1))<.33?1:.12);
                case ICE_BLOOM->swell*(Math.sin((Math.abs(nx)+Math.abs(ny))*(14+12*t))>.84?1:-.18);
                case EDGE_WATCHER->swell*(radius>.66?(radius-.66)*2.7:-.18);
                case IMPACT_RING->swell*(Math.abs(radius-(age%29)/23.0)<.085?1:-.22);
                case GRASPING_EDGES->swell*(Math.abs(nx)>1.0-.48*swell
                    +.10*Math.sin(ny*11+age*.22)?-1:.17);
                case TWIN_ECHO->swell*(Math.abs(nx+.48)<.10||Math.abs(nx-.48)<.10?1:-.15);
                case BLADE_SWEEP->swell*(Math.abs(nx+ny*.65-1.3+2.6*t)<.09?1:-.17);
                case PLAGUE_MIASMA->swell*Math.sin(nx*10+ny*8+age*.10+2*Math.sin(nx*4));
                case NOTHIC_IRIS->swell*(Math.abs(radius-(.72-.55*t))<.13?1:-.22);
            };
            if(score.motif()==Motif.RADIO_GLITCH&&y%7<2)dx=(int)(8*grain*swell);
            if(score.motif()==Motif.PERSPECTIVE_BEND)dx=(int)(5*swell*Math.sin(y*.08+age*.14));
            int sourceX=Math.max(0,Math.min(width-1,x+dx));
            int col=original[y*width+sourceX];
            int delta=(int)(Math.max(-1,Math.min(1,effect))*55);
            int r=(col>>16)&255,g=(col>>8)&255,b=col&255;
            if(score.motif()==Motif.ICE_BLOOM||score.motif()==Motif.PLAGUE_MIASMA){
                r=clamp(r-delta/3);g=clamp(g+delta/2);
                b=clamp(b+(score.motif()==Motif.ICE_BLOOM?delta:-delta/3));
            }else if(score.motif()==Motif.EDGE_WATCHER||score.motif()==Motif.GRASPING_EDGES
                    ||score.motif()==Motif.FLICKER_STEP||score.motif()==Motif.DOORFRAME_WIPE){
                r=clamp(r-Math.abs(delta));g=clamp(g-Math.abs(delta));b=clamp(b-Math.abs(delta)/2);
            }else{
                r=clamp(r+delta);g=clamp(g+delta);b=clamp(b+delta);
            }
            pixels[y*width+x]=(r<<16)|(g<<8)|b;
        }
    }
    private static int clamp(int n){return Math.max(0,Math.min(255,n));}

    /** Pure stereo 16-bit audio, no sound hardware or UI thread requirement. */
    public static byte[] synthesize(SoundFrame frame,int effectsVolume){
        if(effectsVolume<0||effectsVolume>100)throw new IllegalArgumentException("effects volume");
        Beat beat=SCORES.get(frame.id()).beats().get(frame.beat());
        int length=LanAudio.SAMPLE_RATE*3/4;
        byte[] pcm=new byte[length*4];
        int hash=frame.id().hashCode()*31+frame.beat()*104729;
        double pan=frame.pan()/100.0;
        double left=Math.sqrt((1-pan)*.5),right=Math.sqrt((1+pan)*.5);
        double gain=frame.volume()/100.0*effectsVolume/100.0;
        double hz=switch(beat.timbre()){
            case ELECTRIC_ARC->390;case RADIO_HISS->800;case WOOD_CREAK->124;
            case HOLLOW_BREATH->76;case DAMP_DRONE->68;case GLASS_FROST->940;
            case LOW_THUD->74;case WHISPER_SWELL->130;case TWIN_KNOCK->142;
            case METAL_SCRAPE->610;case PLAGUE_BELL->430;case EYE_TONE->155;
        };
        for(int i=0;i<length;i++){
            double t=i/(double)LanAudio.SAMPLE_RATE,phase=(double)i/length;
            hash^=hash<<13;hash^=hash>>>17;hash^=hash<<5;
            double noise=(hash&65535)/32767.5-1;
            double e=Math.pow(Math.sin(Math.PI*(i+.5)/length),.75);
            double wave=switch(beat.timbre()){
                case ELECTRIC_ARC->.50*noise*Math.sin(2*Math.PI*hz*t)+.26*Math.sin(2*Math.PI*hz*.5*t);
                case RADIO_HISS->.55*noise*Math.sin(2*Math.PI*40*t)+.25*Math.sin(2*Math.PI*hz*t);
                case WOOD_CREAK->.42*Math.sin(2*Math.PI*(hz+90*phase*phase)*t)+.18*noise*Math.sin(t*33);
                case HOLLOW_BREATH->.36*noise*Math.sin(2*Math.PI*(20+18*phase)*t)+.30*Math.sin(2*Math.PI*hz*t);
                case DAMP_DRONE->.37*Math.sin(2*Math.PI*hz*t)+.24*Math.sin(2*Math.PI*(hz*.51+13*phase)*t);
                case GLASS_FROST->.28*Math.sin(2*Math.PI*hz*t)+.25*Math.sin(2*Math.PI*hz*1.58*t)+.13*noise;
                case LOW_THUD->.55*Math.sin(2*Math.PI*(hz-50*phase)*t)*Math.exp(-phase*4)+.19*noise*Math.exp(-phase*8);
                case WHISPER_SWELL->.45*noise*Math.sin(2*Math.PI*23*t)+.21*Math.sin(2*Math.PI*hz*t);
                case TWIN_KNOCK->.49*noise*(Math.exp(-24*Math.abs(phase-.17))
                    +Math.exp(-26*Math.abs(phase-.49)))+.38*Math.sin(2*Math.PI*hz*t)*Math.exp(-phase*5);
                case METAL_SCRAPE->.32*noise*Math.sin(2*Math.PI*hz*t)+.23*Math.sin(2*Math.PI*(hz+400*phase)*t);
                case PLAGUE_BELL->.37*Math.sin(2*Math.PI*hz*t)+.27*Math.sin(2*Math.PI*hz*1.4*t)+.19*Math.sin(2*Math.PI*hz*2.05*t);
                case EYE_TONE->.27*Math.sin(2*Math.PI*(hz+500*phase)*t)+.32*Math.sin(2*Math.PI*(hz*.75+250*phase)*t);
            };
            short a=(short)Math.round(Math.max(-.9,Math.min(.9,wave*e*gain))*left*22000);
            short b=(short)Math.round(Math.max(-.9,Math.min(.9,wave*e*gain))*right*22000);
            int at=i*4;
            pcm[at]=(byte)a;pcm[at+1]=(byte)(a>>8);
            pcm[at+2]=(byte)b;pcm[at+3]=(byte)(b>>8);
        }
        return pcm;
    }
}
