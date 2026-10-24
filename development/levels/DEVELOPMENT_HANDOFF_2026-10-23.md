# Latest terrain-first checkpoint — 23 October 2026

User corrected construction order: terrain/site first, meaningful height variation,
footpaths worn into surrounding ground, irregular outdoor areas and natural edge
barriers. Preserve the existing pavilion/models; fit them to the terrain. Do not
resume random structure/detail additions before runtime site gates pass.

New native terrain source/GLB and full scene are built and visually reviewed.
Road Y1.5, shore Y-1.3, lake bed Y-3.1; actual narrow incised treads, rolling land,
meandering creek/shoreline, graded pads and deck landings. Original layout and all
17 rough GLBs/source checkpoints retain their hashes. West table/fire ring have
explicit vertical refits in the new scene; earlier native sources stay intact.

Native checks: 10,377 full-width route samples; 72 pad samples; 113 connected
wet-channel samples; two actual shoulder/depth checks; worst local walking grade
0.192827 under 0.20 authoring limit. Actual GLB loading passes within unchanged
engine budgets. These are native/asset gates, not complete runtime/human acceptance.

New user water candidate: Immaculate Lift Studio's PSX Style Water Surface. Primary
page verified: Godot4 shader MIT; example textures separately CC Attribution.
Archive/license contents are not yet acquired or ported. See WATER_REQUIREMENTS.md.
Required layered transparency, depth colour/foam, sorting and submerged-object review
remain pending. Current native water is explicitly an opaque foundation preview.

Next: integrate terrain/water/crossing ownership and swept movement in the runtime;
place terrain-following hedges, stone/retaining banks, fences and fallen-log barriers
from the recorded irregular site envelope. Keep approach/drainage open and avoid
invisible walls. Validate the site before more facility/detail production.
Pavilion-services files are preserved paused WIP, not accepted circuit implementation.

GitHub access probe passed with escalation; previous d2fe095 is verified on main.
Publish this terrain checkpoint, then verify the refreshed gallery and save readback.
Do not treat the previous 469-image live report as verification of later batches.

---

# Latest continuation — 23 October 2026

The user explicitly created the larger active goal to finish all map development,
models, review and detail. See MAP_COMPLETION_GOAL.md for the existing 23-map roster
and completion gates. Continue from the current map; do not treat checkpoint
publication as completion of the overall goal.

GitHub recovered: 962d007 was pushed normally to main and remote readback matched.
The 469-image gallery was read back publicly: all PNG URLs/dimensions plus 23
full evidence hashes passed. The old connectivity block below is historical.

New checkpoint: Hollow Creek pavilion architecture. Original 2040-triangle kit,
editable asset and additive full scene. Actual BVH checks pass for 158 timber/support
contacts; 24 roof ray samples confirm 120mm thickness and outward upper/downward
soffit normals; closed solids; 1024 standing-surface coverage points; conservative
post/route/table/ritual clearances; prior facility meshes/transforms and protected
sources unchanged. Java17 compilation and both campground GLB tests pass.

Visual review caught underlit joint captures (DEV-017); authoring-only fill fixed
the review visibility, with before evidence retained. Scene-load material lifetime
failure (DEV-018) was fixed before scene publication. Native/runtime site lighting
and circuit acceptance remain open. New gallery count: 474 originals, local check
passes; publish and verify this new batch before claiming public delivery.

Next gate: supported pavilion utilities/detail, then tent/platform and cabin/
bathhouse facilities from recorded requirements. Whole-site authored runtime
collision/rooms/circuits/van and walkthrough/LAN/performance remain pending.
Do not reuse provisional pink/cyan planning paths as finished supported routes.
Do not unlock the map or claim all-map completion from native geometry alone.
Exact run token counts remain unavailable.

---

# Development checkpoint — 23 October 2026

Active checkout: The-Beckenburg-Unified, integration/unified-map-engine.
Preserve the original and mission-board checkouts; neither was rebuilt or reset.

## Completed and verified locally

- Replaced the incorrect D.O.T.S. capsule-wide pattern with deterministic projector
  rays. Each ray chooses one nearest receiving room/prop triangle or nearer front
  ghost collision hit. Removed generic floating points and body sampling. Tier I
  remains still; higher tiers pan. Placement heading and ghost floor elevation
  replicate, with backward-readable placement/passage frames.
- Added nearest-hit, blocker, range, miss, one-sided, heading and static/panning
  checks. Actual paired runtime captures show room projection and interception;
  actual photographs preserve the projected marks. DEV-013 records the correction.
- Java 17 compilation and all 21 focused optical-evidence checks pass, including
  real loopback TCP. See verification/optical-evidence.json. Native OpenGL,
  moving-ghost human review, multi-PC LAN and performance remain unaccepted.
- Camp Hollow Creek: two original models (picnic table 1136 triangles; fire ring
  1760), editable sources, runtime GLBs and four additive native placements.
  Independent Blender reopening validates contacts, clearances, UVs and unchanged
  source layout/17 rough blockouts. Actual runtime asset loading passes.
  See hollow-creek/README.md and verification/hollow-creek-facilities.json.
- Gallery locally indexes 469 original PNGs with dimensions and SHA-256. New
  captures visually inspected: paired DOTS runtime, campsite and pavilion overviews.
  No claim of runtime campground integration or unlocked mission.

## Delivery gate and next work

GitHub fetch and gh API failed on 23 October with DNS/connection errors for
both github.com and api.github.com. Connector github.get_repo also unavailable.
Local work must be fetched/reconciled, pushed without force, and read back before
calling it published. Then wait for Pages and run tools/verify_development_gallery.py;
commit the resulting actual deployed verification report. Do not confuse the
previous live report with this new 469-image checkpoint.

Camp's next gate is open-pavilion architecture, tent/platform and existing map
model inventory, followed by unified collision/rooms/circuits/van and runtime
route/optics/LAN/human checks. Blackpine's unfinished native/human gates stay open.
The baseline CastleHubIntegrationTest sky assertion remains recorded, not fixed.

Exact input/output/cached token counts for this resumed run are unavailable.
No token counts have been inferred from account percentages.
