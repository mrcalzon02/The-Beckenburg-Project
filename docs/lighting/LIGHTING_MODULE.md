# Beckenburg Lighting Module

**Java 17 implementation:** `lighting-module/src/main/java/dev/mrcalzon/lighting/LightingModule.java`. The module contains immutable settings, built-in profiles, range validation and persistent local preferences. It does not own world state, Swing widgets, the GPU, or network authority. `FixtureLighting`, `SceneRenderer`, and the existing Castle Options screen consume it.

## Available Options

| Setting | Allowed values | Actual behavior |
|---|---|---|
| Profile | Performance / Balanced / Quality / Custom | Prepopulates the other fields; manual edits select Custom |
| Ambient fill | 50–150% | Scalar for ambient light contributions |
| Fixture brightness | 50–150% | Scales source power |
| Fixture reach | 65–140% | Scales source radius and affected shadow regions |
| Cosmetic flicker | 0–200% | Controls candle and TV fluctuation only |
| Cache budget | 50–150% | Scales lighting and resident GPU cache limits |
| Light probe cell | 2, 4, 8 metres | Spatial invalidation grouping |
| Distant outdoor LOD | On/off | Optional outdoor-only distance sampling, with physical LOS |
| Changed-only vertex updates | On/off | Reuses unchanged vertex colours/arrays |
| Source dependency reuse | On/off | Skips unchanged fixture-source probe batches |

**Balanced** reproduces the preceding lighting defaults (100% values, four-metre cells, three optimizations on). Settings apply on **APPLY OPTIONS** and persist through Java Preferences. The active renderer notices a changed immutable snapshot and refreshes scene lighting caches **once**; individual doors and switches remain local updates, not global rebuild triggers.

**Multiplayer and game rules remain authoritative**: no client setting changes room power, switches, blackout, ghost hearing/vision, hidden evidence, collision, door state, or shadow ray correctness. No external Blender/Chocolatey/runtime asset dependency is added.

## Packaging and verification

Both `build.sh` and `build.cmd` now compile and package the standalone lighting module in `beckenburg-game.jar` and assert the class is present. `src/test/java/org/beckenburg/LightingModuleTest.java` is a separate optional diagnostic, **not a cosmetic release blocker**.

Pending acceptance: clean Java 17 Windows/Linux builds, actual Options-save/relaunch, House/Castle/solo/LAN parity, door open/close performance at each profile, stable scene IDs on ordinary door interactions, p50/p95/p99 frame time, upload bytes, cache hit rates, and no lighting through solid walls. Source commits and the packaging scripts alone do not prove those results.
