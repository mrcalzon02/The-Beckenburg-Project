# Van information monitor legibility — 2026-11-18

**Defect:** DEV-387 · **Code commit:** [96c4b535](https://github.com/mrcalzon02/The-Beckenburg-Project/commit/96c4b535c64d2a26a2ee406ca3b7ba6c179a883c)

## Reported reproduction

The user supplied a 2048×713 van screenshot showing fragmented, unreadable text across physical CAMERA, ACTIVITY, SOUND, TEAM and CONTRACT monitors. In the authoritative code these monitors authored a 320×180 surface using 11–13px secondary text. The GPU used nearest filtering and world-PSX snap/affine UV, while the map's special material had a mismatched 128×128 reported upload size despite its 768×512 source.

## Scoped correction

- 480×270 antialiased live face bitmaps with a sparse 320×180 virtual layout, larger headers/rows and pixel-width-clipped status/objectives
- Readable diegetic faces skip PSX vertex snap and affine UV distortion in both software and GPU world projection; all regular world materials retain their authored PSX effects
- GPU displays use linear/clamped sampling; map size follows the source image, and the GL texture is recreated on resolution transitions
- Regression assertions added to VanLiveScreensTest and DynamicTextureRevisionTest for dimensions, material identification, independent revisions and map sizing

## Actual verification

- GitHub main ref was updated to 96c4b535 and new file bytes read back by SHA
- [GPU renderer acceptance workflow 44436638667](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44436638667): `compile-gpu-java17` job **SUCCESS** on the exact commit. Broader workflow was in progress at documentation time
- Physical GPU readback/screenshots, detailed text-legibility review and two targeted tests **not yet verified**
- Exact-commit installer and full multiplayer workflows were **canceled**, not accepted

## Required closeout

Capture 1080p physical in-van monitor images at ordinary standing/aisle distance and close inspection on GTX 1660 SUPER. Verify all five panels, map and camera frames, solo and at least three-seat multiplayer, with PSX CLASSIC, graphical detail Balanced/High and optional chromatic/scanlines. Confirm full-screen vs expanded text, no camera privacy or hidden-evidence leaks, texture upload/rebuild stability, no frame-time regression and no accidental blur of normal retro scenery. Only then close DEV-387.
