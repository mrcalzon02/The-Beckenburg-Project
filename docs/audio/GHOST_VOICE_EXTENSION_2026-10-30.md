# Author ghost voice extension: recordings 21–29 (2026-10-30)

The author supplied nine files: `ghost21.mp3`–`ghost26.mp3`,
`ghsot27.mp3` (original typo retained in archive), `ghost28.mp3`,
and `ghost-29.mp3`. Byte comparison confirmed `ghsot27.mp3`
is identical to `ghost26.mp3` (same SHA-256), so one runtime WAV
(`ghost_line_26.wav`) represents both. There are **eight unique**
new WAVs and **30 total** author-provided vocal resources when combined
with the previous 22-file batch.

**Source-of-truth and Drive transfer:** [archived original MP3s, eight
converted PCM WAVs, hashes and manifest](https://drive.google.com/file/d/1oDkAaAx342d0T324IRTVEBHesV4-deHN/view).
Transferred from source through Google Drive, then to GitHub as
authenticated Git blobs. Players do not need Drive or audio codecs.

| Runtime asset | Provisional role | Primary event route |
| --- | --- | --- |
| ghost_line_21.wav | quiet presence | whisper / disturbance / drift |
| ghost_line_22.wav | quiet presence | whisper / disturbance / drift |
| ghost_line_23.wav | abrupt voice | manifestation / impact / horror sting |
| ghost_line_24.wav | idle/searching | whisper / footfall / manifestation |
| ghost_line_25.wav | forceful voice | strike / impact / hunt buildup |
| ghost_line_26.wav | intermittent searching | ghost footfall / strike / hunt buildup |
| ghost_line_28.wav | sustained threatening vocal | **hunt buildup**, not short jump stings |
| ghost_line_29.wav | searching/manifesting | ghost footfall / manifestation / strike |

These roles are selected using duration, dynamic profile and the user's
stated acceptable categories—not verified spoken-text transcription. Do
not invent subtitles or infer ghost identity from these voices.

The host/solo simulation creates legitimate `LanSound.Cue` events;
`RecordedSfx` only changes some events into authored voice playback.
Existing authority over evidence, AI, LOS, sound propagation, volume,
weather masking and privacy is unchanged. All decisions use
`CueEvent.tick()` and cue type, no client-local randomness. Normal
door/UI/equipment sound cannot select ghost recordings. Existing 20
numbered lines remain reachable. Non-selected or absent recordings
fall back to existing normal sound cues.

The longer recording 28 needs up to 16 seconds of buffered voice PCM.
Ordinary equipment and short Foley keep their existing limits.
SFX are packaged by existing Unix/Windows build copy rules; original
MP3s stay in Drive. No invented CC0 or redistribution license claim.

`GhostVoiceExpansionTest` is deliberately a **non-release-blocking
audio presentation diagnostic** in the full-round workflow, checking
deterministic placement, use of all eight unique assets, legacy
reachability, absence of ghost voice on normal door actions, and
full-length runtime WAV decoding. Physical device audition and
subjective volume/balance remain pending.
