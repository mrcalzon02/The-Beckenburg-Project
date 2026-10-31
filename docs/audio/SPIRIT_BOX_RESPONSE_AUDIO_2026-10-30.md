# Spirit Box: audible evidence chain

## Runtime change
The initial four real, intelligible WAV samples are at:
- `assets/runtime/sfx/spirit_box_leave.wav` — "Leave."
- `assets/runtime/sfx/spirit_box_help.wav` — "Help me."
- `assets/runtime/sfx/spirit_box_here.wav` — "I'm here."
- `assets/runtime/sfx/spirit_box_out.wav` — "Get out."

These are **temporary original, locally synthesized TTS performances** rendered as
radio-bandlimited speech, **not ElevenLabs performances**. The owner has spent
zero ElevenLabs credits on this production pass. Preserve the owner's remaining
credit budget for a proper upgraded vocal pack if desired.

Corrected full-phrase source archive, verified in Google Drive:
https://drive.google.com/file/d/1Cep9-kNZSjLln_x7NgZxWXDelgCPXFdO/view?usp=drivesdk

The existing build.sh and build.cmd already package `assets/runtime/sfx/*.wav`
into the game JAR. `RecordedSfx` decodes these files to 16-kHz signed stereo PCM,
and `LanAudio` mixes them through the same existing nonblocking effects output.

The emitted Spirit Box effects are:
- SPIRIT_BOX_STATIC (procedural bounded radio hiss): every accepted scan
- SPIRIT_BOX_LEAVE, HELP, HERE or OUT: only when a *real* host-originated
  WHISPER or SIGNATURE action is fresh, in the correct room and in actual tool
  range, compatible with the evidence/difficulty policy
- No reply from mere ghost proximity, unrelated ghost actions, old voice events,
  expired event windows, incorrect room or insufficient real tool range
- One audible reply per unique ghost vocal-event tick **per investigator**
- A host-authorized line is a raw observation; it does **not** automatically
  mark or confirm Spirit Box evidence, select a ghost identity or unlock a rite
- No individual ghosts are assigned personalities or unique voice profiles

## Physical audio

The host emits static at the actual held device and projects spoken replies
from the holder's real location. Nearby investigators receive audible speech
through normal room physics; distant van seats do not receive remote ghost
speech simply because a teammate used a radio. The existing `FieldSoundRecorder`
captures these physical sound cues for replay if active. The radio never queries
hidden ghost identity to choose a phrase.

Solo `InvestigationSession` now tracks actual vocal actions separately from
general EMF disturbances; old room-only yes/no logic is retired. In-hand use in
`InvestigationGame` plays the same packaged WAVs and static PCM through the
existing solo audio engine.

## QA and acceptance

`SpiritBoxResponsesTest` tests positive/negative world-event eligibility,
replay prevention, missing/expired voice events, range, room, four original
packaged spoken WAVs and the audible static fallback. Both build drivers run
this objective functionality regression. Local JavaSound testing confirmed
all four source WAVs decode into non-silent 16-kHz stereo PCM; exact GitHub
asset blob hashes were read back.

Full Java 17 game build, live Windows audio playback, solo world interaction
and full networked hunter/ghost playtest must still be checked independently
against the current commit/CI run. The code and audio being committed do not
by themselves certify those runtime tests. Do not disable the main build on a
subjective voice-acting style judgment; absent/malformed audio is a real
feature/functionality failure.

### Future voice production

The *script text* for an ElevenLabs upgrade is exactly four lines, in this
order: Leave. / Help me. / I'm here. / Get out.

Use the same four filenames for a no-logic-change replacement after validating
the actual spoken words and clipping; preserve the originals in the source
archive and do not spend owner credits without an explicit request.
