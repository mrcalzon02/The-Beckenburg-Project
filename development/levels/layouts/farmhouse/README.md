# Hollowmere Farm

**FARMHOUSE · Beckenburg district · Established reference · rough layout v1**

Trace of current authored architecture for review. Preserve its measured footprint and floor elevations while planning the full roster.

[Measured floor plans](plan.svg) · [PNG preview](plan.png) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| r00 | Mudroom | [20.0, 22.0, 26.0, 27.0] | 0 / 3 m | Investigation and ordinary objects |
| r01 | Farm parlour | [12.0, 19.0, 20.0, 27.0] | 0 / 3 m | Investigation and ordinary objects |
| r02 | Farm kitchen | [12.0, 12.0, 20.0, 19.0] | 0 / 3 m | Investigation and ordinary objects |
| r03 | Family dining room | [20.0, 12.0, 26.0, 15.0] | 0 / 3 m | Investigation and ordinary objects |
| r04 | Central stair passage | [20.0, 15.0, 26.0, 22.0] | 0 / 3 m | Investigation and ordinary objects |
| r05 | Upper farm landing | [20.0, 12.0, 26.0, 27.0] | 3 / 3 m | Investigation and ordinary objects |
| r06 | Parents bedroom | [12.0, 19.0, 20.0, 27.0] | 3 / 3 m | Investigation and ordinary objects |
| r07 | Child bedroom | [12.0, 12.0, 20.0, 19.0] | 3 / 3 m | Investigation and ordinary objects |
| r08 | Barn hayloft | [31.0, 16.0, 43.0, 24.0] | 3 / 3 m | Investigation and ordinary objects |
| r09 | Root cellar | [12.0, 12.0, 26.0, 27.0] | -3 / 3 m | Investigation and ordinary objects |
| r10 | Barn interior | [31.0, 9.0, 43.0, 24.0] | 0 / 6 m | Investigation and ordinary objects |
| r11 | Tool shed | [31.0, 24.0, 39.0, 31.0] | 0 / 3 m | Investigation and ordinary objects |
| grounds | Rear grounds / exterior | [5, 2, 48, 31] | 0 / 12 m | Investigation and ordinary objects |
| entry | Approach / arrival | [1, 31, 48, 50.4] | 0 / 12 m | Investigation and ordinary objects |
| stage | Van service apron | [18.5, 37, 34.5, 49] | 0 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Use the existing interior loops and distinct rear exits. Staging, outdoor grounds and exact door/stair construction remain governed by current runtime coordinates.

- Loop: r00 → r01 → r02 → r03 → r04 → r00
- Loop: r00 → grounds → r02 → r01 → r00

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | r00 ↔ r01 | 1.1 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L02 | r01 ↔ r02 | 1.1 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L03 | r02 ↔ r03 | 1.1 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L04 | r03 ↔ r04 | 1.1 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L05 | r04 ↔ r00 | 1.25 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L06 | r05 ↔ r06 | 1.05 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L07 | r05 ↔ r07 | 1.05 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L08 | r00 ↔ grounds | 1.2 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L09 | r02 ↔ grounds | 1.1 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L10 | r10 ↔ grounds | 2.4 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L11 | r11 ↔ grounds | 1.3 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L12 | r04 ↔ r05 | 1.5 m | Existing stair relation; preserve actual separate runtime flights, deck cuts and guards. |
| L13 | r09 ↔ r04 | 1.5 m | Existing stair relation; preserve actual separate runtime flights, deck cuts and guards. |
| L14 | r10 ↔ r08 | 1.5 m | Existing stair relation; preserve actual separate runtime flights, deck cuts and guards. |
| L15 | grounds ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L16 | entry ↔ stage | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=26.5, Z=43, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[26.5, 0, 39.372], [24.5, 0, 40.7]]. Existing shared van on a level service apron/roadhead; no new transport mechanic.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | r10 | [37.0, 0.0, 16.5] | Barn chain containment ring |
| Cleansing | r02 | [16.0, 0.0, 15.5] | Kitchen cleansing basin |
| Absolution | r01 | [16.0, 0.0, 23.0] | Family heirloom memorial |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

House cellar/ground/upper at -3/0/+3 m; separate 6 m barn volume with rear hayloft at +3 and detached tool shed. Yard wind, barn draught and domestic heating remain separate baselines.

## Campaign use

Domestic-to-outbuilding search escalation; family records can lead to regional supply routes without deciding the encounter.

## Refinement risks / next pass

- Barn volume extends through the hayloft elevation by design; refine partial deck voids rather than adding a ceiling across the whole barn.
- Reserve a field loop and weather shelter; preserve the existing farmhouse, wash facilities and separate barn stair.
- Primary source: existing farm references in ARCHITECTURAL_REFERENCES.md; exact prop and ritual integration remains outstanding.

## Source basis

- src/main/java/org/beckenburg/LevelPlans.java (house/farm/school methods, read 2026-10-21)
- development/levels/ARCHITECTURAL_REFERENCES.md
- docs/environments/THREE_RITUAL_SITES.md
