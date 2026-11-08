# Saved editor terrain punching — 7 November 2026

Lighthouse, Silo and Mortuary now use actual saved editor worlds, authored through
StitchedHeightmapWorld / AdaptiveTerrainTopology / TerrainFootprintCutout.
No running local editor or editor window is opened. TerrainScenePackage publishes
its actual OBJ; TerrainSceneNativePhysics inspects and exports the same physical
triangles. The structural scene imports that exact published OBJ, checks all
bound file hashes, and removes the predecessor proposal terrain from its export.

The three worlds contain11920 saved punched leaves and28473 physical terrain
triangles. All2304 authored aperture/footprint row probes are clear optically and
for a0.1m-radius body crossing the source surface; unpunched terrain blocks both.
Source PNG bytes remain unchanged by punching. Saved topology and scene reopen
checks pass. Repeating authoring adds zero cutouts and retains source heights.
All three GLB terrain parts match their saved editor HCS2 triangle float values
and flags exactly. This is real punched topology, not a transparent overlay,
lowered ground or collision-disabled mesh.

Combined native map probe:1720/1720 actual tread centres have support AND clear
0.3–1.8m spine rays. All42 remaining terrain-overhead obstructions are removed.
Literal body queries pass938treads; current traversability queries pass880.
Full actor-radius continuous ascent/descent is still unaccepted (DEV-361). The
small-radius aperture checks above do not substitute for that movement gate.

These worlds seed their heights from the original terrain at immutable commit
de4edf80de20b86f9ac42f6a616913e109505894. The source is resampled on a2m grid
and quantized to16-bit in a64m elevation range (maximum sample rounding about
0.5mm). Non-source padding uses nearest source mesh heights, and the published
footprint is punched on the0.25m grid to follow the prior source outline/voids.
This conversion is not a claim of exact prior terrain triangle preservation.
The committed source-provenance records identify the original GLB/layout hashes.

**Terrain is not accepted.** The earlier proposal perimeter remains too broadly
rectangular; Lighthouse has visible raw cliff/water fringe artifacts. Building,
stair and shaft openings still need designed retaining walls, rims, foundations,
guards, terrain materials, real physical boundaries and canonical-van arrival
review. The three native terrain inspections and actual reimported map cutaways
were viewed. Neither the original rough footprint nor these open cuts pass the
DESQUAREIFY_GATE. No map registry unlock, production movement or LAN change.

Reproduce without the local editor (JDK17, built game JAR and Blender5):

```powershell
python tools/build_roster_editor_terrain.py --sites WLD-14 WLD-18 WLD-07-MORTUARY --seed-ref de4edf80
blender --background --factory-startup --python-exit-code 1 --python tools/build_roster_structures.py -- --remaining --replace
blender --background --factory-startup --python-exit-code 1 --python tools/export_roster_collision.py -- --remaining
python tools/audit_roster_routes.py --remaining --output development/levels/rebuild/verification/roster-editor-terrain-2026-11-07 --require-clear-spines
python tools/review_roster_structures.py
python tools/review_roster_structures.py --check
python tools/check_map_rebuild_terrain_checkout.py
```

A failed sample must return nonzero before changing any authoritative world.
The invalid-site negative verifies all27 saved world files unchanged. The earlier
failed seed-layout resolution log is retained; the sampler now resolves paths at
the immutable seed commit, and Blender uses --python-exit-code1 (DEV-365).
Native/collision/heightmap source bytes are protected by the terrain-world Git
attributes. Verify actual checkout bytes after committing, then publish/read back.

Scope remains all29 packets and follow-on unfinished maps. The terrain roster
records every packet decision; the other22new candidates still need this stage.
Next: supported source-library model placement across the roster, explicit stair
movement integration, and terrain/enclosure refinement with saved native holes.
