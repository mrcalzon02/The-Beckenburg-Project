# Camp Hollow Creek: first facility refinement

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

## Next unfinished gate

Refine the provisional pavilion into an actual open shelter. Continue the existing
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
