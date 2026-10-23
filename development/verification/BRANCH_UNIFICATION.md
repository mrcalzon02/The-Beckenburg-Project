# Completed map branches unified with optimized main — 23 October 2026

Integrated main `db9bb0d` with completed map/model branch `6ac4214` in a clean
checkout. The map branch already contains `b2ce1cb` (circular headrig/metal refit)
and the earlier local-main map/lore milestones; none of those commits is replaced
or rebased. No force push is used.

Four files required semantic conflict resolution:

- Castle map: retain editable scene furniture, architecture and door behavior;
  keep the Common Archive resident's actual collision and station.
- Castle panel: preserve editor-authored spawn, physical quartermaster and its
  input pause; keep archive reading and pointer-capture suppression while open.
- Solo view: retain render diagnostics alongside graded-ground recovery.
- Renderer: retain main's OpenGL batching, asynchronous frame identity, frustum
  rejection, frame timing, weather and signature/Twins privacy. Map scenery
  templates enter that same geometry pipeline once, with shared source meshes
  and conservative instance rejection. Do not draw a second CPU scenery pass
  over a GPU framebuffer. Retain terrain and map-specific van document surfaces.

Validation found a real merged API mismatch: generated foliage/terrain textures
registered pixels and diffuse factors without the newer per-material UV-origin
metadata. Both registration paths now extend all required metadata arrays. The
new `MergedMaterialMetadataTest` covers those slots, a subsequent imported GLB
and television material. The live-editor model index includes the merged camp,
story and working-place asset registries.

The complete Java 17 application and test sources compile. Focused regression
results are in [branch-unification.json](branch-unification.json). The exact
unmodified-main source/test reproduction in
[unification-main-baseline.json](unification-main-baseline.json) fails the same
castle sky-variation assertion as the merged tree. It is recorded as an existing
main failure, not marked green or suppressed. Actual OpenGL hardware acceptance,
full release packaging and target-machine performance are not claimed here.

Seventeen unbuilt-map blockouts and all twenty-three model requirement packets
remain intact. Existing accepted scene geometry is not regenerated. Current-main
lighting occlusion and physical-light regression checks pass after the merge.

The unfinished mission-board UI in the separate `The-Beckenburg-Mission-Board`
checkout and the paused, uncommitted local lighting edits in the original
checkout are preserved and excluded. They must be rebased onto this unified
main before feature work resumes. No further mission-board or map feature work
was performed after the user's unification instruction.
