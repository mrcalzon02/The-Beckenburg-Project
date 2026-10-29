# Owner-original audio reuse — Beckenburg (2026-10-29)

Source: [ReverieVR native Red Ledger sound bank](https://github.com/mrcalzon02/ReverieVR/blob/main/app/src/main/java/io/github/mrcalzon02/reverievr/RedLedgerSound.java), explicitly documented there as six **owner-exported** effects. Three were copied **byte-for-byte** (verified matching Git blob identity) into Beckenburg rather than using any external media. The original upstream files are preserved unchanged. No independent CC0/public-domain sublicense is asserted.

| Beckenburg path | Upstream original | Git blob SHA-1 | PCM format | Duration | RMS / peak dBFS |
| --- | --- | --- | --- | --- | --- |
| `assets/runtime/sfx/ui_click.wav` | [red_ledger_click.wav](https://github.com/mrcalzon02/ReverieVR/blob/main/app/src/main/res/raw/red_ledger_click.wav) | `f9307c1e9ea21216294971a8a5c0a2031d276b4f` | 44.1 kHz stereo 16-bit | 0.360 s | -36.3 / -8.3 |
| `assets/runtime/sfx/door.wav` | [red_ledger_door.wav](https://github.com/mrcalzon02/ReverieVR/blob/main/app/src/main/res/raw/red_ledger_door.wav) | `127c196f9539a448dc6d038e3ea719629fa22b76` | 44.1 kHz stereo 16-bit | 0.685 s | -26.3 / -6.8 |
| `assets/runtime/sfx/book.wav` | [red_ledger_book.wav](https://github.com/mrcalzon02/ReverieVR/blob/main/app/src/main/res/raw/red_ledger_book.wav) | `f506e52028fb55c0fdc0d4449eafd924e48a86a6` | 44.1 kHz stereo 16-bit | 0.511 s | -32.1 / -3.0 |

All three are standard RIFF PCM16 with valid headers and nonzero amplitudes (sample-by-sample metadata audit). `RecordedSfx` converts original WAV formats into bounded 16 kHz stereo PCM without a third-party decoder. `SoundCoverageTest` now requires the three recordings to decode and have nonzero output from the final classpath. The underlying sounds were **not audibly auditioned in this connector environment**; measured RMS/peak does not prove appropriateness or user perceived loudness.

Bindings: UI_CLICK / DOOR / BOOK. Everyday Foley stays separate from ghost signatures, hunt warnings and evidence. Existing NOX curated samples remain unchanged. `build.sh` and `build.cmd` already include the `assets/runtime/sfx/*.wav` directory in the packaged JAR; no new download or source archive is required at runtime.

The cross-repository authorization remains owner-original first-party game reuse, not permission to publish extracted assets as a generic free sound library. Keep provenance on public builds and repeat a physical Windows listening/volume check before release.
