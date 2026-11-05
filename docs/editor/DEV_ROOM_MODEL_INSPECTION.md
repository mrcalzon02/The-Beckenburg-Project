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
- `gallery-model-audits.csv`: native GLB/sprite decoding, source-only classification,
  triangle/material/sample counts, and explicit missing-model findings.
- `gallery-visibility.csv`: each rendered bay's changed pixels against the
  *same camera with no model meshes*. A bay is rejected if it is blank or over
  60% of the frame is replaced by a nearby mesh/backing plate.
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

## Fast reusable native model-gallery workflow

The independent `.github/workflows/dev-room-gallery-fast.yml` is intentionally
smaller than the full DEV asset-room audit. It runs the *same* game decoder and
3D gallery renderer, validates unique stand positions, audits model visibility,
and uploads the PNG/CSV package as `beckenburg-dev-model-gallery`.
It does not run every ghost sprite, character, editor, UV A/B, topology and
infrastructure capture.

Open **Actions → DEV Room Model Gallery - Fast Inspection → Run workflow**.
Inputs are `kind` (default SOURCE_ONLY), `filter` (case-insensitive ID
substring), `wing` (zero-based, default 0), and `limit` (default 72; set
0 to consider all filtered models before selecting the wing). The workflow also
re-runs when the primitive-model builder finishes successfully on main, and
when the gallery implementation itself changes.

For example, use `kind=SOURCE_ONLY`, `filter=relic_`, `wing=0` to
inspect Castle relic studies; use `filter=level_blockout_refined_` for
the 15 independently modeled refinement fixtures. A full library can be
reviewed in successive wings without changing model origins or runtime
registration.

### Actual Windows evidence, November 5, 2026

The complete [native DEV Room run 42130219315](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/42130219315)
**PASSED** Java source compilation, 22 Castle relic/crypt source decodes,
native Castle fit studies, the 12 gallery-bay model visibility checks, the
15 refined-model GLBs and the existing per-model four-angle captures.

The preceding [run 42128827427](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/42128827427)
correctly **FAILED** after the gallery capture, revealing an off-ground
bronze astrolabe and preserving the image of floor-2/bay-2 completely occluded
by the previous bay's cannery machine (256,000 of 256,000 pixels changed).
Its repaired screenshot in the passing run shows six readable cannery models
instead; all 12 gallery-bay visibility rows are now below the obstruction
threshold. Unique model-name suffixes replace ambiguous shared prefixes on
the screenshot captions. Original source names remain in CSV.

These are real screenshots and decoder checks, **not final production texture
approval, in-world collision, player egress or runtime map placement approval**.
