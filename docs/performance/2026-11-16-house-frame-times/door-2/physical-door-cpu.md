# House door 2 CPU lighting on the physical GTX 1660 SUPER

The source-aware CPU lighting cache cut median toggle time in every pair, from 81.32 to 34.85 ms standing and 81.55 to 36.48 ms on the stepped route. It roughly doubled intermediate-swing p95 in all eight pairs. The candidate kept every over-100 ms interaction and raised the toggle p95 in all four stepped pairs. The prototype remains experimental.

Core30 ran Java 17.0.21 on Windows 10 Home, with an Intel i7-10700 and NVIDIA GeForce GTX 1660 SUPER (driver 32.0.16.1088). Both arms used the same packaged game at `5d7c933b7d49e98f9dd9484d1049b98781fba699` (JAR SHA-256 `cb890ef0a0324c527d8659a3ea7633ab2e7ae13b4f4100e1f959c41e750deb06`). The candidate placed the reviewed source-aware world-object lighting classes first on the classpath. Each camera had four counterbalanced pairs with one fresh JVM per arm. The gameplay check used real loopback TCP. [Physical run](https://github.com/mrcalzon02/Dens-Mortis/actions/runs/44003061335) · [Worker at its run revision](https://github.com/mrcalzon02/Dens-Mortis/blob/83a03869c94f04ff7c0717085a6729922ad81371/tools/local-worker/beckenburg_performance.py)

House door 2 uses its authored 450 ms animation. Each live process warmed for eight frames, then opened and closed the door 24 times. The first submitted frame per toggle, four intermediate samples and the settled frame after at least 500 ms include full 320 × 200 ARGB and raw-depth readback. The player flashlight was on. The stepped route moves 0.31 m on toggle 11 and holds that position for the remaining 14 interactions. Timings exclude the gap between samples. The evidence measures submission through completed readback at this doorway. Monitor presentation and continuous full-screen movement require separate measurements.

## Physical frame times

Cells give nearest-rank p50 / p95 / p99 in milliseconds and the number of frames strictly over 100 ms. Every arm has 96 first-changed frames, 384 intermediate frames and 96 settled frames per camera. Four first-ever samples make the first-ever p95 and p99 the maximum of the four.

| Camera | Frame | Per arm | Baseline synchronous | Candidate synchronous | Baseline render-thread CPU | Candidate render-thread CPU |
|---|---|---|---|---|---|---|
| Standing | All toggles | 96 | 81.32 / 98.20 / 266.85 / 4 | 34.85 / 96.85 / 227.63 / 4 | 78.12 / 93.75 / 250.00 / 4 | 31.25 / 93.75 / 218.75 / 4 |
| Standing | First-ever toggle | 4 | 218.57 / 266.85 / 266.85 / 4 | 212.29 / 227.63 / 227.63 / 4 | 218.75 / 250.00 / 250.00 / 4 | 203.12 / 218.75 / 218.75 / 4 |
| Standing | Subsequent toggles | 92 | 79.78 / 90.75 / 98.20 / 0 | 34.29 / 91.52 / 96.85 / 0 | 78.12 / 93.75 / 93.75 / 0 | 31.25 / 78.12 / 93.75 / 0 |
| Standing | Intermediate | 384 | 14.99 / 24.84 / 30.30 / 0 | 15.13 / 45.82 / 48.02 / 0 | 15.62 / 15.62 / 31.25 / 0 | 15.62 / 46.88 / 46.88 / 0 |
| Standing | Settled | 96 | 18.89 / 28.26 / 31.83 / 0 | 19.43 / 24.81 / 34.26 / 0 | 15.62 / 15.62 / 31.25 / 0 | 15.62 / 15.62 / 31.25 / 0 |
| 0.31 m step | All toggles | 96 | 81.55 / 181.79 / 218.43 / 8 | 36.48 / 188.95 / 243.86 / 8 | 78.12 / 171.88 / 203.12 / 8 | 31.25 / 187.50 / 234.38 / 8 |
| 0.31 m step | First-ever toggle | 4 | 200.53 / 218.43 / 218.43 / 4 | 201.13 / 243.86 / 243.86 / 4 | 187.50 / 203.12 / 203.12 / 4 | 203.12 / 234.38 / 234.38 / 4 |
| 0.31 m step | Subsequent toggles | 92 | 81.10 / 94.89 / 181.79 / 4 | 36.30 / 98.52 / 191.37 / 4 | 78.12 / 93.75 / 171.88 / 4 | 31.25 / 93.75 / 187.50 / 4 |
| 0.31 m step | Intermediate | 384 | 15.28 / 25.16 / 30.87 / 0 | 15.46 / 45.58 / 49.68 / 0 | 15.62 / 15.62 / 31.25 / 0 | 15.62 / 46.88 / 46.88 / 0 |
| 0.31 m step | Settled | 96 | 17.25 / 29.35 / 32.53 / 0 | 17.16 / 26.88 / 32.05 / 0 | 15.62 / 15.62 / 31.25 / 0 | 15.62 / 31.25 / 31.25 / 0 |

The first-ever toggle exceeded 100 ms in every native process. The interaction coinciding with the step on toggle 11 was the only additional over-100 ms toggle in either arm. The fourth intermediate sample, scheduled 400 ms into each swing, had standing p95 of 28.66 ms on baseline and 47.91 ms on candidate. The stepped figures were 28.45 and 48.86 ms. The candidate moved this part of the animation beyond the 33.3 ms target.

On the stepped route, the synchronous distribution from the first moved frame through the final toggle (56 toggles per arm) was 79.43 / 177.81 / 181.79 / 4 on baseline and 37.23 / 186.28 / 191.37 / 4 on candidate. The individual four movement-plus-toggle samples and all component CPU/GL timings are available from the verifier.

| Camera / pair | Order | Baseline toggle | Candidate toggle | First-ever baseline / candidate (ms) |
|---|---|---|---|---|
| Standing 1 | A, B | 79.38 / 98.20 / 212.67 / 1 | 34.85 / 92.76 / 202.29 / 1 | 212.67 / 202.29 |
| Standing 2 | B, A | 79.78 / 97.95 / 218.57 / 1 | 34.53 / 95.37 / 218.45 / 1 | 218.57 / 218.45 |
| Standing 3 | A, B | 81.32 / 89.26 / 266.85 / 1 | 35.87 / 94.78 / 227.63 / 1 | 266.85 / 227.63 |
| Standing 4 | B, A | 82.50 / 90.91 / 255.37 / 1 | 33.90 / 96.85 / 212.29 / 1 | 255.37 / 212.29 |
| 0.31 m step 1 | B, A | 81.65 / 181.79 / 218.43 / 2 | 36.48 / 186.28 / 201.13 / 2 | 218.43 / 201.13 |
| 0.31 m step 2 | A, B | 80.92 / 177.81 / 186.55 / 2 | 36.93 / 186.11 / 243.86 / 2 | 186.55 / 243.86 |
| 0.31 m step 3 | B, A | 79.43 / 176.93 / 200.53 / 2 | 36.30 / 191.37 / 208.68 / 2 | 200.53 / 208.68 |
| 0.31 m step 4 | A, B | 81.55 / 181.57 / 215.81 / 2 | 35.78 / 183.00 / 188.95 / 2 | 215.81 / 183.00 |

A is the packaged baseline and B is the candidate. CPU and GL wall-time metrics are separate in the CSVs. Windows thread CPU samples can quantize to multiples of 15.625 ms. Synchronous high-resolution completion determines the player-interaction comparison.

## Doorway color, depth and state

A second fixture held all 17 native leaf positions for opening and closing. Baseline and candidate matched all 144 ARGB hashes, 144 raw-depth hashes, transferred bytes and uploaded batch counts at each camera, including warmup. The stepped fixture moved on its fifth toggle. The earlier mixed-route, shadow and per-receiver comparisons cover changes to circuits, fixtures, other portals, camera order, flashlight and UV, topology and map instances. The original 23 mm pair still exhibits its existing rounded diffuse-shadow alias in both arms. The literal exact-shadow experiment changed 56 of 58 reference colors. [Recorded doorway shadow and depth comparisons](README.md) · [Source-aware mixed-route evidence and memory stress](door-cpu.md)

Live hash comparisons align the same simulation tick, camera and logical portal state. Different wall-clock progress through the 450 ms motion can submit different leaf positions. An endpoint depth sample counts as comparable if both draws had already reached their common destination. Every settled frame and warmup qualifies. The following counts give observed complete ARGB / raw-depth differences per category. A/A and B/B compare each later trial to the first trial of the same executable. Exact deterministic lattice comparisons isolate scene correctness from live scheduling.

| Camera | Pairing | First-ever | Other toggles | Intermediate | Settled | Comparable raw-depth differences |
|---|---|---|---|---|---|---|
| Standing | A/B | 0 / 0 of 4 | 0 / 0 of 92 | 49 / 45 of 384 | 19 / 0 of 96 | 0 of 128 |
| Standing | A/A | 0 / 0 of 3 | 0 / 0 of 69 | 34 / 30 of 288 | 16 / 0 of 72 | 0 of 96 |
| Standing | B/B | 0 / 0 of 3 | 0 / 0 of 69 | 43 / 38 of 288 | 11 / 0 of 72 | 0 of 96 |
| 0.31 m step | A/B | 0 / 0 of 4 | 0 / 0 of 92 | 49 / 42 of 384 | 19 / 0 of 96 | 0 of 128 |
| 0.31 m step | A/A | 0 / 0 of 3 | 0 / 0 of 69 | 29 / 27 of 288 | 13 / 0 of 72 | 0 of 96 |
| 0.31 m step | B/B | 0 / 0 of 3 | 0 / 0 of 69 | 36 / 33 of 288 | 14 / 0 of 72 | 0 of 96 |

Both full-round receipts confirm an owner hunter, a remote hunter and a human ghost joined. Both arms exercised tools and shared evidence, identification, ritual, ABSOLVED settlement for both hunters and rejection of post-round commands.

## CPU reuse, transfer and memory

| Camera | Arm | Restored vertices in pairs 1–4 | Median uploaded bytes per toggle | Median uploaded batches | Total uploaded bytes across 96 toggles |
|---|---|---|---|---|---|
| Standing | baseline | 0, 0, 0, 0 | 426,528 | 19 | 42,978,816 |
| Standing | candidate | 2,790,081, 2,790,081, 2,790,081, 2,790,081 | 426,528 | 19 | 42,978,816 |
| 0.31 m step | baseline | 0, 0, 0, 0 | 463,776 | 20 | 45,907,584 |
| 0.31 m step | candidate | 2,249,037, 2,249,037, 2,249,037, 2,249,037 | 463,776 | 20 | 45,907,584 |

The counter measures resident world-object vertices whose certified color payload was restored. Other CPU and GL work still runs. The candidate caches alternate payloads and source-factor snapshots under a 16 MiB accounting limit and 8,192 entries per renderer. Independent pressure testing against 9,000 unique resources reached 16,777,072 charged bytes and exercised eviction and clearing. Physical trials used a 3 GiB maximum Java heap per process. These runs collected no process peak RSS, live heap or GPU VRAM telemetry. Deterministic equal transfer counts/bytes cover both cameras. [Memory stress and original probes](door-cpu.md)

The original 201,842-byte ZIP contains 71 files. Its SHA-256 is `dfd62935083b1a0f1c31c7ffd34d9c27e4663bf7d8672a0c35b3d93dac6cef0b`. The handoff recovery script verified its log chunks, original length, SHA-256 and ZIP CRCs.

Run `python3 analyze_physical_door_cpu.py --json metrics.json` alongside the original ZIP and member manifest to reproduce the per-pair and pooled metrics, fresh-first measurements, parity tables, counters and receipts. [Original physical frame CSVs, logs and receipts](physical-door-cpu.zip) · [SHA-256 manifest for the archive and every member](physical-door-cpu.sha256) · [Independent verifier](analyze_physical_door_cpu.py)
