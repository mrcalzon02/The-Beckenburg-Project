# Beckenburg — ElevenLabs human NPC voice archive (2026-10-30)

Owner-supplied source: ElevenLabs_voiced.mp3 (5m 26s, mono 44.1 kHz).
Source zip: https://drive.google.com/file/d/1LUxcXYfl8eiv1WrC-Ocm_9-Y_buQMF07/view?usp=drivesdk
Owner Drive folder: https://drive.google.com/drive/folders/1vzb5q3WhZFaJMaH6jYYKn9cP0oFN6umy

The verified Drive archive is 16,867,470 bytes and includes the unmodified MP3 master, 72 derived mono 22,050-Hz/16-bit WAV clips, dialogue_manifest.csv, dialogue_manifest.json, and READ_ME_FIRST.txt. The source was archived without editing the original recording. Drive staging remains private.

A verified non-destructive importer is at tools/import_human_npc_voices.py. To use the pack in a checkout, download the exact ZIP from Drive and run:

    python tools/import_human_npc_voices.py Beckenburg_ElevenLabs_Human_Voices_72_clips.zip --dry-run
    python tools/import_human_npc_voices.py Beckenburg_ElevenLabs_Human_Voices_72_clips.zip

The importer verifies ZIP and source digests, CRC, manifests, filenames, 72 WAV headers, role counts, and refuses silent overwrites; it writes software/beckenburg/assets/runtime/audio/human-npc/.

Provisional line distribution: Matthis Rook 10, Quartermaster 10, Dispatcher 8, Lantern Concord Warden 8, Meridian Survey Technician 8, Ashen Collegium Scholar 8, Veteran Investigator 12, Infirmary Medic 8. No individual ghost voices.

KNOWN REMAINING WORK: The clip/line assignments are provisional (silence- and script-order based, not speech-transcription verified). Listening QA, real binary files committed to GitHub, runtime NPC interaction and subtitle triggers, audio occlusion and Windows Java 17 acceptance are not complete. This Drive upload and importer commit do not by themselves constitute in-game dialogue playback.

ZIP SHA-256: fa8d0bfa17ed651b9b25154c935f551085f2f0277c60e06c7a3fc104b8bc3d64
Master MP3 SHA-256: 52bd095579a9f90c2041c8c901c612e58a31a57b77eda2754726fbd1a425d94f
