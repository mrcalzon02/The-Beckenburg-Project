# Gold standards review: module-owned workspaces

The owner confirmed that the requested UI redesign targets the **topological 3D model editor**, not the main level view. Each separate module owns its workspace. A module must not inherit the preceding module's screen arrangement. Shared visual conventions, native command/history contracts and small widgets are reusable; the workflow and panel composition are module-specific.

Reference: owner-supplied *Level Editor gold standards*, section4, “Ideal Workspace UI Layout (The Golden Standard)”, the current pasted UI advice, and the [committed full standards audit](../../development/LEVEL_EDITOR_OVERHAUL_AUDIT.md). The original five-page PDF is now [archived](sources/editor-gold-standards.pdf) at SHA ea093401390be6fe445c6e989ac21fa31abfcdc4877faa6fd3a92dffa8737935 with [complete page-marked text](sources/editor-gold-standards.txt). All five pages were reviewed; the section4 table spans pages4–5. The ten core standards and four professional QoL groups remain mapped below; page5 confirms numeric scrubbing/math entry, eye/lock states, bottom-dock collapse and Play Level are distinct requirements, not completion implied by rearranging panels.

## Full standards applicability review

| Standard | Topological model editor acceptance and current evidence | Remaining work / other-module scope |
|---|---|---|
| True WYSIWYG | The same native indexed state drives human editing, previews, native persistence and checked game publication. Every published position/UV/winding is tested. | Current model viewport is a wireframe inspector, not full game material/lighting parity. Native authored-normal preservation, transforms and actual runtime normal lighting are now tested. Native embedded diffuse materials and a separate actual runtime material preview now work. Original editable GLB source bindings and model viewport/world-lighting parity remain open. |
| Placement/manipulation | Native vertex/edge/face picking; visible/X-ray box/lasso; global move gizmos/grid; previewed global rotation and axis/uniform scale, origin/centroid pivot and angle snap. | Local/normal axes, vertex/surface snap, perspective camera and rotate/scale handles remain open. World collision placement belongs to the level workflow. |
| Organization | Searchable native mesh parts select actual stable vertex IDs. Contextual properties operate the native project. | Full hierarchy/layers, eye/lock states and material/component inspectors require authoritative implementations; no inert toggles added. |
| Safety and speed | Shared preview/apply/cancel/history; Ctrl+S/O/Z/Y; topology diagnostics; stale owner/revision refusal; real GUI regressions. | Visible named history/jump, custom hotkeys, contextual menus and additional missing-asset/material validation remain open. |
| Persistence and testing | Versioned native BTOP, observed-file protection, journal recovery; deterministic validated OBJ copy/reimport and ordinary runtime publication. | Export is not a one-button level playtest. Native diffuse material GLB/OBJ-bundle interchange is verified. Full editable GLB source identities and remaining schema migrations remain open. |
| Asset management | Actual file import and five native parametric primitives are available through their own tool settings. | No unified rendered-thumbnail/tag/favorites catalog is claimed. Do not add a fake asset browser to fill a dock. |
| World controls | Model editing does not change global weather/gravity/gameplay. | Level/environment modules own these controls and their scene adapters. A model workspace may eventually own real preview-lighting settings. |
| Diagnostics | Native topology report, candidate description and geometry counts; collapsible Diagnostics browser. | Continuous FPS/draw/memory overlays and additional real runtime overlays are not complete. |
| Customization | Model-specific resizable inspector/browser, collapsible native-part outliner and Ctrl+Space Focus View with panel restoration. | Arbitrary drag docking, tear-off windows, persisted workspace presets, numeric scrubbing, pie menus and key mapping remain open. |
| Collaboration/recovery | Observed disk SHA checks and journaled native save/reopen retain existing authoritative ownership. | Automatic timed save, source-control conflict UI and collaborative locks/sublevels remain open; normal Git work is not an in-editor collaboration system. |

Professional quality-of-life review: scatter, prefab groups, replace-in-selection and world drop/alignment belong principally to level/terrain authoring and remain separately audited. Model right-click commands, selection framing/isolation, nested-part breadcrumbs, favorites and remapping remain open. Focus View expands presentation; it does **not** claim isolate-selection or camera bookmarks.

## Section4 applied to the model editor

- Center: one large working mesh view by default. Explicit Compare or pending primitive/model previews show two views derived from actual native state. Picking, area selection and gizmos use the current viewport width.
- Left: compact model tool modes; **Mesh parts** switches to a searchable outliner derived from native part names/counts. Selecting a part selects its current native vertex IDs without editing geometry.
- Right: one contextual tool form at a time, with shared native Apply/Cancel. No duplicated tab row or always-visible wall of tool parameters.
- Bottom: collapsible actual Diagnostics. An asset catalog is deferred until it has real backed content/actions.
- Top: File/Edit/View, discoverable standard shortcuts, selection/view controls and shared Undo/Redo. Ctrl+Space hides/restores panels. Essential operations retain the existing human handlers.

The main level editor's source/layout is unchanged by this checkpoint. `EditorTopologyWorkspace` is composed only by the model editor and is not a universal parent screen.

## Distinct workspace contracts for later module reviews

| Module | Its own primary work area | Its supporting regions |
|---|---|---|
| Topological model editing | Mesh geometry and native candidate comparison | Parts/selection, topology tools, contextual parameters, diagnostics |
| Terrain / stitched worlds | Landscape and tile/seam editing | Brush/layer controls, native tile ownership, route/height diagnostics |
| Animation | Model/part motion preview | Timeline, tracks/keys, clip and playback properties |
| Soundscape / audio | Actual waveform or spatial sound scene | Clips/assets, emitters/zones or routing, profile properties |
| Campaign / NPC rules | State graph or actual rule inspection | Nodes/transitions, scoped properties, test/telemetry results |
| Billboard / image assets | Source and authored image preview | Palette/aspect controls, source/export provenance |

These are acceptance/design contracts, not claims that the later screens were redesigned. Review each against its actual operations before implementation. No module may create a parallel editable representation of the authoritative scene.

## Verification and next priorities

`EditorTopologyWorkspaceGuiTest` drives native part selection, real tool navigation, comparison, menus, diagnostics/focus restoration and 900x620 layout. It proves presentation changes leave the native snapshot object unchanged. Existing human picking/gizmo/selection/modeling/export/runtime tests pass with the revised viewport geometry. `EditorTransformTest --stress` verifies120000 faces/360000 native vertices under256MiB in the independent editor JAR.

Next: finish G2 authored material interchange and editable GLB source bindings and remaining modeling/selection; improve model-specific preview rendering and active-tool context without exposing unimplemented controls. Inspect remote source-specific acceptance before any complete-stage claim. Full G0–G12 remains active.


**2026-11-02 owner clarification — Materials:** use an independent Material Editor window, with texture/UV canvas, native face assignments, material properties and actual runtime preview. Do not add material controls to the topology inspector or inherit its tool rail/layout. Both windows share the same authoritative EditorTopologyProject, stable IDs, previews, history and saves. EditorMaterialWorkspace is a separate composition; no main level layout changes. Texture painting/unwrap requirements are tracked in TEXTURE_PAINTING_INTEGRATION.md, with unfinished tools left unexposed.
