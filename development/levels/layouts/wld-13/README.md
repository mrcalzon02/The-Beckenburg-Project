# Greywater Cannery

**WLD-13 · Coastal Territories · Medium-large / industrial wilderness · rough layout v1**

A shore-side processing hall connects cold stores, boiler plant and shipping dock. Workers housing sits along the forest edge, linked by a short service-yard loop.

[Measured floor plans](plan.svg) · [PNG preview](plan.png) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Forest-road van apron | [24, 76, 40, 88] | 0 / 3 m | Investigation and ordinary objects |
| entry | Cannery gate / service yard | [24, 62, 44, 76] | 0 / 3 m | Investigation and ordinary objects |
| processing | Fish-processing floor | [24, 26, 48, 56] | 0 / 6 m | Investigation and ordinary objects |
| cold | Refrigerated stores | [48, 26, 64, 44] | 0 / 3 m | Investigation and ordinary objects |
| boiler | Boiler house | [8, 26, 24, 44] | 0 / 3 m | Investigation and ordinary objects |
| wash | Workers washroom | [8, 44, 24, 56] | 0 / 3 m | Investigation and ordinary objects |
| office | Managers office | [24, 14, 36, 26] | 0 / 3 m | Investigation and ordinary objects |
| stores | Packing / dry stores | [36, 14, 48, 26] | 0 / 3 m | Investigation and ordinary objects |
| dock | Shipping dock | [24, 0, 48, 14] | 0 / 3 m | Investigation and ordinary objects |
| bunk | Workers bunkhouse | [-8, 44, 8, 62] | 0 / 3 m | Investigation and ordinary objects |
| mess | Workers mess | [-8, 26, 8, 44] | 0 / 3 m | Investigation and ordinary objects |
| forest | Forest edge return | [-16, 26, -8, 62] | 0 / 3 m | Investigation and ordinary objects |
| side-yard | East service return | [48, 44, 64, 62] | 0 / 3 m | Investigation and ordinary objects |
| gallery | Inspection mezzanine | [24, 26, 30, 56] | 3 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Processing hall has two service exits plus shipping access. Separate worker loop reaches bunkhouse and mess without crossing machines. Mezzanine is optional observation; all rites remain at ground level.

- Loop: entry → processing → cold → side-yard → entry
- Loop: processing → office → stores → processing

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | stage ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L02 | entry ↔ processing | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L03 | entry ↔ wash | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L04 | wash ↔ boiler | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L05 | boiler ↔ processing | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L06 | processing ↔ cold | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L07 | cold ↔ side-yard | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L08 | side-yard ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L09 | processing ↔ office | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L10 | office ↔ stores | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L11 | stores ↔ processing | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L12 | stores ↔ dock | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L13 | dock ↔ office | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L14 | wash ↔ bunk | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L15 | bunk ↔ mess | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L16 | mess ↔ boiler | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L17 | bunk ↔ forest | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L18 | forest ↔ mess | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L19 | processing ↔ gallery | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=32, Z=82, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[32.0, 0, 78.372], [34.0, 0, 69.0]]. Van is already staged on a prepared roadhead/service apron through expedition logistics. Travel to this remote region is a campaign transition; no new vehicle, ferry or flight gameplay is assumed.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | processing | [36.0, 0, 41.0] | Grounded loading-floor containment seal |
| Cleansing | wash | [16.0, 0, 50.0] | Workers purification wash |
| Absolution | mess | [0.0, 0, 35.0] | Payroll remembrance table |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Cold-room gradients, boiler heat, wet floors, refrigeration noise and forest wind remain real normal signals. Industrial machinery is ordinary map infrastructure, never free investigator gear.

## Campaign use

Manager ledger and worker payroll continue beyond closure; dock log can connect to Drowned Wharf. A forest boundary implies wider wilderness without adding empty playable kilometres.

## Refinement risks / next pass

- Research cannery processing/boiler separation and shore construction before detailed meshes.
- Refine machine islands around at least two clear hall lanes and a reachable cold-store manual release.
- Cold-store doors must always have an interior escape; no powered lock may create a permanent hunt trap.

## Source basis

- docs/environments/WILDERNESS_AND_REMOTE_SITE_ROSTER.md
- docs/CAMPAIGN_AND_ALLIED_INVESTIGATORS_DESIGN.md
- docs/environments/SHARED_VAN_MODULE.md

## Shared world thread

[Emergence Day and survival thread](LORE.md): connect this location to late 2022 and the long reclamation without prescribing its current ghost.
