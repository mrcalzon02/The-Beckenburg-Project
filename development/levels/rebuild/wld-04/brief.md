# Widow's Peak Fire Lookout — first-principles function brief

**S01 IN_PROGRESS.** These are authored design requirements, not completed construction.

## Place and normal use

A small hillside station clusters around a steep but bounded path and a tall lookout. Investigations span hut, ruined station and clearing; the tower is an optional risky observation branch.

One-person lookout duty station with occasional maintenance support; tower remains an optional risky branch.

Proposed design program for normal former use; verify against measured assets and final plans. These are not historical facts, current NPC counts or changes to the six-player contract.

## Privacy, services and circulation

Public arrivals must not cross private sleeping, washing or confidential work; goods, maintenance and waste need a credible service route. Spatial access classes are design requirements, not new gameplay permissions or locks.

The adjacent `space-program.csv` assigns a specific everyday activity to every named space. `routes.csv` preserves the existing measured route reservations for review; lengths and walking estimates are planning quantities, not swept movement results.

## Required subordinate spaces and working zones

Inspect existing facilities first. Each row is a requirement to resolve in the rough plan, not a declaration that the source currently lacks the facility.

| Parent reservation | Required child space / task zone | Decision |
| --- | --- | --- |
| hut | Maintenance work, emergency shelter and sanitary provision | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| cab | Observation, radio and one sleeping place | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| store | Fuel/supply and service separation | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |

## Dimension and construction decisions

Review inherited room size against actual task furniture, capacity, source-mesh bounds, door swings, window privacy, stair headroom, service access and the ritual/hiding contracts. No inherited rectangle is accepted merely because it already has a name. Real child rooms need physical walls/openings and an explicit mapping into stable gameplay zones.

## Reference and acceptance status

Preserve the approved concept/lore and consult `ARCHITECTURAL_REFERENCES.md`; per-location measured reference inspection and final capacity/dimension acceptance are still required. Use the [site work package](../../FIRST_PRINCIPLES_SITE_PACKAGES.md) and the existing baseline record to distinguish retained assets, changes and missing runtime integration. This brief grants no map unlock.

Pinned existing layout: `development/levels/layouts/wld-04/layout.json`; SHA-256 `0a477753e3b1cc8613421df2ae293c06ba034b7663b769410098596f57c426c9`.
