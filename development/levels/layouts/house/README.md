# The Briar House

**HOUSE · Beckenburg district · Established reference · rough layout v1**

Trace of current authored architecture for review. Preserve its measured footprint and floor elevations while planning the full roster.

[Measured floor plans](plan.svg) · [PNG preview](plan.png) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| r00 | Foyer | [18.0, 21.0, 24.0, 24.0] | 0 / 3 m | Investigation and ordinary objects |
| r01 | Living room | [12.0, 17.0, 18.0, 24.0] | 0 / 3 m | Investigation and ordinary objects |
| r02 | Family kitchen | [12.0, 10.0, 18.0, 17.0] | 0 / 3 m | Investigation and ordinary objects |
| r03 | Dining nook | [18.0, 10.0, 24.0, 15.0] | 0 / 3 m | Investigation and ordinary objects |
| r04 | Main hall and stair | [18.0, 15.0, 24.0, 21.0] | 0 / 3 m | Investigation and ordinary objects |
| r05 | Upper landing | [18.0, 15.0, 24.0, 24.0] | 3 / 3 m | Investigation and ordinary objects |
| r06 | Master bedroom | [12.0, 17.0, 18.0, 24.0] | 3 / 3 m | Investigation and ordinary objects |
| r07 | Child bedroom | [12.0, 10.0, 18.0, 17.0] | 3 / 3 m | Investigation and ordinary objects |
| r08 | Bathroom | [18.0, 10.0, 24.0, 15.0] | 3 / 3 m | Investigation and ordinary objects |
| r09 | Cellar stair landing | [18.0, 15.0, 24.0, 24.0] | -3 / 3 m | Investigation and ordinary objects |
| r10 | Boiler and breaker room | [18.0, 10.0, 24.0, 15.0] | -3 / 3 m | Investigation and ordinary objects |
| r11 | Cellar storage | [12.0, 10.0, 18.0, 24.0] | -3 / 3 m | Investigation and ordinary objects |
| grounds | Rear grounds / exterior | [6, 2, 30, 24] | 0 / 12 m | Investigation and ordinary objects |
| entry | Approach / arrival | [2, 24, 30, 43.4] | 0 / 12 m | Investigation and ordinary objects |
| stage | Van service apron | [14, 30, 30, 42] | 0 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Use the existing interior loops and distinct rear exits. Staging, outdoor grounds and exact door/stair construction remain governed by current runtime coordinates.

- Loop: r00 → r01 → r02 → r03 → r04 → r00
- Loop: r00 → entry → grounds → r02 → r01 → r00

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | r00 ↔ r01 | 1.15 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L02 | r01 ↔ r02 | 1.15 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L03 | r02 ↔ r03 | 1.15 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L04 | r03 ↔ r04 | 1.15 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L05 | r04 ↔ r00 | 1.4 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L06 | r01 ↔ r04 | 1.1 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L07 | r05 ↔ r06 | 1.1 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L08 | r05 ↔ r07 | 1.1 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L09 | r05 ↔ r08 | 1.05 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L10 | r06 ↔ r07 | 1.05 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L11 | r09 ↔ r10 | 1.1 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L12 | r09 ↔ r11 | 1.1 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L13 | r10 ↔ r11 | 1.05 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L14 | r00 ↔ entry | 1.2 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L15 | r02 ↔ grounds | 1.1 m | Existing door relation traced from LevelPlans; exact aperture stays in runtime source. |
| L16 | r04 ↔ r05 | 1.5 m | Existing stair relation; preserve actual separate runtime flights, deck cuts and guards. |
| L17 | r09 ↔ r04 | 1.5 m | Existing stair relation; preserve actual separate runtime flights, deck cuts and guards. |
| L18 | grounds ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L19 | entry ↔ stage | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=22, Z=36, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[22, 0, 32.372], [16.0, 0, 33.7]]. Existing shared van on a level service apron/roadhead; no new transport mechanic.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | r11 | [15.0, -3.0, 17.0] | Cellar containment circle |
| Cleansing | r02 | [15.0, 0.0, 13.5] | Kitchen purification basin |
| Absolution | r01 | [15.0, 0.0, 20.5] | Family remembrance stand |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Separate boiler circuit, insulated bedrooms, wet bathroom and exposed yard. Existing floor surfaces at -3/0/+3 m and main block X12–24/Z10–24 are retained.

## Campaign use

Nearby introductory survey, return visits to family documents, controlled retreat. Reuse the same zones for future ghost-versus-investigator contracts.

## Refinement risks / next pass

- The old House ritual table uses obsolete room numbers and a library absent from this building. The proposed three clear areas use current cellar storage, kitchen and living room; final installation choices require reconciliation before runtime changes.
- Existing runtime ritual sockets differ from these planning reservations; this pass does not relocate them.
- Rear exterior rectangle underlies the building; room ownership must prioritize interiors as in the existing implementation.
- Compare each traced floor against latest scene exports; validate exact supported van exit and both stair flights before construction changes.

## Source basis

- src/main/java/org/beckenburg/LevelPlans.java (house/farm/school methods, read 2026-10-21)
- development/levels/ARCHITECTURAL_REFERENCES.md
- docs/environments/THREE_RITUAL_SITES.md
