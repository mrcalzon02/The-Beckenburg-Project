# Broad terrain foundation pass — 23 October 2026

User-directed step after the existing crude layouts: original landforms and elevation
foundations for all **16 remaining rough sites**. Previously developed maps and all
source layout/blockout files are preserved. No existing map has been rebuilt or unlocked.

Each folder contains `contract.json`, an editable `site-terrain.blend`, independent
`verification.json`, and links to its original terrain GLB/editable asset. Building
floor plates are source foundation reservations; gray plates are not completed buildings.
Layer-study captures preserve underground/tower datums without pretending that
a single-valued terrain surface can replace stacked interiors.

## Measured checks

- Java 17 + actual game decoder: 16 GLBs, 157,440 triangles total; existing per-asset budgets unchanged.
- Actual native triangle rays: 9,107 deterministic ground samples.
- 552 original floor-part meshes/transforms preserved exactly, including split floor pieces.
- 87 prototype pile footings independently checked against baked ground.
- Original source hashes, metre origins, finite geometry, UVs and upward faces checked.

Route diagnostics examined 15,552 locations and retained 937 sampled grade/support flags.
These are refinement flags, not unique defect counts or a passing navigation result.
Every flagged position and every unengineered transition is in the per-site report.
Canyon and railway are the next priorities for fine grading/crossing/rock-boundary work.

## Terrain roster

| Location | Landform | Ground range (m) | Transition reservations | Route sample flags |
|---|---|---:|---:|---:|
| [St. Alban Academy](academy/contract.json) | Cloister ridge and drained quadrangle | -0.62 to 2.22 | 2 | 9 |
| [St. Dymphna Hospital](asylum/contract.json) | Stepped hospital garden slopes | -1.32 to 1.30 | 2 | 8 |
| [Beckenburg College](college/contract.json) | Campus green and service-tunnel cover | -0.56 to 2.35 | 4 | 65 |
| [Blackthorn Manor](mansion/contract.json) | Manor terrace and descending service garden | -0.57 to 2.69 | 4 | 31 |
| [Beckenburg Penitentiary](prison/contract.json) | Excavated yard and perimeter earthwork | 0.00 to 4.24 | 3 | 49 |
| [Red Mesa Ancestral Caves](wld-03/contract.json) | Red Mesa canyon and 4/8m cliff terraces | 0.00 to 13.58 | 3 | 287 |
| [Widow's Peak Fire Lookout](wld-04/contract.json) | Lookout hillside and contour return | -0.44 to 7.80 | 8 | 24 |
| [Dead Man's Claim](wld-05/contract.json) | Mine ridge with layered adit/drift levels | -1.64 to 8.08 | 4 | 58 |
| [Drowned Fisherman's Wharf](wld-06/contract.json) | Tidal shore and piled wharf foundations | -4.10 to 0.72 | 2 | 62 |
| [Saint Bartholomew's Forgotten Cemetery](wld-07/contract.json) | Rolling burial ground and ossuary cover | -0.64 to 2.04 | 2 | 7 |
| [Northwatch Research Station](wld-08/contract.json) | Snow shelf, wind drifts and tunnel cover | 0.00 to 1.93 | 3 | 45 |
| [Blackwater Hydroelectric Dam](wld-09/contract.json) | Dam abutment, six-metre cut and tailrace | -9.20 to 1.20 | 6 | 20 |
| [Monastery of Ashen Bells](wld-10/contract.json) | Monastery high garden and service banks | -0.58 to 3.27 | 3 | 52 |
| [Mourning Marsh](wld-11/contract.json) | Marsh basin and raised boardwalk foundations | -1.10 to 0.26 | 1 | 12 |
| [Gallows Pass Railway](wld-12/contract.json) | Rail cutting, trestle ravine and tunnel cover | -5.25 to 3.45 | 2 | 203 |
| [Greywater Cannery](wld-13/contract.json) | Cannery tidal bank and forest-side slope | -2.00 to 1.37 | 1 | 5 |

## Reproduce and continue

Run Blender 5 background with `--python-exit-code 1 --python`:
`development/levels/build_terrain_foundations.py -- <keys>`, then
`refresh_terrain_foundation_views.py -- <keys>` and `validate_terrain_foundations.py -- <keys>`.
Run `TerrainFoundationAssetsTest` against the actual packaged resources.
Refresh and verify the shared gallery at each published checkpoint.

[Review the native captures](REVIEW.html). These captures use the saved meshes;
they are authoring overviews/studies, not screenshots of completed playable missions.

Next per-site gates: fit fine grades and supported transitions; carve underground
volumes/headroom; add necessary retaining/guard and natural boundary models; integrate
terrain/floors/water/room ownership/collision/van together; then actual play, LAN,
light-separation and performance checks. The larger all-map completion goal remains open.
