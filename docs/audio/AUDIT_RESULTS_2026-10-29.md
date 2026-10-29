# The Beckenburg Project — Audio Audit Results and Integration Acceptance Record
**Snapshot:** 2026-10-29 • **Branch:** `main` • **Status:** implementation and source provenance pushed; **Windows audible acceptance outstanding**.

## Summary of exact repository inventory

The authoritative machine-readable [runtime/source audio manifest](RUNTIME_AUDIO_MANIFEST.csv) was generated from the complete GitHub `main` recursive tree. It records Git blob object IDs and sizes for **75 tracked audio-related objects**, including historical/development duplicates (NOT 75 independent sound effects).

| Class | Tracked entries | Meaning |
| --- | ---: | --- |
| `assets/runtime/sfx/` | 19 WAV files | 4 first-party owner-exported Foley; 3 new NOX rock walks; 9 previously curated NOX effects; 2 existing NOX climate loops; 1 Hollow Creek goose omen |
| `assets/runtime/music/` | 8 ZIP files/parts | **6 logical recorded score tracks**, two split over pairs of files |
| `assets/runtime/sounds/` | 2 `.sounds` documents | Castle (new, 10 spatial zones) and Hollow Creek (existing, three zones) |
| `assets/runtime/weather/` | 11 bundles | Shared weather module content (includes audio plus other effects); DO NOT count as eleven separate songs |
| `assets/libraries/events/audio/` | 16 sound/event library files | Source archive, not automatically packaged in runtime as standalone WAV |
| `development/audio/hollow-creek/` | 3 sources | Goose record + original input/master/provenance; source only |
| `development/levels/event-audio/` | 16 files | Duplicate source/event sound objects used by development, not runtime additions |
| **Total** | **75** | Includes same-byte duplicates, source-only assets and archive pieces |

The audio-source verifier is [`tools/audio_audit.py`](../../tools/audio_audit.py), runnable offline from a full checkout. It checks object identity, 52-cue source fallback coverage, resource presence, sound-zone layout and registration in `build.sh` and `build.cmd`. It is **not** a hardware sound-card test or a claim that every game control has an audible hook.

## Baseline and implemented fixes

- `LanAudio`: existing priority-aware, bounded, off-EDT 16 kHz stereo mixing; host projection/ghost location privacy remain unchanged. Existing synthesized event vocabulary now covers **52** cue types including new explicit `PURCHASE` and `UI_REJECT` results.
- `SoundtrackAudio`: existing castle / ambient / ritual / hunt states and six original scores. Verified code selects hunt above ritual, ritual above idle field, while the castle has its own score. The game does not need extra music for every map; atmospheric silence is an intentional viable state.
- `WeatherSoundscape`: weather-local outdoor/indoor mixing and existing NOX rain/wind recordings. Now controlled by `EnvironmentAudio.volume()`, not the urgent gameplay-effects master.
- `LevelSoundscape`: existing source-specific architectural room tones and `RoomAcousticPresets` for the field maps, controlled by Environment gain; no authored new per-room music implied.
- `SoundVolumeAudio`: existing authored 3D ambience/mixer. Added `castle.sounds` with **ten zone sources** (courtyard and gate wind; timber movement in armory, contracts, wardrobe, archive and upper library; stone resonance in store/practice; low operations electric hum gated by power). Authored music and environmental emitters are separate buses.
- Castle workstations now retain local sound-volume playback while their non-exiting overlay is open. Options exposes persistent independent environmental gain beside Music and Effects. The front Castle door now emits the actual `DOOR` event (rather than misusing a light-switch click); opening the Castle archive emits `BOOK`, and workstation exceptions emit `UI_REJECT`. Existing separate LAN voice remains intact.
- Current short physical event sounds: exact original owner-exported `ui_click.wav`, `door.wav`, `book.wav`, `purchase.wav` from [ReverieVR's Red Ledger bank](../../assets/runtime/sfx/OWNER_REUSE.md). Every clip has a manifest and provenance record. There is **no** new external media source or network runtime dependency.
- Confirmed campaign purchases/sales emit a distinct `PURCHASE`; failed save/transaction emits synthesized `UI_REJECT`, and equipment equip emits `SELECT`. Requests awaiting the multiplayer host are not mislabeled as confirmed purchases. Duplicate quartermaster pickup callback audio was removed.
- `STEP_STONE` now uses three genuinely distinct, owner-archived, publisher-CC0 [NOX stone-footstep WAVs](../../assets/runtime/sfx/NOX_STONE_FOOTSTEPS.md) selected by the accepted event tick. All three original 24-bit/48-kHz mono WAVs were fed through a standalone JavaSound Java 17-targeted conversion check, producing nonzero 16-bit/16-kHz stereo PCM. Existing procedural fallback remains for missing/unavailable files and for other materials.
- The `SoundCoverageTest` and new `EnvironmentAudioTest` have been registered in both platform build scripts. These tests require original recorded source decoding, distinct rock walking takes, 10 castle sound zones, proper environment mute isolation, and power gating. **Tests have been authored and registered; a full clean Java 17 application/test build has not been executed by this connector work session.**

## Rights and owned-first source evaluation

**Source order observed:** Beckenburg original/curated material → owner Google Drive → owner's first-party GitHub apps → potential external CC0 only for documented residual gaps. No sound was copied from an unrelated unapproved third-party game. The upstream original [NOX SOUND Essentials Series page](https://nox-sound-design.itch.io/essentials-series-sfx-nox-sound) explicitly labels the 1,644 audio recordings CC0 and permits commercial reuse. That resolves the older Drive master index's general 'license review' note for **this NOX product**, not for unrelated horror or sound packs. See the exact original Drive IDs and SHA-256 in the stone provenance record.

**Other first-party repositories checked (2026-10-29):**

| Repository | Direct tracked audio files in inspected tree | Fit/reuse decision |
| --- | ---: | --- |
| ReverieVR | 24 | Four short original Red Ledger Foley **reused now**. Eleven owner-original songs are separate, non-CC0 assets: refrain from duplicating them because six Beckenburg scores already exist |
| NUTBUSTER-R | 42 | Existing owner sound library overlaps Drive and HB-TTRPG. Comedic announcer and music are stylistically wrong; do not import |
| TheMechanist | 461 | Large mechanical/industrial audio archive; candidate for future rare infrastructure accents, but rights/provenance not universally cleared |
| Tech-Priests | 490 | Many packaged Factorio-related and custom OGG sounds; do **not** reuse game-derived or third-party files without exact provenance |
| The-Malefic-Veil | 10 | Several shared UI/weapon effects; not needed over owned short physical Beckenburg assets |
| Catastrophic-Diplomacy | 12 | Electronic/combat effects and shared owner music; `Final Surge` and `Cold Unresolved Drone` overlap Beckenburg soundtrack titles, so avoid redundant import |
| Jewel_Brook | 14 | Puzzle-game synthesized confirmation sounds; stylistically unnecessary for this horror interface |
| Alfheim-Reclaimed | 6 | Elf murmurs; not appropriate generic investigator or ghost vocal cues |
| Magenheim | 0 | No individual WAV/OGG/MP3 files in examined main tree; this does **not** rule out BepInEx/Unity packaged sound in other formats |
| Continuity-Works | 0 | No direct audio recordings observed |
| HB-TTRPG-tools | 74 | Exact earlier Beckenburg source duplicates and shared old Sounds clips; authoritative runtime is current Beckenburg main |
| Infinite-Domain | at least 539 (tree truncated) | Minecraft sound-resource copies in a resource pack; **not** Beckenburg-compatible first-party or CC0 rights |

These direct-media counts are file counts, not unique audible sample counts, and do not prove a complete audio review of every compressed package. NOX's nested wood/metal walk folders remain empty in the connected Drive listing; available rock-walk samples are real. Do not fill gaps with the first filename that looks plausible.

## Scene coverage / acceptance matrix

| Context | Implemented audio layers | Remaining proof or priority |
| --- | --- | --- |
| Castle and courtyard | Original castle music + 10 authored spatial beds + material footsteps, doors, nearby workstation UI | Real Windows scene/door/acoustic listen, confirm volume balance and no FPS regression |
| Mission staging / van | Existing electronic/monitor NOX, radio/fixture procedural sound, score/weather + room acoustics | Verify actual van placement and active monitor/door hooks on Windows |
| House/mansion/other interior investigation | Existing zone-sensitive procedural architectural ambience, environmental weather, footstep floors + ghost/event effects | Walk through every implemented site and verify physical acoustics, surface selection, room/door masking |
| Hollow Creek outdoor/camps | Existing `hollow-creek.sounds` layout, environmental winds/weather, a goose omen recording | Verify location, loop mix, transitions and intended timing |
| Ghosts/hunts/rituals | Original tiered event mix, captioned projected spatial warnings, scored hunt/ritual | Confirm host sound/privacy, volume priority, no false evidence |
| Equipment/evidence | Curated equipment/monitor/DOTS NOX, sound recorder, procedural per-device controls | Identify every **functionally implemented** tool's real accepted-event sound hook; don't fake completion for incomplete devices |
| Menus, purchases, stock/loadout | Owner-original click and purchase, distinct rejected cue and selection feedback | Windows audible positive/negative confirmation and UI repeat-rate test |
| Solo, LAN, accessibility | Existing quantified positional host events, captions, room material attenuation, independent voice | 1–6 hunter hardware session; voice/mix priority, disconnect/resume, muted captions |

The application contains **52 captioned cue identities with a generated fallback**, but this does **not** mean 52 separate authored recordings, nor prove all current menus/field interaction branches have been triggered. That requires interactive scene walkthrough acceptance.

## Outstanding external/hardware gates — do not falsely close

1. **Java 17 full build:** run `sh build.sh` or `build.cmd` on a complete checkout; attach test logs for `SoundCoverageTest`, `EnvironmentAudioTest`, `SoundtrackAudioTest`, `FixtureSoundscapeTest`, site/acoustic regressions and compile errors. This implementation used GitHub source connector and *did not* run the full proprietary/large-asset game build.
2. **Packaged Windows install:** verify JAR includes all four owned Foley WAVs, three recorded rock takes and `castle.sounds`; check release installer actually launches separately from game executable.
3. **Listening & mixer:** compare castle against problematic first house FPS, listen for clipping, stale loops, broken panning, inaudible positive/negative UI cues, overloaded audio threads, and prompt playback when opening doors. Inspect old sound device behavior when no output is available.
4. **Source license outliers:** the separate user-provided PS1 Horror SFX Vol1 numbered archive lacks verified redistribution rights; **do not** ship its originals. Continue synthesized generic supernatural warnings. User-supplied Beckenburg music public redistribution warranties still need review before release.
5. **Event-coverage review:** capture a site-by-site click/walk/play test matrix so no unsupported tool is advertised as fully implemented merely because audio feedback is present.

No new GitHub Actions, forced branch changes, full-pack audio dumps, copied game IP, or third-party online runtime dependencies were introduced in this audio pass.

**Claim contract:** Owned-first source inventory, new playable-source sound connections, and test authoring: COMMITTED. Three original NOX rock WAVs decoded in an isolated Java format test: VERIFIED. Complete game build/Windows audible acceptance: NOT YET VERIFIED.

## Final-JAR audio packaging gate — 2026-10-29

**New implementation:** [`PackagedAudioAudit.java`](../../src/main/java/org/beckenburg/PackagedAudioAudit.java), executed from the **assembled JAR's own classpath** immediately after packaging in both [`build.sh`](../../build.sh) and [`build.cmd`](../../build.cmd), **including Windows `package` mode**. Neither builder may report a new packaged JAR as accepted if this audit rejects its audio.

The checker opens and validates exactly **19 runtime WAVs** through JavaSound's 16-kHz stereo mixer conversion, all **six logical music archives** including split ZIP parts and U-law decoding compatibility, **10 Castle + 3 Hollow Creek authored zones** in their packaged `.sounds` files, and **11 populated shared-weather ZIP bundles**. Failure is fatal and names the missing, truncated, invalid or unsupported resource. It loads nothing from Drive and requires no sound card, extra audio library or runtime network dependency.

**Isolated verifier acceptance performed:** The exact Java source was compiled successfully with OpenJDK 21 `javac --release 17`. Against a purpose-built **synthetic** packaged JAR with the expected files, the command `java -Djava.awt.headless=true -cp positive.jar org.beckenburg.PackagedAudioAudit` passed (19 WAVs / 6 scores / 13 zones / 11 weather packs). The negative control deliberately omitted `sfx/book.wav` and exited nonzero with `Absent packaged audio: sfx/book.wav`. These fixture tests validate the verifier; they are **not tests of the actual Beckenburg asset bytes, full Java game build, or Windows speaker playback**.

**Remaining exit evidence:** full actual checkout build and a screenshot/log of the newly required packaged check running against `build/beckenburg-game.jar`; then hands-on Windows audio mix, performance and LAN acceptance. The issue remains open until those tests pass. 

### Device-disconnect recovery pass (2026-10-29 continuation)

Source implementation is now committed for each audio output path previously found to terminate permanently after a transient device error:

- `LanAudio` (positional/event effects): recover and reopen on the **next accepted new event**, with a bounded 350 ms delay after output failure. Previously queued voices/events are discarded to avoid stale ghost and hunt sounds. The host owns the sound location; no protocol or evidence-authority rule was modified.
- `SoundVolumeAudio` (Castle/Hollow Creek spatial environmental volumes) and `LevelSoundscape` (room architecture): retain the worker, close a failed/partially opened line safely, and retry; active scene/power gating remains intact.
- `WeatherSoundscape`: retry after output failure, clear partly played thunder and drain old weather-module cues so late thunder is not presented as a fresh event.
- `SoundtrackAudio`: safely release a lost output, reset the current score decoder on recovery, restart the appropriate castle/ambient/ritual/hunt track with its existing soft entrance; normal shutdown no longer logs an erroneous audio failure.
- `LanVoiceClientAudio`: keep the multiplayer voice playback loop alive across speaker loss, discard stale buffered words, report loss/restoration via voice status, and safely close capture/playback lines after unplugging.

**Deterministic test authored and registered:** `src/test/java/org/beckenburg/AudioDeviceRecoveryTest.java` injects an output factory into the production `LanAudio` worker. A fake JavaSound device intentionally fails its first opening, then accepts stereo writes; the test requires new gameplay audio to resume and the device to close cleanly. It is registered in **`regression-tests.txt`, `build.sh`, and `build.cmd`**. The fake `SourceDataLine` implementation was independently compiled with `javac --release 17`; this is **not** a claim that the integrated Beckenburg test was executed. Integrated compilation, Windows unplug/replug and multiplayer listening remain open functional gates.

**Packaging parity found during this pass:** `build.sh` compiled the common water module but omitted `dev/mrcalzon/water` from the JAR (the Windows builder already shipped it). Linux now packages and checks `WaterDocument.class` and `WaterModule.class`, preventing a source-to-distributable mismatch that could stop authored sites from opening. This is an additional package integrity fix, not independent proof of a clean whole-game build.

**Verification scope:** Every changed source, test, regression manifest and both platform scripts was fetched back from GitHub `main` by exact path after push. Live Java output-device reconnection and full Beckenburg builds have **not** been run in this session because the connected GitHub repository is not mounted as a local checkout and container Git networking cannot resolve GitHub. Keep issue #43 open until the integrated Java 17, installer, speaker, LAN and scene-performance gates pass.

### Per-sample performance and non-progressing output audit — 2026-10-29

**Confirmed source-level inefficiency:** the investigation `LevelSoundscape.run()` PCM loop invoked `LevelSoundscape.sample()` 16,000 times per second. Each call needlessly re-ran `RoomAcousticPresets.forRoom()`, including many string/regex matches, even while the same room remained active for the full block. Now a single per-block room/acoustic/power snapshot selects the profile and seed, and `sampleResolved()` renders the identical waveform. `RoomAcousticPresetsTest` compares public and optimized PCM across **all authored `HouseMap.Site` values**, rooms, both power states and seven sample positions including outdoor silence. This is a clear CPU-work reduction, **not measured proof of a faster frame rate on the first-house map**.

**Weather mixer:** the shared weather integration previously called synchronized `original(layer.id())` for every active recording on **every PCM sample**, repeatedly doing resource-map lookup. The worker now resolves recorded `LoopBed` handles once per 1024-sample output block and uses immutable short arrays while mixing. Missing optional captures produce a one-time diagnostic and fallback procedural weather rather than killing the worker; `originalSamples()` remains strict for packaged-original tests. Unknown weather cues are skipped and reported rather than silently terminating ambient playback.

**Stopped or disconnected audio devices:** all six live sound workers (positional cues, authored environment, building ambience, weather, score and LAN voice) treat a non-progressing, zero/negative-byte output write as a recoverable line failure. Previously several loops would silently drop PCM or spin instead of trying another device. An expanded `AudioDeviceRecoveryTest` covers both a failed initial output open and an output device that opens but returns zero written bytes; in each case, the **production positional worker** is expected to release the old line, reopen on a subsequent cue, produce PCM and shut down. The test is registered in both build scripts and the release manifest; **a full integrated run remains outstanding**.

**Concurrent map transitions and application exit:** environmental, room and weather workers now hold a stable local line reference while mixing, avoiding a null read when a view closes its output concurrently. Positional audio retains an output-line reference explicitly for `close()` to stop/flush/close and unblock a possible blocking write. Existing local-only mixing, host authoritative ghost privacy, independent Music/Environment/Effects controls and separate LAN voice were preserved.

**Remote verification (source/manifest):** the `main` recursive tree contains all **75 pinned manifest entries** with exactly matching Git blob IDs and byte sizes. It also contains **all 19 runtime WAVs**. The 11 relevant Java/build/test files were fetched back from remote `main` and passed specific source-invariant checks for the new retry logic, cached-acoustic loop, parity regression and build registration. These are static checks, **not a full `javac` game build or physical audio/performance measurement**.

**Remaining necessary release gates:** full Java 17 source compile and complete release manifest execution; actual final-JAR verification with `PackagedAudioAudit`; real Windows output-device disconnect/reconnect and case-to-Castle transition; first-house frame-time/audio CPU profiling; listening and 1–6-player LAN-voice checks; final rights clearance for any third-party horror/audio assets. Continue tracking them in [issue #43](https://github.com/mrcalzon02/The-Beckenburg-Project/issues/43); do not close it prematurely.
