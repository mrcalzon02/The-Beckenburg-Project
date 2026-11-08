# Beckenburg asset library â€” start here

**Consult this pointer before making, selecting, refining or placing a model.**
The existing roughs are intended for allocating space, model budgets and actual
placements now. Refine their geometry and textures in later passes. Keep their
identities and source history; do not recreate something because a small sample
or the runtime directory omitted it.

The canonical searchable file is
[asset-library-index.json](development/assets/asset-library-index.json).
It records existing file paths, SHA-256 hashes, sizes, categories, map assignments,
mount hints and source stages. The 24-model furniture inspection is a sample.

<!-- asset-library-counts:start -->

| Collection | Local files |
|---|---:|
| Map-specific rough props | 298 |
| Existing prop refinements | 15 |
| Runtime model library | 580 |
| Hollow Creek native map components | 384 |
| Source-library masters | 120 |
| Additional custom model sources | 22 |
| Review/contact assemblies | 173 |
| Reference/rejected studies | 94 |
| Native map exports | 231 |
| **Total local GLB files** | **1,917** |

1,760 distinct file hashes. Copies and review assemblies are indexed
without counting them as unique usable props. Source stage and intended use
remain attached to every entry.

<!-- asset-library-counts:end -->

Start with [map assignments](development/levels/primitive-props-v1/catalog.json),
[individual roughs](development/levels/primitive-props-v1/models),
[29 site kits](development/levels/primitive-props-v1/site-kits/index.json),
[refinements](development/levels/primitive-props-v1/refinement/index.json),
[source-library masters](assets/libraries) and [runtime models](assets/runtime/models).
The canonical index also scans every other tracked/unignored GLB, so separate
custom exports, references, map scenes and contact kits remain discoverable.

The [archive inventory](development/assets/local-library-manifest.json) also
records 8,656 entries, including 1,632 GLBs, 1,676 FBXs, 851 OBJs and 181 Blender
files. Formats may represent the same model. Archive entries are references;
the index does not pretend they have all been extracted or inspected.

## Find an existing model

Run from the repository root:

```powershell
python tools/index_staged_map_models.py --query "turbine"
python tools/index_staged_map_models.py --site ASYLUM
python tools/index_staged_map_models.py --query "mortuary trolley"
```

Start with the site's existing prototypes and
[room requirements](development/levels/model-requirements/catalog.json).
For example, ward beds, hydro turbines, ore carts, science tables, mortuary
fixtures and lighthouse equipment already have source studies. Inspect the
actual file and allocate its footprint; retain a source-specific refinement task.
Mount, support, clearance and collision checks still govern placement. Runtime
mission acceptance remains a separate gate from using a rough to block space.

## Additional source conversations

The owner identified these as further rough-model sources:

- [Continue Map Modeling](https://chatgpt.com/c/6aed02d7-4bd4-83e8-9fa5-6ab9071863a4)
- [Continue Map Prop Modeling](https://chatgpt.com/c/6aecd6f0-6b90-83e8-aee9-269bd7bff42b)
- [Map Prop Modeling](https://chatgpt.com/c/6aec37d7-9848-83e8-aaaa-a68233bd9345)

Recent turns from **Continue Map Modeling** and **Map Prop Modeling** have now
been read and matched to existing catalogs, refinements, site kits and relic/crypt
inventories. **Continue Map Prop Modeling** currently times out. Exact checked
paths, historical commits and remaining retrieval work are in
[model-source-conversations.json](development/assets/model-source-conversations.json).
Older reported counts are historical; the generated local index supplies current
file counts. Further history/artifact review remains open. Existing source roughs
are already usable and being placed; retrieval delays do not make them absent.

## Keep this pointer current

```powershell
python tools/index_staged_map_models.py
python tools/index_staged_map_models.py --check
```

Regenerate after adding, refining, moving or importing source models. The check
compares the complete indexed local set and its file hashes; a missing model or
stale entry fails. Preserve earlier sources when adding a refinement. Record
new conversation packs with their provenance and exact local paths rather than
leaving their existence only in chat history.
