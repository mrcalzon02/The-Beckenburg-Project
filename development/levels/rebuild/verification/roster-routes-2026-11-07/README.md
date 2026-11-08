# Native route revision checkpoint — 7 November 2026

This is map construction work. No local game/editor UI was opened for this pass.
Six remote sources now replace the eight unbuildable vertical/short reservations:
Red Mesa Caves, Widow’s Peak Lookout, Dead Man’s Claim, Ashen Bells Monastery,
Gallows Pass Railway and MV Saint Elara. Lookout has three separated flights and
explicit turn platforms. Monastery west basement stair is additionally rerouted
away from the north stair. Original source layouts and predecessor scenes remain.

The shared recipe reserves headroom through intermediate decks, preserves flight
end landings, uses explicitly authored constructed exterior decks, and applies
floor apertures to exported horizontal route supports. The last correction is
important: those supports had refilled the holes and obstructed45 head-height
centre rays. The old failed rows and exact source GLB hashes are retained in
`rejected-strip-obstructions/`; no acceptance is inferred from a successful export.

All506 actual exported tread centres have native support and clear vertical
rays from0.3m to1.8m above the tread. These are headless measurements through the
existing AuthoredSitePhysics kernel, with exact GLB, HCS2 and tested JAR hashes.
The HCS2 files are candidate exports of the actual same GLB triangles, not box
substitutes. `tread-probes.tsv` retains every measured pose and result.

**Capsule movement remains blocked.** Clear centre rays are not standing body,
edge clearance or continuous ascent/descent acceptance. The current body and
four-neighbour traversability queries still reject many tread poses (all68 in
Monastery fail traversability). Diagnose the exact component contacts and explicit
stair handling before integrating/unlocking any candidate. The game’s existing
movement kernel, map registry and multiplayer protocol have not been changed.

Basements still require actual persisted editor terrain holes. Neutral caps are
not final roof forms. Guards, complete landings, terrain supports/enclosure
returns, canonical van, room ownership, services, hiding budgets, furnishings,
lighting, loose physics and source-specific detail remain roster obligations.
No round-duration tests or editor UI automation are part of this checkpoint.

Reproduce from the repository root (Blender is background only):

```powershell
blender --background --factory-startup --python tools/build_roster_structures.py -- --remaining --replace
blender --background --factory-startup --python tools/export_roster_collision.py -- --sites WLD-03 WLD-04 WLD-05 WLD-10 WLD-12 WLD-15
python tools/audit_roster_routes.py --sites WLD-03 WLD-04 WLD-05 WLD-10 WLD-12 WLD-15 --output development/levels/rebuild/verification/roster-routes-2026-11-07 --require-clear-spines
python tools/review_roster_structures.py
python tools/review_roster_structures.py --check
```

The probe requires a locally built game JAR and JDK17. It hashes the actual JAR it
uses. This is a scoped native construction checkpoint, not runtime acceptance.
Defects DEV-359/360/361 track redesigned runs, the corrected obstruction and the
remaining movement failures respectively.

## Whole-roster probe

All25 candidate HCS2 files load through the real kernel. Of1720 tread centres,
1720 have support,1630 have clear spine rays,852 pass the literal body query
and819 pass the current traversability query. These are independent point
measurements, not continuous swept-route acceptance. Six further sites have90
overhead obstructions; exact components are in all25/tread-probes.tsv (DEV-363).
WLD-16 has no treads and gains no route acceptance from zero samples.
