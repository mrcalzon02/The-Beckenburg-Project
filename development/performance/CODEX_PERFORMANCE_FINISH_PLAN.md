# Codex finish plan — Beckenburg renderer and performance A–G

**Status:** committed execution plan, NOT an implementation-complete claim.
**Repository:** https://github.com/mrcalzon02/The-Beckenburg-Project
**Tracking:** https://github.com/mrcalzon02/The-Beckenburg-Project/issues/7
**Portable process:** portable/MILESTONES_A-G.md and portable/CHECKPOINT_TEMPLATE.md.
**Entry point for a future goal-oriented Codex session:** CODEX_HANDOFF.md.
**Recovery obligations:** ../../development/RECOVERY_PLAN_2026-10-25.md and ../../AGENTS.md.

## Outcome, not activity

Finish the native Java 17 / LibGDX OpenGL 3.3 Beckenburg renderer so the installed Windows game supports a **stable 30 FPS target on a recorded reference Windows GPU**, with responsive first-case loading, retained static House geometry, correct per-region lighting, safe visibility, functioning ghost/equipment depth, and playable case-start-to-return progression. Work must preserve the PSX art/UVs, 22 authored campaign locations, editor, vanilla/software recovery path, single-player and actual TCP multiplayer.

**Project-wide finish definition:** all functional/regression/release tests pass on the exact release commit; real installed Windows hardware captures p50, p95 and p99 frame latency for repeatable case routes at a documented resolution/graphics preset/driver; steady-state p95 frame time is at most 33.3 ms (30 FPS) on the reference machine after a documented warmup; any p99 hitches, load stalls, memory growth and hardware limitations are disclosed and reviewed; one fully interactive first case and a subsequent round complete without a hang. No cosmetic test blocks release unless explicitly requested. If the hardware target is not yet identified, call the hardware acceptance **pending**, not passed.

**Working principle:** fewer reliable passes; not seven simultaneous half-integrations. Optimize only after bottlenecks and dependencies are measured. Keep a recoverable renderer route and original art. Do not create gameplay/content features while closing this performance plan.

## Snapshot to reconcile at session start

Last verified inspection on **2026-10-26**:
- **Production/release source:** df221653471eda5364cf8653fa83cb2aba5d35e9, containing B1/B2a/B2b/B2c and F0/F1 foundations. Exact-SHA GPU 40424723169, full multiplayer 40424723227 and installer 40424723354 all PASSED. Repository main later advanced by documentation-only merge to a3073cde777b87eeb1362e2ad93db9dea6139036, with no renderer behavior change. Always fetch current main before acting.
- Last verified public release: **beckenburg-v0.1.82**, source df221653471eda5364cf8653fa83cb2aba5d35e9, with Windows x64 installer, Java 17 editor kit and SHA256SUMS. Publication confirmed from GitHub release record and exact-SHA complete installer workflow.
- D3 House GPU-light revision diagnostic: PR #26, f4a1b3c271d26dd10956de7e2886d2ad0b1f122a, GPU 40417305528 PASSED. It uploaded 71,616 bytes for 350 authored faces / 746 triangles, retained 2,238 fixture probes across camera movement with zero reupload and revised/reuploaded on a door transition. **Not live case rendering.**
- D4 real-scene immutable-face ownership: PR #28, 2fe3400ae597d7765f1d6883eaea99fa2f67a91c, exact-SHA GPU run 40425466364 PASSED all available jobs. The branch includes a functional fix preserving authored normals in triangle subdivision. **Branch tests passed, but normal-gameplay static buffer ownership has NOT been promoted or hardware-accepted.**
- E0 readback profiling: PR #27, bff0f48708346ea031916dd4e27acc599c0656a0, GPU 40418245781 PASSED. In one 640 x 360 Mesa smoke frame, RGBA and depth were each 921,600 bytes; reported glReadPixels call-wall times were 1.713 ms and 0.191 ms, plus 1.105 ms Java conversion. NOT dedicated hardware GPU time.
- C1–C3 authored portal candidate/ray diagnostics: PRs #15/#17/#22. C2 CSV found 425, 306 and 134 **frustum** faces from basement rooms 10/11 excluded by candidates from respective indoor views. These faces are NOT necessarily depth-visible pixels; depth evidence needed; issue #20. Hard portal culling must remain OFF.
- D0–D2 source lighting revisions, local-region invalidation and RGB probe cache: PRs #13/#14/#19, branch-level acceptance passed. B2d/B2e real House diagnostics are on PRs #12/#18, branch-level acceptance passed. They are not automatically in main.
- Other open branches and PRs may have advanced; this is a pinned historical checkpoint. Read current GitHub state and reconcile before touching files.

Do not blindly cherry-pick a stack or close open PRs on sight. Map each change to the current main tree, identify code already merged, compare parent SHA and actual diff, resolve overlap and avoid duplicate imports. No force push. Preserve documentation plan and evidence even when changing branches.

## Ordered work packages and acceptance gates

Each package produces: a minimal source diff, focused functional evidence, exact commit/remote SHA readback, CI links, raw performance artifacts if relevant, updated CODEX_PROGRESS_LOG.md, and an explicit next dependency. A passing compile is not runtime completion.

### 0. Stabilize the baseline and CI/release [must precede production switching]

1. Fetch actual main and all relevant PRs (including #11–#30 and any newer); record open/merged/abandoned statuses; verify release v0.1.82 is still the most recent verified release or update the record based on current GitHub data.
2. Verify native installed v0.1.82 or a newer fully published and accepted release plus the main recovery option; capture one clean House start and known first-case hang reproduction or lack thereof. Keep BECK-LIVE-012 open until exercised on local hardware.
3. Confirm Java 17, actual Blender exporter, approved assets, real GPU client/driver, diagnostics flush, functional regressions and publisher; investigate failures, do not mute them. A past HotSpot/JIT native access violation in HouseMap.wallBlocked must remain in incident notes if it recurs; preserve hs_err_pid report.
4. Write a benchmark baseline with exact SHA, Windows version, CPU/GPU/driver, RAM, preset, resolution, warmup, camera/player path, CPU p50/p95/p99, GPU timer results if available, memory, GC and case-start latency. Use existing 33-view House route as supplementary CI evidence; it is not physical player navigation.

**Gate 0:** reproducible installed build and genuine baseline; CI/installer integrity established; no pending unknown source collision with running branches. If not, fix this gate first.

### 1. Finish immutable gameplay source ownership (D4 / B)

1. Audit SceneRenderer.scene, levelArchitecture, staticShellChunks and current drawGpuFaces; distinguish authored immutable House shell, enclosure/outdoor assets, static scenery templates, live doors/windows, moving objects, ghost visual effects, portable evidence equipment, TV/CRT and van displays. Retain authored per-vertex normals and material UVs during subdivision; D4 exact-source functional tests must pass on Windows and Mesa.
2. Create an explicit scene-generation/chunk-identity contract for geometry: owning HouseMap, authored revision/editor mutation, spatial room/outdoor chunk, material, generation, vertex count and resource lifetime. Do not use material equality or position equality as sole static ownership.
3. Make nonstatic assets and dynamic lights stay on their existing path. Avoid changing draw order/alpha/depth rules as a side effect of classification.

**Gate 1:** authored House scene partition is verified at van, entrance, interior, upstairs and a newly created second map; no old-scene handles, missing triangles, lost normals or altered gameplay logic. Preserve CPU fallback.

### 2. Deliver **live** resident House geometry plus locally revisioned static lighting (B/D)

1. Promote the D3 PROVEN diagnostic concept into the *normal investigation* GPU path behind a default-off internal feature switch. Prepare real immutable static architecture per map/room/material/chunk; upload on the GL thread; draw by stable positive resource handles. Camera-only movement must not rebuild/re-upload static geometry or its unchanged fixture samples.
2. Use FixtureLighting regional/source/occluder revisions (D0–D2) for lighting uploads; door/fixture transitions refresh affected buffers while topology remains independent. Current D3 combined vertex+RGB buffer reupload on lighting change is a safe interim design; splitting geometry/lighting attributes later is optional only after profiling.
3. Never keep camera-distance light LOD in a supposed world-space stationary sample; preserve opposite wall face normals, LOS/shadow sides, ceiling occlusion, transparent glass and no wall bleed. Keep portable flashlight/UV, sensor overlays, ghost pulses, live CRT/TV, NPCs, projectiles and interactions dynamically updated each intended game tick.
4. Bound residency memory (scene generation disposal, map changes, resize/context loss, editor revisions and optionally LRU/eviction). Prevent high-workset cache thrash and stale handles.
5. Differential-test GPU legacy vs new path on House from exterior, approach, room, upstairs, moving doors, light toggles, in/out case, multiplayer ghost reveal; compare functional geometry/depth, not style-driven visual snapshots. Capture profiling with exactly the same route/hardware.

**Gate 2:** installed House investigation exercises *real* resident chunks in production; camera-only viewpoint changes show zero static uploads and zero static rebuilds after warmup; light/door changes have correct revisions; full evidence/hunts/ritual gameplay stays functional; no newly leaking light. If parity fails, turn off feature switch and repair, never hide the defect by weakening a test.

### 3. Conservative depth-validated visibility (C)

1. Keep current chunk frustum pruning. Use C2/C3 diagnostic CSV as **candidates for investigation**, not proof of pixel visibility; basement rooms 10/11 require floor/stair/door/window depth tests.
2. Gather real per-pixel/depth-buffer visibility or a validated occluder reference across those viewpoints, plus actual physical paths. Test open/closed doors, windows, multi-floor stairwell, exterior sightlines, transparent surfaces and editor changes.
3. Implement portal/frustum rejection only where proven safe; when uncertain, draw the geometry. Never use rendered visibility to change host-authoritative ghost state, evidence or network messages.

**Gate 3:** no visible triangle, ghost silhouette, equipment, clue or collision/interaction target disappears incorrectly; perf savings measured; no hard culling on ambiguous room candidates.

### 4. Composition and GPU/CPU synchronization (E)

1. Use E0's color/depth transfer timing and byte records; add driver/hardware measurements and GPU timer queries to distinguish GPU execution from GL call-wall stall.
2. Keep depth-tested ghost sprites, generic-vs-signature reveal rules, equipment/DOTS/UV, native PSX text, all UI layers and color/tint blending exactly correct. Migrate one layer at a time to GPU-native composition where justified.
3. Test any asynchronous PBO/fence path against scene shutdown, skipped frames, resize, device loss, fallback rendering, and target hardware. Do not eliminate CPU depth before its users are migrated or proven independent.

**Gate 4:** measured lower synchronization/transfer overhead under matched conditions; correct depth and interaction; no unstated latency, stale frames or UI failures. Leave legacy compositor selectable until production acceptance.

### 5. Deterministic first-case preparation, warmup and flyover (F)

1. F0/F1 first-preview timing and bounded playback gate already exist on main. Extend the actual case pipeline: selected map -> approved assets decoded -> immutable chunks built -> fixture/revisions prepared -> GL uploads/shaders/FBO warm -> first complete playable frame -> scripted overflight into van -> responsive hunter control.
2. Readiness comes from actual completion signals, not sleep delays. Include cancellation, progress reporting, safe return to menu, crash diagnostics flushed to disk, scene/lifecycle disposal and software fallback.
3. Reproduce BECK-LIVE-012 first-case hang with startup-stage and per-stage timings. Fix its measured root cause; avoid treating a 7.5-second cinematic or a successful loading screen as proof of gameplay readiness.

**Gate 5:** two consecutive playable launches/cases including first case under intended presets, skip and normal flyover, cold/warm cache and device reset paths; no forced Task Manager termination or invalid preview flash/resize; stage logs survive a forced kill.

### 6. Map and installed-product acceptance (G)

1. Prove House on real Windows target hardware first, then Castle and remaining 22-location campaign and free-play variants; editor open/edit/save/reload; van/hunter navigation; door/fixture/equipment/sensor/ritual; reveal-hidden ghost sprite distinction; solo and packaged real TCP host+guest; updater/reinstall.
2. Profile the same route and machine before/after; report p50, p95, p99 frame times, sustained FPS, CPU/GPU timing separation, GPU VBO upload/readback bytes, startup latency, heap/native GPU memory and GC. Preserve source/build/driver/asset hashes and inspect representative captures for actual geometric regressions.
3. Do not gate CI on unrequested cosmetic image diffs or software Mesa FPS. **Stable 30 FPS** is a real-local-Windows-machine acceptance objective with documented test conditions, not a claim from hosted software GL.
4. Run the current exact-SHA GPU workflow, focused tests, full BeckenburgRegressionSuite, build.cmd e2e, packaged Windows GUI and multiplayer, and installer release workflow. Publish only from a revision that passes each applicable functional/release gate; verify remote tag/asset/checksum and actual installed executable.

**Gate 6 / DONE:** owner can install the native game and repeatedly complete the entire solo/multiplayer investigation loop on supported hardware with no hard hangs; documented real hardware steady-state p95 frame time <= 33.3 ms at reference resolution/preset and no undisclosed p99 stalls or regressions; map/editor coverage is recorded per site; published release SHA exactly matches accepted code. Mark A–G complete only after this evidence.

## Measurement contract

- Treat scripted CI House waypoints, Mesa software GL, unit tests, screenshot visualizations, Windows real-GPU local profiling and installed-case E2E as **distinct** evidence tiers.
- Save per-stage CPU / lighting / draw / GL issue / color readback / depth readback / CPU conversion / upload bytes / reused bytes / VRAM and CPU memory / native crashes / timestamps. Use matched hardware and route, ≥3 representative repeated runs when feasible, after warmup.
- Never present the 111–147 ms software Mesa frame data as hardware FPS, and never cite a green test from a nearby SHA as evidence of the current source.
- Keep tests functional and diagnostic: missing visual geometry, broken input, build, scene changes, resource invalidation, depth, load, network and lifecycle failures may block. Cosmetic differences do not block unless requested.
- No screenshot-only claim of gameplay correctness; the actual case must run from menu to result and return.

## Branching, failure and release discipline

- Begin each stage from the latest verified main; compare all stacked PRs and cherry-pick only compatible changes. Keep the feature switch/previous path until accepted; do not merge the full D stack blindly. Use atomic commits per proven behavior, not broad speculative passes.
- Never force-push or bypass failing functional diagnostics. Remote readback, exact-SHA CI run links and rollback steps required for every stage. Preserve failed-run artifacts; rerun only when the failure is genuinely infrastructure, not to conceal regressions.
- Update CODEX_PROGRESS_LOG.md, issue #7 and any affected Beckenburg-specific ledger after every bounded verified step. Use the portable method as *documentation* for other repositories; do not autonomously edit ReverieVR, Malefic Veil, Nutbuster, Rupee Rush or Tech-Priests.
- If local Windows GPU access is unavailable, finish source/CI work and report **hardware acceptance pending**; do not claim completion, simulate results or change the target.
- **Do not autonomously create or disable scheduled tasks.** This handoff authorizes future Codex implementation when the user starts that session, not background execution now.

## Compact next-action sequence for Codex

1. Snapshot current main, open PRs and exact CI results; update the run log.
2. Validate/repair D4 normals and real gameplay identity; reconcile D0–D3 into a single integration branch.
3. Add feature-gated normal-gameplay resident House draw; protect dynamic effects and CPU fallback.
4. Test door/fixture/ghost/van/level/map transitions; measure geometry/lighting savings.
5. Complete C depth, E compositor, F warmup in measured order; finish G on Windows hardware and publish an accepted release.

This file is the **finish plan**, not permission to mark all tasks done simply because the document exists.
