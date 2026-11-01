# St. Dymphna Hospital — rebuild baseline

Packet: `ASYLUM`. **S00 IN_PROGRESS; no construction stage accepted.**

## Preserved source inventory

| Source | Bytes | SHA-256 |
| --- | ---: | --- |
| `development/levels/layouts/asylum/layout.json` | 18981 | `d3a12c5153d18c7aa27f851ba2ac1c7d408d94cff29d2f6362445b11eea9d63b` |
| `development/levels/blockouts/asylum/implementation.json` | 8993 | `9f64d3daee273ec59d9f3a5dfbf066d141f7f175c54546baece0848c0a9980f6` |
| `development/levels/blockouts/asylum/NEXT.md` | 1044 | `f7f69e8ccd4450054f4eed03350347b2504a2dadbbedda202f7aed2b68564766` |
| `development/levels/blockouts/asylum/rough-shell.blend` | 178025 | `a87e8468dce9179630b96e1145e87be933135f79a02a9f2ac8ef688511dadf11` |
| `development/levels/blockouts/asylum/rough-shell.glb` | 1334272 | `b649a2af291f43b0897e9bb9c846610ff38ee82233d090a31eb4e30a7e4ea1dd` |
| `development/levels/blockouts/asylum/preview.png` | 622392 | `9f0d66c0433c5718f60023a3f5750771fc70af3f3c50327a2ce3015126ba7bf0` |
| `development/levels/model-requirements/asylum.json` | 24460 | `b038ebd1478b1c0c6976b8c3620fdaf869e465e2b9f85f1b0971bb5eb09e3e4a` |
| `development/levels/edge-sky-plans/asylum/plan.json` | 52669 | `756624d524b3619117ee145e2fdf9900bc7f0848f686137c075386b20f34a850` |

## Source planning reservations

These are existing layout reservations, not measurements of accepted runtime rooms.

| Space | Purpose in existing source | X/Z bounds (m) | Standing Y / clear height (m) |
| --- | --- | --- | --- |
| stage: Front-drive van apron | Investigation and ordinary objects | [28, 72, 44, 84] | 0 / 3 |
| entry: Front drive | Investigation and ordinary objects | [28, 62, 44, 72] | 0 / 3 |
| reception: Admissions reception | Investigation and ordinary objects | [28, 50, 44, 62] | 0 / 3 |
| spine: Central ward corridor | Investigation and ordinary objects | [28, 20, 34, 50] | 0 / 3 |
| records: Records office | Investigation and ordinary objects | [16, 50, 28, 62] | 0 / 3 |
| day: Day room | Investigation and ordinary objects | [10, 34, 28, 50] | 0 / 3 |
| ward: Patient ward | Investigation and ordinary objects | [0, 18, 22, 34] | 0 / 3 |
| west-link: Garden ward passage | Investigation and ordinary objects | [22, 18, 28, 34] | 0 / 3 |
| treatment: Treatment theatre | Investigation and ordinary objects | [34, 20, 50, 34] | 0 / 3 |
| hydro: Hydrotherapy room | Investigation and ordinary objects | [50, 20, 64, 34] | 0 / 3 |
| dining: Dining ward | Investigation and ordinary objects | [34, 34, 54, 50] | 0 / 3 |
| chapel: Remembrance chapel | Investigation and ordinary objects | [10, 2, 28, 18] | 0 / 3 |
| court: Walled courtyard | Investigation and ordinary objects | [0, 34, 10, 62] | 0 / 3 |
| upper: Upper ward gallery | Investigation and ordinary objects | [28, 20, 34, 50] | 3.4 / 3 |
| upper-ward: Upper patient ward | Investigation and ordinary objects | [10, 34, 28, 50] | 3.4 / 3 |
| plant: Basement boiler | Investigation and ordinary objects | [34, 34, 54, 50] | -3 / 3 |

## Actual capture coverage

Kind: **RUNTIME_RENDERER_DIAGNOSTIC**. Rooms: 15; directional views: 60; missing views: 0; displaced cameras: 1.

Source-only packets need native source inspection and later actual runtime integration. A complete camera matrix proves capture coverage, not a structurally coherent place, swept access, gameplay or hardware performance.

## Next acceptance work

Inspect every room/child space, resolve generic function descriptions, compare against the site work package, record retain/refit/replace decisions and attach physical route/fixture evidence. Consult the complete fitness checklist and preserve the canonical van contract.

Absent candidate inputs (not all apply to every site):

- `development/levels/power-plans/asylum.json`
