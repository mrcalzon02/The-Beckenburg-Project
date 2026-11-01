# Camp Hollow Creek — rebuild baseline

Packet: `WLD-02`. **S00 IN_PROGRESS; no construction stage accepted.**

## Preserved source inventory

| Source | Bytes | SHA-256 |
| --- | ---: | --- |
| `development/levels/layouts/wld-02/layout.json` | 14108 | `a60a1cd25311f701933f79af3454d67e1830ca60218a5e8733e5e13a50d9fdff` |
| `development/levels/blockouts/wld-02/implementation.json` | 2189 | `bebdef5c7cd6ecec926b60f4feabaff8d7b1bde1fcfed695e416cbcb4e3e2924` |
| `development/levels/blockouts/wld-02/NEXT.md` | 1009 | `70278b75ff943a15b04d620b4e8ec0fa2c49064473e2437dad5a146db88c0ada` |
| `development/levels/blockouts/wld-02/rough-shell.blend` | 125982 | `2d7115522c7bf0e6b0173f069c916ad23a46bb5e872c222f73eced06237e20dd` |
| `development/levels/blockouts/wld-02/rough-shell.glb` | 1008220 | `68258cb0851a60f6f82ed5a24233713fadd6a316dd0fdfc5e894e103a1d743b1` |
| `development/levels/blockouts/wld-02/preview.png` | 596571 | `733f8b17a5a70c7da86206d6623faf6680e3e0891dec7aa8643c5ce0c3a25c92` |
| `development/levels/model-requirements/wld-02.json` | 19442 | `fdd6dd333a16baf29d64aa918f4121a015ee40f8b0a19b87a900a705386bd3ca` |
| `development/levels/edge-sky-plans/wld-02/plan.json` | 55750 | `3422f9f2776f415a225ece47b708f574c13b5abbec77775fb1584976a363ddae` |
| `development/levels/power-plans/wld-02.json` | 2196 | `8c50134f6f2186739939911108c5fe89dfcaf8e43477284a7df08937f681009c` |
| `development/levels/hollow-creek/hollow-creek-route-grade.blend` | 34061239 | `96d6f8dc295739fbe48d3d1b9e07f764f4a7efcd196fcfff589aa9006373a506` |
| `development/levels/hollow-creek/route-grade.json` | 1726 | `e038dd7b6efb8f943f7e0431ab55cf7f7c769fc26f05cef1dd5969826cd699a9` |
| `development/levels/hollow-creek/hollow-creek-green-grass.blend` | 33898427 | `e7d53ad52972b5963132876aa40540fa197c8f92db131d8360c90c378ee7740f` |
| `development/levels/hollow-creek/green-grass.json` | 98506 | `cbdaef58080c7f1bca9d72c0f957fb0b381ce781c5a0460ad1eb555a41db5fc0` |
| `development/levels/DEVELOPMENT_HANDOFF_2026-10-25_WATER_RUNTIME.md` | 11636 | `2a90106d917553e2bbf78768c139a48f5e1b6477acf680595febbd408245c261` |

## Source planning reservations

These are existing layout reservations, not measurements of accepted runtime rooms.

| Space | Purpose in existing source | X/Z bounds (m) | Standing Y / clear height (m) |
| --- | --- | --- | --- |
| stage: Camp-road van apron | Investigation and ordinary objects | [24, 72, 40, 84] | 0 / 3 |
| entry: Ranger gate / noticeboard | Investigation and ordinary objects | [24, 60, 40, 72] | 0 / 3 |
| ranger: Ranger cabin | Investigation and ordinary objects | [8, 48, 22, 60] | 0 / 3 |
| bath: Bathhouse | Investigation and ordinary objects | [42, 48, 56, 60] | 0 / 3 |
| pavilion: Dining pavilion | Investigation and ordinary objects | [24, 34, 40, 50] | 0 / 3 |
| camp-a: West tent pitches | Investigation and ordinary objects | [6, 28, 22, 46] | 0 / 3 |
| camp-b: East tent pitches | Investigation and ordinary objects | [42, 28, 58, 46] | 0 / 3 |
| junction: Trail-marker junction | Investigation and ordinary objects | [24, 20, 40, 34] | 0 / 3 |
| camper: Abandoned camper | Investigation and ordinary objects | [8, 14, 18, 26] | 0 / 3 |
| shore: Lakeside clearing | Investigation and ordinary objects | [24, 4, 40, 20] | 0 / 3 |
| dock: Lake dock | Investigation and ordinary objects | [24, -10, 32, 4] | 0 / 3 |
| creek: Creek footbridge | Investigation and ordinary objects | [42, 8, 54, 18] | 0 / 3 |
| store: Ranger tool store | Investigation and ordinary objects | [2, 48, 8, 58] | 0 / 3 |

## Actual capture coverage

Kind: **RUNTIME_RENDERER_DIAGNOSTIC**. Rooms: 15; directional views: 60; missing views: 0; displaced cameras: 2.

Source-only packets need native source inspection and later actual runtime integration. A complete camera matrix proves capture coverage, not a structurally coherent place, swept access, gameplay or hardware performance.

## Next acceptance work

Inspect every room/child space, resolve generic function descriptions, compare against the site work package, record retain/refit/replace decisions and attach physical route/fixture evidence. Consult the complete fitness checklist and preserve the canonical van contract.
