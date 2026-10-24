# Camp Hollow Creek — edge blocking and sky inclusion

planning-only / no runtime geometry or navigation mutation

Camp woodland edge, open lake and creek corridor

[Diagram](plan.svg) · [Exact planning contract](plan.json)

## Layer roles

- **near**: 0–12 m beyond accepted playable edge: grounded physical enclosure with irregular rhythm, visible support and closed returns.
- **middle**: World-space terrain/vegetation/buildings beyond boundary: no accessible paths or hidden evidence; parallax from player translation.
- **far**: Direction-fixed distant horizon/sky/weather; no collision. Per-map sector masks replace generic silhouette only in a separately reviewed renderer change.

## Physical edge reservations

| Span | Treatment | Height / depth reservation | Fit / containment |
|---|---|---|---|
| edge-01 | logfall | 2.0 / 4.5 m | A single low log is traversable decoration. Use continuous visible tangled volume and bank; test crawl/climb gaps. |
| edge-02 | logfall | 2.0 / 4.5 m | A single low log is traversable decoration. Use continuous visible tangled volume and bank; test crawl/climb gaps. |
| edge-03 | logfall | 2.0 / 4.5 m | A single low log is traversable decoration. Use continuous visible tangled volume and bank; test crawl/climb gaps. |
| edge-04 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-05 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-06 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-07 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-08 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-09 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-10 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-11 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-12 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-13 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-14 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-15 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-16 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-17 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-18 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-19 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-20 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-21 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-22 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-23 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-24 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-25 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-26 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-27 | logfall | 2.0 / 4.5 m | A single low log is traversable decoration. Use continuous visible tangled volume and bank; test crawl/climb gaps. |
| edge-28 | logfall | 2.0 / 4.5 m | A single low log is traversable decoration. Use continuous visible tangled volume and bank; test crawl/climb gaps. |

All base heights must be fitted to authoritative terrain, not Y0. The outer ring encloses source reservations but grants no extra walkable area. Fit it to actual terrain/architecture before construction.

Arrival: 8 m road portal reservation beyond staged van; retain its exact cargo route and fit a visible closed outer gate/road bend.

## Sky inclusion sectors

| Azimuth | Identity | Provisional crest / open sky above | Mid-distance |
|---|---|---|---|
| [0, 45]° | Low open lake horizon and distant wooded shore | [-0.5, 1.5]° / 1.5° | [500, 4000] m |
| [45, 90]° | Low open lake horizon and distant wooded shore transitioning to Creek-cut bank and woodland shoulder | [-0.5, 1.5]° / 1.5° | [500, 4000] m |
| [90, 135]° | Creek-cut bank and woodland shoulder | [4, 12]° / 12° | [60, 450] m |
| [135, 180]° | Creek-cut bank and woodland shoulder transitioning to Camp road curving back through conifers | [4, 12]° / 12° | [60, 450] m |
| [180, 225]° | Camp road curving back through conifers | [4, 12]° / 12° | [60, 450] m |
| [225, 270]° | Camp road curving back through conifers transitioning to Fallen timber and rising wooded ground | [4, 12]° / 12° | [60, 450] m |
| [270, 315]° | Fallen timber and rising wooded ground | [4, 14]° / 14° | [200, 2000] m |
| [315, 360]° | Fallen timber and rising wooded ground transitioning to Low open lake horizon and distant wooded shore | [4, 14]° / 14° | [200, 2000] m |

Sector heights/distances are original art-direction targets, not measured horizon coverage. Real camera captures must fit them, particularly from high towers and windows.

## Viewpoints

| Anchor | Eye position X/Y/Z | Source |
|---|---|---|
| van-arrival | [32, 3.12, 78] | Canonical staged van eye/roadhead; preserve cargo route |
| primary-approach | [32.0, 2.62, 66.0] | Authored approach-space reservation; actual viewpoint must be stood in |
| highest-accessible-layer | [32.0, 3.12, 78.0] | Highest authored standing zone; actual window/balcony aperture and guards still require review |
| outdoor-entry | [32.0, 2.62, 66.0] | Outdoor-space viewpoint reservation |
| outdoor-camp-a | [14.0, 1.34, 37.0] | Outdoor-space viewpoint reservation |
| outdoor-camp-b | [50.0, 1.7200000000000002, 37.0] | Outdoor-space viewpoint reservation |
| outdoor-junction | [32.0, 0.9700000000000001, 27.0] | Outdoor-space viewpoint reservation |
| outdoor-shore | [32.0, 0.32000000000000006, 12.0] | Outdoor-space viewpoint reservation |
| outdoor-dock | [28.0, 0.32000000000000006, -3.0] | Outdoor-space viewpoint reservation |
| outdoor-creek | [48.0, 0.6200000000000001, 13.0] | Outdoor-space viewpoint reservation |
| below-grade-seal | [32.0, 0.32000000000000006, 12.0] | Negative test: no sky through sealed floor/wall/roof; allow only actual authored entrance/shaft apertures. |

## Site-specific constraints

- Existing site_perimeter describes the land edge only: extend the containment review around the real dock and creek crossing. Original native terrain and its perimeter are reference inputs, unchanged.

## Inclusion and separation rules

- Sky exists behind real world geometry and must not write foreground depth. Closed opaque rooms occlude sky; actual windows and open courtyards reveal it.
- No backdrop may appear through a wall/floor/roof join, under an upper deck or through a light-leak seam. Apply the shared defect checklist.
- Keep the water/sky horizon level and continuous; layered water transparency/depth remains a separate acceptance gate.
- Do not use fog/darkness/painted horizon as a physical barrier or conceal unsupported terrain edges.
- Near occluders must frame vistas without cutting routes, door swings, van/ramp, ritual squares or sparse hiding pockets.
- No player-facing map detail should depend on distant scenery collision. Keep ghost/human containment rules explicit and host-authoritative.
- Highest windows, towers and platforms need a larger visible terrain apron and farther horizon than ground-only cameras.
- No stock sky texture has been acquired or licensed by this plan. Use existing procedural sky/weather; any new bitmap needs separate source/license intake.

## Next gates

- Fit proposed perimeter to actual native terrain and existing enclosure; extend off-map terrain before placing any new barrier.
- Author supported barrier spans, road portal and closed returns as selected additive geometry; inspect all overlap/clearance.
- Run swept human/ghost/crouch and under/over/around checks, water/fall recovery and van extraction.
- Render ordinary-viewpoint frame captures and verify no sky leaks, near-card artifacts or horizon discontinuity; gallery publish.
- Integrate per-map scenery/sky profile only after engine branch coordination and CPU/GPU parity/performance review.

No boundary continuity, collision, actual screenshot framing, runtime sky mask or gameplay acceptance is claimed.
