package org.beckenburg;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Advisory, headless audio-content diagnostic; no audio hardware is required. */
public final class GhostVoiceExpansionTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args){
        Set<String> audible=new HashSet<>();
        Set<String> hunting=new HashSet<>();
        Set<String> old=new HashSet<>();
        List<LanSound.Cue> contexts=List.of(
            LanSound.Cue.WHISPER,LanSound.Cue.DISTURB,LanSound.Cue.GHOST_DRIFT,
            LanSound.Cue.FOOTFALL,LanSound.Cue.MANIFEST,LanSound.Cue.STRIKE,
            LanSound.Cue.TENSION_BUILD,LanSound.Cue.IMPACT,
            LanSound.Cue.HORROR_STING_A);
        LanSound.CueEvent longOne=null;
        for(int tick=0;tick<3000;tick++){
            for(LanSound.Cue cue:contexts){
                var event=new LanSound.CueEvent(cue,0,80,tick);
                String clip=RecordedSfx.ghostVoiceName(event);
                check(java.util.Objects.equals(clip,RecordedSfx.ghostVoiceName(event)),
                    "Vocal selection must be deterministic across peers");
                if(clip==null)continue;
                audible.add(clip);
                check(!clip.equals("ghost_line_27.wav"),
                    "Duplicate ghost27 should alias ghost26, not add a second file");
                if(cue==LanSound.Cue.TENSION_BUILD&&clip.equals("ghost_line_28.wav"))
                    longOne=event;
                if(cue==LanSound.Cue.FOOTFALL)hunting.add(clip);
                if(cue==LanSound.Cue.STRIKE&&clip.startsWith("ghost_line_")){
                    int n=Integer.parseInt(clip.substring(11,13));
                    if(n<=20)old.add(clip);
                }
            }
            check(RecordedSfx.ghostVoiceName(new LanSound.CueEvent(
                LanSound.Cue.DOOR,0,80,tick))==null,
                "Ordinary door Foley must never masquerade as a ghost voice");
        }
        for(int i:new int[]{21,22,23,24,25,26,28,29})
            check(audible.contains(String.format(java.util.Locale.ROOT,
                "ghost_line_%02d.wav",i)),"New recording not reachable: "+i);
        check(old.size()==20,"All 20 previous lines remain reachable");
        check(hunting.contains("ghost_line_26.wav"),
            "Searching footfall can issue the intermittent vocalization");
        check(longOne!=null,"Long-form hunt recording is never selected");
        byte[] pcm=RecordedSfx.render(longOne,100);
        check(pcm!=null&&pcm.length>16000*4*12,
            "13-second authored recording missing or truncated in packaged build");
        System.out.println("PASS GhostVoiceExpansionTest / eight unique clips, legacy routing, long-form hunt and alias");
    }
}
