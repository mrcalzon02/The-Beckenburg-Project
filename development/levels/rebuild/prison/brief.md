# Beckenburg Penitentiary — first-principles function brief

**S01 IN_PROGRESS.** These are authored design requirements, not completed construction.

## Place and normal use

Two parallel cell blocks face a bounded exercise yard. Administration forms a front bar and mess/medical spaces close the rear, giving a legible perimeter circuit.

Bounded two-wing compound: eight single cells per wing per floor over two floors, plus two isolation cells; thirty-four prisoner places in the proposed design.

Proposed design program for normal former use; verify against measured assets and final plans. These are not historical facts, current NPC counts or changes to the six-player contract.

## Privacy, services and circulation

Public arrivals must not cross private sleeping, washing or confidential work; goods, maintenance and waste need a credible service route. Spatial access classes are design requirements, not new gameplay permissions or locks.

The adjacent `space-program.csv` assigns a specific everyday activity to every named space. `routes.csv` preserves the existing measured route reservations for review; lengths and walking estimates are planning quantities, not swept movement results.

## Required subordinate spaces and working zones

Inspect existing facilities first. Each row is a requirement to resolve in the rough plan, not a declaration that the source currently lacks the facility.

| Parent reservation | Required child space / task zone | Decision |
| --- | --- | --- |
| block-a | Eight real single cells per floor; beds, WC, basin and barred/solid door interfaces | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| block-b | Eight real single cells per floor with separately controlled access | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| gallery-a | Actual upper cells, guarded galleries and physical stair landing | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| gallery-b | Actual upper cells, guarded galleries and independent stairs | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| intake | Search, holding, records and staff/public sanitary facilities | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| guard | Supervision and physically placed gate controls | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| mess | Preparation kitchen, food store, washing-up and controlled serving | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| chapel | Physically separate visitation and worship areas | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| medical | Examination and clean storage | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| isolation | Two cells, observation and private sanitary provision | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| plant | Maintenance and shower-service distribution | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |

## Dimension and construction decisions

Review inherited room size against actual task furniture, capacity, source-mesh bounds, door swings, window privacy, stair headroom, service access and the ritual/hiding contracts. No inherited rectangle is accepted merely because it already has a name. Real child rooms need physical walls/openings and an explicit mapping into stable gameplay zones.

## Reference and acceptance status

Preserve the approved concept/lore and consult `ARCHITECTURAL_REFERENCES.md`; per-location measured reference inspection and final capacity/dimension acceptance are still required. Use the [site work package](../../FIRST_PRINCIPLES_SITE_PACKAGES.md) and the existing baseline record to distinguish retained assets, changes and missing runtime integration. This brief grants no map unlock.

Pinned existing layout: `development/levels/layouts/prison/layout.json`; SHA-256 `ab20558ecb1820020a7dd872f81ae079291134422db7e8e59a8dbedc7954205a`.
