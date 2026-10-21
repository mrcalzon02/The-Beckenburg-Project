# Blackwater Hydroelectric Dam

**WLD-09 · Highland Frontier · Large / industrial vertical · rough layout v1**

The roadhead feeds a dam crest and an offset powerhouse. A guarded external stair and internal service stair connect crest, turbine floor and lower galleries without entering the spillway.

[Measured floor plans](plan.svg) · [PNG preview](plan.png) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Dam access van apron | [28, 76, 44, 88] | 0 / 3 m | Investigation and ordinary objects |
| entry | Dam service forecourt | [28, 62, 44, 76] | 0 / 3 m | Investigation and ordinary objects |
| crest | Guarded dam crest | [8, 48, 72, 56] | 0 / 3 m | Investigation and ordinary objects |
| control | Control room | [8, 34, 26, 48] | 0 / 3 m | Investigation and ordinary objects |
| office | Shift records office | [8, 22, 26, 34] | 0 / 3 m | Investigation and ordinary objects |
| west-stair | West service stair enclosure | [26, 22, 36, 48] | 0 / 3 m | Investigation and ordinary objects |
| turbine | Turbine hall | [36, 22, 64, 48] | -6 / 8 m | Investigation and ordinary objects |
| workshop | Maintenance workshop | [36, 48, 54, 62] | -6 / 3 m | Investigation and ordinary objects |
| east-stair | East external stair court | [64, 22, 76, 48] | 0 / 3 m | Investigation and ordinary objects |
| gallery | Lower service gallery | [36, 22, 64, 32] | -10 / 3 m | Investigation and ordinary objects |
| valve | Valve inspection room | [36, 32, 50, 48] | -10 / 3 m | Investigation and ordinary objects |
| return | Lower maintenance return | [50, 32, 64, 48] | -10 / 3 m | Investigation and ordinary objects |
| spillway | Guarded spillway observation | [64, 48, 76, 62] | -6 / 3 m | Investigation and ordinary objects |
| plant | Switchgear room | [36, 8, 54, 22] | -6 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Two full-height routes reach turbine floor. Lower gallery has a ring around isolated service rooms and two ways up. Observation pad is behind permanent rails; flowing water and working penstocks are outside legal hunter/ghost navigation.

- Loop: crest → west-stair → turbine → east-stair → crest
- Loop: gallery → valve → return → gallery

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | stage ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L02 | entry ↔ crest | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L03 | crest ↔ control | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L04 | control ↔ office | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L05 | office ↔ west-stair | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L06 | west-stair ↔ crest | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L07 | west-stair ↔ turbine | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L08 | turbine ↔ workshop | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L09 | workshop ↔ east-stair | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L10 | east-stair ↔ crest | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L11 | turbine ↔ east-stair | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L12 | east-stair ↔ spillway | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L13 | turbine ↔ plant | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L14 | turbine ↔ gallery | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L15 | gallery ↔ valve | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L16 | valve ↔ return | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L17 | return ↔ gallery | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L18 | return ↔ east-stair | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=36, Z=82, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[36.0, 0, 78.372], [36.0, 0, 69.0]]. Van is already staged on a prepared roadhead/service apron through expedition logistics. Travel to this remote region is a campaign transition; no new vehicle, ferry or flight gameplay is assumed.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | workshop | [45.0, -6, 55.0] | Grounded industrial containment seal |
| Cleansing | valve | [43.0, -10, 40.0] | Isolated purification vessel |
| Absolution | office | [17.0, 0, 28.0] | Shift memorial records stand |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Turbine vibration, electrical fields, ventilation and cold water all have real sources. Isolation controls change machinery state visibly; ghost events cannot falsify a reading without an allowed world action.

## Campaign use

Shift logs and mismatched alarm times underpin an industrial case. The crest provides a distant regional landmark without implying a whole reservoir is playable.

## Refinement risks / next pass

- Plan actual turbine machinery bays and guarded openings; the hall volume is not final plant geometry.
- Engineer 6 m descents with switchback landings; schematic lines must not become unsupported ramps.
- Study industrial electrical separation, acoustics and usable inspection paths before runtime authoring.

## Source basis

- docs/environments/WILDERNESS_AND_REMOTE_SITE_ROSTER.md
- docs/CAMPAIGN_AND_ALLIED_INVESTIGATORS_DESIGN.md
- docs/environments/SHARED_VAN_MODULE.md
- https://www.nps.gov/pagr/learn/historyculture/hydroelectric-plant.htm (text inspected 2026-10-21: turbine-hall function; not a reproduced dam plan)
