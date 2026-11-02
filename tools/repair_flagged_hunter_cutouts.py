#!/usr/bin/env python3
"""Repair only the eight Hunter cutouts with missing source heads/boots.

Uses the exact approved master boards, not generated or painted pixels.
The other 16 character images are copied byte-for-byte from the existing ZIP.
Visual acceptance remains advisory and requires real renderer screenshots.
"""
from __future__ import annotations

import hashlib
import io
import json
import zipfile
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ARCHIVE = ROOT / "assets/runtime/character-directions.zip"
MANIFEST = ROOT / "assets/runtime/character-directions-manifest.json"
PREVIEW = ROOT / "development/verification/flagged-hunter-source-repair.png"
VIEWS = ("front", "side", "back", "three_quarter")

# Original approved source columns, with the *real* boot rows restored.
# The legacy rectangles stopped at y=685/690, amputating all four boot pairs.
# y=706 is beyond the photographed soles, but above the printed view labels.
SOURCES = {
    "observer": (
        ROOT / "assets/concept-art/v001/01-hunters/observer-character-profile.png",
        ((24, 294, 206, 706), (215, 294, 370, 706),
         (383, 294, 554, 706), (574, 294, 735, 706)),
    ),
    "demonologist": (
        ROOT / "assets/concept-art/v001/01-hunters/demonologist-character-profile.png",
        ((23, 291, 208, 706), (215, 291, 371, 706),
         (380, 291, 544, 706), (550, 291, 734, 706)),
    ),
}


def source_silhouette(board: Image.Image, bounds: tuple[int, int, int, int]) -> Image.Image:
    """Source-colour foreground matte; retain dark enclosed clothing and hair.

    The old torso/leg GrabCut forced rectangles marked the dark source plate as
    foreground and discarded rear heads; high-contrast source silhouettes
    determine the shape here, with enclosed dark folds restored by contours.
    """
    rgb = np.asarray(board.crop(bounds).convert("RGB"))
    height, width = rgb.shape[:2]
    visible = np.uint8((rgb.max(axis=2) > 18) * 255)
    edge = max(5, width // 32)
    visible[:edge, :] = visible[-edge:, :] = 0
    visible[:, :edge] = visible[:, -edge:] = 0
    visible = cv2.morphologyEx(
        visible, cv2.MORPH_CLOSE,
        cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (9, 9)),
    )
    components, labels, stats, _ = cv2.connectedComponentsWithStats(visible, 8)
    if components < 2:
        raise ValueError("Master source contains no separable body")
    body = max(range(1, components), key=lambda i: int(stats[i, cv2.CC_STAT_AREA]))
    alpha = np.uint8((labels == body) * 255)
    # Fill only truly enclosed dark patches; exterior gaps between legs remain.
    contours, _ = cv2.findContours(alpha, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    solid = np.zeros_like(alpha)
    cv2.drawContours(solid, contours, -1, 255, -1)
    alpha = cv2.GaussianBlur(solid, (3, 3), .5)
    patch = Image.fromarray(np.dstack((rgb, alpha)), "RGBA")
    actual = patch.getbbox()
    if actual is None:
        raise ValueError("Empty sprite after source alpha segmentation")
    patch = patch.crop(actual)
    size = min(115 / patch.width, 215 / patch.height)
    rw = max(1, round(patch.width * size))
    rh = max(1, round(patch.height * size))
    frame = Image.new("RGBA", (128, 224), (0, 0, 0, 0))
    frame.alpha_composite(
        patch.resize((rw, rh), Image.Resampling.LANCZOS),
        ((128 - rw) // 2, 224 - rh),
    )
    check_frame(frame)
    return frame


def check_frame(frame: Image.Image) -> None:
    """Catch the previous amputated heads and missing shoes objectively."""
    alpha = np.asarray(frame.getchannel("A"))
    for label, lo, hi, minimum in (
        ("head", 12, 60, 700),
        ("torso", 75, 145, 1900),
        ("feet", 178, 219, 650),
    ):
        if int(np.count_nonzero(alpha[lo:hi] >= 128)) < minimum:
            raise ValueError("Source cutout lost " + label)
    if frame.size != (128, 224):
        raise ValueError("Sprite world-space canvas changed")


def repair() -> None:
    old_zip_bytes = ARCHIVE.read_bytes()
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    with zipfile.ZipFile(io.BytesIO(old_zip_bytes)) as archive:
        original = {name: archive.read(name) for name in archive.namelist()}
    if len(original) != 24:
        raise ValueError("Expected the full 24 approved source cutouts")
    changed = {}
    sheet = Image.new("RGB", (4 * 128 * 2, 4 * 224), (35, 37, 41))
    draw = ImageDraw.Draw(sheet)
    for row, (identity, (source_path, rects)) in enumerate(SOURCES.items()):
        master_bytes = source_path.read_bytes()
        if hashlib.sha256(master_bytes).hexdigest() != manifest["variants"][identity]["source_sha256"]:
            raise ValueError("Approved master hash changed for " + identity)
        board = Image.open(io.BytesIO(master_bytes)).convert("RGB")
        if board.size != (1448, 1086):
            raise ValueError("Unexpected original master dimensions")
        for col, (facing, bounds) in enumerate(zip(VIEWS, rects)):
            name = f"assets/{identity}/{facing}.png"
            if name not in original:
                raise ValueError("Missing original approved direction " + name)
            old = Image.open(io.BytesIO(original[name])).convert("RGBA")
            repaired = source_silhouette(board, bounds)
            data = io.BytesIO()
            repaired.save(data, format="PNG", optimize=False)
            changed[name] = data.getvalue()
            for side, image in ((0, old), (1, repaired)):
                tile = Image.new("RGBA", image.size, (80, 80, 80, 255))
                tile.alpha_composite(image)
                sheet.paste(tile.convert("RGB"), (col * 256 + side * 128, row * 448))
            record = manifest["variants"][identity]["views"][facing]
            record.update(
                bytes=len(changed[name]),
                sha256=hashlib.sha256(changed[name]).hexdigest(),
                visible_pixels=int(np.count_nonzero(
                    np.asarray(repaired.getchannel("A")) >= 24
                )),
                crop=list(bounds),
            )
            print(f"REPAIRED_SOURCE {identity}/{facing}: "
                  f"{len(changed[name])} bytes original={len(original[name])}")
    # All 16 unrelated frames remain identical by actual PNG bytes.
    entries = {**original, **changed}
    if any(entries[name] != blob for name, blob in original.items() if name not in changed):
        raise AssertionError("Unrelated character frame modified")
    with zipfile.ZipFile(ARCHIVE, "w", compression=zipfile.ZIP_DEFLATED,
                         compresslevel=9) as archive:
        for name, blob in sorted(entries.items()):
            info = zipfile.ZipInfo(name, date_time=(2026, 10, 20, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(info, blob, compress_type=zipfile.ZIP_DEFLATED,
                             compresslevel=9)
    manifest["archive_sha256"] = hashlib.sha256(ARCHIVE.read_bytes()).hexdigest()
    manifest["source_silhouette_repair"] = {
        "targets": sorted(changed),
        "unchanged_source_frames": 16,
        "method": "original-board silhouette, enclosed dark-detail fill, boots through row 706",
        "visual_approval": "requires post-commit in-engine screenshots",
    }
    MANIFEST.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(PREVIEW)
    print("PASS eight source-preserving repaired Hunter views; 16 untouched frames")


if __name__ == "__main__":
    repair()
