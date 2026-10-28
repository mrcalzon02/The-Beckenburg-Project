# Codex A–G execution checkpoint log

This record is the per-project progress ledger for the FINISH plan. Update **after each verified commit**; never backfill "passed" from an unrelated SHA.

## Local preservation/unification entry (2026-10-26)

### Unified baseline accepted; live House source connected

PR #32 merged to remote main `1ba745cd3e0a1a9acfc5b32fe857866c6d05dfe9` after expanded source `17a8594f` passed exact-head full acceptance [40436670628](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40436670628), GPU [40436667806](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40436667806), and PR full acceptance. Local complete manifest: **253 passed / 0 failed / 0 skipped**. This saves/unifies the three local worktrees plus user-requested PRs #3/#29 and fatal-report retention #11. New main installer checks are independent and were not cancelled.

Reconciled existing tested D3/D4 code at `07774041`; source ownership/revision/probe tests pass against the unified game. A native app-image (not installed product acceptance) reached House: initial scene 1,796 ms; hunter handoff 12,322 ms. User's House-facing turn reproduced a 2,357 ms frame (`RENDER ... lighting_raster_ms=2340`). This is the actual live bottleneck; unification did not fix it. Desktop control was stopped by physical Escape and has not been resumed.

Live candidate now connects immutable House architecture/scenery to normal GPU draws behind a default-off internal switch. Functional contract: 209,244 source faces / 789 chunks / 665,532 vertices / 21,297,024 packed bytes, with **zero static rebuilds, probe refreshes or uploads on camera-only turns after warmup**; dropped upload, fixture/door, resize, UV fallback, scene and new-map checks pass. Separately retained stationary visibility avoids retracing rays on source switches. Actual GPU timing/depth and installed case gates remain pending. See `LIVE_HOUSE_RESIDENCY.md`; no FPS or A–G completion claim.

Expanded by explicit user direction to include feature PRs #3/#29. Earlier unified source `d2c644a0` passed exact-head full acceptance [40434726259](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40434726259) and GPU [40434626216](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40434626216); newer feature integration requires its own checks. Reconciled Quartermaster `e988eb01`, hunter/companion `f3c86035`, fatal JVM log retention `84cac58a`, native teardown `417980e6`. The latter passed three real RTX 5070 diagnostics with process exit zero, versus baseline `f6793bda` exit 0xC0000409. Their cold-route CPU p50/p95 remain 108–123 / 2796–2852 ms, **not accepted interactive FPS**. Fixed a reproduced hunter-branch ghost-resume failure without weakening recovery tests. Detailed code/evidence/status: `development/integration/2026-10-26/` and `hardware/2026-10-26/`. Remaining: expanded exact-SHA full acceptance, native case/hardware baseline, then work packages 0/1. No A–G milestone marked complete.

User requested saving/unifying existing work before beginning performance implementation. Inspected current main `f6793bda4f1c6c821fe66e49050b076b01cce6c0`, required recovery plans and issues #6/#7/#20. Three local worktrees were dirty. Pushed and read back unverified preservation checkpoints `d07d7450`, `b7b193f4`, `06341f84`; retained exact details and original hashes under `development/integration/2026-10-26/`. Unified current main at `5956e173`, then preserved native map sources at `4f92b65d`; mission-board reconciliation and full CI are next. Local Java 17/OpenGL compilation and focused functional checks passed as documented there. Hardware detected: RTX 5070 / 616.64; **installed hardware acceptance PENDING**. No A–G gate closed and no release published by this session.

## Initial handoff checkpoint (2026-10-26)

- Mission status: PLAN COMMITTED; **A–G NOT COMPLETE**. Implementation not yet handed to an executing local Codex agent.
- Renderer release source: df221653471eda5364cf8653fa83cb2aba5d35e9 (F0/F1 merged); repository main subsequently moved to documentation-only merge a3073cde777b87eeb1362e2ad93db9dea6139036, with no renderer changes. Check fresh SHA at execution.
- Last verified public installer: **beckenburg-v0.1.82**, source df221653471eda5364cf8653fa83cb2aba5d35e9, Windows x64 EXE + Java 17 editor kit + SHA256SUMS.
- Exact release-source GPU 40424723169, full multiplayer 40424723227 and installer 40424723354: **ALL SUCCESS**. GitHub release v0.1.82 verified.
- D3 real House resident fixture test 40417305528: SUCCESS, diagnostic only.
- E0 readback profiling 40418245781: SUCCESS, measurement only.
- D4 normal-preservation and real-gameplay static ownership candidate: PR #28 SHA 2fe3400ae597d7765f1d6883eaea99fa2f67a91c; exact GPU 40425466364: **SUCCESS**. Production source switch still NOT implemented/accepted.
- C basement frustum/portal mismatch still unresolved depth-wise; issue #20. Hard portal occlusion disabled.
- Outstanding primary dependency: D4 source ownership -> live resident/static/dynamic scene split -> correct lighting/depth and hardware benchmarking.

## Verified pre-handoff addendum (2026-10-26)

- Original plan source f7729eab4d10005cca14229b60cca640c48c5481 saved as draft PR #30 and linked from milestone #7 and goal issue #31. Project evidence PR #21 was merged at a3073cde777b87eeb1362e2ad93db9dea6139036, preserving C2 depth findings and the Beckenburg-only ledger.
- Published release v0.1.82 and exact-source GPU/multiplayer/installer SUCCESS confirmed. No need to wait on the former release workflows to preserve the Codex documentation on main.
- D4 exact-source GPU suite SUCCESS confirms authored-face identity and subdivision normal test, but does not establish actual gameplay GPU residency.
- **Do not infer that a Codex run has started.** This checkpoint only prepares future goal-oriented work.

## Every next checkpoint

### Checkpoint [add sequential identifier]

- UTC date/time:
- Task/work package:
- User-authorized scope:
- Starting main SHA / branch:
- New source SHA and remote readback:
- Files changed and fallback switch:
- Functional test commands, exact CI run links and outcome:
- Benchmark environment: OS / CPU / GPU / driver / resolution / preset:
- Baseline vs change: p50 / p95 / p99, GL transfer bytes, case-start milliseconds, memory:
- Genuine hardware result or reason pending:
- Gameplay/evidence/network/ghost/van/editor regressions checked:
- Defect or blocker and its reproducible evidence:
- Was this deployed to main? Was it released? Exact source SHA and checksum:
- Rollback and next prerequisite:
- Gate status: NOT STARTED / IMPLEMENTED / CODE PASSED / CI PASSED / HARDWARE ACCEPTED / RELEASED.

Do not delete previous checkpoints or convert planned statuses into successes. Link new regressions to the authoritative defect ledger and issue #7.

## Live House measured checkpoint (2026-10-27 03:11 UTC)

Source `performance/live-house-residency`, pushed/read back `6e0c698599e66553adf54f2de4d26750e6c81d9d`; draft [PR #33](https://github.com/mrcalzon02/The-Beckenburg-Project/pull/33). Concurrent main PSX art docs/tool retained. RTX 5070 / 616.64 / 712x200 / Retro 200p measured twice each: legacy frame p95 108.523-109.803 ms; resident fixed 35.858-36.077 ms; live fixture clock 36.075-36.729 ms. First exposed yard 3880-3970 ->600-624 ms. **Improved, not accepted 30 FPS.** Zero camera-only static uploads; retained GL vertex capacity 32,044,800 bytes. Depth parity: zero original pixels missing/farther beyond 5 cm at van/interior/upstairs. Cold prewarm about five seconds. Raw evidence under hardware/2026-10-26/live-house-6e0c6985; cargo-obscured first route excluded and labeled separately.

Exact-head GPU [40442270881](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40442270881) and full [40442306416](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40442306416) pending; full local regression running. DEV-131 tracks remaining stalls. Next task: stationary door-ray dependencies, then lifecycle and installed full case. Desktop input remains stopped after user Escape. No new release or A–G completion claim.

## Localized stationary door dependencies (2026-10-27 03:18 UTC)

Source `6e0c6985` completed GPU [40442270881](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40442270881) successfully, including new real live-path Mesa/depth check. Complete local mandatory regression: **255 passed / 0 failed / 0 skipped**; retained log under this checkpoint. Newly verified increment tracks source intensity separately from each stationary ray's intersected, conservative door/window volume revisions. Every actual House door matches fresh uncached visibility/RGB; **585 localized rays vs 7860 fresh rays**, maximum 35/59 probes refreshed. Existing source-switch, normals, physical lighting, optical evidence, D3/D4 and actual live-scene tests pass. No geometry rebuild or unrelated RGB upload on a door transition. Emissive changes now participate in packed RGB comparison even when ordinary irradiance is unchanged.

Fallback remains default-off; legacy/software/global revisions remain intact. Exact new-source GPU/full CI and hardware door-transition profiling next. Full installed investigation-to-Castle acceptance remains pending; do not advance C/E/G or claim 30 FPS based on this change.

## Explicit GPU viewport lifetime checkpoint (2026-10-27 03:28 UTC)

Localized doors committed/pushed/read back `b88f4405`; exact-head full [40443415880](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40443415880) and GPU [40443414814](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40443414814) pending. Source `600c0456` adds exhaustive pixel-equivalent quantization lookup and stage/event hardware profiling. Raw repeat runs retained under hardware/2026-10-26/live-house-600c0456. Resident CPU p50 now about 12 ms; p95 completed frame still 34.8-36.4 ms fixed /38.0-51.2 ms live clock /53.5-55.3 ms events. No 30 FPS claim.

Next verified code increment: explicit nonblocking release on case, Castle, ghost, opening, editor and water-preview close, with deletion only on owning GL thread. Disposal epoch forces safe restoration even when a closed preview clears latest framebuffer. Contract test covers inactive close preserving another viewport, active close, restoration and prohibition on closed resubmission. Java17 GPU compilation and existing identity/opening tests passed; actual GPU lifecycle test added, execution pending. Worker teardown remains unchanged.

Existing full acceptance workflow cancelled earlier superseded same-branch runs automatically; no manual cancellation was issued. Concurrency now retains each exact SHA/event with cancel-in-progress false, preserving checks during subsequent recovery checkpoints. No installer workflow was edited/cancelled. Full installed native case and A-G acceptance remain pending.

## Preserved continuation checkpoint (2026-10-27 06:49 UTC)

The agreed wall-time window has elapsed; preserve this candidate for later, with A-G still active/incomplete. Verified remote `fe0df50a8a545c98434da606518b0f97849f10b3` passed full multiplayer/release-manifest [40444429571](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40444429571), GPU [40444425588](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40444425588), and gallery [40444429450](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40444429450). Actual RTX run on runtime-equivalent `8b93e93b` passed two-way depth plus live GL close/release: closing an inactive preview preserves the House; closing the active viewport releases resident vertex capacity while backend stays ready. Source 600c0456 also passed both full/GPU CI. All remain default-off draft PR #33, not installed or released.

Final incremental cache retains exact world-position fixture visibility for dynamic geometry on the eligible live House path. It uses a bounded 100,000-probe LRU and separately revised source/portal dependencies; UV/DOTS/portable-light fallback still selects legacy sampling. Targeted all-door fresh-occlusion equivalence, stable dynamic-ray reuse, switch/normals, physical lighting, receiving-side joins and optical-evidence tests pass; Java17 GPU package succeeds. Latest exact-source hardware/full CI still required, so do not inherit earlier green checks for this increment.

Next action: inspect current HEAD/remote and CI; run `--live-house-profile` with resident=true, liveClock/events/depthParity flags on this exact source, preserving raw files. Remaining first-view cold work and worker scheduling are measurable: libGDX 1.14.2 bytecode confirms idle Thread.sleep(1000/idleFPS) and foreground Sync at configured 60 Hz; no scheduling change was made in this window. Then explicitly re-enable desktop input with the user (prior physical Escape stop still applies), build an isolated native candidate and complete real House investigation/return with gameplay/multiplayer/editor checks. Only after House correctness/acceptance advance C/E/F/G. Do not enable globally or publish a release on renderer timing alone.

## Final preserved checkpoint - experiment rejected (2026-10-27 06:54 UTC)

Actual RTX event/depth/lifetime run rejected bcdb6ab9 for performance: p95 frame 71.474 ms, p99 413.814 ms; first-frame scalar rays 157362. Depth and GL lifecycle passed. DEV-132 records the regression. Runtime/test/workflow sources restored **exactly** to verified fe0df50a (git diff --quiet over src, gpu-client, .github, tools and regression manifest exit0). Rejected source remains in pushed history; raw evidence retained. This supersedes the preceding pending dynamic-cache proposal.

Final deliverable: default-off House GPU residency candidate in draft PR33, pushed recoverable checkpoint. Verified source fe0df50a full/GPU CI SUCCESS (40444429571/40444425588); source 8b93e93b actual RTX depth/lifetime PASS. Matched House p95 108.5-109.8 ->35.9-36.1ms; exposed-yard first frame 3880-3970 ->600-624ms. Camera-only static uploads0, GL vertex capacity32044800bytes. Still not stable30FPS, installed investigation-to-Castle, context recreation, optical-tool performance or A-G completion; no candidate release. New final-SHA CI remains independent.

Next session: fetch and inspect main/PR33/current exact-source checks; preserve concurrent work. Diagnose remaining dynamic first-view/source-change cost and worker60Hz/idle scheduling. Obtain explicit desktop-input re-enable after prior physical Escape before native play. Complete House installed investigation/return and gameplay/editor/multiplayer gates before C/E/F/G. Wall-time window exhausted; preserve for later.

## Queued acceptance completed (2026-10-26 23:16 AKDT)

User requested finishing queued work after the time-window checkpoint. Exact source `e783fdf73f9fcd95d014fd8e6e3955404cefefde`: [head full 40464099189](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40464099189), [PR full 40464015857](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40464015857), [GPU 40464010267](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40464010267) and [gallery 40464015834](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40464015834): **ALL SUCCESS**. Complete CI manifest **257 passed /0 failed /0 skipped**. Exact-head packaged UI executed real House dispatch, skippable flyover and three advancing gameplay frames; actual Windows app-image reached initialized menu. These are CI startup/menu gates, not a user's installed full investigation/return loop.

Downloaded GPU artifact source matches e783fdf7. Its120 camera frames show1 static build,789 packed chunks and134714 probe computations unchanged across the sweep,0 resident upload bytes, maximum32044800 allocated resident vertex bytes. Two-way depth:0 farther/0 nearer mismatches in each van/interior/upstairs view. Actual GL active/inactive scene lifetime assertion passed. This is Mesa/llvmpipe functional evidence; use earlier RTX records for hardware timings. Durable raw CSV, depth/summary, artifact hashes and selected actual acceptance log lines are under `ci/2026-10-26/e783fdf7/`.

No new renderer code in this closeout; retained runtime/workflows/tests still equal verified e783fdf7/fe0df50a. Main separately remains eef747d0; its existing installer/GPU workflows passed and public v0.1.84 targets eef747d0, **not this default-off residency candidate**. PR33 remains draft. No new release/merge, no desktop input after Escape, no A-G completion or installed30FPS claim. Next work remains the saved dynamic first-view/source-change and worker scheduling investigation followed by explicitly re-enabled native acceptance.

## Concurrent-main reconciliation before closeout (2026-10-27 09:24 AKDT)

User requested finishing the queued work. Saved head8594d1a5 passed full40466276341 and gallery40466276306; its runtime e783fdf7 passed GPU40464010267 and exact-head full40464099189. Fresh remote main is now b6f6d697a29175e197a7958a0f89804f9514d2d3 with independently committed case-trace lifecycle and DEV124 metal refinement. Main full40513590160/GPU40516359221/installer40513590094 all passed. PR33 is now conflicting against that concurrent work. Reconcile the newer changes without dropping House residency, resource teardown, regression entries or either capture/quality ledger; then verify the combined exact source. Earlier green checks do not establish the new combined checkpoint. No feature enablement or hardware acceptance claim.

Reconciliation result: retained both caseStop and scene.close, newer first-real-frame/intro/pause trace semantics, DEV124 material refinement, both sets of captures and all259 mandatory manifest entries (including the parents' pre-existing repeated audio test). Gallery rebuild/check passes1001PNG images. Fresh isolated Java17 tests pass for trace lifecycle, metal/atlas, live House residency, door dependencies and exact PSX quantization.

DEV133 reproduced a separate source-provenance bug: Maven copied .class files from build/classes as resources and could skip compilation of restored source. Added compiled-class exclusion and an obsolete .class CI sentinel. Clean local GPU package passed; sentinel and rejected dynamic-probe API are absent. Historical local JARs after source restoration must be verified by clean build; prior fresh exact-SHA CI remains independent. Combined checkpoint CI/hardware results are not inherited from either parent. Default-off/draft and installed30FPS/full-loop gates stay open.

## Reconciled source hardware/source proof (2026-10-27 09:34 AKDT)

Pushed/read back combined source `fe217a091e8bd31a4ba19ff5eb8db66fcf47ce19`; PR33 mergeable after incorporating main b6f6d697. Fresh **clean** Java17 GPU package SHA256 `40dbbbd90d51f2e82506da350cf600d2975c77c5aef6c56991aa8dbb39c132bd`; rejected API/sentinel absent. Actual idle RTX5070/616.64,712x200,FOV93,Classic PS1: one120-frame fixed-clock diagnostic, cold5391.170ms, CPU p50/p95 12.096/19.479ms, completed-frame p50/p95/p99 19.839/36.411/203.643ms. Camera-only static uploaded bytes0; source build/chunk data stable. Two-way depth and actual GL active/inactive lifetime assertions PASS. No concurrent local test/build workload. Raw logs,CSV,PNGs/hash metadata under hardware/2026-10-27/reconciled-fe217a09; this is not installed30FPS or full-case acceptance.

Combined exact-source GPU [40551454110](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40551454110), head full [40551454146](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40551454146), PR full [40551461042](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40551461042) are queued/running at this evidence checkpoint. Record their terminal results in issue7/PR33 without treating earlier parent green checks as combined acceptance. Current follow-up only preserves hardware evidence/notes; no additional runtime changes. Candidate remains default-off/draft; no release or hardware gate closed.

## Authorized House continuation window (2026-10-27 10:11-11:11 AKDT)

User explicitly authorized another60minutes. Starting clean head75868f79 and mainb6f6d697; final259/259 full40552034659, GPU40552671419 and gallery40552034611 are SUCCESS. Previous queue is closed. Preserve default-off feature and prior Escape desktop-input stop. Work from fixed/events baselines on the current clean-built Jar, then verify worker latency, unchanged legacy visibility semantics and concrete GPU ownership/global-lighting correctness before claiming improvement. No A-G completion or installed/hardware gate implied by this window.

Window implementation checkpoint: DEV134 fixes stale resident ACK/late viewport eviction with immutable recovery references and deterministic before/after-epoch tests. DEV135 restores the same global environment/weather multiplier used by software/water to GPU fixture/precomputed-emission/flashlight energy; brightness changes remain uniforms, not vertex uploads. Current-source Java17 contract/global-light/recovery/parity tests and clean GPU package pass. Actual GL test confirms scaled pixels/depth and evicted-handle restoration; synthetic emission attributes complement existing real-source tests.

Optional A/B controls remain default-off: beckenburg.gpu.lowLatency removes foreground cap and wakes the owning noncontinuous worker on requests; beckenburg.fixture.retainVisibility preserves old scalar quantization/representatives, exact normal keys, RGB math and distance LOD while retaining source-to-surface visibility across source/portal revisions. Exact-key dynamic cache DEV132 was not recreated. New legacy parity test and nine prior lighting suites pass off/on. Original-JAR baseline retained at build/window-60-baseline; fixed p95 34.780/36.972ms, live-event p95 57.951/71.124ms. Controlled new same-JAR scripted30Hz A/B is next; these diagnostic numbers are not installed30FPS acceptance.

## Continuation checkpoint verified and preserved (2026-10-27 17:52 AKDT)

Source branch performance/live-house-residency, pushed/read back2e85049f54a4a0a8900169c551909f68eee55a1d. DEV134/135 functional and final RTX GL assertions PASS. [GPU40562195922](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40562195922) checks out exact2e85049f. [Full40562204969](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40562204969) checks out merge2f44c8e35b7d29be66e6cec22b9b55bcca5709dc, with independently verified tree dd3d2a33dbdc6f4984d024a19fd26c0ad99f1b2a IDENTICAL to2e85049f:262 passed/0failed/0skipped; real packaged House startup/3 frames,multiplayer UI,native app-image menu PASS. [Source gallery40562204981](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40562204981) SUCCESS. Do not call the full run's merge SHA the source SHA.

Final clean JAR SHA25671b581b19e3f43d0db186641b2a3b330e6dc11d3ec2b2f2d2374ef52c9f9770b. Same-JAR RTX5070/616.64,712x200,FOV93,Classic PS1,two repeats per configuration: fixed worker p95 35.799-35.968ms -> opt-in wake31.732-32.329ms; zero camera-only static transfers. Scripted30Hz event clock holds light state identical across comparisons. With wake worker, legacy visibility retention reduces door15 rays67432->23617 and frame671-766->201-215ms; event-route p99 615.647-656.342->233.612-239.767ms. Eight saved cache-off/on RGB pairs are pixel-identical; static event transfers match7083648bytes. Initial scalar rays remain44434 (DEV132 not recreated). Event p95 mixed50.7-59.1ms; first exposed House still641-650ms with retained visibility, cold5.3s. This remains short of stable30FPS/full investigation acceptance.

Raw historical baselines, ten same-JAR runs, exploratory and failed sandbox attempt separately labeled, flags/checksums, CSV/log/PNG evidence and exact reproduction script are in hardware/2026-10-27/house-continuation-2e85049f. Both final wake repeats pass actual GL depth/lifetime/global fixture-emission-flashlight scaling/evicted-handle recovery and clean exit0. Synthetic emission attributes complement real-source tests; no independent new emission-source claim. Complete process-memory/GC/full-case hardware proof remains pending.

Concurrent main is now7f1515d03817e771d05604c136cbe2f5ed990d22 (desktop art and additive non-culling C3 diagnostic). Read-only merge preview has no conflicts; keep concurrent commits intact. This evidence-only follow-up does not incorporate or test that newer main. Before promotion, reconcile and run combined exact-source checks. Existing public v0.1.87 targetsdf148978,NOT this candidate; no release published here, no candidate release checksum.

Authorized60minute window elapsed; finish queued verification/evidence only. No desktop input after prior physical Escape. A-G remains OPEN, draft PR33 and all optimization flags default-off. Next exact action: in a newly authorized work window reconcile7f1515d0, diagnose remaining first-view dynamic work and repeated multi-camera uploads, build isolated native candidate, obtain explicit desktop-input re-enable, then physically complete House investigation/return plus gameplay/editor/multiplayer acceptance before C/E/F/G promotion. Preserve source2e85049f fallback/checksums and turn off optional gates if an installed regression appears.
