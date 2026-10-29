# Castle and investigation-map performance regression

Status: scoped Castle and shared-level repair implemented, pushed and measured on Windows hardware. Corrected renderer CI, all 277 regression checks, packaged TCP/UI acceptance and the local native GPU review passed. This is not a release or complete A–G claim.

The user requested comparison with the smooth previous Castle, diagnosis and repair of the rebuilt Castle and investigation levels, and readable shelf labels. Work is isolated on `performance/castle-regression` from main `ba3d8885f07487af94b0c9aae6a6d4563e21eb58`. The previous Castle reference is `68d1142d62a6237ff2567a54868b568b9f711487`, immediately before construction commit `8a854a7e`. The pre-fix current source is main `ba3d8885` with timing instrumentation only.

## Reproduced differences and causes

- At the courtyard reference, source geometry rose from 55,147 triangles to 220,671; fixtures rose from 31 light sources to 64. The warehouse grew from 43 single-tier exhibits on 8 racks to 129 tiered exhibits on 15 racks. Thick masonry/slabs, additional dressing and revised surrounding terrain also increased geometry. Old distance filters built fewer objects in distant views. Counts are view-specific where the old source culled during construction.
- OpenGL rasterization was active, but the Castle passed `null` to the investigation-map-gated retention path. Static Castle props and fixtures were transformed, allocated, CPU-lit and packed every frame. Current baseline allocated about 63–201 MiB per submission-thread frame at 640x400.
- Cached investigation van, arrival road, enclosure and authored fixture faces still took transient lighting/packing paths. Native Hollow Creek had no GPU residency and could churn its 96-entry transformed-face cache.
- Cosmetic candle/TV brightness revisions repeated source visibility work. The first retained-geometry candidate was rejected after reproducing 300–400 ms relighting stalls. Static attenuation/visibility must survive brightness changes.
- JFR identified per-pixel floating-point PSX quantization and per-vertex lowercase material classification as shared costs. The former used approximately 6–13 ms/frame in these baselines.
- A hidden-worker 60 Hz foreground cap and idle polling interval added scheduling delay after Swing had already paced submissions.
- The 129 labels each owned a material and squeezed text into a 105 mm plaque.

## Implementation and fallbacks

Static authored topology and spatial chunks now retain explicit GPU IDs, revisions and source arrays. Castle lamps/doors have state-keyed assemblies. Investigation props, van/stock/doors, arrival scenery, enclosure, authored sockets and native non-billboard geometry use retained resources. Context/scene recovery can resupply source arrays, and scene changes dispose backend mesh generations. The new shared-world geometry buffer budget is 128 MiB; bounded fallback preserves geometry. Existing House shell buffers and probe/CPU object memory are additional and are measured separately.

Stationary receiver probes retain visibility and attenuation separately from live source factors. Flicker and true switches evaluate lighting without retessellation or repeated architectural rays. Door/occluder revisions refresh visibility conservatively. UV and deployed portable lights keep the dynamic path; DOTS retains physical receiver geometry; billboard orientation, actual water, sprites and live CRT textures stay on their intended paths. Subdivision retains authored normals and interpolates midpoint normals.

Exact numerical lookup tables replace the PSX framebuffer quantizer, and emission classification is cached by append-only material ID. Noncontinuous GL rendering still draws only submitted work and sleeps while idle, with no second foreground throttle.

Larger 190 mm shelf plaques share one atlas/material. Aimed exhibits expose full-resolution family and tier text inside the Castle viewport.

`-Dbeckenburg.world.resident.disabled=true` disables the new world path; `-Dbeckenburg.castle.resident.disabled=true` selects the legacy Castle submission path. The existing software renderer remains available. These are internal recovery/diagnostic switches, not user-facing graphics menus.

## Measurement contract

Actual Windows Java 17.0.12 / NVIDIA RTX 5070 / OpenGL 3.3 driver 616.64, 640x400, Classic PSX. One persistent renderer/map per run; fixed named viewpoints and small continuous camera turns. Warmup and sample counts accompany every CSV. Completed-frame time includes CPU render submission and waiting for that exact GPU result. GL-worker wall time is **not** a GPU timer query. Submission-thread allocation is **not** total JVM allocation. These are scripted rendering views, not collision-proven walking or installed-case acceptance.

Reproduce a packaged candidate with `tools/profile_world_performance.ps1`. It isolates home, temporary files and preferences, captures JAR hash/source head, and defaults to three runs over all eleven Castle/map types. `--world-residency-review` independently verifies completed real GL frames, zero unchanged VBO uploads, live torch/flicker/blackout, doors, resize and scene return. Headless `WorldResidencyTest` is a submission-contract test, not GPU evidence.

## Final matched three-trial results

Corrected source: `06b76cceafd82ea82869b531ada0dac9e062ea9c`, with remote branch readback verified. Packaged GPU JAR SHA-256: `1127F32057025421ADA0B115B6D6199DCCCAD97334CA676D78E94C869E7227DD`. The final package has no overlay classes. Historical old/pre-fix runs use complete source-class overlays and the identical instrumentation harness; their class hashes and source labels are preserved in provenance JSON.

Every view has **three independent JVM trials, 90 warmup frames and 120 measured frames per trial**. Percentiles pool the 360 completed-frame samples per view using nearest rank. They are not averaged trial percentiles. Hardware was Intel Core i7-14700K / RTX 5070 / driver 616.64 / Java 17.0.12, Windows 11 build 26200, Classic PSX at 640×400. No CPU-heavy builds or other GPU diagnostics ran concurrently with final samples.

| View | Previous Castle p50 ms | Slow version p50 ms | Fixed p50 / p95 / p99 ms |
|---|---:|---:|---:|
| Castle courtyard | 19.36 | 35.71 | 8.80 / 19.59 / 29.36 |
| Castle store | 22.14 | 40.40 | 9.31 / 21.85 / 29.41 |
| Castle library | 20.84 | 36.64 | 9.22 / 13.31 / 17.91 |
| Castle exterior | 20.71 | 73.55 | 11.44 / 16.79 / 50.45 |
| House interior | — | 22.26 | 8.66 / 12.30 / 14.60 |
| School van | — | 56.36 | 7.33 / 11.38 / 13.22 |
| School interior | — | 39.48 | 8.34 / 12.39 / 13.87 |
| Church interior | — | 31.84 | 8.44 / 11.06 / 11.61 |
| Hollow Creek native reference | — | 319.71 | 9.91 / 17.25 / 18.41 |

All **24 sampled Castle/map-type views** have fixed p95 below 22 ms. Median reductions range from 55.8% to 96.9%. This is a scripted rendering workload, not a claim of interactive game FPS or acceptance of every authored campaign location. The game can retain its own frame cap. [Complete 24-view comparison](hardware/2026-10-28-castle-regression/matched/comparison.md), [all metrics and trial ranges](hardware/2026-10-28-castle-regression/matched/summary.csv), and raw per-frame CSV/provenance are committed alongside this report. Recompute with `python tools/summarize_world_performance.py development/performance/hardware/2026-10-28-castle-regression/matched`.

Typical Castle submission-thread allocation is now **0.079–0.150 MiB/frame**, versus **60–201 MiB/frame** before; median CPU submission is about 1.1–1.3 ms versus 27–67 ms. Median static mesh upload is zero in every sampled view. Real brightness changes can update retained RGB/VBO data; the separate unchanged-frame checks verify zero uploads, rather than pretending live relighting never transfers data.

All 64 Castle lights and all authored exhibits remain present. Fewer longer-range lights are not required for these savings and can enlarge influence/relighting volumes. A final asset check compared 1,354 selected original GLB/PNG/ZIP/water/sound resources against the reference package: zero missing and zero changed. Existing preliminary/rejected measurements remain under `preliminary/`; the superseded `0f886e63` Church result is not presented as the fixed result.

Cold preparation and newly exposed views can still hitch. The Castle exterior p99 remains about 50 ms even after warmup. The separate cold/waypoint House route has completed-frame p95 of 76 ms on this hardware. Physical installed navigation and a complete solo/TCP investigation-to-Castle loop remain separate acceptance work; the scoped warm measurements do not close those gates.

The harness calls its second pose `interior` across all Site types. In outdoor-native Hollow Creek that pose is an outdoor authored reference, not an indoor room. Existing user applications were left running; an installed-game process inspected at closeout was nearly idle. No other profiling jobs or builds ran during the final measurements.

## Supplementary 1280×800 result

One additional trial per view uses the same 90-frame warmup and 120 samples at 1280×800. This is supplementary evidence with less repetition than the primary experiment. Fixed medians are 18.8–22.9 ms and p95 values 22.0–27.4 ms over all ten sampled views. Examples: Castle exterior 87.73 → 22.89 ms median, House reference 63.26 → 21.39 ms, School reference 79.92 → 22.80 ms, native Hollow Creek reference 295.76 → 21.67 ms. Castle exterior p99 remains 65.52 ms. [Full comparison and raw data](hardware/2026-10-28-castle-regression/high-resolution/comparison.md).

## Final acceptance and local runnable preview

Runtime source `06b76cceafd82ea82869b531ada0dac9e062ea9c` is published on `performance/castle-regression` in [PR #36](https://github.com/mrcalzon02/The-Beckenburg-Project/pull/36). Main remained `ba3d8885` during acceptance. Final evidence/tool-only follow-up changes do not alter Java, assets or workflow behavior.

- [Renderer acceptance 40844278212](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40844278212): **all jobs passed**, including Java 17, Windows source/contract/native build, original Mesa torch reuse, all-ten-map GPU controls, live TV/map pixels and real solo UI entry. Original House torch repeat uploaded zero bytes and reused 17,536,032 bytes. No assertions were weakened.
- [Full acceptance 40844284430](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40844284430): **277 passed / 0 failed / 0 skipped**, packaged TCP round, desktop splash/menu/multiplayer/solo startup with progressing game frames, and Windows app-image checks passed. This independently verifies both TLS tests that AVG intercepted locally.
- [Source-checkpoint gallery 40844284423](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40844284423): passed. Final gallery has 1,042 original PNGs and passes local hash/dimension/index validation. Gallery and new captures are review-branch artifacts; the deployed main gallery is not claimed updated.
- Actual RTX 5070 original GPU smoke and expanded all-ten-map review passed. Separate six-cycle lifecycle memory, sparse-ID 96 MiB child JVM, live-pixel controls, translation ownership and three tier-caption UI captures passed within their stated scope.

A self-contained Windows app-image is available locally at `build/performance-preview/app/Beckenburg/Beckenburg.exe`; it contains the exact measured JAR and bundled Java 17. The native executable independently passed `--world-residency-review` across all ten map types with exit 0. Its JAR SHA-256 equals the benchmark JAR above; EXE SHA-256 is `6BD8F2A03701B865242A86C52983D0874B94D91824C31B0D6E27046805E8BCF4`. The app-image input contained only the GPU JAR, never test certificates/state. This creates a local preview rather than updating the installed application or publishing an installer.

Local clean-package rerun encountered Windows access errors while replacing existing generated resources; direct source compilation/Maven GPU packaging succeeded, asset parity passed, and clean CI package/full acceptance independently passed. The native review's first attempt likewise failed to overwrite an existing diagnostic CSV; a fresh isolated diagnostic directory passed. These failed attempts remain recorded rather than being misreported as successes.

## Early verification checkpoint (historical)

Clean Java 17 game package and offline Maven shaded GPU package built successfully. Exact PSX quantization parity, prepared scalar/normal light factors, shelf-label atlas/focus, Castle/map recording residency and existing focused architecture/lighting/cache checks passed. The actual RTX GPU residency review passed zero unchanged uploads, torch, flicker, blackout, door, resize and return checks. Full regression and latest-source revalidation are in progress; final counts and source/remote SHA will be appended.

Main baseline checks inspected: GPU `40828826828`, full multiplayer `40826909975`, installer `40826910008` passed at `ba3d8885`. Focused Castle `40826909984` failed an obsolete editor-menu source assertion after the earlier menu redesign; its route check is being updated to the real Extras entry without changing production editor behavior. The connected GitHub reader was unavailable, so authenticated `gh` provided current workflow evidence. Pending performance PR stacks remain isolated.

## Local full-manifest result and environmental limitation

The clean-package preliminary full run executed all 274 manifest entries: **272 passed, 2 failed, 0 skipped**. Failures are CommonNetworkTest and SecureLanIntegrationTest. Both remain failures in the retained output, not waived checks. A separate TLS handshake diagnostic confirmed `CERTIFICATE_UNKNOWN / Server identity changed (NET-03)`: the generated test certificate issuer was CN=Private Game Server, while the received certificate issuer was AVG Web/Mail Shield Self-signed Root. The test server identity was therefore substituted locally. Explicit IPv4 loopback did not change the result. No trust/pinning checks or antivirus settings were weakened. Exact-source CI must verify encrypted tests independently. All 272 passing checks include real socket gameplay tests, rendering, Castle construction and the four new performance contracts.

The preliminary full run used the package before the subsequent normal-preserving subdivision refinement and editor route-test repair. Those latest changes require scoped revalidation and an exact-source CI/full run before promotion.

## All-map exception found during matched experiment

Checkpoint 0f886e63 improved the major sampled paths but left the Church interior around33ms. Its unchanged frames still traced1630 shadows and uploaded roughly4.9MB. Investigation found a regression in the new generic retention helper: an empty structural-socket pass replaced the real hiding cupboard under the same owner key, recreating its chunk and GPU scene generation every frame. Empty lists now bypass auxiliary cache ownership. Both recording identity/array tests and real-GPU zero-repeat-upload checks are expanded from three representative maps to every HouseMap.Site type. The complete 0f886e63 measurements remain historical evidence; replacement final candidate runs must be tied to the corrected source.

## First checkpoint CI readback

Source checkpoint `0f886e63bfa52b1cc7b6ea7ef3a569341a4dd6ba` was pushed and its remote branch SHA read back, in draft PR36. Exact-source full acceptance40836192919 passed: **275 passed / 0 failed / 0 skipped**, packaged TCP round, GUI startup/lobby/House dispatch with advancing gameplay frames, and native Windows app-image startup. This independently exercises the TLS tests without this workstation's AVG substitution. Renderer40836185785 passed Java17 compile and Windows build/contract checks but **failed Mesa smoke**: a fixed House torch view reuploaded all17,158,176 mesh bytes with zero reused batches. That reveals the same empty-socket ownership collision found by the Church timing exception. The renderer gate remains failed until the corrected source passes; no test assertion was removed. Expanded all-map repeat checks and the existing torch reuse check will verify the repair.

## Corrected movement, memory and live-pixel checkpoint (2026-10-29)

The empty-socket fix now passes recording identity/revision checks and real-GPU zero-repeat-upload checks over all ten map types. The original House torch smoke that failed Mesa at0f886e63 now shows17,536,032 reused bytes and **zero mesh upload bytes** on the torch-only repeat. No assertion was removed.

Source audit also found the newly cached emission classifier treating the reserved live-map material ID `Integer.MAX_VALUE-2` as a dense BitSet index. Reserved/unregistered IDs are now excluded. A child-JVM regression successfully exercises these IDs with a96MiB heap.

Resident chunks now retain full close-range lighting topology rather than retessellating when a walking camera crosses a distance band. Source normals/UVs remain intact. The existing33-pose diagnostic improved from about268ms to40ms CPU p95 between staged candidates; its cold/waypoint completed-frame p95 remains76ms, so **first-visit/preparation hitches are not claimed eliminated**. Full map/flyover warmup and physical installed navigation remain separate acceptance work. The ordinary moving-camera tests now prove unchanged shared arrays/revisions under translation as well as rotation.

GPU live textures have independent source revisions. The backend now updates television/telemetry/map pixels when changed and avoids reuploading unchanged dynamic pixels or repeating the same upload for each draw. Actual GPU pixel controls verified TV and map image changes: changed frames uploaded32768/65536 texture bytes with zero geometry bytes; unchanged repeats uploaded zero texture and geometry bytes.

Six real-GPU lifecycle cycles through House, School, native Hollow Creek and Castle showed post-GC Java heap between663,052,960 and663,988,576 bytes (about633MiB), stable final1158 meshes/607 textures and14,486,784 shared-world geometry bytes. Temporary preparation heap was higher; this is a bounded lifecycle diagnostic with explicit GC, not a steady FPS measurement or hardware VRAM query.

Three actual offscreen Castle Swing-panel captures with completed GPU worlds verify full-resolution aimed captions for all tier rows. They create no desktop Window and use isolated anonymous preview state. Tools are preserved as WorldResidencyMemoryReview.java and CastleShelfLabelCapture.java. Latest-source hardware smoke/all-map checks passed; replacement repeated timings and exact-source CI are next.
