# Prepared-light writes and unchanged batch retention — 2026-10-29

Final runtime source `82bd31accb17ea8fb154998de2afd7a22f019616`, [PR #40](https://github.com/mrcalzon02/The-Beckenburg-Project/pull/40), stacked on the accepted readback source in PR #39. No textures, source models, authored normals, visibility, host gameplay or source brightness rules change.

## Measured cause and implementation

A separate JFR run on the previous source found prepared-probe RGB records and immutable-list iterators among the largest warm main-thread allocation sources. Selected warm allocation samples estimated about 1.53 GB of RGBs and 1.16 GB of iterators; these are sampling weights, not exact allocation counts. Execution samples also reached Probe.sample, WorldVertexLight.apply and appendResidentWorldChunk. The recording remains under the ignored local build directory.

Prepared probes now use private arrays with shared empty arrays. Their direct writer preserves the sampled arithmetic order, thresholds, emission/normal handling, clamps and final tinted float bits. Only live receivers are retained, with explicit offsets into the material buffer. Relighting clones a published array lazily after its first actual color change; unchanged batches preserve resource IDs/revisions and GPU data. Door/geometry changes still rebuild visibility/probes through the existing occluder revision.

`beckenburg.lighting.directWrite.disabled=true` selects sampled RGB evaluation. `beckenburg.lighting.changedOnly.disabled=true` restores dense unconditional updates, including constant receivers. Neither fallback changes host authority.

## Verification

Java 17 GPU packaging passed. PreparedLightWriteTest verifies exact float bits and untouched neighboring position/UV slots across all tints/normals, emission, live candle/TV changes, blackout, doors and all map types. It also verifies lazy-copy identity and immutable prior arrays. PreparedLightBatchParityTest compares every complete submitted vertex buffer against dense updates across Castle, House, Farmhouse and School, flicker/blackout/doors and scene changes. This is recording-backend source evidence, not GPU FPS. Existing prepared/physical lighting, retention and all-ten-map residency tests passed; explicit sampled fallback passed.

Actual RTX 5070/NVIDIA 616.64 OpenGL controls passed Castle and all ten map types, torch, candle flicker, blackout, door, resize, return and live TV/map pixels. All 12 diagnostic captures match parent hashes; all 12 matched final-profile pairs also match exactly. The sampled Castle flicker frame uploads 7,552,128 bytes instead of 17,778,528, reusing 10,688,832 bytes, with zero new shadow rays. Raw `frames.csv` and `live-textures.csv` retain the full controls.

Six GPU lifecycle cycles returned to 641,442,080–644,138,272 post-GC Java heap bytes and exactly 1158 meshes/607 textures/14,486,784 resident geometry bytes. `lifecycle.csv` includes intermediate phases. This diagnostic uses explicit GC; it is not hardware VRAM or frame-rate evidence.

Final GPU JAR SHA-256: `89DB82E28976630B60EC2933321B0D5389B1B2F574C8FFDAF7F729E588EA7B1C`.

## Three-trial Castle tail comparison

The parent SceneRenderer and FixtureLighting sources from runtime `35054b76` were compiled into a class overlay against the same candidate package/backend/assets. The existing profile script records overlay/JAR hashes, source labels and isolated settings in `provenance.json`. Three trials per variant, four scripted views, 1280x800, 90 warmup + 240 measured frames per view, same cosmetic clock/camera sweep, Java 17.0.12 and RTX 5070/616.64. Parent trials ran first, then candidate trials; no other local rendering test ran concurrently. Longer samples expose recurring relighting frames; do not compare these percentiles directly with earlier 120-frame trials.

`before/`, `after/`, summaries and `comparison.md` contain raw frames/environment records and pooled percentiles with trial median ranges. Pooled p95 improves about 8–16% across the four views, with little change in medians. Castle exterior p95 moves 41.55→35.05 ms and p99 moves 47.74→41.63 ms. Its p95 uploaded geometry falls 26,803,680→9,360,288 bytes. Exterior stress p95 remains above 33.3 ms; cold preparation and remaining tail stalls are open. This scripted aerial stress view is not a collision-proven player route or installed-product acceptance.

Earlier candidates remain under `historical/56f64704/` (direct write only) and `historical/b63f7acb/` (lazy unchanged-color copies before filtering fixed receivers). Their measured exterior p95 remained39.65ms and36.92ms respectively under their own trials. They are historical comparisons, not substitutes for final-source validation; all failed-to-meet-target results remain recorded.

Final source CI: [GPU40880772640](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40880772640), [full gameplay40880777632](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40880777632) were running at this checkpoint. Earlier-source CI does not certify this source. Later changes are evidence/docs only. Main merge, release, actual installed interaction and full A–G completion remain open.
