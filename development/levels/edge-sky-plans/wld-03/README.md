# Red Mesa Ancestral Caves — edge blocking and sky inclusion

planning-only / no runtime geometry or navigation mutation

Canyon rim, recesses and distant mesa shelves

[Diagram](plan.svg) · [Exact planning contract](plan.json)

## Layer roles

- **near**: 0–12 m beyond accepted playable edge: grounded physical enclosure with irregular rhythm, visible support and closed returns.
- **middle**: World-space terrain/vegetation/buildings beyond boundary: no accessible paths or hidden evidence; parallax from player translation.
- **far**: Direction-fixed distant horizon/sky/weather; no collision. Per-map sector masks replace generic silhouette only in a separately reviewed renderer change.

## Physical edge reservations

| Span | Treatment | Height / depth reservation | Fit / containment |
|---|---|---|---|
| edge-01 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-02 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-03 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-04 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-05 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-06 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-07 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-08 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-09 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
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
| edge-26 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-27 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |

All base heights must be fitted to authoritative terrain, not Y0. The outer ring encloses source reservations but grants no extra walkable area. Fit it to actual terrain/architecture before construction.

Arrival: 8 m road portal reservation beyond staged van; retain its exact cargo route and fit a visible closed outer gate/road bend.

## Sky inclusion sectors

| Azimuth | Identity | Provisional crest / open sky above | Mid-distance |
|---|---|---|---|
| [0, 45]° | Broken mesa skyline above canyon opening | [8, 25]° / 25° | [150, 2500] m |
| [45, 90]° | Broken mesa skyline above canyon opening transitioning to Layered canyon rim and narrow sky slots | [8, 25]° / 25° | [150, 2500] m |
| [90, 135]° | Layered canyon rim and narrow sky slots | [8, 25]° / 25° | [150, 2500] m |
| [135, 180]° | Layered canyon rim and narrow sky slots transitioning to Access wash bending between red-rock shoulders | [8, 25]° / 25° | [150, 2500] m |
| [180, 225]° | Access wash bending between red-rock shoulders | [8, 25]° / 25° | [150, 2500] m |
| [225, 270]° | Access wash bending between red-rock shoulders transitioning to Eroded rock buttresses and far mesa | [8, 25]° / 25° | [150, 2500] m |
| [270, 315]° | Eroded rock buttresses and far mesa | [8, 25]° / 25° | [150, 2500] m |
| [315, 360]° | Eroded rock buttresses and far mesa transitioning to Broken mesa skyline above canyon opening | [8, 25]° / 25° | [150, 2500] m |

Sector heights/distances are original art-direction targets, not measured horizon coverage. Real camera captures must fit them, particularly from high towers and windows.

## Viewpoints

| Anchor | Eye position X/Y/Z | Source |
|---|---|---|
| van-arrival | [34.0, 1.62, 86.0] | Canonical staged van eye/roadhead; preserve cargo route |
| primary-approach | [34.0, 1.62, 73.0] | Authored approach-space reservation; actual viewpoint must be stood in |
| highest-accessible-layer | [34.0, 9.620000000000001, 16.0] | Highest authored standing zone; actual window/balcony aperture and guards still require review |
| outdoor-entry | [34.0, 1.62, 73.0] | Outdoor-space viewpoint reservation |
| outdoor-floor | [34.0, 1.62, 53.0] | Outdoor-space viewpoint reservation |
| outdoor-lower-path | [12.0, 5.62, 34.0] | Outdoor-space viewpoint reservation |
| outdoor-upper-path | [34.0, 9.620000000000001, 16.0] | Outdoor-space viewpoint reservation |
| outdoor-dwellings | [34.0, 9.620000000000001, 31.0] | Outdoor-space viewpoint reservation |
| outdoor-return | [57.0, 5.62, 13.0] | Outdoor-space viewpoint reservation |

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
