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

The workflow enumerates the actual GlbProps.availableModels, EquipmentModelCatalog, ImportedAssetLibrary, GhostIdentity.Type and CharacterVisuals registries automatically. It also directly previews source-only GLBs in the developer checkout using the same original GLB texture sampler; installed builds show only packaged/approved assets and their separately registered local imports. It captures the actual 3D renderer in six-stand PNG contact sheets with a separate caption band, reports every asset and uploads them to GitHub Actions artifacts.

**Cosmetic and visual asset reviews are diagnostic only and must never block builds or releases.** Source compilation, launching, required game functionality, and the existing multiplayer acceptance remain functional gates. An automated decoder pass does not certify visual fidelity; a human reviews the captured geometry and source art.

This room is not meant to alter campaign saves or add a hidden dev player to standard matchmaking. It exists solely as a permanent, repeatable visual QA scene.
