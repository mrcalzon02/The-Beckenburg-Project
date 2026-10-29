# The Beckenburg Project — Owned-First Audio Audit and Implementation Plan

**Working inventory:** 2026-10-29. **Primary repository:** `mrcalzon02/The-Beckenburg-Project`, branch `main`.
**Status:** source/Drive catalog audit started; candidate selection is **not** proof of playback or rights to redistribute. Do not call the audit or the soundscape complete until runtime and listening gates pass.

## Non-negotiable sourcing order

1. **Existing Beckenburg assets and sound engines:** retain approved and correctly routed audio.
2. **Owner's Google Drive assets:** search its Beckenburg master index, Sounds library, NOX archives, original compositions, sound-source/editing sessions, license paperwork and other owner-provided recordings.
3. **Owner's other original GitHub repositories:** search actual packaged audio, source generators, manifests and provenance before deciding anything is missing. Prefer shared-source references to committing duplicate binaries. Favor sound appropriate to the specific event over availability alone.
4. **External, source-page-verified CC0 only for remaining gaps.** Search/download externally only after stages 1–3 have recorded an unmet cue and an explicit rejected/absent owner-owned candidate. Keep creator, exact license URL/statement, original and optimized SHA-256, and audio audition outcome. Never treat a site search result or an unverified "free" label as a CC0 grant.
5. If CC0 cannot satisfy a required signature sound, create an **original** effect or recording rather than quietly importing CC BY, NC, SA, proprietary or unknown-rights media.

**Rights classes are distinct:** owner-authored, licensed commercial game-use, CC0, third-party with separate license, and unknown. An asset's presence on Drive does not establish public redistribution rights. Owner music authorized for first-party products is **not** thereby CC0. Existing Beckenburg NOX files have been called CC0 in source comments, but the Drive master asset index marks the upstream NOX license for review: reconcile actual pack license/receipt and exact curated sample provenance before expanding or publishing additional NOX content. Do **not** mistake the purchased PSX model pack license for NOX audio rights.

## Stage A — Existing game audio: inventory and runtime-reference map

Inspect every version-controlled `.wav`, `.ogg`, `.mp3`, relevant archive and procedural source, including `assets/runtime/music`, `assets/runtime/sfx`, `assets/runtime/weather`, `build.sh`, `build.cmd`, and installers. Build an auto-generated `docs/audio/beckenburg-audio-manifest.csv` (path, source/hash, format/rate, category, bus, runtime cue(s), scene, license, test status). The inventory must be from the **current main tree**, not a historic snapshot or a stale Drive copy.

Initial source evidence in **current `main`**:
- `src/main/java/org/beckenburg/LanSound.java`: finite captioned cues and host-authoritative room/distance/door/weather projection. Preserve ghost-position privacy; packet contains only quantized pan/gain.
- `LanAudio.java`: off-EDT 16 kHz stereo PCM sound mixer, 32-item bounded queue, up to 12 voices, priority admission, source-specific fixture gate/cancel, procedural fallback and recorded cue playback.
- `RecordedSfx.java`: curated classpath `/org/beckenburg/sfx/*.wav`; supports multi-variant horror stings, source conversion, short clip bounds and procedural fallback. `SoundCoverageTest` currently names at least nine required packaged cues: `PICKUP`, `DEVICE_ON`, `DEVICE_OFF`, `DROP`, `SCANNER`, `ELECTRIC_HUM`, `EQUIPMENT`, `DOTS_MOTOR`, `MONITOR_HUM`.
- `SoundtrackAudio.java`: distinct CASTLE/AMBIENT/RITUAL/HUNT score states, independent `music_volume`, and **six logical authored scores**: `castle-candlelit-shadows`, `ambient-cold-unresolved-drone`, `ambient-sombra-profunda`, `ritual-of-ash`, `ritual-final-strike`, `hunt-final-surge`. `build.sh` explicitly packages eight ZIP or ZIP-part files corresponding to these six scores; `SoundtrackAudioTest` checks extraction/decoding/selection, not audible Windows playback.
- `FixtureSoundscape.java` and `FixtureSoundscapeTest`: nine procedural, phase-continuous device sound categories: telephone, bedside bell, fire gong, smoke alarm, radio, boom box, running water, washer, TV static. Validate that active fixtures start/stop with their actual world state.
- `GhostHauntingChoreography.java`: authored sound beats for minor/moderate/dangerous/signature hauntings and named elder effects; `GameSfx.java` provides UI/solo events. Preserve the reveal rule: only signature events expose a ghost's identity-specific manifestation; generic effects must not accidentally reveal it.
- `build.sh` packages `assets/runtime/weather/audio_00`–`audio_05` plus other weather packs. Check which are genuinely heard in Castle and each site; packaging alone is not an event hook.
- Existing tests: `SoundCoverageTest`, `SoundtrackAudioTest`, `FixtureSoundscapeTest`. Run the full project Java 17 test/build path later; **no hardware test or full build was executed during this document's initial connector audit**.

**Stage A exit evidence:** exact asset list with hashes, cue-to-trigger/callsite map, JAR/installer inclusion map and 1) actual 2) synthesized fallback 3) present-but-unused 4) missing 5) rights hold statuses. Confirm opening/closing scenes, field enter/exit, map change and game shutdown do not leak sound or resurrect stale loops. Show separate deterministic tests and real playback evidence.

## Stage B — Google Drive source inventory, rights and purpose

Source index: [BECKENBURG — MASTER ASSET INDEX](https://docs.google.com/document/d/18G144Qd0-L5rL7NdqxMykqkXaBHtu8-ytr_yYPol6rI/edit); [Beckenburg asset root](https://drive.google.com/drive/folders/1IW8c_FO1YkkLkVWQyRqhKmRBd1S5Ql8R).

**Observed source folders and first candidates** (listing is not an audition):
- [Owner's Sounds library](https://drive.google.com/drive/folders/15_nLObkdUim_51DMVtxHEdqfHQLUdnfA): short `Klick`, `Snick`, `Snap`, `Bash`, `Krunk`, `Curlunk`, `Thrumthump`, `Lightning-crackle`, `Thundering`, plus `.aup3` original/editing sessions (e.g. Book snap, Book flip, Door close). The `.aup3` files are production sources, not distributable playback clips until exported/inspected.
- [NOX Nature Essentials](https://drive.google.com/drive/folders/1rIDSIU3X67OtK93XtDUDFIyRRTR_wcY1): 18 listed WAVs for wind/forest, fire, sea, cicadas/night, rain, waterfall/river/stream, cave depths/drips. **Ambience candidates only** until audio and license audit.
- [NOX Footsteps Essentials](https://drive.google.com/drive/folders/1mcG8-nzzm9eIrr8kOM76yfZxy3cSeAW5): wood, metal, water, leaves, tile, snow, rock, gravel, sand, mud, grass and dirty ground subfolders. Compare to existing carpet/wood/stone/general cues; do not add redundant recordings blindly.
- [NOX Electromagnetic](https://drive.google.com/drive/folders/1R8f6z3yXU2OxiHjceKFcT2cH48qWKlRe): 72 directly listed WAVs, including computer idle/load, electric box and other electronic sources; potential van monitor/evidence device beds.
- [NOX Iceland Flows](https://drive.google.com/drive/folders/1CUVgEgR3nzeVzK876SH6uVYz3DzoZLbH): 17 water/shore/stream WAVs plus a Pictures folder. Only applicable to sites whose visible geography actually supports the audio.
- [NOX Voices Essentials](https://drive.google.com/drive/folders/1A8pkJPJGlYSW-DCfQNn1DtNZvVwyATyS): male/female subfolders; never auto-relabel a generic human sample as a specific ghost, a cast actor, or evidence.
- [Drive Audio archive](https://drive.google.com/drive/folders/1_W4GQhZ2kBpTT-yrDN70CZoiMDddNudY): music/voice subfolders currently contain unrelated NUTBUSTER recordings; the owner library is broad, not automatically Beckenburg-appropriate.
- The master index records the archived 1,036,374,981-byte `Essentials_Series_NOX_SOUND.zip` as AS-001. Do not unpack/commit the entire archive into this game's root or publicly release an unlicensed source pack.

For each candidate: preserve original URL, actual bytes/hash, duration, channels, sample rate, peak/RMS/loudness, noise/clipping/trim/loop seams, appropriate room/material/event, license evidence and decision KEEP / PROCESS / REJECT / HOLD. Prefer original authored Foley and atmospheric texture that fit Beckenburg's PSX horror aesthetic. Differentiate background environmental beds from score/music.

## Stage C — Cross-repository owned-material search (mandatory BEFORE public CC0)

The owner GitHub repository list was enumerated, including ReverieVR, NUTBUSTER-R, TheMechanist, Catastrophic-Diplomacy, The-Malefic-Veil, Magenheim, Infinite-Domain, Jewel_Brook and HB-TTRPG-tools. **Only identified candidates below have so far been source-checked; a full recursive binary/hash sweep remains outstanding.**

1. **[ReverieVR original music manifest](https://github.com/mrcalzon02/ReverieVR/blob/main/music/source/manifest.json)**: eleven checksum-recorded, owner-authorized MP3 masters (rights: `LicenseRef-Owner-Original-Music`, NOT CC0). First audition candidates by *title only*, not judged fit: `Ballroom of the Dead`, `World Below`, `Pressure Under Europa`, `Nowhere King`, `End Of Deep`, `Deep Embrace`. Other tracks are explicitly assigned to different native games/lobbies; avoid retargeting automatically. Check first-party cross-project authorization and whether Beckenburg's six existing scores already fulfill the job.
2. **[ReverieVR Home atmosphere](https://github.com/mrcalzon02/ReverieVR/blob/main/docs/project/HOME_ENVIRONMENT_ATMOSPHERE.md)**: owner-generated PCM for indoor HVAC, wind/leaves/birds and surf/gulls. Its Android `HomeAmbientAudio` is a design/reference for a Java 17 compatible original synthesis port, not a drop-in Java desktop library. Its listed Freesound tracks are *external CC0 candidates only*, not packaged files, so do **not** count them as owned inventory.
3. **[NUTBUSTER-R audio catalog](https://github.com/mrcalzon02/NUTBUSTER-R/blob/main/assets/audio/catalog.json)**: 41 locally packaged assets including **17 short source-hashed effects** from the shared Drive Sounds library; `Klick`, `Snick`, `Snap`, `Bash`, `Krunk`, `Curlunk`, `Thrumthump`, `Lightning-crackle`, `Thundering` and others can be *auditioned* for UI, physical impacts, mechanisms and storm. The announcer/comedy voice lines and `Bubbly Whistle` are not suitable default horror replacements. The audio pipeline documents owner in-game-use authorization of the shared clips, not a universal CC0 grant.
4. **[TheMechanist sound/voice practices](https://github.com/mrcalzon02/TheMechanist)**: packaged positional WAVs, mechanical weapon/impact families and bounded 12-voice playback. Screen for neutral metallic impacts or interaction Foley; don't inject sci-fi gunfire into quiet investigations simply to reuse it. File-level hash/rights scan pending.
5. **[Catastrophic-Diplomacy audio documentation](https://github.com/mrcalzon02/Catastrophic-Diplomacy/blob/main/docs/AUDIO_SYSTEM.md)**: original synthesized energy effects, plus Drive-sourced metal clicks/impacts. Its two tracks `Final Surge (hunted)` and `Cold Unresolved Drone` match already-known Beckenburg score families: **deduplicate by source hash before importing**. Catastrophic's source-Drive redistribution rights are separately unresolved.
6. **[The-Malefic-Veil](https://github.com/mrcalzon02/The-Malefic-Veil)** and remaining original projects: check original sources and manifests for useful sound, but lack of a README audio statement is **not** proof no assets exist. Malefic Veil's own README calls final sound future work.
7. [HB-TTRPG-tools](https://github.com/mrcalzon02/HB-TTRPG-tools): check the older Beckenburg sound prototype and Simple Sound Manager for original synthesis, stems and unported cues, but **current Beckenburg repo main is the authoritative game**. Prevent two divergent runtime sound implementations.

**Stage C exit:** cross-project matrix listing candidate, exact upstream path, original ownership/license, checksum, rights scope, Beckenburg purpose, audio quality, redundant/incompatible flags and decision. No destructive move of other project originals; do not duplicate source blobs unless a verified Beckenburg shipping package truly requires curated bytes.

## Stage D — Gap analysis and external CC0 exception

Only after A–C, generate `docs/audio/UNFILLED_CUES.md` from semantic event coverage; an event is filled only by a triggered effect, not by a file with a plausible name. For each gap record why existing Beckenburg/Drive/cross-repo options were absent, redundant, unsuitable or not cleared.

Possible future CC0 catalogs (already referenced by other projects, **not yet searched/imported for Beckenburg**): Kenney audio categories, OpenGameArt creator-authored CC0 effects, Freesound **individual clips carrying explicit CC0 declarations**. Verify each source's *current* individual license on original pages at acquisition; no blanket claims for sound packs, mirrors, royalties-free or unverified uploads. Acquire a minimal curated subset; avoid copyright-sensitive speech or identifiable movie/game audio.

## Stage E — Unified soundscape runtime (after coverage map)

Preserve existing Java 17 APIs, host event authority, graphics performance and procedural fallbacks. Add separate controls/lifecycles for **MUSIC, ENVIRONMENT, SFX, VOICE** (and optionally paranormal category) without overlapping continuously playing sources. Distinguish long-running low-gain environmental loops, physical positional emitters, one-shot Foley, musical score and critical gameplay warnings.

- Provide room/zone/time/weather selection: outside rain/wind/trees/water; inside room tone, distant creak/pipes, appliances/van computer; location/biome correctness.
- Maintain an ambient bed until weather/location/scene state changes. Crossfade/ramp instead of restarting on each render/update. Do not generate/reload full files every frame; stream or reuse bounded decoded chunks. No graphics thread, Swing EDT or host tick audio I/O.
- Prioritize attack, evidence and interaction cues above low-priority footfalls/ambient; bounded voices and queues, avoid clipping, appropriate ducking and audio device failure handling.
- Make physically audible events respect real room/door material and distance, preserve hunter/ghost information secrecy, and keep useful screen captions when muted.
- Low-level ambience **is not continuous horror stings**. Music may become sparse/silent during investigations to preserve tension; a healthy soundscape can include deliberate quiet.

## Stage F — Coverage matrix and scene integration

Each row must have **source → owned-or-CC0 rights → runtime event → verified sound output → listener test**:

| Scene/state | Required audio coverage |
| --- | --- |
| Title/menu/settings/shop | Theme/low ambience, navigation select/back, purchases/equipment preview, open/close, controlled independent volumes |
| Castle hub | Castle score, stone/wood footsteps, doors, hub room tone, distant exterior weather, workstations/quartermaster/mission start |
| Van/mission staging | Low electrical ventilation, monitor hum, radio/static, racks, gear clicks, van door, exterior weather, in-van/field attenuation |
| House/mansion/farmhouse | Room-type beds, timber movement, wind, rain, pipes, clock/electrical hum, material-matched footsteps, doors/drawers |
| Church/academy/school/college | Environment-correct room acoustics, bell/electrical/large hallway textures, distinct reverberation |
| Asylum/prison | Ventilation/pipes, secure doors, hard-surface footsteps, room-specific mechanics and atmospheric quiet |
| Outdoor/camp/canyon/caves/wilderness | Actual authored-map weather and site sound (forest/water/wind/fire/cave drips) only where appropriate |
| Equipment/evidence | Each finished detector, DOTS, EMF, scan, thermal/camera, incense/salt/ward, sound recorder and journal actions. Do not invent success confirmation |
| Ghost and ritual | Minor/moderate/dangerous generic haunting, signature/elder identity, hunt pressure and spatial telegraph, ritual initiation/progress/outcome |
| Mission transition & results | Intro/arrival, investigation begin, extraction, success/failure; music and ambience stop/transition cleanly |
| Solo and LAN | Correct positional hearing, voice-radio separation, 1–6 hunter/ghost privacy, death/disconnect/resume transitions |

Do not add sounds to incomplete or nonexistent devices merely to fake equipment implementation. Each *implemented* user interaction gets coherent audio or a recorded, intentional-silence exemption.

## Stage G — Quality gates, packaging and release

1. **Static tests:** mandatory resource files present, source SHA-256, decodability, provenance, source/trigger coverage, no unused copies, bounded duration and channel formats, captions; optional asset fallbacks deliberate.
2. **Behavior tests:** change of map/room/weather; begin/stop song and ambience; active/inactive fixtures; death, pause, menu, reconnect and shutdown; priority/ducking/mute isolation; no host-origin hidden-ghost leakage.
3. **Load/performance:** representative castle and problematic house sustained playback, overlapping haunt/footsteps/voice, low-power machine; no EDT blocking, unbounded PCM buffering, per-frame decode, audio thread leaks or new FPS regression. Explicitly measure effects rather than infer from passing unit tests.
4. **Real acceptance:** listen on Windows Java 17 final installer and a LAN session; identify missing/too-loud/too-quiet, looping clicks, wrong positional attenuation, clipping, broken device selection and stale environmental beds. A build test alone is not a listening test.
5. **Release discipline:** source + packaged output + actual GUI playback results; commit on **main** and read back remote hashes. Do **not** add GitHub Actions without approval, force-push, turn map/art polish into a functional release blocker, or claim completion when an installer is still broken.

### Explicit progress snapshot (2026-10-29)

- [x] Read current Beckenburg sound engine, soundtrack and test callsites through GitHub connector.
- [x] Identify Google Drive master asset index, shared Sounds collection and five relevant NOX sound sublibraries.
- [x] Check cross-project music/sound documentation and manifests in ReverieVR, NUTBUSTER-R, TheMechanist and Catastrophic-Diplomacy.
- [ ] Full exact tree enumeration of game binary assets + content hashes + every runtime trigger.
- [ ] Full Drive recursive inventory; audition files, compare peak/quality, reconcile NOX license.
- [ ] Full remaining-owner-repository binary inventory; compare hashes and actual fitness.
- [ ] Complete per-scene unfilled-cue matrix and choose owned clips.
- [ ] Implement missing low-cost environmental routing, sound layers and actual user-event hooks.
- [ ] External CC0 search **only for documented residual gaps**.
- [ ] Full Java 17 build, clean installer verification, Windows listening and LAN validation.

**No new audio has been imported, no new runtime behavior has been claimed, and no external CC0 search was performed as part of this initial audit.**
