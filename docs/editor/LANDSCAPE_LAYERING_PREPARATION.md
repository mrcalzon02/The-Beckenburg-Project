# Landscape Layering — next specification preparation

**Status:** READY FOR IMPLEMENTATION AFTER TERRAIN REQUIREMENTS. This is
architecture/preflight for the user's eight-page `Landscape layering.pdf`,
not a claim that its material/shader features have been implemented.
The first specification remains authoritative for active work:
[Terrain requirements integration](TERRAIN_REQUIREMENTS_INTEGRATION.md).

## 4-tier design from the supplied document

| Tier | Document terminology | Reusable editor responsibility | Engine integration boundary |
|---|---|---|---|
| 1 | Global Inputs & Data | Shared world coordinates, deterministic source heights, terrain material IDs, protection masks, wind, derived curvature/flow/moisture | Neutral attribute buffers and revision/fingerprint IDs |
| 2 | The Material Domain | Grass, soil, rock, snow and extensible stable material profiles, per-material UV scale, optional packed maps | Unreal/Unity-specific Material Attributes and `SetMaterialAttributes` belong to host adapter, NOT the editor model |
| 3 | Blend Domain Matrix | Curvature, height, slope and flow classifiers, priority/weight normalization, horizon-noise variation, override masks | Portable evaluator and diagnostic mask previews; host may compile a shader |
| 4 | Master Output Graph | Layered terrain outputs, PSX tile-ID/vertex-color or optional modern material inputs | Renderer-owned RVT/parallax/normal/displacement facilities, with explicit capability flags |

The document defines the topology chain as elevation -> structural slope ->
steep/flat decisions and low-elevation curvature/AO -> cavity/ridge decisions.
Keep this separable from authored terrain heights. It also recommends
`SetMaterialAttributes`, `StaticSwitchParameter`, `Shared: Wrap`,
`PixelNormalWS`, `VertexNormalWS`, RVT and shader-wave optimization.
These names are **Unreal/HLSL-specific examples**, not portable Java 17 API
requirements. Implement equivalent semantics in engine-specific adapters,
never by adding dependencies to `org.beckenburg.editor`.

## Existing handoff contracts

- `StitchedHeightmapWorld`: authoritative, seam-validated R16 source tiles.
- `TerrainLayerStack`: non-destructive height, protection, legacy material IDs,\n  and individually paintable sculpt-layer alpha masks (v2), with source bake,\n  source-format v1 migration and sparse mixed-edit history.
- `TerrainTopologyFields`: derived terrain normals/slope proxy,
  curvature, D8 drainage, flow-direction/speed estimates and moisture proxy.
  Fields are **not** actual hydraulic sediment particle measurements.
  Fingerprints invalidate derived maps when sculpting changes heights.
- `TerrainInfrastructureSplines` v2: legacy routes remain unmodified on
  migration; newly managed road/rail/river alignments supply deterministic
  reversible procedural terrain grading, rebuilding on edit/delete with
  collision and authored mesh heights using the same sampler. Route material
  overlay and track junction geometries remain pending.
- `TerrainRailMesh`: portable genuine ballast, sleepers and left/right rail\n  geometry exported through the shared terrain mesh writer; optional host\n  adapter still needed for live rendering/collision.\n- `StitchedHeightmapWorld.importTilesBatch`: seam-aware, rollback-journaled\n  source PNG replacement across up to nine tiles, with recovery on reopen.
- `TerrainComposedHeightSampler`: source plus saved sculpt-layer deltas and
  alpha masks use one identical height sampler for actual exported terrain mesh
  geometry and physical surface/navigation queries; scene fingerprints include
  layer files. Long-lived query consumers explicitly refresh after edits.
- `TerrainLayerStack`: blocks stale saves after another editor changes either
  the source tile or authored document; the source batch journal can rebase
  layer checksum metadata without changing its creative content.
- `TerrainSeamSculpt` and `TerrainSeamSculptEditor`: global-vertex
  non-destructive seam editing, sparse undo, session save/reopen, source
  fingerprint checks, discard, and guarded explicit batch bake. Source-bake
  transactions now rebase v1/v2 layer documents in the SAME recovery journal;
  live cross-tile sculpt-mask painting is still a separate authoring task.

## First implementation tranche, after finishing Terrain editor requirements

**Route/material priority boundary:** Reversible infrastructure grading
already preserves ordinary material paint. The next material-domain evaluator
must combine user-painted material IDs and managed route corridor preferences
explicitly, not repaint the base material array or treat source heightmap
baking as a procedural-route save.

1. **Stable material catalog.** Material profiles with immutable numeric IDs,
   albedo/tile atlas/normal/roughness/AO/height URI slots, physical surface type,
   texture scale and supported render profiles. Keep legacy material IDs stable.
   Add Moss and Concrete to the catalog for the document's nine-material test.
2. **Neutral four-tier evaluation model.** Typed inputs, material description,
   topology blend rule graph and output buffer; no host engine class imports.
   Rules include slope normals, height-gradient start/range with world-space
   low-frequency perturbation, curvature/cavity classification and downstream
   moisture. Human-authored material overrides have declared priority.
3. **CPU reference evaluator and real preview.** Deterministic testable topology
   masks and material assignments for a selected region, with ability to
   inspect each tier separately before any shader is involved.
4. **Renderer capability profile.** `RETRO_PSX` baseline: tile material-ID
   lookup, atlas/nearest-neighbor, vertex lighting; optional `MODERN`
   with normalized layer weights, height blending, distance UV biasing,
   triplanar, packed textures and RVT only if host supports them.
   Mobile/standalone-VR profiles use a lean fallback and no mandatory RVT.
5. **Nine-material stress scene.** Grass, Dirt, Gravel, Rock, Sand, Mud,
   Moss, Snow, Concrete. Test all authored material IDs decode, blend
   weights normalize, no missing resource references, and all configured
   host rendering paths build and launch. Shader complexity/pixel imagery
   are **diagnostic outputs only**; visual style does not block release.
6. **Host-specific shader adapters.** Only after neutral evaluator is
   stable, implement adapters to actual Beckenburg/other project renderer
   capabilities: UV inputs, shared sampler state, optional static feature
   variants, distance LOD, packed texture maps, and RVT where supported.

## Source-derived feature inventory (eight-page document)

- Four tiers and package-each-material-attribute guidance.
- Material-function instruction-count *target* (<35 recommended).
- Shared UV inputs, world-space dot-product slope, vertex/pixel normal mix.
- Height falloff with noise-warped elevation boundary.
- Curvature/cavity or baked AO approximation.
- Sampler sharing and near/far UV blend.
- RVT/virtual texture optional optimization, with matching texture
  dimensions and power-of-two mip layouts.
- Two packed maps: albedo RGB + height A; normal XY + roughness B + AO A.
- Height-field contrast blend scalar `Global_Blend_Contrast` (source
  recommendation: 0.02–0.07), plus flow-vector-driven silt/debris.
- Static feature variants and optional SM6 wave-based culling.
- Platform split: desktop/console modern optimization vs. mobile/VR
  low-overhead, no required displacement/RVT.
- Nine-layer overlap stress test, close/far shader complexity inspection.

## Explicit engineering cautions

- Do not reproduce the PDF's Unreal node names in portable core interfaces.
- Do not treat `Shared: Wrap` as a universal setting outside Unreal or
  assume a universal four-layer/16-sampler limit across all GPUs.
- Do not promise that an RVT page makes an arbitrary number of materials free;
  baking, updates, residency and memory remain real runtime costs.
- Flow-driven dirt/silt shaders must distinguish **derived drainage proxies**
  from physically simulated sediment fields.
- Do not call purely analytical previews an actual in-game render.
- Keep hard release gates strictly functional; shader complexity, visual
  artifacts and retro style remain diagnostic screenshot-based review.

## Preconditions for starting active implementation

- Verify standalone Java 17 editor build with first-spec tests.
- Finish / deliberately scope missing first-spec features: multi-tile seam-
  safe editing, true infrastructure geometry, robust rerouting, materials
  and collision/nav integration.
- Add first-spec material/terrain dependency dirty-region notifications.
- Then implement the four tiers from the bottom upward; verify generic
  project adapters before connecting Beckenburg-specific rendering.
