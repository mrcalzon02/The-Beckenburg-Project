# WLD-12: complete stair and landing sweep checkpoint

Source-native candidate only; no playable unlock.
0/2 vertical segments ascend and 0/2 descend. 32/34 tread centres traverse.

All intermediate flat landing segments are now included in complete-link forward/reverse checks. Exact endpoint chains carry actual player feet across connected links without reinitializing at each floor. Source hashes, per-link results and failed physical component contacts: ../verification/roster-landings-2026-11-07/.

Remaining directional flight contacts:

- L16:0 up NO_FOOT_SUPPORT_0.0_0.22 - at(31.0,0.0,33.2)
- L16:0 down NO_FOOT_SUPPORT_0.0_0.22 - at(31.0,0.0,33.4)
- L17:0 up PHYSICAL_COMPONENT 105:L16 flight0 step12 at(31.0,-2.8235294818878174,32.0)
- L17:0 down NO_FOOT_SUPPORT_-0.22_0.0 - at(11.0,0.0,42.0)

Next: repair actual missing support/landing, walls/portal heights, crossing stair solids and terrain contacts from these measured poses; preserve rough allocations. Native owner/room/navigation/services integration, guards, terrain/foundation/van/story and older-map audits remain. No local editor UI or round-duration test.
