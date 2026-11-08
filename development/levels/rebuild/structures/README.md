# Roster structural pass

This is the first breadth pass through the remaining **25 packets**. Castle,
House, Blackpine and Hollow Creek retain their mature native sources and require
individual retrofit audits. All **29 decisions** are explicit in `roster.json`.
The complete original and follow-on map scope remains active.

These are editable Blender construction candidates and reimported GLB exports,
not unlocked or finished missions. Revised first-principles roughs govern the
eight earlier building locations. The remaining measured source layouts govern
the remote candidates and cemetery/mortuary annex. Each packet's
`implementation.json` binds input, generator and preserved-original hashes.

The pass adds closed ceiling/deck caps, explicit160mm floor depth and undersides,
whole-flight openings and supported stacked stair treads where the authored run
permits them. An existing upper floor supplies its own underside instead of a
duplicate ceiling. Circular top caps close the weather enclosure without copying
an intermediate maintenance core through the roof. Open-world zone pads are
omitted; pre-existing authored terrain proposals are retained as proposals.
Native labels stay editable but are excluded from exported structural geometry.
Diagram van placeholders and ritual markers are likewise excluded from the GLB.

Authored sealed context links no longer cut openings or create route supports.
Prison's shared exterior service-sector mouths receive full-width fixed infill;
the exported boundary is ray-checked. Terrain support, enclosure returns and
around/under/over bypasses remain actual runtime audit obligations. A threshold
ray alone is not proof of a completely bounded forty-space playable envelope.

Final native verification covers **131,219 triangles**, **6,932 closed solids**,
**2,077 measured floor/deck parts**, **739 cap rays**, **1,538 stair treads** and
**192 fixed-link boundary checks** across the25exports. Every GLB was independently
reimported and every native source reopened. Original source scenes and inputs
were independently hash-checked unchanged. `verification.json` supplies per-site
counts, basements and unresolved routes.

Eight distinct links still require authored longer flights/switchbacks or a
different elevation solution: WLD03/L13; WLD04/L13,L14; WLD05/L15;
WLD10/L16,L19; WLD12/L16; WLD15/route14. They remain rough support masses, not
accepted stairs. The source has16 diagnostic records for these8links. Treads
elsewhere still need actual capsule, landing, guard and headroom verification.

## Bounded visual review

All seven contact sheets, containing both exported views for all25packets, were
viewed. Farmhouse's pilot closed/cutaway exports were also inspected individually.
The sheets distinguish the cloister courtyards, ward/cell wings, teaching spaces,
church volume, domestic/outbuilding groups, remote structures, round lighthouse,
ship hull and mortuary grouping. They verify massing and visible cap/cutaway
construction at this stage. They do not establish final architectural character,
surface quality, circulation, light separation or support against finished earth.

Visible remaining work includes neutral unfinished caps instead of final exterior
roof forms; omitted or proposal-only terrain; unsupported diagram route strips;
Prison infill awaiting full enclosure/earth returns; and the8unresolved links.
No source library assets are discarded or replaced by these neutral blocks.

## Next breadth passes

1. Bridge each authored coordinate source into native/runtime geometry, room
   ownership and collision; resolve the8links and sweep all required routes.
2. Author actual terrain and persisted editor holes for the listed basements;
   fit foundations, drainage, world boundaries and canonical van placement.
3. Audit the unified reusable model library against each site's functional
   program and place actual supported fixtures/models with preserved UVs/pivots.
4. Iterate roof forms, services, lighting, lore, loose physics and encounter
   detail across the roster. Runtime/solo/LAN/GPU/hardware gates remain separate.

Generate with Blender5, preserving existing scenes:

```powershell
blender --background --factory-startup --python tools/build_roster_structures.py -- --remaining
```

Use `--sites FARMHOUSE CHURCH` for a selected pass and `--replace` only for these
derived outputs. Failed packets are recorded as REVISE while independent packets
continue. The process fails overall if any structural check fails.

The review tool needs Pillow (this run used13.0.0, installed only under build):

```powershell
python -m pip install --target build/structure-review-python Pillow
python tools/review_roster_structures.py
python tools/review_roster_structures.py --check
python tools/build_development_gallery.py
python tools/build_development_gallery.py --check
```

Failed open-context candidates and logs are retained. Model gallery publication
is branch publication until an accepted merge and actual deployed-page readback.
No round-duration tests or physical FPS claims are added.
