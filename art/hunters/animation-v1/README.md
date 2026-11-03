# Playable hunter animation v1

Cleric, Observer and Demonologist are the three currently playable runtime appearances. Concept-only alternate gender/model boards are not selectable appearances and are outside this export.

Each original generated PNG is a transparent 4-column, 3-row sheet. Columns: front, right-facing side, back, rear three-quarter. Rows: walking stride, walking passing pose, low crouch. These are newly generated animation derivatives of the checked-in character-profile references; they do not replace the original masters or standing turnarounds.

Generated with the built-in image_gen tool. Run `python tools/build_hunter_animation.py` to rebuild 36 individual transparent 128x224 PNGs, the deterministic runtime ZIP, SHA-256 manifest and contact sheet. The individual exports are under `assets/runtime/hunter-animation/`. Standing proportions are used for walk frames; crouch vertical sampling is normalized because the renderer already applies the authoritative crouch height. Horizontal scale stays constant across poses.

Runtime: received teammate movement alternates the two walk poses every 0.32 metres; no position updates for 0.20 seconds returns to original idle. Teleports, stale seats and clock resets clear the gait. Host crouch takes priority and selects the authored bent-knee sprite. Physical dimensions, collision and network protocol are unchanged. The existing four-view mapping is retained; no additional viewing angles are claimed.

Generation prompt set:

- Reference: the corresponding `assets/concept-art/v001/01-hunters/<role>-character-profile.png`.
- Initial specification for each role: identity-preserving production sprite sheet, exactly 4 columns by 3 equal rows, actual transparency, no text/borders/shadows; front, right-facing profile, back and rear three-quarter; two distinct walking poses and a low bent-knee crouch; preserve face, hair, costume, asymmetric equipment, muted palette and early PlayStation textured low-poly style; equal scale, centered actors, common boot baseline and transparent gutters. Cleric preserves collar/cross/black coat; Observer preserves auburn ponytail/olive jacket/radio/camera; Demonologist preserves beard/brown coat/pentacle/occult book.
- Correction: replace row two's too-similar stride, preserving the other rows and asymmetric gear.
- Final edit prompt (role name substituted): “Edit this <role> 4x3 sprite sheet: ONLY change all four ROW TWO characters to MID-STEP PASSING POSE. Feet CLOSE TOGETHER directly underneath hips, one supporting leg STRAIGHT vertically underneath body, other knee bent with foot lifted just behind supporting ankle. Side/rear-three-quarter silhouettes narrow at legs, NO long stride, NO wide spread legs. Arms near neutral hanging close to hips. Front/back feet close under pelvis with one foot slightly raised, no crossing legs. Row one wide stride and row two narrow passing pose form a clear two-state walk cycle. Preserve face, hair, costume, equipment, camera directions and sprite sizes. Preserve rows ONE and THREE exactly, actual transparency, same 4 column 3 row equal grid, no text.”

The contact sheet is an asset review, not a physical multiplayer acceptance capture. Focused Java checks cover atlas availability, distinct frames, received movement, stop, crouch, teleport, clock reset and actual rasterizer wall occlusion. Installed Windows multiplayer visual review remains separate.
