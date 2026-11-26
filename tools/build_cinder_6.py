"""Fit the Site Cinder-6 plant, room lighting and native investigation bindings."""
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
from build_lookout_native_arrival import table
from build_mine_case import Case, DOWN
from build_prison_sally_gate import export
from build_roster_foundations import material
from build_school_electrical import cupboards
from review_roster_macroterrain import sha
from verify_college_electrical import native_records
from verify_college_services import bvh

BASE = ROOT / "development/levels/rebuild"
SOURCE = BASE / "flush-porch-and-threshold-pass/wld-18"
OUT = BASE / "cinder-6"
ANCHORS = {
    "servicehouse": (0, 19, 0, 62), "head": (1, 34, 0, 48), "shop": (2, 47, 0, 48),
    "exit": (3, 7, 0, 52), "records": (4, 15, -4, 45), "west": (5, 18, -4, 32),
    "upperring": (6, 23, -4, 28), "exhibit": (7, 48, -4, 26.5), "east": (8, 48, -8, 28),
    "lowring": (9, 41, -8, 28), "crew": (10, 18, -8, 32), "plant": (11, 49, -8, 45),
    "yard": (12, 40, 0, 62), "stage": (13, 32, 0, 86),
}
CHILDREN = {"entry": 13, "pump": 10, "escape-lower-connector": 3}
RECEIVERS = (
    ("service forecourt",13,(16.6,70,40,84),0,3,True),
    ("staging road head",13,(24,96,40,99.58),0,3,True),
    ("west stair head",1,(16.7,46.9,28,49.1),0,3,False),
    ("west flight",5,(16.5,31.6,19.5,47.25),-4,3.95,False),
    ("east flight",7,(50.5,36.8,53.5,47.25),-4,3.95,False),
    ("escape flight",3,(4.75,33.15,7.25,48.1),-4,3.98,False),
    ("east switchback",8,(43.7,27.6,52.4,35.5),-8,3.96,False),
    ("west crew flight",10,(11.4,27.6,19.32,30.43),-8,3.98,False),
)
OPENINGS = (
    (13,12,36,0,70,2,"z","trail"), (1,5,18,-2,40,1.5,"z","stair"),
    (5,4,15,-4,38,1.7,"z","trail"),(2,7,52,-2,44,1.5,"z","stair"),
    (7,8,51,-5.2,32,1.6,"z","stair"),(10,5,15.5,-6.1,29,1.6,"x","stair"),
    (8,11,49,-8,38,1.6,"z","trail"),
)
EXTERIOR = {1:"service forecourt",3:"yard",4:"west stair head",6:"yard",
            7:"upperring",8:"upperring",9:"lowring",10:"lowring"}
SWINGS = (1,-1,-1,1,-1,1,1,-1,1,1,-1)
REFUGES = (("servicehouse",12,66.1,-math.pi/2),("records",10.2,48,-math.pi/2),
           ("crew",20.1,33.0,math.pi/2))


def main_supply(assembly, zone):
    floor=zone["y"]
    for asset, kind, x, z, yaw, y in (
        ("level_trace_generator_refined","generator",49.2,40.2,math.pi,.56),
        ("level_electrical_bank_worn","breaker",53.2,51.1,math.pi,.89),
    ):
        parts=assembly.place(asset,Matrix.Translation(point(x,floor+.06,z)) @ Matrix.Rotation(yaw,4,"Z"),
                             f"Cinder {kind}","founded lower plant equipment","plant")
        low,high=bounds(parts)
        assert abs(low[1]-floor-.06)<.004
        for px in (low[0],high[0]):
            for pz in (low[2],high[2]):
                hit,_,_=assembly.measured(point(px,floor+.18,pz),point(0,-1,0),.30,kind+" slab")
                assert abs(hit.z-floor)<.006
        assembly.box(f"Cinder {kind} plinth",(low[0]-.04,low[2]-.04,high[0]+.04,high[2]+.04),
                     floor,floor+.06,assembly.concrete,"equipment slab foundation","plant")
        sign=1 if kind=="generator" else -1
        hit,_,_,_=bvh(parts).ray_cast(point(x,floor+y,high[2]+1 if sign==1 else low[2]-1),
                                      point(0,0,-sign),2)
        assert hit is not None,kind
        px,py,pz=world(hit)
        assembly.effect("cinder-"+kind,kind,zone["owner"],(px,py,pz+.015*sign),
                        0 if sign==1 else math.pi,"C6-MAIN")
    assembly.raceway("Cinder lower plant main feeder",((49.2,-7.42,40.2),(55.71,-7.42,40.2),
        (55.71,-5.25,40.2),(55.71,-5.25,51.70),(53.2,-5.25,51.70),(53.2,-6.15,51.38)),.017)


def room_power(assembly,zones):
    branches=(
        ("servicehouse",(11.2,61.7),(11.2,63.4),(-1,0)),
        ("head",(30,43.15),(32,43.15),(0,-1)),
        ("shop",(52.8,49.6),(52.8,51.3),(1,0)),
        ("exit",(5.15,53.4),(5.15,51.5),(-1,0)),
        ("records",(20.8,44),(20.8,46.3),(1,0)),
        ("west",(9.2,36),(9.2,37.1),(-1,0)),
        ("exhibit",(54.8,24),(54.8,25.7),(1,0)),
        ("east",(54.8,24),(54.8,25.7),(1,0)),
        ("crew",(20.8,33.5),(20.8,35.4),(1,0)),
        ("plant",(54.8,46.5),(54.8,48.2),(1,0)),
    )
    for room,switch,lamp,(dx,dz) in branches:
        zone=zones[room]
        label=f"cinder-{room}"
        assembly.switch(zone,*switch,dx,dz)
        circuit=f"C6-{zone['owner']:02d}"
        assembly.sockets[-1].update(id=label+"-switch",style=circuit)
        assembly.controllers[-1].update(id=label+"-switch",circuit=circuit)
        drop=assembly.routes[-1]["points"][-1]
        toward=point(dx,0,dz)
        assembly.mounted_lamp(zone,label+"-light",point(lamp[0],zone["y"]+2.35,lamp[1]),
                              toward,1.7,point(-dz,0,dx))
        assembly.loads[-1]["circuit"]=circuit
        x,y,z=assembly.loads[-1]["mount"]
        target=(x-.028*dx,drop[1],z-.028*dz)
        assert abs((target[0]-drop[0])*dx+(target[2]-drop[2])*dz)<.012,room
        assembly.raceway("Cinder "+room+" lamp circuit",(drop,target,(target[0],y+.12,target[2])),.012)


def gallery_power(assembly, zone, switch_segment):
    floor=zone["y"]
    room=zone["id"]
    circuit=f"C6-{zone['owner']:02d}"

    def polar(radius, angle):
        return 32+radius*math.cos(angle),28+radius*math.sin(angle)

    switch_angle=(switch_segment+.5)*math.tau/64
    dx,dz=math.cos(switch_angle),math.sin(switch_angle)
    assembly.switch(zone,*polar(9.15,switch_angle),dx,dz)
    switch=f"cinder-{room}-switch"
    assembly.sockets[-1].update(id=switch,style=circuit)
    assembly.controllers[-1].update(id=switch,circuit=circuit)
    drop=assembly.routes[-1]["points"][-1]
    mount_height=None
    for lamp in range(8):
        angle=switch_angle+lamp*math.tau/8
        x,z=polar(9.15,angle)
        assembly.mounted_lamp(zone,f"cinder-{room}-lamp-{lamp+1}",
                              point(x,floor+2.3,z),point(0,1,0),.65,
                              point(-math.sin(angle),0,math.cos(angle)))
        fixture=assembly.loads[-1]
        fixture["circuit"]=circuit
        ceiling=fixture["mount"][1]
        if mount_height is None:
            mount_height=ceiling-.032
        assert abs(mount_height+.032-ceiling)<.008, room
    # A continuous ceiling raceway stays on the annular roof and away
    # from the guarded shaft, door heads and receiving slabs.
    loop=[]
    for join in range(17):
        x,z=polar(9.15,switch_angle+join*math.tau/16)
        loop.append((x,mount_height,z))
    assembly.raceway(f"Cinder {room} ceiling feeder",loop,.016)
    assembly.raceway(f"Cinder {room} switch branch",
                      (drop,(drop[0],mount_height,drop[2]),loop[0]),.014)


def fit_workrooms(case):
    furnishings=[]
    for asset,name,zone,x,y,z in (
        ("level_work_repair_bench","surface maintenance bench","shop",49.6,0,43.5),
        ("level_workbench_worn","surface duty desk","servicehouse",23.8,0,56.6),
        ("level_trace_camp_bed","west standby bunk","crew",11.6,-8,36.15),
        ("level_trace_camp_bed","east standby bunk","crew",16.15,-8,36.15),
        ("level_work_crew_footlocker","crew equipment locker","crew",18.7,-8,36.7),
    ):
        _,placement=case.fit(asset,name,zone,x,y,z)
        furnishings.append(dict(name=name,room=zone,**placement))

    x,y,z=13.7,-8,33.65
    table,placement=case.fit("level_event_side_table_worn","crew telephone stand","crew",x,y,z)
    top,normal,_,_=bvh(table).ray_cast(point(x,y+1.55,z),DOWN,1.5)
    assert top is not None and normal.z>.98,"Emergency telephone needs a supported stand"
    phone,installation=case.fit("level_event_phone_worn","crew emergency telephone","crew",x,top.z+.001,z,support=table)
    assert len(phone)==1
    phone[0].name="Cinder lower crew emergency telephone"
    low,high=bounds(phone)
    face,_,_,_=bvh(phone).ray_cast(point(x,low[1]+.065,high[2]+.8),point(0,0,-1),1.6)
    assert face is not None,"Emergency telephone needs an exposed casing"
    px,py,pz=world(face)
    case.effect(phone[0].name,"native-device",10,(px,py,pz+.035),0,"PHONE")
    furnishings.append(dict(name="crew emergency telephone",room="crew",stand=placement,device=installation))
    return furnishings


def fit_staging_enclosure(case):
    gate_z=99.58
    west,east=24.10,39.90
    cladding=material("Cinder oxidized pressed steel security panels",(.225,.256,.243))
    steel=material("Cinder dark painted steel fence frames",(.11,.13,.126))
    concrete=material("Cinder exposed aggregate fence footings",(.38,.37,.32))

    def grade(x,z):
        contact,normal,_,_=case.support_surfaces.ray_cast(point(x,.90,z),DOWN,1.3)
        assert contact is not None and normal.z>.94,("Roadside foundation",x,z)
        return contact.z

    posts=set()
    def post(x,z):
        if (x,z) in posts:return
        posts.add((x,z))
        foot=(x-.15,z-.15,x+.15,z+.15)
        levels=[grade(px,pz) for px in (foot[0],foot[2]) for pz in (foot[1],foot[3])]
        base=min(levels)
        case.box("Cinder fence concrete footing",foot,base-.24,max(levels)+.035,
                 concrete,"buried and grounded security fence footing","stage")
        case.box("Cinder square security post",(x-.06,z-.06,x+.06,z+.06),base,2.65,
                 steel,"steel post anchored in its concrete footing","stage")

    def panel(x0,z0,x1,z1,gate=False):
        # Each supported infill sits between its posts. Rail and hinge faces
        # remain visible from the road while the solid leaves stop collision.
        along_x=z0==z1
        width=.048 if gate else .032
        label="closed vehicle gate leaf" if gate else "pressed steel security fence panel"
        rect=(x0-.015,z0-width,x1+.015,z1+width) if along_x else (x0-width,z0-.015,x1+width,z1+.015)
        base=min(grade(x0,z0),grade((x0+x1)/2,(z0+z1)/2),grade(x1,z1))
        case.box("Cinder "+label,rect,base+.025,2.48,cladding,label,"stage")
        frame=(rect[0]-.015,rect[1]-.026,rect[2]+.015,rect[3]+.026) if along_x else (
               rect[0]-.026,rect[1]-.015,rect[2]+.026,rect[3]+.015)
        for height in (.26,1.28,2.38):
            case.box("Cinder "+label+" cross rail",frame,height,height+.075,
                     steel,"bolted steel rail carries the metal infill","stage")

    for x in (west,east):
        stations=[84.06,87.16,90.27,93.37,96.48,gate_z]
        for z in stations:post(x,z)
        for z0,z1 in zip(stations,stations[1:]):panel(x,z0,x,z1)

    for x0,x1 in ((west,26.03),(26.03,28),(36,37.97),(37.97,east)):
        post(x0,gate_z)
        post(x1,gate_z)
        panel(x0,gate_z,x1,gate_z)
    panel(28,gate_z,32,gate_z,True)
    panel(32,gate_z,36,gate_z,True)
    for x in (28.09,35.91):
        for y in (.43,1.92):
            case.box("Cinder vehicle gate hinge plate",(x-.16,gate_z-.11,x+.16,gate_z-.025),
                     y,y+.12,steel,"steel gate hinge joins leaf to its grounded post","stage")
    case.box("Cinder vehicle gate center latch",(31.73,gate_z-.11,32.27,gate_z-.025),
             1.29,1.39,steel,"closed vehicle-gate latch","stage")
    return dict(gate=[28,gate_z,36],fence_returns=[[west,84.06,gate_z],[east,84.06,gate_z]],
                mounted_posts=len(posts),road_head_owner=13)


def build():
    OUT.mkdir(parents=True,exist_ok=True)
    source={ext:SOURCE/f"surveyed-thresholds.{ext}" for ext in ("blend","glb","hcs2")}
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
    assembly=Case()
    pockets=cupboards(assembly,zones,REFUGES,site="cinder",floor_roles=("floor","constructed continuous room or arcade floor"))
    main_supply(assembly,zones["plant"])
    room_power(assembly,zones)
    gallery_power(assembly,zones["upperring"],29)
    gallery_power(assembly,zones["lowring"],2)
    workrooms=fit_workrooms(assembly)
    staging_enclosure=fit_staging_enclosure(assembly)
    for obj in assembly.built:
        obj.name=obj.name.removeprefix("College ").replace("Mine case ","Cinder ",1)
    target={ext:OUT/f"facility.{ext}" for ext in source}
    addition=ROOT/"build/cinder-6-installation.glb"
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
        # The shell source retains its original annulus zoning. The built
        # retaining walls and roof make these two areas indoor gameplay rooms.
        outside=str(z["kind"] in ("outside","staging") and key not in ("upperring","lowring")).lower()
        body=(key,owners[key],*z["bounds"],z["y"],z["height"],outside)
        rows.append(("ROOM",*body,z["name"],*ANCHORS[key][1:]) if key in ANCHORS else ("SPACE",*body))
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
        a=door["rooms"][0]
        b=door["rooms"][1] if len(door["rooms"])==2 else EXTERIOR[i]
        x,y,z=door["frame_center"]
        other=owners[b] if b in owners else next(r[1] for r in RECEIVERS if r[0]==b)
        width=door["clear_width"]
        doors.append((1500+i,door["id"],a,b,owners[a],other,x,y,z,width,
                      door["axis"],*door["frame_span"],door["clear_height"],
                      2 if width>=1.7 else 1,1,SWINGS[i],leaf))
    table(OUT/"door-schedule.tsv",doors)
    (OUT/"construction.json").write_text(json.dumps(dict(
        source=layout_path.relative_to(ROOT).as_posix(),exports={k:sha(p) for k,p in target.items()},
        retained_components=len(prior),pockets=pockets,services=assembly.sockets,
        controllers=assembly.controllers,fixtures=assembly.loads,workrooms=workrooms,
        staging_enclosure=staging_enclosure),indent=2)+"\n",
        encoding="utf-8",newline="\n")


if __name__=="__main__":
    build()
