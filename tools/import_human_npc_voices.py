#!/usr/bin/env python3
"""Verify and import the owner-voiced Beckenburg human NPC dialogue archive.

Usage:
  python tools/import_human_npc_voices.py PACK.zip --dry-run
  python tools/import_human_npc_voices.py PACK.zip

Speech/script alignment is provisional and must be checked by listening.
"""
import argparse
import csv
import hashlib
import io
import json
from pathlib import Path
import wave
import zipfile

EXPECTED_ZIP_SHA = "fa8d0bfa17ed651b9b25154c935f551085f2f0277c60e06c7a3fc104b8bc3d64"
EXPECTED_MP3_SHA = "52bd095579a9f90c2041c8c901c612e58a31a57b77eda2754726fbd1a425d94f"
ROLES = {
    "matthis_rook": 10, "quartermaster": 10, "expedition_dispatcher": 8,
    "lantern_concord_warden": 8, "meridian_survey_technician": 8,
    "ashen_collegium_scholar": 8, "veteran_investigator": 12,
    "infirmary_medic": 8
}

def import_archive(pack, output, dry_run=False):
    if hashlib.sha256(pack.read_bytes()).hexdigest() != EXPECTED_ZIP_SHA:
        raise ValueError("ZIP digest differs from owner-verified pack")
    with zipfile.ZipFile(pack) as z:
        if z.testzip() is not None:
            raise ValueError("ZIP CRC validation failed")
        names = set(z.namelist())
        metadata = json.loads(z.read("dialogue_manifest.json"))
        rows = list(csv.DictReader(io.StringIO(
            z.read("dialogue_manifest.csv").decode("utf-8"))))
        if len(rows) != len(metadata.get("clips", [])) or len(rows) != 72:
            raise ValueError("Expected 72 dialogue clips")
        if metadata.get("master_sha256") != EXPECTED_MP3_SHA:
            raise ValueError("Source master digest incorrect")
        if metadata.get("role_line_counts") != ROLES:
            raise ValueError("Unexpected human character voice roster")
        if hashlib.sha256(z.read("ElevenLabs_voiced_MASTER.mp3")).hexdigest() != EXPECTED_MP3_SHA:
            raise ValueError("Master MP3 changed")
        allowed = {"ElevenLabs_voiced_MASTER.mp3", "dialogue_manifest.json",
                   "dialogue_manifest.csv", "READ_ME_FIRST.txt"}
        for index, row in enumerate(rows):
            rel = Path(row["relative_file"])
            if (rel.is_absolute() or ".." in rel.parts or len(rel.parts) != 3
                    or rel.parts[0] != "dialogue" or rel.parts[1] != row["character"]
                    or rel.name != row["id"] + ".wav"):
                raise ValueError("Invalid entry path: " + str(rel))
            if rel.as_posix() not in names or row["character"] not in ROLES:
                raise ValueError("Audio file absent or unexpected: " + str(rel))
            if row["id"] != metadata["clips"][index]["id"]:
                raise ValueError("CSV/JSON ID mismatch at " + str(index + 1))
            with wave.open(io.BytesIO(z.read(rel.as_posix()))) as wav:
                if ((wav.getnchannels(), wav.getframerate(), wav.getsampwidth())
                        != (1, 22050, 2) or wav.getnframes() < 5000):
                    raise ValueError("Invalid WAV format: " + str(rel))
            allowed.add(rel.as_posix())
        if names - allowed:
            raise ValueError("Unexpected archive entry")
        copies = [(entry, output / entry) for entry in allowed
                  if entry.startswith("dialogue/")]
        copies += [(entry, output / entry) for entry in
                   ("dialogue_manifest.csv", "dialogue_manifest.json",
                    "READ_ME_FIRST.txt")]
        if any(dest.exists() for _, dest in copies):
            raise FileExistsError("Refusing to overwrite existing dialogue assets")
        if not dry_run:
            for entry, dest in copies:
                dest.parent.mkdir(parents=True, exist_ok=True)
                dest.write_bytes(z.read(entry))
    return len(rows)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("pack", type=Path)
    parser.add_argument("--output", type=Path, default=Path(
        "software/beckenburg/assets/runtime/audio/human-npc"))
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    count = import_archive(args.pack, args.output, args.dry_run)
    print("Validated %d clips: %s" % (count, "dry run only"
          if args.dry_run else str(args.output)))

if __name__ == "__main__":
    main()
