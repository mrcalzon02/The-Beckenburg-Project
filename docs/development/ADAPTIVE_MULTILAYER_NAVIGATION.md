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

**Status (2026-10-31):** N0 is documented. N1 now has a **standalone diagnostic/query prototype** in `editor/LayeredTerrainQueries.java` that samples real stitched source triangles, excludes punched cutout leaves, retains independent floor layer IDs, and checks AABB solids, capsule-footprint support and headroom. N2 has a **standalone adaptive region-graph prototype** in `editor/AdaptiveNavigationRegions.java` with bounded split budgets, exact source-gradient inspection, physical portal validation, confirmed portal waypoints and explicit ability-gated vertical links. The corresponding deterministic acceptance tests are `LayeredTerrainQueriesTest` and `AdaptiveNavigationRegionsTest`. The initial Windows CI for N1/N2 passed at [run 41231554595](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/41231554595); revised waypoint validation passed at [run 41231621400](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/41231621400). The later steep-source-PNG regression requires its own CI result, and must not be counted as passed before readback.

The real `DevTerrainTopologyLab` now computes diagnostic surface nav regions/links and a sample cross-cutout route beside its real renderer patch, exposed in `DevAssetShowroom` status and headless report. Physical cave mesh authoring, fully conforming 3D collision, production runtime movement, navmesh integration with `GhostPracticeHunterAi` and `GhostAiNavigationAudit`, multiplayer replication, dynamic stream invalidation, and live six-hunter simulation **remain outstanding**. An explicit transition in the graph describes an author-approved connection; it is not yet proof that the actual ladder, staircase, animation or swept movement implementation exists. N3–N6 are not implemented, and the prototype must not replace the game's existing movement authority until runtime parity is tested.


## N2a — Authored seed flood, selective regression and protected transition barriers (2026-10-31)

**Editor UI:** Open a stitched source world, choose **Adaptive Topology → NAV FLOOD EDITOR**. The dedicated Swing editor displays an up-to-8×8-source-cell area in actual world coordinates.

- **Continuous rebuild:** persisted per world; when enabled, regenerate a diagnostic graph after each author edit and when source/topology files change. Builds use a worker rather than freezing the editor. When disabled, changes still save but the graph only updates via **FLOOD FILL PULSE**.
- **Seed at click:** authored entry/start for the reachable-component flood. If no seed has been authored, the builder returns all physically valid candidate regions for inspection. With one or more seeds, only reachable regions are in the final navigation graph.
- **Draw blocked gate:** click two axis-aligned segment endpoints across an open doorway, loading-zone transition, prohibited passage or NPC-only boundary. The generator refuses graph connections crossing the segment even when the 3D models appear open. A segment only blocks its actual span; AI can legitimately go around its ends if geometry permits. Use persistent trimming or enclosing collision barriers when a whole destination instance must be excluded.
- **Trim selected:** click a generated polygon, then permanently remove its world-space rectangle from the navigable output. **Trim connected:** deliberately remove the entire displayed connected branch, with a confirmation count. Rebuilds never restore these authored masks.
- **Untrim last / Remove last gate / Remove last seed / Undo / Redo:** edit the separate persisted authoring document. Save and immediate disk readback verify the authored state.

**Storage:** \`navmesh-authoring.bin\` is separate from source \`tile_*.png\` and \`adaptive-topology.bin\`. It stores stable world-space seeds, exclusions, blocked gate segments, and manual/continuous mode. Generated region indices are not permanent; only source-coordinate masks are. The document uses a bounded binary format and atomic replacement.

**Builder integration:** \`AdaptiveNavigationRegions.build(..., AdaptiveNavMeshEdits, minCell)\` augments the existing exact-collision subdivider: refine near cut masks and gate interiors, reject regions intersecting forbidden masks, omit blocked peer connections and ability-restricted vertical links, and breadth-first flood from seeds over the remaining connectivity. No renderer LOD determines this output. Graphs reject stale authored/topology revisions. \`DevTerrainTopologyLab\` uses the same saved masks on QA refresh.

**Acceptance:** \`AdaptiveNavMeshEditsTest\` exercises physical geometry remaining open behind a blocked level-transition gate, persistent no-regrowth after seed flood, trimming sub-areas from otherwise coarse navigable regions, undo snapshots, source tile preservation, and continuous mode persistence. It is a functional Windows Java 17 test, not a cosmetic release blocker.

**Not yet integrated:** A live runtime agent does not consume this authored graph, and a blocked nav edge is *not* an actual collision wall. Layered cave mesh and production scene physics remain separate milestones. An editor window preview is not proof of Hunter/ghost AI traversal.
