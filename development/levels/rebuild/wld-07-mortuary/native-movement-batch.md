# WLD-07-MORTUARY: complete stair and landing sweep checkpoint

Source-native candidate only; no playable unlock.
0/2 vertical segments ascend and 0/2 descend. 34/34 tread centres traverse.

All intermediate flat landing segments are now included in complete-link forward/reverse checks. Exact endpoint chains carry actual player feet across connected links without reinitializing at each floor. Source hashes, per-link results and failed physical component contacts: ../verification/roster-landings-2026-11-07/.

Remaining directional flight contacts:

- L14:0 up PHYSICAL_COMPONENT 41:Wall west-stair E at(16.266666666666666,-0.8823530673980713,16.0)
- L14:0 down PHYSICAL_COMPONENT 41:Wall west-stair E at(15.733333333333333,-0.7058824300765991,16.0)
- L15:0 up PHYSICAL_COMPONENT 52:Wall east-stair W at(39.75,-0.8823530673980713,16.0)
- L15:0 down PHYSICAL_COMPONENT 52:Wall east-stair W at(40.25,-0.7058824300765991,16.0)

Next: repair actual missing support/landing, walls/portal heights, crossing stair solids and terrain contacts from these measured poses; preserve rough allocations. Native owner/room/navigation/services integration, guards, terrain/foundation/van/story and older-map audits remain. No local editor UI or round-duration test.
