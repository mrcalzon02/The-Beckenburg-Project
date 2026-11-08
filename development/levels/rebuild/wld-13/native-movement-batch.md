# WLD-13: complete stair and landing sweep checkpoint

Source-native candidate only; no playable unlock.
0/1 vertical segments ascend and 0/1 descend. 16/17 tread centres traverse.

All intermediate flat landing segments are now included in complete-link forward/reverse checks. Exact endpoint chains carry actual player feet across connected links without reinitializing at each floor. Source hashes, per-link results and failed physical component contacts: ../verification/roster-landings-2026-11-07/.

Remaining directional flight contacts:

- L19:0 up PHYSICAL_COMPONENT 93:Wall gallery E at(30.266666666666666,1.941176414489746,41.0)
- L19:0 down PHYSICAL_COMPONENT 93:Wall gallery E at(29.733333333333334,2.117647171020508,41.0)

Next: repair actual missing support/landing, walls/portal heights, crossing stair solids and terrain contacts from these measured poses; preserve rough allocations. Native owner/room/navigation/services integration, guards, terrain/foundation/van/story and older-map audits remain. No local editor UI or round-duration test.
