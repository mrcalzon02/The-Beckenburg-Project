# WLD-18: complete stair and landing sweep checkpoint

Source-native candidate only; no playable unlock.
1/6 vertical segments ascend and 1/6 descend. 113/116 tread centres traverse.

All intermediate flat landing segments are now included in complete-link forward/reverse checks. Exact endpoint chains carry actual player feet across connected links without reinitializing at each floor. Source hashes, per-link results and failed physical component contacts: ../verification/roster-landings-2026-11-07/.

Remaining directional flight contacts:

- route-07:1 up PHYSICAL_COMPONENT 62:Wall west S.001 at(18.0,-2.4347827434539795,37.75)
- route-07:1 down PHYSICAL_COMPONENT 62:Wall west S.001 at(18.0,-2.4347827434539795,38.25)
- route-08:1 up PHYSICAL_COMPONENT 41:Wall exit N at(6.0,-1.7391303777694702,41.8)
- route-08:1 down PHYSICAL_COMPONENT 41:Wall exit N at(6.0,-1.56521737575531,42.25)
- route-13:1 up PHYSICAL_COMPONENT 223:route-13 landing0 at(52.0,-0.3478260040283203,47.06666666666667)
- route-13:1 down NO_FOOT_SUPPORT_0.0_-0.22 - at(52.0,0.0,47.46666666666667)
- route-14:1 up PHYSICAL_COMPONENT 95:Floor exhibit.016 at(54.0,-5.8333330154418945,34.0)
- route-14:1 down PHYSICAL_COMPONENT 128:Ceiling cap east.016 at(54.0,-5.8333330154418945,33.8)
- route-14:3 up PHYSICAL_COMPONENT 118:Ceiling cap east.006 at(44.0,-6.0,33.8)
- route-14:3 down PHYSICAL_COMPONENT 118:Ceiling cap east.006 at(44.0,-6.0,34.0)

Next: repair actual missing support/landing, walls/portal heights, crossing stair solids and terrain contacts from these measured poses; preserve rough allocations. Native owner/room/navigation/services integration, guards, terrain/foundation/van/story and older-map audits remain. No local editor UI or round-duration test.
