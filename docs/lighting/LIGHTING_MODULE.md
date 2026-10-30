# Beckenburg Lighting Module

**Java 17 implementation:** `lighting-module/src/main/java/dev/mrcalzon/lighting/LightingModule.java`. The module contains immutable settings, built-in profiles, range validation and persistent local preferences. It does not own world state, Swing widgets, the GPU, or network authority. `FixtureLighting`, `SceneRenderer`, and the existing Castle Options screen consume it.

## Available Options

| Setting | Allowed values | Actual behavior |
|---|---|---|
| Profile | Performance / Balanced / Quality / Custom | Prepopulates the other fields; manual edits select Custom |
| Ambient fill | 50–150% | Scalar for ambient light contributions |
| Fixture brightness | 50–150% | Scales authored world-source power **and matching emissive fixture glow**; does not change handheld flashlight hardware |
| Fixture reach | 65–140% | Scales source radius and affected shadow regions |
| Cosmetic flicker | 0–200% | Controls candle and TV fluctuation only |
| Cache allowance | 50–150% | Scales supported CPU lighting, stationary-shadow, stable GPU and auxiliary resident-world budgets; **not a total JVM/GPU memory cap** (immutable House shell resource cache is not governed by this control) |
| Lighting invalidation cell | 2, 4, 8 metres | Spatial revision grouping; **not lightmap or ray-sampling resolution** |
| Distant outdoor LOD | On/off | Optional outdoor-only distance sampling, with physical LOS |
| Changed-only vertex updates | On/off | Reuses unchanged vertex colours/arrays |
| Source dependency reuse | On/off | Skips unchanged fixture-source probe batches |

**Performance, Balanced and Quality preserve identical authored ambient, fixture power, reach and cosmetic flicker.** Preset differences affect cache sizing and distant outdoor LOD, not evidence visibility. The brightness/reach sliders are explicit Custom appearance controls. Named presets load their canonical current definitions, automatically replacing obsolete per-preset values saved by earlier builds.

**Balanced** reproduces the preceding lighting defaults (100% values, four-metre cells, three optimizations on). Settings apply on **APPLY OPTIONS** and persist through Java Preferences. The active renderer notices a changed immutable snapshot and refreshes scene lighting caches **once**; individual doors and switches remain local updates, not global rebuild triggers.

**Multiplayer and game rules remain authoritative**: no client setting changes room power, switches, blackout, ghost hearing/vision, hidden evidence, collision, door state, or shadow ray correctness. No external Blender/Chocolatey/runtime asset dependency is added.

## Packaging and verification

Both `build.sh` and `build.cmd` now compile and package the standalone lighting module in `beckenburg-game.jar` and assert the class is present. `src/test/java/org/beckenburg/LightingModuleTest.java` is a separate optional diagnostic, **not a cosmetic release blocker**.

Pending acceptance: clean Java 17 Windows/Linux builds, actual Options-save/relaunch, House/Castle/solo/LAN parity, door open/close performance at each profile, stable scene IDs on ordinary door interactions, p50/p95/p99 frame time, upload bytes, cache hit rates, and no lighting through solid walls. Source commits and the packaging scripts alone do not prove those results.

## Initial implementation audit (2026-10-29)

Code review found and corrected: (1) the Performance and Quality presets had changed authored source strength and reach, conflating visibility with performance; (2) emissive bulbs/TV materials did not follow the world-source brightness slider; (3) named presets could reload saved obsolete custom values while still displaying the preset name; (4) the UI used misleading labels for world-source brightness, invalidation-cell size and cache **allowance**. Expanded optional diagnostic `LightingModuleTest` checks preset visual invariants, physical beam/math boundaries, and unchanged host lighting circuits.

**Known unverified / intentionally not claimed:** the immutable authored House-shell cache is not under the new percentage cache allowance; it needs a separate correctly bounded resource-ownership design rather than unsafe VBO eviction. Windows and installed GPU tests have not yet been run; the module's independent source/Java syntax and pipeline integration are subject to a clean actual JDK 17 full-game compile. No new release gate has been added.


## PSX lighting engine PDF cross-check (six-page supplied reference)

The supplied document recommends: 5-bit RGB quantization, per-vertex/Gouraud
illumination, optional clamping and overflow, per-mesh subdivision, ambient
and additive colour combination, alpha/STP hygiene, affine UV and screen
snapping, 4x4 ordered dithering, optional indexed CLUTs, avoiding T-junction
cracks, and avoiding excessive tiny batches.

**Already implemented elsewhere (do not duplicate in this module):**
- `PsxPipeline.finishFrame` quantizes and Bayer-dithers the finished world
  framebuffer; `PsxPipeline.snap` and `uv` implement the existing PSX
  presets. These are presentation effects and must not warp the Swing UI.
- `SceneRenderer` prepares vertex lighting, keeps resident source meshes
  with revisioned VBO IDs, uses limited near-camera subdivision and retains
  OpenGL colour interpolation. The handheld camera flashlight is deliberately
  a GPU fragment effect; converting it to a CPU full-world relight would
  recreate the door/camera hitch. Don't introduce another global light pass.
- `FixtureLighting` maintains hard occlusion checks against actual doors,
  geometry and source positions, as well as colour and shadow caches.

**Newly connected:** independent source reach and diffuse-edge width.
`LightingModule.Settings.diffusionPercent` (5–100%) sets how much of each
source radius is used for the fade band. At 100% it is identical to the
original `1-distance/radius` falloff; a narrower band retains a stronger
inner core. `LightModel.diffusionFade` is constant-time math, used by
both prepared-probe and live CPU samples. It adds no blur shader, shadow
map, full-screen pass, GPU texture, or new ray. This is light-edge softness,
**not a physically simulated penumbra or volumetric scattering**.

**Light cell layer priority:** the PDF does not prescribe light layers or
a priority mechanism. The implementation spatially indexes *contributing
sources*, then adds ambient and actual physically occluded source
contributions, with self-emissive surfaces applied as a max rather than an
unoccluded illumination source. Host power/outage/interference decide
fixture activation. There is no explicit ordered light-layer override
scheduler yet, and it must not be invented merely to choose a winning
lamp inside a spatial cell. If future gameplay effects require overrides,
they should be resolved before local light sampling and must never bypass
wall LOS or authoritative network rules.

**Post-processing:** `SceneRenderer` currently performs the ordered RGB
PSX pass and then `GraphicsEffects.apply`; these are two distinct
full-frame traversals when effects are enabled. They are measured
independently. Do not add a second shader dither, an automatic fragment
lighting mode, or a CLUT conversion on top of the current look without a
demonstrated need. `GraphicsEffects` already bypasses itself when all
additional effects are off.

**Performance diagnostics** (`LightingFrameProfiler`):
- A 120-frame bounded primitive-array rolling window records CPU frame,
  scene build, geometry/raster, PSX quantization/dither, and optional
  graphics-effects duration; it also records triangle high-water marks,
  shadow rays, changed lighting cells and delayed GPU readback counts.
- `SceneRenderer.lightingPerformance()` returns p50/p95/p99 frame times,
  p95 stage times and peaks on demand. Recording is O(1) with no sorting,
  allocations or I/O on the per-frame path. Percentile sorting is only
  performed when a snapshot is explicitly requested.
- Pass `-Dbeckenburg.lighting.profile=true` to opt in to one local
  `LIGHTING PERF` log approximately every five seconds. No telemetry,
  no online upload, and no added blocking GitHub test.
- These are **CPU timings**. Async GPU duration and VRAM residency require
  separate backend/driver instrumentation: GPU readback latency is a
  symptom metric, not a substitute for GPU timer queries.

**Follow-up acceptance**: compare all three presets at 200p, 360p and
540p for the same camera/door scene: p50/p95/p99 *frame* times,
PSX/post-processing ratio, after-door frame drop, retained vs rebuilt VBO
counts, GC pauses, CPU and GPU frame workloads, live shadow correctness,
T-junction gaps, and emissive/normal parity. Investigate any full-screen
CPU pass or shadow-probe burst that exceeds the chosen frame budget. As
always, non-functional appearance/performance diagnostics are non-blocking
unless the actual game does not function.

**Not included by default:** per-fragment Phong/PBR lighting, expensive
soft-shadow blur, CLUT conversion, variable per-pixel ray casting,
additional alpha simulations, or a large collection of microscopic
per-light draw calls. These are optional/editor concerns, not necessary
for an efficient PSX-style horror game.
