# Castle Beckenburg: room-based rebuild, 28 October 2026

User direction: rebuild the current home/menu level from first principles, with real walls, floors, rooms, roofs, lighting and switches. The quartermaster must visibly stock every equipment tier in neat shelf rows. Preserve working HQ services and the current UI work.

## Baseline and production methods

Started from main 68d1142d62a6237ff2567a54868b568b9f711487. Main's floor, doorway and menu fixes were included before inspection. Full-round40720112675 and installer40720112725 were still running at initial inspection; this is not a release acceptance claim. Later concurrent main cddba0df has full40721770890/installer40721771195 in progress and Pages40721769664 success. Reconcile it before delivery.

Applied PRODUCTION_WORKFLOW, MAP_FITNESS_CHECKLIST, DESQUAREIFY_GATE, source-mesh measurement, supported fixtures, shared visual/collision coordinates, exact GPU frame readback, independent swept routes and defect publication. Remote-site generator and hunt hiding budgets do not apply to this peaceful grid-connected HQ. The expanded castle reservation and its training bays remain plans, rather than silently becoming completed facilities.

## Construction checkpoint

- CastleRooms defines the current metre-based volumes and their floor, soffit, material and circuit identity. Thin collision raster walls are replaced with actual 420mm masonry collision. Fractional wall shoulders have full supporting floor geometry.
- Decks are unioned before construction:220mm structural slabs have tops, soffits and closed exposed edges. Stairs have closed risers on their actual approach edge, continuous support and thick side guards. Upper circulation no longer intersects the eastern pitched roof.
- Whole roofs are retained during overflight. The outdoor corner omitted by the old rectangular terrain exclusion is filled. Existing tower meshes enter the actual rendered scene, rather than remaining a metadata-only count.
- Every current room receives supported light coverage. Courtyard circulation has wall sconces. Ceiling mounts follow the slab soffits. Ground-floor room switches and an upper-library switch control their own circuits. E/L require a physical switch; an absent switch cannot crash L.
- Service fixture collision shares measured source bounds with rendering. Tabletop props follow their supports. Backwards wall shelving is reoriented. Obsolete warehouse armoires and furniture are removed from the store defaults.
-15 original open retail fixtures hold129 displays:43 catalog families, three tier rows each. Source tier aliases remain truthful for fixed fixtures. Each family owns a column; labels name its model family and tier. Item bottoms match .35/1.10/1.85m shelf tops. Fitted source bounds and rotated shelf collision leave real browsing aisles and both entrances open. Menu interactions still own purchases and save data.
- Existing pre-rebuild editor documents are copied verbatim to a before-structure-v2 backup before new defaults are installed. New-version custom edits remain persistent. Player progression is separate.
- GPU resources exclude stale .class files, preventing incremental builds from replacing source-compiled logic with old bytecode.

## Evidence and current gates

Before:21 actual RTX5070 OpenGL frames in previews/castle-survey/before. First rebuilt captures are in previews/castle-survey/rebuild; regenerate after the final source checkpoint. The capture CLI is --castle-survey-review and never consumes desktop input. It does not change the user's preferences or saves when run with isolated home/preferences.

CastleRebuildTest passes physical approaches to all stations and ground volumes, stair ascent/descent and library access, non-overlapping deck union,129 real tier-source displays, fitted support/browsing positions, actual lamp/switch attachment and local lighting controls. Existing focused Castle regressions are being run separately. Native export, final captures, final exact-source full manifest, concurrency reconciliation and deployed gallery verification remain the next gates. No installed full-case, stable30FPS, whole campaign, or full future Castle expansion claim is made.

## Reproduce

Use Java17, a local java.io.tmpdir, and VerificationPreferencesFactory to isolate settings. Build actual resources with build.cmd package and GPU sources with mvn -f gpu-client/pom.xml clean package. Never infer tests passed from a packaging command; package mode intentionally skips regression execution. Export CASTLE_REBUILD through LevelSceneSource, then build_scene.py -- castle-rebuild for the exact runtime-native scene. Preserve source PNGs and run tools/build_development_gallery.py plus --check at each checkpoint.
