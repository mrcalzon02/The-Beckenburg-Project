#!/usr/bin/env python3
"""Pinned-source Beckenburg extraction, preserving all original bytes except audited edits."""
from __future__ import annotations
import hashlib, json, sys
from pathlib import Path

SOURCE_SHA = "a4e0da912d4c297830e4c31b03362086310fd4cd"
NEW_REPO = "mrcalzon02/The-Beckenburg-Project"
OLD_REPO = "mrcalzon02/HB-TTRPG-tools"

def rename(path):
    for a, b in (("software/beckenburg/", ""), ("docs/beckenburg/", "docs/"), ("assets/beckenburg/", "assets/")):
        if path.startswith(a): return b + path[len(a):]
    if path in ("beckenburg.html", "beckenburg-asset-libraries.html",
                "styles.css", "workspace-landing.css"): return path
    return None

def rewrite(path, content):
    changed = None
    if path.endswith("/src/main/java/org/beckenburg/BeckenburgUpdates.java"):
        changed = content.decode("utf-8")
        old = 'REPOSITORY="mrcalzon02/HB-TTRPG-tools"'
        assert changed.count(old) == 1
        changed = changed.replace(old, 'REPOSITORY="mrcalzon02/The-Beckenburg-Project"')
    elif path.endswith("/src/test/java/org/beckenburg/BeckenburgUpdatesTest.java"):
        changed = content.decode("utf-8").replace(
            "https://github.com/mrcalzon02/HB-TTRPG-tools/releases/download/beckenburg-v",
            "https://github.com/mrcalzon02/The-Beckenburg-Project/releases/download/beckenburg-v")
    elif path == "software/beckenburg/README.md":
        changed = content.decode("utf-8").replace("../../docs/beckenburg/", "docs/")
    elif path == "software/beckenburg/RELEASES.md":
        changed = content.decode("utf-8")
        changed = changed.replace("mrcalzon02/HB-TTRPG-tools", NEW_REPO)
        changed += "\n\nStandalone migration: initial release version floor 0.1.13; the old repository retains 0.1.11 and earlier. A migration-only 0.1.12 bridge must be verified before old clients can update to this independent release channel.\n"
    elif path in ("beckenburg.html", "beckenburg-asset-libraries.html"):
        changed = content.decode("utf-8")
        for original, new in (
            ("https://mrcalzon02.github.io/HB-TTRPG-tools", "https://mrcalzon02.github.io/The-Beckenburg-Project"),
            (OLD_REPO, NEW_REPO), ("software/beckenburg/", ""), ("docs/beckenburg/", "docs/"),
            ("assets/beckenburg/", "assets/")):
            changed = changed.replace(original, new)
    return changed.encode("utf-8") if changed is not None else content

def main():
    source, dest = [Path(x).resolve() for x in sys.argv[1:3]]
    assert source.is_dir() and (dest/".git").is_dir()
    manifest, seen = [], set()
    for original in sorted(source.rglob("*")):
        if not original.is_file() or ".git" in original.parts: continue
        path = original.relative_to(source).as_posix()
        target = rename(path)
        if target is None: continue
        assert target not in seen, "Destination collision: " + target
        seen.add(target)
        target_path = dest/target
        assert not target_path.exists(), "Refusing to overwrite " + target
        data = original.read_bytes()
        output = rewrite(path, data)
        target_path.parent.mkdir(parents=True, exist_ok=True)
        target_path.write_bytes(output)
        src_digest = hashlib.sha256(data).hexdigest()
        dst_digest = hashlib.sha256(target_path.read_bytes()).hexdigest()
        assert dst_digest == hashlib.sha256(output).hexdigest()
        manifest.append({"source":path,"dest":target,"source_sha256":src_digest,
                         "destination_sha256":dst_digest,"source_bytes":len(data),
                         "destination_bytes":len(output),"changed":src_digest!=dst_digest})
    count, size = len(manifest), sum(x["source_bytes"] for x in manifest)
    assert count >= 1500 and size > 600_000_000, (count, size)
    required = ("build.cmd", "build.sh", "src/main/java/org/beckenburg/InvestigationGame.java",
        "src/main/java/org/beckenburg/BeckenburgUpdates.java",
        "src/test/java/org/beckenburg/BeckenburgUpdatesTest.java",
        "assets/runtime/directional-sprites.zip",
        "assets/runtime/ghost-sprite-parts/ghost-sprite-expansion-v3.zip.part00",
        "assets/runtime/elder-masters/elder-nothic.png",
        "assets/concept-art/v001/03-ghosts/elder-plague-demon-master-concept.png",
        "assets/libraries/household/beckenburg-household-cc0.zip",
        "development/levels/blender/house-complete.blend", "docs/stage-ledger.json",
        "beckenburg.html", "beckenburg-asset-libraries.html",
        "styles.css", "workspace-landing.css")
    for file in required: assert file in seen, "Missing required file " + file
    (dest/"index.html").write_bytes((dest/"beckenburg.html").read_bytes())
    (dest/".gitignore").write_text(
        "build/\n*.class\n*.log\n.DS_Store\nThumbs.db\n__pycache__/\n*.pyc\n", encoding="utf-8")
    info_dir = dest/"docs/migration"
    info_dir.mkdir(parents=True, exist_ok=True)
    (info_dir/"CUTOVER.md").write_text(
        "# Beckenburg standalone migration (2026-10-20)\n\n"
        "Source development is frozen in " + OLD_REPO + ". All future gameplay work belongs in " + NEW_REPO + ".\n\n"
        "Pinned source SHA: " + SOURCE_SHA + "\n\n"
        "The complete game-specific Java source, tests, runtime assets, Blender scenes, "
        "curated libraries, docs and relevant CI workflows were imported and hashed. "
        "See IMPORT_MANIFEST.json for every original-to-destination path, size and SHA-256.\n\n"
        "Do not mark the cutover complete until GitHub-hosted Windows build, actual packaged "
        "files, independent release, and old-version update bridge have been verified.\n"
        "Existing old releases (through 0.1.11) remain historical. First new release floor: 0.1.13. "
        "The migration-only 0.1.12 source-repo bridge is required for older installed clients.\n",
        encoding="utf-8")
    output = {"source_repository":OLD_REPO,"source_commit":SOURCE_SHA,
       "destination_repository":NEW_REPO,"file_count":count,"source_bytes":size,
       "changed_files":sum(x["changed"] for x in manifest),"files":manifest}
    (info_dir/"IMPORT_MANIFEST.json").write_text(json.dumps(output,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({k:output[k] for k in ("source_commit","file_count","source_bytes","changed_files")}))
    print("PASS: full mapped extraction and file hashes")

if __name__=="__main__": main()
