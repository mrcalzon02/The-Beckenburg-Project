# Terrain Editor Requirements — implementation contract

Basis: user-provided *Terrain editor requirements.pdf* (17 pages). This document
tracks implementation, not promises. The editor is a reusable Java 17 library:
the Beckenburg game renderer is an optional consumer, not its source of truth.

## Shipped in the standalone LevelEditorKit Java17 build

| Requirement | Implementation | Limit |
|---|---|---|
| Stitched 16-bit source heightmaps | `StitchedHeightmapWorld`, `StitchedWorldEditor` | Sparse tiles, 3×3 generation action |
| Procedural noise | `TerrainLayerStack.Noise`: Perlin, Simplex, Worley, ridged, billow, domain warp | Deterministic octave stack; no graph UI |
| Live low-res generation preview | Workbench `LIVE LOW-RES PREVIEW` | 2D analytical height preview, not live 3D |
| Non-destructive stacked sculpt layers | `TerrainLayerStack` | Up to 12 named layers with enable and opacity |
| Precision brush base | Raise/lower/smooth/flatten/revert/trench/terrace; inner radius, three curves, alpha images, rotation, event-independent dab spacing, elevation/slope guards | No mesh stamp-angle fitting yet |
| Localized differential undo/redo | Sparse per-vertex height changes, per stroke | Session history is memory-resident; other data edits not yet undoable |
| Terrain stamping | `stamp` and image-based height stamp in workbench | Height textures only; no arbitrary 3D mesh conversion |
| Thermal erosion | Talus relaxation and bounded selected-region iterations; protects source seams | Simplified four-neighbor solver |
| Hydraulic erosion | Seeded droplets constrained to active region, downhill sediment transport/evaporation | No persisted flow-velocity/sediment maps |
| Aeolian erosion | Directional mass transfer inside selectable region | No raycast wind shadowing yet |
| Slope/height auto-biomes | `autoBiome`, `slope` | Fixed rules; not yet a node-based rule graph |
| Surface materials | Eight stable material IDs and paint brush | Renderer/footstep/physics adapter must consume these |
| Freeze/exclusion masks | Rectangle freeze, material-based lock and brush respect | No mesh-hull raycast or cylinder masks yet |
| Infrastructure grading | `gradePath` with bounded segment rasterization (up to 50k samples), persistent `TerrainInfrastructureSplines`; saved route load/edit/drag/delete in workbench | Road/rail/river grading; not track mesh generation |
| Grade constraints | Rail/road maximum grade validation; clamp controls to target grade | No turnout, bridge or tunnel geometry |
| Background operations | SwingWorker for heavy generation, erosion, save, bake | Full GPU/physics pipeline not wired |
| Safe explicit source bake | Checks 16-bit range and source checksum; tile border frozen; 16-bit PNG import with seam validation | No multi-tile seamless atomic commit |
| Durable document | Versioned atomic binary layer and spline file writes | Source PNG remains authoritative until deliberate bake |
| Source/runtime separation | Editor package standalone JAR and separate functional CI | Game renderer and physics integration still incomplete |

Implementation entry point: `StitchedWorldEditor` > **LAYERED TERRAIN TOOLS**.
Standalone build: `tools/build-editor-kit.sh` or `tools/build-editor-kit.cmd`.
Tests: `TerrainLayerStackTest`, `TerrainInfrastructureSplinesTest`,
`TerrainBrushProfileTest`, `TerrainErosionRegionTest`;
workflow: `.github/workflows/standalone-editor-kit.yml`.

## Mandatory preservation rules

1. Existing 16-bit PNGs are authoritative until the user explicitly approves a bake.
2. No game-specific meshes, assets, level classes or renderer references may enter the
   portable terrain engines. Beckenburg-specific integration belongs in adapters.
3. A single-tile bake must preserve every shared edge sample exactly, or fail; no
   automatic seam patching or arbitrary overwrites.
4. A failed operation reports failure; never display a fake 3D/game screenshot.
5. Material IDs, masks and layer configuration survive baking. Edits support save/reopen.
6. Functional failures (compile, data corruption, broken save/reopen, required runtime
   integration) may block a release. Visual quality, texture look and style are diagnostic.
7. Preview calculations must not mutate terrain or generate excessive full-map work.

## Next integration passes, in dependency order

### Authoring data and precision
- Add independent Bezier falloff curves for plateau interior/exterior and brush presets; directional raise-only/lower-only flattening and image alpha/spacing/rotation are integrated.
- Add non-destructive per-layer masks, explicit per-pixel alpha and per-layer bounds.
- Add resampling with landscape-preserving interpolation and irreversible resolution-change
  preview. Preserve world-space datum, source seam positions and landmark control points.
- Add mesh-to-height stamping using real mesh ray projection, normalized histogram
  matching, slope/water/grid anchor snapping and generated stamp previews.
- Add disk-backed, bounded-memory differential undo/redo covering materials, layer
  operations, masks, splines and erosion. Unlimited logical history requires disk paging.

### Simulation and derived outputs
- Multi-tile localized erosion with halo samples across adjacent tiles, cancellation
  checkpoints, deterministic seeds, protected gameplay zones and conservative transfer.
- Persist hydraulic flow direction/velocity, sediment depth and moisture channels as
  versioned derived layers, with explicit invalidation on terrain edits.
- Implement wind shadow ray marching and angle-of-repose brush constraints.
- Tie vegetation, water drainage, terrain materials, collision and navigation to dirty
  region notifications. Recompute expensive physics on stroke completion, not per sample.
- Add background bake progress, cancel and crash-recovery journaling for multi-tile patches.

### Infrastructure / train module
- Expose persisted spline control handles, tangents, profile edits and path list in UI.
- Add railway cross sections: ballast, sleepers, rails, minimum curvature,
  banking/cant, switches, junctions, station platforms and multiple tracks.
- Grade embankments/cuttings while supporting our three seam/cutout entrance methods.
- Build and publish real bridge/tunnel geometry, not painted heightmap approximations.
- Produce portable spline and corridor output consumed by rail and AI/nav modules.
- Support multi-tile paths with atomic seam-aware update transactions.

### Materials, visualization and engine adapters
- Implement tile/face material-ID grids for strict retro mode, atlas packing,
  vertex colors, palette/nearest-neighbor rules, baked lighting and optional affine warp.
- Implement modern renderer adapter separately: normalized splat weights, height-blend
  transitions, slope-aware triplanar projection, macro-variation and physically anchored
  surface types. Actual material sample budgets are renderer/hardware specific; the
  source document's four-layer ceiling is a suggested optimization, not a universal law.
- Add biome-rule graphs with textures, foliage and audio rule outputs.
- Implement explicit adapter capability negotiation for virtual texturing, packed
  normal/roughness maps, terrain LOD and optional GPU displacement. Do not require
  modern APIs for the PSX renderer.
- Capture genuine editor/game screenshots for visual diagnostics, never as
  release-blocking tests.

## Definition of complete

Every named tool in the source document is either usable end-to-end with
save/reopen, undo, and source protection, or clearly marked unavailable in the UI.
Authoring data must be portable into non-Beckenburg projects. Runtime collision,
navigation, visual materials and physics require separate actual integration and
verification. The present work is a tested foundational implementation, not a claim
of full feature parity with the entire PDF.
