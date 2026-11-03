# Hollowmere Farm — first-principles function brief

**S01 IN_PROGRESS.** These are authored design requirements, not completed construction.

## Place and normal use

Trace of current authored architecture for review. Preserve its measured footprint and floor elevations while planning the full roster.

Family farmhouse with a working barn/hayloft and separate tool maintenance; domestic and agricultural traffic remain distinct.

Proposed design program for normal former use; verify against measured assets and final plans. These are not historical facts, current NPC counts or changes to the six-player contract.

## Privacy, services and circulation

Public arrivals must not cross private sleeping, washing or confidential work; goods, maintenance and waste need a credible service route. Spatial access classes are design requirements, not new gameplay permissions or locks.

The adjacent `space-program.csv` assigns a specific everyday activity to every named space. `routes.csv` preserves the existing measured route reservations for review; lengths and walking estimates are planning quantities, not swept movement results.

## Required subordinate spaces and working zones

Inspect existing facilities first. Each row is a requirement to resolve in the rough plan, not a declaration that the source currently lacks the facility.

| Parent reservation | Required child space / task zone | Decision |
| --- | --- | --- |
| r05 | Private WC, basin and washing child room | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| r02 | Food storage and cleanup | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| r10 | Stall/feed and equipment bays | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| r11 | Repair bench, parts and laundry separation | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |

## Dimension and construction decisions

Review inherited room size against actual task furniture, capacity, source-mesh bounds, door swings, window privacy, stair headroom, service access and the ritual/hiding contracts. No inherited rectangle is accepted merely because it already has a name. Real child rooms need physical walls/openings and an explicit mapping into stable gameplay zones.

## Reference and acceptance status

Preserve the approved concept/lore and consult `ARCHITECTURAL_REFERENCES.md`; per-location measured reference inspection and final capacity/dimension acceptance are still required. Use the [site work package](../../FIRST_PRINCIPLES_SITE_PACKAGES.md) and the existing baseline record to distinguish retained assets, changes and missing runtime integration. This brief grants no map unlock.

Pinned existing layout: `development/levels/layouts/farmhouse/layout.json`; SHA-256 `e050c3627240ac10ba6f44191e549ac631bb9a43c7701cb2daf8291f12c00895`.
