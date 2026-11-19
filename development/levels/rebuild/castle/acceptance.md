# Castle Beckenburg — next complete-map integration checkpoint

**Selection:** 18 November 2026 (America/Sitka); live `main` reconnaissance HEAD `af7d662e1efc0bc7857619c7e0b907f07b111181`.
**Status:** NEXT CANDIDATE / RECONCILIATION AND FIRST SCOPED REPAIR IN PROGRESS.
**Do not claim Castle complete, stage-accepted, physical-hardware accepted or newly published.**

## Why Castle is next

- Owner's first-principles priority override explicitly completes Castle HQ before the rest of the map roster. New Briar House default was promoted in `ea4e36cc`, with runtime generation gating in `917585ce`.
- The 7 November stage register (stale tracking) shows Castle S00–S08 IN_PROGRESS, S09–S11 NOT_AUDITED. It gives Castle **no accepted stage**. House has S00–S09 IN_PROGRESS; Farmhouse/School and other institutions only S00–S02, most remote sites S00–S01.
- The scoped 6 November Castle reviews include 42 native architectural spaces, 39 accessible and 3 permanently sealed, with 168 actual 320×200 four-direction render captures; local native-upgrade checkpoint has 42 named passing focused test bodies. These are historical checkpoints at older SHAs, not same-source acceptance for current main.
- The register's pending “saved-native upgrade, story, loose objects” text is outdated: actual `CastleRebuildUpgrade` and custom-source retention/conflict tests, `CastleStory` and `CastleLooseClutterTest` already exist on main. Do not recreate them; **revalidate and inspect** them after current motion, renderer and user interface changes.
- Current main has the physical county campaign board and fixture source-mount corrections; the latest 50 Hz LAN hunter/ghost movement changes do not themselves certify the local Castle walk/interaction experience.

## S09–S11 finish-gate sequence

| Order | Acceptance gate | Current evidence / deficit | Test/review needed before “finished” |
|---|---|---|---|
| 1 | Authoritative geometry/navigation/search envelope | Historical 39 open / 3 sealed; stairs, cellar, west annex, ramparts built | Re-run native reachability and every floor opening, guard, seal and actual swept approach after current editor/load changes; protect 40-space cap |
| 2 | Saved-native restoration, edition and rollback | Three-way rebuild, exact backup, custom delta and conflict refusal implemented | Existing CastleRebuildUpgradeTest + editor save/reopen/undo/redo on exact HEAD; compare historical/current source identities and no silent edits |
| 3 | Ground-floor function and case continuity | Nine stations, 5 residents, role/stock/contract/van, story/loose props/training previously tested | Entire Castle → contract selection → case → end/results → same Castle and persisted profile, with solo and host/guest/TCP; inspect multiplayer loadouts and return placement |
| 4 | Real portcullis arrival/exit | **DEV-389**: entrance box previously ignored ground support and native obstructions | This iteration guards threshold using real body traversal; run CastleHomeInteractionTest and installed walking/retreat/closed-gate/blocked native edit cases |
| 5 | Lighting, materials, optical and art | Historical 168 compass views and clear-night correction; issue #57 still has NPC sprite, UV, facing/occlusion concerns | Matched ordinary-height day/night/adjacent circuit/door/portable-light reviews; original model UV/art preserved; avoid treating small compass captures as art signoff |
| 6 | Current full, multiplayer, GPU and installed acceptance | Earlier Castle suites passed, but current full-run wrapper alone is not site hardware proof | Exact SHA focused/native/full CI with raw manifest; physical Windows GTX review of fps tails, movement/camera input, menu readability, memory, and solo + 3-seat LAN; publish verified images/gallery |

**Release policy:** Only build/launch/run and required multiplayer/updater failures block unrelated releases. Visual, balance and style issues remain explicit diagnostics. No round-duration testing is added to this map rebuild gate.

## First bounded source iteration

- **DEV-389 (source guard):** `CastleGateExit.inExitThreshold` now requires an open gate, finite position, a ground-floor foot height within 0.15m, the existing threshold bounds and `map.traversable(x,z,elevation)`. The same authoritative native surface/collision used for the player's real position must approve exit.
- **Native negative cases:** `CastleHomeInteractionTest` checks cellar-height/raised/nonfinite poses, blocks the real gate apron through `CastleLevelEdits.setNavBlocked(15,35,true)`, proves the threshold refuses it, restores the snapshot and proves the exit works again. It retains the original closed-gate, wrong-location, upper floor, pause and options checks.
- **Risk containment:** No geometry files, model source art, House/mission van extraction, ghost simulation, network protocol or saved scene schema changed. The test must pass on this source before map closeout; status remains pending until run.
- **Next focused iteration:** Evaluate the installed Castle gate and enter/retreat control on a physical Windows runner, run full Castle-to-case-to-Castle round-trip and inspect open issue #57 render defects. Attach raw test logs, annotated captures, actual map screenshots, native snapshots and exact same-SHA manifest. Reconcile all 29 stage packets without inventing completion percentages.

**Historical references:** `development/levels/rebuild/verification/castle-closeout-2026-11-06/`, `.../castle-native-upgrade-2026-11-06/`, `.../castle-story-2026-11-06/`, `.../castle-loose-2026-11-06/`; `development/quality/MAP_FITNESS_CHECKLIST.md`; `development/levels/rebuild/stage-register.json`; issue #57. These are evidence sources, not current passing results.
