# Packed readback conversion — 2026-10-29

Runtime source `35054b762c2663d4d7a618ba42c342f000099d29`, [PR #39](https://github.com/mrcalzon02/The-Beckenburg-Project/pull/39), stacked on the accepted pose-cache source in PR #38. Existing E0 `ReadbackDetail` phase instrumentation from PR #27 was reconciled into the current GPU backend rather than merging its historical renderer tree.

The RGBA transfer buffer has explicit big-endian byte order. One packed 32-bit load plus rotate produces ARGB instead of four byte loads and shifts. CPU output arrays remain newly owned by each completed Result; row inversion, alpha and the original float linear-depth formula are preserved. No transfer is removed, delayed or reused. `-Dbeckenburg.gpu.packedReadback.disabled=true` selects byte-wise conversion.

## Functional evidence

Offline Java 17 GPU Maven packaging passed. `GpuReadbackTest` passed heap/direct buffers, every alpha value with randomized channels, odd row dimensions, 1x1 through 1920x600, exact float depth including infinity/NaN, preserved positions, invalid dimensions and explicit byte-order handling. The original depth expression is independently reproduced in the test.

Actual RTX 5070/NVIDIA 616.64 OpenGL review passed Castle and all ten map types, zero unchanged mesh uploads, torch, candle flicker, blackout, door, resize, return and live TV/map controls. All 12 diagnostic PNG hashes equal the preceding pose-cache captures. All 24 final before/after benchmark PNG pairs match exactly. Captures here are runtime GPU diagnostics, not installed gameplay or whole-map quality acceptance.

GPU JAR SHA-256: `C1805462AD2B957095510F1F88D550A013E47BA30863850FD88A6A9960397CB2`.

## Matched hardware measurement

Same package and backend, byte-wise versus packed conversion; Java 17.0.12, Windows/RTX 5070, NVIDIA 616.64, 1280x800, default PSX preset. Three trials per site, 90 warmup + 120 measured frames per view, existing scripted camera sweep, in-memory isolated VerificationPreferencesFactory. Within each trial the before/after pair ran sequentially for House, Castle, Hollow Creek. No other local renderer test ran concurrently. Environment files record CPU, GPU, runtime and settings. This is a scripted completed-frame profile, not physical installed navigation.

The command uses `CastlePerformanceReview` with `beckenburg.profile.site`, `.width=1280`, `.samples=120`, `.warmup=90`, and `beckenburg.gpu.packedReadback.disabled=true` (before) / `false` (after). Both use the same JAR hash above. Root `summary.json`/`summary.csv` and `comparison.md` pool actual frame samples rather than averaging percentiles; trial median ranges remain included.

`readback-phases.md` separates color transfer, depth transfer, and CPU conversion (including output-array allocation). GL call-wall times include synchronization and are **not hardware GPU timestamps**. Each completed frame still reads 4,096,000 RGBA and 4,096,000 depth bytes.

Across eight views, median CPU conversion falls from **2.018–2.146 ms to 1.407–1.594 ms**, approximately **24–31%**. House/Castle pooled median frame times improve about **5–10%**; Hollow Creek total frame time is effectively unchanged. The latter remains governed by other work.

Tail limits remain visible: Castle exterior p95 increases from 17.54 to 20.91 ms and p99 from 50.92 to 51.62 ms in these trials. Its slow rows spend approximately 34–40 ms in CPU scene/render preparation while conversion generally remains 1.3–2.2 ms; these recurring relighting/preparation spikes are not eliminated by faster conversion. Do not infer whole-product acceptance or consistent tail improvement from the median gain.

Source CI: [GPU 40876041513](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40876041513), [full gameplay 40876045372](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40876045372) were running at this checkpoint. Later evidence/reporting-tool changes do not modify the runtime source. Physical installed play, main merge, release and full A–G completion remain open.
