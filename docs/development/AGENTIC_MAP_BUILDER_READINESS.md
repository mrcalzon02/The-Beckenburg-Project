# Beckenburg — Agentic Map Builder Capability and Acceptance Register

**Authority:** source inspection of `mrcalzon02/The-Beckenburg-Project` at main `3160f2723b57cb9b96d758ff5242dc8895f58d2c`, November 10, 2026. **Machine-readable companion:** [AGENTIC_MAP_BUILDER_REQUIREMENTS.json](AGENTIC_MAP_BUILDER_REQUIREMENTS.json). Recheck live source and CI before claiming any row has advanced. This is a requirements/gap register, NOT evidence of new construction.

**Staged engineering design:** [Full architectural implementation specification](AGENTIC_MAP_BUILDER_ARCHITECTURE.md), [implementation stages and gates](AGENTIC_MAP_BUILDER_IMPLEMENTATION_STAGES.md), [machine-readable stage DAG](AGENTIC_MAP_BUILDER_STAGES.json). **Build sequence:** finish task-critical editor operations → agentic scheduler/iteration → complete playable Lighthouse proof → full map rebuild. These additional documents are plans, not delivered runtime.

**User outcome:** say “build Morrow Point Lightstation” and have an authorized agent inspect the approved source, construct real geometry and gameplay systems through the **same native editor** a human uses, iterate with genuine rendered and gameplay review, save/reopen/recover, and produce a reviewable map. Multiple map tasks may execute concurrently only with independent scene ownership and shared-asset safeguards.

## Why the editor work is real, but not yet a complete map-building agent

There are three different surfaces. **Human authoring:** `DeveloperLevelEditor` supports Castle wall/floor/roof, light, door and nav tools; on missions it allows prop addition/removal/transform/rotation/scale and custom levels cloned from templates, but the code explicitly reports “Mission geometry editing requires the common level geometry adapter.” Separate human tools include `StitchedWorldEditor`, adaptive/cross-tile/layered terrain workspaces, scene packaging, `EnvironmentModuleEditor`, Model/Material Editor, animation and sound labs. Some source tools publish data but do not yet provide a unified running-game construction path. **Native agent operations:** `CastleEditorAgent` allows twelve named changes; `LevelEditorAgent` allows only `move-prop` on non-House missions and `move-prop` plus `rebuild-house` on House; `MaterialEditorAgent` allows `set-layer-opacity`. The generic headless protocol, authenticated MCP, readback, transaction/save recovery and capture transports are already strong. **Map construction orchestrator:** the current `tools/ollama_editor_pilot.py` allows one model-selected 0.125 m loose-prop adjustment on School, not spatial design or successive structural tasks.

**Proof already delivered:** Windows Editor Agent Service Acceptance [run #42861975784](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/42861975784) **SUCCESS** at the inspected source, including Castle/House MCP edit and save, installed Windows editor, mesh/material operations, and renderer captures. The bounded School Ollama run is [#42778419461](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/42778419461); it demonstrates one scene edit and fixed-camera before/after, **not** autonomous map completion. Full-map/multiplayer acceptance is separate. [Draft PR #86](https://github.com/mrcalzon02/The-Beckenburg-Project/pull/86) contains broader physical rebuild work but is unmerged; do not count its contents as selectable, runtime-authorable main-branch locations.

**Crucial gate:** `HouseMap.Site` supports HOUSE, FARMHOUSE, SCHOOL, MANSION, PRISON, ASYLUM, ACADEMY, COLLEGE, CHURCH, HOLLOW_CREEK and USER. `CASTLE` is handled separately. **WLD-14 Morrow Point Lightstation is NOT an accepted site enum or native mission editor target.** Its planned/rebuilt source packets and Blender/HCS2 work must be bound to the runtime and editor rather than substituted with a School or House scaffold.

Status legend: **PROVEN** = implemented and observed in relevant native/CI fixtures, strictly within the stated scope; **HUMAN_ONLY** = existing human/module operation, not agent-exposed in the corresponding map path; **PARTIAL** = useful implementation exists, missing unified authority or coverage; **GAP** = required operation/integration unavailable in the evaluated agent path (not necessarily absent from every repository subsystem); **GATE** = acceptance/evidence needed before permission is expanded. **P0** blocks unattended full-map authoring; **P1** is needed for confidently finished quality or mature operations. Rows describe distinct requirements; status counts are not map completion percentages.

## Full inventory (124 traceable requirements)

### A. Scene identity, map roster and persistence

| ID | Priority | Status | Required capability | Exit proof |
|---|---|---|---|---|
| A01 | P0 | PROVEN | Authoritative Castle editor scene | Castle agent alters the same singleton used by the runtime and human editor |
| A02 | P0 | PROVEN | Headless built-in mission scene | EditorAgentCli opens HouseMap.Site and reads native LevelAuthoringSession |
| A03 | P0 | GAP | Open all 28 base sites plus mortuary annex | Every roster ID, including WLD-14 and annex, opens a real editable runtime-linked scene |
| A04 | P0 | GAP | Native Morrow Point Lightstation adapter | WLD-14 source geometry, layout, HCS2, assets and runtime share one edit/save interface |
| A05 | P0 | HUMAN_ONLY | Create a template-based custom investigation level | Expose existing USER cloning with validated IDs, authoring root and catalog identity |
| A06 | P0 | GAP | Create an empty non-template level | Author an original building, terrain, rooms, docks and systems without fake template geometry |
| A07 | P0 | PARTIAL | Canonical per-site source and dependency manifest | One version-pinned manifest resolves layout, scene, terrain, GLBs, physics, nav, rooms, water, power, sound and game hooks |
| A08 | P0 | PROVEN | Stable native object IDs and expected revisions | Original and reconstructed IDs survive move, undo, redo, save and reopen; stale revisions fail |
| A09 | P0 | PARTIAL | Hierarchy with parents, layers and typed components | Native mission and game adapters round-trip real parent/child and component state without flattening or dropping it |
| A10 | P0 | PROVEN | Atomic save, recover-previous, reopen and shared history | Validate journal, competing-writer rejection, backup and exact disk readback in native scenes |
| A11 | P0 | PROVEN | Original/rebuilt House source compatibility | Legacy and rebuilt source versions round-trip with fixture protections and migrations |
| A12 | P0 | GATE | Promote reviewed output into actual game source | Saved map becomes selectable and starts the actual case with the same geometry and references |

### B. Architecture, rooms and vertical construction

| ID | Priority | Status | Required capability | Exit proof |
|---|---|---|---|---|
| B01 | P0 | PROVEN | Castle surface creation and removal | put-surface and remove-surface edit native Castle WALL/FLOOR/ROOF/VOID records |
| B02 | P0 | GAP | Mission structural creation and editing | Agent can create/update/delete walls, floors, roofs and foundations in a mission, not only move props |
| B03 | P0 | GAP | Wall thickness and physical joins | Renderer, standing collision, room sealing and sound obstruction agree at wall corners |
| B04 | P0 | GAP | Floors with depth, slab underside and support | Multi-level floors block skylight and provide correct upper/lower physics |
| B05 | P0 | GAP | Real roofs, gables, returns and eave joins | Ceiling/roof closure passes sun/weather/sound leaks from interior and exterior |
| B06 | P0 | GAP | Actual door apertures, frames and swings | Every authored leaf is matched to a physical opening and shares collision, portal and light data |
| B07 | P0 | GAP | Window apertures, sills, shutters and visibility | Real window voids and glass/blocked variants match LOS, light, sound and geometry |
| B08 | P0 | GAP | Stairs, landings, flight continuity and guards | Ascending/descending native player capsules pass flights without clipping or unprotected drops |
| B09 | P0 | GAP | Ramps, tunnels, cellar cuts and vertical connections | Cut actual blocking floor/terrain geometry; test support, body and optical passage |
| B10 | P0 | GAP | Room creation, child spaces and zone mapping | Create real rooms and subordinate spaces, with explicit map to stable gameplay zones |
| B11 | P0 | PARTIAL | Supported object transforms | Castle agent edits prop transforms; mission agent currently changes XYZ position only |
| B12 | P0 | HUMAN_ONLY | Mission props add, remove, rotate and scale | Expose already human-supported add/remove/rotate/scale as typed native agent operations |
| B13 | P1 | GAP | Prefab groups and reusable building sections | Compose, place, update and version matched rooms/stair modules without duplicate hardcoded geometry |
| B14 | P1 | HUMAN_ONLY | Grid, workplane and viewport-guided drawing | Provide machine-readable snaps, measured anchors and exact supported positions rather than screen gestures |
| B15 | P0 | GAP | Structural edit validators and supports | A real unsupported beam, floating floor or impossible clearance fails before saving |
| B16 | P0 | PARTIAL | Atomic architectural batches | Castle bounded history exists; use the same transactional boundary for entire mission shell edits |
| B17 | P1 | GAP | Measured construction anchors and scale references | Approve support/pivot/size against authoritative meshes and room dimensions |
| B18 | P0 | GAP | Path and objective-aware room layout | Human-approved spatial program becomes rooms, openings and connected routes with measurable clearances |

### C. Terrain, outdoor world, water and infrastructure

| ID | Priority | Status | Required capability | Exit proof |
|---|---|---|---|---|
| C01 | P0 | HUMAN_ONLY | Stitched 16-bit heightfield creation/import | Expose existing import/generate/open/export to agent, retaining non-overwrite safety |
| C02 | P0 | HUMAN_ONLY | Human adaptive topology and seam tools | Agent commands validate split/merge/cross-tile seams with transactional undo |
| C03 | P0 | HUMAN_ONLY | Layered terrain, material brushes and cross-tile sculpt | Typed brush edits modify saved terrain and exact native triangles |
| C04 | P0 | PARTIAL | Physical cave/subterranean topology | Combine existing cutout, cavity and ramp modules into one native editable map scene |
| C05 | P0 | GAP | Unified agent terrain sculpt/cut/foundation commands | One interface edits terrain and updates the actual scene, collision, LOS and associated structures |
| C06 | P0 | PARTIAL | Outdoor terrain runtime/physics parity for all sites | Per-site exact authored terrain—not planning heightmaps—feeds rendered world and physics |
| C07 | P1 | PARTIAL | Roads, paths, rails, retaining banks and crossings | Make editable route geometry with physical elevation, guards and vehicle/van access |
| C08 | P1 | PARTIAL | Shoreline, tide, water and weather joins | Water cannot intrude sealed rooms and actual shoreline geometry shares physics |
| C09 | P0 | GAP | Van arrival grade and terrain-linked staging | Regrade arrival, orient van, preserve doors/equipment access and extraction geometry |
| C10 | P0 | GAP | Perimeter barriers and horizon closure | Standing/crouch/ghost containment and near/far view from highest accessible floors |
| C11 | P1 | GATE | Terrain LOD, streaming and long-range visibility | Camera LOD cannot alter native walking or scope rays; measured no-crack/occlusion tests |
| C12 | P1 | GAP | Desquareify and natural grading pass | Remove arbitrary zone-shaped pads; prove authored terrain and foundation contact |
| C13 | P0 | GAP | Multi-map terrain ownership and source revisions | Independent agents cannot modify adjacent/shared terrain without locked ownership and merge checks |

### D. Assets, models, materials and furnishing

| ID | Priority | Status | Required capability | Exit proof |
|---|---|---|---|---|
| D01 | P0 | PROVEN | Live native asset catalog and fingerprints | Agent reads human palette pages and a stable catalog identity |
| D02 | P0 | PROVEN | Castle prop placement and field edits | Agent places/deletes props and edits allowed Castle fields in shared history |
| D03 | P0 | HUMAN_ONLY | Mission prop creation, deletion and duplication | Provide stable-ID equivalent of already functional human mission placement tools |
| D04 | P0 | GAP | Mission rotate/scale/replace via typed API | Agent changes yaw, scale and approved mesh while preserving collision footprint and attachments |
| D05 | P1 | HUMAN_ONLY | Actual mesh topology and GLB authoring | Bridge tested human Model Editor operations instead of duplicating meshes or generating artwork |
| D06 | P1 | PROVEN | Bounded material layer opacity edit | Existing material MCP changes selected face layer opacity, validates and saves |
| D07 | P1 | GAP | Agent-accessible mesh/UV/texture tools beyond opacity | Expose existing operations with native source preservation, real game readback and undo |
| D08 | P1 | GAP | Paint, assign and inspect site materials | Author surface material, trim, wetness, wear, decals and UV alignment on actual geometry |
| D09 | P1 | GAP | Snap, drop-to-support and mount fittings | Use source bounds and sockets so shelves/lamps/fixtures touch legitimate support without blocking circulation |
| D10 | P1 | GAP | Multi-select, scatter, replacement and variants | Bounded reproducible instancing/prefabs preserve density, original UVs and walking clearances |
| D11 | P0 | PARTIAL | Source/provenance/asset availability checks | Reject missing/misaligned runtime assets; preserve approved originals and correctly packaged GLBs |
| D12 | P1 | GAP | Asset-reference remap and replacement dependency graph | A replaced model updates existing supported references without changing unrelated fixtures |

### E. Shared gameplay systems and authored services

| ID | Priority | Status | Required capability | Exit proof |
|---|---|---|---|---|
| E01 | P0 | PROVEN | Mission prop/collider runtime parity check | Native test confirms current unsaved props and corresponding actual colliders |
| E02 | P0 | GAP | Create rooms, portals and gameplay-zone ownership | Rendered, collision, visibility, portal and acoustic topology share authored boundaries |
| E03 | P0 | GAP | Author navigable corridors and multilayer navmesh | Validate capsules, stairs, doors and dynamic obstructions for player and ghost agents |
| E04 | P0 | GAP | Door/window game interaction contracts | Socket, leaf, hinge, lock/interaction, sound and collision share one native component |
| E05 | P0 | PROVEN | Castle ceiling-lamp, source and power edits | Existing Castle agent inserts and changes lamp; native emitter/renderer test passed |
| E06 | P0 | GAP | Mission lighting, shadow and switch commands | Set lights/switches in mission rooms, verify native emitter clearance and targeted invalidation |
| E07 | P0 | GAP | Electrical generators, breakers and circuits | Site generator/distribution connects fixtures to real trip/reset and network authority |
| E08 | P0 | GAP | Hiding positions and difficulty budget | Indoor/outdoor candidates share HidingBudget.MAXIMUM and zero-cover difficulty is enforceable |
| E09 | P0 | GAP | Evidence and investigation equipment sockets | EMF, DOTS, camera, placement, observation and evidence constraints use real case systems |
| E10 | P0 | GAP | Ghost, hunter and equipment spawns | Real hostile navigation, fairness, chase routes and equipment positions are host-authoritative |
| E11 | P0 | GAP | Rituals, objective triggers and safe extraction | Three ritual modes, required objectives and van departure remain reachable and playable |
| E12 | P1 | GAP | Acoustic rooms, material absorption and portals | Real geometry and open/closed portal revisions drive sound occlusion |
| E13 | P1 | GAP | Weather, wind, water, fog and environmental time controls | Site-linked modules are editable and checked against live renderer and simulation |
| E14 | P1 | GAP | Story props, loose-object physics and scripted encounters | Authored details retain support, interaction, deterministic physics and scene references |
| E15 | P0 | GATE | Network host/guest and mission persistence acceptance | Solo and real TCP case flows share exact map revision with saving, loading and progression |
| E16 | P1 | GAP | Camera, monitoring and device mount references | Van-mounted and site cameras, observers, lighting and stationary equipment preserve owner boundaries |

### F. Observation, verification and visual review

| ID | Priority | Status | Required capability | Exit proof |
|---|---|---|---|---|
| F01 | P0 | PROVEN | Revision, hierarchy, nodes, properties and palette pages | Read bounded native state from the active editor and reject stale revisions |
| F02 | P0 | PROVEN | Native change receipts and conflict rejection | Batch edit, undo, action log, request sequences and no-duplicate retries work |
| F03 | P0 | PROVEN | Saved bytes and position proof | Compare exact current node position and native diskState; save/reopen round trip |
| F04 | P0 | PROVEN | Actual scene renderer PNG capture | Get real game/editor pixels with metadata, not a synthetic preview |
| F05 | P0 | PROVEN | Fixed-camera before/after on one School edit | Matched camera proof shows a validated one-step prop adjustment |
| F06 | P0 | PROVEN | Mission runtime-parity and open-routes diagnostics | Existing two named native tests run, with limited open-door coverage |
| F07 | P0 | PROVEN | Castle cellar, sockets and scene diagnostics | Existing named Castle tests cover focused cellar passage and wall/door sockets |
| F08 | P0 | GAP | Full 3D route sweeps and clearance matrix | Every required room and approach passes both directions, crouch/standing and locked/open doors |
| F09 | P0 | GAP | Exact structural fit and mesh-overlap scanner | Check supports, wall/ceiling bleed, hanging fixtures, UV bleed and physical prop clearance |
| F10 | P0 | GAP | Roof/terrain/water/light leak matrix | Capture and test day/night, ceiling closure, basement cuts, shoreline and sound separation |
| F11 | P1 | GAP | All-room acoustic and line-of-sight audit | Measure generated room/portal/sound graphs against actual scene and speakers |
| F12 | P0 | GAP | Actual investigation encounter and AI test scenes | Evidentiary gameplay, ghost pursuit, rituals, cameras and objectives run on edited map |
| F13 | P0 | GATE | Host/guest full-round acceptance for authored map | Multiplayer, case result, economy and next-load survive installed runtime |
| F14 | P1 | GAP | Standard camera viewpoints and structured visual regression | Capture van arrival, each floor, exterior skyline, high point and critical room at stable angles |
| F15 | P1 | GATE | Measured in-game frame timing, memory and long-view cost | Record p50/p95/p99, loading/bake cost, render mode and real hardware results |
| F16 | P0 | GAP | Defect ledger linkage with exact before/after evidence | Every failed check produces source ID, coordinates, error, severity and reproducer |
| F17 | P1 | GAP | Automated diff review and visual-meaning scoring | Distinguish camera moves from geometry and detect clipping rather than merely changed pixels |
| F18 | P1 | GATE | Human visual/design acceptance | Reviewer accepts layout, composition, readable evidence, atmosphere and safety before promotion |

### G. Agent abilities, permissions and planning

| ID | Priority | Status | Required capability | Exit proof |
|---|---|---|---|---|
| G01 | P0 | PROVEN | Opt-in authenticated loopback observation/edit scopes | Only authorized editor sessions can use the real scene; port closes with window |
| G02 | P0 | PROVEN | Typed allowlisted command discovery and schema validation | MCP advertises only real host operations and rejects unsupported types/fields |
| G03 | P0 | PROVEN | One local Ollama-selected editor transaction | Bounded School pilot selected a real prop move, saved/reopened and captured pixels |
| G04 | P0 | GAP | Structured natural-language map-build goal | Convert build-map brief into site identity, required functions, constraints and acceptance goals |
| G05 | P0 | GAP | Planner with staged S00–S11 construction tasks | Decompose physical map build into dependency-ordered measurable construction and tests |
| G06 | P0 | GAP | Semantic high-level scene commands | create-room, build-wall-run, cut-opening, make-stairs, place-service, place-prefab use trusted ops |
| G07 | P0 | GAP | Read enough world/structure to plan | Expose room volumes, terrain/mesh faces, supports, sockets, doors, nav and referenced objects |
| G08 | P0 | GAP | Observe–edit–test–critique–repair iteration loop | Agent reviews actual result and autonomously chooses the next verified edit or stops safely |
| G09 | P0 | GAP | Bounded corrective replanning on failures | Failure is classified; command replans within same scene/constraints, not arbitrary code edits |
| G10 | P0 | GAP | Objective progress scoring vs bookkeeping | Count changed geometry and real gameplay integration, not task names, reports or unchanged copies |
| G11 | P1 | GAP | Model context/ability and cost controls | Report planner model, source fingerprint, prompt budget, latency, decisions and rejection rate |
| G12 | P0 | GATE | Escalation and human approval checkpoints | Unclear provenance, scope expansion, destructive changes and final map promotion require review |

### H. Concurrent task allocation and recovery

| ID | Priority | Status | Required capability | Exit proof |
|---|---|---|---|---|
| H01 | P0 | PROVEN | Single-session scene revision and atomic edit history | One trusted scene owner applies each batch with stale revision rejection and rollback |
| H02 | P0 | PROVEN | Native per-document save writer lock | Simultaneous conflicting saves reject rather than overwrite supported native files |
| H03 | P0 | GAP | Durable map task queue and dependency DAG | Each work item has map ID, phase, inputs, owner, dependencies, artifact paths and state |
| H04 | P0 | GAP | Per-map worker lease and one-writer rule | Never permit two independently editing processes to own one scene/revision |
| H05 | P0 | GAP | Parallel independent map workers | Run two or more distinct site sessions in isolation with controlled resource budgets |
| H06 | P0 | PARTIAL | Isolated disposable map workspace | Single School pilot uses an isolated temp home; extend to each concurrent map and shared source |
| H07 | P0 | GAP | Shared asset-library version pins and cross-map locks | Model/texture changes coordinate all consuming maps and cannot silently mutate another task |
| H08 | P0 | GAP | Task-level quotas and max-iteration budgets | Cap per-task commands, tokens, runtime, inference calls, memory and asset imports |
| H09 | P0 | GAP | Persistent iteration checkpoints across runner restarts | Resume from verified scene revision, saved hashes and completed DAG nodes |
| H10 | P0 | PROVEN | Same-session request idempotency and retry receipts | Duplicate action with same sequence returns original result; wrong payload is rejected |
| H11 | P0 | GAP | Cross-session retry and uncertain-write reconciliation | After reconnect, inspect authoritative scene/receipts before performing any non-idempotent action |
| H12 | P0 | GAP | Branch and scene-file conflict reconciliation | Integrate concurrently completed work without force-push, silent overwrite or stale source commit |
| H13 | P0 | GAP | Iterative concurrent acceptance fixture | Two maps, two task types, several edit/review rounds, failures injected, exact output checked |
| H14 | P1 | GAP | Compact user-facing progress and intervention | Show tasks actually constructing, passing and blocked, plus reviewer actions and proofs |

### I. Shipping, review gates and full-map confidence

| ID | Priority | Status | Required capability | Exit proof |
|---|---|---|---|---|
| I01 | P0 | GATE | A complete Lighthouse first-principles pilot | WLD-14 tower, keeper house, covered passage, generator, cistern and coast physically built and reviewed |
| I02 | P0 | GATE | One real new mission map created entirely through agent operations | No hardcoded substitute geometry, renamed metadata or mock-only report counted |
| I03 | P0 | GATE | End-to-end source/renderer/collision/nav/rooms/acoustics parity | Each authored revision is consumed by gameplay and every required subsystem |
| I04 | P0 | GATE | Independent input and integration test before merge | Source SHA, artifacts, original state, retries and build/launch/host acceptance recorded |
| I05 | P0 | GATE | Human approval and version-safe promotion | Reviewed source is merged without force-push, old saves preserved, map appears in installed game |
| I06 | P0 | GATE | Complete 29-location stage and acceptance register | 28 bases plus annex remain distinct; counts never equate packets with finished maps |
| I07 | P1 | GATE | Repeatable result under bounded resource usage | Repeated identical plan produces equivalent native scene/validation and controlled cost |
| I08 | P0 | GATE | Fail closed on incomplete essential runtime behavior | A green construction check cannot override failed game launch, required multiplayer or updater |
| I09 | P0 | GATE | Continuous error discovery and smaller next batch | Agent audits actual build/test evidence, fixes one class of defect and resumes only validated work |

## Minimum viable agentic map-builder implementation order

1. **Native site adapter first.** Add an authenticated adapter for mission structures and WLD-14 without creating a second preview scene: actual terrain/GLB/HCS2, room identities, support, renderer, collision, nav, services and native saved document. Open and persist WLD-14 via the same game selection/loading authority. Do not silently rewrite existing House/Castle formats.
2. **Human-to-agent operation parity.** Expose native mission add/delete/rotate/scale with source protections; then implement bounded create/update/remove for foundations, walls, floors, roofs, real apertures, stairs/ramps/voids and associated room/portal links. Every operation requires IDs, schema, allowed asset references, expected revision and one undo entry. Never treat Castle's flat SURFACE alone as a finished multi-storey structure.
3. **Services + verification.** Extend typed operations for sockets, lights, power, water, acoustics, spawns, hiding, ghosts, evidence, rituals, van and gameplay. Add actual capsule, route, roof, lighting, room ownership and network tests plus stable-camera captures. No “geometry changed” without runtime parity and evidence.
4. **Bounded iterative planner.** One explicit map brief becomes a dependency DAG of measurable tasks (S00–S11 from `FIRST_PRINCIPLES_REBUILD_PLAN.md`). An agent observes the live scene, proposes allowlisted edits, applies one bounded transaction, verifies, saves checkpoint, inspects screenshot/tests, chooses the next physical edit, or reports the blocking defect. Do not count reports or renamed route files as construction.
5. **Concurrence after single-map multi-step proof.** First run three distinct operations on one School or Farmhouse scene, then **two map workers**, e.g. School furnishing and Farmhouse support/clearance, each with its own isolated native home/document and exclusive scene lease, plus one model planner shared under bounded queue. Run two or more rounds per worker; hold the shared asset library immutable/version-pinned; intentionally inject one stale revision/failed batch and prove rollback. Generate separate artifacts and a merged index, not two writers to one scene.
6. **Real Lighthouse pilot.** Morrow Point must produce physical tower flights and lantern housing, keeper-house links, coastal terrain/roads, utility sheds, generator/cistern, van staging and runtime systems, and must satisfy the site’s accessible search-space cap of 40. The finished source must be opened by the real game, traversed by real player collision in both directions, survive save/reopen/undo, and pass visual/multiplayer owner review. The architecturally correct model is not complete until all dependencies are integrated.
7. **Controlled promotion.** Commit on authorized `main` only after source reconciliation and owner review, read back the remote SHA and artifacts, do not force-push, preserve older saves and user work. Do not call a map complete from passes limited to geometry/preview. Only functional build/launch/essential gameplay and required multiplayer/updater failures block a release; design/visual fitness is an explicit map-review gate instead.

## A concrete acceptance challenge (not yet passed)

**Input:** Two user-directed jobs: (1) improve Ravenshade School circulation and furnishing with a true native room/collision edit; (2) improve Hollowmere Farm structural landing or stairs with a true native architectural edit. **Constraints:** two isolated scene owners, one serial writer per scene, no shared asset mutation, at most 3 iterative edit/checkpoint rounds per map, exact revision and source SHA, save/reopen and genuine fixed-camera PNGs each round. **Success:** at least one substantive physical edit per map, relevant native gameplay checks remain good or documented baseline failures do not increase, explicit before/after and persisted scene, bounded retries after an injected error, no unintended changes in other locations, and a structured per-map PASS/BLOCKED review. This challenge is not satisfied by two independently run 0.125m table moves.

**Follow-on:** WLD-14 is the first new-roster construction acceptance, not the first safe concurrency experiment.

## Source and evidence index

- `src/main/java/org/beckenburg/DeveloperLevelEditor.java` — human Castle and mission tool boundaries; mission architecture edit explicitly unavailable.
- `src/main/java/org/beckenburg/CastleEditorAgent.java`, `LevelEditorAgent.java`, `MaterialEditorAgent.java`, `EditorAgentCli.java` — actual machine operation allowlists and host site identity.
- `src/main/java/org/beckenburg/editor/EditorAgentService.java`, `EditorAgentProtocol.java`, `EditorObservationBridge.java`, `EditorProject.java`, `RuntimeScene.java` — authority, permissions, transactions, history, capabilities and portable schema.
- `src/main/java/org/beckenburg/editor/StitchedWorldEditor.java`, `TerrainScenePackageEditor.java`, `TerrainSeamSculptEditor.java` — human terrain and source packaging boundaries.
- `tools/editor_mcp.py`, `tools/ollama_editor_pilot.py`, `.github/workflows/editor-agent-service.yml` — current typed MCP and single-step Ollama controller.
- `docs/development/AGENTIC_EDITOR_ACCESS.md`, `EDITOR_AGENT_SERVICE.md`, `TERRAIN_WORLD_SYSTEM_PLAN.md`, `docs/editor/requirements/STAGED_INTEGRATION_PLAN.md` — existing service and G0–G12 contracts.
- `development/levels/FIRST_PRINCIPLES_REBUILD_PLAN.md`, `MAP_COMPLETION_GOAL.md`, `rebuild/wld-14/brief.md`, `development/quality/MAP_FITNESS_CHECKLIST.md` — physical map requirements and Lighthouse acceptance.

**Update protocol:** for each requirement, update `status` in JSON only after referencing an actual implementation commit, test/run and direct source review. Do not change a row from GAP to PROVEN based on a document, tool registration or test skipped by CI. All unverified statuses remain explicit.
