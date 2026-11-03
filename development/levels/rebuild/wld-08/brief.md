# Northwatch Research Station — first-principles function brief

**S01 IN_PROGRESS.** These are authored design requirements, not completed construction.

## Place and normal use

A compact ring of insulated prefabs surrounds a flagged service court. A heated tunnel links lab, dormitory and generator while the instrument array remains an exposed optional branch.

Eight-person remote research station with four paired bunk spaces, shared meals/washing, research and independent services.

Proposed design program for normal former use; verify against measured assets and final plans. These are not historical facts, current NPC counts or changes to the six-player contract.

## Privacy, services and circulation

Public arrivals must not cross private sleeping, washing or confidential work; goods, maintenance and waste need a credible service route. Spatial access classes are design requirements, not new gameplay permissions or locks.

The adjacent `space-program.csv` assigns a specific everyday activity to every named space. `routes.csv` preserves the existing measured route reservations for review; lengths and walking estimates are planning quantities, not swept movement results.

## Required subordinate spaces and working zones

Inspect existing facilities first. Each row is a requirement to resolve in the rough plan, not a declaration that the source currently lacks the facility.

| Parent reservation | Required child space / task zone | Decision |
| --- | --- | --- |
| dorm | Eight bunks, WC, showers and clothing transition | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| mess | Cooking, food storage and cleanup | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| lab | Research benches, sample storage and handwashing | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| generator | Source, distribution, water plant and maintenance access | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |

## Dimension and construction decisions

Review inherited room size against actual task furniture, capacity, source-mesh bounds, door swings, window privacy, stair headroom, service access and the ritual/hiding contracts. No inherited rectangle is accepted merely because it already has a name. Real child rooms need physical walls/openings and an explicit mapping into stable gameplay zones.

## Reference and acceptance status

Preserve the approved concept/lore and consult `ARCHITECTURAL_REFERENCES.md`; per-location measured reference inspection and final capacity/dimension acceptance are still required. Use the [site work package](../../FIRST_PRINCIPLES_SITE_PACKAGES.md) and the existing baseline record to distinguish retained assets, changes and missing runtime integration. This brief grants no map unlock.

Pinned existing layout: `development/levels/layouts/wld-08/layout.json`; SHA-256 `616f6928fa4a55515027b3b3dd434ba6db6d044df2ab9ecb19e862efb675955b`.
