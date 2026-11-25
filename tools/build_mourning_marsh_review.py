"""Fit walk-in refuges and bind the surveyed Mourning Marsh settlement to native gameplay."""
import hashlib
import json
import math
from pathlib import Path
import sys

import bpy

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import Assembly
from build_college_grounds import import_scene
from build_lookout_native_arrival import table
from build_prison_sally_gate import export
from build_school_electrical import cupboards
from compose_room_terrain import split_native
from review_roster_macroterrain import records
from verify_college_services import match_geometry

BASE = ROOT / "development/levels/rebuild"
SOURCE = BASE / "canyon-marsh-pass/wld-11"
OUT = BASE / "mourning-marsh-native-pass"
OWNERS = {
    "house-a": (0, 20, 1.2, 46), "house-b": (1, 59, 1.2, 46), "chapel": (2, 20, 1.2, 25),
    "main": (3, 30, 1.2, 38), "warehouse": (4, 40, 1.2, 25), "service": (5, 50, 1.2, 38),
    "shack": (6, 58, 1.2, 25), "ferry": (7, 34, 1.2, 1), "pump": (8, 58, 1.2, 59),
    "south-deck": (9, 40, 1.2, 59), "north-deck": (10, 40, 1.2, 11),
    "levee-landing": (11, 39.98, 0, 67.1), "entry": (12, 32, 0, 72), "stage": (13, 32, 0, 77),
}

# Dedicated owner for the actual low landing, with the adjoining stair bay.
LANDING = {"bounds": (37.70, 62, 42.30, 70), "y": 0, "height": 3, "kind": "outside",
           "name": "Founded levee stair landing"}
OPENINGS = (
    ("stage", "entry", 32, 0, 76, 2, "z", "trail"),
    ("entry", "levee-landing", 39.98, 0, 70, 2, "z", "trail"),
    ("levee-landing", "south-deck", 39.98, .9, 62.7, 2.1, "z", "stair"),
    ("south-deck", "main", 30, 1.2, 56, 2, "z", "trail"),
    ("south-deck", "service", 50, 1.2, 56, 2, "z", "trail"),
    ("main", "north-deck", 30, 1.2, 14, 2, "z", "trail"),
    ("service", "north-deck", 50, 1.2, 14, 2, "z", "trail"),
    ("north-deck", "ferry", 34, 1.2, 8, 2, "z", "trail"),
)

REFUGES = (("house-a", 13.5, 49, -math.pi / 2),
           ("house-b", 64.5, 49, math.pi / 2),
           ("chapel", 15.5, 17.5, 0))

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    source = {ext: SOURCE / f"canyon-marsh.{ext}" for ext in ("blend", "glb", "hcs2")}
    contract = json.loads((SOURCE / "construction.json").read_bytes())
    for ext, digest in (("blend", "blend_sha256"), ("glb", "glb_sha256"), ("hcs2", "physics_sha256")):
        assert sha(source[ext]) == contract[digest], ext
    layout_path = SOURCE / "authored-layout.json"
    assert sha(layout_path) == contract["layout_sha256"]
    layout = json.loads(layout_path.read_bytes())
    zones = {z["id"]: {**z, "owner": OWNERS[z["id"]][0]} for z in layout["zones"]}
    zones["levee-landing"] = LANDING
    bpy.ops.wm.open_mainfile(filepath=str(source["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    assembly = Assembly()
    refuges = cupboards(assembly, zones, REFUGES, site="marsh", floor_roles=("floor",))
    for piece in assembly.built:
        piece.name = piece.name.removeprefix("College ")
    target = {ext: OUT / f"marsh-review.{ext}" for ext in source}
    addition = ROOT / "build/marsh-refuges.glb"
    export(addition, assembly.built)
    append(source["glb"], addition, target["glb"], reuse_embedded_images=True)
    parts = native_write(source["hcs2"], target["hcs2"], set(), assembly.built,
                         open_components={obj.name for obj in assembly.built})
    retained = split_native(source["hcs2"])
    assert split_native(target["hcs2"])[:len(retained)] == retained
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(target["blend"]), compress=True)
    actual = import_scene(target["glb"])
    for name, faces in parts:
        assert match_geometry([face for face, _ in faces], [face for face, _ in records(actual[name])]), name

    dock = layout["staging"]
    rows = [("SOURCE", sha(target["glb"]), sha(target["hcs2"])), ("DOCK", *dock["point"], dock["yaw"])]
    for key, (owner, *spawn) in OWNERS.items():
        z = zones[key]
        rows.append(("ROOM", key, owner, *z["bounds"], z["y"], z["height"],
                     str(z["kind"] in ("outside", "staging")).lower(), z["name"], *spawn))
    # The causeway widens where it turns onto the supported low landing.
    rows.append(("SPACE", "causeway-turn", OWNERS["entry"][0], 37.7, 70, 42.3, 74, 0, 3, "true"))
    rows.extend(("TREAD", name) for name in contract["stair_components"])
    for i, (a, b, x, y, z, width, axis, kind) in enumerate(OPENINGS, 950):
        rows.append(("OPEN", i, OWNERS[a][0], OWNERS[b][0], x, y, z, width, axis, kind))
    for rite in layout["rituals"]:
        rows.append(("RITUAL", rite["rite"].lower(), OWNERS[rite["zone"]][0], *rite["point"]))
    for pocket in refuges:
        rows.append(("COVER", pocket["id"], pocket["owner"], *pocket["position"], pocket["yaw"]))
    for path in layout["links"]:
        rows.extend(("POINT", path["id"], *point) for point in path["points"])
    table(OUT / "runtime-input.tsv", rows)

    leaf = sha(ROOT / "assets/runtime/models/door_1_loose.glb")
    doors = []
    for i, frame in enumerate(layout["authored_doorways"], 980):
        a, b = frame["rooms"]
        axis, boundary, along = (frame[k] for k in ("axis", "boundary", "along"))
        x, z = (along, boundary) if axis == "z" else (boundary, along)
        # Leaves swing into the buildings, clear of shared boardwalk traffic.
        direction = (-1, 1, -1, 1, -1, 1, 1)[i - 980]
        if frame["id"] == "pump room door":
            a = "south-deck"
        doors.append((i, frame["id"], a, b, OWNERS[a][0], OWNERS[b][0], x, frame["sill_y"], z,
                      frame["clear_width"], axis, boundary-.12, boundary+.12, frame["clear_height"],
                      2, 1, direction, leaf))
    table(OUT / "door-schedule.tsv", doors)
    record = {
        "site": "WLD-11", "source": str(layout_path.relative_to(ROOT).as_posix()),
        "geometry_sha256": {ext: sha(path) for ext, path in target.items()},
        "retained_native_components": len(retained), "refuges": refuges,
    }
    (OUT / "construction.json").write_bytes((json.dumps(record, indent=2) + "\n").encode())
    print("PASS retained", len(retained), "native parts; refuge floor contacts",
          [refuge["base_contacts"] for refuge in refuges])


if __name__ == "__main__":
    build()
