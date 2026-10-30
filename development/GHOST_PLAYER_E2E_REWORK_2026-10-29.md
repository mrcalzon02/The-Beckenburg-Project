# Ghost-player end-to-end fairness contract (2026-10-29)

This is a continuing gameplay overhaul, not a declaration that all visual/manual acceptance is complete.

## Core invariants
- Human and AI ghosts share one authoritative GhostDomain energy, ability, cooldown, movement, attack and evidence dispatcher. A player never gets hunter coordinates, inventory, health, journals or map-team telemetry.
- Human ghosts perceive hunter **sound events**, including direction/intensity-limited footsteps, door activity, equipment, voice and rituals. There are no rendered hunter bodies in the ghost viewport. Mere room occupancy may charge ghost energy but must not enable target-aware abilities or expose a legal-action side channel.
- The van/extraction room remains unreachable by ordinary movement, DRIFT or Frenzy. Frenzy's temporary extraction seal restricts hunter retreat, never grants van entry.
- All authored hide sockets (indoor cupboards and outdoor shrub pockets) are physically inaccessible to the ghost controller, including sprint-through attempts. Hunter concealment wins against direct attacks and Frenzy contact. No special ghost-side hiding inventory, seat or detection packet.
- A randomized **5–45 second** host-authored action deadline starts each cycle, resetting after a genuine accepted haunting action, not moving/DRIFTing or rejected attempts. The host shows up to three legal choices to a human ghost; on expiry it randomly chooses a viable action, using the same dispatcher as AI/human requests. Illegal or warded actions cannot be forged into events.
- Frenzy is separate from ordinary forced haunting. It retains warning, rage, recovery, wards, attack distance/LOS and species-permitted attack families. AI and human controllers share those rules.
- Twins retain one case identity, one shared resource budget, one ritual outcome, and an AI sister with separately authored physical/sound origin. Companion activity must not double damage.

## Implemented in this iteration
1. Randomize each action deadline independently from 50 to 450 simulation ticks.
2. Randomize the forced fallback among eligible choices instead of always taking the first.
3. Separate actual audible hunter events from silent co-occupancy in the host's GhostDomain update. Keep occupancy-based energy recovery.
4. Block authored indoor/outdoor hiding pockets in physical spectral movement including sub-step sprint checks. Preserve existing van boundary.
5. Add mandatory regression tests for random cadence, legal fallback and hide/van boundaries.

## End-to-end acceptance still necessary
- Real graphical/three-to-six-peer host sessions: ghost joins, view spawns, mouse input, accessible legible action HUD, controls and errors; distinguish muted hearing caption from positional audio.
- Verify no confidential hunter data is sent through **any** ghost packet, including reconnection, and no quiet presence can be discovered through action lists, status indicators or failed attacks.
- Test room/door topology and stair movement on every authored site, every active hiding socket, outdoor pockets, 0%/100% hide density, and Frenzy closure/recovery.
- Exercise normal attacks and Frenzy with multiple hunters; distinguish sonic evidence from coordinate tracking, verify ward/hide/reaction counters and single-victim damage caps.
- Exercise timed action completion, deadline missed, repeated misses, exhausted energy, wards, AI takeover/reconnect, Twins sibling and round shutdown.
- Compare species-specific abilities and Ancient/Elder variants, including evidence fairness at difficulty levels and outcomes of every ritual.
- Review release regression and packaged Windows UI outputs before claiming playable end-to-end acceptance.

**Release policy:** Functionally broken ghost gameplay is a blocker. Cosmetic art refinements alone are not. Preserve a single main branch and remote CI tests; report CI outcome rather than assuming passing tests or a shipped release.
