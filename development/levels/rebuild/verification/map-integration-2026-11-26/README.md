# Map integration checkpoint — 26 November 2026

The saved roster work has been reconciled with newer main behavior and checked in a combined local build. **The current Northwatch packet completes a continuous ordinary packaged TCP case, and all 28 public investigation sources pass the supported-camera render smoke.** Castle/House native source, recovery, route, encounter and lighting checks also pass within the scopes below. The final settled-door capture, ordinary packaged full multiplayer round and staged gallery verification also pass.

This is an integration milestone. **No complete-map stage is newly accepted.** The [original 29-map diagnostic](../../../MAP_DEVELOPMENT_DIAGNOSTIC_2026-11-26.md) remains pinned historical evidence; its construction and remaining-map obligations still guide subsequent work.

## Source and build identity

| Input or artifact | Identity |
| --- | --- |
| Main input | `9f1cf5e2b5535b2ded46eba07d55c78f61211775` |
| Saved roster candidate | `d9f706f44984aaf05cadaf0911eb058880056bb2` |
| Common base | `7c4fe3be13e9f330aeaf554002ac0df97dc1ea71` |
| Tested ordinary game JAR | `f83b71c540f1c195be03ea396f8f2d7349920dd93951cb4ba9755452a69c5237` |
| JAR size | 450,123,048 bytes; build artifact, not committed |

The input SHAs alone do not identify the repaired merge. The enclosing commit and the [source-file digest receipt](checks/source-files-sha256.json) identify this checkpoint. [Build and check commands](checks/build-and-check-commands.json) preserve the actual Java17 invocation and package layout. Production source/JAR content was held stable after the Java 17 build while verification-harness retests ran.

Java 17 production compilation and **all 709 test-source compilations** passed. That count describes compilable test sources, not 709 executed tests. The [resource receipt](checks/resource-manifest-verification.json) records all **101 native packet resources** matched byte-for-byte in the ordinary `InvestigationGame` JAR. Compiler logs and the source list are retained under `checks/`.

**Local package limitation:** the tested `f83b71c5…` JAR contains a **zero-byte WLD-06 mission-board thumbnail** at `org/beckenburg/mission-board/wld-06.png`, caused by local sparse resource staging. The committed source PNG is unchanged and has been restored to 629,117 bytes, Git blob `60ed0033dd935d970c7f9f9be200cdd4cfadf3eb`. The [limitation receipt](checks/local-package-thumbnail-limitation.json) records both facts. All101 native packet comparisons and the actual case results remain valid, with their original JAR bindings unchanged. Exact-commit CI must verify normal complete packaging and UI/installer operation; this local JAR is not that proof.

## What the retained evidence establishes

| Area | Current result and scope | Principal evidence |
| --- | --- | --- |
| Castle | Supported gate exit and invalid/blocked-pose negatives; native backup, recovery and snapshot contracts pass locally | [Gate retest](checks/CastleHomeInteractionTest-capture-validation.log), [upgrade](checks/CastleRebuildUpgradeTest.log), [recovery](checks/CastleDocumentRecoveryTest.log), [snapshot](checks/CastleSnapshotContractTest.log) |
| House source and editor | Fresh rebuilt default, original-save isolation, three-way migration, custom deltas/rollback, parcel and actual porch/van movement pass | [Default](checks/HouseDefaultPromotionTest.log), [migration](checks/HouseRebuildUpgradeTest.log), [routes](checks/DevMatchNavigationTest.log), [editor retest](checks/EditorAgentServiceTest-after-kenney.log) |
| House encounter | Authored train sequence, solo photo negatives, four-client ownership/reconnect/abort contracts pass | [Solo retest](checks/HouseEncounterSessionTest-retest.log), [TCP encounter](checks/HouseEncounterNetworkTest.log) |
| Lighting and atmosphere | Main portal/optical behavior retained; parallel and cached sanctuary shading receives the current immutable atmosphere policy | [Portal parity](checks/PortalLightReachabilityTest.log), [optical checks](checks/OpticalEvidenceTest.log), [haze parity](checks/SafeAreaHazeColumnsTest.log) |
| LAN and catalogue | Ordinary protocol, source selection, movement/clock checks pass; all 28 public sources render from supported authored cameras | [28-site receipt](checks/catalogue-supported-camera-results.json), [final log](checks/SiteSelectionTest-supported-cameras-final-1024m.log) |
| Northwatch | One current-resource, ordinary packaged, continuous three-peer case completes with `BANISHED` and payout | [Case transcript](checks/northwatch-current-case.tsv), [source/JAR binding](checks/northwatch-current-case-binding.json) |
| Porch pictures | Four real 960×600 settled-state captures pass in 8.75s; original first animation frame retained separately | [Final capture log](checks/HousePorchVisualCaptureTest-final.log), [capture hashes](checks/capture-files-sha256.json), [preserved initial frame](captures/house-porch/diagnostic-first-frame-after-open-command.png) |
| Gallery | Final staged generation and check retain15,302 historical PNGs and nine new diagnostics | [Gallery receipt](checks/gallery-verification.json) |
| Packaged multiplayer | Complete real host/join/evidence/hypothesis/ritual/settlement round passes in 111.16s; post-round actions rejected | [Terminal full-round log](checks/BeckenburgFullRoundE2ETest-final.log), [final batch receipt](checks/final-targeted-results.json) |
| Native model/material retention | GLB source bytes, hierarchy/pivots/materials, PNG/UV bindings, edit history and export/reimport checks pass; portal-flux cache checks pass | [GLB](checks/EditorGlbAuthoringTest-final.log), [materials](checks/EditorMeshMaterialTest-final.log), [portal flux](checks/PortalFluxCacheTest-final.log) |

These LAN receipts use same-build protocol 3 peers; older JOIN/RESUME clients are deliberately rejected.

The catalogue smoke required at least 20 distinct colours and more than 5% actual geometry coverage. The final 1024 MB run passed all 28 sources, with a minimum of 39 colours and 44,719 physical pixels out of 64,000. This proves source dispatch and a supported rendered view; it does not certify full routes, native replacement of legacy layouts, or completed maps.

### Northwatch current-source proof

The ordinary packaged case uses GLB `2a91ae44128c2b1f583863ff6d4640c237bf5117d041283e9f85367ab882c190` and HCS2 `13c838555e063dccbfb22f07431d864ef42e05a16e1d179939ca767a0427d909`. The validator checks all five current packet files, including reused archives, against current build inputs and the SOURCE manifest.

The new receipt covers the cargo ramp, courtyard and archive doors/evidence; three refuges at two depths while standing and crouched; the service tunnel down to −2.5 m and back; and the ordinary configured 900-tick rite. All three peers receive `BANISHED`, 506 credits and 148 XP. The host clock and gameplay duration were unchanged; the old harness deadline was corrected to wait for the real configured rite. This is functional completion of this scoped case, not round-duration tuning.

The case transcript SHA-256 is `4a80c274631b9dbbb115470f3ee1bfc26d65e71339c8f3d27b6af490871e3f1a`. Both transcript and terminal-log hashes match their binding receipt. The historical Northwatch case remains untouched. This supplies one current-source replacement for the six receipt gaps recorded by DEV-747; five others still need current-source replays.

## Repairs and retained failed evidence

| Record | Classification | Change or remaining obligation |
| --- | --- | --- |
| DEV-797 | Production integration | Pass immutable `LevelAtmosphere.Policy` into parallel/cached sanctuary shading, preserving ray/opacity/column caches and depth. Held-clock weather changes and the parallel path pass parity checks. |
| DEV-798 | Verification resource authority | Validate the exact current five-file Northwatch packet instead of stale historical hashes; preserve older receipts. |
| DEV-799 | Verification camera/cleanup | Use supported map-specific camera poses and real geometry coverage; preserve recursive temporary-home cleanup. Final 28-source retest passes. |
| DEV-800 | Evidence preservation | Prevent sparse gallery generation from dropping committed images or video attachments; reuse only exact parent path/blob metadata or hash actual bytes. |
| DEV-801 | Verification contracts, partially open | Match current photo/hypothesis semantics and Northwatch's real rite deadline. Seven sibling case tools still need deadline repair and exact-source replay. |
| DEV-802 | Capture harness | Wait for the actual settled door pose and require distinct open/closed pixels. Original first-animation-frame image retained; final four-image retest passes. |

Original failures are preserved under their original filenames. The first Castle/House/editor failures lacked locally generated Kenney assets; distinct post-asset retests passed. The initial House photo assertion, Northwatch response/deadline timeouts, Elara unsupported camera, and catalogue SIGKILL/heap-limited attempts remain available. A filename containing `final` can still identify a failed historical attempt: use the explicit current-result table and `checkpoint.json`, not the filename alone. The incomplete first settled-porch attempt has its own `-incomplete-snapshot.log` name.

The defect registry keeps all 789 reconciled historical identities and 136 verbatim historical ledger headings, and adds six new records DEV-797–802 for 795 unique canonical records. Old references and failed evidence are not rewritten into passing results.

## Resume work

1. **Verify the published checkpoint:** run and inspect exact-commit CI after publication, including correct normal packaging of the restored WLD-06 thumbnail. Local source/resource checks, final porch capture and staged gallery verification are complete; installer/desktop and hardware evidence still need their own terminal results.
2. **Close Castle/House end to end:** perform the installed solo and host/guest Castle → case → results → Castle loop, including real gate crossing/retreat, profile/loadout persistence, native save/reopen/undo and current lighting/material inspection. [Castle acceptance](../../castle/acceptance.md) remains authoritative for its outstanding gates.
3. **Repair the seven remaining case harness deadlines:** Greywater, Mine, Blackwater, Monastery, Red Mesa, Mourning Marsh and Asylum still use historical 15-second result waits. Derive the bound from each live mission's required ticks at the unchanged host clock, then replay the exact final packaged sources. Do not relax result, location, witness or payout checks.
4. **Refresh remaining current-source case evidence:** Red Mesa, Blackwater, Mourning Marsh, Gallows and Greywater retain the other five final-collision receipt gaps from DEV-747. Preserve every older receipt and label new ones with actual packet hashes.
5. **Continue unfinished native integration:** work through the gated/missing institutional runtime/editor adapters and map-specific construction/acceptance obligations in the 29-map diagnostic. Keep the 40-accessible-space cap and existing 22 campaign ordinals. Do not postpone maps for the entire generic editor roadmap.

The ordinary packaged multiplayer gate passed locally. Required exact-commit CI and Windows desktop/installer/updater gates remain separate from these local checks. Physical GPU/input/frame-tail and visual review remain open. Headless software captures and frame timings here do not establish hardware FPS or complete-map acceptance; visual diagnostics do not block an otherwise functional unrelated release.

## Evidence integrity

[The machine-readable checkpoint](checkpoint.json) separates scoped passes from open acceptance. [The evidence manifest](checks/evidence-files-sha256.json) hashes the retained raw logs and receipts. Original failed attempts are kept unchanged; newer results use distinct files. The source snapshot fingerprint is `a2e0119a7693c05b23065f8de8b6afbe287333d9b896f107b6ba26f48e804d61`. No JAR, installed application or downloaded dependency is committed in this evidence directory.
