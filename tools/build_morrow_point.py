"""Fit Morrow Point's safety rails, oil-house services and native investigation."""
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
from build_lookout_native_arrival import table
from build_prison_sally_gate import export
from build_school_electrical import cupboards
from review_roster_macroterrain import sha
from verify_college_electrical import native_records
from verify_college_services import bvh

BASE = ROOT / "development/levels/rebuild"
SOURCE = BASE / "coast-entrances-pass/wld-14"
OUT = BASE / "morrow-point"
ANCHORS = {
    "foyer": (0, 28, 0, 48), "sitting": (1, 20, 0, 48), "kitchen": (2, 37, 0, 48),
    "workroom": (3, 22, 0, 36), "store": (4, 34.5, 0, 36), "stairs": (5, 45, 0, 49),
    "bed": (6, 22, 3.2, 48), "study": (7, 35, 3.2, 48), "tower0": (8, 34.8, 0, 10),
    "tower3": (9, 34.8, 10.8, 10), "tower5": (10, 34.8, 18, 10.8),
    "generator": (11, 53, 0, 40), "porch": (12, 32, 0, 60), "stage": (13, 32, 0, 80),
}
CHILDREN = {
    "entry": 13, "upperhall": 5, "service-stair": 3, "service-upper": 5, "yard": 12,
    "passage": 4, "tower1": 8, "tower2": 9, "tower4": 9,
}
COURTS = (
    ("kitchen court", 12, (38.1, 54, 54.8, 57.4), 0, 3),
    ("main stair flight", 5, (44.05, 39.9, 45.95, 48.1), 0, 3.24),
    ("service stair flight", 3, (13.05, 35.9, 14.95, 44.1), 0, 3.24),
)
PASSAGES = (
    (0, 1, 24, 0, 48, 1.7, "x", "trail"),
    (1, 3, 21, 0, 42, 1.7, "z", "trail"),
    (3, 4, 28, 0, 36, 1.7, "x", "trail"),
    (4, 2, 36, 0, 42, 1.7, "z", "trail"),
    (2, 0, 32, 0, 48, 1.7, "x", "trail"),
    (4, 5, 42, 0, 40, 1.7, "x", "trail"),
    (7, 6, 28, 3.2, 48, 1.7, "x", "trail"),
    (12, 13, 32, 0, 66, 2.3, "z", "trail"),
    (8, 9, 34.8, 7.2, 10, 1.5, "z", "stair"),
    (9, 10, 34.8, 18, 10, 1.5, "z", "stair"),
)
SWINGS = (1, -1, 1, 1, -1, 1)
POCKETS = (("sitting", 18.25, 51.4, -math.pi/2), ("study", 30.5, 50.4, -math.pi/2),
           ("generator", 56.3, 42.4, math.pi/2))


def power(assembly, zone):
    for asset, kind, x, z, yaw, control_y in (
        ("level_trace_generator_refined", "generator", 55.3, 35.2, math.pi, .56),
        ("level_electrical_bank_worn", "breaker", 51.2, 44.35, 0, .89),
    ):
        parts = assembly.place(asset, Matrix.Translation(point(x, .06, z)) @ Matrix.Rotation(yaw, 4, "Z"),
                               "Morrow " + kind, "oil-house electrical equipment", "generator")
        low, high = bounds(parts)
        assert abs(low[1] - .06) < .004
        for px in (low[0], high[0]):
            for pz in (low[2], high[2]):
                hit, _, _ = assembly.measured(point(px, .18, pz), point(0, -1, 0), .30, kind + " slab")
                assert abs(hit.z) < .006
        assembly.box("Morrow " + kind + " plinth", (low[0]-.04, low[2]-.04, high[0]+.04, high[2]+.04),
                     0, .06, assembly.concrete, "equipment foundation", "generator")
        hit, _, _, _ = bvh(parts).ray_cast(point(x, control_y, high[2]+1), point(0, 0, -1), 2)
        assert hit is not None, kind
        px, py, pz = world(hit)
        assembly.effect("morrow-" + kind, kind, zone["owner"], (px, py, pz+.016), 0, "MP-MAIN")
    assembly.switch(zone, 53, 35.45, 0, -1)
    assembly.sockets[-1].update(id="morrow-oil-house-switch", style="MP-OIL")
    assembly.mounted_lamp(zone, "morrow-oil-house-light", point(53.9, 2.42, 35.4),
                          point(0, 0, -1), 1.5, point(1, 0, 0))
    assembly.raceway("Morrow oil-house branch", ((55.3, .62, 35.0), (55.3, .62, 34.31),
        (55.3, 2.66, 34.31), (48.25, 2.66, 34.31), (48.25, 2.66, 44.35), (51.2, 2.66, 44.35), (51.2, 1.8, 44.35)), .016)


def tower_guards(assembly, contract):
    # Tread contacts set the baluster feet. The lower outside entrance and
    # the top exit onto the lantern floor stay clear of the outside rail.
    names = {name.split(":", 1)[-1] for name in contract["stair_components"]}
    supports = [o for o in assembly.scene.objects if o.type == "MESH"
                and o.name in names and any(f"route-{n} " in o.name for n in range(20, 25))]
    assert supports, "No original tower treads"
    stair = bvh(supports)
    for label, radius, start, end in (("inside", 2.20, 0, 139), ("outside", 3.36, 4, 137)):
        top, middle = [], []
        for i in range(start, end+1):
            theta = (i+.5)*math.tau/28
            x, z = 32+radius*math.cos(theta), 10+radius*math.sin(theta)
            nominal = 3.6*(i+1)/28
            hit, normal, _, _ = stair.ray_cast(point(x, nominal+.22, z), point(0, -1, 0), .58)
            assert hit is not None and normal.z > .98, (label, i, radius)
            y = hit.z
            top.append((x, y+1.035, z))
            middle.append((x, y+.50, z))
            if (i-start)%4 == 0 or i == end:
                assembly.tube(f"Morrow tower {label} baluster {i}", (x,y-.022,z), (x,y+1.036,z), .034)
        assembly.raceway(f"Morrow tower {label} handrail", top, .045)
        assembly.raceway(f"Morrow tower {label} intermediate rail", middle, .028)
    return stair


def lantern_guard(assembly):
    rim = bvh([obj for obj in assembly.scene.objects if obj.type == "MESH"
               and "floor tower5" in obj.name.lower()])
    radius = 1.12
    plan = [(32+radius*math.cos(i*math.tau/16), 10+radius*math.sin(i*math.tau/16))
            for i in range(17)]
    for i, (x, z) in enumerate(plan[:-1]):
        hit, normal, _, _ = rim.ray_cast(point(x, 18.20, z), point(0, -1, 0), .25)
        assert hit is not None and abs(hit.z-18) < .01 and normal.z > .98, i
        assembly.tube(f"Morrow lantern well baluster {i}", (x,17.98,z), (x,19.04,z), .035)
    for name, height, thickness in (("handrail",19.04,.044), ("midrail",18.50,.028)):
        assembly.raceway(f"Morrow lantern well {name}", [(x,height,z) for x,z in plan], thickness)


def sampled_tread(stair, i):
    angle = (i + .5)*math.tau/28
    x, z = 32 + 2.8*math.cos(angle), 10 + 2.8*math.sin(angle)
    nominal = 3.6*(i+1)/28
    hit, normal, _, _ = stair.ray_cast(point(x, nominal+.18, z), point(0, -1, 0), .42)
    assert hit is not None and normal.z > .98, i
    return x, hit.z, z


def points_at_lantern():
    return [(34.8, 18, 10.8), (33.2, 18, 10.8)]


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    source = {ext: SOURCE / f"coast-entrances.{ext}" for ext in ("blend", "glb", "hcs2")}
    contract = json.loads((SOURCE / "construction.json").read_bytes())
    layout_path = SOURCE / "authored-layout.json"
    for ext, key in (("blend", "blend_sha256"), ("glb", "glb_sha256"), ("hcs2", "physics_sha256")):
        assert sha(source[ext]) == contract[key]
    assert sha(layout_path) == contract["layout_sha256"]
    layout = json.loads(layout_path.read_bytes())
    ownership = {**CHILDREN, **{k: row[0] for k, row in ANCHORS.items()}}
    zones = {z["id"]: {**z, "owner": ownership[z["id"]]} for z in layout["zones"]}
    bpy.ops.wm.open_mainfile(filepath=str(source["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    assembly = Assembly()
    refuges = cupboards(assembly, zones, POCKETS, site="morrow", floor_roles=("floor",))
    power(assembly, zones["generator"])
    stair = tower_guards(assembly, contract)
    lantern_guard(assembly)
    for obj in assembly.built:
        obj.name = obj.name.replace("College ", "", 1)
    target = {ext: OUT / f"lightstation.{ext}" for ext in source}
    addition = ROOT / "build/morrow-point-installation.glb"
    export(addition, assembly.built)
    append(source["glb"], addition, target["glb"], reuse_embedded_images=True)
    native_write(source["hcs2"], target["hcs2"], set(), assembly.built,
                 open_components={p.name for p in assembly.built if p.get("source_asset")})
    prior = native_records(source["hcs2"])
    assert native_records(target["hcs2"])[:len(prior)] == prior
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(target["blend"]), compress=True)
    dock = layout["staging"]
    rows = [("SOURCE", sha(target["glb"]), sha(target["hcs2"])), ("DOCK", *dock["point"], dock["yaw"])]
    for key, z in zones.items():
        floor = z["y"]
        height = 3.6 if key.startswith("tower") else z["height"]
        outside = str(z["kind"] in ("staging", "outside")).lower()
        body = (key, z["owner"], *z["bounds"], floor, height, outside)
        if key in ANCHORS:
            rows.append(("ROOM", *body, z["name"], *ANCHORS[key][1:]))
        else:
            rows.append(("SPACE", *body))
    rows.extend(("SPACE", key, owner, *area, datum, headroom, str(owner == 12).lower())
                for key, owner, area, datum, headroom in COURTS)
    rows.extend(("TREAD", key) for key in contract["stair_components"])
    for i, connection in enumerate(PASSAGES, 940):
        rows.append(("OPEN", i, *connection))
    for rite in layout["rituals"]:
        rows.append(("RITUAL", rite["rite"].lower(), ownership[rite["zone"]], *rite["point"]))
    for socket in assembly.sockets:
        if socket["type"] != "hide":
            rows.append(("SERVICE", socket["id"], socket["type"], socket["room"],
                         *socket["position"], socket["yaw"], socket["style"]))
    for pocket in refuges:
        rows.append(("COVER", pocket["id"], pocket["owner"], *pocket["position"], pocket["yaw"]))
    for route in layout["links"]:
        key = route["id"]
        points = route["points"]
        if key == "route-10":
            points = [(45, 0, 48.30), points[-1]]
        elif key == "route-14":
            points = [points[0], (14, 0, 44.30)]
        elif key == "route-17":
            points = [*points[:-1], sampled_tread(stair, 0)]
        elif int(key.split("-")[-1]) >= 20:
            start = (int(key.split("-")[-1])-20)*28
            points = [sampled_tread(stair, i) for i in range(start, start+28)]
            if key == "route-24":
                points.extend(points_at_lantern())
        rows.extend(("POINT", key, *p) for p in points)
    table(OUT / "runtime-input.tsv", rows)
    leaf = sha(ROOT / "assets/runtime/models/door_1_loose.glb")
    doors = []
    for i, door in enumerate(layout["authored_doorways"]):
        a, b = door["rooms"]
        if door["id"] == "kitchen service door":
            b = "kitchen court"
        x, z = (door["along"], door["boundary"]) if door["axis"] == "z" else (door["boundary"], door["along"])
        width = door["clear_width"]
        doors.append((1200+i, door["id"], a, b, ownership[a], 12 if b == "kitchen court" else ownership[b], x, door["sill_y"], z,
            width, door["axis"], door["boundary"]-.12, door["boundary"]+.12, door["clear_height"],
            2 if width >= 1.7 else 1, 1, SWINGS[i], leaf))
    table(OUT / "door-schedule.tsv", doors)
    (OUT / "construction.json").write_text(json.dumps(dict(
        source=layout_path.relative_to(ROOT).as_posix(), exports={k: sha(p) for k,p in target.items()},
        retained_components=len(prior), refuges=refuges, services=assembly.sockets), indent=2)+"\n",
        encoding="utf-8", newline="\n")


if __name__ == "__main__":
    build()
