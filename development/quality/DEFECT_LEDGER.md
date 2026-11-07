# Development defect ledger

Every discovered error, regression, placement problem or unresolved issue belongs here. `defects.json` is authoritative; regenerate this view with `python tools/build_defect_ledger.py`. A scoped fix never certifies unaudited maps.

[Required fitness checks](MAP_FITNESS_CHECKLIST.md) · [Screenshot gallery](../../beckenburg-development-gallery.html)

## Required issue record

Stable ID, symptom, cause or uncertainty, affected sites, reproduction/evidence, fix/status, verification and older-map audit obligations. Add discoveries immediately, including issues introduced by fixes; do not delete failed evidence.

## Recorded defects

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

BlackpineGrayboxTest failed at ascent step 113 immediately after adding slab collision.

Cause / uncertainty: The swept capsule overlaps the final riser before its feet reach landing height.

Fix / next action: Allow only an authored stair-to-slab landing contact within 200mm of the top and at the actual ramp elevation; other slab edges remain opaque/solid.

Evidence and verification: BlackpineGrayboxTest failed before transition correction and passes afterward; swept route test still reaches all 15 zones over 292910 positions.

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

**Status:** implemented and verified by42 packaged Castle/editor/rite checks; exact-source remote acceptance pending · **Affected:** Castle native document loading, saved layout generation and explicit editor rebuild

Unchanged old defaults do not receive newer rooms/fittings; actual old structure-v1 loader changes an authored chair x16.0 back to default15.9.

Cause / uncertainty: No built-in content lineage was stored; new defaults are replaced by incoming records. Structure-v1 loading unconditionally selected defaults, discarding valid authored payloads.

Fix / next action: Freeze three actual source-generated baselines; automatically migrate only exact known built-ins with original-byte backups. Keep changed scenes untouched unless explicitly rebuilt by a three-way native editor transaction. Retain edits/deletions, metadata, shared undo and explicit save; reject new masonry/model/support/nav conflicts without modifying source data. Store rebuild generation so current deletions are never resurrected.

Evidence and verification: LegacySceneLossControl old packaged native loader fails with x15.9; corrected native loader retains16.0. CastleRebuildUpgradeTest covers original/pre-furnishing defaults, modified/deleted props, source additions, light/geometry/paint/nav/station/spawn, shared undo/redo/save, actual wall/new-statue/support negatives, current deletions, authored-v1/partial/future source preservation.

Older-map obligation: Castle-specific native document migration only. Other map codecs and arbitrary custom scene visual fitness remain individually unaudited; no global migration or map acceptance.

### DEV-292 — Native rebuild conflict validation missed fully contained retained objects

**Status:** implemented and verified by42 packaged Castle/editor/rite checks; exact-source remote acceptance pending · **Affected:** Castle explicit saved-scene upgrade validation

A retained book on its supported 8cm plinth could be swallowed by the newly placed keeper base without any triangle edge crossing.

Cause / uncertainty: Triangle-edge intersections alone do not detect volume containment or coincident geometry.

Fix / next action: Validate welded closed components using solid-angle winding in both directions; reject identical transformed triangle geometry. Preserve surface contact tolerance. Roll back the complete native transaction on conflict.

Evidence and verification: Actual retained supported book negative failed before correction (castle-upgrade-rejected-containment.log). Corrected native test rejects containment; additional exact-crate duplicate control included.

Older-map obligation: Castle saved-scene upgrade only; other map/model intersections remain individually unaudited.

### DEV-293 — Development gallery deployment times out while syncing oversized whole-repository artifact

**Status:** open; exact failed deployment diagnosed; source captures remain committed · **Affected:** Shared GitHub Pages development gallery at main25578d283ec4a77971229172098f267da4c66ad3

Pages42339395744 builds successfully then exceeds deployment deadline syncing files. Artifact3883804673 bytes exceeds advertised1GB allowance.

Cause / uncertainty: Legacy root Pages publishes the entire source repository including binary production assets and all historical review artifacts.

Fix / next action: Preserve all original evidence and diagnose a bounded gallery publication artifact; independently read back actual deployed HTML/index/images before accepting deployment. Do not infer deployment from source upload or gallery-index success.

Evidence and verification: gh run view42339395744 --log-failed: artifact size warning, syncing_files and timeout; main installer42339396543/gallery42339396635 pass. Full42339396539 still live when observed.

Older-map obligation: Shared publication issue affects all locations; no loss or acceptance of their runtime content is inferred.

### DEV-294 — GPU UI smoke rejects valid van exits needing diagonal WASD input

**Status:** diagnostic correction verified locally; actual corrected OpenGL run pending · **Affected:** GPU live UI van movement planning, House/Farmhouse/School

Exact source2293b75f GPU42341030548 passed Castle/11-map renderer checks but failed Briar House live movement with no short collision-valid WASD route.

Cause / uncertainty: Smoke planner tries only W/S/A/D individually. At the actual House spawn148/360 camera headings have no straight .6m single-key route despite valid two-key diagonal movement in the narrow van aisle.

Fix / next action: Plan all8 actual normalized WASD combinations and press/release both real keys for outward and return movement. Preserve .6m swept native-body candidate, >.5m route threshold, >.30m actual forward movement, <.22m return and real X extraction. Include site/coordinates/elevation/yaw in genuine no-route failures.

Evidence and verification: Actual old single-key reproduction148/360; actual corrected production GPU planner passes1080 real House/Farmhouse/School headings,444 needing two keys. Sources/logs in verification/van-input-recovery-2026-11-06. Fresh corrected GPU acceptance required; no geometry/physics threshold was changed.

Older-map obligation: This corrects the GPU diagnostic, not map geometry. Other location van routes and physical-hardware performance remain independently required. Preserve existing van implementation.

### DEV-295 — Windows Git checkout corrupts frozen native Castle baseline digests

**Status:** exact Windows-style checkout correction verified locally; fresh Windows/native/full acceptance pending · **Affected:** Castle saved-native rebuild resource loading on core.autocrlf=true installations/CI

Full42341031882 actual transcript is342passed/1failed despite overall workflow success: CastleRebuildUpgradeTest cannot load original baseline because its immutable SHA changed.

Cause / uncertainty: Frozen properties were treated as normal text; Windows checkout converts LF to CRLF before packaging. The runtime byte-integrity guard correctly refuses the corrupted baseline.

Fix / next action: Mark only the frozen runtime/source baseline property paths -text in.gitattributes, preserving exact authored bytes and unchanged SHA guards. Add actual historical saved-document upgrade test to required native editor host command, where failure is not advisory.

Evidence and verification: Actual git core.autocrlf=true checkout before pin reproduces all3bad CRLF digests; after pin all3match original immutable hashes and all3source copies. Verification sources/raw failing full transcript in verification/castle-baseline-checkout-2026-11-06. Original43local packaged bodies pass with canonical bytes, but this does not establish fresh remote Windows acceptance.

Older-map obligation: Castle frozen resources only. Individually audit other fingerprinted text assets; do not normalize arbitrary user scenes or infer old map codecs/other packaging fixed.

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
