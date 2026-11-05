# Beckenburg loading prebake and 3D expedition approach

## Contract

A case starts with a genuine world-space 3D aerial view, descends over the actual authored property, travels down the correctly rotated van's rear cargo ramp, crosses the cargo doorway, enters the central aisle and hands off precisely to the hunter's first-person view. All backends get a perspective render; the 2D tactical-map fallback is forbidden. The loading transition never advances player movement, ghosts, hidden evidence or multiplayer authority.

## Stage 1 — implemented

- A dedicated low-resolution (180–300p) 3D software renderer runs off the Swing event thread, even on systems lacking OpenGL. It never competes for the active GPU context.
- The real gameplay SceneRenderer makes up to three low-resolution camera prewarm passes before the final initial gameplay frame. Pass starts are checked against a 5-second budget, and its camera and framebuffer dimensions are then restored. The third pass targets the actual first authored interior when the five-second budget permits. This admits static shell chunks and nearby scenery/light visibility into the game's own caches.
- The opening readiness window is bounded at seven seconds; a failed preview displays preparation status rather than the regressed map graphic. The transition can be skipped, and a failed cinematic cannot block mission access.
- An automated renderer test records actual aerial/loading, van approach, rear-door crossing, interior aisle and exact player-camera handoff PNGs plus prewarm timing and geometry statistics.
- Existing heavy scene geometry and materials are not duplicated into a separate cinematic-owned gameplay cache.

## Stage 2 — in progress: reusable loading preparation by revision

| Asset or calculation | Safe to prepare once | Must update during play |
|---|---|---|
| Geometry | Authored shell, GLB vertex/UV/normal batches, LODs, bounding volumes, terrain/road chunks | Changed/editor props, swinging doors |
| Materials | Atlas decoding, mipmaps, texture residency, material lookup and shader variants | Animation, live camera/video images, procedural effects |
| Lighting | Receiver-side static probe geometry, same-side light visibility, sorted lamp contributors | Door-dependent shadow rays, switching/flicker |
| Visibility | Portal adjacency, static potential-visibility and BVH/octree structures | Frustum and moving-occluder checks |
| Navigation | Static navmesh, ramp/stair stitching and traversal graph | Doors, obstacles and moving actors |
| Sound | Source positions, attenuation and room acoustic paths | Real-time events and door acoustic openness |
| Water/outdoors | Near-site terrain, shore/seabed tiles and geometry LOD | Dynamic water, weather and far streaming |

No full-global lighting invalidation when a door moves. Keep static rays and only invalidate exact door-dependent cached receivers. Do not precompute every possible set of door states; the combinations explode exponentially.

## Stage 3 — proposed: GPU upload and persistence

CPU work may happen in workers; GPU resource creation must happen on the actual OpenGL owner thread. Version every prepared resource by level, editor, material and lighting revision. Upload the van, immediate exterior and first accessible room first, then expand without exceeding the established resident-world 128 MiB budget or prepared-scene 120,000-triangle limit. Track reuse, cache misses, submitted bytes, heap/VRAM pressure and eviction rather than pretending all geometry can stay resident.

## Verification and performance targets

Record true loading elapsed time, additional prewarm passes, generated shell chunks, submitted GPU bytes, geometry hits, light-ray counts and initial gameplay frame time. Compare cold and warmed p50/p95/p99, maintain a 30 FPS target (33.3ms budget) and run real door-open/close profiler checks. The earlier door-toggle submission-recorder p95 of 105.8ms is a separate open runtime problem; a loading prewarm alone does not fix a door-triggered cache invalidation.

### Implemented Stage 2 slice: existing game-renderer immutable geometry (2026-11-04)

- `InvestigationScenePrewarm` calls `SceneRenderer.prepareStaticScene(map)` before the first low-resolution camera pass on **the same renderer instance that gameplay uses**. This constructs/reuses the authored House shell triangles, chunk bounds, material face groups, and scenery instance index. No second cinematic-owned gameplay cache or renderer is introduced.
- Preparation keys include the authored `level().revision()` and hiding-density revision. Exact map identity is also required. A changing door state is intentionally **not** part of the immutable preparation key; door-dependent physical lighting is still checked in the existing receiver-cell lighting system at runtime.
- The resident scene owner now detects changes to those authored revision keys even when the map object is unchanged. It rejects old lighting owners/readbacks and assigns new GPU scene identity for an editor/authoring revision, **not** for each door movement.
- `LOADING STATIC PREBAKE` diagnostics emit actual preparation duration, scene revision, staged shell chunks/faces, staged scenery instance count, warm-cache reuse flags and low-resolution camera passes. The 3D loading proof records these in `verification.txt`; `StaticScenePrebakeTest` checks warm reuse, a real door toggle, revision changes, distinct maps, cancellation and unchanged camera/player state. The GPU acceptance workflow runs this test.
- This is **CPU-side immutable-scene staging and reuse**, not finished GPU upload scheduling, texture residency, navmesh prebaking, or static/acoustic precomputation across every map backend. Native-site and Castle-specific preparation still follow their existing owner-specific paths. Never report a GPU upload merely because a CPU batch exists.

Stage 3 remains planned. The 5-second prewarm budget limits the start of expensive camera passes, but the initial construction itself is not preemptible once begun; wall-clock and latency evidence must be reviewed before claiming a strict five-second ceiling. Resident GPU uploads remain owned by the actual OpenGL thread. The separate real door-toggle p95/p99 regression remains open.

## Measured runtime improvement (2026-11-04)

The House door submission-recorder A/B on Windows run 41963610654 kept the same authored map and 24 open/close toggles. A bounded 150,000-entry alternate stationary ray archive, now applied to House shell material batches as well as retained props, reduced cumulative shadow rays from 163,089 to 111,503 (31.6%). Recorded toggle p50 changed from 123.6 ms to 93.1 ms and p95 from 160.3 ms to 125.4 ms, while p99 grew from 361.9 ms to 393.5 ms. These results are **not OpenGL frame times** and do not prove worst-case 30 FPS. The initial unbaked door transition and resident VBO updates remain a priority.

Static shell material lists and material-face grouping are prepared with the scene rather than recreated every frame; region revisions are cached by the exact lighting generation. A true door change still revalidates affected light cells, and any unrelated portal or global edit discards the door-state archive instead of reusing stale rays. Disabled A/B operation is available with the Java property `beckenburg.lighting.doorShadowArchive.disabled=true`.
