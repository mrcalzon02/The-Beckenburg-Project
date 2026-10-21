# Drowned Fisherman's Wharf

**WLD-06 · Coastal Territories · Medium / shore and piers · rough layout v1**

A shore street feeds two parallel working piers joined by a cross-dock. Cottages sit on land, work sheds on piles, and a dry tidal undercroft is reached by two guarded descents.

[Measured floor plans](plan.svg) · [PNG preview](plan.png) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Shore-road van apron | [24, 68, 40, 80] | 0 / 3 m | Investigation and ordinary objects |
| entry | Shore street | [20, 54, 44, 68] | 0 / 3 m | Investigation and ordinary objects |
| cottage-a | West fisherman cottage | [4, 42, 20, 54] | 0 / 3 m | Investigation and ordinary objects |
| cottage-b | East fisherman cottage | [44, 42, 60, 54] | 0 / 3 m | Investigation and ordinary objects |
| quay | Quay apron | [20, 36, 44, 54] | 0 / 3 m | Investigation and ordinary objects |
| pier-w | West dock walkway | [20, 10, 24, 36] | 0 / 3 m | Investigation and ordinary objects |
| pier-e | East dock walkway | [40, 10, 44, 36] | 0 / 3 m | Investigation and ordinary objects |
| crossdock | Cross-dock | [20, 6, 44, 10] | 0 / 3 m | Investigation and ordinary objects |
| boathouse | Boathouse | [4, 10, 20, 28] | 0 / 3 m | Investigation and ordinary objects |
| net | Net shed | [44, 10, 56, 24] | 0 / 3 m | Investigation and ordinary objects |
| fish | Fish house | [24, 10, 40, 24] | 0 / 3 m | Investigation and ordinary objects |
| undercroft | Dry tidal undercroft | [24, 24, 40, 36] | -2.5 / 3 m | Investigation and ordinary objects |
| west-stair | West shore stair bay | [12, 28, 24, 38] | 0 / 3 m | Investigation and ordinary objects |
| east-stair | East shore stair bay | [40, 28, 52, 38] | 0 / 3 m | Investigation and ordinary objects |

## Routes and retreat

A pier ladder is never the only way out: wide supported deck returns run on both sides. Undercroft remains dry for the entire mission seed; rising-tide hazards cannot strand a player or remove the required extraction path.

- Loop: quay → pier-w → crossdock → pier-e → quay
- Loop: quay → west-stair → undercroft → east-stair → quay

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | stage ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L02 | entry ↔ cottage-a | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L03 | entry ↔ cottage-b | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L04 | entry ↔ quay | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L05 | cottage-a ↔ quay | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L06 | cottage-b ↔ quay | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L07 | quay ↔ pier-w | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L08 | pier-w ↔ crossdock | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L09 | crossdock ↔ pier-e | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L10 | pier-e ↔ quay | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L11 | pier-w ↔ boathouse | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L12 | pier-e ↔ net | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L13 | crossdock ↔ fish | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L14 | boathouse ↔ west-stair | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L15 | west-stair ↔ quay | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L16 | west-stair ↔ undercroft | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L17 | undercroft ↔ east-stair | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L18 | east-stair ↔ quay | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=32, Z=74, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[32.0, 0, 70.372], [32.0, 0, 61.0]]. Van is already staged on a prepared roadhead/service apron through expedition logistics. Travel to this remote region is a campaign transition; no new vehicle, ferry or flight gameplay is assumed.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | boathouse | [12.0, 0, 19.0] | Moored iron containment frame |
| Cleansing | fish | [32.0, 0, 17.0] | Fish-house purification wash |
| Absolution | cottage-a | [12.0, 0, 48.0] | Departure remembrance table |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Tidal boundary, wave impacts, wet timber, cold storage and wind have stable baselines. Boat silhouettes are scenery with collision barriers, not undeclared transport. Safe van stays on land.

## Campaign use

Logbooks at fish house and cottage, storm records in boathouse. A shipment record can link Greywater Cannery while contradicting departure dates.

## Refinement risks / next pass

- Refine support piles, edge rails, guard openings and at least 2 m clear deck paths; diagrams show deck envelopes.
- Choose and lock tide envelope before mission start; no required wading.
- Research working-wharf and fish-house construction before detailed shell authoring.

## Source basis

- docs/environments/WILDERNESS_AND_REMOTE_SITE_ROSTER.md
- docs/CAMPAIGN_AND_ALLIED_INVESTIGATORS_DESIGN.md
- docs/environments/SHARED_VAN_MODULE.md
