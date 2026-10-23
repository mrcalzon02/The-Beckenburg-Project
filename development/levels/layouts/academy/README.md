# St. Alban Academy

**ACADEMY · Beckenburg district · Medium-large / cloister · rough layout v1**

Four unequal ranges frame a real open quadrangle. Public teaching stays on the south/east, library on the west, upper dormitories on the north, and sealed archive below.

[Measured floor plans](plan.svg) · [PNG preview](plan.png) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Gate van apron | [24, 68, 40, 80] | 0 / 3 m | Investigation and ordinary objects |
| entry | Front gate court | [24, 58, 40, 68] | 0 / 3 m | Investigation and ordinary objects |
| reception | Reception hall | [24, 46, 40, 58] | 0 / 3 m | Investigation and ordinary objects |
| south | South cloister | [12, 40, 52, 46] | 0 / 3 m | Investigation and ordinary objects |
| west | West cloister | [12, 16, 18, 40] | 0 / 3 m | Investigation and ordinary objects |
| north | North cloister | [12, 10, 52, 16] | 0 / 3 m | Investigation and ordinary objects |
| east | East cloister | [46, 16, 52, 40] | 0 / 3 m | Investigation and ordinary objects |
| quad | Inner quadrangle | [18, 16, 46, 40] | 0 / 3 m | Investigation and ordinary objects |
| library | Library | [0, 16, 12, 40] | 0 / 3 m | Investigation and ordinary objects |
| lecture | Lecture room | [52, 26, 68, 40] | 0 / 3 m | Investigation and ordinary objects |
| lab | Laboratory / study | [52, 10, 68, 26] | 0 / 3 m | Investigation and ordinary objects |
| dining | Dining hall | [18, 0, 46, 10] | 0 / 3 m | Investigation and ordinary objects |
| office | Headmaster office | [0, 0, 12, 16] | 0 / 3 m | Investigation and ordinary objects |
| dorm | Upper dormitory | [18, 0, 46, 10] | 3.6 / 3 m | Investigation and ordinary objects |
| upper | Upper north gallery | [12, 10, 52, 16] | 3.6 / 3 m | Investigation and ordinary objects |
| archive | Basement archive | [0, 16, 12, 40] | -3 / 3 m | Investigation and ordinary objects |
| rear | Rear quadrangle / service yard | [52, 40, 68, 58] | 0 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Cloister ring is the main pursuit loop with the open quad as a short exposed alternative. Teaching-room loop branches east; service yard provides a second ground-level return. Upper dorms are a bounded search branch rather than a duplicate teaching floor.

- Loop: south → west → north → east → south
- Loop: east → lecture → lab → east

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | stage ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L02 | entry ↔ reception | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L03 | reception ↔ south | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L04 | south ↔ west | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L05 | west ↔ north | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L06 | north ↔ east | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L07 | east ↔ south | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L08 | south ↔ quad | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L09 | quad ↔ north | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L10 | west ↔ library | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L11 | north ↔ dining | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L12 | north ↔ office | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L13 | east ↔ lecture | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L14 | lecture ↔ lab | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L15 | lab ↔ east | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L16 | south ↔ rear | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L17 | rear ↔ lecture | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L18 | north ↔ upper | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L19 | upper ↔ dorm | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L20 | west ↔ archive | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=32, Z=74, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[32.0, 0, 70.372], [32.0, 0, 63.0]]. Existing shared van on a level service apron/roadhead; no new transport mechanic.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | archive | [6.0, -3, 28.0] | Locked archive seal grid |
| Cleansing | lab | [60.0, 0, 18.0] | Reagent purification station |
| Absolution | library | [6.0, 0, 28.0] | Founders remembrance lectern |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Stone cloister reverberation contrasts with library absorption. Archive humidity, laboratory apparatus and dining heat use ordinary baselines; lore puzzles never gate all legal ritual routes.

## Campaign use

Founders records and student accounts can inform Castle research restoration while remaining separate from measured encounter evidence.

## Refinement risks / next pass

- Reference pass must inspect a real cloister/academic plan before shell work; current proportions are original reservations.
- Engineer upper-gallery stairs, archive descent and real continuous cloister openings.
- Do not conflate this compact esoteric academy with the larger multi-building college.

## Source basis

- docs/environments/ROOM_AND_ASSET_REQUIREMENTS.md
- docs/environments/THREE_RITUAL_SITES.md
- development/levels/ARCHITECTURAL_REFERENCES.md

## Shared world thread

[Emergence Day and survival thread](LORE.md): connect this location to late 2022 and the long reclamation without prescribing its current ghost.
