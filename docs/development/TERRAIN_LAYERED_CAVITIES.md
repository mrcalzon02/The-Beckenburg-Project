# Layered Terrain and Real Excavation — Beckenburg Editor

**Requirement confirmed 2026-10-31:** Adaptive terrain authoring MUST make genuine holes, caves, cavern networks, bunkers, sewer systems, tunnels, mines and multiple stacked walkable levels straightforward. This is a human-editor feature before any agentic interface.

## Geometry and data ownership

1. **Stitched source heightfield:** immutable 16-bit reference samples. One Y per X/Z cannot represent a cave roof and cave floor simultaneously.
2. **Sparse adaptive surface topology:** independently authored quadtree split nodes and persistent *open* leaf masks. A cut removes the actual terrain faces; it is not transparency, a lowered height, or a decal. Undo and restore expose the original source geometry. Shared coordinates cross PNG tile borders.
3. **Layered 3D submeshes:** separately addressable geometry, in arbitrary world X/Y/Z, supporting ceilings, floors, roofs, walls, caves with overhangs and tunnels under other tunnels. One X/Z position may contain multiple valid surfaces.
4. **Entrance connections:** explicit perimeter/portal reference joining surface cutout edges to an underground mesh opening; optional rock rim, lining, shaft, door, stair, ladder or ramp. An incomplete hole is explicitly an open void and receives no invented bottom or cap.

Each solid surface has a stable layer ID and revisions independent of rendering LOD. Physics, nav and occlusion derive from authoritative source+cutouts+3D submeshes, never from averaged distant graphics.

## User-facing operations

- **Cut Hole**, **Restore Surface** on selected leaf (already implemented as authoring API); extend to brush, polygon/ring and silhouette-aware selection. All edits reversible, bounded and atomic.
- **Refine Hole Rim** preserves open state upon split. **Merge** rejects mixed open/solid areas unless the user explicitly fills/cuts to make them uniform; no accidental tunnel sealing.
- **Create Underground Layer** with depth, width/ceiling profiles and textured geometry. Support arbitrary branching and crossing paths.
- **Extrude Entrance** consumes the open perimeter, generates a matched side-wall loop and tunnel cross-section, then connects to a placed cavern/sewer/bunker mesh. Keep portal connections inspectable.
- **Layer panel**, cutaway/section view, vertical slice, wireframe and entrance graph. Editors can isolate upper ground, roof, interiors and individual underground strata without changing collision.
- **Local undo/redo, readback and revisioned save** must include mask, boundary, layer meshes, portals and invalidation references together.

## Real gameplay consequences

- **Collision:** missing terrain faces must also be missing in collision and line/ray tests. A hunter entering a hole hits the actual cavity floor, not an invisible source-heightfield lid. Support valid falls and intentional dangerous voids.
- **AI navigation:** a route crosses the cutout only via a walkable ramp, stair, ladder or other explicit traversal link. Navigating to an opening is not permission to walk across it. The multilayer navigation policy is specified in [ADAPTIVE_MULTILAYER_NAVIGATION.md](ADAPTIVE_MULTILAYER_NAVIGATION.md).
- **Lighting/visibility:** cave mouth, tunnel and ceiling surfaces occlude correctly; sky exposure and placed lights recognize real apertures. The far visual terrain proxy must not visually seal an entrance that is truly open. Scopes and distant actors must be evaluated against exact geometry.
- **Streaming:** when an entrance is reachable, its immediate connected underground chunk is prepared before collision/navigation transition. Links remain valid across map tiles and stacked levels.
- **Water/hazards:** floods, sewer flow, air and material effects are independently authored world volumes and use real enclosure and passage data.
- **Performance:** rebuild only edited cells, intersecting seams, portals and invalidated collision/nav/light fragments. Preserve GPU batching and bounded persistent caches. No global re-bake per frame.

## Current milestone and tests (not full runtime support)

**Implemented in code, awaiting CI readback:** AdaptiveTerrainTopology v2 persists sparse hole leaves with backwards read of v1. It offers `punch`, `fill`, snapshot/restore, safe refinement and guarded merge. AdaptiveTopologyEditor exposes CUT HOLE and RESTORE SURFACE; the isolated DevTerrainTopologyLab skips cutout triangles when drawing. The DEV-room's preexisting studio floor can still appear beneath the sample terrain: that is **not** a real excavated chamber.

**Still missing:** cave mesh authoring, tunnel/portal extrusion, connected collision/LOS/nav, actual gameplay cutouts, distant LOD/occlusion parity, and integrated layer-specific saves. Do not label a 3D diagnostic render as proof of these.

### Acceptance path

1. Round-trip a single subdivided hole, half-filled rim, tile-border opening and legacy uncut v1 document. Confirm original 16-bit PNGs retain byte identity.
2. In real dev-room screenshots, prove a cut deletes terrain triangles; follow-up build must expose a separate cavity floor/wall and render it through a real opening.
3. Attach a multi-branch cave beneath intact hillside with one real entrance, stairs to second stratum and sewer crossing below; test distinct Y layers at equal X/Z.
4. Exercise in-game collision, AI crossing, flashlight shadows, camera visibility, scope distance LOD and water volume boundaries under randomized edit revisions.
5. Profile localized regeneration versus area size; art judgments remain advisory, broken functionality blocks acceptance.
