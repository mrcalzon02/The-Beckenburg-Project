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
- Corrected three recovery issues exposed by the baseline: the Matthis placement assertion's self-collision, Rowan's actual intersection with the network chair, and `MissionSites.title("USER")` throwing before a user project is selected. Player collision and fixed campaign ordinals remain intact. See DEV-167–170.

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
