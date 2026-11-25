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
from review_roster_macroterrain import records, sha
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


def room_furniture(assembly, zones):
    placements = [
        ("office", "reception counter", "table_large_2", 21.35, 46.1, .85, 2.18),
        ("office", "waiting chair", "armchair_1", 15.0, 48.4, .95, .95),
        ("manager", "manager bed", "bed_2", 8.35, 42.1, 1.55, 2.15),
        ("manager", "linen press", "level_lived_linen_press_worn", 11.65, 48.7, .85, .60),
        ("laundry", "washer", "level_laundry_washer_worn", 43.7, 41.05, .80, .75),
        ("laundry", "linen shelf", "level_laundry_shelf_worn", 44.3, 45.15, 1.65, .60),
    ]
    for number in range(1, 9):
        east = number > 4
        center = 22.5 + (number - 1) % 4 * 5
        room = f"room{number}"
        placements.extend([
            (room, "guest bed", "bed_1" if number in (1, 3, 6, 8) else "bed_2",
             43.5 if east else 8.5, center - .50, 1.72, 2.18),
            (room, "bedside table", "bedside_table_1",
             42.05 if east else 9.95, center - 1.40, .60, .58),
        ])

    installed = []
    for room, name, asset, x, z, width, depth in placements:
        zone = zones[room]
        parts = assembly.place(asset, Matrix.Identity(4), f"Eight Pines {room} {name}",
                               "fixed room furniture", room)
        low, high = bounds(parts)
        turn = (high[0] - low[0] > high[2] - low[2]) != (width > depth)
        rotation = Matrix.Rotation(math.pi / 2 if turn else 0, 4, "Z")
        for part in parts:
            part.matrix_world = rotation @ part.matrix_world
        bpy.context.view_layer.update()
        low, high = bounds(parts)
        model_width, model_depth = high[0] - low[0], high[2] - low[2]
        scale = min(width / model_width, depth / model_depth, 1.15)
        y = zone["y"]
        transform = Matrix.Translation(point(
            x - scale * (high[0] + low[0]) / 2, y - scale * low[1],
            z - scale * (high[2] + low[2]) / 2)) @ Matrix.Scale(scale, 4)
        half_x, half_z = model_width * scale / 2, model_depth * scale / 2
        x0, z0, x1, z1 = zone["bounds"]
        assert x0 + .27 < x - half_x < x + half_x < x1 - .27, (room, name)
        assert z0 + .27 < z - half_z < z + half_z < z1 - .27, (room, name)
        for px in (x - half_x, x + half_x):
            for pz in (z - half_z, z + half_z):
                hit, _, _ = assembly.measured(point(px, y + .17, pz), point(0, -1, 0), .28,
                                              f"{room} {name} slab")
                assert abs(hit.z - y) < .006, (room, name, hit)
        for part in parts:
            part.matrix_world = transform @ part.matrix_world
        bpy.context.view_layer.update()
        low, high = bounds(parts)
        assert abs(low[1] - y) < .004
        installed.append(dict(room=room, asset=asset, use=name, bounds=[low, high]))
        if name=="reception counter":
            guest_register(assembly,zone["owner"],parts,x,z,high[1])
        if name=="washer":
            assert len(parts)==1
            part=parts[0]
            part.name=part.name.removeprefix("College ")
            ray_y=y+.57
            hit, _, _, _=bvh(parts).ray_cast(point(x,ray_y,high[2]+.9),point(0,0,-1),1.8)
            assert hit is not None, "Washer needs a reachable interaction face"
            px,py,pz=world(hit)
            assembly.effect(part.name,"native-device",zone["owner"],(px,py,pz+.045),0,"WASHING_MACHINE")
    return installed


def guest_register(assembly, owner, counter, x, z, top):
    hit, normal, _, _ = bvh(counter).ray_cast(point(x,top+.1,z),point(0,-1,0),.35)
    assert hit is not None and normal.z>.98, ("Guest register needs a flat counter",hit,normal)
    parts = assembly.place("notebook_1",Matrix.Identity(4),"Eight Pines guest register",
                           "readable guest register","office")
    low, high = bounds(parts)
    scale = min(.32/(high[0]-low[0]),.30/(high[2]-low[2]),1.0)
    transform = Matrix.Translation(point(x-scale*(high[0]+low[0])/2,hit.z+.002-scale*low[1],
                                         z-scale*(high[2]+low[2])/2)) @ Matrix.Scale(scale,4)
    for part in parts:
        part.matrix_world = transform @ part.matrix_world
    bpy.context.view_layer.update()
    # Register every visible mesh part so the ray can resolve the same document.
    for part in parts:
        part.name=part.name.removeprefix("College ")
        _, upper = bounds([part])
        assembly.effect(part.name,"native_story",owner,(x,upper[1],z),0,"PINES_GUEST_REGISTER")


def collision_records(obj):
    # The engine rejects zero-area faces after world coordinates become floats.
    # A few of the existing decorative GLBs have collapsed seam triangles.
    valid = []
    for points, flags in records(obj):
        ax, ay, az, bx, by, bz, cx, cy, cz = points
        ux, uy, uz = bx-ax, by-ay, bz-az
        vx, vy, vz = cx-ax, cy-ay, cz-az
        area_squared = (uy*vz-uz*vy)**2 + (uz*vx-ux*vz)**2 + (ux*vy-uy*vx)**2
        if area_squared >= 1e-24:
            valid.append((points, flags))
    assert valid, obj.name
    return valid


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
    placeholders={"level_blockout_motel_reception_counter.001", "level_blockout_motel_laundry_unit.001",
                  *(f"level_blockout_motel_guest_bed.{n:03d}" for n in range(1,9))}
    for name in sorted(placeholders):
        obj=bpy.data.objects[name]
        assert obj.get("role")=="placed source-library furnishing", name
        bpy.data.objects.remove(obj,do_unlink=True)
    bpy.context.view_layer.update()
    assembly=Assembly()
    pockets=cupboards(assembly,zones,REFUGES,site="pines",floor_roles=("floor","constructed continuous room or arcade floor"))
    plant_power(assembly,zones["plant"])
    room_power(assembly,zones)
    furniture=room_furniture(assembly,zones)
    for obj in assembly.built:
        obj.name=obj.name.removeprefix("College ")
    target={ext:OUT/f"motor-lodge.{ext}" for ext in source}
    addition=ROOT/"build/eight-pines-installation.glb"
    export(addition,assembly.built)
    append(source["glb"],addition,target["glb"],remove_mesh_names=sorted(placeholders),reuse_embedded_images=True)
    native_write(source["hcs2"],target["hcs2"],placeholders,assembly.built,
                 open_components={p.name for p in assembly.built if p.get("source_asset")}, mesh_records=collision_records)
    source_records=native_records(source["hcs2"])
    prior=[record for record in source_records if record[0].split(":",1)[-1] not in placeholders]
    assert len(source_records)-len(prior)==len(placeholders)
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
        retained_components=len(prior),replaced_placeholders=sorted(placeholders),pockets=pockets,services=assembly.sockets,furniture=furniture),indent=2)+"\n",
        encoding="utf-8",newline="\n")


if __name__=="__main__":
    build()
