# Beckenburg College

**COLLEGE · Beckenburg district · Large / bounded campus cluster · rough layout v1**

Three distinct campus buildings share a compact green: teaching block west, library north, residence/union east. A service tunnel and upper library bridge create alternate routes.

[Measured floor plans](plan.svg) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Campus service van apron | [28, 76, 44, 88] | 0 / 3 m | Investigation and ordinary objects |
| entry | Campus green approach | [28, 64, 44, 76] | 0 / 3 m | Investigation and ordinary objects |
| plaza | Main facade plaza | [18, 50, 54, 64] | 0 / 3 m | Investigation and ordinary objects |
| lobby | Teaching lobby | [4, 38, 18, 50] | 0 / 3 m | Investigation and ordinary objects |
| lecture | Lecture theatre | [4, 18, 18, 38] | 0 / 6 m | Investigation and ordinary objects |
| seminar | Seminar classroom | [18, 38, 32, 50] | 0 / 3 m | Investigation and ordinary objects |
| lab | Laboratory | [18, 18, 32, 38] | 0 / 3 m | Investigation and ordinary objects |
| library | Library reading room | [18, 0, 46, 14] | 0 / 3 m | Investigation and ordinary objects |
| stacks | Library stacks | [18, 0, 46, 14] | 3.6 / 3 m | Investigation and ordinary objects |
| bridge | Upper campus bridge | [32, 14, 46, 20] | 3.6 / 3 m | Investigation and ordinary objects |
| dorm | Dormitory corridor / rooms | [46, 20, 62, 38] | 3.6 / 3 m | Investigation and ordinary objects |
| res-lobby | Residence stair lobby | [46, 20, 62, 38] | 0 / 3 m | Investigation and ordinary objects |
| union | Union cafe | [46, 38, 62, 50] | 0 / 3 m | Investigation and ordinary objects |
| admin | Administration office | [62, 38, 74, 50] | 0 / 3 m | Investigation and ordinary objects |
| rear | Rear quad and parking | [62, 14, 74, 38] | 0 / 3 m | Investigation and ordinary objects |
| tunnel | Maintenance tunnel hub | [22, 20, 34, 38] | -3 / 3 m | Investigation and ordinary objects |
| tunnel-west | West service tunnel | [4, 38, 22, 44] | -3 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Teaching block has a compact indoor circuit. Campus loop uses green/plaza, library and union. Upper bridge links library to residence with a ground-level alternative; service tunnel reconnects teaching rooms to lobby without making a mandatory underground commute.

- Loop: lobby → lecture → lab → seminar → lobby
- Loop: plaza → lobby → lecture → lab → library → res-lobby → union → plaza

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | stage ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L02 | entry ↔ plaza | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L03 | plaza ↔ lobby | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L04 | lobby ↔ lecture | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L05 | lecture ↔ lab | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L06 | lab ↔ seminar | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L07 | seminar ↔ lobby | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L08 | lab ↔ library | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L09 | library ↔ res-lobby | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L10 | res-lobby ↔ union | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L11 | union ↔ plaza | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L12 | union ↔ admin | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L13 | admin ↔ rear | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L14 | rear ↔ res-lobby | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L15 | library ↔ stacks | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L16 | stacks ↔ bridge | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L17 | bridge ↔ dorm | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L18 | dorm ↔ res-lobby | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L19 | lab ↔ tunnel | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L20 | tunnel ↔ tunnel-west | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L21 | tunnel-west ↔ lobby | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=36, Z=82, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[36.0, 0, 78.372], [36.0, 0, 70.0]]. Existing shared van on a level service apron/roadhead; no new transport mechanic.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | tunnel | [28.0, -3, 29.0] | Breaker-fed containment seal |
| Cleansing | lab | [25.0, 0, 28.0] | Distilled-water purification basin |
| Absolution | stacks | [32.0, 3.6, 7.0] | Memorial records display |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Each building is an electrical/audio district. Cafe refrigeration, laboratory heat and tunnel ventilation are separate normal confounders. Campus boundary stops the playable cluster from implying an unlimited university.

## Campaign use

Administration and residence records support a single bounded campus dossier. Future faculty/residence expansions remain outside this footprint until actually authored.

## Refinement risks / next pass

- Tunnel arms and bridge bearings need exact connecting corridors; link centre lines are reservations, not finished bridge meshes.
- Subdivide dormitory rooms while retaining readable suite numbers and tested clear routes.
- Acquire campus plan reference and compare approved college concept before detailed construction.

## Source basis

- docs/environments/ROOM_AND_ASSET_REQUIREMENTS.md
- docs/environments/THREE_RITUAL_SITES.md
- development/levels/ARCHITECTURAL_REFERENCES.md
