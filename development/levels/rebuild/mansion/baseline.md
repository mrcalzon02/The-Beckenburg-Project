# Blackthorn Manor — rebuild baseline

Packet: `MANSION`. **S00 IN_PROGRESS; no construction stage accepted.**

## Preserved source inventory

| Source | Bytes | SHA-256 |
| --- | ---: | --- |
| `development/levels/layouts/mansion/layout.json` | 19830 | `a909daacc0e24fca86dbfd01027ec476b34ca79d4b2c9b8a647b9e4ea4e015d6` |
| `development/levels/blockouts/mansion/implementation.json` | 9555 | `1c5fd4b8640d3bfb30e74e7e1537f83b74baceef06b0eefcac0d77ff9c70a074` |
| `development/levels/blockouts/mansion/NEXT.md` | 1052 | `070b1d364b527101c18ff89af75bd6aa9315d93854417bb739083c3355a86c13` |
| `development/levels/blockouts/mansion/rough-shell.blend` | 181396 | `23ebd55b9bc028324547a0b15aea27d4ea3d47f03d8063663bb8be398936c83a` |
| `development/levels/blockouts/mansion/rough-shell.glb` | 1381828 | `0ce64ffb798021c4ee8891b15e3a318a497ac0017685b236c7b9e3c565ef2322` |
| `development/levels/blockouts/mansion/preview.png` | 634326 | `f546695e916e4a79ebf3f53f0b3a1ac36ef8cfc011020cd5c0476d6ce463736d` |
| `development/levels/model-requirements/mansion.json` | 25276 | `96dfa132d12afd6b36cefcdb5998c89fa263e364836d22fa7473d3d2baec03e4` |
| `development/levels/edge-sky-plans/mansion/plan.json` | 50740 | `255ab7e2fca3d87ba0e122adcb83d57779684aec92c777abd047392f86055756` |

## Source planning reservations

These are existing layout reservations, not measurements of accepted runtime rooms.

| Space | Purpose in existing source | X/Z bounds (m) | Standing Y / clear height (m) |
| --- | --- | --- | --- |
| stage: Carriage-drive van apron | Investigation and ordinary objects | [20, 65, 36, 77] | 0 / 3 |
| entry: Entrance terrace | Investigation and ordinary objects | [20, 55, 36, 65] | 0 / 3 |
| foyer: Entrance foyer | Investigation and ordinary objects | [20, 45, 36, 55] | 0 / 3 |
| stair: Grand stair hall | Investigation and ordinary objects | [20, 27, 28, 45] | 0 / 7.2 |
| drawing: Drawing room | Investigation and ordinary objects | [4, 45, 20, 55] | 0 / 3 |
| library: Library | Investigation and ordinary objects | [4, 27, 20, 45] | 0 / 3 |
| ballroom: Ballroom | Investigation and ordinary objects | [36, 27, 54, 55] | 0 / 7.2 |
| dining: Dining hall | Investigation and ordinary objects | [36, 15, 54, 27] | 0 / 3 |
| scullery: Scullery / kitchen | Investigation and ordinary objects | [36, 5, 54, 15] | 0 / 3 |
| service: Servants corridor | Investigation and ordinary objects | [28, 5, 36, 45] | 0 / 3 |
| garden: Walled rear garden | Investigation and ordinary objects | [4, 5, 28, 27] | 0 / 3 |
| greenhouse: Greenhouse | Investigation and ordinary objects | [4, 5, 16, 15] | 0 / 3 |
| gallery: Upper gallery | Investigation and ordinary objects | [20, 27, 28, 55] | 3.6 / 3 |
| suite: Master suite | Investigation and ordinary objects | [4, 27, 20, 45] | 3.6 / 3 |
| servants: Upper servants passage | Investigation and ordinary objects | [28, 15, 36, 55] | 3.6 / 3 |
| attic: Attic archive | Investigation and ordinary objects | [4, 27, 20, 45] | 7.2 / 3 |
| cellar: Wine cellar | Investigation and ordinary objects | [36, 15, 54, 27] | -3 / 3 |

## Actual capture coverage

Kind: **RUNTIME_RENDERER_DIAGNOSTIC**. Rooms: 15; directional views: 60; missing views: 0; displaced cameras: 1.

Source-only packets need native source inspection and later actual runtime integration. A complete camera matrix proves capture coverage, not a structurally coherent place, swept access, gameplay or hardware performance.

## Next acceptance work

Inspect every room/child space, resolve generic function descriptions, compare against the site work package, record retain/refit/replace decisions and attach physical route/fixture evidence. Consult the complete fitness checklist and preserve the canonical van contract.

Absent candidate inputs (not all apply to every site):

- `development/levels/power-plans/mansion.json`
