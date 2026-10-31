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

    /** Storyboard motifs use the same physically scoped, organic PSX spectral
     * texture engine as unscored events. There are no HUD bars or fake actors.
     */
    public static void paint(int[] pixels,int width,int height,
            GhostHauntingFolio.Occurrence event,int tick){
        Score score=forEvent(event.entry().id());
        if(score==null)return;
        HauntingScreenFx.paint(pixels,width,height,event,tick,score.motif());
    }

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
