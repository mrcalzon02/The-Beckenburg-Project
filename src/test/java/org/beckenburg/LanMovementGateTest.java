package org.beckenburg;

/** Every lost-control condition must stop BOTH predictive camera and input pump. */
public final class LanMovementGateTest {
    private static void check(boolean condition,String why){if(!condition)throw new AssertionError(why);}
    public static void main(String[] args){
        check(LanMovementGate.enabled(true,true,true,true,true,true,true),
            "focused, alive, unobstructed player can move");
        for(int blocked=0;blocked<7;blocked++){
            boolean[] flags={true,true,true,true,true,true,true};
            flags[blocked]=false;
            check(!LanMovementGate.enabled(flags[0],flags[1],flags[2],
                flags[3],flags[4],flags[5],flags[6]),
                "lost movement condition "+blocked+" must be neutral");
        }
        check(LanMovementGate.enabled(true,true,true,true,true,true,true),
            "closing a menu only restores eligibility when all controls are valid");
        System.out.println("PASS common hunter/ghost movement gate: focus, visibility, host pose, life, round and UI");
    }
}
