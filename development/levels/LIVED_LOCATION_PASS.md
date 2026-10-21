# Lived locations and usable environmental audio

Confirmed repository: https://github.com/mrcalzon02/The-Beckenburg-Project (main).
Local development now uses this standalone checkout. The previous HB-TTRPG-tools checkout is retained as history/reference and is no longer the upload destination. The migrated interaction UI is present; this checkout also includes the newer directional-character and startup fixes at 48e3dfc.

## Ordered delivery

1. Preserve the existing exact crosshair interaction. Give every audible fixture an identity in the playback mixer, so switching that source off cancels its pending and playing audio without cutting unrelated sounds. Player-started fixtures stay on until switched off; supernatural activation keeps its bounded event lifetime. Preserve host authority, acoustic projection, recorder observations and ghost privacy. Synthesize original, distinct telephone, bedside bell, old fire gong, modern alarm, radio melody, boom-box melody, water and washing-machine loops; package deterministic CC0 WAVs and provenance.
2. Finish and inspect the house before applying its method elsewhere: audit sink fronts against the supporting wall; complete its toilet/bath/shower fittings; add a laundry hamper, actual washing machine and laundry supplies; add supported personal belongings and readable location identity. Preserve both clean originals and separate worn variants. Reserve doors, windows, cupboards, stairs and operating approaches before decorating.
3. Give each location a stable street/address, building history and resident/use chronology. Express these through actual small props, repairs, paperwork, labels and room arrangements; do not put unexplained piles everywhere. Keep fiction distinct from architectural reference facts.
4. Apply the verified placement method to the farmhouse, including a real washroom and outdoor drying area. Fit school men's/women's facilities with privacy, circulation, distinct adult/younger-pupil fixtures and reachable handwashing. Integrate these as real architectural subspaces while retaining the established semantic room and networking contracts.
5. Capture actual game views of every changed room, check native dimensions/support/collision and sound start/stop over sockets, refresh sources/libraries where assets changed, then commit/push scoped checkpoints. Record omissions honestly and leave the next step reproducible.

## Audit at migration

Existing ordinary E interaction already toggles mounted phones, clocks, fire bells, radios and sinks, using the exact visible mesh on both client and host. The missing audio behavior is cancellation: anonymous short sound effects can keep playing after their fixture turns off. The existing five cues are original synthesized sounds, but radio and boom box share a short cue and player activation currently inherits the ghost's 20-second timer. The next code checkpoint addresses those specific gaps.

The house has a bathroom volume and base plumbing; the farm has a landing basin but no complete private washroom; the school has no designated pupil washrooms. Sink facing and new plumbing must be checked from actual source geometry. The current major furniture, ghost-event stands and difficulty-dependent cupboards are retained unless a measured layout correction requires moving them.

## Audio implementation checkpoint

Fixture sounds now travel with their individual source ID. The actual production mixer cancels both queued and playing voices when that fixture switches off; a per-source generation also prevents rapid off/on from resurrecting stale playback. Other radios, game effects and ghost warnings remain independent. Client captions retain direction. Player activation persists until stopped, whereas accepted ghost events expire after 20 seconds. Solo now uses the actual acoustic projection instead of discarding its pan/volume. Eight deterministic original loop WAVs and hashes are exported under event-audio with a CC0 license. Modern smoke-alarm and washer loops are included for the forthcoming fixture geometry; current rooms retain their physical fire gongs.

FixtureSoundscapeTest, HauntedFixturesTest, LooseClutterNetworkTest, InteractionNetworkTest and LanSoundTest passed in the new checkout; waveform and actual production-mixer checks include cancellation of one source while preserving another. Physical sound-card latency is not claimed measured.

## House geometry checkpoint

Ten original bathroom/laundry fixture families have native metre geometry, editable Blender sources, actual previews, separate clean/worn GLBs and CC0 metadata. Washer doors, shower pipes, curtain rings and wall stays are connected geometry, not floating accessories. The house basin faces into the room. Its bath faces north so the tap and shower supports attach toward the south wall. Mirror/shelf and towel positions were corrected after actual camera inspection to clear the tap and basin. The cellar laundry has a usable front-loading washer, independent audio, wall-supported supplies and two household hampers. The existing toilet is retained. Twenty-five focused checks and the explicit DomesticFitoutTest passed; final house captures are in previews/lived-location.

## Farm and school checkpoint

Farm landing washroom has private partitions and a functioning door, a recessed walk-in shower tray, toilet, inward-facing basin and supported toiletries. Washing is in the shed, with an outdoor pegged clothesline. The school has two separate upper-floor washrooms, each with three private toilet cubicles and child/adult basin sizes; one toilet per washroom is child-sized, and the boys facility has a screened urinal. Existing stairs and the broad west corridor remain clear. Same-zone privacy doors now block exact interaction sight while shut and open individually by ID. Child sink water and fixture targeting use the same uniform native scale as their mesh.

26/26 focused checks passed (verification/domestic-all-results.json). Actual runtime views are captured in previews/lived-location. The revised public packages contain 90 original clean/worn GLBs, editable Blender sources and thirteen synthesized WAV cues/loops, with license declarations and verified archive hashes. Publisher paths now target this standalone repository. New repository Pages configuration returned 404; deployment must be established and checked before claiming these updates are live.

Next: reusable suburban/woodland/urban enclosures and a separable van arrival road tile; mountainous variant for a future castle, without unlocking unfinished building plans. Preserve the actual movement and arrival contract. Add readable fictional location history and curate small supported narrative belongings.
