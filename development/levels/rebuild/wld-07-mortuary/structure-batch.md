# WLD-07-MORTUARY: native structural route checkpoint

S03 remains IN_PROGRESS. Source/native/export/generator hashes are bound in
`../structures/wld-07-mortuary/implementation.json`. Commit binding is in the
stage register. Original scenes/layouts are retained.

Verified native scope: 6464 triangles; 162 closed
solids; 39 measured160mm floor/deck parts; 17 cap
rays; 34 treads; 0 fixed-boundary checks.

Candidate collision is exported from the same GLB triangles into HCS2. Per-map
headless probe results and exact rejected rows are in
`../verification/roster-routes-2026-11-07/`. Body/traversability failures remain
blockers; no map registry unlock or production movement change was made.

Basements requiring actual persisted editor terrain holes: ossuary, chamber.

Native overview/cutaway review is separate from runtime acceptance. Final roof
forms, guarded landings, complete capsule sweeps, terrain/support/enclosure
returns, canonical van, source-library furnishings, circuits, hiding budgets,
light separation and authored story/physics remain open. Preserve at most40
accessible search spaces. No round-duration tests or local editor UI automation.

Next: repair map-specific movement failures and continue the reusable-model
placement batch with individually supported, functional room arrangements.


## Saved editor terrain revision

Actual persisted punched topology now supplies the native terrain OBJ and HCS2;
the GLB terrain float triangles/flags match that saved native physical source.
Source height PNGs are unchanged by punching, disk reopen and non-hole physical
negatives pass. Current evidence: ../verification/roster-editor-terrain-2026-11-07/.
All1720roster tread centres now have support and clear spine rays; full body
sweeps still fail. Terrain remains unaccepted: finish actual boundaries, rims,
retaining structures, guards and canonical-van arrival. DEV-361/364/366 remain.
