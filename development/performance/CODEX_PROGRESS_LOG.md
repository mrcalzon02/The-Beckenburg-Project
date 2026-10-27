# Codex A–G execution checkpoint log

This record is the per-project progress ledger for the FINISH plan. Update **after each verified commit**; never backfill "passed" from an unrelated SHA.

## Local preservation/unification entry (2026-10-26)

### Unified baseline accepted; live House source connected

PR #32 merged to remote main `1ba745cd3e0a1a9acfc5b32fe857866c6d05dfe9` after expanded source `17a8594f` passed exact-head full acceptance [40436670628](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40436670628), GPU [40436667806](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40436667806), and PR full acceptance. Local complete manifest: **253 passed / 0 failed / 0 skipped**. This saves/unifies the three local worktrees plus user-requested PRs #3/#29 and fatal-report retention #11. New main installer checks are independent and were not cancelled.

Reconciled existing tested D3/D4 code at `07774041`; source ownership/revision/probe tests pass against the unified game. A native app-image (not installed product acceptance) reached House: initial scene 1,796 ms; hunter handoff 12,322 ms. User's House-facing turn reproduced a 2,357 ms frame (`RENDER ... lighting_raster_ms=2340`). This is the actual live bottleneck; unification did not fix it. Desktop control was stopped by physical Escape and has not been resumed.

Live candidate now connects immutable House architecture/scenery to normal GPU draws behind a default-off internal switch. Functional contract: 209,244 source faces / 789 chunks / 665,532 vertices / 21,297,024 packed bytes, with **zero static rebuilds, probe refreshes or uploads on camera-only turns after warmup**; dropped upload, fixture/door, resize, UV fallback, scene and new-map checks pass. Separately retained stationary visibility avoids retracing rays on source switches. Actual GPU timing/depth and installed case gates remain pending. See `LIVE_HOUSE_RESIDENCY.md`; no FPS or A–G completion claim.

Expanded by explicit user direction to include feature PRs #3/#29. Earlier unified source `d2c644a0` passed exact-head full acceptance [40434726259](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40434726259) and GPU [40434626216](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40434626216); newer feature integration requires its own checks. Reconciled Quartermaster `e988eb01`, hunter/companion `f3c86035`, fatal JVM log retention `84cac58a`, native teardown `417980e6`. The latter passed three real RTX 5070 diagnostics with process exit zero, versus baseline `f6793bda` exit 0xC0000409. Their cold-route CPU p50/p95 remain 108–123 / 2796–2852 ms, **not accepted interactive FPS**. Fixed a reproduced hunter-branch ghost-resume failure without weakening recovery tests. Detailed code/evidence/status: `development/integration/2026-10-26/` and `hardware/2026-10-26/`. Remaining: expanded exact-SHA full acceptance, native case/hardware baseline, then work packages 0/1. No A–G milestone marked complete.

User requested saving/unifying existing work before beginning performance implementation. Inspected current main `f6793bda4f1c6c821fe66e49050b076b01cce6c0`, required recovery plans and issues #6/#7/#20. Three local worktrees were dirty. Pushed and read back unverified preservation checkpoints `d07d7450`, `b7b193f4`, `06341f84`; retained exact details and original hashes under `development/integration/2026-10-26/`. Unified current main at `5956e173`, then preserved native map sources at `4f92b65d`; mission-board reconciliation and full CI are next. Local Java 17/OpenGL compilation and focused functional checks passed as documented there. Hardware detected: RTX 5070 / 616.64; **installed hardware acceptance PENDING**. No A–G gate closed and no release published by this session.

## Initial handoff checkpoint (2026-10-26)

- Mission status: PLAN COMMITTED; **A–G NOT COMPLETE**. Implementation not yet handed to an executing local Codex agent.
- Renderer release source: df221653471eda5364cf8653fa83cb2aba5d35e9 (F0/F1 merged); repository main subsequently moved to documentation-only merge a3073cde777b87eeb1362e2ad93db9dea6139036, with no renderer changes. Check fresh SHA at execution.
- Last verified public installer: **beckenburg-v0.1.82**, source df221653471eda5364cf8653fa83cb2aba5d35e9, Windows x64 EXE + Java 17 editor kit + SHA256SUMS.
- Exact release-source GPU 40424723169, full multiplayer 40424723227 and installer 40424723354: **ALL SUCCESS**. GitHub release v0.1.82 verified.
- D3 real House resident fixture test 40417305528: SUCCESS, diagnostic only.
- E0 readback profiling 40418245781: SUCCESS, measurement only.
- D4 normal-preservation and real-gameplay static ownership candidate: PR #28 SHA 2fe3400ae597d7765f1d6883eaea99fa2f67a91c; exact GPU 40425466364: **SUCCESS**. Production source switch still NOT implemented/accepted.
- C basement frustum/portal mismatch still unresolved depth-wise; issue #20. Hard portal occlusion disabled.
- Outstanding primary dependency: D4 source ownership -> live resident/static/dynamic scene split -> correct lighting/depth and hardware benchmarking.

## Verified pre-handoff addendum (2026-10-26)

- Original plan source f7729eab4d10005cca14229b60cca640c48c5481 saved as draft PR #30 and linked from milestone #7 and goal issue #31. Project evidence PR #21 was merged at a3073cde777b87eeb1362e2ad93db9dea6139036, preserving C2 depth findings and the Beckenburg-only ledger.
- Published release v0.1.82 and exact-source GPU/multiplayer/installer SUCCESS confirmed. No need to wait on the former release workflows to preserve the Codex documentation on main.
- D4 exact-source GPU suite SUCCESS confirms authored-face identity and subdivision normal test, but does not establish actual gameplay GPU residency.
- **Do not infer that a Codex run has started.** This checkpoint only prepares future goal-oriented work.

## Every next checkpoint

### Checkpoint [add sequential identifier]

- UTC date/time:
- Task/work package:
- User-authorized scope:
- Starting main SHA / branch:
- New source SHA and remote readback:
- Files changed and fallback switch:
- Functional test commands, exact CI run links and outcome:
- Benchmark environment: OS / CPU / GPU / driver / resolution / preset:
- Baseline vs change: p50 / p95 / p99, GL transfer bytes, case-start milliseconds, memory:
- Genuine hardware result or reason pending:
- Gameplay/evidence/network/ghost/van/editor regressions checked:
- Defect or blocker and its reproducible evidence:
- Was this deployed to main? Was it released? Exact source SHA and checksum:
- Rollback and next prerequisite:
- Gate status: NOT STARTED / IMPLEMENTED / CODE PASSED / CI PASSED / HARDWARE ACCEPTED / RELEASED.

Do not delete previous checkpoints or convert planned statuses into successes. Link new regressions to the authoritative defect ledger and issue #7.

## Live House measured checkpoint (2026-10-27 03:11 UTC)

Source `performance/live-house-residency`, pushed/read back `6e0c698599e66553adf54f2de4d26750e6c81d9d`; draft [PR #33](https://github.com/mrcalzon02/The-Beckenburg-Project/pull/33). Concurrent main PSX art docs/tool retained. RTX 5070 / 616.64 / 712x200 / Retro 200p measured twice each: legacy frame p95 108.523-109.803 ms; resident fixed 35.858-36.077 ms; live fixture clock 36.075-36.729 ms. First exposed yard 3880-3970 ->600-624 ms. **Improved, not accepted 30 FPS.** Zero camera-only static uploads; retained GL vertex capacity 32,044,800 bytes. Depth parity: zero original pixels missing/farther beyond 5 cm at van/interior/upstairs. Cold prewarm about five seconds. Raw evidence under hardware/2026-10-26/live-house-6e0c6985; cargo-obscured first route excluded and labeled separately.

Exact-head GPU [40442270881](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40442270881) and full [40442306416](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40442306416) pending; full local regression running. DEV-131 tracks remaining stalls. Next task: stationary door-ray dependencies, then lifecycle and installed full case. Desktop input remains stopped after user Escape. No new release or A�G completion claim.

## Localized stationary door dependencies (2026-10-27 03:18 UTC)

Source `6e0c6985` completed GPU [40442270881](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40442270881) successfully, including new real live-path Mesa/depth check. Complete local mandatory regression: **255 passed / 0 failed / 0 skipped**; retained log under this checkpoint. Newly verified increment tracks source intensity separately from each stationary ray's intersected, conservative door/window volume revisions. Every actual House door matches fresh uncached visibility/RGB; **585 localized rays vs 7860 fresh rays**, maximum 35/59 probes refreshed. Existing source-switch, normals, physical lighting, optical evidence, D3/D4 and actual live-scene tests pass. No geometry rebuild or unrelated RGB upload on a door transition. Emissive changes now participate in packed RGB comparison even when ordinary irradiance is unchanged.

Fallback remains default-off; legacy/software/global revisions remain intact. Exact new-source GPU/full CI and hardware door-transition profiling next. Full installed investigation-to-Castle acceptance remains pending; do not advance C/E/G or claim 30 FPS based on this change.

## Explicit GPU viewport lifetime checkpoint (2026-10-27 03:28 UTC)

Localized doors committed/pushed/read back `b88f4405`; exact-head full [40443415880](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40443415880) and GPU [40443414814](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40443414814) pending. Source `600c0456` adds exhaustive pixel-equivalent quantization lookup and stage/event hardware profiling. Raw repeat runs retained under hardware/2026-10-26/live-house-600c0456. Resident CPU p50 now about 12 ms; p95 completed frame still 34.8-36.4 ms fixed /38.0-51.2 ms live clock /53.5-55.3 ms events. No 30 FPS claim.

Next verified code increment: explicit nonblocking release on case, Castle, ghost, opening, editor and water-preview close, with deletion only on owning GL thread. Disposal epoch forces safe restoration even when a closed preview clears latest framebuffer. Contract test covers inactive close preserving another viewport, active close, restoration and prohibition on closed resubmission. Java17 GPU compilation and existing identity/opening tests passed; actual GPU lifecycle test added, execution pending. Worker teardown remains unchanged.

Existing full acceptance workflow cancelled earlier superseded same-branch runs automatically; no manual cancellation was issued. Concurrency now retains each exact SHA/event with cancel-in-progress false, preserving checks during subsequent recovery checkpoints. No installer workflow was edited/cancelled. Full installed native case and A-G acceptance remain pending.
