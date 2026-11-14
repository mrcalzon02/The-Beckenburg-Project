# Beckenburg benchmark release audit — 2026-11-13

**Disposition:** Freeze the existing **v0.1.328** Windows release as today's reproducible development benchmark. Do not publish an otherwise identical v0.1.329 merely because the current main branch has a later test-only commit. **CI-functional acceptance: PASS. Physical Windows GPU / headset / all-map acceptance: OPEN.** This is a historical pinned snapshot, not an assertion that subsequent main-branch changes were measured.

## Exact-source identities and downloadable baseline

| Item | Identity / outcome |
| --- | --- |
| [Published Beckenburg v0.1.328](https://github.com/mrcalzon02/The-Beckenburg-Project/releases/tag/beckenburg-v0.1.328) | Non-draft, non-prerelease, published 2026-11-13 10:45:10 UTC |
| Immutable release tag and installer source | `c64691e19ffe0ef5c727fa921071a66fcff9d298` (tag ref independently read back) |
| Main HEAD at audit | `2c5368adaac54ebe152fcaf951730fe7837fcde4` (one commit ahead: only `LanGhostHearingIntegrationTest.java` and `LanVoiceHostIntegrationTest.java` changed) |
| [Release/installer workflow 43410354362](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43410354362) | SUCCESS; packaged TCP round, Castle/solo launch, embedded solo campaign, Windows installation and installed Castle start, updater handoff, release asset publication/readback |
| [Full-round v0.1.328 source 43410354280](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43410354280) | SUCCESS; **378 passed, 0 failed, 0 skipped**, full manifest |
| [Full-round newer test-only HEAD 43464676986](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43464676986) | SUCCESS; **378 passed, 0 failed, 0 skipped**, packaged LAN, hearing/ripples/voice, Blackpine stairs and native Castle smoke |
| [Latest exact-source renderer job 43344719695](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43344719695) | SUCCESS on **v0.1.327 source** `a3c0047a9819dcb094dd307ff14ee12a25fa9436`; *not* v0.1.328 hardware performance verification |

Release assets (GitHub-published metadata; these are **asset digests**, not independently rehashed downloads):

- `Beckenburg-0.1.328-Windows-x64.exe`: 761,020,355 bytes; SHA-256 `da6ebe7cd7a3a4b7102a5166880a20bf0d1fbc9376e2ba9e5d0b40710be4ddb2`.
- `LevelEditorKit-Java17.jar`: 921,960 bytes; SHA-256 `f7e0aa59f3bf6c657c53e83ca06708680836bc204e1aa9ba8ba0343e293f3d15`.
- `SHA256SUMS.txt`: 195 bytes; SHA-256 `38c251c898912076f9d8edf607129f27e01a6e896c86f755b33d91f7aab918de`.

**Code delta:** v0.1.328 follows v0.1.327 with a small `GhostSoundRipples.java` change and `GhostSoundRipplesTest` expansion plus recovery notes; it has independent successful release and full-round checks. The main HEAD thereafter changes tests only. Neither fact licenses claiming v0.1.328 has measured GPU FPS.

## Existing renderer measurements — reproducible CI references, **not Windows GPU FPS**

Values below were extracted from the completed **Mesa llvmpipe software-OpenGL** `GPU CONTINUOUS ROUTE` log output: 33 House camera samples in each run. The two CI runs may differ in host scheduling, thermal state, image, and background load; a single increase is not proven causal regression.

| Measure | [v0.1.326 code / run 43300765849](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43300765849) | [v0.1.327 code / run 43344719695](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43344719695) |
| --- | ---: | ---: |
| CPU frame p50 | 7.786 ms | 8.193 ms |
| CPU frame p95 | 63.273 ms | 89.341 ms |
| GL draw issue p95 | 23.182 ms | 29.520 ms |
| GL readback p95 | 9.368 ms | 14.399 ms |
| End-to-end p95 | **98.435 ms** | **124.527 ms** |
| House rotation confirmation mean / worst (6) | 461 / 734 ms | 612 / 1,021 ms |
| UI House hunter ready | 3,457 ms | 4,389 ms |
| UI Farmhouse hunter ready | 8,945 ms | 9,554 ms |
| UI School hunter ready | 1,913 ms | 2,163 ms |

The end-to-end p95 increased by **26.092 ms (26.5%)** in these *unmatched, single-run* CI samples; do not attribute that difference to a code change without a repeated same-runner A/B. The project's 30 FPS goal implies a 33.3 ms frame budget, which these **software-GL end-to-end** p95 results do not meet. They cannot be converted directly into dedicated-GPU interactive FPS.

The v0.1.327 Mesa acceptance additionally passed real OpenGL flashlight and resident-VBO reuse, 11-map-type renderer residency, and three UI loops from Castle to House/Farmhouse/School to authenticated van retreat back to Castle. It produced PNGs, CSVs and an artifact; these are narrower than full installed all-map exploration.

## Release acceptance and limitations

**Accepted now (CI):** release source/tag match, Windows installer published, standalone editor kit present, packaged Java 17 game, TCP investigation completion, actual desktop/Castle startup, installed software recovery route, updater helper validation, two successful **378/378** manifest runs, ghost hearing/stillness/ripple/network authority contracts, and focused Blackpine physical stair/rail native regression.

**Still open (do not represent as passed):**

1. Reference **physical Windows GPU** cold/warm benchmark of the exact v0.1.328 installer: p50/p95/p99 frame, draw, upload, readback, load/first frame, FPS, memory/GC, hardware/driver, resolution/preset and repeated camera route.
2. Two-computer Windows audio/headset hearing, voice and stillness perception test; confirm the stationary ghost sees expanding sound-origin ripples through walls and that motion mutes hearing with intended tolerance/fade.
3. First-person, installed Blackpine stair landing/guardrail inspection and per-map upper-floor/stair/navigation audit; the focused stair tests do **not** certify the other maps.
4. Blackpine live-versus-editor snapshot import/save/reopen verification; all-map editor-agent construction acceptance and genuinely completed environments remain separate.
5. Installed multi-version updater, persistent case progression, hardware GPU route stability and open renderer A–G / C3 depth-portal gates ([#7](https://github.com/mrcalzon02/The-Beckenburg-Project/issues/7), [#20](https://github.com/mrcalzon02/The-Beckenburg-Project/issues/20)).

**Release classification:** CI-tested development build / **benchmark baseline**, not physical test accepted and not product complete. Do not promote the existing release merely because packaging and synthetic renderer diagnostics passed.

## Next benchmark execution

1. Keep v0.1.328 and its exact tag immutable. On an actual Windows self-hosted GPU machine, install and run that published executable. Capture exact installer SHA-256, GPU model/driver, CPU/RAM, OS, viewport, graphics settings, software/GPU mode, test date and source SHA.
2. Use the established 33-waypoint House camera/profile path and three real-case UI cycles, plus a cold restart and warm reruns (at least 3 per condition). Retain `continuous-route.csv`, `continuous-route-summary.txt`, `case-cycles.csv`, screenshots, diagnostic logs, load/GC/memory data, and crashes. Do not compare different machines as if identical.
3. If an apparent performance regression persists, compare v0.1.326, v0.1.327 and v0.1.328 on **the same machine**, repeat with matching settings, and isolate architecture-light/cold-prewarm paths before changing the release pipeline.
4. Execute the headset, Blackpine installed traversal and editor snapshot acceptance separately. Continue development after archiving this checkpoint. Publish a new numbered installer for a substantive gameplay/runtime/editor change that passes its functional release gates; **do not spend a release number on this test-only main delta**.

Sources: linked exact-SHA workflow logs, the live release/tag/asset API, `AGENTS.md`, `RELEASES.md`, `development/RECOVERY_PLAN_2026-10-25.md`, `docs/RELEASE_ACCEPTANCE.md`, and [`LOADING_PREBAKE_STAGING.md`](LOADING_PREBAKE_STAGING.md).
