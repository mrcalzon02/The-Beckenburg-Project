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
    /** Chat size affects one architectural loss unit at most. */
    public static int acousticBudget(LanGhostPursuit.Sound sound,Profile profile){
        Objects.requireNonNull(profile);int base=Objects.requireNonNull(sound).budget;
        if(sound!=LanGhostPursuit.Sound.CHAT)return base;
        return Math.max(0,base+(profile.intensity()>=75?1:profile.intensity()<42?-1:0));
    }
}
