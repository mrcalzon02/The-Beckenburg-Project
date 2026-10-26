# Beckenburg-specific A–G evidence ledger

This file tracks the **source-specific Beckenburg implementation**, separated from the portable guidance. Evidence last updated on 2026-10-26; entries explicitly distinguish merged source, passing branch CI, and pending acceptance. Results can change; inspect current Actions and issue #7 before using them.

| Step | Implementation and exact revision | Evidence | Current credit |
| --- | --- | --- | --- |
| A initial House GPU route | 003b72e4871d093e0fcfb9b3e6727029a0550d82 | GPU 40389641265 success; 33 viewpoints, Mesa timings and CSV | Automated profiling accepted; continuous physical player route and actual hardware timers outstanding |
| B1 prepared scenery faces | 8bc88ac0ca3e90684db53ca4efd2b7c8c99829df | GPU 40399486816, full multiplayer 40399486612 and installer 40399486685 all success | Released baseline B1 |
| B2a versioned resident GPU surface handles | 97eba4cdd934bcfe85d4d84089910f74da1739f9 | Branch GPU 40400718326 success; full multiplayer 40400884423 rerun attempt 2 success | Promoted to main; same-SHA main GPU 40406490638, multiplayer 40406490657, installer 40406490618 success at this ledger checkpoint |
| B2b conservative static shell chunks | 1249aeead14ad0f6ea257e381b72e852942da846; merged on main as 53b5940cea6926c98643c065f2fa44fb6367d249 | GPU 40402913529 and exact-main GPU 40410064323 passed; Castle checks 40410064298 passed | Code merged; main multiplayer 40410064284 and installer 40410064334 still pending at last check |
| B2c room-owned chunk boundaries | ee31e5c5fff140e2157c9fcbbac6fa16eae48a71 | GPU 40403714223 success | Branch validated; stacked PR #10 |
| B2d resident actual House sample | 60303b3400cff804ac47bd7f43b1167a3d78ff9f | GPU 40405376878 success: 350 faces, 746 triangles, 71,616 initial upload bytes, zero subsequent camera upload | Diagnostic proof only; not wired into the production Case renderer |
| C1 portal candidate graph | 42e2a03060a4be5ba3599521b8ac726af30b85b6 | GPU 40407155221 success | Diagnostic-only candidate reachability; hard occlusion not proved |
| B2e camera-invariant authored fixture samples | df8829dbf0dc4be9660e59a41b553b87a302b1c0 | GPU 40411146705: compilation+Windows passed, Mesa pending at last inspection | PR #18 diagnostic only; do not infer gameplay residency or no-bleed lighting |
| C2 portal/frustum discrepancy audit | 1918b289ec056618385c95ac1175b92a9bf8db3d | GPU 40410389913 SUCCESS; audited CSV downloaded: interior room 0 had 425 frustum-accepted House faces outside candidate graph (rooms 10,11), room 4 had 306 (10,11), room 8 had 134 (11), in both sampled door states | Diagnostic ONLY. Basement geometry is frustum-intersecting, NOT proven pixel-visible behind floors. Depth-aware check tracked in issue #20; hard portal culling OFF |
| D2 world-space normal-aware fixture probe cache | dd403ca40239272496f049d9ecfca6000ac9350e | GPU 40412014565 in progress, Java 17 compile passed | PR #19: selective static-light sample invalidation candidate; no live GPU lightmaps |
| D0 global light and occluder revision | df70a9796022c9e4657f30f8880eb33614e0f8d9 | GPU 40405238900 and full multiplayer 40405278777 success | Version API on branch, not GPU lighting migration |
| D1 regional light revisions | a87dcd3e556793311753c34a6db99db5497a40fb | GPU 40406382960 success | Influence cell revision contract on branch, not complete independent lightmaps |
| CI native HotSpot fatal logs | d1f9e985451c0497fc5c6c96f54c6a0aadbfb4ea | Multiplayer 40404317442 success | PR #11 preserves hs_err_pid logs on failures; no tests disabled |

**Caution:** Earlier B2a multiplayer run 40400884423 attempt 1 had 247/248 passed and crashed in HotSpot/JIT HouseMap.wallBlocked during LanHunterNameplateTest. Rerun passed; intermittent hardware/JIT failures should remain an observation, not be erased or called root-caused.

**Still unfinished:** fully resident static House geometry in normal live scene; camera-invariant static lighting and dynamic light overlays with proven wall-side parity; complete room/portal occlusion (issue #20); GPU-native effects/depth composition and reduced CPU readback; deterministic preload/warmup/flyover handoff; real hardware 30 FPS acceptance across 22 maps, installed single-player and multiplayer.

**Master tracking:** https://github.com/mrcalzon02/The-Beckenburg-Project/issues/7

**Reminder:** Mesa software OpenGL p95 metrics vary with runner load. Never convert one run into asserted speedup or target-device FPS.

## 2026-10-26 checkpoint: CI resilience and opening readiness

- **B2c promoted to main** (cbe22780beaf61c14b82cb1d9399d23b465f2e3b). Java 17 and Castle functional checks succeeded. Exact-main GPU run 40413196375 first attempt failed BEFORE any rendering test because Chocolatey Blender feed returned HTTP 504, followed by unscoped Program Files discovery access-denied. Failed Windows job rerun requested; do not misclassify as renderer defect or mark exact-SHA GPU accepted until rerun passes. Main multiplayer 40413196299 and installer 40413196183 still in progress at this checkpoint. Previous verified installer is 0.1.79 from 53b5940.
- **CI resilience**: draft PR #23 (e2ad831f9ba3dd63de79017e2e1d0766d8201678) centralizes real Blender setup into scripts/ci/ensure-blender.ps1 with bounded official Chocolatey retries, scoped executable path discovery and fail-closed asset export. GPU CI 40415371185 pending. No tests weakened.
- **F0 first-frame readiness**: draft PR #24 (19bab1e1b54c3d05345e9431d52a11223dae5cfa) records durable initial GPU flyover preview request, actual first-frame completion timing and cancellation/fallback outcome. Existing all-site flyover test ensures the player spawn/camera is unmodified; GPU acceptance file filter now includes cinematic and flyover changes. GPU run 40415807639 pending. F0 is not preloading, shader warmup or a proven case-start hang fix.
- **C3**: 40412860035 GPU acceptance passed on 36f1dab4b668b5218af7bc57bb72ab8a2ca9f684. Bounded collision centroid ray diagnostics do not prove final pixel/depth visibility and cannot yet authorize portal culling.
- **B2e/D2**: 40411146705 and 40412014565 GPU suites passed on their exact branch revisions; preparation and local fixture probe cache are diagnostic/data-only, not production shader or resident House scene integration.

**Outstanding A–G:** connect camera-independent static geometry with region-invalidation lighting in actual play, with wall-normal/occlusion parity; validate depth-aware room culling without hidden geometry; reduce GPU readbacks; prepare shader/level readiness before flyover; benchmark and verify installation on actual Windows hardware across authored maps.

## 2026-10-26 follow-up: release 0.1.80, F1 and D3 implementation

- **Published installer**: Beckenburg v0.1.80 at https://github.com/mrcalzon02/The-Beckenburg-Project/releases/tag/beckenburg-v0.1.80, tag target and current main both cbe22780beaf61c14b82cb1d9399d23b465f2e3b, 556258696-byte Windows x64 installer, Java 17 editor JAR, SHA256SUMS; same-commit GPU 40413196375 retried Windows job passed, final Mesa passed, multiplayer 40413196299 passed, Castle 40413196269 passed, installer 40413196183 passed. Initial Blender 504 on this run was a package-infrastructure error, not a renderer regression.
- **F0 acceptance**: F0 instrumentation branch 19bab1e1b54c3d05345e9431d52a11223dae5cfa GPU run 40415807639 passed; it records initial preview first-frame timing/cancellation but is not a warmup solution.
- **F1 implementation**: draft PR #25 SHA 3b405fffc9c3be56293bb448febfd35370567cb2 implements a bounded 3-second first GPU preview readiness gate for the existing 7.5-second intro flyover with immediate software fallback and monotonic late-frame behavior; deterministic functional test and Windows GPU acceptance run 40416597707 pending at ledger update. No gameplay simulation/character teleport, no assertion that case hang is root-caused.
- **D3 integration diagnostic**: draft PR #26 SHA f4a1b3c271d26dd10956de7e2886d2ad0b1f122a stacks on B2e and incorporates source-owned D0-D2 fixture revisions/probe cache into actual authored House resident VBO sample. Keeps geometry IDs stable across camera motion, reuses world-position normals/fixture RGB, advances room/material VBO revisions and uploads real buffers on door occlusion change. Exact-source GPU CI 40417305528 in progress; no production pipeline switch, no hardware FPS claim.
- **CI resilience**: draft PR #23 SHA e2ad831f9ba3dd63de79017e2e1d0766d8201678 shared fail-closed Blender script has passed GPU 40415371185; multiplayer 40415449044 still in progress at this ledger update. Do not label validated CLI tools as released until same-main installer checks pass.

Transferable insight: first-preview readiness is distinct from elapsed cinematic duration; GPU buffer geometry identity is distinct from lighting revision and per-region lighting cache; a software package feed timeout is distinct from functional test failure. Continue tracking production integration, C3 depth visibility, E GPU/CPU compositing, F full shader warmup and G installed hardware acceptance separately.

## 2026-10-26 further A–G engineering checkpoint

- **CI fix promoted to main:** exact commit e2ad831f9ba3dd63de79017e2e1d0766d8201678 fast-forwarded without force after full branch GPU 40415371185 and multiplayer 40415449044 success. GitHub PR command briefly failed, so compare-and-swap main SHA was used; remote readback and branch ancestry confirmed and PR #23 closed. Same-main GPU 40417700717 and installer 40417700858 in progress as of this checkpoint. Last published verified installer is v0.1.80 from cbe2278.
- **F1 — full GPU acceptance passed:** draft PR #25 at 3b405fffc9c3be56293bb448febfd35370567cb2, GPU run 40416597707 SUCCESS. The cold intro clock starts at its first copied preview or a 3-second bounded fallback; late frame cannot rewind the presentation. Still a development branch, no installed Windows case-start hang resolution or shader warmup claimed.
- **D3 — actual House fixture-probe/VBO diagnostic integration:** draft PR #26 at f4a1b3c271d26dd10956de7e2886d2ad0b1f122a, GPU CI 40417305528 Java17 compiled, Windows/Mesa pending. Stacks on B2e and D0-D2. Stable room/material VBO IDs and per-region lighting revisions, no upload for unchanged camera view, explicit reupload on authored door changes. Diagnostic only, not a production renderer migration.
- **E0 — readback decomposition:** draft PR #27 at bff0f48708346ea031916dd4e27acc599c0656a0, GPU CI 40418245781 started. Measures original OpenGL GL call-wall color/depth readbacks, bytes, and Java pixel conversion, requires depth availability for existing overlays. This is evidence gathering, not GPU timer queries or an async PBO migration.

Keep remaining milestones independently gated: C true pixel/depth occlusion rather than frustum/LOS heuristics; D dynamic equipment/ghost effects and wall-side parity; E readback elimination after evidence; F complete level asset + shader warmup/first-case hang; G hardware low-end GPU 30 FPS with all maps, installer and multiplayer. No other repository was changed.
