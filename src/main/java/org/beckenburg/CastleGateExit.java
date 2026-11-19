package org.beckenburg;

/** A deliberate walk through the open front portcullis, not a menu teleport. */
public final class CastleGateExit {
    private CastleGateExit(){}
    public static final double COUNTDOWN_SECONDS=5.0;
    public static final String GATE_ID="courtyard-main-gate";
    /** The authored apron ends at z=36; start inside it and give players room to retreat. */
    public static boolean inExitThreshold(CastleHubMap map,double x,double elevation,double z){
        // A rectangular trigger must not start an exit from a raised, buried,
        // unsupported or newly blocked editor-authored pose. Use the same
        // authoritative body clearance and support as live Castle movement.
        return map!=null&&map.doorOpen(GATE_ID)
            &&Double.isFinite(x)&&Double.isFinite(z)&&Double.isFinite(elevation)
            &&Math.abs(elevation)<=.15
            &&x>=13.75&&x<=17.25&&z>=34.7&&z<=35.8
            &&map.traversable(x,z,elevation);
    }
}
