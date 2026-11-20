package org.beckenburg;

import java.util.Objects;
import org.beckenburg.LevelDefinition.Vec;

/**
 * Speculative CAMERA collision only. The real hunter body and all authoritative
 * movement remain on the unchanged host solver. Sweeps use that same geometry.
 */
final class LanCameraCollision {
    private LanCameraCollision(){}

    @FunctionalInterface
    interface Sweep {
        /** Return null when the host-equivalent capsule sweep is blocked. */
        Vec resolve(Vec from,double toX,double toZ);
    }

    static LanVisualMotion.Pose resolve(HouseMap map,LanVisualMotion.Pose from,
                                        double targetX,double targetZ,double height){
        Objects.requireNonNull(map);
        return resolve(from,targetX,targetZ,
            (start,x,z)->map.resolveMove(start,x,z,height));
    }

    static LanVisualMotion.Pose resolve(LanVisualMotion.Pose from,
                                        double targetX,double targetZ,Sweep sweep){
        Objects.requireNonNull(from);
        Objects.requireNonNull(sweep);
        var start=new Vec(from.x(),from.y(),from.z());
        // Always check the complete swept path before displaying any motion.
        var destination=sweep.resolve(start,targetX,targetZ);
        if(destination==null)return null;

        // The host's independent-axis corner gate matters only for genuinely
        // diagonal movement. With one axis unchanged, both additional sweeps
        // were repeated full or zero-length collision queries against heavy
        // authored scenery, wasting render-thread time at every host pose.
        if(Math.abs(targetX-from.x())>1e-10&&Math.abs(targetZ-from.z())>1e-10
                &&(sweep.resolve(start,targetX,from.z())==null
                ||sweep.resolve(start,from.x(),targetZ)==null))
            return null;
        return new LanVisualMotion.Pose(destination.x(),destination.y(),destination.z());
    }
}
