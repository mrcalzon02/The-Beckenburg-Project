# Portable-light guard follow-up — 2026-10-29

Runtime source: `36a41c7086b7f1fbf8b3e257912de58846a9035a` in PR #37. Parent renderer: `f82fb0857ec7b5ba984c9d33e034cda3cd904dbf`. Approved meshes, textures, gameplay and backend are shared. The parent SceneRenderer source was extracted with `git archive`, compiled separately with Java 17, and placed first on the baseline classpath; all other classes and assets use the candidate GPU JAR.

GPU JAR SHA-256: `87F647AD0D4FEFC22059177FBE0D26D1264D03EFA22FB42026D70016AAA3A34A`.

Local `build.cmd package` passed with installed Java 17.0.12 and Blender 5.0. Offline Maven GPU packaging passed. `WorldResidencyTest` passed Castle and all ten map types, including both portable-light kinds powered off/on/off, UV, buffer reuse, doors, resize and editor revisions. Prepared scenery exact pixels, prepared lighting and camera visibility checks passed.

The full local manifest executed all 277 entries: 274 passed, three failed, zero skipped. `DeveloperEditorLaunchTest` failed because the inherited sparse checkout excluded the tracked `installer/beckenburg.iss`; materializing the exact tracked installer file and rerunning the unchanged test passed. The remaining `CommonNetworkTest` and `SecureLanIntegrationTest` failed. A diagnostic TLS rerun confirmed the generated server certificate issuer `Private Game Server` was replaced on receipt by `AVG Web/Mail Shield Self-signed Root`, resulting in `CERTIFICATE_UNKNOWN / Server identity changed (NET-03)`. This matches the previously recorded workstation limitation; no pinning, trust, antivirus or network behavior was changed. The initial 274/3 result remains failed, with one separately passing repair rerun; clean CI must independently verify TLS.

The actual RTX 5070 / NVIDIA 616.64 OpenGL review passed Castle and all ten map types. Every unchanged repeat uploaded zero geometry bytes. Castle reused 18,251,520 bytes on its initial repeated frame. Live TV/map updates changed their pixels with zero geometry uploads; unchanged texture repeats uploaded zero bytes. Raw evidence is in `frames.csv`, `live-textures.csv` and `captures/`. Captures are runtime GPU diagnostics, not whole-map visual acceptance.

## Small matched House measurement

640x400, existing PSX preset, 90 warmup and 120 measured frames per view, identical scripted camera sweep. One trial per candidate, with a separate local regression process active during both trials. These results establish a small allocation change and image parity; they do not establish a statistically repeatable FPS gain or installed gameplay acceptance.

| View | Parent p50 / p95 / p99 ms | Candidate p50 / p95 / p99 ms | Parent / candidate median allocated bytes |
| --- | --- | --- | --- |
| Van | 9.06 / 11.11 / 12.35 | 8.55 / 11.34 / 12.79 | 5,954,560 / 5,951,968 |
| Interior | 9.14 / 12.59 / 13.43 | 8.99 / 11.38 / 13.12 | 6,657,912 / 6,648,832 |

Final before/after PNG SHA-256 hashes match exactly in each view: van `086E397C39C92AF14082C929BDDAF1796037EACDCFF2C01939B1D8DBC93BF6D8`, interior `A64AE7F4A6879CBDA751B921BF7BDD27B47F0AA7B438657E13FA7F00B34B8F72`. Raw matched CSV and environment records are in `before/` and `after/`.

Source acceptance workflows at `36a41c70`: [GPU 40869821516](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40869821516) **SUCCESS** for all three jobs (Java 17, Windows build/contracts/native launcher, Mesa OpenGL); [full multiplayer 40869825759](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40869825759) **SUCCESS**, including **277 passed / 0 failed / 0 skipped**, packaged TCP round, desktop/multiplayer UI, solo case startup and the Windows app-image executable. This independently verifies both TLS tests on a clean runner. Later checkpoint commits change evidence/gallery/docs only; their source is identical to tested runtime `36a41c70`. Full A–G, physical installed interaction and a released installer remain pending. No main merge or release was performed.

## Next measured allocation target

A separate Java Flight Recorder profile used the candidate GPU JAR on the same House camera sweep with 90 warmup and 240 measured frames per view. Recording overhead means its timings are not the matched comparison above. Warm main-thread Point/Face samples after initial preparation were dominated by `SceneRenderer.looseClutter` and `SceneRenderer.imported -> eventFixtures`. Estimated sampled allocation weights were about 1.16 GB and 579 MB respectively over that selected interval, not exact byte counts or per-frame rates. The recording remains in the local ignored build directory. The next bounded implementation should retain world-space geometry by actual clutter/fixture pose and state, preserving pickup/throw/roll, alarms, water, television pixels, map generation and bounded disposal. No such dynamic-geometry cache is implemented or accepted by this checkpoint.
