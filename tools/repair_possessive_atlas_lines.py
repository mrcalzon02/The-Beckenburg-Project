#!/usr/bin/env python3
"""Remove four detached border-line alpha artifacts from the shipped Possessive.

Reassembles exact nine committed v3 binary parts; edits only alpha pixels
outside the largest connected real ghost body, in the four verified edge strips.
No RGB paint, original master edit, fabricated source art, or atlas replacement.
Other 180+ ghost atlas PNG entries are retained byte-for-byte.
"""
from __future__ import annotations
import hashlib
import io
import json
import zipfile
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
PARTS = ROOT / "assets/runtime/ghost-sprite-parts"
PREFIX = "ghost-sprite-expansion-v3.zip.part"
PREVIEW = ROOT / "development/verification/possessive-source-line-alpha-repair.png"
REPORT = ROOT / "development/verification/possessive-source-line-alpha-repair.json"
CELL_W, CELL_H, COLS, ROWS = 64, 112, 4, 8
STRIPS = {
    "front": (53, 64),
    "back_right": (0, 12),
    "back": (0, 12),
    "back_left": (52, 64),
}
DIRECTIONS = ("front", "front_right", "right", "back_right",
              "back", "back_left", "left", "front_left")


def repair_sheet(original: bytes, facing: str) -> tuple[bytes, dict]:
    image = Image.open(io.BytesIO(original)).convert("RGBA")
    if image.size != (CELL_W * COLS, CELL_H * ROWS):
        raise ValueError(f"Unexpected shipped Possessive {facing} atlas dimensions")
    array = np.asarray(image).copy()
    xlo, xhi = STRIPS[facing]
    transparent = 0
    strong = 0
    untouched_body = 0
    preserved_faded_frames = 0
    for row in range(ROWS):
        for col in range(COLS):
            tile = array[row * CELL_H:(row + 1) * CELL_H,
                         col * CELL_W:(col + 1) * CELL_W]
            alpha = tile[:, :, 3]
            count, labels, stats, _ = cv2.connectedComponentsWithStats(
                np.uint8(alpha >= 24), 8
            )
            if count < 2:
                raise ValueError(f"Missing original ghost body: {facing} {row}/{col}")
            principal = max(range(1, count), key=lambda n: int(stats[n, cv2.CC_STAT_AREA]))
            if stats[principal, cv2.CC_STAT_AREA] < 400:
                # The source atlas also contains intentional fade/dissipate
                # frames with only a few surviving bright pixels. Those are
                # not a missing/defective body; do not strip or repaint them.
                preserved_faded_frames += 1
                continue
            # One-pixel fringe around connected source body protects antialias
            # coverage at the clothing edge, but cannot reach the thin border.
            keep = cv2.dilate(
                np.uint8(labels == principal),
                np.ones((3, 3), dtype=np.uint8),
                iterations=1,
            ) != 0
            strip = np.zeros_like(alpha, dtype=bool)
            strip[:, xlo:xhi] = ~keep[:, xlo:xhi]
            strong += int(np.count_nonzero(strip & (alpha >= 24)))
            transparent += int(np.count_nonzero(strip & (alpha != 0)))
            untouched_body += int(np.count_nonzero(keep[:, xlo:xhi] & (alpha[:, xlo:xhi] >= 24)))
            alpha[strip] = 0
            if np.any((alpha[:, xlo:xhi] >= 24) & ~keep[:, xlo:xhi]):
                raise AssertionError("Detached edge line survived the cleanup")
    if strong < 150:
        raise ValueError(f"Expected real detached source line in {facing}, got {strong}")
    # The original RGB pixels are preserved even under newly transparent alpha.
    if not np.array_equal(array[:, :, :3], np.asarray(image)[:, :, :3]):
        raise AssertionError("Possessive source RGB changed")
    output = io.BytesIO()
    Image.fromarray(array, "RGBA").save(output, format="PNG")
    return output.getvalue(), {
        "facing": facing, "detached_alpha_pixels_cleared": transparent,
        "visible_line_pixels_cleared": strong,
        "protected_original_body_pixels": untouched_body,
        "preserved_faded_source_frames": preserved_faded_frames,
        "source_rgb_unchanged": True,
    }


def main() -> None:
    part_paths = [PARTS / (PREFIX + f"{i:02d}") for i in range(9)]
    if any(not p.is_file() or p.stat().st_size < 100 for p in part_paths):
        raise ValueError("Missing actual nine-part shipped v3 archive")
    original_parts = [p.read_bytes() for p in part_paths]
    original = b"".join(original_parts)
    with zipfile.ZipFile(io.BytesIO(original)) as z:
        original_entries = {name: z.read(name) for name in z.namelist()}
        source_info = list(z.infolist())
    expected = {"frames/possessive/" + facing + ".png" for facing in DIRECTIONS}
    if not expected.issubset(original_entries):
        raise ValueError("Incomplete approved Possessive source views")

    changed = {}
    diagnostics = []
    for facing in STRIPS:
        key = f"frames/possessive/{facing}.png"
        changed[key], detail = repair_sheet(original_entries[key], facing)
        detail["original_sha256"] = hashlib.sha256(original_entries[key]).hexdigest()
        detail["corrected_sha256"] = hashlib.sha256(changed[key]).hexdigest()
        diagnostics.append(detail)

    # A source comparison uses the exact first idle frame, not new concept art.
    contact = Image.new("RGBA", (len(DIRECTIONS) * 128, 224), (78, 78, 78, 255))
    for index, facing in enumerate(DIRECTIONS):
        key = f"frames/possessive/{facing}.png"
        for y, payload in ((0, original_entries[key]), (112, changed.get(key, original_entries[key]))):
            frame = Image.open(io.BytesIO(payload)).convert("RGBA").crop((0, 0, 64, 112))
            contact.alpha_composite(frame, (index * 128, y))
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    contact.save(PREVIEW)

    binary = io.BytesIO()
    with zipfile.ZipFile(binary, "w") as zout:
        for info in source_info:
            zout.writestr(info, changed.get(info.filename, original_entries[info.filename]))
    patched = binary.getvalue()
    with zipfile.ZipFile(io.BytesIO(patched)) as verify:
        if len(verify.namelist()) != len(original_entries):
            raise AssertionError("Ghost identity archive entry count changed")
        for name, payload in original_entries.items():
            if name not in changed and verify.read(name) != payload:
                raise AssertionError(f"Unrelated ghost sprite changed: {name}")
    # Existing packaging assembles the 9 parts in numeric order. Preserve all
    # eight original 768 KiB part boundaries and one nonempty final remainder.
    split = len(original_parts[0])
    if any(len(x) != split for x in original_parts[:-1]):
        raise ValueError("Unexpected source v3 part layout")
    if not (split * 8 < len(patched) < split * 9):
        raise ValueError("Patched archive does not fit exactly nine parts")
    for index, part in enumerate(part_paths):
        part.write_bytes(patched[index * split:(index + 1) * split])

    REPORT.write_text(json.dumps({
        "source_archive_sha256": hashlib.sha256(original).hexdigest(),
        "repaired_archive_sha256": hashlib.sha256(patched).hexdigest(),
        "changed_pngs": sorted(changed),
        "unchanged_png_entries": len(original_entries) - len(changed),
        "source_rgb_preserved": True,
        "runtime_visual_approval": "pending post-commit game renderer capture",
        "repairs": diagnostics,
    }, indent=2) + "\n", encoding="utf-8")
    print("PASS four source-backed Possessive line repairs; "
          f"{len(original_entries) - len(changed)} other source PNG entries identical")


if __name__ == "__main__":
    main()
