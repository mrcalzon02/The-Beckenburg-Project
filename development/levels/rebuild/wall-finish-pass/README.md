# Roster wall finish

The shared native level importer assigns plaster, concrete, masonry and timber finishes to plain structural walls in game. Metre-based texture coordinates cross the authored joints without changing wall, doorway or stair geometry. Existing textured meshes and their UVs are retained. Castle, the House, Blackpine and Hollow Creek use their dedicated surface implementations.

The [roster audit](roster.tsv) decoded all 25 active native exports, including the mortuary annex. It verified 61,147 newly textured wall triangles, the original positions of every triangle, the original UVs and material names on every unaffected face, and actual diffuse variation in every new material. It also checked that the full-height neutral structural faces received a texture. The current Blender and GLB source files are retained. The game-side finish appears when the native importer loads them.

Software and OpenGL packages show the finish from standing height in the Asylum ward corridor, Blackwater turbine hall and Monastery memorial crypt. The [Asylum OpenGL view](captures/opengl/asylum-ward-corridor.png) shows both sides of the textured spine. The local packages are `build/beckenburg-game.jar` and `gpu-client/target/BeckenburgGPU.jar`. Native map selection still uses each site's review flag. For example, run `java "-Dbeckenburg.review.asylumNative=true" -jar build/beckenburg-game.jar` to select the rebuilt Asylum.

Source-only roster locations still need game integration. The GPU production package was built with Maven test compilation skipped because the GPU test source set does not resolve the root `AllLevelAtmosphereCapture` test helper. The focused OpenGL captures ran against that rebuilt production archive.
