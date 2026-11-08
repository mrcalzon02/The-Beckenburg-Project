# G2 textured model roundtrip

The Java 17 host acceptance suite runs `G2TexturedModelGuiTest` with a 256 MiB heap and an isolated user asset library. The Model Editor created an 8-vertex box, exported it as OBJ and imported the authored per-corner normals. The separate Material Editor bound the original four-color PNG to each of the 12 triangles. Back in the Model Editor, a 30-degree world Y rotation and 1.5× world X scale went through preview and Apply. Undo and Redo restored the exact native snapshots.

The registered game asset matched the independently computed positions, inverse-transpose normals, UVs and outward winding at all 36 corners. Every game face sampled the expected top and bottom texels. The editor reopened the textured OBJ bundle, GLB and native document with six authored quads. The archived native state matched the one saved by the editor.

| Evidence | Contents |
|---|---|
| created.obj, checker.png | Geometry and texture supplied through the editor |
| edited.btop, edited.obj.zip, edited.glb | Published native archive and interchange exports |
| materials.png, model.png | Captures of the two actual editor windows |
| game.png | SceneRenderer drawing the asset from the game's import registry |

The sample meets the textured roundtrip exit check. G1 remains partial in the current-state audit. G2 still has standalone JSON glTF interchange and incremental updates beyond the wireframe projection to verify. Native undo history is session-local. The fixture measures correctness and heap bounds for this asset. It does not measure agent token consumption or establish hardware acceptance.
