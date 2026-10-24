# Blackwater Hydroelectric Dam — edge blocking and sky inclusion

planning-only / no runtime geometry or navigation mutation

Dam abutments, reservoir and tailrace

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
| edge-03 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-04 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-05 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-06 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-07 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-08 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-09 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-10 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-11 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-12 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-13 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-14 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-15 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-16 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-17 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-18 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-19 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-20 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-21 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-22 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-23 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-24 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-25 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |

All base heights must be fitted to authoritative terrain, not Y0. The outer ring encloses source reservations but grants no extra walkable area. Fit it to actual terrain/architecture before construction.

Arrival: 8 m road portal reservation beyond staged van; retain its exact cargo route and fit a visible closed outer gate/road bend.

## Sky inclusion sectors

| Azimuth | Identity | Provisional crest / open sky above | Mid-distance |
|---|---|---|---|
| [0, 45]° | Reservoir surface and distant valley sides | [-0.5, 1.5]° / 1.5° | [500, 4000] m |
| [45, 90]° | Reservoir surface and distant valley sides transitioning to Guarded tailrace and cut rock abutment | [-0.5, 1.5]° / 1.5° | [500, 4000] m |
| [90, 135]° | Guarded tailrace and cut rock abutment | [4, 14]° / 14° | [200, 2000] m |
| [135, 180]° | Guarded tailrace and cut rock abutment transitioning to Service approach against high valley shoulder | [4, 14]° / 14° | [200, 2000] m |
| [180, 225]° | Service approach against high valley shoulder | [4, 14]° / 14° | [200, 2000] m |
| [225, 270]° | Service approach against high valley shoulder transitioning to Vegetated retaining bank and far ridge | [4, 14]° / 14° | [200, 2000] m |
| [270, 315]° | Vegetated retaining bank and far ridge | [4, 14]° / 14° | [200, 2000] m |
| [315, 360]° | Vegetated retaining bank and far ridge transitioning to Reservoir surface and distant valley sides | [4, 14]° / 14° | [200, 2000] m |

Sector heights/distances are original art-direction targets, not measured horizon coverage. Real camera captures must fit them, particularly from high towers and windows.

## Viewpoints

| Anchor | Eye position X/Y/Z | Source |
|---|---|---|
| van-arrival | [36.0, 1.62, 82.0] | Canonical staged van eye/roadhead; preserve cargo route |
| primary-approach | [36.0, 1.62, 69.0] | Authored approach-space reservation; actual viewpoint must be stood in |
| highest-accessible-layer | [36.0, 1.62, 82.0] | Highest authored standing zone; actual window/balcony aperture and guards still require review |
| outdoor-entry | [36.0, 1.62, 69.0] | Outdoor-space viewpoint reservation |
| outdoor-crest | [40.0, 1.62, 52.0] | Outdoor-space viewpoint reservation |
| outdoor-east-stair | [70.0, 1.62, 35.0] | Outdoor-space viewpoint reservation |
| outdoor-spillway | [70.0, -4.38, 55.0] | Outdoor-space viewpoint reservation |
| below-grade-seal | [50.0, -8.379999999999999, 27.0] | Negative test: no sky through sealed floor/wall/roof; allow only actual authored entrance/shaft apertures. |

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
