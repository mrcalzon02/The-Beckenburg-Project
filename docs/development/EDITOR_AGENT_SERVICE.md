# Editor Agent Service

Owner priority, 2026-11-01: finish a small service over proven human editor operations, then continue the G0–G12 staged plan. Broad scripting and authenticated MCP remain later integrations. This priority supersedes the old requirement to defer **all** agent infrastructure until terrain completion; terrain-specific mutation still requires working human/runtime terrain gates.

`org.beckenburg.editor.EditorAgentService<S>` owns no scene. An `EditorProject<S>`, its existing `EditorHistory<S>` and a host adapter supply all data, operations, renderer and tests. The portable core has no Swing or Beckenburg imports. The existing editor kit includes it without depending on game assets.

## Current working scope

### Observe the running desktop editor

Open **Verify Scene → Start Local Observation** in the level editor. The dialog displays a loopback address and the session's Authorization header. Use that header on each GET request. The listener starts only when selected. Stop it from Verify Scene. Switching levels or closing the window also stops the listener. Starting another session issues a fresh token.

Read `/summary` for the active scene ID, revision, supported observation routes and targeted tests. Every other route requires the revision from that summary. A human edit invalidates the previous revision and returns HTTP 409.

| GET path | Query parameters | Response |
|---|---|---|
| `/hierarchy` | revision, offset, limit | Up to 64 human hierarchy rows |
| `/node` | revision, id | One native row |
| `/nodes` | revision, repeated id | Up to 64 native rows in request order |
| `/changes` | revision, offset, limit | Latest committed change page |
| `/assets` | revision, offset, limit | Active editor palette entries and catalog fingerprint |
| `/test` | revision, id | Named host test with individual structured checks |
| `/capture` | revision | PNG from the active host renderer |

The response carries `X-Editor-Revision`. URL-encode parameter values. The bridge limits request URIs to 4,096 characters and responses to 4 MiB. It accepts requests only on 127.0.0.1, requires the session bearer token, rejects browser-origin requests and exposes read operations only. The headless CLI and this running-editor bridge both call the same service.

### Native service capabilities

The Level Editor’s **Playtest** button runs local gameplay against the current unsaved level. Castle uses its walkable hub with a temporary player profile. Mission levels use spectral practice with their current authored geometry and a fresh gameplay session. **Return to editor** closes the gameplay panel and restores access to the same edit session. The editor retains its revision, selection, camera and undo history. The observation API declines scene requests while gameplay owns input. Playtest currently starts from the human editor.

- Inspection returns the actual unsaved authoring snapshot, hierarchy, document path, revision and explicit capabilities/operation/test lists.
- Every exposed write requires the last observed revision. A transaction contains 1–64 allowlisted commands and creates one shared undo entry. Invalid commands restore the entire before-state. Human edits, Undo, Redo and Save use this same service/history.
- Save uses existing host serialization. Reopen replaces state in the existing authoring object, is undoable, and rolls back malformed input.
- Mission BSCN and Castle properties saves now use the portable `EditorDocumentStore`: 8 MiB enforced during output, forced staged bytes and checksummed write-ahead journal, atomic replacement, one distinct valid previous document and a per-document writer lock. Reopen finishes an interrupted save only when current bytes still match its recorded prior/committed hash. Corrupt journals and externally changed targets fail without overwriting them. This is tested process-crash recovery; filesystem/provider power-loss durability is not asserted.
- The human mission/Castle verification menu and headless `recover` request explicitly restore the previous saved version through native identity/prop validation. The same live session and history are used. Undo restores the preceding live edits, while the recovered disk document changes only through a later explicit save. Castle retains its Properties format and existing migrations, with deterministic bytes so identical saves preserve the prior distinct document.
- Typed batches accept an owner-side cancellation predicate and honor thread interruption between host operations. Cancellation rolls back the whole native state, including IDs, and preserves prior undo/redo history; it does not preempt a single running host operation.
- Capture renders a new frame through the host's `SceneRenderer` at the current viewport camera and returns PNG path, dimensions, renderer entry point and scene revision. A host without capture capability fails explicitly.
- Targeted tests return individual `{code, passed, detail}` checks plus scene identity/revision. `castle-scene-audit` reports the existing advisory centre-probe audit, not full geometry acceptance. `mission-runtime-parity` compares native `HouseMap` props and colliders with current unsaved authoring data; it is not a full gameplay test.

The Castle adapter binds `CastleLevelEdits.current()` because that is what Castle collision and rendering consume. It supports `set-spawn` (`x`, `z`, `yaw` in radians). The mission adapter binds the active `LevelAuthoringSession` and the same native `HouseMap` as its human viewport. It supports `move-prop` (`id`, `x`, `y`, `z`). Mission snapshots contain `{sceneId, ids, props}`. Existing BSCN v1 node IDs are preserved; new objects receive fresh UUID IDs. Deletion does not rename surviving objects, and undo/save/reopen retain identity. Use IDs returned by inspection and revision preconditions. USER session document ID/path are pinned at open; native loading explicitly rejects unsupported parenting, pitch/roll and nonuniform scale.

In the human editor, **VERIFY SCENE** offers Inspect, Test, Capture PNG, Reopen Saved and Recover Previous Save. `DeveloperLevelEditor.agentService()` exposes only its active scene and requires the EDT. Previously active scene services and closed-editor services reject requests. No listener is opened. Trusted host code may invoke `edit(Runnable)`; a transport must expose only typed allowlisted transactions, never this callback.

## Headless interface

After a normal game build, run Java 17:

```text
java -cp build/beckenburg-game.jar org.beckenburg.EditorAgentCli HOUSE C:/path/to/isolated-project-home
```

Use `CASTLE` or a supported `HouseMap.Site` name. The explicit home selects existing host documents and preferences rather than constructing an agent-only scene. Use the same project home as the human editor to compare saved scene data. A separate headless process does not attach to another editor's unsaved state: the in-process active service is the attachment point for a future authenticated transport.

Requests are UTF-8 lines, fields separated by **literal tabs**. Read a fresh `inspect` response for every revision; do not predict revisions after rollback/reopen. Commands:

```text
inspect
edit<TAB>revision<TAB>move-prop<TAB>id=prop-0<TAB>x=4.5<TAB>y=0<TAB>z=8
undo<TAB>revision
redo<TAB>revision
save<TAB>revision
reopen<TAB>revision
recover<TAB>revision
capture<TAB>revision<TAB>C:/path/to/frame.png
test<TAB>revision<TAB>mission-runtime-parity
```

One JSON response is emitted per request on stdout; runtime diagnostics go to stderr. `ok` denotes successful request execution; inspect each test check for pass/fail. Exit status is 1 if any request or test failed, otherwise 0 at EOF. Requests are limited to 16,384 UTF-16 code units while reading, before unbounded line buffering. A protocol read/limit error emits a structured `phase: protocol` failure and terminates the process. Startup failures emit `phase: open` and do not create a placeholder scene. Both output channels are UTF-8 independently of the Java17 platform default. The process has normal local project/file permissions; this is a trusted local CLI, not an authentication/security boundary. No remote MCP, arbitrary script/class/shell execution or terrain commands are exposed.

## Evidence and remaining gates

`EditorAgentServiceTest` exercises real mission/Castle state, stale rejection, shared history, mid-batch/cancellation rollback, saved game load, undoable reopen, corruption rejection, renderer PNG, owner-thread enforcement and CLI failures. `EditorAgentGuiTest` opens the actual Windows editor, edits a House prop, clicks human Undo/Redo, saves/reopens, drives the real recovery chooser/confirmation, verifies native collider parity, captures Castle/House PNGs, rejects the inactive Castle service and rejects closed-editor access. `LevelAuthoringRecoveryTest` proves exact native save recovery and game loading with wrong-scene negative controls. `EditorDocumentStoreTest` halts actual child processes before/after atomic replacement and verifies exact replay, backup, conflict/corruption/path checks, output bounds and writer locking without game files. `EditorCorePortabilityTest` exercises a small unrelated project without game files and proves a renderer-less host cannot fake capture.

Local Java 17 compilation and these tests passed during implementation. GUI captures are under `build/editor-agent-evidence/` and were visually inspected. This is source-editor evidence, not installed-release, physical FPS, full gameplay or G11/MCP acceptance. Follow the recovery checkpoint for exact committed SHA and remote CI results.

G1's stable mission IDs, pinned USER ownership, journaled mission/Castle persistence and cooperative batch cancellation are implemented and locally tested. Continue G0's provenance/gap audit and G1's broader migrations, dependency ownership/revision events and incremental deltas before admitting new G2–G12 capabilities. Service existence does not close those gates.

`CastleDocumentRecoveryTest` additionally proves full native properties readback, deterministic no-op saves, distinct backups, corrupt-file refusal, same-singleton explicit recovery, shared undo and preserved legacy structure backups. Existing geometry and lighting migration regressions are mandatory in the native host check. The GUI test drives Castle recovery through the actual menu, and the public CLI test reopens and recovers Castle in fresh processes.

## Committed change metadata

The portable `EditorChange` record carries the owning scene/document, before/after native revisions, immutable stable-ID deltas and content fingerprints, referenced assets, dependency invalidation and optional native physics bounds. It stores no scene and cannot apply edits. A trusted host can receive one notification per committed human/agent transaction or shared undo/redo. Rejected/cancelled batches publish nothing. Save does not fabricate an edit. Hosts without a granular descriptor, or whose descriptor fails, conservatively invalidate all dependencies.

The mission adapter compares complete native prop records by persisted ID, so rotation, mesh, collision dimensions and other same-position edits are detected. Physics dirty bounds union the old/new **actual** `LevelPlans.propCollider` footprints, including origin offsets, rotation and native height corrections. These bounds do not claim GLB/render extents; the renderer and immutable native map still rebuild conservatively. The human viewport consumes notifications and native revision changes instead of comparing/copying the full prop list on every frame. Castle currently uses full invalidation.

`EditorChangeIntegrationTest` proves committed batch coalescing, no rollback/cancellation leakage, exact owner/revision provenance, native fingerprints, rotated collider coverage, removal/undo/addition, redo/reopen and Castle fallback. The actual GUI test checks that unchanged revisions reuse the native map and a committed edit rebuilds it from authoritative props. The standalone adapter test proves failed metadata extraction still invalidates a committed edit. Notification callbacks are trusted synchronous host code; no external subscription/remote API or arbitrary callback surface is exposed.

## Observed disk revisions and recovery before open

Native mission and Castle sessions now retain the exact SHA of the bytes they decoded or successfully saved. Save compares this observed revision inside the shared writer lock after staging; competing edits and external deletions refuse without replacing newer files or discarding unsaved live edits. Explicit reopen observes the newer file; shared undo can restore the prior live edit, and a subsequent explicit save deliberately writes it against that now-observed disk revision. Undo never rewinds disk ownership. Snapshot data is immutable, and failed host restores retain undo/redo targets instead of consuming history.

For a corrupt document that prevents creating the runtime, invoke the same validated recovery operation **before opening**:

```text
java -cp build/beckenburg-game.jar org.beckenburg.EditorAgentCli CASTLE C:/path/to/project-home --recover-previous
```

Then send normal `inspect`/other requests. The flag explicitly replaces the target with its validated previous document, using the same backend as the human recovery menu, and only then constructs the actual host. It does not create a default/fake scene to make recovery available. Without the flag, invalid input remains untouched and startup returns a structured error.

Native schema compatibility is explicit: mission components must contain only supported fields; custom labels/layers, contradictory duplicate yaw values and unsupported transforms are refused rather than discarded. Legacy optional `yawRad` remains supported. Future Castle layout versions and station coordinates that the human editor rejects cannot be installed through load/recovery. Castle codecs for explicitly selected files no longer initialize or migrate an unrelated default runtime profile. Door serialization uses stable socket order, preserving useful prior versions after value-equivalent remove/reinstall or history restore.

The process test also runs the complete House workflow while the unrelated default Castle document is corrupt and proves that document remains untouched. The independent editor JAR has no Castle/game imports; native adapters deliberately retain their actual engine-owned objects.

## Localized inspection

Start with `summary` to get the active scene ID, revision, capabilities, operations and test names. `hierarchy<TAB>revision<TAB>offset<TAB>limit` returns the total count and up to 64 native hierarchy rows in editor order. `node<TAB>revision<TAB>id` returns one row with its stable ID, kind, label and position. The angle-bracketed TAB denotes one literal tab in the existing line protocol. Use the returned revision for a validated edit. Refresh the summary after a stale revision error. Full `inspect` remains available for full native snapshot data.

Both localized queries use the host's current human editor hierarchy on its owner thread. The service keeps no independent scene index. The small replies bound transmitted data. A single-node request builds one row in the two native adapters. Castle resolves the ID in its existing scene maps. Mission levels locate it in the existing stable-ID list and read the corresponding prop. Both native adapters construct only the requested hierarchy rows. Mission pages address the native prop list by index. Castle pages slice its ordered native maps and read station or spawn rows only when requested. Reaching a late entry in a Castle map still requires walking earlier entries. On the November 7 verification fixtures the full and single-node UTF-8 responses were 42,296 and 191 bytes for House, and 103,149 and 154 bytes for Castle. These measurements cover payload size only.

For a sparse selection, `nodes<TAB>revision<TAB>id1<TAB>id2` returns up to 64 native hierarchy rows in request order under one revision. A missing ID or stale revision fails the entire read. This also lets a client fetch the current values for several updated IDs from a change page in one request.

The CLI also accepts `assets<TAB>revision<TAB>offset<TAB>limit`. Asset pages follow the active editor's unfiltered palette order. An import can change the runtime catalog without editing the scene. Compare the catalog fingerprint across pages and restart at offset zero if it changes.

## Batched headless edits

After an edit or undo, `changes<TAB>revision<TAB>offset<TAB>limit` pages through the latest committed native change in this session. Each page gives the preceding revision, total changed node count, and up to 64 stable IDs with change kinds and content fingerprints. Fetch a changed node with `node` to inspect its current value. Removed nodes carry only their previous fingerprint. When `fullScene` is true, the host requires whole-scene invalidation and the listed IDs may be incomplete. A new process starts without a last change. A stale revision or a revision without commit metadata returns an error.

`batch` takes the expected revision followed by one to 64 ordinary edit operations. Separate operations with a literal `|` field. All fields use the same tab delimiter as `edit`. A pipe inside a `key=value` field is part of its value. For example, the following request moves two props in a single native history entry. Replace `7` with the revision from `summary`.

```text
batch	7	move-prop	id=prop-0	x=1	y=0	z=3	|	move-prop	id=prop-1	x=2	y=0	z=4
```

The response carries the revision after the entire transaction. Use that revision for the next read, edit or undo. The service checks all operation names, applies supported human editor operations on the owner thread, validates the resulting scene and rolls back the transaction on failure. After an error, use `summary` to obtain the current revision. Native save is still explicit. The existing request limit of 16,384 UTF-16 code units applies to the complete line.
Castle change pages report resolved hierarchy IDs for surfaces, props, lamps, stations and spawn. Spawn-only edits normally return one start node. Fingerprints cover the complete native record, including spawn yaw. World edits retain full-scene invalidation, so a client may receive affected node IDs alongside fullScene=true. Changes such as navigation or floor paint can have no hierarchy rows. Shared IDs use the same first-match order as the human hierarchy.
