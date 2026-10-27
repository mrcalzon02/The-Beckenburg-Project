# Blackpine: first reusable remote location

21 October 2026. Constructed development site with exterior, terrain and first environmental story dressing. **Not unlocked or accepted as a production map.** All 23 rough map packets were completed before beginning this reuse-first build.

[Visual review](BLACKPINE_REVIEW.html) · [Editable full scene](blender/blackpine-dressed.blend) · [Current authored definition](blackpine-definition.json) · [Story chronology and room ledger](SITE_STORYTELLING.md)

## Normal buildings, an exposed place

The investigation yard is approximately 65 × 59 m, plus its road approach and canonical van. Human-scale cabins, an 8 × 12 m bunkhouse, 8 × 10 m mess, 6 × 6 m washhouse and 12 × 18 m mill remain separated by exposed working space. The mill has a continuous 1.5 m stair to a guarded inspection loft, three metres above its own foundation. The camp retains three sparse outdoor refuge shrubs and no automatic indoor cupboards. Ctrl crouching, lower collision and host-owned concealment remain implemented.

Twenty original camp/woodland meshes supply the sealed covered well, two supported patios/canopies, cold fireplace/chimney pairs, benches, timber stacks, sawhorses, rain barrels, varied low planting and forest details. Six real windows cut the walls. Four staggered forest bands contain 99 backdrop trees, with near undergrowth, deadfall, boulders and banks. The road corridor remains open. [Library and editable sources](WOODLAND_DETAILS.md).

## Terrain and plant support

The far edge rises exactly 1.78 m above the road: a slow overall grade, with additional local shoulders and flattened building/patio foundations. A half-metre grid with 20,698 vertices supplies both rendered triangles and physical support. Soil and needle diffuse is baked into a single 1024 px terrain image along the worn routes; standalone path stamps are no longer placed. Measured shrub/log clearance from route centrelines is at least 1.808 m, beyond the worn core.

Grass and flower bases conform to the same terrain while stems remain upright. Stable foliage seeds vary height, independent width, crown asymmetry, rotation, gentle lean/twist and sixteen leaf texel palettes. Lower solid trunks and actual refuge pockets keep their collision contract. Under-ground checks recover players, AI and loose entities to validated supported positions, including graded ground, stairs, upper floors and the van. [Implementation and checks](TERRAIN_AND_RECOVERY.md).

## First story layer

Nineteen new reusable story meshes add pens/pencils, papers and journals, calendars, photographs, old coats/boots/clothes, surviving beds, a recent bedroll/food-waste cluster, superficial floor/wall damage, damp, leak buckets, boarded windows and practical heating/services. Twelve room histories explain function, heat, condition and recent use. Nine supported records use the existing E reading interaction. The foreman's desk was moved clear of the fireplace.

The authored story layers the November 2009 closure, years of weathering, a three-night February 2025 shelter stay and a March caretaker inspection. Payroll and dispatch headcounts remain an unresolved discrepancy. None of these items identify the haunting. Wood fires have real flues; the washhouse oil unit heats water only; work rooms remain cold. Water and fuel are isolated, while the lighting branches have a maintenance power inlet. Plumbing, heat and rubbish are scenery, not newly simulated systems. [Complete room ledger, reusable kit and backport principles](SITE_STORYTELLING.md).

## Renderer and validation

Shared scenery templates and conservative instance culling preserve the existing menus, HUD, controls, textures, lighting and character drawing. Blackpine currently has 768 scenery placements using 46 templates: 116,652 expanded scenery triangles versus 20,226 shared source triangles. Six-view reference comparison matched all 384,000 pixels. Measured CPU medians remain approximately equal; this is geometry sharing and culling, not GPU instanced draw calls or a target-hardware frame-rate acceptance. [Measured renderer report](verification/scenery-instancing.json).

An independent scoped-source compile excludes unrelated unfinished workspace lighting edits. Physical/support, exact reading-ray, foliage, rendering, crouching, stairs, entity recovery, source material and reference-map checks pass. Real TCP crouch/interaction and existing HUD tests pass. Two older assertions in pursuit/room-lighting tests also fail from the exact pre-change source; [baseline reproduction](verification/pre-existing-dressing-checks.json) records them rather than calling the whole suite green.

Swept capsule navigation from the van reaches all fifteen zones, functional sockets and story-record approaches over 296,207 supported positions. Exact source export contains 230,227 textured triangles including full surrounding landscape and van. The editable export expands instances for authoring. Native validation reopens all 39 new original sources and the complete scene, checking hashes, packed images, runtime triangles and every socket transform. Daylight and bright interior images are temporary art inspection lighting; runtime captures retain actual night lighting.

Reports: [isolated source/regression/export](verification/blackpine-committed-source.json), [TCP and HUD](verification/blackpine-network-ui.json), [story support/interaction](verification/blackpine-story.json), [terrain/recovery](verification/blackpine-terrain.json), [native validation](verification/blackpine-dressed-scene.json), [foliage](verification/foliage-variation.json).

## Remaining gates

Stay on Blackpine for working-mill machinery, exterior site signage, finer weather/lighting composition and fuller room dressing. Human/AI hunts, defended ritual outcomes, evidence balance, LAN site selection/rejoin, native-window controls and target-hardware performance still need acceptance before production unlock. Earlier maps retain their current dressing until their individual stories are authored; the reusable kit and principles are ready for later backports. Historical graybox scene and atlas packets remain available.

The development adapter uses the old farmhouse enum only to construct an unselectable custom definition. Blackpine's coordinates, title, revision, rooms and rendering cache identity are its own; it is not a renamed farmhouse. The canonical van placement remains unchanged.
