# House sanctuary shading on the GTX 1660 SUPER

The four-lane sanctuary shader and 30 ms visual timer cut solo walking p95 from 72.57 ms to 44.17 ms, and stopping p95 from 69.91 ms to 38.06 ms. The corresponding three-seat figures were 76.61 to 43.16 ms and 70.64 to 38.31 ms. All six role/phase distributions improved in every paired trial. The renderer still misses the steady 33.3 ms p95 target.

The Core30 i7-10700 on Windows 10 ran a fresh process for each of 32 investigations. Four counterbalanced sets exercised both solo and one rendered hunter with two loopback TCP peers. Each trial held idle for 20 seconds, walked out of the van, returned, moved 0.31 m once inside the sanctuary and stopped for five seconds. The 640×360 offscreen panel used 960×540 internal frames on the physical GTX 1660 SUPER. Each interval spans two distinct nonnegative OpenGL frame IDs consumed by the panel within the same phase. Percentiles use nearest rank on pooled intervals. The last column counts individual trials with p95 at or below 33.3 ms. [Run 43993545950](https://github.com/mrcalzon02/Dens-Mortis/actions/runs/43993545950)

The control is the unmodified v0.1.335 JAR at `dc7e497e74f3b0303ef72e1c7b17df764a3ebf7d`. The other arms share the game at `1b0d74f66e8a183368ad75f4d3972b08827424b1`, which includes v0.1.336 remote hunter preloading. The three experimental arms isolate serial opacity reuse, parallel scanlines and the 30 ms visual timer in succession. Normal gameplay still samples movement on the 110 ms timer. The archive records the compiled source checksums and both package checksums.

| Role | Arm | Phase | Intervals | p50 ms | p95 ms | p99 ms | Over 50 ms | Over 100 ms | Pairs at target |
|---|---|---|---:|---:|---:|---:|---:|---:|---:|
| Solo | v0.1.335 | Idle | 2366 | 33.62 | 35.52 | 37.12 | 5 | 0 | 0/4 |
| Solo | v0.1.335 | Walking | 290 | 33.92 | 72.57 | 119.00 | 50 | 6 | 0/4 |
| Solo | v0.1.335 | Stopping inside | 292 | 67.75 | 69.91 | 71.40 | 292 | 0 | 0/4 |
| Solo | Serial opacity cache | Idle | 2365 | 33.62 | 52.83 | 54.76 | 160 | 0 | 0/4 |
| Solo | Serial opacity cache | Walking | 307 | 33.70 | 77.91 | 86.74 | 43 | 0 | 0/4 |
| Solo | Serial opacity cache | Stopping inside | 546 | 33.54 | 74.34 | 76.42 | 40 | 0 | 0/4 |
| Solo | Four-lane shading, 33 ms | Idle | 2367 | 33.64 | 37.07 | 38.35 | 0 | 0 | 0/4 |
| Solo | Four-lane shading, 33 ms | Walking | 341 | 33.59 | 45.56 | 81.57 | 11 | 0 | 0/4 |
| Solo | Four-lane shading, 33 ms | Stopping inside | 589 | 33.56 | 41.52 | 42.71 | 0 | 0 | 0/4 |
| Solo | Four-lane shading, 30 ms | Idle | 2600 | 30.62 | 33.64 | 35.37 | 0 | 0 | 0/4 |
| Solo | Four-lane shading, 30 ms | Walking | 368 | 30.71 | 44.17 | 81.86 | 14 | 0 | 0/4 |
| Solo | Four-lane shading, 30 ms | Stopping inside | 648 | 30.57 | 38.06 | 39.98 | 0 | 0 | 0/4 |
| Three-seat TCP | v0.1.335 | Idle | 2372 | 33.54 | 35.70 | 36.85 | 0 | 0 | 0/4 |
| Three-seat TCP | v0.1.335 | Walking | 262 | 33.83 | 76.61 | 369.22 | 42 | 10 | 0/4 |
| Three-seat TCP | v0.1.335 | Stopping inside | 292 | 67.54 | 70.64 | 71.61 | 292 | 0 | 0/4 |
| Three-seat TCP | Serial opacity cache | Idle | 2370 | 33.61 | 52.71 | 54.72 | 160 | 0 | 0/4 |
| Three-seat TCP | Serial opacity cache | Walking | 300 | 33.80 | 75.96 | 90.04 | 46 | 1 | 0/4 |
| Three-seat TCP | Serial opacity cache | Stopping inside | 544 | 33.60 | 74.46 | 76.23 | 40 | 0 | 0/4 |
| Three-seat TCP | Four-lane shading, 33 ms | Idle | 2368 | 33.63 | 36.90 | 38.04 | 0 | 0 | 0/4 |
| Three-seat TCP | Four-lane shading, 33 ms | Walking | 339 | 33.58 | 45.92 | 82.62 | 9 | 0 | 0/4 |
| Three-seat TCP | Four-lane shading, 33 ms | Stopping inside | 589 | 33.61 | 41.10 | 42.81 | 0 | 0 | 0/4 |
| Three-seat TCP | Four-lane shading, 30 ms | Idle | 2600 | 30.64 | 33.83 | 35.30 | 0 | 0 | 0/4 |
| Three-seat TCP | Four-lane shading, 30 ms | Walking | 371 | 30.70 | 43.16 | 82.36 | 13 | 0 | 0/4 |
| Three-seat TCP | Four-lane shading, 30 ms | Stopping inside | 649 | 30.62 | 38.31 | 40.68 | 0 | 0 | 0/4 |

## What the comparisons establish

Serial opacity reuse on its own regressed idle p95. Splitting fresh large-dome shading over up to four scanline workers improved p95 in all 24 matched role/phase observations relative to serial. Changing the visual timer from 33 ms to 30 ms improved 22 of 24 paired p95s. On the final arm the worst observed crossing was 92.96 ms solo and 96.29 ms over TCP. Walking entirely outside the dome reached p95 of 32.46 and 32.59 ms. Walking entirely inside took 72.87 and 60.10 ms. These distributions require both ends of an interval on the indicated side of the sanctuary boundary.

All 32 final stops held the same camera position. The four-lane renderer caches ray intersections for an exact projection, pose and sanctuary. Cached opacity also requires the identical animation clock. Every frame reads the current color and scene depth, including characters. A large freshly shaded region uses disjoint scanlines and finishes before the next translucent shell is blended. The second double array costs up to 3.96 MiB per populated full-frame cache at 960×540. The host, input rate, lighting equations, texture and UV sampling, depth writes and game rules retain their released behavior.

The TCP state receive-to-Swing-dispatch p95 improved from 23.91 to 5.27 ms idle, 32.58 to 4.26 ms walking and 42.34 to 6.72 ms stopping. This tests render-thread interference on the same machine. The baseline and combined candidate both completed the authoritative three-seat evidence, identification, rite and settlement path. Each intermediate shader ran the live walk and stop in both modes. Independent local checks exercised moving/crouching hunters, depth and color, optical evidence, UV and firelight, and full-round gameplay. [Local gameplay and optical checks](sanctuary-stop/local-gameplay-optics.zip)

[Original Windows frames, CPU stages, TCP state receipts, full-round logs and provenance](windows-shading.zip) · [Recomputed per-trial metrics, moving domains, stops, state timing and analysis scripts](windows-shading-analysis.zip) · [SHA-256](windows-shading-SHA256SUMS)
