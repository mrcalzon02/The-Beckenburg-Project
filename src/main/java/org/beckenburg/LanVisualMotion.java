package org.beckenburg;

/**
 * Presentation-only first-person motion between the host's 10 Hz world steps.
 * Never modifies an authoritative body, sends extra movement, or relaxes collision.
 * This class is confined to the Swing event thread by its owning panel.
 */
final class LanVisualMotion {
    record Pose(double x, double y, double z) {
        Pose {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
                throw new IllegalArgumentException("nonfinite visual pose");
        }
    }

    @FunctionalInterface interface Collision {
        /** Null means that the predicted viewpoint is not locally traversable. */
        Pose resolve(Pose authoritative, double x, double z);
    }

    private static final double STEP_SECONDS = .110;
    private static final double DEFAULT_STEP_METRES = .31;
    private static final double TELEPORT_METRES = 1.20;
    // A held W key rotates with mouse yaw. Treating a routine 14-degree
    // heading change as a new movement epoch repeatedly re-anchors the
    // prediction to zero and makes turning while walking visibly staccato.
    // A near-right-angle input change (including W -> A/D or reversal)
    // still resets the visual epoch; the host continues to own all motion.
    private static final double SHARP_DIRECTION_CHANGE_DOT = .20;
    private static final long STOP_HOLD_NS = 130_000_000L;
    private Pose authoritative, displayed, stopPose;
    private long lastPositionNs, intentStartNs, stopUntilNs;
    private double stepMetres = DEFAULT_STEP_METRES;
    private double observedStepSeconds = STEP_SECONDS;
    private double priorDirectionX, priorDirectionZ;
    private boolean previouslyMoving;
    // Collision sweeps are expensive in authored worlds. Each render interpolates
    // within a cached host-pose/heading segment, rather than re-sweeping geometry.
    private Pose sweptFrom, sweptEnd;
    private double sweptDx, sweptDz, sweptStep;

    void invalidateCollision() { sweptFrom = sweptEnd = null; }

    void reset() {
        authoritative = displayed = stopPose = null;
        lastPositionNs = intentStartNs = stopUntilNs = 0;
        stepMetres = DEFAULT_STEP_METRES;
        observedStepSeconds = STEP_SECONDS;
        priorDirectionX = priorDirectionZ = 0;
        previouslyMoving = false;
        invalidateCollision();
    }

    void accept(Pose next, long nowNs) {
        if (next == null) throw new IllegalArgumentException("missing host pose");
        if (authoritative == null ||
                Math.hypot(next.x() - authoritative.x(), next.z() - authoritative.z()) > TELEPORT_METRES ||
                Math.abs(next.y() - authoritative.y()) > .85) {
            authoritative = displayed = next;
            lastPositionNs = nowNs;
            stopUntilNs = 0;
            invalidateCollision();
            return;
        }
        double travelled = Math.hypot(next.x() - authoritative.x(), next.z() - authoritative.z());
        if (travelled > .002 || Math.abs(next.y() - authoritative.y()) > .002) {
            // One accepted host movement is a distance budget, not an FPS-derived velocity.
            // Learn custom walk/sprint/crouch speed without changing the host's 10 Hz rules.
            if (travelled >= .012 && travelled < .8) {
                stepMetres = travelled;
                if (lastPositionNs != 0) {
                    observedStepSeconds = Math.max(.016, Math.min(.110,
                        (nowNs - lastPositionNs) / 1e9));
                }
            }
            authoritative = next;
            lastPositionNs = nowNs;
            stopUntilNs = 0;
            invalidateCollision();
        }
        // Duplicated BODY/GPOSE packets from a TURN or host broadcast must not
        // restart the prediction clock and visibly freeze locomotion.
    }

    /**
     * Move only the drawn viewpoint, at render cadence. At most one 110 ms
     * movement quantum is shown ahead of the most recent approved pose;
     * collision is resolved against that approved pose, never against a
     * chain of speculative steps. A delayed/rejected host cannot let the
     * viewpoint run through a building or race indefinitely across the map.
     */
    Pose sample(long nowNs, double elapsedSeconds, double directionX,
                double directionZ, Collision collision) {
        if (authoritative == null) return null;
        double length = Math.hypot(directionX, directionZ);
        boolean moving = Double.isFinite(length) && length > 1e-6;
        if (moving) { directionX /= length; directionZ /= length; }
        if (moving && (!previouslyMoving ||
                directionX * priorDirectionX + directionZ * priorDirectionZ < SHARP_DIRECTION_CHANGE_DOT))
            intentStartNs = nowNs;
        if (!moving && previouslyMoving) {
            stopPose = displayed;
            stopUntilNs = nowNs + STOP_HOLD_NS;
        }
        previouslyMoving = moving;
        priorDirectionX = moving ? directionX : 0;
        priorDirectionZ = moving ? directionZ : 0;

        Pose target = authoritative;
        if (moving) {
            long anchor = Math.max(lastPositionNs, intentStartNs);
            double seconds = Math.max(0, Math.min(.126, (nowNs - anchor) / 1e9));
            double fraction = Math.min(1, seconds / observedStepSeconds);
            if (fraction > 0 && collision != null) {
                // Full-path collision is validated only when the host changes
                // pose, the input heading changes or a world barrier moves.
                // Intermediate rendered poses lie inside that valid segment.
                if (sweptFrom != authoritative ||
                        directionX * sweptDx + directionZ * sweptDz < .997 ||
                        stepMetres != sweptStep) {
                    sweptFrom = authoritative;
                    sweptDx = directionX;
                    sweptDz = directionZ;
                    sweptStep = stepMetres;
                    sweptEnd = collision.resolve(authoritative,
                        authoritative.x() + directionX * stepMetres,
                        authoritative.z() + directionZ * stepMetres);
                }
                if (sweptEnd != null) {
                    target = new Pose(
                        authoritative.x() + (sweptEnd.x() - authoritative.x()) * fraction,
                        authoritative.y() + (sweptEnd.y() - authoritative.y()) * fraction,
                        authoritative.z() + (sweptEnd.z() - authoritative.z()) * fraction);
                }
            }
        } else if (nowNs < stopUntilNs && stopPose != null) {
            target = stopPose;
        }
        double dt = Math.max(0, Math.min(.05, elapsedSeconds));
        double blend = 1 - Math.exp(-42 * dt);
        displayed = new Pose(
            displayed.x() + (target.x() - displayed.x()) * blend,
            displayed.y() + (target.y() - displayed.y()) * blend,
            displayed.z() + (target.z() - displayed.z()) * blend);
        return displayed;
    }

    Pose authoritative() { return authoritative; }
}
