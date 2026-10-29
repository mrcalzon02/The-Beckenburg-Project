# Castle and shared-map hardware evidence

The authoritative explanation and limitations are in [the performance report](../../CASTLE_REGRESSION_2026-10-28.md).

- `matched/old`: three previous-Castle source trials (`68d1142d`) with timing-only instrumentation.
- `matched/before`: three slow-version source trials (`ba3d8885`) across Castle and all ten investigation map types, with timing-only instrumentation.
- `matched/after`: three corrected packaged-source trials (`06b76cce`), without source overlays.
- `matched/summary.csv` and `comparison.md`: pooled nearest-rank percentiles, 360 samples per view. Per-trial median ranges are retained.
- `high-resolution`: one supplementary 1280×800 trial over Castle, House, School and native Hollow Creek, with the same warmup/sample lengths. This does not have three-trial confidence.
- `corrected-checkpoint`: actual GPU controls and six scene-change memory cycles. Memory is Java heap, not VRAM; the diagnostic explicitly collects between cycles.
- `captures`: actual GPU diagnostics and actual offscreen Castle UI captures. These are rendering evidence, not photographs of installed physical traversal.
- `preliminary`: earlier experiments, including rejected or superseded candidates. These do not replace final matched data.

Matched settings: 640×400, Classic PSX, lights on, clock/flicker advancing, 90 warmup frames, 120 measured frames, small continuous camera turns, same renderer/map instance throughout each trial. Intel Core i7-14700K, RTX 5070, NVIDIA 616.64, Windows 11 build 26200, Java 17.0.12. Completed-frame latency includes CPU submission and waiting for the exact GL result; GL wall times are not hardware timer queries. Allocation counts only the submission thread.

The generic `interior` CSV pose is an outdoor authored reference for native Hollow Creek. Existing user applications were left running; no other profiling jobs or builds ran during final sampling.

Reproduction: use `tools/profile_world_performance.ps1`, then `tools/summarize_world_performance.py`. Provenance records the source label, working checkout, JAR hash, and any complete-source overlay class hashes. The old/pre-fix class overlays take precedence over the dependency/resource JAR; its own source revision is not the measured source. No user save files, security certificates, or isolated test-state trees are included here.

No release, complete A–G acceptance, all-22-campaign-map audit, or installed investigation-loop acceptance is inferred from these artifacts.
