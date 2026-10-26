# Beckenburg rendering migration — milestones A through G

Authoritative engine: Java 17 / LibGDX OpenGL 3.3.
Baseline SHA: 5909f66a6314666fd5f5a6cf4e86847eb11a7314.
Scope: incremental renderer/resource architecture; no unrelated gameplay or artwork revision.
Status: A GPU profiling source accepted on Mesa run 40389641265, but still lacks a physical traversal/hardware timing; B1 prepared-scenery candidate under validation, B2–G not implemented.

## Execution environments

- Local Codex + Windows GPU: rapid capture and actual driver/frame-time measurements. This chat cannot run the user's local PC directly.
- GitHub Actions: per-SHA compile, CPU fallback, Mesa real-OpenGL smoke, images/CSV and gameplay/regression/installer verification. Mesa timings are *not* representative hardware GPU FPS.
- No cosmetic tests should block a build absent explicit approval. Functional smoke tests still block when actual rendering or gameplay is broken. Never force push.

## Existing code contracts

- SceneRenderer.scene currently builds per-frame Face lists; drawGpuFaces performs frustum tests, adaptive subdivisions, CPU fixture light samples and repacks material batches.
- GpuWorldRenderer.Frame carries full vertex arrays plus camera uniforms; OpenGlWorldRenderer owns meshes but uses Arrays.equals on newly generated arrays to decide whether to upload. This saves GPU transfer, *not* CPU preparation.
- FixtureLighting caches room/cell influence, RGB probes and LOS. Keep its correct door/circuit/ghost interference invalidation.
- OpenGlWorldRenderer compiles the existing GLSL program in create(). Shader precompilation is already partly present, not a substitute for resident geometry.
- Every frame currently glReadPixels RGBA and depth into Java arrays for the Swing/CPU effect and sprite path; do not remove until a replacement GPU compositor preserves ghost, DOTS, UV, character depth and visual correctness.
- The van CRT screen is dynamic. Its live texture must not become static through a cache optimization.
- Original GLB UVs/materials, PSX pixel snapping/affine warp, skies, weather, water, Castle and all gameplay modes must remain compatible.

## A — Profiling foundation (first implementation)

GPU smoke reuses exactly one HouseMap and one SceneRenderer across 11 authored viewpoint checkpoints (van, yard, front path, entrance, interior, upstairs, return). Each checkpoint has approach/repeated-identical/rotated frames, with occasional GPU flashlight, 33 samples total. These are scripted camera waypoints, **not collision-proven player navigation**. For each completed exact-ID framebuffer the profile records CPU scene/build/geometry/effects, lighting samples/cache/shadow rays, triangle counts, GL worker wall time for draw submission and synchronous readback/copy, and upload/reuse bytes. Save CSV, summary, three screenshots; Actions uploads artifacts and rejects missing CSV (not slow frames).

A continuation must add collision-respecting actual Hunter movement and hardware GPU timer queries; also collect GC/memory/p99. Capture the driver, resolution, preset, warmup and SHA. Compare only matching machines/paths. No strict shared-runner FPS threshold.

## B — Resident scene resources

Introduce explicit immutable PreparedScene identity keyed by map/content revision, static SceneChunk meshes by room/outdoor cell/material, shared MeshTemplate handles, separate mutable SceneState and owned GL ResidentMesh handles. Asset decode/preparation is worker-safe; GPU creation/upload/deletion stays on GL thread. Static geometry is world-space, never camera-baked. The existing Frame/Surface path remains a compatibility fallback while House architecture is migrated. Test camera movement causing zero static VBO rebuilds, edited door updates without rebuilding walls, resize, map switching and deterministic disposal.

## C — Visibility and portal traversal

Use authored level rooms, portals, doors, windows and stairwell with conservative intersection and frustum rejection; outdoor terrain uses spatial cells. Render uncertain chunks rather than incorrectly hiding visible walls or ghost/equipment sprites. Check door/window/stair/outdoor viewpoint screenshots and counts; simulation/host authority never depends on visibility results.

## D — Static and dynamic lighting separation

Prepare baseline fixture lighting and physical LOS in source-influence chunks without baking it into immutable mesh position buffers. Maintain independent revisioned lighting data, overlay dynamic flashlight, circuit switches, TV, equipment and deliberate ghost pulses. Door/geometry changes invalidate dependent light visibility only. Noncritical lighting may retain previous frame during rebuild; authoritative ghost/evidence response must appear on correct tick. No wall/ceiling shadow bleed; retain accurate fixture sample and normal responses.

## E — GPU composition

Instrument readback synchronization separately from GPU timer queries. Try buffered asynchronous readback while retaining correct CPU compositor. Migrate sprites/ghost depth/effects to GPU only with equivalence tests for four-direction art, generic-vs-signature reveal, translucent visual effects and correct native fonts. Keep recoverable software path.

## F — Loading and warmup

Case selection -> decode/materials -> spatial chunks -> static lighting -> GL uploads -> shader/texture/FBO initialization -> render verification -> cinematic overflight -> van control. Use actual readiness, not fixed sleeps. Show progress, cancellation and clear diagnostic failures. Optionally cache program binaries with driver-keyed invalidation. Keep case-start hang hardware validation open.

## G — Whole-game acceptance

House first; then Farmhouse, School, Castle and remaining maps after per-map visual/operability review. Require installed Windows GPU-first test, real solo case, TCP multiplayer, lighting/portals/editor, menu/evidence/ritual/return, memory/GC and actual hardware p50/p95/p99. Target stable 30 FPS at measured reference hardware before seeking 60 FPS. Separate loading seconds from frame latency.

## Invalidation contract

| Event | Update | Preserve |
| --- | --- | --- |
| Camera move | visibility, camera uniforms | static meshes, stationary lightmap |
| Door move | door transform, portal cells, dependent LOS | all unrelated rooms/meshes |
| Switch toggle | local lighting data | structural VBOs |
| Flashlight | dynamic GPU uniforms/overlays | fixture baseline |
| Ghost reveal | authorized dynamic sprite/effect | static scene, hidden identity |
| Equipment placement | dynamic instance/evidence | all unaffected chunks |
| Van CRT refresh | live screen texture pixels | all geometry |
| Editor wall change | local geometry, portal, collision and lighting | unrelated resources |
| Resize | FBO and projection | static scene |
| Map exit | scene resources and generation | shared templates if still referenced |

## Acceptance rules

Each milestone has its own source diff, tests, screenshot evidence and measured before/after numbers. Preserve true functional tests and packaging gates; do not add cosmetic blockers. Read back remote SHA. No phase is marked complete solely from a plan, a synthetic test or a successful build. Local hardware tests remain separate from Mesa software evidence.

## B1 incremental implementation — bounded CPU world-face preparation

SceneRenderer now retains immutable world-space Face records for visible SceneryInstances.Instance objects in a 120,000-triangle LRU. Repeated frames and camera turns reuse the same world-space records rather than transforming vertices and reconstructing triangles. Distinct HouseMap instances or changes in level revision/hiding density invalidate them. An actual Windows CPU rendering check verifies exact unchanged-frame pixels, warm reuse, camera rotation, new-map invalidation and memory budget. This is *not* a persistent OpenGL VBO scheme, and still repacks lit GPU triangles each frame. B2 must deliver stable chunk IDs, resident VBO ownership, static/dynamic updates and measured performance against milestone A.

## B1 first CI defect and repair candidate

GPU acceptance run 40393357870 failed the newly added Windows test: it demanded zero additional builds on an unchanged frame although the cache deliberately holds at most 120,000 triangles. The original pure LRU also risks thrashing when the visible working-set exceeds that budget. Correction: protect entries used in the current or immediately previous frame from eviction and skip cache admission when the entire protected working-set fills the budget. The functional test requires **all retained first-frame entries** to be reused on a repeated identical frame, stable pixels, bounded memory and map identity invalidation. It does not invent infinite memory or waive the true reuse requirement. Exact-SHA Windows/GPU acceptance pending. Installer 40393357807 correctly blocked publication after that same-SHA GPU failure; multiplayer 40393357728 passed.

## D0 — Explicit fixture and occluder light revisions (candidate)

FixtureLighting.stateRevision() now reports its existing generation for source-influence changes and increments it whenever shadow-blocking geometry (House doors/windows/hiding layout, Castle doors) invalidates cached LOS probes. Identical frames preserve the revision. It is scene-owned; callers must also key by the map/scene identity. The functional FixtureLightingRevisionTest checks unchanged warm frames, breaker blackout/recovery and door changes. This is a contract for future independent GPU lightmaps, not a completed static/dynamic GPU lighting system. Camera-dependent distance LOD still prevents production resident mesh lighting reuse until refactored.

## D1 — Spatially localized fixture revisions

FixtureLighting.regionRevision(x,y,z) now returns a stable per-influence-cell change token combined with an independently tracked conservative global occluder revision. Camera-only frames keep their tokens. A switch affecting one fixture region advances only intersecting source-influence cells; door/window/hiding/known Castle door changes advance occluderGeneration and invalidate all regions conservatively. Existing internal sample/normal caches and LOS ray behavior remain unchanged; no new shader/geometry effects, and no invalidation of network/evidence game state. Callers must key resources with the owning scene and must not reuse camera-distance lighting LOD as static. The functional revision test checks local-vs-distant fixture edits, global occlusion changes, stable frames and nonfinite argument rejection. This is a region revision interface for future D GPU lightmaps, **not** the finished D milestone.

## D2 — Scene-owned local fixture probe cache (candidate)

A separate PreparedFixtureProbeCache stores immutable RGB responses for world-space positions and exact normals. It uses D1 per-influence-cell revisions and D0 global occluder invalidation; the test requires unchanged frames to reuse probes without shadow raycasts, locally toggled circuits to refresh both nearby opposite-face normals but not distant probes, and door movement to refresh all probes conservatively. Camera LOD, ghost effects, dynamic UV, handheld lamps and portable equipment stay on the current gameplay path; this is NOT yet GPU lightmap integration, wall-side visual parity or hardware FPS acceptance. Ownership is tied to one FixtureLighting instance to prevent collision of local revision counters across scenes.
