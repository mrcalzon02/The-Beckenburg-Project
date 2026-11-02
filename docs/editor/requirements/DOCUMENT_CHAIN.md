# Editor requirements document chain

**Canonical index:** [DOCUMENT_CHAIN.json](DOCUMENT_CHAIN.json) (machine-readable and versioned).
**Implementation sequence:** [STAGED_INTEGRATION_PLAN.md](STAGED_INTEGRATION_PLAN.md) (gates G0–G12).
**Current scope:** portable Java 17 LevelEditorKit with game-owned Beckenburg host adapters.

This chain **extends** the already committed [terrain requirements](../TERRAIN_REQUIREMENTS_INTEGRATION.md), [landscape layering](../LANDSCAPE_LAYERING_PREPARATION.md), [PSX runtime/editor architecture](../../development/PSX_RUNTIME_AND_EDITOR_SUITE_ARCHITECTURE.md), [gold-standards audit](../../development/LEVEL_EDITOR_OVERHAUL_AUDIT.md), adaptive topology, layered caves, multilayer navigation and the [agentic access contract](../../development/AGENTIC_EDITOR_ACCESS.md). None is removed or superseded.

## Ordered owner-source register

| # | Owner-supplied PDF | Registered requirement document / source transcription | Intake state |
|---|---|---|---|
| 1 | Level Editor gold standards.pdf (5 pp) | [Original](sources/editor-gold-standards.pdf), [complete text](sources/editor-gold-standards.txt), [module audit](MODULE_WORKSPACE_AUDIT.md) | Original hash/size and all5 pages checked |
| 2 | Terrain editor requirements.pdf (17 pp) | [Original](sources/terrain-editor-requirements.pdf), [complete text](sources/terrain-editor-requirements.txt), [terrain contract](../TERRAIN_REQUIREMENTS_INTEGRATION.md) | Original hash/size and all17 pages checked |
| 3 | Landscape layering.pdf (8 pp) | [Original](sources/landscape-layering.pdf), [complete text](sources/landscape-layering.txt), [four-tier contract](../LANDSCAPE_LAYERING_PREPARATION.md) | Original hash/size and all8 pages checked |
| 4 | Topological editor.pdf (36 pp) | [Original](sources/topological-editor.pdf), [direct extraction](sources/topological-editor-pdf.txt), [prior transcription](sources/topological-editor.txt) | Original matches pre-existing SHA; all36 PDF pages checked |
| 5 | Building a 3D Soundscape Environment Manager.pdf (19 pp) | [Original](sources/soundscape-environment-manager.pdf), [direct extraction](sources/soundscape-environment-manager-pdf.txt), [prior transcription](sources/soundscape-environment-manager.txt) | Original matches pre-existing SHA; all19 PDF pages checked |
| 6 | Engine base.pdf (29 pp) | [Original](sources/engine-base.pdf), [direct extraction](sources/engine-base-pdf.txt), [prior transcription](sources/engine-base.txt) | Original matches pre-existing SHA; all29 PDF pages checked |
| 7 | multi-genre game creation engine.pdf (19 pp) | [Original](sources/multi-genre-game-creation-engine.pdf), [direct extraction](sources/multi-genre-game-creation-engine-pdf.txt), [prior18-marker transcription](sources/multi-genre-game-creation-engine.txt) | Original matches pre-existing SHA; complete19-page extraction now included |
| 8 | Texture Painting.pdf (10 pp) | [Original](sources/texture-painting.pdf), [complete text](sources/texture-painting.txt), [acceptance map](TEXTURE_PAINTING_INTEGRATION.md) | Original hash/size and all10 pages checked; native material/UV foundation partial |

**Provenance and fidelity (2026-11-02):** all eight original PDFs are now archived (143 pages total), recovered from the owner's Downloads. The four formerly pinned original hashes match exactly; hashes for the three older previously unpinned sources are newly established. Complete direct PDF text extraction has ordered original page markers and LF hashes. Earlier four transcriptions remain unchanged, including the old18-marker multi-genre extraction; the original and new19-page extraction resolve that coverage ambiguity. Text extraction does not reproduce PDF tables/diagrams, so the exact original binaries remain authoritative. `python tools/audit_editor_requirements.py --require-originals` passes with0 errors/0 provenance gaps. This accepts G0 intake/indexing, not runtime features or later gates.

## How to continue the sequence

1. Open [STAGED_INTEGRATION_PLAN.md](STAGED_INTEGRATION_PLAN.md), select the earliest unaccepted gate whose dependencies have passed and check the linked original source requirements.
2. Open the corresponding real implementation modules and prior contracts; perform one integrated implementation slice. *Do not construct an alternative scene/runtime/renderer just to satisfy an example.*
3. Run standalone Java 17 tests, applicable host runtime/CI tests, actual save/reload and real gameplay/dev-room evidence where relevant. Link precise SHA and workflow run.
4. Update the stage status in [DOCUMENT_CHAIN.json](DOCUMENT_CHAIN.json), the relevant earlier subsystem contract, and [recovery checkpoint](../../../development/RECOVERY_PLAN_2026-10-25.md). Mark a stage complete only when its own acceptance evidence exists.
5. Keep optional plugins, new JDK/native capabilities and agent access behind explicit scope/feature gates. No unrequested changes to other game repositories.

## Priority and compatibility

G1 portable contract and G4 unfinished real-terrain binding are immediate foundations. Work on G2 topology and G5 pure audio documents may proceed independently after their corresponding shared data definitions exist. G8 registry and G9 genre tools follow deliberate plugin boundaries; G10 script and G11 agent execution are **after proven human editing**, not substitutes for it.

**Java 17 warning:** the source PDFs include newer Java examples (virtual-thread executors from Java 21, finalized java.lang.foreign from Java 22 and later record-pattern switches), unimplemented audio DSP stubs, and assumptions about script sandboxing. These must be adapted and validated; never copy them verbatim and call them integrated.

**Test policy:** actual broken build, launch, save/reopen, required gameplay/multiplayer/update are release blockers. Source-document format checks, artistic quality, rendering appearance and audio taste are review diagnostics only.

## Consolidated all-in-one master (2026-11-01)

**[Editor, Engine and Game-Creation Master Compendium](../EDITOR_ENGINE_MASTER_COMPENDIUM.md)** is the single long-form reading document for this full editor creation effort. It includes the integrated recommendations, the G0–G12 staged plan, all four new source-PDF text archives, full copies of previously committed PSX editor/animation/AI/audio/menu/asset guides, and detailed terrain/landscape/topology/navigation/lighting/water/model review/agent contracts: **29 labeled source sections in one file**. It also indexes the earlier nine-PDF PSX reference ingestion (including duplicate source names and recorded checksums) and the owner's inline menu/campaign/billboard brief.

Unlike this seven-source intake register, the master joins the earlier independent PSX editor engineering document family as well. It is a compiled reference, **not** a replacement for original files or proof all its recommendations are implemented. Original PDFs remain authoritative for diagrams and page layout. The full master is also mirrored into the owner's Google Drive as a native Google Doc, title: *Beckenburg Editor & Game Creation — Master Recommendations Compendium (2026-11-01)*; the repository remains the versioned textual source of truth.
