# Topological model editor workspace fixtures

Owner scope: the topological/model module only. The main level view is unchanged. Other modules own distinct workspace layouts. Gold standards section4 and the full applicability review are recorded in docs/editor/requirements/MODULE_WORKSPACE_AUDIT.md.

Actual Swing fixture captures show a single full-width working model view, contextual transform/primitive inspector, 900x620 operation, Focus View and native rotation/scale candidate comparison. These are UI fixtures, not production art or complete G2 visual/material acceptance. The native part outliner resolves current native stable IDs; layout, filtering, panel toggles and selection do not edit the native geometry.

Full native human/service/CLI suite passes locally. Independent Java17 editor kit verifies maximum120000-face/360000-vertex native transforms under256MiB plus primitive/OBJ compatibility. Real human transformations retain stable IDs/corner UVs, exact reviewed preview/history/native archives and every game-decoder corner. Authored normals/materials, later modeling and full arbitrary docking remain open. Main-view source has no diff against the pre-UI checkpoint.
