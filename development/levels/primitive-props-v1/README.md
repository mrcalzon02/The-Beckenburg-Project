# Map-specific primitive prop blockouts (v1)

The authoritative planning backlog is [catalog.json](catalog.json), which maps **283 individual study models across 28 planned maps plus the Vale Mortuary expansion**. It augments the existing `development/levels/model-requirements/*.json` files rather than replacing their full room-by-room checklists.

## What this package is

- Measured, ground-origin, Y-up glTF 2.0 geometry in metres: basic boxes, faceted cylinders, and an enclosed tent prism. The model material colors are placeholder glTF factors, now complemented by **individually embedded deterministic 32×32 procedural RGB albedo studies** with nondegenerate planar UV0. These do not replace final hand-authored textures.
- Each `level_blockout_*.glb` is an individually selectable study asset. No duplicate of any existing finished `level_*` model is overwritten.
- The catalogue's `sites[].prototypes` field is an **assignment proposal**, not an automatic scene placement. No map availability, collision, navmesh, lighting, acoustics, interactivity or network state has been modified.
- The collection contains **283 distinct GLB models** (116 expanded-map additions, three countable sarcophagi, and 18 additional relic/display definitions). It covers structural opening/rails, Castle service and training fixtures, academic/hospital/chapel and domestic interiors, forests/campgrounds/caverns, generator/electrical studies, maritime/industrial scenes, lightstation/lodge/bunker/shaft structures and mortuary fixtures. Each study remains independent and replaceable.
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


## Expanded map-model library — 178 models, 29 map kits (verified)

The expanded worklist has been authored as **98 additional primitive GLBs**,
bringing the catalog to **178 independent meshes**, including three closed sarcophagus variants and twelve individual Castle relic artifacts. The major construction
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


## Expanded Castle Beckenburg crypt: individually counted sarcophagi

The Castle has **two different subterranean concepts** that must not be
confused:

- **Existing playable Castle:** Its \`CastleCellarLayout\` currently declares a
  stair, cellar service hall, a *captured-relic vault*, and a plant room.
  **0 crypt chambers / 0 placed sarcophagi**. None of those four rooms is
  authorized as a tomb or crypt.
- **Proposed expanded Castle:** The planned chapel has a proposed *separate*
  two-chamber subterranean crypt below it. Source of truth:
  [castle-crypt-proposal.json](castle-crypt-proposal.json). The *provisional
  authoring count is 8 sarcophagi*, split **4 west / 4 east**. Three different
  rough GLB sarcophagus models are individually enumerated: 3 plain, 3
  inscribed, and 2 effigy. These counts are not invented runtime installs.

The [crypt.py](crypt.py) generator validates each of the eight distinct
sarcophagus IDs against room margins, source-model footprint, vertical
clearance, nonoverlap and a reserved **2.0 m** central aisle. It writes
\`crypt-review/castle-crypt-count.json\` with all eight world-space footprints
and both chamber and model totals, and
\`crypt-review/castle-crypt-preview.glb\` for an open-floor spatial review.
A matching \`--check\` verifies all bytes deterministically; the same step
is wired to the primitive-map-props GitHub Action. There are no built
crypt walls or stairways in that study and no automatic game scene injection.

The proposed subterranean area uses **the expanded-map chapel's own
coordinate system**, NOT the playable 44x36 m Castle. The existing relic
vault must never silently become a crypt. Future phase: build supported
crypt architecture and routes, re-fit the final models and perform actual
editor, lighting, navmesh, collision and four-angle runtime visual checks
before converting **proposed** counts to **installed** counts.

\`\`\`bash
python development/levels/primitive-props-v1/crypt.py
python development/levels/primitive-props-v1/crypt.py --check
\`\`\`


## Relic collection and model refinement — source geometry completed

The planned **captured-relic vault is a storage/exhibition area, not a
sarcophagus crypt**. Current development now includes **12 distinct
physical relic meshes**:

Mourning locket; votive handbell; bronze astrolabe; sealed funerary urn;
tarnished black mirror; padlocked folio; lidded reliquary box; pilgrim
lantern; ossuary keys; closed censer; ceramic funerary mask; and ceremonial
chalice.

Each is an individual named source-only GLB in [models/](models/), not an
invisible generic placeholder. The set also includes **six new display or
handling fixtures** (padded tray, locking showcase, barred shelf, placard,
repair stand and tall open-frame pedestal), and an improved existing relic
containment plinth/cage. This creates **18 additional individual model
definitions** beyond the previous 142; the containment plinth refinement
does not create another model ID.

The [relic-review inventory](relic-review/relic-inventory.json) counts **12
separate relics in 12 open-frame stands**, split into two banks of six, and
keeps a protected central approach near the planned vault stair. Both banks
have actual 3D binary reviews:
[west-vault-bank.glb](relic-review/west-vault-bank.glb) and
[east-vault-bank.glb](relic-review/east-vault-bank.glb).

**Important:** Twelve is a proposed artifact-display collection,
**not 12 actual captured relics or 12 playable Castle installations**.
Collection names are art-study labels, not lore or ghost identities.
The in-engine evidence, ritual-tier, captured-item and multiplayer systems
remain authoritative. This source package does NOT silently add treasure,
lore rewards or objects to campaign inventory, and the expanded Castle plan
uses different coordinates from the native 44×36m headquarters.

The 8 sarcophagi remain a **separate, still-unbuilt below-chapel crypt
proposal**, not mixed into the captured-relic vault.

### Refinement and acceptance checks

Every source GLB now has spatial planar UV0 coordinates and a separate
tiny (32×32) material-specific procedural RGB texture for each material:
stone/mineral mottling, wood/leather grain, cloth weave, brushed metal,
bronze/patina and ceramic tone variation. Textures use glTF nearest
filtering and repeat wrapping, appropriate to a low-resolution PSX-style
study. Glass remains **opaque placeholder material**; it is not a final
translucent PBR/glass shader. Model shapes remain intentionally low-poly
and require visual art review, proper authored albedo/normal maps where
appropriate, and native renderer/collision inspection.

The mesh builder and independent audit decode all embedded PNGs, check
CRC/scanlines and meaningful source UV area, and enforce exact source-only
approval boundaries. The [relics.py](relics.py) layout generator validates
unique identities, physical support area, fit under the roof of the
review case, side-bank separation and reserved stair/aisle.

\`\`\`bash
python development/levels/primitive-props-v1/build.py --check
python development/levels/primitive-props-v1/relics.py --check
python development/levels/primitive-props-v1/audit.py
\`\`\`

The \`CastleRelicSourceDecodeTest\` runs **actual game GLB decoding and
texture sampling**, with four-angle screenshots of representative models
via the existing Windows developer-room workflow. The Java and screenshot
checks are diagnostic, and the model count must not be mistaken for
playable-scene integration.

## Next primitive refinement pass — 18 additional source models

This pass grows the catalogue to **178 individually authored source-only GLB models** across the existing 29 planning kits, without replacing existing production meshes. New individual fixtures cover Castle tapestries/ossuary displays, chapels and cemeteries, hospital and mortuary carts, school book trolleys, prison cots, logging sawhorses, harbor rope coils, marsh reeds, railway buffers, cannery cargo, a lighthouse foghorn, coastal supply crates, research benches, motel ice machines, dam gratings, decontamination arches and mine hand tools.

Geometry repairs include an **open-centre shaft ring** (the old cap incorrectly closed the shaft), **proper vertical-plane mine fan blade roll**, a **3+2+1 physically staggered log stack**, an actually clear doorframe and stone rather than wood elements on a plain sarcophagus. All three counted sarcophagus variants received additional small sculptural details, without altering their **eight proposals / zero playable installations** status.

The builder accepts optional `roll` on box parts: local XY rotation precedes the existing Y-axis yaw; measured placement footprint bounds also account for roll. This does not add gameplay hinges, lit candles, active foghorns, open shafts in production geometry, door portals, navmesh/collision, pickups or burial records. The new `geometry_regression.py` independently checks critical shapes and committed GLB identities before any future promotion.

## Refinement pass three — 43 source-only models, 221 total

Expanded all 29 planning kits with independent object IDs and geometry for reusable architecture and site-appropriate furnishings. The individual models include open window casements, genuinely open stone/mine arches, pitched timber trusses/roofs, modular landings, supported piping, medieval wall sconces, tapestries' companion paintings/rugs/statues/seating, Castle warehouse portable-field supply crates, complete domestic stove/fireplace/chimney/bookcase/porch pieces, school and asylum furniture, prison furnishings, forestry and mine machinery, real outdoor/cemetery/harbor pieces, and landmark-specific ship, lighthouse, bunker, hydroelectric and research station fixtures.

All newly defined models contain `authoring_hints` for expected mount, facing, units, collision review, and final editor approval. They are **NOT automatically inserted into playable levels**; no gameplay inventory, lighting, navigation, collision, visual acceptance, or mechanics is claimed. The eight Castle crypt sarcophagi are still only proposed, not installed. Source-only gallery generation and byte-wise GLB validation are performed by `primitive-map-props.yml` and `geometry_regression.py` after each commit.

### Follow-on geometry correction — inclined cannery chute

The cannery chute source model now rotates its deck and both guard rails together by 10 degrees, with four vertical supports recomputed to contact the actual sloped underside and remain floor-origin. The source regression checks the front/back posts against the inclined deck analytically; a tilted visible panel no longer floats above unchanged post heights. This is an authoring model only, not operational conveyance.

## Geometry refinement: frustum low-poly source primitive

The deterministic builder now accepts source parts of `type: frustum` with `bottom_radius`, `top_radius`, `height`, and optional `segments` / `axis` / `yaw`. A zero upper radius creates a real point with nondegenerate triangles, enabling tapered stone spires, trees and funnels without cylinder-stacking. The footprint fitter and independent binary integrity validator both recognize the new primitive. The existing cavern stalagmite cluster now has actual tapered facets instead of flat cylinder tops.

This is a source-model enhancement only. Existing gameplay geometry, collision, navmesh and lighting are not modified.

## Location-specific modeling: 32 additional independent studies, 253 total

Following the validated tapered primitive, the library adds 32 separately selectable GLBs for the maps with the thinnest distinctive prop coverage. Camp Hollow Creek and the wilderness receive clustered conifers, tables, notice kiosk, water tank, open cooking shelter, timber footbridge, deadwood snag and raised blind. Red Mesa and Dead Man's Claim gain pointed rockfall, ceiling stalactites and a survey instrument. Widow's Peak receives a map table and reservoir. Gallows Pass gets a straight rail/tie section, loading platform, telegraph mast, signal relay box, handcar and coal bunker. Morrow Point gets maintenance steps, oil rack, weather vane, ring rescue float and landing capstan. The mortuary gets a closed bier, cart, three drawers and wreath. Site-appropriate supporting assets include an industrial handwheel, open ship hatch, fishing crates and an open bunker frame.

All source meshes retain the non-playable approval gate. Neither individual asset authorship nor the site kit galleries imply working props, world placement, finished textures, collisions, navmesh, lighting, waterproofing, hanging/tower access or captured relic/burial state. Preview geometry is for refinement and verification before selective editor import.

## Automated source mesh-readiness audit

`source_readiness.py` produces `readiness-report.json` on each library build and verifies the output byte-for-byte. It independently opens the GLBs and tracks mesh dimensions, bounding extents, count of tapered parts, material families, map-kit memberships and source hashes. Diagnostic flags identify legacy models without authored mount metadata, uncertain ground/attachment pivots, overly tall studies or model footprints likely to overrun the review stand. Warnings are authoring guidance, not release blockers.

**Critical:** This report verifies geometry only. Every source-only model still has `native_renderer_accepted=false`, `collision_and_navmesh_accepted=false`, `runtime_editor_registered=false`, and `production_approved=false`. No diagnostic report is allowed to promote an asset or silently add it to a scene. The generated summary provides an exact list for later model-by-model four-view inspection and measured scene placement.

## Readiness audit follow-through: bed feet and contact-gallery sizing

The source-readiness audit found the motel guest bed hovering **0.12m** above its local floor. Four grounded timber feet now make source contact at exactly y=0. The existing passenger coach measured 5.825m in depth, beyond the original fixed 5.60m review plinth. Map-kit generation now gives every model a footprint-aware review stand (up to 6.6m on 7m centers), without shrinking models or clipping the coach. The readiness diagnostic uses the same 6.6m threshold. Tall lookout towers, radio masts and signal poles remain legitimately flagged for camera framing rather than being scaled down.

## Explicit provisional mounts for the entire source catalogue

The initial 178 primitive studies now carry explicit editor-authoring mount categories: floor, wall, ceiling, roof, opening, structure, terrain, water and display surface. Existing newer source objects retain their prior mount choices. All **253 models** now have `mount_confidence="provisional_unreviewed"`, `source_pivot_reviewed=false`, `renderer_orientation_reviewed=false` and `runtime_placement=false`.

Classification is a physical placement *proposal* only, **not** a verified orientation, correct pivot, collider or actual editor import. Readiness diagnostics now distinguish wall/ceiling/doorway attachment from structural, water, terrain and relic display constraints, while preserving all legitimate height and ground-offset warnings. Nothing changes live map geometry or gameplay state.

## Primitive geometry production pass — 30 additional studies (283 total)

Each newly constructed low-poly model has its own GLB identity, source geometry, map-kit assignment and provisional physical mounting contract.

- **House/Farm/Mansion/Lodge/Lightkeeper (6):** paired shutters, open roof gutter, supported downspout, separated picket fence, framed gate and timber cellar staircase.
- **Camp Hollow Creek (5):** real bathhouse privacy partitions, field cot, separated tent entrance flap geometry, stacked log lean-to and manual pump.
- **Red Mesa caves (5):** ledge boundary posts, open rope bridge, survey stone cairns, geological wall strata and supported excavation trestle.
- **Gallows Pass Railway (5):** four-post platform canopy, raised semaphore, wall clock, wheeled luggage cart and open tunnel mouth.
- **Morrow Point Lightstation (5):** twelve-part open tower wall course, individual radial stair-tread module, keeper porch, optic pedestal and rockside storm drain.
- **Vale Mortuary (2):** basin/faucet station and reception visitor register with separate bell.
- **Castle crypt proposals (2):** funerary stone column and standalone support for existing sarcophagus variants.

No source asset is automatically imported into a level, promoted to a runtime approval, used as wall collision or attached to a real shaft. Shaft rings remain open; Castle's eight sarcophagi remain proposed and **zero installed** in the current playable scene. These models still need native four-angle screenshots, material UV inspection, editor pivot/orientation acceptance, clearance and collision/navigation tests.
