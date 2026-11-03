# Castle Beckenburg — first-principles function brief

**S01 IN_PROGRESS.** These are authored design requirements, not completed construction.

## Place and normal use

A complete proposed headquarters plan around a courtyard: briefing and dispatch at the south gate, archive and lodging west, armory/workshop/infirmary east, sanctuary and research north, protected training annex beyond the archive.

Existing investigator headquarters with six-player team circulation and real equipment stock; lodging and workshops serve the building rather than inventing extra investigation rules.

Proposed design program for normal former use; verify against measured assets and final plans. These are not historical facts, current NPC counts or changes to the six-player contract.

## Privacy, services and circulation

Public arrivals must not cross private sleeping, washing or confidential work; goods, maintenance and waste need a credible service route. Spatial access classes are design requirements, not new gameplay permissions or locks.

The adjacent `space-program.csv` assigns a specific everyday activity to every named space. `routes.csv` preserves the existing measured route reservations for review; lengths and walking estimates are planning quantities, not swept movement results.

## Required subordinate spaces and working zones

Inspect existing facilities first. Each row is a requirement to resolve in the rough plan, not a declaration that the source currently lacks the facility.

| Parent reservation | Required child space / task zone | Decision |
| --- | --- | --- |
| lodging | Sleeping, washing, WC and personal storage | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| dining | Kitchen preparation, serving and cleanup | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| workshop | Repair/chemistry task separation and supported storage | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| infirmary | Private recovery and handwashing | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| armory | Actual stock aisles and issue/return space | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| radio | Communications chair/desk/resident clearance | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |

## Accessible search envelope

Owner direction, 3 November: maximum **40 accessible search spaces** for this site, including corridors, stairs, service rooms, outdoor search sectors and staging. Build the maps inside this size budget; round-duration testing is outside this task. Retain complete normal-use architecture as sealed context. A wing may expose only its hallway; permanent closures must keep tasks, evidence, ghost activity and required routes in the connected open portion. See [search-envelope policy](../SEARCH_ENVELOPE_POLICY.json) and [all-site register](../search-envelope-register.json). Stable fifteen gameplay zones are not a substitute for actual accessible room counts.


## Dimension and construction decisions

Review inherited room size against actual task furniture, capacity, source-mesh bounds, door swings, window privacy, stair headroom, service access and the ritual/hiding contracts. No inherited rectangle is accepted merely because it already has a name. Real child rooms need physical walls/openings and an explicit mapping into stable gameplay zones.

## Reference and acceptance status

Preserve the approved concept/lore and consult `ARCHITECTURAL_REFERENCES.md`; per-location measured reference inspection and final capacity/dimension acceptance are still required. Use the [site work package](../../FIRST_PRINCIPLES_SITE_PACKAGES.md) and the existing baseline record to distinguish retained assets, changes and missing runtime integration. This brief grants no map unlock.

Pinned existing layout: `development/levels/layouts/castle/layout.json`; SHA-256 `6c0c1c078c3423e9debd16fcbc8de1af4b6f011f3b6ba0ab7d48be9754be7d22`.
