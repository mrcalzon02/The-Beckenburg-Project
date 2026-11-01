# Beckenburg Penitentiary — rebuild baseline

Packet: `PRISON`. **S00 IN_PROGRESS; no construction stage accepted.**

## Preserved source inventory

| Source | Bytes | SHA-256 |
| --- | ---: | --- |
| `development/levels/layouts/prison/layout.json` | 20259 | `ab20558ecb1820020a7dd872f81ae079291134422db7e8e59a8dbedc7954205a` |
| `development/levels/blockouts/prison/implementation.json` | 9687 | `499901f5eaa627401367f8e98afb78b18cd98420ce25560d638b49cddf154b22` |
| `development/levels/blockouts/prison/NEXT.md` | 1087 | `4ed26a85645ffecc9e26ff67a3c2e1bb56064e933c6448a59fad90377227fa31` |
| `development/levels/blockouts/prison/rough-shell.blend` | 181230 | `1e39c3f2985162ee691bd258518dfb47e4e4b9844d59ff106f816ef0c874dfcb` |
| `development/levels/blockouts/prison/rough-shell.glb` | 1399208 | `ccba5c8671dbe58870c5610b6a5782772cac5040595c103754ff1b8920ff2085` |
| `development/levels/blockouts/prison/preview.png` | 645412 | `ad7e42c0a7de02c05f4442d55aedf4791d3bb00c97481934fafce28accb6b869` |
| `development/levels/model-requirements/prison.json` | 24075 | `58f7ebef6048042a15b54669ccbc5286a54ba692b289794885043b187dbce138` |
| `development/levels/edge-sky-plans/prison/plan.json` | 53175 | `46a329419a7ee1f6a3e0ca9cb9d223ce27c85c4d79af1006496cc08a80a24a6c` |

## Source planning reservations

These are existing layout reservations, not measurements of accepted runtime rooms.

| Space | Purpose in existing source | X/Z bounds (m) | Standing Y / clear height (m) |
| --- | --- | --- | --- |
| stage: Perimeter van apron | Investigation and ordinary objects | [26, 78, 42, 90] | 0 / 3 |
| entry: Perimeter gate approach | Investigation and ordinary objects | [26, 68, 42, 78] | 0 / 3 |
| sally: Vehicle sally port | Investigation and ordinary objects | [26, 58, 42, 68] | 0 / 3 |
| intake: Intake and records | Investigation and ordinary objects | [10, 58, 26, 68] | 0 / 3 |
| guard: Guard station | Investigation and ordinary objects | [42, 58, 54, 68] | 0 / 3 |
| block-a: Main cell block | Investigation and ordinary objects | [10, 22, 22, 58] | 0 / 6.8 |
| block-b: Secondary cell block | Investigation and ordinary objects | [46, 22, 58, 58] | 0 / 6.8 |
| yard: Exercise yard | Investigation and ordinary objects | [22, 22, 46, 58] | 0 / 3 |
| cross: Rear cross passage | Investigation and ordinary objects | [10, 16, 58, 22] | 0 / 3 |
| mess: Mess hall | Investigation and ordinary objects | [10, 4, 28, 16] | 0 / 3 |
| medical: Infirmary | Investigation and ordinary objects | [28, 4, 46, 16] | 0 / 3 |
| chapel: Chapel / visitation | Investigation and ordinary objects | [58, 34, 70, 52] | 0 / 3 |
| isolation: Isolation wing | Investigation and ordinary objects | [58, 16, 70, 34] | 0 / 3 |
| gallery-a: A-block upper gallery | Investigation and ordinary objects | [10, 22, 14, 58] | 3.4 / 3 |
| gallery-b: B-block upper gallery | Investigation and ordinary objects | [54, 22, 58, 58] | 3.4 / 3 |
| plant: Boiler and maintenance | Investigation and ordinary objects | [22, 22, 46, 42] | -3 / 3 |

## Actual capture coverage

Kind: **RUNTIME_RENDERER_DIAGNOSTIC**. Rooms: 15; directional views: 60; missing views: 0; displaced cameras: 1.

Source-only packets need native source inspection and later actual runtime integration. A complete camera matrix proves capture coverage, not a structurally coherent place, swept access, gameplay or hardware performance.

## Next acceptance work

Inspect every room/child space, resolve generic function descriptions, compare against the site work package, record retain/refit/replace decisions and attach physical route/fixture evidence. Consult the complete fitness checklist and preserve the canonical van contract.

Absent candidate inputs (not all apply to every site):

- `development/levels/power-plans/prison.json`
