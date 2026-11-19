package org.beckenburg;

import java.util.Arrays;

/** Host-side admission of one genuine footstep stimulus per simulation tick.
 * The 50Hz movement protocol must not multiply ghost acoustic/AI events.
 * No player displacement, sound range, AI tick or evidence rule changes.
 */
final class FootstepStimulusCadence {
    private final int[] last;
    FootstepStimulusCadence(int seats){
        if(seats<1||seats>64)throw new IllegalArgumentException("invalid seats");
        last=new int[seats];
        Arrays.fill(last,-1);
    }
    boolean accept(int seat,int tick){
        if(seat<0||seat>=last.length||tick<0)throw new IllegalArgumentException("invalid footstep");
        if(tick<=last[seat])return false;
        last[seat]=tick;
        return true;
    }
}
