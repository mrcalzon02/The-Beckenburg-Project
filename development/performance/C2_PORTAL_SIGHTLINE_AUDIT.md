# C2 visibility safety audit: comparing portal graph against actual drawn geometry

**Status:** diagnostic-only until exact-commit test pass. **Parent:** C1 portal candidate graph and B2c room-owned static chunks.

The core architecture already gives us a reachability graph, but portal reachability is not actual camera occlusion. This candidate now compares every authored House shell face that passes the established per-face frustum test with the graph's candidate rooms. Unknown/unowned face attribution is tallied separately. No call to this diagnostic removes or alters geometry, ghosts, equipment, evidence or network authority.

The test emits a ten-state CSV in build/e2e/viewport-profile/portal-candidate-audit.csv: five House camera positions in closed and open-door states. Every visible face is accounted for and unknown/exterior camera states must fail open. Nonzero interior candidate misses are explicitly **evidence against enabling hard room culling**; they are not silently fixed by suppressing the data or weakening the functional test.

Next C milestones: audit window/void/stair/portal sightlines against the counts; build portal-plane clipping with verified authored bounds; test representative play eye paths and dynamic door/window transitions on real installed GPU; only then allow production room/portal rejection. Never use portal culling for gameplay state, ghost identity inference or equipment logic.

Portable adoption: see development/performance/portable/MILESTONES_A-G.md at main. For 2D board games, use actual viewport/tile visibility instead of copying this 3D graph.

## C3 — Bounded physical-occluder ray diagnostic (candidate)

On actual House cameras, classify up to 32 frustum-accepted but graph-excluded triangle centroids **per omitted room** using HouseMap.clearLineOfSight, which examines authored wall, door, deck/floor, and collision state. Extend the original 10-view CSV with ray_sampled, ray_clear, ray_blocked and ray_by_room; annotate open/closed door state and camera room. Never use these samples for draw rejection or game state. A clear centroid ray is possible line-of-sight evidence, not a pixel-visibility measurement. A blocked centroid ray does not establish the whole triangle or ghost sprite is hidden. Further C3 acceptance needs per-pixel depth/reference rasterization, physical movement routes, and window/stair openings.

This diagnostic intentionally uses the real authored collision system with bounded samples to avoid multi-minute raycasting on a CI runner. Beckenburg #20 tracks the unresolved actual GPU screen-visibility proof.
