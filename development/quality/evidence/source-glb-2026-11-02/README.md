# Actual source-backed GLB and separate module windows

These are real Swing/SceneRenderer captures from AssetImportGlbGuiTest, not mockups or production artwork. Model Editor and Material Editor use independent layouts on the same native project/history. The asymmetric 4x4 PNG and two instanced panels deliberately expose UV and hierarchy mistakes.

- glb-human-model-window.png: actual topology workspace after an independent instance vertex edit.
- glb-human-material-window.png: separate texture/UV/material workspace with actual runtime preview after a native UV move.
- glb-human-uv-preview.png: pending native UV edit, before Apply; cancel is checked to leave authoring state unchanged.
- glb-human-original.glb / glb-human-runtime.glb / glb-human-model.btop: actual original, published runtime derivative and exact edited native v4 archive.
- original.glb / edited.glb / edited.btop: portable save/history, UV/material/extrude test artifacts.
- matrix-negative.txt / uv-negative.txt: identical actual-game test fails with exact previous18001e8a GlbProps; current-positive.txt records corrected runtime pass. Old matrix puts first vertex at z2 instead of z1; old external UV sampler yields blue instead of source red.
- glb-source-matrix-audit.json:1088 bundled GLBs, zero nonidentity active node.matrix. This is source scope, not actual placed-map visual acceptance.
- native-suite.txt: entire real native service/model/material/UV/GLB/recovery/CLI suite passes.
- baseline-full-a813.txt: previous green advisory workflow still has raw308/310; DEV211 remains open. New CI acceptance must be recorded separately.

Independent Java17 JAR tests pass under256MiB: source-backed GLB plus existing native material, corner-normal and topology compatibility tests. Real purchased GLB, Blender UV/CRT, Hollow Creek BLEND and prepared-light checks also pass locally. Unsupported rigs/morph/additional/sparse attributes/texture transforms remain read-only/refused. This checkpoint does not close whole G2, painting or later gates. No new editable agent mesh endpoint or main level layout change.
