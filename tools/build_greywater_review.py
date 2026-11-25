"""Fit supported walk-in refuges and bind the surveyed Greywater Cannery to gameplay."""
import hashlib
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Matrix

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import Assembly, bounds, point, world
from build_college_grounds import import_scene
from build_lookout_native_arrival import table
from build_prison_sally_gate import export
from build_school_electrical import cupboards
from compose_room_terrain import split_native
from review_roster_macroterrain import records
from verify_college_services import bvh, match_geometry

BASE = ROOT / "development/levels/rebuild"
SOURCE = BASE / "workyard-station-pass/wld-13"
OUT = BASE / "greywater-native-pass"
OWNERS = {
    "wash": (0, 16, 0, 50), "cold": (1, 56, 0, 35), "boiler": (2, 16, 0, 35),
    "office": (3, 30, 0, 20), "processing": (4, 36, 0, 41), "stores": (5, 42, 0, 20),
    "dock": (6, 36, 0, 7), "bunk": (7, 0, 0, 53), "mess": (8, 0, 0, 35),
    "forest": (9, -11.8, 0, 44), "side-yard": (10, 56, 0, 53),
    "gallery": (11, 27, 3, 41), "entry": (12, 34, 0, 69), "stage": (13, 32, 0, 77),
}

# The wide south loading yard and established grade connect the three
# actual buildings. These volumes assign custody, not walking support.
YARDS = (
    ("loading-yard", (8, 56, 48, 63)),
    ("east-return-bend", (44, 55, 61, 71)),
)
OPENINGS = (
    ("stage", "entry", 32, 0, 76, 2, "z", "trail"),
    ("entry", "side-yard", 48, 0, 59, 2, "x", "trail"),
    ("processing", "gallery", 27, 1.5, 47, 1.4, "z", "stair"),
)
EXTERIOR = {2: "loading-yard", 5: "side-yard", 8: "loading-yard", 10: "dock", 12: "dock"}
RETIRED = {"Header processing E", "Header cold W"}
SWINGS = (-1, -1, -1, -1, -1, -1, 1, -1, -1, -1, 1, 1, 1, -1, 1, 1)
REFUGES = (("wash", 10, 53.5, -math.pi / 2), ("cold", 62.4, 39, math.pi / 2),
           ("bunk", -5.5, 58, -math.pi / 2))


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def power(assembly, boiler):
    down = point(0, -1, 0)
    for asset, label, x, z, yaw in (
        ("level_trace_generator_refined", "generator", 12.4, 28.1, math.pi),
        ("level_electrical_bank_worn", "breaker", 19.8, 26.40, 0),
    ):
        at = point(x, .06, z)
        pieces = assembly.place(asset, Matrix.Translation(at) @ Matrix.Rotation(yaw, 4, "Z"),
                                "Greywater " + label, "founded boiler equipment", "boiler")
        lo, hi = bounds(pieces)
        assert abs(lo[1] - .06) < .004, (label, lo)
        for px in (lo[0], hi[0]):
            for pz in (lo[2], hi[2]):
                base, _, _ = assembly.measured(point(px, .16, pz), down, .24, label + " slab")
                assert abs(base.z) < .006, (label, px, pz)
        assembly.box("Greywater " + label + " plinth", (lo[0]-.05, lo[2]-.04, hi[0]+.05, hi[2]+.04),
                     0, .06, assembly.concrete, "equipment foundation on built slab", "boiler")
        mount_height = .56 if label == "generator" else .89
        hit, _, _, _ = bvh(pieces).ray_cast(point(x, mount_height, hi[2]+1), point(0, 0, -1), 2)
        assert hit is not None, (label, lo, hi)
        contact = world(hit)
        assembly.effect("greywater-" + label, label, boiler["owner"],
                        (contact[0], contact[1], contact[2]+.015), 0, "GW-MAIN")
    assembly.switch(boiler, 22.1, 27.1, 0, -1)
    assembly.mounted_lamp(boiler, "greywater-boiler-light", point(18, 2.35, 27.15),
                          point(0, 0, -1), 1.25, point(1, 0, 0))
    # The feeder and lamp branch run along the north boiler wall above head height.
    for name, run in (
        ("generator feed", [(12.4, .48, 27.60), (12.4, .48, 26.15), (12.4, 2.58, 26.15),
                            (19.8, 2.58, 26.15), (19.8, 1.85, 26.30)]),
        ("boiler lighting branch", [(19.8, 2.58, 26.15), (22.1, 2.58, 26.15),
                                    (22.1, 2.35, 26.15), (18, 2.35, 26.15)]),
    ):
        assembly.raceway("Greywater " + name, run, .017)


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    source = {ext: SOURCE / f"workyard-station.{ext}" for ext in ("blend", "glb", "hcs2")}
    contract = json.loads((SOURCE / "construction.json").read_bytes())
    for ext, digest in (("blend", "blend_sha256"), ("glb", "glb_sha256"), ("hcs2", "physics_sha256")):
        assert sha(source[ext]) == contract[digest], ext
    layout_path = SOURCE / "authored-layout.json"
    assert sha(layout_path) == contract["layout_sha256"]
    layout = json.loads(layout_path.read_bytes())
    zones = {z["id"]: {**z, "owner": OWNERS[z["id"]][0]} for z in layout["zones"]}
    bpy.ops.wm.open_mainfile(filepath=str(source["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    for name in RETIRED:
        bpy.data.objects.remove(bpy.data.objects[name], do_unlink=True)
    assembly = Assembly()
    refuges = cupboards(assembly, zones, REFUGES, site="greywater", floor_roles=("floor",))
    power(assembly, zones["boiler"])
    for piece in assembly.built:
        piece.name = piece.name.removeprefix("College ")
    target = {ext: OUT / f"greywater-review.{ext}" for ext in source}
    addition = ROOT / "build/greywater-refuges.glb"
    export(addition, assembly.built)
    append(source["glb"], addition, target["glb"], remove_mesh_names=sorted(RETIRED), reuse_embedded_images=True)
    parts = native_write(source["hcs2"], target["hcs2"], RETIRED, assembly.built,
                         open_components={obj.name for obj in assembly.built})
    prior = split_native(source["hcs2"])
    retained = [row for row in prior if row[0].split(":", 1)[-1] not in RETIRED]
    assert len(prior) - len(retained) == len(RETIRED)
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
    for name, bounds in YARDS:
        rows.append(("SPACE", name, OWNERS["entry"][0], *bounds, 0, 3, "true"))
    rows.extend(("TREAD", name) for name in contract["stair_components"])
    for i, (a, b, x, y, z, width, axis, kind) in enumerate(OPENINGS, 950):
        rows.append(("OPEN", i, OWNERS[a][0], OWNERS[b][0], x, y, z, width, axis, kind))
    for rite in layout["rituals"]:
        rows.append(("RITUAL", rite["rite"].lower(), OWNERS[rite["zone"]][0], *rite["point"]))
    for service in assembly.sockets:
        if service["type"] != "hide":
            rows.append(("SERVICE", service["id"], service["type"], service["room"],
                         *service["position"], service["yaw"], service["style"]))
    for pocket in refuges:
        rows.append(("COVER", pocket["id"], pocket["owner"], *pocket["position"], pocket["yaw"]))
    for path in layout["links"]:
        # Sample the lower receiving floor clear of the first nosing.
        rows.extend(("POINT", path["id"], *( (27, 0, 51.17) if point == [27, 0, 51] else point))
                    for point in path["points"])
    table(OUT / "runtime-input.tsv", rows)

    leaf = sha(ROOT / "assets/runtime/models/door_1_loose.glb")
    doors = []
    for index, frame in enumerate(layout["authored_doorways"]):
        rooms = frame["rooms"]
        a, b = rooms if len(rooms) == 2 else (rooms[0], EXTERIOR[index])
        x, y, z = frame["frame_center"]
        owner_b = "entry" if b == "loading-yard" else b
        width = frame["clear_width"]
        doors.append((1000 + index, frame["id"], a, b, OWNERS[a][0], OWNERS[owner_b][0], x, y, z,
                      width, frame["axis"], *frame["frame_span"], frame["clear_height"],
                      2 if width >= 1.7 else 1, 1, SWINGS[index], leaf))
    table(OUT / "door-schedule.tsv", doors)
    record = {
        "site": "WLD-13", "source": str(layout_path.relative_to(ROOT).as_posix()),
        "geometry_sha256": {ext: sha(path) for ext, path in target.items()},
        "retained_native_components": len(retained), "refuges": refuges, "removed_headers": sorted(RETIRED),
        "service_sockets": [s for s in assembly.sockets if s["type"] != "hide"],
    }
    (OUT / "construction.json").write_bytes((json.dumps(record, indent=2) + "\n").encode())
    print("PASS retained", len(retained), "native parts; refuge floor contacts",
          [refuge["base_contacts"] for refuge in refuges])


if __name__ == "__main__":
    build()
