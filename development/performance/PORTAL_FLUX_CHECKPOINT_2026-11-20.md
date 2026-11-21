# Portal-light transfer and GPU acceptance checkpoint — 20 November 2026

## Current source

Main at preparation: `18e6808d1271d59de4e5dcd8d8669a7f9fe4282a`. Keep the single `main` branch, fetch its live head
before new edits, use non-force fast-forward pushes with readback, and preserve the
existing authored level data, evidence visibility and host movement simulation.

## Work now in the actual engine

- `603816e7`: shared bidirectional doorway contract. The acoustic side uses the
  production `LanAcoustics` path; lighting and sound have different physical loss.
- `d21de843`, fixed at `601d1b89`: safe room-connectivity fast reject of
  impossible stationary source candidates, with a direct-ray JVM opt-out.
- `df46b0c1`: production `FixtureLighting` substitutes one-hop, real
  source-to-aperture illumination and a cached doorway-to-receiver visibility
  query for eligible adjacent indoor rooms. Prepared GPU probes and ordinary CPU
  probes use the same source factors. The fallback remains exact direct light.
- `ef24719e`, `43c65ebc`: door changes invalidate off-axis virtual-light
  receivers; conservative aperture reach limits which chunks need revisions.
- `03a792c2`: demands actual positive portal transfer from authored fixtures
  across House, Farmhouse, School. The **Windows stationary lighting-contract
  stage passed** for this source (run 44858573726); per-site numbers require
  archived job-log readback before a quantitative claim.
- `58f1c438`: the on-screen A/B camera now chooses a doorway that actually
  receives positive incident flux, instead of assuming one arbitrary House door.
- `18e6808d`: live HUD and Ctrl+Shift+F8 CSV separate direct shadow rays,
  portal-rejected candidates, portal-resolved samples, current transfer count
  and **cumulative** incident/receiver/reused rays. New counters are CPU
  instrumentation, not real GPU hardware timestamp queries.

## Evidence and limitations

- [GPU 44857305760](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44857305760)
  **SUCCESS** on `5a6bb5d6`: Windows build, renderer lighting/contracts,
  native software recovery and real Mesa OpenGL smoke. The earlier arbitrary
  House door comparison recorded closed RGB delta **0**, open RGB delta **0**.
  These zero deltas are *not* proof that the new portal transport improves real
  gameplay; that camera was not selected from actual transfer activity.
- The reachability differential printed **7,924 candidate exclusions** over its
  House/Farmhouse/School suite. That is a candidate count, **not** FPS or direct
  shadow ray savings.
- Real-source `03a792c2` Windows job 44858573726 **passed** its lighting
  contract and recorded per-site `PHYSICAL PORTAL COVERAGE`: House **31**
  authored emitters, **22** cached transfers, **22** positive receiver samples,
  **22** incident rays, **12** receiver rays; Farmhouse **31/26/26/27/14**;
  School **56/12/12/13/7** (same order). Across all three maps, this is
  **60 real cached transfers / 60 positive receiver samples / 62 incident
  rays / 33 receiver rays**. These are checked probe counts, not a frame
  benchmark and not an inference of saved GPU draw time.
- A more discriminating `58f1c438` active-door capture succeeded in run
  44858766959 (artifact 14689035883). It chose House door **12**, real source
  **19**, positive incident flux **1.1984638**. Closed-door RGB delta **0**;
  open-door direct vs portal delta **14,948** of 409,920 pixels. On raw-PNG
  inspection, **every changed pixel lies within image rows 0-29**, mostly
  the upper doorway/ceiling surface. This proves active transfer but does not
  yet prove credible illumination throughout the room. Source `8c521173`
  therefore adds a second A/B with receiving-room circuits OFF; inspect
  that evidence before approving visual quality.
- Reach-limited invalidation `43c65ebc` and telemetry `18e6808d` need
  exact-source final run readback; cumulative counters are not per-frame.
  Compare lighting/normal probes with portal opt-out and confirm both
  cold/open and rapid closing paths preserve other room cache revisions.
- No matched physical NVIDIA GTX 1660 SUPER solo/three-seat stop-and-go p50,
  p95/p99 or sixth-frame sanctuary result has yet been established **for these
  portal-flux revisions**. Do not extrapolate from Mesa or cold CPU images.
- Optical transport is currently **one hop** and conservative: Castle source
  regions, overlapping storeys, native sites, windows, stairs and multi-hop
  propagation still use the correct existing full-shadow path. Never add
  light through closed solid walls or automatically collect camera evidence.
- A separate House door stationary-shadow advisory assertion
  (`StationaryDoorShadowRetentionTest`, expected 0 vs 1 same-side ray)
  existed before this work; its cause is not proved to be portal flux. Do not
  change thresholds/geometry to silence an unrelated visual diagnostic.
- Some installer publishing workflows have been rejected with HTTP 403 at
  GitHub Releases despite a `contents: write` token. Separate permission
  failures from genuine install/play/launch/updater failures. Do not retag.

## Next exact checks

1. Inspect the GPU run logs of `03a792c2` and all successor revisions.
   Record each source's actual `PHYSICAL PORTAL COVERAGE` by map: emitters,
   source-door pairs, cached transfers, positive receiver samples and ray counts.
   If no live portal transfer exists for a map, diagnose source mounting/room
   ownership and aperture incident-ray blockage before enabling more aliases.
2. Review paired screenshot PNGs and delta from the dynamically selected
   active-light doorway. The open view needs meaningful optical evidence;
   verify closed-door zero leak. Keep both the original and cached screenshots.
3. Verify `FrameTimeCaptureTest` with the exact-source instrumentation and
   actual CSV headers, then use portal-resolution/cached-ray counts alongside
   p50/p95/p99 consumed GPU frames in a matched A/B. The cumulative counters
   require subtraction across consecutive samples to estimate interval work.
4. Use the **existing trusted private hardware worker** only through its
   authorized dispatch path for actual GTX 1660 SUPER solo/three-seat runs.
   No public AI-triggered runner execution or secret exposure. Preserve host
   50Hz input, continuous movement and authentic light/ghost/evidence.
5. Once the single-hop visual and physical gates are proved, consider bounded
   multi-portal transfer and Castle region ownership with the same
   `FixtureLighting` engine, not a second rendering system.

## Reproduction controls

Opt out independently with
`-Dbeckenburg.lighting.portalFlux.disabled=true` and
`-Dbeckenburg.lighting.portalGate.disabled=true`.
The first restores direct cross-room rays; the second disables only the
disconnected-room source rejection. In game `Ctrl+Shift+F7` shows the
performance HUD; `Ctrl+Shift+F8` starts/stops the bounded local CSV.
Both source revision and renderer state must be recorded with every comparison.

Release-blocking checks remain limited to required gameplay build/launch/run,
multiplayer and updater failures. Visual, shader, balance and benchmark
diagnostics remain real and actionable but advisory.
