# House construction authority

The construction candidate consumes one authored coordinate system. Start with
`HouseRebuildServices.source()`; its retained layers are documented in
[landscape-construction.md](landscape-construction.md),
[envelope-construction.md](envelope-construction.md),
[fitout-construction.md](fitout-construction.md),
[story-construction.md](story-construction.md) and
[heating-water-construction.md](heating-water-construction.md).

`services-definition.json` and `services-runtime-export.zip` are generated reports
of actual source nodes, geometry, texture sampling, fixture states and interactive
loose bodies. The editable `briar-rebuild-services-construction.blend` is built
from that export. Do not author a disconnected display model or maintain a second
coordinate list. `asset-use.csv` records actual source GLB hashes, triangle counts,
placements, support and retained metre scale/UVs for all39 static source props.

The editor-persisted world mesh removes actual terrain over the cellar. Its
resource/hash, swept terrain routes and negative flat-ground reproduction remain
linked in the landscape record. Both stair/deck openings and continuous player
ramps remain unchanged. Room floors, fixtures, geometry, collision, circuits,
acoustics, spawns and canonical van dock consume the same level definition.

Heating/water uses original fixture surfaces and closed generated service geometry;
under-deck distribution avoids the cellar and upper stair apertures. Source and
breaker state stay independent. Physical electrical controls and accepted ghost
trips are checked with real TCP peers against this exact candidate. Water and heat
are scene construction, without introducing an unrelated fluid/thermal simulator.

This remains an isolated candidate. Live House selection, saved-native migration,
final fixture/vent detail, full light-separation review, authored encounter and
complete location/client/mission/network/hardware acceptance remain open.
