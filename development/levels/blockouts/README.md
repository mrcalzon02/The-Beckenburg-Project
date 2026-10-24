# Remaining proposed levels — rough shell blocking

23 October 2026. The **17 remaining measured proposals** now have separate editable Blender
shell scenes and portable GLBs: five unbuilt original investigation families and twelve remote proposals.

House, Farmhouse, School, Church, Blackpine and Castle are excluded because
authored versions already exist. No replacement blockouts for those maps are
retained in this delivery.

[Visual review](REVIEW.html) · [All-map ZIP](all-remaining-level-blockouts.zip) ·
[Integrity report](verification.json) · [Detailed manifest](manifest.json)

This moves one stage beyond the old solid volume studies. Native scenes contain
separately named floor slabs and full-height room walls, rough boundary openings
derived from route intersections, headers, supported connector strips, sloping
elevation reservations, rough upper-deck openings, zone labels, clear ritual pads
and the van's metre-scale envelope. Open tops make the interior massing visible.
Collections keep individual zones and circulation separately editable. Coordinates
remain those of the planning JSON; Blender uses Z up and portable glTF uses Y up.

The colored supports express the existing proposed routes. They are not a claim
that a steep ramp is an acceptable staircase or that a graph route fits through
every intermediate wall. Vertical-only links remain explicit core reservations.
Every map's `implementation.json` and `NEXT.md` flag its elevation work and
preserve the source-specific refinement concerns. Resolve flights, landings,
guards, headroom, overlapping shared partitions, connecting corridors, actual
terrain and route crossings before authoring physical navigation.

Each packet includes the exact source layout JSON and measured source plan.
The earlier layouts are unchanged. Existing map scenes and runtime definitions
are retained. Church is conservatively excluded because its authored LevelPlans
geometry already exists, despite its production lock.

## Verification and remaining gates

The generator reopens every saved Blender scene, checks its source hash and mesh
count, and renders its actual native shell. Independent packaging checks exact
17-map pending-roster coverage, unchanged source JSON hashes, native-file hashes, finite
GLB position bounds, triangle accessors, preview presence and ZIP CRC integrity.
These are artifact-integrity checks, not collision, headroom, hunt, multiplayer,
stair safety or performance acceptance. No maps are unlocked by this pass.

Next development run: choose a packet; resolve its recorded connector and shell
issues; integrate authoritative room/collision/portal coordinates and the existing
van module; then add utilities, reusable models, dressing and gameplay checks.

Engine optimization remains on hold locally. This pass changes no Java sources,
runtime map availability, network IDs, existing scene files or engine settings.
Previously unfinished local lighting/occlusion work remains uncommitted and
excluded from this delivery.

## Rebuild

From the repository root:

```powershell
& 'C:/Program Files/Blender Foundation/Blender 5.0/blender.exe' --background --factory-startup --python-exit-code 1 --python development/levels/blockouts/build_blockouts.py
python development/levels/blockouts/package_blockouts.py
```

Generation uses the original `development/levels/layouts/*/layout.json` files;
only the separate `blockouts/` delivery is written. No engine compilation is
required. Rough Blender files retain their actual elevations and omit review
camera/light objects; native PNGs are neutral studio inspections.

## Needed models

Each pending map packet includes MODELS.md and models.json: standard furnishings and fixtures, verified local reuse candidates, unique models, clutter and finish detail by room. The ZIP also includes model-requirements/ for all 23 maps; existing maps receive documentation only.


## User-requested additional scope — 23 October

Five new lore-named sites and an additive cemetery/mortuary study are now planned. See [new location native reviews](NEW_LOCATIONS_REVIEW.html). These are rough reservations and native blockouts; model lists do not count as model completion. Detailed terrain support, stairs/guards, architecture, fixtures/dressing, runtime and human review remain pending. The original 23 maps and 17 rough source blockouts are preserved.
