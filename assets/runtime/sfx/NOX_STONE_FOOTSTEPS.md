# Curated NOX Essentials rock-footstep recordings — 2026-10-29

**Publisher & current rights statement:** [Nox_Sound_Design — Essentials Series, itch.io](https://nox-sound-design.itch.io/essentials-series-sfx-nox-sound). The source publisher states the 1,644 effects are released under **CC0** for personal and commercial projects. This is the source for these original mono 24-bit, 48-kHz recordings, not a mirror or unrelated Minecraft file.

**Source:** The owner's existing Google Drive NOX Essentials / Footsteps / Rock / Walk folders. The full ~1 GB NOX ZIP remains unmodified on Drive; only the three chosen short files were imported into the playable Java game. The Java 17-targeted JavaSound test on the three original WAV files confirmed they each convert to nonzero 16-kHz, 16-bit stereo PCM using the runtime's exact conversion format (OpenJDK 21 `javac --release 17` format test, not a Windows sound-device listening test).

| Runtime filename | Exact original Drive source | Original size | SHA-256 of original/runtime bytes | Git blob SHA-1 | Duration |
| --- | --- | ---: | --- | --- | ---: |
| `step_stone_0.wav` | [Footsteps_Rock_Walk_01.wav](https://drive.google.com/file/d/1BWGY_oZfiB43NN9lt5rJIlgf6MoaIpYY/view) | 63,598 | `156069957f17b89d191497184a409b7f12ac7ff1c64596256bcfcd5922f7bd4e` | `0918b51a618acd6ef6f12053a2a6a57125024b84` | .436 s |
| `step_stone_1.wav` | [Footsteps_Rock_Walk_03.wav](https://drive.google.com/file/d/1HWwwEuU6B2s_n5HvunqrOxcQZg00VLX8/view) | 65,422 | `bcae0ee8297decd9a2ced9552465ca44163492517b94ce1c449e833ff7fb4506` | `be6bde4af905d4d90e29d9b31b2f835b63d7f6b2` | .449 s |
| `step_stone_2.wav` | [Footsteps_Rock_Walk_06.wav](https://drive.google.com/file/d/1SKr4CEaO2iMG_XiMyBPSrkEIKmP6fX3R/view) | 66,574 | `26bd855d9897c5f24fa36e8debe5aafd3a255ef32852c6b1e45753cab1099012` | `4edc39d565783c7690dad54bef8b6682941f67ad` | .457 s |

`RecordedSfx` chooses variants by accepted event tick modulo three and falls back to original deterministic synthesis only when a source cannot decode. For local/solo steps, `GameSfx` supplies a monotonic per-accepted-step variation seed; its existing debounce remains in place. The materials are not misapplied to carpet, wood, grass or concrete without a separately authored fit decision.

**Gap left on purpose:** NOX Wood and Metal Walk subfolders currently list no recordings in this user's connected Drive; don't assert either has playable clips. Continue using existing original synthesized STEP_WOOD and other material fallbacks until appropriate recorded samples are found. Actual in-game loudness, contact realism, and footsteps aligned to movement animation still need Windows listening/GUI verification.

The dedicated `SoundCoverageTest` verifies that all three separate takes produce bounded, nonzero, distinct and deterministic stereo PCM in a correctly packaged Java build.
