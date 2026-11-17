# House walking hitches and single-face Material Editor latency

## Finished work and priority

* [v0.1.351](https://github.com/mrcalzon02/The-Beckenburg-Project/releases/tag/beckenburg-v0.1.351) shipped the retained unchanged editor paint. Its [full Windows run](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44131171455) recorded 391 passed, zero failed and zero skipped. Its [standalone editor](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44131171027), [native editor](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44131171098) and [installed game and TCP settlement](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44131170964) passed.
* First priority in play is the first steps inside the House sanctuary. On the last physical GTX run all 43 intervals above 50 ms occurred in the first 375 ms of walking. Fresh shadow rays were zero. The horizontal projection buffer increased solo hitch frequency. It stays disabled. [Raw data and original experiment](../2026-11-17-engine-editor/README.md).
* First priority in the editor was the next full redraw on a local edit. On 120,000 faces the old painter spent seconds redoing unchanged translucent outlines. This revision repairs the prior and new projected areas and replays intersecting faces in document order. A changed view, changed texture, ambiguous selection, multiple visible face edits or coalesced state replacements can use the original full redraw. Closing releases the retained scene and canvas.
* A separate [focused Castle workflow](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44131170811) stopped at an assertion forbidding the Ancient Shade from appearing alone. The game explicitly allows that species to manifest in an empty room. The corrected test checks a silent control, zero harm and fear for empty-room actions, and the physical-victim requirement for STRIKE across every tested species.

## Editor timings

Wall time includes the actual EDT state observation, list update and Swing canvas paint. The timing excludes immutable document construction, image hashing and monitor presentation. The normalized atlas has 120,000 triangles visible. On the tiled fixture the measured edit targets a visible face. Each preview changes a UV coordinate, pin and seam, followed by undo. Each JVM warms up one complete select/reselect/edit/undo cycle per case, then records six. Four fresh JVMs ran in baseline, candidate, candidate, baseline order. Each reported row contains 24 samples per arm. Percentiles use nearest rank. The p99 in this small sample is the maximum.

| UV layout | Operation | Baseline p50 / p95 / p99 ms | Baseline median allocated bytes | New p50 / p95 / p99 ms | New median allocated bytes |
|---|---|---:|---:|---:|---:|
| atlas-edges | select/reselect | 1784.22 / 4709.73 / 5365.71 | 534,777,060 | 11.63 / 36.67 / 41.45 | 1,005,136 |
| atlas-edges | edit/undo | 1891.03 / 3378.91 / 4423.93 | 536,350,480 | 16.53 / 36.21 / 38.16 | 1,283,740 |
| atlas-guides | select/reselect | 1616.61 / 2023.00 / 2367.62 | 534,778,044 | 16.70 / 24.00 / 30.97 | 1,886,216 |
| atlas-guides | edit/undo | 1607.03 / 4558.67 / 8558.26 | 534,778,476 | 18.73 / 28.78 / 35.28 | 2,267,548 |
| tiled-edges | select/reselect | 94.78 / 277.39 / 292.54 | 51,892,028 | 3.56 / 4.85 / 5.66 | 55,760 |
| tiled-edges | edit/undo | 114.41 / 273.10 / 283.31 | 51,892,960 | 5.08 / 8.81 / 11.23 | 56,368 |
| tiled-guides | select/reselect | 135.96 / 419.39 / 1122.33 | 51,893,012 | 9.20 / 54.14 / 95.77 | 64,360 |
| tiled-guides | edit/undo | 127.32 / 387.45 / 508.85 | 51,893,668 | 10.90 / 18.20 / 23.15 | 63,824 |

All 192 matched images have identical whole-canvas ARGB hashes. The differential test also passed 144 full-image comparisons covering overlapping translucent faces, guides, local UV/pin/seam edits, coalesced updates, partial clips, antialiasing, fonts, transforms, resizing and document retirement.

Measurements used Java 17.0.21, a 1 GiB heap, headless Java2D, a 520 × 500 canvas and the same shared Linux executor. The baseline is the actual painter at `c06ccdcc1d14b3c285f44a7b1a5694722e1f94dd`. Frame pacing on a display, complete UV transaction preparation, multi-face changes and the latency of a held pencil stroke have separate costs. All source hash and raw file receipts are in `editor-sha256.txt`.

Reproduce using the same freshly compiled `EditorUvPaintProfile --changes OUTPUT` test driver with the baseline or candidate painter first on the classpath. The four raw CSVs include wall time, EDT CPU, allocated bytes and full-image SHA-256 per operation. The optional Windows window test uses the real `previewUvTransform` and cancel path with the prior painter compiled against the same candidate host. Run `tools/performance/windows_uv_window.ps1 -Baseline COMMIT` after `build.cmd package`.

## Physical game investigation

[Core30 trial 44151619792](https://github.com/mrcalzon02/Dens-Mortis/actions/runs/44151619792) pairs the clean production renderer with a timing-only overlay on one packaged game. Both use the same driver, disabled columns, solo route, loopback three-seat route and one-step hold. The clocks split characters, distance haze, weather, sanctuary, hiding-place rendering, PSX, graphics effects and haunting overlays. EDT time excludes parallel worker CPU. JVM collection counts and compiler activity are process-wide observations. The worker also verifies optical correctness and full TCP settlement.

This source checkpoint starts the physical study. The raw completed trial determines which pass to change next.

## Windows runner time recovered

Three consecutive evidence commits triggered a complete new installer because the workflow ignored the recovery plan but still considered its companion performance progress log and offline scripts release inputs. These jobs also offered three updater downloads to users despite identical gameplay/editor runtime sources.

| Evidence commit | Release | Successful installer job | Occupied seconds | EXE bytes |
|---|---|---:|---:|---:|
| 2117262 | 0.1.352 | [44133686620](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44133686620) | 522 | 761,378,464 |
| d6036a4 | 0.1.353 | [44134665394](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44134665394) | 665 | 761,364,645 |
| c06ccdc | 0.1.354 | [44136008869](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44136008869) | 531 | 761,373,435 |

The three jobs occupied 1,718 seconds and published 2,284,116,544 bytes of Windows executables. A path-filter replay of all 10, 6 and 6 changed files respectively now skips those three releases. The 18-path editor implementation still ships. Mixed changes to game source, the GPU renderer, runtime assets, installer, editor-kit build and main build script each still ship alongside receipts. Manual dispatch remains available. This figure measures the observed three-job opportunity, not a monthly projection.

## Visible Windows editor and released build

The native [Windows editor run](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44153591003) used the existing showing-window harness, the project's actual one-face UV transform and cancel, a 526 × 539 canvas and 28 visible list rows. Four fresh JVMs ran baseline, candidate, candidate, baseline, ten samples per JVM. The baseline painter ran against the same compiled editor. Each sample includes observation, frame validation, complete visible workspace paint and Toolkit sync.

| Windows full-workspace update | Samples | p50 ms | p95 ms | p99 / max ms | Over 33.3 ms | Over 50 ms |
|---|---:|---:|---:|---:|---:|---:|
| Released prior painter | 20 | 907.660 | 930.756 | 930.968 | 20 | 20 |
| Local redraw | 20 | 15.938 | 21.896 | 22.268 | 0 | 0 |

[Raw Windows samples](windows-window.tsv). Native source-model UV manipulation, apply/cancel, shared history, save/reopen, runtime UV/color parity and installed MCP repair passed. CI artifact 14361461649 contains the native editor evidence. These numbers cover a single visible changed face and a real window on this configuration. Scaling fallbacks, larger multi-face edits and other display configurations still have their own costs.

[Release v0.1.355](https://github.com/mrcalzon02/The-Beckenburg-Project/releases/tag/beckenburg-v0.1.355) points to source `96bb6cfd7554ff60a2c699144abcfad1688d7d3f`. The [installer](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44153590626) passed packaged three-seat settlement, real embedded solo/two bots, installed Castle launch and external updater. Windows EXE SHA-256 `978757097b8663d4f0ed960ce197e20fbe4ff9dcb3c85fc8a5836f1804dcfb46`. The [focused Castle workflow](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44153590607) passed the empty-room Shade and physical-victim checks.

## Physical post-geometry attribution

Run 44151619792 completed all 16 matched GTX trials, both three-seat settlements and optical controls. The clean/probe arms use one game JAR at c06ccdc with identical gameplay renderer source to release v0.1.355. The probe overlays only diagnostic clocks. The 20,582 advancing frame intervals include one rendered 960 × 540 viewport with two loopback peers in multiplayer. A CPU-side consumed frame interval includes callback scheduling and readback. Hardware scanout is outside this measurement.

| Clean game mode / phase | Intervals | p50 ms | p95 ms | p99 ms | Worst ms | Over 50 ms | Over 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|
| solo / idle | 3679 | 21.560 | 23.796 | 26.572 | 48.149 | 0 | 0 |
| solo / moving | 533 | 21.465 | 31.752 | 62.997 | 70.690 | 11 | 0 |
| solo / stopping | 929 | 21.369 | 23.726 | 30.147 | 30.910 | 0 | 0 |
| tcp / idle | 3683 | 21.513 | 23.639 | 26.396 | 45.454 | 0 | 0 |
| tcp / moving | 536 | 21.478 | 31.371 | 57.675 | 66.839 | 10 | 0 |
| tcp / stopping | 929 | 21.382 | 23.792 | 29.282 | 30.968 | 0 | 0 |

All 35 clean and probed gaps above 50 ms ended inside the sanctuary in the first 0.269 seconds after the first moving callback. No fresh shadow rays were generated in any of those gaps. The 14 instrumented gaps each spanned one callback. Sanctuary rendering alone took 42.535–61.136 ms and accounted for 96.8–97.8% of post-geometry time. Its median on these gaps was 55.87 ms solo and 51.51 ms TCP. PSX processing was about 1.3 ms. The hiding-place pass never exceeded 0.136 ms in the instrumented gaps. Two gaps overlapped an increment in JVM collection count and eleven overlapped an increase in total compiler time. These process-wide counters cannot identify the method compiled, a safepoint duration or a causal GC/JIT pause. EDT CPU accounting on Windows is coarse and excludes parallel helper threads.

Each of the 16 one-step stops finished inside, and sixth stable-frame render costs were 7.889–15.870 ms. The historical 127 ms interval remains an open tail risk. [Original worker archive](gtx-stage-raw.zip) · [Per-trial and per-hitch results](gtx-stage-results.json) · [Exact instrumentation diff](effect-stages.patch). Reproduce with `python tools/performance/effect_stage_report.py gtx-stage-raw.zip results.json`. Archive SHA-256 `901b8bcb9a97128ddda64c52c52a119d59df029e7df713c991104adaa76ffcd3`.

## A shader change rejected before using more physical runner time

A candidate skipped the upper ellipsoid test for downward rays below the sanctuary shoulder or rays proved to exit its cylindrical wall. It matched 2.4 million seeded analytic hit distances bit for bit, and every image hash, depth hash and shaded-pixel count matched in both moving-camera pairs. Shader-only p95 worsened in both pairs on the shared Linux worker. The first went from 19.98 to 47.16 ms, the second from 37.77 to 39.73 ms. The baseline p50s were 14.64 and 14.95 ms, versus 32.42 and 16.03 ms with the change. The release retains the original analytic path. [Four raw CSVs and unshipped patch](surface-replay.zip).

The next hardware experiment is [44157367610](https://github.com/mrcalzon02/Dens-Mortis/actions/runs/44157367610). The game package and test driver are identical between arms. One arm runs twelve disposable shader draws before play to exercise changing camera poses, held poses, animation and depth. The experiment separately records priming cost, elapsed time from Java main to the first observed in-view callback, input acceptance, frame tails and the one-step hold.
