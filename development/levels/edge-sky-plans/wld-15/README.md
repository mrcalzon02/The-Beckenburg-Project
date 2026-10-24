# MV Saint Elara — edge blocking and sky inclusion

planning-only / no runtime geometry or navigation mutation

Moored vessel within a quayside investigation envelope

[Diagram](plan.svg) · [Exact planning contract](plan.json)

## Layer roles

- **near**: 0–12 m beyond accepted playable edge: grounded physical enclosure with irregular rhythm, visible support and closed returns.
- **middle**: World-space terrain/vegetation/buildings beyond boundary: no accessible paths or hidden evidence; parallax from player translation.
- **far**: Direction-fixed distant horizon/sky/weather; no collision. Per-map sector masks replace generic silhouette only in a separately reviewed renderer change.

## Physical edge reservations

| Span | Treatment | Height / depth reservation | Fit / containment |
|---|---|---|---|
| edge-01 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-02 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-03 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-04 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-05 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-06 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-07 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-08 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-09 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-10 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-11 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-12 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-13 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-14 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-15 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-16 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-17 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-18 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-19 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-20 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-21 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-22 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-23 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-24 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-25 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |

All base heights must be fitted to authoritative terrain, not Y0. The outer ring encloses source reservations but grants no extra walkable area. Fit it to actual terrain/architecture before construction.

Arrival: 8 m road portal reservation beyond staged van; retain its exact cargo route and fit a visible closed outer gate/road bend.

## Sky inclusion sectors

| Azimuth | Identity | Provisional crest / open sky above | Mid-distance |
|---|---|---|---|
| [0, 45]° | Open harbor water and distant breakwater | [-0.5, 1.5]° / 1.5° | [500, 4000] m |
| [45, 90]° | Open harbor water and distant breakwater transitioning to Low open water vista beside guarded vessel rails | [-0.5, 1.5]° / 1.5° | [500, 4000] m |
| [90, 135]° | Low open water vista beside guarded vessel rails | [-0.5, 1.5]° / 1.5° | [500, 4000] m |
| [135, 180]° | Low open water vista beside guarded vessel rails transitioning to Quay access and distant warehouse silhouettes | [-0.5, 1.5]° / 1.5° | [500, 4000] m |
| [180, 225]° | Quay access and distant warehouse silhouettes | [3, 9]° / 9° | [100, 800] m |
| [225, 270]° | Quay access and distant warehouse silhouettes transitioning to Quay fence and industrial harbor backdrop | [3, 9]° / 9° | [100, 800] m |
| [270, 315]° | Quay fence and industrial harbor backdrop | [3, 9]° / 9° | [100, 800] m |
| [315, 360]° | Quay fence and industrial harbor backdrop transitioning to Open harbor water and distant breakwater | [3, 9]° / 9° | [100, 800] m |

Sector heights/distances are original art-direction targets, not measured horizon coverage. Real camera captures must fit them, particularly from high towers and windows.

## Viewpoints

| Anchor | Eye position X/Y/Z | Source |
|---|---|---|
| van-arrival | [32.0, 1.62, 86.0] | Canonical staged van eye/roadhead; preserve cargo route |
| primary-approach | [32.0, 1.62, 78.0] | Authored approach-space reservation; actual viewpoint must be stood in |
| highest-accessible-layer | [32.0, 7.62, 24.0] | Highest authored standing zone; actual window/balcony aperture and guards still require review |
| outdoor-entry | [32.0, 1.62, 78.0] | Outdoor-space viewpoint reservation |
| outdoor-quay | [32.0, 1.62, 70.0] | Outdoor-space viewpoint reservation |
| outdoor-portpier | [19.0, 1.62, 56.0] | Outdoor-space viewpoint reservation |
| outdoor-aft | [32.0, 1.62, 47.0] | Outdoor-space viewpoint reservation |
| outdoor-side | [41.0, 1.62, 24.0] | Outdoor-space viewpoint reservation |
| outdoor-fore | [32.0, 1.62, 3.0] | Outdoor-space viewpoint reservation |
| below-grade-seal | [32.0, -1.38, 32.0] | Negative test: no sky through sealed floor/wall/roof; allow only actual authored entrance/shaft apertures. |

## Site-specific constraints

- Vessel rail/hull/gangway is the inner physical boundary. Harbor is visual background; quay roadhead remains accessible by both gangways. No moving sky/horizon tied to ship motion.

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
