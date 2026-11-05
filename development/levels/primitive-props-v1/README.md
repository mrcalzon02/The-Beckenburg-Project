# Map-specific primitive prop blockouts (v1)

The authoritative planning backlog is [catalog.json](catalog.json), which maps **139 individual study models across 28 planned maps plus the Vale Mortuary expansion**. It augments the existing `development/levels/model-requirements/*.json` files rather than replacing their full room-by-room checklists.

## What this package is

- Measured, ground-origin, Y-up glTF 2.0 geometry in metres: basic boxes, faceted cylinders, and an enclosed tent prism. The colors are placeholder material factors over a deliberately tiny **valid embedded 2×2 PNG swatch**, not final authored UV textures.
- Each `level_blockout_*.glb` is an individually selectable study asset. No duplicate of any existing finished `level_*` model is overwritten.
- The catalogue's `sites[].prototypes` field is an **assignment proposal**, not an automatic scene placement. No map availability, collision, navmesh, lighting, acoustics, interactivity or network state has been modified.
- The collection contains **139 distinct GLB models made of 1,292 primitive pieces** (98 added in the expanded-map pass). It covers structural opening/rails, Castle service and training fixtures, academic/hospital/chapel and domestic interiors, forests/campgrounds/caverns, generator/electrical studies, maritime/industrial scenes, lightstation/lodge/bunker/shaft structures and mortuary fixtures. Each study remains independent and replaceable.
- Existing source LOD/original model assets remain unchanged. Completed prop models require geometry refinement, proper UV/materials, origin/orientation review, scene placement, and actual editor/renderer acceptance.

## Rebuild / inspect

```bash
python development/levels/primitive-props-v1/build.py
python development/levels/primitive-props-v1/build.py --check
python development/levels/primitive-props-v1/fit.py
python development/levels/primitive-props-v1/fit.py --check
python development/levels/primitive-props-v1/kits.py
python development/levels/primitive-props-v1/kits.py --check
python development/levels/primitive-props-v1/audit.py
```

No external Python dependencies are required. `build.py` writes independent binary GLBs to `models/` and `verification.json`; `--check` compares all files to the catalog-derived deterministic bytes. Confirm individual GLBs through the game's existing import/review path before approving their use. All source-coordinate changes should be made in `catalog.json`, not by post-export ad hoc edits.

## Priority map handoff

1. Replace closed-volume stand-ins with engineered openings and valid actual door/frame/collider bindings; inspect upper-floor and courtyard wall intersection repairs in Castle separately.
2. Test stairs and rails against flight rise, headroom, turns, landings and navmesh before populating upper floors.
3. Build authentic mechanical signatures by map: Blackpine logway, Widow's Peak tower, Dead Man's Claim mine, wharf pile bay, Blackwater turbine, Gallows Pass rail signal, Greywater processing line.
4. Make new locations identifiable: lightstation optic, MV Saint Elara moorings, Eight Pines lodge sign, Battery Brackenhead blast door, Site Cinder-6 shaft.
5. Promote only approved refinements to the actual runtime model index and authored editor scene; verify pivots, bounding extents, UVs, static-light prewarm cost and screenshot rendering.

**Acceptance:** These are real geometry files, but they are not completed production models, and their visual quality is intentionally placeholder. `verification.json` is format/integrity evidence only, not playable map acceptance.

## Phase 2: measured review placement (implemented)

The **61 route-aware review placements** are generated from the real measured
source layouts for the Castle (18), Academy (12), Prison (13), Widow's Peak (7)
and Dead Man's Claim (11). [fit-recipe.json](fit-recipe.json) specifies each
model, destination zone, facing and preferred corner. The deterministic
[fit.py](fit.py) generator computes actual floor-elevation translations, model
footprint bounds, a wall margin, buffer around planned walking links, separation
from other models and exclusion of reserved 4.4 m ritual pads.

Each site receives a detailed manifest in [placements/](placements/) and a
**standalone, nonplayable 3D review GLB** in [previews/](previews/). The preview
contains floor plates for the selected authored zones and actual graybox meshes;
the plates are only spatial context, not replacement room shells. The original
scene files, mission selector, game approval lists, collision, AI navigation,
and multiplayer authority are untouched.

```bash
python development/levels/primitive-props-v1/build.py --check
python development/levels/primitive-props-v1/fit.py
python development/levels/primitive-props-v1/fit.py --check
```

The pipeline stores hashes of the layout source, the placement manifest and
review GLB in [fit-verification.json](fit-verification.json). An edited plan
automatically makes the old placement verification stale; rerun the generator.
The scoped GitHub Actions builder uses a sparse checkout of only this asset
package and the layout plans, performs both independent byte-for-byte checks,
and commits the generated binaries using a bounded non-force push. Its generated
artifact commit is **not** a new playable-map release.

### Acceptance still required

- Inspect each primitive's actual four-angle appearance through the DEV
  showroom. It lists individual `level_blockout_*.glb` models as SOURCE_ONLY.
- Compare the proposed placement GLBs to the relevant rough-shell or authored
  production geometry; planning bounds are not the final Castle runtime walls.
- Replace schematic stair runs, guardrails, cave supports, lighthouse optics,
  and mine/cart proxies with physically engineered source geometry.
- Test in-world collisions, path clearance, door animation, UV mapping, ground
  pivots, visibility, dynamic lighting and level performance before any
  individual model can be promoted into the game's runtime model registry.
- The Castle Quartermaster model is a **single-player/co-op campaign supply
  counter** study. Warehouse shelving remains multiplayer inventory
  presentation, never a display for fixed van consoles and clipboards.

## Native Castle fit check and actual renderer evidence

**There are two different Castle coordinate authorities.** The proposed
expansion in `development/levels/layouts/castle/layout.json` spans roughly
126 × 92 metres, while `CastleHubMap` is the playable **44 × 36 metre** Castle.
The 18 proposed expansion placements in `placements/castle.json` may not be
blindly copied into the playable headquarters, scaled uniformly, or imported
as a scene. They remain useful for the expansion's future scene work.

`src/test/java/org/beckenburg/CastlePrimitiveRuntimeFitTest.java` is an
independent **native-space** authoring audit. It inspects actual
`CastleRooms` floor elevations, `CastleLevelEdits` wall/deck/prop state,
`CastleDoorSlots`, station and player-spawn reservations, stairs and
`CastleHubMap.traversable`. It searches only for clear positions and emits
`REVIEW_FIT` or `BLOCKED` for each desired primitive. It never creates or
saves gameplay scene props.

The test can temporarily enable the exact source-only GLB mesh through
`SceneRenderer.setCastlePrototypeReview(...)`. These faces are drawn using
the actual Castle camera, depth, texture sampler and lighting, **not** an
image-generation mockup. The overlay defaults to empty and cannot mutate
the persistent geometry, runtime model allowlist or the live multiplayer scene.

A Windows CI run under the
`Developer Asset Room - Continuous Visual Review` workflow writes the
native fit CSV and screenshots to the **beckenburg-dev-asset-room** artifact:
`native-castle-graybox/native-castle-fit.csv` and
`native-castle-graybox/screenshots/*.png`. It also captures four actual
renderer angles for selected individual meshes and complete proposed-map
review files. This is **advisory authoring QA**: no screenshot or
`REVIEW_FIT` alone authorizes production placement or resolves unknown
site navigation, lighting or network behavior.

To run against a source checkout with a compiled game JAR on Windows:

```cmd
javac --release 17 -encoding UTF-8 -cp build\beckenburg-game.jar -d build\test src\test\java\org\beckenburg\CastlePrimitiveRuntimeFitTest.java
java -Djava.awt.headless=true -ea -cp build\beckenburg-game.jar;build\test org.beckenburg.CastlePrimitiveRuntimeFitTest build\native-castle-prototype-review
```

If an ordinary player has modified their Castle via the editor, the audit
reads the local `CastleLevelEdits.current()` scene; CI evaluates its own
fresh default scene. Neither run edits those overrides. A blocked placement
is a real stop condition **for that proposal only**, not a release failure.


## Expanded map-model library — 139 models, 29 map kits (verified)

The expanded worklist has been authored as **98 additional primitive GLBs**,
bringing the catalog to **139 independent meshes**. The major construction
groups include:

- **Academy, asylum, college, parish and school:** cloister bays, administrator
  desk, lecture tiers, lab hood, dorm bunks, patient bath/examination carts,
  nurse records, gym fixtures, organ, bell frame, confessional, altar and
  crypt recesses.
- **Farmhouse, Briar House, manor and Castle:** loft hay, trough, root-bin,
  feed sacks, toy chest, domestic pipes, unlettered house post, grand-stair
  balustrade, chandelier, greenhouse benches, monastery well and scriptorium.
- **Forests, camps, caves, mine and research:** stopped sawmill, extractor,
  survey marker, camper shell, campsite ring, rockfall barricade, mine winch,
  inert generator, main breaker cabinet, cable tray, Northwatch airlock and
  weather instruments.
- **Coast, dam, cannery and rail:** raised marsh walk, boat haul cradle, fishing
  net support, railcar/wheelset/switch, valve gallery, dam actuator, processing
  machinery, boiler, lighthouse lantern/cistern, and dry shoreline structures.
- **Saint Elara, Eight Pines, Brackenhead, Cinder-6 and Vale Mortuary:**
  visual bow and watertight-bulkhead proxies, marine engine/pump/helm/boom,
  lodging laundry/canopy/parking equipment, empty period casemate and barrack
  lockers, open-center shaft liner, pump/grate and mortuary preparation/cold
  storage/viewing/service fittings.

**Model construction is not the same as functional gameplay integration.**
These shapes have no working interactions, opening doors, powered machinery,
lighting, sound, AI collision, networking, water mechanics or playable routes.
No existing runtime asset was overwritten. Existing source 3D models and
environment assets remain authoritative until separately reviewed.

### Review all 29 map palettes

The [site-kits directory](site-kits/) now contains **78 deterministic
source-only GLB contact-room pages**, arranged as 3 columns × 2 rows per page.
Each specimen keeps its original scale in metres, and the stand's floor
position is computed from the source geometry rather than assumed. See
[site-kits/index.json](site-kits/index.json) to identify each of the
**402 displayed map-specific model assignments**, including an exact model ID,
slot, source ground offset, page and SHA-256 digest. The six universal
architecture/utility models remain assigned to each location but are not
duplicated physically on every gallery page.

Open the individual GLBs in [models/](models/) for close inspection, or
the site-specific sheets for whole-map family comparisons. The contact
rooms are NOT placements in proposed level shell coordinates; the 61
route-aware arrangements in [placements/](placements/) remain a **separate**
five-map experiment. In particular, expanded-Castle coordinates are not
playable-Castle coordinates.

### Independent integrity gates

[build.py](build.py) regenerates all model binaries. [fit.py](fit.py)
regenerates the five route-aware placement previews. [kits.py](kits.py)
regenerates the 29 map portfolios. [audit.py](audit.py) independently parses
the GLB headers, indexed triangles, UV/material accessors and embedded
PNG bytes (signature, CRC and lossless decompression), compares SHA-256 hashes,
checks explicit catalog assignment symmetry and verifies every kit's contents.
No dependency beyond the Python standard library is required.

The GitHub Action publishes the reproducible binaries via a bounded non-force
push and checks their exact bytes. This is **mesh-format, integrity and
authoring-gallery validation**, not in-engine 3D appearance, actual texture
art, or mission-playability acceptance.

The next acceptance pass is individual four-angle in-engine source preview,
room-shell geometry matching and replacement of simplified constructions with
appropriately textured, properly anchored and collider-validated meshes.
