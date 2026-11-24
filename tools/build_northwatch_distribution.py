"""Extend Northwatch's existing main to three independently switched rooms."""
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import ASSETS, Assembly, point, world
from build_prison_sally_gate import export
from compose_room_terrain import split_native
from review_roster_macroterrain import records, sha
from verify_college_services import bvh, match_geometry

BASE = ROOT / "development/levels/rebuild"
PARENT = BASE / "northwatch-power-pass"
OUT = BASE / "northwatch-distribution-pass/wld-08"
HIGH = 2.791
ROOMS = (
    ("lab", 0, (8.8, 27.44, 0, -1), (12, 33.4),
     ((7,0,27.28),(7,0,29.25),(8.8,0,29.25),(8.8,0,27.44))),
    ("radio", 3, (21.44, 17.20, -1, 0), (26, 16),
     ((21.10,0,19),(21.44,0,18.4),(21.44,0,17.2))),
    ("records", 4, (33.44, 17.20, -1, 0), (38, 16),
     ((33.10,0,19),(33.44,0,18.4),(33.44,0,17.2))),
)
SLEEVES = (("shed to radio", "x", 13.15, 19.76, 20.24, "generator E pier0", "radio W pier0"),
           ("radio to records", "x", 13.15, 31.76, 32.24, "radio E pier0", "records W pier0"),
           ("shed to lab", "z", 15.0, 25.76, 26.24, "generator S end", "lab N end"))


def install(assembly):
    # Survey solely against the existing Blender scene. Added conduit and
    # fixtures cannot become the bearing surface for their own anchors.
    original = bvh([obj for obj in assembly.scene.objects if obj.type == "MESH"])
    anchors = []

    def ceiling_conduit(label, points, radius=.016):
        assembly.raceway(label, [(x, HIGH, z) for x,z in points], radius)
        for (a,b),(c,d) in zip(points,points[1:]):
            length = math.hypot(c-a,d-b)
            for index in range(max(1, math.ceil(length/1.22))):
                t = (index+.5)/max(1,math.ceil(length/1.22))
                x,z = a+(c-a)*t,b+(d-b)*t
                hit,normal,_,_ = original.ray_cast(point(x,HIGH+.025,z),Vector((0,0,1)),.12)
                assert hit is not None and abs(hit.z-2.84)<.006 and normal.z<-.98, (label,x,z,hit,normal)
                assembly.box(label+" ceiling saddle", (x-.045,z-.045,x+.045,z+.045),
                    HIGH-.024,hit.z+.003,assembly.mount,"ceiling-backed service conduit saddle","station")
                anchors.append(dict(id=label, contact=world(hit)))

    def sleeve(spec):
        label,axis,along,left,right,west,east=spec
        along_point=lambda at: (at,HIGH,along) if axis=="x" else (along,HIGH,at)
        direction=point(1,0,0) if axis=="x" else point(0,0,1)
        surveyed=[]
        for face,sign,expected in ((left,1,west),(right,-1,east)):
            start=point(*along_point(face-.13*sign))
            hit,normal,obj=assembly.measured(start,direction*sign,.18,label)
            assert expected in obj.name and abs((hit-point(*along_point(face))).dot(direction))<.003
            surveyed.append(dict(face=world(hit),receiver=obj.name))
            x,_,z=along_point(face-.022*sign)
            assembly.box(label+" capped termination", (x-.052,z-.052,x+.052,z+.052),
                         HIGH-.044,HIGH+.044,assembly.housing,"masonry-backed sealed cable termination","station")
        assembly.raceway(label+" sealed sleeve", [along_point(left-.04),along_point(right+.04)], .020, False)
        return dict(id=label, faces=surveyed)

    sleeves=[sleeve(spec) for spec in SLEEVES]
    drops={}
    for name,owner,location,lamp,_ in ROOMS:
        zone=dict(id=name,owner=owner,y=0,height=3)
        assembly.switch(zone,*location)
        controller=assembly.controllers[-1]
        assert "authored doorway masonry" == assembly.scene.objects[controller["support"]].get("role")
        drops[name]=next(route["points"][-1] for route in assembly.routes if route["id"]==f"EM-{owner:02d} switch drop")
        assembly.mounted_lamp(zone,"northwatch-"+name+"-task-light",point(lamp[0],1.8,lamp[1]),
                              Vector((0,0,1)),1.3,point(1,0,0))
        assert assembly.loads[-1]["support"]=="Ceiling cap "+name

    assembly.raceway("NW-MAIN north wall and ceiling takeoff",
                     [(11.90,2.44,8.29),(15,2.44,8.29),(15,HIGH,8.29)],.025)
    for x,y in ((13.2,2.44),(14.55,2.44),(15,2.62)):
        at,normal,_,_=original.ray_cast(point(x,y+.045,8.39),point(0,0,-1),.19)
        assert at is not None and abs(-at.y-8.24)<.003 and normal.y<-.98,(x,y,at,normal)
        assembly.box("main takeoff wall saddle",(x-.045,8.236,x+.045,8.321),y-.035,y+.035,
                     assembly.mount,"masonry-backed main takeoff saddle","generator")
        anchors.append(dict(id="NW-MAIN north wall and ceiling takeoff",contact=world(at)))

    ceiling_conduit("NW-MAIN shed overhead", [(15,8.29),(15,13.15),(15,25.72)],.023)
    ceiling_conduit("NW-MAIN shed to radio", [(15,13.15),(19.72,13.15)],.023)
    ceiling_conduit("NW-MAIN radio to records", [(20.28,13.15),(31.72,13.15)],.023)
    feeds={"radio": [(20.29,13.15),(20.29,17.20)],"records":[(32.28,13.15),(32.29,13.15),(32.29,17.20)],
           "lab":[(15.,26.28),(15.,26.29),(8.8,26.29)]}
    for name,owner,_,lamp,_ in ROOMS:
        dx,dy,dz=drops[name]
        feed=feeds[name]
        ceiling_conduit(f"NW-{owner:02d} supply to switch",feed)
        x,z=feed[-1]
        assembly.raceway(f"NW-{owner:02d} switch junction link",[(x,HIGH,z),(dx,dy,dz)],.014)
        x,z=lamp
        elbow=(dx,z) if name=="lab" else (x,dz)
        assembly.raceway(f"NW-{owner:02d} switched leg riser",[(dx,dy,dz),(dx,HIGH,dz)],.014)
        ceiling_conduit(f"NW-{owner:02d} switched leg",[(dx,dz),elbow,(x,z)])

    for rows in (assembly.sockets, assembly.controllers, assembly.loads, assembly.routes):
        for row in rows:
            row["id"]=row["id"].replace("college-","northwatch-").replace("EM-","NW-")
            for key in ("circuit","style"):
                if key in row:
                    row[key]=row[key].replace("EM-","NW-")
    return dict(sleeves=sleeves,anchors=anchors,services=assembly.sockets,loads=assembly.loads,
                controllers=assembly.controllers,raceways=assembly.routes,
                canonical_assets={key:sha(ASSETS/(key+".glb")) for key in ("light_switch_1","level_utility_light")})


def build(parent_dir=PARENT, output=OUT, rooms=ROOMS, fit=install, stem="station-with-distribution"):
    parent=json.loads((parent_dir/"checkpoint.json").read_bytes())
    for name,digest in (parent["exports"]|parent["evidence"]).items():
        assert sha(ROOT/name)==digest,name
    old={Path(name).suffix[1:]:ROOT/name for name in parent["exports"]}
    bpy.ops.wm.open_mainfile(filepath=str(old["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    assembly=Assembly()
    receipt=fit(assembly)
    for obj in assembly.built:
        obj.name="Northwatch distribution "+obj.name.removeprefix("College ")
    output.mkdir(parents=True,exist_ok=True)
    paths={ext:output/f"{stem}.{ext}" for ext in ("blend","glb","hcs2")}
    fragment=ROOT/"build"/f"{stem}-fragment.glb"
    export(fragment,assembly.built)
    append(old["glb"],fragment,paths["glb"],reuse_embedded_images=True)
    fragment.unlink()
    added=native_write(old["hcs2"],paths["hcs2"],set(),assembly.built,
                       open_components={obj.name for obj in assembly.built if obj.get("source_asset")})
    before,after=(split_native(path) for path in (old["hcs2"],paths["hcs2"]))
    assert after[:len(before)]==before and len(after)-len(before)==len(added)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(paths["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(paths["glb"]))
    for name,expected in added:
        mesh=records(bpy.context.scene.objects[name])
        assert match_geometry([face for face,_ in expected],[face for face,_ in mesh]),name
    prior=parent_dir/"wld-08"
    rows=[("SERVICE",s["id"],s["type"],s["room"],*s["position"],s["yaw"],s["style"]) for s in assembly.sockets]
    for name,_,_,_,route in rooms:
        rows.extend(("POINT",f"Northwatch-switch-{name}",*p) for p in route)
    (output/"runtime-input.tsv").write_bytes((prior/"runtime-input.tsv").read_bytes()
        +"".join("\t".join(map(str,row))+"\n" for row in rows).encode())
    (output/"door-schedule.tsv").write_bytes((prior/"door-schedule.tsv").read_bytes())
    receipt.update(exports={p.relative_to(ROOT).as_posix():sha(p) for p in paths.values()},
        previous_scene=parent["native_scene_revision"],retained_collision_parts=len(before),added_collision_parts=len(added))
    (output/"construction.json").write_text(json.dumps(receipt,indent=2)+"\n",encoding="utf-8",newline="\n")
    print("PASS Northwatch",len(receipt["anchors"]),"surveyed clips and",len(receipt["sleeves"]),"sealed transitions",len(added),"new native parts")


if __name__=="__main__":
    build()
