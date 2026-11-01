# Hollowmere Farm — rebuild baseline

Packet: `FARMHOUSE`. **S00 IN_PROGRESS; no construction stage accepted.**

## Preserved source inventory

| Source | Bytes | SHA-256 |
| --- | ---: | --- |
| `development/levels/layouts/farmhouse/layout.json` | 17767 | `e050c3627240ac10ba6f44191e549ac631bb9a43c7701cb2daf8291f12c00895` |
| `development/levels/model-requirements/farmhouse.json` | 21937 | `c40df2674d59486abb220485808c4d45957992fdec56553f42d79a9f74344e1a` |
| `development/levels/edge-sky-plans/farmhouse/plan.json` | 46507 | `df4af341d94ac31c0529a13ea922483ce762ef48d5351e9855974111d6de6898` |
| `development/levels/blender/farmhouse-complete.blend` | 16566617 | `5c548893eb80f7fbd2390b9c92715adc72cbfa3fcd488e51b5d2c1b3bb3ba74a` |

## Source planning reservations

These are existing layout reservations, not measurements of accepted runtime rooms.

| Space | Purpose in existing source | X/Z bounds (m) | Standing Y / clear height (m) |
| --- | --- | --- | --- |
| r00: Mudroom | Investigation and ordinary objects | [20.0, 22.0, 26.0, 27.0] | 0.0 / 3.0 |
| r01: Farm parlour | Investigation and ordinary objects | [12.0, 19.0, 20.0, 27.0] | 0.0 / 3.0 |
| r02: Farm kitchen | Investigation and ordinary objects | [12.0, 12.0, 20.0, 19.0] | 0.0 / 3.0 |
| r03: Family dining room | Investigation and ordinary objects | [20.0, 12.0, 26.0, 15.0] | 0.0 / 3.0 |
| r04: Central stair passage | Investigation and ordinary objects | [20.0, 15.0, 26.0, 22.0] | 0.0 / 3.0 |
| r05: Upper farm landing | Investigation and ordinary objects | [20.0, 12.0, 26.0, 27.0] | 3.0 / 3.0 |
| r06: Parents bedroom | Investigation and ordinary objects | [12.0, 19.0, 20.0, 27.0] | 3.0 / 3.0 |
| r07: Child bedroom | Investigation and ordinary objects | [12.0, 12.0, 20.0, 19.0] | 3.0 / 3.0 |
| r08: Barn hayloft | Investigation and ordinary objects | [31.0, 16.0, 43.0, 24.0] | 3.0 / 3.0 |
| r09: Root cellar | Investigation and ordinary objects | [12.0, 12.0, 26.0, 27.0] | -3.0 / 3.0 |
| r10: Barn interior | Investigation and ordinary objects | [31.0, 9.0, 43.0, 24.0] | 0.0 / 6.0 |
| r11: Tool shed | Investigation and ordinary objects | [31.0, 24.0, 39.0, 31.0] | 0.0 / 3.0 |
| grounds: Rear grounds / exterior | Investigation and ordinary objects | [5, 2, 48, 31] | 0 / 12 |
| entry: Approach / arrival | Investigation and ordinary objects | [1, 31, 48, 50.4] | 0 / 12 |
| stage: Van service apron | Investigation and ordinary objects | [18.5, 37, 34.5, 49] | 0 / 3 |

## Actual capture coverage

Kind: **RUNTIME_RENDERER_DIAGNOSTIC**. Rooms: 15; directional views: 60; missing views: 0; displaced cameras: 0.

Source-only packets need native source inspection and later actual runtime integration. A complete camera matrix proves capture coverage, not a structurally coherent place, swept access, gameplay or hardware performance.

## Next acceptance work

Inspect every room/child space, resolve generic function descriptions, compare against the site work package, record retain/refit/replace decisions and attach physical route/fixture evidence. Consult the complete fitness checklist and preserve the canonical van contract.

Absent candidate inputs (not all apply to every site):

- `development/levels/blockouts/farmhouse/implementation.json`
- `development/levels/blockouts/farmhouse/NEXT.md`
- `development/levels/blockouts/farmhouse/rough-shell.blend`
- `development/levels/blockouts/farmhouse/rough-shell.glb`
- `development/levels/blockouts/farmhouse/preview.png`
- `development/levels/power-plans/farmhouse.json`
