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
- Castle workstations now retain local sound-volume playback while their non-exiting overlay is open. Options exposes persistent independent environmental gain beside Music and Effects. Existing separate LAN voice remains intact.
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
