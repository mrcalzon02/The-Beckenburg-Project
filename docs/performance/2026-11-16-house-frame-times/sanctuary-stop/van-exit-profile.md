# The first frame outside the House van

All eight solo and three-seat GTX trials of the four-lane shader at 30 ms reached the same first outward camera position. The frame ended at (18.11, 1.62, 36.25), facing away from the sanctuary. It spent 55.7–71.1 ms in post-geometry work. The physical trace records that stage as a whole. [Physical shading run and raw frames](../windows-shading.md)

On the local Mesa route, the projected sanctuary box straddled the near plane. That sent the renderer through the full 960×540 raster even though every screen ray pointed away from the nearest face of the box. Every one of the 518,400 pixels missed the sanctuary. A guard checks the sign of the minimum outward ray direction over the entire viewport before entering that fallback. The same authored shell and depth tests still handle views that can reach the box.

| Fresh local process | Sanctuary time on first exit, ms | Pixels shaded | Distinct GL frame gap enclosing exit, ms |
|---|---:|---:|---:|
| Control 1 | 134.670 | 0 | 160.934 |
| Guard 1 | 0.041 | 0 | 47.914 |
| Control 2 | 81.068 | 0 | 138.778 |
| Guard 2 | 0.050 | 0 | Incomplete route |

The second guarded process timed out later on the return route. Its crossing stage was recorded and its full-phase statistics are unavailable. At the same crossing position in warmed, standalone alternated calls, the two fresh JVMs gave median times of 3.595 to 0.042 ms and 3.903 to 0.052 ms. Each JVM also verified exact output colors, unmodified raw depths and shaded-pixel counts over 2,400 randomized camera views and 300 repeated exit/re-entry frames with moving occluders. This separates the intrinsic raster cost from its larger cold cost in the panel.

The archive contains the guard and instrumentation patches, captured logs and CSVs, exact-view replay, geometric proof and reproduction instructions. The guard is an experiment outside the v0.1.338 executable. Archive SHA-256 is `d5ef21077247226332bebaf3a65f0f1d84cbd5469e83f84dd1c3c46ff8be9ed3`. [Raw trace and reproduction files](van-exit-profile.zip)
