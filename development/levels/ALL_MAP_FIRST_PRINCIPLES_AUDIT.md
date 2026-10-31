# Project Beckenburg — All-Map First-Principles Architecture Audit
**Established:** 2026-10-30  
**Authority:** Java 17 runtime geometry, collision, doors, fixtures and SceneRenderer.  
**Status:** Baseline capture workflow and auditable inspection matrix implemented. Visual judgement remains human-reviewed.

## Problem statement

Some early sites are effectively named boxes. A real institution cannot be created
by changing "dining room" to "prison cell block" and keeping the same rooms,
furnishings and door logic. This pass inspects whether the building *works as the
building that it claims to be*, not merely whether the player can finish a hunt.

Do not rewrite gameplay map IDs, network room indices, evidence rules or player
spawns in response to these observations without a separate revision plan. The
fifteen investigation zones are a **protocol indexing limit**, not a requirement
to draw only twelve architectural rooms: bathrooms, offices, cells, corridors,
closets and stair landings may be separately bounded and connected inside one
gameplay zone. Child areas need real door openings and real collision.

## Map inventory and current evidence

| Location | Actual runtime authority | Preliminary architectural judgement | Priority |
| --- | --- | --- | --- |
| **Beckenburg Penitentiary** | `LevelPlans.wing("prison")` | Generic cross-wing boxes, not a credible secured cellhouse; no explicit cell construction in its runtime room generator. Separate, much better proposed 16-zone/21-route compound exists in `development/levels/blockouts/prison/layout-source.json`, but is still **rough blockout, not accepted runtime**. | **P1 — structural redesign** |
| **Ravenshade School** | `LevelPlans.school()` + `DomesticFitout.school()` | More carefully authored than other institutions, but two full washrooms are awkwardly partitioned inside **upper locker corridor zone 7**. Each has three toilet stalls and a common self-door; there are no independently named washroom gameplay zones. Audit every toilet stall, entrance and corridor approach, not just the locker-hall center. | **P1 — circulation and washroom redesign** |
| St. Dymphna Hospital / Asylum | `LevelPlans.wing("asylum")` | Generic institution template; real planned staggered-ward concept exists in blockouts, not yet integrated. Bathrooms and staff/service circulation require an architectural pass. | P2 — overhaul |
| Blackthorn Manor / Mansion | `LevelPlans.wing("mansion")` | Generic cross-wing; the planned asymmetric estate and servant/service route are not represented as independent runtime geometry. | P2 — overhaul |
| St. Alban Academy | `LevelPlans.wing("academy")` | Generic cross-wing rather than the proposed cloister/quadrangle plan. | P2 — overhaul |
| Beckenburg College | `LevelPlans.wing("college")` | Generic cross-wing instead of the planned separated campus buildings with library bridge and service link. | P2 — overhaul |
| The Briar House | `LevelPlans.house()` | Individually authored floor loop, stairs, cellar, fittings and dressing; inspect and repair local deficiencies before major layout revisions. | P3 — review |
| Hollowmere Farmhouse | `LevelPlans.farm()` | Individually authored house, barn, hayloft, root cellar and service structures. | P3 — review |
| St. Brigid Parish / Church | `LevelPlans.church()` | Individually authored nave/chancel, choir loft, belfry, crypt, parish offices; verify sightlines, stair and escape logic. | P3 — review |
| Camp Hollow Creek | `HollowCreekLevel.build()` via `HouseMap.Site.HOLLOW_CREEK` | Native, still catalog-locked prototype; staged separately from nine dispatchable sites, capture what actually loads and report failures instead of silently calling it playable. | P3 — prototype |
| Castle Beckenburg | `CastleHubMap` / `CastleRooms.ALL` | Independent headquarters layout; audit its actual 10 named castle rooms separately from investigation zone count. | P3 — headquarters |

Also include Blackpine Logging Camp in the source inventory (currently an
independently authored `BlackpineLevel.map()`, **not an unlocked expedition**);
additional WLD-03 through WLD-13 and five later source proposals have Blender
shells/layout JSONs but not active Java 17 investigation maps. A rough source
blockout is **not** equivalent to a live, traversable game site. Each should
eventually receive the same camera protocol when integrated.

## Permanent per-room audit procedure

Use `src/test/java/org/beckenburg/SiteRoomCompassAudit.java` through
`.github/workflows/map-room-first-principles-audit.yml`.

1. Enumerate **every** actual `LevelDefinition.Room`, including outside staging
   and van; record site title, map revision, bounds, area, floor, clear height.
2. Take full PSX-rendered **north, east, south, and west** views from the
   geometric centre of each room. If the centre collides with furniture, a
   wall, or a missing floor, choose the nearest genuinely traversable same-room
   point and **record the displacement**; never pretend the centre was clear.
3. Capture **child-room / bay** views from independently reserved subspaces
   and self-connected doors. These are crucial for the school corridor's
   two washrooms and for any future individual prison cell row.
4. Enumerate each doorway, width, type (door/open/stair/window), 3D position,
   connected room and floor. Record stairs separately. Check whether the
   room is topologically reachable from van staging.
5. Run the existing authoritative swept-physics route audit from the van for
   every playable map. Distinguish reachable graph edges from actually
   traversable hunter-body movement; record which room or interactive socket
   failed. Tests are for missing floors, blocked doors, invalid stair transitions
   and displaced interactive objects, *not* whether the walls look attractive.
6. Record furnishing counts and functional sockets, but NEVER use those counts
   to claim a classroom, cell block or infirmary looks correct.
7. Compare the actual four-view room essay against the source building's real
   purpose, historical/plausible building sequence, access-control layers,
   recognizable program, props, power/lighting, window logic and scene story.
   Each inspection resolves to **PASS / REVISE / REBUILD / BLOCKED** with
   specific evidence and screenshot filenames.
8. Before changes, preserve map ID, every gameplay zone index, active contracts,
   hunter/ghost routes, UV/evidence placement, ritual sockets, rescue/extraction
   thresholds and editor-module integration. Revise in source-level authoring,
   not by layering huge hardcoded overlay cubes over an incorrect layout.
9. After revision, repeat all four-view captures and physics tests from the
   *same cameras*, compare before/after and archive them in the workflow
   artifact; new major architectural additions must be added to the editor's
   source asset library and the Dev Room if they introduce new models.

The assessor must answer:

- **Readability:** What would a visitor believe this room is without reading
  the HUD name? Are there location-specific design cues?
- **Spatial logic:** Does the building have a believable public -> controlled ->
  private/service sequence? Do circulation and vertical access make sense?
- **Doorways:** Do opened doors actually lead somewhere appropriate, with
  adequate clear width and no disappearing wall behind them?
- **Purpose:** Are essential facilities present, suitably sized, furnished,
  connected and separated? Do toilets and shower cubicles have privacy and
  reasonable access?
- **Gameplay:** Does every room have a viable access/retreat path and usable
  equipment sightline; do doors, stairs, hallways and hiding places permit
  actual hunter and ghost movement?
- **Presentation:** The environment should read PSX-authentic without
  incorrect billboard/collider clipping, randomly decorated boxes,
  stretched textures or repeated ornamental clutter.

## Specific work packages

### Penitentiary: preserve the better rough layout, replace the generic runtime plan

Review `development/levels/blockouts/prison/NEXT.md`,
`layout-source.json`, `implementation.json` and `source-plan.svg`.
They already propose a sally port and intake sequence, separate guard control,
two long cell wings facing a constrained exercise yard, upper galleries,
mess/medical rear connections, isolation/chapel and a service cellar.

Do **not** simply import the shell and declare success: the packet explicitly
says individual cells, upper gallery voids, gate controls and real stair routes
are not yet complete. Required redesign gates:
- Actual individual cell volumes, doors/bars, sightlines, toilets/bunks,
  gallery guardrails and supervisory positions;
- Secure entrance vestibule, sally port, controlled access and durable gate
  state, including extraction/frenzy behavior;
- Plausible cell-to-yard and service segregation, visitation/medical access,
  fire/maintenance routes, emergency retreat;
- Meaningful multi-floor navigation and ghost hunts, not a single staircase
  connecting a stack of generic rectangular rooms.

### School: repair the washroom architecture, not only the signs

`DomesticFitout.school()` defines two 3.2m x 6.7m washrooms inside
`Upper locker corridor` (room #7), with six internal stall doors, two
entrance doors, basins and a urinal. The child geometry is currently part
of the corridor's zone instead of a separately described architectural
area. Capture it at the actual restroom centres and internal door
thresholds as well as the corridor centre.

Then decide whether the layout should instead place student bathrooms along
a realistically accessible corridor, support the gym with actual changing/
shower facilities, preserve classroom exits and circulation, and provide a
credible accessible toilet. Reuse existing approved bathroom assets if
they fit; do not change textures to compensate for an invalid floorplan.

## Reports, integration and pass criteria

- GitHub Action: `Map First-Principles Room Audit`
- Output: one folder per site, `room-matrix.csv`, `site-purpose-review.txt`,
  per-room `room-essay.txt` and four direction PNGs, optional
  `subspaces/subspace-matrix.csv`, independent 3D-traversal logs.
- Retained artifact: `beckenburg-all-room-four-direction-audit`
- All screenshots are from actual renderer geometry. No synthetic
  interpretation is accepted as a test result.
- No subjective art, balance, room meaning or inferred realism check can
  block a release. Missing compiled classes, a failed launch, unusable
  required route or broken required interaction are genuine functionality
  failures and may block acceptance in the normal gameplay suite.

**Current state:** This document contains source-grounded findings and an
automatic diagnostic capture pipeline. The entire generated image set has
not yet been manually examined, and no map has been rebuilt under this
work package. Consequently none of the above architecture judgements
should be described as final four-view visual acceptance.
