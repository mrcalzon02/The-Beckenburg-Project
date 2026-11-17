# Engine and editor performance checkpoint — 17 November 2026

Starting main: `2dbb8b3a85ff6dbcac9a4dc9d1ba8fdce9dd8154`. The historical `c41bc66` checkpoint has already been integrated and superseded by the measured sanctuary work. This iteration changes the Material Editor painter; the projection-column engine experiment remains opt-in pending its physical comparison.

## Material Editor: retain an unchanged completed canvas

The original UV canvas redraws every native outline for every repaint, even when its document, selection, texture and projection are unchanged. A 120,000-triangle atlas allocates over 500 MB on the EDT during that Java2D repaint. Reusing immutable stroke/color objects reduces allocation but produced mixed timing results in the initial reversed-order comparison, so that experiment alone was not accepted as a latency fix.

The implementation now retains one completed opaque UV canvas and copies it on an unchanged repaint. Its key includes native state identity, material identity, both selected-face identities, visibility, UV tile, zoom/pan, dimensions, pins/seams/guide, font, background and rendering hints. Document changes and closure immediately release the old state references. Active pencil strokes, LCD subpixel text, printing, fractional/HiDPI transforms, nonstandard composites and oversized canvases use the direct painter. The image is capped at 4,194,304 pixels (16 MiB); the measured canvas uses 1,040,000 bytes. `-Dbeckenburg.editor.uvPaintCache=false` retains the direct fallback.

These are **headless Java2D EDT repaint timings**, not Windows monitor presentation or complete editor FPS. Both arms use Java 17.0.21, a 1 GiB heap, the same native grid, 520×500 ARGB targets and shared Xeon Platinum 8573C Linux executor. Only the workspace class differs. There are two fresh JVMs per arm, six warmups per case and twelve recorded unchanged repaints per JVM. The accepted order is baseline/candidate/candidate/baseline. See [provenance](editor-provenance.json), [raw CSVs and captures](editor-raw.zip), and [recomputed results including individual JVMs](editor-results.json).

| Unchanged UV canvas | Baseline p50 / p95 / p99 ms | Retained p50 / p95 / p99 ms | Allocated bytes, baseline → retained median |
|---|---:|---:|---:|
| Atlas edges | 1,238.004 / 2,836.539 / 3,809.594 | 0.659 / 1.132 / 1.283 | 531,415,688 → 1,288 |
| Atlas pins/seams | 1,259.212 / 2,760.342 / 2,918.257 | 0.945 / 1.642 / 1.927 | 541,976,888 → 1,288 |
| Tiled edges | 91.436 / 123.520 / 134.199 | 0.733 / 1.853 / 2.107 | 64,373,864 → 1,288 |
| Tiled pins/seams | 97.972 / 189.285 / 230.337 | 0.580 / 1.963 / 2.163 | 74,934,824 → 1,288 |

Every one of 112 matched whole-image hashes is identical, including the separately measured selection-invalidated paints. `EditorUvPaintCacheTest` passes 48 further direct/retained whole-image comparisons at a 256 MiB heap: overlapping sparse-ID faces, guides, filtering, preview/cancel, texture/origin, pencil, pan/zoom, mirrored tiles, resize/font/background, rendering hints, partial clips, translated views and fallback transforms. Existing row, pencil, UV transform and relaxation checks pass. The functional cache check is included in the normal regression manifest; timing is not a release gate.

**DEV-362 remains open.** Selection-invalidated frames still rasterize the full outline set. Four invalidation samples per case are recorded separately: candidate p95 ranges from 163–3,242 ms, with mixed before/after results. They must not be pooled with cache hits or presented as improved edit latency. Initial paint, changing previews, scaled/LCD/print paths and actual showing-window Windows latency still need further work. The small sample's p99 is its maximum, not a long-run tail guarantee.

Recompute after extracting the archive:

```sh
python tools/performance/editor_uv_report.py \
  --baseline extracted/pair-1-baseline extracted/pair-2-baseline \
  --candidate extracted/pair-1-candidate extracted/pair-2-candidate \
  --out editor-results.json
```

Compile/run `src/test/java/org/beckenburg/editor/EditorUvPaintProfile.java` against the same Java 17 game classes in fresh JVMs. For the original control, compile the workspace source at the pinned baseline into a separate class directory placed before the otherwise identical game classes. Source and class SHA-256 values are retained in provenance. Initialization, source construction, observe/list layout, image allocation and pixel hashing are outside the timed `canvas.paint` call. The counter covers allocations occurring inside that call.

## House engine: physical comparison

[Core30 run 44125683659](https://github.com/mrcalzon02/Dens-Mortis/actions/runs/44125683659) compares the optional horizontal-projection buffer using the same intact GPU JAR at `2dbb8b3`, with only `beckenburg.experimental.hazeColumns=false/true` changed. `2e7c31a` is the ancestry reference, not a second runtime arm. The private worker implementation is `446dbb8e3ad7463e4395da1bba46db778e5a44c1` and its isolated request is `fe8407e756f5d6a78954b55306b3c4cf9ba2a59c`.

It runs four counterbalanced pairs of solo and one rendered hunter with two TCP peers on the physical GTX 1660 SUPER. Every trial walks out, returns, takes one small step inside and holds for five seconds; both feature arms run independent optical parity controls and full TCP settlements. Raw consumed-frame IDs, all callbacks, live poses, per-phase process CPU, TCP dispatch and package/source hashes are retained. The six-frame stopped-pose check remains part of the audit. The optical controls also pass locally with both flag values. The fresh hardware outcome will be recorded here after the job terminates; no speedup or hitch removal is inferred from its running status.

`tools/performance/haze_columns_report.py` validates the original archive and recomputes p50/p95/p99, >33.3/>50/>100 ms counts, every individual trial, repeated-readback callback work, CPU and inside-stop poses. Frame gaps are between distinct consumed IDs within a phase. A repeated-ID draw contributes work to the following distinct completion; it is not a new displayed frame. These remain offscreen viewport measurements, not monitor scanout or cross-PC latency.

## Historical raw-frame verification

The retained [Core30 software archive](../2026-11-16-house-frame-times/core30-followup.zip) independently reproduces all four cold remote-hunter improvements: maximum moving post-geometry cost 318.893–327.502 → 27.834–28.889 ms; solo controls remain 26–28 ms. [Per-trial recomputation](historical-cold-peer.csv).

The retained [GTX exit archive](../2026-11-16-house-frame-times/windows-exit.zip) independently reproduces the still-open first-inside-step gap: 127.3485 ms between IDs 1420 and 1422, with two callbacks totaling 105.2389 ms of post-geometry work. [All >50 ms intervals with callback attribution](historical-hitches.json). This is the historical directional-cull study, separate from the fresh columns test. The rejected RGB-only and source-aware door-lighting experiments stay experimental; their measured swing/tail regressions are not promoted.

The renderer A–G program, all-map acceptance, sustained installed gameplay, cold edit latency and hardware presentation remain open. This checkpoint does not claim engine/editor performance completion.

Parent exact-main Windows acceptance [44124137497](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44124137497) completed with raw `passed=390 failed=0 skipped=0 manifest=390`. Installer [44124111288](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44124111288) also completed successfully. Those establish the reconciled parent, not acceptance of the new painter.
