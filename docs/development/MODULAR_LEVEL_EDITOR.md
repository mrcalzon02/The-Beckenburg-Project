# Modular Level Editor (Java 17)

The source of truth remains each game's actual scene/collision data, not a duplicated preview.

## Portable kit

The Java-only `org.beckenburg.editor` package has **no Castle Beckenburg imports**:
- `EditorProject<S>`: adapter interface exposing scene nodes, named assets, snapshot/restore and save.
- `EditorTool`: shared select/move/rotate/scale/wall/floor/roof/prop/light/paint/delete tools.
- `EditorHistory<S>`: bounded, transactional undo/redo (failed commands rollback).
- `EditorAssets`: secure `scheme://path` paths and interchangeable disk/resource providers.
- `EditorPalettePanel`: compact Swing asset dock, filter by category/name.

The first concrete adapter is `CastleEditorProject`; it maps Castle Beckenburg's saved `CastleLevelEdits` geometry, props, lamps, materials, spawn and interaction stations to the generic editor contract. Its game-specific 3D viewport remains `DeveloperLevelEditor` / `SceneRenderer`, rather than incorrectly claiming this is an OpenGL toolkit.

## Add another project

Implement `EditorProject<MySnapshot>` with its own hierarchy, asset descriptors, snapshot/restore and save; use `EditorHistory<>(project,60)` for all commands and `EditorPalettePanel(project, listener)` to browse its assets. Implement a viewport adapter for that game's rendering and hit-testing; do not import `CastleLevelEdits` or `CastleHubMap` into the portable kit. Keep project assets in their original project.

## Architecture authoring

Walls/floors/roofs are real `CastleLevelEdits.Surface` records. Drawing and inspector transforms modify persistent records read by castle geometry, shadows and physical collision. The editor supports snapping, fly/top/ground/upper camera presets, painting, duplicate, delete and undo/redo. Existing `castle.properties` without surface records automatically reconstructs its initial castle architecture; exported saves include full surfaces.

## Limits / next integration

The shared core is reusable now; the castle-specific 3D viewport is **not** yet a turnkey cross-game renderer. Hardware instancing and GL VAOs/VBOs are intentionally not claimed: this game currently has a Java software rasterizer. Cross-project ports should inject a renderer, physics/nav adapter and save format while preserving the project-independent core contracts. Moving legacy grid boundaries has to be reconciled with the grid topology (not just visual meshes). This is a real remaining limitation.
