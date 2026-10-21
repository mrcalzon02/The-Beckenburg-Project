# Camp Hollow Creek

**WLD-02 · Northern Woodlands · Medium / lakeside campground · rough layout v1**

Two short woodland trail loops link readable tent clusters, ranger services and lake frontage. The lake is a hard boundary, with no required swimming or boat traversal.

[Measured floor plans](plan.svg) · [PNG preview](plan.png) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Camp-road van apron | [24, 72, 40, 84] | 0 / 3 m | Investigation and ordinary objects |
| entry | Ranger gate / noticeboard | [24, 60, 40, 72] | 0 / 3 m | Investigation and ordinary objects |
| ranger | Ranger cabin | [8, 48, 22, 60] | 0 / 3 m | Investigation and ordinary objects |
| bath | Bathhouse | [42, 48, 56, 60] | 0 / 3 m | Investigation and ordinary objects |
| pavilion | Dining pavilion | [24, 34, 40, 50] | 0 / 3 m | Investigation and ordinary objects |
| camp-a | West tent pitches | [6, 28, 22, 46] | 0 / 3 m | Investigation and ordinary objects |
| camp-b | East tent pitches | [42, 28, 58, 46] | 0 / 3 m | Investigation and ordinary objects |
| junction | Trail-marker junction | [24, 20, 40, 34] | 0 / 3 m | Investigation and ordinary objects |
| camper | Abandoned camper | [8, 14, 18, 26] | 0 / 3 m | Investigation and ordinary objects |
| shore | Lakeside clearing | [24, 4, 40, 20] | 0 / 3 m | Investigation and ordinary objects |
| dock | Lake dock | [24, -10, 32, 4] | 0 / 3 m | Investigation and ordinary objects |
| creek | Creek footbridge | [42, 8, 54, 18] | 0 / 3 m | Investigation and ordinary objects |
| store | Ranger tool store | [2, 48, 8, 58] | 0 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Ranger, pavilion and bathhouse roofs act as landmarks. A central path and two signed loop trails keep the search bounded. Dock is a short visible terminal branch with no mandatory rite and no safe-water escape exploit.

- Loop: entry → ranger → camp-a → pavilion → bath → entry
- Loop: junction → shore → creek → camp-b → junction

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | stage ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L02 | entry ↔ ranger | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L03 | entry ↔ bath | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L04 | ranger ↔ camp-a | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L05 | camp-a ↔ pavilion | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L06 | pavilion ↔ bath | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L07 | pavilion ↔ camp-b | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L08 | camp-b ↔ junction | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L09 | junction ↔ camp-a | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L10 | camp-a ↔ camper | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L11 | junction ↔ shore | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L12 | shore ↔ dock | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L13 | shore ↔ creek | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L14 | creek ↔ camp-b | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L15 | ranger ↔ store | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=32, Z=78, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[32.0, 0, 74.372], [32.0, 0, 66.0]]. Van is already staged on a prepared roadhead/service apron through expedition logistics. Travel to this remote region is a campaign transition; no new vehicle, ferry or flight gameplay is assumed.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | shore | [32.0, 0, 12.0] | Temporary expedition containment posts |
| Cleansing | bath | [49.0, 0, 54.0] | Bathhouse cleansing station |
| Absolution | pavilion | [32.0, 0, 42.0] | Guest remembrance stand |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Rain runoff, creek audio, lake wind and ordinary campfire remnants have explicit baseline causes. Sheltered device-placement surfaces belong to actual deployed loadouts; no free equipment is scenery.

## Campaign use

Guest ledger at ranger desk, mismatched trail markers and an abandoned camper account. Revisit exposes alternate interpretation of the same physical paths rather than inventing an unbuilt island.

## Refinement risks / next pass

- Second remote exemplar: test shelter/calibration and tent visibility without foliage changing hiding geometry.
- Refine boardwalk/bridge supports, rails, lake collision and dock retreat timing.
- References and campground-specific sanitary/service construction still need measured review.

## Source basis

- docs/environments/WILDERNESS_AND_REMOTE_SITE_ROSTER.md
- docs/CAMPAIGN_AND_ALLIED_INVESTIGATORS_DESIGN.md
- docs/environments/SHARED_VAN_MODULE.md
