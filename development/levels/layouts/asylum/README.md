# St. Dymphna Hospital

**ASYLUM · Beckenburg district · Large / stepped wards · rough layout v1**

A stepped pavilion plan: administration faces a daylight court, with a quiet ward west and wet treatment/service spaces east. This replaces the generic cross-wing layout.

[Measured floor plans](plan.svg) · [PNG preview](plan.png) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Front-drive van apron | [28, 72, 44, 84] | 0 / 3 m | Investigation and ordinary objects |
| entry | Front drive | [28, 62, 44, 72] | 0 / 3 m | Investigation and ordinary objects |
| reception | Admissions reception | [28, 50, 44, 62] | 0 / 3 m | Investigation and ordinary objects |
| spine | Central ward corridor | [28, 20, 34, 50] | 0 / 3 m | Investigation and ordinary objects |
| records | Records office | [16, 50, 28, 62] | 0 / 3 m | Investigation and ordinary objects |
| day | Day room | [10, 34, 28, 50] | 0 / 3 m | Investigation and ordinary objects |
| ward | Patient ward | [0, 18, 22, 34] | 0 / 3 m | Investigation and ordinary objects |
| west-link | Garden ward passage | [22, 18, 28, 34] | 0 / 3 m | Investigation and ordinary objects |
| treatment | Treatment theatre | [34, 20, 50, 34] | 0 / 3 m | Investigation and ordinary objects |
| hydro | Hydrotherapy room | [50, 20, 64, 34] | 0 / 3 m | Investigation and ordinary objects |
| dining | Dining ward | [34, 34, 54, 50] | 0 / 3 m | Investigation and ordinary objects |
| chapel | Remembrance chapel | [10, 2, 28, 18] | 0 / 3 m | Investigation and ordinary objects |
| court | Walled courtyard | [0, 34, 10, 62] | 0 / 3 m | Investigation and ordinary objects |
| upper | Upper ward gallery | [28, 20, 34, 50] | 3.4 / 3 m | Investigation and ordinary objects |
| upper-ward | Upper patient ward | [10, 34, 28, 50] | 3.4 / 3 m | Investigation and ordinary objects |
| plant | Basement boiler | [34, 34, 54, 50] | -3 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Administration/day-room loop provides a short return. East wet-treatment loop has two independent doors. West passage connects ward, chapel and garden exit; patient wards remain ordinary inhabited-space histories with protected personal accounts.

- Loop: reception → records → day → spine → reception
- Loop: spine → treatment → hydro → dining → spine

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | stage ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L02 | entry ↔ reception | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L03 | reception ↔ records | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L04 | records ↔ day | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L05 | day ↔ spine | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L06 | spine ↔ reception | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L07 | spine ↔ west-link | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L08 | west-link ↔ ward | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L09 | ward ↔ chapel | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L10 | chapel ↔ west-link | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L11 | day ↔ court | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L12 | court ↔ ward | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L13 | spine ↔ treatment | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L14 | treatment ↔ hydro | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L15 | hydro ↔ dining | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L16 | dining ↔ spine | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L17 | spine ↔ upper | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L18 | upper ↔ upper-ward | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L19 | dining ↔ plant | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=36, Z=78, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[36.0, 0, 74.372], [36.0, 0, 67.0]]. Existing shared van on a level service apron/roadhead; no new transport mechanic.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | treatment | [42.0, 0, 27.0] | Copper containment ring |
| Cleansing | hydro | [57.0, 0, 27.0] | Draining cleansing basin |
| Absolution | chapel | [19.0, 0, 10.0] | Patient remembrance stand |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Wet-room water, boiler heat and ward ventilation produce normal baselines. Separate ward and clinical circuits avoid a single inexplicable all-building switch. Court light and window sightlines orient both players and later allies.

## Campaign use

Case files and staff correspondence present conflicting testimony respectfully. Neither diagnoses nor treatment history prescribe ghost hostility or evidence.

## Refinement risks / next pass

- Staggered ward concept informed by primary DC planning brochure; inspect actual floor drawings before detailed construction.
- Add believable sanitary/service rooms as child spaces; preserve broad corridor turn clearance.
- Draw basement stairs and evacuation route; avoid an upper ward with a single untelegraphed trap.

## Source basis

- docs/environments/ROOM_AND_ASSET_REQUIREMENTS.md
- https://planning.dc.gov/sites/default/files/dc/sites/op/publication/attachments/St%20Elizabeths%20Brochure.pdf (indexed text inspected 2026-10-21; wards staggered for light; dimensions authored)
