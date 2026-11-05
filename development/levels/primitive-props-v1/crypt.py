#!/usr/bin/env python3
"""Count and spatially audit sarcophagi for the PROPOSED expanded-Castle chapel crypt.

The shipped Castle cellar (cellar-hall, relic vault, stair, plant) IS NOT a
burial crypt. This tool cannot and does not install geometry in that game.
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
ROOT = HERE.parent
PROPOSAL = HERE / "castle-crypt-proposal.json"
OUTPUT = HERE / "crypt-review"


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def check_layout(proposal: dict, layout: dict) -> None:
    if layout["id"] != "CASTLE" or layout["status"] == "runtime_approved":
        raise ValueError("Wrong expanded Castle layout")
    zones = {z["id"]: z for z in layout["zones"]}
    chapel = zones["chapel"]
    vault = zones["vault"]
    if chapel["y"] != 0 or vault["y"] != proposal["proposed_floor_y"]:
        raise ValueError("Chapel/vault elevations have changed: reauthor the proposal")
    if "crypt" in zones or any("sarcoph" in name.lower() for name in zones):
        raise ValueError("Source plan contains crypt already; reconcile actual geometry first")
    if proposal["native_playable_castle"]["actual_installed_sarcophagi"] != 0:
        raise ValueError("Do not claim live-installed sarcophagi from a proposal")
    if proposal["native_playable_castle"]["actual_authored_crypt_rooms"] != 0:
        raise ValueError("Native cellar crypt implementation must be checked separately")
    if proposal["doorway_or_stair_implemented"] is not False:
        raise ValueError("No crypt stairs/doors have been built in this package")
    floor = proposal["proposed_floor_y"]
    assert isinstance(floor, (int, float)) and math.isfinite(floor)
    assert proposal["proposed_ceil_y"] > floor
    corridor = proposal["corridor"]
    ax, az, bx, bz = chapel["bounds"]
    ca, cz, cb, cd = corridor["bounds"]
    if corridor["min_clear_width_m"] < 1.5 or cb-ca < corridor["min_clear_width_m"]:
        raise ValueError("Crypt central aisle is too narrow")
    if not (ax < ca < cb < bx and az < cz < cd < bz):
        raise ValueError("Central aisle not under the expanded chapel")
    if corridor["y"] != floor:
        raise ValueError("Disconnected crypt floor")
    for chamber in proposal["proposal_rooms"]:
        x0, z0, x1, z1 = chamber["bounds"]
        if (chamber["y"] != floor or chamber["height"] < 2.6
            or not (ax < x0 < x1 < bx and az < z0 < z1 < bz)
            or fit.overlap(chamber["bounds"], corridor["bounds"])
            or fit.overlap(chamber["bounds"], vault["bounds"])):
            raise ValueError("Crypt chamber conflicts with chapel, vault or route: "+chamber["id"])
    if len(proposal["proposal_rooms"]) != 2:
        raise ValueError("This count authority assumes exactly two chambers")


def render() -> tuple[dict, bytes]:
    source = PROPOSAL.read_bytes()
    proposal = json.loads(source)
    if proposal["schema"] != "beckenburg.castle-sarcophagus-count-proposal/1":
        raise ValueError("Unknown sarcophagus count proposal schema")
    layout_path = ROOT / "layouts" / "castle" / "layout.json"
    layout_raw = layout_path.read_bytes()
    layout = json.loads(layout_raw)
    check_layout(proposal, layout)
    catalog_raw = (HERE / "catalog.json").read_bytes()
    catalog = json.loads(catalog_raw)
    models = {model["id"]: model for model in catalog["models"]}
    castle = next(site for site in catalog["sites"] if site["id"] == "CASTLE")
    chambers = {room["id"]: room for room in proposal["proposal_rooms"]}
    margin = proposal["clearance"]["wall_margin_m"]
    separation = proposal["clearance"]["object_separation_m"]
    corridor_margin = proposal["clearance"]["corridor_margin_m"]
    headroom = proposal["clearance"]["minimum_headroom_above_tomb_m"]
    corridor = proposal["corridor"]["bounds"]
    installed = []
    ids = set()
    by_chamber = Counter()
    by_model = Counter()
    for entry in proposal["placements"]:
        rid = entry["id"]
        if rid in ids:
            raise ValueError("Duplicate physical crypt sarcophagus ID "+rid)
        ids.add(rid)
        room = chambers[entry["room"]]
        model = models[entry["model"]]
        if (not model["id"].startswith("level_blockout_sarcophagus_")
            or model["id"] not in castle["prototypes"]
            or model["runtime_approved"] is not False):
            raise ValueError("Not an unapproved, individual Castle sarcophagus model: "+model["id"])
        if any(not isinstance(entry[k], (int, float)) or not math.isfinite(entry[k])
               for k in ("x", "z", "yaw")):
            raise ValueError("Nonfinite sarcophagus transform")
        x, z, yaw = entry["x"], entry["z"], entry["yaw"]
        bounds, bottom, top = fit.footprint(model, yaw, x, z)
        if not fit.zone_contains(bounds, room["bounds"], margin):
            raise ValueError("Sarcophagus intersects chamber's masonry clearance: "+rid)
        if room["height"] - (top - bottom) < headroom:
            raise ValueError("Sarcophagus does not fit under crypt roof: "+rid)
        if fit.overlap(fit.inflate(bounds, corridor_margin), corridor):
            raise ValueError("Sarcophagus blocks the reserved crypt aisle: "+rid)
        for placed in installed:
            if fit.overlap(fit.inflate(bounds, separation), placed["world_bounds_xz"]):
                raise ValueError("Two sarcophagi intersect or eliminate service passage: "
                                 +rid+" and "+placed["id"])
        transform = [round(x, 4), round(room["y"]-bottom, 4), round(z, 4)]
        installed.append({"id":rid, "zone":entry["room"],
                          "model":model["id"],"position":transform,
                          "yaw_rad":yaw,
                          "floor_elevation":room["y"],
                          "world_bounds_xz":[round(n, 4) for n in bounds],
                          "runtime_approved":False,
                          "collision_verified":False,
                          "placement_status":"PROPOSED_ONLY"})
        by_chamber[entry["room"]] += 1
        by_model[model["id"]] += 1
    planned = proposal["proposed_total_sarcophagi"]
    if len(installed) != planned:
        raise ValueError("Total count drift: "+str(len(installed))+" vs "+str(planned))
    if dict(by_model) != proposal["model_count_targets"]:
        raise ValueError("Model-family sarcophagus counts differ from proposal")
    for room in proposal["proposal_rooms"]:
        if by_chamber[room["id"]] != room["proposed_sarcophagus_count"]:
            raise ValueError("Chamber count drift: "+room["id"])
    zone_map = {z["id"]:{"id":z["id"],"y":z["y"],"bounds":z["bounds"]}
                for z in proposal["proposal_rooms"]}
    preview, triangles = fit.review_glb(installed, models, zone_map)
    doc = {
        "schema":"beckenburg.castle-sarcophagus-placement-audit/1",
        "status":"SOURCE_ONLY_CRYPT_PROPOSAL_NO_BUILT_GAME_CRYPT",
        "site":"CASTLE",
        "expanded_castle_chapel_plan_sha256":sha(layout_raw),
        "source_proposal_sha256":sha(source),
        "source_model_catalog_sha256":sha(catalog_raw),
        "model_count":len(models),
        "total_sarcophagi_proposed":len(installed),
        "total_sarcophagi_installed_in_playable_castle":0,
        "count_by_crypt_chamber":dict(sorted(by_chamber.items())),
        "count_by_model":dict(sorted(by_model.items())),
        "preview_triangles":triangles,
        "preview_sha256":sha(preview),
        "floor_elevation_m":proposal["proposed_floor_y"],
        "central_aisle_width_m":proposal["corridor"]["min_clear_width_m"],
        "checks":["unique object IDs","per-chamber count","per-model count",
                  "room dimensions under existing proposed chapel",
                  "vertical clearance","wall margins",
                  "protected aisle","nonoverlap and lateral access"],
        "not_verified":["actual chapel crypt architecture or staircase",
                        "runtime Castle cellar fit","wall/floor support",
                        "doors and navigation","collision","textures/lighting",
                        "sarcophagus gameplay interaction"],
        "placements":installed,
        "runtime_approved":False,
        "scene_mutated":False
    }
    return doc, preview


def main() -> None:
    parser=argparse.ArgumentParser()
    parser.add_argument("--check",action="store_true")
    args=parser.parse_args()
    report, preview=render()
    outputs={
        OUTPUT/"castle-crypt-count.json":
            (json.dumps(report,indent=2,ensure_ascii=False)+"\n").encode("utf-8"),
        OUTPUT/"castle-crypt-preview.glb":preview
    }
    for target, payload in outputs.items():
        if args.check:
            if not target.exists() or target.read_bytes()!=payload:
                raise SystemExit("MISSING OR STALE crypt count/render: "+str(target))
        else:
            target.parent.mkdir(parents=True,exist_ok=True)
            target.write_bytes(payload)
    if args.check:
        from audit import read_glb
        glb, data = read_glb(OUTPUT/"castle-crypt-preview.glb")
        if glb["extras"]["runtimeApproved"] is not False or data!=preview:
            raise ValueError("Crypt preview must remain nonshipping")
    print("PASS EXPANDED CASTLE SARCOFHAGUS COUNT:",
          report["total_sarcophagi_proposed"], "PROPOSED in",
          len(report["count_by_crypt_chamber"]),"unbuilt crypt rooms;",
          "0 actually installed in playable Castle;",
          "MODE","check" if args.check else "generate")


if __name__ == "__main__":
    main()
