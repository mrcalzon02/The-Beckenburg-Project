# House van exit and one-step sanctuary stop on Windows

Run [43992154305](https://github.com/mrcalzon02/Dens-Mortis/actions/runs/43992154305) used the Core30 i7-10700 and physical GTX 1660 SUPER. Each of four blocks ran solo and a rendered hunter with a remote hunter and a human ghost over loopback TCP. The route idled at the van for 20 seconds, walked outside, returned inside, took one host-authoritative step and held. A fresh process and profile were used for each trial.

The baseline game was `dc7e497e74f3b0303ef72e1c7b17df764a3ebf7d`. The candidate checkout was `599f897138b7910d8ca38a92fd7638ebac719edc`. Controller `d877a875ee2801feec6e9a018bbfe8f103bd9925` ran the four matched sets on Windows 10 Home with Temurin 17.0.21.

The baseline is the .335 game. The six-frame prototype and staged cache are overlays on the candidate game, which includes .336 remote-hunter preloading. The comparison cannot isolate cache changes from that preloading in baseline TCP trials. Each overlay's exact normalized source hash and the common harness hash are in `compiled-overlays.json` inside the raw ZIP.

## Frame intervals

The intervals run between panel completions consuming distinct nonnegative GL frame IDs. Samples are partitioned by trial and phase before pooling and use nearest-rank percentiles. The 640×360 offscreen panel consumed 960×540 internal renderer frames throughout this test. These CPU timestamps exclude physical monitor presentation.

| Mode | Implementation | Phase | Intervals | p50 ms | p95 ms | p99 ms | >50 ms | >100 ms | Maximum ms |
|---|---|---|---:|---:|---:|---:|---:|---:|---:|
| Solo | v0.1.335 | idle | 2370 | 33.591 | 35.611 | 36.853 | 0 | 0 | 40.219 |
| Solo | v0.1.335 | walking | 289 | 33.747 | 72.148 | 118.855 | 51 | 7 | 123.094 |
| Solo | v0.1.335 | stopping | 292 | 67.66 | 70.494 | 72.505 | 292 | 0 | 73.343 |
| Solo | Six frames | idle | 2364 | 33.653 | 35.776 | 36.931 | 5 | 0 | 60.634 |
| Solo | Six frames | walking | 292 | 33.787 | 71.017 | 75.31 | 56 | 0 | 76.476 |
| Solo | Six frames | stopping | 291 | 67.676 | 70.546 | 74.57 | 291 | 0 | 96.066 |
| Solo | Staged rows | idle | 2366 | 33.619 | 35.967 | 37.117 | 3 | 0 | 61.202 |
| Solo | Staged rows | walking | 297 | 33.952 | 72.538 | 75.754 | 52 | 0 | 77.445 |
| Solo | Staged rows | stopping | 292 | 67.751 | 70.276 | 73.567 | 292 | 0 | 76.234 |
| Three-seat TCP | v0.1.335 | idle | 2371 | 33.605 | 35.708 | 36.982 | 0 | 0 | 38.909 |
| Three-seat TCP | v0.1.335 | walking | 258 | 33.811 | 84.856 | 372.43 | 45 | 8 | 380.745 |
| Three-seat TCP | v0.1.335 | stopping | 292 | 67.575 | 70.848 | 73.271 | 292 | 0 | 78.544 |
| Three-seat TCP | Six frames | idle | 2367 | 33.633 | 35.948 | 37.519 | 1 | 0 | 63.921 |
| Three-seat TCP | Six frames | walking | 295 | 33.742 | 72.222 | 76.439 | 53 | 0 | 77.419 |
| Three-seat TCP | Six frames | stopping | 292 | 67.606 | 70.786 | 73.321 | 292 | 0 | 75.27 |
| Three-seat TCP | Staged rows | idle | 2372 | 33.561 | 35.595 | 37.029 | 0 | 0 | 41.221 |
| Three-seat TCP | Staged rows | walking | 292 | 33.832 | 72.67 | 75.696 | 56 | 0 | 75.989 |
| Three-seat TCP | Staged rows | stopping | 292 | 67.425 | 70.375 | 74.346 | 292 | 0 | 75.296 |

## One step and stop inside

All 24 walks crossed the sanctuary boundary. The final step was 0.310 m and every recorded stopping frame was inside. The final unchanged camera was held 4.912–4.955 s. Its sixth render is the live pose that `SceneRenderer.renderSafeAreas` passes to `SafeAreaHaze.draw`. The GL image may have been submitted on an earlier frame. Post-geometry time includes the sanctuary and other scene effects.

| Mode | Implementation | Sixth effects range (ms) | Enclosing distinct GL gap range (ms) | Trials with >100 ms GL gap enclosing frame six |
|---|---|---:|---:|---:|
| Solo | v0.1.335 | 42.7–45.0 | 66.4–68.0 | 0/4 |
| Solo | Six frames | 55.6–56.7 | 64.0–65.9 | 0/4 |
| Solo | Staged rows | 58.3–59.2 | 67.3–68.5 | 0/4 |
| Three-seat TCP | v0.1.335 | 44.6–46.3 | 67.0–69.2 | 0/4 |
| Three-seat TCP | Six frames | 56.4–57.6 | 65.2–70.0 | 0/4 |
| Three-seat TCP | Staged rows | 56.9–61.1 | 66.3–71.6 | 0/4 |

Across the eight six-frame trials, median post-geometry time at rest on pose frames four through eight was 59.4, 59.2, 56.5, 45.1 and 44.9 ms. Their largest distinct-frame gap in that window was 70.803 ms. Each cache variant removed every observed walking gap over 100 ms in both modes. The sustained stop still ran at roughly 68 ms per frame, and none of the 18 pooled p95 measurements reached 33.3 ms.

The supplemental `pose-events.csv` records the first twelve held renders, plus all walking and stopping gaps above 100 ms. It includes phase-transition intervals explicitly labeled by their starting phase. `over-100.csv` counts only same-phase intervals. `route-audit.csv` verifies every walk crossed the dome and every TCP investigation received player states and approved steps. The baseline and staged arms separately completed the full three-seat round, including shared evidence, correct rite and both hunters’ settlement. The six-frame arm completed the instrumented walk and stop in both modes. [Raw Windows evidence](windows-stop.zip) · [Per-trial, per-pose and checksum data](windows-stop-analysis.zip) · [Download checksums](windows-stop-SHA256SUMS)
