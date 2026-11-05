#!/usr/bin/env python3
"""Deterministic source-only 3D map-kit contact rooms.

One kit per each of the 28 prospective maps plus the separate mortuary annex.
Six TRUE-SCALE independent GLB prototypes per contact page, grouped by map.
These are NOT scene placements and deliberately DO NOT enter runtime assets.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import build
import fit

ROOT = Path(__file__).resolve().parent
SHEETS = ROOT / "site-kits"
PER_PAGE = 6
COL_SPACING = 7.0
ROW_SPACING = 7.0


def digest(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def max_model_floor(parts: list[dict]) -> float:
    low = float("inf")
    for part in parts:
        sy = fit.local_extent(part)[1]
        low = min(low, part["p"][1] - sy)
    return low


def positioned_part(part: dict, dx: float, dy: float, dz: float) -> dict:
    p = dict(part)
    x, y, z = p["p"]
    p["p"] = [round(x + dx, 6), round(y + dy, 6), round(z + dz, 6)]
    return p


def make_page(site: dict, objects: list[dict], page_number: int) -> tuple[bytes, dict]:
    pieces = []
    labels = []
    for i, model in enumerate(objects):
        col, row = i % 3, i // 3
        x = (col - 1) * COL_SPACING
        z = (row - .5) * ROW_SPACING
        floor = max_model_floor(model["parts"])
        # The low 10 cm plinth has its top at zero; every original model is
        # moved by -minY to sit on the plane while keeping its source size.
        # Preserve full source scale, enlarging the review plate for long
        # assets without overlapping the fixed 7m contact-room grid.
        bounds, _, _ = fit.footprint(model, 0, 0, 0)
        stand_width = round(max(5.65, 2*max(abs(bounds[0]),abs(bounds[2]))+.32), 3)
        stand_depth = round(max(5.60, 2*max(abs(bounds[1]),abs(bounds[3]))+.32), 3)
        if stand_width > 6.6 or stand_depth > 6.6:
            raise ValueError("Individual large-asset review needed: "+model["id"])
        pieces.append({"type": "box", "p": [x, -.06, z],
                       "size": [stand_width, .10, stand_depth], "material": "stone"})
        pieces.append({"type": "box", "p": [x, -.005, z-stand_depth/2+.10],
                       "size": [stand_width, .015, .18], "material": "accent"})
        for part in model["parts"]:
            pieces.append(positioned_part(part, x, -floor, z))
        labels.append({"slot": i + 1, "id": model["id"], "title": model["label"],
                       "source_ground_offset": round(-floor, 6),
                       "stand_center": [x, 0, z],
                       "stand_dimensions_m": [stand_width, stand_depth],
                       "review_only": True})
    slug = site["id"].lower().replace("-", "_")
    name = "level_blockout_kit_" + slug + "_p" + str(page_number + 1).zfill(2)
    spec = {"id": name, "label": site["title"] + " map-kit contact room",
            "parts": pieces}
    binary, triangles = build.glb(spec)
    return binary, {"page": page_number + 1, "name": name,
                    "file": name + ".glb", "triangles": triangles,
                    "bytes": len(binary), "sha256": digest(binary),
                    "display_slots": labels}


def render() -> tuple[dict[Path, bytes], bytes]:
    source = json.loads((ROOT / "catalog.json").read_text(encoding="utf-8"))
    if source["schema"] != "beckenburg.primitive-props/1":
        raise ValueError("Unknown source catalog")
    models = {m["id"]: m for m in source["models"]}
    shared = set(source["shared_reuse"])
    if len(source["sites"]) != 29:
        raise ValueError("Map-kit roster changed unexpectedly")
    out: dict[Path, bytes] = {}
    index = {"schema": "beckenburg.primitive-map-kit/1",
             "status": "source-only-model-contact-rooms",
             "runtime_approved": False,
             "model_library_count": len(models),
             "planning_packets": len(source["sites"]),
             "model_scale": "true-scale metres, not resized",
             "page_layout": "3 columns x 2 rows; width 7 m / depth 7 m per stand",
             "not_gameplay": ["No player collision, navigation or lighting",
                              "No model replacement, automatic level dressing or editor save",
                              "Readability of fine detail needs individual model review"],
             "sites": []}
    exhibited = 0
    for site in source["sites"]:
        choices = [models[name] for name in site["prototypes"] if name not in shared]
        # The six universally reusable shell models are listed in each kit
        # manifest, but not repeated 29 times as physical gallery stands.
        if not choices:
            raise ValueError("Location has no identity-specific blockouts " + site["id"])
        pages = []
        for start in range(0, len(choices), PER_PAGE):
            blob, info = make_page(site, choices[start:start+PER_PAGE], len(pages))
            out[SHEETS / info["file"]] = blob
            pages.append(info)
            exhibited += len(info["display_slots"])
        index["sites"].append({"site": site["id"], "name": site["title"],
                               "requirements": site["requirements"],
                               "shared_shell_models": sorted(shared),
                               "specific_models": [x["id"] for x in choices],
                               "pages": pages,
                               "still_source_only": True})
    index["total_stands"] = exhibited
    index["total_pages"] = len(out)
    report = (json.dumps(index, indent=2, ensure_ascii=False) + "\n").encode("utf-8")
    return out, report


def main() -> None:
    args = argparse.ArgumentParser()
    args.add_argument("--check", action="store_true")
    opt = args.parse_args()
    images, manifest = render()
    outputs = {SHEETS / "index.json": manifest, **images}
    if not opt.check:
        SHEETS.mkdir(parents=True, exist_ok=True)
        for old in SHEETS.glob("level_blockout_kit_*.glb"):
            if old not in images:
                old.unlink()
    for path, data in outputs.items():
        if opt.check:
            if not path.exists() or path.read_bytes() != data:
                raise SystemExit("Missing or stale map-kit sheet: " + str(path))
        else:
            path.write_bytes(data)
    if opt.check:
        existing = set(SHEETS.glob("level_blockout_kit_*.glb"))
        if existing != set(images):
            raise SystemExit("Orphaned/extra map-kit contact rooms")
    summary = json.loads(manifest)
    print("PASS MAP KITS", summary["planning_packets"], "MAP AND ANNEX PACKETS,",
          summary["total_pages"], "SOURCE-ONLY 3D PAGES,",
          summary["total_stands"], "MODEL STANDS,",
          "MODE", "check" if opt.check else "build")


if __name__ == "__main__":
    main()
