# Exploratory model production run

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
