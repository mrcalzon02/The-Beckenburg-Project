# House frame times, textures and multiplayer join stalls

The production comparison uses baseline `7e7764b208e82785ded237e2953473b2661e064b` and candidate `7225b34abd7da5213623e6e7ed4f79ed1657a0f4`. The first code checkpoint is `d00dfc575f2f800674ef3d6e9cd160e150bccd38`. The second restores the direct ray loop on camera motion. Both are on main. The Windows release also includes the hidden OpenGL window startup fix in `dc7e497e74f3b0303ef72e1c7b17df764a3ebf7d`. The follow-up for first sight of another hunter shipped in **v0.1.336**, source `13fcd0a4d3002e1084f0ab585865c6d8c1d18691`. The project target is a steady-state p95 frame interval of 33.3 ms.

[Engine probes, raw data and gameplay checks](benchmark-data.zip) · [Physical Windows OpenGL frame CSVs](windows-opengl.zip) · [Physical Windows software frame CSVs](windows-software.zip). Each archive includes a SHA-256 manifest.

[Continue from the benchmark handoff](HANDOFF.md) · [Movement and sanctuary reproduction probes](movement-probes.zip)

The four-lane sanctuary shader has been measured on the GTX 1660 SUPER. Solo and three-seat walking p95 reached 44.17 and 43.16 ms, with zero gaps over 100 ms across idle, walking and the sustained stop. [Physical shading comparison and raw data](windows-shading.md) · [Six-frame stop experiment](windows-stop.md) · [House door 2 physical upload results](door-2/physical-door.md)

The v0.1.338 Windows installer ships the measured four-lane shader and 30 ms visual timer from `eaa32a32438b2106d6733636b2bb7ce5e2ecb67a`. Packaged solo, three-seat full-round and installed Castle startup checks passed. Installer SHA-256 is `a9327a7fa4d7ef9dfc4e43ae48a6f47d1387dad40a16c752e5aa45a2ca94a17e`. [Release](https://github.com/mrcalzon02/The-Beckenburg-Project/releases/tag/beckenburg-v0.1.338) · [Exact-source Windows installer checks](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44002540893) · [First outward crossing, local stage trace](sanctuary-stop/van-exit-profile.md)

The 20 ms deadline measured 23.81 / 32.40 / 23.60 ms pooled p95 for solo idle, walking and stopping. Three-seat p95 was 23.88 / 32.67 / 25.86 ms. Two individual walking trials missed 33.3 ms. [GTX pacing, CPU, route and network evidence](windows-pacing.md)

## Direct measurements

| Workload | Baseline | Candidate | Measured scope |
|---|---:|---:|---|
| Prepare texture upload images | 65.48 ms CPU | 17.07 ms CPU | Median of 15 measured preparations in three fresh Java 17 processes per revision. Nine shipped models, 15 materials. |
| Allocation in texture preparation | 36.45 MB | 0.29 MB | Median allocated on the measuring thread per 6.05 MB set of RGBA images. |
| Sanctuary in a fixed van view | 26.46 ms CPU | 20.34 ms CPU | First 160 paired calls, 960×540. The second process measured 26.78 → 20.21 ms. |
| Sanctuary with a changing view | 25.93 ms CPU | 25.70 ms CPU | First 160 pairs. Second process measured 25.72 → 26.16 ms. Moving-frame tails varied across trials. |
| Decode 53,850 ordered TCP frames on loopback | 25.34 ms CPU | 13.48 ms CPU | Median receive-thread CPU in six paired trials per reader. |
| TCP receiver allocation for that replay | 27.89 MB | 9.15 MB | Includes all decode and consumer work in the socket probe. |

Texture timings begin after the source PNGs have been decoded. They exclude OpenGL transfer, scene load, and draw time. The baseline and candidate produced the same 6,050,568 RGBA bytes, SHA-256 `1eb55a8a247b5965866f28dc381707d02689ba511e11aa979d3c3532ffb085df`. The runtime shares packed pixels across identical imported PNGs. The per-material sampler retains the authored UV origin, wrap modes, tint, mask threshold and alpha. The final build matched 114,688 UV, RGB and alpha lookups against the reference sampler, including integer and out-of-range UVs. It also passed live surface, television, van map and CRT updates. Editor substitutions continue to read their current images.

Packing the source pixels has a measurable cost. The 12 distinct decoded rasters for those nine models occupy 3.69 MB versus 3.33 MB previously. In one cold model-only harness, median loading CPU rose from 245 to 348 ms. In a separate test that loaded the same models and prepared their first GPU upload images, the combined median was 653 → 502 ms (three freshly launched Java 17 processes per revision). Those timings exclude transfer to the device. First-case frame delivery is measured separately. A prototype that retained the native byte rasters saved the 358,930 bytes but increased combined load and first preparation CPU from 399.58 to 497.92 ms in a separate four-pair experiment. We kept the packed representation.

The sanctuary cache retains the optical intersection for a repeated camera pose and sanctuary shape. Every draw applies the current scene depth and animation clock. The moving view uses the direct loop. Across 1,626,800 analytic test samples, the original and optimized paths agreed on RGB, changed-pixel count and raw depth. Two independent crossover processes gave paired median fixed-camera CPU reductions of 23.3% and 24.1%. The paired moving-camera CPU changes were +0.05% and +0.84%.

## Lighting and visual parity

At the House doorway, the deterministic six-phase route covers a closed and opened door, lamp off, white and flame light, near-plane geometry and UV. All 72 final-candidate RGB and raw depth buffers matched the pinned pre-optimization baseline frame for frame. The six-phase intermediate benchmark also matched for all 216 candidate frames. Three interleaved software trials per version on a shared Linux host (24 warmup and 72 measured frames per trial, 320×200) gave median renderer-thread CPU of 87.92 → 83.10 ms, p95 171.73 → 157.65 ms and median allocation 23.57 → 21.08 MB/frame. This series started at `d5222b831cf7f7946623aecca21983cbb3469a1c` and used the candidate before the final moving-loop change. The exact-branch comparison is under `software/final`. The archive's per-frame times are in ns and allocations are in bytes.

The hosted Windows doorway profiler on the first checkpoint measured p95 **62.76 ms stationary and 91.29 ms on a door toggle** in the software renderer at 320×200. A toggle cast a median 1,846 static visibility rays. The CPU submission recorder went from 8.48 ms p95 stationary to 77.97 ms on toggles and revised a median 18 resources. On fixed-camera repeat openings, shadow work was still measurable after warmup. Frame construction, software lighting and actual GPU time are distinct quantities. [Hosted measurements](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43977070247/artifacts/14282695214)

At the House door-2 pose, median CPU time preparing and submitting geometry rose from 4.25 ms while idle to 52.17 ms on a toggle. Median scene construction was 0.88 ms idle and 0.50 ms on a toggle. A replay of the same 24 isolated transitions identified which geometry changed. Every toggle revised 18 resident buffers (12,558 vertices, 401,856 bytes). The CPU probe compared all eight float fields in every vertex across all 432 revisions. All XYZ positions and UVs were bit-identical. Only RGB changed, on 3,938–3,944 vertices per toggle. The worn hiding cupboards accounted for 218,112 bytes and the wooden shell finishes for another 117,504. A separate contiguous RGB buffer for those same batches would contain 150,696 bytes, 62.5% less resident data per update. Device cost and frame-time impact need a matched test. The probe and raw per-field counts are under `lighting/door-resident`. The next CPU experiment can retain exact per-vertex geometry and recompute affected lights for doors and windows. Test the CPU and upload changes separately, with alternating doors, fixed lights, frame hashes, and an OpenGL context restart. [Chunk rebuild](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/7225b34abd7da5213623e6e7ed4f79ed1657a0f4/src/main/java/org/beckenburg/SceneRenderer.java#L3328-L3388) · [OpenGL mesh upload](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/7225b34abd7da5213623e6e7ed4f79ed1657a0f4/gpu-client/src/main/java/org/beckenburg/gpu/OpenGlWorldRenderer.java#L297-L333)

A scratch experiment reused the GPU stationary-shadow archive for software lighting. Two room-1 receivers 2.3 cm apart shared its rounded key, although only one could see the source through door 2. Reopening the door returned RGB (0.9905, 0.8822, 0.7466) for the second point, against (1.0768, 0.9582, 0.8087) with a fresh visibility query. The change was rejected. A future test should use exact receiver identity and alternate two portals and windows before measuring reuse. The reproduction and captured light values are in `lighting/alias`.

## Gameplay and multiplayer

A legitimate three-hunter join/start/view sequence produced 357–360 pending outbound messages for a temporarily delayed peer. The old bounded queue closed its connection on message 257. With a 512-entry bound, all three delayed-writer runs finished the retreat, and a real three-seat round completed evidence sharing, ghost identification, ritual and both hunters’ payouts. The final source also passed disconnect, reconnect, heartbeats, van extraction and ghost-hearing checks. On the four-seat ghost route, 10 accepted movement steps yielded 3.10 m of travel, 26 muted packets, 316 listening packets and zero private-packet leaks.

The first checkpoint also passed hosted OpenGL/Mesa startup, WASD movement, retreat and return to Castle on House, Farmhouse and School. Its 33-waypoint House route recorded 118.07 ms end-to-end p95, 82.85 ms CPU-stage p95 and 14.59 ms readback/copy p95. The same run’s Windows compile and software-renderer checks passed. [Three-site capture and route CSV](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43977070266/artifacts/14282216290)

The same GPU job exercised Castle and 11 site configurations. Every unchanged repeat and torch-repeat recorded zero geometry upload bytes and zero shadow rays. The first Castle view required 1,044,044 rays and 33.51 MB of geometry upload. The transition from the Castle blackout to an open gate with the lights restored required 15,831 rays and 27.00 MB of geometry upload. Both states changed in that capture, so it cannot isolate the gate cost. In a separate local Mesa capture with power held on, the first gate opening uploaded 454,560 bytes and the second 375,072. Both openings cast 15,831 rays. The unchanged repeats uploaded zero mesh bytes and cast zero rays. These are counters from ten verified GL frames. The process later hit the host memory limit, before the independent power-restoration case. The raw partial trace is under `lighting/castle-gate-partial`. A live monitor revision uploaded its changed texture with zero mesh bytes. These counters identify the specific transitions that still do work. They do not measure upload bandwidth or GPU execution time.

The first packaged performance build at `7225b34` passed its multiplayer round, playable solo round, Castle and installer startup. Its release publication returned HTTP 403. The subsequent installer workflow at `dc7e497` succeeded and published **v0.1.335** with the Windows x64 installer and SHA-256 checksums. All jobs in its renderer workflow passed, including the full OpenGL UI route on Mesa. The earlier full-multiplayer workflow passed its required gameplay checks and was cancelled during its advisory manifest. [v0.1.335 release](https://github.com/mrcalzon02/The-Beckenburg-Project/releases/tag/beckenburg-v0.1.335) · [Release workflow](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43981343669) · [Renderer workflow](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43981343688) · [Windows build](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43977243730/artifacts/14282975977) · [Full multiplayer workflow](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43977243880)

## Local frame delivery

These values measure intervals between successive distinct GL frames consumed by the offscreen game panel on Java 17 and Mesa llvmpipe. They include scheduling and CPU OpenGL work on a heavily shared host. All rows below are final-source, successful processes. The ghost retry used a 600 MiB serial-GC heap. Failed processes, including exit 137 after an image was written, are excluded. The local host has no physical graphics device.

| House role and internal resolution | Idle intervals | p50 | p95 | p99 | Over 100 ms |
|---|---:|---:|---:|---:|---:|
| Solo hunter, 960×540 | 32 | 186.0 ms | 493.2 ms | 498.1 ms | 32 |
| Three-seat TCP hunter, 960×540 | 54 | 115.5 ms | 286.3 ms | 445.3 ms | 42 |
| Four-seat human ghost, 320×200 | 141 | 52.8 ms | 175.8 ms | 304.8 ms | 28 |

The raw interval files retain moving and idle phases separately. All three modes registered movement and advancing frame IDs. The TCP tests use separate real loopback connections. Independent-PC latency, display scan-out, other maps and a full installed campaign loop are outside this measurement.

## Physical Windows comparison

The Core30 reference PC used an Intel i7-10700, NVIDIA GeForce GTX 1660 SUPER (Windows driver 32.0.16.1088), Windows 10 Home 19045 and Temurin 17.0.21. For each renderer we requested four pairs, each with a fresh Java process per revision and mode, alternating both revision and mode order. Solo ran a local investigation. Multiplayer rendered one hunter with a second hunter and a human-role ghost connected over real loopback TCP. Each case warmed up for ten renders plus four seconds, idled for 20 seconds and held movement for three seconds. Timings describe the offscreen game panel on one physical PC. The 33.3 ms target applies to individual frame intervals.

### Windows software renderer

All 16 trials and both authoritative full TCP rounds passed. These use the game's software renderer at 640×360, comparing `7e7764b` with `7225b34`. Each interval ends when the renderer finishes a new frame. A trial p95 uses the nearest-rank 95th percentile. Each paired trial carries equal weight in the median comparison. Pooling combines all frame intervals and can put more weight on a faster trial. [Windows software run](https://github.com/mrcalzon02/Dens-Mortis/actions/runs/43980855135)

| House run | Median trial p95, before → after | Trials with lower p95 | Pooled p95, before → after | Candidate pooled p99 | Candidate gaps over 100 ms |
|---|---:|---:|---:|---:|---:|
| Solo, idle | 167.6 → 139.7 ms | 4/4 | 167.8 → 140.0 ms | 141.8 ms | 587 / 587 |
| Solo, walking | 136.3 → 136.9 ms | 1/4 | 136.6 → 137.1 ms | 140.8 ms | 76 / 105 |
| TCP hunter, idle | 170.8 → 140.1 ms | 4/4 | 171.3 → 170.4 ms | 173.0 ms | 567 / 567 |
| TCP hunter, walking | 152.7 → 142.1 ms | 3/4 | 160.7 → 143.0 ms | 548.9 ms | 76 / 91 |

Every recorded software interval on both builds exceeded the 33.3 ms target. In idle solo play all four trial p95s improved. Solo walking p95 was essentially unchanged. The TCP idle result varied across runs, with a pooled p95 of 170.4 ms even though the median candidate trial p95 was 140.1 ms. In TCP walking, only 90 baseline and 91 candidate intervals entered the pooled distribution. Their p99s were 450.1 and 548.9 ms. All four candidate TCP walking trials recorded a single 327–438 ms post-geometry stall shortly after starting to walk. No candidate solo trial recorded a post-geometry spike above 200 ms. Meanwhile, median candidate idle software geometry cost was 109.3 ms solo and 113.6 ms over TCP. At the median, scene assembly took 0.7 ms in both modes. Frame timestamps and per-stage measurements are included in the archive.

### Cold first sight of another hunter

The 327–438 ms post-geometry spike occurred once in each of the eight multiplayer walking trials across the original two builds. All eight solo trials lacked that spike. On a separate instrumented three-seat House run, the first valid draw of another hunter at 1.63 m synchronously decoded the directional sprites and hunter animation art on Swing's event thread. Cold-load probes counted 61 directional entries and 36 hunter animation frames. Visiting Castle first can already warm some of the art.

The follow-up in [13fcd0a](https://github.com/mrcalzon02/The-Beckenburg-Project/commit/13fcd0a4d3002e1084f0ab585865c6d8c1d18691) begins loading the same art off the renderer thread after the first validated remote hunter position arrives. First visibility still uses the normal image/depth drawing path. In two fresh-process local pairs, time drawing the first peer fell from **261.8 to 4.3 ms** and from **160.9 to 2.2 ms**. The full completion interval at first sight fell from 413.9 to 213.6 ms and from 267.8 to 101.1 ms. Whole-walk p95 varied on the shared Linux host. A solo case traveled 7.75 m and loaded neither atlas. Existing live multiplayer sprite tests passed for all three hunter classes, including walking, stopping, crouching and depth occlusion. The exact release source also passed a complete TCP round locally. The v0.1.336 installer workflow passed the full packaged multiplayer investigation, playable solo investigation and installed Castle startup. Its published 761,100,400-byte Windows x64 installer has SHA-256 `f84460daf697cc3b411e9116ad90a47f552cce8f8653c16c700a1587a6fcbe6f`. [Release](https://github.com/mrcalzon02/The-Beckenburg-Project/releases/tag/beckenburg-v0.1.336) · [Installer checks](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43985343393). Instrumentation, both paired trials and functional logs are in `network/first-visible-peer`.

The four-pair Windows software follow-up finished. In all four cold TCP walks, peak post-geometry work fell from 319–328 ms to 28–29 ms. The longest complete-frame gaps fell from 431–440 ms to 137–140 ms. [Trial results and checksummed raw frames](core30-windows-software.md). [Follow-up run](https://github.com/mrcalzon02/Dens-Mortis/actions/runs/43985468339)

### Windows OpenGL renderer

The first physical GPU attempt reached hidden-window creation and failed when GLFW tried to center it on a missing primary monitor in Windows service Session 0. Each side completed a full TCP round. Four native launches ended before the first measured frame. The v0.1.335 launcher explicitly positions the hidden window. The subsequent A/B used `7e7764b` with the same one-line positioning change on its launch classpath and the unmodified `dc7e497` release candidate. [First run](https://github.com/mrcalzon02/Dens-Mortis/actions/runs/43978027320) · [Matched GPU retry](https://github.com/mrcalzon02/Dens-Mortis/actions/runs/43982264387)


The retry succeeded in all 16 timing cases on the physical NVIDIA OpenGL 3.3 context. The Swing panel measured 640×360. All 10,029 recorded renderer frames used an internal 960×540 framebuffer. Both complete multiplayer rounds passed. Every frame interval below spans distinct OpenGL frame IDs consumed by the panel. The Windows 10 driver reported NVIDIA 610.88.

| House run | Median trial p95, before → after | Trials with lower p95 | Pooled p95, before → after | Candidate pooled p99 | Candidate gaps over 100 ms |
|---|---:|---:|---:|---:|---:|
| Solo, idle | 70.3 → 36.0 ms | 4/4 | 70.3 → 35.9 ms | 37.3 ms | 0 / 2368 |
| Solo, walking | 71.2 → 73.4 ms | 0/4 | 71.9 → 73.1 ms | 123.7 ms | 8 / 282 |
| TCP hunter, idle | 70.3 → 35.6 ms | 4/4 | 70.3 → 35.8 ms | 36.7 ms | 0 / 2372 |
| TCP hunter, walking | 75.6 → 78.6 ms | 1/4 | 76.5 → 83.5 ms | 373.6 ms | 10 / 255 |

Solo idle gaps over 50 ms fell from 1,176/1,178 to 2/2,368. The three-seat hunter fell from 1,179/1,181 to 0/2,372. Moving results were worse on this build. Solo walking went from no intervals exceeding 100 ms to eight across four trials. Every candidate solo trial had two early long frames, with approximately 87–95 ms in post-geometry work, under 2.1 ms in geometry and zero shadow rays. The TCP walking p99 was 370.6 ms before and 373.6 ms after. The hardware CSVs capture composited application delivery, CPU stages and movement. They exclude monitor scan-out and independent-PC latency. Original worker ZIP SHA-256 `e2aa33303c0dadc7eafefbc8cc3fc6ff0041a5a53ed6a3e6263c75ed0dc94ed6`.

### Unshipped movement experiment

A real instrumented solo House run on Mesa traced a 157.3 ms render-thread CPU spike to the first sanctuary cache fill at a repeated camera pose. A scratch prototype waits for six identical poses before filling. Across two fresh-process comparisons, the route’s walking p95 went from 146.5 to 108.0 ms and from 125.7 to 104.8 ms. Every measured color and depth hash matched in 800 deterministic replay frames. Short synthetic holds showed mixed CPU results, and retaining the direct path for five frames cost some reuse. The normal route leaves the dome before stopping. The subsequent physical test repeated the exit and added a single step and a sustained stop **inside** the sanctuary. Both cache experiments removed all observed walking intervals over 100 ms. Stopping p95 remained near 70 ms. [Four-pair GTX results, code provenance and raw frame CSVs](windows-stop.md). The prototype is excluded from v0.1.336. [Source patch, probes and raw local timings](movement-probes.zip)

## Reproduction

The `texture/TextureComparison.java` probe runs from the GPU test classpath with `--assets`. Use the Java 17 build and the project's GPU dependencies plus `GpuTextureUploadProfile`. Use the same probe class on both source revisions. Each process first hashes the generated images and then records five preparations. The six independent processes used B/A, A/B, B/A order. Use `SoftwareLightingFrameReview <output-dir>` to record the six lighting phases. Its diagnostic from the candidate can run against baseline production classes.

For the isolated sanctuary comparison, see `sanctuary/README.txt` and `PairedHaze.java`. For the socket and live-panel workloads, see `network/README.md`, the two network drivers and the per-role CSVs. The hardware worker runs the repository's `LanViewportPerformanceReview [--gpu] [--solo] <output-dir>` in a fresh process for each case. The OpenGL measurements use intervals between successive distinct non-negative frame IDs consumed by the panel. The CPU renderer measurements use consecutive render-completion timestamps. Each trial also checks movement. The worker separately runs full-round multiplayer acceptance on both revisions.
