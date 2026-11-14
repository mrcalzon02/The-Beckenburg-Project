# Beckenburg County — physical Castle campaign board

**Implementation slice:** the county campaign map is now a **fixed, tangible, textured GLB board** in the ground-floor **briefing/contract room**, at the *existing* `CastleHubMap.Station.CONTRACT` location. This is not a camera-facing NPC billboard and does not replace the approved cartographic base image. The Castle workstation's **E** interaction displays the existing interactive `CountyCampaignMapPanel` as the **first page** of `CampaignBoardPanel`; clicking sites, reading the dossier, zoom/pan, and deploying a genuinely ready chapter use the same profile and runtime checks as the original contract menu.

## Geometry and authority

- Runtime mesh: `assets/runtime/models/level_county_campaign_board.glb`. Independently indexed editable model in `LevelAssets.AUTHORED_SURFACES`, shipped through the existing `level_*.glb` copy in Windows and Linux builds. Source generator: `development/assets/build_county_campaign_board.py`. Model is native-scale, approximately **2.53 m × 2.56 m × 0.61 m**, **294 triangles**. It has supported feet, a framed panel, visible edge thickness, iron fittings and the approved *unlabeled* map image as an embedded PNG diffuse material. The texture is resampled to 768 × 576 to respect the project's strict 4 MB GLB resource budget and PSX display constraints; the higher-resolution approved campaign UI JPEG is unaffected.
- World placement: `CastleServiceFixtures` uses the authoritative, editor-movable `CastleLevelEdits.station(CONTRACT)` coordinates to draw *and* collide the board. The preexisting contract desk and open book are replaced, avoiding two competing fixtures. The board is world-oriented facing the visitor who approaches from the south, not adjusted toward the camera on each frame.
- Interaction: `CastleHubMap.aimed` and `CastleHubPanel.use(CONTRACT)` remain the physically constrained E-action path with real Castle sight-line checks; `InvestigationGame.showCampaign` calls `CampaignBoardPanel.showCountyMap` before opening the original Castle-contained service overlay. There is no second campaign registry, new launch authority, or automatic level dispatch.
- Editor: the GLB appears in the actual approved asset palette; the station itself can be relocated via the existing Castle station editor. The fixture follows its station position in prior user-authored Castle documents without adding new save fields or overwriting saved scenery.

## Reproduction

```
python development/assets/build_county_campaign_board.py assets/runtime/campaign/beckenburg-county.jpg assets/runtime/models/level_county_campaign_board.glb
```

This is an **offline asset-authoring step**, not a game startup dependency. It uses Pillow. The generated GLB contains only embedded image data, native indexed geometry, and glTF2 standard materials, so the shipped Java 17 renderer loads it without Python or Blender.

## Verification

- `CastleCountyBoardTest` checks that the game JAR contains the GLB; geometry and embedded approved map texture decode through the real `GlbProps` reader; the edited Castle station and fixture agree on coordinates; collision blocks traversing the board; a reachable E-facing approach selects the contract action; looking away does not; `SceneRenderer.drawCastle` renders; and the actual campaign map opens with chapter locks and progress guards preserved.
- Prior acceptance remains `CountyCampaignMapPanelTest` and `CountyCampaignMapNavigationTest`. No existing chapter order or node positions change.
- The source-model preview in `development/levels/previews/menu-workflow/` is a *true GLB geometry/material render* for modeling proof, **not** a claimed installed Windows game screenshot. Run the test with `-Dbeckenburg.county.board.capture=build/county-castle-panel.png` for an actual software Castle view, then perform installed Windows visual/interaction review before calling the in-world feature fully hardware-accepted.
