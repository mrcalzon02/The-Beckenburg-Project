# MV Saint Elara — rebuild baseline

Packet: `WLD-15`. **S00 IN_PROGRESS; no construction stage accepted.**

## Preserved source inventory

| Source | Bytes | SHA-256 |
| --- | ---: | --- |
| `development/levels/layouts/wld-15/layout.json` | 17569 | `7e73e778acdf196caf28e4be5a9bf264035eb9ffa6f8bdaff413aadf280c10c3` |
| `development/levels/blockouts/wld-15/implementation.json` | 6092 | `325e61bee60fe3f56c4ba693b7101c0172a6c2b80137d566c1e83972b3b00088` |
| `development/levels/blockouts/wld-15/NEXT.md` | 1449 | `e2ef3e19f0ea78cd21ebe3812c49cf5df007385896a47b2d08628a03ca305703` |
| `development/levels/blockouts/wld-15/rough-shell.blend` | 310113 | `87b775383b64882aff41699b1638649951f69f4e75f97909b31e2815a6266f24` |
| `development/levels/blockouts/wld-15/rough-shell.glb` | 1314904 | `b5eb478c1f1e876f453452ca978c361520df47f588568e8bd931c3849da792d5` |
| `development/levels/blockouts/wld-15/preview.png` | 622051 | `4e673faf43c0d235aca70d1325d42dec8014ccac49efaac2a2f067456c6328f4` |
| `development/levels/model-requirements/wld-15.json` | 12154 | `83c815b4a7b6fcdf78f72510f4620bc51097526a92cec1b15539cd9195b42db3` |
| `development/levels/edge-sky-plans/wld-15/plan.json` | 53557 | `c7d9d909ea4fb262f6ecc60fa7a24a49e6464ea59113f20608a4786422f2b50f` |
| `development/levels/power-plans/wld-15.json` | 1557 | `25eb0d9cf05995f3c66f8f516a8c13fdacc766988dd77f00d6daaef465026893` |

## Source planning reservations

These are existing layout reservations, not measurements of accepted runtime rooms.

| Space | Purpose in existing source | X/Z bounds (m) | Standing Y / clear height (m) |
| --- | --- | --- | --- |
| stage: Quayside van apron | Quayside van apron | [24, 80, 40, 92] | 0 / 3 |
| entry: Quay cargo lane | Quay cargo lane | [24, 76, 40, 80] | 0 / 3 |
| quay: Mooring quay | Mooring quay | [24, 64, 40, 76] | 0 / 3 |
| portpier: Port service pier | Port service pier | [16, 40, 22, 72] | 0 / 3 |
| aft: Aft working deck | Aft working deck | [22, 40, 42, 54] | 0 / 3 |
| processing: Fish processing hall | Fish processing hall | [24, 8, 40, 30] | 0 / 3 |
| galley: Galley | Galley | [24, 30, 32, 40] | 0 / 3 |
| mess: Crew mess | Crew mess | [32, 30, 40, 40] | 0 / 3 |
| side: Starboard deck walkway | Starboard deck walkway | [40, 8, 42, 40] | 0 / 3 |
| fore: Foredeck | Foredeck | [24, -2, 40, 8] | 0 / 3 |
| engine: Engine room | Engine room | [24, 24, 40, 40] | -3 / 3 |
| hold: Chilled fish hold | Chilled fish hold | [24, 4, 40, 24] | -3 / 3 |
| pump: Bilge pump room | Bilge pump room | [24, -2, 40, 4] | -3 / 3 |
| crew: Crew bunks | Crew bunks | [24, 30, 40, 40] | 3 / 3 |
| bridge: Wheelhouse | Wheelhouse | [24, 18, 40, 30] | 6 / 3 |
| net: Net and gear store | Net and gear store | [24, 8, 40, 18] | 3 / 3 |

## Actual capture coverage

Kind: **NO_RUNTIME_CAPTURE**. Rooms: 0; directional views: 0; missing views: 0; displaced cameras: 0.

Source-only packets need native source inspection and later actual runtime integration. A complete camera matrix proves capture coverage, not a structurally coherent place, swept access, gameplay or hardware performance.

## Next acceptance work

Inspect every room/child space, resolve generic function descriptions, compare against the site work package, record retain/refit/replace decisions and attach physical route/fixture evidence. Consult the complete fitness checklist and preserve the canonical van contract.
