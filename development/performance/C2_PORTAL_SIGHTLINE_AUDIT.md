# C2 visibility safety audit: comparing portal graph against actual drawn geometry

**Status:** diagnostic-only until exact-commit test pass. **Parent:** C1 portal candidate graph and B2c room-owned static chunks.

The core architecture already gives us a reachability graph, but portal reachability is not actual camera occlusion. This candidate now compares every authored House shell face that passes the established per-face frustum test with the graph's candidate rooms. Unknown/unowned face attribution is tallied separately. No call to this diagnostic removes or alters geometry, ghosts, equipment, evidence or network authority.

The test emits a ten-state CSV in build/e2e/viewport-profile/portal-candidate-audit.csv: five House camera positions in closed and open-door states. Every visible face is accounted for and unknown/exterior camera states must fail open. Nonzero interior candidate misses are explicitly **evidence against enabling hard room culling**; they are not silently fixed by suppressing the data or weakening the functional test.

Next C milestones: audit window/void/stair/portal sightlines against the counts; build portal-plane clipping with verified authored bounds; test representative play eye paths and dynamic door/window transitions on real installed GPU; only then allow production room/portal rejection. Never use portal culling for gameplay state, ghost identity inference or equipment logic.

Portable adoption: see development/performance/portable/MILESTONES_A-G.md at main. For 2D board games, use actual viewport/tile visibility instead of copying this 3D graph.
