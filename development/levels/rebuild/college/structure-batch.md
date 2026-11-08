# COLLEGE: native structural route checkpoint

S03 remains IN_PROGRESS. Source/native/export/generator hashes are bound in
`../structures/college/implementation.json`. Commit binding is in the
stage register. Original scenes/layouts are retained.

Verified native scope: 8288 triangles; 611 closed
solids; 151 measured160mm floor/deck parts; 38 cap
rays; 94 treads; 44 fixed-boundary checks.

Candidate collision is exported from the same GLB triangles into HCS2. Per-map
headless probe results and exact rejected rows are in
`../verification/roster-routes-2026-11-07/`. Body/traversability failures remain
blockers; no map registry unlock or production movement change was made.

Basements requiring actual persisted editor terrain holes: west-tunnel, plant, maintenance, west-cellar-stair, cross-tunnel, east-tunnel, east-cellar-stair.

Native overview/cutaway review is separate from runtime acceptance. Final roof
forms, guarded landings, complete capsule sweeps, terrain/support/enclosure
returns, canonical van, source-library furnishings, circuits, hiding budgets,
light separation and authored story/physics remain open. Preserve at most40
accessible search spaces. No round-duration tests or local editor UI automation.

Next: repair map-specific movement failures and continue the reusable-model
placement batch with individually supported, functional room arrangements.
