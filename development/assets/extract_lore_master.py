"""Reproducible segmentation of the approved Matthis Rook four-view concept art.

Requires Pillow, numpy and opencv-python only for offline source extraction.
The shipped game needs ONLY the generated Zip of transparent 128x224 PNGs.
This never invents new poses or replaces approved artist source art.
"""
from __future__ import annotations
import io
import sys
import zipfile
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

CROPS = {
    'front': (32, 210, 351, 955),
    'side': (395, 210, 692, 955),
    'back': (725, 210, 1062, 955),
    'right': (1079, 210, 1407, 955),
}


def extract(source: Path, target: Path) -> None:
    art = np.array(Image.open(source).convert('RGB'))
    if art.shape[1:] != (1448, 3) or art.shape[0] != 1086:
        raise ValueError('This extractor is for the approved 1448 x 1086 four-view board only')
    target.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(target, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name, (left, top, right, bottom) in CROPS.items():
            crop = art[top:bottom, left:right]
            brightness = crop.max(axis=2)
            mask = np.full(brightness.shape, cv2.GC_PR_BGD, np.uint8)
            bright = (brightness > 48).astype(np.uint8)
            vicinity = cv2.dilate(bright, np.ones((31, 31), np.uint8))
            mask[vicinity > 0] = cv2.GC_PR_FGD
            mask[bright > 0] = cv2.GC_FGD
            mask[(brightness < 17) & (vicinity == 0)] = cv2.GC_BGD
            mask[:2] = cv2.GC_BGD
            mask[-2:] = cv2.GC_BGD
            mask[:, :2] = cv2.GC_BGD
            mask[:, -2:] = cv2.GC_BGD
            cv2.grabCut(cv2.cvtColor(crop, cv2.COLOR_RGB2BGR), mask, None,
                        np.zeros((1, 65), np.float64),
                        np.zeros((1, 65), np.float64), 5, cv2.GC_INIT_WITH_MASK)
            alpha = np.where((mask == cv2.GC_FGD) | (mask == cv2.GC_PR_FGD), 255, 0).astype(np.uint8)
            # Remove narrow dark-gold presentation-panel borders that are NOT
            # part of the NPC silhouette, without trimming coat or accessories.
            if name == 'back':
                alpha[:, :6] = 0
            elif name == 'right':
                alpha[:, :34] = 0
            rgba = Image.fromarray(np.dstack([crop, alpha]), 'RGBA')
            # Retain original proportions in a 128 x 224 game cell, rather than
            # stretching a slender coat silhouette sideways to fill the slot.
            scale = min(120 / rgba.width, 216 / rgba.height)
            w = round(rgba.width * scale)
            h = round(rgba.height * scale)
            small = rgba.resize((w, h), Image.Resampling.LANCZOS)
            result = Image.new('RGBA', (128, 224))
            result.alpha_composite(small, ((128 - w) // 2, 224 - h - 4))
            if result.getbbox() is None:
                raise ValueError('Empty sprite: ' + name)
            buf = io.BytesIO()
            result.save(buf, format='PNG', optimize=True)
            archive.writestr('assets/lore_master/' + name + '.png', buf.getvalue())
    print('Authored four-view Lore Master sprite resource:', target, target.stat().st_size)


if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise SystemExit('Usage: python extract_lore_master.py approved-four-view.png lore-master-directions.zip')
    extract(Path(sys.argv[1]), Path(sys.argv[2]))
