# Roster landing repair checkpoint — 7 November 2026

Three native scene revisions,25map movement/contact updates and29roster decisions. Original masters and mature Castle/House/Blackpine/Hollow Creek sources retained. Existing124rough instances/60designs retain exact placement lists. Rough assets remain current spatial/model budgets; finish later. No local editor UI or round-duration tests.

CollegeL84 and MansionL59/L60 now end their incoming flights0.75m before the sideways turn, with real straight landings. The old slabs overhung the last risers. Source widths and room counts remain fixed. Lighthouse matched cyclic miter end boundaries connect its five revolutions; a tangent/radial landing reaches the lantern floor. Actual blend/GLB/HCS2 derivatives generated for allthree sites. All140curved treads remain12triangles each; no hidden ramp or native collision relaxation. Kernel source is byte-identical to the prior checkpoint.

Actual before/after results:

- 182/221vertical segments pass ascent/descent in each structural/furnished variant, up from170. Every earlier passing segment/direction is retained.39segments remain blocked, with exact failure coordinates and first physical component/foot-support diagnosis committed.
- 44/84complete source stair links pass each direction, now including every intermediate flat landing.40complete links remain blocked. The previous82contiguous vertical-only chains had a narrower scope.
- Seven maximal unique exact-endpoint cross-link chains are exercised without reinitializing feet at floors. CollegeL84/L80, MansionL59/L60/L61 and lighthouse route20–24 PASS both directions. The lighthouse chain contains142source segments from ground to18m lantern landing. Four other chains fail and remain listed.
- All1720tread centres retain support/head-height spines;1689traverse. Centre probes alone do not accept continuous routes.
- Current flat before/after furnishing results: {"samples": 109581, "baseline_valid": 73146, "new_block": 0, "baseline_sweep": 68291, "new_sweep_block": 0}.36435baseline-invalid samples remain real blockers, not accepted circulation.

The diagnostic invokes the unchanged kernel's private triangle-distance helper read-only. It reproduces each failing5cm production sweep and identifies actual triangle contact or missing foot support; it throws if its reconstruction diverges. The implementation changes no movement state or geometry. Names/contract/GLB/HCS2/layout/current source and JAR dependency hashes are recorded.

Native integrity:147132structural triangles,6540closed solids,1488depth parts,517cap rays,1720treads and192fixed boundary checks across25candidates. Furnished total163252triangles includes16120existing prop triangles. Three saved editor terrain triangle/flag identities remain exact. No saved topology bytes changed in this checkpoint.

All12before/after isolated stair images and12updated whole/room exports were actually viewed. Three labelled comparison PNGs accompany originals. Construction3/5, finish1/5: landings fit, but guards, architectural surfaces, model capacities, roof/terrain edges/foundations/van arrival and runtime/story integration remain open. Rejected prior geometry stays in Gitfe7f4972 and the labelled captures; no new prop master invented.

Reproduce headlessly:

-  python tools/audit_roster_stairs.py --output <directory>
-  python tools/audit_roster_routes.py --remaining --furnished --authored-stairs --require-clear-spines --output <directory>
-  python tools/audit_furnishing_routes.py --authored-stairs --output <directory>
-  python tools/review_roster_structures.py --check
-  python tools/review_roster_furnishings.py --check
-  python tools/index_staged_map_models.py --check
-  python tools/build_development_gallery.py --check

Publication requires exact commit/remote readback. Gallery7925PNGs generated/checked; actual new deployment remains pending accepted stack integration. Main remainscc65453c; observed full42431982917, renderer42431982951 and installer42431982920 atfad6c7a9 SUCCESS remain separate from these candidate checks. Previous raw365/0/0/365 is unchanged main evidence, not exact-source candidate acceptance.

Next: fix the recorded wall-crossing elevations, crossing stair runs, unsupported end landings and actual terrain contacts across the remaining roster; use saved editor punching for new basement terrain changes. Complete room/owner/navigation/services and independent power/hiding/story/loose physics, continuing source-library major/mounted allocations and later refinement. No runtime unlock or full-goal completion.
