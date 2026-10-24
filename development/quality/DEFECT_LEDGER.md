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

Fix / next action: Place split wood on actual grate bars, align table fixings to measured rails at X±.5, lower charred timber onto actual ash. Preserve clean sources. Actual ray checks pass for three grate/log contacts, four tabletop/bearing fixings and three ash/log contacts.

Evidence and verification: development/levels/previews/defects/DEV-054-before-firewood-grate-support.png; verification/hollow-creek-refined.json; hollow-creek-refinement-parts.json

Older-map obligation: Audit actual support parts on every older source/placement; this refined-only correction does not establish unaudited other-map acceptance.

### DEV-055 — Delivery verifier compared Git-normalized text to Windows worktree bytes

**Status:** fixed-verification-comparison · **Affected:** Refined source publication verification

Binary source/model downloads passed, but refined-models.json failed an exact raw hash comparison.

Cause / uncertainty: Git stores normalized LF text while the Windows worktree contains translated CRLF; the verifier incorrectly treated worktree byte identity as commit identity.

Fix / next action: Compare every actual download to the immutable committed Git blob with git show commit:path. Keep exact length/hash assertions for every file. Final46 actual source/export/review downloads match the committed bytes.

Evidence and verification: verification/hollow-creek-refined-delivery.json; final immutable GitHub download verification

Older-map obligation: Use committed bytes for remote delivery checks when text normalization applies. Preserve raw binary equality checks; do not waive real source/export mismatch.

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
