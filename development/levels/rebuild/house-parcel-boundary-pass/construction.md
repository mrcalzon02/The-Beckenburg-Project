# Briar House parcel and entrance approaches

The 66 timber panels follow fourteen surveyed boundary runs. Each panel sits at its original installed bearing on saved terrain. The game draws and queries the same 28,512 triangles, including the posts, rails and irregular pickets. Their physical height and the space between pickets now determine what a player can see and what a moving body or item can pass.

The plan shows how the ramp, front garden grade, stone apron and flush porch lead to door 14 at the vestibule. Door 15 serves the rear kitchen and garden. The dashed approaches are route guidance. Players walk on the saved ground, ramp, apron and porch, and enter through the framed opening.

[Measured parcel plan](parcel-plan.svg) · [Panel, grade and door survey](boundary-survey.json) · [Prior porch section and editable scene](../flush-porch-and-threshold-pass/house/construction.md)

## Game verification

The two actual door approaches passed 920 movement samples in both directions while standing and crouching. Both leaves stopped the exterior approaches when closed.

Every panel and join was checked against the survey. The ground reached the base at all 33 samples along each panel. The highest measured gap was −24.3 mm, meaning the base was still buried. All 396 supported standing and crouching movement sweeps stopped at the fence. The physics check found all 17,424 expected body contacts, with clear space above each tested post.

Gameplay rays stopped at timber in 6,697 checks and passed through real gaps in 3,709 checks. Small and large objects behaved differently in 65 confirmed gap controls, and 2,068 tests confirmed timber stopped an object. The House carriageway remained usable and all three source readables passed. Farmhouse and School also passed their enclosure, carriageway and readable checks.

[Quantitative checks and capture hashes](verification/house-controls.json) · [Regression and roster checkpoint](checkpoint.json)

## Standing views

- [Rear service return](../captures/house-parcel-boundary-batch/house-rear-service-return.png)
- [West bend and grade](../captures/house-parcel-boundary-batch/house-west-bend.png)
- [Carriageway and south return](../captures/house-parcel-boundary-batch/house-carriageway-south-return.png)
- [Van and south boundary](../captures/house-parcel-boundary-batch/house-van-and-south-boundary.png)

House perimeter acceptance is recorded in the active roster. Whole-site gameplay and roster acceptance remain open.
