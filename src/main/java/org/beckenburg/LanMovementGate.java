package org.beckenburg;

/** One eligibility contract for multiplayer movement input and presentation.
 * No world state is modified here; the authoritative host owns every step. */
final class LanMovementGate {
    private LanMovementGate(){}
    static boolean enabled(boolean showing,boolean displayable,boolean focused,
                           boolean hasHostPose,boolean alive,boolean roundActive,
                           boolean noBlockingUi){
        return showing&&displayable&&focused&&hasHostPose&&alive
            &&roundActive&&noBlockingUi;
    }
}
