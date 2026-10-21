# Crouching and sparse outdoor refuge

21 October 2026. First reusable movement/cover milestone for the remote map builds.

**Controls:** Ctrl toggles crouching in solo investigation, the LAN hunter viewport and Castle headquarters. L enters/leaves concealment while inside an available cupboard, or while crouched in the centre of a dense shrub. Crouching in open ground is not concealment.

Shared hunter dimensions: standing body 1.78 m / eye 1.62 m; crouched body 1.05 m / eye 0.92 m. Feet retain their supported world elevation. Crouching moves at 48% walking speed, cannot sprint, and must clear the same swept walls, doors and floors with its actual body height. Standing under an obstruction is rejected. Castle retains its conservative existing standing collision envelope; crouching there lowers view/speed without introducing new crawl routes.

The host owns LAN posture, verifies headroom and broadcasts `POSTURE` separately from the unchanged `BODY` frame. Hunter view, teammate silhouette and nameplate height follow that state. Object targeting uses the actual lower eye height in both viewport and host; held loose objects follow the lower carrying height. Remote silhouettes currently use a reduced-height standing sprite; bespoke kneeling animation is a later art pass.

`level_shrub_refuge` is an original CC0 low-poly asset with packed procedural diffuse textures, a floor-centre pivot and a hollow central pocket. Its eight stem/leaf clusters contain 464 triangles. Foliage is soft cover rather than an impassable box. Authored socket IDs and contract hiding availability determine whether effective cover exists; graphics detail does not change the rule. Render caching includes authored revision and hiding availability so a disabled pocket does not retain visible dense refuge geometry.

Existing reference maps receive one authored outdoor pocket each for integration checks; Blackpine receives only three across its entire compound and no automatic indoor cupboards. A player must be crouched and within 0.36 m of the pocket centre. Moving or standing exposes the player; a ghost searching within 2.4 m exposes shrub refuge. Both human and AI LAN ghosts use the same host search rule. The current room-based solo ghost uses its actual rendered room-centre position for that check; independent solo ghost spatial pursuit remains part of the existing simulation backlog.

## Checks completed

- All Java 17 main and test sources compile in an isolated verification output.
- Crouch capsule sweeps under a real 1.30 m obstruction; stand-up is rejected there and succeeds after retreat. Sprint intent cannot accelerate crouching or consume sprint stamina.
- Shrub boundaries, 0%/100% availability, movement/stand exposure, nearby search exposure and solo posture/cover behavior pass.
- Real TCP crouch/stand snapshots, unchanged feet, ghost-role rejection and actual Ctrl viewport dispatch pass. Server replies are forwarded to the viewport's normal event handler.
- Existing house/farm/school physical stairs, doors/state, utilities, solo elevation, sprint, cupboards, enclosures, surface integrity, exact-ray targeting, fixtures, loose objects, acoustics, view handedness and device displays pass targeted checks. The real TCP interaction check also passes.
- The new shrub GLB and editable source were generated and studio-rendered; standing and crouched runtime views were captured in Blackpine.

No native interactive Windows playthrough, crouch animation polish, full equipment-by-posture matrix, AI/human hunt balance pass or target-hardware performance acceptance is claimed. Next gate: play through the first Blackpine site before adding another location.
