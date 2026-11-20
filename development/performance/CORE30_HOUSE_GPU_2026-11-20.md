# Core30 physical House renderer benchmark — November 20, 2026

## Provenance and scope

- **Evidence:** private owner-controlled [physical Windows run 44826034694](https://github.com/mrcalzon02/Dens-Mortis/actions/runs/44826034694), completed **SUCCESS** after correcting a benchmark-only A/D screen-handedness assertion. The private runner retains the exact reviewed input driver pin and one-file request trust boundary.
- **Hardware:** physical NVIDIA GTX 1660 SUPER, Windows 10 Home (19045), Intel i7-10700, NVIDIA OpenGL 3.3 driver 610.88, Java 17.0.21.
- **Pinned game baseline:** `8598e7af05503d5223da8805bd6ce4624f97bcdb`.
- **Pinned game candidate:** `93578ee16a5d3b34e0a86fa012697d33a0e913ab`.
- **Trial design:** four alternated baseline/candidate pairs. Each arm ran solo and real three-seat local TCP (one rendered hunter, two additional connected peers). Each case included ~20 seconds of idle and ~3 seconds of walking, with actual OpenGL 3.3 renderer and collected composited GPU readback frame IDs. **16 runs total, 32 idle/moving phase summaries.**
- **Artifact limitation:** GitHub Actions artifact uploads were blocked by the account's storage quota, but the job emitted its 512,713-byte checksum-identified compressed evidence archive into its Actions log (`BECK_ARCHIVE`, SHA-256 `d32762fee4898f12fae0a86262970b018d98b07b1799618339a948c638f0a9af`). The values below were extracted from the actual terminal trial summaries, not assumed from the workflow's success conclusion.

## Measured frame intervals

These are **averages of each run's distinct-frame p50/p95/p99**, in milliseconds, *not percentiles pooled from all raw CSV rows*. Smaller is better. Frame counts and hitch counts add across four trials in each row.

| Phase / seats | Arm | Frames | Mean p50 | Mean p95 | Mean p99 | Intervals >33ms | >50ms | >100ms |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| Idle / solo | Baseline | 3,672 | 21.372 | 24.076 | 33.315 | 35 | 27 | 0 |
| Idle / solo | Candidate | 3,669 | 21.420 | 24.186 | 29.026 | 31 | 27 | 0 |
| Walking / solo | Baseline | 511 | 21.724 | 29.152 | 51.584 | 17 | 6 | 0 |
| Walking / solo | Candidate | 514 | 21.593 | 28.806 | 46.593 | 16 | 4 | 0 |
| Idle / 3-seat TCP | Baseline | 3,664 | 21.481 | 24.036 | 26.775 | 28 | 28 | 0 |
| Idle / 3-seat TCP | Candidate | 3,676 | 21.387 | 23.808 | 26.767 | 28 | 28 | 0 |
| Walking / 3-seat TCP | Baseline | 515 | 21.856 | 28.352 | 44.121 | 16 | 2 | 0 |
| Walking / 3-seat TCP | Candidate | 518 | 21.780 | 28.351 | 44.181 | 15 | 1 | 0 |

**Interpretation:** solo walking candidate p99 improved ~4.99ms (51.58 to 46.59ms) while TCP walking p95/p99 is effectively unchanged. The median of ~21–22ms across all rows suggests the old 20ms presentation timer is a plausible throughput limit, but these runs do **not** isolate that timer and should not be described as proof. No interval exceeded 100ms **in this route's measured phase samples**; cold-start stalls and other maps are different questions.

**Movement verification:** all eight TCP trials recorded 150 host-accepted movement steps and ~9.295m of host-confirmed travel. Solo runs observed ~9.29m max travel but the offscreen solo probe exported `accepted_steps=-1` / `confirmed_body_travel_metres=-1` (not instrumented). Do not rewrite that as solo authoritative travel confirmation. These were headless/offscreen panel trial routes and do not establish sustained end-user mouse smoothness.

## Follow-up 60Hz experiment

Source [`4c38ed58`](https://github.com/mrcalzon02/The-Beckenburg-Project/commit/4c38ed585b4c6abc9bce96c6a0e67a095362a0c2) changes only the default hunter/player-ghost render scheduling target from the former ~20ms/50fps to 60fps while retaining the independent 50Hz input sender and host simulation. It adds an in-game and Castle Graphics selector for client presentation targets 50/60/90/120fps and stamps `presentation_target_hz` in local frame CSV evidence. Swing rendering remains sequential without overlapping frames.

**Requested physical A/B:** private [run 44831206462](https://github.com/mrcalzon02/Dens-Mortis/actions/runs/44831206462), baseline `8450a4f7a9fdc783584a09088b77a423691689e4` (50fps default) versus candidate `4c38ed585b4c6abc9bce96c6a0e67a095362a0c2` (60fps default), matched hardware and route. **Pending:** evaluate terminal results and CSV, verify no regressions in p95/p99, mean cadence, collision or input acceptance; ensure the benchmark actually observes the live scheduling path rather than a separate fixed-rate offscreen clock. The output must not be advertised as a 60fps performance win before evidence confirms it.

**Remaining acceptance:** physical on-screen 60Hz walking and mouse turns, stop-and-go sanctuary, first view of another hunter, and actual three-seat gameplay on current source at target resolution, plus exact-source release-critical build/network checks and readability checks for the settings tabs.
