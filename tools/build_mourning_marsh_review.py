"""Fit walk-in refuges and bind the surveyed Mourning Marsh settlement to native gameplay."""
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
from build_roster_foundations import material
from verify_college_services import bvh, match_geometry

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


def south_crossing_guards(assembly):
    finish=material("Marsh treated boardwalk guard timber",(.26,.21,.145))
    deck=bvh([obj for obj in assembly.scene.objects if obj.type=="MESH"
              and "continuous settlement boardwalk" in obj.name])
    z=61.87
    for side,a,b in (("west",28.14,38.62),("east",41.38,51.86)):
        bays=math.ceil((b-a)/1.85)
        for i in range(bays+1):
            x=a+(b-a)*i/bays
            for px in (x-.055,x+.055):
                for pz in (z-.054,z+.054):
                    hit,normal,_,_=deck.ray_cast(point(px,1.35,pz),point(0,-1,0),.24)
                    assert hit is not None and abs(hit.z-1.2)<.004 and normal.z>.98,(px,pz)
            assembly.box(f"Marsh south {side} guard post {i}",(x-.055,z-.055,x+.055,z+.055),
                         1.17,2.28,finish,"boardwalk edge post fixed to deck","south-deck")
        for part,y in (("mid rail",1.72),("handrail",2.23)):
            assembly.box(f"Marsh south {side} guard {part}",(a-.045,z-.05,b+.045,z+.05),
                         y-.045,y+.045,finish,"guard at the lower water edge","south-deck")


def pump_services(assembly, zone):
    floor=zone["y"]
    for asset,kind,x,z,control in (
        ("level_trace_generator_refined","generator",61.6,61.85,.56),
        ("level_electrical_bank_worn","breaker",58.9,63.28,.89),
    ):
        parts=assembly.place(asset,Matrix.Translation(point(x,floor+.06,z)) @ Matrix.Rotation(math.pi,4,"Z"),
                             f"Mourning Marsh {kind}","pump shelter electrical equipment","pump")
        low,high=bounds(parts)
        for px in (low[0]-.03,high[0]+.03):
            for pz in (low[2]-.03,high[2]+.03):
                hit,_,_=assembly.measured(point(px,floor+.17,pz),point(0,-1,0),.26,kind)
                assert abs(hit.z-floor)<.004
        assembly.box(f"Mourning Marsh {kind} plinth",(low[0]-.03,low[2]-.03,high[0]+.03,high[2]+.03),
                     floor,floor+.06,assembly.concrete,"equipment secured above the deck","pump")
        hit,_,_,_=bvh(parts).ray_cast(point(x,floor+control,low[2]-.9),point(0,0,1),1.8)
        assert hit is not None,kind
        px,py,pz=world(hit)
        assembly.effect("marsh-"+kind,kind,zone["owner"],(px,py,pz-.016),math.pi,"MM-MAIN")
    assembly.raceway("Marsh shelter feeder",((61.6,1.72,62.1),(63.85,1.72,62.1),
                     (63.85,3.7,62.1),(63.85,3.7,63.5),(58.9,3.7,63.5)),.016)
    pump_fixture(assembly,zone)


def pump_fixture(assembly,zone):
    # Use the existing compact pump model and keep its support and ports on
    # the pump shelter's raised deck, away from the cleansing position.
    asset="level_blockout_cinder_sump_pump"
    source=ROOT/"development/levels/primitive-props-v1/models"/(asset+".glb")
    before=set(assembly.scene.objects)
    bpy.ops.import_scene.gltf(filepath=str(source))
    imported=set(assembly.scene.objects)-before
    assembly.assets[asset]=[(obj.data,obj.matrix_world.copy()) for obj in sorted(imported,key=lambda o:o.name)
                            if obj.type=="MESH"]
    for obj in imported:
        bpy.data.objects.remove(obj,do_unlink=True)
    parts=assembly.place(asset,Matrix.Identity(4),"Mourning Marsh pump",
                         "founded settlement transfer pump","pump")
    x,z,floor=61.5,55.7,zone["y"]
    low,high=bounds(parts)
    scale=min(1.72/(high[0]-low[0]),.95/(high[2]-low[2]),1.0)
    frame=Matrix.Translation(point(x-scale*(low[0]+high[0])/2,floor+.08-scale*low[1],
                                  z-scale*(low[2]+high[2])/2)) @ Matrix.Scale(scale,4)
    for part in parts:
        part.matrix_world=frame @ part.matrix_world
    bpy.context.view_layer.update()
    low,high=bounds(parts)
    for px in (low[0]-.04,high[0]+.04):
        for pz in (low[2]-.04,high[2]+.04):
            hit,_,_=assembly.measured(point(px,floor+.18,pz),point(0,-1,0),.27,"pump footing")
            assert abs(hit.z-floor)<.004
    assembly.box("Mourning Marsh pump foundation",(low[0]-.04,low[2]-.04,high[0]+.04,high[2]+.04),
                 floor,floor+.08,assembly.concrete,"pump plinth seated on original deck","pump")
    assert len(parts)==1,"Expected one fitted pump casing"
    part=parts[0]
    casing=bvh(parts)
    face=None
    for tx,py in ((x-.28,.48),(x+.28,.48),(x-.28,.31),(x+.28,.31),(x,.18),(x,.12)):
        face,_,_,_=casing.ray_cast(point(tx,floor+py,high[2]+.9),point(0,0,-1),2)
        if face is not None:break
    assert face is not None,"Pump has no reachable casing"
    part.name=part.name.removeprefix("College ")
    px,py,pz=world(face)
    assembly.effect(part.name,"native-device",zone["owner"],(px,py,pz+.045),0,"WATER_PUMP")
    assembly.raceway("Marsh pump outlet and wall riser",((high[0]-.15,floor+.28,z),(63.83,floor+.28,z),
                      (63.83,floor+.75,z),(63.97,floor+.75,z)),.055)


def room_lights(assembly,zones):
    branches=(
        ("house-a",26.92,43.1,41.3,1),
        ("house-b",53.08,43.1,41.3,-1),
        ("chapel",26.92,22.2,20.4,1),
        ("warehouse",33.1,28.0,29.8,-1),
        ("shack",53.08,27.5,29.6,-1),
        ("pump",53.08,60.9,62.7,-1),
    )
    for room,x,sz,lz,dx in branches:
        zone=zones[room]
        assembly.switch(zone,x,sz,dx,0)
        assembly.sockets[-1].update(id=f"marsh-{room}-switch",style=f"MM-{room.upper()}")
        drop=assembly.routes[-1]["points"][-1]
        assembly.mounted_lamp(zone,f"marsh-{room}-light",point(x,zone["y"]+2.35,lz),
                              point(dx,0,0),1.7,point(0,0,1))
        wall_x,height,z=assembly.loads[-1]["mount"]
        join=(wall_x-dx*.028,drop[1],z)
        assert abs(drop[0]-join[0])<.012,room
        assembly.raceway(f"Marsh {room} circuit",(drop,join,(join[0],height+.12,join[2])),.012)


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
    cupboard_parts=set(assembly.built)
    south_crossing_guards(assembly)
    pump_services(assembly,zones["pump"])
    room_lights(assembly,zones)
    for piece in assembly.built:
        piece.name = piece.name.removeprefix("College ")
    target = {ext: OUT / f"marsh-review.{ext}" for ext in source}
    addition = ROOT / "build/marsh-refuges.glb"
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
    # The causeway widens where it turns onto the supported low landing.
    rows.append(("SPACE", "causeway-turn", OWNERS["entry"][0], 37.7, 70, 42.3, 74, 0, 3, "true"))
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
        "services":[s for s in assembly.sockets if s["type"]!="hide"],
    }
    (OUT / "construction.json").write_bytes((json.dumps(record, indent=2) + "\n").encode())
    print("PASS retained", len(retained), "native parts; refuge floor contacts",
          [refuge["base_contacts"] for refuge in refuges])


if __name__ == "__main__":
    build()
