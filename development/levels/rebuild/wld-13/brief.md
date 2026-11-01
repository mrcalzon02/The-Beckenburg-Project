# Greywater Cannery — first-principles function brief

**S01 IN_PROGRESS.** These are authored design requirements, not completed construction.

## Place and normal use

A shore-side processing hall connects cold stores, boiler plant and shipping dock. Workers housing sits along the forest edge, linked by a short service-yard loop.

Small processing plant with eight crew lodging places; receiving, processing, cold storage, packing and dispatch remain distinct.

Proposed design program for normal former use; verify against measured assets and final plans. These are not historical facts, current NPC counts or changes to the six-player contract.

## Privacy, services and circulation

Public arrivals must not cross private sleeping, washing or confidential work; goods, maintenance and waste need a credible service route. Spatial access classes are design requirements, not new gameplay permissions or locks.

The adjacent `space-program.csv` assigns a specific everyday activity to every named space. `routes.csv` preserves the existing measured route reservations for review; lengths and walking estimates are planning quantities, not swept movement results.

## Required subordinate spaces and working zones

Inspect existing facilities first. Each row is a requirement to resolve in the rough plan, not a declaration that the source currently lacks the facility.

| Parent reservation | Required child space / task zone | Decision |
| --- | --- | --- |
| processing | Receiving, processing, packing transfer and hygiene boundaries | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| wash | WC, showers, changing and cleaner storage | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| bunk | Eight sleeping places and personal storage | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| mess | Cooking, food storage and cleanup | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| boiler | Process plant and generator/distribution maintenance | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |

## Dimension and construction decisions

Review inherited room size against actual task furniture, capacity, source-mesh bounds, door swings, window privacy, stair headroom, service access and the ritual/hiding contracts. No inherited rectangle is accepted merely because it already has a name. Real child rooms need physical walls/openings and an explicit mapping into stable gameplay zones.

## Reference and acceptance status

Preserve the approved concept/lore and consult `ARCHITECTURAL_REFERENCES.md`; per-location measured reference inspection and final capacity/dimension acceptance are still required. Use the [site work package](../../FIRST_PRINCIPLES_SITE_PACKAGES.md) and the existing baseline record to distinguish retained assets, changes and missing runtime integration. This brief grants no map unlock.

Pinned existing layout: `development/levels/layouts/wld-13/layout.json`; SHA-256 `4d8dd90826c02b30cb4c64073f745a847f63e16110aaa4f8297b4543e0c6a732`.
