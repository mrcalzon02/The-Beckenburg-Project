# WLD-03: complete stair and landing sweep checkpoint

Source-native candidate only; no playable unlock.
1/3 vertical segments ascend and 1/3 descend. 68/69 tread centres traverse.

All intermediate flat landing segments are now included in complete-link forward/reverse checks. Exact endpoint chains carry actual player feet across connected links without reinitializing at each floor. Source hashes, per-link results and failed physical component contacts: ../verification/roster-landings-2026-11-07/.

Remaining directional flight contacts:

- L04:0 up PHYSICAL_COMPONENT 6:Wall shelter N at(12.0,1.39130437374115,46.25)
- L04:0 down PHYSICAL_COMPONENT 6:Wall shelter N at(12.0,1.39130437374115,45.75)
- L07:0 up NO_FOOT_SUPPORT_-0.22_0.0 - at(12.03859649122807,4.17391300201416,33.968421052631584)
- L07:0 down NO_FOOT_SUPPORT_0.22_0.0 - at(34.0,8.0,16.0)

Next: repair actual missing support/landing, walls/portal heights, crossing stair solids and terrain contacts from these measured poses; preserve rough allocations. Native owner/room/navigation/services integration, guards, terrain/foundation/van/story and older-map audits remain. No local editor UI or round-duration test.
