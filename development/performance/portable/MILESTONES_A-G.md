# Portable milestones A–G: performance, renderer and load modernization

**Portable method, edition 1 — 2026-10-26.** Applicable to 3D, 2D, UI-focused and XR projects *after* identifying which components they own. Factorio game-engine internals, browser compositor internals and drivers are typically outside a mod/app's control.

## Six-question preflight

Record: (1) explicitly authorized task; (2) project and file scope; (3) authoritative repository/main SHA and existing assets, gameplay, tests and hardware; (4) fallback if optimization fails; (5) whether this action is still relevant to current code; (6) exact evidence that code, test, push and downstream behavior succeeded. A complaint, diagnosis or idea alone is not authorization to edit, run or publish a different project.

## A: Measurement and continuous reference routes

Capture **one real persistent engine/renderer instance**, not just disconnected sample frames. Measure cold/warm load, simulation, scene build, geometry, uploads, lighting/shadows, rendering, GPU synchronization, readback, UI composition, input latency, memory/GC, scene disposal and asset I/O as independently as the engine permits. Record hardware/OS/GPU/driver/runtime, target resolution/preset, sample count, warmup, fixed camera/input trace, p50/p95/p99, captures, SHA and workflow ID. Explicitly distinguish automated viewpoint jumps from physically validated player navigation. Measure a test *without* changing target behavior first.

Software OpenGL/CPU CI proves code path and functional regressions, **not** dedicated-GPU FPS. GL-worker wall time is not a hardware GPU timer-query. Never impose machine-dependent FPS thresholds on shared CI runners. Correlate on real target devices using repeated comparable trials.

**Gate:** independently replayable route and baseline artifact, no ungrounded hardware performance claim.

## B: Asset and scene residency

Inventory immutable topology, shared templates, material/texture resources, actor/state overlays, animated data and genuinely live textures. Separate CPU preparation from GPU/upload or engine-owned resources. Identity needs scene generation, asset key, spatial chunk, resource handle, geometry revision, lighting revision and lifecycle owner. Context-thread ownership governs GPU allocate/upload/free. Switching scenes, resize and GPU context loss cannot reuse stale handles. Bounded working-set eviction must not thrash.

A synthetic VBO test validates an API; a representative authored level and gameplay round validate integration. A hash or array-equality cache does not avoid rebuilding source vertex arrays.

**Gate:** camera-only movement creates no unnecessary static regeneration/upload, and scene/material revisions refresh correctly with bounded memory and safe fallback.

## C: Conservative visibility, relevance and culling

Build spatial and/or portal candidates from authored geometry, then prove rejection conservatively. Account for multi-floor layouts, doors, windows, stairs, exterior sightlines, thin occluders, open roofs, actors crossing boundaries and transparent overlays. Candidate reachability is **not** occlusion proof; preserve uncertainty by drawing extra geometry. The visibility subsystem must not change host-authoritative game logic or reveal/hide ghost identities based on network side channels.

For 2D games this gate may mean viewport clipping and avoiding redraws outside the board; do not invent 3D portal graphs.

**Gate:** no missing visual geometry, sprites, interaction targets or incorrect bounds across closed/open portal states and representative cameras; diagnostic comparisons show conservative losses only.

## D: Static/dynamic effects and local invalidation

Lighting is a key example, but the general rule is **separate immutable source data from changing derived data**. Distinguish light/source switch revisions, door/occluder topology, animated cosmetic effects, camera LOD, sensor/evidence events and active tools. Maintain a dependency or influence map so a changed source does not globally invalidate unrelated chunks; use conservative broader invalidation when topology is uncertain. Preserve previously valid results while async noncritical updates compute; event-critical output must respond at its intended tick.

Do not bake camera-dependent sampling into a resource expected to survive camera turns. Preserve opaque wall and ceiling shadow blocking and source-normal behavior.

**Gate:** warm unchanged frames retain derived data, localized changes touch relevant regions only, global occlusion changes invalidate safely, real visual/gameplay light behavior remains accurate.

## E: Rendering/compositing and data movement

Measure GPU readback stalls, depth and color transfer, CPU/GPU synchronization, texture updates, UI repaints and allocations before changing them. Migrate effects, translucent sprites, overlays and postprocessing individually; preserve layering, depth, native glyph text, evidence and input behavior. Replace synchronous readbacks only with explicit ownership/fence and loss recovery. 2D targets may focus on tile/board layout, drag input and controlled dirty rectangles instead of GL pixel copies.

**Gate:** equivalent visible output and interaction, measured transfer work avoided, correct soft/hard fallback.

## F: Prepared loading and start-of-level readiness

Author an explicit state machine: select level -> resolve approved assets -> decode -> prepare geometry/state -> upload on graphics thread -> warm actual shaders/pipelines -> validate first complete frame -> play approved camera flyover -> hand over responsive control. Support cancellation, progress and a durable crash log. Avoid arbitrary wait/sleep masquerading as readiness. Cache programs only when invalidation accounts for source, driver and runtime.

**Gate:** real installed build reaches controllable round without hang or premature intro, after a measured predictable readiness path.

## G: Whole-product acceptance and rollout

Test release-format product on target hardware and software, not merely a compiled package: full navigation and input, core progression, evidence/sensors/physics, multiplayer, editor (if relevant), every approved level, low-end presets, resizing, multiple rounds, restart/update, memory stability and crash recovery. Compare both baseline and candidate under identical conditions. Maintain per-environment acceptance ledger; require real hardware 30 FPS only where explicitly scoped, never generalize across every project.

**Gate:** accepted gameplay/operability and performance measurements tied to exact release commit and artifact checksum. Do not block a release on self-invented cosmetic screenshot comparisons.

## Standard invalidation matrix (adapt to ownership)

| Event | Recalculate | Keep stable |
| --- | --- | --- |
| Camera change | projections and visibility | world asset geometry |
| Door/portal change | portal state and dependent visibility/lighting | unrelated chunks |
| Light switch | influenced lighting state | structural geometry |
| Moving actor/evidence | affected dynamic instance and effects | static world |
| Live material/video | its texture content | unrelated geometry and textures |
| Level editor edit | affected asset, spatial graph and dependent caches | other independent areas |
| Resize/device mode change | targets and projection | authoritative scene data |
| Scene switch/device loss | handle ownership and GPU resources | reusable decoded templates where safe |

## Delivery and rollback rules

- Each step has a short design, source diff, before/after evidence, functional checks, readback verified remote SHA and rollback route.
- CI diagnostics and captures are evidence, **not** automatically GPU performance gains.
- Preserve functionality and original artwork. No unrelated refactors, fake assets, automated test weakening or force pushes.
- Keep concurrency safe: read current main and in-flight PRs, work on separate branches for incompatible code, use only forward-moving verified refs.
- A failed write/test is a pending defect, never progress credited as complete. Never disable recurring work solely because write access fails.
