# Exploratory model production run

**Current mill correction, 22 October:** the primary logway now ends at an
original circular headrig; the small workshop bandsaw has moved to the repair
shed. Four new models and a non-striped metal refresh of all twelve working-place
assets are documented in [the measured refinement and next gate](MILL_REFINEMENT.md)
and [visual review](BLACKPINE_MILL_REVIEW.html). Earlier descriptions below are
the historical first-run checkpoints; current manifests supersede their counts
and placements.

One original model at a time: source geometry and packed diffuse → native GLB → editable Blender source → studio inspection → real level placement/collision → physical/runtime check → scoped commit. Runtime scenery is shared through the existing instance renderer. No powered machinery simulation is implied by a static environment mesh.

## 01 / mill bandsaw

`level_work_bandsaw`: 2,114 source triangles, 176,700-byte GLB. A bolted plinth, stopped band wheels, returning-blade guard, cutting teeth and throat slot, adjustable feed fence, tension wheel, idle motor and disconnected lead explain its use and condition. Blackpine placement X42.6/Z33.8 is outside the mill's stair and central ritual reservation. Conservative physical bounds include its native extents. Reusable in industrial workshops, sheds, mine maintenance and later mill layouts.

[Manifest and hash](production/bandsaw.json) · [Editable source](blender/level_work_bandsaw.blend) · [Studio render](previews/level_work_bandsaw.png) · [Actual game view](previews/model-production/blackpine-level_work_bandsaw.png).

Next useful candidates: rail log carriage, repair bench and hand tools, sawdust collection/ducting, crew footlockers and survivor cooking/repair details. Keep working routes broad and existing rites/spawns accessible. These are usable art assets and checked static placements, not production acceptance of the entire map.

## 02 / rail log carriage

`level_work_log_carriage`: 2348 source triangles. Flanged wheels, bolted timber bearers, rails/stops, screw clamping dogs and a partly milled log with cut-end growth rings. Placed X43.2/Z30.6 in the sawmill, aligned toward the bandsaw and outside the ritual reserve. Original native source, packed diffuse, studio and real runtime views, and matching collision/support checks are complete. Static scenery; no wheel or log simulation.

[Manifest](production/log_carriage.json) · [Source](blender/level_work_log_carriage.blend) · [Studio](previews/level_work_log_carriage.png) · [Runtime](previews/model-production/blackpine-level_work_log_carriage.png).

## 03 / repair bench and hand tools

`level_work_repair_bench`: 715 triangles. Self-supporting timber bench/backboard, vice, spanner, hammer, toothed hand saw, screwdriver, plane, sharpening stone, oil tin and saved offcut. Placed X47.0/Z46.7 in the repair shed with generator and sawhorse clearance. Native source reopened, UV and packed textures verified, runtime mesh/support/collision and clear-spawn/rite checks passed. Static tools are not new inventory pickups.

[Manifest](production/repair_bench.json) · [Source](blender/level_work_repair_bench.blend) · [Studio](previews/level_work_repair_bench.png) · [Runtime](previews/model-production/blackpine-level_work_repair_bench.png).

## 04 / crew footlocker and personal effects

`level_work_crew_footlocker`: 628 triangles. An open hinged trunk, rusted bands, faded crew card, folded shirts, personal photograph/envelope, thermos, spare boot laces and mending packet. Two supported instances sit beside surviving bunkhouse beds at X13.3/Z24.4 and X10.4/Z24.2. Native source/UV/packed images and physical support/bounds, clear spawns/rites and readable records pass. Static original kit; no trunk inventory or lid simulation.

[Manifest](production/crew_footlocker.json) · [Source](blender/level_work_crew_footlocker.blend) · [Studio](previews/level_work_crew_footlocker.png) · [Runtime](previews/model-production/blackpine-level_work_crew_footlocker.png).

## 05 / hearth tending kit

`level_work_hearth_tools`: 1118 triangles. Free-standing rack with shovel, poker and tongs, a lidded cold-ash pail, wire handle and split kindling. Used beside the foreman and mess fireplaces. Reopened native source, UV/packed images, full-source bounds, floor support, clear spawns/rites and readable-record checks pass. Tools and ash are static scenery.

[Manifest](production/hearth_tools.json) · [Source](blender/level_work_hearth_tools.blend) · [Studio](previews/level_work_hearth_tools.png) · [Runtime](previews/model-production/blackpine-level_work_hearth_tools.png).

## 06 / washhouse boot and towel rack

`level_work_boot_rack`: 1140 triangles. Slatted open drying rack, two draped towels, folded cloths and the existing original worn boots with preserved UV/materials. Placed at X11.50/Z38.8 against the washhouse east wall, facing into the room and outside the central ritual footprint. Source reopens with packed images, runtime native bounds/support and clear-spawn/rite/reading checks pass.

[Manifest and reused-source hash](production/boot_rack.json) · [Source](blender/level_work_boot_rack.blend) · [Studio](previews/level_work_boot_rack.png) · [Runtime](previews/model-production/blackpine-level_work_boot_rack.png).

## 07 / survivor cookbox

`level_work_survivor_cookbox`: 1048 triangles. Salvaged slatted crate and supported lid, soot-dark lidded pot and wire bail, enamel mug, wrapped ration, spoon, cooking rag and dented lunch tin. Placed X34.9/Z9.0 in the recently occupied logging cabin. Source/UV/packed-image reopening, native bounds, support, collision, spawn/rite and readable-record checks pass. Static cold cooking gear, reusable in remote refuges and campgrounds.

[Manifest](production/survivor_cookbox.json) · [Source](blender/level_work_survivor_cookbox.blend) · [Studio](previews/level_work_survivor_cookbox.png) · [Runtime](previews/model-production/blackpine-level_work_survivor_cookbox.png).

## 08 / camp entrance and return-route sign

`level_work_camp_sign`: 2021 triangles. Bearing posts and iron shoes, three name planks, original readable mesh lettering, a later road-survey warning and direction arrow. Placed X40.5/Z58.3 on the arrival verge. Every source vertex stays more than 1.25 m from baked path centrelines. Native/source support and collision tests pass; the sign explicitly says the road is surveyed and the site is not cleared.

[Manifest](production/camp_sign.json) · [Source](blender/level_work_camp_sign.blend) · [Studio](previews/level_work_camp_sign.png) · [Runtime](previews/model-production/blackpine-level_work_camp_sign.png).


## Construction revision / 21 October

All eight first-run assets were revisited after the visual review. The direction arrow now bolts to a crossrail between the posts; lettering sits against the sign faces. The cookbox has a connected, upward-facing concave spoon and supported lunch tin. The footlocker lid meets its hinges and its belongings rest on a tray. Boot-rack shelves have end bearers; towels wrap over the real rail. Tool hooks reach the repair-bench backboard; spanner, saw handle, vice, screwdriver, stone and lower-shelf offcut have connected/supporting geometry. The bandsaw has open cast wheel rims and real spokes; the carriage has an actual truncated sawn log and wheels at rail-head height. The separate bedroll pillow rests in a clear head space, with quilt underside and connected zipper pull.

The regenerated eight assets total 12,387 source triangles; per-asset current counts/bounds are in their manifests (superseding the initial counts above). Native construction audit covers nine models including the bedroll. Separate loose kindling is floor-supported. Runtime native bounds, physical supports and spawn/rite clearances pass. Conservative part bounds are not a substitute for final visual acceptance, and these remain revised development art pending the user's review.
