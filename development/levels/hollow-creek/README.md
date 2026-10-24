# Camp Hollow Creek: facilities and open pavilion

Native facility fit-out only; no new runtime map or mission unlock.
The WLD-02 layout, source rough scene and all 17 blockout GLBs retain their hashes.

Two original CC0 reusable models: 2.44m picnic table (1136 triangles) and cold steel
fire ring with grate, ash and wood (1760 triangles). Both have floor pivots,
packed UV materials, editable Blender sources and approved runtime asset IDs.

The additive native scene places one table and fire ring in the west campsite,
and two tables along the pavilion edges. Tent and central ritual reservations
remain clear. Independent validation reopens models and scene, checks support-part
contacts, heights, knee/toe spaces, overlaps, zones, planning connector clearances,
UVs, metal texture contrast and original source hashes. Planning reservations are
not a runtime navigation or accessibility acceptance test.

Placements/reservations: `facilities.json`. Native assembly:
`hollow-creek-facilities.blend`. Report: `../verification/hollow-creek-facilities.json`.
Captures: `../previews/hollow-creek/` and the two root model inspection PNGs.

## Completed open-pavilion iteration

`pavilion.json` records the original 2040-triangle architecture kit, source hashes,
post footprints and native placement. `hollow-creek-pavilion.blend` refines only the
pavilion placeholder in a new copy, preserving the prior native scene and all
original blockouts. A closed 120mm roof and 180mm slab have proper upper/underside
faces. Six posts, shoes, bearing beams, knee braces, trusses, purlins and rafters
have independently checked actual mesh contacts. Four apron strips retain standing
Y=0 without duplicate floors. Planning corridors, tables and ritual space stay clear.

Validation reopens the deliveries, checks all 158 authored contact pairs, ray-samples
roof thickness/normals, checks closed-edge topology and 1024 floor coverage points,
and compares previous furniture vertices/transforms exactly. See the current report
for the authoritative contact count. Runtime GLB loading independently checks 2040
triangles, metres/UVs, pivot and downward roof faces. Native captures use an explicitly
labeled inspection-only fill light; this is not a gameplay fixture/circuit test.

## Terrain-first correction and current site authority

User-directed priority: site terrain precedes further buildings/models/dressing.
`hollow_creek_ground.py` and `terrain.json` now define graded site elevations,
carved water beds and supported routes. `hollow-creek-terrain.blend` preserves
reviewed structure meshes in a new terrain-derived scene; earlier source files and
all original rough blockouts remain unchanged. The existing west table and fire ring
are fitted vertically to their local footings; the pavilion remains at Y0.

The road apron is Y1.5, lakeshore Y-1.3, and the bed reaches Y-3.1. Outdoor zones
retain rolling terrain rather than full rectangular flattening. Narrow worn treads
sit in graded shoulders with nominal 120mm incision, fading into building/crossing
landings. The shoreline and creek meander; outdoor reservation boxes are hidden
in default review images. The ground canvas is not a finished gameplay boundary.

`validate_hollow_creek_terrain.py` reopens actual meshes: 10,377 full-width route
samples, 72 pad samples, 113 connected wet-bed samples, worn-tread shoulder depth,
water beds, grade limit, unchanged source hashes and corrected crossing support.
`HollowCreekTerrainAssetTest` loads the real export within unchanged engine limits.
See the report for actual triangle count, surface values and limitations.

The existing 452-triangle pavilion-services files are **paused authoring WIP**;
their circuit previews are not accepted runtime wiring/lighting. Preserve them,
but do not advance utility/detail work ahead of the site gates.

## Next unfinished gate

Complete site/runtime terrain ownership, water restrictions, swept walking and natural edge barriers first. Then continue the existing
map model inventory with tent/platform, bathhouse, ranger facilities, dock and
bridge. Check joints, supports, tabletop placement and circulation at each iteration
against the shared defect ledger. Integrate geometry, collision, room ownership,
circuits and canonical van coordinates together before runtime reachability,
evidence/cameras, LAN, native lighting and human walkthrough acceptance.
Blackpine's remaining native/human gates stay pending independently.

## Reproduce

Blender 5 background `--python-exit-code 1 --python`:
`development/levels/build_hollow_creek_facilities.py`, then
`development/levels/validate_hollow_creek_facilities.py`.
Run `HollowCreekFacilitiesAssetTest` against packaged assets.
Refresh/check the shared gallery before publishing captures.

For the pavilion pass, run `build_hollow_creek_pavilion.py`, then
`validate_hollow_creek_pavilion.py` and `HollowCreekPavilionAssetTest`.

Terrain reproduction: run `build_hollow_creek_terrain.py`,
`validate_hollow_creek_terrain.py`, then `HollowCreekTerrainAssetTest`.
Natural edge palette recorded in terrain.json: hedges, stone retaining banks,
service fences and grounded fallen-log barriers. Their actual placement/collision
has not yet been implemented or accepted. See WATER_REQUIREMENTS.md for the
Godot/MIT candidate, separate example-texture attribution and transparency gates.


## 24 October — desquareified terrain and foliage checkpoint

[Current native review](ORGANIC_FOLIAGE_REVIEW.html) · [Detail accumulation requirements](DETAIL_PASS.md).

Original checkpoints remain unchanged. New derived terrain removes unbuilt zone-shaped pads and oversized aprons, narrows the arrival neck, and contains the actual canonical van. Actual saved-scene tests pass 10377 full-width terrain samples (worst grade 0.1928266), 1392 land/gate rays, 385 static capsule-contact crossings, 9837 protected body samples, grounded tree supports and corridor/ritual planting checks. Eight real GLBs load within unchanged per-model budgets.

Retained 60 tree placements, refined 40 conifers, added 67 supplemental camera-facing transparent crowns with solid trunks, and 150 grounded decorative ferns. Billboard textures are native renders of original branch geometry. Runtime alpha/LOD/parity/performance and overall art acceptance remain open. Decorative plants do not create hiding privileges.

All remote maps now require generator/distribution/breaker plans retaining ghost trip behavior; the plans are not implemented circuits. The lake interfaces, water-entry restrictions, runtime map integration and remaining detail queue stay unfinished. No map is unlocked.


## 24 October nightly campsite checkpoint

Two original pitched tent styles and a rolled-bedroll model are now constructed, exported and placed in a separate native scene. Two existing laid-out bedrolls are reused with original materials/UVs and terrain-fitted support. [Campsite review](CAMPSITE_REVIEW.html) in the Hollow Creek source folder links the actual native/studio captures.

Actual native checks: 10377 protected-route body samples; 3912 standing approach samples; 510 approach tread samples (maximum grade 0.1843600); 500 crouch-entry samples; 30 floor contacts and 21 bedding contacts. Both actual canvas shells measure 8mm; three real GLBs reload within unchanged budgets (8532 triangles total). These are native geometry checks, not runtime human/ghost/LAN acceptance.

Portable toilets, camp furniture/gear, collapsed/packed tent variants, park details, designated refuge shrubs, lanterns/fire states, dock shed/generator/light string and rowboat remain in the required queue. Runtime tent/circuit/AI integration and human art review remain open.
