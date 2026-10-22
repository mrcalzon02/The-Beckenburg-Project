package org.beckenburg;

import java.util.Arrays;
import java.util.Objects;

/**
 * One deterministic skill authority for embodied human and AI investigators.
 * Abilities are class-locked, cooldown-driven and cannot reveal the hidden
 * ghost identity, true anchor or evidence quota.
 */
public final class HunterDiscipline {
    public enum Ability {
        COMFORT(CharacterVisuals.Hunter.CLERIC,140,"Comfort"),
        HALLOW(CharacterVisuals.Hunter.CLERIC,280,"Hallow"),
        SURVEY(CharacterVisuals.Hunter.OBSERVER,110,"Survey"),
        FOCUS(CharacterVisuals.Hunter.OBSERVER,260,"Focus"),
        SIGIL(CharacterVisuals.Hunter.DEMONOLOGIST,250,"Binding Sigil"),
        INVOKE(CharacterVisuals.Hunter.DEMONOLOGIST,320,"Invocation");
        public final CharacterVisuals.Hunter discipline;
        public final int cooldown;
        public final String label;
        Ability(CharacterVisuals.Hunter discipline,int cooldown,String label){
            this.discipline=discipline;this.cooldown=cooldown;this.label=label;
        }
    }
    public record Result(boolean accepted,String message) { }
    private final int[][] last;
    private final int[] hallowedUntil,focusedUntil,sigilUntil;
    public HunterDiscipline(int actors){
        if(actors<1||actors>6)throw new IllegalArgumentException("actors");
        last=new int[actors][Ability.values().length];
        for(int[] row:last)Arrays.fill(row,-100000);
        hallowedUntil=new int[actors];focusedUntil=new int[actors];sigilUntil=new int[actors];
        Arrays.fill(hallowedUntil,-1);Arrays.fill(focusedUntil,-1);Arrays.fill(sigilUntil,-1);
    }
    public static Ability primary(CharacterVisuals.Hunter role){
        return switch(Objects.requireNonNull(role)){
            case CLERIC -> Ability.COMFORT;
            case OBSERVER -> Ability.SURVEY;
            case DEMONOLOGIST -> Ability.SIGIL;
        };
    }
    public static Ability secondary(CharacterVisuals.Hunter role){
        return switch(Objects.requireNonNull(role)){
            case CLERIC -> Ability.HALLOW;
            case OBSERVER -> Ability.FOCUS;
            case DEMONOLOGIST -> Ability.INVOKE;
        };
    }
    private void actor(int seat){
        if(seat<0||seat>=last.length)throw new IllegalArgumentException("skill actor");
    }
    public int remaining(int seat,Ability ability,int tick){
        actor(seat);Objects.requireNonNull(ability);
        return Math.max(0,last[seat][ability.ordinal()]+ability.cooldown-tick);
    }
    public Result activate(int seat,CharacterVisuals.Hunter discipline,Ability ability,int tick){
        actor(seat);Objects.requireNonNull(discipline);Objects.requireNonNull(ability);
        if(tick<0)throw new IllegalArgumentException("tick");
        if(ability.discipline!=discipline)return new Result(false,"ability belongs to "+ability.discipline);
        int waiting=remaining(seat,ability,tick);
        if(waiting>0)return new Result(false,ability.label+" recharging "+(waiting+9)/10+"s");
        last[seat][ability.ordinal()]=tick;
        switch(ability){
            case HALLOW -> hallowedUntil[seat]=tick+100;
            case FOCUS -> focusedUntil[seat]=tick+130;
            case SIGIL -> sigilUntil[seat]=tick+95;
            default -> {}
        }
        return new Result(true,ability.label+" activated");
    }
    /** Hallow can shield nearby allies, but only after a real Cleric succeeds. */
    public void shareHallow(int seat,int tick){
        actor(seat);
        hallowedUntil[seat]=Math.max(hallowedUntil[seat],tick+100);
    }
    /** Host/solo authority may extend a sigil to an embodied companion's ally. */
    public void shareSigil(int seat,int tick){
        actor(seat);
        sigilUntil[seat]=Math.max(sigilUntil[seat],tick+95);
    }
    public boolean focused(int seat,int tick){actor(seat);return tick<=focusedUntil[seat];}
    public boolean hallowed(int seat,int tick){actor(seat);return tick<=hallowedUntil[seat];}
    public boolean sigilActive(int seat,int tick){actor(seat);return tick<=sigilUntil[seat];}
    /** Multipliers are intentionally bounded: protection is never invulnerability. */
    public int fear(int seat,CharacterVisuals.Hunter role,int tick,int amount){
        actor(seat);if(amount<0)throw new IllegalArgumentException("negative fear");
        int percent=switch(role){
            case CLERIC -> 85;
            case OBSERVER -> 95;
            case DEMONOLOGIST -> 100;
        };
        if(focused(seat,tick))percent=Math.min(percent,55);
        if(hallowed(seat,tick))percent=Math.min(percent,55);
        if(sigilActive(seat,tick))percent=Math.min(percent,65);
        return amount==0?0:Math.max(1,(amount*percent+99)/100);
    }
    public int harm(int seat,CharacterVisuals.Hunter role,int tick,int amount){
        actor(seat);if(amount<0)throw new IllegalArgumentException("negative harm");
        int percent=role==CharacterVisuals.Hunter.CLERIC?90:100;
        if(hallowed(seat,tick))percent=Math.min(percent,55);
        if(sigilActive(seat,tick))percent=Math.min(percent,60);
        return amount==0?0:Math.max(1,(amount*percent+99)/100);
    }
}
