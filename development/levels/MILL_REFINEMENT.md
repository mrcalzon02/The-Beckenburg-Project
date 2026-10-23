# Blackpine circular-headrig refinement — 22 October 2026

The mill now uses a **two metre circular head saw at the end of its logway**.
The user's historical sawmill photograph corrected the earlier workshop-bandsaw
interpretation. The original small bandsaw is reduced to workshop scale and moved
to the equipment repair shed. This is original measured development scenery,
not a literal reconstruction of the photographed mill or a powered simulation.

[Visual review](BLACKPINE_MILL_REVIEW.html) ·
[Original logway assembly](blender/blackpine-logway-assembly.blend) ·
[Expanded complete scene](blender/blackpine-mill-expanded.blend) ·
[Twelve-model CC0 kit](../../assets/libraries/industrial/blackpine-mill-refinement.zip)

## Cutting station and logway

- Circular headrig at X43.2/Z34.0, carriage at X43.2/Z30.6, both yaw zero.
  Their rail ends meet at Z32.5. The rear carriage buffer and outfeed buffer
  terminate the complete 6.8 m rail run; no stop obstructs the connecting ends.
- Blade and vertical sawn face both use local X−0.95, world X42.25. The sixty-four
  teeth are actual thickness-bearing mesh geometry, rather than a texture.
- Raised carriage knees carry the offset stock. The log underside is 1.17 m
  above its supported floor; the arbor's upper edge is 1.105 m. Its 65 mm
  clearance is independently checked. The blade lies outside the cross-bearers,
  so a longitudinal feed would not saw through its own carriage.
- Independent bearing pedestals, retaining flanges, stopped pulley, guarded
  drive, isolated motor and slotted sawdust trough explain the machine.
- The smaller bandsaw now stands X45.2/Z52.0 in the repair shed, approximately
  2.3 m tall. It is no longer presented as the sawmill's primary cutting machine.

## Added working-place models

Four new original models: circular headrig, isolated dust extractor, dry-stock
rack and first-aid station. All have metre-based GLBs, preserved native floor
pivots, packed diffuse maps, separate editable Blender sources, studio previews
and source hashes. Static first-aid supplies belong to the 2009 closure layer;
they do not heal players or add inventory. The closed, isolated dust extractor
does not imply active suction or new particle simulation.

The dry-stock rack was moved to X60.4/Z20.75 after the overlap check caught its
first placement intersecting the existing tool-store ledger table. Its label
has an actual bearing rail attached to the posts. The first-aid blanket rests
on its shelf; the dust sack rests on a platform beneath a real throat opening.

## Metal correction

The old shared procedural metal shader added fixed-amplitude sine rows and
bright border strips to dark material colors. Long UV-mapped surfaces turned
those rows into repeating black/gray bands. `rebuild_support.material()` now
uses subtle proportional, non-directional patina for metal. Twelve original
working-place models were rebuilt and re-rendered with that correction.
Independent packed-image checks bound the relative variation of row means
below 2.5%, catching the former banding pattern without flattening the texture.

This refresh covers the twelve-model working-place kit. Other previously baked
asset families need their own regeneration, hash updates and in-room inspection
before claiming the material correction has reached every model in the game.
Purchased source-pack textures and UV layouts retain their source identity.

## Recorded verification

[Runtime report](verification/blackpine-mill-expansion.json) records the exact
base commit, scoped Java overlays and SHA-256 of all packaged working-place
models. An independent Java 17 source compile excludes the pre-existing
uncommitted castle and lighting work. Production source bounds, floor support,
solid collision, furniture separation, door approaches, spawns and ritual
reservations pass. Existing story records retain reachable center-ray reading.
Swept capsule navigation from the van reaches all fifteen zones and required
functional sockets. Actual runtime captures use standing, traversable camera
positions; the bright assembly and studio images are labeled inspection views.

[Native report](verification/blackpine-mill-native.json) reopens all twelve
source files, checks the actual packed metal pixels, construction supports,
blade/log/arbor relationship, model hashes and the complete exported scene.
Every exported architecture triangle vertex and UV, authored socket transform,
packed image and scene hash is checked. The CC0 archive is reopened and CRC
verified; its hash and counts appear in the [package report](verification/blackpine-mill-package.json).

## Next unfinished step

Stay on Blackpine. Inspect composition in normal play and tune the mill's
lighting with the separately unfinished lighting work. Then exercise actual
human/AI hunts, defended ritual outcomes, evidence balance, LAN site selection
and rejoin, native-window controls and target-hardware performance. The map
remains locked pending those gates. No second map is claimed completed by
this pass. The canonical van dock, protocol room IDs and existing story/evidence
contracts remain in place.

The local branch and remote main already held divergent development before
this task. Refinement is published on `map/blackpine-mill-refinement`, preserving
both histories and all unrelated uncommitted work. Integration into current
main and full release acceptance remain separate work.

## Reproduce this scoped delivery

From the repository root, use Python and Blender 5.0 with the installed JDK 17:

```powershell
python development/levels/verify_mill_expansion.py
& 'C:/Program Files/Blender Foundation/Blender 5.0/blender.exe' --background --factory-startup --python-exit-code 1 --python development/levels/_mill_scene.py -- blackpine
& 'C:/Program Files/Blender Foundation/Blender 5.0/blender.exe' --background --factory-startup --python-exit-code 1 --python development/levels/validate_mill_expansion.py
python development/levels/package_mill_expansion.py
```

The `_mill_scene.py` exporter is generated from the committed scene builder by
the first command, with a separate output filename to preserve the older
Blackpine source scene. Source generators are `build_production_asset.py` and
`build_mill_expansion.py`; `render_headrig_review.py` builds the aligned native
assembly from those original files.
