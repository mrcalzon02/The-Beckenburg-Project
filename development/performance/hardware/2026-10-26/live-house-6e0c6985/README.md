# Actual Windows live House comparison

Source branch `performance/live-house-residency`, exact source `6e0c698599e66553adf54f2de4d26750e6c81d9d`. Same JAR, feature switch off/on, isolated fresh Java 17 JVM for each run. Windows 11 / i7-14700K / RTX 5070 / NVIDIA 616.64 / 712x200 / FOV 93 / current Retro 200p preferences. JAR SHA256 in measurements.json. No concurrent local tests during timed runs.

12 van warmup frames, then 40 yard, 40 entrance, 40 upstairs camera turns. Camera positions jump between authored spawns; this is a renderer diagnostic, **not physical traversal, installed case or 30 FPS acceptance**. Captures were inspected; the yard/entrance actually expose House geometry. The earlier cargo-spawn route was obscured by a monitor wall; its raw logs are separately labeled and excluded from the conclusions below.

| Run | p50 frame ms | p95 frame ms | p99 frame ms | first yard frame ms | static uploaded bytes during turns |
|---|---:|---:|---:|---:|---:|
| Legacy fixed 1 | 73.054 | 109.803 | 886.152 | 3880.005 | legacy batches |
| Legacy fixed 2 | 72.408 | 108.523 | 871.887 | 3970.426 | legacy batches |
| Resident fixed 1 | 19.930 | 35.858 | 202.138 | 624.257 | 0 |
| Resident fixed 2 | 20.413 | 36.077 | 206.475 | 600.264 | 0 |
| Resident live clock 1 | 20.122 | 36.075 | 204.824 | 601.436 | 4593216 |
| Resident live clock 2 | 20.621 | 36.729 | 202.985 | 621.343 | 4782336 |

Feature-on cold prewarm 4988-5165 ms versus legacy 1159-1162 ms; preparation moves work before the first exposed House turn. Actual retained GL vertex capacity 32,044,800 bytes (allocator capacity; payload 21,297,024 bytes). Fixed-clock camera turns: zero static rebuilds/repacking/uploads. Live-clock source changes legitimately update RGB buffers. GPU depth parity: van/interior/upstairs each 64,000 reference pixels; zero original pixels missing/farther beyond 5 cm, snap disabled for geometric diagnosis.

**Remaining defect:** p95 exceeds 33.3 ms and first exposed yard frame is still 600-624 ms. Native user case previously recorded 2357 ms at a House turn; this controlled route reproduced 3880-3970 ms on the legacy path. Do not equate different poses or controlled timing with that native observation. Door occluder invalidation is still global, UV/DOTS/portable lights preserve full legacy fallback, immediate exit/context lifetime and installed full investigation-to-Castle acceptance remain pending. No release published for this candidate.

Exact-head CI at this checkpoint: GPU 40442270881 and full acceptance 40442306416 / 40442307089 pending. PR #33 is a draft, default-off candidate.
