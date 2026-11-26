# Terrain-bearing construction batch

25editable native candidates retain the prior terrain/structure/model sources and add actual ground-to-floor construction. Four mature maps remain explicitly retained for individual audits.

| Site | New closed parts | New triangles | New flat pose/sweep failures |
|---|---:|---:|---|
| ACADEMY | 147 | 1764 | 0 / 0 |
| ASYLUM | 180 | 2160 | 0 / 0 |
| CASTLE | Retained | Individual audit | Not run / Not run |
| CHURCH | 45 | 540 | 0 / 0 |
| COLLEGE | 201 | 2412 | 0 / 0 |
| FARMHOUSE | 85 | 1020 | 0 / 0 |
| HOUSE | Retained | Individual audit | Not run / Not run |
| MANSION | 111 | 1332 | 0 / 0 |
| PRISON | 216 | 2592 | 0 / 0 |
| SCHOOL | 82 | 984 | 0 / 0 |
| WLD-01 | Retained | Individual audit | Not run / Not run |
| WLD-02 | Retained | Individual audit | Not run / Not run |
| WLD-03 | 40 | 480 | 0 / 0 |
| WLD-04 | 28 | 336 | 0 / 0 |
| WLD-05 | 36 | 432 | 0 / 0 |
| WLD-06 | 99 | 1188 | 0 / 0 |
| WLD-07 | 2 | 24 | 0 / 0 |
| WLD-08 | 75 | 900 | 0 / 0 |
| WLD-09 | 12 | 144 | 0 / 0 |
| WLD-10 | 134 | 1608 | 0 / 0 |
| WLD-11 | 105 | 1260 | 0 / 0 |
| WLD-12 | 43 | 516 | 0 / 0 |
| WLD-13 | 83 | 996 | 0 / 0 |
| WLD-14 | 27 | 324 | 0 / 0 |
| WLD-15 | 2 | 24 | 0 / 0 |
| WLD-16 | 7 | 84 | 0 / 0 |
| WLD-17 | 20 | 240 | 0 / 0 |
| WLD-18 | 18 | 216 | 0 / 0 |
| WLD-07-MORTUARY | 13 | 156 | 0 / 0 |

## Scope and verification

- `*/supported-terrain.blend`, `.glb`, `.hcs2`: actual editable/exported construction; original collision records preserved before new parts.
- `*/construction.json`: exact source/hash lineage, every embedded support and owning upper receiver, withheld contacts and current captures.
- `verification/bearing-contact.json`: independent ground/underside rays, owning mesh contact, embed depth, explicit beam attachment, closed outward solids and render/native parity. Rejects17floating reservation-post negatives.
- `verification/export-preservation.json`: every retained exported mesh/UV/image binding/pose within declared quantization.
- `verification/native-route-fit.json`:110409flat samples,98449baseline-valid poses/93321qualified sweeps, zero new blocks.
- `verification/stairs/`:221vertical segments,84complete links,7automatic multi-link chains and2declared cemetery loops in both before/after variants; no new passing-direction regressions. Both variants185/221flights and47/84complete links pass;36flights/37links remain failing.
- `existing-terrain-review.json`: individual current source/geometry/view decisions for all7older terrain candidates. These have not passed the desquareify gate.

The prior terrain batch exposes mesa WLD-03/L13/segment0 in addition to prior stair failures: the older furnished source passed, but both pre/post bearing terrain composites fail. Record it as terrain fit, not a bearing regression or accepted movement.72actual failed directional poses have contact diagnosis. A missing phase now rejects instead of producing an empty success-looking diagnosis.

Next: construct real exterior decks where source reservations lack meshes; resolve overall terrain/building/stair relationships, including the7existing terrain sources; add correct low-route headers, cliff/retaining/foundation rims, pier and cave structures, water/bank and physical boundaries. Canonical van arrival is still absent. Existing orange/cyan route strips are construction inputs, not terrain constraints or finished path surfaces. Finish surface/textures, architecture and full runtime/network/hardware gates later.

No current source master, reusable prop pivot/UV or saved editor punch was overwritten. No editor UI or timed round test. Structural massing, terrain fit, visual acceptance and actual runtime acceptance remain separate.
