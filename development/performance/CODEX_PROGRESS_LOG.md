# Codex A–G execution checkpoint log

This record is the per-project progress ledger for the FINISH plan. Update **after each verified commit**; never backfill "passed" from an unrelated SHA.

## Scheduled follow-up checkpoint (2026-10-29)

- Prepared-light final runtime `82bd31ac`, [PR #40](https://github.com/mrcalzon02/The-Beckenburg-Project/pull/40): private probe arrays/direct RGB write, only live receivers with explicit offsets, lazy immutable copies and unchanged GPU revision retention. Exact float, full sparse/dense production-buffer parity and previous lighting/residency checks pass; RTX all-map controls and12diagnostic/12matched image pairs remain identical. Three longer Castle trials improve p95 about8–16%, exterior41.55→35.05ms (target still open), p99 47.74→41.63ms; sampled flicker mesh upload17.78→7.55MB. Six lifecycle cycles retain stable resources. Historical insufficient candidates56f64704/b63f7acb preserved. [Raw evidence](hardware/2026-10-29-prepared-light-write/README.md). Final-source GPU40880772640/full40880777632 remain running. Next: inspect exact outcomes, then actual native launch/playable-route acceptance and remaining measured costs. No release/main merge/A–G completion claimed.

- Final packed-readback acceptance at `35054b76`: [GPU 40876041513](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40876041513) **SUCCESS** all three jobs; [full gameplay 40876045372](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40876045372) **SUCCESS**, **279 passed / 0 failed / 0 skipped**, packaged TCP round, desktop/solo case startup and native executable. This closes source CI for PR #39, not physical installed/release/A–G acceptance. The recurring Castle tail profile identifies prepared-probe RGB/list iterator allocations and unnecessary color-buffer revisions as the next measured target.

- Packed readback source `35054b76`, [PR #39](https://github.com/mrcalzon02/The-Beckenburg-Project/pull/39): reconciled E0 color/depth/CPU timing into current backend and replaced four byte loads with one endian-explicit packed color load. Pure Java color/alpha/row/depth/order/dimension tests and actual RTX all-map controls pass. Twelve diagnostic captures and 24 matched benchmark images have identical hashes. Three trials at1280x800 show about24–31% lower median CPU conversion time and5–10% lower House/Castle median frame time; native Hollow Creek total unchanged, Castle exterior tails still open. Source GPU `40876041513` and full `40876045372` are running. [Raw evidence and limits](hardware/2026-10-29-packed-readback/README.md). No physical installed/release/A–G completion claimed.

- Final pose-cache source acceptance `e8cec914`: [GPU 40873064007](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40873064007) **SUCCESS** all three jobs; [full gameplay 40873068301](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40873068301) **SUCCESS**, **278 passed / 0 failed / 0 skipped**, packaged TCP round, desktop/solo startup and native Windows executable. Evidence-only commits follow. PR #38 is verified within these scopes; installed physical interaction and release remain open. Next stage compares existing E0 readback phase instrumentation and packed RGBA conversion while preserving result ownership/depth.

- Pose-retention source `e8cec914`, [PR #38](https://github.com/mrcalzon02/The-Beckenburg-Project/pull/38): retain bounded CPU source faces by actual clutter/fixture pose, preserving dynamic lighting and live overlays. New differential source-face regression passes all ten maps, movement/holding/redaction, alarm animation, water/power, deletion, map changes and a 158,000-triangle overflow case. Existing physics/network/fixture/pixel/lighting/residency checks and local game/GPU packages passed. Actual RTX all-map controls passed; 12 diagnostic and 18 matched-profile capture hashes remain identical. Three-trial parent comparison shows **65.7–94.3% lower median main-thread allocation**, Farmhouse/School median frame time about 3–7% lower, House essentially unchanged. Six GPU lifecycle cycles retain stable post-GC resources. [Evidence](hardware/2026-10-29-dynamic-pose/README.md). GPU `40873064007` and full `40873068301` remain running; no full acceptance/main merge/release claimed.

- Final source acceptance at `36a41c70`: [GPU 40869821516](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40869821516) **SUCCESS** all three jobs; [full multiplayer 40869825759](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40869825759) **SUCCESS**, **277 passed / 0 failed / 0 skipped**, packaged TCP round, actual desktop/solo startup, Windows app-image launch. Local full manifest had 274 pass/3 fail; the sparse-checkout installer input was restored and the unchanged editor test passed, while both local TLS failures were traced to AVG certificate substitution. Clean CI independently passed both TLS cases. Evidence-only/gallery commits follow the tested runtime source. PR #37 remains reviewable; no main merge/release or full A–G completion claimed. Next measured target: repeated `looseClutter` and `eventFixtures` geometry allocations, with pose/state invalidation and physics parity required.

- Continued source checkpoint: `36a41c7086b7f1fbf8b3e257912de58846a9035a`, [PR #37](https://github.com/mrcalzon02/The-Beckenburg-Project/pull/37). Hoisted the shell guard once per shell pass and added both portable flashlight/firelight off/on/off transitions to the existing all-ten-map renderer regression. Java 17 package, Blender export, offline GPU Maven package, all-map submission regression, exact prepared-scenery pixels, fixture probe and visibility checks passed locally. Actual RTX 5070/NVIDIA 616.64 review passed all ten maps plus Castle, live TV/map pixels and zero unchanged mesh uploads. Small matched House trial shows lower per-frame allocation and identical final-image hashes; timing changes are small and mixed, not a proven FPS gain. Raw evidence and limitations: [hardware checkpoint](hardware/2026-10-29-portable-light-guard/README.md). Full manifest and source CI are still running at this entry; no installed/release gate closed.

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

## 2026-10-29 — native package and playable-case acceptance continuation

Prepared-light runtime `82bd31ac` completed GPU40880772640 and full40880777632 SUCCESS, including **281/281, zero skipped**, packaged TCP, desktop solo startup and native executable. Three Castle trials already show p95 reductions of8–16%, with exterior stress still35.05ms; no target-completion claim. Main advanced independently to `3e41ec54`; reconciliation `78088f67` preserves its editor Extras action, Options contract and both recovery histories.

The previous GPU UI smoke could accept an opening framebuffer and its EDT wait could hang indefinitely. DEV-161 now tracks the stronger candidate: explicit House/Farmhouse/School selection, first sized frame, skip/natural intro handoff, advancing GPU hunter frames, real WASD/collision movement near van spawn, position-validated X retreat, debrief and menu return before repeat dispatch. It captures only its own surface and preserves partial CSV evidence. New lifecycle CI remains pending at source checkpoint; short van movement does not certify a successful investigation or interior routes.

A fresh Java17 self-contained diagnostic executable passed the real RTX5070 all-ten-map OpenGL controls and teardown with exact embedded JAR hash. `tools/verify_native_gpu_preview.ps1` reproduces this without packaging unrelated build files, replacing an installed game or opening a local visible game window. Initial evidence and explicit limits: hardware/2026-10-29-native-case-acceptance/. Next: source-checkpoint publication/readback and exact-source GPU UI acceptance; then remaining playable-route/cold-frame work. No main promotion, release or A-G completion.

Implementation checkpoint `10e2f4e` and clean-source native rerun pass; embedded JAR `EBDE4C0734F00891410A70DA1583676EFDA8D2BAC90767F673809EC6B8E5929C`, exit0, twelve captures identical to prepared-light parent. Raw controls/provenance/captures are preserved in the evidence folder; gallery1102 passes. The updated headless real editor action passes after restoring the sparse-omitted tracked installer input. Publication retries currently encounter intermittent GitHub DNS failures; local commits/evidence remain durable and no new remote delivery is yet claimed at this evidence checkpoint.

Subsequent publication recovered: source `10e2f4e6675426de92f751b1b18adb63456bb293` pushed/readback-verified in draft PR41. GPU40923212823 and full40923217003 were dispatched for that exact source and remain pending at the native-evidence checkpoint. Intermittent DNS still affects status reads; no credential, TLS, antivirus or network settings were changed.

## 2026-10-29 — exact contributor factors and handheld surface controls

Native acceptance source10e2f4e completed both GPU40923212823 and full40923217003 SUCCESS. GPU logs explicitly require actual House/Farmhouse/School player handoff, WASD movement and real RETREATED/debrief/menu/next-case cycles. This closes scoped DEV-161, not the installed full investigation gate.

The next measured change snapshots only the actual base/normal/emissive source factors of each retained material batch, using raw double bits. Spatially dirty batches with identical real inputs can retain their full arrays/revisions. Door/topology/scene lifecycle and full RGB math remain authoritative. One-ULP input, cross-scene ownership, all-ten-map exact dense-reference parity under2GiB and fallback pass. Three matched Castle trials at1280x800 improve exterior p95 34.50 to20.60ms and p99 39.46 to24.12ms; p50 is slightly slower, and library p95 is slightly higher. Actual RTX all-map controls pass with twelve identical parent captures. DEV-162 and hardware/2026-10-29-light-factor-dependencies/ preserve methods and limits; new exact-source CI pending.

User asked whether flashlight/UV beams stop at the first surface. Production GPU/CPU flashlight paths use visible-surface depth, and UV performs map line-of-sight. New focused production GPU control passes9248 covered pixel comparisons in both draw orders, positive illumination and finite reach. New core test exercises actual UV vertex lighting across real closed/open doors in all three playable authored maps. Thin/loose prop and off-camera dropped-light visibility remain a separate audit; no global first-triangle guarantee is inferred from collider sampling. No shader, UV evidence, gameplay or authored asset behavior changed by these guards.

Source99baa799 pushed/readback-verified in PR42; exact-source GPU40927250498 and full40927255222 are running. Clean native app-image passes all-map controls with candidate/embedded JAR482B72E910FF124A7A2E0E2D85C1F4D321CD4AB3C9181FA5CA0837B958838034 and exit0. Supplementary final-package comparison reproduces exterior p95 36.56 to20.09ms. Six lifecycle cycles retain643,376,200–643,915,080 post-GC bytes,1158 meshes/607 textures/14,486,784 resident bytes.32 image pairs match exactly; actual trials, native captures/provenance, parent real-UI captures and labeled synthetic controls retained; gallery1164 passes. User explicitly requested finishing publication and merging. Freeze new optimization scope, wait for source acceptance, merge the reviewed stack preserving current main, then verify main acceptance/shipping results. No merge or release claimed at this evidence checkpoint.

Final source99baa799 acceptance readback: GPU40927250498 and full40927255222 both SUCCESS,282/282 with zero skipped. Required GPU log assertions include production first-surface flashlight depth/finite reach, actual UV closed/open doors and three real dispatch/hunter/movement/extraction/menu cycles. Structured source-acceptance.json retained; DEV-162 now scoped RTX/CI verified. No runtime changes follow. User-authorized guarded merge and exact-main shipping verification proceed next; no physical installed full-investigation or A-G completion claimed.
