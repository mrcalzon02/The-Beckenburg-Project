# Layout planning checkpoints

21 October 2026. User authorized all proposed maps, including campaign outdoor and distant locations, to receive rough layouts with individual commits before refinement.

Each completed entry means JSON bounds, named spaces, route reservations, two graph cycles, three ritual areas, van placement, story/system notes, measured SVG and OBJ volume study have been authored and planning-checked. It does **not** mean a playable graybox, collision pass, measured escape timing or completed campaign.

Unrelated in-progress scene, library, lighting and preview edits were present at intake and are excluded from layout commits. Concurrent remote character work is reconciled separately without replacing these local edits.

## Completed checkpoints

- **HOUSE / The Briar House**: 15 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **FARMHOUSE / Hollowmere Farm**: 15 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **SCHOOL / Ravenshade School**: 15 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **MANSION / Blackthorn Manor**: 17 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **PRISON / Beckenburg Penitentiary**: 16 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **ASYLUM / St. Dymphna Hospital**: 16 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **ACADEMY / St. Alban Academy**: 17 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **COLLEGE / Beckenburg College**: 17 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **CHURCH / St. Brigid Parish**: 15 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **WLD-01 / Blackpine Logging Camp**: 14 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **WLD-02 / Camp Hollow Creek**: 13 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **WLD-03 / Red Mesa Ancestral Caves**: 14 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **WLD-04 / Widow's Peak Fire Lookout**: 13 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **WLD-05 / Dead Man's Claim**: 14 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **WLD-06 / Drowned Fisherman's Wharf**: 14 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **WLD-07 / Saint Bartholomew's Forgotten Cemetery**: 13 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **WLD-08 / Northwatch Research Station**: 14 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **WLD-09 / Blackwater Hydroelectric Dam**: 14 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **WLD-10 / Monastery of Ashen Bells**: 17 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **WLD-11 / Mourning Marsh**: 13 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **WLD-12 / Gallows Pass Railway**: 14 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **WLD-13 / Greywater Cannery**: 14 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.
- **CASTLE / Castle Beckenburg**: 23 space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.

## Full planning pass / visual QA

All 23 packets passed the complete roster check, bounds/overlap checks, connected route graph, two declared cycles per map, three distinct clear rite reservations and van-ramp transform checks. Rendered all 23 SVGs to PNG and inspected the complete contact sheet plus full House, Red Mesa and Marsh plans. Corrected ground support drawing order, moved original exterior labels out of building footprints, retained exact existing door centres/widths and stair endpoints, and routed adjacent-space connections through shared thresholds. Corrected the monastery gatehouse/cloister overlap. These remain planning checks, not runtime navigation acceptance.

The local browser preview could not reach the Windows localhost server, so no browser interaction acceptance is claimed. The portable HTML atlas and rendered PNGs are delivered directly. Package CRC and per-file SHA-256 checks cover the archive. No Java runtime or site unlock changed in the planning pass.

## Next unfinished work after the complete roster

User requested reuse-first construction, normal building sizes with large exposed places, sparse outdoor shrub hiding and real crouching. Follow [REUSE_BUILD_ORDER.md](REUSE_BUILD_ORDER.md). The [first Blackpine graybox](../BLACKPINE_BUILD.md) now exists, with actual compact buildings, doors, a loft and exposed yard; the swept van route reaches all fifteen volumes and functional sockets. [Physical crouching and sparse shrub refuge](../OUTDOOR_REFUGE_AND_CROUCH.md) are implemented and targeted checks pass, including actual Ctrl dispatch over real TCP. Blackpine has three refuge pockets and no automatic indoor cupboards.

The combined committed source tree was independently compiled and checked, including concurrent remote character work, without incorporating the unfinished local lighting/source edits. See `../verification/merged-map-build.json`. Keep the next work on Blackpine: machinery and furnishing, windows/roof details, readable signs/story, lighting review, defended ritual and human/AI hunt checks, native-window acceptance and performance. No additional remote map has begun construction and Blackpine is not unlocked.

## Blackpine dressing, terrain and room-story checkpoint

Completed twenty original camp/woodland meshes and nineteen original story/detail meshes, with native sources, procedural diffuse, recorded bounds/hashes and studio images. Blackpine now has baked worn routes and eroded shoulders, a 1.78 m grade toward the road, correctly supported upright low foliage, layered woodland, solid camp structures and validated under-map recovery. Three refuge shrubs remain; logs and shrubs sit outside the worn routes.

Twelve room histories and nine placed readable records connect the 2009 closure, weather damage and localized 2025 shelter use. Heating, water isolation and inspection power now have visible fittings and a consistent ledger. The payroll/manifest discrepancy remains unresolved. See [SITE_STORYTELLING.md](../SITE_STORYTELLING.md) for reusable assets and principles to backport after each earlier map's history is authored.

Scoped source, physical support, centre-ray reading, 384,000-pixel renderer parity, terrain/recovery and actual TCP/HUD checks pass. Full swept navigation reaches all zones and functional/story approaches over 296,207 positions. Two existing pursuit/lighting assertions reproduce from pre-change source and are documented. Exact native scene and art/runtime images are linked from [BLACKPINE_BUILD.md](../BLACKPINE_BUILD.md).

Next: Blackpine mill machinery, site signage, finer weather/lighting composition, hunts/ritual/evidence acceptance, native-window playthrough, LAN site integration and target-hardware performance. No second remote map has begun construction and Blackpine remains locked. CPU mesh sharing/culling is implemented; GPU instanced draw calls remain future work.

Local milestones are committed. Publication of this new dressing/terrain/foliage/renderer/story payload was rejected by automatic approval review because the previous upload approval named only crouching, shrub and graybox. Obtain explicit approval for this completed payload before retrying upload to shared main. Preserve concurrent remote and unrelated workspace work.


## 21 October / early-site and book checkpoint

Revised all eight last-run production models plus the bedroll for attached parts, native support and readable construction. Added 36 authored room histories across Briar House, Hollowmere Farm and Ravenshade School, nineteen supported ray-readable records and curated story-kit traces. Rebuilt ten clean/worn signs with lettering contained in their inset. Added the actual site-specific van clipboard sheet plus paged/scrollable solo/LAN dossiers. Three open book tiers now have blank parchment and all three drawn writing alternatives, selected only by an authoritative writing event and restored on reconnect. See LEGACY_SITE_STORIES.md and model-construction-audit.json for exact acceptance and limitations.

Current local compilation and focused XYZ, surfaces, interaction, lore, source-mesh, writing-state and artwork checks pass. Runtime captures and final merged-source/network verification must use this new checkpoint; older full-scene and Blackpine review reports predate these revisions. No approval has been granted to publish this expanded payload after automatic review rejected the prior upload. Next: reconcile concurrent remote Twins work, verify the merged source and network visuals, update full native-scene review, and obtain explicit upload authorization before shared-main publication.


## 23 October: terrain foundation breadth pass

At the user's request, all 16 remaining rough sites now have separate terrain
contracts, original ground GLBs/editable sources and native site/layer reviews.
See [terrain-foundations](../terrain-foundations/README.md) and its actual checks.
These add a terrain step to the existing layouts; original JSON/rough meshes,
source floor parts, known developed maps and canonical van design are preserved.
Per-site route diagnostics retain unresolved grades/crossings. Do not promote
this breadth checkpoint to completed shells, navigation acceptance or mission unlock.


## 23 October additional planning scope

- [Morrow Point Lightstation](wld-14/README.md)
- [MV Saint Elara](wld-15/README.md)
- [Eight Pines Motor Lodge](wld-16/README.md)
- [Battery Brackenhead](wld-17/README.md)
- [Site Cinder-6](wld-18/README.md)
- [Existing cemetery mortuary expansion](../layout-expansions/wld-07-mortuary/README.md)

These five new sites extend the catalog to 28 packets including HQ. The cemetery expansion is a separate proposal, not a 29th site. Original layouts and blockouts are preserved.
