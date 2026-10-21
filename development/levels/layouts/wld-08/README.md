# Northwatch Research Station

**WLD-08 · Northern Expanse · Medium / polar compound · rough layout v1**

A compact ring of insulated prefabs surrounds a flagged service court. A heated tunnel links lab, dormitory and generator while the instrument array remains an exposed optional branch.

[Measured floor plans](plan.svg) · [PNG preview](plan.png) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Prepared station van apron | [24, 70, 40, 82] | 0 / 3 m | Investigation and ordinary objects |
| entry | Flagged arrival lane | [24, 56, 40, 70] | 0 / 3 m | Investigation and ordinary objects |
| court | Station service court | [20, 26, 44, 56] | 0 / 3 m | Investigation and ordinary objects |
| lab | Observation laboratory | [2, 26, 20, 44] | 0 / 3 m | Investigation and ordinary objects |
| dorm | Dormitory | [44, 26, 62, 44] | 0 / 3 m | Investigation and ordinary objects |
| mess | Mess / galley | [44, 44, 62, 56] | 0 / 3 m | Investigation and ordinary objects |
| radio | Radio room | [20, 12, 32, 26] | 0 / 3 m | Investigation and ordinary objects |
| records | Log archive | [32, 12, 44, 26] | 0 / 3 m | Investigation and ordinary objects |
| generator | Generator / water shed | [2, 8, 20, 26] | 0 / 3 m | Investigation and ordinary objects |
| array | Exterior instrument array | [44, 0, 62, 20] | 0 / 3 m | Investigation and ordinary objects |
| store | Cold stores | [2, 44, 20, 56] | 0 / 3 m | Investigation and ordinary objects |
| tunnel | Heated service tunnel | [20, 26, 44, 36] | -2.5 / 3 m | Investigation and ordinary objects |
| tunnel-n | Generator tunnel branch | [2, 18, 20, 26] | -2.5 / 3 m | Investigation and ordinary objects |
| shelter | Array wind shelter | [62, 8, 70, 20] | 0 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Flagged court is the visual reference; return routes remain traversable in all allowed weather. Heated tunnel gives an interior alternative rather than a mandatory long snow crossing. Array is visible from the radio room and has a near shelter.

- Loop: entry → court → dorm → mess → entry
- Loop: court → lab → generator → radio → court

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | stage ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L02 | entry ↔ court | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L03 | court ↔ lab | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L04 | lab ↔ store | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L05 | store ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L06 | court ↔ dorm | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L07 | dorm ↔ mess | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L08 | mess ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L09 | court ↔ radio | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L10 | radio ↔ records | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L11 | records ↔ court | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L12 | radio ↔ generator | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L13 | generator ↔ lab | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L14 | records ↔ array | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L15 | array ↔ dorm | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L16 | array ↔ shelter | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L17 | lab ↔ tunnel | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L18 | tunnel ↔ dorm | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L19 | tunnel ↔ tunnel-n | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L20 | tunnel-n ↔ generator | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=32, Z=76, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[32.0, 0, 72.372], [32.0, 0, 63.0]]. Van is already staged on a prepared roadhead/service apron through expedition logistics. Travel to this remote region is a campaign transition; no new vehicle, ferry or flight gameplay is assumed.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | lab | [11.0, 0, 35.0] | Anchored research containment frame |
| Cleansing | mess | [53.0, 0, 50.0] | Galley purification basin |
| Absolution | radio | [26.0, 0, 19.0] | Crew remembrance station |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Outdoor freezing is weather, never automatic paranormal evidence. Calibrated inside/outside baselines, generator vibration and ventilation must preserve tool usefulness. Snow visuals may not erase paths or alter collision per client.

## Campaign use

Observation logs and contradictory transmissions support the remote campaign arc. Van deployment is a pre-established station logistics arrangement; this plan creates no polar driving minigame.

## Refinement risks / next pass

- Verify physical prefabs and tunnel fire/vent separation with primary station references before detailed construction.
- Snowdrift barriers must leave two clear loops and one readable retreat route at every allowed condition.
- Station expansion and radio plot remain design-only until persistence and legal events exist.

## Source basis

- docs/environments/WILDERNESS_AND_REMOTE_SITE_ROSTER.md
- docs/CAMPAIGN_AND_ALLIED_INVESTIGATORS_DESIGN.md
- docs/environments/SHARED_VAN_MODULE.md
- https://www.bas.ac.uk/project/signy-research-station-modernisation/ (text inspected 2026-10-21: separate house, stores and generator functions only; fictional station geometry authored)

## Shared world thread

[Emergence Day and survival thread](LORE.md): connect this location to late 2022 and the long reclamation without prescribing its current ghost.
