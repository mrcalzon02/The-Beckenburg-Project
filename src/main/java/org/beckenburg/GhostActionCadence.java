package org.beckenburg;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Repeating, host-authoritative action deadline shared by human and AI ghosts.
 * It does not generate evidence directly: only the ordinary GhostDomain dispatcher
 * can apply an actual event (sound, disturbance, manifestation or signature).
 */
public final class GhostActionCadence {
    /** Host simulation runs at 10Hz: each deadline is independently rolled between 5 and 45 seconds. */
    public static final int MIN_PERIOD=50,MAX_PERIOD=450;
    private final GhostIdentity.Type species;
    private final Random random;
    private int dueTick;
    private int lastPeriod;
    private int successfulEvents;
    public GhostActionCadence(GhostIdentity.Type species){
        this(species,new Random());
    }
    /** Injectable RNG permits repeatable balance tests without predictable production rolls. */
    GhostActionCadence(GhostIdentity.Type species,Random random){
        this.species=Objects.requireNonNull(species);
        this.random=Objects.requireNonNull(random);
        dueTick=rollPeriod();
    }
    private int rollPeriod(){
        lastPeriod=MIN_PERIOD+random.nextInt(MAX_PERIOD-MIN_PERIOD+1);
        return lastPeriod;
    }
    /** The active interval; requesting it must not change the deadline. */
    public int period(){return lastPeriod;}
    public int dueTick(){return dueTick;}
    public int successfulEvents(){return successfulEvents;}
    /** DRIFT chooses a destination but cannot substitute for a haunting event. */
    public void completed(int tick,GhostDomain.Resolution result){
        if(tick<0||result==null)throw new IllegalArgumentException("invalid action result");
        if(result.applied()&&result.action()!=GhostDomain.Action.DRIFT){
            successfulEvents++;
            dueTick=tick+rollPeriod();
        }
    }
    /** Countdown does not stall on client rendering, and cannot be reset by movement. */
    public boolean due(int tick){return tick>=dueTick;}
    /** Small public selection list; never exposes private hunter coordinates. */
    public List<GhostDomain.Action> choices(GhostDomain.Observation state){
        Objects.requireNonNull(state);
        List<GhostDomain.Action> result=new ArrayList<>();
        var legal=state.legalActions();
        var favored=GhostBehavior.profile(species).favored();
        if(favored!=GhostDomain.Action.DRIFT&&legal.contains(favored))result.add(favored);
        for(var action:List.of(GhostDomain.Action.DISTURB,GhostDomain.Action.WHISPER,
                GhostDomain.Action.MANIFEST,GhostDomain.Action.SIGNATURE,
                GhostDomain.Action.STRIKE))
            if(legal.contains(action)&&!result.contains(action)&&result.size()<3)
                result.add(action);
        return List.copyOf(result);
    }
    /**
     * At the deadline, randomly select one of the currently legal events. Do not manufacture fake world evidence
     * and do not degrade to DRIFT, which could let a player evade activity forever.
     * If warded/empty of energy, wait for an actual legal event on later host ticks.
     */
    public GhostDomain.Action fallback(GhostDomain.Observation state,boolean hasStrikeVictim){
        Objects.requireNonNull(state);
        List<GhostDomain.Action> eligible=new ArrayList<>();
        for(var action:choices(state))
            if(action!=GhostDomain.Action.STRIKE||hasStrikeVictim)eligible.add(action);
        return eligible.isEmpty()?null:eligible.get(random.nextInt(eligible.size()));
    }
    public String prompt(GhostDomain.Observation state){
        Objects.requireNonNull(state);
        StringBuilder out=new StringBuilder("GPROMPT|").append(state.tick())
            .append('|').append(dueTick).append('|').append(state.room());
        for(var action:choices(state))out.append('|').append(action);
        return out.toString();
    }
}
