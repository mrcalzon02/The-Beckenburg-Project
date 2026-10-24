# Northwatch Research Station — edge blocking and sky inclusion

planning-only / no runtime geometry or navigation mutation

Research compound in wind-shaped snow terrain

[Diagram](plan.svg) · [Exact planning contract](plan.json)

## Layer roles

- **near**: 0–12 m beyond accepted playable edge: grounded physical enclosure with irregular rhythm, visible support and closed returns.
- **middle**: World-space terrain/vegetation/buildings beyond boundary: no accessible paths or hidden evidence; parallax from player translation.
- **far**: Direction-fixed distant horizon/sky/weather; no collision. Per-map sector masks replace generic silhouette only in a separately reviewed renderer change.

## Physical edge reservations

| Span | Treatment | Height / depth reservation | Fit / containment |
|---|---|---|---|
| edge-01 | snowbank | 2.5 / 6.0 m | Snow veneer is not a solid barrier. Match rock/packed-bank body and verify visibility in snow/fog. |
| edge-02 | snowbank | 2.5 / 6.0 m | Snow veneer is not a solid barrier. Match rock/packed-bank body and verify visibility in snow/fog. |
| edge-03 | snowbank | 2.5 / 6.0 m | Snow veneer is not a solid barrier. Match rock/packed-bank body and verify visibility in snow/fog. |
| edge-04 | snowbank | 2.5 / 6.0 m | Snow veneer is not a solid barrier. Match rock/packed-bank body and verify visibility in snow/fog. |
| edge-05 | snowbank | 2.5 / 6.0 m | Snow veneer is not a solid barrier. Match rock/packed-bank body and verify visibility in snow/fog. |
| edge-06 | snowbank | 2.5 / 6.0 m | Snow veneer is not a solid barrier. Match rock/packed-bank body and verify visibility in snow/fog. |
| edge-07 | snowbank | 2.5 / 6.0 m | Snow veneer is not a solid barrier. Match rock/packed-bank body and verify visibility in snow/fog. |
| edge-08 | snowbank | 2.5 / 6.0 m | Snow veneer is not a solid barrier. Match rock/packed-bank body and verify visibility in snow/fog. |
| edge-09 | snowbank | 2.5 / 6.0 m | Snow veneer is not a solid barrier. Match rock/packed-bank body and verify visibility in snow/fog. |
| edge-10 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-11 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-12 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-13 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-14 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-15 | secure | 2.8 / 0.4 m | Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review. |
| edge-16 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-17 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-18 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-19 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-20 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-21 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-22 | snowbank | 2.5 / 6.0 m | Snow veneer is not a solid barrier. Match rock/packed-bank body and verify visibility in snow/fog. |
| edge-23 | snowbank | 2.5 / 6.0 m | Snow veneer is not a solid barrier. Match rock/packed-bank body and verify visibility in snow/fog. |
| edge-24 | snowbank | 2.5 / 6.0 m | Snow veneer is not a solid barrier. Match rock/packed-bank body and verify visibility in snow/fog. |
| edge-25 | snowbank | 2.5 / 6.0 m | Snow veneer is not a solid barrier. Match rock/packed-bank body and verify visibility in snow/fog. |

All base heights must be fitted to authoritative terrain, not Y0. The outer ring encloses source reservations but grants no extra walkable area. Fit it to actual terrain/architecture before construction.

Arrival: 8 m road portal reservation beyond staged van; retain its exact cargo route and fit a visible closed outer gate/road bend.

## Sky inclusion sectors

| Azimuth | Identity | Provisional crest / open sky above | Mid-distance |
|---|---|---|---|
| [0, 45]° | Snow ridge and restrained distant mountains | [1, 7]° / 7° | [200, 4000] m |
| [45, 90]° | Snow ridge and restrained distant mountains transitioning to Fenced service edge against low white terrain | [1, 7]° / 7° | [200, 4000] m |
| [90, 135]° | Fenced service edge against low white terrain | [1, 7]° / 7° | [200, 4000] m |
| [135, 180]° | Fenced service edge against low white terrain transitioning to Flagged access track and far snow horizon | [1, 7]° / 7° | [200, 4000] m |
| [180, 225]° | Flagged access track and far snow horizon | [1, 7]° / 7° | [200, 4000] m |
| [225, 270]° | Flagged access track and far snow horizon transitioning to Wind-scoured bank and dark rock outcrops | [1, 7]° / 7° | [200, 4000] m |
| [270, 315]° | Wind-scoured bank and dark rock outcrops | [8, 22]° / 22° | [700, 5000] m |
| [315, 360]° | Wind-scoured bank and dark rock outcrops transitioning to Snow ridge and restrained distant mountains | [8, 22]° / 22° | [700, 5000] m |

Sector heights/distances are original art-direction targets, not measured horizon coverage. Real camera captures must fit them, particularly from high towers and windows.

## Viewpoints

| Anchor | Eye position X/Y/Z | Source |
|---|---|---|
| van-arrival | [32.0, 1.62, 76.0] | Canonical staged van eye/roadhead; preserve cargo route |
| primary-approach | [32.0, 1.62, 63.0] | Authored approach-space reservation; actual viewpoint must be stood in |
| highest-accessible-layer | [32.0, 1.62, 76.0] | Highest authored standing zone; actual window/balcony aperture and guards still require review |
| outdoor-entry | [32.0, 1.62, 63.0] | Outdoor-space viewpoint reservation |
| outdoor-court | [32.0, 1.62, 41.0] | Outdoor-space viewpoint reservation |
| outdoor-array | [53.0, 1.62, 10.0] | Outdoor-space viewpoint reservation |
| below-grade-seal | [32.0, -0.8799999999999999, 31.0] | Negative test: no sky through sealed floor/wall/roof; allow only actual authored entrance/shaft apertures. |

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
