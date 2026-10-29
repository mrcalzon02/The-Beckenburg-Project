# The Beckenburg Project — Menu Backgrounds Batch 01

Ten original PSX-horror menu scenes are now bundled into the **playable desktop JAR** and rendered by `MenuBackgrounds` behind the real `PsxMenuScreen` controls. The controls sit in one left-aligned panel; the right side remains available for the cinematic scene. No static menu buttons are baked into the JPEGs.

- Source assets: ten `*_menu_1920x1080.jpg` files directly in this directory (1920 × 1080 each).
- The Windows and Linux build scripts copy all ten images into `org/beckenburg/menu-backgrounds/` in the distributable game JAR; packaging fails if the batch is incomplete.
- Runtime scene choice is tied to the menu route: main Castle, extras town, options study, controls archway, solo lakeshore, display confirmation chapel and notice viaduct. The remaining forest, cloister and mill scenes are included for later mode screens.
- Rendering covers the viewport without distortion and keeps the right-hand focal area visible at narrow aspect ratios. A two-image LRU bounds decoded image memory instead of loading every source at once.
- `MenuBackgroundIntegrationTest` checks packaging, dimensions, layout at 1060×725 and 760×510, and live navigation behavior.
- `contact_sheet.jpg` remains a quick visual reference and `manifest.json` records source crop provenance and SHA-256 hashes.

These are scenic menu assets, not screenshots of existing 3D game levels. Existing in-game 3D environments, character art and gameplay routing remain separate.
