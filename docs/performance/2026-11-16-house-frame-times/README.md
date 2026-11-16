# House frame times, textures and multiplayer join stalls

The production comparison uses baseline `7e7764b208e82785ded237e2953473b2661e064b` and candidate `7225b34abd7da5213623e6e7ed4f79ed1657a0f4`. The first code checkpoint is `d00dfc575f2f800674ef3d6e9cd160e150bccd38`. The second restores the direct ray loop on camera motion. Both are on main. The project target is a steady-state p95 frame interval of 33.3 ms.

[Raw CSVs, reproduction drivers, functional checks and SHA-256 manifest](benchmark-data.zip)

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

A scratch experiment reused the GPU stationary-shadow archive for software lighting. Two room-1 receivers 2.3 cm apart shared its rounded key, although only one could see the source through door 2. Reopening the door returned RGB (0.9905, 0.8822, 0.7466) for the second point, against (1.0768, 0.9582, 0.8087) with a fresh visibility query. The change was rejected. A future test should use exact receiver identity and alternate two portals and windows before measuring reuse. The reproduction and captured light values are in `lighting/alias`.

## Gameplay and multiplayer

A legitimate three-hunter join/start/view sequence produced 357–360 pending outbound messages for a temporarily delayed peer. The old bounded queue closed its connection on message 257. With a 512-entry bound, all three delayed-writer runs finished the retreat, and a real three-seat round completed evidence sharing, ghost identification, ritual and both hunters’ payouts. The final source also passed disconnect, reconnect, heartbeats, van extraction and ghost-hearing checks. On the four-seat ghost route, 10 accepted movement steps yielded 3.10 m of travel, 26 muted packets, 316 listening packets and zero private-packet leaks.

The first checkpoint also passed hosted OpenGL/Mesa startup, WASD movement, retreat and return to Castle on House, Farmhouse and School. Its 33-waypoint House route recorded 118.07 ms end-to-end p95, 82.85 ms CPU-stage p95 and 14.59 ms readback/copy p95. The same run’s Windows compile and software-renderer checks passed. [Three-site capture and route CSV](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43977070266/artifacts/14282216290)

The same GPU job exercised Castle and 11 site configurations. Every unchanged repeat and torch-repeat recorded zero geometry upload bytes and zero shadow rays. The first Castle view required 1,044,044 rays and 33.51 MB of geometry upload. Opening a Castle door required 15,831 rays and 27.00 MB of geometry upload. A live monitor revision uploaded its changed texture with zero mesh bytes. These counters identify the specific transitions that still do work. They do not measure upload bandwidth or GPU execution time.

The final revision passed the Windows installer workflow’s real packaged multiplayer round, playable solo round, Castle and installer startup, and produced a checksummed Windows package. GitHub Releases returned HTTP 403 to the publication job. The verified build is retained as an artifact. A sibling full-multiplayer workflow passed the required game checks and was cancelled during its advisory manifest. [Windows build](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43977243730/artifacts/14282975977) · [Full multiplayer workflow](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43977243880)

## Local frame delivery

These values measure intervals between successive distinct GL frames consumed by the offscreen game panel on Java 17 and Mesa llvmpipe. They include scheduling and CPU OpenGL work on a heavily shared host. All rows below are final-source, successful processes. The ghost retry used a 600 MiB serial-GC heap. Failed processes, including exit 137 after an image was written, are excluded. The local host has no physical graphics device.

| House role and internal resolution | Idle intervals | p50 | p95 | p99 | Over 100 ms |
|---|---:|---:|---:|---:|---:|
| Solo hunter, 960×540 | 32 | 186.0 ms | 493.2 ms | 498.1 ms | 32 |
| Three-seat TCP hunter, 960×540 | 54 | 115.5 ms | 286.3 ms | 445.3 ms | 42 |
| Four-seat human ghost, 320×200 | 141 | 52.8 ms | 175.8 ms | 304.8 ms | 28 |

The raw interval files retain moving and idle phases separately. All three modes registered movement and advancing frame IDs. The TCP tests use separate real loopback connections. Independent-PC latency, display scan-out, other maps and a full installed campaign loop are outside this measurement.

## Physical Windows comparison

The private headful Core30 benchmark accepted both immutable revisions on the GTX 1660 SUPER and is running four counterbalanced pairs per mode. Hardware numbers will be added after the raw frame IDs are verified. [Physical run](https://github.com/mrcalzon02/Dens-Mortis/actions/runs/43978027320)

## Reproduction

The `texture/TextureComparison.java` probe runs from the GPU test classpath with `--assets`. Use the Java 17 build and the project's GPU dependencies plus `GpuTextureUploadProfile`. Use the same probe class on both source revisions. Each process first hashes the generated images and then records five preparations. The six independent processes used B/A, A/B, B/A order. Use `SoftwareLightingFrameReview <output-dir>` to record the six lighting phases. Its diagnostic from the candidate can run against baseline production classes.

For the isolated sanctuary comparison, see `sanctuary/README.txt` and `PairedHaze.java`. For the socket and live-panel workloads, see `network/README.md`, the two network drivers and the per-role CSVs. Each physical trial runs the repository's `LanViewportPerformanceReview --gpu [--solo] <output-dir>` and checks that distinct OpenGL frame IDs advance and that the character covers ground.
