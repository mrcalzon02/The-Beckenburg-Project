# Crouch camera smoothing

Baseline: merged resident checkpoint `55bea511eb8199ddebdc21b615a8e96a46f59946`, PR #61. The user approved uploading and merging the prior source, art and logs; remote main readback matched this SHA.

Shared critically damped eye-height motion eases crouch/stand and reversals in Castle, solo and LAN. Physical posture, movement speed, standing clearance and support elevation remain immediate and authoritative. Castle and solo ignore held-Ctrl autorepeat. LAN renders additional camera-only frames at 33ms during the transition while keeping its existing 110ms action timer. Its new timer is stopped on close, and camera state resets on a site change. Solo evidence uses physical support elevation independently of the eased camera.

Validation logs cover frame-rate independence, monotonic convergence, fast reversal continuity, exact settling, feet stability through accepted TCP posture changes, unchanged action cadence, blocked standing under low cover, sprint exclusion, existing hunter animation and Castle interaction/floor contact. `exact-*` logs refer to the isolated source boundary excluding unrelated pending sanctuary changes.

The prior NPC merge triggered renderer `41871105610` and installer `41871106076`; these were still running when first inspected. Local test success does not imply an updated installed copy or completed installer CI.

Final isolated Java 17 main-source compilation and all seven exact-source checks pass: camera curve, low-cover posture, real TCP camera/feet/cadence, live hunter gameplay, Castle interaction, sprite floor contact and synchronous photographic projection.
