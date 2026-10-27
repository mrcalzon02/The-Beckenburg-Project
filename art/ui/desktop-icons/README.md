# Beckenburg desktop and internal UI icon reference kit

**Primary production icon:** `assets/runtime/ui/branding/beckenburg.png` (512×512) — red moon/black castle. This is already baked by `UiAssetBake` to `build/Beckenburg.ico` and `org/beckenburg/ui/branding/beckenburg.png` in the Windows package and game JAR. The Inno Setup installer and jpackage consume the ICO. No change is made to the existing approved app mark.

**Candidate internal UI atlas:** `beckenburg_internal_ui_atlas_128.png` is a 384×256 / 3×2 grid of six candidate identity icons with 128px tiles. It is **reference-only**, not embedded in the runtime and not mapped to working gameplay controls. Use `beckenburg_internal_ui_atlas_128.json` for the crop rectangle and review status.

Preserved upstream [Google Drive icon library](https://drive.google.com/drive/folders/1LZNCynBx7Gd_fRGz-8uG5fFnvaAoeKFk) contains full 512px masters, Windows multi-frame ICO files, 16–256px Linux hicolor PNGs, launcher template, alternate styles, and the [combined desktop + internal UI kit](https://drive.google.com/file/d/1VxgXlNqZacaRAR0GsZH4f0QHSJrRUSfY/view).

The **only** selected game identity is `beckenburg`. B monogram, stone castle, lantern, van and ritual cross are candidates only, not substitutes for actual equipment or verified interface affordances. Runtime approvals must use actual gameplay/UI model semantics and accessibility checks.

Verification: source file and Inno jpackage link remotely inspected; ICO 16,24,32,48,64,128,256 embedded frames validated from downloadable artifact; packed ZIP CRC tested. Physical Windows desktop/taskbar and installer appearance remains a separate user acceptance gate. Existing GPU, full-round and installer workflows on `760fd47634d46f140be35be15241e9eb72612b8d` reported success; that does not prove post-change build acceptance.
