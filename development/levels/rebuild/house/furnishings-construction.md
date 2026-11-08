# Daily household and garden functions

Actual four-direction review found the vestibule, cold-food storage and garden
functions unfinished. `HouseRebuildFurnishings` adds six fitted source props: the
original full-size boot/towel rack, source-preserving supported coat rail, original
180triangle fridge, original garden bench and two small source planters. All45
static props are recorded in `asset-use.csv` with real source hashes and placement.

The rack leaves the entrance, sitting-room and stair doors clear. The coat peg rail
touches the actual finished east wall; its added hanger closes the source gap.
The fridge occupies dedicated south-wall storage, facing north into the kitchen.
Its authored centre is X14.9/Z14.1925 with yawPI and a6cm rear ventilation gap.
The short wall between garden and dining doors stays clear; both doorway centres
are more than2m away. The counter, cellar stair and sampled door swings stay clear. It introduces no unrelated
food/thermal simulation. Existing source/breaker and lighting behavior stays intact.

The bench uses a measured nearly level patch of the unchanged editor-authored
garden terrain, outside the worn walking route. Sixteen foot samples differ by
less than1mm; planter footprints also contact actual terrain. No zone-shaped ground
pad or terrain flattening was added, and decorative planting creates no hiding
socket. The garden's construction label still says reservation; production labels
are cleaned during promotion, without treating a bench as a paved patio.

Packaged checks retain all eight authored loose belongings, hiding candidates,
stairs/voids/van/portal coordinates and50164required route positions. Door leaves
are checked at21sampled poses against added furnishing bounds. The actual furnished
candidate passes real4peer source/breaker control and solo/human ghost trip/reset/
resync. The original coat geometry/UV/material subset is retained; fridge atlas
sampling is exact. All six native texture variants pass; current140materials/
13733203texels match actual runtime sampling.

Native131793triangles/UVs,230packed images,45props,15spawns and original socket
transforms reopen unchanged. Four purpose-specific runtime views and the native
overview were inspected. The rejected fridge capture preserves an incorrect
camera-room hint that selected the cellar; current cameras require actual room
ownership as well as physical support. Full361/native/GPU remote acceptance,
matched closed-door/circuit and final wear review, authored encounter, saved-native
migration, live promotion and complete House/client/mission/hardware gates remain.


Owner correction DEV-334: the old north-east position between doors is rejected,
even though its sampled collision checks passed. Retain that actual rejected
image. Current south-wall source/native revision is
first-principles-functional-furnishings-v1-981935462ed806a0.
The131793triangle/230image native file independently reopens with every source
vertex/UV and socket; original180triangle fridge model/atlas are unchanged.
Doorway-clear, new-location and native-editor runtime views are separate from
the historical whole-room diagnostic. Full362/editor/GPU and whole-map gates
remain pending for the current combined source.
