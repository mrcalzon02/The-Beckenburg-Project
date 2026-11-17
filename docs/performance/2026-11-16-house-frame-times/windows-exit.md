# Core30 GTX 1660 SUPER sanctuary exit

Run [44007798428](https://github.com/mrcalzon02/Dens-Mortis/actions/runs/44007798428) compared the same four-lane sanctuary shading and 20 ms deadline scheduler with and without the outward dome cull. The retail-target viewport was 960×540. Core30 ran four alternating pairs per mode on its i7-10700 and GTX 1660 SUPER. Each three-seat trial used one rendered hunter and two real loopback peers. The driver recorded distinct consumed GL readbacks in an offscreen viewport.

The guarded shader reduced the frame gap at the first outward crossing in all eight trials. The post-geometry stage fell to 1.37–1.45 ms at the crossing. Pooled walking p95 reached 31.592 ms solo and 31.002 ms over TCP. Two individual walking trials still missed the 33.3 ms target. A three-seat test recorded a 127.349 ms gap at the first step inside the van. Across the two renderer callbacks in that interval, post-geometry work took 105.239 ms.

The source change skips the near-plane raster only when every screen ray points away from the sanctuary bounding box. It retains the original analytic intersection and color/depth treatment for all rays that can hit. A focused independent analytic check passed 2,114,840 exact RGB/depth samples including camera positions grazing all four faces. Local coverage also includes 2,400 randomized views and 300 repeated exits and re-entries per JVM. The merged source retains the campaign and evidence changes introduced since the physical benchmark pin. [Van-exit geometry proof, raw per-pixel checks and local stage timing](sanctuary-stop/van-exit-profile.md) · [Original Windows frames and full-round receipts](windows-exit.zip) · [Analysis, scripts and per-pair CSVs](windows-exit-analysis.zip) · [SHA-256](windows-exit-SHA256SUMS)

## Frame pacing

| Mode | Phase | Arm | Intervals | p50 ms | p95 ms | p99 ms | Max ms | >100 ms | Trials p95 ≤33.3 ms |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Solo | Idle | Deadline20 | 3675 | 21.58 | 23.90 | 26.52 | 44.22 | 0 | 4/4 |
| Solo | Idle | Deadline20 + cull | 3674 | 21.59 | 23.96 | 26.56 | 45.17 | 0 | 4/4 |
| Solo | Walking | Deadline20 | 528 | 21.50 | 31.88 | 67.23 | 86.84 | 0 | 3/4 |
| Solo | Walking | Deadline20 + cull | 540 | 21.37 | 31.59 | 55.55 | 75.46 | 0 | 3/4 |
| Solo | Stopping | Deadline20 | 923 | 21.53 | 23.75 | 30.30 | 32.16 | 0 | 4/4 |
| Solo | Stopping | Deadline20 + cull | 933 | 21.28 | 23.26 | 29.61 | 32.71 | 0 | 4/4 |
| Three-seat | Idle | Deadline20 | 3687 | 21.48 | 23.78 | 26.25 | 44.58 | 0 | 4/4 |
| Three-seat | Idle | Deadline20 + cull | 3685 | 21.49 | 23.82 | 26.38 | 44.65 | 0 | 4/4 |
| Three-seat | Walking | Deadline20 | 529 | 21.44 | 32.54 | 64.95 | 84.48 | 0 | 3/4 |
| Three-seat | Walking | Deadline20 + cull | 537 | 21.47 | 31.00 | 53.40 | 127.35 | 1 | 3/4 |
| Three-seat | Stopping | Deadline20 | 929 | 21.34 | 23.64 | 30.28 | 31.48 | 0 | 4/4 |
| Three-seat | Stopping | Deadline20 + cull | 929 | 21.40 | 23.33 | 29.88 | 31.68 | 0 | 4/4 |

Each percentile pools distinct-GL intervals within the stated phase over the four trials. frame-paired-trials.csv compares every matched pair and phase. The table below shows each matched walk.

| Mode | Pair | Deadline20 p95 ms | With cull p95 ms | Deadline20 >100 ms | With cull >100 ms |
| --- | ---: | ---: | ---: | ---: | ---: |
| Solo | 1 | 32.252 | 31.377 | 0 | 0 |
| Solo | 2 | 31.768 | 31.219 | 0 | 0 |
| Solo | 3 | 33.398 | 31.771 | 0 | 0 |
| Solo | 4 | 31.482 | 33.457 | 0 | 0 |
| Three-seat | 1 | 33.329 | 30.41 | 0 | 1 |
| Three-seat | 2 | 32.752 | 29.548 | 0 | 0 |
| Three-seat | 3 | 31.456 | 31.411 | 0 | 0 |
| Three-seat | 4 | 31.587 | 33.707 | 0 | 0 |

| Mode | Arm | Interior intervals | Interior p95 ms | Exterior intervals | Exterior p95 ms | Crossings | Worst crossing ms |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Solo | Deadline20 | 153 | 51.27 | 371 | 22.84 | 4 | 86.84 |
| Solo | Deadline20 + cull | 156 | 49.60 | 380 | 22.67 | 4 | 17.57 |
| Three-seat | Deadline20 | 151 | 50.28 | 374 | 22.85 | 4 | 84.48 |
| Three-seat | Deadline20 + cull | 151 | 43.79 | 382 | 22.94 | 4 | 18.00 |

The interior and exterior distributions include moving intervals whose first and last observed CPU poses lie on the same side of the dome. Boundary intervals are kept separately. moving-domain-trials.csv preserves the three distributions per individual trial.

## Camera-matched crossing and first two inside steps

The five camera values (x, eye height, z, yaw and pitch) matched at every distinct pose through the boundary in 8/8 pairs. They matched along the common part of the entire three-second walk in 8/8 pairs. Maximum travel ranged from 8.06–8.68 m. The paths and final positions for every pair are in trajectory-pairs.csv.

The ten measured x coordinates through the first exterior view were 20.9, 20.59, 20.28, 19.97, 19.66, 19.35, 19.04, 18.73, 18.42, 18.11 m.

| Mode | Pose | Arm | Renderer ms | Build ms | Geometry ms | Post-geometry ms | GL gap ms | GL gap range ms |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Solo | First inside step | Deadline20 | 57.38 | 0.13 | 1.91 | 54.28 | 70.00 | 67.23–74.08 |
| Solo | First inside step | Deadline20 + cull | 52.48 | 0.13 | 2.16 | 49.02 | 67.29 | 65.84–75.46 |
| Solo | Second inside step | Deadline20 | 49.94 | 0.13 | 1.95 | 46.82 | 51.32 | 17.28–57.05 |
| Solo | Second inside step | Deadline20 + cull | 39.72 | 0.13 | 1.97 | 36.57 | 48.38 | 24.68–58.74 |
| Solo | First outside step | Deadline20 | 66.87 | 0.14 | 1.90 | 63.79 | 80.00 | 49.67–86.84 |
| Solo | First outside step | Deadline20 + cull | 4.41 | 0.14 | 1.87 | 1.38 | 16.87 | 16.26–17.57 |
| Three-seat | First inside step | Deadline20 | 50.06 | 0.13 | 2.01 | 46.88 | 65.04 | 62.17–73.50 |
| Three-seat | First inside step | Deadline20 + cull | 51.52 | 0.13 | 1.64 | 48.59 | 67.70 | 64.58–127.35 |
| Three-seat | Second inside step | Deadline20 | 16.02 | 0.14 | 2.12 | 12.84 | 24.84 | 19.38–56.02 |
| Three-seat | Second inside step | Deadline20 + cull | 14.82 | 0.13 | 1.82 | 11.72 | 22.93 | 17.15–53.40 |
| Three-seat | First outside step | Deadline20 | 70.12 | 0.14 | 2.17 | 66.56 | 82.94 | 64.95–84.48 |
| Three-seat | First outside step | Deadline20 + cull | 4.65 | 0.14 | 2.00 | 1.43 | 17.02 | 16.84–18.00 |

Stage values are median elapsed CPU-side renderer timings across four independent first arrivals at that exact pose. Post-geometry includes sanctuary shading, character sprites and other effects. The physical driver did not time the dome separately. The camera passed to sanctuary shading is the camera recorded in frames.csv. Consumed OpenGL RGB/depth can come from an earlier submission. Each GL gap spans the two consecutive distinct readback timestamps bracketing the CPU event, with the ending timestamp included. gl-neighborhoods.csv retains the previous and following two intervals where available. GPU execution and input-to-display latency require different timestamps.

4 of the 48 camera-event brackets span the transition from idle to moving. Their phase endpoints and frame IDs are preserved in camera-events.csv. These transition intervals are excluded from every per-phase percentile and over-100 count.

| Mode | Pair | Post-geometry before ms | Post-geometry after ms | GL gap before ms | GL gap after ms |
| --- | ---: | ---: | ---: | ---: | ---: |
| Solo | 1 | 70.77 | 1.41 | 86.84 | 16.72 |
| Three-seat | 1 | 48.16 | 1.43 | 64.95 | 18.00 |
| Solo | 2 | 63.44 | 1.39 | 79.39 | 17.02 |
| Three-seat | 2 | 66.21 | 1.45 | 82.59 | 16.86 |
| Solo | 3 | 64.15 | 1.37 | 80.60 | 16.26 |
| Three-seat | 3 | 69.33 | 1.43 | 84.48 | 17.18 |
| Solo | 4 | 32.65 | 1.37 | 49.67 | 17.57 |
| Three-seat | 4 | 66.91 | 1.43 | 83.28 | 16.84 |

The guarded build reduced first-exterior post-geometry time in 8/8 matched trials and reduced the enclosing frame interval in 8/8. Each row in camera-events-paired.csv keeps the matched build, geometry, post-geometry, total and GL-interval differences for both earlier inside steps and the first exterior view.

## Intervals above 100 ms and the inside stop

| Mode | Pair | Arm | Phase | x start → end m | Gap ms | Rendered panel completions | Total renderer ms | Total post-geometry ms | Readback ID sequence |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Three-seat | 1 | Deadline20 + cull | Walking | 20.9 → 20.59 | 127.35 | 2 | 111.25 | 105.24 | 1420 1420 1422 |

Renderer and post-geometry totals cover every completed panel draw after the starting readback through the ending readback. Repeating a readback ID keeps the preceding interval open. The first inside camera pose can therefore span two expensive draws before the next distinct GL frame appears.

All 16 stopping traces recorded a 0.31 m step followed by a stable five-component camera pose, held for 4.96–4.99 s. The sixth identical panel completion is bracketed by distinct GL timestamps.

| Arm | Holds | Sixth-frame effects range ms | Sixth GL gap range ms | Worst GL gap within ±2 intervals ms |
| --- | ---: | ---: | ---: | ---: |
| Deadline20 | 8 | 4.76–13.49 | 12.88–29.19 | 29.19 |
| Deadline20 + cull | 8 | 5.02–5.81 | 20.30–22.63 | 29.28 |

## Process CPU, peer state and round receipts

| Mode | Arm | Idle cores | Walking cores | Stopping cores | Idle CPU ms/frame | Walking CPU ms/frame | Stopping CPU ms/frame |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Solo | Deadline20 | 0.8095 | 1.2299 | 0.7977 | 17.604 | 27.755 | 17.226 |
| Solo | Deadline20 + cull | 0.7982 | 1.0621 | 0.7974 | 17.363 | 23.438 | 17.026 |
| Three-seat | Deadline20 | 0.8544 | 1.3286 | 0.8334 | 18.521 | 29.931 | 17.869 |
| Three-seat | Deadline20 + cull | 0.8778 | 1.147 | 0.8804 | 19.039 | 25.474 | 18.891 |

Process CPU covers the entire Java process, including the local host, clients, renderer and other threads. Core-equivalents divide summed CPU time by summed elapsed phase time. Counter reads occur at phase boundaries.

| Arm | Phase | STATE arrivals | EDT queue p50 ms | p95 ms | p99 ms | Max ms | >100 ms |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Deadline20 | Idle | 160 | 0.10 | 5.70 | 7.00 | 9.01 | 0 |
| Deadline20 | Walking | 132 | 0.07 | 7.90 | 25.66 | 57.86 | 0 |
| Deadline20 | Stopping | 44 | 0.12 | 6.83 | 7.11 | 7.11 | 0 |
| Deadline20 + cull | Idle | 160 | 0.11 | 5.62 | 6.87 | 7.44 | 0 |
| Deadline20 + cull | Walking | 133 | 0.07 | 4.96 | 16.65 | 51.06 | 0 |
| Deadline20 + cull | Stopping | 44 | 0.14 | 7.51 | 8.74 | 8.74 | 0 |

Queue latency runs from socket receipt of a decoded STATE entry to its dispatch on the Swing event thread. state-pooled.csv also reports the stricter subset whose arrival and dispatch both occurred between the first and last rendered phase samples.

All 8 TCP trials maintained both peers, recorded at least one visible peer hunter and confirmed 50–54 authoritative accepted movement records. Both arms passed the full separate network-round tests. Their receipts cover spectral privacy, evidence tools and sharing, hunter settlement and rejected post-round input.

## Reproduction

Raw archive SHA-256 is `fedd354defd0c470cf84adf6ffe717bd74769ae8491a231707d2d6f5cf11ecab`. Baseline source is `b4c36c0b34e79187a37c52f12f77c4d39e472fbc` and candidate package source is `49288cb383955307d06647fa8267ccda318709c0`. The worker compared both production trees and compiled only the guarded sanctuary class for the candidate. Deadline20 panel SHA-256 is `31a889e9f4923ef838200dc25e8f3964c6fa950118b9ba0d788cb1dd2934b653` and the guarded shader SHA-256 is `f582008ec7929243ab05fd2cd7ef7ede4cf3836bf56d79cb6fa83756ccb59cf3`.

Extract windows-exit-analysis.zip alongside windows-exit.zip and run the following commands from that directory.

```sh
python3 audit.py windows-exit.zip results --baseline-sha b4c36c0b34e79187a37c52f12f77c4d39e472fbc --candidate-sha 49288cb383955307d06647fa8267ccda318709c0 --controller-sha 8e9c1ebe51e9c92589a13bff530c13ab171a0d11
python3 events.py windows-exit.zip results
python3 report.py windows-exit.zip results --run 44007798428
```
