# Hunter pain and rounded fallen-body acceptance

Focused implementation over main `1fe0e2490a93c5f148f9f16c686ff7794aa7617e`, compiled with Java 17 (453 main source files). Earlier controls/jump work remains on its original branch and is not part of this forward port.

- Fifteen transparent resources: four directional pain cutouts and one overhead body per Cleric, Observer and Demonologist. Only the repaired Demonologist alpha-v2 source is packaged.
- Fallen bodies are textured three-dimensional oval ellipsoid shells, with a top 0.30m above their supporting floor, curved surface normals, real per-pixel depth and an oval contact shadow. This is visual geometry, not a physical ragdoll.
- Real host health loss triggers a 0.4-second pain pose. Incapacitation overrides pain; persistent dead BODY snapshots survive subsequent broadcasts. All six seats, duplicate DOWN handling, reset/departure, photo/video parity and human-ghost telemetry privacy are checked through actual TCP host/client traffic.
- Seven focused regressions pass: HunterReactionSpritesTest, HunterReactionsNetworkTest, HunterSpriteAnimationTest, LanHunterAnimationGameplayTest, LanIncapacitationTest, CrouchNetworkTest and CameraSnapshotTest (`rounded-*.log`). Existing locomotion, authoritative movement/damage and low crouched silhouettes are preserved.
- `rounded-packaged.log` passes with the new atlas loaded from a packaged JAR. `rounded-flat-negative.log` is an EXPECTED failure: the previous flat implementation fails the new raised-volume assertion.
- `raster-oval/` contains twelve synthetic floor/depth controls: each class at four headings. Tests measure actual height variation and reject a raised flat plane; walls occlude the body.
- `runtime-oval-gpu/` contains six actual House scene pain/body images. `rounded-OpenGL-review.log` confirms NVIDIA RTX 5070 OpenGL 3.3. The capture harness awaits and pins each exact completed native frame before saving; GPU mode alone was insufficient to guarantee finished scenery.

All captures are under `development/levels/previews/hunter-reactions-2026-11-07/`. `model-production/` is source asset review. The older `raster/`, `runtime/`, `runtime-gpu/`, `exact-*.log` and `packaged-HunterReactionSpritesTest.log` are the INITIAL FLAT prototype at art/source checkpoint `6f2e0d76` (isolated 428-file baseline `62c6a66f`), retained for provenance. They do not certify the final rounded body.

DEV-335 tracks hunter reactions and rounded bodies; DEV-336 records the corrected native capture synchronization. Main CI inspected at `1fe0e249`: installer run 42422292783 succeeded; full multiplayer run 42422292792 was still running. Latest renderer workflow 42388510671 succeeded at different SHA `d7b60acf`. Those remote runs predate this feature. No installed update, full-release or older-map slope/ragdoll acceptance is claimed.
