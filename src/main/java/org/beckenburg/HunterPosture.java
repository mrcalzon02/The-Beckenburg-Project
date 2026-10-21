package org.beckenburg;

/** Shared physical hunter dimensions; feet never move when posture changes. */
public final class HunterPosture {
    private HunterPosture(){}
    public static final double STANDING_HEIGHT=1.78,CROUCH_HEIGHT=1.05;
    public static final double STANDING_EYE=1.62,CROUCH_EYE=.92,CROUCH_SPEED=.48;
    public static double height(boolean crouched){return crouched?CROUCH_HEIGHT:STANDING_HEIGHT;}
    public static double eye(boolean crouched){return crouched?CROUCH_EYE:STANDING_EYE;}
    public static double speed(boolean crouched){return crouched?CROUCH_SPEED:1;}
    public static boolean fits(HouseMap map,LevelDefinition.Vec feet,boolean crouched){
        return map.traversable(feet.x(),feet.y(),feet.z(),height(crouched));
    }
}
