# Widow's Peak Fire Lookout — edge blocking and sky inclusion

planning-only / no runtime geometry or navigation mutation

High lookout landscape with long elevated views

[Diagram](plan.svg) · [Exact planning contract](plan.json)

## Layer roles

- **near**: 0–12 m beyond accepted playable edge: grounded physical enclosure with irregular rhythm, visible support and closed returns.
- **middle**: World-space terrain/vegetation/buildings beyond boundary: no accessible paths or hidden evidence; parallax from player translation.
- **far**: Direction-fixed distant horizon/sky/weather; no collision. Per-map sector masks replace generic silhouette only in a separately reviewed renderer change.

## Physical edge reservations

| Span | Treatment | Height / depth reservation | Fit / containment |
|---|---|---|---|
| edge-01 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-02 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-03 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-04 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-05 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-06 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-07 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-08 | cliff | 2.0 / 5.0 m | Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery. |
| edge-09 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-10 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-11 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-12 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-13 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-14 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-15 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-16 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-17 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-18 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-19 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-20 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-21 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-22 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-23 | logfall | 2.0 / 4.5 m | A single low log is traversable decoration. Use continuous visible tangled volume and bank; test crawl/climb gaps. |
| edge-24 | logfall | 2.0 / 4.5 m | A single low log is traversable decoration. Use continuous visible tangled volume and bank; test crawl/climb gaps. |
| edge-25 | logfall | 2.0 / 4.5 m | A single low log is traversable decoration. Use continuous visible tangled volume and bank; test crawl/climb gaps. |
| edge-26 | logfall | 2.0 / 4.5 m | A single low log is traversable decoration. Use continuous visible tangled volume and bank; test crawl/climb gaps. |
| edge-27 | logfall | 2.0 / 4.5 m | A single low log is traversable decoration. Use continuous visible tangled volume and bank; test crawl/climb gaps. |

All base heights must be fitted to authoritative terrain, not Y0. The outer ring encloses source reservations but grants no extra walkable area. Fit it to actual terrain/architecture before construction.

Arrival: 8 m road portal reservation beyond staged van; retain its exact cargo route and fit a visible closed outer gate/road bend.

## Sky inclusion sectors

| Azimuth | Identity | Provisional crest / open sky above | Mid-distance |
|---|---|---|---|
| [0, 45]° | Multiple receding mountain ridges | [8, 22]° / 22° | [700, 5000] m |
| [45, 90]° | Multiple receding mountain ridges transitioning to Wind-shaped forest slope below the tower | [8, 22]° / 22° | [700, 5000] m |
| [90, 135]° | Wind-shaped forest slope below the tower | [4, 14]° / 14° | [200, 2000] m |
| [135, 180]° | Wind-shaped forest slope below the tower transitioning to Access track swallowed by woodland | [4, 14]° / 14° | [200, 2000] m |
| [180, 225]° | Access track swallowed by woodland | [4, 12]° / 12° | [60, 450] m |
| [225, 270]° | Access track swallowed by woodland transitioning to Open valley framed by close conifers | [4, 12]° / 12° | [60, 450] m |
| [270, 315]° | Open valley framed by close conifers | [8, 22]° / 22° | [700, 5000] m |
| [315, 360]° | Open valley framed by close conifers transitioning to Multiple receding mountain ridges | [8, 22]° / 22° | [700, 5000] m |

Sector heights/distances are original art-direction targets, not measured horizon coverage. Real camera captures must fit them, particularly from high towers and windows.

## Viewpoints

| Anchor | Eye position X/Y/Z | Source |
|---|---|---|
| van-arrival | [28.0, 1.62, 78.0] | Canonical staged van eye/roadhead; preserve cargo route |
| primary-approach | [28.0, 1.62, 66.0] | Authored approach-space reservation; actual viewpoint must be stood in |
| highest-accessible-layer | [27.0, 16.62, 5.0] | Highest authored standing zone; actual window/balcony aperture and guards still require review |
| outdoor-entry | [28.0, 1.62, 66.0] | Outdoor-space viewpoint reservation |
| outdoor-lower | [14.0, 1.62, 51.0] | Outdoor-space viewpoint reservation |
| outdoor-bend | [28.0, 4.62, 36.0] | Outdoor-space viewpoint reservation |
| outdoor-clearing | [30.0, 7.62, 22.0] | Outdoor-space viewpoint reservation |
| outdoor-return | [43.0, 4.62, 51.0] | Outdoor-space viewpoint reservation |
| outdoor-foot | [30.0, 7.62, 8.0] | Outdoor-space viewpoint reservation |
| outdoor-deck | [33.0, 16.62, 5.0] | Outdoor-space viewpoint reservation |

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
