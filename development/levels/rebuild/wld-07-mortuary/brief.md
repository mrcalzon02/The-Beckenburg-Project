# Saint Bartholomew's Cemetery & Vale Mortuary — first-principles function brief

**S01 IN_PROGRESS.** These are authored design requirements, not completed construction.

## Place and normal use

A bounded burial ground is organized by a main avenue and memorial ring. Ruined chapel and caretaker cottage face the approach; two ossuary entrances join underground. Add the adjoining Vale Mortuary as a separately reviewed east-side annex; the original cemetery packet remains the source of truth until this expansion is accepted.

Shared cemetery mission plus a small mortuary; separate public viewing and confidential staff/preparation/transfer functions.

Proposed design program for normal former use; verify against measured assets and final plans. These are not historical facts, current NPC counts or changes to the six-player contract.

## Privacy, services and circulation

Public arrivals must not cross private sleeping, washing or confidential work; goods, maintenance and waste need a credible service route. Spatial access classes are design requirements, not new gameplay permissions or locks.

The adjacent `space-program.csv` assigns a specific everyday activity to every named space. `routes.csv` preserves the existing measured route reservations for review; lengths and walking estimates are planning quantities, not swept movement results.

## Required subordinate spaces and working zones

Inspect existing facilities first. Each row is a requirement to resolve in the rough plan, not a declaration that the source currently lacks the facility.

| Parent reservation | Required child space / task zone | Decision |
| --- | --- | --- |
| cottage | Caretaker domestic and sanitary support | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| mort-reception | Visitor WC and confidential consultation | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| mort-viewing | Private viewing room separated from service transfer | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| mort-prep | Preparation, handwashing, clean/soiled separation and staff-only storage | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| mort-office | Confidential records and staff changing/WC | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |
| mort-cold | Serviced cold storage with maintenance clearance | REVIEW_EXISTING_OR_AUTHOR_IN_ROUGH_PLAN |

## Accessible search envelope

Owner direction, 3 November: maximum **40 accessible search spaces** for this site, including corridors, stairs, service rooms, outdoor search sectors and staging. Build the maps inside this size budget; round-duration testing is outside this task. Retain complete normal-use architecture as sealed context. A wing may expose only its hallway; permanent closures must keep tasks, evidence, ghost activity and required routes in the connected open portion. See [search-envelope policy](../SEARCH_ENVELOPE_POLICY.json) and [all-site register](../search-envelope-register.json). Stable fifteen gameplay zones are not a substitute for actual accessible room counts.


## Dimension and construction decisions

Review inherited room size against actual task furniture, capacity, source-mesh bounds, door swings, window privacy, stair headroom, service access and the ritual/hiding contracts. No inherited rectangle is accepted merely because it already has a name. Real child rooms need physical walls/openings and an explicit mapping into stable gameplay zones.

## Reference and acceptance status

Preserve the approved concept/lore and consult `ARCHITECTURAL_REFERENCES.md`; per-location measured reference inspection and final capacity/dimension acceptance are still required. Use the [site work package](../../FIRST_PRINCIPLES_SITE_PACKAGES.md) and the existing baseline record to distinguish retained assets, changes and missing runtime integration. This brief grants no map unlock.

Pinned existing layout: `development/levels/layout-expansions/wld-07-mortuary/layout.json`; SHA-256 `36dd0c843f6162bbb7461f729a20bf2c4ed5345f7c74cb7eb89ec37379fb06c0`.
