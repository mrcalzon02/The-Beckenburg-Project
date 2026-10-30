package org.beckenburg;

import java.util.HashSet;
import java.util.Set;

/** Headless, deterministic vocal-cue selection, independent of an audio device. */
public final class GhostVoicePackRoutingTest {
    private static void check(boolean value,String why){
        if(!value)throw new AssertionError(why);
    }
    public static void main(String[] args){
        Set<String> voices=new HashSet<>();
        int strikes=0;
        for(int tick=0;tick<1000;tick++){
            var strike=new LanSound.CueEvent(LanSound.Cue.STRIKE,0,80,tick);
            String chosen=RecordedSfx.ghostVoiceName(strike);
            check(java.util.Objects.equals(chosen,RecordedSfx.ghostVoiceName(strike)),
                    "variant must be replay/network deterministic");
            if(chosen!=null){
                check(chosen.matches("ghost_line_\\d\\d\\.wav"),
                        "hunting variant points to the staged asset set");
                voices.add(chosen);strikes++;
            }
            check(RecordedSfx.ghostVoiceName(
                new LanSound.CueEvent(LanSound.Cue.DOOR,0,80,tick))==null,
                "normal environment must not turn into vocal jump scares");
        }
        check(voices.size()==20,"all 20 numbered recordings must be reachable");
        check(strikes>0&&strikes<400,"hunting lines must be occasional");
        boolean idle=false,impact=false;
        for(int tick=0;tick<100;tick++){
            idle|="ghost_idles.wav".equals(RecordedSfx.ghostVoiceName(
                new LanSound.CueEvent(LanSound.Cue.WHISPER,0,80,tick)));
            impact|="ghost_jump.wav".equals(RecordedSfx.ghostVoiceName(
                new LanSound.CueEvent(LanSound.Cue.IMPACT,0,80,tick)));
        }
        check(idle&&impact,"dedicated idle and jump clips must be reachable");
        System.out.println("PASS GhostVoicePackRoutingTest / 22 assets routable, no synthetic evidence");
    }
}
