"""Connect the measured Saint Elara quay, decks and cabins to the native mission."""
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
SOURCE = BASE / "coast-entrances-pass/wld-15"
OUT = BASE / "saint-elara"
ANCHORS = {
    "aft": (0, 32, 0, 47), "galley": (1, 28, 0, 35), "mess": (2, 36, 0, 35),
    "engine": (3, 32, -3, 32), "processing": (4, 32, 0, 19),
    "fore": (5, 32, 0, 3), "hold": (6, 32, -3, 14), "pump": (7, 32, -3, -.5),
    "crew": (8, 32, 3, 35), "bridge": (9, 30, 6, 24), "net": (10, 32, 3, 13),
    "port stair": (11, 23, 0, 46.75), "portpier": (12, 19, 0, 46.75),
    "stage": (13, 32, 0, 82),
}
CHILDREN = {"entry": 13, "quay": 13, "side": 5}
# These volumes record ownership of the constructed gangway, shore shelf,
# stair bays and receiving slabs. They have no collision or floor surfaces.
RECEIVERS = (
    ("shore shelf", 13, (16.1, 64, 42, 76), 0, 3, True),
    ("stern gangway", 13, (30.5, 56, 33.5, 64), 0, 3, True),
    ("stern boarding deck", 0, (25, 53.9, 39, 56), 0, 3, True),
    ("port transfer", 12, (21.74, 44.64, 22.13, 47.56), 0, 3, True),
    ("weather bow", 5, (22.1, -9.75, 41.91, 8), 0, 3, True),
    ("upper work deck", 10, (24, 18, 40, 30), 3, 3, True),
    ("aft port approach", 11, (22.13, 45.93, 24.0, 47.59), 0, 3, False),
    ("engine lower landing", 11, (22.20, 21.93, 24.0, 35.53), -3, 3.04, False),
    ("engine upper landing", 11, (22.20, 21.93, 24.0, 24.05), 0, 3, False),
    ("crew flight", 11, (22.20, 33.42, 24.0, 46.10), 0, 3.04, False),
    ("crew upper landing", 11, (22.20, 33.42, 24.0, 36.01), 3, 3, False),
    ("net deck stair landing", 11, (22.20, 12.46, 24.0, 26.34), 3, 3.04, False),
    ("bridge stair landing", 11, (22.20, 23.95, 24.0, 26.34), 6, 3, False),
    ("pump hatch", 7, (28.50, .05, 34.67, 1.95), -3, 3, False),
)
OPENINGS = (
    (13, 0, 32,0,56, 2.5,"z","trail"), (13,12,19,0,64, 4,"z","trail"),
    (12,11,22.12,0,46.75, 1.8,"x","trail"), (11,0,24,0,46.75, 1.8,"x","trail"),
    (1,4,29.25,0,30, 1.7,"z","trail"), (2,4,34.75,0,30, 1.7,"z","trail"),
    (4,5,32,0,8, 1.7,"z","trail"), (5,0,41,0,40, 1.7,"z","trail"),
    (5,7,31.6,-1.5,1, 1.8,"x","stair"), (7,6,32,-3,4, 1.7,"z","trail"),
    (6,3,32,-3,24, 1.7,"z","trail"), (8,10,32,3,30, 1.7,"z","trail"),
)
STAIR_SIDES = {2: "engine lower landing", 3: "net deck stair landing",
               4: "engine upper landing", 5: "crew upper landing", 6: "bridge stair landing"}
SWINGS = (-1,-1,1,1,1,1,1)
REFUGES = (("galley", 25.35, 37.5, -math.pi/2), ("net", 38.25, 12, math.pi/2),
           ("hold", 25.25, 11, -math.pi/2))


def engine_power(assembly, zone):
    floor = zone["y"]
    for asset, kind, x, z, yaw, h in (
        ("level_trace_generator_refined", "generator", 36.9, 26.3, math.pi, .56),
        ("level_electrical_bank_worn", "breaker", 37.5, 38.9, math.pi, .89),
    ):
        parts = assembly.place(asset, Matrix.Translation(point(x, floor+.06, z)) @ Matrix.Rotation(yaw, 4, "Z"),
                               "Saint Elara " + kind, "engine-deck electrical equipment", "engine")
        low, high = bounds(parts)
        assert abs(low[1]-floor-.06) < .004
        for px in (low[0], high[0]):
            for pz in (low[2], high[2]):
                hit, _, _ = assembly.measured(point(px,floor+.18,pz),point(0,-1,0),.30,kind+" deck")
                assert abs(hit.z-floor) < .006
        assembly.box("Saint Elara " + kind + " plinth", (low[0]-.04,low[2]-.04,high[0]+.04,high[2]+.04),
                     floor,floor+.06,assembly.concrete,"engine-deck equipment foundation","engine")
        sign = 1 if kind == "generator" else -1
        front = high[2]+1 if sign == 1 else low[2]-1
        hit, _, _, _ = bvh(parts).ray_cast(point(x,floor+h,front),point(0,0,-sign),2)
        assert hit is not None, kind
        px,py,pz=world(hit)
        assembly.effect("elara-"+kind,kind,zone["owner"],(px,py,pz+sign*.015),
                        0 if sign == 1 else math.pi,"SE-MAIN")
    assembly.switch(zone,38.8,29,1,0)
    assembly.sockets[-1].update(id="elara-engine-switch",style="SE-ENGINE")
    assembly.controllers[-1].update(id="elara-engine-switch",circuit="SE-ENGINE")
    assembly.mounted_lamp(zone,"elara-engine-light",point(38.8,-.62,30.5),
                          point(1,0,0),1.5,point(0,0,1))
    assembly.loads[-1]["circuit"]="SE-ENGINE"
    assembly.raceway("Saint Elara engine feeder",((36.9,-2.42,26.3),(39.72,-2.42,26.3),
        (39.72,-.37,26.3),(39.72,-.37,38.9),(37.5,-.37,38.9),(37.5,-1.25,38.9)),.016)
    assembly.raceway("Saint Elara engine lamp branch",((39.72,-.37,29),(39.72,-.37,30.5),
        (39.72,-.58,30.5)),.012)


def compartment_lights(assembly, zones):
    # Switches sit on the approach side of each compartment. Keep the
    # gangway, door swings, refuge entrances and hatch landings clear.
    branches = (
        ("galley", 31.05, 38.65, 36.80, 1),
        ("mess", 38.95, 38.55, 36.75, 1),
        ("processing", 24.85, 25.15, 27.05, -1),
        ("hold", 38.95, 16.10, 13.90, 1),
        ("pump", 38.95, 2.65, .90, 1),
        ("crew", 38.95, 36.15, 38.15, 1),
        ("bridge", 24.85, 23.15, 21.10, -1),
        ("net", 24.85, 15.40, 17.15, -1),
    )
    for room, x, switch_z, lamp_z, dx in branches:
        zone = zones[room]
        floor = zone["y"]
        assembly.switch(zone,x,switch_z,dx,0)
        circuit = f"SE-{room.upper()}"
        assembly.sockets[-1].update(id=f"elara-{room}-switch",style=circuit)
        assembly.controllers[-1].update(id=f"elara-{room}-switch",circuit=circuit)
        drop = assembly.routes[-1]["points"][-1]
        assembly.mounted_lamp(zone,f"elara-{room}-light",point(x,floor+2.35,lamp_z),
                              point(dx,0,0),1.7,point(0,0,1))
        assembly.loads[-1]["circuit"]=circuit
        lamp_x,lamp_y,lamp_z = assembly.loads[-1]["mount"]
        offset_x = lamp_x-.028*dx
        assert abs(offset_x-drop[0]) < .012, room
        assembly.raceway(f"Saint Elara {room} branch",
                         (drop,(offset_x,drop[1],lamp_z),(offset_x,lamp_y+.12,lamp_z)),.012)


def pump_guards(assembly):
    # Guard the long sides of the real rectangular hatch. Its receiving
    # ends stay open for the existing foredeck and pump-room stair.
    deck = bvh([o for o in assembly.scene.objects if o.type == "MESH"
                and o.name in assembly.originals and "joined aft starboard and fore" in o.name])
    for label,z in (("forward",.035),("after",1.965)):
        for i,x in enumerate((28.7,30.15,31.6,33.05,34.5)):
            hit,normal,_,_ = deck.ray_cast(point(x,.14,z),point(0,-1,0),.22)
            assert hit is not None and abs(hit.z) < .004 and normal.z > .98,(label,x)
            assembly.tube(f"Saint Elara hatch {label} post {i}",(x,-.025,z),(x,1.04,z),.036)
        assembly.tube(f"Saint Elara hatch {label} top rail",(28.7,1.04,z),(34.5,1.04,z),.045)
        assembly.tube(f"Saint Elara hatch {label} mid rail",(28.7,.49,z),(34.5,.49,z),.026)


def flight_rails(assembly):
    for route in (13, 14, 17, 19):
        along_x = route == 14
        direction_index, cross_index = (0, 2) if along_x else (2, 0)
        treads = [assembly.scene.objects[f"route-{route} flight0 step{i}"] for i in (1,5,9,13,17)]
        for side in ("port", "starboard"):
            centers = []
            cross = 0 if side == "port" else 1
            for i, tread in enumerate(treads, 1):
                lo, hi = bounds([tread])
                course = (lo[direction_index]+hi[direction_index])/2
                edge = (lo, hi)[cross][cross_index] + (.062 if cross == 0 else -.062)
                x, z = (course,edge) if along_x else (edge,course)
                y = hi[1]
                assembly.tube(f"Saint Elara flight {route} {side} baluster {i}", (x,y-.025,z),(x,y+.99,z),.030)
                centers.append((x,y+.99,z))
            assembly.raceway(f"Saint Elara flight {route} {side} handrail",centers,.037)


def build():
    OUT.mkdir(parents=True,exist_ok=True)
    source={ext:SOURCE/f"coast-entrances.{ext}" for ext in ("blend","glb","hcs2")}
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
    pockets=cupboards(assembly,zones,REFUGES,site="elara",floor_roles=("floor",))
    engine_power(assembly,zones["engine"])
    compartment_lights(assembly,zones)
    pump_guards(assembly)
    flight_rails(assembly)
    for obj in assembly.built:
        obj.name=obj.name.removeprefix("College ")
    target={ext:OUT/f"vessel.{ext}" for ext in source}
    addition=ROOT/"build/saint-elara-installation.glb"
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
    rows.append(("ROOM","port stair",11,22.13,45.93,24,47.59,0,3,"false","Port stair bays",23,0,46.75))
    rows.extend(("SPACE",key,owner,*area,datum,height,str(outside).lower())
                for key,owner,area,datum,height,outside in RECEIVERS)
    rows.extend(("TREAD",name) for name in contract["stair_components"])
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
        a,b=door["rooms"]
        x,z=(door["along"],door["boundary"]) if door["axis"]=="z" else (door["boundary"],door["along"])
        room_a, room_b = owners[a], owners[b]
        if a == "port stair":
            a = STAIR_SIDES[i]
        elif b == "port stair":
            b = STAIR_SIDES[i]
        doors.append((1300+i,door["id"],a,b,room_a,room_b,x,door["sill_y"],z,door["clear_width"],
            door["axis"],door["boundary"]-.12,door["boundary"]+.12,door["clear_height"],2,1,SWINGS[i],leaf))
    table(OUT/"door-schedule.tsv",doors)
    (OUT/"construction.json").write_text(json.dumps(dict(
        source=layout_path.relative_to(ROOT).as_posix(),exports={k:sha(p) for k,p in target.items()},
        retained_components=len(prior),pockets=pockets,services=assembly.sockets,
        controllers=assembly.controllers,fixtures=assembly.loads),indent=2)+"\n",
        encoding="utf-8",newline="\n")


if __name__=="__main__":
    build()
