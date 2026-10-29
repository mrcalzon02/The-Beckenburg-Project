# Codex A–G execution checkpoint log

This record is the per-project progress ledger for the FINISH plan. Update **after each verified commit**; never backfill "passed" from an unrelated SHA.

## Scheduled follow-up checkpoint (2026-10-29)

- Source checkpoint: `012989195cca567fee3e56f0c7c0d2092914e3ca` on `codex/perf-map-followup-20261029`, based on the active all-map retained-world performance revision `f82fb0857ec7b5ba984c9d33e034cda3cd904dbf`.
- Change: `SceneRenderer` now determines active portable illumination once per renderer phase using an allocation-free loop. The unchanged safety rule still routes all scenery, shell, and auxiliary retained-world geometry through the transient path whenever any portable light is on; otherwise high-instance outdoor/map views no longer create a stream and rescan the same light map for every visible instance.
- Verification: `git diff --check` passed and remote branch readback matched `01298919`. The scheduler command window did not produce a complete local Java build/test result, so **no build, full-regression, CI, FPS, or hardware acceptance is claimed** for this SHA.
- Next: run the exact-SHA renderer and full regression workflow, then compare matched per-map frame CPU allocation/submit timing with portable lights off and on before considering the optimization accepted. Gate status: **IMPLEMENTED / REMOTE CHECKPOINTED / CI PENDING**.

## Local preservation/unification entry (2026-10-26)

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

## 2026-10-28 — Castle and investigation performance recovery checkpoint

User authorized comparison and repair over a five-hour work window, including unreadable shelf text and investigation-level performance. Isolated branch performance/castle-regression began at 65643f3c and fast-forwarded to actual main ba3d8885. Main exact-SHA GPU 40828826828, full multiplayer 40826909975 and installer 40826910008 passed; focused Castle 40826909984 failed the obsolete editor route assertion. Draft performance stacks remain unmerged. Read the current renderer tracking issues and A–G contracts.

Implemented actual Castle/static-world GPU retention, separate stationary light contribution probes, conservative spatial chunking, correct scene disposal/recovery, investigation van/scenery/fixture/native retention, exact PSX lookup quantization, cached material emission classification, removal of double frame pacing, and larger atlased shelf labels with readable aimed-exhibit UI. Kept lights, original models, source UVs, live optical/CRT/water/actor paths and CPU recovery. Details and preliminary matched RTX5070 results: CASTLE_REGRESSION_2026-10-28.md. Native Hollow Creek revealed and now repairs a severe repeated-preparation path as well.

Clean Java17 and Maven GPU packages built; actual OpenGL residency review passed zero unchanged VBO uploads, real torch/flicker/blackout, door, resize and return. Preliminary local full suite: 272 passed / 2 failed / 0 skipped / 274 entries. Both pinned TLS tests reproduced AVG certificate substitution; strict pinning and antivirus were left intact. Latest normal-preserving subdivision and stale editor-route test refinement still require scoped/source-exact revalidation. No A–G completion, installed full-round acceptance or new release is claimed. Next: source checkpoint/remote readback; exact-source CI; three matched all-map-type timing repeats; stable memory checks and retained visual evidence; final focused/full acceptance.

## 2026-10-29 — all-map and lifecycle correction checkpoint

First pushed source0f886e63 was remote-readback verified in PR36. Full40836192919 passed all275 manifest tests, packaged TCP, desktop House entry and native app-image startup. GPU40836185785 passed Java17 and Windows builds but failed Mesa torch reuse (17,158,176 reuploaded bytes / zero reused). The all-map timing matrix independently exposed the empty-socket/cupboard owner collision in Church; it is fixed without weakening assertions, and all10 map types now pass real-GPU zero unchanged uploads.

Additional audit corrected the sparse CRT material sentinel expanding the emission BitSet, with a96MiB child-JVM regression. Source pixel revisions restore live TV/telemetry and suppress unchanged map uploads. Resident interior topology is fixed across camera translation rather than rebuilt at distance bands. Actual corrected GPU smoke, all-map review, texture pixel controls and six scene cycles pass. Post-GC heap stays about633MiB; final mesh/texture counts remain1158/607. Three real offscreen Castle UI captures show readable targeted tier captions. Cold/waypoint33-frame p95 is still76ms; warm timings are not installed-case acceptance. Corrected source checkpoint, new exact-SHA CI and replacement three-run timings follow; no release/A–G completion claimed.


## 2026-10-29 — measured recovery and branch closeout

Runtime source `06b76cceafd82ea82869b531ada0dac9e062ea9c` pushed and remote-readback verified in PR #36; main remained `ba3d8885`. Renderer [40844278212](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40844278212) passed every job, including original torch reuse and expanded actual GPU checks for all ten map types. Full [40844284430](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40844284430) passed **277/277, zero skipped**, packaged TCP round, real desktop/solo entry and Windows app-image checks. Source gallery 40844284423 passed. Strict TLS was preserved; independent CI passes the locally AVG-intercepted tests.

Primary hardware experiment: three JVM trials × 90 warmup × 120 samples for each of 24 Castle/map-type views, 640×400 Classic PSX, i7-14700K / RTX 5070 / driver 616.64 / Windows 11 / Java 17.0.12. Castle p50 is now 8.80–11.44 ms versus 35.71–73.55 ms slow version and 19.36–22.14 ms previous Castle. All sampled p95 values are below 22 ms. House reference 22.26 → 8.66 ms, School 39.48 → 8.34 ms, native Hollow Creek reference 319.71 → 9.91 ms. Supplementary single-trial 1280×800 p95 is below 28 ms across ten sampled views. Complete raw CSV/provenance, trial ranges and original captures are committed; `tools/summarize_world_performance.py` recomputes summaries.

Exact measured JAR SHA-256 `1127F32057025421ADA0B115B6D6199DCCCAD97334CA676D78E94C869E7227DD`. Local self-contained app-image under `build/performance-preview/app/Beckenburg` passed the native all-map GPU review with exit 0 and matching embedded JAR hash. First native diagnostic attempt hit an existing-output access denial; fresh isolated output passed. Clean CI packaging and full acceptance passed independently of the local build-script resource replacement error. No installed application, security settings or other user game processes were changed.

Final evidence/tool-only checkpoint retains identical runtime source. Gallery contains 1,042 PNGs and passes local validation; captures are published on the review branch, not claimed deployed to main. Cold/new-view preparation, Castle exterior p99 (50 ms at 640×400 / 66 ms at 1280×800), physical installed solo/TCP return loop, full all-22-location audit and release gates remain open. This closes the measured repeated-preparation regression, not the entire A–G program.
