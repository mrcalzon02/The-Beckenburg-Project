package org.beckenburg;
import java.util.Objects;
/** Host-owned bounded sound profile: no transcript, identity, or voice samples. */
public final class GhostSoundProfile {
    public static final int MAX_DURATION_TICKS=60,WALK_INTENSITY=55,VOICE_INTENSITY=70;
    private GhostSoundProfile(){}
    public record Profile(int intensity,int durationTicks){
        public Profile{
            if(intensity<1||intensity>100||durationTicks<1||durationTicks>MAX_DURATION_TICKS)
                throw new IllegalArgumentException("Invalid acoustic profile");
        }
    }
    public static Profile chat(int chars){
        if(chars<1||chars>LanChat.MAX_CHARS)throw new IllegalArgumentException("Invalid chat length");
        return new Profile(Math.min(85,30+(chars*55+79)/80),
            Math.min(46,6+(chars+1)/2));
    }
    public static Profile forSound(LanGhostPursuit.Sound sound){
        return switch(Objects.requireNonNull(sound)){
            case FOOTSTEP,EMOTE->new Profile(WALK_INTENSITY,8);
            case CHAT->chat(32);
            case VOICE->new Profile(VOICE_INTENSITY,10);
            case DOOR,EQUIPMENT->new Profile(65,12);
            case RITUAL->new Profile(85,18);
        };
    }
    /** Common host-only prioritization law for primary ghosts and Twins:
     * architectural sound loss lowers perceived strength, and old cues decay
     * by one point per host tick. Three-point hysteresis prevents ping-pong
     * between competing similar sources. Never uses private body updates.
     */
    public static int perceived(Profile profile,int attenuation){
        Objects.requireNonNull(profile);
        if(attenuation<0)throw new IllegalArgumentException("negative acoustic loss");
        return Math.max(0,profile.intensity()-12*Math.min(attenuation,9));
    }
    public static boolean supersedes(int newStrength,int oldStrength,int ageTicks){
        if(newStrength<0||newStrength>100||oldStrength< -1||oldStrength>100||ageTicks<0)
            throw new IllegalArgumentException("invalid acoustic memory comparison");
        return oldStrength<0 || newStrength>=Math.max(0,oldStrength-Math.min(100,ageTicks))+3;
    }
    /** Chat size affects one architectural loss unit at most. */
    public static int acousticBudget(LanGhostPursuit.Sound sound,Profile profile){
        Objects.requireNonNull(profile);int base=Objects.requireNonNull(sound).budget;
        if(sound!=LanGhostPursuit.Sound.CHAT)return base;
        return Math.max(0,base+(profile.intensity()>=75?1:profile.intensity()<42?-1:0));
    }
}
