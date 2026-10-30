# PSX NPC-AI architecture reference — Beckenburg adoption review

**Reference:** user-supplied 17-page `PSX NPC-AI-1.pdf` (30 October 2026).
This is an engineering adaptation of its recommendations, **not** an instruction
to reproduce original PlayStation hardware limitations in a Java 17 Windows game.

## First dependency: measured House door-opening profiles

The dedicated GitHub Actions workflow
`.github/workflows/door-opening-profiler.yml` builds the actual game on
Windows/Java 17, then runs `DoorOpeningFrameProfile` in separate JVMs:
- **CPU software:** real authored House, real `SceneRenderer.drawPractice`,
  baseline frames, 16 alternating actual door transitions, and swing frames.
- **GPU submission recorder:** real world-buffer preparation and explicit
  resident resource revision accounting. A recorder is **not** OpenGL timing.
- Each produces baseline/toggle/swing/recovery p50, p95, p99 CSV and
  peak shadow rays, invalidated cells, scene generations, upload bytes, GC and
  heap snapshots. They are diagnostic; no subjective FPS/art release gate.
- Installed Windows hardware GPU timing, visual inspection and real-player
  input still require separate acceptance. Do not claim those from the
  CPU or recording backends.

**Implementation dependency:** inspect the actual CSV and profiler results
before changing the AI update cadence or doing an unmeasured rewrite. A door
hitch with zero AI actors is by definition not repaired by throttling AI.

## Directly applicable recommendations (priority order)

### P0 — Preserve simulation correctness while measuring
1. Profile the host's existing **10 Hz** authoritative tick and the solo
   practice simulation separately. Time AI decisions, route queries, sensory
   work, physical collision and animation/image preparation. Report bounded
   p50/p95/p99, allocation/GC and actor counts; diagnostic only.
2. Keep one authoritative tick number and **no AI-side wall-clock loops**.
   Rendering may skip frames; commands, ghost deadlines, player collision,
   ritual state and network snapshots cannot be skipped arbitrarily.
3. Stabilize door-change handling: cache room connectivity and each
   actor's route until the destination/topology changes. Invalidate using
   portal revisions rather than rebuilding an entire graph or restarting all
   actors on every camera frame.

### P1 — Low-risk navigation and sensing
4. **Reuse authored room/portal graphs**. `CampaignAlly.route` uses a
   breadth-first traversal with fresh arrays/queue/path allocations whenever
   an ally changes rooms. `LanHost.campaignBotInput` builds a new BFS every
   input call when rooms differ; `LanGhostPursuit.route` does another
   bounded search; all are over only ~15 rooms, so first measure whether
   object churn is material. A cached next-hop table per topology revision
   is a candidate, not a justification for a new unbounded A* engine.
5. **Bound expensive decisions**, not movement or responsiveness.
   Use an event/goal-dirty flag and per-actor deterministic time slots for
   patrol target selection. Hearing, being attacked, door collision,
   spectator input and ritual/hunt reactions must still be delivered
   immediately through their existing authoritative paths.
6. **Sound propagation cannot become a simple flat distance check.**
   `LanAcoustics` already traverses physical portals, closed doors,
   materials, floors and weather noise; the ghost excludes the extraction
   van. Cache a room-to-room attenuation matrix by door/window topology
   revision if its Dijkstra/priority-queue allocations show up in profiling.
   Approximate the sound source position without leaking exact hunter state.
7. **Persist navigation intent and use tolerance/hysteresis** at door
   thresholds, nearby goal nodes and search/alert states. Use small
   squared-distance comparisons where they simplify hot checks, while
   preserving actual `HouseMap.resolveMove` swept collision.
8. **Coalesce simultaneous route requests** with a bounded, fair host
   scheduler only if measured demand warrants it. An AI can keep its
   previously safe target for a short, explicit interval; don't produce
   starvation, fake teleportation or delayed emergency decisions.

### P2 — Editor and authoring accelerators, conditional
9. Export prevalidated portal/waypoint adjacency and optional cover/ambush
   tags *for existing authored sites*. Never blindly cap nodes to four
   connections: preserve all real door/stair links or insert a validated
   intermediate node.
10. Precompute static *potential* room visibility/sector interest for
    **render/candidate rejection only**. Dynamic doors, transparent windows,
    vertical floors, flashlight/evidence interactions and shared LAN state
    still need authoritative runtime checks.
11. Preload known animation/sprite variants at scene preparation so an
    NPC switching to alert/hurt doesn't synchronously import a 3D model or
    decode images on the host tick/EDT. Measure memory tradeoffs first.
12. Use presentation-only staggered bark/footstep animation offsets and
    natural telegraphing **where fair**; do not alter ghost identity,
    evidence entitlement, attack rules or cadence merely to hide slow AI.

## Parts not appropriate to import literally

- The PDF's MIPS/GTE assembly, 12-bit fixed-point arithmetic, 360-entry
  sine/cosine LUT, PSX 1 KB scratchpad, 2 MB RAM ledger, GPU VRAM data
  tricks, manual unrolling, and fixed 4–8 NPC slots are **hardware-specific**,
  not sensible Java 17 optimizations. Modern JVM optimizations must be
  measured rather than cargo-culted.
- Its recommendation to turn off far-away brains or only activate full
  combat when inside the player's field of view would create exploitable
  multiplayer/gameplay discontinuities. Instead reduce *presentation*,
  optional cosmetic inspection and redundant planning while retaining
  authoritative sound, collision, deadlines and simulation continuity.
- Its proximity "Spidey Sense", gaze-triggered aggression and exact
  omniscience cheats conflict with Beckenburg's intended **auditory ghost
  awareness** and fairness. Ghosts must not acquire hunter positions from
  visibility, stare direction, hidden evidence, or van location.
- Node teleportation, temporary collision disabling and a jump to the
  nearest node are **not acceptable safety recovery defaults**. Stuck
  actors should diagnose, choose a valid alternative portal and replan
  without passing through walls/floors or hiding pockets.
- Hazard-zone shortcuts must not bypass real line-of-sight and collision
  checks for rituals, attacks or evidence. NPCs/ghosts cannot enter the
  safe van where forbidden, and player ghosts cannot enter hiding spaces.
- The PDF's absolute bans on dynamic allocations, trigonometry, A*,
  steering, and complex sound are historical design cautions, not proof
  of current bottlenecks on JVMs. Prefer measured bounded work.

## Existing implementation facts to preserve

- `LanHost` owns a scheduled 100 ms tick; multiplayer authority and ghost
  forced-action cadence (5–45 s) remain host-owned.
- `CampaignAlly` already uses portal BFS and the same swept move/elevation
  resolver as the player; `GhostPracticeHunterAi` uses audible cue events
  and room-level suspicions, not true ghost identities.
- `LanGhostPursuit` uses finite recent sound-memory and coarse location
  rather than continuous private hunter tracking; `LanGhostSpatial`
  forbids the extraction van and hiding pockets.
- `TwinCompanionAi` is a simulated second actor with sound and movement
  state; it may not teleport, cross solid walls or invent secondary damage.
- `LanAcoustics` charges attenuation through authored doors and materials,
  including weather noise, instead of a naive omnidirectional radius.

## Acceptance and documentation

Build Java 17, run a controlled authored House/Castle navigation case and
door-toggle case with **real** ally, practice hunter, AI ghost and Twin
actor on separate scenarios. Include vanilla baseline and optimized
rerun with identical seeded intent, player inputs and portal transitions.
Validate legal rooms, no wall/floor phasing, no safe-van or hiding-space
entry by ghosts, unchanged evidence/ritual outcomes, no omniscient hunt,
consistent host and connected-client snapshots, and bounded path work.

Write per-scenario summaries for decision/route/sensory/collision time,
path request backlog age, allocation/GC, active actor counts and p99
simulation-tick time. Flag quality/performance anomalies as **diagnostic**;
only objectively nonfunctional build, startup, essential gameplay or
multiplayer/update failures are release-blocking.

**Status:** adoption design prepared; no claim of AI refactor implementation,
installed Windows playtest, or actual GPU FPS improvement.
