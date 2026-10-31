package org.beckenburg;

/** Host-only Spirit Box signal routing. Real source events, never random proof.
 * Audible phrases are presentation and never expose the ghost's identity.
 */
public final class SpiritBoxResponses {
    private SpiritBoxResponses(){}

    public enum Phrase {
        LEAVE("Leave.",LanSound.Cue.SPIRIT_BOX_LEAVE),
        HELP("Help me.",LanSound.Cue.SPIRIT_BOX_HELP),
        HERE("I'm here.",LanSound.Cue.SPIRIT_BOX_HERE),
        OUT("Get out.",LanSound.Cue.SPIRIT_BOX_OUT);
        public final String words;
        public final LanSound.Cue cue;
        Phrase(String words,LanSound.Cue cue){this.words=words;this.cue=cue;}
    }

    /** A successful measurement must correspond to a fresh, actual host-
     * authorized vocal action and be taken at the equipped tool's real reach.
     * Repeating an earlier event is static only, not another claimed reply. */
    public static boolean qualifies(int hunterRoom,int ghostRoom,
            int vocalEventRoom,int eventTick,int currentTick,
            boolean vocalAction,boolean allowed,double distance,double reach,
            int previousReplyEventTick) {
        return hunterRoom>=0 && hunterRoom==ghostRoom
            &&hunterRoom==vocalEventRoom
            &&eventTick>=0 &&currentTick>=eventTick
            &&currentTick-eventTick<=45 &&eventTick>previousReplyEventTick
            &&vocalAction&&allowed
            &&Double.isFinite(distance)&&Double.isFinite(reach)
            &&reach>0&&distance<=reach;
    }

    /** Stable presentation variety, never one voice/profile per ghost type. */
    public static Phrase choose(int eventTick,int hunterSeat){
        if(eventTick<0||hunterSeat<0||hunterSeat>=6)
            throw new IllegalArgumentException("invalid Spirit Box event");
        var phrases=Phrase.values();
        return phrases[Math.floorMod(eventTick*7+hunterSeat*3,phrases.length)];
    }

    public static String observation(Phrase phrase){
        return phrase==null?"Radio static; no intelligible voice":
            "Vocal fragment heard: \""+phrase.words+"\"";
    }
}
