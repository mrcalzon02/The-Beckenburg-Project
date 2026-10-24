# Water rendering and license intake

User direction, 23 October 2026: use layered transparency and appropriate water
rendering for the authored lake and creek. Water must follow the terrain-first
site contract, not conceal unsupported paths, dams or unresolved bank seams.

Candidate: https://immaculate-lift-studio.itch.io/psx-style-water-surface
Author: Immaculate Lift Studio. Primary page inspected 23 October 2026.
The page identifies the shader as MIT and the package as a Godot 4 project.
It lists two moving texture layers, vertex waves, pixelation, opacity and edge foam.
The author's page comment distinguishes the example textures as CC Attribution.
The actual archive/license text and texture attribution have not yet been acquired
or validated; do not relabel its contents CC0 or claim the Godot shader is installed.

## Required adaptation and acceptance

- Two independently moving, restrained pixel-style surface layers.
- Depth-dependent shallow/deep colour and opacity; visible submerged bed/objects.
- Correct opaque/translucent pass ordering, depth test and sensible depth writes.
- Shore/obstacle foam derived from real depth/intersection; no oversized white rims.
- Modest waves with stable surface normals, no foam/texture swimming or black seams.
- Viewed above/below and across overlap cases: banks, bridge supports, dock,
  submerged props, other translucent surfaces, flashlight/UV and camera photographs.
- CPU/native GPU render paths reviewed independently; performance measured.
- Water/shore exclusion and ghost/player collision remain terrain/gameplay contracts.
- Retain full MIT notice with adapted shader code. Verify each third-party texture's
  exact attribution requirements or use original project textures instead.

Current water in hollow-creek-terrain.blend is an opaque native foundation preview,
not a completed transparency shader. Terrain runtime integration and grade checks
precede promoting water or additional facility detail.
