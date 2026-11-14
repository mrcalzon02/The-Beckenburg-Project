# Development defect ledger

Every discovered error, regression, placement problem or unresolved issue belongs here. `defects.json` is authoritative; regenerate this view with `python tools/build_defect_ledger.py`. A scoped fix never certifies unaudited maps.

[Required fitness checks](MAP_FITNESS_CHECKLIST.md) · [Screenshot gallery](../../beckenburg-development-gallery.html)

## Required issue record

Stable ID, symptom, cause or uncertainty, affected sites, reproduction/evidence, fix/status, verification and older-map audit obligations. Add discoveries immediately, including issues introduced by fixes; do not delete failed evidence.

## Recorded defects

### DEV-355 — GPU solo smoke falsely fails after retirement of GamePanel and obsolete startup counters

**Status:** fixed-scoped; exact-source Windows GPU/native and 3-site Mesa GPU-UI cycle passed; hardware performance acceptance pending · **Affected:** gpu-client GpuUiSmoke (Castle -> solo -> shared-LAN hunter -> van retreat -> Castle), all default smoke sites

In exact-source GPU run 43179085546 the standalone OpenGL geometry/lighting/resident-VBO checks and eleven-map residency review succeeded, but the UI stage reported 'solo.deploy was invoked but no live GamePanel appeared' and produced only a CSV header, with no persisted failure screenshot.

Cause / uncertainty: The acceptance harness still searched for a retired '$GamePanel' nested class. Current solo returns a real LanWorldPanel. Subsequent reflective reads referenced retired scene, initialFrameReady, prewarmGpu* and session fields, and its VK_X/glass-pane exit flow was no longer the physical host-authoritative van-door action.

Fix / next action: GpuUiSmoke now discovers LanWorldPanel and actual map/renderer/opening/frameCount; gates on first real cinematic preview and GPU readback; verifies physical hunter movement, authenticated VANDOOR/RESULT|RETREATED, and the registered return-to-Castle handler; emits case-stage and native failure capture if a case cannot start. Keep actual multi-site Mesa and installed Windows checks; do not weaken GPU or multiplayer functional gates.

Evidence and verification: Confirmed exact-source GPU acceptance https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43258886439 all three jobs SUCCESS: compile-gpu-java17, windows-build (actual packaged native software-recovery executable), and opengl-mesa-smoke (real OpenGL, world residency, UI). Native 3-site GPU artifact https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43258886439/artifacts/13962993117: HOUSE 5889ms 11 frames, FARMHOUSE 9845ms 8 frames, SCHOOL 3255ms 7 frames; each moved 0.31m, GPU readback=true, host RESULT RETREATED, returned to Castle. Exact source 2f83b2fadfc9834781efd189f3751618afd4f5ae. This repairs the old false-negative smoke. Hardware FPS and other individual location checks remain open.

Older-map obligation: The shared GPU UI smoke cycles House, Farmhouse and School. Verify each actual launch, movement, extraction and return individually; Castle and the other map types have separate native residency checks. Windows hardware FPS and headphones remain outside Mesa acceptance.

### DEV-354 — Blackpine editor snapshot disagreed with live stairwell landing aperture

**Status:** source/snapshot synchronized; editor save/reopen and installed visual verification pending · **Affected:** Blackpine mill loft serialized development definition and live mission geometry

The checked-in Blackpine source snapshot ended its upper stair aperture at Z31.4 while the current runtime aperture ends at Z31.6; the serialized scene also retained five obsolete loft fence sockets and colliders.

Cause / uncertainty: The development export was not refreshed after the runtime last-riser collision fix and continued to carry out-of-date loft rail decorations.

Fix / next action: Align the recorded upper opening at Z31.6 and remove only the five duplicated room-11 fence entries and colliders. Verify editor load/save parity before accepting the archive as a current construction source.

Evidence and verification: Source snapshot vs runtime mismatch confirmed at b55948a0 and corrected in 5216f206. Exact-source Blackpine step in CI 43179085398 completed SUCCESS, with artifact 13926116342. Export/import/save-reopen acceptance not established by stair test.

Older-map obligation: Audit editor exports against live runtime stairs on each older map; correcting Blackpine does not certify the other saved maps.

### DEV-353 — Overlapping unsupported loft railing sets obstruct Blackpine stair access

**Status:** focused exact-source native rail/route regression and Windows release passed; installed user-eye and hardware acceptance pending · **Affected:** Blackpine sawmill flight and upper inspection loft; shared loft-wall rendering and opening-guard generation

Five manual field-fence rails, automatic perimeter rails and automatic stair-void guard rails overlapped. Void guards extended three metres beyond the upper deck over the lower flight, and an uninterrupted loft-edge rail crossed the actual stairwell entry.

Cause / uncertainty: Rail construction did not share one deck-and-aperture geometry contract: authored enclosure colliders duplicated renderer rails, side guards were not clipped to physical slabs, and loft edge generation failed to split at stair openings.

Fix / next action: Delete five redundant loft fences and matching colliders; clip raised opening rails to supported upper floor, split the southern loft edge around the opening, and leave the physical stair handrails and deck intact. Reinspect actual rendering and both traversal lanes.

Evidence and verification: Remote main source commit 5216f206b8f7677b90fedd213ca748186f66a7be. Full-round 43179085398 passed its packaged TCP, ghost-hearing, BlackpineGrayboxTest, BlackpineStairVisualTest and native screenshot upload steps. Original native screenshot artifact 13926116342 is downloadable at https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43179085398/artifacts/13926116342. Exact-source installer 43179085274 completed SUCCESS and released beckenburg-v0.1.320, targeting 5216f206. Full advisory manifest and physical/installed visual walkdown remain separate.

Older-map obligation: Shared guard clipping and loft-edge renderer require separate checks on House, Farmhouse, Church and other upper-level sites. Do not infer old maps are fixed from Blackpine.

### DEV-270 — Spectral practice camera ignored the current room elevation

**Status:** fixed-scoped; real House cellar playtest verified · **Affected:** Ghost practice gameplay and mission playtests in rooms above or below ground

In House Cellar Storage, the ghost HUD identified the basement but the renderer viewed it from ground-floor eye height.

Cause / uncertainty: The practice camera copied room spawn X and Z while leaving the renderer eye height at its default 1.62 metres.

Fix / next action: Set the camera from the authored spawn in all three axes on each sync, including after drifting rooms.

Evidence and verification: The Windows editor test failed its room-height assertion before the fix. The corrected real renderer passed at -1.38 metres in Cellar Storage, whose authored floor is -3 metres. The desktop suite captures house-playtest.png.

Older-map obligation: The shared practice panel uses the current room spawn on every map. Direct basement evidence covers House.

### DEV-269 — Mission editor retained Castle assets after switching scenes

**Status:** fixed-scoped; real Castle and House palette parity verified · **Affected:** Level Editor scene selection and post-import palette refresh

After opening House the Assets tab still offered Castle materials and the Castle water module.

Cause / uncertainty: The palette was created with the Castle project and refreshed against that project after imports regardless of the active level.

Fix / next action: Reload from the active project on scene open and import. Serve paged assets through the same project adapter.

Evidence and verification: EditorAgentGuiTest failed at aa4b8956 on the Castle-only Materials category in House. The corrected test compares every visible Castle and House row with the native catalog and verifies HTTP asset readback.

Older-map obligation: All mission sites share this selection path. Desktop parity was verified directly for House and Castle.

### DEV-268 — Committed Markdown development logs contain non-UTF8 Windows punctuation bytes

**Status:** fixed-scoped; UTF-8 ledger decode verified · **Affected:** development/quality/DEFECT_LEDGER.md and development/RECOVERY_PLAN_2026-10-25.md

Reading committed Markdown development logs as UTF-8 fails on standalone Windows-1252 dash bytes.

Cause / uncertainty: A prior documentation writer emitted legacy punctuation outside the UTF-8 encoding used by quality tools.

Fix / next action: Normalize invalid legacy bytes to their equivalent Unicode punctuation while preserving all valid Unicode and every issue entry. Keep registry and generated ledger writes explicitly UTF-8.

Evidence and verification: Baseline 62c6a66f contains invalid UTF-8 byte 0x97 in both Markdown files; scoped normalization now decodes as UTF-8. The JSON registry was already valid UTF-8.

Older-map obligation: Documentation encoding repair does not certify map fitness or close existing issues.

### DEV-267 — Hunters stay upright after damage and incapacitation

**Status:** fixed-scoped; reaction assets, real TCP and native OpenGL review pass; installed acceptance pending · **Affected:** Cleric, Observer and Demonologist world sprites, all six hunter seats and hunter camera lenses

No pain pose is selected for real damage and a downed teammate retains their last standing/walking cutout.

Cause / uncertainty: Only locomotion poses exist; the host stops publishing dead bodies, and the hunter-down view accepts only three seat IDs.

Fix / next action: Add four directional pain cutouts and one overhead body texture per class. Broadcast timed pain only after actual host health loss, retain downed BODY/posture snapshots to hunters, render bodies as horizontal floor cards, and share state with video/photo views. Make duplicate down snapshots idempotent; preserve ghost privacy and reset on site/departure.

Evidence and verification: development/quality/evidence/hunter-reactions-2026-11-07/; fifteen validated transparent sprites, real TCP damage/down/privacy checks, seven focused regressions and six real native RTX 5070 pain/body captures. Initial Demonologist backdrop rejected and repaired as a separate alpha-v2 source.

Older-map obligation: Shared hunter rendering applies across maps. Review captures cover the actual House; floor cards use local horizontal support and are not ragdolls or a sloped-terrain corpse mesh.

### DEV-266 — Default player lacks jumping and portable airborne crouch capability

**Status:** fixed-scoped; focused physics/TCP acceptance; installed acceptance pending · **Affected:** Shared solo/co-op investigation controller and portable editor-kit default player motion

The player can only move along continuous support; crouch lowers the body and cannot produce a tucked running jump.

Cause / uncertainty: No independent airborne collision/gravity state or jump intent exists in the shared player package.

Fix / next action: Add portable default jump controller, airborne capsule clearance and raised solid support; wire host clock, client intent and Space controls; reuse crouch art at raised feet and auto-stand after landing when headroom permits.

Evidence and verification: development/quality/evidence/player-jump-2026-11-07/; PlayerJumpTest and PlayerJumpNetworkTest plus focused movement/crouch regressions.

Older-map obligation: Audit stair openings, ledges, low ceilings and prop tops individually on older maps. Castle and downstream runtimes require their own jump adapter adoption; installed GPU acceptance remains open.

### DEV-265 — Routine controls require function keys and solo uses a different equipment/runtime path

**Status:** fixed-scoped; Java compilation and focused real localhost acceptance; installed acceptance pending · **Affected:** Castle movement, solo free play, solo campaign, LAN hunter and ghost controls

Voice, books, props and equipment require F keys; solo uses a separate six-item GamePanel with different interactions and no shared gear drops.

Cause / uncertainty: Duplicated solo and multiplayer view/controller implementations and scattered input prompts.

Fix / next action: Remove GamePanel; launch private local LanHost/LanClient with LanWorldPanel for solo and campaign. Share the conventional keyboard layout and field-action menu, preserve host validation, expand physical drops to carried device types, and update live prompts and docs.

Evidence and verification: development/quality/evidence/controls-audit-2026-11-06/; LocalInvestigationTest and focused network/control/equipment/campaign regressions. Exact scoped source and installed limitations recorded there.

Older-map obligation: Shared controls apply to all selected runtime sites; this does not certify unaudited map geometry or installed release behavior.

### DEV-264 — Rear van departure button cannot be used from front desk

**Status:** fixed-scoped; Java 17 compilation, exact mesh and TCP extraction checks pass; installed acceptance pending · **Affected:** Shared expedition van; House, Farmhouse, School and Hollow Creek targeting; solo and LAN departure

Rear wall control loses its prompt when standing at the front desk; legacy solo button toggles cargo doors without ending the mission.

Cause / uncertainty: Two metre world targeting cap and 2.05 model-unit broad phase truncate the scaled button ray; legacy solo handler only toggles the shutter.

Fix / next action: Allow exact visible button hits across the interior at cargo-deck height, preserve normal interaction reach, and settle legacy solo extraction after collision-safe closure. Retain the concurrent shared-host solo refactor.

Evidence and verification: development/quality/evidence/van-departure-2026-11-06/: isolated and working-source compilation; VanCargoDoorTest, LanVanExtractionIntegrationTest, VanModuleIntegrationTest, VanHumanScaleTest. TCP test walks to front desk, sends aimed INTERACT, checks closed shutter and one RETREATED result.

Older-map obligation: Shared van module covers all placements; physical installed gameplay remains unverified. Four-site targeting and three-site circulation checked; no broader map audit claimed.

### DEV-263 — Crouch and stand snap the local camera between eye heights

**Status:** fixed-scoped; camera math and actual TCP posture tests pass; installed acceptance pending · **Affected:** Castle, solo investigation and LAN hunter camera

Pressing Ctrl abruptly jumps the eye by 0.70m; LAN only renders on the 110ms gameplay timer.

Cause / uncertainty: Viewport eye height is assigned directly from the binary physical posture.

Fix / next action: Add a shared critically damped camera transition, retain independent physical feet for collision/evidence and moving support, and animate LAN camera-only frames at 33ms without extra STEP/actions. Ignore Ctrl key-repeat in Castle and solo; retain host posture authority and reset on site changes.

Evidence and verification: development/quality/evidence/crouch-smoothing-2026-11-04/: CrouchCameraTest, expanded real TCP CrouchNetworkTest, low-ceiling/sprint/feet checks, hunter animation gameplay, Castle interaction and sprite floor regressions.

Older-map obligation: Shared player camera paths cover investigation sites and Castle; physical posture dimensions and movement rules remain unchanged.

### DEV-276 — Closing Beckenburg leaves hidden GPU worker and Java process alive

**Status:** SOURCE FIX COMMITTED; Java child-process regression and installed Windows GPU exit acceptance pending · **Affected:** Normal desktop Exit, main window X, Castle gate, solo pause Exit, GPU launcher and auxiliary water editor, interactive DEV asset showroom and headless DEV room captures

After the visible Project Beckenburg window closes, the game process can remain active indefinitely in the background; multiple user reproductions.

Cause / uncertainty: GpuDesktop starts an explicitly non-daemon beckenburg-opengl LWJGL3 worker. Main-menu exit only disposed Swing; Castle shutdown disposed panels; neither signaled Gdx.app.exit and awaited native worker. Main window close used EXIT_ON_CLOSE but bypassed orderly renderer disposal.

Fix / next action: Unify normal exit routes through guarded scene/network/audio cleanup, request GPU worker exit on the owning GLFW thread, join with a 15s bound off the Swing EDT, and terminate the JVM even if a native service refuses cleanup. Keep headless dedicated server independent. Interactive DEV Room closure now runs the same process exit; DEV Room capture commands stop the GPU worker synchronously when returning.

Evidence and verification: User report 2026-11-05; source inspection of InvestigationGame.java shutdown/start.exit/GamePanel.pause, GpuDesktop.java startGpu/stopReviewGpu, and InGamePauseMenu.java. DesktopShutdownTest adds a child-JVM regression using an intentionally lingering non-daemon service; exact-source Windows CI and installed gameplay exit tests remain pending. DEV room path inspection also confirmed its JFrame used DISPOSE_ON_CLOSE with no GPU worker teardown.

Older-map obligation: No map geometry changed. Verify normal game/window/quit/pause exit after House, Castle, campaign and LAN sessions; confirm no Java or helper process survives and that diagnostics record clean GL shutdown.

### DEV-275 — DEV Room reports green despite failed source-only decoder and catalog diagnostics

**Status:** VERIFIED DEV-QA repair: Windows native review 42130219315 PASS with strict Hollow Creek roster, 22 Castle relics, source-only models and Castle fit; no release or full gameplay acceptance inferred · **Affected:** Developer Asset Room diagnostic workflow, Castle source relic decoder, native Castle graybox fit, Hollow Creek asset registry

Green workflow 42126655787 uploaded captures even though three advisory assertions failed; test marked REVIEW_REGISTRATION source meshes as undecodable and omitted 18 new source refinement models.

Cause / uncertainty: Audit.loaded() excluded successful source-only REVIEW_REGISTRATION state; a hardcoded catalog count ignored 15 new GLBs plus three contact rooms; continue-on-error hid semantic failures.

Fix / next action: Count source-only models as decodable without approving them; check refinement roster dynamically; require proper source-only classification; make native source audit, Castle fit and gallery capture steps fail the DEV-only workflow on regression while keeping cosmetic checks out of release blocking.

Evidence and verification: Workflow 42126655787 passed superficially with three suppressed assertion errors in job logs; strict workflow 42128827427 correctly exposed independent astrolabe geometry error. Commits 2f70f63b, 0764e27e, 80858c52, 365c6958, eb5f642d and 84997117; final follow-up Windows acceptance pending. Native full run 42130219315 PASS; independent fast gallery 42131185143 PASS after fixes.

Older-map obligation: All future location/model catalogs and source-only inspection rosters must compare actual registry membership and decoded GLB status without changing runtime approval.

### DEV-274 — Castle bronze astrolabe dial penetrates floor and rotates radial markers around wrong axis

**Status:** VERIFIED SOURCE-ONLY repair: deterministic GLB/geometry generator 42129321468 PASS; native 22-relic/crypt decode, Castle fit and screenshot workflow 42130219315 PASS; final texture/in-world placement remains unapproved · **Affected:** Castle relic source-only astrolabe model and all proposed vault/museum display banks using the shared GLB

Native relic decoder refused the astrolabe as below-floor bounds; source ring extended down to -0.15 m and ornaments below ground.

Cause / uncertainty: The bronze vertical disc was centered at y=.31 with .46 m radial extent; its decorative ring rotated ticks with Y-axis yaw despite the dial occupying XY face plane. It also lacked a supporting upright.

Fix / next action: Raise dial assembly .16 m, rotate its ticks and needle in face-plane roll, add a bronze load-bearing support and keep the wood base at y≈.005 m. Preserve source-only/never-runtime-approved status and require exact regenerated GLB/grounded source checks.

Evidence and verification: Real DEV Room 42128827427 error 'Off-ground/reversed model bounds level_blockout_relic_bronze_astrolabe'; source commit aa33e7b4, generator and geometry regression run 42129321468 PASS, native follow-up pending. Native full run 42130219315 PASS; independent fast gallery 42131185143 PASS after fixes.

Older-map obligation: Audit other relic mounts and display plinths independently when fitting them into Castle/Church/Mortuary; source-only ground contact is not actual scene fit.

### DEV-273 — DEV Room multi-floor camera starts inside previous bay cannery machine

**Status:** VERIFIED DEV ROOM repair: 42130219315 full native renderer and 42131185143 fast gallery PASS; screenshot floor2/bay2 visibly readable and 23911/256000 delta rather than previous 256000/256000 camera-occlusion; gameplay placement remains unapproved · **Affected:** All 3-storey developer asset-room wings, especially floor 2 bay 2 with cannery boiler behind the camera

Native floor-2/bay-2 screenshot was almost entirely an opaque teal machine texture rather than six readable asset stands; delta-based visibility reported 256000 changed pixels and falsely passed.

Cause / uncertainty: The entrance camera was placed only .5 m ahead of the previous bay's rear row (z=10.5, camera z=11), inside an adjacent 3D prop. Visibility check had no upper coverage limit.

Fix / next action: Move each bay entrance to 6 m before its front row and 4 m past the previous rear row, assert both clearances in Java layout test, and reject gallery captures with over 60% pixel replacement compared with an empty review stage. Shorten model labels to distinctive suffixes for readable contact sheets.

Evidence and verification: Directly inspected 12 native gallery PNGs and gallery-visibility.csv from artifact of 42128827427; floor2/bay2 showed 256000/256000 changed pixels. Source commits 32a53128, af785d61, 795739ce, 8de31911; smoke run 42129937613 PASS, native rerun pending. Native full run 42130219315 PASS; independent fast gallery 42131185143 PASS after fixes.

Older-map obligation: The DEV Room is isolated from playable scenes; apply adjacent-prop camera/view obstruction checks when authoring actual multi-floor maps and their editor inspection viewpoints.

### DEV-272 — Castle lower wall caps z-fight upstairs floors

**Status:** Renderer patch fa6cad4a is on main; exact-source geometry diagnostic and real upper-hall screenshot pending; installed GPU visual acceptance pending · **Affected:** Beckenburg Castle upper-west corridor, lodging and second-storey slabs above ground partitions

Stone/gray strip appears across timber flooring in upper rooms; user's screenshot shows apparent walls emerging through floors.

Cause / uncertainty: SceneRenderer.architecturalWall previously emitted upward-facing cap triangles at the same y=3.4 as the upper-floor surfaces drawn by castleSlabs, causing coplanar depth conflict.

Fix / next action: Keep actual wall side faces, structure and collision; subtract only same-height floor coverage from cap triangles. Test full/partial/uncovered cases, inspect actual upper-hall capture and confirm installed build.

Evidence and verification: User screenshot 2026-11-04, source commit fa6cad4a374aad2158e2ff4f72f91964ad799039; focused Castle run and 3D screenshot acceptance pending at checkpoint.

Older-map obligation: Audit every Castle upper join, stairs/voids and older authored multi-storey map separately for hidden wall caps and co-planar slabs.

### DEV-271 — Castle wall lights use uncorrected editor yaw and independent on/off source pivots

**Status:** source repair 0329d79 pushed and remotely read back; focused CI and installed camera-angle verification pending · **Affected:** Castle masonry wall-sconce renderer and editor-authored wall-light transforms; review other location light socket mounts separately

Player screenshot shows an imported lit wall fixture visibly canted or poorly seated against stone masonry near an upper wall/ceiling join.

Cause / uncertainty: CastleWallLampMount used saved yaw without snapping fixture geometry to the nearest authored wall plane; SceneRenderer re-centred the lit and unlit GLBs independently. Exact visible-source root cause still requires matched runtime capture.

Fix / next action: Use authored wall normal for final render yaw and support offset, project real source vertices for wall clearance and use one off-state pivot for both light states; keep source GLBs and saved authoring coordinates untouched. Validate screenshot-equivalent angle and model mount on installed build.

Evidence and verification: User-provided in-game screenshot (2026-11-04 local); source commit 0329d79be6acdcee55633ef11e577274c7c57410 remotely verified. Focused CI at 41986692072 was in progress at ledger entry; no visual acceptance claimed.

Older-map obligation: Audit imported wall and ceiling fixtures on House, Farmhouse, School and all older sites; do not infer map-wide fixture validity from Castle source tests.

### DEV-270 — Screenshot pushes leave development gallery index stale

**Status:** CI generator repaired c080e29b; gallery refresh run 41951988544 PASS · **Affected:** Development screenshot gallery, image SHA-256 index and documentation CI

Committed captures were omitted from the index; gallery check failed although source PNGs existed.

Cause / uncertainty: The old gallery workflow checked generated files but never regenerated them after new screenshot pushes.

Fix / next action: Rebuild index against current main with serialized non-force retries and original image hashes; retain pull-request stale-index enforcement.

Evidence and verification: Prior failed gallery run 41948681639; fixed run 41951988544 PASS.

Older-map obligation: Every additional map capture must be indexed without implying that map was fully accepted.

### DEV-269 — Shallow Windows installer tag push exceeds GitHub 2 GiB Git-pack limit

**Status:** FIX VERIFIED: Windows installer 0.1.213 (41953756191) and 0.1.214 (41954551988) released with immutable GitHub API tag, installer and SHA readback · **Affected:** Windows installer publishing, versioning and game updater release feed

Build/launch/smoke passed but remote tag push was rejected: pack exceeds maximum allowed size (2.00 GiB).

Cause / uncertainty: A shallow checkout pushed a Git tag, requiring a multigigabyte history pack; version selection did not count reserved unpublished tags.

Fix / next action: Keep the lightweight GitHub REST reference creation and remote readback; reserve version above all existing refs to prevent accidental retags, retain shallow checkout and avoid large Git pack pushes.

Evidence and verification: Release tag beckenburg-v0.1.213 verified at 88bc6e631 and 0.1.214 at c815ed2f2 with published installer assets. Jobs 41953756191 and 41954551988 PASS.

Older-map obligation: No visual maps are implicated. Installed updater must read published non-draft releases including prereleases.

### DEV-268 — Castle NPC screenshot audit overwrites failed angles and falsely accepts one passing view

**Status:** Recovered: exact actor-pixel instrumentation replaces unreliable full-world subtraction; 41956167865 and 41956495409 PASS 5/5 visible grounded residents · **Affected:** All Castle resident screenshot and grounding evidence

The audit reported five of five characters grounded even as a large fraction of their real in-engine angle measurements failed.

Cause / uncertainty: Comparing full scene depths with residents disabled/enabled misclassified unrelated floors, windows and props as actor pixels because rendering/readback cache state differed. Reused filenames also overwrote earlier failed views.

Fix / next action: Use scoped, zero-production-overhead thread-local tracing of accepted actor fragments from world sprite compositor; report accepted near-sole pixels and hidden shoe pixels, mark blocked cameras inconclusive, preserve all frame/crop/mask files per run.

Evidence and verification: Before: 41952700012 reported 12 false-or-mixed failing angles and huge magenta floor masks. After: 41956167865 and 41956495409 PASS, 5/5 residents and 0 clear-view failures; actual magenta actor mask now excludes floors and furniture.

Older-map obligation: Apply similar visual acceptance to hunters and all authored investigation maps; no single shot certifies the whole site.

### DEV-267 — Camera-selected directional art still moves stationary NPC world-space geometry

**Status:** Source geometry and camera-view hysteresis mismatch repaired; exact-source Castle camera orbit run 41956495409 PASS; installed GPU verification pending · **Affected:** Castle resident and playable grounded human character cards

Character outlines wobble or hover when the camera changes even though NPC world coordinates are stationary.

Cause / uncertainty: Grounded NPC plane dimensions and horizontal pivot used image alpha margins. An additional bug picked the art via stableFacing() but the world plane via raw facing(), causing an abrupt 90-degree geometry switch while the previous image remained latched.

Fix / next action: Keep static world card bounds, use one selected direction for both cutout frame and physical plane, and verify near-boundary reversible orbit with CharacterBillboardAnchorTest and full-scene images.

Evidence and verification: Commits 5e355b1f and 325055d, corrected regression 783f66b; focused Castle run 41956495409 PASS, 42 runtime game screenshots.

Older-map obligation: Audit all stances, stair elevations, 360-degree camera motions and intentionally hovering ghost types independently.

### DEV-264 — Castle light-switch source GLB gains an enormous stretched sconce backplate

**Status:** source corrected in ede4bb147f1051ad646e59cf0b197ed32d979970; focused Castle CI 41947018830 PASS; physical installed screenshot acceptance pending · **Affected:** 25 authored Castle light-switch instances; wall-mount renderer shared with Castle fixtures

User screenshot shows a long off-colour horizontal strip with a small switch and dark aperture placed on a Castle wall.

Cause / uncertainty: buildCastleStatic incorrectly rendered light_switch_1 with mountedLamp, adding the generic sconce steel-strap plate and applying lamp-sized 0.85 scale without source-dimension fitting.

Fix / next action: Skip sconce strap for switch meshes; use source GLB with original UVs/materials and uniform physical plate bounds; choose yaw from nearest supporting authored wall and avoid floating mounts after wall deletion. Include captured real-renderer switch view and wall-alignment regression.

Evidence and verification: User Castle screenshot 2026-11-04; CastleSwitchMountTest: 25 switches PASS and build/e2e/castle-repairs/castle-switch-source-fitted.png from focused CI https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/41947018830

Older-map obligation: Audit other mounted wall controls and imported fixtures individually; fitting this Castle path does not prove all models elsewhere are oriented correctly.

### DEV-265 — Door state refresh discards unrelated stationary shadow visibility and causes frame stalls

**Status:** Castle regional fix committed 0d12ad3e; House and Castle same-side shadow journal committed d09e4737; exact-source performance and installed GPU acceptance pending; initial new cache test sample-quantization failure corrected in f0b6d4f9 · **Affected:** Castle doors and House investigation portals, fixture occlusion cache and resident world chunk lighting

Opening doors stalls game, potentially dropping frame rate to single digits; CI House submission benchmark showed toggle p50 375.616 ms and peak 80,874 shadow rays on baseline 0d12ad3e.

Cause / uncertainty: Castle code globally advanced occlusion epoch and discarded all cached light visibility on door changes; subsequent retained-chunk code also evicted all cached rays from an affected source even when each particular ray stayed on the source side of the portal.

Fix / next action: Invalidate only the swept-door receiver cells and crossing visibility rays; retain independent GPU geometry/material/UV revisions. Maintain bounded revisioned door event history, with conservative re-probe if events overflow. Preserve circuit, ghost effect and true occlusion changes. Compare exact-source tests and numeric before/after on real Windows GPU.

Evidence and verification: Profiler https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/41945883549 (diagnostic only); selective Castle test and House stationarity test; current exact-source profiler https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/41947973193 (pending at entry)

Older-map obligation: Audit every investigation-map door and all Castle storeys plus installed Windows GPU frame-time p50/p95/p99; do not count packaging success as FPS acceptance.

### DEV-266 — Castle tabletop decorative models ignore edited supporting furniture rotation

**Status:** source correction committed aacb391ddfc64ff716c146f4dca8b73e7582c955; latest full and installed acceptance pending · **Affected:** Castle decorative imported books/screens and parent furniture placements edited in the native Castle editor

Moving/rotating a table leaves its decorative GLB object at the moved position but with its previous world-facing rotation.

Cause / uncertainty: CastleHubDecor.placedAccents recomputed the object's parent translation/height but reused original accent yaw without applying parent yaw delta.

Fix / next action: Apply editor support yaw minus original authored support yaw to decorative object's yaw; preserve original art, model size and current support-derived height; check rotation and translation together in CastleLightingPlacementRepairTest.

Evidence and verification: Source aacb391d; expanded CastleLightingPlacementRepairTest; exact-source CI/installed review pending

Older-map obligation: Audit all parented decorations on other Castle furniture and later native maps for relative orientation and physical support before marking those maps complete.

### DEV-263 — Character boots visibly float above the Castle floor despite earlier alpha-pad correction

**Status:** Castle source renderer PASSED production-scene software acceptance for all 5 residents / 20 clear angles, 0 genuine contact failures and 1 obstructed angle; installed GPU acceptance pending · **Affected:** All grounded human sprite billboards in Castle and investigation scenes; ghost hovering remains deliberate

NPC boot soles visibly detach from the rendered floor at certain camera views, as in the user-provided Castle screenshot.

Cause / uncertainty: Earlier character planes followed camera-selected image margins; the old test also misidentified unrelated cached geometry as NPC pixels, reporting fictitious -22px gaps. Directional art and world plane could use different hysteresis choices.

Fix / next action: Keep actor plane geometry immutable, use the SAME latched view for art and plane, UV-fit shoe soles to world support, clip source matte below floor, and test direct actor raster fragments. Audit installed OpenGL, movement and multiplayer actor stances before final closure.

Evidence and verification: Actual game renderer job 41956167865 PASS 5/5, 0 genuine gaps and 1 occlusion; 42 unedited game screenshots plus actor-only masks and crops under development/levels/previews/npc-ground-proof-2026-11-04/run-41956167865. Latest hysteresis source test 41956495409 PASS; commits fbdf523, 325055d, 783f66b.

Older-map obligation: Every grounded human across Castle, solo, co-op and LAN must receive independent orbit/stance/floor checks; do not adjust ghosts' intentional hover.

### DEV-260 — Castle residents remain static despite hunter walk and crouch animation

**Status:** fixed-scoped; focused local acceptance; installed release acceptance pending · **Affected:** All five Castle residents

Residents only display standing turnarounds.

Cause / uncertainty: Castle sprites bypassed animation and lacked a movement routine.

Fix / next action: Package 60 authored directional walk/crouch poses; bounded collision-checked routines pause for approaching players; dialogue targeting and labels follow actual positions.

Evidence and verification: development/quality/evidence/resident-animation-2026-11-03/ and development/levels/previews/resident-animation-2026-11-04/

Older-map obligation: Shared rendering changes cover visible actors across maps; individual terrain and installed-copy visual acceptance remain separate.

### DEV-261 — Visible entities lack circular ground contact shadows

**Status:** fixed-scoped; focused local acceptance; installed release acceptance pending · **Affected:** Players, Castle residents, ghosts and both Twins bodies

Actors appear visually disconnected from their support surface.

Cause / uncertainty: Sprite rendering never submitted a ground contact decal.

Fix / next action: Project soft black circular disks at actual support height; reject walls, sky and floor holes by receiver depth; preserve depth and keep ghost shadows independent of hover animation.

Evidence and verification: development/quality/evidence/resident-animation-2026-11-03/ and development/levels/previews/resident-animation-2026-11-04/

Older-map obligation: Shared rendering changes cover visible actors across maps; individual terrain and installed-copy visual acceptance remain separate.

### DEV-262 — Standing resident crops lose boots and pitched projection clips shoe rows

**Status:** fixed-scoped; focused local acceptance; installed release acceptance pending · **Affected:** Five Castle resident standing turnarounds and shared human sprite floor projection

Standing Elsbeth boots end at a straight cutoff and look buried.

Cause / uncertainty: The original cutout bounds truncate boots; midpoint projection and linear screen-depth interpolation further clip feet when pitched.

Fix / next action: Rebuild all five standing turnarounds with complete boots and transparent sole margins, preserving concept masters; use exact endpoint projection and ray/card depth. SpriteFloorContactTest rejects the old projection with 49 missing opaque pixels and passes corrected rendering.

Evidence and verification: development/quality/evidence/resident-animation-2026-11-03/ and development/levels/previews/resident-animation-2026-11-04/

Older-map obligation: Shared rendering changes cover visible actors across maps; individual terrain and installed-copy visual acceptance remain separate.

### DEV-254 — Safe areas lack a shared animated energy boundary matching ghost exclusion

**Status:** fixed-scoped; focused CPU/TCP and actual OpenGL evidence; installer acceptance pending · **Affected:** Shared van at investigation sites, entire Castle HQ, and settled active protective wards

The investigation van has no recognizable sanctuary haze; the old optional arena cue uses rectangular motes rather than a circular fading dome.

Cause / uncertainty: Protection and visualization use separate room/rectangle rules; normal investigations do not enable the prior arena-only presentation.

Fix / next action: Add a shared circular van dome and whole-Castle sanctuary, animated blue cloud/wisp material with continuous wall-to-crown fading, settled-ward reuse, swept host exclusion, protected spawning/combat, Twins handling and public ghost-view snapshots.

Evidence and verification: development/levels/previews/safe-area-barrier/: actual runtime frames and two 12-frame animations. SafeAreaBarrierTest, SafeAreaHazeTest, LanSafeAreaBarrierTest, existing ghost/counter/Twins/van checks and complete TCP round pass. Actual RTX 5070 OpenGL frames pass. Logs: development/quality/evidence/safe-area-barrier-2026-11-03/.

Older-map obligation: All built-in/custom-map ghost spawns tested. Visual captures cover the dispatched House and Castle; older-map terrain/lighting and installed full-game acceptance remain individual obligations. House rebuild remains a separate graybox and is not claimed dispatched.

### DEV-255 — Ward ground seals remain at world Y zero and appear before landing

**Status:** fixed-scoped; focused CPU/TCP and actual OpenGL evidence; installer acceptance pending · **Affected:** Shared van at investigation sites, entire Castle HQ, and settled active protective wards

A thrown or upstairs ward can show its seal at ground-floor height rather than at its actual settled support.

Cause / uncertainty: The renderer hardcodes Y .038 and does not distinguish a landed ward from a flying projectile when drawing ground bars.

Fix / next action: Resolve the settled support once for both seal and haze; keep airborne projectiles visible without advertising sanctuary; place upstairs seals at the authoritative floor height.

Evidence and verification: development/levels/previews/safe-area-barrier/: actual runtime frames and two 12-frame animations. SafeAreaBarrierTest, SafeAreaHazeTest, LanSafeAreaBarrierTest, existing ghost/counter/Twins/van checks and complete TCP round pass. Actual RTX 5070 OpenGL frames pass. Logs: development/quality/evidence/safe-area-barrier-2026-11-03/.

Older-map obligation: All built-in/custom-map ghost spawns tested. Visual captures cover the dispatched House and Castle; older-map terrain/lighting and installed full-game acceptance remain individual obligations. House rebuild remains a separate graybox and is not claimed dispatched.

### DEV-234 — Castle native bounds reject coherent west annex and cellar construction

**Status:** fixed-scoped authoring/runtime foundation; full Castle geometry and terrain remain unfinished · **Affected:** Castle native geometry, human editor tools, shared agent service, ground support and cell persistence

Actual pre-fix Castle rejects annex-floor at negative X; surfaces/props below -0.5 are forbidden and naive negative cells would alias legacy paint/nav IDs.

Cause / uncertainty: The old headquarters assumes a nonnegative44x36 footprint and ground/upper authoring only; runtime ground support ignores added native floors.

Fix / next action: Add signed bounded extent and append-only cell encoding, permit actual cellar source heights, refresh runtime ground support on native edits/undo/redo, use native floor materials, update human authoring bounds and expose strict put-surface/remove-surface through shared history.

Evidence and verification: development/levels/rebuild/verification/castle-construction-2026-11-03; pre-fix real JAR refuses negative floor; final packaged test passes actual floor/wall/nav, native save/reopen, history, all2232 unique cells and service renderer capture.

Older-map obligation: Castle-only authoring expansion. Preserve all1584 original cell keys and saved custom native scenes. Below-ground source support does not punch terrain or prove new cellar traversal; those remain required next.

### DEV-233 — Viewport controls regression expects retired physical extraction shortcut

**Status:** fixed-scoped; actual packaged TCP viewport controls pass locally · **Affected:** LanWorldControlsTest assertion after VIEW3D

The test waits for ACCEPT|EXTRACT although the host correctly rejects EXTRACT from physical hunters.

Cause / uncertainty: Old room-mode extraction expectation survived the physical van door contract change.

Fix / next action: Retain actual X-key dispatch, assert host rejection and active round, then issue physical VANDOOR over TCP and require closed-door acknowledgment plus synchronized retreat result. Host behavior is unchanged.

Evidence and verification: development/levels/rebuild/verification/physical-controls-2026-11-03 before/after logs; main127 full41672923465 advisory failure independently reproduced.

Older-map obligation: Shared control regression; no map unlock or release acceptance. Existing full van extraction integration remains authoritative for occupancy and abandonment policy.

### DEV-232 — Contained Church choir loft doors are sealed by the tall nave wall and stair guard

**Status:** fixed-scoped; packaged upper-door and editor-service traversal checks pass locally · **Affected:** Church upper choir loft to tower/archive; shared contained-upper-wall opening authority

Actual swept movement cannot reach tower stair, organ archive or belfry and six functional sockets; nine failed goals, 159429 states.

Cause / uncertainty: Tall nave boundary ignores upper child portals; tower threshold at Z31 overlaps choir stair side guard.

Fix / next action: Share eligible contained-upper portals between physical and rendered walls, retain lower wall/jamb/lintel, and move tower threshold to Z32.65 beyond the guard. Expose the same open-route solver as mission-open-routes in the actual Editor Agent Service.

Evidence and verification: development/levels/rebuild/verification/church-openings-2026-11-03: before failure, after 169097 swept positions Y-3..6, closed/open/crouch/optical and actual triangle controls. Runtime captures show both upper openings.

Older-map obligation: Run shared traversal against every selectable map; preserve saved terrain surface in audit clones. Open-route success does not accept closed-door interaction, hunts, complete map quality or hardware performance.

### DEV-231 — Hunter pose history survives level replacement and departed seats

**Status:** fixed-scoped; actual TCP gameplay and RTX OpenGL acceptance pass · **Affected:** Shared hunter animation tracker and LanWorldPanel peer lifecycle

Reusing a seat near its prior coordinates in another scene can animate a newly stationary hunter; departing seats retain their last sampled pose.

Cause / uncertainty: Renderer animation state was keyed by seat without a scene boundary or explicit peer-departure reset.

Fix / next action: Reset the tracker when the world map instance changes and forget a departing seat in world, video and photo renderers. Exercise the same world-render method used by live gameplay.

Evidence and verification: LanHunterAnimationGameplayTest passes real STEP and Ctrl transitions for all three classes, stop, scene replacement and departing crouched seat through actual TCP/onEvent/renderWorld. A separate compiled negative control omitting scene reset fails exactly at new scene resets gait cleric. Positive RTX 5070 OpenGL test exits 0. development/quality/evidence/hunter-gameplay-2026-11-02/.

Older-map obligation: Shared lifecycle applies to every map; acceptance captures use House fixtures. No all-map or multi-PC animation acceptance is inferred.

### DEV-230 — Rear cargo liner, roof and hinges sit behind the donor body and generic doors discard its atlas

**Status:** fixed-scoped; 13 focused checks pass; installed GPU review pending · **Affected:** Shared Castle and every investigation-arrival van

Rear liner/roof/floor project behind the outer shell; doors pivot from unsupported points and use plain panels with invented windows.

Cause / uncertainty: Exterior fit uses bumper Z -2.10 rather than actual donor rear sheet-metal Z -1.982265830, shifting the body about 19cm forward of its interior. Donor large rear panel faces were omitted.

Fix / next action: Use actual donor rear datum; join liner and exterior profiles with a finished rear frame; restore all six original donor door triangles/UV corners and attach visible hinges at outer jambs.

Evidence and verification: VanRearFitTest checks actual rear datum, rendered UV corners and pivot attachment; matched rear-open, rear-closed and rear-frame views at all three captured sites. See development/levels/previews/van-unification/ and development/quality/evidence/van-unification-2026-11-02/.

Older-map obligation: Shared-module code and all-site station regression verified; individual image review covers Castle, House and Hollow Creek. No complete older-map or installed GPU acceptance inferred.

### DEV-229 — Full-width cargo deck slices through the exterior rear wheel barrels

**Status:** fixed-scoped; 13 focused checks pass; installed GPU review pending · **Affected:** Shared Castle and every investigation-arrival van

From outside, the raised cargo floor forms a horizontal sheet across each rear tyre.

Cause / uncertainty: Prototype floor slab spans the full body width through the donor wheel opening; a bottom face on a wheel cover repeats the intersection.

Fix / next action: Split the deck around both wheel openings, terminating it at the housing inner wall; leave wheel-well undersides open. Optical floor slabs share the cutout.

Evidence and verification: VanWheelWellTest rejects actual deck triangles overlapping wheel volumes; matched exterior-wheel closeups at Castle, House and Hollow Creek. See development/levels/previews/van-unification/ and development/quality/evidence/van-unification-2026-11-02/.

Older-map obligation: Shared-module code and all-site station regression verified; individual image review covers Castle, House and Hollow Creek. No complete older-map or installed GPU acceptance inferred.

### DEV-227 — Imported rear tyres protrude through the unified van cargo floor

**Status:** fixed-scoped; 13 focused local checks pass; installed GPU review pending · **Affected:** Shared Castle and investigation-arrival van module; runtime captures at Castle, House and Hollow Creek

Rear tread and hub remain exposed above the cargo floor and beyond the small legacy wheel cover.

Cause / uncertainty: Imported tyre bounds exceed the prototype cover in both height and longitudinal span.

Fix / next action: Fit closed 3D metal wheel housings around the actual tyres; wrap lower shelving and native-size stock packing around the housing; share shelf and housing coordinates with optical/aim blockers.

Evidence and verification: development/levels/previews/van-unification/: 42 matched before/after production software-renderer PNGs. VanWheelWellTest fails baseline actual cap geometry and passes repair; VanRackStockTest checks all 372 units per tier with no housing intersections. Thirteen passing focused logs: development/quality/evidence/van-unification-2026-11-02/.

Older-map obligation: Shared assembly repair reaches every VanStaging consumer. All-site station access includes a persisted custom map. Individual visual evidence covers Castle, House and Hollow Creek only; this does not certify every older map, installed gameplay or GPU performance.

### DEV-228 — All-site van integration test depends on a selected developer custom level

**Status:** fixed-scoped; isolated all-site check passes · **Affected:** VanModuleIntegrationTest, USER site

The all-site test throws No authored user level selected on a clean profile.

Cause / uncertainty: Site.values() includes USER but the test constructs HouseMap(USER) without authoring/selecting a fixture.

Fix / next action: Use existing AuthoredTestSupport.userFixture() and fixture.map(site), preserving USER and all built-in locations in the assertions.

Evidence and verification: VanModuleIntegrationTest.log in development/quality/evidence/van-unification-2026-11-02/; isolated test home and temporary directory; no developer selection required.

Older-map obligation: Test-fixture correction only; no runtime level selection behavior changes.

### DEV-226 — Playable hunters remain static while walking and crouch by squashing idle art

**Status:** fixed-scoped; real TCP gameplay and local native RTX OpenGL build verified; multi-PC review pending · **Affected:** All three playable hunter appearances in the shared teammate billboard renderer

Received teammate movement uses one standing turnaround; crouch scales that standing image vertically instead of bending knees.

Cause / uncertainty: Only static four-view hunter art was packaged; RemoteBody crouch altered billboard height without selecting a new pose.

Fix / next action: Add 36 generated transparent pose derivatives: two walking states and crouch across four views for each playable appearance. Select walking from received movement and crouch from host posture, retaining base idle and the same physical dimensions.

Evidence and verification: HunterSpriteAnimationTest covers actual atlas frames, movement, stop, crouch, teleport, clock reset and production-rasterizer occlusion. CharacterDirectionalVisualTest, CharacterSpriteWorldMotionTest, GroundedSpriteFeetTest and CharacterBillboardAnchorTest pass. Asset-review preview and source/provenance: art/hunters/animation-v1/.

Older-map obligation: Shared renderer applies across maps; this is not installed Windows multiplayer or per-map visual acceptance. Alternate concept-only appearances are not selectable and are not claimed implemented.

### DEV-001 — Paper-thin loft floor and apparent upstairs light bleed

**Status:** fixed-scoped · **Affected:** Blackpine inspection loft

User observed apparent light from above on the under-loft ceiling.

Cause / uncertainty: The upper floor had one rendered face, no separate soffit/fascia or structural depth.

Fix / next action: Added a 160mm authored slab with physical collision, an independent underside and exposed edges; lowered the utility fitting to Y2.84 before grading.

Evidence and verification: BlackpineDeckDepthTest fails against pre-fix compiled 4b2bb24: loft needs a real 160mm underside. It passes after correction, including actual triangle area/normals and upstairs circuit isolation. Before/after PNGs: development/levels/previews/defects/ and mill-lighting/.

Older-map obligation: Audit floor depth and cross-storey lighting in House, Farmhouse, School, Church and Castle; depth is opt-in and has only been applied/verified at Blackpine.

### DEV-002 — Corner, floor/wall and wall/roof light bleed

**Status:** open-audit · **Affected:** Previously reported joins; broader map set unverified

User reports light bleed through floor/wall joins and requests corner and roof intersection coverage.

Cause / uncertainty: Multiple possible causes: zero-depth geometry, open seams, normals, endpoint sampling or lighting occlusion; do not assume one cause for every join.

Fix / next action: Audit each surface and circuit pairing using MAP_FITNESS_CHECKLIST.md; log each reproducible location as a linked issue.

Evidence and verification: BlackpineLightingTest covers selected wall/floor rays; LightingOcclusionRepairTest covers selected castle planes. These do not certify all corners/roofs.

Older-map obligation: All prior maps require a documented join/corner/roof sweep and matched light-state captures.

### DEV-003 — Black and gray striped procedural metal

**Status:** fixed-scoped · **Affected:** Twelve original working-place models plus mill suspension

Metal developed regular dark/light bandage-like stripes.

Cause / uncertainty: Fixed-amplitude sine rows and bright border strips in the procedural metal shader.

Fix / next action: Rebuilt the working-place family with restrained proportional non-directional patina; suspension uses the corrected material.

Evidence and verification: development/levels/MILL_REFINEMENT.md; validate_mill_expansion.py and validate_mill_lighting.py independently inspect packed row contrast below 2.5%.

Older-map obligation: Other historical model families require regeneration, source/hash updates and in-room inspection before being called corrected.

### DEV-004 — Workshop bandsaw used as primary sawmill machine

**Status:** fixed-scoped · **Affected:** Blackpine mill and repair shed

Small workshop saw did not fit the logway or sawmill purpose in the supplied reference.

Cause / uncertainty: Wrong machine type/scale for processing full logs.

Fix / next action: Added circular headrig aligned to the log carriage; relocated the small bandsaw to the repair shed.

Evidence and verification: BlackpineMillExpansionTest; native blade/log/arbor and bearing checks in validate_mill_expansion.py.

Older-map obligation: Check purpose and human/material scale whenever industrial models are reused.

### DEV-005 — Timber rack intersects ledger table

**Status:** fixed-scoped · **Affected:** Blackpine timber store

Initial rack placement overlapped an existing furnishing.

Cause / uncertainty: Bounds were not separated in the first candidate placement.

Fix / next action: Moved the rack to X60.4/Z20.75; retain source bounds and measured room coordinates.

Evidence and verification: BlackpineMillExpansionTest independently checks solid furnishing separation; MILL_REFINEMENT.md records the rejected placement.

Older-map obligation: Run furniture/furniture and furniture/wall checks for all old room kits; tabletop items need actual support-part checks.

### DEV-006 — High mill worklights lack appropriate physical supports

**Status:** fixed-scoped · **Affected:** Blackpine mill

Worklight arrangement needed real roof attachment and a below-deck fixture.

Cause / uncertainty: Ceiling-origin fixtures must be supported at their authored height, not floated or floor-normalized.

Fix / next action: Added ceiling plates, suspension rods, crossbar, junction and protected electrical drop; corrected under-loft mount after slab addition.

Evidence and verification: BlackpineLightingTest; validate_mill_lighting.py native contact/pivot checks.

Older-map obligation: Inspect brackets, fixture backs and circuit access at every older site.

### DEV-007 — Added slab blocks final stair approach

**Status:** fixed-scoped · **Affected:** Blackpine mill stair; introduced during DEV-001 fix

Exact-source full manifest at 7951f049 failed at stair ascent step 110 (376 passed, 1 failed), despite prior partial stair fixes.

Cause / uncertainty: Authored loft floor void ended at Z31.30; at step 110 the capsule centre reaches Z31.10 and its 0.22m radius enters solid slab while feet remain 0.25m beneath the deck and beyond the permitted terminal contact band.

Fix / next action: Extend the real visual and physical loft aperture to Z31.60 (10cm beyond ramp end), retain intact deck beyond it, and preserve narrow stair-owned collision exception and railing. Add direct step-110 and bidirectional proof plus native renderer captures.

Evidence and verification: Exact-source main c2810734 full-round run 43148970756 PASS: 378/378 advisory regressions, zero skipped/failures, real 120-step bidirectional ascent/descent, solid off-stair floor, renderer proof, packaged TCP and installed Windows native executable. Repeat native screenshots in run 43157419417 targeted PASS; independently retrieved and visually reviewed PNG artifact 13916144218 (stair approach, railings, loft opening, overhead landing; scene is very dark). v0.1.319 tag points exactly to 6ce17c0c and includes the geometry correction; installer run 43157419403 SUCCESS. Physical two-Windows-PC player navigation/headphone review remains open; this is a scoped correction only.

Older-map obligation: Every structural-depth retrofit must recheck both stair directions and landing transitions.

### DEV-008 — Castle sky variation assertion fails on existing main

**Status:** open · **Affected:** Castle headless integration check

CastleHubIntegrationTest fails its sky color-variation assertion.

Cause / uncertainty: Reproduced on unchanged main db9bb0d during branch unification; not attributed to new map assets.

Fix / next action: Keep open for renderer owner; do not suppress the assertion to claim a full green release.

Evidence and verification: development/verification/branch-unification.json and unification-main-baseline.json.

Older-map obligation: Recheck when the responsible renderer change lands; do not silently drop this known failure.

### DEV-009 — Slow software-renderer capture frames

**Status:** open-measurement · **Affected:** Headless Blackpine capture process

Capture logs report slow scene assembly/raster frames.

Cause / uncertainty: Target-hardware/native OpenGL behavior is not established by headless captures.

Fix / next action: Retain timings and profile native gameplay before claiming acceptable performance.

Evidence and verification: development/levels/verification/blackpine-worklights.json contains actual RENDER SLOW timings.

Older-map obligation: Measure each site on target hardware; do not copy a performance claim from another map.

### DEV-010 — Purchased Tier I flashlight weaker than default

**Status:** fixed-scoped · **Affected:** Shared flashlight optics

Tier I was 6m / .58 while the default was 7.5m / .72.

Cause / uncertainty: Hardware defaults and tier configuration diverged.

Fix / next action: All paid tiers now exceed default reach, power and width: 9.5/12.83/16.63m, .98/1.18/1.40 power.

Evidence and verification: OpticalEvidenceTest and TieredMediaAndPowerTest; unchanged 280/420/630s host batteries.

Older-map obligation: Inspect actual handheld illumination in older sites, not just numeric stats.

### DEV-011 — GPU handheld flashlight omitted from submitted light

**Status:** fixed-scoped · **Affected:** Shared GPU scene submission

CPU raster added the held torch per pixel but GPU vertex submission only included room/portable light.

Cause / uncertainty: Different CPU/GPU lighting paths omitted the same hardware beam.

Fix / next action: GPU vertices now include the shared torch profile; CPU retains one beam contribution.

Evidence and verification: OpticalEvidenceCapture independently compares actual GPU RGB output for default and purchased torch.

Older-map obligation: Native OpenGL hardware and per-map performance still need acceptance.

### DEV-012 — UV only reports room traces; no purple light or surface splats

**Status:** fixed-scoped · **Affected:** UV tool, local solo and LAN

The UV action produced text from room recency without visible inspection light or positioned decals.

Cause / uncertainty: No rendered UV emitter or surface-bound trace representation.

Fix / next action: Added purple cone, bounded host-authored passage marks, geometric reveal, switch/battery state and textured shallow surface cubes.

Evidence and verification: OpticalEvidenceTest covers genuine/absent/expired traces, direction, six mounting normals and texture; OpticalEvidenceCapture verifies real purple/green pixels; LanOpticalEvidenceNetworkTest uses real sockets and tests depletion/privacy.

Older-map obligation: Current producer leaves supported floor residue. Six-axis cube support does not mean wall/ceiling contact producers or teammate UV spill have been implemented; those remain separate refinements.

### DEV-013 — D.O.T.S. generated a body-wide pattern instead of intersecting projector rays

**Status:** fixed-scoped · **Affected:** D.O.T.S. projector

The first correction sampled the entire rounded capsule, producing dots unrelated to the real stationary/panning projector rays.

Cause / uncertainty: Mistook a receiving collision volume for a source of points; user caught the incorrect interpretation.

Fix / next action: Removed all capsule sampling and floating grid dots. Shared source rays now choose exactly one nearest room/prop or front ghost intersection; heading, panning and elevation replicate.

Evidence and verification: DotsRayFieldTest checks transferred wall hit, front side only, blocker priority, miss/range/heading, static Tier I and panning II/III; controlled runtime pair and photographed pixels.

Older-map obligation: Standing capsule approximates collision volume only. Actual native moving-ghost, multi-PC camera and per-site performance acceptance remain pending; do not promote based on headless captures.

### DEV-014 — Laser evidence has text but no visible physical distortion

**Status:** fixed-scoped · **Affected:** LASER_GRID

Device returned generic readings without a rendered beam/ghost intersection effect.

Cause / uncertainty: No spatial grid geometry or optical-contact event.

Fix / next action: Added placed tiered beam fans clipped by architecture, exact horizontal beam/capsule intervals and spirals only within those intervals; host-authorized events, battery state, camera views and photographs.

Evidence and verification: OpticalEvidenceTest rejects non-intersections and checks fixed endpoints; OpticalEvidenceCapture compares straight/distorted pixels and photographs.

Older-map obligation: Per-site and native hardware performance still require review; species/quota/event rules remain authoritative.

### DEV-015 — New laser station falls through to thermometer readout

**Status:** fixed-scoped · **Affected:** Laser station integration, discovered during this pass

After enabling laser station placement, the general station reader would treat it as a thermometer.

Cause / uncertainty: The old reader only distinguished EMF from other pre-existing sensor types.

Fix / next action: Use one laserContact calculation for both optical events and station readings; label laser stations L on the van map.

Evidence and verification: LanOpticalEvidenceNetworkTest now waits for the real SENSOR READ packet and rejects unrelated thermometer output.

Older-map obligation: Audit device-specific dispatch when adding a new shared station type.

### DEV-016 — UTF-8 UI labels corrupted by editing script

**Status:** fixed-scoped · **Affected:** Two local Java edits during this pass

The regression test detected a corrupted pound sign and other non-ASCII labels before publication.

Cause / uncertainty: A Windows-default cp1252 read was written as UTF-8.

Fix / next action: Restored original UTF-8 text and retained only intended source changes; all future scripts use explicit encoding.

Evidence and verification: EquipmentStoreDescriptionsTest reproduced the failure and passes after correction; review the scoped diff for unrelated text changes.

Older-map obligation: Do not rewrite historical Unicode text via platform-default encoding.

### DEV-017 — Pavilion native inspection was too dark to review joins

**Status:** fixed-scoped · **Affected:** Camp Hollow Creek authoring captures

Sun-only inspection left roof underside and timber bearing faces nearly black.

Cause / uncertainty: Open shelter shadows without adequate inspection illumination.

Fix / next action: Add explicitly labeled authoring-only camera area fill; recapture the same native joints. This does not add a gameplay fixture or certify runtime lighting.

Evidence and verification: previews/defects/DEV-017-before-pavilion-join.png and refreshed pavilion-join-native-fitout.png; actual mesh contact and roof-depth validator.

Older-map obligation: Native review images must make the inspected details readable; perform separate real circuit/portable-light runtime reviews.

### DEV-018 — Blender scene reload invalidated an old material handle

**Status:** fixed-scoped · **Affected:** New pavilion builder, before first scene save

Builder raised StructRNA material removed while creating apron strips after opening the prior native file.

Cause / uncertainty: Retained an in-memory material reference across open_mainfile.

Fix / next action: Resolve/create apron material in the newly loaded scene before use; rerun builder and independent source-preservation checks.

Evidence and verification: build_hollow_creek_pavilion.py completes and validate_hollow_creek_pavilion.py reopens both deliveries.

Older-map obligation: Never carry Blender object/material references across a file reload; preserve originals on generation failure.

### DEV-019 — Hollow Creek structures advanced before continuous site terrain

**Status:** fixed-native-only · **Affected:** Camp Hollow Creek authoring order and all future map gates

Tables and pavilion were refined on isolated rough pads and schematic paths before the terrain, shoreline, creek and approach were validated.

Cause / uncertainty: Promoted facility and architecture refinement before the site-foundation gate.

Fix / next action: Preserve authored structures; put terrain, water, drainage, foundation pads, van access and supported circulation first. Add a dedicated site terrain contract and independent surface checks before more facilities.

Evidence and verification: hollow-creek-terrain.json: 10,377 full-width native route samples, 72 foundation samples, actual drainage/bed checks and exact runtime GLB loading; runtime map integration remains pending.

Older-map obligation: Audit terrain/site support first at every older location; never infer route support from graph connectivity or a planning line.

### DEV-020 — First terrain pass left a dry sill across the creek mouth

**Status:** fixed-native-only · **Affected:** Hollow Creek new terrain draft

Review of the water-crossings view exposed an isolated creek strip instead of a connected lake outlet.

Cause / uncertainty: Both creek ends faded to dry ground, including its lake mouth.

Fix / next action: Continue the carved creek bed into the lake; retain the inland headwater transition. Independently sample the actual native mesh along the full wet channel.

Evidence and verification: DEV-020-before-creek-mouth.png, corrected terrain-water-crossings-native-fitout.png and 113 native bed ray checks below water.

Older-map obligation: Check drainage continuity at every channel-to-waterbody junction; a blue surface is not proof of a connected carved bed.

### DEV-021 — Terrain draft exceeded the runtime GLB primitive budget

**Status:** fixed-export-scoped · **Affected:** New Hollow Creek terrain export

Actual game loader rejected the first terrain GLB with Triangle buffer budget, despite native mesh checks passing.

Cause / uncertainty: A whole-site flat-normal/cube-UV primitive exceeded existing per-primitive vertex/index limits.

Fix / next action: Use shared site UVs, spatial primitives and a bounded grid with exact foundation/crossing axes. Respect both per-primitive and 50,000-total-triangle limits; no engine limits changed.

Evidence and verification: HollowCreekTerrainAssetTest now passes on the final exported GLB, with source UVs, upward normals, actual metre bounds and negative bed elevations.

Older-map obligation: Always load new terrain exports through the actual runtime loader; native Blender validity does not prove runtime compatibility.

### DEV-022 — First terrain draft flattened all outdoor pads and trails to Y0

**Status:** fixed-native-only · **Affected:** Hollow Creek terrain draft, user-caught

Initial terrain left the usable site too flat and showed square outdoor reservations as if they were finished areas.

Cause / uncertainty: Preserved every provisional standing Y rather than designing terrain-led grades and fitting facilities.

Fix / next action: Create descending road-to-lake grades, incised narrow treads, irregular shoreline/creek and an irregular boundary contract. Keep only needed foundations level; hide reservation boxes in default review views and refit the existing table/fire ring vertically.

Evidence and verification: Native site/close-trail captures; measured bed range, two shoulder rises, 1.5m van pad and -1.3m lakeshore anchors in terrain report.

Older-map obligation: Audit terrain-led foundation heights and organic outdoor edges before further buildings or detail. Boundary barriers and runtime integration remain separate unfinished gates.

### DEV-023 — New trail grading introduced steep junction and crossing-edge transitions

**Status:** fixed-native-only · **Affected:** Hollow Creek variable-grade terrain iterations

Actual native triangle normals found steep full-width path edges near a foundation corner and crossing abutments.

Cause / uncertainty: Nearest-branch height discontinuities, incision against rigid foundations and soil interpolation at deck ends.

Fix / next action: Blend trail junction profiles, reroute approaches onto dry banks, taper incision into hardscape and align terrain axes/abutments with the decks. Checker gives the deck its exact standing face at coincident boundary hits.

Evidence and verification: Independent native ray checks now pass all 10,377 full-width samples; measured worst local grade 0.192827, below the stated 0.20 authoring threshold. This is not human/runtime traversal acceptance.

Older-map obligation: Check full-width terrain normals and transitions, not graph connectivity or centre lines alone. Preserve failures when adding erosion or changing foundation heights.

### DEV-024 — Rough diagram connectors obscured new terrain review views

**Status:** fixed-native-review-only · **Affected:** Breadth-first terrain previews

Inherited colored route/ritual markers and floating upper floor plates made the terrain overview look like unfinished structure geometry.

Cause / uncertainty: Retained source blockout visualization objects were rendered as if they were terrain infrastructure.

Fix / next action: Preserve all original source objects/files; hide schematic markers and upper layers in default ground overview. Provide separately labeled layer-study captures and engineering-pending metadata.

Evidence and verification: DEV-024-before-terrain-review.png and corrected per-site native terrain overviews; actual retained floor transforms remain tested.

Older-map obligation: Keep terrain reviews readable and distinguish planning markers, foundation plates and actual completed structures.

### DEV-025 — Terrain review missed split floor pieces with numbered names

**Status:** fixed-native-review-only · **Affected:** New terrain-foundation scene filtering

A refresh failed on Floor west.001; inherited floor chunks around openings were not all assigned to their source zone by filename.

Cause / uncertainty: Display/verification filters treated a numbered object name as the authoritative zone ID.

Fix / next action: Use the authored zone property, with a conservative name fallback, for every floor part. Recompute retained-part lists and verify all source floor geometry/placements remain intact.

Evidence and verification: refresh_terrain_foundation_views.py and the per-site native layer checks; original blockouts retain hashes.

Older-map obligation: Floor openings often split a slab into several objects. Ownership must follow authored metadata, never an assumed unique mesh name.

### DEV-026 — Broad terrain pass exposes unresolved grade and crossing support locations

**Status:** open-terrain-refinement · **Affected:** Sixteen remaining rough development sites; six new-location proposal diagnostics tracked separately

Actual native level-link probes flagged 937 sampled support/grade locations; canyon and railway account for the largest groups.

Cause / uncertainty: Existing coarse connector reservations and new first-pass terrain heights still need fine grading, engineered crossings, stair/rock-volume work and exact surface ownership.

Fix / next action: Keep each location and unengineered transition in the per-site verification reports. Prioritize canyon/rail terrain refinement; do not promote broad foundation checks to navigation acceptance or unlock missions.

Evidence and verification: development/levels/verification/terrain-foundations.json and terrain-foundations/<key>/verification.json contain every diagnostic position and scope. Additional new-location-native.json retains 127 terrain-only sampled findings out of 399 probes. These include deck/walkway points and are not unique defects or a swept route acceptance.

Older-map obligation: Apply actual full-width/swept traversal review before detail work; graph connectivity and native floor plates are not proof of a usable route.

### DEV-027 — New lighthouse wall rings stopped short of the next storey

**Status:** fixed-native-rough-only · **Affected:** Morrow Point first rough blockout

Visible horizontal gaps remained between circular wall rings.

Cause / uncertainty: Standing levels were spaced 3.6 m apart while the initial wall height was 3.0 m.

Fix / next action: Extend each authored tower wall ring to the next standing level; regenerate and reopen only the new lighthouse packet.

Evidence and verification: development/levels/previews/defects/DEV-027-before-tower-storey-gaps.png; new-location-native.json measures five continuous wall-level joins; corrected native overview.

Older-map obligation: Audit storey spacing against actual wall top/roof/underside coordinates. A continuous rough ring is not a passed runtime lighting test.

### DEV-028 — New rough connector meshes had no UV layer

**Status:** fixed-native-export-only · **Affected:** Six new-location rough proposal exports

Independent GLB reload failed the all-mesh UV presence check.

Cause / uncertainty: The old rough route-strip generator authored position faces without texture coordinates.

Fix / next action: Before saving/exporting new terrain-tagged proposals, add finite metre-plan UVs to meshes lacking a UV layer; retain existing UVs and old maps.

Evidence and verification: development/quality/evidence/DEV-028-native-uv-before.txt records the initial failure; independent rerun development/levels/verification/new-location-native.json passes all six actual GLB UV/finite geometry checks.

Older-map obligation: Audit legacy route-strip UVs before applying textured finishes. Presence of UVs alone does not establish correct material scale or finish quality.

### DEV-029 — Shared procedural hill silhouette conflicts with site-specific open horizons

**Status:** open-runtime-integration · **Affected:** All-map background silhouette; coastal/wetland/new high-view sites require individual review

SceneRenderer.paintSky unconditionally paints a trigonometric hill silhouette for all map backgrounds, including proposed open-water views.

Cause / uncertainty: One common horizon composition has no map-specific sector masks or horizon profile.

Fix / next action: Per-map boundary/sky plans now reserve distinct water, field, roof, woodland, mesa and mountain sectors. Coordinate a separately tested renderer integration; no engine code changed in this planning batch.

Evidence and verification: src/main/java/org/beckenburg/SceneRenderer.java paintSky; development/levels/edge-sky-plans/*/plan.json and per-map planning diagrams.

Older-map obligation: Audit 360-degree ground and highest-window/tower views, CPU/GPU depth behavior, sky leaks and weather parity before claiming a finished skyline.

### DEV-030 — Legacy model-worklist generator assumed the original fixed roster

**Status:** fixed-authoring-tool · **Affected:** Model requirements catalog maintenance after adding WLD-14 through WLD-18

Source inspection found unconditional global regeneration, a SPECIAL lookup limited to the original maps and a final 23-map assertion; rerunning would overwrite curated packets or fail on new IDs.

Cause / uncertainty: The legacy generator combined initial worklist creation and catalog refresh into one unguarded fixed-roster script.

Fix / next action: Make the public entry point refresh/validate only the catalog from existing worklists. Preserve authored JSON/Markdown; support original and new room schemas and separate annexes. Disable legacy global regeneration.

Evidence and verification: development/levels/model-requirements/refresh_catalog.py; build_requirements.py now reports 28 base maps and one additive expansion; source-hash and per-room counts checked.

Older-map obligation: Do not globally regenerate curated worklists to append a map. Refresh catalogs from actual packets; retain source references and existing map geometry.

### DEV-031 — First land boundary read as a uniform wall in an underlit review

**Status:** fixed-native-geometry-art-review-open · **Affected:** Hollow Creek first boundary draft

Continuous steep core, block stones and dark inspection framing hid natural edge fitness.

Cause / uncertainty: Rough authoring assumptions were not sufficient; independent geometry and ordinary-view review exposed the issue.

Fix / next action: Slope the bank profile, add embedded rocks/rooted growth and retain inspection lighting. Further fine art review remains open.

Evidence and verification: development/levels/previews/defects/DEV-031-before-uniform-boundary.png

Older-map obligation: Audit this category on each older map; a corrected Hollow Creek derivative or passing source check does not establish other-map acceptance.

### DEV-032 — Lowered service-bank transition left uncovered land-boundary samples

**Status:** fixed-native-land-only · **Affected:** Hollow Creek boundary authoring

Initial native check flagged 69 ray locations. Fence selection skipped a segment when only part of it crossed the lower-bank threshold.

Cause / uncertainty: Rough authoring assumptions were not sufficient; independent geometry and ordinary-view review exposed the issue.

Fix / next action: Clip fence spans at the actual transition and overlap the lower bank. Final checks pass 1392 land/gate rays and 385 capsule-contact crossings. Two deliberately open shoreline interfaces remain explicit, with exact coplanar end-cap rays excluded and capsule contact retained.

Evidence and verification: development/quality/evidence/DEV-032-boundary-native-before.json; development/levels/verification/hollow-creek-boundaries.json

Older-map obligation: Audit this category on each older map; a corrected Hollow Creek derivative or passing source check does not establish other-map acceptance.

### DEV-033 — Organic clearing/deck changes produced five steep native route samples

**Status:** fixed-native-only · **Affected:** Hollow Creek desquareification iteration

Ellipse plateau incision and soil above a creek deck produced five sampled grades above 0.20, peaking at 0.27609.

Cause / uncertainty: Rough authoring assumptions were not sufficient; independent geometry and ordinary-view review exposed the issue.

Fix / next action: Fade incision near the actual arrival pad, remove forced flattening in unbuilt clearings, and keep soil below the deck. All 10377 native route samples now pass; worst grade 0.1928266.

Evidence and verification: development/quality/evidence/DEV-033-organic-grade-before.json; development/levels/verification/hollow-creek-organic.json

Older-map obligation: Audit this category on each older map; a corrected Hollow Creek derivative or passing source check does not establish other-map acceptance.

### DEV-034 — Planning rectangles survived in landscape pads and loose perimeter

**Status:** fixed-native-derived-scene-only · **Affected:** User-caught Hollow Creek terrain/blockout carryover

Unbuilt building-zone pads and oversized square aprons remained visible; the boundary still broadly wrapped a rectangle.

Cause / uncertainty: Rough authoring assumptions were not sufficient; independent geometry and ordinary-view review exposed the issue.

Fix / next action: Require DESQUAREIFY_GATE for every map. Derive terrain with organic clearings and a narrowed arrival neck, remove oversized aprons, retain only the actual 14m physical slab. Validate real cropped terrain margins. Older maps remain unaudited.

Evidence and verification: development/quality/DESQUAREIFY_GATE.md; hollow-creek/organic-assets.json; before/after terrain and organic site captures

Older-map obligation: Audit this category on each older map; a corrected Hollow Creek derivative or passing source check does not establish other-map acceptance.

### DEV-035 — Canonical van was omitted from native campground arrival review

**Status:** fixed-native-review-only · **Affected:** User-caught Hollow Creek native scene

Arrival point was recorded in coordinates but the actual vehicle was absent from the scene.

Cause / uncertainty: Rough authoring assumptions were not sufficient; independent geometry and ordinary-view review exposed the issue.

Fix / next action: Export the exact 4968 runtime van triangles with their UVs/materials and place them at X32/Y1.5/Z78/yaw0. Do not substitute a placeholder envelope or rebuilt vehicle.

Evidence and verification: src/test/java/org/beckenburg/HollowCreekVanSource.java; hollow-creek/organic-assets.json; forest-van-and-roadhead-native-fitout.png

Older-map obligation: Audit this category on each older map; a corrected Hollow Creek derivative or passing source check does not establish other-map acceptance.

### DEV-036 — New authoring tools had failed initial smoke-check assumptions

**Status:** fixed-authoring-tools · **Affected:** Boundary/organic model pipeline

A missing random import, reused loop-count/normal variable, and a whole-site bound assertion incorrectly applied to a short fence interrupted initial authoring checks.

Cause / uncertainty: Rough authoring assumptions were not sufficient; independent geometry and ordinary-view review exposed the issue.

Fix / next action: Fix imports/variable naming and validate the service fence against its actual narrowed arrival extent. Run native saved-scene checks and all eight real GLB loads.

Evidence and verification: build_hollow_creek_boundaries.py; validate_hollow_creek_organic.py; HollowCreekBoundaryAssetsTest.java

Older-map obligation: Audit this category on each older map; a corrected Hollow Creek derivative or passing source check does not establish other-map acceptance.

### DEV-037 — Coarse tree crowns and surface breakup did not meet requested art quality

**Status:** open-art-refinement · **Affected:** User-caught campground models/textures

Stacked cones, rounded replacement canopy lumps and coarse material breakup were visibly unsuitable as finished foliage.

Cause / uncertainty: Rough authoring assumptions were not sufficient; independent geometry and ordinary-view review exposed the issue.

Fix / next action: Replace rounded pine masses with attached fine branch/needle sprays, increase original diffuse detail, add supplemental alpha crowns on solid trunks, and add protected close ferns. Preserve rejected rounded-canopy evidence. Continue reviewing broadleaf/shrub/rock art and runtime alpha/LOD behavior; do not call the whole art pass accepted.

Evidence and verification: development/levels/previews/defects/DEV-037-before-rounded-pine-crowns.png; hollow-creek/foliage-detail.json; current native captures

Older-map obligation: Audit this category on each older map; a corrected Hollow Creek derivative or passing source check does not establish other-map acceptance.

### DEV-038 — Initial tent entries and bedding were not fit for the authored crouch body

**Status:** fixed-native-campsite-only · **Affected:** First Hollow Creek tent/bedding draft

Initial ridge door matched the 1.05m crouch height without head margin; one dome pole crossed the entrance; a bedding corner extended past the actual inset floor.

Cause / uncertainty: Tent silhouette and rough floor envelope were not enough to establish a usable entry or true sleeping-mat support.

Fix / next action: Raise the ridge entry to 1.20m and ridge to 1.90m, move dome poles onto diagonal support arcs, author an oval inset floor and refit the existing bedroll entirely onto it. Validate the current body capsule, native floor faces and mat/roll underside contact.

Evidence and verification: development/levels/previews/defects/DEV-038-before-dome-entry-pole.png; development/quality/evidence/DEV-038-before-tent-fit.json; DEV-038-first-native-tent-check.json; development/levels/verification/hollow-creek-tents.json

Older-map obligation: Measure real body/door and support-part clearance. Do not infer usable entrances from an open-looking model or use an overall bounding box as its floor.

### DEV-039 — Unused canopy vertices overstated the native source bounds

**Status:** fixed-authoring-export · **Affected:** New dome tent source mesh and metadata

Source bounds included unreferenced vertices in the cut doorway that were correctly absent from actual GLB triangles. An initial origin assertion also assumed symmetric guys despite intentionally omitting the doorway guy.

Cause / uncertainty: The cut canopy retained orphan vertices, while the metadata helper measured every vertex rather than only actual faces.

Fix / next action: Remove 21 orphan vertices, merge the canopy apex/closed seams before solidifying, and validate real GLB triangle bounds plus the actual floor component. Retain the intentional floor pivot instead of re-centering an asymmetric footprint.

Evidence and verification: build_hollow_creek_tents.py source_geometry_cleanup; tents.json actual bounds; HollowCreekTentAssetsTest and native floor/shell checks.

Older-map obligation: Clean orphan geometry and verify bounds against actual exported faces. Functional floor pivots may differ from the centre of asymmetric anchors or accessories.

### DEV-040 — Shortest tent approaches crossed steep shoulders and guyline clearance

**Status:** fixed-native-contour-approaches-only · **Affected:** Hollow Creek west/east campsite placement iteration

Full-width native probes found 15 approach grade samples above 0.20 (maximum 0.31169); a later contour draft exposed five lateral body/guyline contacts.

Cause / uncertainty: Centre-line access and proximity to an existing path were insufficient on the actual terrain and anchored model footprints.

Fix / next action: Author a western contour approach around the pitched tent, shift the eastern approach beyond the steep edge, clear only the affected decorative ferns, and test 3912 standing body samples plus 510 tread samples. Final maximum approach grade is 0.1843600; no terrain source or grade threshold was changed.

Evidence and verification: development/quality/evidence/DEV-040-approach-grades-before.json; DEV-040-corridor-guylines-before.json; hollow-creek/tents.json approaches and verification/hollow-creek-tents.json.

Older-map obligation: Check full tread width and body envelope around guy ropes, pegs, terrain transitions and entries. Actual runtime movement, AI fairness and wear/approach artwork remain separate gates.

### DEV-041 — Gallery readback aborted on a transient Pages 503

**Status:** fixed-verification-tool; final live rerun passed · **Affected:** Nightly public gallery verification

The first 602-image live readback failed with HTTP 503 during an image request even though Pages was built and all seven published model/native source hashes matched.

Cause / uncertainty: The verifier had no per-URL transient retry and the legacy failure did not identify the affected image URL.

Fix / next action: Retry transient 429/5xx/network failures at most four times per URL, respect short Retry-After headers, defer long server backoff, log successful-run retry events and include the path on fatal failures. Keep all actual PNG/hash assertions. Final rerun verified 602 images and 156 full hashes.

Evidence and verification: tools/verify_development_gallery.py; development/verification/live-development-gallery.json; initial recorded failure excerpt in development/quality/evidence/DEV-041-gallery-503-before.txt

Older-map obligation: A transient service error does not invalidate local implementation or prove remote publication failed. Preserve checkpoints and separately verify source bytes and final public image delivery.

### DEV-042 — Shore crop and scenery returns left a jagged lake-facing seam

**Status:** fixed-native-only · **Affected:** Hollow Creek coast and forest apron

User-caught jagged shoreline; an intermediate linear shore return created a triangular projecting ledge.

Cause / uncertainty: Terrain cell culling and scenery-apron termination were authored independently.

Fix / next action: Restore 1594 missing coast triangles, match the retained apron edge, clip the west return at the terrain grid and fit its inner edge to actual terrain. Preserve old terrain checkpoints.

Evidence and verification: development/levels/previews/defects/DEV-042-before-incomplete-shore-return.png; verification/hollow-creek-dock.json

Older-map obligation: Audit each older location individually for this category; scoped native fixes or shared-policy tests do not establish full other-map gameplay/art acceptance.

### DEV-043 — Boat ends were open and corner posts pierced shed roof

**Status:** fixed-native-assets-only · **Affected:** Original dock shed and rowboat first draft

Uncapped bow/stern and visible timber post tips on roof top.

Cause / uncertainty: Silhouette authoring omitted end-wall closure and post-to-roof elevation check.

Fix / next action: Close both double-skin hull ends and shorten corner posts below roof top; adjust actual rowboat draft and mooring endpoints.

Evidence and verification: development/levels/previews/defects/DEV-043-before-open-boat-ends.png; DEV-043-before-roof-post-tips.png; current actual GLB reimports

Older-map obligation: Audit each older location individually for this category; scoped native fixes or shared-policy tests do not establish full other-map gameplay/art acceptance.

### DEV-044 — Camp furnishings encroached on protected walking routes

**Status:** fixed-native-placement-only · **Affected:** First folding-chair/cooler arrangement

Nineteen body samples touched furniture on L09/L10.

Cause / uncertainty: Visual placement near a table was insufficient to prove lateral route width.

Fix / next action: Move two chairs and a cooler; retain full route/body threshold. Reopen saved scene and rerun all 10377 protected route samples.

Evidence and verification: development/quality/evidence/DEV-044-first-dock-detail-check.json; verification/hollow-creek-dock.json

Older-map obligation: Audit each older location individually for this category; scoped native fixes or shared-policy tests do not establish full other-map gameplay/art acceptance.

### DEV-045 — Additional campsite floors and entries needed actual terrain fit

**Status:** fixed-native-tests; complete approach grading pending · **Affected:** Expanded Hollow Creek tents/toilet ramps

First expansion exposed path/guy collisions, nonplanar dome-floor gaps, bedding support errors and one steep entry.

Cause / uncertainty: An ngon floor cannot conform internally by moving its boundary alone. Terrain crop edges and exact floating-point ramp boundaries also needed explicit authoring/check handling.

Fix / next action: Move tents clear of paths, triangulate/subdivide real floors before fitting, preserve bedding lift and relocate the final dome onto usable ground. Reject shrub footprints outside terrain. Check ramp tread just inside its physical boundary without relaxing grade limit.

Evidence and verification: development/quality/evidence/DEV-045-first-amenities-check.json; verification/hollow-creek-amenities.json

Older-map obligation: Audit each older location individually for this category; scoped native fixes or shared-policy tests do not establish full other-map gameplay/art acceptance.

### DEV-046 — Creek rendered as a black slot despite material changes

**Status:** fixed-native-water-overlap-only · **Affected:** Hollow Creek lake/creek preview water

Changing color left an almost black rectangular water strip with narrow colored edges.

Cause / uncertainty: The expanded lake and separate creek surface were coplanar and overlapping.

Fix / next action: Remove the duplicate creek surface in the derived scene and retain one continuous native water surface. Layered/translucent runtime water remains unimplemented.

Evidence and verification: development/levels/previews/defects/DEV-046-before-culvert-skin-and-water.png; current culvert capture; duplicate-surface native assertion

Older-map obligation: Audit each older location individually for this category; scoped native fixes or shared-policy tests do not establish full other-map gameplay/art acceptance.

### DEV-047 — Culvert outlet was recessed and obscured by terrain

**Status:** fixed-native-bore-only · **Affected:** First culvert headwall and excavated terrain

User correctly observed the outlet too far uphill and a green terrain plug obstructing its bore.

Cause / uncertainty: Circular mesh presence and a face-count cut did not prove a visible flow path through the actual terrain.

Fix / next action: Move outlet two metres toward creek, refine the local terrain mesh, excavate the approach bed and cut the complete bore footprint. Reopen and pass nine terrain sight rays through the bore; retain the recessed debris grate.

Evidence and verification: development/levels/previews/defects/DEV-047-before-recessed-culvert.png; current culvert capture; verification/hollow-creek-amenities.json

Older-map obligation: Audit each older location individually for this category; scoped native fixes or shared-policy tests do not establish full other-map gameplay/art acceptance.

### DEV-048 — Hiding availability differed by map and combined indoor/outdoor pools

**Status:** fixed-runtime-built-definitions; remaining authoring audits open · **Affected:** Ten built investigation definitions and future maps

House/farm/school cupboard pools and three Blackpine refuges had independent density hashes; other built templates retained inert recess placeholders.

Cause / uncertainty: No shared combined maximum or cross-map exact-count check existed.

Fix / next action: Use HidingBudget maximum3, floor percentage count and one monotonic indoor/outdoor candidate pool. Add measured cupboard bays on previously inert templates through existing clearance placement. Verify all ten built definitions at all101 settings, body fit, solo and host hiding tests. Future maps must author three suitable candidates.

Evidence and verification: HidingBudgetTest.java; VanHidingTest; CrouchAndOutdoorHidingTest; development/quality/HIDING_SPOT_BUDGET.md

Older-map obligation: Audit each older location individually for this category; scoped native fixes or shared-policy tests do not establish full other-map gameplay/art acceptance.

### DEV-049 — Ordinary planting reused the special refuge shrub model

**Status:** fixed-native-decorative-separation · **Affected:** First approach shrub expansion

User rejected many visual refuge shrubs even where a decorative flag disabled hiding.

Cause / uncertainty: Semantic flags alone did not provide a distinct ordinary shrub asset family.

Fix / next action: Create two original leafy decorative shrubs, replace all20 ordinary placements, and reserve only three separately named proposed refuge sites. Remove duplicate reverse leaf faces that generated an export warning; actual clean GLB reimports pass.

Evidence and verification: level_camp_shrub_decorative_a/b sources and studio captures; amenities.json proposed_refuges; native separation assertion

Older-map obligation: Audit each older location individually for this category; scoped native fixes or shared-policy tests do not establish full other-map gameplay/art acceptance.

### DEV-050 — Waterfront and dock blockers are not yet runtime movement constraints

**Status:** open-runtime-integration · **Affected:** Hollow Creek lake, creek, culvert, bridge and dock

User asked whether players are actually prevented from entering water or stepping off dock; current native scene cannot guarantee this.

Cause / uncertainty: Hollow Creek map geometry is not yet connected to runtime player/ghost collision.

Fix / next action: Author250 hidden closed barrier volumes, bridge-side protection, shore/berm/dock/creek-mouth joins and clear crossings.1250 actual native rays pass. Integrate into the eventual level collision authority and test swept bodies, multiplayer and movement before claiming working barriers.

Evidence and verification: amenities.json blockers; verification/hollow-creek-amenities.json; expanded review explicit NOT INTEGRATED status

Older-map obligation: Audit each older location individually for this category; scoped native fixes or shared-policy tests do not establish full other-map gameplay/art acceptance.

### DEV-051 — Global bevel exceeded the unchanged rowboat mesh budget

**Status:** fixed-refinement-export · **Affected:** Second Hollow Creek model pass

First global three-segment bevel produced77037 rowboat triangles; authoring assertion stopped export.

Cause / uncertainty: Applying bevel indiscriminately to thin curved hulls and many segmented rods multiplied topology without improving the silhouette.

Fix / next action: Keep real curved hull geometry, soften curve normals, and apply small radii to appropriate construction edges only. All20 final refined assets pass unchanged50k triangle/4MB caps.

Evidence and verification: refine_hollow_creek_models.py assertion; HollowCreekRefinementAssetsTest; actual native GLB reimports

Older-map obligation: Audit this failure category on each older model/site; refined source/geometry checks are not full runtime/human acceptance.

### DEV-052 — Smoothing exposed flat-panel shading smear and overlapping boat end caps

**Status:** fixed-refined-assets-only · **Affected:** Refined masonry/metal panels and rowboat ends

Broad panels looked warped; boat cap faces rendered black in actual studio captures.

Cause / uncertainty: Unselective smooth normals blurred planar surfaces. The earlier closed-end draft had coplanar inner/outer cap faces and redundant curved end strips.

Fix / next action: Keep axis-aligned panels/caps flat while smoothing actual curves. Rebuild boat ends with50mm separation, remove redundant end strips and close top rim. Replace low-frequency sinusoidal weather marks with nondirectional interpolated noise; add recessed front flue stone courses without changing roof clearance.

Evidence and verification: development/levels/previews/defects/DEV-052-before-refined-boat-end-overlap.png; DEV-052-before-panel-normal-smearing.png; final actual source/GLB captures

Older-map obligation: Audit this failure category on each older model/site; refined source/geometry checks are not full runtime/human acceptance.

### DEV-053 — Refined instance assignment duplicated empty support-group definitions

**Status:** fixed-native-authoring-metadata · **Affected:** Second-pass scene and chair support verifier

A legacy point comparison tested zero chair-foot vertices; component review then found empty duplicate group names, causing30 strict contact-check findings.

Cause / uncertainty: Blender carried mesh group definitions during data replacement; the authoring loop added a second name set. The legacy test silently relied on pre-bevel vertex positions.

Fix / next action: Reuse existing source group definitions, remove21626 unused duplicate definitions from the current derived scene, and verify each actual leg group instead of old vertex positions. Final960 real contact vertices cover24 chair legs. Geometry/transforms did not change during metadata cleanup.

Evidence and verification: development/quality/evidence/DEV-053-empty-group-refinement-check.json; verification/hollow-creek-refinement-group-cleanup.json; hollow-creek-refined-dock.json

Older-map obligation: Audit this failure category on each older model/site; refined source/geometry checks are not full runtime/human acceptance.

### DEV-054 — Detail parts needed actual support rather than plausible overall bounds

**Status:** fixed-refined-parts-only · **Affected:** Hollow Creek fireplace, picnic-table fixings and cold fire ring

Detailed review found split firewood below rather than resting on fireplace grate, new tabletop bolt heads away from bearing rails, and a small ash/log contact gap.

Cause / uncertainty: Overall model silhouette and bounding boxes did not establish support by the intended subcomponent.

Fix / next action: Place split wood on actual grate bars, align table fixings to measured rails at XÃ‚Â±.5, lower charred timber onto actual ash. Preserve clean sources. Actual ray checks pass for three grate/log contacts, four tabletop/bearing fixings and three ash/log contacts.

Evidence and verification: development/levels/previews/defects/DEV-054-before-firewood-grate-support.png; verification/hollow-creek-refined.json; hollow-creek-refinement-parts.json

Older-map obligation: Audit actual support parts on every older source/placement; this refined-only correction does not establish unaudited other-map acceptance.

### DEV-055 — Delivery verifier compared Git-normalized text to Windows worktree bytes

**Status:** fixed-verification-comparison · **Affected:** Refined source publication verification

Binary source/model downloads passed, but refined-models.json failed an exact raw hash comparison.

Cause / uncertainty: Git stores normalized LF text while the Windows worktree contains translated CRLF; the verifier incorrectly treated worktree byte identity as commit identity.

Fix / next action: Compare every actual download to the immutable committed Git blob with git show commit:path. Keep exact length/hash assertions for every file. Final46 actual source/export/review downloads match the committed bytes.

Evidence and verification: verification/hollow-creek-refined-delivery.json; final immutable GitHub download verification

Older-map obligation: Use committed bytes for remote delivery checks when text normalization applies. Preserve raw binary equality checks; do not waive real source/export mismatch.

### DEV-056 — Uniform surface wear and oversized timber grain survive construction refinement

**Status:** improved-authored-family-only · **Affected:** Hollow Creek refined model family

Current model review still reads as crude: repetitive timber stripes and uniform plastic/stone/metal surfaces.

Cause / uncertainty: Shared low-resolution UV-local weather marks do not describe part direction or physical exposure.

Fix / next action: Create14 separate authored surface variants with part-aligned grain, physical lower-edge dirt/damp, exposure fade, sparse oxidation/corner wear and localized fireplace soot. Preserve prior geometry/smoothing. Tent, shrub, creek and cable families retain previous refinement and further human art review remains open.

Evidence and verification: previews/defects/DEV-056-before-uniform-surfaces-native-fitout.png; hollow-creek/AUTHORED_REVIEW.html; verification/hollow-creek-authored-surfaces.json

Older-map obligation: Audit this failure category independently on older maps/assets. This scoped native/atlas pass does not certify their geometry, lighting, performance or gameplay.

### DEV-057 — New weather atlas initially used unsupported UV channel and legacy vertical origin

**Status:** fixed-new-authored-family-only · **Affected:** 14 new Hollow Creek authored runtime exports

First exported shed GLB selects texCoord1 while the reader consumes TEXCOORD_0. Inspection also found that the legacy non-vehicle image-row convention would invert a spatially authored glTF atlas.

Cause / uncertainty: Blender preserves original UVs as channel0; default non-vehicle sampler uses the opposite vertical origin.

Fix / next action: Preserve all source UVs in editable blends; use a temporary mesh copy with atlas-only UV0 for GLB. Apply glTF top-left image origin only to the curated14 authored IDs. Check1008 triangle-centroid UV/color samples independently decoded from actual GLB/PNG data.

Evidence and verification: tools/hollow_creek_authored_pixel_reference.py; HollowCreekAuthoredSurfacesTest; verification/hollow-creek-authored-pixels.tsv

Older-map obligation: Audit this failure category independently on older maps/assets. This scoped native/atlas pass does not certify their geometry, lighting, performance or gameplay.

### DEV-058 — Blender RNA handles invalidated by attribute and UV-layer allocation

**Status:** fixed-authoring-pipeline · **Affected:** New surface authoring script

Initial authoring attempt caused EXCEPTION_ACCESS_VIOLATION; deleting UV layers through stored RNA handles later reported UV map .uv_select_vert not found.

Cause / uncertainty: Allocating/removing custom-data layers can invalidate retained RNA wrappers.

Fix / next action: Re-fetch attributes after both allocations; remove UV maps by saved names rather than stale wrappers. Correct Blender5 geometry node identifier. Full14-family build and independent native review now complete.

Evidence and verification: author_hollow_creek_surfaces.py; review_hollow_creek_authored.py; verification/hollow-creek-authored-surfaces.json

Older-map obligation: Audit this failure category independently on older maps/assets. This scoped native/atlas pass does not certify their geometry, lighting, performance or gameplay.

### DEV-059 — Pending native physics Java generator emitted extra closing parentheses

**Status:** corrected-local-pending-integration · **Affected:** Uncommitted Hollow Creek physics-export work

Compiling all current main sources found three semicolon-expected syntax errors at final Zone, Collider and Link entries.

Cause / uncertainty: The generator closed the constructor and then emitted two additional closing parentheses for List.of.

Fix / next action: Correct the pending generator and generated Java locally; full current-source JDK17 compilation passes. Physics exporter/binary/contract remain separate uncommitted integration work, preserved and not represented as working containment.

Evidence and verification: development/levels/export_hollow_creek_physics.py; pending HollowCreekContract.java; dated surface handoff

Older-map obligation: Audit this failure category independently on older maps/assets. This scoped native/atlas pass does not certify their geometry, lighting, performance or gameplay.

### DEV-060 — Existing GGBot vehicle color-count assertion fails on baseline loader

**Status:** open-baseline-regression · **Affected:** GgBotBlenderSourceUvTest with current ggbot_car01 source

Vehicle source regression test fails its original128px textured color atlas assertion.

Cause / uncertainty: Cause unconfirmed. Reproduced with the pre-change committed GlbProps.java compiled separately; the same ggbot_car01 assertion fails.

Fix / next action: Retain the assertion and failing baseline evidence. Audit separately; do not alter vehicle art, lower its threshold or claim this regression passed. Eight legacy equipment tests and14 new authored atlas loads pass.

Evidence and verification: quality/evidence/DEV-060-baseline-vehicle-uv.txt; verification/hollow-creek-authored-runtime.json

Older-map obligation: Audit this failure category independently on older maps/assets. This scoped native/atlas pass does not certify their geometry, lighting, performance or gameplay.

### DEV-061 — Diffuse bake double-applied the source palette transfer function

**Status:** fixed-authored-bake-color-space · **Affected:** First third-pass surface bake

Matched fireplace/pavilion native capture became pale, clean-looking and washed out despite the intended aging pass.

Cause / uncertainty: Encoded source-image averages were assigned directly to linear shader-color inputs.

Fix / next action: Decode source sRGB palette averages and authored tint colors into linear space before emission baking. Rebuild all14 actual exports, rerender all native/studio views and rerun1008 runtime PNG/UV comparisons. Preserve failing image.

Evidence and verification: previews/defects/DEV-061-before-bake-palette-washout.png; hollow-creek/AUTHORED_REVIEW.html

Older-map obligation: Audit this failure category independently on older maps/assets. This scoped native/atlas pass does not certify their geometry, lighting, performance or gameplay.

### DEV-062 — Dome canopy opening retains whole-cell staircase

**Status:** fixed-tailored-source-and-eight-tent-native-pass · **Affected:** Hollow Creek dome tent

Close-up shows a stepped/angular cutout instead of a continuous sewn entrance.

Cause / uncertainty: Original canopy removed complete latitude/longitude grid faces for the opening.

Fix / next action: Author a continuous arched boundary, constrained interior triangulation, joined front/rear curved shell and surface-fitted binding. Retain floors/frame/anchors and rerun actual crouch entry.

Evidence and verification: previews/defects/DEV-062-before-stepped-dome-aperture.png; verification/hollow-creek-tailored-shape.json; hollow-creek/TAILORED_REVIEW.html

Older-map obligation: Audit the same topology, transform, actual support and positive-presence category on each older map/model independently. This native source correction is not full gameplay, lighting, LAN or performance acceptance.

### DEV-063 — Projected hemisphere seam produced overlapping shell rim faces

**Status:** fixed-tailored-dome-topology · **Affected:** Intermediate tailored dome canopy

Actual shell audit found16 edges with four adjacent faces at the joined hemisphere seam.

Cause / uncertainty: Floating-point ellipse projection left tiny nonzero Y values at the shared boundary, preventing a clean weld before solidification.

Fix / next action: Set the mathematical shared boundary to Y0, weld before solidification and check all actual shell edges have exactly two adjacent faces. Final canopy has no open/three/four-face edges. Constrained interior sampling replaces the stretched triangle fan.

Evidence and verification: verification/hollow-creek-tailored-shape.json; review_hollow_creek_fabric.py; tailored native/studio captures

Older-map obligation: Audit the same topology, transform, actual support and positive-presence category on each older map/model independently. This native source correction is not full gameplay, lighting, LAN or performance acceptance.

### DEV-064 — Independent floor verifier rebuilt the same vertex set inside its loop

**Status:** fixed-verifier-complexity · **Affected:** Fabric scene review script

Initial floor comparison spent unbounded useful time recalculating membership per vertex instead of reaching visual review.

Cause / uncertainty: A set-producing component query was nested inside the floor-vertex iteration.

Fix / next action: Precompute selected vertices once per mesh, then compare exact saved positions. Stop only the confirmed task-specific Blender process and restart corrected review. Final review reaches six native captures and exact232864-vertex check.

Evidence and verification: review_hollow_creek_fabric.py; verification/hollow-creek-tailored-shape.json

Older-map obligation: Audit the same topology, transform, actual support and positive-presence category on each older map/model independently. This native source correction is not full gameplay, lighting, LAN or performance acceptance.

### DEV-065 — Matrix decomposition perturbed an exact fitted tent rotation

**Status:** fixed-preserving-original-transform-components · **Affected:** Additional pitched tent3 and equivalent pi rotations

Independent world-space floor comparison failed even though all2648 local floor vertices matched exactly.

Cause / uncertainty: Reassigning matrix_world decomposed +pi into a slightly different -pi float rotation.

Fix / next action: Copy original location/rotation/scale components directly to inserted geometry; retain those exact components on the existing object. Do not widen the equality check. All232864 floor vertices across8 tents now match exact world positions.

Evidence and verification: quality/evidence/DEV-065-before-transform-decomposition.txt; verification/hollow-creek-tailored-shape.json

Older-map obligation: Audit the same topology, transform, actual support and positive-presence category on each older map/model independently. This native source correction is not full gameplay, lighting, LAN or performance acceptance.

### DEV-066 — Replacement canopies buried by a stale dependency transform

**Status:** fixed-with-positive-native-roof-presence-gate · **Affected:** Initial tailored native assembly

Native capture shows a groundsheet and bedding with the whole canopy below ground; floor/route/door clearance negatives had passed.

Cause / uncertainty: New object transform components were assigned but matrix_world was read for terrain conformance before dependency evaluation updated it.

Fix / next action: Update the view layer before fitting. Add72 positive roof/floor rays over all8 tents, retaining exact floor positions. Rerun actual crouch, routes, bedding and visuals after the correction. Require positive model presence as well as negative overlap/clearance tests on future placements.

Evidence and verification: previews/defects/DEV-066-before-buried-canopy-native-fitout.png; verification/hollow-creek-tailored-shape.json; verification/hollow-creek-tailored-fit.json

Older-map obligation: Audit the same topology, transform, actual support and positive-presence category on each older map/model independently. This native source correction is not full gameplay, lighting, LAN or performance acceptance.

### DEV-067 — Retained native tent floors carry excessive authoring tessellation

**Status:** fixed-native-floor-topology-runtime-performance-open · **Affected:** Hollow Creek eight terrain-conformed tent floors

Native inventory measures465696 retained floor triangles out of1500410 render-visible native triangles, despite small tent footprints.

Cause / uncertainty: Earlier floor conformance subdivided many source polygons densely instead of clipping/refitting to the actual terrain triangle boundaries.

Fix / next action: Before runtime acceptance, replace excessive fitted-floor topology with separately validated terrain-conforming geometry, retaining the real ground support, depth, boundaries, entry/bedding fit and shared render/collision coordinates. Preserve current source; do not raise asset/loader caps or claim native counts prove runtime FPS. Current fitted-floor checkpoint replaces465696 floor triangles with2788 by clipping actual terrain triangles to the original4/64-edge source footprints. Eight fitted assemblies export under unchanged caps;50776 dense terrain probes, exact source/GLB triangle equality and native fit checks pass. Actual runtime FPS/LOD remains unmeasured.

Evidence and verification: verification/hollow-creek-tailored-geometry-cost.json; profile_hollow_creek_geometry.py; final tailored handoff

Older-map obligation: Profile older terrain-conformed meshes independently. Count native geometry separately from actual renderer culling/LOD/FPS evidence.

### DEV-068 — Original west rolled bedding intersects sloping tent canvas

**Status:** fixed-native-placement-and-all-seven-bag-audit · **Affected:** West pitched ridge bedding and fabric inspection camera

The close-up shows part of the rolled bag protruding through the sloping canvas; the camera outside that wall also obscures the intended inspection.

Cause / uncertainty: Floor support alone did not establish clearance from the low sloping roof. The parent tent also cannot be inferred from matching numeric suffixes: authored tent names are1-based and bag names0-based.

Fix / next action: Move only the west rolled bag22cm inward from X7.9 to8.12, refit its standing height to the same exact floor, and aim the camera through the doorway. Resolve parent tents from the authored placement packet. Check40936 actual bag-vertex/roof rays and21 actual floor-contact rays across all7 bags; minimum canopy clearance is0.202m. Tent floors/frame/anchors remain intact.

Evidence and verification: previews/defects/DEV-068-before-bedding-through-canvas-native-fitout.png; verification/hollow-creek-tailored-shape.json; hollow-creek/tailored-fabric.json placement_corrections; final bedding native capture

Older-map obligation: Audit supported props against sloping walls/roofs and full usable volume as well as their supporting floor/table. Keep inspection cameras outside opaque obstructions.

### DEV-069 — Full multiplayer CI stops at stale equipment manifest coverage totals

**Status:** open-existing-baseline-test-mismatch · **Affected:** EquipmentAssetAssignmentsTest and full multiplayer CI run40119993589

CI and local replay fail manifest family/alias coverage changed before completing full multiplayer/regression acceptance. Manifest has43 families,129 stages,53 state aliases and162 unique resources; the assertion expects44 state aliases and153 resources.

Cause / uncertainty: Equipment manifest, runtime EquipmentModelCatalog and EquipmentAssetAssignmentsTest are byte-identical to pre-art commit f3c7b777d8cb202b2854e7f09714a69931790259. Existing state totals and the test coverage snapshot disagree; actual unique state resources are49 rather than40.

Fix / next action: Review the nine additional authored state resources/provenance against the current delivered manifest and catalog, then reconcile explicit coverage totals without removing per-slot/state/provenance/allowlist checks. Rerun actual full multiplayer/regression pipeline. Do not claim that local map-asset checks establish a full suite pass.

Evidence and verification: quality/evidence/DEV-069-full-multiplayer-baseline.txt; verification/hollow-creek-tailored-runtime.json; dated TAILORED_FABRIC handoff

Older-map obligation: Shared CI failure predates these map asset changes and affects broad release acceptance; retain it as an open prerequisite across all map acceptance audits.

### DEV-070 — Tolerance-classified clipping intersections extrapolated outside short edges

**Status:** fixed-terrain-floor-clipping · **Affected:** Intermediate floor retopology

A clipped floor produced reversed/duplicate top triangles and an edge with four adjacent faces.

Cause / uncertainty: An absolute half-plane epsilon classified slightly negative endpoints as inside, but interpolation still intersected the zero line, extrapolating outside the segment. Dense old boundary wrinkles amplified the issue.

Fix / next action: Use consistent signed half-plane intersections and the original4/64-corner source footprints. Weld only new floor vertices at micrometre scale and require closed shells, correct signed area, full coverage and actual terrain contact. Preserve failing topology diagnostic.

Evidence and verification: quality/evidence/DEV-070-clipped-floor-degenerates.json; verification/hollow-creek-fitted-floors.json

Older-map obligation: Audit equivalent fitted floors and nonplanar source/export surfaces independently. Native triangle counts and source equality do not establish gameplay, optical, host/LAN or runtime performance acceptance.

### DEV-071 — Nearest-face verification misreported thin-face export disagreement

**Status:** fixed-with-exact-oriented-raw-triangle-audit · **Affected:** Fitted tent source/export verification

Blender nearest-face queries reported up to1.3mm apparent source/GLB separation, including after explicit triangulation, despite matching bounds.

Cause / uncertainty: Float nearest-triangle queries are unreliable on some extremely thin source canvas faces; exact raw oriented triangle buffers demonstrate agreement. Implicit quad diagonals also make future matching ambiguous.

Fix / next action: Make original rendered diagonals explicit without moving nonfloor vertices or UVs, retain split normals, and compare complete oriented triangle multisets directly against actual GLB float/index buffers. All8 source/export sets match exactly. Guard source scene SHA before verification and run dependent checks only after generation completes; do not loosen geometric tolerances to silence the nearest-query result.

Evidence and verification: quality/evidence/DEV-071-implicit-quad-export-drift.txt; quality/evidence/DEV-071-raw-triangle-agreement.txt; verification/hollow-creek-fitted-floors.json

Older-map obligation: Audit equivalent fitted floors and nonplanar source/export surfaces independently. Native triangle counts and source equality do not establish gameplay, optical, host/LAN or runtime performance acceptance.

### DEV-072 — Primitive memorial silhouette and blank plaque

**Status:** fixed-native-sculpture-and-inscription · **Affected:** Camp Hollow Creek memorial

User observed disconnected primitive body/neck/wings and an empty plaque.

Cause / uncertainty: Initial component construction remained visible in the silhouette.

Fix / next action: Replace the casting with one closed connected12002-vertex surface, seated feet and actual supported plaque letters; verify native and exact source/GLB geometry. Stone/metal weathering and gameplay remain separate open work.

Evidence and verification: verification/hollow-creek-siteart.json; hollow-creek/SITE_ART_REVIEW.html

Older-map obligation: Audit analogous older-map models/material UV layers, actual support and authored placements individually; source/native checks do not establish runtime, ghost, host/LAN or performance acceptance.

### DEV-073 — Native ground renderer retained the old stretched UV layer

**Status:** fixed-native-and-portable-UV-selection · **Affected:** New Hollow Creek grass/dirt/shore pass

First native close-up stretched one tile across the entire site while the temporary export used new UVs.

Cause / uncertainty: Selecting the active editing UV layer does not select active_render in Blender.

Fix / next action: Explicitly select SiteArtWorldTile2m for native rendering and temporary exportUV0; independently inspect127646 UV loops and actual game reader pixels. Preserve original UV layer/source bitmap.

Evidence and verification: verification/hollow-creek-siteart.json; verification/hollow-creek-siteart-runtime.json

Older-map obligation: Audit analogous older-map models/material UV layers, actual support and authored placements individually; source/native checks do not establish runtime, ghost, host/LAN or performance acceptance.

### DEV-074 — Scattered boulder intersected a sanitary cubicle

**Status:** fixed-native-scatter-facility-clearance · **Affected:** Initial Hollow Creek rock-scatter candidate

Native close-up showed a rock intruding into the open toilet.

Cause / uncertainty: Path and tent-centre exclusions did not reserve actual facility/model bounds.

Fix / next action: Reserve actual transformed facility bounds with rock clearance, seat each rock against actual terrain vertices, and independently reject three-axis facility bounds overlap.105 final rocks pass4410 terrain-vertex probes and facility checks.

Evidence and verification: verification/hollow-creek-siteart.json

Older-map obligation: Audit analogous older-map models/material UV layers, actual support and authored placements individually; source/native checks do not establish runtime, ghost, host/LAN or performance acceptance.

### DEV-075 — Clearing instance groups erased newly assigned mesh weights

**Status:** fixed-memorial-instance-transfer · **Affected:** Organic memorial source-to-site installation

Independent native casting audit found zero vertices in the expected group although the source sculpture was connected.

Cause / uncertainty: Object.vertex_groups.clear was called after assigning the copied source mesh; Blender erased its deform membership.

Fix / next action: Clear old groups before assigning new source data. Copy group definitions in source order; require the native casting to have exactly one nonempty connected closed component and both seated feet.

Evidence and verification: verification/hollow-creek-siteart.json

Older-map obligation: Audit analogous older-map models/material UV layers, actual support and authored placements individually; source/native checks do not establish runtime, ghost, host/LAN or performance acceptance.

### DEV-076 — Environmental surface second-pass audit remains incomplete

**Status:** open-environmental-material-refinement · **Affected:** Hollow Creek and older map audits

User identified poor grass/dirt and generally crude environmental materials.

Cause / uncertainty: Old site atlas had inadequate texel density; boundary banks, foliage, timber, metal and stone still need individual close inspection.

Fix / next action: New dirty grass, soil and bare shore sources and portable derivatives are installed. Continue smoother face-transition blending, steep-slope UV density, stone/bronze patina, bank/bark/foliage/timber/weathering and close player-height runtime checks. Do not claim all environmental textures accepted.

Evidence and verification: hollow-creek/SITE_ART_REVIEW.html; textures/hollow-creek/GENERATION_PROMPTS.json

Older-map obligation: Audit analogous older-map models/material UV layers, actual support and authored placements individually; source/native checks do not establish runtime, ghost, host/LAN or performance acceptance.

### DEV-077 — All campground tents shared north-south headings

**Status:** fixed-native-refitted-varied-headings · **Affected:** Eight Hollow Creek tents and associated bedding

User noticed identical N-S tent orientation.

Cause / uncertainty: Placement pass reused unvaried headings.

Fix / next action: Apply eight distinct heading deltas, undo/reapply terrain deformation for every actual part and attached bedding, reclip closed12mm floors and export complete new fitted assemblies. Require dense support, positive canopy, crouch entry, route, ritual and actual reader checks.

Evidence and verification: hollow-creek/siteart-headings.json; verification/hollow-creek-siteart-floors.json; verification/hollow-creek-siteart-fit.json

Older-map obligation: Audit analogous older-map models/material UV layers, actual support and authored placements individually; source/native checks do not establish runtime, ghost, host/LAN or performance acceptance.

### DEV-078 — Invisible UV carrier material failed strict game asset loading

**Status:** fixed-portable-alpha-carrier-export · **Affected:** Five cosmetic memorial UV footprint cubes

Actual game reader rejected the carrier with Material has no original texture.

Cause / uncertainty: Native transparent shader lacked the embedded original PNG required by the unchanged bounded loader.

Fix / next action: Embed a2x2 transparent RGBA PNG on carrier edges/inner faces and a distinct webbed outer-face mask. Keep loader rules unchanged; actual reader must verify all60 triangles and both transparent/footprint alpha cases. This does not implement the runtime reveal or encounter.

Evidence and verification: verification/hollow-creek-siteart-runtime.json

Older-map obligation: Audit analogous older-map models/material UV layers, actual support and authored placements individually; source/native checks do not establish runtime, ghost, host/LAN or performance acceptance.

### DEV-079 — Boundary berm retained an obsolete material palette

**Status:** fixed-native-earth-bank-material-family · **Affected:** Camp Hollow Creek bank/crest refinement

The berm visibly differed from the revised grass/dirt ground.

Cause / uncertainty: The site-art ground pass did not include the boundary mesh.

Fix / next action: Use the same packed ground material family on the newly authored bank, with worn-soil transitions and an appropriate separate solid-bedrock material. Actual game source PNG pixels are checked; other environmental families remain DEV-076.

Evidence and verification: hollow-creek/EARTHEN_BANK_REVIEW.html; verification/hollow-creek-berm-runtime.json

Older-map obligation: Audit corresponding old-map boundary profiles, palette continuity, rock purpose and actual supported assemblies individually. Native source proofs are not gameplay, optical, host/LAN or performance acceptance.

### DEV-080 — Height-displaced UVs compressed texture at bank bends

**Status:** superseded-unpublished-prototype-not-shipped · **Affected:** Camp Hollow Creek bank/crest refinement

An independent slope-density test found a0.628 singular value at triangle777 in the material-only candidate.

Cause / uncertainty: Per-vertex terrain height gradients and skewed quads distorted a normal/height UV shortcut.

Fix / next action: Preserve the failure; do not relax the test to accept the prototype. Latest user direction replaces the old physical bank with rounded authored cross sections using surface-distance UVs. Do not claim the abandoned prototype passed its strict source-preservation/density tests.

Evidence and verification: quality/evidence/DEV-080-berm-slope-uv-stretch.txt

Older-map obligation: Audit corresponding old-map boundary profiles, palette continuity, rock purpose and actual supported assemblies individually. Native source proofs are not gameplay, optical, host/LAN or performance acceptance.

### DEV-081 — Copying source normals across quad splits changed their smooth fans

**Status:** superseded-unpublished-prototype-normal-audit-required · **Affected:** Camp Hollow Creek bank/crest refinement

An unpublished material-only prototype changed source corner-normal vectors by up to0.01453 while exposing rendered diagonals.

Cause / uncertainty: Blender custom-normal fan behavior differed after quad splitting. Copying values/face labels alone did not prove normal identity.

Fix / next action: Preserve diagnostic; do not reuse that prototype or loosen its failed normal gate. Current user-authorized new bank has new authored geometry/normals and exact source/export triangles. Audit normal preservation separately before applying a quad-retopology helper to older finished assets.

Evidence and verification: quality/evidence/DEV-081-berm-normal-fan-drift.txt

Older-map obligation: Audit corresponding old-map boundary profiles, palette continuity, rock purpose and actual supported assemblies individually. Native source proofs are not gameplay, optical, host/LAN or performance acceptance.

### DEV-082 — Uniform flat-topped berm read as a dirt-coloured wall

**Status:** fixed-native-authored-earth-profile · **Affected:** Camp Hollow Creek bank/crest refinement

User identified repetitive straight sides and flat crest despite the texture update.

Cause / uncertainty: Original narrow2.6m four-point profile repeated along the boundary.

Fix / next action: Replace it with curved, terrain-supported15-point closed sections; vary widths/relief, round shoulders, add small irregular drainage wear and constrain the inner toe around actual facilities/routes. Preserve ground, van and existing camp placements.

Evidence and verification: verification/hollow-creek-berm.json; hollow-creek/grounded-berm.json

Older-map obligation: Audit corresponding old-map boundary profiles, palette continuity, rock purpose and actual supported assemblies individually. Native source proofs are not gameplay, optical, host/LAN or performance acceptance.

### DEV-083 — Embedded stones read as gravel dollops and then cut blocks

**Status:** fixed-native-fractured-bedrock-outcrops · **Affected:** Camp Hollow Creek bank/crest refinement

User rejected pebbly rounded blobs; close review also rejected a rectangular wedge candidate.

Cause / uncertainty: Rounded repeated boulder forms used an aggregate shore texture; the first solid-stone candidate retained a rectangular front/lid.

Fix / next action: Remove original blobs; author12 irregular staggered fractured wedges with buried backs/bases, expose angular faces, add a separate solid-rock source texture and validate actual buried/exposed vertices. Inspect actual standing camera close-up.

Evidence and verification: hollow-creek/EARTHEN_BANK_REVIEW.html; textures/hollow-creek/bedrock-v1-prompt.json; verification/hollow-creek-berm.json

Older-map obligation: Audit corresponding old-map boundary profiles, palette continuity, rock purpose and actual supported assemblies individually. Native source proofs are not gameplay, optical, host/LAN or performance acceptance.

### DEV-084 — Reshaped bank left old crest dressing unsupported

**Status:** fixed-native-rigid-crest-refit · **Affected:** Camp Hollow Creek bank/crest refinement

Actual original log bearings sit53â€“63mm into the original soil, but unrefitted original bearings reach655mm above the new bank. A first metadata-based contact check had reported about0.20m.

Cause / uncertainty: Changing terrain without refitting supported log and rooted scrub assemblies invalidated their old contacts. Metadata and axis/cap rays alone were insufficient support evidence.

Fix / next action: Rigidly refit all28 complete logs and their branches using actual end-bearing vertices, and translate24 complete rooted shrub assemblies. Independent source-coordinate checks show no stretching/disconnected branches; actual both-end bearings now sit40mm into soil.

Evidence and verification: quality/evidence/DEV-084-actual-crest-bearing-before-after.json; verification/hollow-creek-berm.json; hollow-creek/grounded-berm.json

Older-map obligation: Audit corresponding old-map boundary profiles, palette continuity, rock purpose and actual supported assemblies individually. Native source proofs are not gameplay, optical, host/LAN or performance acceptance.

### DEV-085 — Cylinder-axis end ray misclassified log bearing support

**Status:** fixed-with-actual-bearing-geometry · **Affected:** Camp Hollow Creek bank/crest refinement

An axis ray near a slanted log end reported a gap while sampling its cap rather than its lowest support rim.

Cause / uncertainty: Finite tilted cylinders have different cap/axis/rim intersections; an ideal radius at an axis point is not the actual bearing surface.

Fix / next action: Solve rigid placement against actual end-ring vertex/terrain clearances and independently check both actual bearing regions. Do not silence a failed gap by widening tolerance or treating an axis formula as geometry evidence.

Evidence and verification: verification/hollow-creek-berm.json

Older-map obligation: Audit corresponding old-map boundary profiles, palette continuity, rock purpose and actual supported assemblies individually. Native source proofs are not gameplay, optical, host/LAN or performance acceptance.

### DEV-086 — Editable asset was saved only as a dependency library

**Status:** fixed-isolated-packed-model-scenes · **Affected:** Camp Hollow Creek bank/crest refinement

Dependency-only blend snapshots contained the model but no directly openable model scene.

Cause / uncertainty: Library snapshots are appropriate export intermediates, not finished authoring scenes. Saving back over a currently used library also failed.

Fix / next action: Copy snapshots into ignored staging, append into a clean scene and save the actual editable blend. Independently open both files, require exactly the intended model, packed images, no linked-library dependency and exact native/export triangle identity.

Evidence and verification: verification/hollow-creek-berm.json; finalize_earthen_bank_sources.py

Older-map obligation: Audit corresponding old-map boundary profiles, palette continuity, rock purpose and actual supported assemblies individually. Native source proofs are not gameplay, optical, host/LAN or performance acceptance.

### DEV-087 — Pending physics export referenced an obsolete native scene

**Status:** fixed-current-source-hash-guard · **Affected:** Camp Hollow Creek native-to-runtime geometry component

A pending HCS1 export predated fitted art and rounded earth banks.

Cause / uncertainty: Geometry export and source authority had diverged.

Fix / next action: Export HCS2 from the hash-verified current scene, guard local grading against its parent, and independently compare every raw oriented triangle. The mission remains locked.

Evidence and verification: verification/hollow-creek-native-source.json

Older-map obligation: Audit source/export identity, real capsule footprints and sweeps, opaque versus movement-only blockers, closed-solid occupancy and supported spawn anchors individually on older maps. Component tests do not establish world renderer, ghost, player, host/LAN or complete map acceptance.

### DEV-088 — Native sight query overran a mixed-direction grid endpoint

**Status:** fixed-bounded-grid-endpoint · **Affected:** Camp Hollow Creek native-to-runtime geometry component

Independent Blender rays exposed a nonterminating cell traversal when a ray ended on a grid plane.

Cause / uncertainty: Simultaneous X/Z crossings could move beyond t=1; negative zero also needed a direction of zero.

Fix / next action: Stop at the segment endpoint and explicitly handle zero direction; analytic regression covers both mixed-direction rays and negative zero. Verify all 346 actual native optical rays.

Evidence and verification: src/test/java/org/beckenburg/AuthoredSitePhysicsTest.java; verification/hollow-creek-native-runtime.json

Older-map obligation: Audit source/export identity, real capsule footprints and sweeps, opaque versus movement-only blockers, closed-solid occupancy and supported spawn anchors individually on older maps. Component tests do not establish world renderer, ghost, player, host/LAN or complete map acceptance.

### DEV-089 — L14 centerline passed while the standing foot radius hit steep terrain

**Status:** fixed-local-native-grading · **Affected:** Camp Hollow Creek native-to-runtime geometry component

Four capsule/sweep findings occurred at the east footbridge approach despite valid route-center support.

Cause / uncertainty: A narrow actual terrain strip reached about28.4% grade under the back rim of a standing footprint; earlier sparse point checks missed it.

Fix / next action: Adjust only three local terrain heights by at most12.54mm. Eighteen protected faces now meet19% grade without changing the20% runtime cap. All969 standing route points and939 nonzero sweeps pass; preserve and recheck actual model supports.

Evidence and verification: quality/evidence/DEV-089-L14-steep-foot-radius-strip.txt; verification/hollow-creek-route-grade.json; verification/hollow-creek-native-runtime.json

Older-map obligation: Audit source/export identity, real capsule footprints and sweeps, opaque versus movement-only blockers, closed-solid occupancy and supported spawn anchors individually on older maps. Component tests do not establish world renderer, ghost, player, host/LAN or complete map acceptance.

### DEV-090 — Draft native zone anchor targeted the memorial plinth

**Status:** fixed-native-anchor-contract · **Affected:** Camp Hollow Creek native-to-runtime geometry component

The draft memorial anchor used the center of the statue rather than its approach. Other draft elevations were rough layout hints.

Cause / uncertainty: Planning centers are not safe actor spawn positions.

Fix / next action: Use the authored memorial route endpoint X5,Z53 and derive all12 anchor elevations from actual intended walking surfaces. Require standing body and footprint clearance at every anchor. Future mission wiring must use this contract.

Evidence and verification: src/test/java/org/beckenburg/HollowCreekNativePhysicsTest.java; verification/hollow-creek-physics-export.json

Older-map obligation: Audit source/export identity, real capsule footprints and sweeps, opaque versus movement-only blockers, closed-solid occupancy and supported spawn anchors individually on older maps. Component tests do not establish world renderer, ghost, player, host/LAN or complete map acceptance.

### DEV-091 — Windows default text encoding broke the deployed handoff

**Status:** fixed-explicit-utf8-writers · **Affected:** Hollow Creek native physics publication

Pages run40201935399 rejected the new dated handoff as invalid UTF-8; the generated review also used the wrong declared encoding.

Cause / uncertainty: New checkpoint writer omitted encoding on Path.write_text and inherited Windows CP1252.

Fix / next action: Emit explicit UTF-8 from every new checkpoint writer, regenerate the handoff and review and strictly decode all staged publication text. Rebuild and read back the real deployed review and PNG before claiming gallery delivery.

Evidence and verification: quality/evidence/DEV-091-pages-encoding.txt

Older-map obligation: Check text-writing producers and strictly decode future authored publication text; do not infer that successful local file opening proves Jekyll-compatible encoding.

### DEV-092 — New grass export family initially used the wrong texture V convention

**Status:** fixed-runtime-atlas-orientation · **Affected:** Hollow Creek green grass material variant

Caught during local export/actual loader validation; failing candidate was not published.

Cause / uncertainty: New asset IDs were curated before being added to the original-top-left texture family in GlbProps.

Fix / next action: Add GREEN_GROUND to the original atlas orientation path. All1152 independent PNG/UV samples pass across four exports.

Evidence and verification: verification/hollow-creek-green.json; verification/hollow-creek-green-pixels.tsv; green_hollow_creek_grass.py; HollowCreekGreenGroundTest

Older-map obligation: Apply actual UV/pixel checks to each new export family and avoid live Blender collection references during deletion; older material families are not automatically accepted.

### DEV-093 — Removing Blender UV layers via live RNA wrappers invalidated later names

**Status:** fixed-exporter-uv-name-snapshot · **Affected:** Hollow Creek green grass material variant

Caught during local export/actual loader validation; failing candidate was not published.

Cause / uncertainty: Removing a UV layer reindexes Blender collection wrappers; a saved list of layer references became stale during mutation.

Fix / next action: Snapshot UV layer names before removal and resolve each by name. All four exports and directly editable scenes now validate.

Evidence and verification: verification/hollow-creek-green.json; verification/hollow-creek-green-pixels.tsv; green_hollow_creek_grass.py; HollowCreekGreenGroundTest

Older-map obligation: Apply actual UV/pixel checks to each new export family and avoid live Blender collection references during deletion; older material families are not automatically accepted.

### DEV-094 — Grass green failed to match the distant woodland palette

**Status:** revised-native-palette-user-review-pending · **Affected:** Hollow Creek ground, bank and shore-return material

User rejected the first greener v3 texture because its yellow-green hue still differed from the distant wood.

Cause / uncertainty: The first prompt targeted general camp greens instead of the actual distant woodland ground in the same view.

Fix / next action: Use the actual overview as a colour reference and remove the yellow cast in a new grey-green v4 source. Recompose all three dirt blends, export the four affected models and show same-camera before/after overviews. Keep lighting and geometry unchanged. Human art acceptance remains pending.

Evidence and verification: hollow-creek/GREEN_GRASS_REVIEW.html; textures/hollow-creek/grass-woodland-v4-prompt.json; verification/hollow-creek-woodland-palette.json

Older-map obligation: Compare near/far material colours together under the same scene lighting; a generic request for greener grass does not establish palette continuity.

### DEV-095 — Native runtime discarded authored site geometry

**Status:** fixed-scoped · **Affected:** Hollow Creek

Earlier game level construction did not consume the native mesh/pose/physics contract.

Cause / uncertainty: Recorded during native-runtime/water review; see reproducible source and capture evidence.

Fix / next action: Native HCS2 bridge, 384 bounded original GLBs and 598 exact authored poses; no replacement room slabs.

Evidence and verification: HollowCreekRuntimeTest: 969 route points / 939 sweeps; independent exact geometry/UV/pose audit.

Older-map obligation: Audit every older map runtime against its actual source, independently.

### DEV-096 — Van vertical placement lost across runtime systems

**Status:** fixed-scoped · **Affected:** Elevated canonical van

Dock Y was ignored by support, rendering, fixture lights and interaction targets.

Cause / uncertainty: Recorded during native-runtime/water review; see reproducible source and capture evidence.

Fix / next action: Propagate dock elevation through reusable van contracts, support/ramp geometry, mounting/focus and optics.

Evidence and verification: ElevatedVanTest: three heights and rotated placements, cargo/ramp and real side/roof occlusion.

Older-map obligation: Audit any map with nonzero van elevation; legacy placements remain compatible.

### DEV-097 — Native exterior zones treated as indoor by numeric room ID

**Status:** fixed-scoped · **Affected:** Hollow Creek outdoor zones 0–11

Outdoor investigation ownership reused indoor numeric IDs and received indoor ambient/shelter assumptions.

Cause / uncertainty: Recorded during native-runtime/water review; see reproducible source and capture evidence.

Fix / next action: Use actual Room.outside/native roof queries; source shelter geometry remains authoritative.

Evidence and verification: Runtime pavilion/van shelter and route tests; actual native-game captures.

Older-map obligation: Audit room ownership separately from indoor/outdoor classification.

### DEV-098 — GPU texture upload flipped V twice and destroyed packed atlas resolution

**Status:** fixed-scoped · **Affected:** GPU material upload; exposed by Hollow Creek boat

Boat looked intact in native reviews but acquired black bands and unrelated plank colours in actual GPU capture.

Cause / uncertainty: Recorded during native-runtime/water review; see reproducible source and capture evidence.

Fix / next action: Retain authored texture width/height; upload CPU sampler coordinates without a second V inversion.

Evidence and verification: flat-baseline vs atlas-corrected dock PNGs; final runtime boat video; native atlas resolution checks.

Older-map obligation: All GPU-rendered older assets require individual visual review after this shared sampler correction.

### DEV-099 — Boat seats and oar blades penetrated the narrowing hull

**Status:** fixed-scoped · **Affected:** Hollow Creek west moored rowing boat

Seat ends protruded through curved sides; stowed blades crossed the bow.

Cause / uncertainty: Recorded during native-runtime/water review; see reproducible source and capture evidence.

Fix / next action: Boat-only editable variant: fit three thwarts to actual hull with 12mm clearance; move brackets/fastenings; stow oars on seats with full-vertex hull checks. Hull/pivot/pose unchanged.

Evidence and verification: rowboat-refinement.json: 228 moved detail vertices; minimum oar-blade clearance 114.5mm; unchanged hull vertices; final dock runtime view.

Older-map obligation: Check complete accessory extents against curved support geometry, not centerline width.

### DEV-100 — Boat painted atlas and lighting read as flat artificial surfaces

**Status:** revised-runtime-art-human-review-pending · **Affected:** Hollow Creek rowing boat

Restored upload still exposed crude flat paint, repetitive grain and missing surface-normal response.

Cause / uncertainty: Recorded during native-runtime/water review; see reproducible source and capture evidence.

Fix / next action: Three original weathered directional albedos; retain source normals and add audited boat directional fixture/torch shading in GPU path.

Evidence and verification: 20,484 loaded boat normal samples are unit length; real fixture direction check; final dock capture.

Older-map obligation: Other assets and software-path directional shading remain unaudited; do not infer all-map shading acceptance.

### DEV-101 — Water rendered through boat cockpit

**Status:** fixed-scoped · **Affected:** Hollow Creek water and rowboat

Flat surface occupied the boat interior; a single waterline cut would leak after adding waves.

Cause / uncertainty: Recorded during native-runtime/water review; see reproducible source and capture evidence.

Fix / next action: 23 actual inner-hull sections at 10mm intervals, adjacent-section union within thick shell, full 95mm wave envelope. Ignore mooring-ring cuts.

Evidence and verification: WaterEffectsTest full-wave center/exterior exclusion and final 72-frame dry-hull sample counts.

Older-map obligation: Every future boat requires its own actual hull profile; rectangular cutouts are not acceptable.

### DEV-102 — Underwater surface had no optical basin depth

**Status:** fixed-scoped · **Affected:** Hollow Creek water

Nominal constant depth could not reveal shallows or refract a visible bed.

Cause / uncertainty: Recorded during native-runtime/water review; see reproducible source and capture evidence.

Fix / next action: Author 12,000 non-walkable optical bed triangles from actual bank controls with shared exact triangle depth sampler and existing shore grit. Exclude dock blockers.

Evidence and verification: GridBed diagonal/footprint tests; water-bed.json and beach/river runtime captures.

Older-map obligation: Per-map authored bathymetry required; current basin is an initial authored profile, not a survey.

### DEV-103 — Distant water exposed a harsh clipping horizon

**Status:** revised-runtime-review-pending · **Affected:** Hollow Creek water and scenery

User observed a hard distance line and excessive distant texture sharpness in live views.

Cause / uncertainty: Recorded during native-runtime/water review; see reproducible source and capture evidence.

Fix / next action: Depth-aware distant blur with foreground rejection, plus exponential convergence to actual camera sky pixels before far clipping.

Evidence and verification: WaterReviewRegressionTest near sharpness, foreground isolation and horizon convergence; final beach clip.

Older-map obligation: Audit each map horizon and sky frame; no blanket completion inferred.

### DEV-104 — Shoreline lacked slow advancing and receding waves

**Status:** revised-runtime-review-pending · **Affected:** Hollow Creek beach and creek margins

Scrolling flat texture did not provide visible shore wave action or animated edge foam.

Cause / uncertainty: Recorded during native-runtime/water review; see reproducible source and capture evidence.

Fix / next action: Depth-driven 5.5-second shoreline wave train, coherent 65mm background waves, 30mm shore run-up, moving broken crest foam and two independent pixelated layers.

Evidence and verification: Independent nearest-wave rays and hull envelope tests; fixed-camera six-second runtime clips.

Older-map obligation: Tune wave scale, shore textures and currents individually; creek-specific flow and interaction ripples remain open.

### DEV-105 — Generic signature breaker rule would ignore ghost-specific restrictions

**Status:** open-host-rule-audit · **Affected:** Host/solo remote-site electrical events

Integration review found no verified existing ghost-specific trip path; a draft generic SIGNATURE trip would force every ghost to trip a breaker.

Cause / uncertainty: Recorded during native-runtime/water review; see reproducible source and capture evidence.

Fix / next action: Removed draft generic trip. Preserve separate source/breaker controls; audit and wire only actually eligible ghost electrical rules before host/LAN acceptance.

Evidence and verification: HollowCreekPowerTest proves supported controls and source isolation only, explicitly not ghost-trip or host/LAN acceptance.

Older-map obligation: Audit eligible ghost rules and authority on all generator maps before unlock.

### DEV-106 — Submerged creek bed remained an eligible native standing surface

**Status:** fixed-scoped · **Affected:** Hollow Creek physical terrain below water

A review-camera probe found traversable native creek floor at (49,23), feet about -2.64m, below the -1.95m water surface.

Cause / uncertainty: The edge blockers constrain crossings, but terrain support alone still allowed a direct underwater spawn query.

Fix / next action: Reject below-water native support when the actual water body is wet; retain canonical van and dry-hull rules. Move river review cameras to supported dry bank positions.

Evidence and verification: HollowCreekRuntimeTest directly rejects the submerged query while all969 existing route supports and939 sweeps still pass. River cameras now expose47–55k water pixels per frame instead of3–6k.

Older-map obligation: Audit both edge-crossing barriers and direct submerged spawn/support eligibility for every water map.

### DEV-107 — Review launcher crashed during abrupt native GPU shutdown

**Status:** fixed-scoped · **Affected:** Hollow Creek GPU review command

All frames and reports were written, then the process exited with Windows native status -1073740791 (0xC0000409).

Cause / uncertainty: The review command called System.exit while the GLFW/OpenGL worker was still active.

Fix / next action: Request application exit and join the owning worker, allowing its resource disposal/window teardown before main returns.

Evidence and verification: Same real dock render writes fresh world/depth/water evidence and now exits with native code0. Previous frame files remain actual capture evidence, not a clean launcher test.

Older-map obligation: Audit other GPU smoke/shutdown entry points separately; only the new review lifecycle was corrected.

### DEV-108 — Visible regular modulation in the water review

**Status:** open-art-quality-review · **Affected:** Hollow Creek lit water

Increasing opacity exposed repeating surface/bed lighting bands in the beach beam.

Cause / uncertainty: Sine-product ripple texture and coarse lit bed interpolation are possible contributors; not yet isolated.

Fix / next action: Replace the strong product pattern with two independent softly warped directional ripple layers; retain the remaining banding for follow-up art/lighting review.

Evidence and verification: DEV-108-before-regular-water-ripple-grid.png and latest murkier beach clip; no full banding acceptance claimed.

Older-map obligation: Audit water repetition and bed-light tessellation under the actual beam, not only flat texture inspection.

### DEV-109 — Local review video showed a poster without reliable playback

**Status:** mitigated-published-HTTPS-verified-local-preview-unverified · **Affected:** Water review HTML and archived animations

User could see stills but could not play the local review video.

Cause / uncertainty: Local preview delivery/decoder cause is not proven. Published HTTPS MP4 was directly observed playing at4.1856 of6 seconds, readyState4.

Fix / next action: Add VP9 WebM before MP4 fallback, explicit Play/Pause controls, visible ready/time/error status and direct media links; use the tested published HTTPS review as the primary artifact.

Evidence and verification: Browser DOM and screenshot confirmed actual playing state; final new controls and WebM require deployed follow-up verification.

Older-map obligation: Verify browser playback rather than inferring it from ffprobe frame counts.

### DEV-110 — Reused shore-grit bed read poorly through overly clear water

**Status:** revised-runtime-art-human-review-pending · **Affected:** Hollow Creek underwater bed and water style

User requested a different creek-bed texture and less transparent water.

Cause / uncertainty: Shore grit was reused for the optical basin, while low absorption/minimum opacity exposed excessive ground detail.

Fix / next action: New original image-generated silt/rounded-stone albedo; minimum opacity48%, absorption1.8/m, maximum97%; approximately89% opacity at1m. Keep shallow edges visibly translucent.

Evidence and verification: creek-bed-silt-stone-v1-prompt.json; new water-murkier game views; transmission regression retains shallow visibility and strongly suppresses the deeper bed.

Older-map obligation: Author and review dedicated optical beds and turbidity for each map rather than copying ground materials.

### DEV-111 — Equipment assignment CI retained pre-existing stale variant counts

**Status:** fixed-scoped · **Affected:** EquipmentAssetAssignmentsTest / full multiplayer build preflight

Published water checkpoint full multiplayer workflow stopped before the round at manifest family/alias coverage changed.

Cause / uncertainty: The unchanged pre-water manifest/catalog already contain53 state aliases,49 unique state meshes and162 total resources; the test still expected44/40/153. All explicit mappings passed before the stale count check.

Fix / next action: Update the delivered coverage contract to53/49/162 while retaining every independently checked mapping, provenance and allowlist-equality assertion. No equipment models or catalog mappings changed.

Evidence and verification: Verified no diff in manifest/catalog/test against15b964c before the correction. Local test now passes43 families/129 stage aliases/53 states/162 resources. Re-run full workflow after publication.

Older-map obligation: Keep source catalog, independent manifest and explicit coverage assertions synchronized when adding equipment states; do not weaken mapping checks.

### DEV-112 — 3D water motion too subtle; wet objects lacked contact foam and spray

**Status:** revised-runtime-human-review-pending · **Affected:** Hollow Creek water / recorded development checks

User approved texture motion but requested stronger geometric waves and foamy collision edges/spray at banks and water objects.

Cause / uncertainty: Earlier65mm waves and shallow-depth foam did not expose actual solid-water intersections.

Fix / next action: Keep scrolling rates; use160mm broad swell/shorter chop plus60mm shore run-up, visible opaque-geometry contact foam and bounded light/depth-tested ballistic spray. Expand actual hull sections to38, including near-floor cap.

Evidence and verification: WaterContactTest: >200mm actual vertical travel, no floating/distant halos, spray visibility/occlusion and complete dry cockpit cycle. Latest actual water-contacts clips record foam/spray sample counts.

Older-map obligation: Per-map wet solid contacts and hull/freeboard must be reviewed in actual animated game views; visible-depth pass does not imply offscreen physics coverage.

### DEV-113 — Prototype contact foam mistook shallow bed for impact edges

**Status:** fixed-scoped · **Affected:** Hollow Creek water / recorded development checks

Initial contact pass frosted most of the shallow creek, rather than just its collision edges.

Cause / uncertainty: World proximity alone accepted the flat underwater bed as a solid impact surface.

Fix / next action: Use opaque-depth surface normals to reject gently sloping beds except at the actual waterline; retain separate shoreline run-up foam. Add a direct flat-bed regression.

Evidence and verification: DEV-113-before-shallow-bed-contact-foam.png; WaterContactTest; final east-bank contact samples drop from2452515 to626355 over72 frames with bank/object edges retained.

Older-map obligation: Test open shallow beds and lifted props as negative foam cases, not only successful wet-contact examples.

### DEV-114 — Dressing CI expected every hiding candidate to appear at maximum density

**Status:** fixed-scoped · **Affected:** Hollow Creek water / recorded development checks

Full multiplayer preflight passed equipment mapping, then failed HOUSE geometry delta10824 vs actual4608.

Cause / uncertainty: Pre-existing dressing test counted all five cupboard candidates even though the shared combined hiding cap selects only two plus one outdoor refuge.

Fix / next action: Retain scattered candidate checks, compute expected geometry from the actual shared-budget selected sockets, and test physical concealment/collision only for enabled cupboards. No gameplay hiding budget changed.

Evidence and verification: LivedLevelDressingTest passes HOUSE/FARMHOUSE/SCHOOL: two actual physically enterable cupboards and supported small objects each. Full CI rerun remains required.

Older-map obligation: Test candidate supply separately from the equal shared active hiding budget; never restore unlimited hiding to satisfy a stale assertion.

### DEV-115 — Water lacked visible editor handles and saved map ownership

**Status:** fixed-scoped · **Affected:** Map environment authoring

Water lacked visible editor handles and saved map ownership

Cause / uncertainty: Runtime and editor integration gap

Fix / next action: Five actual footprints, placement icon, live settings, multi-edit, handles, edge/rim snapping, atomic save/export and immutable map revisions added. Portable tests and actual OpenGL UI copy/pill/resize/rotate/undo/save passed.

Evidence and verification: Source audit25October2026; water editor live-handles.png; water/editor/weather regression suites

Older-map obligation: Review basin, boat exclusions and player blockers separately on every map.

### DEV-116 — GPU weather ground coatings and inspector incomplete

**Status:** partially-fixed-audit-open · **Affected:** Map environment authoring

GPU weather ground coatings and inspector incomplete

Cause / uncertainty: Runtime and editor integration gap

Fix / next action: Actual weather preview inspector and castle precipitation pass added. Core rain/snow/wind/sky/lightning tests pass. Source audit confirms GPU faces bypass software ground wet/snow coating; roof-exposure audit remains open.

Evidence and verification: Source audit25October2026; water editor live-handles.png; water/editor/weather regression suites

Older-map obligation: Do not infer GPU weather parity from software checks.

### DEV-117 — No visible ambient/environmental sound editor volumes

**Status:** implementation-in-progress · **Affected:** Map environment authoring

No visible ambient/environmental sound editor volumes

Cause / uncertainty: Runtime and editor integration gap

Fix / next action: Existing room and weather soundscapes have no selectable visible volume inspector. Add placement, playback, source gain/range/falloff and save/runtime integration per user request.

Evidence and verification: Source audit25October2026; water editor live-handles.png; water/editor/weather regression suites

Older-map obligation: Audit existing beds to avoid duplicate loops; authored ambience must remain separate from ghost/evidence events.

### DEV-118 — Opening pan rejected fully outdoor native maps

**Status:** fixed-scoped · **Affected:** Existing-map lighting/camera compatibility

LevelOpeningFlyover required indoor bounds; Hollow Creek has outdoor ownership zones only. Expanded all-ten-map regression reproduced IllegalStateException before repair.

Cause / uncertainty: LevelOpeningFlyover required indoor bounds; Hollow Creek has outdoor ownership zones only. Expanded all-ten-map regression reproduced IllegalStateException before repair.

Fix / next action: Share finite indoor/outdoor framing with the software fallback; exclude van/approach zones and avoid presenting outdoor zones as rectangular buildings.

Evidence and verification: LevelOpeningFlyoverTest now covers all10maps, finite fallback rendering and exact player handoff.

Older-map obligation: Audit each future map camera path, framing, support and final view; no full path collision or human acceptance inferred.

### DEV-119 — Long prototype halls had no effective entrance lighting

**Status:** fixed-scoped · **Affected:** Existing-map lighting/camera compatibility

Actual GPU lit and blackout images were identical in Mansion, Prison, Asylum, Academy and College; Church entry was nearly black. A single central fixture was beyond range of the actual entry.

Cause / uncertainty: Actual GPU lit and blackout images were identical in Mansion, Prison, Asylum, Academy and College; Church entry was nearly black. A single central fixture was beyond range of the actual entry.

Fix / next action: Place repeated physical fittings at up to7m spacing in unfinished indoor prototypes, preserving refined House/Farmhouse/School fixture layouts.

Evidence and verification: AllMapLightingCoverageTest passes nine indoor entries and blackout; actual GPU before/after captured for six prototypes.

Older-map obligation: Review every room, ceiling attachment, stairs/openings, switch circuit and all joins. Entry samples are not whole-map leak acceptance.

### DEV-120 — Native outdoor aerial review has excessive water cost and obscuring fog

**Status:** open-profile-and-art-review · **Affected:** Existing-map lighting/camera compatibility

Hollow Creek aerial/water captures take minutes; aerial site detail is heavily obscured by fog.

Cause / uncertainty: Hollow Creek aerial/water captures take minutes; aerial site detail is heavily obscured by fog.

Fix / next action: Record actual scripted captures and keep real-time performance and aerial composition open. Do not change global fog or hide the native map to force a pass.

Evidence and verification: 480x300 first capture and320x200 isolated rerun; no hardware frame-time benchmark or acceptance claimed.

Older-map obligation: Profile water, lighting, geometry and camera effects separately for every future outdoor/water map before mission unlock.

### DEV-121 — Capture JAR was rebuilt while an earlier worker still used it

**Status:** fixed-tooling-workflow · **Affected:** Existing-map lighting/camera compatibility

An overlapping Maven build replaced a running capture JAR, causing a lazy LWJGL class load error at shutdown. Captured frames alone did not prove a clean run.

Cause / uncertainty: An overlapping Maven build replaced a running capture JAR, causing a lazy LWJGL class load error at shutdown. Captured frames alone did not prove a clean run.

Fix / next action: Run subsequent captures from immutable copied JARs. Castle rerun completed exit0; repeat native capture from a separate immutable copy.

Evidence and verification: build/map-audit-captures.log retains failure; map-audit-castle.log clean; native rerun recorded separately.

Older-map obligation: Never overwrite a running validation executable; record process completion separately from valid captured frame hashes.

### DEV-122 — Van upper racks dark under narrow ceiling beams

**Status:** fixed-scoped · **Affected:** Shared van / Castle

Van upper racks dark under narrow ceiling beams

Cause / uncertainty: Source distribution / placement / material quality

Fix / next action: Three physically mounted broad diffusers on vehicle supply; all10map placements tested.

Evidence and verification: VanLightingAndCastleDoorTest,8actualGPUframes: van-castle-repair.json

Older-map obligation: Inspect every van placement and installed Castle doorway; full fitness and human acceptance remain open.

### DEV-123 — Castle leaves vanish when open and ignore floor elevation

**Status:** fixed-scoped-human-review-pending · **Affected:** Shared van / Castle

Castle leaves vanish when open and ignore floor elevation

Cause / uncertainty: Source distribution / placement / material quality

Fix / next action: Paired fitted leaves remain on hinges at actual storey height; collision and shadows follow them. Portcullis also uses slot bottom.

Evidence and verification: VanLightingAndCastleDoorTest,8actualGPUframes: van-castle-repair.json

Older-map obligation: Inspect every van placement and installed Castle doorway; full fitness and human acceptance remain open.

### DEV-124 — Van metal textures show banding

**Status:** partial-runtime-refinement-human-review-pending · **Affected:** Shared van / Castle

Van metal textures show banding

Cause / uncertainty: Generated IRON tile darkened every eighth X pixel by17levels; repeated UVs stamp grooves across broad painted panels.

Fix / next action: Replace forced directional grooves with periodic 2D weathering, restrained grain and sparse pits. Preserve all15other tiles, atlas layout, UVs, source geometry, lighting and exposure. Roof/interpolation and native model metal families remain separate audits.

Evidence and verification: MetalMaterialRefinementTest: groove phase contrast16.652→0.057. Twelve matched actual GPU van images; renderer/fixture/van bytecode identical before/after. metal-refinement-2026-10-27.json.

Older-map obligation: Procedural IRON users receive this tile correction; original GLB textures, roof shading bands, all-map in-room visual acceptance and hardware frame timing remain unaccepted.

### DEV-125 — Recorder CI assumed every WHISPER action uses the WHISPER audio cue

**Status:** fixed-scoped-full-CI-pending · **Affected:** LanSoundRecorderIntegrationTest / recovery gate

a635d0c8 full multiplayer run40296444087 failed246/247 with missing SOUND|WHISPER, although same-SHA installer passed.

Cause / uncertainty: Random authored haunting selection can legitimately choose a different cue for the WHISPER action.

Fix / next action: Use the actual host-authored HAUNT entry cue for received sound and recorded clip assertions. doorway_sigh legitimately uses HORROR_STING_A. Preserve real recorder transactions, playback, reconnect and privacy checks; no runtime evidence/audio changes.

Evidence and verification: Focused four-socket rack/paid tiers/capture/private playback/reconnect test passes.

Older-map obligation: Keep test expectations bound to real authored actions and cue identities on all maps. Full exact-SHA acceptance remains required.

### DEV-126 — Saved mission-board browser would reorder persistent campaign chapters

**Status:** fixed-local-full-CI-pending · **Affected:** Mission board / campaign / LAN lobby integration

Saved mission-board browser would reorder persistent campaign chapters

Cause / uncertainty: Old local catalog used alphabetic image-browser order for ordinal-based saved story progression.

Fix / next action: Keep the original 22 chapter IDs in explicit campaign order; use alphabetic order only for browsing.

Evidence and verification: MissionBoardIntegrationTest and CampaignStoryProgressionTest pass locally; exact-SHA full CI pending.

Older-map obligation: Preserve every saved chapter ordinal and prototype availability when adding or renaming locations.

### DEV-127 — Saved mission-board labels omitted the newer Hollow Creek authoring enum

**Status:** fixed-local-full-CI-pending · **Affected:** Mission board / campaign / LAN lobby integration

Saved mission-board labels omitted the newer Hollow Creek authoring enum

Cause / uncertainty: Newer main added HOLLOW_CREEK, while local MissionSites.title only accepted original lore IDs.

Fix / next action: Resolve the authoring enum to its existing WLD-02 lore record without making prototype geometry playable.

Evidence and verification: MissionBoardIntegrationTest iterates every authoring enum and verifies Hollow Creek remains locked; SiteSelectionTest passes locally.

Older-map obligation: Preserve every saved chapter ordinal and prototype availability when adding or renaming locations.

### DEV-128 — GPU diagnostics crashed after successful captures during abrupt JVM shutdown

**Status:** fixed-local-exact-CI-pending · **Affected:** Unified recovery / native GPU diagnostics / hunter feature integration

Native RTX run from main f6793bda completed all assertions but exited 0xC0000409.

Cause / uncertainty: Smoke entry points called System.exit while the owning GLFW/GL worker remained alive, despite the existing review teardown contract.

Fix / next action: Stop and join the owning GPU worker before exiting; teardown failures must fail acceptance.

Evidence and verification: Source 417980e6: three RTX 5070 diagnostic runs exited 0, 56.109/56.125/55.359 seconds; raw baseline failure and repeat-run logs retained under performance/hardware/2026-10-26.

Older-map obligation: Verify native shutdown and same-seat/privacy behavior on every supported map and installed hardware; no project-wide acceptance inferred.

### DEV-129 — Hunter unit fixture mixed active Hallow into independent Focus-expiry comparison

**Status:** fixed-local-exact-CI-pending · **Affected:** Unified recovery / native GPU diagnostics / hunter feature integration

HunterDisciplineTest failed at its Focus-expiry assertion during integration.

Cause / uncertainty: The prior Cleric scenario shared Hallow with the Observer; both Hallow and Focus deliberately apply 55% fear.

Fix / next action: Retain every original assertion, isolate the Focus fixture, and additionally verify shared Hallow protection and expiry.

Evidence and verification: HunterDisciplineTest, HunterClassIntegrationTest and SoloClassAbilityTest pass locally.

Older-map obligation: Verify native shutdown and same-seat/privacy behavior on every supported map and installed hardware; no project-wide acceptance inferred.

### DEV-130 — Hunter feature branch rejected ghost reconnect by assigning a hunter discipline

**Status:** fixed-local-exact-CI-pending · **Affected:** Unified recovery / native GPU diagnostics / hunter feature integration

Unchanged LanSessionResumeIntegrationTest failed in ghostRejoin with invalid join response at source 417980e6.

Cause / uncertainty: Resume handshake passed the ghost seat to LanCase.selectDiscipline, which correctly rejects non-hunter seats.

Fix / next action: Guard class restoration to hunter seats, preserve ghost resume and host ownership, and reset fresh lobby occupant class authority to its own visual choice.

Evidence and verification: LanSessionResumeIntegrationTest and LanHostOwnerRecoveryTest pass after fix; strengthened real-TCP tests verify resumed Cleric ability, fresh Observer authority and ghost privacy.

Older-map obligation: Verify native shutdown and same-seat/privacy behavior on every supported map and installed hardware; no project-wide acceptance inferred.

### DEV-136 — Production light sampler bypassed receiving-side architectural join guard

**Status:** fixed-scoped-runtime-diagnostic-verified · **Affected:** FixtureLighting on generated indoor maps; Blackpine grazing-view reproducer

Actual scalar sampler added4.613energy through a solid wall endpoint. Grazing GPU view showed2260bright pixels from an exterior diagnostic source despite closed geometry.

Cause / uncertainty: ArchitecturalLightOcclusion.visible existed and its standalone tests passed, but production scalar and normal sampling never called it. Prop self-shadow endpoint bias could move the receiving endpoint across a wall. Shadow key also omitted receiving room.

Fix / next action: Use full receiving-side architectural check before prop bias for non-native investigation geometry; scope scalar shadow cache by receiver. Preserve Castle visibility and native triangle paths.

Evidence and verification: FixtureJoinIntegrationTest fails on7f1515d0 at4.8vs0.187ambient and passes after repair, including real door open/close invalidation and same-side light control. AllMapArchitecturalJoinTest6789closed-boundary rays across9indoor maps.18matched actualGPUdiagnostic images: corrected exterior-source frames exactly equal source-off frames; same-side interior source remains lit.

Older-map obligation: All generated-map joins still need moving-camera/human review. Native physical meshes, roof joins and every portal state require independent evidence. No full map, LAN or30FPS acceptance inferred.

### DEV-137 — Castle construction disagrees with its collision raster

**Status:** fixed-scoped-local-native-and-GPU-verified · **Affected:** Castle floor, doorway shoulders and upper circulation

Current main uses separately maintained raster collision, a large rectangular foundation and single-face overlapping decks. Overflight also omits entire roofs at a 27m center distance.

Cause / uncertainty: Independently authored legacy geometry, rendering and display coordinates.

Fix / next action: Replace with measured room volumes, unioned 220mm slabs, closed roof geometry and actual thick-wall collision. Preserve functional station positions.

Evidence and verification: CastleRebuildTest and focused Castle regressions;26 actual RTX5070 captures in castle-survey/rebuild; native reopen verification castle-native-rebuild.json:220671 triangles,706 packed images. Full manifest and installed acceptance remain separate.

Older-map obligation: Castle-only rebuild; other maps retain individual geometry, lighting, support and route audit obligations.

### DEV-138 — Castle warehouse omits tiers and suspends items above shallow shelves

**Status:** fixed-scoped-local-native-and-GPU-verified · **Affected:** Castle quartermaster

Eight .22m-deep bookshelves face one direction. Only 43 tier-I displays are authored at .40/1.00/1.60m despite source shelf tops .09/.39/.69/.99/1.29m at scale1.05. Top displays float; higher tiers are absent.

Cause / uncertainty: Independently authored legacy geometry, rendering and display coordinates.

Fix / next action: Replace with original open retail shelves, aisle-facing placements,129 catalog/tier displays, exact support planes and shelf labels; retain menu purchase authority.

Evidence and verification: CastleRebuildTest and focused Castle regressions;26 actual RTX5070 captures in castle-survey/rebuild; native reopen verification castle-native-rebuild.json:220671 triangles,706 packed images. Full manifest and installed acceptance remain separate.

Older-map obligation: Castle-only rebuild; other maps retain individual geometry, lighting, support and route audit obligations.

### DEV-139 — Castle courtyard and circulation lack usable local lighting

**Status:** fixed-scoped-local-native-and-GPU-verified · **Affected:** Castle rooms, courtyard, stairs and store

Twenty-one actual RTX frames show very dark exterior circulation and uneven interior coverage. All three existing switches toggle every lamp; new slab soffits require refitted ceiling mounts.

Cause / uncertainty: Independently authored legacy geometry, rendering and display coordinates.

Fix / next action: Provide supported coverage in every room, courtyard sconces, room circuits and an upper-library switch. Verify lit/off/torch controls and per-room light separation.

Evidence and verification: CastleRebuildTest and focused Castle regressions;26 actual RTX5070 captures in castle-survey/rebuild; native reopen verification castle-native-rebuild.json:220671 triangles,706 packed images. Full manifest and installed acceptance remain separate.

Older-map obligation: Castle-only rebuild; other maps retain individual geometry, lighting, support and route audit obligations.

### DEV-140 — Castle service fixtures are visible but lack collision and supported props

**Status:** fixed-scoped-local-native-and-GPU-verified · **Affected:** Castle stations and wall bookshelves

Service desks/cabinets are rendered independently of collision. Some source library shelves face walls; several display items use guessed heights.

Cause / uncertainty: Independently authored legacy geometry, rendering and display coordinates.

Fix / next action: Share measured service placements between collision and rendering; correct shelf orientation; remove obsolete store dressing; refit supported accents.

Evidence and verification: CastleRebuildTest and focused Castle regressions;26 actual RTX5070 captures in castle-survey/rebuild; native reopen verification castle-native-rebuild.json:220671 triangles,706 packed images. Full manifest and installed acceptance remain separate.

Older-map obligation: Castle-only rebuild; other maps retain individual geometry, lighting, support and route audit obligations.

### DEV-141 — GPU packaging can overwrite fresh code with stale resource bytecode

**Status:** fixed-local-exact-CI-pending · **Affected:** gpu-client packaged builds from a previously compiled build/classes directory

A second incremental GPU build restored old CastleHubMap collision while retaining newer store code; the solid-service-fixture regression failed despite the source repair.

Cause / uncertainty: gpu-client/pom.xml copied **/*.class from build/classes as resources into target/classes. Some unchanged sources were not then recompiled.

Fix / next action: Exclude bytecode from GPU resources. Keep actual model/texture resources, compile all Java from source, clean-rebuild and repeat physical Castle checks.

Evidence and verification: build/castle-rebuild-compile.log and CastleRebuildTest before/after the clean source build. Source-level remedy matches the independent performance branch; no optimization flags are enabled.

Older-map obligation: Any GPU build using mixed resource bytecode needs a fresh source rebuild; this does not certify installed gameplay or performance.

### DEV-142 — Campaign and multiplayer equipment were split across incompatible store and reference pages

**Status:** fixed-scoped-local-verification · **Affected:** Campaign and multiplayer menus

The live shop exposed a small subset while the model library was a separate non-purchasing tab; three-tier overview pictures were reused as item icons.

Cause / uncertainty: Two unrelated panel implementations and family contact-sheet thumbnails.

Fix / next action: Replace both with EquipmentCatalogPanel and persistent full-tier campaign ownership; render 129 singular Blender item-tier icons.

Evidence and verification: EquipmentTierIconTest, CampaignMenuWorkflowTest, CampaignProgressionAndStoreTest; equipment-menu contact sheets and minimum-window UI captures.

Older-map obligation: Menus and shared field inventory affect all maps. No map geometry, release, GPU or physical two-machine acceptance is implied.

### DEV-143 — Campaign team booking supported one permanent assistant instead of five mission hires

**Status:** fixed-scoped-local-verification · **Affected:** Campaign lobby and host dispatch

Only one assistant could be purchased; guests had no campaign host flow or shared six-slot hiring rule.

Cause / uncertainty: A permanent boolean recruit flag and separate single-player/LAN mission launchers.

Fix / next action: Add per-mission reservations, refunds, six ordinary human/AI seats and host-only campaign settlement; retain migration of old saves.

Evidence and verification: CampaignCoopLobbyTest and CampaignEmbeddedDesktopTest use actual TCP/desktop workflows; retreat and guest isolation verified.

Older-map obligation: Menus and shared field inventory affect all maps. No map geometry, release, GPU or physical two-machine acceptance is implied.

### DEV-144 — PSX text-field artwork painted over entered connection details

**Status:** fixed-scoped-local-verification · **Affected:** All PSX-styled address, nickname, chat and password fields

The border painted its opaque centre after Swing rendered the text, concealing the real field content.

Cause / uncertainty: TextFieldBorder painted a full nine-slice image during border paint.

Fix / next action: Clip the border artwork to its outline, preserving text and caret visibility.

Evidence and verification: Minimum-window multiplayer-lobby captures and real desktop workflow.

Older-map obligation: Menus and shared field inventory affect all maps. No map geometry, release, GPU or physical two-machine acceptance is implied.

### DEV-145 — Medicine client sent decorated tier labels instead of protocol enum identifiers

**Status:** fixed-scoped-local-verification · **Affected:** LAN purchasing and medicine use

A request encoded BASIC / price / restoration text where the host requires BASIC, rejecting legitimate purchases.

Cause / uncertainty: LanClient concatenated Tier.toString rather than Tier.name.

Fix / next action: Encode stable enum names for BUY and USESUPPLY; retain display formatting solely in the UI.

Evidence and verification: CampaignCoopLobbyTest confirms actual pre-mission medicine purchase and private host stock snapshots.

Older-map obligation: Menus and shared field inventory affect all maps. No map geometry, release, GPU or physical two-machine acceptance is implied.

### DEV-146 — In-game updater launched installer before game released its installation files

**Status:** source-repaired-windows-install-acceptance-pending · **Affected:** Installed Windows client, UPDATES UI and EXE installer

Update requested from an open game could not complete setup and had no independent setup log.

Cause / uncertainty: Game launched setup before System.exit(0) without waiting for full termination.

Fix / next action: Use separate Windows helper in LocalAppData, wait for game exit, reverify SHA-256, launch Inno Setup and log independently. Gate next release on real Windows silent install.

Evidence and verification: User report 2026-10-28, code regression and workflow; Windows acceptance pending.

Older-map obligation: All Windows installations/upgrades, no map modifications; preserve saved profiles.

### DEV-147 — Rebuilt Castle bypassed retained GPU geometry

**Status:** fixed-scoped-rtx-and-ci-verified · **Affected:** Castle headquarters

Rebuilt Castle measured 35–73 ms median frames versus 18 ms before the rebuild, allocating 63–201 MiB per frame at 640x400 on RTX5070.

Cause / uncertainty: Castle calls drawGpuFaces with no investigation map, excluding it from the map-gated residency path; 220671 source triangles were reprocessed each frame.

Fix / next action: Retain authored Castle topology, state-keyed fixture/door assemblies, conservative spatial chunks and explicitly revisioned GPU buffers.

Evidence and verification: Corrected runtime source 06b76cce: renderer CI 40844278212 and full acceptance 40844284430 (277/277, zero skipped) passed. Actual RTX 5070 and native app-image all-ten-map GPU controls pass. Three matched 640x400 trials per view and supplementary 1280x800 raw timing/capture evidence: development/performance/hardware/2026-10-28-castle-regression/. Cold/new-view hitches and installed full-loop acceptance remain open.

Older-map obligation: Shared-path change requires per-map verification. No installed full-round, all-map acceptance or release is inferred.

### DEV-148 — PSX framebuffer quantization repeated costly floating-point rounding

**Status:** fixed-scoped-rtx-and-ci-verified · **Affected:** All rendered maps and Castle

JFR identified PSX finishFrame as a major CPU cost, 6–13 ms per frame in the baseline.

Cause / uncertainty: Six floating-point rounding operations per pixel repeated an invariant 256-channel/16-phase transform.

Fix / next action: Use precomputed channel lookup tables with exhaustive numerical and full-image pixel parity.

Evidence and verification: Corrected runtime source 06b76cce: renderer CI 40844278212 and full acceptance 40844284430 (277/277, zero skipped) passed. Actual RTX 5070 and native app-image all-ten-map GPU controls pass. Three matched 640x400 trials per view and supplementary 1280x800 raw timing/capture evidence: development/performance/hardware/2026-10-28-castle-regression/. Cold/new-view hitches and installed full-loop acceptance remain open.

Older-map obligation: Shared-path change requires per-map verification. No installed full-round, all-map acceptance or release is inferred.

### DEV-149 — Per-vertex emissive classification allocated lowercase material names

**Status:** fixed-scoped-rtx-and-ci-verified · **Affected:** All renderer paths

Castle JFR recorded StringLatin1.toLowerCase and material-name classification on every lit vertex.

Cause / uncertainty: Invariant material emission classification was recomputed for hundreds of thousands of vertices per frame.

Fix / next action: Cache emission classification by append-only material ID; retain live source factors and texture updates.

Evidence and verification: Corrected runtime source 06b76cce: renderer CI 40844278212 and full acceptance 40844284430 (277/277, zero skipped) passed. Actual RTX 5070 and native app-image all-ten-map GPU controls pass. Three matched 640x400 trials per view and supplementary 1280x800 raw timing/capture evidence: development/performance/hardware/2026-10-28-castle-regression/. Cold/new-view hitches and installed full-loop acceptance remain open.

Older-map obligation: Shared-path change requires per-map verification. No installed full-round, all-map acceptance or release is inferred.

### DEV-150 — Cosmetic light flicker invalidated retained shadow work

**Status:** fixed-scoped-rtx-and-ci-verified · **Affected:** Castle and shared stationary fixture lighting

First residency candidate produced 300–400 ms flicker stalls; retaining geometry alone was insufficient.

Cause / uncertainty: Brightness revisions repeated tessellation, ray visibility and source attenuation; bounded global shadow caches thrashed.

Fix / next action: Separate immutable receiver attenuation/occlusion probes from live brightness factors; retain regional revisions and invalidate on actual occluder changes.

Evidence and verification: Corrected runtime source 06b76cce: renderer CI 40844278212 and full acceptance 40844284430 (277/277, zero skipped) passed. Actual RTX 5070 and native app-image all-ten-map GPU controls pass. Three matched 640x400 trials per view and supplementary 1280x800 raw timing/capture evidence: development/performance/hardware/2026-10-28-castle-regression/. Cold/new-view hitches and installed full-loop acceptance remain open.

Older-map obligation: Shared-path change requires per-map verification. No installed full-round, all-map acceptance or release is inferred.

### DEV-151 — Cached van and map fixtures were still lit and repacked every frame

**Status:** fixed-scoped-rtx-and-ci-verified · **Affected:** House, School and authored investigation maps

Warm School approach cost about 52 ms CPU per baseline frame.

Cause / uncertainty: CPU face caching stopped at transforms; van, arrival scenery, enclosure, fixtures and portals still followed transient GPU batches.

Fix / next action: Retain actual static source arrays and GPU buffers; preserve live CRT, doors, stock, circuits, UV, portable lights and DOTS receiver geometry.

Evidence and verification: Corrected runtime source 06b76cce: renderer CI 40844278212 and full acceptance 40844284430 (277/277, zero skipped) passed. Actual RTX 5070 and native app-image all-ten-map GPU controls pass. Three matched 640x400 trials per view and supplementary 1280x800 raw timing/capture evidence: development/performance/hardware/2026-10-28-castle-regression/. Cold/new-view hitches and installed full-loop acceptance remain open.

Older-map obligation: Shared-path change requires per-map verification. No installed full-round, all-map acceptance or release is inferred.

### DEV-152 — Castle shelf labels were too small and consumed 129 materials

**Status:** fixed-scoped-rtx-and-ci-verified · **Affected:** Castle quartermaster store

A 105 mm label compressed family names to a few native pixels and created one material per family/tier.

Cause / uncertainty: Small world plaques plus independent 256x64 textures.

Fix / next action: Use larger high-contrast plaques in one atlas and full-resolution targeted family/tier text inside the Castle viewport.

Evidence and verification: Corrected runtime source 06b76cce: renderer CI 40844278212 and full acceptance 40844284430 (277/277, zero skipped) passed. Actual RTX 5070 and native app-image all-ten-map GPU controls pass. Three matched 640x400 trials per view and supplementary 1280x800 raw timing/capture evidence: development/performance/hardware/2026-10-28-castle-regression/. Cold/new-view hitches and installed full-loop acceptance remain open.

Older-map obligation: Shared-path change requires per-map verification. No installed full-round, all-map acceptance or release is inferred.

### DEV-153 — Native Hollow Creek geometry lacked GPU residency

**Status:** fixed-scoped-rtx-and-ci-verified · **Affected:** Native investigation map

Measured native interior view at about 337 ms median completed frame before the native retention fix.

Cause / uncertainty: A 96-entry transformed-face cache could churn, while native geometry still repeated CPU lighting and GPU repacking.

Fix / next action: Retain non-billboard native geometry and water bed; keep camera-facing billboards, actual water, live power and effects dynamic.

Evidence and verification: Corrected runtime source 06b76cce: renderer CI 40844278212 and full acceptance 40844284430 (277/277, zero skipped) passed. Actual RTX 5070 and native app-image all-ten-map GPU controls pass. Three matched 640x400 trials per view and supplementary 1280x800 raw timing/capture evidence: development/performance/hardware/2026-10-28-castle-regression/. Cold/new-view hitches and installed full-loop acceptance remain open.

Older-map obligation: Shared-path change requires per-map verification. No installed full-round, all-map acceptance or release is inferred.

### DEV-154 — Hidden GPU worker added a second frame pacing delay

**Status:** fixed-scoped-rtx-and-ci-verified · **Affected:** All OpenGL scenes

Completed-frame timing included roughly a second 16 ms scheduling interval even after CPU work dropped.

Cause / uncertainty: Swing submission pacing and a separate 60 Hz GLFW foreground throttle/idle interval stacked.

Fix / next action: Use event-requested noncontinuous GL rendering without a second foreground cap; bounded 240 Hz idle polling still sleeps and draws only requested work.

Evidence and verification: Corrected runtime source 06b76cce: renderer CI 40844278212 and full acceptance 40844284430 (277/277, zero skipped) passed. Actual RTX 5070 and native app-image all-ten-map GPU controls pass. Three matched 640x400 trials per view and supplementary 1280x800 raw timing/capture evidence: development/performance/hardware/2026-10-28-castle-regression/. Cold/new-view hitches and installed full-loop acceptance remain open.

Older-map obligation: Shared-path change requires per-map verification. No installed full-round, all-map acceptance or release is inferred.

### DEV-155 — Focused Castle workflow asserted obsolete pre-redesign editor menu source

**Status:** fixed-scoped-rtx-and-ci-verified · **Affected:** Focused Castle acceptance workflow

Main ba3d8885 focused workflow 40826909984 failed DeveloperEditorLaunchTest at the obsolete Main menu button assertion.

Cause / uncertainty: The earlier menu redesign moved the unchanged real editor action to the Extras page, but the focused packaging test still required removed JButton/Options source.

Fix / next action: Verify the actual main-to-Extras navigation and editor action route while preserving --editor, installer shortcut and shipped kit checks.

Evidence and verification: Corrected runtime source 06b76cce: renderer CI 40844278212 and full acceptance 40844284430 (277/277, zero skipped) passed. Actual RTX 5070 and native app-image all-ten-map GPU controls pass. Three matched 640x400 trials per view and supplementary 1280x800 raw timing/capture evidence: development/performance/hardware/2026-10-28-castle-regression/. Cold/new-view hitches and installed full-loop acceptance remain open. DeveloperEditorLaunchTest passes through the existing Extras route in full CI.

Older-map obligation: Acceptance-route repair only; no production menu change or map acceptance is implied.

### DEV-156 — Local AVG TLS interception prevents pinned encrypted regression connections

**Status:** external-environment-limitation · **Affected:** Local CommonNetworkTest and SecureLanIntegrationTest acceptance

Both local encrypted tests fail NET-04 / underlying NET-03 certificate substitution; 272 other manifest checks passed.

Cause / uncertainty: Handshake diagnostic received AVG Web/Mail Shield issuer instead of the generated Private Game Server self-signed issuer. IPv4-only profile reproduced the failure.

Fix / next action: Preserve strict pinning and system security settings. Verify encrypted tests in independent exact-source CI. No bypass or test waiver.

Evidence and verification: Local full regression 272/274 and bounded TLS issuer/fatal diagnostic, 2026-10-28. Corrected-source independent full CI 40844284430 passes both tests; local interception remains unchanged.

Older-map obligation: No networking production change; affects local verification, not a performance-root-cause claim.

### DEV-157 — Empty socket retention replaced a rendered hiding cupboard and churned GPU generations

**Status:** fixed-scoped-rtx-and-ci-verified · **Affected:** Retention candidate Church interior and authored sockets with overlapping rendering passes

The all-map experiment found Church interior still at about33ms, with1630 shadow rays and about4.9MB of mesh uploads every unchanged frame.

Cause / uncertainty: The hiding cupboard pass and the later non-rendering socket pass shared an auxiliary owner key. Retaining the empty list replaced/released the real cupboard chunk and orphaned a current-frame chunk.

Fix / next action: Skip empty geometry before auxiliary ownership/cache admission. Extend headless identity/array checks and actual-GPU zero-repeat-upload acceptance to all10 map types.

Evidence and verification: Corrected runtime source 06b76cce: renderer CI 40844278212 and full acceptance 40844284430 (277/277, zero skipped) passed. Actual RTX 5070 and native app-image all-ten-map GPU controls pass. Three matched 640x400 trials per view and supplementary 1280x800 raw timing/capture evidence: development/performance/hardware/2026-10-28-castle-regression/. Cold/new-view hitches and installed full-loop acceptance remain open. Earlier source 0f886e63 passed full 275/275 but failed original Mesa torch reuse; the unchanged assertion now passes with zero mesh uploads.

Older-map obligation: This is a regression discovered during this optimization. Audit every shared socket pass; do not infer correction from only House/School/native acceptance.

### DEV-158 — Emission classification used the sparse live-map sentinel as a dense BitSet index

**Status:** fixed-scoped-rtx-and-ci-verified · **Affected:** Retention candidate light-material classification and van views

Memory audit found VAN_MAP_MATERIAL is Integer.MAX_VALUE-2, which could expand the new emission cache by about256MiB per lighting instance.

Cause / uncertainty: The initial classification optimization assumed every nonnegative material ID belonged to the dense model texture registry.

Fix / next action: Reject reserved and unregistered texture IDs before BitSet insertion. Verify public emission calls for sparse IDs in a96MiB-heap child JVM.

Evidence and verification: Corrected runtime source 06b76cce: renderer CI 40844278212 and full acceptance 40844284430 (277/277, zero skipped) passed. Actual RTX 5070 and native app-image all-ten-map GPU controls pass. Three matched 640x400 trials per view and supplementary 1280x800 raw timing/capture evidence: development/performance/hardware/2026-10-28-castle-regression/. Cold/new-view hitches and installed full-loop acceptance remain open. Sparse reserved texture IDs also pass the 96MiB child-JVM budget check.

Older-map obligation: Regression caused by the performance optimization. Validate van views and all map types; never claim the GPU-byte bound alone bounds Java probe/cache memory.

### DEV-159 — GPU TV/telemetry pixels froze while unchanged map pixels were repeatedly uploaded

**Status:** fixed-scoped-rtx-and-ci-verified · **Affected:** Shared GPU TV and van CRT/map texture lifecycle

Backend refreshed only VAN_MAP_MATERIAL, on every draw; ordinary TV and telemetry textures could remain at their first upload.

Cause / uncertainty: Geometry residency and texture content had no independent source revision contract.

Fix / next action: Track source pixel revisions for television, telemetry and map materials. Update only changed dynamic textures; keep geometry retained. Verify changed GPU pixels and zero unchanged texture/VBO uploads.

Evidence and verification: Corrected runtime source 06b76cce: renderer CI 40844278212 and full acceptance 40844284430 (277/277, zero skipped) passed. Actual RTX 5070 and native app-image all-ten-map GPU controls pass. Three matched 640x400 trials per view and supplementary 1280x800 raw timing/capture evidence: development/performance/hardware/2026-10-28-castle-regression/. Cold/new-view hitches and installed full-loop acceptance remain open. GPU TV/map controls change pixels without geometry uploads; unchanged repeats upload neither.

Older-map obligation: Shared source fix requires real GPU controls and unchanged native model UVs. No claim of installed whole-game acceptance.

### DEV-160 — Walking across lighting-detail bands retessellated resident source geometry

**Status:** fixed-scoped-rtx-and-ci-verified · **Affected:** New shared world chunks and existing resident House shell

The33-pose diagnostic still exhibited recurring source-preparation spikes after head-turn retention passed.

Cause / uncertainty: Lighting tessellation depended on camera distance even for retained static resources; movement across bands rebuilt vertices/probes.

Fix / next action: Prepare fixed full close-range lighting topology for resident interior chunks; preserve exterior/native rules, original normals and UVs, and dynamic reference paths.

Evidence and verification: Corrected runtime source 06b76cce: renderer CI 40844278212 and full acceptance 40844284430 (277/277, zero skipped) passed. Actual RTX 5070 and native app-image all-ten-map GPU controls pass. Three matched 640x400 trials per view and supplementary 1280x800 raw timing/capture evidence: development/performance/hardware/2026-10-28-castle-regression/. Cold/new-view hitches and installed full-loop acceptance remain open. Translation identity/revision contract passes. Staged 33-pose CPU p95 improved about 268ms to 40ms; completed-frame cold/waypoint p95 remains 76ms.

Older-map obligation: Warm representative paths require repeated final timing and actual installed navigation. No claim that first visits or complete A–G acceptance are finished.

### DEV-161 — GPU UI acceptance could pass during the opening without reaching playable control

**Status:** fixed-scoped-ci-verified · **Affected:** GPU UI acceptance diagnostic; House, Farmhouse and School case lifecycle

The smoke accepted any new opaque GPU frame after GamePanel appeared, including an opening frame. It did not require hunter handoff, movement, extraction or another case, and invokeAndWait could exceed its advertised timeout indefinitely.

Cause / uncertainty: Framebuffer availability was used as a proxy for playable case readiness; UI polling itself had an unbounded EDT wait.

Fix / next action: Require sized first-frame readiness, actual opening skip or natural handoff, advancing hunter frames and GPU readback. Drive production WASD handlers over a collision-valid short van route, then the real X extraction/debrief/menu path in three successive cases. Bound EDT polling with one pending task. Preserve scoped captures and partial CSV evidence on failure.

Evidence and verification: Source10e2f4e: GPU40923212823 and full40923217003 SUCCESS. Mesa explicitly passes House3350ms, Farmhouse11360ms (natural intro), School1048ms hunter readiness, real WASD movement, RETREATED, debrief/menu and repeat dispatch. Separate clean Windows native image passes RTX all-ten-map controls with exact embedded JAR hash; that hidden review does not exercise Swing input.

Older-map obligation: Three publicly playable contract locations only. Short van movement and retreat do not certify interior routes, a successful investigation, installed multiplayer, physical player acceptance or A-G completion.

### DEV-162 — Spatial light invalidation relit batches whose actual contributors stayed unchanged

**Status:** fixed-scoped-rtx-and-ci-verified · **Affected:** Retained world material batches across Castle and map types

Castle still showed recurring preparation tails despite unchanged-color upload suppression; conservative light spheres overlapped batches with unrelated or occluded contributions.

Cause / uncertainty: Every material in a spatially dirty chunk evaluated all prepared vertex probes even when none of its actual base, normal or emissive source factors changed.

Fix / next action: Snapshot the exact union of contributing source factor bits per material. Skip only exact matches; preserve full RGB evaluation, topology/door invalidation and immutable GPU revisions. Provide factorDependencies.disabled fallback.

Evidence and verification: Source99baa799: GPU40927250498 and full40927255222 SUCCESS,282/282 with zero skipped, packaged TCP/desktop/native and all three actual GPU case cycles. Java17 GPU package, one-ULP precision/ownership, all-ten-map dense-reference vertex parity under2GiB, fallback and actual RTX controls pass.32 image pairs match exactly. Three1280x800 Castle trials reduce exterior p95 from34.50ms to20.60ms; final package confirms36.56 to20.09ms. Slightly slower medians/library p95 are disclosed. Six scene cycles remain bounded. Evidence: development/performance/hardware/2026-10-29-light-factor-dependencies/.

Older-map obligation: Shared implementation requires all-map controls and final CI. Scripted warm camera timings do not certify physical routes, cold starts, installed full rounds or release.

### DEV-163 — Castle wall sconce meshes embedded in 42 cm masonry

**Status:** code-fix-pushed-awaiting-visual-and-ci-verification · **Affected:** Castle Beckenburg wall-mounted imported GLB lamps and light-switch mounting plates

2026-10-29 user screenshot shows illuminated wall fitting partially swallowed by the corridor stonework.

Cause / uncertainty: The castle renderer centred an imported sconce on its saved fixture coordinate. Several nominal wall mounts sit 0.20-0.27 m from the wall centerline while the stone is 0.42 m thick; the mesh's rear triangles and synthetic mounting plate were inside the visible masonry face. Earlier mountingWall checks validated an anchor near a wall, not transformed GLB vertex clearance.

Fix / next action: Source commit 732a0911c00c3faaa8ca80893635bfdf6289e69e computes the rear extent of the actual on/off GLB geometry, fits the rendered fixture in front of the real masonry face with 18 mm clearance, and places its plate at the wall. Preserves the editor scene coordinates, switched lighting and UV materials. Close-up on/off inspection captures added; do not treat image generation or packaging alone as visual signoff.

Evidence and verification: User-supplied screenshot 2026-10-29; CastleLightingPlacementRepairTest now measures world-transformed vertices for each authored wall fitting in both power states against the visible masonry face. Source commit 732a0911 pushed/readback verified; GitHub focused checks 40945959372, GPU 40945959443, full-round 40945959390 and installer 40945959398 pending at time of ledger entry. Close-up frames emitted by CastleLiveRepairVisualTest in build/e2e/castle-repairs.

Older-map obligation: Inspect all wall-mounted model families in other investigation sites individually for physical wall thickness, mesh-pivot placement, wall-face support, and clipping. A Castle-only repair does not certify other maps.

### DEV-164 — Castle equipment shelf lettering unreadable at retro framebuffer resolutions

**Status:** code-change-pushed-awaiting-gpu-capture-and-installed-review · **Affected:** Castle Beckenburg retail room, all 129 tiered equipment plaques; desktop first-person UI

2026-10-29 user screenshot shows shelf lettering degraded into scattered white pixels/strokes, despite a modern wide display, and HUD type extremely small.

Cause / uncertainty: GraphicsOptions defaulted new profiles to a 200p world framebuffer, used nearest-neighbor filtering on glyph atlases, and painted native-resolution HUD lettering at fixed 12-14 physical pixels. High output window resolution did not increase the 3D raster target. Long-distance legibility of physically small plaques remains an independent constraint.

Fix / next action: Code commit a28b6c2a6760404a13942364f44d976c003254e5 adds an explicit GPU 1080p internal render choice with CPU 720p safety cap, raises new-profile GPU/CPU defaults to 540p/360p, preserves ultra-wide projection when render dimensions cap out, scales Castle HUD independently of world pixelation, and uses linear sampling only on the signage atlas. Retro/nearest-neighbor remain available by selection. Do not claim readable distant text without comparing exact hardware screenshots.

Evidence and verification: Screenshot supplied 2026-10-29 (2048x574). GraphicsOptionsTest updated to check ultrawide projection, explicit FHD fallback and HUD scale. Source push and readback verified at a28b6c2; focused workflow 40946601310, GPU 40946601416, full-round 40946601337 and installer 40946601450 pending when logged. Need 200p/540p/720p/1080p same-camera physical GPU captures and player confirmation that labels are readable at practical shopping distance.

Older-map obligation: Review readable authoring/UI text on van control consoles, map screens, evidence boards, journals and other playable sites independently. Castle font correction alone is not cross-map certification.

### DEV-165 — Castle warehouse incorrectly opens campaign Quartermaster inventory

**Status:** source-fix-pushed-exact-CI-pending · **Affected:** Beckenburg Castle warehouse display/counter; rear Quartermaster supply wardrobe; multiplayer lobby

The warehouse stock counter previously called openQuartermaster() and exposed the campaign wallet and owned equipment instead of multiplayer contract inventory, making the Castle appear to contain two competing stores.

Cause / uncertainty: CastleHubPanel.use(null) was wired to its campaign store for the physical warehouse counter. The network lobby already had a common EquipmentCatalogPanel with independent host-authoritative stock, but the warehouse had no route to that mode.

Fix / next action: Code commit 61fe14c5e34bc6206517aab99424f7384901bfb2 routes the warehouse to InvestigationGame.openMultiplayerInventory(), selecting LanLobbyPanel's real Equipment & Supplies tab, while retaining rear armory/wardrobe campaign profile purchases. Network desk still opens the connection tab. Both wrappers inherit EquipmentCatalogPanel; no new store or inventory transfer is introduced.

Evidence and verification: Source push and remote readback confirmed. New CastleInventoryWorkstationTest compares real inventory catalog entries, exercises physical workstation callback separation and checks disconnected network purchases remain disabled; added to regression-tests.txt. Java compilation, exact-SHA workflow and installed Castle player interaction are not yet accepted; focused Castle checks 40947285516 were in progress on the code commit when logged.

Older-map obligation: Audit actual inventory/store routing in campaign preparation, multiplayer van rack, campaign co-op host/guest screens and existing menu back navigation individually. One Castle routing fix is not cross-mode persistence acceptance.

### DEV-166 — Investigation doors snap between poses rather than physically swinging

**Status:** animation-source-pushed-CI-and-installed-review-pending · **Affected:** House, Farmhouse, School and shared HouseMap investigation portals (solo/LAN, including host door snapshots)

Pressing E instantly swaps closed/open geometry; no visible motion, and the old portal-key cache path also resets the complete OpenGL scene while repainting.

Cause / uncertainty: Door state was stored solely as a Boolean, consumed directly by imported door geometry and interaction rays. Portal geometry caching reused the portal-only residency key, forcing GPU scene ID change when the same door switched pose.

Fix / next action: Source commits 814d7c1, ad2176c, d1c857c animate per-door eased visual opening/closing for 450ms, support mid-swing reversal and unchanged host snapshots, render moving leaves as transient meshes with settled-pose-specific retained GPU keys and maintain ray alignment. 62b0ef3 adds DoorSwingTest to full regression manifest b0afe0a4. 6def2a3 aligns solo door sound with LanSound.Cue.DOOR. Compile blocker in unrelated GPU acceptance smoke variable repaired in f8fe48e. Authoritative open/closed collision remains immediate; physical sweep collision is a separate acceptance/design concern. Broad CPU lighting-cache invalidation remains in issue #44.

Evidence and verification: Live user report 2026-10-29 (House door snaps, hitches, flashes). Source pushes verified individually; full-round CI and real installed Windows GPU frame/motion capture pending. Do not claim measured 450ms visible animation without hardware frames and profile.

Older-map obligation: Audit all authored site door meshes, hinge pivots, double doors/gates, Castle doorway and van cargo doors independently; shared HouseMap source alone does not certify older maps.

### DEV-167 — 26 numbered model assets require source/UV/material and rendered visual repair

**Status:** 26-of-26-targeted-source-asset-repairs-four-angle-render-verified · **Affected:** All 26 original owner-flagged asset identities now have documented scoped repairs and exact four-angle in-game visual acceptance. Separate full-scene placement, silhouette design, material quality and gameplay fit remain independently auditable.

Owner flags suspected misaligned exterior textures and severe graphical defects across imported and 3D-authored asset families; supplied list has 26 distinct numeric asset IDs and an additional screenshot.

Cause / uncertainty: Imported source glTF atlas V-orientation mismatches, six authored bedroll seam UV mistakes, clipped Hunter hair/boots from board segmentation, blue Twin side-head matte defect, and detached source alpha strips in Possessive/Twins packaged ghost atlases. Corrected with original source artwork/materials only.

Fix / next action: Targeted corrective work complete for all 26 original flags. Original textures/meshes preserved except approved six bedroll UV restorations; source-aligned imported 3D V sampling, actor alpha segmentation and indexed ghost strip removals shipped. Continue independent review of map placement/collision, detailed texture aesthetic quality, character motions and the other 85 non-targeted diagnostic review flags. Do not treat completion of these specific repairs as acceptance of unrelated defects; visual checks remain non-release-blocking.

Evidence and verification: Original UV and art review JSON: development/assets/texture-revision-review-2026-11-01.json. 25 earlier assets accepted in actual four-angle run 41405326985 (artifact 13122898542), source atlases/GLBs audited with zero UV warnings and direct source-V vs legacy-V screenshots. Final Twins ghost source fix in runtime archive commit 828659674770cdc00a6c0b07be9f4b7521588b88; original 181 other ghost sprites byte-identical and 15666 stray alpha pixels cleared. Latest Windows developer-room run 41408571829 (artifact 13125290269) PASS actual game build + TwinsAtlasRuntimeTest (all 8 facing sheets/256 cells) + PossessiveAtlasRuntimeTest + original scene screenshots. Pixel-identical Twins at 0/180°, only 642 pixels changed each at 90/270° inside x274-282 y206-321 around removed strand; both figures intact.

Older-map obligation: Audit any level using these assets after mapping; House, Castle, Farmhouse, School and every shared fixture use require independent placement/orientation and material captures. Visual defects are diagnostic and not release blockers.

### DEV-168 — Editor GLB reduction silently substitutes checkerboard for imported material/UV artwork

**Status:** source-guard-and-Windows-CI-verified-existing-derivatives-unaudited · **Affected:** AssetImportDialog, ImportedAssetLibrary and any GLB optimization imported via the developer editor

Previously nonzero GLB regression could be published with a single fallback checkerboard texture replacing original named material bindings and per-part source UV/material identity after dialog warning.

Cause / uncertainty: EditorMeshPipeline flattens imported GLB geometry to triangles without source material identifiers; EditorGlbWriter.encode writes a generic one-material checkerboard on export. Warning/confirmation alone still permitted destructive derivative publication.

Fix / next action: Commit 2a3271b adds persistent publish guard; 0af5f9f disables unsafe GUI button and blocks bypass; df65881 adds regression check for unchanged level-zero GLB bytes and refusal of nonzero GLB regression. Source-only/no-destructive GLB publishing remains available; material-preserving decimation remains future implementation.

Evidence and verification: Editor guard commits 2a3271b and 0af5f9f read back, test df65881 added; matching Windows installer workflow 41380366927 and developer room workflow 41380366943 completed success at source ce0c01d4. Original GLB publishing byte-identical at level zero, reduced GLB material-loss path blocked. GUI manual interaction and preexisting damaged derivative audit remain open. Asset-specific visual assertions removed from release-blocking GlbPropsTest in eec70ef. Reconciled native host suite passes: AssetImportTopologyGuiTest drives the actual slider, proves nonzero GLB publish stays disabled and level0 re-enables; AssetImportRuntimeTest proves exact level0 bytes and native nonzero refusal. Preexisting damaged derivatives remain unaudited.

Older-map obligation: Identify previously imported GLB derivatives that used nonzero regression; compare source/runtime SHA and visual texture mapping, replace only after authoring a real corrected derivative. Do not infer existing models have been repaired by the new guard.

### DEV-208 — Castle resident placement assertion treated its own body as an obstruction

**Status:** corrected-locally; wider resident approach review pending · **Affected:** CastleSupportArtTest / Matthis Rook placement inspection

The advisory CI and local test failed at Matthis Rook although CastleHubIntegrationTest intentionally requires that players cannot walk through his body.

Cause / uncertainty: The placement test called player traversable at the resident centre, which correctly includes CastleArchive.occupies.

Fix / next action: Use a shared ground-placement query that excludes only character occupancy and retains floors, walls, doors, furnishings, warehouse and service fixture collision. Keep normal player traversal unchanged.

Evidence and verification: CastleSupportArtTest now passes with negative controls for unsupported ground, furniture intersection and preserved keeper-body collision; CastleHubIntegrationTest also passes. Historical branch identifier DEV-169 collided with main Hollow Creek alpha-tree defect; canonical merged identifier is DEV-208. See DEFECT_ID_RECONCILIATION_2026-11-01.md.

Older-map obligation: Separate support/placement from self-collision in actor placement checks; never relax actual furniture or wall collision to satisfy a test.

### DEV-170 — Labeling the USER authoring slot throws Unknown site USER

**Status:** corrected-locally; remote acceptance pending · **Affected:** MissionSites.title / campaign-board and lobby authoring labels

Full-round advisory run 41273695792 and local MissionBoardIntegrationTest reach IllegalArgumentException Unknown site USER while enumerating authoring enums.

Cause / uncertainty: The USER enum was added to editor availability but is deliberately absent from built-in MissionSites/campaign order; title delegated unconditionally to that roster.

Fix / next action: Provide the established User authored level slot label without opening a user project or adding a campaign chapter.

Evidence and verification: Actual MissionBoardIntegrationTest passes locally, covering packaged pictures, asynchronous loading, retained 22-chapter order, prototype privacy and locked/unlocked dispatch.

Older-map obligation: Keep custom authoring slots separate from fixed built-in/campaign roster and saved ordinals; do not silently substitute a built-in map.

### DEV-171 — House graybox top stair intersects the foyer partition

**Status:** corrected-locally; pilot only · **Affected:** Isolated Briar House rebuild graybox

The first actual physics run failed the last ascent steps, top landing and descent; swept navigation could not reach four upper rooms.

Cause / uncertainty: The stair ended 0.20m from a lower-floor partition, within the capsule/wall clearance at the last rise.

Fix / next action: Move the foyer partition and doorway to leave 0.60m clearance beyond the top stair endpoint. Keep the shared physics unchanged.

Evidence and verification: house-rebuild-physics-first.log and house-rebuild-reachability-first.log reproduce failure; final physics and 61,328-position swept reachability pass, with all 15 zones and sockets reached.

Older-map obligation: Recheck both ends of every flight after changing room proportions; graph or midpoint checks do not certify landings.

### DEV-172 — House graybox service panel placed inside its doorway aperture

**Status:** corrected-locally; pilot only · **Affected:** House rebuild heating/laundry service room

AuthoredUtilitiesTest HOUSE_REBUILD failed service-panel has clear operating position.

Cause / uncertainty: Panel and cellar/service doorway were authored at the same wall coordinate.

Fix / next action: Move the panel onto actual solid wall north of the doorway, preserving the aperture and working approach.

Evidence and verification: house-rebuild-utilities-first.log fails; current utility checks pass twelve switches, the panel, fourteen acoustic links and two stairs.

Older-map obligation: Review fixtures against every actual aperture and clear aim/operating point after layout edits.

### DEV-173 — Legacy House envelope overlaid on a smaller rebuilt interior

**Status:** corrected-locally; shared renderer source binding · **Affected:** House rebuild graybox; future revised sources using House gameplay slot

Native export showed blank outer walls and a legacy roof hiding actual revised windows and entrances.

Cause / uncertainty: Renderer selected fixed original House cladding/porch coordinates by the HouseMap site enum, independent of the revised LevelDefinition.

Fix / next action: Bind that fixed legacy skin only to the original house source ID. Revised definitions use their own authored shell. Register and test the correct suburban setting.

Evidence and verification: HouseRebuildShellTest failed at x24 beyond the revised footprint before correction; now checks 20,817 elevated shell vertices and correct setting. Real runtime/native views and rejected source are retained.

Older-map obligation: Audit every hardcoded facade/roof/enclosure against its source coordinates before reusing a gameplay slot.

### DEV-174 — Basement architectural opening does not remove the broad world-ground sheet

**Status:** fixed in isolated House pilot; other cellar/cavity sites remain open · **Affected:** Rebuilt House pilot and older cellar/cavity maps requiring independent audit

Architectural stair/deck openings can pass movement while a separate broad world-ground/backdrop sheet still spans the basement elevation. Owner explicitly requires genuine world-floor holes.

Cause / uncertainty: Room/deck voids and the broad terrain/backdrop surface currently have different generation paths. The latter is not removed by the architectural void.

Fix / next action: Use saved editor AdaptiveTerrainTopology punched leaves and actual TerrainEntranceMesh geometry; bind the same missing triangles into real rendering, native body/support and optical queries. Test an unpunched negative control and original PNG byte preservation.

Evidence and verification: HouseRebuildTerrainTest passes: 30 saved subdivisions/52 punched leaves; source PNG unchanged; reopen, idempotence and atomic invalid-boundary rollback; 3,430 exact shared render/body/support/optical faces; real cellar-stair player/object/sight passage; separately published unpunched editor-world control blocks all three. HouseRebuildPhysicsTest still reaches all 15 zones through 61,328 swept positions. TerrainSceneNativePhysicsTest passes existing terrain/rail, stale/tamper and replay checks. Reopened native pilot matches 37,949 runtime triangles/UVs; basement cutaway and actual stair views retained. No all-map acceptance.

Older-map obligation: Individually audit House, Farmhouse, School, Church, institutional service basements, crypts, mines, tunnels and all stacked remote cavities.

### DEV-175 — Selection and campaign regressions still assume three built-in maps

**Status:** fixed in regression coverage; no map art or promotion acceptance implied · **Affected:** SiteSelectionTest and CampaignStoryProgressionTest on current nine-built-in catalog plus USER authoring slot

Selection fails its nine-total-enum assumption before rendering; campaign fails after third victory because it assumes chapter four must be unavailable.

Cause / uncertainty: Tests predate the current built-in availability and USER slot; restoring the older lock state would change current production behavior.

Fix / next action: Assert the explicit nine-built-in dispatch contract, isolate the USER profile, render each distinct source and retain prototype rejection. Exercise physical campaign victories, no-skip locks, duplicate-receipt rejection, restart persistence and correct source ownership through all nine available chapters; reject all unintegrated destinations. Run these independently in the existing map audit workflow.

Evidence and verification: Before: SiteSelectionTest fails all-planned-families assertion; CampaignStoryProgressionTest fails fourth-chapter-unavailable assertion. After: both pass against packaged JAR, all nine maps rendered and all nine actual victory gates persisted. Logs in development/levels/rebuild/verification/recovery-*-{before,after}.log. This does not certify generic institution architecture, LAN or physical GPU acceptance.

Older-map obligation: Current dispatch availability does not accept any generic institution layout; rebuilt geometry still requires all per-map gates.

### DEV-176 — All-map reviews construct USER without a persisted project

**Status:** fixed-scoped; raw310/310 regression plus actual all11-map GPU and three UI cycles accepted · **Affected:** WorldResidencyGpuReview and LevelOpeningFlyoverTest; RoomAcousticPresetsTest, HidingBudgetTest, AllMapLightingCoverageTest, VanLightingAndCastleDoorTest; AllMapArchitecturalJoinTest

Mesa workflow 41320075324 passes the core smoke, then world-residency exits after HOLLOW_CREEK; the retained frame CSV has every built-in/prototype but no USER row. Flyover regression similarly instantiates every enum without its authoring prerequisite.

Cause / uncertainty: USER is a selected authored-project slot, not a built-in map. The reviews construct it in an empty default authoring profile.

Fix / next action: Create and persist an explicit House-derived USER source inside an isolated review evidence home; assert absent selection fails first, load the actual saved source, restore the real home before frame sampling, and retain USER coverage. Reuse that real persisted fixture through AuthoredTestSupport for all four all-map regressions, retain every enum value and original behavioral assertion, and clean only the temporary profile.

Evidence and verification: Downloaded exact-SHA Mesa artifact ends after HOLLOW_CREEK-torch-repeat. New packaged LevelOpeningFlyoverTest passes all eleven map enum values including persisted USER and unchanged physical spawn. Java 17 package/all-root-tests compile. Real GPU review remains pending new-source CI; do not call it fixed on GPU yet. Full-round 41324869128 at fcf7c52e passed packaged TCP/UI but raw manifest was 260 passed / 12 failed / 27 unexecuted of 299, including these four absent-USER failures. All four were reproduced locally, then passed with the persisted fixture: soundscape sample parity, 12 locations at 101 hiding densities, USER physical lighting, and all 11 van map types. Before/after logs retained under build/editor-agent-evidence. Earlier exact-source GPU 41322252644 at d4294b4c passed; no newer hardware or full manifest acceptance is inferred. Next full-round 41327744396 at 11b57bbf executes all 300 tests (291 pass / 9 fail / 0 unexecuted), exposing the same missing-USER prerequisite in AllMapArchitecturalJoinTest. It now retains all original boundary/aperture assertions with the persisted fixture and passes 7,544 closed and 1,962 open probes including USER locally; exact newest-source CI pending. Exact ff93601a source CI: full-round 41330858899 raw summary passed=303 failed=0 skipped=0 manifest=303; native editor host 41330853164 success. See evidence/editor-full-round-ff93601a.txt. Physical installed acceptance remains separate. Later main6b9088f4 GPU41362364664 still lacks this branch repair and reproduces the same pre-USER stop. Combined c679aedb GPU41368128355 SUCCESS, actual residency all11 map types including persisted USER and all three UI cycles; raw excerpts in evidence/editor-gpu-c679aedb.txt. Physical hardware acceptance remains separate.

Older-map obligation: Every enum-driven review must handle USER prerequisites explicitly rather than assuming a built-in source or selecting a developer project.

### DEV-177 — Windows checkout rewrites hash-bound editor terrain source bytes

**Status:** local attribute fix and Git checkout-filter proof; exact-source CI pending · **Affected:** Saved House editor terrain world.properties and published world.obj; HouseRebuildTerrainTest in Windows CI

Full-round 41320069837 at ddcef3be passes 61,328 pilot positions and utilities but fails Editor scene integrity. Raw full-manifest transcript contradicts advisory step success labels.

Cause / uncertainty: Git autocrlf=true converts the committed LF properties and OBJ into CRLF, changing both persisted source and mesh fingerprints. Reproduced with git cat-file --filters: stored OBJ SHA 0c18e683... becomes 1db2c7cf..., world properties df6b917f... becomes 2f03b7b3....

Fix / next action: Mark saved rebuild terrain-world files -text in .gitattributes so byte fingerprints survive checkout; add an actual Git-filter verifier for true/false/input modes and include concrete inspector errors in terrain failures. Do not weaken hash or tamper checks.

Evidence and verification: Git-filter verifier proves all five saved world files match committed bytes across Windows/Unix/input checkout configurations. Packaged House pilot passes again, including 61,328 swept positions and real editor-hole positive/negative controls. Existing source and native physics hashes remain unchanged. Exact new-source Windows CI remains pending.

Older-map obligation: Preserve exact authoring source and published geometry bytes for every future saved terrain world; line-ending portability is not permission to bypass source staleness or tamper checks.

### DEV-178 — Editor agent CLI recursively encodes Path as Iterable

**Status:** fixed-scoped; local Java 17 native and public-process tests pass · **Affected:** New EditorAgentCli inspect/capture/save JSON output

Initial native service acceptance raised StackOverflowError while encoding an inspection document path.

Cause / uncertainty: java.nio.file.Path implements Iterable<Path>; the output encoder treated path components as nested lists before string conversion.

Fix / next action: Encode Path as a string before the iterable branch. Keep public CLI process coverage for inspect, edit/save, capture, native parity and reopen.

Evidence and verification: EditorAgentServiceTest, EditorAgentGuiTest and tools/verify_editor_agent_cli.py pass locally on Java 17; real House/Castle PNGs generated and inspected. Exact-source CI pending publication.

Older-map obligation: Protocol correction applies to every supported host; no map geometry changed or broader map fitness asserted.

### DEV-179 — Mission deletion renumbers surviving authored object IDs

**Status:** fixed-scoped; local native, GUI and headless regressions pass; exact-source CI pending · **Affected:** LevelAuthoringSession hierarchy, saved BSCN nodes, undo and agent ID lookup

Removing prop index zero changed every later prop-N identity; saving recreated IDs from indices.

Cause / uncertainty: Hierarchy and serializer regenerated IDs from current list positions, and history retained only prop values.

Fix / next action: Preserve BSCN v1 node IDs, assign new UUID IDs on add, snapshot IDs with native props, resolve agent edits by persisted ID and keep one native scene.

Evidence and verification: LevelAuthoringIdentityTest passes deletion, stable-ID move, shared undo/redo, deterministic repeated saves, reopen and real HouseMap prop equality. Existing Windows GUI and public CLI tests rerun.

Older-map obligation: Existing BSCN v1 IDs remain valid; no map geometry or broad fitness acceptance implied.

### DEV-180 — Existing USER authoring session follows another active custom document

**Status:** fixed-scoped; local two-project ownership regression passes; exact-source CI pending · **Affected:** LevelAuthoringSession USER id, documentPath, save and reopen

After opening one USER session then selecting a second project, the older session resolved the second project ID/path dynamically and could save its props there.

Cause / uncertainty: Session identity and destination were read from global UserLevelLibrary.activeId/activeDocument on every access.

Fix / next action: Pin document ID and path at session open. Save and reopen use the pinned source and destination.

Evidence and verification: LevelAuthoringIdentityTest creates two projects with different templates, saves/reopens the older session while the second is active, proves second bytes unchanged, then verifies first through real game loading.

Older-map obligation: Ownership protection covers all new USER sessions; no corrupted prior project is automatically reconstructed.

### DEV-181 — Mission BSCN load silently flattens unsupported portable transforms

**Status:** fixed-scoped; local native rejection and state-preservation regression passes; exact-source CI pending · **Affected:** LevelAuthoringSession native loader and LevelPlans authored mission load

Portable nodes can express parenting, pitch/roll and nonuniform scale, but mission loading discarded those fields and built different native geometry.

Cause / uncertainty: Loader consumed only world position, yaw and scaleX without checking unsupported transform fields; native apply also used weaker prop validation.

Fix / next action: Reject unsupported parent/pitch/roll/nonuniform transformations explicitly and apply the same native prop validator in editor and game loading.

Evidence and verification: LevelAuthoringIdentityTest saves an actual valid portable BSCN with nonuniform scale, requires native reopen failure and checks the entire previous scene snapshot remains intact. Existing scene persistence and real editor checks pass.

Older-map obligation: Existing supported yaw/uniform-scale documents are preserved. Unsupported externally authored documents require explicit conversion rather than silent geometry loss.

### DEV-182 — BSCN output budget checked only after full serialization

**Status:** fixed-scoped; local portable crash/storage and native persistence tests pass; exact-source CI pending · **Affected:** RuntimeSceneDocument save output, including mission and terrain/rail documents

Valid node/component count limits still allow more than 8 MiB of serialized properties; the previous implementation checked file size only after writing the complete temporary file.

Cause / uncertainty: MAX_FILE_BYTES was enforced after stream closure rather than during output, permitting unbounded excess temporary-file writes relative to the document budget.

Fix / next action: Route BSCN encoding through EditorDocumentStore with a bounded output stream, staged readback validation, forced journal and atomic replacement. Leave the current document unchanged on budget failure.

Evidence and verification: EditorDocumentStoreTest rejects both a writer exceeding 8 MiB and a real BSCN scene with otherwise valid 30-by-128 large-property entries, preserving exact target bytes and no pending journal. Actual child-process crash/replay, native mission recovery, terrain/rail publication and HCS2 physics regressions pass locally.

Older-map obligation: The bound applies to all BSCN producers without changing format v1 or existing source files. It does not certify other serializers or physical power-loss durability.

### DEV-183 — LAN HUD receives but does not paint sound captions and strike warnings

**Status:** fixed-scoped; exact-source full regression 41330858899 passed 303/303 · **Affected:** LanWorldPanel accessibility, muted/no-device sound cues and telegraphed strike feedback

Unchanged LanSoundHudTest and LanTelegraphedHudTest fail with absent lavender caption pixels and warning pixel counts 0/0 despite valid received state.

Cause / uncertainty: The current paint path no longer consumes activeSoundCaption, threatUntilTick or threatOutcome; only state and diegetic monitor updates remain.

Fix / next action: Restore transient caption and strike/resolution bars, including arrival/journal overlays. Keep simultaneous warning/caption rows distinct and expire host-timed feedback.

Evidence and verification: Original pixel assertions now pass; added simultaneous-caption, rendered expiry and resolved-evasion checks pass through native LanWorldPanel with real local TCP peers. Inspected PNGs under development/quality/evidence/lan-hud-2026-11-01 are actual headless HUD component captures, not world/gameplay acceptance. Exact ff93601a source CI: full-round 41330858899 raw summary passed=303 failed=0 skipped=0 manifest=303; native editor host 41330853164 success. See evidence/editor-full-round-ff93601a.txt. Physical installed acceptance remains separate.

Older-map obligation: The shared LAN HUD fix applies across maps. Installed-game, muted-device and arrival-case acceptance remain separate.

### DEV-184 — Ground recovery snaps an accepted lower-floor move onto overlapping stairs

**Status:** fixed-scoped; exact-source full regression 41330858899 passed 303/303 · **Affected:** GroundRecovery, HouseMap position-relative support, LanWorld host ticks and LanSpatialEvidenceIntegrationTest

Accepted House movement at x21.93/y0/z18 is changed on the next tick to x22.18/y1.5/z18 in room5, then subsequent real TCP motion is collision-rejected.

Cause / uncertainty: Walking resolves support relative to current elevation, while recovery validates against groundHeight and nominal stair height above valid feet.

Fix / next action: Reuse the exact position-relative supportNear query already used by movement before treating supported feet as penetration. Preserve existing traversability/body-height checks and true penetration recovery.

Evidence and verification: GroundRecoverySupportParityTest fails against the previous compiled GroundRecovery at the exact floor-jump assertion and passes after the fix. Accepted host movement, same-floor round trip, .06m/10m penetration controls, shipped room spawns, original graded-terrain recovery in an isolated output directory, authored stairs/physics, and four-peer spatial evidence round all pass locally. Exact ff93601a source CI: full-round 41330858899 raw summary passed=303 failed=0 skipped=0 manifest=303; native editor host 41330853164 success. See evidence/editor-full-round-ff93601a.txt. Physical installed acceptance remains separate.

Older-map obligation: No geometry or body-clearance rule changed. Broader underside/stair visual clearance and each map fitness gate remain unaccepted; valid supported poses must not be reinterpreted by a second height authority.

### DEV-185 — Legacy regression contracts reject approved voice/sprite packs and neutral Spirit Box readings

**Status:** fixed-scoped; exact-source full regression 41330858899 passed 303/303 · **Affected:** GhostVoicePackRoutingTest, DirectionalSpritesIntegrationTest and HandheldSpiritBoxEvidenceTest

Tests expect exactly 20 strike routes, 45 total frames, and automatic voice detection from generic event strength; actual approved runtime has 24 strike routes, 61 frames and host-attested spoken responses.

Cause / uncertainty: Assertions predate eight unique voice expansion clips, sixteen Castle support frames, and the explicit real-vocal Spirit Box contract.

Fix / next action: Retain exact coverage of all twenty legacy strike lines plus four approved contextual strike lines; verify all sixty-one frames and actor directions; require static from generic scans and verify real spoken positives through the existing host-authorized TCP integration.

Evidence and verification: Updated exact-set/count and native depth/manifestation assertions pass. GhostVoiceExpansionTest, SpiritBoxResponsesTest and LanSpiritBoxAudioIntegrationTest also pass, proving the expansion, actual packaged speech, real host vocal replies, withheld/non-vocal/range rules and no van-wide broadcast. No production asset or speech-evidence rule was changed. Exact ff93601a source CI: full-round 41330858899 raw summary passed=303 failed=0 skipped=0 manifest=303; native editor host 41330853164 success. See evidence/editor-full-round-ff93601a.txt. Physical installed acceptance remains separate.

Older-map obligation: These are compatibility assertions for current approved source contracts, not art-quality, subjective audio or full gameplay acceptance.

### DEV-186 — Acoustic direction fixtures cross native stairs while claiming one room/floor

**Status:** fixed-scoped; exact-source full regression 41330858899 passed 303/303 · **Affected:** LanGhostHearingTest, LanSoundTest and spatial evidence integration diagnostics

House hall spawn plus X offsets resolves to staircase support at +1.5m/room5 or -1.5m/room9, so forged same-room source assertions receive null instead of a bearing.

Cause / uncertainty: Older flat-room offsets and zero-height listener constructors ignore authored stairs and actual bedroom elevation.

Fix / next action: Select cardinal hearing points through native room/support/body queries, use the hall flat axis with a rotated listener for full stereo pan, and retain actual listener elevation. Spatial TCP checks now read host BODY/SITE state, fail immediately on rejection, and verify actual same-room/floor distance and return coordinates.

Evidence and verification: All original direction, door attenuation, privacy, stereo-energy and malformed-frame assertions pass locally. The native probe records the exact changed floor/room for the old offsets; real TCP spatial-range completion passes after the independent GroundRecovery production fix. Exact ff93601a source CI: full-round 41330858899 raw summary passed=303 failed=0 skipped=0 manifest=303; native editor host 41330853164 success. See evidence/editor-full-round-ff93601a.txt. Physical installed acceptance remains separate.

Older-map obligation: Fixture selection never changes geometry, substitutes a preview map, drops a bearing or skips a native collision check. Cross-floor audio-model acceptance remains a separate gate.

### DEV-187 — Castle editor properties saves lack journal recovery and distinct previous versions

**Status:** fixed-scoped; exact-source native CI and full 306/306 regression accepted · **Affected:** CastleLevelEdits native properties persistence and Castle human/headless editor service

Mission documents support interrupted-save replay and explicit previous-save recovery, while Castle properties writes have no journal or previous version and timestamps change identical saves.

Cause / uncertainty: Castle retains a separate temporary-file replacement serializer with a non-atomic fallback, rather than the shared validated EditorDocumentStore.

Fix / next action: Preserve native Properties format and all existing migrations; use the bounded native decoder for readback and recovery validation, deterministic sorted bytes, journaled atomic replacement and one prior distinct document. Enable recovery on the authoritative singleton through the same human menu, shared history and CLI.

Evidence and verification: CastleDocumentRecoveryTest passes native full-state roundtrip, removed objects, no-op backup retention, corrupt source and invalid backup refusal, same-singleton recovery/undo and legacy migrations. Existing CastleEditorGeometryTest and CastleLightingPlacementRepairTest pass unchanged. Extended actual Windows editor menu and headless fresh-process acceptance pass. Exact a99d0a1d source native workflow41352028873 SUCCESS and full41352069449 raw passed=306 failed=0 skipped=0 manifest=306; see evidence/editor-full-round-a99d0a1d.txt.

Older-map obligation: Castle properties only. Mission/BSCN format unchanged. Decoding uses private native codec state with no renderer/editor/agent binding. Generic store process-crash tests remain applicable; physical power-loss and installed-game acceptance remain separate.

### DEV-188 — Headless editor JSON uses Windows system encoding instead of UTF-8

**Status:** fixed-scoped; exact-source native CI and full 306/306 regression accepted · **Affected:** EditorAgentCli main response stream and Castle hierarchy labels

The public Castle inspect response contains bullet characters encoded as Windows-1252 byte 0x95, causing the UTF-8 Python client to fail decoding JSON.

Cause / uncertainty: Input explicitly selects UTF-8, but the response stream reused System.out and therefore inherited the Java 17 platform encoding.

Fix / next action: Wrap the captured stdout stream with an explicit UTF-8 PrintStream before redirecting runtime diagnostics to stderr. Force Windows-1252 as the test JVM default and require exact native Unicode labels in decoded responses. Follow-up also encodes stderr as UTF-8, preserving Unicode startup diagnostics under a Windows-1252 JVM default.

Evidence and verification: Expanded verify_editor_agent_cli.py reproduces UnicodeDecodeError before the fix and passes mission/Castle edit, save, fresh-process reopen and recovery after the fix; native Castle bullet labels survive UTF-8 decoding. Exact a99d0a1d source native workflow41352028873 SUCCESS and full41352069449 raw passed=306 failed=0 skipped=0 manifest=306; see evidence/editor-full-round-a99d0a1d.txt. Extended process tests use an accented Castle home path and decode both channels strictly as UTF-8.

Older-map obligation: Applies to the shared local CLI for every host. Does not add a listener, authentication or arbitrary script execution.

### DEV-189 — Human mesh import report prints literal newline escapes

**Status:** fixed-scoped; native41358289381 and standalone41358301989 CI accepted · **Affected:** AssetImportDialog style/texture warnings and source report

The actual import dock displays a literal backslash-n between style commentary and source metadata, producing an unnecessarily long horizontally clipped report line.

Cause / uncertainty: Three report strings contain a double-escaped newline instead of a Java newline escape.

Fix / next action: Use actual newline escapes; assert the complete human report contains no literal backslash-n sequence.

Evidence and verification: Actual before capture development/quality/evidence/mesh-topology-2026-11-01/import-review-before-newline-fix.png; corrected actual GUI capture import-review.png. AssetImportTopologyGuiTest loads real OBJ, reviews topology, publishes actual runtime GLB and proves exact source preservation.

Older-map obligation: Human import report only; no model geometry/material or game-world edits.

### DEV-190 — Human asset publication can reread a source changed after review

**Status:** fixed-scoped; exact-source native/standalone and full307/307 at0fb70ac2 accepted · **Affected:** AssetImportDialog source observation/publication and ImportedAssetLibrary provenance

A reviewed OBJ can be published with stale geometry but newly changed source archive; GLB regression0 rereads and directly publishes externally replaced geometry that the user never reviewed.

Cause / uncertainty: The human importer retained decoded triangles but no fingerprint of their exact captured bytes, and publication reopened the source without a revision precondition.

Fix / next action: Parse the captured bounded OBJ/STL bytes and pin their SHA; retain the exact GLB/native load fingerprint. Refuse changed sources before publication and check again when capturing registry input. Preserve exact native authoring bytes alongside the original/runtime model. Disable edits during publication.

Evidence and verification: AssetImportTopologyGuiTest modifies actual OBJ geometry after review and replaces a valid zero-regression GLB; both attempts fail with no new approved asset or source overwrite. Successful publication compares every runtime triangle corner and exact original/authoring archive bytes. Exact0fb70ac2 native41360968300 and standalone41360973706 SUCCESS; full41360981244 raw307/307, no failed/skipped entries. Later native adapter guard adoption has separate evidence.

Older-map obligation: Human import path only. Existing programmatic publish remains source-compatible; the human workflow uses the checked entry point. No existing art or map geometry changed.

### DEV-191 — Native topology prototype used a backup validator as a save precondition

**Status:** fixed-scoped; exact-source native/standalone and full307/307 at0fb70ac2 accepted · **Affected:** New EditorTopologyProject competing-editor saves and portable EditorDocumentStore API

The new two-editor regression initially failed because a stale native topology editor overwrote a newer save despite its validator throwing IOException.

Cause / uncertainty: The existing format validator intentionally controls whether prior bytes qualify for backup; invalid prior data may still be replaced. It is not an authorization or optimistic-concurrency hook.

Fix / next action: Add a separate saveIfUnchanged operation with an expected SHA or expected-missing state, checked under the writer lock after staging and before backup/journal/source replacement. Adopt it for the native topology project. Keep existing unguarded save behavior explicit and compatible.

Evidence and verification: EditorTopologyProjectTest reproduces two live projects on one native file, verifies refusal preserves newer disk and unsaved live edits, then observes/reopens/saves successfully. EditorDocumentStoreTest injects a noncooperative write during staging and external deletion; guarded saves preserve both. Existing real child-process crash/replay tests continue to pass. Exact0fb70ac2 native41360968300 and standalone41360973706 SUCCESS; full41360981244 raw307/307, no failed/skipped entries. Later native adapter guard adoption has separate evidence.

Older-map obligation: Caught in the new indexed-topology prototype before delivery. Legacy mission/Castle callers retain their prior semantics until explicit resource-fingerprint adoption; do not infer that all existing serializers now enforce this new precondition.

### DEV-192 — Castle snapshots expose mutable station arrays and compare array identity

**Status:** fixed-scoped; native/standalone CI and raw310/310 full regression accepted · **Affected:** CastleLevelEdits.Snapshot and shared history observations

Caller-owned station arrays/maps can mutate a retained snapshot; two snapshots of equal native state compare unequal because arrays use identity.

Cause / uncertainty: The public record had no defensive constructor/accessor or structural station equality.

Fix / next action: Defensively own all collections/coordinate arrays, return isolated coordinate copies, validate structural fields and implement value equality/hash.

Evidence and verification: CastleSnapshotContractTest mutates constructor inputs and accessor arrays, verifies retained/native values and equality/hash, then restores and saves/reopens exact state. Exact47b4ad47: native41367120522 and standalone41367126147 SUCCESS; full41367132677 raw310/310, zero failed/skipped. See evidence/editor-native-contracts-2026-11-01.json.

Older-map obligation: Native snapshot values only; no map/model geometry changes.

### DEV-193 — Castle decoder accepts station states rejected by the human editor

**Status:** fixed-scoped; native/standalone CI and raw310/310 full regression accepted · **Affected:** Castle properties load and previous-save recovery

Nonfinite/out-of-world station coordinates can pass the old native decoder; backup validation can install data the live editor cannot represent.

Cause / uncertainty: Station properties were parsed without the bounds/finite checks used by setStation; format validation did not validate a complete native snapshot.

Fix / next action: Validate complete native snapshot invariants during decode and reject invalid station data before recovery replacement.

Evidence and verification: NaN station backup and x999 current-file controls preserve exact valid target/live scene on failure; actual human bounds are shared. Exact47b4ad47: native41367120522 and standalone41367126147 SUCCESS; full41367132677 raw310/310, zero failed/skipped. See evidence/editor-native-contracts-2026-11-01.json.

Older-map obligation: Invalid external/backup data is refused; existing valid native saves and legacy geometry/lighting migrations retain their acceptance.

### DEV-194 — Mission native loading silently discards unsupported metadata and contradictory yaw

**Status:** fixed-scoped; native/standalone CI and raw310/310 full regression accepted · **Affected:** LevelAuthoringSession BSCN adapter

Unknown component properties, custom node labels/layers and yaw fields inconsistent with portable transforms are ignored then lost or misinterpreted on native save.

Cause / uncertainty: The native adapter extracted its known prop fields without checking the rest of the portable node contract.

Fix / next action: Refuse unrepresentable fields/metadata and inconsistent duplicate yaw while retaining the existing legacy optional-yaw fallback.

Evidence and verification: EditorSchemaCompatibilityTest preserves exact source bytes/live state for all negative cases and proves legacy yaw save/reopen; existing native identity tests remain intact. Exact47b4ad47: native41367120522 and standalone41367126147 SUCCESS; full41367132677 raw310/310, zero failed/skipped. See evidence/editor-native-contracts-2026-11-01.json.

Older-map obligation: No claim that arbitrary generic BSCN data is supported by native mission runtime; unsupported schemas fail explicitly.

### DEV-195 — Castle future layout versions can be rewritten as current version2

**Status:** fixed-scoped; native/standalone CI and raw310/310 full regression accepted · **Affected:** Castle properties schema migrations

A format1 file with layout3 is interpreted with current fields and a later save stamps layout2, risking loss of future data.

Cause / uncertainty: Migration branches checked only whether layout versions were below2; newer unsupported versions were accepted.

Fix / next action: Reject future/invalid layout versions before applying live state; retain supported legacy0/1 and current2 handling.

Evidence and verification: EditorSchemaCompatibilityTest protects exact bytes and native state for future versions of all six layout families. Exact47b4ad47: native41367120522 and standalone41367126147 SUCCESS; full41367132677 raw310/310, zero failed/skipped. See evidence/editor-native-contracts-2026-11-01.json.

Older-map obligation: Older supported migrations remain tested separately; this does not admit unknown future schemas.

### DEV-196 — Explicit Castle codecs eagerly initialize an unrelated default profile

**Status:** fixed-scoped; native/standalone CI and raw310/310 full regression accepted · **Affected:** CastleLevelEdits default singleton initialization

Constructing a codec for an explicit path can fail on, read or migrate the default user Castle instead of only touching the selected resource.

Cause / uncertainty: The default runtime singleton was created in the outer class static initializer.

Fix / next action: Initialize the authoritative runtime through a lazy holder accessed only by current(); explicit codecs remain scoped to their own paths.

Evidence and verification: A fresh child with a corrupt unrelated default profile successfully saves/reopens an explicit file without touching the sentinel; actual current() still rejects the corrupt default. Exact47b4ad47: native41367120522 and standalone41367126147 SUCCESS; full41367132677 raw310/310, zero failed/skipped. See evidence/editor-native-contracts-2026-11-01.json.

Older-map obligation: Agent adapters continue to require the actual singleton; no independent codec instance is admitted as game runtime.

### DEV-197 — Value-equivalent Castle door order changes rotate prior-save history

**Status:** fixed-scoped; native/standalone CI and raw310/310 full regression accepted · **Affected:** Castle Properties serializer and distinct backups

Removing/reinstalling the same fixed door changes map insertion order and serialized numeric indices despite identical native authoring value.

Cause / uncertainty: Door entries were emitted in mutable insertion order.

Fix / next action: Serialize fixed door socket assignments in canonical ID order.

Evidence and verification: CastleSnapshotContractTest proves equal native values after a door cycle produce identical bytes and retain the actual prior distinct saved version. Exact47b4ad47: native41367120522 and standalone41367126147 SUCCESS; full41367132677 raw310/310, zero failed/skipped. See evidence/editor-native-contracts-2026-11-01.json.

Older-map obligation: Canonical writer ordering is compatible with existing ID-based readers; no door geometry or behavior changes.

### DEV-198 — Headless request cap is checked after unbounded readLine buffering

**Status:** fixed-scoped; native/standalone CI and raw310/310 full regression accepted · **Affected:** EditorAgentCli input protocol

A delimiter-free oversized request can grow the entire line in memory before the advertised request limit is checked.

Cause / uncertainty: BufferedReader.readLine was called before inspecting the length.

Fix / next action: Read with a bounded builder, emit a structured protocol failure and stop on overflow/read failure; document code-unit limits precisely.

Evidence and verification: The native service test supplies a guarded unending reader and proves failure before32768 characters are consumed and before any scene edit. Exact47b4ad47: native41367120522 and standalone41367126147 SUCCESS; full41367132677 raw310/310, zero failed/skipped. See evidence/editor-native-contracts-2026-11-01.json.

Older-map obligation: Trusted local protocol only; no remote listener/security sandbox is claimed.

### DEV-199 — Failed undo/redo consumes history before the native restore succeeds

**Status:** fixed-scoped; native/standalone CI and raw310/310 full regression accepted · **Affected:** Portable EditorHistory and all project adapters

A host restore failure can leave partial state and pop/move undo/redo entries, preventing a safe retry; rollback failure can hide the original command cause.

Cause / uncertainty: History stacks were modified before restore committed, and rollback exceptions replaced original failures.

Fix / next action: Restore from peeked targets first, roll back partial failure, commit stack transfer only on success, and preserve rollback failures as suppressed causes.

Evidence and verification: EditorHistoryFailureTest proves retained stacks/state/retry and failure provenance; it fails against the previous compiled0fb70ac2 implementation and passes after repair. Exact47b4ad47: native41367120522 and standalone41367126147 SUCCESS; full41367132677 raw310/310, zero failed/skipped. See evidence/editor-native-contracts-2026-11-01.json.

Older-map obligation: Native adapters and standalone projects share this boundary; arbitrary persistently broken host restores cannot be claimed successful.

### DEV-200 — Room audit omitted real exterior views beyond its search radius

**Status:** corrected-locally; complete-roster baseline review pending · **Affected:** School and generic institution exterior zones; other large exterior ownership volumes

The first baseline recorded 56 views and four missing views for School and several institutions despite existing outdoor support.

Cause / uncertainty: The seven-metre camera search stayed inside building footprints that overlap the exterior gameplay ownership rectangle. ALL also included the unrelated USER authoring slot.

Fix / next action: Search the full owning room bounds when the local search finds no valid same-zone camera; skip USER in built-in ALL scope while retaining explicit USER audits. Permit direct CASTLE capture.

Evidence and verification: Corrected School/Prison/College and other targeted audit logs show 60 directional views, zero missing, and an explicitly displaced exterior camera. Actual matrices and PNGs: development/levels/rebuild/captures/baseline/. Historical branch identifier DEV-167 was reused independently on main; canonical merged identifier is DEV-200. See DEFECT_ID_RECONCILIATION_2026-11-01.md; original evidence is retained.

Older-map obligation: Inspect displaced cameras and coverage individually; camera availability never proves route or architecture acceptance.

### DEV-201 — Rowan Pike intersects the Castle network-desk chair

**Status:** corrected-locally; publication and visual closeout pending · **Affected:** Castle Beckenburg operations / Rowan Pike

After excluding the keeper self-collision from placement inspection, CastleSupportArtTest reproducibly failed for Rowan Pike.

Cause / uncertainty: Resident (23.75,17.40) intersected chair_mp_5 at (24.1,17.7), whose collider is half-width .35/half-depth .39 plus .21 body clearance.

Fix / next action: Move the shared resident/render/interaction anchor to (23.30,17.40), preserving the chair and network station.

Evidence and verification: Measured collision inspection identified the exact chair; CastleSupportArtTest, CastleRelationsTest and CastleHubIntegrationTest pass locally with JDK17. Separate real-renderer after views: development/levels/rebuild/captures/after-resident-fix/castle/. Historical branch identifier DEV-168 was reused independently on main; canonical merged identifier is DEV-201. See DEFECT_ID_RECONCILIATION_2026-11-01.md; original evidence is retained.

Older-map obligation: Audit every resident and supporting furniture independently; do not generalize to investigation props or moved editor fixtures.

### DEV-202 — OBJ import implicitly welds distinct authored position indices

**Status:** fixed-scoped; exact-source native/standalone and raw310/310 regression accepted · **Affected:** EditorMeshPipeline OBJ reader, EditorMeshTopology derivation and actual human native mesh import

Six referenced source positions containing a distinct coincident pair become five native vertices, creating an unintended shared edit and disconnected fan. Moving one authored source vertex can drag another piece.

Cause / uncertainty: The OBJ parser discarded source position references and native import inferred connectivity only from part and coordinate equality.

Fix / next action: Retain immutable optional per-corner source position identities; build native topology from those identities, keeping corner UVs separate. Verify contradictory source references, preserve the legacy unindexed constructor and keep BTOP v1 native state unchanged.

Evidence and verification: Exact1e505d5e parser/topology classes reproduce the six-to-five assertion failure. Corrected native importer keeps six identities, original sharing and UV seams, negative OBJ indices/fans, shared undo/redo and save/reopen. Real human import/move/publication verifies the unrelated coincident vertex remains stationary, exact source/native archives and every native GLB corner. Maximum120000 indexed faces/360000 vertices pass in the independent Java17 JAR under256MiB. See evidence/obj-native-identity-2026-11-01/. Exact4be25e3a native41390504086 and standalone41390498466 SUCCESS; full41390507634 raw310/310, zero failed/skipped.

Older-map obligation: Existing saved BTOP IDs remain authoritative and are never silently split/rebuilt. Previously welded native projects require explicit source-based reimport and edit review. Unindexed STL/render buffers retain their documented per-part geometric connectivity policy; full normal/material interchange remains open.

### DEV-203 — Inline OBJ face comments are parsed as vertex references

**Status:** fixed-scoped; exact-source native/standalone and raw310/310 regression accepted · **Affected:** Portable OBJ source reader and human importer

An otherwise readable authored face followed by an inline comment fails with an invalid OBJ/# position-index exception.

Cause / uncertainty: Only whole comment lines were skipped; the face tokenizer included trailing comment words as vertex references.

Fix / next action: Strip trailing comments before bounded tokenization and stream lines. Bound position/UV declarations to the portable source-index budget.

Evidence and verification: Exact1e505d5e parser fails the targeted comment check with Invalid OBJ at line11 caused by index #. Corrected reader retains the same authored corner identities with or without the trailing comment; native source bytes remain archived unchanged. See evidence/obj-native-identity-2026-11-01/obj-inline-comment-negative.txt. Exact4be25e3a native41390504086 and standalone41390498466 SUCCESS; full41390507634 raw310/310, zero failed/skipped.

Older-map obligation: No authored source file is rewritten. Existing native formats and previously readable OBJ geometry retain their version and native state.

### DEV-204 — Model publication can pair an old reviewed runtime mesh with newer native authoring state

**Status:** fixed-scoped; exact-source native/standalone and raw310/310 regression accepted · **Affected:** Actual AssetImportDialog, EditorTopologyProject native observations/reviewed saves and runtime publication

A real native edit after review can save/archive native X=-0.25 while publishing cached runtime X=0.0; independent UI dirty flags may miss native changes.

Cause / uncertainty: The reviewed reduction was checked against a UI mesh cache, not the native owner/revision. Native save and later serialization were separate state observations.

Fix / next action: Use owner-bound immutable native observations, refuse stale review before writes, atomically validate/save/capture the reviewed native bytes and fingerprint, publish the same reviewed render input, and derive unsaved state from the native project. Explicit re-review refreshes actual native state while retaining valid selection.

Evidence and verification: AssetImportPublicationRevisionGuiTest against exact4be25e3a UI source prints actual runtime x=0.0 / actual native x=-0.25 and fails. Corrected actual UI refuses all stale writes, then publishes matching native/runtime data after real human re-review. Native tests verify immutable saved bytes, fingerprint, later unsaved edits, and distinct epoch handling. Full local native service/modeling/GUI/CLI suite passes. See evidence/face-modeling-2026-11-01/. Exact848f8b21 native41396364295 and standalone41396361150 SUCCESS; full41396371226 raw310/310, zero failed/skipped. GPU41396374380 all11-map residency and three UI cycles also SUCCESS.

Older-map obligation: Existing mismatched published derivatives are not silently rewritten; compare their original/native/runtime provenance individually. No new agent/MCP mesh operations are enabled.

### DEV-205 — Topological editor exposes every tool row and crowds out the working viewport

**Status:** fixed-scoped; local actual GUI/native tests passed, remote acceptance pending · **Affected:** AssetImportDialog / topological model workspace only

Owner reports a horrifically messy modeling UI: all primitive, vertex, selection, transform, face, repair and reduction controls compete with a shallow viewport; ordinary editing shows a redundant comparison.

Cause / uncertainty: Unconditional stacked controls and fixed two-view assumptions accumulated with native modeling operations.

Fix / next action: Use a model-owned contextual workspace, native part outliner, single working view, explicit/pending comparisons, collapsible diagnostics and focus restoration. Preserve actual native handlers/history/persistence/runtime publication. Main level-view source is unchanged.

Evidence and verification: EditorTopologyWorkspaceGuiTest plus actual human picking/selection/modeling/export/publication tests pass locally. Fixture captures: development/quality/evidence/topology-workspace-2026-11-01/. Full standards applicability review: docs/editor/requirements/MODULE_WORKSPACE_AUDIT.md.

Older-map obligation: No map/model art modified. Other modules require their own workflow-specific workspace audit; do not inherit this model layout or claim complete gold-standard acceptance.

### DEV-206 — Authored corner normals are discarded by model import, native state and export

**Status:** fixed-scoped; exact pre-fix negative control and local native/portable/human tests passed; remote acceptance pending · **Affected:** OBJ/STL importer, native indexed corner schema, transform/modeling paths, OBJ/GLB export and read-only GLB normal inspection

A real OBJ normal(0,0,2) becomes zero direction in the actual game decoder after editor publication. Hard-edge/smooth corner attributes cannot survive native save or transforms.

Cause / uncertainty: Portable vertices and native corners contained only position/UV data; vn/STL directions were discarded and generated GLB omitted NORMAL.

Fix / next action: Keep validated optional immutable corner normals; write v2 only for normal-bearing BTOP and preserve v1 otherwise. Retain raw OBJ/native values, apply inverse-transpose/differential under native edits, preserve stable IDs and UVs, validate real importer readback and write normalized glTF NORMAL. Show actual attributes in the model-only overlay/read-only GLB view.

Evidence and verification: Exact3cd88128 old importer/writer fails NormalLossControl through the real GlbProps decoder with0,0,0; corrected classes pass the identical control. EditorCornerNormalsTest --stress and AssetImportNormalsGuiTest pass; see evidence/corner-normals-2026-11-02/.

Older-map obligation: No source art rewritten. Existing normal-less native files retain v1 encoding. Audit each previously published derivative against its own authored source; do not silently replace registered imports or claim all materials preserved.

### DEV-207 — Imported model normals do not reach software lighting and are dropped during subdivision

**Status:** fixed-scoped; local production placement/subdivision/native-light parity tests passed; GPU/full regression pending · **Affected:** SceneRenderer imported mesh placement, software drawLitFace and directional-light preparation

Even decoded authored normal metadata is omitted from imported Face attributes; software drawLitFace samples without normals and its subdivided children lose normal/owner attributes. GPU has a directional path that software does not match.

Cause / uncertainty: Imported faces used the no-normal constructor, software vertices bypassed directional sampling, and subdivision used the geometry-only constructor.

Fix / next action: Carry actual registered import directions through world inverse-transpose/yaw, preserve/interpolate normal and owner attributes through software subdivision, and share the existing directional adjustment between software and GPU vertex preparation. Preserve concurrent source-alpha repairs during reconciliation.

Evidence and verification: AssetImportNormalsGuiTest inspects actual production faces after nonuniform placement/yaw, actual subdivided faces/owners and actual native House lamp direction response; software and GPU prepared RGB match. Real fixture rasterizer and UI captures are in evidence/corner-normals-2026-11-02/.

Older-map obligation: No broad visual signoff: inspect existing authored-normal props/van and renderer behavior individually. Required actual GPU/full-round checks remain source-pinned and pending until observed.

### DEV-169 — Hollow Creek 67 original alpha tree billboards rendered as opaque black rectangles

**Status:** 67-tree-alpha-software-render-verified-catalog-repaired-GPU-and-live-site-visuals-pending · **Affected:** 67 approved Hollow Creek original glTF BLEND tree billboards with black-background card defect (software renderer), plus 17 additional production-approved models (16 terrain source meshes and Matthis proxy) previously miscataloged as SOURCE_ONLY. One pavilion-services model remains genuinely review-only.

The original developer-room gallery rendered 67 alpha tree crowns as opaque black rectangular cards. The source registry incorrectly called 16 already-approved terrain mesh exports and the approved Matthis proxy unapproved. Separate maps containing terrain source assets are NOT automatically completed or approved for gameplay.

Cause / uncertainty: Software triangle renderer ignored original GLB alphaMode BLEND and source PNG alpha, flattening source transparent pixels to black while incorrectly writing depth. Foliage palette variation discarded ARGB. The catalog sampled only transparent UV corners and categorized source models using a narrower HollowCreekScene allowlist rather than the true LevelAssets.approved allowlist.

Fix / next action: Source glTF ALPHA BLEND support and hardware-neutral RGB+alpha material decoding committed. The exact original 67 tree cards now draw transparent background and actual branch art in software renderer (67/67 individual real gallery comparisons visually accepted). The first native gallery completed success at run 41410917812. Approved Matthis proxy and all 16 TerrainFoundationAssets meshes are now correctly cataloged as WORLD_MODEL, with only pavilion services remaining SOURCE_ONLY; their site maps remain unfinished. Corrected regression now passed as an advisory step in Windows workflow 41412400705. Continue production GPU/live-site lighting and side-view inspection separately. Neither style nor art tests block release.

Evidence and verification: Source glTF audit 41409865926 artifact 13124519834: 67 two-triangle source BLEND textures, valid PNG alpha (representative source 520105 clear + 30093 partial + 39626 opaque pixels). Windows real game build and source material test in 41410917812 success; developer-room 998-asset screenshot artifact 13125039813 verifies all 67 black rectangles gone. In the black-card bounding regions over 67 assets, total dark pixels fell from 2,242,086 to 6,059; sample asset 284 fell from 33,429 to 86. Actual image comparisons preserve branch silhouettes. Production runtime catalog now 997 LOADED, 1 source-only. The original 26 DEV-167 target review remains closed. Corrected audit regression step 'Validate Hollow Creek source approval and catalog membership' succeeded in followup Windows run 41412400705 at commit 23af62e, confirming 16 TerrainFoundationAssets meshes and Matthis recognized as approved original source models. 67 before/after actual screenshot comparisons and per-asset pixel metrics packaged separately for owner review.

Older-map obligation: Verify Hollow Creek tree billboard compositing in playable camera view, under weather/lighting and hardware GPU backend (if active). Inspect 16 approved terrain GLBs independently before any integration of unfinished maps. Pavilion services remains unapproved until separately justified; never auto-promote it.

### DEV-209 — Normal-aware OBJ export can bypass degenerate refusal or leak an unchecked error

**Status:** fixed-scoped; exact intermediate negative control and current native I/O refusal tests passed; remote acceptance pending · **Affected:** EditorObjWriter normal-bearing and missing-normal degenerate face export

Intermediate47f563b exports a degenerate triangle when its corners have explicit normals; missing-normal degenerate faces can escape as unchecked normal-math errors rather than the existing checked export refusal.

Cause / uncertainty: The new normal writer validated attributes only through effective corner normals; authored values bypassed geometric face validation and derived-vector validation used unchecked math exceptions.

Fix / next action: Validate finite nondegenerate face geometry independently of authored attributes and adapt invalid normal math to IOException in writing/readback. Preserve missing or pre-existing destinations on refusal.

Evidence and verification: Exact47f563b EditorObjWriter compiled from its Git blob fails the current production-backend test with Degenerate face exported, authored=true. Corrected EditorCornerNormalsTest checks both authored/missing-normal cases and structured IOException with no destination publication. See evidence/corner-normals-2026-11-02/.

Older-map obligation: Regression was found and corrected in this local implementation before remote publication. Keep normal metadata validation distinct from geometry validity; no silent repair or broad map acceptance.

### DEV-210 — Identity model transforms can alter raw normal values or introduce floating-point drift

**Status:** fixed-scoped; exact intermediate negative control and current native epoch/history checks passed; remote acceptance pending · **Affected:** EditorTopologyProject previewTransform with unit scale or whole-turn rotation

Intermediate47f563b accepts axis scale1 on authored normal(0,0,2) because normal normalization changes its stored magnitude. Whole-turn rotations can also produce tiny coordinate differences instead of a no-op.

Cause / uncertainty: Identity detection relied on equality after floating-point coordinate and attribute transformation rather than rejecting mathematically neutral input before computing it.

Fix / next action: Reject unit scales and rotations equivalent to whole turns before native geometry/normal work. Preserve exact snapshot, epoch and history on refusal.

Evidence and verification: Exact47f563b native project compiled from its Git blob fails the current identity-transform regression with Invalid normal input accepted. Corrected EditorCornerNormalsTest checks axis scale1, rotation360 and unchanged snapshot/revision/history.

Older-map obligation: No source art rewritten. Intermediate regression was corrected before remote publication; identity actions must not manufacture dirty state, new asset hashes or metadata normalization.

### DEV-211 — Merged atlas checkpoint has two failing full-regression art assertions despite green advisory workflow

**Status:** open; exact terminal raw 308/310 recorded; source-art review required before any golden update · **Affected:** GhostSpriteAtlasIntegrationTest, TwinBodyVisualTest at merged source bcd7469828f8736a5dfb5c6266a48543573fb294

Full run41486307374 completed with green workflow status but raw regression job125417465992 reports passed308 failed2 skipped0 manifest310. Ghost archive bytes6883065 differ from expected6868626; Twin A side-frame fingerprint differs from previous approved silhouette.

Cause / uncertainty: Full manifest is explicitly advisory in this workflow. Main source atlas repairs were merged; existing fixed-byte/fingerprint expectations have not been reconciled through actual visual/source review. Green workflow status is insufficient acceptance.

Fix / next action: Retain raw failures and previous goldens, compare actual atlas/side silhouette against approved original source before determining test staleness versus visual regression. Do not change artwork or bless new hashes merely to pass. Keep factual renderer acceptance separate.

Evidence and verification: development/quality/evidence/editor-full-round-bcd74698.txt records exact failures and summary. Companion GPU41486319863 job125420512759 genuinely passes actual11-map residency and3 UI cycles; see editor-gpu-bcd74698.txt.

Older-map obligation: No blanket old-map/art acceptance. Preserve original owner art, all source repairs, and existing visual-golden tests until reviewed.

### DEV-212 — OBJ material declarations are silently discarded by geometry-only native import and GLB fallback

**Status:** fixed-scoped; actual human material window/native/bundle/runtime tests pass locally; exact-source CI pending · **Affected:** EditorMeshPipeline OBJ mtllib/usemtl, EditorTopologyProject native schema and EditorGlbWriter

Actual previous3ca731b5 pipeline imports a material-bearing OBJ but exposes no native material table and emits generated checkerboard instead of original map_Kd PNG/material assignment.

Cause / uncertainty: Native triangles held geometry/UV/normals only and the GLB writer generated one fallback atlas; no immutable source material/binding schema or dependency capture existed.

Fix / next action: Capture original PNG, factors, alpha/cutoff, wraps and UV origin in native BTOPv3 with face references. Use explicit MTL parsing/context/dependency validation, same preview/history/save and actual game GLB compiler; preserve material-bearing OBJ via validated atomic bundle. Plain export refuses material loss. Material window has independent layout and native UV/face selection, not a parallel scene.

Evidence and verification: Exact3ca731b5 Git blobs compiled in build/material-negative-control reproduce geometry-only import. EditorMeshMaterialTest checks native/history/fresh open, real OBJ/MTL dependencies and bundle readback. AssetImportMaterialsGuiTest drives actual face selection/window/PNG/cancel/apply/save/publish, exact UV/RGBA/wrap/tint and real renderer screenshot; repeated scoped previews release transient runtime registrations. Fixture evidence material-workspace-2026-11-02.

Older-map obligation: Do not bulk-reimport existing assets, overwrite source textures or flatten GLB node/pivot/material provenance. Existing geometry-only v1 and normal v2 paths remain compatible. Full painting/PBR/editable source GLB remains open.

### DEV-213 — OBJ material dependency containment compares lexical aliases with canonical asset root

**Status:** fixed-scoped; exact318 standalone41508789243 and native41508797528 SUCCESS, including real material/UV regressions · **Affected:** EditorObjMaterials.Reader.resolve; standalone Windows material import41506322166/job125486457313

A dependency inside the selected asset root is rejected with Material dependency escapes the asset root. Native suite passes while Windows standalone material test fails; an in-root directory junction reproduces the same exception against exact644 editor JAR.

Cause / uncertainty: Asset root was canonicalized with toRealPath, but containment first compared an uncanonicalized dependency path, so equivalent alias/short-path spellings could differ.

Fix / next action: Resolve the dependency to its canonical real path before checking containment against the canonical root. Retain relative-path restrictions and actual outside-root refusal; record canonical dependency fingerprints.

Evidence and verification: Exact644 standalone raw excerpt under evidence/material-uv-2026-11-02. DependencyAliasControl compiled against the exact published JAR fails through build/material-root-alias, then passes against corrected source with one original material. EditorMeshMaterialTest still refuses ../outside dependency and changed MTL. Actual source-aware OBJ/MTL human UV test also passes. Corrected source31876318 exact standalone job125494894856 passes native material and maximum UV steps; native job125494926967 raw human material/UV and complete suite PASS are retained in material-uv evidence.

Older-map obligation: No bulk source migration or bypass of containment. Keep absolute/outside-root, unsupported options and changed-dependency guards. Await exact new-source standalone acceptance.

### DEV-214 — Material UV inspector can grow wider than its viewport and clip numeric input

**Status:** fixed before UV publication; actual window capture and input-bounds assertion passed · **Affected:** EditorMaterialWorkspace material/UV properties scroll pane

UV transform explanatory label expanded the inspector preferred width, showing a horizontal scrollbar and clipping the angle spinner at the right side.

Cause / uncertainty: Scroll pane used raw preferred width of the settings/tab content rather than tracking the available inspector width.

Fix / next action: Use a Scrollable properties container that tracks viewport width; wrap explanatory text and preserve a bounded minimum inspector width. Label shared native Apply as Apply edit for both materials and UV operations.

Evidence and verification: AssetImportUvGuiTest captures the actual independent material window and asserts the UV input bounds fit inside it, while proving current native snapshot/material/selection and runtime UVs.

Older-map obligation: This changes only the independent material workspace composition. Main level and topology tool layouts remain unchanged; it does not implement painting/unwrap.

### DEV-215 — Runtime GLB decoder ignores original node matrix and changes source pivots

**Status:** fixed-scoped; exact prior game negative controls and current actual runtime/human tests pass locally; exact-source CI pending · **Affected:** GlbProps actual runtime decode and source-backed native GLB authoring

The exact previous18001e8a game decoder places the source fixture first vertex at (3,0,2), although its actual matrix plus parent hierarchy places it at (3,0,1).

Cause / uncertainty: Runtime scene traversal applied TRS but silently omitted column-major node.matrix.

Fix / next action: Use the shared bounded runtime reader for actual game and human editor, including original matrix/TRS hierarchy and inverse-transpose normals. Preserve all original transforms and compile edits back into source local coordinates.

Evidence and verification: GlbSourceAuthorityTest matrix fails against exact18001e8a GlbProps and passes against current game. Fixture has parent rotation/translation, a nonuniform matrix child and a shared mesh instance. Bundled1088-source audit finds no active nonidentity node.matrix; source audit is not placed-map acceptance. See evidence/source-glb-2026-11-02/.

Older-map obligation: Do not rewrite source art or infer complete old-map visual acceptance. Retain asset-specific origin rules; inspect actual changed sources individually. Full source-backed GLB rigs/additional attributes and G2-G12 remain open.

### DEV-216 — Generic external GLB textures use inverted legacy PNG origin

**Status:** fixed-scoped; exact prior game negative controls and current actual runtime/human tests pass locally; exact-source CI pending · **Affected:** GlbProps actual runtime decode and source-backed native GLB authoring

The exact previous18001e8a game decoder samples blue ff0000ff at source (.25,.25), where the original external glTF PNG is red ffff0000.

Cause / uncertainty: Legacy bottom-left default was applied to every unnamed/new imported GLB, including standard external glTF images.

Fix / next action: Use source top-left origin for generic external imports unless explicitly declared otherwise; retain exact legacy editor generator and reviewed named-source rules. Keep original image and UV bytes.

Evidence and verification: GlbSourceAuthorityTest uv reproduces old actual runtime failure and current pass. Human source-backed publication checks RGBA8000ff00 on the original asymmetric PNG. Named purchased models, Blender UV/CRT and Hollow Creek BLEND tests pass locally. See evidence/source-glb-2026-11-02/.

Older-map obligation: Do not rewrite source art or infer complete old-map visual acceptance. Retain asset-specific origin rules; inspect actual changed sources individually. Full source-backed GLB rigs/additional attributes and G2-G12 remain open.

### DEV-217 — Packaged full-round test compilation omits shared portable GLB fixture sources

**Status:** fixed-scoped; corrected full41547165572 build and actual TCP stage passed; raw307/310 has separate DEV211/221 failures · **Affected:** build.cmd and build.sh test source lists

Source57337f4f full41546195876/job125622271926 fails before TCP acceptance: EditorGlbAuthoringTest is absent while compiling AssetImportGlbGuiTest and GlbSourceAuthorityTest. Standalone41546137572 and native41546191383 pass because their explicit fixture dependencies are present.

Cause / uncertainty: The game build compiles only top-level host test sources, while new human/runtime tests reuse portable test fixtures in the editor subpackage.

Fix / next action: Include exactly the shared GLB and PNG fixture source files in both standard host build scripts; retain independent editor JAR compilation and do not move test fixtures into production code.

Evidence and verification: Exact failed remote compiler diagnostics retained in source-glb evidence. Clean host test compilation must include all top-level host/hbnet sources plus the two portable fixture dependencies; full corrected acceptance remains distinct from native/standalone passes.

Older-map obligation: Build integration only; no source art/goldens changed. Existing DEV211 advisory art assertions remain open, and failed pre-TCP run does not prove gameplay.

### DEV-218 — Camera picker generalization drops existing enum-typed binary API signatures

**Status:** fixed before perspective publication; actual prior-compiled client fails intermediate and passes final independent JAR · **Affected:** EditorMeshPicking public pick(State,Projection,double,double) and prepare(State,Projection)

A client compiled against the published da2b21da picker throws NoSuchMethodError when the intermediate camera generalization replaces enum parameter signatures with Projector parameters.

Cause / uncertainty: Source calls still compile because Projection implements Projector, but JVM method descriptors include the original enum parameter type.

Fix / next action: Retain original typed overloads and delegate to the new Projector implementation; keep the display-camera requirement for perspective. Add signature guards and run the unchanged prior-compiled client against the real final JAR.

Evidence and verification: perspective-workspace evidence contains real intermediate negative and final positive outputs, the same compiled consumer and source. Consumer was compiled against exact da2b21da picker Git blob. Final independent Java17 JAR and maximum old/new picking tests pass under256MiB.

Older-map obligation: Unpublished intermediate integration defect, corrected before publication. No scene or source asset changes. Portable public signatures remain stable; this does not prove all other G1/G2 APIs or gate completion.

### DEV-219 — Unanchored surface mode unboxes a null native face anchor

**Status:** fixed before surface publication; actual human unanchored-mode refusal and complete workflow pass · **Affected:** AssetImportDialog activeSurfaceFace/normal mode and gizmo painting

Selecting surface-normal mode before a face has been picked throws NullPointerException in controls and painting.

Cause / uncertainty: A conditional expression combined nullable Long normalAnchor with primitive selected face ID, forcing null unboxing.

Fix / next action: Keep the selected face ID explicitly boxed; missing anchor disables transform preview and produces no fake normal gizmo. Require an actual native face before deriving basis.

Evidence and verification: surface-unanchored-negative.txt retains actual intermediate UI exception. AssetImportSurfaceGuiTest selects the visible Transform card and normal mode before picking, verifies disabled preview/empty handles, then completes real surface edits and game publication.

Older-map obligation: Unpublished UI integration defect, corrected before publication. No source assets or other module layouts changed.

### DEV-220 — Nearby projected gizmo handles can capture a precise click on the wrong axis

**Status:** fixed-scoped; exact published d620857e world-space negative control and corrected world/surface GUI checks pass · **Affected:** AssetImportDialog beginAxisDrag; world and surface-normal projected handles

An exact click on later axis2 selects earlier axis1 in published d620857e UI when projected handle hit regions overlap. Intermediate surface mode also reproduces axis2 incorrectly captured as axis0.

Cause / uncertainty: Hit testing chose the first handle within8 pixels rather than the nearest actual handle.

Fix / next action: Choose minimum distance within the existing8-pixel hit radius, keeping stable order for exact ties. Preserve native snapshot/epoch validation, cancellation and shared history.

Evidence and verification: gizmo-d620-negative.txt comes from exact Git d620857e AssetImportDialog compiled separately and run with the same real world-axis control. gizmo-current-positive.txt passes. Surface test finds nearby actual projected handles through real orbit settings, clicks the exact later handle and proves cancel changes no native geometry.

Older-map obligation: Editor input correction only; no map or source geometry is migrated. Identical projected handles remain deterministically tied and can be separated by orbiting the actual camera.

### DEV-221 — Equipment assignment regression still reflects into the removed private GLB JSON reader

**Status:** fixed-scoped; exact5bdd full41551592599 passes equipment assignments; raw308/310 retains only DEV211 art assertions · **Affected:** EquipmentAssetAssignmentsTest after shared GLB decoder extraction

Corrected full41547165572/job125625469653 is green advisory but raw307/310: alongside two DEV211 art assertions, equipment test throws ClassNotFoundException for GlbProps$Json.

Cause / uncertainty: The test fixture decoder was coupled through reflection to the old private parser; production JSON parsing moved to the shared EditorGlbJson. Incremental local output could retain obsolete nested class files, so clean source verification is required.

Fix / next action: Use the actual shared production reader for fixture JSON; keep all original manifest/runtime mapping comparisons. Do not restore a second parser or weaken assignment/art assertions. Compile in a fresh main output directory and run without stale classes.

Evidence and verification: equipment-da2b21da.txt records exact remote failure. equipment-clean-negative.txt reproduces in fresh main classes; equipment-clean-positive.txt passes43 families/129 stage aliases/53 states/162 unique resources with corrected test. No goldens changed.

Older-map obligation: Test integration repair only. Actual packaged TCP stage passed in41547165572, but raw full regression remains unaccepted until corrected-source rerun. DEV211 source-art assertions remain open.

### DEV-222 — Per-part framed GLB grouping changes canonical order of interleaved native faces

**Status:** fixed before local-frame publication; actual game interleaved-order control and source/native tests pass · **Affected:** EditorMaterialGlbWriter, source-backed writer, shared GLB reader and actual GlbProps adapter

Intermediate framed export of A/B/A faces produces actual game order A/A/B: second first-corner X7.7274065 instead of native3.8637033 after rotation.

Cause / uncertainty: Grouping primitives into actual per-part nodes changed traversal order without carrying canonical native face sequence.

Fix / next action: Store bounded per-node BECK_native_order descriptors, validate complete permutations, and consume the same ordered faces in native import and the actual game decoder. Source provenance/validation maps now use actual face identities, including shared instances; readback explicitly verifies order.

Evidence and verification: order-negative.txt records real game mismatch. AssetImportLocalFrameGuiTest now verifies actual interleaved game corners. Source authoring and local tests verify hierarchy, material, original bytes and exact native order without flattening nodes.

Older-map obligation: Unpublished integration defect fixed before publication. Existing ordinary assets without order descriptors keep original decoder traversal. No artwork or goldens changed.

### DEV-223 — GLB readback rejects valid sparse native face IDs

**Status:** fixed-scoped; exact published5bdd writer negative and corrected public encoder/readback control passed · **Affected:** EditorGlbWriter generic native face identity mapping

A valid single triangle retaining native face7 fails GLB readback with lost/duplicated native face identity, despite unchanged geometry.

Cause / uncertainty: Generic output ordinal0 was looked up as native ID0 rather than mapped through the expected native face sequence; sparse IDs after repairs need not start at zero.

Fix / next action: Map generic ordinals to expected stable native IDs, retain explicit tags on framed/source derivatives and validate canonical face order independently. Invalid readback returns checked IOException.

Evidence and verification: sparse-negative-5bdd.txt uses exact5bdd5491 writer Git blob. Same SparseGlbControl passes corrected source; portable local test includes face7 without renumbering it.

Older-map obligation: Editor export correction only. Existing saved native IDs remain stable; no mass reimport or renumbering. Confirm new exact-source CI separately.

### DEV-224 — Signed-zero local frame values fail cross-format equality after JSON decoding

**Status:** fixed before local-frame publication; actual negative/positive GLB readback controls and native guard passed · **Affected:** New EditorMeshFrame component canonical numeric representation

Valid native frame entries containing -0.0 fail exact GLB frame readback because decimal JSON decoding yields positive zero.

Cause / uncertainty: The new frame record distinguished signed zero while its numeric JSON representation did not preserve that distinction.

Fix / next action: Canonicalize only zero-valued frame coefficients at construction. Preserve finite nonzero coefficients, original source bytes, native vertices and raw corner attributes.

Evidence and verification: zero-negative.txt/zero-positive.txt run the same FrameZeroControl against actual GLB writer/reader. EditorLocalFrameTest guards canonical identity and the final native/OBJ/GLB roundtrip.

Older-map obligation: New unpublished component normalization, not an artwork or vertex-normal rewrite. Older native1-4 data stays compatible; native5 explicitly carries the new frame fields.

### DEV-225 — Concurrent main installer and full acceptance fail host campaign persistence assertion

**Status:** fixed in integrated editor branch; packaged desktop/TCP and complete native suite pass locally; exact remote acceptance pending · **Affected:** main ce146a69 packaged startup/multiplayer acceptance

Both workflows stop at AssertionError: Host campaign result saved.

Cause / uncertainty: Desktop test enters physical 3D then calls legacy EXTRACT; main ce146a69 rejects this by design. Isolated current-main LanHost/LanCase plus physical vanDoor request passes actual campaign desktop persistence/navigation.

Fix / next action: Merged main ce146a69 without weakening extraction enforcement; desktop test uses physical vanDoor. Retained both recovery histories and all editor changes.

Evidence and verification: main ce146a69 full41566564774 and installer41566564773; raw failed-step logs retained in native-bevel-2026-11-02. Positive current-main host class test and exact proposed patch retained beside negative remote logs. Merged production JAR desktop, all eight physical/room-mode TCP scenarios and native suite logs: campaign-merge-2026-11-02.

Older-map obligation: Campaign/host behavior requires actual packaged regression; no map or release acceptance inferred.

### DEV-235 — Legacy Castle tree belt intersects new training annex

**Status:** fixed-scoped; actual packaged render and physical annex regression pass · **Affected:** Castle west training annex / CastleSurroundings

First runtime capture shows an original exterior tree occupying the new interior corridor.

Cause / uncertainty: Legacy western tree coordinates predate the signed west extension.

Fix / next action: Move the existing western belt beyond the annex west wall; retain models and population. Offset new bay lamps away from actual roof beams; version native annex-light migration so deleted lights stay deleted.

Evidence and verification: rebuild/captures/castle-construction/rejected-training-annex-tree.png and training-annex-runtime.png; verification/castle-annex-2026-11-03 packaged tests, actual 81366-position native-spawn flood, closed/open body and optical leaf, fixture support/clearance and switch-off energy.

Older-map obligation: Castle-specific environment conflict. Other sites require individual building/tree/fixture audits; no complete Castle or roster acceptance inferred.

### DEV-236 — Castle cellar flight cannot join the ground landing across the player radius

**Status:** fixed-scoped; final packaged104609-position native spawn flood and native/filled-floor negative controls pass · **Affected:** New Castle cellar stair / native floor support, slab depth and shared editable opening

Initial packaged flood reaches the annex but no cellar room. Successive stair probes show the upper landing and first descending tread individually supported but their radius-spanning transition blocked. A subsequent explicit ascent sweep also exposes an incorrect top-landing handoff atZ24.

Cause / uncertainty: Legacy ground rooms and new native negative-height tread support were queried separately. The original path also assumes uniform slab depth, no persisted architectural void and elevation-free ground furnishings/interactions. Outside the flight, the lower resolver selected the cellar floor even when one riser below the actual ground landing.

Fix / next action: Combine adjacent real landing/tread support without permitting height teleportation; persist a native VOID edited through the same human inspector/history and actual Editor Agent put/remove-surface operations. Render/occlude actual native floor depth, including filled stair masses. Keep below-ground furniture vertical overlap, headroom, circuit, footfall and ground workstation access elevation-aware. Transfer to the real upper landing only within a physical step height; explicitly sweep both descent and ascent rather than inferring return travel from reachability.

Evidence and verification: build/castle-cellar-rejected-landing.log and first-route-probe.log retained under rebuild/verification/castle-cellar-2026-11-03. Corrected packaged CastleCellarConstructionTest reaches104609 physical positions from native spawn and all4 cellar/4 annex rooms; actual filled editor world and removed architectural VOID block the same passage. Native service undo/redo restores/removes physical and optical access. Actual runtime stair/hall captures retained. Rejected upper-return log is preserved; corrected explicit 25mm descending and ascending sweeps pass.

Older-map obligation: New Castle construction defect repaired before its checkpoint. Other multi-level locations retain separate route/headroom/furniture/terrain audits; no complete Castle or roster acceptance inferred.

### DEV-237 — New Castle stair guard blocks the eastern warehouse browsing aisle

**Status:** fixed-scoped; real browsing positions and transformed rack/exhibit mesh containment pass · **Affected:** Castle ground warehouse / eastern rack bank and north rack

Packaged CastleRebuildTest rejects the LASER_GRID browsing position at40.5/18.74 after constructing the cellar guard.

Cause / uncertainty: Legacy rack positions share space with the planned floor opening. The earlier HQ test also hardcodes equipment centre X<42 instead of checking actual wall faces and transformed mesh extents.

Fix / next action: Retain all15 racks and every portable family/tier. Move eastern bank toX42.45, inside the actual masonry face with a continuous facing aisle; shift north rack toX37.6. Replace arbitrary centre margin with every actual transformed rack/exhibit vertex bounded by real floor, wall and ceiling faces. Keep the existing all-exhibit browsing/body checks.

Evidence and verification: rebuild/verification/castle-cellar-2026-11-03: rejected warehouse aisle and legacy centre-bound logs; final CastleRebuildTest and CastleHeadquartersRevisionTest. Actual final stair and lower hall runtime captures are retained in captures/castle-construction.

Older-map obligation: Castle construction regression repaired before publication. Other site furniture layouts and saved custom edits need individual mesh, circulation and stair/guard audits; no blanket acceptance.

### DEV-238 — New enclosed Castle cellar flight has no direct fixed lighting

**Status:** fixed-scoped; two supported native stair sconces, actual illumination checks and viewed runtime capture · **Affected:** Castle cellar stair fit-out

Initial structural runtime view leaves the lower flight dark despite the side-bypass room ceiling fitting.

Cause / uncertainty: The full-height stone stair cheeks correctly occlude the side-room lamp; the enclosed flight needs its own mounted fittings.

Fix / next action: Add two source-model wall lamps fitted to the actual native cheeks/guard, share the cellar circuit and retain ceiling lamps for the four lower rooms. Wall attachment checks consume the same native masonry and fitted pose as rendering.

Evidence and verification: pre-lighting-cellar-stair-runtime.png and final cellar-stair-runtime.png; CastleCellarConstructionTest verifies native attachment, actual stair/room light energy, circuit separation and unobstructed continuous body sweeps.

Older-map obligation: New Castle structural-to-fit-out issue. Other enclosed stairs need individual support/occlusion/brightness and body-clearance reviews; no inferred older-map repair.

### DEV-239 — Bounded Castle stock layout blocks the original store light switch approach

**Status:** fixed-scoped; actual ground routes, all stock browsing stands and all room switches pass · **Affected:** Castle stock room / existing switch at28.73,26

First ground construction test cannot stand at the legacy stock switch after fitting all15 racks into the bounded stock room.

Cause / uncertainty: The relocated western rack bank projects into the former approach by40mm.

Fix / next action: Move the same switch to25.9 on the real shoulder beside the entrance; preserve15 source-sized racks and all family/tier stock.

Evidence and verification: development/levels/rebuild/verification/castle-ground-2026-11-03/castle-ground-rejected-stock-switch.log; final native route/switch/stock checks.

Older-map obligation: Audit every location fixture approach after new walls or relocated stock; no general older-map repair inferred.

### DEV-240 — Ground-room authoring helper loses the existing native annex ceiling query

**Status:** fixed-scoped; packaged annex fixture, brightness, physical routes and exact save/reopen pass · **Affected:** CastleRooms.mountHeight / existing west annex

Targeted CastleTrainingAnnexTest throws No ceiling at -8,27 after separating default fixture authoring from live singleton initialization.

Cause / uncertainty: The authoring-only ceiling lookup considered the new core room table but omitted the generated annex spaces.

Fix / next action: Include generated annex ceiling dimensions in authoring queries; retain actual native slab attachment as the independent runtime support check.

Evidence and verification: development/levels/rebuild/verification/castle-ground-2026-11-03/castle-ground-rejected-annex-ceiling.log and final CastleTrainingAnnexTest. The retained log is an explicitly labeled current-source controlled omission; the original intermediate JAR was not retained.

Older-map obligation: Castle helper regression repaired before ground publication; custom/legacy native geometry still needs actual support, not a guessed mount plane.

### DEV-241 — Full regression manifest aborts on qualified new test names after running prior tests

**Status:** local repair verified; fresh exact-source full CI still required · **Affected:** regression-tests.txt / actual BeckenburgRegressionSuite runner

Prior b5e full41726781593 is green at workflow level but raw job aborts on org.beckenburg.CastleWorldTerrainTest without a final regression summary. Terrain check is not executed.

Cause / uncertainty: The real runner accepts short class names only; new terrain/cellar entries used fully qualified names. Full preflight also exposes an existing duplicate HorrorAudioRoutingTest.

Fix / next action: Use canonical short names, preserve each test once, validate every name/compiled entrypoint before launching children, require all Castle construction tests, and expose --validate. Actual subprocess negative controls reject qualified tail, duplicate and missing class before any test launches.

Evidence and verification: development/levels/rebuild/verification/castle-ground-2026-11-03/prior-full-b5e.log; final321-entry --validate and BeckenburgRegressionManifestTest logs. Prior raw316 test passes,2 art failures, then invalid-entry abort; no full acceptance inferred.

Older-map obligation: Keep GhostSpriteAtlasIntegrationTest and TwinBodyVisualTest artwork/goldens unchanged. No omission, duplicate inflation or advisory-green badge substitutes for complete raw-suite execution.

### DEV-242 — GPU source compile job exhausts its limit before compilation during canonical checkout

**Status:** fixed-scoped; fresh actual Linux compile, Windows build and Mesa smoke succeed · **Affected:** Existing GPU workflow Linux compile-gpu-java17 job

GPU run41731832476/job126250898962 is cancelled while git fetch is running; source compilation never starts. Windows build and actual Mesa smoke succeed at91e8f47b.

Cause / uncertainty: Canonical repository checkout consumes the full10-minute compile-job deadline.

Fix / next action: Allow20 bounded minutes for the same full checkout, source compile and unchanged contract checks. Do not alter runtime performance thresholds or omit assets/tests.

Evidence and verification: development/levels/rebuild/verification/castle-ground-2026-11-03/prior-gpu-compile-91e.log and prior-gpu-91e.json; fresh GPU run required. GPU41770098733 at30354e24: windows-build, compile-gpu-java17 and opengl-mesa-smoke all success. Physical hardware acceptance remains separate.

Older-map obligation: CI preparation correction only; physical GPU/platform acceptance remains independent and cannot be inferred from source compilation or software Mesa.

### DEV-243 — New ground room owners and controls must not appear in older native geometry

**Status:** fixed-scoped; native-structure-removal negative controls pass · **Affected:** Castle legacy/custom saved scenes / ground child-room metadata, switches and portal availability

Source audit of the first ground refit finds that unconditional child room owners, new static switches and door sockets can describe rooms absent from an older native document.

Cause / uncertainty: Room/circuit tables and socket metadata are compiled separately from the loaded native surface snapshot.

Fix / next action: Tag the actual new native partition surfaces; activate child owners/new controls and ground portal rendering/body/optical behavior only with that native programme. Retain original broad room owners for saved geometry without those surfaces and retain every published lower doorway ID.

Evidence and verification: development/levels/rebuild/verification/castle-ground-2026-11-03/CastleGroundConstructionTest checks removed programme surfaces, legacy owners, unavailable new switches and a forced closed ground leaf that must not create phantom body/optical blockage.

Older-map obligation: Custom native documents and their objects are preserved. This is a source/negative-control repair, not a claim that every older edited Castle scene has been visually audited.

### DEV-244 — Wall-sconce atlas inverted across both power states

**Status:** fixed-scoped; actual source-pixel and three-angle castle comparisons pass · **Affected:** lamp_3_on / lamp_3_off at every runtime site using these shared models

Gray casing paints the fluorescent tubes while yellow/white strips wrap the housing edges.

Cause / uncertainty: Legacy bottom-left V sampling reverses the original glTF top-left embedded PNG atlas.

Fix / next action: Use original glTF image origin for exactly these two inspected model identities; preserve source GLB geometry, UV coordinates, image pixels, pivots and mounts.

Evidence and verification: development/quality/evidence/wall-lamp-uv-2026-11-03; WallLampSourceUvTest fails with original JAR and passes all64 actual triangle-centroid samples after correction. Three-angle actual Castle on/off captures retained in development/captures/wall-lamp-runtime.

Older-map obligation: Shared model repair applies to old sites using these identities. Other models retain their existing origin; no claim that unrelated legacy assets are UV-audited.

### DEV-245 — Wall-light backing plate is oversized and untextured

**Status:** fixed-scoped; actual textured renderer faces, source-geometry contact and captures verified · **Affected:** Castle wall-light mounting plates, including edited scenes and lower/upper fixtures

A large flat brown rectangle extends above and below the textured light housing.

Cause / uncertainty: Renderer adds a hardcoded untextured30x40x5.2cm primitive independent of fixture scale.

Fix / next action: Replace with a compact scaled strap using the original lamp metal atlas region on all six faces; seat into masonry and meet actual source rear lip.

Evidence and verification: development/quality/evidence/wall-lamp-bracket-2026-11-03; WallLampMountPlateTest verifies actual renderer materials/UVs, texture variation, source-triangle and masonry contact; actual five-angle on/off captures under development/captures/wall-lamp-runtime/textured-bracket.

Older-map obligation: Shared Castle renderer correction applies to legacy edited scenes; unrelated map mounting assemblies retain individual audit obligations.

### DEV-246 — Regression preflight cannot load test classes with the real CI command

**Status:** fixed-scoped; actual full324-entry run now completes with322 unique passes and2 pre-existing art failures · **Affected:** Full regression manifest runner / workflow41738088189 at50829e61

Overall workflow is green, but raw advisory step aborts before executing any regression: ClassNotFoundException BeckenburgDiagnosticsTest.

Cause / uncertainty: Preflight uses its parent classloader, while the real workflow starts with build/classes only; child tests live in build/test.

Fix / next action: Preflight uses an additional closeable loader for the actual build/test root. Add subprocess coverage for the precise main-only workflow classpath; retain duplicate, qualified-name and missing-class negatives.

Evidence and verification: development/levels/rebuild/verification/castle-upper-2026-11-03/prior-ground-full-50829.log; BeckenburgRegressionManifestTest.log verifies324 names without launching tests. Actual subsequent full41770104678 at30354e24 reaches summary322/2/0 of324; raw log under castle-museum-2026-11-03.

Older-map obligation: CI verification repair only. Prior overall success does not certify skipped regression bodies. Fresh exact-source full run must reach its actual summary.

### DEV-247 — Old geometry probes assume every Castle aperture and fitting is on the ground storey

**Status:** corrected-probes; actual fixture vertices and all authored casement coordinates pass · **Affected:** CastleGroundConstructionTest ceiling clearance and CastlePortalSlotTest casement sampling

New upper walls are falsely intersected by standing-body volume around a ground ceiling lamp; added upper casements are probed at ground window coordinates.

Cause / uncertainty: Geometry checks substitute a1.55m standing-body volume for imported lamp vertices, and fixed x1/y1.85 for all window slots.

Fix / next action: Check actual transformed ceiling-lamp vertices against native masonry; sample each casement at its own wall plane, sill/head and storey while retaining solid sill/lintel and no-body-passage negatives.

Evidence and verification: development/levels/rebuild/verification/castle-upper-2026-11-03/rejected-legacy-* logs reproduce both earlier probes against actual new geometry; final named checks pass.

Older-map obligation: These are inspection-probe corrections, not removal of clearance/optical requirements. Original ground windows and fittings remain checked.

### DEV-248 — South rampart capture contains flat polygon patches in the night sky

**Status:** fixed-scoped clear-night painter; six actual matched Castle exterior views verified · **Affected:** Castle southern upper exterior view

Actual upper-south-walk-runtime capture shows isolated flat dark polygon patches above the distant horizon.

Cause / uncertainty: WeatherSky always drew at least4 hexagonal cloud polygons, including at actual cloudCover0; minimum alpha35 made hard floating patches in the clear night sky.

Fix / next action: Draw no bands at zero cloud cover and fade in very thin coverage. Retain existing storm/overcast band density/opacity, world-fixed heading/pitch, sky-only composition and actual world depths. Fresh full-source renderer/platform CI remains required.

Evidence and verification: verification/castle-closeout-2026-11-06: original zero-cover test fails, corrected packaged weather checks pass; six exact native before/after PNG pairs,1220 sky pixels corrected, byte-identical world depths and zero changed solid-interior pixels. Earlier upper-south-walk-runtime.png retained.

Older-map obligation: Shared zero-cover painter corrected, but each older map/sky profile and all other weather states still require individual visual acceptance. Do not infer a global map fix from this Castle review.

### DEV-249 — Owner rejects primitive Castle museum studies, especially disconnected armour slabs

**Status:** rejected; reference-led armour reconstruction and whole-kit reference review in progress · **Affected:** Candidate Castle museum kit, not integrated into runtime

Armour reads as generic primitive shapes; three tasset slabs float in front of the torso. Other museum studies lack adequate reference examination and finish.

Cause / uncertainty: First asset construction pass substituted generic primitive assemblies and procedural surface noise for measured object anatomy, suspended articulation and design-specific reference work.

Fix / next action: Quarantine initial studies outside runtime delivery. Examine primary museum references and actual photographs; rebuild shaped articulated shells, visible fastenings and proper load paths; inspect actual exported geometry in clay before surfacing and runtime placement. Review every other kit member independently.

Evidence and verification: development/assets/castle-museum/reference-brief.md; references/sources.json; previews/rejected-primitive-armour and rejected-wide-armour; castle_armour_geometry.py and reimported clay studies.

Older-map obligation: Existing legacy decorations are not assumed correct. This rejection blocks promotion of the new museum kit; older-map source/art audits remain separate.

### DEV-250 — Dense museum armour uses the wrong triangle budget and disconnected-looking foot strips

**Status:** candidate corrected; exported1036-triangle geometry/contact checks pass; full art/runtime acceptance remains open · **Affected:** Castle museum armour study, quarantined outside runtime

Reference rebuild grows to40,796 triangles using geometric rivets/trim; sabaton strips look detached. Owner requires car/character-scale budgets, sharp lower-count shapes, texture-authored detail and no floating pieces.

Cause / uncertainty: Fine details are over-modelled and foot anatomy is approximated by separate strips rather than a coherent enclosing shape.

Fix / next action: Reauthor low-count silhouette and component-specific texture atlas; close toe/side/heel geometry; anchor actual suspension to faceted plate surfaces; audit every export component through actual surface contact graph and reject a real detached-helmet negative.

Evidence and verification: development/assets/castle-museum/armour-review.json, armour-contact-audit-export.json, armour-contact-audit-negative-detached-helmet.json and reviewed close feet captures; rejected dense/foot studies retained.

Older-map obligation: Apply owner budget/attachment direction to future model work and audit existing assets individually. Other museum kit members remain unaccepted and outside runtime.

### DEV-251 — Asset/floor diagnostics use an over-broad anatomical region or native-only support domain

**Status:** corrected diagnostic domains; actual control reproduces misleading results; no renderer change · **Affected:** Castle museum development probes, not a production floor/rasterizer defect

A pixel rectangle below the torso counts the intentional gap between thigh guards as uncovered chest; native-only floor query misses the shared authored archive deck. A tiny body probe also falls inside deliberate step-clearance tolerance.

Cause / uncertainty: Inspection probes are used beyond their spatial/authoring API domain.

Fix / next action: Use actual closed-torso image region and depth ownership, shared structural slabs over the full footprint, physical/optical slab probes outside step tolerance. Remove the ineffective epsilon rasterizer experiment completely.

Evidence and verification: development/levels/rebuild/verification/castle-museum-2026-11-03/ProbeDomainControl.java and museum-probe-control.log:48 intentional-gap pixels,0 uncovered true torso; native-only false/shared floor true. Actual native display checks pass. Tapestry room query uses floor0 rather than hangingY=.55; rejected library camera floor=true/wall=true/guard=false, so move camera to a clear supported position without altering structure.

Older-map obligation: Keep existing floor/body/ray requirements. This correction does not certify unaudited older assets or justify loosening production geometry checks.

### DEV-252 — Candidate museum textures and fine-detail geometry do not read correctly in the real diffuse renderer

**Status:** fixed for scoped armour/caption/barrier pilot; remaining kit requires independent review · **Affected:** Castle museum candidate armour atlas, label geometry and brass post finish

Initial source texture uses dark speckling and a broad unintended back seam; first plaque uses9128 triangles of text geometry; post finish reads flat under diffuse-only lighting.

Cause / uncertainty: Studio lighting and polygon-based details are used without sufficient actual software-renderer review.

Fix / next action: Separate chest/back atlas regions, remove isolated high-contrast pits, use original readable label text in bitmap texture on86 triangles, reduce barrier1344->264 and paint coherent circumferential metal/velvet shading. Preserve source and actual Castle rejected/final captures.

Evidence and verification: development/assets/castle-museum/previews/rejected-software-texture, rejected-flat-brass-runtime and current source/runtime captures; armour1036/caption86/barrier264 independent contact reports and CastleMuseumConstructionTest.

Older-map obligation: Existing scenery remains individually unaudited. These three reviewed models do not accept the other rejected museum studies or unrelated legacy UV/material work.

### DEV-253 — Merged full-suite lighting parity times out despite isolated all-site correctness passing

**Status:** resolved-scoped; exact checkpoint CI parity passes and current-main full suite335/0 · **Affected:** PreparedLightBatchParityTest on merged Castle checkpoint6fcb9791, full workflow41775037193

Actual325-entry suite records322 passes and3 failures: two prior sprite-art defects plus PreparedLightBatchParityTest exceeding90seconds. Workflow green is advisory and not complete regression acceptance.

Cause / uncertainty: Unknown runner/workload sensitivity; isolated current packaged all-site parity passes locally. No functional float mismatch has been observed. Timing evidence must distinguish Castle cost from the other map types.

Fix / next action: Keep90-second deadline and all float/geometry/immutability coverage unchanged. Exact0f718640 full41804015121 completes325/2 of327 with lighting parity passing; later current-main74612d1d full42135141033 completes335/0. Prior timeout retained as historical evidence.

Evidence and verification: development/levels/rebuild/verification/castle-museum-2026-11-03/merged-full-6fcb.log, merged-ci-summary.json and merged-parity-isolated.log See development/levels/rebuild/verification/castle-textiles-2026-11-06/prior-full-0f718640.log and build-main-full-74612.log. This does not certify new branch CI or hardware performance.

Older-map obligation: Test covers Castle and all authored site types. New map dressing does not certify older-map performance or waive sprite-art failures.

### DEV-256 — Castle candidate surfaces lack credible authored materials and fireplace obstructs light-switch approach

**Status:** fixed-scoped source/export/native pilot; final32 checks pass; full Castle/platform acceptance open · **Affected:** Castle tester beds, archive armour and new cold common-room fireplace

Owner rejects flat/striped procedural wood, cloth, steel and stone. First fireplace location obstructs the common-room east-wall light-switch standing point.

Cause / uncertainty: Simple procedural atlas regions do not supply convincing authored detail; initial fireplace reservation checks door routes but misses the nearby existing switch approach.

Fix / next action: Use built-in image generation for each actual atlas, including a genuinely transparent curtain PNG; pack512px textures/256px curtains, preserve original mesh/UV bytes and actual support. Shift the26-face fireplace fromx8.4 tox7.6, bearing flush to actual42cm north masonry; retain all body/ray/door/switch/flood checks.

Evidence and verification: development/assets/castle-museum/imagegen-geometry-invariants.json, source-inspection/imagegen, generated masters/prompts and rejected surface studies; CastleTesterBedTest/CastleFireplaceTest and actual editor-service captures. Final evidence: development/levels/rebuild/verification/castle-imagegen-2026-11-03/verification.json.

Older-map obligation: No automatic visual acceptance for old maps or remaining museum studies. Apply owner low-count and texture-detail direction individually, keeping physical support and fixture approaches.

### DEV-257 — Tester bed stretches decorative atlas onto thin edges and overlaps canopy beam end-caps

**Status:** fixed-scoped source/export/native pilot; final32 checks pass; full Castle/platform acceptance open · **Affected:** New Castle tester-bed source, exported GLB and six native lodging placements

Owner identifies poor UV mapping and dark rectangular patches at canopy/post corners.

Cause / uncertainty: Per-face normalised UVs squeeze whole panels onto thin bedding/headboard edges. Four full cornice boxes duplicate coplanar end areas, producing black corner self-shadowing.

Fix / next action: Use consistent physical wood grain scale, reserve carved atlas for broad inward headboard, fold narrow bedding edge UVs; replace four intersecting beam boxes with one16-face mitered ring. Bed drops844->828tri with unchanged outer bounds. Check actual exported end-face area equals1.42x.18m on both ends; preserve rejected corner/UV views.

Evidence and verification: development/assets/castle-museum/previews/rejected-bed-uv and rejected-bed-canopy-joints; final four-poster-rebuild studio/software views; CastleTesterBedTest and source/export contact reports. Final evidence: development/levels/rebuild/verification/castle-imagegen-2026-11-03/verification.json.

Older-map obligation: These corrected UVs/joints apply to the new bed only. Audit other older furniture and remaining museum studies individually.

### DEV-277 — Repacking already embedded imported GLB images clears the editable source texture payload

**Status:** fixed-scoped in new reused-frame/rug authoring builder; fresh packed-source reopen passes · **Affected:** Blender5 heritage source creation and studio inspection; runtime GLB texture bytes were valid

Independent source renders show magenta frame and rug even though production GLB/PNG decoding succeeds.

Cause / uncertainty: Calling Image.pack() a second time on an already packed imported image clears its packed_file while filepath remains empty in this workflow.

Fix / next action: Pack only file images that do not already have packed_file; assert every material-connected image remains packed, then independently reopen all3 delivered scenes and require loaded nonempty packed images. Preserve original mesh/UV/index bytes in the texture-container patcher.

Evidence and verification: development/assets/castle-museum/previews/rejected-reused-source-pack, packed-heritage-sources.json; verification/castle-textiles-2026-11-06/debug-reused-images.log reproduces True->False packed state before fix; final source/studio/native views are separate.

Older-map obligation: Do not assume older Blender sources are packed or visually accepted. This correction is scoped to the new authoring path; original reusable source GLBs remain unchanged.

### DEV-278 — Heraldic mounting spine projects beyond the shield silhouette

**Status:** fixed-scoped source/export/native pilot; final checks pending · **Affected:** New Castle heraldic candidate only

First front/quarter exported views expose a broad steel board above/below the shield.

Cause / uncertainty: 1.14m spine exceeded the shield outline; physical support alone did not establish visual fit.

Fix / next action: Refit to0.70m while keeping real wall/saddle contacts and316tri. Assert actual wall-bearing source vertices remain Y0.49..1.19. Preserve rejected exposed-spine views.

Evidence and verification: development/assets/castle-museum/previews/rejected-heraldic-exposed-spine; heraldic-rebuild, actual archive-heraldic runtime captures, CastleHeraldicDisplayTest, heraldic-independent-wall-export and actual detached-blade negative.

Older-map obligation: Audit other mounting boards/statue/heraldic studies independently; no retrospective older-map acceptance.

### DEV-279 — Reusable modern boiler has fourteen detached detail components and32 zero-area faces

**Status:** fixed-scoped Castle variant; original library retained with older-map audit obligation · **Affected:** Original level_boiler_modern library model and new Castle plant pilot

Actual triangle contact graph rejects9 grille strips,2 vertical trims and3 control buttons as unsupported. Source has32 degenerate triangles.

Cause / uncertainty: Details are offset from actual body/panel surfaces; bevelled body depth was treated as flat. Duplicate coincident source seam vertices leave32 zero-area faces.

Fix / next action: Keep original GLB untouched. Refit14 detail groups by depth-only translations to real source triangle supports plus1mm intersection. Weld duplicate vertices in variant,1452->1420 triangles, identical13.359344312758171m2 surface area. Require actual combined source/connector support, broken-feed negative, fresh source reopen and native fit tests.

Evidence and verification: development/assets/castle-services/rejected-original-boiler-contact.json, boiler-attachment-repairs.json, plant-connected-export.json, plant-broken-feed-negative.json; CastlePlantFitTest; four actual exported material views/clay and native plant captures.

Older-map obligation: Audit every older map using level_boiler_modern or its worn/other boiler variants individually; original source remains unmodified. No global fix/acceptance claim.

### DEV-280 — New plant connector approval omitted the actual selectable editor model catalog

**Status:** fixed-scoped through LevelAssets.MODELS and actual native constructor/catalog regression · **Affected:** New Castle plant boiler/connector integration before promotion

Actual CastleLevelEdits construction rejected plant-boiler-connections as Invalid editable prop despite boolean approval.

Cause / uncertainty: Adding only LevelAssets.approved did not include new IDs in GlbProps.availableModels, which the real native document validates.

Fix / next action: Use the existing approved MODELS set for both new assets, preserving original UV sampling. Require both IDs in actual editor catalog and instantiate/save/reopen/capture the real native scene. Keep original failed log.

Evidence and verification: verification/castle-plant-2026-11-06/plant-rejected-model-registration.log; final CastlePlantFitTest and native plant captures.

Older-map obligation: No allowance or validation threshold relaxed; audit other approved-only model additions individually when integrating them into native scenes.

### DEV-281 — Shared workbench tool layer floats above the actual tabletop

**Status:** fixed-scoped shared clean/worn model repair; all older placements still require individual audit · **Affected:** level_workbench and level_workbench_worn, including both Castle repair-room native instances

Actual exported triangle-contact audit finds5 unsupported tabletop groups: three sockets and their connected accessory layer.

Cause / uncertainty: Cluster minimumZ0.95 did not meet actual tabletopZ0.945; original layered placement used an assumed top plane.

Fix / next action: Translate the five-group cluster downward0.0059998164m onto actual source-triangle support with1mm intersection. Preserve original library masters, model IDs, outer bounds and688tri. Audit actual final shared exports and reject a real socket raised15cm.

Evidence and verification: development/assets/castle-training/*-rejected-source.json, bench-material-independent-review.json, level_workbench*-independent-export.json and raised-tool-negative reports; WorkbenchMaterialTest; actual native workbench captures.

Older-map obligation: Corrected resource reaches shared IDs in saved scenes, but each older map/placement/circulation remains individually unaudited; no all-map acceptance.

### DEV-282 — Generic workbench materials and face UVs make useful equipment read as toy-like stacked primitives

**Status:** fixed-scoped generated surfaces and component UVs;37 targeted checks pass; full-site polish open · **Affected:** Shared clean/worn workbench textures and all instances using those runtime model IDs

Owner rejects flat colored material layers; paper storage reads as tan blocks, vice as bright blue slabs, timber as generic stripes.

Cause / uncertainty: Generic per-material images and repeated face mapping do not describe actual timber, end grain, worn casting, steel, grips or cartons.

Fix / next action: Built-in image generation creates separate512px clean/worn component atlases. Map horizontal timber grain at physical scale, end grain on cuts, muted cast paint, bare steel, grips and corrugated cartons to their actual regions; correct shared glTF top-left sampling. Preserve repaired688tri geometry through surface pass.

Evidence and verification: development/assets/castle-training/textures/*imagegen*; rejected-generic-bench-materials; generated-bench-materials; actual WorkbenchMaterialTest editor captures and geometry/image hashes.

Older-map obligation: Shared resource/material origin updates every same-ID instance; verify each older site actual placement and lighting separately, without treating resource repair as map acceptance.

### DEV-283 — Signed training annex remains excluded from native saved-spawn validation

**Status:** fixed-scoped; all three native spawn validators share signed bounds; actual negative-X save/reopen passes · **Affected:** CastleLevelEdits.setSpawn and saved-document spawn validation for negative-X annex

setSpawn(-14,18.4,PI) rejects Invalid playable spawn although the annex exists in signed authoring bounds.

Cause / uncertainty: Legacy minimumX1 remained independently in setter, decoder and Snapshot constructor. Fixing only setter/decoder still failed actual saved snapshot construction.

Fix / next action: Use CastleWorldBounds.spawn for setter/decoder/Snapshot. Preserve courtyard default; invalid signed bound refuses without mutation. New training remains attached to actual saved desks/inlays and does not overwrite custom documents.

Evidence and verification: verification/castle-finish-2026-11-06/training-rejected-snapshot-spawn.log; CastleTrainingLessonTest valid bay spawns/native save/reopen and invalid bound controls.

Older-map obligation: Positive existing spawns must remain unchanged; this is a Castle-annex persistence obligation, not proof that other signed maps are audited.

### DEV-284 — Retained keeper study is an assembly rather than continuous carved stone

**Status:** fixed-scoped in final Castle fit-out checkpoint · **Affected:** Castle finishing candidates and their inspection tooling only

Robe, face, mantle and hands retain primitive boundaries; robe-to-plinth gap undermines a carved figure.

Cause / uncertainty: Separate primitive sculpture study was never finished or approved.

Fix / next action: Preserve original study; bridge robe/plinth in real stone, voxel-union and decimate to1080tri. Independent shipped export is one connected watertight solid on its plinth; actual15cm lift fails.

Evidence and verification: keeper-finish-review.json, keeper-independent-review.json, actual native keeper capture; original founder_statue.blend remains preserved.

Older-map obligation: No unaudited older map or model is accepted. Preserve original source assets; review reused models/placements individually.

### DEV-285 — Inherited UV layer hides new keeper surfacing

**Status:** fixed-scoped in final Castle fit-out checkpoint · **Affected:** Castle finishing candidates and their inspection tooling only

First exported limestone renders show stretched streaks rather than generated stone and face/codex regions.

Cause / uncertainty: New authored UV layer was added after an inherited remeshed source UV layer; export used the first map.

Fix / next action: Remove inherited UVs, assign exactly one authored region map and one actual material; inspect final reimported export and assert one UV layer.

Evidence and verification: previews/rejected-keeper-inherited-uv; final keeper-finish previews and independent report.

Older-map obligation: No unaudited older map or model is accepted. Preserve original source assets; review reused models/placements individually.

### DEV-286 — First native collection fittings face their backs into the room

**Status:** fixed-scoped in final Castle fit-out checkpoint · **Affected:** Castle finishing candidates and their inspection tooling only

Actual Castle view shows rear of keeper and vault cabinet doors facing the wall.

Cause / uncertainty: Blender -Y front exports to glTF +Z; native yaw0 faces south.

Fix / next action: Rotate keeper and south-wall cabinets PI; west-wall shelf -PI/2. Preserve exact source dimensions and recheck whole-footprint overlap/support.

Evidence and verification: Rejected native PNGs and final finish-keeper/finish-vault-runtime.png; CastleFinishPlacementTest.

Older-map obligation: No unaudited older map or model is accepted. Preserve original source assets; review reused models/placements individually.

### DEV-287 — Candidate inspection confuses diagnostic domains and lazy packed images

**Status:** fixed-scoped in final Castle fit-out checkpoint · **Affected:** Castle finishing candidates and their inspection tooling only

Initial checks flag supported kitchen furniture and valid packed source images; attempted cameras also occupy existing hearth/wardrobe/masonry clearance.

Cause / uncertainty: Ground floor authority includes CastleRooms.ground, not only explicit upper/cellar floor surfaces or exterior terrain. Blender packed pixels load lazily; unused old material images are not the delivered sculpture.

Fix / next action: Use actual native ground support plus real cellar opening exclusion; inspect only material-connected packed images and force pixel decoding. Move review cameras to supported clear points; preserve existing collision.

Evidence and verification: keeper-rejected-packing-check.log, keeper-pack-debug.log, FinishSupport.java, castle-finish-rejected-camera-route.log; final actual native captures; no support/collision threshold relaxed.

Older-map obligation: No unaudited older map or model is accepted. Preserve original source assets; review reused models/placements individually.

### DEV-288 — Kitchen provisions chest obstructs entry light switch approach

**Status:** fixed-scoped in final Castle fit-out checkpoint · **Affected:** Castle finishing candidates and their inspection tooling only

Existing actual CastleGroundConstructionTest fails Switch inaccessible after room refit at6.27/29.4.

Cause / uncertainty: New kitchen chest was placed in the established switch operating envelope.

Fix / next action: Move chest to supported rear service corner5.6/33.1. Preserve switch coordinates, full model size, clear kitchen aisle and rerun actual whole-Castle access/lighting checks.

Evidence and verification: castle-finish-rejected-switch.log; final CastleGroundConstructionTest and CastleFinishPlacementTest.

Older-map obligation: No unaudited older map or model is accepted. Preserve original source assets; review reused models/placements individually.

### DEV-289 — Symmetric dining seats crowd every direct table approach

**Status:** fixed-scoped in final Castle fit-out checkpoint · **Affected:** Castle finishing candidates and their inspection tooling only

Existing CastleHubIntegrationTest cannot reach any of the four table approach points.

Cause / uncertainty: Four chairs centered on the four table sides occupy all four existing approach envelopes.

Fix / next action: Offset the north seat55cm along its table edge, retaining source scale, four seats, no model overlap and an actual clear table approach. Retain the existing integration check unchanged.

Evidence and verification: castle-finish-rejected-table-approach.log; final CastleHubIntegrationTest, CastleFinishPlacementTest and dining capture.

Older-map obligation: No unaudited older map or model is accepted. Preserve original source assets; review reused models/placements individually.

### DEV-290 — Castle compass audit omits signed annex/cellar and conflates local cameras with search access

**Status:** fixed-scoped complete42-space source audit; all168 cardinal PNGs actually viewed · **Affected:** SiteRoomCompassAudit Castle branch and gallery labels

Auditor used only CastleRooms.ALL, omitting all8 annex/cellar spaces; clear teleport cameras inside3 sealed rooms could be mistaken for accessible rooms.

Cause / uncertainty: Original core/upper ownership list was treated as the complete HQ roster; camera clearance was not distinguished from search access.

Fix / next action: Append actual saved annex floors and active cellar programme, reject duplicate room IDs, retain stable room IDs and explicit OPEN_PROGRAMME/SEALED_CONTEXT_INSPECTION labels. Preserve body-radius camera checks, source poses, existing navigation/seal tests and all raw images.

Evidence and verification: castle-closeout/castle/castle-rooms.csv:42 rows,39 open programme/3sealed; review.json binds all168 visually reviewed cardinal PNGs and source hashes.

Older-map obligation: Review each other location separately. These new captures replace neither its visual audit nor full native/runtime acceptance.

### DEV-291 — Saved Castle layouts miss rebuild content and structure-v1 reset discards authored edits

**Status:** verified and merged through PR69/70; actual343/0 plus required native Windows/GPU acceptance atfb481f4c · **Affected:** Castle native document loading, saved layout generation and explicit editor rebuild

Unchanged old defaults do not receive newer rooms/fittings; actual old structure-v1 loader changes an authored chair x16.0 back to default15.9.

Cause / uncertainty: No built-in content lineage was stored; new defaults are replaced by incoming records. Structure-v1 loading unconditionally selected defaults, discarding valid authored payloads.

Fix / next action: Freeze three actual source-generated baselines; automatically migrate only exact known built-ins with original-byte backups. Keep changed scenes untouched unless explicitly rebuilt by a three-way native editor transaction. Retain edits/deletions, metadata, shared undo and explicit save; reject new masonry/model/support/nav conflicts without modifying source data. Store rebuild generation so current deletions are never resurrected.

Evidence and verification: LegacySceneLossControl old packaged native loader fails with x15.9; corrected native loader retains16.0. CastleRebuildUpgradeTest covers original/pre-furnishing defaults, modified/deleted props, source additions, light/geometry/paint/nav/station/spawn, shared undo/redo/save, actual wall/new-statue/support negatives, current deletions, authored-v1/partial/future source preservation.

Older-map obligation: Castle-specific native document migration only. Other map codecs and arbitrary custom scene visual fitness remain individually unaudited; no global migration or map acceptance.

### DEV-292 — Native rebuild conflict validation missed fully contained retained objects

**Status:** verified and merged through PR69/70; actual343/0 plus required native Windows/GPU acceptance atfb481f4c · **Affected:** Castle explicit saved-scene upgrade validation

A retained book on its supported 8cm plinth could be swallowed by the newly placed keeper base without any triangle edge crossing.

Cause / uncertainty: Triangle-edge intersections alone do not detect volume containment or coincident geometry.

Fix / next action: Validate welded closed components using solid-angle winding in both directions; reject identical transformed triangle geometry. Preserve surface contact tolerance. Roll back the complete native transaction on conflict.

Evidence and verification: Actual retained supported book negative failed before correction (castle-upgrade-rejected-containment.log). Corrected native test rejects containment; additional exact-crate duplicate control included.

Older-map obligation: Castle saved-scene upgrade only; other map/model intersections remain individually unaudited.

### DEV-293 — Development gallery deployment times out while syncing oversized whole-repository artifact

**Status:** deployment recovered; actual main Pages HTML/index/native/sky PNG bytes verified atdd70440f; future checkpoints require their own deployment readback · **Affected:** Shared GitHub Pages development gallery at main25578d283ec4a77971229172098f267da4c66ad3

Pages42339395744 builds successfully then exceeds deployment deadline syncing files. Artifact3883804673 bytes exceeds advertised1GB allowance.

Cause / uncertainty: Observed deployment timeout while syncing a3.88GB legacy root artifact. Size warning was diagnostic, not proof of a hard failure threshold; a later same-scale deployment succeeded without a pipeline change.

Fix / next action: Retain original captures and verify actual deployed bytes after each checkpoint. Successful42344777413 atdd70440f is independently read back; union-index newly captured resident evidence before the next merge.

Evidence and verification: Source verification: verification/castle-story-2026-11-06/main-pages-recovered.json; actual deployed gallery HTML/index plus native museum/cellar and fixed-sky PNGs matchdd70440f bytes. Later story/loose captures remain distinct deployment gates.

Older-map obligation: Shared publication issue affects all locations; no loss or acceptance of their runtime content is inferred.

### DEV-294 — GPU UI smoke rejects valid van exits needing diagonal WASD input

**Status:** verified and merged through PR69/70; actual343/0 plus required native Windows/GPU acceptance atfb481f4c · **Affected:** GPU live UI van movement planning, House/Farmhouse/School

Exact source2293b75f GPU42341030548 passed Castle/11-map renderer checks but failed Briar House live movement with no short collision-valid WASD route.

Cause / uncertainty: Smoke planner tries only W/S/A/D individually. At the actual House spawn148/360 camera headings have no straight .6m single-key route despite valid two-key diagonal movement in the narrow van aisle.

Fix / next action: Plan all8 actual normalized WASD combinations and press/release both real keys for outward and return movement. Preserve .6m swept native-body candidate, >.5m route threshold, >.30m actual forward movement, <.22m return and real X extraction. Include site/coordinates/elevation/yaw in genuine no-route failures.

Evidence and verification: Actual old single-key reproduction148/360; actual corrected production GPU planner passes1080 real House/Farmhouse/School headings,444 needing two keys. Sources/logs in verification/van-input-recovery-2026-11-06. Fresh corrected GPU acceptance required; no geometry/physics threshold was changed.

Older-map obligation: This corrects the GPU diagnostic, not map geometry. Other location van routes and physical-hardware performance remain independently required. Preserve existing van implementation.

### DEV-295 — Windows Git checkout corrupts frozen native Castle baseline digests

**Status:** verified and merged through PR69/70; actual343/0 plus required native Windows/GPU acceptance atfb481f4c · **Affected:** Castle saved-native rebuild resource loading on core.autocrlf=true installations/CI

Full42341031882 actual transcript is342passed/1failed despite overall workflow success: CastleRebuildUpgradeTest cannot load original baseline because its immutable SHA changed.

Cause / uncertainty: Frozen properties were treated as normal text; Windows checkout converts LF to CRLF before packaging. The runtime byte-integrity guard correctly refuses the corrupted baseline.

Fix / next action: Mark only the frozen runtime/source baseline property paths -text in.gitattributes, preserving exact authored bytes and unchanged SHA guards. Add actual historical saved-document upgrade test to required native editor host command, where failure is not advisory.

Evidence and verification: Actual git core.autocrlf=true checkout before pin reproduces all3bad CRLF digests; after pin all3match original immutable hashes and all3source copies. Verification sources/raw failing full transcript in verification/castle-baseline-checkout-2026-11-06. Original43local packaged bodies pass with canonical bytes, but this does not establish fresh remote Windows acceptance.

Older-map obligation: Castle frozen resources only. Individually audit other fingerprinted text assets; do not normalize arbitrary user scenes or infer old map codecs/other packaging fixed.

### DEV-296 — Visible Castle window mullions have no shared optical or small-body solids

**Status:** fixed;49 focused packaged checks after targeted repairs/resumption, final native input/placement reviews pass; exact-source online acceptance pending · **Affected:** Castle window wooden crossbars and surrounds; new local loose-body contact

An actual light ray passes through the centre of the visibly solid wooden crossbar. Existing CastlePortalSlotTest incorrectly expected that exact ray to pass.

Cause / uncertainty: Window timber boxes were authored only inside SceneRenderer; optical/physical queries knew the aperture and masonry but not its visible wood.

Fix / next action: Publish immutable authored frame boxes from CastleDoorSlots. Render the same unchanged boxes/UVs and consume their exact bounds in Castle optical and small-body sweeps. Move the window-gap positive to the actual lower aperture and add the opaque-crossbar negative.

Evidence and verification: Actual old implementation fails enhanced CastlePortalSlotTest at west-casement-north; corrected source passes. Original no-frame failure and corrected logs retained. Final native on/off window views and full focused gates remain required.

Older-map obligation: Only Castle frames were audited. Other map windows, corners, floors, roof joins and furnishings retain individual optical/body audit obligations; no global light-bleed or collision acceptance.

### DEV-297 — Castle dynamic-cache generalization removed the House diagnostic entrypoint

**Status:** fixed;49 focused packaged checks after targeted repairs/resumption, final native input/placement reviews pass; exact-source online acceptance pending · **Affected:** Shared SceneRenderer dynamic-geometry diagnostics during Castle loose-object integration

Actual DynamicGeometryRetentionTest fails NoSuchMethodException for beginDynamicGeometry(HouseMap) after its private parameter was generalized to Object.

Cause / uncertainty: Existing differential diagnostic reflects the original House-specific method signature. The generic Castle cache path removed that contract.

Fix / next action: Retain the original HouseMap overload and delegate to the same shared Object cache implementation. Keep the original differential test unchanged; preserve source-face parity and changed-object-only rebuild behavior.

Evidence and verification: Actual failure retained in castle-dynamic-house-entrypoint-before.log; source bridge restored before publication. Fresh differential/physical/light checks and final native captures remain required.

Older-map obligation: The bridge preserves House-derived callers and diagnostics. It does not accept unaudited location visuals or physical-hardware performance.

### DEV-298 — Upper stair mesh, loose-body support and optical mass disagreed

**Status:** fixed; complete package,7final contact/lighting checks and actual native inputs pass; exact-source online gates pending · **Affected:** Castle existing upper stairs; local loose objects and analytic lighting

At17.5/25.75 the visible tread is2.21m while the new loose-body adapter used the2.125m smooth navigation incline; a ray wholly inside the rendered closed mass also passed.

Cause / uncertainty: Renderer built20closed step boxes independently; physics reused the intentionally smooth player movement height, and optical solids omitted the staircase mass.

Fix / next action: Share immutable existing step boxes through CastleHubArchitecturalPlan for rendering, physical tread support and optical contact. Preserve exact existing geometry/UVs and smooth player navigation.

Evidence and verification: Actual packaged pre-fix support and closed-stair ray negatives fail. Corrected classes pass both controls and the six-object interaction test. Logs retained under castle-loose-2026-11-06.

Older-map obligation: Castle upper steps only. Other location stair support and light-bleed obligations remain individually unaudited.

### DEV-299 — House fitted-candidate toilet initially faced the wall

**Status:** corrected in construction candidate; actual runtime capture reviewed · **Affected:** HouseRebuildFittings pilot bathroom only; dispatched House unchanged

Initial runtime candidate showed the rear cistern from the room; bowl faced the north wall.

Cause / uncertainty: Assumed source front direction without examining the actual placed model.

Fix / next action: Rotate the source model to face into the bathroom, preserving its original pivot, UVs and measured floor/collider contract.

Evidence and verification: development/captures/house-fixture-fit/bathroom-fixture-fit-runtime.png; source bounds, clearances and all15zone swept traversal pass. The initial candidate image was superseded before publication; no retained before-image is claimed.

Older-map obligation: Only this candidate was corrected. Other maps retain individual fixture orientation, support, circulation and light-separation audits.

### DEV-300 — House construction pilot used phantom flat outdoor support above its editor terrain

**Status:** verified in landscape construction candidate;11 packaged checks pass; full mission/visual gates remain open · **Affected:** briar-rebuild original graybox and fitted construction pilot outdoor surfaces; not dispatched House

At8/5, rendered native terrain is-0.649999976m but the pilot accepts feet at0m. Rectangular room decks still cap outdoor ground.

Cause / uncertainty: Supplemental world-terrain integration supplied optical/body obstacles but retained nominal room floors for exterior walking and rendering.

Fix / next action: Explicit world-terrain-walk authority in the landscape candidate consumes actual native triangle support for outdoor movement/ground recovery. Suppress its duplicate room ground decks. Preserve indoor floors, stair openings, canonical van and previous frozen pilot.

Evidence and verification: HouseRebuildLandscapeTest reproduces exact original0/-.65 mismatch, checks1364native foot/mesh samples and real cellar void; landscape-source heights/punched topology/native triangles persisted separately.

Older-map obligation: Only new House landscape candidate uses this opt-in. Existing graybox remains a frozen reproduction. Audit older maps separately for visible versus physical ground, pads, ceilings, stairs and basement caps.

### DEV-301 — House pilot arrival anchor and review cameras could occupy visible neighbour buildings

**Status:** verified in landscape construction candidate;11 packaged checks pass; full mission/visual gates remain open · **Affected:** House construction pilot approach zone and canonical suburban background parcels

Original approach spawn16.5/23.25 and initial review viewpoints lie inside rendered neighbouring residences; old construction body checks did not include their solids.

Cause / uncertainty: The pilot retained canonical rendered arrival surroundings but did not consume LevelEnclosure private-parcel fences and residence colliders.

Fix / next action: Reuse exact canonical arrival parcel fences/building colliders, move the approach anchor to clear19.8/24, place garden approach at24/20.5 and route around the actual private parcels. Validate ordinary review cameras against actual standing/crouched collision.

Evidence and verification: Two rejected runtime camera PNGs retained under house-landscape. Corrected five views include the actual canonical van; updated swept audit reaches all15zones/required sockets at50229positions. Explicit neighbouring-solid negative added.

Older-map obligation: Canonical arrival contract unchanged. Audit every older isolated/native map builder for omission of rendered neighbour colliders, private-parcel containment and standing anchor fit. No global map acceptance.

### DEV-302 — House pilot exterior inherits room finishes and repeated room gables

**Status:** verified in dwelling construction candidate; one closed roof/exterior independent of interior finishes; whole-map gates remain open · **Affected:** House first-principles construction pilot exterior; other generic room-shell maps require separate audit

Native landscape overview exposes several parallel small room gables and exterior wall patches using interior tile/room finishes, rather than one deliberately constructed dwelling envelope.

Cause / uncertainty: Generic renderer builds roof and double-sided wall finish from each gameplay room independently.

Fix / next action: Next House construction pass must author one coherent external roof/wall envelope with real bearing/join logic, preserving internal rooms, openings, source furniture, terrain and canonical arrival. Revalidate optical separation and geometry after repair.

Evidence and verification: development/captures/house-landscape/native-parcel-overview.png; all source vertices/UVs independently match native runtime export. Construction view is not final architecture acceptance.

Older-map obligation: Audit individual older House/Farmhouse/institution room-derived envelopes; current bounds or navigation checks do not prove roof joins, external finish or structural coherence.

### DEV-303 — House pilot kitchen window and garden door overlap

**Status:** verified in current construction/export checkpoint; complete map acceptance remains open · **Affected:** House graybox/fitted/landscape inherited kitchen north-wall opening pair

Door16.5/width1.05 and window15.7/width1.25 overlap0.35m; actual rear runtime view shows conflicting frames.

Cause / uncertainty: Inherited portal reservations were not checked for simultaneous collinear frame clearance.

Fix / next action: Move candidate window to14.9, retaining ID/width/owners/elevation; use source portals for skin, frames, optical/body and navigation.

Evidence and verification: Rejected rear runtime PNG and updated opening/frame reservation checks; corrected actual rear view reviewed.

Older-map obligation: Audit other maps individually. For DEV-306 refresh/review older complete-scene LevelSceneSource exports; runtime captures and standalone GLB reimports do not automatically inherit this defect. No global model, light-bleed, terrain or hardware acceptance.

### DEV-304 — New porch slab initially lies below visible editor terrain

**Status:** verified in current construction/export checkpoint; complete map acceptance remains open · **Affected:** New House envelope construction candidate only

Actual failing clearance check: deck0m, soil0.0006313420587m; terrain could paint across a flush slab.

Cause / uncertainty: Initial porch reused nominal0m instead of providing positive constructed slab clearance.

Fix / next action: Raise deck/support to40mm; posts/feet stand on that same surface; preserve continuous navigation into0m entry.

Evidence and verification: Actual before assertion and corrected body/support/route checks retained; no prior rejected porch PNG is claimed.

Older-map obligation: Audit other maps individually. For DEV-306 refresh/review older complete-scene LevelSceneSource exports; runtime captures and standalone GLB reimports do not automatically inherit this defect. No global model, light-bleed, terrain or hardware acceptance.

### DEV-305 — Arrival background buildings and fences crowd the House

**Status:** verified in current construction/export checkpoint; complete map acceptance remains open · **Affected:** House-side suburban context in the construction pilot; owner-directed correction

Nearby context visually pressed against the dwelling/porch in native and runtime views.

Cause / uncertainty: Canonical street house placements were used without a site-specific clear forecourt reservation.

Fix / next action: Move the three near residences and private fences12m down street through one renderer/collider contract. Retain their models, opposite frontage, road and canonical van.

Evidence and verification: Actual rendered mesh clearance6.295m, corresponding source collider checks and current whole-front/native overview; all15zone/socket traversal53324positions.

Older-map obligation: Audit other maps individually. For DEV-306 refresh/review older complete-scene LevelSceneSource exports; runtime captures and standalone GLB reimports do not automatically inherit this defect. No global model, light-bleed, terrain or hardware acceptance.

### DEV-306 — Native scene texture export scrambles the van atlas and other material origins

**Status:** verified in current construction/export checkpoint; complete map acceptance remains open · **Affected:** Complete scenes exported by LevelSceneSource; original runtime mesh/UVs remain intact

Van atlas mismatches14474/16384runtime texels;12006have substantial channel differences, producing the mangled-looking preview reported by the owner.

Cause / uncertainty: Exporter copied original PNG rows/factors without each material runtime UV origin and exact diffuse sampling; procedural atlas orientation also differed.

Fix / next action: Export original-resolution texels through exact GlbProps/LevelMaterials/MaterialAtlas sampling, retaining source UVs. Regenerate delivered envelope native source and captures.

Evidence and verification: Actual before/after logs: all66used textured materials and8962879texels exactly match runtime. Corrected native van close view reviewed. Source GLB/VanStaging/VanBodyGeometry/GlbProps unchanged.

Older-map obligation: Audit other maps individually. For DEV-306 refresh/review older complete-scene LevelSceneSource exports; runtime captures and standalone GLB reimports do not automatically inherit this defect. No global model, light-bleed, terrain or hardware acceptance.

### DEV-307 — Texture regression overwrites canonical native authoring staging

**Status:** verified in current construction/export checkpoint; complete map acceptance remains open · **Affected:** New native texture diagnostic harness only

Running the regression after other construction checks changed process-local material IDs in the canonical export; the native parity guard correctly rejected old group counts.

Cause / uncertainty: Diagnostic exported into the same staging folder used by native authoring.

Fix / next action: Add explicit output path and isolate regression under build/native-texture-parity; preserve canonical authoring export and rebuild native source through the standard path.

Evidence and verification: Actual failed parity transcript preserved. Enhanced full House construction body now passes and verifies canonical geometry SHA remains unchanged.

Older-map obligation: Audit other maps individually. For DEV-306 refresh/review older complete-scene LevelSceneSource exports; runtime captures and standalone GLB reimports do not automatically inherit this defect. No global model, light-bleed, terrain or hardware acceptance.

### DEV-254 — Safe areas lack a shared animated energy boundary matching ghost exclusion

**Status:** fixed-scoped; focused CPU/TCP and actual OpenGL evidence; installer acceptance pending · **Affected:** Shared van at investigation sites, entire Castle HQ, and settled active protective wards

The investigation van has no recognizable sanctuary haze; the old optional arena cue uses rectangular motes rather than a circular fading dome.

Cause / uncertainty: Protection and visualization use separate room/rectangle rules; normal investigations do not enable the prior arena-only presentation.

Fix / next action: Add a shared circular van dome and whole-Castle sanctuary, animated blue cloud/wisp material with continuous wall-to-crown fading, settled-ward reuse, swept host exclusion, protected spawning/combat, Twins handling and public ghost-view snapshots.

Evidence and verification: development/levels/previews/safe-area-barrier/: actual runtime frames and two 12-frame animations. SafeAreaBarrierTest, SafeAreaHazeTest, LanSafeAreaBarrierTest, existing ghost/counter/Twins/van checks and complete TCP round pass. Actual RTX 5070 OpenGL frames pass. Logs: development/quality/evidence/safe-area-barrier-2026-11-03/.

Older-map obligation: All built-in/custom-map ghost spawns tested. Visual captures cover the dispatched House and Castle; older-map terrain/lighting and installed full-game acceptance remain individual obligations. House rebuild remains a separate graybox and is not claimed dispatched.

### DEV-255 — Ward ground seals remain at world Y zero and appear before landing

**Status:** fixed-scoped; focused CPU/TCP and actual OpenGL evidence; installer acceptance pending · **Affected:** Shared van at investigation sites, entire Castle HQ, and settled active protective wards

A thrown or upstairs ward can show its seal at ground-floor height rather than at its actual settled support.

Cause / uncertainty: The renderer hardcodes Y .038 and does not distinguish a landed ward from a flying projectile when drawing ground bars.

Fix / next action: Resolve the settled support once for both seal and haze; keep airborne projectiles visible without advertising sanctuary; place upstairs seals at the authoritative floor height.

Evidence and verification: development/levels/previews/safe-area-barrier/: actual runtime frames and two 12-frame animations. SafeAreaBarrierTest, SafeAreaHazeTest, LanSafeAreaBarrierTest, existing ghost/counter/Twins/van checks and complete TCP round pass. Actual RTX 5070 OpenGL frames pass. Logs: development/quality/evidence/safe-area-barrier-2026-11-03/.

Older-map obligation: All built-in/custom-map ghost spawns tested. Visual captures cover the dispatched House and Castle; older-map terrain/lighting and installed full-game acceptance remain individual obligations. House rebuild remains a separate graybox and is not claimed dispatched.

### DEV-308 — Completed shared sanctuary implementation omitted from rebuild publication

**Status:** reconciled locally; focused11checks pass; checkpoint publication and new full integration acceptance tracked separately · **Affected:** All investigation vans, settled wards and Castle headquarters; House rebuild review images

Owner observes no protective dome in rebuilt location captures despite completed prior implementation.

Cause / uncertainty: Sanctuary source and evidence survived only on local feat/shared-safe-area-haze-20261103; active rebuild and origin/main did not contain it. Original checkout retained related dirty working files.

Fix / next action: Publish original source branch, preserve original dirty checkout through separate-index recovery commit, merge existing sanctuary and ghost-exclusion changes while retaining newer foot shadows, generate fresh rebuilt-House runtime views and audit every registered worktree/local branch/stash.

Evidence and verification: development/levels/rebuild/verification/sanctuary-reconcile-2026-11-07; development/captures/house-sanctuary; sourcefd8c1984 and recovery8bc1e48c; actual four-peer TCP test passes after reconciliation.

Older-map obligation: Shared runtime van contract applies to all sites; individual map arrival/ghost-route visual acceptance remains required. Analytic animated postprocess haze is not an opaque GLB part and is absent from geometry-only Blender exports. Castle uses whole-headquarters sanctuary.

### DEV-309 — Authored grid-map generator omitted from renderer and LAN source authority

**Status:** corrected candidate; packaged4peerTCP control/state test passes; exact-source online acceptance pending · **Affected:** House functional construction; any authored non-native site with a generator socket

InteractionFocus finds generator sockets but only native-terrain renderer draws starters and LanHost rejects non-native sources or omits state resync.

Cause / uncertainty: Electrical capability is incorrectly inferred from native terrain implementation.

Fix / next action: Use actual authored generator sockets as capability. Share starter geometry across both renderer paths and gate host source interaction/view snapshots on hasGenerator. Existing room-light source/breaker state remains authoritative.

Evidence and verification: house-services-2026-11-07/HouseRebuildPhysicsTest.log includes actual supported starter/panel, connected energy4.8to0.187,4TCP source/breaker,view-resync,ghost/distance rejections; actual runtime controls and lighting captures.

Older-map obligation: Audit each authored generator installation independently. Maps without generator sockets retain prior supply contract; native Hollow Creek control behavior must remain compatible.

### DEV-310 — Full-height generator bounding box makes visible recessed starter unreachable

**Status:** source-derived connected-part collision correction passes actual mesh/host aim and House traversal; final full-map acceptance pending · **Affected:** House functional generator placement

Actual button mesh ray hits61supported camera rays but clearInteractionSight rejects all because base-plinth bounds incorrectly extend to the handle top.

Cause / uncertainty: One overall model AABB is used as solid/optical occupancy over the narrower engine body.

Fix / next action: Preserve overall source bounds for placement reservations. Replace actual generator collider with deterministically derived connected-component source bounds; preserve source mesh/UV/pivot and all interaction reach/occlusion validators.

Evidence and verification: house-services-2026-11-07/rejected-control-approach.log and rejected-control-probe.log; corrected actual4TCP test,50831-position House sweep and generator-control-runtime.png.

Older-map obligation: Audit recessed controls and conservative source collider envelopes individually; this repair does not globally ignore furnishing collision or accept all older installations.

### DEV-311 — Remote-site ghost breaker-trip acceptance remains unproven

**Status:** IMPLEMENTED; packaged domain, actual solo AI and four-peer TCP checks pass; exact-source CI and per-location acceptance pending · **Affected:** Remote-site power gate and rebuilt House; older locations require individual audit

Source-stop and ordinary hunter breaker interaction pass, but inspected host ghost-action lighting path emits temporary pulses; persistent ghost-specific trip/reset behavior has no proven runtime acceptance.

Cause / uncertainty: Existing power tests explicitly defer ghost-host breaker-trip audit. Neither generator presence nor hunter switching proves the required ghost behavior.

Fix / next action: Accepted lightning_static DISTURB arcs trip the normal persistent breaker in solo and host paths; preserve source-running, batteries and independent candles. Host sends existing LIGHTS state; physical reset remains authoritative.

Evidence and verification: development/levels/rebuild/verification/house-story-power-2026-11-07/GhostElectricalTest.log: 52 species/controller combinations, actual solo AI, four TCP peers, resync and physical breaker reset. Other haunt variants and rejected/stale/unpowered commands retain behavior.

Older-map obligation: Audit all remote and older maps individually; no blanket acceptance from House source-stop or packaging. Preserve ghost-specific exceptions and independent fire/oil/battery loads.

### DEV-312 — Guessed service endpoints leave12mm above actual switch plates

**Status:** corrected source-bound endpoints; attachment check and actual runtime view pass · **Affected:** House fit-out conduit ends

Endpoint uses socketY+.08 despite actual decoded plate top+.056;24mm conduit leaves12mm air.

Cause / uncertainty: Endpoint uses socketY+.08 despite actual decoded plate top+.056;24mm conduit leaves12mm air.

Fix / next action: Use decoded light_switch_1 plate bounds and actual wall/soffit planes; retain physical/optical source geometry.

Evidence and verification: development/levels/rebuild/verification/house-fitout-2026-11-07/rejected-switch-endpoint.log; HouseRebuildPhysicsTest.log; distribution-runtime.png

Older-map obligation: Audit each existing map/model/cover installation individually; shared source correction and House evidence do not accept unaudited layouts or rendering.

### DEV-313 — House power acceptance waits local milliseconds instead of authoritative host ticks

**Status:** corrected test-only52188e75 published; fresh full42360138343 live; old06full347/1 not accepted · **Affected:** House power TCP harness and online servicesPR76

A250ms sleep can precede the real3tick interaction cooldown. Actual Windows third toggle rejects,so SOURCE1is absent; local compound run also misses ACCEPT.

Cause / uncertainty: A250ms sleep can precede the real3tick interaction cooldown. Actual Windows third toggle rejects,so SOURCE1is absent; local compound run also misses ACCEPT.

Fix / next action: Wait4actual host ticks under simulation mutex before fixture placement; preserve host cooldown and all interaction validation.

Evidence and verification: development/levels/rebuild/verification/house-fitout-2026-11-07/rejected-local-clock-cooldown.log; services-clock rejected-full-42357637604.log and corrected-tcp.log

Older-map obligation: Audit each existing map/model/cover installation individually; shared source correction and House evidence do not accept unaudited layouts or rendering.

### DEV-314 — Separate authored circuit paths duplicate physical shared conduit lengths

**Status:** corrected65shared closed sections; overlap regression,physics and native parity pass · **Affected:** House fit-out source/distribution and common ceiling trunks

Logical paths each emit a full mesh over the same shared trunk; source input and distribution output also overlap.

Cause / uncertainty: Logical paths each emit a full mesh over the same shared trunk; source input and distribution output also overlap.

Fix / next action: Consolidate collinear same-finish spans; separate actual incoming/outgoing board leads and reuse original textures.

Evidence and verification: development/levels/rebuild/verification/house-fitout-2026-11-07/rejected-overlapping-conduits.log; fitout-services.json; native/runtime captures

Older-map obligation: Audit each existing map/model/cover installation individually; shared source correction and House evidence do not accept unaudited layouts or rendering.

### DEV-315 — Usable hiding places lack requested energy cue while disabled pockets still exclude ghosts

**Status:** shared material implemented and locally verified; exact-source online/older-map visual acceptance remains open · **Affected:** Available indoor/outdoor hiding locations across investigations

User asks for the same energy material on safe hiding locations. Prior renderer has no cue; ghost helper treats all placeholders/outdoor100percent as excluded even when disabled.

Cause / uncertainty: User asks for the same energy material on safe hiding locations. Prior renderer has no cue; ghost helper treats all placeholders/outdoor100percent as excluded even when disabled.

Fix / next action: Reuse exact sanctuary shader for available hiding locations; derive cupboard veil/associated refuge marker from authored data and shared budget. Align ghost exclusion with actual eligibility. Preserve existing hide/crouch activation and network privacy.

Evidence and verification: development/levels/rebuild/verification/house-fitout-2026-11-07/HidingAreaHazeTest.log; rejected-unbudgeted-ghost-boundary-test.log; corrected LanGhostHidingBoundaryTest.log; hiding-energy runtime captures

Older-map obligation: Audit each existing map/model/cover installation individually; shared source correction and House evidence do not accept unaudited layouts or rendering.

### DEV-316 — Mending record overhangs its actual bedside support

**Status:** SCOPED CORRECTION; source-footprint check and actual runtime view pass · **Affected:** Rebuilt House construction candidate; shared behavior where explicitly noted

Initial mending-tin placement at12.5/16.9 overhung the real bedside tabletop.

Cause / uncertainty: First-principles support/service audit of actual source and runtime behavior

Fix / next action: Original source triangles reject unsupported corners; recenter at12.6/17.0 without scaling the source.

Evidence and verification: development/levels/rebuild/verification/house-story-power-2026-11-07/ and development/captures/house-story/: rejected-mending-support.log; HouseRebuildPhysicsTest.log; inherited-mending-runtime.png

Older-map obligation: Audit each older map individually. These House controls do not accept older layouts, plumbing, object supports, lighting or saved native edits.

### DEV-317 — Loose bodies sink into sculpted terrain and rendered stair treads

**Status:** SCOPED CORRECTION; actual ground and renderer-triangle contact checks pass · **Affected:** Rebuilt House construction candidate; shared behavior where explicitly noted

Actual loose mug contacted terrain about5cm low and rendered tread about122mm low.

Cause / uncertainty: First-principles support/service audit of actual source and runtime behavior

Fix / next action: World-walking maps use authored terrain ground. Authored clutter uses actual discretized visible treads; player continuous ramps and legacy unflagged behavior remain.

Evidence and verification: development/levels/rebuild/verification/house-story-power-2026-11-07/ and development/captures/house-story/: rejected-exact-terrain-contact.log; rejected-rendered-tread-contact.log; HouseRebuildPhysicsTest.log

Older-map obligation: Audit each older map individually. These House controls do not accept older layouts, plumbing, object supports, lighting or saved native edits.

### DEV-318 — Electrical outdoor fixtures bypass source and breaker blackout

**Status:** SCOPED CORRECTION; packaged independent-source controls pass · **Affected:** Rebuilt House construction candidate; shared behavior where explicitly noted

Outdoor room brightness stays1 and allowed electrical fixture emission despite lost supply.

Cause / uncertainty: First-principles support/service audit of actual source and runtime behavior

Fix / next action: Check real supplied state before mains intensity; preserve candle and van battery branches.

Evidence and verification: development/levels/rebuild/verification/house-story-power-2026-11-07/ and development/captures/house-story/: rejected-outdoor-supply.log; GhostElectricalTest.log

Older-map obligation: Audit each older map individually. These House controls do not accept older layouts, plumbing, object supports, lighting or saved native edits.

### DEV-319 — Rebuilt Briar location loses canonical House case history

**Status:** SCOPED CORRECTION; canonical address and readable interaction pass · **Affected:** Rebuilt House construction candidate; shared behavior where explicitly noted

Isolated briar-rebuild identity was not aliased to house for public history and records.

Cause / uncertainty: First-principles support/service audit of actual source and runtime behavior

Fix / next action: Canonicalize rebuild identity through SiteStories and SiteCaseFiles, retaining existing address/history without revealing ghost identity.

Evidence and verification: development/levels/rebuild/verification/house-story-power-2026-11-07/ and development/captures/house-story/: HouseRebuildPhysicsTest.log; household-records-runtime.png

Older-map obligation: Audit each older map individually. These House controls do not accept older layouts, plumbing, object supports, lighting or saved native edits.

### DEV-320 — Rebuilt House lacks radiators and visible water distribution

**Status:** SOURCE IMPLEMENTED; scoped packaged/real TCP and native geometry checks pass; complete fixture refinement and full House gates remain · **Affected:** Rebuilt House construction candidate; shared behavior where explicitly noted

Boiler exists but the candidate has no authored radiators or visible domestic water distribution, despite the location heat/plumbing history.

Cause / uncertainty: First-principles support/service audit of actual source and runtime behavior

Fix / next action: Retain seven original full-size radiator assets; source-contact flow/return and water/waste nodes share actual closed renderer/physics geometry. Rear boiler service ports preserve its grille. Under-deck distribution avoids pedestrian routes and both stair openings.

Evidence and verification: development/levels/rebuild/verification/house-heating-water-2026-11-07/HouseRebuildServicesTest.log; services-native-verification.json; services-contacts.json; asset-use.csv;10runtime views and native overview

Older-map obligation: Audit each older map individually. These House controls do not accept older layouts, plumbing, object supports, lighting or saved native edits.

### DEV-321 — Visible generator feed review camera harness does not compile

**Status:** SCOPED CORRECTION; current test compiles and actual cable review captures pass; exact-source CI pending · **Affected:** HouseRebuildStoryTest visible feed camera review; isolated House construction candidate

First feed capture camera is blocked; attempted fallback calls nonexistent HouseMap.clearSight(Vec,Vec), causing javac cannot-find-symbol.

Cause / uncertainty: Review camera API mismatch introduced while selecting an actual supported unobstructed viewpoint.

Fix / next action: Use actual House clearLineOfSight doubles; select supported positions and test the visible surface rather than the occluded cable center. Native scene/export regenerated at visible-feed revision4ae1c4bb4b9f1e3f.

Evidence and verification: development/levels/rebuild/verification/house-story-power-2026-11-07/rejected-feed-camera.log; checkpoint-notes.txt; latest HouseRebuildStoryTest.java source; HouseRebuildStoryTest.log and generator-service-outlet/visible-perimeter-feed/house-service-inlet runtime captures

Older-map obligation: No older-map acceptance inferred. House remains isolated and unpromoted.

### DEV-322 — Multiplayer animation acceptance races on copy-on-write packet sublist

**Status:** SCOPED CORRECTION; exact-source remote full suite pending · **Affected:** Mandatory packaged regression acceptance; House construction and actual hunter animation

Main059ca123 full42363868962 fails LanHunterAnimationGameplayTest with ConcurrentModificationException in Peer.feed after observer animation pass.

Cause / uncertainty: Observed CI concurrency or accumulated stage execution, not a packaging failure

Fix / next action: Take immutable packet-array snapshots before slicing; retain all live TCP movement, posture and scene reset assertions. Actual packaged animation check passes.

Evidence and verification: development/levels/rebuild/verification/house-story-power-2026-11-07/main-059ca123-full-42363868962.log; LanHunterAnimationGameplayTest.log; scoped-results.json

Older-map obligation: No map or physical hardware acceptance inferred from test repair. Preserve per-location audit and exact-source full suite gate.

### DEV-323 — Accumulated House acceptance bundle exceeds per-test process deadline

**Status:** SCOPED CORRECTION; exact-source remote full suite pending · **Affected:** Mandatory packaged regression acceptance; House construction and actual hunter animation

Main059ca123 full42363868962 kills HouseRebuildPhysicsTest after90seconds; actual suite totals347/2 despite green wrapper.

Cause / uncertainty: Observed CI concurrency or accumulated stage execution, not a packaging failure

Fix / next action: Register every existing stage check as a mandatory isolated manifest entry, including both physical power variants and all four texture variants. Preserve90second per-entry deadline; remove no assertions.

Evidence and verification: development/levels/rebuild/verification/house-story-power-2026-11-07/main-059ca123-full-42363868962.log; LanHunterAnimationGameplayTest.log; scoped-results.json

Older-map obligation: No map or physical hardware acceptance inferred from test repair. Preserve per-location audit and exact-source full suite gate.

### DEV-324 — Heating assembly initially treated a placement Assembly as a level

**Status:** SCOPED CORRECTION; packaged source/contact and runtime review pass; exact-source CI pending · **Affected:** New isolated House heating/water construction candidate

Initial compile of HouseRebuildServices used Fit.finish() without its .level() component.

Cause / uncertainty: New service integration or visual-review failure discovered during construction

Fix / next action: Consume actual Assembly.level(); final packaged compilation and complete service test pass.

Evidence and verification: development/levels/rebuild/verification/house-heating-water-2026-11-07/ and development/captures/house-heating-water/: HouseRebuildServices.java; final packaged HouseRebuildServicesTest.log

Older-map obligation: No older-map layouts, pipe supports, source surfaces, UVs or electrical behavior are accepted by this House correction. Audit individually.

### DEV-325 — Initial boiler routing obscures grille and soil pipe has a box silhouette

**Status:** SCOPED CORRECTION; packaged source/contact and runtime review pass; exact-source CI pending · **Affected:** New isolated House heating/water construction candidate

Actual runtime review showed front-connected boiler pipework covering its ventilation grille and a thick square soil service head.

Cause / uncertainty: New service integration or visual-review failure discovered during construction

Fix / next action: Use independently verified actual rear service surfaces; preserve source grille. Forty/100mm sanitary runs use closed8sided28triangle sections and a supported service cap. Preserve source electrical24mm geometry.

Evidence and verification: development/levels/rebuild/verification/house-heating-water-2026-11-07/ and development/captures/house-heating-water/: rejected-boiler-front-pipes-runtime.png; rejected-square-soil-head-runtime.png; corrected boiler-pipework/bathroom-water runtime views

Older-map obligation: No older-map layouts, pipe supports, source surfaces, UVs or electrical behavior are accepted by this House correction. Audit individually.

### DEV-326 — House review finds unfilled daily-use storage and garden functions

**Status:** SCOPED IMPLEMENTATION AND RUNTIME REVIEW PASS; exact-source full361 and full House gates pending · **Affected:** Rebuilt House room/function audit; older site camera coverage remains individually unaudited

Actual four-direction review and current39source props show the coat vestibule without coat/boot storage, kitchen without cold-food storage and garden/patio still an empty reservation.

Cause / uncertainty: Incomplete functional furnishing or legal-but-unrepresentative centre fallback in an advisory visual audit

Fix / next action: Fit original full-size boot/towel rack, source-preserving supported coat, original180triangle generated-enamel fridge and grounded source bench/planters. Preserve all routes, door poses, cupboard budget, source coordinates and original assets.

Evidence and verification: development/levels/rebuild/verification/house-room-review-2026-11-07/reviewed-rooms.csv; verification.json; actual house-room-review compass and authored-spawn PNGs; HouseRebuildFurnishingsTest.log; furnishings-native-verification.json; house-functional-fill runtime captures

Older-map obligation: Audit each older site independently. Legal cameras, graph connectivity and room names alone do not prove physical, functional, visual or gameplay completion.

### DEV-327 — Nearest-centre audit cameras miss important landing and garden coverage

**Status:** SCOPED CORRECTION;8actual supported authored-anchor views inspected · **Affected:** Rebuilt House room/function audit; older site camera coverage remains individually unaudited

Legal nearest-centre camera for upper landing lies on the stair, while garden fallback hugs the back door and sees through it. Those four-way views alone give incomplete functional context.

Cause / uncertainty: Incomplete functional furnishing or legal-but-unrepresentative centre fallback in an advisory visual audit

Fix / next action: Keep actual centre/fallback views and add the source-authored landing/garden spawn cameras; require traversability and same room ownership. Record source coordinates and inspect every supplementary view.

Evidence and verification: development/levels/rebuild/verification/house-room-review-2026-11-07/reviewed-rooms.csv; verification.json; actual house-room-review compass and authored-spawn PNGs

Older-map obligation: Audit each older site independently. Legal cameras, graph connectivity and room names alone do not prove physical, functional, visual or gameplay completion.

### DEV-328 — Fridge generation report accesses a freed Blender image after reset

**Status:** SCOPED CORRECTION; actual source/export/runtime checks pass; exact-source CI pending · **Affected:** New House furnishing/asset-generation diagnostic; older source/audits remain individually unaudited

Export/reimport/renders succeeded, but final report used image.size after factory reset removed its RNA; Blender initially exited0 despite the traceback.

Cause / uncertainty: Source attachment gap, Blender lifetime/failure-propagation or incorrect stacked-floor review metadata

Fix / next action: Cache dimensions before reset; use --python-exit-code1 and require actual verification report. Final source/export/render report passes.

Evidence and verification: development/levels/model-production/house-fridge/; development/levels/rebuild/verification/house-functional-fill-2026-11-07/; development/captures/house-functional-fill/: rejected-report-lifetime.log; house-fridge-build.log

Older-map obligation: Audit older model attachments, camera ownership/elevation and strict generation status individually; no blanket older-map or hardware acceptance.

### DEV-329 — Original coat floats below its source hook

**Status:** SCOPED CORRECTION; actual source/export/runtime checks pass; exact-source CI pending · **Affected:** New House furnishing/asset-generation diagnostic; older source/audits remain individually unaudited

Existing level_trace_coat front/side source review shows a shoulder-to-hook gap. Subsequent component audit also found the original pocket back12mm off the cloth card; the188triangle hanger-only approval was incomplete.

Cause / uncertainty: Source attachment gap, Blender lifetime/failure-propagation or incorrect stacked-floor review metadata

Fix / next action: Deliver200triangle additive sibling: retain all152original geometry/UV/material triangles, add36triangle hanger and12triangle closed cloth gusset. Independent decoded-component test proves garment/pocket bridge. Supersede prior approval and retain188GLB/side view as rejected evidence.

Evidence and verification: development/levels/model-production/house-fridge/; development/levels/rebuild/verification/house-functional-fill-2026-11-07/; development/captures/house-functional-fill/: coat-verification.json; coat-front/coat-side previews; HouseRebuildFurnishingsTest.log

Older-map obligation: Audit older model attachments, camera ownership/elevation and strict generation status individually; no blanket older-map or hardware acceptance.

### DEV-330 — Cold-storage diagnostic camera captures cellar instead of kitchen

**Status:** SCOPED CORRECTION; actual source/export/runtime checks pass; exact-source CI pending · **Affected:** New House furnishing/asset-generation diagnostic; older source/audits remain individually unaudited

Camera row used foyer room hint0 at kitchenXZ; ground fallback found the lower cellar, so the fridge-labeled view showed the washer while position was legally traversable.

Cause / uncertainty: Source attachment gap, Blender lifetime/failure-propagation or incorrect stacked-floor review metadata

Fix / next action: Use actual kitchen room hint2 and require same room ownership as well as traversability; preserve wrong-floor capture. Correct kitchen and function views were actually inspected.

Evidence and verification: development/levels/model-production/house-fridge/; development/levels/rebuild/verification/house-functional-fill-2026-11-07/; development/captures/house-functional-fill/: rejected-cold-storage-camera-floor-runtime.png; corrected kitchen-cold-storage-runtime.png

Older-map obligation: Audit older model attachments, camera ownership/elevation and strict generation status individually; no blanket older-map or hardware acceptance.

### DEV-331 — Saved custom House architecture follows changing shipped template

**Status:** SCOPED IMPLEMENTATION PASS; native legacy/rebuilt USER compatibility checks pass; source-bound full362/native/GPU pending · **Affected:** House-derived USER projects and upcoming House default promotion; other template upgrades need individual migration review

UserLevelLibrary.source rebuilds the template through current LevelPlans.buildSource on every load, then applies existing scene props at their historical coordinates. Replacing the House default would silently change floor elevations, walls and openings below those edits.

Cause / uncertainty: Saved template metadata identifies site but no architectural generation.

Fix / next action: Pin explicit House architectural generations; interpret historical versionless House projects as original architecture without rewriting their files. Resolve the rebuilt generation separately for actual USER-map verification. Keep built-in promotion and explicit author-driven migration pending.

Evidence and verification: HouseTemplateCompatibilityTest; src/main/java/org/beckenburg/UserLevelLibrary.java; house-template-compatibility checkpoint evidence

Older-map obligation: Audit every changed template independently; shipped-v1 for other sites is not a promise of frozen architecture or complete migration.

### DEV-332 — Compatibility test attempted against stale JAR before package completion

**Status:** SCOPED CORRECTION; successful fresh package precedes passing compile/runtime checks · **Affected:** Local saved-map verification process, not shipped gameplay

Test compilation cannot resolve newly added template methods/constants because an earlier JAR was read while packaging was still live. The package then failed sandbox JAR replacement with AccessDeniedException.

Cause / uncertainty: Dependent test compilation was started before authoritative terminal package success.

Fix / next action: Preserve failed logs; rerun authorized packaging outside the failed sandbox boundary and await success before compiling/running tests. No stale JAR test result accepted.

Evidence and verification: development/levels/rebuild/verification/house-template-compatibility-2026-11-07/rejected-stale-jar-test-compile.log; successful package/test logs; original AccessDeniedException was observed in tool output before package log reuse

Older-map obligation: No older-map acceptance from stale binaries; every source-bound verification must follow successful packaging.

### DEV-333 — Native mission composition replaces rebuilt pivots and generator bodies with generic collision

**Status:** SCOPED CORRECTION; exact-source identity, every retained body/multiplicity and actual Agent Services/native/runtime tests pass locally; remote acceptance pending · **Affected:** Rebuilt House used through USER native authoring; older sites require individual collider authority review

Actual custom rebuilt House collider multiset differs from source even without prop edits. Editing uses native GLB origins as generic collision centres and can add a full generator box or false bedding bodies.

Cause / uncertainty: LevelAuthoringSession.compose discarded/re-added all props using legacy LevelPlans.propCollider rather than authored collision policy. Initial list-order suspicion was disproved by a multiplicity-aware comparison.

Fix / next action: Return exact source for unchanged props; preserve exact bodies for unchanged objects during edits; derive rebuilt pivot-aware and multipart collision through shared helpers. Retain bedding overlays as non-colliding. Update Editor Agent Services parity/dirty bounds to consume the same bodies, checking multiplicity.

Evidence and verification: HouseTemplateCompatibilityTest exact source revision5c8da94b90d34afd; retained exact source bodies after real agent fridge edit; rejected-authored-collision-drift.log; edited-custom-house-kitchen-runtime.png; focused existing editor/recovery/furnishing tests

Older-map obligation: Audit each future rebuilt source collision policy and functional attachment behavior before default promotion; no blanket older-map acceptance.

### DEV-334 — Kitchen fridge wedged between back and dining doors

**Status:** SCOPED RELOCATION PASS; source/native/runtime views inspected; current exact-source CI pending · **Affected:** House furnishing source and derived custom-map kitchen

Owner rejects actual kitchen capture: fridge crowds the short wall section between two doors despite sampled collision and route checks passing.

Cause / uncertainty: Placement prioritized numerical clearances over coherent kitchen work/storage arrangement.

Fix / next action: Move fridge to X14.9/Z14.1925 against finished south wall, yawPI facing north,6cm rear ventilation. Both garden/dining door centres remain more than2m away. Source door poses and50164route positions, cellar access/counter, original atlas, actual power/TCP and USER editor checks pass. Rebuild native/export/asset coordinates from actual source.

Evidence and verification: User rejection; development/captures/house-template-compatibility/rejected-fridge-between-doors-runtime.png; development/captures/house-functional-fill/kitchen-doorways-clear-runtime.png; kitchen-cold-storage-runtime.png; development/levels/rebuild/verification/house-fridge-relocation-2026-11-07/

Older-map obligation: Review appliance placement as a functional room arrangement individually; passing collision alone never accepts an older layout.

### DEV-335 — Saved built-in House has no architectural version or safe rebuild operation

**Status:** SCOPED NATIVE/GAMEPLAY SOURCE PASS; final363/native/GPU acceptance pending · **Affected:** House native editor document and upcoming live default promotion

Saved built-in House props contain stable native IDs but no indication of which building owns their coordinates. Replacing default architecture would reinterpret them; resetting would discard custom edits.

Cause / uncertainty: Built-in editor snapshots and BSCN payload only tracked props, unlike Castle layout-version migration.

Fix / next action: Store validated architecture metadata in the same native BSCN; make it undoable with props. Add explicit human/Agent Services three-way rebuild preserving authored deltas, deletions and custom IDs; abort unsupported/obstructing edits, preserve original bytes before first rebuild save, and route real saved House dispatch through selected source. Fresh default promotion remains gated separately.

Evidence and verification: HouseSourceVersions.java; HouseRebuildUpgrade.java; HouseRebuildUpgradeTest; native editor command

Older-map obligation: Every existing saved architecture needs its own baseline/version/conflict tests. No automatic migration or older-map acceptance.

### DEV-336 — House rebuild editor command initially fails compilation on Map reference

**Status:** SOURCE CORRECTED; successful package and native migration checks pass · **Affected:** New DeveloperLevelEditor House rebuild command only

Package compiler reports cannot find symbol Map at new House rebuild command.

Cause / uncertainty: New UI case used unqualified Map without an import in this class.

Fix / next action: Use qualified java.util.Map consistent with surrounding editor commands; preserve failing package log and require fresh successful package.

Evidence and verification: build/house-migration-rejected-missing-map-import.log; native migration checkpoint

Older-map obligation: No older-map or runtime claim from a failed package.

### DEV-337 — Standalone native edits can detach rebuilt functional assemblies

**Status:** SCOPED GUARD PASS; actual native generator move/remove refused without state loss; whole-assembly authoring remains separate · **Affected:** Rebuilt House built-in and USER authoring; source controls, pipes, supported story objects and bedding

The previous generic put/remove path accepts moving or deleting the generator alone while its authored starter/feed sockets remain at their source coordinates; moving a table or bed similarly leaves supported authored children behind.

Cause / uncertainty: Prop-only editor commands had no attachment/dependency contract.

Fix / next action: Identify source service/story/bed/loose-parent dependencies. Refuse standalone edits/removals of those assemblies and validate them before compose/save; keep ordinary independent furniture editable. Migration rejects dependent assembly conflicts rather than detaching or losing user edits. Full assembly-authoring operations remain separate work.

Evidence and verification: HouseAuthoringAttachments.java; HouseRebuildUpgradeTest generator move/remove state-preservation assertions; c1fe6f53 prior put/remove source

Older-map obligation: Audit fixture/parent relationships individually on each old map; these guards cover rebuilt House source only.

### DEV-338 — Retained House object validation needs full source-model containment checks

**Status:** SCOPED SOURCE/CONFIGURED256MB NATIVE PASS; source-bound365/full/editor/GPU pending; no physical FPS or older-map acceptance · **Affected:** New native House three-way migration custom-object conflict validation

A vertex-inside-body check alone cannot prove that a large retained model does not contain a smaller default fitting or duplicate/cross an open source surface.

Cause / uncertainty: Initial House conflict validator used source vertices and physical bodies without complete triangle crossing/closed-volume containment.

Fix / next action: Reuse the existing Castle validation geometry kernel through a world-triangle entrypoint: reject duplicate geometry, proper triangle crossings and containment in either direction. Preserve Castle construction and existing validation behavior. Add actual nested-source-crate regression.

Evidence and verification: HouseRebuildUpgradeTest nested native crate geometry; shared CastleRebuildValidation.modelConflict; older Castle validation regression

Older-map obligation: Audit retained custom models and full source supports individually; broad boxes and vertex-only checks cannot certify older maps.

### DEV-339 — Windows editor retains inactive Castle geometry while loading rebuilt House and exhausts256MB heap

**Status:** SCOPED SOURCE/CONFIGURED256MB NATIVE PASS; source-bound365/full/editor/GPU pending; no physical FPS or older-map acceptance · **Affected:** Actual Windows editor switching Castle and House, including native House migration; retained-renderer scene families

New256MB human-editor migrated kitchen capture produces OutOfMemoryError in D3D Screen Updater, TimerQueue and AWT EventQueue. Histogram retains529264Face objects/50.8MB and881030Point objects/35.2MB plus model/image memory.512MB earlier GUI path passed but does not accept configured256MB.

Cause / uncertainty: activateResidentScene released resident GPU batches but kept inactive Castle static geometry/chunks/assemblies/environment and inactive House prepared geometry reachable across scene-family switches. Castle topology replacement also held previous CPU faces/chunks/active assemblies until after the replacement allocation. House-frame identity sets retained prior source faces on return to Castle.

Fix / next action: Retire inactive CPU source geometry and owners during authoritative scene activation, preserving an already prepared destination and normal warm frames. Require real256MB GUI retry, exact return-render pixel parity, preload identity and existing retained-renderer checks. Keep normal native viewport resolution in the capture.

Evidence and verification: house-migration-rejected-gui-256-oom.log; house-migration-gui-256-histogram.log; EditorSceneRetentionTest; actual Windows editor retry

Older-map obligation: Audit scene changes including native locations and GPU ownership; this fix is not physical FPS or other-map visual acceptance.

### DEV-340 — Scene retirement regression initially calls nonexistent preparation API

**Status:** SCOPED SOURCE/CONFIGURED256MB NATIVE PASS; source-bound365/full/editor/GPU pending; no physical FPS or older-map acceptance · **Affected:** New EditorSceneRetentionTest compilation only

Compiler rejects prepareStaticLevel(HouseMap).

Cause / uncertainty: Test used the wrong preparation method name.

Fix / next action: Use actual prepareStaticScene API; preserve rejected compile log and require fresh compile/run.

Evidence and verification: house-migration-rejected-preparation-api.log; EditorSceneRetentionTest

Older-map obligation: No acceptance from a test that failed compilation.

### DEV-341 — Native GUI test can report PASS after background AWT OutOfMemoryError

**Status:** SCOPED SOURCE/CONFIGURED256MB NATIVE PASS; source-bound365/full/editor/GPU pending; no physical FPS or older-map acceptance · **Affected:** EditorAgentGuiTest and reported Windows native acceptance

256MB retry logs AWT OutOfMemoryError while rebuilding Castle, then prints PASS and exits0 because assertions later succeed.

Cause / uncertainty: Test tracked only exceptions returned through invokeAndWait; Swing timer/D3D background failures were unobserved.

Fix / next action: Install a scoped uncaught-exception handler, retain the first asynchronous failure, restore previous handler on close and fail the test after cleanup if any thread failed. Preserve raw misleading-PASS log; do not accept it.

Evidence and verification: house-migration-rejected-gui-background-oom.log; native GUI uncaught failure assertion

Older-map obligation: Older green GUI results are scoped evidence; inspect raw logs and do not infer absence of background errors.

### DEV-342 — Identical immutable PNGs decode separately for every imported material

**Status:** SCOPED CORRECTION PASS; actual256MB scene return and clean Windows editor pass · **Affected:** Shared runtime GLB material loading, House/Castle/native editor source collection

After complete inactive-scene retirement, the256MB Castle return still fails. Retired-cache histogram shows90.47MB byte arrays,31.37MB int arrays and30.59MB source vertices; all inspected inactive CPU cache fields are empty.

Cause / uncertainty: GlbProps.decode invokes ImageIO.read for each material, retaining independent identical PNG bitmaps even when materials or models reference the same source image.

Fix / next action: Share immutable decoded PNGs by SHA256 plus readable-cover treatment; keep all material IDs/UV/sampler/factor/alpha metadata separate. Use a bounded512-entry weak-reference lookup so transient native reviews are not retained. No image resizing, artwork changes or raised heap. After sharing the matching retired checkpoint shows56.70MB byte arrays; exact RGBA/material tests, return pixels and real256MB GUI pass.

Evidence and verification: SourceImageSharingTest; house-scene-retired-histogram.log and house-scene-retired-shared-images-histogram.log; clean house-migration-gui-256.log;365manifest/native checks

Older-map obligation: Source PNG sharing covers loader identity only. Preserve per-location material/UV and hardware acceptance; raw heap-class differences are not FPS or total process-memory benchmarks.

### DEV-343 — Image-sharing test uses a live-screen name outside the existing contract

**Status:** TEST FIXTURE CORRECTED; actual independent live-screen/source-image check passes · **Affected:** New source-image-sharing regression fixture only

liveSurfaceMaterial refuses sharing-test-live despite the prior PNG/material assertions passing.

Cause / uncertainty: Fixture omitted required van live screen prefix.

Fix / next action: Use actual named live-screen contract and rerun; preserve rejected fixture log.

Evidence and verification: house-migration-rejected-live-surface-name.log; SourceImageSharingTest.log

Older-map obligation: No source/live-screen runtime failure inferred from the rejected test fixture.

### DEV-345 — House CLI recovery reads corrupt primary architecture before restoring valid backup

**Status:** SCOPED CORRECTION PASS; actual failing CLI script and expanded backup matrix pass locally; corrected-source native CI pending · **Affected:** PR82 native42424140546; explicit editor-agent --recover bootstrap after primary corruption

Windows native migration/GUI checks pass, but corrupt-start CLI recovery fails because LevelPlans.buildSource selects architecture by decoding the corrupt primary before the previous document is restored.

Cause / uncertainty: New source selection preceded existing primary-corruption recovery.

Fix / next action: Use a fixed legacy parse fallback for House bootstrap and select/validate architectural authority from the backup being restored. Preserve previous identity/future metadata checks and validate required rebuilt attachments before replacement. Added actual versionless/rebuilt byte-exact bootstrap plus foreign/future/detached-service refusal tests. Rerun the exact previously failing CLI script.

Evidence and verification: development/levels/rebuild/verification/house-native-recovery-2026-11-07/; native-42424140546-failed.log; LevelAuthoringRecoveryTest; actual verify_editor_agent_cli.py PASS

Older-map obligation: Audit pre-recovery source/version selection independently for every other native adapter. This correction does not accept unaudited maps.

### DEV-350 — Live-clock gait acceptance skips stride under cold render delays

**Status:** SCOPED FIXTURE CORRECTION PASS LOCALLY; new remote full365 pending · **Affected:** LanHunterAnimationGameplayTest and parent recovery acceptance

Full42426485514 reports364/1/0/365; cleric renders IDLE/WALK_A but no WALK_B.

Cause / uncertainty: Host advances during seconds-long renderer/capture work; tracker intentionally resets gait after missing a second of observations.

Fix / next action: Quiesce scheduler, invoke every actual host tick and await normal network body/posture batches. Retain all original three-role movement/idle/Ctrl/reset/departure assertions and deliberately delay capture1.1seconds. Added stale-gap/reset/A-B recovery unit controls.

Evidence and verification: development/levels/rebuild/verification/hunter-gait-recovery-2026-11-07/; full-42426485514.log; hunter-gait-recovery-gameplay.log; hunter-gait-recovery-unit.log

Older-map obligation: No production gait, other-map art, GPU gait or physical FPS acceptance inferred. Existing stale-observation reset remains unchanged.

### DEV-351 — Gait fixture mistakes invalid zero-vector STEP for a stop command

**Status:** SCOPED FIXTURE CORRECTION PASS LOCALLY; new remote full365 pending · **Affected:** LanHunterAnimationGameplayTest and parent recovery acceptance

Controlled-clock fixture waits forever for a posture update after STEP(0,0).

Cause / uncertainty: Motion vectors must be nonzero; the old live-clock test accidentally relied on unrelated periodic snapshots after rejection.

Fix / next action: Stop sending movement, advance real ticks and await an actual stationary world snapshot; do not relax motion validation.

Evidence and verification: development/levels/rebuild/verification/hunter-gait-recovery-2026-11-07/; hunter-gait-rejected-zero-step-fixture.log; hunter-gait-recovery-gameplay.log

Older-map obligation: No production gait, other-map art, GPU gait or physical FPS acceptance inferred. Existing stale-observation reset remains unchanged.

### DEV-352 — Controlled gait fixture reads STATE before completed body snapshot

**Status:** SCOPED FIXTURE CORRECTION PASS LOCALLY; new remote full365 pending · **Affected:** LanHunterAnimationGameplayTest and parent recovery acceptance

First controlled-clock stationary retry still renders an older moving body.

Cause / uncertainty: Host periodic STATE precedes its world body/posture batch.

Fix / next action: Wait through the actual final hunter posture row of that tick before rendering. No forged body or panel clock.

Evidence and verification: development/levels/rebuild/verification/hunter-gait-recovery-2026-11-07/; hunter-gait-rejected-state-before-body-fixture.log; hunter-gait-recovery-gameplay.log

Older-map obligation: No production gait, other-map art, GPU gait or physical FPS acceptance inferred. Existing stale-observation reset remains unchanged.

### DEV-344 — House authored train encounter has no runtime implementation

**Status:** CORE/SOLO/REAL4TCP PASS LOCALLY; remote encounter and subjective audio/whole-location acceptance pending · **Affected:** House first-principles story/atmosphere stage

Supported records and loose belongings existed, but the required individually authored optional location encounter remained plan-only.

Cause / uncertainty: No location-specific host/solo controller was connected to runtime.

Fix / next action: Add an original once-per-round supported wooden-train turn while room is empty and investigators lack full-object sight. Reuse real loose-state replication and source support, add original ambient audio, protect possession/death/disconnect/timeout and evidence negatives. Protocol3 is a separate explicit compatibility checkpoint.

Evidence and verification: HouseEncounterTest; HouseEncounterSessionTest; HouseEncounterNetworkTest; actual before/after captures and original windup WAV; house-encounter-2026-11-07 proof directory

Older-map obligation: Each location needs its own authored encounter, predicates, host/privacy/evidence/reset and actual visual/audio review.

### DEV-346 — Encounter test compile omitted existing helper classes and used nonexistent resume API

**Status:** TEST FIXTURE CORRECTED; final actual solo and four-TCP checks PASS · **Affected:** New House encounter integration test fixture

Initial verification attempt failed; rejected evidence preserved.

Cause / uncertainty: Partial javac omitted helper dependencies and referenced confirmResumeTicket.

Fix / next action: Compile actual helpers and use acknowledgeStoredResume.

Evidence and verification: development/levels/rebuild/verification/house-encounter-2026-11-07/house-encounter-rejected-helper-and-resume-api.log; final HouseEncounterSessionTest.log and HouseEncounterNetworkTest.log

Older-map obligation: No older-map runtime defect or acceptance inferred from this fixture correction.

### DEV-347 — Reconnect test acknowledged a fresh nonrotated token

**Status:** TEST FIXTURE CORRECTED; final actual solo and four-TCP checks PASS · **Affected:** New House encounter integration test fixture

Initial verification attempt failed; rejected evidence preserved.

Cause / uncertainty: Fixture attempted RESUMEACK on initial welcome instead of resumed token rotation.

Fix / next action: Persist real capability and acknowledge only after reconnect rotation.

Evidence and verification: development/levels/rebuild/verification/house-encounter-2026-11-07/house-encounter-rejected-fresh-token-ack.log; final HouseEncounterSessionTest.log and HouseEncounterNetworkTest.log

Older-map obligation: No older-map runtime defect or acceptance inferred from this fixture correction.

### DEV-348 — Encounter fixture jumped host clock past ghost authority

**Status:** TEST FIXTURE CORRECTED; final actual solo and four-TCP checks PASS · **Affected:** New House encounter integration test fixture

Initial verification attempt failed; rejected evidence preserved.

Cause / uncertainty: Direct tick jump violated GhostDomain contiguous-clock contract.

Fix / next action: Advance every actual host tick through eligibility; no production clock relaxation.

Evidence and verification: development/levels/rebuild/verification/house-encounter-2026-11-07/house-encounter-rejected-clock-jump.log; final HouseEncounterSessionTest.log and HouseEncounterNetworkTest.log

Older-map obligation: No older-map runtime defect or acceptance inferred from this fixture correction.

### DEV-349 — Encounter fixture discarded sound while waiting for later pose

**Status:** TEST FIXTURE CORRECTED; final actual solo and four-TCP checks PASS · **Affected:** New House encounter integration test fixture

Initial verification attempt failed; rejected evidence preserved.

Cause / uncertainty: Fixture awaited CLUTTER before earlier SOUND and used a closed acoustic path.

Fix / next action: Await sound first and use actual occluded but acoustically connected corner; retain source cue gain.

Evidence and verification: development/levels/rebuild/verification/house-encounter-2026-11-07/house-encounter-rejected-cue-order-fixture.log; final HouseEncounterSessionTest.log and HouseEncounterNetworkTest.log

Older-map obligation: No older-map runtime defect or acceptance inferred from this fixture correction.

### DEV-356 — Fresh House promotion exposes editor fixtures that mutate the first attached assembly

**Status:** SCOPED FIXTURE CORRECTION PASS; seven focused checks and actual256MB GUI/CLI PASS; remote candidate acceptance pending · **Affected:** Fresh-default candidate authoring identity/recovery/session/service and native CLI/GUI fixtures

Four actual local tests reject moving/removing prop-0 because it is now an attached kitchen counter. Historical fixtures also assume that a fresh new template is the old House.

Cause / uncertainty: Tests used incidental array order and historical default generation instead of the intended independent object or actual historical document.

Fix / next action: Keep attachment protections. Select independent furniture by actual source and stable ID; use generation-specific native colliders; manufacture genuine historical fixtures. GUI first verifies fresh rebuilt open/save/game load, then historical recovery/rebuild. CLI selects inspected fridge ID. Retain every existing assertion and rejected logs.

Evidence and verification: development/levels/rebuild/verification/house-default-2026-11-07/; rejected/*.log; final seven focused test logs; actual house-default-gui.log and house-default-cli.log; both fresh runtime captures

Older-map obligation: No older-map authoring, whole-location or hardware acceptance inferred; audit explicit architecture/attachment fixtures individually.

### DEV-357 — Remaining native rough shells omit enclosed ceilings and reserve only top-end stair holes

**Status:** SCOPED NATIVE BATCH CHECKPOINT; actual runtime/location acceptance remains open · **Affected:** Remaining25structural candidates and shared rough/native export workflow

Earlier shell recipe adds walls/floors but no top enclosure; elevated links reserve a small endpoint hole rather than the full flight.

Cause / uncertainty: Rough-only geometry was not yet engineered for enclosed structures.

Fix / next action: Add closed160mm caps/undersides, share actual upper floor undersides, cut whole-flight openings and build measured treads where run permits.25native exports pass;8links/runtime/roof-form gates remain.

Evidence and verification: development/levels/rebuild/structures/; development/levels/rebuild/verification/roster-structure-2026-11-07/; 25packet implementation.json and verification.json; final Blender log; all50export views

Older-map obligation: Castle/House/Blackpine/Hollow Creek are retained and individually unaudited for this new batch; never infer runtime or older-map fixes from native structure checks.

### DEV-358 — Planning text and diagram stand-ins enter structural GLB exports

**Status:** SCOPED NATIVE BATCH CHECKPOINT; actual runtime/location acceptance remains open · **Affected:** Remaining25structural candidates and shared rough/native export workflow

Native labels can become large tessellated meshes; placeholder van and ritual diagram solids also enter a full-scene export.

Cause / uncertainty: Unfiltered scene export treated editable planning annotations as construction geometry.

Fix / next action: Keep editable native labels/marks, export structural meshes only and exclude diagram stand-ins. Actual Farmhouse export is5208triangles; shared pass131219across25. Canonical runtime van remains unchanged.

Evidence and verification: development/levels/rebuild/structures/; development/levels/rebuild/verification/roster-structure-2026-11-07/; build_blockouts.py selection; native/GLB hashes; per-packet triangle and mesh counts

Older-map obligation: Castle/House/Blackpine/Hollow Creek are retained and individually unaudited for this new batch; never infer runtime or older-map fixes from native structure checks.

### DEV-356 — Search-envelope annotations leave context doors and exterior service mouths open

**Status:** SCOPED NATIVE BATCH CHECKPOINT; actual runtime/location acceptance remains open · **Affected:** Remaining25structural candidates and shared rough/native export workflow

Native rough recipe opens SEALED_NO_ROUTE links. First repaired Prison still fails exported boundary L107 between entry and east-service.

Cause / uncertainty: Annotations were not physical seals; exterior sectors have no building wall to fill automatically.

Fix / next action: Exclude sealed openings/support routes, retain solid walls/caps and build full shared exterior-mouth infill. All192authored fixed boundary checks pass. Preserve failed Academy and Prison exports. Actual runtime terrain/enclosure returns and bypass checks remain open.

Evidence and verification: development/levels/rebuild/structures/; development/levels/rebuild/verification/roster-structure-2026-11-07/; rejected/structure-open-context-v1/; roster-structure-seals.log actual Prison failure; final native boundary arrays

Older-map obligation: Castle/House/Blackpine/Hollow Creek are retained and individually unaudited for this new batch; never infer runtime or older-map fixes from native structure checks.

### DEV-357 — Reusing an intermediate circular deck core can leave a top cap open

**Status:** SCOPED NATIVE BATCH CHECKPOINT; actual runtime/location acceptance remains open · **Affected:** Remaining25structural candidates and shared rough/native export workflow

Source review found shaped floor core fallback applied to ceiling copies, also duplicating intermediate upper-floor surfaces.

Cause / uncertainty: The floor ring helper was reused without distinguishing the top enclosure from an intermediate stair/maintenance core.

Fix / next action: Separate shaped structural decks, suppress caps already covered by the next floor, use a closed top disk and apply real apertures. Reimported lighthouse caps/decks and all final closed-solid/depth/ray checks pass; actual engine spiral/headroom/sky audit remains.

Evidence and verification: development/levels/rebuild/structures/; development/levels/rebuild/verification/roster-structure-2026-11-07/; structural_pass.shaped_deck; WLD14 final exported source and reimported views; final Blender log

Older-map obligation: Castle/House/Blackpine/Hollow Creek are retained and individually unaudited for this new batch; never infer runtime or older-map fixes from native structure checks.

### DEV-358 — Native batch review assumes Pillow is installed in the active Python

**Status:** SCOPED NATIVE BATCH CHECKPOINT; actual runtime/location acceptance remains open · **Affected:** Remaining25structural candidates and shared rough/native export workflow

Review command exits with ModuleNotFoundError before producing contact sheets.

Cause / uncertainty: The environment lacks the image dependency used by the new review script.

Fix / next action: Install dependency only under workspace build/structure-review-python, add its lookup and document reproduction. Review and --check pass; all7sheets manually viewed.

Evidence and verification: development/levels/rebuild/structures/; development/levels/rebuild/verification/roster-structure-2026-11-07/; roster-structure-review-missing-pillow.log; dependency log; final review PASS

Older-map obligation: Castle/House/Blackpine/Hollow Creek are retained and individually unaudited for this new batch; never infer runtime or older-map fixes from native structure checks.

### DEV-359 — Eight remote elevation links have zero or insufficient physical stair run

**Status:** NATIVE ROUTE REDESIGN CHECKPOINT; capsule movement and runtime integration remain open · **Affected:** WLD-03 L13; WLD-04 L13/L14 plus new upper landing link L14B; WLD-05 L15; WLD-10 L16/L19; WLD-12 L16; WLD-15 route14

Eight reservations cannot form 180mm risers / 240mm treads, including zero-run basement/tower links.

Cause / uncertainty: Schematic centre lines were retained without a buildable flight or complete supporting storey.

Fix / next action: Author six revised coordinate sources with measurable flights, separated lookout lanes/landings, continuous cave/tower volumes and explicit constructed railway/ship decks. Preserve original layouts and scenes. Verify actual reimported solids and tread poses. Do not unlock from native results.

Evidence and verification: development/levels/rebuild/verification/roster-routes-2026-11-07/; six revised rough-layout.json; structures/<site>/physics-contract.json and structure-physics.hcs2; tools/audit_roster_routes.py

Older-map obligation: Audit every older map independently for physical stair run, landings, headroom, deck openings, body/edge clearance and supported arrivals. No inherited acceptance.

### DEV-360 — Level route reservation strips refill stair openings and obstruct heads

**Status:** NATIVE SPINE OBSTRUCTIONS CORRECTED; swept capsule acceptance OPEN · **Affected:** Shared structural generator; reproduced WLD-03, WLD-04, WLD-05 and WLD-10; all other packets require individual audits

Six-site headless native query found 45 obstructed tread-centre spine rays despite earlier floor-hole construction.

Cause / uncertainty: Separate level connector strips were exported across the voids already reserved in floor slabs. Monastery L21 also crossed under the north catacomb stair.

Fix / next action: Apply shared stair apertures to horizontal connector supports; move monastery west stair into its own shaft with source-authored level approach. All506 actual tread centres now have support and clear 0.3â€“1.8m spine rays. Retain rejected probe rows and GLB hashes. Full capsule traversal is not established.

Evidence and verification: development/levels/rebuild/verification/roster-routes-2026-11-07/; six revised rough-layout.json; structures/<site>/physics-contract.json and structure-physics.hcs2; tools/audit_roster_routes.py

Older-map obligation: Audit every older map independently for physical stair run, landings, headroom, deck openings, body/edge clearance and supported arrivals. No inherited acceptance.

### DEV-361 — Native stepped candidates fail current body and traversability queries

**Status:** PARTIAL: explicit stair movement implemented; source landings and runtime integration OPEN · **Affected:** Six revised remote candidates and potentially other new tread-based sites; existing mature maps not certified

506 supported tread centres with clear spine rays still include capsule/body and four-footprint support failures; monastery has zero traversable tread centres in the current kernel.

Cause / uncertainty: Authored treads, body clearance, edges and the kernel narrow neighbour-height allowance need component-specific diagnosis. Clear centre rays cannot establish a full body sweep.

Fix / next action: Explicit step handling retained. Authored landing repairs now yield182/221vertical segments and44/84complete links passing both directions; all intermediate flat landings included. Continuous exact-endpoint College, Mansion and five-turn lighthouse chains PASS; four other chains fail.39vertical segments remain physical blockers. No runtime integration/guard/terrain acceptance inferred.

Evidence and verification: development/levels/rebuild/verification/roster-landings-2026-11-07/; prior stair checkpoint retains earlier failures

Older-map obligation: Audit every older map independently for physical stair run, landings, headroom, deck openings, body/edge clearance and supported arrivals. No inherited acceptance.

### DEV-362 — Expanded stair apertures create collapsed ceiling sliver cells

**Status:** NATIVE REPAIR VERIFIED across25exports; runtime acceptance remains open · **Affected:** WLD-14 keeper foyer ceiling; shared rectangular slab partitioner

All-roster rebuild rejects reimported Ceiling cap foyer.010 as not closed after expanded shaft openings.

Cause / uncertainty: Nearly coincident partition planes from different stair reservations form tiny cells that collapse under exported float/weld precision. Distant nonintersecting holes also unnecessarily subdivide slabs.

Fix / next action: Preserve failed GLB and failure record. Filter cuts that do not intersect the slab and merge construction planes within0.1mm while retaining exact outer boundaries. Re-export lighthouse pilot, then regenerate coherent25-packet batch and retain closed-solid assertions. Circular aperture cleanup also welds/dissolves only sub0.1mm Boolean noise and recalculates normals. Final25 independent reimports pass6562closed solids and1528measured depth parts.

Evidence and verification: development/levels/rebuild/rejected/route-shaft-sliver-wld14/; development/levels/blockouts/build_blockouts.py slab; roster-routes-2026-11-07 evidence

Older-map obligation: Check every slab family after aperture changes. No unaudited source or circular Boolean mesh inherits acceptance from this rectangular-cell repair.

### DEV-363 — Roster collision probe exposes crossing stairs and terrain covering authored flights

**Status:** NATIVE HEAD-OBSTRUCTION REPAIRS VERIFIED across1720treads; full capsule/location acceptance OPEN · **Affected:** College L84; Mansion L59/L60; WLD-09 L14/L18; WLD-14 route14/20; WLD-18 route07/08/13/18; mortuary L14/L15

All25 native collision exports load and1720tread centres have support, but90 head-height spine rays are obstructed across these6sites.

Cause / uncertainty: Actual exported triangles identify crossing stair flights, a folded-back landing, gallery/exhibit walls and uncut proposal terrain over underground approaches. Native closed-solid checks cannot establish route clearance.

Fix / next action: Retain exact per-pose overhead component names and hashes. Next separate crossing stair lanes/landings, repair wall passage coordinates and implement actual persisted terrain holes before physical terrain acceptance. Recheck full capsule routes, neighbouring routes and optical/solid negatives. Do not delete terrain visually or weaken collision to pass. Separated five-source revision removes48of90spine hits; College/Mansion/Hydroelectric386/386supported with clear spine rays. Remaining42hits all identify terrain triangles in Lighthouse/Silo/Mortuary. Evidence: verification/roster-separated-2026-11-07/. Real saved editor hole-punching in Lighthouse/Silo/Mortuary removes the last42terrain hits. All1720tread centres now supported and spine-clear. Actual GLB terrain matches saved native terrain triangles/flags; terrain finish and full actor movement remain open. Evidence: verification/roster-editor-terrain-2026-11-07/.

Evidence and verification: development/levels/rebuild/verification/roster-routes-2026-11-07/all25/tread-probes.tsv and route-probes.json; structures/<site>/physics-contract.json

Older-map obligation: Audit retained Castle/House/Blackpine/Hollow Creek separately. Zero-tread WLD-16 is not a route pass. No map is unlocked by this source-wide read-only probe.

### DEV-364 — Hydroelectric return stair still needs authored shaft enclosure and foundations

**Status:** OPEN CONSTRUCTION DETAIL; native candidate only · **Affected:** WLD-09 east return stair L18 and its connection to the east-stair volume

Revised return clears other flights but the native overview shows the intermediate/upper stairs beyond the original east-stair shell; final supporting foundations/enclosure are absent.

Cause / uncertainty: Original shell contains only the upper landing storey; separated return needs its own full-height structural program rather than intersecting existing flight masses.

Fix / next action: Preserve revised clear circulation coordinates, author the shared shaft/landing/storey surfaces and properly elevated wall apertures, then foundation/terrain support and guards. Keep candidate locked and rerun neighbouring routes after those additions.

Evidence and verification: development/levels/rebuild/structures/wld-09/; captures/structure-batch/roster-structure-sheet-04.png; verification/roster-separated-2026-11-07/

Older-map obligation: Audit full stair load paths and room/shaft volume parity individually across remote and retained maps; solid tread boxes are not final structural engineering or terrain support acceptance.

### DEV-365 — Offline terrain sampler resolves seed inputs using newly created working paths and can swallow Blender errors

**Status:** FIXED IN AUTHORING TOOL; failed-sampling atomicity verified · **Affected:** Saved editor terrain reproduction for WLD-07-MORTUARY and shared offline terrain wrapper

After creating a rebuilt annex source, sampling an earlier seed ref chooses that new path although it did not exist in the seed commit. Blender exits zero on an uncaught Python exception without --python-exit-code.

Cause / uncertainty: Path selection consulted current filesystem rather than immutable seed tree, and the wrapper relied on Blender default exit semantics.

Fix / next action: Resolve authoritative layout candidates inside the seed tree with git cat-file; pass --python-exit-code1 before sampling. Successful reproduction binds all three saved worlds. Invalid-site control exits nonzero and leaves27authoritative world files unchanged.

Evidence and verification: verification/roster-editor-terrain-2026-11-07/terrain-rejected-seed-layout.log, failure-atomicity.json and fixed reproduction log; tools/sample_roster_terrain.py and build_roster_editor_terrain.py

Older-map obligation: Use explicit immutable source refs and nonzero Python failure exits in other Blender authoring pipelines. Do not infer a sampler pass from a process returning normally.

### DEV-366 — Saved terrain seeds retain rough proposal perimeters and unfinished aperture rims

**Status:** OPEN DESQUAREIFY/CONSTRUCTION FINISH GATE · **Affected:** Lighthouse, Silo and Mortuary saved native terrain candidates; individual audits remain for all other maps

Native inspection retains broadly rectangular extents; lighthouse cliff/water fringe shows raw slivers. Newly punched building/stair/shaft cuts need rims, retaining walls, guards and foundation finish.

Cause / uncertainty: The saved-world bridge deliberately starts from existing rough terrain sources; correct persistent hole topology is not final geographic/architectural design.

Fix / next action: Overall terrain-first audit now records18remaining native candidates without continuous terrain,3proposal landforms and4saved punched sources needing macro review. Settle landform/height layers, slopes/drainage/foundations and reshuffle blockouts as authorized before further isolated path repairs. Finish edges/rims/guards/canonical van/desquareify per site; retained4mature maps require individual audits.

Evidence and verification: development/levels/rebuild/terrain-overall-pass/; verification/roster-basements-2026-11-07/; earlier saved-editor terrain evidence retained

Older-map obligation: Audit every terrain footprint individually. Native hole success does not certify prior ground shapes, source padding, model support or boundary containment.

### DEV-367 — Asset discovery overlooks staged site kits and treats a small runtime sample as the working library

**Status:** CANONICAL DISCOVERY POINTER AND INDEX IMPLEMENTED; named chat pack imports remain pending · **Affected:** Map rebuild model selection, staged roughs/refinements, source-library masters and source-chat artifact discovery

Initial furnishing preparation inspected24runtime furniture models without consulting the298map-specific roughs,15refinements, source-library masters or the owner-identified model conversations.

Cause / uncertainty: No mandatory root-level inventory pointer connected the separate staged catalogs, masters, runtime set and conversation sources.

Fix / next action: Add ASSET_LIBRARY.md and AGENTS/README lookup rule. Canonical index scans every tracked/unignored GLB, records paths/stages/map assignments/hashes and distinguishes aliases/map/review assemblies. Add query/site lookup, regeneration/staleness gate and313staged-model bounds. Rough assets are permitted for blocking/budget/placement now, refinement later; do not invent duplicate replacements. Three exact named chat references remain recorded, with history rate-limited and artifact import pending.

Evidence and verification: ASSET_LIBRARY.md; development/assets/asset-library-index.json; tools/index_staged_map_models.py; development/levels/rebuild/furnishings/; native staged-source inspection and runtime sample images

Older-map obligation: Reconcile every requested missing/new prop against this index and its site kits before creating a replacement. Imported chat packs must preserve provenance and update this pointer; no automatic quality or map acceptance from indexing.

### DEV-368 — Furnishing export guard reads deleted Blender objects after independent reset

**Status:** FIXED; all25final exports independently reimported · **Affected:** Shared staged-model furnishing generator

First pilot aborts in the hidden-cache export guard with StructRNA Object has been removed.

Cause / uncertainty: The guard retained Blender objects across read_factory_settings instead of storing their count.

Fix / next action: Capture the structural mesh count before reset; compare imported unassigned meshes against that scalar. All124final source instances have independent bounds/UV and no hidden source-cache leak.

Evidence and verification: development/levels/rebuild/verification/roster-prop-allocation-2026-11-07/; furnishings/<site>/placements.json; room-programs.json; actual export captures

Older-map obligation: Retained Castle/House/Blackpine/Hollow Creek require individual source/model/route audits. First spatial allocation does not certify complete model quality, capacity, services, native collision/navigation or map availability.

### DEV-369 — Furnishing fit double-counts route buffers and overextends endpoint clearance

**Status:** FIXED IN ROUGH ALLOCATION; source widths preserved · **Affected:** School office/changing rooms and shared allocation reservation geometry

Valid small-room furniture allocations are rejected despite preserving the authored clear pedestrian width.

Cause / uncertainty: The gate added an actor buffer to already clear route widths, then expanded longitudinal endpoints by the full corridor half-width.

Fix / next action: Reserve the full authored lateral width,0.3m endpoint extension and0.12m prop separation. Use exact polygon/AABB separation for diagonal corridors. Validate the actual1.8m/.22m kernel before/after; no body/sweep regressions remain.

Evidence and verification: development/levels/rebuild/verification/roster-prop-allocation-2026-11-07/; furnishings/<site>/placements.json; room-programs.json; actual export captures

Older-map obligation: Retained Castle/House/Blackpine/Hollow Creek require individual source/model/route audits. First spatial allocation does not certify complete model quality, capacity, services, native collision/navigation or map availability.

### DEV-370 — Props can obstruct physical routes that cross rooms without naming those rooms

**Status:** FIXED IN ROUGH ALLOCATION; zero new qualified route blocks · **Affected:** Asylum treatment, Church records, College cafe, Farmhouse stall and shared route reservation workflow

Actual composite native physics finds46new body/support blocks and53new failed baseline-qualified sweeps in the first124-instance batch.

Cause / uncertainty: The allocation gate reserved only links whose named endpoints included the room, missing routes physically passing through other rooms. Farm loading route L22 itself crossed stall A.

Fix / next action: Reserve every physical link segment at the relevant height using polygon intersection, reposition props and reroute the2.4m farm loading approach around the stall while retaining its original barn threshold. Final109506samples preserve73058baseline-valid poses and68167baseline-valid sweeps with zero new failures. Preserve rejected rows and placements.

Evidence and verification: development/levels/rebuild/verification/roster-prop-allocation-2026-11-07/; furnishings/<site>/placements.json; room-programs.json; actual export captures

Older-map obligation: Retained Castle/House/Blackpine/Hollow Creek require individual source/model/route audits. First spatial allocation does not certify complete model quality, capacity, services, native collision/navigation or map availability.

### DEV-371 — Many authored flat route samples fail baseline native support or body checks

**Status:** PARTIAL; path winding corrected,9511current baseline-invalid poses remain · **Affected:** 25remaining native map candidates; per-site baseline-invalid counts in route-regressions.json

Of109506flat-route centre/side samples,36448are already unsupported or body-blocked before furnishing. These are not valid walkable samples.

Cause / uncertainty: Native proposals still include schematic connectors, terrain/foundation omissions, incomplete portal/ownership integration and physically conflicting routes; diagnosis is per source rather than inferred from closed solids.

Fix / next action: Current110409flat samples contain100898valid/9511invalid poses; both revision and furnishing gates have zero new qualified blocks. Corrected876inward walking tops, but remaining source support/wall/terrain conditions require terrain-first whole-site review. Preserve initial36448/109506failed evidence and current exact rows.

Evidence and verification: development/levels/rebuild/verification/roster-basements-2026-11-07/; prior roster-prop-allocation-2026-11-07/; terrain-overall-pass/

Older-map obligation: Retained Castle/House/Blackpine/Hollow Creek require individual source/model/route audits. First spatial allocation does not certify complete model quality, capacity, services, native collision/navigation or map availability.

### DEV-372 — Asset index regeneration encounters Windows invalid-argument error on direct overwrite

**Status:** ATOMIC PUBLICATION IMPLEMENTED; regeneration and stale check PASS, exact OS cause unconfirmed · **Affected:** Canonical development/assets/asset-library-index.json and generated root pointer

One direct Path.write_bytes overwrite returned OSError errno22; the prior readable index remained stale until successful regeneration.

Cause / uncertainty: Direct canonical-file overwrite failed on Windows; the tool output does not establish which OS reader or filesystem condition caused it.

Fix / next action: Write a temporary sibling, close it and atomically replace the canonical file/pointer. Regeneration and --check pass with1692current GLBs. Preserve the old canonical artifact if replacement fails.

Evidence and verification: tools/index_staged_map_models.py write_atomic; current asset-library-index.json and furnishing index-verification.json; observed OSError errno22 during direct overwrite in this authoring session

Older-map obligation: Publish generated source indexes atomically and retain authoritative prior bytes on failure. This changes no asset or runtime geometry.

### DEV-373 — Rectangular stair treads overlap inside curved lighthouse flights and leave outside gaps

**Status:** CURVED JOINS AND FULL TOWER CHAIN FIXED IN NATIVE CANDIDATE; guards/runtime OPEN · **Affected:** WLD-14 native stair candidate and shared opt-in native movement contract

140 curved lighthouse tread solids have overlapping inner corners and visible outer gaps; whole-flight movement fails.

Cause / uncertainty: Each chord was generated as an independently rotated rectangle rather than a ribbon with shared boundaries.

Fix / next action: Shared cyclic miter boundaries join consecutive stair revolutions. Tangent/radial final landing connects lantern floor. All140treads retained at12triangles each; five-turn ground-to-lantern chain now passes actual continuous forward/reverse native sweeps without floor resets. Guards and runtime/story/room integration still open.

Evidence and verification: development/levels/rebuild/verification/roster-landings-2026-11-07/; previous rejected rectangle/first miter views retained

Older-map obligation: All other locations and retained Castle/House/Blackpine/Hollow Creek require individual route and join audits. No inferred fix or playable unlock.

### DEV-374 — Float32 shared stair joins can report unsupported micrometre seams

**Status:** BOUNDED OPT-IN JOIN QUERY FIXED; analytic negative controls PASS · **Affected:** WLD-14 native stair candidate and shared opt-in native movement contract

Mitered treads can disagree by several micrometres after separate GLB object transforms; exact shared path endpoints report START_UNSUPPORTED.

Cause / uncertainty: Independent float32 transformed vertices cross an overly strict barycentric edge tolerance.

Fix / next action: Permit at most10micrometres of world-space edge allowance on explicitly named stair components only. 6micrometre seam passes,0.1mm gap and absent tread remain unsupported. Ordinary floor and unannotated-reader tolerances are unchanged; HCS triangles and optical/body geometry remain exact.

Evidence and verification: development/levels/rebuild/verification/roster-stair-movement-2026-11-07/

Older-map obligation: All other locations and retained Castle/House/Blackpine/Hollow Creek require individual route and join audits. No inferred fix or playable unlock.

### DEV-375 — Turn landings overhang the incoming stair flight and block standing movement

**Status:** SCOPED NATIVE REPAIR VERIFIED; full map/runtime acceptance remains open · **Affected:** COLLEGE, MANSION, WLD-14 and shared full-source route audit

CollegeL84 and MansionL59/L60 fail ascent against the landing slab, and descent lacks foot support.

Cause / uncertainty: The sideways landing full width extends behind the flight endpoint over its final treads; schematic endpoint joins are not a usable turn.

Fix / next action: Set back each flight endpoint0.75m and author a straight landing before the sideways turn, preserving original clear widths/rooms. All complete links and actual connected College basement/upper and Mansion basement/service/upper chains pass native sweeps both directions. Three native blend/GLB/HCS2 composites regenerated; source rough placements unchanged.

Evidence and verification: development/levels/rebuild/verification/roster-landings-2026-11-07/

Older-map obligation: Retained Castle/House/Blackpine/Hollow Creek and every remaining site require individual physical route/landing/guard/owner audits. These checks do not certify unevaluated maps, services, networking or finished art.

### DEV-376 — Cyclic stair end boundaries and missing final landing interrupt lighthouse ascent

**Status:** SCOPED NATIVE REPAIR VERIFIED; full map/runtime acceptance remains open · **Affected:** COLLEGE, MANSION, WLD-14 and shared full-source route audit

Nine individual lighthouse endpoint segments and allfive full spiral links remain blocked after the first mitered-tread revision.

Cause / uncertainty: First/last offsets use separate tangent normals, leaving a cyclic gap; final upper floor lacks an authored tangent/radial landing.

Fix / next action: Use shared averaged cyclic end normals and append actual tangent/radial lantern landing coordinates. Allfive revolutions and the connected142-segment chain pass1.8m/.22m production movement forward/reverse. Exported support/solids checked; no hidden ramp or collision waiver added.

Evidence and verification: development/levels/rebuild/verification/roster-landings-2026-11-07/

Older-map obligation: Retained Castle/House/Blackpine/Hollow Creek and every remaining site require individual physical route/landing/guard/owner audits. These checks do not certify unevaluated maps, services, networking or finished art.

### DEV-377 — Flight-only chain audit omits flat landing segments and resets at link boundaries

**Status:** SCOPED NATIVE REPAIR VERIFIED; full map/runtime acceptance remains open · **Affected:** All25candidate route manifests and connected stair checks

Previous82contiguous vertical chains do not prove intermediate flat landings or travel across an entire multi-link stair.

Cause / uncertainty: The input manifest filtered level segments before joining, and separate link calls initialized a fresh feet pose.

Fix / next action: Audit all84active vertically changing source links with every intermediate flat segment; also carry actual feet through seven maximal unique exact-endpoint chains.44links/three chains pass,40links/four chains remain failing and recorded. Failed source routes are not omitted or promoted.

Evidence and verification: development/levels/rebuild/verification/roster-landings-2026-11-07/

Older-map obligation: Retained Castle/House/Blackpine/Hollow Creek and every remaining site require individual physical route/landing/guard/owner audits. These checks do not certify unevaluated maps, services, networking or finished art.

### DEV-378 — Regenerated native scaffold next-step notes retain superseded sloped-mass instructions

**Status:** CURRENT25HANDOFFS UPDATED; generator carry-forward remains OPEN · **Affected:** COLLEGE, MANSION, WLD-14 structure NEXT.md and future scaffold regenerations

Generated notes can still say that tower stairs are only sloped masses after actual140treads and connected native sweeps have been delivered.

Cause / uncertainty: The scaffold generator copies historical source refine bullets without reconciling later native implementation evidence.

Fix / next action: All25current NEXT/workflow notes now reflect measured source results and owner terrain-first priority. Shared scaffold still copies historical refine notes on later regeneration; fix during the terrain-generator pass without treating them as runtime evidence.

Evidence and verification: development/levels/rebuild/verification/roster-landings-2026-11-07/; structures/{college,mansion,wld-14}/NEXT.md; previous fe7f4972 notes

Older-map obligation: Inspect each older map handoff against its actual current source and evidence; a regenerated planning note is not proof of a remaining runtime defect or of completed work.

### DEV-379 — Legacy advisory stair run/riser fields lag the actual authored coordinate revision

**Status:** KNOWN3DIMENSION HINTS CORRECTED; rest-of-roster metadata audit OPEN · **Affected:** COLLEGE L84, MANSION L59/L60 and retrospective roster stair dimension metadata

Changed turn-landing paths retain inherited run_m/risers values; CollegeL84 advertises4.8m/18risers while its current flight coordinates span4.65m and actual native treads number17.

Cause / uncertainty: The authored geometry builder derives steps from point coordinates, while inherited schematic dimension hints are independent fields and were not normalized during path revision.

Fix / next action: Coordinated native regeneration normalizes CollegeL84/MansionL59/L60run/riser hints to their actual point geometry and exported tread counts. Preserve exact source geometry as authority; other inherited metadata still requires individual review.

Evidence and verification: development/levels/rebuild/{college,mansion}/rough-layout.json; structures/{college,mansion}/physics-contract.json; verification/roster-landings-2026-11-07/

Older-map obligation: Compare each older source metadata row to its actual built geometry before consuming it for runtime/AI/service authoring. No global metadata correctness inferred from tested native paths.

### DEV-380 — Terrain sampling uses immutable seed-layout cuts instead of current authored basement entries

**Status:** CURRENT CUT DATA IMPLEMENTED; selected worlds verified · **Affected:** WLD-07/WLD-07-MORTUARY and shared terrain/export/route authoring; explicit per-site results in evidence

Saved terrain could retain requests from an earlier layout after stair/foundation revisions.

Cause / uncertainty: The sampler used the seed layout for both stable height sampling and mutable foundation/stair cut requests.

Fix / next action: Keep seed mesh/heights/coastline immutable, take current zone/link cuts separately and fingerprint the cut definition without the derived world binding. Both cemetery worlds persist/reopen current punches; older world cut definitions require individual audits.

Evidence and verification: development/levels/rebuild/verification/roster-basements-2026-11-07/

Older-map obligation: Retained Castle/House/Blackpine/Hollow Creek require individual source/terrain/route audits. No playable availability, services, guards, final terrain, visuals or complete mission acceptance is inferred for unaudited maps.

### DEV-381 — Base cemetery lacks a terrain source and profile for saved-world integration

**Status:** SHARED CEMETERY SOURCE AUTHORED; final terrain acceptance OPEN · **Affected:** WLD-07/WLD-07-MORTUARY and shared terrain/export/route authoring; explicit per-site results in evidence

Base cemetery terrain sampling fails with no ground mesh; an attached saved world alone is not imported by the scaffold without a terrain contract.

Cause / uncertainty: The older base packet omitted terrain while its mortuary annex already carried a same-world cemetery landform.

Fix / next action: Explicitly sample the annex terrain at its immutable source SHA, cropped to base bounds. All13original cemetery zone coordinates match at that seed. Add the actual cemetery profile, import saved physical triangles, never silently substitute a flat or unrelated map. Invalid donor rejects and preserves18world files.

Evidence and verification: development/levels/rebuild/verification/roster-basements-2026-11-07/

Older-map obligation: Retained Castle/House/Blackpine/Hollow Creek require individual source/terrain/route audits. No playable availability, services, guards, final terrain, visuals or complete mission acceptance is inferred for unaudited maps.

### DEV-382 — Cemetery ossuary stairs cross vestibule walls before reaching the basement floor

**Status:** NATIVE BASEMENT LINKS AND DECLARED CIRCULATION LOOPS VERIFIED; runtime OPEN · **Affected:** WLD-07/WLD-07-MORTUARY and shared terrain/export/route authoring; explicit per-site results in evidence

Both cemetery/annex routes hit west/east vestibule walls aroundY-0.9, while destination floor isY-3.

Cause / uncertainty: A single schematic diagonal runs through the partition midway through the descent.

Fix / next action: Allfour basement links pass continuous ascent/descent. Both14-segment source circulation loops include approaches, descent, passage, ascent and surface return; forward/reverse PASS in structural and furnished variants.

Evidence and verification: development/levels/rebuild/verification/roster-basements-2026-11-07/

Older-map obligation: Retained Castle/House/Blackpine/Hollow Creek require individual source/terrain/route audits. No playable availability, services, guards, final terrain, visuals or complete mission acceptance is inferred for unaudited maps.

### DEV-383 — Native collision exporter overwrites its previous payload before terrain validation rejects a candidate

**Status:** PREVALIDATE AND STAGED FILE REPLACEMENT IMPLEMENTED; negative controls PASS · **Affected:** WLD-07/WLD-07-MORTUARY and shared terrain/export/route authoring; explicit per-site results in evidence

Rejected missing-terrain candidate changes the old HCS2 while leaving its old contract behind.

Cause / uncertainty: Authoritative binary write precedes exact saved-terrain triangle/flag validation.

Fix / next action: Build bytes in memory, finish geometry/fidelity assertions, stage binary/metadata and replace files only afterward. Actual legacy66942b34 negative control destroys the previous pair; corrected exporter rejects the same source and preserves both prior byte hashes. This is prevalidation/per-file replacement, not a crash-atomic two-file transaction.

Evidence and verification: development/levels/rebuild/verification/roster-basements-2026-11-07/

Older-map obligation: Retained Castle/House/Blackpine/Hollow Creek require individual source/terrain/route audits. No playable availability, services, guards, final terrain, visuals or complete mission acceptance is inferred for unaudited maps.

### DEV-384 — Moving cemetery stair openings invalidates old approach endpoints and room-centre anchors

**Status:** APPROACHES AND AUTHORITATIVE STANDING RESERVATIONS VERIFIED; runtime mapping OPEN · **Affected:** WLD-07/WLD-07-MORTUARY and shared terrain/export/route authoring; explicit per-site results in evidence

Initial revision preserves vertical routes but creates67qualified flat-route blocks,48on the old entrance approaches.

Cause / uncertainty: The new stair void passes through the former room-centre approach/spawn reservation.

Fix / next action: Four authored upper standing anchors pass literal native walking; fourold room-centre negatives correctly fail. Both complete entrance/surface loops pass; unchanged route comparisons preserve allpreviously qualified samples/sweeps.

Evidence and verification: development/levels/rebuild/verification/roster-basements-2026-11-07/

Older-map obligation: Retained Castle/House/Blackpine/Hollow Creek require individual source/terrain/route audits. No playable availability, services, guards, final terrain, visuals or complete mission acceptance is inferred for unaudited maps.

### DEV-385 — Uncut route slabs expose inward top faces and mark undersides as walking supports

**Status:** ALL25CANDIDATE ROUTE SOLIDS FIXED;1015flat tops independently PASS · **Affected:** WLD-07/WLD-07-MORTUARY and shared terrain/export/route authoring; explicit per-site results in evidence

Independent prior HCS2 inspection finds876of1011flat route solids with inward or nonwalking top faces. Some Boolean-trimmed strips happen to be correct.

Cause / uncertainty: X/-Z authoring reverses original winding; Boolean operations incidentally repair some objects while untouched strips stay inside out.

Fix / next action: Rebuilt25native/furnished scenes. Actual HCS2 audit finds0inward/nonwalking tops among1015flat12cm path solids, compared with876of1011at66942b34. Independent GLB checks validate1025outward closed route solids including10sloping ones. All124rough placements unchanged.

Evidence and verification: development/levels/rebuild/verification/roster-basements-2026-11-07/

Older-map obligation: Retained Castle/House/Blackpine/Hollow Creek require individual source/terrain/route audits. No playable availability, services, guards, final terrain, visuals or complete mission acceptance is inferred for unaudited maps.

### DEV-386 — Local path grading can disturb adjoining route shoulders when overall terrain is unsettled

**Status:** SOURCE NETWORK GRADING VERIFIED; overall terrain acceptance OPEN · **Affected:** Cemetery/annex and shared native map authoring; PRISON specifically for DEV-388

A new terrain shoulder blocks a prior qualified crypt path, and isolated approach grading alters adjacent source routes.

Cause / uncertainty: Coarse2m lattice and unrelated local path strokes do not establish a coherent shared walking network.

Fix / next action: Use actual editable TerrainLayerStack gradePath layers on the connected source network. Raw height PNGs remain byte-identical to the original annex source; punches remain actual topology. Grade programs and documents fingerprinted, append-only updates preserve earlier layers, exact rerun preserves24world files. Owner now prioritizes full-site terrain/elevations and blockout reshuffles before further route refinements.

Evidence and verification: development/levels/rebuild/verification/roster-basements-2026-11-07/; captures/structure-batch/roster-structure-sheet-02.png; captures/basement-batch/

Older-map obligation: Audit other maps individually. Neither physical path probes nor a native overview proves finished terrain, model support, lighting, services, networking or complete map acceptance.

### DEV-387 — Before/after route comparator reuses old feet Y after valid terrain support changes

**Status:** SCENE-SPECIFIC GROUNDING FIXED; independent negative controls PASS · **Affected:** Cemetery/annex and shared native map authoring; PRISON specifically for DEV-388

A supported millimetre-scale terrain change can be reported as a body/sweep obstruction because the after scene is queried at the old ground height.

Cause / uncertainty: The comparator reused one feet vector for two distinct native surfaces.

Fix / next action: Each scene uses its own measured support Y within the original2cm comparison budget; body/footprint/production sweep rules remain unchanged. Exact ground heights emitted in rows. Independent1cm plane fixture reproduces old false positive; new low roof, missing support and excessive-height negatives still fail. Accepted unchanged-route and current prop-route audits have zero new blocks.

Evidence and verification: development/levels/rebuild/verification/roster-basements-2026-11-07/; captures/structure-batch/roster-structure-sheet-02.png; captures/basement-batch/

Older-map obligation: Audit other maps individually. Neither physical path probes nor a native overview proves finished terrain, model support, lighting, services, networking or complete map acceptance.

### DEV-388 — Prison blocking overview shows isolated unsupported boundary panels

**Status:** OPEN; terrain/foundation and enclosure assembly required · **Affected:** Cemetery/annex and shared native map authoring; PRISON specifically for DEV-388

Several exterior wall/barrier panels appear detached from the main prison structure with no visible ground or footing in the native overview.

Cause / uncertainty: Fixed closure blockers were authored without the overall compound terrain/foundation context; exact structural attachment still requires per-panel inspection.

Fix / next action: During terrain-first pass author the compound ground and continuous supporting enclosure/returns, then inspect each panel placement and containment route.192fixed boundary rays do not prove physical support or visual coherence.

Evidence and verification: development/levels/rebuild/verification/roster-basements-2026-11-07/; captures/structure-batch/roster-structure-sheet-02.png; captures/basement-batch/

Older-map obligation: Audit other maps individually. Neither physical path probes nor a native overview proves finished terrain, model support, lighting, services, networking or complete map acceptance.

### DEV-389 — Route reservation surfaces overlap real floor planes in rough native assemblies

**Status:** OPEN; remove coplanar source overlaps during terrain/structure refinement · **Affected:** Cemetery/annex and shared native map authoring; PRISON specifically for DEV-388

Native basement-entry views show a black foreground band; source route/floor surfaces share the same authored walking plane.

Cause / uncertainty: Confirmed coplanar slab/floor geometry; renderer-specific contribution to the visible band still requires separation review.

Fix / next action: Retain actual support while removing overlapping reservation geometry where real floors or accepted terrain already supply the surface. Review native/runtime images and normal/collision parity. Do not conceal the problem with lighting, alpha, a global collision change or a claimed visual pass.

Evidence and verification: development/levels/rebuild/verification/roster-basements-2026-11-07/; captures/structure-batch/roster-structure-sheet-02.png; captures/basement-batch/

Older-map obligation: Audit other maps individually. Neither physical path probes nor a native overview proves finished terrain, model support, lighting, services, networking or complete map acceptance.

### DEV-390 — Thin outside-cut strips exhaust editor topology budget without adding footprint fidelity

**Status:** AUTHORING DECOMPOSITION FIXED; independent2,013,824cell mask parity PASS · **Affected:** 18new macroterrain candidates across remaining roster; exact affected sites/segments in terrain-overall-pass reports

Asylum new terrain generation exceeds20,000cut leaves after the Academy pilot consumed19,595leaves.

Cause / uncertainty: Sequential quarter-metre row strips unnecessarily subdivide fully absent source cells into64tiny leaves.

Fix / next action: Use source-cell aligned dyadic cut rectangles. Exact quarter-metre mask is unchanged; all18worlds fit existing20,000cut limit. No core editor limit raised. Rejected pilot/log retained.

Evidence and verification: terrain-overall-pass/rejected-row-stripes/; verification/source-integrity.json

Older-map obligation: No previous map receives terrain acceptance. Audit each existing terrain and mature map individually. Preserve original source/model IDs, UVs and transforms; re-run actual native and visual checks after each terrain/blockout/foundation revision.

### DEV-391 — Prison construction footprint does not align with editable terrain leaves

**Status:** OPEN PHYSICAL RIMS; explicit excavation envelope authored · **Affected:** 18new macroterrain candidates across remaining roster; exact affected sites/segments in terrain-overall-pass reports

Prison foundation edge46.8m cannot be represented by the quarter-metre editable grid; editor correctly rejects it.

Cause / uncertainty: Existing construction reservations include off-grid dimensions.

Fix / next action: Preserve building dimensions; new macroterrain source explicitly records aligned excavation bounds and every excess in samples/prison/provenance.json. Construct physical foundation rims before promotion; no claim that authoring an enlarged aperture fills the gap.

Evidence and verification: terrain-overall-pass/rejected-row-stripes/unaligned-prison-foundation.log; samples/prison/provenance.json

Older-map obligation: No previous map receives terrain acceptance. Audit each existing terrain and mature map individually. Preserve original source/model IDs, UVs and transforms; re-run actual native and visual checks after each terrain/blockout/foundation revision.

### DEV-392 — New site terrain exposes old flat-route and blockout elevation conflicts

**Status:** OPEN; actual native failures retained, master maps preserved · **Affected:** 18new macroterrain candidates across remaining roster; exact affected sites/segments in terrain-overall-pass reports

87,060flat-route samples retain79,235baseline-valid poses, but2,453poses and2,355qualified sweeps become blocked in new macroterrain composites. Ten sites have new findings; eight have none in this scoped check.

Cause / uncertainty: Historical routes and building groups were authored without coherent whole-site landform. Broad slopes/rock shoulders now physically meet or overtop those coordinates.

Fix / next action: Revise terrain/blockout relationships in stage batches, moving structures/stair bays/links with shared coordinate bindings where needed. Do not force terrain around every orange pad or weaken physics. Newly authored composite candidates are not promoted to existing masters or unlocked. Current independent vertical comparison also exposes WLD-03/L13/segment0: the older furnished source passed but both terrain-before-bearing and terrain-after-bearing fail, leaving185/221vertical segments and47/84complete links passing. No new bearing regression; exact72failed directional poses retained.

Evidence and verification: terrain-overall-pass/verification/native-route-fit.json and native-route-probes.tsv; captures/macroterrain-batch/

Older-map obligation: No previous map receives terrain acceptance. Audit each existing terrain and mature map individually. Preserve original source/model IDs, UVs and transforms; re-run actual native and visual checks after each terrain/blockout/foundation revision.

### DEV-393 — Larger macroterrain sites exceed the single editor mesh export area

**Status:** CHUNKED AUTHORING VERIFIED; runtime location integration OPEN · **Affected:** 18new macroterrain candidates across remaining roster; exact affected sites/segments in terrain-overall-pass reports

Prison terrain rectangle exceeds2,048source cells when published as one terrain mesh.

Cause / uncertainty: The single-mesh editor API intentionally bounds source area; roster terrain needs multiple owned chunks.

Fix / next action: Publish real64m source-aligned chunks through TerrainScenePackage, inspect reopened ownership/fingerprints and encode all nodes through actual TerrainSceneNativePhysics. No global export limit raised; composite GLB and native triangles match exactly.

Evidence and verification: terrain-overall-pass/rejected-row-stripes/single-chunk-prison-limit.log; verification/source-integrity.json; worlds/prison/terrain-world/scene-package/

Older-map obligation: No previous map receives terrain acceptance. Audit each existing terrain and mature map individually. Preserve original source/model IDs, UVs and transforms; re-run actual native and visual checks after each terrain/blockout/foundation revision.

### DEV-394 — Macroterrain composites expose unfinished supports, rock enclosure and angular boundaries

**Status:** OPEN; construction and landscape fitness not accepted · **Affected:** 18new macroterrain candidates across remaining roster; exact affected sites/segments in terrain-overall-pass reports

Native full-site views show unsupported marsh/harbour decks, cave shelves and canopy-like cave room slabs; boundaries and drainage cross-sections remain coarse and angular, canonical van missing.

Cause / uncertainty: First broad ground surfaces have been composed with earlier rough structural exports; piles, foundations, cave mass, water/banks and location-specific enclosure were not yet built.

Fix / next action: Build actual load-bearing/pile/retaining/cave volumes and supported van arrival. Revise broad landform silhouettes and contour cross-sections, not just edge colours. Current six sheets were viewed; construction2/5, landscape2/5, finish1/5, no visual acceptance. Bearing batch now adds1811closed parts (1646masonry skirts,152piles,13connected beams) across25candidates; exact contact/export/native checks pass with zero new flat or vertical regressions. Seven pre-existing terrain candidates now individually reviewed and all remain REVISE; full landscape/construction acceptance remains open.

Evidence and verification: captures/macroterrain-batch/macroterrain-sheet-01.png through06.png; terrain-overall-pass/verification/visual-review.json

Older-map obligation: No previous map receives terrain acceptance. Audit each existing terrain and mature map individually. Preserve original source/model IDs, UVs and transforms; re-run actual native and visual checks after each terrain/blockout/foundation revision.

### DEV-395 — Support placement under a deck reservation can leave detached posts without a real deck mesh

**Status:** SOURCE RULE FIXED;17actual missing-receiver negatives reproduced · **Affected:** Remaining25native rebuild candidates; exact affected contacts in terrain-fit-pass/*/construction.json

Initial marsh pilot puts posts at corners of reserved boardwalk rectangles where no constructed deck exists.

Cause / uncertainty: Source zone footprint was treated as a receiving structure instead of tracing actual exported underside geometry.

Fix / next action: Require an actual downward-facing source underside for every support. Use real route/deck positions for piles and explicit connected bearing beams for displaced piles; verify native terrain contact and closed outward geometry independently.25finalexports pass; unsupported reservations remain withheld, not accepted.

Evidence and verification: terrain-fit-pass/rejected/marsh-reservation-posts.json; captures/terrain-fit-batch/rejected-marsh-reservation-posts.png; verification/bearing-contact.json

Older-map obligation: Four mature maps retain individual audit obligations. No source master, saved terrain punch or reusable model was overwritten. Passing contact/native regression checks do not establish complete terrain, structures, visual, runtime/network or hardware acceptance.

### DEV-396 — Masonry skirt can attach incidentally to a crossing route rather than its planned structural floor

**Status:** SOURCE CLASSIFICATION FIXED; real deck construction still OPEN · **Affected:** Remaining25native rebuild candidates; exact affected contacts in terrain-fit-pass/*/construction.json

Wharf quay reservation produced a masonry strip whose declared receiver was L02support rather than a constructed quay floor.

Cause / uncertainty: Source reservation perimeter overlapped a narrow old route strip; a single underside ray did not establish that the intended floor existed.

Fix / next action: Masonry now requires an actual Floor component receiver; piles may explicitly support real deck/path meshes. Exact declared mesh is queried independently to resolve coplanar BVH tie order without pretending duplicates are removed. Rejected wharf GLB/manifest retained.

Evidence and verification: terrain-fit-pass/rejected/wharf-skirt-on-route.glb and .json; verification/bearing-contact.json; DEV-389

Older-map obligation: Four mature maps retain individual audit obligations. No source master, saved terrain punch or reusable model was overwritten. Passing contact/native regression checks do not establish complete terrain, structures, visual, runtime/network or hardware acceptance.

### DEV-397 — Failure-contact diagnostic silently excludes new before/after variant names

**Status:** SOURCE FIX VERIFIED; missing-phase negative rejects and preserves prior output · **Affected:** Remaining25native rebuild candidates; exact affected contacts in terrain-fit-pass/*/construction.json

First bearing comparison reports zero diagnosed failures despite36failed vertical segments in the actual movement result.

Cause / uncertainty: Diagnostic filter was hardcoded to structure; new before_bearing/after_bearing rows were all skipped.

Fix / next action: Accept an explicit selected variant, require at least one selected input row and pass after_bearing from the driver. Actual final72failed directional poses diagnosed. Missing-variant control rejects and leaves prior output byte-identical. Native movement assertions were never changed.

Evidence and verification: terrain-fit-pass/verification/stairs/failure-contacts.tsv; verification/empty-variant-negative.json; tools/java/RosterFailureContacts.java

Older-map obligation: Four mature maps retain individual audit obligations. No source master, saved terrain punch or reusable model was overwritten. Passing contact/native regression checks do not establish complete terrain, structures, visual, runtime/network or hardware acceptance.

### DEV-398 — Exterior deck and walkway reservations lack continuous constructed mesh

**Status:** OPEN; individually recorded across25support candidates · **Affected:** Remaining25native rebuild candidates; exact affected contacts in terrain-fit-pass/*/construction.json

Native inspections show missing portions of wharf/quay/pier, motor-lodge walkways, keeper porch and marsh boardwalk reservations; existing cyan strips supply only narrower crossing routes.

Cause / uncertainty: Structural rough generation omitted exterior floors unless explicitly constructed; zone rectangles and old route plates do not define complete physical decks.

Fix / next action: Author actual measured constructed decks and their load-bearing connections with continuous clearances/openings. Use rectangles only for real deck objects; do not stamp a whole outdoor reservation into terrain. Revalidate all footprints, floor/underface depths, piles, actual native movement and canonical arrival.989withheld source contacts remain explicit in this pass, including missing ground and terrain-overfloor cases.

Evidence and verification: terrain-fit-pass/existing-terrain-review.json; */construction.json; captures/terrain-fit-batch/; README.md

Older-map obligation: Four mature maps retain individual audit obligations. No source master, saved terrain punch or reusable model was overwritten. Passing contact/native regression checks do not establish complete terrain, structures, visual, runtime/network or hardware acceptance.

### DEV-399 — Distance fog bypassed by OpenGL and native-only far-haze selection

**Status:** SHARED COMPOSITOR FIX VERIFIED LOCALLY; exact-source CI/release acceptance separate · **Affected:** Shared Castle/investigation rendering, including custom levels and future integrated native packets

Software triangles fade independently while OpenGL geometry bypasses the fog; only native-site render calls apply additional far haze.

Cause / uncertainty: Atmosphere was embedded in one raster path and one map classification instead of the completed world-depth composition shared by Castle and investigation levels.

Fix / next action: Use one shared radial distance law and immutable per-frame policy for software fragments, OpenGL fragment-shader uniforms, water, directional sprites, safe-area shells and foot shadows before alpha blending. Heavy baseline clear6m/fullyopaque24m; weather/mission fog can thicken it. Preserve depth and near colours; no per-map opt-in. Remove native-only haze.10focused checks, labeled synthetic frame-contract control, paired actual GPU shader controls and52actual runtime frames pass across12sources. Final surface-depth implementation supersedes the initial final-frame composite approach.

Evidence and verification: src/main/java/org/beckenburg/LevelAtmosphere.java; SceneRenderer.java; development/levels/rebuild/verification/atmosphere-all-levels/; captures/atmosphere-all-levels/

Older-map obligation: All12currently loadable runtime sources captured in software and real OpenGL. Remaining17rebuild packets inherit the common rendering path when integrated; this does not unlock them or establish complete terrain, physical boundary, gameplay/network or performance acceptance.

### DEV-400 — Hard-painted lower horizon differs from distant world fog colour

**Status:** SHARED SKY HORIZON FIX VERIFIED LOCALLY; physical edge work remains OPEN · **Affected:** Shared Castle/investigation rendering, including custom levels and future integrated native packets

Visible empty space beyond map geometry exposes artificial dark hill bands and a different colour from the fogged far surfaces.

Cause / uncertainty: The lower sky painted independent opaque sinusoidal hills, while software geometry blended toward another fixed colour.

Fix / next action: Keep the existing world-anchored weather skybox/clouds and smoothly blend its horizon/lower hemisphere to the same weather-aware fog colour. Actual geometry occludes the background; sky/fog never write depth. Viewed all52software/OpenGLframes and5comparison sheets; weather sky anchoring and all tested sky/water/depth controls pass; final fog is evaluated at each receiving surface before transparency composition.

Evidence and verification: LevelAtmosphere.skyboxHaze; SceneRenderer.paintSky; verification/atmosphere-all-levels/capture-manifest.json

Older-map obligation: All12currently loadable runtime sources captured in software and real OpenGL. Remaining17rebuild packets inherit the common rendering path when integrated; this does not unlock them or establish complete terrain, physical boundary, gameplay/network or performance acceptance.

### DEV-401 — Relative JAR packaging target can fail final move from restricted system temp

**Status:** WORKSPACE-LOCAL PACKAGE TARGET FIX; final package check recorded separately · **Affected:** Shared Castle/investigation rendering, including custom levels and future integrated native packets

build.cmd package compiles/resources successfully but jartool final rename from sandbox system Temp to relative build/beckenburg-game.jar is denied, removing the previous output.

Cause / uncertainty: Relative JAR target and default temporary directory select a protected/cross-sandbox temp location during replacement.

Fix / next action: Preserve the previous terrain-authoring dependency JAR under ignored build, use an absolute workspace JAR target and explicit workspace-local JVM temp in the packaging command. Recover completed package with those arguments and verify packaged audio/classes. This is a local packaging reliability fix, not installer acceptance.

Evidence and verification: build.cmd package_jar_only; verification/atmosphere-all-levels/packaging logs/receipt

Older-map obligation: All12currently loadable runtime sources captured in software and real OpenGL. Remaining17rebuild packets inherit the common rendering path when integrated; this does not unlock them or establish complete terrain, physical boundary, gameplay/network or performance acceptance.

### DEV-402 — Final-frame fog can erase nearby translucent objects against a distant background

**Status:** FIX VERIFIED: surface-depth fog before alpha in both renderers and world layers · **Affected:** Initial atmosphere checkpoint862b65c2; shared world rendering and translucent ghost/material effects

A nearby materializing ghost below220alpha retains background opaque depth; a final-depth fog pass can erase it using the distant wall depth.

Cause / uncertainty: Post-composition fog has lost each translucent layer's own depth and cannot reconstruct correct foreground/background mixing.

Fix / next action: Replace image post-fog with scoped shared per-surface policy. Software/GPU fog each fragment before blend; water, sprites, safe-area shells and foot shadows use their actual surface depth. Keep final depth inspection read-only. Real128-alpha ghost control passes near/far and unchanged depth; actual OpenGL fog-off/fog-on plane has exact fog colour and bit-identical depth. Legacy frame constructors preserve raw/studio callers; runtime policy is explicit. Register LevelAtmosphereTest in the full regression manifest (371entries; complete suite not claimed run).

Evidence and verification: src/main/java/org/beckenburg/LevelAtmosphere.java; DirectionalSprites.java; GpuWorldRenderer.java; gpu-client/src/main/java/org/beckenburg/gpu/OpenGlWorldRenderer.java; verification/atmosphere-all-levels/

Older-map obligation: All runtime maps share the same fix; source-only maps inherit on integration. Full map, physical terrain/boundary, installed multiplayer and performance acceptance remain open.

### DEV-403 — Sequential coplanar deck Boolean leaves open or zero-thickness pieces

**Status:** FIX VERIFIED: projected convex clipping and independently closed native export · **Affected:** Five physical deck candidates WLD06/11/14/15/16; shared offline authoring/audit tools

The first marsh deck pilot failed strict solid checks at intersecting deck/route boundaries.

Cause / uncertainty: Blender Boolean on exact/near-coplanar imported route solids left flat/sliver surfaces.

Fix / next action: Use deterministic convex polygon difference on actual source top triangles, retain original sources, remove only recorded sub0.1mm boundary residues; independent closed/outward/native coverage checks pass. Preserve rejected pilot log.

Evidence and verification: development/levels/rebuild/deck-fit-pass/verification/; tools/build_roster_decks.py; tools/check_roster_decks.py

Older-map obligation: All29roster decisions explicit; other24 sources retained individually, no unaudited older-map acceptance. Preserve true basement holes,40-space envelope, shared authored coordinates and canonical van contract.

### DEV-404 — Collinear projected deck seam vertices export degenerate native triangles

**Status:** FIX VERIFIED: authored polygon cleanup; unchanged production parser threshold · **Affected:** Five physical deck candidates WLD06/11/14/15/16; shared offline authoring/audit tools

First deck native movement invocation rejected a zero-area face.

Cause / uncertainty: Projected seam vertices remained collinear through prism fan triangulation and float export.

Fix / next action: Clean projected collinear construction vertices before creating/exporting plates. Exact native parser/nondegenerate checker and current production capsule/flight audits pass; production physics tolerance unchanged. Preserve rejection log.

Evidence and verification: development/levels/rebuild/deck-fit-pass/verification/; tools/build_roster_decks.py; tools/check_roster_decks.py

Older-map obligation: All29roster decisions explicit; other24 sources retained individually, no unaudited older-map acceptance. Preserve true basement holes,40-space envelope, shared authored coordinates and canonical van contract.

### DEV-405 — Deck support receipts lose part names when original bearing names already exist

**Status:** FIX VERIFIED: map each generated object directly to its proof row · **Affected:** Five physical deck candidates WLD06/11/14/15/16; shared offline authoring/audit tools

Independent checker found missing support proofs despite real exported supports.

Cause / uncertainty: Blender appends .001 to reused names but foundation generator proof used requested logical names.

Fix / next action: Pair generated parts and proof rows before assigning unique Deck bearing identities. Independent expected new-mesh set equals every proof and every named upper/lower bearing contact passes.

Evidence and verification: development/levels/rebuild/deck-fit-pass/verification/; tools/build_roster_decks.py; tools/check_roster_decks.py

Older-map obligation: All29roster decisions explicit; other24 sources retained individually, no unaudited older-map acceptance. Preserve true basement holes,40-space envelope, shared authored coordinates and canonical van contract.

### DEV-406 — Exterior deck framing, edge guards and terrain transitions remain incomplete

**Status:** OPEN: closed walking plates added across five sites, final structure/terrain refinement required · **Affected:** Five physical deck candidates WLD06/11/14/15/16; shared offline authoring/audit tools

WLD06/11/14/15/16 had large authored pier/boardwalk/porch/walk envelopes with only narrow route strips; new plates still lack complete intermediate framing/guards.

Cause / uncertainty: Exterior slab generation was omitted for outside zones; prior bearing generation correctly withheld supports without actual deck undersides.

Fix / next action: Fifteen authored envelopes now have physical0.16m closed slabs, exact stair cutouts and84new native-grounded bearing parts. Quarter-metre coverage independently proves24796newly supported samples,11564retained source top samples; full terrain/guard/frame and gameplay acceptance remain open.

Evidence and verification: development/levels/rebuild/deck-fit-pass/verification/; tools/build_roster_decks.py; tools/check_roster_decks.py

Older-map obligation: All29roster decisions explicit; other24 sources retained individually, no unaudited older-map acceptance. Preserve true basement holes,40-space envelope, shared authored coordinates and canonical van contract.

### DEV-407 — Independent deck audit misclassifies tiny closed plates and shared top boundaries

**Status:** CHECKER FIX VERIFIED: translated double-precision volume and paired interior edge probes · **Affected:** Offline physical exterior deck verification; no production collision tolerance change

World-origin float32 tetrahedra gave a false negative volume of -2.1484990914661523e-05 for closed WLD11 exterior service plate81; quarter-metre grid rays on an exact diagonal seam hit both disjoint tops.

Cause / uncertainty: Cancellation in global-coordinate single-precision volume accumulation and treating shared polygon boundaries as interior overlap.

Fix / next action: Translate all tetrahedra to a local vertex and accumulate in Python double precision; retain the closed edge count and positive-volume requirement. For a shared top hit, probe both interiors at0.2mm offsets and reject actual overlapping interiors. Actual five-site export/contact/coverage checks pass; production body/support/depth tolerances untouched.

Evidence and verification: tools/check_roster_decks.py; development/levels/rebuild/deck-fit-pass/verification/export-contact-coverage.json; independent-export.log

Older-map obligation: Retain prior independent map audit obligations. This is an audit numerical fix, not automatic terrain or map acceptance; rerun actual artifacts per site.

### DEV-408 — Terrain excavation omits elevated and deeper occupied room volumes

**Status:** FIX VERIFIED IN FIVE CANDIDATES; overall terrain/cave/runtime gates open · **Affected:** Native terrain/structure construction candidates WLD03/04/09/14/17 and shared offline authoring/import/audit pipeline

4749half-metre occupied-height terrain samples across13rooms in five actual native scenes.

Cause / uncertainty: The cut programs only excavated rooms spanning world Y=0; the dam rooms lie below0 and cave/lookout rooms above0.

Fix / next action: Measure actual terrain triangles inside room envelopes at every elevation and author real persistent room excavations. Four original saved height PNGs/worlds retained, one explicitly seeded battery world added. After audit clear across25candidates;1237flat poses recovered, zero new movement regressions, stair/link gains verified.

Evidence and verification: development/levels/rebuild/terrain-occupancy-pass/; development/levels/rebuild/captures/room-terrain-batch/; tools/append_static_glb.py; tools/check_room_terrain.py

Older-map obligation: All29packets retained; other20candidate geometry unchanged and four mature maps retain individual retrofit obligations. No runtime unlock or complete map acceptance; preserve40space envelope, canonical van, power/hiding/story, original props/UVs/pivots and real basement punches.

### DEV-409 — Conservative circular tower punch leaves earth inside the wall

**Status:** FIX VERIFIED: actual stepped editor cut and physical retaining collar · **Affected:** Native terrain/structure construction candidates WLD03/04/09/14/17 and shared offline authoring/import/audit pipeline

Morrow tower retained12sampled interior earth contacts; first contained-cell revision left6.

Cause / uncertainty: The circular cut used an artificial inset and only fully contained quarter-metre cells.

Fix / next action: Punch cells intersecting the actual circular footing; add50closed curved retaining collar parts at real source soil, reserve circulation and existing unsupported joins. Actual room samples clear and exported/native solids pass. Final collar texture/edge and whole landscape acceptance remain open.

Evidence and verification: development/levels/rebuild/terrain-occupancy-pass/; development/levels/rebuild/captures/room-terrain-batch/; tools/append_static_glb.py; tools/check_room_terrain.py

Older-map obligation: All29packets retained; other20candidate geometry unchanged and four mature maps retain individual retrofit obligations. No runtime unlock or complete map acceptance; preserve40space envelope, canonical van, power/hiding/story, original props/UVs/pivots and real basement punches.

### DEV-410 — Diagonal stair AABBs remove unrelated room-floor and cap regions

**Status:** BATCH REPAIR VERIFIED: six candidate sites; legacy producer migration and older-map audit OPEN · **Affected:** Native terrain/structure construction candidates WLD03/04/09/14/17 and shared offline authoring/import/audit pipeline

Excavation exposed4new blocked poses and2failed sweeps where the lookout ruin floor had been removed by an unrelated diagonal flight bounding box.

Cause / uncertainty: Shared structural_pass.deck_holes reserves each entire axis-aligned flight rectangle instead of its actual shaft footprint.

Fix / next action: Restore the ruin gap only outside exact diagonal flight polygons and source walking tops with12closed0.16m plates. Final all25comparison has zero new blocks and no passing stairs/links/chains regress. Preserve rejected raw probes. Next broad batch must correct exact floor/cap shaft reservations across all25candidates and individually audit older mature maps. Subsequent floor/cap pass audits25current candidates and adds296closed parts/9816triangles restoring580.964m² across6sites, retaining old source geometry. Joined exact polygons preserve all baseline passing stairs/links/chains/loops and recover248flat poses. The legacy broad-bounds helper remains for frozen producers; its migration and mature-source audits are not claimed complete.

Evidence and verification: development/levels/rebuild/terrain-occupancy-pass/; development/levels/rebuild/captures/room-terrain-batch/; tools/append_static_glb.py; tools/check_room_terrain.py

Older-map obligation: All29packets retained; other20candidate geometry unchanged and four mature maps retain individual retrofit obligations. No runtime unlock or complete map acceptance; preserve40space envelope, canonical van, power/hiding/story, original props/UVs/pivots and real basement punches.

### DEV-411 — Re-exporting retained GLB can shift an existing stair vertex

**Status:** FIX VERIFIED: append new static resources without re-exporting retained geometry · **Affected:** Native terrain/structure construction candidates WLD03/04/09/14/17 and shared offline authoring/import/audit pipeline

Independent source preservation rejected L11flight0step12 at the lookout: a tiny position shift crossed the0.1mm rounding bin.

Cause / uncertainty: Another Blender glTF import/export decomposes and recomposes the retained mesh transform.

Fix / next action: Append selected new terrain/repair geometry to the original binary/resource arrays, preserving original meshes/accessors/UV/image/material data. Only explicit old terrain node mesh references are disabled. Actual source mesh/UV/image/transform and exact retained resource checks pass; no tolerance widened.

Evidence and verification: development/levels/rebuild/terrain-occupancy-pass/; development/levels/rebuild/captures/room-terrain-batch/; tools/append_static_glb.py; tools/check_room_terrain.py

Older-map obligation: All29packets retained; other20candidate geometry unchanged and four mature maps retain individual retrofit obligations. No runtime unlock or complete map acceptance; preserve40space envelope, canonical van, power/hiding/story, original props/UVs/pivots and real basement punches.

### DEV-412 — Disconnected retained GLB terrain node can still import as geometry

**Status:** FIX VERIFIED: remove mesh binding on explicitly replaced terrain nodes · **Affected:** Native terrain/structure construction candidates WLD03/04/09/14/17 and shared offline authoring/import/audit pipeline

Independent export checker found the old terrain mesh present after first static append, despite removal from scene root lists.

Cause / uncertainty: The importer can enumerate orphaned glTF nodes independently of default-scene reachability.

Fix / next action: Remove the mesh reference from explicitly replaced terrain nodes while preserving other source nodes and all immutable original binary resources. Repeated actual-export room audit now clears all recorded volumes, and exactly one original terrain mesh is replaced per site.

Evidence and verification: development/levels/rebuild/terrain-occupancy-pass/; development/levels/rebuild/captures/room-terrain-batch/; tools/append_static_glb.py; tools/check_room_terrain.py

Older-map obligation: All29packets retained; other20candidate geometry unchanged and four mature maps retain individual retrofit obligations. No runtime unlock or complete map acceptance; preserve40space envelope, canonical van, power/hiding/story, original props/UVs/pivots and real basement punches.

### DEV-413 — Battery native room view has unresolved black floor regions

**Status:** OPEN: inspect actual floor/shaft geometry and material/normal state · **Affected:** Native terrain/structure construction candidates WLD03/04/09/14/17 and shared offline authoring/import/audit pipeline; WLD04ruin dark interior floor region in paired threshold native inspection (cause unverified)

The after native battery first-person frame exposes black rectangular and polygonal floor regions after earth is removed.

Cause / uncertainty: Not yet established; could include authored stair openings or malformed/unrelated floor reservations/surfaces.

Fix / next action: Retain paired intact PNG and actual source hashes. Inspect the exact exported floors, openings, normals/materials and native body/light behavior in the next roster-wide floor/shaft pass; do not infer that this image is accepted or that all black areas share one cause.

Evidence and verification: development/levels/rebuild/terrain-occupancy-pass/; development/levels/rebuild/captures/room-terrain-batch/; tools/append_static_glb.py; tools/check_room_terrain.py; WLD04ruin paired threshold views also show a residual dark L-shaped interior region; distinguish floor coverage from lighting/occlusion before visual acceptance.

Older-map obligation: All29packets retained; other20candidate geometry unchanged and four mature maps retain individual retrofit obligations. No runtime unlock or complete map acceptance; preserve40space envelope, canonical van, power/hiding/story, original props/UVs/pivots and real basement punches.

### DEV-414 — Convex clipping creates excessive structural fragments around circular floors

**Status:** REJECTED FIRST STUDY; compact planar restoration verified · **Affected:** Source-only structural floor/cap construction across25remaining candidates, with six actual repair sites and four mature sources retained

The initial lighthouse floor restoration emitted9905parts/116128triangles for22.55m² before joined-turn correction.

Cause / uncertainty: Subtracting disjoint triangles still split polygons along infinite edge lines; independent prisms retained every fragment.

Fix / next action: Reject disjoint cutter intersections before splitting, union constrained planar patches, dissolve only coplanar seams and extrude edge-connected regions. Final whole6site batch296parts/9816triangles includes joined spiral headroom. Triangle/part counts are geometry evidence, not sustained FPS acceptance.

Evidence and verification: development/levels/rebuild/floor-closure-pass/verification/; tools/build_roster_floor_closures.py; tools/planar_structure.py; structural_pass.deck_openings

Older-map obligation: All29roster decisions explicit. Nineteenzero-addition results do not establish complete map fitness. Preserve source assets, exact native earth punches,40space envelope and canonical van/power/hiding/story; remaining35stairsegments/35links, legacy producer migration, mature-source and actual runtime/network/hardware gates remain.

### DEV-415 — Merged planar floor patches can have pinched solids and degenerate triangulation

**Status:** FIX VERIFIED: closed edge-connected regions and zero-area cleanup · **Affected:** Source-only structural floor/cap construction across25remaining candidates, with six actual repair sites and four mature sources retained

A merged circular plate had25nonmanifold vertical edges at point contacts; the native reader also rejected repeated-vertex triangles in a merged lookout floor.

Cause / uncertainty: Regions touching only at a point shared extruded vertical edges; dissolved planar bridge loops produced zero-area triangles.

Fix / next action: Extrude edge-connected planar regions independently, explicitly triangulate and remove zero-area planar faces before building thickness. Actual exported solids/native reader pass with exact edge closure and0.16m volume/area thickness. Production parser and collision tolerances unchanged; preserve rejected native log.

Evidence and verification: development/levels/rebuild/floor-closure-pass/verification/; tools/build_roster_floor_closures.py; tools/planar_structure.py; structural_pass.deck_openings

Older-map obligation: All29roster decisions explicit. Nineteenzero-addition results do not establish complete map fitness. Preserve source assets, exact native earth punches,40space envelope and canonical van/power/hiding/story; remaining35stairsegments/35links, legacy producer migration, mature-source and actual runtime/network/hardware gates remain.

### DEV-416 — Independent flight rectangles leave spiral-turn headroom wedges

**Status:** FIX VERIFIED: joined authored intermediate corridor corners · **Affected:** Source-only structural floor/cap construction across25remaining candidates, with six actual repair sites and four mature sources retained

First closure draft regressed80previously passing lighthouse stair segments in both directions, fivecomplete links and their automatic chain; the independent standing-body sample also failed.

Cause / uncertainty: Adjacent chord rectangles meet at the centreline but leave outer turn wedges that were incorrectly restored as floor above the stair.

Fix / next action: Add24-sided swept-corridor join polygons at intermediate connected points with unchanged2.2m clearance; retain original terminal landings. All221flights/84links compare without new passing-direction regressions,3237independent body shaft samples clear; both cemeteryloops remain pass both directions. Preserve full rejected stair/flight tables.

Evidence and verification: development/levels/rebuild/floor-closure-pass/verification/; tools/build_roster_floor_closures.py; tools/planar_structure.py; structural_pass.deck_openings

Older-map obligation: All29roster decisions explicit. Nineteenzero-addition results do not establish complete map fitness. Preserve source assets, exact native earth punches,40space envelope and canonical van/power/hiding/story; remaining35stairsegments/35links, legacy producer migration, mature-source and actual runtime/network/hardware gates remain.

### DEV-417 — High stair landing overhangs descending treads; narrowing it removes walkway support

**Status:** FIX VERIFIED IN NATIVE CANDIDATES: full25movement/source regression; runtime integration open · **Affected:** Battery WLD-17 routes11/12 and analogous high landing corners WLD-09/WLD-18

Original high landing blocks standing headroom on descending treads. Rejected narrow-platform pilot passed stair centres but regressed six side-lane standing samples and sweeps.

Cause / uncertainty: Treating the stair centre independently of the full-width adjoining flat walkway.

Fix / next action: Retain original full landing and all flat route coordinates; inset the descending flight beyond the overhang, add a visible closed bridge and revise the shared authored route points. Never change support/body tolerances or remove blockers to manufacture passes. Final all25 native checks:110409flat samples,99937baseline valid,94879qualified sweeps,47recovered standing poses; zero new blocks/sweepfailures.221flight segments186→196passing both ways,84full links49→56; zero passing-direction regressions. Both battery links pass, original walkway full width retained. Independent exported/native solids and all11editable packed sources pass. Bay revision also retains existing terminal platform meshes and shallow upper tread standing surfaces when full tread columns move, preserving the adjoining full-width flat approaches.

Evidence and verification: development/levels/rebuild/stair-landing-pass/rejected/battery-narrowed-turn-*; final verification and native captures under stair-landing-pass/

Older-map obligation: Individually audit all29packets and older maps for stair/landing joins, guards, full-width circulation, depth and surface support; source candidate passes are not runtime integration.

### DEV-418 — Unupdated tread rotation cuts an unsupported wedge from a new stair landing

**Status:** FIX VERIFIED IN NATIVE CANDIDATES: full25movement/source regression; runtime integration open · **Affected:** Stair landing construction candidate WLD-17 route12; rebuilt high corners across WLD-09/WLD-18 also regenerated

First full-width candidate retained all standing poses but introduced one side-lane sweep failure and lost support at the route12 stair-to-bridge transition.

Cause / uncertainty: Blender matrix_world had not updated after rotating newly created tread boxes; planar subtraction used an unrotated top footprint. Splitting coplanar triangles and float slivers made the missing wedge harder to diagnose.

Fix / next action: Update the dependency graph before reading final world top coordinates; subtract a complete convex top only when its area equals the actual triangle union. Snap authored cutters to10micrometres, bake only new transforms and triangulate before exact export/native comparison. Preserve rejected native results; do not modify production tolerances. Final all25 native checks:110409flat samples,99937baseline valid,94879qualified sweeps,47recovered standing poses; zero new blocks/sweepfailures.221flight segments186→196passing both ways,84full links49→56; zero passing-direction regressions. Both battery links pass, original walkway full width retained. Independent exported/native solids and all11editable packed sources pass.

Evidence and verification: development/levels/rebuild/stair-landing-pass/rejected/stale-rotation-*; tools/build_stair_landings.py; tools/check_stair_landings.py

Older-map obligation: Audit actual rotated object footprints after transform/geometry changes; preserve original props, UVs, pivots and saved terrain holes. All25candidate movement and source checks required; complete runtime/network/hardware acceptance remains open.

### DEV-419 — New transformed tread boxes differ from their exported native triangle records

**Status:** FIX VERIFIED: bake new transforms and triangulation; all11sources pass exact parity · **Affected:** Stair end/corner generator initial WLD-17 pilot; new transformed tread boxes

Independent reimport stopped with export/native mismatch for Rebuilt stair WLD-17 route-11 flight0 part0.

Cause / uncertainty: New box transforms and exporter triangulation applied after native world-float records were computed can produce different triangle/float representations.

Fix / next action: Bake transforms on newly authored meshes only and triangulate before both GLB export and native record generation. Preserve all original pivots/resources; independently compare every new exported triangle and editable source to native records. Final eleven candidates pass.

Evidence and verification: Observed interrupted build/stair-end-pilot.log traceback at session recovery (log later replaced by rerun); tools/build_stair_landings.py and tools/check_stair_landings.py; development/levels/rebuild/stair-landing-pass/verification/independent-sources.json

Older-map obligation: Individually check render/native coordinates after transformed geometry generation; never globally reset original imported asset pivots or UV materials.

### DEV-420 — Several native stair review cameras are occluded or too distant for join acceptance

**Status:** OPEN: diagnostic framing/structural join review required · **Affected:** WLD04terrain closeup; WLD13/WLD18roof/wall closeups; isolated whole-site landing additions

WLD04closeup is filled by terrain; several WLD13/WLD18joins remain behind walls/caps. Isolated additions in whole-site overviews are too small for detailed seam inspection.

Cause / uncertainty: A common high-end camera and bounding-distance context rule does not guarantee a clear line of sight to every distinct architectural join.

Fix / next action: Keep captures truthfully labeled and withheld from complete visual acceptance; retain geometry/physics blockers. Next native review should author per-join cameras/cut planes or physically resolve the terrain/wall/cap conflicts first. Do not remove those blockers from playable geometry to improve a diagnostic view.

Evidence and verification: development/levels/rebuild/captures/stair-landing-fit-batch/closeup-sheet-1.png; closeup-sheet-2.png; closeup-sheet-3.png; verification/captures.json; physical aperture batch nine actual native sheets reviewed; WLD03terrain/WLD05mine/WLD09turbine views still partly obstructed, not complete visual acceptance.; bay revision has19final native frames/four sheets: shaft plans expose actual mine opening, while several context cameras remain occluded. Not complete visual acceptance.

Older-map obligation: Audit actual visible joins at standing/crouched height across all29packets. Whole-site and isolated exports do not replace in-room/native/runtime visual review.

### DEV-421 — Authored stair routes intersect continuous structural walls and floor caps

**Status:** PARTIAL NATIVE REPAIR VERIFIED: eight candidates; remaining terrain/crossed-flight blockers open · **Affected:** WLD03/04/05/06/08/09/13/18source stair corridors

Production native sweeps stop at room/undercroft/lab/dorm/gallery walls and internal floor/cap cells along authored vertical routes.

Cause / uncertainty: Original structural shell openings did not follow the full authored stair body volume, despite stair/route coordinates being present.

Fix / next action: Replace29individually named shell parts with closed physical apertures; retain all unrelated meshes/resources/UVs/pivots/native records and fixed context.5049independent body samples and actual native sweep comparisons pass:221flights196→208and84links56→68bothways, zero previous passing-direction regression.13flights/16links remain failing; no runtime or whole-map completion inferred.

Evidence and verification: development/levels/rebuild/stair-opening-pass/; tools/build_stair_openings.py; tools/check_stair_openings.py; tools/audit_macroterrain_fit.py; verification/failure-contacts.tsv; captures/stair-opening-batch/

Older-map obligation: Audit all29packets and older sources individually for real render/body/optical openings,160mmdeck depth, wall thickness, supports and roof/light isolation; keep native runtime integration/power/hiding/story/hardware gates open.

### DEV-422 — Flight-only headroom cuts leave blocked landing caps and tread-level wall thresholds

**Status:** FIX VERIFIED IN EIGHT NATIVE CANDIDATES; broader map acceptance open · **Affected:** WLD08L20, WLD09wall crossings, WLD18route14landing/floor-cap cells

First aperture draft cleared central wall areas but stopped at the terminal plane or remaining cap over the intermediate flat landing. Opened wall lower edge also struck the actual foot capsule between treads.

Cause / uncertainty: Only the sloped centre flight was subtracted, excluding intermediate flats, endpoint footprint and stepped foot/body envelope.

Fix / next action: Use the full existing continuous link including flat landings,350mmconstant-height terminal extensions,2.2mheadroom and full authored width+40mm. Wall lower face follows tread clearance down250mm; slab lower face retains supports atfeet+5mm. Do not change production movement tolerances. Exact closed source/native and complete-route comparisons pass; remaining failures explicitly preserved. Bay revision found the analogous sheared lower-cut wedge in an owned floor. Sloped vertical shaft cuts now remove full slab thickness, while flat supports remain; explicit new landing parts restore actual upper/lower walking faces.

Evidence and verification: development/levels/rebuild/stair-opening-pass/; tools/build_stair_openings.py; tools/check_stair_openings.py; tools/audit_macroterrain_fit.py; verification/independent-sources.json; verification/flight-sweeps.tsv; development/levels/rebuild/stair-bay-pass/rejected/observed-construction-failures.txt

Older-map obligation: Audit every stair/body footprint and all intermediate/terminal landings; one centreline ray or disconnected flight pass does not prove complete circulation.

### DEV-423 — Crossed mine stair shaft removes support from an upper walkway

**Status:** OPEN GEOMETRIC CONFLICT; rejected shaft deletion withheld and six regressions restored · **Affected:** WLD05L16stair and L18flat walkway atX11,Y-6,Z42

Uncoupled shaft draft caused six previously valid flat samples to lose support at the centre and both side lanes, across two adjoining walkway segments.

Cause / uncertainty: Existing rough stair bay climbs through the floor used by another authored route. Carving necessary headroom there destroys the higher walkway.

Fix / next action: Preserve rejected actual GLB/HCS2/metadata and full failing probes. Withhold cuts for terrain/crossed-step-only links until a separate stair bay is authored; retain the original upper floor. Final110409flat samples restore all previous valid poses with zero new blocks/sweepfailures. Stair conflict remains a required redesign, not called fixed.

Evidence and verification: development/levels/rebuild/stair-opening-pass/rejected/mine-walkway-conflict-*; rejected/uncoupled-shaft-native-route-probes.tsv; verification/native-route-fit.json

Older-map obligation: Individually audit grade-separated crossings and all upper routes before removing floors for stair shafts. Preserve40space limits, geometry/native shared coordinates and actual saved terrain holes.

### DEV-424 — Structural fit reporter could return success while recording new flat-route regressions

**Status:** FIX VERIFIED: opening-pass now asserts no new blocks or sweep failures after preserving raw report · **Affected:** New structural opening batch using tools/audit_macroterrain_fit.py

Initial draft produced6new_block findings while the general measurement script still exited0; manual raw-probe inspection caught and rejected it.

Cause / uncertainty: The original tool was designed to report terrain findings, so a successful measurement process was not a no-regression acceptance gate.

Fix / next action: For the new opening-pass mode, retain all raw probes/report then assert every site has zero new_block and new_sweep_block. Final complete run exits0 with both counters zero, and docs distinguish measured findings from map acceptance.

Evidence and verification: development/levels/rebuild/stair-opening-pass/; tools/build_stair_openings.py; tools/check_stair_openings.py; tools/audit_macroterrain_fit.py; rejected/uncoupled-shaft-native-route-fit.json; verification/stair-openings-flat.log

Older-map obligation: Do not infer geometry acceptance from reporter exit codes alone. Retain before/after production-pose evidence, all failed cases and per-map obligations; existing general terrain finding mode remains a measurement tool.

### DEV-425 — Converging solid stair columns occupy room anchors and intersect independent routes

**Status:** PARTIAL NATIVE BAY REPAIR VERIFIED: three sites; remaining mine/terrain conflicts open · **Affected:** WLD05ore-west/lower/vent, WLD09east/turbine/spillway/return, WLD12trestle/lower/return

Multiple flights share a high anchor but their full solid columns overlap. Lower mine room centre lies inside a previous stair, and railway return crosses the descending column. First bay draft moved a mine flight into upper haul supportL10 and regressed a passing route.

Cause / uncertainty: Rough links became separate full-height solid flights instead of a coherent shared stair hall or separated bays; moving one centreline did not account for all upper routes.

Fix / next action: Mine: move shaft to west bay and share its actual flight with the lower return, retaining original link endpoints and rebuilding vacated floor shafts. Powerhouse: reuse existing main stair and add coherent lower bypass/landing; remove duplicate columns. Railway: separate descent and return with straight lead-ins. Preserve terminal pads and exact native component identities. Final25candidate checks:221flight segments208→214and84links68→73bothways,292recovered flat poses, zero previous passing-direction or flat regressions.7flights/11links remain, including mineL17and actual saved-terrain work.

Evidence and verification: development/levels/rebuild/stair-bay-pass/; tools/build_stair_bays.py; verification/; rejected/first-bay-*; verification/remaining-links.json; source design and authored-layout.json

Older-map obligation: Individually audit room/spawn anchors, upper paths, shared stair bays, full footprints and physical earth openings across29packets. Source candidates do not establish runtime ownership, global walkability, guard/framing or hardware acceptance.

### DEV-426 — Stair exit clips diagonal tread corners when no physical toe landing is authored

**Status:** FIX VERIFIED: actual closed toe landing and aligned lower approach; no kernel tolerance change · **Affected:** WLD09main east stair L09with new L11/L12routes; existing west stair L07lower approach

Native complete paths stopped on first tread side/corner while standing poses remained valid; direct contacts identify L09flight0step1 and L07flight0step2.

Cause / uncertainty: Flat approach left the diagonal flight sideways without a complete landing/straight lead-in. Generic floor ownership did not represent the actual stair transition, and full solid treads still occupied the approach.

Fix / next action: Route away from the west stair low face before turning; align the main stair lower lead-in with its flight. Physically partition a measured2.5mclosed160mmtoe landing from the existing floor and give that actual native part its stair-landing identity. No phantom identities, broad whole-room stair tagging, movement exception or tolerance change. Widen physical upper hub; preserve existing pads and shallow upper tread standing surfaces while removing conflicting lower columns. Full native links and original110409flat comparisons pass with zero new regressions.

Evidence and verification: development/levels/rebuild/stair-bay-pass/; tools/build_stair_bays.py; verification/; rejected/bay-path-contacts.tsv; bay-flat-contacts.tsv; verification/complete-route-sweeps.tsv; design.landing_pockets and hub_landings

Older-map obligation: Audit actual foot/capsule corner clearance, complete flat-to-stair chains and landing dimensions. Centreline steps alone do not prove approach or exit circulation.

### DEV-427 — Repeated cuts through already-authored shared passages create nonmanifold shell slivers

**Status:** FIX VERIFIED: exact retained segment reuse and deduplicated physical cuts · **Affected:** Stair bay constructor WLD09workshop north wall existing physical aperture

Generator rejected Bayed shell Opened stair shell Wall workshop N.001 as a new open solid.

Cause / uncertainty: A shared route was independently recut through the same existing aperture, including overlapping duplicate cutter segments.

Fix / next action: Retain exact pre-existing shared segment geometry; skip its duplicate shaft cutting and deduplicate equivalent new segments before Boolean work. Independent exported/native closed nondegenerate outward geometry checks pass for all3final sources. Preserve unrelated source resources and protected boundaries.

Evidence and verification: development/levels/rebuild/stair-bay-pass/; tools/build_stair_bays.py; verification/; rejected/observed-construction-failures.txt; tools/check_stair_bays.py; verification/independent-sources.json

Older-map obligation: Repeat independent geometric closure and exact native parity checks after every shared passage edit; never accept a Boolean merely because export succeeds.

### DEV-428 — Bay floor restoration treats shaft-opening records as raw polygons

**Status:** FIX VERIFIED: explicit polygon adapter; actual restored shaft solids checked · **Affected:** Initial tools/build_stair_bays.py old/new deck_openings integration

Background authoring stopped with TypeError unhashable type slice while clipping a vacated shaft.

Cause / uncertainty: deck_openings returns metadata records containing polygon coordinates, not polygon lists directly.

Fix / next action: Read each record polygon explicitly before intersection/difference. Final three candidate builds and actual reopened exported/native/editable checks pass; movement comparisons recover292standing poses with no new flat or stair regressions.

Evidence and verification: development/levels/rebuild/stair-bay-pass/; tools/build_stair_bays.py; verification/; rejected/observed-construction-failures.txt; restored_shaft_parts in actual construction metadata

Older-map obligation: Preserve source helper contracts and exact saved-world fingerprints when migrating frozen map producers; no unverified old map fix inferred.

### DEV-429 — Current saved-terrain layout points to a nonexistent scene descriptor

**Status:** FIX VERIFIED: actual scene.bin bound and independently checked · **Affected:** Initial stair-terrain composite current_saved_editor_terrain contract

New contract initially named scene-package/scene.properties although the persistent editor uses scene.bin.

Cause / uncertainty: Assumed scene descriptor filename rather than inspecting the actual persisted package.

Fix / next action: Bind actual scene.bin, current native physics, all published OBJ chunks and exact saved-world files. Independent checker requires the descriptor to exist and all declared files to match hashes. Shared producer consumes actual current contract.

Evidence and verification: development/levels/rebuild/stair-terrain-pass/verification/independent-sources.json; tools/compose_stair_terrain.py

Older-map obligation: Inspect real package filenames and saved topology, never infer editor/runtime integration from plausible metadata. Individual older-map contracts remain audit obligations.

### DEV-430 — Unchanged terrain-pass fallback reuses stale prior-bay stair identities

**Status:** FIX VERIFIED: unchanged cases normalize before state to their current source · **Affected:** tools/audit_roster_stairs.py --bench-pass unchanged WLD05/WLD09bay candidates

Native reader correctly rejected unknown old L15/L16tread IDs because the fallback loaded a current bay payload with its prior-bay component set.

Cause / uncertainty: Nested checkpoint metadata carried before_layout/before_stair_components from a previous construction comparison into an unchanged self-comparison.

Fix / next action: Normalize unchanged before layout, segment mapping and explicit stair component identities to the current source metadata. Keep strict unknown-component rejection. Final221segment/84link comparison, all25bindings and source hashes pass with zero previously passing-direction regressions.

Evidence and verification: development/levels/rebuild/stair-terrain-pass/verification/stair-movement.json; tools/audit_roster_stairs.py

Older-map obligation: Audit every before/after source coordinate/layout/component binding when combining stage snapshots. Do not weaken physics or silently discard unknown stair identities.

### DEV-431 — Isolated upper stair grading obstructs lower circulation through the same hillside

**Status:** FIX VERIFIED FOR THREE NATIVE CANDIDATES; landscape/retaining acceptance open · **Affected:** WLD03L15lower path, WLD12L06/L07west return, coupled hillside/railway terrain levels

First absolute grading draft introduced145standing and151sweep regressions. An upper-only lower-only draft still lowered previously high soil into lower paths headroom rather than below their circulation plane.

Cause / uncertainty: Treating an upper path datum as an independent terrain target; existing lower paths sometimes ran beneath a higher ground surface. Raising/filling was also inappropriate for an excavation bench.

Fix / next action: Use existing editor LOWER_ONLY brush API on named non-destructive layers; jointly constrain stair beds and every existing flat circulation segment. Preserve old source height PNGs and all punches. Independent comparison17010common vertices:2361lowered, zero raised. Final110409flat samples recover852poses with zero new standing/sweep regressions. Coarse cut banks remain explicitly unaccepted.

Evidence and verification: development/levels/rebuild/stair-terrain-pass/rejected/raised-grade-*; rejected/upper-only-cut-*; rejected/bench-flat-contacts.tsv; verification/independent-sources.json; tools/java/RosterTerrainBuild.java

Older-map obligation: Audit the complete terrain/floor hierarchy and buried or stacked circulation, not isolated connector ribbons. Retaining/cave/water/physical edges and canonical van arrival remain individual gates across29packets.

### DEV-432 — Ruin threshold uses incidental soil instead of a complete structural walking surface

**Status:** FIX VERIFIED: real closed160mmthreshold restores swept support · **Affected:** WLD04L07ruin west threshold X40,Y6,Z22; centre and left approach lanes

Excavation left all standing samples valid but two previously passing sweeps lost foot support immediately west of the room floor.

Cause / uncertainty: A small gap between the approach support and room slab had relied on the old terrain for footprint support.

Fix / next action: Add a measured2mclear-width160mmclosed bearing threshold, subtracting existing top surfaces to avoid coplanar duplicates. Rendered/exported/native/editable source matches. Exact native contact diagnosis retained; final all25flat regression counters zero. Paired intact threshold images inspected. Actual pre-threshold native bytes restored with SHA c6853c362f4c0cd556c2b1137d164710388fc18ab625fd21c9d3dcdc819dcd4d matching captured failure:4directional failures versus0after the repair.

Evidence and verification: development/levels/rebuild/stair-terrain-pass/rejected/bench-flat-sweep-contacts.tsv; assemblies/wld-04/construction.json; verification/independent-sources.json; captures/stair-terrain-batch/wld-04-threshold-after.png; verification/threshold-control.json; rejected/ruin-threshold-before.hcs2

Older-map obligation: Audit complete threshold foot footprints at door/floor/route joins, including under changed terrain. Centre support alone is insufficient.

### DEV-433 — Terrain excavation exposes previously embedded railway foundation feet

**Status:** FIX VERIFIED: five physical extensions restore seven newly exposed footing samples · **Affected:** WLD12Bearing skirts0016/0017/0018/0022/0042after lower circulation excavation

Before/after actual terrain rays found7previously embedded external footing samples above the revised soil.

Cause / uncertainty: Preserved foundation geometry still referenced the former higher soil datum after terrain excavation.

Fix / next action: Append closed connected extensions touching the original skirt bottoms and embedding350mminto the actual new soil; preserve original model pivots/resources/native records. Independent audit uses actual native extension vertices and soil rays, yielding zero new exposed footing samples across all3sites. Full native movement regressions remain zero; five isolated soil-contact renders inspected.

Evidence and verification: development/levels/rebuild/stair-terrain-pass/rejected/pre-extension-bearing-contacts.json; verification/bearing-contacts.json; assemblies/wld-12/construction.json; captures/stair-terrain-batch/*soil-bearing.png

Older-map obligation: Recheck foundations, piles, roots and ground-mounted facilities individually whenever terrain moves. This scoped footing repair does not establish all retaining/framing/guard or whole-map support acceptance.

### DEV-434 — Excavated terrain banks remain coarse and serrated in native whole-site views

**Status:** OPEN: authored slope/retaining/erosion and landscape visual refinement required · **Affected:** WLD03hillside and WLD04forestry terrace cuts; WLD12trench context

Full-site native renders expose faceted deep banks around newly cleared routes, with rough structure/support geometry and incomplete edge treatment.

Cause / uncertainty: Initial coherent circulation excavation uses the existing source grid/adaptive topology; physical usability correction is ahead of finished civil/rock/landscape construction.

Fix / next action: Keep visual terrain acceptance open. Author site-specific retaining/rock transitions, coherent slope sections, drainage and physical boundaries, then review whole sites with the canonical van. Do not pass the desquareify/landscape gate from native route counts or conceal the banks with fog.

Evidence and verification: development/levels/rebuild/stair-terrain-pass/verification/visual-review.json; captures/stair-terrain-batch/terrain-sheet-*.png; development/quality/DESQUAREIFY_GATE.md

Older-map obligation: Individually audit all29packets, including older landforms; no blockout rectangle/material stamp or perturbed outer box counts as accepted terrain.

### DEV-435 — Shared map producers ignore current saved terrain and chunked editor publications

**Status:** FIX VERIFIED IN SHARED SOURCE CONSUMERS; runtime integration open · **Affected:** structural_pass.saved_editor_terrain, structural build caller, collision/furnishing terrain fidelity checks and envelope classification

New composite layouts contain current_saved_editor_terrain with actual chunked publications; older consumers only recognized the single legacy saved_editor_terrain mesh field.

Cause / uncertainty: Stage-specific terrain authority was bound in metadata without corresponding shared producer selection/OBJ-chunk support.

Fix / next action: Prefer the explicit current contract, import each actual published chunk with correct vertex offsets, retain hash checking and exact legacy single-mesh behavior. Align structural/export/furnishing/envelope consumers. Actual3current worlds and real WLD18legacy control import with exact native triangle/flag equality; deliberately stale legacy control cannot replace current terrain. No installed runtime unlock inferred.

Evidence and verification: development/levels/rebuild/stair-terrain-pass/verification/terrain-contract-consumer.json; tools/check_saved_terrain_consumer.py; development/levels/blockouts/structural_pass.py

Older-map obligation: Migrate actual native/runtime ownership only through verified coordinate/layout/terrain contracts; metadata or export existence is not gameplay acceptance. Preserve frozen sources and legacy controls.

### DEV-436 — New connection builder assumes every retained floor-stage contract already declares stair identities

**Status:** FIX VERIFIED: explicit before/after identities pass native movement and export/source checks · **Affected:** WLD15retained floor-closure source during connection batch

First source build stopped with KeyError stair_components after WLD04/05 exported.

Cause / uncertainty: Floor-closure contracts predate the explicit per-map stair identity migration.

Fix / next action: Use the actual original furnishings physics contract only when the current contract has no explicit stair_components; preserve current declarations otherwise. New source report records exact before/after identity sets, independently exercised by unchanged production movement.

Evidence and verification: development/levels/rebuild/stair-connection-pass/verification/rejected-missing-floor-contract-build.log; tools/build_stair_bays.py

Older-map obligation: Individually consume each source-stage contract; never infer a whole scene is stairs or relax unknown component checks.

### DEV-437 — Saved-terrain continuation reads tile settings from the wrong source stage

**Status:** FIXED AND ACTUAL BATTERY WORLD REOPENED · **Affected:** WLD17saved occupancy-world terrain continuation

First authoring attempt failed because terrain-overall-pass/samples/wld-17/settings.tsv does not exist.

Cause / uncertainty: Helper assumed every source world originated in macroterrain rather than occupancy construction.

Fix / next action: Read settings from the exact selected saved worlds owning source package. Original WLD17world/height image retained; new lower-only layer verified against actual native/export/editable geometry, original sources and persistent hole probes.

Evidence and verification: development/levels/rebuild/stair-connection-pass/verification/rejected-wrong-terrain-settings.log; terrain/wld-17-world.json; terrain/verification/independent-sources.json

Older-map obligation: Saved-world/settings/height/layer/topology provenance must all refer to the same actual source; audit older worlds individually.

### DEV-438 — Revised stair hubs and excavated entry joins expose incomplete full-footprint structural support

**Status:** FIX VERIFIED FOR FIVE CANDIDATES; remaining flat/map acceptance open · **Affected:** WLD05L05/L08near adit high hub; WLD17route01/02lower entrance and route03approach

First connection pilot passed all221flights but mine flat sweeps regressed twice; battery flat approach still hit terrain. Lowering actual soil then exposed two lower entry sweeps and a previously passing complete route02.

Cause / uncertainty: Old hub/top tread and entry joins relied on incidental soil or incomplete platform edges; centre standing support did not prove the moving foot footprint.

Fix / next action: Mine receives a real closed common hub plate after both sloping rises finish. Battery receives actual LOWER_ONLY saved terrain plus closed full-width approach and lower entry bearing plates; rerun all flat lanes and continuous flight/link/chain comparisons without changing movement tolerances. Final all25native comparison:221/221flight segments and84/84complete vertical links pass both ways, no passing direction/chain regression.110409flat samples recover149poses with0newstanding/0newsweep failures. All9129remaining pre-existing invalid flat samples diagnosed separately.

Evidence and verification: development/levels/rebuild/stair-connection-pass/rejected/pilot-01/verification; rejected/pilot-02/verification; tools/build_stair_connections.py; tools/finish_connection_terrain.py

Older-map obligation: Reaudit physical threshold/endpoint/turn footprints after terrain and stair revision. All29packets retain individual native/runtime/visual acceptance obligations.

### DEV-439 — Native connection inspection shows black triangular landing regions and unfinished stair surfaces

**Status:** OPEN: actual coverage/material/light attribution and final architecture review required · **Affected:** WLD04clearing/hut level landing; WLD05adit shared hub; audit other source landings individually; WLD18exhibit threshold black rectangular region in paired native views

Paired neutral150Wnative captures show large black regions bounded by structural/old surface edges. New routes may pass while the visible joins remain unaccepted.

Cause / uncertainty: Unverified: distinguish actual coverage voids, existing material faces/normals, adjacent slab occlusion and neutral-light shadowing with native triangle/face diagnostics and actual runtime light separation.

Fix / next action: Keep construction/finish acceptance open; retain paired captures and audit floor coverage, underside/closed edges, original materials and actual circuits/roof/corner separation. Do not mask with fog or call source movement evidence a complete visual map approval.

Evidence and verification: development/levels/rebuild/captures/stair-connection-batch/connection-sheet-1.png; wld-04-clearing-hub-after.png; wld-04-hut-entry-after.png; wld-05-adit-hub-after.png

Older-map obligation: Complete individual MAP_FITNESS_CHECKLIST floor/light/material audits across all29packets and follow-on locations; isolated neutral construction views are not runtime acceptance.

### DEV-440 — Complete vertical circulation does not close the remaining invalid flat route network

**Status:** OPEN: actual native flat samples diagnosed, physical revisions required across all25candidates · **Affected:** All25rebuild candidates;9129of110409sampled flat positions still invalid after connection batch

All221vertical flights and84complete vertical links pass, but101280of110409flat centre/side samples are valid.6714remaining pose diagnoses identify actual physical components,2079lack centre support,252closed-solid/interior or invalid poses; remaining84involve footprint/height spread.

Cause / uncertainty: Existing flat route reservations intersect actual walls/caps/terrain/props or lack full support. Zero new regressions cannot be treated as all routes passing.

Fix / next action: Retain complete native failed-pose table and per-site component/segment/reason counts. Next batch authors real wall/door/cap apertures, coherent flat approaches, terrain/body clearance and prop moves against current shared source coordinates; repeat all flat/vertical/chained regressions. Preserve search envelope, services and reusable asset library. Latest flat-circulation checkpoint: original110409body samples now102726valid/7683invalid,1446recovered with0newstanding/0newsweep failures. Separate current strict full-footprint table119775samples/13483invalid and380of566complete flat links passing both ways; do not compare different sample sets.

Evidence and verification: development/levels/rebuild/stair-connection-pass/verification/remaining-flat-routes.json; remaining-flat-contacts.tsv; remaining-flat-input.tsv; tools/diagnose_remaining_flat_routes.py; development/levels/rebuild/flat-circulation-pass/verification/current-flat-movement.json; native-route-fit.json

Older-map obligation: Each of29packets and follow-on maps needs actual native and runtime route/support audit; four mature sources are not covered by the25candidate table. No acceptance inferred from vertical routes alone.

### DEV-441 — Diagonal threshold approaches graze walls and create oversized longitudinal aperture slots

**Status:** FIX VERIFIED FOR SOURCE APPROACHES; remaining route design is open · **Affected:** ACADEMYeast cloister L41 and other explicit shared-wall threshold approaches in25candidate layouts

Initial flat aperture pilot followed schematic diagonal approaches close to a wall, carving a long slot instead of a normal doorway.

Cause / uncertainty: Treating a threshold reservation point as a complete approach design; route lines converged toward the boundary from far down a narrow corridor.

Fix / next action: Preserve actual route endpoints and threshold coordinates, route perpendicular to the actual shared wall using interior approach legs, then construct body apertures along those authored approaches. Preserve grazing pilot exports/native/scenes/probes and verify current complete links plus unchanged valid-coordinate controls.

Evidence and verification: development/levels/rebuild/flat-circulation-pass/rejected/grazing-approach-pilot; tools/build_flat_circulation.py; captures/flat-circulation-batch/flat-pilot-sheet.png

Older-map obligation: Audit doorway angle, usable jamb/approach space and wall continuity individually; native recovery counts cannot justify incoherent wall slots.

### DEV-442 — Raised tunnel ceiling overlaps the occupied shed floor above it

**Status:** FIX VERIFIED: five shared ceiling ownership cases /1,072actual top/underside probes; fixture/runtime acceptance open · **Affected:** WLD08generator tunnel-n ceiling topY0.5 beneath generator floorY0; audit other fully stacked source zones

Actual cap96:Ceiling cap tunnel-n.003 occupiesY0.34..0.5 above the generator walking floor. Path-only cutting creates a partial roof trench through the upper room.

Cause / uncertainty: Lower tunnelY-2.5 retained nominal3mheight even though an actual occupied floor atY0 already owns the shared ceiling.

Fix / next action: Assign the fully covering upper floor as ceiling owner, set lower zone height2.5m, retire raised lower caps and trim lower walls/headers to the shared datum. Preserve actual upper floor depth/shaft openings and verify lower/upper routes and visual closure. Partial overlaps require separate design, not blanket lowering.

Evidence and verification: development/levels/rebuild/flat-circulation-pass/rejected/partial-ceiling-trench; tools/build_flat_circulation.py; captures/flat-circulation-batch/rejected-partial-cap-wld-08-threshold-after.png

Older-map obligation: Audit stacked floor/ceiling/volume ownership at authoritative elevations; fixture remounting, ceiling rays, service heights and whole room visual acceptance remain individual obligations.

### DEV-443 — Body-only flat standing counters omit unsupported capsule-foot footprints

**Status:** AUDIT VERIFIED FOR25CURRENTSOURCES;13483strictinvalidsamples/186incompleteflatlinks remain OPEN · **Affected:** All25source candidates and original flat-route comparison interpretation

Original flat after_valid counter checked centre support and capsule body only; full movement/traversable can still reject neighbouring feet. Threshold rerouting also changes which samples represent current design.

Cause / uncertainty: Reusing a historical unchanged-coordinate regression table as proof of every current authored path being fully usable.

Fix / next action: Keep historical body/sweep regression unchanged. Add explicit opt-in strict-footprints production traversable probes of actual current centre/side lanes and continuous forward/reverse complete flat links with original endpoints. Bind exact layouts/native hashes and diagnose every remaining strict invalid sample; never compare counts across different sample sets as if unchanged.

Evidence and verification: tools/audit_current_flat_routes.py; tools/java/FurnishingRouteProbe.java; development/levels/rebuild/flat-circulation-pass/verification/current-flat-movement.json

Older-map obligation: Verify physical full-footprint support and complete current routes individually, including four mature sources outside25candidate scope. No runtime/full-map acceptance from body-only samples or green vertical routes.

### DEV-444 — Independent stacked-ceiling checker imports blockout geometry before installing its module path

**Status:** FIX VERIFIED: all25independent source checks execute · **Affected:** tools/check_flat_circulation.py added stacked ceiling audit

First ceiling ownership check stopped at ModuleNotFoundError structural_pass before running native/source checks.

Cause / uncertainty: New helper import preceded setup of the repository blockout module directory.

Fix / next action: Add explicit repository tools/blockout paths before importing deck_openings. Reopen actual editable/export sources;544actual upper-floor/underside probes pass outside source stair openings, lower walls/headers stay below shared datum.

Evidence and verification: development/levels/rebuild/flat-circulation-pass/verification/rejected-checker-import.log; verification/flat-check-pilot.log; verification/ceiling-ownership.json

Older-map obligation: A stopped checker and Blender zero process exit are not a validation pass. Require actual current per-site reports and assertions.

### DEV-445 — Redundant rough route prisms share coplanar room-floor faces and produce black native surface patches

**Status:** FIX VERIFIED FOR673FULLYOWNEDROUGHPRISMS; partial overlays/other dark regions remain open · **Affected:** WLD08L12support1 and other original fully covered rough flat-route supports across25candidates

Actual exported pixel rays hit Floor generator.006 and L12support1 at identical distances. Native image contains a black triangular floor region despite544floor/underside coverage probes passing.

Cause / uncertainty: Retained schematic route support prisms lie exactly on actual owned room floors, giving multiple surfaces the same floor plane.

Fix / next action: Remove only original rough route prisms whose complete actual upward top surface is covered by real equal-height floor triangles, keeping props, treads/landings, floors, partial/exterior bearings and all original mesh resources. Independent checker replays source-prism coverage and requires complete flat extrusion. Pilot14removed beams eliminate the large triangular black patch in matched native view; narrower partial overlaps remain unaccepted.

Evidence and verification: development/levels/rebuild/flat-circulation-pass/verification/native-dark-floor-rays.json; rejected/coplanar-floor-pilot; tools/resolve_flat_floor_ownership.py; verification/floor-surface-ownership.json; captures/flat-circulation-batch/rejected-coplanar-wld-08-threshold-after.png; captures/flat-circulation-batch/wld-08-threshold-after.png

Older-map obligation: Audit actual floor ownership and render overlaps independently acrossall29packets; this scoped removal does not resolve all dark regions, partial overlays, stair overlaps, optical/runtime or complete map acceptance.

### DEV-446 — Powerhouse Boolean wall slivers collapse after native float coordinate transforms

**Status:** FIX VERIFIED: all25current native files parse; exact rejected states retained · **Affected:** WLD09Flat route shell Bayed shell Wall turbine S and generated shell family; final established-route build also exposes4WLD03generated-shell zero-area float triangles

Actual native parser rejects26zero-area triangles. Independent source check also refuses the payload; Blender process exit alone had not shown the failure.

Cause / uncertainty: Very short CSG edges remained distinct in local mesh coordinates but coincided after float world transforms atX43metres.

Fix / next action: Preserve actual rejected export/native/editable state and failed raw logs. Bake only25newowned wall/cap parts to native world coordinate precision, weld20micrometres/dissolve10micrometreedges, restore closed outward triangles and appendexact render/native replacements. Original assets/resources/stairs, terrain and literal movement tolerances retained. Update construction/layout replacement identities together. Final exact-float scan retains both actual bad input families; apply the same closed world-coordinate repair to WLD03/WLD09 only. Preserve exact driver versions for replay. Final authoring bakes new wall world coordinates before Boolean operations. Four remaining Asylum near-degenerate panels use closed extrusion of actual front/back silhouette intersection on a10micrometreprofile grid. Original resources stay exact. Final25native/export/editable checks and literal production parser pass; broad weld cleanup failures remain rejected.

Evidence and verification: development/levels/rebuild/flat-circulation-pass/rejected/native-precision-collapse; verification/precision-repair.log; tools/repair_flat_native_precision.py

Older-map obligation: Require actual native parser and closed/export/UV/native checks for every new source. Never remove triangles only from collision, suppress degeneracy rejection or call a stopped checker successful.

### DEV-447 — Blanket flat-route slab cuts and unnecessary normalization regress working upper floors and approaches

**Status:** FIX VERIFIED:0newold-coordinate standing/sweep failures and0complete-path regressions; current invalid footprints remain open · **Affected:** ChurchL18/MansionL28/SchoolL36old-coordinate probes; establishedMansion/School/WLD03/WLD07/WLD09/mortuaryflat links and cemetery loops

Initial complete batch recovered1739body poses but introduced1standing/2sweep failures and regressed already-passing complete paths, including both cemetery loops.

Cause / uncertainty: Flat2.2mheadroom cutters cut into upper floors over low spaces, and normalizing already-working approaches moved them off existing supports or toward stair conflicts.

Fix / next action: Preserve actual rejected all25payloads/raw probes. Flat routes do not cut floor/roof slabs; full shared stacked ceiling conflicts use actual ownership design. Preserve source polylines already passing complete native movement; normalize only failing threshold approaches, keeping endpoints/thresholds. Rerun unchanged old-coordinate, strict actual-current flat, vertical and cemetery-loop gates without loosening movement. Flat wall cuts now start at the actual walking plane, preserving old wall-top bearings. The School probe remains strict-footprint-invalid in BOTH original and final sources; that older defect is not mislabeled newly fixed. Both cemetery loops pass14segments again.

Evidence and verification: development/levels/rebuild/flat-circulation-pass/rejected/slab-and-established-approach-pilot; established-flat-links.json; verification/initial-failure-contacts.tsv; tools/build_flat_circulation.py

Older-map obligation: Existing complete movement is protected but does not accept a whole map. Low headroom, flat/stair crossings, terrain, full widths and final architectural elevation design remain individual obligations across29packets.

### DEV-448 — Active flat wall apertures reopen permanently sealed context boundaries

**Status:** FIX VERIFIED AT192LINKS/750BOUNDARYCENTRE RAYS; full envelope/runtime acceptance open · **Affected:** ACADEMYL39and retrospective192fixed links across25source candidates

Fresh reimported boundary audit finds AcademyL39open after flat cuts. Actual preceding source independently passes all26Academysealed link boundaries.

Cause / uncertainty: Preserving explicit infill nodes alone is insufficient when a nearby active path cuts another wall belonging to the closed room shell.

Fix / next action: Preserve complete sealed room volumes and full fixed-mouth reservations in source authoring; never apply a wall cutter where it conflicts with those protected bounds. Preserve all source sealed definitions and actual original walls there. Record each withheld conflict for subsequent spatial rerouting; independently rerun all fixed boundary rays, old/current movement and source preservation. Final authoring records394withheldwall/corridor conflicts instead of cutting protected bounds.186completeflatlinks and13,483strictcurrentstanding samples remain unaccepted; reroute coherently before runtime integration.

Evidence and verification: development/levels/rebuild/flat-circulation-pass/rejected/fixed-boundary-and-bearing-pilot; verification/fixed-boundary-before.log; tools/build_flat_circulation.py; verification/fixed-context-boundaries.json

Older-map obligation: Actual full width/body/around/under/ghost/object and runtime40space envelope audits remain required; centre rays do not accept whole boundaries.

### DEV-449 — Boolean union of clipped protected wall fragments creates a nonmanifold reception shell

**Status:** FIX VERIFIED: no fragment unions; source solids/volume/native/retained-resource checks pass · **Affected:** ACADEMYFlat route shell Wall reception E.001during sealed-context guard pilot

Closed-solid assertion stops the source build before export; Blender still returns process exit0.

Cause / uncertainty: Unioning exact original protected fragments into a repeatedly cut wall produces point-touching/degenerate topology.

Fix / next action: Retain exact failing builder and raw log; remove fragment-union strategy. Withhold conflicting aperture cutters before geometry generation and keep original protected wall architecture. No manifold/volume/native rejection threshold changed.

Evidence and verification: development/levels/rebuild/flat-circulation-pass/rejected/union-guard-nonmanifold; tools/build_flat_circulation.py

Older-map obligation: Require fresh actual reports and per-part assertions; a Blender process exit or unexecuted construction design is not successful source generation.

### DEV-450 — Inherited layout hashes bind working CRLF bytes instead of published repository LF bytes

**Status:** FIX VERIFIED: exact frozen inputs and alias-aware movement/source checks pass · **Affected:** Five inherited WLD06/08/11/13/17JSONinputs; current candidate before-layout references and historic route controls

Staged-byte preflight finds existing layout working bytes differ from repository bytes despite identical JSON coordinates.

Cause / uncertainty: Legacy layout paths predate hash-bound source attributes and inherit checkout newline conversion.

Fix / next action: Freeze exact consumed input bytes inside the new hash-bound source package; preserve original layout files and all coordinates. Alias-aware builders and route audits load the byte-identical snapshots. Candidate before-layout bindings reference frozen copies, and original working/repository SHA values are recorded. No native/model/route geometry changes. All three movement comparisons and25source/native/editable reopens pass with unchanged counts. Actual geometry/image hashes remain identical to implementation49ad38e.

Evidence and verification: development/levels/rebuild/flat-circulation-pass/verification/source-layout-aliases.json; source-layouts; tools/audit_current_flat_routes.py; tools/audit_roster_stairs.py; tools/audit_macroterrain_fit.py

Older-map obligation: Audit legacy byte-bound inputs individually across platforms; do not rewrite unrelated original layouts or infer all prior contracts fixed.

### DEV-451 — Finite arrival path prisms end under standing anchors without full footprint support

**Status:** FIX VERIFIED FOR21SOURCE ANCHORS; OTHER ARRIVAL AND RUNTIME GATES OPEN · **Affected:** 21 revised roster construction sources; WLD-09 explicitly rejected; three already-supported source arrivals and four mature maps retained individually

Standing anchor is supported at its centre but the rear .22m footprint falls beyond the authored finite route slab.

Cause / uncertainty: Rough route prisms terminate exactly at the spawn/staging point, with no standing setback.

Fix / next action: Append a .35m closed terminal slab at the actual path end, using its full route width, measured12cm depth and original source material. Preserve all existing GLB/node/resource/native/world records and source IDs. Verify21anchors with production full-foot traversal and inspect actual paired exported construction captures. Entire arrival routes, canonical van relationship and overall foundations remain separate.

Evidence and verification: development/levels/rebuild/arrival-bearing-pass/roster.json; verification/current-flat-movement.json; verification/independent-sources.json; captures/arrival-bearing-batch

Older-map obligation: Audit every other arrival/spawn/hiding/ritual anchor with actual full footprints, including mature Castle/House/Blackpine/Hollow Creek. This source repair does not accept runtime arrival/van/terrain or allmap circulation.

### DEV-452 — New arrival source inspector mistakes exported UV seams and lazy image data for broken assets

**Status:** FIX VERIFIED ON21FINAL SOURCES · **Affected:** tools/review_arrival_bearings.py; first local pilot checks only

First checker reports open edges on a closed UV-split box; next checker rejects lazily loaded packed images after reopening Blender.

Cause / uncertainty: GLB UV boundaries intentionally split vertices; Blender defers packed pixel decoding until used. Object-level topology and has_data alone are insufficient.

Fix / next action: Inspect coordinate-coincident topology in a temporary bmesh at1micrometre, preserve actual source/export bytes, require packed referenced images with real dimensions and decoded pixel storage. Independently compare actual exported/native triangle flags and unchanged original binary/resource/node prefixes. Correct new construction metadata to report only newly appended parts, linking inherited work through its exact source contract.

Evidence and verification: development/levels/rebuild/arrival-bearing-pass/verification/logs/arrival-pilot-review.log; arrival-pilot-review-final.log; independent-sources.json; tools/review_arrival_bearings.py

Older-map obligation: Do not rewrite actual model UVs or weld production geometry merely to satisfy an inspector. Continue individual exported/editable/native checks for each source.

### DEV-453 — WLD-09 saved terrain intersects the original arrival body; slab-only repair is buried

**Status:** SOURCE TERRAIN/ARRIVAL FIX VERIFIED; FULL WLD09/RUNTIME GATES OPEN · **Affected:** WLD-09 arrival L01 original anchor [36,0,82]

After filling the missing rear footprint, production traversal still fails against terrain-site-0-1:surface. Actual exported closeup shows terrain covering the proposed slab.

Cause / uncertainty: Existing terrain/arrival datum conflict was masked by earlier missing-foot support diagnostics. Adding floor support cannot remove soil from the body volume.

Fix / next action: Preserve rejected GLB/native/Blender/layout and paired PNGs with exact relocation hashes. Keep flat-circulation-pass/wld-09 as the active source. Next coordinated terrain/arrival revision must remove actual soil intrusion while preserving saved basement holes, intended landform, stable coordinates and passing vertical routes. Do not hide it with fog or move a spawn independently. Later saved circulation cuts remove all241measured WLD09soil contacts, including the arrival anchor. Reuse the exact end-slab only in that verified terrain context; the earlier buried study stays rejected.

Evidence and verification: development/levels/rebuild/arrival-bearing-pass/rejected/terrain-blocked-wld09; development/levels/rebuild/captures/arrival-bearing-batch/rejected-terrain-wld09; verification/current-flat-failure-contacts.tsv

Older-map obligation: Audit arrival/van bodies against actual saved terrain across all locations; individually verify support and body/head clearance. Other unsupported anchors may mask additional terrain/wall collisions.

### DEV-454 — Lower-only terrain cuts expose missing pavement beneath previously valid standing poses

**Status:** SOURCE FIX VERIFIED ACROSS FULL25CANDIDATE REPLAY; COMPLETE MAP GATES OPEN · **Affected:** Academy circulation L02 during saved circulation terrain pilot; all remaining sites need the same actual standing regression check

Soil-only pilot gains313strict poses but loses20previously valid Academy full-foot positions; complete links and body-only checks do not detect this regression.

Cause / uncertainty: Rough path prisms do not cover revised circulation coordinates. Old soil supplied walking support before the cut.

Fix / next action: Retain exact failing sources/probes/captures. Build actual closed terrain-rooted walkway beds outside real stair openings and closed context. Final three-site pilot recovers999positions at16104unchanged samples, zero new failures. Native and exported beds share exact coordinates; no physics threshold change. Full batch: eight sources,1831recovered full-foot poses at119775unchanged samples,0newfailures;221flights/84vertical links and both cemetery loops remain passing.

Evidence and verification: development/levels/rebuild/flat-terrain-pass/rejected/soil-support-only-pilot; pilot-verification/strict-standing-regression.json; tools/build_cut_walkway_beds.py

Older-map obligation: Run identical-coordinate full-foot comparisons after every terrain cut; a net improvement or passing centre/body/stair check never excuses newly unsupported poses.

### DEV-455 — Whole-strip and boundary-ray bed tests reject valid ground beside a punched landform edge

**Status:** SOURCE FIX VERIFIED IN EIGHT-MAP BATCH; COMPLETE TERRAIN/LAYOUT GATE OPEN · **Affected:** Academy L02 near [50.75,0,61.88]; other soil-edge and punched-hole receivers

Whole2mstrip receiver rejection leaves7new failures; clipping to terrain still leaves3because BVH rays miss exact source-triangle edge vertices.

Cause / uncertainty: A section crosses a real landform void; refusing the whole section drops valid near-side ground. Exact boundary ray misses are not evidence that its backing native triangle is absent.

Fix / next action: Clip each new pavement footprint to exact saved soil triangle coverage, carry its native triangle identity, and compute root heights on that actual plane. Independent checks verify footprint coverage, triangle bytes, native/export match and15cmembedding. Do not extend terrain, fill holes or relax production support. Preserve both failed whole-strip and boundary-ray studies. Three-site unchanged-position regression now passes with0newinvalid positions.

Evidence and verification: development/levels/rebuild/flat-terrain-pass/rejected/whole-strip-rooting-pilot; rejected/boundary-ray-rooting-pilot; pilot-verification/independent-sources.json; tools/review_flat_terrain.py

Older-map obligation: Individually audit punched boundaries, genuine stair shafts and road/terrain ownership. Route portions that actually leave the landform still require coherent rerouting; this bed repair does not accept them.

### DEV-456 — WLD-16 terrain body collisions have no exact persistent editor-world owner in current sources

**Status:** OPEN; ORIGINAL AUTHORED TERRAIN PRESERVED · **Affected:** WLD-16 route-12/13/14,211current terrain body contacts

Actual native terrain geometry does not match any existing saved terrain world for this site.

Cause / uncertainty: The current source retains its original authored roadside terrain rather than the later saved macroterrain candidate.

Fix / next action: Keep the exact original mesh and native coordinates. Record the missing owner; require a deliberate geometry-preserving editor/native migration or coordinated terrain redesign. Never swap in a merely similar world. Independent matched worlds can continue in this batch.

Evidence and verification: development/levels/rebuild/flat-terrain-pass/terrain-repair-plan.json; tools/plan_flat_terrain_repairs.py

Older-map obligation: Resolve every terrain replacement through exact transformed native geometry and saved-world provenance. No other location is accepted because eight saved sources match.

### DEV-457 — New terrain review camera and scoped-chain check use unscoped source assumptions

**Status:** FIX VERIFIED IN THREE-SITE PILOT · **Affected:** tools/review_flat_terrain.py camera; tools/audit_roster_stairs.py --sites pilot mode

Contact camera uses the first flat segment instead of the actual diagnosed segment; selected three-site stair check incorrectly requires cemetery chains outside its scope.

Cause / uncertainty: New batch tooling did not carry contact segment identity and selected-site declaration scope through its assertions.

Fix / next action: Use actual diagnosed segment index for the paired camera. Require exactly all declared chains belonging to selected sites; full-roster mode still requires both cemetery loops. Final three-site source/standing/body/stair checks pass; the final25site batch must rerun full loop controls. Retain original failed check logs and rejected camera captures. Full25site batch independently passes both cemetery14segmentloops after the scope correction.

Evidence and verification: development/levels/rebuild/flat-terrain-pass/verification/logs; pilot-verification/stair-movement.json; pilot-verification/captures.json

Older-map obligation: Label capture type and actual tested sites. Do not infer whole-roster route coverage from a selected pilot or treat an empty selected declaration set as whole-roster verification.

### DEV-458 — Circulation excavations expose coarse cut banks and unfinished physical edge interfaces

**Status:** OPEN; NATIVE SOURCE CLEARANCE DOES NOT ACCEPT LANDSCAPE · **Affected:** Eight revised terrain sources, especially WLD05/WLD09/WLD13cut banks and Academy perimeter

Paired native sheets expose sawtooth excavation faces, rough pavement/rim junctions and unfinished protective edges after soil/body conflicts are removed.

Cause / uncertainty: Two-metre persistent sculpt grids and rough circulation reservations establish coarse clearance before measured retaining/edge/landscape refinement.

Fix / next action: Preserve landform and saved punches. Author supported retaining/rim/guard interfaces, refine grades/materials and inspect standing/crouched/photo and highest views with the canonical van. Consolidate bed components without changing support. Current2/5construction,1/5finish is not production terrain.

Evidence and verification: development/levels/rebuild/flat-terrain-pass/verification/visual-review.json; captures/flat-terrain-batch/*-terrain-final-sheet.png; development/quality/DESQUAREIFY_GATE.md

Older-map obligation: Every older map needs individual landform, physical edge and full-width route audits. Native contact samples do not establish containment, light separation, visual fitness or runtime readiness.

### DEV-459 — Pilot and final native renders reuse filenames and invalidate older PNG hash bindings

**Status:** FIX VERIFIED BEFORE FINAL BATCH PUBLICATION · **Affected:** Three-site saved-terrain pilot capture paths after full eight-site regeneration

Exact staged preflight detects pilot captures.json SHA mismatch after final renders overwrite shared PNG filenames.

Cause / uncertainty: Original review tool names frames by site/view/phase without a unique generation namespace.

Fix / next action: Recover every exact pilot PNG blob from published8f759bd9, verify its original SHA and archive18PNGs under pilot-archive. Rebind only pilot report paths, retaining original hashes/models/cameras and full-batch frames. Save the exact v1checker executed for both model reviews; future renders use unique UTCmicrosecond run directories. No model, native geometry or test result changes.

Evidence and verification: development/levels/rebuild/flat-terrain-pass/pilot-verification/capture-rebinding.json; verification/generators/review_flat_terrain-v1.py; tools/review_flat_terrain.py

Older-map obligation: Keep immutable capture namespaces for every iteration; compare actual PNG bytes to reports before upload. An unchanged model or similar pixels does not prove an older image hash still matches.

### DEV-460 — Route endpoints in a shared room do not establish physical room connectivity

**Status:** OPEN; EXACT NATIVE ROOM SOCKET AUDIT RECORDED · **Affected:** Rooms touched by access-route-pass across14source maps; seven groups retain invalid original sockets,29groups have unproven direct joins

Individual link paths can pass while other incident room sockets remain inside props or on opposite sides of physical obstacles.

Cause / uncertainty: Original layout links reused room centres or independent door access points without proving intra-room movement.

Fix / next action: Retain valid route changes, audit actual unique incident endpoints and bidirectional5cm production sweeps, then author physical room connections or refit interfering geometry. Do not claim map-wide connected navigation from matching room IDs.

Evidence and verification: development/levels/rebuild/access-route-pass/verification/room-socket-connectivity.json; tools/audit_room_socket_connectivity.py; invalid and multi-component groups explicitly retained. Complete flat and stair regression checks remain separate.

Older-map obligation: Audit actual endpoints and within-room connections separately for every location, including mature maps; direct-join failure does not prove that an indirect path is impossible.

### DEV-461 — Native route search can find a physical path through unintended room domains

**Status:** REJECTED BEFORE AUTHORITATIVE SOURCE ADOPTION · **Affected:** WLD-08 L15; WLD-15 route-03; WLD-16 route-03

Three full-width proposals leave the union of their intended endpoint-room bounds.

Cause / uncertainty: Physics and sealed-context checks alone do not enforce authored room relationships.

Fix / next action: Preserve proposals as unadopted evidence; exclude them from access-route-pass. Redesign actual intended circulation/room arrangement instead of accepting a convenient physical shortcut.

Evidence and verification: development/levels/rebuild/access-route-pass/verification/authoring.json rejected records; <=0.1m path-domain sampling with0.3m wall/door tolerance.51other proposals remain pending independent route/source review.

Older-map obligation: Apply physical and intended-domain review to each older authored path. Ground support alone does not establish a coherent door/room connection.

### DEV-462 — Headless route review assumes an imported GLB supplies a Blender world

**Status:** FIXED;104REAL FINAL FRAMES AND26SHEETS HASH-VERIFIED · **Affected:** New tools/review_access_routes.py initial attempt; zero captures produced

Blender exits0 but logs AttributeError on scene.world.color after factory reset and GLB import.

Cause / uncertainty: Factory empty scenes have no world and GLB need not supply one; process exit alone is not a valid render acceptance check.

Fix / next action: Create an explicit neutral review world; rerun in a fresh unique capture directory, inspect logs and require actual image hashes/counts.

Evidence and verification: build/access-route-render.log initial terminal error; tools/review_access_routes.py corrected world creation; final captures.json will bind all actual PNGs and checker.

Older-map obligation: Check actual render reports and PNGs; never infer render success from Blender exit0 alone.

### DEV-463 — Generic route-change metadata confuses current and inherited map stages

**Status:** FIXED; EXPLICIT14SITE SCOPE AND EXACT MODEL/ALL-FLOOR CAPTURE IDENTITY VERIFIED · **Affected:** New access route review selector at unchanged WLD-05 source; initial partial32frame render study

Initial review followed an inherited route_changes field and then correctly failed its byte-identical GLB assertion on a map outside the new14site batch.

Cause / uncertainty: Several authoring stages use route_changes; its presence does not identify the newest source revision.

Fix / next action: Use an explicit access_route_changes field, preserve older metadata, scope review/audit to that new field, rerun movement/source bindings and keep partial captures under rejected-metadata-selector. Review all affected floors, not only the most common floor.

Evidence and verification: development/levels/rebuild/access-route-pass/verification/rejected-capture-study.json; exact failed log retained; primary-floor-captures.json and supplemental captures retain separate exact checker hashes.

Older-map obligation: Select stages from exact manifest and explicit revision fields; inherited construction metadata cannot prove that a map was revised by a later pass.

### DEV-464 — Exported roof closure review must weld UV seam vertices

**Status:** FIXED IN ROOF CHECKER · **Affected:** Farmhouse, Mansion and School roof construction verifier

Initial exported-solid check rejected a closed roof because glTF splits vertices at UV seams.

Cause / uncertainty: Raw edge topology on imported UV split vertices is not geometric closure.

Fix / next action: Weld coincident vertices only in temporary inspection BMesh; require manifold edges, positive volume and exact render/native triangle identity. Source assets remain untouched.

Evidence and verification: tools/build_roster_enclosed_roofs.py exported closure checks; final three construction contracts and captures.

Older-map obligation: Use geometric closure after seam welding for other GLB solids; retain exact triangle comparison.

### DEV-465 — New wing roofs retain unfinished weather joins and runtime integration

**Status:** OPEN · **Affected:** Farmhouse barn and house; Mansion stepped wings; School assembly/gym/health roofs

Closed native roof blocking is present, but step flashing, gutters and standing-height junction review are unfinished; live maps still use previous runtime authorities.

Cause / uncertainty: This bounded construction batch adds roof geometry to current native source candidates; whole-site runtime conversion remains a separate unfinished gate.

Fix / next action: Fit roof-to-taller-wall flashing and drainage; inspect joins under live lights; integrate exact source coordinates across runtime systems before acceptance or unlock.

Evidence and verification: development/levels/rebuild/roof-construction-pass/roster.json; captures/roof-construction-batch native overviews; native collision and route comparisons.

Older-map obligation: Other roster roofs remain individually unaudited; this batch confers no acceptance on retained sources.

### DEV-466 — Diagonal lower-access stair columns and retained hub obstruction

**Status:** PHYSICAL CANDIDATES REBUILT; WHOLE MAP ACCEPTANCE OPEN · **Affected:** WLD-06 Wharf L16/L17, WLD-08 Research L17/L18, WLD-12 Railway L17

Long skewed flights cross building/undercroft axes; initial orthogonal reconstruction retained old upper tread-hub plates across descending openings.

Cause / uncertainty: Path-first diagonal flights and generic hub-preservation assumptions did not represent stair-bay construction.

Fix / next action: Rebuild five flights along site axes with level turns, real shaft booleans and vacated-shaft restoration. Disable old-hub preservation for these plans only. All seven selected vertical routes now pass both ways, with zero previously passing flat-link regressions.

Evidence and verification: development/levels/rebuild/orthogonal-stair-pass/verification; captures/orthogonal-stair-batch; source/native/export checks in builder

Older-map obligation: Institutional selected stairs are axis aligned; remaining diagonal outdoor slopes, spiral towers, whole roster terrain/room connectivity and runtime acceptance remain individually open.

### DEV-467 — Stair guard ends obstruct turning landings unless construction reserves the approach

**Status:** INTERMEDIATE COLLISIONS CORRECTED; GUARD RETURNS AND LANDING PERIMETERS OPEN · **Affected:** Wharf WLD06, Research WLD08, Railway WLD12 five rebuilt flights

First fitted guards crossed approach/turn sweeps; short setback still invalidated nine previously valid strict lane samples.

Cause / uncertainty: Full-flight rail bays and side stringers occupied sideways entry/exit clearance at the flight ends.

Fix / next action: Use fitted existing guardrail profiles and seated side stringers on central flight sections, with1.6mend approach reserves. All7vertical routes and29flatlinks remain passing both ways; exact13791pose comparison has0newinvalid. End sections and landing perimeters still need widened landings/returned guard construction; not a completed safety enclosure.

Evidence and verification: development/levels/rebuild/stair-guard-pass/verification/unchanged-lanes.json; native movement reports and captures/stair-guard-batch

Older-map obligation: Audit each older flight/landing for full-width turning clearance; passing individual flights never proves the end-to-end route.

### DEV-468 — Wharf pier approach crossed descending-stair headroom and raised undercroft roof

**Status:** PHYSICAL CANDIDATE REBUILT; RUNTIME AND WHOLE-MAP GATES OPEN · **Affected:** WLD-06 L07/L16/L17; related widened approaches at WLD-08 and WLD-12

Opening and widening the actual stairs exposed a pier path over the descending flight and an undercroft cap above surface level. Full end guards and end stringers also intercepted turning paths.

Cause / uncertainty: Earlier path-oriented construction retained contradictory upper walking and lower headroom volumes, with insufficient straight landing depth before a turn.

Fix / next action: Build a supported 2.4m-wide quay bypass with undercroft-floor bearings; re-seat undercroft cap by0.8m and shorten its wall tops; source undercroft height becomes2.2m. Add1.2m straight lower turning approaches, flared tread wings and matched physical openings across five flights in three maps. End guards/perimeter returns remain open rather than being accepted as safe. Final7vertical routes/31flatlinks pass both ways; zero complete-link regressions and zero new invalid positions among common XYZ lane samples.

Evidence and verification: development/levels/rebuild/stair-end-pass/roster.json; verification/stairs, current-flat-movement.json and unchanged-lanes.json; six native views in captures/stair-end-batch

Older-map obligation: Review other map upper/lower paths as physical volume relationships. Native passing paths do not prove arbitrary room connectivity, light separation, edge guarding or runtime integration.

### DEV-469 — Repeated flared-shaft cuts produced degenerate float32 native plate triangles

**Status:** CORRECTED IN CURRENT GENERATED CANDIDATES · **Affected:** Wharf procedural deck plates; Research cut-plate attempt

Boolean outputs could appear closed in Blender but produce collinear or coincident native float32 triangle vertices; the unchanged production native loader rejected them.

Cause / uncertainty: Repeated coplanar cuts and sub-millimetre residual polygons exceeded reliable export precision.

Fix / next action: Reconstruct affected procedural floor/cap solids from their actual upward source triangles minus measured openings; remove below0.1mm boundary residuals and use a10micron coordinate grid on generated geometry only. Keep unaffected source binary resources and every retained native record unchanged. Final actual native loader, exact export/native triangle checks and movement replays pass.

Evidence and verification: tools/build_stair_end_batch.py; source-bound construction contracts and native probes in stair-end-pass/verification

Older-map obligation: Do not relax native-loader validity checks or claim Blender closure proves exported native correctness. Other scenes remain individually unaudited.

### DEV-470 — Rough saved circulation cuts lack retaining construction at Mine Dam and Cannery

**Status:** ROOTED SHOULDER CANDIDATES CONSTRUCTED; FULL TERRAIN AND RUNTIME ACCEPTANCE OPEN · **Affected:** WLD-05 Dead Mans Claim; WLD-09 Blackwater Dam; WLD-13 Greywater Cannery

Earlier lower-only soil cuts established rough playable bed levels but left raw exposed grade shoulders without construction or edge treatment.

Cause / uncertainty: Saved terrain excavation preceded retaining, drainage and terrain-to-structure finishing.

Fix / next action: Construct closed 0.32m thick retaining runs outside route and room reservations, with horizontal footing bases sampled over actual terrain footprints. 750 reopened-export root probes retain at least0.39999m soil embedding. Source terrain/topology, models and existing native records remain unchanged. All12flights/11vertical routes,29flat links pass both ways;13,329unchanged strict positions have zero newly invalid samples.

Evidence and verification: development/levels/rebuild/retaining-shoulder-pass/roster.json; individual construction root_checks and native/export bindings; verification/unchanged-lanes.json; terrain-interface closeups

Older-map obligation: Coping, weeps/drainage, returns, terrain art and full landscape/runtime gates remain open. Academy off-landform paths and WLD16terrain ownership are not fixed by these walls.

### DEV-471 — Cinder retaining stub attempt passes movement but lacks coherent bank construction

**Status:** REJECTED; PRIOR CINDER SOURCE RETAINED · **Affected:** WLD-18 Cinder-6 two isolated retaining blocks

A placement rule produced two individually rooted wall stubs rather than a coherent treatment of the cut bank.

Cause / uncertainty: Local physical feasibility and movement checks do not establish an appropriate architectural arrangement.

Fix / next action: Preserve the actual rejected model/native/editable files and native review images under rejected/cinder-6-stubs. Do not adopt that source. Redesign its bank as a whole before adding retention. Academy and Monastery also retain their sources because no justified wall fit was found.

Evidence and verification: development/levels/rebuild/retaining-shoulder-pass/rejected/cinder-6-stubs; captures/retaining-shoulder-batch/rejected-cinder-6-stubs; explicit all29roster decisions

Older-map obligation: Review every generated structural assembly visually; passing native probes cannot accept site design or runtime gameplay.

### DEV-472 — Institutional roof construction initially omitted Academy north cloister

**Status:** CORRECTED IN CURRENT ROOF CANDIDATES; FINISH AND RUNTIME GATES OPEN · **Affected:** Academy roof wing selection; Asylum and College complete aboveground footprint scope checked independently

The first selected-wing program enclosed its declared wings but missed the low north cloister between taller Academy ranges.

Cause / uncertainty: Checking only the chosen roof rectangles could not discover an omitted room footprint.

Fix / next action: Add the actual north-cloister pitched roof with closed gables/eaves. Independently ray-check all aboveground indoor/context room footprints; retain below-ground ceiling/ground enclosure as a distinct obligation. Current kernel optical probes begin above the exposed old caps:423Academy,396Asylum and513College rays were open before and all are blocked after. Preserve the superseded Academy GLB/native/editable scene, original contract and PNG with corrected archive path provenance.

Evidence and verification: development/levels/rebuild/institution-roof-pass/rejected/missing-north-cloister-roof; current construction contracts and verification/roof-optical-results.tsv

Older-map obligation: Recheck per-room roof coverage independently of selected wing lists. Native optical closure is not full lighting, seam, camera or gameplay acceptance.

### DEV-473 — Institutional new roof steps retain unfinished drainage flashing and runtime integration

**Status:** OPEN · **Affected:** Academy residential/cloister tiers; Asylum ward/pavilion steps; College library/bridges/residence roof joins

Closed roof forms are present but material refinement, drainage/flashings, detailed load framing and standing-height seam review remain unfinished. These sources are not loaded by normal gameplay dispatch.

Cause / uncertainty: This batch establishes architectural roof mass and opaque native geometry; source-to-game ownership/render/room/service adapters remain incomplete.

Fix / next action: Finish measured roof-to-wall steps, valleys, coping and drainage; review actual lighting and room volumes and integrate the exact sources without changing map availability until gates pass.

Evidence and verification: development/levels/rebuild/institution-roof-pass/roster.json; current roof overviews and native route/optical verification

Older-map obligation: Preserve Castle/House/Blackpine/Hollow Creek authorities; audit other roster roofs and all current candidate runtime bindings separately.

### DEV-474 — Mixed Prison cellhouse cap heights require enclosing roof-bearing upper facades

**Status:** CONSTRUCTED IN SOURCE CANDIDATE; DETAIL AND RUNTIME GATES OPEN · **Affected:** Prison west/east cellhouses; Church and Monastery distinct roof/tower construction retained in the same batch

Existing cellhouse room caps end at3m or6.4m while taller dayhall/bypass construction reaches6.8m. A continuous roof at the taller datum needs actual masonry enclosure below its eaves.

Cause / uncertainty: Room-first structural caps do not alone provide one coherent exterior cellhouse envelope.

Fix / next action: Build the full pitched cellhouse roofs with0.24m thick closed upper-facade bands seated into the existing ceiling/wall tops. Preserve the old occupied rooms and stairs; add no accessible attic or sockets. Production native optical controls show74previously clear horizontal band rays now blocked. Longitudinal ridge handling and closed hipped Church/Monastery caps use the current measured footprints. All9flights/9complete vertical routes and69flatlinks retain passing movement;15,600unchanged XYZ poses have zero new invalid samples.

Evidence and verification: development/levels/rebuild/secure-sacred-roof-pass/roster.json; individual roof_wings/facade_closures; verification/facade-optical-results.tsv and roof-optical-results.tsv; three native exported roof overviews

Older-map obligation: Coping, roof-step drainage/flashing, loads/framing, standing seam review and actual runtime integration remain open. Earlier roof candidates remain byte-preserved; their joins are not automatically accepted by this batch.

### DEV-475 — First facade-enabled roof build referenced an undefined imported-object list

**Status:** CORRECTED; FINAL THREE-SITE BUILD AND NATIVE CHECKS PASS · **Affected:** Optional FACADE_CLOSURES mode in shared headless roof constructor

First facade-enabled attempt stopped with NameError before model export while choosing an existing wall material.

Cause / uncertainty: New mode referred to originals; the helper import list is named oldobjects.

Fix / next action: Use the actual imported-object collection. Optional hip/long-axis/facade modes default off for prior builders. Build and inspect all three current sites, retaining original source resource bytes and passing native route/optical checks.

Evidence and verification: development/quality/evidence/roof-facade-initial-error.txt; tools/build_roster_enclosed_roofs.py and current secure-sacred roof contracts

Older-map obligation: No mature map or earlier model was regenerated by this change; preserve their independent physical/runtime obligations.

### DEV-476 — Diagonal route slots cut building corners instead of deliberate entrances

**Status:** SELECTED ENTRANCES RECONSTRUCTED; OTHER CIRCULATION OPEN · **Affected:** WLD-06 Wharf, WLD-08 Research, WLD-13 Cannery

Old diagonal flat supports passed corner masonry; roofing the existing layout perpetuated the defect.

Cause / uncertainty: Route research drove wall cuts before coherent building envelopes.

Fix / next action: Reconstruct cottage/store/mess/wash/processing perimeters with0.24m solid walls, intact corners and explicit2.45m-high lintelled doors. Square approaches through actual doors have continuous0.25m pavement, complete turns and terrain-rooted bearings. All11changed links pass continuously both directions. Five old corner crossing native optical rays now block. Preserve existing props/stairs and source resources.

Evidence and verification: development/levels/rebuild/entrance-construction-pass/roster.json; per-site construction.json; verification/current-flat-movement.json; verification/restored-corners-results.tsv; entrance-construction-batch native cutaways

Older-map obligation: All29packets need individual architectural review. Next: Wharf L07 fish-house wall conflict, Research L13 generator-to-lab and L19 lower tunnel, Cannery L09 gallery stair conflict. Academy off-landform and WLD-16 exact terrain ownership remain unresolved. No runtime acceptance or map unlock.

### DEV-477 — Research main tunnel cap projects above ground-level courtyard

**Status:** GROUND-LEVEL DECK CONSTRUCTED; EDGE GUARDS AND RUNTIME OPEN · **Affected:** WLD-06 Wharf, WLD-08 Research, WLD-13 Cannery

Tunnel floor-2.5 plus height3 put caps/wall tops at+0.5 through the y0 courtyard.

Cause / uncertainty: Underground enclosure and outdoor walking datum were inconsistent.

Fix / next action: Replace main tunnel caps with one0.30m-deep courtyard deck at0, underside-0.30, cutting only actual stair shafts. Trim main tunnel bearing wall tops to0 with closed solids. Floor stays-2.5 giving2.20m clearance; main tunnel envelope height2.5. Retain tunnel-n and exact saved terrain punches. All Research vertical routes pass. Edge guards, framing/load review and runtime ownership remain open.

Evidence and verification: development/levels/rebuild/entrance-construction-pass/roster.json; per-site construction.json; verification/current-flat-movement.json; verification/restored-corners-results.tsv; entrance-construction-batch native cutaways

Older-map obligation: All29packets need individual architectural review. Next: Wharf L07 fish-house wall conflict, Research L13 generator-to-lab and L19 lower tunnel, Cannery L09 gallery stair conflict. Academy off-landform and WLD-16 exact terrain ownership remain unresolved. No runtime acceptance or map unlock.

### DEV-478 — First entrance repair lacked continuous threshold and excavation-spanning support

**Status:** CORRECTED IN FINAL THREE-SITE CANDIDATE · **Affected:** WLD-06 Wharf, WLD-08 Research, WLD-13 Cannery

Initial shallow-bed attempt regressed eight previously passing flat-link directions. Imported seam splits also stopped initial basement wall trims.

Cause / uncertainty: Triangle-local shallow filling cannot bridge excavated ground or support full thresholds; imported UV seams require temporary welding before solid cuts.

Fix / next action: Replace fragmented beds with continuous thick approach slabs and actual soil-rooted bearings at at most2m spacing, plus existing structure overlaps at thresholds. Weld temporary imported seams before solid trimming. Final six flights/six vertical routes and eleven changed flat routes pass both ways, with zero complete-link direction regressions. Strict full-footprint samples retain1204invalid of14499 across all inspected routes; these remain open rather than accepted.

Evidence and verification: development/levels/rebuild/entrance-construction-pass/roster.json; per-site construction.json; verification/current-flat-movement.json; verification/restored-corners-results.tsv; entrance-construction-batch native cutaways

Older-map obligation: All29packets need individual architectural review. Next: Wharf L07 fish-house wall conflict, Research L13 generator-to-lab and L19 lower tunnel, Cannery L09 gallery stair conflict. Academy off-landform and WLD-16 exact terrain ownership remain unresolved. No runtime acceptance or map unlock.

### DEV-479 — Flat routing crosses Wharf fish house and Cannery transverse gallery stair

**Status:** SELECTED PHYSICAL CIRCULATION RECONSTRUCTED; OTHER ROUTES OPEN · **Affected:** WLD-06 Wharf perimeter and west stair; WLD-08 Research generator/lab aisle; WLD-13 Cannery gallery stair

Wharf west-pier path entered fish-house masonry; generator/lab link squiggled around a central stair; Cannery office approach crossed a transverse flight.

Cause / uncertainty: Earlier path research accepted local collision solutions without coherent deck, doorway and stair arrangements.

Fix / next action: Build a thick piled Wharf perimeter boardwalk around the existing stair, with steel spines/crossheads/girders rooted in exact native terrain and indexed guardrails around the actual joined perimeter. Boathouse and west stair return connect on this deck with1.5m clear routes. Rebuild Research generator/lab perimeter walls around aligned x7 doorway/aisle. Rehouse Cannery flight longitudinally within the mezzanine, provide a normal two-metre lower landing, restore old floor shaft and cut only the new real shaft. Fit library guards to flight, shaft returns and mezzanine edge. All5changed flat links and6complete vertical routes pass both ways; all strict sampled lanes on those changed links pass. Other routes retain993invalid of15267sampled poses.

Evidence and verification: development/levels/rebuild/circulation-construction-pass/roster.json; per-site constructed-circulation.blend/glb/hcs2; verification/stairs and current-flat-movement.json; native circulation-construction-batch cutaways

Older-map obligation: Preserve mature map authorities and all29packet roster. Research lower tunnel L19 remains an architecturally unaudited diagonal even though its native sweep now passes. Wharf east return/pier thresholds, Research courtyard excavation guards and full runtime binding remain open. Academy off-landform and WLD-16 terrain ownership remain explicit.

### DEV-480 — Initial guard runs crossed walk turns and initial landing capped descending treads

**Status:** CORRECTED IN FINAL THREE-SITE CONSTRUCTION · **Affected:** WLD-06 Wharf perimeter and west stair; WLD-08 Research generator/lab aisle; WLD-13 Cannery gallery stair

First Cannery lower stair approach ran sideways through a guard. Segment-based Wharf guards crossed their own right-angle turns; a square terminal pad overhung the first descending west-stair treads. Fragmented old-floor subtraction also created a nonmanifold attempt.

Cause / uncertainty: Terminal and perimeter construction were inferred independently from connected physical walking surfaces.

Fix / next action: Approach Cannery flight along its axis with a real2m lower landing. Derive Wharf guards from joined deck outer boundaries with deliberate connection gaps. Stop the new landing and its bearing exactly at the descending stair lip. Deduct the boathouse floor as a coherent footprint instead of tiny overlapping triangles. Six flights and complete routes now pass continuously both ways with zero prior passing direction regressions. Three production native optical controls through the vacated gallery shaft are clear before and blocked after.

Evidence and verification: development/levels/rebuild/circulation-construction-pass/roster.json; per-site constructed-circulation.blend/glb/hcs2; verification/stairs and current-flat-movement.json; native circulation-construction-batch cutaways

Older-map obligation: Preserve mature map authorities and all29packet roster. Research lower tunnel L19 remains an architecturally unaudited diagonal even though its native sweep now passes. Wharf east return/pier thresholds, Research courtyard excavation guards and full runtime binding remain open. Academy off-landform and WLD-16 terrain ownership remain explicit.

### DEV-481 — Imported parent axis reversed a reused Research bench translation

**Status:** CORRECTED WITH VERIFIED WORLD TRANSFORM AND ACTUAL FLOOR RECEIVERS · **Affected:** WLD-06 Wharf perimeter and west stair; WLD-08 Research generator/lab aisle; WLD-13 Cannery gallery stair

Initial local-x move shifted the indexed sample station east, compromising the new aisle. Its inherited placement also intersected the rebuilt north wall by0.08m.

Cause / uncertainty: Imported parent transforms were not accounted for when changing local position; prior prop bounds used the old wall arrangement.

Fix / next action: Apply explicit world translation x-2m and game-z+0.24m, preserving original asset mesh, UVs, pivot and materials. Exported bounds now x3.24..5.52 and z26.40..27.35, clear of the x7/1.5m aisle and north masonry endingz26.24. All four bottom corners and center ray-hit actual lab floor at0. Final aisle center and side-lane samples pass.

Evidence and verification: development/levels/rebuild/circulation-construction-pass/roster.json; per-site constructed-circulation.blend/glb/hcs2; verification/stairs and current-flat-movement.json; native circulation-construction-batch cutaways; verification/reused-station-support.json

Older-map obligation: Preserve mature map authorities and all29packet roster. Research lower tunnel L19 remains an architecturally unaudited diagonal even though its native sweep now passes. Wharf east return/pier thresholds, Research courtyard excavation guards and full runtime binding remain open. Academy off-landform and WLD-16 terrain ownership remain explicit.

### DEV-482 — Traversability studies were incorrectly extruded into physical architecture and coplanar room landings

**Status:** ROOT GENERATORS CORRECTED; THREE SOURCES STRIPPED; FULL ROSTER REMEDIATION OPEN · **Affected:** All route-derived blockout candidates require audit; current removal at WLD-06 Wharf, WLD-08 Research and WLD-13 Cannery; mature Castle/House/Blackpine/Hollow Creek preserved

Owner screenshot shows an amber approach strip flickering through Cannery hall floor. Cyan centerline supports and subsequent route-rooted bed fragments were physical mesh/collision and masked missing actual terrain/deck support.

Cause / uncertainty: Navigation guidance was treated as a construction command. build_stair_bays passed landings_authored_in_shell while stair_flights reads landings_authored_in_zones, creating long coplanar landing boxes through existing room floors. Narrow movement passes were incorrectly used as evidence of coherent architecture.

Fix / next action: Base blockout route_mesh now creates only non-rendering CURVE study objects, with no mesh or collision geometry. Correct the stair landing flag. Remove all rough route support, route-rooted fill fragments and line-end bearings from these three active candidates. Replace Cannery processing floor and ground approach boxes with one closed0.16m slab; retain actual stair/upper shaft/guards and reusable structures/assets. All6flights and6complete vertical routes pass both ways. Strict flat review now has4228invalid of15267samples,25passing complete links and28regressed directions over14links. Preserve failing evidence: previous strips supplied false support. Do not add ribbon geometry back or relax checks. Rebuild actual arrival terrain, quays/decks, thresholds and damaged architectural openings. Current candidate status is explicitly incomplete remediation, not accepted maps.

Evidence and verification: Owner Cannery screenshot; development/levels/rebuild/study-solid-removal-pass/roster.json; per-site study_solid_removal lists; verification/current-flat-movement.json and verification/stairs; native correction cutaways; tools/remove_study_solids.py; build_blockouts.route_mesh and build_stair_bays landing flag

Older-map obligation: Audit every remaining active candidate for physical study ribbons, coplanar landings and earlier route-driven wall/floor/roof cuts. Native checks do not validate architecture. Deliberately authored slabs/bridges also require sensible building/terrain review. Full production rebuild and runtime integration remain open; no unlock or held integration merge.

### DEV-483 — Dock and courtyard surfaces were incomplete around removed physical route guides

**Status:** SELECTED ARCHITECTURAL AREAS CONSTRUCTED; FULL REMEDIATION OPEN · **Affected:** Wharf quay/jetty/crossdock and east stair head; Research compound courtyard/forecourt; Cannery loading dock/service yard

Removing physical study strips exposed missing actual deck surfaces and unsupported ground-level outdoor circulation. Earlier narrow authoring surfaces also overlapped subsequent area slabs.

Cause / uncertainty: Prior deck fragments had been built around study meshes instead of authoring the complete architectural deck/yard footprints.

Fix / next action: Replace Wharf dock floor fragments with complete0.20m decks, actual stair-only voids, native-soil driven piles and a continuous orthogonal steel bearing grid. Enlarge east pier-head landing around the actual well. Build Research retaining/fill foundations and0.20m courtyard/forecourt/apron pavement, with guards on exposed retaining edges. Construct Cannery piled loading deck and chamfered service yard. Clip old Wharf porch/perimeter surfaces to terminate at real room/dock boundaries and cap perimeter guards at their jetty joins; remove obsolete rails within the joined deck. No route ribbon is restored. All6flights/6complete vertical routes pass both ways,40complete flatlinks pass with zero direction regressions from the ribbon-free source. Strict samples retain1333invalid of15525.

Evidence and verification: development/levels/rebuild/courts-and-docks-pass/roster.json; per-site area_construction with source-soil receivers, real pile bottoms and framing coordinates; verification/current-flat-movement.json and stairs; native courts-and-docks-batch captures

Older-map obligation: Arrival terrain is unfinished at all3sites; Research array/shelter and Cannery forest approaches remain unbuilt. Wharf outer guard continuity/underside/joins, courtyard drainage/finish and structure load/span detailing remain open. Other22active candidates still need guide-solid removal and architectural repair; preserve4mature authorities. No runtime acceptance, landscape/desquareify acceptance or availability promotion.

### DEV-484 — Research courtyard connections crossed actual descending stair openings

**Status:** SEPARATE COURT DOORWAYS CONSTRUCTED; RUNTIME OPEN · **Affected:** WLD-08 lab east face and dormitory west face; first Cannery wash approach and Wharf coplanar overlap corrected during same batch

Court links used the same faces/positions as descending stairs, so a ground-level straight approach crossed the real lower-flight opening. First Cannery yard reuse also led an old wash approach outside the chosen chamfered yard.

Cause / uncertainty: Earlier paths were treated as clearance commands rather than following actual doorway, floor and shaft arrangements.

Fix / next action: Close the obsolete z35 ground-level court wall openings and build normal2.4m-wide/2.45m-high doorways atz41, preserving the below-grade stair geometry. Lab circulation approaches its upper stair from the west; dormitory approaches from the east. Actual courtyard construction precedes guide adjustment. Refit Cannery wash approach through the authored yard. Final strict center/side samples on the new Research entrances pass and all stair chains remain passing. Production native controls show the two former above-shaft wall crossings now block; three formerly open surface controls block on real new area floors.

Evidence and verification: tools/build_site_courts_and_docks.py; courts-and-docks-pass/verification/new-surfaces-and-closed-stair-crossings-results.tsv; current-flat and stair checks; final native cutaways

Older-map obligation: Review every courtyard-to-stair interface across full roster. Lower Research tunnel diagonal, arrival/array ground, fixture/door interaction bindings and real rendering/collision/room/portal/acoustic/spawn/service integration remain open.

### DEV-485 — Arrival and field ground was absent beneath removed physical study supports

**Status:** SAVED GEOGRAPHIC EARTHWORKS CONSTRUCTED; LANDSCAPE AND RUNTIME GATES OPEN · **Affected:** Wharf settlement arrival/forecourt; Research arrival and equipment grounds; Cannery arrival and workers woodland edge

Ribbon-free maps retained unsupported arrivals, equipment-ground connections and woodland approaches despite useful building/dock construction.

Cause / uncertainty: Earlier physical study strips concealed elevations and native terrain that did not reach the actual occupied thresholds.

Fix / next action: Verify each current terrain mesh against its exact saved world, then sculpt real geographic clearings through TerrainLayerStack. Reopen and publish native terrain/OBJ in the existing editor scene; preserve source R16 and adaptive punched topology/cut rows byte-for-byte. Shape broad irregular arrival/settlement clearings, equipment grounds, and shallow undulating woodland earth; feather into retained landform. Remove obsolete Wharf cottage walk meshes where new soil owns the forecourt. Nonterrain native records remain byte-identical except those explicit walk removals. All3arrivals, equipment connections and woodland chains now pass continuous movement;49of51flatlinks and6complete vertical routes pass both ways with zero direction regressions. Woodland full-width checks at actual soil elevation pass522poses. Terrain material and landscape detail remain rough, not final acceptance.

Evidence and verification: development/levels/rebuild/site-earthworks-pass/roster.json; persistent worlds and program/features; verification/spatial-and-source-preservation.json; verification/woodland-actual-ground-verification.json; native site-earthworks-batch cutaways

Older-map obligation: Wharf L18 east-stair return and Research L19 lower tunnel still fail. Raw historical height0sampling retains343invalid of15525poses, including300woodland poses above actual undulating ground; do not confuse guidance Y with walking surface. Other22active candidates still need equivalent guide removal and construction. Academy off-landform and WLD-16 original terrain ownership remain open. No runtime/map availability promotion.

### DEV-486 — Protective terrain setback left small collision lips at real floor boundaries

**Status:** BOUNDARY GRADES CORRECTED; BROADER TERRAIN FITNESS OPEN · **Affected:** Arrival apron/floor joins across the selected3sites; Research equipment doors and Cannery workers doors

An initial4cm below-slab sculpt allowance created a lip at actual floor boundaries, preventing capsule movement onto the preserved floors. Initial writer verification also parsed the optional cut-row reason as a height.

Cause / uncertainty: The grade treated every point covered by a floor triangle as interior; it did not distinguish the outer surface boundary or authored-cut numeric columns from metadata.

Fix / next action: Use the union of actual floor footprints to keep buried earth below interior surfaces while meeting their outer edges at floor elevation. Update the same saved sculpt layer relative to original source, preserving idempotence and outside-feature heights. Parse only the first4cut coordinates; resume verification of already-saved layers instead of duplicating edits. All saved worlds reopen and retain exact punch topology, with all stored hole controls clear. Verify actual terrain feet separately from historical flat-height sampling. No collision constants or acceptance checks were relaxed.

Evidence and verification: tools/java/RebuildSiteEarthworks.java; tools/compose_site_earthworks.py; site-earthworks-pass/worlds/*/terrain-world/earthworks-verification.json; verification/stairs and current-flat-movement.json

Older-map obligation: Review joins against real floor edges and standing/crouched collision across all sites. Final drainage, edge materials, landscape/desquareify acceptance, canonical van arrival visualization and real-game renderer/room/portal/acoustic/spawn/service integration remain unfinished.

### DEV-487 — Clearance-driven flared tread wings and copied ceiling cuts produced incoherent Wharf stair wells

**Status:** WHARF STAIR CELLS RECONSTRUCTED; OTHER SITES AND RUNTIME OPEN · **Affected:** Wharf west/east stairs and undercroft roof; equivalent flared stair construction elsewhere requires rejection/rebuild

Owner screenshots show giant tapered tread wings without sensible supports, fragmented rails and ceiling holes copied from those taper outlines.

Cause / uncertainty: Earlier work widened treads to satisfy side-lane probes and used their shapes to cut floors/caps. This reversed the House method: real structural layout first, construction next, traversability as validation. Passing checks did not justify the architecture.

Fix / next action: Remove both old flights, tapered treads/stringers, fragmented end landings and obsolete roof/wall fragments. Build constant2.4m gross tread flights inside2.6m clear rectangular cells,14regular risers over2.5m, supported upper landings, shared continuous cellar floor,0.25m retaining walls and0.30m roof/coping structure. Main cellar walls terminate at roof underside; clear lower height2.2m. Grade guards sit on real walls/coping and flight handrails on continuous stringers. Rectangular roof penetrations follow the stair cells, independent of tread outlines. East upper handrail extension is0.6m, preserving the actual landing bypass. Both flights/complete vertical routes and all16Wharf flatlinks pass both ways;5157strict foot samples have zero invalid poses. Inspect exported close view before accepting any further work.

Evidence and verification: development/levels/rebuild/wharf-stair-cells-pass/roster.json; wld-06/rectangular-stair-cells.blend/glb/hcs2; verification/stairs and current-flat-movement.json; captures/wharf-stair-cells-batch/wharf-stair-cells.png

Older-map obligation: Other active maps need the same architecture-first stair/shaft audit; Research lower tunnel still fails. Owner explicitly requires cleanup of all irregular doorway cuts throughout the roster with consistent door-type dimensions, real jambs/lintels and full wall depth. Preserve mature authorities. Whole-map load/soil/finish/runtime and availability gates remain open.

### DEV-488 — Irregular room openings lacked coherent door types and full-depth frames

**Status:** THREE-SITE GROUND-ROOM DOORWAYS CONSTRUCTED; FULL ROSTER AND RUNTIME OPEN · **Affected:** WLD-06 Wharf5ground rooms; WLD-08 Research8ground rooms; WLD-13 Cannery8ground rooms

Oversized or irregular rough wall cuts and arbitrary headers did not describe usable architectural doorways.

Cause / uncertainty: Earlier wall cuts followed rough circulation allowances without explicit door schedules and common sill/head datums.

Fix / next action: Rebuild selected room perimeter walls as0.24m closed solids with nonoverlapping corners and explicit pedestrian1.05x2.10m, service1.80x2.30m and loading2.40x2.60m clear openings. Place38unique instances using existing indexed level_blockout_doorway jamb/head geometry, original UVs/materials and unchanged source master. Flush thresholds; one frame spans both0.24m wall halves at shared boundaries. Build architecture before adjusting Wharf boathouse/net entrance guides. Independent GLB reimport verifies frame sill/head/width/depth; native body sweeps pass114actual-aperture profiles both ways. All50previously passing ordinary links and6complete stair routes remain passing; zero direction regressions. Three native optical controls close formerly oversized gaps. No door dimensions or physics constants changed to accommodate broad guidance lane samples.

Evidence and verification: development/levels/rebuild/doorway-construction-pass/roster.json; per-site construction.json/authored-layout.json and clean-doorways.blend/glb/hcs2; verification/door-aperture-verification.json, exported-frame-dimensions.json, closed-oversized-openings-results.tsv, current-flat-movement.json and stairs; captures/doorway-construction-batch including standing shared-door close view

Older-map obligation: 22active packets pending individual doorway construction;4mature authorities preserved and not inferred audited. Selected basements/upper floors and Research lower tunnel remain unfinished. Raw broad guidance footprints retain515invalid of15648poses, including lane shoulders wider than actual door apertures; do not widen doors to pass those samples. Research/WLD-12 flared stairs still require architectural reconstruction. Door leaves, interactions, room/portal/acoustic/service binding and real-game integration are open. No unlock, runtime or whole-map acceptance.

### DEV-489 — Institution entrance blocks retained irregular circulation-cut walls and incomplete door frames

**Status:** THREE ENTRANCE BLOCKS CONSTRUCTED; OTHER ROOMS AND RUNTIME OPEN · **Affected:** School foyer/interview/visitor WC/office and corridor south face; Asylum admission/interview/records and spine south face; Prison reception/intake/records and front-link south face

Oversized/irregular study-cut wall openings, separate arbitrary headers and missing full-depth door assemblies survived outside the first waterfront doorway batch.

Cause / uncertainty: Rough traversal allowances had replaced an architectural doorway schedule; original headers lacked zone extras and needed explicit wall/side identity matching.

Fix / next action: Reconstruct10ground rooms plus3adjoining corridor faces around explicit1.05x2.10m pedestrian and1.80x2.30m service doors, thick closed masonry and complete jamb/head instances from the existing indexed standard doorway. Frames span both wall halves at shared openings. Remove original headers by actual wall/side identity through repair prefixes. Build full-width3m-deep entrance landings on solid plinths embedded below sampled original terrain at their outside bearing. Retain adjacent sealed-context walls and all access metadata. Normal approach guides follow the constructed room doorways.39actual-aperture center/side profiles pass continuous movement both directions,13exported frame dimensions/datums pass independent reimport, and three native optical controls show the old excessive opening heads now blocked at2.4m. All7flights and7complete stair routes remain passing. Final native overviews/standing entry views inspected.

Evidence and verification: development/levels/rebuild/institution-doorway-pass/roster.json; per-site clean-doorways.blend/glb/hcs2, authored-layout.json and construction.json; verification/door-aperture-verification.json, exported-frame-dimensions.json, closed-oversized-openings-results.tsv, source-preservation.json and stairs; captures/institution-doorway-batch

Older-map obligation: These are entrance blocks only: remaining institution rooms and upper/basement doors are unfinished. Roster retains3previous ground-room batches,3partial institution entrance blocks,19pending active packets and4mature authorities. Source world/native terrain, original floor/roof/stair/prop records except explicitly removed studies, zones/capacity/staging/punches and SEALED_CONTEXT remain unchanged. No whole-map or runtime acceptance, interactive door binding, availability promotion or held-parent merge.

### DEV-490 — Removing institution study solids exposes missing court and service-ground construction

**Status:** THREE EXPOSED LINKS PHYSICALLY RECONNECTED; OTHER OUTDOOR/GRADE WORK OPEN · **Affected:** Asylum L26 day room to court, Prison L080 sally port to yard, School L40 receiving to grounds; broader arrivals/outdoor routes on all3sites

Before removal, physical blue study strips ran through actual entrance floors and beyond thresholds. Several ordinary links relied on those strips rather than proper ground/yard construction.

Cause / uncertainty: The earlier studies became hard support geometry. School outdoor grounds also uses a broad envelope whose historical common route endpoint lies among actual building rooms, so its diagonal needs architectural reassessment rather than another hard ribbon.

Fix / next action: Remove all active rough-route-support, rooted-circulation-bed and arrival-end-bearing study solids from the three current site exports and native collision. Keep real stairs/floors and author complete entrance landings instead of reconnecting lines. Source-preservation check retains all native records outside explicit wall/header/study removals byte-for-byte. Final complete-flat regression check correctly FAILS:96of121active ordinary links pass both ways versus99before;6direction regressions on3links. Strict broad samples retain4076invalid of22458poses. Keep the unweakened failing check and exact failure contacts. Do not restore strips or widen doors to hide missing areas. Subsequent institution-courts-pass builds actual Asylum patient court, Prison secure yard and School teaching/play/service courts with closed paving and terrain-embedded foundations. The three newly exposed links now pass both ways. School normal exterior exits and relocated outdoor anchor produce103of121passing links versus96before, zero direction regressions. Seven complete stair routes remain passing. Native terrain, saved punches, original roofs/floors/stairs/props are preserved; new foundations exclude all actual ground/below-grade room footprints.

Evidence and verification: institution-doorway-pass/verification/current-flat-movement.json complete_link_regressions and source_bindings; current-flat-links.tsv and failure contacts; source-preservation.json; native entry images now show actual landing surfaces without blue strips

Older-map obligation: Construct real court/yard and service-ground interfaces, reassess School grounds endpoint outside the building and its normal entrances/exits, then rerun the affected checks. Previously failing arrival/perimeter and School upper links remain open; removing guides does not establish terrain/desquareify, floor/stair or runtime acceptance. Other active maps still need individual study-solid removal and architecture-first rebuilding.

### DEV-491 — School outdoor route hub lay inside actual building rooms and service exits lacked coherent exterior courts

**Status:** NORMAL EXTERIOR DOORS AND REAL COURTS CONSTRUCTED; RUNTIME AND REMAINING ROUTES OPEN · **Affected:** School grounds hub and west science/classroom, north gym and east receiving exits; Asylum patient-court doors; Prison sally-port yard door

The historical School outdoor envelope center at30,19 lay among the actual health/stair rooms. Exterior link diagonals cut through the building instead of using external circulation; removed guide solids exposed missing actual area surfaces at3sites.

Cause / uncertainty: Outdoor envelope centers and traversal studies were treated as physical circulation architecture.

Fix / next action: Construct School north playground, east receiving/gym service court, west teaching court and south forecourt as actual0.20m closed paving with foundations embedded below measured source soil. Exclude every existing ground/below-grade room, retained basement roof footprint, stair aperture and entrance landing from new construction; do not fill basements or duplicate existing floors. Build Asylum enclosed patient court and Prison secure exercise yard similarly. Reconstruct7normal exterior door faces with standard indexed frames and thick masonry. Relocate the School grounds circulation anchor to52,0,26 in its actual east service court; route west exits around the north playground and receiving exit directly to that court. All3previously exposed links now pass, plus4other newly continuous exterior connections:103of121complete ordinary links pass, zero regressions; all7complete stair routes pass.60actual aperture profiles across retained/new doors pass both ways;20exported frames satisfy independent dimensions/datums. Three native vertical controls previously clear now block on real new paving. Final native overviews inspected.

Evidence and verification: development/levels/rebuild/institution-courts-pass/roster.json; per-site constructed-courts.blend/glb/hcs2, authored-layout.json/construction.json; verification/current-flat-movement.json, source-preservation.json, door-aperture-verification.json, exported-frame-dimensions.json and new-court-surfaces-results.tsv; captures/institution-courts-batch

Older-map obligation: School L36 service-stair exterior start remains unsupported; L27/L33 upper routes, L35/L41/L42 arrival links still fail. Prison L065/L066/L084/L085/L111 and Asylum L19/L20/L71/L72/L73/L74/L76 remain incomplete. Actual runtime outdoor room ownership/portals/acoustics/spawn/service integration still open; the source circulation anchor does not establish runtime binding. Other institution doors and entire remaining roster require individual construction.

### DEV-492 — Raised institution court edges and unfinished arrivals require final grade transitions and protection

**Status:** CONSTRUCTION OPEN; NOT TERRAIN OR WHOLE-MAP ACCEPTANCE · **Affected:** School new court outer edges/arrival transition, Asylum patient court west edge and all3site arrival interfaces

Real paving is at building datum0while surrounding retained native soil is lower. School source soil under constructed areas ranges roughly-0.82to-0.30m; exposed retaining faces and unbuilt approach transitions remain.

Cause / uncertainty: This bounded batch constructs courts and foundations while retaining the original saved landform. Geographic arrivals, drainage and protected transitions remain separate necessary physical work.

Fix / next action: Give every court real0.20m depth, closed underside and continuous foundation below minimum measured soil; all sampled new court ground positions have actual soil receivers and no overlap with saved basement/stair holes. Preserve the original source terrain rather than edit a mesh without its saved world. Record raised-edge and arrival limitations explicitly. Do not call these courts landscape/desquareify, guarded-arrival or runtime acceptance.

Evidence and verification: institution-courts-pass per-site court_construction actual_soil_range/soil_range and constructed_polygons; native source-preservation.json and final overview images

Older-map obligation: Build appropriate supported stairs/landings or saved terrain grades, edge protection and drainage at exposed interfaces; include canonical van in arrival acceptance. Preserve saved hole punches, all basement headroom, existing assets and functional contracts. Do not bridge missing grades with extruded study lines. Full roster, final materials and runtime remain open.

### DEV-493 — Partial exterior wall repairs used shortened returns intended for all-new room corners

**Status:** CONSTRUCTED RETURNS CORRECTED; THREE NATIVE OPTICAL CONTROLS PASS · **Affected:** School west science/classroom and east receiving wall corner joins in institution-courts-pass

The first court checkpoint shortened W/E face ends by0.24m while adjoining retained N/S source walls were thinner/centered. This left small light/sight gaps near three corners.

Cause / uncertainty: A corner convention appropriate when rebuilding all4walls was reused for a partial single-face repair against retained walls.

Fix / next action: Extend each partial W/E face to its original full along-wall bounds, joining retained corner walls. Keep doorway locations/dimensions and all court/floor geometry. Export/native solids remain closed/positive. Three native rays that crossed the actual checkpoint corner gaps now block on the repaired wall returns; before geometry reproduced exactly from publishedd6c43dbgit object. Rerun final ordinary movement:103of121links pass with zero regressions. Seven complete stair routes and60actual door-aperture profiles remain passing;20frame dimensions verified after final reimport. Retained source records remain byte-identical.

Evidence and verification: institution-courts-pass/verification/corner-return-source.json, corner-return-input.tsv and corner-return-results.tsv; current-flat-movement.json; door-aperture-verification.json; tools/build_institution_courts.py

Older-map obligation: Audit all partial-face repairs against actual retained wall ends and thickness rather than applying a whole-room corner rule. Earlier institution entrance blocks replace whole rooms/N-S corridor faces; no inference that other older map corners are accepted. Runtime lighting, grade edges and other construction gates remain open.

## Required audit categories

- Corner, floor/wall, wall/wall, wall/roof and roof intersection light bleed
- Floor depth, underside normals, exposed edges and cut openings
- Models in walls/roofs, furniture overlaps and blocked routes
- Objects floating, sinking or incorrectly stacked on desks/shelves/counters; actual support-part bounds
- Fixture attachment, pipes/cables, circuit and physical interaction access
- Incorrect model purpose/scale, UV and texture defects
- Any newly added/discovered development error, including regressions caused by fixes

## Older-map review status

House, Farmhouse, School, Church, Castle and all other previously authored areas remain **not audited against the complete new checklist**. Existing individual passing tests remain valid within their scope. Review each location independently and attach evidence before changing its audit status. The 17 rough blockouts remain rough, not failed or completed playable maps.

## Continuity

At every map/model checkpoint: update open issues; check for related defects in reused assets; record new checks and remaining limitations; refresh the shared gallery; commit and push actual evidence. Before a fix is marked complete, rerun the checks it can affect (especially stairs after changing floor depth).
