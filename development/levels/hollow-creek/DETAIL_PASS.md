# Hollow Creek — active detail accumulation

User requests, 23 October 2026. This work follows the desquareified terrain and
actual boundary/van review. All placements below require native support/clearance
review and later runtime collision, lighting, evidence and network acceptance.
No listed requirement is model-completion credit.

| Family | Required variation / intended use | Reuse or construction gate |
|---|---|---|
| Tents | Multiple pitched sizes, opened/closed entrances and collapsed/packed variants; clustered natural clearings | Purpose-built canvas/poles/pegs, supported interiors and unobstructed entrances; no zone-sized platforms |
| Camping gear | Packs, rolls, folding furniture, cookware, water carriers and restrained personal clutter | Audit existing models; support each object on ground or a measured table/seat |
| Bedrolls | Rolled and laid-out variants appropriate to tent interiors | Existing `level_trace_bedroll` is a candidate, not an automatically accepted fit |
| Portable toilets | Complete outdoor cubicles, doors, ventilation and actual toilet fixtures | Ordinary `toilet_mp_1` alone is not a portable toilet; reserve a level, serviced spot near bathhouse without blocking doors |
| Shrubbery | Dense hiding pockets plus general bushes and woody boundary growth | Reuse/audit `level_shrub_refuge`; only explicitly authored host-recognized refuge pockets grant hiding |
| Hedges | Managed park clusters, irregular woodland transition and screened service areas | Fit to terrain; preserve circulation, visibility and ritual clear squares |
| Park details | Birdbath, modest weathered park statue, associated plinths and planting | Appropriate human scale and stable grounded bases; do not turn them into compulsory ritual anchors |
| Hanging camp lights | Oil lanterns with actual hooks/arms/chains on supported posts or structures | Native fixture first; warm fire-light source, visibility/occlusion and powered/fuel rules remain separate runtime gates |
| Campfires | Existing fire-ring family plus believable logs/embers and cold/burning variants | Reuse reviewed ring; no fire beneath canvas, bushes or a roof; host-owned light/evidence behavior |
| Dock shed | Small boat-service/generator shed at the dock end | Provisional 2.0 × 2.4 m footprint on the actual dock; engineered load/support, solid roof/joins, ventilated service fittings and door swing |
| Generator | Compact scenery generator in the dock service shed | Audit existing generator family before making another; actual mounting, service clearance, vent and cable support |
| Dock light string | Warm lamps along the dock from shed to shore | Real supported posts, fittings and cables; lowest hanging hardware above the reviewed walking clearance; switch/circuit mapping required |
| Rowboat | A small moored rowboat beside the dock | Actual hull, seats, gunwale, oars and mooring support; waterline fit and dock access clear. No new sailing system assumed |

Provisional dock arrangement: preserve the central access route on the existing
5 m wide deck. Fit the small shed toward one end/side, not across the whole deck;
prove a useful clear route and door approach in the final placement. A candidate
rowboat mooring lies west of the dock, separate from its footing posts. These are
placement intentions, not already accepted coordinates or fixtures.

Construction order: terrain/desquareify → grounded boundaries and scenery →
clearings and major tent/sanitary facilities → grounded furnishings/park details →
dock structure and utilities → supported clutter/wear → actual lighting/circuit,
movement, evidence/camera, host/LAN and human review. Revisit earlier fitness gates
whenever another model changes a route, support, silhouette or lighting join.


## 24 October nightly campsite checkpoint

Two original pitched tent styles and a rolled-bedroll model are now constructed, exported and placed in a separate native scene. Two existing laid-out bedrolls are reused with original materials/UVs and terrain-fitted support. [Campsite review](CAMPSITE_REVIEW.html) in the Hollow Creek source folder links the actual native/studio captures.

Actual native checks: 10377 protected-route body samples; 3912 standing approach samples; 510 approach tread samples (maximum grade 0.1843600); 500 crouch-entry samples; 30 floor contacts and 21 bedding contacts. Both actual canvas shells measure 8mm; three real GLBs reload within unchanged budgets (8532 triangles total). These are native geometry checks, not runtime human/ghost/LAN acceptance.

Portable toilets, camp furniture/gear, collapsed/packed tent variants, park details, designated refuge shrubs, lanterns/fire states, dock shed/generator/light string and rowboat remain in the required queue. Runtime tent/circuit/AI integration and human art review remain open.
