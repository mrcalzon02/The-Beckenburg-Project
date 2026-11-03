# St. Brigid Parish — rebuild baseline

Packet: `CHURCH`. **S00 IN_PROGRESS; no construction stage accepted.**

## Preserved source inventory

| Source | Bytes | SHA-256 |
| --- | ---: | --- |
| `development/levels/layouts/church/layout.json` | 17931 | `e5ed72ba9fa9bd002b34f75b951c876b5416666db80e219142f604159b80baca` |
| `development/levels/model-requirements/church.json` | 23595 | `715367027f88925b78c621e361eb8f4c42c4566d28f0042681ac18ab213ca621` |
| `development/levels/edge-sky-plans/church/plan.json` | 46592 | `17d8978e3223288511fd304a7412fee4b8fa362f35fd2dd45dd88779852c6f63` |

## Source planning reservations

These are existing layout reservations, not measurements of accepted runtime rooms.

| Space | Purpose in existing source | X/Z bounds (m) | Standing Y / clear height (m) |
| --- | --- | --- | --- |
| r00: Nave | Investigation and ordinary objects | [18.0, 12.0, 26.0, 34.0] | 0.0 / 6.0 |
| r01: Vestibule | Investigation and ordinary objects | [18.0, 34.0, 26.0, 39.0] | 0.0 / 3.0 |
| r02: Parish office | Investigation and ordinary objects | [10.0, 26.0, 18.0, 34.0] | 0.0 / 3.0 |
| r03: Confessional chamber | Investigation and ordinary objects | [10.0, 18.0, 18.0, 26.0] | 0.0 / 3.0 |
| r04: Chancel and altar | Investigation and ordinary objects | [18.0, 6.0, 26.0, 12.0] | 0.0 / 5.0 |
| r05: Side chapel | Investigation and ordinary objects | [26.0, 14.0, 34.0, 26.0] | 0.0 / 4.0 |
| r06: Vestry | Investigation and ordinary objects | [26.0, 6.0, 34.0, 14.0] | 0.0 / 3.0 |
| r07: Choir loft | Investigation and ordinary objects | [18.0, 26.0, 26.0, 34.0] | 3.0 / 3.0 |
| r08: Bell tower stair chamber | Investigation and ordinary objects | [10.0, 26.0, 18.0, 34.0] | 3.0 / 3.0 |
| r09: Organ archive | Investigation and ordinary objects | [26.0, 26.0, 34.0, 34.0] | 3.0 / 3.0 |
| r10: Belfry | Investigation and ordinary objects | [10.0, 26.0, 18.0, 34.0] | 6.0 / 3.0 |
| r11: Crypt | Investigation and ordinary objects | [10.0, 6.0, 34.0, 34.0] | -3.0 / 3.0 |
| grounds: Rear grounds / exterior | Investigation and ordinary objects | [4, 0, 40, 39] | 0 / 12 |
| entry: Approach / arrival | Investigation and ordinary objects | [0, 39, 40, 58.4] | 0 / 12 |
| stage: Van service apron | Investigation and ordinary objects | [14, 45, 30, 57] | 0 / 3 |

## Actual capture coverage

Kind: **RUNTIME_RENDERER_DIAGNOSTIC**. Rooms: 15; directional views: 60; missing views: 0; displaced cameras: 1.

Source-only packets need native source inspection and later actual runtime integration. A complete camera matrix proves capture coverage, not a structurally coherent place, swept access, gameplay or hardware performance.

## Next acceptance work

Inspect every room/child space, resolve generic function descriptions, compare against the site work package, record retain/refit/replace decisions and attach physical route/fixture evidence. Consult the complete fitness checklist and preserve the canonical van contract.

Absent candidate inputs (not all apply to every site):

- `development/levels/blockouts/church/implementation.json`
- `development/levels/blockouts/church/NEXT.md`
- `development/levels/blockouts/church/rough-shell.blend`
- `development/levels/blockouts/church/rough-shell.glb`
- `development/levels/blockouts/church/preview.png`
- `development/levels/power-plans/church.json`
