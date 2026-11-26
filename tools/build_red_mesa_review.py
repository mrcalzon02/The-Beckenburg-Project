"""Fit canonical walk-in cupboards and bind the measured Red Mesa plan to native gameplay."""
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
from build_school_electrical import cupboards
from compose_room_terrain import split_native
from review_roster_macroterrain import records
from build_roster_foundations import material
from verify_college_services import bvh, match_geometry

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
    ("lower-ledge", "lower-path", (12, 32.2, 55.75, 44.25), 4, 2.6),
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


def guard_lower_ledge(assembly):
    rail=material("Red Mesa powder-coated expedition rail",(.18,.16,.13))
    deck=bvh([assembly.scene.objects["Red Mesa full lower cave floor and landing region0"]])
    span=(((47.64,44.10),(55.60,44.10)),((55.60,44.10),(55.60,40.21)))
    seen=set()
    for index,(a,b) in enumerate(span,1):
        length=math.dist(a,b)
        bays=math.ceil(length/1.95)
        for post in range(bays+1):
            t=post/bays
            x,z=(a[i]+t*(b[i]-a[i]) for i in range(2))
            if (x,z) in seen:continue
            seen.add((x,z))
            for px in (x-.055,x+.055):
                for pz in (z-.055,z+.055):
                    hit,n,_,_=deck.ray_cast(point(px,4.14,pz),point(0,-1,0),.23)
                    assert hit is not None and abs(hit.z-4)<.004 and n.z>.98,(px,pz)
            assembly.box(f"Red Mesa lower ledge post {index}-{post}",
                         (x-.055,z-.055,x+.055,z+.055),3.96,5.10,rail,
                         "supported lower landing guard post","lower-ledge")
        x0,x1=sorted((a[0],b[0]))
        z0,z1=sorted((a[1],b[1]))
        for edge,y in (("mid",4.52),("top",5.04)):
            assembly.box(f"Red Mesa lower ledge {edge} rail {index}",
                         (x0-.04,z0-.04,x1+.04,z1+.04),y-.045,y+.045,rail,
                         "fixed lower landing edge guard","lower-ledge")


def expedition_supply(assembly,zone):
    equipment=(
        ("level_trace_generator_refined","generator",16.35,56.5,.56),
        ("level_electrical_bank_worn","breaker",18.45,56.45,.89),
    )
    for asset,kind,x,z,control_y in equipment:
        pieces=assembly.place(asset,Matrix.Translation(point(x,.06,z)) @ Matrix.Rotation(math.pi,4,"Z"),
                               f"Red Mesa shelter {kind}","expedition electrical equipment","shelter")
        low,high=bounds(pieces)
        assert abs(low[1]-.06)<.004
        for px in (low[0]-.03,high[0]+.03):
            for pz in (low[2]-.03,high[2]+.03):
                hit,_,_=assembly.measured(point(px,.17,pz),point(0,-1,0),.26,kind)
                assert abs(hit.z)<.004
        assembly.box(f"Red Mesa {kind} pad",(low[0]-.03,low[2]-.03,high[0]+.03,high[2]+.03),
                     0,.06,assembly.concrete,"sealed plant equipment foundation","shelter")
        face,_,_,_=bvh(pieces).ray_cast(point(x,control_y,low[2]-.9),point(0,0,1),1.8)
        assert face is not None,kind
        px,py,pz=world(face)
        assembly.effect("red-mesa-"+kind,kind,zone["owner"],(px,py,pz-.018),math.pi,"RM-MAIN")
    assembly.raceway("Red Mesa service feeder",((16.35,.55,56.7),(16.35,.55,57.88),
                      (16.35,2.55,57.88),(18.45,2.55,57.88),(18.45,1.88,56.90)),.018)


def room_services(assembly,zones):
    branches=(
        ("shelter",(18.85,49.0),((18.85,47.4),),(1,0)),
        ("wash",(58.85,49.4),((58.85,51.0),),(1,0)),
        ("gallery",(48.95,26.1),((48.95,24.25),(48.95,35.2)),(-1,0)),
        ("fork",(74.85,30.4),((74.85,32.3),),(1,0)),
        ("chamber",(80.85,11.0),((80.85,9.2),(80.85,15.9)),(1,0)),
        ("lower-cave",(60.7,38.9),((62.5,38.9),),(0,1)),
        ("alcove",(58.85,46.7),((58.85,48.6),),(1,0)),
    )
    for room,switch,lights,(dx,dz) in branches:
        zone=zones[room]
        assembly.switch(zone,*switch,dx,dz)
        name="red-mesa-"+room
        assembly.sockets[-1].update(id=name+"-switch",style=f"RM-{zone['owner']:02d}")
        drop=assembly.routes[-1]["points"][-1]
        junctions=[drop]
        for i,(x,z) in enumerate(lights,1):
            assembly.mounted_lamp(zone,f"{name}-light-{i}",point(x,zone["y"]+2.38,z),
                                  point(dx,0,dz),1.75,point(-dz,0,dx))
            mx,my,mz=assembly.loads[-1]["mount"]
            junction=(mx-dx*.028,drop[1],mz-dz*.028)
            assert abs((junction[0]-drop[0])*dx+(junction[2]-drop[2])*dz)<.012,room
            junctions.append(junction)
            assembly.raceway(f"Red Mesa {room} lamp drop {i}",(junction,(junction[0],my+.12,junction[2])),.012)
        junctions.sort(key=lambda p:p[2] if dx else p[0])
        assembly.raceway(f"Red Mesa {room} distribution",junctions,.012)


def fit_shelters(case):
    desk,placement=case.fit("level_workbench_worn","expedition communications bench","shelter",7.65,0,47.7)
    top,normal,_,_=bvh(desk).ray_cast(point(7.65,1.65,47.7),DOWN,1.6)
    assert top is not None and normal.z>.98,"Expedition radio needs a measured desk surface"
    radio,device=case.fit("level_event_radio_worn","expedition base radio","shelter",7.65,top.z+.001,47.7,support=desk)
    assert len(radio)==1
    radio[0].name="Red Mesa expedition base radio"
    low,high=bounds(radio)
    face,_,_,_=bvh(radio).ray_cast(point(7.65,low[1]+.065,high[2]+.8),point(0,0,-1),1.6)
    assert face is not None,"Expedition radio needs an accessible casing"
    x,y,z=world(face)
    case.effect(radio[0].name,"native-device",0,(x,y,z+.035),0,"RADIO")
    fittings=[dict(room="shelter",name="expedition base radio",desk=placement,radio=device)]
    for asset,name,zone,x,z in (
        ("level_camp_bench","expedition rest bench","shelter",8.2,56.20),
        ("level_work_crew_footlocker","expedition gear chest","shelter",6.8,50.4),
        ("level_work_repair_bench","sample sorting bench","wash",52.1,55.9),
        ("shelf_mp_6","sample storage rack","wash",50.5,47.65),
    ):
        _,placement=case.fit(asset,name,zone,x,0,z)
        fittings.append(dict(room=zone,name=name,**placement))
    return fittings


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
    assembly = Case()
    refuges = cupboards(assembly, zones, REFUGES, site="red-mesa", floor_roles=("floor",))
    cupboard_parts=set(assembly.built)
    guard_lower_ledge(assembly)
    expedition_supply(assembly,zones["shelter"])
    room_services(assembly,zones)
    fittings=fit_shelters(assembly)
    for piece in assembly.built:
        piece.name = piece.name.removeprefix("College ").replace("Mine case ","Red Mesa ",1)
    target = {ext: OUT / f"red-mesa-review.{ext}" for ext in source}
    addition = ROOT / "build/red-mesa-refuges.glb"
    export(addition, assembly.built)
    append(source["glb"], addition, target["glb"], reuse_embedded_images=True)
    parts = native_write(source["hcs2"], target["hcs2"], set(), assembly.built,
                         open_components={obj.name for obj in assembly.built if obj in cupboard_parts or obj.get("source_asset")})
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
    for service in assembly.sockets:
        if service["type"]!="hide":
            rows.append(("SERVICE",service["id"],service["type"],service["room"],*service["position"],
                         service["yaw"],service["style"]))
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
        "services":[s for s in assembly.sockets if s["type"]!="hide"],
        "shelter_furnishings":fittings,
    }
    (OUT / "construction.json").write_bytes((json.dumps(record, indent=2) + "\n").encode())
    print("PASS retained", len(retained), "native parts; refuge floor contacts",
          [refuge["base_contacts"] for refuge in refuges])


if __name__ == "__main__":
    build()
