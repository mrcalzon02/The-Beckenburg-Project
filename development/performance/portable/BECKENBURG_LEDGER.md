# Beckenburg-specific A–G evidence ledger

This file tracks the **source-specific Beckenburg implementation**, separated from the portable guidance. Last verified on 2026-10-26 during the creation of the playbook. Results can change; inspect current Actions and issue #7 before using them.

| Step | Implementation and exact revision | Evidence | Current credit |
| --- | --- | --- | --- |
| A initial House GPU route | 003b72e4871d093e0fcfb9b3e6727029a0550d82 | GPU 40389641265 success; 33 viewpoints, Mesa timings and CSV | Automated profiling accepted; continuous physical player route and actual hardware timers outstanding |
| B1 prepared scenery faces | 8bc88ac0ca3e90684db53ca4efd2b7c8c99829df | GPU 40399486816, full multiplayer 40399486612 and installer 40399486685 all success | Released baseline B1 |
| B2a versioned resident GPU surface handles | 97eba4cdd934bcfe85d4d84089910f74da1739f9 | Branch GPU 40400718326 success; full multiplayer 40400884423 rerun attempt 2 success | Promoted to main; same-SHA main GPU 40406490638, multiplayer 40406490657, installer 40406490618 success at this ledger checkpoint |
| B2b conservative static shell chunks | 1249aeead14ad0f6ea257e381b72e852942da846 | GPU 40402913529 success, geometry visibility parity, 2,048 skipped shell faces in test | Branch validated; runtime architecture remains on stacked PR #9 |
| B2c room-owned chunk boundaries | ee31e5c5fff140e2157c9fcbbac6fa16eae48a71 | GPU 40403714223 success | Branch validated; stacked PR #10 |
| B2d resident actual House sample | 60303b3400cff804ac47bd7f43b1167a3d78ff9f | GPU 40405376878 success: 350 faces, 746 triangles, 71,616 initial upload bytes, zero subsequent camera upload | Diagnostic proof only; not wired into the production Case renderer |
| C1 portal candidate graph | 42e2a03060a4be5ba3599521b8ac726af30b85b6 | GPU 40407155221 success | Diagnostic-only candidate reachability; hard occlusion not proved |
| D0 global light and occluder revision | df70a9796022c9e4657f30f8880eb33614e0f8d9 | GPU 40405238900 and full multiplayer 40405278777 success | Version API on branch, not GPU lighting migration |
| D1 regional light revisions | a87dcd3e556793311753c34a6db99db5497a40fb | GPU 40406382960 success | Influence cell revision contract on branch, not complete independent lightmaps |
| CI native HotSpot fatal logs | d1f9e985451c0497fc5c6c96f54c6a0aadbfb4ea | Multiplayer 40404317442 success | PR #11 preserves hs_err_pid logs on failures; no tests disabled |

**Caution:** Earlier B2a multiplayer run 40400884423 attempt 1 had 247/248 passed and crashed in HotSpot/JIT HouseMap.wallBlocked during LanHunterNameplateTest. Rerun passed; intermittent hardware/JIT failures should remain an observation, not be erased or called root-caused.

**Still unfinished:** fully resident static House geometry in normal live scene; camera-invariant static lighting and dynamic light overlays; complete room/portal occlusion; GPU-native effects/depth composition and reduced CPU readback; deterministic preload/warmup/flyover handoff; real hardware 30 FPS acceptance across 22 maps, installed single-player and multiplayer.

**Master tracking:** https://github.com/mrcalzon02/The-Beckenburg-Project/issues/7

**Reminder:** Mesa software OpenGL p95 metrics vary with runner load. Never convert one run into asserted speedup or target-device FPS.
