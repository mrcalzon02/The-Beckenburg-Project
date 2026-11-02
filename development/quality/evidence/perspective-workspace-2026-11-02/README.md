# Actual perspective model workspace

The PNG is a real Swing Model Editor capture of two original native OBJ triangles at different depths, after a snapped foreground-face move. It is a UI fixture, not production artwork. Source OBJ, exact native BTOP and actual published game GLB are retained beside it.

Human checks: select the visible foreground face; right-drag orbit, middle-drag pan, wheel zoom and Fit; prove native state/epoch unchanged; visible box excludes the hidden three native vertices; Escape cancels an axis gesture; an exact snapped X+.25 move commits through shared history; undo/redo, save/fresh reopen and every actual game corner match. Material Editor and main Level Editor layouts are unchanged.

Portable checks: same immutable camera for draw and native BVH selection; reciprocal-depth occlusion and perspective-correct UVs (analytic hit expects1/6 rather than affine1/4); box/lasso and stable IDs; exact projective axis inversion over multiple orbit/depth configurations; camera-horizon refusal. Both perspective and legacy orthographic picking pass120000 faces/360000 vertices under256MiB in the independent Java17 JAR.

native-suite.txt records the whole actual native service/model/material/UV/GLB/recovery/CLI suite passing. The new camera is observation state only; no editable proxy scene or agent mesh endpoint is introduced. Camera framing stays outside native scene serialization. Loop/ring selection, remaining local/normal modeling, bevel/slide, incremental buffers, unwrap/painting and later gates remain open.

DEV218: negative.txt reproduces the unpublished missing binary overload; positive.txt passes the same prior-compiled PickingAbiControl.class against the final implementation (and final standalone JAR). Original enum-typed signatures are preserved; no published consumer needs recompilation.
