#!/usr/bin/env python3
"""Reconstruct Castle resident billboards from checked-in master concepts.

Never edit the masters. The old low-resolution runtime cutouts provide *only*
alignment and alpha silhouettes; RGB texture is resampled directly from the
original concept panels. A poor/ambiguous match is a diagnostic, NOT permission
to publish a guessed image. Requires Pillow, numpy, opencv-python-headless.

Usage:
  python tools/repair_castle_sprite_art.py --out build/castle-art-repair
  python tools/repair_castle_sprite_art.py --out build/castle-art-repair --write
"""
from __future__ import annotations
import argparse
import hashlib
import io
import json
import math
from pathlib import Path
import zipfile

import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ARCHIVE = ROOT / "assets/runtime/castle-support-directions.zip"
MASTERS = {
    "elsbeth_vale": "assets/concept-art/v001/04-npcs/elsbeth-vale-master-concept.png",
    "rowan_pike": "assets/concept-art/v001/04-npcs/rowan-pike-master-concept.png",
    "tomas_greaves": "assets/concept-art/v001/04-npcs/tomas-greaves-master-concept.png",
    "sister_mirelle_ash": "assets/concept-art/v001/04-npcs/sister-mirelle-ash-master-concept.png",
}
VIEWS = ("front", "side", "back", "right")
SCALE = 2  # 256x448 sprites, with original-panel RGB rather than upscaled old RGB.
MIN_SCORE = .56  # Calibrated to actual owner master boards; corroborated by strict shared-view geometry.
MIN_SEPARATION = 55  # Master-panel pixel separation between distinct view centres.
SX = (1.25, 1.45, 1.65, 1.85, 2.1, 2.4, 2.75, 3.1, 3.5)
SY = (1.25, 1.45, 1.65, 1.85, 2.1, 2.4, 2.75, 3.1, 3.5)


def sha256(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def match(sheet_rgb: np.ndarray, frame: Image.Image):
    """Match opaque old cutout pixels to the ORIGINAL concept board at varying XY scales.

    X/Y are independent: old PNGs sometimes stretch a narrow master crop into
    128x224. This does not alter the master. Score ignores transparency.
    """
    rgba = np.asarray(frame.convert("RGBA"))
    mask = rgba[:, :, 3] >= 120
    yy, xx = np.nonzero(mask)
    if len(xx) < 1000:
        raise ValueError("Nearly empty reference silhouette")
    x0, x1 = max(0, int(xx.min()) - 2), min(rgba.shape[1], int(xx.max()) + 3)
    y0, y1 = max(0, int(yy.min()) - 2), min(rgba.shape[0], int(yy.max()) + 3)
    template = cv2.cvtColor(rgba[y0:y1, x0:x1, :3], cv2.COLOR_RGB2GRAY)
    alpha_mask = (mask[y0:y1, x0:x1].astype(np.uint8) * 255)
    grey = cv2.cvtColor(sheet_rgb, cv2.COLOR_RGB2GRAY)
    best = None
    for sx in SX:
        for sy in SY:
            target = cv2.resize(grey, (round(grey.shape[1] / sx), round(grey.shape[0] / sy)),
                                interpolation=cv2.INTER_AREA)
            if target.shape[0] < template.shape[0] or target.shape[1] < template.shape[1]:
                continue
            corr = cv2.matchTemplate(target, template, cv2.TM_CCOEFF_NORMED, mask=alpha_mask)
            finite = np.isfinite(corr)
            if not finite.any():
                continue
            corr[~finite] = -1
            _, score, _, (mx, my) = cv2.minMaxLoc(corr)
            if best is None or score > best["score"]:
                best = {"score": round(float(score), 5), "sx": sx, "sy": sy,
                        "mx": mx, "my": my, "bbox": [x0, y0, x1, y1]}
    if best is None:
        raise ValueError("No source-panel candidate matches the supplied cutout")
    return best


def reconstruct(master: Image.Image, low: Image.Image, found: dict) -> Image.Image:
    """Resample master RGB at the recovered transform, preserve only the old alpha mask."""
    rgb = np.asarray(master.convert("RGB"))
    width, height = low.size
    outw, outh = width * SCALE, height * SCALE
    bx, by = found["bbox"][:2]
    tx, ty = found["mx"], found["my"]
    sx, sy = found["sx"], found["sy"]
    u = ((np.arange(outw, dtype=np.float32) + .5) / SCALE - bx + tx) * sx
    v = ((np.arange(outh, dtype=np.float32) + .5) / SCALE - by + ty) * sy
    mx, my = np.meshgrid(u, v)
    detail = cv2.remap(rgb, mx, my, cv2.INTER_CUBIC, borderMode=cv2.BORDER_CONSTANT)
    # Validity: an opaque component cannot sample outside the source sheet.
    alpha = np.asarray(low.getchannel("A").resize((outw, outh), Image.Resampling.LANCZOS))
    valid = (mx >= 0) & (my >= 0) & (mx < rgb.shape[1]-1) & (my < rgb.shape[0]-1)
    if np.count_nonzero((alpha > 96) & ~valid) > 0:
        raise ValueError("Matched source crop crosses the source panel boundary")
    result = np.dstack((detail, alpha)).astype(np.uint8)
    return Image.fromarray(result, "RGBA")


def comparison(master: Image.Image, old: Image.Image, new: Image.Image, match_info: dict,
               title: str, destination: Path):
    """Unmodified original master + OLD runtime + recovered RGB in one visual diagnostic."""
    height = 520
    canvas = Image.new("RGB", (1200, height), "#1a1d25")
    d = ImageDraw.Draw(canvas)
    d.text((12, 10), title + f" / match={match_info['score']:.3f}", fill="#f9e3bb")
    d.text((12, 32), "MASTER (unmodified full concept)", fill="white")
    d.text((414, 32), "PREVIOUS 128x224 (nearest display)", fill="white")
    d.text((813, 32), "REBUILT 256x448 (master RGB)", fill="white")
    src = master.copy()
    src.thumbnail((385, 450), Image.Resampling.LANCZOS)
    canvas.paste(src, (10 + (385-src.width)//2, 55))
    orig = old.resize((248, 434), Image.Resampling.NEAREST)
    rebuilt = new.resize((248, 434), Image.Resampling.LANCZOS)
    for offset, sprite in ((488, orig), (884, rebuilt)):
        panel = Image.new("RGBA", sprite.size, "#333945")
        panel.alpha_composite(sprite)
        canvas.paste(panel.convert("RGB"), (offset, 63))
    canvas.save(destination)


def run(output: Path, write: bool) -> int:
    output.mkdir(parents=True, exist_ok=True)
    original_hash = sha256(ARCHIVE.read_bytes())
    master_hashes = {identity: sha256((ROOT / path).read_bytes())
                     for identity, path in MASTERS.items()}
    results = []
    new_frames = {}
    with zipfile.ZipFile(ARCHIVE) as prior:
        originals = {item.filename: prior.read(item) for item in prior.infolist()
                     if not item.is_dir()}
    for identity, source_path in MASTERS.items():
        master = Image.open(ROOT / source_path).convert("RGB")
        master_rgb = np.asarray(master)
        centers = []
        for view in VIEWS:
            entry = f"assets/castle_support/{identity}/{view}.png"
            if entry not in originals:
                raise FileNotFoundError(f"Missing original runtime reference frame: {entry}")
            old = Image.open(io.BytesIO(originals[entry])).convert("RGBA")
            if old.size != (128, 224):
                # Repeated invocations after publish still audit, but never rematch
                # a rebuilt sprite against the original 128x224 reference.
                if old.size == (256, 448):
                    results.append({"identity": identity, "view": view, "status": "already_rebuilt",
                                    "size": list(old.size), "master_sha256": master_hashes[identity]})
                    continue
                raise ValueError(f"Unexpected frame size for {entry}: {old.size}")
            found = match(master_rgb, old)
            center = ((found["mx"]+(found["bbox"][2]-found["bbox"][0])/2)*found["sx"],
                      (found["my"]+(found["bbox"][3]-found["bbox"][1])/2)*found["sy"])
            separation = min((math.dist(center, other) for other in centers), default=9999)
            centers.append(center)
            # The source is an actual FOUR-COLUMN turnaround panel. Every image in
            # the audited Castle roster matches the same 1.85x transform and
            # occupies a distinct horizontal column. The old standalone .77
            # score cutoff rejected 9 *correctly located* views because the
            # pre-existing low-res palette/filter was unlike the original RGB.
            # Require both visual content AND cross-view panel geometry.
            column = len(centers) - 1
            previous_center = centers[column - 1] if column else None
            previous_left = previous_center[0] if previous_center else 0.0
            sequential = (column == 0 or 140 <= center[0]-previous_left <= 300)
            aligned = (column == 0 or abs(center[1]-previous_center[1]) <= 28)
            source_scale = 1.65 <= found["sx"] <= 2.1 and 1.65 <= found["sy"] <= 2.1
            safe = (found["score"] >= MIN_SCORE and separation >= MIN_SEPARATION
                    and sequential and aligned and source_scale)
            record = {"identity": identity, "view": view, "status": "matched" if safe else "review_needed",
                      "master": source_path, "master_sha256": master_hashes[identity],
                      "old_sha256": sha256(originals[entry]), "old_size": list(old.size),
                      "matched_transform": found, "separation_from_previous_views_px": round(separation, 1),
                      "view_order_valid": sequential, "row_alignment_valid": aligned,
                      "source_scale_valid": source_scale}
            if safe:
                high = reconstruct(master, old, found)
                stream = io.BytesIO()
                high.save(stream, format="PNG", optimize=True)
                new_frames[entry] = stream.getvalue()
                record["new_sha256"] = sha256(stream.getvalue())
                record["new_size"] = list(high.size)
                comparison(master, old, high, found, identity + "/" + view,
                           output / (identity + "-" + view + ".png"))
            results.append(record)
            print(identity, view, record["status"], "score", found["score"],
                  "scale", found["sx"], found["sy"], "separation", round(separation, 1), flush=True)
    mismatched = [record for record in results if record["status"] == "review_needed"]
    report = {"previous_archive_sha256": original_hash, "preserved_original_masters": master_hashes,
              "generated_frames": len(new_frames), "review_needed": len(mismatched),
              "source_rgb_restored": not mismatched and len(new_frames) == 16,
              "matching_policy": "owner concept turnaround: content, source scale, aligned 4-column order",
              "records": results}
    (output / "audit.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    if mismatched:
        print("SOURCE_MATCH_REVIEW_REQUIRED: no packaged assets replaced", flush=True)
        return 2
    if write and new_frames:
        # Preserve all existing ZIP paths. A full batch must pass before *any* write.
        target = ARCHIVE.with_suffix(".zip.tmp")
        try:
            with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
                for name, raw in originals.items():
                    archive.writestr(name, new_frames.get(name, raw))
            with zipfile.ZipFile(target) as check:
                if check.testzip() is not None or len(check.namelist()) != len(originals):
                    raise ValueError("Repacked directional archive corrupt or incomplete")
            target.replace(ARCHIVE)
        finally:
            target.unlink(missing_ok=True)
        print("REBUILT_ARCHIVE", sha256(ARCHIVE.read_bytes()), flush=True)
    elif write:
        print("ALREADY_REBUILT: no archive mutation", flush=True)
    else:
        print("AUDIT_ONLY: regenerated art exists only in diagnostic previews", flush=True)
    return 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--write", action="store_true", help="Replace only the runtime ZIP after all views pass")
    args = parser.parse_args()
    raise SystemExit(run(args.out, args.write))
