# Separated stair and doorway revision — 7 November 2026

The previous probe found90 obstructed head-height tread-centre rays. This actual
five-site geometry revision removes48 of those obstructions. All1720 treads retain
native support;1678 now have clear spine rays. Every remaining42 overhead hit is
an identified terrain triangle, across Lighthouse, Silo and Mortuary. Those sites
require the actual persisted editor punch operation before terrain acceptance.
No terrain triangles were hidden or their collision flags weakened in this pass.

College: separate basement and upper-storey flight lanes with a ground landing.
Mansion: alternate the cellar/upper/attic flight lanes and connect their landings.
Hydroelectric site: separate the lower gallery approach and east return flights;
close the gallery storey gap. The east stair enclosure/foundations remain rough.
Lighthouse: remove folded-back level approaches over flights, close storey gaps.
Silo: move the crew stair away from the upper flight, close the underground storey
gap, and finish the exhibit descent outside its wall before entering at floor Y.

All386 treads in College, Mansion and Hydroelectric have support and clear spine
rays. Independent GLB reimport, measured depth/closed solids, generator and original
scene hash checks pass across25 packets. Five updated contact sheets were viewed;
rough caps, terrain, supports and site character remain unfinished. The same GLB
triangles supply candidate HCS2; source hashes and every point result are retained.

Literal body queries pass895 tread centres, and current traversability queries
pass837. These results still block continuous gameplay acceptance. Point probes,
including zero samples in WLD-16, cannot prove a complete route. Map registry,
production movement, networking and the running local editor were not changed.

Next: use the editor terrain topology punch APIs offline to author real saved
basement holes, export matching terrain/collision, then continue supported source
library placements across the roster and resolve explicit stair movement sweeps.
No round-duration tests. Original layouts/scenes and earlier rejected evidence
remain in version history and the previous route checkpoint.
