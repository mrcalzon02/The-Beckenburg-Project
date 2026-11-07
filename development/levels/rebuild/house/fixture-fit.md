# House major-fixture construction checkpoint

Source authority: `HouseRebuildFittings.source()`. This candidate consumes the original House pilot rooms, stairs, floor openings, editor-punched terrain, service sockets and canonical van placement. The empty graybox and dispatched House are retained. This checkpoint does not accept the finished terrain, complete utilities, furniture, story or mission.

Five individually fitted existing GLBs retain their source pivots, metre scale, UVs and materials. Actual decoded vertex bounds determine floor contact and matching oriented collision. The toilet's negative source floor offset and bath's raised source origin are handled explicitly through measured bounds. The kitchen/dining doorway moves from Z11.8 to Z10.85 in this candidate to admit the full 2.4m counter without narrowing the opening. The counter remains outside the cellar stair/void and garden entrance.

The bath occupies the bathroom's east wall, toilet and basin its north wall, leaving the south entrance and standing anchor free. The washer occupies the laundry north wall, clear of the breaker and store/service doors. An initial toilet rotation faced its bowl toward the wall; native render review corrected it to face into the room. DEV-299 records that construction-candidate correction; the dispatched House was never changed.

Actual packaged checks: five mesh floor contacts, native pivot/collider correspondence, finished-wall bounds, pairwise separation, door approach clearances, standing anchors and swept traversal to all15 zones/required sockets pass at 60,964 visited positions. Existing empty-pilot physics, shell, service and terrain checks also pass, including 52 persisted editor cutouts and 3,430 shared rendered/physical terrain triangles. These are construction checks, not round-duration tests or complete map acceptance.

Four actual SceneRenderer runtime images (kitchen, toilet/basin, bath, laundry) were viewed. Models remain at source-library quality; no art regeneration or new asset family is required. Evidence is under `verification/house-fit-2026-11-06`; captures are under `development/captures/house-fixture-fit`.

Next: complete the House terrain/boundary gate before promoting this fitted candidate, then measured heating/storage/private/shared room furniture and authored utility distribution. Reuse the fixture-fit method in the institutional blockout batch after the pilot is inspected. The remaining maps retain their individual acceptance gates and the 40 accessible-space ceiling.
