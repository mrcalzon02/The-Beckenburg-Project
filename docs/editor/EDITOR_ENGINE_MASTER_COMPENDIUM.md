# Master Compendium — Editor, Engine, and Game-Creation Recommendations

**Project:** Reusable Java 17 LevelEditorKit / Beckenburg-based authoring and runtime suite.  
**Prepared:** 2026-11-01. **Version:** 1.0 (document consolidation, not a claim of newly implemented software).  
**Source authority:** original owner-supplied PDFs where provided; exact repository-authored requirement and engineering guides where PDF originals are unavailable. This master compendium is a *single, self-contained text reference* containing the complete available extracted text of four new PDFs and the full text of the existing editor/engine requirement guides listed below. No owner source is silently rewritten into an implementation claim.

## How to use this compendium

- **Part I — Unified engineering brief:** the requirements across all editor, terrain, animation, AI, audio, scripting and genre sources, organized by shared contracts, dependencies and acceptance. This is an editorial synthesis and does not replace the source-derived details.
- **Part II — Previously committed implementation contracts:** existing canonical guides reproduced in full with their original repository paths. Their historical dates and status statements remain exactly as authored, even when newer work advanced them.
- **Part III — New original-document source transcriptions:** four complete available PDF text extractions with original page markers. Original PDF art, diagrams and exact pagination/formatting are not duplicated here.
- **Part IV — Earlier PSX editor and engineering recommendations:** text of source-informed guides for the prior nine-PDF PSX batch and owner's supplemental menu/campaign/billboard brief.
- **Part V — Terrain, environment, model audit and agent-specific detailed contracts:** the previously authored module plans, including requirements still unimplemented.
- **Source provenance is explicit.** Reference PDFs whose original binary is not in the repository remain represented by the existing review/engineering guide, **not** by a fabricated transcription. When reading historical appendices, treat their implementation statuses as statements at that earlier date; check the current source and CI before asserting that a milestone shipped.

### Source intake: owner-authored PDFs and briefs

**Current seven-source chain** ([source register](requirements/DOCUMENT_CHAIN.md)): Level Editor gold standards; Terrain editor requirements; Landscape layering; Topological editor (36 pp); Building a 3D Soundscape Environment Manager (19 pp); Engine base (29 pp); multi-genre game creation engine (19 pp).

**Additional PSX batch** ([verified reference ledger](../development/psx-reference-ingestion-2026-10-30.json), nine supplied filenames): Developing assets for the PlayStation 1 (PSX) architecture(1).pdf; Level Editor gold standards(2).pdf [same subject as chain source]; PSX animations gold-1.pdf; PSX level building(1).pdf; PSX lighting engine(2).pdf; PSX NPC-AI-1(2).pdf; PSX ui guide and user(1).pdf; PSX-era level editor.pdf; and "🏹 Sound Design_ The PSX _Arrow_ Aesthetic.pdf". The historical ingestion identifies six repeats and three new inputs within that batch, and gives source SHA-256 values, source lengths and adoption dispositions; **do not** treat these nine as nine entirely distinct new subsystems.

**Owner's 2026-10-30 inline specification:** Menu and Save Management, Campaign Scripting, Billboards and 3D Environments, preserved in a committed engineering reference rather than a PDF.

**Coverage includes:** PSX asset import, geometry, editable models, 3D scene and layout authoring, lighting, water/weather, terrain topology and layered materials, collision/nav and loading zones, rail/road infrastructure, skeletal animation, NPC/AI, environment sound, UI/menu/save, campaign/story graphs, billboards, multi-genre tools and registry, script runtime, machine/agent tools, headless testing, multiplayer, visual review and release validation.

## Part I — Integrated engineering brief

### 1. Core principle: one authoritative scene and one real runtime
The editor must manipulate the same authored game geometry, materials, assets, collisions, lights, portal/room volumes, cameras, navigation, weather and audio data that the actual game consumes. A preview-only substitute, second renderer or second physics environment is never evidence of correctness. The portable Java 17 editor owns data contracts and editing; Beckenburg-specific adapters own game integration. Deterministic source IDs, versioned schema, explicit coordinate axes, source fingerprints, save/reopen verification and protected user-authored layers are mandatory.

### 2. User-facing editor standards
Functional WYSIWYG viewport; hierarchy/scene graph; property inspector with typed inputs; consistent select/move/rotate/scale and local/global/normal gizmos; grid/vertex/angle snap; multi-select, duplicate, group, layer, prefab; real source asset browser; sensible four-dock arrangement; orthographic/perspective views; undo/redo; recoverable saved transactions; direct playtesting; performance and correctness overlays. Select the real target mesh face, not a fake bounding-box center; respect the model's natural facing, original UVs, pivot and collision. Capture actual game and dev-room screenshots for visual review. Visual/style defects are diagnostics unless they break required functionality.

### 3. Model/topology and rigging
Editable half-edge topology, predictable manifold boundaries and explicit errors; primitive generation with non-destructive parameters; extrude/inset/bevel; loops, slides, sculpt/flatten/smooth, symmetry and remesh with preview; efficient incremental normals/VBO updates; topology counts and normal debug. Preserve source UV/materials and use explicit coordinate/handedness/winding conversion, GLB interchange and a separate native working format. Support tablet input only on capable devices and rigid/organic rig weighting with recognized skeleton templates; avoid destructive auto-rig defaults.

### 4. Terrain, materials, water, roads and subterranean spaces
Stitched 16-bit global height samples, sparse tile coordinates, shared seams, revision-safe non-destructive sculpt/masks, region and provenance aware undo; managed roads/rails/rivers and cut/fill side grading; snap-aligned banks, rocks, water/river joins and meaningful real infrastructure meshes. Four-tier landscape composition (global data, material domain, blending, output). Genuine geometric entrances and separate underground solids because a heightfield cannot represent roofs and floors at the same horizontal coordinate. Height/collision/LOS/nav share canonical queries; rendered visual LOD must not rewrite physics or distant actor visibility.

### 5. Lighting, visual PSX profiles and presentation
Portable lighting, fog/weather and water interfaces; correct geometric light contribution/occlusion; authoring for baked versus dynamic lighting where supported. Retro mode supports limited palette/CLUT, dithering, texture filtering, vertex/Gouraud light appearance, affine wobble as *opt-in style*, and asset budgets. Modern renderer features are optional capabilities. Import assets with their real textures; don't reassign a flat diffuse map to every part, replace imported spritesheets with placeholders, or confuse dev previews with gameplay captures.

### 6. NPC, AI, navigation and deterministic simulation
Expose the *actual* ghost/hunter and future character rule registries to structured inspectors; use bounded AI/path budgets, sensory/perception overlays, path debugging, portal/room transitions and exact authored navigation surfaces independent from visual terrain LOD. Toggle automatic nav updates versus manual rebuild; permit exclusion surfaces, disconnected entry/exit zones and three terrain-to-subterranean seam methods. Functional AI-versus-AI test matches, seed/replay, host-level telemetry and multi-floor path acceptance should validate actual engine behavior.

### 7. Spatial audio and accessible sound design
Acoustic zones/material matrices, room-to-room portal transmission, geometry-based obstruction, HRTF or stereo fallback, EFX reverb when supported, distance curves, interpolation and bounded worker-thread voice buffers; integrated with existing SoundDocument/Volume tools. Separate dialog/voice, ambience, effects and music, downmix safely to mono/stereo/TV, make TV/Night and Theatre dynamic-range options, preserve speech intelligibility and avoid hard clipping, pops and long-stream memory bloat. Retro PSX "Arrow" guidance is a **sound-design reference**, not authorization to add bows.

### 8. Animation, UI, campaigns and authoring workflows
Animation clips, keyframe reduction, fixed-rate evaluation when useful, rigs and weight inspection, true model animation in development room. Save-slot/memory-card-inspired UI remains a style option; real saves must have bounded state schemas, versioning and tests. Campaign/event/dialogue graphs and billboard/3D displays must expose dependencies, trigger conditions, asset references, preview and safe round-trip. Avoid separate fake campaign simulation.

### 9. Multi-genre architecture without a giant compulsory engine
Keep a central versioned archetype/schema registry, portable event/turn/tick, deterministic randomness, save schema, input maps and authorized simulation tools. Add optional plugin modules for puzzle, strategy, racing, RPG, horror/park, city, action, survival and procedural genres **after** core scene/physics/editor interfaces are proven. Do not migrate other game repos merely because they are adoption examples.

### 10. Scripting, machine tools and agent interfaces
Typed semantic read tools, then validated reversible modifications, then save/readback/real render/verification, finally optional local headless and authenticated agent/MCP access. Same human operations and same authoritative game scene, never a parallel agent world. Scoped VFS, immutable DTOs, no arbitrary native pointers or shell, capability controls, idempotent action keys, bounded queue, cancellation, diagnostic token/revision and collision-free editing. Separate optional scripting runtimes; a ClassLoader alone is not a sandbox. Java 17 compatibility matters: virtual threads (Java 21), modern stable Foreign Function & Memory API (Java 22) and record-pattern switch snippets are not drop-in Java 17 code. Existing fully functioning base must be retained.

### 11. Staging and acceptance
Preserve the [G0–G12 implementation plan](requirements/STAGED_INTEGRATION_PLAN.md) below verbatim; do not equate planned with implemented. Prefer objective acceptance gates (build, launch, persist, runtime play, multiplayer sync, update) while treating art quality, sound taste and visual review as diagnostic; report failures with exact commit/CI. All new authoring modules should be portable and isolated from the Castle's game logic, but their host adaptation must work in real Beckenburg before adoption elsewhere.

### 12. Known consolidation limits and no-loss policy
The original four newest PDFs were not committed as binary, only their full available text extractions. The earlier seven-plus source PDFs have existing digest/analysis documents but are not mounted as original binary files in this session. This master includes the *full text of available existing engineering/requirements guides* instead of inventing missing PDF paragraphs; it does not claim that those guides are verbatim copies of originals. Original internal links in copied documents remain historical and may resolve relative to their original paths, which are explicitly named at each appendix. Machine JSON ledgers remain registered rather than duplicated as large raw protocol dumps.

---

## Part II — Original committed architecture and editor requirement contracts

_The following source documents are reproduced as originally committed, with source path labels. Repetition is intentional where independent requirements use overlapping terminology. If the original source has a later amendment, do not assume this historic snapshot is the current implementation state._



---

### Source: Standalone editor and human-workflow gold standards

**Original repository path:** `docs/development/LEVEL_EDITOR_OVERHAUL_AUDIT.md`.

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

## Authoritative input: Level Editor gold standards.pdf (5 pages, supplied by project owner)

This document governs editor acceptance. All features below are requirements, not claims of implementation. Keep this list distinct from current capabilities.

### Ten essential standards
1. **True WYSIWYG:** exact runtime renderer, lighting, physics and assets; edit feedback without launching another build.
2. **Placement:** axis gizmos, grid/object/surface snapping, collision-free fly camera.
3. **Organization:** distinct visual, terrain, collision and trigger layers; parented scene tree; full typed inspector.
4. **Safety and speed:** reliable undo/redo, consistent shortcuts, in-editor validation for missing references, spawns, materials and unreachable spaces.
5. **Persistence and testing:** deterministic versionable serialization, single-action playtest and return, optional player-facing import/export and workshop sharing.
6. **Assets:** centralized browser across meshes, sprites, audio, textures and blueprints; tagging/collections; rendered thumbnails.
7. **World controls:** global gravity, weather, time cycle, sound, fog/exposure/post-processing and player/objective baselines.
8. **Diagnostics:** FPS, draw/triangle/memory metrics, adjustable budgets and visible navigation/collision/trigger overlays.
9. **Customization:** docking and detachable panels, scripting/macro API with safe bounds and preset workspaces (lighting/blockout/AI).
10. **Collaboration:** source-control conflict awareness, autosave/crash recovery, chunked/sub-level organization.

### Professional quality-of-life standards
- Scatter/paint brushes with deterministic random seeds, density, scale and rotation controls.
- Multi-object replace-in-selection, preserving transforms and references.
- Prefab group authoring and re-use across maps (canonical vehicle, furnishings, structures).
- Drop to floor, surface hit-testing and nearest-collider alignment.
- Right-click context menus appropriate to entity type.
- Camera bookmarks and isolate/focus selection.
- Breadcrumb paths for nested levels and hierarchy groups.
- Favorites, drag-to-replace, reference remapping and customizable hierarchy colors.

### Default four-dock layout from source
- **Center:** maximized live viewport with small overlays and optional full-screen mode.
- **Left:** searchable hierarchy with eye visibility toggles and edit locks.
- **Right:** collapsible inspector with scrubbable and relative numeric fields.
- **Bottom:** collapsible asset browser and live diagnostics/console.
- **Top:** tool/snap/local-world controls and prominent Play Level action.

Maintain compactness; no duplicated/inert panels, tabs or controls. Any UI element must operate a real editor or runtime function. F11 may control editor viewport presentation but must not be a required in-game gameplay control.

### Current incremental changes
- Selectable placement grid: Off, 0.25, 0.5, 1, 2 metres; commit `1ee8ab0`.
- Four per-session camera bookmark slots with Ctrl+F1-F4 store / F1-F4 recall, plus toolbar buttons; commit `073ba8b`.
- Both are source-level changes; no runtime compile or visual verification claimed.

### Priority and tests
P0: runtime parity, round-trip saving, restore-on-playtest, crash-safe save and functional editor launch.
P1: full gizmos/surface snapping, true hierarchy/layers, typed inspector, asset thumbnails and console.
P2: profiling, world and weather authoring, prefabs, multi-select/replacement and scattering.
P3: dockable presets, scripting, chunk authoring, optional community sharing and collaboration locks.

Test each requirement using representative Castle, house, basement and outdoor environments. Ensure environment controls respect the unified weather system: no reinstatement of the removed fog/power-outage fixed-condition selector. Nonfunctional diagnostics do not gate releases, consistent with owner release policy.

## Source-integrated scene audit — 2026-10-30

The Castle editor command strip now offers **SCENE AUDIT** in addition to **PSX GUIDE**. `CastleEditorAudit.inspect` reads the same `CastleLevelEdits` snapshot and `wallBlocks` collision used by the running game, checks centre intersection of lamps and props with authored walls, globally duplicated IDs across Castle authoring kinds, blocked spawn tiles and wall-embedded spawns. Findings include kind, ID and coordinates; the dialog has refresh and jump-to-object actions, clears hierarchy filtering, positions the editor camera and highlights the selected object. No finding gates publication or rewrites a map automatically. Wall-mounted fixtures and tightly placed furniture require human review; the centre-point test does **not** prove complete mesh bounds or absence of all penetrations.

`CastleEditorAuditTest` creates a temporary wall, prop and lamp and checks collision-backed warnings, disappearance after wall removal, duplicate IDs and blocked spawn state. The focused Castle workflow runs this optional diagnostic with `continue-on-error: true`. Successful Java 17 compile and test execution must still be checked against the relevant GitHub Actions run. This is a **Castle-specific real-scene diagnostic**, not proof that the future portable `RuntimeScene` core supports all scenes or all requested inspector authoring.

## Owner-source continuation (2026-10-31)

The original *Level Editor gold standards.pdf* audit is retained in the same
[owner document chain](../editor/requirements/DOCUMENT_CHAIN.md) as the later
Terrain editor requirements, Landscape layering, Topological editor, Soundscape,
Engine base and multi-genre game-creation inputs. See the
[staged integration plan](../editor/requirements/STAGED_INTEGRATION_PLAN.md)
for dependencies and explicit non-implementation status of new requirements.



---

### Source: Portable modular editor architecture

**Original repository path:** `docs/development/MODULAR_LEVEL_EDITOR.md`.

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

## Standalone distributable

Run `tools/build-editor-kit.cmd` on Windows or `sh tools/build-editor-kit.sh` on Unix to compile the reusable editor classes into `build/editor-kit/LevelEditorKit-Java17.jar` **without compiling Beckenburg, downloading 3D assets or installing Blender**. Each Beckenburg installer release also publishes this standalone jar and its SHA-256. The separate `standalone-editor-kit.yml` workflow checks compilation and a synthetic, unrelated project adapter against the jar alone.

## Add another project

Implement `EditorProject<MySnapshot>` with its own hierarchy, asset descriptors, snapshot/restore and save; use `EditorHistory<>(project,60)` for all commands and `EditorPalettePanel(project, listener)` to browse its assets. Implement a viewport adapter for that game's rendering and hit-testing; do not import `CastleLevelEdits` or `CastleHubMap` into the portable kit. Keep project assets in their original project.

## Architecture authoring

Walls/floors/roofs are real `CastleLevelEdits.Surface` records. Drawing and inspector transforms modify persistent records read by castle geometry, shadows and physical collision. The editor supports snapping, fly/top/ground/upper camera presets, painting, duplicate, delete and undo/redo. Existing `castle.properties` without surface records automatically reconstructs its initial castle architecture; exported saves include full surfaces.

## Limits / next integration

The shared core is reusable now; the castle-specific 3D viewport is **not** yet a turnkey cross-game renderer. Hardware instancing and GL VAOs/VBOs are intentionally not claimed: this game currently has a Java software rasterizer. Cross-project ports should inject a renderer, physics/nav adapter and save format while preserving the project-independent core contracts. Moving legacy grid boundaries has to be reconciled with the grid topology (not just visual meshes). This is a real remaining limitation.



---

### Source: PSX runtime / editor suite architecture

**Original repository path:** `docs/development/PSX_RUNTIME_AND_EDITOR_SUITE_ARCHITECTURE.md`.

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

## Source-document chain continuation (2026-10-31)

The previously supplied *Level Editor gold standards.pdf* remains authoritative
for its established editor requirements. The four new owner documents
(*Topological editor*, *Building a 3D Soundscape Environment Manager*,
*Engine base*, and *multi-genre game creation engine*) are indexed with the
prior terrain and landscape specifications in the
[unified owner requirements chain](../editor/requirements/DOCUMENT_CHAIN.md).
The [dependency-gated plan](../editor/requirements/STAGED_INTEGRATION_PLAN.md)
preserves this architecture's separation of portable kit and actual game runtime.



---

### Source: Existing staged integration plan (G0–G12)

**Original repository path:** `docs/editor/requirements/STAGED_INTEGRATION_PLAN.md`.

# Unified Editor/Engine Requirements — staged integration plan

**Owner intake:** 2026-10-31. **Target:** reusable, portable Java 17 editor/engine kit, integrated through existing Beckenburg host adapters without a parallel renderer, scene model, audio state, collision solver, or AI world. **Status:** PLANNED; this plan itself implements no features. **Source register:** [DOCUMENT_CHAIN.md](DOCUMENT_CHAIN.md) and [DOCUMENT_CHAIN.json](DOCUMENT_CHAIN.json).

## Authority, scope, and existing chain

The four newly supplied PDFs are primary requirements inputs, not drop-in implementation code:
- [Topological editor](sources/topological-editor.txt) (36 pages of extractable text): half-edge topology, low-poly parametric modeling, sculpt, retopology, selection, native/interchange formats, tablet/mocap, rigging/animation and presets.
- [Building a 3D Soundscape Environment Manager](sources/soundscape-environment-manager.txt) (19 pages): 3D acoustic materials, rooms, OpenAL/EFX/HRTF, portal/occlusion, asynchronous voices, mixer and consumer-hardware dialogue intelligibility.
- [Engine base](sources/engine-base.txt) (29 pages): polyglot script controls, batching, native/system capability broker, constrained AI tools, telemetry/structural diffs, compact commands and process-isolated diagnostics.
- [multi-genre game creation engine](sources/multi-genre-game-creation-engine.txt) (18 text-bearing pages in the extraction, original PDF 19 pages): puzzle, TBS/RTS, racing, RPG, arcade/horror, management, action, survival and procedural editor handles; shared archetype registry.

Pre-existing owner inputs stay in the same chain, not superseded:
- *Level Editor gold standards.pdf* -> [LEVEL_EDITOR_OVERHAUL_AUDIT.md](../../development/LEVEL_EDITOR_OVERHAUL_AUDIT.md) and [PSX runtime/editor suite architecture](../../development/PSX_RUNTIME_AND_EDITOR_SUITE_ARCHITECTURE.md).
- *Terrain editor requirements.pdf* -> [TERRAIN_REQUIREMENTS_INTEGRATION.md](../TERRAIN_REQUIREMENTS_INTEGRATION.md).
- *Landscape layering.pdf* -> [LANDSCAPE_LAYERING_PREPARATION.md](../LANDSCAPE_LAYERING_PREPARATION.md).
- Existing follow-ons: [TERRAIN_WORLD_SYSTEM_PLAN.md](../../development/TERRAIN_WORLD_SYSTEM_PLAN.md), [ADAPTIVE_TERRAIN_TOPOLOGY.md](../../development/ADAPTIVE_TERRAIN_TOPOLOGY.md), [TERRAIN_LAYERED_CAVITIES.md](../../development/TERRAIN_LAYERED_CAVITIES.md), [ADAPTIVE_MULTILAYER_NAVIGATION.md](../../development/ADAPTIVE_MULTILAYER_NAVIGATION.md), [MODULAR_LEVEL_EDITOR.md](../../development/MODULAR_LEVEL_EDITOR.md) and [AGENTIC_EDITOR_ACCESS.md](../../development/AGENTIC_EDITOR_ACCESS.md).

**Order of authority:** saved original source data and directly approved owner decisions; versioned portable/editor schemas; authoritative runtime host adapter; optional advanced engine implementations; diagnostic/visual polish. Existing native sources are not rewritten to satisfy speculative architecture. Human-usable tools, save/load and runtime binding precede AI mutation interfaces. Never silently migrate other game repositories.

**Shared invariant:** each document supplies complementary requirements. A topology mesh is an editable source representation, not a second game renderer; acoustics derive from authoritative geometry and material revisions, not decorative room labels; genre logic consumes one versioned data registry but does not change unrelated games; AI scripts call the same validated user operations.

## Stages and acceptance gates

### G0 — Requirements intake, deduplication and current-state audit
**Inputs:** all seven owner document references (four new, three prior) and current Beckenburg code/recovery work. **Deliverable:** this chain manifest, exact source provenance, requirement-to-module map, and audit of what exists, what overlaps, what is a proposal, and what cannot compile on Java 17.
- Record the existing EditorProject / RuntimeScene / EditorHistory / EditorMeshPipeline / TerrainLayerStack / AdaptiveTerrainTopology / TerrainSceneNativePhysics entry points.
- Audit SoundDocument, SoundVolume, LevelSoundscape, SoundVolumeEditor, EditorSoundLab/Profile, Room/portal geometry, existing javax.sound playback and LAN voice before selecting a hardware backend.
- Audit EditorAnimationClip/Lab and model import pipelines before adding a separate rig animation controller.
- Maintain separate states: EXISTING_VERIFIED, PARTIAL, PLANNED, BLOCKED, OUT_OF_SCOPE; a document commit never counts as feature completion.
**Exit:** source hashes checked, owner documents indexed, no orphaned prior requirements. **Plan entry accepted; runtime features are not.**

### G1 — Stable portable runtime/editor and transactional data contracts
**Depends on:** G0. **Sources:** Engine base; Level Editor gold standards; terrain/landscape contracts.
- Keep the independent Java 17 portable editor package free from Beckenburg game imports. Share versioned scene node/component IDs, transforms, material references, origin/axis convention (Beckenburg X/Z horizontal, Y up), revision fingerprints and resource ownership.
- Define stable authoring snapshots, incremental changes, bounded transactions, cancellation, undo/redo, atomic and journaled save/reopen, deterministic import, asset provenance and dependency dirty-region events.
- Preserve the existing game-owned graphics/physics backend and game-level authority. Expose **adapters**, not another render engine.
- Establish schema migrations with compatibility tests before adding new component types. Add capability flags so unsupported host features do not claim success.
**Exit:** standalone kit compiles on Java 17; round-trip/migration/rollback and existing scene tests pass; the same node is consumed by editor and real host.

### G2 — Topological mesh foundation and parametric modeling (human operated)
**Depends on:** G1. **Source:** Topological editor, pages 1–9, 15–18.
- Add an editable half-edge/winged-edge adjacency graph with twins, next/prev, boundaries, islands, face materials, UV seams, normals and explicit topology errors; keep the existing render triangle buffers as derived output.
- Non-destructive primitives (box, plane, cylinder, sphere, torus) with subdivision limits; explicit Apply/Commit. Implement mesh-manifold checks, degenerate rejection/repair, vertex welding, edge orientation and deterministic serialization.
- Add vertex/edge/face selection, correct triangle-level picking, visible-only / X-ray box/lasso selection, loop/ring selection, instant wireframe/normals inspection, topology counts, front/top/side/perspective views and axis/pivot gizmos.
- Global/local/normal transforms, vertex/grid/angle snap, highlighted anchors; reversible extrude/inset/bevel, edge slide and incremental dirty-buffer updates.
- Native topology+editor-state binary with version/migration; explicit glTF/GLB and OBJ import/export, coordinates/handedness, normals, UV and material roundtrip; no OBJ as hot editing state.
**Exit:** create/edit/export/import/reopen a textured sample with correct facing, geometry counts and transformed hard-edge normals; tests prove roundtrip, manifold controls, undo and bounded memory. Capture actual editor frames as advisory review.

### G3 — Sculpting, remeshing, high-fidelity interchange and input
**Depends on:** G2. **Source:** Topological editor, pages 1–18.
- Clay/standard, grab, smooth, flatten/planar brushes; falloff, screen-space radius, stroke interpolation, symmetry, topology-safe deltas, brush-local normal rebuild.
- Non-destructive quad-dominant remesh preview and controlled apply; do not arbitrarily remesh authored textured geometry, erase hand-placed UVs or change low-poly styling without authorization. Treat non-manifold repair as explicit preview/transaction.
- Asynchronous save/export and bounded regional GPU buffer updates. Add pressure/tilt and high-frequency tablet adapters only where hardware APIs are available, with mouse fallback. Mocap/VRPN/OpenVR adapters optional, isolated and bounded.
**Exit:** deterministic stroke replay and undo, geometry/UV preservation, no missing faces after sculpt, performance tests demonstrate no full-frame global mesh rebuild during local strokes.

### G4 — Terrain, entrances, infrastructure, layered landscapes and world geometry
**Depends on:** G1–G3 for arbitrary mesh placement; existing terrain plan remains separately authoritative.
- Finish stitched 16-bit source / non-destructive sculpt masks / shared seam heights / adaptive authored topology. Implement genuine cutout, the three seam-cut/destitch/tab methods, caves and multiple levels with distinct collision ownership.
- Retain portable rail/ballast/sleeper meshes and shared scene package; extend road side grading, junctions, poles/wires, riverbanks/rock geometry, physical water snap anchors, bridges/tunnels, and route profiles without overwriting artist paint.
- Integrate Landscape layering's four stages: Global Inputs & Data -> Material Domain -> Blend Domain Matrix -> Master Output Graph; modern splat/triplanar as optional adapters, PSX nearest/palette as a first-class profile.
- Bind persisted selected USER terrain world to game-level scene identity, actual rendering/materials, same authoritative physics support/LOS, navigation and multiplayer source revision; never use LOD proxy for physics or ray occlusion.
- Dirty regions trigger only necessary renderer/physics/AI/water recalcs. Continuous versus manual navmesh rebuild remains author-controlled, including disconnected loading-zone boundaries.
**Exit:** one saved/reopened authored scene displays authentic materials; physics/LOS/nav agree on exact same geometry, including cave/bridge and tile seams; real game screenshot and targeted multiplayer/AI route test. Existing terrain plan gates T0–T9 and adaptive multilayer navigation gates are not overwritten by this schedule.

### G5 — 3D acoustic model and safe hardware backend
**Depends on:** G1 and authoritative geometry/portal interfaces from G4 (the pure audio model can start earlier). **Source:** Soundscape PDF, pages 1–10.
- Reuse and extend SoundDocument/SoundVolume/LevelSoundscape. Define versioned acoustic spaces (AABB/OBB), material reflectivity/absorption including HF, humidity/air absorption as configured, density/diffusion, RT60, dry/wet mix and priority; save and preview in existing water/weather/sound editor.
- Build read-only snapshot bridge from real solids/materials/doors/portals and world position; periodic, budgeted LOS ray occlusion; delayed low-pass portal transmission and cross-room blending; no audio collision proxy incompatible with the game.
- Interface for stereo/2D fallback and optional LWJGL OpenAL Soft + EFX/HRTF with capability probe, context lifetime, listener orientation and safe device hot-plug fallback. Do not claim HRTF enabled merely after constructing an attribute list.
- Dedicated audio worker (actual worker thread compatible with Java 17), bounded command queue, gain/pitch/filter smoothing in elapsed time, inverse-distance-clamped or measured attenuation, Doppler where applicable, streaming/voice budgets.
**Exit:** real scene door/wall and listener tests change attenuation/filters coherently; WAV/loop streaming does not hitch render/audio, fallback runs without OpenAL or HRTF, save/reopen and no thread/context leaks.

### G6 — Audibility, dialogue intelligibility and speaker profiles
**Depends on:** G5. **Source:** Soundscape PDF, pages 6–19.
- Category/priority submixes: narration/dialogue, SFX, ambience, music, voice chat; voice virtualization and limits, limiter/headroom, attack/release smoothing, dry/wet controls and scripted event-to-asset variation maps.
- Explicit output profile/headphone HRTF, stereo TV, mono and optional surround; channel normalization, phase/mono checks, real DSP high-pass and compressor (not document pseudocode placeholders).
- User-selectable Theatrical and TV/Night/Handheld dynamic-range presets; frequency-conscious dialogue sidechain over approximately 1–3.5 kHz with measurements on low-end stereo/mono speakers; compare against simple broad ducking and avoid audible pumping.
- Audio snapshots/listening tests are diagnostics. No cosmetic or subjective mix score blocks release unless it objectively breaks sound/output.
**Exit:** speech remains intelligible in mono/stereo test mixes, no clipping/clicks in overlap/crossfade tests, controls persist and accessibility mode changes live correctly.

### G7 — Rigging, skinning, animation and presets
**Depends on:** G2–G3 and G1. **Source:** Topological editor, pages 18–36.
- Editable root/joint/parent hierarchy, bind matrices, IK/constraint review, left/right symmetry; per-vertex capped/normalized top-four influences, weight locking, capsule envelope fallback, optional measured diffusion weighting.
- Template profiles: biped, quadruped, arachnid, wheeled vehicle/tank, fixed-wing aircraft, helicopter, hovering drone. Document distinguishes tank and vehicle animation clips and lists eight loop types; do not incorrectly collapse authoring profiles.
- Optional SDF/medial-axis auto-rig with explicit 5-point human-assisted anchor fallback; mesh island assignment and mechanical rigid weights, no destructive auto-remeshing without preview.
- Extend existing EditorAnimationClip/Lab; apply procedural default loops to actual skeleton/buffer, glTF skin export with correct inverse binds, axis and winding mapping.
**Exit:** all template classes save/reopen and animate at runtime; wheels/rotors/bones deform or rotate the intended mesh only, no missing limbs or reversed normals; genuine review captures and data roundtrips.

### G8 — Universal data registry and deterministic simulation services
**Depends on:** G1. **Source:** multi-genre PDF, especially pages 1–8 and 16–18.
- Implement a portable, schema-validated entity/archetype data-table registry with types, IDs, versioned save/migration, CSV/JSON import/export and UI; one source of truth for rules instead of copied per-game registries.
- Reusable tick/turn/phase controller, deterministic RNG, event flags, dependency graph, safe formula language, item/stat modifiers, input remapping and bounded headless simulation.
- Provide game-specific adapters that consume this registry without mass-replacing current Beckenburg equipment/evidence/faction save formats.
**Exit:** sample puzzle tile, strategy unit, RPG item and horror gate use the same registry schema, with deterministic save/replay/undo and independent module packaging.

### G9 — Genre plugins, organized by dependency, not a monolithic rewrite
**Depends on:** G4, G7 and G8 as relevant. **Source:** multi-genre PDF.
- **Puzzle/board:** orthogonal/isometric/hex grid connectivity, stateful matches, cascades/refill, turn phases and seeded win/loss analysis.
- **TBS/RTS:** weighted graphs, influence/territory/fog overlays, hierarchical navigation, HSM/behavior trees, steering/box selection, flow fields for groups, economy/tech trees and deterministic tick/network diagnostics.
- **Racing:** curve/spline track extrusion with banking/checkpoints/laps, surface friction, wheel suspension and drivetrain/torque/drag panel, racing line/telemetry/ghost replay.
- **RPG/JRPG:** battle/turn-order graph, dialogue/quest/flag editor, items/status/progression formulas, menu focus and save-state compatibility.
- **Horror/park/arcade:** embedded mini-game cabinet input/screen context, stealth vision/acoustic debug, lock-and-key gates, breaker/utility cascade and attraction state.
- **City builder/management:** layered zones, network utilities, influence/decay, population/traffic aggregate simulation, large delta-compressed saves.
- **Action/survival/roguelike:** KCC, projectile pools, inventory/container synchronization, snapping structures, seeded generation and modifier synergies.
**Exit:** a minimal playable, saved/loaded sample in an isolated test project for each admitted plugin. Balance, visual styling and AI quality are diagnostic; broken gameplay/load/network is release-blocking only for shipped required features.

### G10 — Script/runtime integration and bounded machine capability broker
**Depends on:** G1 and usable G8 services; no prerequisite to regular human editing. **Source:** Engine base PDF, pages 1–13 and 24–29.
- Portable script engine interface; optional Groovy/GraalJS/GraalPy/Lua runtimes with version-negotiated classloader/process isolation. Treat each third-party script as untrusted, with strict immutable DTOs, opt-in capabilities, VFS, time/CPU/memory and output limits and bounded compilation cache.
- Do **not** assume a ClassLoader alone is a security sandbox. For hostile/untrusted scripts, enforce execution process isolation and OS-level resource limits, and supervise crash/restart. Never hand scripts raw native pointers, reflection, arbitrary library loading or writable host objects.
- Job queue, bounded batch/cancellation, idempotency keys, error provenance/correlation IDs, bounded telemetry and dry-run previews. Use Java 17 supported executors and NIO first; native/Panama/SIMD options require feature detection, a dedicated build target and stable fallback.
- Define tested scripting->portable action->runtime adapter chain without granting direct GL/Vulkan or filesystem access.
**Exit:** concurrent scripts cannot cross-write resources or escape project root, timeout/cancel do not hang UI, worker crash leaves host alive, replayed idempotency key does not duplicate edits.

### G11 — Agent/remote API, semantic commands and diagnostics (after human workflows)
**Depends on:** G1, G8, G10 and G0–G6 human/runtime gates for the target capability. **Sources:** Engine base PDF, pages 13–29; [AGENTIC_EDITOR_ACCESS](../../development/AGENTIC_EDITOR_ACCESS.md).
- Read-only inspect (scene hierarchy/selection, component schema, asset provenance, CPU/GPU/audio/AI diagnostics) then staged, permission-scoped edit transactions; never open hidden machine surfaces to agents by default.
- Define compact typed commands and validated compressed diffs; small macro DSL only as a syntax over *existing* checked operations, not a bypass. Versioned endpoint idempotency, audit events, rollback and bounded, structured error codes.
- Optional external API (local HTTP, SSE/WebSocket or gRPC if justified by measured use), authentication and non-public default binding; read-only before write/execute. Critical/destructive/native operations require explicit authorization and may be disallowed entirely.
- Agent verifies in the real editor/game host and collects actual screenshots. It does not use screenshot images as a substitute for correctness or invent completed tests.
**Exit:** end-to-end inspect->edit->save->reopen->verify against existing scene, refusal and rollback tests, no secrets in logs, agent action auditable. Retain existing machine task ledger milestones M0–M6.

### G12 — Cross-platform interoperability, acceptance and optional adoption
**Depends on:** accepted relevant gates, not on all optional genre packs.
- Build portable Java 17 library independently of Beckenburg; Windows/Linux compile and smoke, native optional backend availability, bounded memory/leak checks, source compatibility and load/save migration. Mac where actually supported.
- Game adapters run existing real renderer/collision/network regression paths; host/guest must agree on scene/material/audio revision semantics.
- Collect factual saved-scene screenshots and model-room reviews; visual quality and mix taste remain diagnostics rather than release-blocking checks.
- Publishing to ReverieVR, Catastrophic Diplomacy, Jewel Brook or any other project is **not** authorized by their mention as possible adopters. Separate explicit scope/approval and compatibility gate for each.
**Exit:** source + CI + host readback and actual runtime acceptance. No gate closes from documentation, compilation alone, an art preview, or an unverified release tag.

## Engineering corrections required before copying example code

The PDFs contain useful architecture and **illustrative code**, not a fully tested Java 17 implementation.

1. **Virtual threads:** java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor is Java 21, not standard Java 17. Use supported bounded executors in the Java 17 baseline; virtual threads only in an explicitly upgraded optional toolchain.
2. **Foreign Function & Memory:** java.lang.foreign Arena, Linker, SymbolLookup shown in Engine base is from newer JDK releases (the stable API is Java 22); it is not available unmodified on Java 17. Do not replace working LWJGL/JNI/native access blindly. Check Java 17 incubator compatibility separately, with optional backend, no raw pointers exposed to scripts and no use-after-close native segment.
3. **Pattern matching:** record-deconstruction switch examples use language features newer than Java 17; ordinary Java 17 sealed interfaces, records and explicit instanceof dispatch must compile without preview flags.
4. **Security:** a sealed script interface, classloader, named token or JVM exception is not a secure sandbox by itself. Process isolation, least-privilege OS permissions, cancellation, input validation and versioned dependencies are mandatory for untrusted scripts.
5. **Audio code:** the HRTF example constructs attributes without applying/resetting a context; the high-pass example is a no-op and gain sketches are not production acoustic models. Implement real device negotiation and DSP and verify output in tests. OpenAL EFX is an optional native dependency, not an editor-wide requirement.
6. **Topology/auto-rig:** remeshing or automatically smoothing mesh islands can destroy UV seams, normals, silhouette and hard-surface rig semantics. Preserve originals; apply only after previewed author-approved transformation.
7. **Genre APIs:** a registry, graph or generic formula does not by itself constitute a playable game. Each plugin must demonstrate real game loop, persistence and (where needed) multiplayer authority.
8. **Source figures:** GitHub transcriptions preserve all extracted text but not PDF figures or exact page layout. The PDF SHA-256 provenance remains in the register; originals should be preserved separately and their binary repository archival is still open.

## CI / release-check discipline

- **Release blockers:** compile/package/launch failures; corrupt source or broken save/reopen; required runtime collision/AI/navigation behavior that prevents gameplay; required multiplayer/update failures. Do not disable failed workflows to obtain green.
- **Diagnostics, not blockers:** subjective artwork fidelity, LOD cosmetic seams that do not objectively break required functionality, audio taste, stylized PSX look, brush ergonomics, balancing and visual captures.
- Test each stage by editing its **actual modules**, not a fake parallel system. Record exact commit SHA, CI run ID, OS, generated files/hashes, and remaining gates in the normal recovery document.
- Sequence may overlap work on independent modules, but a later stage cannot mark accepted before the foundational data contract and its own end-to-end acceptance.



---

## Part III — Complete available text from the four newly supplied original PDFs

The four original uploads are authoritative for images, layout, formula typography and illustrations. The full text from the repository text archives is reproduced here, including each original page marker and source title; these are full *available text transcriptions*, not summarized recommendations.


---

### PDF source transcription: Topological editor.pdf

**Versioned repository extraction:** `docs/editor/requirements/sources/topological-editor.txt`.

# PDF transcription archive: Topological editor.pdf
# Original PDF remains the source of truth for figures, formatting and pagination.
# Extracted text below preserves source page headers but may omit graphical content.

<PARSED TEXT FOR PAGE: 1 / 36>
When building a custom topological mesh generator and low-poly/primitive sculpting
toolset—especially one designed to run locally, efficiently, and natively—your data
structures and tool paradigms must prioritize topological predictability, edge-flow
retention, and performance optimization.

Here is the comprehensive breakdown of the must-haves, should-haves, wish list
features, and critical engineering pitfalls to avoid for a standalone primitive modeling
and sculpting module.




🧱 Core Architecture & Must-Haves
The absolute foundational layer of a geometric mesh manipulation engine.

   ●​ Manifold Mesh Data Structure: Use a Half-Edge or Winged-Edge data
      structure. Avoid simple triangle-soup formats (like raw vertex/index buffers) for
      your internal topology representation. Sculpting, extruding, and boundary
      traversal require instant access to adjacent faces, edges, and vertices (O(1)
      lookup).
   ●​ Non-Destructive Primitive Generation: Parametric control over initial shapes
      (e.g., box, sphere, cylinder, torus). Vertices should not be fixed until the user
      "commits" or performs a destructive edit. You must expose explicit subdivision
      counts (X, Y, Z steps) at generation time to keep low-poly limits tight.
   ●​ Strict Quad-Dominant Remesher: Primitive manipulation and sculpting quickly
      distort geometry. A baseline uniform quad-remesher (using a simple voxel-grid
      distance field or cross-field approach) is mandatory to clean topology and reset
      vertex distribution.
   ●​ Essential Transformation Matrix Tools: Absolute and relative snapping
      systems. Must include vertex-snapping, grid-snapping, and angle-snapping (15°,
      45°, 90°).
   ●​ Local and Normal Transform Gizmos: Every extrusion or deformation tool
      must support switching between Global, Local, and Component-Normal
      coordinate systems instantly.




🎨 Sculpting & Primitive Modeling Techniques
Must-Haves (The Core Toolset)
<PARSED TEXT FOR PAGE: 2 / 36>
   ●​ Extrude, Inset, and Bevel: The holy trinity of box modeling. Extrusion must
      support both individual face normals and averaged group normals.
   ●​ Standard / Clay Brush: Moves vertices along their normals based on a
      spherical radius and a customizable falloff curve (e.g., linear, smooth, sharp).
   ●​ Grab / Move Brush: Deforms geometry parallel to the screen plane or along a
      locked axis using screen-space delta movement, utilizing a radius-based
      distance falloff.
   ●​ Smooth Brush: A Laplacian smoothing filter applied to vertex positions. It
      calculates the average position of adjacent vertices and shifts the target vertex
      toward it without altering topology.
   ●​ Dynamic Topological Symmetry: Real-time reflection across a chosen local or
      global axis (X, Y, or Z). Edits on one side must mirror instantly by mapping
      corresponding vertex indices or spatial coordinates.

Should-Haves (High-Priority Quality of Life)

   ●​ Edge Loop Split & Ring Select: Quick insertion of perpendicular edge loops
      across a quad strip. Selection tools must include "Select Loop" (Alt + Click) and
      "Select Ring".
   ●​ Flatten / Planar Brush: Samples the average normal of all vertices within the
      brush radius upon click, creates a virtual plane, and snaps or projects all
      manipulated vertices onto that plane.
   ●​ Flatten / Plateau Terrain Tools: If adapting this engine for terrain or
      environments, simulation-based flattening, plateauing, and directional erosion
      stamps are highly recommended to balance manual sculpting with procedural
      shapes.

Wish List (Advanced Features)

   ●​ Dynamic Topology (Splatting/Dynamic Tessellation): Dynamically dividing or
      collapsing edges on the fly only beneath the brush stroke based on screen-space
      density.
   ●​ Bridge & Weld: Selecting two open border loops or two separate vertices and
      bridging them with new faces or welding them together based on a distance
      threshold.




⚠️ Pitfalls to Avoid (The Anti-Patterns)
   ●​ Pitfall: Naive Density Averaging
<PARSED TEXT FOR PAGE: 3 / 36>
          ○​ The Flaw: Relying on simplistic spatial averaging for density calls or vertex
              distributions during remeshing or generation.
          ○​ The Result: This causes a distinct "chunky" or blocky appearance in your
              terrain and meshes. Use a smooth interpolation scheme (like Hermite or
              cubic interpolation) instead of raw step averaging to maintain crisp
              topological flow.
   ●​ Pitfall: Allowing Non-Manifold Geometry
          ○​ The Flaw: Allowing operations that create internal faces, T-junctions
              (where a vertex sits on an edge of another face without splitting it), or
              edges shared by more than two faces.
          ○​ The Result: Immediate crashes or erratic behavior in your remeshers,
              subdivision algorithms, and smoothing filters. Your editing functions must
              explicitly reject or automatically fix non-manifold generation.
   ●​ Pitfall: Disregarding Vertex Normal Recalculation
          ○​ The Flaw: Deforming, sculpting, or extruding geometry without
              continuously marking face and vertex normals as dirty.
          ○​ The Result: Broken lighting calculations, blocky/faceted artifacting during
              smooth shading, and incorrect brush projections. Normals must update
              instantly within the brush radius during an active stroke.
   ●​ Pitfall: Screen-Space Scale Invariance
          ○​ The Flaw: Hardcoding brush radii to a fixed world-space metric without
              scaling dynamically based on camera distance or zoom factor.
          ○​ The Result: Zooming out makes the brush massive and destructive;
              zooming in makes it microscopic and useless. Implement screen-space
              radius calculations as an alternative mode.
   ●​ Pitfall: Monolithic Undo/Redo Buffers
          ○​ The Flaw: Saving a complete copy of the entire mesh data structure into
              memory for every single undo step.
          ○​ The Result: Massive RAM consumption and performance hitching on
              larger meshes. Use a Command Pattern that stores only the specific
              topological changes or delta vertex transformations per stroke/action.



To elevate a custom 3D primitive modeling and sculpting tool from a basic geometry
manipulator into a fluid, responsive development suite, you must focus heavily on
interaction ergonomics and deterministic spatial feedback. When working locally
on custom engines, minor friction in selection, viewport navigation, or feedback loops
will completely destroy asset authoring workflows.

Here is the continuation of quality-of-life (QoL) must-haves, essential feedback
mechanisms, and UI/UX paradigms necessary for a native mesh generator.
<PARSED TEXT FOR PAGE: 4 / 36>
🔍 Viewport & Interaction QoL Essentials
How the user interacts with, perceives, and navigates around the raw topological data.

   ●​ Focal-Point Centered Navigation: Viewport rotation and zooming must anchor
      dynamically to the current mouse cursor location or the active selection bounding
      box, rather than a hardcoded world origin (0,0,0).
   ●​ Instant Wireframe Overlay Toggle: A dedicated global hotkey (e.g., Z or
      Shift+Z) that instantly toggles a clean, low-overhead wireframe drawing on top
      of shaded geometry. This is mandatory for low-poly mesh editing to evaluate
      edge-flow, quad distribution, and density artifacts on the fly.
   ●​ Screen-Space Drag-Selecting (Marquee / Lasso): Support for box-selecting
      multiple vertices, edges, or faces simultaneously.
   ●​ Backface Culling Exclusion Toggle: The selection engine must support a
      toggle between Visible Only (occluded elements are ignored) and Through
      Selection (X-Ray mode, selecting everything within the marquee screen
      projection bounds, including backfaces).
   ●​ Dynamic Visual Snapping Guides: When translation snapping is active, render
      lightweight visual lines or a localized bounding grid showing exactly which vertex,
      edge, or grid coordinate the component is currently locking onto. Do not leave
      the user guessing if a vertex successfully welded or aligned.




🛠️ Geometric Editing & Cleanup Essentials
   ●​ Automatic Degenerate Geometry Pruning: Real-time background cleanup of
      invalid geometry. If an extrusion or transformation scales a face down to an area
      of zero, or collapses two distinct vertices to the exact same spatial coordinate,
      the engine must automatically merge them (Vertex Welding) and delete the
      zero-area faces to keep the mesh clean.
   ●​ Edge & Vertex Slide: A transformation mode that allows a user to move a
      selected vertex or an entire edge loop strictly along its connected edges without
      altering the overall silhouette or underlying surface volume of the model.
   ●​ Active Component Highlighting (Pre-selection): Before clicking to select a
      vertex, edge, or face, the component directly beneath the cursor must highlight or
      change color slightly. This provides instant visual confirmation of target accuracy,
      completely eliminating accidental misclicks in tight, dense geometric regions.
<PARSED TEXT FOR PAGE: 5 / 36>
  ●​ Pivot Point Overrides: The ability to temporarily unlock the transformation
     gizmo from the selection center and manually place it somewhere else (e.g.,
     snapping it to a specific corner vertex of a primitive). This allows custom
     rotational origins and precise orientation alignments during block-outs.




📊 Visual Feedback & Performance Essentials
  ●​ Real-Time Mesh Topology Counters: A lightweight, non-intrusive heads-up
     display (HUD) in the corner of the viewport tracking live counts for Vertices,
     Edges, Faces (Quads/Tris), and total mesh islands. In low-poly asset or
     retro-era emulation development, keeping explicit track of budgets is a core
     priority.
  ●​ Visual Normals Debugger: A toggleable rendering mode that draws short lines
     extending outward from vertex positions or face centers along their computed
     normal vectors. This makes it instantly obvious if a face normal is flipped inward,
     which causes backface rendering holes and broken shading behavior.
  ●​ Local Performance Safety-Locks: When scaling up procedural generation
     parameters (like changing a primitive cube subdivision slider from 4 to 128),
     implement a hard warning wall or an asynchronous task handler. Never block the
     main graphics or UI thread with deep topological evaluations, or the application
     will freeze.




⚠️ Pitfalls to Avoid in Interaction Systems
  ●​ Pitfall: Raycasting Selection Inaccuracy
         ○​ The Flaw: Using poor bounding boxes or pure center-point proximity
             checks to determine which polygon the mouse is hovering over.
         ○​ The Result: The user tries to click a small edge or a narrow polygon face,
             but the selection system selects a massive object directly behind it. Use
             accurate triangle-level screen-space raycasting or color-buffer indexing for
             pixel-perfect component picking.
  ●​ Pitfall: Input Latency on Dense Mesh Modification
         ○​ The Flaw: Rebuilding the entire GPU vertex, index, and color buffer from
             scratch across the whole mesh on every single frame during a brush
             stroke or drag-extrude.
         ○​ The Result: Massive frame-rate drops and stuttering when editing. Use a
             dirty-flag system or regional sub-buffer updates to modify only the specific
<PARSED TEXT FOR PAGE: 6 / 36>
             spatial chunks or vertex arrays that the brush or transform tool is actively
             touching.



When building a native, high-performance 3D modeling and sculpting suite—especially
one designed to run completely locally, smoothly, and without heavy external
dependencies—your file format ecosystem needs to balance

two completely distinct requirements:

   1.​ An Internal Native Format: Optimized for lightning-fast memory serialization, full
       topological retention (Half-Edge data), tracking structural relationships, editing
       states, and keeping disk footprint minimal.
   2.​ An External Interchange Format: Optimized for exporting clean, standard
       assets to game engines (like Unreal Engine) or other third-party tooling, without
       dragging massive or complex library dependencies into your codebase.




💾 1. The Internal Native Format: Bespoke Binary (.MESH / .PRJ)
For native drawing and fast file operations, do not use text-based formats (like XML,
JSON, or OBJ) for saving project files. Parsing strings into floats is one of the biggest
bottlenecks in Java-based file operations and will cause noticeable hitching as mesh
density grows.

The Implementation Blueprint

Create a lightweight, custom binary format using standard Java DataOutputStream
and DataInputStream (or NIO ByteBuffer mapped directly to files for zero-copy
memory reads).

  None‎

  [HEADER] -> Magic Bytes (e.g., "MY3D") + Format Version (Int)



  [METADATA] -> Active tool settings, camera position matrices, history
  state indices
<PARSED TEXT FOR PAGE: 7 / 36>
 [VERTEX SECTION] -> Total Count (Int) -> Loop: [X, Y, Z (Floats)] +
 [U, V (Floats)]



 [HALF-EDGE TOPOLOGY]



      -> Total Face Count (Int) -> Loop: [Vertex Index array per face]



      -> Structural Adjacency Maps (Int arrays mapping Twin, Next, Prev
 pointers)



 [SCULPT/MATERIAL DATA] -> Vertex layers, weights, or material index
 assignments (Bytes/Shorts)




Why it's a Must-Have:

   ●​ Instant Load/Save: You are writing raw bytes straight from memory arrays onto
      the disk. Reading a file means copying sequential chunks directly into your
      primitive arrays (float[] and int[]), bypassing object instantiation overhead.
   ●​ Preserves Structural Truth: Standard export formats only understand triangles
      or basic polygons. Your native format can store the exact state of your Half-Edge
      structural graphs, vertex symmetry pairings, or edge loop markers without
      needing to recalculate them every time a file is opened.




🔄 2. External Interchange Formats: The Best Options
When exporting to external environments, you need formats that play nice with game
loops and standard engines. Here are the three best formats ranked by implementation
complexity and suitability.
<PARSED TEXT FOR PAGE: 8 / 36>
  Format         Native 3D Draw      Implementation Complexity         Best Used For
                  Performance                in Java


 glTF 2.0     🚀 Highest              Moderate (JSON + Binary      Game Loop Engines, Web
 (.glb)       (GPU-Ready)            Chunks)                      & Runtime


 OBJ (.obj)   ⚠️ Slow (String        🟢 Extremely Easy (Plain      Basic geometry exchange,
              Parsing)               Text)                        legacy test


 FBX (.fbx)   🚀 High (Optimized) 🔴 Extremely Complex /            Complex rigs, animation
                                     Massive Libs                 (Overkill here)



🥇 Top Pick: glTF 2.0 Binary Variant (    .glb)

The GL Transmission Format is the modern standard for 3D asset delivery. The .glb
variant wraps a small JSON text description and a massive raw binary data block into a
single file.

   ●​ Why it's perfect for Native Rendering: The binary buffers inside a .glb file are
      structured to align exactly with how modern graphics cards expect data. When
      you read the asset, you can pass the extracted byte arrays straight into your
      vertex buffer objects (VBOs) with minimal processing.
   ●​ Implementation Strategy: You do not need massive external SDKs. Write a
      simple encoder that constructs the small JSON header (defining attributes like
      vertex positions, normals, and texture coordinates) and appends the raw
      primitive float and int arrays immediately after it.

🥈 Backup Option: Wavefront        .obj (Import Only)

The .obj format is a simple, human-readable text file. While it is incredibly easy to write
a parser for it, it should strictly be used as a legacy import capability.

   ●​ The Flaw: Reading massive text lines like v 1.043 -0.232 5.112 requires
      thousands of string splitting and float parsing operations. It is exceptionally slow
      for complex geometry and does not support modern material layers natively.
   ●​ The Rule: Include a clean .obj importer/exporter for raw vertex compatibility, but
      never use it for native, active workspace operations.
<PARSED TEXT FOR PAGE: 9 / 36>
⚙️ Architectural Essentials for File Operations
   ●​ Decouple Topology from Render Buffers: When importing any format (like a
      glTF or OBJ), your import pipeline must have two phases:
         1.​ Parse the file into raw vertex/index lists.
         2.​ Pass those lists into a topological constructor that builds your internal
             Half-Edge network (resolving twins, boundaries, and vertex valences).
             Never try to sculpt directly on an index buffer read from an external file.
   ●​ Asynchronous I/O via Streaming: Implement file saving and exporting as
      background worker threads. Large mesh data transfers to disk should update a
      visual progress bar in the viewport HUD without locking your application's input
      processing or main rendering loops.
   ●​ Deterministic Vertex Merging on Import: External formats often duplicate
      vertices along texture seams or sharp edges to satisfy GPU requirements. Your
      importer must include a tolerance-based Vertex Welding step (e.g., merging any
      vertices within 0.0001 units of each other) to restore an integrated, clean
      manifold mesh for sculpting.

Integrating precise digitizer tablet support (Wacom) and motion capture data
capture/integration directly into a custom local 3D sculpting suite requires deep,
low-level integration with the OS input processing layer.

Standard mouse inputs sample at low rates and miss physical dimensions like pressure
and tilt. For an organic sculpting feel, you must capture high-frequency tablet events
and handle incoming motion capture coordinates asynchronously without hitching your
rendering loop.




🖋️ 1. Wacom & Pen Tablet Integration
To support pen pressure, tilt, and high-frequency hover data natively, you must bypass
standard high-level mouse events (which clamp tracking data to integer screen
coordinates and drop pressure data).

The Technical Requirement

Modern digitizer tablets use API protocols like Wintab (Windows), Windows Ink /
DirectManipulation, or NSView tablet events (macOS). Because you are building in
<PARSED TEXT FOR PAGE: 10 / 36>
Java, you should utilize a native access binding library like JPen or interface directly via
JNI/LWJGL/GLFW native callbacks.

Core Parameters to Extract & Map:

   1.​ Normalized Pressure (float 0.0 to 1.0): Must map dynamically to your brush
       intensity or deformation depth.
   2.​ Pen Tilt (float X/Y Angles): Maps to brush orientation or directional flow.
       Essential for asymmetric sculptors' tools (e.g., chisel brushes).
   3.​ High-Frequency Sub-Pixel Coordinates: Tablets sample at 125Hz to 200Hz+,
       much faster than screens refresh. You must grab sub-pixel floating-point
       locations (double x, double y) rather than integer window pixels, or your brush
       strokes will look jagged when drawing fast.

Implementation Architecture:

java
  None‎


  public class TabletInputHandler {



          private float currentPressure = 1.0f;



          private float tiltX = 0.0f;



          private float tiltY = 0.0f;




          // Triggered by your native OS input loop callback (e.g.,
  WinTab/GLFW window events)
<PARSED TEXT FOR PAGE: 11 / 36>
         public void handleTabletEvent(double x, double y, float pressure,
  float tx, float ty) {



              this.currentPressure = pressure <= 0.0f ? 1.0f : pressure; //
  Fallback to 1 for mouse



              this.tiltX = tx;



              this.tiltY = ty;




              // Process a sub-pixel brush stroke point



              SculptEngine.getInstance().addStrokePoint(x, y,
  currentPressure, tiltX, tiltY);



         }



  }




Use code with caution.

      ●​ The Brush Equation: When calculating vertex displacement within your active
         brush radius, modify your distance falloff formula by the active pressure value:​
         𝐷𝑖𝑠𝑝𝑙𝑎𝑐𝑒𝑚𝑒𝑛𝑡 = 𝑁𝑜𝑟𝑚𝑎𝑙 × 𝐵𝑎𝑠𝑒𝐼𝑛𝑡𝑒𝑛𝑠𝑖𝑡𝑦 × 𝐶𝑢𝑟𝑟𝑒𝑛𝑡𝑃𝑟𝑒𝑠𝑠𝑢𝑟𝑒 × 𝐹𝑎𝑙𝑙𝑜𝑓𝑓(𝑑𝑖𝑠𝑡)
<PARSED TEXT FOR PAGE: 12 / 36>
🦿 2. Motion Capture Integration
Integrating motion capture (mocap) into a sculpting suite generally serves two distinct
workflows: using tracked hardware as a real-world spatial 3D brush (or camera
controller), or importing skeletal data streams to deform and pose organic meshes
during an edit session.

The Protocol Standards: VRPN & OpenVR

For real-time streaming from tracking hardware (like an OptiTrack system, an HTC Vive
tracker, or specialized hand trackers), do not write bespoke drivers. Rely on standard
protocols:

   ●​ VRPN (Virtual Reality Peripheral Network): A lightweight, open-source network
      protocol that broadcasts low-latency spatial positions (X, Y, Z) and rotations
      (Quaternion) over UDP.
   ●​ OpenVR / SteamVR API: If using VR hardware as a spatial sculpting input tool.

Asynchronous Threading Network Architecture (Mandatory)

Mocap hardware typically streams updates via UDP at rates between 120Hz and
240Hz. If you handle network packet parsing on your main graphics or UI thread, your
application frame rate will tank.

You must implement a Producer-Consumer ring buffer pattern:

   1.​ Network Thread (Producer): Runs an isolated network loop listening for UDP
       tracking packets. It unpacks the raw tracking coordinates and pushes them
       directly into a thread-safe concurrent queue.
   2.​ Render/Logic Thread (Consumer): At the beginning of each frame, the main
       loop drains the queue, averages or interpolates the spatial coordinates to match
       the exact delta-time (dt), and transforms the mesh or camera.

java
  None‎


  import java.util.concurrent.ConcurrentLinkedQueue;
<PARSED TEXT FOR PAGE: 13 / 36>
public class MocapStreamReceiver implements Runnable {



    private final ConcurrentLinkedQueue<MocapFrame> frameQueue = new
ConcurrentLinkedQueue<>();



    private boolean running = true;




    @Override



    public void run() {



        // Run on dedicated thread



        while (running) {



            MocapFrame packet = readIncomingUDPPacket(); // Blocking
network call



            if (packet != null) {



                frameQueue.offer(packet);



                // Keep the buffer size under control to minimize
input lag
<PARSED TEXT FOR PAGE: 14 / 36>
                         if (frameQueue.size() > 5) {



                             frameQueue.poll(); // Drop oldest frame if backing
  up



                         }



                    }



              }



        }




        public MocapFrame getLatestFrame() {



              return frameQueue.poll();



        }



  }




Use code with caution.
<PARSED TEXT FOR PAGE: 15 / 36>
⚠️ Critical Integration Pitfalls to Avoid
   ●​ Pitfall: Lack of Stroke Interpolation (The "Dotted Line" Effect)
          ○​ The Flaw: Only executing your sculpting/deformation logic exactly when
              an OS event fires.
          ○​ The Result: If your frame rate drops or the user sweeps the stylus across
              the tablet at high speeds, your brush leaves isolated circular clumps
              instead of a fluid, solid stroke.
          ○​ The Fix: Calculate the vector distance between the previous stroke point
              and the current point. If it exceeds a tiny percentage of the brush radius,
              procedurally step and interpolate additional sample points linearly across
              the path.
   ●​ Pitfall: Jitter and Latency on Mocap Data
          ○​ The Flaw: Directly applying raw spatial data from mocap hardware straight
              to your mesh vertices. High-frequency positional tracking naturally carries
              subtle millimeter-scale noise and data jitter.
          ○​ The Result: The mesh appears to vibrates rapidly while sculpting, creating
              wrinkled or artifact-heavy topology surfaces.
          ○​ The Fix: Run a rolling Exponential Moving Average (EMA) filter or a
              basic Kalman filter over incoming position vectors to smooth out tracking
              noise before projecting the transform matrix onto your topological model.



When engineering a professional-grade 3D modeling and sculpting suite, your viewport
layout paradigms and import/export pipelines must mirror the ergonomics and
structural compliance of industry-standard tools (like Blender, Maya, and Unreal
Engine).

Because your architecture targets a local, highly deterministic workflow, navigating and
extracting spatial data must remain predictable, uniform, and tightly aligned with global
industry specifications.




🎛️ 1. Essential Viewport Layout Tools
A production-ready viewport must provide instant spatial orientation and split workflows
without degrading rendering performance.

The Standard Multi-View Split (Quad-View Architecture)
<PARSED TEXT FOR PAGE: 16 / 36>
The editor must support a customizable panel layout system, with the industry standard
being a toggleable 4-Pane Grid:

   1.​ Perspective View: 3D camera projection utilizing adjustable Field of View (FOV)
       sliders.
   2.​ Top View: Orthographic projection looking down the Y or Z axis.
   3.​ Front View: Orthographic projection looking down the X or Y axis.
   4.​ Side View: Orthographic projection looking down the remaining lateral axis.

Core Viewport Features:

   ●​ The Axis Orientation Gizmo (Navigation Widget): A highly interactive, 3D
      coordinate cross rendered in the corner of every panel. Clicking on an axis
      handle (X, Y, Z) must instantly transition that specific viewport panel into a
      perfectly aligned orthographic projection mode facing that direction.
   ●​ Frustum-Aware Screen Grids: Orthographic viewports require a dynamic,
      infinity-scaled floor grid. As the user zooms in or out, the grid must procedurally
      subdivide or collapse its spacing markers (e.g., transitioning smoothly from
      10-unit blocks to 1-unit blocks) to maintain relative spatial scale references.
   ●​ Per-Panel Render Mode Triggers: Viewport panels must operate independently.
      A user needs to be able to set their Perspective panel to full smooth shading
      while keeping their Side and Top orthographic panels in clean, low-overhead
      wireframe mode.




📐 2. Coordinate System Alignment & Industry Standards
One of the largest friction points in 3D tool ecosystems is the Coordinate System
Conflict. If your backend architecture does not account for this natively, imported
models will arrive flipped on their backs or mirrored backwards.

When building your conversion pipelines, map your transformation vectors explicitly to
the target engine requirements:

  System / Engine      Up Axis            Handedness             Common File Format


 Your Engine         Y-Up or      Right-Handed (Standard       .glb (glTF) / Bespoke
 Default             Z-Up         Math)                        Binary
<PARSED TEXT FOR PAGE: 17 / 36>
 Unreal Engine 5     Z-Up          Left-Handed                 .fbx / .gltf


 Unity               Y-Up          Left-Handed                 .fbx


 Blender             Z-Up          Right-Handed                .blend / .obj / .glb


 Maya / glTF Spec    Y-Up          Right-Handed                .glb / .fbx



The Conversion Matrix Solution:

Do not force your internal topological data structure to change its mathematical basis.
Instead, implement a Coordinate Transformer Module on your import and export
boundaries.

When exporting a mesh to a destination engine, apply a dedicated conversion transform
pass to all vertex arrays:

   ●​ Y-Up to Z-Up Conversion: Swap the Y and Z values of your position and normal
      vectors, then invert the new Z axis coordinate to preserve relative winding paths.
   ●​ Handedness Flips (Right to Left): Multiply the X-axis coordinate by -1.
      Crucially, you must also reverse the vertex winding order of your geometric
      polygons (e.g., swapping index 1 and index 2 of a face triplet), or your polygon
      faces will point inside-out, causing them to render completely invisible due to
      backface culling.




📦 3. Strict Compatibility Requirements for Import/Export
For a custom modeling engine to successfully communicate with modern production
tools, it must enforce rigid data structures during serialization.

Position and Element Alignment

   ●​ Interleaved Vertex Buffers: When building glTF or binary export arrays,
      interleave your primary vertex component arrays [Position.XYZ, Normal.XYZ,
      UV.UV] into a single contiguous byte array. Modern graphics systems ingest
<PARSED TEXT FOR PAGE: 18 / 36>
      sequential, tightly packed memory blocks vastly faster than pulling data from
      isolated arrays.
   ●​ Explicit Normal Calculations: Never export a mesh hoping the target engine
      will figure out the surface angles. You must explicitly calculate and write out the
      vertex and face normal data. For low-poly models, support split vertex normals to
      preserve hard, sharp box edges alongside smooth, organic surfaces.

Material and Data Layer Masking

   ●​ Splat Maps & Virtual Texturing Alignment: If your tool authors terrain or dense
      vertex surfaces, export data arrays using standardized channels. For material
      blending, pack terrain weighting percentages into raw, multi-channel image maps
      (e.g., Channel R = Material 1, Channel G = Material 2, etc.) or embed them
      directly into the vertex attributes as secondary vertex color groups
      (Color0.RGBA).
   ●​ UV Coordinate Inversion: The global industry standard for texture coordinates
      places the origin (0,0) at the bottom-left corner. However, some graphics
      systems read the vertical texture coordinate (V or T axis) from the top-down. Your
      export pipeline must include a quick checkbox to invert the V coordinate (V =
      1.0 - V) to instantly fix upside-down textures in third-party software.

Prepping a character or object for animation—traditionally called rigging—is notorious
for being one of the most tedious stages in 3D production. When designing a native
rigging and animation-prep module, maximizing Ease of Use and Quality of Life (QoL)
means minimizing manual weight-painting and point-clicking.

Your implementation must automate structural bone discovery, offer fluid deformation
tools, and strictly adhere to industry skeletal standards to allow seamless
cross-migration with engines like Unreal Engine 5 or Blender.




🦴 1. Bone Creation & Automatic Skeleton Detection
Manual bone placement can be tedious. A modern engine needs an elegant system for
constructing skeletons both manually and procedurally.

Automatic Bone Detection (Auto-Rigging)
<PARSED TEXT FOR PAGE: 19 / 36>
For bipedal or quadrupedal meshes, implement a geometric Symmetric Distance Field
(SDF) or a Voxelized Center-of-Mass algorithm to eliminate manual bone creation:

   1.​ Mesh Voxelization: Convert the low-poly mesh topology into a low-resolution,
       watertight solid voxel grid.
   2.​ Medial Axis Extraction: Execute a thinning algorithm on the voxel grid to extract
       the "skeleton" or central pathways of the mesh volume.
   3.​ Joint Placement Hooks: Automatically place joint entities at critical junctions
       where the extracted paths branch (e.g., hips, shoulders, knees, elbows).
   4.​ User-Assisted Anchor Mode: If pure automatic detection fails on complex
       models, give the user a 5-Point Placement Guide. Have them manually place
       exactly five markers (Head, Left Hand, Right Hand, Left Foot, Right Foot), and
       use those anchors to map a pre-built bipedal skeletal template inside the mesh
       bounds.

Manual Bone Arrangement & Modification QoL

   ●​ Joint Snap-to-Center: When a user is manually drawing a bone chain, the joints
      must automatically calculate the local geometric volume of the surrounding mesh
      chunk and snap directly to its center of mass. This prevents bones from sitting
      incorrectly near the surface skin.
   ●​ Bone Hierarchical Parenting Rules: Deleting a parent bone must never delete
      its children. Instead, automatically re-parent the child bones up one level to the
      deleted bone's parent.
   ●​ Mirroring Skeletal Chains: Building an arm or leg chain must instantly mirror
      across a designated local plane with smart naming conventions (e.g.,
      auto-converting Bone_L_UpperArm to Bone_R_UpperArm).




🎨 2. Bone Weighting & Skinning Ergonomics
Skinning defines how much a bone's transformation deforms specific regions of the
mesh topology.

Automated Weighting (The Baseline Essentials)

   ●​ Automatic Heat Diffusion Weighting: This is the gold standard for animation
      prep. It treats bones as heat sources inside a solid mesh container and allows
      the heat to diffuse out through the vertex graph. Vertices closer to a bone receive
<PARSED TEXT FOR PAGE: 20 / 36>
      higher weights, resulting in smooth, organic falloff zones instantly without manual
      painting.
   ●​ Bone Capsular Envelopes: A lightweight fallback mode where every bone
      generates an adjustable 3D cylinder or capsule around itself. Any vertices falling
      inside the capsule are explicitly assigned to that bone. This is incredibly efficient
      for hard-surface models, mechanical assemblies, or ultra-low-poly retro assets.

Weight Modification QoL

   ●​ Strict Vertex Influence Limits: Most modern game loops and vertex shaders
      enforce a hard limit of 4 influences per vertex (Max 4 bones can pull on a single
      vertex). Your weighting engine must automatically prune and normalize skin
      weights, keeping only the top 4 highest values and adjusting them so they always
      sum perfectly to exactly 1.0.
   ●​ Lock Weights Toggle: Allow users to lock specific bones or vertex groups.
      When smoothing weights across a shoulder joint, locked vertex weights are
      entirely protected from accidental modification.




🔄 3. Compatibility & Cross-Migration (Import/Export)
To guarantee that your authored rigs deform identically when exported to external
packages, you must construct your skeletal hierarchies to mimic standard joint matrices.

Skeletal Layout Requirements

   ●​ Root Bone Architecture: Every skeleton exported must inherit from a single,
      dedicated Root Bone at the absolute spatial origin (0,0,0). Never allow an
      engine rig to export with multiple independent baseline parents (like independent
      left and right hip chains), or game engines will reject the asset.
   ●​ Uniform Binding Matrices (Bind Pose): Store an immutable Inverse Bind
      Matrix array for every bone at the exact moment the mesh skin is bound. This
      matrix records the resting world transform of the bone, allowing vertex shaders in
      tools like Unity or Unreal to calculate real-time rotational deltas without shearing
      or collapsing the geometry.

Cross-Migration Data Formats
<PARSED TEXT FOR PAGE: 21 / 36>
When serializing animated meshes or skeleton assets for third-party workflows, format
properties to ensure compatibility with standard bone tools:

   ●​ glTF 2.0 Skins (.glb): Highly recommended for native Web/OpenGL rendering.
      glTF structures skeletons using explicit skins arrays that define joints lists,
      mapping your internal hierarchical nodes cleanly into a format readable by
      external engines out of the box.
   ●​ FBX (Import/Export): If interacting with legacy rigging tools, ensure your
      exporter accurately maps bone node attributes to the standard FBX cluster
      system. Ensure that your joint rotational channels are ordered deterministically
      (e.g., standard XYZ Euler or pure Quaternion) to completely avoid Gimbal Lock
      issues when the rig transitions between applications.

Implementing an integrated pipeline that seamlessly connects a proprietary
Auto-Remesher, Auto-Rigger, and Animation System requires a highly deterministic
data contract. Because your development ecosystem prioritizes native, local hardware
execution and lightweight architectures, you must avoid monolithic dependencies
and build highly decoupled modules that communicate via clean memory structures.

Below is the engineering blueprint for connecting a topological mesh engine to a custom
bone and animation execution layer.




⛓️ The Unified Local Pipeline Data Flow
To achieve an automated pipeline without lagging your main thread, data must
transform sequentially through specialized stages:

\(\text{[Raw\ Volumetric\ Sculpt]}\xrightarrow{\text{Quad\ Remesher}}\text{[Manifold\
Mesh]}\xrightarrow{\text{SDF\ Thinning}}\text{[Skeletal\ Joints]}\xrightarrow{\text{Heat\
Diffusion}}\text{[Skinned\ Model]}\xrightarrow{\text{Anim\ Engine}}\text{[GPU\ VBO\
Buffers]}\)




1. The Core Data Blueprint (The Contract)
<PARSED TEXT FOR PAGE: 22 / 36>
To make a mesh animation-ready, your internal file structure or runtime memory state
must bind the vertex structure to a skeletal joint hierarchy. Every vertex in your model
must transition from containing raw static coordinates to tracking dynamic skeletal
weights.

java
  None‎


  // Contiguous memory blocks for high-performance GPU transfers



  public class SkinnedVertexData {



          public float[] positions;           // [x, y, z,     x, y, z, ...]



          public float[] normals;             // [nx, ny, nz, ...]



          public float[] uvCoordinates;       // [u, v,    u, v, ...]




          // Rigging Layers (Max 4 bone influences per vertex)



          public byte[] boneIndices;          // [b0, b1, b2, b3] per vertex



          public float[] boneWeights;         // [w0, w1, w2, w3] per vertex
  (Normalized, Sum = 1.0)



  }
<PARSED TEXT FOR PAGE: 23 / 36>
Use code with caution.




2. Attaching Auto-Remeshing to the Auto-Rigger
An automatic rigging engine will break or produce catastrophic deformation artifacts if
forced to evaluate non-manifold topology, internal faces, or un-welded vertices.
Therefore, your auto-remesher acts as the mandatory gateway.

Step A: Topological Standardization

    1.​ Execute the Quad-Remesher: Before bone calculation begins, standard
        step-averaging density functions must be evaluated, cleaned, and updated with
        continuous interpolation schemes (like Hermite splines). This guarantees a
        uniform distribution of quad polygons across the model.
    2.​ Isolate Mesh Islands: The auto-rigger must read your internal Half-Edge data
        structure to confirm the model is a single integrated manifold shell. If
        independent mesh components exist (e.g., loose clothing or weapon assets), the
        engine needs to flag them to inherit weights from the closest primary body
        vertices rather than generating separate, floating bone chains.

Step B: Joint Extraction via Signed Distance Fields (SDF)

    1.​ Compute Mesh Bounds: Enclose the remeshed model in a local, discrete 3D
        voxel grid.
    2.​ Calculate Distance Fields: For every voxel center, compute its shortest
        distance to the nearest polygon face. Voxels deep inside the body limbs will
        evaluate with the highest positive distance values.
    3.​ Trace the Centerline: Connect the local maxima of your distance field. This
        extracts a clean spatial centerline (medial axis) directly through the neck, spine,
        arms, and legs.
    4.​ Instantiate Joint Nodes: Place a joint at every node where the centerline splits
        or changes direction sharply based on your targeted template profile (e.g., a
        bipedal skeleton).




3. Integrating the Proprietary Rig into Your Custom Animation System
<PARSED TEXT FOR PAGE: 24 / 36>
Once joints are placed and weights are automatically generated (using a capsule or
heat-diffusion falloff), the skeletal hierarchy must interface directly with your local
animation playback module.

The Transform Hierarchy

Every bone in your skeleton is treated as a transformation node. The local position and
rotation of a child joint depend entirely on its parent node.

java
  None‎


  public class JointNode {



          public int jointId;



          public String name;



          public JointNode parent;



          public List<JointNode> children;




          // Spatial Matrices



          public Matrix4f localBindTransform;          // Initial rest state
  relative to parent



          public Matrix4f inverseBindMatrix;           // Global inverse rest
  transform matrix
<PARSED TEXT FOR PAGE: 25 / 36>
        // Active Runtime State



        public Matrix4f animatedTransform = new Matrix4f(); // Calculated
  per animation frame



  }




Use code with caution.


The Playback Matrix Loop

During real-time animation playback, your execution engine must recursively walk the
joint tree from the absolute Root Bone (0,0,0) down to the tips of the extremities.

For every frame, calculate the global animated transform matrix (𝑀𝑔𝑙𝑜𝑏𝑎𝑙) for each joint:

𝑀𝑔𝑙𝑜𝑏𝑎𝑙 = 𝑀𝑝𝑎𝑟𝑒𝑛𝑡_𝑔𝑙𝑜𝑏𝑎𝑙 × 𝑀𝑙𝑜𝑐𝑎𝑙_𝑎𝑛𝑖𝑚𝑎𝑡𝑖𝑜𝑛

Then, calculate the final Skinning Matrix sent to your GPU vertex shaders:

𝑀𝑠𝑘𝑖𝑛𝑛𝑖𝑛𝑔 = 𝑀𝑔𝑙𝑜𝑏𝑎𝑙 × 𝐼𝑛𝑣𝑒𝑟𝑠𝑒𝐵𝑖𝑛𝑑𝑀𝑎𝑡𝑟𝑖𝑥

This skinning matrix effectively moves a vertex from its original resting character space
into the current actively deformed space dictated by the tracking animation data.




4. Maximizing External Cross-Migration with Preferred Suites
If you choose to export your model outputs to industry standard systems (such as
Blender or Unreal Engine 5) rather than playing them back inside a custom runtime
<PARSED TEXT FOR PAGE: 26 / 36>
engine, your exporter must strictly map your internal binary data layouts to standardized
glTF (.glb) formatting structures.

   ●​ Linearize the Joint Hierarchy: External suites cannot parse complex, bespoke
      tree classes efficiently. Your glTF exporter must flatten your skeletal node
      structure into a clean, flat sequential array of indices (Node_0 = Root, Node_1 =
      Pelvis, Node_2 = Spine).
   ●​ Write the Inverse Bind Matrix Buffer: Pack your calculated
      InverseBindMatrix values into a single contiguous binary block (float[]). Map
      this block explicitly to the inverseBindMatrices structural accessor within the
      glTF file schema.
   ●​ Winding Order Harmonization: If you are migrating assets out of a native
      Right-Handed Y-Up coordinate system into an engine like Unreal Engine 5
      (Left-Handed Z-Up), ensure your exporter swaps your spatial Y and Z axes and
      multiplies the final X coordinate values by -1. This guarantees your animations
      don't collapse or distort upon import.




To establish an end-to-end pipeline, your auto-rigging engine must hold pre-defined,
standardized structural templates in memory. When the auto-mesher finishes cleaning a
sculpt, it uses the extracted centerline (medial axis) or spatial boundary coordinates to
stretch and anchor one of these standard archetypes.

Here is the exact technical blueprint, node hierarchy, and rigging intent for the 6
essential primitive animation rigs required by standard setups and custom local game
engines.




👤 1. Human (Bipedal Rig)
Designed for character locomotion, combat, and organic deformation.

The Node Hierarchy

  None‎
  Root (0,0,0)
  └── Pelvis
<PARSED TEXT FOR PAGE: 27 / 36>
         ├── Spine_01
         │   └── Spine_02
         │      └── Neck
         │           └── Head
         │                ├── Clavicle_L -> UpperArm_L -> LowerArm_L ->
 Hand_L
         │                └── Clavicle_R -> UpperArm_R -> LowerArm_R ->
 Hand_R
         ├── Thigh_L -> Calf_L -> Foot_L -> Toe_L
         └── Thigh_R -> Calf_R -> Foot_R -> Toe_R


   ●​ Auto-Mesher Matching: Hips and shoulders act as primary center-of-mass
      branching targets.
   ●​ Rigging Constraint: The Foot joints must be aligned perfectly parallel to your
      infinity floor grid to prevent floor-clipping artifacting during Inverse Kinematics (IK)
      foot-snapping loops.




🐾 2. Quadruped (4-Legged Mammal/Creature)
Designed for wolves, horses, or monster assets.

The Node Hierarchy

 None‎
 Root (0,0,0)
 └── Pelvis (Rear Hip Center)
         ├── Tail_01 -> Tail_02 -> Tail_03
         └── Spine_01
             └── Spine_02
                └── Chest (Front Shoulder Center)
                     ├── Neck -> Head
                     ├── Clavicle_Front_L -> UpperLeg_Front_L ->
 LowerLeg_Front_L -> Paw_Front_L
                     └── Clavicle_Front_R -> UpperLeg_Front_R ->
 LowerLeg_Front_R -> Paw_Front_R
         ├── UpperLeg_Back_L -> LowerLeg_Back_L -> Paw_Back_L
<PARSED TEXT FOR PAGE: 28 / 36>
         └── UpperLeg_Back_R -> LowerLeg_Back_R -> Paw_Back_R


   ●​ Auto-Mesher Matching: Requires a horizontal medial axis path split into distinct
      front (Chest) and rear (Pelvis) cluster zones.
   ●​ Rigging Constraint: Quadruped back legs require an inverted hock joint
      orientation (digitigrade structure). Ensure your knee/elbow bend indicators are
      flipped relative to the front legs to allow natural procedural gait blending.




🕷️ 3. Arachnid (Multi-Legged Insect/Sci-Fi Drone)
Optimized for spiders, crabs, or mechanical multi-legged walkers.

The Node Hierarchy

 None‎
 Root (0,0,0)
 └── Abdomen (Center Hull)
         ├── Head_Chelicerae
         ├── Leg_01_L_Coxa -> Femur_01_L -> Tibia_01_L -> Tarsus_01_L
         ├── Leg_01_R_Coxa -> Femur_01_R -> Tibia_01_R -> Tarsus_01_R
         ├── Leg_02_L_Coxa -> Femur_02_L -> Tibia_02_L -> Tarsus_02_L
         ├── Leg_02_R_Coxa -> Femur_02_R -> Tibia_02_R -> Tarsus_02_R
         ├── [Repeat for Legs 03 and 04 Left and Right...]


   ●​ Auto-Mesher Matching: Radiates outward from a central solid bounding sphere.
      The engine samples rays at uniform angular steps (e.g., every 45 degrees) to
      find the leg protrusion shells.
   ●​ Rigging Constraint: Vertices must be weighted with a tight falloff. Overlapping
      influence between Leg 01 and Leg 02 will cause the mesh surfaces to
      unnaturally stretch and web when walking. Keep capsule envelopes narrow.




🏎️ 4. Vehicles (Wheeled / Tanks)
Designed for cars, trucks, or armored treads. Hard-surface weighting dictates that
elements do not deform organically; they scale and rotate rigidly.
<PARSED TEXT FOR PAGE: 29 / 36>
The Node Hierarchy

  None‎
  Root (0,0,0)
  └── Hull (Main Body Chassis)
          ├── Wheel_Front_L (Rotational Parent)
          │   └── Steering_Hub_Front_L (Y-Axis Turn Only)
          ├── Wheel_Front_R
          │   └── Steering_Hub_Front_R
          ├── Wheel_Rear_L
          ├── Wheel_Rear_R
          └── Turret_Main (Y-Axis Traverse)
              └── Barrel_Main (X-Axis Elevation / Recoil Axis)


   ●​ Auto-Mesher Matching: Separates isolated geometric coordinate clusters
      (disconnected wheels/turrets) from the main core mesh envelope.
   ●​ Rigging Constraint: 100% Rigid Weighting. Vertices inside a wheel cluster
      must have a weight of 1.0 assigned explicitly to that wheel bone and 0.0 to
      everything else. Do not smooth or blend weights across mechanical components.




✈️ 5. Aircraft (Fixed-Wing)
Optimized for airplanes, jets, and gliders.

The Node Hierarchy

  None‎
  Root (0,0,0)
  └── Fuselage (Main Center Body)
          ├── Engine_Propeller (Continuous Z-Axis Spin)
          ├── Flap_Wing_L (X-Axis Trailing Edge Pivot)
          ├── Flap_Wing_R
          ├── Rudder_Tail (Y-Axis Yaw Pivot)
          ├── Elevator_Tail_L (X-Axis Pitch Pivot)
          ├── Elevator_Tail_R
          ├── Gear_Landing_Front -> Wheel_Landing_Front
          └── Gear_Landing_Rear_L -> Wheel_Landing_Rear_L
<PARSED TEXT FOR PAGE: 30 / 36>
   ●​ Auto-Mesher Matching: Maps to trailing geometric edges and localized hinge
      points identified via flat bounding boxes.
   ●​ Rigging Constraint: Ensure the local joint transform orientation axes align
      perfectly with the control surface hinge lines (e.g., local X-axis pointing directly
      along the wing spar) so that setting a simple rotational float controls the flap
      deflection precisely.




🚁 6. Aircraft (Helicopter / Rotary Wing)
Optimized for choppers or multi-rotor vertical take-off assets.

The Node Hierarchy

  None‎
  Root (0,0,0)
  └── Fuselage_Main (Chassis cabin)
          ├── Rotor_Main_Hub (Continuous Y-Axis Spin)
          │   ├── Blade_01 (Local X-Axis Pitch Control/Flex)
          │   ├── Blade_02
          │   ├── Blade_03
          │   └── Blade_04
          ├── Rotor_Tail_Hub (Continuous X-Axis Spin)
          ├── Door_Cargo_L (Slide/Hinge Pivot)
          └── Landing_Skid_L / Landing_Skid_R


   ●​ Auto-Mesher Matching: Identifies top-vertical structures for the main mast hub
      and rear-most terminal axes for the tail assembly.
   ●​ Rigging Constraint: For realistic blade movement under high velocity, separate
      the spinning hub matrix from the minor local deflection vectors of individual
      blades. This lets your animation loop apply continuous rotation to the hub while
      blending vertical blade flex or pitch changes on top seamlessly.


🛸 7. Modern Hovering Drone (Multi-Rotor / Quadcopter Rig)
<PARSED TEXT FOR PAGE: 31 / 36>
Unlike legacy mechanical aircraft or organic characters, modern hovering drones
(quadcopters, hexacopters, and vectored-thrust sci-fi drones) operate on principles of
high-frequency independent propulsion, localized stabilization, and
gimbal-isolated sensor tracking.

When building a preset for this asset class, your rig must decouple the high-frequency
vibration and rotational spin of the thrusters from the smooth, stabilized tracking
interpolation of the payload cameras.

The Node Hierarchy

    None‎
    Root (0,0,0) [Anchored at the geometric Center of Mass]
    └── Frame_Main (The central chassis/hull)
            ├── Arm_01 -> Motor_Hub_01 -> Rotor_Blade_01 (Continuous Local
    Y-Axis Spin)
            ├── Arm_02 -> Motor_Hub_02 -> Rotor_Blade_02
            ├── Arm_03 -> Motor_Hub_03 -> Rotor_Blade_03
            ├── Arm_04 -> Motor_Hub_04 -> Rotor_Blade_04
            ├── Thruster_Vector_L (Optional: X-Axis Tilt Pivot for VTOL
    variants)
            ├── Thruster_Vector_R
            └── Mount_Gimbal_Y (Pan / Yaw Joint - 360° Continuous Local
    Y-Axis)
                └── Mount_Gimbal_X (Tilt / Pitch Joint - Local X-Axis)
                    └── Mount_Gimbal_Z (Roll Joint - Local Z-Axis
    stabilization)
                        └── Camera_Sensor_Payload (Target Anchor Point)




🤖 Auto-Mesher Detection Strategies
To automatically isolate and assign bones for a multi-rotor drone shape, your meshing
parser must run a radial symmetry and volumetric segmentation evaluation

:

     1.​ Chassis Clustering: The core solid volume is isolated via a bounding sphere
         calculation. This volume is completely weighted (1.0) to the Frame_Main bone.
<PARSED TEXT FOR PAGE: 32 / 36>
   2.​ Ray-Cast Arm Detection: The engine shoots outward horizontal rays from the
       center of mass at uniform angular steps (e.g., matching common profiles like an
       𝑋 configuration or an 𝐻 configuration). When a ray hits an elongated cylinder
       topology that terminates in a flat circular disk, it instantiates an Arm node ending
       in a Motor_Hub.
   3.​ Propeller Separation: Disconnected, flat planar geometry layers stacked directly
       on top of the Motor_Hub targets are automatically isolated and assigned to
       independent Rotor_Blade spin tracks with absolute rigid weighting.




🎬 Animation Preset and Simulation Mechanics
To make drone animations look authentic inside your proprietary engine runtime, your
animation processing loop should combine explicit structural animation keys with
procedural calculation passes:

   ●​ Propeller Angular Velocity Blending: Never animate rotor blades using
      traditional timeline keyframes. Instead, apply a continuous rotational delta
      variable to the local transformation matrix of the Rotor_Blade bones per frame:​
      θ𝑛𝑒𝑤 = θ𝑜𝑙𝑑 + (𝑅𝑜𝑡𝑜𝑟𝑅𝑃𝑀 × ∆𝑡)​
      When transitioning from a stationary land state to a hover state, smoothly blend
      this rotation speed upward to activate a visual motion-blur shader effect.
   ●​ Procedural Chassis Lean (Pitch/Roll Bank): Modern drones do not move
      laterally without tilting. Your animation runtime engine should intercept the
      Frame_Main matrix. When velocity is applied forward, procedurally inject a slight
      negative rotation on the local X-axis (Pitch down); when strafing sideways, inject
      a rotation on the local Z-axis (Roll into the turn) proportional to acceleration.
   ●​ The Look-At Camera Gimbal Loop: The Camera_Sensor_Payload requires its
      own isolated tracking script. At the end of your skeletal hierarchy matrix updates,
      evaluate a look-at target vector (e.g., tracking a player or an enemy coordinate).
      Back-calculate the necessary angles through the Mount_Gimbal_Y and
      Mount_Gimbal_X nodes recursively, applying an inverse transform to cancel out
      any rapid pitch/roll wobbles affecting the main drone frame.
   ●​ High-Frequency Micro-Jitter Filter: To simulate wind resistance and drone
      motor resonance, inject low-amplitude, high-frequency Perlin Noise
      transformations into the Frame_Main bone coordinates during active flight
      animations. This provides an authentic hovering immersion that completely
      removes the sterile look of static path movement.
<PARSED TEXT FOR PAGE: 33 / 36>
To create a frictionless "one-click" pipeline, your system must store hardcoded
canonical weights and procedural/keyframed animation templates directly inside
the rigging module. When the user hits Auto Mesh Apply, the system solves the spatial
scale, attaches the target skeleton archetype, runs the weight assignment, and instantly
registers the animation state machine.

Here is the technical design and execution framework for the preloaded weights and
simplistic animation clips for all 7 presets.




📥 1. The Preloaded Weighting Blueprints
Because you want instant application without manual calculation loops, the system
utilizes a Geometric Primitive Matcher that maps vertices based on their normalized
distance from the generated bone lines.

java
  None‎
  public enum WeightingArchetype {
          ORGANIC_FALLOFF, // Uses bone capsule radii with 4-influence
  normalization (Human, Quadruped, Arachnid)
          MECHANICAL_RIGID // 100% assignment to the single nearest bone
  container (Vehicle, Tank, Aircraft, Drone)
  }

Use code with caution.

      ●​ Organic Falloff (1-Click Execution): The system generates a capsule around
         each bone line segment. Vertices within the inner capsule radius get a weight of
         1.0. Vertices in the outer falloff zone blend smoothly with adjacent bone capsules
         using a distance-based cosine distribution, then prune down to the top 4 max
         influences and normalize so their sum equals exactly 1.0.
      ●​ Mechanical Rigid (1-Click Execution): The system completely bypasses vertex
         blending. It partitions the mesh into disconnected topological components or
         localized bounding boxes. Every vertex inside a part (like a drone propeller or a
         car wheel) is snapped with a weight of 1.0 to its corresponding tracking joint.




🏃 2. Preloaded Simplistic Animation Presets
<PARSED TEXT FOR PAGE: 34 / 36>
Each archetype comes pre-packaged with a single, highly characteristic looping
animation clip. These clips are computed procedurally or via low-overhead math loops
to avoid massive external file assets.

👤 1. Human (Bipedal)
   ●​ Preset Clip: Biped_Walk_Loop (30 frames)
   ●​ Mathematical Execution: A simplistic sinusoidal stride. The Pelvis bobs up and
      down on a sine wave. Left and right Thigh joints rotate inversely on the X-axis
      (Math.sin(time)), while Calf joints delay their rotation by 90 degrees to
      simulate a natural knee bend and heel strike.

🐾 2. Quadruped (4-Legged)
   ●​ Preset Clip: Quad_Trot_Loop (24 frames)
   ●​ Mathematical Execution: A synchronized diagonal gait. The Pelvis and Chest
      alternate heights. The Front-Left and Back-Right legs swing forward in perfect
      unison, while the Front-Right and Back-Left legs swing backward, maintaining
      clean structural footing balance.

🕷️ 3. Arachnid (Multi-Legged)
   ●​ Preset Clip: Arachnid_Scuttle_Loop (16 frames)
   ●​ Mathematical Execution: An alternating tripod gait. For an 8-legged mesh, legs
      1 and 3 on the left side move in sync with legs 2 and 4 on the right side. The joint
      rotations use phase-shifted cosine waves, creating a highly realistic, rapid
      skittering motion without feet overlapping.

🏎️ 4. Vehicles (Wheeled)
   ●​ Preset Clip: Vehicle_Drive_Loop (Continuous)
   ●​ Mathematical Execution: Pure procedural rotation. The engine loops through all
      joints tagged as Wheel and applies continuous rotation around their local lateral
      axis based on a virtual velocity variable, while the front steering hubs apply a
      slight sway to simulate active tracking adjustments.

🚂 5. Tanks (Treaded)
   ●​ Preset Clip: Tank_Idling_Stationary (Loop)
   ●​ Mathematical Execution: Stationary chassis resonance. The main Hull bone
      receives a continuous, low-amplitude micro-vibration via a high-frequency noise
<PARSED TEXT FOR PAGE: 35 / 36>
         loop to simulate engine idle, while the Turret_Main executes a slow, periodic
         sweep left and right to verify articulation limits.

✈️ 6. Aircraft (Fixed-Wing)
     ●​ Preset Clip: Aircraft_Flight_Pose (Continuous)
     ●​ Mathematical Execution: Aerodynamic surface checking. The propeller or jet
        turbine joint spins at maximum velocity. The Flap_Wing and Elevator joints
        execute minor, periodic sinusoidal deviations (±5 degrees) to simulate active
        flight stabilization adjustments against wind currents.

🚁 7. Aircraft (Helicopter)
     ●​ Preset Clip: Helo_Rotor_Spin (Continuous)
     ●​ Mathematical Execution: Dual-axis rotational linkage. The Rotor_Main_Hub
        rotates continuously at high speed around the vertical axis, while the
        Rotor_Tail_Hub rotates at a synchronized ratio around the lateral axis. The main
        rotor blades receive a micro-flex wave on their local pitch axes.

🛸 8. Modern Hovering Drone
     ●​ Preset Clip: Drone_Hover_Idle (Continuous)
     ●​ Mathematical Execution: Multi-directional high-frequency stability simulation. All
        4 (or more) rotor blades spin at individual, high-frequency velocity tracks. The
        Frame_Main bone references a continuous 3D Perlin noise loop to execute an
        authentic, floating, auto-correcting hover jitter.




⚡ 3. The "Auto Mesh Apply" Runtime Orchestrator
When the user selects their target rig type and clicks Auto Mesh Apply, your engine
should execute this unified code transaction sequence in the background:

java
 None‎
 public void executeAutoMeshApply(Mesh targetMesh, RigArchetype choice)
 {
         // Phase 1: Clean and unify the geometry
         TopologyEngine.quadRemesh(targetMesh);
<PARSED TEXT FOR PAGE: 36 / 36>
        // Phase 2: Instantiate the skeleton blueprint and scale to target
  mesh dimensions
        Skeleton skeleton = SkeletonFactory.createPreset(choice);
        skeleton.fitToMeshBounds(targetMesh.getBoundingBox());


        // Phase 3: Bind vertices to bones based on configuration rules
        if (choice.getWeightingType() ==
  WeightingArchetype.MECHANICAL_RIGID) {
              WeightAssigner.applyRigidComponentWeights(targetMesh,
  skeleton);
        } else {
              WeightAssigner.applyCapsuleEnvelopeWeights(targetMesh,
  skeleton);
        }


        // Phase 4: Inject the default loop clip into the active Animation
  Controller
        AnimationController controller =
  targetMesh.getAnimationController();
        controller.registerAnimation("Default_Loop",
  AnimationLibrary.getClip(choice));
        controller.play("Default_Loop");


        // Phase 5: Refresh graphics memory buffers
        targetMesh.markBuffersDirty();
  }



Use code with caution.



---

### PDF source transcription: Building a 3D Soundscape Environment Manager.pdf

**Versioned repository extraction:** `docs/editor/requirements/sources/soundscape-environment-manager.txt`.

# PDF transcription archive: Building a 3D Soundscape Environment Manager.pdf
# Original PDF remains the source of truth for figures, formatting and pagination.
# Extracted text below preserves source page headers but may omit graphical content.

<PARSED TEXT FOR PAGE: 1 / 19>
Building a 3D Soundscape Environment Manager in Java 17 involves simulating
acoustic physics—attenuation, occlusion, obstruction, and reverberation—tailored for
stereoscopic (binaural) or traditional stereo output.

Because Java’s native sound API (javax.sound.sampled) is too low-level for advanced
3D spatialization, you will want to build this architecture using OpenAL (via LWJGL 3),
which natively handles 3D vectors, Doppler effects, and environmental audio extensions
(EFX).




1. Architectural Blueprint
To manage spaces, you need a decoupled architecture that separates the physical
game geometry from the acoustic simulation data.

 None‎
 [Audio Source Engine] ---> [Environmental Manager (EFX)] ---> [OpenAL
 Context]
                                           ^
 [Game Room Geometry] --------------+ (Material & Size Map)


   ●​ Audio Space (Room/Zone): Defined as an AABB (Axis-Aligned Bounding Box)
      or an oriented bounding box containing specific acoustic properties.
   ●​ Material Matrix: A registry mapping game texture keys (e.g., "CONCRETE",
      "WOOD", "CARPET") to real-world absorption and reflectivity coefficients.




2. Core Acoustic Parameters for Your Editor
When exposing controls to your game editor suite, model your parameters after the
Environmental Audio Extensions (EFX) standard for predictable physics simulation.

Density and Diffusion

   ●​ Reverb Density: Controls the coloration of the late reverb tail. Low values sound
      metallic (like a tin pipe); high values sound natural.
   ●​ Reverb Diffusion: Controls the echo density in the late reverb tail. Low diffusion
      makes individual echoes distinct.
<PARSED TEXT FOR PAGE: 2 / 19>
Reflectivity & Materials Matrix

Different materials absorb high frequencies faster than low frequencies. Use a
dual-frequency model (Gain and Gain HF).

   Material      Reflectivity     High-Frequency Reflection            Description
                   (Gain)                 (GainHF)


Concrete       0.95             0.80                          High reflection, harsh
Block                                                         high-end.


Thick Carpet   0.15             0.05                          Absorbs almost all high
                                                              frequencies.


Wood           0.70             0.50                          Warm decay, mild high-end
Paneling                                                      absorption.


Water          0.98             0.95                          Massive reflectivity, bright
Surface                                                       echo.



Late Reverb & Decay

   ●​ Decay Time: Total time (in seconds) for the sound to drop by 60dB (RT60).
   ●​ Decay HF Ratio: The ratio of high-frequency decay relative to low frequencies.
      Air naturally absorbs high frequencies over distance, so this should generally be
      < 1.0.




3. Java 17 Implementation Strategy (LWJGL 3 / OpenAL)
To implement this in Java 17, use modern language features like sealed classes and
records to define your acoustic spaces and materials safely and cleanly.

Step A: Define the Material Coefficients

java
 None‎
 public record AcousticMaterial(
<PARSED TEXT FOR PAGE: 3 / 19>
          String name,
          float density,
          float reflectivity,      // Air absorption / Direct reflection
          float gainHF             // High-frequency attenuation multiplier
  ) {}

Use code with caution.


Step B: Define the Room/Environment Space

java
  None‎
  import java.util.UUID;


  public record AudioSpace(
          UUID id,
          Vector3f dimensions,         // Width, Height, Depth
          AcousticMaterial walls,
          AcousticMaterial floor,
          float roomHFReference,       // Reference high-frequency (e.g.,
  5000.0Hz)
          float decayTime              // RT60 value
  ) {}

Use code with caution.


Step C: The EFX Direct Slot Mapping

To pass these room properties down to your OpenAL backend, map your AudioSpace to
an OpenAL Reverb Effect slot.

java
  None‎
  import org.lwjgl.openal.AL10;
  import org.lwjgl.openal.EXTEfx;


  public class AudioEnvironmentManager {
          private final int auxiliaryEffectSlot;
<PARSED TEXT FOR PAGE: 4 / 19>
        private final int reverbEffect;


        public AudioEnvironmentManager() {
              // Generate EFX target handles
              this.auxiliaryEffectSlot = EXTEfx.alGenAuxiliaryEffectSlots();
              this.reverbEffect = EXTEfx.alGenEffects();


              // Initialize effect as a Reverb type
              EXTEfx.alEffecti(reverbEffect, EXTEfx.AL_EFFECT_TYPE,
  EXTEfx.AL_EFFECT_REVERB);
        }


        public void transitionToSpace(AudioSpace space) {
              // Map space properties directly into OpenAL EFX
              EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_DECAY_TIME,
  space.decayTime());
              EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_GAIN,
  space.walls().reflectivity());
              EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_GAINHF,
  space.walls().gainHF());
              EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_DENSITY,
  space.walls().density());


              // Commit effect to the slot
              EXTEfx.alAuxiliaryEffectSloti(auxiliaryEffectSlot,
  EXTEfx.AL_EFFECTSLOT_EFFECT, reverbEffect);
        }


        public int getAuxiliaryEffectSlot() {
              return this.auxiliaryEffectSlot;
        }
  }

Use code with caution.




4. Stereoscopic & Headphone Rendering (HRTF)
<PARSED TEXT FOR PAGE: 5 / 19>
For realistic headphone rendering, panning sounds between left and right channels
using volume manipulation is not enough. You must enable HRTF (Head-Related
Transfer Function). HRTF introduces tiny phase shifts, time delays (ITD), and
frequency modifications (ILD) caused by the shape of the human head and ears.

OpenAL Soft (packaged with LWJGL) supports HRTF natively. You must query and
enable it during device context creation:

java
  None‎
  import org.lwjgl.openal.ALC10;
  import org.lwjgl.openal.ALC11;
  import org.lwjgl.openal.ALCcapabilities;
  import org.lwjgl.openal.SOFTHRTF;
  import java.nio.IntBuffer;


  public class SpatialAudioEngine {
          public void enableStereoscopicHRTF(long deviceHandle) {
              // Check if HRTF extension is available
              if (ALC10.alcIsExtensionPresent(deviceHandle,
  "ALC_SOFT_HRTF")) {
                    // Force OpenAL Soft to use HRTF mode
                    IntBuffer attributes =
  org.lwjgl.BufferUtils.createIntBuffer(5);


  attributes.put(SOFTHRTF.ALC_HRTF_SOFT).put(ALC10.ALC_TRUE);
                    attributes.put(0); // Terminate attribute list
                    attributes.flip();


                    // Reopen context or apply attributes
                    System.out.println("Stereoscopic HRTF successfully engaged
  for headphone listening.");
              } else {
                    System.out.println("HRTF not supported by underlying
  native drivers. Falling back to basic stereo.");
              }
          }
  }

Use code with caution.
<PARSED TEXT FOR PAGE: 6 / 19>
5. Architectural Considerations for the Editor UI
   1.​ Portals (Doorways/Windows): When a player is standing in a dead carpeted
       room but looking out into a massive concrete cathedral, you must mix
       environments. Implement Audio Portals—invisible boundary windows that read
       the reverb profile of the adjacent room and generate a low-pass, delayed version
       of that space's sound at the portal's physical location.
   2.​ Ray-Casted Occlusion: Run periodic, asynchronous ray-casts from the player's
       head position to active audio sources. If geometry intersects the ray, look up the
       material of that geometry and adjust the source's AL_DIRECT_FILTER using a
       low-pass filter (EXTEfx.AL_FILTER_LOWPASS) to simulate the sound passing
       through walls.

To engineer a world-class 3D audio engine that satisfies both discerning game
developers and critical audiophiles, you must account for the physical constraints of
human hearing and digital hardware limitations.

Here is the architectural breakdown of critical engineering pitfalls to eliminate and the
foundational patterns required to build a flawless audio simulation.




Critical Pitfalls to Avoid at All Costs

🛑 Linear Volume Attenuation (The "Abrupt Silence" Bug)
                                                                    𝐷𝑖𝑠𝑡𝑎𝑛𝑐𝑒
Never scale volume linearly relative to distance (𝐺𝑎𝑖𝑛 = 1. 0 − 𝑀𝑎𝑥𝐷𝑖𝑠𝑡𝑎𝑛𝑐𝑒 ). Sound
dissipates following the inverse-square law, which translates logarithmically to human
perception. Linear scaling causes sounds to drop off unnaturally fast close to the
source, then linger weakly at long distances before abruptly cutting to zero.

   ●​ The Fix: Always use Logarithmic Clamped Attenuation Curves
      (AL_INVERSE_DISTANCE_CLAMPED).

🛑 Re-triggering and "Clicking" Audio Frames
Instantly cutting a sound off or changing its properties across a single frame step
boundary generates a discontinuous waveform spike. Audiophiles perceive this as an
irritating pop or click.
<PARSED TEXT FOR PAGE: 7 / 19>
   ●​ The Fix: Never hard-set gain or frequency properties instantly. Always
      interpolate (lerp) audio parameter modifications over a brief window of 10 to
      50 milliseconds using your engine's delta time.

🛑 Thread-Blocking Audio Pipelines
Never query geometric collision assets, load high-fidelity files, or execute heavy spatial
vector equations directly on the main audio processing thread or the engine’s main
render/logic loop. If your frame rate hitches, your audio engine will starve, resulting in
buffer underruns, stuttering, and crackles.

   ●​ The Fix: Treat your audio engine as a completely detached asynchronous
      worker, consuming read-only data structures synchronized from the game world
      state.

🛑 Accumulative Audio Clipping (Digital Overdrive)
If 20 distinct explosion sound effects fire simultaneously and each requests full scale
(Gain = 1.0), mixing them together directly causes hard digital clipping. This destroys
the dynamic range and outputs a distorted crunch.

   ●​ The Fix: Implement an internal hardware limiting headroom buffer combined
      with strict sound category voice limits.




Architectural Design Matrix

 Priority Vector    Anti-Pattern (What to      Gold-Standard Pattern (What to Implement)
                           Avoid)


 Voice             Allowing infinite        Dynamic Virtualization: Instantly drops
 Budgeting         concurrent ambient       imperceptible or distant voices to 0% CPU footprint
                   voices.                  while tracking positions virtually.


 Dynamic           Flattening all audio     Submix Hierarchy & Dynamic Sidechaining:
 Range             channels to the same     Ambient tracks duck dynamically when spatial
                   priority scale.          narrative cues fire.
<PARSED TEXT FOR PAGE: 8 / 19>
Memory           Loading entire long          Double-Buffered Streaming Streams: Stream
Blueprint        music tracks directly into   background loops in 16KB/32KB buffer chunks; keep
                 RAM.                         small, generic impacts in memory.


Spatialization   Relying on basic             True HRTF + EFX Integration: Accounts for head
                 left/right panning           acoustics, interaural delays, and material reflection.
                 models.




The Gold-Standard Implementation Pattern
To achieve clean rendering, implement a thread-safe, decoupled state manager. The
pattern below demonstrates how to format your parameters, prevent hard property
changes using smooth interpolation, and properly register multi-channel environmental
sub-mixes.

java
 None‎
 package com.soundscape.engine;


 import java.util.concurrent.ConcurrentHashMap;
 import java.util.UUID;


 /**
   * Gold-Standard Soundscape Manager.
   * Thread-safe, non-blocking, parameter-interpolated state model.
   */
 public final class SoundscapeEngine {


         // Headroom safety cap to entirely eliminate digital hard-clipping
         private static final float MAX_SYSTEM_HEADROOM_GAIN = 0.85f;
         private static final float INTERPOLATION_FACTOR = 0.12f; // Smooth
 parameter glide


         private final ConcurrentHashMap<UUID, VirtualVoice> activeVoices =
 new ConcurrentHashMap<>();
         private float MasterVolume = 0.75f;
<PARSED TEXT FOR PAGE: 9 / 19>
    public record AudioFrameState(
           float sourceX, float sourceY, float sourceZ,
           float listenerX, float listenerY, float listenerZ,
           float baseGain, boolean isOccluded
    ) {}


    public static class VirtualVoice {
           private float currentGain = 0.0f;
           private float currentLowPassHF = 1.0f;


           /**
           * Core processing loop executing on the dedicated async audio
thread.
           * Smooths property transitions to eliminate digital audio
artifacts or clicks.
           */
           public void processFrame(AudioFrameState state, float
masterVolume) {
                 // 1. Logarithmic Distance Calculation
                 float distance = calculateDistance(state.sourceX,
state.sourceY, state.sourceZ,
                                                    state.listenerX,
state.listenerY, state.listenerZ);


                 // Inverse-square law simulation
                 float targetGain = state.baseGain * (1.0f / Math.max(1.0f,
distance));


                 // Apply occlusion damping if obstructions exist
                 if (state.isOccluded()) {
                     targetGain *= 0.4f; // Muffle volume
                 }


                 // Clamp against system headroom threshold limit
                 targetGain = Math.min(targetGain * masterVolume,
MAX_SYSTEM_HEADROOM_GAIN);
<PARSED TEXT FOR PAGE: 10 / 19>
              // 2. Exponential Parameter Smoothing (Eliminates
waveforms popping/clicking)
              this.currentGain += (targetGain - this.currentGain) *
INTERPOLATION_FACTOR;


              float targetHF = state.isOccluded() ? 0.35f : 1.0f;
              this.currentLowPassHF += (targetHF -
this.currentLowPassHF) * INTERPOLATION_FACTOR;


              // 3. Dispatch to Hardware backend API (e.g., OpenAL / EFX
Mixer)
              applyNativeHardwareParameters(this.currentGain,
this.currentLowPassHF);
         }


         private float calculateDistance(float sx, float sy, float sz,
float lx, float ly, float lz) {
              return (float) Math.sqrt(Math.pow(sx - lx, 2) +
Math.pow(sy - ly, 2) + Math.pow(sz - lz, 2));
         }


         private void applyNativeHardwareParameters(float gain, float
hfFilter) {
              // Under-the-hood bindings happen here:
              // AL10.alSourcef(openALSourceId, AL10.AL_GAIN, gain);
              // EXTEfx.alFilterf(lowPassFilterId,
EXTEfx.AL_LOWPASS_GAINHF, hfFilter);
         }
    }


    public void updateActiveSoundscapes(UUID voiceId, AudioFrameState
updatedFrame) {
         VirtualVoice voice = activeVoices.get(voiceId);
         if (voice != null) {
              // Executed asynchronously without stalling the rendering
logic threads
              voice.processFrame(updatedFrame, this.MasterVolume);
         }
<PARSED TEXT FOR PAGE: 11 / 19>
         }
  }

Use code with caution.




Crucial Golden Rules for the Sound Designer Experience
      1.​ Expose the Dry/Wet Mix Engine: Allow sound designers to independently
          balance the "Dry" sound (unprocessed direct signal) and the "Wet" sound
          (reverberant reflected space environment). A metal room has a very high wet
          ratio, while an open grass field is almost entirely dry.
      2.​ Decouple Sound Assets from Code Triggers: Designers should never have to
          ask a coder to change an asset file. Implement an event abstraction lookup layer
          (e.g., triggering the event "player.walk.concrete" runs an asset-mapped
          container tracking structural round-robin random variations and Pitch/Gain
          randomization modifiers automatically).


The Mechanics of the "Muddy Dialogue" Disaster
The failure of audio clarity in modern multimedia stems from a fundamental mismatch
between high-end studio mixing environments and real-world consumer playback
hardware.

Films and premium games are mixed on calibrated theatrical stages with massive
dynamic range and dedicated discrete center channels for dialogue. When that complex
spatial mix is automatically flattened (downmixed) into stereo or mono, it creates a
chaotic overlap of frequencies that drowns out essential speech.




Why Downmixing Liquidates Dialogue Clarity
In a standard 5.1 or 7.1 surround mix, dialogue is routed almost exclusively to the
dedicated Center (C) channel, while sound effects, music, and ambient textures live in
the Left (L), Right (R), and Surround channels.

When your game engine or receiver downmixes this layout to standard Stereo, it
applies a standardized mathematical fold-down formula:
<PARSED TEXT FOR PAGE: 12 / 19>
𝐿𝑠𝑡𝑒𝑟𝑒𝑜 = 𝐿 + (− 3𝑑𝐵 × 𝐶) + (− 3𝑑𝐵 × 𝐿𝑠)

𝑅𝑠𝑡𝑒𝑟𝑒𝑜 = 𝑅 + (− 3𝑑𝐵 × 𝐶) + (− 3𝑑𝐵 × 𝑅𝑠)

When folded down further into Mono, the formula merges everything:

𝑀𝑜𝑛𝑜 = 𝐿𝑠𝑡𝑒𝑟𝑒𝑜 + 𝑅𝑠𝑡𝑒𝑟𝑒𝑜


The Physics of the Failure

   1.​ Acoustic Masking: When the discrete Center channel is folded directly into the
       Left and Right channels, the crisp, dry dialogue is instantly forced to compete for
       the exact same physical speaker cones as the heavy stereo music and wide
       panning explosions. Because explosions and music share the same fundamental
       mid-range frequencies as human speech (250Hz to 4kHz), they acoustically
       mask the dialogue.
   2.​ Phase Cancellation Over Mono: High-end stereo effects often use
       phase-inversion tricks to sound wide and immersive on headphones. When a
       user forces mono playback (like a mobile phone speaker or a single smart
       speaker), those out-of-phase ambient sounds cancel each other out or form
       destructive interference patterns, creating a hollow, metallic, muddy soup.




The Modern Auto-Ducking Anti-Pattern (What Production Fails to Do)
Most automated ducking systems implemented in modern middleware fail because they
treat audio mixing as a simple, flat volume problem rather than a frequency-specific
problem.

The Wrong Way: Broadband RMS Ducking

Traditional ducking uses a simple compressor sidechain: when a dialogue line plays, the
engine looks at the overall volume (RMS) of the voice and lowers the entire volume
slider of the music/SFX category.

   ●​ The Resulting Failure: This creates an irritating "pumping" effect. The
      background music visibly drops in volume, sounds unnatural, and then
      awkwardly breathes back up when the character pauses for breath. It sounds
      amateurish and still leaves the mid-range frequencies muddy during the actual
      speech.
<PARSED TEXT FOR PAGE: 13 / 19>
The Golden God Solution: Intelligent Frequency-Conscious Ducking
To deliver clean dialogue on high-end headphones, cheap stereo TVs, and mono mobile
devices alike, your custom Java 17 engine must implement a real-time, multiband
dynamic sidechain combined with an internal matrix downmix awareness manager.

 None‎
                                   +----> [Direct Voice Signal]
 -------------------------> [Stereo Matrix Mixer]
                                   |
 ^
 [Dialogue Submix Voice] ---+ (Sidechain Control Signal)
 | (Cleanly Decoupled)


                                   |
 |
                                   +----> [Multiband Inverse EQ Filter] ->
 [Music/SFX Submix] --+



1. Implement Dynamic Frequency Notch-Ducking

Instead of turning down the entire music and ambient background mix, look up the
active frequency range of human speech—specifically the presence and intelligibility
bands (1kHz to 3.5kHz).

When dialogue triggers, use an asynchronous control signal to smoothly cut only that
specific frequency pocket out of your ambient and music submixes by 3dB to 6dB.

     ●​ The Audiophile Result: The background tracks feel completely intact,
        full-bodied, and loud to the user because their low bass and sparkling highs are
        untouched. However, an open acoustic pocket is carved out precisely where
        speech needs to sit, ensuring crystal clarity.

2. Native Dynamic Downmix Matrix Scaling

Do not leave downmixing up to the operating system or the user's hardware. Your
engine should know exactly what speaker configuration is active and dynamically adjust
channel attenuation balances at runtime.
<PARSED TEXT FOR PAGE: 14 / 19>
java
 None‎
 package com.soundscape.engine;


 public enum OutputProfile {
         MONO,
         STEREO_HEADPHONES,
         SURROUND_5_1
 }


 public final class MatrixMixer {


         // Extra gain boost applied to dialogue when downmixing to low-end
 profiles
         private static final float MONO_DIALOGUE_COMPENSATION_GAIN =
 1.414f; // +3dB boost
         private static final float STEREO_DIALOGUE_COMPENSATION_GAIN =
 1.189f; // +1.5dB boost


         public record AudioChannels(float left, float right, float center,
 float surround) {}


         /**
         * Safely flattens spatial channels down to user speaker
 constraints
         * while actively preserving dialogue presence.
         */
         public AudioChannels calculateDownmix(AudioChannels source,
 OutputProfile profile) {
               return switch (profile) {
                  case SURROUND_5_1 -> source; // Clean separation, output
 native


                  case STEREO_HEADPHONES -> {
                      // Boost center channel voice presence before merging
 into stereo boundaries
                      float adjustedCenter = source.center() *
 STEREO_DIALOGUE_COMPENSATION_GAIN;
<PARSED TEXT FOR PAGE: 15 / 19>
                         float outLeft = source.left() + (adjustedCenter *
  0.707f) + (source.surround() * 0.5f);
                         float outRight = source.right() + (adjustedCenter *
  0.707f) + (source.surround() * 0.5f);
                         yield new AudioChannels(outLeft, outRight, 0.0f,
  0.0f);
                    }


                    case MONO -> {
                         // Heavily prioritize center channel presence and
  completely compress/attenuate wide surrounds
                         float dominantDialogue = source.center() *
  MONO_DIALOGUE_COMPENSATION_GAIN;
                         float combinedMono = (source.left() * 0.5f) +
  (source.right() * 0.5f) + dominantDialogue;
                         yield new AudioChannels(combinedMono, combinedMono,
  0.0f, 0.0f);
                    }
              };
         }
  }

Use code with caution.


3. Real-Time Dynamic Range Compression (DRC) Profiles

Audiophiles love raw, uncompressed dynamic range when they are sitting in a silent
room with high-end open-back headphones. But if that same user plays your game on a
laptop in a noisy coffee shop or on a steam deck on a train, the quiet dialogue becomes
completely unhearable over the ambient room noise.

      ●​ The Fix: Implement an Editor Toggle for Dynamic Range Profiles:
            ○​ Theatrical Mode: Maximum dynamic range (100dB). Whispers are dead
               quiet; explosions shake the room.
            ○​ Night/Handheld Mode: High-level dynamic compression. Boosts the
               quietest parts of the dialogue mix by up to 12dB and limits loud impacts,
               flattening the overall output profile so everything remains clearly audible
               over a loud real-world environment.
<PARSED TEXT FOR PAGE: 16 / 19>
An audio engine built with engineering integrity must never carry an elitist bias.
Designing an engine under the assumption that every user has a perfectly calibrated,
multi-channel surround setup is a massive failure of real-world deployment. Statistically,
the vast majority of players will experience a game through standard stereo
headphones, desktop monitors, or the built-in, downward-firing plastic speakers of a
standard flat-panel television.

When a multi-channel mix is blindly compressed and mixed down by a device's
hardware, dialogue is almost always the first casualty—buried entirely beneath a
muddy, overblown wall of dramatic music and un-ducked environmental sound effects.
An engine must be architected defensively to protect speech intelligibility on the lowest
common denominator hardware.




The Reality Shift: High-End vs. Real-World TV Hardware
To understand why dialogue becomes unhearable on a standard bi-output stereo TV, it
helps to directly contrast how the two audio environments handle spatial distribution and
physical limitations.

 Mixing Vector        Idealized Multi-Channel             Real-World Flat-Panel Stereo TV
                       Surround (5.1 / Atmos)


 Dialogue        Isolated completely to a physical,   Folded directly into Left and Right
 Routing         dedicated Center (C) channel         channels, fighting for the same speaker
                 speaker.                             cones as the music.


 Dynamic         Wide headroom (~100dB).              Narrow acoustic floor. Whispers drop below
 Range           Whispers stay quiet; explosions      living room ambient noise; loud peaks rattle
                 physically punch.                    the plastic casing.


 Frequency       High physical separation due to      Complete acoustic masking. Mid-range
 Separation      multiple distinct driver locations   vocal frequencies are blurred together with
                 across a room.                       wide-stereo string instruments.


 Bass Delivery   Low-frequency effects (LFE) are      Heavy low-end frequencies distort small
                 cleanly shunted to an independent    drivers, completely destroying mid-range
                 active subwoofer.                    vocal clarity.
<PARSED TEXT FOR PAGE: 17 / 19>
Implementation Standards for Defensive Mixing
To ensure your Java-based game suite natively counters this issue, your engineering
adoption guidelines should enforce three core spatial audio rules.

1. Implement Frequency-Pocket Masking (Instead of Global Volume Ducking)

Traditional ducking turns down the entire background music track when a character
speaks. This introduces an unnatural, amateurish "pumping" effect that audiophiles
hate, while still failing to clarify vocals on tinny TV speakers.

   ●​ The Gold Standard: Instead of dropping the whole track volume, apply a
      dynamic parametric notch filter specifically to the music and sound effects
      submixes. When dialogue triggers, carve out a clean -4dB to -6dB pocket
      between 1.5 kHz and 3.5 kHz (the critical speech intelligibility band) only on
      conflicting background tracks. The music retains its full bass and high-end
      sparkle, but the dialogue slices cleanly through the middle.

2. Enforce Hardware-Aware Channel Normalization Coefficients

When downmixing a multi-channel environment to stereo for standard television sets,
never trust the consumer hardware or the operating system to handle the fold-down
math. Implement precise scaling coefficients in your mixer class to boost speech
presence natively before the signal leaves your engine.

java
 None‎
 package com.soundscape.engine;


 /**
   * Defensive Audio Downmixer designed to preserve dialogue clarity on
 standard consumer hardware.
   */
 public final class IntelligentDownmixer {


         // Scale factors to lift dialogue cleanly out of the mixed stereo
 field
         private static final float TV_DIALOGUE_BOOST_COEFFICIENT = 1.25f;
 // +2dB vocal presence boost
<PARSED TEXT FOR PAGE: 18 / 19>
       private static final float TV_AMBIENT_REDUCTION = 0.707f;
// -3dB attenuation on wide surrounds


       public record SurroundSignal(float left, float right, float
center, float sub, float leftSurr, float rightSurr) {}
       public record StereoOutput(float leftChannel, float rightChannel
) {}


       /**
       * Converts a surround sound matrix to a defensive, dialogue-first
stereo mix optimized for TV speakers.
       */
       public StereoOutput downmixToTVSpeakers(SurroundSignal source) {
             // 1. Permanently boost the center channel dialogue before
folding it down
             float prioritizedDialogue = source.center() *
TV_DIALOGUE_BOOST_COEFFICIENT;


             // 2. Blend signals down while compressing wide ambient
effects to prevent vocal masking
             float outputLeft = source.left() + (prioritizedDialogue *
0.707f) + (source.leftSurr() * TV_AMBIENT_REDUCTION);
             float outputRight = source.right() + (prioritizedDialogue *
0.707f) + (source.rightSurr() * TV_AMBIENT_REDUCTION);


             // 3. Prevent heavy sub frequencies from rattling tiny
built-in TV cones if a separate kicker isn't detected
             float highPassedLeft = applyHighPassFilter(outputLeft, 90.0f);
// Cut muddy low-end rumble below 90Hz
             float highPassedRight = applyHighPassFilter(outputRight,
90.0f);


             return new StereoOutput(highPassedLeft, highPassedRight);
       }


       private float applyHighPassFilter(float sample, float
cutoffFrequency) {
<PARSED TEXT FOR PAGE: 19 / 19>
              // Implementation of standard digital high-pass filter node
  goes here
              return sample;
         }
  }

Use code with caution.


3. Expose Accessible Dynamic Range Compression (DRC) Toggles

An engine must provide user-selectable profiles that change how audio amplitude is
handled at runtime.

      ●​ Theatrical / Studio Mode: Full, raw dynamic range. Intended only for users with
         high-end, dedicated home theater gear or high-fidelity open-back headphones in
         completely silent rooms.
      ●​ TV / Night Mode: An aggressive, short-attack look-ahead compressor that
         squashes the delta between quiet speech and loud actions. It automatically pulls
         whisper volumes up into audible ranges and strictly clamps down on explosions
         and sudden musical crescendos, completely removing the need for a user to
         constantly ride their volume remote.

This totally doesn't express and expose the author's intense hatred for modern sound mixing
forgetting that you know normal people fucking exist .



---

### PDF source transcription: Engine base.pdf

**Versioned repository extraction:** `docs/editor/requirements/sources/engine-base.txt`.

# PDF transcription archive: Engine base.pdf
# Original PDF remains the source of truth for figures, formatting and pagination.
# Extracted text below preserves source page headers but may omit graphical content.

<PARSED TEXT FOR PAGE: 1 / 29>
Building an enterprise-grade editing engine in Java 17 requires a careful balance
between high-performance core processing and a highly flexible control plane.

Here is the comprehensive, integrated architectural blueprint to handle multi-language
scripting integration, batch orchestration, and endpoint API design.




Scripting & Interpretation Integration

🛠️ System Architecture & Engine Isolation
   ●​ Isolate engines via JVM-isolated ClassLoaders. Prevent third-party scripting
      dependencies from bleeding into your core engine classpath.
   ●​ Abstract via Java 17 Sealed Interfaces. Define a strict
      ScriptExecutionEngine contract using permitted subclasses for each language
      driver (GroovyDriver, GraalPyDriver, V8Driver) to enforce exhaustive pattern
      matching.
   ●​ Enforce CPU and memory quotas per script. Utilize GraalVM's
      ResourceLimitConfig to automatically kill scripts that loop indefinitely or allocate
      excessive memory.
   ●​ Bind context data explicitly. Pass immutable Data Transfer Objects (DTOs) or
      native Java 17 record types to the script context instead of giving scripts direct
      reference access to mutable engine state.

📦 Supported Scripting Engines
   ●​ Groovy (Indy/CompileStatic): Best for high-performance JVM-native scripting
      and heavy pipeline manipulation.
   ●​ GraalJS / GraalPy: Ideal for offering standard JavaScript (ES2022+) and Python
      integration within a unified polyglot ecosystem.
   ●​ LuaJ or GraalWasm: Excellent for ultra-lightweight, sandboxed mathematical
      calculations or byte-level manipulation without garbage collection overhead.




Batching, Controls, & System Integration

 None‎
 [API / Webhook Endpoints]
<PARSED TEXT FOR PAGE: 2 / 29>
             │ (Virtual Threads / Loom)
             ▼
  [Engine Request Queue] ──► [GraalVM Polyglot Sandbox] ──► [Batch
 Aggregator]
                                                                                │
                                                                                ▼
                                                                  [Ring Buffer /
 LMAX Disruptor]
                                                                                │
                                                                                ▼
                                                                  [Java 17
 Foreign Function API]



⚡ High-Throughput Batch Processing
  ●​ Leverage Virtual Threads (Project Loom). Use
     Executors.newVirtualThreadPerTaskExecutor() to handle thousands of
     concurrent scripting sessions without exhausting thread pools.
  ●​ Implement a Ring Buffer architecture. Use libraries like LMAX Disruptor for
     lock-free, ultra-low-latency passing of batch tasks from script engines to the
     native rendering pipeline.
  ●​ Maximize Vector API performance. For pixel or text-array batch
     transformations, compile your hot execution paths using Java 17’s incubating
     Vector API to utilize SIMD hardware instructions.

🔗 Native Systems Integration
  ●​ Migrate from JNI to the Foreign Function & Memory API (Project Panama).
     Use Linker and SymbolLookup to bind native C/C++ libraries safely and
     performantly without complex JNI boilerplate.
  ●​ Enforce zero-copy memory mapping. Use
     FileChannel.map(MapMode.READ_WRITE, ...) along with MemorySegment to
     share massive editing buffers directly between Java and OS storage.




Unified Endpoint APIs
<PARSED TEXT FOR PAGE: 3 / 29>
   ●​ GraphQL / gRPC Core: Use gRPC for intra-system microservice coordination
      and high-frequency real-time timeline editing updates. Use GraphQL for
      client-facing control dashboards to avoid over-fetching complex engine metadata.
   ●​ SSE (Server-Sent Events): Standardize on SSE over standard WebSockets for
      long-running batch progress notifications; it natively handles automated
      reconnection and scales cleaner behind load balancers.
   ●​ Idempotency Keys: Enforce an X-Idempotency-Key header on all mutation,
      batch, or script creation endpoints to prevent accidental double-execution of
      render pipelines.




Engine Guardrails: Must-Haves, Pitfalls, & Anti-Patterns

🚨 Must-Haves (Essentials)
   ●​ Thread-local isolation: Ensure every script instance runs in its own execution
      context; sharing a single language script engine across multiple engine threads
      will cause race conditions.
   ●​ Strict compilation caching: Cache compiled script ASTs (Abstract Syntax
      Trees) or bytecode segments using a size-bounded LRU cache. Compiling
      scripts on every execution path will cripple your engine throughput.
   ●​ Graceful timeouts: Mandate a strict wall-clock time limit for every batch script
      chunk.
   ●​ Structured structured logging: Pass a correlation ID through the engine core,
      scripting environment, and output API responses to track individual batch failures
      across polyglot boundaries.

⚠️ Should-Haves (Quality of Life)
   ●​ Dry-run verification endpoint: An API that syntax-checks and analyzes a
      script's AST for restricted imports without executing it against live engine buffers.
   ●​ Unified exceptions: Wrap polyglot engine errors (PolyglotException,
      GroovyRuntimeException) into a singular, clean domain exception
      (ScriptCompilationException / ScriptTimeoutException) with precise line
      and column numbering.
   ●​ Hot-swapping pipelines: Ability to register, update, and deprecate script
      controls at runtime without rebooting the main engine application.

⛔ Do Not Do's (Critical Anti-Patterns)
<PARSED TEXT FOR PAGE: 4 / 29>
   ●​ Do not use Runtime.getRuntime().exec(). Running external script files via OS
      processes breaks memory tracking, introduces shell injection vulnerabilities, and
      destroys performance.
   ●​ Do not expose native Reflection (java.lang.reflect) to scripts. This allows
      scripts to easily bypass security sandboxes and crash the entire host JVM.
   ●​ Do not use standard Java Serialization. Avoid it entirely for saving script state
      or transferring data between your API and editing engines; rely on JSON,
      Protocol Buffers, or FlatBuffers instead.

💣 Common Pitfalls
   ●​ The "Metaspace" Leak: Dynamically generating classes (e.g., via un-cached
      Groovy Shell conversions or JavaScript evaluations) without proper engine
      recycling will permanently fill the JVM Metaspace, causing unrecoverable
      OutOfMemoryError crashes.
   ●​ Thread Hijacking: Scripting languages often allow users to spawn threads. If
      your sandbox doesn't block thread creation, a malicious script can compromise
      the parent JVM's process scheduling.
   ●​ State Bleeding: Failing to completely clear or reset global variables in language
      engine contexts between batch iterations, causing subsequent script runs to
      behave unpredictably based on data left by previous tasks.

To grant your dynamic scripting environment deep, core-level machine access
(interacting with native OS windows, file systems, GPU contexts, or external C/C++
libraries) without destroying host security, you must establish a strict,
performance-optimized access layer.

Here is the architectural blueprint to safely expose hardware and operating system
capabilities directly to your Java 17 polyglot editing engine.




Core Architecture: Hardware & OS Access Layer
To avoid the performance penalties and security liabilities of old Java approaches,
machine access should be structured into three distinct layers:

  None‎
  [ Dynamic Scripting Context (Sandboxed) ]
                        │
                        ▼ (Safe Script Bindings / Proxies)
<PARSED TEXT FOR PAGE: 5 / 29>
 [ Core Java 17 Engine Control Plane ]
                        │
                        ▼ (Project Panama: Foreign Function & Memory API)
 [ Native Machine Access Layer (C / C++ / OS APIs) ]
           ├── Direct GPU Memory / Vulkan / Metal / DirectX
           ├── OS Window Handles & Display Buffers
           └── Low-Latency Native Audio & Storage I/O




Machine Access Essentials: The Inventory

🚨 Must-Haves (Essentials)
   ●​ Project Panama (java.lang.foreign): Standardize all native library interactions
      using the Foreign Function & Memory API. Use SymbolLookup to bind native
      binaries (.dll, .so, .dylib) and MemorySegment for zero-copy off-heap memory
      allocations that bypass the JVM Garbage Collector.
   ●​ Granular Machine Capabilities Mapping: Define a explicit Java enum for
      machine permissions (e.g., MACHINE_FILE_WRITE, MACHINE_DISPLAY_ACCESS,
      GPU_COMPUTE). Map these strictly to the authenticated script token before opening
      the context.
   ●​ Shared Memory Pools (MemoryLayout): Define structured native memory
      layouts in Java to map out editing frames, timeline buffers, or audio tracks. Pass
      raw pointers (MemoryAddress) to the scripting engine so scripts can modify
      high-performance binary data directly without serialization overhead.
   ●​ Thread-Bound Native Arenas: Enforce strict lifecycle bounds on native memory
      blocks using Arena.ofConfined() or Arena.ofShared(). This ensures that when
      a script completes its batch execution, all associated machine/native resources
      are instantly and safely freed, preventing severe native memory leaks.

⚠️ Should-Haves (Quality of Life)
   ●​ Virtual File System (VFS) Layer: Instead of giving scripts direct access to the
      host's java.io.File, wrap disk access in a sandboxed, root-restricted VFS.
      Scripts see a clean virtual root (e.g., /project/), while the engine securely maps
      this to specific machine paths behind the scenes.
   ●​ Native Crash Guardrails: Since native C/C++ errors (like segmentation faults)
      will instantly crash the entire JVM process, isolate highly volatile machine-access
<PARSED TEXT FOR PAGE: 6 / 29>
         scripts into a separate, lightweight fork of the JVM process that communicates
         with the main engine via high-speed IPC (like shared memory or Unix domain
         sockets).

⛔ Do Not Do's (Critical Anti-Patterns)
   ●​ Do not expose raw pointers directly to the script context. Always wrap native
      memory pointers inside type-safe, bounds-checked Java wrapper objects before
      handing them to an interpreted language.
   ●​ Do not use System.loadLibrary inside script-accessible code. Scripts should
      never have the power to dynamically load arbitrary machine binaries onto the
      host system.
   ●​ Do not perform blocking machine I/O on the main render thread. Any
      machine-level interaction (such as saving a heavy render buffer to an NVMe
      drive) must be offloaded to dedicated background worker threads or Java 17
      Virtual Threads.




Low-Level Native Bindings: Java 17 Implementation
This example demonstrates how to perform high-speed, type-safe machine-level
allocation and native binding using Project Panama, structured to pass a clean,
bounded handler to your scripting engines:

java
 None‎
 import java.lang.foreign.*;
 import java.lang.invoke.MethodHandle;


 public final class MachineMemoryController {


         // Example linking a native C allocation/processing function
 safely
         public static MemorySegment allocateNativeFrame(long width, long
 height, int channels) {
             long totalBytes = width * height * channels;


             // Use a confined arena: memory is automatically freed when
 this block closes
<PARSED TEXT FOR PAGE: 7 / 29>
          try (Arena arena = Arena.ofConfined()) {
               // Allocate off-heap native machine memory
               MemorySegment nativeBuffer = arena.allocate(totalBytes);


               // Fill memory with a clean slate at a machine level
               nativeBuffer.fill((byte) 0);


               // Slice or wrap this segment with explicit read/write
boundaries
               // before presenting a restricted view to the scripting
context
               return nativeBuffer;
          }
    }


    public static void LinkNativeGraphicsLibrary(String libraryPath,
String functionName) throws Throwable {
          // Look up a machine-level library dynamically without JNI
boilerplate
          SymbolLookup nativeLib =
SymbolLookup.libraryLookup(libraryPath, Arena.global());
          Linker machineLinker = Linker.nativeLinker();


          MemorySegment functionAddress = nativeLib.find(functionName)
               .orElseThrow(() -> new IllegalArgumentException("Function
not found in machine binary"));


          // Define the exact C function signature (e.g., void
process_frame(void* data, int size))
          FunctionDescriptor descriptor = FunctionDescriptor.ofVoid(
               ValueLayout.ADDRESS, // Pointer to data


               ValueLayout.JAVA_INT // Buffer size
          );


          MethodHandle nativeMethod =
machineLinker.downcallHandle(functionAddress, descriptor);
<PARSED TEXT FOR PAGE: 8 / 29>
              // This handle can now be safely invoked inside your batch
  pipelines
         }
  }

Use code with caution.




Common Machine Access Pitfalls
      ●​ The Unbounded Array Trap: Interpreted languages (like Python or JavaScript
         running inside GraalVM) do not natively understand Java off-heap memory
         boundaries. If you pass a raw machine pointer, a malformed script loop can read
         or write past the editing buffer, corrupting random host system memory or
         causing sudden process termination.
      ●​ File System Race Conditions: Allowing multiple parallel batch scripts to write
         directly to machine files without an engine-level lock manager will result in file
         corruption and locked OS handles.
      ●​ Asynchronous Signal Deadlocks: When scripts trigger native OS window
         processes or handle direct hardware interrupts (like MIDI or keyboard capture
         events for the editor UI), doing so synchronously blocks the JVM event loops,
         leading to intermittent thread deadlocks.

To accelerate your Java 17 editing engine, you must establish an explicit
Application/Hardware (AH) Machine Tool Integration Path. This layer acts as the unified
operability platform, bridging your dynamic scripting contexts with low-level physical
machine resources, native Window Server handles (HWND/NSWindow), direct GPU
render pipelines, and hardware-accelerated drivers.

The architecture below demonstrates how to achieve raw performance while exposing
zero-cost abstractions to the scripting engines.




The AH Operability Platform Architecture
The key is treating native machine capabilities not as raw OS features, but as an
inventory of Machine Tools managed by a central engine broker.
<PARSED TEXT FOR PAGE: 9 / 29>
  None‎
  ┌─────────────────────────────────────────────────────────┐
  │                    Dynamic Scripting Core (GraalVM / Polyglot)│
  └────────────────────────────┬────────────────────────────┘
                                       │ (Safe Script Tool Interface Binding)
  ┌────────────────────────────▼────────────────────────────┐
  │               AH Operability Broker (Java 17 Registry)                 │
  └────────────────────────────┬────────────────────────────┘
                                       │ (Project Panama Linker & Arenas)
  ┌────────────────────────────▼────────────────────────────┐
  │          Native Capability Layer (Shared Memory C-Bridges)            │
  └───────┬────────────────────┼────────────────────┬───────┘
              │                        │                         │
  ┌───────▼────────┐         ┌───────▼────────┐        ┌───────▼────────┐
  │     OS Compositor │      │    GPU Context     │    │ HW Acceleration│
  │(Win32/Wayland) │         │ (Vulkan/Metal) │        │(AVX-512/SIMD)     │
  └────────────────┘         └────────────────┘        └────────────────┘




Core Integration Paths & Implementation Blueprint

1. The Low-Latency Window Server & OS Compositor Path

To handle interactive editing components (such as a real-time scrubbing canvas or
preview window), your scripting layer needs direct visibility into native UI surfaces
without routing pixels back through heavy JVM objects.

      ●​ The Strategy: Use Project Panama to intercept or request raw OS Window
         Handles (HWND on Windows, NSWindow on macOS, or wl_surface on Wayland).
      ●​ The Operability Bridge: Register these handles as specialized Java
         MemorySegment objects. Wrap them in an engine-defined NativeCanvas tool.
         Pass this tokenized canvas directly to the scripting context. The script can then
         instruct the underlying engine to re-bind native drawing loops entirely off-heap.

2. The Direct Memory & Memory Map (MMAP) Path

When performing mass batch editing operations on heavy visual data, text matrices, or
tracking data, your engine must sidestep the JVM Heap entirely.
<PARSED TEXT FOR PAGE: 10 / 29>
   ●​ The Strategy: Create standard shared memory structures (shm_open or
      file-backed memory maps via FileChannel).
   ●​ The Operability Bridge: Map machine chunks directly using Java 17's
      MemorySegment.mapFile(). This exposes zero-copy, lightning-fast raw binary
      data directly to the sandboxed runtime environment, maximizing performance
      without running into the JVM garbage collector overhead during active execution
      runs.




Machine Tool System Guardrails: Must-Haves & Pitfalls

🚨 Must-Haves (Essentials)
   ●​ Isolated Resource Registries: Implement a strict Token-to-Resource Registry
      in Java. Scripts must never see or manipulate the raw physical address memory
      strings. They should instead reference opaque handles (e.g.,
      tool://gpu/framebuffer_01).
   ●​ Arena Lifecycle Hooks: Bind all native hardware contexts to short-lived Java 17
      confined memory Arena lifecycles. Ensure that when a script task concludes its
      batch cycle, the parent Arena forces a hard machine close, freeing VRAM, OS
      handles, and file descriptors.
   ●​ Deterministic Engine-Level Fencing: Enforce structural memory fences
      (VarHandle.fullFence()) between runtime script loops and raw off-heap
      machine states to protect multi-threaded tasks from data corruption or race
      conditions.

⚠️ Should-Haves (Quality of Life)
   ●​ Virtual Device Emulators for Dry-Runs: Build an interface option to allow
      testing scripts against simulated "null devices" or synthetic frame buffers so
      automated scripts can be completely verified without seizing lock-sensitive
      hardware resources.
   ●​ Strict Thread-Affinity Binding: When interacting with hardware layers that rely
      on explicit thread pinning (such as certain GPU context queues or audio drivers),
      pin script worker paths to concrete OS-level thread threads via Panama
      execution extensions.

⛔ Do Not Do's (Critical Anti-Patterns)
<PARSED TEXT FOR PAGE: 11 / 29>
   ●​ Do not allow scripts to execute arbitrary downcall lookups directly. Scripts
      should never invoke raw function pointer addresses. If a script needs to interface
      with a hardware API, it must run through a pre-bound, engine-verified, type-safe
      Java middleware contract.
   ●​ Do not use traditional un-bounded byte-arrays for crossing borders.
      Passing bulky byte[] segments from Java to native drivers forces expensive
      JVM copying operations, degrading performance instantly.

💣 Common Pitfalls
   ●​ The Graphics Context Desync: If an active script context crashes while holding
      an unreleased lock on an external hardware component (such as an open Vulkan
      frame queue or a camera pipeline handle), the entire OS device context will
      freeze, necessitating a system reboot.
   ●​ The OS-Level Signal Cascade: Unchecked memory manipulation within a script
      can spark native segmentation faults (SIGSEGV). Without a dedicated
      inter-process crash supervisor, an error inside a dynamic script will immediately
      terminate the entire main application JVM process.




Type-Safe Hardware Execution Bridge
Here is how you can implement an execution engine path that wraps raw machine
bindings into a streamlined tool interface container designed to safely bridge your
execution contexts to your hardware layers:

java
  None‎
  import java.lang.foreign.*;
  import java.lang.invoke.VarHandle;


  /**
   * Concrete blueprint for an Application/Hardware (AH) tool
  abstraction.
   * This class wraps a high-speed machine pointer into an immutable,
  type-safe
   * structure that can be cleanly exposed to dynamic script runners.
   */
  public final class AHMachineToolBridge implements AutoCloseable {
<PARSED TEXT FOR PAGE: 12 / 29>
    // Rigid representation of a hardware block (e.g., direct-access
GPU memory layout)
    private static final GroupLayout FRAME_BUFFER_LAYOUT =
MemoryLayout.structLayout(
           ValueLayout.JAVA_INT.withName("width"),
           ValueLayout.JAVA_INT.withName("height"),
           ValueLayout.JAVA_LONG.withName("pixel_pointer")
    );


    private final Arena toolArena;
    private final MemorySegment deviceBufferSegment;


    public AHMachineToolBridge() {
           // Confine this hardware tool wrapper to a single thread
lifecycle
           this.toolArena = Arena.ofConfined();


           // Allocate space according to our physical layout descriptor
           this.deviceBufferSegment =
toolArena.allocate(FRAME_BUFFER_LAYOUT);
    }


    /**
    * Initializes structural hardware dimensions within raw memory.
    * This provides a clean interface for scripting systems to
interact with off-heap bounds safely.
    */
    public void configureHardwareFrame(int w, int h, long rawAddress)
{
           VarHandle widthHandle =
FRAME_BUFFER_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("w
idth"));
           VarHandle heightHandle =
FRAME_BUFFER_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("h
eight"));
<PARSED TEXT FOR PAGE: 13 / 29>
               VarHandle pointerHandle =
  FRAME_BUFFER_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("p
  ixel_pointer"));


               // Inject configuration details safely at the raw hardware
  memory offset layer
               widthHandle.set(deviceBufferSegment, 0, w);
               heightHandle.set(deviceBufferSegment, 0, h);
               pointerHandle.set(deviceBufferSegment, 0, rawAddress);
        }


        /**
          * Returns a strictly bounded context token that is safe to expose
  to dynamic engines.
          */
        public MemorySegment getExposableDeviceSegment() {
               // Returns a restricted view ensuring scripts cannot access
  arbitrary regions outside this frame
               return this.deviceBufferSegment.asReadOnly();
        }


        @Override
        public void close() {
               // Deterministically tear down the tool context and release
  machine handles
               if (toolArena.scope().isAlive()) {
                    toolArena.close();
               }
        }
  }

Use code with caution.

To allow an AI assistant to natively operate your Java 17 editing engine and safely
interact with your GitHub development pipeline, you must establish an explicit
Autonomous Agent Capabilities Matrix. Giving an AI agent core-level editor access
requires shifting from a simple text-generation model to a strictly bounded Tool-Use
(Function Calling) Runtime Architecture.
<PARSED TEXT FOR PAGE: 14 / 29>
Here is the blueprint for the systems, tools, permissions, and default access levels
required for a secure, autonomous AI engineering integration.




Core Tool Matrix & Capabilities
The AI assistant should interact with your editing engine through an explicit Agent
Facade Pattern in Java 17. The agent does not type keys or read arbitrary raw
memory; it invokes highly semantic, schema-backed engine operations.

  None‎
  [ GitHub Webhooks / Workspace Context ]
                       │
                       ▼
     [ AI Agent Control Interface ] ──► (Validates Agent Session Token)
                       │
                       ▼
    [ Agent Security Isolation Facade ]
             ├── Read-Only System Tools (Safe / Public by Default)
             ├── Mutation & Editing Tools (Requires Token / Sandboxed)
             └── High-Risk Machine Tools (Requires Human-In-The-Loop
  Fencing)




Agent System Architecture & Tool Classifications

🔍 1. Discovery & Observation Tools (Read-Only)
These tools should be Enabled by Default for the agent to provide situational
awareness of the editing application state.

   ●​ AST & Codebase Inspectors: Tools that parse the project structure, query
      active Java 17 sealed classes, and extract schema details for your integrated
      scripting engines.
   ●​ Engine Telemetry Monitors: Access to live engine metrics, pipeline throughput
      performance logs, and error stack traces to allow the AI to diagnose scripting
      failures autonomously.
   ●​ Active Environment Diffing: Read-only access to uncommitted workspace
      edits, current configuration files, and script layouts.
<PARSED TEXT FOR PAGE: 15 / 29>
🔧 2. Mutation & Structural Editing Tools (Active Operations)
These tools should be Enabled with Sandbox Constraints by default.

   ●​ AST-Driven Script Generator: The capability to programmatically generate and
      inject scripts (such as custom Groovy or GraalJS routines) directly into the
      engine's memory-safe compilation cache.
   ●​ Component Registration Controller: A tool allowing the AI to dynamically
      register or swap endpoint definitions, API routing rules, or validation logic inside
      the engine's control plane.
   ●​ Automated Test Runner: Access to trigger localized JUnit validation suites or
      headless script evaluations to check code viability before finalizing commits.

🚨 3. High-Risk System Tools (Destructive Operations)
These tools must be Disabled by Default and require explicit system authorization
overrides.

   ●​ Direct File System Writers: Writing files outside the engine's virtual root or
      changing production server deployment scripts.
   ●​ Native Machine Allocation Bindings: The ability to alter low-level Project
      Panama native descriptors (FunctionDescriptor) or direct memory
      configurations.
   ●​ GitHub Repository Push/Merge Access: The capability to forcefully overwrite
      branches or bypass CI/CD pipeline blocking flags.




Security Guardrails: Must-Haves, Pitfalls, & Anti-Patterns

🚨 Must-Haves (Essentials)
   ●​ Capability Tokens & Session Scoping: Bind every AI request to an immutable,
      transient session token. If a request lacks a valid cryptographically signed origin,
      immediately block all tool execution.
   ●​ Strict Virtual File System (VFS) Rooting: Force the AI's file system tooling to
      run inside a strictly isolated folder. The agent must never be able to traverse
      paths using ../ to access system-critical files or credentials.
   ●​ Semantic Schema Enforcement: Expose tools using rigid JSON Schema
      definitions. If the AI generates an argument that deviates even slightly from the
      schema definition, the Java engine must automatically reject it before execution
      hits core code paths.
<PARSED TEXT FOR PAGE: 16 / 29>
⚠️ Should-Haves (Quality of Life)
   ●​ Human-In-The-Loop (HITL) Fences: Implement a tiered validation flag for
      high-impact actions. For example, if the AI agent attempts to run a script
      containing complex machine native calls, intercept the execution and display an
      interactive approve/deny dialog in the control UI.
   ●​ Reversible Transaction Buffers: Route all AI structural modifications through a
      transactional staging environment. If an optimization script degrades the engine's
      frame-rate or compromises stability, the platform should be able to cleanly roll
      back to the stable state.

⛔ Do Not Do's (Critical Anti-Patterns)
   ●​ Do not give the AI raw shell access (Runtime.exec()). The agent should
      never have access to run arbitrary OS terminal commands, as this makes the
      system highly vulnerable to prompt injection or supply-chain exploits.
   ●​ Do not expose raw system security keys to the agent's context window.
      Keep API tokens, GitHub App private keys, and hardware authorization phrases
      securely stored inside Java environmental memory, away from the AI's
      processing view.




Type-Safe AI Capability Guard
Here is how you can implement an explicit, type-safe permission gate in Java 17 using
pattern matching to enforce tool access controls before letting an AI agent execute
structural engine modifications:

java
 None‎
 public sealed interface AgentCapability permits ReadTelemetry,
 ModifyScript, ExecuteNativeCode {}


 record ReadTelemetry(String contextPath) implements AgentCapability {}
 record ModifyScript(String scriptTargetId, String updatedPayload)
 implements AgentCapability {}
 record ExecuteNativeCode(long nativePointerAddress) implements
 AgentCapability {}
<PARSED TEXT FOR PAGE: 17 / 29>
  public final class AIAgentSecurityManager {


        private final boolean isHumanInTheLoopApproved;


        public AIAgentSecurityManager(boolean isHumanInTheLoopApproved) {
               this.isHumanInTheLoopApproved = isHumanInTheLoopApproved;
        }


        /**
          * Enforces explicit, type-safe security evaluation using Java 17
  pattern matching.
          * Evaluates agent requests based on tool severity levels before
  execution.
          */
        public boolean authorizeAction(AgentCapability capability) {
               return switch (capability) {
                    // Read-only observation tools are safe and allowed by
  default
                    case ReadTelemetry read -> true;


                    // System mutation tools require strict internal target
  validation
                    case ModifyScript modify -> {
                         yield
  modify.scriptTargetId().startsWith("sandbox.user.");
                    }


                    // High-risk machine tools are blocked unless explicit
  human validation is active
                    case ExecuteNativeCode nativeCode -> {
                         yield isHumanInTheLoopApproved;
                    }
               };
        }
  }

Use code with caution.
<PARSED TEXT FOR PAGE: 18 / 29>
Common Agent Operability Pitfalls
   ●​ The Infinite Generation Loop: If the AI agent encounters an execution error
      during a batch script execution and attempts to fix it by iteratively re-generating
      and running code without a strict retry quota, it can rapidly deplete computational
      resources and block your task pipelines.
   ●​ Indirect Prompt Injection: If your editing engine processes untrusted user data
      (such as external text tracks or asset metadata) and the AI assistant reads this
      data through its discovery tools, a malicious string could hijack the agent's
      instructions, tricking it into modifying core scripts or wiping configuration files.
   ●​ Stale Environment Divergence: If the AI agent caches workspace snapshots
      locally in memory and coordinates modifications based on outdated state while
      an engineer is simultaneously working on the engine, the agent's tool calls will
      mismatch, resulting in corrupted project states.

To enable an integrated AI developer to natively operate your Model Editor and
surrounding system modules, you must expose a highly structured Semantic Kernel
Interface Layer. An AI cannot read standard GUI pixels or track mouse events
efficiently; instead, it requires structural abstractions, bidirectional state forwarding, and
programmatic automation boundaries.

Here is the exact blueprint of what you must expose, forward, and automate at the
core engine level to make your modules fully AI-operable.




1. What You Need to Expose (The Tool Surface)
You must expose the underlying operations of your Model Editor and modules as clean,
discoverable Java methods wrapped in clear schemas (such as JSON-Schema
definitions using standard OpenAPI or LangChain4j structures).

The Model Editor Module

   ●​ Abstract Syntax Tree (AST) Access: Expose methods to read and rewrite the
      current scene graph, entity-component trees, or 3D/2D model asset structural
      definitions.
   ●​ Component Parameter Introspection: Allow the model to query valid data
      fields, constraints, and ranges for any selected node (e.g., MeshRenderer,
      Transform3D, MaterialShader).
<PARSED TEXT FOR PAGE: 19 / 29>
   ●​ Gizmo & Transform Hooks: Expose programmatic equivalent primitives for
      standard editor interactions: translateNode(id, x, y, z), rotateNode(id,
      qx, qy, qz, qw), and scaleNode(id, x, y, z).
   ●​ Asset Importer Contracts: Expose raw ingestion handles so the AI can
      programmatically command the engine to load or re-cache source meshes
      (.gltf, .fbx), textures, or animations.

Surrounding System Modules

   ●​ The Dynamic Scripting Registry: Expose the endpoints to query which
      automated Groovy or GraalJS routines are bound to specific pipeline nodes.
   ●​ The Batch Pipeline Scheduler: Expose control methods to sequence, prioritize,
      pause, or kill current rendering or data-processing queues.




2. What You Need to Forward (The Real-Time Context)
The AI assistant cannot act blindly; it requires a continuous feed of application events to
maintain situational awareness. You must forward these core engine vectors via
high-speed asynchronous streams (such as SSE or gRPC lines):

  None‎
  [ Core Model Editor Engine ] ──► (Event Bus) ──► [ AI Context
  Aggregator ]
                                                                         │
     ┌──────────────────────┬───────────────────────────────┤
     ▼                            ▼                                      ▼
  [ Selection State ]        [ Pipeline Diagnostics ]         [ Structural Diffs ]


   ●​ The Selection State Vector: Forward the active user selections (ActiveNodeID,
      FocusedViewport, LastModifiedComponent). This allows the AI to understand
      exactly what the human engineer is looking at or working on in real time.
   ●​ Continuous Structural Diffs: Whenever any module state shifts, forward a
      serialized delta token (e.g., a lightweight JSON Patch document) to the AI
      context. This prevents the model from having to re-fetch the entire world state
      layout on every turn.
   ●​ Pipeline Diagnostic Streams: Forward engine logs, material compilation errors,
      and Panama-native execution crashes immediately. If a script or model
<PARSED TEXT FOR PAGE: 20 / 29>
      modification fails, the AI should ingest the raw stack trace vector without human
      intervention.




3. What You Need to Automate Access To (The Agent Autonomy)
To make the AI an efficient co-developer rather than a passive assistant, you must
automate its ability to manipulate the system environment cleanly:

   ●​ Automated Rollback & Staging Frames: Automate the creation of a temporary
      "transaction layer" for every AI workspace adjustment. The AI must be able to
      push changes to a speculative context, evaluate performance, and run a safe
      rollback() if the modification triggers engine latency spikes.
   ●​ Batch Dependency Resolution: When the AI inserts a new tool path or
      modifies a model reference, automate the underlying resource assembly. The
      engine should automatically pull structural requirements, rebuild necessary
      off-heap memory arenas, and clear the compilation caches without forcing the
      model to coordinate low-level JVM lifecycles manually.
   ●​ Headless Validation Cycles: Provide an automated, high-speed execution route
      where the AI can command the engine to run headless renders, verify asset
      bounds checks, or test scripting integrity prior to locking changes into the active
      viewport.




Summary Matrix for System Integration

  Functional             Responsibility                    Implementation Mechanism
    Layer


Expose         Provide semantic command hooks        Java 17 records mapped to strict
               for every manual tool action.         JSON-Schema function parameters.


Forward        Keep the AI's internal memory state   High-speed, lock-free gRPC streams or
               synced with the runtime platform.     SSE push brokers.


Automate       Protect the host system while         Transactional memory sandboxes, VFS
               allowing rapid iterations.            rooting, and headless unit verifications.
<PARSED TEXT FOR PAGE: 21 / 29>
To maximize the performance and efficiency of an integrated AI developer, you must
minimize the number of tokens exchanged over the network and reduce the cognitive
overhead of the LLM context window. The secret to low-token, high-frequency
automation is semantic compression—shifting from raw code generation to highly
optimized, macro-driven declarative state tokens.

Here is the programmatic blueprint to minimize token usage while accelerating your
Java 17 editing engine's automated operability.




1. Token Compression: Shifting to Compact State Vectors
If the AI has to read or write hundreds of lines of explicit Java or JSON code on every
turn, your context windows will quickly fill with redundant data, driving up operational
costs. You must collapse this communication channel into dense, compact
representations.

🗜️ Use Protocol Buffers or FlatBuffers for Context Snippets
Instead of passing raw, verbose JSON structures across your API line, use standard
Protocol Buffers (Protobuf) or FlatBuffers. When the AI requires a textual overview,
use an engine-level encoder that translates your complex entity graph into a
hyper-compressed, single-line text variant.

🏷️ The "Entity-Component-Delta" (EC-Delta) Pattern
Never send the entire scene graph to the model. Force your forwarder engine to track a
precise, string-minified delta syntax.

   ●​ Verbose Approach (Bad): Sending 200 lines of a node's full configuration.
   ●​ Compressed Approach (Good): Passing a short change string:
      MOD:node_421|T[10,0,-5]|R[0,90,0]. The AI can easily parse this shorthand,
      saving thousands of input tokens per roundtrip.




2. Micro-Commands & Shorthands (The Core Tool DSL)
Build a streamlined, text-based Domain Specific Language (DSL) directly into your
scripting interpreter. By exposing dense micro-commands, the AI can perform complex
multi-module operations using just a few characters.
<PARSED TEXT FOR PAGE: 22 / 29>
⚡ Unified Micro-Command Batching
Provide a unified, single-string execution route (engine.runDsl(String macro)) that
parses multi-target adjustments instantly. Design a dense shorthand grammar for
common model editing and scripting tasks:

    ●​ +N[id, type] → Create a new node entity.
    ●​ .S[id, prop, val] → Set a specific property value on a node.
    ●​ >P[src, dest] → Parent a node to another target node.
    ●​ !B[pipeline_id] → Immediately trigger a headless batch validation frame.

By teaching the AI this micro-shorthand via its system prompt, a complex operation that
would normally require a massive multi-argument function call can be compressed
down into a clean, single-token text array:

text
  None‎
  // AI Output occupying a fraction of standard token weights
  +N[mesh_0, StaticMesh]; .S[mesh_0, path, "cube.gltf"]; >P[mesh_0,
  root]; !B[render_v1]

Use code with caution.




3. Programmatic Shorthands & Java 17 Coding Tricks
To keep your backend architecture fast, clean, and highly responsive to these
compressed inputs, leverage modern Java 17 features to process micro-commands
without performance overhead.

🗂️ Sealed Interface Pattern Matchers (Zero-Allocation Token Routers)
Parse your compressed micro-commands directly into highly performant Java 17
record types using sealed command routing. This keeps parsing pipelines free from
complex regex performance leaks:

java
  None‎
  public sealed interface EngineMacro permits CreateNode, SetProperty,
  RunBatch {}
<PARSED TEXT FOR PAGE: 23 / 29>
  record CreateNode(String id, String type) implements EngineMacro {}
  record SetProperty(String id, String property, String value)
  implements EngineMacro {}
  record RunBatch(String pipelineId) implements EngineMacro {}


  public final class MacroCommandProcessor {


          /**
          * Rapidly routes compressed tokens using Java 17 type pattern
  matching.
          */
          public void executeMacro(EngineMacro macro) {
                switch (macro) {
                    case CreateNode(var id, var type) ->
  coreEngine.provisionNode(id, type);
                    case SetProperty(var id, var prop, var val) ->
  coreEngine.modifyComponentValue(id, prop, val);
                    case RunBatch(var pipelineId) ->
  pipelineScheduler.triggerHeadlessExecution(pipelineId);
                }
          }
  }

Use code with caution.


🧵 Virtual-Threaded Command Aggregators
When the AI submits a batch of micro-commands, do not process them sequentially on
your main UI or render threads. Wrap the macro array inside an atomic execution frame
and offload the calculations to a high-speed Project Loom executor:

java
  None‎
  public void processIncomingAIBatch(List<EngineMacro> batch) {
          try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                executor.submit(() -> {
<PARSED TEXT FOR PAGE: 24 / 29>
                    // Apply transactional memory fencing before running the
  batch
                    VarHandle.fullFence();


                    for (EngineMacro command : batch) {
                         macroProcessor.executeMacro(command);
                    }


                    // Commit changes to the off-heap native display arenas
                    coreEngine.flushNativeBuffers();
              });
         }
  }

Use code with caution.




Guardrails for Low-Token AI Automation

🚨 Must-Haves (Essentials)
      ●​ Pre-compiled Macro Schemas: Cache the text-parsing definitions for your
         micro-commands at runtime. Never force the engine to dynamically evaluate
         strings using expensive reflection calls.
      ●​ Structural Command Idempotency: Assign a single unique transaction ID to
         every compressed macro sequence to prevent network glitches from triggering
         duplicate model updates.
      ●​ Deterministic Macro Validation: If the AI outputs a malformed micro-command
         shorthand (e.g., missing a bracket or specifying an illegal node property),
         short-circuit execution immediately and return a compressed error code (e.g.,
         ERR:404|INVALID_PROP) to keep the diagnostic loop token-efficient.


⛔ Do Not Do's (Critical Anti-Patterns)
      ●​ Do not allow the AI to generate raw Java source files for standard
         modifications. Forcing the AI to draft complete classes for simple tasks like
         modifying a transform properties structural element wastes millions of tokens and
         floods the Metaspace cache.
<PARSED TEXT FOR PAGE: 25 / 29>
      ●​ Do not include full system logs in the AI's diagnostic context. Instead of
         passing a 1,000-line stack trace when a pipeline fails, wrap errors in clean,
         minified diagnostic codes that highlight only the failing line, index, and error
         cause.

To build a bulletproof editing platform, you cannot rely on standard Java exceptions
when interfacing with raw hardware and dynamic code. A native crash inside a Project
Panama downcall or an invalid pointer write will bypass JVM catch blocks, immediately
causing a hard SIGSEGV crash that takes down the entire application.

To prevent this, you must implement an Isolated Diagnostics & Validation Plane
directly inside the base platform.




The Dual-Process Safety Architecture
To prevent volatile script or hardware errors from crashing your core editor, decouple
the engine into a stable Control Host Process and an ephemeral, hardware-bound
Execution Worker Process.

  None‎
  ┌────────────────────────────────────────────────────────┐
  │                  CONTROL HOST JVM (Stable Interface)                     │
  │     ├── AI Agent & User API Endpoints                                    │
  │     └── Heartbeat Watchdog & Memory Ring Buffer                          │
  └───────────────────────────┬────────────────────────────┘
                                       │ (High-Speed IPC: Shared Memory / IPC
  Pipe)
  ┌───────────────────────────▼────────────────────────────┐
  │                 EXECUTION WORKER JVM (Volatile Core)                         │
  │     ├── GraalVM Polyglot Runtime & Micro-DSL Runner                  │
  │     ├── Project Panama Native AH Machine Tools                           │
  │     └── Diagnostic Interceptor (SIGSEGV / Signal Catcher) │
  └────────────────────────────────────────────────────────┘




Core Components of the AH Diagnostics Engine

1. Hardware State Verification (Pre-Flight Sanitization)
<PARSED TEXT FOR PAGE: 26 / 29>
Before any script or AI command interacts with an off-heap asset or native graphics
context, the engine must run a deterministic pre-flight check.

   ●​ Pointer Bounds Fencing: Verify that target MemorySegment addresses fall
      strictly inside allocated off-heap arena bounds before executing downcalls.
   ●​ Hardware Context Interrogation: Check device statuses (e.g., Vulkan device
      lost flags, native window visibility, file lock tables) before writing data.

2. Native Signal Channelling (java.lang.Process Supervisor)

Because a segmentation fault terminates the JVM immediately, the Host JVM
supervises the Worker JVM process. If the Worker JVM drops offline due to a bad
native pointer write, the Host JVM catches the crash exit code, captures the shared
memory crash logs, re-provisions the worker thread in milliseconds, and returns a clean,
structured diagnostic token to the user or AI agent.

3. Continuous Function Testing & Speculative Frames

Implement Speculative Execution Pipelines inside your engine. When an AI or
automated script submits a batch of operations:

   ●​ The engine forks the current model state into a temporary, isolated off-heap
      memory buffer.
   ●​ It executes a "headless validation render frame" on this sandbox branch.
   ●​ It monitors native diagnostic flags. If execution succeeds without memory leaks
      or timing traps, the buffer is flipped to the live view. If it fails, the transaction is
      abandoned with zero system pollution.




Core Implementation: Type-Safe Diagnostic & Validation Engine
Here is how you can implement a type-safe platform validator in Java 17 that verifies
memory boundaries, runs deterministic function tests, and packages errors into clean,
token-efficient diagnostic data structures:

java
 None‎
 package com.engine.diagnostics;


 import java.lang.foreign.MemorySegment;
<PARSED TEXT FOR PAGE: 27 / 29>
import java.lang.foreign.Arena;
import java.util.Optional;


public final class AHHardwareDiagnosticsEngine {


    // Token-efficient, dense diagnostic codes
    public enum DiagStatusCode {
          OK,
          ERR_OUT_OF_BOUNDS,
          ERR_HARDWARE_DISCONNECTED,
          ERR_FUNCTION_TEST_TIMEOUT
    }


    // Minified diagnostic payload designed for low-token AI parsing
    public record DiagnosticReport(DiagStatusCode code, String
metadata) {
          @Override
          public String toString() {
                return "DIAG:" + code.name() + "|" + metadata;
          }
    }


    /**
     * Pre-flight validation gate ensuring off-heap memory boundaries
are pristine
     * before native execution takes place.
     */
    public DiagnosticReport validateMemoryToolBounds(MemorySegment
activeSegment, long targetedOffset, long length) {
          if (activeSegment == null || !activeSegment.scope().isAlive())
{
                return new
DiagnosticReport(DiagStatusCode.ERR_HARDWARE_DISCONNECTED,
"SEG_DEAD");
          }


          // Verify bounds deterministically prior to downcall routing
<PARSED TEXT FOR PAGE: 28 / 29>
          if (targetedOffset < 0 || (targetedOffset + length) >
activeSegment.byteSize()) {
              return new
DiagnosticReport(DiagStatusCode.ERR_OUT_OF_BOUNDS,
                  "LIMIT:" + activeSegment.byteSize() + "/REQ:" +
(targetedOffset + length));
          }


          return new DiagnosticReport(DiagStatusCode.OK,
"SYS_PRISTINE");
    }


    /**
     * Executes an isolated, headless function test against a
speculative
     * system frame buffer to catch rendering regressions safely.
     */
    public DiagnosticReport runSpeculativeFunctionTest(Runnable
validationTask, long timeoutMs) {
          long startTime = System.currentTimeMillis();


          try (Arena speculativeArena = Arena.ofConfined()) {
              // Run the tool simulation inside an isolated, short-lived
arena
              validationTask.run();


              if ((System.currentTimeMillis() - startTime) > timeoutMs)
{
                  return new
DiagnosticReport(DiagStatusCode.ERR_FUNCTION_TEST_TIMEOUT,
"TIME_EXCEEDED");
              }


              return new DiagnosticReport(DiagStatusCode.OK,
"TEST_PASSED");
          } catch (Throwable nativeEx) {
              // Trap low-level exceptions before they bleed into host
frameworks
<PARSED TEXT FOR PAGE: 29 / 29>
                    return new
  DiagnosticReport(DiagStatusCode.ERR_HARDWARE_DISCONNECTED, "EX:" +
  nativeEx.getClass().getSimpleName());
              }
         }
  }

Use code with caution.




Robust Diagnostics Guardrails: Must-Haves & Pitfalls

🚨 Must-Haves (Essentials)
      ●​ Zero-Allocation Error String Aggregation: Do not dynamically allocate heavy
         string buffers during high-frequency diagnostic validation steps. Rely on fixed,
         pre-allocated diagnostic enum codes or bitwise flags to save memory overhead.
      ●​ Deterministic Heartbeat Interceptor: Implement an active watchdog loop that
         checks the health of active hardware contexts and background worker threads
         every few milliseconds.
      ●​ Leaked Arena Detectors: Wrap memory spaces in tracked scopes. If a script
         creates an allocation task and exits without explicitly disposing of its native
         workspace, the diagnostic layer must flag the resource leak and auto-close the
         dangling scope.

⛔ Do Not Do's (Critical Anti-Patterns)
      ●​ Do not print raw native memory stack traces directly to logs. Exposing
         un-mapped physical addresses or hex dumps to an AI agent can confuse its
         prompt context. Always parse raw native faults into standardized, dense error
         signatures (DIAG:ERR_OUT_OF_BOUNDS|LIMIT:1024).
      ●​ Do not let diagnostic tasks block hot rendering loops. Real-time telemetry
         gathering must run asynchronously or handle data passing via lock-free memory
         ring buffers.



---

### PDF source transcription: multi-genre game creation engine.pdf

**Versioned repository extraction:** `docs/editor/requirements/sources/multi-genre-game-creation-engine.txt`.

# PDF transcription archive: multi-genre game creation engine.pdf
# Original PDF remains the source of truth for figures, formatting and pagination.
# Extracted text below preserves source page headers but may omit graphical content.

<PARSED TEXT FOR PAGE: 1 / 18>
Building a multi-genre game creation engine in Java 17 requires abstraction layers that
decouple game logic from your core rendering and systems loop. To support widely
different genres like Turn-Based Strategy (TBS), Real-Time Strategy (RTS), and
Match-Three puzzles within the same editor ecosystem, you need specialized,
high-level authoring tools.

Here are the essential tools and modules you should include in your engine
configuration to seamlessly handle these varied genres:


🧩 Puzzle & Board Logic Tools (Match-Three, Logic Games)
A generic tilemap editor isn't enough for puzzle mechanics. You need tools that treat
tiles as dynamic, stateful entities rather than just static background textures.

   ●​ Matrix & Grid Relationship Designer: A dedicated editor panel to establish
      board dimensions, coordinate systems (orthogonal, isometric, hexagonal), and
      cell-to-cell structural relationships.
   ●​ Grid State Change & Matching Engine: An integrated rule builder where you
      can visually define match criteria (e.g., straight lines, T-shapes, L-shapes) and
      fall-away cascade behaviors without rewriting core loop code.
   ●​ Turn & Phase Lifecycle Manager: A framework for managing step-by-step
      game loops. It tracks who or what acts next, handling phases like Player Input ->
      Match Detection -> Board Cascade -> Refill -> Check Defeat/Victory Conditions.


🗺️ Turn-Based & Real-Time Strategy Tools
Strategy games live and die by spatial reasoning, resource distribution, and macro-level
management.

   ●​ Heuristic Graph & Node Linker: A visual tool layered over your map editor to
      bake pathfinding graphs. It must support customizable node weights (e.g., higher
      travel costs for mountains, mud, or water) for algorithms like A* or Dijkstra.
   ●​ Influence Map & Control Visualizer: An editor viewport overlay that calculates
      and displays strategic metrics like territory control, resource density, threat zones,
      and fog-of-war bounds.
   ●​ Hierarchical State Machine (HSM) Designer: A node-based tool to construct
      complex AI behaviors. It lets you cleanly separate micro-behaviors (e.g., pathing
      to a resource) from macro-behaviors (e.g., retreating when health drops below
      20%).
<PARSED TEXT FOR PAGE: 2 / 18>
📊 Systemic, Economy, & Global State Tools
Both strategy game progression and puzzle unlock systems rely heavily on numbers,
dependencies, and data structures.

   ●​ Virtual Database & Infopedia Sync: A data-table editor (handling CSV, JSON,
      or custom binary manifests) that links item registries, faction attributes, upgrade
      paths, and build costs directly to runtime components.
   ●​ Virtual Cursor & Multi-Schema Input Emulator: An input translation tool that
      cleanly maps mouse actions to simulated game pad inputs or virtualization
      wrappers, allowing you to easily test strategy cursor controls or puzzle dragging
      schemas side-by-side.
   ●​ Macro Simulation & Balance Tester: A headless runner capability that can
      simulate thousand-turn economies or puzzle-board generations instantly. It logs
      progression curves, resource accumulation rates, and win/loss possibilities to
      catch balance issues before rendering a single frame.

o expand your Java 17 engine to support racing games, you need to pivot from discrete,
turn-based grid data structures to continuous spatial mathematics, high-frequency
physics interactions, and spline-driven automation frameworks.

Here are the critical tools and modules you should implement in your editor to handle
racing mechanics natively:


🛣️ Track Authoring & Spline Tools
Racing games rely heavily on vector mathematics to define paths, boundaries, and AI
orientations.

   ●​ Bezier & Catmull-Rom Spline Tool: A tool to plot 3D paths for track centerlines.
      The editor must automatically extrude a 3D mesh (or layout track tiles) along this
      curve and handle banking angles, track widening, and vertical elevation
      smoothly.
   ●​ Vector Sector & Checkpoint Slicer: A tool that slices the spline track into
      sequential triggers or spatial sectors. This handles mandatory checkpoint
      validation (to prevent shortcuts/cheating), lap counting, and split-time tracking.
   ●​ Surface Material & Friction Matrix Map: A tool to paint or assign physics
      properties (friction coefficient, drag, bounce, surface noise) to different parts of
      the track mesh (e.g., asphalt, gravel, ice, grass).


🏎️ Vehicle Physics & Rig Tuning Panels
<PARSED TEXT FOR PAGE: 3 / 18>
Rather than hardcoding car physics, your editor needs a specialized debugging and
configuration interface to tweak vehicles at runtime.

   ●​ Suspension & Raycast Wheel Visualizer: A specialized viewport debugging
      overlay that renders raycast suspension lines, spring compression states, wheel
      travel, and contact points in real time to balance vehicle stability.
   ●​ Drivetrain & Torque Curve Grapher: A tool to visually map engine RPM to
      torque output, along with gear ratio configurations, clutch slip values, and
      differential locking types.
   ●​ Aerodynamic Drag & Downforce Profiler: A simple editor panel to define
      frontal surface area, drag coefficients, and velocity-scaled downforce vectors
      applied at the front and rear axles.


🤖 AI Pathing & Telemetry Tools
Racing AI needs to act like an analog driver making millisecond corrections, completely
different from a strategy game's pathfinding graph.

   ●​ Ideal Racing Line Optimizer: A tool that samples your track spline, analyzes the
      geometry of upcoming curves, and computes the mathematically optimal "fastest
      line" (braking, apex, and exit acceleration points).
   ●​ Spatial Steering Profiles & Catch-Up (Rubber-Banding) Editor: A node or
      slider interface to define AI behaviors, such as aggression levels, defensive
      steering lanes, and velocity scaling factors based on distance from the player.
   ●​ Ghost Car Telemetry Recorder: A serialization module that snapshots vehicle
      position, velocity, and rotation vectors at a high frequency (e.g., 20Hz or 30Hz) to
      save, compress, and playback a "ghost car" run for time-trial modes.

Real-Time Strategy (RTS) games are among the most resource-intensive genres to
engineer. Unlike turn-based systems, an RTS requires hundreds of autonomous entities
to constantly update their spatial, logical, and tactical states simultaneously.

To support an RTS natively in your Java 17 engine, your editor and backend require
highly specialized architectural tools designed for mass concurrency, spatial
efficiency, and deterministic synchronization.




🗺️ 1. Spatial Partitioning & High-Density Pathfinding Tools
<PARSED TEXT FOR PAGE: 4 / 18>
When 500 units move across a map at the same time, running a standard A*
pathfinding search for each individual unit every frame will completely tank your JVM's
performance. You need tools to bake and manage spatial data structures.

   ●​ Hierarchical Pathfinding (HPA) Grid Baker:*
         ○​ The Tool: An editor module that breaks your massive world map into
             macro-chunks (e.g., 32 × 32 tile clusters) and pre-calculates entrance/exit
             "clusters."
         ○​ Why it's essential: Units pathfind across the world map macro-chunk by
             macro-chunk (incredibly fast), and then use local A* only to navigate their
             immediate chunk. This keeps pathfinding calculations lightweight and
             scalable.
   ●​ Dynamic Flow Field / Vector Field Generator:
         ○​ The Tool: An engine system that generates a map of directional vectors
             guiding units toward a target destination.
         ○​ Why it's essential: Instead of 100 selected units calculating 100
             individual paths to the same gold mine, you calculate one flow field for that
             destination. All units in that area simply read the vector directly beneath
             them to know which way to steer.
   ●​ Density Call Averaging & Terrain Analyzer:
         ○​ The Tool: A viewport utility that analyzes unit grouping behaviors and
             aggregates local density calls.
         ○​ Why it's essential: It prevents bottleneck congestion in tight choke points.
             Note that over-averaging or smoothing these density passes during world
             generation can lead to a blocky, chunkier terrain profile—balance the
             calculation frequency carefully to keep your terrain geometry smooth.




🏹 2. Unit Logic, Selection, & Micro-Management Tools
Managing crowds of units requires precise grouping logic and decoupled behavior
management.

   ●​ Frustum & Quadtree Box-Selection Marquee:
         ○​ The Tool: A viewport projection tool that maps your 2D screen drag-box
            into a 3D world space bounding volume.
         ○​ Why it's essential: It query-filters units instantly via a spatial Quadtree or
            Octree structure rather than looping through every entity in the game
            world to see if its coordinates fall inside the box.
<PARSED TEXT FOR PAGE: 5 / 18>
   ●​ Decoupled Steering Behavior Sandbox:
        ○​ The Tool: A visual tuning profile panel to configure Reynolds steering
           behaviors (Flocking, Separation, Alignment, Obstacle Avoidance).
        ○​ Why it's essential: RTS units need to move as a cohesive army without
           phasing through each other or getting stuck on geometry. This panel lets
           you balance separation forces against arrival speed in real time.
   ●​ Behavior Tree / Hierarchical State Machine (HSM) Editor:
        ○​ The Tool: A node-based logic constructor that visualizes unit states.

                                                   ➡️                      ➡️
        ○​ Why it's essential: An RTS unit has complex layered behaviors. A worker

             ➡️
           unit needs a state machine like: Idle      Move to Resource        Gather
               Return to Base Stack. If attacked midway, an interrupt branch must
           smoothly transition it to Flee or Defend without destroying its previous task
           memory.




🌾 3. Economy, Tech Trees, & Global State Manifests
RTS games rely on complex, branching production loops where players harvest
resources, build structures, and unlock upgrades.

   ●​ Centralized Entity Registry & Virtual Database (Data-Tables):
          ○​ The Tool: A built-in spreadsheet editor (exporting to clean JSON, CSV, or
             custom binary manifests) that links unit stats, armor types, move speeds,
             and construction costs.
          ○​ Why it's essential: It separates data from implementation. Modifying a
             unit's health or training time shouldn't require recompiling Java code; your
             simulation engine should simply parse these localized registries at startup.
   ●​ Interdependent Tech-Tree Graph Designer:
          ○​ The Tool: A visual node editor where you draw dependency wires
             between structures, technologies, and unit unlocks (e.g., unlocking
             Barracks requires Supply Depot; researching Iron Working applies a
             global +1 attack modifier to all Swordsman entity instances).
   ●​ Fog-of-War Surface Mask Baker:
          ○​ The Tool: A tool that dynamically renders and updates a low-resolution
             overlay texture mapping out three visibility states: Unexplored (black),
             Explored/Shroud (shadowed), and Visible (clear).
<PARSED TEXT FOR PAGE: 6 / 18>
🛡️ 4. Concurrency & Network Simulation Diagnostics
Because RTS games require total synchronization across multiple clients, you need
tools to enforce strict lockstep execution.

   ●​ Deterministic Simulation Frame-Ticker:
         ○​ The Tool: A profiling tool that isolates the simulation clock from your
            engine's rendering frame rate.
         ○​ Why it's essential: RTS multiplayer typically relies on a lockstep
            architecture—only player inputs (commands) are sent over the network,
            not unit positions. The game engine must execute commands
            deterministically on a rigid tick rate (e.g., exactly 20 ticks per second) so
            every player's game simulates identical results.
   ●​ Memory Exploitation Guard & ByteBuf Profiler:
         ○​ The Tool: A low-level performance view integrated into your developer
            tools (or an F3 debug overlay extension) tracking data allocations.
         ○​ Why it's essential: Network commands pass rapidly through your I/O
            layer. Incorporating native resource leak detection and tracking buffer
            release cycles ensures your networking modules don't suffer from memory
            fragmentation or heap exhaustion during prolonged matches with
            thousands of active network packets.



Turn-Based RPGs and JRPGs shift the engine’s primary burden away from
high-frequency physics loops and mass pathfinding. Instead, they focus heavily on
complex state machines, deeply nested data dependencies, narrative branching,
and deterministic combat phases.

To support these sub-genres natively in your Java 17 engine, your editor suite needs
tools that can manage thousands of discrete items, massive dialogue trees, and highly
structured combat systems.




⚔️ 1. Phase-Driven Combat & Battle Arena Editors
Combat in JRPGs takes place in a highly regulated, discrete state space (often
transitioning from a world map to a dedicated battle scene).

   ●​ Turn-Order Pipeline Visualizer (Tick/Speed Based):
<PARSED TEXT FOR PAGE: 7 / 18>
        ○​ The Tool: An editor panel to construct and debug turn-allocator algorithms
           (e.g., Traditional Round-Robin, Conditional Speed Ticks like Final
           Fantasy’s Active Time Battle, or Conditional Action-Cost systems).
        ○​ Why it's essential: You need to simulate how speed modifiers, haste
           statuses, or heavy-weapon delay penalties dynamically shift the combat
           queue order without running a full gameplay instance.
   ●​ Combat Grid & Spatial Position Mapper:
        ○​ The Tool: A visual grid-placement utility within the battle arena editor.
        ○​ Why it's essential: Many tactical or traditional JRPGs rely on rows (Front
           Line/Back Line) or spatial positioning grids. This tool allows you to map
           out entry zones, area-of-effect (AoE) geometric shapes, and
           proximity-based defense scaling parameters.




🎭 2. Narrative, Dialogue, & Scripting Tools
JRPGs are inherently narrative-driven, requiring modular authoring tools for writers and
designers that sit entirely apart from the source code.

   ●​ Visual Node-Based Dialogue Tree Editor:
         ○​ The Tool: A flowchart-style graph interface where nodes represent spoken
            text, choices, and execution blocks.
         ○​ Why it's essential: Dialogue isn't just text; it drives game logic. Wires
            coming off a choice node must easily link to dynamic execution
            commands, such as checking player quest logs, triggering inventory
            updates, or modifying faction affinity variables.
   ●​ Global Event Flag & Quest Lifecycle Tracker:
         ○​ The Tool: A dedicated database interface within the editor tracking
            boolean and integer state flags (e.g., quest_king_saved = true,
            village_destroyed_stage = 2).
         ○​ Why it's essential: The engine needs a unified, searchable registry to
            verify the world's status instantly, enabling conditional rendering of NPCs,
            changing store inventories, or lock/unlock states of world map
            destinations.
<PARSED TEXT FOR PAGE: 8 / 18>
📊 3. Stat Matrices, Itemization, & Progression Tools
RPGs handle vast configurations of calculations, formulas, status effects, and gear
variables.

   ●​ Dynamic Formula Expression Parser:
         ○​ The Tool: A text-based formula validation interface embedded in your item
            and skill editors.
         ○​ Why it's essential: Instead of hardcoding math, designers should be able
            to write expressions directly into the UI (e.g., Damage = (Attacker.STR *
            4) - (Defender.DEF * 2)). The engine can compile these string
            expressions safely at launch using a lightweight script interpreter or Java's
            internal evaluation capabilities.
   ●​ Status Effect & Buff/Debuff Modifier Graph:
         ○​ The Tool: An effects configuration panel where designers build discrete
            logic blocks for combat modifiers.
         ○​ Why it's essential: You need to handle complex, compounding tick-based
            modifiers cleanly (e.g., Poison ticks for 5% max HP at the start of a
            combat phase; Stun bypasses turn allocations for 2 cycles; Shield
            increases base defense value by a scaling scalar).
   ●​ Progression Curve Balancer & Grapher:
         ○​ The Tool: A visual plotting tool that maps level (1 to 99) against core
            attributes (HP, MP, Strength, Agility).
         ○​ Why it's essential: It allows you to instantly visualize stat-scaling curves
            across different character classes or enemy archetypes, catching balance
            anomalies or unintended power spikes mathematically before testing.




🎛️ 4. UI Layout, Menu Nesting, & Input Emulators
Because JRPGs spend a massive percentage of their gameplay loop inside menus,
inventory management, and tactical sub-screens, the UI system requires heavy
infrastructure.

   ●​ Hierarchical Menu & Focus Traversal Designer:
         ○​ The Tool: A visual UI canvas tool that maps out menu nesting structures
            and defines navigation focus links.
         ○​ Why it's essential: In a JRPG, UI must be fully navigable via keyboard or
            gamepad without mouse fallback. This tool lets you explicitly specify
<PARSED TEXT FOR PAGE: 9 / 18>
            where the selection cursor shifts when a player presses
            Up/Down/Left/Right or Cancel/Accept across your inventory, equipment,
            and skill sub-menus.
   ●​ Save State Serialization Sandbox:
        ○​ The Tool: A utility within your developer console layer to test object
            serialization and deserialization.
        ○​ Why it's essential: RPG save files are massive and highly complex,
            tracking every item, player stat, and quest flag. This module lets you test
            save/load state compatibility across different versions of your engine data
            structures instantly without corrupting user profiles.



To round out a comprehensive, multi-genre development ecosystem for Horror,
Walking Simulators, and Theme Park Creators, you should implement handles for a
few highly specific adjacent gameplay styles. These styles provide the mechanical
crossover needed to let creators build deep, emergent experiences (like a simulated
arcade inside a haunted boardwalk, or a mascot horror theme park).

Here are the final core game style handles you should expose in your Java 17 editor:


🪙 1. Retro Arcade, Booth Games, & Mini-Game Nodes
Theme parks and boardwalks rely heavily on small, self-contained interactive stations
(crane games, shooting galleries, fortune tellers, ticket redemption booths).

   ●​ Isolated Input & Display Context Switcher:
         ○​ The Handle: A MiniGameContext hook that temporarily hijacks the
             primary player input controller and diverts it to a localized sub-state
             machine.
         ○​ The Editor Tool: A "Virtual Screen / Cabinet Painter." If a creator builds
             an arcade cabinet or a shooting booth asset, this tool lets them map a
             secondary, discrete 2D grid logic loop directly onto a texture face of that
             3D prop. The player can step up, click the booth, and interact with a retro
             mini-game without unloading the main park layout.



🎭 2. Mascot Horror & Stealth A.I. Archetypes
Popularized by modern horror, this style involves specialized, persistent threats that
stalk the player through large attraction zones or service tunnels.
<PARSED TEXT FOR PAGE: 10 / 18>
   ●​ Sensory Perception & Investigation Map:
        ○​ The Handle: An A.I. sensory module (StealthPerceptionComponent)
           exposing distinct variables for visual cones, acoustic thresholds, and
           flashlight beam detection.
        ○​ The Editor Tool: A "Sightline & Sound-Leak Visualizer." Creators can
           place a roaming mascot enemy in a maze and instantly see lines drawing
           where the monster can spot the player's flashlight glare, or where
           floorboard creaks will draw its investigation vectors. It allows fine-tuning
           stealth-based terror layout design.



🗺️ 3. Metroidvania-Style Progression & Structural Lock-and-Key
Layouts
Walking simulators and horror tracking zones frequently use gating mechanics to guide
the player through explicit paths (e.g., finding a hydraulic valve to open a theme park
ride exit, or locating an old keycard).

   ●​ Spatial Sequence & Gate Dependency Editor:
        ○​ The Handle: A global state validator linked directly to your world entities
            (ProgressionGateComponent).
        ○​ The Editor Tool:​
             A dependency node map. Instead of writing conditional code for every
            single locked door or broken generator, creators visually wire an item
            instance (e.g., Rusty Crowbar) to a gate node (e.g., Subway Maintenance
            Hatch). The engine handles inventory lookup, item consumption, and
            animation state changes automatically upon interaction.



🚨 4. Emergency, Maintenance, & Disaster Management Styles
The macro-level management side of a theme park or haunted attraction needs crisis
elements to stay engaging—power outages, ride breakdowns, or monster containment
breaches.

   ●​ System Failure Grid & Cascade Profiler:
        ○​ The Handle: A resource-routing network interface
           (UtilityGridComponent) that handles flows like electricity, water, or
           hydraulic pressure across park assets.
        ○​ The Editor Tool: A "Breaker & Fuse Box Connector." Inside the park
           creator layer, designers can draw utility wires connecting main generators
<PARSED TEXT FOR PAGE: 11 / 18>
              to specific attraction sectors. If a fuse blows or an entity breaks a power
              line, the engine automatically calculates the downstream failure
              cascade—flickering the sector's localized spot-lights, killing ride physics,
              and plunging that area of the park into pitch-black survival horror
              gameplay.



To cleanly support a City Builder or Macro-Management style game alongside your
existing genre toolsets in Java 17, your engine architecture must shift focus. You are
moving from tracking individual, hyper-detailed entity actions (like in a horror sim) to
tracking massive, aggregate data flows, spatial zone grids, and systemic
resource-networks.

To build a robust, performant management engine, you should implement these specific
modules and system handles in your editor suite:




🗺️ 1. Asynchronous Cell-Grid Layering & Zoning Modules
City builders rely on infinite or highly expansive tile arrays where tiles hold hidden,
compounding datasets (pollution levels, land value, fire risk) rather than just a visual
texture index.

   ●​ Multi-Layer Bitmasked Grid Allocator:
        ○​ The Module: A tool to partition your world into separate, overlapping data
             layers (e.g., Terrain Height, Zoning Layer, Utility Mesh, Environmental
             Layer).
        ○​ The Handle: Expose a Grid Overlay Matrix View in your editor. This
             allows creators to paint structural zone boundaries (Residential,
             Commercial, Industrial) or resource veins (Oil, Water Table). To keep
             calculations lightweight in Java, the backend should use highly packed
             primitive flat-arrays or BitSet objects, letting the simulation look up
             localized cell attributes via fast bitwise operations.
   ●​ Dynamic Influence Spread & Cell-Decay Ticker:
        ○​ The Module: A background execution system that processes
             cellular-automata-style logic passes at regular simulation ticks.
        ○​ The Handle: Provide a Cellular Propagation Profiler. Designers can
             visually configure how specific placed structures radiate statistical values
             outward into nearby cells (e.g., a Police Station asset spreads a circular
<PARSED TEXT FOR PAGE: 12 / 18>
             radius of +20 Safety to its neighbors, while an Industrial Factory slowly
             bleeds a decaying value of +5 Air Pollution downwind).




🚰 2. Linear Utility Network & Resource Flow Routers
Whether routing power lines in a metropolis, conveyor belts in a factory, or pathways in
a theme park, management games require immediate, reactive distribution networks.

   ●​ Directed Graph Utility Grid & Node Linker:
         ○​ The Module: A customized spatial network tool that automatically builds
            structural connections when linear assets (pipes, power lines, roads) are
            placed adjacently.
         ○​ The Handle: Build a system hook class like NetworkNodeComponent that
            maps graph topologies. When a player builds a power plant and connects
            it to a city block via power lines, the engine treats this as a directed tree
            graph, running fast breadth-first or depth-first searches to distribute
            resource packages (MW of power, liters of water) across the network.
   ●​ Throughput & Bottleneck Telemetry Analyzer:
         ○​ The Module: A diagnostic viewport overlay charting transport network
            traffic, line pressure, or grid capacity.
         ○​ The Handle: Provide real-time visualization lines highlighting structural
            network failures—such as a highway color-coding to deep red when transit
            volume hits 90% capacity, or water pressure lines tapering off if the pipe
            diameter is too small to supply downstream nodes.




👥 3. Macro Economy Simulation & Agent Population Engines
A city builder needs to simulate life. You cannot run full behavior trees for 50,000
individual citizens simultaneously without freezing the JVM garbage collector; you need
statistical abstraction.

   ●​ Headless Demographics Simulator & Balance Tester:
<PARSED TEXT FOR PAGE: 13 / 18>
         ○​ The Module: A decoupled, pure-math simulation core that processes
             macroeconomic formulas (Supply/Demand curves, Tax Rate impacts,
             Employment percentages, Inflation tracking).
         ○​ The Handle: A visual graph canvas where designers balance
             macro-progression curves. Similar to your RPG stat-curve balancer, this
             panel lets you mock-run 10 years of a city's economic life in a fraction of a
             second to monitor cash-flow trends, business bankruptcy rates, and tax
             feedback behavior before compiling game builds.
   ●​ Statistical "Ghost Agent" / Statistical Flow Router:
         ○​ The Module: A spatial traffic allocation tool that computes optimal paths
             between residential centers and employment zones.
         ○​ The Handle: Implement a Flow Field Route Pre-Calculator. Instead of
             simulating 1,000 real cars trying to pathfind to a factory, the engine runs a
             global path query once from the residential zone to the workplace zone,
             and then simply draws a rolling particle effect on the roads while adjusting
             the aggregate usage data counters on those road cells.




📊 4. Structural Data Manifests & Save State Marshalling
Management loops generate incredible amounts of changing world state variables that
must be updated instantly and safely saved.

   ●​ Virtual Data-Table Spreadsheet & Registry Sync:
          ○​ The Module: A built-in registry layout tool editing raw structured data
             values (CSV/JSON configurations mapping structural building costs,
             upgrades, maintenance expenses, and population generation metrics).
          ○​ The Handle: A data registry hook allowing runtime modifications to pass
             straight to entity templates. This ensures changes to base stats (like
             reducing the maintenance cost of a wind turbine) update instantly in your
             simulation environment without forcing code modifications or project
             rebuilds.
   ●​ Delta-Compressed Deserialization Sandbox:
          ○​ The Module: A high-speed serialization mechanism customized for saving
             massive grid arrays efficiently.
          ○​ The Handle: Since management save files can easily swell to hundreds of
             megabytes, build a memory-profiling pipeline handle that reads and writes
             only cell differences (deltas) from the default map layout, heavily
<PARSED TEXT FOR PAGE: 14 / 18>
             compressing data buffers via GZIPOutputStream to keep storage prints
             minimal and loading processes snappy.
   ●​



If you want your engine to be a comprehensive games creation platform, you need
handles that can manage dynamic projectile physics, fast movement states, and
automated world-generation layouts.




🏃 1. Sci-Fi Action, Platforming, & Combat Styles
Action-focused games drop the macro abstractions of strategy or puzzle systems and
require precision, low-latency hooks for movement, physical interactions, dodging, and
gunplay.

   ●​ Kinematic Character Controller (KCC) Module:
         ○​ The Handle: A dedicated character movement script
            (KinematicMovementComponent) that operates independently of standard
            rigid-body physics.
         ○​ The Tool Feature: A precision collision-response tuning panel inside your
            editor. Creators need to fine-tune fast movement states like
            variable-height jumping, wall-sliding, air-dashing, and invincibility-frame
            (I-frame) dodging mechanics without clipping through walls or dealing with
            erratic, physics-engine-driven bounciness.
   ●​ Object-Pooling Bullet & Projectile Manager:
         ○​ The Handle: A memory-safe, pre-allocated entity pool structure designed
            for fast recycling.
         ○​ The Tool Feature: A weapon and hitscan/projectile pattern visualizer. If
            creators are designing complex firing patterns, spawning 200 laser
            projectiles a second will trigger Java's garbage collector and freeze the
            application if arrays are constantly allocated and destroyed. The engine
            handles memory footprint reclamation silently by recycling dormant object
            memory blocks.




🌲 2. Survival, Crafting, & Sandbox Styles
<PARSED TEXT FOR PAGE: 15 / 18>
Survival games change the gameplay loop to focus entirely on physical inventory
storage, world destruction/modification, and dynamic structural building.

   ●​ Modular Inventory & Container Synchronization Engine:
        ○​ The Handle: A thread-safe data synchronization hook
           (InventoryContainerComponent) that decouples item data from UI
           rendering.
        ○​ The Tool Feature: A container layout editor. This lets creators build
           anything from personal player inventory grids to world containers (like
           chests, resource nodes, or crafting benches) with built-in networking
           handles so storage updates sync instantly across multiplayer sessions
           without item duplication exploits.
   ●​ Dynamic Structure Placement & Snapping Matrix:
        ○​ The Handle: A runtime positioning matrix that validates structural stability,
           terrain clipping, and player proximity when placing pieces in the world.
        ○​ The Tool Feature: A building-piece configuration panel where creators
           can design modular base-building components (walls, floors, stairs) and
           assign specific "snap points" (bounding box vectors) directly onto the 3D
           meshes for frictionless player building.




🎴 3. Roguelike & Procedural Generation Styles
Roguelikes and dungeon crawlers shift structural design away from manually sculpted
editor maps into randomized, algorithmically generated play spaces.

   ●​ Procedural Rule-Set & Level Generation Baker:
         ○​ The Handle: A seed-driven random number generation pipeline coupled
            to modular cell layout structures.
         ○​ The Tool Feature: A node-based map generation rule builder. Instead of
            sculpting a room, creators define structural rules (e.g., use a Cellular
            Automata or Binary Space Partitioning algorithm to generate rooms,
            connect them with corridors, and enforce that an exit door must spawn at
            the maximum possible distance from the entry coordinates).
   ●​ Run-Based Dynamic Modifier & Synergy Database:
         ○​ The Handle: A modular reward and item-effect modifier interface that
            wraps existing entity attributes.
         ○​ The Tool Feature: A synergy logic table. When players pick up stacked or
            random items during a run, this subsystem parses operational logic to
<PARSED TEXT FOR PAGE: 16 / 18>
              smoothly handle compounding status effects (e.g., transforming a basic
              projectile into an exploding, ice-elemental split shot) without forcing
              creators to write hardcoded exceptions for every possible combination.



Based on the extensive ecosystem of genres you want your Java 17 engine to support,
the single most critical module that should be included natively by default in your editor
is the Centralized Entity Registry & Virtual Database (Data-Tables) module.

This module acts as the universal foundational layer that underpins every single genre
you have mentioned:

   ●​ Turn-Based RPGs/JRPGs: Manages stat scaling, item itemization, status
      effects, and dialogue tree variables.
   ●​ Real-Time & Turn-Based Strategy: Controls unit stats, costs, construction
      times, and tech-tree unlock dependencies.
   ●​ Match-Three Puzzles: Dictates tile types, match scoring structures, and
      cascade logic rules.
   ●​ Horror Walking Simulators: Tracks global event flags, quest lifecycles, and
      environmental gaslighting triggers.
   ●​ City Builders & Management Games: Pools macro-economic variables, grid
      occupancy attributes, and zoning coefficients.
   ●​ Action/Platformers & Survival: Holds item stack sizes, crafting recipes,
      weapon damage data, and projectile properties.


Why this must be the Default Core Module
In a multi-genre engine, you cannot afford to hardcode game data into your Java
classes, or you will constantly break compatibility across different project types.

By building a robust, visual Data-Table Spreadsheet Editor into your engine's default
workspace wrapper, you achieve absolute separation of data and logic. Designers can
create custom templates, modify values on the fly, and live-reload spreadsheets
(parsing to clean JSON, CSV, or packed binary) directly within the editor layout without
needing a code recompilation.




Architectural Layout: Default Core Registry Setup
To make this registry highly performant and flexible in Java 17, your default module
architecture should use a decoupled Registry Schema Definition pattern:
<PARSED TEXT FOR PAGE: 17 / 18>
  None‎
  [ Your Java 17 Core Engine Editor Application Wrapper ]
                │
                ├──► [ Visual Data-Table Editor Tab ] (Default Module View)
                │            │
                │            ▼
                │     [ Raw Configuration Data ] (JSON / CSV / Manifest
  Binary)
                │            │
                │            ▼
                └──► [ Dynamic Schema Schema Engine ]
                             │
                             ├──► Archetype Template (e.g., RPG_Hero_Class,
  Strategy_Unit, Puzzle_Tile)
                             └──► Component Field Definitions (Primitive
  Strings, Ints, Floats, Bitmasks)



Code Implementation Blueprint

You can implement this registry cleanly using Java 17's Records for immutable data
rows, structured tightly within a memory-efficient map pattern. This structure ensures
that any subsystem can request an entity configuration archetype instantaneously:

java
  None‎
  import java.util.Map;
  import java.util.concurrent.ConcurrentHashMap;


  /**
   * Core configuration registry built natively into the engine's
  default module layer.
   * This holds the static data archetypes parsed from the editor
  data-tables.
   */
  public final class GameDataRegistry {


          // Nested schema record handling primitive attributes across
  genres
<PARSED TEXT FOR PAGE: 18 / 18>
        public record EntityArchetype(
               String archetypeId,
               String category, // e.g., "RPG_HERO", "RTS_UNIT",
  "PUZZLE_TILE"
               Map<String, Double> numericalModifiers, // e.g., "health" ->
  100.0, "cost" -> 50.0
               Map<String, String> stateFlags // e.g., "spawn_trigger" ->
  "on_click"
        ) {}


        private final Map<String, EntityArchetype> registryMap = new
  ConcurrentHashMap<>();


        /**
          * Registers a new data configuration row compiled by the editor
  spreadsheet.
          */
        public void registerArchetype(EntityArchetype archetype) {
               if (archetype == null || archetype.archetypeId() == null) {
                    throw new IllegalArgumentException("Archetype data cannot
  be null.");
               }
               registryMap.put(archetype.archetypeId(), archetype);
        }


        /**
          * Fetches a configuration template instantly for runtime
  instantiation.
          */
        public EntityArchetype getArchetype(String archetypeId) {
               return registryMap.get(archetypeId);
        }


        public void clearRegistry() {
               registryMap.clear();
        }
  }

Use code with caution.



---

## Part IV — Earlier PSX-era editor recommendation and creation guides

These sources consolidate the previous nine-PDF PSX intake and the separate owner-written menu/campaign/billboards brief. PDFs not currently present as binaries are represented here by the full existing source-informed guides, **not** by invented PDF transcriptions. Where multiple guides overlap, source distinctions remain intact.


---

### Source: Gold standards, level building, PSX lighting, UI and asset editor engineering guide

**Original repository path:** `docs/wiki/psx-editor-engineering-guide.md`.

# PSX-inspired editor and runtime — curated engineering reference

**Adoption review:** 2026-10-30 · **Scope:** Beckenburg Java 17 runtime and the portable editor kit · **Status:** normative design guidance, **not** a claim that all requested tools are implemented.

This guide adapts five owner-provided PDFs selectively. It is not an instruction to emulate an actual PlayStation's fragile hardware. The editor must author **the actual running game**, not a decorative second renderer. Beckenburg gameplay, authoritative networking and accessibility take precedence over historical fidelity.

## Source register and adoption verdict

| Owner reference | Where the advice belongs | Verdict |
| --- | --- | --- |
| *Level Editor gold standards* (5 pages) | Editor core, scene, inspector, persistence, UX, diagnostics | **Adopt as feature acceptance criteria**, phased; existing [editor audit](../development/LEVEL_EDITOR_OVERHAUL_AUDIT.md) governs current completion claims |
| *PSX level building* (18 pages) | Materials/presentation profiles, geometry authoring, layouts, portals and atmosphere | **Adopt selectively**; never intentionally break collision, input, readability or streamed-world integrity |
| *PSX lighting engine* (6 pages) | Unified lighting module plus renderer post-process; editor gets adapters | **Adapt existing modules**, avoid a duplicate lighting engine; see [lighting review](../lighting/LIGHTING_MODULE.md) |
| *PSX NPC-AI-1* (17 pages) | Authored navigation, portal metadata, AI diagnostics and fair simulation | **Adapt with constraints**; see [source-level AI review](../ai/PSX_NPC_AI_ADOPTION.md) |
| *PSX UI guide and user* (20 pages) | Input abstraction, compact UI language, controller navigation, accessibility | **Adopt usable interaction rules; historic presentation optional**, never insert intentional latency or invented memory-card requirements |

These are owner-supplied design references, not independent historical or benchmark verification. Recommendations labeled as PSX hardware facts in them should be independently checked before being repeated as hardware-engineering claims. The editor is Java 17 with a software renderer and optional GPU backend, **not** native PS1 C/MIPS.

## Cross-cutting non-negotiables

1. **Game/runtime first, editor second.** A single authoritative scene model drives renderer, collision, physics, navigation, audio, evidence, ghost powers, van, ritual zones and environment modules. The editor is an adapter/view/controller onto that same model. Preview, playtest and shipped scenes must use identical actual assets, fixtures and spatial coordinates.
2. **Stable identity and versionable state.** Object, scene, room, portal, layer and prefab identities remain stable through edits, save/load, Undo/Redo, import and playtest restoration. Save atomically; reject invalid references without silently converting live game state.
3. **Parity and fairness outrank retro tricks.** The host owns ghost/hunter state and evidence. An AI ghost and player ghost operate under the same permitted sensory inputs, movement restrictions, ability costs and timers. No seeing through walls, van entry, hiding-pocket entry or omniscient aim shortcuts.
4. **Presentation knobs cannot become gameplay bypasses.** Resolution, quantization, color depth, dither, affine mapping, jitter, palette treatment, distance fog and lighting visual profiles must never change line-of-sight, evidence visibility guarantees, collision, locked doors or network outcomes. Exclude Swing inspector text/gizmos from global PSX warp.
5. **Performance must be observed.** Show editor FPS and p50/p95/p99 frame timings, CPU build/raster/post-process, GPU uploads/readback where real, AI budget, draw calls/triangles, source probes, dirty lighting cells, heap and GC. Update dirty regions, avoid unnecessary full-scene invalidation. Optional limits warn; only objective nonworking game/installer/multiplayer requirements block release.
6. **No inert authoring controls.** Every exposed slider, toggle, inspector, shortcut, prefab, brush, button or view must affect the live authored scene or actual runtime. Hide or mark unsupported facilities as unavailable rather than rendering decorative controls.

## Level construction: rules to implement

**Authoring structure.** Grid/vertex/edge/surface/object snapping, true XYZ transform and parent hierarchy, separate visual/collision/navigation/trigger layers, selected-object inspector, material alignment and reusable prefabs. A floor over a basement must have an actual stair/shaft opening and collision route. Houses must remain recognizable buildings rather than arbitrary untextured cubes; avoid clipped shelving, wall lamps and furniture. The **same canonical van prefab** serves castle and mission. Exterior terrain, roofs and walls must be compatible with world topology and weather.

**Visibility and staging.** Room sectors, door/portal topology, occlusion/interest queries and selective asset loading can keep the game fast. Camera framing, threshold composition, distance fog, vertical loops, readable landmark texture contrast, audio landmarks and paced negative space are useful design tools. Fog is owned by the unified weather module—do not restore a separate “fog / outage / weather” mission-condition selector. Never unload a room by freezing gameplay just to imitate CD-ROM seeks, reveal unbuilt voids, or force fixed cameras on a first-person game.

**Retro materials / rendering.** Prefer compact hand-authored diffuse textures, nearest-neighbor/pixelated profiles, intentionally limited polygon density, authored color gradients, optional 4x4 dithering/quantization, optional PSX vertex/UV jitter and affine distortion. Treat 64/128/256 px textures, 320x240 and 4:3 as **art-direction presets**, not universal hard caps; modern widescreen and scalable accessible text must remain supported. A visual seam/wobble is okay only as a bounded rendering effect; structural gaps, physics tunneling, geometry bleed, unreadable text, and real world/skybox holes are defects.

**Tools and visual diagnostics.** Add portal/room/PVS visualizers, collision/navigation/trigger overlays, light reach with actual occluders, texture seam and wall-penetration checks, basements and vertical passage QA, item/van clearance and jump-to-object warnings. Collision geometry may deliberately be simpler than visible geometry, but it must remain aligned with actual playable floors/walls.

## Lighting: existing authority and editor adaptation

The **existing** `dev.mrcalzon.lighting.LightingModule`, `FixtureLighting`, `SceneRenderer` and `PsxPipeline` remain responsible. Add authored fixture inspectors, radius/diffusion reach overlays, occlusion probes, power-circuit state, scene preview and measured cache invalidation through published APIs; do not introduce a rival shader/CPU light system. Existing diffusion width and invalidation-cell size are distinct controls, not a “priority” winner for lights in one cell. Source contributions must be occlusion-aware; self-emission must not illuminate the far side of a masonry wall. The PDF's per-pixel fallback, optional CLUTs and mesh subdivision are candidates **only if** supported by renderer and a measurable, justified need. Do not blindly subdivide every surface, add redundant post-process passes or recalculate a whole House after a door opens. Benchmark the real door profile and review p95/p99, rays, upload bytes, cache revisions and visual evidence.

## Navigation and AI: safe adoption

The editor should expose real traversable room/portal and stair connections, authored waypoint/cover/ambush metadata, safe door-state edge invalidation, route reachability diagnostics and optional offline PVS/next-hop preprocessing. These are **authoring accelerators**, not replacement runtime rules.

Keep Beckenburg's host-owned 100 ms simulation, audible-event ghost perception, door/material/weather sound attenuation, swept collisions, stable ghost action cadence, van exclusion and hiding rules. Dynamic topology updates must be reflected in valid routes. AI may cache results, throttle optional decisions and avoid excess allocations **when profiling demonstrates benefit**; hearing, contact, rituals, essential reactions and authoritative events cannot be throttled out. No 4-edge artificial node cap, global teleport-on-stuck, PS1 2 MB memory ledger, GTE/MIPS assembly, universal no-allocation ban, fake ghost gaze detection or NPC omniscience. Measure before introducing object pools, time slicing or path queues.

## UI and input: PSX visual language, modern usability

Use compact rectangular panels, sharp optional pixel fonts, bounded dithered backplates, visible selection, controller D-pad navigation, predictable back/escape, mouse/keyboard/controller parity, configurable bindings/deadzones and instantaneous functional response. Maintain scroll and page-jump, text-skip plus history, subtitle/audio cue alternatives, adjustable UI scale/contrast and inclusive readibility. The low-res 4:3 mockup is a **preset**, not a lock on desktop aspect ratio, internationalization or accessibility. No fake CD seek freeze, mandatory button delay, unreadable UI at 240p, forced memory-card ritual, impossible 1 MB VRAM cap, or nonremappable input. The editor UI itself should stay legible at modern resolution, even when the 3D viewport imitates PSX rendering.

## Editor implementation acceptance map

| Level | Deliverable | Evidence required |
| --- | --- | --- |
| P0 | Runtime parity, live scene/collision, stable scene IDs, atomic save and safe edit/playtest snapshot | Editor and game read same scene; round-trip load; unsaved edits survive playtest |
| P1 | True gizmos and snapping, hierarchy/layers, typed inspectors, authored portal/navigation/lighting helpers | Real placement and doorway/stair test, inspector update in rendered world |
| P2 | On-demand frame/AI/lighting profilers, collision/occlusion debug views, prefab and replacement tooling | Door-opening p95/p99, no full redraw, vanished assets identified |
| P3 | Advanced brushes, macro API, docking, multi-user locking, optional player sharing | Live functionality; no inert controls or untrusted arbitrary script execution |

**Concrete fixtures:** House door opening; House wall-clipping furniture; all cellar entrances; Castle wall-mounted lamps and rear shelves; Castle/field van identity; dense weather fog; ghost sensing and hiding/van exclusions; UI navigation under mouse, keyboard and controller. Warnings and subjective quality checks remain diagnostic unless they demonstrate an objectively nonfunctional product.

## Maintenance, machine access and publication

- Machine catalog: [PSX authoring adoption JSON](../development/psx-editor-reference.json), with stable IDs, applicability classes, source page references, integration owner and acceptance proof. This is **a guidance manifest**, not evidence of runtime feature completion.
- Existing detailed authorities: [Editor audit](../development/LEVEL_EDITOR_OVERHAUL_AUDIT.md), [suite architecture](../development/PSX_RUNTIME_AND_EDITOR_SUITE_ARCHITECTURE.md), [lighting module](../lighting/LIGHTING_MODULE.md), [AI adoption review](../ai/PSX_NPC_AI_ADOPTION.md).
- The standalone Java 17 editor kit includes a concise offline guide viewer. The wiki and manifest are versioned sources; the viewer is an orientation aid, not the implementation authority.
- For the two sites, publish the same policy with site-relative links. Original PDFs remain owner-supplied source material; these pages are an **adapted synthesis** rather than a verbatim republication.
- Actual installed Windows playtest, network/camera and hardware GPU profiling remain separate verifications. Do not equate a successful documentation commit with those tests.

## Current editor-facing adoption checkpoint (2026-10-30)

- The **PSX GUIDE** toolbar action opens a bundled offline `EditorAuthoringReference` viewer and links to the full Markdown/JSON authorities; it is available in the portable editor JAR.
- The **SCENE AUDIT** toolbar action is Castle-specific: real `CastleLevelEdits` wall collision checks for possibly embedded props/lights, globally duplicate authoring IDs, spawn wall/nav blockers and a jump-to-object review. It is manually invoked, not a per-frame profiler or full geometric mesh-intersection solver.
- `CastleEditorAuditTest` is a non-blocking focused-workflow diagnostic. A successful source commit does not prove the full Windows installer and interactive window acceptance.
- General-purpose prefab placement, PVS export, typed cross-game inspector, live one-button playtest and scene-format parity remain roadmap work, **not** delivered by these documentation and initial audit changes.



---

### Source: PSX asset import, preparation and regression

**Original repository path:** `docs/wiki/psx-asset-import-and-regression.md`.

# PSX Asset Import, Reduction and Over-Regression

**Authority:** owner-supplied *Developing assets for the PlayStation 1 (PSX) architecture.pdf* (13 pages). This guide is a curated engineering adaptation for Beckenburg's Java 17 **PSX-style modern runtime**, not a claim that the game runs on original PS1 hardware. **Implementation checkpoint:** 2026-10-30. [Architecture](../development/PSX_RUNTIME_AND_EDITOR_SUITE_ARCHITECTURE.md) · [Machine rules](../development/psx-asset-import-reference.json).

## Core import workflow
1. Open the developer level editor and select **IMPORT MODEL** from the toolbar or Assets dock.
2. Choose **OBJ**, **STL** (ASCII/binary), or glTF 2.0 **GLB** with embedded PNG textures. These are the **implemented readers**. A text **.gltf**, FBX, DAE/Collada, PLY, USD/USDZ and Blender project files are **not** silently accepted as working native readers.
3. Preview the **source** and **reduced derivative** side by side with live triangle and named-part counts. Choose an advisory asset budget and geometry regression level 0–12. Level 0 retains geometry and an original GLB byte-for-byte; increased levels use deterministic spatial vertex clustering.
4. Review explicit warnings for excess source size, estimated CPU triangle memory, large decoded texture memory, vanishing parts and style-budget excess. Preset budgets are **advisory**, never game-release gates.
5. **OVER-REGRESSION** identifies a named OBJ group/part that loses nearly all its faces or unique positions. A head, arm, wheel or separate prop section may become a flat remnant or disappear entirely. If all drawable triangles collapse, **Publish** is disabled; partial collapse requires an explicit confirm. The source remains untouched.
6. **Publish** writes a bounded GLB derivative and original input to the user's own `~/.beckenburg/editor/imports` library, plus source hash, sizes, regression level and warnings. It uses the real `GlbProps.decode` before registration; the game/editor references the same `import_<hash>` ID in scene saves. Imported items appear under **Models / Imported**. Place using the normal PROP tool and save the Castle scene.

**Important limitation:** reduction currently clusters vertices; it is not curvature-aware, bone-aware, silhouette-aware or a production quadric-error-metric decimator. OBJ `o`/`g` names preserve analysis boundaries; the current GLB decoder flattens named mesh-node identity to one `GLB assembly` part for reduction analysis. A GLB imported at level 0 retains its original embedded textures. When reducing a GLB, the generated derivative currently uses an explicit checker fallback atlas and **does not retain original material/rig/named-node identity**; the UI warns and requires confirmation. OBJ material libraries/MTL links are not yet converted; STL has no material UVs. Neither format conversion nor LOD conversion grants a source model redistribution license. Source-copy policy is user-local by default, not a global repository upload.

## PSX source rules — selective adoption

| PDF section/pages | Adapt into modern editor | Do **not** import as literal runtime defect |
| --- | --- | --- |
| Core do/don't, pages 1–2 | Warnings for small texture atlases, palette/nearest-neighbor style, polygon/VRAM estimates, affine-warp simulation as an optional rendering profile | Force all art to 16–256px, remove modern depth testing, break textures, or require integer-space physics |
| Puppet characters, pages 2–3 | Preserve named head/arms/torso parts, origins, joint clearances, distinct facial/texture variants and multiple authoring LODs | Flatten animation rigs or destroy weights automatically during import |
| Vehicles, page 4 and page 9 | Keep roof/hood panels and independent wheels visually distinct; caution against over-aggressive reduction | Replace 3D wheels with 2D sprites as a required rule, or deliberately make van models look like cardboard |
| Visibility/LODs, pages 6–7 and 10–11 | Separate near/far variants; protect important silhouette edges. Far LOD can be cheap; near LOD must remain readable | Assume a single camera or hardcode historical PS1 draw-distance limits across Beckenburg |
| Genre assets, pages 7–10 | Distinguish weapons/pickups from architectural props; preserve gameplay-significant contours and mechanical attachment points | Automatically collapse first-person weapons to single planes regardless of functionality |
| Textures/lighting, pages 11–12 | Optional palettization, quantization, nearest-neighbor and baked-detail authoring in harmony with unified LightingModule | Make the game unlit or bypass authoritative dynamic occlusion/flashlights |
| Architecture, page 13 | Room/portal visibility cells, modular repeated surfaces and useful art-direction hints | Reinstate fog-as-mission-condition selector or artificially clip collision to mimic hardware |

The PDF's representative budgets are **style references**, not universal targets: under ~100 triangles for simple props; ~300–800 for characters; up to ~1,200 for particular close-up fighters. Level-of-detail variants can legitimately be far smaller. Other PSX source claims are historically framed and must be checked against actual modern rendering behavior before engineering decisions. We maintain collision fidelity, depth handling, memory safety, functionality, and accessibility.

## Safety and budget contracts
- Read only finite geometry from files of at most 32 MiB; cap raw triangle count (120,000). Use non-blocking SwingWorker ingestion and regression to avoid freezing the game editor.
- Export currently supports at most 16,000 triangle-soup triangles per model and 4,000,000 bytes per GLB, reflecting **the current real game decoder**, rather than arbitrary nominal PS1 budgets.
- Estimate CPU triangle storage separately from actual source file size and decoded image texture bytes; do not claim these are exact GPU/heap allocations. Profiler instrumentation is a separate next step.
- Preserve the source original and SHA-256 alongside output, with atomic file replacement, safe stable IDs, bounded local library scanning, and no executable authoring tool dependency.
- Don't fetch arbitrary remote URLs inside the importer, execute scripts embedded in source data, or treat source texture/asset licensing as automatic.
- Source and optimized mesh are compared in the UI; a regression warning is **editor diagnostic**, not a CI release blocker.
- Require a pre-publish real decoder check and a saved-level round trip. A successful build proves compilation, not visual parity or game balance.

## Implementation and verified-vs-planned scope

**Implemented source paths:** `editor/EditorMeshPipeline`, `editor/EditorGlbWriter`, `AssetImportDialog`, `ImportedAssetLibrary`, `GlbProps`, `CastleLevelEdits`, `CastleEditorProject`, `EditorPalettePanel`. Automated tests: `editor/EditorMeshPipelineTest` (portable), `AssetImportRuntimeTest` (native GLB decoder and persistent placement). Check the matching Actions jobs for actual outcome; do not infer success merely from an implementation commit.

**Next import upgrades:** native JSON glTF 2.0 + external buffer/image resolver constrained to an import sandbox; PLY support; material/texture MTL conversion; original GLB node/group-aware reduction; part locks and important-bone/socket protection; UV island/material seam preservation; silhouette/normal/rig integrity tests; faster quadric-error simplification; multiple LOD exports and draw-distance preview; texture quantization and atlas bake with exact estimated texture heap; automatic metadata/permissions provenance; asset repair and unreferenced cache cleanup. **Original high-resolution assets and approved low-poly sibling exports remain separate artifacts under the established asset archiving policy**, with their own verified storage destination.

**Release rule:** only objective build, launch, essential function and required multiplayer failures can block delivery. Model style, polygon count and over-regression thresholds are authoring warnings; the editor may disallow exporting an actually empty GLB because the current runtime cannot render it.



---

### Source: Animation authoring from PSX animations gold-1.pdf

**Original repository path:** `docs/wiki/psx-animation-authoring.md`.

# PSX Animation Authoring — Curated Engineering Reference

**Source:** owner-supplied **PSX animations gold-1.pdf**, 8 pages, SHA-256 `e23ca682ac405dec241b256aabf484ce541554aac56065f733749e7895a549e7`. **Status:** selectively adopted for Beckenburg's Java 17 PSX-styled runtime and portable editor kit. [Machine source/disposition ledger](../development/psx-reference-ingestion-2026-10-30.json). This is a source-informed reference, not a promise that all historical PlayStation techniques are in game.

## What has actually been wired
**ANIMATE** on the Beckenburg developer editor toolbar opens the portable `EditorAnimationLab`. `EditorAnimationClip` provides named, independent rigid-part pose tracks, 15/30 FPS keyframe timelines, stepped and optional linear authoring previews, a distinct root-displacement track, bounded 8,192-key clips, first/last-preserving pruning of redundant stepped poses, and an atomic, diffable `.anim` document. A local XY puppet preview and key editing afford direct authoring without installing Blender. Automated `EditorAnimationClipTest` covers playback, key pruning, root-motion separation, format round-trip and bounds.

**Scope boundary:** the editor lab does not yet animate an actor in gameplay, import Blender/FBX/glTF animations, drive the ghost controller, bind collision boxes, alter multiplayer authority, or produce a compressed runtime binary. An authored file is a *clip source* only; those integrations need real adapters and test cases.

## Selective adoption, with source pages

| Source pages | Guidance to adopt | Implementation and caveat |
| --- | --- | --- |
| 1 | Rigid segmented mesh hierarchy vs vertex-keyframe morphing; independent parts; keyframe reduction | Rigid-part named pose authoring exists; morph vertex streams and joint hierarchy parenting are planned |
| 1–2 | Low-resolution preview, vertex snap, cycle/memory estimates | Existing game renderer offers retro profile; animation lab has a 2D projection, not true render-parity 3D animation preview. Profiling integration planned |
| 2 | Hierarchical transform reuse, offline constraint bake, interleaved clip data | Architecture targets; no claims of full hierarchy or pre-bake yet |
| 2–3 | 15/30 Hz sampling, separate locomotion root from local mesh motion, UV swaps | Implemented discrete sampling and isolated root track; UV-animated faces planned |
| 3–4 | Readable extreme key poses, optional stepped interpolation, one-frame impact, texture palette | Recommended **style profile**; not mandatory for every game, animation, cutscene or accessibility mode |
| 4–5 | Detect disconnected joint gaps, broken limb pivots, atlas bleeding, excessive key density | Diagnostics/rig structural tests planned; do not turn intentional visual tricks into broken collision or invisible entities |
| 5–7 | Dialogue gesture injection, optional portrait mouth swaps, hit-stall poses, genre-specific locomotion | Game-specific authoring templates planned; Beckenburg ghosts must remain host-controlled and telegraph abilities fairly |
| 7–8 | Track camera shots separately; avoid decoupled camera/actor poses | Camera track support planned; only apply deliberate low-frame styling where legible and comfortable |

## High-priority next acceptance tasks
1. Register actual rigid-part hierarchy and pivot validation (parent-loop rejection, joint-gap preview, protected head/hand/feet sockets). Keep scene collision independent from visual animation.
2. Add true 3D pose preview with the **same** renderer, materials and lighting as gameplay; bind imported GLB submesh identifiers without destructive conversion. The current GLB geometry-regression importer loses part identities on reduction: fix before moving character rigs across.
3. Add keyframe timeline channels for UV/texture swap, event markers and telegraphed strike moments. Never let an animator grant visibility or a strike independent of game rules.
4. Add an optional, non-destructive fixed-point/15-bit/nearest-neighbor visual profile, not forced true-PS1 CPU restrictions. Profile clip memory/CPU and warn without blocking releases for stylistic issues.
5. Define `AnimationRuntimeAdapter` to map validated clips to existing `LanGhostAnimation` phase messages, preserving host authorization and preexisting visibility rules. Test solo/LAN parity **before** calling runtime integration complete.

## Rejected hardware-era mandates
The document presents historical PS1 limitations as absolutes. Beckenburg is **not** a native PS1 executable: do not require fixed-point GTE, intentional input lag, broken Z behavior, disabled modern collision, disabled dynamic lighting, visible limb gaps, floor clipping, 320×240-only readable UI, or obsolete RAM caps. The source itself contains inconsistent Z-buffer claims; preserve its art-direction idea rather than treating that as a runtime specification. A true retro look should remain a chosen rendering/animation profile, without making UI text, navigation, tools, or ghost fairness worse.

**Release rule:** visual style, clip density and silhouette differences are *diagnostic*. Only game functionality/build/launch/essential multiplayer breakage may block a release.



---

### Source: PSX Arrow sound-design source and adopted practices

**Original repository path:** `docs/wiki/psx-arrow-audio-design.md`.

# PSX Arrow Audio — Reusable Retro Sound-Design Reference

**Source:** owner-supplied **🏹 Sound Design_ The PSX _Arrow_ Aesthetic.pdf**, 9 pages, SHA-256 `a3bc12a57828c9ceecf4728f3adc8ba2b0c52051c41f92b298fc90acf24d55a0`. The document describes **arrow/projectile sounds**. Beckenburg does **not** acquire a bow weapon merely by absorbing this source. Generalize reusable sound design cautiously: a short ghost/tool/interaction sound may use some of the same synthesis/voice-budget techniques, but arrow gameplay is neither requested nor implemented by this reference.

## Source content and relevance
- **Pages 1–2:** PS1 SPU-era sample/voice budgets, VAG ADPCM, 11,025/22,050 Hz mono examples and a synthetic/filtered whoosh plus dry wooden impact transient. Use as **historical reference and optional retro-audio preset**, not a hard 512KB/24-voice limit in modern Java.
- **Pages 2–3:** envelope/pitch bend for perceived flight speed, impact body and separate shared reverb. Useful for an *asset-authoring preview* or a scoped arrow/throwable sound family in games that actually contain projectiles.
- **Pages 3–5:** ADSR envelope, pitch modulation, shared reverb send, voice stealing/priority, panning and sample-cache accounting. Adopt bounded queues and categories only where measured overload/voice masking exists; preserve critical proximity/evidence cues.
- **Pages 6–9:** the guide proposes dry source assets, optional DSP bitcrushing, modern procedural synthesis, reverb bus and a nominal four-voice projectile pool. Treat these as **creative presets**; they are not verified engine APIs nor universal acoustics rules. Its “zero RAM” description of procedural audio is figurative, not a literal memory-allocation guarantee.

## Existing Beckenburg ownership
`SoundVolumeEditor` already supports spatial volumes, falloff, pitch, gain, fade, loop and local audition. `SoundVolumeAudio`, `RecordedSfx`, `GameSfx`, `LanSound` and the audit documents under `docs/audio/` own audible playback/coverage. **Do not spawn another competing sound engine** from this PDF or erase real-source/provenance checks. The source does not prove that a new sound asset exists or that an arrow projectile should be added to the game.

## Proposed authoring-only PSX audio profile
A reusable audio-asset pipeline may support original WAV import, mono/downsampled *derivatives*, sample length/decoded-byte estimates, per-category voice priority, stepped pitch envelopes, 1–2 short transient layers, dry/wet audition and room effect routing. Preserve full-quality sources and metadata and leave voice cues/evidence audibility intact. Use opt-in 11.025/22.05kHz/8–16-bit export profiles; default gameplay should prefer **audible clarity**, not gratuitous clipping, distortion or physical-media wait screens. Any import must validate licensing, file size, sampling format, clipping and origin and must never execute embedded data.

## Specific adoption decisions

| Item | Decision | Rationale |
| --- | --- | --- |
| Short dry sounds, compressed derivatives, source-preserving import | **Adopt where applicable** | Small files, predictable loading and mix clarity |
| Separate whoosh/transient/impact and pitch variation | **Optional per game sound** | Relevant to arrows/projectiles; not required for EMF detector, UI or spirits |
| Authoring a shared reverb send and material/room presets | **Adapt to existing SoundVolume module** | One real audio authority with credible positional attenuation and material/door/weather differences |
| Voice priority and limit diagnostics | **Adopt with profiling** | Prioritize UI and gameplay-critical audio; no invented PS1 hard cap |
| Mono/subtitle/visual cues for essential evidence | **Mandatory accessibility consideration** | Retro aesthetics cannot deny players needed sensory information |
| Native VAG/XA conversion, exact PS1 SPU RAM maps, fixed 24 voices | **Reject as compulsory** | We ship Java/Windows, not Sony PS1 hardware |
| Intentionally broken clicks, hard cuts, 1-second stalls | **Reject by default** | Avoid unnecessary audio defects and user-facing latency |

## Acceptance
An authored asset must be playable in the same audio routing as the game before it is marked integrated. Test at least three listening conditions: close/front, behind walls/door, and from the van; use existing simulation/event sound semantics and don't render remote camera/audio streams when no listener is consuming them. Profile concurrent sound load. Audio style/quality diagnostics are advisory; build/launch and genuine missing **essential** audio behavior may block a release when functionality truly fails.

## Current authoring implementation (2026-10-30)

The editor's **SFX ASSETS** action opens `EditorSoundLab`, backed by portable `EditorSoundProfile`. It inspects bounded PCM WAV source length, channel count, sample rate, file bytes and decoded PCM bytes; it can export a **separate** mono 11,025 Hz or 22,050 Hz 8-/16-bit WAV derivative while preserving the original. `EditorSoundProfileTest` exercises stereo-to-mono conversion, valid WAV re-decode, source preservation and format rejection. This is an **audio-source preparation tool**, not a connected gameplay event, projectile weapon, new mixer or SPU emulator. Those distinctions are intentional.



---

### Source: PSX-era level editor: event graphs, campaign flow, authoring

**Original repository path:** `docs/wiki/psx-era-editor-campaign-tools.md`.

# PSX-Era Level Editor — Campaign, Events & Authoring Contracts

**Source:** owner-supplied **PSX-era level editor.pdf**, 9 pages, SHA-256 `dc3bbd98dd74f573aeaf3b63578ebdb52def99f00adf50f37a2c835687a950a3`. **Disposition:** selectively adopt the *editor workflow and graph discipline* for a modern Java 17 game, not raw-PS1 disc tools. [Machine batch ledger](../development/psx-reference-ingestion-2026-10-30.json).

## Source-to-Beckenburg reconciliation

| Source section/pages | Correct placement in our toolchain | Status |
| --- | --- | --- |
| Spawning, page 1 | Real entity authoring with estimated visible draw/LOD rings, per-room active AI counts, editor-only budget warnings, direction arrows, location/floor validity | **Partially existing**: current scene/prop/light tools, overlay/navigation audit and model-size warnings; dedicated spawn gizmo and active-AI-budget profiler **planned** |
| Campaign graph/flags, pages 1–2 | Stable map entrance/exit IDs, directed level transitions, typed persistent campaign flags and stage-preview overrides; explicit persistence ownership | **Planned**, do not fake a finished campaign graph or checkpoint engine |
| UI flow, page 2 | Optional UI menu state graph, focus/escape recovery, string localization IDs and Z-layer editing; test all branches | **Planned**, integrate shared UI rather than spawning a second menu implementation |
| Saves, pages 2–3 | Atomic saves, versioned schema/migrations, corruption recovery and test fixtures for out-of-space/unavailable storage | **Atomic castle editor saves exist**; PS1 card blocks/emulated physical cards **rejected as required platform function** |
| Triggers/scripts, page 3 | Visible AABB trigger volumes, typed event ports and safe template actions; runtime execution trace and no arbitrary scripts from untrusted user levels | **Planned**, match the existing game event/ritual/door APIs instead of forked trigger simulation |
| Team source control, page 4 | Sub-level separation, explicit ownership and versioned/diffable scene documents | **Partially existing** editor snapshots, existing props/surface documents; source-control UX planned |
| Textures/VRAM, pages 4–5 | Material atlas inspection, memory-use estimates, palette variants and warnings | **Partly existing** via GLB importer and active game texture atlas. Literal PS1 T-pages and 512KB SPU management are optional simulations, **not hard limits** |
| Audio, page 5 | Sound source and reverb-zone controls, directional falloff, preview, data-driven event bindings | **Existing** `SoundVolumeEditor`, `SoundVolumeAudio`, `SoundDocument` with placement/falloff preview. Native XA/VAG conversion and SPU emulation **not planned for a Java 17 game** |
| Collision, pages 5–6 | Colliders separate from visual LODs; walkability, stairs, doorway volumes, material-ID diagnostics | **Partially existing** collision/nav checks; dedicated collision brush and per-face sound material painting planned |
| Animation, page 6 | Rigid-part animation authoring, low-frame sampling, redundant-key pruning | **Initial tool implemented** `EditorAnimationClip` and `EditorAnimationLab`; live actor integration outstanding |
| Hardware build, pages 6–9 | Keep existing Windows Java installer, game/runtime test workflow and optional software/GPU render modes | **Native .TIM/.TMD/.VAG, .bin/.cue ISO, CD-ROM sector ordering and DuckStation launcher rejected as mandatory**; a separate actual-PS1 exporter would be a distinct project |

## Safety and architecture nonnegotiables
- Every visible editor operation must either mutate the authoritative game scene through real adapters or be labeled **authoring-only**. Never misrepresent mockup/editor-only data as live-game functionality.
- Reference IDs resolve to actual assets; save/load round-trips retain transforms, layers and parent IDs. Preserve originals and implement migrations without destroying authored data.
- Use **the unified lighting and weather modules**, actual water module and existing sound-volume module; do not reinvent competing simulation, independent fog selector or new game audio engine in the editor.
- Underlying host-owned AI/ghost logic stays authoritative. A campaign/editor trigger may not reveal hidden hunter positions to ghosts, allow ghost van entry, or change attack timing outside host rules.
- Budget and classic-hardware-era counts are warning channels. The active renderer/Java memory/performance measurements, not a native PS1 spec sheet, determine what is truly executable.
- Scene regression QA should test that open doors connect physically, basements have walkable stair openings, and imported props don't block critical traversable routes; never block a release merely over taste or an out-of-date visual expectation.

## Prioritized expansion
First wire campaign map graph and typed flag registry to the existing mission scene data; expose trigger visualizations and actual door/stair/nav paths in the editor. Next add scene persistence and invalid-reference repair, authored entry/exit preview and 100% actual-game playtest. Then add edit/preview for sound zones, spawn orientation, and collision proxy gizmos. Keep sandboxed user-level scripting constrained to vetted actions and explicit permissions; do not deserialize arbitrary executable Java code from map content.

## 2026-10-30 authoring module checkpoint

**CAMPAIGN** on the game editor opens `EditorCampaignLab`. Its portable `EditorCampaignFlow` accepts only bounded typed lifecycle commands (START/PAUSE/RESUME/COMPLETE/STOP/RESET), preserves named simulation flags across STOP, detects duplicate commands, maintains reference→asset dependencies, and writes diff-friendly authoring state. **It does not start or stop any live game session.** `EditorCampaignFlowTest` checks idempotence, invalid transitions, reference guards and save/load.

**BILLBOARDS** opens `EditorBillboardLab`, backed by `EditorBillboardProfile` for source-preserving PNG/JPEG → separate PNG derivatives. The current 2D source/processed comparison, 256/512 canvas options, aspect-preserving nearest-neighbor fit, RGB555/332 quantization, optional ordered dither and estimated decoded RGBA bytes are actual editor controls. A **game-renderer 3D mapped preview, scene placement adapter, skybox/atmosphere commit and per-scene VRAM counters remain outstanding**, and the UI labels the current preview as 2D authoring-only.



---

### Source: Owner brief: menu/save, campaign, billboard and background authoring

**Original repository path:** `docs/wiki/psx-editor-menu-campaign-billboards.md`.

# PSX Editor: Menu/Save, Campaign Control, Billboards, and Backgrounds

**Authority:** owner-provided *Editor-Based Manipulation System* design brief, 2026-10-30 (inline design guidance, not one of the nine uploaded PDFs). This reference belongs beside the other PSX editor engineering sources in `docs/wiki/`, and is mirrored to Home Brew's `docs/beckenburg/wiki/` with an API-discoverable JSON contract.

**Target:** Beckenburg's Java 17 **modern game with a PSX-style rendering profile**. Historical PlayStation memory cards, T-pages, and disc latency are **optional simulation targets**, not hard runtime requirements. The existing game renderer, lighting, `SoundVolumeEditor`, authoritative campaign/mission state, UI and asset loader remain the owners; **never implement duplicate gameplay systems just to match a document**.

## Menu and save management

- **Editor-facing UI graph:** inspect menu screens, actions, transitions, focus order and escape/back paths, with a shallow three-level *style preset* and an explicit override where needed. Every actionable control must resolve to a real action or a deliberate disabled/coming-soon state, not a dead button.
- **Retro skin:** optional readable bitmap/monospace font, high-contrast backing plates, crisp keyboard/controller focus, instant item-to-item selection and optional short feedback sounds; keep mouse, screen-reader-scale options and accessibility. Retain modern input and actual reliable save behavior.
- **Save editor:** expose save-slot titles/icon preview, per-slot payload bytes, version, timestamp and validity; allow optional historical 15-block, 8KiB-per-block *simulation* for an emulation profile only. Default Beckenburg saves remain unconstrained by physical PS1 memory cards. Do not invent a formatting wait before real save operations; a brief **cancelable decorative** animation is permissible if the underlying action stays immediate.
- **Safety:** atomic writes, document migrations, corrupt-file warnings and explicit dependency notices before deleting a menu action/save reference. No path should silently discard campaign progress.

## Campaign script and lifecycle authoring

- **Visual flow states:** `PENDING → ACTIVE ↔ PAUSED → COMPLETE`, with explicit `STOP → PENDING` cleanup/reset. Green = Active, red = stopped/pending, amber = paused/needs review; completed can use a neutral distinct indicator. Do not infer green merely from a button click.
- **Idempotent operations:** duplicate START and RESUME cannot create duplicate active systems; duplicate STOP must be harmless. Complete may only happen from Active when actually authorized. An editor mock simulation must be labeled as such until adapted to real mission state.
- **Cleanup ownership:** stopping an actual game session must call existing lifecycle cleanup, release only session-owned temporary entities, reset *temporary* variables and restore an authorized start position. Never silently erase persistent campaign flags, player inventory or multiplayer state.
- **Bounded event logic:** no unrestricted imported Java, external scripts, runaway busy loops or synchronous editor-thread execution. Prefer typed trigger actions, bounded event queues and time/step quotas. Guard against recursion and no-yield cycles; preview/test the graph before attaching actions to the game.
- **Dependency inspection:** warn before deleting an asset, menu state, event, flag or trigger referenced by live scripts or maps. Provide source/referrer IDs, safe retargeting and migration. The editor must not automatically modify the referenced objects until confirmed.
- **Editor graph:** model stages as nodes with typed start, pause, resume, complete and stop edges; display actual script state separately from authored design. Restore from an editor testing snapshot without affecting gameplay sessions.

## Billboard, skybox and 3D background authoring

- **Source vs. mapped preview:** show an unmodified source bitmap alongside a textured 3D plane/skybox with the actual game's sampler, UVs, alpha/culling and lighting preview. The desired preview is *the live renderer*, not an unrelated image mockup; mark an interim 2D preview honestly.
- **Retro image presets:** anchor a preview to 256×256, 512×256 or another power-of-two/retro framing preset, with aspect-preserving padding/cropping rather than destructive rescale. Palette reduction, CLUT swaps, nearest-neighbor and ordered dithering are optional render profiles, never mandatory source corruption.
- **Source preservation:** keep the original image, license/provenance, pixel dimensions and hash. Commit/bake a derivative only when the user confirms; provide undo/revert and the ability to inspect the final mapped result.
- **Performance:** do not trigger expensive scene relights or full environment rebuilds on each text edit. Debounce/lightweight preview, then explicit **BAKE/COMMIT** for heavy changes. Any baking task should be cancelable, provide progress and retain the previous working asset on failure.
- **Budget overlay:** per-layer estimates for UI sprites, billboard textures, background textures, and other decoded images; label GPU/heap **estimates** separately from measured allocator statistics. Support selectable historical PS1 VRAM simulation without treating it as an actual 1MB limit on a Windows Java game.

## Engineering adoption matrix

| Requirement | Disposition | Current status |
| --- | --- | --- |
| Menu/save state graph and UI focus recovery | Adopt | Planned editor adapters; existing UI must remain the authoritative implementation |
| Virtual 15-block memory card boot/format manager | Optional aesthetic/hardware profile | Planned, not a real Beckenburg save requirement |
| Typed bounded campaign flow and lifecycle preview | Adopt | Portable authoring-only state model targeted; actual game lifecycle adapter separate |
| Idempotent START/STOP and cleanup | Adopt | Must be verified on actual game lifecycle before marking integrated |
| Explicit asset/script dependency warnings | Adopt | Planned graph/ref index and deletion guards |
| Billboard source/3D mapped split preview | Adopt | Planned runtime-renderer adapter |
| Palette/CLUT/retro atlas preview and bake | Adapt | Atlas/import foundation exists; billboard-specific non-destructive bake planned |
| Visual VRAM budget | Adapt | Diagnostic/optional simulated historical profile, not a hard release gate |
| No nested menus, smooth scrolling or modern input | Reject as absolute | Optional style defaults, preserving accessibility and user choice |
| Artificial load/format delays and synchronous hard loops | Reject as runtime requirements | Gameplay input stays responsive; no busy loops |

## Minimum acceptance tests

1. Menu graph: no orphaned routes, every Back returns somewhere valid, controller/keyboard/mouse behavior remains accessible.
2. Campaign: START twice creates one active session, PAUSE/RESUME and STOP are idempotent, invalid transitions are explicit, no event loop can run forever, and STOP never erases persistent inventory.
3. Dependency: delete an asset with active references → warning with concrete referrers; cancel must leave the project untouched.
4. Billboard: source hash remains unchanged; 2D and in-engine mapped result correctly preserve orientation/aspect; a failed bake restores the last valid asset; optional retro effects do not warp text or collision.
5. Performance: expensive textures, scripts and light changes are scheduled/committed, not re-run per keystroke; counters distinguish estimated bytes from measured heap/GPU usage.

The PSX reference documents are engineering *guidance*, not executable specifications that override host-authoritative ghost fairness, collision, weather, lighting, audio or release policy. Aesthetic warnings remain diagnostic; only genuinely broken functionality belongs in a release gate.

## Implemented authoring slice (2026-10-30)

The game editor now exposes **CAMPAIGN** and **BILLBOARDS** on its toolbar. Campaign is a *non-executing typed state-graph simulation* with idempotent transition commands, flags and declared asset dependency warnings. Billboards is a *2D asset preparation tool* with source-vs-retro derivative, aspect-safe presets, optional palette reduction/dithering, decoded RGBA memory estimates and explicit PNG baking without source overwrite. Java source: `editor/EditorCampaignFlow`, `editor/EditorCampaignLab`, `editor/EditorBillboardProfile`, `editor/EditorBillboardLab`. Separate portable tests exercise transition and file safety. No campaign action presently alters the actual game's mission state; no billboard is yet drawn using the live 3D game rasterizer.

This first slice is deliberately more limited than the full blueprint. The next source integration must use the authoritative runtime mission/cutscene/trigger APIs and actual scene billboard materials, rather than creating editor-only impostors that never appear in gameplay.



---

### Source: NPC/AI canonical authoring inspector

**Original repository path:** `docs/wiki/psx-npc-ai-authoring.md`.

# NPC / AI Authoring and Automation — Canonical Ghost Rule Inspector

**Architecture checkpoint: 2026-10-30.** This is a reusable Java 17 **editor AI module**, separate from the gameplay ghost engine. Source inspiration: the supplied 17-page *PSX NPC-AI-1.pdf*, the existing [AI adoption analysis](../ai/PSX_NPC_AI_ADOPTION.md), and the game's **actual executable ghost rule classes**. Historical PS1 RAM/GTE/MIPS limitations are stylistic references, not runtime specifications.

## What the working module reads

The editor's **NPC / AI** button opens `GhostAiRuleLab`. It uses the dedicated portable package `org.beckenburg.editor.ai`:

- `NpcRuleBook`: typed, immutable species definitions; stable IDs and base/variant relationships; labels for locomotion, traversal, visibility, energy charging, preferred action, frenzy thresholds, and candidate evidence. It rejects duplicate/invalid rule IDs, explains rules in readable terms, and emits advisory structural findings. An on-demand **AUDIT ROSTER** command lists these findings; **EXPORT RULES JSON** atomically writes a deterministic source-derived species catalog (\`NpcRuleExport\`) to the chosen file. Neither touches live NPCs.
- `NpcDecisionWorkbench`: read-only legality explanations, safe candidate proposals, explicitly **host-verified spatial-victim** guard for physical strikes, portal-exit validity, and bounded 512-observation deterministic replay. It only uses already-filtered coarse observations. **It never dispatches actions.**

The **game adapter**, `GhostAiRuleAdapter`, reads all `GhostIdentity.Type.values()` from **actual runtime source**, including Ancient and Elder variants:

| Runtime owner | Values interpreted in editor |
| --- | --- |
| `GhostIdentity` | stable species/base/variant, assigned rite, described archetype |
| `GhostTraits` | WALK/FLOAT, DOORS/WALLS, VISIBLE/CAMERA_ONLY/ORBS, energy charge condition and period, movement multiplier, UV and solitary-hunt constraint |
| `GhostBehavior` | favored event, per-species AI bias period, event tell |
| `GhostFrenzy` | energy entrance cost, earliest tick, permitted CLAW/DREAD attack families |
| `GhostEvidenceMatrix` | **possible** EMF, temperature, writing, UV, camera and additional sensor evidence; no evidence is confirmed by this table |
| `GhostDomain.observe()` | **host-approved** legal action list, current room, exits, coarse hearing, energy and manifestation; sole legality authority |

No duplicate handwritten species matrix was introduced: changes to the canonical Java rules are reflected the next time the editor opens the rulebook.

## Manual test workflow

1. Open the level editor → **NPC / AI**, choose one of the actual ghost species.
2. Read the species contract and distinguish *inherited base mechanics* from Ancient/Elder variant identity. Confirm camera-only ghosts and wall-travel restrictions in the actual source.
3. Pick an isolated **10 Hz** test tick and a **synthetic host hearing** condition. The tool builds a fresh `GhostDomain(new HouseMap(), ...)` and advances it one simulation tick at a time, **without touching the running investigation**.
4. Click **SIMULATE LEGAL ACTIONS**. The right pane displays real `observe()` energy, room, portal exits, manifestation, and legal actions. It then explains each proposed or rejected action using `NpcDecisionWorkbench`.
5. The **host-verified spatial victim** checkbox is explicitly labeled as a sandbox precondition. An audible hunter **does not** prove physical strike contact. The editor cannot grant a strike or reveal exact player coordinates.

The preview is deliberately not a production ghost AI. It does **not** reproduce stochastic haunting roll deadlines, real active-room acoustics/weather, physical pursuit, joint geometry, or measured in-game hunter contact. Any results must not be interpreted as proof of gameplay acceptance without real host tests.

## Architecture invariants

- **One authority**: real `GhostDomain.dispatch` applies action energy/cooldowns/fear/harm, and `LanGhostPursuit`, `LanAcoustics`, `LanGhostSpatial`, `HouseMap`, the host clock and collision keep their existing authority. The editor must never reimplement these systems to shortcut a behavior preview.
- **Perception security**: NPC decision input must be derived from **legitimately delivered events and coarse room/portal information**, not hunter health, exact body position, gear, private journal, hidden identity or extraction-van state. No wall-phase is granted except for species/paths the actual host permits.
- **Presentation vs gameplay**: AI staggering, caching and visualization can reduce overhead but never skip immediate sound, strike telegraphs, movement/collision, ritual/hunt, and multiplayer safety conditions.
- **Portable interface**: new NPC types (hunter allies, guards, wildlife) use a distinct adapter that supplies an explicit `NpcRuleBook.Rule` and filtered `NpcDecisionWorkbench.Observation`; they must not be passed fabricated ghost species attributes.
- **Performance**: bounded editor previews are on a SwingWorker, not the paint/EDT loop. Production AI scheduling must be measured on real maps; no full A* over every actor each frame, no unvalidated teleport recovery, no arbitrary 8-character cap.
- **Testing and release**: portable rulebook/replay tests run in **Modular Level Editor Standalone**; canonical roster and isolated `GhostDomain` behavior are checked in **Focused Castle Repair Checks** as non-blocking diagnostics. Only objective build/launch/essential game or required multiplayer failures may block a release.

## What is implemented versus next

**Implemented source:** `src/main/java/org/beckenburg/editor/ai/NpcRuleBook.java`, `NpcDecisionWorkbench.java`, `src/main/java/org/beckenburg/GhostAiRuleAdapter.java`, `GhostAiRuleLab.java`, developer-editor button, explicit package compilation in Windows game and standalone editor kit. Tests: `NpcRuleWorkbenchTest` and `GhostAiRuleAdapterTest` (test results must be read from GitHub Actions).

**Implemented second module layer:** [Host telemetry, navigation, and deterministic scenario documentation](psx-npc-ai-telemetry-navigation.md). The source now includes `NpcAiTelemetry` and `NpcAiTelemetryReport` (per-host rolling duration samples and aggregate CSV), `GhostAiNavigationAudit` (actual portal/acoustic checks) and `GhostAiScenarioRunner` (bounded real-domain/cadence decision replay). The **NPC / AI** dialog exposes **AUDIT NAV** and **PROFILE RULES**. Host recording is off by default, explicitly enabled locally, and never shared with multiplayer clients. New CI tests are submitted; passing earlier tests must not be interpreted as proof of the latest commits.

**Next priorities:**

1. **Attach permissioned live-host profiler readout** to developer UI (current UI displays isolated scenario timings; actual per-host metrics are accessible from trusted JVM code/local stderr).
2. **Collision and acoustic invalidation coverage**: verify dynamic door crossings, stair connectivity, swept movement and weather/material sound-path changes; record route topology revisions without bypassing the existing HouseMap and LanAcoustics.
3. **Expand scenario fidelity**: cover frenzy warning/recovery, wards, ritual interruption, human-to-AI takeover and actual LAN host parity without inventing ghost perception.
4. **Behavior editing with safety**: editable proposed parameters in a sandbox with diff/review; never mutate canonical rules without an explicit approved patch or run arbitrary user scripts.
5. **Other NPC adapters**: connect existing hunter, ally and Twin companions through their own canonical behavior systems rather than pretending all NPCs are ghosts.
6. **End-to-end real-world profiles**: pair existing door-opening CPU/GPU diagnostics with host timing histograms, allocation/GC statistics and real playtesting before any scheduler or AI cadence changes.

This is a true first step toward an NPC/AI tool suite, **not a declaration that all NPC behavior authoring or automated AI gameplay has been implemented**.



---

### Source: NPC/AI telemetry, navigation and seeded scenarios

**Original repository path:** `docs/wiki/psx-npc-ai-telemetry-navigation.md`.

# NPC/AI Tools — Host Telemetry, Navigation Audits & Seeded Rule Scenarios

**2026-10-30 implementation checkpoint.** Companion to [NPC/AI canonical rule inspector](psx-npc-ai-authoring.md) and the [PSX NPC-AI adoption analysis](../ai/PSX_NPC_AI_ADOPTION.md). This page records actual Java 17 source entrypoints and explicitly distinguishes host instrumentation from authoring-only tests.

## Live, per-host performance sampling

`org.beckenburg.editor.ai.NpcAiTelemetry` is a **portable, bounded diagnostic recorder**, compiled into both the standalone level editor kit and the game. `LanHost` owns **one instance per host**, and `LanGhostPursuit` receives that instance for acoustic/route timing. Metrics are:

| Metric | Actual code path instrumented | Note |
| --- | --- | --- |
| `HOST_TICK` | `LanHost.tickSafely()`, including real simulation processing | Whole host tick, not AI-only CPU cost |
| `PERCEPTION` | `ghost.observe()` inside authoritative AI tick | Host-generated legal action observation only |
| `DECISION` | `LanGhostPursuit.decide(...)` inside host tick | Actual production AI choice, not the authoring workbench |
| `APPROACH` | `LanGhostPursuit.approach(...)` inside host tick | Direction decision; `LanGhostSpatial.step` still owns movement |
| `HEARING` | `LanAcoustics.audible(...)` in real host sound delivery, and `LanGhostPursuit.heard` acoustic processing | May record two distinct checks for one audible event |
| `ROUTE` | `LanGhostPursuit.route(...)` | Real bounded BFS; no replacement pathfinder |

Each metric records **at most the newest 256 durations**, while retaining a total call count. `snapshot()` computes p50/p95/p99, minimum, maximum and mean over the current window. **No hunter coordinates, identity, health, inventory, secret role, journal, inferred position, acoustic cell location, or other player information is recorded.** No telemetry is sent to any multiplayer packet or GhostDomain observation.

Profiling is disabled by default. To enable it for a **locally controlled host process**, start the JVM with `-Dbeckenburg.ai.profile=true`, or invoke `LanHost.enableAiDiagnostics(true)` through trusted *in-process* developer code. `LanHost.aiDiagnosticsSnapshot()` returns a read-only snapshot; `aiDiagnosticsCsv()` provides a CSV report. When a profiled host is closed, it prints the aggregate CSV to **local stderr**, never the LAN. `NpcAiTelemetryReport.save(...)` supports atomic export of the same aggregate CSV. The flag is opt-in because even nanosecond sampling has runtime overhead.

**Important limits:** these are Java wall-clock timings of real functions on the hardware that executes them, **not GPU frametimes**, absolute CPU-cycle counts, complete Java allocation/GC analysis, or proof of stable 30/60 FPS. Compare p50/p95/p99 to real baseline captures and the separate door-opening profiler before altering cadence. Disabled sampling must impose negligible overhead and never change the AI decision logic.

## Real-world portal and acoustic navigation audit

`GhostAiNavigationAudit` reads the actual `HouseMap.ghostExits` (including authored doors/stairs) and `LanAcoustics.cost`. It lists directed links, reverse-link problems, unreachable room IDs, acoustic disconnects and the van/extraction boundary. `GhostAiNavigationAudit.path(map,from,to)` computes a **read-only** bounded breadth-first traversal; it **cannot** move the ghost or grant illegal wall travel. Closed doors can still be an authorized route: the collision/door API decides whether physical passage is possible at execution time.

The editor's **NPC / AI → AUDIT NAV** takes an existing authored site from `HouseMap.Site`, constructs a **fresh, isolated** map, and reports links, route-to-origin and hearing losses. It does not edit that map or perform live-scene traversal. Findings are diagnostic: an intentionally isolated room may not be a gameplay defect; verify authored layout before repair. Future live visualization should use actual editor camera/viewport overlays and jump-to-problem rather than a second scene renderer.

## Seeded, bounded ghost-rule scenarios

`GhostAiScenarioRunner.run(species,site,durationTicks,seed,syntheticSound)` runs for **1–1800 ticks** using actual:
- `GhostDomain.advance/observe/dispatch`, including real energy/cooldowns/legal actions;
- `GhostActionCadence` with a **seeded authoring-test RNG** for reproducibility;
- `LanGhostPursuit.decide/heard`, with the game's real `LanAcoustics` and portal graph;
- the shared `NpcAiTelemetry` for observed rule/decision/hearing/route costs.

A synthetic sound source may pulse every 40 ticks from **authored room 4**. Its position is a controlled test marker, **not a real player tracker**. No physical strike contact is ever supplied, so the runner rejects all `STRIKE` attempts. It stores at most 128 recent action resolutions. Results expose accepted, rejected and DRIFT counts, remaining rule-domain energy and aggregate timings. All rule decisions use real code, but this **is not a full LAN host, physics, ghost pose/collision, camera/lighting, ward, frenzy or player-contact simulation**. The editor's **NPC / AI → PROFILE RULES** displays this isolated trace and performance metrics, with explicit scope warnings.

## Diagnostics acceptance and next integrations

- The `NpcAiTelemetryTest` portable regression validates default-off behavior, bounded rolling percentiles, opt-in toggles, reset, and CSV with **no session secrets**.
- The `GhostAiNavigationAuditTest` game diagnostic validates actual portal edges, hearing costs, no extraction path and instrumented pursuit hearing.
- The `GhostAiScenarioRunnerTest` runs multiple base/Ancient/Elder species with a fixed seed, compares repeatable action traces, and forbids synthetic strikes. These game-side performance/behavior tests are **non-blocking diagnostics** under the release policy; failures still need repair and honest reporting.
- The full game build/launch, authentic ghost-human parity and essential multiplayer are the only applicable objective release gates. Subjective AI balance, timing targets and retro aesthetics remain advisory.

**Still outstanding:** a permissioned **live-host readout** wired into the developer UI (currently profiling is readable in the local process or stderr, while editor profiling is isolated), physical waypoint/door geometry visualization, acoustic topology invalidation metrics, event/collision/animation and memory-allocation profiling, actual Frenzy/ward/ritual scenarios, AI ally adapters, and LAN AI/human equivalence tests. No hardcoded native-PS1 scratchpad or NPC slot limits.



---

### Source: NPC/AI source adoption assessment

**Original repository path:** `docs/ai/PSX_NPC_AI_ADOPTION.md`.

# PSX NPC-AI architecture reference — Beckenburg adoption review

**Reference:** user-supplied 17-page `PSX NPC-AI-1.pdf` (30 October 2026).
This is an engineering adaptation of its recommendations, **not** an instruction
to reproduce original PlayStation hardware limitations in a Java 17 Windows game.

## First dependency: measured House door-opening profiles

The dedicated GitHub Actions workflow
`.github/workflows/door-opening-profiler.yml` builds the actual game on
Windows/Java 17, then runs `DoorOpeningFrameProfile` in separate JVMs:
- **CPU software:** real authored House, real `SceneRenderer.drawPractice`,
  baseline frames, 16 alternating actual door transitions, and swing frames.
- **GPU submission recorder:** real world-buffer preparation and explicit
  resident resource revision accounting. A recorder is **not** OpenGL timing.
- Each produces baseline/toggle/swing/recovery p50, p95, p99 CSV and
  peak shadow rays, invalidated cells, scene generations, upload bytes, GC and
  heap snapshots. They are diagnostic; no subjective FPS/art release gate.
- Installed Windows hardware GPU timing, visual inspection and real-player
  input still require separate acceptance. Do not claim those from the
  CPU or recording backends.

**Implementation dependency:** inspect the actual CSV and profiler results
before changing the AI update cadence or doing an unmeasured rewrite. A door
hitch with zero AI actors is by definition not repaired by throttling AI.

## Directly applicable recommendations (priority order)

### P0 — Preserve simulation correctness while measuring
1. Profile the host's existing **10 Hz** authoritative tick and the solo
   practice simulation separately. Time AI decisions, route queries, sensory
   work, physical collision and animation/image preparation. Report bounded
   p50/p95/p99, allocation/GC and actor counts; diagnostic only.
2. Keep one authoritative tick number and **no AI-side wall-clock loops**.
   Rendering may skip frames; commands, ghost deadlines, player collision,
   ritual state and network snapshots cannot be skipped arbitrarily.
3. Stabilize door-change handling: cache room connectivity and each
   actor's route until the destination/topology changes. Invalidate using
   portal revisions rather than rebuilding an entire graph or restarting all
   actors on every camera frame.

### P1 — Low-risk navigation and sensing
4. **Reuse authored room/portal graphs**. `CampaignAlly.route` uses a
   breadth-first traversal with fresh arrays/queue/path allocations whenever
   an ally changes rooms. `LanHost.campaignBotInput` builds a new BFS every
   input call when rooms differ; `LanGhostPursuit.route` does another
   bounded search; all are over only ~15 rooms, so first measure whether
   object churn is material. A cached next-hop table per topology revision
   is a candidate, not a justification for a new unbounded A* engine.
5. **Bound expensive decisions**, not movement or responsiveness.
   Use an event/goal-dirty flag and per-actor deterministic time slots for
   patrol target selection. Hearing, being attacked, door collision,
   spectator input and ritual/hunt reactions must still be delivered
   immediately through their existing authoritative paths.
6. **Sound propagation cannot become a simple flat distance check.**
   `LanAcoustics` already traverses physical portals, closed doors,
   materials, floors and weather noise; the ghost excludes the extraction
   van. Cache a room-to-room attenuation matrix by door/window topology
   revision if its Dijkstra/priority-queue allocations show up in profiling.
   Approximate the sound source position without leaking exact hunter state.
7. **Persist navigation intent and use tolerance/hysteresis** at door
   thresholds, nearby goal nodes and search/alert states. Use small
   squared-distance comparisons where they simplify hot checks, while
   preserving actual `HouseMap.resolveMove` swept collision.
8. **Coalesce simultaneous route requests** with a bounded, fair host
   scheduler only if measured demand warrants it. An AI can keep its
   previously safe target for a short, explicit interval; don't produce
   starvation, fake teleportation or delayed emergency decisions.

### P2 — Editor and authoring accelerators, conditional
9. Export prevalidated portal/waypoint adjacency and optional cover/ambush
   tags *for existing authored sites*. Never blindly cap nodes to four
   connections: preserve all real door/stair links or insert a validated
   intermediate node.
10. Precompute static *potential* room visibility/sector interest for
    **render/candidate rejection only**. Dynamic doors, transparent windows,
    vertical floors, flashlight/evidence interactions and shared LAN state
    still need authoritative runtime checks.
11. Preload known animation/sprite variants at scene preparation so an
    NPC switching to alert/hurt doesn't synchronously import a 3D model or
    decode images on the host tick/EDT. Measure memory tradeoffs first.
12. Use presentation-only staggered bark/footstep animation offsets and
    natural telegraphing **where fair**; do not alter ghost identity,
    evidence entitlement, attack rules or cadence merely to hide slow AI.

## Parts not appropriate to import literally

- The PDF's MIPS/GTE assembly, 12-bit fixed-point arithmetic, 360-entry
  sine/cosine LUT, PSX 1 KB scratchpad, 2 MB RAM ledger, GPU VRAM data
  tricks, manual unrolling, and fixed 4–8 NPC slots are **hardware-specific**,
  not sensible Java 17 optimizations. Modern JVM optimizations must be
  measured rather than cargo-culted.
- Its recommendation to turn off far-away brains or only activate full
  combat when inside the player's field of view would create exploitable
  multiplayer/gameplay discontinuities. Instead reduce *presentation*,
  optional cosmetic inspection and redundant planning while retaining
  authoritative sound, collision, deadlines and simulation continuity.
- Its proximity "Spidey Sense", gaze-triggered aggression and exact
  omniscience cheats conflict with Beckenburg's intended **auditory ghost
  awareness** and fairness. Ghosts must not acquire hunter positions from
  visibility, stare direction, hidden evidence, or van location.
- Node teleportation, temporary collision disabling and a jump to the
  nearest node are **not acceptable safety recovery defaults**. Stuck
  actors should diagnose, choose a valid alternative portal and replan
  without passing through walls/floors or hiding pockets.
- Hazard-zone shortcuts must not bypass real line-of-sight and collision
  checks for rituals, attacks or evidence. NPCs/ghosts cannot enter the
  safe van where forbidden, and player ghosts cannot enter hiding spaces.
- The PDF's absolute bans on dynamic allocations, trigonometry, A*,
  steering, and complex sound are historical design cautions, not proof
  of current bottlenecks on JVMs. Prefer measured bounded work.

## Existing implementation facts to preserve

- `LanHost` owns a scheduled 100 ms tick; multiplayer authority and ghost
  forced-action cadence (5–45 s) remain host-owned.
- `CampaignAlly` already uses portal BFS and the same swept move/elevation
  resolver as the player; `GhostPracticeHunterAi` uses audible cue events
  and room-level suspicions, not true ghost identities.
- `LanGhostPursuit` uses finite recent sound-memory and coarse location
  rather than continuous private hunter tracking; `LanGhostSpatial`
  forbids the extraction van and hiding pockets.
- `TwinCompanionAi` is a simulated second actor with sound and movement
  state; it may not teleport, cross solid walls or invent secondary damage.
- `LanAcoustics` charges attenuation through authored doors and materials,
  including weather noise, instead of a naive omnidirectional radius.

## Acceptance and documentation

Build Java 17, run a controlled authored House/Castle navigation case and
door-toggle case with **real** ally, practice hunter, AI ghost and Twin
actor on separate scenarios. Include vanilla baseline and optimized
rerun with identical seeded intent, player inputs and portal transitions.
Validate legal rooms, no wall/floor phasing, no safe-van or hiding-space
entry by ghosts, unchanged evidence/ritual outcomes, no omniscient hunt,
consistent host and connected-client snapshots, and bounded path work.

Write per-scenario summaries for decision/route/sensory/collision time,
path request backlog age, allocation/GC, active actor counts and p99
simulation-tick time. Flag quality/performance anomalies as **diagnostic**;
only objectively nonfunctional build, startup, essential gameplay or
multiplayer/update failures are release-blocking.

**Status:** adoption design prepared; no claim of AI refactor implementation,
installed Windows playtest, or actual GPU FPS improvement.



---

## Part V — Terrain, environment, navigation, visual and agentic implementation contracts

These are existing committed technical requirements, including details of tested partial implementations and explicitly unfinished acceptance. They are preserved without replacing one another, and should be read using the stage dependencies in Part II.


---

### Source: Terrain requirements (17-page owner PDF: existing integration contract)

**Original repository path:** `docs/editor/TERRAIN_REQUIREMENTS_INTEGRATION.md`.

# Terrain Editor Requirements — implementation contract

Basis: user-provided *Terrain editor requirements.pdf* (17 pages). This document
tracks implementation, not promises. The editor is a reusable Java 17 library:
the Beckenburg game renderer is an optional consumer, not its source of truth.

## Shipped in the standalone LevelEditorKit Java17 build

| Requirement | Implementation | Limit |
|---|---|---|
| Stitched 16-bit source heightmaps | `StitchedHeightmapWorld`, `StitchedWorldEditor` with seam-validated, journaled batches (up to nine tiles), crash recovery, and interactive shared-vertex seam sculpt | The cross-tile brush uses its own non-destructive session; merging it with sculpt-layer v2 masks is pending |
| Procedural noise | `TerrainLayerStack.Noise`: Perlin, Simplex, Worley, ridged, billow, domain warp | Deterministic octave stack; no graph UI |
| Live low-res generation preview | Workbench `LIVE LOW-RES PREVIEW` | 2D analytical height preview, not live 3D |
| Non-destructive stacked sculpt layers | `TerrainLayerStack` v2 with editable per-layer sample masks, enable and opacity; existing v1 files are readable | Up to 12 layers; source heightmaps remain unchanged until explicit bake |
| Non-destructive seam authoring | `TerrainSeamSculpt`: unique global vertices, 3×3 viewport, sparse undo/redo, save/reopen, guarded source bake; coordinated layer-document rebasing preserves v1/v2 sculpt deltas, masks, materials and freeze state | Seam edits are still kept in a separate authoring session until bake; interactive cross-tile layer-mask editing remains pending |
| Precision brush base | Raise/lower/smooth/flatten/revert/trench/terrace; inner radius, three curves, alpha images, rotation, event-independent dab spacing, elevation/slope guards | No mesh stamp-angle fitting yet |
| Brush hot-swap presets | `TerrainBrushPresets`: versioned atomic save/load/delete, bundled alpha and numeric profiles | Presets scoped to a world; share/export UX later |
| Localized differential undo/redo | Mixed sparse height, material paint, freeze/unfreeze, material locks and layer enable/opacity history; painted drag grouped into one edit | Session history is memory-resident; layer add/delete and route documents are not yet undoable |
| Terrain stamping | `stamp` and image-based height stamp in workbench | Height textures only; no arbitrary 3D mesh conversion |
| Thermal erosion | Talus relaxation and bounded selected-region iterations; protects source seams | Simplified four-neighbor solver |
| Hydraulic erosion | Seeded droplets constrained to active region, downhill sediment transport/evaporation | Droplet speed/sediment outputs not yet persisted |
| Aeolian erosion | Directional mass transfer inside selectable region | No raycast wind shadowing yet |
| Slope/height auto-biomes | `autoBiome`, `slope` | Fixed rules; not yet a node-based rule graph |\n| Derived topology / drainage | `TerrainTopologyFields`: analytical curvature, slope, D8 flow direction/speed estimates, accumulation and moisture proxies; versioned disk cache + height fingerprint | Not simulated droplet flow/sediment; no cross-tile drainage yet |
| Surface materials | Eight stable IDs; explicit authored-material bitset; reversible GRAVEL/DIRT/RIVERBED managed corridor surfaces; paint and erase-override brush; preview + world-space physical material query | Live Beckenburg renderer/footsteps still require a host adapter; no blending between route shoulder materials yet |
| Freeze/exclusion masks | Rectangle freeze, material-based lock and brush respect | No mesh-hull raycast or cylinder masks yet |
| Infrastructure grading | Bounded grade rasterizer with reversible road/rail/river height and material overlays; route move/delete rebuilds without destroying painted material/height source; legacy v1 routes remain unmanaged | Old painted legacy grades lack provenance; procedural overlays still do not alter shared border samples |
| Grade constraints | Rail/road maximum grade validation; clamp controls to target grade | No turnout, bridge or tunnel geometry |\n| Real railway track geometry | `TerrainRailMesh` builds ballast prisms, two extruded rails and spaced sleeper meshes from RAIL alignments; shared real-mesh inspector and validated OBJ export | Geometry-only: no switch, bridge, tunnel, rolling-stock physics or host-renderer adapter |
| Background operations | SwingWorker for heavy generation, erosion, save, bake | Full GPU/physics pipeline not wired |
| Safe explicit source bake | Checks R16 range and checksum; journaled batches can include both source PNGs and existing authored layer documents under explicit opt-in, with rollback of both | Multi-file writes are journal-recoverable on reopen, not atomically visible to concurrent external readers |
| Durable document | Backward-compatible v1/v2 terrain reads; v3 explicit paint-provenance and sculpt-mask writer; SHA-256 stale-save safety and atomic binary layer/spline writes | v1/v2 intentionally painted GRASS cannot be distinguished from their unpainted default, so historical GRASS provenance cannot be reconstructed |
| Dependency dirty regions | `TerrainLayerStack.EditEvent`: per-transaction bounds, affected domains, revisions and explicit listeners; no per-dab callbacks | Consumers must subscribe and schedule actual foliage/nav/physics rebuilds; no runtime adapter yet |
| Shared authored-surface geometry | `TerrainComposedHeightSampler` provides one exact triangle-height function to `TerrainEntranceMesh` and `LayeredTerrainQueries`, using saved unbaked sculpt/mask layers; source scene fingerprint includes layer files | Runtime Beckenburg host renderer/collision adapter not yet wired; long-lived queries must explicitly refresh cached data after layer saves |
| Concurrent document safety | Saved layer documents use SHA-256 optimistic readback and reject stale edits/bakes; coordinated rebases retain authored metadata | Cross-process source reads are not fully atomic |
| Source/runtime separation | Editor package standalone JAR and separate functional CI | Game renderer and physics integration still incomplete |

Implementation entry point: `StitchedWorldEditor` > **LAYERED TERRAIN TOOLS**.
Standalone build: `tools/build-editor-kit.sh` or `tools/build-editor-kit.cmd`.
Tests: `TerrainLayerStackTest`, `TerrainInfrastructureSplinesTest`,
`TerrainBrushProfileTest`, `TerrainBrushPresetsTest`,
`TerrainErosionRegionTest`, `TerrainMixedHistoryTest`,\n`TerrainTopologyFieldsTest`, `TerrainEditEventTest`,\n`TerrainRailMeshTest`, `StitchedHeightmapBatchTest`,\n`TerrainLayerMaskTest`, `TerrainSeamSculptTest`,
`StitchedHeightmapLayerRebaseTest`, `TerrainLayerConcurrentSaveTest`,
`TerrainComposedHeightSamplerTest`, `TerrainManagedInfrastructureTest`,
`TerrainRouteMaterialOverlayTest`;
workflow: `.github/workflows/standalone-editor-kit.yml`.

## Mandatory preservation rules

1. Existing 16-bit PNGs are authoritative until the user explicitly approves a bake.
2. No game-specific meshes, assets, level classes or renderer references may enter the
   portable terrain engines. Beckenburg-specific integration belongs in adapters.
3. A single-tile bake must preserve every shared edge sample exactly, or fail; no
   automatic seam patching or arbitrary overwrites.
4. A failed operation reports failure; never display a fake 3D/game screenshot.
5. Material IDs, masks and layer configuration survive baking. Edits support save/reopen.
6. Functional failures (compile, data corruption, broken save/reopen, required runtime
   integration) may block a release. Visual quality, texture look and style are diagnostic.
7. Preview calculations must not mutate terrain or generate excessive full-map work.

## Next integration passes, in dependency order

### Authoring data and precision
- Add independent Bezier falloff curves for plateau interior/exterior and project-independent preset sharing; local brush preset save/load and directional flattening, alpha/spacing/rotation are integrated.
- Add per-layer editable extent bounds and mask clipping profiles; per-vertex per-layer sculpt masks are now implemented with undo/redo, view inspection and versioned save/reopen.
- Add resampling with landscape-preserving interpolation and irreversible resolution-change
  preview. Preserve world-space datum, source seam positions and landmark control points.
- Add mesh-to-height stamping using real mesh ray projection, normalized histogram
  matching, slope/water/grid anchor snapping and generated stamp previews.
- Add disk-backed, bounded-memory differential undo/redo covering materials, layer
  operations, masks, splines and erosion. Unlimited logical history requires disk paging.

### Reversible infrastructure authoring
- Managed route v2 grades now rebuild from spline metadata, preserve hand-sculpted heights
  and material paint, and are sampled by terrain mesh and physical queries even
  without a saved ordinary sculpt document.
- Legacy v1 route records retain their original directly-painted semantics; no
  fictitious reconstruction of already overwritten heights is attempted.
- Managed route material overlays now display GRAVEL for rails, DIRT for roads,
  RIVERBED for rivers; explicit user-painted GRASS/MUD wins. Erasing paint
  overrides reveals the route again, with grouped undo/redo and v3 persistence.
- Next: junction/switch geometry, bridge/tunnel physical structures, blended
  material shoulders and multi-tile grade seam coordination.
- While managed routes exist, source bake refuses to flatten their procedure
  irreversibly into R16. Export authored mesh geometry instead.

### Simulation and derived outputs
- Multi-tile localized erosion with halo samples across adjacent tiles, cancellation
  checkpoints, deterministic seeds, protected gameplay zones and conservative transfer.
- Add actual hydraulic particle velocity, sediment depth and wetness outputs to the existing versioned topology cache (which currently stores only height-derived D8 drainage and moisture estimates); preserve explicit input-fingerprint invalidation.
- Implement wind shadow ray marching and angle-of-repose brush constraints.
- Connect host-level foliage, water, materials, collision and navigation rebuild adapters to the portable dirty-region contract; editor-side mesh and support queries share the same composed height sampler, but host collision and actual rendering remain unverified. Long-lived queries expose explicit refresh rather than per-sample file scans.
- Add background bake progress, cancel and crash-recovery journaling for multi-tile patches.

### Infrastructure / train module
- Expose persisted spline control handles, tangents, profile edits and path list in UI.
- Add railway cross sections: ballast, sleepers, rails, minimum curvature,
  banking/cant, switches, junctions, station platforms and multiple tracks.
- Grade embankments/cuttings while supporting our three seam/cutout entrance methods.
- Build real switches, bridge/tunnel geometry and route intersections, not painted heightmap approximations. Real standalone rail/ballast/sleeper geometry is now exported as OBJ.
- Produce portable spline and corridor output consumed by rail and AI/nav modules.
- Integrate cross-tile edits into v2 per-layer sculpt masks while keeping shared vertices seam-safe *before* source bake. Coordinated source PNG + saved layer CRC rebasing is now journaled and implemented; the cross-tile edit session remains separate until bake, and cross-process atomic visibility is not available.

### Materials, visualization and engine adapters
- Implement tile/face material-ID grids for strict retro mode, atlas packing,
  vertex colors, palette/nearest-neighbor rules, baked lighting and optional affine warp.
- Implement modern renderer adapter separately: normalized splat weights, height-blend
  transitions, slope-aware triplanar projection, macro-variation and physically anchored
  surface types. Actual material sample budgets are renderer/hardware specific; the
  source document's four-layer ceiling is a suggested optimization, not a universal law.
- Add biome-rule graphs with textures, foliage and audio rule outputs.
- Implement explicit adapter capability negotiation for virtual texturing, packed
  normal/roughness maps, terrain LOD and optional GPU displacement. Do not require
  modern APIs for the PSX renderer.
- Capture genuine editor/game screenshots for visual diagnostics, never as
  release-blocking tests.

## Next document handoff\n\nThe supplied *Landscape layering.pdf* (eight pages) is separately analyzed in\n[LANDSCAPE_LAYERING_PREPARATION.md](LANDSCAPE_LAYERING_PREPARATION.md).\nIts four-tier material/shader architecture remains pending until the first\nterrain authoring specification reaches an explicit, verified scope boundary.\n\n## Definition of complete

Every named tool in the source document is either usable end-to-end with
save/reopen, undo, and source protection, or clearly marked unavailable in the UI.
Authoring data must be portable into non-Beckenburg projects. Runtime collision,
navigation, visual materials and physics require separate actual integration and
verification. The present work is a tested foundational implementation, not a claim
of full feature parity with the entire PDF.

## Unified owner requirements document chain (2026-10-31)

This remains the detailed **terrain** implementation contract. The added topology,
soundscape, engine base and multi-genre source inputs are registered **alongside**
this document, not instead of it: [source chain](requirements/DOCUMENT_CHAIN.md)
and [dependency-ordered gates G0–G12](requirements/STAGED_INTEGRATION_PLAN.md).
The prior landscape-layering handoff remains active; feature acceptance still
requires code and runtime tests rather than this documentation link.



---

### Source: Landscape layering (eight-page owner PDF: four-tier material architecture)

**Original repository path:** `docs/editor/LANDSCAPE_LAYERING_PREPARATION.md`.

# Landscape Layering — next specification preparation

**Status:** READY FOR IMPLEMENTATION AFTER TERRAIN REQUIREMENTS. This is
architecture/preflight for the user's eight-page `Landscape layering.pdf`,
not a claim that its material/shader features have been implemented.
The first specification remains authoritative for active work:
[Terrain requirements integration](TERRAIN_REQUIREMENTS_INTEGRATION.md).

## 4-tier design from the supplied document

| Tier | Document terminology | Reusable editor responsibility | Engine integration boundary |
|---|---|---|---|
| 1 | Global Inputs & Data | Shared world coordinates, deterministic source heights, terrain material IDs, protection masks, wind, derived curvature/flow/moisture | Neutral attribute buffers and revision/fingerprint IDs |
| 2 | The Material Domain | Grass, soil, rock, snow and extensible stable material profiles, per-material UV scale, optional packed maps | Unreal/Unity-specific Material Attributes and `SetMaterialAttributes` belong to host adapter, NOT the editor model |
| 3 | Blend Domain Matrix | Curvature, height, slope and flow classifiers, priority/weight normalization, horizon-noise variation, override masks | Portable evaluator and diagnostic mask previews; host may compile a shader |
| 4 | Master Output Graph | Layered terrain outputs, PSX tile-ID/vertex-color or optional modern material inputs | Renderer-owned RVT/parallax/normal/displacement facilities, with explicit capability flags |

The document defines the topology chain as elevation -> structural slope ->
steep/flat decisions and low-elevation curvature/AO -> cavity/ridge decisions.
Keep this separable from authored terrain heights. It also recommends
`SetMaterialAttributes`, `StaticSwitchParameter`, `Shared: Wrap`,
`PixelNormalWS`, `VertexNormalWS`, RVT and shader-wave optimization.
These names are **Unreal/HLSL-specific examples**, not portable Java 17 API
requirements. Implement equivalent semantics in engine-specific adapters,
never by adding dependencies to `org.beckenburg.editor`.

## Existing handoff contracts

- `StitchedHeightmapWorld`: authoritative, seam-validated R16 source tiles.
- `TerrainLayerStack` v3: non-destructive height, protection, eight stable
  material IDs, sculpt-layer alpha masks (v2+) and explicit authored-material
  provenance, including deliberately painted GRASS. Version 1 and 2 files
  remain readable, though historical painted-GRASS provenance was not stored.
  Erasing authored paint reveals managed route material without altering R16.
- `TerrainTopologyFields`: derived terrain normals/slope proxy,
  curvature, D8 drainage, flow-direction/speed estimates and moisture proxy.
  Fields are **not** actual hydraulic sediment particle measurements.
  Fingerprints invalidate derived maps when sculpting changes heights.
- `TerrainInfrastructureSplines` v2: legacy routes remain unmodified on
  migration; newly managed road/rail/river alignments supply deterministic
  reversible procedural terrain grading, rebuilding on edit/delete with
  collision and authored mesh heights using the same sampler. Reversible
  material overlays are now rail GRAVEL, road DIRT, river RIVERBED, subordinate
  to deliberate user paint. Junction geometry and shoulder blending remain pending.
- `TerrainRailMesh`: portable genuine ballast, sleepers and left/right rail
  geometry exported through the shared terrain mesh writer; optional host
  adapter still needed for live rendering/collision.
- `StitchedHeightmapWorld.importTilesBatch`: seam-aware, rollback-journaled
  source PNG replacement across up to nine tiles, with recovery on reopen.
- `TerrainComposedHeightSampler`: source plus saved sculpt-layer deltas and
  alpha masks use one identical height sampler for actual exported terrain mesh
  geometry and physical surface/navigation queries; scene fingerprints include
  layer files. Long-lived query consumers explicitly refresh after edits.
- `TerrainLayerStack`: blocks stale saves after another editor changes either
  the source tile or authored document; the source batch journal can rebase
  layer checksum metadata without changing its creative content.
- `TerrainSeamSculpt` and `TerrainSeamSculptEditor`: global-vertex
  non-destructive seam editing, sparse undo, session save/reopen, source
  fingerprint checks, discard, and guarded explicit batch bake. Source-bake
  transactions now rebase v1/v2 layer documents in the SAME recovery journal;
  live cross-tile sculpt-mask painting is still a separate authoring task.

## First implementation tranche, after finishing Terrain editor requirements

**Route/material priority boundary:** Implemented portable precedence is:
explicit human paint (including GRASS) over a managed route's material, over
implicit default GRASS. Both `TerrainComposedHeightSampler.surfaceMaterial`
and `LayeredTerrainQueries.groundMaterial` expose that result. The second
specification must build its blend matrix atop this semantics, not repaint
source material IDs or flatten procedural corridors into source heightmaps.

1. **Stable material catalog.** Material profiles with immutable numeric IDs,
   albedo/tile atlas/normal/roughness/AO/height URI slots, physical surface type,
   texture scale and supported render profiles. Keep legacy material IDs stable.
   Add Moss and Concrete to the catalog for the document's nine-material test.
2. **Neutral four-tier evaluation model.** Typed inputs, material description,
   topology blend rule graph and output buffer; no host engine class imports.
   Rules include slope normals, height-gradient start/range with world-space
   low-frequency perturbation, curvature/cavity classification and downstream
   moisture. Human-authored material overrides have declared priority.
3. **CPU reference evaluator and real preview.** Deterministic testable topology
   masks and material assignments for a selected region, with ability to
   inspect each tier separately before any shader is involved.
4. **Renderer capability profile.** `RETRO_PSX` baseline: tile material-ID
   lookup, atlas/nearest-neighbor, vertex lighting; optional `MODERN`
   with normalized layer weights, height blending, distance UV biasing,
   triplanar, packed textures and RVT only if host supports them.
   Mobile/standalone-VR profiles use a lean fallback and no mandatory RVT.
5. **Nine-material stress scene.** Grass, Dirt, Gravel, Rock, Sand, Mud,
   Moss, Snow, Concrete. Test all authored material IDs decode, blend
   weights normalize, no missing resource references, and all configured
   host rendering paths build and launch. Shader complexity/pixel imagery
   are **diagnostic outputs only**; visual style does not block release.
6. **Host-specific shader adapters.** Only after neutral evaluator is
   stable, implement adapters to actual Beckenburg/other project renderer
   capabilities: UV inputs, shared sampler state, optional static feature
   variants, distance LOD, packed texture maps, and RVT where supported.

## Source-derived feature inventory (eight-page document)

- Four tiers and package-each-material-attribute guidance.
- Material-function instruction-count *target* (<35 recommended).
- Shared UV inputs, world-space dot-product slope, vertex/pixel normal mix.
- Height falloff with noise-warped elevation boundary.
- Curvature/cavity or baked AO approximation.
- Sampler sharing and near/far UV blend.
- RVT/virtual texture optional optimization, with matching texture
  dimensions and power-of-two mip layouts.
- Two packed maps: albedo RGB + height A; normal XY + roughness B + AO A.
- Height-field contrast blend scalar `Global_Blend_Contrast` (source
  recommendation: 0.02–0.07), plus flow-vector-driven silt/debris.
- Static feature variants and optional SM6 wave-based culling.
- Platform split: desktop/console modern optimization vs. mobile/VR
  low-overhead, no required displacement/RVT.
- Nine-layer overlap stress test, close/far shader complexity inspection.

## Explicit engineering cautions

- Do not reproduce the PDF's Unreal node names in portable core interfaces.
- Do not treat `Shared: Wrap` as a universal setting outside Unreal or
  assume a universal four-layer/16-sampler limit across all GPUs.
- Do not promise that an RVT page makes an arbitrary number of materials free;
  baking, updates, residency and memory remain real runtime costs.
- Flow-driven dirt/silt shaders must distinguish **derived drainage proxies**
  from physically simulated sediment fields.
- Do not call purely analytical previews an actual in-game render.
- Keep hard release gates strictly functional; shader complexity, visual
  artifacts and retro style remain diagnostic screenshot-based review.

## Preconditions for starting active implementation

- Verify standalone Java 17 editor build with first-spec tests.
- Finish / deliberately scope missing first-spec features: multi-tile seam-
  safe editing, true infrastructure geometry, robust rerouting, materials
  and collision/nav integration.
- Add first-spec material/terrain dependency dirty-region notifications.
- Then implement the four tiers from the bottom upward; verify generic
  project adapters before connecting Beckenburg-specific rendering.

## Unified owner requirements document chain (2026-10-31)

The existing four-tier landscape materials specification continues under
[document chain](requirements/DOCUMENT_CHAIN.md) and
[staged cross-module gates](requirements/STAGED_INTEGRATION_PLAN.md),
particularly G4. Topological editing, audio and genre plugins must reuse the
same terrain material and scene identity, without replacing this four-tier model.



---

### Source: Stitched heightmap and long-distance terrain world system

**Original repository path:** `docs/development/TERRAIN_WORLD_SYSTEM_PLAN.md`.

# Beckenburg Terrain World System — engine and editor contract

**Owner requirements (2026-10-31):** build the human-operable terrain and loading-zone modules first, then their agentic interface. Support both finite authored sites and extremely large, unified, stitched-heightmap open worlds. Support miles of sight distance without distant people/objects being falsely buried or exposed by averaged LOD hills.

**Status:** architectural specification and staged acceptance criteria. This file is not proof that streaming, runtime LOD or open-world gameplay is complete. The existing `TerrainZoneDocument` and `TerrainZoneEditor` are finite-grid foundations; Blackpine's `SiteTerrain` consumes saved height offsets. Independent stitched-tile support is a new module milestone and must be verified separately.

## 1. PSX-era baseline, modern technique, and Beckenburg extension

| Domain | PSX-era typical constraint | Modern established approach | Beckenburg requirement (not automatically implemented) |
|---|---|---|---|
| World size | Predivided maps, loading screens, heavily limited geometry memory | World partition / streaming cells | Globally addressed tile coordinates with sparse creation, edit, and load, not one huge image or fixed volume; loading zones optional for interiors and disconnected worlds |
| Elevation | Low-resolution static geometry, coarse meshes | 16-bit+ heightfields, tiled import, terrain sculpt/paint | Multiple independent 16-bit PNG height tiles sharing exact borders; global metre scale and datum; lossless editable source separate from runtime caches |
| Landscape generation | Manually assembled terrain and repeating textures | Seeded noise, biome masks, erosion and splat maps | Reproducible multi-octave generator, seeded biome/landform masks, editable overrides, import/export and provenance; no unreviewed destructive rebake |
| Detail near/far | Fixed draw distance, fog, pop-in | Quadtree/clipmap/geomorph, screen-space-error LOD | Multiple distance rings from exact source, measured geometric-error bounds, LOD stitching, hysteresis, geomorph; distance in world metres, not arbitrary tile count |
| Occlusion | Painter sorting/z-buffer limitations and aggressive cutoffs | Depth/occlusion culling, HLOD and visibility data | Canonical *full-resolution* terrain for line of sight and actor physics; LOD mesh NEVER authoritative for gameplay obstruction or actor support |
| Precision | Small coordinate range, large-world jitter | Origin rebasing, relative/64-bit world coordinates | 64-bit tile indices or doubles for world positions; local floats for GPU vertices; origin shift is presentation-only and preserves gameplay coordinates |
| Texture | Tiny atlases and repeated UVs | Virtual textures, clipmaps, layered materials | Biome weights / splat masks and distance texture mips; preserve PSX aesthetic as a configurable presentation effect, not a geographic limit |
| Objects/foliage | Sparse hand-placed objects or sprites | Instanced vegetation, impostors, HLOD | Deterministic scattering, stable persistent IDs, per-biome density, instance batching and separate object LOD and collision |
| Physics/navigation | Simple mostly loaded regions | Independent collision/nav meshes in resident cells | Exactly supported sampled terrain and separate simulation interest zones; unloaded visual tiles don't invalidate authoritative actors or host AI |
| Editing | Fixed levels and heavy offline rebuild | Non-destructive brushes, layer history, asynchronous baking | Human editor with terrain brush, texture layers, road cuts, water integration, local procedural regeneration, undo/redo, region locks and exact saved readback |
| Networking | Small synchronous map state | Partition state replication and interest management | Host-authoritative world and seed, separately versioned tile deltas, revisions and affected-zone updates; no client-authoritative terrain height |
| Testing | Manual eyeball checks | Differential geometry/performance tests | Seam, ray, scope, crest, cross-LOD, long-distance actors, fast travel and streamer-load test fixtures, with actual engine captures |

Background references: [Epic World Partition](https://dev.epicgames.com/documentation/en-us/unreal-engine/world-partition-in-unreal-engine) (streaming cells); [Epic HLOD](https://dev.epicgames.com/documentation/en-us/unreal-engine/world-partition---hierarchical-level-of-detail-in-unreal-engine) (distant proxies); [Cesium 3D Tiles spec](https://github.com/CesiumGS/3d-tiles/blob/main/specification/README.adoc) (geometric error and screen-space refinement); [NVIDIA GPU Gems 2, geometry clipmaps](https://developer.nvidia.com/gpugems/gpugems2/part-i-geometric-complexity/chapter-2-terrain-rendering-using-gpu-based-geometry) (nested LOD rings and transition morphs). These provide comparisons, not vendor code to copy or evidence Beckenburg already works this way.

## 2. Unified terrain coordinate / tile model

- One global XY-in-horizontal convention for our game: **X/Z horizontal, Y metres up**, and one world elevation datum. Named source/world origin; no implicit image stretch to world size.
- A world manifest records `worldId`, schema version, tile cells N (power of two), metres per cell, world origin X/Z, elevation minimum and maximum, generation seed, texture-layer catalog, and precision/revision. One **N+1 by N+1** 16-bit grayscale tile image contributes N by N cells.
- Tile `(tx,tz)` covers `[originX + tx*N*step, originX + (tx+1)*N*step]` and equivalent Z. Negative tile indices are valid.
- Edge heights are **identical numeric 16-bit samples** between adjacent tiles. Reject conflicting imports, offer previewed repair modes later. Verify corners shared by four tiles. No bilinear mismatch, duplicate last-column ambiguity or silently painted seam.
- Global quantization means identical sample `q` maps to exactly one elevation `minY + (q/65535)*(maxY-minY)`. A local 16-bit scale per tile is forbidden because it breaks seams.
- Height query uses the **same triangle diagonal** for renderer/physics (not one bilinear function for walking and different GPU triangles). Sparse unknown tiles return explicit unavailable status; do not silently return sea level.
- Tile imports and edits are atomic, revisioned and recoverable; bounded PNG decode, tile cache, overflow checks and stable file naming. Source 16-bit images are preserved. Runtime mesh/LODs are derived caches, not the master terrain.
- Generator samples global integer vertex coordinates and a fixed seed; generating tiles in any order yields the same shared border. Terrain edits are separate from seed-based source and explicitly dirty the dependent tiles/LODs.
- Sparse world size is **not** limited by `TerrainZoneDocument.Grid`'s 65,536-vertex finite editing cap. That cap remains appropriate only for finite grid documents.

### Heightfields do not replace actual 3D caves and structures

A heightmap represents **one elevation per horizontal X/Z coordinate**. It cannot describe two vertically stacked floors, cave ceilings, overhangs, bridges or mine shafts correctly on its own. Treat it as the outdoor ground foundation, with separate authored volumetric meshes / solid collision / portals for excavations, multi-floor geometry and exposed rock faces. A subtractive hole mask in the ground permits cave entrances but must not invent a walkable terrain surface inside the void. The editor must show which system owns collision at each position (terrain ground, structural mesh, stair, water, tunnel). Keep the same real gameplay collision/nav system at every LOD, regardless of which visual layer is currently drawn.

**Acceptance fixtures:** tunnel under a surface hill remains navigable without terrain snapping the player onto the roof; bridge and road span keep their own supports; cave mouth remains open in every streamed LOD, including scope view from far outside; long-distance cliff/cave silhouettes cannot turn into solid averaged berms that falsely hide distant characters.

## 3. Long-distance LOD: visual simplification cannot change reality

**Non-negotiable:** game physics, character feet, projectiles, ground-contact, AI, hiding and line-of-sight resolve against authoritative high-fidelity world geometry, **not** a rendered LOD proxy. If a character stands on a real ridge at 2 miles, averaging a distant chunk must not produce an apparent mountain in front of them or a false exposed position.

1. **Canonical terrain pyramid.** Level 0 is exact stitched source height samples + deltas, with mesh diagonal fixed. Every coarser tile stores a representative height, *minimum/maximum* height envelope, maximum deviation from the exact surface in metres, sampling/datum and source revision. Height-only averaging is insufficient for visibility.
2. **LOD selection.** Distance rings may start with steps of 1, 2, 4, 8, 16... cells, but screen-space geometric error, camera FOV and viewport resolution drive refine/cull, not distance alone. Scopes/binocular zoom reduce world-space footprint per pixel and therefore demand finer LOD even when far away.
3. **Conservative occlusion.** To determine whether a terrain tile could hide an actor, use a hierarchical min/max elevation envelope to rapidly classify candidates, then fine/full-resolution terrain ray tests for ambiguous cases. Never let a **coarse mesh's render depth alone** decide that a character is hidden. If exact terrain isn't resident, prefetch the needed samples or mark visibility undecided; do not fabricate an occluder or expose a guaranteed hidden target.
4. **Render correction for actor silhouettes.** A correct gameplay ray alone does not solve a wrong visual z-buffer. If a distant actor is potentially visible through a scope, force-fine terrain refinement along the camera-to-actor corridor or use a carefully depth-validated special occlusion path, preserving ridge silhouette. A coarse proxy must not draw over a correctly visible actor. Do not draw the actor through real foreground hills.
5. **Cracks and transitions.** Neighbour LODs have shared logical border samples and deterministic seam-stitch meshes, or skirts only where visually acceptable. Prefer geomorph/hysteresis to hide LOD popping. Physics elevation does not morph with the camera.
6. **Independent budgets.** Streams for terrain height, texture, foliage, and simulation run on separate budgets; prioritise camera/aim corridor and destination warm-up. Avoid synchronous disk/PNG decoding on render thread.
7. **Origin rebasing.** Keep world positions stable in double/global space; shift only frame-local render coordinates and preserve deterministic simulation, raycasts and network messages.

### Required regression: scoped actor behind averaged hill

Construct two adjacent tiles with a narrow saddle and a high shoulder. Place a human-sized actor on an exact lower surface beyond the saddle. Choose a coarse averaged LOD that would place geometry in front of the actor in a naive z-buffer. From a scoped camera several kilometres away:

- Authoritative fine-resolution raycast and hit test agree with actor support and line-of-sight.
- Coarse min/max bound marks the ray ambiguous, forces refine/exact verification and does not classify the actor as hidden without proof.
- Rendered depth shows the actor if actual terrain leaves the ray clear; hides the actor if actual ridge blocks it.
- Results persist after tile unload/reload, tile edge crossing, mismatched neighbor LOD, FOV change/zoom, rapid travel and origin shift.
- Negative counterpart: actor behind a real ridge stays hidden (no see-through LOD bug). Both distant and nearby physics must agree.
- Each assertion needs actual scene coordinates, tile hashes, exact/coarse height samples, LOD selection, ray intersection, screenshot and source SHA.

## 4. Streaming and generation

Use a quadtree for sparse persistent world spatial indexing and a resident-budget LRU for decoded source tiles. The render pipeline creates nested LOD regions around camera and optional explicit interest sources (scope sightline, waypoint, remote player, editor camera). Use async prioritized requests, cancellation, bounded outstanding tasks, deterministic fallback to a known coarser resident tile, and revision-aware eviction. Support cold-start, traversal, teleport, night/fog, and worst-case altitude/scope tests. **No** world-wide allocation proportional to a hypothetical full continent; memory should scale with loaded regions and configured budgets.

LOD summaries must be invalidated/rebuilt after edited source vertices, including any touching neighbouring tiles; stale mesh or stale occlusion summaries are unsafe. A shared source seam edit must modify both sides atomically or reject.

## 5. Human editor modules before agent access

The editor must first let a human create/open a world, choose import vs procedural generation, select tile(s) or rectangular regions, view the stitched topology, set local brush/texture parameters, preview LOD shells, inspect seam defects, change biome and road layers, and save with undo. Loading zones are *optional* authored triggers linking interiors/disconnected worlds; streaming of adjacent outdoor tiles must **not** depend on hand-placed loading volumes.

Dedicated inspectors: tile coordinates, actual world metre bounds, min/max elevations, seam conflicts, LOD error, current resident/cache bytes, streaming requests, visible LOD band, authoritative actor-ground and visibility diagnostic rays. Do not falsely label 2D authoring maps as live game screenshots. Preserve original Beckenburg Castle and map workflows.

**Then** add the separately planned observation/manipulation/verification/headless/MCP agent interfaces to these proven human operations, never a second engine.

## 6. Dependency-ordered implementation and evidence

| Gate | Implementation deliverable | Must prove before marked accepted |
|---|---|---|
| T0 | Preserve finite terrain/loading-zone document and authoring UI, compile and run tests | Existing Castle, Blackpine, water/weather and saves unchanged |
| T1 | Independent Java 17 stitched world manifest, 16-bit grayscale PNG import, exact border checks, deterministic tile generation, true triangle height sampling, bounded LRU | Neighbor/corner/negative-index/absent-tile/byte-validity tests, local Java compile and standalone CI |
| T2 | Human stitched-world editor window; tile viewport, generate/import/export, seam diagnostics and undo | A manually created 3x3 world loads back exactly; corrupt tile import rolls back |
| T3 | Integrate stitched-world canonical height into an *actual outdoor runtime adapter* with matching collision and rendering | Real ground support and renderer sample agree at edges and non-planar diagonals; actual map screenshot |
| T4 | Hierarchical min/max/error LOD summaries and independent stream budgets | Memory bounded independent of world extent; no cracks or stale caches; measured timings |
| T5 | Distance LOD rings + morph and zoom-sensitive screen-space error | FOV/resolution/refinement and quick camera motion; visual frames with real render proof |
| T6 | Long-range accurate visibility/occlusion including scope rays and distant actors | Both scoped buried-actor and real-ridge-hidden tests; origin-shift/ray case pass; no incorrect GPU depth |
| T7 | Biomes, texture layers, erosion and vegetation/roads, weather/water integration | Stable reproducible textures, footprint/collision and no editor/runtime divergence |
| T8 | Multiplayer host-synchronized world/tile revisions and optional loading-zone game handoff | Real host/guest consistency, deterministic spawn, rollback and updates unaffected |
| T9 | Agentic interface after human/runtime modules work | An authorized agent reads/edits/verifies this **same** world, no unproved completion claims |

Statuses until proved: **T0 work in progress; T1–T9 planned**. Earlier Beckenburg 16 terrain foundations and Blackpine grading are not a general stitched/streaming/LOD implementation. Note exact test/CI evidence and revise these statuses per real completed gate.

## 7. Release and resource discipline

Only objective broken build, launch, essential gameplay or required multiplayer/update function blocks shipping. Visual cracks, biased LOD, content/biome mismatches and authoring quality warnings are developer diagnostics unless they objectively break play. Never mask a known failure by disabling a task or replacing a real playable map with synthetic placeholders. No forced push. Preserve current game environments and original art. Maintain source hashes and no unrelated edits.


## 8. Implementation increment — manual stitched world source window (2026-10-31)

T1 source proof: the Windows Java 17 terrain-module acceptance workflow succeeded for commit
ac81d6f92002f3729234592cccd016d4dfdd926e:
https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/41228485115 .
This verifies focused standalone Java module tests, not a running world renderer.

T2 **in progress**: \`StitchedWorldEditor\` is reachable from the existing finite
Terrain / Zones editor via **STITCHED WORLD**. It edits real source-world files
(not a synthetic game scene), supporting world creation/open, tile-coordinate
navigation, bounded 2D tile thumbnails, single-tile and 3×3 deterministic
generation, non-overwriting 16-bit imports, source-identical exports and seam
inspection. A 3×3 generation rejects existing files before writing and attempts
to roll back newly created files if a normal operation fails. Multi-file crash
durability, sculpting with cross-border edits, undo/redo, in-game runtime
integration and render verification **are not implemented** by this step.
Source writes are immediate and visibly labeled as such.

Next: implement a revisioned multi-tile edit transaction that atomically
updates shared-border vertices and supports undo/redo and crash recovery;
then bind the stitched-world source to actual outdoor renderer + physics
(T3). Do not claim T2 accepted until a human can sculpt/save/undo/reload
a real 3x3 world and reproduce seam diagnostics.

## 9. Adaptive authored topology (2026-10-31)

See **[Adaptive Terrain Topology contract](ADAPTIVE_TERRAIN_TOPOLOGY.md)**.
This is a separate *authoring-detail* system layered on stitched 16-bit base
cells, not a camera-dependent distance LOD and not a higher-resolution rewrite
of every source PNG. A base cell may be subdivided recursively only where
needed; nodes use signed global base-cell indexes and dyadic subcell coordinates.
Neighbor edge-break lists permit a later mesh builder to triangulate both
sides of a T-junction against the same positions. Merely generating edge lists
does **not** prove the GPU draws a crack-free surface.

Implementation sources:
- \`AdaptiveTerrainTopology.java\` sparse split/merge, leaf enumeration, signed
  cross-tile neighbor edge breaks, save/load, revision and snapshot restore.
- \`AdaptiveTopologyEditor.java\` human-operated 2D quadtree inspector, refine,
  merge, undo/redo, explicit save with re-open verification.
- \`StitchedWorldEditor.java\` provides the ADAPTIVE TOPOLOGY entry point.
- \`AdaptiveTerrainTopologyTest.java\` tests nested splits, cross-source-tile
  boundaries, negative indexes, failed mutations, save/reload and malformed files.

**Status:** A1 Java source + focused CI acceptance passed at
\`e7eca44c34ff7273312b0745c472f6d3f1ad4f4c\`
([workflow run 41229681626](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/41229681626)).
A2 panel source added; the post-UI focused compile, installed Windows editor
interaction and real screenshots need separate evidence. No authored
subvertex height displacement yet (A3). The runtime renderer, collision/LOS
and mesh stitching remain unimplemented (A4+); don't count topology metadata
as visual/physical terrain detail or proof of distance visibility correctness.

Immediate dependency order: A2 manual save/reload acceptance, A3 globally
shared vertex height delta plus cross-border transactions, A4 conforming
triangulation and real runtime integration, then existing LOD/scoped actor
verification T4–T6, agent access last.



---

### Source: Authored adaptive terrain topology

**Original repository path:** `docs/development/ADAPTIVE_TERRAIN_TOPOLOGY.md`.

# Beckenburg Adaptive Terrain Topology — design and staged implementation

**2026-10-31 — scope:** human-authored, persistent, sparse recursive terrain refinement *within* independently stitched heightmap tiles. This is **not** a replacement for the tiled 16-bit heightmap source and **not** distance-based rendering LOD. Continue human-accessible editor modules before any machine/agent interface.

## 1. Sketch: topology is authored where needed

```text
STITCHED 16-BIT SOURCE TILES (tile edges share exact height values)
+---------------------+---------------------+
| coarse hillside     | cliff base          |
| . . . . . . . . . . | . . . . . . . . . . |
|          +----------+----------+          |
|          |  fine    |  coarse  |          |
|          | +--+--+  |          |          |
|          | |++|++|  |          |          |
|          | +--+--+  |          |          |
|          +----------+----------+          |
+---------------------+---------------------+

One original heightmap quad
 depth 0: [           one base cell           ]
 depth 1: [        ][        ]  each axis => 4 children
 depth 2: [    ][    ][    ][    ]  selected children subdivide again
 depth 3+: recursive refinement only where authoring requires it

Independent, orthogonal systems:
 SOURCE: stitched 16-bit heights and their provenance
 AUTHORING: persistent quadtree split nodes + future sparse high-resolution height deltas
 DISPLAY: distance/zoom-dependent mesh LOD derived from canonical authored world
```

Note the drawings illustrate *quadtree subdivision*, not proof of a crack-free runtime mesh.

## 2. Authoritative coordinates, boundaries and persistence

- Global **X/Z** with Y upwards (metres). A canonical source tile contains N×N *base cells* plus N+1 height samples on each side; heightmap decoding/triangles remain the responsibility of `StitchedHeightmapWorld`.
- An adaptive root is exactly one base cell indexed by globally signed integer `(gx,gz)`; e.g. tile boundary cell X `(tx*N+N-1)` is adjacent to `((tx+1)*N)`. Do not introduce tile-local boundary identities that could disagree.
- Node address `(gx,gz,depth,localX,localZ)`, with `0 <= localX,localZ < 2^depth`. Nodes subdivide into NW/NE/SW/SE quadrants. Only explicitly split nodes persist; unmodified cells consume zero per-cell storage. Parent closure, max subdivision depth, a minimum physical edge length and a node-budget limit prevent runaway allocations.
- For portable, exact edge comparison use integer **dyadic** coordinates at a fixed maximum subdivision lattice, `unit=2^MAX_DEPTH` per base cell. Negative global coordinates must use floor division. Refinement/splitting of base-cell edges must work across PNG tile boundaries.
- A new vertex created by subdivision initially samples the *exact source triangle* from the stitched heightmap (not bilinear averaging). Splitting **does not raise/lower** terrain by itself.
- Future authored displacement is a separate sparse vertex layer keyed by **global dyadic integer coordinates**, with one displacement value per shared vertex. An edit touching a tile edge is a multi-tile transaction; never create two different heights for the same vertex.
- Topology files are versioned, bounded, atomic-save, tied to the heightmap world's exact grid/settings and validated on load. Reopen must preserve the same split tree and edge data. Source tile PNGs are never rewritten by splitting/merging metadata.

## 3. Boundary conformity: no naive T-junction meshes

If a leaf's opposite side is subdivided more deeply, the coarse leaf must *also* expose its neighbor's edge-break positions to the eventual mesh builder. The geometry adapter is required to triangulate against those shared break positions **using identical positions and heights**. Returning lists of edge breaks is *not* sufficient proof of a watertight GPU mesh.

We choose **edge-conforming polygon triangulation** over automatically refining a whole continent to the same depth. Recursive split balancing (neighbor depth gap <= 1) may be added as a bounded optimization, not a hidden requirement of editing. Unequal depths can meet only when the mesh builder consumes all edge breaks; arbitrary-depth mismatch must not silently become skirts or a visual-only seam mask.

The source core must expose: leaf enumeration, world/dyadic bounds, neighbor edge-break positions, source-sample lookup, split/merge/snapshot/readback and explicit failure modes.

**Failure cases:** absent/unloaded source tile, depth/budget overflow, non-leaf split, missing parent, conflicting PNG edges, malformed/cross-world topology document, save failure, stale revision, and wrong-thread UI use. No operation should partially mutate state on a validation error.

## 4. Terrain physics, distant vision and limits of heightfields

- **Height ownership:** before runtime adapter T3, this module only stores authoring topology. Nothing should pretend that a refined mountain automatically appears in gameplay. The runtime adapter must use the identical triangulated authored surface for ground contact, projectile/ray queries, AI navigation and mesh emission.
- **LOD is downstream:** authored splits do not disappear because camera distance changes. Distant LOD may approximate, but must retain an error envelope and fine-resolution corridor refinement for scopes and distant actors. Exact terrain determines true occlusion; GPU depth must also be refined or corrected to avoid burying visible actors behind coarse proxy geometry.
- **Caves/overhangs:** a heightfield (even adaptively refined) still represents only one Y per X/Z. Use separately owned solid mesh/volumetric geometry and ground cutout/portal masks for caves, bridges, tunnels and cliffs with undersides.
- **Material detail:** textures/atlas UV density, splat masks, decals and surface displacement should use independent resolution policy. Increasing geometric topology is not the same as increasing texture resolution; prevent atlas bleeding across tile/LOD seams.
- **Precision:** double-precision global metres (and integer global dyadic indices) for authoritative address; subtract rebased render origin before GPU float conversion.

## 5. Stages, acceptance and status

| Stage | Deliverable | Tests |
|---|---|---|
| A0 DESIGN | This explicit contract and dependency map | Correct base-cell/address examples and failure cases |
| A1 CORE | Sparse Java 17 split/merge quadtree, leaf enumeration, global dyadic bounds, cross-cell edge breaks, snapshot/restore, bounded atomic topology file | Nested splits, signed coordinates, 16-bit tile boundary, no source change, save/reload, corrupt file rejection, node limits |
| A2 HUMAN UI | Choose tile/base cell/subcell, refine/merge, undo/redo, inspect topology density and edge breaks; save explicitly | Human edits 3×3 world, sees density change, save/reopen exact, existing editor unchanged |
| A3 HEIGHT DETAIL | Non-destructive sparse dyadic vertex height offsets, world ownership, atomic cross-border edits and revision/invalidation | Cross-border brush, no cracks, failed batch rolled back, true triangle surface and saved readback |
| A4 GAME ADAPTER | Conforming triangle generation, collision/nav/ray consistency, real-world rendering and chunk invalidation | Captured real renderer, terrain foot heights/edge crossing, no visible seam; runtime uses authored surface |
| A5 DISTANCE & SCOPES | LOD selection, min/max error envelopes, scope/actor correctness | Human-sized distant actor genuinely visible vs ridge-hidden, real rendered z-buffer, teleport/zoom/origin-shift |
| A6 AGENT ACCESS | Observe, manipulate and verify same approved editor operations, later | Authorized real-scene readback and undo; no parallel simulated scene |

**Current implementation:** A0 spec committed. A1 core and regression tests
passed Windows Java 17 focused CI at e7eca44c ([run 41229681626](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/41229681626)).
A2 human interface source was added subsequently; compilation and a physical
Windows UI interaction still require separate verification. A3–A6 remain planned. A1 or A2 is **not** A3–A6. Finite TerrainZoneDocument and existing loading-zone handoff remain independent.

## 6. Reference basis

- Frostbite *Adaptive Hardware-accelerated Terrain Tessellation* documents T-vertices and stitch permutations: https://media.contentapi.ea.com/content/dam/eacom/frostbite/files/adaptive-terrain-tessellation.pdf
- Adaptive mesh discussion and edge-split remedy: https://www.mdpi.com/2071-1050/11/7/2137
- Quadtree/continuous LOD historical reference: https://courses.cs.duke.edu/cps124/fall02/notes/12_datastructures/lod_terrain.html

These are architectural references, not imported dependencies or assertions that our engine has completed its renderer.


## 7. Reuse of the model-showcase DEV room (2026-10-31)

User requested topology testing alongside existing exhibits. Beckenburg's
\`--dev-room\` is a separate actual-renderer *model showroom*, not a gameplay
heightfield map, so we reuse its renderer and free camera without registering
the QA patch as a mission or moving the six model stands.

- \`DevTerrainTopologyLab\` creates/reopens an isolated 2-tile 16-bit test world
  at \`~/.beckenburg/dev-room/topology-qa\`. It seeds a depth-1/depth-2 example
  immediately against the neighboring unsplit cell at the original PNG boundary;
  an existing QA source is never overwritten.
- \`DevAssetShowroom\` adds explicit on/off, edit and reload controls. The patch
  is opt-in in an unused side aisle. Existing model catalog and unmodified
  3-argument \`SceneRenderer.drawDevAssetRoom\` still render exactly as before.
- \`DevTerrainTopologyLab.build\` enumerates real authored leaves, gathers
  each neighbor's global edge-break positions, triangulates an edge-conforming
  polygon fan, and samples the **actual source triangle** height. Its triangles
  are passed into the existing software 3D renderer for live depth tests.
  Depth colors are diagnostic. There is **no independent authored vertex
  displacement, terrain-body collision, lighting/texture atlas integration,
  character occlusion proof or gameplay runtime adapter** yet.
- Reopen / reload reads actual saved topology from disk. A malformed document
  reports failure and keeps the prior lab geometry rather than faking success.
- Optional headless DEV capture: \`DevAssetShowroom --dev-room --terrain-qa-capture
  --output=build/dev-room\` writes a *real* scene renderer screenshot and
  a source/triangle-count report. The visual review CI treats the capture as
  advisory. Verify Windows CI success and capture artifact separately before
  calling the DEV lab fully accepted.

QA tasks: manually split tile 0 cell (15,7) along the tile 1 boundary, inspect
the resulting diagnostic subdivisions in the real 3D renderer, save/reload,
verify both coarse/fine sides draw without a visible gap, compare source PNG
hashes, and document FPS and rendering artifacts. The existing model asset
cameras and screenshots must still behave unchanged.

This is **a staging fixture for A2 and early A4**, not proof that A3 authored
sculpting, physical collision or T5/T6 distant scope occlusion works.


## 8. Cutout and multi-level navigation requirements (2026-10-31)

**Correction to the earlier design:** adaptive topology must support punching true holes in terrain and multiple overlapping 3D floors/ceilings at the same X/Z for caves, caverns, mines, bunkers and sewers. A heightfield alone cannot provide this. Sparse cutout leaf flags are now in authoring format v2; the human editor has cut/restore controls, and the isolated DEV patch suppresses cutout triangles. Underground mesh, matching collision, portal connectivity and layered gameplay are NOT yet implemented.

See [Layered Terrain and Real Excavation](TERRAIN_LAYERED_CAVITIES.md) for authored surface cuts, 3D subsurface layers, entrance perimeter welding and collision/occlusion acceptance.

**Separate requirement:** low/high rendering mesh densities must not respectively under- or over-sample NPC waypoints. Use independently built adaptive nav regions with physical clearance sampling, explicit layer/portal IDs, hierarchical routes and verified local swept movement. See [Adaptive Multi-Layer Navigation](ADAPTIVE_MULTILAYER_NAVIGATION.md) for the staged design and diagnostics. Camera/render LOD must never become NPC collision authority.



---

### Source: Layered terrain cavities, real holes, caves and entrances

**Original repository path:** `docs/development/TERRAIN_LAYERED_CAVITIES.md`.

# Layered Terrain and Real Excavation — Beckenburg Editor

**Requirement confirmed 2026-10-31:** Adaptive terrain authoring MUST make genuine holes, caves, cavern networks, bunkers, sewer systems, tunnels, mines and multiple stacked walkable levels straightforward. This is a human-editor feature before any agentic interface.

## Geometry and data ownership

1. **Stitched source heightfield:** immutable 16-bit reference samples. One Y per X/Z cannot represent a cave roof and cave floor simultaneously.
2. **Sparse adaptive surface topology:** independently authored quadtree split nodes and persistent *open* leaf masks. A cut removes the actual terrain faces; it is not transparency, a lowered height, or a decal. Undo and restore expose the original source geometry. Shared coordinates cross PNG tile borders.
3. **Layered 3D submeshes:** separately addressable geometry, in arbitrary world X/Y/Z, supporting ceilings, floors, roofs, walls, caves with overhangs and tunnels under other tunnels. One X/Z position may contain multiple valid surfaces.
4. **Entrance connections:** explicit perimeter/portal reference joining surface cutout edges to an underground mesh opening; optional rock rim, lining, shaft, door, stair, ladder or ramp. An incomplete hole is explicitly an open void and receives no invented bottom or cap.

Each solid surface has a stable layer ID and revisions independent of rendering LOD. Physics, nav and occlusion derive from authoritative source+cutouts+3D submeshes, never from averaged distant graphics.

## User-facing operations

- **Cut Hole**, **Restore Surface** on selected leaf (already implemented as authoring API); extend to brush, polygon/ring and silhouette-aware selection. All edits reversible, bounded and atomic.
- **Refine Hole Rim** preserves open state upon split. **Merge** rejects mixed open/solid areas unless the user explicitly fills/cuts to make them uniform; no accidental tunnel sealing.
- **Create Underground Layer** with depth, width/ceiling profiles and textured geometry. Support arbitrary branching and crossing paths.
- **Extrude Entrance** consumes the open perimeter, generates a matched side-wall loop and tunnel cross-section, then connects to a placed cavern/sewer/bunker mesh. Keep portal connections inspectable.
- **Layer panel**, cutaway/section view, vertical slice, wireframe and entrance graph. Editors can isolate upper ground, roof, interiors and individual underground strata without changing collision.
- **Local undo/redo, readback and revisioned save** must include mask, boundary, layer meshes, portals and invalidation references together.

## Real gameplay consequences

- **Collision:** missing terrain faces must also be missing in collision and line/ray tests. A hunter entering a hole hits the actual cavity floor, not an invisible source-heightfield lid. Support valid falls and intentional dangerous voids.
- **AI navigation:** a route crosses the cutout only via a walkable ramp, stair, ladder or other explicit traversal link. Navigating to an opening is not permission to walk across it. The multilayer navigation policy is specified in [ADAPTIVE_MULTILAYER_NAVIGATION.md](ADAPTIVE_MULTILAYER_NAVIGATION.md).
- **Lighting/visibility:** cave mouth, tunnel and ceiling surfaces occlude correctly; sky exposure and placed lights recognize real apertures. The far visual terrain proxy must not visually seal an entrance that is truly open. Scopes and distant actors must be evaluated against exact geometry.
- **Streaming:** when an entrance is reachable, its immediate connected underground chunk is prepared before collision/navigation transition. Links remain valid across map tiles and stacked levels.
- **Water/hazards:** floods, sewer flow, air and material effects are independently authored world volumes and use real enclosure and passage data.
- **Performance:** rebuild only edited cells, intersecting seams, portals and invalidated collision/nav/light fragments. Preserve GPU batching and bounded persistent caches. No global re-bake per frame.

## Current milestone and tests (not full runtime support)

**Implemented in code, awaiting CI readback:** AdaptiveTerrainTopology v2 persists sparse hole leaves with backwards read of v1. It offers `punch`, `fill`, snapshot/restore, safe refinement and guarded merge. AdaptiveTopologyEditor exposes CUT HOLE and RESTORE SURFACE; the isolated DevTerrainTopologyLab skips cutout triangles when drawing. The DEV-room's preexisting studio floor can still appear beneath the sample terrain: that is **not** a real excavated chamber.

**Still missing:** cave mesh authoring, tunnel/portal extrusion, connected collision/LOS/nav, actual gameplay cutouts, distant LOD/occlusion parity, and integrated layer-specific saves. Do not label a 3D diagnostic render as proof of these.

### Acceptance path

1. Round-trip a single subdivided hole, half-filled rim, tile-border opening and legacy uncut v1 document. Confirm original 16-bit PNGs retain byte identity.
2. In real dev-room screenshots, prove a cut deletes terrain triangles; follow-up build must expose a separate cavity floor/wall and render it through a real opening.
3. Attach a multi-branch cave beneath intact hillside with one real entrance, stairs to second stratum and sewer crossing below; test distinct Y layers at equal X/Z.
4. Exercise in-game collision, AI crossing, flashlight shadows, camera visibility, scope distance LOD and water volume boundaries under randomized edit revisions.
5. Profile localized regeneration versus area size; art judgments remain advisory, broken functionality blocks acceptance.



---

### Source: Adaptive multilayer navigation, flood controls and three seam methods

**Original repository path:** `docs/development/ADAPTIVE_MULTILAYER_NAVIGATION.md`.

# Adaptive Multi-Layer Navigation — Beckenburg Terrain, Hunters and Ghosts

**Requirement confirmed 2026-10-31:** NPC routes must be adaptive under both low- and high-density topology, and through stacked underground spaces. Rendering mesh vertex count **must not** dictate navigation node count. Human editor modules come first; agents later invoke the same sanctioned operations.

## The critical separation

1. **Exact physical world:** stitched terrain source, authored sparse height detail (when implemented), cutout masks, doors, placed objects, underground layered solid geometry and authoritative collision.
2. **Navigation surface:** agent-specific traversable regions constructed by validating precise physical geometry. This is independent of camera/distance/rendering LOD. A coarse-looking terrain patch can still be scanned at finer sampling to establish gradient, obstacle and clearance limits.
3. **Search representation:** hierarchical region/portal graph with compact polygonal areas (and per-layer ID), plus local high-detail connections only at difficult transitions. A forest clearing does NOT require one waypoint per visual vertex; a narrow sewer turn DOES require enough local samples even when the visible mesh is coarse.
4. **Behavior:** existing HouseMap and ghost room/portal rules remain authoritative for their current maps. A new world-navigation adapter refines local routes, without quietly overriding ghost door restrictions, evidence perception, permissions or van boundaries.

Do not derive walkability from a visual LOD approximation or use a geometry simplifier whose error can mask a hole, cliff or doorway.

## Adaptive construction

- Partition walkable geometry into **regions** by tile, loaded chunk, vertical layer and connected components. A region may contain large convex or locally simple polygons, not thousands of unneeded triangles.
- Subdivide locally where **traversal complexity** demands: sharp elevation gradient, step height, fall hazard, tight clearance, obstacle proximity, hole boundary, portal, slope break, branching corridor, AI turn radius or a jump/ladder/ramp link.
- For *coarse* source terrain, probe the canonical collision/height function adaptively within cells; refine until surface error, maximum slope and clearance are bounded. Never accept 'looks flat at vertices' as proof of safe passage.
- For *fine* visual mesh, simplify by merging adjacent navigable polygons **only if** exact collision samples and swept-volume checks prove consistent clearance, slope, connectivity and no missed hazard/portal. Do not store a waypoint for every decorative triangle.
- Preserve critical nodes: entrances, narrow choke points, bridges, stairs, vertical connections, door thresholds, hazard boundaries and cover/visibility-critical corners. These are topology constraints, not optional low-detail approximations.
- Make the builder deterministic, budgeted, incremental and revision-based. A changed terrain leaf or cave wall invalidates only touching navigation regions and portal links. Retain stable IDs for unaffected regions. Never silently mark dirty chunks as walkable.
- Neighbor regions connect through **explicit traversable portals**, with measured width/height and ability requirements. A cutout is initially a void: it becomes an entrance link only if a valid floor, ramp, stair, ladder or drop behavior connects it.

## Runtime routing

**Stage A — strategic route:** A* or another suitable bounded search through region/portal graph, from start layer to destination layer. Cost accounts for actual distance, door effort, exposure, environmental hazards and agent abilities. Never authorize crossing a closed/blocked or player-only boundary by guessing.

**Stage B — local route:** Corridor/funnel smoothing through navigable polygons; add local intermediate points only as required by geometry and turning constraints. Validate each segment against authoritative swept collision. Adjacent avatars and movable furniture are handled by short-horizon local avoidance, not full navmesh rebuild every tick.

**Stage C — recovery:** If a segment fails real collision, mark its local edge invalid, locally refine sampling or replan around it. Never repeatedly push an NPC into an invisible slope or teleport through rock. Surface/floor Y is resolved with layer ID and actual collision, *not* closest heightfield Y. Avoid allowing agents on underground sewer level to snap to the surface road.

**Agent profiles:** hunter, crouching hunter, ghost (with explicit ghost-specific capabilities), and optional larger entities have height, radius, slope, step, jump/drop, ladder, door and water tolerances. A single mesh cache may share raw geometry, but passability/links differ per ability class; ghost powers require explicit rules rather than universal clipping.

**Dynamic changes:** open/closed doors, temporary equipment, moved clutter, blocked rubble, terrain edits and water changes update edge costs/availability or localized mesh revisions. Live multiplayer server owns route collision truth, not an individual client's approximate LOD.

## LOD, scopes and tall worlds

- Camera distance should NEVER lower authoritative navigation accuracy; nav streaming is based on active NPCs, routes, portals and nearby goals, independently of view.
- A faraway actor may use cheaper **planning** across coarse region graph, but exact local movement and position need high-fidelity surfaces once simulated. Predictive loading along path avoids stopping at a cave threshold.
- Visual LOD and depth occlusion must honor cutouts, passage envelopes and real cave roofs. Scope zoom may force visual refinement, but should not rewrite a NPC's route or move it to a different floor.
- Track `(regionId, layerId, polygonId)` for the agent; X/Z alone cannot identify a ground plane in a multilayer scene.

## Tests and instrumentation

Test all examples with *physical* route probes and actor simulation, not only generated graph connectivity:

| Environment | Test |
|---|---|
| Huge nearly flat meadow with dense decorative triangles | Few nav regions, no explosion in graph/search cost |
| Coarsely sampled hill with narrow ravine | Collision probing detects ravine and builds sufficient safe local points |
| High-detail rubble and narrow furniture passages | Clearance-constrained route correctly rejects oversized agent |
| Hole punched in field | No false edge crosses the open void; explicit bridge/ramp works |
| Cutout across stitched PNG border | Stable links and equal positions on both sides |
| Two rooms stacked at same X/Z | Independent nav layer IDs, no wrong-floor snap |
| Bunker stairs, cave branches, deeper sewer crossing | Connectivity only through authored vertical/portal links |
| Doors, fallen clutter, flooding | Incremental local invalidation, route recovery without infinite collision loop |
| Scope observing NPC a mile away | NPC remains on correct terrain; visual occlusion does not bury/expose it incorrectly |

Capture deterministic seeds, generated region/node counts, memory/CPU build times, path search p50/p95, max error bounds, failed segment count, number of local invalidations and behavior traces. Maintain acceptance tests for ghost/Hunter movement rules separately. Visual screenshots are advisory; failure to move/collide/navigate is objective functionality.

## Implementation staging

**N0 design:** document system and expose diagnostic requirements (this milestone).
**N1 geometry adapter:** build authoritative layered collision/height samples and cutout boundaries, with stable per-layer IDs.
**N2 sparse region builder:** generate verified walkable polygons with clearance/slope error bounds; join layer-aware portals.
**N3 hierarchical search:** high-level route + local funnel/swept-volume validation; agent profiles and recovery.
**N4 incremental updates:** doors, moving clutter, terrain edits, water, streaming; replan only impacted regions.
**N5 DEV room and AI simulation:** build indoor/outdoor/cave test fixtures, display regions/portals, allow six AI hunters + ghost and admin observer, collect real movement traces and collision failures.
**N6 agentic editor:** make agents read and invoke approved editor APIs with undo and real-scene readback.

**Status (2026-10-31):** N0 is documented. N1 now has a **standalone diagnostic/query prototype** in `editor/LayeredTerrainQueries.java` that samples real stitched source triangles, excludes punched cutout leaves, retains independent floor layer IDs, and checks AABB solids, capsule-footprint support and headroom. N2 has a **standalone adaptive region-graph prototype** in `editor/AdaptiveNavigationRegions.java` with bounded split budgets, exact source-gradient inspection, physical portal validation, confirmed portal waypoints and explicit ability-gated vertical links. The corresponding deterministic acceptance tests are `LayeredTerrainQueriesTest` and `AdaptiveNavigationRegionsTest`. The initial Windows CI for N1/N2 passed at [run 41231554595](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/41231554595); revised waypoint validation passed at [run 41231621400](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/41231621400). The later steep-source-PNG regression requires its own CI result, and must not be counted as passed before readback.

The real `DevTerrainTopologyLab` now computes diagnostic surface nav regions/links and a sample cross-cutout route beside its real renderer patch, exposed in `DevAssetShowroom` status and headless report. Physical cave mesh authoring, fully conforming 3D collision, production runtime movement, navmesh integration with `GhostPracticeHunterAi` and `GhostAiNavigationAudit`, multiplayer replication, dynamic stream invalidation, and live six-hunter simulation **remain outstanding**. An explicit transition in the graph describes an author-approved connection; it is not yet proof that the actual ladder, staircase, animation or swept movement implementation exists. N3–N6 are not implemented, and the prototype must not replace the game's existing movement authority until runtime parity is tested.


## N2a — Authored seed flood, selective regression and protected transition barriers (2026-10-31)

**Editor UI:** Open a stitched source world, choose **Adaptive Topology → NAV FLOOD EDITOR**. The dedicated Swing editor displays an up-to-8×8-source-cell area in actual world coordinates.

- **Continuous rebuild:** persisted per world; when enabled, regenerate a diagnostic graph after each author edit and when source/topology files change. Builds use a worker rather than freezing the editor. When disabled, changes still save but the graph only updates via **FLOOD FILL PULSE**. The **minimum region size** is adjustable separately from the render topology to trade local detail against graph size.
- **Seed at click:** authored entry/start for the reachable-component flood. If no seed has been authored, the builder returns all physically valid candidate regions for inspection. With one or more seeds, only reachable regions are in the final navigation graph.
- **Draw blocked gate:** click two axis-aligned segment endpoints across an open doorway, loading-zone transition, prohibited passage or NPC-only boundary. The generator refuses graph connections crossing the segment even when the 3D models appear open. A segment only blocks its actual span; AI can legitimately go around its ends if geometry permits. Use persistent trimming or enclosing collision barriers when a whole destination instance must be excluded.
- **Trim selected:** click a generated polygon, then permanently remove its world-space rectangle from the navigable output. **Trim connected:** deliberately remove the entire displayed connected branch, with a confirmation count. Rebuilds never restore these authored masks. The editor retains **grey, unreachable pre-flood candidate polygons** past a barred gateway so you can select the disconnected side and permanently regress it without ever allowing the AI to navigate there. The flood-reachable output alone is used for routes.
- **Untrim last / Remove last gate / Remove last seed / Undo / Redo:** edit the separate persisted authoring document. Save and immediate disk readback verify the authored state.

**Storage:** \`navmesh-authoring.bin\` is separate from source \`tile_*.png\` and \`adaptive-topology.bin\`. It stores stable world-space seeds, exclusions, blocked gate segments, and manual/continuous mode. Generated region indices are not permanent; only source-coordinate masks are. The document uses a bounded binary format and atomic replacement.

**Builder integration:** \`AdaptiveNavigationRegions.build(..., AdaptiveNavMeshEdits, minCell)\` augments the existing exact-collision subdivider: refine near cut masks and gate interiors, reject regions intersecting forbidden masks, omit blocked peer connections and ability-restricted vertical links, and breadth-first flood from seeds over the remaining connectivity. No renderer LOD determines this output. Graphs reject stale authored/topology revisions. \`DevTerrainTopologyLab\` uses the same saved masks on QA refresh.

**Acceptance:** \`AdaptiveNavMeshEditsTest\` exercises physical geometry remaining open behind a blocked level-transition gate, persistent no-regrowth after seed flood, trimming sub-areas from otherwise coarse navigable regions, undo snapshots, source tile preservation, and continuous mode persistence. It is a functional Windows Java 17 test, not a cosmetic release blocker.

**Not yet integrated:** A live runtime agent does not consume this authored graph, and a blocked nav edge is *not* an actual collision wall. Layered cave mesh and production scene physics remain separate milestones. An editor window preview is not proof of Hunter/ghost AI traversal.


## N2b — Persisted underground authoring from the navigation editor (2026-10-31)

**Open:** Stitched World → Adaptive Topology → NAV FLOOD EDITOR → **UNDERGROUND GEOMETRY**. The layered geometry window lists and allows adding, revising and deleting independent floors, axis-aligned solids and cross-layer traversal links. Undo/redo and save perform immediate disk readback. Source heightmap PNGs and adaptive cutout topology are untouched.

- **Floor:** named layer (for example \`bunker\` or \`sewer\`), world-space X/Z rectangle, base height and X/Z rise. Multiple nonoverlapping slabs can share one layer. Overlapping slabs on the same layer are rejected.
- **Solid:** unique ID plus world-space 3D bounding box; collision-query clearance and navigation authoring reject impassable solid walls and ceilings. This does **not** create a visible wall mesh.
- **Connection:** explicit \`STAIRS\`, \`LADDER\` or \`DROP\` link between two named layers at authored endpoints. Both endpoints must be within their destination support slabs; links with orphaned layers are rejected. Cross-layer routes remain capability-gated.
- **Viewport:** choose the layer to inspect; flood rebuild consumes the saved floor/solid/link data. Existing nav seeds, gates and exclusions remain layer-specific. Editor overlay uses teal floor outlines, white obstacle rectangles and magenta transition markers. Agent ladder/drop capability toggles affect reachability.
- **Storage:** separate bounded, atomically saved \`layered-geometry.bin\` alongside \`navmesh-authoring.bin\` and \`adaptive-topology.bin\`, leaving the source \`tile_*.png\` unchanged. Authored geometry is independent of generated nav region IDs.

**Test:** \`LayeredTerrainDocumentTest\` validates source tile preservation, three physically distinct floors, stairs/ladder capability restrictions, obstacle collision, persistence and readback, overlap rejection, orphan-link rejection and snapshot restoration. It runs in Windows Java 17 focused terrain acceptance.

**Important:** This milestone supplies **editor-owned physical query geometry**, not full cave meshes, mesh welding at the surface cutouts, model rendering, or live game collision. A stair link in the nav graph is author intent, not an animated staircase or automatic level-instance transfer. Those runtime and visual connections remain subsequent tasks.


## N3 — Three distinct terrain entrance seam methods (2026-10-31)

Open Stitched World → Adaptive Topology → ENTRANCE SEAMS 1/2/3 (also linked from Smooth Ramp Entrances). All three are topology operations, not level heights:

1. **Method 1 — NEW INTERIOR SEAM.** Add a source-tile interior seam along adaptive-grid-aligned endpoints. The explicit CONFORM LEAVES + PUNCH TABS command refines actual quadtree leaves until the seam coincides with their edges. Crossing the new seam on the outside surface is blocked. A separate CUT HOLE operation, or Method 3, can create the corresponding opening. The seam retains its underground destination layer identity.
2. **Method 2 — DE-STITCH TILE EDGE.** Select a real existing source-tile boundary. The surface graph stops connecting across this boundary, preserving both underlying heightmap tiles while recording the intended underground destination. Rendering/mesh ownership of the replaced edge remains a downstream integration; it is not automatically connected to an underground render tile yet.
3. **Method 3 — DETACHED TAB / PUNCH.** A matching Method 1 seam is required along one edge of a rectangular tab. The explicit CONFORM command splits and punches the outer source leaves underneath this patch, and the physical query module reassigns the *same original height surface* to an independent named navigation layer. Normal outdoor surface connections do not regenerate over it. A ramp mouth inside the tab stitches to this distinct tab layer rather than to the main outdoor surface.

The authored operation document is entrance-seams.bin, independent of the source PNG, adaptive-topology.bin, layered-geometry.bin, ramp-entrances.bin and navmesh-authoring.bin. Editor masks have undo/redo and disk readback. The explicit physical CONFORM command also provides REVERT LAST CONFORM, which refuses to erase unrelated subsequent topology edits. Original 16-bit source height pixels are never rewritten by this work.

The adaptive flood fill now loads method 1/2 seam barriers, method 3 independent tab supports, and explicit ramp-to-tab navigation endpoints. It visually marks the three kinds of opening in the 2D authoring map. The targeted Java 17 test is EntranceSeamDocumentTest: subdivision, tile de-stitch separation, tab punching and standability, no outdoor AI cross-edge route, disk persistence, idempotent reapplication and source PNG seam preservation.

**Status boundary:** this is actual editor-side collision/query/topology authoring. Full 3D cave-mouth mesh emission, visual subterranean-tile re-welding, GPU rendering and live Hunter/ghost movement integration are still future work. The editor may not claim an authored seam is already a playable cave entrance.


## N4 — Real authored 3D entrance mesh emission and inspection (2026-10-31)

From **Smooth Ramp Entrances**, select a ramp and press **3D MESH PREVIEW** or **EXPORT MESH OBJ**. From **ENTRANCE SEAMS 1/2/3**, select a seam or tab and use the same 3D preview/export commands. The preview is a rotatable, zoomable projection of actual generated mesh triangles, labeled explicitly as an editor inspection view and **not an in-game screenshot**.

The source-consistent geometry generator, \`TerrainEntranceMesh\`, emits:

- Outdoor terrain triangles from **real 16-bit heightmap pixel samples** and actual adaptive quadtree leaves; polygon edge vertices follow \`AdaptiveTerrainTopology.edgeBreaks\`, including fine/coarse neighbors. Split terrain borders are exported with separately owned vertex indices rather than automatically rewelded.
- The original-height **detached navigation tab** as its own mesh part. Punched outer surface leaves remain absent from the outdoor surface mesh.
- Continuous cubic-Hermite ramp triangles using the **identical height function** used by collision/navigation, including C1-eased mouth and interior grades.
- Independently authored underground floor slabs, omitting surface patches where the ramp overlaps the destination floor.
- **Exposed rock cheek walls** *only* where a real cutout meets a physical ramp and an adjoining uncut terrain edge. Wall endpoints use source terrain heights and ramp heights; no arbitrary-depth fake cavern wall or unrequested backdrop is invented.

The **geometry-only OBJ** output has named groups for surface seam sides, detached tabs, ramps, floors and rock edges. It includes UV coordinates but **does not fabricate a replacement diffuse texture**. The exporter parses the written file through \`EditorMeshPipeline.read\` before publishing it, and the editor performs a second readback after export. Geometry is limited to 16,000 triangles and a bounded source-cell region: larger entrances must be exported in chunks, not silently downsampled.

Windows Java 17 \`TerrainEntranceMeshTest\` checks adaptive mesh face orientation and source-height agreement, separate sides of existing and new cut seams, preserved detached tab triangles, ramp/query height agreement, non-fabricated cave cheek walls, OBJ roundtrip group and face preservation, sparse outer-tile borders and original source heightmap seam integrity. This test blocks only on a functional mesh/export regression, not visual style.

**Still pending:** hooking emitted OBJ/GLB or mesh batches into the live game-level renderer and collision/AI runtime; dynamically streaming many tiles, building full tunnel/ceiling walls beyond authored rock edges, and texture/material assignment from the approved asset library. OBJ output is an inspectable intermediate asset, not proof of a finished in-game cave.


## N5 — Reusable runtime-scene document and terrain package module (2026-10-31)

The portable Java 17 editor kit now provides two deliberately separate functional modules:

- **RuntimeSceneDocument** serializes any game-owned RuntimeScene adapter into an atomic, versioned \`scene.bin\` with stable scene ID, node IDs, hierarchy, transforms, layers, component types and string properties. On load it enforces bounds, rejects bad parent graphs, checks deterministic disk readback, and can restore into an existing matching runtime scene without overwriting a different level.
- **TerrainScenePackage** is an opt-in EnvironmentModule with \`AUTHORING\`, \`SERIALIZATION\`, and \`DIAGNOSTICS\` capabilities. Given saved source terrain and explicit patch bounds, it builds the *real* TerrainEntranceMesh, publishes geometry-only OBJ into \`scene-package/meshes/<chunk>.obj\`, checks the importer and SHA-256 hash, and registers a stable \`terrain-<chunk>\` scene node carrying a \`world://\` URI, mesh part/triangle counts and source-query file references. Regenerating a named chunk replaces only its owned mesh/node, not unrelated props or a foreign OBJ. Failed operations restore the scene and previous geometry. The scene itself is saved in \`scene-package/scene.bin\`.

**Source staleness safeguard:** published scene nodes also store a SHA-256 fingerprint of the source 16-bit tiles covering their patch plus the authored adaptive topology, seam, ramp, and underground-layer documents. **VERIFY PACKAGE** distinguishes an untouched OBJ from an up-to-date OBJ; changing those inputs reports **STALE** until the chunk is rebuilt. Existing assets are never silently regenerated or overwritten because of a warning.

**Editor route:** Stitched World → **TERRAIN SCENE PACKAGE**. Select tile, local cell offsets and a bounded patch side; specify the stable chunk ID. **3D MESH PREVIEW** generates an inspection view without saving it. **PUBLISH PATCH TO SCENE** bakes OBJ and saves the authoritative scene manifest asynchronously. **VERIFY PACKAGE** checks current files and hashes; **RELOAD SCENE** refreshes from disk.

This is a real reusable scene **data handoff**, not a claim that runtime SceneRenderer already parses these OBJ asset references. Explicit \`rendererStatus=requires-host-adapter\`, \`materialStatus=unassigned\`, and \`collisionStatus=query-authoring-only\` deliberately prevent placeholder material art and phantom collision. A future Beckenburg adapter must import/stream these mesh sections into GPU buffers, bind approved material assets, and supply authoritative collision/navmesh to Hunters and ghosts. Separate render/material/AI diagnostics must not gate release for aesthetic differences.

Functional tests \`RuntimeSceneDocumentTest\` and \`TerrainScenePackageTest\` run in Windows Java 17 acceptance. They cover save/reload, identity isolation, stable node updates, integrity verification, rejection of un-conformed seams, refusing to overwrite foreign assets, rollback and 16-bit heightmap/source preservation.



---

### Source: Lighting editor and runtime module

**Original repository path:** `docs/lighting/LIGHTING_MODULE.md`.

# Beckenburg Lighting Module

**Java 17 implementation:** `lighting-module/src/main/java/dev/mrcalzon/lighting/LightingModule.java`. The module contains immutable settings, built-in profiles, range validation and persistent local preferences. It does not own world state, Swing widgets, the GPU, or network authority. `FixtureLighting`, `SceneRenderer`, and the existing Castle Options screen consume it.

## Available Options

| Setting | Allowed values | Actual behavior |
|---|---|---|
| Profile | Performance / Balanced / Quality / Custom | Prepopulates the other fields; manual edits select Custom |
| Ambient fill | 50–150% | Scalar for ambient light contributions |
| Fixture brightness | 50–150% | Scales authored world-source power **and matching emissive fixture glow**; does not change handheld flashlight hardware |
| Fixture reach | 65–140% | Scales source radius and affected shadow regions |
| Cosmetic flicker | 0–200% | Controls candle and TV fluctuation only |
| Cache allowance | 50–150% | Scales supported CPU lighting, stationary-shadow, stable GPU and auxiliary resident-world budgets; **not a total JVM/GPU memory cap** (immutable House shell resource cache is not governed by this control) |
| Lighting invalidation cell | 2, 4, 8 metres | Spatial revision grouping; **not lightmap or ray-sampling resolution** |
| Distant outdoor LOD | On/off | Optional outdoor-only distance sampling, with physical LOS |
| Changed-only vertex updates | On/off | Reuses unchanged vertex colours/arrays |
| Source dependency reuse | On/off | Skips unchanged fixture-source probe batches |

**Performance, Balanced and Quality preserve identical authored ambient, fixture power, reach and cosmetic flicker.** Preset differences affect cache sizing and distant outdoor LOD, not evidence visibility. The brightness/reach sliders are explicit Custom appearance controls. Named presets load their canonical current definitions, automatically replacing obsolete per-preset values saved by earlier builds.

**Balanced** reproduces the preceding lighting defaults (100% values, four-metre cells, three optimizations on). Settings apply on **APPLY OPTIONS** and persist through Java Preferences. The active renderer notices a changed immutable snapshot and refreshes scene lighting caches **once**; individual doors and switches remain local updates, not global rebuild triggers.

**Multiplayer and game rules remain authoritative**: no client setting changes room power, switches, blackout, ghost hearing/vision, hidden evidence, collision, door state, or shadow ray correctness. No external Blender/Chocolatey/runtime asset dependency is added.

## Packaging and verification

Both `build.sh` and `build.cmd` now compile and package the standalone lighting module in `beckenburg-game.jar` and assert the class is present. `src/test/java/org/beckenburg/LightingModuleTest.java` is a separate optional diagnostic, **not a cosmetic release blocker**.

Pending acceptance: clean Java 17 Windows/Linux builds, actual Options-save/relaunch, House/Castle/solo/LAN parity, door open/close performance at each profile, stable scene IDs on ordinary door interactions, p50/p95/p99 frame time, upload bytes, cache hit rates, and no lighting through solid walls. Source commits and the packaging scripts alone do not prove those results.

## Initial implementation audit (2026-10-29)

Code review found and corrected: (1) the Performance and Quality presets had changed authored source strength and reach, conflating visibility with performance; (2) emissive bulbs/TV materials did not follow the world-source brightness slider; (3) named presets could reload saved obsolete custom values while still displaying the preset name; (4) the UI used misleading labels for world-source brightness, invalidation-cell size and cache **allowance**. Expanded optional diagnostic `LightingModuleTest` checks preset visual invariants, physical beam/math boundaries, and unchanged host lighting circuits.

**Known unverified / intentionally not claimed:** the immutable authored House-shell cache is not under the new percentage cache allowance; it needs a separate correctly bounded resource-ownership design rather than unsafe VBO eviction. Windows and installed GPU tests have not yet been run; the module's independent source/Java syntax and pipeline integration are subject to a clean actual JDK 17 full-game compile. No new release gate has been added.


## PSX lighting engine PDF cross-check (six-page supplied reference)

The supplied document recommends: 5-bit RGB quantization, per-vertex/Gouraud
illumination, optional clamping and overflow, per-mesh subdivision, ambient
and additive colour combination, alpha/STP hygiene, affine UV and screen
snapping, 4x4 ordered dithering, optional indexed CLUTs, avoiding T-junction
cracks, and avoiding excessive tiny batches.

**Already implemented elsewhere (do not duplicate in this module):**
- `PsxPipeline.finishFrame` quantizes and Bayer-dithers the finished world
  framebuffer; `PsxPipeline.snap` and `uv` implement the existing PSX
  presets. These are presentation effects and must not warp the Swing UI.
- `SceneRenderer` prepares vertex lighting, keeps resident source meshes
  with revisioned VBO IDs, uses limited near-camera subdivision and retains
  OpenGL colour interpolation. The handheld camera flashlight is deliberately
  a GPU fragment effect; converting it to a CPU full-world relight would
  recreate the door/camera hitch. Don't introduce another global light pass.
- `FixtureLighting` maintains hard occlusion checks against actual doors,
  geometry and source positions, as well as colour and shadow caches.

**Newly connected:** independent source reach and diffuse-edge width.
`LightingModule.Settings.diffusionPercent` (5–100%) sets how much of each
source radius is used for the fade band. At 100% it is identical to the
original `1-distance/radius` falloff; a narrower band retains a stronger
inner core. `LightModel.diffusionFade` is constant-time math, used by
both prepared-probe and live CPU samples. It adds no blur shader, shadow
map, full-screen pass, GPU texture, or new ray. This is light-edge softness,
**not a physically simulated penumbra or volumetric scattering**.

**Light cell layer priority:** the PDF does not prescribe light layers or
a priority mechanism. The implementation spatially indexes *contributing
sources*, then adds ambient and actual physically occluded source
contributions, with self-emissive surfaces applied as a max rather than an
unoccluded illumination source. Host power/outage/interference decide
fixture activation. There is no explicit ordered light-layer override
scheduler yet, and it must not be invented merely to choose a winning
lamp inside a spatial cell. If future gameplay effects require overrides,
they should be resolved before local light sampling and must never bypass
wall LOS or authoritative network rules.

**Post-processing:** `SceneRenderer` currently performs the ordered RGB
PSX pass and then `GraphicsEffects.apply`; these are two distinct
full-frame traversals when effects are enabled. They are measured
independently. Do not add a second shader dither, an automatic fragment
lighting mode, or a CLUT conversion on top of the current look without a
demonstrated need. `GraphicsEffects` already bypasses itself when all
additional effects are off.

**Performance diagnostics** (`LightingFrameProfiler`):
- A 120-frame bounded primitive-array rolling window records CPU frame,
  scene build, geometry/raster, PSX quantization/dither, and optional
  graphics-effects duration; it also records triangle high-water marks,
  shadow rays, changed lighting cells and delayed GPU readback counts.
- `SceneRenderer.lightingPerformance()` returns p50/p95/p99 frame times,
  p95 stage times and peaks on demand. Recording is O(1) with no sorting,
  allocations or I/O on the per-frame path. Percentile sorting is only
  performed when a snapshot is explicitly requested.
- Pass `-Dbeckenburg.lighting.profile=true` to opt in to one local
  `LIGHTING PERF` log approximately every five seconds. No telemetry,
  no online upload, and no added blocking GitHub test.
- These are **CPU timings**. Async GPU duration and VRAM residency require
  separate backend/driver instrumentation: GPU readback latency is a
  symptom metric, not a substitute for GPU timer queries.

**Follow-up acceptance**: compare all three presets at 200p, 360p and
540p for the same camera/door scene: p50/p95/p99 *frame* times,
PSX/post-processing ratio, after-door frame drop, retained vs rebuilt VBO
counts, GC pauses, CPU and GPU frame workloads, live shadow correctness,
T-junction gaps, and emissive/normal parity. Investigate any full-screen
CPU pass or shadow-probe burst that exceeds the chosen frame budget. As
always, non-functional appearance/performance diagnostics are non-blocking
unless the actual game does not function.

**Not included by default:** per-fragment Phong/PBR lighting, expensive
soft-shadow blur, CLUT conversion, variable per-pixel ray casting,
additional alpha simulations, or a large collection of microscopic
per-light draw calls. These are optional/editor concerns, not necessary
for an efficient PSX-style horror game.



---

### Source: Rendering and environment rigging source register

**Original repository path:** `docs/PSX_RENDERING_AND_ENVIRONMENT_RIGGING.md`.

# Beckenburg — PSX Rendering, Environment Rigging & Shader Source Register

**2026-10-18 | Status: asset/design catalogue, not a runtime feature claim.** The authoritative machine-readable source list is [`software/beckenburg/assets/psx-shaders/index.json`](../../software/beckenburg/assets/psx-shaders/index.json). The owner's [Drive master asset index](https://docs.google.com/document/d/18G144Qd0-L5rL7NdqxMykqkXaBHtu8-ytr_yYPol6rI/edit) is the asset-source counterpart. Read alongside [Hardware, Accessibility, Graphics & Menu Options](HARDWARE_ACCESSIBILITY_GRAPHICS_AND_OPTIONS_STANDARD.md) and [Quality Gates](QUALITY_GATES.md).

## Renderer reality and boundaries

Beckenburg currently runs a **Java 17-targeted Swing/software raster renderer** for the prototype. A Godot `.gdshader`, Unity `.shader`, or libGDX GLSL `.glsl` is **not executable or importable by this rasterizer**. Do not put a random shader in the game assets directory and claim an effect has been implemented. The first runtime route is a **Beckenburg-owned CPU raster/postprocess implementation** with tests; a future GPU backend must be a deliberate engine decision with feature detection, Java/Windows packaging and fallback. The catalogue contains links/metadata only, **no copied external shader code**.

Our €11 owner-purchased **PSX Mega Pack v3.2.1** models and textures are a *separate* source/licence from the external shaders below. Its pack-wide commercial-use grant does not extend to unrelated shader repositories, and shader licences do not grant rights to unrelated model packs. Keep original paid archives on the owner's Drive; select and provenance-track only models genuinely used by the game.

## Source index / reuse policy

The full register contains nine candidates, their upstream links, engine, licence and **not-imported** status. Prioritize:

| ID | Library | License (source checked unless stated) | Backend | Beckenburg decision |
|---|---|---|---|---|
| PSX-S001 | [snotbane/psx_visuals](https://github.com/snotbane/psx_visuals) | Unlicense | Godot 4 | Preferred public-domain behaviour reference: vertex jitter, UV affine, fog, 5-bit color, dither |
| PSX-S002 | [Rytelier/Godot-PSX-Visuals](https://github.com/Rytelier/Godot-PSX-Visuals) | MIT | Godot 4.4+ | Rich environment/material reference; copyright/permission notice if adapted verbatim |
| PSX-S003 | [MenacingMecha/godot-psx-style-demo](https://github.com/MenacingMecha/godot-psx-style-demo) | MIT | Godot 4 | Lean material system, billboards and fog reference |
| PSX-S004 | [AnalogFeelings/godot-psx](https://github.com/AnalogFeelings/godot-psx) | MIT | Godot 4 | Archived; contrast material versus screen-space stages |
| PSX-S005 | [TotesNotJosh/Godot-PSX](https://github.com/TotesNotJosh/Godot-PSX) | CC0 | Godot 4.4+ | 15-bit/dither postprocess reference; object shader not yet shipped as described |
| PSX-S006 | [TotesNotJosh/PS1-3D-Shader](https://github.com/TotesNotJosh/PS1-3D-Shader) | CC BY 4.0 | Unity | Attribution required if copying; reference only now |
| PSX-S007 | [davidbrowne17/PsxShader](https://github.com/davidbrowne17/PsxShader) | CC BY-SA 3.0 per repository LICENSE.txt | libGDX / Kotlin / GLSL | JVM-adjacent example; **do not vendor** without share-alike review |
| PSX-S008 | [Francisco Cidade PSX - Godot Shader](https://store.godotengine.org/asset/fcidade/psx-godot-shader/) | CC0 per store | Godot 4.6 | Current all-in-one example; source archive still uninspected, store marks release unstable |
| PSX-S009 | [scolastico/psx_visuals_gd4](https://github.com/scolastico/psx_visuals_gd4) | MIT per Godot listing, upstream audit pending | Godot 4 | Runtime material-conversion design reference only |

Each index entry is **available for investigation**, not embedded in the executable. Licensing obligations belong with **each external source**, not the purchased PSX model pack. If code is eventually vendored: pin upstream commit/version, retain required license/attribution in the package, store source path, SHA-256 and transformation notes, and separately test the actual backend.

## Beckenburg-owned rendering feature matrix (all PROPOSED unless a test proves otherwise)

| Feature ID | CPU implementation target | Visual tuning & limits | Important cross-system requirements |
|---|---|---|---|
| PX-01 Pixel canvas | Render world to fixed low-res buffer, nearest-neighbour integer-ish scale/letterbox | Baseline 320×240 (4:3), 426×240 (~16:9), 640×360 comfort; never distort camera aspect | Keep menus, small evidence text and HUD native-resolution/readable; pointer mapping must account for scale and bars |
| PX-02 Vertex snap | Quantize *projected* screen-space vertex positions before triangle raster | 0 / subtle / authentic; stable with viewport size and camera motion | Visual-only: **never snap server-world coordinates, triggers, collision or ghost detection volumes** |
| PX-03 Affine UV | Interpolate u/v in screen space instead of perspective-correct u/z,v/z; blend with perspective mapping | Off / subtle / authentic, avoid maximal warp on long floors | Keep meshes sub-divided enough for doors, walls, floors and evidence decals; prevent UV crawl masking symbols |
| PX-04 Texture sampling | Nearest sampling, selectable conventional filtering fallback; mip/bias controlled | 64/128/256 px typical texture targets, power-of-two as convenient, atlas with gutters | UV seam inspection, consistent texel density, readability for labels, fingerprints, ghost writing and tool displays |
| PX-05 Color depth | RGB 5 bits per channel (15-bit-like) and optional palette quantization in postprocess | Off / reduced / 5:5:5; alpha handled separately | Preserve functional contrast, color-blind alternatives, grayscale checks; do not quantize procedural equipment data itself |
| PX-06 Dither | 4×4 ordered Bayer pattern applied before RGB quantization; anchored to final pixel grid | off/25/50/100% with static matrix, no random temporal noise | No stochastic scene flashing, ghost traces/motion text must stay legible; screenshot-deterministic |
| PX-07 Distance fog | Distance blend or per-vertex haze with tunable near/far per map/weather | Fog may hide geometry but cannot defeat mandatory visual evidence range | Flashlights, D.O.T.S., UV/thermal presentation use tested contrast priority; the host alone decides what evidence exists |
| PX-08 Lighting | Per-vertex/flat lighting, low fixed ambient, emulated vertex fog, local emissive meshes | Limit light radius, intensity, color; preserve physically meaningful darkness | Couple lamp meshes, toggles and flickers to existing RoomLighting; flashlight cone and van screens remain functional |
| PX-09 Transparency | Alpha cutout leaves/fences; explicit sorting for glass, decals, particles and ghosts | Prefer hard cutout over expensive blended foliage; scene-order deterministic | Do not let postprocessing occlude clues, physical silhouettes or player/ghost tells |
| PX-10 Retro optics | Subtle chromatic offset, vignette, film grain/CRT pass only where appropriate | Default restrained/off for user-sensitive effects; CRT on diegetic monitor material, not entire menu | Photos/camera evidence use defined capture source and host events; no accessibility-hostile motion/flash defaults |

A genuine preset is a synchronized set of supported, tested subfeatures. **Do not expose dead graphics controls**. Design presets can be documented now without pretending the CPU renderer already supports them. Effect state is client-local and must not alter authoritative ghost class, manifestations, range, equipment numerical readings or networking.

### Suggested visual profiles — draft, not wired into settings

- **Cinematic horror (default target):** 426×240 widescreen world; subtle projected snap; partial affine warping; nearest sampled 128 px textures; gentle static dither; 5:5:5 tone; aggressive-but-legible distance fog; native-resolution HUD.
- **Authentic PSX:** 320×240 4:3 or letterboxed, full pixel snap/affine, 5:5:5 with 4×4 ordered dither; strong fog; limited vertex-lit surfaces; expect old-school instability without game-logic instability.
- **Readable/comfort:** 640×360 or higher internal canvas; vertex snap/affine/grain disabled or reduced; fog and darkness lifted only within fair-play limits; UI and tool text crisp; documented high-contrast and reduced-flicker overrides.
- **Low-spec CPU:** internal resolution/per-object density/particle count and drawing complexity reduced after benchmarking; no claim of a particular FPS without Windows hardware evidence.

## Environmental rigging contract (applicable to castle hub, van and all planned investigation maps)

**World/layout:** use one authored placement unit/grid scale with exact world-to-model transforms, pivots, handedness, collision plane, measured door clearances and stairs. Record `.glb` source name+hash and level placement; no duplicate unlicensed stand-in. Divide scenes into room/portal cells so doors, hallways, adjoining rooms and van apertures participate in visibility and audio occlusion. Keep navigation, movement/hiding probes and evidence-spawn volumes separate from the visual mesh and PSX snap.

**Modular architecture:** prefabs for straight/corner/end wall, door frame, hinge-door, shutter, trim, stair, balcony, floor/ceiling, railing, windows, basement entrances and outdoor fence/gate. Seal backsides; use repeatable UVs and atlas gutters so affine sampling doesn't expose cracks. Define door hinge/pivot, lock state, swing arc, max opening, collision footprint and authoritative network state. A flickering light should swap or change an existing luminaire material state, not spawn an unrelated lamp model.

**Lighting rig:** classify ambient sky, powered room lights, portable tools, candles/lamps, emergency lights, paranormal flash/flicker, ghost manifestations and diegetic screens. Every placed switch has a room/power group; each light has location, radius, falloff, color, flicker ceiling, shadow/occlusion policy and on/off visible-source mesh. Bake vertex colors/ambient hints where possible but retain actual dynamic signals from RoomLighting. Candle/fire or security monitors don't turn into functional EMF or ghost indications without host event input.

**Outdoor rig:** low-poly ground and paths with atlas/UV seams; Retro Tree source-preserving trunks/leaf cutouts; distance LOD/billboards and fog horizon; night sky, weather and ambient audio zoned per biome/courtyard. Avoid foliage sorting spikes and render-thread overdraw on older Windows CPUs. Vegetation density presets may change decoration, **not hiding-spot, path or line-of-sight gameplay** unless explicitly host-authored/replicated.

**Character/entity rig:** consistent coordinate scale, orientation, hip/root, skeleton hierarchy, hand sockets and head/camera offsets. Declare flashlight/EMF/book/grab interaction anchors; male/female lowpoly source bases remain actual source meshes, not interchangeable generated previews. Ensure tool swaps align with real source geometry. Ghost entity readability prioritizes user-approved concept silhouettes, gender variants, flicker state and visible event timing, not internet placeholders. Any billboard sprites must face appropriately without corrupting hit/collision or multiplayer state.

**Surface classes:** opaque textured architecture, double-sided cutout vegetation, unlit signage/ritual marks, emissive bulbs, glass/mirrors, wet surfaces, decal evidence and CRT monitors. Avoid per-frame temporary materials in hot render loops; cache geometry/texture and material variants. Keep runtime authorship and licensing IDs in a single manifest. Reserve high contrast for functional UV, ghost book markings, D.O.T.S. traces, salt footprints and emergency navigation.

**Gameplay clarity:** no shader profile may fabricate/suppress ghost evidence, change EMF (normal 2–7), mislead the thermometer, omit actual ritual sites, ghost/door interactions or make Tier I–III tool displays unreadable. Graphics settings alter *how* information is shown, never *which host-authorized information exists*. Asymmetric ghost view may have different aesthetic treatment but the same host simulation and privacy rules.

## Integration windows and acceptance gates

1. **Inventory:** reconcile the nine source references and licence texts; no external binary/source additions yet. File status stays `INDEXED_REFERENCE_NOT_IMPORTED`; no claims of installed shaders.
2. **Baseline render:** capture deterministic image of one existing purchased-pack GLB, a lit/unlit ceiling lamp, door edge and tools from the real Java renderer. Measure draw calls, frame time and color/contrast with pixel canvas disabled/enabled.
3. **First real slice:** implement independently authored CPU pixel buffer downscale/nearest upscale and 5:5:5 4×4 Bayer dither with tests and readable native UI. Only promote these IDs after a passing Java render test and screenshots from the actual game.
4. **Geometry slice:** CPU vertex screen snap and affine UV mode alongside correct perspective alternative. Test triangles across clipping, nearby camera, extreme FOV, door rotation, long floors, transparency and resize/fullscreen.
5. **Environment slice:** integrate room-linked fog, light group material state, castle/van placements and outdoor billboard/cutout sorting; validate against host-owned switch states and real maps.
6. **Quality:** stress six hunters and human ghost view; evidence tool Tier I–III displays, D.O.T.S., writing book and ritual visuals; test accessible reduced-flicker/comfort profile, colorblind and 4:3/16:9 scaling. Profile older Windows CPUs and verify no network simulation divergence.
7. **Promotion:** register output snapshots/logs and fresh Java/Windows build evidence against `QUALITY_GATES.md`. Publish only the specific source/asset licences actually included. If a GPU renderer later replaces the CPU path, reconfirm every effect in real build; nothing auto-transitions to `INTEGRATED`.

**Source/file state as of this catalogue:** PSX shader references recorded, architecture/rigging standards written; **none of the nine upstream shaders has been bundled, executed or shown in a Beckenburg playable build by this change**.



---

### Source: Permanent DEV-room model and asset visual-review workflow

**Original repository path:** `docs/development/DEV_ASSET_ROOM_PERMANENT_REVIEW_WORKFLOW.md`.

# Beckenburg — Permanent DEV Asset Room Review

This is a standalone, separate development room, not a normal haunted map, not a player seat and not a multiplayer observer. Actual game meshes retain their approved original embedded textures, UV mapping and triangle geometry. Character exhibits use actual packaged directional sprite frames. There are no invented surrogate models.

## Launch and inspect

From installed Windows game folder:

    Beckenburg.exe --dev-room

From checkout after running build.cmd e2e:

    dev-room.cmd

The showroom is an unlimited series of six-exhibit 3D bays. The left-side searchable catalog contains all approved environment models, every equipment model and variant, all registered local imported models, all unapproved source-only GLBs present in a source checkout (marked SOURCE_ONLY and never enabled in gameplay), the ghost identities (including ancients and elders), playable hunters and all castle-support NPCs.

Click a model in the catalog to teleport the developer camera to it; PgUp/PgDn change bays; 1–6 teleport to a stand; WASD and Q/E fly without collision; Shift moves faster; drag mouse to look; R rotates exhibits and F12 captures the exact active renderer PNG. The saved image goes to the Windows user profile, .beckenburg/dev-room/captures.

For four focused image angles of an actual model on headless machines, use:

    java -Djava.awt.headless=true -cp "build\beckenburg-game.jar" org.beckenburg.DevAssetShowroom --dev-room --capture --asset=MODEL_ID --output=build/dev-room

Replace MODEL_ID with a real listed asset identifier.

## Mandatory workflow for any model/character addition or change

1. Register the model in the runtime's approved library and keep its original files intact. When imported through the level editor, the model automatically enters ImportedAssetLibrary, which the showroom enumerates.
2. Verify the asset appears in the DEV ROOM without adding a second hand-maintained filename list. If it does not, fix the *owning* runtime catalog rather than hiding an issue in the showroom.
3. Teleport to its real 3D stand. Check front/side/back texture mapping, alpha, feet, scale, material selection, geometry holes and whether it still looks like the approved source. Capture visual evidence.
4. If the asset is bad, repair the actual importer, original-to-runtime binding, mesh transform or source model. The showroom MUST NOT cosmetically hide the defect by replacing the model, repainting textures, flattening UVs or swapping in generic cubes.
5. Check the next GitHub Actions "Developer Asset Room - Continuous Visual Review" results. It must inventory the asset, and its real-render screenshot must be available for review. Re-review changed assets.

## Automated audit meanings

- LOADED: real model/sprite decoded and texture samples were readable; NOT a visual approval.
- REVIEW_UV: UV or material samples fail; inspect screenshots/source.
- REVIEW_TEXTURE: the sampled texture appears unusually uniform; possible issue, not proof.
- MISSING: runtime sprite/GLB cannot load.
- REVIEW_REGISTRATION: a source GLB is not in the approved runtime registry. It is still displayed as a SOURCE_ONLY exhibit in the development checkout, without granting any gameplay approval.

The workflow enumerates the actual GlbProps.availableModels, EquipmentModelCatalog, ImportedAssetLibrary, GhostIdentity.Type and CharacterVisuals registries automatically. It also directly previews source-only GLBs in the developer checkout using the same original GLB texture sampler; installed builds show only packaged/approved assets and their separately registered local imports. It runs the packaged game's real --dev-room entrypoint, captures the actual 3D renderer in six-stand PNG contact sheets AND one close-range PNG for every individual exhibit, reports every asset and uploads them to GitHub Actions artifacts. The showroom uses a neutral enclosed studio and does not repaint the original textures.

**Cosmetic and visual asset reviews are diagnostic only and must never block builds or releases.** Source compilation, launching, required game functionality, and the existing multiplayer acceptance remain functional gates. An automated decoder pass does not certify visual fidelity; a human reviews the captured geometry and source art.

This room is not meant to alter campaign saves or add a hidden dev player to standard matchmaking. It exists solely as a permanent, repeatable visual QA scene.


## Opt-in terrain topology QA in the same DEV room

The DEV asset showroom now has three separate developer actions: **Topology QA on/off**,
**Edit QA topology**, and **Reload QA topology**. These do not change the normal model
catalog, plinth positions, imported textures, camera controls, or ordinary source asset
screenshots. The patch appears only when enabled, in the otherwise unused right side
aisle. Enabling it first creates (or subsequently reopens) an isolated 16-bit
stitched-heightmap test world under
\`%USERPROFILE%\\.beckenburg\\dev-room\\topology-qa\\\`, with two adjacent
PNG tiles and an authored refinement example spanning their shared base-cell boundary.
The test fixture is never added to campaign maps or the model allowlist.

The colored 3D patch is made from the real 16-bit source heights and saved
\`AdaptiveTerrainTopology\` leaves in \`SceneRenderer.drawDevAssetRoom\`.
Different authored subdivision depths use different diagnostic tints.
It is deliberately **not** a physical ground/collision implementation:
the current vertex heights still come from canonical base heightmap triangle
interpolation, and subdivision metadata alone cannot create cliffs or local
independent sculpting detail.

Use **Edit QA topology** to open the human adaptive editor, select cells and
subquadrants, split/merge, then save. Closing that editor reloads its saved
source data into the DEV renderer; the explicit Reload button allows manual
readback. Use the free camera to move around the patch and F12 for an actual
rendered screenshot (not concept art). Reopening the DEV room leaves existing
saved QA terrain data intact. Errors in saved topology do not silently replace
it with invented default geometry.

To capture the exact dev renderer's topology patch without a GUI:

    java -Djava.awt.headless=true -cp "build\\beckenburg-game.jar" org.beckenburg.DevAssetShowroom --dev-room --terrain-qa-capture --output=build/dev-room

This writes \`terrain-topology-devroom-real-render.png\` and
\`terrain-topology-devroom-report.txt\`. The existing DEV room visual-review
CI also attempts this capture as a **diagnostic only**.

Acceptance work still outstanding: height offsets on sparse shared dyadic
vertices; cell-edge/PNG-border sculpt transactions and rollback; visually
watertight deforming mesh, gameplay collision/LOS from the identical geometry;
zoom-sensitive LOD and true distant actor/occlusion tests. A real renderer
screenshot of the current *undeformed* patch is not proof of any of those.



---

### Source: Permanent infrastructure exhibit; terrain and rail physical adapter status

**Original repository path:** `development/levels/DEV_INFRASTRUCTURE_EXHIBIT.md`.

# Existing DEV Room — Infrastructure Material and Water Demonstration

This is a toggleable, substantial construction exhibit inside the **existing**
`DevAssetShowroom` side aisle, not a new gameplay map, second renderer or
unreviewed replacement art. Default six-model bays remain unchanged.

## Open and review

Run `dev-room.cmd`, open the normal DEV model/character room, and click
**Infrastructure demo on/off**. Use WASD/QE free-flight; inspect all three
sections, then **Save actual renderer PNG** for a real source/model review.

The exhibit occupies the showroom's previously unused side aisle:

- **Rail sample:** genuine `TerrainRailMesh` ballast, individual sleepers and
  two extruded steel rails using its existing profile and mesh triangles.
- **Road sample:** physically graded lateral shoulders, crowned asphalt,
  pavement markings, surrounding ground and a run of round wooden utility
  poles with actual crossarms and suspended two-wire spans.
- **River sample:** real channel bed with sloped bank geometry, exposed
  sediment, rock GLBs, grass edges, and two true `WaterPlacement` bodies
  joined through `WaterSnap`. Water body footprints are explicitly rendered
  as *diagnostic geometry*, not passed off as live water shader screenshots.
- **Vegetation and rocks:** the real packaged GLB assets such as
  `level_tree_conifer`, `level_tree_broadleaf`,
  `level_camp_pine_refined_a`, `level_woodland_boulder` and
  `level_camp_siteart_rocks`; the same `SceneRenderer.imported` path used
  by the regular DEV room. If a model is not approved for gameplay, its real
  source-only model is loaded through the review-only importer, not granted
  approval silently or replaced with a generic colored block.

## Real water integration and snap points

`TerrainRiverWaterSockets.fromRoute` turns **existing RIVER spline controls**
into positional/oriented `WaterTerrainSnap.Socket` objects. Each socket
inherits its location, elevation, width and local downstream tangent from the
actual authored route. `WaterTerrainSnap.connect` invokes the installed
`WaterSnap.find` edge/height/rotation matcher, never approximate screen-space
snapping or duplicate fluid physics.

Click **Edit river snap/water...** to open the existing portable `WaterCanvas`
and `WaterEditorPanel`. All authored river socket controls display as orange
crosshairs. Drag a selected water body to a socket or use **Snap selected**;
the usual water-editor undo/redo applies. **Save** writes a real
`dev-infrastructure-demo.water` document through `WorldWater.save`,
readbacks through `WorldWater.load`, and updates the DEV room water footprints.
**Export demo water...** supports explicit standalone document export.

The component is portable: no hard-coded Beckenburg game types were inserted
into the water module, and the river-to-water conversion lives in the host
adapter. The source R16 tiles and normal gameplay level files are unaffected.

## Portable authored-rail scene export

The real terrain module now exposes **3D RAIL PREVIEW** and **PUBLISH SAVED
RAIL TO SCENE** from `TerrainScenePackageEditor`. To author a durable rail:

1. In the existing terrain infrastructure spline editor, save a **RAIL**
   route in the world's `terrain-infrastructure-splines.bin` document.
2. Open the terrain-to-scene package panel, enter its exact saved route ID,
   and choose **3D RAIL PREVIEW** to inspect the actual ballast, twin rails,
   and sleeper faces with the existing orbiting mesh inspector.
3. Choose **PUBLISH SAVED RAIL TO SCENE**. This uses the same
   `scene-package/scene.bin` manifest as terrain chunks, adds a stable
   `terrain-rail-<routeId>` scene node, and publishes its genuine OBJ under
   `scene-package/meshes/rail-<routeId>.obj`.
4. **VERIFY PACKAGE** checks actual OBJ groups, triangle inventory, file
   checksums, and saved route source fingerprints for both terrain and rails.
   A changed saved spline flags the published mesh as STALE; rebuild the
   route's owned node. Foreign OBJ files are never overwritten.

`TerrainRailScenePackage` is part of the independently compiled Java 17
editor library; it calls the existing `TerrainRailMesh`, `RuntimeScene`,
`EditorMeshPipeline` and `TerrainScenePackage`, not a Beckenburg-only
renderer or a parallel scene system. The railway scene material, game
collision/navmesh and runtime renderer bindings are explicitly **not yet
integrated**; scene export and real geometry inspection are distinct from
live game-world acceptance. The source 16-bit terrain tiles are unchanged.

`TerrainRailScenePackageTest` verifies genuine mesh generation, shared
terrain-and-rail scene persistence, stale-source rejection, readback,
re-publish identity, rollback and foreign asset protection in the standalone
editor CI. This functional regression is separate from the DEV room's
non-blocking subjective visual inspection.

## Game-side physical scene intake (opt-in, bounded)

`TerrainSceneNativePhysics.load(world,scene)` now verifies the published
terrain and railway OBJ assets through the SAME scene package inspector and
converts their actual triangles into the existing Beckenburg `AuthoredSitePhysics`
HCS2 engine. It does not invent collision AABBs or a second nav/collision
implementation. Surface normals determine the support classification:

- Upward terrain and ballast faces at or below the existing HCS2 maximum
  walking grade are support triangles.
- Sleeper and steel rail geometry is physically solid and can occlude sight,
  but is **not** falsely classified as a full-width walkable platform.
- Hash mismatches, stale source fingerprints, unsupported extents and absent
  walking support reject the native import rather than silently improvising
  geometry. Arbitrary terrain worlds beyond the native HCS2 +/-256m envelope
  are explicitly rejected; world-streamed physics is a separate task.

The native HCS2 parser and its existing support/raycast routines remain the
only gameplay physics calculations used by this adapter. The adapter is
available for host integration and verified by
`TerrainSceneNativePhysicsTest`; it is **not yet automatically bound to
HouseMap, multiplayer world snapshots, AI navmesh or live game rendering**.
No gameplay map acceptance is implied by a successful isolated import.

Custom `USER` maps now open the working `StitchedWorldEditor` from the
actual developer editor's **TERRAIN / ZONES** command. Their worlds are saved
under `.beckenburg/editor/user-levels/<level>/terrain-worlds/<world>`, not in
the shared stitched-world source directory. The portable world editor accepts
a host-scoped root and prevents opening terrain from outside that scope.
Existing built-in level terrain tools retain their previous paths.

## Automated verification and evidence

`.github/workflows/dev-room-asset-review.yml`:
- **Release-relevant functional test:** genuine rail/road/poles/river geometry
  exists; original tree/rock GLB files exist; water sockets derive from the
  authored spline; snapping rejects distant or wrong-height requests; water
  undo/redo and native module export/readback/build succeed.
- **Advisory visual diagnostics:** launches the same real
  `SceneRenderer.drawDevAssetRoom` at three observation points to write
  `dev-infrastructure-actual-00.png` through `02.png` and a report,
  alongside other DEV room screenshots. These may report broken/missing art
  but cannot block a build solely for visual quality.

`DevAssetShowroom --dev-room --infrastructure-demo-capture --output=build/dev-room`
does not fabricate or AI-generate any in-game screenshot.

### Explicit limitations

This exhibit proves sample mesh construction, actual model import, real water
module data and water socket math. It does **not** prove the full Beckenburg
gameplay map's terrain collision/navmesh adapters or the full animated water
shader in the DEV scene. The DEV river shows actual `WaterDocument`
footprints using untextured diagnostic surfaces; later host water-renderer
integration should use that same native document, not a second water system.



---

### Source: Active water editor integration handoff

**Original repository path:** `development/levels/ACTIVE_WATER_EDITOR_HANDOFF.md`.

# Active water editor integration — 25 October 2026

Latest user scope: independent water module editor placement icon; live wave/transparency/texture-sheet/flow-direction/speed controls; multi-selection; reusable per-map save/export; real SQUARE/CIRCLE/OBLONG/OCTAGON/PILL footprints; draggable editor handles and edge/rim snapping. Also audit weather integration. Keep approved texture scroll rates. Continue active all-map goal; do not mark it complete.

Published implementation:6574a8c (native bridge, stronger160mm waves +60mm shore run-up, real visible-depth solid-contact foam, ballistic spray,38-section actual dry hull, new silt/stone bed,48–97% opacity, WebM+MP4 player). Earlier6ef84b5 material/player revision browser-tested all4 primary WebMs playing; actual screenshot proof archived. Latest gallery has768PNG entries. Network DNS recovered after3 failures. Source/refinement/usage/handoff and defect ledger throughDEV-114 already published. All actual capture batches exit0 after graceful GPU worker shutdown. Full multiplayer CI previously blocked by stale equipment counts (fixed) then stale all-cupboard assumptions (fixed in6574); newer full CI needs inspection. Native mission remains locked; no full LAN/human/performance acceptance inferred.

Current uncommitted portable editor core in water-module/src/main/java/dev/mrcalzon/water/: WaterSettings,WaterPlacement,WaterDocument,WaterTextures,WaterEditorModel,WaterEditorPanel,WaterIcon,WaterSnap. All compile independently with Java17. Existing WaterEffects now has backwards-compatible contact/spray switches; Body has backwards-compatible optional seamKey. Compatible overlapping tiles deduplicate actual ray hits; distinct-height transparent bodies still layer. Shapes are actual polygon masks; circles keep a real diameter, octagons respect stated bounds, pills support either axis. Snap uses real facing footprint segments with small optional angle alignment; curved-rim fallback uses closest boundary points and no rotation. Gesture history supports one undo for a drag+snap and exact cancellation. Tests WaterEditorModelTest and WaterShapesSnapTest both pass against build/water-editor-check.

Current host wiring compiled successfully via offline Maven: WorldWater adapter loads per-map local overrides from property beckenburg.water.documents or ~/.beckenburg/editor/water, then packaged /org/beckenburg/water/<id>.water, then defaults. Current Hollow Creek default preserves exact rectangle/basin/hull and approved texture motion. Other maps default empty. Native bed/hull resolved by profile, generic flat-depth fallback supported. Optional assets:// PNG registry/import is bounded. LevelDefinition now owns an immutable WaterDocument and includes its canonical revision in asset/host handshake; old constructor preserved. CastleHubMap also owns an immutable water document. SceneRenderer now uses generic sceneWater for missions/castle with a live preview override and frame-matched module snapshot; newly added pinCompletedGpuFrame should be checked actually consumes pinned result (replacement may miss typed available declaration). Native player-water guard follows the map document. No generic closed-room water physics has been claimed.

Still required: implement actual EnvironmentModuleEditor host window/viewport + map chooser, connect portable panel to WorldWater, add real draggable size/move/rotate/edge handles and snap preview, save/export/import texture UI, add entry/icon in existing DeveloperLevelEditor assets/toolbar, and an authorized standalone GPU editor launch flag for actual UI verification. Plan is read-only existing level geometry + editable water, background renderer worker with immutable camera/module snapshot, copied completed image, generation-safe map switching. Preserve original castle editor geometry controls. Need test actual UI placement/parameter changes/shapes/multi-selection/snapping/save+reload, capture and publish honest screenshots. Do not stop at the portable model only.

Weather audit: RecordedWeatherTest, MissionWeatherTest, WeatherAssetsIntegrationTest all passed on actual packaged resources. Core rain/snow/fog/wind/lightning/thunder/roof coverage/scheduling/audio are implemented. Source inspection found no editor weather inspector; GPU path appendGpuFace does not apply weatherCoating (software path does); castle previously drew sky but omitted particle mesh pass, which current uncommitted sceneWater wiring added. Generic weather roof exposure still needs validation, particularly castle/new native roofs. Add a preview inspector or report exact remaining scope; do not claim GPU wet/snow coatings complete. Weather should remain host-authoritative in play; preview controls are separate.

Environment: explicit unified workdir, JDK17 C:/Program Files/Java/jdk-17/bin, offline Maven C:/Users/Admin/scoop/apps/maven/current/bin/mvn.cmd (require_escalated normally succeeds). Runtime capture classpath gpu-client/target/BeckenburgGPU.jar;build/camp-detail-check/classes;build/unification/classes, jar first. Prefix every PowerShell -D argument with quotes (unquoted -Djava.awt.headless=true becomes bad mainclass). Private cache contains all946models and resources; source runtime assets committed. Root build scripts currently compile water/*.java, so portable Swing classes stay same package. Need package saved .water profiles/imported textures in build scripts/GPU pom. Read/write UTF-8 explicitly; preserve upstream shader bytes (.gitattributes -text). GUI tests must override water documents/assets roots into workspace build/, not mutate user .beckenburg. No subagents authorized.

Preserve unrelated files: evidence-book-pages.json,hollow-creek-fitted-fit.json,world-lore.json modified; blackpine-story.json,blackpine-terrain.json,foliage-variation.json,scenery-instancing.json untracked. Exclude them from commits. Sparse checkout: new water-module files require git add --sparse. Fetch/ancestor-check before authorized git push origin HEAD:main; never force. Continue pending gallery verifier session59011 only if still running; get final output and preserve delivery proof. Browser tool blocks local file pages; use published HTTPS page, not a local-server workaround. CUA tab claims can expire on user steering; reacquire/create same IAB as needed. Last successful WebM browser tab4 was paused after proof, may be stale.


CHECKPOINT UPDATE: EnvironmentModuleEditor, actual WaterGamePreview and portable WaterCanvas now implemented. Offline GPU and standalone SDK Maven packages pass. Actual OpenGL UI copy, pill shape,250→201m resize,30degree rotation, save, four undos and restore verified. Isolated save path build/water-editor-live; screenshot live-handles.png labelled Editor UI verification. All water/editor/shape tests,969HollowCreekroutes/939sweeps and three weather suites pass. Actual HOUSE/FARMHOUSE/SCHOOL host/client test passes after sandbox socket denial resolved via escalation. New sound-volume request is next; GPU ground coating and per-roof audits remain open. Full Hollow Creek mission/LAN/performance acceptance remains open; all-map goal stays active.

2026-10-26 pending sound checkpoint: SoundVolume, SoundDocument, SoundVolumeAudio, SoundVolumeEditor and X-ray SoundVolumeGizmos compile; SoundVolumeTest passes attenuation, stereo, power, one-shots, mute, atomic save and immutable handshake. Actual OpenGL window displayed seeded Hollow Creek bounds; user stopped Computer Use before completing Sound volumes tab review. UI review, publication evidence and listening acceptance remain open. Latest user priority is all-map new lighting/camera/texture compatibility audit; preserve these sound changes while reconciling main.



---

### Source: Agentic observation, manipulation and verification implementation contract

**Original repository path:** `docs/development/AGENTIC_EDITOR_ACCESS.md`.

# Agentic access to the Beckenburg editor — implementation contract

**Owner direction:** 2026-10-31. **Repository:** `mrcalzon02/The-Beckenburg-Project`, `main`.  
**Inspected source baseline:** `3f42f2ce2110c084e34525f975a3bc8ce5a0e98f`.  
**Document status:** specification and execution plan; NOT a claim that machine control, headless operation, or an MCP connection is working.
**Owner superseding sequence:** complete and validate human-operated terrain, loading-zone, stitched-heightmap and terrain-LOD modules **before** any corresponding agentic controls. See [`TERRAIN_WORLD_SYSTEM_PLAN.md`](TERRAIN_WORLD_SYSTEM_PLAN.md), gates T0–T9. This document covers the later agent access work, not the current first build gate.  
**Machine task ledger:** [`AGENTIC_EDITOR_ACCESS_TASKS.json`](AGENTIC_EDITOR_ACCESS_TASKS.json).

## 1. User outcome and scope

An authorized agent must eventually be able to carry out: **"Open Castle Beckenburg; inspect the second floor; find and fix misplaced geometry; furnish the room; check collisions; playtest; show me an actual rendered result."** It must perform operations on the *same* editor/game scene that human tools use, not manufacture a second approximation or merely commit scene-looking code.

Deliver the capabilities incrementally, each independently demonstrated before expanding:

1. **Observation** — live scene identity, hierarchy, components, assets, rendered view, collision/navigation and nonblocking diagnostics.
2. **Manipulation** — structured, bounded and reversible edits through the exact editor/runtime command boundary.
3. **Verification** — post-edit readback, runtime render/playtest, scene validation, stored evidence and honest success/failure results.
4. **Headless access** — scripts and CLI can carry out those capabilities without an open Swing window, while optionally attaching to one.
5. **MCP adapter** — an authorized agent can discover and invoke the same capabilities through an MCP server, without UI coordinate automation.

"One at a time" means no milestone is described as delivered based solely on a document or on components built for a later milestone. An early observation bridge can be useful without exposing mutations. No requirement to migrate other game repositories now.

## 2. Existing code and current limitations

Source inspection establishes the following **existing**, not hypothetical, components:

| Source | Verified behavior | Limit |
| --- | --- | --- |
| `src/main/java/org/beckenburg/DeveloperLevelEditor.java` | Swing editor displaying `SceneRenderer.drawCastle`; uses the Castle scene, hierarchy, inspector and ~90 ms viewport timer | No verified external machine command channel; Castle viewport is not a general-purpose headless backend |
| `src/main/java/org/beckenburg/CastleLevelEdits.java` | Persistent real Castle scene; snapshots and restore; props, lamps, architectural surfaces, stations, doors, spawn, paint, blocked tiles; revision counter | Castle-specific persistence; unsaved edits must not be mistaken for saved document |
| `src/main/java/org/beckenburg/CastleEditorProject.java` | `EditorProject` adapter exposes Castle hierarchy, asset catalog, snapshots and save | Flat representation, not yet full runtime scene/physics/typed inspector parity |
| `src/main/java/org/beckenburg/editor/RuntimeScene.java` and `InMemoryRuntimeScene.java` | Portable node IDs, parent/layer, transform/components, replace/remove and snapshots | Portable contract exists; it is not yet an authoritative adapter for all live Beckenburg scene kinds |
| `src/main/java/org/beckenburg/editor/EditorHistory.java` | Bounded transactional undo/redo; failing command restores before-state | Must become part of the authoritative machine-edit operation path, rather than a separate history |
| `src/main/java/org/beckenburg/CastleEditorAudit.java` | Diagnostics for duplicate IDs, embedded prop/light centres, blocked spawn using actual Castle collision | Advisory, limited sampling; not exhaustive mesh-overlap/physics/navigation proof |
| `tools/build-editor-kit.sh` and `tools/build-editor-kit.cmd` | Package the portable Java 17 editor classes | A distributable JAR does not imply a connected agent, live machine access or an operational MCP adapter |

Read and obey existing architecture/quality contracts: `LEVEL_EDITOR_OVERHAUL_AUDIT.md`, `MODULAR_LEVEL_EDITOR.md`, `PSX_RUNTIME_AND_EDITOR_SUITE_ARCHITECTURE.md`, `../wiki/psx-editor-engineering-guide.md`, and repository `AGENTS.md`. Preserve active recovery gates and existing game/multiplayer/updater behavior. A previously prepared but **uncommitted Git blob** is not a feature and must not be counted as delivered.

## 3. Architectural boundary — one authoritative scene

```text
  human Swing editor     headless CLI       agent / MCP client
         |                    |                   |
         +--------------------+-------------------+
                              |
              permissioned Editor Agent Service
      observe | transact | diagnose | verify | capture
                              |
        same authoritative editor/runtime scene model
              |                 |                |
          renderer         physics/nav       document IO
```

- **Game/runtime is authoritative.** No parallel "agent scene", preview-only geometry or independent physics. Existing Castle data remains a specific adapter; portable abstractions do not import Castle classes or Swing.
- **Transport is an adapter.** An in-process Java service exposes operations first; loopback HTTP/JSON, command line and MCP wrap that service rather than each implementing custom scene mutations.
- **Stable IDs and revisions.** Each operation addresses `sceneId`, `objectId`, and relevant expected revision. Never resolve a mutation by ambiguous label or approximate screen position alone.
- **Thread correctness.** Swing changes/UI capture execute on EDT as appropriate; runtime scene commits use one serialized transaction boundary. Never read half-applied edits.
- **Persistence is separate from mutation.** In-memory editing does not silently overwrite files; save uses existing authoritative persistence with atomic replacement, backup/recovery, version/readback checks.
- **Environment systems** (lighting, water, weather, audio, nav, ghost AI) remain authoritative in their own modules. Agent tooling discovers adapters and cannot silently invent a substitute system.
- **Network authority stays with gameplay host.** Editor playtests are isolated and must not mutate a real multiplayer session.

## 4. Machine protocol v1

Use a versioned envelope, independent of HTTP/MCP wiring.

**Read request:** `operation`, `sceneId`, `requestId`, optional `objectId`, filters/limits and viewport settings.  
**Write request (future phase):** additionally `expectedRevision`, typed arguments, explicit permission scope and unique idempotency key.  
**Response:** `schemaVersion`, `requestId`, `sceneId`, `revisionBefore`, `revisionAfter`, `ok`, `result` or `error`, `verified`, `evidence`.

Illustrative response (schema proposal, **not an observed result**):

```json
{
  "schemaVersion": "beckenburg.agent/v1",
  "requestId": "example-001",
  "sceneId": "beckenburg/castle",
  "revisionBefore": 42,
  "revisionAfter": 42,
  "ok": false,
  "verified": false,
  "error": {
    "code": "STALE_REVISION",
    "message": "Scene changed after the agent's inspection.",
    "retryable": true
  },
  "evidence": []
}
```

The response schema must distinguish **accepted**, **committed**, **saved**, **verified**, and **playtested**. An accepted command is not a verified scene edit. Captures are labeled genuine in-game/editor renders only when generated from the actual renderer with source revision and capture metadata. Never substitute artwork.

## 5. Failure contract — errors are expected, safe and inspectable

The requirement is **recoverability and truthful reporting**, not pretending there will be no errors.

1. Validate authentication, capability, object ID, asset reference, transform bounds, collision rules and expected revision **before** a write.
2. Capture a before-state and execute the entire logical edit as **one transaction**. On exception or failed validation, roll back to exactly that state and return a structured failure; do not leave half a room altered.
3. A command only returns `verified=true` after re-querying the same authoritative scene and comparing affected IDs, transforms, components and expected revision. A save only returns `saved=true` after a durable filesystem readback. Visual/playtest verification must be separately identified.
4. Use bounded timeouts, bounded queues, response/payload caps, cancellation, lifecycle cleanup and idempotency keys. Retrying the same edit must not duplicate props or lights.
5. Reject stale revisions before mutation; report current revision. If two agents compete, serialize writes and report conflicts, never silently overwrite.
6. Use fixed error classes: `UNAUTHORIZED`, `READ_ONLY`, `INVALID_ARGUMENT`, `NOT_FOUND`, `STALE_REVISION`, `ASSET_MISSING`, `OUT_OF_BOUNDS`, `COLLISION`, `UNSUPPORTED_CAPABILITY`, `TIMEOUT`, `SAVE_FAILED`, `RENDER_UNAVAILABLE`, `INTERNAL`. Each includes `code`, user-safe `message`, `retryable` and request ID. Never reveal tokens, local private paths or stack traces to an untrusted client.
7. **Fail closed** when readback is unavailable: return "unverified", not "success." Store redacted diagnostic logs and attach source revision / test artifacts.
8. Preserve autosave/recovery and older file versions; never destroy known-good scene data because a machine command failed. Every edit must remain undoable via the same human editor history.
9. Diagnostic geometry/art/balance warnings are **nonblocking**, except demonstrably nonfunctional build/launch/essential gameplay or required multiplayer/update behavior according to owner release policy.

**Required negative tests:** invalid mesh; invalid coordinates/NaN; stale revision; nonexistent ID; duplicate idempotency key; partial batch failure; undo/redo across human and agent actions; simulated disk-full save; renderer unavailable; terminated session; unauthorized request; TCP remote bind attempt; malformed/oversized command; two concurrent conflicting writes. Each must prove actual before/after state and correct error classification.

## 6. Execution milestones, in dependency order

### M0 — preparation and compatibility baseline (not a new feature)

Inspect current `main` SHA, relevant existing CI and recovery ledger. Verify the existing Castle editor launch, scene audit, portable runtime scene and history tests. Record exact pass/fail state, do not infer installed Windows acceptance from a Linux simulation. Preserve main branch; no destructive rewrites, no forced pushes.

**Exit:** exact SHA, reproducible test commands, current failure list and a no-regression check.

### M1 — OBSERVATION: live read-only access (first implementation milestone)

- Add a bounded, opt-in read-only observation service directly backed by the active Castle/editor scene: `capabilities`, `scene`, `objects`, `object/{id}`, `assets`, `diagnostics`, `viewport capture`. Provide stable identifiers, transforms, materials, lighting attributes, navigation metadata, revision, editor-versus-saved state and available capabilities.
- Snapshot scene data coherently; include unsaved in-memory edits; acquire viewport captures from the actual renderer, never a separately re-created mock scene. Track image dimensions, revision and render status.
- Provide an authorization-protected local machine endpoint (suggest loopback-only HTTP/JSON). **Disabled by default**, bind `127.0.0.1` only, use random per-session bearer token, do not expose token in URL, log or source control, reject non-GET methods during this phase. Enforce request/response limits. Local bridge availability is not the same thing as this ChatGPT session being connected.
- Keep the generic service interface independent of Swing; attach the Castle renderer and game-specific data through an adapter.
- Reuse `CastleEditorAudit`; label findings as advisory, incomplete centre probes, not proof of zero intersections.

**Acceptance:** start actual editor, query its unsaved scene, move a prop manually, query again and observe same stable ID with changed transform/revision; compare returned object catalog with editor hierarchy; obtain actual rendered viewport PNG; prove no write endpoint or unauthorized access; close editor and prove listener shuts down. Test launch and running scene on the supported Windows runtime. Log exact SHA, request-response evidence and screenshots.

### M2 — MANIPULATION: transactional structured commands

Implement first: `select`, `setTransform`, `placeAsset`, `setProperty`, `delete`, `duplicate`, `group/parent` where supported, `undo`, `redo`, `save`, `restore`. Only publish operations backed by real supported runtime/editor capabilities; report unsupported ones explicitly. Add typed commands for doors, lights, surfaces, spawns, nav and scene modules incrementally. Preserve IDs and asset pivots, apply collision-aware placement and use one transaction per logical edit with revision preconditions. Expose batch edits with all-or-nothing rollback.

**Acceptance:** one agent-driven Castle room furnish/repair task; exact readback, repeat/idempotency and undo/redo proof; simulated mid-batch error restores every prior object; save/reload with IDs and components identical. Both agent and human edit the *same* scene and same history.

### M3 — VERIFICATION: inspect → edit → prove

Expose explicit `audit`, `captureViewport`, `compareRevision`, `validateReferences`, `inspectCollision`, `runPlaytest`, `endPlaytestAndRestore`, and structured evidence manifest; implement only when corresponding real runtime adapters exist. The actual game renderer, navigation and collision must be used. Playtest snapshots unsaved work, runs actual gameplay in an isolated session and restores editing state without losing changes. Record frame timing where measurable; label absent metrics unavailable rather than inventing them.

**Acceptance:** actual Castle, first House, and basement scenario regression; prop-wall collision warnings, missing/unreachable stair opening, and door-opening performance diagnostics; screenshot from actual renderer, successful and failed cases; one-click equivalent playtest and exact unsaved-state return. Only claim fixture coverage for environments with a working editor/runtime adapter.

### M4 — HEADLESS automation and scripting

Build a CLI/client that connects to the same service or instantiates the runtime safely in headless mode; supports inspection, deterministic edit batches, dry-run, validate, rollback, and machine-readable output. Provide scripts for a small unrelated Java 17 synthetic scene to prove absence of Castle/Swing dependence. Headless captures need an actual supported renderer; do not fabricate a screenshot if none is available.

**Acceptance:** scripted project with no window, exit codes matching structured results, reproducible scene serialization, failed edit rollback, and match of headless vs GUI scene records.

### M5 — MCP agent adapter and supervised end-to-end task

Expose tool/resource names mapping **one-to-one** to the same service (for example `list_scenes`, `inspect_scene`, `inspect_object`, `list_assets`, `capture_viewport`, `run_diagnostics`, `apply_scene_edit`, `undo_scene_edit`, `verify_scene_edit`). Publish discoverable capability scopes, schemas and concrete errors. Local MCP uses authenticated explicit opt-in; never silently open ports or grant arbitrary file/shell access. A connected agent can perform a bounded room repair after observation and receive confirmed real-scene readback.

**Acceptance:** authorized agent connects to *running installed editor* via the intended client, lists objects, performs a reversible real edit, saves after explicit scope authorization, observes an actual runtime capture and audit, and reports what was and wasn't proved. Reject unauthorized client and cleanly disconnect; no player multiplayer exposure.

### M6 — portability and optional expansion

Once M1–M5 have evidence, generalize adapters across other game scenes and modules and prove a small independent game adapter. Consider standalone extraction only after second-project proof; avoid any speculative cross-repo migration.

## 7. Task-level delivery evidence

For **every** task store a ledger entry with:

- Task ID; capability; exact source files and affected runtime subsystem; dependencies; current status.
- Pre-change SHA and final pushed SHA; specific acceptance test(s) and results, including failures.
- Test class / workflow run URL and logs; real data and renderer captures labeled with provenance.
- Verified remote readback of committed content. A created blob or successful API write is **not** remote branch verification.
- What remains unverified, limitations, security behavior, next executable task.

Status vocabulary: `planned`, `in_progress`, `implemented_unverified`, `tested`, `blocked`. Do not use `tested` without evidence of source tests, and do not conflate source tests with live installed acceptance. Only objective essential-function breakage gates shipping; visual and editorial findings remain advisory.

## 8. First next task

**Prerequisite first: terrain-world gates T0–T8** in [`TERRAIN_WORLD_SYSTEM_PLAN.md`](TERRAIN_WORLD_SYSTEM_PLAN.md). Finish real human authoring, original terrain collision/rendering integration, stitched images, LOD/scope-safe visibility, streaming and validated loading transitions before making those features agent-controllable. The original M1-01 read-only in-process observation service is retained as a later task; M1–M5 remain planned. A portable tile module does not qualify as complete world rendering or agent access.

## 9. Current checkpoint

- **2026-10-31 module-first update:** Human-operated terrain, loading zones and stitched open-world terrain precede agent control. The initial finite-grid terrain/zone source and Blackpine height offsets have been committed; the portable stitched PNG class and focused test workflow are committed but not marked tested until actual Java 17 CI evidence exists. LOD, physics/render integration, scope-safe visibility and loading-zone gameplay transitions remain planned.

- **2026-10-31:** documentation and machine task ledger are being authored from inspected `main` baseline `3f42f2ce`. No completed agent-driven editing, Windows live-client connection, headless command execution or MCP integration is claimed by this checkpoint.
- Previous source inspection confirmed the shared scene, snapshots, current Castle-only audit and portable core. A Git object/blob not included in `main` is **not** a delivered implementation. Do not promote it as such.



---

## Part VI — Machine-readable source ledgers and update workflow

The following JSON contracts remain separate machine-readable canonical inputs. Their existence and source paths are recorded here rather than dumping raw JSON into a reader-facing requirements document: 

- \`docs/editor/requirements/DOCUMENT_CHAIN.json\` — seven-source chain, original-PDF checksums/bytes and 13-stage dependency ledger.
- \`docs/development/psx-reference-ingestion-2026-10-30.json\` — nine PSX source filenames, provenance, deduplication, adoption, SHA-256 and engine-specific decisions.
- \`docs/development/psx-editor-reference.json\` — structured PSX editor source decisions.
- \`docs/development/psx-editor-manipulation-contract.json\` — menu/save, campaign and billboard structural contract.
- \`docs/development/psx-npc-ai-authoring.json\` — NPC/AI authoring contract.
- \`docs/development/AGENTIC_EDITOR_ACCESS_TASKS.json\` — agent observation, manipulation, verification, headless and MCP milestones.

### Rules for maintaining this single master document

1. Original owner recommendations are not silently discarded, rewritten as an already-working implementation, or confused with historical test status.
2. When a new source is supplied, register filename, provenance, content coverage and concrete integration dependencies; append the complete available text or the corresponding source-based approved engineering guide in its own labeled appendix.
3. Increment the master reference version and record the linked source SHA and current CI state. Confirm remote main readback after writing.
4. Preserve source fidelity: formulas, figures, code or screenshots that are missing from extracted text must be marked as limitations, not guessed.
5. Keep the separate original guide files for software maintainers and clean Git diffs; this compiled master is a reading and discovery document, not a substitute for subsystem ownership.
6. Do not block builds for subjective appearance, balance or design preferences. Objective broken build, launch, required gameplay/persistence/multiplayer/update may block.

**End of consolidated master source text.**
