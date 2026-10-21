# Blackpine Logging Camp

**WLD-01 · Northern Woodlands · Medium-large / dispersed buildings · rough layout v1**

A horseshoe of working buildings around a log yard, with a separate cabin path and original foundations beyond the sawmill. Distinct roofs and yard landmarks bound the search.

[Measured floor plans](plan.svg) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Logging-road van apron | [24, 78, 40, 90] | 0 / 3 m | Investigation and ordinary objects |
| entry | Camp gate and roadhead | [24, 66, 40, 78] | 0 / 3 m | Investigation and ordinary objects |
| yard | Stacked-timber yard | [20, 38, 44, 66] | 0 / 3 m | Investigation and ordinary objects |
| mill | Sawmill floor | [44, 30, 62, 58] | 0 / 6 m | Investigation and ordinary objects |
| mill-deck | Sawmill inspection deck | [44, 30, 50, 48] | 3 / 3 m | Investigation and ordinary objects |
| mess | Mess hall | [2, 42, 20, 58] | 0 / 3 m | Investigation and ordinary objects |
| bunk | Bunkhouse | [2, 22, 20, 42] | 0 / 3 m | Investigation and ordinary objects |
| cabin-a | Foreman cabin | [24, 14, 34, 26] | 0 / 3 m | Investigation and ordinary objects |
| cabin-b | Logging cabin | [38, 10, 48, 22] | 0 / 3 m | Investigation and ordinary objects |
| shed | Equipment repair shed | [44, 58, 58, 70] | 0 / 3 m | Investigation and ordinary objects |
| wash | Washhouse | [2, 58, 12, 68] | 0 / 3 m | Investigation and ordinary objects |
| foundations | Original foundations | [48, 4, 62, 22] | 0 / 3 m | Investigation and ordinary objects |
| trail | Cabin trail junction | [20, 26, 44, 38] | 0 / 3 m | Investigation and ordinary objects |
| store | Payroll / stores office | [20, 4, 34, 14] | 0 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Main yard/mess/bunkhouse loop is short and sheltered on one side. Sawmill/shed loop is more exposed. Two cabin-trail returns avoid a long compulsory dead end. Inspection deck is optional; all rites stay on the ground.

- Loop: yard → mess → bunk → trail → yard
- Loop: yard → mill → shed → yard

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | stage ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L02 | entry ↔ yard | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L03 | yard ↔ mess | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L04 | mess ↔ bunk | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L05 | bunk ↔ trail | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L06 | trail ↔ yard | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L07 | trail ↔ cabin-a | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L08 | cabin-a ↔ store | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L09 | store ↔ cabin-b | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L10 | cabin-b ↔ trail | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L11 | cabin-b ↔ foundations | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L12 | foundations ↔ mill | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L13 | mill ↔ yard | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L14 | yard ↔ shed | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L15 | shed ↔ mill | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L16 | mess ↔ wash | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L17 | wash ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L18 | mill ↔ mill-deck | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=32, Z=84, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[32.0, 0, 80.372], [32.0, 0, 72.0]]. Van is already staged on a prepared roadhead/service apron through expedition logistics. Travel to this remote region is a campaign transition; no new vehicle, ferry or flight gameplay is assumed.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | mill | [53.0, 0, 44.0] | Grounded industrial containment ring |
| Cleansing | wash | [7.0, 0, 63.0] | Washhouse purification vessel |
| Absolution | mess | [11.0, 0, 50.0] | Workers memorial table |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Mill, domestic and service electrical districts; timber stacks break sightlines without moving between graphics presets. Wind, loose roofing and inert machinery produce ordinary sounds; any powered event needs a real source/state.

## Campaign use

Payroll omissions in stores, manifest in sawmill office and personal accounts in bunkhouse. Proposed shipping connection to Dead Man's Claim is a found-document thread, never automatic ghost evidence.

## Refinement risks / next pass

- First remote construction exemplar after the established house reference: prove all tools, three rites and human/AI ghost access here.
- Subdivide sawmill into believable machine and circulation bays while retaining the diagram footprint.
- Inspect primary logging-camp building references before shell work; proposed metres are not historical measurements.

## Source basis

- docs/environments/WILDERNESS_AND_REMOTE_SITE_ROSTER.md
- docs/CAMPAIGN_AND_ALLIED_INVESTIGATORS_DESIGN.md
- docs/environments/SHARED_VAN_MODULE.md
- https://npgallery.nps.gov/GetAsset/693f6b20-44db-4db6-bbc7-2290ab12d7a2 (indexed primary text inspected 2026-10-21: multiple habitation and service buildings; no source dimensions copied)
