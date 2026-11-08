#!/usr/bin/env python3
"""Twelve source-only physical relics in the expanded Castle's captured-relic vault.

A gallery is an authoring proposal, never an inventory grant or a native
CastleHubMap scene edit. Every artifact has its own GLB, support and placement ID.
The burial crypt remains physically and conceptually separate.
"""
from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import json
import math
from pathlib import Path

import build
import fit

HERE = Path(__file__).resolve().parent
LAYOUT = HERE.parent / "layouts" / "castle" / "layout.json"
CATALOG = HERE / "catalog.json"
OUT = HERE / "relic-review"
CASE = "level_blockout_relic_open_guard"
FLOOR_PAD_HEIGHT_M = 0.645
CASTLE_SITE = "CASTLE"
# These are fictional exhibition candidates, not confirmed items recovered
# by campaign progression. The two side-banks keep stair arrival at X=11 clear.
RELICS = (
    ("mourning-locket", "level_blockout_relic_mourning_locket"),
    ("votive-bell", "level_blockout_relic_votive_bell"),
    ("bronze-astrolabe", "level_blockout_relic_bronze_astrolabe"),
    ("sealed-urn", "level_blockout_relic_sealed_urn"),
    ("black-mirror", "level_blockout_relic_black_mirror"),
    ("bound-folio", "level_blockout_relic_bound_folio"),
    ("reliquary-box", "level_blockout_relic_reliquary_box"),
    ("pilgrim-lantern", "level_blockout_relic_pilgrim_lantern"),
    ("ossuary-keys", "level_blockout_relic_ossuary_keys"),
    ("censer", "level_blockout_relic_censer"),
    ("burial-mask", "level_blockout_relic_burial_mask"),
    ("pilgrim-chalice", "level_blockout_relic_pilgrim_chalice"),
)
AISLE = [9.10, 35.0, 12.90, 49.0]
X = (6.30, 15.60)
Z = (30.75, 35.15, 39.55, 43.95, 48.35, 52.75)


def hash_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def bounded_parts(model: dict, x: float, y: float, z: float) -> list[dict]:
    """Models remain physically distinct; only review-scene copies move."""
    result = []
    for part in model["parts"]:
        p = dict(part)
        px, py, pz = p["p"]
        p["p"] = [round(x + px, 6), round(y + py, 6), round(z + pz, 6)]
        result.append(p)
    return result


def render() -> tuple[dict, dict[str, bytes]]:
    catalog_raw = CATALOG.read_bytes()
    layout_raw = LAYOUT.read_bytes()
    source = json.loads(catalog_raw)
    layout = json.loads(layout_raw)
    models = {m["id"]: m for m in source["models"]}
    castle = next(s for s in source["sites"] if s["id"] == CASTLE_SITE)
    vault = next(z for z in layout["zones"] if z["id"] == "vault")
    if (layout["id"] != "CASTLE" or vault["y"] != -3
        or vault["bounds"] != [4, 28, 18, 56]):
        raise ValueError("Expanded Castle vault geometry changed, refit the relics")
    if not any(z["id"] == "chapel" for z in layout["zones"]):
        raise ValueError("Chapel context missing")
    if CASE not in castle["prototypes"] or len(RELICS) != 12:
        raise ValueError("Relic model inventory and support assignment changed")
    support = models[CASE]
    if support["runtime_approved"] is not False:
        raise ValueError("Unapproved display support became a production asset")
    bounds, support_low, support_high = fit.footprint(support, 0, 0, 0)
    if support_low < -.001 or support_high > 2.85:
        raise ValueError("Inappropriate case headroom")
    support_half_x = max(abs(bounds[0]), abs(bounds[2]))
    support_half_z = max(abs(bounds[1]), abs(bounds[3]))
    placed = []
    cases = []
    banks = {"west": [], "east": []}
    seen = set()
    for i, (short, model_id) in enumerate(RELICS):
        x, z = X[i // 6], Z[i % 6]
        model = models[model_id]
        uid = "castle-relic-study-" + short
        if uid in seen or model["id"] in seen:
            raise ValueError("Relic ID or mesh reused: " + uid)
        seen.update((uid, model["id"]))
        if model["category"] != "relic" or model["runtime_approved"] is not False:
            raise ValueError("This is not an independently modeled source relic")
        if model_id not in castle["prototypes"]:
            raise ValueError("Relic not assigned to Castle study catalog: " + model_id)
        footprint, lowest, highest = fit.footprint(model, 0, 0, 0)
        # Source display pad is 1.24 x 1.08 metres, centered at the stand.
        if (footprint[0] < -.60 or footprint[2] > .60 or
            footprint[1] < -.52 or footprint[3] > .52):
            raise ValueError("Physical relic won't fit its padded display: " + uid)
        if highest - lowest + FLOOR_PAD_HEIGHT_M > 2.43:
            raise ValueError("Relic touches case top/ceiling: " + uid)
        case_rect = [x + bounds[0], z + bounds[1],
                     x + bounds[2], z + bounds[3]]
        if not fit.zone_contains(case_rect, vault["bounds"], 1.0):
            raise ValueError("Support intersects Castle vault wall clearance: " + uid)
        if fit.overlap(fit.inflate(case_rect, .45), AISLE):
            raise ValueError("Support obstructs vault/stair aisle: " + uid)
        for other in cases:
            if fit.overlap(fit.inflate(case_rect, .50), other):
                raise ValueError("Relic cases overlap: " + uid)
        cases.append(case_rect)
        bank = "west" if i < 6 else "east"
        caseY = round(vault["y"] - support_low, 4)
        objectY = round(vault["y"] + FLOOR_PAD_HEIGHT_M - lowest, 4)
        entry = {
            "id": uid,
            "catalog_source_mesh": model_id,
            "case_source_mesh": CASE,
            "zone": "vault",
            "bank": bank,
            "case_position": [x, caseY, z],
            "relic_position": [x, objectY, z],
            "yaw_rad": 0,
            "case_bounds_xz": [round(n, 4) for n in case_rect],
            "relic_world_height_m": round(highest - lowest, 4),
            "source_only": True,
            "captured_in_campaign": False,
            "installed_in_playable_castle": False,
            "gameplay_effect_defined": False,
            "authoring_status": "RELIC_MODEL_GEOMETRY_ONLY",
        }
        placed.append(entry)
        banks[bank].append(entry)

    # Not just a file list: compose both real-geometry types for a physical
    # six-stand bank on a measured vault-floor overview.
    pages = {}
    for bank, group in banks.items():
        parts = [{
            "type": "box",
            "p": [11, vault["y"] - .07, 42],
            "size": [14, .12, 28],
            "material": "stone",
        }]
        for entry in group:
            cx, cy, cz = entry["case_position"]
            ox, oy, oz = entry["relic_position"]
            parts.extend(bounded_parts(support, cx, cy, cz))
            parts.extend(bounded_parts(models[entry["catalog_source_mesh"]], ox, oy, oz))
        model = {"id": "review_relic_vault_" + bank,
                 "label": "Expanded Castle inert relic gallery - " + bank,
                 "parts": parts}
        blob, tri = build.glb(model)
        pages[bank] = {
            "name": bank + "-vault-bank.glb",
            "bytes": len(blob),
            "sha256": hash_bytes(blob),
            "triangles": tri,
            "relic_count": len(group),
        }
        pages[bank]["binary"] = blob
    result = {
        "schema": "beckenburg.source-relic-study/1",
        "status": "SOURCE_ONLY_NOT_CAPTURED_NOT_INSTALLED",
        "site": "CASTLE",
        "source_layout": "development/levels/layouts/castle/layout.json",
        "source_layout_sha256": hash_bytes(layout_raw),
        "source_model_catalog_sha256": hash_bytes(catalog_raw),
        "proposed_vault_relics": len(placed),
        "counts_by_bank": {bank: len(group) for bank, group in banks.items()},
        "actual_captured_in_campaign": 0,
        "actual_installed_in_playable_castle": 0,
        "source_only_display_cases": len(cases),
        "reserve_aisle_xz": AISLE,
        "checks": ["unique physical artifact IDs", "one distinct GLB per relic",
                   "case support and headroom", "room bounds and masonry margin",
                   "stairs/aisle reservation", "case nonoverlap", "unapproved source assets"],
        "limitations": [
            "Expanded Castle's vault is not the native playable Castle cellar",
            "No captured-in-campaign truth or recovery persistence wired",
            "No playable scene save, interactable cases, locks or inventory system",
            "The reserved schematic stairs still need structural architecture",
            "Source material swatches/UV0 are not production textures",
        ],
        "placements": placed,
        "pages": [{k:v for k,v in page.items() if k!="binary"} for page in pages.values()],
        "runtime_scene_changed": False,
        "runtime_approved": False,
    }
    return result, {page["name"]: page["binary"] for page in pages.values()}


def main() -> None:
    cli = argparse.ArgumentParser()
    cli.add_argument("--check", action="store_true")
    args = cli.parse_args()
    manifest, pages = render()
    payloads = {
        OUT / "relic-inventory.json":
            (json.dumps(manifest, indent=2, ensure_ascii=False) + "\n").encode("utf-8"),
        **{OUT / name: data for name, data in pages.items()}
    }
    if not args.check:
        OUT.mkdir(parents=True, exist_ok=True)
    for path, blob in payloads.items():
        if args.check:
            if not path.exists() or path.read_bytes() != blob:
                raise SystemExit("MISSING OR STALE RELIC STUDY: " + str(path))
        else:
            path.write_bytes(blob)
    if args.check:
        from audit import read_glb
        for name in pages:
            doc, raw = read_glb(OUT / name)
            if doc["extras"]["runtimeApproved"] is not False or raw != pages[name]:
                raise ValueError("Relic preview was misclassified as runtime geometry")
    print("PASS INERT RELICS", manifest["proposed_vault_relics"], "independent meshes,",
          manifest["counts_by_bank"], "review cases; playable Castle installed=0;",
          "MODE", "check" if args.check else "generate")


if __name__ == "__main__":
    main()
