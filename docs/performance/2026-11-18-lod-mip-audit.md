# Beckenburg — real geometry LOD and runtime mipmapping audit

Source baseline: `af7d662e1efc0bc7857619c7e0b907f07b111181`. Audit date: 2026-11-18.

## Verdict

**Neither is fully implemented in production.** GPU texture creation previously used `new Texture(bitmap)` and only nearest/linear minification; no mip chain. `MaterialReviewMip` is a Material Editor preview feature. `DistanceDetail` is a CPU depth-based filter/fog, not less geometry. Existing `FixtureLighting.sampleDistanceLod` and `SceneRenderer.lightingSubdivisionLimit` reduce *lighting-probe* cost and CPU/prepared GPU subdivisions. These are real optimizations but not general authored mesh LODs. SceneRenderer also has coarse object and chunk frustum rejection, static resident VBOs, and a few authored billboards; frustum culling is not mesh simplification.

## Initial safe implementation (experimental, NOT the default)

`-Dbeckenburg.experimental.staticWorldMipmaps=true` enables libGDX/OpenGL mip-chain creation **only** for immutable `LevelMaterials.BASE..ROOF` diffuse textures, at least 32x32, with `MipMapNearestNearest` minification and `Nearest` magnification. By default, original nearest-neighbour material sampling remains unchanged. The project retains imported glTF texture UVs, alpha, explicit glTF wrapping and PSX look. No new asset or GPU-dependent dependency is required by core Java.

Never include dynamic monitors, live site maps, cameras, text, evidence, ghost silhouettes, alpha foliage, imported GLB atlases, or UI in the pilot. They need an independently verified UV/padding/alpha policy and before/after framebuffer parity checks. Preserve exact gameplay visibility. GPU logs must separate base uploads, mip generations, estimated uncompressed additional RGBA bytes and actual GPU render/readback timings; the estimate is not VRAM measurement.

## Actual mesh/terrain LOD is still pending

Author / validate high-, medium- and low-density geometry for **selected distant static terrain and props**, not a generic decimator applied to walls, doors, steps or collision. The LOD selection must use **projected pixel footprint**, with hysteresis and conservative bounding volumes. Save resources/GL handles per LOD and count geometry actually submitted (not merely cull queries). The collision/navigation mesh, stairs, lights/evidence volumes, door apertures, legal ghost manifestations and visibility constraints remain full-authority and unchanged. Draw-call batching, instancing, immutable VBOs, portal/frustum culling and texture residency are independent wins. For distant repeated shrubs/trees/rocks, test impostors or simple geometry **only** after authoring and near/far silhouette comparisons.

## Acceptance matrix — no simulation cheating

For every optimization A/B compare identical map, seed, equipment, lighting, FOV, resolution, camera route and role on the GTX 1660 SUPER and a CPU fallback where relevant. Retain all raw run CSVs and source SHA.

- **Rendering:** actual distinct displayed frame intervals (p50/p95/p99, max, >33.3/50/100ms), native driver/GL worker stages, CPU geometry+post time, frame IDs and stale readbacks, GPU timer queries where available; separate cold startup, van exit, inside sanctuary and outdoor travel.
- **LOD:** triangles submitted/culled at near/medium/far distances, draw calls, LOD transition count and transition flicker/silhouette error; constant camera movement, near collision and indoor step/rail contacts. Check that no hidden/far object disappearance changes evidence or sightlines.
- **Textures:** base uploads, mip-chain builds/levels, estimated extra RGBA bytes and actual VRAM if available, texture-filter options, cache hit/eviction, first-use/upload hitch, projected texel aliasing. Compare nearest baseline against nearest-mip at matched near/far views; assert text/CRT/alpha unchanged.
- **Playability:** actual input event-to-host acceptance, input-to-displayed pose, input callback gaps, world updates/s, measured walking and ghost traversal distance/s, stopping overshoot, sprint drain seconds, ghost hearing fade, collisions and network jitter; no FPS win is accepted with worse movement or lost evidence/AI actions.
- **Repeatability:** cold/warm runs, solo and 3-seat TCP, long-session GC/memory and p99 hitch counts. Performance diagnostics are advisory; broken gameplay/multiplayer, build or updater remain release blockers.

**No reduction in player/ghost simulation frequency, evidence sensitivity, monster logic or network authority may be called an optimization.** Profile CPU allocation/GC, GPU shader compile and startup caching, pixel readback (the current CPU HUD/sprite/depth bridge), retained VBOs, batching and authored geometry before considering any quality trade-off. Mipmaps reduce aliasing and may improve texture sampling/cache behaviour; they do not guarantee meaningful CPU or GPU frame-time gains, and initial mip generation can itself cause startup hitches.
