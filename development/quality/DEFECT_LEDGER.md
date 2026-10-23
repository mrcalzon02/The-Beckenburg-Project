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
