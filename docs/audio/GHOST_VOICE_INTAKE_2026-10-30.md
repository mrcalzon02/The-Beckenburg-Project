# Ghost vocal intake: 2026-10-30

This package was supplied directly by the game author as `ghost.zip` and
transferred **through Google Drive staging** into the repository's runtime
`assets/runtime/sfx` asset set, then baked by the existing Windows/Unix
resource-copy rules. The original MP3s, converted WAVs, and SHA-256 checksums
are preserved in the [Beckenburg Drive archive](https://drive.google.com/file/d/12_41RaIwkbLwBJ23tO0YjiqOnLZipSAM/view).

## Content

- `ghost_idles.wav`: occasional, authentic WHISPER action
- `ghost_jump.wav`: occasional, authentic IMPACT action
- `ghost_line_01.wav` through `ghost_line_20.wav`: rare, authentic
  MANIFEST / TENSION_BUILD / STRIKE action
- Audio format: 16 kHz signed 16-bit stereo PCM WAV; no runtime decoder needed.
- Original MP3s stay in Drive, not the release JAR.
- Dialogue classification is **provisional**, based on filenames, not a
  verified transcript. Do not claim or generate literal subtitles.
- All numbered lines preserve the entire original duration (up to ~12 sec),
  while normal equipment/footstep Foley retains its four-second mixer cap.

## Rules and verification

The host/network already decides whether a sound is audible, when the event
exists, its room, pan, weather attenuation and volume. `RecordedSfx` selects
only among presentations of those **real** events using the shared event tick.
It never authors a new AI action, hunt, evidence, damage or secret position.
Missing/corrupt recordings fall back to procedural audio. Effects volume and
existing stereo mixer priority still apply; sounds are not streamed from Drive.

`GhostVoicePackRoutingTest` checks deterministic selection and the
rejection of ordinary door sounds. Inspect actual hunt/jump playback and
packaged JAR inventory separately; headless routing does not prove subjective
voice clarity, volume balance, or real-hardware audio timing. Auditory
polish and balance remain diagnostic, not release blockers.

Source assets are user-supplied; no unsupported CC0/license assertion is made.
