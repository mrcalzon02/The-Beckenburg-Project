# Widow's Peak Fire Lookout

**WLD-04 · Northern Woodlands · Small / vertical outpost · rough layout v1**

A small hillside station clusters around a steep but bounded path and a tall lookout. Investigations span hut, ruined station and clearing; the tower is an optional risky observation branch.

[Measured floor plans](plan.svg) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Forest-road van apron | [20, 72, 36, 84] | 0 / 3 m | Investigation and ordinary objects |
| entry | Lower trail gate | [20, 60, 36, 72] | 0 / 3 m | Investigation and ordinary objects |
| lower | Lower signed trail | [8, 42, 20, 60] | 0 / 3 m | Investigation and ordinary objects |
| bend | Boulder trail bend | [20, 30, 36, 42] | 3 / 3 m | Investigation and ordinary objects |
| clearing | Hillside clearing | [20, 14, 40, 30] | 6 / 3 m | Investigation and ordinary objects |
| hut | Maintenance hut | [4, 14, 20, 26] | 6 / 3 m | Investigation and ordinary objects |
| ruin | Ruined ranger station | [40, 14, 54, 30] | 6 / 3 m | Investigation and ordinary objects |
| store | Ranger store | [40, 30, 50, 40] | 6 / 3 m | Investigation and ordinary objects |
| return | Contour return path | [36, 42, 50, 60] | 3 / 3 m | Investigation and ordinary objects |
| foot | Tower footing / fenced base | [24, 2, 36, 14] | 6 / 3 m | Investigation and ordinary objects |
| landing | Tower midway landing | [24, 2, 28, 14] | 10.5 / 3 m | Investigation and ordinary objects |
| cab | Lookout cab | [24, 2, 30, 8] | 15 / 3 m | Investigation and ordinary objects |
| deck | Lookout exterior deck | [30, 2, 36, 8] | 15 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Contour return avoids requiring a retreat down the same narrow ascent. Tower has one guarded stair route and a visible warning before ascent; no mandatory anchor, rite or objective may depend on surviving a blocked cab.

- Loop: bend → clearing → hut → bend
- Loop: entry → lower → bend → clearing → return → entry

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | stage ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L02 | entry ↔ lower | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L03 | lower ↔ bend | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L04 | bend ↔ clearing | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L05 | clearing ↔ hut | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L06 | hut ↔ bend | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L07 | clearing ↔ ruin | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L08 | ruin ↔ store | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L09 | store ↔ return | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L10 | return ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L11 | return ↔ clearing | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L12 | clearing ↔ foot | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L13 | foot ↔ landing | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L14 | landing ↔ cab | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L15 | cab ↔ deck | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=28, Z=78, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[28.0, 0, 74.372], [28.0, 0, 66.0]]. Van is already staged on a prepared roadhead/service apron through expedition logistics. Travel to this remote region is a campaign transition; no new vehicle, ferry or flight gameplay is assumed.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | clearing | [30.0, 6, 22.0] | Grounded clearing containment posts |
| Cleansing | hut | [12.0, 6, 20.0] | Maintenance wash purification |
| Absolution | ruin | [47.0, 6, 22.0] | Watchkeeper remembrance station |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Wind exposure increases above the clearing. Radio, sighting table and ordinary weather instruments belong to station history, not free player equipment. Lightning exposure must be telegraphed and bounded.

## Campaign use

Final transmission transcript in station and sighting log in cab. A mission can investigate the transmissions without forcing a tower climb during a hunt.

## Refinement risks / next pass

- Refine tower into at least four actual stair flights with landings; the two diagram connectors are grouped reservations.
- The single tower exit is a deliberate risk requiring playtest; do not promise hunt fairness from graph connectivity.
- Hillside grades and contour return need terrain support and collision tests.

## Source basis

- docs/environments/WILDERNESS_AND_REMOTE_SITE_ROSTER.md
- docs/CAMPAIGN_AND_ALLIED_INVESTIGATORS_DESIGN.md
- docs/environments/SHARED_VAN_MODULE.md
