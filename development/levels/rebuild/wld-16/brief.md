# Eight Pines Motor Lodge — first-principles function brief

**S01 IN_PROGRESS.** These are authored design requirements, not completed construction.

## Place and normal use

An independent old eight-unit roadside motor lodge, with exterior room doors, reception/manager lodging and a believable linen/plant service route.

Eight guest units, each with its own sanitary child space, plus separate manager accommodation and housekeeping/service rooms.

Proposed design program for normal former use; verify against measured assets and final plans. These are not historical facts, current NPC counts or changes to the six-player contract.

## Privacy, services and circulation

Public arrivals must not cross private sleeping, washing or confidential work; goods, maintenance and waste need a credible service route. Spatial access classes are design requirements, not new gameplay permissions or locks.

The adjacent `space-program.csv` assigns a specific everyday activity to every named space. `routes.csv` preserves the existing measured route reservations for review; lengths and walking estimates are planning quantities, not swept movement results.

## Required subordinate spaces and working zones

Inspect existing facilities first. Each row is a requirement to resolve in the rough plan, not a declaration that the source currently lacks the facility.

| Parent reservation | Required child space / task zone | Decision |
| --- | --- | --- |
| room1 | Private sanitary compartment | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| room2 | Private sanitary compartment | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| room3 | Private sanitary compartment | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| room4 | Private sanitary compartment | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| room5 | Private sanitary compartment | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| room6 | Private sanitary compartment | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| room7 | Private sanitary compartment | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| room8 | Private sanitary compartment | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| manager | Separate living, cooking, sleeping and sanitation | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| laundry | Dirty/clean linen, washing, drying and storage | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |

## Dimension and construction decisions

Review inherited room size against actual task furniture, capacity, source-mesh bounds, door swings, window privacy, stair headroom, service access and the ritual/hiding contracts. No inherited rectangle is accepted merely because it already has a name. Real child rooms need physical walls/openings and an explicit mapping into stable gameplay zones.

## Reference and acceptance status

Preserve the approved concept/lore and consult `ARCHITECTURAL_REFERENCES.md`; per-location measured reference inspection and final capacity/dimension acceptance are still required. Use the [site work package](../../FIRST_PRINCIPLES_SITE_PACKAGES.md) and the existing baseline record to distinguish retained assets, changes and missing runtime integration. This brief grants no map unlock.

Pinned existing layout: `development/levels/layouts/wld-16/layout.json`; SHA-256 `58c73bd8f3693cad8bc163b03546b177c5e84bf835d7715605d56a71af3fe054`.
