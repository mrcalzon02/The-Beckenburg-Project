# Gallows Pass Railway — first-principles function brief

**S01 IN_PROGRESS.** These are authored design requirements, not completed construction.

## Place and normal use

A bounded railway cutting runs past derailed coaches to a tunnel. A maintenance siding path parallels the track, while a guarded trestle has a service walkway beneath its deck.

Bounded track-maintenance and derailment site; coaches retain compartment and baggage functions without implying an operating train.

Proposed design program for normal former use; verify against measured assets and final plans. These are not historical facts, current NPC counts or changes to the six-player contract.

## Privacy, services and circulation

Public arrivals must not cross private sleeping, washing or confidential work; goods, maintenance and waste need a credible service route. Spatial access classes are design requirements, not new gameplay permissions or locks.

The adjacent `space-program.csv` assigns a specific everyday activity to every named space. `routes.csv` preserves the existing measured route reservations for review; lengths and walking estimates are planning quantities, not swept movement results.

## Required subordinate spaces and working zones

Inspect existing facilities first. Each row is a requirement to resolve in the rough plan, not a declaration that the source currently lacks the facility.

| Parent reservation | Required child space / task zone | Decision |
| --- | --- | --- |
| signal | Duty work, records and discreet sanitary support | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| coach-a | Real compartment, corridor and lavatory layout | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| coach-b | Baggage storage and service aisle | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| store | Tools, spares and electrical service | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |

## Dimension and construction decisions

Review inherited room size against actual task furniture, capacity, source-mesh bounds, door swings, window privacy, stair headroom, service access and the ritual/hiding contracts. No inherited rectangle is accepted merely because it already has a name. Real child rooms need physical walls/openings and an explicit mapping into stable gameplay zones.

## Reference and acceptance status

Preserve the approved concept/lore and consult `ARCHITECTURAL_REFERENCES.md`; per-location measured reference inspection and final capacity/dimension acceptance are still required. Use the [site work package](../../FIRST_PRINCIPLES_SITE_PACKAGES.md) and the existing baseline record to distinguish retained assets, changes and missing runtime integration. This brief grants no map unlock.

Pinned existing layout: `development/levels/layouts/wld-12/layout.json`; SHA-256 `a873a8669fdec20b35cdcfdb0c8b97e39b9ef18b26aa545423b61961918b155d`.
