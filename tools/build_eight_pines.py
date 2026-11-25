"""Fit Eight Pines Motor Lodge to the existing native investigation."""
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
SOURCE = BASE / "native-foyer-door-pass/wld-16"
OUT = BASE / "eight-pines"
ANCHORS = {
    "room1": (0, 10.8, 0, 22.5), "room2": (1, 10.8, 0, 27.5), "room3": (2, 10.8, 0, 32.5),
    "room4": (3, 10.8, 0, 37.5), "office": (4, 18, 0, 46),
    "room5": (5, 41.2, 0, 22.5), "room6": (6, 41.2, 0, 27.5), "room7": (7, 41.2, 0, 32.5),
    "room8": (8, 41.2, 0, 37.5), "manager": (9, 9.5, 0, 45), "laundry": (10, 42, 0, 43),
    "plant": (11, 42.5, 0, 49), "court": (12, 26, 0, 31), "stage": (13, 26, 0, 64),
}
CHILDREN = {"entry": 13, "westwalk": 12, "eastwalk": 12, "northwalk": 12, "service": 12}
RECEIVERS = (
    ("office drive", 13, (16.8, 50, 34, 57), 0, 3, True),
    ("rear drive", 12, (23, 42, 39, 52), 0, 3, True),
    ("north service return", 12, (38.5, 17, 52, 23), 0, 3, True),
    ("south service return", 12, (34, 51, 53, 57), 0, 3, True),
)
OPENINGS = ((13,12,26,0,51,2.4,"z","trail"),)
EXTERIOR = {0:"court",1:"office drive",4:"eastwalk",5:"service",
            **{i:"westwalk" for i in range(6,10)}, **{i:"eastwalk" for i in range(10,14)}}
SWINGS = (1,-1,-1,1,1,-1,-1,-1,-1,-1,1,1,1,1)
REFUGES = (("manager", 7.55, 48.0, -math.pi/2), ("room2", 7.45, 28.8, -math.pi/2),
           ("room6", 44.55, 28.8, math.pi/2))


def plant_power(assembly, zone):
    for asset, kind, x, z, yaw, y in (
        ("level_trace_generator_refined","generator",44.2,47.6, math.pi,.56),
        ("level_electrical_bank_worn","breaker",41.15,51.35,math.pi,.89),
    ):
        parts = assembly.place(asset,Matrix.Translation(point(x,.06,z)) @ Matrix.Rotation(yaw,4,"Z"),
                               f"Eight Pines {kind}", "founded plant-room electrical equipment", "plant")
        low, high = bounds(parts)
        assert abs(low[1]-.06)<.004
        for px in (low[0],high[0]):
            for pz in (low[2],high[2]):
                hit, _, _ = assembly.measured(point(px,.18,pz),point(0,-1,0),.30,kind+" slab")
                assert abs(hit.z)<.006
        assembly.box(f"Eight Pines {kind} plinth",(low[0]-.04,low[2]-.04,high[0]+.04,high[2]+.04),
                     0,.06,assembly.concrete,"equipment foundation","plant")
        sign = 1 if kind == "generator" else -1
        hit, _, _, _ = bvh(parts).ray_cast(point(x,y,high[2]+1 if sign==1 else low[2]-1),
                                          point(0,0,-sign),2)
        assert hit is not None, kind
        x_hit,y_hit,z_hit=world(hit)
        assembly.effect("pines-"+kind,kind,zone["owner"],(x_hit,y_hit,z_hit+.015*sign),
                        0 if sign==1 else math.pi,"EP-MAIN")
    assembly.raceway("Eight Pines main feeder",((44.2,.55,47.65),(45.73,.55,47.65),
        (45.73,2.72,47.65),(45.73,2.72,51.72),(41.15,2.72,51.72),(41.15,1.87,51.67)),.016)


def room_power(assembly, zones):
    branches = [
        ("office",(20.2,48.9),(21.0,48.9),(0,1)), ("manager",(7.12,43),(7.12,44.6),(-1,0)),
        ("laundry",(44.8,42),(44.8,44),(1,0)), ("plant",(43.8,50.75),(44.6,50.75),(0,1)),
    ]
    for n in range(1,9):
        east = n>4
        center = 22.5 + ((n-1)%4)*5
        x = 44.8 if east else 7.12
        branches.append((f"room{n}",(x,center-1),(x,center+.85),(1 if east else -1,0)))
    for room, switch, lamp, (dx,dz) in branches:
        zone=zones[room]
        name=f"pines-{room}"
        assembly.switch(zone,*switch,dx,dz)
        assembly.sockets[-1].update(id=name+"-switch",style=f"EP-{zone['owner']:02d}")
        drop=assembly.routes[-1]["points"][-1]
        direction=point(dx,0,dz)
        assembly.mounted_lamp(zone,name+"-light",point(lamp[0],2.35,lamp[1]),direction,1.7,point(-dz,0,dx))
        x,y,z=assembly.loads[-1]["mount"]
        endpoint=(x-.028*dx,drop[1],z-.028*dz)
        assert abs((endpoint[0]-drop[0])*dx+(endpoint[2]-drop[2])*dz)<.012,room
        assembly.raceway(f"Eight Pines {room} light circuit",(drop,endpoint,(endpoint[0],y+.12,endpoint[2])),.012)


def build():
    OUT.mkdir(parents=True,exist_ok=True)
    source={ext:SOURCE/f"fitted-approaches.{ext}" for ext in ("blend","glb","hcs2")}
    contract=json.loads((SOURCE/"construction.json").read_bytes())
    layout_path=SOURCE/"authored-layout.json"
    for ext,key in (("blend","blend_sha256"),("glb","glb_sha256"),("hcs2","physics_sha256")):
        assert sha(source[ext])==contract[key]
    assert sha(layout_path)==contract["layout_sha256"]
    layout=json.loads(layout_path.read_bytes())
    owners={**CHILDREN,**{name: row[0] for name,row in ANCHORS.items()}}
    zones={z["id"]:{**z,"owner":owners[z["id"]]} for z in layout["zones"]}
    bpy.ops.wm.open_mainfile(filepath=str(source["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    assembly=Assembly()
    pockets=cupboards(assembly,zones,REFUGES,site="pines",floor_roles=("floor","constructed continuous room or arcade floor"))
    plant_power(assembly,zones["plant"])
    room_power(assembly,zones)
    for obj in assembly.built:
        obj.name=obj.name.removeprefix("College ")
    target={ext:OUT/f"motor-lodge.{ext}" for ext in source}
    addition=ROOT/"build/eight-pines-installation.glb"
    export(addition,assembly.built)
    append(source["glb"],addition,target["glb"],reuse_embedded_images=True)
    native_write(source["hcs2"],target["hcs2"],set(),assembly.built,
                 open_components={p.name for p in assembly.built if p.get("source_asset")})
    prior=native_records(source["hcs2"])
    assert native_records(target["hcs2"])[:len(prior)]==prior
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(target["blend"]),compress=True)
    dock=layout["staging"]
    rows=[("SOURCE",sha(target["glb"]),sha(target["hcs2"])),("DOCK",*dock["point"],dock["yaw"])]
    for key,z in zones.items():
        outside=str(z["kind"] in ("outside","staging")).lower()
        body=(key,owners[key],*z["bounds"],z["y"],z["height"],outside)
        rows.append(("ROOM",*body,z["name"],*ANCHORS[key][1:]) if key in ANCHORS else ("SPACE",*body))
    rows.extend(("SPACE",key,owner,*area,datum,height,str(outside).lower())
                for key,owner,area,datum,height,outside in RECEIVERS)
    for i,p in enumerate(OPENINGS,930):
        rows.append(("OPEN",i,*p))
    for rite in layout["rituals"]:
        rows.append(("RITUAL",rite["rite"].lower(),owners[rite["zone"]],*rite["point"]))
    for socket in assembly.sockets:
        if socket["type"]!="hide":
            rows.append(("SERVICE",socket["id"],socket["type"],socket["room"],*socket["position"],
                         socket["yaw"],socket["style"]))
    for pocket in pockets:
        rows.append(("COVER",pocket["id"],pocket["owner"],*pocket["position"],pocket["yaw"]))
    for route in layout["links"]:
        rows.extend(("POINT",route["id"],*p) for p in route["points"])
    table(OUT/"runtime-input.tsv",rows)
    leaf=sha(ROOT/"assets/runtime/models/door_1_loose.glb")
    doors=[]
    for i,door in enumerate(layout["authored_doorways"]):
        a=door["rooms"][0]
        b=door["rooms"][1] if len(door["rooms"])==2 else EXTERIOR[i]
        x,y,z=door["frame_center"]
        other=owners[b] if b in owners else next(r[1] for r in RECEIVERS if r[0]==b)
        width=door["clear_width"]
        doors.append((1400+i,door["id"],a,b,owners[a],other,x,y,z,width,
                      door["axis"],*door["frame_span"],door["clear_height"],
                      2 if width>=1.7 else 1,1,SWINGS[i],leaf))
    table(OUT/"door-schedule.tsv",doors)
    (OUT/"construction.json").write_text(json.dumps(dict(
        source=layout_path.relative_to(ROOT).as_posix(),exports={k:sha(p) for k,p in target.items()},
        retained_components=len(prior),pockets=pockets,services=assembly.sockets),indent=2)+"\n",
        encoding="utf-8",newline="\n")


if __name__=="__main__":
    build()
