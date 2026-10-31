# Beckenburg Adaptive Terrain Topology — design and staged implementation

**2026-10-31 — scope:** human-authored, persistent, sparse recursive terrain refinement *within* independently stitched heightmap tiles. This is **not** a replacement for the tiled 16-bit heightmap source and **not** distance-based rendering LOD. Continue human-accessible editor modules before any machine/agent interface.

## 1. Sketch: topology is authored where needed

```text
STITCHED 16-BIT SOURCE TILES (tile edges share exact height values)
+---------------------+---------------------+
| coarse hillside     | cliff base          |
| . . . . . . . . . . | . . . . . . . . . . |
|          +----------+----------+          |
|          |  fine    |  coarse  |          |
|          | +--+--+  |          |          |
|          | |++|++|  |          |          |
|          | +--+--+  |          |          |
|          +----------+----------+          |
+---------------------+---------------------+

One original heightmap quad
 depth 0: [           one base cell           ]
 depth 1: [        ][        ]  each axis => 4 children
 depth 2: [    ][    ][    ][    ]  selected children subdivide again
 depth 3+: recursive refinement only where authoring requires it

Independent, orthogonal systems:
 SOURCE: stitched 16-bit heights and their provenance
 AUTHORING: persistent quadtree split nodes + future sparse high-resolution height deltas
 DISPLAY: distance/zoom-dependent mesh LOD derived from canonical authored world
```

Note the drawings illustrate *quadtree subdivision*, not proof of a crack-free runtime mesh.

## 2. Authoritative coordinates, boundaries and persistence

- Global **X/Z** with Y upwards (metres). A canonical source tile contains N×N *base cells* plus N+1 height samples on each side; heightmap decoding/triangles remain the responsibility of `StitchedHeightmapWorld`.
- An adaptive root is exactly one base cell indexed by globally signed integer `(gx,gz)`; e.g. tile boundary cell X `(tx*N+N-1)` is adjacent to `((tx+1)*N)`. Do not introduce tile-local boundary identities that could disagree.
- Node address `(gx,gz,depth,localX,localZ)`, with `0 <= localX,localZ < 2^depth`. Nodes subdivide into NW/NE/SW/SE quadrants. Only explicitly split nodes persist; unmodified cells consume zero per-cell storage. Parent closure, max subdivision depth, a minimum physical edge length and a node-budget limit prevent runaway allocations.
- For portable, exact edge comparison use integer **dyadic** coordinates at a fixed maximum subdivision lattice, `unit=2^MAX_DEPTH` per base cell. Negative global coordinates must use floor division. Refinement/splitting of base-cell edges must work across PNG tile boundaries.
- A new vertex created by subdivision initially samples the *exact source triangle* from the stitched heightmap (not bilinear averaging). Splitting **does not raise/lower** terrain by itself.
- Future authored displacement is a separate sparse vertex layer keyed by **global dyadic integer coordinates**, with one displacement value per shared vertex. An edit touching a tile edge is a multi-tile transaction; never create two different heights for the same vertex.
- Topology files are versioned, bounded, atomic-save, tied to the heightmap world's exact grid/settings and validated on load. Reopen must preserve the same split tree and edge data. Source tile PNGs are never rewritten by splitting/merging metadata.

## 3. Boundary conformity: no naive T-junction meshes

If a leaf's opposite side is subdivided more deeply, the coarse leaf must *also* expose its neighbor's edge-break positions to the eventual mesh builder. The geometry adapter is required to triangulate against those shared break positions **using identical positions and heights**. Returning lists of edge breaks is *not* sufficient proof of a watertight GPU mesh.

We choose **edge-conforming polygon triangulation** over automatically refining a whole continent to the same depth. Recursive split balancing (neighbor depth gap <= 1) may be added as a bounded optimization, not a hidden requirement of editing. Unequal depths can meet only when the mesh builder consumes all edge breaks; arbitrary-depth mismatch must not silently become skirts or a visual-only seam mask.

The source core must expose: leaf enumeration, world/dyadic bounds, neighbor edge-break positions, source-sample lookup, split/merge/snapshot/readback and explicit failure modes.

**Failure cases:** absent/unloaded source tile, depth/budget overflow, non-leaf split, missing parent, conflicting PNG edges, malformed/cross-world topology document, save failure, stale revision, and wrong-thread UI use. No operation should partially mutate state on a validation error.

## 4. Terrain physics, distant vision and limits of heightfields

- **Height ownership:** before runtime adapter T3, this module only stores authoring topology. Nothing should pretend that a refined mountain automatically appears in gameplay. The runtime adapter must use the identical triangulated authored surface for ground contact, projectile/ray queries, AI navigation and mesh emission.
- **LOD is downstream:** authored splits do not disappear because camera distance changes. Distant LOD may approximate, but must retain an error envelope and fine-resolution corridor refinement for scopes and distant actors. Exact terrain determines true occlusion; GPU depth must also be refined or corrected to avoid burying visible actors behind coarse proxy geometry.
- **Caves/overhangs:** a heightfield (even adaptively refined) still represents only one Y per X/Z. Use separately owned solid mesh/volumetric geometry and ground cutout/portal masks for caves, bridges, tunnels and cliffs with undersides.
- **Material detail:** textures/atlas UV density, splat masks, decals and surface displacement should use independent resolution policy. Increasing geometric topology is not the same as increasing texture resolution; prevent atlas bleeding across tile/LOD seams.
- **Precision:** double-precision global metres (and integer global dyadic indices) for authoritative address; subtract rebased render origin before GPU float conversion.

## 5. Stages, acceptance and status

| Stage | Deliverable | Tests |
|---|---|---|
| A0 DESIGN | This explicit contract and dependency map | Correct base-cell/address examples and failure cases |
| A1 CORE | Sparse Java 17 split/merge quadtree, leaf enumeration, global dyadic bounds, cross-cell edge breaks, snapshot/restore, bounded atomic topology file | Nested splits, signed coordinates, 16-bit tile boundary, no source change, save/reload, corrupt file rejection, node limits |
| A2 HUMAN UI | Choose tile/base cell/subcell, refine/merge, undo/redo, inspect topology density and edge breaks; save explicitly | Human edits 3×3 world, sees density change, save/reopen exact, existing editor unchanged |
| A3 HEIGHT DETAIL | Non-destructive sparse dyadic vertex height offsets, world ownership, atomic cross-border edits and revision/invalidation | Cross-border brush, no cracks, failed batch rolled back, true triangle surface and saved readback |
| A4 GAME ADAPTER | Conforming triangle generation, collision/nav/ray consistency, real-world rendering and chunk invalidation | Captured real renderer, terrain foot heights/edge crossing, no visible seam; runtime uses authored surface |
| A5 DISTANCE & SCOPES | LOD selection, min/max error envelopes, scope/actor correctness | Human-sized distant actor genuinely visible vs ridge-hidden, real rendered z-buffer, teleport/zoom/origin-shift |
| A6 AGENT ACCESS | Observe, manipulate and verify same approved editor operations, later | Authorized real-scene readback and undo; no parallel simulated scene |

**Current implementation:** A0 spec committed. A1 core and regression tests
passed Windows Java 17 focused CI at e7eca44c ([run 41229681626](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/41229681626)).
A2 human interface source was added subsequently; compilation and a physical
Windows UI interaction still require separate verification. A3–A6 remain planned. A1 or A2 is **not** A3–A6. Finite TerrainZoneDocument and existing loading-zone handoff remain independent.

## 6. Reference basis

- Frostbite *Adaptive Hardware-accelerated Terrain Tessellation* documents T-vertices and stitch permutations: https://media.contentapi.ea.com/content/dam/eacom/frostbite/files/adaptive-terrain-tessellation.pdf
- Adaptive mesh discussion and edge-split remedy: https://www.mdpi.com/2071-1050/11/7/2137
- Quadtree/continuous LOD historical reference: https://courses.cs.duke.edu/cps124/fall02/notes/12_datastructures/lod_terrain.html

These are architectural references, not imported dependencies or assertions that our engine has completed its renderer.


## 7. Reuse of the model-showcase DEV room (2026-10-31)

User requested topology testing alongside existing exhibits. Beckenburg's
\`--dev-room\` is a separate actual-renderer *model showroom*, not a gameplay
heightfield map, so we reuse its renderer and free camera without registering
the QA patch as a mission or moving the six model stands.

- \`DevTerrainTopologyLab\` creates/reopens an isolated 2-tile 16-bit test world
  at \`~/.beckenburg/dev-room/topology-qa\`. It seeds a depth-1/depth-2 example
  immediately against the neighboring unsplit cell at the original PNG boundary;
  an existing QA source is never overwritten.
- \`DevAssetShowroom\` adds explicit on/off, edit and reload controls. The patch
  is opt-in in an unused side aisle. Existing model catalog and unmodified
  3-argument \`SceneRenderer.drawDevAssetRoom\` still render exactly as before.
- \`DevTerrainTopologyLab.build\` enumerates real authored leaves, gathers
  each neighbor's global edge-break positions, triangulates an edge-conforming
  polygon fan, and samples the **actual source triangle** height. Its triangles
  are passed into the existing software 3D renderer for live depth tests.
  Depth colors are diagnostic. There is **no independent authored vertex
  displacement, terrain-body collision, lighting/texture atlas integration,
  character occlusion proof or gameplay runtime adapter** yet.
- Reopen / reload reads actual saved topology from disk. A malformed document
  reports failure and keeps the prior lab geometry rather than faking success.
- Optional headless DEV capture: \`DevAssetShowroom --dev-room --terrain-qa-capture
  --output=build/dev-room\` writes a *real* scene renderer screenshot and
  a source/triangle-count report. The visual review CI treats the capture as
  advisory. Verify Windows CI success and capture artifact separately before
  calling the DEV lab fully accepted.

QA tasks: manually split tile 0 cell (15,7) along the tile 1 boundary, inspect
the resulting diagnostic subdivisions in the real 3D renderer, save/reload,
verify both coarse/fine sides draw without a visible gap, compare source PNG
hashes, and document FPS and rendering artifacts. The existing model asset
cameras and screenshots must still behave unchanged.

This is **a staging fixture for A2 and early A4**, not proof that A3 authored
sculpting, physical collision or T5/T6 distant scope occlusion works.


## 8. Cutout and multi-level navigation requirements (2026-10-31)

**Correction to the earlier design:** adaptive topology must support punching true holes in terrain and multiple overlapping 3D floors/ceilings at the same X/Z for caves, caverns, mines, bunkers and sewers. A heightfield alone cannot provide this. Sparse cutout leaf flags are now in authoring format v2; the human editor has cut/restore controls, and the isolated DEV patch suppresses cutout triangles. Underground mesh, matching collision, portal connectivity and layered gameplay are NOT yet implemented.

See [Layered Terrain and Real Excavation](TERRAIN_LAYERED_CAVITIES.md) for authored surface cuts, 3D subsurface layers, entrance perimeter welding and collision/occlusion acceptance.

**Separate requirement:** low/high rendering mesh densities must not respectively under- or over-sample NPC waypoints. Use independently built adaptive nav regions with physical clearance sampling, explicit layer/portal IDs, hierarchical routes and verified local swept movement. See [Adaptive Multi-Layer Navigation](ADAPTIVE_MULTILAYER_NAVIGATION.md) for the staged design and diagnostics. Camera/render LOD must never become NPC collision authority.
