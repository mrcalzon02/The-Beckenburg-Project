# Existing DEV Room — Infrastructure Material and Water Demonstration

This is a toggleable, substantial construction exhibit inside the **existing**
`DevAssetShowroom` side aisle, not a new gameplay map, second renderer or
unreviewed replacement art. Default six-model bays remain unchanged.

## Open and review

Run `dev-room.cmd`, open the normal DEV model/character room, and click
**Infrastructure demo on/off**. Use WASD/QE free-flight; inspect all three
sections, then **Save actual renderer PNG** for a real source/model review.

The exhibit occupies the showroom's previously unused side aisle:

- **Rail sample:** genuine `TerrainRailMesh` ballast, individual sleepers and
  two extruded steel rails using its existing profile and mesh triangles.
- **Road sample:** physically graded lateral shoulders, crowned asphalt,
  pavement markings, surrounding ground and a run of round wooden utility
  poles with actual crossarms and suspended two-wire spans.
- **River sample:** real channel bed with sloped bank geometry, exposed
  sediment, rock GLBs, grass edges, and two true `WaterPlacement` bodies
  joined through `WaterSnap`. Water body footprints are explicitly rendered
  as *diagnostic geometry*, not passed off as live water shader screenshots.
- **Vegetation and rocks:** the real packaged GLB assets such as
  `level_tree_conifer`, `level_tree_broadleaf`,
  `level_camp_pine_refined_a`, `level_woodland_boulder` and
  `level_camp_siteart_rocks`; the same `SceneRenderer.imported` path used
  by the regular DEV room. If a model is not approved for gameplay, its real
  source-only model is loaded through the review-only importer, not granted
  approval silently or replaced with a generic colored block.

## Real water integration and snap points

`TerrainRiverWaterSockets.fromRoute` turns **existing RIVER spline controls**
into positional/oriented `WaterTerrainSnap.Socket` objects. Each socket
inherits its location, elevation, width and local downstream tangent from the
actual authored route. `WaterTerrainSnap.connect` invokes the installed
`WaterSnap.find` edge/height/rotation matcher, never approximate screen-space
snapping or duplicate fluid physics.

Click **Edit river snap/water...** to open the existing portable `WaterCanvas`
and `WaterEditorPanel`. All authored river socket controls display as orange
crosshairs. Drag a selected water body to a socket or use **Snap selected**;
the usual water-editor undo/redo applies. **Save** writes a real
`dev-infrastructure-demo.water` document through `WorldWater.save`,
readbacks through `WorldWater.load`, and updates the DEV room water footprints.
**Export demo water...** supports explicit standalone document export.

The component is portable: no hard-coded Beckenburg game types were inserted
into the water module, and the river-to-water conversion lives in the host
adapter. The source R16 tiles and normal gameplay level files are unaffected.

## Automated verification and evidence

`.github/workflows/dev-room-asset-review.yml`:
- **Release-relevant functional test:** genuine rail/road/poles/river geometry
  exists; original tree/rock GLB files exist; water sockets derive from the
  authored spline; snapping rejects distant or wrong-height requests; water
  undo/redo and native module export/readback/build succeed.
- **Advisory visual diagnostics:** launches the same real
  `SceneRenderer.drawDevAssetRoom` at three observation points to write
  `dev-infrastructure-actual-00.png` through `02.png` and a report,
  alongside other DEV room screenshots. These may report broken/missing art
  but cannot block a build solely for visual quality.

`DevAssetShowroom --dev-room --infrastructure-demo-capture --output=build/dev-room`
does not fabricate or AI-generate any in-game screenshot.

### Explicit limitations

This exhibit proves sample mesh construction, actual model import, real water
module data and water socket math. It does **not** prove the full Beckenburg
gameplay map's terrain collision/navmesh adapters or the full animated water
shader in the DEV scene. The DEV river shows actual `WaterDocument`
footprints using untextured diagnostic surfaces; later host water-renderer
integration should use that same native document, not a second water system.
