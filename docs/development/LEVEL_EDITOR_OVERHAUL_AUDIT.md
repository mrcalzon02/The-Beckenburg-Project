# Level Editor — top-to-bottom runtime-parity audit

Status: in progress. This is an implementation acceptance contract, not a claim that all features exist.

## Verified in source (2026-10-29)
- `DeveloperLevelEditor` is a Swing viewport using `SceneRenderer.drawCastle`, with a 90 ms render timer.
- Castle-only `CastleEditorProject` maps to persistent `CastleLevelEdits`.
- Portable `EditorProject` currently exposes flat nodes, assets, snapshots and save; it does not expose renderer, collision, layers, parent/child hierarchy, triggers, playtest or diagnostics.
- `EditorHistory` has transactional rollback and a bounded 60-snapshot undo/redo stack.
- Existing viewport tools include SELECT, MOVE, ROTATE, SCALE, VERTEX, EDGE, WALL, FLOOR, ROOF, PROP, LIGHT, PAINT and DELETE. A tool enum is not proof of polished axis gizmos.
- Fly controls include WASD, Q/E and mouse drag; renderer position is currently clamped to the castle editing area.
- Export currently copies Castle-specific properties; a universal versioned scene format is not implemented.
- Grid selection now provides Off / 0.25 / 0.5 / 1 / 2 metres (commit 1ee8ab0).

## Required architecture
1. Keep one scene data model for editing, rendering, collision and playtesting. Adapter boundaries inject **the same** runtime renderer, lighting, shaders/materials, physics, audio, weather, entity behaviour and interaction components where applicable. Never maintain a hand-synchronized approximation.
2. Support live editing with selective invalidation: changing a door must not rebuild an entire level or flush the full screen. Dirty object/light/collision regions are updated only as needed.
3. Expose canonical, stable object IDs, parent IDs, type, layer, transforms, properties and asset references. Hierarchy grouping and filtering must not modify actual gameplay state.
4. Render interactive X/Y/Z move, rotate and scale gizmos with local/world axes and pivot control. Apply drag changes as one undo transaction on release; Escape cancels.
5. Support grid sizes, vertex/edge/surface/object snapping, collision-aware placement hints, adjustable work planes and visible level/floor filters. Snapping must not silently offset assets through walls.
6. Reuse game runtime for instantaneous playtest. Snapshot unsaved edits, enter gameplay in same process, then restore editing state without losing changes. Editor-only overlays and free-cam must be excluded from actual playtest.
7. Inspector must dispatch typed properties to game systems: doors and locks, lights, audio zones, weather, ghost/player spawns, evidence, ritual triggers, navigation, objectives and camera/monitor entities.
8. Saving must be atomic, versioned and deterministic, with stable ID references, import/export migration and helpful missing-asset warnings. Do not mutate shipped level data merely by opening it.
9. Diagnostics are NON-BLOCKING unless build/launch/core gameplay functionality is demonstrably broken. Broken asset references, missing spawn, bad collision, unreachable rooms, clipped models, basement access and texture problems should be shown in the editor with jump-to-object actions, not gate unrelated releases.
10. Levels created by the editor must load through the real level-selection/runtime pipeline. The castle is a test adapter, not the exclusive supported level.

## Concrete regression fixtures
- First house: opening a door must animate without full-screen flash, full scene reconstruction or severe hitch.
- Basement sites: stairs and openings must allow normal player traversal without clipping through terrain.
- First house: detect props protruding through walls, especially the front-left room.
- Castle: detect rear shelves intersecting external walls and lights embedded in masonry.
- Van: use the canonical van prefab in Castle and investigation maps.
- Weather: dense fog is a weather state, not a separate fixed mission-condition selector.
- Audio: spatial sound trigger preview must be audible within the editor.
- Multiplayer: playtest and live editing must not silently mutate network-authoritative session state.

## Acceptance sequence
A. Compile editor and run existing `DeveloperEditorLaunchTest`, `DeveloperLevelEditorDataTest`, `CastleEditorGeometryTest`, `EditorCorePortabilityTest`; verify new snap selection in viewport.
B. Implement a renderer/scene/physics adapter contract and source-level proof that edit and gameplay share the same scene objects and paths.
C. Implement full gizmos and collision-aware snapping with automated round-trip transform tests.
D. Implement hierarchy, parented transforms, layers, typed inspector, and undo transaction grouping.
E. Implement playtest toggle and verify dirty scene/return restoration.
F. Implement validation, schema migration, import/export, and load edited levels from the game.
G. Playtest real Castle, house and basement fixtures at runtime, record measured frametimes and defect reports.

**Release policy:** only objective build, launch, essential gameplay, and required multiplayer failures may block a release. Editorial, artwork, performance tuning targets, diagnostic scene warnings and subjective visual checks are advisory unless the failure objectively makes the product unusable.

## Unverified
No claim is made here that live gameplay, one-button playtest, parenting, axis gizmos, collision-aware snapping, general-purpose scene serialization or runtime regression tests already work. These require implementation and execution.
