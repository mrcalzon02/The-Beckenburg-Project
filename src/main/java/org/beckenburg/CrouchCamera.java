package org.beckenburg;

/** Cosmetic critically damped eye motion; physical feet/posture remain authoritative. */
public final class CrouchCamera {
    private static final double RESPONSE=20;
    private double eye=HunterPosture.STANDING_EYE,velocity;
    public double eye(){return eye;}
    public boolean settled(boolean crouched){
        return Math.abs(eye-HunterPosture.eye(crouched))<.0001&&Math.abs(velocity)<.001;
    }
    public void reset(boolean crouched){eye=HunterPosture.eye(crouched);velocity=0;}
    public double advance(boolean crouched,double seconds){
        if(!Double.isFinite(seconds)||seconds<=0)return eye;
        double target=HunterPosture.eye(crouched),delta=eye-target;
        double c=velocity+RESPONSE*delta,decay=Math.exp(-RESPONSE*seconds);
        double next=target+(delta+c*seconds)*decay;
        velocity=(velocity-RESPONSE*c*seconds)*decay;
        eye=Math.max(HunterPosture.CROUCH_EYE,Math.min(HunterPosture.STANDING_EYE,next));
        if(eye!=next)velocity=0;
        if(settled(crouched)){eye=target;velocity=0;}
        return eye;
    }
}
