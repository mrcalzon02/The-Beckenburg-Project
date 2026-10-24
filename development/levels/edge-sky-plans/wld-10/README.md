# Monastery of Ashen Bells — edge blocking and sky inclusion

planning-only / no runtime geometry or navigation mutation

Monastery wall, terraces and high garden views

[Diagram](plan.svg) · [Exact planning contract](plan.json)

## Layer roles

- **near**: 0–12 m beyond accepted playable edge: grounded physical enclosure with irregular rhythm, visible support and closed returns.
- **middle**: World-space terrain/vegetation/buildings beyond boundary: no accessible paths or hidden evidence; parallax from player translation.
- **far**: Direction-fixed distant horizon/sky/weather; no collision. Per-map sector masks replace generic silhouette only in a separately reviewed renderer change.

## Physical edge reservations

| Span | Treatment | Height / depth reservation | Fit / containment |
|---|---|---|---|
| edge-01 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-02 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-03 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-04 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-05 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-06 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-07 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-08 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-09 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-10 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-11 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-12 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-13 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-14 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-15 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-16 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-17 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-18 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-19 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-20 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-21 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-22 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-23 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-24 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-25 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |

All base heights must be fitted to authoritative terrain, not Y0. The outer ring encloses source reservations but grants no extra walkable area. Fit it to actual terrain/architecture before construction.

Arrival: 8 m road portal reservation beyond staged van; retain its exact cargo route and fit a visible closed outer gate/road bend.

## Sky inclusion sectors

| Azimuth | Identity | Provisional crest / open sky above | Mid-distance |
|---|---|---|---|
| [0, 45]° | Broken monastery roofline and far hills | [4, 14]° / 14° | [200, 2000] m |
| [45, 90]° | Broken monastery roofline and far hills transitioning to Valley view beyond guarded terrace | [4, 14]° / 14° | [200, 2000] m |
| [90, 135]° | Valley view beyond guarded terrace | [8, 22]° / 22° | [700, 5000] m |
| [135, 180]° | Valley view beyond guarded terrace transitioning to Walled approach with a recessed service gate | [8, 22]° / 22° | [700, 5000] m |
| [180, 225]° | Walled approach with a recessed service gate | [4, 12]° / 12° | [60, 450] m |
| [225, 270]° | Walled approach with a recessed service gate transitioning to Garden bank and layered cypress silhouettes | [4, 12]° / 12° | [60, 450] m |
| [270, 315]° | Garden bank and layered cypress silhouettes | [4, 12]° / 12° | [60, 450] m |
| [315, 360]° | Garden bank and layered cypress silhouettes transitioning to Broken monastery roofline and far hills | [4, 12]° / 12° | [60, 450] m |

Sector heights/distances are original art-direction targets, not measured horizon coverage. Real camera captures must fit them, particularly from high towers and windows.

## Viewpoints

| Anchor | Eye position X/Y/Z | Source |
|---|---|---|
| van-arrival | [32.0, 1.62, 80.0] | Canonical staged van eye/roadhead; preserve cargo route |
| primary-approach | [32.0, 1.62, 68.0] | Authored approach-space reservation; actual viewpoint must be stood in |
| highest-accessible-layer | [55.0, 7.62, 23.0] | Highest authored standing zone; actual window/balcony aperture and guards still require review |
| outdoor-entry | [32.0, 1.62, 68.0] | Outdoor-space viewpoint reservation |
| outdoor-garden | [29.0, 1.62, 36.0] | Outdoor-space viewpoint reservation |
| outdoor-rear | [64.0, 1.62, 23.0] | Outdoor-space viewpoint reservation |
| below-grade-seal | [29.0, -1.38, 19.0] | Negative test: no sky through sealed floor/wall/roof; allow only actual authored entrance/shaft apertures. |

## Site-specific constraints


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
