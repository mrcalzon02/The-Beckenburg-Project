# Beckenburg — Agentic Map Construction Architecture and Implementation Specification

**Version:** design v1, November 10, 2026. **Status:** ENGINEERING DESIGN ONLY. This document is not implementation evidence. **Source inspection baseline:** main commit \`cc3ea3a4e6e0f07a2c194909d8067e41a07cb323\`. Reconcile current main before writing code.

**Companions:** [124-item native-capability register](AGENTIC_MAP_BUILDER_READINESS.md), [machine-readable capabilities](AGENTIC_MAP_BUILDER_REQUIREMENTS.json), [staged implementation runbook](AGENTIC_MAP_BUILDER_IMPLEMENTATION_STAGES.md), [machine-readable stage DAG](AGENTIC_MAP_BUILDER_STAGES.json), [existing Editor Agent Service](EDITOR_AGENT_SERVICE.md), [G0–G12](../editor/requirements/STAGED_INTEGRATION_PLAN.md), [S00–S11 first-principles rebuild](../../development/levels/FIRST_PRINCIPLES_REBUILD_PLAN.md), [29-site stage register](../../development/levels/rebuild/stage-register.json).

## 0. Priority decision and exit conditions

**The order is non-negotiable:**

1. **Complete the task-critical editor upgrades.** Existing human geometry, terrain, model, service and gameplay tools must be exposed through authoritative native scene adapters. The editor must be able to create, change, inspect, save and playtest substantive mission architecture without brittle source-file substitutions. Do **not** require every optional G0–G12 engine feature first.
2. **Complete the agentic task-editor/orchestration upgrades.** Add typed semantic operations, site work plans, dependency scheduling, bounded Ollama decisions, multi-round checks, durable checkpoints, scene-owned writer leases, cancellation and reviewer handoff.
3. **Demonstrate one genuine construction method.** Validate multiple dependent edits on a playable representative location, then two isolated simultaneous site tasks, then bind and build Morrow Point Lightstation (WLD-14) through the same actual editor/game route.
4. **Rebuild the remaining locations.** Use the proven system on the 28 base sites plus cemetery/mortuary annex, preserving Castle-first priority, approved asset masters, site lore, source geometry and the S00–S11 breadth-first stage contract.

**Gate E0 — editor production-ready for agents:** mission scenes can create and revise physical foundations, walls, real apertures, thick floors/roofs, stairs and room/portal boundaries; terrain, support, model, fixtures and game systems have typed native operations, explicit unsupported capabilities and shared render/collider/nav/room/acoustics/service identities. Essential persistence and old documents work. Gate E0 is not closed merely because Castle supports \`put-surface\`, the standalone kit compiles, or the human terrain editor shows a preview.

**Gate E1 — agent orchestration ready:** a bounded agent performs three dependent scene-edit/test/render/save cycles, handles one injected failure, and can execute two independent map tasks without conflicting source or loss of progress. No green result from isolated table nudges is enough.

**Gate E2 — construction-method proven:** one **real** Lighthouse or equivalently reviewed representative site has substantive physical structures, services, full native runtime/collision/nav/room integration, real case tests, actual screenshots, saved/editable source and human review. Distinguish the editor pilot from final site acceptance.

**Gate E3 — production rollout:** the per-site S00–S11 ledger is the source of truth; map workers may proceed in bounded batches with exact evidence. No reports, renamed route files, unchanged copied geometry, standalone screenshots or source packets are counted as constructed/playable rooms.

REC-01/02 essential gameplay and release-integrity repairs take priority when broken. Visual, layout and balance findings are explicit **map quality review** gates, not automatically release-blocking CI conditions.

## 1. The user interaction model

The desired user request is compact: “Build Morrow Point Lightstation and the keeper house to the approved first-principles brief; include the generator shed, cistern, access, terrain and hiding/ritual services. Keep the search envelope at or below 40 accessible spaces. Show real before/after and review all failures.” Agents should not need the user to repeat file paths or explain how to navigate Swing controls.

The controller resolves site ID, approved design/lore, stage, native document(s), source hash, asset constraints, actual current scene and available operations. It decomposes the request into a dependency graph of testable **physical edits**, asks a small planner model for a bounded next edit from real operation schemas, applies it through the authoritative host, runs native diagnostics, captures rendered evidence, checkpoints, critiques, and proceeds or stops with an exact blocker. An agent may suggest structural edits it cannot yet perform, but must report \`UNSUPPORTED_CAPABILITY\` rather than generate unauthorized or synthetic scene files.

**Human review occurs** at irreversible source replacement, questionable scale/support, structural redesign with major site consequences, promotion to shipped playable mission, and final visual/story approval. Routine validated edits within the approved bounding box and command budget are safe for supervised auto-apply.

## 2. One authoritative scene, multiple host adapters

Existing authoritative components:

- \`EditorAgentService\`, \`EditorAgentProtocol\`, \`EditorHistory\`, \`EditorDocumentStore\`, \`EditorObservationBridge\` and \`EditorRequestJournal\` provide a tested transactional service, process-bound headless protocol, restricted authenticated loopback/MCP, saved document integrity, receipts and rollback.
- \`CastleEditorAgent\` has twelve native operation kinds: \`rebuild-castle\`, \`set-spawn\`, \`put-surface\`, \`remove-surface\`, \`place-prop\`, \`set-door\`, \`set-nav-cell\`, \`place-ceiling-lamp\`, \`set-prop-field\`, \`set-lamp-field\`, \`remove-prop\`, and \`remove-lamp\`.
- \`LevelEditorAgent\` currently has \`move-prop\` on supported mission sites and \`rebuild-house\` additionally on House. The **human mission editor** already has add/delete/rotate/scale prop actions, but does not have the Castle structural adapter. Its existing mission viewport explicitly refuses Castle-only wall/floor/roof commands.
- \`MaterialEditorAgent\` supports a narrow native \`set-layer-opacity\` operation; more extensive mesh/UV/texture tools belong to the human Model and Material editors.
- \`StitchedWorldEditor\`, \`TerrainSeamSculptEditor\`, \`TerrainScenePackageEditor\` and related adaptive/layer modules have useful human functionality, but not one authorized mission-writing broker with full in-game physics/scene parity.
- \`HouseMap.Site\` is finite and does not currently expose WLD-14 as a headless mission target. The Lighthouse has layout/rebuild/Blender/HCS2 authoring sources, not an accepted editable/runtime scene identity.

The architecture MUST be a thin capability broker over these actual owners, not a replacement scene, independent collision solver or direct automation of arbitrary UI mouse coordinates.

\`\`\`text
user brief / approved site constraints
         |
goal compiler (S-stage + site ID + immutable source refs)
         |
dependency DAG + approval policy + budgets
         |
   [model planner / Ollama]   [read-only validator / critic]
          |                         |
      typed proposals <-------------+
          |
native operation capability broker (schema + scope + epoch)
          |
single scene owner / EditorAgentService / EditorHistory
          |
+---------+------------+------------+-----------+
| Castle host           | Mission host           |
| terrain/world adapter | Model/material adapter |
+---------+------------+------------+-----------+
          |
same game renderer, support/collision, nav,
rooms/portals/acoustics, light/circuits, AI/ghost,
water/weather, van/equipment, saved scene document
          |
real tests -> fixed camera captures -> checkpoints
          |
independent acceptance / human review -> staged promotion
\`\`\`

**Separation of concerns:** planning is not mutation; mutation is not save; save is not runtime promotion; runtime promotion is not human acceptance; gallery publication is not playability. Each step has an explicit evidence contract and cannot advance on the prior step’s success alone.

## 3. Canonical scene descriptor and typed operation contracts

**SceneDescriptor v1** (design): \`site_id\`, \`scene_id\`, \`site_role\` (hub/investigation/annex), \`document_uri\` (restricted workspace), \`document_codec\` and version, \`source_commit\`, \`source_hash\`, \`revision\`, \`catalog_hash\`, \`coordinate_convention\`, \`modules[]\`, \`capabilities[]\`, \`protected_refs[]\`, \`owner_epoch\`. Never accept only a friendly display name as write authority.

**OperationDescriptor v1:** stable operation identifier and version, target site kinds, supported node kinds, exact input schema, expected mutation footprint, affected dependent systems, estimated scene cost, preconditions, native validator, rollback behavior, save support and evidence tests. Capabilities are discovered from the actual host and do not grant the model direct file access.

**Illustrative write envelope** (not yet a working API):

\`\`\`json
{
  "schema": "beckenburg.map.operation/v1",
  "task_id": "school-pilot-01",
  "site_id": "SCHOOL",
  "scene_id": "beckenburg/school",
  "owner_epoch": 4,
  "source_sha": "<pinned-source-commit>",
  "document_hash_before": "<actual-SHA256>",
  "expected_revision": 108,
  "idempotency_key": "school-pilot-01:round-2:op-1",
  "operation": "build-wall-run",
  "arguments": {
    "id": "school-west-corridor-return",
    "x0_m": 10.5,
    "z0_m": 4.0,
    "x1_m": 10.5,
    "z1_m": 8.0,
    "base_y_m": 0.0,
    "height_m": 3.4,
    "thickness_m": 0.42,
    "material_uri": "assets://approved/stone"
  },
  "checks": ["support", "wall-room-ownership", "capsule-clearance", "save-reopen"]
}
\`\`\`

Angle units are explicitly radians/degrees per operation; Beckenburg uses X/Z horizontal, Y vertical; metric coordinates are finite. An accepted edit must carry \`before_revision\`, \`after_revision\`, affected object IDs and bounds, dependency invalidations, native tests and screenshot match conditions. **A rejected edit must leave source digest, revision, nav/collision state, history and saved document unchanged.**

**Operation families and proposed high-level commands** (implement as trusted compositions of existing human operations; these are NOT all currently supported):

| Family | Semantic operations | Physical or game authority | Minimum proof |
|---|---|---|---|
| Scene | \`open-site\`, \`inspect-space\`, \`inspect-component\`, \`list-capabilities\`, \`save\`, \`reopen\`, \`undo\`, \`redo\` | Current scene owner and \`EditorDocumentStore\` | Exact native ID/revision, on-disk hash |
| Loose props | \`place-prop\`, \`duplicate-prop\`, \`transform-prop\`, \`remove-prop\`, \`replace-mesh\` | \`LevelAuthoringSession\` / \`CastleLevelEdits\` | Protected attachments, source bounds, collision |
| Structure | \`place-foundation\`, \`build-wall-run\`, \`lay-floor-slab\`, \`join-roof\`, \`cut-door-opening\`, \`cut-window-opening\` | Mission structural host + native render/collider/room topology | Real thickness/underside/void, roof/LOS proof |
| Vertical | \`build-stairs\`, \`add-landing\`, \`cut-upper-slab\`, \`make-ramp\`, \`add-guard\` | Structure + support/nav + real capsule | Ascend/descend, headroom, no fall-through |
| Terrain | \`sculpt-height\`, \`cut-terrain-hole\`, \`edit-stitched-border\`, \`place-road\`, \`fit-foundation\` | Real terrain source/scene package/physics | Same actual terrain rendered/collided/ray tested |
| Rooms/services | \`create-room\`, \`link-portal\`, \`set-door\`, \`place-light\`, \`connect-circuit\`, \`set-acoustics\` | Shared rooms/nav/light/sound/game services | Physical doors, power, sound, no global light flush |
| Gameplay | \`set-van-dock\`, \`set-hunter-spawn\`, \`set-ghost-spawn\`, \`place-hiding\`, \`place-evidence\`, \`reserve-ritual\` | Game host/case/AI/evidence/ritual systems | Accessible sockets and real solo/TCP checks |
| Materials | \`assign-surface\`, \`mount-fixture\`, \`scatter-approved-prefab\`, \`edit-layer\` | Source-approved GLB/mesh/UV/material library | Stable original UV/pivot and supported placement |
| Review | \`run-native-test\`, \`sweep-route\`, \`capture-fixed-view\`, \`playtest\`, \`compare-checkpoint\` | True game engine and renderer | Structural + gameplay evidence and defects |

**Typed semantic macros are not a bypass.** Each decomposes to existing validated low-level commands inside a bounded transaction. If a macro requires an unimplemented geometry primitive, it is unavailable, not emulated with fake boxes or report text.

## 4. Operation validation and transaction invariants

1. Authenticate and authorize the target scene, command family and disk scope. Check exact site identity, owner epoch/fence, immutable catalog hash and source commit.
2. Check strict schema, finite metric coordinates, bounded geometry and active native object IDs. Reject unknown keys, unapproved assets or invented room/portal IDs.
3. Validate protected service/story attachments, support/collider bounds, default player capsule, route reservations and physical dependencies.
4. Acquire one exclusive scene-write permission; compare native revision and document digest. Reject stale edits, even when apparent object positions match.
5. Apply a bounded batch using the **existing** shared editor transaction/history. No direct write to GameMap, PNG, source JSON, Blender file or DLL outside its authoring adapter.
6. Re-evaluate affected render/collision/nav/rooms/portals/sound/light/services from the same native revision; use localized invalidation where possible.
7. Run affected native checks; on failed invariant undo the *whole* transaction and record a diagnostic. On nonblocking visual concern retain in a review sandbox but do not promote.
8. Save by explicit requested checkpoint only after validation. Read back the original document through the real native codec. Verify hash and scene identity; recover without overwriting user-owned files on failure.
9. Capture fixed-camera real pixels and an exact change report. A changed PNG is not proof of meaningful construction; compare actual structural mesh, physics bounds and gameplay test.

**Maximum batch:** existing \`EditorAgentService\` supports 1–64 commands, but first agent release should use a stricter 1–16 commands per transaction, 3 accepted edit rounds per task and explicit time/command/inference budgets. These are **proposed starting budgets**, not limits already enforced in source.

## 5. Task compiler: site work package to construction DAG

**TaskEnvelope v1** (design): \`task_id\`, \`request\`, \`site_id\`, \`site_stage\`, \`source_manifest_sha\`, \`allowed_tools\`, \`required_dependencies[]\`, \`geometry_region\`, \`expected_native_changes[]\`, \`acceptance_checks[]\`, \`budget\`, \`owner_epoch\`, \`human_review_gate\`, \`status\`. Dependencies must be explicit: a room cannot be furnished before it has structural support and safe access; a lit fixture cannot be accepted before its mount and circuit exist; a stair cannot be accepted without upper-floor opening.

A site goal compiles into stage-specific jobs from S00–S11, honoring the 29-site source register, approved concept, search-envelope maximum 40, actual existing assets and the user's Castle-first priority. Each job must point to **physical construction or gameplay integration** when in S03–S10. Invented map IDs, unapproved architectural history or source-less procedural meshes are rejected. The planner may fill spatial details inside approved bounds, but must escalate incompatible room functions or unresolvable site scope.

**Meaningful progress evidence by phase:**

- S00–S02: approved retain/refit/replace decisions, real space program and measured physical source. These are useful planning milestones but *not constructed/playable map progress*.
- S03: added or corrected real support, wall/floor geometry, openings, stairs and actual traversable route results.
- S04: edited terrain triangle/support geometry, physical foundations, grades, roads, cave cuts, water and bounds with tested runtime parity.
- S05: fully joined structural shells, decks/soffits, roofs and physical door/window apertures.
- S06: actual power/lighting, nav/room/portal/acoustic, spawns, hiding/evidence/ritual and van/equipment services.
- S07–S09: supported original asset placements, material/wear/weather and real visual composition without intersections.
- S10: actual in-game encounter and atmosphere integration, not storyboard-only links.
- S11: installed solo/host/guest gameplay, capture review, human acceptance, source/scene/catalog readback and promotion.

A field named \`percent_complete\` must not exist without a defined denominator for physical/gameplay gates. Publish counts of accepted real geometry changes, actual connected rooms, verified routes, fully integrated systems, map-stage gates and unresolved blockers instead.

## 6. Iterative planner / critic control loop

Implement a small trusted deterministic controller around local Ollama. Prefer a lightweight inference model for selection and ordered work proposals; escalate uncertain spatial/design interpretation to a larger model or human rather than granting broader tool permissions. The planner never gets unrestricted shell, private credentials, network or GitHub write capability.

\`\`\`python
def run_site_task(task, scene, budget):  # SPECIFICATION, not runnable code
    verify_pinned_authority(task, scene)
    for round_id in range(budget.max_rounds):
        state = scene.inspect_authoritative()              # one coherent revision
        checks = scene.run_targeted_native_tests(state)
        candidates = broker.list_allowed_operations(task, state, checks)
        if not candidates:
            return blocked("UNSUPPORTED_CAPABILITY", state, checks)
        proposal = ollama.propose_json(task, state, checks, candidates)
        edit = broker.validate(proposal, task, state, budget)
        if not edit.valid:
            journal.reject(edit.reason, state)
            if budget.retries_exhausted: return blocked("INVALID_PROPOSAL")
            continue
        result = scene.transaction_once(edit, state.revision, owner_fence=task.fence)
        if result.rejected:
            journal.record_failure(result)
            if result.recoverable: continue
            return blocked(result.code, result)
        post = scene.inspect_authoritative()
        proof = verify_real_geometry_and_gameplay(state, post, task)
        if not proof.passed:
            scene.rollback_transaction(post.revision)
            journal.record_failed_validation(proof)
            continue
        scene.save_and_reopen_verified(post.revision)
        capture_fixed_camera_pair_and_receipts()
        journal.checkpoint_accepted_round(task, proof)
        if task.acceptance_checks_met: return ready_for_review(proof)
    return blocked("BUDGET_OR_PROGRESS_LIMIT")
\`\`\`

The trusted controller must not describe “background processing” or claim task completion after it stops. It records exact job state and supplies a resumable checkpoint for the next authorized runner invocation. The critic compares actual scene geometry, physical routes, native errors and fixed-camera PNGs; **pixels changed** and **number of generated reports** are not sufficient success signals.

## 7. Concurrency: isolated map writers, shared limited planner

**Concurrency model:**
- One exclusive **writer lease per native site document**. A site worker owns only its scene and acquired source files. The lease is bound to an epoch/fencing token, current source hash and process identity.
- Multiple **independent** scene workers may run in parallel. Initial test: School and Farmhouse, no shared scene or source save path.
- A separate **single inference queue** feeds Ollama requests (initial 1 active generation). This avoids oversubscribing a domestic machine while still allowing edits/tests/captures in different processes. Local runner memory/network conditions may vary.
- Shared model, texture, GLB and prototype sources are immutable and version-pinned while map workers run. Any change to a shared asset becomes a separate task with explicit dependency fan-out and approval before consumers resume.
- Region-level concurrency **inside one scene** is prohibited in the initial design. It can be considered only after scene-owned transactional region locks, overlapping physics/portal dependencies and merge correctness are proven.
- Git commit production is separately serialized. Agents can propose tested source diffs; only a reconciler pushes reviewed results to the authorized main branch after checking current remote SHA. Never force-push.

**Lease and failover:** acquiring a map grants \`site_id\`, \`document_uri\`, \`writer_epoch\`, \`lease_deadline\`, \`source_sha\` and document digest. A stale worker cannot write after its epoch is superseded, even if it still holds a cached revision. A grace period does not make a lease safe: the native broker validates the current fence *at write time*. If a worker crashes after a successful edit but before receiving the response, the replacement process checks the request journal and saved scene fingerprint before resending; a different payload with the same idempotency key is rejected.

**Resource defaults (proposal, adjustable):** two native workers, one local Ollama generation, 16 native commands per edit batch, three acceptance rounds per worker task, four model attempts per round, three bounded retries after transient failures. Track CPU, memory, render GPU/raster capacity, disk, network and local inference model loading. Lower the number of workers on measured memory pressure; do not silently spawn unlimited CI jobs. A machine without Ollama must return \`MODEL_UNAVAILABLE\`, not substitute a hardcoded edit and call it agentic.

## 8. Durable task journal and state machine

**States:** \`QUEUED\`, \`WAITING_FOR_DEPENDENCY\`, \`LEASED\`, \`OBSERVING\`, \`PROPOSING\`, \`VALIDATING\`, \`APPLYING\`, \`TESTING\`, \`CHECKPOINTING\`, \`READY_FOR_REVIEW\`, \`ACCEPTED\`, \`BLOCKED\`, \`RETRYABLE_FAILURE\`, \`CANCELLED\`. Stage acceptance (\`AMB-xx\`) and map acceptance (\`Sxx\`) are different ledgers and must not be conflated.

**Journal fields:** event sequence, monotonic attempt count, task/site/scene ID, owner/fence, source and catalog digest, native revision before/after, command and payload digest, receipt/response status, failure code, native document hash, protected dependencies, test and capture manifest, planner model and sampled context limit, run ID, operator review decision. Keep user secrets and per-session bearer tokens out of logs.

**Checkpoint rules:** only checkpoint after the real native save and independent reopen/readback prove that persisted geometry equals the tested scene revision. Store evidence in immutable CI artifacts and the project gallery, linked by digest, with per-map status in the existing stage register after review. A runner cancellation must not leave an untracked accepted edit, and a retry must never duplicate a wall or prop placement. Reconciliation handles unknown responses before continuing.

**Failure classes:** \`UNSUPPORTED_CAPABILITY\`, \`BAD_SITE_ID\`, \`UNAUTHORIZED_SCOPE\`, \`STALE_REVISION\`, \`EXPIRED_WRITER_LEASE\`, \`SOURCE_HASH_CONFLICT\`, \`ATTACHMENT_CONFLICT\`, \`GEOMETRY_INVALID\`, \`COLLISION_BLOCKED\`, \`PORTAL_DISCONNECTED\`, \`RENDER_CAPTURE_UNAVAILABLE\`, \`DISK_CONFLICT\`, \`MODEL_INVALID_RESPONSE\`, \`MODEL_UNAVAILABLE\`, \`NO_SUBSTANTIVE_PROGRESS\`, \`HUMAN_REVIEW_REQUIRED\`, \`GAMEPLAY_ACCEPTANCE_FAILED\`. All code paths must be explicit; do not use unbounded broad exception catch-and-retry.

## 9. Test strategy: unit to full native playable acceptance

**Layer A — portable contracts:** JSON schema validation, geometry bounds, node identity, cap discovery, scene revision/fence, transaction rollback, corrupt save/migration, idempotency/nonce, source/read-only catalog. Focused Java 17 tests and Python MCP schema tests.

**Layer B — real native editor:** headless real House/School/Castle on Windows; exact authored scene and model source in memory; command/evidence parity; save/reopen/undo with stable identifiers. Include negative protected kitchen fixture and bad source/owner/mesh.

**Layer C — structural/runtime parity:** after new walls/floors/roof/door apertures, the actual renderer and player-support/collider/line-of-sight/nav/rooms/portals/acoustics must agree. Inject deliberately bad stair void, clipped wall, embedded lamp, disconnected corridor and unsupported floor; each must fail a **real** check. Do not weaken failing assertions.

**Layer D — iterative planning:** run three dependent edits, including one failed proposal and one retry. Require an observable native structural/physical change, not a 0.125 m cosmetic table nudge. Check wall thickness, route and actual fixed-camera image provenance after each accepted round.

**Layer E — concurrent task allocation:** two different sites, two fenced writer processes, isolated user homes and a shared single-slot Ollama. Inject overlap on same scene, unknown-result retry and cancellation; verify unaffected other site, no shared asset writes and exact per-site receipts.

**Layer F — WLD-14 source integration:** run first class native scene open, verified source/terrain/GLB/HCS2 readback; modify one real tower/keeper-house/cutout, then test actual game render/support/LOS/room/nav and native scene save. Keep mission locked until complete.

**Layer G — complete playable Lighthouse:** create tower, keeper house, covered passage, generator house, cistern, real coastal terrain, van staging, services and story. Validate 40-space limit, actual stair traversal, evidence/hiding/ritual, both lights states, installed Windows solo case, host/guest multiplayer and visual owner approval.

**Layer H — 29-site production:** preserve the owner-first-principles S00–S11 status register, reconcile existing PR construction and source ownership, publish stage-specific map and model proof, keep every failed site visible and bounded. Full map completion requires exact native structural/gameplay proof plus human review, not a green isolated editor workflow.

**Release discipline:** only objective build, launch, essential gameplay and required multiplayer/updater failures may block releases; subjective map quality/visual balance remain diagnostics/review conditions. A map still cannot be declared complete before its review. Preserve one CI job per needed workflow and do not add unrelated workflows.

## 10. Integration milestones and code ownership

See \`AGENTIC_MAP_BUILDER_IMPLEMENTATION_STAGES.md\` for the **16 ordered implementation stages, AMB-00 through AMB-15**, and \`AGENTIC_MAP_BUILDER_STAGES.json\` for machine-readable dependencies, code targets and acceptance checks. Every readiness requirement from A01 through I09 is assigned exactly one engineering stage; these assignments denote intended ownership, not feature completion.

**Required order of gates:**
- Editor operations: AMB-00 → AMB-01 → AMB-02 → AMB-03 → AMB-04/05/06/07/08.
- Directed work: AMB-09 iterative controller → AMB-10 durable checkpoints → AMB-11 concurrent scheduler.
- Site integration and proof: AMB-12 WLD-14 adapter → AMB-13 complete Lighthouse pilot.
- Scale and controlled promotion: AMB-14 → AMB-15.
- Technical stages can overlap when distinct source files and explicit prerequisites permit. **No major full-roster reconstruction starts before the agent can edit actual architecture and pass representative construction + verification.** Planning, approved asset refinement and essential game repairs can continue in parallel without crediting construction.

### Site-based stage alignment

| Engineering stage | G0–G12 alignment | Map S00–S11 output |
|---|---|---|
| AMB-00/01 | G0/G1/G10/G11 | S00 source ownership, consistent scene |
| AMB-02/03/04 | G1/G2/G4/G11 | S02/S03/S05 actual props, shells, floors/stairs |
| AMB-05/06/07 | G2–G9/G11 | S04/S06–S10 real terrain, services, assets |
| AMB-08/09 | G10/G11/G12 | Repeated native checks and iterative construction |
| AMB-10/11 | G1/G10/G11/G12 | Durable and concurrent S-stage execution |
| AMB-12/13 | G4/G8–G12 | WLD-14 S00–S11 complete pilot |
| AMB-14/15 | G10–G12 | All-site staged construction and reviewed promotion |

## 11. Review artifacts and proof policy

Each accepted round emits a compact machine-readable \`report.json\`, native source document, transaction receipt, checks, failure list, source/catalog/document SHA-256s, fixed-camera before/after PNG and optional true gameplay screenshot, plus user-facing summary. All images must be **real** renderer/Blender/source captures; image generation or a synthesized preview cannot prove geometry. A link to an artifact alone is not equivalent to inline visual proof during actual model/geometry work. Do not expose installed model files or personal tokens.

At a site milestone, publish the source/PNG review to the development gallery using the repository's existing gallery build/check procedure; include exact checked image paths, game version and source SHA. Respect source/model provenance; no silent original asset deletion. Each defect is added to the project defect ledger with reproduction, coordinates and next action.

The map is **READY_FOR_REVIEW** only when the actual native scene passes relevant construction/runtime checks and the fixed-camera captures exist. It is **ACCEPTED** only after human review when required and actual gameplay proof; unattended work stops at the review gate without assuming consent.

## 12. Deliberately deferred / nonblocking work

Do **not** postpone map construction indefinitely for unrelated optional editor features: a universal rigging suite, every UV sculpt brush, all acoustic hardware backends, cross-game renderer rewrite, full generic IDE/docking, distributed build farm, multi-writer same-scene region merges, arbitrary external plugin host or complete G0–G12 general-purpose engine certification.

Such work is worthwhile when directly required by a specific map function or by an accepted stage gate. It should not silently become an artificial prerequisite for an otherwise proven mission construction path.

**Actual remaining hard stop:** the current mission agent has no authoritative structural authoring commands, and WLD-14 is not a native runnable/editable mission. Solve these before calling a full-map automated builder operational.

## 13. Deliverable contract for future implementation turns

For any “Continue Agentic Map Builder” request, use this checklist:

1. Read current main commit, AGENTS.md, recovery REC-01/02 and editor acceptance runs; identify one smallest unaccepted AMB stage/subtask.
2. Verify the required human/engine operation already exists or implement that native primitive with exact render/collider parity first.
3. Add its typed capability descriptor, trusted host operation, fixed error semantics, positive and negative tests, bounded CLI/MCP agent entry and save/reopen proof.
4. Exercise real edited scene, actual renderer, physical and gameplay checks. Provide inline proof for visual/model work.
5. Commit one tested recoverable change to authorized main after reconciling the remote; read it back; link exact CI/artifacts and remaining failure.
6. Update the 124-item capabilities and AMB stage records **only with exact verified evidence**. Do not mark future phases complete based on planning, schema stubs, synthetic geometry or tests not run.

**No full map-builder completion claim until AMB-13 has actual native Lighthouse gameplay and review evidence.**
