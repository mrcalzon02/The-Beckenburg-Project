# Full map rebuild — execution continuity

Goal activated 31 October 2026, America/Anchorage: execute the complete staged rebuild across 28 base locations and the mortuary annex. The goal is active; no map is declared rebuilt by this checkpoint.

## First executed milestone

- Preserved the prior blockout branch and planning commit; synchronized to main `fba22b2a3eb56871eeda009f7cd0ab8c4ad4aa5c`.
- Built the actual game/assets with installed JDK 17. Java stalls inside this sandbox but runs through the approved elevated command path; use `C:\Program Files\Java\jdk-17\bin\java.exe` and quote PowerShell arguments such as `'-Djava.awt.headless=true'`.
- Captured nine built-in investigation definitions, Blackpine, Hollow Creek and Castle: **700 principal directional views**. Additional room contact sheets, child-washroom/door views and 40 separate Castle after-fix views are retained. Render evidence is diagnostic, not native Windows interactive acceptance. Hollow Creek software captures are extremely slow (some frames around 30 seconds); this is not a physical GPU benchmark.
- Corrected missing exterior audit cameras by searching the full owning zone after the local search fails. Re-ran School, Mansion, Prison, Asylum, Academy, College and Church: 60 views each, zero missing. USER is excluded from future built-in ALL runs; the original failed USER attempt is retained as out-of-scope diagnostic history.
- Inspected all House cardinal panels and its approved concept; recorded specific retain/refit/replace decisions in `house/baseline-review.md`. Inspected the Prison main-hall directions and its existing compound preview, plus the School upper-corridor panel. Prison's current hall is visibly not a constructed cellhouse.
- Reviewed a contact sheet of 23 **existing** native blockout previews. These historical shell studies are not new geometry and do not supersede later Blackpine/Hollow Creek authoring. Source-only sites still need detailed native inspection and runtime construction.
- Added deterministic, hash-pinned source inventories for all 29 packets, including the annex's actual `layout-expansions/` path, existing native files, requirements and power/edge packets. `python tools/build_map_rebuild_baseline.py --check` checks staleness and does not accept maps.
- Corrected three recovery issues exposed by the baseline: the Matthis placement assertion's self-collision, Rowan's actual intersection with the network chair, and `MissionSites.title("USER")` throwing before a user project is selected. Player collision and fixed campaign ordinals remain intact. See DEV-200/201 (historical branch DEV-167/168) and DEV-208/170 (historical branch DEV-169 for Matthis).

## Recovery observations

Remote full multiplayer run `41273695792` on `f97ec96943dc57e90be5f735de4cfd061aa146c3` finished SUCCESS for the mandatory workflow, but its advisory regression explicitly reported **108 passed, 12 failed, 176 unexecuted of 296**. Installer `41273753752` on `fba22b2a` succeeded. GPU run `41273682437` on `44aa9c0d` failed. These are distinct revisions and scopes; no complete-source gameplay or hardware green claim is made.

Locally passing scoped checks: CastleSupportArtTest (with real furniture/unsupported-ground negatives and preserved keeper collision), CastleRelationsTest, CastleHubIntegrationTest, MissionBoardIntegrationTest, AuthoredLevelPhysicsTest, House AuthoredLevelReachabilityTest, AuthoredUtilitiesTest and LevelSurfaceIntegrityTest. The complete regression suite and new-source Windows GPU/installed multiplayer acceptance remain open.

## Next unfinished work

1. Preserve and publish this verified recovery/baseline checkpoint, refreshing the source inventory and gallery after the final evidence set. Reconcile concurrent main changes before upload.
2. Continue REC-01/02 diagnosis from actual remaining failures; local fixes do not certify the whole advisory manifest. Keep map promotion gated.
3. Complete S00 detailed per-site baseline findings and S01 function/reference programs across the roster. Prioritize House method, School facilities/privacy and Prison cell/control structure. Replace generic purpose placeholders with specific operating programs; do not copy a generic prop list into each room.
4. Finish S02 rough plans/sections across all packets, then prove House's revised physical construction method and perform the S03 blockout batch. Continue all subsequent passes in the authorized stage order.
5. Preserve newer Hollow Creek geometry/water/terrain authority and current Castle HQ behavior; the older shell contact sheet is reference history only.

All 348 decisions remain individually tracked. S00 inventory work is in progress for every site; later stages have not earned acceptance.

## Function-program and House construction-pilot milestone

Authored normal-use activities for all 456 named spaces across all 29 packets, with 554 existing route reservations and 141 explicit subordinate-room/task-zone requirements. Each packet now has a generated function brief, space program and route worksheet. Proposed capacities are design decisions for former normal use, not historical facts, NPC populations or player-limit changes. Measured references, final privacy/adjacency decisions and dimensions remain under review; S01 is IN_PROGRESS.

`HouseRebuild.java` now builds an isolated real-runtime House graybox. It does not replace dispatched House geometry. The ground loop, independent private-room entrances, separate kitchen/cellar stair and preserved canonical van use the same LevelDefinition/HouseMap/SceneRenderer authority as the game. Current local checks pass: door/stair physics, 61,328 swept positions reaching all 15 zones and functional reservations, exact deck/wall/guard surfaces, twelve usable switches, service panel, fourteen acoustic doors and two stair links. Sixty room-direction views are captured. The native source was reopened and compared vertex-by-vertex and UV-by-UV against 34,521 actual runtime triangles; source and inspection hashes are recorded in `house/native-graybox-verification.json`. Native front/rear/temporary-cutaway images are labeled by type. Rejected legacy-envelope source/render is preserved.

The pilot exposed and corrected a top-stair/foyer partition collision, a service panel authored into its doorway, a legacy fixed-size House skin applied to revised coordinates, and an urban fallback setting for the revised suburban House. The per-room roof is temporary graybox construction; exterior parcel clearance, terrain, structural refinement, furnishing and all full mission/hardware gates remain outstanding.

**New owner steering:** Architectural deck voids must be accompanied by genuine terrain-editor hole punching for basement/cavity sections. Investigation found a separate broad world-ground/backdrop sheet. The pilot's current architectural movement checks do not certify that sheet. The next work slice uses AdaptiveTerrainTopology's persisted punched leaves, TerrainEntranceMesh and the existing native physics adapter to connect actual missing world faces to rendering, collision and visibility, with a filled-surface negative control. Do not mark the basement or terrain stage accepted before this passes.

The owner explicitly approved verification-log and recovery-note upload on 31 October after the automated review rejection. Publication authorization persists; no repeat permission question is needed for these authorized project evidence checkpoints.

## Editor-punched basement checkpoint

Implemented the owner's terrain-hole requirement in the isolated House pilot. Saved editor topology cuts the complete cellar footprint using 30 subdivisions and 52 punched leaves. The 16-bit source heights remain unchanged. The scene publisher exports 3,430 conforming faces; the same SHA-bound HCS2 faces replace the renderer's broad world floor and participate in real body, optical and small-object queries. Existing architectural floor/wall/stair support remains in authority. `HouseRebuildTerrainTest` passes saved/reopened topology, idempotence, invalid-boundary rollback, source preservation, shared exported/rendered/physical geometry and a separately published filled-ground negative control. Both class-directory and packaged-JAR `HouseRebuildPhysicsTest` pass, including 61,328 swept positions reaching all 15 zones. Existing shared terrain/rail adapter regression passes. Refreshed 60 runtime directions: zero missing. The actual native source reopens with vertex/UV parity for 37,949 triangles; inspected stair and basement-terrain cutaway PNGs are retained and labeled as inspection views.

See `house/terrain-verification.md`. DEV-174 is fixed for this isolated pilot and remains open across the rest of the roster. No map/stage is accepted. The full roster's basement/cavity gate is now explicit in `stage-register.json`.

**Publication boundary:** the evidence/log approval is recorded above. Automatic review subsequently rejected a combined source/native-assets/evidence push because it considered the earlier approval evidence-only. A broader publication question is pending; do not retry that combined upload without its answer. Continue authorized local implementation and scoped local commits. Remote branch last verified at `d406a5f4`; local `be3f9802` and this terrain checkpoint still need authorized publication and exact-SHA CI.

Next: finish the measured rough-plan batch and detailed House facilities/structure/parcel pilot, then propagate its proven construction method. Every basement/cavity must receive its own saved editor opening and physical/optical/render evidence. Remaining mission/LAN/hardware/recovery and all later refinement gates are unchanged.


## Dispatch/campaign recovery and publication authorization — 1 November

The owner explicitly approved full rebuild publication: source changes, generated map/model files, Blender scenes, captures, design data and verification evidence on `map/first-principles-rebuild-plan` in `mrcalzon02/The-Beckenburg-Project`. This resolves the earlier combined-upload approval barrier. Publish accumulated checkpoints through the normal expected-head path and verify remote readback; do not keep treating evidence-only scope as the current authorization.

Reproduced and corrected DEV-175: stale selection/campaign checks assumed only three maps before the current nine-built-in/USER catalog. Against the actual packaged JAR, all nine built-ins now render with distinct source ownership, unselected USER and Hollow Creek remain unavailable, and nine physical campaign victories advance one chapter at a time with restart/receipt/no-skip checks. All thirteen unintegrated campaign destinations remain blocked. These recovery checks are explicit advisory steps in the existing room-audit workflow; they do not accept the generic institutional architecture.

Remote main remains `fba22b2a`; previous installer success and older full-round/advisory failures are unchanged at this read. Issue #6 remains open. Next publish/dispatch exact-source audit/full-round/GPU checks, inspect their actual steps and logs, and continue the measured rough-plan batch.


## Publication verified and School measured rough — 1 November

Full publication approval was executed normally: remote rebuild branch readback is `ddcef3bea3923fa8f2245b161095de1aeab82547`, containing the all-map function programs, House native/physics pilot, saved editor terrain opening, captures/evidence and repaired catalog/campaign regressions. Exact-source workflows were dispatched: room audit `41320065087`, full multiplayer `41320069837`, GPU `41320075324`. Windows GPU build/checks pass. Advisory audit-step labels alone are insufficient: the full regression transcript exposes a terrain-byte integrity failure in the House pilot. Mesa core smoke passes, but the following world-residency review exits 1; inspect actual artifact/logs before repair. Full round/advisory and complete capture runs were still live at this checkpoint. These statuses are not complete acceptance.

New School S02 authority in `tools/build_map_rebuild_roughs.py` produces 40 bounded architectural spaces, 44 reserved connections, explicit 15-owner mapping, real-run stair sections and source-asset bounds/hashes. The visually inspected planning PNG is labeled as a diagram. It replaces the broad corridor/toilet arrangement with a 3 m public spine, separate sanitary/service/changing rooms, classroom stores, kitchen receiving and a reduced service basement. Actual School runtime, editor cutout, secondary upper exit and full fit/structure gates remain open. Continue the full roster rough batch; do not skip the other 27 packets because House and School have revised studies.


The GPU artifact for `ddcef3be` confirms core smoke success and world-residency frames through HOLLOW_CREEK, then failure before USER: that slot has no selected authored project in the review process. DEV-176 now has an explicit persisted USER fixture in an isolated evidence home, with absent-selection negative control and real-home restoration before rendering. Packaged flyover checks pass all eleven enum values; exact new-source GPU validation remains pending. The full-round workflow `41320069837` is now terminal SUCCESS for mandatory multiplayer/UI/native package steps; inspect its advisory regression transcript separately. Room audit `41320065087` remains live in complete captures, so do not start a duplicate.


The full multiplayer run's mandatory steps pass, but the advisory manifest is **240 passed / 12 failed / 45 unexecuted of 297**, not complete acceptance. Failures: GhostVoicePackRoutingTest, LevelOpeningFlyoverTest, HandheldSpiritBoxEvidenceTest, LanGhostHearingTest, LanSpatialEvidenceIntegrationTest, LanSoundTest, LanSoundHudTest, LanTelegraphedHudTest, DirectionalSpritesIntegrationTest, RoomAcousticPresetsTest, HouseRebuildPhysicsTest and HidingBudgetTest. DEV-177 reproduces the House failure as Git autocrlf rewriting source/OBJ fingerprints. New -text attributes preserve exact saved terrain bytes; Git-filter proof covers Windows/Unix/input configurations without weakening inspection. The USER fixture repairs the separate flyover/residency precondition. Both fixes still require exact-new-source CI. Continue the same live room audit handle; do not infer failure/termination from a polling interval.


## Unification checkpoint — 3 November 2026

Owner direction: unify, merge and publish before resuming the map rebuild. Preserved all newer native-editor, van-fit and playable-hunter commits. Pending hover/prepared-picking work is saved in `f672140b`. Online main was merged at `e76d7817`, retaining both recovery histories and the stronger physical-extraction/durable-receipt campaign test. The first actual desktop run exposed a redacted-log expectation; online main independently supplied the privacy-safe notice correction in `ca31e54e`. Reconciled that newer main into `b503c67d`; no host extraction policy was weakened.

Complete Java 17 package and fresh root-test compilation pass. Actual packaged TCP host/join/evidence/identification/ritual/settlement passes. Reconciled desktop campaign passes physical mode acknowledgement, refused legacy extraction, actual van control, durable one-time result and same-window return. Native stale-publication protection, indexed/perspective picking and real perspective mouse/history/save/reopen/publication checks pass. Gallery has 2,307 indexed PNGs. Logs and exact source references are retained under `verification/unification-2026-11-03/`.

Prior map-source `d4294b4c` runs 41322252644 (GPU), 41322260127 (full multiplayer) and 41322265955 (room audit) are terminal SUCCESS. Raw logs prove the saved editor terrain-hole and persisted USER fixes. That older full run still has advisory 260 passed / 12 failed / 25 unexecuted of 297, and Church swept traversal still fails; later editor recovery records have their own newer results. Do not transfer old counters or map acceptance to the unified source. Next: verify unified remote/main publication and exact-source checks, then resume the complete roster rough batch, starting with actual Prison cell/gallery/service construction decisions. Full map goal remains active.


## Unification published, roster roughing resumed — 3 November

PR #58 merged successfully. Remote main and rebuild branch were independently read back at `127bead2e3a519f2c985701d5a37fcd86d8fc2ee`; local main and the active rebuild checkout were fast-forwarded to that same commit. This completes the owner's unify/merge/publish-first instruction. Main's exact-source full multiplayer 41672923465, installer 41672923313, GPU 41672923389, native editor 41672923352 and map audit 41672923322 are live at last poll. Gallery index 41672923226 passes; Pages 41672915772 is queued. Preserve these handles and inspect actual terminal results rather than launching duplicates.

Resumed S02 with Prison: 98 actual architectural-space reservations and 112 connections. Thirty-two independent ordinary cells plus two isolation cells, cell-door galleries, two separated flights per wing, shared showers/changing, public/custody visitor separation, real intake/search/holding/records/guard functions, medical/food-service separation and underground plant/laundry now have explicit measured locations. The fifth stair serves the basement, whose full editor hole is reserved but not authored. Source-sized cell fixture/bed fit is checked; compact camp-bed bounds are a size reference only, with a fixed steel bed derivative still required. Planning SVG/PNG and five stair sections are generated from one authority. No runtime promotion or stage acceptance is granted.

Next: inspect merged-main CI and Church's still-open traversal issue against current source, then continue the entire roster rough batch (Mansion, Farmhouse, Asylum, Academy, College, Church, Castle and remote/annex packets). Finish all 29 rough decisions before general blockout; House remains the proven construction-method pilot. Full map goal remains active.


Merged-source follow-up: installer 41672923313 and native editor 41672923352 are terminal SUCCESS; Pages 41672915772 and gallery index 41672923226 also SUCCESS. The deployed gallery returns HTTP 200 and its actual basement-terrain cutaway PNG exactly matches the committed `127bead2` bytes (record under `verification/unification-2026-11-03/deployed-gallery.json`). GPU 41672923389, full multiplayer 41672923465 and complete room audit 41672923322 remain live at last read. These successes do not accept the complete roster.

The new Prison rough and labeled PNG are being checkpointed on the rebuild branch after unification. They remain subsequent S02 work rather than a new runtime release; their gallery deployment follows the next accepted batch integration. Full map scope and all unaccepted stage gates remain intact.


## Editor-agent use verified — 3 November

Owner asked that Editor Agent Services be useful to the map rebuild. Inspected the actual mission host over `EditorAgentCli` in a dedicated project home. Church inspection exposes INSPECT/TRANSACT/UNDO/REDO/SAVE/REOPEN/RECOVER_PREVIOUS/CAPTURE/TEST, but its only mutation operation is `move-prop`; its only test is `mission-runtime-parity`. Executed that test against the native session: all 32 current Church props and their colliders match the actual runtime. Captured and visually inspected the real `SceneRenderer.drawPractice` output through the service, retained in `captures/editor-service/`. No prop was moved merely to demonstrate an API.

This service is useful immediately for fit-out edits, native parity, journals/recovery and real render evidence. It does not yet expose architecture, stairs or terrain edits, and its parity test does not certify traversability. Those structural operations must be connected to the same human-editor/native authority when the rebuild needs them; do not mislabel Java/rough-plan edits as service transactions.

Merged main 127bead2 GPU41672923389, full41672923465 and room audit41672923322 are now terminal SUCCESS. Raw full transcript executes all315 tests: 312 pass and three fail (GhostSpriteAtlasIntegrationTest, TwinBodyVisualTest, LanWorldControlsTest). Raw GPU proves all11 map types including persisted USER. Actual map audit still reports Church traversal failure. Local reproduction reaches -3..+3 but cannot enter rooms8/9/10 (nine room/socket goals,159429 states). LanWorldControlsTest locally reproduces obsolete ACCEPT|EXTRACT expectation after entering VIEW3D. Next: use the service for supported native inspection/evidence while fixing actual Church upper-opening authority and the stale physical-extraction control test; preserve art golden tests until their source changes are inspected. Then continue the entire roster rough batch. Full map goal remains active.


## Native editor service now checks physical routes — 3 November

Implemented mission-open-routes on the actual LevelEditorAgent used by the editor and CLI. It shares the runtime swept-capsule flood with AuthoredLevelReachabilityTest, clones door state without touching live state, and preserves persisted editor terrain surfaces. This is an open-door goal check, not hunt/interaction/complete-map acceptance. Supported mission mutations remain move-prop; architecture and terrain punching are not falsely presented as agent transactions.

DEV-232 repairs the reproduced Church failure: contained choir-loft doors now penetrate the tall parent nave wall in rendering and physics, and the tower threshold moves past the stair guard. Actual packaged regression checks closed/open player and optical collision, crouch, rendered aperture, solid jamb/lintel/lower wall, all15 room goals and functional sockets. The actual service reports native parity and169097 reached positions, Y-3..6. House saved editor terrain holes and filled-ground negative controls remain passing, with61328 physical pilot route positions. Actual door captures are in captures/church-upper-openings; detailed logs/service response in verification/church-openings-2026-11-03. No stage is promoted.

Next: finish the complete roster rough batch and use this service diagnostic at map checkpoints; retain outstanding physical van shortcut test correction and both art-golden diagnoses. Full map rebuild remains active.

All nine selectable maps pass the shared physical route check. Final packaged Church regression passes; no map acceptance is inferred.
