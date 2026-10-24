# Site Cinder-6 — edge blocking and sky inclusion

planning-only / no runtime geometry or navigation mutation

Decommissioned compound with service and escape access

[Diagram](plan.svg) · [Exact planning contract](plan.json)

## Layer roles

- **near**: 0–12 m beyond accepted playable edge: grounded physical enclosure with irregular rhythm, visible support and closed returns.
- **middle**: World-space terrain/vegetation/buildings beyond boundary: no accessible paths or hidden evidence; parallax from player translation.
- **far**: Direction-fixed distant horizon/sky/weather; no collision. Per-map sector masks replace generic silhouette only in a separately reviewed renderer change.

## Physical edge reservations

| Span | Treatment | Height / depth reservation | Fit / containment |
|---|---|---|---|
| edge-01 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-02 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-03 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-04 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-05 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-06 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-07 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-08 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-09 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-10 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-11 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-12 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-13 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-14 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-15 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-16 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-17 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-18 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-19 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-20 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-21 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-22 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-23 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-24 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-25 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-26 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |

All base heights must be fitted to authoritative terrain, not Y0. The outer ring encloses source reservations but grants no extra walkable area. Fit it to actual terrain/architecture before construction.

Arrival: 8 m road portal reservation beyond staged van; retain its exact cargo route and fit a visible closed outer gate/road bend.

## Sky inclusion sectors

| Azimuth | Identity | Provisional crest / open sky above | Mid-distance |
|---|---|---|---|
| [0, 45]° | Low scrub ridge beyond compound fencing | [4, 14]° / 14° | [200, 2000] m |
| [45, 90]° | Low scrub ridge beyond compound fencing transitioning to Uneven maintenance bank and far woodland | [4, 14]° / 14° | [200, 2000] m |
| [90, 135]° | Uneven maintenance bank and far woodland | [4, 12]° / 12° | [60, 450] m |
| [135, 180]° | Uneven maintenance bank and far woodland transitioning to Service road disappearing below a low ridge | [4, 12]° / 12° | [60, 450] m |
| [180, 225]° | Service road disappearing below a low ridge | [4, 14]° / 14° | [200, 2000] m |
| [225, 270]° | Service road disappearing below a low ridge transitioning to Fenced escape-bank edge and sparse distant trees | [4, 14]° / 14° | [200, 2000] m |
| [270, 315]° | Fenced escape-bank edge and sparse distant trees | [4, 12]° / 12° | [60, 450] m |
| [315, 360]° | Fenced escape-bank edge and sparse distant trees transitioning to Low scrub ridge beyond compound fencing | [4, 12]° / 12° | [60, 450] m |

Sector heights/distances are original art-direction targets, not measured horizon coverage. Real camera captures must fit them, particularly from high towers and windows.

## Viewpoints

| Anchor | Eye position X/Y/Z | Source |
|---|---|---|
| van-arrival | [32.0, 1.62, 90.0] | Canonical staged van eye/roadhead; preserve cargo route |
| primary-approach | [32.0, 1.62, 77.0] | Authored approach-space reservation; actual viewpoint must be stood in |
| highest-accessible-layer | [32.0, 1.62, 90.0] | Highest authored standing zone; actual window/balcony aperture and guards still require review |
| outdoor-entry | [32.0, 1.62, 77.0] | Outdoor-space viewpoint reservation |
| outdoor-yard | [40.0, 1.62, 62.0] | Outdoor-space viewpoint reservation |
| outdoor-upperring | [23, -2.38, 28] | Outdoor-space viewpoint reservation |
| outdoor-lowring | [23, -6.38, 28] | Outdoor-space viewpoint reservation |
| below-grade-seal | [23, -6.38, 28] | Negative test: no sky through sealed floor/wall/roof; allow only actual authored entrance/shaft apertures. |

## Site-specific constraints

- Empty silo shaft is an internal guarded hazard, not an outer boundary or a sky-textured floor; inspect ring views upward through only the real shaft opening.

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
