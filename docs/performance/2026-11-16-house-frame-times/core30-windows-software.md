# Core30 software follow-up for v0.1.336

On Windows 10 Home 19045, Core i7-10700, Temurin 17.0.21, the first-peer walking hitch disappeared in all four cold three-seat comparisons. The software renderer ran at 640×360 with one rendered hunter, a second hunter and a human-role ghost on loopback TCP. The [completed worker run](https://github.com/mrcalzon02/Dens-Mortis/actions/runs/43985468339/job/133712363566) compared `7225b34abd7da5213623e6e7ed4f79ed1657a0f4` with released `13fcd0a4d3002e1084f0ab585865c6d8c1d18691` in fresh Java processes. Both full multiplayer rounds passed through evidence, ritual and settlement for both hunters. All timed trials recorded movement and each TCP trial reported a visible peer.

| Trial | Solo maximum post-geometry, ms | Solo longest completion gap, ms | TCP maximum post-geometry, ms | TCP longest completion gap, ms |
|---|---:|---:|---:|---:|
| 1 | 26.3 → 26.4 | 140.1 → 137.8 | 326.0 → 28.9 | 440.1 → 139.5 |
| 2 | 27.2 → 26.6 | 138.4 → 136.6 | 320.4 → 27.8 | 431.0 → 137.1 |
| 3 | 27.5 → 26.7 | 137.7 → 136.4 | 327.5 → 28.3 | 435.9 → 138.5 |
| 4 | 27.0 → 26.2 | 136.8 → 138.2 | 318.9 → 27.9 | 432.2 → 140.3 |

Post-geometry cost is the maximum `effects_ns` of any moving frame. The completion maximum uses differences of consecutive moving `time_ns` values within the same process. The baseline spike and longest gap coincided on the fourth or fifth moving frame in each TCP trial. The trace reports peer visibility per trial, so attribution to the exact first visible draw comes from the checkpoint’s earlier instrumented run.

## Pooled software render-completion intervals

| Role | Phase | Build | Intervals | p50 ms | p95 ms | p99 ms | >50 ms | >100 ms |
|---|---|---|---:|---:|---:|---:|---:|---:|
| Solo | idle | 7225b34 | 587 | 135.8 | 140.0 | 141.5 | 587 | 587 |
| Solo | idle | v0.1.336 | 588 | 135.7 | 139.8 | 141.5 | 588 | 588 |
| Solo | moving | 7225b34 | 106 | 101.5 | 136.8 | 138.4 | 106 | 86 |
| Solo | moving | v0.1.336 | 104 | 102.1 | 136.4 | 137.8 | 104 | 86 |
| Solo | cooldown | 7225b34 | 19 | 101.5 | 103.3 | 103.3 | 19 | 11 |
| Solo | cooldown | v0.1.336 | 19 | 100.4 | 106.1 | 106.1 | 19 | 11 |
| TCP | idle | 7225b34 | 588 | 135.7 | 139.5 | 141.7 | 588 | 588 |
| TCP | idle | v0.1.336 | 588 | 135.7 | 139.5 | 142.1 | 588 | 588 |
| TCP | moving | 7225b34 | 93 | 102.7 | 151.3 | 440.1 | 93 | 76 |
| TCP | moving | v0.1.336 | 105 | 102.4 | 136.2 | 140.0 | 105 | 84 |
| TCP | cooldown | 7225b34 | 16 | 101.5 | 107.8 | 107.8 | 16 | 14 |
| TCP | cooldown | v0.1.336 | 16 | 101.9 | 105.9 | 105.9 | 16 | 16 |

Percentiles use nearest rank after pooling within-phase intervals from four trials. Recomputed per-trial idle and moving results agree with all worker summary CSVs to the reported 0.001 ms. Cooldown follows the full walk out of the van and contains only 16–19 intervals pooled per role/build. The one-step stop inside the sanctuary requires a separate route. The physical GPU, desktop presentation and cross-machine network latency are outside the scope of this software result.

[Original checksummed worker archive](core30-followup.zip) · [Per-file SHA-256 for the archive](core30-archive-files.sha256) · [Download checksums](core30-SHA256SUMS)
