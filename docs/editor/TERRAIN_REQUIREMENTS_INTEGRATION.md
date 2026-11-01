# Terrain Editor Requirements — implementation contract

Basis: user-provided *Terrain editor requirements.pdf* (17 pages). This document
tracks implementation, not promises. The editor is a reusable Java 17 library:
the Beckenburg game renderer is an optional consumer, not its source of truth.

## Shipped in the standalone LevelEditorKit Java17 build

| Requirement | Implementation | Limit |
|---|---|---|
| Stitched 16-bit source heightmaps | `StitchedHeightmapWorld`, `StitchedWorldEditor` with seam-validated, journaled batches (up to nine tiles), crash recovery, and interactive shared-vertex seam sculpt | The cross-tile brush uses its own non-destructive session; merging it with sculpt-layer v2 masks is pending |
| Procedural noise | `TerrainLayerStack.Noise`: Perlin, Simplex, Worley, ridged, billow, domain warp | Deterministic octave stack; no graph UI |
| Live low-res generation preview | Workbench `LIVE LOW-RES PREVIEW` | 2D analytical height preview, not live 3D |
| Non-destructive stacked sculpt layers | `TerrainLayerStack` v2 with editable per-layer sample masks, enable and opacity; existing v1 files are readable | Up to 12 layers; source heightmaps remain unchanged until explicit bake |
| Non-destructive seam authoring | `TerrainSeamSculpt`: unique global vertices, 3×3 viewport, sparse undo/redo, save/reopen, guarded source bake; coordinated layer-document rebasing preserves v1/v2 sculpt deltas, masks, materials and freeze state | Seam edits are still kept in a separate authoring session until bake; interactive cross-tile layer-mask editing remains pending |
| Precision brush base | Raise/lower/smooth/flatten/revert/trench/terrace; inner radius, three curves, alpha images, rotation, event-independent dab spacing, elevation/slope guards | No mesh stamp-angle fitting yet |
| Brush hot-swap presets | `TerrainBrushPresets`: versioned atomic save/load/delete, bundled alpha and numeric profiles | Presets scoped to a world; share/export UX later |
| Localized differential undo/redo | Mixed sparse height, material paint, freeze/unfreeze, material locks and layer enable/opacity history; painted drag grouped into one edit | Session history is memory-resident; layer add/delete and route documents are not yet undoable |
| Terrain stamping | `stamp` and image-based height stamp in workbench | Height textures only; no arbitrary 3D mesh conversion |
| Thermal erosion | Talus relaxation and bounded selected-region iterations; protects source seams | Simplified four-neighbor solver |
| Hydraulic erosion | Seeded droplets constrained to active region, downhill sediment transport/evaporation | Droplet speed/sediment outputs not yet persisted |
| Aeolian erosion | Directional mass transfer inside selectable region | No raycast wind shadowing yet |
| Slope/height auto-biomes | `autoBiome`, `slope` | Fixed rules; not yet a node-based rule graph |\n| Derived topology / drainage | `TerrainTopologyFields`: analytical curvature, slope, D8 flow direction/speed estimates, accumulation and moisture proxies; versioned disk cache + height fingerprint | Not simulated droplet flow/sediment; no cross-tile drainage yet |
| Surface materials | Eight stable IDs; explicit authored-material bitset; reversible GRAVEL/DIRT/RIVERBED managed corridor surfaces; paint and erase-override brush; preview + world-space physical material query | Live Beckenburg renderer/footsteps still require a host adapter; no blending between route shoulder materials yet |
| Freeze/exclusion masks | Rectangle freeze, material-based lock and brush respect | No mesh-hull raycast or cylinder masks yet |
| Infrastructure grading | Bounded grade rasterizer with reversible road/rail/river height and material overlays; route move/delete rebuilds without destroying painted material/height source; legacy v1 routes remain unmanaged | Old painted legacy grades lack provenance; procedural overlays still do not alter shared border samples |
| Grade constraints | Rail/road maximum grade validation; clamp controls to target grade | No turnout, bridge or tunnel geometry |\n| Real railway track geometry | `TerrainRailMesh` builds ballast prisms, two extruded rails and spaced sleeper meshes from RAIL alignments; shared real-mesh inspector and validated OBJ export | Geometry-only: no switch, bridge, tunnel, rolling-stock physics or host-renderer adapter |
| Background operations | SwingWorker for heavy generation, erosion, save, bake | Full GPU/physics pipeline not wired |
| Safe explicit source bake | Checks R16 range and checksum; journaled batches can include both source PNGs and existing authored layer documents under explicit opt-in, with rollback of both | Multi-file writes are journal-recoverable on reopen, not atomically visible to concurrent external readers |
| Durable document | Backward-compatible v1/v2 terrain reads; v3 explicit paint-provenance and sculpt-mask writer; SHA-256 stale-save safety and atomic binary layer/spline writes | v1/v2 intentionally painted GRASS cannot be distinguished from their unpainted default, so historical GRASS provenance cannot be reconstructed |
| Dependency dirty regions | `TerrainLayerStack.EditEvent`: per-transaction bounds, affected domains, revisions and explicit listeners; no per-dab callbacks | Consumers must subscribe and schedule actual foliage/nav/physics rebuilds; no runtime adapter yet |
| Shared authored-surface geometry | `TerrainComposedHeightSampler` provides one exact triangle-height function to `TerrainEntranceMesh` and `LayeredTerrainQueries`, using saved unbaked sculpt/mask layers; source scene fingerprint includes layer files | Runtime Beckenburg host renderer/collision adapter not yet wired; long-lived queries must explicitly refresh cached data after layer saves |
| Concurrent document safety | Saved layer documents use SHA-256 optimistic readback and reject stale edits/bakes; coordinated rebases retain authored metadata | Cross-process source reads are not fully atomic |
| Source/runtime separation | Editor package standalone JAR and separate functional CI | Game renderer and physics integration still incomplete |

Implementation entry point: `StitchedWorldEditor` > **LAYERED TERRAIN TOOLS**.
Standalone build: `tools/build-editor-kit.sh` or `tools/build-editor-kit.cmd`.
Tests: `TerrainLayerStackTest`, `TerrainInfrastructureSplinesTest`,
`TerrainBrushProfileTest`, `TerrainBrushPresetsTest`,
`TerrainErosionRegionTest`, `TerrainMixedHistoryTest`,\n`TerrainTopologyFieldsTest`, `TerrainEditEventTest`,\n`TerrainRailMeshTest`, `StitchedHeightmapBatchTest`,\n`TerrainLayerMaskTest`, `TerrainSeamSculptTest`,
`StitchedHeightmapLayerRebaseTest`, `TerrainLayerConcurrentSaveTest`,
`TerrainComposedHeightSamplerTest`, `TerrainManagedInfrastructureTest`,
`TerrainRouteMaterialOverlayTest`;
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
- Add independent Bezier falloff curves for plateau interior/exterior and project-independent preset sharing; local brush preset save/load and directional flattening, alpha/spacing/rotation are integrated.
- Add per-layer editable extent bounds and mask clipping profiles; per-vertex per-layer sculpt masks are now implemented with undo/redo, view inspection and versioned save/reopen.
- Add resampling with landscape-preserving interpolation and irreversible resolution-change
  preview. Preserve world-space datum, source seam positions and landmark control points.
- Add mesh-to-height stamping using real mesh ray projection, normalized histogram
  matching, slope/water/grid anchor snapping and generated stamp previews.
- Add disk-backed, bounded-memory differential undo/redo covering materials, layer
  operations, masks, splines and erosion. Unlimited logical history requires disk paging.

### Reversible infrastructure authoring
- Managed route v2 grades now rebuild from spline metadata, preserve hand-sculpted heights
  and material paint, and are sampled by terrain mesh and physical queries even
  without a saved ordinary sculpt document.
- Legacy v1 route records retain their original directly-painted semantics; no
  fictitious reconstruction of already overwritten heights is attempted.
- Managed route material overlays now display GRAVEL for rails, DIRT for roads,
  RIVERBED for rivers; explicit user-painted GRASS/MUD wins. Erasing paint
  overrides reveals the route again, with grouped undo/redo and v3 persistence.
- Next: junction/switch geometry, bridge/tunnel physical structures, blended
  material shoulders and multi-tile grade seam coordination.
- While managed routes exist, source bake refuses to flatten their procedure
  irreversibly into R16. Export authored mesh geometry instead.

### Simulation and derived outputs
- Multi-tile localized erosion with halo samples across adjacent tiles, cancellation
  checkpoints, deterministic seeds, protected gameplay zones and conservative transfer.
- Add actual hydraulic particle velocity, sediment depth and wetness outputs to the existing versioned topology cache (which currently stores only height-derived D8 drainage and moisture estimates); preserve explicit input-fingerprint invalidation.
- Implement wind shadow ray marching and angle-of-repose brush constraints.
- Connect host-level foliage, water, materials, collision and navigation rebuild adapters to the portable dirty-region contract; editor-side mesh and support queries share the same composed height sampler, but host collision and actual rendering remain unverified. Long-lived queries expose explicit refresh rather than per-sample file scans.
- Add background bake progress, cancel and crash-recovery journaling for multi-tile patches.

### Infrastructure / train module
- Expose persisted spline control handles, tangents, profile edits and path list in UI.
- Add railway cross sections: ballast, sleepers, rails, minimum curvature,
  banking/cant, switches, junctions, station platforms and multiple tracks.
- Grade embankments/cuttings while supporting our three seam/cutout entrance methods.
- Build real switches, bridge/tunnel geometry and route intersections, not painted heightmap approximations. Real standalone rail/ballast/sleeper geometry is now exported as OBJ.
- Produce portable spline and corridor output consumed by rail and AI/nav modules.
- Integrate cross-tile edits into v2 per-layer sculpt masks while keeping shared vertices seam-safe *before* source bake. Coordinated source PNG + saved layer CRC rebasing is now journaled and implemented; the cross-tile edit session remains separate until bake, and cross-process atomic visibility is not available.

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

## Next document handoff\n\nThe supplied *Landscape layering.pdf* (eight pages) is separately analyzed in\n[LANDSCAPE_LAYERING_PREPARATION.md](LANDSCAPE_LAYERING_PREPARATION.md).\nIts four-tier material/shader architecture remains pending until the first\nterrain authoring specification reaches an explicit, verified scope boundary.\n\n## Definition of complete

Every named tool in the source document is either usable end-to-end with
save/reopen, undo, and source protection, or clearly marked unavailable in the UI.
Authoring data must be portable into non-Beckenburg projects. Runtime collision,
navigation, visual materials and physics require separate actual integration and
verification. The present work is a tested foundational implementation, not a claim
of full feature parity with the entire PDF.

## Unified owner requirements document chain (2026-10-31)

This remains the detailed **terrain** implementation contract. The added topology,
soundscape, engine base and multi-genre source inputs are registered **alongside**
this document, not instead of it: [source chain](requirements/DOCUMENT_CHAIN.md)
and [dependency-ordered gates G0–G12](requirements/STAGED_INTEGRATION_PLAN.md).
The prior landscape-layering handoff remains active; feature acceptance still
requires code and runtime tests rather than this documentation link.
