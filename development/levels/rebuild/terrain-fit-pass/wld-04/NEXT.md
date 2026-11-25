# WLD-04: terrain bearing construction

28new closed parts /336triangles: {'skirt': 28}. Actual source terrain determines embedded bottoms; source deck undersides determine tops and owning receivers. Perimeter masonry uses the union of exact lowest floor rectangles, never internal room partitions. Timber piles and short explicit bearing beams support actual raised decks/routes; the moored vessel hull is excluded from land foundations.

Source: `development/levels/rebuild/terrain-overall-pass/assemblies/wld-04/terrain-assembly.glb`. Original native collision bytes, terrain, source meshes/UVs/image bindings and world poses retained. Source model masters and current map masters remain unchanged. Actual reimported GLB/native parity and independent contact/closed-solid checks pass. These are native construction candidates, not map acceptance.

Flat before/after comparison: 1413samples /853baseline-valid poses /750qualified sweeps; 0new blocks /0new sweep failures. Whole flight/path comparisons have no new bearing regressions. Existing terrain/stair failures remain open.

Unfinished contacts: {'RESERVE_LOW_ROUTE_L06': 9, 'NO_ACTUAL_DECK_UNDERSIDE': 23, 'NO_STRUCTURAL_FLOOR_BEARING': 1, 'EARTH_OVERTOPS_WALKING_FLOOR': 18, 'RESERVE_LOW_ROUTE_L11': 10, 'RESERVE_LOW_ROUTE_L09': 2}. These are withheld construction decisions, not invented ground or silent passes. Build missing real deck meshes, foundation/retaining/rock transitions and low-route support headers; revise the terrain/building relationship where earth overtops existing floors. Preserve actual saved basement punches and the40-space envelope. Complete canonical van arrival, physical containment, water, services/story and native runtime/network/hardware acceptance.
