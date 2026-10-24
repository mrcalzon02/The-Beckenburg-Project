# Morrow Point Lightstation — edge blocking and sky inclusion

planning-only / no runtime geometry or navigation mutation

Lightstation headland with an exposed sea horizon

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
| edge-14 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-15 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-16 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-17 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-18 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-19 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-20 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-21 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-22 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-23 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-24 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |

All base heights must be fitted to authoritative terrain, not Y0. The outer ring encloses source reservations but grants no extra walkable area. Fit it to actual terrain/architecture before construction.

Arrival: 8 m road portal reservation beyond staged van; retain its exact cargo route and fit a visible closed outer gate/road bend.

## Sky inclusion sectors

| Azimuth | Identity | Provisional crest / open sky above | Mid-distance |
|---|---|---|---|
| [0, 45]° | Uninterrupted sea horizon beyond the lantern | [-0.5, 1.5]° / 1.5° | [500, 4000] m |
| [45, 90]° | Uninterrupted sea horizon beyond the lantern transitioning to Open sea with a very low distant headland | [-0.5, 1.5]° / 1.5° | [500, 4000] m |
| [90, 135]° | Open sea with a very low distant headland | [-0.5, 1.5]° / 1.5° | [500, 4000] m |
| [135, 180]° | Open sea with a very low distant headland transitioning to Keeper-house road recessed in coastal vegetation | [-0.5, 1.5]° / 1.5° | [500, 4000] m |
| [180, 225]° | Keeper-house road recessed in coastal vegetation | [4, 14]° / 14° | [200, 2000] m |
| [225, 270]° | Keeper-house road recessed in coastal vegetation transitioning to Rock headland and low windswept land | [4, 14]° / 14° | [200, 2000] m |
| [270, 315]° | Rock headland and low windswept land | [4, 14]° / 14° | [200, 2000] m |
| [315, 360]° | Rock headland and low windswept land transitioning to Uninterrupted sea horizon beyond the lantern | [4, 14]° / 14° | [200, 2000] m |

Sector heights/distances are original art-direction targets, not measured horizon coverage. Real camera captures must fit them, particularly from high towers and windows.

## Viewpoints

| Anchor | Eye position X/Y/Z | Source |
|---|---|---|
| van-arrival | [32.0, 1.62, 84.0] | Canonical staged van eye/roadhead; preserve cargo route |
| primary-approach | [32.0, 1.62, 72.0] | Authored approach-space reservation; actual viewpoint must be stood in |
| highest-accessible-layer | [34.8, 19.62, 10] | Highest authored standing zone; actual window/balcony aperture and guards still require review |
| outdoor-entry | [32.0, 1.62, 72.0] | Outdoor-space viewpoint reservation |
| outdoor-porch | [32.0, 1.62, 60.0] | Outdoor-space viewpoint reservation |
| outdoor-yard | [55.0, 1.62, 53.0] | Outdoor-space viewpoint reservation |

## Site-specific constraints

- Preserve open sea vistas from upper tower/director; do not replace coastal horizon with the shared all-map hill waveform.

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
