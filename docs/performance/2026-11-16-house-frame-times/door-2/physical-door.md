# House door 2 on the physical GTX 1660 SUPER

The RGB-only upload saved 251,160 bytes on every door toggle. Three of four paired trials had a higher 95th-percentile frame time, and the pooled p99 rose from 210.70 to 292.89 ms. The upload patch remains experimental.

Both arms used the same packaged 79a7f967b0b75513d92a56de67f0a6c2d4c7e766 source on Core30. The private worker applied only the recorded RGB buffer patch to the second arm. Java 17.0.21 and Windows OpenGL rendered the fixed-camera 320 × 200 scene. Each of four counterbalanced pairs launched a fresh process for each arm and measured 24 settled toggles. These synchronous frame measurements span scene construction, OpenGL work and completed readback. [Physical run 43992902756](https://github.com/mrcalzon02/Dens-Mortis/actions/runs/43992902756) · [Worker at its run revision](https://github.com/mrcalzon02/Dens-Mortis/blob/8688d9e3659b190b9fb6b92d8a319e1099225cac/tools/local-worker/beckenburg_performance.py)

| Toggle measurement | Baseline p50 / p95 / p99 (ms) | RGB-only p50 / p95 / p99 (ms) | Over 100 ms, baseline / RGB |
|---|---:|---:|---:|
| Synchronous frame | 86.42 / 114.59 / 210.70 | 85.95 / 110.99 / 292.89 | 12 / 11 |
| Render-thread CPU | 78.12 / 93.75 / 203.12 | 78.12 / 109.38 / 265.62 | 4 / 6 |
| GL draw issue | 0.53 / 1.29 / 1.79 | 0.66 / 5.12 / 6.49 | 0 / 0 |
| GL worker | 3.73 / 4.54 / 5.47 | 3.78 / 8.32 / 10.65 | 0 / 0 |

The pooled rows contain 96 toggles per arm. Percentiles use nearest rank. Each trial below reports synchronous p50, p95, p99 in milliseconds and count over 100 ms.

| Pair | Trial order | Baseline p50 / p95 / p99 / count | RGB-only p50 / p95 / p99 / count |
|---:|---|---:|---:|
| 1 | Baseline, RGB | 88.03 / 107.82 / 196.66 / 4 | 85.24 / 110.99 / 292.89 / 3 |
| 2 | RGB, baseline | 84.49 / 100.61 / 191.24 / 2 | 84.81 / 107.76 / 251.61 / 3 |
| 3 | Baseline, RGB | 82.85 / 102.92 / 209.80 / 2 | 86.51 / 108.46 / 180.88 / 2 |
| 4 | RGB, baseline | 87.46 / 114.59 / 210.70 / 4 | 88.23 / 102.51 / 209.91 / 3 |

The 18 revised buffers used 401,856 bytes in the baseline and 150,696 bytes in the candidate. The first toggle in each process also uploaded 71,616 bytes for two other batches on both arms. All 58 complete ARGB and raw-depth frame hashes matched across all eight processes. The route covers door 3, window 19, lamp, UV, near-plane geometry and restoration. Both complete TCP rounds passed evidence sharing, ghost identification, ritual and settlement for both hunters. The CPU shadow computation is identical in both arms. The separately tested exact-key shadow change affected 56 of 58 original full-color frames. [Doorway shadow witness and pixel comparisons](README.md)

The worker emitted a 37,176-byte ZIP with SHA-256 ef92f74a798eb6b043cb3bcd0742665179d74588c8f786a56ca7ea894e194283. Run `python3 verify_physical_door.py` alongside the archive and manifest to verify the original bytes, member hashes, GPU identity, paired frames, uploads, percentiles and settlements. [Raw frame CSVs and run receipts](physical-door.zip) · [Member SHA-256 manifest](physical-door-members.sha256) · [Verifier](verify_physical_door.py)
