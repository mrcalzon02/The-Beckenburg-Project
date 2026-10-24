# Mourning Marsh — edge blocking and sky inclusion

planning-only / no runtime geometry or navigation mutation

Marsh boardwalk enclave with genuine wetland depth

[Diagram](plan.svg) · [Exact planning contract](plan.json)

## Layer roles

- **near**: 0–12 m beyond accepted playable edge: grounded physical enclosure with irregular rhythm, visible support and closed returns.
- **middle**: World-space terrain/vegetation/buildings beyond boundary: no accessible paths or hidden evidence; parallax from player translation.
- **far**: Direction-fixed distant horizon/sky/weather; no collision. Per-map sector masks replace generic silhouette only in a separately reviewed renderer change.

## Physical edge reservations

| Span | Treatment | Height / depth reservation | Fit / containment |
|---|---|---|---|
| edge-01 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-02 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-03 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-04 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-05 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-06 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-07 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-08 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-09 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-10 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-11 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-12 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-13 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-14 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-15 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-16 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-17 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-18 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-19 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-20 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-21 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-22 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-23 | gate | 2.2 / 1.0 m | Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line. |
| edge-24 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-25 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |
| edge-26 | shore | 1.8 / 4.0 m | Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access. |

All base heights must be fitted to authoritative terrain, not Y0. The outer ring encloses source reservations but grants no extra walkable area. Fit it to actual terrain/architecture before construction.

Arrival: 8 m road portal reservation beyond staged van; retain its exact cargo route and fit a visible closed outer gate/road bend.

## Sky inclusion sectors

| Azimuth | Identity | Provisional crest / open sky above | Mid-distance |
|---|---|---|---|
| [0, 45]° | Low reed islands and broad open wetland sky | [-0.5, 1.5]° / 1.5° | [500, 4000] m |
| [45, 90]° | Low reed islands and broad open wetland sky transitioning to Flooded timber and distant reed horizon | [-0.5, 1.5]° / 1.5° | [500, 4000] m |
| [90, 135]° | Flooded timber and distant reed horizon | [-0.5, 1.5]° / 1.5° | [500, 4000] m |
| [135, 180]° | Flooded timber and distant reed horizon transitioning to Raised causeway back to firm ground | [-0.5, 1.5]° / 1.5° | [500, 4000] m |
| [180, 225]° | Raised causeway back to firm ground | [4, 12]° / 12° | [60, 450] m |
| [225, 270]° | Raised causeway back to firm ground transitioning to Sparse swamp trees above open water | [4, 12]° / 12° | [60, 450] m |
| [270, 315]° | Sparse swamp trees above open water | [4, 12]° / 12° | [60, 450] m |
| [315, 360]° | Sparse swamp trees above open water transitioning to Low reed islands and broad open wetland sky | [4, 12]° / 12° | [60, 450] m |

Sector heights/distances are original art-direction targets, not measured horizon coverage. Real camera captures must fit them, particularly from high towers and windows.

## Viewpoints

| Anchor | Eye position X/Y/Z | Source |
|---|---|---|
| van-arrival | [32.0, 1.62, 82.0] | Canonical staged van eye/roadhead; preserve cargo route |
| primary-approach | [32.0, 1.62, 69.0] | Authored approach-space reservation; actual viewpoint must be stood in |
| highest-accessible-layer | [30.0, 2.8200000000000003, 38.0] | Highest authored standing zone; actual window/balcony aperture and guards still require review |
| outdoor-entry | [32.0, 1.62, 69.0] | Outdoor-space viewpoint reservation |
| outdoor-main | [30.0, 2.8200000000000003, 38.0] | Outdoor-space viewpoint reservation |
| outdoor-service | [50.0, 2.8200000000000003, 38.0] | Outdoor-space viewpoint reservation |
| outdoor-south-deck | [40.0, 2.8200000000000003, 59.0] | Outdoor-space viewpoint reservation |
| outdoor-north-deck | [40.0, 2.8200000000000003, 11.0] | Outdoor-space viewpoint reservation |
| outdoor-ferry | [34.0, 2.8200000000000003, 1.0] | Outdoor-space viewpoint reservation |

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
