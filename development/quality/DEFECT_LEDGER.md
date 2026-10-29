# Development defect ledger

Every discovered error, regression, placement problem or unresolved issue belongs here. `defects.json` is authoritative; regenerate this view with `python tools/build_defect_ledger.py`. A scoped fix never certifies unaudited maps.

[Required fitness checks](MAP_FITNESS_CHECKLIST.md) · [Screenshot gallery](../../beckenburg-development-gallery.html)

## Required issue record

Stable ID, symptom, cause or uncertainty, affected sites, reproduction/evidence, fix/status, verification and older-map audit obligations. Add discoveries immediately, including issues introduced by fixes; do not delete failed evidence.

## Recorded defects

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

**Status:** code-change-pushed-awaiting-gpu-capture-and-installed-review · **Affected:** Castle retail room, all 129 tiered plaques; desktop first-person HUD

The user-provided 2026-10-29 screenshot (2048×574) shows equipment signage reduced to white pixel fragments on modern display output. The desktop had a default 200p world framebuffer with nearest-only glyph sampling; native HUD fonts were fixed at 12–14 physical pixels. The window size and 3D rendering size were separate.

Source commit `a28b6c2a6760404a13942364f44d976c003254e5` adds selectable GPU 1080p rendering (720p software fallback), new-profile 540p GPU/360p software defaults, native HUD scale, aspect-preserving render-size caps and linear filtering only on Castle shelf text. Saved retro quality remains opt-in. This is a **source fix candidate**, not yet accepted visual legibility or installed gameplay.

Verify using exact-position Castle GPU screenshots at retro, default and Full HD presets, measuring real sign readability at aisle distance, performance and retained PSX world appearance. The focused, GPU, multiplayer and gated installer jobs for `a28b6c2` were in progress at logging. Audit other map signage and van/evidence screens individually rather than assuming universal closure.

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
