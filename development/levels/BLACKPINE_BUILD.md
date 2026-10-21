# Blackpine — first reusable remote build

21 October 2026. **Status: constructed and physically checked development graybox; not yet a selectable production map.** The complete rough roster was finished first. This is the only new remote site begun in the reuse-first sequence.

[Runtime/scene review](BLACKPINE_REVIEW.html) · [Editable Blender scene](blender/blackpine-graybox.blend) · [Actual source definition](blackpine-definition.json) · [Scene verification](verification/blackpine-scene.json) · [Crouch and shrub rules](OUTDOOR_REFUGE_AND_CROUCH.md)

## Normal buildings, a larger exposed place

The camp occupies an approximately 65×59 m investigation envelope plus its logging-road approach and standard van module. A 6×8 m foreman cabin and logging cabin, 6×8 m repair shed, 6×6 m washhouse, 8×10 m mess and 12×18 m mill keep human-scale functions. The bunkhouse is 8×12 m. Separation between these shelters, broad yard sightlines and only three shrub pockets create exposure. Rooms were not all enlarged to fill the site.

The sawmill has a real +3 m inspection loft, a continuous 1.5 m clear stair and guarded deck edges. The mill, mess/kitchen, cabins and shed have alternate exterior doors. The washhouse is deliberately a short terminal branch. Woodland enclosures and five interior trees have matching trunk collision. No automatic cupboard is installed in every room.

This construction refines the larger WLD-01 rough reservations; it does not overwrite the historical v1 atlas. `BlackpineLevel.java` and `LevelDefinition` now own its actual metres. The 15-zone adapter retains twelve meaningful interior volumes, yard, approach and van; `BlackpineLevel.map()` uses the legacy farmhouse enum solely as an unselectable development adapter. The geometry, revision, title, furnishings, dock and rendering cache identity are Blackpine's. A catalog/protocol promotion needs explicit integration and acceptance; no House/Farmhouse geometry is substituted as a new playable site.

## Reusable results

- Compact timber/service construction using existing source-preserving materials and real openings.
- A dispersed site with two-ended building access, open yard navigation and a guarded partial loft.
- Original CC0 shrub asset with a crouched central pocket; stable availability, lower body collision, host-owned posture and shared concealment rules.
- Authored-revision rendering caches, preventing a development definition from reusing another site's cached shell, and allowing cover-density changes to rebuild the right geometry.
- A repeatable runtime → editable Blender export, supported-route checks and reference-map regression checks.

## Checks actually completed

All Java 17 main/test sources compiled. `BlackpineGrayboxTest` verifies modest building dimensions, three sparse refuges, no indoor cupboard population, distinct rites, clear physical spawns, open/closed doors and continuous loft ascent/descent. The full swept flood from the van reached all fifteen spaces and all functional sockets over **301,405 supported positions**, including a 0.30 m approach to each shrub centre. The existing house/farm/school stair suite passed after fixing capsule approach tolerance at an upper-room stair boundary. Targeted posture, TCP interaction, cupboard, utilities, material surfaces, object handling and exact-ray checks passed.

Seven actual software-renderer images cover yard, cabin, mill, loft, building spacing and the same shrub from standing/crouching heights. The overview comes from the editable scene built from **106,851 exact runtime triangles**, including surrounding terrain, van, fences, trees and fixtures. That count and render timings are source measurements, not a performance budget pass. Blender reopens the saved scene and verifies triangle count, packed diffuse images, all socket transforms and three distinct rite rooms.

## Next unfinished gate — stay on Blackpine

Add believable working-mill machinery and timber stacks, sleeping/mess arrangements, windows, roofs and lighting refinement, payroll/manifest story props and readable site signage. Verify their native pivots and supported placement. Mark the investigation paths clearly without filling the exposed yard with plentiful hiding cover. Then test human/AI hunts, evidence baselines, three defended ritual outcomes, LAN site selection/rejoin, actual native-window controls and target-hardware performance before unlocking it. The full campaign, NPC allies and completed kneeling sprite animations remain unimplemented.
