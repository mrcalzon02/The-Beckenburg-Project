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
        Set<String> expected=new HashSet<>();
        for(int i=1;i<=20;i++)expected.add(String.format(java.util.Locale.ROOT,"ghost_line_%02d.wav",i));
        check(voices.containsAll(expected),"all 20 legacy numbered recordings must remain reachable");
        for(int i:new int[]{23,25,26,29})expected.add(String.format(java.util.Locale.ROOT,"ghost_line_%02d.wav",i));
        check(voices.equals(expected),"strike routes exactly the preserved legacy and four approved contextual lines");
        check(strikes>0&&strikes<400,"hunting lines must be occasional");
        boolean idle=false,impact=false;
        for(int tick=0;tick<100;tick++){
            idle|="ghost_idles.wav".equals(RecordedSfx.ghostVoiceName(
                new LanSound.CueEvent(LanSound.Cue.WHISPER,0,80,tick)));
            impact|="ghost_jump.wav".equals(RecordedSfx.ghostVoiceName(
                new LanSound.CueEvent(LanSound.Cue.IMPACT,0,80,tick)));
        }
        check(idle&&impact,"dedicated idle and jump clips must be reachable");
        System.out.println("PASS GhostVoicePackRoutingTest / 20 legacy strike lines, four contextual strike lines, idle and jump; no synthetic evidence");
    }
}
