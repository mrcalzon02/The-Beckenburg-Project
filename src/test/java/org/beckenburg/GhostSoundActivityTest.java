package org.beckenburg;

/** Pure profile law, valid wire payloads, gestures and privacy. */
public final class GhostSoundActivityTest {
    private static void ok(boolean test,String label){
        if(!test)throw new AssertionError(label);
    }
    private static void invalid(Runnable step){
        try{step.run();throw new AssertionError("Malformed acoustic event accepted");}
        catch(IllegalArgumentException expected){}
    }
    public static void main(String[] args){
        var walking=GhostSoundProfile.forSound(LanGhostPursuit.Sound.FOOTSTEP);
        var emote=GhostSoundProfile.forSound(LanGhostPursuit.Sound.EMOTE);
        var shortText=GhostSoundProfile.chat(2);
        var mediumText=GhostSoundProfile.chat(30);
        var longText=GhostSoundProfile.chat(LanChat.MAX_CHARS);
        var voice=GhostSoundProfile.forSound(LanGhostPursuit.Sound.VOICE);
        ok(walking.equals(emote),"Emotes equal walking in loudness and time");
        ok(shortText.intensity()<mediumText.intensity()
            &&mediumText.intensity()<longText.intensity(),
            "Chat intensity increases strictly with text length");
        ok(shortText.durationTicks()<mediumText.durationTicks()
            &&mediumText.durationTicks()<longText.durationTicks(),
            "Chat duration increases strictly with text length");
        ok(voice.intensity()==GhostSoundProfile.VOICE_INTENSITY
            &&voice.durationTicks()==10,
            "Live voice audio uses one constant-strength one-second sound pulse");
        ok(GhostSoundProfile.acousticBudget(LanGhostPursuit.Sound.CHAT,shortText)
            <GhostSoundProfile.acousticBudget(LanGhostPursuit.Sound.CHAT,longText),
            "Long chat has bounded greater reach than brief text");
        ok(GhostSoundProfile.acousticBudget(LanGhostPursuit.Sound.EMOTE,emote)
            ==GhostSoundProfile.acousticBudget(LanGhostPursuit.Sound.FOOTSTEP,walking),
            "Emote and walk have the same acoustic budget");
        invalid(()->GhostSoundProfile.chat(0));
        invalid(()->GhostSoundProfile.chat(81));
        invalid(()->new GhostSoundProfile.Profile(0,10));
        invalid(()->new GhostSoundProfile.Profile(80,61));
        ok(HunterEmote.fromCommand(" /WaVe ")==HunterEmote.WAVE
            &&HunterEmote.fromCommand("/laugh")==HunterEmote.LAUGH,
            "Slash gestures resolve to explicitly authorized actions");
        ok(HunterEmote.fromCommand("/unknown")==null,
            "Unknown slash text stays in the normal text chat path");
        var signal=new LanGhostHearing.Event(LanGhostPursuit.Sound.CHAT,
            LanGhostHearing.Direction.LEFT,LanGhostHearing.Strength.NEAR,50,longText);
        String frame=signal.frame();
        ok(frame.split("\\|",-1).length==7
            &&LanGhostHearing.decode(frame).equals(signal),
            "Variable-length chat has a bounded seven-field cue and roundtrip");
        var old=new LanGhostHearing.Event(LanGhostPursuit.Sound.VOICE,
            LanGhostHearing.Direction.RIGHT,LanGhostHearing.Strength.FAINT,51);
        ok(old.frame().split("\\|",-1).length==5
            &&LanGhostHearing.decode(old.frame()).equals(old),
            "Existing constant voice cues retain their five-field codec");
        invalid(()->LanGhostHearing.decode("GHEAR|CHAT|LEFT|NEAR|50|101|12"));
        invalid(()->LanGhostHearing.decode("GHEAR|CHAT|LEFT|NEAR|50|70|80"));
        ok(!frame.contains("hello")&&!frame.contains("H1|"),
            "Ghost sound packets contain no chat text or sender identity");
        System.out.println("PASS: movement/emotes/chat/voice event strengths, durations, bounds and privacy");
    }
}
