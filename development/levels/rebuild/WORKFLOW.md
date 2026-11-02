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
