# Textured wall-light bracket

Owner correction: fix both the oversized shape and the untextured backing plate. The old SceneRenderer primitive was a flat brown 30x40x5.2cm box regardless of lamp scale. Replace it with a compact steel strap, scaled with the original source fixture and located at its rear upper lip.

All six sides now sample the original off-state lamp atlas gray weathered-metal region (U0.08..0.92, V0.60..0.95). They use actual UV-textured renderer faces. No flat-color substitution, fluorescent strip sampling, mesh/image replacement or change to lamp strength. The same source finish remains in both power states.

The strap is76cm wide and12cm high at source scale, with24mm thickness; at the original72% scale it is54.7x8.6cm. Its inner1mm seats into the masonry and its front overlaps the fixture's18mm clearance, verified against clipped actual source triangles. Source GLB geometry, UVs, PNGs and pivots remain unchanged.

WallLampMountPlateTest inspects the actual renderer faces and UV region, source-texture variation, scaled dimensions, masonry contact and clipped source-triangle contact for all27 current Castle wall fixtures. The current local scene includes the unpublished upper revision; this count is not represented as the number in the prior published baseline. CastleLightingPlacementRepairTest retains fixture vertex clearance, emitters, lighting, migration and support checks. Both pass.

Actual SceneRenderer.drawCastle captures, five angles with the real room circuit on/off, are in development/captures/wall-lamp-runtime/textured-bracket. The first three match the prior atlas-repair cameras; the last two inspect the bracket from oblique angles. The large tan rectangle no longer protrudes above/below the housing. Reviewed source hashes remain in ../wall-lamp-uv-2026-11-03/source-sha256.json.

Scope: shared Castle wall-light renderer/mount only. Unfinished upper-floor source remains preserved separately. No full-map or physical GPU acceptance is claimed.
