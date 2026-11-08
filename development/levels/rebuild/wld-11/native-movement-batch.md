# WLD-11: complete stair and landing sweep checkpoint

Source-native candidate only; no playable unlock.
0/1 vertical segments ascend and 0/1 descend. 7/7 tread centres traverse.

All intermediate flat landing segments are now included in complete-link forward/reverse checks. Exact endpoint chains carry actual player feet across connected links without reinitializing at each floor. Source hashes, per-link results and failed physical component contacts: ../verification/roster-landings-2026-11-07/.

Remaining directional flight contacts:

- L02:0 up NO_FOOT_SUPPORT_-0.22_0.0 - at(32.0,0.17142856121063232,69.0)
- L02:0 down NO_FOOT_SUPPORT_0.22_0.0 - at(40.0,1.2000000476837158,59.0)

Next: repair actual missing support/landing, walls/portal heights, crossing stair solids and terrain contacts from these measured poses; preserve rough allocations. Native owner/room/navigation/services integration, guards, terrain/foundation/van/story and older-map audits remain. No local editor UI or round-duration test.
