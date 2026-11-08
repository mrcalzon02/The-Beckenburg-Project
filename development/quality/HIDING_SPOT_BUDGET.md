# Equal hiding allowance on every investigation location

User requirement, 24 October 2026. Every location has the same maximum effective
hiding count, combining cupboards and authored outdoor refuge sockets. Decorative
shrubs, tents, toilets, scenery shadows and story props never manufacture sockets.

The shared `HidingBudget.MAXIMUM` is currently **3**, matching the existing sparse
Blackpine outdoor pool. This is one tuning constant, not a per-map override.
The contract percentage selects `floor(3 × percentage / 100)` places: 100% gives
three, default 70% gives two, and 0% gives zero, including the highest-difficulty
zero-cover setting. Indoor and outdoor hiding spend the same allowance.
Availability is deterministic, monotonic and shared by solo, host and clients
through the existing contract configuration.

All ten built investigation definitions are checked at every integer percentage.
Locations previously carrying inert `recess` placeholders now receive three actual
measured cupboard bays through the existing placement helper, with doorway/window/
furniture clearance and pocket/approach body checks. This does not unlock unfinished
maps or establish their complete navigation/art acceptance.

Every remaining map/annex packet must reserve three fit-for-purpose candidates
before gameplay acceptance. A mortuary annex shares its parent mission's allowance.
Castle headquarters has no investigation hunt and grants no extra mission cover.
Candidate placement remains an individual map audit gate.

Ordinary Hollow Creek foliage uses two new decorative shrub meshes. Three separate
native refuge placements are proposed; host sockets/fairness remain pending.
Native placement does not establish working Hollow Creek concealment.

Available hiding locations must display the shared animated sanctuary-energy material (user direction,7November2026). Visibility uses this same deterministic eligibility list, including zero at0percent. Cupboard veils occupy the open pocket; authored refuge foliage receives an enclosing energy marker. Existing hiding activation and crouch rules remain. Decorative bushes and disabled/inert sockets never gain a marker or hidden spectral exclusion. Native geometry-only overviews omit this analytic runtime compositing effect. Inspect each map's actual runtime cues individually.
