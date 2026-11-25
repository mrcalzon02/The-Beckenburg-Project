# Agentic Map Builder — staged implementation and acceptance runbook

**Design-only plan.** This document and the [machine-readable stage register](AGENTIC_MAP_BUILDER_STAGES.json) specify future engineering, not an implemented map-construction agent. Current native editor/MCP/School pilot proofs are preserved independently. Last audited runtime baseline: cc3ea3a4e6e0f07a2c194909d8067e41a07cb323.

The stage IDs **AMB-00–AMB-15** are a new engineering work breakdown, **not replacements** for G0–G12 (engine integration) or S00–S11 (per-map construction). Stage status is NOT_STARTED until a new implementation patch and exact-source tests pass. A stage may consume previously working modules without claiming the entire stage is accepted.

## Execution protocol

1. Inspect current GitHub main, branch ownership, AGENTS.md, recovery REC-01/02, required installer/multiplayer acceptance and all concurrent work.
2. Resolve dependencies below. Only implement one bounded stage slice at a time; carry smaller source changes than a massive editor rewrite.
3. Preserve one authoritative runtime scene/document and one native transaction history. Every agent command must be typed, source-scoped, revision-bound, reversible and checkable.
4. Execute **new negative and positive native tests** for the changed operation, then verify existing House/Castle/installed GUI tests and actual game use. Record results, source SHA, evidence files and outstanding defects.
5. On failed essential build/launch/multiplayer/updater, prioritize recovery and retain the failing map in review; do not change release gates. On diagnostic style failures, record the defect without blocking unrelated releases.
6. For each accepted stage, add a stage record containing commit, relevant Windows CI run, tests, native scene document hash, actual capture paths, user-review decision when applicable and next step. Do not flip state in this register without those facts.
7. No new independent GitHub workflows merely to monitor individual tasks; preserve the existing single editor acceptance job. Run concurrent scene workers inside a controlled executor only when AMB-11 is proven.
8. The source-of-truth map register remains development/levels/rebuild/stage-register.json; design-stage evidence cannot mark a specific site’s S-stage accepted.

## Dependency graph

~~~mermaid
flowchart TD
 A00[AMB-00 Preflight] --> A01[AMB-01 Broker]
 A01 --> A02[AMB-02 Mission props]
 A02 --> A03[AMB-03 Structural geometry]
 A03 --> A04[AMB-04 Rooms and stairs]
 A01 --> A05[AMB-05 Terrain]
 A03 --> A05
 A04 --> A06[AMB-06 Game systems]
 A05 --> A06
 A02 --> A07[AMB-07 Assets]
 A03 --> A07
 A04 --> A08[AMB-08 Verification]
 A03 --> A08
 A02 --> A09[AMB-09 Iterative agent]
 A08 --> A09
 A09 --> A10[AMB-10 Checkpoints]
 A10 --> A11[AMB-11 Concurrency]
 A08 --> A11
 A03 --> A12[AMB-12 WLD-14 adapter]
 A04 --> A12
 A05 --> A12
 A08 --> A12
 A06 --> A13[AMB-13 Lighthouse pilot]
 A07 --> A13
 A09 --> A13
 A11 --> A13
 A12 --> A13
 A13 --> A14[AMB-14 29-site rollout]
 A13 --> A15[AMB-15 Promotion]
 A14 --> A15
~~~

The graph allows independent engineering where dependencies permit, but **does not authorize concurrent writes to a single native scene**, nor unfinished sites entering game rotation. The Lighthouse adapter begins only after actual mission geometry/terrain are available, and its full pilot begins after game services and iterative testing are real.

## Acceptance ledger fields

Each stage completion record must include stage ID, status, exact main SHA, authoritative affected files, native scene ID, source document fingerprint(s), test names/results, GitHub run URL, actual renderer captures, error receipts, rollback proof, reviewer identity/decision (where needed) and remaining blockers. Use NOT_STARTED / IMPLEMENTING / BLOCKED / READY_FOR_REVIEW / ACCEPTED only with evidence. A successful prerequisite (such as the current Editor Agent Service Acceptance) never upgrades unrelated stages automatically.

## Detailed stages

### AMB-00 — Authority, scope and six-check preflight

**Current stage state:** NOT_STARTED. **Priority:** P0. **Dependencies:** none. **124-register requirements:** G01. **Existing G alignment:** G0, G1. **Map S alignment:** S00, S01.

**Deliverable.** Establish verified source ownership, recovery priority, task scope and preflight before edits.

**Implementation tasks**
1. Reconcile main SHA and the required recovery, installer, full multiplayer and native editor results; identify concurrent owners.
2. Record per-site canonical source paths, concept approvals, all 28 base locations and annex; distinguish Castle headquarters and playable missions.
3. Define six-check preflight: owner/scope, source hash, backup, dependencies, reversible operation, verification/review.
4. Do not infer installed/hardware acceptance from a package build or a documentation checkpoint.

**Implementation surfaces:** AGENTS.md; development/RECOVERY_PLAN_2026-10-25.md; development/levels/rebuild/stage-register.json.

**Acceptance tests (not claimed passed by this document)**
- Inventory at exact SHA; no content overwritten.
- Recovery REC-01/02 blockers remain prominent.
- Baseline fixture and owner-map register read back from repository.

**Non-negotiable controls:** Never force-push or retag. No new CI workflows for design-only changes..

**Completion proof:** new tested native implementation, full relevant positive/negative checks, exact source and commit readback, diagnostic/artifact evidence and applicable owner review. Until then AMB-00 stays NOT_STARTED.


### AMB-01 — Runtime scene capability broker

**Current stage state:** NOT_STARTED. **Priority:** P0. **Dependencies:** AMB-00. **124-register requirements:** A01, A02, A07, A08, A09, A10, F01, F02, G02, G03, H01, H02. **Existing G alignment:** G1, G10, G11. **Map S alignment:** S00, S03.

**Deliverable.** Unify typed operation discovery, revisions and scene authority without adding a second scene.

**Implementation tasks**
1. Define SceneDescriptor with site ID, scene ID, document codec/hash, source commit, runtime modules, allowed operations and unsupported reasons.
2. Route all planner commands through the same native host/service used by Swing and the CLI; never let model code call a free-form Runnable.
3. Make coordinate units/angles, stable IDs, scene revision, side effects and dependency invalidations explicit.
4. Keep serialized owner-thread commits and shared history; adapt per-site capability sets rather than advertising universal controls.
5. Constrain MCP/CLI authorization, target site, document root and revision; include immutable source fingerprints.

**Implementation surfaces:** src/main/java/org/beckenburg/editor/EditorAgentService.java; src/main/java/org/beckenburg/editor/RuntimeScene.java; src/main/java/org/beckenburg/EditorAgentCli.java; tools/editor_mcp.py.

**Acceptance tests (not claimed passed by this document)**
- Castle and House human/headless/MCP equal-state readback.
- Wrong owner, unsupported operation, stale revision and unauthorized scope fail without side effects.
- Existing save/reopen and old document fixtures pass on Windows.

**Non-negotiable controls:** No parallel scene storage. Use existing single-writer save lock and request idempotency..

**Completion proof:** new tested native implementation, full relevant positive/negative checks, exact source and commit readback, diagnostic/artifact evidence and applicable owner review. Until then AMB-01 stays NOT_STARTED.


### AMB-02 — Mission prop parity and safe scene cloning

**Current stage state:** NOT_STARTED. **Priority:** P0. **Dependencies:** AMB-01. **124-register requirements:** A05, A06, B11, B12, D01, D02, D03, D04, D05. **Existing G alignment:** G1, G11. **Map S alignment:** S03, S07.

**Deliverable.** Expose existing human mission prop actions to the agent using stable IDs and protected attachments.

**Implementation tasks**
1. Implement place, duplicate, remove, transform XYZ/yaw/scale and approved mesh replace with native room/support/bounds validation.
2. Reuse human editor native mutation and transaction/history instead of an agent-specific props list.
3. Retain source-bound plumbing, fixtures and dependent controls; infer no support from a mesh name alone.
4. Support an explicitly scoped USER level cloned from an approved template without claiming empty-site construction.
5. Publish typed schema and actual GUI/headless/MCP parity tests with invalid-asset and protected-fixture negatives.

**Implementation surfaces:** src/main/java/org/beckenburg/LevelEditorAgent.java; src/main/java/org/beckenburg/LevelAuthoringSession.java; src/main/java/org/beckenburg/HouseAuthoringAttachments.java; src/main/java/org/beckenburg/DeveloperLevelEditor.java.

**Acceptance tests (not claimed passed by this document)**
- Mission objects can be placed, rotated, duplicated and deleted, saved/reopened/undone without changing others.
- Attached kitchen/service geometry remains protected even under planner input.
- Legacy House/USER BSCN compatibility retained.

**Non-negotiable controls:** No standalone edit of source-bound assemblies. Site clones do not confer WLD-14 identity..

**Completion proof:** new tested native implementation, full relevant positive/negative checks, exact source and commit readback, diagnostic/artifact evidence and applicable owner review. Until then AMB-02 stays NOT_STARTED.


### AMB-03 — Mission foundations, walls, floors, roofs and openings

**Current stage state:** NOT_STARTED. **Priority:** P0. **Dependencies:** AMB-02. **124-register requirements:** B01, B02, B03, B04, B05, B06, B07, B14, B15, B16, B17. **Existing G alignment:** G1, G2, G4, G11. **Map S alignment:** S03, S05.

**Deliverable.** Implement real structural authoring on a mission, not only Castle surfaces and loose objects.

**Implementation tasks**
1. Add typed foundation, thick-wall-run, joined-wall, floor-slab with underside, roof-shell and cut-aperture operations.
2. Support actual frame/sill/doorway/void geometry; no zero-thickness facade, alpha-only holes or route-only metadata.
3. Bind every geometry patch to the same rendered/collision/room/nav/portal/acoustic revisions, with scoped invalidation.
4. Use native IDs, metric coordinates, measured bounds, material/pivot references and capped batch complexity.
5. Implement atomic rollback of structural edits and correct both-direction raster/body checks.

**Implementation surfaces:** src/main/java/org/beckenburg/LevelEditorAgent.java; src/main/java/org/beckenburg/LevelPlans.java; src/main/java/org/beckenburg/editor/RuntimeScene.java.

**Acceptance tests (not claimed passed by this document)**
- Construct an enclosed new room in School or Farmhouse with physical aperture and real floor support.
- Undo/save/reopen yields exactly matching geometry, render and standing collision.
- Negative unsupported roof/opening/foundation edit leaves document identical.

**Non-negotiable controls:** Do not copy Castle surface JSON and call it mission construction. Reject unsupported mesh and floating support..

**Completion proof:** new tested native implementation, full relevant positive/negative checks, exact source and commit readback, diagnostic/artifact evidence and applicable owner review. Until then AMB-03 stays NOT_STARTED.


### AMB-04 — Multi-storey rooms, portals and circulation

**Current stage state:** NOT_STARTED. **Priority:** P0. **Dependencies:** AMB-03. **124-register requirements:** B08, B09, B10, B18, E02, E03, E04. **Existing G alignment:** G4, G5, G11. **Map S alignment:** S02, S03, S05.

**Deliverable.** Complete actual floor-to-floor access, named spaces and shared circulation/room ownership.

**Implementation tasks**
1. Author continuous stairs/landings/ramps, upper-floor cutouts, rail/guards and supported cellar transitions.
2. Model subordinate rooms and gameplay zones with stable ownership and meaningful privacy/service routes.
3. Derive nav, acoustic portals, optical visibility and door collision from the same physical room boundaries.
4. Run native swept capsule routes up/down and between all required rooms at standing and crouched height.
5. Expose stable portal/room IDs and diagnostic coordinates for blocked points.

**Implementation surfaces:** src/main/java/org/beckenburg/LevelPlans.java; src/main/java/org/beckenburg/editor/AdaptiveNavMeshEditor.java; src/main/java/org/beckenburg/editor/AdaptiveNavigationRegions.java.

**Acceptance tests (not claimed passed by this document)**
- A native two-level site can be traversed in both directions without penetrating floor slabs.
- Doors open into real authored apertures and actual room identities agree in graphics/physics/nav.
- Invalid headroom/landing or orphan portal fails visibly.

**Non-negotiable controls:** No disconnected graph-only stairs. No arbitrary generic room coordinates substituted for physical geometry..

**Completion proof:** new tested native implementation, full relevant positive/negative checks, exact source and commit readback, diagnostic/artifact evidence and applicable owner review. Until then AMB-04 stays NOT_STARTED.


### AMB-05 — Stitched terrain, caves, roads, water and boundaries

**Current stage state:** NOT_STARTED. **Priority:** P0. **Dependencies:** AMB-01, AMB-03. **124-register requirements:** C01, C02, C03, C04, C05, C06, C07, C08, C09, C10, C11, C12. **Existing G alignment:** G4, G10, G11. **Map S alignment:** S03, S04.

**Deliverable.** Bind existing human terrain modules to native agent commands and outdoor runtime/physics.

**Implementation tasks**
1. Expose safe height brush, seam, adaptive topology, terrain layer, cutout/cave and existing terrain-scene package operations through site-owned commands.
2. Ensure cross-tile joins are atomic and edited terrain support/visibility/body uses exact published triangle geometry.
3. Author graded foundations, roads, rails/bridges, shoreline/water interface, retaining slopes and permanent perimeters.
4. Maintain matching van parking elevation and extraction clearances after regrading.
5. Preserve source PNG/BLEND/GLB ownership and prevent generated preview assets from replacing native source.
6. Test real underpass/floor hole with body and optical passes across tile edges.

**Implementation surfaces:** src/main/java/org/beckenburg/editor/StitchedWorldEditor.java; src/main/java/org/beckenburg/editor/TerrainScenePackageEditor.java; src/main/java/org/beckenburg/editor/AdaptiveTerrainTopology.java; src/main/java/org/beckenburg/editor/TerrainSeamSculptEditor.java.

**Acceptance tests (not claimed passed by this document)**
- 3x3 native world sculpt/seam roundtrip with rollback and no cracks.
- One outdoor site renders, collides and rays against the same modified terrain revision.
- Van access, upper views and perimeter/horizon checks survive regrade.

**Non-negotiable controls:** No display-only 2D terrain claimed as playable. Water and terrain cannot leak into enclosed interiors..

**Completion proof:** new tested native implementation, full relevant positive/negative checks, exact source and commit readback, diagnostic/artifact evidence and applicable owner review. Until then AMB-05 stays NOT_STARTED.


### AMB-06 — Electrical, environmental and ghost-gameplay services

**Current stage state:** NOT_STARTED. **Priority:** P0. **Dependencies:** AMB-04, AMB-05. **124-register requirements:** E05, E06, E07, E08, E09, E10, E11, E12, E13, E14, E15, E16. **Existing G alignment:** G5, G6, G8, G9, G11. **Map S alignment:** S06, S10.

**Deliverable.** Expose native site services as typed components tied to geometry and real host gameplay.

**Implementation tasks**
1. Implement room-scoped light/switch/mount commands, remote generator and breaker/distribution ownership.
2. Attach actual ghost/hunter spawns, evidence/ritual locations, hiding sockets, cameras and canonical van equipment locations.
3. Bind audio volumes, openings/occlusion, weather/water and room circuits to physical revision owners.
4. Validate HidingBudget.MAXIMUM, accessible search envelope, AI/pursuit routes and ordinary site service functions.
5. Enforce host-authoritative gameplay, no independent physics AI/audio replica or fabricated encounter pass.

**Implementation surfaces:** src/main/java/org/beckenburg/FixtureLighting.java; src/main/java/org/beckenburg/editor/EnvironmentModules.java; src/main/java/org/beckenburg/LevelPlans.java.

**Acceptance tests (not claimed passed by this document)**
- A native game room light and switch change affects exact in-game rendered lighting and real source state.
- Generator trip/reset, evidence and ritual placement pass native and multiplayer checks.
- Sealed geometry does not leak sound, weather/water or uncontrolled lighting after edit.

**Non-negotiable controls:** No auto-solved clues or replacement of existing ghost AI. Do not invalidate full-world lighting for a local door change..

**Completion proof:** new tested native implementation, full relevant positive/negative checks, exact source and commit readback, diagnostic/artifact evidence and applicable owner review. Until then AMB-06 stays NOT_STARTED.


### AMB-07 — Asset/prefab, model and material operations

**Current stage state:** NOT_STARTED. **Priority:** P1. **Dependencies:** AMB-02, AMB-03. **124-register requirements:** B13, D06, D07, D08, D09, D10, D11, D12. **Existing G alignment:** G2, G3, G7, G11. **Map S alignment:** S07, S08, S09.

**Deliverable.** Reuse approved model, texture, fixture and prefab workspaces through measured, reversible operations.

**Implementation tasks**
1. Expose approved palette and source manifest with license, exact bounds/pivot, support and material identifiers.
2. Add safe prefab/part assembly, replace-in-place, surface mount, drop-to-support and measured placement macros.
3. Bridge selected already-tested topology, UV and material operations to native agent API; do not give arbitrary Blender/script execution.
4. Apply deterministic bounded scatter and visual wear without altering shared original assets.
5. Use immutable version-pinned shared-library references while concurrent map tasks execute.

**Implementation surfaces:** src/main/java/org/beckenburg/MaterialEditorAgent.java; src/main/java/org/beckenburg/AssetImportDialog.java; src/main/java/org/beckenburg/editor/EditorTopologyProject.java.

**Acceptance tests (not claimed passed by this document)**
- An approved GLB replacement retains geometry/material/UV/collision and survives native save/reopen.
- A model agent cannot mutate the shared binary during concurrent scene edits.
- Unsupported rig/UV transforms and missing original assets refuse safely.

**Non-negotiable controls:** No pasted generic collision box for all models. No missing-source or unsafe-license material publication..

**Completion proof:** new tested native implementation, full relevant positive/negative checks, exact source and commit readback, diagnostic/artifact evidence and applicable owner review. Until then AMB-07 stays NOT_STARTED.


### AMB-08 — Runtime verification, renderer proofs and defect evidence

**Current stage state:** NOT_STARTED. **Priority:** P0. **Dependencies:** AMB-03, AMB-04. **124-register requirements:** E01, F03, F04, F05, F06, F07, F08, F09, F10, F11, F12, F13, F14, F15, F16, F17, F18. **Existing G alignment:** G1, G4, G11, G12. **Map S alignment:** S03, S04, S05, S06, S11.

**Deliverable.** Extend genuine native inspection to full geometry, gameplay and reproducible fixed-camera evidence.

**Implementation tasks**
1. Build targeted checks for support, exact wall/roof/portal containment, clipped objects, 3D capsule sweeps and source/collider parity.
2. Record stable camera, render mode, light/time/weather state, actual before/after PNG and changed geometry IDs.
3. Attach exact failed code, object/room ID, scene revision, coordinates and source hashes to defect ledger.
4. Distinguish visual/surface advisory issues from required build/launch/host/network/update release failures.
5. Verify scene across real renderer, physics, nav, rooms, acoustics and services before promotion.

**Implementation surfaces:** src/main/java/org/beckenburg/editor/EditorAgentService.java; src/main/java/org/beckenburg/SceneRenderer.java; tools/verify_editor_agent_cli.py.

**Acceptance tests (not claimed passed by this document)**
- A deliberately blocked stair, open roof or embedded lamp fails a real check; repair makes it pass.
- Camera-mismatched or identical PNG alone never certifies a structural change.
- Evidence bundle has source SHA, native document digest, receipts, PNGs, tests and unresolved issues.

**Non-negotiable controls:** Never weaken an assertion to turn CI green. Do not claim physical GPU/FPS or installed acceptance from headless software render..

**Completion proof:** new tested native implementation, full relevant positive/negative checks, exact source and commit readback, diagnostic/artifact evidence and applicable owner review. Until then AMB-08 stays NOT_STARTED.


### AMB-09 — Bounded multi-round agentic map iteration

**Current stage state:** NOT_STARTED. **Priority:** P0. **Dependencies:** AMB-02, AMB-08. **124-register requirements:** G04, G05, G06, G07, G08, G09. **Existing G alignment:** G10, G11. **Map S alignment:** S02, S03, S05, S07, S08.

**Deliverable.** Turn the single-choice Ollama test into staged observe–plan–edit–test–review–replan.

**Implementation tasks**
1. Compile typed site goals and S00–S11 work packages into dependencies and read-only context, filtered to host capabilities.
2. Let Ollama propose only listed native commands; trusted broker validates state, model output, ownership, budget and dependencies.
3. Apply bounded atomic batches, rerun actual tests, capture matching camera views and inspect physical diffs.
4. Checkpoint accepted rounds; replan on diagnosed failure, and stop on no progress or human approval requirement.
5. Track substantive physical work separately from tiny cosmetic edits, renamed metadata and screenshots.

**Implementation surfaces:** tools/ollama_editor_pilot.py; docs/development/OLLAMA_EDITOR_AGENT_MANIFEST.md; docs/development/AGENTIC_MAP_BUILDER_REQUIREMENTS.json.

**Acceptance tests (not claimed passed by this document)**
- One site completes at least three meaningful dependent edits and post-edit tests through the real editor.
- Injected unsafe proposal, stale revision, and failed multi-command batch never persist partial edits.
- A planner cannot execute shell commands or write GitHub directly.

**Non-negotiable controls:** Model is not granted arbitrary source-code execution. One revision owner per iterative scene..

**Completion proof:** new tested native implementation, full relevant positive/negative checks, exact source and commit readback, diagnostic/artifact evidence and applicable owner review. Until then AMB-09 stays NOT_STARTED.


### AMB-10 — Durable task journal, retry and checkpoint restoration

**Current stage state:** NOT_STARTED. **Priority:** P0. **Dependencies:** AMB-01, AMB-09. **124-register requirements:** G10, H03, H04, H08, H09, H10, H11. **Existing G alignment:** G1, G10, G11. **Map S alignment:** S00, S03, S11.

**Deliverable.** Preserve atomic task progress across crashes, runner restarts, unknown responses and resource interruptions.

**Implementation tasks**
1. Create versioned TaskEnvelope, dependency DAG, work item states, budgets and manifest-identity checks.
2. Persist task records, lease/snapshot metadata, edit sequence, request digest, source state and result hash on durable runner storage plus immutable artifact checkpoints.
3. On retry reconcile native saved scene and sequence receipts; never blindly replay uncertain non-idempotent mutations.
4. Bound model calls, retries, infer context size, worker memory, total rounds and execution time.
5. Prove interruption at before-commit, after-commit-before-response, after-save and during image upload.

**Implementation surfaces:** src/main/java/org/beckenburg/editor/EditorRequestJournal.java; src/main/java/org/beckenburg/editor/EditorDocumentStore.java; tools/ollama_editor_pilot.py.

**Acceptance tests (not claimed passed by this document)**
- All interrupted tasks resume without duplicate geometry or leaked scene ownership.
- Bounded retries surface unresolvable conflicts, not infinite background loops.
- Reviewable report links exact scene document, native transactions and evidence hashes.

**Non-negotiable controls:** Scheduled tasks are never silently disabled. Runner artifacts cannot stand in for a verified canonical source revision..

**Completion proof:** new tested native implementation, full relevant positive/negative checks, exact source and commit readback, diagnostic/artifact evidence and applicable owner review. Until then AMB-10 stays NOT_STARTED.


### AMB-11 — Concurrent site task allocation, worker leases and merge safety

**Current stage state:** NOT_STARTED. **Priority:** P0. **Dependencies:** AMB-08, AMB-10. **124-register requirements:** C13, G11, G12, H05, H06, H07, H12, H13, H14. **Existing G alignment:** G10, G11, G12. **Map S alignment:** S03, S05, S07.

**Deliverable.** Run at least two independent maps concurrently without concurrent writers to one native scene.

**Implementation tasks**
1. Allocate one owner process and fenced lease per scene; forbid stale lease writes even when native revision appears equal.
2. Schedule two isolated native site workers and a shared, bounded Ollama planning queue.
3. Pin shared asset catalog and source revision; shared model edits are dependency tasks with separate ownership.
4. Use durable task DAG with block/retry/review states, per-site receipts, workspace paths and artefact manifests.
5. Inject a stale writer, worker timeout and shared-asset conflict; only the affected task fails.
6. Reconcile upstream Git commits before any reviewed promotion; no automated force-push.

**Implementation surfaces:** tools/ollama_editor_pilot.py; src/main/java/org/beckenburg/editor/EditorAgentService.java; development/levels/rebuild/stage-register.json.

**Acceptance tests (not claimed passed by this document)**
- School and Farmhouse complete independently checkable multi-round real edits with distinct saved documents and captures.
- Same-scene dual writer and expired lease cannot commit.
- Failure/rollback of one task leaves the other map and shared library unchanged.

**Non-negotiable controls:** Never launch multiple workflows per task. Do not run multiple CPU/GPU-heavy planners simultaneously on one constrained runner by default..

**Completion proof:** new tested native implementation, full relevant positive/negative checks, exact source and commit readback, diagnostic/artifact evidence and applicable owner review. Until then AMB-11 stays NOT_STARTED.


### AMB-12 — First-class WLD-14/outdoor site adapter

**Current stage state:** NOT_STARTED. **Priority:** P0. **Dependencies:** AMB-03, AMB-04, AMB-05, AMB-08. **124-register requirements:** A03, A04. **Existing G alignment:** G4, G8, G9, G11. **Map S alignment:** S00, S02, S03, S04.

**Deliverable.** Make Morrow Point Lightstation a native, runtime-linked editable site rather than a renamed template.

**Implementation tasks**
1. Identify existing WLD-14 editable Blender/GLB/HCS2/terrain source and its exact runtime loader/owner; preserve all originals.
2. Bind the actual site geometry and required services to a native editor adapter, saved scene and catalog identity.
3. Expose safe structural/terrain changes through existing CLI/MCP host, with real render/collider/room consistency.
4. Prove native save/reopen and the same changed location in the installed game, leaving any unintegrated systems catalog-locked.
5. Do not copy unchanged School or generic wing geometry and relabel it WLD-14.

**Implementation surfaces:** src/main/java/org/beckenburg/HouseMap.java; src/main/java/org/beckenburg/EditorAgentCli.java; development/levels/layouts/wld-14/layout.json; development/levels/rebuild/wld-14/brief.md.

**Acceptance tests (not claimed passed by this document)**
- Morrow Point opens by its own stable site ID with real authored model, terrain and native revision.
- One bounded structural edit produces a saved native scene and matching runtime render/body change.
- Game-site availability remains gated until whole-mission acceptance.

**Non-negotiable controls:** No fake placeholder mission unlock. Existing map packets are planning until physically consumed by game..

**Completion proof:** new tested native implementation, full relevant positive/negative checks, exact source and commit readback, diagnostic/artifact evidence and applicable owner review. Until then AMB-12 stays NOT_STARTED.


### AMB-13 — Lighthouse complete construction and visual/gameplay pilot

**Current stage state:** NOT_STARTED. **Priority:** P0. **Dependencies:** AMB-06, AMB-07, AMB-09, AMB-11, AMB-12. **124-register requirements:** I01, I02, I03. **Existing G alignment:** G9, G10, G11, G12. **Map S alignment:** S00, S01, S02, S03, S04, S05, S06, S07, S08, S09, S10, S11.

**Deliverable.** Supervise one start-to-finish true Lighthouse build across all site construction/acceptance stages.

**Implementation tasks**
1. Construct tower/lantern, keeper house/covered passage, generator/oil house, cistern, real coastal grading and bounded search zones.
2. Build genuine stairs, floors/voids, windows, access, room/portal, services, van/extraction and correct lighting/weather.
3. Iteratively furnish/age/author encounters using approved assets; preserve normal former function and approved story.
4. Run day/dusk/night fixed camera review, full body/optical/nav/hiding/ritual checks, installed case gameplay and TCP.
5. Publish exact source hashes, accepted construction diffs, rejected alternatives and human approval; retain max 40 accessible search spaces.

**Implementation surfaces:** development/levels/rebuild/wld-14; development/levels/FIRST_PRINCIPLES_REBUILD_PLAN.md; development/levels/MAP_COMPLETION_GOAL.md.

**Acceptance tests (not claimed passed by this document)**
- Real site structure and subsystems are committed through native editor and survive reopen.
- Game mission starts and can progress through equipment, evidence, ritual and extraction under actual host/guest.
- Human confirms legible layout/material and full-map status; before approval keep locked.

**Non-negotiable controls:** Not complete from design packets or 2D route study. No promotional release from artwork-only acceptance..

**Completion proof:** new tested native implementation, full relevant positive/negative checks, exact source and commit readback, diagnostic/artifact evidence and applicable owner review. Until then AMB-13 stays NOT_STARTED.


### AMB-14 — Full-roster staged production and metrics

**Current stage state:** NOT_STARTED. **Priority:** P1. **Dependencies:** AMB-13. **124-register requirements:** I06, I09. **Existing G alignment:** G10, G11, G12. **Map S alignment:** S00, S01, S02, S03, S04, S05, S06, S07, S08, S09, S10, S11.

**Deliverable.** Apply the proven pipeline to the existing 28 base plus annex, retaining blocked sites visibly.

**Implementation tasks**
1. Sync per-site S00–S11 status and evidence; Castle-first owner priority is preserved while other planning can proceed.
2. Allocate independent construction batches by map readiness, prerequisites, native system support and resource budgets.
3. Only count changed physical geometry and genuinely playable integrations; keep graphics/asset refinements separate.
4. Capture actual per-site tested source, scene, gallery evidence, unresolved defects and review records.
5. Measure validated edits/hour, retries, token/model budget, collisions, worker overlap and map stage throughput.

**Implementation surfaces:** development/levels/rebuild/stage-register.json; development/levels/FIRST_PRINCIPLES_SITE_PACKAGES.md.

**Acceptance tests (not claimed passed by this document)**
- All 29 sites either show evidence-backed stage acceptance or explicit blockers; none silently omitted.
- Per-site review links actual render/collision/nav/service progress, not reports or filenames.
- Concurrent tasks do not clobber shared source models or native saved documents.

**Non-negotiable controls:** No automatic completion percentage from packet count. No unrelated workflow proliferation..

**Completion proof:** new tested native implementation, full relevant positive/negative checks, exact source and commit readback, diagnostic/artifact evidence and applicable owner review. Until then AMB-14 stays NOT_STARTED.


### AMB-15 — Controlled promotion, installed acceptance and operational handoff

**Current stage state:** NOT_STARTED. **Priority:** P0. **Dependencies:** AMB-13, AMB-14. **124-register requirements:** A11, A12, I04, I05, I07, I08. **Existing G alignment:** G10, G11, G12. **Map S alignment:** S11.

**Deliverable.** Close only genuinely accepted maps and promote under existing recovery/release gates.

**Implementation tasks**
1. Review source diff, exact native document/scene/multiplayer/renderer/acoustics/AI parity and license/source provenance.
2. Run required game build, launch, updater, essential gameplay and TCP multiplayer tests; distinguish release blockers from advisory design checks.
3. Require human visual approval before map promotion and preserve existing saves/paths with migrations.
4. Push only reconciled authorized main changes, record commit SHA and artifact/gallery readback; never force-push or retag.
5. Provide supervisor stop/retry/intervention controls, failure receipts and cost/reproducibility dashboards.

**Implementation surfaces:** development/RECOVERY_PLAN_2026-10-25.md; development/quality/MAP_FITNESS_CHECKLIST.md; development/levels/MAP_COMPLETION_GOAL.md.

**Acceptance tests (not claimed passed by this document)**
- Accepted site survives installed launch, real case, network, save/reopen and next launch as appropriate.
- User-review result and exact source/artifact evidence accompany remote commit.
- No unfinished map is presented as complete and no failed release gate bypassed.

**Non-negotiable controls:** No release claimed from a green editor test alone. Do not silently overwrite installed or player-owned documents..

**Completion proof:** new tested native implementation, full relevant positive/negative checks, exact source and commit readback, diagnostic/artifact evidence and applicable owner review. Until then AMB-15 stays NOT_STARTED.

## Proposed minimal pilot sequence (before autonomous Lighthouse work)

**Pilot 1: native mission parity.** On a scratch School BSCN, perform real add, rotate/scale and remove with scene IDs retained, then undo/save/reopen. Protected kitchen fittings on House must reject invalid standalone edits. A GUI-only edit without a typed service call is not sufficient.

**Pilot 2: actual physical construction.** On School or Farmhouse, create one thick wall segment, connected floor support and genuine doorway; open/closed traversal and renderer/body collision must agree. Review one matched camera comparison. Route-coordinate edits alone do not count.

**Pilot 3: iterative goal.** Give one worker a site-based 3-round task: inspect the room and plan, place a supported loose fitting, fix its clearance, verify and checkpoint. A failed or no-progress round must be classified and not counted.

**Pilot 4: concurrent goal.** School and Farmhouse run in separate isolated native homes, each 2–3 real edit rounds, one fenced scene owner each; one planner queue. Force a mid-batch rollback in one worker and prove the other’s saved document, asset catalog and receipts are unchanged.

**Pilot 5: Morrow Point.** Bind true WLD-14 source, then produce a physical Lighthouse tower/keeper-house/covered passage and actual coastal terrain with game collision and services; cap accessible spaces at 40. Continue S00–S11 and retain human review, actual installed gameplay and required multiplayer gates.

## Failure semantics / stage handoff

- **Design or policy ambiguity:** BLOCKED/REVIEW_REQUIRED; ask for a targeted owner decision, not an invented geometry solution.
- **Native validation refusal:** preserve old state; report code, ID, geometry coordinates, source revision, failed constraint and next bounded fix.
- **Stale revision or write conflict:** re-observe authoritative scene, reconcile receipt and existing save; do not replay on old state.
- **Runner/inference interruption:** bounded retry, source-hash verification, exact checkpoint; preserve tasks and never mark success without disk/readback proof.
- **Unrelated CI diagnostics:** record separately; do not turn off workflows or conceal results.
- **Cross-site asset/source mutation:** hold all dependent tasks and require reviewed integration of the asset change.
- **Release gating:** obey REC-01/02 and owner policy: essential build/launch/gameplay and required multiplayer/updater failures block release; art, balance and visual review gate map quality without inventing release blockers.

**Maintenance:** update the stage JSON and this runbook together, with linked implementation/evidence. The 124-item readiness register remains the detailed capability inventory and may only be advanced after direct source and test verification.
