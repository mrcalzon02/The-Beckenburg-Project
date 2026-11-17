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
