# Ravenshade School — first-principles function brief

**S01 IN_PROGRESS.** These are authored design requirements, not completed construction.

## Place and normal use

Trace of current authored architecture for review. Preserve its measured footprint and floor elevations while planning the full roster.

Two general classes of 24 pupils sharing one science room, library, meals, assembly and physical education; approximately six staff support positions.

Proposed design program for normal former use; verify against measured assets and final plans. These are not historical facts, current NPC counts or changes to the six-player contract.

## Privacy, services and circulation

Public arrivals must not cross private sleeping, washing or confidential work; goods, maintenance and waste need a credible service route. Spatial access classes are design requirements, not new gameplay permissions or locks.

The adjacent `space-program.csv` assigns a specific everyday activity to every named space. `routes.csv` preserves the existing measured route reservations for review; lengths and walking estimates are planning quantities, not swept movement results.

## Required subordinate spaces and working zones

Inspect existing facilities first. Each row is a requirement to resolve in the rough plan, not a declaration that the source currently lacks the facility.

| Parent reservation | Required child space / task zone | Decision |
| --- | --- | --- |
| r00 | Visitor/admin interview and accessible WC | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| r04 | Food delivery, kitchen preparation, cleanup and refuse | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| r06 | Two changing/shower groups and equipment store | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| r07 | Independently bounded student washrooms with privacy at entrances | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| r05 | Nurse consultation, rest and handwashing | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| r01 | Staff room, cleaner cupboard and classroom storage | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |

## Dimension and construction decisions

Review inherited room size against actual task furniture, capacity, source-mesh bounds, door swings, window privacy, stair headroom, service access and the ritual/hiding contracts. No inherited rectangle is accepted merely because it already has a name. Real child rooms need physical walls/openings and an explicit mapping into stable gameplay zones.

## Reference and acceptance status

Preserve the approved concept/lore and consult `ARCHITECTURAL_REFERENCES.md`; per-location measured reference inspection and final capacity/dimension acceptance are still required. Use the [site work package](../../FIRST_PRINCIPLES_SITE_PACKAGES.md) and the existing baseline record to distinguish retained assets, changes and missing runtime integration. This brief grants no map unlock.

Pinned existing layout: `development/levels/layouts/school/layout.json`; SHA-256 `811fdf96d90cf1e154e4279f924a3c108a7c5318c98d2d2fff644ac854736b00`.
