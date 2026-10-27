# Desktop icon artwork — The Beckenburg Project

The approved Beckenburg primary desktop identity for this icon pass is the **red moon / black castle** with no baked words. Master: `assets/runtime/ui/branding/beckenburg.png` (512 × 512, RGBA). The castle-and-moon is derived from the owner-reviewed Beckenburg PSX horror art directions, not an equipment stand-in.

`UiAssetBake` copies the checked-in master to `ui/branding/beckenburg.png` inside the game JAR and writes `build/Beckenburg.ico` with 16, 24, 32, 48, 64, 128 and 256px embedded PNG frames. The existing jpackage/Inno Setup installer already consumes this file as its application and setup icon. This change does **not** bypass the game's release-quality gates.

Five additional reviewed alternatives (stone castle, B crest, lantern, van and ritual cross), Windows ICO masters, Linux hicolor variants, and source-crop manifest are preserved in the [Drive desktop-icon library](https://drive.google.com/drive/folders/1LZNCynBx7Gd_fRGz-8uG5fFnvaAoeKFk) and supplied in the user ZIP. They are not auto-assigned to unrelated items or equipment. Linux `.desktop` included in the ZIP is a template and not evidence of a shipped Linux app.

### Verification expectations

1. 512×512 RGBA primary PNG is checked into git with no generated words.
2. `UiAssetBake` must fail on a missing or corrupt primary, never silently fall back to the old generic shield.
3. Confirm all seven ICO dimensions; verify the application, taskbar, shortcuts and installer use the same master on Windows.
4. Do not claim a packaged/released build until the same source SHA passes the CI release gates and real Windows install is inspected.
