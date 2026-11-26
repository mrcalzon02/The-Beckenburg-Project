# Furnished House: matched lighting review

Reviewed source: encounter checkpoint `4afae307efe7b4903e14aca2d51d545ebcb0a21a`,
construction `first-principles-functional-furnishings-v1-981935462ed806a0`.
The original 45 props, corrected south-wall fridge, fixtures, geometry and
materials are unchanged. This review does not promote the fresh default House.

`HouseFinalLightingReview` renders the actual source with `SceneRenderer`,
CLASSIC pipeline, 640×400, 256MB Java heap. Every camera is checked against actual
traversability and its authored 3D room. The authored spawn looks toward the
northwest joins at upper and lower pitch. The 180 original runtime PNGs and 30
six-state contact sheets are under `development/captures/house-final-lighting`.
`cameras-and-states.csv` records exact positions, angles, states and PNG hashes.

Each camera has six matched states: all doors closed/all circuits on; closed/own
room circuit off with other circuits on; closed/breaker off; all doors open/own
circuit off; open/all circuits on; closed/breaker off with portable light. Source
running stays independent and unchanged. Van power is independent of House power.

All 30 sheets were visually inspected, covering every one of their 180 runtime
frames. Findings within these views:

- Foyer and dining closed leaves obscure the adjacent room; opening them reveals
  the neighboring room and expected light through the actual doorway. Their
  close door faces limit the visible area of the review; these are threshold
  checks rather than whole-room views.
- Sitting, kitchen, parents' and child's rooms lose their powered floor and
  furniture illumination when their own circuit is off. Portable light restores
  a localized pool without restoring the circuit.
- Lower and upper stair landings retain their real opening and rails. Light
  visible through that opening is distinguished from transmission through a deck.
- Bathroom wall tiles, radiator, toilet and basin respond to its circuit; the
  window and source plumbing remain visible in their actual places.
- Cellar passage, plant/laundry and store floor views show powered illumination
  disappearing on their circuit and on breaker loss. Their darker upper views
  are outside the main downward fixture spread. Doorway illumination changes
  with the actual leaves; portable light remains available during blackout.
- Garden fence and street/house exterior retain their silhouettes and physical
  ground in these views. They do not acquire a new indoor circuit.
- Van fixtures and the visible barrier stay powered during the House blackout.

No new visible join light leak, sky-through-ceiling or unsupported item was
identified in this particular view set. This is bounded visual evidence. It does
not certify every wall face, every corner, crouched/high-window viewpoints,
renderer parity or physical hardware FPS. Complete room compass coverage on the
latest furnishing layer, source/receiver join probes, final client/mission and
fresh default promotion remain separate unfinished work. Existing older-map
audit obligations remain unchanged.

Reproduce:

```powershell
javac --release 17 -encoding UTF-8 -cp 'build/beckenburg-game.jar;build/test' -d build/test src/test/java/org/beckenburg/HouseFinalLightingReview.java
java -Djava.awt.headless=true -Xmx256m -cp 'build/beckenburg-game.jar;build/test' org.beckenburg.HouseFinalLightingReview development/captures/house-final-lighting
```

An optional second argument selects comma-separated room IDs for a targeted
re-review. Raw compilation/render logs and source hashes accompany the checkpoint.
