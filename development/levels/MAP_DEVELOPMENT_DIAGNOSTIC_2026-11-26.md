# Map development diagnostic — 26 November 2026

**Finding:** all 29 location packets still have unfinished construction, integration or final acceptance obligations. The large partial implementation exists and is committed: much of it is on the held `map/roster-structure-batch` branch, not current `main`. Preserve and reconcile that work before building replacements.

**Audited main:** [`771b55ae652c12a43aeacfbe65d55c3ffc1bc57c`](https://github.com/mrcalzon02/The-Beckenburg-Project/commit/771b55ae652c12a43aeacfbe65d55c3ffc1bc57c)  
**Audited map candidate:** [`d9f706f44984aaf05cadaf0911eb058880056bb2`](https://github.com/mrcalzon02/The-Beckenburg-Project/commit/d9f706f44984aaf05cadaf0911eb058880056bb2)  
**Candidate review:** [draft PR #86](https://github.com/mrcalzon02/The-Beckenburg-Project/pull/86), based on the held `map/house-default` branch. The candidate tip records a user-requested halt; this audit does not lift the implementation/integration hold.  
**Scope:** 28 base locations including Castle headquarters, plus the cemetery/mortuary annex: 29 packets, or 28 investigations and one headquarters.  
**Audit type:** source and recorded-evidence review, including SHA-256 of six actual committed HCS2 collision files. No new game launch, runtime route, multiplayer, hardware performance or visual acceptance is claimed.

## What exists and what is unfinished

| Layer | Current main | Committed map candidate | Diagnostic meaning |
|---|---|---|---|
| Ordinary investigations | Nine built-in choices; Blackpine/Hollow Creek remain developer/prototype sources | 28 investigation choices, all WLD-01–18 and Mortuary included | Catalog integration is largely done on the candidate, not delivered on main |
| Rebuilt institutions | Existing legacy/shared sources | Native Hospital, Prison, School and College exist behind four review properties | Complete native promotion and saved/editor migration after per-map work |
| Four other rebuilds | Legacy Academy/Manor/Farm/Parish | New native construction packets, but no replacement runtime adapter | Concrete integration work remains; source construction is not absent |
| Castle and Briar House | Rebuilt HQ and fresh-default furnished House already integrated | Additional House encounter/porch/parcel work plus divergent Castle changes | Preserve both histories; do not redo existing default promotion or native migration |
| Complete-map certification | No accepted stage in the main register | No accepted stage in the candidate register | Tracking is stale; no finished-map claim is supported by those registers |

Source: [main catalog](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/771b55ae652c12a43aeacfbe65d55c3ffc1bc57c/src/main/java/org/beckenburg/SiteCatalog.java), [candidate catalog](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/SiteCatalog.java), [candidate runtime selection](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/LevelPlans.java). Catalog PLAYABLE validates a buildable set of stable gameplay owner zones, not full map completion. Fifteen ownership zones do not mean fifteen architectural rooms.

## Shared work to resume first

### P0 — Reconcile the saved work and its evidence

1. **Reconcile the held map candidate against current main when the map integration hold is lifted.** It is 335 commits ahead and 228 behind the audited main in Git ancestry. These counts are not a count of unique features. The candidate lacks main’s `PortalLightReachability.java`, retains the old Castle exit predicate, and differs in fixture lighting, House and Castle scene/editor code. A whole-tree replacement would discard newer main fixes. Preserve the current movement cadence, lighting optimization, van behavior and saved custom sources.
2. **Repair the map status and defect cross-references.** Main’s stage register is dated 7 November; the candidate’s date field is 14 November. Both contain 348 unaccepted stage records while later checkpoints document native missions and completed cases. This audit found 38 shared DEV identifiers with different title sets across the two branches. Main contains 10 duplicated DEV identifiers and the candidate 13; eight conflicting title pairs are already identically duplicated in both. Preserve immutable historical references and establish a remapping before combining ledgers; do not overwrite one issue with another.
3. **Refresh evidence that predates the final geometry.** Six retained full-case collision hashes differ from the actual candidate HCS2 bytes: Red Mesa, Northwatch, Blackwater, Mourning Marsh, Gallows Pass and Greywater. All six current manifests match the actual committed binaries. These are valid historical successes with stale source coverage, not proof of current gameplay failure. Re-run the relevant cases on the final integrated source before marking them accepted.
4. **Complete native editing/save parity.** The ordinary mission editor excludes the WLD roster and Mortuary through `editableTemplate()`. The four review-enabled institutions still create editor sessions from legacy `buildSource()`. Add the specific native document adapters needed for each map; unrelated G0–G12 editor work is not a reason to postpone the maps.

Sources: [runtime/editor template policy](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/LevelPlans.java), [ordinary editor selection](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/DeveloperLevelEditor.java), [editor source and saved-document authority](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/LevelAuthoringSession.java), [main light reachability](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/771b55ae652c12a43aeacfbe65d55c3ffc1bc57c/src/main/java/org/beckenburg/PortalLightReachability.java), [main exit guard](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/771b55ae652c12a43aeacfbe65d55c3ffc1bc57c/src/main/java/org/beckenburg/CastleGateExit.java). The detailed branch and digest measurements are preserved in the companion JSON.

### Evidence that must not be turned back into unfinished implementation

- Fresh Briar House default promotion is already on main. Historical versionless saves deliberately retain their old building. Candidate train-encounter code exists and needs integration, not reinvention.
- Castle saved-native upgrade, story interactions and loose objects already exist. Its open work is current whole-location acceptance and remaining presentation/interaction findings.
- The old “122 incomplete flat links” count is superseded by a later recorded native sweep: 25 staged packets, 140,646 standing samples with zero failures, 567 ordinary links, 238 stair flights and 86 vertical routes. This is scoped historical source evidence, not final runtime or all-map certification.
- The later native packet files already contain Gallows room lighting; Morrow’s beacon/keeper services; Elara’s nine room lighting groups/bilge pump; Pines’ twelve groups/washer/register; and Cinder’s twelve switches, 26 lights and staging enclosure. Older workflow prose understates those installations. Their operation and final case coverage still require verification.
- Keeping 22 saved campaign chapters is intentional. WLD-14–18 and Mortuary are Solo/LAN locations with Castle board pins; adding campaign chapters is not an audit fix.

Sources: [House source selection](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/771b55ae652c12a43aeacfbe65d55c3ffc1bc57c/src/main/java/org/beckenburg/HouseSourceVersions.java), [existing candidate encounter](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/HouseEncounter.java), [Castle acceptance](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/771b55ae652c12a43aeacfbe65d55c3ffc1bc57c/development/levels/rebuild/castle/acceptance.md), [later native route sweep](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/seacliff-receiving-pass/checkpoint.json), [county board and free-play entries](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/CountyCampaignMapPanel.java).

## Complete diagnostic resume list

**Priority key:** P1 = most useful next closeout candidates after shared reconciliation; P2 = concrete integration, room-function or full-case work; P3 = larger remaining environmental/services work. Every row also inherits the common completion gates below. Priorities are a proposed work order, not completion percentages.

### Headquarters and reference house

#### CASTLE — Castle Beckenburg (P1)

**Main:** Rebuilt headquarters on main. **Candidate:** Rebuilt headquarters; divergent scene/editor changes.

**Preserve:** 42 authored spaces; 39 accessible and three sealed; stations, residents, cellar, native migration, story and loose objects exist. Main has the newer supported-ground exit guard.

**Remaining:**

- Current physical inspection of all 39 reachable spaces, three permanent seals, stairs and guard openings.
- Save/reopen/undo and custom-source migration; all nine stations and five residents; complete solo and multiplayer Castle → case → results → same Castle persistence.
- Installed gate crossing/return and remaining ordinary-view lighting, fixture, sprite/UV, material and movement review.

**First resume task:** Use the existing main Castle acceptance sequence to close one exact-source headquarters milestone; preserve the newer main gate and lighting behavior during branch reconciliation.

**Evidence:** [castle/acceptance.md — main](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/771b55ae652c12a43aeacfbe65d55c3ffc1bc57c/development/levels/rebuild/castle/acceptance.md); [beckenburg/CastleRebuildValidation.java — main](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/771b55ae652c12a43aeacfbe65d55c3ffc1bc57c/src/main/java/org/beckenburg/CastleRebuildValidation.java); [beckenburg/CastleGateExit.java — main](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/771b55ae652c12a43aeacfbe65d55c3ffc1bc57c/src/main/java/org/beckenburg/CastleGateExit.java).

#### HOUSE — The Briar House (P1)

**Main:** Fresh installations already use the rebuilt furnished house; old versionless saves retain their historical source. **Candidate:** Adds the authored train encounter, corrected porch, measured parcel boundary and window-host replay.

**Preserve:** Default promotion and generation-aware saved-source behavior are already on main. Candidate adds HouseEncounter and its host/TCP integration, 66 physical parcel panels, 396 blocked boundary approaches and a recorded window-host correction.

**Remaining:**

- Reconcile candidate encounter, porch and parcel work with current main lighting/movement changes.
- Verify fresh, historical and custom saved houses, including encounter eligibility and migration retention.
- Whole-location doors/windows/circuits/portable-light, supported props, evidence/privacy, solo/LAN return and installed movement/visual closeout.

**First resume task:** Integrate the existing candidate House improvements with current main; rerun the affected saved-source and complete-case checks rather than rebuilding the house or repeating default promotion.

**Evidence:** [beckenburg/HouseSourceVersions.java — main](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/771b55ae652c12a43aeacfbe65d55c3ffc1bc57c/src/main/java/org/beckenburg/HouseSourceVersions.java); [beckenburg/UserLevelLibrary.java — main](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/771b55ae652c12a43aeacfbe65d55c3ffc1bc57c/src/main/java/org/beckenburg/UserLevelLibrary.java); [beckenburg/HouseEncounter.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/HouseEncounter.java); [house-parcel-boundary-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/house-parcel-boundary-pass/checkpoint.json); [house-window-network-2026-11-18/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/verification/house-window-network-2026-11-18/checkpoint.json).

### Existing institutions and estates

#### ACADEMY — St. Alban Academy (P2)

**Main:** Legacy shared wing runtime. **Candidate:** New construction source exists; no Academy native mission adapter.

**Preserve:** Authored campus/terrain/structural and stair work is committed in the native construction roster. Later aggregate route checks supersede the old off-landform/122-link snapshot.

**Remaining:**

- Finish roof flashing/drainage and structural joins; inspect actual terrain/foundation and standing-height optical finish. Existing first-pass furnishings do not establish complete room capacity.
- Bind the authored campus to real runtime room ownership, portals, acoustics, spawns, utilities and canonical van.
- Complete functional room fit, services, evidence/hiding/ritual and location-specific encounter work on that source.
- Native editor save/reopen and full location acceptance.

**First resume task:** Inventory the final authored campus packet and implement its native mission/authoring adapter, then inspect actual gaps on the loaded geometry.

**Evidence:** [beckenburg/LevelPlans.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/LevelPlans.java); [academy/construction.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/flush-porch-and-threshold-pass/academy/construction.json); [greywater-native-pass/roster.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/greywater-native-pass/roster.json).

#### ASYLUM — St. Dymphna Hospital (P1)

**Main:** Legacy shared wing runtime. **Candidate:** Native hospital behind beckenburg.review.asylumNative.

**Preserve:** Fitted admission/interior doors, two stair systems, three refuges, power, founded enclosure and a retained continuous TCP ABSOLVED case.

**Remaining:**

- Final garden planting/materials and directional skyline.
- Other rite-specific full cases and authored encounter.
- Native default promotion, saved editor migration and installed acceptance.

**First resume task:** Finish the small remaining environment/encounter scope and verify the reconciled native case before promoting it from the review property.

**Evidence:** [beckenburg/AsylumNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/AsylumNativeLevel.java); [asylum-enclosure-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/asylum-enclosure-pass/checkpoint.json); [asylum-enclosure-pass/case.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/asylum-enclosure-pass/case.tsv).

#### CHURCH — St. Brigid Parish (P2)

**Main:** Existing church runtime. **Candidate:** Rebuilt architectural source remains separate from ordinary runtime.

**Preserve:** Household/parish structural bays, roof enclosure, stair routes and terrain are committed; the established church runtime remains available.

**Remaining:**

- Finish roof flashing/drainage and structural joins; inspect actual terrain/foundation and standing-height optical finish. Existing first-pass furnishings do not establish complete room capacity.
- Connect the rebuilt parish to shared native gameplay and ordinary editor persistence.
- Audit the final tower/crypt/upper-floor geometry, functional service/prop placement and optical containment in the actual map.
- Working evidence, rites, encounters and full location acceptance.

**First resume task:** Bring the preserved rebuilt church source into a native mission adapter; assess remaining construction from the final packet instead of old rough-route failures.

**Evidence:** [beckenburg/LevelPlans.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/LevelPlans.java); [church/construction.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/native-foyer-door-pass/church/construction.json); [greywater-native-pass/roster.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/greywater-native-pass/roster.json).

#### COLLEGE — Beckenburg College (P2)

**Main:** Legacy shared wing runtime. **Candidate:** Native college behind beckenburg.review.collegeNative.

**Preserve:** Doors, perimeter and well guards, fitted power, plumbing/heating/ventilation, plus sixteen residence beds/study stations/wardrobes are recorded.

**Remaining:**

- Finish teaching, library, café and other ordinary-use workrooms; usable evidence/hiding arrangements and final surfaces.
- Resolve/recheck the furnishing-specific regression recorded by the dispatch review.
- Continuous complete case, native promotion, saved-editor migration and installed acceptance.

**First resume task:** Inspect the dispatch checkpoint and furnish the remaining functional rooms while preserving the already fitted services; then complete a continuous case.

**Evidence:** [beckenburg/CollegeNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/CollegeNativeLevel.java); [college-services-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/college-services-pass/checkpoint.json); [college-dispatch-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/college-dispatch-pass/checkpoint.json).

#### FARMHOUSE — Hollowmere Farm (P2)

**Main:** Existing farm runtime. **Candidate:** New authored farm construction is not the runtime default.

**Preserve:** Rebuilt domestic structural bays, roof/terrain and circulation packets exist alongside the established farm implementation.

**Remaining:**

- Finish roof flashing/drainage and structural joins; inspect actual terrain/foundation and standing-height optical finish. Existing first-pass furnishings do not establish complete room capacity.
- Runtime/editor binding for the new structure, with coherent room/portal/circuit/spawn/van coordinates.
- Inspect room purpose, physical supports, upper/basement openings, boundaries, utilities and prop fill on the final geometry.
- Evidence/rites/encounters and complete saved solo/LAN location acceptance.

**First resume task:** Implement the native adapter from the current construction packet and compare it with the established playable farm before replacing any source.

**Evidence:** [beckenburg/LevelPlans.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/LevelPlans.java); [farmhouse/construction.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/measured-arrival-and-approach-pass/farmhouse/construction.json); [greywater-native-pass/roster.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/greywater-native-pass/roster.json).

#### MANSION — Blackthorn Manor (P2)

**Main:** Legacy shared wing runtime. **Candidate:** New manor construction exists; no native Mansion mission adapter.

**Preserve:** Estate rooms, structural/roof/terrain and circulation construction exist in the branch; normal dispatch still uses the generic wing.

**Remaining:**

- Finish roof flashing/drainage and structural joins; inspect actual terrain/foundation and standing-height optical finish. Existing first-pass furnishings do not establish complete room capacity.
- Bind the manor construction to the actual mission and native authoring/save path.
- Finish and review room services, supported functional furnishing, privacy/optics and site story/encounter.
- Prove full arrival, vertical routes, investigation and return on the integrated source.

**First resume task:** Create the manor native mission/authoring integration from the existing construction rather than iterating the shared placeholder wing.

**Evidence:** [beckenburg/LevelPlans.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/LevelPlans.java); [mansion/construction.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/interior-stair-landing-pass/mansion/construction.json); [greywater-native-pass/roster.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/greywater-native-pass/roster.json).

#### PRISON — Beckenburg Penitentiary (P2)

**Main:** Legacy shared wing runtime. **Candidate:** Native prison behind beckenburg.review.prisonNative.

**Preserve:** Vehicle/yard sally gates, plant and circuits, supported three-cupboard hiding, packaged solo/four-peer service and route checks.

**Remaining:**

- Remaining intake and west-cellhouse custody closures/security routes.
- Complete evidence, ritual, rescue, extraction and progression; room containment and story.
- Native promotion/save-editor parity and current installed operating review.

**First resume task:** Finish the actual custody doors and security path, then run a continuous native investigation through the existing plant and refuge installation.

**Evidence:** [beckenburg/PrisonNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/PrisonNativeLevel.java); [prison-hiding-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/prison-hiding-pass/checkpoint.json).

#### SCHOOL — Ravenshade School (P2)

**Main:** Established school runtime. **Candidate:** Native school behind beckenburg.review.schoolNative.

**Preserve:** Forty authored spaces map onto fifteen stable gameplay owners; twenty fitted doors, twelve circuits, 26 loads, twelve alarm sockets and three cupboards are recorded.

**Remaining:**

- Room-by-room optical containment and remaining services/functional fit.
- Continuous evidence/ritual/rescue/progression case and ordinary dispatch promotion.
- Native editor/save parity and representative/worst-case installed operating review.

**First resume task:** Run the entire rebuilt school case after closing room-level service/optical gaps; retain the measured doors, assembly partition and electrical work.

**Evidence:** [beckenburg/SchoolNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/SchoolNativeLevel.java); [school-electrical-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/school-electrical-pass/checkpoint.json).

### Remote and additional investigations

#### WLD-01 — Blackpine Logging Camp (P2)

**Main:** Substantial developer runtime source, not publicly dispatchable. **Candidate:** Regular Solo/LAN choice using the existing camp and mill source.

**Preserve:** Mill/loft and authored camp; founded generator/start and independent breaker; twelve room circuits/28 loads and packaged service review. Main already contains the scoped loft rail fix.

**Remaining:**

- Individual evidence, boundary, dressed-space and mill editor save/reopen inspection.
- Full uninterrupted investigation and current physical renderer/performance review.
- Reconcile public dispatch and final location publication evidence.

**First resume task:** Retain the built camp and power work; complete a normal full case and recheck the mill source/editor parity rather than redoing fixed railings.

**Evidence:** [beckenburg/BlackpineLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/BlackpineLevel.java); [blackpine-dispatch-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/blackpine-dispatch-pass/checkpoint.json); [quality/defects.json — main](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/771b55ae652c12a43aeacfbe65d55c3ffc1bc57c/development/quality/defects.json).

#### WLD-02 — Camp Hollow Creek (P2)

**Main:** Native terrain/physics/runtime prototype, catalog-locked. **Candidate:** Regular Solo/LAN choice using the native camp.

**Preserve:** Authored terrain, elevated canonical van, shoreline/water and supported camp assemblies; source/breaker controls and native runtime bridge exist.

**Remaining:**

- Host ghost-breaker disturbance and source/breaker independence during a complete case.
- Water/shore containment, source editing/persistence, final normal-view camp/amenity review.
- Current native GPU operation and installed solo/LAN case acceptance.

**First resume task:** Verify the branch public dispatch and play a complete case through the actual shoreline, ramp, source and breaker interactions.

**Evidence:** [hollow-creek-dispatch-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/hollow-creek-dispatch-pass/checkpoint.json); [beckenburg/HollowCreekLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/HollowCreekLevel.java); [beckenburg/SiteCatalog.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/SiteCatalog.java); [rebuild/WORKFLOW.md — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/WORKFLOW.md); [levels/DEVELOPMENT_HANDOFF_2026-10-25_WATER_RUNTIME.md — main](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/771b55ae652c12a43aeacfbe65d55c3ffc1bc57c/development/levels/DEVELOPMENT_HANDOFF_2026-10-25_WATER_RUNTIME.md).

#### WLD-03 — Red Mesa Ancestral Caves (P3)

**Main:** Catalog plan only; no dispatch. **Candidate:** Native mission in regular Solo/LAN.

**Preserve:** Saved canyon landform, +4/+8 m stairs and bridge, four operating frames, three refuges and a retained ABSOLVED case. Final source adds a generator, independent breaker, seven switches/nine lights, supported shelter furnishings and an expedition radio.

**Remaining:**

- Finish reachable perimeter and drainage; verify the already installed source, distribution and room services operationally.
- Archaeology, room use, sound/story and encounters.
- Replay final geometry: retained case collision hash predates current HCS2; remaining rites and installed acceptance.

**First resume task:** Finish canyon edge/drainage and verify the existing utilities/radio, then replay the full case against the final resource hashes.

**Evidence:** [beckenburg/RedMesaNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/RedMesaNativeLevel.java); [red-mesa-native-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/red-mesa-native-pass/checkpoint.json); [red-mesa-native-pass/runtime-input.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/red-mesa-native-pass/runtime-input.tsv); [red-mesa-native-pass/case.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/red-mesa-native-pass/case.tsv); [red-mesa-native-pass/construction.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/red-mesa-native-pass/construction.json).

#### WLD-04 — Widow's Peak Fire Lookout (P1)

**Main:** Catalog plan only; no dispatch. **Candidate:** Native mission in regular Solo/LAN.

**Preserve:** Built arrival/tower/landings, source/breaker, three refuges and records; named package checks cover camera/van feed/reconnect, rites and retreat. Later workroom furnishings and radio are also committed.

**Remaining:**

- Site-specific event and loose-object physics.
- Final environmental/containment/presentation review and current full package acceptance.
- Reconcile source and capture publication with main.

**First resume task:** Finish the authored event/loose-physics scope and close the existing named-case acceptance on the reconciled build.

**Evidence:** [beckenburg/LookoutNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/LookoutNativeLevel.java); [lookout-case-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/lookout-case-pass/checkpoint.json).

#### WLD-05 — Dead Man's Claim (P1)

**Main:** Catalog plan only; no dispatch. **Candidate:** Native mission in regular Solo/LAN.

**Preserve:** Working circuits, shaft loop, receiving slabs, handholds, runoff and guarded underground sump; named camera/rites/retreat evidence exists. A later mine phone is fitted; an additional continuous cleansing transcript is preserved.

**Remaining:**

- Dedicated atmosphere and location encounter review.
- Final independent map fitness, saved progression and installed acceptance.
- Retain the current service/shaft/water work during integration.

**First resume task:** Complete atmosphere/encounter refinement and verify the whole mine case on the reconciled package.

**Evidence:** [beckenburg/MineNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/MineNativeLevel.java); [mine-case-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/mine-case-pass/checkpoint.json); [mine-investigation-pass/case.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/mine-investigation-pass/case.tsv).

#### WLD-06 — Drowned Fisherman's Wharf (P2)

**Main:** Catalog plan only; no dispatch. **Candidate:** Native mission in regular Solo/LAN.

**Preserve:** Founded shore/quay, rebuilt regular stair cells, landward tide source, fitted doors, room power and named packaged case. Final source additionally furnishes the cottages and fits communications.

**Remaining:**

- Verify the newly furnished cottage functions; finish or demonstrate fish-house wet/cold tasks and net-drying fit.
- Loose-object physics, location encounters, material wear and final shore/containment inspection.
- Final solo, multiplayer and hardware acceptance on current source.

**First resume task:** Finish ordinary work/living functions within the already built wharf, then run the full case through tide/shore and stair routes.

**Evidence:** [beckenburg/WharfNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/WharfNativeLevel.java); [wharf-case-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/wharf-case-pass/checkpoint.json); [wharf-case-pass/construction.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/wharf-case-pass/construction.json).

#### WLD-07 — Saint Bartholomew's Forgotten Cemetery (P2)

**Main:** Catalog plan only; no dispatch. **Candidate:** Named native cemetery mission in regular Solo/LAN.

**Preserve:** Physical cemetery/ossuary routes, fitted doors, room power, supported archive records and named camera/EMF/rite/refuge checks. A later caretaker telephone is fitted.

**Remaining:**

- Detailed ordinary-use furnishings and remaining room/service functions.
- Final material, atmosphere, lore/encounter and perimeter review.
- Current progression, hardware and complete-map acceptance independently from the annex.

**First resume task:** Audit and finish actual cemetery/ossuary room use, retaining the existing named source and records.

**Evidence:** [beckenburg/CemeteryNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/CemeteryNativeLevel.java); [cemetery-named-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/cemetery-named-pass/checkpoint.json); [wld-07/construction.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/cemetery-furnishings-pass/wld-07/construction.json).

#### WLD-07-MORTUARY — Saint Bartholomew’s Cemetery & Vale Mortuary annex (P2)

**Main:** No separate mission-catalog entry. **Candidate:** Separate MORTUARY native choice and Castle board entry.

**Preserve:** Separate source identity, fitted doors/power and supported records/furnishings; named package and rite/refuge evidence for the annex. Later basin and telephone installations are committed.

**Remaining:**

- Verify complete body-preparation, cold, receiving and visitor workflows against the already fitted basin, phones and furnishings; finish the remaining functions.
- Material/atmosphere/encounter and boundary review, with independent case and save identity.
- Current full package/installed acceptance; preserve intentional Solo/LAN availability.

**First resume task:** Finish the mortuary workflow and its own full-case acceptance rather than treating cemetery checks as automatic annex acceptance.

**Evidence:** [beckenburg/CemeteryNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/CemeteryNativeLevel.java); [cemetery-named-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/cemetery-named-pass/checkpoint.json); [wld-07-mortuary/construction.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/cemetery-furnishings-pass/wld-07-mortuary/construction.json).

#### WLD-08 — Northwatch Research Station (P1)

**Main:** Catalog plan only; no dispatch. **Candidate:** Native mission in regular Solo/LAN.

**Preserve:** Founded perimeter, sixteen operating frames, complete surface/tunnel/yard power, wash-water, records and furnishings; retained BANISHED continuous case. Later crew-quarter furnishings and radio are committed after that case.

**Remaining:**

- Final finish, atmosphere and remaining location-specific encounter review.
- Campaign persistence and representative installed operation.
- Replay final geometry: current HCS2 differs from the retained continuous-case collision hash; full current package acceptance.

**First resume task:** Run the archive→array→dormitory→heated-tunnel→laboratory case on the final resources, then close the limited finish/persistence backlog.

**Evidence:** [beckenburg/NorthwatchNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/NorthwatchNativeLevel.java); [northwatch-investigation-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/northwatch-investigation-pass/checkpoint.json); [northwatch-investigation-pass/case.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/northwatch-investigation-pass/case.tsv); [northwatch-named-pass/source-hashes.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/northwatch-named-pass/source-hashes.tsv); [wld-08/construction.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/northwatch-furnishing-pass/wld-08/construction.json).

#### WLD-09 — Blackwater Hydroelectric Dam (P2)

**Main:** Catalog plan only; no dispatch. **Candidate:** Native mission in regular Solo/LAN.

**Preserve:** Rebuilt vertical route, water/headpond/carrier, perimeter, fourteen doors, refuges and ventilated power plant; retained continuous CLEANSED case to the −10 m valve gallery. Later electrical and workroom/radio additions are committed after that case.

**Remaining:**

- Verify the expanded final electrical distribution and fitted workrooms; finish or demonstrate remaining water/mechanical service operation.
- Final furnishing function, site sound, surfaces and narrative/encounter review, preserving the later room/radio installation.
- Other rites and final-source continuous replay: the retained case predates current collision bytes; installed acceptance.

**First resume task:** Audit final electrical/mechanical operation and remaining site functions, then replay the complete van-to-valve case at the current resource revision.

**Evidence:** [beckenburg/BlackwaterNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/BlackwaterNativeLevel.java); [dam-investigation-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/dam-investigation-pass/checkpoint.json); [dam-investigation-pass/case.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/dam-investigation-pass/case.tsv); [dam-named-pass/source-hashes.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/dam-named-pass/source-hashes.tsv); [wld-09/construction.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/dam-ventilation-pass/wld-09/construction.json); [wld-09/electrical-schedule.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/dam-ventilation-pass/wld-09/electrical-schedule.tsv).

#### WLD-10 — Monastery of Ashen Bells (P1)

**Main:** Catalog plan only; no dispatch. **Candidate:** Native mission in regular Solo/LAN.

**Preserve:** Fitted doors/perimeter, grounded drainage, distribution through all ranges, furnishings/loose belongings and three refuges; retained complete TCP BANISHED case. Later bell and washbasin fittings are present.

**Remaining:**

- Other two rite-specific complete cases.
- Remaining authored encounter, wall/landscape/material finish and directional scenery.
- Current installed/package and publication acceptance.

**First resume task:** Close the other rites and final encounter/finish review, preserving the already verified physical case and estate installation.

**Evidence:** [beckenburg/MonasteryNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/MonasteryNativeLevel.java); [monastery-investigation-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/monastery-investigation-pass/checkpoint.json); [monastery-investigation-pass/case.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/monastery-investigation-pass/case.tsv).

#### WLD-11 — Mourning Marsh (P3)

**Main:** Catalog plan only; no dispatch. **Candidate:** Native mission in regular Solo/LAN.

**Preserve:** Levee, seven-riser approach, continuous piled boardwalk, seven operating leaves and three refuges; retained chapel ABSOLVED case. Final source adds the working pump, generator, independent breaker, six switched lighting groups and south-boardwalk guards.

**Remaining:**

- Shared water behavior and complete physical perimeter.
- Verify current pump/electrical operation and complete room function, sound/story and encounter; do not rebuild utilities already present.
- Replay final collision source and other rites, then installed acceptance.

**First resume task:** Complete shared-water behavior and whole perimeter review, verify existing utilities, then rerun the full boardwalk-to-chapel case on current bytes.

**Evidence:** [beckenburg/MourningMarshNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/MourningMarshNativeLevel.java); [mourning-marsh-native-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/mourning-marsh-native-pass/checkpoint.json); [mourning-marsh-native-pass/runtime-input.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/mourning-marsh-native-pass/runtime-input.tsv); [mourning-marsh-native-pass/case.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/mourning-marsh-native-pass/case.tsv); [mourning-marsh-native-pass/construction.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/mourning-marsh-native-pass/construction.json).

#### WLD-12 — Gallows Pass Railway (P2)

**Main:** Catalog plan only; no dispatch. **Candidate:** Native mission in regular Solo/LAN.

**Preserve:** Station/siding/tunnel/trestle, eleven leaves, two trestle flights and three refuges. Final construction already has six switched lighting groups plus radio, telephone and signal register.

**Remaining:**

- Complete physical perimeter and final railway-specific encounter/atmosphere review.
- Verify all final room services/furnishings operationally; do not re-add already installed circuits.
- Replay the current complete case: retained ABSOLVED case uses an older collision digest.

**First resume task:** Re-run the final railway source through its actual signal/tunnel/coach/store/refuge services and containment before certifying the earlier successful case.

**Evidence:** [beckenburg/GallowsNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/GallowsNativeLevel.java); [gallows-pass/construction.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/gallows-pass/construction.json); [gallows-pass/runtime-input.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/gallows-pass/runtime-input.tsv); [gallows-pass/case.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/gallows-pass/case.tsv).

#### WLD-13 — Greywater Cannery (P2)

**Main:** Catalog plan only; no dispatch. **Candidate:** Native mission in regular Solo/LAN.

**Preserve:** Surveyed cannery, sixteen fitted doors, supported loading/gallery routes, boiler controls and three refuges; retained UV observation and ABSOLVED case. Later source expands to nine switches/17 lights, crew/workroom furnishings, a readable record and basin.

**Remaining:**

- Verify final complete case against current geometry; retained transcript uses earlier collision bytes.
- Survey final containment, services, production-room function, story and atmosphere against the map checklist; latest checkpoint has no exhaustive remaining-item list.
- Other rite coverage, saved progression and installed acceptance.

**First resume task:** Establish an exact-final-source case and enumerate the remaining room/service/finish findings in-game; do not invent an unobserved specific geometry defect.

**Evidence:** [beckenburg/GreywaterNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/GreywaterNativeLevel.java); [greywater-native-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/greywater-native-pass/checkpoint.json); [greywater-native-pass/runtime-input.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/greywater-native-pass/runtime-input.tsv); [greywater-native-pass/case.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/greywater-native-pass/case.tsv); [greywater-native-pass/construction.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/greywater-native-pass/construction.json).

#### WLD-14 — Morrow Point Lightstation (P2)

**Main:** No mission-catalog entry. **Candidate:** Native Solo/LAN choice; nonchapter Castle board entry.

**Preserve:** Keeper house, coast, six doors, +18 m tower climb and fitted rails/shaft guard. Final packet includes twelve switch entries, 21 lights including beacon, radio and washbasin.

**Remaining:**

- Verify operational beacon/tower and keeper-house services on final source; old “missing lantern fit-out” prose is incomplete.
- Complete coast/perimeter and whole-site use/encounter review.
- End-to-end current-source case, native editor/save parity and installed acceptance.

**First resume task:** Inspect and operate the existing beacon and keeper installation, then prove the final van-to-lantern case and coastal containment.

**Evidence:** [beckenburg/MorrowNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/MorrowNativeLevel.java); [morrow-point/construction.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/morrow-point/construction.json); [morrow-point/runtime-input.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/morrow-point/runtime-input.tsv).

#### WLD-15 — MV Saint Elara (P2)

**Main:** No mission-catalog entry. **Candidate:** Native Solo/LAN choice; nonchapter Castle board entry.

**Preserve:** Shore shelf, gangway/decks, seven doors, four railed flights, hatch guards and three refuges; final packet has nine switched lighting groups and a bilge pump.

**Remaining:**

- Complete hull/deck/shore containment and wet-area operational review.
- Verify installed compartment circuits, bilge pump and functional furnishings; do not list these as absent.
- Full final-source investigation, encounter and installed acceptance.

**First resume task:** Play the complete vessel case across all decks and the pump installation, checking hull limits and final electrical interactions.

**Evidence:** [beckenburg/ElaraNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/ElaraNativeLevel.java); [saint-elara/construction.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/saint-elara/construction.json); [saint-elara/runtime-input.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/saint-elara/runtime-input.tsv).

#### WLD-16 — Eight Pines Motor Lodge (P2)

**Main:** No mission-catalog entry. **Candidate:** Native Solo/LAN choice; nonchapter Castle board entry.

**Preserve:** Parking court, covered walks, fourteen doors, eight guest rooms and services; twelve switched lighting groups, three refuges, 22 furniture records, guest register and washer.

**Remaining:**

- Final physical perimeter and room-by-room functional/optical inspection.
- Location-specific encounter and complete current-source case.
- Native editing/persistence and installed acceptance.

**First resume task:** Audit the lodging loop and actual guest/manager/laundry functions, then finish its distinctive encounter and complete-case proof.

**Evidence:** [beckenburg/PinesNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/PinesNativeLevel.java); [eight-pines/construction.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/eight-pines/construction.json); [eight-pines/runtime-input.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/eight-pines/runtime-input.tsv).

#### WLD-17 — Seacliff Battery (older roster: Battery Brackenhead) (P2)

**Main:** No mission-catalog entry. **Candidate:** Native Solo/LAN choice; nonchapter Castle board entry.

**Preserve:** Founded receiving stairs, twelve door sets, plant/upper-service power, three cupboards and packaged solo/LAN service review. Both names refer to WLD-17. Final room-service additions, telephone and sump pump supersede the earlier two-room power limitation.

**Remaining:**

- Verify final expanded room circuits, telephone and sump pump; identify remaining actual service/evidence functions rather than repeating the superseded two-room limitation.
- Individual evidence installation, complete perimeter/sealed-context and architectural finish.
- Full investigation and installed acceptance; preserve one stable map identity.

**First resume task:** Verify the final battery service/evidence installation and complete boundaries, then prove one uninterrupted case through its vertical layout.

**Evidence:** [beckenburg/SeacliffNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/SeacliffNativeLevel.java); [seacliff-dispatch-pass/checkpoint.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/seacliff-dispatch-pass/checkpoint.json); [beckenburg/MissionSites.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/MissionSites.java); [seacliff-room-services/runtime.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/seacliff-room-services/runtime.tsv).

#### WLD-18 — Site Cinder-6 (P2)

**Main:** No mission-catalog entry. **Candidate:** Native Solo/LAN choice; nonchapter Castle board entry.

**Preserve:** Surface/escape stair and two guarded underground galleries, eleven doors and three refuges. Final packet has twelve switches/26 lights, workrooms, emergency telephone and a staging gate/fence enclosure.

**Remaining:**

- Whole-site perimeter/escape containment, including the partial staging enclosure.
- Operational verification of the final gallery and workroom installation.
- Full current-source case, authored encounter, native editor/save parity and installed acceptance.

**First resume task:** Verify the finished gallery circuits and all surface/escape boundaries, then run the complete investigation; do not report the final lighting or staging fence as unbuilt.

**Evidence:** [beckenburg/CinderNativeLevel.java — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/src/main/java/org/beckenburg/CinderNativeLevel.java); [cinder-6/construction.json — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/cinder-6/construction.json); [cinder-6/runtime-input.tsv — candidate](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/d9f706f44984aaf05cadaf0911eb058880056bb2/development/levels/rebuild/cinder-6/runtime-input.tsv).

## Case-evidence source mismatch register

The actual final HCS2 bytes were hashed from the committed Git blobs. Each current manifest matches those bytes; the listed historical case does not. Full hashes, paths, byte sizes and results are in the companion JSON. This check establishes evidence age, not a reproduced gameplay defect.

| Map | Final HCS2 SHA-256 prefix | Recorded case SHA-256 prefix | Next evidence action |
|---|---|---|---|
| WLD-12 Gallows Pass Railway | `7985938b07ea8512` | `f0e0659079aaa246` | Re-run final-source case; retain the historical transcript |
| WLD-13 Greywater Cannery | `13e8dcc3f44895f7` | `d398a515907a3da1` | Re-run final-source case; retain the historical transcript |
| WLD-11 Mourning Marsh | `3035bfc8ac1c6318` | `7ca187ccfae9e668` | Re-run final-source case; retain the historical transcript |
| WLD-03 Red Mesa | `a0efc3684305df4e` | `9e4fada9b3b0dedd` | Re-run final-source case; retain the historical transcript |
| WLD-08 Northwatch | `13c838555e063dcc` | `0291baa5c2f17f26` | Re-run final-source case; retain the historical transcript |
| WLD-09 Blackwater Dam | `2718d62de0cb6a17` | `f03a75eb9bcf332d` | Re-run final-source case; retain the historical transcript |

## Branch recovery inventory

The review enumerated 76 branches, including 27 `map/*` branches. Twenty-three map branch tips are ancestors of the audited main; four diverge. In particular, `map/remaining-level-rough-blockouts` is already included in main’s ancestry (zero ahead, 2,718 behind), so it is not another missing implementation to merge.

| Diverged map branch | Head | Ahead / behind audited main | Meaning |
|---|---|---:|---|
| `map/house-default` | [`5c3daf923a73`](https://github.com/mrcalzon02/The-Beckenburg-Project/commit/5c3daf923a7359554dc075c9e99be139bbcb1441) | 7 / 443 | Older House stack; assess content equivalence, not ancestry alone |
| `map/house-encounter` | [`4afae307efe7`](https://github.com/mrcalzon02/The-Beckenburg-Project/commit/4afae307efe7b4903e14aca2d51d545ebcb0a21a) | 4 / 444 | Older House stack; assess content equivalence, not ancestry alone |
| `map/house-final-review` | [`02f83c858acb`](https://github.com/mrcalzon02/The-Beckenburg-Project/commit/02f83c858acb9d4f99a01d4c907591e965fd912f) | 5 / 444 | Older House stack; assess content equivalence, not ancestry alone |
| `map/roster-structure-batch` | [`d9f706f44984`](https://github.com/mrcalzon02/The-Beckenburg-Project/commit/d9f706f44984aaf05cadaf0911eb058880056bb2) | 335 / 228 | Substantial saved roster implementation; draft PR #86 |

The Castle loose-object recovery stash also has divergent ancestry, but its central clutter/occlusion/window behavior is already present on main. Preserve the recovery reference; do not count its stash parents as three missing feature patches. No branches were merged, deleted or retagged by this audit.

## Completion criteria and sequencing

Use the existing map checklist and first-principles stages. A map is finished only when its applicable construction, runtime, editing, gameplay and review obligations have evidence for the intended source. Shared requirements are:

- Reconcile existing candidate commits and assets with newer main; preserve newer portal lighting, gate guard, movement and saved-source fixes.
- Bind each map to one current construction/runtime/editor resource identity; retain historical evidence separately.
- Keep architecture, terrain holes, floors/stairs, collision, room ownership, portals, acoustics, services, spawns and canonical van coordinates coherent.
- Complete supported room functions, utilities, shared hiding budget including zero-cover difficulty, evidence/ritual/encounter behavior and ordinary-view visual review.
- Validate native save/reopen and custom document migration, complete solo and real multiplayer cases, and installed operating behavior on the exact integrated revision.
- Update the existing per-map stage register with scoped evidence and remaining work; complete captures/publication for actual implementation milestones.

Preserve the existing per-site search-space limit and real permanent seals; stable gameplay owner groups are not physical-room counts. Preserve independent source-running and breaker states, host-authoritative events and shared indoor/outdoor hiding limits. Keep visual/style/performance diagnostics distinct from release-blocking build/launch/run and required multiplayer/updater failures. Do not add round-duration testing to this map audit.

**Recommended work order:** resolve the shared integration/evidence ownership first; close Castle HQ and the current Briar House; then finish the strongest saved native cases (Northwatch, Monastery, Mine, Lookout and Hospital). Run institutional native promotion and the remaining four native-adapter tasks as scoped work, while continuing the explicit room-service, perimeter, water and encounter gaps for the rest of the roster.

Sources: [map fitness checklist](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/771b55ae652c12a43aeacfbe65d55c3ffc1bc57c/development/quality/MAP_FITNESS_CHECKLIST.md), [first-principles sequencing](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/771b55ae652c12a43aeacfbe65d55c3ffc1bc57c/development/levels/FIRST_PRINCIPLES_REBUILD_PLAN.md), [task-critical editor requirements](https://github.com/mrcalzon02/The-Beckenburg-Project/blob/771b55ae652c12a43aeacfbe65d55c3ffc1bc57c/docs/development/AGENTIC_MAP_BUILDER_ARCHITECTURE.md).

## Verification performed for this diagnostic

- Read current main and held candidate source, map registries, runtime selectors, ordinary editor paths, 29-packet registers and per-map current checkpoint records.
- Compared map branch ancestry and actual implementation, including the held roster branch and older House/recovery work.
- Validated all 29 diagnostic IDs against the committed roster and checked every cited file exists at its pinned commit.
- Checked retained case-report hashes and six actual current HCS2 resource digests; kept historical success separate from current-source coverage.
- Identified cross-branch and within-candidate defect ID conflicts. Existing records were not silently renumbered or marked accepted.
- Read latest main workflow results: full multiplayer [44863686526](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44863686526) and GPU [44863686455](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44863686455) succeeded at `cb220372fb721fbe588f49d3dc488f6dbba1602d`; installer [44863580794](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44863580794) succeeded at different source `c238cff07d73d4e326b220521974aca5494078ed`. These are not whole-roster candidate acceptance.
- No code, map geometry, gameplay, release state or runner schedule was changed as part of this review. The output is a diagnostic backlog, not a new completion certificate.

Companion structured inventory: [MAP_DEVELOPMENT_DIAGNOSTIC_2026-11-26.json](MAP_DEVELOPMENT_DIAGNOSTIC_2026-11-26.json).
