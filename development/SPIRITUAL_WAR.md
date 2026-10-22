# Spiritual War — optional post-campaign mode foundation

**Status:** rules model only; not yet a selectable or playable network mode. This does not change the core investigation or campaign schedule.

## Unlock and isolation

The authoritative local campaign profile must have a generated story seed and have settled all 22 chapters (`storyCheckpoint() == CampaignStory.LENGTH`). Replaying older chapters does not revoke access. Free-play and multiplayer progression do not unlock this mode. Hosts must validate entitlement when a future Spiritual War lobby is created; never trust a client UI toggle. Do not award normal campaign currency or XP for arena combat.

## Match contract

Two teams, 1–12 Hunters against 1–12 Ghosts (maximum 24 participants). Last surviving team wins; no respawns. A **hard 30-minute limit** begins at match start, not in the lobby. At 00:00, if both teams survive, the result is a draw; eliminating the opposing team before the deadline wins immediately. Host-authoritative monotonic elapsed time determines the countdown, not client clocks. Ghosts begin in permanent hunt state; signature ghost abilities remain available, with their own existing cooldowns and telegraphs. Original and Ancient ghosts are eligible; Elder ghosts are banned. Ghost selection remains a player choice.

Hunters have a fixed offensive/defensive/healing kit: spiritual sword, defensive wards, and a limited health-pack supply. No investigative tools, scanning, rituals, evidence, or earned equipment bonuses are brought into arena play. Ghosts heal when they kill a Hunter, capped at maximum health. All combat stats here are **starting tuning constants**, not finalized balance.

## Code boundary

`org.beckenburg.SpiritualWar` provides the unlock predicate, eligible roster, per-player lives and inventory, wards, healing, damage resolution, elimination and winner calculation. It is deliberately headless and has no network transport or UI assumptions. Future integration must use the real authoritative combat/ability dispatcher, movement, collision/line-of-sight checks, server clock cooldowns, ward placement and player-controlled ghosts. The current `strike` entry point trusts a caller-supplied hit and **must not** be exposed directly to untrusted network packets. Match creation, sockets, maps, lobby entry, controls, replication and real-world playtesting are not yet delivered.

## Later integration acceptance

1. Locked entry point is visible but disabled until a legitimate 22nd-chapter win is durably saved.
2. Reconnect, hostile client requests and stale sessions cannot bypass host-side unlock, team caps, ghost restrictions or legal hits.
3. Four-sided actor control is not assumed: both teams must genuinely move/attack/use abilities from their distinct controllers.
4. Damage requires real geometry, proximity and world-state validation; a ward cannot prevent an unrelated hit indefinitely.
5. Survivorship, ghost kill-triggered healing and resource consumption remain stable under simultaneous hits/disconnects.
6. The 30-minute timeout prevents further actions, produces a draw if both sides survive, and cannot be extended by clients or reconnects.
7. A completed match returns players safely to Castle Beckenburg or lobby without modifying investigation outcomes or campaign progression.
