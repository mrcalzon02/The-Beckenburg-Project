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

**Initially:** design only. As modules land, record their actual commit and CI evidence separately. A1 or A2 is **not** A3–A6. Finite TerrainZoneDocument and existing loading-zone handoff remain independent.

## 6. Reference basis

- Frostbite *Adaptive Hardware-accelerated Terrain Tessellation* documents T-vertices and stitch permutations: https://media.contentapi.ea.com/content/dam/eacom/frostbite/files/adaptive-terrain-tessellation.pdf
- Adaptive mesh discussion and edge-split remedy: https://www.mdpi.com/2071-1050/11/7/2137
- Quadtree/continuous LOD historical reference: https://courses.cs.duke.edu/cps124/fall02/notes/12_datastructures/lod_terrain.html

These are architectural references, not imported dependencies or assertions that our engine has completed its renderer.
