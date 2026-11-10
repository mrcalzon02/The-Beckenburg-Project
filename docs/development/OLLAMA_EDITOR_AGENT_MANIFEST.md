# Beckenburg Ollama → Native Level Editor Agent Manifest

This is a manifest of operations *implemented by the actual Java level editor* and guardrails for the single-edit Ollama test. The model is a proposal generator, NOT a shell agent and NOT an independent scene author. The existing EditorAgentCli, EditorAgentProtocol, EditorAgentService, LevelAuthoringSession, HouseMap and SceneRenderer remain authoritative.

## Target and current capability
- Current pilot: Ravenshade School (SCHOOL), scene identity beckenburg/school. It is an unfinished built-in mission supported by HouseMap.Site and LevelPlans.buildSource.
- Its real native document lives under an isolated CI user home at .beckenburg/editor/levels/school.bscn. Its current editor agent operation is move-prop only.
- Requested follow-on: Morrow Point Lightstation (WLD-14), in development/levels/layouts/wld-14/layout.json and development/levels/rebuild/wld-14. WLD-14 is not a HouseMap.Site and cannot currently be opened in EditorAgentCli as a native mission. No Lighthouse edit or gameplay acceptance is claimed.
- Castle uses a separate CastleEditorAgent with a different operation list; MESH handles native BTOP materials and is not a mission editor.

## Launch, transport and permissions
Build native Java 17 classes with build.cmd package on Windows. Invoke: java -Djava.awt.headless=true -cp "build/classes;build/test" org.beckenburg.EditorAgentCli SCHOOL <temporary-home>. Each UTF-8 stdin line is tab-delimited, yielding a JSON line on stdout. Success is {"ok":true,"result":...}; refusals have ok:false and an error code. A successful test command can still return failed individual checks. Observe scene/revision first, use the exact current revision on all other commands. Model input is untrusted; only the controller can call native protocol operations. No inbound HTTP or TCP port, no execution of scripts proposed by Ollama.

## Full native command discovery and use

| Native command | Documented behavior |
|---|---|
| revision | Read current sceneId and revision |
| summary | Read current revision, capabilities, operations and test IDs |
| inspect | Full authoritative state: document, props, stable IDs, hierarchy, test names |
| hierarchy TAB revision TAB offset TAB limit | Paginated native hierarchy, 1-64 entries |
| node TAB revision TAB id | Inspect one node by exact ID |
| nodes TAB revision TAB id... | Batch-read up to 64 exact node IDs |
| properties TAB revision TAB id | Read native mesh, room, collision footprint, transform and support |
| assets TAB revision TAB offset TAB limit | Paginated approved native editor palette |
| changes TAB revision TAB offset TAB limit | Last committed scene edit metadata |
| walkability TAB revision TAB x TAB y TAB z | Call genuine HouseMap standing-body collision |
| focus TAB revision TAB id | Aim real editor renderer at selected native prop, no scene mutation |
| capture TAB revision TAB absolute-png | Render genuine SceneRenderer.drawPractice PNG, not simulated artwork |
| test TAB revision TAB mission-runtime-parity | Check current authoring props and native colliders against runtime |
| test TAB revision TAB mission-open-routes | Check physical open-door swept traversal |
| edit TAB revision TAB move-prop TAB id=ID TAB x=X TAB y=Y TAB z=Z | Validated single mission transform in native shared undo history |
| batch TAB revision TAB operation... | Atomic 1-64 commands; this pilot allows one only |
| undo / redo TAB revision | Native shared edit history, revision bound |
| save TAB revision | Validate then atomically write native BSCN |
| reopen TAB revision | Load saved native BSCN and advance revision |
| recover TAB revision | Explicit previous saved version; excluded from this pilot |
| verify-position TAB revision TAB id TAB x TAB y TAB z | Verify exact node coordinates, commit evidence and on-disk state |
| actions TAB revision TAB after TAB limit | Bounded paginated recent editor actions |

A mutable action may be prefixed with once-v1 TAB positive-sequence TAB exact-original-command for idempotency; retries must use the same sequence and identical request. Scene changes invalidate previously observed revisions. Never infer saving from a successful move.

## Ollama planning contract
- API: local POST http://127.0.0.1:11434/api/generate. format=json, stream=false, temperature=0, num_predict=180, num_ctx=4096.
- Initial bounded model: qwen2.5:0.5b. Do not expose this endpoint publicly.
- Input includes this manifest, genuine inspect scene identity/revision, baseline route/test checks and finite candidate list. The controller constructs proposals only for floor/surface-mounted loose props; excludes walls, windows, doors, fixtures, pipes, light fittings, rails and roofs. Allowed movement is exactly 0.125m along one horizontal axis, unchanged y/room/scale/yaw. One transaction only.
- Model response is exactly a JSON object with choice_id matching an offered candidate and reason of 5-240 characters. Example: {"choice_id":"C01","reason":"Shift the loose chair slightly while preserving access."}. The model may not invent tools, command text, file paths, IDs, coordinates, scripts, shell instructions or source patches.
- Invalid JSON, extra keys, unknown ID, unsafe movement or surprising scene identity fails closed.

## Required workflow and evidence
1. Checkout commit; build actual game and editor classes; controller self-test; start local Ollama and pull the small model with bounded timeouts.
2. Use a fresh temporary project home (never the runner user's real map documents). Inspect actual SCHOOL state and run native mission-runtime-parity plus mission-open-routes before edits. Existing route failures are logged, not silently converted to passes.
3. Give the inspected candidates to Ollama; validate its selection. Focus chosen native ID, take before.png from the real renderer.
4. Execute exactly one move-prop through native EditorAgentCli with once-v1 sequence=1. Verify revision increase, precise coordinates, committed membership and no unrelated native props changing.
5. Run native parity and open-routes again; fail on parity errors or new/extra route failure codes. Architectural quality is not inferred from a passing route test.
6. Save native BSCN, reopen on same authoritative editor, compare full native state and require verify-position diskState=MATCHES. Capture after.png. Upload native BSCN, images, compact report and logs in existing editor-agent-evidence artifact.
7. No auto-merge, runtime promotion, physical GPU/FPS claims, map-completion claims or remote branch write from the Action. If the run fails, report BLOCKED_UNVERIFIED and preserve the unmodified source repository.

## Lightstation integration prerequisite
To run this on WLD-14, first expose the Lighthouse's actual Blender/GLB/HCS2 authored scene through a native editor project with bounded terrain/structure operations, shared render/physics/room/navigation/portal/service/van coordinates, stairs/lantern checks, scene saves and recoveries. Never substitute School or merely edit display JSON and call it a Lighthouse editor run.

## CI hygiene
Extend existing .github/workflows/editor-agent-service.yml only; keep its one Windows job and its normal acceptance behavior. Trigger an explicit workflow_dispatch input run_ollama_pilot or a single checkpoint commit containing [ollama-editor-pilot]. GitHub-hosted Windows avoids relying on the self-hosted runner registered elsewhere. No extra workflows, background agents or automatic CI commits.
