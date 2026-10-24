# Mandatory remote-site electrical source and ghost breaker behavior

User requirement, 23 October 2026: every remote map must contain a site-appropriate
generator so electrical ghost activity and breaker-tripping events remain meaningful.
Remote geography does not remove the electrical interaction system.

- Place the actual generator, distribution board/main breaker, service wiring and
  named electrical loads in authored map coordinates. Audit existing generator
  models first; a propulsion engine or an ordinary electrical cabinet is not
  automatically the site generator.
- Keep generator availability/running state separate from the breaker state.
  A running source must **not** silently restore a tripped breaker. Do not create
  a second circuit state that bypasses ordinary switches, ghost events or saves.
- Route electrical ghost activity through the same host-authoritative breaker
  trip/reset behavior used by other maps. Respect existing ghost-specific rules;
  the map requirement does not invent evidence or force every ghost to trip power.
- Connected fixtures must actually go dark, including dock/string/service lights.
  Door/switch approaches and the generator/breaker remain reachable in darkness.
  Use actual physical mounting, cables, circuit IDs and a readable service layout.
- Oil lanterns, candles, campfires and battery equipment retain their existing
  independent power rules. An electrical breaker must not extinguish an oil flame
  or replenish/disable unrelated batteries.
- Verify ordinary switching, host ghost trip, allowed reset, camera/evidence
  behavior, CPU/GPU lighting parity, save/reconnect and LAN authority. Record all
  source/state/placement/lighting failures in the shared defect ledger.

[Per-map power plans](../levels/power-plans) cover WLD-01 through WLD-18. The
cemetery/mortuary variant shares a planned service source rather than duplicating
the cemetery. These are mandatory requirements, not claims that locked maps have
finished runtime circuits or that a newly listed generator model already exists.
