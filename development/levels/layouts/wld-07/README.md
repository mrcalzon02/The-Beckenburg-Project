# Saint Bartholomew's Forgotten Cemetery

**WLD-07 · Northern Woodlands · Small-medium / enclosed grounds · rough layout v1**

A bounded burial ground is organized by a main avenue and memorial ring. Ruined chapel and caretaker cottage face the approach; two ossuary entrances join underground.

[Measured floor plans](plan.svg) · [PNG preview](plan.png) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Cemetery-lane van apron | [20, 72, 36, 84] | 0 / 3 m | Investigation and ordinary objects |
| entry | Lychgate and entry court | [20, 60, 36, 72] | 0 / 3 m | Investigation and ordinary objects |
| avenue | Main cemetery avenue | [24, 20, 32, 60] | 0 / 3 m | Investigation and ordinary objects |
| chapel | Ruined chapel | [6, 38, 24, 56] | 0 / 3 m | Investigation and ordinary objects |
| cottage | Caretaker cottage | [32, 38, 46, 56] | 0 / 3 m | Investigation and ordinary objects |
| garden | Memorial garden | [6, 20, 24, 38] | 0 / 3 m | Investigation and ordinary objects |
| east-graves | East grave plots | [32, 20, 50, 38] | 0 / 3 m | Investigation and ordinary objects |
| old-graves | Old numbered markers | [16, 4, 40, 20] | 0 / 3 m | Investigation and ordinary objects |
| tool | Caretaker tool store | [46, 42, 54, 52] | 0 / 3 m | Investigation and ordinary objects |
| west-stair | West ossuary vestibule | [6, 12, 16, 20] | 0 / 3 m | Investigation and ordinary objects |
| east-stair | East ossuary vestibule | [40, 12, 50, 20] | 0 / 3 m | Investigation and ordinary objects |
| ossuary | Ossuary passage | [16, 12, 40, 20] | -3 / 3 m | Investigation and ordinary objects |
| chamber | Memorial record chamber | [16, 0, 28, 12] | -3 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Avenue and marker ring provide two surface search loops. Both underground vestibules have full stairs with matching openings; the record chamber is a short branch. Headstones remain fixed sightline objects across all detail settings.

- Loop: entry → chapel → garden → avenue → entry
- Loop: avenue → garden → old-graves → east-graves → avenue

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | stage ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L02 | entry ↔ avenue | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L03 | entry ↔ chapel | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L04 | entry ↔ cottage | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. Connecting path needs an explicit supported corridor during shell/terrain refinement. |
| L05 | chapel ↔ garden | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L06 | garden ↔ avenue | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L07 | avenue ↔ east-graves | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L08 | east-graves ↔ cottage | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L09 | garden ↔ old-graves | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L10 | old-graves ↔ east-graves | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L11 | cottage ↔ tool | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L12 | garden ↔ west-stair | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L13 | east-graves ↔ east-stair | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L14 | west-stair ↔ ossuary | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L15 | ossuary ↔ east-stair | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L16 | ossuary ↔ chamber | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=28, Z=78, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[28.0, 0, 74.372], [28.0, 0, 66.0]]. Van is already staged on a prepared roadhead/service apron through expedition logistics. Travel to this remote region is a campaign transition; no new vehicle, ferry or flight gameplay is assumed.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | chamber | [22.0, -3, 6.0] | Modern removable containment seal |
| Cleansing | cottage | [39.0, 0, 47.0] | Caretaker wash purification |
| Absolution | garden | [15.0, 0, 29.0] | Memorial reconciliation stand |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Tree cover, wind, surface water and enclosed stone passages have distinct sound/temperature baselines. Memorial records offer narrative rather than a magic detector reading.

## Campaign use

Numbered markers, caretaker succession and missing parish entries connect provisionally to St. Brigid and Ashen Bells. Ordinary burial and remembrance are not implied sources of evil.

## Refinement risks / next pass

- Research burial-ground circulation and ossuary structure before detailed construction; keep fictional history explicit.
- Place no rites on graves or inaccessible marker tops; all reservation squares are on clear paths/floors.
- Test underground exit visibility and tree-occlusion consistency.

## Source basis

- docs/environments/WILDERNESS_AND_REMOTE_SITE_ROSTER.md
- docs/CAMPAIGN_AND_ALLIED_INVESTIGATORS_DESIGN.md
- docs/environments/SHARED_VAN_MODULE.md
