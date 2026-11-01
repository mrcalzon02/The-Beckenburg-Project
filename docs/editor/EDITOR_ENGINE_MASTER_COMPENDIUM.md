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

