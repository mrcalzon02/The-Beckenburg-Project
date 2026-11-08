# Refinement contact gallery — source-only assets

This folder contains **15 independently generated GLB geometry studies** and
**three true-scale contact-room pages**. The canonical geometry is in
`../refine.py`; the model index is `index.json`.

Each `level_blockout_refined_*.glb` is a separate low-polygon model with
ground contact at local Y=0. `level_blockout_kit_refined_p01.glb` through
`p03.glb` are **review stages**, not complete levels. Models are mapped to
their proposed location names in the index, but no placement occurs in-game.

Rebuild and check from the repository root:

```bash
python development/levels/primitive-props-v1/refine.py
python development/levels/primitive-props-v1/refine.py --check
```

The native DEV room scans this directory as **SOURCE_ONLY**. A model must not
enter `assets/runtime/models`, the approved game-asset allowlist, or editable
map scenes without independent four-angle renderer evidence, author-approved
scale and orientation, texture review, collision/navmesh checks, and site-fit
validation. The Castle's eight proposed sarcophagus placements remain unchanged,
and this folder does not install any of them.

The prison visitation grille was corrected to **open bars and side frames**
rather than a large opaque wall-height backplate; the source generator now
explicitly rejects a large backplate regression. Wheels, feet, bases, and
open structural panels are built in separate physical pieces.

Mechanical and electrical models are inert visual references: they have no
live power, opening, kinematic, lighting, sound, ghost, or multiplayer behavior.
