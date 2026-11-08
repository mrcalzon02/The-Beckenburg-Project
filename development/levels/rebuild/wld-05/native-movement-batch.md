# WLD-05: complete stair and landing sweep checkpoint

Source-native candidate only; no playable unlock.
0/4 vertical segments ascend and 0/4 descend. 109/114 tread centres traverse.

All intermediate flat landing segments are now included in complete-link forward/reverse checks. Exact endpoint chains carry actual player feet across connected links without reinitializing at each floor. Source hashes, per-link results and failed physical component contacts: ../verification/roster-landings-2026-11-07/.

Remaining directional flight contacts:

- L09:0 up NO_FOOT_SUPPORT_0.22_0.0 - at(30.0,0.0,52.0)
- L09:0 down NO_FOOT_SUPPORT_0.22_0.0 - at(30.0,0.0,52.0)
- L15:0 up NO_FOOT_SUPPORT_0.0_0.22 - at(11.0,-6.0,34.3)
- L15:0 down NO_FOOT_SUPPORT_0.0_0.22 - at(11.0,-6.0,34.5)
- L16:0 up PHYSICAL_COMPONENT 177:L15 flight0 step11 at(11.0,-9.82608699798584,31.0)
- L16:0 down NO_FOOT_SUPPORT_0.0_-0.22 - at(11.0,-5.823529243469238,51.2)
- L17:0 up PHYSICAL_COMPONENT 102:Wall vent E at(19.733333333333334,-3.1764707565307617,52.0)
- L17:0 down NO_FOOT_SUPPORT_0.22_0.0 - at(30.0,0.0,52.0)

Next: repair actual missing support/landing, walls/portal heights, crossing stair solids and terrain contacts from these measured poses; preserve rough allocations. Native owner/room/navigation/services integration, guards, terrain/foundation/van/story and older-map audits remain. No local editor UI or round-duration test.
