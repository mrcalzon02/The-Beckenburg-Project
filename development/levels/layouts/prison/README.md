# Beckenburg Penitentiary

**PRISON · Beckenburg district · Large / secured compound · rough layout v1**

Two parallel cell blocks face a bounded exercise yard. Administration forms a front bar and mess/medical spaces close the rear, giving a legible perimeter circuit.

[Measured floor plans](plan.svg) · [PNG preview](plan.png) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Perimeter van apron | [26, 78, 42, 90] | 0 / 3 m | Investigation and ordinary objects |
| entry | Perimeter gate approach | [26, 68, 42, 78] | 0 / 3 m | Investigation and ordinary objects |
| sally | Vehicle sally port | [26, 58, 42, 68] | 0 / 3 m | Investigation and ordinary objects |
| intake | Intake and records | [10, 58, 26, 68] | 0 / 3 m | Investigation and ordinary objects |
| guard | Guard station | [42, 58, 54, 68] | 0 / 3 m | Investigation and ordinary objects |
| block-a | Main cell block | [10, 22, 22, 58] | 0 / 6.8 m | Investigation and ordinary objects |
| block-b | Secondary cell block | [46, 22, 58, 58] | 0 / 6.8 m | Investigation and ordinary objects |
| yard | Exercise yard | [22, 22, 46, 58] | 0 / 3 m | Investigation and ordinary objects |
| cross | Rear cross passage | [10, 16, 58, 22] | 0 / 3 m | Investigation and ordinary objects |
| mess | Mess hall | [10, 4, 28, 16] | 0 / 3 m | Investigation and ordinary objects |
| medical | Infirmary | [28, 4, 46, 16] | 0 / 3 m | Investigation and ordinary objects |
| chapel | Chapel / visitation | [58, 34, 70, 52] | 0 / 3 m | Investigation and ordinary objects |
| isolation | Isolation wing | [58, 16, 70, 34] | 0 / 3 m | Investigation and ordinary objects |
| gallery-a | A-block upper gallery | [10, 22, 14, 58] | 3.4 / 3 m | Investigation and ordinary objects |
| gallery-b | B-block upper gallery | [54, 22, 58, 58] | 3.4 / 3 m | Investigation and ordinary objects |
| plant | Boiler and maintenance | [22, 22, 46, 42] | -3 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Yard is the orientation landmark. Two cell-block routes join front intake and rear cross passage. Mechanical manual releases on both sides of security gates prevent power failure or ghost action from making permanent traps. Upper galleries are narrow observational branches, not the only way out.

- Loop: sally → intake → block-a → yard → sally
- Loop: yard → block-a → cross → block-b → yard

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | stage ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L02 | entry ↔ sally | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L03 | sally ↔ intake | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L04 | sally ↔ guard | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L05 | intake ↔ block-a | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L06 | guard ↔ block-b | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L07 | sally ↔ yard | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L08 | block-a ↔ yard | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L09 | block-b ↔ yard | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L10 | block-a ↔ cross | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L11 | block-b ↔ cross | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L12 | yard ↔ cross | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L13 | cross ↔ mess | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L14 | mess ↔ medical | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L15 | medical ↔ cross | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L16 | block-b ↔ chapel | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L17 | chapel ↔ isolation | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L18 | isolation ↔ cross | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L19 | block-a ↔ gallery-a | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L20 | block-b ↔ gallery-b | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L21 | cross ↔ plant | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=34, Z=84, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[34.0, 0, 80.372], [34.0, 0, 73.0]]. Existing shared van on a level service apron/roadhead; no new transport mechanic.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | isolation | [64.0, 0, 25.0] | Bolted suppression seal |
| Cleansing | medical | [37.0, 0, 10.0] | Clinical purification wash |
| Absolution | chapel | [64.0, 0, 43.0] | Reconciliation memorial |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Security gates, lighting and plant have explicit independent circuits. Metal galleries, concrete cell rows and open yard require distinct audio portals. Cell bays remain a later subdivision of each block, with stable child IDs.

## Campaign use

Visitor records, medical accounts and guard rotas support contradictory history without equating incarceration with supernatural evil.

## Refinement risks / next pass

- Lay out real individual cells and upper gallery voids; block volumes alone are not complete cell geometry.
- Test two-sided manual gate release during all power states before allowing hunts.
- Primary reference text inspected: https://www.nps.gov/places/000/alcatraz-cellhouse.htm — separate blocks and hospital; dimensions here are fictional game proposals.

## Source basis

- docs/environments/ROOM_AND_ASSET_REQUIREMENTS.md
- docs/environments/THREE_RITUAL_SITES.md
- https://www.nps.gov/places/000/alcatraz-cellhouse.htm (text inspected 2026-10-21; not a measured reproduction)

## Shared world thread

[Emergence Day and survival thread](LORE.md): connect this location to late 2022 and the long reclamation without prescribing its current ghost.
