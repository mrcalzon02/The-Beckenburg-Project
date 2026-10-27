# Codex A–G execution checkpoint log

This record is the per-project progress ledger for the FINISH plan. Update **after each verified commit**; never backfill "passed" from an unrelated SHA.

## Initial handoff checkpoint (2026-10-26)

- Mission status: PLAN COMMITTED; **A–G NOT COMPLETE**. Implementation not yet handed to an executing local Codex agent.
- Baseline main: df221653471eda5364cf8653fa83cb2aba5d35e9 (F0/F1 merged).
- Last verified public installer: beckenburg-v0.1.81 from e2ad831f9ba3dd63de79017e2e1d0766d8201678.
- Exact-main GPU 40424723169: SUCCESS; full multiplayer 40424723227 and installer 40424723354: IN PROGRESS at initial handoff.
- D3 real House resident fixture test 40417305528: SUCCESS, diagnostic only.
- E0 readback profiling 40418245781: SUCCESS, measurement only.
- D4 normal-preservation and real-gameplay static ownership candidate: PR #28 SHA 2fe3400ae597d7765f1d6883eaea99fa2f67a91c; exact GPU 40425466364: IN PROGRESS at initial handoff.
- C basement frustum/portal mismatch still unresolved depth-wise; issue #20. Hard portal occlusion disabled.
- Outstanding primary dependency: D4 source ownership -> live resident/static/dynamic scene split -> correct lighting/depth and hardware benchmarking.

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
