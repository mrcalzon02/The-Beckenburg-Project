# Actual human import/authoring UI captures

The initial `import-review.png` and `import-review-before-newline-fix.png` are actual Swing component captures from the read-only topology checkpoint96aea898. The latter preserves the observed DEV-189 formatting defect.

`indexed-vertex-authoring.png` is a later actual component capture after human vertex0 moved to X=-0.25, with stable-ID selection highlight and native mesh save/reopen controls. The test uses real buttons and fresh native source load, compares every published runtime corner, and verifies exact original/editable source archives and changed-source refusals.

All images show a four-face test OBJ fixture in the human tool. They are UI/fixture evidence, not authored map/model art, installed gameplay, renderer-quality or complete G2 acceptance. Selection/gizmos, primitives, edge/face modeling and full material/normal interchange remain open.

`primitive-authoring.png` captures the actual human dock after five separately owned primitive fixture parts were committed, with a pending torus preview. Native topology reports five islands, 16 plane boundary edges and zero issues. This is fixture UI evidence, not production model art or complete G2 acceptance. Primitive creation is now implemented; picking/gizmos, edge/face modeling and full interchange remain open.

`triangle-selection.png` is a later actual fixture UI capture: real mouse clicks select a native face in FRONT view, then ISOMETRIC view displays its highlight. Individual triangle/vertex/edge/face picking is now implemented; multi-selection, gizmos and modeling remain open. Primitive721103f2 native41368621679 and standalone41368616675 completed SUCCESS.

`global-move-gizmos.png` captures the real selected fixture face and native global X/Y/Z handles after actual mouse drags, undo/redo and a canceled final drag. This remains UI fixture evidence, not production model art. Global translation is implemented; advanced coordinate systems/pivots, rotation/scale, multi-selection and modeling remain open.

`area-selection.png` is actual fixture UI after visible/X-ray box/lasso gestures and a shared native group move. The two triangles overlap in FRONT projection, so six distinct selected IDs occupy three screen positions. Real decoder readback verifies both layers. This remains UI/fixture evidence, not model art or full G2 acceptance.
