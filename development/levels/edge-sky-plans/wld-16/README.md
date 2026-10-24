# Eight Pines Motor Lodge — edge blocking and sky inclusion

planning-only / no runtime geometry or navigation mutation

Motor court roadside edge and service-bank backdrop

[Diagram](plan.svg) · [Exact planning contract](plan.json)

## Layer roles

- **near**: 0–12 m beyond accepted playable edge: grounded physical enclosure with irregular rhythm, visible support and closed returns.
- **middle**: World-space terrain/vegetation/buildings beyond boundary: no accessible paths or hidden evidence; parallax from player translation.
- **far**: Direction-fixed distant horizon/sky/weather; no collision. Per-map sector masks replace generic silhouette only in a separately reviewed renderer change.

## Physical edge reservations

| Span | Treatment | Height / depth reservation | Fit / containment |
|---|---|---|---|
| edge-01 | hedge | 2.3 / 2.6 m | Full visible woody/core volume, no crouch-sized holes; verify both approach directions. |
| edge-02 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-03 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-04 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-05 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-06 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-07 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-08 | fence | 2.2 / 0.25 m | Match visible fence envelope; avoid triangle/crouch gaps and terminate against real ground. |
| edge-09 | fence | 2.2 / 0.25 m | Match visible fence envelope; avoid triangle/crouch gaps and terminate against real ground. |
| edge-10 | fence | 2.2 / 0.25 m | Match visible fence envelope; avoid triangle/crouch gaps and terminate against real ground. |
| edge-11 | fence | 2.2 / 0.25 m | Match visible fence envelope; avoid triangle/crouch gaps and terminate against real ground. |
| edge-12 | fence | 2.2 / 0.25 m | Match visible fence envelope; avoid triangle/crouch gaps and terminate against real ground. |
| edge-13 | fence | 2.2 / 0.25 m | Match visible fence envelope; avoid triangle/crouch gaps and terminate against real ground. |
| edge-14 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-15 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-16 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-17 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-18 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-19 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-20 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-21 | hedge | 2.3 / 2.6 m | Full visible woody/core volume, no crouch-sized holes; verify both approach directions. |
| edge-22 | hedge | 2.3 / 2.6 m | Full visible woody/core volume, no crouch-sized holes; verify both approach directions. |
| edge-23 | hedge | 2.3 / 2.6 m | Full visible woody/core volume, no crouch-sized holes; verify both approach directions. |
| edge-24 | hedge | 2.3 / 2.6 m | Full visible woody/core volume, no crouch-sized holes; verify both approach directions. |

All base heights must be fitted to authoritative terrain, not Y0. The outer ring encloses source reservations but grants no extra walkable area. Fit it to actual terrain/architecture before construction.

Arrival: 8 m road portal reservation beyond staged van; retain its exact cargo route and fit a visible closed outer gate/road bend.

## Sky inclusion sectors

| Azimuth | Identity | Provisional crest / open sky above | Mid-distance |
|---|---|---|---|
| [0, 45]° | Low pine ridge behind guest rooms | [4, 14]° / 14° | [200, 2000] m |
| [45, 90]° | Low pine ridge behind guest rooms transitioning to Screened service edge and distant trees | [4, 14]° / 14° | [200, 2000] m |
| [90, 135]° | Screened service edge and distant trees | [4, 12]° / 12° | [60, 450] m |
| [135, 180]° | Screened service edge and distant trees transitioning to Open roadside view between fictional sign and reception | [4, 12]° / 12° | [60, 450] m |
| [180, 225]° | Open roadside view between fictional sign and reception | [1, 4]° / 4° | [150, 1500] m |
| [225, 270]° | Open roadside view between fictional sign and reception transitioning to Uneven hedge and scattered roadside trees | [1, 4]° / 4° | [150, 1500] m |
| [270, 315]° | Uneven hedge and scattered roadside trees | [4, 12]° / 12° | [60, 450] m |
| [315, 360]° | Uneven hedge and scattered roadside trees transitioning to Low pine ridge behind guest rooms | [4, 12]° / 12° | [60, 450] m |

Sector heights/distances are original art-direction targets, not measured horizon coverage. Real camera captures must fit them, particularly from high towers and windows.

## Viewpoints

| Anchor | Eye position X/Y/Z | Source |
|---|---|---|
| van-arrival | [26.0, 1.62, 68.0] | Canonical staged van eye/roadhead; preserve cargo route |
| primary-approach | [26.0, 1.62, 57.0] | Authored approach-space reservation; actual viewpoint must be stood in |
| highest-accessible-layer | [26.0, 1.62, 68.0] | Highest authored standing zone; actual window/balcony aperture and guards still require review |
| outdoor-entry | [26.0, 1.62, 57.0] | Outdoor-space viewpoint reservation |
| outdoor-court | [26.0, 1.62, 31.0] | Outdoor-space viewpoint reservation |
| outdoor-westwalk | [14.5, 1.62, 34.0] | Outdoor-space viewpoint reservation |
| outdoor-eastwalk | [37.5, 1.62, 34.0] | Outdoor-space viewpoint reservation |
| outdoor-northwalk | [26.0, 1.62, 18.5] | Outdoor-space viewpoint reservation |
| outdoor-service | [51.0, 1.62, 37.0] | Outdoor-space viewpoint reservation |

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
