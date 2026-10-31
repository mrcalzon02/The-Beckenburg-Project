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

- **Continuous rebuild:** persisted per world; when enabled, regenerate a diagnostic graph after each author edit and when source/topology files change. Builds use a worker rather than freezing the editor. When disabled, changes still save but the graph only updates via **FLOOD FILL PULSE**. The **minimum region size** is adjustable separately from the render topology to trade local detail against graph size.
- **Seed at click:** authored entry/start for the reachable-component flood. If no seed has been authored, the builder returns all physically valid candidate regions for inspection. With one or more seeds, only reachable regions are in the final navigation graph.
- **Draw blocked gate:** click two axis-aligned segment endpoints across an open doorway, loading-zone transition, prohibited passage or NPC-only boundary. The generator refuses graph connections crossing the segment even when the 3D models appear open. A segment only blocks its actual span; AI can legitimately go around its ends if geometry permits. Use persistent trimming or enclosing collision barriers when a whole destination instance must be excluded.
- **Trim selected:** click a generated polygon, then permanently remove its world-space rectangle from the navigable output. **Trim connected:** deliberately remove the entire displayed connected branch, with a confirmation count. Rebuilds never restore these authored masks. The editor retains **grey, unreachable pre-flood candidate polygons** past a barred gateway so you can select the disconnected side and permanently regress it without ever allowing the AI to navigate there. The flood-reachable output alone is used for routes.
- **Untrim last / Remove last gate / Remove last seed / Undo / Redo:** edit the separate persisted authoring document. Save and immediate disk readback verify the authored state.

**Storage:** \`navmesh-authoring.bin\` is separate from source \`tile_*.png\` and \`adaptive-topology.bin\`. It stores stable world-space seeds, exclusions, blocked gate segments, and manual/continuous mode. Generated region indices are not permanent; only source-coordinate masks are. The document uses a bounded binary format and atomic replacement.

**Builder integration:** \`AdaptiveNavigationRegions.build(..., AdaptiveNavMeshEdits, minCell)\` augments the existing exact-collision subdivider: refine near cut masks and gate interiors, reject regions intersecting forbidden masks, omit blocked peer connections and ability-restricted vertical links, and breadth-first flood from seeds over the remaining connectivity. No renderer LOD determines this output. Graphs reject stale authored/topology revisions. \`DevTerrainTopologyLab\` uses the same saved masks on QA refresh.

**Acceptance:** \`AdaptiveNavMeshEditsTest\` exercises physical geometry remaining open behind a blocked level-transition gate, persistent no-regrowth after seed flood, trimming sub-areas from otherwise coarse navigable regions, undo snapshots, source tile preservation, and continuous mode persistence. It is a functional Windows Java 17 test, not a cosmetic release blocker.

**Not yet integrated:** A live runtime agent does not consume this authored graph, and a blocked nav edge is *not* an actual collision wall. Layered cave mesh and production scene physics remain separate milestones. An editor window preview is not proof of Hunter/ghost AI traversal.


## N2b — Persisted underground authoring from the navigation editor (2026-10-31)

**Open:** Stitched World → Adaptive Topology → NAV FLOOD EDITOR → **UNDERGROUND GEOMETRY**. The layered geometry window lists and allows adding, revising and deleting independent floors, axis-aligned solids and cross-layer traversal links. Undo/redo and save perform immediate disk readback. Source heightmap PNGs and adaptive cutout topology are untouched.

- **Floor:** named layer (for example \`bunker\` or \`sewer\`), world-space X/Z rectangle, base height and X/Z rise. Multiple nonoverlapping slabs can share one layer. Overlapping slabs on the same layer are rejected.
- **Solid:** unique ID plus world-space 3D bounding box; collision-query clearance and navigation authoring reject impassable solid walls and ceilings. This does **not** create a visible wall mesh.
- **Connection:** explicit \`STAIRS\`, \`LADDER\` or \`DROP\` link between two named layers at authored endpoints. Both endpoints must be within their destination support slabs; links with orphaned layers are rejected. Cross-layer routes remain capability-gated.
- **Viewport:** choose the layer to inspect; flood rebuild consumes the saved floor/solid/link data. Existing nav seeds, gates and exclusions remain layer-specific. Editor overlay uses teal floor outlines, white obstacle rectangles and magenta transition markers. Agent ladder/drop capability toggles affect reachability.
- **Storage:** separate bounded, atomically saved \`layered-geometry.bin\` alongside \`navmesh-authoring.bin\` and \`adaptive-topology.bin\`, leaving the source \`tile_*.png\` unchanged. Authored geometry is independent of generated nav region IDs.

**Test:** \`LayeredTerrainDocumentTest\` validates source tile preservation, three physically distinct floors, stairs/ladder capability restrictions, obstacle collision, persistence and readback, overlap rejection, orphan-link rejection and snapshot restoration. It runs in Windows Java 17 focused terrain acceptance.

**Important:** This milestone supplies **editor-owned physical query geometry**, not full cave meshes, mesh welding at the surface cutouts, model rendering, or live game collision. A stair link in the nav graph is author intent, not an animated staircase or automatic level-instance transfer. Those runtime and visual connections remain subsequent tasks.


## N3 — Three distinct terrain entrance seam methods (2026-10-31)

Open Stitched World → Adaptive Topology → ENTRANCE SEAMS 1/2/3 (also linked from Smooth Ramp Entrances). All three are topology operations, not level heights:

1. **Method 1 — NEW INTERIOR SEAM.** Add a source-tile interior seam along adaptive-grid-aligned endpoints. The explicit CONFORM LEAVES + PUNCH TABS command refines actual quadtree leaves until the seam coincides with their edges. Crossing the new seam on the outside surface is blocked. A separate CUT HOLE operation, or Method 3, can create the corresponding opening. The seam retains its underground destination layer identity.
2. **Method 2 — DE-STITCH TILE EDGE.** Select a real existing source-tile boundary. The surface graph stops connecting across this boundary, preserving both underlying heightmap tiles while recording the intended underground destination. Rendering/mesh ownership of the replaced edge remains a downstream integration; it is not automatically connected to an underground render tile yet.
3. **Method 3 — DETACHED TAB / PUNCH.** A matching Method 1 seam is required along one edge of a rectangular tab. The explicit CONFORM command splits and punches the outer source leaves underneath this patch, and the physical query module reassigns the *same original height surface* to an independent named navigation layer. Normal outdoor surface connections do not regenerate over it. A ramp mouth inside the tab stitches to this distinct tab layer rather than to the main outdoor surface.

The authored operation document is entrance-seams.bin, independent of the source PNG, adaptive-topology.bin, layered-geometry.bin, ramp-entrances.bin and navmesh-authoring.bin. Editor masks have undo/redo and disk readback. The explicit physical CONFORM command also provides REVERT LAST CONFORM, which refuses to erase unrelated subsequent topology edits. Original 16-bit source height pixels are never rewritten by this work.

The adaptive flood fill now loads method 1/2 seam barriers, method 3 independent tab supports, and explicit ramp-to-tab navigation endpoints. It visually marks the three kinds of opening in the 2D authoring map. The targeted Java 17 test is EntranceSeamDocumentTest: subdivision, tile de-stitch separation, tab punching and standability, no outdoor AI cross-edge route, disk persistence, idempotent reapplication and source PNG seam preservation.

**Status boundary:** this is actual editor-side collision/query/topology authoring. Full 3D cave-mouth mesh emission, visual subterranean-tile re-welding, GPU rendering and live Hunter/ghost movement integration are still future work. The editor may not claim an authored seam is already a playable cave entrance.


## N4 — Real authored 3D entrance mesh emission and inspection (2026-10-31)

From **Smooth Ramp Entrances**, select a ramp and press **3D MESH PREVIEW** or **EXPORT MESH OBJ**. From **ENTRANCE SEAMS 1/2/3**, select a seam or tab and use the same 3D preview/export commands. The preview is a rotatable, zoomable projection of actual generated mesh triangles, labeled explicitly as an editor inspection view and **not an in-game screenshot**.

The source-consistent geometry generator, \`TerrainEntranceMesh\`, emits:

- Outdoor terrain triangles from **real 16-bit heightmap pixel samples** and actual adaptive quadtree leaves; polygon edge vertices follow \`AdaptiveTerrainTopology.edgeBreaks\`, including fine/coarse neighbors. Split terrain borders are exported with separately owned vertex indices rather than automatically rewelded.
- The original-height **detached navigation tab** as its own mesh part. Punched outer surface leaves remain absent from the outdoor surface mesh.
- Continuous cubic-Hermite ramp triangles using the **identical height function** used by collision/navigation, including C1-eased mouth and interior grades.
- Independently authored underground floor slabs, omitting surface patches where the ramp overlaps the destination floor.
- **Exposed rock cheek walls** *only* where a real cutout meets a physical ramp and an adjoining uncut terrain edge. Wall endpoints use source terrain heights and ramp heights; no arbitrary-depth fake cavern wall or unrequested backdrop is invented.

The **geometry-only OBJ** output has named groups for surface seam sides, detached tabs, ramps, floors and rock edges. It includes UV coordinates but **does not fabricate a replacement diffuse texture**. The exporter parses the written file through \`EditorMeshPipeline.read\` before publishing it, and the editor performs a second readback after export. Geometry is limited to 16,000 triangles and a bounded source-cell region: larger entrances must be exported in chunks, not silently downsampled.

Windows Java 17 \`TerrainEntranceMeshTest\` checks adaptive mesh face orientation and source-height agreement, separate sides of existing and new cut seams, preserved detached tab triangles, ramp/query height agreement, non-fabricated cave cheek walls, OBJ roundtrip group and face preservation, sparse outer-tile borders and original source heightmap seam integrity. This test blocks only on a functional mesh/export regression, not visual style.

**Still pending:** hooking emitted OBJ/GLB or mesh batches into the live game-level renderer and collision/AI runtime; dynamically streaming many tiles, building full tunnel/ceiling walls beyond authored rock edges, and texture/material assignment from the approved asset library. OBJ output is an inspectable intermediate asset, not proof of a finished in-game cave.


## N5 — Reusable runtime-scene document and terrain package module (2026-10-31)

The portable Java 17 editor kit now provides two deliberately separate functional modules:

- **RuntimeSceneDocument** serializes any game-owned RuntimeScene adapter into an atomic, versioned \`scene.bin\` with stable scene ID, node IDs, hierarchy, transforms, layers, component types and string properties. On load it enforces bounds, rejects bad parent graphs, checks deterministic disk readback, and can restore into an existing matching runtime scene without overwriting a different level.
- **TerrainScenePackage** is an opt-in EnvironmentModule with \`AUTHORING\`, \`SERIALIZATION\`, and \`DIAGNOSTICS\` capabilities. Given saved source terrain and explicit patch bounds, it builds the *real* TerrainEntranceMesh, publishes geometry-only OBJ into \`scene-package/meshes/<chunk>.obj\`, checks the importer and SHA-256 hash, and registers a stable \`terrain-<chunk>\` scene node carrying a \`world://\` URI, mesh part/triangle counts and source-query file references. Regenerating a named chunk replaces only its owned mesh/node, not unrelated props or a foreign OBJ. Failed operations restore the scene and previous geometry. The scene itself is saved in \`scene-package/scene.bin\`.

**Source staleness safeguard:** published scene nodes also store a SHA-256 fingerprint of the source 16-bit tiles covering their patch plus the authored adaptive topology, seam, ramp, and underground-layer documents. **VERIFY PACKAGE** distinguishes an untouched OBJ from an up-to-date OBJ; changing those inputs reports **STALE** until the chunk is rebuilt. Existing assets are never silently regenerated or overwritten because of a warning.

**Editor route:** Stitched World → **TERRAIN SCENE PACKAGE**. Select tile, local cell offsets and a bounded patch side; specify the stable chunk ID. **3D MESH PREVIEW** generates an inspection view without saving it. **PUBLISH PATCH TO SCENE** bakes OBJ and saves the authoritative scene manifest asynchronously. **VERIFY PACKAGE** checks current files and hashes; **RELOAD SCENE** refreshes from disk.

This is a real reusable scene **data handoff**, not a claim that runtime SceneRenderer already parses these OBJ asset references. Explicit \`rendererStatus=requires-host-adapter\`, \`materialStatus=unassigned\`, and \`collisionStatus=query-authoring-only\` deliberately prevent placeholder material art and phantom collision. A future Beckenburg adapter must import/stream these mesh sections into GPU buffers, bind approved material assets, and supply authoritative collision/navmesh to Hunters and ghosts. Separate render/material/AI diagnostics must not gate release for aesthetic differences.

Functional tests \`RuntimeSceneDocumentTest\` and \`TerrainScenePackageTest\` run in Windows Java 17 acceptance. They cover save/reload, identity isolation, stable node updates, integrity verification, rejection of un-conformed seams, refusing to overwrite foreign assets, rollback and 16-bit heightmap/source preservation.
