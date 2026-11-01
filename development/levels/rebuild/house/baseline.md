# The Briar House — rebuild baseline

Packet: `HOUSE`. **S00 IN_PROGRESS; no construction stage accepted.**

## Preserved source inventory

| Source | Bytes | SHA-256 |
| --- | ---: | --- |
| `development/levels/layouts/house/layout.json` | 19556 | `1076c2f3bc2c13f785c278bb6490172458e1461510c9a2b9f6cc928276bcfa9c` |
| `development/levels/model-requirements/house.json` | 21520 | `6de09bc70a692d9a859f48fe23df5220763617c3420d666a163a4b6edf5a0547` |
| `development/levels/edge-sky-plans/house/plan.json` | 47706 | `12027dfe42b79ceeaf6468003c6b25b8d9042a3842576a4bbe2522843529e4ab` |
| `development/levels/blender/house-complete.blend` | 19090463 | `20a56e33b9dd02a3b1f322592668e7799bfdbe654acbb448d34705a097e6c347` |

## Source planning reservations

These are existing layout reservations, not measurements of accepted runtime rooms.

| Space | Purpose in existing source | X/Z bounds (m) | Standing Y / clear height (m) |
| --- | --- | --- | --- |
| r00: Foyer | Investigation and ordinary objects | [18.0, 21.0, 24.0, 24.0] | 0.0 / 3.0 |
| r01: Living room | Investigation and ordinary objects | [12.0, 17.0, 18.0, 24.0] | 0.0 / 3.0 |
| r02: Family kitchen | Investigation and ordinary objects | [12.0, 10.0, 18.0, 17.0] | 0.0 / 3.0 |
| r03: Dining nook | Investigation and ordinary objects | [18.0, 10.0, 24.0, 15.0] | 0.0 / 3.0 |
| r04: Main hall and stair | Investigation and ordinary objects | [18.0, 15.0, 24.0, 21.0] | 0.0 / 3.0 |
| r05: Upper landing | Investigation and ordinary objects | [18.0, 15.0, 24.0, 24.0] | 3.0 / 3.0 |
| r06: Master bedroom | Investigation and ordinary objects | [12.0, 17.0, 18.0, 24.0] | 3.0 / 3.0 |
| r07: Child bedroom | Investigation and ordinary objects | [12.0, 10.0, 18.0, 17.0] | 3.0 / 3.0 |
| r08: Bathroom | Investigation and ordinary objects | [18.0, 10.0, 24.0, 15.0] | 3.0 / 3.0 |
| r09: Cellar stair landing | Investigation and ordinary objects | [18.0, 15.0, 24.0, 24.0] | -3.0 / 3.0 |
| r10: Boiler and breaker room | Investigation and ordinary objects | [18.0, 10.0, 24.0, 15.0] | -3.0 / 3.0 |
| r11: Cellar storage | Investigation and ordinary objects | [12.0, 10.0, 18.0, 24.0] | -3.0 / 3.0 |
| grounds: Rear grounds / exterior | Investigation and ordinary objects | [6, 2, 30, 24] | 0 / 12 |
| entry: Approach / arrival | Investigation and ordinary objects | [2, 24, 30, 43.4] | 0 / 12 |
| stage: Van service apron | Investigation and ordinary objects | [14, 30, 30, 42] | 0 / 3 |

## Actual capture coverage

Kind: **RUNTIME_RENDERER_DIAGNOSTIC**. Rooms: 15; directional views: 60; missing views: 0; displaced cameras: 1.

Source-only packets need native source inspection and later actual runtime integration. A complete camera matrix proves capture coverage, not a structurally coherent place, swept access, gameplay or hardware performance.

## Next acceptance work

Inspect every room/child space, resolve generic function descriptions, compare against the site work package, record retain/refit/replace decisions and attach physical route/fixture evidence. Consult the complete fitness checklist and preserve the canonical van contract.

Absent candidate inputs (not all apply to every site):

- `development/levels/blockouts/house/implementation.json`
- `development/levels/blockouts/house/NEXT.md`
- `development/levels/blockouts/house/rough-shell.blend`
- `development/levels/blockouts/house/rough-shell.glb`
- `development/levels/blockouts/house/preview.png`
- `development/levels/power-plans/house.json`
