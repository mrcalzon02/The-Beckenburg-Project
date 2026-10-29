#!/usr/bin/env python3
"""Repeatable Beckenburg audio inventory verification; no network or third-party libraries.

Run: python3 tools/audio_audit.py
Uses docs/audio/RUNTIME_AUDIO_MANIFEST.csv as a pinned Git-object snapshot.
Source-level checks complement, never replace, JavaSound tests and real listening.
"""
from __future__ import annotations

import csv
import hashlib
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "docs/audio/RUNTIME_AUDIO_MANIFEST.csv"


def blob_sha1(data: bytes) -> str:
    """Git blob object identity, intentionally distinct from raw SHA-256."""
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def entries():
    with MANIFEST.open(encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(line for line in stream if not line.startswith("#"))
        required = {"path", "github_blob_sha1", "bytes", "category", "packaging", "provenance_or_rights"}
        if not required.issubset(reader.fieldnames or ()):
            raise AssertionError("Invalid audio manifest columns")
        return list(reader)


def cue_names(source: str):
    start = source.index("public enum Cue {")
    finish = source.index("final int acousticBudget", start)
    declaration = source[start:finish]
    return re.findall(r"(?:^|[,;\n])\s*([A-Z][A-Z0-9_]*)\s*\(", declaration)


def switch_cases(source: str, opener: str):
    start = source.index(opener)
    finish = source.index("};", start)
    names = set()
    for clause in re.findall(r"case\s+([A-Z0-9_,\s]+?)\s*->", source[start:finish]):
        names.update(name.strip() for name in clause.split(","))
    return names


def audit():
    rows = entries()
    errors = []
    seen = set()
    duplicate_objects = {}
    for row in rows:
        path = row["path"]
        if path in seen:
            errors.append("Duplicate manifest path: " + path)
            continue
        seen.add(path)
        actual = ROOT / path
        if not actual.is_file():
            errors.append("Missing tracked sound file: " + path)
            continue
        data = actual.read_bytes()
        if len(data) != int(row["bytes"]):
            errors.append("Changed byte length: " + path)
        if blob_sha1(data) != row["github_blob_sha1"]:
            errors.append("Changed Git blob identity: " + path)
        duplicate_objects.setdefault(row["github_blob_sha1"], []).append(path)

    src = ROOT / "src/main/java/org/beckenburg"
    cues = cue_names((src / "LanSound.java").read_text(encoding="utf-8"))
    synth = (src / "LanAudio.java").read_text(encoding="utf-8")
    sample = switch_cases(synth, "int samples=switch(event.cue())")
    waveform = switch_cases(synth, "double wave=switch(event.cue())")
    if len(cues) != len(set(cues)):
        errors.append("Duplicate cue declaration in LanSound")
    for cue in cues:
        if cue not in sample or cue not in waveform:
            errors.append("Uncovered synthesized event: " + cue)
    for special in ("ui_click", "book", "door", "purchase"):
        if not (ROOT / "assets/runtime/sfx" / (special + ".wav")).is_file():
            errors.append("Missing owner-original Foley: " + special)
    for i in range(3):
        if not (ROOT / "assets/runtime/sfx" / ("step_stone_" + str(i) + ".wav")).is_file():
            errors.append("Missing NOX stone walking take " + str(i))
    for build in ("build.sh", "build.cmd"):
        body = (ROOT / build).read_text(encoding="utf-8")
        for term in ("SoundCoverageTest", "EnvironmentAudioTest"):
            if term not in body:
                errors.append(build + " omits " + term)
    castle = (ROOT / "assets/runtime/sounds/castle.sounds").read_text(encoding="utf-8")
    if len([x for x in castle.splitlines() if x.count("|") == 17]) != 10:
        errors.append("Castle does not contain ten well-formed authored sound-volume rows")
    castle_code = (src / "CastleHubPanel.java").read_text(encoding="utf-8")
    if "GameSfx.play(LanSound.Cue.DOOR);" not in castle_code:
        errors.append("Castle door has no real DOOR audio event")
    if "archive.open();GameSfx.play(LanSound.Cue.BOOK);" not in castle_code:
        errors.append("Castle archive lacks book-opening Foley")
    runtime = [r for r in rows if r["packaging"] == "packaged"]
    copies = [paths for paths in duplicate_objects.values() if len(paths) > 1]
    status = {
        "snapshot_assets": len(rows),
        "runtime_or_packaged_entries": len(runtime),
        "source_only_entries": len(rows) - len(runtime),
        "cue_vocabulary": len(cues),
        "cue_synthesis_fallbacks_covered": len(cues) - sum(cue not in sample or cue not in waveform for cue in cues),
        "owned_recorded_foley": 4,
        "cc0_stone_footstep_takes": 3,
        "castle_authored_sound_volumes": 10,
        "duplicate_object_groups": len(copies),
        "errors": errors,
        "note": "Static source/asset checks only; no sound-card, mixer playback or Windows installer proof",
    }
    print(json.dumps(status, indent=2))
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(audit())
