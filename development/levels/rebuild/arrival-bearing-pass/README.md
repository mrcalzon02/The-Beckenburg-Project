# Arrival bearing construction checkpoint

Twenty-one current map sources now have a physically attached path-end extension behind their unchanged arrival anchor. Each extension is .35 m long, uses the original full path width and measured12 cm depth/material, and joins the actual terminal path support. This is252 new triangles in21 closed parts. Existing GLB resource bytes/nodes, UVs, pivots, native records and saved terrain/basement punches remain exact. No reusable props are replaced.

All29 packets have explicit decisions in roster.json. Its active_sources entries identify the direct next source for25 candidates. Castle, House, Blackpine and Hollow Creek remain mature sources with individual acceptance gates. Three already-supported rough arrivals retain their sources. WLD-09 retains the prior flat source: its slab-only study is rejected because actual saved terrain intersects the original arrival body.

Actual scoped verification:

- All21 original arrival anchors now pass full production standing footprints.
- Complete fully-flat links improve380 to389 of566, with no previously passing directional regression.177 links remain incomplete.
- Exactly119,775 unchanged current centre/side samples recover63 full-foot positions, with zero newly invalid positions;13,420 remain invalid. The separate historical110,409 body-only samples retain102,726 valid positions, with no new body or qualified-sweep blocks. These are different sample sets/criteria.
- All221 flights and84 complete vertical links pass both directions. Both authored cemetery14 segmentloops remain passing. No kernel tolerance changed.
- Actual exports/native solids/packed editable sources independently inspected. Original binary/node/resource prefixes and native records are retained exactly.42 final paired native frames and six final sheets inspected; the rejected terrain-covered WLD-09 frames are retained. Construction3/5, finish1/5. No canonical van arrival, overall foundation/terrain, material finish, optical, runtime, gameplay, networking or hardware acceptance inferred.

The earlier34 link route-only study found no full-width reroute through its bounded native space while preserving endpoints, required thresholds and sealed context. No candidate was adopted; exact inputs/decisions remain in ../route-design-pass. It led to this actual construction repair, rather than relaxed movement rules.

Reproduce with background Blender tools/build_arrival_bearings.py, tools/review_arrival_bearings.py; Python tools/audit_current_flat_routes.py --arrival-pass, tools/audit_roster_stairs.py --arrival-pass --require-authored-chains --output development/levels/rebuild/arrival-bearing-pass/verification, and tools/audit_macroterrain_fit.py --arrival-pass. Check decoded raw evidence with python tools/archive_flat_verification.py --root development/levels/rebuild/arrival-bearing-pass --check. Large tables are losslessly gzip archived; hashes bind decoded original bytes.

No local editor UI, mission-duration test or runtime unlock. Shared heavy fog/weather sky remains unchanged. Gallery captures are committed on the rebuild branch; deployment is reported independently after actual remote observation.

Published implementation: 414d8df61549685b84e6746ef62eee8fae1c8147. Exact source delivery and observed pending gallery deployment are recorded in verification/publication-readback.json.
