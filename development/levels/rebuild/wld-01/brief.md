# Blackpine Logging Camp — first-principles function brief

**S01 IN_PROGRESS.** These are authored design requirements, not completed construction.

## Place and normal use

A horseshoe of working buildings around a log yard, with a separate cabin path and original foundations beyond the sawmill. Distinct roofs and yard landmarks bound the search.

Small timber operation with twelve crew sleeping places, communal meals/washing, log processing and maintenance.

Proposed design program for normal former use; verify against measured assets and final plans. These are not historical facts, current NPC counts or changes to the six-player contract.

## Privacy, services and circulation

Public arrivals must not cross private sleeping, washing or confidential work; goods, maintenance and waste need a credible service route. Spatial access classes are design requirements, not new gameplay permissions or locks.

The adjacent `space-program.csv` assigns a specific everyday activity to every named space. `routes.csv` preserves the existing measured route reservations for review; lengths and walking estimates are planning quantities, not swept movement results.

## Required subordinate spaces and working zones

Inspect existing facilities first. Each row is a requirement to resolve in the rough plan, not a declaration that the source currently lacks the facility.

| Parent reservation | Required child space / task zone | Decision |
| --- | --- | --- |
| bunk | Twelve sleeping places and personal storage | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| mess | Cooking, food storage and cleanup | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| wash | Private WC, wash and change facilities | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| mill | Machine feed/output and maintenance envelopes | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| shed | Repair surfaces and parts storage | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |

## Dimension and construction decisions

Review inherited room size against actual task furniture, capacity, source-mesh bounds, door swings, window privacy, stair headroom, service access and the ritual/hiding contracts. No inherited rectangle is accepted merely because it already has a name. Real child rooms need physical walls/openings and an explicit mapping into stable gameplay zones.

## Reference and acceptance status

Preserve the approved concept/lore and consult `ARCHITECTURAL_REFERENCES.md`; per-location measured reference inspection and final capacity/dimension acceptance are still required. Use the [site work package](../../FIRST_PRINCIPLES_SITE_PACKAGES.md) and the existing baseline record to distinguish retained assets, changes and missing runtime integration. This brief grants no map unlock.

Pinned existing layout: `development/levels/layouts/wld-01/layout.json`; SHA-256 `ebdfe3334052edac85caaad5e52f9c940cec1c26cd4a12cd67cae8b2a512281a`.
