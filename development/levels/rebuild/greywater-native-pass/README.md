# Greywater Cannery

Select **Greywater Cannery** in the ordinary solo or LAN mission picker. The campaign keeps the original Cannery slot. The packaged level keeps the surveyed logging-road approach, loading yard, processing hall, work buildings, and raised inspection gallery. The path annotations guide review of the built doors, floors, and U-return stair.

Two obsolete overlapping cold-store headers were removed from the Blender source, render mesh, and collision. Both built higher lintels remain. The map has 16 fitted doors, 14 supported world locations, and the van. The washroom, cold store, and bunkhouse have walk-in refuges. Each refuge has 69 verified floor positions, including usable depth past the threshold. Refuge coverage follows the selected cover difficulty.

The boiler house has a generator, breaker, switch, and wired lamp. The Spirit starts on the processing floor, leaves fresh footprints visible with an equipped UV light, and has physical ward and absolution locations. The playable case runs from the van, up and down the gallery stair, through the refuges and boiler controls, and to absolution in the mess hall.

Run the package checks with `python tools/verify_greywater_review.py`, `--gpu`, or `--case`. See the [construction record](construction.json) for fitted equipment and [verification record](checkpoint.json) for observed results.
