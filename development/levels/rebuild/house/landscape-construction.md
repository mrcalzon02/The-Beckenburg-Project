# House terrain and parcel construction

This candidate advances the original House pilot through actual editor terrain and parcel construction. `HouseRebuildLandscape` consumes the fitted source fixtures; the empty graybox and dispatched legacy House remain unchanged. Architectural rooms/stairs/deck openings, native fixture transforms and the canonical van placement are retained.

## Actual source and physical construction

`landscape-world` contains a separate reproducible16-bit editor heightmap, persisted adaptive topology and native terrain scene. The 6,072 runtime triangles include the actual punched cellar footprint. The current explicit resource hash is `30e12a7d833441b738fbe239e1da58218b1a6fddbafdd9ee21e722ec58974d58`. Sampled garden relief spans0.226m, with the house threshold and constructed street kept level and broad drainage/ground shoulders outside. No whole gameplay-zone ground material stamp is added.

An explicit terrain walking authority removes the old nominal outdoor deck rectangles and takes exterior feet/ground recovery from the same editor triangles that render and occlude light and small bodies. Indoor decks and smooth stairs retain their actual existing authority; the raised canonical van deck/ramp still wins at arrival. The frozen original pilot reproduces its0m walking/−0.65m visual terrain mismatch (DEV-300).

The physical garden boundary has a narrow west side passage, rear service garden, changing north/east parcel returns and the real constructed street apron. Closed garden-fence sections have matching colliders; their feet follow the native terrain. This is a private garden parcel, so straight constructed fence runs remain where appropriate. The wider site still needs final visual landscape refinement before S04 acceptance; a varied fence alone does not close that gate.

Canonical neighbouring residences already rendered inside the old arrival reservation. Their private-parcel fences and actual building colliders now come from the shared arrival contract. The former approach anchor was inside one residence (DEV-301); the new anchor is19.8/24 and the garden transition is24/20.5, with a service path around the east side of the house. Neighbour buildings remain sealed context, not new search rooms. The source programme remains below40 accessible spaces; no timed round tests are used.

Paths blend the existing woodland grass/track source surfaces in world coordinates with feathered worn edges. Their appearance is independent of terrain leaf boundaries. Source GLB pivots, UVs, geometry and textures are unchanged. The former hard triangle material switches were replaced during this construction review.

## Verification and visual review

The packaged physical audit checks1,364 exact triangle/feet samples, real cellar aperture, absence of duplicate rendered outdoor decks, canonical neighbour-solid negative, original missing-solid reproduction, all parcel seams in standing/crouched/small-object sweeps and reachable outdoor anchors. All15 zones and required functional sockets remain swept reachable at50,229 positions. The focused11-body run also covers existing House/Farmhouse/School architecture, utilities, surfaces, renderer cache, physical lighting and Castle object/window regressions. Exact logs and scopes are under `verification/house-landscape-2026-11-06`; they are not complete mission/LAN/hardware acceptance.

Four useful actual runtime standing/crouched views include the actual canonical van and ramp. Two rejected initial viewpoints inside neighbouring buildings are preserved. The high runtime overview is too dark to approve landscape composition and is explicitly rejected. The separately labeled daylight native overview exposes actual runtime geometry; native parity checks every exported triangle/UV, packed images, portal count,15spawns and source sockets without resaving the delivered `.blend`. Runtime lighting and native inspection lighting remain separate evidence.

## Reproduction and next gate

Run `HouseRebuildLandscapeAuthor`, package the game and run `HouseRebuildPhysicsTest` plus `HouseRebuildLandscapeTest development/captures/house-landscape`. Export through `LevelSceneSource HOUSE_REBUILD_LANDSCAPE`; generate `briar-rebuild-landscape` with the shared Blender builder and `--output-suffix -construction --no-render`. Reopen/verify using `verify_house_fixture_native.py -- --landscape`.

Next: finish terrain/landscape visual refinement and architectural envelope/roof joins before final furniture, plant/distribution, story and mission promotion. The repeated room gables and exterior finishes visible in the native overview remain construction work, not accepted final architecture. Preserve this concrete pilot, then propagate the verified measured-fit/terrain-authority method through the remaining stage batches. Castle implementation has separately merged at3edcbb1e with actual345/0; no further Castle feature expansion is included here.
