# DEV Room model inspection — reusable multi-floor gallery

The existing **Developer Asset Room** (not the Castle lobby or a playable mission)
now has a physically stacked inspection wing. It reuses the actual source/runtime
GLB decoder, material/UV sampling, depth-tested scene renderer, sprite sources,
original-texture capture, and manual art-review CSV.

## Gallery address and scale

| Unit | Count | Position |
| --- | ---: | --- |
| Gallery wing | 72 model positions | Virtual paging; no finite asset-library limit |
| Floor | 3 per wing | Y=0, 4.4, and 8.8 metres |
| Bay | 4 per floor | 16 metres between bay origins along Z |
| Exhibit stand | 6 per bay | 3 across X and 2 rows on Z |

The gallery has **12 physical bays / 72 stands per wing**. Wings are successive
non-overlapping pages of the sorted/filtered asset catalog. The layout contract
is `src/main/java/org/beckenburg/DevAssetGalleryLayout.java`; increasing its
`FLOORS` constant extends the floors, but deck dimensions and gallery tests
must be updated before using a different building envelope.

Every stand uses the same original GLB triangles and textures as the prior
DEV room. Models may be normalized **for inspection** to fit a stand, but
their source files, pivots and runtime registry are never rewritten. Individual
mesh audits report source coordinates rather than falsely certifying this
review scaling as in-world fit.

All three floors have physical world-Y decks and a 3 × 4 metre lift void in
the upper decks. Floor and bay controls teleport the *inspection camera*.
There is no generated elevator interaction, navmesh, collision certificate,
map room, or player-gameplay transition. Source-only models remain unapproved.

For interactive work the renderer draws every stand/deck but decodes only
nearby model meshes on the camera's floor. This avoids rendering 72 large
GLBs every frame while preserving their absolute inspection addresses.
Free-flight across a floor/bay boundary updates the active label/selection.
The existing separate topology and infrastructure side-aisle diagnostic modes
retain their original six-stand rendering path.

## Interactive controls

Start the existing Developer Asset Room in a game source checkout (as before).
The catalog remains searchable and has a `SOURCE_ONLY` category.

- **Wing - / Wing +** or `[` / `]`: move through sets of 72 models.
- **Floor - / Floor +** or `F9` / `F10`: jump between the three real levels.
- **Previous bay / Next bay** or `PgUp` / `PgDn`: move between sets of six; this crosses floor and wing boundaries.
- **1–6**: move the inspection camera to the selected stand.
- **WASD**, **Q/E**, mouse drag and wheel: unrestricted free-flight inspection.
- **R**: rotate displayed models 45 degrees; **F12**: save a native renderer PNG.
- **Rescan registry**: pick up newly generated source files without importing
  them into the shipping registry. Manual review records do not approve gameplay.

Selecting an asset in the left catalog immediately resolves its exact wing,
floor, bay and stand; no manual search among empty floors is necessary.

## Repeatable headless gallery runs

From a built Java 17 checkout, using the same `beckenburg-game.jar` as the
ordinary single-model capture path:

```text
java -Djava.awt.headless=true -cp build/beckenburg-game.jar org.beckenburg.DevAssetShowroom --dev-room --gallery-capture --kind=SOURCE_ONLY --limit=72 --output=build/dev-room/multi-floor-source-gallery

java -Djava.awt.headless=true -cp build/beckenburg-game.jar org.beckenburg.DevAssetShowroom --dev-room --gallery-capture --filter=level_blockout_refined_ --output=build/dev-room/refined-gallery

java -Djava.awt.headless=true -cp build/beckenburg-game.jar org.beckenburg.DevAssetShowroom --dev-room --gallery-capture --wing=1 --kind=SOURCE_ONLY --output=build/dev-room/source-wing-2
```

`--kind` accepts one of the native DEV categories; `--filter` is an
ID/key substring (case-insensitive), `--wing` is **zero-based**, and
`--limit` is an optional max number of entries before wing slicing.

Each output includes:

- `gallery-placements.csv`: the exact source identity and immutable
  floor/bay/stand coordinates for every displayed model.
- `gallery-wing-NNN-floor-NN-bay-NN.png`: **actual renderer** scene images
  with captions placed below, not on top of the inspected materials.
- `gallery-summary.txt`: actual captured counts and review-only status.

The original `--capture --asset=...` four-angle workflow and other
single-room test modes remain intact. Never substitute the gallery
contact-sheet image for a four-angle close-up or a source UV audit.

## CI and acceptance boundaries

`.github/workflows/dev-room-asset-review.yml` now verifies the wing-floor-bay
coordinate mapping and asks the Windows renderer to capture 12 bays across
three floors, followed by the dedicated 15-model refinement selection.
Screenshots, manifests and test logs are exported in the
`beckenburg-dev-asset-room` artifact. Visual deficiencies remain advisory,
but a Java build failure is a real implementation failure.

This gallery does **not** assert navigable map floors, source-model placement
correctness, collision accuracy, rendered texture artistry, editor save/import,
site power, lighting prewarm, or multiplayer authority. Each model still needs
normal map fitness, fit/origin/attachment, and actual scene acceptance before
shipping.
