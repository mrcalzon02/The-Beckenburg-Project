# Preserve and unify before performance recovery

The user requested that all existing local Beckenburg work be saved and unified before starting A–G implementation. No other game repository was modified.

Remote main inspected: `f6793bda4f1c6c821fe66e49050b076b01cce6c0`. Its installer run 40431515485 was still running at entry; it was not cancelled or counted as passed.

## Durable originals

Each checkpoint was pushed without force and independently read back with `git ls-remote`:

| Remote preservation branch | Exact SHA | Content |
| --- | --- | --- |
| checkpoint/local-map-2026-10-26 | d07d7450fb232ae233f72b386a9c6f33d53ea8ac | Native map sources, original GLBs/textures, captures, local lighting code and tests |
| checkpoint/local-mission-board-2026-10-26 | b7b193f486eff33d1a122fdaf8e62b43e349d0c2 | Mission-board UI/images and two authoring files initially excluded by sparse checkout |
| checkpoint/local-verification-2026-10-26 | 06341f8448038dbbdfe3dc499ca6baa3199c9eaa | Existing verification results; historical evidence, not fresh acceptance |

The original checkouts retain their branches and saved files. Entry manifests here record original HEAD, branch and dirty-file hashes; they are not test acceptance records. Binary patches are additionally stored outside the repositories in `integration-checkpoints/2026-10-26` on the workstation.

## Integration decisions

Current main and all three checkpoint histories are retained by merge commits on `integration/unified-map-engine`. The local `map/blackpine-mill-refinement` history was already an ancestor of main; there was no additional unique commit to import.

Most map conflicts were older local implementations of already-advanced fixes. `map-merge-resolutions.json` records the retained current implementations: physical deck depth and mounted lamps, supported source suspension/cable, newer Castle/editor/frustum behavior, bounded regional and normal-lighting caches, curated assets and mains-power behavior. The exact older alternatives remain recoverable in the pushed map checkpoint. No performance PR stack was blindly merged.

The historical leak test originally required the corrected current loft to leak. Both positive historical assertions are retained using a pre-depth fixture that removes only the actual slab colliders; an added assertion requires current production deck occlusion. Existing current-source lighting/deck tests remain unchanged and pass.

Mission-board reconciliation preserves the saved 22-chapter order (House, School, Farmhouse, then the original remaining assignments), rather than the old local alphabetical browser order. Native prototype labels cannot throw in the lobby or unlock a prototype. The picture card is reachable from the actual campaign desk and retains real dispatch actions and locks. Windows, shell and GPU packaging include its 23 original pictures.

## Actual checks at this candidate

Windows 11 workstation; NVIDIA GeForce RTX 5070, driver 616.64, 12,227 MiB VRAM; Java 17.0.12. Fresh Java compilation: 252 main/water sources and 333 test sources. Maven OpenGL compile: success, 256 sources. Gallery generation and `--check`: 913 original PNGs.

Functional passes: MissionBoardIntegrationTest, CampaignBoardPanelTest, CampaignStoryProgressionTest, CampaignCareerTest, SiteSelectionTest, LightJoinOcclusionTest, RoomLightingTest, BlackpineRitualAcceptanceTest, BlackpineLightingTest, PhysicalLightingTest, BlackpineDeckDepthTest, CastleFurnitureWallAuditTest, CastleHubIntegrationTest. Raw first-attempt failures and subsequent runs are retained in the JSON records here.

These local runs used freshly compiled classes and current repository asset overlays, with the existing installed JAR supplying unchanged original resources. They are compatibility evidence, **not** a fresh packaged installer or installed GPU acceptance. Blender is not currently available on PATH or the documented Program Files location; actual full asset assembly, exact-SHA multiplayer/GPU/installer CI and installed hardware acceptance remain pending.

Next: verify the unified candidate remotely and in full CI; resolve actual failures before promoting main. Only then resume work packages 0/1 and the gated live-resident House path. A–G remains incomplete; issue #6 and hardware 30 FPS remain open.

## Expanded unification by explicit user direction

The user additionally requested both existing feature PRs #29 (Quartermaster/branding, head `d9908a7b`) and #3 (hunter/companion abilities, head `443031a2`) and resolution of their failures. These are now reconciled into the same branch, with the existing fatal JVM evidence retention from #11. No performance prototype stack was promoted.

The first local-only unification source `d2c644a0` passed exact-head GPU run [40434626216](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40434626216), exact-head full multiplayer/desktop/native run [40434726259](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40434726259), and PR merge-context full acceptance [40434625814](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40434625814). These passes **do not accept the subsequently expanded feature integration**.

Quartermaster baking produces 71 original model thumbnails, actual Castle splash and native icon; purchasing/resale/persistence tests pass. Hunter branch's earlier failed CI was a stale compile surface; compilation against current main succeeds. A real isolated Focus test failure was caused by the same fixture retaining shared Hallow; all original assertions remain and independent expiry/overlap coverage was added. Existing real-socket recovery tests then reproduced a genuine ghost-resume regression introduced by the feature branch. Hunter-only class restoration and fresh-seat class reset repair it; ghost owner recovery, same-seat class ability and class/privacy tests pass. Before/after logs are retained here.

Three real RTX diagnostic runs at `417980e6` now exit zero after the native teardown fix; main baseline's nonzero exit is preserved, not waived. See `development/performance/hardware/2026-10-26/`. CPU p95 remains about 2.8 seconds on this cold waypoint audit; it does not meet or measure the steady interactive acceptance gate. Full local regression and new exact-SHA CI are still required for the expanded source.
