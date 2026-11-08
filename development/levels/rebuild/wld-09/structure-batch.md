# WLD-09: roster structural checkpoint

Source implementation: `c34e4f9f133e220f98348850e961ff268ab84fb7`. S03 remains IN_PROGRESS.

The actual editable Blender source, GLB and independently reimported views are in
`../structures/wld-09/`. Its implementation record binds the authored
input, unchanged original scenes and generator hashes.

Verified native scope: 4056 triangles; 315 checked
closed solids; 44 measured160mm floor/deck parts;
9 exported cap rays; 215 stair treads;
0 authored fixed-link boundary checks.

Elevation work still needing redesign: No too-short flight flagged by this native pass; actual capsule/headroom/landing/guard tests still required.

Basement zones requiring actual saved editor terrain-hole integration:
turbine, workshop, gallery, valve, return, plant

Both exported views were inspected in the roster contact sheets. These are
neutral structural candidates, not finished roof forms or unlocked missions.
Runtime coordinate parity, continuous capsule routes, headroom/guards, terrain
support, light separation, complete enclosure returns, canonical van, authored
power, hiding budgets, supported source-library models and network/hardware
acceptance remain open. Existing mature maps are not certified by this pass.

Next: continue across the roster with runtime/route and supported model placement
passes. Preserve the at-most40accessible-space policy. No round-duration tests.
