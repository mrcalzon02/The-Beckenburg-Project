# Castle and investigation-map performance regression

Status: implementation and acceptance in progress. No release or complete A–G claim.

The user requested comparison with the smooth previous Castle, diagnosis and repair of the rebuilt Castle and investigation levels, and readable shelf labels. Work is isolated on `performance/castle-regression` from main `ba3d8885f07487af94b0c9aae6a6d4563e21eb58`. The previous Castle reference is `68d1142d62a6237ff2567a54868b568b9f711487`, immediately before construction commit `8a854a7e`. The pre-fix current source is main `ba3d8885` with timing instrumentation only.

## Reproduced differences and causes

- At the courtyard reference, source geometry rose from 55,147 triangles to 220,671; fixtures rose from 31 light sources to 64. The rebuild added 129 tiered equipment exhibits, 15 retail racks, thick masonry/slabs, additional dressing and the revised surrounding terrain. Old distance filters also built fewer objects in distant views. Counts are view-specific where the old source culled during construction.
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

## Preliminary results, before final repeat runs

| View | Pre-fix median ms | Candidate median ms | Candidate p95 ms |
|---|---:|---:|---:|
| Castle courtyard | 35.55 | 8.76 | 13.77 |
| Castle store | 38.91 | 9.29 | 13.21 |
| Castle library | 35.40 | 9.20 | 14.90 |
| Castle exterior | 73.07 | 11.90 | 24.43 |
| House van | 36.16 | 9.51 | 15.35 |
| House interior | 23.40 | 9.28 | 11.36 |
| School van | 53.79 | 14.30 | 21.19 |
| School interior | 39.16 | 12.35 | 19.40 |
| Hollow Creek native view | 337.03 | 9.19 | 11.41 |

These preliminary runs used different sample/warmup lengths and do not replace the final matched repeated experiment. Some newly exposed views and first uploads still hitch. Cold preparation, installed solo/TCP rounds, all-map coverage, memory stability and exact-commit CI/release acceptance remain separate gates. All 64 Castle lights and all authored exhibits remain present; fewer longer-range lights are not required for these savings and can enlarge influence/relighting volumes.

## Verification checkpoint

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
