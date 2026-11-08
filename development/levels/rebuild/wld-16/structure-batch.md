# WLD-16: roster structural checkpoint

Source implementation: `c34e4f9f133e220f98348850e961ff268ab84fb7`. S03 remains IN_PROGRESS.

The actual editable Blender source, GLB and independently reimported views are in
`../structures/wld-16/`. Its implementation record binds the authored
input, unchanged original scenes and generator hashes.

Verified native scope: 3636 triangles; 93 checked
closed solids; 24 measured160mm floor/deck parts;
12 exported cap rays; 0 stair treads;
0 authored fixed-link boundary checks.

Elevation work still needing redesign: No too-short flight flagged by this native pass; actual capsule/headroom/landing/guard tests still required.

Basement zones requiring actual saved editor terrain-hole integration:
None identified by negative indoor zone elevation in this source; other cavities still need individual audit.

Both exported views were inspected in the roster contact sheets. These are
neutral structural candidates, not finished roof forms or unlocked missions.
Runtime coordinate parity, continuous capsule routes, headroom/guards, terrain
support, light separation, complete enclosure returns, canonical van, authored
power, hiding budgets, supported source-library models and network/hardware
acceptance remain open. Existing mature maps are not certified by this pass.

Next: continue across the roster with runtime/route and supported model placement
passes. Preserve the at-most40accessible-space policy. No round-duration tests.
