# St. Alban Academy — rebuild baseline

Packet: `ACADEMY`. **S00 IN_PROGRESS; no construction stage accepted.**

## Preserved source inventory

| Source | Bytes | SHA-256 |
| --- | ---: | --- |
| `development/levels/layouts/academy/layout.json` | 19592 | `46ddfa3e1e57e67c0ce51518da8dfe0b040f0d3f2b716850ec3ae0359244bdc5` |
| `development/levels/blockouts/academy/implementation.json` | 8902 | `f90b8239e6dc1b59806ee9c395932c56ad27576b536e7cd3f42f0a3d0ead4894` |
| `development/levels/blockouts/academy/NEXT.md` | 1020 | `b6fc7978f7ac211ae62288837d68372dfaeb9eeb02b90a5535a3e0916f3b3f37` |
| `development/levels/blockouts/academy/rough-shell.blend` | 179338 | `22f4aacd7215d1e173f7ff12e0f47ff398344c1871f66284ef8d6308e9703d46` |
| `development/levels/blockouts/academy/rough-shell.glb` | 1325140 | `84e178be286ad9f49ebe29d0273d39c24a4b8f85c601d822f7be5b7cb3ab443c` |
| `development/levels/blockouts/academy/preview.png` | 649637 | `c4846edc934c0590e59e048f90b9c3e05ae6ee5f8f74e1d72a2f626d1382baac` |
| `development/levels/model-requirements/academy.json` | 26083 | `616bd406f102d3bdf4c32a5d0b271c87cce20532496e406160843f4123a16828` |
| `development/levels/edge-sky-plans/academy/plan.json` | 52644 | `a4924660f463af191b5c1ef65e519516ed25f6bca6db80f3bc27aabf91c23a51` |

## Source planning reservations

These are existing layout reservations, not measurements of accepted runtime rooms.

| Space | Purpose in existing source | X/Z bounds (m) | Standing Y / clear height (m) |
| --- | --- | --- | --- |
| stage: Gate van apron | Investigation and ordinary objects | [24, 68, 40, 80] | 0 / 3 |
| entry: Front gate court | Investigation and ordinary objects | [24, 58, 40, 68] | 0 / 3 |
| reception: Reception hall | Investigation and ordinary objects | [24, 46, 40, 58] | 0 / 3 |
| south: South cloister | Investigation and ordinary objects | [12, 40, 52, 46] | 0 / 3 |
| west: West cloister | Investigation and ordinary objects | [12, 16, 18, 40] | 0 / 3 |
| north: North cloister | Investigation and ordinary objects | [12, 10, 52, 16] | 0 / 3 |
| east: East cloister | Investigation and ordinary objects | [46, 16, 52, 40] | 0 / 3 |
| quad: Inner quadrangle | Investigation and ordinary objects | [18, 16, 46, 40] | 0 / 3 |
| library: Library | Investigation and ordinary objects | [0, 16, 12, 40] | 0 / 3 |
| lecture: Lecture room | Investigation and ordinary objects | [52, 26, 68, 40] | 0 / 3 |
| lab: Laboratory / study | Investigation and ordinary objects | [52, 10, 68, 26] | 0 / 3 |
| dining: Dining hall | Investigation and ordinary objects | [18, 0, 46, 10] | 0 / 3 |
| office: Headmaster office | Investigation and ordinary objects | [0, 0, 12, 16] | 0 / 3 |
| dorm: Upper dormitory | Investigation and ordinary objects | [18, 0, 46, 10] | 3.6 / 3 |
| upper: Upper north gallery | Investigation and ordinary objects | [12, 10, 52, 16] | 3.6 / 3 |
| archive: Basement archive | Investigation and ordinary objects | [0, 16, 12, 40] | -3 / 3 |
| rear: Rear quadrangle / service yard | Investigation and ordinary objects | [52, 40, 68, 58] | 0 / 3 |

## Actual capture coverage

Kind: **RUNTIME_RENDERER_DIAGNOSTIC**. Rooms: 15; directional views: 60; missing views: 0; displaced cameras: 1.

Source-only packets need native source inspection and later actual runtime integration. A complete camera matrix proves capture coverage, not a structurally coherent place, swept access, gameplay or hardware performance.

## Next acceptance work

Inspect every room/child space, resolve generic function descriptions, compare against the site work package, record retain/refit/replace decisions and attach physical route/fixture evidence. Consult the complete fitness checklist and preserve the canonical van contract.

Absent candidate inputs (not all apply to every site):

- `development/levels/power-plans/academy.json`
