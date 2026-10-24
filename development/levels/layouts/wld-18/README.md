# Site Cinder-6

**WLD-18 · Beckenburg coast and hinterland · Measured rough site / metres · rough layout v1**

A decommissioned fictional missile-site maintenance complex, with an empty shaft, annular galleries and inert service equipment. No launch mechanics or working weapon.

[Measured floor plans](plan.svg) · [PNG preview](plan.png) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Service road van apron | [24, 84, 40, 96] | 0 / 3 m | Service road van apron |
| entry | Security approach | [24, 70, 40, 84] | 0 / 3 m | Security approach |
| servicehouse | Surface service building | [10, 54, 28, 70] | 0 / 3 m | Surface service building |
| yard | Fenced maintenance yard | [28, 54, 52, 70] | 0 / 3 m | Fenced maintenance yard |
| head | Access headhouse | [28, 42, 40, 54] | 0 / 3 m | Access headhouse |
| shop | Maintenance workshop | [40, 42, 54, 54] | 0 / 3 m | Maintenance workshop |
| exit | Hillside emergency exit | [4, 42, 10, 62] | 0 / 3 m | Hillside emergency exit |
| upperring | Upper shaft gallery | [22, 18, 42, 38] | -4 / 3 m | Upper shaft gallery |
| lowring | Lower shaft gallery | [22, 18, 42, 38] | -8 / 3 m | Lower shaft gallery |
| west | Upper west access | [8, 26, 22, 38] | -4 / 3 m | Upper west access |
| records | Maintenance records | [8, 38, 22, 52] | -4 / 3 m | Maintenance records |
| exhibit | Inert control equipment room | [42, 18, 56, 38] | -4 / 3 m | Inert control equipment room |
| east | Lower east access | [42, 18, 56, 38] | -8 / 3 m | Lower east access |
| crew | Lower crew room | [8, 26, 22, 38] | -8 / 3 m | Lower crew room |
| pump | Sump and pump room | [8, 14, 22, 26] | -8 / 3 m | Sump and pump room |
| plant | Lower ventilation plant | [42, 38, 56, 52] | -8 / 3 m | Lower ventilation plant |

## Routes and retreat

Two declared graph cycles reserve retreat options. Optional tower/bridge branches must never host mandatory evidence without separate fairness review. Actual physical routes and timing remain unaccepted.

- Loop: head → west → upperring → exhibit → head
- Loop: upperring → exhibit → east → lowring → crew → west → upperring

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| route-01 | stage ↔ entry | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-02 | entry ↔ servicehouse | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-03 | entry ↔ yard | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-04 | yard ↔ head | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-05 | yard ↔ shop | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-06 | shop ↔ head | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-07 | head ↔ west | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-08 | west ↔ exit | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-09 | exit ↔ servicehouse | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-10 | west ↔ records | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-11 | west ↔ upperring | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-12 | upperring ↔ exhibit | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-13 | head ↔ exhibit | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-14 | exhibit ↔ east | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-15 | east ↔ lowring | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-16 | lowring ↔ crew | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-17 | crew ↔ pump | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-18 | crew ↔ west | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-19 | east ↔ plant | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |

## Arrival and extraction

Shared van placement: X=32, Z=90, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[32.0, 0, 86.372], [32.0, 0, 77.0]]. Reuse the canonical van on the roadhead/quay; remote travel is an expedition transition. No new driving or sailing system.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | yard | [40.0, 0, 62.0] | Removable modern investigation fitting on a clear 4.4 m floor square; reserve access around it. |
| Cleansing | records | [15.0, -4, 45.0] | Removable modern investigation fitting on a clear 4.4 m floor square; reserve access around it. |
| Absolution | plant | [49.0, -8, 45.0] | Removable modern investigation fitting on a clear 4.4 m floor square; reserve access around it. |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Terrain and water boundaries first; then solid architecture with floor depth, fitted doors and circuit isolation. Separate exterior/interior acoustics, supported fixtures and surface-mounted UV evidence. All gameplay integration remains locked.

## Campaign use

Fictional location and records connect to the district investigation; narrative never predetermines the ghost or prevents an evidence-led outcome.

## Refinement risks / next pass

- Terrain-first massing is not terrain acceptance: resolve drainage, incised paths, retaining edges, foundation contacts and every grade before detailed structures.
- Check floor/wall/roof joins, upper-deck undersides and corners with adjacent lights toggled; log every discovered failure.
- Engineer stairs, landings, openings, guards, door swings and two exits before collision/navigation approval.
- Inspect each model purpose, scale, UVs and true support surface before placing furniture or clutter.
- Public decommissioned-site context only; not a reconstruction of an operational facility.
- Keep every mandatory route off the shaft void. Real ring guards, falling protection and stair headroom require engineering.

## Source basis

- https://www.nps.gov/places/delta-09.htm
