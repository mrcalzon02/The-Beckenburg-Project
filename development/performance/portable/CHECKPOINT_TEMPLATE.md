# Milestone checkpoint record — copy into target issue or PR

## Identity and scope
- Project/repository and authoritative branch:
- Current main SHA / candidate SHA:
- Who requested this precise change; scope and exclusions:
- Milestone (A–G), issue/PR and parent prerequisites:
- Engine/runtime, target hardware, OS, resolution/preset and validated assets:
- Existing build/installer and functional gates to preserve:
- Six-question preflight answered? Link:

## Hypothesis and minimal change
- Observed bottleneck (measured, not assumed):
- Responsible code and render/simulation stage:
- Proposed change, revision/resource ownership and affected invariants:
- Separate static vs dynamic data and invalidation triggers:
- Fallback/revert plan and current known limitations:
- Other concurrent branches considered:

## Evidence
| Test | Baseline | Candidate | Environment | Source/run/artifact | Verdict |
| --- | --- | --- | --- | --- | --- |
| CPU p50/p95/p99 (ms) |  |  |  |  |  |
| GPU execution timers, if available (ms) |  |  |  |  |  |
| Upload/readback bytes and stalls |  |  |  |  |  |
| Cache-hit / invalidated regions |  |  |  |  |  |
| Loaded scene/startup latency |  |  |  |  |  |
| Functional gameplay + input |  |  |  |  |  |
| Real installed application |  |  |  |  |  |

- Raw CSV/log/images paths; identity and capture timestamp:
- CI outcome by **exact SHA**, not any nearby green run:
- Hardware conditions held constant? sample count, warmup and error bars:
- Does the test actually exercise the requested system or only a synthetic substitute?
- Defects introduced/observed, issue IDs and owner:
- Cosmetic-only comparisons explicitly non-blocking unless user requested them:

## Publication
- Commit and branch:
- Remote readback confirms SHA? yes/no (proof):
- PR merged? main HEAD after merge:
- Functional CI and release workflow complete? Link:
- Release filename/checksum and actual playable round proof, if requested:
- Clear status: planned / code pushed / compiled / GPU functional / hardware accepted / released:
- Next **scoped** action and dependency; no implied automatic work in other repos:
