package org.beckenburg;

import javax.sound.sampled.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Optional real-world Foley clips. Curated CC0 NOX files are loaded as packaged
 * WAV sources (including original 24-bit 48/96-kHz recordings); JavaSound resamples
 * to 16-kHz signed stereo PCM. Missing clips fall back to synthesized cues.
 * A source pack or Drive connection is never required on an installed player's PC.
 */
final class RecordedSfx {
    private static final AudioFormat FORMAT=new AudioFormat(16000f,16,2,true,false);
    private static final Map<String,byte[]> CACHE=new HashMap<>();
    private static final int STING_VARIANTS=6;
    private static final int GHOST_LINE_VARIANTS=20;
    // Full spoken utterances can be longer than the four-second Foley safety cap.
    private static final int MAX_GHOST_VOICE_BYTES=16000*4*16;
    // Short Foley clips must not monopolize the single effects line behind critical attack cues.
    private static final int MAX_PCM_BYTES=16000*4*4;
    private static final int SHORT_MECHANICAL_BYTES=16000*4;
    private RecordedSfx(){}

    private static boolean isSting(LanSound.Cue cue){
        return cue==LanSound.Cue.HORROR_STING_A||cue==LanSound.Cue.HORROR_STING_B
            ||cue==LanSound.Cue.HORROR_STING_C;
    }
    static String variantName(LanSound.Cue cue,int index){
        if(!isSting(cue)||index<0||index>=STING_VARIANTS)
            throw new IllegalArgumentException("invalid horror cue variant");
        return cue.name().toLowerCase(java.util.Locale.ROOT)+"_"+index+".wav";
    }

    /** Only presentation: the event already exists and was spatially projected by
     * the authoritative solo/host world. Names are provisional acoustic tags, NOT
     * evidence, translated dialogue, target detection, or hidden ghost coordinates.
     */
    private static final int[] GHOST_AMBIENT={21,22,24};
    private static final int[] GHOST_SEARCH={24,26,29};
    private static final int[] GHOST_MANIFEST={23,24,29};
    private static final int[] GHOST_HUNT={25,26,28,29};
    private static final int[] GHOST_STRIKE={23,25,26,29};
    private static final int[] GHOST_JUMP={23,25};
    private static String ghostVariant(LanSound.CueEvent event,int[] pool){
        int index=Math.floorMod(event.tick()*13+event.cue().ordinal()*11,pool.length);
        return String.format(java.util.Locale.ROOT,"ghost_line_%02d.wav",pool[index]);
    }
    private static String originalLine(LanSound.CueEvent event){
        int index=Math.floorMod((event.tick()/5)*7+event.cue().ordinal()*13,
            GHOST_LINE_VARIANTS)+1;
        return String.format(java.util.Locale.ROOT,"ghost_line_%02d.wav",index);
    }
    /** Sparse, deterministic contextual variations preserve legacy lines 1-20.
     * Track 27 is an exact duplicate of 26 and intentionally has no second WAV.
     */
    static String ghostVoiceName(LanSound.CueEvent event){
        if(event==null)throw new IllegalArgumentException("event");
        LanSound.Cue cue=event.cue();
        int selector=Math.floorMod(event.tick()*31+cue.ordinal()*7,60);
        return switch(cue){
            case WHISPER ->
                selector%9==0?"ghost_idles.wav":
                selector%11==0?ghostVariant(event,GHOST_AMBIENT):null;
            case DISTURB -> selector%17==0?ghostVariant(event,GHOST_AMBIENT):null;
            case GHOST_DRIFT -> selector%11==0?ghostVariant(event,GHOST_AMBIENT):null;
            case FOOTFALL -> selector%13==0?ghostVariant(event,GHOST_SEARCH):null;
            case MANIFEST ->
                selector%5==0?originalLine(event):
                selector%7==0?ghostVariant(event,GHOST_MANIFEST):null;
            case TENSION_BUILD ->
                selector%5==0?originalLine(event):
                selector%7==0?ghostVariant(event,GHOST_HUNT):null;
            case STRIKE ->
                selector%5==0?originalLine(event):
                selector%7==0?ghostVariant(event,GHOST_STRIKE):null;
            case IMPACT ->
                selector%3==0?"ghost_jump.wav":
                selector%5==0?ghostVariant(event,GHOST_JUMP):null;
            case HORROR_STING_A,HORROR_STING_B,HORROR_STING_C ->
                selector%13==0?ghostVariant(event,GHOST_JUMP):null;
            default -> null;
        };
    }

    private static synchronized byte[] original(String filename){
        if(CACHE.containsKey(filename))return CACHE.get(filename);
        String resource="/org/beckenburg/sfx/"+filename;
        byte[] pcm=null;
        try(InputStream input=RecordedSfx.class.getResourceAsStream(resource)){
            if(input!=null){
                try(AudioInputStream file=AudioSystem.getAudioInputStream(new BufferedInputStream(input))){
                    if(!AudioSystem.isConversionSupported(FORMAT,file.getFormat()))
                        throw new UnsupportedAudioFileException("Unsupported curated clip: "+resource);
                    try(AudioInputStream converted=AudioSystem.getAudioInputStream(FORMAT,file)){
                        // Prevent long source captures from serially starving urgent
                        // attack/interaction effects. Keep only the first relevant
                        // second of an ordinary mechanical cue; never buffer the
                        // remainder of the source recording in a shipping build.
                        int ceiling=filename.startsWith("ghost_")?MAX_GHOST_VOICE_BYTES:
                            filename.equals("pickup.wav")||filename.equals("device_on.wav")
                            ||filename.equals("drop.wav")||filename.equals("scanner.wav")
                            ||filename.equals("device_off.wav")
                            ||filename.equals("electric_hum.wav")
                            ||filename.equals("equipment.wav")||filename.equals("dots_motor.wav")
                            ||filename.equals("monitor_hum.wav")
                            ?SHORT_MECHANICAL_BYTES:MAX_PCM_BYTES;
                        // The original NOX Smartphone On&Off capture contains both actions.
                        // Device-off plays the later transition instead of duplicating the on click.
                        if(filename.equals("device_off.wav")){
                            long remaining=16000L*4*4; // skip the first four seconds of stereo PCM
                            while(remaining>0){
                                long skipped=converted.skip(remaining);
                                if(skipped<=0)throw new EOFException("Missing off transition: "+resource);
                                remaining-=skipped;
                            }
                        }
                        byte[] candidate=converted.readNBytes(ceiling);
                        if(candidate.length>0&&candidate.length<=ceiling
                                &&candidate.length%4==0)pcm=candidate;
                        else throw new IOException("Curated clip invalid: "+resource);
                    }
                }
            }
        }catch(Exception invalid){
            System.err.println("Beckenburg Foley ignored (using generated fallback): "+invalid.getMessage());
        }
        CACHE.put(filename,pcm);
        return pcm;
    }

    /** A stable host tick chooses a different archived generic sting, never #1 or #5.
     * Sparse private installations can supply one or several variants; missing
     * recordings retain the deterministic procedural synth fallback.
     */
    private static byte[] original(LanSound.CueEvent event){
        LanSound.Cue cue=event.cue();
        String vocal=ghostVoiceName(event);
        if(vocal!=null){
            byte[] recorded=original(vocal);
            if(recorded!=null)return recorded;
        }
        String stem=cue.name().toLowerCase(java.util.Locale.ROOT);
        if(cue==LanSound.Cue.STEP_STONE){
            // Curated owner's Drive archive: three distinct real rock footsteps.
            // Ticks are chosen by the host in multiplayer and locally for castle steps.
            int first=Math.floorMod(event.tick(),3);
            for(int k=0;k<3;k++){
                byte[] candidate=original("step_stone_"+((first+k)%3)+".wav");
                if(candidate!=null)return candidate;
            }
        }
        if(isSting(cue)){
            int start=Math.floorMod(event.tick()*31+cue.ordinal()*7,STING_VARIANTS);
            for(int k=0;k<STING_VARIANTS;k++){
                byte[] candidate=original(variantName(cue,(start+k)%STING_VARIANTS));
                if(candidate!=null)return candidate;
            }
        }
        return original(stem+".wav");
    }
    /** Return a fresh, attenuated/panned buffer; never modify the cached clean master. */
    static byte[] render(LanSound.CueEvent event,int effectsVolume){
        byte[] source=original(event);
        if(source==null)return null;
        if(effectsVolume<0||effectsVolume>100)throw new IllegalArgumentException("effects volume");
        double pan=event.pan()/100d;
        double left=Math.sqrt(1-pan),right=Math.sqrt(1+pan);
        double volume=event.volume()/100d*effectsVolume/100d;
        byte[] out=new byte[source.length];
        for(int i=0;i<source.length;i+=4){
            short l=(short)((source[i]&255)|(source[i+1]<<8));
            short r=(short)((source[i+2]&255)|(source[i+3]<<8));
            int lv=(int)Math.round(l*volume*left);
            int rv=(int)Math.round(r*volume*right);
            lv=Math.max(Short.MIN_VALUE,Math.min(Short.MAX_VALUE,lv));
            rv=Math.max(Short.MIN_VALUE,Math.min(Short.MAX_VALUE,rv));
            out[i]=(byte)lv;out[i+1]=(byte)(lv>>8);
            out[i+2]=(byte)rv;out[i+3]=(byte)(rv>>8);
        }
        return out;
    }
    static boolean isPackaged(LanSound.Cue cue){
        if((cue==LanSound.Cue.DISTURB||cue==LanSound.Cue.GHOST_DRIFT)
                &&original("ghost_line_21.wav")!=null)return true;
        if(cue==LanSound.Cue.FOOTFALL&&original("ghost_line_26.wav")!=null)
            return true;

        if(cue==LanSound.Cue.WHISPER&&original("ghost_idles.wav")!=null)return true;
        if(cue==LanSound.Cue.IMPACT&&original("ghost_jump.wav")!=null)return true;
        if(cue==LanSound.Cue.MANIFEST||cue==LanSound.Cue.STRIKE
                ||cue==LanSound.Cue.TENSION_BUILD){
            for(int i=1;i<=GHOST_LINE_VARIANTS;i++)
                if(original(String.format(java.util.Locale.ROOT,
                        "ghost_line_%02d.wav",i))!=null)return true;
        }
        if(cue==LanSound.Cue.STEP_STONE){
            for(int i=0;i<3;i++)
                if(original("step_stone_"+i+".wav")!=null)return true;
        }
        if(isSting(cue)){
            for(int i=0;i<STING_VARIANTS;i++)
                if(original(variantName(cue,i))!=null)return true;
        }
        return original(cue.name().toLowerCase(java.util.Locale.ROOT)+".wav")!=null;
    }
}
