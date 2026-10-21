# Gallows Pass Railway

**WLD-12 · Highland Frontier · Medium-large / linear railway with return · rough layout v1**

A bounded railway cutting runs past derailed coaches to a tunnel. A maintenance siding path parallels the track, while a guarded trestle has a service walkway beneath its deck.

[Measured floor plans](plan.svg) · [PNG preview](plan.png) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Maintenance-road van apron | [28, 88, 44, 100] | 0 / 3 m | Investigation and ordinary objects |
| entry | Rail access gate | [28, 76, 44, 88] | 0 / 3 m | Investigation and ordinary objects |
| signal | Signal hut | [12, 62, 28, 76] | 0 / 3 m | Investigation and ordinary objects |
| cutting | Rail cutting | [28, 42, 40, 76] | 0 / 3 m | Investigation and ordinary objects |
| coach-a | Derailed passenger coach | [16, 46, 24, 62] | 0 / 3 m | Investigation and ordinary objects |
| coach-b | Derailed baggage coach | [44, 46, 52, 62] | 0 / 3 m | Investigation and ordinary objects |
| siding | Maintenance siding | [52, 30, 62, 76] | 0 / 3 m | Investigation and ordinary objects |
| store | Track stores hut | [52, 16, 64, 30] | 0 / 3 m | Investigation and ordinary objects |
| trestle | Trestle guarded deck | [28, 22, 40, 42] | 0 / 3 m | Investigation and ordinary objects |
| tunnel | Tunnel mouth / dry bore | [28, -8, 40, 22] | 0 / 3 m | Investigation and ordinary objects |
| refuge | Tunnel maintenance refuge | [40, 0, 52, 12] | 0 / 3 m | Investigation and ordinary objects |
| lower | Lower service walkway | [28, 22, 34, 42] | -3 / 3 m | Investigation and ordinary objects |
| west-return | Cutting west return path | [6, 22, 16, 62] | 0 / 3 m | Investigation and ordinary objects |
| crossing | North maintenance crossing | [40, 12, 62, 22] | 0 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Parallel siding prevents the entire mission becoming a single corridor. Coach interiors have two ground-supported exits; derailment does not imply mandatory climbing. Tunnel refuge cross-passage returns to the siding; trestle walkway provides a guarded secondary path.

- Loop: entry → signal → cutting → coach-b → siding → entry
- Loop: trestle → tunnel → refuge → crossing → trestle

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | stage ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L02 | entry ↔ signal | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L03 | signal ↔ cutting | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L04 | entry ↔ siding | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L05 | cutting ↔ coach-a | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L06 | coach-a ↔ west-return | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L07 | west-return ↔ signal | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L08 | cutting ↔ coach-b | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L09 | coach-b ↔ siding | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L10 | cutting ↔ trestle | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L11 | trestle ↔ tunnel | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L12 | tunnel ↔ refuge | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L13 | refuge ↔ crossing | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L14 | crossing ↔ siding | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L15 | siding ↔ store | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L16 | trestle ↔ lower | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L17 | lower ↔ west-return | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L18 | crossing ↔ trestle | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |

## Arrival and extraction

Shared van placement: X=36, Z=94, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[36.0, 0, 90.372], [36.0, 0, 82.0]]. Van is already staged on a prepared roadhead/service apron through expedition logistics. Travel to this remote region is a campaign transition; no new vehicle, ferry or flight gameplay is assumed.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | store | [58.0, 0, 23.0] | Track-store containment frame |
| Cleansing | refuge | [46.0, 0, 6.0] | Maintenance purification stand |
| Absolution | signal | [20.0, 0, 69.0] | Passenger remembrance records table |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Wind through cutting, loose sheet metal, signal wiring and rail-borne sound remain ordinary baselines. There is no moving train hazard until actual telegraph and simulation support exists.

## Campaign use

Signal register, carriage inventories and timetable contradictions form a transport case that can tie remote supply routes together without prescribing supernatural cause.

## Refinement risks / next pass

- Make coach orientation and end-door support physically believable; the rectangles are footprint reservations.
- Engineer trestle deck, lower stairs, rail barriers and tunnel rock support; visible voids cannot be traversable ghost-only shortcuts.
- Limit mission to the authored tunnel segment; a locked maintenance end is a visible world boundary, not an infinite railway.

## Source basis

- docs/environments/WILDERNESS_AND_REMOTE_SITE_ROSTER.md
- docs/CAMPAIGN_AND_ALLIED_INVESTIGATORS_DESIGN.md
- docs/environments/SHARED_VAN_MODULE.md
