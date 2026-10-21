# Mourning Marsh

**WLD-11 · Coastal Territories · Medium / stilt settlement · rough layout v1**

A small settlement straddles a main raised boardwalk and a parallel service boardwalk. Homes, chapel and warehouse are reachable on dry supported decks; flooded terrain is a visible boundary.

[Measured floor plans](plan.svg) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Causeway van apron | [24, 76, 40, 88] | 0 / 3 m | Investigation and ordinary objects |
| entry | Raised causeway landing | [24, 62, 40, 76] | 0 / 3 m | Investigation and ordinary objects |
| main | Main settlement boardwalk | [28, 14, 32, 62] | 1.2 / 3 m | Investigation and ordinary objects |
| service | Service boardwalk | [48, 14, 52, 62] | 1.2 / 3 m | Investigation and ordinary objects |
| south-deck | South crossing deck | [28, 56, 52, 62] | 1.2 / 3 m | Investigation and ordinary objects |
| north-deck | North crossing deck | [28, 8, 52, 14] | 1.2 / 3 m | Investigation and ordinary objects |
| house-a | West stilt house | [12, 38, 28, 54] | 1.2 / 3 m | Investigation and ordinary objects |
| house-b | East stilt house | [52, 38, 66, 54] | 1.2 / 3 m | Investigation and ordinary objects |
| chapel | Settlement chapel | [12, 16, 28, 34] | 1.2 / 3 m | Investigation and ordinary objects |
| warehouse | Flooded warehouse dry deck | [32, 14, 48, 36] | 1.2 / 3 m | Investigation and ordinary objects |
| shack | Fishing shack | [52, 18, 64, 32] | 1.2 / 3 m | Investigation and ordinary objects |
| ferry | Ferry stage | [28, -6, 40, 8] | 1.2 / 3 m | Investigation and ordinary objects |
| pump | Pump / wash shelter | [52, 54, 64, 64] | 1.2 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Parallel boardwalks and two crossings keep every required building connected without entering water. A broad graded entrance rises to the +1.2 m decks. Ferry stage is reachable scenery, not extraction transport; return remains to the land-based van.

- Loop: south-deck → main → north-deck → service → south-deck
- Loop: main → warehouse → service → north-deck → main

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | stage ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L02 | entry ↔ south-deck | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L03 | south-deck ↔ main | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L04 | south-deck ↔ service | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L05 | main ↔ north-deck | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L06 | north-deck ↔ service | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L07 | main ↔ house-a | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L08 | service ↔ house-b | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L09 | main ↔ chapel | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L10 | main ↔ warehouse | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L11 | warehouse ↔ service | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L12 | service ↔ shack | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L13 | north-deck ↔ ferry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L14 | service ↔ pump | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=32, Z=82, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[32.0, 0, 78.372], [32.0, 0, 69.0]]. Van is already staged on a prepared roadhead/service apron through expedition logistics. Travel to this remote region is a campaign transition; no new vehicle, ferry or flight gameplay is assumed.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | warehouse | [40.0, 1.2, 25.0] | Dry-deck containment frame |
| Cleansing | pump | [58.0, 1.2, 59.0] | Pump-shelter purification vessel |
| Absolution | chapel | [20.0, 1.2, 25.0] | Settlement remembrance stand |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Flooded lower warehouse floor is visible below the dry route and excluded from navigation. Water reflection, insects, pumps and timber creak have ordinary sources; fog cannot hide all route markers.

## Campaign use

Property deeds describe homes lost to water; the actual reachable homes remain fixed. Reclaimed settlement contracts may revisit records without requiring a simulated receding tide.

## Refinement risks / next pass

- Replace long boardwalk envelopes with measured supports, continuous deck meshes and invariant rails; ensure 2 m clear width.
- Define flood plane and collision once per mission; no required wading, swimming or leap.
- The entrance rise needs a real supported ramp and level intermediate landings.

## Source basis

- docs/environments/WILDERNESS_AND_REMOTE_SITE_ROSTER.md
- docs/CAMPAIGN_AND_ALLIED_INVESTIGATORS_DESIGN.md
- docs/environments/SHARED_VAN_MODULE.md
