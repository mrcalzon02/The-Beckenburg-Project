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
