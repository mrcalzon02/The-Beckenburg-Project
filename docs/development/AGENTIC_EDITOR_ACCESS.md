# Agentic access to the Beckenburg editor — implementation contract

**Owner direction:** 2026-10-31. **Repository:** `mrcalzon02/The-Beckenburg-Project`, `main`.  
**Inspected source baseline:** `3f42f2ce2110c084e34525f975a3bc8ce5a0e98f`.  
**Document status:** specification and execution plan; NOT a claim that machine control, headless operation, or an MCP connection is working.
**Owner superseding sequence:** complete and validate human-operated terrain, loading-zone, stitched-heightmap and terrain-LOD modules **before** any corresponding agentic controls. See [`TERRAIN_WORLD_SYSTEM_PLAN.md`](TERRAIN_WORLD_SYSTEM_PLAN.md), gates T0–T9. This document covers the later agent access work, not the current first build gate.  
**Machine task ledger:** [`AGENTIC_EDITOR_ACCESS_TASKS.json`](AGENTIC_EDITOR_ACCESS_TASKS.json).

**2026-11-01 owner priority update:** finish a small reusable service over proven existing editor operations first, then continue G0–G12. [Implemented service and evidence](EDITOR_AGENT_SERVICE.md) covers native mission and Castle adapters, shared human/agent transactions/history, save/reopen, genuine captures, structured tests and local headless protocol. No authenticated network/MCP adapter is claimed. Terrain-specific controls still require their human/runtime gates; the broader milestone acceptance below remains open.

**Current implementation design:** [Agentic Map Builder Architecture](AGENTIC_MAP_BUILDER_ARCHITECTURE.md), [stage-by-stage runbook](AGENTIC_MAP_BUILDER_IMPLEMENTATION_STAGES.md) and [stage DAG](AGENTIC_MAP_BUILDER_STAGES.json). Complete the task-critical editor upgrades before automated full-roster rebuild. These are future-stage specifications, not proof of implementation.

**Current full-map readiness audit:** [Agentic Map Builder Capability and Acceptance Register](AGENTIC_MAP_BUILDER_READINESS.md) and its [machine-readable requirements](AGENTIC_MAP_BUILDER_REQUIREMENTS.json). The proven agent transport is not the complete 29-site construction interface; see the detailed gap/status matrix.

## 1. User outcome and scope

On November 8, 2026, the Windows editor passed authenticated loopback observation, isolated local playtests and MCP editing. In the installed executable a client repaired a wall across the Castle spawn, captured the renderer, saved the repair and returned control to the human editor, whose Undo restored the wall. Other Castle audit findings remain independent of this repair. [Installed Windows acceptance](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/42473083566)

The installed upper-floor fixture subsequently inspected an authored lodging bed, placed an armchair from the human palette, checked its collision and the neighboring aisle, and returned gameplay and editor captures. The repair survived save/reopen and reversed through human Undo. The separate Material Editor and native mesh stdio host also passed against the same BTOP document and rendered pixels. [Installed furnishing and mesh acceptance](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/42492463902)

An authorized agent must eventually be able to carry out: **"Open Castle Beckenburg; inspect the second floor; find and fix misplaced geometry; furnish the room; check collisions; playtest; show me an actual rendered result."** It must perform operations on the *same* editor/game scene that human tools use, not manufacture a second approximation or merely commit scene-looking code.

Deliver the capabilities incrementally, each independently demonstrated before expanding:

1. **Observation** — live scene identity, hierarchy, components, assets, rendered view, collision/navigation and nonblocking diagnostics.
2. **Manipulation** — structured, bounded and reversible edits through the exact editor/runtime command boundary.
3. **Verification** — post-edit readback, runtime render/playtest, scene validation, stored evidence and honest success/failure results.
4. **Headless access** — scripts and CLI can carry out those capabilities without an open Swing window, while optionally attaching to one.
5. **MCP adapter** — an authorized agent can discover and invoke the same capabilities through an MCP server, without UI coordinate automation.

"One at a time" means no milestone is described as delivered based solely on a document or on components built for a later milestone. An early observation bridge can be useful without exposing mutations. No requirement to migrate other game repositories now.

## 2. Existing code and current limitations

Source inspection establishes the following **existing**, not hypothetical, components:

| Source | Verified behavior | Limit |
| --- | --- | --- |
| `src/main/java/org/beckenburg/DeveloperLevelEditor.java` | Swing editor displaying `SceneRenderer.drawCastle`; uses the Castle scene, hierarchy, inspector and ~90 ms viewport timer | No verified external machine command channel; Castle viewport is not a general-purpose headless backend |
| `src/main/java/org/beckenburg/CastleLevelEdits.java` | Persistent real Castle scene; snapshots and restore; props, lamps, architectural surfaces, stations, doors, spawn, paint, blocked tiles; revision counter | Castle-specific persistence; unsaved edits must not be mistaken for saved document |
| `src/main/java/org/beckenburg/CastleEditorProject.java` | `EditorProject` adapter exposes Castle hierarchy, asset catalog, snapshots and save | Flat representation, not yet full runtime scene/physics/typed inspector parity |
| `src/main/java/org/beckenburg/editor/RuntimeScene.java` and `InMemoryRuntimeScene.java` | Portable node IDs, parent/layer, transform/components, replace/remove and snapshots | Portable contract exists; it is not yet an authoritative adapter for all live Beckenburg scene kinds |
| `src/main/java/org/beckenburg/editor/EditorHistory.java` | Bounded transactional undo/redo; failing command restores before-state | Must become part of the authoritative machine-edit operation path, rather than a separate history |
| `src/main/java/org/beckenburg/CastleEditorAudit.java` | Diagnostics for duplicate IDs, embedded prop/light centres, blocked spawn using actual Castle collision | Advisory, limited sampling; not exhaustive mesh-overlap/physics/navigation proof |
| `tools/build-editor-kit.sh` and `tools/build-editor-kit.cmd` | Package the portable Java 17 editor classes | A distributable JAR does not imply a connected agent, live machine access or an operational MCP adapter |

Read and obey existing architecture/quality contracts: `LEVEL_EDITOR_OVERHAUL_AUDIT.md`, `MODULAR_LEVEL_EDITOR.md`, `PSX_RUNTIME_AND_EDITOR_SUITE_ARCHITECTURE.md`, `../wiki/psx-editor-engineering-guide.md`, and repository `AGENTS.md`. Preserve active recovery gates and existing game/multiplayer/updater behavior. A previously prepared but **uncommitted Git blob** is not a feature and must not be counted as delivered.

## 3. Architectural boundary — one authoritative scene

```text
  human Swing editor     headless CLI       agent / MCP client
         |                    |                   |
         +--------------------+-------------------+
                              |
              permissioned Editor Agent Service
      observe | transact | diagnose | verify | capture
                              |
        same authoritative editor/runtime scene model
              |                 |                |
          renderer         physics/nav       document IO
```

- **Game/runtime is authoritative.** No parallel "agent scene", preview-only geometry or independent physics. Existing Castle data remains a specific adapter; portable abstractions do not import Castle classes or Swing.
- **Transport is an adapter.** An in-process Java service exposes operations first; loopback HTTP/JSON, command line and MCP wrap that service rather than each implementing custom scene mutations.
- **Stable IDs and revisions.** Each operation addresses `sceneId`, `objectId`, and relevant expected revision. Never resolve a mutation by ambiguous label or approximate screen position alone.
- **Thread correctness.** Swing changes/UI capture execute on EDT as appropriate; runtime scene commits use one serialized transaction boundary. Never read half-applied edits.
- **Persistence is separate from mutation.** In-memory editing does not silently overwrite files; save uses existing authoritative persistence with atomic replacement, backup/recovery, version/readback checks.
- **Environment systems** (lighting, water, weather, audio, nav, ghost AI) remain authoritative in their own modules. Agent tooling discovers adapters and cannot silently invent a substitute system.
- **Network authority stays with gameplay host.** Editor playtests are isolated and must not mutate a real multiplayer session.

## 4. Machine protocol v1

Use a versioned envelope, independent of HTTP/MCP wiring.

**Read request:** `operation`, `sceneId`, `requestId`, optional `objectId`, filters/limits and viewport settings.  
**Write request (future phase):** additionally `expectedRevision`, typed arguments, explicit permission scope and unique idempotency key.  
**Response:** `schemaVersion`, `requestId`, `sceneId`, `revisionBefore`, `revisionAfter`, `ok`, `result` or `error`, `verified`, `evidence`.

Illustrative response (schema proposal, **not an observed result**):

```json
{
  "schemaVersion": "beckenburg.agent/v1",
  "requestId": "example-001",
  "sceneId": "beckenburg/castle",
  "revisionBefore": 42,
  "revisionAfter": 42,
  "ok": false,
  "verified": false,
  "error": {
    "code": "STALE_REVISION",
    "message": "Scene changed after the agent's inspection.",
    "retryable": true
  },
  "evidence": []
}
```

The response schema must distinguish **accepted**, **committed**, **saved**, **verified**, and **playtested**. An accepted command is not a verified scene edit. Captures are labeled genuine in-game/editor renders only when generated from the actual renderer with source revision and capture metadata. Never substitute artwork.

## 5. Failure contract — errors are expected, safe and inspectable

The requirement is **recoverability and truthful reporting**, not pretending there will be no errors.

1. Validate authentication, capability, object ID, asset reference, transform bounds, collision rules and expected revision **before** a write.
2. Capture a before-state and execute the entire logical edit as **one transaction**. On exception or failed validation, roll back to exactly that state and return a structured failure; do not leave half a room altered.
3. A command only returns `verified=true` after re-querying the same authoritative scene and comparing affected IDs, transforms, components and expected revision. A save only returns `saved=true` after a durable filesystem readback. Visual/playtest verification must be separately identified.
4. Use bounded timeouts, bounded queues, response/payload caps, cancellation, lifecycle cleanup and idempotency keys. Retrying the same edit must not duplicate props or lights.
5. Reject stale revisions before mutation; report current revision. If two agents compete, serialize writes and report conflicts, never silently overwrite.
6. Use fixed error classes: `UNAUTHORIZED`, `READ_ONLY`, `INVALID_ARGUMENT`, `NOT_FOUND`, `STALE_REVISION`, `ASSET_MISSING`, `OUT_OF_BOUNDS`, `COLLISION`, `UNSUPPORTED_CAPABILITY`, `TIMEOUT`, `SAVE_FAILED`, `RENDER_UNAVAILABLE`, `INTERNAL`. Each includes `code`, user-safe `message`, `retryable` and request ID. Never reveal tokens, local private paths or stack traces to an untrusted client.
7. **Fail closed** when readback is unavailable: return "unverified", not "success." Store redacted diagnostic logs and attach source revision / test artifacts.
8. Preserve autosave/recovery and older file versions; never destroy known-good scene data because a machine command failed. Every edit must remain undoable via the same human editor history.
9. Diagnostic geometry/art/balance warnings are **nonblocking**, except demonstrably nonfunctional build/launch/essential gameplay or required multiplayer/update behavior according to owner release policy.

**Required negative tests:** invalid mesh; invalid coordinates/NaN; stale revision; nonexistent ID; duplicate idempotency key; partial batch failure; undo/redo across human and agent actions; simulated disk-full save; renderer unavailable; terminated session; unauthorized request; TCP remote bind attempt; malformed/oversized command; two concurrent conflicting writes. Each must prove actual before/after state and correct error classification.

## 6. Execution milestones, in dependency order

### M0 — preparation and compatibility baseline (not a new feature)

Inspect current `main` SHA, relevant existing CI and recovery ledger. Verify the existing Castle editor launch, scene audit, portable runtime scene and history tests. Record exact pass/fail state, do not infer installed Windows acceptance from a Linux simulation. Preserve main branch; no destructive rewrites, no forced pushes.

**Exit:** exact SHA, reproducible test commands, current failure list and a no-regression check.

### M1 — OBSERVATION: live read-only access (first implementation milestone)

- Add a bounded, opt-in read-only observation service directly backed by the active Castle/editor scene: `capabilities`, `scene`, `objects`, `object/{id}`, `assets`, `diagnostics`, `viewport capture`. Provide stable identifiers, transforms, materials, lighting attributes, navigation metadata, revision, editor-versus-saved state and available capabilities.
- Snapshot scene data coherently; include unsaved in-memory edits; acquire viewport captures from the actual renderer, never a separately re-created mock scene. Track image dimensions, revision and render status.
- Provide an authorization-protected local machine endpoint (suggest loopback-only HTTP/JSON). **Disabled by default**, bind `127.0.0.1` only, use random per-session bearer token, do not expose token in URL, log or source control, reject non-GET methods during this phase. Enforce request/response limits. Local bridge availability is not the same thing as this ChatGPT session being connected.
- Keep the generic service interface independent of Swing; attach the Castle renderer and game-specific data through an adapter.
- Reuse `CastleEditorAudit`; label findings as advisory, incomplete centre probes, not proof of zero intersections.

**Acceptance:** start actual editor, query its unsaved scene, move a prop manually, query again and observe same stable ID with changed transform/revision; compare returned object catalog with editor hierarchy; obtain actual rendered viewport PNG; prove no write endpoint or unauthorized access; close editor and prove listener shuts down. Test launch and running scene on the supported Windows runtime. Log exact SHA, request-response evidence and screenshots.

### M2 — MANIPULATION: transactional structured commands

Implement first: `select`, `setTransform`, `placeAsset`, `setProperty`, `delete`, `duplicate`, `group/parent` where supported, `undo`, `redo`, `save`, `restore`. Only publish operations backed by real supported runtime/editor capabilities; report unsupported ones explicitly. Add typed commands for doors, lights, surfaces, spawns, nav and scene modules incrementally. Preserve IDs and asset pivots, apply collision-aware placement and use one transaction per logical edit with revision preconditions. Expose batch edits with all-or-nothing rollback.

**Acceptance:** one agent-driven Castle room furnish/repair task; exact readback, repeat/idempotency and undo/redo proof; simulated mid-batch error restores every prior object; save/reload with IDs and components identical. Both agent and human edit the *same* scene and same history.

### M3 — VERIFICATION: inspect → edit → prove

Expose explicit `audit`, `captureViewport`, `compareRevision`, `validateReferences`, `inspectCollision`, `runPlaytest`, `endPlaytestAndRestore`, and structured evidence manifest; implement only when corresponding real runtime adapters exist. The actual game renderer, navigation and collision must be used. Playtest snapshots unsaved work, runs actual gameplay in an isolated session and restores editing state without losing changes. Record frame timing where measurable; label absent metrics unavailable rather than inventing them.

**Acceptance:** actual Castle, first House, and basement scenario regression; prop-wall collision warnings, missing/unreachable stair opening, and door-opening performance diagnostics; screenshot from actual renderer, successful and failed cases; one-click equivalent playtest and exact unsaved-state return. Only claim fixture coverage for environments with a working editor/runtime adapter.

### M4 — HEADLESS automation and scripting

Build a CLI/client that connects to the same service or instantiates the runtime safely in headless mode; supports inspection, deterministic edit batches, dry-run, validate, rollback, and machine-readable output. Provide scripts for a small unrelated Java 17 synthetic scene to prove absence of Castle/Swing dependence. Headless captures need an actual supported renderer; do not fabricate a screenshot if none is available.

**Acceptance:** scripted project with no window, exit codes matching structured results, reproducible scene serialization, failed edit rollback, and match of headless vs GUI scene records.

### M5 — MCP agent adapter and supervised end-to-end task

Expose tool/resource names mapping **one-to-one** to the same service (for example `list_scenes`, `inspect_scene`, `inspect_object`, `list_assets`, `capture_viewport`, `run_diagnostics`, `apply_scene_edit`, `undo_scene_edit`, `verify_scene_edit`). Publish discoverable capability scopes, schemas and concrete errors. Local MCP uses authenticated explicit opt-in; never silently open ports or grant arbitrary file/shell access. A connected agent can perform a bounded room repair after observation and receive confirmed real-scene readback.

**Acceptance:** authorized agent connects to *running installed editor* via the intended client, lists objects, performs a reversible real edit, saves after explicit scope authorization, observes an actual runtime capture and audit, and reports what was and wasn't proved. Reject unauthorized client and cleanly disconnect; no player multiplayer exposure.

### M6 — portability and optional expansion

Once M1–M5 have evidence, generalize adapters across other game scenes and modules and prove a small independent game adapter. Consider standalone extraction only after second-project proof; avoid any speculative cross-repo migration.

## 7. Task-level delivery evidence

For **every** task store a ledger entry with:

- Task ID; capability; exact source files and affected runtime subsystem; dependencies; current status.
- Pre-change SHA and final pushed SHA; specific acceptance test(s) and results, including failures.
- Test class / workflow run URL and logs; real data and renderer captures labeled with provenance.
- Verified remote readback of committed content. A created blob or successful API write is **not** remote branch verification.
- What remains unverified, limitations, security behavior, next executable task.

Status vocabulary: `planned`, `in_progress`, `implemented_unverified`, `tested`, `blocked`. Do not use `tested` without evidence of source tests, and do not conflate source tests with live installed acceptance. Only objective essential-function breakage gates shipping; visual and editorial findings remain advisory.

## 8. Remaining work

Continue M2-02 for outstanding native command families and M3-03 for House and basement adapter diagnostics. Terrain-specific agent controls still depend on the human authoring, collision, rendering, streaming and loading-zone checks in the [terrain world plan](TERRAIN_WORLD_SYSTEM_PLAN.md). The machine ledger records the installed Castle, House and Material Editor evidence separately from those gates.

## 9. Current checkpoint

- **2026-10-31 module-first update:** Human-operated terrain, loading zones and stitched open-world terrain precede agent control. The initial finite-grid terrain/zone source and Blackpine height offsets have been committed; the portable stitched PNG class and focused test workflow are committed but not marked tested until actual Java 17 CI evidence exists. LOD, physics/render integration, scope-safe visibility and loading-zone gameplay transitions remain planned.

- **2026-10-31:** documentation and machine task ledger are being authored from inspected `main` baseline `3f42f2ce`. No completed agent-driven editing, Windows live-client connection, headless command execution or MCP integration is claimed by this checkpoint.
- Previous source inspection confirmed the shared scene, snapshots, current Castle-only audit and portable core. A Git object/blob not included in `main` is **not** a delivered implementation. Do not promote it as such.
