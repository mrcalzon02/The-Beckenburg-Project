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

**2026-11-01 owner priority:** first finish the small [Editor Agent Service](../../development/EDITOR_AGENT_SERVICE.md) around existing human operations and authoritative runtime adapters, then continue G0–G12. The service now has local Java 17, actual Windows editor and headless-process acceptance for native mission/Castle state, shared history, save/reopen, renderer capture and focused tests. This does not admit unfinished terrain/model/audio operations or close full G11: authenticated MCP, scripting and broader host acceptance remain gated.

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
