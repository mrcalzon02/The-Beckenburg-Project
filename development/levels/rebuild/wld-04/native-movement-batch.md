# WLD-04: complete stair and landing sweep checkpoint

Source-native candidate only; no playable unlock.
2/9 vertical segments ascend and 2/9 descend. 149/153 tread centres traverse.

All intermediate flat landing segments are now included in complete-link forward/reverse checks. Exact endpoint chains carry actual player feet across connected links without reinitializing at each floor. Source hashes, per-link results and failed physical component contacts: ../verification/roster-landings-2026-11-07/.

Remaining directional flight contacts:

- L03:0 up NO_FOOT_SUPPORT_-0.22_0.0 - at(14.0,0.1764705777168274,51.0)
- L03:0 down NO_FOOT_SUPPORT_0.22_0.0 - at(27.966019417475728,3.0,36.036407766990294)
- L04:0 up NO_FOOT_SUPPORT_0.0_0.22 - at(28.028169014084508,3.1764705181121826,35.80281690140845)
- L04:0 down NO_FOOT_SUPPORT_0.0_-0.22 - at(30.0,6.0,22.0)
- L06:0 up PHYSICAL_COMPONENT 5:Wall hut S at(18.273127753303967,4.941176414489746,26.273127753303967)
- L06:0 down NO_FOOT_SUPPORT_-0.22_0.0 - at(12.0,6.0,20.0)
- L09:0 up NO_FOOT_SUPPORT_0.0_0.22 - at(43.02469135802469,3.1764705181121826,50.80246913580247)
- L09:0 down NO_FOOT_SUPPORT_0.22_0.0 - at(45.0,6.0,35.0)
- L10:0 up NO_FOOT_SUPPORT_-0.22_0.0 - at(28.0,0.1764705777168274,66.0)
- L10:0 down NO_FOOT_SUPPORT_0.22_0.0 - at(42.96478873239437,3.0,51.03521126760563)
- L11:0 up NO_FOOT_SUPPORT_0.0_-0.22 - at(30.08176100628931,6.0,22.18238993710692)
- L11:0 down NO_FOOT_SUPPORT_0.0_-0.22 - at(30.0,6.0,22.0)
- L13:0 up NO_FOOT_SUPPORT_0.0_0.22 - at(26.0,6.180000305175781,12.2)
- L13:0 down NO_FOOT_SUPPORT_0.0_0.22 - at(26.0,6.180000305175781,12.0)

Next: repair actual missing support/landing, walls/portal heights, crossing stair solids and terrain contacts from these measured poses; preserve rough allocations. Native owner/room/navigation/services integration, guards, terrain/foundation/van/story and older-map audits remain. No local editor UI or round-duration test.
