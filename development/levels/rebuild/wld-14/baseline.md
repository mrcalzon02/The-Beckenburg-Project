# Morrow Point Lightstation — rebuild baseline

Packet: `WLD-14`. **S00 IN_PROGRESS; no construction stage accepted.**

## Preserved source inventory

| Source | Bytes | SHA-256 |
| --- | ---: | --- |
| `development/levels/layouts/wld-14/layout.json` | 37268 | `080e5ac734ad19a9027c775b2beb84f5717855eaa5461cc569106e38b7537800` |
| `development/levels/blockouts/wld-14/implementation.json` | 9063 | `d97da8b755f4a93707e247ed8b6056637912e9d623f5a463d7c1e93b0c890ea9` |
| `development/levels/blockouts/wld-14/NEXT.md` | 1496 | `d1b09c78e0c5a1689c2bd8d8716ed7e0043bc687ccb048d49ced7f9425b7d414` |
| `development/levels/blockouts/wld-14/rough-shell.blend` | 452827 | `6b8d0496c954c075010f6fe6c53264cf6880168668c432b43b72dc886284a27b` |
| `development/levels/blockouts/wld-14/rough-shell.glb` | 2937220 | `81dbb0d76ea5e0e6259c35915e9ed6e0bd6fd6930f04e7c5b4531b05e5334c00` |
| `development/levels/blockouts/wld-14/preview.png` | 691358 | `3d5b7c6af0c6fd7845c660fd67f8f661b15139a6d4f2966e35ad9d88c6d17082` |
| `development/levels/model-requirements/wld-14.json` | 16064 | `f90a7552c683bdebff50ada8d926bff8975eabc638aa0b2bc2184dd22e9e8a0a` |
| `development/levels/edge-sky-plans/wld-14/plan.json` | 69299 | `47a1093461933eb34fa45de1f2e7c748cb920b8d2b1930a581fc4a751bb439fa` |
| `development/levels/power-plans/wld-14.json` | 1537 | `6a420d92408eb12964a3bafb822a3d150b057bcd08861a42ac59e1c0430aa628` |

## Source planning reservations

These are existing layout reservations, not measurements of accepted runtime rooms.

| Space | Purpose in existing source | X/Z bounds (m) | Standing Y / clear height (m) |
| --- | --- | --- | --- |
| stage: Roadhead van apron | Roadhead van apron | [24, 78, 40, 90] | 0 / 3 |
| entry: Worn headland approach | Worn headland approach | [24, 66, 40, 78] | 0 / 3 |
| porch: Keeper porch | Keeper porch | [24, 54, 40, 66] | 0 / 3 |
| foyer: Keeper entrance | Keeper entrance | [24, 42, 32, 54] | 0 / 3 |
| sitting: Sitting room | Sitting room | [16, 42, 24, 54] | 0 / 3 |
| kitchen: Keeper kitchen | Keeper kitchen | [32, 42, 42, 54] | 0 / 3 |
| workroom: Maintenance workroom | Maintenance workroom | [16, 30, 28, 42] | 0 / 3 |
| store: Dry store | Dry store | [28, 30, 42, 42] | 0 / 3 |
| stairs: Main stair hall | Main stair hall | [42, 30, 48, 54] | 0 / 3 |
| bed: Keeper bedroom | Keeper bedroom | [16, 42, 28, 54] | 3.2 / 3 |
| study: Signal records study | Signal records study | [28, 42, 42, 54] | 3.2 / 3 |
| upperhall: Upper stair landing | Upper stair landing | [42, 30, 48, 54] | 3.2 / 3 |
| service-stair: Service stair landing | Service stair landing | [12, 30, 16, 54] | 0 / 3 |
| service-upper: Upper service landing | Upper service landing | [12, 30, 16, 54] | 3.2 / 3 |
| passage: Attached covered tower passage | Attached covered tower passage | [28, 14.5, 36, 30] | 0 / 3 |
| generator: Generator and oil house | Generator and oil house | [48, 34, 58, 46] | 0 / 3 |
| yard: Service yard and cistern reserve | Service yard and cistern reserve | [48, 46, 62, 60] | 0 / 3 |
| tower0: Tower base | Tower base | [27.5, 5.5, 36.5, 14.5] | 0.0 / 3.6 |
| tower1: First tower landing | First tower landing | [27.5, 5.5, 36.5, 14.5] | 3.6 / 3.6 |
| tower2: Second tower landing | Second tower landing | [27.5, 5.5, 36.5, 14.5] | 7.2 / 3.6 |
| tower3: Third tower landing | Third tower landing | [27.5, 5.5, 36.5, 14.5] | 10.8 / 3.6 |
| tower4: Watch room | Watch room | [27.5, 5.5, 36.5, 14.5] | 14.4 / 3.6 |
| tower5: Lantern room | Lantern room | [27.5, 5.5, 36.5, 14.5] | 18.0 / 3.6 |

## Actual capture coverage

Kind: **NO_RUNTIME_CAPTURE**. Rooms: 0; directional views: 0; missing views: 0; displaced cameras: 0.

Source-only packets need native source inspection and later actual runtime integration. A complete camera matrix proves capture coverage, not a structurally coherent place, swept access, gameplay or hardware performance.

## Next acceptance work

Inspect every room/child space, resolve generic function descriptions, compare against the site work package, record retain/refit/replace decisions and attach physical route/fixture evidence. Consult the complete fitness checklist and preserve the canonical van contract.
