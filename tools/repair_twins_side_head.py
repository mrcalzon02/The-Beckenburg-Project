#!/usr/bin/env python3
"""Restore missing blue Twin's side-view face from the canonical paired master.

Only twins/a/side.png is changed; all seven other approved siblings' source
pixels stay byte-identical. No generated faces or painted replacement geometry.
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
MASTER = ROOT / "assets/concept-art/v001/03-ghosts/twins-ghost-profile.png"
ARCHIVE = ROOT / "assets/runtime/twins-directions.zip"
MANIFEST = ROOT / "assets/runtime/twins-directions-manifest.json"
PREVIEW = ROOT / "development/verification/twins-side-head-source-repair.png"
ENTRY = "twins/a/side.png"
SOURCE_BOUNDS = (242, 281, 316, 614)


def restore(original: Image.Image) -> Image.Image:
    rgb = np.asarray(original.crop(SOURCE_BOUNDS).convert("RGB"))
    height, width = rgb.shape[:2]
    visible = np.uint8((rgb.max(axis=2) > 18) * 255)
    edge = max(3, min(width, height) // 24)
    visible[:edge, :] = visible[-edge:, :] = 0
    visible[:, :edge] = visible[:, -edge:] = 0
    visible = cv2.morphologyEx(
        visible, cv2.MORPH_CLOSE,
        cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (7, 7)),
    )
    count, labels, stats, _ = cv2.connectedComponentsWithStats(visible, 8)
    if count <= 1:
        raise ValueError("Approved blue Twin source silhouette missing")
    principal = max(range(1, count), key=lambda i: int(stats[i, cv2.CC_STAT_AREA]))
    mask = np.uint8((labels == principal) * 255)
    contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    solid = np.zeros_like(mask)
    cv2.drawContours(solid, contours, -1, 255, -1)
    # The blue sister's dark side-facing hair/face was punched full of holes
    # by the old GrabCut. Reconnect its EXISTING source pixels by filling only
    # the convex head region. Original source RGB is never painted or replaced.
    head = solid[:110, :]
    ys, xs = np.nonzero(head >= 128)
    if len(xs) <= 50:
        raise ValueError("Missing source hair/face contour")
    hull = cv2.convexHull(np.column_stack((xs, ys)).astype(np.int32))
    cv2.fillConvexPoly(head, hull, 255)
    alpha = cv2.GaussianBlur(solid, (3, 3), .5)
    rgba = Image.fromarray(np.dstack((rgb, alpha)), "RGBA")
    bounds = rgba.getbbox()
    if bounds is None or bounds[1] > 45 or bounds[3] < 290:
        raise ValueError("Head or feet absent in repaired blue Twin")
    rgba = rgba.crop(bounds)
    factor = min(116 / rgba.width, 214 / rgba.height)
    tw = max(1, round(rgba.width * factor))
    th = max(1, round(rgba.height * factor))
    output = Image.new("RGBA", (128, 224), (0, 0, 0, 0))
    output.alpha_composite(
        rgba.resize((tw, th), Image.Resampling.LANCZOS),
        ((128 - tw) // 2, 224 - th),
    )
    arr = np.asarray(output.getchannel("A"))
    if min(
        int(np.count_nonzero(arr[12:60] >= 128)) - 1200,
        int(np.count_nonzero(arr[75:145] >= 128)) - 1800,
        int(np.count_nonzero(arr[178:219] >= 128)) - 650,
    ) <= 0:
        raise ValueError("Blue Twin lost face, body or feet")
    return output


def main() -> None:
    source = MASTER.read_bytes()
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    if hashlib.sha256(source).hexdigest() != manifest["source_sha256"]:
        raise ValueError("Approved Twins source has changed")
    if Image.open(io.BytesIO(source)).size != (1448, 1086):
        raise ValueError("Unexpected canonical paired master dimensions")
    with zipfile.ZipFile(ARCHIVE) as source_zip:
        original = {name: source_zip.read(name) for name in source_zip.namelist()}
    if len(original) != 8 or ENTRY not in original:
        raise ValueError("Expected exactly eight independently approved Twin views")
    old = Image.open(io.BytesIO(original[ENTRY])).convert("RGBA")
    repaired = restore(Image.open(io.BytesIO(source)).convert("RGB"))
    buff = io.BytesIO()
    repaired.save(buff, format="PNG")
    new_bytes = buff.getvalue()
    for name in original:
        if name != ENTRY and name != manifest["directions"][name.split("/")[2].removesuffix(".png")][name.split("/")[1]]["path"]:
            raise ValueError("Twin source identity registry changed")
    entries = {**original, ENTRY: new_bytes}
    with zipfile.ZipFile(ARCHIVE, "w", compression=zipfile.ZIP_DEFLATED,
                         compresslevel=9) as archive:
        for name, payload in sorted(entries.items()):
            info = zipfile.ZipInfo(name, date_time=(2026, 10, 21, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(info, payload)
    record = manifest["directions"]["side"]["a"]
    alpha = np.asarray(repaired.getchannel("A"))
    record.update(
        sha256=hashlib.sha256(new_bytes).hexdigest(),
        visible_pixels=int(np.count_nonzero(alpha >= 24)),
        head_pixels=int(np.count_nonzero(alpha[6:62] >= 24)),
        torso_pixels=int(np.count_nonzero(alpha[64:158] >= 24)),
        feet_pixels=int(np.count_nonzero(alpha[159:220] >= 24)),
    )
    manifest["side_head_source_repair"] = {
        "source_crop": list(SOURCE_BOUNDS),
        "repaired_entry": ENTRY,
        "unchanged_entries": 7,
        "source_rgb_unchanged": True,
        "renderer_verification": "pending four-angle in-game visual comparison",
    }
    manifest["archive_sha256"] = hashlib.sha256(ARCHIVE.read_bytes()).hexdigest()
    MANIFEST.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    preview = Image.new("RGBA", (256, 224), (80, 80, 80, 255))
    preview.alpha_composite(old, (0, 0))
    preview.alpha_composite(repaired, (128, 0))
    preview.save(PREVIEW)
    print("PASS blue Twin side face restored from exact source; other seven PNG bytes preserved")


if __name__ == "__main__":
    main()
