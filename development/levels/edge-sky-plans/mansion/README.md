# Blackthorn Manor — edge blocking and sky inclusion

planning-only / no runtime geometry or navigation mutation

Manor terraces and wooded estate limits

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
| edge-03 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-04 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-05 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-06 | bank | 2.5 / 5.0 m | Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams. |
| edge-07 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-08 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-09 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-10 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-11 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-12 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-13 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-14 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-15 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-16 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-17 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-18 | hedge | 2.3 / 2.6 m | Full visible woody/core volume, no crouch-sized holes; verify both approach directions. |
| edge-19 | hedge | 2.3 / 2.6 m | Full visible woody/core volume, no crouch-sized holes; verify both approach directions. |
| edge-20 | hedge | 2.3 / 2.6 m | Full visible woody/core volume, no crouch-sized holes; verify both approach directions. |
| edge-21 | hedge | 2.3 / 2.6 m | Full visible woody/core volume, no crouch-sized holes; verify both approach directions. |

All base heights must be fitted to authoritative terrain, not Y0. The outer ring encloses source reservations but grants no extra walkable area. Fit it to actual terrain/architecture before construction.

Arrival: 8 m road portal reservation beyond staged van; retain its exact cargo route and fit a visible closed outer gate/road bend.

## Sky inclusion sectors

| Azimuth | Identity | Provisional crest / open sky above | Mid-distance |
|---|---|---|---|
| [0, 45]° | Estate ridge above the upper garden | [4, 14]° / 14° | [200, 2000] m |
| [45, 90]° | Estate ridge above the upper garden transitioning to Terraced stone boundary and far woodland | [4, 14]° / 14° | [200, 2000] m |
| [90, 135]° | Terraced stone boundary and far woodland | [4, 12]° / 12° | [60, 450] m |
| [135, 180]° | Terraced stone boundary and far woodland transitioning to Formal drive disappearing into trees | [4, 12]° / 12° | [60, 450] m |
| [180, 225]° | Formal drive disappearing into trees | [4, 12]° / 12° | [60, 450] m |
| [225, 270]° | Formal drive disappearing into trees transitioning to Overgrown garden and orchard canopy | [4, 12]° / 12° | [60, 450] m |
| [270, 315]° | Overgrown garden and orchard canopy | [4, 12]° / 12° | [60, 450] m |
| [315, 360]° | Overgrown garden and orchard canopy transitioning to Estate ridge above the upper garden | [4, 12]° / 12° | [60, 450] m |

Sector heights/distances are original art-direction targets, not measured horizon coverage. Real camera captures must fit them, particularly from high towers and windows.

## Viewpoints

| Anchor | Eye position X/Y/Z | Source |
|---|---|---|
| van-arrival | [28.0, 1.62, 71.0] | Canonical staged van eye/roadhead; preserve cargo route |
| primary-approach | [28.0, 1.62, 60.0] | Authored approach-space reservation; actual viewpoint must be stood in |
| highest-accessible-layer | [12.0, 8.82, 36.0] | Highest authored standing zone; actual window/balcony aperture and guards still require review |
| outdoor-entry | [28.0, 1.62, 60.0] | Outdoor-space viewpoint reservation |
| outdoor-garden | [16.0, 1.62, 16.0] | Outdoor-space viewpoint reservation |
| below-grade-seal | [45.0, -1.38, 21.0] | Negative test: no sky through sealed floor/wall/roof; allow only actual authored entrance/shaft apertures. |

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
