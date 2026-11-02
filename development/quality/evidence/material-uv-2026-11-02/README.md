# Actual native UV editing and canonical material dependency controls

Fixture evidence, not production art or complete G2/painting acceptance.

`uv-transform-material-window.png` and `uv-transform-preview.png` are actual captures from the independent Material Editor, using real OBJ/MTL/PNG source and native corner UVs. The human UI test checks that its canvas observes the exact current native snapshot, selected face and assigned material. The texture/UV tab shares native preview/apply/cancel, history and saves with modeling; it owns no editable copy of the scene.

`uv-transform.btop` is the actual saved material-bearing UV project. `uv-transform.glb` is actual published runtime output. `uv-transform.obj.zip` is the actual human OBJ/MTL/PNG bundle export. Every game-decoded UV is checked against native state, unchanged geometry/normals retained, and an asymmetric texel proves that the half-width UV move affects actual runtime texture sampling. Fresh reopen and archived native state are exact. Original OBJ remains unchanged.

`EditorUvTransformTest --stress` in the independent Java17 JAR checks face/connected-UV-island move, bounds/origin rotation and signed scale; traversal stops at authored seams/material boundaries and distinct source identities. One shared undo restores exact state; canceled/stale/neutral/invalid inputs cannot mutate native authority. Maximum120000 faces/360000 indexed vertices passes under256MiB. `AssetImportUvGuiTest` drives actual human controls and width-bounded properties. Full native service/GUI/recovery/headless suite also passed; the final width-only layout refinement received its own targeted UI check.

`standalone-6445403a-failure.txt` records an actual source644 standalone failure. `DependencyAliasControl.java` reproduces it against the exact published JAR using an in-root directory junction: the old resolver compares a lexical alias with the canonical asset root and refuses. Corrected source canonicalizes both before containment. The same alias passes, while actual outside-root paths and changed dependencies still refuse. This is DEV213, awaiting corrected-source CI. DEV214 records and fixes horizontal property clipping observed during UV integration.

`artifacts.json` records exact source/fixture hashes and source-specific status. Source644 native41506330136 succeeded; full41506342845 and GPU41506353573 were still live/queued at this checkpoint. Preserve their handles; do not infer terminal success or replace live runs. New-source dispatch/results are recorded separately.

Open: explicit seam marking, unwrap/packing/pins/relax/density tools, native paint layers/brushes/MIPs/large textures, complete editable GLB hierarchy/pivot/attribute preservation, remaining G2 modeling/selection and original G0–G12. UV transforms alone do not close these requirements.
