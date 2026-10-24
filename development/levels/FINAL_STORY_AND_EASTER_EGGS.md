# Final story dressing and authored Easter eggs

User-directed phases, 24 October 2026. Apply to every map after terrain,
architecture, major facilities, routes, lighting/circuits and hiding allowance pass
construction gates. These phases do not replace technical acceptance.

## Story detail pass

Give each space a short account of its normal use, interruption and surviving
traces. Place minor lore documents, personal knickknacks, maintenance leftovers,
wear, rubbish and small loose physics objects where that account calls for them.
Choose legible clusters instead of uniform random scatter. Record lore IDs,
source assets, actual support surfaces, movable/static status, pickup mass,
collision, examination/photo behavior and host ownership. Follow
[the loose-object workflow](LOOSE_OBJECTS_WORKFLOW.md).
Recheck doors, rituals, evidence sightlines, hiding pockets and route widths after
scatter. Loose clutter must not jam progression or become extra hiding sockets.
Native decorative meshes alone do not count as loose physics.

## Cultural-reference/Easter-egg pass

Select strong horror-film/location associations that fit each map. Author original
props and sound cues, observation/time/round predicates, state transitions,
anti-spam/cooldown, reset and multiplayer behavior. References are optional
encounters, separate from ghost identity, evidence and case scoring.

Each encounter packet records context, coordinates, trigger/occlusion, elapsed-time
and round eligibility, owner/witnesses, visual/audio envelope, exit/abort/replay,
physical safety, source assets, evidence negatives, host/reconnect/reset checks
and actual visual review. A design packet is not implemented behavior.

### Hollow Creek candidate: watcher at the dock

Status: **designed, not implemented**. Context: campsite, lake and hockey-mask horror
imagery. Use an original aged hockey mask and original ominous cue. Reserve water
beyond the dock tip, outside walkable/collision space.

Proposed trigger: an eligible round, investigator near the dock tip, powered
flashlight aimed at the water patch, uninterrupted unobstructed observation for
2.5 seconds. Host validates body/aim/light and elapsed ticks. Brief target loss
resets the initial dwell; normal flashlight battery/drain rules continue.

Host states: dormant → observed → rising (bubbles and localized ominous music) →
floating (small buoyant wobble) → sinking → spent. The first qualified observer
owns the sequence; nearby players see the same world object through normal
occlusion. When that observer looks away beyond a short grace interval, it sinks
slowly. Looking back does not spam another rise. Disconnect, death, mission end
or timeout also causes a clean sink/reset. One activation per eligible round;
author the final round/time predicate during implementation.

The mask is not evidence, an item reward or a compulsory objective. Photos must
not generate false evidence credit. Bubbles, mask/water sorting, submersion,
transparency, beam visibility, sound attenuation, CPU/GPU parity, host/LAN/reconnect
and looking-away negatives require actual runtime checks. Finish water and
containment integration first.
