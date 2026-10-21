# Ravenshade School

**SCHOOL · Beckenburg district · Established reference · rough layout v1**

Trace of current authored architecture for review. Preserve its measured footprint and floor elevations while planning the full roster.

[Measured floor plans](plan.svg) · [PNG preview](plan.png) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| r00 | Front office and foyer | [20.0, 32.0, 28.0, 38.0] | 0 / 3.6 m | Investigation and ordinary objects |
| r01 | Main school corridor | [20.0, 12.0, 28.0, 32.0] | 0 / 3.6 m | Investigation and ordinary objects |
| r02 | Classroom A | [10.0, 24.0, 20.0, 38.0] | 0 / 3.6 m | Investigation and ordinary objects |
| r03 | Science classroom | [10.0, 12.0, 20.0, 24.0] | 0 / 3.6 m | Investigation and ordinary objects |
| r04 | Cafeteria | [28.0, 24.0, 40.0, 38.0] | 0 / 3.6 m | Investigation and ordinary objects |
| r05 | Nurse office | [28.0, 18.0, 34.0, 24.0] | 0 / 3.6 m | Investigation and ordinary objects |
| r06 | Gymnasium | [34.0, 8.0, 46.0, 24.0] | 0 / 7.2 m | Investigation and ordinary objects |
| r07 | Upper locker corridor | [20.0, 12.0, 28.0, 38.0] | 3.6 / 3.6 m | Investigation and ordinary objects |
| r08 | School library | [10.0, 24.0, 20.0, 38.0] | 3.6 / 3.6 m | Investigation and ordinary objects |
| r09 | Classroom B | [10.0, 12.0, 20.0, 24.0] | 3.6 / 3.6 m | Investigation and ordinary objects |
| r10 | Assembly hall | [28.0, 24.0, 40.0, 38.0] | 3.6 / 4.5 m | Investigation and ordinary objects |
| r11 | Boiler and electrical room | [10.0, 12.0, 34.0, 38.0] | -3 / 3 m | Investigation and ordinary objects |
| grounds | Rear grounds / exterior | [5, 1, 51, 38] | 0 / 12 m | Investigation and ordinary objects |
| entry | Approach / arrival | [1, 38, 51, 58.4] | 0 / 12 m | Investigation and ordinary objects |
| stage | Van service apron | [20.0, 45, 36.0, 57] | 0 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Use the existing interior loops and distinct rear exits. Staging, outdoor grounds and exact door/stair construction remain governed by current runtime coordinates.

- Loop: r00 → r02 → r01 → r00
- Loop: r04 → r05 → r06 → r04

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | r00 ↔ r01 | 1.8 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L02 | r00 ↔ r02 | 1.35 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L03 | r00 ↔ r04 | 1.35 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L04 | r01 ↔ r02 | 1.35 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L05 | r01 ↔ r03 | 1.35 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L06 | r01 ↔ r05 | 1.2 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L07 | r02 ↔ r03 | 1.2 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L08 | r04 ↔ r05 | 1.2 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L09 | r04 ↔ r06 | 1.8 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L10 | r05 ↔ r06 | 1.35 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L11 | r07 ↔ r08 | 1.35 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L12 | r07 ↔ r09 | 1.35 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L13 | r07 ↔ r10 | 1.8 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L14 | r08 ↔ r09 | 1.2 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L15 | r00 ↔ entry | 1.8 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L16 | r03 ↔ grounds | 1.2 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L17 | r06 ↔ grounds | 1.8 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L18 | r01 ↔ r07 | 1.5 m | Existing stair relation; preserve actual separate runtime flights, deck cuts and guards. |
| L19 | r11 ↔ r01 | 1.5 m | Existing stair relation; preserve actual separate runtime flights, deck cuts and guards. |
| L20 | grounds ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L21 | entry ↔ stage | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=28, Z=51, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[28.0, 0, 47.372], [26.0, 0, 48.2]]. Existing shared van on a level service apron/roadhead; no new transport mechanic.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | r06 | [40.0, 0.0, 16.0] | Gym containment square |
| Cleansing | r03 | [15.0, 0.0, 18.0] | Science wash station |
| Absolution | r08 | [15.0, 3.6, 31.0] | Library remembrance desk |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Ground teaching wings, upper library/assembly at +3.6, service basement at -3. Gym is a distinct 7.2 m high offset volume. Bells, water and plant sounds remain ordinary until a lawful event changes them.

## Campaign use

Records and witness recollections support a longer institutional search. Ghost campaign later reuses the same doors and stairs.

## Refinement risks / next pass

- Keep gym roof and upper assembly volume independent; avoid phantom floors over the gym.
- Two routes connect gym/cafeteria/nurse wing; verify real door swings and refuge positions at full team capacity.
- Library rite requires a real cleared furniture area in the upper plan; no runtime placement is changed here.

## Source basis

- src/main/java/org/beckenburg/LevelPlans.java (house/farm/school methods, read 2026-10-21)
- development/levels/ARCHITECTURAL_REFERENCES.md
- docs/environments/THREE_RITUAL_SITES.md
