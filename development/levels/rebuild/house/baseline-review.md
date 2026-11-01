# Briar House — inspected baseline and rebuild decisions

31 October 2026. Baseline geometry: online main `fba22b2a3eb56871eeda009f7cd0ab8c4ad4aa5c`; authored scene identity `production-2026-10-20-lived-location-v1-31a060513814dc2d`. Inspected the approved House concept board and all fifteen `four-way.png` panels in `../captures/baseline/house/` (60 actual renderer directions). This is a baseline review, not final art/physical acceptance.

## Findings and component decisions

| Component | Observation | Rebuild decision |
| --- | --- | --- |
| Ground-floor organization | Entry, living, kitchen, dining and stair hall form an understandable circulation sequence. | Retain the functional adjacency, redraw measured room proportions from use. |
| Vertical circulation | Two physically separate flights and matching guards/voids exist. Standing ascent/descent checks pass; the basement, ground and upper elevations are reachable. | Retain the construction method and assets, re-engineer coordinates if room proportions change. |
| Bedroom privacy | Both bedrooms open onto the landing, but the source also connects them directly. | Review removal of the inter-bedroom shortcut in the rough plan; a private bedroom need not create a pursuit loop through another bedroom. |
| Room scale | Current bathroom is 30 m², upper landing 54 m², and each bedroom 42 m². The concept calls for close quarters and a small bathroom. | Rebalance toward actual household use; reserve dimensions for the measured rough-plan pass rather than treating these inherited rectangles as requirements. |
| Furniture collection | Existing beds, seating, fitted kitchen/bath/laundry pieces, source cabinets and lamps are valuable and recognizable. | Preserve clean/worn assets and native pivots/UVs; replace their layout and selection room by room. |
| Domestic composition | Repeated shelves/tables and similar small-item groupings appear across living, bedrooms and cellar. | Consolidate storage and create purposeful family, cooking, sleeping and maintenance clusters during prop passes. |
| Cellar | Several large rooms contain similar storage furniture. Their service relationships need a stronger normal-use account. | Separate plant/laundry, seasonal storage and stair/service circulation in the function program. |
| Exterior | Flat fenced ground and a street arrival exist; the baseline shows a supported van and surrounding street scenery. | Preserve the canonical van. Rebuild yard drainage, path/patio footprint, foundations, planting and skyline under the terrain/boundary gates. |
| Envelope and light | The inspected directions show working openings, trim and distinct surfaces. | Retain useful source kits. Matched circuit, door-closed, torch/UV and corner-section reviews are still required; these views cannot establish complete light separation. |

The four cardinal bathroom views are not a complete sanitary-fit inspection: `LevelPlans` explicitly places a toilet, so its visibility cannot be inferred from an individual screenshot. Inspect the native mesh, scale, support and practical approach before changing or duplicating it.

## Actual local checks

- JDK 17 `build.cmd package`: passed, including packaged audio audit.
- `AuthoredLevelPhysicsTest`: House, Farmhouse and School XYZ geometry passed.
- `AuthoredLevelReachabilityTest HOUSE`: 65,437 physically swept positions; all 15 zones and required sockets reachable; standing elevations −3 to +3 m.
- `AuthoredUtilitiesTest`: 36 reachable switches, 52 acoustic door links and seven stair links across its defined reference scope passed.
- `LevelSurfaceIntegrityTest`: House/Farmhouse/School wall separation, deck apertures, materials and guards passed.

Raw results are retained under `rebuild/verification/`. These checks establish the baseline within their scope. Closed-door behavior, all decorative-object supports, revised architecture, real human/AI hunts, complete rituals, LAN and hardware performance remain open.

## S01 design brief

An older suburban family dwelling with a sheltered entrance, ordinary shared rooms downstairs, private sleeping/washing upstairs and cellar services. Use the existing lore, including its documented plumbing repairs; do not invent a new historical identity or evidence outcome. The lived-in arrangement should explain eating, cooking, rest, storage, laundry and garden use before adding abandonment.

Public arrival proceeds curb/van -> gate/path -> covered threshold -> foyer. Visitors reach shared living/dining without crossing private bedrooms or the boiler room. Cooking connects dining and a rear service/garden door. Upper bedrooms have independent landing access and a nearby private bathroom. Cellar access supports maintenance and household storage. Garden/patio use and street-facing planting follow actual terrain and maintenance history.

The measured redesign must provide fitted kitchen work surfaces and real appliance/sink positions; a full bathroom with toilet/basin/bath; plausible hot-water/heating distribution; storage that fits possessions; and visible evidence of actual repairs. Reuse available assets by fit. No extra corridor or bedroom is added to satisfy a zone count. Three ritual reservations, shared hiding settings, evidence and extraction consume the same authored space without replacing its normal function.

**Next:** complete the roster's S01 function/reference decisions; then redraw House's measured plan and sections as the construction pilot, retaining this before-state for comparison. S00/S01 remain in progress until their full required records are complete.
