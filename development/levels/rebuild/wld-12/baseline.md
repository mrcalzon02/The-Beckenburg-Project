# Gallows Pass Railway — rebuild baseline

Packet: `WLD-12`. **S00 IN_PROGRESS; no construction stage accepted.**

## Preserved source inventory

| Source | Bytes | SHA-256 |
| --- | ---: | --- |
| `development/levels/layouts/wld-12/layout.json` | 17166 | `a873a8669fdec20b35cdcfdb0c8b97e39b9ef18b26aa545423b61961918b155d` |
| `development/levels/blockouts/wld-12/implementation.json` | 4658 | `7c985b31192bdc302365258bad0d6ab92a342e777f81ce9b14f508a76569e5dc` |
| `development/levels/blockouts/wld-12/NEXT.md` | 1096 | `a74e622342915c51f4a709e0e3e60cefca9d24894f3943e89d5cef0d391ace8e` |
| `development/levels/blockouts/wld-12/rough-shell.blend` | 142268 | `4a91504c0dcfaccd3e515599182494e7918c5c0c6b64f26affbd9b83156bc773` |
| `development/levels/blockouts/wld-12/rough-shell.glb` | 1342336 | `b1d0d8929cdc63cc5561a5de773fb17e7a5d9aadd4cd16003860837f8aa51f98` |
| `development/levels/blockouts/wld-12/preview.png` | 607808 | `582b97d4a5dd3afa9d29abca5b2a5160431ff601deecc884d53981995972b7e7` |
| `development/levels/model-requirements/wld-12.json` | 20854 | `92c1f0aa3bb0cc41ecc157e50f6c5b7a739752406ffd71bd29038fe79dfb2951` |
| `development/levels/edge-sky-plans/wld-12/plan.json` | 57554 | `7619e54d583766b0645b3338770629d89952c5b8ed03d7bdcec02e37aa53c34b` |
| `development/levels/power-plans/wld-12.json` | 1535 | `9b62dd935cb497a172f3ed842b46486d31f48d48e818f3de2b7b5b9d8f42b5b5` |

## Source planning reservations

These are existing layout reservations, not measurements of accepted runtime rooms.

| Space | Purpose in existing source | X/Z bounds (m) | Standing Y / clear height (m) |
| --- | --- | --- | --- |
| stage: Maintenance-road van apron | Investigation and ordinary objects | [28, 88, 44, 100] | 0 / 3 |
| entry: Rail access gate | Investigation and ordinary objects | [28, 76, 44, 88] | 0 / 3 |
| signal: Signal hut | Investigation and ordinary objects | [12, 62, 28, 76] | 0 / 3 |
| cutting: Rail cutting | Investigation and ordinary objects | [28, 42, 40, 76] | 0 / 3 |
| coach-a: Derailed passenger coach | Investigation and ordinary objects | [16, 46, 24, 62] | 0 / 3 |
| coach-b: Derailed baggage coach | Investigation and ordinary objects | [44, 46, 52, 62] | 0 / 3 |
| siding: Maintenance siding | Investigation and ordinary objects | [52, 30, 62, 76] | 0 / 3 |
| store: Track stores hut | Investigation and ordinary objects | [52, 16, 64, 30] | 0 / 3 |
| trestle: Trestle guarded deck | Investigation and ordinary objects | [28, 22, 40, 42] | 0 / 3 |
| tunnel: Tunnel mouth / dry bore | Investigation and ordinary objects | [28, -8, 40, 22] | 0 / 3 |
| refuge: Tunnel maintenance refuge | Investigation and ordinary objects | [40, 0, 52, 12] | 0 / 3 |
| lower: Lower service walkway | Investigation and ordinary objects | [28, 22, 34, 42] | -3 / 3 |
| west-return: Cutting west return path | Investigation and ordinary objects | [6, 22, 16, 62] | 0 / 3 |
| crossing: North maintenance crossing | Investigation and ordinary objects | [40, 12, 62, 22] | 0 / 3 |

## Actual capture coverage

Kind: **NO_RUNTIME_CAPTURE**. Rooms: 0; directional views: 0; missing views: 0; displaced cameras: 0.

Source-only packets need native source inspection and later actual runtime integration. A complete camera matrix proves capture coverage, not a structurally coherent place, swept access, gameplay or hardware performance.

## Next acceptance work

Inspect every room/child space, resolve generic function descriptions, compare against the site work package, record retain/refit/replace decisions and attach physical route/fixture evidence. Consult the complete fitness checklist and preserve the canonical van contract.
