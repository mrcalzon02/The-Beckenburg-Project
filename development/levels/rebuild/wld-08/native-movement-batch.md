# WLD-08: complete stair and landing sweep checkpoint

Source-native candidate only; no playable unlock.
0/3 vertical segments ascend and 0/3 descend. 40/42 tread centres traverse.

All intermediate flat landing segments are now included in complete-link forward/reverse checks. Exact endpoint chains carry actual player feet across connected links without reinitializing at each floor. Source hashes, per-link results and failed physical component contacts: ../verification/roster-landings-2026-11-07/.

Remaining directional flight contacts:

- L17:0 up PHYSICAL_COMPONENT 13:Wall lab E at(20.273364485981308,-1.0714285373687744,33.23364485981308)
- L17:0 down NO_FOOT_SUPPORT_0.0_0.22 - at(11.0,0.0,35.0)
- L18:0 up PHYSICAL_COMPONENT 28:Wall dorm W at(43.726635514018696,-1.0714285373687744,33.23364485981308)
- L18:0 down NO_FOOT_SUPPORT_0.0_0.22 - at(53.0,0.0,35.0)
- L20:0 up PHYSICAL_COMPONENT 98:Wall tunnel-n N at(11.0,-0.5357142686843872,18.266666666666666)
- L20:0 down PHYSICAL_COMPONENT 98:Wall tunnel-n N at(11.0,-0.3571428060531616,17.733333333333334)

Next: repair actual missing support/landing, walls/portal heights, crossing stair solids and terrain contacts from these measured poses; preserve rough allocations. Native owner/room/navigation/services integration, guards, terrain/foundation/van/story and older-map audits remain. No local editor UI or round-duration test.
