# Blackthorn Manor

**MANSION · Beckenburg district · Large / estate · rough layout v1**

An asymmetric service court beside a formal reception wing; grand stair and attic are offset from the kitchen servants route.

[Measured floor plans](plan.svg) · [PNG preview](plan.png) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Carriage-drive van apron | [20, 65, 36, 77] | 0 / 3 m | Investigation and ordinary objects |
| entry | Entrance terrace | [20, 55, 36, 65] | 0 / 3 m | Investigation and ordinary objects |
| foyer | Entrance foyer | [20, 45, 36, 55] | 0 / 3 m | Investigation and ordinary objects |
| stair | Grand stair hall | [20, 27, 28, 45] | 0 / 7.2 m | Investigation and ordinary objects |
| drawing | Drawing room | [4, 45, 20, 55] | 0 / 3 m | Investigation and ordinary objects |
| library | Library | [4, 27, 20, 45] | 0 / 3 m | Investigation and ordinary objects |
| ballroom | Ballroom | [36, 27, 54, 55] | 0 / 7.2 m | Investigation and ordinary objects |
| dining | Dining hall | [36, 15, 54, 27] | 0 / 3 m | Investigation and ordinary objects |
| scullery | Scullery / kitchen | [36, 5, 54, 15] | 0 / 3 m | Investigation and ordinary objects |
| service | Servants corridor | [28, 5, 36, 45] | 0 / 3 m | Investigation and ordinary objects |
| garden | Walled rear garden | [4, 5, 28, 27] | 0 / 3 m | Investigation and ordinary objects |
| greenhouse | Greenhouse | [4, 5, 16, 15] | 0 / 3 m | Investigation and ordinary objects |
| gallery | Upper gallery | [20, 27, 28, 55] | 3.6 / 3 m | Investigation and ordinary objects |
| suite | Master suite | [4, 27, 20, 45] | 3.6 / 3 m | Investigation and ordinary objects |
| servants | Upper servants passage | [28, 15, 36, 55] | 3.6 / 3 m | Investigation and ordinary objects |
| attic | Attic archive | [4, 27, 20, 45] | 7.2 / 3 m | Investigation and ordinary objects |
| cellar | Wine cellar | [36, 15, 54, 27] | -3 / 3 m | Investigation and ordinary objects |

## Routes and retreat

Formal loop runs through foyer, drawing room and library. Separate kitchen/service loop reaches ballroom without crossing the grand stair. Attic is a deliberate terminal search with warning at the upper servants passage; retreat via that passage and grand gallery.

- Loop: foyer → drawing → library → stair → foyer
- Loop: foyer → stair → service → scullery → dining → ballroom → foyer

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| L01 | stage ↔ entry | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L02 | entry ↔ foyer | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L03 | foyer ↔ drawing | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L04 | drawing ↔ library | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L05 | library ↔ stair | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L06 | stair ↔ foyer | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L07 | stair ↔ service | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L08 | service ↔ scullery | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L09 | scullery ↔ dining | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L10 | dining ↔ ballroom | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L11 | ballroom ↔ foyer | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L12 | library ↔ garden | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L13 | garden ↔ greenhouse | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L14 | garden ↔ service | 2 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L15 | stair ↔ gallery | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L16 | gallery ↔ suite | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L17 | gallery ↔ servants | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L18 | servants ↔ service | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L19 | servants ↔ attic | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |
| L20 | service ↔ cellar | 1.5 m | Reserve a supported route and actual boundary opening; centre line is schematic. |

## Arrival and extraction

Shared van placement: X=28, Z=71, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[28.0, 0, 67.372], [28.0, 0, 60.0]]. Existing shared van on a level service apron/roadhead; no new transport mechanic.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | attic | [12.0, 7.2, 36.0] | Chained iron containment circle |
| Cleansing | library | [12.0, 0, 36.0] | Marble purification basin |
| Absolution | ballroom | [45.0, 0, 41.0] | Family commemorative plinth |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Reception, service and upper suites have separate circuits. Tall ballroom carries sound differently from bookshelves and low attic rafters; greenhouse and garden have exposed weather baselines.

## Campaign use

Estate correspondence introduces suppliers and family patrons. Optional attic archive revisits use the same reserved geometry; no hidden story room is required for a legal rite.

## Refinement risks / next pass

- Grand stair requires a two-flight return and real gallery void; upper gallery rectangle is a planning envelope around that void.
- Draw attic headroom under roof slopes before placing the 4.4 m ritual square.
- Compare with approved mansion board; refine servants access and door apertures before replacing the cross-wing prototype.

## Source basis

- docs/environments/ROOM_AND_ASSET_REQUIREMENTS.md
- docs/environments/THREE_RITUAL_SITES.md
- development/levels/ARCHITECTURAL_REFERENCES.md
