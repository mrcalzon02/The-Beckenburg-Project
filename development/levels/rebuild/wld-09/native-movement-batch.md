# WLD-09: complete stair and landing sweep checkpoint

Source-native candidate only; no playable unlock.
1/7 vertical segments ascend and 1/7 descend. 204/215 tread centres traverse.

All intermediate flat landing segments are now included in complete-link forward/reverse checks. Exact endpoint chains carry actual player feet across connected links without reinitializing at each floor. Source hashes, per-link results and failed physical component contacts: ../verification/roster-landings-2026-11-07/.

Remaining directional flight contacts:

- L07:0 up PHYSICAL_COMPONENT 28:Wall west-stair E at(36.25,-1.5882353782653809,35.0)
- L07:0 down PHYSICAL_COMPONENT 28:Wall west-stair E at(35.75,-1.4117646217346191,35.0)
- L09:0 up PHYSICAL_COMPONENT 58:Wall turbine S.001 at(53.41121495327103,-3.882352828979492,48.271028037383175)
- L09:0 down PHYSICAL_COMPONENT 61:Wall turbine E at(64.27570093457945,-1.2352941036224365,39.57943925233645)
- L11:0 up PHYSICAL_COMPONENT 61:Wall turbine E at(63.75,-1.7647058963775635,35.0)
- L11:0 down NO_FOOT_SUPPORT_-0.22_0.0 - at(68.45,-0.17647051811218262,35.0)
- L12:0 up NO_FOOT_SUPPORT_0.0_0.22 - at(70.0,-5.823529243469238,55.0)
- L12:0 down NO_FOOT_SUPPORT_0.0_0.22 - at(70.0,0.0,36.06666666666667)
- L18:0 up PHYSICAL_COMPONENT 61:Wall turbine E at(63.75,-7.5,43.0)
- L18:0 down CLOSED_SOLID_INTERIOR_OR_INVALID_POSE - at(70.0,-5.0,43.0)
- L18:2 up PHYSICAL_COMPONENT 292:L18 landing3 at(74.0,-0.35714292526245117,28.2)
- L18:2 down NO_FOOT_SUPPORT_0.0_0.22 - at(74.0,0.0,27.933333333333334)

Next: repair actual missing support/landing, walls/portal heights, crossing stair solids and terrain contacts from these measured poses; preserve rough allocations. Native owner/room/navigation/services integration, guards, terrain/foundation/van/story and older-map audits remain. No local editor UI or round-duration test.
