# Editor Agent Service

Owner priority, 2026-11-01: finish a small service over proven human editor operations, then continue the G0–G12 staged plan. Broad scripting and authenticated MCP remain later integrations. This priority supersedes the old requirement to defer **all** agent infrastructure until terrain completion; terrain-specific mutation still requires working human/runtime terrain gates.

`org.beckenburg.editor.EditorAgentService<S>` owns no scene. An `EditorProject<S>`, its existing `EditorHistory<S>` and a host adapter supply all data, operations, renderer and tests. The portable core has no Swing or Beckenburg imports. The existing editor kit includes it without depending on game assets.

## Current working scope

- Inspection returns the actual unsaved authoring snapshot, hierarchy, document path, revision and explicit capabilities/operation/test lists.
- Every exposed write requires the last observed revision. A transaction contains 1–64 allowlisted commands and creates one shared undo entry. Invalid commands restore the entire before-state. Human edits, Undo, Redo and Save use this same service/history.
- Save uses existing host serialization. Reopen replaces state in the existing authoring object, is undoable, and rolls back malformed input.
- Mission BSCN saves now use the portable `EditorDocumentStore`: 8 MiB enforced during output, forced staged bytes and checksummed write-ahead journal, atomic replacement, one distinct valid previous document and a per-document writer lock. Reopen finishes an interrupted save only when current bytes still match its recorded prior/committed hash. Corrupt journals and externally changed targets fail without overwriting them. This is tested process-crash recovery; filesystem/provider power-loss durability is not asserted.
- The human mission verification menu and headless `recover` request explicitly restore the previous saved version through native identity/prop validation. The same live session and history are used. Undo restores the preceding live edits, while the recovered disk document changes only through a later explicit save. Castle retains its existing properties serializer and reports previous-save recovery unavailable.
- Typed batches accept an owner-side cancellation predicate and honor thread interruption between host operations. Cancellation rolls back the whole native state, including IDs, and preserves prior undo/redo history; it does not preempt a single running host operation.
- Capture renders a new frame through the host's `SceneRenderer` at the current viewport camera and returns PNG path, dimensions, renderer entry point and scene revision. A host without capture capability fails explicitly.
- Targeted tests return individual `{code, passed, detail}` checks plus scene identity/revision. `castle-scene-audit` reports the existing advisory centre-probe audit, not full geometry acceptance. `mission-runtime-parity` compares native `HouseMap` props and colliders with current unsaved authoring data; it is not a full gameplay test.

The Castle adapter binds `CastleLevelEdits.current()` because that is what Castle collision and rendering consume. It supports `set-spawn` (`x`, `z`, `yaw` in radians). The mission adapter binds the active `LevelAuthoringSession` and the same native `HouseMap` as its human viewport. It supports `move-prop` (`id`, `x`, `y`, `z`). Mission snapshots contain `{sceneId, ids, props}`. Existing BSCN v1 node IDs are preserved; new objects receive fresh UUID IDs. Deletion does not rename surviving objects, and undo/save/reopen retain identity. Use IDs returned by inspection and revision preconditions. USER session document ID/path are pinned at open; native loading explicitly rejects unsupported parenting, pitch/roll and nonuniform scale.

In the human editor, **VERIFY SCENE** offers Inspect, Test, Capture PNG and Reopen Saved. `DeveloperLevelEditor.agentService()` exposes only its active scene and requires the EDT. Previously active scene services and closed-editor services reject requests. No listener is opened. Trusted host code may invoke `edit(Runnable)`; a transport must expose only typed allowlisted transactions, never this callback.

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

One JSON response is emitted per request on stdout; runtime diagnostics go to stderr. `ok` denotes successful request execution; inspect each test check for pass/fail. Exit status is 1 if any request or test failed, otherwise 0 at EOF. Input lines are limited to 16 KiB. The process has normal local project/file permissions; this is a trusted local CLI, not an authentication/security boundary. No remote MCP, arbitrary script/class/shell execution or terrain commands are exposed.

## Evidence and remaining gates

`EditorAgentServiceTest` exercises real mission/Castle state, stale rejection, shared history, mid-batch/cancellation rollback, saved game load, undoable reopen, corruption rejection, renderer PNG, owner-thread enforcement and CLI failures. `EditorAgentGuiTest` opens the actual Windows editor, edits a House prop, clicks human Undo/Redo, saves/reopens, drives the real recovery chooser/confirmation, verifies native collider parity, captures Castle/House PNGs, rejects the inactive Castle service and rejects closed-editor access. `LevelAuthoringRecoveryTest` proves exact native save recovery and game loading with wrong-scene negative controls. `EditorDocumentStoreTest` halts actual child processes before/after atomic replacement and verifies exact replay, backup, conflict/corruption/path checks, output bounds and writer locking without game files. `EditorCorePortabilityTest` exercises a small unrelated project without game files and proves a renderer-less host cannot fake capture.

Local Java 17 compilation and these tests passed during implementation. GUI captures are under `build/editor-agent-evidence/` and were visually inspected. This is source-editor evidence, not installed-release, physical FPS, full gameplay or G11/MCP acceptance. Follow the recovery checkpoint for exact committed SHA and remote CI results.

G1's stable mission IDs, pinned USER ownership, journaled BSCN persistence and cooperative batch cancellation are implemented and locally tested. Continue G0's provenance/gap audit and G1's broader migrations, Castle persistence, dependency ownership/revision events and incremental deltas before admitting new G2–G12 capabilities. Service existence does not close those gates.
