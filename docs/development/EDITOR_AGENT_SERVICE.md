# Editor Agent Service

The reusable service exposes proven human editor operations over the trusted headless protocol and authenticated MCP. Terrain-specific mutations still require their human and runtime gates. Broad scripting remains a later stage.

`org.beckenburg.editor.EditorAgentService<S>` owns no scene. An `EditorProject<S>`, its existing `EditorHistory<S>` and a host adapter supply all data, operations, renderer and tests. The portable core has no Swing or Beckenburg imports. The existing editor kit includes it without depending on game assets.

## Current working scope

### Observe the running desktop editor

Open **Verify Scene → Start Local Observation** in the level editor. The dialog displays a loopback address and the session's Authorization header. Use that header on each GET request. The listener starts only when selected. Stop it from Verify Scene. Switching levels or closing the window also stops the listener. Starting another session issues a fresh token.

Read `/summary` for the active scene ID, revision, supported observation routes and targeted tests. Use `/revision` for just the current scene ID and revision. Other observation routes require a revision. A request carrying a revision superseded by a human edit returns HTTP 409.

| GET path | Query parameters | Response |
|---|---|---|
| `/revision` | none | Active scene ID and revision |
| `/hierarchy` | revision, offset, limit | Up to 64 human hierarchy rows |
| `/node` | revision, id | One native row |
| `/nodes` | revision, repeated id | Up to 64 native rows in request order |
| `/changes` | revision, offset, limit | Latest committed change page |
| `/assets` | revision, offset, limit | Active editor palette entries and catalog fingerprint |
| `/verify-position` | revision, id, x, y, z | Actual node, expected-position match, latest commit membership and native disk comparison |
| `/test` | revision, id | Named host test with individual structured checks |
| `/capture` | revision | PNG from the active host renderer |

The response carries `X-Editor-Revision`. URL-encode parameter values. The bridge limits request URIs to 4,096 characters and responses to 4 MiB. It accepts requests only on 127.0.0.1 and requires the session bearer token. Observation sessions allow GET requests. The headless CLI and the running-editor bridge both call the same service.

### Connect an MCP client

The separate **Material Editor** can open its own read-only session through **File → Start local observation**. Use its displayed address and token with the same MCP client. The scene ID is the active mesh document. `hierarchy` pages its native vertices and `inspect_nodes` resolves stable vertex IDs. `verify_position` compares the vertex with expected coordinates and reports the mesh document's disk state. `run_test(revision=revision, id="visible-material")` checks the selected face's native material and its coverage in the displayed renderer frame. **File → Verify visible material** runs the same diagnostic. `capture_viewport` returns that frame as a PNG, including the active rotation, visibility and preview mode.

Apply or cancel a pending material preview before observing. A human edit or undo invalidates revision-bound reads. Returning to modeling, closing the Material Editor, opening another source, or choosing **Stop local access** ends the session.

For a write session, choose **File → Start local material editing**. The optional disk checkbox grants save and reopen for this mesh. Call `inspect_selection` for the selected face ID, material key, layer ID, blend mode and opacity. The `set-layer-opacity` command takes `face` (integer native ID), `layer` (string ID), and `opacity` (integer percent from 0 to 100). It uses the same validated layer edit and undo history as the human controls. Saving records the native mesh with the current workspace settings. Reopen reads that document into the existing editor and adds one undo step. The agent can verify the layer with `inspect_selection`, compare the document to disk with `verify_position`, and capture the rendered result.

1. In a Python 3.12 virtual environment, run `python -m pip install -r tools/editor_mcp_requirements.txt`.
2. In **Verify Scene**, choose **Start Local Observation** for reads or **Start Local Editing** for validated transactions and shared undo/redo. Select **Also allow saving and reopening this document** to grant those two operations. Reopening reads the host's existing document and adds one undo step.
3. Configure the client's stdio server command as that environment's Python executable. Pass the absolute path to `tools/editor_mcp.py` as its only argument. Set `EDITOR_AGENT_URL` to the displayed loopback address and `EDITOR_AGENT_TOKEN` to the token following `Bearer` in the access dialog. Supply the token through the client's private environment settings.
4. Call `inspect_scene` for the scene ID and revision, or read `editor://active/summary`. Use `inspect_nodes` to read selected objects, `run_test` for the named native checks, and `capture_viewport` for an actual PNG. Tool discovery reflects the selected access scope. The edit tool's batch schema lists the active host's operations and their required fields.

For example, Castle's `set-spawn` operation takes numeric x, z and yaw fields. Use the revision returned from a successful edit for `verify_position`, undo or save. If the revision is stale, inspect the scene again before retrying. After stopping access, switching scenes or closing the editor, start a fresh session and reconnect with its new address and token.

When the scene's capabilities are already known, call `inspect_scene(revision_only=True)` over MCP or send `revision` over the headless protocol. Both read the live owner thread. The House fixture returned a 64-byte headless response, including the protocol envelope. After a stale edit, read the affected nodes at the new revision before deciding whether to retry.

### Furnish and inspect a Castle room

Use `check_walkability(revision, x, y, z)` for a standing player's foot position. Castle and mission levels call their respective game map. The human tool is **Verify Scene → Check walkability**.

In a Castle editing session, `place-prop` takes a new hierarchy ID, an approved palette mesh name, and world coordinates `x`, `y`, `z`. It uses the human palette's scale, orientation and collision footprint. `focus_node` selects that native row and positions the actual editor camera without changing the document revision. **Verify Scene → Focus object** uses the same view. Capture the viewport there, then check the prop's center and the neighboring passage. In the tested upper lodging the chair blocked standing at (5.75, 3.4, 13.5) and the aisle at (5.75, 3.4, 14.35) remained walkable. The trusted stdio equivalents are `focus<TAB>revision<TAB>id` and `walkability<TAB>revision<TAB>x<TAB>y<TAB>z`.

### Inspect a Castle door or stair

In **Paint & nav → Door sockets**, choose a physical aperture and its mounted leaf. The dropdown shows only leaves compatible with the socket. The corresponding `set-door` command takes `slot` and `leaf` (`EMPTY`, `WOOD` or `PORTCULLIS`). `castle-door-sockets` checks the available native sockets and reports their IDs, positions and assignments. `set-nav-cell` takes integer cell coordinates and a state of `BLOCKED` or `CLEAR`. These are the same navigation edits as **Set nav**.

Use **Verify Scene → Test → castle-cellar-passage**, or call `run_test` with that ID. It follows the native player elevation and standing collision down the cellar flight and back up. A failed check reports the first blocked foot position. The baseline reached the −3 and 0 landings. Blocking the upper landing or removing the native stair opening made both directions fail. Undo restored passage.

### Retry a write after a lost response

For a live authenticated session that advertises `requestSequences: ["v1"]`, pass an optional positive integer `sequence` to an MCP action. If the response is lost, send the same action, original revision, arguments and sequence. The editor returns the original receipt, failure or gameplay PNG even if later edits have changed the scene. A replay does not create an undo entry or run gameplay again. Read the current revision before issuing a new action.

For trusted stdio, prefix a write with `once-v1<TAB>sequence<TAB>` on the same input line. For direct HTTP, set the `sequenceV1` query parameter on a POST. Give each distinct action a fresh, increasing number within the session. A different request with a retained number receives a conflict. The journal retains up to 64 receipts and 4 MiB of response data. It retires the lowest numbers first and refuses every sequence at or below the retired boundary. The history ends with the authenticated session or stdio process. Clients sharing a session must coordinate their numbers; after reconnecting, inspect the native scene to resolve an uncertain prior write.

In an editing session, `recent_actions(revision, after, limit)` returns events following the supplied event number. Stdio uses `actions<TAB>revision<TAB>after<TAB>limit`. The session retains the latest 128 attempts, and each page holds up to 64. `firstAvailable` and `lastEvent` identify the retained window. Events record UTC time, action, allowlisted command names, sequence, requested and observed revisions, outcome and SHA-256 request fingerprint. This lets a client distinguish an applied command from its replay or a refused attempt without storing bearer tokens or raw arguments. Closed-session history is released. The installed Castle acceptance artifact includes a copy of its session events.

### Embed the same transports in another editor

The standalone Java 17 kit includes `EditorAgentProtocol.run(service, reader, writer)` for trusted headless processes and `EditorObservationBridge(service, scope, ownerExecutor)` for interactive editors. Construct the bridge on the project's owner thread. The executor must dispatch future requests to that thread. Display the session address and token to the user who enabled access, and close the bridge when the scene changes or the window closes.

The project's host supplies its operation field schemas and capabilities. The same MCP adapter discovers those schemas at startup. The independent kit test's renderer-less project advertises its own increment operation, handles edits and undo through its native state, and omits viewport capture from discovery.

### Native service capabilities

The Level Editor’s **Playtest** button runs local gameplay against the current unsaved level. Castle uses its walkable hub with a temporary player profile. Mission levels use spectral practice with their current authored geometry and a fresh gameplay session. **Return to editor** closes the gameplay panel and restores access to the same edit session. The editor retains its revision, selection, camera and undo history. The observation API declines scene requests while gameplay owns input. An editing session also offers the MCP tool `playtest(revision)`. It opens that same gameplay panel, waits for the Castle clock or mission simulation to advance, captures the live panel as a PNG, and returns to the editor. A stale revision is rejected before launch. Human-initiated playtests still run until the user returns.

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

Use `CASTLE` or a supported `HouseMap.Site` name. The explicit home selects the host's documents and preferences. To inspect unsaved changes in an open human editor, connect to that editor's authenticated session.

For a mesh, pass the native BTOP document itself. The process uses the Material Editor's validated operations, bounded native history, file revision checks and game renderer.

```text
java -cp build/beckenburg-game.jar org.beckenburg.EditorAgentCli MESH "C:/assets/door.btop"
```

Send `revision`, then `selection<TAB>revision` to read the face saved in the workspace. The headless material view initially selects the top layer. If the workspace has no selected face, the result is null. Append `--face 12` to inspect a specific native face for the session. This option leaves the saved workspace selection intact. Use `set-layer-opacity` with the face and layer IDs, then read the selection again at the returned revision. Save and reopen use the existing BTOP path. A competing writer must reopen before it can save over a newer version.

Mesh captures use the Material Editor's 280 × 190 base-material view at zero rotation with all faces enabled. Give `capture` a new PNG filename. The `visible-material` diagnostic tests whether the selected face contributes pixels. A bound face behind another surface fails the coverage check. The authenticated desktop session remains the entry point for inspecting its unsaved document, camera, visibility filters or diagnostic mip.

Requests are UTF-8 lines with fields separated by **literal tabs**. Start with `summary` for the active revision, then use the revision returned by each operation. For a local edit, use `node` to read the target and `verify-position` to check the resulting coordinates. Use `inspect` when you need the entire scene.

```text
summary
node<TAB>revision<TAB>prop-0
edit<TAB>revision<TAB>move-prop<TAB>id=prop-0<TAB>x=4.5<TAB>y=0<TAB>z=8
verify-position<TAB>returned-revision<TAB>prop-0<TAB>4.5<TAB>0<TAB>8
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

Use `verify-position<TAB>revision<TAB>id<TAB>x<TAB>y<TAB>z` to check an expected position, or choose **Verify Scene → Verify Position** with a row selected. The proof reads the authoritative node again. `verified` compares its three coordinates exactly. `committed` means the native change record for that revision lists the object as added or updated. A fresh host has no commit record. `diskState` compares the complete live authoring snapshot with the decoded bytes currently on disk and reports MATCHES, DIFFERS, MISSING, UNREADABLE or UNSUPPORTED. This read leaves the document, journal and history untouched.

The November 8 House fixture moved two props in one transaction and verified the first prop at the returned revision. The receipt was 78 UTF-8 bytes. The position proof was 257 bytes and matched the native hierarchy row in the 42,296-byte full inspection. The localized path returned 335 bytes in total versus 42,374 bytes with full inspection. The headless acceptance test writes these measurements to `localized-edit-response.json` in its evidence directory. Model tokens, tool schema overhead and elapsed time are separate measurements.

With `tiktoken` installed, add `--measure-tokens` to the headless acceptance command to count the raw JSON response text using `o200k_base`. One run counted 24 tokens for the edit receipt, 88 for the position proof, 18,020 for the full inspection and 20 for the revision-only response. That puts the two-response localized sequence at 112 tokens versus 18,044 for the full inspection sequence. Request text, transport framing and tool schemas are outside this count. Exact inspection counts can vary with the scene and document path.

## Batched headless edits

After an edit or undo, `changes<TAB>revision<TAB>offset<TAB>limit` pages through the latest committed native change in this session. Each page gives the preceding revision, total changed node count, and up to 64 stable IDs with change kinds and content fingerprints. Fetch a changed node with `node` to inspect its current value. Removed nodes carry only their previous fingerprint. When `fullScene` is true, the host requires whole-scene invalidation and the listed IDs may be incomplete. A new process starts without a last change. A stale revision or a revision without commit metadata returns an error.

`batch` takes the expected revision followed by one to 64 ordinary edit operations. Separate operations with a literal `|` field. All fields use the same tab delimiter as `edit`. A pipe inside a `key=value` field is part of its value. For example, the following request moves two props in a single native history entry. Replace `7` with the revision from `summary`.

```text
batch	7	move-prop	id=prop-0	x=1	y=0	z=3	|	move-prop	id=prop-1	x=2	y=0	z=4
```

The response carries the revision after the entire transaction. Use that revision for the next read, edit or undo. The service checks all operation names, applies supported human editor operations on the owner thread, validates the resulting scene and rolls back the transaction on failure. After an error, use `summary` to obtain the current revision. Native save is still explicit. The existing request limit of 16,384 UTF-16 code units applies to the complete line.
Castle change pages report resolved hierarchy IDs for surfaces, props, lamps, stations and spawn. Spawn-only edits normally return one start node. Fingerprints cover the complete native record, including spawn yaw. World edits retain full-scene invalidation, so a client may receive affected node IDs alongside fullScene=true. Changes such as navigation or floor paint can have no hierarchy rows. Shared IDs use the same first-match order as the human hierarchy.
