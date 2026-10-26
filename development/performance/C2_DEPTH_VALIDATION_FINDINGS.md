# C2 audit interpretation and C3 depth-evidence gate

Source: Beckenburg full GPU acceptance 40410389913 on commit 1918b289ec056618385c95ac1175b92a9bf8db3d. Artifact beckenburg-van-house-viewport-profile includes portal-candidate-audit.csv, captured from the actual authored House scene. The diagnostic itself passed. It does not enable portal culling.

| Camera room | Frustum-accepted static faces | Faces outside portal candidate set | Authored missing room IDs |
| --- | ---: | ---: | --- |
| 14, van (outdoors) | 15361 | 0 | none |
| 13, approach (outdoors) | 12309 | 0 | none |
| 0, entrance | 8847 | 425 | 10 and 11 |
| 4, interior stair hall | 4333 | 306 | 10 and 11 |
| 8, upstairs | 8411 | 134 | 11 |

The five observations repeat unchanged after opening the single sampled door. In House LevelPlans, rooms 10 and 11 are **basement** rooms, and stairs connect main hall 4 to basement landing 9. Their faces can intersect the frustum while being blocked by authored floors and walls; this audit did **not** test actual final screen/depth visibility. Consequently it is inaccurate to call these 865 frustum-level faces missing **pixels** or a proven on-screen renderer defect.

**C3 acceptance dependency:** obtain depth/occlusion-resolved reference for frustum-visible faces and real physical camera traces, with stairwell/floor void and open/closed basement door cases. Never simply whitelist the two numbered rooms, conceal the frustum mismatch, or enable hard room rejects until depth-visible authored geometry, sprites and interactive targets have passed. The GPU depth buffer and CPU fallback should agree. Gameplay and network authority may not depend on culling.

Issue: https://github.com/mrcalzon02/The-Beckenburg-Project/issues/20. This is a reusable precaution for other projects: a connectivity graph, a frustum test and an actual occlusion/depth-visible result are three distinct concepts; document which measurement each test provides.
