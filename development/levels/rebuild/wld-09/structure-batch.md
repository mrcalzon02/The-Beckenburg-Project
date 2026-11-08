# WLD-09: native structural route checkpoint

S03 remains IN_PROGRESS. Source/native/export/generator hashes are bound in
`../structures/wld-09/implementation.json`. Commit binding is in the
stage register. Original scenes/layouts are retained.

Verified native scope: 4288 triangles; 333 closed
solids; 62 measured160mm floor/deck parts; 34 cap
rays; 215 treads; 0 fixed-boundary checks.

Candidate collision is exported from the same GLB triangles into HCS2. Per-map
headless probe results and exact rejected rows are in
`../verification/roster-routes-2026-11-07/`. Body/traversability failures remain
blockers; no map registry unlock or production movement change was made.

Basements requiring actual persisted editor terrain holes: turbine, workshop, gallery, valve, return, plant.

Native overview/cutaway review is separate from runtime acceptance. Final roof
forms, guarded landings, complete capsule sweeps, terrain/support/enclosure
returns, canonical van, source-library furnishings, circuits, hiding budgets,
light separation and authored story/physics remain open. Preserve at most40
accessible search spaces. No round-duration tests or local editor UI automation.

Next: repair map-specific movement failures and continue the reusable-model
placement batch with individually supported, functional room arrangements.
