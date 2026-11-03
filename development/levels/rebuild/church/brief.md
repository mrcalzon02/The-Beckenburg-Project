# St. Brigid Parish — first-principles function brief

**S01 IN_PROGRESS.** These are authored design requirements, not completed construction.

## Place and normal use

Trace of current authored architecture for review. Preserve its measured footprint and floor elevations while planning the full roster.

Small parish worship building with approximately forty seated places, choir, parish work and memorial/crypt access.

Proposed design program for normal former use; verify against measured assets and final plans. These are not historical facts, current NPC counts or changes to the six-player contract.

## Privacy, services and circulation

Public arrivals must not cross private sleeping, washing or confidential work; goods, maintenance and waste need a credible service route. Spatial access classes are design requirements, not new gameplay permissions or locks.

The adjacent `space-program.csv` assigns a specific everyday activity to every named space. `routes.csv` preserves the existing measured route reservations for review; lengths and walking estimates are planning quantities, not swept movement results.

## Required subordinate spaces and working zones

Inspect existing facilities first. Each row is a requirement to resolve in the rough plan, not a declaration that the source currently lacks the facility.

| Parent reservation | Required child space / task zone | Decision |
| --- | --- | --- |
| r02 | Parish office and discreet sanitary support | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| r06 | Vestment preparation and washing | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| r07 | Choir circulation outside loft guards | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| r09 | Music storage and organ maintenance access | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |

## Accessible search envelope

Owner direction, 3 November: maximum **40 accessible search spaces** for this site, including corridors, stairs, service rooms, outdoor search sectors and staging. Build the maps inside this size budget; round-duration testing is outside this task. Retain complete normal-use architecture as sealed context. A wing may expose only its hallway; permanent closures must keep tasks, evidence, ghost activity and required routes in the connected open portion. See [search-envelope policy](../SEARCH_ENVELOPE_POLICY.json) and [all-site register](../search-envelope-register.json). Stable fifteen gameplay zones are not a substitute for actual accessible room counts.


## Dimension and construction decisions

Review inherited room size against actual task furniture, capacity, source-mesh bounds, door swings, window privacy, stair headroom, service access and the ritual/hiding contracts. No inherited rectangle is accepted merely because it already has a name. Real child rooms need physical walls/openings and an explicit mapping into stable gameplay zones.

## Reference and acceptance status

Preserve the approved concept/lore and consult `ARCHITECTURAL_REFERENCES.md`; per-location measured reference inspection and final capacity/dimension acceptance are still required. Use the [site work package](../../FIRST_PRINCIPLES_SITE_PACKAGES.md) and the existing baseline record to distinguish retained assets, changes and missing runtime integration. This brief grants no map unlock.

Pinned existing layout: `development/levels/layouts/church/layout.json`; SHA-256 `e5ed72ba9fa9bd002b34f75b951c876b5416666db80e219142f604159b80baca`.
