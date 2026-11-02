# Material workspace fixture evidence

These are actual human-editor/native/runtime captures of a deliberately asymmetric four-color two-triangle fixture, not production model art or an art-quality acceptance claim.

- `material-workspace.png`: independent Material Editor window, native UV canvas/face assignments and actual game-renderer preview. Modeling has its own window; the level editor layout is unchanged.
- `material-binding-preview.png`: actual captured native assignment before Apply; authoritative project unchanged until commit.
- `material-binding-runtime.png`: published fixture through the production developer asset-room renderer.
- `material-binding.btop`: actual human-saved v3 native project with immutable original PNG and face assignment. Fresh native open and publication archive equality are tested.
- `material-binding.glb`: actual published runtime derivative; exact original colors/origin/alpha/tint/wrap and face bindings are checked through GlbProps, not an alternate scene.
- `material-binding.obj.zip`: actual human material-preserving export, re-read and validated by the same source importer. Standard OBJ/MTL/PNG are derived from canonical native material data; conflicting views refuse.
- `material-binding-original.png`: original captured PNG, deliberately changed on disk after preview in the integration test to prove the candidate owns the reviewed bytes.
- `previous-checkerboard.glb` and `MaterialLossControl.java`: exact previous3ca731b5 pipeline/normal/writer Git blobs compiled independently reproduce discarded OBJ/MTL assignments and generated fallback. This is a negative control, not editable runtime authority.
- `artifacts.json`: exact byte lengths/SHA-256 of fixture outputs.

Local checks: complete Java17 host compilation; independent Java17 editor JAR; native service, disk recovery, all human topology/modeling UI and public headless protocol suite; EditorMeshMaterialTest; AssetImportMaterialsGuiTest including two-window ownership, UV/list/native selection sync, preview/history/save/fresh open/publication, material bundle, real sampling and runtime registration cleanup. Existing portable topology/OBJ/normal/repair/transform/primitive/picking controls pass; maximum120000-face/360000-vertex normal transform stays under256MiB.

Source-specific remote CI is recorded separately after publication. Previous bcd74698 GPU41486319863 really passes all11 map types and3 UI cycles. Its full workflow41486307374 is green only because the manifest is advisory: raw308 passed/2 failed/0 skipped. DEV211 and the retained raw excerpts preserve that unresolved atlas/side-silhouette discrepancy. Do not describe it as310/310 or bless revised visual goldens.

Incomplete: painting/layers/channels, MIPs/4K/8K, unwrap/packing/pins, live texture links, remaining native modeling/selection, original GLB hierarchy/pivots and editable GLB interchange, complete G2 and all remaining G0–G12 gates. See the complete Texture Painting integration map.
