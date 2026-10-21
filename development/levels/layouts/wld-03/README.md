# Red Mesa Ancestral Caves

**WLD-03 · Western Badlands · Medium-large / canyon and caves · rough layout v1**

A bounded canyon floor rises through a switchback terrace into fictional cliff dwellings and branching caves. Modern expedition traces provide the investigation installations.

[Measured floor plans](plan.svg) · [PNG preview](plan.png) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Canyon-road van apron | [26, 80, 42, 92] | 0 / 3 m | Investigation and ordinary objects |
| entry | Canyon mouth | [26, 66, 42, 80] | 0 / 3 m | Investigation and ordinary objects |
| floor | Canyon floor | [20, 40, 48, 66] | 0 / 3 m | Investigation and ordinary objects |
| shelter | Modern expedition shelter | [4, 46, 20, 58] | 0 / 3 m | Investigation and ordinary objects |
| wash | Expedition sample shelter | [48, 46, 60, 58] | 0 / 3 m | Investigation and ordinary objects |
| lower-path | Lower switchback terrace | [4, 22, 20, 46] | 4 / 3 m | Investigation and ordinary objects |
| upper-path | Upper switchback terrace | [20, 10, 48, 22] | 8 / 3 m | Investigation and ordinary objects |
| dwellings | Fictional cliff-dwelling terrace | [20, 22, 48, 40] | 8 / 3 m | Investigation and ordinary objects |
| gallery | Cave entrance gallery | [48, 22, 66, 40] | 8 / 3 m | Investigation and ordinary objects |
| fork | Branching cave junction | [66, 22, 76, 40] | 8 / 3 m | Investigation and ordinary objects |
| chamber | Dry side chamber | [66, 4, 82, 22] | 8 / 3 m | Investigation and ordinary objects |
| lower-cave | Lower cave chamber | [48, 22, 66, 40] | 4 / 3 m | Investigation and ordinary objects |
| return | Rock-arch return shelf | [48, 4, 66, 22] | 4 / 3 m | Investigation and ordinary objects |
| alcove | Expedition records alcove | [48, 40, 60, 52] | 8 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Two physical cave entrances connect upper terrace and lower return shelf. Switchback reservations require generous terrain runs; no ladders, jumps or spectral-only shortcut can be mandatory for hunters. Terminal alcove stays a short branch.

- Loop: lower-path → upper-path → dwellings → gallery → lower-cave → return → lower-path
- Loop: upper-path → dwellings → gallery → fork → chamber → upper-path

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | stage ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L02 | entry ↔ floor | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L03 | floor ↔ shelter | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L04 | shelter ↔ lower-path | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L05 | floor ↔ wash | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L06 | wash ↔ floor | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L07 | lower-path ↔ upper-path | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L08 | upper-path ↔ dwellings | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L09 | dwellings ↔ gallery | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L10 | gallery ↔ fork | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L11 | fork ↔ chamber | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L12 | chamber ↔ upper-path | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L13 | gallery ↔ lower-cave | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L14 | lower-cave ↔ return | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L15 | return ↔ lower-path | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L16 | gallery ↔ alcove | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=34, Z=86, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[34.0, 0, 82.372], [34.0, 0, 73.0]]. Van is already staged on a prepared roadhead/service apron through expedition logistics. Travel to this remote region is a campaign transition; no new vehicle, ferry or flight gameplay is assumed.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | shelter | [12.0, 0, 52.0] | Modern expedition containment mat |
| Cleansing | wash | [54.0, 0, 52.0] | Modern sample purification basin |
| Absolution | chamber | [74.0, 8, 13.0] | Missing expedition remembrance stand |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Sun/shade temperature, wind at mouths and dry cave reverberation remain environmental baselines. Named elevation bands replace ambiguous cave-floor room detection; rock walls must constrain both ghost controllers.

## Campaign use

Archaeological record discrepancies and a later modern expedition form separate narrative layers. The culture and site are fictional; no real burial tradition is used as an evil source. Future explicit Diné/Navajo identity would require separate research/consultation.

## Refinement risks / next pass

- Third remote exemplar: shape actual irregular chamber polygons and switchbacks before building art.
- The +4/+8 m terraces are reservations, not proven graded terrain. Engineer two-way routes, headroom and visible falls boundaries.
- Modern rite installations must leave the fictional ancient fabric intact; validate all three against ghost interference.

## Source basis

- docs/environments/WILDERNESS_AND_REMOTE_SITE_ROSTER.md
- docs/CAMPAIGN_AND_ALLIED_INVESTIGATORS_DESIGN.md
- docs/environments/SHARED_VAN_MODULE.md
- https://www.nps.gov/meve/learn/nature/geology.htm (text/search record inspected 2026-10-21: alcove-forming sandstone context only; no culture, plan or dimensions copied)
