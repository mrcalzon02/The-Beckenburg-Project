# Beckenburg County — accepted campaign map and level selection surface

**Decision (2026-11-10): accepted.** The top-down, unlabeled Beckenburg County map with a western sea coast, estuary, northern forests/lakes, central Castle Beckenburg, eastern mountains and a continuous, plausible regional road network is the **campaign world-map base layer** and the **level-selection map**. It supersedes the earlier scenic-location collage/illustrated concept. The master artwork itself has **no baked-in names, dots, legends or mission descriptions**.

## Authoritative asset slots

- `assets/concept-art/campaign/beckenburg-county-approved.jpg`: approved cartographic illustration (1448×1086, high-quality optimized source reproduction).
- `assets/runtime/campaign/beckenburg-county.jpg`: same image, packaged for Java desktop campaign UI.
- `assets/runtime/campaign/locations.tsv`: normalized map locations (`site-id`, `x`, `y`); coordinates are *illustrative positioning* editable without altering the art or changing chapter IDs.
- The in-chat approved full-size lossless PNG is preserved separately with SHA-256 `2d8cd629e06a934d1b2041cfbdc53e9db94f3b8b9350a8188de1630de486233c`. This Git checkpoint carries a pixel-faithful, quality-85 JPEG derivative for smaller builds; do not mistake the earlier `beckenburg-county-base.png` for this accepted image.

## Campaign and level-select integration

`CountyCampaignMapPanel` renders the approved artwork as a static base, draws map nodes dynamically and maps them to the **existing authoritative** `MissionSites` catalog. Node overlays are UI elements, not part of the base image. Titles, region, lore/description, location status and saved progression gates stay in existing `MissionSites`, `CampaignStory`, `ProfileStore` and `CampaignBoardPanel`; do **not** duplicate the campaign roster or reorder persisted chapter ordinals.

- Open **COUNTY MAP** in the campaign lobby. Wheel or `+`/`−` zooms about the pointer/viewport; drag pans; Home/FULL MAP resets; FOCUS SITE centers the current location; arrow keys pan when the canvas has focus.
- Click a node to select it and display the location's name, region, status and description. The contract list, site-picture panel and deployment button synchronize with that selection. The central castle is depicted as **headquarters**, not a field mission, and cannot bypass campaign gates.
- Nodes reflect chapter lock and real map availability; planned wilderness levels remain non-dispatchable even when visible. No visual interaction ever authorizes a level that `SiteCatalog` and the saved profile forbid.
- The approved image must remain label-free as the visual base. Optional dynamic labels, mission status, routes, progress overlays and hover cards belong in separately togglable layers, never baked into a new painting.

## Node editing / future expansion

The initial points in `locations.tsv` are proposed cartographic positions only; they are not surveyed gameplay coordinates or evidence of finished environment geometry. Designers may adjust the normalized positions after art review, add regional boundaries, discovery paths, unlocked-site filters and connecting routes, and add new catalog sites only with their own approval. Preserve the base map artwork, old campaign save indices and a full zoomable/pannable view. In-game image resource `/org/beckenburg/campaign/beckenburg-county.jpg` and registry `/org/beckenburg/campaign/locations.tsv` must both be copied into Windows and Unix packages.

**Verification:** isolate Java compile and image/node registry tests are expected; full native Windows and installed graphical QA plus real mission launch remain separate acceptance. An image, node, label or passing compile is not evidence that unbuilt levels are playable.
