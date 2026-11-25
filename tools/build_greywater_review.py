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
from build_college_electrical import bounds, point, world
from build_college_grounds import import_scene
from build_lookout_native_arrival import table
from build_mine_case import Case, DOWN
from build_prison_sally_gate import export
from build_roster_foundations import material
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
    assembly.sockets[-1].update(id="greywater-boiler-switch",style="GW-BOILER")
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


def room_services(assembly, zones):
    branches=(
        ("wash",22.95,49.3,(47.2,52.0),1),
        ("cold",62.9,33.5,(29.1,35.8),1),
        ("office",25.15,18.5,(21.4,),-1),
        ("stores",46.85,21.0,(23.1,),1),
        ("bunk",6.85,55.7,(47.1,58.5),1),
        ("mess",-6.85,31.6,(28.8,38.5),-1),
        ("processing",46.85,31.1,(29,39.1,48.25,53.3),1),
        ("gallery",25.15,33.0,(30.4,38.45),-1),
    )
    for room,x,switch_z,lamps,dx in branches:
        zone=zones[room]
        floor=zone["y"]
        assembly.switch(zone,x,switch_z,dx,0)
        assembly.sockets[-1].update(id=f"greywater-{room}-switch",style=f"GW-{room.upper()}")
        drop=assembly.routes[-1]["points"][-1]
        junctions=[drop]
        for number,lamp_z in enumerate(lamps,1):
            key=f"greywater-{room}-light-{number}"
            assembly.mounted_lamp(zone,key,point(x,floor+2.36,lamp_z),point(dx,0,0),1.85,point(0,0,1))
            wall_x,height,z=assembly.loads[-1]["mount"]
            junction=(wall_x-dx*.028,drop[1],z)
            assert abs(junction[0]-drop[0])<.012,(room,key,junction,drop)
            junctions.append(junction)
            assembly.raceway(f"Greywater {room} lamp drop {number}",(junction,(junction[0],height+.095,z)),.012)
        junctions.sort(key=lambda p:p[2])
        assembly.raceway(f"Greywater {room} raceway",junctions,.012)


def dispatch_log(assembly):
    desk=assembly.scene.objects["level_blockout_headmaster_desk.001"]
    x,z=27.15,14.64
    hit,normal,_,_=bvh([desk]).ray_cast(point(x,1.02,z),point(0,-1,0),.34)
    assert hit is not None and normal.z>.98
    parts=assembly.place("notebook_1",Matrix.Identity(4),"dispatch log","readable loading log","office")
    assert len(parts)==1
    book=parts[0]
    book.name="Greywater dispatch log"
    low,high=bounds(parts)
    scale=min(.35/(high[0]-low[0]),.29/(high[2]-low[2]),1)
    t=Matrix.Translation(point(x-scale*(low[0]+high[0])/2,hit.z+.002-scale*low[1],
                               z-scale*(low[2]+high[2])/2))
    book.matrix_world=t @ Matrix.Scale(scale,4) @ book.matrix_world
    bpy.context.view_layer.update()
    _,high=bounds(parts)
    assembly.effect(book.name,"native_story",OWNERS["office"][0],(x,high[1],z),0,"GREYWATER_DISPATCH_LOG")


def workrooms(case):
    fittings=[]
    for asset, name, room, x, z in (
        ("level_trace_camp_bed", "crew bunk", "bunk", 4.5, 46.8),
        ("dinner_table_1", "crew mess table", "mess", 3.6, 29.4),
        ("level_camp_bench", "crew mess seats", "mess", 3.6, 30.42),
        ("shelf_mp_6", "cold stock shelf", "cold", 59.8, 27.23),
        ("level_work_repair_bench", "packing workbench", "stores", 46.35, 16.75),
    ):
        _, receipt=case.fit(asset,name,room,x,0,z)
        fittings.append(dict(room=room,name=name,**receipt))

    x,z=20.6,44.82
    parts=case.place("level_bath_basin",Matrix.Translation(point(x,0,z)),
                     "washroom basin","floor-bearing washbasin","wash")
    assert len(parts)==1
    parts[0].name="Greywater workers washbasin"
    for px in (x-.08,x+.08):
        for pz in (z-.08,z+.08):
            contact,normal,_,_=case.support_surfaces.ray_cast(point(px,.16,pz),DOWN,.24)
            assert contact is not None and abs(contact.z)<.004 and normal.z>.98,(px,pz)
    wall,_,_=case.measured(point(x,.65,z-.27),point(0,0,-1),.52,"Washroom plumbing wall")
    _,_,back=world(wall)
    copper=material("Greywater oxidized washroom supply",(.29,.16,.09))
    iron=material("Greywater painted iron waste",(.17,.18,.17))
    case.tube("Greywater basin supply riser",(x,.13,back+.035),(x,.97,back+.035),.017,copper)
    case.tube("Greywater basin feed union",(x,.97,back+.035),(x,.97,z-.14),.017,copper)
    case.tube("Greywater basin waste union",(x,.25,z-.09),(x,.25,back+.026),.027,iron)
    case.effect(parts[0].name,"native-device",OWNERS["wash"][0],(x,.86,z),0,"SINK_BATH")
    fittings.append(dict(room="wash",name="workers washbasin",floor=0,wall=back,bounds=bounds(parts)))
    return fittings


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
    assembly = Case()
    refuges = cupboards(assembly, zones, REFUGES, site="greywater", floor_roles=("floor",))
    power(assembly, zones["boiler"])
    room_services(assembly,zones)
    dispatch_log(assembly)
    fittings = workrooms(assembly)
    for piece in assembly.built:
        piece.name = piece.name.removeprefix("College ").replace("Mine case ", "Greywater ", 1)
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
        "workroom_fittings": fittings,
        "service_sockets": [s for s in assembly.sockets if s["type"] != "hide"],
    }
    (OUT / "construction.json").write_bytes((json.dumps(record, indent=2) + "\n").encode())
    print("PASS retained", len(retained), "native parts; refuge floor contacts",
          [refuge["base_contacts"] for refuge in refuges])


if __name__ == "__main__":
    build()
