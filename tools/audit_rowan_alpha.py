#!/usr/bin/env python3
"""Diagnostic only: inspect all four *shipped* Rowan Pike alpha masks.
Do not repaint faces, modify masters or replace packaged sprite assets.
"""
from __future__ import annotations
import argparse
import hashlib
import io
import json
from pathlib import Path
import zipfile

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage as ndi

ROOT = Path(__file__).resolve().parents[1]
ARCHIVE = ROOT / "assets/runtime/castle-support-directions.zip"
MASTER = ROOT / "assets/concept-art/v001/04-npcs/rowan-pike-master-concept.png"
VIEWS = ("front", "side", "back", "right")

def sha256(raw):
    return hashlib.sha256(raw).hexdigest()

def analyze(image):
    rgba = np.asarray(image.convert("RGBA"))
    rgb, alpha = rgba[:, :, :3], rgba[:, :, 3]
    opaque = alpha >= 128
    yy, xx = np.nonzero(opaque)
    if len(xx) < 1000:
        raise ValueError("Rowan facing has no usable opaque silhouette")
    x0, x1 = int(xx.min()), int(xx.max()) + 1
    y0, y1 = int(yy.min()), int(yy.max()) + 1
    height = y1 - y0
    top, bottom = y0 + int(height * .02), y0 + int(height * .30)
    head_x = np.nonzero(opaque[top:bottom])[1]
    center = x0 + (x1 - x0) // 2 if len(head_x) == 0 else int(np.median(head_x))
    half = max(8, int((x1 - x0) * .32))
    left, right = max(0, center - half), min(alpha.shape[1], center + half)
    roi = np.zeros_like(opaque, dtype=bool)
    roi[top:bottom, left:right] = True
    enclosed = ndi.binary_fill_holes(opaque) & ~opaque & roi
    narrow = ndi.binary_closing(opaque, structure=np.ones((7, 7), dtype=bool)) & ~opaque & roi
    suspect = enclosed | narrow
    labels, n = ndi.label(suspect, structure=np.ones((3, 3), dtype=bool))
    border = np.zeros_like(opaque, dtype=bool)
    border[:8] = True
    border[-8:] = True
    border[:, :8] = True
    border[:, -8:] = True
    background_pixels = rgb[border & (alpha < 12)]
    if len(background_pixels) < 30:
        background_pixels = rgb[alpha < 12]
    if len(background_pixels) < 30:
        raise ValueError("Cannot estimate transparent source-background RGB")
    background = np.median(background_pixels, axis=0)
    contrast = np.linalg.norm(rgb.astype(np.float32) - background, axis=2)
    components = []
    for label in range(1, n + 1):
        cy, cx = np.nonzero(labels == label)
        if len(cx) < 3:
            continue
        components.append({
            "pixels": int(len(cx)),
            "bbox_xyxy": [int(cx.min()), int(cy.min()), int(cx.max()) + 1, int(cy.max()) + 1],
            "median_source_rgb_distance": round(float(np.median(contrast[cy, cx])), 2),
            "source_contrast_pixels_gt_20": int(np.count_nonzero(contrast[cy, cx] > 20)),
            "fully_enclosed_pixels": int(np.count_nonzero(enclosed[cy, cx])),
        })
    components.sort(key=lambda item: item["pixels"], reverse=True)
    return {
        "dimensions": list(image.size),
        "opaque_bbox_xyxy": [x0, y0, x1, y1],
        "head_inspection_roi_xyxy": [left, top, right, bottom],
        "background_rgb_estimate": [int(v) for v in background],
        "head_enclosed_transparent_pixels": int(enclosed.sum()),
        "head_narrow_notch_pixels": int(narrow.sum()),
        "head_suspect_pixels": int(suspect.sum()),
        "components_ge_3px": components,
        "interpretation": "candidate alpha gaps only; compare actual Castle camera captures",
    }, suspect

def contact_sheet(image, suspect, path):
    rgba = image.convert("RGBA")
    w, h = rgba.size
    canvas = Image.new("RGB", (w * 3, h + 34), "#20242b")
    draw = ImageDraw.Draw(canvas)
    for col, bg in enumerate(("#c8c4bb", "#30363d", "#c8c4bb")):
        tile = Image.new("RGBA", (w, h), bg)
        tile.alpha_composite(rgba)
        if col == 1:
            tile = Image.merge("RGBA", (rgba.getchannel("A"),) * 3
                               + (Image.new("L", (w, h), 255),))
        if col == 2:
            pixels = np.zeros((h, w, 4), dtype=np.uint8)
            pixels[suspect] = (255, 25, 25, 225)
            tile.alpha_composite(Image.fromarray(pixels, "RGBA"))
        canvas.paste(tile.convert("RGB"), (col * w, 34))
    for col, title in enumerate(("SHIPPED RGBA", "ALPHA", "CANDIDATES (RED)")):
        draw.text((col * w + 7, 7), title, fill="white")
    canvas.save(path, optimize=True)

def self_test():
    rgb = np.zeros((96, 64, 4), dtype=np.uint8)
    rgb[:, :, :3] = 12
    rgb[8:88, 16:48, :3] = (125, 95, 75)
    rgb[8:88, 16:48, 3] = 255
    rgb[18:22, 28:32, 3] = 0
    stats, suspect = analyze(Image.fromarray(rgb, "RGBA"))
    assert stats["head_enclosed_transparent_pixels"] >= 16, stats
    assert np.all(suspect[18:22, 28:32]), stats
    print("PASS ROWAN_ALPHA_SYNTHETIC_HOLE_DETECTION")

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, default=ROOT / "build/rowan-alpha-review")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return
    args.out.mkdir(parents=True, exist_ok=True)
    archive_bytes = ARCHIVE.read_bytes()
    master_bytes = MASTER.read_bytes()
    records = []
    with zipfile.ZipFile(io.BytesIO(archive_bytes)) as source:
        for view in VIEWS:
            name = f"assets/castle_support/rowan_pike/{view}.png"
            raw = source.read(name)
            image = Image.open(io.BytesIO(raw)).convert("RGBA")
            stats, suspect = analyze(image)
            stats.update({"identity": "rowan_pike", "view": view,
                          "entry": name, "png_sha256": sha256(raw)})
            contact_sheet(image, suspect, args.out / f"rowan-{view}-alpha.png")
            records.append(stats)
            print("ROWAN_ALPHA_VIEW", view,
                  "enclosed=", stats["head_enclosed_transparent_pixels"],
                  "notch=", stats["head_narrow_notch_pixels"],
                  "components=", len(stats["components_ge_3px"]), flush=True)
    report = {
        "source": "actual packaged four-view Castle support sprite ZIP",
        "runtime_archive_sha256": sha256(archive_bytes),
        "original_master_sha256": sha256(master_bytes),
        "source_art_modified": False,
        "runtime_asset_modified": False,
        "visual_acceptance": "UNREVIEWED; compare matching real Castle viewpoint/lighting screenshots",
        "views": records,
    }
    (args.out / "rowan-alpha-report.json").write_text(json.dumps(report, indent=2) + "\n")
    print("ROWAN_ALPHA_DIAGNOSTIC_ONLY", len(records), "views; no art changes")

if __name__ == "__main__":
    main()
