# A 20 ms render deadline on the physical GTX 1660 SUPER

The four-lane House renderer met the 33.3 ms p95 target in all six pooled distributions. Deadline20 reached 32.40 ms while walking solo and 32.67 ms with three seats. It improved p95 in all 24 paired role/phase observations against the repeating 30 ms timer shipped in v0.1.338. Of the 24 individual trial phases, 22 met the target. One solo walk reached 34.050 ms p95 and one multiplayer walk reached 33.747 ms.

A single solo walking interval took 106.424 ms, on the first 0.31 m step inside the sanctuary in pair 3. The renderer consumed the same GL frame ID for the first draw at the new pose, then consumed a fresh ID on the next draw. Those draws spent 48.56 and 33.42 ms in post-geometry work with zero fresh shadow rays. The other seven walks recorded zero intervals over 100 ms. Walking entirely inside the dome remained demanding, with p95 of 53.740 and 55.881 ms solo and over TCP. Walking entirely outside reached 22.826 and 22.757 ms.

Each of four counterbalanced blocks ran idle, a 3-second walk from the van, and one step followed by a 5-second stop inside the van. All eight deadline20 trials traveled 8.37 m on the outward walk. All 32 stops held a single final pose. The Core30 i7-10700 ran the normal House viewport on the physical GTX at 960×540 internal resolution. One hunter rendered in the three-seat test with a second hunter and human-role ghost on two real loopback connections. The raw frames identify the OpenGL frame actually consumed by the panel. Each interval uses consecutive distinct, nonnegative frame IDs within the same trial and phase. Percentiles use nearest rank. [Private runner receipt](https://github.com/mrcalzon02/Dens-Mortis/actions/runs/44002135665)

The original control is the untouched v0.1.335 JAR. The other arms use the same v0.1.336 gameplay and four-lane shader. Repeating 30 ms reproduces the v0.1.338 production classes. Each deadline arm restarts the render timer after a draw, subtracting elapsed rendering time from the requested period and allowing at least 1 ms for other events. The original 110 ms gameplay/input timer remains independent. Deadline30 alone was slower on all six pooled p95s than the repeating timer.

| Role | Arm | Phase | Intervals | p50 ms | p95 ms | p99 ms | Over 50 ms | Over 100 ms | Pairs at target |
|---|---|---|---:|---:|---:|---:|---:|---:|---:|
| Solo | v0.1.335 | Idle | 2368 | 33.62 | 35.63 | 37.07 | 3 | 0 | 0/4 |
| Solo | v0.1.335 | Walking | 292 | 33.74 | 73.39 | 122.21 | 47 | 6 | 0/4 |
| Solo | v0.1.335 | Stopping inside | 292 | 67.63 | 70.04 | 71.89 | 292 | 0 | 0/4 |
| Solo | Four-lane, repeating 30 ms | Idle | 2599 | 30.66 | 33.71 | 35.35 | 1 | 0 | 0/4 |
| Solo | Four-lane, repeating 30 ms | Walking | 370 | 30.59 | 43.59 | 82.33 | 13 | 0 | 0/4 |
| Solo | Four-lane, repeating 30 ms | Stopping inside | 650 | 30.54 | 38.19 | 39.60 | 0 | 0 | 0/4 |
| Solo | Four-lane, 30 ms deadline | Idle | 2531 | 31.48 | 34.55 | 36.13 | 0 | 0 | 0/4 |
| Solo | Four-lane, 30 ms deadline | Walking | 367 | 31.30 | 44.63 | 79.89 | 13 | 0 | 0/4 |
| Solo | Four-lane, 30 ms deadline | Stopping inside | 630 | 31.46 | 39.33 | 40.99 | 0 | 0 | 0/4 |
| Solo | Four-lane, 20 ms deadline | Idle | 3678 | 21.54 | 23.81 | 26.80 | 0 | 0 | 4/4 |
| Solo | Four-lane, 20 ms deadline | Walking | 527 | 21.41 | 32.40 | 71.64 | 13 | 1 | 3/4 |
| Solo | Four-lane, 20 ms deadline | Stopping inside | 925 | 21.53 | 23.60 | 30.43 | 0 | 0 | 4/4 |
| Three-seat TCP | v0.1.335 | Idle | 2370 | 33.62 | 35.79 | 36.89 | 0 | 0 | 0/4 |
| Three-seat TCP | v0.1.335 | Walking | 255 | 33.86 | 88.15 | 371.04 | 47 | 10 | 0/4 |
| Three-seat TCP | v0.1.335 | Stopping inside | 292 | 67.68 | 70.18 | 72.39 | 292 | 0 | 0/4 |
| Three-seat TCP | Four-lane, repeating 30 ms | Idle | 2599 | 30.64 | 33.71 | 35.08 | 0 | 0 | 1/4 |
| Three-seat TCP | Four-lane, repeating 30 ms | Walking | 371 | 30.73 | 43.23 | 89.18 | 12 | 0 | 0/4 |
| Three-seat TCP | Four-lane, repeating 30 ms | Stopping inside | 648 | 30.60 | 38.12 | 39.96 | 0 | 0 | 0/4 |
| Three-seat TCP | Four-lane, 30 ms deadline | Idle | 2533 | 31.48 | 34.62 | 36.06 | 0 | 0 | 0/4 |
| Three-seat TCP | Four-lane, 30 ms deadline | Walking | 363 | 31.45 | 44.75 | 89.33 | 14 | 0 | 0/4 |
| Three-seat TCP | Four-lane, 30 ms deadline | Stopping inside | 630 | 31.47 | 38.34 | 40.46 | 0 | 0 | 0/4 |
| Three-seat TCP | Four-lane, 20 ms deadline | Idle | 3687 | 21.50 | 23.88 | 26.50 | 0 | 0 | 4/4 |
| Three-seat TCP | Four-lane, 20 ms deadline | Walking | 525 | 21.46 | 32.67 | 66.33 | 15 | 0 | 3/4 |
| Three-seat TCP | Four-lane, 20 ms deadline | Stopping inside | 929 | 21.34 | 25.86 | 30.69 | 0 | 0 | 4/4 |

## CPU and multiplayer

The shorter period draws more frames and uses more CPU per second. Each value is total benchmark-process CPU time divided by elapsed phase time across the four trials. It includes rendering, networking, JVM compilation and GC.

| Role | Timer | Idle cores | Walking cores | Stopping cores |
|---|---|---:|---:|---:|
| Solo | Repeating 30 ms | 0.648 | 1.052 | 0.639 |
| Solo | Deadline 20 ms | 0.802 | 1.197 | 0.801 |
| Three-seat | Repeating 30 ms | 0.681 | 1.119 | 0.837 |
| Three-seat | Deadline 20 ms | 0.867 | 1.229 | 1.391 |

One three-seat stop used 3.048 CPU cores on average. The three other stops used 0.797–0.872. All four held the requested final pose. Local TCP state receipt to Swing event dispatch reached p95 of 6.190 ms idle, 2.997 ms walking and 7.702 ms stopping. Each three-seat deadline20 walk recorded a visible hunter and 52 approved steps. Both intact-control and deadline20 rounds reached evidence sharing, identification, ritual and settlement. The local viewport hide, resume, close and input checks exercised the new scheduler. The physical traces measure frames consumed by the application. Monitor scanout and separate-PC transport are outside this measurement. [Timer lifecycle and local replay](timer-replay.md)

Production uses the normalized Java sources verified by the Windows worker. The deadline20 panel SHA-256 is `31a889e9f4923ef838200dc25e8f3964c6fa950118b9ba0d788cb1dd2934b653`. The four-lane shader SHA-256 is `de95f3d3f4b5b415787094c48c95dbc0b526c5b651ca430b3dd114478c3a4401`. The raw archive SHA-256 is `7608a57d8c8b21ec9d70ac4f18cf6c882456130c12e576a4722a98e60fdbcf37`. Both independent analyses reproduced all 96 trial-phase summaries. [Original Windows frames, CPU and network traces](windows-pacing.zip) · [Analysis scripts and per-trial outputs](windows-pacing-analysis.zip) · [SHA-256 checksums](windows-pacing-SHA256SUMS)
