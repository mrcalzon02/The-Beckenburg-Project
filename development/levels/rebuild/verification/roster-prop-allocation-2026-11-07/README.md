# Existing rough-model spatial allocation - 7 November 2026

All25remaining native candidates now contain actual instances of existing staged
models:124placements from60source designs,16120prop triangles. The stage allocates
major functional footprints and model limits; it is not complete furnishing,
seat/bed capacity, finish quality, runtime integration or mission acceptance.
Castle, House, Blackpine and Hollow Creek retain their mature sources; the full
29packet roster remains explicit in furnishings/roster.json.

Examples are ward beds/treatment/hydrotherapy, school teaching/science/lockers,
church seating/altar/organ, farm stalls/hay/repair, mine ore carts/ventilation,
hydroelectric turbine/control/valves, lodge guest beds/laundry and mortuary
preparation/storage/reception. These are existing source roughs, not newly
invented substitutes. Their IDs, source files, pivots, UVs and material images
are retained. Native masters are separate; source GLBs are never rewritten.

Each source asset's measured footprint is allocated inside its authored room,
with four supported floor corners, actual floor elevation, ceiling clearance,
preserved full route widths, ritual reservations and separation from other
props. All physical crossing routes are protected, even when the room is not
one of the route's named graph endpoints. Exact polygon intersection avoids
rejecting free corners merely because a diagonal route has a wide AABB.

The native source and delivered GLB are hash-bound to the structure master,
source layout, per-site program/recipes, measured asset catalog and generator.
Each delivered GLB is independently reimported and every placed instance checked
for its transformed bounds and source UVs. Source-cache meshes are excluded.
The same actual composite triangles supply the candidate HCS2 collision export;
the three saved editor terrain parts retain exact native triangle/flag fidelity.

Before/after native movement:109506flat-route centre/side-lane samples;73058are
baseline-valid, with zero new body/support blocks.68167baseline-valid production
movement sweeps have zero new failures. These use the real1.8m/.22m actor and
resolveMove kernel in headless Java, not an editor UI or a round-duration test.
The existing36448invalid baseline samples remain failures to repair (DEV-371),
not evidence that every route is playable. All1720tread centres remain supported
and spine-clear; the existing full stair movement failures remain DEV-361.

Fit revisions were authored rather than shrinking functional clearances:
- School placement stops double-counting an actor buffer on an already clear
  route width; reserve full lateral width and a bounded longitudinal endpoint.
- Farm loading approach L22 now goes around stall A instead of through it,
  retaining the2.4m route and original barn threshold. The immutable old source
  remains in history; source revision records the original link.
- Lodge laundry ritual centre moves0.5m west within the same room, retaining
  the full4.4m ritual pad and a full-size laundry unit. Source records the old
  point; no runtime ritual registry was modified.
- Three rough instances have explicit uniform model-fit budgets: organ0.96,
  confession screen0.92, hay stack0.8. Their original meshes/pivots/UVs remain;
  placed bounds and scale limits are recorded for subsequent refinement.

All7contact sheets containing the25actual export overviews and25isolated room
cutaways were viewed. These show the initial allocation, not finished rooms.
Other rooms, capacity, model details, mounted/terrain/water assets and source
kit proposals still need subsequent passes. No power/hiding/interaction service
is inferred from a rough generator, cabinet, panel or fixture. The public map
registry, production movement and network protocol remain unchanged.

Rejected crossing-route placements and exact failing pose/sweep rows are retained.
The Blender guard initially accessed deleted native objects after a factory
reset; it now stores the structural mesh count before reset (DEV-368). DEV-369
tracks overconservative corridor/end bounds; DEV-370 the cross-room obstructions.
Terrain/roof/guard/foundation/arrival and complete collision/navigation/service/
material/story acceptance remain open for each location.

Reproduce in background, with a built game JAR, JDK17 and Blender5:

```powershell
blender --background --factory-startup --python-exit-code 1 --python tools/build_roster_furnishings.py -- --sites ASYLUM SCHOOL WLD-09
# Supply all25IDs from furnishings/room-programs.json for the full batch.
blender --background --factory-startup --python-exit-code 1 --python tools/export_roster_collision.py -- --remaining --furnished
python tools/audit_furnishing_routes.py --output development/levels/rebuild/verification/roster-prop-allocation-2026-11-07
python tools/audit_roster_routes.py --remaining --furnished --output development/levels/rebuild/verification/roster-prop-allocation-2026-11-07 --require-clear-spines
python tools/review_roster_furnishings.py
python tools/review_roster_furnishings.py --check
python tools/index_staged_map_models.py
python tools/index_staged_map_models.py --check
```

Next: repair native baseline/stair routes and bridge candidates into actual
runtime ownership/navigation/services, while continuing room capacity, mounted
models and model refinement across the roster. Preserve all originals and the
at-most40accessible search-space requirement. Source-chat pack recovery remains
pending; the existing indexed rough library is already being used.
