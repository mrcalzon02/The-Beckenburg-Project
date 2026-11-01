# Beckenburg College — rebuild baseline

Packet: `COLLEGE`. **S00 IN_PROGRESS; no construction stage accepted.**

## Preserved source inventory

| Source | Bytes | SHA-256 |
| --- | ---: | --- |
| `development/levels/layouts/college/layout.json` | 19997 | `e424444d53ba348e83d53af4a3066722d9c1003b3e27abc638b25d7f900307e4` |
| `development/levels/blockouts/college/implementation.json` | 8785 | `9fb8ba28ff4ce8ce0cf6438469c9dca2a72207a07e49f41d8b5f8e11f94b4e28` |
| `development/levels/blockouts/college/NEXT.md` | 1041 | `ae9dfa4beee4a22b6450c274c15e235ba8cfac9fde25da0be4c4de1913e2bd67` |
| `development/levels/blockouts/college/rough-shell.blend` | 184592 | `ad5328a656b07d9c5cdd00cd118aba54ac7f75a70a1f606c38d1b0358a6ad1e0` |
| `development/levels/blockouts/college/rough-shell.glb` | 1536112 | `4da60e9e6bae49a0950c4737686e63404121524862cf28ce0c93e271b8fc9465` |
| `development/levels/blockouts/college/preview.png` | 632966 | `e2d866b81d1cabcbc5197a8b5004673a77c57cb5cff8b4571d6cee9e9424a4c7` |
| `development/levels/model-requirements/college.json` | 25798 | `7bca014a7353006ff291d3323005af2c37a897fb86c21a2cd40836a19d67dad3` |
| `development/levels/edge-sky-plans/college/plan.json` | 54847 | `afe8368cd64899102f0dbe0c8955dc2b28730c3fdbba8e3919d34b2d962d4cc2` |

## Source planning reservations

These are existing layout reservations, not measurements of accepted runtime rooms.

| Space | Purpose in existing source | X/Z bounds (m) | Standing Y / clear height (m) |
| --- | --- | --- | --- |
| stage: Campus service van apron | Investigation and ordinary objects | [28, 76, 44, 88] | 0 / 3 |
| entry: Campus green approach | Investigation and ordinary objects | [28, 64, 44, 76] | 0 / 3 |
| plaza: Main facade plaza | Investigation and ordinary objects | [18, 50, 54, 64] | 0 / 3 |
| lobby: Teaching lobby | Investigation and ordinary objects | [4, 38, 18, 50] | 0 / 3 |
| lecture: Lecture theatre | Investigation and ordinary objects | [4, 18, 18, 38] | 0 / 6 |
| seminar: Seminar classroom | Investigation and ordinary objects | [18, 38, 32, 50] | 0 / 3 |
| lab: Laboratory | Investigation and ordinary objects | [18, 18, 32, 38] | 0 / 3 |
| library: Library reading room | Investigation and ordinary objects | [18, 0, 46, 14] | 0 / 3 |
| stacks: Library stacks | Investigation and ordinary objects | [18, 0, 46, 14] | 3.6 / 3 |
| bridge: Upper campus bridge | Investigation and ordinary objects | [32, 14, 46, 22] | 3.6 / 3 |
| dorm: Dormitory corridor / rooms | Investigation and ordinary objects | [46, 20, 62, 38] | 3.6 / 3 |
| res-lobby: Residence stair lobby | Investigation and ordinary objects | [46, 20, 62, 38] | 0 / 3 |
| union: Union cafe | Investigation and ordinary objects | [46, 38, 62, 50] | 0 / 3 |
| admin: Administration office | Investigation and ordinary objects | [62, 38, 74, 50] | 0 / 3 |
| rear: Rear quad and parking | Investigation and ordinary objects | [62, 14, 74, 38] | 0 / 3 |
| tunnel: Maintenance tunnel hub | Investigation and ordinary objects | [22, 20, 34, 38] | -3 / 3 |
| tunnel-west: West service tunnel | Investigation and ordinary objects | [4, 38, 22, 44] | -3 / 3 |

## Actual capture coverage

Kind: **RUNTIME_RENDERER_DIAGNOSTIC**. Rooms: 15; directional views: 60; missing views: 0; displaced cameras: 1.

Source-only packets need native source inspection and later actual runtime integration. A complete camera matrix proves capture coverage, not a structurally coherent place, swept access, gameplay or hardware performance.

## Next acceptance work

Inspect every room/child space, resolve generic function descriptions, compare against the site work package, record retain/refit/replace decisions and attach physical route/fixture evidence. Consult the complete fitness checklist and preserve the canonical van contract.

Absent candidate inputs (not all apply to every site):

- `development/levels/power-plans/college.json`
