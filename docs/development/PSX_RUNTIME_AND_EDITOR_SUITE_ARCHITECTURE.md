# PSX Runtime API and Authoring Suite — Architectural Direction

Status: **proposed platform direction**, approved as a design exploration, not an implemented engine or a mandate to migrate other repositories.

## Thesis
Beckenburg's level editor can become the foundational authoring interface for a **reusable PSX-style Java 17 game development suite**, but the shared **runtime APIs** must remain independent of Swing, Beckenburg castle data, and editor UI. The suite edits and playtests the real game world through those APIs; it must never maintain a second preview world with different lighting, geometry or physics.

## Source standards
Owner-supplied **Level Editor gold standards.pdf**, five pages, defines WYSIWYG parity, manipulation/snapping, hierarchy/layers, inspector, undo/redo, validation, saving and playtest, asset browsing, global environment, profiling, dockable UI, collaboration/recovery, prefab and scatter tooling, and the four-dock workspace. See `LEVEL_EDITOR_OVERHAUL_AUDIT.md` for acceptance requirements.

## Separation of concerns
1. **Runtime core (headless where possible):** scene graph and stable IDs; transforms and component/property schemas; asset references; clock and deterministic simulation; collision/nav and trigger interfaces; serialization and version migration; event/command systems; audio/world/environment contracts. No Swing or hardcoded Beckenburg entities.
2. **Rendering adapters:** software PSX rasterizer first (current Beckenburg implementation), with a backend contract for later alternatives. Render the same scene and materials in editing and play. Include stable picking, selection visualization, light volumes and profiling hooks.
3. **Editor shell:** docks, gizmos, hierarchy, typed inspector, asset browser, undo transactions, live diagnostics, prefab and brush authoring. The shell consumes runtime interfaces and remains separate from game-loop logic.
4. **Game-specific plugins/adapters:** Beckenburg ghost logic, weather, investigation rules, van, rituals, dialogue and campaign; other original games can provide their own systems, palettes and inspectors. No implicit cross-project migrations or shared asset copying.
5. **Build/pipeline:** standalone editor kit and game launchers, reproducible format/schema checks, runtime smoke testing, and optional import/export/share mechanisms. Blender is never a player runtime dependency.

## API seams to define before code migration
- `RuntimeScene`: enumerate/query by stable ID, parent/child and layer, CRUD commands, immutable snapshot.
- `RuntimeComponent` + `PropertyDescriptor`: typed editing and validation without hardcoded Swing fields.
- `RuntimeRenderer`: scene camera, shared lighting and material state, frame capture, picking, resource counters.
- `RuntimeSimulation`: pause/resume, step, playtest start/stop, deterministic edit snapshot and rollback.
- `RuntimePhysics`: raycast, collision/nav/debug geometry, floor dropping, placement validity.
- `RuntimeAssetCatalog`: searchable assets, references, tags, thumbnails, prefabs and missing-reference reports.
- `RuntimeDocument`: atomic save/load, stable IDs, schema versions, migrations and error reporting.
- `RuntimeDiagnostics`: FPS/memory/draw metrics, nonblocking warnings, jump-to-object diagnostics.
- `EditorExtension`: optional type-specific inspectors, commands, tools, menus and safe scripts.

These names describe contracts to design; they are **not existing classes**.

## First-class environment module integration

Three peer modules belong in the portable editor/runtime integration design:

| Module | Current integration position | Editor responsibility |
| --- | --- | --- |
| Water | Existing `dev.mrcalzon.water` module and `EnvironmentModuleEditor` integration | Author and preview actual water placements/material settings with game renderer |
| Weather | Existing `dev.mrcalzon.weather` integration through the environment editor | Configure live environment and atmospheric effects through the unified weather system |
| Lighting | **Under production; integration pending module availability** | Discover its published runtime/editor API, then connect real fixture, light-volume, shadow, exposure and effect controls |

**Integration rule:** each module retains its own runtime simulation, serialization, performance management, and asset semantics. The editor exposes adapters/inspectors and reflects the module's authoritative state; it must not implement a competing shadow system or fork a separate editor-only lighting simulation.

For every environment module, use a common integration lifecycle: capability discovery, load or attach to a scene, expose relevant edit operations and inspector metadata, notify on live state changes, support atomic save/restore and playtest, register diagnostics, and detach/clean up. This is a **target contract**, not a claim that the modules already support every operation.

Before integrating lighting, inspect the actual delivered module and its exports, compare its lighting/shadow APIs to `SceneRenderer`, and retire duplicated fixture behaviors only when parity is verified. Lighting and water/weather must interact coherently (reflection and refraction where supported, fog attenuation, wet-surface shading, shadowing, day/night lighting) without requiring fictional effects or duplicate authority.

Weather is the authoritative source for fog banks and related atmospheric conditions. Do **not** bring back the old fixed fog/power-outage/weather mission-condition selector. Environmental power systems can affect illumination without becoming a competing weather option.

Avoid a hard dependency on the unfinished lighting module: the existing game and editor must compile and remain functional until its real API is available. Shared editor contracts should remain Java 17 and game-agnostic; adapters can be independently registered when installed.

## Safety and lifecycle
- Use one in-memory scene instance for editing; playtest snapshots dirty state and returns safely without overwriting editor changes or multiplayer authority.
- Editor scripts run within restricted, auditable APIs; never turn imported community levels into arbitrary host-code execution.
- Non-destructive schema migrations, stable asset IDs, autosave/recovery, conflict detection, and deterministic prefab instantiation.
- Keep existing Beckenburg playable during incremental extraction; adapters permit old and new formats side by side during transition.
- Only objective build/launch/essential-function/multiplayer failures block releases. Quality diagnostics do not.
- A new shared-library repository should be considered **only after** proven extraction and at least one non-Beckenburg sample game adapter; otherwise preserve main branch work in Beckenburg.

## Implementation sequence
0. Stabilize Beckenburg's active release and regression recovery gates.
1. Extend portable editor contracts with scene IDs, layers, typed properties, and renderer/physics/serialization interfaces; add a tiny synthetic test project.
2. Route Beckenburg live scene, collision and rendering through those interfaces without changing visuals or behavior.
3. Implement actual axis gizmos, object/surface snapping, hierarchy, workspace and real playtest toggle.
4. Add world configuration, audio, weather, diagnostics, asset thumbnails, prefabs and portable serialization.
5. Prove second-game reuse with a small independent PSX sample; only then decide whether to extract the suite into its own repository.

## Acceptance gates
- Identical scene assets, geometry and rendering paths between editor and live game.
- Save/reload roundtrip retains IDs, transforms, property values and references.
- One-button playtest returns to the unsaved edit snapshot, without leaking state.
- An unrelated Java 17 project can use the runtime/editor contracts without importing Castle classes.
- No regression in Beckenburg's gameplay, multiplayer networking or release updater.

No claim of a working general-purpose engine, complete editor, successful test run or shipping artifact follows from this document alone.

## Curated PSX source acceptance (2026-10-30)

Five owner PDFs are filtered in the [human engineering wiki](../wiki/psx-editor-engineering-guide.md) and [machine-readable adoption manifest](psx-editor-reference.json). They establish priorities for scene authoring, measured lighting, safe navigation and accessible PSX-style UI. They are guidance, not evidence of completed implementation. An offline reference viewer ships as part of the portable editor core and is available from the Beckenburg editor command strip.
