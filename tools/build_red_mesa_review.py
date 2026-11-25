"""Fit canonical walk-in cupboards and bind the measured Red Mesa plan to native gameplay."""
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
SOURCE = BASE / "canyon-marsh-pass/wld-03"
OUT = BASE / "red-mesa-native-pass"
OWNERS = {
    "shelter": (0, 12, 0, 52), "wash": (1, 54, 0, 52), "lower-path": (2, 12, 4, 33.5),
    "upper-path": (3, 34, 8, 16), "gallery": (4, 55, 8, 31), "dwellings": (5, 34, 8, 31),
    "fork": (6, 71, 8, 31), "chamber": (7, 74, 8, 13), "lower-cave": (8, 55, 4, 31),
    "return": (9, 57, 4, 14), "alcove": (10, 54, 8, 46), "floor": (11, 34, 0, 53),
    "entry": (12, 34, 0, 73), "stage": (13, 34, 0, 81),
}

# Name, zone, measured envelope, lower datum and usable height. These volumes
# assign regions to already-built stair bays, landings and connecting decks.
TRANSITION_SPACES = (
    ("shelter-stair", "lower-path", (10.4, 32.9, 13.6, 46), 0, 5.8),
    ("upper-stair", "upper-path", (22.1, 18.3, 25.15, 32.8), 4, 6.0),
    ("upper-bridge", "upper-path", (47.8, 11.95, 66, 15.25), 8, 3),
    ("lower-ledge", "lower-path", (12, 32.2, 55.25, 44.15), 4, 2.6),
    ("gallery-stair", "lower-cave", (56.1, 26.5, 57.9, 33.5), 4, 4),
)

OPENINGS = (
    ("stage", "entry", 34, 0, 80, 2, "z", "trail"),
    ("entry", "floor", 34, 0, 66, 2, "z", "trail"),
    ("floor", "shelter", 20, 0, 52, 1.6, "x", "trail"),
    ("floor", "wash", 48, 0, 52, 1.6, "x", "trail"),
    ("lower-path", "upper-path", 23.625, 6, 25.15, 1.6, "z", "stair"),
    ("upper-path", "dwellings", 34, 8, 22, 2, "z", "trail"),
    ("gallery", "fork", 66, 8, 31, 1.6, "x", "trail"),
    ("fork", "chamber", 71, 8, 22, 1.6, "z", "trail"),
    ("gallery", "lower-cave", 57, 6, 30, 1.6, "z", "stair"),
    ("lower-cave", "return", 57, 4, 22, 1.6, "z", "trail"),
    ("gallery", "alcove", 54, 8, 40, 1.6, "z", "trail"),
)

# Take waypoints on the actual receiving face, clear of the coincident riser.
NOSINGS = {
    (12, 0, 44.6): (12, 0, 44.77),
    (12, 2, 39.06): (12, 2, 39.23),
    (23.625, 4, 29.77): (23.625, 4, 29.94),
    (23.625, 6, 24.23): (23.625, 6, 24.40),
    (57, 4, 26.5): (57, 4, 26.32),
}
FRAME_SPACES = {
    "shelter stair door": ("shelter", "shelter-stair"),
    "lower ledge cave door": ("lower-ledge", "lower-cave"),
    "chamber bridge door": ("upper-bridge", "chamber"),
}

REFUGES = (("shelter", 5.5, 55, -math.pi / 2),
           ("wash", 58.5, 55, math.pi / 2),
           ("chamber", 75, 5.5, 0))


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
    bpy.ops.wm.open_mainfile(filepath=str(source["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    assembly = Assembly()
    refuges = cupboards(assembly, zones, REFUGES, site="red-mesa", floor_roles=("floor",))
    for piece in assembly.built:
        piece.name = piece.name.removeprefix("College ")
    target = {ext: OUT / f"red-mesa-review.{ext}" for ext in source}
    addition = ROOT / "build/red-mesa-refuges.glb"
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
    for name, key, box, floor, height in TRANSITION_SPACES:
        rows.append(("SPACE", name, OWNERS[key][0], *box, floor, height, "true"))
    rows.extend(("TREAD", name) for name in contract["stair_components"])
    for i, (a, b, x, y, z, width, axis, kind) in enumerate(OPENINGS, 950):
        rows.append(("OPEN", i, OWNERS[a][0], OWNERS[b][0], x, y, z, width, axis, kind))
    for rite in layout["rituals"]:
        rows.append(("RITUAL", rite["rite"].lower(), OWNERS[rite["zone"]][0], *rite["point"]))
    for pocket in refuges:
        rows.append(("COVER", pocket["id"], pocket["owner"], *pocket["position"], pocket["yaw"]))
    for path in layout["links"]:
        rows.extend(("POINT", path["id"], *NOSINGS.get(tuple(point), point)) for point in path["points"])
    table(OUT / "runtime-input.tsv", rows)

    leaf = sha(ROOT / "assets/runtime/models/door_1_loose.glb")
    doors = []
    for i, frame in enumerate(layout["authored_doorways"], 970):
        a, b = frame["rooms"]
        axis, boundary, along = (frame[k] for k in ("axis", "boundary", "along"))
        x, z = (along, boundary) if axis == "z" else (boundary, along)
        # Swing off the stair, ledge and bridge into their receiving rooms.
        direction = (1, -1, 1, 1)[i - 970]
        room_a, room_b = FRAME_SPACES.get(frame["id"], (a, b))
        doors.append((i, frame["id"], room_a, room_b, OWNERS[a][0], OWNERS[b][0], x, frame["sill_y"], z,
                      frame["clear_width"], axis, boundary-.12, boundary+.12, frame["clear_height"],
                      2, 1, direction, leaf))
    table(OUT / "door-schedule.tsv", doors)
    record = {
        "site": "WLD-03", "source": str(layout_path.relative_to(ROOT).as_posix()),
        "geometry_sha256": {ext: sha(path) for ext, path in target.items()},
        "retained_native_components": len(retained), "refuges": refuges,
    }
    (OUT / "construction.json").write_bytes((json.dumps(record, indent=2) + "\n").encode())
    print("PASS retained", len(retained), "native parts; refuge floor contacts",
          [refuge["base_contacts"] for refuge in refuges])


if __name__ == "__main__":
    build()
