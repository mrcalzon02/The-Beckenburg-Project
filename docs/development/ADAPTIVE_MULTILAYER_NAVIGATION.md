# Adaptive Multi-Layer Navigation — Beckenburg Terrain, Hunters and Ghosts

**Requirement confirmed 2026-10-31:** NPC routes must be adaptive under both low- and high-density topology, and through stacked underground spaces. Rendering mesh vertex count **must not** dictate navigation node count. Human editor modules come first; agents later invoke the same sanctioned operations.

## The critical separation

1. **Exact physical world:** stitched terrain source, authored sparse height detail (when implemented), cutout masks, doors, placed objects, underground layered solid geometry and authoritative collision.
2. **Navigation surface:** agent-specific traversable regions constructed by validating precise physical geometry. This is independent of camera/distance/rendering LOD. A coarse-looking terrain patch can still be scanned at finer sampling to establish gradient, obstacle and clearance limits.
3. **Search representation:** hierarchical region/portal graph with compact polygonal areas (and per-layer ID), plus local high-detail connections only at difficult transitions. A forest clearing does NOT require one waypoint per visual vertex; a narrow sewer turn DOES require enough local samples even when the visible mesh is coarse.
4. **Behavior:** existing HouseMap and ghost room/portal rules remain authoritative for their current maps. A new world-navigation adapter refines local routes, without quietly overriding ghost door restrictions, evidence perception, permissions or van boundaries.

Do not derive walkability from a visual LOD approximation or use a geometry simplifier whose error can mask a hole, cliff or doorway.

## Adaptive construction

- Partition walkable geometry into **regions** by tile, loaded chunk, vertical layer and connected components. A region may contain large convex or locally simple polygons, not thousands of unneeded triangles.
- Subdivide locally where **traversal complexity** demands: sharp elevation gradient, step height, fall hazard, tight clearance, obstacle proximity, hole boundary, portal, slope break, branching corridor, AI turn radius or a jump/ladder/ramp link.
- For *coarse* source terrain, probe the canonical collision/height function adaptively within cells; refine until surface error, maximum slope and clearance are bounded. Never accept 'looks flat at vertices' as proof of safe passage.
- For *fine* visual mesh, simplify by merging adjacent navigable polygons **only if** exact collision samples and swept-volume checks prove consistent clearance, slope, connectivity and no missed hazard/portal. Do not store a waypoint for every decorative triangle.
- Preserve critical nodes: entrances, narrow choke points, bridges, stairs, vertical connections, door thresholds, hazard boundaries and cover/visibility-critical corners. These are topology constraints, not optional low-detail approximations.
- Make the builder deterministic, budgeted, incremental and revision-based. A changed terrain leaf or cave wall invalidates only touching navigation regions and portal links. Retain stable IDs for unaffected regions. Never silently mark dirty chunks as walkable.
- Neighbor regions connect through **explicit traversable portals**, with measured width/height and ability requirements. A cutout is initially a void: it becomes an entrance link only if a valid floor, ramp, stair, ladder or drop behavior connects it.

## Runtime routing

**Stage A — strategic route:** A* or another suitable bounded search through region/portal graph, from start layer to destination layer. Cost accounts for actual distance, door effort, exposure, environmental hazards and agent abilities. Never authorize crossing a closed/blocked or player-only boundary by guessing.

**Stage B — local route:** Corridor/funnel smoothing through navigable polygons; add local intermediate points only as required by geometry and turning constraints. Validate each segment against authoritative swept collision. Adjacent avatars and movable furniture are handled by short-horizon local avoidance, not full navmesh rebuild every tick.

**Stage C — recovery:** If a segment fails real collision, mark its local edge invalid, locally refine sampling or replan around it. Never repeatedly push an NPC into an invisible slope or teleport through rock. Surface/floor Y is resolved with layer ID and actual collision, *not* closest heightfield Y. Avoid allowing agents on underground sewer level to snap to the surface road.

**Agent profiles:** hunter, crouching hunter, ghost (with explicit ghost-specific capabilities), and optional larger entities have height, radius, slope, step, jump/drop, ladder, door and water tolerances. A single mesh cache may share raw geometry, but passability/links differ per ability class; ghost powers require explicit rules rather than universal clipping.

**Dynamic changes:** open/closed doors, temporary equipment, moved clutter, blocked rubble, terrain edits and water changes update edge costs/availability or localized mesh revisions. Live multiplayer server owns route collision truth, not an individual client's approximate LOD.

## LOD, scopes and tall worlds

- Camera distance should NEVER lower authoritative navigation accuracy; nav streaming is based on active NPCs, routes, portals and nearby goals, independently of view.
- A faraway actor may use cheaper **planning** across coarse region graph, but exact local movement and position need high-fidelity surfaces once simulated. Predictive loading along path avoids stopping at a cave threshold.
- Visual LOD and depth occlusion must honor cutouts, passage envelopes and real cave roofs. Scope zoom may force visual refinement, but should not rewrite a NPC's route or move it to a different floor.
- Track `(regionId, layerId, polygonId)` for the agent; X/Z alone cannot identify a ground plane in a multilayer scene.

## Tests and instrumentation

Test all examples with *physical* route probes and actor simulation, not only generated graph connectivity:

| Environment | Test |
|---|---|
| Huge nearly flat meadow with dense decorative triangles | Few nav regions, no explosion in graph/search cost |
| Coarsely sampled hill with narrow ravine | Collision probing detects ravine and builds sufficient safe local points |
| High-detail rubble and narrow furniture passages | Clearance-constrained route correctly rejects oversized agent |
| Hole punched in field | No false edge crosses the open void; explicit bridge/ramp works |
| Cutout across stitched PNG border | Stable links and equal positions on both sides |
| Two rooms stacked at same X/Z | Independent nav layer IDs, no wrong-floor snap |
| Bunker stairs, cave branches, deeper sewer crossing | Connectivity only through authored vertical/portal links |
| Doors, fallen clutter, flooding | Incremental local invalidation, route recovery without infinite collision loop |
| Scope observing NPC a mile away | NPC remains on correct terrain; visual occlusion does not bury/expose it incorrectly |

Capture deterministic seeds, generated region/node counts, memory/CPU build times, path search p50/p95, max error bounds, failed segment count, number of local invalidations and behavior traces. Maintain acceptance tests for ghost/Hunter movement rules separately. Visual screenshots are advisory; failure to move/collide/navigate is objective functionality.

## Implementation staging

**N0 design:** document system and expose diagnostic requirements (this milestone).
**N1 geometry adapter:** build authoritative layered collision/height samples and cutout boundaries, with stable per-layer IDs.
**N2 sparse region builder:** generate verified walkable polygons with clearance/slope error bounds; join layer-aware portals.
**N3 hierarchical search:** high-level route + local funnel/swept-volume validation; agent profiles and recovery.
**N4 incremental updates:** doors, moving clutter, terrain edits, water, streaming; replan only impacted regions.
**N5 DEV room and AI simulation:** build indoor/outdoor/cave test fixtures, display regions/portals, allow six AI hunters + ghost and admin observer, collect real movement traces and collision failures.
**N6 agentic editor:** make agents read and invoke approved editor APIs with undo and real-scene readback.

**Status:** N0 documentation only. Existing GhostAiNavigationAudit is room/portal oriented, not a general open-world adaptive multilayer navmesh. Do not claim N1–N6 or actual cave traversal is implemented.
