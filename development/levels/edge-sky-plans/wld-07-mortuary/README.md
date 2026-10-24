# Saint Bartholomew's Cemetery & Vale Mortuary — edge blocking and sky inclusion

planning-only / no runtime geometry or navigation mutation

Expanded cemetery and screened mortuary service boundary

[Diagram](plan.svg) · [Exact planning contract](plan.json)

## Layer roles

- **near**: 0–12 m beyond accepted playable edge: grounded physical enclosure with irregular rhythm, visible support and closed returns.
- **middle**: World-space terrain/vegetation/buildings beyond boundary: no accessible paths or hidden evidence; parallax from player translation.
- **far**: Direction-fixed distant horizon/sky/weather; no collision. Per-map sector masks replace generic silhouette only in a separately reviewed renderer change.

## Physical edge reservations

| Span | Treatment | Height / depth reservation | Fit / containment |
|---|---|---|---|
| edge-01 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-02 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-03 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-04 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-05 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-06 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-07 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-08 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-09 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-10 | hedge | 2.3 / 2.6 m | Full visible woody/core volume, no crouch-sized holes; verify both approach directions. |
| edge-11 | hedge | 2.3 / 2.6 m | Full visible woody/core volume, no crouch-sized holes; verify both approach directions. |
| edge-12 | hedge | 2.3 / 2.6 m | Full visible woody/core volume, no crouch-sized holes; verify both approach directions. |
| edge-13 | hedge | 2.3 / 2.6 m | Full visible woody/core volume, no crouch-sized holes; verify both approach directions. |
| edge-14 | hedge | 2.3 / 2.6 m | Full visible woody/core volume, no crouch-sized holes; verify both approach directions. |
| edge-15 | hedge | 2.3 / 2.6 m | Full visible woody/core volume, no crouch-sized holes; verify both approach directions. |
| edge-16 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-17 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-18 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-19 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-20 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-21 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-22 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-23 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-24 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-25 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-26 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-27 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |
| edge-28 | wall | 2.0 / 0.6 m | Closed wall solid following visible faces; low decorative sections require a backed bank or railing. |

All base heights must be fitted to authoritative terrain, not Y0. The outer ring encloses source reservations but grants no extra walkable area. Fit it to actual terrain/architecture before construction.

Arrival: 8 m road portal reservation beyond staged van; retain its exact cargo route and fit a visible closed outer gate/road bend.

## Sky inclusion sectors

| Azimuth | Identity | Provisional crest / open sky above | Mid-distance |
|---|---|---|---|
| [0, 45]° | Original cemetery tree frame and parish roof | [4, 12]° / 12° | [60, 450] m |
| [45, 90]° | Original cemetery tree frame and parish roof transitioning to Screened mortuary service edge with quiet distant trees | [4, 12]° / 12° | [60, 450] m |
| [90, 135]° | Screened mortuary service edge with quiet distant trees | [4, 12]° / 12° | [60, 450] m |
| [135, 180]° | Screened mortuary service edge with quiet distant trees transitioning to Lane access kept clear of reception and service circulation | [4, 12]° / 12° | [60, 450] m |
| [180, 225]° | Lane access kept clear of reception and service circulation | [3, 9]° / 9° | [100, 800] m |
| [225, 270]° | Lane access kept clear of reception and service circulation transitioning to Original cemetery wall and woodland frame | [3, 9]° / 9° | [100, 800] m |
| [270, 315]° | Original cemetery wall and woodland frame | [4, 12]° / 12° | [60, 450] m |
| [315, 360]° | Original cemetery wall and woodland frame transitioning to Original cemetery tree frame and parish roof | [4, 12]° / 12° | [60, 450] m |

Sector heights/distances are original art-direction targets, not measured horizon coverage. Real camera captures must fit them, particularly from high towers and windows.

## Viewpoints

| Anchor | Eye position X/Y/Z | Source |
|---|---|---|
| van-arrival | [28.0, 1.62, 78.0] | Canonical staged van eye/roadhead; preserve cargo route |
| primary-approach | [28.0, 1.62, 66.0] | Authored approach-space reservation; actual viewpoint must be stood in |
| highest-accessible-layer | [28.0, 1.62, 78.0] | Highest authored standing zone; actual window/balcony aperture and guards still require review |
| outdoor-entry | [28.0, 1.62, 66.0] | Outdoor-space viewpoint reservation |
| outdoor-avenue | [28.0, 1.62, 40.0] | Outdoor-space viewpoint reservation |
| outdoor-garden | [15.0, 1.62, 29.0] | Outdoor-space viewpoint reservation |
| outdoor-east-graves | [41.0, 1.62, 29.0] | Outdoor-space viewpoint reservation |
| outdoor-old-graves | [28.0, 1.62, 12.0] | Outdoor-space viewpoint reservation |
| below-grade-seal | [28.0, -1.38, 16.0] | Negative test: no sky through sealed floor/wall/roof; allow only actual authored entrance/shaft apertures. |

## Site-specific constraints

- Use the annex footprint in this alternate proposal; retain original cemetery plan. Screen the service bay without blocking reception/service approaches.

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
