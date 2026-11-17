# CPU lighting work during House door 2 swings

The vertex-light calculation can reuse the previous open and closed colors for resident world objects such as cupboards. The source-aware prototype verifies both poses against full rebuilds before allowing reuse. The camera and player flashlight must remain in a stable state. New rooms, camera movement, flashlight transitions, active UV or portable overlays, structural edits and incompatible portal or light states invalidate reuse. The architectural shell continues to use its current lighting path. On shared Mesa, the median toggle and count over 100 ms improved. The synchronous p95 rose in both camera cases. The subsequent GTX test reproduced the median gain, raised intermediate-swing p95 in all eight pairs and left the hitch counts unchanged. The prototype remains experimental. [Physical CPU result and raw frames](physical-door-cpu.md)

## Timed player actions on Mesa

These trials use the game's 450 ms door motion, Windows-worker-compatible Java 17 GPU client, software Mesa OpenGL and the normal player flashlight. The host simulation ticks every 100 ms. After each real door interaction the test renders the first changed frame, four intermediate samples, and a settled frame at least 500 ms after the interaction. The step moves the viewpoint 0.31 m on toggle 11 and leaves it there. Each fresh process has 8 warmup frames and 24 toggles. This local experiment used one Mesa worker thread, two visible Java processors, Serial GC and a 210 MiB heap. The time includes synchronous color and depth readback. GL issue and worker times are recorded separately in every CSV.

All times below are in milliseconds. Each cell gives nearest-rank p50, p95, p99 and count over 100 ms from two fresh processes. The first set measured both baseline camera cases before the final candidate. The repeat measured standing baseline then candidate, and stepped candidate then baseline. These shared-host timings are exploratory.

| Camera | Frame | Count per arm | Baseline CPU | Baseline synchronous | Candidate CPU | Candidate synchronous |
|---|---|---:|---:|---:|---:|---:|
| Standing | toggle | 48 | 81.27 / 223.14 / 257.46 / 7 | 159.35 / 319.51 / 509.75 / 47 | 29.30 / 117.69 / 233.29 / 4 | 79.17 / 419.58 / 634.16 / 17 |
| Standing | swing | 192 | 9.45 / 30.36 / 155.96 / 2 | 40.52 / 86.60 / 266.68 / 8 | 9.61 / 46.15 / 56.90 / 0 | 47.34 / 128.45 / 170.56 / 19 |
| Standing | settled | 48 | 10.60 / 23.53 / 145.54 / 1 | 42.93 / 90.07 / 272.97 / 2 | 9.06 / 16.69 / 17.48 / 0 | 45.61 / 88.10 / 89.37 / 0 |
| One step on toggle 11 | toggle | 48 | 72.01 / 209.08 / 267.39 / 4 | 154.94 / 282.26 / 432.93 / 47 | 31.81 / 230.61 / 286.79 / 5 | 77.99 / 286.31 / 563.65 / 19 |
| One step on toggle 11 | swing | 192 | 9.07 / 21.22 / 26.30 / 0 | 38.34 / 69.89 / 120.86 / 3 | 10.09 / 49.46 / 62.79 / 0 | 41.55 / 95.73 / 125.38 / 7 |
| One step on toggle 11 | settled | 48 | 13.95 / 22.11 / 23.29 / 0 | 42.62 / 65.12 / 121.93 / 1 | 11.14 / 22.11 / 25.93 / 0 | 44.04 / 62.24 / 73.25 / 0 |

The first toggle in each fresh process preceded any certified alternate pose.

| Camera | First-toggle baseline completion, trials 1 / 2 | First-toggle candidate completion, trials 1 / 2 |
|---|---:|---:|
| Standing | 307.61 / 319.51 | 634.16 / 321.00 |
| One step on toggle 11 | 223.98 / 282.26 | 286.31 / 285.12 |

Cumulative restored vertex counts per process were standing 2,790,081 and 2,790,081, one step on toggle 11 2,249,037 and 2,249,037. They count world-object vertices whose lighting payload could be restored, including objects whose final colors stayed identical. The GPU still uploads revised buffers through the existing renderer.

## Frame and shadow comparisons

The settled-state doorway test, the three seeded 160-frame routes and the moving-leaf fixture all use known camera, lighting and aperture states. The three mixed routes compare all 480 complete color and raw-depth frames, plus upload counts and bytes. They visit House, School and a new House instance, change the hiding layout, circuits, breaker and fixtures, use UV and the lamp, look back from the other side of the threshold and reverse the action order. The moving-leaf fixture holds each of the renderer's 17 1/16 poses through capture. All 144 full-color and raw-depth frames matched with the player flashlight on, including 8 opening and closing transitions. The earlier 58-frame route is included for the comparison with the prior prototypes.

Real-time swings can visit different intermediate poses before both renderers settle. The values below give ARGB mismatch count / raw depth mismatch count among the 24 settled frames in each comparison. A/A and B/B repeat the same executable. The independent 17-pose fixture isolates raster output from elapsed-time scheduling.

| Camera | Baseline A/A | Candidate B/B | A/B first pair | A/B second pair |
|---|---:|---:|---:|---:|
| Standing | 5 / 0 | 7 / 0 | 6 / 0 | 8 / 0 |
| One step on toggle 11 | 2 / 0 | 3 / 0 | 2 / 0 | 1 / 0 |

The original 23 mm receiver witness still exposes the renderer's existing rounded diffuse-shadow alias. The source-aware cache reproduces the existing bits, including the incorrect open-pose light on one of the two receivers. The separate exact-shadow experiment passes both receiver orderings and changes 56 of 58 archived full-color images. Its depth hashes match. [Doorway shadow and pixel comparison](README.md)

The budget test generated 9,000 unique resources. The final prototype reached 16,777,072 charged bytes and 8,192 descriptors under a 16 MiB alternate-payload and factor-snapshot budget. After eviction, a revisit triggered a cache miss. A changed source factor, portal state or structural epoch also triggered a miss. Clearing the scene released all records. Six original shadow, occlusion and depth contracts and a complete three-player TCP evidence/ritual/settlement round passed with the final overlay.

## Why the alternatives were kept separate

- Replaying all tessellated receivers preserved all original frames but raised render-thread CPU p95 from 158.20 to 175.22 ms across 48 toggles per arm.
- Caching both shell and world-object payloads accelerated a fixed view. Returning to a different viewing angle changed 365 of 64,000 pixels at frame 61 of the reversed-camera route. The maximum channel difference was 5/255 with identical alpha and depth.
- A single global factor generation made the resident-object cache look effective with a frozen clock. At the real 100 ms lighting tick it restored zero vertices over 24 half-second door toggles in both standing and stepped cases. Only six room-3 chandelier sources changed factors during this test. The revised cupboard materials depended on other source IDs, 0, 1 and 22. Across standing and stepped trials, all 230 and 210 revisited world-object materials respectively matched their prior per-source raw factor bits and complete vertex arrays. This includes 138 and 126 cupboard revisits.
- The per-material version still cleared every frame when the default player flashlight was on. Both continuous swing routes restored zero vertices under that guard. The final prototype clears on a change of handheld flashlight state and continues to evaluate the current handheld beam in the existing shader.

## Raw files and reproduction

Each experimental patch applies to the renderer in a633e8d937a9c928c90cc68c118b5507b123de5b. Apply only the selected patch per checkout. Build the Java 17 GPU client and original test dependencies. Compile the included Java probe against the game JAR with its shipped resources, libGDX and LWJGL. Run a fresh JVM for each arm, place the probe and optional overlay classes before the original JAR on the classpath, and pass an output CSV path with an existing parent directory. The normal standing swing uses org.beckenburg.DoorSwingClockReview. Set the JVM system property door.step=true for the stepped route. org.beckenburg.DoorPoseLatticeReview exercises all 17 native leaf poses. The trial data includes exact global and per-material factor traces, optional incomplete host-memory traces, raw frame/CPU data, match receipts, relevant per-pixel buffers and an internal SHA-256 manifest.

[Raw traces, code and member SHA-256 sums](door-cpu-experiments.zip) · [Verification and percentile calculation](verify_door_cpu.py) · [Physical RGB-only upload measurements](physical-door.md)
