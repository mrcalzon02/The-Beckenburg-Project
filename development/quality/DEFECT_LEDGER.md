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
