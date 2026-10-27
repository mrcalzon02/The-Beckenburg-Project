#!/usr/bin/env python3
"""Validate and stage *approved* Beckenburg UI candidates from the Drive ZIP.

No installation or runtime linkage occurs. By default this tool only verifies.
See docs/art/PSX_UI_CONCEPT_LIBRARY_2026_10_26.md.
"""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import sys
import tempfile
import zipfile

SHA256 = "9055a8884c5d622a3107bc349794268337646756bf37d0fe41721527e72e5efc"
ARCHIVE_ROOT = "Beckenburg_PSX_UI_Library_2026-10-26/"

def digest(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fd:
        for block in iter(lambda: fd.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()

def main() -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("archive", type=Path, help="Locally downloaded Drive ZIP")
    p.add_argument("--stage", type=Path, help="Destination for review-only staging")
    p.add_argument("--approve-skin", action="append", default=[], metavar="SKIN-ID",
                   help="Explicitly allow ONE already-reviewed text-free skin ID")
    args = p.parse_args()
    if not args.archive.is_file() or digest(args.archive) != SHA256:
        print("Refusing archive: missing file or unexpected SHA-256", file=sys.stderr)
        return 2
    with tempfile.TemporaryDirectory() as td, zipfile.ZipFile(args.archive) as z:
        root = Path(td)
        for member in z.infolist():
            name = member.filename.replace("\\", "/")
            # No traversal / absolute extraction from downloaded archives.
            if not name.startswith(ARCHIVE_ROOT) or ".." in Path(name).parts:
                print(f"Unsafe archive path: {name}", file=sys.stderr)
                return 2
        z.extractall(root)
        base = root / ARCHIVE_ROOT
        manifest = json.loads((base / "manifest.json").read_text(encoding="utf-8"))
        for entry in manifest["sources"] + manifest["slices"] + manifest["candidate_skins"]:
            fn = base / entry.get("file", entry.get("filename", ""))
            if not fn.is_file() or digest(fn) != entry["sha256"]:
                print(f"Checksum mismatch: {fn}", file=sys.stderr)
                return 3
        print(f"Verified {len(manifest['sources'])} sheets, "
              f"{len(manifest['slices'])} crops, "
              f"{len(manifest['candidate_skins'])} skin candidates")
        ids = set(args.approve_skin)
        valid = {entry["id"]: entry for entry in manifest["candidate_skins"]}
        if ids - set(valid):
            print(f"Unknown skin IDs: {sorted(ids - set(valid))}", file=sys.stderr)
            return 4
        if ids and not args.stage:
            print("Specify --stage when explicitly approving candidates", file=sys.stderr)
            return 4
        if args.stage:
            args.stage.mkdir(parents=True, exist_ok=True)
            for sid in sorted(ids):
                entry = valid[sid]
                if entry["text_embedded"] or entry["status"] != "unapproved_candidate":
                    print(f"Not eligible for text-free candidate staging: {sid}", file=sys.stderr)
                    return 5
                source = base / entry["filename"]
                target = args.stage / source.name
                if target.exists() and digest(target) != digest(source):
                    print(f"Refusing overwrite: {target}", file=sys.stderr)
                    return 6
                shutil.copy2(source, target)
                print(f"Staged for human/game QA: {target}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
