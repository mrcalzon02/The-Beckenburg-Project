# Map-specific primitive prop blockouts (v1)

The authoritative planning backlog is [catalog.json](catalog.json), which maps **41 starter props to 28 proposed maps plus the separate Vale Mortuary expansion**. It augments the existing `development/levels/model-requirements/*.json` files rather than replacing their full room-by-room checklists.

## What this package is

- Measured, ground-origin, Y-up glTF 2.0 geometry in metres: basic boxes, faceted cylinders, and an enclosed tent prism. The colors are placeholder materials, not final textured UV assets.
- Each `level_blockout_*.glb` is an individually selectable study asset. No duplicate of any existing finished `level_*` model is overwritten.
- The catalogue's `sites[].prototypes` field is an **assignment proposal**, not an automatic scene placement. No map availability, collision, navmesh, lighting, acoustics, interactivity or network state has been modified.
- These 41 starter studies cover structural openings/rails, storage/workshop fixtures, institutions, archives, camps/mining, maritime/industrial scenes, new lighthouse/lodge/bunker/shaft locations and the mortuary addition. Phase 2 adds Castle inventory counters, map board, relic containment, training rig and restoration scaffold, plus institutional medical/security and remote mechanical landmarks.
- Existing source LOD/original model assets remain unchanged. Completed prop models require geometry refinement, proper UV/materials, origin/orientation review, scene placement, and actual editor/renderer acceptance.

## Rebuild / inspect

```bash
python development/levels/primitive-props-v1/build.py
python development/levels/primitive-props-v1/build.py --check
```

No external Python dependencies are required. `build.py` writes independent binary GLBs to `models/` and `verification.json`; `--check` compares all files to the catalog-derived deterministic bytes. Confirm individual GLBs through the game's existing import/review path before approving their use. All source-coordinate changes should be made in `catalog.json`, not by post-export ad hoc edits.

## Priority map handoff

1. Replace closed-volume stand-ins with engineered openings and valid actual door/frame/collider bindings; inspect upper-floor and courtyard wall intersection repairs in Castle separately.
2. Test stairs and rails against flight rise, headroom, turns, landings and navmesh before populating upper floors.
3. Build authentic mechanical signatures by map: Blackpine logway, Widow's Peak tower, Dead Man's Claim mine, wharf pile bay, Blackwater turbine, Gallows Pass rail signal, Greywater processing line.
4. Make new locations identifiable: lightstation optic, MV Saint Elara moorings, Eight Pines lodge sign, Battery Brackenhead blast door, Site Cinder-6 shaft.
5. Promote only approved refinements to the actual runtime model index and authored editor scene; verify pivots, bounding extents, UVs, static-light prewarm cost and screenshot rendering.

**Acceptance:** These are real geometry files, but they are not completed production models, and their visual quality is intentionally placeholder. `verification.json` is format/integrity evidence only, not playable map acceptance.
