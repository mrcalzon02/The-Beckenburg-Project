# Original household function assets

The180triangle refrigerator uses an original generated enamel atlas, explicit
front/freezer/side/top/hardware UV rectangles and an aspect-matched rear condenser
panel. Fifteen closed box components intersect through actual attached supports:
body, plinth, feet, two doors, handle mounts/grips and rear panel. The budget is200
triangles. Generated panel boundaries were measured from the actual image rather
than assumed from the prompt. Source Blender, staged GLB, reimported front/rear
renders, hashes and reasoned visual review are retained. Runtime source sampling
checks the actual atlas origin and diffuse factor at256points.

The188triangle supported coat is an additive sibling of the original152triangle
`level_trace_coat`. Its mesh coordinates, UVs, materials and native pivot remain;
36hanger triangles connect its shoulder to the actual existing hook. The original
source file is untouched, and every original source triangle is checked as a
retained subset. Reimported front/side and actual in-room views were inspected.

`provenance.json` records the generated image identity and original source hashes.
`visual-review.json` binds actual reviews to asset and render hashes. These are
scoped construction assets; full House saved editing, client/hardware and location
acceptance remain separate. `rejected-report-lifetime.log` preserves the original
Blender report failure; generation now uses strict Python failure propagation.
