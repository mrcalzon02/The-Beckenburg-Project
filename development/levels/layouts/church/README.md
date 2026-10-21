# St. Brigid Parish

**CHURCH · Beckenburg district · Established reference · rough layout v1**

Trace of current authored architecture for review. Preserve its measured footprint and floor elevations while planning the full roster.

[Measured floor plans](plan.svg) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| r00 | Nave | [18.0, 12.0, 26.0, 34.0] | 0 / 6 m | Investigation and ordinary objects |
| r01 | Vestibule | [18.0, 34.0, 26.0, 39.0] | 0 / 3 m | Investigation and ordinary objects |
| r02 | Parish office | [10.0, 26.0, 18.0, 34.0] | 0 / 3 m | Investigation and ordinary objects |
| r03 | Confessional chamber | [10.0, 18.0, 18.0, 26.0] | 0 / 3 m | Investigation and ordinary objects |
| r04 | Chancel and altar | [18.0, 6.0, 26.0, 12.0] | 0 / 5 m | Investigation and ordinary objects |
| r05 | Side chapel | [26.0, 14.0, 34.0, 26.0] | 0 / 4 m | Investigation and ordinary objects |
| r06 | Vestry | [26.0, 6.0, 34.0, 14.0] | 0 / 3 m | Investigation and ordinary objects |
| r07 | Choir loft | [18.0, 26.0, 26.0, 34.0] | 3 / 3 m | Investigation and ordinary objects |
| r08 | Bell tower stair chamber | [10.0, 26.0, 18.0, 34.0] | 3 / 3 m | Investigation and ordinary objects |
| r09 | Organ archive | [26.0, 26.0, 34.0, 34.0] | 3 / 3 m | Investigation and ordinary objects |
| r10 | Belfry | [10.0, 26.0, 18.0, 34.0] | 6 / 3 m | Investigation and ordinary objects |
| r11 | Crypt | [10.0, 6.0, 34.0, 34.0] | -3 / 3 m | Investigation and ordinary objects |
| grounds | Rear grounds / exterior | [4, 0, 40, 39] | 0 / 12 m | Investigation and ordinary objects |
| entry | Approach / arrival | [0, 39, 40, 58.4] | 0 / 12 m | Investigation and ordinary objects |
| stage | Van service apron | [14, 45, 30, 57] | 0 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Use the existing interior loops and distinct rear exits. Staging, outdoor grounds and exact door/stair construction remain governed by current runtime coordinates.

- Loop: r00 → r02 → r03 → r00
- Loop: r00 → r04 → r06 → r05 → r00

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | r00 ↔ r01 | 1.5 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L02 | r00 ↔ r02 | 1.5 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L03 | r00 ↔ r03 | 1.5 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L04 | r00 ↔ r04 | 1.5 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L05 | r00 ↔ r05 | 1.5 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L06 | r05 ↔ r06 | 1.5 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L07 | r04 ↔ r06 | 1.5 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L08 | r02 ↔ r03 | 1.5 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L09 | r07 ↔ r08 | 1.5 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L10 | r07 ↔ r09 | 1.5 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L11 | r01 ↔ entry | 2 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L12 | r06 ↔ grounds | 2 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L13 | r00 ↔ r07 | 1.5 m | Existing stair relation; preserve actual separate runtime flights, deck cuts and guards. |
| L14 | r11 ↔ r00 | 1.5 m | Existing stair relation; preserve actual separate runtime flights, deck cuts and guards. |
| L15 | r08 ↔ r10 | 1.5 m | Existing stair relation; preserve actual separate runtime flights, deck cuts and guards. |
| L16 | grounds ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L17 | entry ↔ stage | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=22, Z=51, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[22, 0, 47.372], [20.0, 0, 48.7]]. Existing shared van on a level service apron/roadhead; no new transport mechanic.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | r11 | [22.0, -3.0, 20.0] | Crypt binding dais |
| Cleansing | r04 | [22.0, 0.0, 9.0] | Chancel purification vessel |
| Absolution | r05 | [30.0, 0.0, 20.0] | Side chapel remembrance altar |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Keep the current long nave, separate chancel, side chapel and vestry. Choir at +3, belfry at +6 and crypt at -3 provide contrasting vertical acoustics.

## Campaign use

Parish records can link cemetery and monastery stories. Church remains an investigation site, distinct from Castle sanctuary services.

## Refinement risks / next pass

- This existing church definition already has a distinct plan and is retained, although it remains locked pending production acceptance.
- The partial choir loft must leave the nave volume open; do not infer a full deck from stacked room bounds.
- Review tower stair access and crypt route for escape fairness; no permanently safe elevated rite location.

## Source basis

- src/main/java/org/beckenburg/LevelPlans.java (house/farm/school methods, read 2026-10-21)
- development/levels/ARCHITECTURAL_REFERENCES.md
- docs/environments/THREE_RITUAL_SITES.md
