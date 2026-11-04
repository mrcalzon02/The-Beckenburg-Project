# Resident animation, foot shadows and complete standing boots

Started 2026-11-03; completed local acceptance 2026-11-04. Implementation commit: `47338e0b95b065138f001f5bf3ddb310513d6880`.

Scoped implementation builds over `d2f90703b556f816ff9c00c1f97ff6377b68a825`, including committed museum content at `0f718640`, while excluding unrelated pending sanctuary/ghost changes from the isolated source compilation.

- Java 17: 423 scoped main source files compile in an isolated source export.
- `exact-*.log`: focused routine, shadow, floor contact, existing hunter, dialogue, home UI, foot-anchor and billboard regressions; real three-client TCP gameplay included.
- `floor-contact-old-projection-negative.log`: old endpoint/midpoint projection fails with 49 clipped opaque shoe pixels on repaired Elsbeth art. The corrected projection passes with the same assets.
- `LanHunterAnimationGameplayTest-gpu.log`: real RTX 5070 OpenGL 3.3 acceptance for the local integrated checkout and existing native GPU backend. This run includes independent pending sanctuary source; it is not presented as exact-source CI for this scoped commit.
- `CastleResidentMotionReview.log`: twenty actual Castle scene captures covering all five residents and four routine states. Repeated draws at a single clock preserve the current walking pose.
- `CastleResidentArtRenderAudit-final.log`: forty Castle scene captures, five residents, all four views at PSX off/classic, zero missing residents.
- `castle-runtime/` and `foot-shadow-raster.png`: earlier diagnostic captures before the final standing boot repair; retained as reproduction evidence.
- Runtime captures: `development/levels/previews/resident-animation-2026-11-04/`; `model-production/animation-asset-review.png` is an asset sheet, not a runtime screenshot.

The upper character concept masters were preserved. New derivative standing sheets repair boot cutoffs for all five residents; no image has been shifted up to disguise missing shoes. The common renderer now uses exact endpoint projection and ray/card depth for shoe contact, and real receiver depth for soft circular shadows. Actor shadows do not alter depth or show on sky, floor holes or foreground walls.

Latest independently inspected existing CI: full round `41804015121` PASS and renderer `41804010652` PASS at `0f718640`; installer `41775227415` PASS at `02bf804a`. Those predate this implementation. No claim of an installed-copy update, new installer publication, complete regression-suite acceptance, or multi-PC visual acceptance is made here.

Publication: automatic approval review rejected the GitHub push because explicit source/asset export authorization is required. Local implementation and evidence are complete; remote publication awaits user approval.
