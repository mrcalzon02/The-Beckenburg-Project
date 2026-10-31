# Beckenburg Terrain World System — engine and editor contract

**Owner requirements (2026-10-31):** build the human-operable terrain and loading-zone modules first, then their agentic interface. Support both finite authored sites and extremely large, unified, stitched-heightmap open worlds. Support miles of sight distance without distant people/objects being falsely buried or exposed by averaged LOD hills.

**Status:** architectural specification and staged acceptance criteria. This file is not proof that streaming, runtime LOD or open-world gameplay is complete. The existing `TerrainZoneDocument` and `TerrainZoneEditor` are finite-grid foundations; Blackpine's `SiteTerrain` consumes saved height offsets. Independent stitched-tile support is a new module milestone and must be verified separately.

## 1. PSX-era baseline, modern technique, and Beckenburg extension

| Domain | PSX-era typical constraint | Modern established approach | Beckenburg requirement (not automatically implemented) |
|---|---|---|---|
| World size | Predivided maps, loading screens, heavily limited geometry memory | World partition / streaming cells | Globally addressed tile coordinates with sparse creation, edit, and load, not one huge image or fixed volume; loading zones optional for interiors and disconnected worlds |
| Elevation | Low-resolution static geometry, coarse meshes | 16-bit+ heightfields, tiled import, terrain sculpt/paint | Multiple independent 16-bit PNG height tiles sharing exact borders; global metre scale and datum; lossless editable source separate from runtime caches |
| Landscape generation | Manually assembled terrain and repeating textures | Seeded noise, biome masks, erosion and splat maps | Reproducible multi-octave generator, seeded biome/landform masks, editable overrides, import/export and provenance; no unreviewed destructive rebake |
| Detail near/far | Fixed draw distance, fog, pop-in | Quadtree/clipmap/geomorph, screen-space-error LOD | Multiple distance rings from exact source, measured geometric-error bounds, LOD stitching, hysteresis, geomorph; distance in world metres, not arbitrary tile count |
| Occlusion | Painter sorting/z-buffer limitations and aggressive cutoffs | Depth/occlusion culling, HLOD and visibility data | Canonical *full-resolution* terrain for line of sight and actor physics; LOD mesh NEVER authoritative for gameplay obstruction or actor support |
| Precision | Small coordinate range, large-world jitter | Origin rebasing, relative/64-bit world coordinates | 64-bit tile indices or doubles for world positions; local floats for GPU vertices; origin shift is presentation-only and preserves gameplay coordinates |
| Texture | Tiny atlases and repeated UVs | Virtual textures, clipmaps, layered materials | Biome weights / splat masks and distance texture mips; preserve PSX aesthetic as a configurable presentation effect, not a geographic limit |
| Objects/foliage | Sparse hand-placed objects or sprites | Instanced vegetation, impostors, HLOD | Deterministic scattering, stable persistent IDs, per-biome density, instance batching and separate object LOD and collision |
| Physics/navigation | Simple mostly loaded regions | Independent collision/nav meshes in resident cells | Exactly supported sampled terrain and separate simulation interest zones; unloaded visual tiles don't invalidate authoritative actors or host AI |
| Editing | Fixed levels and heavy offline rebuild | Non-destructive brushes, layer history, asynchronous baking | Human editor with terrain brush, texture layers, road cuts, water integration, local procedural regeneration, undo/redo, region locks and exact saved readback |
| Networking | Small synchronous map state | Partition state replication and interest management | Host-authoritative world and seed, separately versioned tile deltas, revisions and affected-zone updates; no client-authoritative terrain height |
| Testing | Manual eyeball checks | Differential geometry/performance tests | Seam, ray, scope, crest, cross-LOD, long-distance actors, fast travel and streamer-load test fixtures, with actual engine captures |

Background references: [Epic World Partition](https://dev.epicgames.com/documentation/en-us/unreal-engine/world-partition-in-unreal-engine) (streaming cells); [Epic HLOD](https://dev.epicgames.com/documentation/en-us/unreal-engine/world-partition---hierarchical-level-of-detail-in-unreal-engine) (distant proxies); [Cesium 3D Tiles spec](https://github.com/CesiumGS/3d-tiles/blob/main/specification/README.adoc) (geometric error and screen-space refinement); [NVIDIA GPU Gems 2, geometry clipmaps](https://developer.nvidia.com/gpugems/gpugems2/part-i-geometric-complexity/chapter-2-terrain-rendering-using-gpu-based-geometry) (nested LOD rings and transition morphs). These provide comparisons, not vendor code to copy or evidence Beckenburg already works this way.

## 2. Unified terrain coordinate / tile model

- One global XY-in-horizontal convention for our game: **X/Z horizontal, Y metres up**, and one world elevation datum. Named source/world origin; no implicit image stretch to world size.
- A world manifest records `worldId`, schema version, tile cells N (power of two), metres per cell, world origin X/Z, elevation minimum and maximum, generation seed, texture-layer catalog, and precision/revision. One **N+1 by N+1** 16-bit grayscale tile image contributes N by N cells.
- Tile `(tx,tz)` covers `[originX + tx*N*step, originX + (tx+1)*N*step]` and equivalent Z. Negative tile indices are valid.
- Edge heights are **identical numeric 16-bit samples** between adjacent tiles. Reject conflicting imports, offer previewed repair modes later. Verify corners shared by four tiles. No bilinear mismatch, duplicate last-column ambiguity or silently painted seam.
- Global quantization means identical sample `q` maps to exactly one elevation `minY + (q/65535)*(maxY-minY)`. A local 16-bit scale per tile is forbidden because it breaks seams.
- Height query uses the **same triangle diagonal** for renderer/physics (not one bilinear function for walking and different GPU triangles). Sparse unknown tiles return explicit unavailable status; do not silently return sea level.
- Tile imports and edits are atomic, revisioned and recoverable; bounded PNG decode, tile cache, overflow checks and stable file naming. Source 16-bit images are preserved. Runtime mesh/LODs are derived caches, not the master terrain.
- Generator samples global integer vertex coordinates and a fixed seed; generating tiles in any order yields the same shared border. Terrain edits are separate from seed-based source and explicitly dirty the dependent tiles/LODs.
- Sparse world size is **not** limited by `TerrainZoneDocument.Grid`'s 65,536-vertex finite editing cap. That cap remains appropriate only for finite grid documents.

### Heightfields do not replace actual 3D caves and structures

A heightmap represents **one elevation per horizontal X/Z coordinate**. It cannot describe two vertically stacked floors, cave ceilings, overhangs, bridges or mine shafts correctly on its own. Treat it as the outdoor ground foundation, with separate authored volumetric meshes / solid collision / portals for excavations, multi-floor geometry and exposed rock faces. A subtractive hole mask in the ground permits cave entrances but must not invent a walkable terrain surface inside the void. The editor must show which system owns collision at each position (terrain ground, structural mesh, stair, water, tunnel). Keep the same real gameplay collision/nav system at every LOD, regardless of which visual layer is currently drawn.

**Acceptance fixtures:** tunnel under a surface hill remains navigable without terrain snapping the player onto the roof; bridge and road span keep their own supports; cave mouth remains open in every streamed LOD, including scope view from far outside; long-distance cliff/cave silhouettes cannot turn into solid averaged berms that falsely hide distant characters.

## 3. Long-distance LOD: visual simplification cannot change reality

**Non-negotiable:** game physics, character feet, projectiles, ground-contact, AI, hiding and line-of-sight resolve against authoritative high-fidelity world geometry, **not** a rendered LOD proxy. If a character stands on a real ridge at 2 miles, averaging a distant chunk must not produce an apparent mountain in front of them or a false exposed position.

1. **Canonical terrain pyramid.** Level 0 is exact stitched source height samples + deltas, with mesh diagonal fixed. Every coarser tile stores a representative height, *minimum/maximum* height envelope, maximum deviation from the exact surface in metres, sampling/datum and source revision. Height-only averaging is insufficient for visibility.
2. **LOD selection.** Distance rings may start with steps of 1, 2, 4, 8, 16... cells, but screen-space geometric error, camera FOV and viewport resolution drive refine/cull, not distance alone. Scopes/binocular zoom reduce world-space footprint per pixel and therefore demand finer LOD even when far away.
3. **Conservative occlusion.** To determine whether a terrain tile could hide an actor, use a hierarchical min/max elevation envelope to rapidly classify candidates, then fine/full-resolution terrain ray tests for ambiguous cases. Never let a **coarse mesh's render depth alone** decide that a character is hidden. If exact terrain isn't resident, prefetch the needed samples or mark visibility undecided; do not fabricate an occluder or expose a guaranteed hidden target.
4. **Render correction for actor silhouettes.** A correct gameplay ray alone does not solve a wrong visual z-buffer. If a distant actor is potentially visible through a scope, force-fine terrain refinement along the camera-to-actor corridor or use a carefully depth-validated special occlusion path, preserving ridge silhouette. A coarse proxy must not draw over a correctly visible actor. Do not draw the actor through real foreground hills.
5. **Cracks and transitions.** Neighbour LODs have shared logical border samples and deterministic seam-stitch meshes, or skirts only where visually acceptable. Prefer geomorph/hysteresis to hide LOD popping. Physics elevation does not morph with the camera.
6. **Independent budgets.** Streams for terrain height, texture, foliage, and simulation run on separate budgets; prioritise camera/aim corridor and destination warm-up. Avoid synchronous disk/PNG decoding on render thread.
7. **Origin rebasing.** Keep world positions stable in double/global space; shift only frame-local render coordinates and preserve deterministic simulation, raycasts and network messages.

### Required regression: scoped actor behind averaged hill

Construct two adjacent tiles with a narrow saddle and a high shoulder. Place a human-sized actor on an exact lower surface beyond the saddle. Choose a coarse averaged LOD that would place geometry in front of the actor in a naive z-buffer. From a scoped camera several kilometres away:

- Authoritative fine-resolution raycast and hit test agree with actor support and line-of-sight.
- Coarse min/max bound marks the ray ambiguous, forces refine/exact verification and does not classify the actor as hidden without proof.
- Rendered depth shows the actor if actual terrain leaves the ray clear; hides the actor if actual ridge blocks it.
- Results persist after tile unload/reload, tile edge crossing, mismatched neighbor LOD, FOV change/zoom, rapid travel and origin shift.
- Negative counterpart: actor behind a real ridge stays hidden (no see-through LOD bug). Both distant and nearby physics must agree.
- Each assertion needs actual scene coordinates, tile hashes, exact/coarse height samples, LOD selection, ray intersection, screenshot and source SHA.

## 4. Streaming and generation

Use a quadtree for sparse persistent world spatial indexing and a resident-budget LRU for decoded source tiles. The render pipeline creates nested LOD regions around camera and optional explicit interest sources (scope sightline, waypoint, remote player, editor camera). Use async prioritized requests, cancellation, bounded outstanding tasks, deterministic fallback to a known coarser resident tile, and revision-aware eviction. Support cold-start, traversal, teleport, night/fog, and worst-case altitude/scope tests. **No** world-wide allocation proportional to a hypothetical full continent; memory should scale with loaded regions and configured budgets.

LOD summaries must be invalidated/rebuilt after edited source vertices, including any touching neighbouring tiles; stale mesh or stale occlusion summaries are unsafe. A shared source seam edit must modify both sides atomically or reject.

## 5. Human editor modules before agent access

The editor must first let a human create/open a world, choose import vs procedural generation, select tile(s) or rectangular regions, view the stitched topology, set local brush/texture parameters, preview LOD shells, inspect seam defects, change biome and road layers, and save with undo. Loading zones are *optional* authored triggers linking interiors/disconnected worlds; streaming of adjacent outdoor tiles must **not** depend on hand-placed loading volumes.

Dedicated inspectors: tile coordinates, actual world metre bounds, min/max elevations, seam conflicts, LOD error, current resident/cache bytes, streaming requests, visible LOD band, authoritative actor-ground and visibility diagnostic rays. Do not falsely label 2D authoring maps as live game screenshots. Preserve original Beckenburg Castle and map workflows.

**Then** add the separately planned observation/manipulation/verification/headless/MCP agent interfaces to these proven human operations, never a second engine.

## 6. Dependency-ordered implementation and evidence

| Gate | Implementation deliverable | Must prove before marked accepted |
|---|---|---|
| T0 | Preserve finite terrain/loading-zone document and authoring UI, compile and run tests | Existing Castle, Blackpine, water/weather and saves unchanged |
| T1 | Independent Java 17 stitched world manifest, 16-bit grayscale PNG import, exact border checks, deterministic tile generation, true triangle height sampling, bounded LRU | Neighbor/corner/negative-index/absent-tile/byte-validity tests, local Java compile and standalone CI |
| T2 | Human stitched-world editor window; tile viewport, generate/import/export, seam diagnostics and undo | A manually created 3x3 world loads back exactly; corrupt tile import rolls back |
| T3 | Integrate stitched-world canonical height into an *actual outdoor runtime adapter* with matching collision and rendering | Real ground support and renderer sample agree at edges and non-planar diagonals; actual map screenshot |
| T4 | Hierarchical min/max/error LOD summaries and independent stream budgets | Memory bounded independent of world extent; no cracks or stale caches; measured timings |
| T5 | Distance LOD rings + morph and zoom-sensitive screen-space error | FOV/resolution/refinement and quick camera motion; visual frames with real render proof |
| T6 | Long-range accurate visibility/occlusion including scope rays and distant actors | Both scoped buried-actor and real-ridge-hidden tests; origin-shift/ray case pass; no incorrect GPU depth |
| T7 | Biomes, texture layers, erosion and vegetation/roads, weather/water integration | Stable reproducible textures, footprint/collision and no editor/runtime divergence |
| T8 | Multiplayer host-synchronized world/tile revisions and optional loading-zone game handoff | Real host/guest consistency, deterministic spawn, rollback and updates unaffected |
| T9 | Agentic interface after human/runtime modules work | An authorized agent reads/edits/verifies this **same** world, no unproved completion claims |

Statuses until proved: **T0 work in progress; T1–T9 planned**. Earlier Beckenburg 16 terrain foundations and Blackpine grading are not a general stitched/streaming/LOD implementation. Note exact test/CI evidence and revise these statuses per real completed gate.

## 7. Release and resource discipline

Only objective broken build, launch, essential gameplay or required multiplayer/update function blocks shipping. Visual cracks, biased LOD, content/biome mismatches and authoring quality warnings are developer diagnostics unless they objectively break play. Never mask a known failure by disabling a task or replacing a real playable map with synthetic placeholders. No forced push. Preserve current game environments and original art. Maintain source hashes and no unrelated edits.
