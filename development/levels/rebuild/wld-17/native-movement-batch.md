# WLD-17: complete stair and landing sweep checkpoint

Source-native candidate only; no playable unlock.
3/5 vertical segments ascend and 3/5 descend. 85/87 tread centres traverse.

All intermediate flat landing segments are now included in complete-link forward/reverse checks. Exact endpoint chains carry actual player feet across connected links without reinitializing at each floor. Source hashes, per-link results and failed physical component contacts: ../verification/roster-landings-2026-11-07/.

Remaining directional flight contacts:

- route-11:1 up PHYSICAL_COMPONENT 0:Authored battery terrain at(20.0,0.17391304671764374,16.0)
- route-11:1 down NO_FOOT_SUPPORT_0.22_0.0 - at(12.55,4.0,16.0)
- route-12:1 up PHYSICAL_COMPONENT 0:Authored battery terrain at(56.0,0.17391304671764374,16.0)
- route-12:1 down NO_FOOT_SUPPORT_0.22_0.0 - at(48.53333333333333,4.0,16.0)

Next: repair actual missing support/landing, walls/portal heights, crossing stair solids and terrain contacts from these measured poses; preserve rough allocations. Native owner/room/navigation/services integration, guards, terrain/foundation/van/story and older-map audits remain. No local editor UI or round-duration test.
