# Parent regression recovery: hunter gait sampling

Parent source: `c93d141d1d9fa0e9fa1bef55485d182553f70ff0`.
Full run42426485514 completed with **364 passed, 1 failed, 0 skipped, 365 manifest**.
The workflow wrapper is green because that step is advisory; full acceptance is
rejected. The raw log preserves the actual missing WALK_B assertion.

The test let the host advance in wall time while software renders/captures took
seconds. The real animation tracker deliberately resets stale gait after a
one-second observation gap. Each accepted .31m step could therefore restart at
WALK_A, never accumulating enough contiguous movement for WALK_B. Earlier green
runs do not eliminate this fixture error.

The corrected test stops the scheduled clock after its in-flight tick completes
and invokes the actual host tick method exactly once per simulation step. It
retains actual TCP input, host movement validation, normal body/posture packets,
live panel packet handling, renderer and all original assertions for all three
roles. A deliberate 1.1-second wall delay reproduces slow capture cost without
inventing simulation time or missing observations. No positions, animation poses,
panel clocks or network frames are injected after the initial camera/body fixture.
No runtime source, animation threshold, deadline or assertion is relaxed.

Two additional fixture mistakes surfaced and are preserved as rejected logs:
STEP(0,0) is invalid, so stopping now means no movement input followed by a real
stationary periodic snapshot. STATE precedes that world batch, so the helper
waits for its final actual hunter posture row rather than rendering old bodies.

Final actual four-state gameplay checks pass at256MB for cleric, observer and
demonologist, including scene reset and departure. The existing animation unit
test now explicitly verifies stale-gap reset followed by contiguous A/B stride
recovery and passes. Twelve actual software diagnostic PNGs are retained. Two
representative frames were viewed; no new artwork or general aesthetic approval
is claimed. Actual GPU gait and physical hardware performance are not certified.

The corrected source still needs a new full365 run. Parent native42426323930 and
GPU42424143713 passed within their source scopes; unchanged runtime/render/assets
remain unchanged here. Do not merge PR82 on its failed full summary. PR84's own
full368 run42427846432 was still live when this checkpoint was prepared; preserve
that handle. Its native42427846331 and GPU42427852522 completed successfully.

Separate new work remains recoverable on map/house-final-review at02f83c85 (180
matched runtime views,30inspected sheets) and map/house-default at8994d699 (fresh
default candidate with actual saved-legacy/four-TCP tests). Neither is accepted
or merged. Continue those after the recovery gate, then all29map packets and
remaining unfinished maps. No round-duration test was added.
