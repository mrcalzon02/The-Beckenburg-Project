# Retained live-pose geometry — 2026-10-29

Source `e8cec914dd492135edc612d40bd452d4f04e5767`, [PR #38](https://github.com/mrcalzon02/The-Beckenburg-Project/pull/38), stacked on PR #37. JFR from the previous checkpoint identified repeated `looseClutter` and `imported -> eventFixtures` Point/Face construction as the largest sampled warm geometry allocations.

The renderer now retains CPU source faces by current clutter state or fixture mesh pose. A changed pose replaces that object's entry; hidden/deleted objects and scene changes release entries. The cache admits at most 120,000 triangles, then renders excess objects through rebuilding without evicting the warm set. These remain dynamically lit/transient GPU submissions. Alarm yaw, fixture indicators, water and television pixels still update through their existing paths. Use `-Dbeckenburg.dynamic.geometry.cache.disabled=true` for fallback.

## Verification

Java 17.0.12 + Blender 5.0 game packaging and offline Maven GPU packaging passed. `DynamicGeometryRetentionTest` compares every source face (position, normal, UV, material, tint and order) against rebuilding across all ten map types, camera turns, moved/rotated/held/ghost-redacted objects, alarm animation, water/power, deletion and scene transitions. Its 500-bowl stress scene has 158,000 clutter triangles alone, exceeding the 120,000 admission limit; the complete geometry still matches rebuilding. Skipped admissions become cacheable after deleted objects release their budget on the next frame.

Existing LooseClutterTest, LooseClutterNetworkTest, HauntedFixturesTest, PreparedSceneryRetentionTest, PhysicalLightingTest and the all-ten-map WorldResidencyTest passed. The initial new test expected all fixtures to be immediately admitted on the same frame that a full clutter set was removed; its expectation was corrected to verify immediate clutter disposal plus next-frame fixture admission, retaining complete geometry parity throughout. No production assertion was removed.

Actual NVIDIA RTX 5070 / 616.64 OpenGL review passed Castle and all ten map types, including zero unchanged mesh uploads, handheld torch, candle flicker, blackout, doors, resize, return and live TV/map pixels. All 12 diagnostic PNG hashes match the previous source `36a41c70` exactly. All 18 final before/after benchmark captures also match exactly. These captures are scripted runtime diagnostics, not installed player navigation or complete map visual acceptance.

Six separate lifecycle cycles through House, School, native Hollow Creek and Castle returned to 662,263,336–663,428,512 post-GC Java heap bytes, exactly 1,158 backend meshes / 607 textures and 14,486,784 resident geometry bytes. Raw `lifecycle.csv` records intermediate peaks too. Explicit GC is used in this diagnostic; it is not an FPS test or VRAM measurement.

GPU JAR SHA-256: `36247DC77477A3E6491B0FB6AD7010AAB1617A6B370CD08DF97AF01F1B63F92B`.

## Three-trial hardware comparison

The primary comparison uses the **untouched parent renderer** `e0b93b66` compiled into an overlay against the same candidate package/backend/assets. The repository's `profile_world_performance.ps1` records overlay class hashes, JAR hash and isolated settings in each `provenance.json`. Parent trials ran first, followed by candidate trials. Each site has three trials, 90 warmup frames and 120 measured frames per view, 640x400, default PSX preset, identical scripted camera sweep. No concurrent local rendering test ran during these profiles.

Raw CSVs/environment files are under `before/` and `after/`; pooled frame percentiles are in `comparison.md`, `summary.csv` and `summary.json`. Trial medians and their ranges remain in the summaries. Results do not prove statistical significance or physical installed acceptance.

| Site / view | Parent median allocated MiB | Candidate median allocated MiB | Allocation reduction |
| --- | ---: | ---: | ---: |
| House / van | 4.699 | 0.267 | 94.3% |
| House / interior | 5.363 | 0.932 | 82.6% |
| Farmhouse / van | 6.081 | 0.378 | 93.8% |
| Farmhouse / interior | 7.141 | 1.238 | 82.7% |
| School / van | 5.360 | 0.531 | 90.1% |
| School / interior | 7.346 | 2.517 | 65.7% |

Pooled median completed-frame time improved approximately 3–7% for Farmhouse and School, while House was effectively unchanged. This is primarily an allocation/CPU-preparation improvement; it does not eliminate cold preparation, GPU readback stalls or all tail latency.

Source validation: [GPU 40873064007](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40873064007) and [full gameplay 40873068301](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40873068301) were still running at this checkpoint. Full source acceptance, physical installed play, main merge and release remain pending until independently verified. Later evidence/gallery commits do not modify this runtime source.
