# First-principles rebuild of the complete map roster

Owner direction: 31 October 2026, America/Anchorage. Status: **planning delivered; rebuild execution not started by this checkpoint**. Source baseline: `ad9bb45c20c3754931637bdfa50968a35defeaf9`, synchronized from online `main`. Scope: **28 base locations, including Castle HQ, plus the existing cemetery/mortuary annex**. No additional locations are introduced.

Execution was subsequently authorized on 31 October. Follow [the live rebuild workflow](rebuild/WORKFLOW.md), [source/capture inventory](rebuild/source-baseline.json) and stage register for actual progress. The planning checkpoint status above is historical, not a claim that execution remains unstarted.

## Owner amendment - 3 November 2026: bounded accessible search

Every site must contain at most **40 accessible search spaces**. Count corridors, stairs, service rooms, exterior search sectors and staging conservatively; the stable fifteen gameplay zones do not represent the number of rooms players search. Build the maps inside that size budget. The owner explicitly excludes round-duration testing from this task; do not introduce timed-mission acceptance gates.

Preserve coherent complete buildings and useful assets as inaccessible context. Seal individual rooms or entire wings permanently, exposing only a hallway where appropriate. Historical household/clinical/educational capacity does not require every bed, WC, store or service room to be searchable. All tasks, evidence, hiding, ghost anchors/spawns, ritual sites and required routes must remain inside the connected accessible portion. Author real fixed leaves/infill, capped stairs and render/body/optical/ghost/object/interaction boundaries; ordinary shuttable or unlockable doors do not count as seals. No ghost event or power transition may open the context.

This direction supersedes any earlier implication that all architectural spaces in a rough must be playable. Follow [the active policy](rebuild/SEARCH_ENVELOPE_POLICY.json) and [all-site envelope register](rebuild/search-envelope-register.json). The current rough access annotations are construction requirements, not implemented runtime seals or accepted runtime construction.

## Owner priority override - complete Castle first

The owner directs completing Castle's full revision as the central hub and primary testing area before returning to the remaining maps. Castle now proceeds through construction, terrain, interiors/detail and actual functional/native/runtime verification as one complete location. Preserve its campaign, equipment, network, role, resident, protected tutorial and gate-exit contracts. The rest of the roster remains in scope and resumes afterward in the established stage batches. Maximum40 accessible spaces and exclusion of round-duration testing remain active. A Castle rough or implementation checkpoint is not completion.

## 1. Outcome and governing decisions

Owner urgent atmosphere amendment, 7 November2026: every level must use heavy distance fog and a skybox beyond visible geometry. Implement this through the shared Castle/investigation renderer across software and OpenGL, preserving close detail and depth/collision authority. Source-only packets inherit the same runtime policy on integration; fog does not replace terrain shaping, real physical edges or their acceptance gates. Actual implementation/evidence: `rebuild/verification/atmosphere-all-levels/`.

Owner terrain-priority amendment, 7 November 2026: settle each remaining site's
overall terrain layers, landform and elevations before further isolated path or
orange connector repairs. Review the whole site: slopes, terraces, drainage,
retaining edges and foundation heights. Reshuffle building groups, room blocks or
stair bays where needed to fit a coherent landform; preserve stable IDs, reusable
models, the40-space envelope and shared authored coordinates. Checkpoint current
work first, then execute this as a roster-wide terrain pass. Existing rough path
coordinates are review inputs, not an instruction to force terrain around them.

Owner amendment, 7 November 2026: resume breadth by stage across the remaining
roster. The House construction pilot has established the representative method;
remaining House closeout runs continue independently. Do not hold all other maps
behind further single-map refinement. Sweep blocking and traversal, then actual
structural depth/openings/roof enclosure, then the shared asset library and
supported model placement, then successive detail passes. Preserve mature Castle,
House, Blackpine and Hollow Creek work while individually auditing their gates.
Every roster pass records a concrete decision for all 29 packets; unchanged,
blocked or unverified locations are not silently counted as finished. Floors need
depth and undersides, walls need thickness, stair decks need real openings, and
closed roofs must block skylight. Native structure blocking is an intermediate
artifact, not permission to unlock a site before actual runtime integration.

Every location must make sense as a place before it is dressed as a horror level. Its terrain, construction, circulation, services, daily activities and subsequent damage must explain its appearance. A prison needs actual cells and control layers; a school needs a coherent teaching day, washrooms and specialist spaces; a home needs ordinary household relationships. A room name and an asset count establish neither architecture nor identity.

Rebuild the spatial design from its purpose. Preserve the valuable asset collection, clean masters, distinctive successful assemblies, approved concepts, lore, gameplay contracts and historical evidence. Each existing component receives an evidence-based retain/refit/replace decision. No map receives automatic acceptance because it is newer, and no useful asset is discarded merely because its original placement was poor.

The production method is **breadth by stage**: rough every location, then block every location, then establish terrain and structure, then furnish and refine in successive complete-roster passes. A stage finishes with an explicit decision for each location. Blocked locations remain visible and can leave the active batch; independent locations may proceed. Never call a batch complete while silently excluding its difficult maps.

This owner direction supersedes earlier one-map-at-a-time sequencing in `MAP_COMPLETION_GOAL.md` and older workflow paragraphs. Retain their technical gates. House remains the construction-method pilot: inspect its representative terrain/structure/stair/fixture/prop pipeline before repeating that method across the roster. That pilot does not require finishing all House story dressing before roughing other locations.

Recovery work remains the runtime integration prerequisite: REC-01/02 functional/release failures take precedence over promoting rebuilt maps. Planning, source inventories and isolated authoring may proceed; no release gate is bypassed. Architectural/art review governs rebuild acceptance and map promotion, without adding subjective artistic judgements to the installer release gate.

## 2. What the synchronized source actually establishes

Read [the existing all-map architecture audit](ALL_MAP_FIRST_PRINCIPLES_AUDIT.md), [completion scope](MAP_COMPLETION_GOAL.md), [fitness checklist](../quality/MAP_FITNESS_CHECKLIST.md) and [recovery contract](../RECOVERY_PLAN_2026-10-25.md) together.

- The source audit identifies generic cross-wing runtime plans for Prison, Asylum, Mansion, Academy and College. Distinct proposed replacements already exist; integrate and improve them after physical review.
- School has authored specialist volumes but a specific corridor/washroom redesign obligation. House, Farmhouse and Church have more individual architecture, requiring the same fresh audit rather than assumed replacement of every component.
- Castle has its own HQ authority and rebuilt content. Preserve active stations, equipment, campaign and exit behavior throughout renewal.
- Blackpine and Hollow Creek contain substantial authored work. Hollow Creek native support, water and runtime bridging have separate evidence and remaining mission gates. Preserve their sources and verify their current adapters.
- Other remote packets include reservations, rough native shells and terrain proposals. These are useful design inputs, not proof of runtime completion.
- Existing registers differ in scope and age. The edge/sky catalog lists 28 base entries and one expansion; some older atlas text still says 23. The rebuild roster explicitly covers the full 29 packets.

These are source-grounded starting points, not a completed visual inspection. The existing four-direction room audit explicitly says its full image set has not been manually reviewed. This plan does not convert those preliminary findings into final verdicts.

## 3. Production artifacts and source authority

Use [the per-location work packages](FIRST_PRINCIPLES_SITE_PACKAGES.md) and [the stage register](rebuild/stage-register.json). The register starts every execution gate at `NOT_AUDITED`; prior work stays linked as evidence to evaluate. This is a rebuild tracking register, not a replacement for release, equipment or recovery ledgers.

For each folder in `rebuild/<site>/`, execution will produce:

| Record | Required contents |
| --- | --- |
| `brief.md` | Site purpose, era, occupants/capacity, working day, construction history, functional program, identity cues, narrative premise, omissions and constraints |
| `baseline.md` | Exact source SHA, runtime authority, editable scene/hash, four-view captures, defects, retained assemblies and proposed replacements |
| `space-program.csv` | Each architectural space and use, dimensions/clear height, occupancy, public/service/security class, daylight/privacy, adjacencies, doors, services and gameplay-zone mapping |
| `routes.csv` | Arrival, public, staff, service, emergency and hunt paths; supported elevation transitions, measured clearances and actual traversal observations |
| `asset-use.csv` | Actual asset ID/hash, source/license, native bounds/pivot, UV/materials, clean/worn variant, support/mount contract, collider/interaction type and selected placements |
| `construction.md` | Foundation, roof/bearing logic, floor depths, stairs/voids, utilities, circuits, acoustics, boundaries and terrain interfaces |
| `story.md` | Normal use, interruption, later occupation/maintenance, supported trace clusters, lore IDs and optional encounter states |
| `acceptance.md` | Per-stage decisions, measured results, exact revision, captures, defects, limitations and next action |

These filenames are deliverable specifications, not claims that those records already exist. Create them during the relevant pass. Avoid duplicate coordinate files: link to actual authoring nodes and generate reports from their transforms.

The integrated game source remains authoritative. Existing investigation architecture uses `LevelPlans.java`; Castle, Blackpine and Hollow Creek have their own actual builders/adapters. A migration may use the shared versioned scene contracts, but geometry, rendering, physical support, colliders, zones, openings, electrical fixtures, acoustics, spawn points and van staging must consume the same authored coordinates. Never maintain an attractive Blender display model alongside different runtime geometry.

Preserve site IDs and network/save contracts. The 15 investigation indices constrain gameplay indexing, not the number of architectural rooms. Author real bounded child rooms, cells, toilets and landings with physical doorways, then map them deliberately to gameplay zones. If a design genuinely requires a protocol change, record and implement a versioned migration separately; do not silently merge unlike spaces to fit an array.

## 4. Staged production passes

### S00 — Synchronize, preserve and establish the baseline

**Work:** Fetch remote refs; inspect worktree/branch state; preserve local edits and active concurrent work; start from current `main`. Record exact workflow state for full multiplayer, renderer and installer, plus recovery blockers. Pin source and asset hashes before altering a location. Capture actual runtime rooms and child spaces through `SiteRoomCompassAudit`, including four directions, doorway views and stairs both ways. Source-only sites receive labeled native captures and an explicit missing-runtime status.

**Deliverables:** Full roster, baseline evidence, dependency map, defect entries, retained/replaced component list and recovery snapshot. Identify whether archive assets referenced at external paths are actually available here.

**Exit:** Every packet is accounted for; missing files and runtime adapters are explicit. No map is rated from a label or stale screenshot. Current checkpoint completes repository synchronization and the plan/register only; baseline scene inspection remains outstanding.

### S01 — Define the real place before drawing its rooms

**Work:** Write each location's identity brief and space program. Explain who used it, what work happened, how people/goods/waste moved, where people slept/washed/ate, and how the place changed over time. Separate essential functions from optional picturesque features. Review approved concept boards and suitable primary plans/photos; record inspected drawing identities, dimensions and limitations in `ARCHITECTURAL_REFERENCES.md` during execution.

Describe the player's arrival and three or four defining spatial experiences. Establish original construction and later additions; asymmetry should follow use, terrain or history. Define what a visitor should infer without HUD labels. Give each room a one-sentence purpose, adjacency requirements and a furnishing rationale. Preserve the existing narrative without using props to predetermine ghost identity or evidence outcomes.

**Deliverables:** 29 briefs; architectural reference log; space and adjacency programs; location-specific retain/refit/replace decisions. Reuse old plans only where they satisfy the new brief.

**Exit:** Essential facilities and public/private/service relationships make sense. Institutional maps have distinct organizing principles. No map advances with only a room-name list.

### S02 — Rough out every site and its circulation

**Work:** Reserve terrain envelope, roadhead/arrival, actual canonical van and cargo/ramp area, water, buildable footprints, floor elevations and skyline. Draw bubble diagrams, measured plans and sections. Mark public/service/security sequences, route loops, deliberate dead ends, three ritual reservations and combined hiding candidates. Rough structures as masses with representative doors and stairs, not decoration.

Use current atlas route widths and ritual reservations as initial design inputs, then check the actual capsule and systems. Record predicted travel using route length, doors and elevation; label predictions separately from measured movement. Place mandatory objectives where lawful hunter/ghost access is possible. Give intentional risky branches a gameplay explanation and a way to avoid softlocking a case.

**Deliverables:** Complete-roster rough plans/sections, massing views, route and function diagrams, initial risk list, arrival views with van.

**Exit:** Every required function has a plausible location and connection; tall spaces and stacked floors are intelligible in section. Diagrams establish intent, not traversability. Finish all 29 decisions before general detailed blockout.

### S03 — Build empty, physically traversable blockouts

**Work:** Build full-scale walkable ground, shell masses, actual openings, ramps/flights, landings, upper deck voids and temporary safety guards. Use continuous supporting surfaces and real floor depth. Integrate standing elevations, physical doors and zone ownership early. Place placeholder colliders for the largest necessary machines/furniture, reserving their operating and maintenance clearances.

Run swept movement from the van through every required room and socket approach, both directions on stairs, standing/crouched and with open/closed doors as appropriate. Test threshold bodies and headroom, not only route centre lines. Check multi-floor ownership and representative host/guest movement now. Verify actual arrival, return and extraction geometry; round-duration testing is outside this task.

**Deliverables:** Playable empty blockouts, route logs, stair sections, before/after captures and unresolved exceptions.

**Exit:** Required movement works on actual collision geometry. A connected graph or native flythrough is insufficient. House proves the representative construction method before its helpers are propagated. Apply the same method across all remaining sites before detailed shell production.

### S04 — Engineer terrain, boundaries and site infrastructure

**Additional owner requirement, 31 October:** Basement/cavity construction must use the editor's persistent terrain hole-punching topology. An architectural stair/deck void alone is insufficient: remove the actual world-ground faces above the cavity, consume the same mesh in world collision/optical queries, preserve valid floor/wall/stair support and demonstrate real above/below views and traversal. Check the broad backdrop/ground sheet as well as room decks. Do not replace this with transparency or a renderer-only exception.

**Work:** Starting from accepted rough circulation, shape actual slopes, drainage, rock/shore edges, terraces and excavations. Fit foundations, retaining structures, cuts, crossings and guard edges to the terrain. Apply `DESQUAREIFY_GATE.md`: preserve rectangular constructed slabs where purposeful, remove zone-shaped ground patches and arbitrary planning pads. Grade walking routes using actual mesh support and full capsule clearance.

Author near/middle/far composition and closed physical site boundaries using each edge/sky packet. Inspect from highest accessible floors and shorelines. Keep water out of sealed interiors/holds; a water plane, fog or painted horizon cannot contain players. Place remote power sources, service boards and cable-route reservations. Any terrain change updates foundations, paths, van elevation and all affected anchors together.

**Deliverables:** Accepted terrain/support package, water interface, boundaries/horizon views, foundation/contact report and remote utility plan.

**Exit:** No floating pads, buried routes, unsupported crossings or escapable perimeter. Runtime/native adapters agree on the same revision. Ground and route work is accepted before final structures are promoted.

### S05 — Assemble structurally coherent buildings

**Work:** Replace temporary masses with location-specific foundations, walls, frames, floors, roofs, stairs, galleries and envelope details. Show plausible support/load paths and construction sequence at game scale; this is fictional architecture, not certified engineering. Coordinate bays, window rhythm, floor heights and building additions with actual room functions.

Build deck undersides/edges, wall thickness, real window apertures and sills, roof returns, thresholds, handrails and necessary service penetrations. Doors open into credible rooms and have usable swings. Stairs cut both upper floors and obstructing ceilings; guards derive from the same void. Large/tall volumes receive appropriate structure rather than enlarged domestic partitions. Special kits such as cells, ship frames or industrial conveyors retain their actual identity.

**Deliverables:** Complete empty structures, material regions, editable structural kits, exact collision/room/portal bindings and structural review captures.

**Exit:** Each building reads correctly before loose dressing. Surface-integrity, support, movement and light-separation checks pass for the changed geometry. Institutional child spaces and real exterior connections exist physically.

### S06 — Install services and gameplay systems

**Work:** Install room-appropriate lighting, supported switches, breaker/distribution, heating/plumbing/ventilation and visible necessary service routes. Bind fixtures to finished wall/ceiling supports. Define acoustic rooms/portals/material behavior from actual partitions. Keep generator-running state separate from breaker/circuit state and independent oil/fire/battery lights.

Bind spawn/rescue/extraction, van stations, equipment approaches, evidence surfaces, photo sightlines, ritual spaces, door states, human/AI ghost routes and permitted hiding to the shared geometry. Enforce `HidingBudget.MAXIMUM` across indoor/outdoor candidates; zero-cover settings yield zero. The mortuary annex shares the parent mission budget. Decorative foliage adds none. Castle follows its HQ station/tutorial contracts rather than investigation-hunt quotas.

**Deliverables:** Circuit and acoustic diagrams, supported fixture placements, working interaction/socket map and representative host/guest checks.

**Exit:** A generator or switch prop is not accepted as an electrical system. Mandatory interactions are physically reachable with valid aim/occlusion. Case-critical services and blackout recovery work before furniture detail.

### S07 — Rough functional prop fill using the existing index

**Work:** Resolve required functions against `development/assets/local-library-manifest.json`, `assets/libraries/index.json`, family manifests, actual runtime GLBs and each model-requirements packet. Their entries are candidates, not fit certification. The local inventory contains unresolved dimensions/license fields and external archive paths; inspect actual available sources rather than trusting filenames.

Populate major equipment and furniture in work sequences: receive -> store -> prepare -> use -> clear away. Establish desks with task views, beds with access, kitchens with working zones, cell fittings, classroom furniture with usable aisles, and machinery with credible feed/output and maintenance space. Select exact native bounds/pivots/UVs, preserve uniform scale and record support planes. Place major objects first, then a small representative cluster per function. Record static, pickup, hinged, destructible or event behavior explicitly.

Create new assets only for demonstrated gaps after library search and fit inspection. Reuse a boiler as a boiler, not as a stretched substitute for a different industrial plant. Keep clean masters and licensed source boundaries intact. Do not place investigator loadout equipment as fake room decoration.

**Deliverables:** Asset-to-function assignments, actual supported furniture placements, shared gap backlog and restrained rough dressing across all maps.

**Exit:** Essential room activities are recognizable from space and major objects. No random prop quota, unsupported clutter, blocked door/ritual/hiding space, or accidental physical trap. Repeat route and interaction checks after furnishing.

### S08 — Refine construction and material identity

**Work:** First complete frames, skirtings, sills, joints, fasteners, brackets, conduits and purposeful built-in fittings. Then refine material scale, normals, UV seams, roughness and transitions under actual room lighting. Prioritize silhouette and construction readability at player distance. Check ceiling/floor corners, window returns and adjacent light states; do not bury bad joins under clutter or darkness.

Build shared kits by actual function/material family: domestic fittings, institutional sanitation, secure cells, timber camp structures, dock equipment, industrial service hardware and fixtures. Batch asset production after site dimensions are known, then inspect fit independently in each location. Shared fixes invalidate affected site evidence and reopen relevant gates.

**Deliverables:** Refined architectural kits and furnished in-room comparisons; updated per-site fit records.

**Exit:** Source-library visual quality is the minimum; no stretched textures, repeated metal striping, conflicting coplanar faces or wrong mounting planes. Each map keeps its own material and construction language.

### S09 — Weather, wear and environmental storytelling

**Work:** Write a causal wear map: rain exposure, water path, prevailing weather, ground contact, hand traffic, repairs, heat, salt, neglect and current occupation. Apply large exposure gradients, then construction-joint staining, then localized touch/wear. Dirt belongs where it accumulates; corroded machinery and damp timber need different treatment. Preserve clean masters and derive separate worn variants; use the existing clean/worn validation workflow where applicable.

For each space, show normal use -> interruption -> later traces. Add supported personal clusters, maintenance records, discarded tools, rationing, repairs, calendars and lore where the story requires them. Vary density deliberately; empty working space is useful. Keep traces legible under lights on/off and real player illumination. Loose objects require real host-owned physics/interaction/reset behavior, not a decorative mesh renamed as movable.

**Deliverables:** Per-room story records, wear rationale, clean/worn comparisons, supported cluster captures and loose-object checks.

**Exit:** Detail explains the location's history, does not randomly fill floors, and does not assign ghost identity or auto-confirm evidence. Movement, readability and functional interactions still pass after detail.

### S10 — Atmosphere and authored encounters

**Work:** Tune light hierarchy, exterior silhouettes, weather/water, ambient machinery/nature and room acoustics while preserving useful navigation and equipment signals. Build on existing sound/scene adapters; the editor requirement chain is a dependency reference, not proof that proposed audio/topology features exist. Necessary map authoring can continue through proven tools while advanced editor work remains pending.

Add optional map-specific encounters only after core function/detail acceptance. Specify host state, trigger, visibility/occlusion, observers, timing, abort/reset/cooldown, source models/audio and evidence-negative behavior. Use the existing final-story/Easter-egg workflow. Verify quiet/empty negatives as well as the event; do not let decorative scares fake actual evidence or block extraction.

**Deliverables:** Same-camera day/night/blackout/portable-light reviews, acoustic observations, encounter state tests and actual in-game captures.

**Exit:** Atmosphere supports the site's identity and readable play. Sound does not reveal private roles or replace manual observation. Optional encounters survive host/guest/reconnect and reset correctly.

### S11 — Accept, integrate and publish each rebuilt location

**Work:** Run affected architecture/physics/utility/surface tests, actual solo case progression, real TCP host/guest integration, hunter/AI/human-ghost paths, evidence/camera behavior, all required ritual protocols, hiding settings, rescue/extraction and progression round-trip. Measure real Windows GPU performance at representative and worst-case viewpoints under controlled settings; include traversal/hunts, not just stationary screenshots.

Define performance budgets from the current accepted engine/hardware baseline: frame-time percentiles, stutter, memory, visible instances, draw/geometry/light work, load times and audio voices. Record target hardware/settings before judging. Do not invent a fixed polygon/FPS budget without measurement. Compare new and baseline scenes at the same views and states; preserve current performance work and authoritative renderer.

Export the same runtime authority to editable scenes and validate source/export hashes. Refresh the development gallery with actual PNGs and labeled native/runtime/model views. Run its normal build and `--check`; verify deployed images before claiming gallery delivery. Fetch/reconcile concurrent changes, make scoped commits, push without force and verify the remote commit.

**Exit:** Per-map evidence packet names exact SHA, tests/results, manual inspection, physical hardware limits and remaining defects. Unlock/promotion follows existing availability contracts only after required gates. Full-roster completion requires every packet's applicable gates accepted or a user-approved scope change; blocked or unreviewed entries remain outstanding.

## 5. Batch execution order and dependencies

| Batch | Locations | Why this order within each stage |
| --- | --- | --- |
| A: domestic method | House -> Farmhouse -> Mansion | Prove human scale, services, stairs, supported furniture and distinct public/service circulation |
| B: institutions | School -> Prison -> Asylum -> Academy -> College -> Church | Address documented school/prison issues; distinguish cellhouse, ward, cloister, campus and worship programs |
| C: HQ continuity | Castle | Review active arrival/stations/library/store/tutorial and vertical routes with its own HQ acceptance |
| D: authored outdoor references | Blackpine -> Hollow Creek | Retain successful construction; audit source/runtime parity, power, terrain and remaining mission gates |
| E: terrain-led sites | Red Mesa -> Lookout -> Mine -> Cemetery/annex -> Monastery -> Marsh | Resolve caves/grades/vertical links, daylight and dry/wet boundaries before fine construction |
| F: working waterfront and industry | Wharf -> Cannery -> Lightstation -> MV Saint Elara -> Research station -> Dam -> Railway | Coordinate process flow, service access, water, large structures and special movement risks |
| G: remaining established packets | Motor Lodge -> Battery -> Cinder-6 | Distinct courtyard, defensive and shaft/ring circulation; reuse only inspected kits |

This ordering is for work queues, not seven independent production pipelines. Complete S01 across A–G, then S02 across A–G, and so on. Use one representative House construction pilot at the start of S03–S07; stage its demonstration before scaling that method. Keep terrain-led outliers visible rather than forcing them into domestic construction assumptions.

Essential dependencies: S00 -> S01 -> S02 -> S03 -> S04 -> S05 -> S06 -> S07 -> S08 -> S09 -> S10 -> S11. Functional reservations begin in S02, blocking object bounds in S03, and utility routing in S04: early reservation prevents late demolition, while acceptance waits for the full implementation. Recheck movement throughout. Editor enhancements proceed only when a concrete map blocker justifies them; do not wait for completion of the entire editor/engine roadmap.

If a map fails a gate, record the failing evidence, smallest correction, affected downstream gates and next action. Reopen derived exports/captures whose source hashes changed. Terrain/elevation changes reopen structure, systems, props and routes; moving a table reopens all supported items. A reusable helper passing once never marks every consuming map accepted.

## 6. Evidence, review and progress discipline

Each stage record uses `NOT_AUDITED`, `IN_PROGRESS`, `BLOCKED`, `REVISE`, `ACCEPTED` or `NOT_APPLICABLE`, with source revision, evidence paths, defects and next action. `NOT_APPLICABLE` needs a reason; Castle's hunt tests are one legitimate example. Existing sources are separately recorded as prior evidence. Do not equate file existence with acceptance.

Review every architectural room/child space, not only gameplay-zone centres. Pair consistent N/E/S/W camera captures with entry/task views and stair/vertical sections. Add lights-on/off, adjoining-circuit, breaker, torch, UV and relevant camera states as stages mature. Review highest accessible exterior views and actual van arrival. Label missing captures explicitly.

Use actual available tests, selecting by changed contract: `AuthoredLevelPhysicsTest`, `AuthoredLevelReachabilityTest`, `AuthoredSitePhysicsTest`, `AuthoredUtilitiesTest`, `LevelSurfaceIntegrityTest`, `LevelEnclosureTest`, `AuthoredLevelNetworkTest`, `AuthoredLevelRitualCaseTest`, `HidingBudgetTest`, site-specific native tests and `SiteRoomCompassAudit`. Verify current test invocation through repository build/workflow definitions before execution. Add meaningful regression coverage for newly discovered functional defects; visual judgement still requires inspection.

Every checkpoint reports: packets advanced, exact stage decisions, defects found/fixed, evidence revision, source/remote SHA and next unfinished step. Show progress as accepted packets per stage, with blocked/revise counts and quality observations. Avoid an invented overall percentage that hides unfinished runtime work.

Calibrate schedule after the House pilot and first institution blockout. Record actual effort by stage and site complexity, separate shared-kit work from map placement, and re-estimate the remaining roster using observed throughput. Commit in reviewable site or kit milestones; do not keep a 29-map uncommitted batch. Batch sequencing does not mean postponing recoverable checkpoints until the entire pass finishes.

## 7. First execution queue

1. Refresh current `main`, inspect recovery workflows and preserve all concurrent work. The planning branch is a reviewable checkpoint, not authorization to overwrite `main`.
2. Run/retrieve baseline room audits for live maps and native views for source-only packets. Start detailed structural inspection with House, School and Prison; retain all three distinct findings.
3. Complete S01 briefs and function programs across all 29 packets, using the site packages below as the starting questions. Inspect references during that pass; this planning checkpoint performs no new architectural research.
4. Reconcile rough plans across the complete roster, especially generic runtime replacements, and finish S02 with arrival, sections, routes, service access and purpose explained.
5. Prove House's representative full-scale circulation/structure/fixture/prop method; correct it before reuse. Then complete the S03 empty-blockout pass across the roster.
6. Continue S04–S11 in stage batches with the acceptance and publication discipline above. Retain map locks and old recoverable versions until replacements pass their actual contracts.

The first visible success is a complete set of credible, measured places with proven circulation. Fine detail earns its place on those foundations in subsequent passes.

## Planning checkpoint validation

Local checks passed for exact roster equality with the existing edge/sky catalog, 29 unique matching site packages, twelve stages per packet (348 decisions), existing prior-source paths, relative links in both new documents, strict UTF-8 decoding and `git diff --check`. All execution stages remain `NOT_AUDITED` with no claimed evidence or accepted revision. No runtime code or geometry changed, so no game compile, new render/gallery capture or gameplay test was run for this documentation-only delivery. Current recovery workflow observations are recorded separately in the recovery checkpoint log.


Owner amendment13November2026: retain the House ordering—predictable real-world structure and supported landings/openings first, then traversability validation. Reject clearance-driven tread flares and matching arbitrary ceiling cuts. Clean all doorway openings across the full roster to consistent sill/head datums and appropriate door-type widths, with real jambs/lintels and wall thickness; navigation studies guide construction and never become physical ribbons or swept-cut architecture.
