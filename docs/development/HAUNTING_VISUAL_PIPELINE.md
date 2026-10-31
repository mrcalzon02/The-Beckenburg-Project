# Haunting visuals — organic PSX post-process

## What was broken
The earlier `GhostHauntingChoreography.paint` authored several full-width periodic
scan rows (`y % 7`) and rectangular bands against the screen; the fallback
`GhostHauntingFolio.paint` used 2×2 repeated color cells. In first-person
these resembled bars pasted over a player's face rather than spatially credible
haunting phenomena.

## Runtime correction
`GhostHauntingFolio.paint` still accepts host-authorized events, local
room, current 10-Hz tick and final RGB game framebuffer. It now chooses
only the strongest currently active local effect and invokes the shared
`HauntingScreenFx` module once per frame. The effect is purely visual:
never drives ghost location, evidence, damage, species revelation, multiplayer
authority, input mapping or ritual progress.

`HauntingScreenFx` consumes real composited world pixels after the PSX
rasterizer and graphics options. It uses a deterministic time-interpolated
organic noise field, peripheral spectral falloff, foggy tendrils, occasional
electric dust, a small lens shift and optional edge chromatic drift.
No actual line-row flicker or HUD bars are authored. Most of the central
aim/readout region stays legible; the darkest action is concentrated near the
periphery. Every visual fades in and out across its authoritative event
duration. On local-room mismatch or after expiry, pixels remain untouched.

Distinct environmental treatments:
- VEIL: cold drifting spectral mist
- SHADOW: irregular, near-black creeping peripheral presence
- STATIC: broken electrical motes and local distortion, not horizontal bars
- CHILL: pale frost-blue atmosphere and branching highlighted edges
- PULSE: a faint expanding pressure ring
- DISTORT: softly bent world geometry around the periphery

Fourteen choreography motifs also use this underlying material system rather
than replacing it with rectangles: lamp convulsion, radio glitch, bent
perspective, doorframe watcher, shroud, flicker steps, ice bloom, edge watcher,
impact ring, grasping edges, twins, blade sweep, plague miasma and nothic iris.
These effects do **not** represent or identify a new world-space ghost actor;
the apparition's real depth-tested sprite/mesh is handled separately.

## Budget and memory
One reusable thread-local unmodified RGB scratch frame and a low-frequency
noise grid are allocated to the maximum previously rendered size. All
interpolation is made on 2×2 PSX-native blocks before writing the full RGB
framebuffer. No per-frame `pixels.clone()` allocations or per-event full-screen
stacking. Visual performance readings are diagnostics, not release gates.

## Review workflow
`.github/workflows/haunting-visual-review.yml` compiles real game resources
and runs advisory effect comparisons. `HauntingVisualReview` renders an
actual investigation scene once with the game `SceneRenderer` and applies the
exact runtime `GhostHauntingFolio.paint` path to 20 selected haunted events.
It uploads individual PNG captures and an indexed 4-column contact sheet
under `beckenburg-haunting-fx-art-review`. Screenshot style/quality checks
are **never release-blocking**. The core game build must still compile and
start; playback and multiplayer round checks are handled by their existing
independent acceptance workflows.

Build with Java 17 and generate diagnostic captures locally:
```
java -Djava.awt.headless=true -cp build/classes:build/test org.beckenburg.HauntingVisualReview build/haunting-visual-review
```

Acceptance in a live player view still requires examining the contact sheet
and walking through a few real live events. No automatic test may claim
"looks terrifying" or verify taste from pixel hashes.
