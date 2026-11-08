# Existing model sources for the furnishing pass

Start at [the definitive asset pointer](../../../../ASSET_LIBRARY.md) and
[canonical searchable inventory](../../../assets/asset-library-index.json).
Those cover source masters, staged roughs, refinements, runtime models and
review/map assemblies. Do not use a selected measurement sheet as the inventory.

`staged-asset-bounds.json` measures all298 existing map-specific roughs and15
existing refinements in the Blender X/-Z/Y frame. It preserves source paths,
hashes, map assignments, mount hints, transformed bounds, triangle counts, UVs
and image counts. Rough models are authorized for spatial blocking, budget and
placement; refinement comes afterward. These measurements are not new runtime
approval or a reason to hold all rough placement pending finished textures.

`runtime-sample-assets.json` is only the earlier24-model furniture sample.
Its associated image is labelled as a sample. It is not a replacement library.
The staged-source inspection image shows24 selected floor props from the full
measured staged set; large structural models are retained in the measurements.

Additional packs referenced by the three named source chats remain listed in
the canonical index pending history/artifact retrieval. Never mark those assets
absent simply because they have not been imported into this checkout yet.

Regenerate source measurements in Blender background:

```powershell
blender --background --factory-startup --python-exit-code 1 --python tools/inspect_furnishing_library.py
```

The tool reads the canonical inventory and does not rewrite source GLBs, pivots,
UVs or materials. Use each map's existing site-kit assignments and room functions
when allocating actual floor/wall/ceiling placements. Preserve source identities
when subsequent model/texture refinement is committed.

## Initial native allocation batch

Actual existing source roughs are now placed in25native candidates:124instances
from60designs,16120prop triangles. Per-site .blend/.glb/HCS2 and placements.json
are in site folders. room-programs.json is the authored first allocation;
roster.json explicitly retains the other4mature maps. verification.json and
the native contact sheets are bound to current sources. Full room capacity,
remaining site-kit models, mounts and later refinement remain open. Exact
physical checks and rejected studies are in ../verification/roster-prop-allocation-2026-11-07/.
