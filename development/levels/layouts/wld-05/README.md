# Dead Man's Claim

**WLD-05 · Highland Frontier · Medium-large / mine levels · rough layout v1**

Small surface shacks lead into a two-level mine with a main haul loop and ventilation return. Collapsed lift remains visible scenery, never a required traversal mechanic.

[Measured floor plans](plan.svg) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Mine-road van apron | [20, 76, 36, 88] | 0 / 3 m | Investigation and ordinary objects |
| entry | Mine gate / surface yard | [20, 62, 40, 76] | 0 / 3 m | Investigation and ordinary objects |
| shack | Miners shack | [2, 46, 20, 62] | 0 / 3 m | Investigation and ordinary objects |
| assay | Assay office | [40, 46, 54, 62] | 0 / 3 m | Investigation and ordinary objects |
| stores | Surface stores | [2, 30, 14, 46] | 0 / 3 m | Investigation and ordinary objects |
| adit | Adit mouth | [20, 42, 40, 62] | 0 / 3 m | Investigation and ordinary objects |
| haul | Main haul junction | [20, 20, 36, 42] | -6 / 3 m | Investigation and ordinary objects |
| ore-west | West ore room | [2, 20, 20, 42] | -6 / 3 m | Investigation and ordinary objects |
| ore-east | East ore room | [36, 20, 54, 42] | -6 / 3 m | Investigation and ordinary objects |
| cross | North crosscut | [12, 8, 44, 20] | -6 / 3 m | Investigation and ordinary objects |
| lift | Collapsed lift barrier bay | [44, 8, 54, 20] | -6 / 3 m | Investigation and ordinary objects |
| lower | Lower working chamber | [2, 20, 20, 42] | -10 / 3 m | Investigation and ordinary objects |
| vent | Ventilation return | [2, 42, 20, 62] | -6 / 3 m | Investigation and ordinary objects |
| wash | Surface washhouse | [40, 30, 54, 46] | 0 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Haul circuit gives two approaches to the ore rooms. A graded ventilation return reaches the adit without the lift. Collapsed bay is clearly blocked beyond its reachable viewing area; lower workings have two reserved connections.

- Loop: entry → shack → stores → adit → entry
- Loop: haul → ore-west → cross → ore-east → haul

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | stage ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L02 | entry ↔ shack | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L03 | entry ↔ assay | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L04 | shack ↔ stores | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L05 | stores ↔ adit | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L06 | adit ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L07 | assay ↔ wash | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L08 | wash ↔ adit | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L09 | adit ↔ haul | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L10 | haul ↔ ore-west | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L11 | ore-west ↔ cross | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L12 | cross ↔ ore-east | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L13 | ore-east ↔ haul | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L14 | cross ↔ lift | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L15 | ore-west ↔ lower | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L16 | lower ↔ vent | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L17 | vent ↔ adit | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L18 | ore-west ↔ vent | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=28, Z=82, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[28.0, 0, 78.372], [30.0, 0, 69.0]]. Van is already staged on a prepared roadhead/service apron through expedition logistics. Travel to this remote region is a campaign transition; no new vehicle, ferry or flight gameplay is assumed.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | ore-west | [11.0, -6, 31.0] | Bolted mine containment seal |
| Cleansing | wash | [47.0, 0, 38.0] | Surface wash purification basin |
| Absolution | assay | [47.0, 0, 54.0] | Payroll remembrance desk |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Rock conduction, ventilation draft, wet walls and rails are ordinary confounders. Support-timber hazards are visible static boundaries until a host-authorized event exists; no random cave-in may seal every escape.

## Campaign use

Assay records and impossible payroll dates connect provisionally to Blackpine shipping documents. Mine geometry, clues and ghost identity stay separate.

## Refinement risks / next pass

- Engineer the adit descent as a long graded drift rather than using the schematic direct line as a steep stair.
- Refine shaft void, lower-level gradients and two-way return with headroom and rock boundary tests.
- Inspect mine engineering references before detailed supports or ventilation routing.

## Source basis

- docs/environments/WILDERNESS_AND_REMOTE_SITE_ROSTER.md
- docs/CAMPAIGN_AND_ALLIED_INVESTIGATORS_DESIGN.md
- docs/environments/SHARED_VAN_MODULE.md
