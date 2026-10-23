# Blackpine: supported mill worklights

23 October 2026. Refinement of the existing authored site on unified main.
The mill's next recorded gate was lighting and composition. This iteration
adds one reusable original support model and four working light placements.
No existing map is replaced; the 17 rough blockouts retain their prior state.

[Matched runtime views](BLACKPINE_WORKLIGHTS_REVIEW.html) ·
[Runtime checks](verification/blackpine-worklights.json) ·
[Native checks](verification/blackpine-worklights-native.json) ·
[Editable full scene](blender/blackpine-worklights.blend)

## Design and placement

The circular headrig remains the mill's primary saw, aligned to its carriage.
The small bandsaw remains in the repair shed. Worklight positions follow those
facilities and the open circulation aisle rather than the historical rough plan.

| Facility | Local placement before common terrain grading | Fitness requirement |
| --- | --- | --- |
| Logway pendant | X43.2 / Z26.7, mounting Y4.56 | Read the logway without intruding on standing circulation |
| Headrig pendant | X43.2 / Z34, mounting Y4.56 | Illuminate the circular blade and stopped work position |
| Approach pendant | X49 / Z24, mounting Y4.56 | Read the aisle; stay outside the stair/loft footprint |
| Under-loft fitting | X48.5 / Z35, mounting Y2.84 | Emit below the actual deck, preserving upper-floor ownership |

All four belong to the existing mill circuit. The inspection loft retains its
separate circuit. Terrain grading moves roof attachments, fixtures, rooms and
collision together. Existing portals, evidence, rites, furniture and van dock
stay in their authored coordinates.

`level_mill_suspension` is an original CC0 model: 1.08 m wide, 0.16 m deep,
1.40 m drop and 272 triangles. Its origin is the ceiling attachment plane;
the mesh extends downward. Do not floor-normalize it. Plates, attachment screws,
threaded drops, nuts and bearing crossbar carry the existing utility fitting.
A roof junction enclosure, protected electrical drop and lower terminal explain
the electrical connection. These are static models using the existing light
and circuit systems.

## Iterative review and results

1. Measured the existing utility fitting and authored roof/deck planes before
   creating the support. The crossbar's lower face and fitting mount coincide.
2. Compiled every current Java main/test source with JDK 17. Fifteen scoped
   runtime checks passed, including the new switch/breaker, source energy,
   fitting clearance and wall/loft/join occlusion checks.
3. Rechecked production model separation, supported spawns, ritual reservations,
   physical stairs and swept navigation from the van: all 15 zones and required
   functional sockets remain reachable over 292,910 supported positions.
4. Inspected six matched standing runtime views, with circuit on/off and no
   flashlight, CRT/grain or studio fill. The blade and under-loft floor become
   readable when powered; the mill returns to darkness when switched off.
5. Reopened the saved support and full scene in Blender. Connected support/cable
   bounds, ceiling pivot, original UVs, packed textures, every exported triangle
   vertex/UV and all socket transforms pass. The scene has 253,341 exact runtime
   triangles. Actual packed metal row contrast is 1.16–1.27%, below the 2.5%
   banding guard; this checks this asset, not every historical metallic texture.

The software renderer reported a slow frame during capture. These images and
checks establish authored placement and headless behavior, not native OpenGL
performance or human-play acceptance. The engine's lighting implementation was
not changed. Join tests cover the specified Blackpine rays; they do not prove
the absence of every possible light leak throughout the game.

## Floor-depth correction after user review

The single-face loft floor was inadequate. DEV-001 in the shared defect ledger
records a reproduced failing geometry test against 4b2bb24, the 160mm slab fix,
separate downward-facing soffit, exposed fascia, shared collision and a fitting
relocated to the actual underside. DEV-007 records the resulting stair-landing
regression and its correction. Stair ascent/descent and the full swept flood
pass again. Matched corrected views replace the current review images; original
before-fix views remain under `previews/defects/`. The correction is scoped to
Blackpine; other maps require individual floor/roof/corner audits.

## Next unfinished gate

The user has authorized starting Camp Hollow Creek next while these acceptance
gates remain recorded. Blackpine stays locked. Walk the lit/unlit mill and loft in the native game, then
exercise real human/AI hunts, defended rites, evidence balance, LAN site
selection/rejoin, controls and target-hardware performance. The site remains
locked. Do not advance Camp Hollow Creek to a completed map on these results.
After those gates, derive its facilities from its existing blockout and model
requirements rather than rebuilding the rough map.

## Reproduce

From the repository root, with Python, JDK 17 and Blender 5.0 installed:

```powershell
& 'C:/Program Files/Blender Foundation/Blender 5.0/blender.exe' --background --factory-startup --python-exit-code 1 --python development/levels/build_mill_lighting.py
python development/levels/verify_mill_lighting.py
& 'C:/Program Files/Blender Foundation/Blender 5.0/blender.exe' --background --factory-startup --python-exit-code 1 --python development/levels/build_scene.py -- blackpine --output-suffix -worklights --no-render
& 'C:/Program Files/Blender Foundation/Blender 5.0/blender.exe' --background --factory-startup --python-exit-code 1 --python development/levels/validate_mill_lighting.py
```

The source exporter and new suffix preserve the older graybox, dressed and
mill-expanded checkpoints. Blender uses the exported runtime geometry directly.
