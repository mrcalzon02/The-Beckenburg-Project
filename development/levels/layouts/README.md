# Rough map layout atlas

Planning pass, 21 October 2026. Scope: **nine initial investigation families, thirteen proposed remote sites, and Castle Beckenburg headquarters**. Open [REVIEW.html](REVIEW.html) for the complete visual map list. Each map has an editable `layout.json`, a measured multi-floor `plan.svg`, a readable brief and an OBJ volume study. JSON is the planning authority; derived files are rebuilt with `python development/levels/layouts/build_review.py`.

These are original rough spatial proposals, not playable maps, collision meshes or architectural surveys. The three established maps are traced from their current `LevelPlans.java` architecture rather than replaced. The remaining six families receive distinct replacement proposals for the shared cross-wing prototype. No runtime IDs, availability flags, stage ledger or campaign implementation changes are made.

## Read the plans

Coordinates are metres: X east/right, Z south/down, Y standing elevation. Each horizontal diagram has a north arrow, coordinate grid and scale. Colored rectangles are **space reservations**, not finished walls. Label numbers refer to the zone key. Cyan paths are same-level circulation proposals; amber dashed links change elevation. Graph routes do not establish collision-safe access. Links reserve 1.5 m clear indoors and 2 m outdoors unless stated otherwise. Stairs reserve 1.5 m width; future implementation must author flights, landings, headroom, guards and matching deck/ceiling openings.

Ritual markers reserve a clear 4.4 m square (2 m operating radius plus margin) in three distinct zones. The refuge at the expedition van uses the existing reusable module. Each placement is an explicit `VanStaging.Placement(x,z,yawRadians)` with cargo exit/ground-ramp route. Remote vans are delivered to pre-established roadheads/service aprons by logistics; distant travel is a campaign transition, not permission to drive into caves, onto boardwalks or across polar terrain. No new helicopter/boat gameplay is implied.

OBJ files contain separately named solid zone volumes and thin exterior pads, **not traversable graybox shells**. Use them for proportions and stacking only. Plans deliberately leave furniture, shell apertures, terrain support, exact stair engineering, swept movement, sound propagation, ghost fairness, networking and performance for the next pass. Suggested travel estimates use route length / 1.4 m/s plus doors/stairs during refinement; no timing is represented as measured gameplay.

## Scope and source reconciliation

- Original families: `docs/environments/ROOM_AND_ASSET_REQUIREMENTS.md`, `THREE_RITUAL_SITES.md`, approved concept boards and `development/levels/ARCHITECTURAL_REFERENCES.md`.
- Outdoor/remote sites: `docs/environments/WILDERNESS_AND_REMOTE_SITE_ROSTER.md`, WLD-01 through WLD-13.
- Campaign and HQ: `docs/CAMPAIGN_AND_ALLIED_INVESTIGATORS_DESIGN.md`, `docs/environments/CASTLE_TUTORIAL_AND_READINESS.md`, `docs/locations-environments.md`.
- Existing geometry authority: `src/main/java/org/beckenburg/LevelPlans.java`; existing shared van: `VanStaging.java` and `SHARED_VAN_MODULE.md`.
- Old concept room IDs and runtime room numbers differ, especially the early House ritual table. The traced House reserves future rite spaces by current names, with the discrepancy called out in its brief. None of these planning IDs is a protocol index. Existing runtime metadata remains authoritative until an explicit integration pass.

The reference-only room list's prose asks for ten indoor plus two exterior spaces, but several of its twelve-row tables actually have nine indoor and three exterior spaces. This atlas preserves every listed function and adds purposeful circulation/support reservations where needed; it does not claim a room-count gate was already satisfied by the old lists.

## Refinement order

The user requested breadth first: complete and checkpoint every rough layout before refinement. Afterwards retain House as the construction reference, then Farmhouse/School and the remaining initial families. Validate Blackpine → Hollow Creek → Red Mesa as the remote exemplars. All maps need review against approved concepts and primary building references before detailed architecture. Story connections remain proposals; story records never prescribe ghost identity, evidence or ritual outcome. Human and AI ghosts share lawful traversal and actions. Place investigator equipment only through loadout systems.

Run `python development/levels/layouts/validate_layouts.py` for planning integrity: roster coverage, unique IDs, finite bounds, same-level overlap, connected graph, real declared loops, route endpoints, elevations, staging transform and ritual clearances. The validator cannot prove walkability; its report explicitly lists that limitation. Workflow checkpoints and next steps are in [WORKFLOW.md](WORKFLOW.md).
